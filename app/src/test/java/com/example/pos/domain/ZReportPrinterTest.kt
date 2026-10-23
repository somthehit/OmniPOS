package com.example.pos.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.pos.data.local.BranchEntity
import com.example.pos.data.local.OrganizationEntity
import com.example.pos.data.local.RegisterEntity
import com.example.pos.data.local.SaleEntity
import com.example.pos.data.local.SaleItemEntity
import com.example.pos.data.local.SalePaymentEntity
import com.example.pos.data.model.PaymentType
import com.example.pos.data.model.TaxMode
import com.example.pos.data.model.ZReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ZReportPrinterTest {

    private lateinit var context: Context
    private val testOrg = OrganizationEntity(
        id = "org_test",
        name = "Apex Mart Kathmandu",
        taxId = "600123456",
        currencySymbol = "Rs",
        taxMode = TaxMode.EXCLUSIVE,
        ownerUid = "usr_owner_01"
    )
    private val testBranch = BranchEntity(
        id = "br_main_01",
        orgId = "org_test",
        name = "Thamel Branch",
        code = "THM",
        address = "Tridevi Marg, Kathmandu",
        phone = "9800000000"
    )
    private val testRegister = RegisterEntity(
        id = "reg_01",
        orgId = "org_test",
        branchId = "br_main_01",
        name = "Terminal #1",
        code = "REG1"
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test z-report esc pos generation with balanced cash snapshot`() {
        val zReport = ZReport(
            shiftId = "shift_20261021_001",
            cashierName = "Rohan Sharma",
            openedAt = 1774000000000L,
            closedAt = 1774028800000L,
            openingFloatInCents = 500000L, // Rs 5000.00
            totalSalesCount = 14,
            totalSalesAmountInCents = 1850000L, // Rs 18500.00
            cashSalesInCents = 1200000L, // Rs 12000.00
            cardSalesInCents = 400000L, // Rs 4000.00
            qrSalesInCents = 250000L, // Rs 2500.00
            creditSalesInCents = 0L,
            payInInCents = 100000L, // Rs 1000.00
            payOutInCents = 50000L, // Rs 500.00
            expectedCashInCents = 1750000L, // 5000 + 12000 + 1000 - 500 = 17500.00
            countedCashInCents = 1750000L,
            cashVarianceInCents = 0L,
            expectedOnlineInCents = 650000L, // 4000 + 2500
            countedOnlineInCents = 650000L,
            onlineVarianceInCents = 0L,
            onlineBillsCount = 8,
            safeDropInCents = 0L,
            creditCollectionInCents = 0L,
            closingFloatInCents = 500000L,
            depositAmountInCents = 1250000L,
            status = "APPROVED"
        )

        // Generate ESC/POS bytes for 80mm
        val bytes = EscPosReceiptFactory.createZReport(
            org = testOrg,
            zReport = zReport,
            paperWidthMm = 80,
            branch = testBranch,
            register = testRegister,
            isReprint = false
        )

        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val text = String(bytes, Charsets.ISO_8859_1)

        // Check Header & Identifiers
        assertTrue(text.contains("APEX MART KATHMANDU"))
        assertTrue(text.contains("REGISTER CLOSING Z-REPORT"))
        assertTrue(text.contains("Thamel Branch"))
        assertTrue(text.contains("Terminal #1"))
        assertTrue(text.contains("Rohan Sharma"))
        assertTrue(text.contains("TAX / PAN ID: 600123456"))

        // Check Financials
        assertTrue("Opening float must match snapshot", text.contains("Rs 5,000.00"))
        assertTrue("Gross sales must match snapshot", text.contains("Rs 18,500.00"))
        assertTrue("Cash sales must match snapshot", text.contains("Rs 12,000.00"))
        assertTrue("Card sales must match snapshot", text.contains("Rs 4,000.00"))
        assertTrue("QR sales must match snapshot", text.contains("Rs 2,500.00"))
        assertTrue("Balanced label should appear", text.contains("[BALANCED]"))
        assertTrue("Audit review status should be displayed", text.contains("APPROVED"))

        // Handover & Deposit
        assertTrue(text.contains("Rs 12,500.00"))

        // Ensure Cash Drawer is NEVER kicked during Z-Report printing
        var foundDrawerPulse = false
        for (i in 0 until bytes.size - 4) {
            if (bytes[i] == EscPosCommandBuilder.ESC &&
                bytes[i + 1] == 0x70.toByte() &&
                bytes[i + 2] == 0x00.toByte()
            ) {
                foundDrawerPulse = true
                break
            }
        }
        assertFalse("Cash drawer pulse MUST NOT be emitted when printing Z-Reports", foundDrawerPulse)

        // Ensure Paper Cut is included
        var foundCut = false
        for (i in 0 until bytes.size - 3) {
            if (bytes[i] == EscPosCommandBuilder.GS &&
                bytes[i + 1] == 0x56.toByte()
            ) {
                foundCut = true
                break
            }
        }
        assertTrue("Paper cut command should be present at end of Z-Report", foundCut)
    }

    @Test
    fun `test z-report esc pos generation with cash shortage discrepancy and pending review`() {
        val zReport = ZReport(
            shiftId = "shift_shortage_999",
            cashierName = "Bikram Thapa",
            openedAt = 1774000000000L,
            closedAt = 1774028800000L,
            openingFloatInCents = 200000L,
            totalSalesCount = 5,
            totalSalesAmountInCents = 1000000L,
            cashSalesInCents = 1000000L,
            cardSalesInCents = 0L,
            qrSalesInCents = 0L,
            creditSalesInCents = 0L,
            payInInCents = 0L,
            payOutInCents = 0L,
            expectedCashInCents = 1200000L, // Rs 12000.00
            countedCashInCents = 1150000L, // Rs 11500.00 (shortage of Rs 500)
            cashVarianceInCents = -50000L,
            varianceReason = "Lost 500 note during cash exchange",
            status = "PENDING_REVIEW"
        )

        val bytes = EscPosReceiptFactory.createZReport(
            org = testOrg,
            zReport = zReport,
            paperWidthMm = 58,
            branch = testBranch,
            register = testRegister,
            isReprint = false
        )

        val text = String(bytes, Charsets.ISO_8859_1)
        assertTrue(text.contains("[SHORTAGE]"))
        assertTrue(text.contains("Rs 500.00"))
        assertTrue(text.contains("Lost 500 note during cash exchange"))
        assertTrue(text.contains("PENDING_REVIEW"))
    }

    @Test
    fun `test z-report reprint banner formatting`() {
        val zReport = ZReport(
            shiftId = "shift_reprint_777",
            cashierName = "Anita Giri",
            openedAt = 1774000000000L,
            closedAt = 1774028800000L,
            openingFloatInCents = 300000L,
            totalSalesCount = 2,
            totalSalesAmountInCents = 500000L,
            cashSalesInCents = 500000L,
            cardSalesInCents = 0L,
            qrSalesInCents = 0L,
            creditSalesInCents = 0L,
            payInInCents = 0L,
            payOutInCents = 0L,
            expectedCashInCents = 800000L,
            countedCashInCents = 800000L,
            cashVarianceInCents = 0L,
            status = "APPROVED"
        )

        val bytes = EscPosReceiptFactory.createZReport(
            org = testOrg,
            zReport = zReport,
            paperWidthMm = 80,
            branch = testBranch,
            register = testRegister,
            isReprint = true,
            reprintedBy = "Manager Sandesh"
        )

        val text = String(bytes, Charsets.ISO_8859_1)
        assertTrue("Reprint banner should be prominent", text.contains("*** Z-REPORT REPRINT ***"))
        assertTrue("Reprinted by user should be printed", text.contains("Manager Sandesh"))
    }

    @Test
    fun `test receipt duplicate reprint watermark and drawer suppression`() {
        val sale = SaleEntity(
            id = "sale_reprint_test",
            orgId = "org_test",
            branchId = "br_main_01",
            registerId = "reg_01",
            invoiceNo = "INV-2026-0001",
            fiscalYear = "2082/83",
            subtotalInCents = 100000L,
            taxInCents = 13000L,
            discountInCents = 0L,
            grandTotalInCents = 113000L,
            paymentStatus = "COMPLETED",
            createdAt = System.currentTimeMillis(),
            createdBy = "Staff A"
        )

        val items = listOf(
            SaleItemEntity(
                id = "item_1",
                saleId = sale.id,
                productId = "prod_1",
                productName = "Himalayan Coffee 250g",
                unitPriceInCents = 100000L,
                quantity = 1,
                lineTotalInCents = 100000L,
                taxInCents = 13000L,
                discountInCents = 0L
            )
        )

        val payments = listOf(
            SalePaymentEntity(
                id = "pay_1",
                saleId = sale.id,
                paymentType = PaymentType.CASH,
                amountInCents = 113000L,
                changeGivenInCents = 0L
            )
        )

        // 1. First-time original sale print (Cash drawer SHOULD kick)
        val origBytes = EscPosReceiptFactory.createReceipt(
            org = testOrg,
            sale = sale,
            items = items,
            payments = payments,
            paperWidthMm = 80,
            triggerCashDrawer = true,
            isReprint = false
        )

        var foundOrigDrawerPulse = false
        for (i in 0 until origBytes.size - 4) {
            if (origBytes[i] == EscPosCommandBuilder.ESC &&
                origBytes[i + 1] == 0x70.toByte() &&
                origBytes[i + 2] == 0x00.toByte()
            ) {
                foundOrigDrawerPulse = true
                break
            }
        }
        assertTrue("Cash drawer pulse should be present on original cash sale", foundOrigDrawerPulse)

        // 2. Duplicate / Reprint print (Cash drawer MUST NOT kick, reprint banner MUST be present)
        val reprintBytes = EscPosReceiptFactory.createReceipt(
            org = testOrg,
            sale = sale,
            items = items,
            payments = payments,
            paperWidthMm = 80,
            triggerCashDrawer = true, // even if trigger requested, reprint blocks it
            isReprint = true,
            reprintCount = 2
        )

        val reprintText = String(reprintBytes, Charsets.ISO_8859_1)
        assertTrue(reprintText.contains("*** DUPLICATE / REPRINT ***"))
        assertTrue(reprintText.contains("Reprint #:"))
        assertTrue(reprintText.contains("2"))

        var foundReprintDrawerPulse = false
        for (i in 0 until reprintBytes.size - 4) {
            if (reprintBytes[i] == EscPosCommandBuilder.ESC &&
                reprintBytes[i + 1] == 0x70.toByte() &&
                reprintBytes[i + 2] == 0x00.toByte()
            ) {
                foundReprintDrawerPulse = true
                break
            }
        }
        assertFalse("Cash drawer pulse MUST NOT be emitted on receipt reprints", foundReprintDrawerPulse)
    }

    @Test
    fun `test plain text formatter mirrors z-report snapshot without alteration`() {
        val zReport = ZReport(
            shiftId = "shift_text_fmt_01",
            cashierName = "Pooja Gurung",
            openedAt = 1774000000000L,
            closedAt = 1774028800000L,
            openingFloatInCents = 100000L,
            totalSalesCount = 8,
            totalSalesAmountInCents = 800000L,
            cashSalesInCents = 500000L,
            cardSalesInCents = 300000L,
            qrSalesInCents = 0L,
            creditSalesInCents = 0L,
            payInInCents = 0L,
            payOutInCents = 0L,
            expectedCashInCents = 600000L,
            countedCashInCents = 600000L,
            cashVarianceInCents = 0L,
            status = "APPROVED"
        )

        val text58 = ReceiptFormatter.formatZReport(
            org = testOrg,
            zReport = zReport,
            branch = testBranch,
            register = testRegister,
            isReprint = true,
            reprintCount = 1,
            widthCols = 32
        )

        assertTrue(text58.contains("REGISTER CLOSING Z-REPORT"))
        assertTrue(text58.contains("*** Z-REPORT REPRINT ***"))
        assertTrue(text58.contains("Pooja Gurung"))
        assertTrue(text58.contains("[BALANCED]"))
        assertTrue(text58.contains("APPROVED"))
        assertTrue(text58.contains("*** END OF Z-REPORT ***"))
    }
}
