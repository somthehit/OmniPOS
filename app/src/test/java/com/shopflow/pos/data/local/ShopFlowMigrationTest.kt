package com.shopflow.pos.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShopFlowMigrationTest {

    private val TEST_DB = "migration_test_shopflow.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShopFlowDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_preservesExistingDataAndValidatesSchema() {
        val now = 1729000000000L
        val orgId = "org_mig_1"
        val branchId = "br_mig_1"
        val regId = "reg_mig_1"
        val userId = "usr_mig_1"
        val customerId = "cust_mig_1"
        val catId = "cat_mig_1"
        val taxId = "tax_mig_1"
        val prodId = "prod_mig_1"
        val barcodeId = "bar_mig_1"
        val saleId = "sale_mig_1"
        val saleItemId = "item_mig_1"
        val paymentId = "pay_mig_1"
        val stockMoveId = "stock_mig_1"
        val outboxId = "outbox_mig_1"

        // Step 1: Create real Version 1 database and insert fixture data
        helper.createDatabase(TEST_DB, 1).apply {
            // Organization
            execSQL(
                "INSERT INTO organizations (id, display_name, business_type, country_code, currency_code, currency_minor_digits, timezone_id, tax_mode, rounding_rule, status, created_at, updated_at, version, device_id, sync_state) " +
                    "VALUES ('$orgId', 'ShopFlow Store', 'RETAIL', 'US', 'USD', 2, 'UTC', 'EXCLUSIVE', 'HALF_UP', 'ACTIVE', $now, $now, 1, 'dev_01', 0)"
            )

            // Branch
            execSQL(
                "INSERT INTO branches (id, organization_id, code, name, status, created_at, updated_at, version, device_id, sync_state) " +
                    "VALUES ('$branchId', '$orgId', 'BR01', 'Downtown Branch', 'ACTIVE', $now, $now, 1, 'dev_01', 0)"
            )

            // Register
            execSQL(
                "INSERT INTO registers (id, organization_id, branch_id, name, receipt_prefix, status, created_at, updated_at, version, device_id, sync_state) " +
                    "VALUES ('$regId', '$orgId', '$branchId', 'POS 1', 'BR1-REG1', 'ACTIVE', $now, $now, 1, 'dev_01', 0)"
            )

            // User
            execSQL(
                "INSERT INTO users (id, organization_id, full_name, email, created_at, updated_at, version, device_id, sync_state) " +
                    "VALUES ('$userId', '$orgId', 'Alice Cashier', 'alice@test.org', $now, $now, 1, 'dev_01', 1)"
            )

            // Membership
            execSQL(
                "INSERT INTO memberships (id, organization_id, user_id, role_key, status, created_at, updated_at, version, device_id, sync_state) " +
                    "VALUES ('mem_mig_1', '$orgId', '$userId', 'CASHIER', 'ACTIVE', $now, $now, 1, 'dev_01', 1)"
            )

            // Category
            execSQL(
                "INSERT INTO categories (id, organization_id, name, sort_order, created_at, updated_at, version, device_id, sync_state) " +
                    "VALUES ('$catId', '$orgId', 'Beverages', 0, $now, $now, 1, 'dev_01', 0)"
            )

            // Tax Rate
            execSQL(
                "INSERT INTO tax_rates (id, organization_id, name, rate_bps, is_default, created_at, updated_at, version, device_id, sync_state) " +
                    "VALUES ('$taxId', '$orgId', 'Standard VAT', 1300, 1, $now, $now, 1, 'dev_01', 0)"
            )

            // Product
            execSQL(
                "INSERT INTO products (id, organization_id, category_id, tax_rate_id, name, sku, unit_code, price_minor, track_stock, reorder_level_milli, status, created_at, updated_at, version, device_id, sync_state) " +
                    "VALUES ('$prodId', '$orgId', '$catId', '$taxId', 'Mineral Water 1L', 'SKU-WATER-01', 'pcs', 250, 1, 10000, 'ACTIVE', $now, $now, 1, 'dev_01', 0)"
            )

            // Barcode
            execSQL(
                "INSERT INTO barcodes (id, organization_id, product_id, code, created_at, updated_at, version, device_id, sync_state) " +
                    "VALUES ('$barcodeId', '$orgId', '$prodId', '8901234567890', $now, $now, 1, 'dev_01', 0)"
            )

            // Customer
            execSQL(
                "INSERT INTO customers (id, organization_id, name, phone, email, created_at, updated_at, version, device_id, sync_state) " +
                    "VALUES ('$customerId', '$orgId', 'Acme Corp', '+1555123456', 'contact@acme.com', $now, $now, 1, 'dev_01', 0)"
            )

            // Stock Movement
            execSQL(
                "INSERT INTO stock_movements (id, organization_id, branch_id, product_id, movement_type, quantity_milli, source_type, source_id, idempotency_key, created_at, device_id, sync_state) " +
                    "VALUES ('$stockMoveId', '$orgId', '$branchId', '$prodId', 'OPENING', 50000, 'manual', 'opening-01', 'idem_stock_01', $now, 'dev_01', 0)"
            )

            // Sale
            execSQL(
                "INSERT INTO sales (id, organization_id, branch_id, register_id, cashier_id, customer_id, receipt_number, idempotency_key, status, currency_code, tax_mode, subtotal_minor, discount_minor, tax_minor, rounding_minor, total_minor, paid_minor, change_minor, sold_at, timezone_id, created_at, updated_at, version, device_id, sync_state) " +
                    "VALUES ('$saleId', '$orgId', '$branchId', '$regId', '$userId', '$customerId', 'BR1-REG1-000001', 'idem_sale_01', 'COMPLETED', 'USD', 'EXCLUSIVE', 500, 0, 65, 0, 565, 565, 0, $now, 'UTC', $now, $now, 1, 'dev_01', 0)"
            )

            // Sale Item
            execSQL(
                "INSERT INTO sale_items (id, organization_id, sale_id, product_id, product_name, sku, quantity_milli, unit_price_minor, discount_minor, tax_rate_bps, tax_minor, line_total_minor, created_at, device_id, sync_state) " +
                    "VALUES ('$saleItemId', '$orgId', '$saleId', '$prodId', 'Mineral Water 1L', 'SKU-WATER-01', 2000, 250, 0, 1300, 65, 500, $now, 'dev_01', 0)"
            )

            // Payment (Cash)
            execSQL(
                "INSERT INTO payments (id, organization_id, sale_id, method, amount_minor, change_minor, provider_status, created_at, device_id, sync_state) " +
                    "VALUES ('$paymentId', '$orgId', '$saleId', 'CASH', 565, 0, 'CONFIRMED', $now, 'dev_01', 0)"
            )

            // Sync Outbox (Pending sync operation)
            execSQL(
                "INSERT INTO sync_outbox (id, organization_id, entity_type, entity_id, operation, payload_json, idempotency_key, status, attempt_count, next_attempt_at, created_at, updated_at) " +
                    "VALUES ('$outboxId', '$orgId', 'sales', '$saleId', 'UPSERT', '{\"receipt_number\":\"BR1-REG1-000001\"}', 'idem_outbox_01', 'PENDING', 0, $now, $now, $now)"
            )

            close()
        }

        // Step 2: Run MIGRATION_1_2 and validate schema against 2.json
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Step 3: Verify that existing data and new columns with safe defaults are present
        // 3a. Customer verification (existing columns + new v2 defaults)
        val customerCursor = migratedDb.query("SELECT id, organization_id, name, phone, email, customer_type, status, preferred_language, default_discount_bps, marketing_consent, has_credit_enabled FROM customers WHERE id = '$customerId'")
        assertTrue("Customer record must survive migration", customerCursor.moveToFirst())
        assertEquals(customerId, customerCursor.getString(0))
        assertEquals(orgId, customerCursor.getString(1))
        assertEquals("Acme Corp", customerCursor.getString(2))
        assertEquals("+1555123456", customerCursor.getString(3))
        assertEquals("contact@acme.com", customerCursor.getString(4))
        // Safe defaults
        assertEquals("INDIVIDUAL", customerCursor.getString(5))
        assertEquals("ACTIVE", customerCursor.getString(6))
        assertEquals("en", customerCursor.getString(7))
        assertEquals(0, customerCursor.getInt(8))
        assertEquals(0, customerCursor.getInt(9))
        assertEquals(0, customerCursor.getInt(10))
        customerCursor.close()

        // 3b. Barcode verification (pack_multiplier_milli default = 1000)
        val barcodeCursor = migratedDb.query("SELECT id, code, pack_multiplier_milli FROM barcodes WHERE id = '$barcodeId'")
        assertTrue("Barcode record must survive migration", barcodeCursor.moveToFirst())
        assertEquals(barcodeId, barcodeCursor.getString(0))
        assertEquals("8901234567890", barcodeCursor.getString(1))
        assertEquals(1000L, barcodeCursor.getLong(2))
        barcodeCursor.close()

        // 3c. Product verification (nullable v2 columns)
        val productCursor = migratedDb.query("SELECT id, name, sku, default_margin_bps, supplier_id, mrp_minor FROM products WHERE id = '$prodId'")
        assertTrue("Product record must survive migration", productCursor.moveToFirst())
        assertEquals("Mineral Water 1L", productCursor.getString(1))
        assertTrue(productCursor.isNull(3))
        assertTrue(productCursor.isNull(4))
        assertTrue(productCursor.isNull(5))
        productCursor.close()

        // 3d. Payment verification (credit_approval_status default = 'NONE')
        val paymentCursor = migratedDb.query("SELECT id, method, amount_minor, credit_approval_status, manager_override_uid FROM payments WHERE id = '$paymentId'")
        assertTrue("Payment record must survive migration", paymentCursor.moveToFirst())
        assertEquals("CASH", paymentCursor.getString(1))
        assertEquals(565L, paymentCursor.getLong(2))
        assertEquals("NONE", paymentCursor.getString(3))
        assertTrue(paymentCursor.isNull(4))
        paymentCursor.close()

        // 3e. Sync outbox verification
        val outboxCursor = migratedDb.query("SELECT id, entity_type, entity_id, idempotency_key, status, payload_json FROM sync_outbox WHERE id = '$outboxId'")
        assertTrue("Pending sync outbox item must survive migration", outboxCursor.moveToFirst())
        assertEquals(outboxId, outboxCursor.getString(0))
        assertEquals("sales", outboxCursor.getString(1))
        assertEquals(saleId, outboxCursor.getString(2))
        assertEquals("idem_outbox_01", outboxCursor.getString(3))
        assertEquals("PENDING", outboxCursor.getString(4))
        assertEquals("{\"receipt_number\":\"BR1-REG1-000001\"}", outboxCursor.getString(5))
        outboxCursor.close()

        // Step 4: Verify new v2 tables are functional
        // 4a. customer_credit_terms insertion
        val termsId = "terms_01"
        migratedDb.execSQL(
            "INSERT INTO customer_credit_terms (id, organization_id, customer_id, credit_limit_minor, payment_terms_days, grace_period_days, interest_rate_bps, hold_status, created_at, updated_at, version, device_id, sync_state) " +
                "VALUES ('$termsId', '$orgId', '$customerId', 100000, 30, 5, 0, 'ACTIVE', $now, $now, 1, 'dev_01', 0)"
        )
        val termsCursor = migratedDb.query("SELECT id, customer_id, credit_limit_minor, payment_terms_days FROM customer_credit_terms WHERE id = '$termsId'")
        assertTrue(termsCursor.moveToFirst())
        assertEquals(100000L, termsCursor.getLong(2))
        termsCursor.close()

        // 4b. customer_ledger insertion
        val ledgerId = "ledg_01"
        migratedDb.execSQL(
            "INSERT INTO customer_ledger (id, organization_id, customer_id, entry_type, amount_minor, balance_after_minor, reference_type, reference_id, idempotency_key, posted_at, device_id, sync_state) " +
                "VALUES ('$ledgerId', '$orgId', '$customerId', 'CHARGE', 5000, 5000, 'SALE', '$saleId', 'idem_ledg_01', $now, 'dev_01', 0)"
        )
        val ledgerCursor = migratedDb.query("SELECT id, amount_minor, balance_after_minor FROM customer_ledger WHERE id = '$ledgerId'")
        assertTrue(ledgerCursor.moveToFirst())
        assertEquals(5000L, ledgerCursor.getLong(1))
        ledgerCursor.close()

        // 4c. price_history insertion
        val priceHistId = "ph_01"
        migratedDb.execSQL(
            "INSERT INTO price_history (id, organization_id, product_id, old_price_minor, new_price_minor, change_reason, effective_at, device_id, sync_state) " +
                "VALUES ('$priceHistId', '$orgId', '$prodId', 200, 250, 'Annual supplier price adjustment', $now, 'dev_01', 0)"
        )
        val priceHistCursor = migratedDb.query("SELECT id, old_price_minor, new_price_minor FROM price_history WHERE id = '$priceHistId'")
        assertTrue(priceHistCursor.moveToFirst())
        assertEquals(200L, priceHistCursor.getLong(1))
        assertEquals(250L, priceHistCursor.getLong(2))
        priceHistCursor.close()

        // Step 5: Verify trigger immutability on customer_ledger (cannot update financial history)
        try {
            migratedDb.execSQL("UPDATE customer_ledger SET amount_minor = 99999 WHERE id = '$ledgerId'")
            fail("Expected exception when updating immutable customer_ledger table")
        } catch (e: Exception) {
            assertTrue("Expected immutability trigger abort", e.message?.contains("immutable") == true || e.message?.contains("abort") == true || e.message?.contains("ABORT") == true)
        }

        // Step 6: Verify trigger on CREDIT payment
        // 6a. CREDIT payment without customer_id on sale or without CONFIRMED/OVERRIDDEN status must fail
        val unconfirmedCreditPayId = "pay_fail_credit"
        try {
            migratedDb.execSQL(
                "INSERT INTO payments (id, organization_id, sale_id, method, amount_minor, change_minor, credit_approval_status, provider_status, created_at, device_id, sync_state) " +
                    "VALUES ('$unconfirmedCreditPayId', '$orgId', '$saleId', 'CREDIT', 1000, 0, 'NONE', 'CONFIRMED', $now, 'dev_01', 0)"
            )
            fail("Credit payment with status NONE must fail trigger")
        } catch (e: Exception) {
            assertTrue("Expected payment trigger abort", e.message?.contains("invalid payment authorization") == true || e.message?.contains("ABORT") == true)
        }

        // 6b. CREDIT payment with CONFIRMED approval on sale with customer_id succeeds
        val validCreditPayId = "pay_valid_credit"
        migratedDb.execSQL(
            "INSERT INTO payments (id, organization_id, sale_id, method, amount_minor, change_minor, credit_approval_status, provider_status, created_at, device_id, sync_state) " +
                "VALUES ('$validCreditPayId', '$orgId', '$saleId', 'CREDIT', 1000, 0, 'CONFIRMED', 'PENDING', $now, 'dev_01', 0)"
        )
        val validPayCursor = migratedDb.query("SELECT id, method, credit_approval_status FROM payments WHERE id = '$validCreditPayId'")
        assertTrue(validPayCursor.moveToFirst())
        assertEquals("CREDIT", validPayCursor.getString(1))
        assertEquals("CONFIRMED", validPayCursor.getString(2))
        validPayCursor.close()

        migratedDb.close()
    }
}
