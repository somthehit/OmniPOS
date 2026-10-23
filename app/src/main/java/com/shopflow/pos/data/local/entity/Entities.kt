package com.shopflow.pos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * ShopFlow POS - local (offline) schema.
 *
 * Design rules (from the Blueprint):
 *  - One encrypted DB file per organization per user. organization_id is STILL on every row
 *    (defense in depth + sync scoping + composite FKs).
 *  - IDs are client-generated UUIDv7 strings so devices can create records offline.
 *  - Money = Long minor units. Quantity = Long thousandths (qty_milli). No floating point.
 *  - Timestamps = epoch milliseconds UTC.
 *  - Composite FKs (organization_id, id) make cross-organization references impossible.
 *  - Ledger tables (sales, sale_items, payments, stock_movements) are append-only (see triggers).
 *  - Stock is NOT stored; it is the SUM of stock_movements.
 */

object SyncState { const val PENDING = 0; const val SYNCED = 1; const val FAILED = 2; const val CONFLICT = 3 }
object SaleStatus { const val COMPLETED = "COMPLETED"; const val VOIDED = "VOIDED"; const val PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED"; const val REFUNDED = "REFUNDED" }
object PaymentMethod { const val CASH = "CASH"; const val CARD = "CARD"; const val QR = "QR"; const val WALLET = "WALLET"; const val CREDIT = "CREDIT" }
object CreditApprovalStatus { const val NONE = "NONE"; const val CONFIRMED = "CONFIRMED"; const val OVERRIDDEN = "OVERRIDDEN"; const val REJECTED = "REJECTED" }
object CreditHoldStatus { const val ACTIVE = "ACTIVE"; const val OVERDUE_HOLD = "OVERDUE_HOLD"; const val SUSPENDED = "SUSPENDED" }
object LedgerEntryType { const val CHARGE = "CHARGE"; const val PAYMENT = "PAYMENT"; const val REFUND_CREDIT = "REFUND_CREDIT"; const val ADJUSTMENT = "ADJUSTMENT" }
object ProviderStatus { const val CONFIRMED = "CONFIRMED"; const val PENDING = "PENDING"; const val FAILED = "FAILED" }
object MovementType {
    const val SALE = "SALE"; const val RECEIPT = "RECEIPT"; const val ADJUSTMENT = "ADJUSTMENT"
    const val REFUND_RESTOCK = "REFUND_RESTOCK"; const val TRANSFER_OUT = "TRANSFER_OUT"
    const val TRANSFER_IN = "TRANSFER_IN"; const val COUNT_CORRECTION = "COUNT_CORRECTION"; const val OPENING = "OPENING"
}
object OutboxStatus { const val PENDING = "PENDING"; const val IN_FLIGHT = "IN_FLIGHT"; const val FAILED = "FAILED"; const val DONE = "DONE" }

// ───────────────────────── Organizations ─────────────────────────

@Entity(tableName = "organizations")
data class OrganizationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "legal_name") val legalName: String?,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "business_type") val businessType: String,
    @ColumnInfo(name = "country_code") val countryCode: String,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "currency_minor_digits") val currencyMinorDigits: Int = 2,
    @ColumnInfo(name = "timezone_id") val timezoneId: String,
    @ColumnInfo(name = "tax_id") val taxId: String? = null,                 // PAN / VAT / TIN
    @ColumnInfo(name = "registration_number") val registrationNumber: String? = null,
    @ColumnInfo(name = "tax_mode") val taxMode: String = "EXCLUSIVE",      // INCLUSIVE | EXCLUSIVE
    @ColumnInfo(name = "rounding_rule") val roundingRule: String = "HALF_UP",
    @ColumnInfo(name = "invoice_prefix") val invoicePrefix: String? = null,
    @ColumnInfo(name = "status") val status: String = "ACTIVE",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "version") val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
)

@Entity(
    tableName = "branches",
    foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "code"], unique = true),
    ],
)
data class BranchEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    val code: String,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    @ColumnInfo(name = "receipt_header") val receiptHeader: String? = null,
    @ColumnInfo(name = "receipt_footer") val receiptFooter: String? = null,
    val status: String = "ACTIVE",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
)

