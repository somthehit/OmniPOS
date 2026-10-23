package com.example.pos.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.pos.data.local.BranchEntity
import com.example.pos.data.local.CashMovementEntity
import com.example.pos.data.local.CategoryEntity
import com.example.pos.data.local.CustomerEntity
import com.example.pos.data.local.CustomerLedgerEntity
import com.example.pos.data.local.ItemBarcodeEntity
import com.example.pos.data.local.OrganizationEntity
import com.example.pos.data.local.ProductEntity
import com.example.pos.data.local.RegisterEntity
import com.example.pos.data.local.SaleEntity
import com.example.pos.data.local.SaleItemEntity
import com.example.pos.data.local.SalePaymentEntity
import com.example.pos.data.local.ShiftEntity
import com.example.pos.data.local.StockMovementEntity
import com.example.pos.data.local.StoreSettingsEntity
import com.example.pos.data.local.UserEntity
import com.example.pos.data.model.MovementType
import com.example.pos.data.model.PaymentType
import com.example.pos.data.model.StaffRole
import com.example.pos.data.model.TaxMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupCounts(
    val productsCount: Int = 0,
    val barcodesCount: Int = 0,
    val categoriesCount: Int = 0,
    val customersCount: Int = 0,
    val customerLedgerCount: Int = 0,
    val salesCount: Int = 0,
    val saleItemsCount: Int = 0,
    val paymentsCount: Int = 0,
    val stockMovementsCount: Int = 0,
    val shiftsCount: Int = 0,
    val cashMovementsCount: Int = 0
) {
    val totalRecords: Int
        get() = productsCount + barcodesCount + categoriesCount + customersCount +
                customerLedgerCount + salesCount + saleItemsCount + paymentsCount +
                stockMovementsCount + shiftsCount + cashMovementsCount
}

data class ParsedBackup(
    val version: Int,
    val exportedAt: Long,
    val orgId: String,
    val orgName: String,
    val counts: BackupCounts,
    val categories: List<CategoryEntity>,
    val products: List<ProductEntity>,
    val itemBarcodes: List<ItemBarcodeEntity>,
    val customers: List<CustomerEntity>,
    val customerLedger: List<CustomerLedgerEntity>,
    val sales: List<SaleEntity>,
    val saleItems: List<SaleItemEntity>,
    val salePayments: List<SalePaymentEntity>,
    val stockMovements: List<StockMovementEntity>,
    val shifts: List<ShiftEntity>,
    val cashMovements: List<CashMovementEntity>,
    val storeSettings: StoreSettingsEntity?
)

object DataBackupManager {

    const val CURRENT_SCHEMA_VERSION = 1

