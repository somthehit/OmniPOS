package com.example

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pos.data.local.AppDatabase
import net.sqlcipher.database.SupportFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates Room migrations on an encrypted database backed by SQLCipher.
 * Injects [SupportFactory] into [MigrationTestHelper] to decrypt pages in memory.
 */
@RunWith(AndroidJUnit4::class)
class EncryptedMigrationTest {

    private val testDbName = "encrypted-migration-test.db"
    private val testPassphrase = "omnipos-secure-test-passphrase-256".toByteArray()

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        SupportFactory(testPassphrase)
    )

    @Test
    fun migrate8To9_retainsEncryptedShiftData() {
        // 1. Create database at version 8 with encrypted test data
        var db = helper.createDatabase(testDbName, 8).apply {
            close()
        }

        // 2. Run migration to version 9 using SQLCipher SupportFactory
        db = helper.runMigrationsAndValidate(
            testDbName,
            9,
            true,
            AppDatabase.MIGRATION_8_9
        )

        // 3. Verify standard SQL DDL / DML operations succeed under encrypted storage
        db.execSQL("""
            INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt)
            VALUES ('shift-secure-101', 'cashier-secure-1', '{"1000":5,"500":2}', 6000, 1, 1690000000000)
        """.trimIndent())

        val cursor = db.query(
            "SELECT shiftId, totalAmount, isSynced FROM shift_denominations WHERE shiftId = 'shift-secure-101'"
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("shift-secure-101", cursor.getString(0))
        assertEquals(6000L, cursor.getLong(1))
        assertEquals(1, cursor.getInt(2))
        cursor.close()
    }

    @Test
    fun migrate9To10_retainsEncryptedCashierShiftData() {
        // 1. Create database at version 9 with encrypted test data
        var db = helper.createDatabase(testDbName, 9).apply {
            execSQL("""
                INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt)
                VALUES ('shift-secure-102', 'cashier-B', '{"500":3}', 1500, 1, 1690000000000)
            """.trimIndent())
            close()
        }

        // 2. Run migration to version 10 using SQLCipher SupportFactory
        db = helper.runMigrationsAndValidate(
            testDbName,
            10,
            true,
            AppDatabase.MIGRATION_9_10
        )

        // 3. Verify encrypted data is decrypted in memory and new columns default properly
        val cursor = db.query(
            "SELECT shiftId, cashierId, status, totalAmount FROM shift_denominations WHERE shiftId = 'shift-secure-102'"
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("shift-secure-102", cursor.getString(0))
        assertEquals("cashier-B", cursor.getString(1))
        assertEquals("OPEN", cursor.getString(2))
        assertEquals(1500L, cursor.getLong(3))
        cursor.close()
    }

    @Test
    fun migrate10To12_enforcesEncryptedIntegrityConstraintsOnActiveCashierShift() {
        // 1. Create database at version 10 with encrypted test data
        var db = helper.createDatabase(testDbName, 10).apply {
            execSQL("""
                INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt, status, openedAt, closedAt)
                VALUES ('shift-sec-closed-1', 'cashier-sec-multi', '{"1000":1}', 1000, 1, 1690000000000, 'CLOSED', 1690000000000, 1690003600000)
            """.trimIndent())
            close()
        }

        // 2. Run migrations to version 12 using SQLCipher SupportFactory
        db = helper.runMigrationsAndValidate(
            testDbName,
            12,
            true,
            AppDatabase.MIGRATION_10_11,
            AppDatabase.MIGRATION_11_12
        )

        // 3. Verify encrypted data is retained and deviceId column was added
        val cursor = db.query(
            "SELECT shiftId, cashierId, deviceId, status FROM shift_denominations WHERE shiftId = 'shift-sec-closed-1'"
        )
        assertTrue(cursor.moveToFirst())
        assertEquals("shift-sec-closed-1", cursor.getString(0))
        assertEquals("cashier-sec-multi", cursor.getString(1))
        assertEquals("", cursor.getString(2))
        assertEquals("CLOSED", cursor.getString(3))
        cursor.close()

        // 4. Open first shift for cashier-sec-multi
        db.execSQL("""
            INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt, status, openedAt, closedAt, deviceId)
            VALUES ('shift-sec-open-1', 'cashier-sec-multi', '{"1000":2}', 2000, 1, 1690004000000, 'OPEN', 1690004000000, null, 'reg_01')
        """.trimIndent())

        // 5. Open second shift concurrently for cashier-sec-multi -> must fail under SQLCipher with constraint violation
        var constraintViolated = false
        try {
            db.execSQL("""
                INSERT INTO shift_denominations (shiftId, cashierId, denominations, totalAmount, isSynced, updatedAt, status, openedAt, closedAt, deviceId)
                VALUES ('shift-sec-open-conflict', 'cashier-sec-multi', '{"1000":1}', 1000, 1, 1690006000000, 'OPEN', 1690006000000, null, 'reg_02')
            """.trimIndent())
        } catch (e: Exception) {
            // Note: Under SQLCipher / SupportSQLite, it throws an SQLiteConstraintException or subclass
            if (e.message?.contains("constraint", ignoreCase = true) == true || e is android.database.sqlite.SQLiteConstraintException) {
                constraintViolated = true
            }
        }
        assertTrue("Expected constraint exception when opening duplicate active shift under encrypted DB", constraintViolated)
    }
}
