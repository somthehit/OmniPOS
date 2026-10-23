package com.example.pos.ui.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos.data.local.ShiftDenominationEntity
import com.example.pos.data.repository.ShiftRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Offline-first ViewModel that binds directly to Room's Flow for real-time reactivity
 * and manages both debounced network sync (800ms) and the 1-minute fallback heartbeat.
 */
@OptIn(FlowPreview::class)
class OfflineShiftViewModel(
    private val repository: ShiftRepository,
    private val shiftId: String,
    private val cashierId: String
) : ViewModel() {

    private val _syncingState = MutableStateFlow(false)
    val syncingState: StateFlow<Boolean> = _syncingState.asStateFlow()

    // Expose Room flow directly to UI for offline-first rendering
    val currentShiftFlow: Flow<ShiftDenominationEntity?> = repository.observeCurrentShift(shiftId)

    private val changeDebounceFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val syncMutex = Mutex()
    private var heartbeatJob: Job? = null

    init {
        observeDebouncedSync()
        startPeriodicHeartbeat()
    }

    /**
     * Fast local persistence to Room database on every input change,
     * immediately followed by a debounced sync trigger.
     */
    fun onCountChanged(denom: Int, count: Int, currentEntity: ShiftDenominationEntity?) {
        viewModelScope.launch {
            val currentMap = currentEntity?.denominations?.toMutableMap() ?: mutableMapOf()
            if (count > 0) {
                currentMap[denom] = count
            } else {
                currentMap.remove(denom)
            }

            // 1. Instant local persistence to Room (isSynced = false)
            repository.saveDenominationsLocally(shiftId, cashierId, currentMap)

            // 2. Trigger debounced network sync
            changeDebounceFlow.tryEmit(Unit)
        }
    }

    /**
     * Debounced sync (800ms) to prevent flooding the network during typing.
     */
    private fun observeDebouncedSync() {
        viewModelScope.launch {
            changeDebounceFlow
                .debounce(800L)
                .collect {
                    triggerSync()
                }
        }
    }

    /**
     * Periodic 1-minute fallback heartbeat to ensure pending unsynced changes
     * are pushed even without user interaction.
     */
    private fun startPeriodicHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L) // 1-minute fallback check
                triggerSync()
            }
        }
    }

    /**
     * Thread-safe sync operation.
     */
    suspend fun triggerSync() {
        if (!syncMutex.tryLock()) return
        try {
            _syncingState.value = true
            repository.syncShift(shiftId, cashierId)
        } catch (e: Exception) {
            // Fails gracefully when offline; data remains preserved in Room with isSynced = false
        } finally {
            _syncingState.value = false
            syncMutex.unlock()
        }
    }

    override fun onCleared() {
        super.onCleared()
        heartbeatJob?.cancel()
    }
}
