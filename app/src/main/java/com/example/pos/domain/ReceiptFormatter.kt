package com.example.pos.domain

import com.example.pos.data.local.BranchEntity
import com.example.pos.data.local.OrganizationEntity
import com.example.pos.data.local.RegisterEntity
import com.example.pos.data.local.SaleEntity
import com.example.pos.data.local.SaleItemEntity
import com.example.pos.data.local.SalePaymentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptFormatter {

    fun formatThermalReceipt(
        org: OrganizationEntity,
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        payments: List<SalePaymentEntity>,
        customerName: String? = null,
        branch: BranchEntity? = null,
        register: RegisterEntity? = null,
        isReprint: Boolean = false,
        reprintCount: Int = 1,
        reprintedAt: Long = System.currentTimeMillis(),
        widthCols: Int = 32, // 32 for 58mm, 48 for 80mm
    ): String {
        val sb = StringBuilder()
        val sep = "-".repeat(widthCols)
        val dSep = "=".repeat(widthCols)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sale.createdAt))
        val cur = org.currencySymbol

        fun center(text: String): String {
            if (text.length >= widthCols) return text.take(widthCols)
            val padding = (widthCols - text.length) / 2
            return " ".repeat(padding) + text
        }

        fun row(left: String, right: String): String {
            val spaceNeeded = widthCols - left.length - right.length
            return if (spaceNeeded > 0) {
                left + " ".repeat(spaceNeeded) + right
            } else {
                left.take(widthCols - right.length - 1) + " " + right
            }
        }

        if (isReprint) {
            sb.appendLine(dSep)
            sb.appendLine(center("*** DUPLICATE / REPRINT ***"))
            val reprintDate = dateFormat.format(Date(reprintedAt))
            sb.appendLine(center("Reprint #$reprintCount at $reprintDate"))
            sb.appendLine(dSep)
        }

        sb.appendLine(dSep)
        sb.appendLine(center(org.name.uppercase()))
        if (branch != null) {
            sb.appendLine(center("${branch.name} (${branch.code})"))
            if (branch.address.isNotBlank()) {
                sb.appendLine(center(branch.address))
            }
        }
        if (org.taxId.isNotBlank()) {
            sb.appendLine(center("PAN / VAT: ${org.taxId}"))
        }
        sb.appendLine(center("OFFLINE-FIRST MOBILE POS"))
        sb.appendLine(dSep)
        sb.appendLine(row("Invoice #:", sale.invoiceNo))
        if (sale.fiscalYear.isNotBlank()) {
            sb.appendLine(row("Fiscal Year:", sale.fiscalYear))
        }
        sb.appendLine(row("Date:", dateStr))
        if (register != null) {
            sb.appendLine(row("Register:", "${register.name} (${register.code})"))
        }
        sb.appendLine(row("Cashier:", sale.createdBy.ifBlank { "Staff" }))
        if (!customerName.isNullOrBlank()) {
            sb.appendLine(row("Customer:", customerName))
        }
        sb.appendLine(sep)
        sb.appendLine(row("ITEM [QTY x PRICE]", "TOTAL"))
        sb.appendLine(sep)

        for (item in items) {
            val nameLine = item.productName
            val qtyPrice = "${item.quantity} x ${CartCalculator.formatMoney(item.unitPriceInCents, cur)}"
            val lineTot = CartCalculator.formatMoney(item.lineTotalInCents, cur)
            sb.appendLine(nameLine)
            sb.appendLine(row("  $qtyPrice", lineTot))
            if (item.discountInCents > 0) {
                sb.appendLine(row("  (Item Discount)", "-${CartCalculator.formatMoney(item.discountInCents, cur)}"))
            }
        }

        sb.appendLine(sep)
        sb.appendLine(row("SUBTOTAL:", CartCalculator.formatMoney(sale.subtotalInCents, cur)))
        if (sale.discountInCents > 0) {
            sb.appendLine(row("DISCOUNT:", "-${CartCalculator.formatMoney(sale.discountInCents, cur)}"))
        }
        if (sale.taxInCents > 0) {
            sb.appendLine(row("TAX (${org.taxMode}):", CartCalculator.formatMoney(sale.taxInCents, cur)))
        }
        sb.appendLine(dSep)
        sb.appendLine(row("GRAND TOTAL:", CartCalculator.formatMoney(sale.grandTotalInCents, cur)))
        sb.appendLine(dSep)

        sb.appendLine("PAYMENT DETAILS:")
        for (payment in payments) {
            val pType = payment.paymentType.name.replace("_", " ")
            sb.appendLine(row("  $pType:", CartCalculator.formatMoney(payment.amountInCents, cur)))
            if (payment.changeGivenInCents > 0) {
                sb.appendLine(row("  Change Returned:", CartCalculator.formatMoney(payment.changeGivenInCents, cur)))
            }
            if (payment.referenceNo.isNotBlank()) {
                sb.appendLine(row("  Ref / Auth:", payment.referenceNo))
            }
        }

        sb.appendLine(sep)
        sb.appendLine(center("Thank You For Your Business!"))
        sb.appendLine(center("Please Retain For Returns (7 Days)"))
        sb.appendLine(dSep)

        return sb.toString()
    }

    fun formatSalesSummaryReport(
        org: OrganizationEntity,
        periodTitle: String,
        totalRevenue: Long,
        totalOrders: Int,
        avgOrderValue: Long,
        totalTax: Long,
        totalDiscounts: Long,
        grossProfit: Long,
        tenderBreakdown: Map<String, Long>,
        topConsumables: List<Triple<String, Int, Long>>, // (name, units, revenue)
        lineVoidsCount: Int = 0,
        staffName: String = "All Staff",
        widthCols: Int = 32,
        toReceiveTotal: Long = 0L,
        toGiveTotal: Long = 0L
    ): String {
        val sb = StringBuilder()
        val sep = "-".repeat(widthCols)
        val dSep = "=".repeat(widthCols)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        val cur = org.currencySymbol

        fun center(text: String): String {
            if (text.length >= widthCols) return text.take(widthCols)
            val padding = (widthCols - text.length) / 2
            return " ".repeat(padding) + text
        }

        fun row(left: String, right: String): String {
            val spaceNeeded = widthCols - left.length - right.length
            return if (spaceNeeded > 0) {
                left + " ".repeat(spaceNeeded) + right
            } else {
                left.take(widthCols - right.length - 1) + " " + right
            }
        }

        sb.appendLine(dSep)
        sb.appendLine(center(org.name.uppercase()))
        sb.appendLine(center("SALES & SUMMARY REPORT"))
        sb.appendLine(dSep)
        sb.appendLine(row("Period:", periodTitle))
        sb.appendLine(row("Generated:", dateStr))
        sb.appendLine(row("Cashier/Staff:", staffName))
        sb.appendLine(sep)
        sb.appendLine(row("Gross Revenue:", CartCalculator.formatMoney(totalRevenue, cur)))
        sb.appendLine(row("Invoices Count:", totalOrders.toString()))
        sb.appendLine(row("Avg Order Value:", CartCalculator.formatMoney(avgOrderValue, cur)))
        sb.appendLine(row("Discounts Given:", CartCalculator.formatMoney(totalDiscounts, cur)))
        sb.appendLine(row("Tax/VAT Collected:", CartCalculator.formatMoney(totalTax, cur)))
        sb.appendLine(row("Est. Gross Profit:", CartCalculator.formatMoney(grossProfit, cur)))
        sb.appendLine(sep)
        sb.appendLine("PAYMENT TENDER SUMMARY:")
        for ((method, amount) in tenderBreakdown) {
            sb.appendLine(row("  $method:", CartCalculator.formatMoney(amount, cur)))
        }

        if (toReceiveTotal > 0L || toGiveTotal > 0L) {
            sb.appendLine(sep)
            sb.appendLine("CUSTOMER KHATA SUMMARY:")
            sb.appendLine(row("  To Receive (Lin Baki):", CartCalculator.formatMoney(toReceiveTotal, cur)))
            sb.appendLine(row("  To Give (Dina Baki):", CartCalculator.formatMoney(toGiveTotal, cur)))
            val net = toReceiveTotal - toGiveTotal
            sb.appendLine(row("  Net Receivable:", CartCalculator.formatMoney(net, cur)))
        }

        if (topConsumables.isNotEmpty()) {
            sb.appendLine(sep)
            sb.appendLine("TOP CONSUMED PRODUCTS:")
            topConsumables.take(6).forEachIndexed { index, (name, units, rev) ->
                val lineLabel = "${index + 1}. $name"
                sb.appendLine(lineLabel.take(widthCols))
                sb.appendLine(row("   $units units", CartCalculator.formatMoney(rev, cur)))
            }
        }
        sb.appendLine(sep)
        sb.appendLine("LOSS PREVENTION & AUDIT:")
        sb.appendLine(row("  Line Voids Count:", lineVoidsCount.toString()))
        sb.appendLine(dSep)
        sb.appendLine(center("*** END OF SUMMARY REPORT ***"))
        sb.appendLine(dSep)

        return sb.toString()
    }

    fun formatZReport(
        org: OrganizationEntity,
        zReport: com.example.pos.data.model.ZReport,
        branch: BranchEntity? = null,
        register: RegisterEntity? = null,
        isReprint: Boolean = false,
        reprintCount: Int = 1,
        reprintedAt: Long = System.currentTimeMillis(),
        widthCols: Int = 32
    ): String {
        val sb = StringBuilder()
        val sep = "-".repeat(widthCols)
        val dSep = "=".repeat(widthCols)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val cur = org.currencySymbol
        val shiftIdShort = zReport.shiftId.takeLast(6).uppercase()
        val reportRef = "Z-SHIFT-$shiftIdShort"

        fun center(text: String): String {
            if (text.length >= widthCols) return text.take(widthCols)
            val padding = (widthCols - text.length) / 2
            return " ".repeat(padding) + text
        }

        fun row(left: String, right: String): String {
            val spaceNeeded = widthCols - left.length - right.length
            return if (spaceNeeded > 0) {
                left + " ".repeat(spaceNeeded) + right
            } else {
                left.take(widthCols - right.length - 1) + " " + right
            }
        }

        sb.appendLine(dSep)
        sb.appendLine(center(org.name.uppercase()))
        if (branch != null) {
            sb.appendLine(center("${branch.name} (${branch.code})"))
            if (branch.address.isNotBlank()) {
                sb.appendLine(center(branch.address))
            }
        }
        if (org.taxId.isNotBlank()) {
            sb.appendLine(center("PAN / VAT: ${org.taxId}"))
        }
        sb.appendLine(center("REGISTER CLOSING Z-REPORT"))
        sb.appendLine(dSep)

        if (isReprint) {
            sb.appendLine(center("*** Z-REPORT REPRINT ***"))
            val reprintDate = dateFormat.format(Date(reprintedAt))
            sb.appendLine(center("Reprint #$reprintCount at $reprintDate"))
            sb.appendLine(dSep)
        }

        sb.appendLine(row("Report Ref:", reportRef))
        sb.appendLine(row("Shift ID:", "#$shiftIdShort"))
        if (branch != null) {
            sb.appendLine(row("Branch:", "${branch.name} (${branch.code})"))
        }
        sb.appendLine(row("Terminal:", "${register?.name ?: "Register #1"} (${register?.code ?: "REG1"})"))
        sb.appendLine(row("Cashier:", zReport.cashierName))
        sb.appendLine(row("Opened At:", dateFormat.format(Date(zReport.openedAt))))
        sb.appendLine(row("Closed At:", dateFormat.format(Date(zReport.closedAt))))
        sb.appendLine(sep)

        // 1. CASH RECONCILIATION
        sb.appendLine("CASH RECONCILIATION:")
        sb.appendLine(row("  Opening Float:", CartCalculator.formatMoney(zReport.openingFloatInCents, cur)))
        sb.appendLine(row("  + Cash Sales:", CartCalculator.formatMoney(zReport.cashSalesInCents, cur)))
        if (zReport.creditCollectionInCents > 0) {
            sb.appendLine(row("  + Credit Collections:", CartCalculator.formatMoney(zReport.creditCollectionInCents, cur)))
        }
        if (zReport.payInInCents > 0) {
            sb.appendLine(row("  + Paid In (Till Top-Up):", "+${CartCalculator.formatMoney(zReport.payInInCents, cur)}"))
        }
        if (zReport.payOutInCents > 0) {
            sb.appendLine(row("  - Paid Out (Expense):", "-${CartCalculator.formatMoney(zReport.payOutInCents, cur)}"))
        }
        if (zReport.safeDropInCents > 0) {
            sb.appendLine(row("  - Safe Drops:", "-${CartCalculator.formatMoney(zReport.safeDropInCents, cur)}"))
        }
        sb.appendLine(sep)

        sb.appendLine(row("Expected Drawer Cash:", CartCalculator.formatMoney(zReport.expectedCashInCents, cur)))
        sb.appendLine(row("Actual Counted Cash:", CartCalculator.formatMoney(zReport.countedCashInCents, cur)))

        val variance = zReport.cashVarianceInCents
        val varLabel = when {
            variance == 0L -> "0.00 [BALANCED]"
            variance > 0L -> "+${CartCalculator.formatMoney(variance, cur)} [OVERAGE]"
            else -> "-${CartCalculator.formatMoney(Math.abs(variance), cur)} [SHORTAGE]"
        }
        sb.appendLine(row("Cash Variance:", varLabel))
        if (zReport.varianceReason.isNotBlank()) {
            sb.appendLine("  Reason: ${zReport.varianceReason}")
        }
        sb.appendLine(sep)

        // 2. CASH HANDOVER & DEPOSIT
        sb.appendLine("CASH HANDOVER & DEPOSIT:")
        sb.appendLine(row("  Closing Float in Till:", CartCalculator.formatMoney(zReport.closingFloatInCents, cur)))
        sb.appendLine(row("  Deposit to Safe/Bank:", CartCalculator.formatMoney(zReport.depositAmountInCents, cur)))
        sb.appendLine(sep)

        // 3. SALES & TENDER BREAKDOWN
        sb.appendLine("SALES BY TENDER:")
        sb.appendLine(row("  Total Invoices:", zReport.totalSalesCount.toString()))
        sb.appendLine(row("  Gross Sales Total:", CartCalculator.formatMoney(zReport.totalSalesAmountInCents, cur)))
        sb.appendLine(row("    Cash Sales:", CartCalculator.formatMoney(zReport.cashSalesInCents, cur)))
        sb.appendLine(row("    Card Sales:", CartCalculator.formatMoney(zReport.cardSalesInCents, cur)))
        sb.appendLine(row("    QR / Online Sales:", CartCalculator.formatMoney(zReport.qrSalesInCents, cur)))
        if (zReport.creditSalesInCents > 0) {
            sb.appendLine(row("    Credit / Khata Sales:", CartCalculator.formatMoney(zReport.creditSalesInCents, cur)))
        }
        sb.appendLine(sep)

        // 4. ONLINE BILLS RECONCILIATION
        sb.appendLine("ONLINE RECONCILIATION:")
        sb.appendLine(row("  Expected (Card + QR):", CartCalculator.formatMoney(zReport.expectedOnlineInCents, cur)))
        sb.appendLine(row("  Counted / Declared:", "${CartCalculator.formatMoney(zReport.countedOnlineInCents, cur)} (${zReport.onlineBillsCount} slips)"))
        val onlineVar = zReport.onlineVarianceInCents
        val onlineVarLabel = when {
            onlineVar == 0L -> "0.00 [BALANCED]"
            onlineVar > 0L -> "+${CartCalculator.formatMoney(onlineVar, cur)} [OVERAGE]"
            else -> "-${CartCalculator.formatMoney(Math.abs(onlineVar), cur)} [SHORTAGE]"
        }
        sb.appendLine(row("  Online Variance:", onlineVarLabel))
        sb.appendLine(sep)

        // 5. AUDIT STATUS
        sb.appendLine(row("Audit Review Status:", zReport.status.uppercase()))
        sb.appendLine(dSep)
        sb.appendLine(center("*** END OF Z-REPORT ***"))
        sb.appendLine(dSep)

        return sb.toString()
    }
}
