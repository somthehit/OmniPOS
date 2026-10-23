package com.example.pos.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.pos.data.model.MovementType
import com.example.pos.data.model.OutboxOp
import com.example.pos.data.model.PaymentType
import com.example.pos.data.model.StaffRole
import com.example.pos.data.model.TaxMode

/*
 * Multi-tenant Room database entities.
 * Every table includes an 'org_id' column for strict tenant isolation.
 */

@Entity(
    tableName = "organizations",
    indices = [Index(value = ["org_id"], unique = true)]
)
data class OrganizationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String = id,
    val name: String,
    @ColumnInfo(name = "tax_id") val taxId: String,
    @ColumnInfo(name = "currency_symbol") val currencySymbol: String = "$",
    @ColumnInfo(name = "tax_mode") val taxMode: TaxMode = TaxMode.EXCLUSIVE,
    @ColumnInfo(name = "owner_uid") val ownerUid: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

typealias Organization = OrganizationEntity

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "email"])
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    val email: String,
    val phone: String? = null,
    val role: StaffRole = StaffRole.CASHIER,
    @ColumnInfo(name = "pin_hash") val pinHash: String = "1234",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

typealias User = UserEntity

@Entity(
    tableName = "branches",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "code"], unique = true)
    ]
)
data class BranchEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val name: String,
    val code: String,
    val address: String,
    val phone: String
)

@Entity(
    tableName = "registers",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "branch_id"])
    ]
)
data class RegisterEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    val name: String,
    val code: String,
    @ColumnInfo(name = "current_shift_id") val currentShiftId: String? = null
)

@Entity(
    tableName = "staff_members",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "uid"])
    ]
)
data class StaffMemberEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val uid: String,
    val name: String,
    val email: String,
    val role: StaffRole,
    @ColumnInfo(name = "pin_hash") val pinHash: String = "1234",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "name"])
    ]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val name: String,
    @ColumnInfo(name = "color_hex") val colorHex: String = "#3B82F6",
    @ColumnInfo(name = "icon_name") val iconName: String = "category"
)

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "sku"], unique = true),
        Index(value = ["org_id", "category_id"]),
        Index(value = ["org_id", "barcode"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    val name: String,
    val sku: String,
    val barcode: String,
    @ColumnInfo(name = "cost_price_in_cents") val costPriceInCents: Long,
    @ColumnInfo(name = "selling_price_in_cents") val sellingPriceInCents: Long,
    @ColumnInfo(name = "tax_rate_percent") val taxRatePercent: Double = 0.0,
    val unit: String = "pcs",
    @ColumnInfo(name = "min_stock_alert") val minStockAlert: Int = 5
)

typealias Product = ProductEntity

@Entity(
    tableName = "item_barcodes",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "barcode"], unique = true),
        Index(value = ["org_id", "item_id"])
    ]
)
data class ItemBarcodeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    val barcode: String,
    @ColumnInfo(name = "pack_qty") val packQty: Int = 1,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "branch_id", "product_id"]),
        Index(value = ["org_id", "product_id"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "quantity_delta") val quantityDelta: Int,
    val type: MovementType,
    @ColumnInfo(name = "reference_id") val referenceId: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_by") val createdBy: String = ""
)

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "invoice_no"], unique = true),
        Index(value = ["org_id", "register_id"]),
        Index(value = ["org_id", "customer_id"]),
        Index(value = ["org_id", "created_at"])
    ]
)
data class SaleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "register_id") val registerId: String,
    @ColumnInfo(name = "invoice_no") val invoiceNo: String,
    @ColumnInfo(name = "fiscal_year") val fiscalYear: String = "",
    @ColumnInfo(name = "customer_id") val customerId: String? = null,
    @ColumnInfo(name = "subtotal_in_cents") val subtotalInCents: Long,
    @ColumnInfo(name = "discount_in_cents") val discountInCents: Long,
    @ColumnInfo(name = "tax_in_cents") val taxInCents: Long,
    @ColumnInfo(name = "grand_total_in_cents") val grandTotalInCents: Long,
    @ColumnInfo(name = "payment_status") val paymentStatus: String = "COMPLETED",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_by") val createdBy: String = ""
)

typealias Sale = SaleEntity

@Entity(
    tableName = "sale_items",
    indices = [
        Index(value = ["sale_id", "id"], unique = true),
        Index(value = ["org_id", "sale_id"]),
        Index(value = ["org_id", "product_id"])
    ]
)
data class SaleItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String = "",
    @ColumnInfo(name = "sale_id") val saleId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "unit_price_in_cents") val unitPriceInCents: Long,
    val quantity: Int,
    @ColumnInfo(name = "discount_in_cents") val discountInCents: Long,
    @ColumnInfo(name = "tax_in_cents") val taxInCents: Long,
    @ColumnInfo(name = "line_total_in_cents") val lineTotalInCents: Long
)

