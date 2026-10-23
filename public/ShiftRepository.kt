package com.omnipos.shift

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ---------- Adapter boundaries: implement these on top of your EXISTING OmniPOS code ----------

interface SessionProvider {
    val tenantId: String
    val branchId: String
    val terminalId: String
    val deviceId: String
    val cashierId: String
    val cashierName: String
}

interface PinVerifier {
    suspend fun verify(pin: String): Boolean
}

data class CreditNoteSummary(
    val issuedCount: Int = 0,
    val issuedTotal: Paisa = 0,
    val cashRefund: Paisa = 0,
    val customerBalance: Paisa = 0,
    val bankRefund: Paisa = 0,
    val redeemedAsTender: Paisa = 0,
)

data class SalesTotals(
    val cashSales: Paisa = 0,
    val creditCollectionsCash: Paisa = 0,
    val cashRefunds: Paisa = 0,                          // cash portion only
    val creditSales: Paisa = 0,
    val posByMethod: Map<PayMethod, Paisa> = emptyMap(), // actual payment transactions, not invoice gross
    val providerConfirmed: Map<PayMethod, Paisa> = emptyMap(), // empty when there is no integration
    val creditNote: CreditNoteSummary = CreditNoteSummary(),
    val unsyncedCount: Int = 0,
)

/** Hook this to your existing sales / payments / returns tables. */
interface ShiftSalesSource {
    suspend fun totalsFor(shiftId: String): SalesTotals
}

// ---------- Close-shift helper models ----------

data class MethodRow(val method: PayMethod, val posAmount: Paisa, val providerAmount: Paisa?)

data class CloseSummary(
    val shift: ShiftEntity,
    val totals: ShiftTotals,
    val sales: SalesTotals,
    val methodRows: List<MethodRow>,
)

data class DeclarationInput(
    val method: PayMethod,
    val declaredAmount: Paisa?,      // needed only when no provider confirmation exists
    val reference: String?,
    val reasonCode: ReasonCode?,
    val note: String? = null,
)

data class CloseInput(
    val shiftId: String,
    val counts: Map<Int, Int>,
    val closingFloat: Paisa,
    val declarations: List<DeclarationInput>,
    val varianceReason: String?,
)

// ---------- Repository ----------

