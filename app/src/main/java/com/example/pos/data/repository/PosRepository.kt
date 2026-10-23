package com.example.pos.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.pos.data.local.AppDatabase
import com.example.pos.data.local.AuditLogEntity
import com.example.pos.data.local.BranchEntity
import com.example.pos.data.local.CashMovementEntity
import com.example.pos.data.local.CategoryEntity
import com.example.pos.data.local.CustomerEntity
import com.example.pos.data.local.CustomerLedgerEntity
import com.example.pos.data.local.HeldOrderEntity
import com.example.pos.data.local.ItemBarcodeEntity
import com.example.pos.data.local.OrganizationEntity
import com.example.pos.data.local.PosDao
import com.example.pos.data.local.ProductEntity
import com.example.pos.data.local.RegisterEntity
import com.example.pos.data.local.SaleEntity
import com.example.pos.data.local.SaleItemEntity
import com.example.pos.data.local.SalePaymentEntity
import com.example.pos.data.local.ShiftEntity
import com.example.pos.data.local.StaffMemberEntity
import com.example.pos.data.local.StockMovementEntity
import com.example.pos.data.local.StoreSettingsEntity
import com.example.pos.data.local.SyncOutboxEntity
import com.example.pos.data.local.UserEntity
import com.example.pos.data.local.FiscalYearEntity
import com.example.pos.domain.FiscalYearHelper
import com.example.pos.data.model.BarcodeLookupResult
import com.example.pos.data.model.CartItem
import com.example.pos.data.model.MovementType
import com.example.pos.data.model.OutboxOp
import com.example.pos.data.model.PaymentEntry
import com.example.pos.data.model.PaymentType
import com.example.pos.data.model.StaffRole
import com.example.pos.data.model.TaxMode
import com.example.pos.data.model.ZReport
import com.example.pos.data.model.ShiftLiveMetrics
import com.example.pos.data.local.ShiftAdjustmentEntity
import com.example.pos.data.local.ShiftCashCountEntity
import com.example.pos.data.local.ShiftCreditNoteEntity
import com.example.pos.data.local.ShiftDeclarationEntity
import com.example.pos.data.local.ShiftEventEntity
import com.example.pos.data.local.ShiftVersionEntity
import com.example.pos.data.model.CreditNoteRecord
import com.example.pos.data.model.DenominationItem
import com.example.pos.data.model.OnlineProviderDeclaration
import com.example.pos.data.model.ShiftCloseDraft
import com.example.pos.data.model.ShiftDetails
import com.example.pos.data.model.ShiftGateDecision
import com.example.pos.data.local.ShiftDao
import com.example.pos.data.local.ShiftDenominationEntity
import com.example.pos.domain.ShiftFinancialEngine
import com.example.pos.domain.CartCalculator
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PosRepository(
    private val dao: PosDao,
    private val shiftDao: ShiftDao? = null,
    private val database: AppDatabase? = null
) {

    constructor(context: Context) : this(
        AppDatabase.getInstance(context).posDao(),
        AppDatabase.getInstance(context).shiftDao(),
        AppDatabase.getInstance(context)
    )

    private suspend inline fun <T> runInTransaction(crossinline block: suspend () -> T): T {
        val db = database
        return if (db != null) {
            db.withTransaction { block() }
        } else {
            block()
        }
    }

    fun getShiftRepository(context: Context, syncRepository: SyncRepository? = null): ShiftRepository {
        val db = AppDatabase.getInstance(context)
        return ShiftRepositoryImpl(shiftDao = db.shiftDao(), posDao = dao, syncRepository = syncRepository)
    }

    // Current Organization Flow
    val organization: Flow<OrganizationEntity?> = dao.getOrganization()

    suspend fun getActiveOrganization(): OrganizationEntity? = dao.getActiveOrganizationSync()

    suspend fun purgeAllMockData() {
        dao.purgeMockOrganizations()
        dao.purgeMockUsers()
        dao.purgeMockStaff()
        dao.purgeMockProducts()
        dao.purgeMockCategories()
        dao.purgeMockCustomers()
        dao.purgeMockStock()
        dao.purgeMockBranches()
        dao.purgeMockRegisters()
    }

    suspend fun initializeDefaultStoreIfEmpty(ownerUid: String = "local_owner") {
        // Enforce clean state - do not insert mock users or mock organizations
        purgeAllMockData()
    }

    fun getBranches(orgId: String): Flow<List<BranchEntity>> = dao.getBranches(orgId)
    fun getRegisters(branchId: String): Flow<List<RegisterEntity>> = dao.getRegisters(branchId)
    fun getCategories(orgId: String): Flow<List<CategoryEntity>> = dao.getCategories(orgId)
    fun getProducts(orgId: String): Flow<List<ProductEntity>> = dao.getProducts(orgId)
    fun getProductsByCategory(orgId: String, catId: String): Flow<List<ProductEntity>> = dao.getProductsByCategory(orgId, catId)
    fun searchProducts(orgId: String, query: String): Flow<List<ProductEntity>> = dao.searchProducts(orgId, query)
    fun observeStock(productId: String, branchId: String): Flow<Int> = dao.observeStockQuantity(productId, branchId)
    fun getCustomers(orgId: String): Flow<List<CustomerEntity>> = dao.getCustomers(orgId)
    fun getCustomerLedger(customerId: String): Flow<List<CustomerLedgerEntity>> = dao.getCustomerLedger(customerId)
    fun getCustomerSales(customerId: String): Flow<List<SaleEntity>> = dao.getCustomerSales(customerId)
    fun getHeldOrders(orgId: String): Flow<List<HeldOrderEntity>> = dao.getHeldOrders(orgId)
    fun getSales(orgId: String): Flow<List<SaleEntity>> = dao.getSales(orgId)
    fun getAllSaleItems(orgId: String): Flow<List<SaleItemEntity>> = dao.getAllSaleItems(orgId)
    fun getAllSalePayments(orgId: String): Flow<List<SalePaymentEntity>> = dao.getAllSalePayments(orgId)
    fun getAllShifts(orgId: String): Flow<List<ShiftEntity>> = dao.getAllShifts(orgId)
    fun getRecentStockMovements(orgId: String): Flow<List<StockMovementEntity>> = dao.getRecentStockMovements(orgId)
    fun getActiveShift(registerId: String): Flow<ShiftEntity?> = dao.getActiveShift(registerId)
    suspend fun getActiveShiftSync(registerId: String): ShiftEntity? = dao.getActiveShiftSync(registerId)
    fun getActiveShiftForCashier(cashierUid: String): Flow<ShiftEntity?> = dao.getActiveShiftForCashier(cashierUid)
    suspend fun getActiveShiftForCashierSync(cashierUid: String): ShiftEntity? = dao.getActiveShiftForCashierSync(cashierUid)
    suspend fun getLastShiftForCashier(cashierUid: String): ShiftEntity? = dao.getLastShiftForCashier(cashierUid)
    suspend fun getShiftById(shiftId: String): ShiftEntity? = dao.getShiftById(shiftId)
    fun getCashMovements(shiftId: String): Flow<List<CashMovementEntity>> = dao.getCashMovements(shiftId)
    fun observePendingSync(orgId: String): Flow<Int> = dao.observePendingSyncCount(orgId)
    fun getAllUsers(): Flow<List<UserEntity>> = dao.getAllUsers()
    fun getUsers(orgId: String): Flow<List<UserEntity>> = dao.getUsers(orgId)
    fun getFiscalYears(orgId: String): Flow<List<FiscalYearEntity>> = dao.getFiscalYears(orgId)
    fun getActiveFiscalYear(orgId: String): Flow<FiscalYearEntity?> = dao.getActiveFiscalYear(orgId)
    suspend fun getActiveFiscalYearSync(orgId: String): FiscalYearEntity? = dao.getActiveFiscalYearSync(orgId)

    suspend fun initializeFiscalYearsIfNeeded(orgId: String): FiscalYearEntity {
        val existing = dao.getActiveFiscalYearSync(orgId)
        if (existing != null) return existing
        val all = dao.getFiscalYearsSync(orgId)
        if (all.isNotEmpty()) {
            val first = all.first()
            dao.setActiveFiscalYear(orgId, first.id)
            return first.copy(isCurrent = true)
        }
        val defaultList = FiscalYearHelper.getDefaultFiscalYears(orgId)
        dao.insertFiscalYears(defaultList)
        return defaultList.first { it.isCurrent }
    }

    suspend fun switchActiveFiscalYear(orgId: String, fyId: String) {
        dao.clearActiveFiscalYear(orgId)
        dao.setActiveFiscalYear(orgId, fyId)
    }

    suspend fun saveFiscalYear(fy: FiscalYearEntity) {
        if (fy.isCurrent) {
            dao.clearActiveFiscalYear(fy.orgId)
        }
        dao.insertFiscalYear(fy)
    }

    suspend fun saveOrganization(org: OrganizationEntity) {
        dao.insertOrg(org)
    }

    suspend fun saveUser(user: UserEntity) {
        dao.insertUser(user)
    }

    suspend fun findUserByRole(orgId: String, role: StaffRole): UserEntity? = dao.getUserByRole(orgId, role)

    suspend fun saveStaffMember(staff: StaffMemberEntity) {
        dao.insertStaff(staff)
    }

    suspend fun removeUser(orgId: String, userId: String) {
        dao.deleteUser(orgId, userId)
    }

    // Complete Sale Transaction
    suspend fun completeSale(
        orgId: String,
        branchId: String,
        registerId: String,
        cartItems: List<CartItem>,
        payments: List<PaymentEntry>,
        orderDiscountPercent: Double,
        taxMode: TaxMode,
        customerId: String?,
        cashierName: String
    ): SaleEntity = runInTransaction {
        val summary = CartCalculator.calculateSummary(cartItems, orderDiscountPercent, taxMode)
        val saleId = UUID.randomUUID().toString()

        val activeFy = dao.getActiveFiscalYearSync(orgId) ?: initializeFiscalYearsIfNeeded(orgId)
        val nextSeq = activeFy.currentInvoiceSequence + 1L
        dao.incrementInvoiceSequence(activeFy.id)
        val invoiceNo = FiscalYearHelper.formatFiscalInvoice(activeFy.invoicePrefix, activeFy.code, nextSeq)

        val sale = SaleEntity(
            id = saleId,
            orgId = orgId,
            branchId = branchId,
            registerId = registerId,
            invoiceNo = invoiceNo,
            fiscalYear = activeFy.name,
            customerId = customerId,
            subtotalInCents = summary.subtotalInCents,
            discountInCents = summary.orderDiscountInCents,
            taxInCents = summary.taxTotalInCents,
            grandTotalInCents = summary.grandTotalInCents,
            paymentStatus = "COMPLETED",
            createdAt = System.currentTimeMillis(),
            createdBy = cashierName
        )
        dao.insertSale(sale)

        val saleItems = cartItems.map { item ->
            SaleItemEntity(
                id = UUID.randomUUID().toString(),
                saleId = saleId,
                productId = item.product.id,
                productName = item.product.name,
                unitPriceInCents = item.unitPriceInCents,
                quantity = item.quantity,
                discountInCents = item.discountInCents,
                taxInCents = item.taxInCents,
                lineTotalInCents = item.totalInCents
            )
        }
        dao.insertSaleItems(saleItems)

        val salePayments = payments.map { p ->
            SalePaymentEntity(
                id = UUID.randomUUID().toString(),
                saleId = saleId,
                paymentType = p.type,
                amountInCents = p.amountInCents,
                changeGivenInCents = p.changeGivenInCents,
                referenceNo = p.referenceNo
            )
        }
        dao.insertSalePayments(salePayments)

        // Decrement stock via immutable ledger
        val stockMovements = cartItems.map { item ->
            StockMovementEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                branchId = branchId,
                productId = item.product.id,
                quantityDelta = -item.quantity,
                type = MovementType.SALE,
                referenceId = invoiceNo,
                createdAt = System.currentTimeMillis(),
                createdBy = cashierName
            )
        }
        dao.insertStockMovements(stockMovements)

        // Handle Customer Transaction / Credit Due recording if customer is attached
        if (!customerId.isNullOrBlank()) {
            val cust = dao.getCustomerById(customerId)
            if (cust != null) {
                val creditPayment = payments.find { it.type == PaymentType.CREDIT_DUE }
                if (creditPayment != null) {
                    dao.updateCustomerDue(customerId, creditPayment.amountInCents)
                    val updatedCust = dao.getCustomerById(customerId)
                    val newDue = updatedCust?.currentDueInCents ?: (cust.currentDueInCents + creditPayment.amountInCents)
                    val ledgerEntry = CustomerLedgerEntity(
                        id = UUID.randomUUID().toString(),
                        orgId = orgId,
                        customerId = customerId,
                        saleId = saleId,
                        entryType = "SALE_CREDIT",
                        amountInCents = creditPayment.amountInCents,
                        balanceAfterInCents = newDue,
                        notes = "Credit purchase on $invoiceNo"
                    )
                    dao.insertCustomerLedgerEntry(ledgerEntry)

                    // Enqueue ledger entry to Sync Outbox
                    val ledgerPayload = JSONObject().apply {
                        put("id", ledgerEntry.id)
                        put("orgId", orgId)
                        put("customerId", customerId)
                        put("saleId", saleId)
                        put("entryType", ledgerEntry.entryType)
                        put("amountInCents", ledgerEntry.amountInCents)
                        put("balanceAfterInCents", ledgerEntry.balanceAfterInCents)
                        put("notes", ledgerEntry.notes)
                        put("createdAt", ledgerEntry.createdAt)
                    }
                    dao.insertSyncOutbox(
                        SyncOutboxEntity(
                            id = UUID.randomUUID().toString(),
                            orgId = orgId,
                            tableName = "customer_ledger",
                            recordId = ledgerEntry.id,
                            payloadJson = ledgerPayload.toString(),
                            operation = OutboxOp.INSERT
                        )
                    )

                    // Sync updated customer due to Firestore
                    val customerPayload = JSONObject().apply {
                        put("id", cust.id)
                        put("orgId", orgId)
                        put("currentDueInCents", newDue)
                        put("updatedAt", System.currentTimeMillis())
                    }
                    dao.insertSyncOutbox(
                        SyncOutboxEntity(
                            id = UUID.randomUUID().toString(),
                            orgId = orgId,
                            tableName = "customers",
                            recordId = cust.id,
                            payloadJson = customerPayload.toString(),
                            operation = OutboxOp.UPDATE
                        )
                    )
                } else {
                    // Non-credit sale (Cash/Card/QR/Split) attached to customer
                    val paymentSummary = payments.joinToString { it.type.name }
                    val ledgerEntry = CustomerLedgerEntity(
                        id = UUID.randomUUID().toString(),
                        orgId = orgId,
                        customerId = customerId,
                        saleId = saleId,
                        entryType = "SALE_PAID",
                        amountInCents = 0L,
                        balanceAfterInCents = cust.currentDueInCents,
                        notes = "Invoice $invoiceNo ($paymentSummary)"
                    )
                    dao.insertCustomerLedgerEntry(ledgerEntry)

                    val ledgerPayload = JSONObject().apply {
                        put("id", ledgerEntry.id)
                        put("orgId", orgId)
                        put("customerId", customerId)
                        put("saleId", saleId)
                        put("entryType", ledgerEntry.entryType)
                        put("amountInCents", 0L)
                        put("balanceAfterInCents", cust.currentDueInCents)
                        put("notes", ledgerEntry.notes)
                        put("createdAt", ledgerEntry.createdAt)
                    }
                    dao.insertSyncOutbox(
                        SyncOutboxEntity(
                            id = UUID.randomUUID().toString(),
                            orgId = orgId,
                            tableName = "customer_ledger",
                            recordId = ledgerEntry.id,
                            payloadJson = ledgerPayload.toString(),
                            operation = OutboxOp.INSERT
                        )
                    )
                }
            }
        }

        // Enqueue to Sync Outbox for cloud synchronization
        val salePayload = JSONObject().apply {
            put("id", sale.id)
            put("orgId", sale.orgId)
            put("branchId", sale.branchId)
            put("registerId", sale.registerId)
            put("invoiceNo", sale.invoiceNo)
            put("fiscalYear", sale.fiscalYear)
            put("customerId", sale.customerId ?: "")
            put("subtotalInCents", sale.subtotalInCents)
            put("discountInCents", sale.discountInCents)
            put("taxInCents", sale.taxInCents)
            put("grandTotalInCents", sale.grandTotalInCents)
            put("paymentStatus", sale.paymentStatus)
            put("createdAt", sale.createdAt)
            put("createdBy", sale.createdBy)
        }.toString()

        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                tableName = "sales",
                recordId = sale.id,
                payloadJson = salePayload,
                operation = OutboxOp.INSERT
            )
        )

        for (item in saleItems) {
            val itemPayload = JSONObject().apply {
                put("id", item.id)
                put("orgId", orgId)
                put("saleId", item.saleId)
                put("productId", item.productId)
                put("productName", item.productName)
                put("unitPriceInCents", item.unitPriceInCents)
                put("quantity", item.quantity)
                put("discountInCents", item.discountInCents)
                put("taxInCents", item.taxInCents)
                put("lineTotalInCents", item.lineTotalInCents)
            }.toString()
            dao.insertSyncOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    orgId = orgId,
                    tableName = "sale_items",
                    recordId = item.id,
                    payloadJson = itemPayload,
                    operation = OutboxOp.INSERT
                )
            )
        }

        for (payment in salePayments) {
            val payPayload = JSONObject().apply {
                put("id", payment.id)
                put("orgId", orgId)
                put("saleId", payment.saleId)
                put("paymentType", payment.paymentType.name)
                put("amountInCents", payment.amountInCents)
                put("changeGivenInCents", payment.changeGivenInCents)
                put("referenceNo", payment.referenceNo)
            }.toString()
            dao.insertSyncOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    orgId = orgId,
                    tableName = "sale_payments",
                    recordId = payment.id,
                    payloadJson = payPayload,
                    operation = OutboxOp.INSERT
                )
            )
        }

        for (sm in stockMovements) {
            val smPayload = JSONObject().apply {
                put("id", sm.id)
                put("orgId", orgId)
                put("branchId", sm.branchId)
                put("productId", sm.productId)
                put("quantityDelta", sm.quantityDelta)
                put("type", sm.type.name)
                put("referenceId", sm.referenceId)
                put("createdAt", sm.createdAt)
                put("createdBy", sm.createdBy)
            }.toString()
            dao.insertSyncOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    orgId = orgId,
                    tableName = "stock_movements",
                    recordId = sm.id,
                    payloadJson = smPayload,
                    operation = OutboxOp.INSERT
                )
            )
        }

        sale
    }

    // Customer Debt Repayment (Credit IN)
    suspend fun receiveCustomerDueRepayment(
        orgId: String,
        customerId: String,
        amountInCents: Long,
        paymentType: PaymentType,
        notes: String
    ) = runInTransaction {
        val cust = dao.getCustomerById(customerId) ?: return@runInTransaction
        dao.updateCustomerDue(customerId, -amountInCents)
        val updatedCust = dao.getCustomerById(customerId)
        val newDue = updatedCust?.currentDueInCents ?: (cust.currentDueInCents - amountInCents)

        val entry = CustomerLedgerEntity(
            id = UUID.randomUUID().toString(),
            orgId = orgId,
            customerId = customerId,
            saleId = null,
            entryType = "DEBT_REPAYMENT",
            amountInCents = -amountInCents,
            balanceAfterInCents = newDue,
            notes = "Repayment via ${paymentType.name}: $notes".trim()
        )
        dao.insertCustomerLedgerEntry(entry)

        val ledgerPayload = JSONObject().apply {
            put("id", entry.id)
            put("orgId", orgId)
            put("customerId", customerId)
            put("entryType", entry.entryType)
            put("amountInCents", entry.amountInCents)
            put("balanceAfterInCents", entry.balanceAfterInCents)
            put("notes", entry.notes)
            put("createdAt", entry.createdAt)
            put("newDue", newDue)
        }

        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                tableName = "customer_ledger",
                recordId = entry.id,
                payloadJson = ledgerPayload.toString(),
                operation = OutboxOp.INSERT
            )
        )

        // Sync updated customer due to Firestore
        val customerPayload = JSONObject().apply {
            put("id", cust.id)
            put("orgId", orgId)
            put("currentDueInCents", newDue)
            put("updatedAt", System.currentTimeMillis())
        }
        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                tableName = "customers",
                recordId = cust.id,
                payloadJson = customerPayload.toString(),
                operation = OutboxOp.UPDATE
            )
        )
    }

    // Customer Credit Issued (Credit OUT)
    suspend fun giveCustomerCredit(
        orgId: String,
        customerId: String,
        amountInCents: Long,
        notes: String
    ) = runInTransaction {
        val cust = dao.getCustomerById(customerId) ?: return@runInTransaction
        dao.updateCustomerDue(customerId, amountInCents)
        val updatedCust = dao.getCustomerById(customerId)
        val newDue = updatedCust?.currentDueInCents ?: (cust.currentDueInCents + amountInCents)

        val entry = CustomerLedgerEntity(
            id = UUID.randomUUID().toString(),
            orgId = orgId,
            customerId = customerId,
            saleId = null,
            entryType = "CREDIT_GIVEN",
            amountInCents = amountInCents,
            balanceAfterInCents = newDue,
            notes = notes.ifBlank { "Store credit issued to customer" }
        )
        dao.insertCustomerLedgerEntry(entry)

        val ledgerPayload = JSONObject().apply {
            put("id", entry.id)
            put("orgId", orgId)
            put("customerId", customerId)
            put("entryType", entry.entryType)
            put("amountInCents", entry.amountInCents)
            put("balanceAfterInCents", entry.balanceAfterInCents)
            put("notes", entry.notes)
            put("createdAt", entry.createdAt)
            put("newDue", newDue)
        }

        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                tableName = "customer_ledger",
                recordId = entry.id,
                payloadJson = ledgerPayload.toString(),
                operation = OutboxOp.INSERT
            )
        )

        // Sync updated customer due to Firestore
        val customerPayload = JSONObject().apply {
            put("id", cust.id)
            put("orgId", orgId)
            put("currentDueInCents", newDue)
            put("updatedAt", System.currentTimeMillis())
        }
        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                tableName = "customers",
                recordId = cust.id,
                payloadJson = customerPayload.toString(),
                operation = OutboxOp.UPDATE
            )
        )
    }

    // Customer Creation & Credit Terms
    suspend fun createCustomer(
        orgId: String,
        name: String,
        phone: String,
        email: String,
        creditLimitInCents: Long,
        paymentTermsDays: Int = 30,
        gracePeriodDays: Int = 7,
        notes: String = "",
        openingBalanceInCents: Long = 0L,
        openingBalanceType: String = "TO_RECEIVE", // "TO_RECEIVE" (Asset / Customer Due) or "TO_GIVE" (Liability / Customer Advance)
        asOfDate: String = ""
    ): CustomerEntity {
        val initialDueInCents = when {
            openingBalanceInCents <= 0L -> 0L
            openingBalanceType == "TO_GIVE" -> -openingBalanceInCents
            else -> openingBalanceInCents
        }

        val customer = CustomerEntity(
            id = "cust_${UUID.randomUUID().toString().take(8)}",
            orgId = orgId,
            name = name.trim(),
            phone = phone.trim(),
            email = email.trim(),
            creditLimitInCents = creditLimitInCents,
            currentDueInCents = initialDueInCents,
            createdAt = System.currentTimeMillis()
        )
        dao.insertCustomer(customer)

        val payload = JSONObject().apply {
            put("id", customer.id)
            put("orgId", orgId)
            put("name", customer.name)
            put("phone", customer.phone)
            put("email", customer.email)
            put("creditLimitInCents", customer.creditLimitInCents)
            put("currentDueInCents", initialDueInCents)
            put("paymentTermsDays", paymentTermsDays)
            put("gracePeriodDays", gracePeriodDays)
            put("notes", notes)
            put("openingBalanceInCents", openingBalanceInCents)
            put("openingBalanceType", openingBalanceType)
            put("asOfDate", asOfDate)
            put("createdAt", customer.createdAt)
            put("updatedAt", System.currentTimeMillis())
        }

        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                tableName = "customers",
                recordId = customer.id,
                payloadJson = payload.toString(),
                operation = OutboxOp.INSERT
            )
        )

        // Automatically record Opening Balance in the Customer Ledger if non-zero
        if (openingBalanceInCents > 0L) {
            val entryType = if (openingBalanceType == "TO_GIVE") "OPENING_BALANCE_ADVANCE" else "OPENING_BALANCE_RECEIVABLE"
            val ledgerNote = buildString {
                append("Opening Balance (${if (openingBalanceType == "TO_GIVE") "To Give / Advance / दिन बाँकी" else "To Receive / Due / लिन बाँकी"})")
                if (asOfDate.isNotBlank()) append(" as of $asOfDate")
                if (notes.isNotBlank()) append(" • $notes")
            }

            val ledgerEntry = CustomerLedgerEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                customerId = customer.id,
                saleId = null,
                entryType = entryType,
                amountInCents = initialDueInCents,
                balanceAfterInCents = initialDueInCents,
                notes = ledgerNote,
                createdAt = System.currentTimeMillis()
            )
            dao.insertCustomerLedgerEntry(ledgerEntry)

            val ledgerPayload = JSONObject().apply {
                put("id", ledgerEntry.id)
                put("orgId", orgId)
                put("customerId", customer.id)
                put("entryType", ledgerEntry.entryType)
                put("amountInCents", ledgerEntry.amountInCents)
                put("balanceAfterInCents", ledgerEntry.balanceAfterInCents)
                put("notes", ledgerEntry.notes)
                put("createdAt", ledgerEntry.createdAt)
                put("newDue", initialDueInCents)
            }

            dao.insertSyncOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    orgId = orgId,
                    tableName = "customer_ledger",
                    recordId = ledgerEntry.id,
                    payloadJson = ledgerPayload.toString(),
                    operation = OutboxOp.INSERT
                )
            )
        }

        return customer
    }

    // Stock receiving / adjustment
    suspend fun recordStockAdjustment(
        orgId: String,
        branchId: String,
        productId: String,
        delta: Int,
        type: MovementType,
        reason: String,
        staffName: String
    ) {
        val movement = StockMovementEntity(
            id = UUID.randomUUID().toString(),
            orgId = orgId,
            branchId = branchId,
            productId = productId,
            quantityDelta = delta,
            type = type,
            referenceId = reason,
            createdAt = System.currentTimeMillis(),
            createdBy = staffName
        )
        dao.insertStockMovement(movement)

        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                tableName = "stock_movements",
                recordId = movement.id,
                payloadJson = JSONObject().apply {
                    put("id", movement.id)
                    put("orgId", orgId)
                    put("branchId", branchId)
                    put("productId", productId)
                    put("quantityDelta", delta)
                    put("type", type.name)
                    put("referenceId", reason)
                    put("createdAt", movement.createdAt)
                    put("createdBy", staffName)
                }.toString(),
                operation = OutboxOp.INSERT
            )
        )
    }

    suspend fun recordStockIn(
        orgId: String,
        branchId: String,
        productId: String,
        quantityMilliUnits: Long,
        unitCostInCents: Long,
        supplierName: String,
        invoiceRef: String,
        batchNumber: String,
        expiryDate: String,
        notes: String,
        staffName: String
    ): StockMovementEntity {
        val qtyUnits = (quantityMilliUnits / 1000L).toInt().coerceAtLeast(1)
        val movement = StockMovementEntity(
            id = UUID.randomUUID().toString(),
            orgId = orgId,
            branchId = branchId,
            productId = productId,
            quantityDelta = qtyUnits,
            type = MovementType.RECEIPT,
            referenceId = if (invoiceRef.isNotBlank()) invoiceRef else "PO-DIRECT",
            createdAt = System.currentTimeMillis(),
            createdBy = staffName
        )
        dao.insertStockMovement(movement)

        // If unit cost provided and differs, update product cost price
        if (unitCostInCents > 0) {
            val product = dao.getProductById(productId)
            if (product != null && product.costPriceInCents != unitCostInCents) {
                dao.insertProduct(product.copy(costPriceInCents = unitCostInCents))
            }
        }

        // Outbox record
        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                tableName = "stock_movements",
                recordId = movement.id,
                payloadJson = JSONObject().apply {
                    put("productId", productId)
                    put("quantityMilliUnits", quantityMilliUnits)
                    put("quantityUnits", qtyUnits)
                    put("unitCostInCents", unitCostInCents)
                    put("supplierName", supplierName)
                    put("invoiceRef", invoiceRef)
                    put("batchNumber", batchNumber)
                    put("expiryDate", expiryDate)
                    put("notes", notes)
                    put("createdBy", staffName)
                }.toString(),
                operation = OutboxOp.INSERT
            )
        )
        return movement
    }

    // Barcode Lookup & Pack Size Management
    suspend fun lookupBarcode(orgId: String, barcode: String): BarcodeLookupResult {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isEmpty()) return BarcodeLookupResult(found = false)

        // 1. Check item_barcodes table (multi-pack & secondary barcodes)
        val link = dao.getBarcodeLink(orgId, cleanBarcode)
        if (link != null) {
            val product = dao.getProductById(link.itemId)
            if (product != null) {
                return BarcodeLookupResult(
                    found = true,
                    item = product,
                    packQty = link.packQty,
                    barcode = cleanBarcode,
                    isSecondaryBarcode = true
                )
            }
        }

        // 2. Check primary barcode on products table
        val product = dao.getProductByBarcode(orgId, cleanBarcode)
        if (product != null) {
            return BarcodeLookupResult(
                found = true,
                item = product,
                packQty = 1,
                barcode = cleanBarcode,
                isSecondaryBarcode = false
            )
        }

        return BarcodeLookupResult(found = false, barcode = cleanBarcode)
    }

    suspend fun linkBarcodeToItem(
        orgId: String,
        itemId: String,
        barcode: String,
        packQty: Int = 1
    ): ItemBarcodeEntity {
        val link = com.example.pos.data.local.ItemBarcodeEntity(
            id = UUID.randomUUID().toString(),
            orgId = orgId,
            itemId = itemId,
            barcode = barcode.trim(),
            packQty = packQty.coerceAtLeast(1)
        )
        dao.insertItemBarcode(link)

        // Outbox sync
        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                tableName = "item_barcodes",
                recordId = link.id,
                payloadJson = JSONObject().apply {
                    put("itemId", itemId)
                    put("barcode", link.barcode)
                    put("packQty", link.packQty)
                }.toString(),
                operation = OutboxOp.INSERT
            )
        )
        return link
    }

    suspend fun createProductWithBarcode(
        orgId: String,
        name: String,
        sku: String,
        barcode: String,
        costPriceInCents: Long,
        sellingPriceInCents: Long,
        unit: String = "pcs",
        packQty: Int = 1
    ): ProductEntity {
        val product = ProductEntity(
            id = UUID.randomUUID().toString(),
            orgId = orgId,
            categoryId = "cat_default",
            name = name.trim(),
            sku = sku.trim(),
            barcode = barcode.trim(),
            costPriceInCents = costPriceInCents,
            sellingPriceInCents = sellingPriceInCents,
            unit = unit.trim().ifBlank { "pcs" }
        )
        dao.insertProduct(product)

        // If packQty > 1, also create secondary link with carton pack size
        if (packQty > 1) {
            linkBarcodeToItem(orgId, product.id, barcode.trim(), packQty)
        }

        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                tableName = "products",
                recordId = product.id,
                payloadJson = JSONObject().apply {
                    put("id", product.id)
                    put("orgId", orgId)
                    put("categoryId", product.categoryId)
                    put("name", product.name)
                    put("sku", product.sku)
                    put("barcode", product.barcode)
                    put("costPriceInCents", product.costPriceInCents)
                    put("sellingPriceInCents", product.sellingPriceInCents)
                    put("taxRatePercent", product.taxRatePercent)
                    put("unit", product.unit)
                    put("minStockAlert", product.minStockAlert)
                }.toString(),
                operation = OutboxOp.INSERT
            )
        )
        return product
    }

    // Shifts Lifecycle & Cash Control
    fun getShiftsByStatus(orgId: String, status: String?): Flow<List<ShiftEntity>> =
        dao.getShiftsByStatus(orgId, status)

    suspend fun getLastClosedShiftExpectedFloat(registerId: String): Long {
        return dao.getLastClosedShiftForRegister(registerId)?.closingFloatInCents ?: 0L
    }

    suspend fun checkShiftGate(registerId: String, cashierUid: String): ShiftGateDecision {
        val now = System.currentTimeMillis()
        val activeShift = dao.getActiveShiftSync(registerId)
        if (activeShift != null) {
            val isStale = ShiftFinancialEngine.isShiftStale(activeShift.openedAt, activeShift.businessDate, now)
            if (isStale) {
                val hours = (now - activeShift.openedAt) / (1000 * 60 * 60)
                return ShiftGateDecision.StaleShiftBlocked(activeShift, hours)
            }
            if (activeShift.cashierUid != cashierUid) {
                return ShiftGateDecision.TerminalInUse(activeShift)
            }
            return ShiftGateDecision.ResumeShift(activeShift)
        }

        // Check if this cashier has an active shift on another terminal
        val cashierActiveShift = dao.getActiveShiftForCashierSync(cashierUid)
        if (cashierActiveShift != null) {
            val isStale = ShiftFinancialEngine.isShiftStale(cashierActiveShift.openedAt, cashierActiveShift.businessDate, now)
            if (isStale) {
                val hours = (now - cashierActiveShift.openedAt) / (1000 * 60 * 60)
                return ShiftGateDecision.StaleShiftBlocked(cashierActiveShift, hours)
            }
            return ShiftGateDecision.TerminalInUse(cashierActiveShift)
        }

        return ShiftGateDecision.NoActiveShift
    }

    suspend fun openShift(
        orgId: String,
        registerId: String,
        cashierUid: String,
        cashierName: String,
        openingFloatInCents: Long,
        branchId: String = "br_main_01",
        expectedFloatInCents: Long = 0L,
        openingGapReason: String = "",
        denominationCounts: List<DenominationItem> = emptyList(),
        deviceId: String = ""
    ): ShiftEntity {
        val now = System.currentTimeMillis()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val openingGap = openingFloatInCents - expectedFloatInCents

        val shift = ShiftEntity(
            id = UUID.randomUUID().toString(),
            orgId = orgId,
            branchId = branchId,
            registerId = registerId,
            cashierUid = cashierUid,
            cashierName = cashierName,
            businessDate = todayStr,
            openedAt = now,
            openingFloatInCents = openingFloatInCents,
            openingExpectedInCents = expectedFloatInCents,
            openingGapInCents = openingGap,
            openingGapReason = openingGapReason,
            status = "OPEN",
            reviewStatus = "PENDING",
            version = 1,
            lastDeviceId = deviceId
        )
        dao.insertShift(shift)
        dao.updateRegisterCurrentShift(registerId, shift.id)

        // Record opening cash count denominations if provided
        if (denominationCounts.isNotEmpty()) {
            val countEntities = denominationCounts.map { item ->
                ShiftCashCountEntity(
                    id = UUID.randomUUID().toString(),
                    shiftId = shift.id,
                    phase = "OPENING",
                    denomination = item.denomination,
                    count = item.count,
                    subtotalInCents = item.totalInCents
                )
            }
            dao.insertShiftCashCounts(countEntities)

            // Also persist into offline-first shift_denominations Room table
            val denomMap = denominationCounts.associate { it.denomination to it.count }
            shiftDao?.upsertShift(
                ShiftDenominationEntity(
                    shiftId = shift.id,
                    cashierId = cashierUid,
                    status = "OPEN",
                    denominations = denomMap,
                    totalAmount = openingFloatInCents / 100L,
                    isSynced = false,
                    openedAt = now,
                    updatedAt = now
                )
            )
        }

        // Audit Trail Event
        val event = ShiftEventEntity(
            id = UUID.randomUUID().toString(),
            shiftId = shift.id,
            eventType = "OPEN_SHIFT",
            actorUid = cashierUid,
            actorName = cashierName,
            detailsJson = """{"openingFloat":$openingFloatInCents,"expectedFloat":$expectedFloatInCents,"gap":$openingGap,"gapReason":"$openingGapReason"}"""
        )
        dao.insertShiftEvent(event)

        // Enqueue Shift to Sync Outbox
        val shiftPayload = JSONObject().apply {
            put("id", shift.id)
            put("orgId", shift.orgId)
            put("branchId", shift.branchId)
            put("registerId", shift.registerId)
            put("cashierUid", shift.cashierUid)
            put("cashierName", shift.cashierName)
            put("openedAt", shift.openedAt)
            put("openingFloatInCents", shift.openingFloatInCents)
            put("status", shift.status)
            put("notes", shift.openingGapReason)
        }.toString()
        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = orgId,
                tableName = "shifts",
                recordId = shift.id,
                payloadJson = shiftPayload,
                operation = OutboxOp.INSERT
            )
        )

        return shift
    }

    suspend fun addCashMovement(
        shiftId: String,
        movementType: String,
        amountInCents: Long,
        reason: String,
        tender: String = "CASH",
        createdBy: String = ""
    ) {
        val movement = CashMovementEntity(
            id = UUID.randomUUID().toString(),
            shiftId = shiftId,
            movementType = movementType,
            amountInCents = amountInCents,
            reason = reason,
            tender = tender,
            createdBy = createdBy,
            createdAt = System.currentTimeMillis()
        )
        dao.insertCashMovement(movement)

        // Audit Trail Event
        val shift = dao.getShiftById(shiftId)
        val event = ShiftEventEntity(
            id = UUID.randomUUID().toString(),
            shiftId = shiftId,
            eventType = "CASH_MOVEMENT_$movementType",
            actorUid = shift?.cashierUid ?: "",
            actorName = shift?.cashierName ?: "",
            detailsJson = """{"type":"$movementType","amount":$amountInCents,"tender":"$tender","reason":"$reason"}"""
        )
        dao.insertShiftEvent(event)

        if (shift != null && shift.orgId.isNotBlank()) {
            val movementPayload = JSONObject().apply {
                put("id", movement.id)
                put("orgId", shift.orgId)
                put("shiftId", movement.shiftId)
                put("movementType", movement.movementType)
                put("amountInCents", movement.amountInCents)
                put("reason", movement.reason)
                put("tender", movement.tender)
                put("createdBy", movement.createdBy)
                put("createdAt", movement.createdAt)
            }.toString()
            dao.insertSyncOutbox(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    orgId = shift.orgId,
                    tableName = "cash_movements",
                    recordId = movement.id,
                    payloadJson = movementPayload,
                    operation = OutboxOp.INSERT
                )
            )
        }
    }

    suspend fun getLiveShiftMetrics(shiftId: String): ShiftLiveMetrics? {
        val currShift = dao.getShiftById(shiftId) ?: dao.getActiveShiftSync(shiftId) ?: dao.getActiveShiftSync("reg_01") ?: return null

        val salesCount = dao.getShiftSalesCount(currShift.registerId, currShift.openedAt)
        val salesTotal = dao.getShiftSalesTotal(currShift.registerId, currShift.openedAt)
        val cashSales = dao.getShiftCashSales(currShift.registerId, currShift.openedAt)
        val cardSales = dao.getShiftCardSales(currShift.registerId, currShift.openedAt)
        val qrSales = dao.getShiftQrSales(currShift.registerId, currShift.openedAt)
        val creditSales = dao.getShiftCreditSales(currShift.registerId, currShift.openedAt)

        val payIns = dao.getShiftPayInTotal(currShift.id)
        val payOuts = dao.getShiftPayOutTotal(currShift.id)
        val safeDrops = dao.getShiftSafeDropTotal(currShift.id)
        val creditCollections = dao.getShiftCreditCollectionTotal(currShift.id)

        // ShiftFinancialEngine calculation
        val expectedCash = ShiftFinancialEngine.calculateExpectedCash(
            openingFloatInCents = currShift.openingFloatInCents,
            cashSalesInCents = cashSales,
            cashCreditCollectionsInCents = creditCollections,
            paidInInCents = payIns,
            cashRefundsInCents = 0L,
            paidOutInCents = payOuts,
            safeDropsInCents = safeDrops
        )
        val expectedOnline = cardSales + qrSales
        val isStale = ShiftFinancialEngine.isShiftStale(currShift.openedAt, currShift.businessDate, System.currentTimeMillis())

        return ShiftLiveMetrics(
            shiftId = currShift.id,
            cashierName = currShift.cashierName,
            openedAt = currShift.openedAt,
            openingFloatInCents = currShift.openingFloatInCents,
            salesCount = salesCount,
            salesTotalInCents = salesTotal,
            cashSalesInCents = cashSales,
            cardSalesInCents = cardSales,
            qrSalesInCents = qrSales,
            creditSalesInCents = creditSales,
            expectedOnlineInCents = expectedOnline,
            payInInCents = payIns,
            payOutInCents = payOuts,
            expectedCashInCents = expectedCash,
            safeDropInCents = safeDrops,
            creditCollectionInCents = creditCollections,
            isStale = isStale
        )
    }

    suspend fun saveCloseDraft(draft: ShiftCloseDraft) {
        val currShift = dao.getShiftById(draft.shiftId) ?: return
        val updatedShift = currShift.copy(
            status = "CLOSING",
            countedCashInCents = draft.countedCashInCents,
            closingFloatInCents = draft.closingFloatInCents,
            depositAmountInCents = draft.depositAmountInCents,
            varianceReason = draft.varianceReason,
            notes = draft.closingNotes
        )
        dao.updateShift(updatedShift)

        if (draft.denominationCounts.isNotEmpty()) {
            val countEntities = draft.denominationCounts.map { item ->
                ShiftCashCountEntity(
                    id = UUID.randomUUID().toString(),
                    shiftId = draft.shiftId,
                    phase = "CLOSING",
                    denomination = item.denomination,
                    count = item.count,
                    subtotalInCents = item.totalInCents
                )
            }
            dao.insertShiftCashCounts(countEntities)
        }

        if (draft.onlineDeclarations.isNotEmpty()) {
            val declEntities = draft.onlineDeclarations.map { item ->
                ShiftDeclarationEntity(
                    id = UUID.randomUUID().toString(),
                    shiftId = draft.shiftId,
                    provider = item.provider,
                    declaredAmountInCents = item.declaredAmountInCents,
                    slipCount = item.slipCount,
                    notes = item.referenceNotes
                )
            }
            dao.insertShiftDeclarations(declEntities)
        }

        if (draft.creditNotes.isNotEmpty()) {
            val cnEntities = draft.creditNotes.map { item ->
                ShiftCreditNoteEntity(
                    id = UUID.randomUUID().toString(),
                    shiftId = draft.shiftId,
                    creditNoteNo = item.creditNoteNo,
                    totalAmountInCents = item.totalAmountInCents,
                    customerCreditInCents = item.customerCreditInCents,
                    bankRefundInCents = item.bankRefundInCents,
                    cashRefundInCents = item.cashRefundInCents,
                    reason = item.reason
                )
            }
            dao.insertShiftCreditNotes(cnEntities)
        }

        dao.insertShiftEvent(
            ShiftEventEntity(
                id = UUID.randomUUID().toString(),
                shiftId = draft.shiftId,
                eventType = "SAVE_CLOSE_DRAFT",
                actorUid = currShift.cashierUid,
                actorName = currShift.cashierName,
                detailsJson = """{"countedCash":${draft.countedCashInCents},"status":"CLOSING"}"""
            )
        )
    }

    suspend fun closeShift(
        shiftId: String,
        countedCashInCents: Long,
        countedOnlineInCents: Long = 0L,
        onlineBillsCount: Int = 0,
        closingNotes: String = ""
    ): ZReport? {
        return closeShiftAdvanced(
            shiftId = shiftId,
            countedCashInCents = countedCashInCents,
            denominationCounts = emptyList(),
            onlineDeclarations = listOf(
                OnlineProviderDeclaration("Online Total", countedOnlineInCents, onlineBillsCount)
            ),
            creditNotes = emptyList(),
            closingFloatInCents = 0L,
            depositAmountInCents = countedCashInCents,
            varianceReason = "",
            closingNotes = closingNotes
        )
    }

    suspend fun closeShiftAdvanced(
        shiftId: String,
        countedCashInCents: Long,
        denominationCounts: List<DenominationItem> = emptyList(),
        onlineDeclarations: List<OnlineProviderDeclaration> = emptyList(),
        creditNotes: List<CreditNoteRecord> = emptyList(),
        closingFloatInCents: Long = 0L,
        depositAmountInCents: Long = 0L,
        varianceReason: String = "",
        closingNotes: String = "",
        closedBy: String = ""
    ): ZReport? {
        val currShift = dao.getShiftById(shiftId) ?: dao.getActiveShiftSync(shiftId) ?: dao.getActiveShiftSync("reg_01") ?: return null

        val salesCount = dao.getShiftSalesCount(currShift.registerId, currShift.openedAt)
        val salesTotal = dao.getShiftSalesTotal(currShift.registerId, currShift.openedAt)
        val cashSales = dao.getShiftCashSales(currShift.registerId, currShift.openedAt)
        val cardSales = dao.getShiftCardSales(currShift.registerId, currShift.openedAt)
        val qrSales = dao.getShiftQrSales(currShift.registerId, currShift.openedAt)
        val creditSales = dao.getShiftCreditSales(currShift.registerId, currShift.openedAt)

        val payIns = dao.getShiftPayInTotal(currShift.id)
        val payOuts = dao.getShiftPayOutTotal(currShift.id)
        val safeDrops = dao.getShiftSafeDropTotal(currShift.id)
        val creditCollections = dao.getShiftCreditCollectionTotal(currShift.id)

        // Only the cash refund portion of credit notes reduces expected cash in drawer
        val cashRefundsFromCreditNotes = creditNotes.sumOf { it.cashRefundInCents }

        val expectedCash = ShiftFinancialEngine.calculateExpectedCash(
            openingFloatInCents = currShift.openingFloatInCents,
            cashSalesInCents = cashSales,
            cashCreditCollectionsInCents = creditCollections,
            paidInInCents = payIns,
            cashRefundsInCents = cashRefundsFromCreditNotes,
            paidOutInCents = payOuts,
            safeDropsInCents = safeDrops
        )
        val variance = ShiftFinancialEngine.calculateCashVariance(countedCashInCents, expectedCash)

        val expectedOnline = cardSales + qrSales
        val declaredOnlineTotal = onlineDeclarations.sumOf { it.declaredAmountInCents }
        val onlineVariance = declaredOnlineTotal - expectedOnline
        val totalOnlineSlips = onlineDeclarations.sumOf { it.slipCount }

        // Determine final review status
        val varianceCheck = ShiftFinancialEngine.checkVarianceThreshold(variance)
        val finalReviewStatus = if (varianceCheck == ShiftFinancialEngine.VarianceStatus.UNDER_TOLERANCE) {
            "APPROVED"
        } else if (varianceCheck == ShiftFinancialEngine.VarianceStatus.WARN_REASON_REQUIRED) {
            "PENDING_REVIEW"
        } else {
            "FLAGGED"
        }

        val closedAt = System.currentTimeMillis()
        val actor = closedBy.ifBlank { currShift.cashierName }

        val combinedNotes = buildString {
            if (declaredOnlineTotal > 0 || totalOnlineSlips > 0) {
                append("Online: $totalOnlineSlips slips, Declared: ${CartCalculator.formatMoney(declaredOnlineTotal)} (Expected: ${CartCalculator.formatMoney(expectedOnline)}, Var: ${CartCalculator.formatMoney(onlineVariance)})")
            }
            if (varianceReason.isNotBlank()) {
                if (isNotEmpty()) append(" | ")
                append("Variance Reason: $varianceReason")
            }
            if (closingNotes.isNotBlank()) {
                if (isNotEmpty()) append(" | ")
                append("Notes: $closingNotes")
            }
        }

        val updatedShift = currShift.copy(
            closedAt = closedAt,
            countedCashInCents = countedCashInCents,
            expectedCashInCents = expectedCash,
            closingFloatInCents = closingFloatInCents,
            depositAmountInCents = depositAmountInCents,
            varianceInCents = variance,
            varianceReason = varianceReason,
            status = "CLOSED",
            closedBy = actor,
            reviewStatus = finalReviewStatus,
            notes = combinedNotes
        )
        dao.updateShift(updatedShift)
        dao.updateRegisterCurrentShift(currShift.registerId, null)

        // Enqueue Shift Update to Sync Outbox
        val shiftPayload = JSONObject().apply {
            put("id", updatedShift.id)
            put("orgId", updatedShift.orgId)
            put("branchId", updatedShift.branchId)
            put("registerId", updatedShift.registerId)
            put("cashierUid", updatedShift.cashierUid)
            put("cashierName", updatedShift.cashierName)
            put("openedAt", updatedShift.openedAt)
            put("openingFloatInCents", updatedShift.openingFloatInCents)
            put("closedAt", updatedShift.closedAt ?: closedAt)
            put("countedCashInCents", updatedShift.countedCashInCents ?: 0L)
            put("expectedCashInCents", updatedShift.expectedCashInCents ?: 0L)
            put("varianceInCents", updatedShift.varianceInCents ?: 0L)
            put("status", updatedShift.status)
            put("closedBy", updatedShift.closedBy)
            put("notes", updatedShift.notes)
        }.toString()
        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = updatedShift.orgId,
                tableName = "shifts",
                recordId = updatedShift.id,
                payloadJson = shiftPayload,
                operation = OutboxOp.UPDATE
            )
        )

        // Save closing cash counts
        if (denominationCounts.isNotEmpty()) {
            val countEntities = denominationCounts.map { item ->
                ShiftCashCountEntity(
                    id = UUID.randomUUID().toString(),
                    shiftId = currShift.id,
                    phase = "CLOSING",
                    denomination = item.denomination,
                    count = item.count,
                    subtotalInCents = item.totalInCents
                )
            }
            dao.insertShiftCashCounts(countEntities)

            // Also persist closing float denominations into offline-first shift_denominations Room table
            val denomMap = denominationCounts.associate { it.denomination to it.count }
            shiftDao?.upsertShift(
                ShiftDenominationEntity(
                    shiftId = updatedShift.id,
                    cashierId = updatedShift.cashierUid,
                    status = "CLOSED",
                    denominations = denomMap,
                    totalAmount = countedCashInCents / 100L,
                    isSynced = false,
                    openedAt = updatedShift.openedAt,
                    closedAt = closedAt,
                    updatedAt = closedAt
                )
            )
        } else {
            shiftDao?.closeShiftForCashier(updatedShift.id, updatedShift.cashierUid, closedAt)
        }

        // Save declarations
        if (onlineDeclarations.isNotEmpty()) {
            val declEntities = onlineDeclarations.map { item ->
                ShiftDeclarationEntity(
                    id = UUID.randomUUID().toString(),
                    shiftId = currShift.id,
                    provider = item.provider,
                    declaredAmountInCents = item.declaredAmountInCents,
                    slipCount = item.slipCount,
                    notes = item.referenceNotes
                )
            }
            dao.insertShiftDeclarations(declEntities)
        }

        // Save credit notes
        if (creditNotes.isNotEmpty()) {
            val cnEntities = creditNotes.map { item ->
                ShiftCreditNoteEntity(
                    id = UUID.randomUUID().toString(),
                    shiftId = currShift.id,
                    creditNoteNo = item.creditNoteNo,
                    totalAmountInCents = item.totalAmountInCents,
                    customerCreditInCents = item.customerCreditInCents,
                    bankRefundInCents = item.bankRefundInCents,
                    cashRefundInCents = item.cashRefundInCents,
                    reason = item.reason
                )
            }
            dao.insertShiftCreditNotes(cnEntities)
        }

        // Audit Trail Event
        dao.insertShiftEvent(
            ShiftEventEntity(
                id = UUID.randomUUID().toString(),
                shiftId = currShift.id,
                eventType = "CLOSE_SHIFT",
                actorUid = currShift.cashierUid,
                actorName = actor,
                detailsJson = """{"expected":$expectedCash,"counted":$countedCashInCents,"variance":$variance,"status":"$finalReviewStatus"}"""
            )
        )

        return ZReport(
            shiftId = currShift.id,
            cashierName = currShift.cashierName,
            openedAt = currShift.openedAt,
            closedAt = closedAt,
            openingFloatInCents = currShift.openingFloatInCents,
            totalSalesCount = salesCount,
            totalSalesAmountInCents = salesTotal,
            cashSalesInCents = cashSales,
            cardSalesInCents = cardSales,
            qrSalesInCents = qrSales,
            creditSalesInCents = creditSales,
            payInInCents = payIns,
            payOutInCents = payOuts,
            expectedCashInCents = expectedCash,
            countedCashInCents = countedCashInCents,
            cashVarianceInCents = variance,
            expectedOnlineInCents = expectedOnline,
            countedOnlineInCents = declaredOnlineTotal,
            onlineVarianceInCents = onlineVariance,
            onlineBillsCount = totalOnlineSlips,
            safeDropInCents = safeDrops,
            creditCollectionInCents = creditCollections,
            closingFloatInCents = closingFloatInCents,
            depositAmountInCents = depositAmountInCents,
            openingGapInCents = currShift.openingGapInCents,
            varianceReason = varianceReason,
            status = finalReviewStatus
        )
    }

    suspend fun getZReportForShift(shiftId: String): ZReport? {
        val currShift = dao.getShiftById(shiftId) ?: return null
        val metrics = getLiveShiftMetrics(currShift.id) ?: return null
        val declarations = dao.getShiftDeclarationsSync(currShift.id)
        val declaredOnlineTotal = declarations.sumOf { it.declaredAmountInCents }
        val totalOnlineSlips = declarations.sumOf { it.slipCount }
        val expectedOnline = metrics.cardSalesInCents + metrics.qrSalesInCents
        val onlineVariance = declaredOnlineTotal - expectedOnline
        val expectedCash = currShift.expectedCashInCents ?: metrics.expectedCashInCents
        val countedCash = currShift.countedCashInCents ?: 0L
        val variance = currShift.varianceInCents ?: (countedCash - expectedCash)

        return ZReport(
            shiftId = currShift.id,
            cashierName = currShift.cashierName,
            openedAt = currShift.openedAt,
            closedAt = currShift.closedAt ?: System.currentTimeMillis(),
            openingFloatInCents = currShift.openingFloatInCents,
            totalSalesCount = metrics.salesCount,
            totalSalesAmountInCents = metrics.salesTotalInCents,
            cashSalesInCents = metrics.cashSalesInCents,
            cardSalesInCents = metrics.cardSalesInCents,
            qrSalesInCents = metrics.qrSalesInCents,
            creditSalesInCents = metrics.creditSalesInCents,
            payInInCents = metrics.payInInCents,
            payOutInCents = metrics.payOutInCents,
            expectedCashInCents = expectedCash,
            countedCashInCents = countedCash,
            cashVarianceInCents = variance,
            expectedOnlineInCents = expectedOnline,
            countedOnlineInCents = declaredOnlineTotal,
            onlineVarianceInCents = onlineVariance,
            onlineBillsCount = totalOnlineSlips,
            safeDropInCents = metrics.safeDropInCents,
            creditCollectionInCents = metrics.creditCollectionInCents,
            closingFloatInCents = currShift.closingFloatInCents,
            depositAmountInCents = currShift.depositAmountInCents,
            openingGapInCents = currShift.openingGapInCents,
            varianceReason = currShift.varianceReason,
            status = currShift.reviewStatus.ifBlank { currShift.status }
        )
    }

    // Admin & Supervisor Operations
    suspend fun adminForceCloseShift(
        shiftId: String,
        adminUid: String,
        adminName: String,
        reason: String
    ): Boolean {
        val currShift = dao.getShiftById(shiftId) ?: return false
        val now = System.currentTimeMillis()
        val updatedShift = currShift.copy(
            status = "FORCE_CLOSED",
            closedAt = now,
            closedBy = "$adminName (Admin Force Close)",
            reviewStatus = "FLAGGED",
            notes = "${currShift.notes} | Force Closed: $reason".trim(' ', '|')
        )
        dao.updateShift(updatedShift)
        dao.updateRegisterCurrentShift(currShift.registerId, null)

        dao.insertShiftEvent(
            ShiftEventEntity(
                id = UUID.randomUUID().toString(),
                shiftId = shiftId,
                eventType = "FORCE_CLOSE",
                actorUid = adminUid,
                actorName = adminName,
                detailsJson = """{"reason":"$reason"}"""
            )
        )
        return true
    }

    suspend fun adminReopenShift(
        shiftId: String,
        adminUid: String,
        adminName: String,
        reason: String
    ): Boolean {
        val currShift = dao.getShiftById(shiftId) ?: return false

        // Take snapshot in shift_versions
        val snapshotJson = JSONObject().apply {
            put("countedCash", currShift.countedCashInCents)
            put("expectedCash", currShift.expectedCashInCents)
            put("variance", currShift.varianceInCents)
            put("closingFloat", currShift.closingFloatInCents)
            put("closedAt", currShift.closedAt)
            put("status", currShift.status)
        }.toString()

        val versionEntry = ShiftVersionEntity(
            id = UUID.randomUUID().toString(),
            shiftId = shiftId,
            version = currShift.version,
            snapshotJson = snapshotJson,
            changedBy = adminUid,
            reason = reason,
            createdAt = System.currentTimeMillis()
        )
        dao.insertShiftVersion(versionEntry)

        val updatedShift = currShift.copy(
            status = "OPEN",
            closedAt = null,
            reviewStatus = "PENDING",
            version = currShift.version + 1,
            notes = "${currShift.notes} | Reopened (v${currShift.version + 1}): $reason".trim(' ', '|')
        )
        dao.updateShift(updatedShift)
        dao.updateRegisterCurrentShift(currShift.registerId, shiftId)

        dao.insertShiftEvent(
            ShiftEventEntity(
                id = UUID.randomUUID().toString(),
                shiftId = shiftId,
                eventType = "REOPEN_SHIFT",
                actorUid = adminUid,
                actorName = adminName,
                detailsJson = """{"newVersion":${updatedShift.version},"reason":"$reason"}"""
            )
        )
        return true
    }

    suspend fun supervisorAdjustment(
        shiftId: String,
        supervisorUid: String,
        supervisorName: String,
        amountInCents: Long,
        reason: String
    ): Boolean {
        val currShift = dao.getShiftById(shiftId) ?: return false
        val adj = ShiftAdjustmentEntity(
            id = UUID.randomUUID().toString(),
            shiftId = shiftId,
            target = "CLOSING",
            supervisorUid = supervisorUid,
            amountInCents = amountInCents,
            beforeJson = """{"variance":${currShift.varianceInCents ?: 0L}}""",
            afterJson = """{"variance":${(currShift.varianceInCents ?: 0L) + amountInCents}}""",
            reason = reason,
            approvedBy = supervisorName,
            createdAt = System.currentTimeMillis()
        )
        dao.insertShiftAdjustment(adj)

        val newVariance = (currShift.varianceInCents ?: 0L) + amountInCents
        val updatedShift = currShift.copy(
            varianceInCents = newVariance,
            notes = "${currShift.notes} | Adj: +$amountInCents ($reason)".trim(' ', '|')
        )
        dao.updateShift(updatedShift)

        dao.insertShiftEvent(
            ShiftEventEntity(
                id = UUID.randomUUID().toString(),
                shiftId = shiftId,
                orgId = currShift.orgId,
                action = "SUPERVISOR_ADJUSTMENT",
                eventType = "SUPERVISOR_ADJUSTMENT",
                actorId = supervisorUid,
                actorUid = supervisorUid,
                actorName = supervisorName,
                payloadJson = """{"amount":$amountInCents,"reason":"$reason","newVariance":$newVariance}""",
                detailsJson = """{"amount":$amountInCents,"reason":"$reason","newVariance":$newVariance}"""
            )
        )
        return true
    }

    suspend fun adminReviewShift(
        shiftId: String,
        adminUid: String,
        adminName: String,
        reviewStatus: String, // "APPROVED" or "FLAGGED"
        reviewNote: String
    ): Boolean {
        val currShift = dao.getShiftById(shiftId) ?: return false
        val updatedShift = currShift.copy(
            reviewStatus = reviewStatus,
            reviewedBy = adminName,
            reviewedAt = System.currentTimeMillis(),
            reviewNote = reviewNote
        )
        dao.updateShift(updatedShift)

        dao.insertShiftEvent(
            ShiftEventEntity(
                id = UUID.randomUUID().toString(),
                shiftId = shiftId,
                orgId = currShift.orgId,
                action = "REVIEW_SHIFT",
                eventType = "REVIEW_SHIFT",
                actorId = adminUid,
                actorUid = adminUid,
                actorName = adminName,
                payloadJson = """{"reviewStatus":"$reviewStatus","note":"$reviewNote"}""",
                detailsJson = """{"reviewStatus":"$reviewStatus","note":"$reviewNote"}"""
            )
        )
        return true
    }

    suspend fun getShiftDetails(shiftId: String): ShiftDetails? {
        val shift = dao.getShiftById(shiftId) ?: return null
        val cashCounts = dao.getShiftCashCountsSync(shiftId, "CLOSING").ifEmpty {
            dao.getShiftCashCountsSync(shiftId, "OPENING")
        }
        val declarations = dao.getShiftDeclarationsSync(shiftId)
        val creditNotes = dao.getShiftCreditNotesSync(shiftId)
        val adjustments = dao.getShiftAdjustmentsSync(shiftId)
        val events = dao.getShiftEventsSync(shiftId)
        val metrics = getLiveShiftMetrics(shiftId)

        return ShiftDetails(
            shift = shift,
            cashCounts = cashCounts,
            declarations = declarations,
            creditNotes = creditNotes,
            adjustments = adjustments,
            events = events,
            metrics = metrics
        )
    }

    // Held Orders (Cart parking)
    suspend fun parkCart(orgId: String, registerId: String, customerName: String, cartItems: List<CartItem>, totalInCents: Long) {
        val cartArray = JSONArray()
        for (item in cartItems) {
            val obj = JSONObject().apply {
                put("productId", item.product.id)
                put("quantity", item.quantity)
                put("discountPercent", item.discountPercent)
            }
            cartArray.put(obj)
        }
        val held = HeldOrderEntity(
            id = UUID.randomUUID().toString(),
            orgId = orgId,
            registerId = registerId,
            customerName = customerName.ifBlank { "Guest" },
            cartJson = cartArray.toString(),
            totalInCents = totalInCents,
            heldAt = System.currentTimeMillis()
        )
        dao.insertHeldOrder(held)
    }

    suspend fun deleteHeldOrder(id: String) {
        dao.deleteHeldOrderById(id)
    }

    suspend fun getSaleDetails(saleId: String): Pair<List<SaleItemEntity>, List<SalePaymentEntity>> {
        val items = dao.getSaleItems(saleId)
        val payments = dao.getSalePayments(saleId)
        return Pair(items, payments)
    }

    suspend fun hasUsers(): Boolean = dao.getUserCount() > 0

    suspend fun findUserByEmail(email: String): UserEntity? = dao.getUserByEmailAcrossOrgs(email.trim().lowercase())

    suspend fun authenticateUser(identifier: String, pinOrPass: String): UserEntity? {
        val clean = identifier.trim()
        val user = dao.findUserByIdentifier(clean)
            ?: dao.getUserByEmailAcrossOrgs(clean.lowercase())
            ?: dao.getUserByEmailAcrossOrgs(clean)
        if (user != null) {
            val cleanSecret = pinOrPass.trim()
            if (user.pinHash == cleanSecret) {
                return user
            }
        }
        return null
    }

    suspend fun getBranchesSync(orgId: String): List<BranchEntity> = dao.getBranchesSync(orgId)
    suspend fun getRegistersSync(orgId: String): List<RegisterEntity> = dao.getRegistersSync(orgId)

    suspend fun registerNewStoreAndUser(
        storeName: String,
        taxId: String,
        currencySymbol: String,
        taxMode: TaxMode,
        userName: String,
        email: String,
        role: StaffRole,
        pin: String,
        ownerUid: String? = null
    ): StoreRegistrationResult {
        val orgId = "org_${UUID.randomUUID().toString().take(8)}"
        val actualUid = if (!ownerUid.isNullOrBlank()) ownerUid else "usr_${UUID.randomUUID().toString().take(8)}"
        val org = OrganizationEntity(
            id = orgId,
            orgId = orgId,
            name = storeName,
            taxId = if (taxId.isNotBlank()) taxId else "VAT-${System.currentTimeMillis().toString().takeLast(6)}",
            currencySymbol = currencySymbol,
            taxMode = taxMode,
            ownerUid = actualUid
        )
        dao.insertOrg(org)

        val branch = BranchEntity(
            id = "br_${UUID.randomUUID().toString().take(8)}",
            orgId = org.id,
            name = "$storeName Main Branch",
            code = "BR1",
            address = "Store Location 1",
            phone = "+1 555-0100"
        )
        dao.insertBranch(branch)

        val register = RegisterEntity(
            id = "reg_${UUID.randomUUID().toString().take(8)}",
            orgId = org.id,
            branchId = branch.id,
            name = "Front Register 1",
            code = "REG1"
        )
        dao.insertRegister(register)

        val cleanPin = pin.trim()
        val user = UserEntity(
            id = actualUid,
            orgId = org.id,
            fullName = userName,
            email = email.trim().lowercase(),
            role = role,
            pinHash = cleanPin,
            isActive = true
        )
        dao.insertUser(user)

        val staff = StaffMemberEntity(
            id = "staff_${UUID.randomUUID().toString().take(8)}",
            orgId = org.id,
            uid = actualUid,
            name = userName,
            email = email.trim().lowercase(),
            role = role,
            pinHash = cleanPin
        )
        dao.insertStaff(staff)

        val settings = StoreSettingsEntity(orgId = org.id)
        dao.insertStoreSettings(settings)

        addAuditLog(
            orgId = org.id,
            action = "STORE_INITIALIZED",
            details = "Store '$storeName' registered by $userName ($role)",
            actorUid = actualUid,
            actorName = userName,
            actorRole = role
        )

        return StoreRegistrationResult(org, user, branch, register)
    }

    // Store Settings
    fun getStoreSettings(orgId: String): Flow<StoreSettingsEntity?> = dao.getStoreSettings(orgId)

    suspend fun getStoreSettingsSync(orgId: String): StoreSettingsEntity {
        var settings = dao.getStoreSettingsSync(orgId)
        if (settings == null) {
            settings = StoreSettingsEntity(orgId = orgId)
            dao.insertStoreSettings(settings)
        }
        return settings
    }

    suspend fun saveStoreSettings(settings: StoreSettingsEntity) {
        dao.insertStoreSettings(settings)
    }

    // Audit Logs
    fun getAuditLogs(orgId: String): Flow<List<AuditLogEntity>> = dao.getAuditLogs(orgId)

    suspend fun addAuditLog(
        orgId: String,
        action: String,
        details: String,
        actorUid: String,
        actorName: String,
        actorRole: StaffRole
    ) {
        val log = AuditLogEntity(
            id = "aud_${UUID.randomUUID().toString().take(8)}",
            orgId = orgId,
            actorUid = actorUid,
            actorName = actorName.ifBlank { "System" },
            actorRole = actorRole,
            action = action,
            details = details
        )
        dao.insertAuditLog(log)
    }

    // Database Backup & Restore Engine
    suspend fun generateBackupJson(orgId: String): String {
        val org = dao.getOrgById(orgId) ?: dao.getActiveOrganizationSync()
            ?: throw IllegalStateException("Active store organization not found")
        val settings = dao.getStoreSettingsSync(orgId)
        val categories = dao.getAllCategoriesSync(orgId)
        val products = dao.getAllProductsSync(orgId)
        val barcodes = dao.getAllItemBarcodesSync(orgId)
        val customers = dao.getAllCustomersSync(orgId)
        val ledger = dao.getAllCustomerLedgerSync(orgId)
        val sales = dao.getAllSalesSync(orgId)
        val saleItems = dao.getAllSaleItemsSync(orgId)
        val payments = dao.getAllSalePaymentsSync(orgId)
        val stockMoves = dao.getAllStockMovementsSync(orgId)
        val shifts = dao.getAllShiftsSync(orgId)
        val cashMoves = dao.getAllCashMovementsSync(orgId)

        return com.example.pos.domain.DataBackupManager.createBackupJson(
            org = org,
            storeSettings = settings,
            categories = categories,
            products = products,
            itemBarcodes = barcodes,
            customers = customers,
            customerLedger = ledger,
            sales = sales,
            saleItems = saleItems,
            salePayments = payments,
            stockMovements = stockMoves,
            shifts = shifts,
            cashMovements = cashMoves
        )
    }

    suspend fun restoreBackupData(
        parsed: com.example.pos.domain.ParsedBackup,
        targetOrgId: String
    ): com.example.pos.domain.BackupCounts {
        if (parsed.categories.isNotEmpty()) dao.insertCategories(parsed.categories)
        if (parsed.products.isNotEmpty()) dao.insertProducts(parsed.products)
        if (parsed.itemBarcodes.isNotEmpty()) dao.insertItemBarcodes(parsed.itemBarcodes)
        if (parsed.customers.isNotEmpty()) dao.insertCustomers(parsed.customers)
        if (parsed.customerLedger.isNotEmpty()) dao.insertCustomerLedgers(parsed.customerLedger)
        if (parsed.sales.isNotEmpty()) dao.insertSales(parsed.sales)
        if (parsed.saleItems.isNotEmpty()) dao.insertSaleItems(parsed.saleItems)
        if (parsed.salePayments.isNotEmpty()) dao.insertSalePayments(parsed.salePayments)
        if (parsed.stockMovements.isNotEmpty()) dao.insertStockMovements(parsed.stockMovements)
        if (parsed.shifts.isNotEmpty()) dao.insertShifts(parsed.shifts)
        if (parsed.cashMovements.isNotEmpty()) dao.insertCashMovements(parsed.cashMovements)
        parsed.storeSettings?.let { ss ->
            dao.insertStoreSettings(ss.copy(orgId = targetOrgId))
        }

        // Record outbox sync entries for restored items
        dao.insertSyncOutbox(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                orgId = targetOrgId,
                tableName = "database_restore",
                recordId = "restore_${System.currentTimeMillis()}",
                payloadJson = JSONObject().apply {
                    put("restoredAt", System.currentTimeMillis())
                    put("totalRecords", parsed.counts.totalRecords)
                    put("products", parsed.counts.productsCount)
                    put("sales", parsed.counts.salesCount)
                }.toString(),
                operation = OutboxOp.INSERT
            )
        )

        return parsed.counts
    }
}

data class StoreRegistrationResult(
    val org: OrganizationEntity,
    val user: UserEntity,
    val branch: BranchEntity,
    val register: RegisterEntity
)