    /**
     * Serializes all organization data into a comprehensive JSON backup string.
     */
    fun createBackupJson(
        org: OrganizationEntity,
        storeSettings: StoreSettingsEntity?,
        categories: List<CategoryEntity>,
        products: List<ProductEntity>,
        itemBarcodes: List<ItemBarcodeEntity>,
        customers: List<CustomerEntity>,
        customerLedger: List<CustomerLedgerEntity>,
        sales: List<SaleEntity>,
        saleItems: List<SaleItemEntity>,
        salePayments: List<SalePaymentEntity>,
        stockMovements: List<StockMovementEntity>,
        shifts: List<ShiftEntity>,
        cashMovements: List<CashMovementEntity>
    ): String {
        val root = JSONObject()
        root.put("app_name", "OmniPOS")
        root.put("version", CURRENT_SCHEMA_VERSION)
        root.put("exported_at", System.currentTimeMillis())
        root.put("exported_at_iso", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
        root.put("org_id", org.id)
        root.put("org_name", org.name)

        // Summary Counts
        val countsJson = JSONObject()
        countsJson.put("categories", categories.size)
        countsJson.put("products", products.size)
        countsJson.put("item_barcodes", itemBarcodes.size)
        countsJson.put("customers", customers.size)
        countsJson.put("customer_ledger", customerLedger.size)
        countsJson.put("sales", sales.size)
        countsJson.put("sale_items", saleItems.size)
        countsJson.put("sale_payments", salePayments.size)
        countsJson.put("stock_movements", stockMovements.size)
        countsJson.put("shifts", shifts.size)
        countsJson.put("cash_movements", cashMovements.size)
        root.put("counts", countsJson)

        // Data payload
        val dataObj = JSONObject()

        // 1. Categories
        val catsArray = JSONArray()
        categories.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("org_id", c.orgId)
            obj.put("name", c.name)
            obj.put("color_hex", c.colorHex)
            obj.put("icon_name", c.iconName)
            catsArray.put(obj)
        }
        dataObj.put("categories", catsArray)

        // 2. Products
        val prodsArray = JSONArray()
        products.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("org_id", p.orgId)
            obj.put("category_id", p.categoryId)
            obj.put("name", p.name)
            obj.put("sku", p.sku)
            obj.put("barcode", p.barcode)
            obj.put("cost_price_in_cents", p.costPriceInCents)
            obj.put("selling_price_in_cents", p.sellingPriceInCents)
            obj.put("tax_rate_percent", p.taxRatePercent)
            obj.put("unit", p.unit)
            obj.put("min_stock_alert", p.minStockAlert)
            prodsArray.put(obj)
        }
        dataObj.put("products", prodsArray)

