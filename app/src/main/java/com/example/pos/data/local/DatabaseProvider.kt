package com.example.pos.data.local

import android.content.Context
import androidx.room.Room
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import javax.crypto.KeyGenerator

/**
 * Provides encrypted Room database instances using SQLCipher 256-bit AES encryption.
 * Automatically decrypts pages in memory before executing DDL migration statements.
 */
object DatabaseProvider {

    private const val ENCRYPTED_DB_NAME = "omnipos_secure.db"

    /**
     * Builds and returns an encrypted Room database instance.
     * Room passes the decryption key via SQLCipher's [SupportFactory] on database open,
     * allowing standard Room [Migration] objects to execute seamlessly against the decrypted session.
     *
     * @param context Application context
     * @param passphrase Encryption key as a [ByteArray] for zero-memory retention hygiene
     */
    fun getEncryptedDatabase(context: Context, passphrase: ByteArray): AppDatabase {
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            ENCRYPTED_DB_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12
            )
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    AppDatabase.createActiveShiftIntegrityTriggers(db)
                }

                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onOpen(db)
                    AppDatabase.createActiveShiftIntegrityTriggers(db)
                }
            })
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /**
     * Securely clears sensitive passphrase byte arrays from RAM after usage.
     */
    fun clearPassphrase(passphrase: ByteArray) {
        passphrase.fill(0)
    }
}