@Entity(
    tableName = "registers",
    foreignKeys = [ForeignKey(entity = BranchEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "branch_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "branch_id"]),
        Index(value = ["organization_id", "receipt_prefix"], unique = true),
    ],
)
data class RegisterEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    val name: String,
    @ColumnInfo(name = "receipt_prefix") val receiptPrefix: String,        // e.g. BR1-REG2
    val status: String = "ACTIVE",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
)

/** Local-only. Per-register receipt sequence so offline devices never collide. Never synced. */
@Entity(
    tableName = "receipt_counters",
    primaryKeys = ["organization_id", "register_id"],
    foreignKeys = [ForeignKey(entity = RegisterEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "register_id"], onDelete = ForeignKey.RESTRICT)],
)
data class ReceiptCounterEntity(
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "register_id") val registerId: String,
    @ColumnInfo(name = "next_seq") val nextSeq: Long = 1,
)

// ───────────────────────── Users, memberships, permissions ─────────────────────────

/** Local projection of user_profiles for members of THIS organization only. */
@Entity(
    tableName = "users",
    foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["organization_id", "id"], unique = true), Index(value = ["organization_id", "email"])],
)
data class UserEntity(
    @PrimaryKey val id: String,                                             // auth provider user id
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    val email: String?,
    val phone: String? = null,
    @ColumnInfo(name = "staff_code") val staffCode: String? = null,
    @ColumnInfo(name = "preferred_language") val preferredLanguage: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.SYNCED,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
)

@Entity(
    tableName = "memberships",
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "user_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "user_id"], unique = true),
    ],
)
data class MembershipEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "role_key") val roleKey: String,                     // OWNER, ADMIN, MANAGER, CASHIER, STOREKEEPER, ACCOUNTANT, custom
    val status: String = "ACTIVE",                                          // ACTIVE | SUSPENDED | REVOKED
    @ColumnInfo(name = "max_discount_bps") val maxDiscountBps: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.SYNCED,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
)

@Entity(
    tableName = "membership_branches",
    primaryKeys = ["organization_id", "membership_id", "branch_id"],
    foreignKeys = [
        ForeignKey(entity = MembershipEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "membership_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = BranchEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "branch_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["organization_id", "branch_id"])],
)
data class MembershipBranchEntity(
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "membership_id") val membershipId: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
)

/** Cached role -> permission keys so permission checks work offline. Server remains the authority. */
@Entity(
    tableName = "role_permissions",
    primaryKeys = ["organization_id", "role_key", "permission_key"],
    foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT)],
)
data class RolePermissionEntity(
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "role_key") val roleKey: String,
    @ColumnInfo(name = "permission_key") val permissionKey: String,         // e.g. sales.create, stock.adjust
)

/** Local-only quick-unlock + offline-login state. Never synced. PIN is a salted slow hash, never plain. */
@Entity(
    tableName = "local_credentials",
    primaryKeys = ["organization_id", "user_id"],
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "user_id"], onDelete = ForeignKey.CASCADE)],
)
data class LocalCredentialEntity(
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "pin_hash") val pinHash: ByteArray?,
    @ColumnInfo(name = "pin_salt") val pinSalt: ByteArray?,
    @ColumnInfo(name = "failed_attempts") val failedAttempts: Int = 0,      // lock after 5
    @ColumnInfo(name = "locked_until") val lockedUntil: Long? = null,
    @ColumnInfo(name = "last_online_auth_at") val lastOnlineAuthAt: Long,
    @ColumnInfo(name = "offline_valid_until") val offlineValidUntil: Long,  // default last_online_auth_at + 7 days
)

// ───────────────────────── Catalog ─────────────────────────

@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["organization_id", "id"], unique = true), Index(value = ["organization_id", "name"])],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    val name: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "default_margin_bps") val defaultMarginBps: Int? = null,
)

