package com.example.pos.domain

import com.example.pos.data.local.BranchEntity
import com.example.pos.data.local.OrganizationEntity
import com.example.pos.data.local.RegisterEntity
import com.example.pos.data.local.SaleEntity
import com.example.pos.data.local.SaleItemEntity
import com.example.pos.data.local.SalePaymentEntity
import com.example.pos.data.model.PaymentType
import com.example.pos.data.model.ZReport
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Universal ESC/POS Command Builder for Thermal Receipt Printers.
 * Supports 58mm (32 columns) and 80mm (48 columns) roll formats.
 */
class EscPosCommandBuilder(
    val paperWidthMm: Int = 80,
    private val charset: Charset = Charset.forName("CP437")
) {
    private val buffer = ByteArrayOutputStream()
    val columns: Int = if (paperWidthMm <= 58) 32 else 48

    companion object {
        // ESC/POS Command Constants
        val ESC: Byte = 0x1B
        val GS: Byte = 0x1D
        val LF: Byte = 0x0A
        val NUL: Byte = 0x00

        // Alignment Constants
        const val ALIGN_LEFT: Byte = 0
        const val ALIGN_CENTER: Byte = 1
        const val ALIGN_RIGHT: Byte = 2

        // Text Size Modes
        const val SIZE_NORMAL: Byte = 0x00
        const val SIZE_DOUBLE_HEIGHT: Byte = 0x01
        const val SIZE_DOUBLE_WIDTH: Byte = 0x10
        const val SIZE_DOUBLE: Byte = 0x11
        const val SIZE_QUAD: Byte = 0x22
    }

    init {
        initPrinter()
    }

    fun initPrinter(): EscPosCommandBuilder {
        buffer.write(byteArrayOf(ESC, 0x40)) // ESC @
        return this
    }

    fun align(alignMode: Byte): EscPosCommandBuilder {
        buffer.write(byteArrayOf(ESC, 0x61, alignMode)) // ESC a n
        return this
    }

    fun alignLeft() = align(ALIGN_LEFT)
    fun alignCenter() = align(ALIGN_CENTER)
    fun alignRight() = align(ALIGN_RIGHT)

    fun bold(enable: Boolean): EscPosCommandBuilder {
        buffer.write(byteArrayOf(ESC, 0x45, if (enable) 1 else 0)) // ESC E n
        return this
    }

    fun underline(enable: Boolean): EscPosCommandBuilder {
        buffer.write(byteArrayOf(ESC, 0x2D, if (enable) 1 else 0)) // ESC - n
        return this
    }

    fun textSize(sizeMode: Byte): EscPosCommandBuilder {
        buffer.write(byteArrayOf(GS, 0x21, sizeMode)) // GS ! n
        return this
    }

    fun text(str: String): EscPosCommandBuilder {
        val sanitized = sanitizeForThermal(str)
        buffer.write(sanitized.toByteArray(charset))
        return this
    }

    fun textLine(str: String = ""): EscPosCommandBuilder {
        if (str.isNotEmpty()) {
            text(str)
        }
        feed(1)
        return this
    }

    fun feed(lines: Int = 1): EscPosCommandBuilder {
        repeat(lines) {
            buffer.write(byteArrayOf(LF))
        }
        return this
    }

    fun divider(char: Char = '-'): EscPosCommandBuilder {
        val line = char.toString().repeat(columns)
        return textLine(line)
    }

    fun doubleDivider(): EscPosCommandBuilder = divider('=')

    /**
     * Prints a row with left-aligned and right-aligned text.
     */
    fun row(left: String, right: String): EscPosCommandBuilder {
        val cleanLeft = sanitizeForThermal(left)
        val cleanRight = sanitizeForThermal(right)
        val spaceNeeded = columns - cleanLeft.length - cleanRight.length
        val rowString = if (spaceNeeded >= 0) {
            cleanLeft + " ".repeat(spaceNeeded) + cleanRight
        } else {
            val maxLeft = (columns - cleanRight.length - 1).coerceAtLeast(1)
            cleanLeft.take(maxLeft) + " " + cleanRight
        }
        return textLine(rowString)
    }

    /**
     * Prints a 3-column table row for items (Name/Desc, Qty x Price, Total).
     */
    fun itemRow(name: String, qtyPrice: String, total: String): EscPosCommandBuilder {
        textLine(name)
        val indentedDetail = "  $qtyPrice"
        val spaceNeeded = columns - indentedDetail.length - total.length
        val detailLine = if (spaceNeeded >= 0) {
            indentedDetail + " ".repeat(spaceNeeded) + total
        } else {
            indentedDetail + " " + total
        }
        return textLine(detailLine)
    }

    /**
     * Native ESC/POS 1D Barcode (Code128).
     */
    fun barcode128(data: String, height: Int = 60): EscPosCommandBuilder {
        val cleanData = data.filter { it in ' '..'~' }
        if (cleanData.isEmpty()) return this

        alignCenter()
        // Barcode height: GS h n
        buffer.write(byteArrayOf(GS, 0x68, height.coerceIn(20, 200).toByte()))
        // Barcode width: GS w n (2 = medium)
        buffer.write(byteArrayOf(GS, 0x77, 2))
        // Barcode HRI characters position: GS H 2 (Below barcode)
        buffer.write(byteArrayOf(GS, 0x48, 2))
        // Barcode font: GS f 0
        buffer.write(byteArrayOf(GS, 0x66, 0))

        // GS k 73 (Code128) len data
        val bytes = cleanData.toByteArray(charset)
        buffer.write(byteArrayOf(GS, 0x6B, 73, bytes.size.toByte()))
        buffer.write(bytes)
        feed(1)
        alignLeft()
        return this
    }

    /**
     * Native ESC/POS 2D QR Code (Model 2).
     */
    fun qrCode(data: String, moduleSize: Int = 5): EscPosCommandBuilder {
        val bytes = data.toByteArray(Charset.forName("UTF-8"))
        val length = bytes.size + 3
        val pL = (length % 256).toByte()
        val pH = (length / 256).toByte()

        alignCenter()
        // 1. QR Code Model 2: GS ( k 4 0 49 65 50 0
        buffer.write(byteArrayOf(GS, 0x28, 0x6B, 4, 0, 49, 65, 50, 0))
        // 2. Module Size: GS ( k 3 0 49 67 size
        buffer.write(byteArrayOf(GS, 0x28, 0x6B, 3, 0, 49, 67, moduleSize.coerceIn(3, 8).toByte()))
        // 3. Error Correction Level M: GS ( k 3 0 49 69 49
        buffer.write(byteArrayOf(GS, 0x28, 0x6B, 3, 0, 49, 69, 49))
        // 4. Store Data in QR Buffer: GS ( k pL pH 49 80 48 <data>
        buffer.write(byteArrayOf(GS, 0x28, 0x6B, pL, pH, 49, 80, 48))
        buffer.write(bytes)
        // 5. Print QR Code from Buffer: GS ( k 3 0 49 81 48
        buffer.write(byteArrayOf(GS, 0x28, 0x6B, 3, 0, 49, 81, 48))
        feed(1)
        alignLeft()
        return this
    }

    /**
     * Pulse pin 2 or pin 5 to pop open standard RJ11/RJ12 Cash Drawer.
     */
    fun openCashDrawer(pin: Int = 0): EscPosCommandBuilder {
        // ESC p m t1 t2 (m=0: pin 2, m=1: pin 5)
        val m = if (pin == 1) 1.toByte() else 0.toByte()
        buffer.write(byteArrayOf(ESC, 0x70, m, 25.toByte(), 250.toByte()))
        return this
    }

    /**
     * Cut paper (Partial cut with feed).
     */
    fun cutPaper(partial: Boolean = true): EscPosCommandBuilder {
        feed(3)
        // GS V m n (m=66: partial cut after feed)
        val cutMode = if (partial) 66.toByte() else 65.toByte()
        buffer.write(byteArrayOf(GS, 0x56, cutMode, 0x00))
        return this
    }

    /**
     * Returns the compiled byte array ready to transmit to printer.
     */
    fun toBytes(): ByteArray = buffer.toByteArray()

    /**
     * Sanitizes strings for thermal printers that may not support Devanagari or special unicode.
     * Converts Nepali currency symbol रु to Rs and transliterates common non-ASCII characters.
     */
    private fun sanitizeForThermal(input: String): String {
        return input
            .replace("रु", "Rs")
            .replace("€", "EUR ")
            .replace("£", "GBP ")
            .replace("¥", "JPY ")
            .replace("₹", "Rs")
            .replace("\u2018", "'")
            .replace("\u2019", "'")
            .replace("\u201C", "\"")
            .replace("\u201D", "\"")
            .replace("\u2013", "-")
            .replace("\u2014", "--")
    }
}