class ShiftRepository(
    private val dao: ShiftDao,
    private val sales: ShiftSalesSource,
    private val session: SessionProvider,
    private val policyProvider: suspend () -> ShiftPolicy,
    private val onOutboxQueued: () -> Unit = {},          // e.g. { ShiftSyncWorker.enqueue(context) }
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Instant = { Instant.now() },
    private val json: Json = Json { encodeDefaults = true },
) {

    suspend fun policy(): ShiftPolicy = policyProvider()

    fun observeShift(id: String): Flow<ShiftEntity?> = dao.observe(id)
    fun observeMovements(shiftId: String): Flow<List<CashMovementEntity>> = dao.observeMovements(shiftId)

    // ===== 4. Login / Register entry gate =====

    suspend fun evaluateGate(): ShiftGate {
        val p = policy()

        val mine = dao.activeForCashier(session.cashierId)
        if (mine != null) {
            if (mine.terminalId != session.terminalId) return ShiftGate.CashierOpenElsewhere(mine.terminalId)
            return if (isStale(mine, p)) ShiftGate.StaleMustClose(mine)
            else ShiftGate.Resume(mine, needsPinTakeover = mine.deviceId != session.deviceId)
        }

        dao.activeForTerminal(session.terminalId)?.let { return ShiftGate.TerminalInUse(it.cashierName) }

        val last = dao.lastClosedForTerminal(session.terminalId)
        if (last != null) {
            val pendingHours = last.closedAt?.let { Duration.between(Instant.ofEpochMilli(it), now()).toHours() } ?: 0
            when {
                last.statusEnum == ShiftStatus.FLAGGED ->
                    return ShiftGate.Blocked("Previous shift is flagged. Ask an admin to review it first.")
                last.statusEnum == ShiftStatus.PENDING_REVIEW && !p.allowOpenWhilePendingReview ->
                    return ShiftGate.Blocked("Previous shift is waiting for admin review.")
                last.statusEnum == ShiftStatus.PENDING_REVIEW && pendingHours >= p.blockIfPendingReviewOverHours ->
                    return ShiftGate.Blocked("Previous shift has been pending review for over ${p.blockIfPendingReviewOverHours}h.")
            }
        }
        return ShiftGate.NeedsOpen(expectedOpening = last?.closingFloat)
    }

    private fun isStale(shift: ShiftEntity, p: ShiftPolicy): Boolean {
        val opened = Instant.ofEpochMilli(shift.openedAt)
        val differentDay = shift.businessDate != businessDateOf(now(), zone).toString()
        val tooLong = Duration.between(opened, now()).toHours() >= p.staleAfterHours
        return differentDay || tooLong
    }

    // ===== 5. Open shift =====

    /** `shiftId` must be generated ONCE per Open screen (UUID) so retries / double taps are idempotent. */
    suspend fun openShift(
        shiftId: String,
        counts: Map<Int, Int>,
        zeroConfirmed: Boolean,
        gapReason: String?,
    ): ShiftEntity {
        val total = ShiftMath.sumDenominations(counts)
        if (total == 0L && !zeroConfirmed) throw ShiftException("Opening cash is Rs 0. Please confirm.")

        val expectedOpening = dao.lastClosedForTerminal(session.terminalId)?.closingFloat
        val gap = expectedOpening?.let { ShiftMath.openingGap(total, it) } ?: 0L
        if (gap != 0L && gapReason.isNullOrBlank()) {
            throw ShiftException("Opening cash differs from the previous closing float by ${formatNpr(gap)}. Enter a reason.")
        }

        val t = now()
        val shift = ShiftEntity(
            id = shiftId,
            tenantId = session.tenantId,
            branchId = session.branchId,
            terminalId = session.terminalId,
            deviceId = session.deviceId,
            cashierId = session.cashierId,
            cashierName = session.cashierName,
            businessDate = businessDateOf(t, zone).toString(),
            status = ShiftStatus.OPEN.name,
            openedAt = t.toEpochMilli(),
            openingCash = total,
            openingExpected = expectedOpening,
            openingGap = gap,
            openingGapReason = gapReason?.trim(),
        )
        val countRows = counts.filterValues { it > 0 }.map { (d, c) ->
            CashCountEntity(shiftId = shiftId, phase = CountPhase.OPENING.name, denomination = d, count = c)
        }
        val payload = OpenShiftRequest(
            shiftId = shiftId, tenantId = shift.tenantId, branchId = shift.branchId,
            terminalId = shift.terminalId, deviceId = shift.deviceId, cashierId = shift.cashierId,
            businessDate = shift.businessDate, openedAt = shift.openedAt,
            counts = countRows.map { CountDto(it.denomination, it.count) },
            openingGapReason = shift.openingGapReason,
        )
        val saved = dao.openShiftAtomically(shift, countRows, outbox(shiftId, "shifts/open", json.encodeToString(payload)))
        onOutboxQueued()
        return saved
    }

    // ===== 6. Cash movements (paid in / paid out / safe drop) =====

    suspend fun addMovement(shiftId: String, type: MovementType, amount: Paisa, reason: String) {
        if (amount <= 0) throw ShiftException("Amount must be greater than 0.")
        if (reason.isBlank()) throw ShiftException("A reason is required.")
        val shift = dao.getById(shiftId) ?: throw ShiftException("Shift not found.")
        if (shift.statusEnum != ShiftStatus.OPEN) throw ShiftException("Movements can only be added to an OPEN shift.")

        val m = CashMovementEntity(
            id = UUID.randomUUID().toString(), shiftId = shiftId, type = type.name,
            amount = amount, reason = reason.trim(), createdBy = session.cashierId, createdAt = now().toEpochMilli(),
        )
        dao.insertMovement(m)   // immutable once saved: no update/delete in the DAO on purpose
        val payload = MovementRequest(m.id, shiftId, m.type, m.amount, m.reason, m.createdAt)
        dao.insertOutbox(outbox(shiftId, "shifts/$shiftId/movements", json.encodeToString(payload), key = m.id))
        onOutboxQueued()
    }

    // ===== 7. Close wizard =====

    suspend fun closeSummary(shiftId: String): CloseSummary {
        val shift = dao.getById(shiftId) ?: throw ShiftException("Shift not found.")
        val s = sales.totalsFor(shiftId)
        val mv = dao.movementTotals(shiftId).associate { it.type to it.total }
        val totals = ShiftTotals(
            openingCash = shift.openingCash,
            cashSales = s.cashSales,
            creditCollectionsCash = s.creditCollectionsCash,
            paidIn = mv[MovementType.PAID_IN.name] ?: 0,
            cashRefunds = s.cashRefunds,
            paidOut = mv[MovementType.PAID_OUT.name] ?: 0,
            safeDrops = mv[MovementType.SAFE_DROP.name] ?: 0,
        )
        val rows = PayMethod.entries.mapNotNull { m ->
            val pos = s.posByMethod[m] ?: 0
            val prov = s.providerConfirmed[m]
            if (pos == 0L && prov == null) null else MethodRow(m, pos, prov)
        }
        return CloseSummary(shift, totals, s, rows)
    }

    suspend fun loadClosingCounts(shiftId: String): Map<Int, Int> =
        dao.counts(shiftId, CountPhase.CLOSING.name).associate { it.denomination to it.count }

    /** Wizard is resumable: moving to CLOSING also blocks new sales on the Register. */
    suspend fun saveCloseDraft(shiftId: String, counts: Map<Int, Int>) {
        val shift = dao.getById(shiftId) ?: throw ShiftException("Shift not found.")
        if (!shift.isActive) throw ShiftException("Shift is already closed.")
        dao.saveClosingDraft(
            shift.copy(status = ShiftStatus.CLOSING.name),
            counts.filterValues { it > 0 }.map {
                CashCountEntity(shiftId = shiftId, phase = CountPhase.CLOSING.name, denomination = it.key, count = it.value)
            },
        )
    }

    suspend fun submitClose(input: CloseInput): ShiftEntity {
        val p = policy()
        val summary = closeSummary(input.shiftId)
        val shift = summary.shift
        if (!shift.isActive) throw ShiftException("Shift is already closed.")

        val counted = ShiftMath.sumDenominations(input.counts)
        if (input.closingFloat < 0 || input.closingFloat > counted) {
            throw ShiftException("Closing float must be between 0 and the counted cash.")
        }
        val expected = summary.totals.expectedCash
        val variance = ShiftMath.cashVariance(counted, expected)
        if (variance != 0L && input.varianceReason.isNullOrBlank()) {
            throw ShiftException("Cash difference of ${formatNpr(variance)}. A reason is required.")
        }

        // Online / QR / card: cashier can never edit the mismatch away.
        val decls = summary.methodRows.map { row ->
            val d = input.declarations.firstOrNull { it.method == row.method }
            val confirmed = row.providerAmount ?: d?.declaredAmount
                ?: throw ShiftException("Enter the ${row.method.name} amount.")
            if (row.posAmount - confirmed != 0L && d?.reasonCode == null) {
                throw ShiftException("Select a reason for the ${row.method.name} mismatch.")
            }
            DeclarationEntity(
                shiftId = shift.id, method = row.method.name, posAmount = row.posAmount,
                providerAmount = row.providerAmount,
                declaredAmount = if (row.providerAmount == null) confirmed else d?.declaredAmount,
                verification = if (row.providerAmount != null) Verification.VERIFIED.name
                else Verification.DECLARED_UNVERIFIED.name,   // never label as bank-verified
                reference = d?.reference, reasonCode = d?.reasonCode?.name, note = d?.note,
            )
        }
        val onlineMismatch = decls.any { it.mismatch != 0L }

        val cn = summary.sales.creditNote
        val cnEntity = CreditNoteEntity(
            shiftId = shift.id, issuedCount = cn.issuedCount, issuedTotal = cn.issuedTotal,
            cashRefund = cn.cashRefund, customerBalance = cn.customerBalance,
            bankRefund = cn.bankRefund, redeemedAsTender = cn.redeemedAsTender,
        )

        val status = when {
            ShiftMath.exceedsThreshold(variance, expected, p) || onlineMismatch -> ShiftStatus.FLAGGED
            variance == 0L && p.autoApproveZeroVariance -> ShiftStatus.APPROVED
            else -> ShiftStatus.PENDING_REVIEW
        }
        val closedAt = now().toEpochMilli()
        val updated = shift.copy(
            status = status.name, closedAt = closedAt, expectedCash = expected, countedCash = counted,
            closingFloat = input.closingFloat, depositAmount = counted - input.closingFloat,
            cashVariance = variance, closeReason = input.varianceReason?.trim(), closedBy = session.cashierId,
            version = shift.version + 1, synced = false,
        )
        val countRows = input.counts.filterValues { it > 0 }.map {
            CashCountEntity(shiftId = shift.id, phase = CountPhase.CLOSING.name, denomination = it.key, count = it.value)
        }
        val payload = CloseShiftRequest(
            shiftId = shift.id, closedAt = closedAt,
            counts = countRows.map { CountDto(it.denomination, it.count) },
            closingFloat = input.closingFloat,
            declarations = decls.map {
                DeclarationDto(it.method, it.declaredAmount, it.reference, it.verification, it.reasonCode, it.note)
            },
            creditNote = CreditNoteDto(
                cn.issuedCount, cn.issuedTotal, cn.cashRefund, cn.customerBalance, cn.bankRefund, cn.redeemedAsTender,
            ),
            varianceReason = input.varianceReason?.trim(),
        )
        dao.closeShiftAtomically(
            updated, countRows, decls, cnEntity,
            outbox(shift.id, "shifts/${shift.id}/close", json.encodeToString(payload)),
        )
        onOutboxQueued()
        return updated
    }

    private fun outbox(shiftId: String, path: String, payload: String, key: String = UUID.randomUUID().toString()) =
        OutboxEntity(
            id = UUID.randomUUID().toString(), shiftId = shiftId, path = path,
            payloadJson = payload, idempotencyKey = key, createdAt = now().toEpochMilli(),
        )
}
