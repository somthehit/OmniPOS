package com.example.pos.ui.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos.data.local.ShiftDenominationEntity
import com.example.pos.data.repository.ShiftRepository
import com.example.pos.data.repository.ShiftStatusResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

import com.example.pos.data.repository.OpenShiftResult

sealed interface CashierShiftUiState {
    object Checking : CashierShiftUiState
    object NeedsDenominationInput : CashierShiftUiState
    data class ShiftRunning(val shift: ShiftDenominationEntity, val isSyncing: Boolean) : CashierShiftUiState
    data class MultiDeviceConflict(
        val activeDeviceId: String,
        val openedAt: Long,
        val activeShiftId: String
    ) : CashierShiftUiState
    data class Error(val message: String) : CashierShiftUiState
}

@OptIn(FlowPreview::class)
class CashierShiftViewModel(
    private val repository: ShiftRepository,
    val currentCashierId: String,
    val currentDeviceId: String = "reg_01"
) : ViewModel() {

    private val _uiState = MutableStateFlow<CashierShiftUiState>(CashierShiftUiState.Checking)
    val uiState: StateFlow<CashierShiftUiState> = _uiState.asStateFlow()

    private val changeDebounceFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val syncMutex = Mutex()
    private var heartbeatJob: Job? = null
    private var activeShiftId: String? = null

    init {
        checkCurrentCashierShift()
        observeDebouncedSync()
        startPeriodicSync()
    }

    fun checkCurrentCashierShift() {
        viewModelScope.launch {
            when (val result = repository.checkLastShiftStatus(currentCashierId)) {
                is ShiftStatusResult.NeedsNewShift -> {
                    activeShiftId = null
                    _uiState.value = CashierShiftUiState.NeedsDenominationInput
                }
                is ShiftStatusResult.ActiveShift -> {
                    val shift = result.shift
                    // If shift was opened on another device and is still open, flag conflict if needed
                    if (shift.deviceId.isNotBlank() && shift.deviceId != currentDeviceId) {
                        activeShiftId = null
                        _uiState.value = CashierShiftUiState.MultiDeviceConflict(
                            activeDeviceId = shift.deviceId,
                            openedAt = shift.openedAt,
                            activeShiftId = shift.shiftId
                        )
                    } else {
                        activeShiftId = shift.shiftId
                        _uiState.value = CashierShiftUiState.ShiftRunning(shift, isSyncing = false)
                    }
                }
            }
        }
    }

    // Called when entering opening float to start a new shift
    fun submitOpeningFloat(denominations: Map<Int, Int>) {
        viewModelScope.launch {
            when (val res = repository.requestOpenShift(currentCashierId, currentDeviceId, denominations)) {
                is OpenShiftResult.Success -> {
                    activeShiftId = res.shift.shiftId
                    _uiState.value = CashierShiftUiState.ShiftRunning(res.shift, isSyncing = false)
                    changeDebounceFlow.tryEmit(Unit)
                }
                is OpenShiftResult.Conflict -> {
                    activeShiftId = null
                    _uiState.value = CashierShiftUiState.MultiDeviceConflict(
                        activeDeviceId = res.activeDeviceId,
                        openedAt = res.openedAt,
                        activeShiftId = res.activeShiftId
                    )
                }
                is OpenShiftResult.Error -> {
                    _uiState.value = CashierShiftUiState.Error(res.message)
                }
            }
        }
    }

    // Administrative override: Force close previous terminal's shift and open on current device
    fun forceClosePreviousShiftAndOpen(
        shiftId: String,
        managerUid: String = "manager_01",
        managerName: String = "Store Manager",
        denominations: Map<Int, Int> = emptyMap()
    ) {
        viewModelScope.launch {
            repository.forceClosePreviousShift(
                shiftId = shiftId,
                cashierId = currentCashierId,
                managerUid = managerUid,
                managerName = managerName,
                reason = "Multi-device terminal transfer override to $currentDeviceId"
            )
            submitOpeningFloat(denominations)
        }
    }

    fun dismissConflict() {
        checkCurrentCashierShift()
    }

    // Called during the shift if counts change
    fun updateDenomination(denom: Int, count: Int, currentShift: ShiftDenominationEntity) {
        viewModelScope.launch {
            val updatedMap = currentShift.denominations.toMutableMap().apply {
                if (count > 0) put(denom, count) else remove(denom)
            }
            repository.updateShiftFloat(currentShift.shiftId, currentCashierId, updatedMap)
            val grandTotal = updatedMap.entries.sumOf { (d, c) -> (d * c).toLong() }
            val updatedShift = currentShift.copy(
                denominations = updatedMap,
                totalAmount = grandTotal,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
            _uiState.value = CashierShiftUiState.ShiftRunning(updatedShift, isSyncing = false)
            changeDebounceFlow.tryEmit(Unit)
        }
    }

    // Closes shift for the current cashier
    fun closeShift() {
        val shiftId = activeShiftId ?: return
        viewModelScope.launch {
            repository.closeShift(shiftId, currentCashierId)
            activeShiftId = null
            _uiState.value = CashierShiftUiState.NeedsDenominationInput
        }
    }

    private fun observeDebouncedSync() {
        viewModelScope.launch {
            changeDebounceFlow
                .debounce(800L)
                .collect { triggerSync() }
        }
    }

    private fun startPeriodicSync() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L) // 1 minute auto-sync
                triggerSync()
            }
        }
    }

    private suspend fun triggerSync() {
        val shiftId = activeShiftId ?: return
        if (!syncMutex.tryLock()) return

        try {
            val current = _uiState.value
            if (current is CashierShiftUiState.ShiftRunning) {
                _uiState.value = current.copy(isSyncing = true)
            }
            repository.syncShift(shiftId, currentCashierId)
            val updated = _uiState.value
            if (updated is CashierShiftUiState.ShiftRunning) {
                _uiState.value = updated.copy(
                    shift = updated.shift.copy(isSynced = true),
                    isSyncing = false
                )
            }
        } catch (_: Exception) {
            val state = _uiState.value
            if (state is CashierShiftUiState.ShiftRunning) {
                _uiState.value = state.copy(isSyncing = false)
            }
        } finally {
            syncMutex.unlock()
        }
    }

    override fun onCleared() {
        super.onCleared()
        heartbeatJob?.cancel()
    }

    companion object {
        fun provideFactory(
            repository: ShiftRepository,
            cashierId: String,
            context: android.content.Context
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val terminalLabel = com.example.pos.security.PosDeviceIdentity.getDisplayTerminalName(context)
                return CashierShiftViewModel(
                    repository = repository,
                    currentCashierId = cashierId,
                    currentDeviceId = terminalLabel
                ) as T
            }
        }
    }
}
