package com.example.pos.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.pos.data.local.OrganizationEntity
import com.example.pos.data.local.SaleEntity
import com.example.pos.data.local.SaleItemEntity
import com.example.pos.data.local.SalePaymentEntity
import com.example.pos.data.model.PaymentType
import com.example.pos.data.model.TaxMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EscPosPrinterTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test esc pos builder column widths and standard command sequences`() {
        // 58mm rolls (32 columns)
        val builder58 = EscPosCommandBuilder(paperWidthMm = 58)
        assertEquals(32, builder58.columns)

        // 80mm rolls (48 columns)
        val builder80 = EscPosCommandBuilder(paperWidthMm = 80)
        assertEquals(48, builder80.columns)

        // Verify ESC @ init prefix
        val bytes = builder58.toBytes()
        assertTrue(bytes.size >= 2)
        assertEquals(EscPosCommandBuilder.ESC, bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
    }

    @Test
    fun `test esc pos formatting alignment styles and cash drawer pulse`() {
        val b = EscPosCommandBuilder(paperWidthMm = 58)
            .initPrinter()
            .alignCenter()
            .bold(true)
            .textLine("STORE NAME")
            .bold(false)
            .alignLeft()
            .row("Subtotal:", "$10.00")
            .openCashDrawer(pin = 0)
            .cutPaper(partial = true)

        val bytes = b.toBytes()
        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val byteString = String(bytes, Charsets.ISO_8859_1)

        // Check text content was serialized
        assertTrue(byteString.contains("STORE NAME"))
        assertTrue(byteString.contains("Subtotal:"))
        assertTrue(byteString.contains("$10.00"))

        // Verify Cash Drawer kick byte sequence: ESC p 0 25 250 (0x1B, 0x70, 0x00, 0x19, 0xFA)
        var foundDrawerPulse = false
        for (i in 0 until bytes.size - 4) {
            if (bytes[i] == EscPosCommandBuilder.ESC &&
                bytes[i + 1] == 0x70.toByte() &&
                bytes[i + 2] == 0x00.toByte() &&
                bytes[i + 3] == 25.toByte() &&
                bytes[i + 4] == 250.toByte()
            ) {
                foundDrawerPulse = true
                break
            }
        }
        assertTrue("Cash drawer pulse command sequence should be emitted", foundDrawerPulse)

        // Verify Paper Cut command: GS V 66 0 (0x1D, 0x56, 0x42, 0x00)
        var foundCut = false
        for (i in 0 until bytes.size - 3) {
            if (bytes[i] == EscPosCommandBuilder.GS &&
                bytes[i + 1] == 0x56.toByte() &&
                bytes[i + 2] == 66.toByte() &&
                bytes[i + 3] == 0.toByte()
            ) {
                foundCut = true
                break
            }
        }
        assertTrue("Paper cut command sequence should be emitted", foundCut)
    }

    @Test
    fun `test esc pos receipt factory produces complete invoice receipt`() {
        val org = OrganizationEntity(
            id = "org_himalayan",
            orgId = "org_himalayan",
            name = "Himalayan Coffee & Bakery",
            taxId = "PAN-601928374",
            currencySymbol = "Rs",
            taxMode = TaxMode.EXCLUSIVE,
            ownerUid = "owner_1"
        )

        val sale = SaleEntity(
            id = "sale_101",
            orgId = org.id,
            branchId = "br_main",
            registerId = "reg_1",
            invoiceNo = "INV-2026-0042",
            subtotalInCents = 120000L,
            discountInCents = 10000L,
            taxInCents = 14300L,
            grandTotalInCents = 124300L,
            paymentStatus = "COMPLETED",
            createdAt = 1774000000000L,
            createdBy = "Siddhartha (Cashier)"
        )

        val items = listOf(
            SaleItemEntity(
                id = "si_1",
                orgId = org.id,
                saleId = sale.id,
                productId = "prod_cappuccino",
                productName = "Himalayan Cappuccino",
                unitPriceInCents = 40000L,
                quantity = 2,
                discountInCents = 0L,
                taxInCents = 10400L,
                lineTotalInCents = 80000L
            ),
            SaleItemEntity(
                id = "si_2",
                orgId = org.id,
                saleId = sale.id,
                productId = "prod_croissant",
                productName = "Almond Croissant",
                unitPriceInCents = 40000L,
                quantity = 1,
                discountInCents = 10000L,
                taxInCents = 3900L,
                lineTotalInCents = 30000L
            )
        )

        val payments = listOf(
            SalePaymentEntity(
                id = "sp_1",
                orgId = org.id,
                saleId = sale.id,
                paymentType = PaymentType.CASH,
                amountInCents = 150000L,
                changeGivenInCents = 25700L,
                referenceNo = ""
            )
        )

        val receiptBytes58 = EscPosReceiptFactory.createReceipt(
            org = org,
            sale = sale,
            items = items,
            payments = payments,
            customerName = "Anish Thapa",
            paperWidthMm = 58,
            triggerCashDrawer = true
        )

        val rawText = String(receiptBytes58, Charsets.ISO_8859_1)

        // Verify key receipt elements
        assertTrue("Store name should be uppercase on receipt", rawText.contains("HIMALAYAN COFFEE & BAKERY"))
        assertTrue("Tax ID should be listed", rawText.contains("PAN-601928374"))
        assertTrue("Invoice number should be listed", rawText.contains("INV-2026-0042"))
        assertTrue("Customer name should be listed", rawText.contains("Anish Thapa"))
        assertTrue("Line items should be printed", rawText.contains("Himalayan Cappuccino"))
        assertTrue("Almond Croissant should be printed", rawText.contains("Almond Croissant"))
        assertTrue("Cash tender should be listed", rawText.contains("CASH"))
        assertTrue("Change returned should be listed", rawText.contains("Change Returned"))

        // Barcode check (GS k 73 / Code128)
        var foundBarcode128 = false
        for (i in 0 until receiptBytes58.size - 3) {
            if (receiptBytes58[i] == EscPosCommandBuilder.GS &&
                receiptBytes58[i + 1] == 0x6B.toByte() &&
                receiptBytes58[i + 2] == 73.toByte()
            ) {
                foundBarcode128 = true
                break
            }
        }
        assertTrue("Barcode 128 command should be present on receipt", foundBarcode128)
    }

    @Test
    fun `test esc pos test page slip generation`() {
        val testPageBytes = EscPosReceiptFactory.createTestPage(
            storeName = "Everest Mart POS",
            paperWidthMm = 80,
            triggerCashDrawer = true
        )

        assertNotNull(testPageBytes)
        val text = String(testPageBytes, Charsets.ISO_8859_1)
        assertTrue(text.contains("PRINTER TEST"))
        assertTrue(text.contains("Everest Mart POS"))
        assertTrue(text.contains("80mm (48 cols)"))
        assertTrue(text.contains("Barcode 128 Test"))
        assertTrue(text.contains("2D QR Code Test"))
    }

    @Test
    fun `test thermal printer manager initializes safely`() {
        val manager = ThermalPrinterManager(context)
        val printers = manager.getAvailablePrinters()
        // In Robolectric environment with mock bluetooth, available list should be safe (empty or discovered)
        assertNotNull(printers)
    }
}
