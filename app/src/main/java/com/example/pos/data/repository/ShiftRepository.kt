package com.example.pos.data.repository

import com.example.pos.data.local.PosDao
import com.example.pos.data.local.ShiftCashCountEntity
import com.example.pos.data.local.ShiftDao
import com.example.pos.data.local.ShiftDenominationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import java.util.UUID

/**
 * Result of checking the shift status for an isolated cashier.
 */
sealed class ShiftStatusResult {
    data class ActiveShift(val shift: ShiftDenominationEntity) : ShiftStatusResult()
    object NeedsNewShift : ShiftStatusResult()
}

/**
 * Result of attempting to open a cashier shift with multi-device protection.
 */
sealed class OpenShiftResult {
    data class Success(val shift: ShiftDenominationEntity) : OpenShiftResult()
    data class Conflict(val activeDeviceId: String, val openedAt: Long, val activeShiftId: String = "") : OpenShiftResult()
    data class Error(val message: String) : OpenShiftResult()
}

/**
 * Data payload for shift denomination network sync.
 */
data class ShiftSyncRequest(
    val shiftId: String = "",
    val cashierId: String = "",
    val deviceId: String = "",
    val status: String = "OPEN",
    val denominations: Map<Int, Int> = emptyMap(),
    val totalAmount: Long = 0L,
    val openedAt: Long = 0L,
    val closedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Repository interface for cashier-scoped shift denominations with Room persistence and auto-sync.
 */
interface ShiftRepository {
    fun observeActiveShift(cashierId: String): Flow<ShiftDenominationEntity?>
    suspend fun checkLastShiftStatus(cashierId: String): ShiftStatusResult
    suspend fun createNewShift(cashierId: String, denominations: Map<Int, Int>): ShiftDenominationEntity
    suspend fun createNewShift(cashierId: String, deviceId: String, denominations: Map<Int, Int>): ShiftDenominationEntity
    suspend fun requestOpenShift(cashierId: String, deviceId: String, denominations: Map<Int, Int>): OpenShiftResult
    suspend fun updateShiftFloat(shiftId: String, cashierId: String, denominations: Map<Int, Int>)
    suspend fun closeShift(shiftId: String, cashierId: String, closedAt: Long = System.currentTimeMillis())
    suspend fun forceClosePreviousShift(
        shiftId: String,
        cashierId: String,
        managerUid: String = "admin",
        managerName: String = "Manager",
        reason: String = "Multi-device terminal switch override"
    ): Boolean
    suspend fun syncShift(shiftId: String, cashierId: String)

    // Compatibility methods
    fun observeCurrentShift(shiftId: String): Flow<ShiftDenominationEntity?>
    suspend fun saveDenominationsLocally(shiftId: String, cashierId: String, denominations: Map<Int, Int>)
    suspend fun syncShift(shiftId: String)
    suspend fun getPendingUnsyncedShifts(): List<ShiftDenominationEntity>
    suspend fun syncShiftData(denominations: Map<Int, Int>)
    suspend fun syncShiftData(shiftId: String, phase: String, denominations: Map<Int, Int>)
    suspend fun getCachedShiftData(shiftId: String, phase: String = "OPENING"): Map<Int, Int>
}

/**
 * Offline-first implementation of ShiftRepository.
 * Writes immediately to local Room database on user input with isSynced = false,
 * and pushes to remote sync/API on sync trigger, setting isSynced = true on success.
 */
class ShiftRepositoryImpl(
    private val shiftDao: ShiftDao,
    private val posDao: PosDao? = null,
    private val syncRepository: SyncRepository? = null,
    private val currentShiftIdProvider: () -> String = { "shift_active" }
) : ShiftRepository {

    override fun observeActiveShift(cashierId: String): Flow<ShiftDenominationEntity?> {
        return shiftDao.observeActiveShiftForCashier(cashierId)
    }

    override suspend fun checkLastShiftStatus(cashierId: String): ShiftStatusResult = withContext(Dispatchers.IO) {
        val lastShift = shiftDao.getLastShiftForCashier(cashierId)
        when {
            lastShift == null -> ShiftStatusResult.NeedsNewShift
            lastShift.status == "OPEN" -> ShiftStatusResult.ActiveShift(lastShift)
            else -> ShiftStatusResult.NeedsNewShift
        }
    }

    override suspend fun createNewShift(
        cashierId: String,
        denominations: Map<Int, Int>
    ): ShiftDenominationEntity = createNewShift(cashierId, "", denominations)

    override suspend fun createNewShift(
        cashierId: String,
        deviceId: String,
        denominations: Map<Int, Int>
    ): ShiftDenominationEntity = withContext(Dispatchers.IO) {
        val grandTotal = denominations.entries.sumOf { (denom, count) -> (denom * count).toLong() }
        val now = System.currentTimeMillis()
        val newShift = ShiftDenominationEntity(
            shiftId = "shift_${cashierId}_${UUID.randomUUID()}",
            cashierId = cashierId,
            deviceId = deviceId,
            status = "OPEN",
            denominations = denominations,
            totalAmount = grandTotal,
            isSynced = false,
            openedAt = now,
            updatedAt = now
        )
        shiftDao.upsertShift(newShift)

        // Also update legacy ShiftCashCountEntity for backwards compatibility with posDao
        posDao?.let { dao ->
            val countEntities = denominations.map { (denom, count) ->
                ShiftCashCountEntity(
                    id = "${newShift.shiftId}_OPENING_$denom",
                    shiftId = newShift.shiftId,
                    phase = "OPENING",
                    denomination = denom,
                    count = count,
                    amountInCents = denom.toLong() * count.toLong() * 100L,
                    subtotalInCents = denom.toLong() * count.toLong() * 100L
                )
            }
            if (countEntities.isNotEmpty()) {
                dao.insertShiftCashCounts(countEntities)
            }
        }

        newShift
    }

    override suspend fun requestOpenShift(
        cashierId: String,
        deviceId: String,
        denominations: Map<Int, Int>
    ): OpenShiftResult = withContext(Dispatchers.IO) {
        // 1. Proactive Check: Identify existing active shift in shiftDao
        val existingShift = shiftDao.getLastShiftForCashier(cashierId)
        if (existingShift != null && existingShift.status == "OPEN") {
            val devId = existingShift.deviceId.ifBlank { "another terminal" }
            return@withContext OpenShiftResult.Conflict(
                activeDeviceId = devId,
                openedAt = existingShift.openedAt,
                activeShiftId = existingShift.shiftId
            )
        }

        // Proactive Check: Identify existing active shift in register shifts (posDao)
        val posShift = posDao?.getActiveShiftForCashierSync(cashierId)
        if (posShift != null && (posShift.status == "OPEN" || posShift.status == "CLOSING")) {
            val devId = posShift.lastDeviceId.ifBlank { posShift.registerId }
            return@withContext OpenShiftResult.Conflict(
                activeDeviceId = devId,
                openedAt = posShift.openedAt,
                activeShiftId = posShift.id
            )
        }

        // 2. Insert new shift with unique constraint safety
        try {
            val newShift = createNewShift(cashierId, deviceId, denominations)
            OpenShiftResult.Success(newShift)
        } catch (e: Exception) {
            // 3. Catch race condition blocked by the DB unique index
            if (e.message?.contains("idx_unique_active_cashier_shift") == true ||
                e is android.database.sqlite.SQLiteConstraintException
            ) {
                val active = shiftDao.getLastShiftForCashier(cashierId)
                OpenShiftResult.Conflict(
                    activeDeviceId = active?.deviceId?.ifBlank { "another terminal" } ?: "another terminal",
                    openedAt = active?.openedAt ?: System.currentTimeMillis(),
                    activeShiftId = active?.shiftId ?: ""
                )
            } else {
                OpenShiftResult.Error(e.localizedMessage ?: "Failed to open shift")
            }
        }
    }

    override suspend fun forceClosePreviousShift(
        shiftId: String,
        cashierId: String,
        managerUid: String,
        managerName: String,
        reason: String
    ): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        shiftDao.closeShiftForCashier(shiftId, cashierId, now)
        posDao?.let { dao ->
            val shift = dao.getShiftById(shiftId)
            if (shift != null) {
                dao.updateShift(
                    shift.copy(
                        status = "CLOSED_UNRECONCILED",
                        closedAt = now,
                        closedBy = "$managerName (Override Force Close)",
                        reviewStatus = "FLAGGED",
                        notes = "${shift.notes} | Force Closed: $reason".trim(' ', '|')
                    )
                )
                dao.updateRegisterCurrentShift(shift.registerId, null)
            }
        }
        true
    }

    override suspend fun updateShiftFloat(
        shiftId: String,
        cashierId: String,
        denominations: Map<Int, Int>
    ): Unit = withContext(Dispatchers.IO) {
        val grandTotal = denominations.entries.sumOf { (denom, count) -> (denom * count).toLong() }
        val existing = shiftDao.getLastShiftForCashier(cashierId) ?: return@withContext

        if (existing.shiftId == shiftId && existing.status == "OPEN") {
            val updated = existing.copy(
                denominations = denominations,
                totalAmount = grandTotal,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
            shiftDao.upsertShift(updated)
        }
    }

    override suspend fun closeShift(
        shiftId: String,
        cashierId: String,
        closedAt: Long
    ): Unit = withContext(Dispatchers.IO) {
        shiftDao.closeShiftForCashier(shiftId, cashierId, closedAt)
    }

    override suspend fun syncShift(shiftId: String, cashierId: String): Unit = withContext(Dispatchers.IO) {
        val shift = shiftDao.getLastShiftForCashier(cashierId) ?: return@withContext
        if (shift.shiftId != shiftId || shift.isSynced) return@withContext

        try {
            syncRepository?.let { syncRepo ->
                val payload = mutableMapOf<String, Any>(
                    "shiftId" to shift.shiftId,
                    "cashierId" to cashierId,
                    "status" to shift.status,
                    "denominations" to shift.denominations,
                    "totalAmount" to shift.totalAmount,
                    "openedAt" to shift.openedAt,
                    "updatedAt" to shift.updatedAt
                )
                shift.closedAt?.let { payload["closedAt"] = it }
                syncRepo.recordShiftDenominations(shift.shiftId, payload)
            }
            shiftDao.markAsSynced(shiftId, cashierId)
        } catch (e: Exception) {
            throw e
        }
    }

    override fun observeCurrentShift(shiftId: String): Flow<ShiftDenominationEntity?> {
        return shiftDao.observeShift(shiftId)
    }

    // Fast local write - UI responds immediately
    override suspend fun saveDenominationsLocally(
        shiftId: String,
        cashierId: String,
        denominations: Map<Int, Int>
    ): Unit = withContext(Dispatchers.IO) {
        val grandTotal = denominations.entries.sumOf { (denom, count) -> (denom * count).toLong() }

        val existing = shiftDao.getShiftById(shiftId)
        val entity = ShiftDenominationEntity(
            shiftId = shiftId,
            cashierId = cashierId,
            status = existing?.status ?: "OPEN",
            denominations = denominations,
            totalAmount = grandTotal,
            isSynced = false,
            openedAt = existing?.openedAt ?: System.currentTimeMillis(),
            closedAt = existing?.closedAt,
            updatedAt = System.currentTimeMillis()
        )
        shiftDao.upsertShift(entity)

        // Also update legacy ShiftCashCountEntity for backwards compatibility with posDao
        posDao?.let { dao ->
            val countEntities = denominations.map { (denom, count) ->
                ShiftCashCountEntity(
                    id = "${shiftId}_OPENING_$denom",
                    shiftId = shiftId,
                    phase = "OPENING",
                    denomination = denom,
                    count = count,
                    amountInCents = denom.toLong() * count.toLong() * 100L,
                    subtotalInCents = denom.toLong() * count.toLong() * 100L
                )
            }
            if (countEntities.isNotEmpty()) {
                dao.insertShiftCashCounts(countEntities)
            }
        }
    }

    // Sync pending or targeted shifts to remote server / Cloud Firestore
    override suspend fun syncShift(shiftId: String): Unit = withContext(Dispatchers.IO) {
        val shift = shiftDao.getShiftById(shiftId) ?: return@withContext
        if (shift.isSynced) return@withContext

        try {
            // Write to Cloud Firestore if sync repository is available
            syncRepository?.let { syncRepo ->
                val payload = mutableMapOf<String, Any>(
                    "shiftId" to shift.shiftId,
                    "cashierId" to shift.cashierId,
                    "status" to shift.status,
                    "denominations" to shift.denominations,
                    "totalAmount" to shift.totalAmount,
                    "openedAt" to shift.openedAt,
                    "updatedAt" to shift.updatedAt
                )
                shift.closedAt?.let { payload["closedAt"] = it }
                syncRepo.recordShiftDenominations(shift.shiftId, payload)
            }

            // Mark record as synced in local Room storage
            shiftDao.markAsSynced(shiftId)
        } catch (e: Exception) {
            // If offline, exception bubbles up so ViewModel can handle/log; Room cache retains isSynced = false
            throw e
        }
    }

    override suspend fun getPendingUnsyncedShifts(): List<ShiftDenominationEntity> = withContext(Dispatchers.IO) {
        shiftDao.getPendingUnsyncedShifts()
    }

    override suspend fun syncShiftData(denominations: Map<Int, Int>) {
        val shiftId = currentShiftIdProvider().ifBlank { "shift_active" }
        saveDenominationsLocally(shiftId, "cashier", denominations)
        syncShift(shiftId)
    }

    override suspend fun syncShiftData(
        shiftId: String,
        phase: String,
        denominations: Map<Int, Int>
    ) {
        val shift = shiftDao.getShiftById(shiftId)
        val cashierId = shift?.cashierId ?: "cashier"
        saveDenominationsLocally(shiftId, cashierId, denominations)
        syncShift(shiftId)
    }

    override suspend fun getCachedShiftData(
        shiftId: String,
        phase: String
    ): Map<Int, Int> = withContext(Dispatchers.IO) {
        val entity = shiftDao.getShiftById(shiftId)
        entity?.denominations ?: emptyMap()
    }
}
