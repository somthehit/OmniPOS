package com.example.pos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pos.data.model.StaffRole
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {

    // Organizations & Branches & Registers
    @Query("SELECT * FROM organizations LIMIT 1")
    fun getOrganization(): Flow<OrganizationEntity?>

    @Query("SELECT * FROM organizations LIMIT 1")
    suspend fun getActiveOrganizationSync(): OrganizationEntity?

    @Query("SELECT * FROM organizations WHERE org_id = :orgId LIMIT 1")
    suspend fun getOrgById(orgId: String): OrganizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrg(org: OrganizationEntity)

    @Query("SELECT * FROM branches WHERE org_id = :orgId")
    fun getBranches(orgId: String): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE org_id = :orgId")
    suspend fun getBranchesSync(orgId: String): List<BranchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchEntity)

    @Query("SELECT * FROM registers WHERE branch_id = :branchId")
    fun getRegisters(branchId: String): Flow<List<RegisterEntity>>

    @Query("SELECT * FROM registers WHERE org_id = :orgId")
    suspend fun getRegistersSync(orgId: String): List<RegisterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegister(register: RegisterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranches(branches: List<BranchEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegisters(registers: List<RegisterEntity>)

    @Query("UPDATE registers SET current_shift_id = :shiftId WHERE id = :registerId")
    suspend fun updateRegisterCurrentShift(registerId: String, shiftId: String?)

    // Users
    @Query("SELECT * FROM users ORDER BY full_name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE org_id = :orgId ORDER BY full_name ASC")
    fun getUsers(orgId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE org_id = :orgId ORDER BY full_name ASC")
    suspend fun getUsersSync(orgId: String): List<UserEntity>

    @Query("SELECT * FROM users ORDER BY full_name ASC")
    suspend fun getAllUsersSync(): List<UserEntity>

    @Query("SELECT * FROM users WHERE org_id = :orgId AND id = :userId LIMIT 1")
    suspend fun getUserById(orgId: String, userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE org_id = :orgId AND email = :email LIMIT 1")
    suspend fun getUserByEmail(orgId: String, email: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmailAcrossOrgs(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:identifier) OR LOWER(full_name) = LOWER(:identifier) OR phone = :identifier LIMIT 1")
    suspend fun findUserByIdentifier(identifier: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY created_at ASC LIMIT 1")
    suspend fun getFirstUser(): UserEntity?

    @Query("SELECT * FROM users WHERE org_id = :orgId AND role = :role LIMIT 1")
    suspend fun getUserByRole(orgId: String, role: StaffRole): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE org_id = :orgId AND id = :userId")
    suspend fun deleteUser(orgId: String, userId: String)

    // Staff
    @Query("SELECT * FROM staff_members WHERE org_id = :orgId AND is_active = 1")
    fun getStaff(orgId: String): Flow<List<StaffMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffMembers(staff: List<StaffMemberEntity>)

    // Categories
    @Query("SELECT * FROM categories WHERE org_id = :orgId ORDER BY name ASC")
    fun getCategories(orgId: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    // Products
    @Query("SELECT * FROM products WHERE org_id = :orgId ORDER BY name ASC")
    fun getProducts(orgId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE org_id = :orgId AND category_id = :categoryId ORDER BY name ASC")
    fun getProductsByCategory(orgId: String, categoryId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE org_id = :orgId AND (barcode = :query OR sku = :query OR name LIKE '%' || :query || '%')")
    fun searchProducts(orgId: String, query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    // Multi-barcode & Pack-size mappings (item_barcodes)
    @Query("SELECT * FROM item_barcodes WHERE org_id = :orgId AND barcode = :barcode LIMIT 1")
    suspend fun getBarcodeLink(orgId: String, barcode: String): ItemBarcodeEntity?

    @Query("SELECT * FROM item_barcodes WHERE org_id = :orgId AND item_id = :itemId")
    fun getItemBarcodes(orgId: String, itemId: String): Flow<List<ItemBarcodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemBarcode(itemBarcode: ItemBarcodeEntity)

    @Query("DELETE FROM item_barcodes WHERE org_id = :orgId AND id = :id")
    suspend fun deleteItemBarcode(orgId: String, id: String)

    @Query("SELECT * FROM products WHERE org_id = :orgId AND barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(orgId: String, barcode: String): ProductEntity?

    // Stock movements & Ledger calculation
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovements(movements: List<StockMovementEntity>)

    @Query("SELECT COALESCE(SUM(quantity_delta), 0) FROM stock_movements WHERE product_id = :productId AND branch_id = :branchId")
    fun observeStockQuantity(productId: String, branchId: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(quantity_delta), 0) FROM stock_movements WHERE product_id = :productId AND branch_id = :branchId")
    suspend fun getStockQuantity(productId: String, branchId: String): Int

    @Query("SELECT * FROM stock_movements WHERE org_id = :orgId ORDER BY created_at DESC LIMIT 100")
    fun getRecentStockMovements(orgId: String): Flow<List<StockMovementEntity>>

    // Sales & Items & Payments
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalePayments(payments: List<SalePaymentEntity>)

    @Query("SELECT * FROM sales WHERE org_id = :orgId ORDER BY created_at DESC")
    fun getSales(orgId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE register_id = :registerId ORDER BY created_at DESC")
    fun getSalesByRegister(registerId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId")
    suspend fun getSaleItems(saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_payments WHERE sale_id = :saleId")
    suspend fun getSalePayments(saleId: String): List<SalePaymentEntity>

    @Query("SELECT si.* FROM sale_items si INNER JOIN sales s ON si.sale_id = s.id WHERE s.org_id = :orgId")
    fun getAllSaleItems(orgId: String): Flow<List<SaleItemEntity>>

    @Query("SELECT sp.* FROM sale_payments sp INNER JOIN sales s ON sp.sale_id = s.id WHERE s.org_id = :orgId")
    fun getAllSalePayments(orgId: String): Flow<List<SalePaymentEntity>>

    @Query("SELECT COUNT(*) FROM sales WHERE register_id = :registerId AND created_at >= :sinceTimestamp")
    suspend fun getShiftSalesCount(registerId: String, sinceTimestamp: Long): Int

    @Query("SELECT COALESCE(SUM(grand_total_in_cents), 0) FROM sales WHERE register_id = :registerId AND created_at >= :sinceTimestamp")
    suspend fun getShiftSalesTotal(registerId: String, sinceTimestamp: Long): Long

    @Query("SELECT COALESCE(SUM(sp.amount_in_cents), 0) FROM sale_payments sp INNER JOIN sales s ON sp.sale_id = s.id WHERE s.register_id = :registerId AND s.created_at >= :sinceTimestamp AND sp.payment_type = 'CASH'")
    suspend fun getShiftCashSales(registerId: String, sinceTimestamp: Long): Long

    @Query("SELECT COALESCE(SUM(sp.amount_in_cents), 0) FROM sale_payments sp INNER JOIN sales s ON sp.sale_id = s.id WHERE s.register_id = :registerId AND s.created_at >= :sinceTimestamp AND sp.payment_type = 'CARD'")
    suspend fun getShiftCardSales(registerId: String, sinceTimestamp: Long): Long

    @Query("SELECT COALESCE(SUM(sp.amount_in_cents), 0) FROM sale_payments sp INNER JOIN sales s ON sp.sale_id = s.id WHERE s.register_id = :registerId AND s.created_at >= :sinceTimestamp AND sp.payment_type = 'QR_WALLET'")
    suspend fun getShiftQrSales(registerId: String, sinceTimestamp: Long): Long

    @Query("SELECT COALESCE(SUM(sp.amount_in_cents), 0) FROM sale_payments sp INNER JOIN sales s ON sp.sale_id = s.id WHERE s.register_id = :registerId AND s.created_at >= :sinceTimestamp AND sp.payment_type = 'CREDIT_DUE'")
    suspend fun getShiftCreditSales(registerId: String, sinceTimestamp: Long): Long

    // Held Orders (Parked carts)
    @Query("SELECT * FROM held_orders WHERE org_id = :orgId ORDER BY held_at DESC")
    fun getHeldOrders(orgId: String): Flow<List<HeldOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeldOrder(heldOrder: HeldOrderEntity)

    @Delete
    suspend fun deleteHeldOrder(heldOrder: HeldOrderEntity)

    @Query("DELETE FROM held_orders WHERE id = :id")
    suspend fun deleteHeldOrderById(id: String)

    // Customers & Customer Credit Ledger
    @Query("SELECT * FROM customers WHERE org_id = :orgId ORDER BY name ASC")
    fun getCustomers(orgId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET current_due_in_cents = current_due_in_cents + :dueDelta WHERE id = :customerId")
    suspend fun updateCustomerDue(customerId: String, dueDelta: Long)

    @Query("SELECT * FROM customer_ledger WHERE customer_id = :customerId ORDER BY created_at DESC")
    fun getCustomerLedger(customerId: String): Flow<List<CustomerLedgerEntity>>

    @Query("SELECT * FROM sales WHERE customer_id = :customerId OR id IN (SELECT sale_id FROM customer_ledger WHERE customer_id = :customerId AND sale_id IS NOT NULL) ORDER BY created_at DESC")
    fun getCustomerSales(customerId: String): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerLedgerEntry(entry: CustomerLedgerEntity)

    // Shifts & Cash movements
    @Query("SELECT * FROM shifts WHERE register_id = :registerId AND (status = 'OPEN' OR status = 'CLOSING') ORDER BY opened_at DESC LIMIT 1")
    fun getActiveShift(registerId: String): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE register_id = :registerId AND (status = 'OPEN' OR status = 'CLOSING') ORDER BY opened_at DESC LIMIT 1")
    suspend fun getActiveShiftSync(registerId: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE cashier_uid = :cashierUid AND (status = 'OPEN' OR status = 'CLOSING') ORDER BY opened_at DESC LIMIT 1")
    fun getActiveShiftForCashier(cashierUid: String): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE cashier_uid = :cashierUid AND (status = 'OPEN' OR status = 'CLOSING') ORDER BY opened_at DESC LIMIT 1")
    suspend fun getActiveShiftForCashierSync(cashierUid: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE cashier_uid = :cashierUid ORDER BY opened_at DESC LIMIT 1")
    suspend fun getLastShiftForCashier(cashierUid: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE register_id = :registerId AND status NOT IN ('OPEN', 'CLOSING') ORDER BY closed_at DESC LIMIT 1")
    suspend fun getLastClosedShiftForRegister(registerId: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE id = :shiftId LIMIT 1")
    suspend fun getShiftById(shiftId: String): ShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity)

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE org_id = :orgId ORDER BY opened_at DESC LIMIT 100")
    fun getAllShifts(orgId: String): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE org_id = :orgId AND (:status IS NULL OR status = :status) ORDER BY opened_at DESC")
    fun getShiftsByStatus(orgId: String, status: String?): Flow<List<ShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashMovement(cashMovement: CashMovementEntity)

    @Query("SELECT * FROM cash_movements WHERE shift_id = :shiftId ORDER BY created_at DESC")
    fun getCashMovements(shiftId: String): Flow<List<CashMovementEntity>>

    @Query("SELECT COALESCE(SUM(amount_in_cents), 0) FROM cash_movements WHERE shift_id = :shiftId AND movement_type = 'PAY_IN'")
    suspend fun getShiftPayInTotal(shiftId: String): Long

    @Query("SELECT COALESCE(SUM(amount_in_cents), 0) FROM cash_movements WHERE shift_id = :shiftId AND movement_type = 'PAY_OUT'")
    suspend fun getShiftPayOutTotal(shiftId: String): Long

    @Query("SELECT COALESCE(SUM(amount_in_cents), 0) FROM cash_movements WHERE shift_id = :shiftId AND movement_type = 'SAFE_DROP'")
    suspend fun getShiftSafeDropTotal(shiftId: String): Long

    @Query("SELECT COALESCE(SUM(amount_in_cents), 0) FROM cash_movements WHERE shift_id = :shiftId AND movement_type = 'CREDIT_COLLECTION'")
    suspend fun getShiftCreditCollectionTotal(shiftId: String): Long

    // Shift Denomination Cash Counts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftCashCounts(counts: List<ShiftCashCountEntity>)

    @Query("SELECT * FROM shift_cash_counts WHERE shift_id = :shiftId AND phase = :phase ORDER BY denomination DESC")
    fun getShiftCashCounts(shiftId: String, phase: String): Flow<List<ShiftCashCountEntity>>

    @Query("SELECT * FROM shift_cash_counts WHERE shift_id = :shiftId AND phase = :phase ORDER BY denomination DESC")
    suspend fun getShiftCashCountsSync(shiftId: String, phase: String): List<ShiftCashCountEntity>

    // Shift Declarations (Online payment provider reconciliation)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftDeclarations(declarations: List<ShiftDeclarationEntity>)

    @Query("SELECT * FROM shift_declarations WHERE shift_id = :shiftId")
    fun getShiftDeclarations(shiftId: String): Flow<List<ShiftDeclarationEntity>>

    @Query("SELECT * FROM shift_declarations WHERE shift_id = :shiftId")
    suspend fun getShiftDeclarationsSync(shiftId: String): List<ShiftDeclarationEntity>

    // Shift Credit Notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftCreditNotes(creditNotes: List<ShiftCreditNoteEntity>)

    @Query("SELECT * FROM shift_credit_notes WHERE shift_id = :shiftId")
    fun getShiftCreditNotes(shiftId: String): Flow<List<ShiftCreditNoteEntity>>

    @Query("SELECT * FROM shift_credit_notes WHERE shift_id = :shiftId")
    suspend fun getShiftCreditNotesSync(shiftId: String): List<ShiftCreditNoteEntity>

    // Shift Adjustments (Supervisor corrections)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftAdjustment(adjustment: ShiftAdjustmentEntity)

    @Query("SELECT * FROM shift_adjustments WHERE shift_id = :shiftId ORDER BY created_at DESC")
    fun getShiftAdjustments(shiftId: String): Flow<List<ShiftAdjustmentEntity>>

    @Query("SELECT * FROM shift_adjustments WHERE shift_id = :shiftId ORDER BY created_at DESC")
    suspend fun getShiftAdjustmentsSync(shiftId: String): List<ShiftAdjustmentEntity>

    // Shift Version History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftVersion(version: ShiftVersionEntity)

    @Query("SELECT * FROM shift_versions WHERE shift_id = :shiftId ORDER BY version DESC")
    fun getShiftVersions(shiftId: String): Flow<List<ShiftVersionEntity>>

    @Query("SELECT * FROM shift_versions WHERE shift_id = :shiftId ORDER BY version DESC")
    suspend fun getShiftVersionsSync(shiftId: String): List<ShiftVersionEntity>

    // Shift Audit Events
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftEvent(event: ShiftEventEntity)

    @Query("SELECT * FROM shift_events WHERE shift_id = :shiftId ORDER BY created_at ASC")
    fun getShiftEvents(shiftId: String): Flow<List<ShiftEventEntity>>

    @Query("SELECT * FROM shift_events WHERE shift_id = :shiftId ORDER BY created_at ASC")
    suspend fun getShiftEventsSync(shiftId: String): List<ShiftEventEntity>

    // Sync outbox
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncOutbox(entry: SyncOutboxEntity)

    @Query("SELECT * FROM sync_outbox WHERE org_id = :orgId ORDER BY created_at ASC LIMIT 50")
    suspend fun getPendingSyncEntries(orgId: String): List<SyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE org_id = :orgId")
    fun observePendingSyncCount(orgId: String): Flow<Int>

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteSyncOutboxById(id: String)

    // Store Settings
    @Query("SELECT * FROM store_settings WHERE org_id = :orgId LIMIT 1")
    fun getStoreSettings(orgId: String): Flow<StoreSettingsEntity?>

    @Query("SELECT * FROM store_settings WHERE org_id = :orgId LIMIT 1")
    suspend fun getStoreSettingsSync(orgId: String): StoreSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreSettings(settings: StoreSettingsEntity)

    // Audit Logs
    @Query("SELECT * FROM audit_logs WHERE org_id = :orgId ORDER BY created_at DESC LIMIT 200")
    fun getAuditLogs(orgId: String): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // Backup & Restore Bulk Operations
    @Query("SELECT * FROM categories WHERE org_id = :orgId")
    suspend fun getAllCategoriesSync(orgId: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("SELECT * FROM products WHERE org_id = :orgId")
    suspend fun getAllProductsSync(orgId: String): List<ProductEntity>

    @Query("SELECT * FROM item_barcodes WHERE org_id = :orgId")
    suspend fun getAllItemBarcodesSync(orgId: String): List<ItemBarcodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemBarcodes(barcodes: List<ItemBarcodeEntity>)

    @Query("SELECT * FROM customers WHERE org_id = :orgId")
    suspend fun getAllCustomersSync(orgId: String): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Query("SELECT * FROM customer_ledger WHERE org_id = :orgId")
    suspend fun getAllCustomerLedgerSync(orgId: String): List<CustomerLedgerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerLedgers(entries: List<CustomerLedgerEntity>)

    @Query("SELECT * FROM sales WHERE org_id = :orgId")
    suspend fun getAllSalesSync(orgId: String): List<SaleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<SaleEntity>)

    @Query("SELECT * FROM sale_items WHERE org_id = :orgId")
    suspend fun getAllSaleItemsSync(orgId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_payments WHERE org_id = :orgId")
    suspend fun getAllSalePaymentsSync(orgId: String): List<SalePaymentEntity>

    @Query("SELECT * FROM stock_movements WHERE org_id = :orgId")
    suspend fun getAllStockMovementsSync(orgId: String): List<StockMovementEntity>

    @Query("SELECT * FROM shifts WHERE org_id = :orgId")
    suspend fun getAllShiftsSync(orgId: String): List<ShiftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShifts(shifts: List<ShiftEntity>)

    @Query("SELECT * FROM cash_movements WHERE org_id = :orgId")
    suspend fun getAllCashMovementsSync(orgId: String): List<CashMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashMovements(cashMovements: List<CashMovementEntity>)

    @Query("SELECT * FROM staff_members WHERE org_id = :orgId")
    suspend fun getAllStaffMembersSync(orgId: String): List<StaffMemberEntity>

    @Query("SELECT * FROM audit_logs WHERE org_id = :orgId ORDER BY created_at DESC")
    suspend fun getAllAuditLogsSync(orgId: String): List<AuditLogEntity>

    @Query("SELECT * FROM held_orders WHERE org_id = :orgId")
    suspend fun getAllHeldOrdersSync(orgId: String): List<HeldOrderEntity>

    // Purge mock data queries
    @Query("DELETE FROM organizations WHERE id = 'org_omnipos_default' OR name LIKE '%Apex Universal%'")
    suspend fun purgeMockOrganizations()

    @Query("DELETE FROM users WHERE org_id = 'org_omnipos_default' OR email LIKE '%@omnipos.store' OR email LIKE '%@omnipos.local' OR full_name LIKE '%Elena Rostova%' OR full_name LIKE '%Alex Vance%' OR full_name LIKE '%Marcus Brody%' OR full_name LIKE '%Dave Miller%' OR full_name LIKE '%Chloe Price%'")
    suspend fun purgeMockUsers()

    @Query("DELETE FROM staff_members WHERE org_id = 'org_omnipos_default' OR email LIKE '%@omnipos.%'")
    suspend fun purgeMockStaff()

    @Query("DELETE FROM products WHERE org_id = 'org_omnipos_default' OR sku LIKE 'BEV-%' OR sku LIKE 'GRC-%' OR sku LIKE 'ELC-%' OR sku LIKE 'APP-%' OR sku LIKE 'PER-%'")
    suspend fun purgeMockProducts()

    @Query("DELETE FROM categories WHERE org_id = 'org_omnipos_default'")
    suspend fun purgeMockCategories()

    @Query("DELETE FROM customers WHERE org_id = 'org_omnipos_default' OR email LIKE '%@example.com'")
    suspend fun purgeMockCustomers()

    @Query("DELETE FROM stock_movements WHERE org_id = 'org_omnipos_default'")
    suspend fun purgeMockStock()

    @Query("DELETE FROM branches WHERE org_id = 'org_omnipos_default'")
    suspend fun purgeMockBranches()

    @Query("DELETE FROM registers WHERE org_id = 'org_omnipos_default'")
    suspend fun purgeMockRegisters()

    // Fiscal Years
    @Query("SELECT * FROM fiscal_years WHERE org_id = :orgId ORDER BY start_date DESC")
    fun getFiscalYears(orgId: String): Flow<List<FiscalYearEntity>>

    @Query("SELECT * FROM fiscal_years WHERE org_id = :orgId ORDER BY start_date DESC")
    suspend fun getFiscalYearsSync(orgId: String): List<FiscalYearEntity>

    @Query("SELECT * FROM fiscal_years WHERE org_id = :orgId AND is_current = 1 LIMIT 1")
    fun getActiveFiscalYear(orgId: String): Flow<FiscalYearEntity?>

    @Query("SELECT * FROM fiscal_years WHERE org_id = :orgId AND is_current = 1 LIMIT 1")
    suspend fun getActiveFiscalYearSync(orgId: String): FiscalYearEntity?

    @Query("SELECT * FROM fiscal_years WHERE id = :id LIMIT 1")
    suspend fun getFiscalYearById(id: String): FiscalYearEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiscalYear(fy: FiscalYearEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiscalYears(fys: List<FiscalYearEntity>)

    @Update
    suspend fun updateFiscalYear(fy: FiscalYearEntity)

    @Query("UPDATE fiscal_years SET is_current = 0 WHERE org_id = :orgId")
    suspend fun clearActiveFiscalYear(orgId: String)

    @Query("UPDATE fiscal_years SET is_current = 1 WHERE org_id = :orgId AND id = :fyId")
    suspend fun setActiveFiscalYear(orgId: String, fyId: String)

    @Query("UPDATE fiscal_years SET current_invoice_sequence = current_invoice_sequence + 1 WHERE id = :fyId")
    suspend fun incrementInvoiceSequence(fyId: String)

    @Query("SELECT * FROM sales WHERE org_id = :orgId AND (fiscal_year = :fyName OR (fiscal_year = '' AND created_at BETWEEN :startTime AND :endTime)) ORDER BY created_at DESC")
    fun getSalesByFiscalYear(orgId: String, fyName: String, startTime: Long, endTime: Long): Flow<List<SaleEntity>>
}
