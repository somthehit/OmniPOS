package com.example.pos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for offline-first shift denominations with cashier-isolated scoping.
 */
@Dao
interface ShiftDao {

    // 1. Observe ONLY the active shift for this specific logged-in cashier
    @Query("SELECT * FROM shift_denominations WHERE cashierId = :cashierId AND status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    fun observeActiveShiftForCashier(cashierId: String): Flow<ShiftDenominationEntity?>

    // 2. One-time check on login: Did THIS cashier close their last shift?
    @Query("SELECT * FROM shift_denominations WHERE cashierId = :cashierId ORDER BY openedAt DESC LIMIT 1")
    suspend fun getLastShiftForCashier(cashierId: String): ShiftDenominationEntity?

    // 3. Close the shift specifically for this cashier
    @Query("UPDATE shift_denominations SET status = 'CLOSED', closedAt = :closedAt, isSynced = 0 WHERE shiftId = :shiftId AND cashierId = :cashierId")
    suspend fun closeShiftForCashier(shiftId: String, cashierId: String, closedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShift(shift: ShiftDenominationEntity)

    @Query("SELECT * FROM shift_denominations WHERE cashierId = :cashierId AND isSynced = 0")
    suspend fun getUnsyncedShiftsForCashier(cashierId: String): List<ShiftDenominationEntity>

    @Query("UPDATE shift_denominations SET isSynced = 1 WHERE shiftId = :shiftId AND cashierId = :cashierId")
    suspend fun markAsSynced(shiftId: String, cashierId: String)

    // Existing compatibility helpers
    @Query("SELECT * FROM shift_denominations WHERE shiftId = :shiftId LIMIT 1")
    fun observeShift(shiftId: String): Flow<ShiftDenominationEntity?>

    @Query("SELECT * FROM shift_denominations WHERE shiftId = :shiftId LIMIT 1")
    suspend fun getShiftById(shiftId: String): ShiftDenominationEntity?

    @Query("SELECT * FROM shift_denominations WHERE isSynced = 0")
    suspend fun getPendingUnsyncedShifts(): List<ShiftDenominationEntity>

    @Query("UPDATE shift_denominations SET isSynced = 1 WHERE shiftId = :shiftId")
    suspend fun markAsSynced(shiftId: String)
}
