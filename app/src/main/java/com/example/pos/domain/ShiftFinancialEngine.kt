package com.example.pos.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Pure financial calculations engine for Cashier Shift Management.
 * All internal money calculations use Long (cents) to guarantee zero floating-point drift.
 */
object ShiftFinancialEngine {

    const val TOLERANCE_VARIANCE_CENTS = 5000L // Rs 50.00
    const val HARD_BLOCK_VARIANCE_CENTS = 50000L // Rs 500.00

    enum class VarianceStatus {
        UNDER_TOLERANCE,
        WARN_REASON_REQUIRED,
        HARD_BLOCK_SUPERVISOR_REQUIRED
    }

    data class DenominationItem(
        val denomination: Int,
        val count: Int
    ) {
        val subtotalInCents: Long get() = denomination.toLong() * count.coerceAtLeast(0) * 100L
    }

    data class ExpectedCashBreakdown(
        val openingCashInCents: Long,
        val cashSalesInCents: Long,
        val cashCreditCollectionsInCents: Long,
        val paidInInCents: Long,
        val cashRefundsInCents: Long, // Note: ONLY cash refunds reduce drawer cash
        val paidOutInCents: Long,
        val safeDropsInCents: Long
    ) {
        val expectedCashInCents: Long
            get() = openingCashInCents + cashSalesInCents + cashCreditCollectionsInCents +
                    paidInInCents - cashRefundsInCents - paidOutInCents - safeDropsInCents
    }

    /**
     * Expected Cash = Opening Cash + Cash Sales + Cash Credit Collections + Paid In - Cash Refunds - Paid Out - Safe Drops
     * Note: Credit notes issued do NOT reduce cash. Only the cash refund portion reduces cash.
     */
    fun calculateExpectedCash(
        openingFloatInCents: Long,
        cashSalesInCents: Long,
        cashCreditCollectionsInCents: Long = 0L,
        paidInInCents: Long = 0L,
        cashRefundsInCents: Long = 0L,
        paidOutInCents: Long = 0L,
        safeDropsInCents: Long = 0L
    ): Long {
        return openingFloatInCents + cashSalesInCents + cashCreditCollectionsInCents +
                paidInInCents - cashRefundsInCents - paidOutInCents - safeDropsInCents
    }

    /**
     * Compute grand total strictly from denomination counts.
     */
    fun calculateDenominationTotalInCents(items: List<DenominationItem>): Long {
        return items.sumOf { it.subtotalInCents }
    }

    fun calculateTotalCash(denominations: List<Pair<Int, Int>>): Long {
        return denominations.sumOf { (denom, count) ->
            denom.toLong() * count.coerceAtLeast(0) * 100L
        }
    }

    /**
     * Cash Variance = Counted Cash - Expected Cash (+ over, - short)
     */
    fun calculateCashVariance(countedCashInCents: Long, expectedCashInCents: Long): Long {
        return countedCashInCents - expectedCashInCents
    }

    /**
     * Checks variance threshold:
     * <= 50 -> UNDER_TOLERANCE
     * > 50 and <= 500 -> WARN_REASON_REQUIRED
     * > 500 -> HARD_BLOCK_SUPERVISOR_REQUIRED
     */
    fun checkVarianceThreshold(varianceInCents: Long): VarianceStatus {
        val absVar = abs(varianceInCents)
        return when {
            absVar <= TOLERANCE_VARIANCE_CENTS -> VarianceStatus.UNDER_TOLERANCE
            absVar <= HARD_BLOCK_VARIANCE_CENTS -> VarianceStatus.WARN_REASON_REQUIRED
            else -> VarianceStatus.HARD_BLOCK_SUPERVISOR_REQUIRED
        }
    }

    /**
     * Net Cash Change = Closing Cash - Opening Cash
     */
    fun calculateNetCashChange(closingCashInCents: Long, openingCashInCents: Long): Long {
        return closingCashInCents - openingCashInCents
    }

    /**
     * Opening Gap = Declared Opening - Previous Closing Float
     */
    fun calculateOpeningGap(declaredOpeningInCents: Long, previousClosingFloatInCents: Long): Long {
        return declaredOpeningInCents - previousClosingFloatInCents
    }

    /**
     * Online Variance = POS Recorded - Provider Confirmed
     */
    fun calculateOnlineVariance(posAmountInCents: Long, providerAmountInCents: Long): Long {
        return posAmountInCents - providerAmountInCents
    }

    /**
     * Deposit Amount = Counted Cash - Closing Float
     */
    fun calculateDepositAmount(countedCashInCents: Long, closingFloatInCents: Long): Long {
        return (countedCashInCents - closingFloatInCents).coerceAtLeast(0L)
    }

    /**
     * Formats amount in NPR with Nepali digit grouping (e.g. 1,00,000.00)
     */
    fun formatNepaliGrouping(amountInCents: Long, currencySymbol: String = "Rs"): String {
        val isNegative = amountInCents < 0
        val absCents = abs(amountInCents)
        val rupees = absCents / 100
        val paisa = absCents % 100

        val rupeeStr = rupees.toString()
        val len = rupeeStr.length

        val groupedRupees = if (len <= 3) {
            rupeeStr
        } else {
            val lastThree = rupeeStr.substring(len - 3)
            val remaining = rupeeStr.substring(0, len - 3)
            val regex = "(\\d)(?=(\\d{2})+$)".toRegex()
            val formattedRemaining = remaining.replace(regex, "$1,")
            "$formattedRemaining,$lastThree"
        }

        val prefix = if (isNegative) "-" else ""
        val formattedPaisa = String.format(Locale.US, "%02d", paisa)
        val symbolPrefix = if (currencySymbol.isNotBlank()) "$currencySymbol " else ""
        return "$prefix$symbolPrefix$groupedRupees.$formattedPaisa"
    }

    fun formatNepaliCurrency(amountInCents: Long, currencySymbol: String = "Rs"): String {
        return formatNepaliGrouping(amountInCents, currencySymbol)
    }

    /**
     * Check if a shift is stale (> 12 hours old or opened on a previous business date).
     */
    fun isShiftStale(
        openedAt: Long,
        shiftBusinessDate: String,
        now: Long = System.currentTimeMillis(),
        staleHours: Int = 12
    ): Boolean {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        if (shiftBusinessDate.isNotBlank() && shiftBusinessDate != todayStr) {
            return true
        }
        val elapsedMs = now - openedAt
        val maxDurationMs = staleHours.toLong() * 60L * 60L * 1000L
        return elapsedMs > maxDurationMs
    }

    fun isShiftStale(
        openedAt: Long,
        shiftBusinessDate: String,
        currentBusinessDate: String,
        staleHours: Int
    ): Boolean {
        if (shiftBusinessDate.isNotBlank() && currentBusinessDate.isNotBlank() && shiftBusinessDate != currentBusinessDate) {
            return true
        }
        val elapsedMs = System.currentTimeMillis() - openedAt
        val maxDurationMs = staleHours.toLong() * 60L * 60L * 1000L
        return elapsedMs > maxDurationMs
    }
}
