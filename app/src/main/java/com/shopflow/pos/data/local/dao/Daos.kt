package com.shopflow.pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.shopflow.pos.data.local.entity.*
import kotlinx.coroutines.flow.Flow

/*
 * RULE: every query takes organizationId and filters on it. No DAO method may omit it.
 * (Enforce with a unit test that parses @Query strings and fails if 'organization_id' is missing.)
 */

data class StockOnHand(
    @androidx.room.ColumnInfo(name = "confirmedMilli") val confirmedMilli: Long,   // server-acknowledged movements
    @androidx.room.ColumnInfo(name = "pendingMilli") val pendingMilli: Long,       // local, unsynced -> "provisional"
) { val provisionalMilli: Long get() = confirmedMilli + pendingMilli }

data class OutboxCounts(
    @androidx.room.ColumnInfo(name = "pending") val pending: Int,
    @androidx.room.ColumnInfo(name = "failed") val failed: Int,
)

// ───────────────────────── 1. OrganizationDao ─────────────────────────

@Dao
interface OrganizationDao {
    @Upsert suspend fun upsert(org: OrganizationEntity)
    @Query("SELECT * FROM organizations WHERE id = :orgId") suspend fun get(orgId: String): OrganizationEntity?
    @Query("SELECT * FROM organizations WHERE id = :orgId") fun observe(orgId: String): Flow<OrganizationEntity?>
    @Query("UPDATE organizations SET display_name = :displayName, tax_id = :taxId, updated_at = :updatedAt, version = version + 1 WHERE id = :orgId")
    suspend fun updateDetails(orgId: String, displayName: String, taxId: String?, updatedAt: Long)
    @Query("UPDATE organizations SET status = 'INACTIVE', updated_at = :nowMs WHERE id = :orgId")
    suspend fun deactivate(orgId: String, nowMs: Long)
}

// ───────────────────────── 2. BranchDao ─────────────────────────

@Dao
interface BranchDao {
    @Upsert suspend fun upsert(branch: BranchEntity)
    @Upsert suspend fun upsertAll(items: List<BranchEntity>)
    @Query("SELECT * FROM branches WHERE organization_id = :orgId AND id = :branchId AND deleted_at IS NULL")
    suspend fun get(orgId: String, branchId: String): BranchEntity?
    @Query("SELECT * FROM branches WHERE organization_id = :orgId AND deleted_at IS NULL ORDER BY name")
    fun observeAll(orgId: String): Flow<List<BranchEntity>>
    @Query("UPDATE branches SET deleted_at = :nowMs, updated_at = :nowMs, version = version + 1 WHERE organization_id = :orgId AND id = :branchId")
    suspend fun softDelete(orgId: String, branchId: String, nowMs: Long)
}

// ───────────────────────── 3. RegisterDao ─────────────────────────

@Dao
interface RegisterDao {
    @Upsert suspend fun upsert(register: RegisterEntity)
    @Upsert suspend fun upsertAll(items: List<RegisterEntity>)
    @Query("SELECT * FROM registers WHERE organization_id = :orgId AND id = :registerId AND deleted_at IS NULL")
    suspend fun get(orgId: String, registerId: String): RegisterEntity?
    @Query("SELECT * FROM registers WHERE organization_id = :orgId AND branch_id = :branchId AND deleted_at IS NULL ORDER BY name")
    fun observeByBranch(orgId: String, branchId: String): Flow<List<RegisterEntity>>
    @Query("UPDATE registers SET deleted_at = :nowMs, updated_at = :nowMs, version = version + 1 WHERE organization_id = :orgId AND id = :registerId")
    suspend fun softDelete(orgId: String, registerId: String, nowMs: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun ensureCounter(counter: ReceiptCounterEntity)
    @Query("UPDATE receipt_counters SET next_seq = next_seq + 1 WHERE organization_id = :orgId AND register_id = :registerId")
    suspend fun bumpCounter(orgId: String, registerId: String)
    @Query("SELECT next_seq FROM receipt_counters WHERE organization_id = :orgId AND register_id = :registerId")
    suspend fun counterValue(orgId: String, registerId: String): Long
}

// ───────────────────────── 4. ReceiptCounterDao ─────────────────────────

@Dao
interface ReceiptCounterDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun ensureCounter(counter: ReceiptCounterEntity)
    @Query("UPDATE receipt_counters SET next_seq = next_seq + 1 WHERE organization_id = :orgId AND register_id = :registerId")
    suspend fun bump(orgId: String, registerId: String)
    @Query("SELECT next_seq FROM receipt_counters WHERE organization_id = :orgId AND register_id = :registerId")
    suspend fun currentValue(orgId: String, registerId: String): Long?
    @Query("SELECT * FROM receipt_counters WHERE organization_id = :orgId AND register_id = :registerId")
    suspend fun get(orgId: String, registerId: String): ReceiptCounterEntity?
}

