package com.omnipos.shift

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/** All money is Long paisa (1 NPR = 100 paisa). Never use Double for money. */
typealias Paisa = Long

val DEFAULT_DENOMINATIONS: List<Int> = listOf(1000, 500, 100, 50, 20, 10, 5, 2, 1)

enum class ShiftStatus { OPEN, CLOSING, PENDING_REVIEW, FLAGGED, APPROVED }
enum class CountPhase { OPENING, CLOSING, ADJUSTMENT }
enum class MovementType { PAID_IN, PAID_OUT, SAFE_DROP }
enum class PayMethod { ESEWA, KHALTI, FONEPAY, CARD, BANK }
enum class Verification { VERIFIED, DECLARED_UNVERIFIED }
enum class ReasonCode { WRONG_ENTRY, CUSTOMER_CANCELLED, PAYMENT_PENDING, PROVIDER_DELAY, OTHER }

val ACTIVE_STATUSES: List<String> = listOf(ShiftStatus.OPEN.name, ShiftStatus.CLOSING.name)

class ShiftException(message: String) : Exception(message)

/** Admin-configurable rules. Load from server settings, cache locally. */
data class ShiftPolicy(
    val blindClose: Boolean = true,
    val staleAfterHours: Long = 12,
    val varianceThresholdPaisa: Paisa = 10_000,        // Rs 100
    val varianceThresholdPercent: Double = 0.5,
    val autoApproveZeroVariance: Boolean = false,
    val allowOpenWhilePendingReview: Boolean = true,
    val blockIfPendingReviewOverHours: Long = 24,
    val denominations: List<Int> = DEFAULT_DENOMINATIONS,
)

/** Inputs for the expected-cash formula. */
data class ShiftTotals(
    val openingCash: Paisa,
    val cashSales: Paisa,
    val creditCollectionsCash: Paisa,
    val paidIn: Paisa,
    val cashRefunds: Paisa,       // ONLY the cash portion of refunds / credit notes
    val paidOut: Paisa,
    val safeDrops: Paisa,
) {
    val expectedCash: Paisa
        get() = openingCash + cashSales + creditCollectionsCash + paidIn -
            cashRefunds - paidOut - safeDrops
}

object ShiftMath {
    fun sumDenominations(counts: Map<Int, Int>): Paisa =
        counts.entries.sumOf { it.key.toLong() * it.value.coerceAtLeast(0) * 100L }

    fun cashVariance(counted: Paisa, expected: Paisa): Paisa = counted - expected
    fun netCashChange(closing: Paisa, opening: Paisa): Paisa = closing - opening
    fun openingGap(declared: Paisa, previousClosingFloat: Paisa): Paisa = declared - previousClosingFloat

    fun exceedsThreshold(variance: Paisa, expected: Paisa, p: ShiftPolicy): Boolean {
        val a = abs(variance)
        if (a == 0L) return false
        val pct = if (expected != 0L) a * 100.0 / abs(expected).toDouble() else 100.0
        return a > p.varianceThresholdPaisa || pct > p.varianceThresholdPercent
    }
}

/** What the Register tab must do when the cashier logs in / taps the shift badge. */
sealed interface ShiftGate {
    data class Resume(val shift: ShiftEntity, val needsPinTakeover: Boolean) : ShiftGate
    data class StaleMustClose(val shift: ShiftEntity) : ShiftGate
    data class CashierOpenElsewhere(val terminalId: String) : ShiftGate
    data class TerminalInUse(val byCashierName: String) : ShiftGate
    data class Blocked(val reason: String) : ShiftGate
    data class NeedsOpen(val expectedOpening: Paisa?) : ShiftGate
}

fun businessDateOf(instant: Instant, zone: ZoneId): LocalDate = instant.atZone(zone).toLocalDate()

/** 1,00,000 style (Nepali / Indian grouping). */
fun formatNpr(paisa: Paisa, withSymbol: Boolean = true): String {
    val neg = paisa < 0
    val a = abs(paisa)
    val rupees = (a / 100).toString()
    val p = (a % 100).toInt()
    val grouped = if (rupees.length <= 3) rupees else {
        val last3 = rupees.takeLast(3)
        val rest = rupees.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        "$rest,$last3"
    }
    val body = if (p == 0) grouped else "$grouped.${p.toString().padStart(2, '0')}"
    return (if (neg) "-" else "") + (if (withSymbol) "Rs " else "") + body
}

/** "1250.50" -> 125050. Returns null for blank / invalid input. */
fun parseRupeesToPaisa(text: String): Paisa? = try {
    if (text.isBlank()) null else BigDecimal(text.trim()).movePointRight(2).toLong()
} catch (e: NumberFormatException) {
    null
}
