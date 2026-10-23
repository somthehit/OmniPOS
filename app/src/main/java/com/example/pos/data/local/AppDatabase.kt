package com.example.pos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.pos.data.model.MovementType
import com.example.pos.data.model.OutboxOp
import com.example.pos.data.model.PaymentType
import com.example.pos.data.model.StaffRole
import com.example.pos.data.model.TaxMode
import java.util.Date
import java.util.UUID

class Converters {
    // Date TypeConverters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // UUID TypeConverters
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun toUUID(value: String?): UUID? = value?.let {
        try {
            UUID.fromString(it)
        } catch (_: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromStaffRole(value: StaffRole?): String? = value?.name

    @TypeConverter
    fun toStaffRole(value: String?): StaffRole? = value?.let { enumValueOf<StaffRole>(it) }

    @TypeConverter
    fun fromPaymentType(value: PaymentType?): String? = value?.name

    @TypeConverter
    fun toPaymentType(value: String?): PaymentType? = value?.let { enumValueOf<PaymentType>(it) }

    @TypeConverter
    fun fromMovementType(value: MovementType?): String? = value?.name

    @TypeConverter
    fun toMovementType(value: String?): MovementType? = value?.let { enumValueOf<MovementType>(it) }

    @TypeConverter
    fun fromTaxMode(value: TaxMode?): String? = value?.name

    @TypeConverter
    fun toTaxMode(value: String?): TaxMode? = value?.let { enumValueOf<TaxMode>(it) }

    @TypeConverter
    fun fromOutboxOp(value: OutboxOp?): String? = value?.name

    @TypeConverter
    fun toOutboxOp(value: String?): OutboxOp? = value?.let { enumValueOf<OutboxOp>(it) }
}

@Database(
    entities = [
        // Initial entities
        Organization::class,
        User::class,
        Product::class,
        Sale::class,
        // Supporting entities
        BranchEntity::class,
        RegisterEntity::class,
        StaffMemberEntity::class,
        CategoryEntity::class,
        StockMovementEntity::class,
        SaleItemEntity::class,
        SalePaymentEntity::class,
        HeldOrderEntity::class,
        CustomerEntity::class,
        CustomerLedgerEntity::class,
        ShiftEntity::class,
        ShiftCashCountEntity::class,
        ShiftDeclarationEntity::class,
        ShiftCreditNoteEntity::class,
        ShiftAdjustmentEntity::class,
        ShiftVersionEntity::class,
        ShiftEventEntity::class,
        CashMovementEntity::class,
        SyncOutboxEntity::class,
        StoreSettingsEntity::class,
        AuditLogEntity::class,
        ItemBarcodeEntity::class,
        FiscalYearEntity::class,
        ShiftDenominationEntity::class
    ],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class, DenominationMapConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun organizationDao(): OrganizationDao
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun posDao(): PosDao
    abstract fun shiftDao(): ShiftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Baseline migration from initial Room schema
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `item_barcodes` (
                        `id` TEXT NOT NULL,
                        `org_id` TEXT NOT NULL,
                        `item_id` TEXT NOT NULL,
                        `barcode` TEXT NOT NULL,
                        `pack_qty` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_item_barcodes_org_id_id` ON `item_barcodes` (`org_id`, `id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_item_barcodes_org_id_barcode` ON `item_barcodes` (`org_id`, `barcode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_barcodes_org_id_item_id` ON `item_barcodes` (`org_id`, `item_id`)")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `store_settings` ADD COLUMN `backup_email` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `store_settings` ADD COLUMN `last_backup_at` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `store_settings` ADD COLUMN `language` TEXT NOT NULL DEFAULT 'en'")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `fiscal_years` (
                        `id` TEXT NOT NULL,
                        `org_id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `start_date` INTEGER NOT NULL,
                        `end_date` INTEGER NOT NULL,
                        `invoice_prefix` TEXT NOT NULL DEFAULT 'INV',
                        `current_invoice_sequence` INTEGER NOT NULL DEFAULT 0,
                        `is_current` INTEGER NOT NULL DEFAULT 0,
                        `is_closed` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_fiscal_years_org_id_id` ON `fiscal_years` (`org_id`, `id`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_fiscal_years_org_id_name` ON `fiscal_years` (`org_id`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fiscal_years_org_id_is_current` ON `fiscal_years` (`org_id`, `is_current`)")
                db.execSQL("ALTER TABLE `sales` ADD COLUMN `fiscal_year` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Ensure store_settings and shift_events indices and constraints
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `store_settings` ADD COLUMN `auto_logout_interval` TEXT NOT NULL DEFAULT 'Never'")
                db.execSQL("ALTER TABLE `store_settings` ADD COLUMN `login_biometric_enabled` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shift_denominations` (
                        `shiftId` TEXT NOT NULL,
                        `cashierId` TEXT NOT NULL,
                        `denominations` TEXT NOT NULL,
                        `totalAmount` INTEGER NOT NULL,
                        `isSynced` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`shiftId`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `shift_denominations` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'OPEN'")
                db.execSQL("ALTER TABLE `shift_denominations` ADD COLUMN `openedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `shift_denominations` ADD COLUMN `closedAt` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_denominations_cashierId_status` ON `shift_denominations` (`cashierId`, `status`)")
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `shift_denominations` ADD COLUMN `deviceId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("DROP INDEX IF EXISTS `idx_unique_active_cashier_shift`")
                db.execSQL("DROP INDEX IF EXISTS `idx_unique_active_cashier_shift_denom`")
                createActiveShiftIntegrityTriggers(db)
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `idx_unique_active_cashier_shift`")
                db.execSQL("DROP INDEX IF EXISTS `idx_unique_active_cashier_shift_denom`")
                createActiveShiftIntegrityTriggers(db)
            }
        }

        fun createActiveShiftIntegrityTriggers(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            // 1. Sanitize any legacy duplicate OPEN shifts before enforcing constraints
            try {
                db.execSQL("""
                    UPDATE `shifts` 
                    SET `status` = 'CLOSED_UNRECONCILED', `closed_at` = strftime('%s','now')*1000 
                    WHERE `status` = 'OPEN' AND `id` NOT IN (
                        SELECT `id` FROM (
                            SELECT `id`, MAX(`opened_at`) FROM `shifts` WHERE `status` = 'OPEN' GROUP BY `cashier_uid`
                        )
                    )
                """.trimIndent())

                db.execSQL("""
                    UPDATE `shift_denominations` 
                    SET `status` = 'CLOSED', `closedAt` = strftime('%s','now')*1000 
                    WHERE `status` = 'OPEN' AND `shiftId` NOT IN (
                        SELECT `shiftId` FROM (
                            SELECT `shiftId`, MAX(`openedAt`) FROM `shift_denominations` WHERE `status` = 'OPEN' GROUP BY `cashierId`
                        )
                    )
                """.trimIndent())
            } catch (_: Exception) {}

            // 2. Prevent simultaneous active shifts in shifts table on INSERT
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS `trg_prevent_duplicate_active_shift_insert`
                BEFORE INSERT ON `shifts`
                FOR EACH ROW
                WHEN NEW.status = 'OPEN' AND EXISTS (
                    SELECT 1 FROM `shifts` WHERE `cashier_uid` = NEW.cashier_uid AND `status` = 'OPEN'
                )
                BEGIN
                    SELECT RAISE(ABORT, 'UNIQUE constraint failed: cashier already has an active OPEN shift');
                END;
            """.trimIndent())

            // 3. Prevent simultaneous active shifts in shifts table on UPDATE
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS `trg_prevent_duplicate_active_shift_update`
                BEFORE UPDATE OF status, cashier_uid ON `shifts`
                FOR EACH ROW
                WHEN NEW.status = 'OPEN' AND EXISTS (
                    SELECT 1 FROM `shifts` WHERE `cashier_uid` = NEW.cashier_uid AND `status` = 'OPEN' AND `id` != NEW.id
                )
                BEGIN
                    SELECT RAISE(ABORT, 'UNIQUE constraint failed: cashier already has an active OPEN shift');
                END;
            """.trimIndent())

            // 4. Prevent simultaneous active shift denominations on INSERT
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS `trg_prevent_duplicate_active_denom_insert`
                BEFORE INSERT ON `shift_denominations`
                FOR EACH ROW
                WHEN NEW.status = 'OPEN' AND EXISTS (
                    SELECT 1 FROM `shift_denominations` WHERE `cashierId` = NEW.cashierId AND `status` = 'OPEN'
                )
                BEGIN
                    SELECT RAISE(ABORT, 'UNIQUE constraint failed: cashier already has an active OPEN shift denomination');
                END;
            """.trimIndent())

            // 5. Prevent simultaneous active shift denominations on UPDATE
            db.execSQL("""
                CREATE TRIGGER IF NOT EXISTS `trg_prevent_duplicate_active_denom_update`
                BEFORE UPDATE OF status, cashierId ON `shift_denominations`
                FOR EACH ROW
                WHEN NEW.status = 'OPEN' AND EXISTS (
                    SELECT 1 FROM `shift_denominations` WHERE `cashierId` = NEW.cashierId AND `status` = 'OPEN' AND `shiftId` != NEW.shiftId
                )
                BEGIN
                    SELECT RAISE(ABORT, 'UNIQUE constraint failed: cashier already has an active OPEN shift denomination');
                END;
            """.trimIndent())
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "omnipos_store.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12
                    )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onCreate(db)
                            createActiveShiftIntegrityTriggers(db)
                        }

                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onOpen(db)
                            createActiveShiftIntegrityTriggers(db)
                        }
                    })
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
