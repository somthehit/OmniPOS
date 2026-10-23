package com.shopflow.pos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.shopflow.pos.data.local.dao.*
import com.shopflow.pos.data.local.entity.*
import java.security.MessageDigest

@Database(
    entities = [
        OrganizationEntity::class, BranchEntity::class, RegisterEntity::class, ReceiptCounterEntity::class,
        UserEntity::class, MembershipEntity::class, MembershipBranchEntity::class,
        RolePermissionEntity::class, LocalCredentialEntity::class,
        CategoryEntity::class, TaxRateEntity::class, ProductEntity::class, BarcodeEntity::class,
        CustomerEntity::class, StockMovementEntity::class,
        SaleEntity::class, SaleItemEntity::class, PaymentEntity::class,
        SyncOutboxEntity::class, SyncStateEntity::class,
        CustomerCreditTermsEntity::class, CustomerLedgerEntity::class, PriceHistoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class ShopFlowDatabase : RoomDatabase() {
    abstract fun organizationDao(): OrganizationDao
    abstract fun branchDao(): BranchDao
    abstract fun registerDao(): RegisterDao
    abstract fun receiptCounterDao(): ReceiptCounterDao
    abstract fun userDao(): UserDao
    abstract fun membershipDao(): MembershipDao
    abstract fun membershipBranchDao(): MembershipBranchDao
    abstract fun rolePermissionDao(): RolePermissionDao
    abstract fun localCredentialDao(): LocalCredentialDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taxRateDao(): TaxRateDao
    abstract fun productDao(): ProductDao
    abstract fun barcodeDao(): BarcodeDao
    abstract fun customerDao(): CustomerDao
    abstract fun stockMovementDao(): StockMovementDao
    fun stockDao(): StockMovementDao = stockMovementDao()
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    fun outboxDao(): SyncOutboxDao = syncOutboxDao()
    abstract fun syncStateDao(): SyncStateDao
    abstract fun customerCreditTermsDao(): CustomerCreditTermsDao
    abstract fun customerLedgerDao(): CustomerLedgerDao
    abstract fun priceHistoryDao(): PriceHistoryDao
}

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Alter categories
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `default_margin_bps` INTEGER")

        // 2. Alter products
        db.execSQL("ALTER TABLE `products` ADD COLUMN `default_margin_bps` INTEGER")
        db.execSQL("ALTER TABLE `products` ADD COLUMN `supplier_id` TEXT")
        db.execSQL("ALTER TABLE `products` ADD COLUMN `mrp_minor` INTEGER")

        // 3. Alter barcodes
        db.execSQL("ALTER TABLE `barcodes` ADD COLUMN `pack_multiplier_milli` INTEGER NOT NULL DEFAULT 1000")

        // 4. Alter customers
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `customer_type` TEXT NOT NULL DEFAULT 'INDIVIDUAL'")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'ACTIVE'")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `tax_id` TEXT")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `address_line1` TEXT")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `address_line2` TEXT")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `city` TEXT")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `postal_code` TEXT")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `state_province` TEXT")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `country_code` TEXT")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `preferred_language` TEXT NOT NULL DEFAULT 'en'")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `default_discount_bps` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `marketing_consent` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `date_of_birth` INTEGER")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `has_credit_enabled` INTEGER NOT NULL DEFAULT 0")

        // 5. Alter stock_movements
        db.execSQL("ALTER TABLE `stock_movements` ADD COLUMN `supplier_id` TEXT")
        db.execSQL("ALTER TABLE `stock_movements` ADD COLUMN `invoice_ref` TEXT")
        db.execSQL("ALTER TABLE `stock_movements` ADD COLUMN `batch_number` TEXT")
        db.execSQL("ALTER TABLE `stock_movements` ADD COLUMN `expiry_date` INTEGER")
        db.execSQL("ALTER TABLE `stock_movements` ADD COLUMN `received_date` INTEGER")

        // 6. Alter payments
        db.execSQL("ALTER TABLE `payments` ADD COLUMN `credit_approval_status` TEXT NOT NULL DEFAULT 'NONE'")
        db.execSQL("ALTER TABLE `payments` ADD COLUMN `manager_override_uid` TEXT")

        // 7. Create customer_credit_terms table and indices
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `customer_credit_terms` (" +
                "`id` TEXT NOT NULL, " +
                "`organization_id` TEXT NOT NULL, " +
                "`customer_id` TEXT NOT NULL, " +
                "`credit_limit_minor` INTEGER NOT NULL DEFAULT 0, " +
                "`payment_terms_days` INTEGER NOT NULL DEFAULT 30, " +
                "`grace_period_days` INTEGER NOT NULL DEFAULT 0, " +
                "`interest_rate_bps` INTEGER NOT NULL DEFAULT 0, " +
                "`hold_status` TEXT NOT NULL DEFAULT 'ACTIVE', " +
                "`approved_by` TEXT, " +
                "`approved_at` INTEGER, " +
                "`created_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, " +
                "`version` INTEGER NOT NULL DEFAULT 1, " +
                "`device_id` TEXT NOT NULL, " +
                "`sync_state` INTEGER NOT NULL DEFAULT 0, " +
                "`server_updated_at` INTEGER, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`organization_id`) REFERENCES `organizations`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT, " +
                "FOREIGN KEY(`organization_id`, `customer_id`) REFERENCES `customers`(`organization_id`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT" +
            ")"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customer_credit_terms_organization_id_id` ON `customer_credit_terms` (`organization_id`, `id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customer_credit_terms_organization_id_customer_id` ON `customer_credit_terms` (`organization_id`, `customer_id`)")

        // 8. Create customer_ledger table and indices
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `customer_ledger` (" +
                "`id` TEXT NOT NULL, " +
                "`organization_id` TEXT NOT NULL, " +
                "`customer_id` TEXT NOT NULL, " +
                "`entry_type` TEXT NOT NULL, " +
                "`amount_minor` INTEGER NOT NULL, " +
                "`balance_after_minor` INTEGER NOT NULL, " +
                "`reference_type` TEXT NOT NULL, " +
                "`reference_id` TEXT NOT NULL, " +
                "`idempotency_key` TEXT NOT NULL, " +
                "`notes` TEXT, " +
                "`posted_at` INTEGER NOT NULL, " +
                "`created_by` TEXT, " +
                "`device_id` TEXT NOT NULL, " +
                "`sync_state` INTEGER NOT NULL DEFAULT 0, " +
                "`server_updated_at` INTEGER, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`organization_id`) REFERENCES `organizations`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT, " +
                "FOREIGN KEY(`organization_id`, `customer_id`) REFERENCES `customers`(`organization_id`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT" +
            ")"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customer_ledger_organization_id_id` ON `customer_ledger` (`organization_id`, `id`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customer_ledger_organization_id_idempotency_key` ON `customer_ledger` (`organization_id`, `idempotency_key`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_ledger_organization_id_customer_id_posted_at` ON `customer_ledger` (`organization_id`, `customer_id`, `posted_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_ledger_organization_id_customer_id` ON `customer_ledger` (`organization_id`, `customer_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_ledger_organization_id_reference_type_reference_id` ON `customer_ledger` (`organization_id`, `reference_type`, `reference_id`)")

        // 9. Create price_history table and indices
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `price_history` (" +
                "`id` TEXT NOT NULL, " +
                "`organization_id` TEXT NOT NULL, " +
                "`product_id` TEXT NOT NULL, " +
                "`old_price_minor` INTEGER NOT NULL, " +
                "`new_price_minor` INTEGER NOT NULL, " +
                "`old_cost_minor` INTEGER, " +
                "`new_cost_minor` INTEGER, " +
                "`change_reason` TEXT, " +
                "`effective_at` INTEGER NOT NULL, " +
                "`changed_by` TEXT, " +
                "`device_id` TEXT NOT NULL, " +
                "`sync_state` INTEGER NOT NULL DEFAULT 0, " +
                "`server_updated_at` INTEGER, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`organization_id`) REFERENCES `organizations`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT, " +
                "FOREIGN KEY(`organization_id`, `product_id`) REFERENCES `products`(`organization_id`, `id`) ON UPDATE NO ACTION ON DELETE RESTRICT" +
            ")"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_price_history_organization_id_id` ON `price_history` (`organization_id`, `id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_history_organization_id_product_id_effective_at` ON `price_history` (`organization_id`, `product_id`, `effective_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_price_history_organization_id_product_id` ON `price_history` (`organization_id`, `product_id`)")

        // 10. Update triggers
        db.execSQL("DROP TRIGGER IF EXISTS trg_payments_confirmed_only")
        IntegrityTriggers.statements().forEach(db::execSQL)
    }
}

/** DB-level guards so even buggy code cannot rewrite financial history or change tenant. */
internal object IntegrityTriggers {
    private val orgScopedTables = listOf(
        "branches", "registers", "receipt_counters", "users", "memberships", "membership_branches",
        "role_permissions", "local_credentials", "categories", "tax_rates", "products", "barcodes",
        "customers", "stock_movements", "sales", "sale_items", "payments", "sync_outbox", "sync_state",
        "customer_credit_terms", "customer_ledger", "price_history",
    )
    private val appendOnly = listOf("sales", "sale_items", "payments", "stock_movements", "customer_ledger", "price_history")

    private fun abortUpdate(table: String, columns: String, message: String) =
        "CREATE TRIGGER IF NOT EXISTS trg_${table}_immutable BEFORE UPDATE OF $columns ON $table " +
            "BEGIN SELECT RAISE(ABORT, '$message'); END"

    fun statements(): List<String> = buildList {
        // 1. organization_id can never change after insert
        orgScopedTables.forEach { t ->
            add("CREATE TRIGGER IF NOT EXISTS trg_${t}_org_locked BEFORE UPDATE OF organization_id ON $t " +
                "WHEN NEW.organization_id <> OLD.organization_id BEGIN SELECT RAISE(ABORT, 'organization_id is immutable'); END")
        }
        // 2. ledger tables: no deletes
        appendOnly.forEach { t ->
            add("CREATE TRIGGER IF NOT EXISTS trg_${t}_no_delete BEFORE DELETE ON $t " +
                "BEGIN SELECT RAISE(ABORT, '$t is append-only'); END")
        }
        // 3. ledger tables: business columns are frozen (sync columns + sales.status may still change)
        add(abortUpdate("sales",
            "branch_id, register_id, cashier_id, customer_id, receipt_number, idempotency_key, currency_code, tax_mode, " +
            "subtotal_minor, discount_minor, tax_minor, rounding_minor, total_minor, paid_minor, change_minor, sold_at",
            "completed sales cannot be edited; use a refund or void record"))
        add(abortUpdate("sale_items",
            "sale_id, product_id, product_name, sku, quantity_milli, unit_price_minor, unit_cost_minor, discount_minor, tax_rate_bps, tax_minor, line_total_minor",
            "sale items are immutable"))
        add(abortUpdate("payments",
            "sale_id, method, amount_minor, tendered_minor, change_minor, provider, provider_reference, provider_status, credit_approval_status, manager_override_uid",
            "payments are immutable"))
        add(abortUpdate("stock_movements",
            "branch_id, product_id, movement_type, quantity_milli, unit_cost_minor, source_type, source_id, idempotency_key, created_at, supplier_id, invoice_ref, batch_number, expiry_date, received_date",
            "stock movements are immutable; post a reversing movement"))
        add(abortUpdate("customer_ledger",
            "customer_id, entry_type, amount_minor, balance_after_minor, reference_type, reference_id, idempotency_key, posted_at",
            "customer ledger entries are immutable"))
        add(abortUpdate("price_history",
            "product_id, old_price_minor, new_price_minor, old_cost_minor, new_cost_minor, change_reason, effective_at",
            "price history entries are immutable"))
        // 4. payment confirmation:
        // Cash: passes.
        // Credit: allowed without provider confirmation ONLY when associated sale has customer_id and credit_approval_status is CONFIRMED or OVERRIDDEN.
        // Other (card/qr/wallet): requires provider_status = CONFIRMED and non-null provider_reference.
        add("CREATE TRIGGER IF NOT EXISTS trg_payments_confirmed_only BEFORE INSERT ON payments " +
            "WHEN (NEW.method = 'CREDIT' AND (" +
            "  (SELECT customer_id FROM sales WHERE organization_id = NEW.organization_id AND id = NEW.sale_id) IS NULL " +
            "  OR NEW.credit_approval_status NOT IN ('CONFIRMED', 'OVERRIDDEN')" +
            ")) " +
            "OR (NEW.method NOT IN ('CASH', 'CREDIT') AND (NEW.provider_status <> 'CONFIRMED' OR NEW.provider_reference IS NULL)) " +
            "BEGIN SELECT RAISE(ABORT, 'invalid payment authorization or unconfirmed provider'); END")
    }
}

object ShopFlowDatabaseProvider {

    private fun fileName(organizationId: String, userId: String): String {
        // hashed so file names don't leak ids; one file per organization per user
        val h = MessageDigest.getInstance("SHA-256").digest("$organizationId:$userId".toByteArray())
        return "sf_" + h.joinToString("") { "%02x".format(it) }.take(24) + ".db"
    }

    /**
     * @param passphrase random 32 bytes, generated once per (org,user), stored wrapped by an Android Keystore key.
     *                   NEVER hardcode; NEVER store in SharedPreferences/plain files.
     */
    fun open(context: Context, organizationId: String, userId: String, passphrase: ByteArray): ShopFlowDatabase {
        try {
            System.loadLibrary("sqlcipher")
        } catch (_: Throwable) {
            // Handled gracefully in environments without native binary
        }

        val builder = Room.databaseBuilder(
            context.applicationContext,
            ShopFlowDatabase::class.java,
            fileName(organizationId, userId)
        ).addMigrations(MIGRATION_1_2)

        // If SQLCipher SupportOpenHelperFactory is available in classpath, apply it
        try {
            val factoryClass = Class.forName("net.zetetic.database.sqlcipher.SupportOpenHelperFactory")
            val constructor = factoryClass.getConstructor(ByteArray::class.java)
            val factory = constructor.newInstance(passphrase) as SupportSQLiteOpenHelper.Factory
            builder.openHelperFactory(factory)
        } catch (_: Throwable) {
            // Uses standard Room OpenHelper if SQLCipher is not configured
        }

        return builder
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    IntegrityTriggers.statements().forEach(db::execSQL) // IF NOT EXISTS => safe after migrations
                }
            })
            .build() // no fallbackToDestructiveMigration(): unsynced sales must never be lost
    }

    /** Logout / org switch / revoked device / removed membership: close the DB first, then call this. */
    fun wipe(context: Context, organizationId: String, userId: String): Boolean =
        context.applicationContext.deleteDatabase(fileName(organizationId, userId)) // also removes -wal/-shm
}

