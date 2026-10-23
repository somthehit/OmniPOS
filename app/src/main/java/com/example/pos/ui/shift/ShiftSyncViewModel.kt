package com.example.pos.ui.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos.data.repository.ShiftRepository
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

data class ShiftSyncState(
    val denominations: Map<Int, Int> = emptyMap(),
    val isSyncing: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val errorMessage: String? = null
)

@OptIn(FlowPreview::class)
class ShiftSyncViewModel(
    private val shiftRepository: ShiftRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftSyncState())
    val uiState: StateFlow<ShiftSyncState> = _uiState.asStateFlow()

    // Trigger pipe for immediate/debounced user modifications
    private val changeTriggerFlow = MutableSharedFlow<Map<Int, Int>>(extraBufferCapacity = 1)

    // Concurrency lock to prevent simultaneous debounce & 1-minute ticker requests
    private val syncMutex = Mutex()
    private var periodicSyncJob: Job? = null

    init {
        observeUserChanges()
        startPeriodicSync()
    }

    /**
     * 1. Debounced change listener (executes 800ms after user stops typing)
     */
    private fun observeUserChanges() {
        viewModelScope.launch {
            changeTriggerFlow
                .debounce(800L)
                .collect { latestDenominations ->
                    executeSync(latestDenominations)
                }
        }
    }

    /**
     * 2. Periodic loop (runs automatically every 1 minute)
     */
    private fun startPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L) // 1 minute interval
                executeSync(_uiState.value.denominations)
            }
        }
    }

    /**
     * Called whenever cashier updates a denomination count in the UI
     */
    fun onDenominationChanged(denom: Int, count: Int) {
        val updatedMap = _uiState.value.denominations.toMutableMap().apply {
            if (count > 0) put(denom, count) else remove(denom)
        }

        _uiState.value = _uiState.value.copy(denominations = updatedMap)

        // Notify debounced sync flow
        changeTriggerFlow.tryEmit(updatedMap)
    }

    /**
     * Thread-safe sync dispatcher
     */
    private suspend fun executeSync(dataToSync: Map<Int, Int>) {
        // Prevent concurrent sync executions
        if (!syncMutex.tryLock()) return

        try {
            _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null)

            // Execute repository call (Room cache + remote sync)
            shiftRepository.syncShiftData(dataToSync)

            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                lastSyncedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                errorMessage = e.localizedMessage ?: "Sync error"
            )
        } finally {
            syncMutex.unlock()
        }
    }

    /**
     * Optional manual force sync
     */
    fun forceSync() {
        viewModelScope.launch {
            executeSync(_uiState.value.denominations)
        }
    }

    override fun onCleared() {
        super.onCleared()
        periodicSyncJob?.cancel()
    }
}