@Entity(
    tableName = "sale_payments",
    indices = [
        Index(value = ["sale_id", "id"], unique = true),
        Index(value = ["org_id", "sale_id"])
    ]
)
data class SalePaymentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String = "",
    @ColumnInfo(name = "sale_id") val saleId: String,
    @ColumnInfo(name = "payment_type") val paymentType: PaymentType,
    @ColumnInfo(name = "amount_in_cents") val amountInCents: Long,
    @ColumnInfo(name = "change_given_in_cents") val changeGivenInCents: Long = 0L,
    @ColumnInfo(name = "reference_no") val referenceNo: String = ""
)

@Entity(
    tableName = "held_orders",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "register_id"])
    ]
)
data class HeldOrderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "register_id") val registerId: String,
    @ColumnInfo(name = "customer_name") val customerName: String,
    @ColumnInfo(name = "cart_json") val cartJson: String,
    @ColumnInfo(name = "total_in_cents") val totalInCents: Long,
    @ColumnInfo(name = "held_at") val heldAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "phone"])
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    val name: String,
    val phone: String,
    val email: String,
    @ColumnInfo(name = "credit_limit_in_cents") val creditLimitInCents: Long = 50000L, // e.g. $500 default
    @ColumnInfo(name = "current_due_in_cents") val currentDueInCents: Long = 0L,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_ledger",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "customer_id"])
    ]
)
data class CustomerLedgerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "customer_id") val customerId: String,
    @ColumnInfo(name = "sale_id") val saleId: String? = null,
    @ColumnInfo(name = "entry_type") val entryType: String, // "SALE_CREDIT" or "DEBT_REPAYMENT"
    @ColumnInfo(name = "amount_in_cents") val amountInCents: Long,
    @ColumnInfo(name = "balance_after_in_cents") val balanceAfterInCents: Long,
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shifts",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "register_id", "status"]),
        Index(value = ["org_id", "cashier_uid", "status"]),
        Index(value = ["org_id", "business_date"])
    ]
)
data class ShiftEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "branch_id") val branchId: String = "br_main_01",
    @ColumnInfo(name = "register_id") val registerId: String,
    @ColumnInfo(name = "cashier_uid") val cashierUid: String,
    @ColumnInfo(name = "cashier_name") val cashierName: String,
    @ColumnInfo(name = "business_date") val businessDate: String = "",
    @ColumnInfo(name = "opened_at") val openedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "opening_float_in_cents") val openingFloatInCents: Long,
    @ColumnInfo(name = "opening_expected_in_cents") val openingExpectedInCents: Long = 0L,
    @ColumnInfo(name = "opening_gap_in_cents") val openingGapInCents: Long = 0L,
    @ColumnInfo(name = "opening_gap_reason") val openingGapReason: String = "",
    @ColumnInfo(name = "closed_at") val closedAt: Long? = null,
    @ColumnInfo(name = "counted_cash_in_cents") val countedCashInCents: Long? = null,
    @ColumnInfo(name = "expected_cash_in_cents") val expectedCashInCents: Long? = null,
    @ColumnInfo(name = "closing_float_in_cents") val closingFloatInCents: Long = 0L,
    @ColumnInfo(name = "deposit_amount_in_cents") val depositAmountInCents: Long = 0L,
    @ColumnInfo(name = "variance_in_cents") val varianceInCents: Long? = null,
    @ColumnInfo(name = "variance_reason") val varianceReason: String = "",
    val status: String = "OPEN", // "NO_SHIFT", "OPEN", "CLOSING", "PENDING_REVIEW", "FLAGGED", "APPROVED", "FORCE_CLOSED"
    @ColumnInfo(name = "closed_by") val closedBy: String = "",
    @ColumnInfo(name = "review_status") val reviewStatus: String = "PENDING",
    @ColumnInfo(name = "reviewed_by") val reviewedBy: String? = null,
    @ColumnInfo(name = "reviewed_at") val reviewedAt: Long? = null,
    @ColumnInfo(name = "review_note") val reviewNote: String? = null,
    val version: Int = 1,
    @ColumnInfo(name = "last_device_id") val lastDeviceId: String = "",
    val notes: String = ""
)

