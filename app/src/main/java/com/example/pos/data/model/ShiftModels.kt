package com.example.pos.data.model

import com.example.pos.data.local.ShiftAdjustmentEntity
import com.example.pos.data.local.ShiftCashCountEntity
import com.example.pos.data.local.ShiftCreditNoteEntity
import com.example.pos.data.local.ShiftDeclarationEntity
import com.example.pos.data.local.ShiftEntity
import com.example.pos.data.local.ShiftEventEntity
import com.example.pos.data.local.ShiftVersionEntity

/**
 * Domain and UI models for Shift Management.
 */

sealed interface ShiftGateDecision {
    object NoActiveShift : ShiftGateDecision
    data class ResumeShift(val shift: ShiftEntity) : ShiftGateDecision
    data class StaleShiftBlocked(val shift: ShiftEntity, val hoursOpen: Long) : ShiftGateDecision
    data class TerminalInUse(val shift: ShiftEntity) : ShiftGateDecision
}

data class DenominationItem(
    val denomination: Int,
    val count: Int
) {
    val totalInCents: Long get() = (denomination.toLong() * count.toLong()) * 100L
}

data class OnlineProviderDeclaration(
    val provider: String, // "eSewa", "Khalti", "FonePay", "Card", "Bank Transfer"
    val declaredAmountInCents: Long,
    val slipCount: Int = 0,
    val referenceNotes: String = ""
)

data class CreditNoteRecord(
    val creditNoteNo: String,
    val totalAmountInCents: Long,
    val customerCreditInCents: Long = 0L,
    val bankRefundInCents: Long = 0L,
    val cashRefundInCents: Long = 0L,
    val reason: String = ""
)

data class ShiftCloseDraft(
    val shiftId: String,
    val countedCashInCents: Long = 0L,
    val denominationCounts: List<DenominationItem> = emptyList(),
    val onlineDeclarations: List<OnlineProviderDeclaration> = emptyList(),
    val creditNotes: List<CreditNoteRecord> = emptyList(),
    val closingFloatInCents: Long = 0L,
    val depositAmountInCents: Long = 0L,
    val varianceReason: String = "",
    val closingNotes: String = ""
)

data class ShiftDetails(
    val shift: ShiftEntity,
    val cashCounts: List<ShiftCashCountEntity> = emptyList(),
    val declarations: List<ShiftDeclarationEntity> = emptyList(),
    val creditNotes: List<ShiftCreditNoteEntity> = emptyList(),
    val adjustments: List<ShiftAdjustmentEntity> = emptyList(),
    val versions: List<ShiftVersionEntity> = emptyList(),
    val events: List<ShiftEventEntity> = emptyList(),
    val metrics: ShiftLiveMetrics? = null
)
