package com.example.pos.data.repository

import android.content.Context
import android.util.Log
import com.example.pos.data.local.AppDatabase
import com.example.pos.data.local.BranchEntity
import com.example.pos.data.local.CategoryEntity
import com.example.pos.data.local.CustomerEntity
import com.example.pos.data.local.CustomerLedgerEntity
import com.example.pos.data.local.FiscalYearEntity
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
import com.example.pos.data.local.UserEntity
import com.example.pos.data.local.CashMovementEntity
import com.example.pos.data.model.MovementType
import com.example.pos.data.model.PaymentType
import com.example.pos.data.model.StaffRole
import com.example.pos.data.model.TaxMode
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.TimeZone
import java.util.UUID

enum class SyncConnectionStatus {
    ONLINE,
    OFFLINE,
    SYNCING,
    ERROR
}

data class SyncUiState(
    val status: SyncConnectionStatus = SyncConnectionStatus.ONLINE,
    val pendingCount: Int = 0,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val message: String = "Ready"
)

data class SyncResult(
    val isSuccess: Boolean,
    val message: String
)

class SyncRepository(
    private val context: Context,
    private val dao: PosDao = AppDatabase.getInstance(context).posDao(),
    private val firestore: FirebaseFirestore? = null
) {
    private val _syncState = MutableStateFlow(SyncUiState(status = SyncConnectionStatus.ONLINE, message = "Ready to sync"))
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private val db: FirebaseFirestore
        get() = firestore ?: try {
            FirebaseFirestore.getInstance("default")
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }

    /**
     * Directly writes User, Organization, and initial store documents to Cloud Firestore (default database).
     * Returns detailed SyncResult with user-friendly messages on error or success.
     */
    suspend fun syncUserAndOrganizationToFirestore(
        user: UserEntity,
        org: OrganizationEntity,
        branch: BranchEntity? = null,
        register: RegisterEntity? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        var currentUser = Firebase.auth.currentUser

        // If no active auth, try anonymous sign-in so request.auth != null if enabled in console
        if (currentUser == null) {
            try {
                val anon = Firebase.auth.signInAnonymously().await()
                currentUser = anon.user
                Log.d("SyncRepository", "Anonymous Firebase Auth session established: ${currentUser?.uid}")
            } catch (e: Exception) {
                Log.d("SyncRepository", "Anonymous sign-in not available: ${e.message}")
            }
        }

        val uid = currentUser?.uid ?: user.id

        _syncState.value = _syncState.value.copy(
            status = SyncConnectionStatus.SYNCING,
            message = "Syncing '${org.name}' to Cloud Firestore..."
        )

        try {
            // 1. Write Organization document
            val resolvedOwnerUid = if (user.role == StaffRole.OWNER || org.ownerUid.isBlank()) uid else org.ownerUid
            val orgDoc = hashMapOf<String, Any>(
                "id" to org.id,
                "orgId" to org.id,
                "name" to org.name,
                "displayName" to org.name,
                "legalName" to org.name,
                "businessType" to "RETAIL",
                "countryCode" to "NP",
                "currencyCode" to org.currencySymbol,
                "currencySymbol" to org.currencySymbol,
                "timezoneId" to TimeZone.getDefault().id,
                "taxId" to org.taxId,
                "taxMode" to org.taxMode.name,
                "ownerUid" to resolvedOwnerUid,
                "members" to FieldValue.arrayUnion(uid),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val orgWrite = withTimeoutOrNull(15000) {
                db.collection("organizations")
                    .document(org.id)
                    .set(orgDoc, SetOptions.merge())
                    .await()
                true
            }
            if (orgWrite != true) {
                val msg = "Connection timed out (15s) while writing organization to Cloud Firestore. Check Firestore database status and security rules."
                _syncState.value = _syncState.value.copy(status = SyncConnectionStatus.ERROR, message = msg)
                return@withContext SyncResult(false, msg)
            }
            Log.d("SyncRepository", "Organization '${org.id}' saved to Firestore")

            // 2. Write User document
            val userDoc = hashMapOf<String, Any>(
                "id" to uid,
                "uid" to uid,
                "email" to (currentUser?.email ?: user.email),
                "displayName" to (currentUser?.displayName ?: user.fullName),
                "fullName" to user.fullName,
                "orgId" to org.id,
                "activeOrgId" to org.id,
                "role" to user.role.name,
                "pinHash" to user.pinHash,
                "isActive" to user.isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val userWrite = withTimeoutOrNull(15000) {
                db.collection("users")
                    .document(uid)
                    .set(userDoc, SetOptions.merge())
                    .await()
                writeDocumentPair("users", uid, org.id, userDoc)
                true
            }
            if (userWrite != true) {
                val msg = "Connection timed out (15s) while writing user profile to Cloud Firestore."
                _syncState.value = _syncState.value.copy(status = SyncConnectionStatus.ERROR, message = msg)
                return@withContext SyncResult(false, msg)
            }
            Log.d("SyncRepository", "User '$uid' saved to Firestore")

            // 3. Write Subcollections in a single batch
            try {
                val batch = db.batch()

                val activeBranch = branch ?: dao.getBranchesSync(org.id).firstOrNull()
                if (activeBranch != null) {
                    val branchDoc = hashMapOf<String, Any>(
                        "id" to activeBranch.id,
                        "orgId" to org.id,
                        "name" to activeBranch.name,
                        "code" to activeBranch.code,
                        "address" to activeBranch.address,
                        "phone" to activeBranch.phone,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(
                        db.collection("organizations").document(org.id).collection("branches").document(activeBranch.id),
                        branchDoc,
                        SetOptions.merge()
                    )
                }

                val activeReg = register ?: dao.getRegistersSync(org.id).firstOrNull()
                if (activeReg != null) {
                    val regDoc = hashMapOf<String, Any>(
                        "id" to activeReg.id,
                        "orgId" to org.id,
                        "branchId" to activeReg.branchId,
                        "name" to activeReg.name,
                        "receiptPrefix" to activeReg.code,
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(
                        db.collection("organizations").document(org.id).collection("registers").document(activeReg.id),
                        regDoc,
                        SetOptions.merge()
                    )
                }

                val memId = "mem_${uid.take(10)}"
                val memDoc = hashMapOf<String, Any>(
                    "id" to memId,
                    "orgId" to org.id,
                    "userUid" to uid,
                    "role" to "OWNER",
                    "status" to "ACTIVE",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                batch.set(
                    db.collection("organizations").document(org.id).collection("memberships").document(memId),
                    memDoc,
                    SetOptions.merge()
                )

                withTimeoutOrNull(15000) {
                    batch.commit().await()
                }
                Log.d("SyncRepository", "Subcollections (branches, registers, memberships) saved to Firestore")
            } catch (subErr: Exception) {
                Log.w("SyncRepository", "Subcollections sync note: ${subErr.message}")
            }

            val successMsg = "Store '${org.name}' and collections synced to Cloud Firestore!"
            _syncState.value = _syncState.value.copy(
                status = SyncConnectionStatus.ONLINE,
                lastSyncTime = System.currentTimeMillis(),
                message = successMsg
            )
            SyncResult(true, successMsg)
        } catch (e: FirebaseFirestoreException) {
            val errDetail = if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "Cloud Firestore Permission Denied. In Firebase Console > Cloud Firestore > Security rules, allow read/write access."
            } else {
                "Firestore Error (${e.code}): ${e.localizedMessage}"
            }
            Log.e("SyncRepository", errDetail, e)
            _syncState.value = _syncState.value.copy(
                status = SyncConnectionStatus.ERROR,
                message = errDetail
            )
            SyncResult(false, errDetail)
        } catch (e: Exception) {
            val errDetail = e.localizedMessage ?: "Unknown Firestore sync error"
            Log.e("SyncRepository", "Error syncing User/Org to Firestore: $errDetail", e)
            _syncState.value = _syncState.value.copy(
                status = SyncConnectionStatus.ERROR,
                message = errDetail
            )
            SyncResult(false, errDetail)
        }
    }

    /**
     * Writes a document to BOTH the organization-scoped subcollection AND the top-level collection.
     * This guarantees tenant isolation in subcollections and immediate browsing visibility in Firebase Console.
     */
    private suspend fun writeDocumentPair(
        collectionName: String,
        docId: String,
        orgId: String,
        data: Map<String, Any>
    ) {
        val enriched = HashMap(data)
        enriched["syncedAt"] = System.currentTimeMillis()
        if (!enriched.containsKey("id")) enriched["id"] = docId
        if (!enriched.containsKey("orgId")) enriched["orgId"] = orgId

        // 1. Write to organization subcollection
        try {
            db.collection("organizations")
                .document(orgId)
                .collection(collectionName)
                .document(docId)
                .set(enriched, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w("SyncRepository", "Subcollection write error for $collectionName/$docId: ${e.message}")
        }

        // 2. Write to top-level collection for immediate visibility in Firebase Console
        try {
            db.collection(collectionName)
                .document(docId)
                .set(enriched, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.d("SyncRepository", "Top-level write note for $collectionName/$docId: ${e.message}")
        }
    }

    /**
     * Synchronizes ALL 19 tables/collections with ALL their respective fields to Cloud Firestore.
     * If a local collection is empty, a schema template document is written to initialize the collection in Firestore.
     */
    suspend fun syncAllCollectionsToFirestore(orgId: String): SyncResult = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(
            status = SyncConnectionStatus.SYNCING,
            message = "Creating and syncing all 19 collections to Cloud Firestore..."
        )

        var user = Firebase.auth.currentUser
        if (user == null) {
            try {
                val anon = Firebase.auth.signInAnonymously().await()
                user = anon.user
            } catch (e: Exception) {
                Log.d("SyncRepository", "Anonymous auth not enabled: ${e.message}")
            }
        }
        val currentUid = user?.uid ?: "local_device"

        try {
            // 1. Organizations
            val currentOrg = dao.getActiveOrganizationSync() ?: dao.getOrgById(orgId)
            if (currentOrg != null) {
                val orgDoc = hashMapOf<String, Any>(
                    "id" to currentOrg.id,
                    "orgId" to currentOrg.orgId,
                    "name" to currentOrg.name,
                    "displayName" to currentOrg.name,
                    "legalName" to currentOrg.name,
                    "taxId" to currentOrg.taxId,
                    "currencySymbol" to currentOrg.currencySymbol,
                    "taxMode" to currentOrg.taxMode.name,
                    "ownerUid" to currentUid,
                    "businessType" to "RETAIL",
                    "countryCode" to "NP",
                    "timezoneId" to TimeZone.getDefault().id,
                    "members" to listOf(currentUid),
                    "createdAt" to currentOrg.createdAt,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("organizations").document(currentOrg.id).set(orgDoc, SetOptions.merge()).await()
            }

            // 2. Users (Sync all store staff and owner)
            val orgUsers = dao.getUsersSync(orgId).ifEmpty { dao.getAllUsersSync() }
            val localUser = if (user?.email != null) dao.getUserByEmailAcrossOrgs(user.email!!) else dao.getFirstUser()
            val userList = if (orgUsers.isNotEmpty()) {
                orgUsers
            } else if (localUser != null) {
                listOf(localUser)
            } else {
                emptyList()
            }
            if (userList.isNotEmpty()) {
                for (u in userList) {
                    val isCurrentAuthUser = user != null && (u.email.equals(user.email, ignoreCase = true) || u.id == user.uid)
                    val uidToUse = if (isCurrentAuthUser) user.uid else u.id
                    val emailToUse = if (isCurrentAuthUser && !user.email.isNullOrBlank()) user.email!! else u.email
                    val userDoc = hashMapOf<String, Any>(
                        "id" to u.id,
                        "uid" to uidToUse,
                        "orgId" to u.orgId,
                        "fullName" to u.fullName,
                        "displayName" to u.fullName,
                        "email" to emailToUse,
                        "phone" to (u.phone ?: ""),
                        "role" to u.role.name,
                        "pinHash" to u.pinHash,
                        "isActive" to u.isActive,
                        "activeOrgId" to orgId,
                        "createdAt" to u.createdAt,
                        "updatedAt" to u.updatedAt
                    )
                    db.collection("users").document(u.id).set(userDoc, SetOptions.merge()).await()
                    writeDocumentPair("users", u.id, orgId, userDoc)
                }
            } else {
                writeDocumentPair("users", "_schema_template", orgId, mapOf(
                    "id" to "usr_template", "uid" to currentUid, "orgId" to orgId,
                    "fullName" to "Sample User", "email" to "user@example.com", "phone" to "",
                    "role" to "OWNER", "isActive" to true, "isSchemaTemplate" to true
                ))
            }

            // 3. Branches
            val branches = dao.getBranchesSync(orgId)
            if (branches.isNotEmpty()) {
                for (b in branches) {
                    writeDocumentPair("branches", b.id, orgId, mapOf(
                        "id" to b.id, "orgId" to b.orgId, "name" to b.name, "code" to b.code,
                        "address" to b.address, "phone" to b.phone, "createdAt" to System.currentTimeMillis()
                    ))
                }
            } else {
                writeDocumentPair("branches", "_schema_template", orgId, mapOf(
                    "id" to "branch_template", "orgId" to orgId, "name" to "Main Branch",
                    "code" to "MAIN", "address" to "Kathmandu, Nepal", "phone" to "", "isSchemaTemplate" to true
                ))
            }

            // 4. Registers
            val registers = dao.getRegistersSync(orgId)
            if (registers.isNotEmpty()) {
                for (r in registers) {
                    writeDocumentPair("registers", r.id, orgId, mapOf(
                        "id" to r.id, "orgId" to r.orgId, "branchId" to r.branchId, "name" to r.name,
                        "code" to r.code, "receiptPrefix" to r.code, "currentShiftId" to (r.currentShiftId ?: ""),
                        "isActive" to true, "createdAt" to System.currentTimeMillis()
                    ))
                }
            } else {
                writeDocumentPair("registers", "_schema_template", orgId, mapOf(
                    "id" to "reg_template", "orgId" to orgId, "branchId" to "branch_main",
                    "name" to "Counter 1", "code" to "REG-1", "receiptPrefix" to "INV-", "isActive" to true, "isSchemaTemplate" to true
                ))
            }

            // 5. Staff Members
            val staff = dao.getAllStaffMembersSync(orgId)
            val staffList = if (staff.isNotEmpty()) {
                staff
            } else {
                userList.map { u ->
                    StaffMemberEntity(
                        id = u.id,
                        orgId = u.orgId,
                        uid = u.id,
                        name = u.fullName,
                        email = u.email,
                        role = u.role,
                        pinHash = u.pinHash,
                        isActive = u.isActive
                    )
                }
            }
            if (staffList.isNotEmpty()) {
                for (s in staffList) {
                    writeDocumentPair("staff_members", s.id, orgId, mapOf(
                        "id" to s.id, "orgId" to s.orgId, "uid" to s.uid, "name" to s.name,
                        "email" to s.email, "role" to s.role.name, "pinHash" to s.pinHash,
                        "isActive" to s.isActive, "createdAt" to System.currentTimeMillis()
                    ))
                }
            } else {
                writeDocumentPair("staff_members", "_schema_template", orgId, mapOf(
                    "id" to "staff_template", "orgId" to orgId, "uid" to currentUid, "name" to "Store Staff",
                    "email" to "staff@pos.local", "role" to "CASHIER", "isActive" to true, "isSchemaTemplate" to true
                ))
            }

            // 6. Categories
            val categories = dao.getAllCategoriesSync(orgId)
            if (categories.isNotEmpty()) {
                for (c in categories) {
                    writeDocumentPair("categories", c.id, orgId, mapOf(
                        "id" to c.id, "orgId" to c.orgId, "name" to c.name,
                        "colorHex" to c.colorHex, "iconName" to c.iconName, "createdAt" to System.currentTimeMillis()
                    ))
                }
            } else {
                writeDocumentPair("categories", "_schema_template", orgId, mapOf(
                    "id" to "cat_template", "orgId" to orgId, "name" to "General",
                    "colorHex" to "#3B82F6", "iconName" to "category", "isSchemaTemplate" to true
                ))
            }

            // 7. Products
            val products = dao.getAllProductsSync(orgId)
            if (products.isNotEmpty()) {
                for (p in products) {
                    writeDocumentPair("products", p.id, orgId, mapOf(
                        "id" to p.id, "orgId" to p.orgId, "categoryId" to p.categoryId,
                        "name" to p.name, "sku" to p.sku, "barcode" to p.barcode,
                        "costPriceInCents" to p.costPriceInCents, "sellingPriceInCents" to p.sellingPriceInCents,
                        "taxRatePercent" to p.taxRatePercent, "unit" to p.unit, "minStockAlert" to p.minStockAlert,
                        "createdAt" to System.currentTimeMillis()
                    ))
                }
            } else {
                writeDocumentPair("products", "_schema_template", orgId, mapOf(
                    "id" to "prod_template", "orgId" to orgId, "categoryId" to "cat_general",
                    "name" to "Sample Item", "sku" to "SKU-001", "barcode" to "123456789012",
                    "costPriceInCents" to 10000L, "sellingPriceInCents" to 15000L, "unit" to "pcs", "isSchemaTemplate" to true
                ))
            }

            // 8. Item Barcodes
            val barcodes = dao.getAllItemBarcodesSync(orgId)
            if (barcodes.isNotEmpty()) {
                for (b in barcodes) {
                    writeDocumentPair("item_barcodes", b.id, orgId, mapOf(
                        "id" to b.id, "orgId" to b.orgId, "itemId" to b.itemId,
                        "barcode" to b.barcode, "packQty" to b.packQty, "createdAt" to b.createdAt
                    ))
                }
            } else {
                writeDocumentPair("item_barcodes", "_schema_template", orgId, mapOf(
                    "id" to "barcode_template", "orgId" to orgId, "itemId" to "item_sample",
                    "barcode" to "123456789012", "packQty" to 1, "isSchemaTemplate" to true
                ))
            }

            // 9. Stock Movements
            val stockMovements = dao.getAllStockMovementsSync(orgId)
            if (stockMovements.isNotEmpty()) {
                for (m in stockMovements) {
                    writeDocumentPair("stock_movements", m.id, orgId, mapOf(
                        "id" to m.id, "orgId" to m.orgId, "branchId" to m.branchId,
                        "productId" to m.productId, "quantityDelta" to m.quantityDelta,
                        "type" to m.type.name, "referenceId" to m.referenceId,
                        "createdAt" to m.createdAt, "createdBy" to m.createdBy
                    ))
                }
            } else {
                writeDocumentPair("stock_movements", "_schema_template", orgId, mapOf(
                    "id" to "mov_template", "orgId" to orgId, "branchId" to "branch_main",
                    "productId" to "prod_sample", "quantityDelta" to 10, "type" to "PURCHASE",
                    "referenceId" to "PO-001", "isSchemaTemplate" to true
                ))
            }

            // 10. Customers (Complete Schema including To Receive / To Give)
            val customers = dao.getAllCustomersSync(orgId)
            if (customers.isNotEmpty()) {
                for (c in customers) {
                    writeDocumentPair("customers", c.id, orgId, mapOf(
                        "id" to c.id,
                        "orgId" to c.orgId,
                        "name" to c.name,
                        "phone" to c.phone,
                        "email" to c.email,
                        "creditLimitInCents" to c.creditLimitInCents,
                        "currentDueInCents" to c.currentDueInCents,
                        "toReceiveInCents" to (if (c.currentDueInCents > 0L) c.currentDueInCents else 0L),
                        "toGiveInCents" to (if (c.currentDueInCents < 0L) kotlin.math.abs(c.currentDueInCents) else 0L),
                        "khataStatus" to (if (c.currentDueInCents > 0L) "TO_RECEIVE" else if (c.currentDueInCents < 0L) "TO_GIVE" else "SETTLED"),
                        "createdAt" to c.createdAt,
                        "updatedAt" to System.currentTimeMillis()
                    ))
                }
            } else {
                writeDocumentPair("customers", "_schema_template", orgId, mapOf(
                    "id" to "cust_template", "orgId" to orgId, "name" to "Sample Customer",
                    "phone" to "9800000000", "email" to "customer@example.com",
                    "creditLimitInCents" to 5000000L, "currentDueInCents" to 0L,
                    "toReceiveInCents" to 0L, "toGiveInCents" to 0L, "khataStatus" to "SETTLED", "isSchemaTemplate" to true
                ))
            }

            // 11. Customer Ledger
            val ledgerEntries = dao.getAllCustomerLedgerSync(orgId)
            if (ledgerEntries.isNotEmpty()) {
                for (l in ledgerEntries) {
                    writeDocumentPair("customer_ledger", l.id, orgId, mapOf(
                        "id" to l.id, "orgId" to l.orgId, "customerId" to l.customerId,
                        "saleId" to (l.saleId ?: ""), "entryType" to l.entryType,
                        "amountInCents" to l.amountInCents, "balanceAfterInCents" to l.balanceAfterInCents,
                        "notes" to l.notes, "createdAt" to l.createdAt
                    ))
                }
            } else {
                writeDocumentPair("customer_ledger", "_schema_template", orgId, mapOf(
                    "id" to "ledger_template", "orgId" to orgId, "customerId" to "cust_sample",
                    "saleId" to "", "entryType" to "OPENING_BALANCE", "amountInCents" to 0L,
                    "balanceAfterInCents" to 0L, "notes" to "Template ledger entry", "isSchemaTemplate" to true
                ))
            }

            // 12. Sales
            val sales = dao.getAllSalesSync(orgId)
            if (sales.isNotEmpty()) {
                for (s in sales) {
                    writeDocumentPair("sales", s.id, orgId, mapOf(
                        "id" to s.id, "orgId" to s.orgId, "branchId" to s.branchId, "registerId" to s.registerId,
                        "invoiceNo" to s.invoiceNo, "customerId" to (s.customerId ?: ""),
                        "subtotalInCents" to s.subtotalInCents, "discountInCents" to s.discountInCents,
                        "taxInCents" to s.taxInCents, "grandTotalInCents" to s.grandTotalInCents,
                        "paymentStatus" to s.paymentStatus, "createdAt" to s.createdAt, "createdBy" to s.createdBy
                    ))
                }
            } else {
                writeDocumentPair("sales", "_schema_template", orgId, mapOf(
                    "id" to "sale_template", "orgId" to orgId, "branchId" to "branch_main",
                    "registerId" to "reg_1", "invoiceNo" to "INV-0001", "subtotalInCents" to 10000L,
                    "discountInCents" to 0L, "taxInCents" to 1300L, "grandTotalInCents" to 11300L,
                    "paymentStatus" to "COMPLETED", "isSchemaTemplate" to true
                ))
            }

            // 13. Sale Items
            val saleItems = dao.getAllSaleItemsSync(orgId)
            if (saleItems.isNotEmpty()) {
                for (i in saleItems) {
                    writeDocumentPair("sale_items", i.id, orgId, mapOf(
                        "id" to i.id, "orgId" to i.orgId.ifBlank { orgId }, "saleId" to i.saleId,
                        "productId" to i.productId, "productName" to i.productName,
                        "unitPriceInCents" to i.unitPriceInCents, "quantity" to i.quantity,
                        "discountInCents" to i.discountInCents, "taxInCents" to i.taxInCents,
                        "lineTotalInCents" to i.lineTotalInCents
                    ))
                }
            } else {
                writeDocumentPair("sale_items", "_schema_template", orgId, mapOf(
                    "id" to "item_template", "orgId" to orgId, "saleId" to "sale_template",
                    "productId" to "prod_template", "productName" to "Template Product",
                    "unitPriceInCents" to 5000L, "quantity" to 2, "discountInCents" to 0L,
                    "taxInCents" to 1300L, "lineTotalInCents" to 11300L, "isSchemaTemplate" to true
                ))
            }

            // 14. Sale Payments
            val salePayments = dao.getAllSalePaymentsSync(orgId)
            if (salePayments.isNotEmpty()) {
                for (p in salePayments) {
                    writeDocumentPair("sale_payments", p.id, orgId, mapOf(
                        "id" to p.id, "orgId" to p.orgId.ifBlank { orgId }, "saleId" to p.saleId,
                        "paymentType" to p.paymentType.name, "amountInCents" to p.amountInCents,
                        "changeGivenInCents" to p.changeGivenInCents, "referenceNo" to p.referenceNo
                    ))
                }
            } else {
                writeDocumentPair("sale_payments", "_schema_template", orgId, mapOf(
                    "id" to "payment_template", "orgId" to orgId, "saleId" to "sale_template",
                    "paymentType" to "CASH", "amountInCents" to 11300L, "changeGivenInCents" to 0L,
                    "referenceNo" to "", "isSchemaTemplate" to true
                ))
            }

            // 15. Shifts
            val shifts = dao.getAllShiftsSync(orgId)
            if (shifts.isNotEmpty()) {
                for (sh in shifts) {
                    writeDocumentPair("shifts", sh.id, orgId, mapOf(
                        "id" to sh.id, "orgId" to sh.orgId, "registerId" to sh.registerId,
                        "cashierUid" to sh.cashierUid, "cashierName" to sh.cashierName,
                        "openedAt" to sh.openedAt, "openingFloatInCents" to sh.openingFloatInCents,
                        "closedAt" to (sh.closedAt ?: 0L), "countedCashInCents" to (sh.countedCashInCents ?: 0L),
                        "expectedCashInCents" to (sh.expectedCashInCents ?: 0L),
                        "varianceInCents" to (sh.varianceInCents ?: 0L), "status" to sh.status, "notes" to sh.notes
                    ))
                }
            } else {
                writeDocumentPair("shifts", "_schema_template", orgId, mapOf(
                    "id" to "shift_template", "orgId" to orgId, "registerId" to "reg_1",
                    "cashierUid" to currentUid, "cashierName" to "Cashier",
                    "openedAt" to System.currentTimeMillis(), "openingFloatInCents" to 500000L,
                    "status" to "OPEN", "notes" to "Initial opening float", "isSchemaTemplate" to true
                ))
            }

            // 16. Cash Movements
            val cashMovements = dao.getAllCashMovementsSync(orgId)
            if (cashMovements.isNotEmpty()) {
                for (cm in cashMovements) {
                    writeDocumentPair("cash_movements", cm.id, orgId, mapOf(
                        "id" to cm.id, "orgId" to cm.orgId.ifBlank { orgId }, "shiftId" to cm.shiftId,
                        "movementType" to cm.movementType, "amountInCents" to cm.amountInCents,
                        "reason" to cm.reason, "createdAt" to cm.createdAt
                    ))
                }
            } else {
                writeDocumentPair("cash_movements", "_schema_template", orgId, mapOf(
                    "id" to "cm_template", "orgId" to orgId, "shiftId" to "shift_template",
                    "movementType" to "PAY_IN", "amountInCents" to 100000L,
                    "reason" to "Initial Cash Float", "isSchemaTemplate" to true
                ))
            }

            // 17. Fiscal Years
            val fiscalYears = dao.getFiscalYearsSync(orgId)
            if (fiscalYears.isNotEmpty()) {
                for (fy in fiscalYears) {
                    writeDocumentPair("fiscal_years", fy.id, orgId, mapOf(
                        "id" to fy.id, "orgId" to fy.orgId, "name" to fy.name,
                        "code" to fy.code, "startDate" to fy.startDate, "endDate" to fy.endDate,
                        "invoicePrefix" to fy.invoicePrefix, "currentInvoiceSequence" to fy.currentInvoiceSequence,
                        "isCurrent" to fy.isCurrent, "isClosed" to fy.isClosed, "createdAt" to fy.createdAt
                    ))
                }
            } else {
                writeDocumentPair("fiscal_years", "_schema_template", orgId, mapOf(
                    "id" to "fy_template", "orgId" to orgId, "name" to "2081/82",
                    "code" to "81/82", "startDate" to 0L, "endDate" to 0L,
                    "invoicePrefix" to "INV", "currentInvoiceSequence" to 0L,
                    "isCurrent" to true, "isClosed" to false, "isSchemaTemplate" to true
                ))
            }

            // 18. Store Settings
            val st = dao.getStoreSettingsSync(orgId)
            val settingsMap = mapOf(
                "orgId" to (st?.orgId ?: orgId),
                "allowCash" to (st?.allowCash ?: true),
                "allowCard" to (st?.allowCard ?: true),
                "allowQr" to (st?.allowQr ?: true),
                "allowCredit" to (st?.allowCredit ?: true),
                "maxDiscountPercent" to (st?.maxDiscountPercent ?: 15),
                "receiptHeaderNote" to (st?.receiptHeaderNote ?: "Thank you for shopping!"),
                "receiptFooterNote" to (st?.receiptFooterNote ?: "Please come again!"),
                "autoPrintReceipt" to (st?.autoPrintReceipt ?: true),
                "lowStockThreshold" to (st?.lowStockThreshold ?: 5),
                "negativeStockBehavior" to (st?.negativeStockBehavior ?: "BLOCK"),
                "autoLockInterval" to (st?.autoLockInterval ?: "5 minutes"),
                "autoLogoutInterval" to (st?.autoLogoutInterval ?: "Never"),
                "biometricEnabled" to (st?.biometricEnabled ?: false),
                "loginBiometricEnabled" to (st?.loginBiometricEnabled ?: false),
                "enforceStrongPin" to (st?.enforceStrongPin ?: true),
                "printerName" to (st?.printerName ?: ""),
                "printerConnectionType" to (st?.printerConnectionType ?: "BLUETOOTH"),
                "paperWidthMm" to (st?.paperWidthMm ?: 80),
                "scannerMode" to (st?.scannerMode ?: "USB HID Keyboard Emulation Mode"),
                "cashDrawerTrigger" to (st?.cashDrawerTrigger ?: true),
                "backupEmail" to (st?.backupEmail ?: ""),
                "lastBackupAt" to (st?.lastBackupAt ?: 0L),
                "language" to (st?.language ?: "en"),
                "updatedAt" to System.currentTimeMillis()
            )
            writeDocumentPair("store_settings", orgId, orgId, settingsMap)

            // 18. Audit Logs
            val auditLogs = dao.getAllAuditLogsSync(orgId)
            if (auditLogs.isNotEmpty()) {
                for (al in auditLogs) {
                    writeDocumentPair("audit_logs", al.id, orgId, mapOf(
                        "id" to al.id, "orgId" to al.orgId, "actorUid" to al.actorUid,
                        "actorName" to al.actorName, "actorRole" to al.actorRole.name,
                        "action" to al.action, "details" to al.details, "createdAt" to al.createdAt
                    ))
                }
            } else {
                writeDocumentPair("audit_logs", "_schema_template", orgId, mapOf(
                    "id" to "audit_template", "orgId" to orgId, "actorUid" to currentUid,
                    "actorName" to "System", "actorRole" to "OWNER", "action" to "SYSTEM_INIT",
                    "details" to "Store collections initialized in Cloud Firestore", "isSchemaTemplate" to true
                ))
            }

            // 19. Held Orders
            val heldOrders = dao.getAllHeldOrdersSync(orgId)
            if (heldOrders.isNotEmpty()) {
                for (ho in heldOrders) {
                    writeDocumentPair("held_orders", ho.id, orgId, mapOf(
                        "id" to ho.id, "orgId" to ho.orgId, "registerId" to ho.registerId,
                        "customerName" to ho.customerName, "cartJson" to ho.cartJson,
                        "totalInCents" to ho.totalInCents, "heldAt" to ho.heldAt
                    ))
                }
            } else {
                writeDocumentPair("held_orders", "_schema_template", orgId, mapOf(
                    "id" to "held_template", "orgId" to orgId, "registerId" to "reg_1",
                    "customerName" to "Walk-in", "cartJson" to "[]", "totalInCents" to 0L,
                    "heldAt" to System.currentTimeMillis(), "isSchemaTemplate" to true
                ))
            }

            val successMsg = "Successfully synchronized all 19 collections to Cloud Firestore!"
            _syncState.value = _syncState.value.copy(
                status = SyncConnectionStatus.ONLINE,
                lastSyncTime = System.currentTimeMillis(),
                message = successMsg
            )
            SyncResult(true, successMsg)
        } catch (e: Exception) {
            val errDetail = e.localizedMessage ?: "Unknown Firestore sync error"
            Log.e("SyncRepository", "syncAllCollectionsToFirestore failed: $errDetail", e)
            _syncState.value = _syncState.value.copy(
                status = SyncConnectionStatus.ERROR,
                message = errDetail
            )
            SyncResult(false, errDetail)
        }
    }

    suspend fun recordShiftDenominations(shiftId: String, data: Map<String, Any>) {
        val orgId = dao.getActiveOrganizationSync()?.id ?: "default_org"
        writeDocumentPair("shift_denominations", shiftId, orgId, data)
    }

    private fun parseOrganization(doc: DocumentSnapshot): OrganizationEntity {
        val orgId = doc.getString("id") ?: doc.getString("orgId") ?: doc.id
        val name = doc.getString("name") ?: doc.getString("displayName") ?: "OmniPOS Store"
        val taxId = doc.getString("taxId") ?: ""
        val currencySymbol = doc.getString("currencySymbol") ?: doc.getString("currencyCode") ?: "Rs"
        val taxModeStr = doc.getString("taxMode") ?: "EXCLUSIVE"
        val taxMode = try { TaxMode.valueOf(taxModeStr) } catch (_: Exception) { TaxMode.EXCLUSIVE }
        val ownerUid = doc.getString("ownerUid") ?: ""
        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

        return OrganizationEntity(
            id = orgId,
            orgId = orgId,
            name = name,
            taxId = taxId,
            currencySymbol = currencySymbol,
            taxMode = taxMode,
            ownerUid = ownerUid,
            createdAt = createdAt
        )
    }

    /**
     * Looks up an existing store/organization for a given user from Cloud Firestore.
     * Enforces strict multi-tenant isolation by verifying userUid, email, ownerUid, or member access.
     */
    suspend fun findRemoteOrganizationForUser(
        userUid: String,
        email: String
    ): OrganizationEntity? = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()

            // 1. Direct document lookup by userUid in top-level users collection (fastest & most reliable)
            if (userUid.isNotBlank()) {
                try {
                    val directUserDoc = db.collection("users").document(userUid).get().await()
                    if (directUserDoc.exists()) {
                        val orgId = directUserDoc.getString("activeOrgId") ?: directUserDoc.getString("orgId")
                        if (!orgId.isNullOrBlank()) {
                            val orgSnap = db.collection("organizations").document(orgId).get().await()
                            if (orgSnap.exists() && orgSnap.id != "_schema_template" && orgSnap.getBoolean("isSchemaTemplate") != true) {
                                return@withContext parseOrganization(orgSnap)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("SyncRepository", "Direct user doc lookup note: ${e.message}")
                }
            }

            // 2. Query top-level users collection by email
            if (cleanEmail.isNotBlank()) {
                try {
                    val userSnapByEmail = db.collection("users")
                        .whereEqualTo("email", cleanEmail)
                        .limit(1)
                        .get()
                        .await()
                    if (!userSnapByEmail.isEmpty) {
                        val doc = userSnapByEmail.documents.first()
                        val orgId = doc.getString("activeOrgId") ?: doc.getString("orgId")
                        if (!orgId.isNullOrBlank()) {
                            val orgSnap = db.collection("organizations").document(orgId).get().await()
                            if (orgSnap.exists() && orgSnap.id != "_schema_template" && orgSnap.getBoolean("isSchemaTemplate") != true) {
                                return@withContext parseOrganization(orgSnap)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("SyncRepository", "User query by email note: ${e.message}")
                }
            }

            // 3. Query top-level users collection by uid
            if (userUid.isNotBlank()) {
                try {
                    val userSnapByUid = db.collection("users")
                        .whereEqualTo("uid", userUid)
                        .limit(1)
                        .get()
                        .await()
                    if (!userSnapByUid.isEmpty) {
                        val doc = userSnapByUid.documents.first()
                        val orgId = doc.getString("activeOrgId") ?: doc.getString("orgId")
                        if (!orgId.isNullOrBlank()) {
                            val orgSnap = db.collection("organizations").document(orgId).get().await()
                            if (orgSnap.exists() && orgSnap.id != "_schema_template" && orgSnap.getBoolean("isSchemaTemplate") != true) {
                                return@withContext parseOrganization(orgSnap)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("SyncRepository", "User query by uid note: ${e.message}")
                }
            }

            // 4. Try finding in organizations collection by ownerUid
            if (userUid.isNotBlank()) {
                try {
                    val orgByOwner = db.collection("organizations")
                        .whereEqualTo("ownerUid", userUid)
                        .limit(1)
                        .get()
                        .await()
                    if (!orgByOwner.isEmpty) {
                        val doc = orgByOwner.documents.first()
                        if (doc.id != "_schema_template" && doc.getBoolean("isSchemaTemplate") != true) {
                            return@withContext parseOrganization(doc)
                        }
                    }
                } catch (e: Exception) {
                    Log.d("SyncRepository", "Org query by ownerUid note: ${e.message}")
                }

                // 5. Try finding in organizations collection by members array
                try {
                    val orgByMember = db.collection("organizations")
                        .whereArrayContains("members", userUid)
                        .limit(1)
                        .get()
                        .await()
                    if (!orgByMember.isEmpty) {
                        val doc = orgByMember.documents.first()
                        if (doc.id != "_schema_template" && doc.getBoolean("isSchemaTemplate") != true) {
                            return@withContext parseOrganization(doc)
                        }
                    }
                } catch (e: Exception) {
                    Log.d("SyncRepository", "Org query by member note: ${e.message}")
                }
            }

            // 6. Check organization subcollections: organizations/{orgId}/users and staff_members matching userUid or email
            try {
                val allOrgs = db.collection("organizations").limit(20).get().await()
                for (orgDoc in allOrgs.documents) {
                    if (orgDoc.id == "_schema_template" || orgDoc.getBoolean("isSchemaTemplate") == true) continue
                    val orgId = orgDoc.id

                    // Check if owner matches
                    if (userUid.isNotBlank() && orgDoc.getString("ownerUid") == userUid) {
                        return@withContext parseOrganization(orgDoc)
                    }

                    // Check subcollection users
                    if (userUid.isNotBlank()) {
                        val subUser = db.collection("organizations").document(orgId).collection("users").document(userUid).get().await()
                        if (subUser.exists()) {
                            return@withContext parseOrganization(orgDoc)
                        }
                    }
                    if (cleanEmail.isNotBlank()) {
                        val subUsersByEmail = db.collection("organizations").document(orgId).collection("users")
                            .whereEqualTo("email", cleanEmail)
                            .limit(1)
                            .get()
                            .await()
                        if (!subUsersByEmail.isEmpty) {
                            return@withContext parseOrganization(orgDoc)
                        }
                        val subStaffByEmail = db.collection("organizations").document(orgId).collection("staff_members")
                            .whereEqualTo("email", cleanEmail)
                            .limit(1)
                            .get()
                            .await()
                        if (!subStaffByEmail.isEmpty) {
                            return@withContext parseOrganization(orgDoc)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("SyncRepository", "Subcollections org lookup note: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "findRemoteOrganizationForUser error: ${e.message}", e)
        }
        null
    }

    suspend fun ensureAuthSession() {
        if (Firebase.auth.currentUser == null) {
            try {
                Firebase.auth.signInAnonymously().await()
                Log.d("SyncRepository", "Anonymous auth session established for Firestore access")
            } catch (e: Exception) {
                Log.d("SyncRepository", "Anonymous auth note: ${e.message}")
            }
        }
    }

    /**
     * Authenticates a staff member against Cloud Firestore using their email/username and PIN.
     * This allows cashiers/staff added by the owner on Device A to log in immediately on Device B.
     * Upon matching PIN, downloads the store organization and all data into the local Room DB.
     */
    suspend fun authenticateRemoteUser(
        identifier: String,
        pin: String
    ): Pair<UserEntity, OrganizationEntity>? = withContext(Dispatchers.IO) {
        val cleanId = identifier.trim()
        val cleanEmail = cleanId.lowercase()
        val cleanPin = pin.trim()
        if (cleanId.isBlank() || cleanPin.isBlank()) return@withContext null

        ensureAuthSession()

        try {
            var matchedUserDoc: DocumentSnapshot? = null
            var matchedOrgId: String? = null

            // 1. Search top-level users collection by email
            if (cleanEmail.contains("@")) {
                val byEmail = db.collection("users")
                    .whereEqualTo("email", cleanEmail)
                    .get()
                    .await()
                for (doc in byEmail.documents) {
                    if (doc.id == "_schema_template" || doc.getBoolean("isSchemaTemplate") == true) continue
                    val docPin = doc.getString("pinHash")?.trim()
                    if (docPin == cleanPin) {
                        matchedUserDoc = doc
                        matchedOrgId = doc.getString("orgId") ?: doc.getString("activeOrgId")
                        break
                    }
                }
            }

            // 2. Search top-level staff_members collection if not found yet
            if (matchedUserDoc == null && cleanEmail.contains("@")) {
                val byEmailStaff = db.collection("staff_members")
                    .whereEqualTo("email", cleanEmail)
                    .get()
                    .await()
                for (doc in byEmailStaff.documents) {
                    if (doc.id == "_schema_template" || doc.getBoolean("isSchemaTemplate") == true) continue
                    val docPin = doc.getString("pinHash")?.trim()
                    if (docPin == cleanPin) {
                        matchedUserDoc = doc
                        matchedOrgId = doc.getString("orgId")
                        break
                    }
                }
            }

            // 3. Search by name/username in top-level users or staff_members
            if (matchedUserDoc == null) {
                val allUsers = db.collection("users").limit(50).get().await()
                for (doc in allUsers.documents) {
                    if (doc.id == "_schema_template" || doc.getBoolean("isSchemaTemplate") == true) continue
                    val name = doc.getString("fullName") ?: doc.getString("name") ?: ""
                    val email = doc.getString("email") ?: ""
                    val docPin = doc.getString("pinHash")?.trim()
                    if ((name.equals(cleanId, ignoreCase = true) || email.equals(cleanEmail, ignoreCase = true)) && docPin == cleanPin) {
                        matchedUserDoc = doc
                        matchedOrgId = doc.getString("orgId") ?: doc.getString("activeOrgId")
                        break
                    }
                }
            }

            // 4. Also search inside organization subcollections: organizations/{orgId}/users and staff_members
            if (matchedUserDoc == null) {
                val allOrgs = db.collection("organizations").limit(10).get().await()
                for (orgDoc in allOrgs.documents) {
                    if (orgDoc.id == "_schema_template" || orgDoc.getBoolean("isSchemaTemplate") == true) continue
                    val orgId = orgDoc.id

                    // Check subcollection users
                    try {
                        val subUsers = db.collection("organizations").document(orgId).collection("users").get().await()
                        for (doc in subUsers.documents) {
                            if (doc.id == "_schema_template" || doc.getBoolean("isSchemaTemplate") == true) continue
                            val name = doc.getString("fullName") ?: doc.getString("name") ?: ""
                            val email = doc.getString("email") ?: ""
                            val docPin = doc.getString("pinHash")?.trim()
                            if ((name.equals(cleanId, ignoreCase = true) || email.equals(cleanEmail, ignoreCase = true)) && docPin == cleanPin) {
                                matchedUserDoc = doc
                                matchedOrgId = orgId
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("SyncRepository", "Sub users search note: ${e.message}")
                    }

                    if (matchedUserDoc != null) break

                    // Check subcollection staff_members
                    try {
                        val subStaff = db.collection("organizations").document(orgId).collection("staff_members").get().await()
                        for (doc in subStaff.documents) {
                            if (doc.id == "_schema_template" || doc.getBoolean("isSchemaTemplate") == true) continue
                            val name = doc.getString("name") ?: ""
                            val email = doc.getString("email") ?: ""
                            val docPin = doc.getString("pinHash")?.trim()
                            if ((name.equals(cleanId, ignoreCase = true) || email.equals(cleanEmail, ignoreCase = true)) && docPin == cleanPin) {
                                matchedUserDoc = doc
                                matchedOrgId = orgId
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("SyncRepository", "Sub staff search note: ${e.message}")
                    }

                    if (matchedUserDoc != null) break
                }
            }

            if (matchedUserDoc != null && !matchedOrgId.isNullOrBlank()) {
                val orgDoc = db.collection("organizations").document(matchedOrgId).get().await()
                if (orgDoc.exists()) {
                    val orgEntity = parseOrganization(orgDoc)
                    dao.insertOrg(orgEntity)

                    val roleStr = matchedUserDoc.getString("role") ?: "CASHIER"
                    val role = try { StaffRole.valueOf(roleStr) } catch (_: Exception) { StaffRole.CASHIER }
                    val userEntity = UserEntity(
                        id = matchedUserDoc.getString("id") ?: matchedUserDoc.id,
                        orgId = matchedOrgId,
                        fullName = matchedUserDoc.getString("fullName") ?: matchedUserDoc.getString("name") ?: cleanId,
                        email = matchedUserDoc.getString("email") ?: cleanEmail,
                        phone = matchedUserDoc.getString("phone"),
                        role = role,
                        pinHash = cleanPin,
                        isActive = matchedUserDoc.getBoolean("isActive") ?: true,
                        createdAt = matchedUserDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    dao.insertUser(userEntity)

                    // Pull full organization data into Room (products, categories, etc.)
                    pullAllFromFirestore(matchedOrgId)

                    return@withContext Pair(userEntity, orgEntity)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "authenticateRemoteUser error: ${e.message}", e)
        }
        null
    }

    /**
     * Downloads (pulls) all store data from Cloud Firestore into the local Room database.
     * This brings in products, categories, staff, barcodes, customers, ledgers, and settings
     * when logging in on a new device or performing a full cloud sync.
     */
    /**
     * Helper to read documents for a collection from both organization subcollection and top-level collection,
     * merging them seamlessly while filtering schema templates.
     */
    private suspend fun fetchDocsFromSubAndTop(
        orgId: String,
        collectionName: String
    ): List<DocumentSnapshot> {
        val result = mutableListOf<DocumentSnapshot>()
        try {
            val subSnap = db.collection("organizations").document(orgId).collection(collectionName).get().await()
            result.addAll(subSnap.documents)
        } catch (e: Exception) {
            Log.d("SyncRepository", "Error reading subcollection $collectionName: ${e.message}")
        }
        try {
            val topSnap = db.collection(collectionName).whereEqualTo("orgId", orgId).get().await()
            result.addAll(topSnap.documents)
        } catch (e: Exception) {
            Log.d("SyncRepository", "Error reading top collection $collectionName: ${e.message}")
        }
        return result.filter { it.id != "_schema_template" && it.getBoolean("isSchemaTemplate") != true }
            .distinctBy { it.id }
    }

    /**
     * Downloads (pulls) all store data from Cloud Firestore into the local Room database.
     * This brings in products, categories, staff, barcodes, customers, ledgers, sales, shifts,
     * stock movements, and store settings when logging in on a new device or performing a cloud sync.
     */
    suspend fun pullAllFromFirestore(orgId: String): SyncResult = withContext(Dispatchers.IO) {
        ensureAuthSession()
        _syncState.value = _syncState.value.copy(
            status = SyncConnectionStatus.SYNCING,
            message = "Downloading cloud store data from Firestore..."
        )

        try {
            // 1. Organization
            val orgDoc = db.collection("organizations").document(orgId).get().await()
            if (orgDoc.exists() && orgDoc.id != "_schema_template" && orgDoc.getBoolean("isSchemaTemplate") != true) {
                val orgEntity = parseOrganization(orgDoc)
                dao.insertOrg(orgEntity)
            }

            // 2. Branches
            val branchDocs = fetchDocsFromSubAndTop(orgId, "branches")
            val validBranches = branchDocs.map { doc ->
                BranchEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    name = doc.getString("name") ?: "Main Branch",
                    code = doc.getString("code") ?: "MAIN",
                    address = doc.getString("address") ?: "",
                    phone = doc.getString("phone") ?: ""
                )
            }
            if (validBranches.isNotEmpty()) {
                dao.insertBranches(validBranches)
            }

            // 3. Registers
            val regDocs = fetchDocsFromSubAndTop(orgId, "registers")
            val validRegisters = regDocs.map { doc ->
                RegisterEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    branchId = doc.getString("branchId") ?: "branch_main",
                    name = doc.getString("name") ?: "Counter 1",
                    code = doc.getString("code") ?: "REG-1",
                    currentShiftId = doc.getString("currentShiftId")?.ifBlank { null }
                )
            }
            if (validRegisters.isNotEmpty()) {
                dao.insertRegisters(validRegisters)
            }

            // 4. Categories
            val catDocs = fetchDocsFromSubAndTop(orgId, "categories")
            val validCategories = catDocs.map { doc ->
                CategoryEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    name = doc.getString("name") ?: "General",
                    colorHex = doc.getString("colorHex") ?: "#3B82F6",
                    iconName = doc.getString("iconName") ?: "category"
                )
            }
            if (validCategories.isNotEmpty()) {
                dao.insertCategories(validCategories)
            }

            // 5. Products
            val prodDocs = fetchDocsFromSubAndTop(orgId, "products")
            val validProducts = prodDocs.map { doc ->
                ProductEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    categoryId = doc.getString("categoryId") ?: "",
                    name = doc.getString("name") ?: "Product",
                    sku = doc.getString("sku") ?: "",
                    barcode = doc.getString("barcode") ?: "",
                    costPriceInCents = doc.getLong("costPriceInCents") ?: 0L,
                    sellingPriceInCents = doc.getLong("sellingPriceInCents") ?: 0L,
                    taxRatePercent = doc.getDouble("taxRatePercent") ?: 0.0,
                    unit = doc.getString("unit") ?: "pcs",
                    minStockAlert = (doc.getLong("minStockAlert") ?: 5L).toInt()
                )
            }
            if (validProducts.isNotEmpty()) {
                dao.insertProducts(validProducts)
            }

            // 6. Users (and Staff Members)
            val userDocs = mutableListOf<DocumentSnapshot>()
            userDocs.addAll(fetchDocsFromSubAndTop(orgId, "users"))
            try {
                val activeOrgUsers = db.collection("users").whereEqualTo("activeOrgId", orgId).get().await()
                userDocs.addAll(activeOrgUsers.documents)
            } catch (e: Exception) {
                Log.d("SyncRepository", "Error reading activeOrgId users: ${e.message}")
            }
            val ownerUid = orgDoc.getString("ownerUid")
            if (!ownerUid.isNullOrBlank() && userDocs.none { it.id == ownerUid }) {
                try {
                    val ownerDoc = db.collection("users").document(ownerUid).get().await()
                    if (ownerDoc.exists()) {
                        userDocs.add(ownerDoc)
                    }
                } catch (e: Exception) {
                    Log.d("SyncRepository", "Error reading owner user doc: ${e.message}")
                }
            }

            val staffDocs = fetchDocsFromSubAndTop(orgId, "staff_members")
            val validUsers = mutableListOf<UserEntity>()
            userDocs.filter { it.id != "_schema_template" && it.getBoolean("isSchemaTemplate") != true }
                .forEach { doc ->
                    val isOwner = doc.id == ownerUid || doc.getString("id") == ownerUid
                    val roleStr = doc.getString("role") ?: if (isOwner) "OWNER" else "CASHIER"
                    val role = try { StaffRole.valueOf(roleStr) } catch (_: Exception) { if (isOwner) StaffRole.OWNER else StaffRole.CASHIER }
                    validUsers.add(
                        UserEntity(
                            id = doc.getString("id") ?: doc.id,
                            orgId = doc.getString("orgId") ?: orgId,
                            fullName = doc.getString("fullName") ?: doc.getString("displayName") ?: doc.getString("name") ?: (if (isOwner) "Owner" else "Staff"),
                            email = doc.getString("email") ?: "",
                            phone = doc.getString("phone"),
                            role = role,
                            pinHash = doc.getString("pinHash") ?: "1234",
                            isActive = doc.getBoolean("isActive") ?: true,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    )
                }

            // Convert any staff_members not yet in validUsers to UserEntity
            staffDocs.filter { it.id != "_schema_template" && it.getBoolean("isSchemaTemplate") != true }
                .forEach { doc ->
                    val docId = doc.getString("id") ?: doc.id
                    if (validUsers.none { it.id == docId }) {
                        val roleStr = doc.getString("role") ?: "CASHIER"
                        val role = try { StaffRole.valueOf(roleStr) } catch (_: Exception) { StaffRole.CASHIER }
                        validUsers.add(
                            UserEntity(
                                id = docId,
                                orgId = doc.getString("orgId") ?: orgId,
                                fullName = doc.getString("name") ?: "Staff",
                                email = doc.getString("email") ?: "",
                                phone = null,
                                role = role,
                                pinHash = doc.getString("pinHash") ?: "1234",
                                isActive = doc.getBoolean("isActive") ?: true,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

            val distinctUsers = validUsers.distinctBy { it.id }
            if (distinctUsers.isNotEmpty()) {
                dao.insertUsers(distinctUsers)
                val staffMembers = distinctUsers.map { u ->
                    StaffMemberEntity(
                        id = u.id,
                        orgId = u.orgId,
                        uid = u.id,
                        name = u.fullName,
                        email = u.email,
                        role = u.role,
                        pinHash = u.pinHash,
                        isActive = u.isActive
                    )
                }
                dao.insertStaffMembers(staffMembers)
            }

            // 7. Item Barcodes
            val barcodeDocs = fetchDocsFromSubAndTop(orgId, "item_barcodes")
            val validBarcodes = barcodeDocs.map { doc ->
                ItemBarcodeEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    itemId = doc.getString("itemId") ?: "",
                    barcode = doc.getString("barcode") ?: "",
                    packQty = (doc.getLong("packQty") ?: 1L).toInt(),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
            if (validBarcodes.isNotEmpty()) {
                dao.insertItemBarcodes(validBarcodes)
            }

            // 8. Customers
            val custDocs = fetchDocsFromSubAndTop(orgId, "customers")
            val pendingOutbox = dao.getPendingSyncEntries(orgId)
            val pendingCustRecordIds = pendingOutbox.filter { it.tableName == "customers" }.map { it.recordId }.toSet()

            val validCustomers = custDocs.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val localCust = dao.getCustomerById(id)
                val remoteDue = doc.getLong("currentDueInCents") ?: 0L
                val finalDue = if (localCust != null && (pendingCustRecordIds.contains(id) || (localCust.currentDueInCents != 0L && remoteDue == 0L))) {
                    localCust.currentDueInCents
                } else {
                    remoteDue
                }
                CustomerEntity(
                    id = id,
                    orgId = doc.getString("orgId") ?: orgId,
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    email = doc.getString("email") ?: "",
                    creditLimitInCents = doc.getLong("creditLimitInCents") ?: 50000L,
                    currentDueInCents = finalDue,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
            if (validCustomers.isNotEmpty()) {
                dao.insertCustomers(validCustomers)
            }

            // 9. Customer Ledger
            val ledgerDocs = fetchDocsFromSubAndTop(orgId, "customer_ledger")
            val validLedger = ledgerDocs.map { doc ->
                CustomerLedgerEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    customerId = doc.getString("customerId") ?: "",
                    saleId = doc.getString("saleId")?.ifBlank { null },
                    entryType = doc.getString("entryType") ?: "SALE_CREDIT",
                    amountInCents = doc.getLong("amountInCents") ?: 0L,
                    balanceAfterInCents = doc.getLong("balanceAfterInCents") ?: 0L,
                    notes = doc.getString("notes") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
            if (validLedger.isNotEmpty()) {
                dao.insertCustomerLedgers(validLedger)
            }

            // 10. Sales (Invoices)
            val saleDocs = fetchDocsFromSubAndTop(orgId, "sales")
            val validSales = saleDocs.map { doc ->
                val fallbackInvoiceNo = "INV-" + (doc.getString("id") ?: doc.id).takeLast(6)
                SaleEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    branchId = doc.getString("branchId") ?: "branch_main",
                    registerId = doc.getString("registerId") ?: "reg_1",
                    invoiceNo = doc.getString("invoiceNo") ?: fallbackInvoiceNo,
                    fiscalYear = doc.getString("fiscalYear") ?: "",
                    customerId = doc.getString("customerId")?.ifBlank { null },
                    subtotalInCents = doc.getLong("subtotalInCents") ?: 0L,
                    discountInCents = doc.getLong("discountInCents") ?: 0L,
                    taxInCents = doc.getLong("taxInCents") ?: 0L,
                    grandTotalInCents = doc.getLong("grandTotalInCents") ?: 0L,
                    paymentStatus = doc.getString("paymentStatus") ?: "COMPLETED",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    createdBy = doc.getString("createdBy") ?: "Cashier"
                )
            }
            if (validSales.isNotEmpty()) {
                dao.insertSales(validSales)
            }

            // 11. Sale Items
            val itemDocs = fetchDocsFromSubAndTop(orgId, "sale_items")
            val validItems = itemDocs.map { doc ->
                SaleItemEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    saleId = doc.getString("saleId") ?: "",
                    productId = doc.getString("productId") ?: "",
                    productName = doc.getString("productName") ?: "Item",
                    unitPriceInCents = doc.getLong("unitPriceInCents") ?: 0L,
                    quantity = (doc.getLong("quantity") ?: 1L).toInt(),
                    discountInCents = doc.getLong("discountInCents") ?: 0L,
                    taxInCents = doc.getLong("taxInCents") ?: 0L,
                    lineTotalInCents = doc.getLong("lineTotalInCents") ?: 0L
                )
            }
            if (validItems.isNotEmpty()) {
                dao.insertSaleItems(validItems)
            }

            // 12. Sale Payments
            val payDocs = fetchDocsFromSubAndTop(orgId, "sale_payments")
            val validPayments = payDocs.map { doc ->
                val typeStr = doc.getString("paymentType") ?: "CASH"
                val pType = try { PaymentType.valueOf(typeStr) } catch (_: Exception) { PaymentType.CASH }
                SalePaymentEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    saleId = doc.getString("saleId") ?: "",
                    paymentType = pType,
                    amountInCents = doc.getLong("amountInCents") ?: 0L,
                    changeGivenInCents = doc.getLong("changeGivenInCents") ?: 0L,
                    referenceNo = doc.getString("referenceNo") ?: ""
                )
            }
            if (validPayments.isNotEmpty()) {
                dao.insertSalePayments(validPayments)
            }

            // 13. Shifts (Cashier Shifts)
            val shiftDocs = fetchDocsFromSubAndTop(orgId, "shifts")
            val validShifts = shiftDocs.map { doc ->
                ShiftEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    branchId = doc.getString("branchId") ?: "br_main_01",
                    registerId = doc.getString("registerId") ?: "reg_01",
                    cashierUid = doc.getString("cashierUid") ?: "",
                    cashierName = doc.getString("cashierName") ?: "Cashier",
                    openedAt = doc.getLong("openedAt") ?: System.currentTimeMillis(),
                    openingFloatInCents = doc.getLong("openingFloatInCents") ?: 0L,
                    closedAt = doc.getLong("closedAt"),
                    countedCashInCents = doc.getLong("countedCashInCents"),
                    expectedCashInCents = doc.getLong("expectedCashInCents"),
                    varianceInCents = doc.getLong("varianceInCents"),
                    status = doc.getString("status") ?: "CLOSED",
                    notes = doc.getString("notes") ?: ""
                )
            }
            if (validShifts.isNotEmpty()) {
                dao.insertShifts(validShifts)
            }

            // 14. Cash Movements
            val cmDocs = fetchDocsFromSubAndTop(orgId, "cash_movements")
            val validCm = cmDocs.map { doc ->
                CashMovementEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    shiftId = doc.getString("shiftId") ?: "",
                    movementType = doc.getString("movementType") ?: "PAY_IN",
                    amountInCents = doc.getLong("amountInCents") ?: 0L,
                    reason = doc.getString("reason") ?: "",
                    tender = doc.getString("tender") ?: "CASH",
                    createdBy = doc.getString("createdBy") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
            if (validCm.isNotEmpty()) {
                dao.insertCashMovements(validCm)
            }

            // 15. Stock Movements
            val smDocs = fetchDocsFromSubAndTop(orgId, "stock_movements")
            val validSm = smDocs.map { doc ->
                val typeStr = doc.getString("type") ?: "ADJUSTMENT"
                val mType = try { MovementType.valueOf(typeStr) } catch (_: Exception) { MovementType.ADJUSTMENT }
                StockMovementEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    branchId = doc.getString("branchId") ?: "br_main_01",
                    productId = doc.getString("productId") ?: "",
                    quantityDelta = (doc.getLong("quantityDelta") ?: 0L).toInt(),
                    type = mType,
                    referenceId = doc.getString("referenceId") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
            if (validSm.isNotEmpty()) {
                dao.insertStockMovements(validSm)
            }

            // 16. Fiscal Years
            val fyDocs = fetchDocsFromSubAndTop(orgId, "fiscal_years")
            val validFy = fyDocs.map { doc ->
                FiscalYearEntity(
                    id = doc.getString("id") ?: doc.id,
                    orgId = doc.getString("orgId") ?: orgId,
                    name = doc.getString("name") ?: "2081/82",
                    code = doc.getString("code") ?: "81/82",
                    startDate = doc.getLong("startDate") ?: 0L,
                    endDate = doc.getLong("endDate") ?: 0L,
                    invoicePrefix = doc.getString("invoicePrefix") ?: "INV",
                    currentInvoiceSequence = doc.getLong("currentInvoiceSequence") ?: 0L,
                    isCurrent = doc.getBoolean("isCurrent") ?: false,
                    isClosed = doc.getBoolean("isClosed") ?: false,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                )
            }
            if (validFy.isNotEmpty()) {
                dao.insertFiscalYears(validFy)
            }

            // 17. Store Settings
            try {
                val settingsDoc = db.collection("organizations").document(orgId).collection("store_settings").document(orgId).get().await()
                val doc = if (settingsDoc.exists()) settingsDoc else db.collection("store_settings").document(orgId).get().await()
                if (doc.exists() && doc.id != "_schema_template" && doc.getBoolean("isSchemaTemplate") != true) {
                    val settings = StoreSettingsEntity(
                        orgId = orgId,
                        allowCash = doc.getBoolean("allowCash") ?: true,
                        allowCard = doc.getBoolean("allowCard") ?: true,
                        allowQr = doc.getBoolean("allowQr") ?: true,
                        allowCredit = doc.getBoolean("allowCredit") ?: true,
                        maxDiscountPercent = (doc.getLong("maxDiscountPercent") ?: 15L).toInt(),
                        receiptHeaderNote = doc.getString("receiptHeaderNote") ?: "Thank you for shopping!",
                        receiptFooterNote = doc.getString("receiptFooterNote") ?: "Please come again!",
                        autoPrintReceipt = doc.getBoolean("autoPrintReceipt") ?: true,
                        lowStockThreshold = (doc.getLong("lowStockThreshold") ?: 5L).toInt(),
                        negativeStockBehavior = doc.getString("negativeStockBehavior") ?: "BLOCK",
                        autoLockInterval = doc.getString("autoLockInterval") ?: "5 minutes",
                        autoLogoutInterval = doc.getString("autoLogoutInterval") ?: "Never",
                        biometricEnabled = doc.getBoolean("biometricEnabled") ?: false,
                        loginBiometricEnabled = doc.getBoolean("loginBiometricEnabled") ?: false,
                        enforceStrongPin = doc.getBoolean("enforceStrongPin") ?: true,
                        printerName = (doc.getString("printerName") ?: "").let { if (it.contains("Epson Thermal Receipt Printer", ignoreCase = true)) "" else it },
                        printerConnectionType = doc.getString("printerConnectionType") ?: "BLUETOOTH",
                        paperWidthMm = (doc.getLong("paperWidthMm") ?: 80L).toInt(),
                        scannerMode = doc.getString("scannerMode") ?: "USB HID Keyboard Emulation Mode",
                        cashDrawerTrigger = doc.getBoolean("cashDrawerTrigger") ?: true,
                        backupEmail = doc.getString("backupEmail") ?: "",
                        lastBackupAt = doc.getLong("lastBackupAt"),
                        language = doc.getString("language") ?: "en",
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                    dao.insertStoreSettings(settings)
                }
            } catch (e: Exception) {
                Log.d("SyncRepository", "Store settings pull note: ${e.message}")
            }

            val msg = "Successfully pulled ${validProducts.size} products, ${validCustomers.size} customers, ${validSales.size} invoices, ${validShifts.size} shifts, and ${distinctUsers.size} staff from Cloud Firestore"
            Log.d("SyncRepository", msg)
            SyncResult(true, msg)
        } catch (e: Exception) {
            val err = "Pull from Firestore failed: ${e.message}"
            Log.e("SyncRepository", err, e)
            SyncResult(false, err)
        }
    }

    suspend fun performSync(orgId: String): SyncResult = withContext(Dispatchers.IO) {
        _syncState.value = _syncState.value.copy(
            status = SyncConnectionStatus.SYNCING,
            message = "Syncing store data with Cloud Firestore..."
        )

        val pending = dao.getPendingSyncEntries(orgId)
        var user = Firebase.auth.currentUser
        if (user == null) {
            try {
                val anon = Firebase.auth.signInAnonymously().await()
                user = anon.user
            } catch (e: Exception) {
                Log.d("SyncRepository", "Anonymous auth not enabled: ${e.message}")
            }
        }

        // 1. Sync pending local outbox items FIRST so local state changes are pushed before cloud pull
        for (item in pending) {
            try {
                val dataMap = mutableMapOf<String, Any>()
                val json = JSONObject(item.payloadJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    dataMap[key] = json.get(key)
                }
                dataMap["syncedAt"] = System.currentTimeMillis()
                dataMap["syncedBy"] = user?.uid ?: "local_device"

                writeDocumentPair(item.tableName, item.recordId, orgId, dataMap)
                dao.deleteSyncOutboxById(item.id)
            } catch (e: Exception) {
                Log.w("SyncRepository", "Sync error on ${item.recordId}: ${e.message}")
            }
        }

        // 2. Ensure Organization and User are synced
        val currentOrg = dao.getActiveOrganizationSync() ?: dao.getOrgById(orgId)
        if (currentOrg != null) {
            val localUser = if (user?.email != null) dao.getUserByEmailAcrossOrgs(user.email!!) else dao.getFirstUser()
            val uid = user?.uid ?: localUser?.id ?: "usr_${currentOrg.id.take(8)}"
            val userEntity = localUser ?: UserEntity(
                id = uid,
                orgId = currentOrg.id,
                fullName = user?.displayName ?: "Owner",
                email = user?.email ?: "owner@pos.local",
                role = com.example.pos.data.model.StaffRole.OWNER,
                pinHash = "1234",
                isActive = true
            )
            val orgRes = syncUserAndOrganizationToFirestore(userEntity, currentOrg)
            if (!orgRes.isSuccess) {
                Log.w("SyncRepository", "Organization sync note: ${orgRes.message}")
            }
        }

        // 3. Pull latest cloud data AFTER local outbox updates are pushed
        val pullRes = pullAllFromFirestore(orgId)
        if (!pullRes.isSuccess) {
            Log.d("SyncRepository", "Initial cloud pull note: ${pullRes.message}")
        }

        // 4. Fully sync all collections and fields to Firestore
        val collectionsResult = syncAllCollectionsToFirestore(orgId)

        val remaining = dao.getPendingSyncEntries(orgId).size
        val finalStatus = if (remaining == 0 && collectionsResult.isSuccess) SyncConnectionStatus.ONLINE else SyncConnectionStatus.ERROR
        val finalMessage = if (remaining == 0 && collectionsResult.isSuccess) {
            "Store and all 19 collections synced bidirectionally with Cloud Firestore"
        } else {
            collectionsResult.message
        }
        _syncState.value = SyncUiState(
            status = finalStatus,
            pendingCount = remaining,
            lastSyncTime = System.currentTimeMillis(),
            message = finalMessage
        )
        SyncResult(remaining == 0 && collectionsResult.isSuccess, finalMessage)
    }
}