@Entity(
    tableName = "shift_cash_counts",
    indices = [
        Index(value = ["shift_id", "phase", "denomination"], unique = true),
        Index(value = ["shift_id"])
    ]
)
data class ShiftCashCountEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "shift_id") val shiftId: String,
    val phase: String, // "OPENING", "CLOSING", "ADJUSTMENT"
    val denomination: Int, // 1000, 500, 100, 50, 20, 10, 5, 2, 1
    val count: Int,
    @ColumnInfo(name = "amount_in_cents") val amountInCents: Long = 0L,
    @ColumnInfo(name = "subtotal_in_cents") val subtotalInCents: Long = amountInCents
)

@Entity(
    tableName = "shift_declarations",
    indices = [
        Index(value = ["shift_id", "method"]),
        Index(value = ["shift_id"])
    ]
)
data class ShiftDeclarationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "shift_id") val shiftId: String,
    val type: String = "ONLINE", // "ONLINE", "CREDIT_NOTE"
    val method: String = "ONLINE", // "eSewa", "Khalti", "FonePay", "Card", "Bank"
    val provider: String = method,
    @ColumnInfo(name = "pos_amount_in_cents") val posAmountInCents: Long = 0L,
    @ColumnInfo(name = "provider_amount_in_cents") val providerAmountInCents: Long = 0L,
    @ColumnInfo(name = "declared_amount_in_cents") val declaredAmountInCents: Long = 0L,
    @ColumnInfo(name = "variance_in_cents") val varianceInCents: Long = 0L,
    val verification: String = "DECLARED_UNVERIFIED", // "VERIFIED", "DECLARED_UNVERIFIED"
    val reference: String = "",
    @ColumnInfo(name = "slip_count") val slipCount: Int = 0,
    @ColumnInfo(name = "reason_code") val reasonCode: String = "",
    val note: String = "",
    val notes: String = note
)

@Entity(
    tableName = "shift_credit_notes",
    indices = [
        Index(value = ["shift_id", "credit_note_id"]),
        Index(value = ["shift_id"])
    ]
)
data class ShiftCreditNoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "shift_id") val shiftId: String,
    @ColumnInfo(name = "credit_note_id") val creditNoteId: String = "",
    @ColumnInfo(name = "credit_note_no") val creditNoteNo: String = creditNoteId,
    @ColumnInfo(name = "total_in_cents") val totalInCents: Long = 0L,
    @ColumnInfo(name = "total_amount_in_cents") val totalAmountInCents: Long = totalInCents,
    @ColumnInfo(name = "cash_refund_in_cents") val cashRefundInCents: Long = 0L, // ONLY portion that reduces drawer cash
    @ColumnInfo(name = "customer_balance_in_cents") val customerBalanceInCents: Long = 0L,
    @ColumnInfo(name = "customer_credit_in_cents") val customerCreditInCents: Long = customerBalanceInCents,
    @ColumnInfo(name = "bank_refund_in_cents") val bankRefundInCents: Long = 0L,
    @ColumnInfo(name = "redeemed_as_tender_in_cents") val redeemedAsTenderInCents: Long = 0L,
    val reason: String = ""
)

@Entity(
    tableName = "shift_adjustments",
    indices = [
        Index(value = ["shift_id"])
    ]
)
data class ShiftAdjustmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "shift_id") val shiftId: String,
    val target: String = "CLOSING", // "OPENING", "CLOSING"
    @ColumnInfo(name = "supervisor_uid") val supervisorUid: String = "",
    @ColumnInfo(name = "amount_in_cents") val amountInCents: Long = 0L,
    @ColumnInfo(name = "before_json") val beforeJson: String = "{}",
    @ColumnInfo(name = "after_json") val afterJson: String = "{}",
    val reason: String = "",
    @ColumnInfo(name = "approved_by") val approvedBy: String = supervisorUid,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shift_versions",
    indices = [
        Index(value = ["shift_id", "version"], unique = true),
        Index(value = ["shift_id"])
    ]
)
data class ShiftVersionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "shift_id") val shiftId: String,
    val version: Int,
    @ColumnInfo(name = "snapshot_json") val snapshotJson: String,
    val reason: String,
    @ColumnInfo(name = "changed_by") val changedBy: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shift_events",
    indices = [
        Index(value = ["shift_id", "created_at"]),
        Index(value = ["org_id", "created_at"])
    ]
)
data class ShiftEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "shift_id") val shiftId: String,
    @ColumnInfo(name = "org_id") val orgId: String = "",
    val action: String = "", // "OPENED", "RESUMED", "CLOSING_DRAFT", "CLOSED", "FORCE_CLOSED", "REOPENED", "ADJUSTED", "REVIEWED", "FLAGGED"
    @ColumnInfo(name = "event_type") val eventType: String = action,
    @ColumnInfo(name = "actor_id") val actorId: String = "",
    @ColumnInfo(name = "actor_uid") val actorUid: String = actorId,
    @ColumnInfo(name = "actor_name") val actorName: String = "",
    @ColumnInfo(name = "device_id") val deviceId: String = "",
    @ColumnInfo(name = "payload_json") val payloadJson: String = "{}",
    @ColumnInfo(name = "details_json") val detailsJson: String = payloadJson,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cash_movements",
    indices = [
        Index(value = ["shift_id", "id"], unique = true),
        Index(value = ["org_id", "shift_id"])
    ]
)
data class CashMovementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String = "",
    @ColumnInfo(name = "shift_id") val shiftId: String,
    @ColumnInfo(name = "movement_type") val movementType: String, // "PAID_IN", "PAID_OUT", "SAFE_DROP", "CREDIT_COLLECTION"
    val tender: String = "CASH", // "CASH", "ONLINE"
    @ColumnInfo(name = "amount_in_cents") val amountInCents: Long,
    val reason: String,
    @ColumnInfo(name = "created_by") val createdBy: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "table_name"])
    ]
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "table_name") val tableName: String,
    @ColumnInfo(name = "record_id") val recordId: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    val operation: OutboxOp,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null
)