        // 3. Item Barcodes
        val barcodesArray = JSONArray()
        itemBarcodes.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("org_id", b.orgId)
            obj.put("item_id", b.itemId)
            obj.put("barcode", b.barcode)
            obj.put("pack_qty", b.packQty)
            obj.put("created_at", b.createdAt)
            barcodesArray.put(obj)
        }
        dataObj.put("item_barcodes", barcodesArray)

        // 4. Customers
        val custsArray = JSONArray()
        customers.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("org_id", c.orgId)
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            obj.put("email", c.email)
            obj.put("credit_limit_in_cents", c.creditLimitInCents)
            obj.put("current_due_in_cents", c.currentDueInCents)
            obj.put("created_at", c.createdAt)
            custsArray.put(obj)
        }
        dataObj.put("customers", custsArray)

        // 5. Customer Ledger
        val ledgerArray = JSONArray()
        customerLedger.forEach { l ->
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("org_id", l.orgId)
            obj.put("customer_id", l.customerId)
            obj.put("sale_id", l.saleId)
            obj.put("entry_type", l.entryType)
            obj.put("amount_in_cents", l.amountInCents)
            obj.put("balance_after_in_cents", l.balanceAfterInCents)
            obj.put("created_at", l.createdAt)
            obj.put("notes", l.notes)
            ledgerArray.put(obj)
        }
        dataObj.put("customer_ledger", ledgerArray)

        // 6. Sales
        val salesArray = JSONArray()
        sales.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("org_id", s.orgId)
            obj.put("branch_id", s.branchId)
            obj.put("register_id", s.registerId)
            obj.put("invoice_no", s.invoiceNo)
            obj.put("customer_id", s.customerId)
            obj.put("subtotal_in_cents", s.subtotalInCents)
            obj.put("discount_in_cents", s.discountInCents)
            obj.put("tax_in_cents", s.taxInCents)
            obj.put("grand_total_in_cents", s.grandTotalInCents)
            obj.put("payment_status", s.paymentStatus)
            obj.put("created_at", s.createdAt)
            obj.put("created_by", s.createdBy)
            salesArray.put(obj)
        }
        dataObj.put("sales", salesArray)

        // 7. Sale Items
        val saleItemsArray = JSONArray()
        saleItems.forEach { si ->
            val obj = JSONObject()
            obj.put("id", si.id)
            obj.put("org_id", si.orgId)
            obj.put("sale_id", si.saleId)
            obj.put("product_id", si.productId)
            obj.put("product_name", si.productName)
            obj.put("unit_price_in_cents", si.unitPriceInCents)
            obj.put("quantity", si.quantity)
            obj.put("discount_in_cents", si.discountInCents)
            obj.put("tax_in_cents", si.taxInCents)
            obj.put("line_total_in_cents", si.lineTotalInCents)
            saleItemsArray.put(obj)
        }
        dataObj.put("sale_items", saleItemsArray)

        // 8. Sale Payments
        val paymentsArray = JSONArray()
        salePayments.forEach { sp ->
            val obj = JSONObject()
            obj.put("id", sp.id)
            obj.put("org_id", sp.orgId)
            obj.put("sale_id", sp.saleId)
            obj.put("payment_type", sp.paymentType.name)
            obj.put("amount_in_cents", sp.amountInCents)
            obj.put("change_given_in_cents", sp.changeGivenInCents)
            obj.put("reference_no", sp.referenceNo)
            paymentsArray.put(obj)
        }
        dataObj.put("sale_payments", paymentsArray)

        // 9. Stock Movements
        val movesArray = JSONArray()
        stockMovements.forEach { sm ->
            val obj = JSONObject()
            obj.put("id", sm.id)
            obj.put("org_id", sm.orgId)
            obj.put("branch_id", sm.branchId)
            obj.put("product_id", sm.productId)
            obj.put("quantity_delta", sm.quantityDelta)
            obj.put("type", sm.type.name)
            obj.put("reference_id", sm.referenceId)
            obj.put("created_at", sm.createdAt)
            obj.put("created_by", sm.createdBy)
            movesArray.put(obj)
        }
        dataObj.put("stock_movements", movesArray)

        // 10. Shifts
        val shiftsArray = JSONArray()
        shifts.forEach { sh ->
            val obj = JSONObject()
            obj.put("id", sh.id)
            obj.put("org_id", sh.orgId)
            obj.put("register_id", sh.registerId)
            obj.put("cashier_uid", sh.cashierUid)
            obj.put("cashier_name", sh.cashierName)
            obj.put("opened_at", sh.openedAt)
            obj.put("opening_float_in_cents", sh.openingFloatInCents)
            obj.put("closed_at", sh.closedAt)
            obj.put("counted_cash_in_cents", sh.countedCashInCents)
            obj.put("expected_cash_in_cents", sh.expectedCashInCents)
            obj.put("variance_in_cents", sh.varianceInCents)
            obj.put("status", sh.status)
            obj.put("notes", sh.notes)
            shiftsArray.put(obj)
        }
        dataObj.put("shifts", shiftsArray)

        // 11. Cash Movements
        val cashMovesArray = JSONArray()
        cashMovements.forEach { cm ->
            val obj = JSONObject()
            obj.put("id", cm.id)
            obj.put("org_id", cm.orgId)
            obj.put("shift_id", cm.shiftId)
            obj.put("movement_type", cm.movementType)
            obj.put("amount_in_cents", cm.amountInCents)
            obj.put("reason", cm.reason)
            obj.put("created_at", cm.createdAt)
            cashMovesArray.put(obj)
        }
        dataObj.put("cash_movements", cashMovesArray)

        // 12. Store Settings
        storeSettings?.let { ss ->
            val ssObj = JSONObject()
            ssObj.put("org_id", ss.orgId)
            ssObj.put("allow_cash", ss.allowCash)
            ssObj.put("allow_card", ss.allowCard)
            ssObj.put("allow_qr", ss.allowQr)
            ssObj.put("allow_credit", ss.allowCredit)
            ssObj.put("max_discount_percent", ss.maxDiscountPercent)
            ssObj.put("receipt_header_note", ss.receiptHeaderNote)
            ssObj.put("receipt_footer_note", ss.receiptFooterNote)
            ssObj.put("auto_print_receipt", ss.autoPrintReceipt)
            ssObj.put("low_stock_threshold", ss.lowStockThreshold)
            ssObj.put("negative_stock_behavior", ss.negativeStockBehavior)
            ssObj.put("auto_lock_interval", ss.autoLockInterval)
            ssObj.put("auto_logout_interval", ss.autoLogoutInterval)
            ssObj.put("biometric_enabled", ss.biometricEnabled)
            ssObj.put("login_biometric_enabled", ss.loginBiometricEnabled)
            ssObj.put("enforce_strong_pin", ss.enforceStrongPin)
            ssObj.put("printer_name", ss.printerName)
            ssObj.put("printer_connection_type", ss.printerConnectionType)
            ssObj.put("paper_width_mm", ss.paperWidthMm)
            ssObj.put("scanner_mode", ss.scannerMode)
            ssObj.put("cash_drawer_trigger", ss.cashDrawerTrigger)
            ssObj.put("backup_email", ss.backupEmail)
            ssObj.put("language", ss.language)
            dataObj.put("store_settings", ssObj)
        }

        root.put("data", dataObj)
        return root.toString(2)
    }

    /**
     * Parses and validates a JSON backup payload.
     */
    fun parseBackupJson(jsonString: String, targetOrgId: String): ParsedBackup {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val exportedAt = root.optLong("exported_at", System.currentTimeMillis())
        val backupOrgId = root.optString("org_id", targetOrgId)
        val orgName = root.optString("org_name", "ShopFlow POS")

        val dataObj = root.optJSONObject("data") ?: root

        // Categories
        val cats = mutableListOf<CategoryEntity>()
        val catsArray = dataObj.optJSONArray("categories")
        if (catsArray != null) {
            for (i in 0 until catsArray.length()) {
                val o = catsArray.getJSONObject(i)
                cats.add(
                    CategoryEntity(
                        id = o.getString("id"),
                        orgId = targetOrgId,
                        name = o.getString("name"),
                        colorHex = o.optString("color_hex", o.optString("color", "#3B82F6")),
                        iconName = o.optString("icon_name", "category")
                    )
                )
            }
        }

        // Products
        val prods = mutableListOf<ProductEntity>()
        val prodsArray = dataObj.optJSONArray("products")
        if (prodsArray != null) {
            for (i in 0 until prodsArray.length()) {
                val o = prodsArray.getJSONObject(i)
                prods.add(
                    ProductEntity(
                        id = o.getString("id"),
                        orgId = targetOrgId,
                        categoryId = o.getString("category_id"),
                        name = o.getString("name"),
                        sku = o.getString("sku"),
                        barcode = o.getString("barcode"),
                        costPriceInCents = o.optLong("cost_price_in_cents", 0L),
                        sellingPriceInCents = o.optLong("selling_price_in_cents", 0L),
                        taxRatePercent = o.optDouble("tax_rate_percent", o.optDouble("tax_rate", 0.0)),
                        unit = o.optString("unit", "pcs"),
                        minStockAlert = o.optInt("min_stock_alert", 5)
                    )
                )
            }
        }

        // Item Barcodes
        val barcodes = mutableListOf<ItemBarcodeEntity>()
        val barcodesArray = dataObj.optJSONArray("item_barcodes")
        if (barcodesArray != null) {
            for (i in 0 until barcodesArray.length()) {
                val o = barcodesArray.getJSONObject(i)
                barcodes.add(
                    ItemBarcodeEntity(
                        id = o.getString("id"),
                        orgId = targetOrgId,
                        itemId = o.getString("item_id"),
                        barcode = o.getString("barcode"),
                        packQty = o.optInt("pack_qty", 1),
                        createdAt = o.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // Customers
        val custs = mutableListOf<CustomerEntity>()
        val custsArray = dataObj.optJSONArray("customers")
        if (custsArray != null) {
            for (i in 0 until custsArray.length()) {
                val o = custsArray.getJSONObject(i)
                custs.add(
                    CustomerEntity(
                        id = o.getString("id"),
                        orgId = targetOrgId,
                        name = o.getString("name"),
                        phone = o.optString("phone", ""),
                        email = o.optString("email", ""),
                        creditLimitInCents = o.optLong("credit_limit_in_cents", 50000L),
                        currentDueInCents = o.optLong("current_due_in_cents", 0L),
                        createdAt = o.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // Customer Ledger
        val ledger = mutableListOf<CustomerLedgerEntity>()
        val ledgerArray = dataObj.optJSONArray("customer_ledger")
        if (ledgerArray != null) {
            for (i in 0 until ledgerArray.length()) {
                val o = ledgerArray.getJSONObject(i)
                ledger.add(
                    CustomerLedgerEntity(
                        id = o.getString("id"),
                        orgId = targetOrgId,
                        customerId = o.getString("customer_id"),
                        saleId = if (o.has("sale_id") && !o.isNull("sale_id")) o.getString("sale_id") else null,
                        entryType = o.getString("entry_type"),
                        amountInCents = o.getLong("amount_in_cents"),
                        balanceAfterInCents = o.getLong("balance_after_in_cents"),
                        notes = o.optString("notes", o.optString("note", "")),
                        createdAt = o.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // Sales
        val sales = mutableListOf<SaleEntity>()
        val salesArray = dataObj.optJSONArray("sales")
        if (salesArray != null) {
            for (i in 0 until salesArray.length()) {
                val o = salesArray.getJSONObject(i)
                sales.add(
                    SaleEntity(
                        id = o.getString("id"),
                        orgId = targetOrgId,
                        branchId = o.optString("branch_id", "main_branch"),
                        registerId = o.optString("register_id", "reg_default"),
                        invoiceNo = o.getString("invoice_no"),
                        customerId = if (o.has("customer_id") && !o.isNull("customer_id")) o.getString("customer_id") else null,
                        subtotalInCents = o.optLong("subtotal_in_cents", 0L),
                        discountInCents = o.optLong("discount_in_cents", 0L),
                        taxInCents = o.optLong("tax_in_cents", 0L),
                        grandTotalInCents = o.optLong("grand_total_in_cents", 0L),
                        paymentStatus = o.optString("payment_status", "COMPLETED"),
                        createdAt = o.optLong("created_at", System.currentTimeMillis()),
                        createdBy = o.optString("created_by", "")
                    )
                )
            }
        }

        // Sale Items
        val saleItems = mutableListOf<SaleItemEntity>()
        val saleItemsArray = dataObj.optJSONArray("sale_items")
        if (saleItemsArray != null) {
            for (i in 0 until saleItemsArray.length()) {
                val o = saleItemsArray.getJSONObject(i)
                saleItems.add(
                    SaleItemEntity(
                        id = o.getString("id"),
                        orgId = targetOrgId,
                        saleId = o.getString("sale_id"),
                        productId = o.getString("product_id"),
                        productName = o.getString("product_name"),
                        unitPriceInCents = o.optLong("unit_price_in_cents", 0L),
                        quantity = o.optInt("quantity", 1),
                        discountInCents = o.optLong("discount_in_cents", 0L),
                        taxInCents = o.optLong("tax_in_cents", 0L),
                        lineTotalInCents = o.optLong("line_total_in_cents", 0L)
                    )
                )
            }
        }

        // Sale Payments
        val salePayments = mutableListOf<SalePaymentEntity>()
        val paymentsArray = dataObj.optJSONArray("sale_payments")
        if (paymentsArray != null) {
            for (i in 0 until paymentsArray.length()) {
                val o = paymentsArray.getJSONObject(i)
                val pTypeName = o.optString("payment_type", "CASH")
                val pType = try {
                    PaymentType.valueOf(pTypeName)
                } catch (e: Exception) {
                    PaymentType.CASH
                }
                salePayments.add(
                    SalePaymentEntity(
                        id = o.getString("id"),
                        orgId = targetOrgId,
                        saleId = o.getString("sale_id"),
                        paymentType = pType,
                        amountInCents = o.optLong("amount_in_cents", 0L),
                        changeGivenInCents = o.optLong("change_given_in_cents", 0L),
                        referenceNo = o.optString("reference_no", "")
                    )
                )
            }
        }

        // Stock Movements
        val stockMoves = mutableListOf<StockMovementEntity>()
        val movesArray = dataObj.optJSONArray("stock_movements")
        if (movesArray != null) {
            for (i in 0 until movesArray.length()) {
                val o = movesArray.getJSONObject(i)
                val moveTypeName = o.optString("type", o.optString("movement_type", "RECEIPT"))
                val mType = try {
                    MovementType.valueOf(moveTypeName)
                } catch (e: Exception) {
                    MovementType.RECEIPT
                }
                stockMoves.add(
                    StockMovementEntity(
                        id = o.getString("id"),
                        orgId = targetOrgId,
                        branchId = o.optString("branch_id", "main_branch"),
                        productId = o.getString("product_id"),
                        quantityDelta = o.optInt("quantity_delta", 0),
                        type = mType,
                        referenceId = o.optString("reference_id", ""),
                        createdAt = o.optLong("created_at", System.currentTimeMillis()),
                        createdBy = o.optString("created_by", "")
                    )
                )
            }
        }

        // Shifts
        val shifts = mutableListOf<ShiftEntity>()
        val shiftsArray = dataObj.optJSONArray("shifts")
        if (shiftsArray != null) {
            for (i in 0 until shiftsArray.length()) {
                val o = shiftsArray.getJSONObject(i)
                shifts.add(
                    ShiftEntity(
                        id = o.getString("id"),
                        orgId = targetOrgId,
                        registerId = o.optString("register_id", "reg_default"),
                        cashierUid = o.optString("cashier_uid", o.optString("opened_by_user_id", "staff_default")),
                        cashierName = o.optString("cashier_name", "Till Cashier"),
                        openedAt = o.optLong("opened_at", System.currentTimeMillis()),
                        openingFloatInCents = o.optLong("opening_float_in_cents", o.optLong("opening_cash_in_cents", 0L)),
                        closedAt = if (o.has("closed_at") && !o.isNull("closed_at")) o.getLong("closed_at") else null,
                        countedCashInCents = if (o.has("counted_cash_in_cents") && !o.isNull("counted_cash_in_cents")) o.getLong("counted_cash_in_cents") else null,
                        expectedCashInCents = if (o.has("expected_cash_in_cents") && !o.isNull("expected_cash_in_cents")) o.getLong("expected_cash_in_cents") else null,
                        varianceInCents = if (o.has("variance_in_cents") && !o.isNull("variance_in_cents")) o.getLong("variance_in_cents") else null,
                        status = o.optString("status", "CLOSED"),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }

        // Cash Movements
        val cashMoves = mutableListOf<CashMovementEntity>()
        val cashMovesArray = dataObj.optJSONArray("cash_movements")
        if (cashMovesArray != null) {
            for (i in 0 until cashMovesArray.length()) {
                val o = cashMovesArray.getJSONObject(i)
                cashMoves.add(
                    CashMovementEntity(
                        id = o.getString("id"),
                        orgId = targetOrgId,
                        shiftId = o.getString("shift_id"),
                        movementType = o.getString("movement_type"),
                        amountInCents = o.optLong("amount_in_cents", 0L),
                        reason = o.optString("reason", ""),
                        createdAt = o.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }
        }

        // Store Settings
        val settingsObj = dataObj.optJSONObject("store_settings")
        val settings = settingsObj?.let { ss ->
            StoreSettingsEntity(
                orgId = targetOrgId,
                allowCash = ss.optBoolean("allow_cash", true),
                allowCard = ss.optBoolean("allow_card", true),
                allowQr = ss.optBoolean("allow_qr", true),
                allowCredit = ss.optBoolean("allow_credit", true),
                maxDiscountPercent = ss.optInt("max_discount_percent", 15),
                receiptHeaderNote = ss.optString("receipt_header_note", "Thank you for shopping!"),
                receiptFooterNote = ss.optString("receipt_footer_note", "Please come again!"),
                autoPrintReceipt = ss.optBoolean("auto_print_receipt", true),
                lowStockThreshold = ss.optInt("low_stock_threshold", 5),
                negativeStockBehavior = ss.optString("negative_stock_behavior", "BLOCK"),
                autoLockInterval = ss.optString("auto_lock_interval", "5 minutes"),
                autoLogoutInterval = ss.optString("auto_logout_interval", "Never"),
                biometricEnabled = ss.optBoolean("biometric_enabled", false),
                loginBiometricEnabled = ss.optBoolean("login_biometric_enabled", false),
                enforceStrongPin = ss.optBoolean("enforce_strong_pin", true),
                printerName = ss.optString("printer_name", ""),
                printerConnectionType = ss.optString("printer_connection_type", "BLUETOOTH"),
                paperWidthMm = ss.optInt("paper_width_mm", 80),
                scannerMode = ss.optString("scanner_mode", "USB HID Keyboard Emulation Mode"),
                cashDrawerTrigger = ss.optBoolean("cash_drawer_trigger", true),
                backupEmail = ss.optString("backup_email", ""),
                language = ss.optString("language", "en")
            )
        }

        val counts = BackupCounts(
            productsCount = prods.size,
            barcodesCount = barcodes.size,
            categoriesCount = cats.size,
            customersCount = custs.size,
            customerLedgerCount = ledger.size,
            salesCount = sales.size,
            saleItemsCount = saleItems.size,
            paymentsCount = salePayments.size,
            stockMovementsCount = stockMoves.size,
            shiftsCount = shifts.size,
            cashMovementsCount = cashMoves.size
        )

        return ParsedBackup(
            version = version,
            exportedAt = exportedAt,
            orgId = backupOrgId,
            orgName = orgName,
            counts = counts,
            categories = cats,
            products = prods,
            itemBarcodes = barcodes,
            customers = custs,
            customerLedger = ledger,
            sales = sales,
            saleItems = saleItems,
            salePayments = salePayments,
            stockMovements = stockMoves,
            shifts = shifts,
            cashMovements = cashMoves,
            storeSettings = settings
        )
    }

    /**
     * Saves backup content to a clean, formatted JSON file in the application's cache directory.
     */
    fun saveBackupToFile(context: Context, backupJson: String, orgName: String): File {
        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val cleanOrg = orgName.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase(Locale.US)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(backupDir, "omnipos_backup_${cleanOrg}_$timestamp.json")

        FileOutputStream(file).use { out ->
            out.write(backupJson.toByteArray(Charsets.UTF_8))
            out.flush()
        }

        return file
    }

    /**
     * Builds an Android Intent to send or store the backup in Gmail / Email.
     */
    fun createGmailBackupIntent(
        context: Context,
        backupFile: File,
        recipientGmail: String,
        orgName: String,
        countsSummary: String
    ): Intent {
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date())
        val subject = "OmniPOS Database Backup - $orgName ($dateStr)"
        val body = """
Hello,

Attached is your automated OmniPOS database backup for:
Store: $orgName
Backup Date: $dateStr

Summary of Saved Records:
$countsSummary

How to restore:
1. Open OmniPOS on your device.
2. Go to Settings > Backup & Restore.
3. Tap 'Restore from Backup' and choose this attached JSON file.

Keep this email safe in your Gmail account.
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            if (recipientGmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientGmail))
            }
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Try explicitly targeting Gmail app if installed
        val gmailPackage = "com.google.android.gm"
        val pm = context.packageManager
        val hasGmail = try {
            pm.getPackageInfo(gmailPackage, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

        return if (hasGmail) {
            intent.setPackage(gmailPackage)
            intent
        } else {
            Intent.createChooser(intent, "Store Backup in Gmail / Email")
        }
    }
}
