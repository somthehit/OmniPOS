package com.omnipos.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Plain constructors so you can plug in Hilt (@HiltViewModel + @Inject), Koin, or a
 * ViewModelProvider.Factory, whichever OmniPOS already uses.
 */

// ===================== Entry gate =====================

class ShiftGateViewModel(
    private val repo: ShiftRepository,
    private val pin: PinVerifier,
) : ViewModel() {
    private val _gate = MutableStateFlow<ShiftGate?>(null)
    val gate: StateFlow<ShiftGate?> = _gate.asStateFlow()

    fun refresh() {
        viewModelScope.launch { _gate.value = repo.evaluateGate() }
    }

    suspend fun verifyPin(p: String): Boolean = pin.verify(p)
}

// ===================== Open shift =====================

data class OpenShiftUi(
    val denominations: List<Int> = DEFAULT_DENOMINATIONS,
    val counts: Map<Int, Int> = emptyMap(),
    val expectedOpening: Paisa? = null,
    val gapReason: String = "",
    val zeroConfirmed: Boolean = false,
    val showPin: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
) {
    val total: Paisa get() = ShiftMath.sumDenominations(counts)
    val gap: Paisa get() = expectedOpening?.let { ShiftMath.openingGap(total, it) } ?: 0L
}

class OpenShiftViewModel(
    private val repo: ShiftRepository,
    private val pin: PinVerifier,
) : ViewModel() {
    /** Generated once per screen: double taps and retries reuse the same id => one shift. */
    private val shiftId = UUID.randomUUID().toString()

    private val _ui = MutableStateFlow(OpenShiftUi())
    val ui: StateFlow<OpenShiftUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val policy = repo.policy()
            val gate = repo.evaluateGate()
            _ui.update {
                it.copy(
                    denominations = policy.denominations,
                    expectedOpening = (gate as? ShiftGate.NeedsOpen)?.expectedOpening,
                    error = (gate as? ShiftGate.Blocked)?.reason,
                )
            }
        }
    }

    fun setCount(denomination: Int, count: Int) =
        _ui.update { it.copy(counts = it.counts + (denomination to count.coerceIn(0, 100_000)), error = null) }

    fun setGapReason(v: String) = _ui.update { it.copy(gapReason = v) }
    fun setZeroConfirmed(v: Boolean) = _ui.update { it.copy(zeroConfirmed = v) }
    fun dismissPin() = _ui.update { it.copy(showPin = false) }

    /** Validates first, then asks for PIN. */
    fun requestStart() {
        val s = _ui.value
        val msg = when {
            s.total == 0L && !s.zeroConfirmed -> "Opening cash is Rs 0. Tick the confirmation to continue."
            s.gap != 0L && s.gapReason.isBlank() -> "Opening differs from previous closing float by ${formatNpr(s.gap)}. Enter a reason."
            else -> null
        }
        _ui.update { it.copy(error = msg, showPin = msg == null) }
    }

    fun confirmWithPin(p: String) {
        val s = _ui.value
        if (s.submitting) return
        viewModelScope.launch {
            _ui.update { it.copy(submitting = true, error = null) }
            if (!pin.verify(p)) {
                _ui.update { it.copy(submitting = false, error = "Wrong PIN.") }
                return@launch
            }
            runCatching { repo.openShift(shiftId, s.counts, s.zeroConfirmed, s.gapReason) }
                .onSuccess { _ui.update { it.copy(submitting = false, showPin = false, done = true) } }
                .onFailure { e -> _ui.update { it.copy(submitting = false, showPin = false, error = e.message) } }
        }
    }
}

// ===================== Active shift dashboard =====================

data class ActiveShiftUi(
    val shift: ShiftEntity? = null,
    val sales: SalesTotals = SalesTotals(),
    val totals: ShiftTotals? = null,
    val blindClose: Boolean = true,
    val error: String? = null,
) {
    /** Hidden while blind-close is on, so the cashier cannot count towards a known number. */
    val expectedPreview: Paisa? get() = if (blindClose) null else totals?.expectedCash
}

class ActiveShiftViewModel(
    private val shiftId: String,
    private val repo: ShiftRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(ActiveShiftUi())
    val ui: StateFlow<ActiveShiftUi> = _ui.asStateFlow()

    val movements = repo.observeMovements(shiftId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.closeSummary(shiftId) to repo.policy() }
                .onSuccess { (s, p) ->
                    _ui.value = ActiveShiftUi(shift = s.shift, sales = s.sales, totals = s.totals, blindClose = p.blindClose)
                }
                .onFailure { e -> _ui.update { it.copy(error = e.message) } }
        }
    }

    fun addMovement(type: MovementType, amountText: String, reason: String, onDone: () -> Unit) {
        val amount = parseRupeesToPaisa(amountText)
        viewModelScope.launch {
            runCatching {
                repo.addMovement(shiftId, type, amount ?: throw ShiftException("Enter a valid amount."), reason)
            }.onSuccess { refresh(); onDone() }
                .onFailure { e -> _ui.update { it.copy(error = e.message) } }
        }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}