@Entity(
    tableName = "store_settings",
    indices = [
        Index(value = ["org_id"], unique = true)
    ]
)
data class StoreSettingsEntity(
    @PrimaryKey @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "allow_cash") val allowCash: Boolean = true,
    @ColumnInfo(name = "allow_card") val allowCard: Boolean = true,
    @ColumnInfo(name = "allow_qr") val allowQr: Boolean = true,
    @ColumnInfo(name = "allow_credit") val allowCredit: Boolean = true,
    @ColumnInfo(name = "max_discount_percent") val maxDiscountPercent: Int = 15,
    @ColumnInfo(name = "receipt_header_note") val receiptHeaderNote: String = "Thank you for shopping!",
    @ColumnInfo(name = "receipt_footer_note") val receiptFooterNote: String = "Please come again!",
    @ColumnInfo(name = "auto_print_receipt") val autoPrintReceipt: Boolean = true,
    @ColumnInfo(name = "low_stock_threshold") val lowStockThreshold: Int = 5,
    @ColumnInfo(name = "negative_stock_behavior") val negativeStockBehavior: String = "BLOCK",
    @ColumnInfo(name = "auto_lock_interval") val autoLockInterval: String = "5 minutes",
    @ColumnInfo(name = "auto_logout_interval") val autoLogoutInterval: String = "Never",
    @ColumnInfo(name = "biometric_enabled") val biometricEnabled: Boolean = false,
    @ColumnInfo(name = "login_biometric_enabled") val loginBiometricEnabled: Boolean = false,
    @ColumnInfo(name = "enforce_strong_pin") val enforceStrongPin: Boolean = true,
    @ColumnInfo(name = "printer_name") val printerName: String = "",
    @ColumnInfo(name = "printer_connection_type") val printerConnectionType: String = "BLUETOOTH",
    @ColumnInfo(name = "paper_width_mm") val paperWidthMm: Int = 80,
    @ColumnInfo(name = "scanner_mode") val scannerMode: String = "USB HID Keyboard Emulation Mode",
    @ColumnInfo(name = "cash_drawer_trigger") val cashDrawerTrigger: Boolean = true,
    @ColumnInfo(name = "backup_email") val backupEmail: String = "",
    @ColumnInfo(name = "last_backup_at") val lastBackupAt: Long? = null,
    @ColumnInfo(name = "language") val language: String = "en",
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "created_at"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "actor_uid") val actorUid: String,
    @ColumnInfo(name = "actor_name") val actorName: String,
    @ColumnInfo(name = "actor_role") val actorRole: StaffRole,
    val action: String,
    val details: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "fiscal_years",
    indices = [
        Index(value = ["org_id", "id"], unique = true),
        Index(value = ["org_id", "name"], unique = true),
        Index(value = ["org_id", "is_current"])
    ]
)
data class FiscalYearEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "org_id") val orgId: String,
    @ColumnInfo(name = "name") val name: String, // e.g. "2081/82"
    @ColumnInfo(name = "code") val code: String, // e.g. "81/82"
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "end_date") val endDate: Long,
    @ColumnInfo(name = "invoice_prefix") val invoicePrefix: String = "INV",
    @ColumnInfo(name = "current_invoice_sequence") val currentInvoiceSequence: Long = 0L,
    @ColumnInfo(name = "is_current") val isCurrent: Boolean = false,
    @ColumnInfo(name = "is_closed") val isClosed: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