// ───────────────────────── 5. UserDao ─────────────────────────

@Dao
interface UserDao {
    @Upsert suspend fun upsertUsers(items: List<UserEntity>)
    @Upsert suspend fun upsertUser(user: UserEntity)
    @Upsert suspend fun upsertMemberships(items: List<MembershipEntity>)
    @Upsert suspend fun upsertRolePermissions(items: List<RolePermissionEntity>)
    @Query("DELETE FROM role_permissions WHERE organization_id = :orgId") suspend fun clearRolePermissions(orgId: String)

    @Query("SELECT * FROM users WHERE organization_id = :orgId AND id = :userId AND deleted_at IS NULL")
    suspend fun getUser(orgId: String, userId: String): UserEntity?
    @Query("SELECT * FROM users WHERE organization_id = :orgId AND deleted_at IS NULL ORDER BY full_name")
    fun observeAll(orgId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM memberships WHERE organization_id = :orgId AND user_id = :userId")
    suspend fun getMembership(orgId: String, userId: String): MembershipEntity?

    @Query("""SELECT rp.permission_key FROM role_permissions rp
              JOIN memberships m ON m.organization_id = rp.organization_id AND m.role_key = rp.role_key
              WHERE m.organization_id = :orgId AND m.user_id = :userId AND m.status = 'ACTIVE'""")
    suspend fun permissionsFor(orgId: String, userId: String): List<String>

    @Query("SELECT branch_id FROM membership_branches WHERE organization_id = :orgId AND membership_id = :membershipId")
    suspend fun branchIdsFor(orgId: String, membershipId: String): List<String>

    @Upsert suspend fun upsertCredential(cred: LocalCredentialEntity)
    @Query("SELECT * FROM local_credentials WHERE organization_id = :orgId AND user_id = :userId")
    suspend fun getCredential(orgId: String, userId: String): LocalCredentialEntity?

    @Query("UPDATE users SET deleted_at = :nowMs, updated_at = :nowMs, version = version + 1 WHERE organization_id = :orgId AND id = :userId")
    suspend fun softDelete(orgId: String, userId: String, nowMs: Long)
}

// ───────────────────────── 6. MembershipDao ─────────────────────────

@Dao
interface MembershipDao {
    @Upsert suspend fun upsert(membership: MembershipEntity)
    @Upsert suspend fun upsertAll(items: List<MembershipEntity>)
    @Query("SELECT * FROM memberships WHERE organization_id = :orgId AND id = :membershipId")
    suspend fun get(orgId: String, membershipId: String): MembershipEntity?
    @Query("SELECT * FROM memberships WHERE organization_id = :orgId AND user_id = :userId")
    suspend fun getByUserId(orgId: String, userId: String): MembershipEntity?
    @Query("SELECT * FROM memberships WHERE organization_id = :orgId AND status = 'ACTIVE'")
    fun observeActiveMemberships(orgId: String): Flow<List<MembershipEntity>>
    @Query("UPDATE memberships SET status = :status, updated_at = :nowMs, version = version + 1 WHERE organization_id = :orgId AND id = :membershipId")
    suspend fun updateStatus(orgId: String, membershipId: String, status: String, nowMs: Long)
}

// ───────────────────────── 7. MembershipBranchDao ─────────────────────────

@Dao
interface MembershipBranchDao {
    @Upsert suspend fun upsertAll(items: List<MembershipBranchEntity>)
    @Query("SELECT branch_id FROM membership_branches WHERE organization_id = :orgId AND membership_id = :membershipId")
    suspend fun branchIdsFor(orgId: String, membershipId: String): List<String>
    @Query("DELETE FROM membership_branches WHERE organization_id = :orgId AND membership_id = :membershipId")
    suspend fun clearForMembership(orgId: String, membershipId: String)
}

// ───────────────────────── 8. RolePermissionDao ─────────────────────────

@Dao
interface RolePermissionDao {
    @Upsert suspend fun upsertAll(items: List<RolePermissionEntity>)
    @Query("SELECT permission_key FROM role_permissions WHERE organization_id = :orgId AND role_key = :roleKey")
    suspend fun permissionsForRole(orgId: String, roleKey: String): List<String>
    @Query("DELETE FROM role_permissions WHERE organization_id = :orgId AND role_key = :roleKey")
    suspend fun clearForRole(orgId: String, roleKey: String)
    @Query("DELETE FROM role_permissions WHERE organization_id = :orgId")
    suspend fun clearAll(orgId: String)
}

// ───────────────────────── 9. LocalCredentialDao ─────────────────────────

@Dao
interface LocalCredentialDao {
    @Upsert suspend fun upsert(cred: LocalCredentialEntity)
    @Query("SELECT * FROM local_credentials WHERE organization_id = :orgId AND user_id = :userId")
    suspend fun get(orgId: String, userId: String): LocalCredentialEntity?
    @Query("UPDATE local_credentials SET failed_attempts = failed_attempts + 1, locked_until = :lockedUntil WHERE organization_id = :orgId AND user_id = :userId")
    suspend fun recordFailedAttempt(orgId: String, userId: String, lockedUntil: Long?)
    @Query("UPDATE local_credentials SET failed_attempts = 0, locked_until = NULL, last_online_auth_at = :nowMs WHERE organization_id = :orgId AND user_id = :userId")
    suspend fun resetLock(orgId: String, userId: String, nowMs: Long)
    @Query("DELETE FROM local_credentials WHERE organization_id = :orgId AND user_id = :userId")
    suspend fun clear(orgId: String, userId: String)
}

// ───────────────────────── 10. CategoryDao ─────────────────────────

@Dao
interface CategoryDao {
    @Upsert suspend fun upsert(category: CategoryEntity)
    @Upsert suspend fun upsertAll(items: List<CategoryEntity>)
    @Query("SELECT * FROM categories WHERE organization_id = :orgId AND id = :id AND deleted_at IS NULL")
    suspend fun get(orgId: String, id: String): CategoryEntity?
    @Query("SELECT * FROM categories WHERE organization_id = :orgId AND deleted_at IS NULL ORDER BY sort_order, name")
    fun observeAll(orgId: String): Flow<List<CategoryEntity>>
    @Query("UPDATE categories SET deleted_at = :nowMs, updated_at = :nowMs, version = version + 1 WHERE organization_id = :orgId AND id = :id")
    suspend fun softDelete(orgId: String, id: String, nowMs: Long)
}

// ───────────────────────── 11. TaxRateDao ─────────────────────────

@Dao
interface TaxRateDao {
    @Upsert suspend fun upsert(taxRate: TaxRateEntity)
    @Upsert suspend fun upsertAll(items: List<TaxRateEntity>)
    @Query("SELECT * FROM tax_rates WHERE organization_id = :orgId AND id = :id AND deleted_at IS NULL")
    suspend fun get(orgId: String, id: String): TaxRateEntity?
    @Query("SELECT * FROM tax_rates WHERE organization_id = :orgId AND is_default = 1 AND deleted_at IS NULL LIMIT 1")
    suspend fun getDefault(orgId: String): TaxRateEntity?
    @Query("SELECT * FROM tax_rates WHERE organization_id = :orgId AND deleted_at IS NULL ORDER BY name")
    fun observeAll(orgId: String): Flow<List<TaxRateEntity>>
    @Query("UPDATE tax_rates SET deleted_at = :nowMs, updated_at = :nowMs, version = version + 1 WHERE organization_id = :orgId AND id = :id")
    suspend fun softDelete(orgId: String, id: String, nowMs: Long)
}

// ───────────────────────── 12. ProductDao ─────────────────────────

@Dao
interface ProductDao {
    @Upsert suspend fun upsert(product: ProductEntity)
    @Upsert suspend fun upsertAll(items: List<ProductEntity>)   // pull-sync must skip rows whose local sync_state != SYNCED
    @Upsert suspend fun upsertCategories(items: List<CategoryEntity>)
    @Upsert suspend fun upsertTaxRates(items: List<TaxRateEntity>)
    @Upsert suspend fun upsertBarcodes(items: List<BarcodeEntity>)