// ===================== Close shift wizard =====================

data class CloseUi(
    val step: Int = 1,                                   // 1 Summary, 2 Cash count, 3 Declarations, 4 Review
    val loading: Boolean = true,
    val summary: CloseSummary? = null,
    val policy: ShiftPolicy = ShiftPolicy(),
    val counts: Map<Int, Int> = emptyMap(),
    val declaredText: Map<PayMethod, String> = emptyMap(),
    val references: Map<PayMethod, String> = emptyMap(),
    val reasonCodes: Map<PayMethod, ReasonCode> = emptyMap(),
    val closingFloatText: String = "",
    val varianceReason: String = "",
    val showPin: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val closedShift: ShiftEntity? = null,
) {
    val counted: Paisa get() = ShiftMath.sumDenominations(counts)
    val expected: Paisa? get() = summary?.totals?.expectedCash

    /** Blind close: expected cash only appears from the review step, after the count is submitted. */
    val expectedVisible: Boolean get() = !policy.blindClose || step >= 4
    val variance: Paisa? get() = expected?.let { ShiftMath.cashVariance(counted, it) }
    val closingFloat: Paisa get() = parseRupeesToPaisa(closingFloatText) ?: 0L

    /** POS vs (provider confirmed, else cashier declared). Read-only for the cashier. */
    fun confirmedFor(row: MethodRow): Paisa? =
        row.providerAmount ?: declaredText[row.method]?.let(::parseRupeesToPaisa)

    fun mismatchFor(row: MethodRow): Paisa? = confirmedFor(row)?.let { row.posAmount - it }
}

class CloseShiftViewModel(
    private val shiftId: String,
    private val repo: ShiftRepository,
    private val pin: PinVerifier,
) : ViewModel() {
    private val _ui = MutableStateFlow(CloseUi())
    val ui: StateFlow<CloseUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { Triple(repo.closeSummary(shiftId), repo.policy(), repo.loadClosingCounts(shiftId)) }
                .onSuccess { (summary, policy, counts) ->
                    _ui.update { it.copy(loading = false, summary = summary, policy = policy, counts = counts) }
                }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun setCount(denomination: Int, count: Int) =
        _ui.update { it.copy(counts = it.counts + (denomination to count.coerceIn(0, 100_000)), error = null) }

    fun setDeclared(m: PayMethod, text: String) = _ui.update { it.copy(declaredText = it.declaredText + (m to text)) }
    fun setReference(m: PayMethod, text: String) = _ui.update { it.copy(references = it.references + (m to text)) }
    fun setReasonCode(m: PayMethod, r: ReasonCode) = _ui.update { it.copy(reasonCodes = it.reasonCodes + (m to r)) }
    fun setClosingFloat(text: String) = _ui.update { it.copy(closingFloatText = text) }
    fun setVarianceReason(text: String) = _ui.update { it.copy(varianceReason = text) }
    fun dismissPin() = _ui.update { it.copy(showPin = false) }

    fun back() = _ui.update { it.copy(step = (it.step - 1).coerceAtLeast(1), error = null) }

    fun next() {
        val s = _ui.value
        val summary = s.summary ?: return
        when (s.step) {
            2 -> viewModelScope.launch {
                // CLOSING draft: the wizard can be resumed after an app kill / crash.
                runCatching { repo.saveCloseDraft(shiftId, s.counts) }
                    .onSuccess { _ui.update { it.copy(step = 3, error = null) } }
                    .onFailure { e -> _ui.update { it.copy(error = e.message) } }
            }
            3 -> {
                val missing = summary.methodRows.firstOrNull { it.providerAmount == null && s.confirmedFor(it) == null }
                val needsReason = summary.methodRows.firstOrNull {
                    (s.mismatchFor(it) ?: 0L) != 0L && s.reasonCodes[it.method] == null
                }
                val err = when {
                    missing != null -> "Enter the ${missing.method.name} amount."
                    needsReason != null -> "Select a reason for the ${needsReason.method.name} mismatch."
                    else -> null
                }
                _ui.update { it.copy(step = if (err == null) 4 else 3, error = err) }
            }
            else -> _ui.update { it.copy(step = (it.step + 1).coerceAtMost(4), error = null) }
        }
    }

    /** Step 4 "Close shift": validate, then ask for PIN. */
    fun requestClose() {
        val s = _ui.value
        val v = s.variance ?: 0L
        val err = when {
            s.closingFloat > s.counted -> "Closing float cannot be more than the counted cash."
            v != 0L && s.varianceReason.isBlank() -> "Cash difference of ${formatNpr(v)}. Enter a reason."
            else -> null
        }
        _ui.update { it.copy(error = err, showPin = err == null) }
    }

    fun confirmWithPin(p: String) {
        val s = _ui.value
        val summary = s.summary ?: return
        if (s.submitting) return
        viewModelScope.launch {
            _ui.update { it.copy(submitting = true, error = null) }
            if (!pin.verify(p)) {
                _ui.update { it.copy(submitting = false, error = "Wrong PIN.") }
                return@launch
            }
            val declarations = summary.methodRows.map { row ->
                DeclarationInput(
                    method = row.method,
                    declaredAmount = if (row.providerAmount == null) s.declaredText[row.method]?.let(::parseRupeesToPaisa) else null,
                    reference = s.references[row.method],
                    reasonCode = s.reasonCodes[row.method],
                )
            }
            runCatching {
                repo.submitClose(CloseInput(shiftId, s.counts, s.closingFloat, declarations, s.varianceReason))
            }.onSuccess { closed ->
                _ui.update { it.copy(submitting = false, showPin = false, closedShift = closed) }
            }.onFailure { e ->
                _ui.update { it.copy(submitting = false, showPin = false, error = e.message) }
            }
        }
    }
}