/** Configurable. Rates are never hardcoded. 1300 bps = 13%. */
@Entity(
    tableName = "tax_rates",
    foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["organization_id", "id"], unique = true)],
)
data class TaxRateEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    val name: String,
    @ColumnInfo(name = "rate_bps") val rateBps: Int,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "category_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = TaxRateEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "tax_rate_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "sku"], unique = true),           // archived rows keep their SKU
        Index(value = ["organization_id", "category_id"]),
        Index(value = ["organization_id", "tax_rate_id"]),
        Index(value = ["organization_id", "name"]),
        Index(value = ["organization_id", "status"]),
    ],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    @ColumnInfo(name = "tax_rate_id") val taxRateId: String? = null,
    val name: String,
    val description: String? = null,
    val sku: String,
    val brand: String? = null,
    @ColumnInfo(name = "unit_code") val unitCode: String = "pcs",
    @ColumnInfo(name = "cost_minor") val costMinor: Long? = null,
    @ColumnInfo(name = "price_minor") val priceMinor: Long,
    @ColumnInfo(name = "wholesale_price_minor") val wholesalePriceMinor: Long? = null,
    @ColumnInfo(name = "track_stock") val trackStock: Boolean = true,
    @ColumnInfo(name = "reorder_level_milli") val reorderLevelMilli: Long = 0,
    @ColumnInfo(name = "image_path") val imagePath: String? = null,         // local cache path of private-bucket image
    val status: String = "ACTIVE",                                          // ACTIVE | ARCHIVED (never hard-deleted once sold)
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "default_margin_bps") val defaultMarginBps: Int? = null,
    @ColumnInfo(name = "supplier_id") val supplierId: String? = null,
    @ColumnInfo(name = "mrp_minor") val mrpMinor: Long? = null,
)

@Entity(
    tableName = "barcodes",
    foreignKeys = [ForeignKey(entity = ProductEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "product_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "code"], unique = true),          // fast scan lookup + duplicate detection
        Index(value = ["organization_id", "product_id"]),
    ],
)
data class BarcodeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    val code: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "pack_multiplier_milli", defaultValue = "1000") val packMultiplierMilli: Long = 1000L,
)

@Entity(
    tableName = "customers",
    foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["organization_id", "id"], unique = true), Index(value = ["organization_id", "phone"])],
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val code: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "customer_type", defaultValue = "'INDIVIDUAL'") val customerType: String = "INDIVIDUAL",
    @ColumnInfo(name = "status", defaultValue = "'ACTIVE'") val status: String = "ACTIVE",
    @ColumnInfo(name = "tax_id") val taxId: String? = null,
    @ColumnInfo(name = "address_line1") val addressLine1: String? = null,
    @ColumnInfo(name = "address_line2") val addressLine2: String? = null,
    val city: String? = null,
    @ColumnInfo(name = "postal_code") val postalCode: String? = null,
    @ColumnInfo(name = "state_province") val stateProvince: String? = null,
    @ColumnInfo(name = "country_code") val countryCode: String? = null,
    @ColumnInfo(name = "preferred_language", defaultValue = "'en'") val preferredLanguage: String = "en",
    @ColumnInfo(name = "default_discount_bps", defaultValue = "0") val defaultDiscountBps: Int = 0,
    @ColumnInfo(name = "marketing_consent", defaultValue = "0") val marketingConsent: Boolean = false,
    @ColumnInfo(name = "date_of_birth") val dateOfBirth: Long? = null,
    @ColumnInfo(name = "has_credit_enabled", defaultValue = "0") val hasCreditEnabled: Boolean = false,
)

// ───────────────────────── Stock ledger (append-only) ─────────────────────────

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(entity = BranchEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "branch_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "product_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "idempotency_key"], unique = true), // prevents duplicate posting
        Index(value = ["organization_id", "branch_id", "product_id"]),
        Index(value = ["organization_id", "branch_id"]),
        Index(value = ["organization_id", "product_id"]),
        Index(value = ["organization_id", "source_type", "source_id"]),
    ],
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "movement_type") val movementType: String,
    @ColumnInfo(name = "quantity_milli") val quantityMilli: Long,           // signed: sale = negative
    @ColumnInfo(name = "unit_cost_minor") val unitCostMinor: Long? = null,
    @ColumnInfo(name = "source_type") val sourceType: String,               // sale | goods_receipt | adjustment | transfer | count
    @ColumnInfo(name = "source_id") val sourceId: String,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    val reason: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "supplier_id") val supplierId: String? = null,
    @ColumnInfo(name = "invoice_ref") val invoiceRef: String? = null,
    @ColumnInfo(name = "batch_number") val batchNumber: String? = null,
    @ColumnInfo(name = "expiry_date") val expiryDate: Long? = null,
    @ColumnInfo(name = "received_date") val receivedDate: Long? = null,
)