    @Query("SELECT * FROM products WHERE organization_id = :orgId AND id = :id AND deleted_at IS NULL")
    suspend fun get(orgId: String, id: String): ProductEntity?

    @Query("""SELECT p.* FROM products p
              JOIN barcodes b ON b.organization_id = p.organization_id AND b.product_id = p.id
              WHERE p.organization_id = :orgId AND b.code = :code
                AND p.status = 'ACTIVE' AND p.deleted_at IS NULL AND b.deleted_at IS NULL
              LIMIT 1""")
    suspend fun findByBarcode(orgId: String, code: String): ProductEntity?

    @Query("""SELECT * FROM products
              WHERE organization_id = :orgId AND status = 'ACTIVE' AND deleted_at IS NULL
                AND (:categoryId IS NULL OR category_id = :categoryId)
                AND (:query = '' OR name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%')
              ORDER BY name LIMIT :limit""")
    fun search(orgId: String, query: String, categoryId: String?, limit: Int = 200): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE organization_id = :orgId AND deleted_at IS NULL ORDER BY name")
    fun observeAll(orgId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM categories WHERE organization_id = :orgId AND deleted_at IS NULL ORDER BY sort_order, name")
    fun observeCategories(orgId: String): Flow<List<CategoryEntity>>

    @Query("UPDATE products SET deleted_at = :nowMs, updated_at = :nowMs, version = version + 1 WHERE organization_id = :orgId AND id = :id")
    suspend fun softDelete(orgId: String, id: String, nowMs: Long)

    @Query("UPDATE products SET status = 'ARCHIVED', updated_at = :nowMs, version = version + 1 WHERE organization_id = :orgId AND id = :id")
    suspend fun archive(orgId: String, id: String, nowMs: Long)
}

// ───────────────────────── 13. BarcodeDao ─────────────────────────

@Dao
interface BarcodeDao {
    @Upsert suspend fun upsert(barcode: BarcodeEntity)
    @Upsert suspend fun upsertAll(items: List<BarcodeEntity>)
    @Query("SELECT * FROM barcodes WHERE organization_id = :orgId AND code = :code AND deleted_at IS NULL LIMIT 1")
    suspend fun findByCode(orgId: String, code: String): BarcodeEntity?
    @Query("SELECT * FROM barcodes WHERE organization_id = :orgId AND product_id = :productId AND deleted_at IS NULL")
    suspend fun getForProduct(orgId: String, productId: String): List<BarcodeEntity>
    @Query("UPDATE barcodes SET deleted_at = :nowMs, updated_at = :nowMs, version = version + 1 WHERE organization_id = :orgId AND id = :id")
    suspend fun softDelete(orgId: String, id: String, nowMs: Long)
}

// ───────────────────────── 14. CustomerDao ─────────────────────────

@Dao
interface CustomerDao {
    @Upsert suspend fun upsert(customer: CustomerEntity)
    @Upsert suspend fun upsertAll(items: List<CustomerEntity>)
    @Query("SELECT * FROM customers WHERE organization_id = :orgId AND id = :customerId AND deleted_at IS NULL")
    suspend fun get(orgId: String, customerId: String): CustomerEntity?
    @Query("SELECT * FROM customers WHERE organization_id = :orgId AND deleted_at IS NULL ORDER BY name")
    fun observeAll(orgId: String): Flow<List<CustomerEntity>>
    @Query("""SELECT * FROM customers
              WHERE organization_id = :orgId AND deleted_at IS NULL
                AND (:query = '' OR name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%')
              ORDER BY name LIMIT :limit""")
    fun search(orgId: String, query: String, limit: Int = 100): Flow<List<CustomerEntity>>
    @Query("UPDATE customers SET deleted_at = :nowMs, updated_at = :nowMs, version = version + 1 WHERE organization_id = :orgId AND id = :customerId")
    suspend fun softDelete(orgId: String, customerId: String, nowMs: Long)
}

// ───────────────────────── 15. StockDao & StockMovementDao ─────────────────────────

@Dao
interface StockMovementDao {
    /** IGNORE on the unique idempotency index => re-posting the same movement is a no-op (returns -1). */
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertMovements(items: List<StockMovementEntity>): List<Long>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(movement: StockMovementEntity): Long

