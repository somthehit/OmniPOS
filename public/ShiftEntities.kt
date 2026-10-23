package com.omnipos.shift

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Add these to your existing @Database(entities = [...]) and expose `abstract fun shiftDao(): ShiftDao`.
 * Room cannot declare partial unique indexes, so the "one active shift per terminal / cashier"
 * rule is enforced inside ShiftDao.openShiftAtomically (single transaction) AND on the server.
 */
@Entity(tableName = "shifts", indices = [Index("terminalId"), Index("cashierId"), Index("status")])
data class ShiftEntity(
    @PrimaryKey val id: String,                 // client-generated UUID => idempotent open
    val tenantId: String,
    val branchId: String,
    val terminalId: String,
    val deviceId: String,
    val cashierId: String,
    val cashierName: String,
    val businessDate: String,                   // yyyy-MM-dd, date the shift was OPENED
    val status: String,                         // ShiftStatus.name
    val openedAt: Long,
    val closedAt: Long? = null,
    val openingCash: Long,
    val openingExpected: Long? = null,          // previous closing float
    val openingGap: Long = 0,
    val openingGapReason: String? = null,
    val expectedCash: Long? = null,
    val countedCash: Long? = null,
    val closingFloat: Long? = null,
    val depositAmount: Long? = null,
    val cashVariance: Long? = null,
    val closeReason: String? = null,
    val closedBy: String? = null,               // cashier id, or "ADMIN:<id>" for force close
    val reviewedBy: String? = null,
    val reviewNote: String? = null,
    val version: Int = 1,
    val synced: Boolean = false,
    val syncConflict: Boolean = false,
) {
    val statusEnum: ShiftStatus get() = ShiftStatus.valueOf(status)
    val isActive: Boolean get() = status in ACTIVE_STATUSES
}

@Entity(tableName = "shift_cash_counts", indices = [Index("shiftId", "phase")])
data class CashCountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: String,
    val phase: String,                          // CountPhase.name
    val denomination: Int,
    val count: Int,
)

@Entity(tableName = "shift_cash_movements", indices = [Index("shiftId")])
data class CashMovementEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val type: String,                           // MovementType.name
    val amount: Long,
    val reason: String,
    val createdBy: String,
    val createdAt: Long,
)

@Entity(tableName = "shift_declarations", indices = [Index("shiftId")])
data class DeclarationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: String,
    val method: String,                         // PayMethod.name
    val posAmount: Long,                        // what the POS recorded
    val providerAmount: Long? = null,           // provider / bank confirmed (read-only, from integration)
    val declaredAmount: Long? = null,           // cashier declaration when no integration
    val verification: String,                   // Verification.name
    val reference: String? = null,
    val reasonCode: String? = null,
    val note: String? = null,
) {
    val confirmedAmount: Long get() = providerAmount ?: declaredAmount ?: 0L
    val mismatch: Long get() = posAmount - confirmedAmount
}

/** credit note activity vs. actual cash outflow are separate lines. */
@Entity(tableName = "shift_credit_notes")
data class CreditNoteEntity(
    @PrimaryKey val shiftId: String,
    val issuedCount: Int,
    val issuedTotal: Long,
    val cashRefund: Long,                       // the ONLY part that reduces drawer cash
    val customerBalance: Long,                  // credit applied to customer balance
    val bankRefund: Long,                       // refunded through bank / wallet
    val redeemedAsTender: Long,                 // credit notes used as payment in this shift
)

@Entity(tableName = "shift_outbox")
data class OutboxEntity(
    @PrimaryKey val id: String,
    val shiftId: String,
    val path: String,                           // e.g. "shifts/open"
    val payloadJson: String,
    val idempotencyKey: String,
    val attempts: Int = 0,
    val createdAt: Long,
)

data class MovementTotal(val type: String, val total: Long)

