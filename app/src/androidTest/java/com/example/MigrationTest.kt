package com.example

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pos.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates Room schema migrations incrementally to ensure no data loss in production.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDbName = "migration-test-db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate8To9_createsShiftDenominationsTable() {
        // Create DB at version 8
        var db = helper.createDatabase(testDbName, 8).apply {
            close()
        }

        // Run migration from 8 to 9
        db = helper.runMigrationsAndValidate(
            testDbName,
            9,
            true,
            AppDatabase.MIGRATION_8_9
        )

        // Verify shift_denominations table exists and is writeable
        db.execSQL("""
            INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt)
            VALUES ('shift-101', 'cashier-1', '{"1000":2,"500":1}', 2500, 0, 1690000000000)
        """.trimIndent())

        val cursor = db.query("SELECT shiftId, totalAmount, isSynced FROM shift_denominations WHERE shiftId = 'shift-101'")
        assertTrue(cursor.moveToFirst())
        assertEquals("shift-101", cursor.getString(0))
        assertEquals(2500L, cursor.getLong(1))
        assertEquals(0, cursor.getInt(2))
        cursor.close()
    }

    @Test
    fun migrate9To10_addsCashierStatusAndIndices() {
        // Create DB at version 9
        var db = helper.createDatabase(testDbName, 9).apply {
            execSQL("""
                INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt)
                VALUES ('shift-102', 'cashier-A', '{"1000":1}', 1000, 0, 1690000000000)
            """.trimIndent())
            close()
        }

        // Run migration from 9 to 10
        db = helper.runMigrationsAndValidate(
            testDbName,
            10,
            true,
            AppDatabase.MIGRATION_9_10
        )

        // Verify status defaults to OPEN and new columns exist
        val cursor = db.query("SELECT shiftId, cashierId, status, openedAt, closedAt FROM shift_denominations WHERE shiftId = 'shift-102'")
        assertTrue(cursor.moveToFirst())
        assertEquals("shift-102", cursor.getString(0))
        assertEquals("cashier-A", cursor.getString(1))
        assertEquals("OPEN", cursor.getString(2))
        assertEquals(0L, cursor.getLong(3))
        cursor.close()
    }

    @Test
    fun migrate10To12_enforcesIntegrityConstraintsOnActiveCashierShift() {
        // Create DB at version 10
        var db = helper.createDatabase(testDbName, 10).apply {
            execSQL("""
                INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt, status, openedAt, closedAt)
                VALUES ('shift-closed-1', 'cashier-multi', '{"1000":1}', 1000, 1, 1690000000000, 'CLOSED', 1690000000000, 1690003600000)
            """.trimIndent())
            close()
        }

        // Run migrations from 10 to 12
        db = helper.runMigrationsAndValidate(
            testDbName,
            12,
            true,
            AppDatabase.MIGRATION_10_11,
            AppDatabase.MIGRATION_11_12
        )

        // Verify deviceId column exists on existing row
        val cursor = db.query("SELECT shiftId, deviceId, status FROM shift_denominations WHERE shiftId = 'shift-closed-1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("shift-closed-1", cursor.getString(0))
        assertEquals("", cursor.getString(1)) // default value
        assertEquals("CLOSED", cursor.getString(2))
        cursor.close()

        // 1. Allow first OPEN shift for cashier-multi
        db.execSQL("""
            INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt, status, openedAt, closedAt, deviceId)
            VALUES ('shift-open-1', 'cashier-multi', '{"1000":2}', 2000, 1, 1690004000000, 'OPEN', 1690004000000, null, 'reg_01')
        """.trimIndent())

        // 2. Allow another CLOSED shift for cashier-multi (multiple CLOSED shifts are permitted)
        db.execSQL("""
            INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt, status, openedAt, closedAt, deviceId)
            VALUES ('shift-closed-2', 'cashier-multi', '{"500":2}', 1000, 1, 1690005000000, 'CLOSED', 1690005000000, 1690008000000, 'reg_01')
        """.trimIndent())

        // 3. Prevent second simultaneous OPEN shift for cashier-multi via database constraint trigger
        var constraintViolated = false
        try {
            db.execSQL("""
                INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt, status, openedAt, closedAt, deviceId)
                VALUES ('shift-open-conflict', 'cashier-multi', '{"1000":1}', 1000, 1, 1690006000000, 'OPEN', 1690006000000, null, 'reg_02')
            """.trimIndent())
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            constraintViolated = true
        }
        assertTrue("Expected SQLiteConstraintException when attempting to open two concurrent shifts for cashier-multi", constraintViolated)

        // 4. Verify a different cashier CAN open a shift simultaneously
        db.execSQL("""
            INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt, status, openedAt, closedAt, deviceId)
            VALUES ('shift-open-other', 'cashier-other', '{"1000":1}', 1000, 1, 1690007000000, 'OPEN', 1690007000000, null, 'reg_02')
        """.trimIndent())
    }
}