    @Query("""SELECT
                COALESCE(SUM(CASE WHEN sync_state = 1 THEN quantity_milli ELSE 0 END), 0) AS confirmedMilli,
                COALESCE(SUM(CASE WHEN sync_state <> 1 THEN quantity_milli ELSE 0 END), 0) AS pendingMilli
              FROM stock_movements
              WHERE organization_id = :orgId AND branch_id = :branchId AND product_id = :productId""")
    suspend fun onHand(orgId: String, branchId: String, productId: String): StockOnHand

    @Query("""SELECT * FROM stock_movements
              WHERE organization_id = :orgId AND branch_id = :branchId AND product_id = :productId
              ORDER BY created_at DESC LIMIT :limit""")
    fun history(orgId: String, branchId: String, productId: String, limit: Int = 100): Flow<List<StockMovementEntity>>

    @Query("""SELECT * FROM stock_movements
              WHERE organization_id = :orgId AND source_type = :sourceType AND source_id = :sourceId
              ORDER BY created_at ASC""")
    suspend fun getBySource(orgId: String, sourceType: String, sourceId: String): List<StockMovementEntity>
}

// Backward-compatible alias interface for existing tests and repository
typealias StockDao = StockMovementDao

// ───────────────────────── 16. SaleDao ─────────────────────────

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSale(sale: SaleEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertItems(items: List<SaleItemEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPayments(items: List<PaymentEntity>)
    // Pull-sync of already-synced history from the server:
    @Upsert suspend fun upsertSalesFromServer(items: List<SaleEntity>)

    @Query("SELECT * FROM sales WHERE organization_id = :orgId AND idempotency_key = :key LIMIT 1")
    suspend fun findByIdempotencyKey(orgId: String, key: String): SaleEntity?

    @Query("SELECT * FROM sales WHERE organization_id = :orgId AND id = :saleId")
    suspend fun get(orgId: String, saleId: String): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE organization_id = :orgId AND sale_id = :saleId ORDER BY created_at")
    suspend fun itemsFor(orgId: String, saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM payments WHERE organization_id = :orgId AND sale_id = :saleId ORDER BY created_at")
    suspend fun paymentsFor(orgId: String, saleId: String): List<PaymentEntity>

    /** cashierId = current user for cashiers (own sales only); null for roles with branch-wide history permission. */
    @Query("""SELECT * FROM sales
              WHERE organization_id = :orgId
                AND (:branchId IS NULL OR branch_id = :branchId)
                AND (:cashierId IS NULL OR cashier_id = :cashierId)
                AND sold_at BETWEEN :fromMs AND :toMs
                AND (:receiptQuery IS NULL OR receipt_number LIKE '%' || :receiptQuery || '%')
              ORDER BY sold_at DESC LIMIT :limit""")
    fun history(orgId: String, branchId: String?, cashierId: String?, fromMs: Long, toMs: Long,
                receiptQuery: String?, limit: Int = 200): Flow<List<SaleEntity>>

    @Query("UPDATE sales SET sync_state = :state, sync_error = :error, server_updated_at = :serverAt WHERE organization_id = :orgId AND id = :saleId")
    suspend fun markSyncState(orgId: String, saleId: String, state: Int, error: String?, serverAt: Long?)

    @Query("UPDATE sale_items SET sync_state = :state, server_updated_at = :serverAt WHERE organization_id = :orgId AND sale_id = :saleId")
    suspend fun markItemsSyncState(orgId: String, saleId: String, state: Int, serverAt: Long?)

    @Query("UPDATE payments SET sync_state = :state, server_updated_at = :serverAt WHERE organization_id = :orgId AND sale_id = :saleId")
    suspend fun markPaymentsSyncState(orgId: String, saleId: String, state: Int, serverAt: Long?)

    @Query("UPDATE stock_movements SET sync_state = :state, server_updated_at = :serverAt WHERE organization_id = :orgId AND source_type = 'sale' AND source_id = :saleId")
    suspend fun markMovementsSyncState(orgId: String, saleId: String, state: Int, serverAt: Long?)

    @Query("UPDATE sales SET status = :status, updated_at = :nowMs, version = version + 1 WHERE organization_id = :orgId AND id = :saleId")
    suspend fun updateStatus(orgId: String, saleId: String, status: String, nowMs: Long)
}

// ───────────────────────── 17. SaleItemDao ─────────────────────────

@Dao
interface SaleItemDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertItems(items: List<SaleItemEntity>)
    @Query("SELECT * FROM sale_items WHERE organization_id = :orgId AND sale_id = :saleId ORDER BY created_at")
    suspend fun itemsForSale(orgId: String, saleId: String): List<SaleItemEntity>
    @Query("UPDATE sale_items SET sync_state = :state, server_updated_at = :serverAt WHERE organization_id = :orgId AND sale_id = :saleId")
    suspend fun markItemsSyncState(orgId: String, saleId: String, state: Int, serverAt: Long?)
}

// ───────────────────────── 18. PaymentDao ─────────────────────────

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPayments(items: List<PaymentEntity>)
    @Query("SELECT * FROM payments WHERE organization_id = :orgId AND sale_id = :saleId ORDER BY created_at")
    suspend fun paymentsForSale(orgId: String, saleId: String): List<PaymentEntity>
    @Query("UPDATE payments SET sync_state = :state, server_updated_at = :serverAt WHERE organization_id = :orgId AND sale_id = :saleId")
    suspend fun markPaymentsSyncState(orgId: String, saleId: String, state: Int, serverAt: Long?)
}

// ───────────────────────── 19. SyncOutboxDao ─────────────────────────

@Dao
interface SyncOutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun enqueue(item: SyncOutboxEntity): Long