// ===================== Admin (online) =====================

data class AdminListUi(
    val loading: Boolean = true,
    val rows: List<AdminShiftRow> = emptyList(),
    val filter: String = "OPEN",                        // OPEN | PENDING_REVIEW | FLAGGED | STALE | ALL
    val error: String? = null,
)

class AdminShiftsViewModel(private val api: ShiftApi) : ViewModel() {
    private val _ui = MutableStateFlow(AdminListUi())
    val ui: StateFlow<AdminListUi> = _ui.asStateFlow()

    init { load("OPEN") }

    fun load(filter: String) {
        _ui.update { it.copy(loading = true, filter = filter, error = null) }
        viewModelScope.launch {
            runCatching {
                when (filter) {
                    "STALE" -> api.adminShifts(notClosedFromYesterday = true)
                    "ALL" -> api.adminShifts()
                    else -> api.adminShifts(status = filter)
                }
            }.onSuccess { rows -> _ui.update { it.copy(loading = false, rows = rows) } }
                .onFailure { e -> _ui.update { it.copy(loading = false, error = e.message) } }
        }
    }
}

data class AdminDetailUi(
    val loading: Boolean = true,
    val detail: AdminShiftDetail? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

class AdminShiftDetailViewModel(
    private val shiftId: String,
    private val api: ShiftApi,
    private val pin: PinVerifier,
) : ViewModel() {
    private val _ui = MutableStateFlow(AdminDetailUi())
    val ui: StateFlow<AdminDetailUi> = _ui.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            runCatching { api.adminShift(shiftId) }
                .onSuccess { d -> _ui.update { it.copy(loading = false, detail = d) } }
                .onFailure { e -> _ui.update { it.copy(loading = false, message = e.message) } }
        }
    }

    /** Every admin action needs PIN re-entry and a reason; the server writes the audit event. */
    private fun act(pinText: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, message = null) }
            if (!pin.verify(pinText)) {
                _ui.update { it.copy(busy = false, message = "Wrong PIN.") }
                return@launch
            }
            runCatching { block() }
                .onSuccess { _ui.update { it.copy(busy = false, message = "Done.") }; load() }
                .onFailure { e -> _ui.update { it.copy(busy = false, message = e.message) } }
        }
    }

    fun review(approve: Boolean, note: String, pinText: String) = act(pinText) {
        if (!approve && note.isBlank()) throw ShiftException("A note is required when flagging.")
        api.review(shiftId, UUID.randomUUID().toString(), ReviewRequest(if (approve) "APPROVE" else "FLAG", note))
    }

    fun forceClose(counts: Map<Int, Int>, closingFloat: Paisa, reason: String, pinText: String) = act(pinText) {
        if (reason.isBlank()) throw ShiftException("A reason is required.")
        api.forceClose(
            shiftId, UUID.randomUUID().toString(),
            ForceCloseRequest(counts.filterValues { it > 0 }.map { CountDto(it.key, it.value) }, closingFloat, reason),
        )
    }

    fun reopen(reason: String, pinText: String) = act(pinText) {
        if (reason.isBlank()) throw ShiftException("A reason is required.")
        api.reopen(shiftId, UUID.randomUUID().toString(), ReopenRequest(reason))
    }
}