/**
 * Receipt Builder helper converting domain entities to complete ESC/POS jobs.
 */
object EscPosReceiptFactory {

    fun createReceipt(
        org: OrganizationEntity,
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        payments: List<SalePaymentEntity>,
        customerName: String? = null,
        branch: BranchEntity? = null,
        register: RegisterEntity? = null,
        paperWidthMm: Int = 80,
        triggerCashDrawer: Boolean = true,
        isReprint: Boolean = false,
        reprintCount: Int = 0,
        reprintedAt: Long = System.currentTimeMillis(),
        headerNote: String = "Thank you for shopping!",
        footerNote: String = "Please come again!"
    ): ByteArray {
        val b = EscPosCommandBuilder(paperWidthMm)
        val cur = org.currencySymbol.replace("रु", "Rs")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sale.createdAt))

        // Open Cash Drawer on sale ONLY if:
        // 1. triggerCashDrawer is true in store settings
        // 2. This is NOT a reprint
        // 3. Cash tender is present in the transaction
        val hasCashTender = payments.any { it.paymentType == PaymentType.CASH && it.amountInCents > 0L }
        if (triggerCashDrawer && !isReprint && hasCashTender) {
            b.openCashDrawer()
        }

        // Store Header (Centered, Bold, Double Size)
        b.alignCenter()
        b.bold(true)
        b.textSize(EscPosCommandBuilder.SIZE_DOUBLE)
        b.textLine(org.name.uppercase())

        b.textSize(EscPosCommandBuilder.SIZE_NORMAL)
        b.bold(false)
        if (org.taxId.isNotBlank()) {
            b.textLine("TAX / VAT ID: ${org.taxId}")
        }
        if (branch != null) {
            if (branch.name.isNotBlank()) b.textLine(branch.name)
            if (branch.address.isNotBlank()) b.textLine(branch.address)
            if (branch.phone.isNotBlank()) b.textLine("Phone: ${branch.phone}")
        } else {
            b.textLine("OFFLINE-FIRST MOBILE POS")
        }
        if (headerNote.isNotBlank()) {
            b.textLine(headerNote)
        }
        b.doubleDivider()

        // Reprint Banner (Prominent Watermark)
        if (isReprint) {
            b.alignCenter()
            b.bold(true)
            b.textSize(EscPosCommandBuilder.SIZE_DOUBLE_HEIGHT)
            b.textLine("*** DUPLICATE / REPRINT ***")
            b.textSize(EscPosCommandBuilder.SIZE_NORMAL)
            b.bold(false)
            b.alignLeft()
            b.row("Reprint #:", if (reprintCount > 0) "$reprintCount" else "1")
            b.row("Reprint Date:", dateFormat.format(Date(reprintedAt)))
            b.divider()
        }

        // Metadata
        b.alignLeft()
        b.row("Invoice #:", sale.invoiceNo)
        if (sale.fiscalYear.isNotBlank()) {
            b.row("Fiscal Year:", sale.fiscalYear)
        }
        b.row("Date:", dateStr)
        b.row("Cashier:", sale.createdBy.ifBlank { "Staff" })
        if (register != null) {
            b.row("Terminal:", "${register.name} (${register.code})")
        }
        if (!customerName.isNullOrBlank()) {
            b.row("Customer:", customerName)
        }
        b.divider()

        // Line Items Table Header
        b.bold(true)
        b.row("ITEM [QTY x PRICE]", "TOTAL")
        b.bold(false)
        b.divider()

        // Line Items
        for (item in items) {
            val qtyPrice = "${item.quantity} x ${CartCalculator.formatMoney(item.unitPriceInCents, cur)}"
            val lineTotal = CartCalculator.formatMoney(item.lineTotalInCents, cur)
            b.itemRow(item.productName, qtyPrice, lineTotal)
            if (item.discountInCents > 0) {
                b.row("  (Item Discount)", "-${CartCalculator.formatMoney(item.discountInCents, cur)}")
            }
        }
        b.divider()

        // Totals
        b.row("Subtotal:", CartCalculator.formatMoney(sale.subtotalInCents, cur))
        if (sale.discountInCents > 0) {
            b.row("Discount:", "-${CartCalculator.formatMoney(sale.discountInCents, cur)}")
        }
        if (sale.taxInCents > 0) {
            b.row("Tax (${org.taxMode.name}):", CartCalculator.formatMoney(sale.taxInCents, cur))
        }
        b.doubleDivider()

        // Grand Total (Bold Double Height)
        b.bold(true)
        b.textSize(EscPosCommandBuilder.SIZE_DOUBLE_HEIGHT)
        b.row("GRAND TOTAL:", CartCalculator.formatMoney(sale.grandTotalInCents, cur))
        b.textSize(EscPosCommandBuilder.SIZE_NORMAL)
        b.bold(false)
        b.doubleDivider()

        // Payment Tender Breakdown
        b.bold(true)
        b.textLine("PAYMENT DETAILS:")
        b.bold(false)
        for (payment in payments) {
            val pType = payment.paymentType.name.replace("_", " ")
            b.row("  $pType:", CartCalculator.formatMoney(payment.amountInCents, cur))
            if (payment.changeGivenInCents > 0) {
                b.row("  Change Returned:", CartCalculator.formatMoney(payment.changeGivenInCents, cur))
            }
            if (payment.referenceNo.isNotBlank()) {
                b.row("  Auth / Ref #:", payment.referenceNo)
            }
        }
        b.divider()

        // Footer & Notes
        b.alignCenter()
        if (footerNote.isNotBlank()) {
            b.textLine(footerNote)
        }
        b.textLine("Retain invoice for returns within 7 days")
        b.feed(1)

        // 1D Barcode of Invoice #
        b.barcode128(sale.invoiceNo, height = 50)
        b.textLine(sale.invoiceNo)
        b.feed(1)

        // Cut Paper
        b.cutPaper()
        return b.toBytes()
    }

    /**
     * Generates a complete, authoritative Z-Report from the finalized shift closing snapshot.
     * Never recalculates accounting values independently.
     */
    fun createZReport(
        org: OrganizationEntity,
        branch: BranchEntity? = null,
        register: RegisterEntity? = null,
        zReport: ZReport,
        paperWidthMm: Int = 80,
        isReprint: Boolean = false,
        reprintedAt: Long = System.currentTimeMillis(),
        reprintedBy: String? = null
    ): ByteArray {
        val b = EscPosCommandBuilder(paperWidthMm)
        val cur = org.currencySymbol.replace("रु", "Rs")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val shiftIdShort = zReport.shiftId.takeLast(8).uppercase()
        val reportRef = "ZRPT-$shiftIdShort-${zReport.closedAt.toString().takeLast(4)}"

        // Header (Store Branding)
        b.alignCenter()
        b.bold(true)
        b.textSize(EscPosCommandBuilder.SIZE_DOUBLE)
        b.textLine(org.name.uppercase())

        b.textSize(EscPosCommandBuilder.SIZE_NORMAL)
        b.bold(false)
        if (org.taxId.isNotBlank()) {
            b.textLine("TAX / PAN ID: ${org.taxId}")
        }
        if (branch != null) {
            if (branch.name.isNotBlank()) b.textLine(branch.name)
            if (branch.address.isNotBlank()) b.textLine(branch.address)
        }
        b.bold(true)
        b.textLine("REGISTER CLOSING Z-REPORT")
        b.bold(false)
        b.doubleDivider()

        // Reprint Banner (if reprinting past shift)
        if (isReprint) {
            b.alignCenter()
            b.bold(true)
            b.textSize(EscPosCommandBuilder.SIZE_DOUBLE_HEIGHT)
            b.textLine("*** Z-REPORT REPRINT ***")
            b.textSize(EscPosCommandBuilder.SIZE_NORMAL)
            b.bold(false)
            b.alignLeft()
            b.row("Reprint Time:", dateFormat.format(Date(reprintedAt)))
            if (!reprintedBy.isNullOrBlank()) {
                b.row("Reprinted By:", reprintedBy)
            }
            b.divider()
        }

        // Shift Identification Metadata
        b.alignLeft()
        b.row("Report Ref:", reportRef)
        b.row("Shift ID:", "#$shiftIdShort")
        if (branch != null) {
            b.row("Branch:", "${branch.name} (${branch.code})")
        }
        b.row("Terminal:", "${register?.name ?: "Register #1"} (${register?.code ?: "REG1"})")
        b.row("Cashier:", zReport.cashierName)
        b.row("Opened At:", dateFormat.format(Date(zReport.openedAt)))
        b.row("Closed At:", dateFormat.format(Date(zReport.closedAt)))
        b.divider()

        // 1. CASH RECONCILIATION
        b.bold(true)
        b.textLine("CASH RECONCILIATION:")
        b.bold(false)
        b.row("  Opening Float:", CartCalculator.formatMoney(zReport.openingFloatInCents, cur))
        b.row("  + Cash Sales:", CartCalculator.formatMoney(zReport.cashSalesInCents, cur))
        if (zReport.creditCollectionInCents > 0) {
            b.row("  + Credit Collections:", CartCalculator.formatMoney(zReport.creditCollectionInCents, cur))
        }
        if (zReport.payInInCents > 0) {
            b.row("  + Paid In (Till Top-Up):", "+${CartCalculator.formatMoney(zReport.payInInCents, cur)}")
        }
        if (zReport.payOutInCents > 0) {
            b.row("  - Paid Out (Expense):", "-${CartCalculator.formatMoney(zReport.payOutInCents, cur)}")
        }
        if (zReport.safeDropInCents > 0) {
            b.row("  - Safe Drops:", "-${CartCalculator.formatMoney(zReport.safeDropInCents, cur)}")
        }
        b.divider()

        b.bold(true)
        b.row("Expected Drawer Cash:", CartCalculator.formatMoney(zReport.expectedCashInCents, cur))
        b.row("Actual Counted Cash:", CartCalculator.formatMoney(zReport.countedCashInCents, cur))

        val variance = zReport.cashVarianceInCents
        val varLabel = when {
            variance == 0L -> "0.00 [BALANCED]"
            variance > 0L -> "+${CartCalculator.formatMoney(variance, cur)} [OVERAGE]"
            else -> "-${CartCalculator.formatMoney(Math.abs(variance), cur)} [SHORTAGE]"
        }
        b.row("Cash Variance:", varLabel)
        b.bold(false)

        if (zReport.varianceReason.isNotBlank()) {
            b.textLine("  Reason: ${zReport.varianceReason}")
        }
        b.divider()

        // 2. CASH HANDOVER & DEPOSIT
        b.bold(true)
        b.textLine("CASH HANDOVER & DEPOSIT:")
        b.bold(false)
        b.row("  Closing Float in Till:", CartCalculator.formatMoney(zReport.closingFloatInCents, cur))
        b.bold(true)
        b.row("  Deposit to Safe/Bank:", CartCalculator.formatMoney(zReport.depositAmountInCents, cur))
        b.bold(false)
        b.divider()

        // 3. SALES & TENDER BREAKDOWN
        b.bold(true)
        b.textLine("SALES BY TENDER:")
        b.bold(false)
        b.row("  Total Invoices:", zReport.totalSalesCount.toString())
        b.bold(true)
        b.row("  Gross Sales Total:", CartCalculator.formatMoney(zReport.totalSalesAmountInCents, cur))
        b.bold(false)
        b.row("    Cash Sales:", CartCalculator.formatMoney(zReport.cashSalesInCents, cur))
        b.row("    Card Sales:", CartCalculator.formatMoney(zReport.cardSalesInCents, cur))
        b.row("    QR / Online Sales:", CartCalculator.formatMoney(zReport.qrSalesInCents, cur))
        if (zReport.creditSalesInCents > 0) {
            b.row("    Credit / Khata Sales:", CartCalculator.formatMoney(zReport.creditSalesInCents, cur))
        }
        b.divider()

        // 4. ONLINE BILLS RECONCILIATION
        b.bold(true)
        b.textLine("ONLINE RECONCILIATION:")
        b.bold(false)
        b.row("  Expected (Card + QR):", CartCalculator.formatMoney(zReport.expectedOnlineInCents, cur))
        b.row("  Counted / Declared:", "${CartCalculator.formatMoney(zReport.countedOnlineInCents, cur)} (${zReport.onlineBillsCount} slips)")
        val onlineVar = zReport.onlineVarianceInCents
        val onlineVarLabel = when {
            onlineVar == 0L -> "0.00 [BALANCED]"
            onlineVar > 0L -> "+${CartCalculator.formatMoney(onlineVar, cur)} [OVERAGE]"
            else -> "-${CartCalculator.formatMoney(Math.abs(onlineVar), cur)} [SHORTAGE]"
        }
        b.row("  Online Variance:", onlineVarLabel)
        b.divider()

        // 5. AUDIT STATUS
        b.bold(true)
        b.row("Audit Review Status:", zReport.status.uppercase())
        b.bold(false)
        b.doubleDivider()

        // Footer Barcode of Shift Reference
        b.alignCenter()
        b.barcode128(shiftIdShort, height = 45)
        b.textLine(reportRef)
        b.feed(1)
        b.textLine("*** END OF OFFICIAL Z-REPORT ***")
        b.doubleDivider()

        b.cutPaper()
        return b.toBytes()
    }

    fun createTestPage(
        storeName: String,
        paperWidthMm: Int = 80,
        triggerCashDrawer: Boolean = false
    ): ByteArray {
        val b = EscPosCommandBuilder(paperWidthMm)
        if (triggerCashDrawer) {
            b.openCashDrawer()
        }

        b.alignCenter()
        b.bold(true)
        b.textSize(EscPosCommandBuilder.SIZE_DOUBLE)
        b.textLine("PRINTER TEST")
        b.textSize(EscPosCommandBuilder.SIZE_NORMAL)
        b.bold(false)
        b.textLine(storeName)
        b.textLine("OmniPOS ESC/POS Diagnostic Slip")
        b.doubleDivider()

        b.alignLeft()
        b.row("Paper Width:", "${paperWidthMm}mm (${b.columns} cols)")
        b.row("Connection:", "Bluetooth / USB Verified")
        b.row("Timestamp:", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        b.divider()

        b.textLine("Alignment & Style Check:")
        b.alignLeft()
        b.textLine("Left Align Normal")
        b.bold(true)
        b.textLine("Left Align Bold")
        b.bold(false)

        b.alignCenter()
        b.textLine("Centered Text")

        b.alignRight()
        b.textLine("Right Align Text")
        b.alignLeft()
        b.divider()

        b.alignCenter()
        b.textLine("Barcode 128 Test:")
        b.barcode128("TEST-123456", height = 45)
        b.textLine("TEST-123456")
        b.feed(1)

        b.textLine("2D QR Code Test:")
        b.qrCode("OmniPOS-Thermal-OK", moduleSize = 4)
        b.feed(1)

        b.textLine("Thermal Head & Cutter OK")
        b.doubleDivider()
        b.cutPaper()

        return b.toBytes()
    }
}