@Dao
abstract class ShiftDao {
    @Query("SELECT * FROM shifts WHERE id = :id")
    abstract suspend fun getById(id: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE id = :id")
    abstract fun observe(id: String): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE cashierId = :cashierId AND status IN ('OPEN','CLOSING') LIMIT 1")
    abstract suspend fun activeForCashier(cashierId: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE terminalId = :terminalId AND status IN ('OPEN','CLOSING') LIMIT 1")
    abstract suspend fun activeForTerminal(terminalId: String): ShiftEntity?

    @Query(
        "SELECT * FROM shifts WHERE terminalId = :terminalId AND status NOT IN ('OPEN','CLOSING') " +
            "ORDER BY closedAt DESC LIMIT 1"
    )
    abstract suspend fun lastClosedForTerminal(terminalId: String): ShiftEntity?

    @Insert abstract suspend fun insertShift(s: ShiftEntity)
    @Update abstract suspend fun updateShift(s: ShiftEntity)

    @Insert abstract suspend fun insertCounts(list: List<CashCountEntity>)

    @Query("DELETE FROM shift_cash_counts WHERE shiftId = :shiftId AND phase = :phase")
    abstract suspend fun clearCounts(shiftId: String, phase: String)

    @Query("SELECT * FROM shift_cash_counts WHERE shiftId = :shiftId AND phase = :phase")
    abstract suspend fun counts(shiftId: String, phase: String): List<CashCountEntity>

    @Insert abstract suspend fun insertMovement(m: CashMovementEntity)

    @Query("SELECT * FROM shift_cash_movements WHERE shiftId = :shiftId ORDER BY createdAt")
    abstract fun observeMovements(shiftId: String): Flow<List<CashMovementEntity>>

    @Query("SELECT type, SUM(amount) AS total FROM shift_cash_movements WHERE shiftId = :shiftId GROUP BY type")
    abstract suspend fun movementTotals(shiftId: String): List<MovementTotal>

    @Insert abstract suspend fun insertDeclarations(list: List<DeclarationEntity>)

    @Query("DELETE FROM shift_declarations WHERE shiftId = :shiftId")
    abstract suspend fun clearDeclarations(shiftId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertCreditNote(c: CreditNoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOutbox(o: OutboxEntity)

    @Query("SELECT * FROM shift_outbox ORDER BY createdAt")
    abstract suspend fun pendingOutbox(): List<OutboxEntity>

    @Query("DELETE FROM shift_outbox WHERE id = :id")
    abstract suspend fun deleteOutbox(id: String)

    @Query("UPDATE shift_outbox SET attempts = attempts + 1 WHERE id = :id")
    abstract suspend fun bumpAttempts(id: String)

    @Query("UPDATE shifts SET synced = 1 WHERE id = :shiftId")
    abstract suspend fun markSynced(shiftId: String)

    @Query("UPDATE shifts SET syncConflict = 1 WHERE id = :shiftId")
    abstract suspend fun markConflict(shiftId: String)

    /** Idempotent: same shift id returns the existing row. Enforces one active shift per terminal + cashier. */
    @Transaction
    open suspend fun openShiftAtomically(
        shift: ShiftEntity,
        counts: List<CashCountEntity>,
        outbox: OutboxEntity,
    ): ShiftEntity {
        getById(shift.id)?.let { return it }
        activeForTerminal(shift.terminalId)?.let {
            throw ShiftException("This terminal already has an active shift (${it.cashierName}).")
        }
        activeForCashier(shift.cashierId)?.let {
            throw ShiftException("You already have an active shift.")
        }
        insertShift(shift)
        insertCounts(counts)
        insertOutbox(outbox)
        return shift
    }

    @Transaction
    open suspend fun closeShiftAtomically(
        shift: ShiftEntity,
        closingCounts: List<CashCountEntity>,
        declarations: List<DeclarationEntity>,
        creditNote: CreditNoteEntity,
        outbox: OutboxEntity,
    ) {
        updateShift(shift)
        clearCounts(shift.id, CountPhase.CLOSING.name)
        insertCounts(closingCounts)
        clearDeclarations(shift.id)
        insertDeclarations(declarations)
        upsertCreditNote(creditNote)
        insertOutbox(outbox)
    }

    @Transaction
    open suspend fun saveClosingDraft(shift: ShiftEntity, closingCounts: List<CashCountEntity>) {
        updateShift(shift)
        clearCounts(shift.id, CountPhase.CLOSING.name)
        insertCounts(closingCounts)
    }
}