    /** Oldest first so dependent operations keep their order. */
    @Query("""SELECT * FROM sync_outbox
              WHERE organization_id = :orgId AND status IN ('PENDING','FAILED') AND next_attempt_at <= :nowMs
              ORDER BY created_at LIMIT :limit""")
    suspend fun nextBatch(orgId: String, nowMs: Long, limit: Int = 25): List<SyncOutboxEntity>

    @Query("UPDATE sync_outbox SET status = 'IN_FLIGHT', updated_at = :nowMs WHERE organization_id = :orgId AND id = :id")
    suspend fun markInFlight(orgId: String, id: String, nowMs: Long)

    @Query("UPDATE sync_outbox SET status = 'DONE', last_error = NULL, updated_at = :nowMs WHERE organization_id = :orgId AND id = :id")
    suspend fun markDone(orgId: String, id: String, nowMs: Long)

    @Query("""UPDATE sync_outbox SET status = 'FAILED', attempt_count = attempt_count + 1,
              next_attempt_at = :nextAttemptAt, last_error = :error, updated_at = :nowMs
              WHERE organization_id = :orgId AND id = :id""")
    suspend fun markFailed(orgId: String, id: String, nextAttemptAt: Long, error: String?, nowMs: Long)

    /** On app restart, anything left IN_FLIGHT was interrupted: retry it (server is idempotent). */
    @Query("UPDATE sync_outbox SET status = 'PENDING' WHERE organization_id = :orgId AND status = 'IN_FLIGHT'")
    suspend fun recoverInterrupted(orgId: String)