// ───────────────────────── Sales (append-only) ─────────────────────────

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(entity = BranchEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "branch_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = RegisterEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "register_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "cashier_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "customer_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "receipt_number"], unique = true),
        Index(value = ["organization_id", "idempotency_key"], unique = true),
        Index(value = ["organization_id", "branch_id", "sold_at"]),
        Index(value = ["organization_id", "cashier_id", "sold_at"]),
        Index(value = ["organization_id", "register_id"]),
        Index(value = ["organization_id", "customer_id"]),
        Index(value = ["organization_id", "sync_state"]),
    ],
)
data class SaleEntity(
    @PrimaryKey val id: String,                                             // client UUID doubles as server idempotency key
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "register_id") val registerId: String,
    @ColumnInfo(name = "cash_session_id") val cashSessionId: String? = null, // FK added when cash_sessions table ships
    @ColumnInfo(name = "cashier_id") val cashierId: String,
    @ColumnInfo(name = "customer_id") val customerId: String? = null,
    @ColumnInfo(name = "receipt_number") val receiptNumber: String,          // allocated by SaleRepository, e.g. BR1-REG2-000123
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    val status: String = SaleStatus.COMPLETED,
    @ColumnInfo(name = "currency_code") val currencyCode: String,            // snapshot
    @ColumnInfo(name = "tax_mode") val taxMode: String,                      // snapshot
    @ColumnInfo(name = "subtotal_minor") val subtotalMinor: Long,
    @ColumnInfo(name = "discount_minor") val discountMinor: Long = 0,
    @ColumnInfo(name = "tax_minor") val taxMinor: Long = 0,
    @ColumnInfo(name = "rounding_minor") val roundingMinor: Long = 0,
    @ColumnInfo(name = "total_minor") val totalMinor: Long,
    @ColumnInfo(name = "paid_minor") val paidMinor: Long,
    @ColumnInfo(name = "change_minor") val changeMinor: Long = 0,
    @ColumnInfo(name = "sold_at") val soldAt: Long,
    @ColumnInfo(name = "timezone_id") val timezoneId: String,                // snapshot for correct report day boundaries
    val notes: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "sync_error") val syncError: String? = null,
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(entity = SaleEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "sale_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "product_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "sale_id"]),
        Index(value = ["organization_id", "product_id"]),
    ],
)
data class SaleItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "sale_id") val saleId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "product_name") val productName: String,            // snapshot
    val sku: String,                                                        // snapshot
    @ColumnInfo(name = "quantity_milli") val quantityMilli: Long,
    @ColumnInfo(name = "unit_price_minor") val unitPriceMinor: Long,        // snapshot
    @ColumnInfo(name = "unit_cost_minor") val unitCostMinor: Long? = null,  // snapshot (for margin)
    @ColumnInfo(name = "discount_minor") val discountMinor: Long = 0,
    @ColumnInfo(name = "tax_rate_bps") val taxRateBps: Int = 0,             // snapshot
    @ColumnInfo(name = "tax_minor") val taxMinor: Long = 0,
    @ColumnInfo(name = "line_total_minor") val lineTotalMinor: Long,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
)

@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(entity = SaleEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "sale_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "sale_id"]),
        Index(value = ["organization_id", "method"]),
    ],
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "sale_id") val saleId: String,
    val method: String,                                                     // CASH | CARD | QR | WALLET | CREDIT
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "tendered_minor") val tenderedMinor: Long? = null,
    @ColumnInfo(name = "change_minor") val changeMinor: Long = 0,
    val provider: String? = null,
    @ColumnInfo(name = "provider_reference") val providerReference: String? = null, // never card numbers / CVV / secrets
    @ColumnInfo(name = "provider_status") val providerStatus: String = ProviderStatus.CONFIRMED,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
    @ColumnInfo(name = "credit_approval_status", defaultValue = "'NONE'") val creditApprovalStatus: String = "NONE",
    @ColumnInfo(name = "manager_override_uid") val managerOverrideUid: String? = null,
)