    @Query("""SELECT COALESCE(SUM(CASE WHEN status IN ('PENDING','IN_FLIGHT') THEN 1 ELSE 0 END),0) AS pending,
                     COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END),0) AS failed
              FROM sync_outbox WHERE organization_id = :orgId AND status <> 'DONE'""")
    fun observeCounts(orgId: String): Flow<OutboxCounts>

    @Query("SELECT * FROM sync_outbox WHERE organization_id = :orgId AND status = 'FAILED' ORDER BY updated_at DESC")
    fun observeFailed(orgId: String): Flow<List<SyncOutboxEntity>>
}

// Backward-compatible alias interface for OutboxDao
typealias OutboxDao = SyncOutboxDao

// ───────────────────────── 20. SyncStateDao ─────────────────────────

@Dao
interface SyncStateDao {
    @Upsert suspend fun upsertSyncState(state: SyncStateEntity)
    @Query("SELECT * FROM sync_state WHERE organization_id = :orgId AND entity_type = :entityType")
    suspend fun getSyncState(orgId: String, entityType: String): SyncStateEntity?
    @Query("SELECT * FROM sync_state WHERE organization_id = :orgId")
    fun observeAll(orgId: String): Flow<List<SyncStateEntity>>
}

// ───────────────────────── 21. CustomerCreditTermsDao ─────────────────────────

@Dao
interface CustomerCreditTermsDao {
    @Upsert suspend fun upsert(terms: CustomerCreditTermsEntity)
    @Query("SELECT * FROM customer_credit_terms WHERE organization_id = :orgId AND customer_id = :customerId")
    suspend fun getByCustomer(orgId: String, customerId: String): CustomerCreditTermsEntity?
    @Query("SELECT * FROM customer_credit_terms WHERE organization_id = :orgId AND customer_id = :customerId")
    fun observeByCustomer(orgId: String, customerId: String): Flow<CustomerCreditTermsEntity?>
    @Query("UPDATE customer_credit_terms SET hold_status = :status, updated_at = :updatedAt, version = version + 1 WHERE organization_id = :orgId AND customer_id = :customerId")
    suspend fun updateHoldStatus(orgId: String, customerId: String, status: String, updatedAt: Long)
}

// ───────────────────────── 22. CustomerLedgerDao ─────────────────────────

@Dao
interface CustomerLedgerDao {
    @Insert suspend fun insert(entry: CustomerLedgerEntity)
    @Query("SELECT * FROM customer_ledger WHERE organization_id = :orgId AND customer_id = :customerId ORDER BY posted_at DESC")
    fun observeByCustomer(orgId: String, customerId: String): Flow<List<CustomerLedgerEntity>>
    @Query("SELECT * FROM customer_ledger WHERE organization_id = :orgId AND customer_id = :customerId ORDER BY posted_at DESC LIMIT 1")
    suspend fun getLatestEntry(orgId: String, customerId: String): CustomerLedgerEntity?
    @Query("SELECT COALESCE(SUM(amount_minor), 0) FROM customer_ledger WHERE organization_id = :orgId AND customer_id = :customerId")
    suspend fun getCurrentBalance(orgId: String, customerId: String): Long
}

// ───────────────────────── 23. PriceHistoryDao ─────────────────────────

@Dao
interface PriceHistoryDao {
    @Insert suspend fun insert(history: PriceHistoryEntity)
    @Query("SELECT * FROM price_history WHERE organization_id = :orgId AND product_id = :productId ORDER BY effective_at DESC")
    fun observeByProduct(orgId: String, productId: String): Flow<List<PriceHistoryEntity>>
    @Query("SELECT * FROM price_history WHERE organization_id = :orgId AND product_id = :productId ORDER BY effective_at DESC LIMIT 1")
    suspend fun getLatestChange(orgId: String, productId: String): PriceHistoryEntity?
}