// ───────────────────────── Customer Credit & Ledger (v2) ─────────────────────────

@Entity(
    tableName = "customer_credit_terms",
    foreignKeys = [
        ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "customer_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "customer_id"], unique = true),
    ],
)
data class CustomerCreditTermsEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "customer_id") val customerId: String,
    @ColumnInfo(name = "credit_limit_minor", defaultValue = "0") val creditLimitMinor: Long = 0L,
    @ColumnInfo(name = "payment_terms_days", defaultValue = "30") val paymentTermsDays: Int = 30,
    @ColumnInfo(name = "grace_period_days", defaultValue = "0") val gracePeriodDays: Int = 0,
    @ColumnInfo(name = "interest_rate_bps", defaultValue = "0") val interestRateBps: Int = 0,
    @ColumnInfo(name = "hold_status", defaultValue = "'ACTIVE'") val holdStatus: String = "ACTIVE",
    @ColumnInfo(name = "approved_by") val approvedBy: String? = null,
    @ColumnInfo(name = "approved_at") val approvedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "version", defaultValue = "1") val version: Long = 1,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state", defaultValue = "0") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
)

@Entity(
    tableName = "customer_ledger",
    foreignKeys = [
        ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "customer_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "idempotency_key"], unique = true),
        Index(value = ["organization_id", "customer_id", "posted_at"]),
        Index(value = ["organization_id", "customer_id"]),
        Index(value = ["organization_id", "reference_type", "reference_id"]),
    ],
)
data class CustomerLedgerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "customer_id") val customerId: String,
    @ColumnInfo(name = "entry_type") val entryType: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    @ColumnInfo(name = "balance_after_minor") val balanceAfterMinor: Long,
    @ColumnInfo(name = "reference_type") val referenceType: String,
    @ColumnInfo(name = "reference_id") val referenceId: String,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    val notes: String? = null,
    @ColumnInfo(name = "posted_at") val postedAt: Long,
    @ColumnInfo(name = "created_by") val createdBy: String? = null,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state", defaultValue = "0") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
)

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["organization_id", "id"], childColumns = ["organization_id", "product_id"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index(value = ["organization_id", "id"], unique = true),
        Index(value = ["organization_id", "product_id", "effective_at"]),
        Index(value = ["organization_id", "product_id"]),
    ],
)
data class PriceHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "old_price_minor") val oldPriceMinor: Long,
    @ColumnInfo(name = "new_price_minor") val newPriceMinor: Long,
    @ColumnInfo(name = "old_cost_minor") val oldCostMinor: Long? = null,
    @ColumnInfo(name = "new_cost_minor") val newCostMinor: Long? = null,
    @ColumnInfo(name = "change_reason") val changeReason: String? = null,
    @ColumnInfo(name = "effective_at") val effectiveAt: Long,
    @ColumnInfo(name = "changed_by") val changedBy: String? = null,
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "sync_state", defaultValue = "0") val syncState: Int = SyncState.PENDING,
    @ColumnInfo(name = "server_updated_at") val serverUpdatedAt: Long? = null,
)

// ───────────────────────── Sync ─────────────────────────

@Entity(
    tableName = "sync_outbox",
    foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT)],
    indices = [
        Index(value = ["organization_id", "idempotency_key"], unique = true),
        Index(value = ["organization_id", "status", "next_attempt_at"]),
        Index(value = ["organization_id", "entity_type", "entity_id"]),
    ],
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    val operation: String,                                                  // UPSERT | UPSERT_AGGREGATE | ARCHIVE
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    val status: String = OutboxStatus.PENDING,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int = 0,
    @ColumnInfo(name = "next_attempt_at") val nextAttemptAt: Long = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "sync_state",
    primaryKeys = ["organization_id", "entity_type"],
    foreignKeys = [ForeignKey(entity = OrganizationEntity::class, parentColumns = ["id"], childColumns = ["organization_id"], onDelete = ForeignKey.RESTRICT)],
)
data class SyncStateEntity(
    @ColumnInfo(name = "organization_id") val organizationId: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "last_pulled_at") val lastPulledAt: Long = 0,        // server-time cursor
    @ColumnInfo(name = "last_pushed_at") val lastPushedAt: Long = 0,
)
