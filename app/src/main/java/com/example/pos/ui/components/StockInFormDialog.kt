package com.example.pos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pos.data.local.ProductEntity
import com.example.pos.data.local.StockMovementEntity
import com.example.pos.data.model.BarcodeLookupResult
import com.example.pos.domain.CartCalculator
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import java.util.Locale

@Composable
fun StockInFormDialog(
    products: List<ProductEntity>,
    preSelectedProduct: ProductEntity? = null,
    stockMovements: List<StockMovementEntity> = emptyList(),
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onSubmit: (
        productId: String,
        quantityMilliUnits: Long,
        unitCostInCents: Long,
        supplierName: String,
        invoiceRef: String,
        batchNumber: String,
        expiryDate: String,
        notes: String
    ) -> Unit,
    onSaveAndNext: ((
        productId: String,
        quantityMilliUnits: Long,
        unitCostInCents: Long,
        supplierName: String,
        invoiceRef: String,
        batchNumber: String,
        expiryDate: String,
        notes: String
    ) -> Unit)? = null,
    onLookupBarcode: ((barcode: String, onResult: (BarcodeLookupResult) -> Unit) -> Unit)? = null,
    onLinkBarcode: ((itemId: String, barcode: String, packQty: Int, onSuccess: () -> Unit) -> Unit)? = null,
    onCreateProductWithBarcode: ((name: String, sku: String, barcode: String, costPriceCents: Long, sellingPriceCents: Long, unit: String, packQty: Int, onSuccess: (ProductEntity) -> Unit) -> Unit)? = null
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    var selectedProduct by remember { mutableStateOf(preSelectedProduct ?: products.firstOrNull()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSelectingProduct by remember { mutableStateOf(preSelectedProduct == null && products.isNotEmpty()) }

    var quantityText by remember { mutableStateOf("10") }
    var isTotalCostMode by remember { mutableStateOf(false) }
    var unitCostText by remember {
        mutableStateOf(
            if (selectedProduct != null) String.format(Locale.US, "%.2f", selectedProduct!!.costPriceInCents / 100.0) else "0.00"
        )
    }
    var totalInvoiceCostText by remember { mutableStateOf("") }
    var isFreeStock by remember { mutableStateOf(false) }

    // Barcode details & pack sizes
    var matchedPackQty by remember { mutableStateOf(1) }
    var matchedBarcodeStr by remember { mutableStateOf(selectedProduct?.barcode ?: "") }

    // Supplier & Batch (Optional collapsible section)
    var isSupplierSectionExpanded by remember { mutableStateOf(false) }
    var supplierName by remember { mutableStateOf("Direct Wholesale Supplier") }
    var vatBillNumber by remember { mutableStateOf("") }
    var invoiceRef by remember { mutableStateOf("PO-${System.currentTimeMillis().toString().takeLast(6)}") }
    var batchNumber by remember { mutableStateOf("BATCH-${System.currentTimeMillis().toString().takeLast(4)}") }
    var expiryDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Dialog sheets & Scanner states
    var showCameraScannerSheet by remember { mutableStateOf(false) }
    var showNotFoundDialog by remember { mutableStateOf(false) }
    var notFoundBarcode by remember { mutableStateOf("") }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showQuickCreateDialog by remember { mutableStateOf(false) }
    var showInternalLabelDialog by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var successToast by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Computations
    val parsedQty = quantityText.toDoubleOrNull() ?: 0.0
    val parsedUnitCost = if (isFreeStock) 0.0 else (unitCostText.toDoubleOrNull() ?: 0.0)
    val totalCostValueInCents = if (isFreeStock) 0L else (parsedQty * parsedUnitCost * 100.0).toLong().coerceAtLeast(0L)

    val retailPriceInCents = selectedProduct?.sellingPriceInCents ?: 0L
    val unitCostInCents = (parsedUnitCost * 100.0).toLong()

    // Margin Calculation (Hidden when cost is 0, color-coded when entered)
    val hasCostEntered = parsedUnitCost > 0.0 && !isFreeStock
    val marginPercent = if (retailPriceInCents > 0L && unitCostInCents > 0L) {
        ((retailPriceInCents - unitCostInCents).toDouble() / retailPriceInCents.toDouble()) * 100.0
    } else {
        0.0
    }

    // Stock on hand & projected stock sanity check
    val currentStockUnits = remember(selectedProduct, stockMovements) {
        if (selectedProduct == null) 0
        else stockMovements.filter { it.productId == selectedProduct!!.id }.sumOf { it.quantityDelta }
    }
    val projectedStockUnits = currentStockUnits + parsedQty.toInt().coerceAtLeast(0)

    // Last purchase cost & variance warning (warn if differs by > 10%)
    val lastPurchaseCostInCents = selectedProduct?.costPriceInCents ?: 0L
    val costVariancePercent = if (lastPurchaseCostInCents > 0L && unitCostInCents > 0L) {
        ((unitCostInCents - lastPurchaseCostInCents).toDouble() / lastPurchaseCostInCents.toDouble()) * 100.0
    } else {
        0.0
    }
    val hasSignificantCostVariance = kotlin.math.abs(costVariancePercent) >= 10.0 && hasCostEntered && lastPurchaseCostInCents > 0L

    // Validation checks
    val isItemValid = selectedProduct != null
    val isQtyValid = parsedQty > 0.0
    val isCostValid = isFreeStock || parsedUnitCost > 0.0
    val canSubmit = isItemValid && isQtyValid && isCostValid && !isSubmitting

    val disabledExplanation = when {
        !isItemValid -> if (isNepali) "अगाडि बढ्न सामान छान्नुहोस्" else "Select an inventory item to continue"
        !isQtyValid -> if (isNepali) "० भन्दा बढी मान्य दाखिला परिमाण प्रविष्ट गर्नुहोस्" else "Enter a valid received quantity greater than 0"
        !isCostValid -> if (isNepali) "लागत रकम प्रविष्ट गर्नुहोस् वा 'नि:शुल्क / नमुना स्टक' छान्नुहोस्" else "Enter cost or check 'Free / sample stock'"
        else -> null
    }

    val isFormDirty = selectedProduct != null || quantityText != "10" || parsedUnitCost > 0.0

    fun handleDismissAttempt() {
        if (isFormDirty && !isSubmitting) {
            showDiscardConfirm = true
        } else {
            onDismiss()
        }
    }

    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.sku.contains(searchQuery, ignoreCase = true) ||
                    it.barcode.contains(searchQuery, ignoreCase = true)
        }
    }

    // Hardware Keyboard Wedge Barcode Detector (USB / Bluetooth scanner emitting rapid keypresses)
    var keyStrokeBuffer by remember { mutableStateOf(StringBuilder()) }
    var lastKeyStrokeTime by remember { mutableStateOf(0L) }

    fun handleScannedBarcode(code: String, isContinuous: Boolean) {
        val clean = code.trim()
        if (clean.isBlank()) return

        if (onLookupBarcode != null) {
            onLookupBarcode(clean) { res ->
                if (res.found && res.item != null) {
                    val wasSameProduct = selectedProduct?.id == res.item.id
                    selectedProduct = res.item
                    matchedPackQty = res.packQty
                    matchedBarcodeStr = clean
                    isSelectingProduct = false
                    searchQuery = ""

                    if (isContinuous && wasSameProduct) {
                        val cur = quantityText.toDoubleOrNull() ?: 0.0
                        val next = cur + res.packQty
                        quantityText = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                        successToast = "Added +${res.packQty} ${res.item.unit} to ${res.item.name}"
                    } else {
                        quantityText = res.packQty.toString()
                        unitCostText = String.format(Locale.US, "%.2f", res.item.costPriceInCents / 100.0)
                        successToast = "Matched: ${res.item.name} (${res.packQty}x pack)"
                    }
                } else {
                    notFoundBarcode = clean
                    showNotFoundDialog = true
                }
            }
        } else {
            // Local fallback match
            val matched = products.find { it.barcode.equals(clean, ignoreCase = true) || it.sku.equals(clean, ignoreCase = true) }
            if (matched != null) {
                val wasSameProduct = selectedProduct?.id == matched.id
                selectedProduct = matched
                matchedPackQty = 1
                matchedBarcodeStr = clean
                isSelectingProduct = false
                searchQuery = ""

                if (isContinuous && wasSameProduct) {
                    val cur = quantityText.toDoubleOrNull() ?: 0.0
                    val next = cur + 1.0
                    quantityText = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                    successToast = "Added +1 to ${matched.name}"
                } else {
                    unitCostText = String.format(Locale.US, "%.2f", matched.costPriceInCents / 100.0)
                    successToast = "Matched: ${matched.name}"
                }
            } else {
                notFoundBarcode = clean
                showNotFoundDialog = true
            }
        }
    }

    fun submitRecord(isContinuousNext: Boolean) {
        val prod = selectedProduct ?: return
        if (!canSubmit) return

        isSubmitting = true
        errorMsg = null

        val milliUnits = (parsedQty * 1000.0).toLong()
        val costCents = if (isFreeStock) 0L else unitCostInCents

        val compiledNotes = buildString {
            if (vatBillNumber.isNotBlank()) append("VAT/PAN: ${vatBillNumber.trim()} | ")
            if (notes.isNotBlank()) append(notes.trim())
        }.trim().removeSuffix("|").trim()

        if (isContinuousNext) {
            if (onSaveAndNext != null) {
                onSaveAndNext(
                    prod.id,
                    milliUnits,
                    costCents,
                    supplierName.trim(),
                    invoiceRef.trim(),
                    batchNumber.trim(),
                    expiryDate.trim(),
                    compiledNotes
                )
            } else {
                onSubmit(
                    prod.id,
                    milliUnits,
                    costCents,
                    supplierName.trim(),
                    invoiceRef.trim(),
                    batchNumber.trim(),
                    expiryDate.trim(),
                    compiledNotes
                )
            }
            // Reset for next intake item
            successToast = "Saved ${prod.name}! Ready for next item."
            selectedProduct = null
            isSelectingProduct = true
            searchQuery = ""
            quantityText = "10"
            unitCostText = "0.00"
            totalInvoiceCostText = ""
            isFreeStock = false
            matchedPackQty = 1
            matchedBarcodeStr = ""
            isSubmitting = false
        } else {
            onSubmit(
                prod.id,
                milliUnits,
                costCents,
                supplierName.trim(),
                invoiceRef.trim(),
                batchNumber.trim(),
                expiryDate.trim(),
                compiledNotes
            )
        }
    }

    Dialog(
        onDismissRequest = { handleDismissAttempt() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 18.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 680.dp)
                .heightIn(max = 760.dp)
                .imePadding()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val now = System.currentTimeMillis()
                    if (now - lastKeyStrokeTime > 80L) {
                        keyStrokeBuffer.clear()
                    }
                    lastKeyStrokeTime = now

                    val native = keyEvent.nativeKeyEvent
                    if (keyEvent.key == Key.Enter || native.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                        if (keyStrokeBuffer.length >= 6) {
                            val code = keyStrokeBuffer.toString()
                            keyStrokeBuffer.clear()
                            handleScannedBarcode(code, isContinuous = false)
                            return@onKeyEvent true
                        }
                    } else {
                        val ch = native.unicodeChar.toChar()
                        if (ch.isLetterOrDigit() || ch == '-' || ch == '_') {
                            keyStrokeBuffer.append(ch)
                        }
                    }
                    false
                }
                .testTag("stock_in_form_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // 1. Header (Concise Nepali/English title & subtitle)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PosEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = PosEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isNepali) "नयाँ स्टक दाखिला" else "Receive Inbound Stock",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = PosEmerald.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (isNepali) "स्टक दाखिला" else "स्टक इन",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PosEmerald,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isNepali) "गोदाम / पसलको मौज्दातमा सामान थप्नुहोस्" else "Add stock to inventory",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { handleDismissAttempt() },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isNepali) "बन्द गर्नुहोस्" else "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Success Toast Banner (Continuous intake notification)
                AnimatedVisibility(visible = successToast != null) {
                    successToast?.let { toast ->
                        Surface(
                            color = PosEmerald.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(toast, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = PosEmerald)
                                }
                                IconButton(onClick = { successToast = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp), tint = PosEmerald)
                                }
                            }
                        }
                    }
                }

                // 2. Primary Scrollable Flow (All critical actions on one screen)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // --- Target Inventory Item with Camera Scan Button ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNepali) "१. लक्षित सामान" else "1. Target Inventory Item",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedProduct == null) {
                                PosCompactPillButton(
                                    text = if (isNepali) "+ नयाँ सामान" else "+ New Product",
                                    icon = Icons.Default.Add,
                                    gradient = PosButtonGradients.Emerald,
                                    height = 28.dp,
                                    onClick = {
                                        notFoundBarcode = searchQuery.trim()
                                        showQuickCreateDialog = true
                                    }
                                )
                            }
                            Text(
                                text = if (isNepali) "बारकोड स्क्यान ➔" else "Scan Barcode ➔",
                                style = MaterialTheme.typography.labelSmall,
                                color = PosEmerald,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { showCameraScannerSheet = true }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    if (selectedProduct == null || isSelectingProduct) {
                        // Search / Scan Input field with Barcode Scan Button at Right
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                isSelectingProduct = true
                            },
                            placeholder = { Text(if (isNepali) "सामानको नाम, SKU वा बारकोड खोज्नुहोस्..." else "Search or scan item (name, SKU, barcode)...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            isSelectingProduct = false
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(onClick = { showCameraScannerSheet = true }) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Scan with Camera",
                                            tint = PosEmerald
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stock_in_product_search_input")
                        )

                        // Product Options Dropdown / List
                        if (isSelectingProduct && filteredProducts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    filteredProducts.forEach { prod ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedProduct = prod
                                                    matchedBarcodeStr = prod.barcode
                                                    matchedPackQty = 1
                                                    unitCostText = String.format(Locale.US, "%.2f", prod.costPriceInCents / 100.0)
                                                    totalInvoiceCostText = ""
                                                    isSelectingProduct = false
                                                    searchQuery = ""
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(prod.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "SKU: ${prod.sku} • Barcode: ${prod.barcode.ifBlank { "N/A" }}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        CartCalculator.formatMoney(prod.costPriceInCents, currencySymbol),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text("Sell: ${CartCalculator.formatMoney(prod.sellingPriceInCents, currencySymbol)}", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }

                                    // Option to create as new product even if partial matches exist
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PosEmerald.copy(alpha = 0.08f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PosEmerald.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                notFoundBarcode = searchQuery.trim()
                                                showQuickCreateDialog = true
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (searchQuery.isNotBlank()) "Register '$searchQuery' as New Product" else "+ Create & Register New Product",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = PosEmerald
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Product Not Found Prompt Card (when searched query/barcode doesn't match any existing item)
                        if (isSelectingProduct && filteredProducts.isEmpty() && searchQuery.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isNepali) "मौजूदातमा सामान फेला परेन" else "Item Not Found in Inventory",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = if (isNepali) "'$searchQuery' नामको कुनै सामान छैन। यस सामानको स्टक दाखिला गर्न नाम र बिक्री मूल्य प्रविष्ट गर्नुहोस्।" else "No existing product matches '$searchQuery'. To post stock for this item, enter its Product Name, Selling Price, and details.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    PosGradientButton(
                                        text = if (isNepali) "नाम प्रविष्ट गरी सामान दर्ता गर्नुहोस्" else "Enter Name & Register Product",
                                        subtitle = if (isNepali) "बारकोड / कोड: $searchQuery" else "Barcode / Code: $searchQuery",
                                        icon = Icons.Default.Add,
                                        gradient = PosButtonGradients.Emerald,
                                        height = 42.dp,
                                        onClick = {
                                            notFoundBarcode = searchQuery.trim()
                                            showQuickCreateDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Selected Product Summary Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = selectedProduct!!.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "SKU: ${selectedProduct!!.sku}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { showCameraScannerSheet = true }) {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = PosEmerald)
                                        }
                                        OutlinedButton(
                                            onClick = { isSelectingProduct = true },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Change", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                // Matched Barcode & Pack Qty Chip
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp), tint = PosPrimary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val displayBarcode = if (matchedBarcodeStr.isNotBlank()) matchedBarcodeStr else (selectedProduct?.barcode?.ifBlank { "No Barcode" } ?: "No Barcode")
                                            Text(
                                                text = "Barcode: $displayBarcode · ${matchedPackQty} unit${if (matchedPackQty > 1) "s (Carton)" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Generate Internal Code128 Label shortcut
                                    Text(
                                        text = "Print Label ➔",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PosPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { showInternalLabelDialog = true }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- 50/50 Equal Grid for Quantity & Cost with Stepper & Quick Chips ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left 50%: Quantity with Stepper and Quick Chips
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isNepali) "आउने मात्रा" else "Inbound Quantity",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = selectedProduct?.unit ?: "units",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // − [ 10 ] + Stepper Component
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val cur = quantityText.toDoubleOrNull() ?: 1.0
                                        val next = (cur - 1.0).coerceAtLeast(1.0)
                                        quantityText = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                                    },
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.primary)
                                }

                                BasicTextField(
                                    value = quantityText,
                                    onValueChange = { quantityText = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("stock_in_quantity_input")
                                )

                                IconButton(
                                    onClick = {
                                        val cur = quantityText.toDoubleOrNull() ?: 0.0
                                        val next = cur + 1.0
                                        quantityText = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                                    },
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = PosEmerald)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 4 Quick Chips (+1, +5, +10, +50) - Horizontally scrollable, never wrap
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("+1", "+5", "+10", "+50").forEach { step ->
                                    val delta = step.replace("+", "").toDoubleOrNull() ?: 0.0
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable {
                                            val cur = quantityText.toDoubleOrNull() ?: 0.0
                                            val next = cur + delta
                                            quantityText = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                                        }
                                    ) {
                                        Text(
                                            text = step,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Right 50%: Cost with Toggle Mode (Unit vs Total Invoice) & Free Stock Toggle
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isNepali) (if (isTotalCostMode) "इन्भवॉयस जुम्ला" else "प्रति इकाई लागत") else (if (isTotalCostMode) "Invoice Total" else "Unit Cost"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isNepali) (if (isTotalCostMode) "प्रति इकाई →" else "जुम्ला →") else (if (isTotalCostMode) "Per Unit ➔" else "Total Inv ➔"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        if (!isTotalCostMode) {
                                            val computedTotal = parsedQty * (unitCostText.toDoubleOrNull() ?: 0.0)
                                            totalInvoiceCostText = if (computedTotal > 0) String.format(Locale.US, "%.2f", computedTotal) else ""
                                            isTotalCostMode = true
                                        } else {
                                            isTotalCostMode = false
                                        }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = if (isTotalCostMode) totalInvoiceCostText else unitCostText,
                                onValueChange = { newVal ->
                                    if (isTotalCostMode) {
                                        totalInvoiceCostText = newVal
                                        val totalVal = newVal.toDoubleOrNull() ?: 0.0
                                        if (parsedQty > 0.0) {
                                            unitCostText = String.format(Locale.US, "%.2f", totalVal / parsedQty)
                                        }
                                    } else {
                                        unitCostText = newVal
                                    }
                                },
                                leadingIcon = {
                                    Text(
                                        text = currencySymbol,
                                        fontWeight = FontWeight.Bold,
                                        color = PosPrimary,
                                        modifier = Modifier.padding(start = 10.dp)
                                    )
                                },
                                enabled = !isFreeStock,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("stock_in_cost_input")
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Free / sample stock toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        isFreeStock = !isFreeStock
                                        if (isFreeStock) {
                                            unitCostText = "0.00"
                                            totalInvoiceCostText = "0.00"
                                        } else if (selectedProduct != null) {
                                            unitCostText = String.format(Locale.US, "%.2f", selectedProduct!!.costPriceInCents / 100.0)
                                        }
                                    }
                            ) {
                                Checkbox(
                                    checked = isFreeStock,
                                    onCheckedChange = { checked ->
                                        isFreeStock = checked
                                        if (checked) {
                                            unitCostText = "0.00"
                                            totalInvoiceCostText = "0.00"
                                        } else if (selectedProduct != null) {
                                            unitCostText = String.format(Locale.US, "%.2f", selectedProduct!!.costPriceInCents / 100.0)
                                        }
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isNepali) "निःशुल्क / नमुना स्टक" else "Free / sample stock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isFreeStock) PosEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- Real-Time Sanity Checking (Stock impact & Cost variance & Margin) ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Current Stock → New Stock sanity check
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isNepali) "अनुमानित स्टक मौजूदात:" else "Projected Stock Balance:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = "$currentStockUnits ➔ $projectedStockUnits ${selectedProduct?.unit ?: "units"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Gross Margin Indicator (Only shown when cost > 0)
                        if (hasCostEntered && retailPriceInCents > 0L) {
                            val marginColor = when {
                                marginPercent >= 25.0 -> PosEmerald
                                marginPercent >= 10.0 -> Color(0xFFD97706) // Amber
                                else -> PosRose
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isNepali) "बिक्री मूल्य र मार्जिन:" else "Selling & Margin:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = marginColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isNepali) "बेच: ${CartCalculator.formatMoney(retailPriceInCents, currencySymbol)} · मार्जिन ${String.format(Locale.US, "%.1f", marginPercent)}%" else "Sell ${CartCalculator.formatMoney(retailPriceInCents, currencySymbol)} · Margin ${String.format(Locale.US, "%.1f", marginPercent)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = marginColor
                                    )
                                }
                            }
                        }

                        // Last purchase cost hint & >10% Variance Warning
                        if (lastPurchaseCostInCents > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isNepali) "अन्तिम खरिद लागत:" else "Last purchase cost:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CartCalculator.formatMoney(lastPurchaseCostInCents, currencySymbol),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (hasSignificantCostVariance) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isNepali) "⚠ लागत ${if (costVariancePercent > 0) "+" else ""}${String.format(Locale.US, "%.0f", costVariancePercent)}% अन्तिम खरिदभन्दा फरक (${CartCalculator.formatMoney(lastPurchaseCostInCents, currencySymbol)})" else "⚠ Cost differs by ${if (costVariancePercent > 0) "+" else ""}${String.format(Locale.US, "%.0f", costVariancePercent)}% compared to last purchase (${CartCalculator.formatMoney(lastPurchaseCostInCents, currencySymbol)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- High-Contrast, Bold Nepali/Rs Batch Total Display ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PosEmerald.copy(alpha = 0.10f)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PosEmerald.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isNepali) "गणना गरिएको आउने माल जम्माशो" else "Calculated Inbound Batch Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isFreeStock) (if (isNepali) "$currencySymbol 0.00 (निःशुल्क)" else "$currencySymbol 0.00 (Free Sample)") else CartCalculator.formatMoney(totalCostValueInCents, currencySymbol),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46)
                                )
                            }

                            if (!isSupplierSectionExpanded) {
                                OutlinedButton(
                                    onClick = { isSupplierSectionExpanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isNepali) "पीओ/भ्याट जोड्नुहोस्" else "Add PO/VAT", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // --- Collapsible Section for Supplier, VAT Bill & Batch Details ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSupplierSectionExpanded = !isSupplierSectionExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isNepali) "आपूर्तिकर्ता र ब्याच म्याद (वैकल्पिक)" else "Supplier & Batch Details (Optional)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (vatBillNumber.isNotBlank() || supplierName != "Direct Wholesale Supplier") {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = PosEmerald.copy(alpha = 0.15f),
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = if (isNepali) "भरिसकेको" else "Configured",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PosEmerald,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand",
                                        modifier = Modifier.graphicsLayer(rotationZ = if (isSupplierSectionExpanded) 180f else 0f)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isSupplierSectionExpanded) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = supplierName,
                                            onValueChange = { supplierName = it },
                                            label = { Text(if (isNepali) "आपूर्तिकर्ता / विक्रेताको नाम" else "Vendor / Supplier Name") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).testTag("supplier_name_input")
                                        )

                                        OutlinedTextField(
                                            value = vatBillNumber,
                                            onValueChange = { vatBillNumber = it },
                                            label = { Text(if (isNepali) "भ्याट / प्यान बिल नं." else "VAT / PAN Bill #") },
                                            placeholder = { Text(if (isNepali) "उदा.: 102938475" else "e.g. 102938475") },
                                            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).testTag("vat_bill_input")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = invoiceRef,
                                            onValueChange = { invoiceRef = it },
                                            label = { Text(if (isNepali) "इन्भवॉयस / पीओ नं." else "Invoice / PO Ref #") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).testTag("invoice_ref_input")
                                        )

                                        OutlinedTextField(
                                            value = batchNumber,
                                            onValueChange = { batchNumber = it },
                                            label = { Text(if (isNepali) "ब्याच / लट नं." else "Batch / Lot #") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = expiryDate,
                                        onValueChange = { expiryDate = it },
                                        label = { Text(if (isNepali) "ब्याच मिति (YYYY-MM-DD)" else "Batch Expiry Date (YYYY-MM-DD)") },
                                        placeholder = { Text("YYYY-MM-DD") },
                                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text(if (isNepali) "माल प्राप्ति टिप्पणी" else "Receiving Inspection Notes") },
                                        placeholder = { Text(if (isNepali) "उदा.: राम्रो अवस्थामा प्राप्त, सिल सुरक्षित" else "e.g. Received in good condition, seal intact") },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Error Message Banner
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PosRose.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PosRose.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = PosRose, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = PosRose)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // 3. Pinned Bottom Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        if (disabledExplanation != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = disabledExplanation,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (!isItemValid) {
                                        Text(
                                            text = if (isNepali) "+ नयाँ सामान" else "+ New Item",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PosEmerald,
                                            modifier = Modifier.clickable {
                                                notFoundBarcode = searchQuery.trim()
                                                showQuickCreateDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PosOutlineButton(
                                text = if (isNepali) "रद्द गर्नुहोस्" else "Cancel",
                                onClick = { handleDismissAttempt() },
                                shape = RoundedCornerShape(12.dp),
                                height = 46.dp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PosOutlineButton(
                                    text = if (isNepali) "सेभ र अर्को" else "Save & Next",
                                    icon = Icons.Default.Add,
                                    onClick = { submitRecord(isContinuousNext = true) },
                                    enabled = canSubmit,
                                    shape = RoundedCornerShape(12.dp),
                                    height = 46.dp,
                                    testTag = "save_and_next_stock_in_btn"
                                )

                                val totalLabel = if (isFreeStock) "$currencySymbol 0.00" else CartCalculator.formatMoney(totalCostValueInCents, currencySymbol)
                                PosGradientButton(
                                    text = if (isNepali) "स्टक दाखिला गर्नुहोस्" else "Post Stock-In",
                                    subtitle = if (isNepali) "जम्मा: $totalLabel" else "Total: $totalLabel",
                                    onClick = { submitRecord(isContinuousNext = false) },
                                    enabled = canSubmit,
                                    isLoading = isSubmitting,
                                    gradient = PosButtonGradients.Emerald,
                                    icon = Icons.Default.CheckCircle,
                                    iconBadge = true,
                                    shape = RoundedCornerShape(14.dp),
                                    height = 48.dp,
                                    testTag = "post_stock_in_button"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Discard Confirmation Dialog ---
    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(if (isNepali) "स्टक दाखिला रद्द गर्नुहोस्?" else "Discard Stock-In?", fontWeight = FontWeight.Bold) },
            text = { Text(if (isNepali) "तपाईंले स्टक मालसमानको विवरण भर्नुभएको छो। सत्तै फेर्नुहोन्छ ?" else "You have entered stock intake details. Are you sure you want to discard your changes?") },
            confirmButton = {
                PosDestructiveButton(
                    text = if (isNepali) "परिवर्तन फ्यांक्नुहोस्" else "Discard Changes",
                    onClick = {
                        showDiscardConfirm = false
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            },
            dismissButton = {
                PosOutlineButton(
                    text = if (isNepali) "सम्पादन जारी राख्नुहोस्" else "Keep Editing",
                    onClick = { showDiscardConfirm = false },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        )
    }

    // --- Half-Height Camera Scanner Sheet ---
    if (showCameraScannerSheet) {
        BarcodeScannerSheet(
            isContinuousModeDefault = false,
            onDismiss = { showCameraScannerSheet = false },
            onBarcodeDetected = { scannedCode, isContinuous ->
                handleScannedBarcode(scannedCode, isContinuous)
            },
            onUndoLastScan = {
                // Revert last scan by decrementing packQty
                val cur = quantityText.toDoubleOrNull() ?: 1.0
                val next = (cur - matchedPackQty).coerceAtLeast(1.0)
                quantityText = if (next % 1.0 == 0.0) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
            }
        )
    }

    // --- Scan Not Found Action Dialog (Create New / Link to Existing) ---
    if (showNotFoundDialog) {
        BarcodeNotFoundDialog(
            scannedBarcode = notFoundBarcode,
            onDismiss = { showNotFoundDialog = false },
            onCreateNewItem = { barcode ->
                showQuickCreateDialog = true
            },
            onLinkToExisting = { barcode ->
                showLinkDialog = true
            }
        )
    }

    // --- Link Barcode Dialog ---
    if (showLinkDialog) {
        LinkBarcodeDialog(
            barcodeToLink = notFoundBarcode,
            products = products,
            currencySymbol = currencySymbol,
            onDismiss = { showLinkDialog = false },
            onConfirmLink = { product, packQty ->
                selectedProduct = product
                matchedPackQty = packQty
                matchedBarcodeStr = notFoundBarcode
                isSelectingProduct = false
                quantityText = packQty.toString()
                unitCostText = String.format(Locale.US, "%.2f", product.costPriceInCents / 100.0)
                if (onLinkBarcode != null) {
                    onLinkBarcode(product.id, notFoundBarcode, packQty) {}
                }
                successToast = "Linked barcode '$notFoundBarcode' (${packQty}x) to ${product.name}"
            }
        )
    }

    // --- Quick Create Product Dialog ---
    if (showQuickCreateDialog) {
        val calculatedUnitCost = if ((unitCostText.toDoubleOrNull() ?: 0.0) > 0.0) {
            unitCostText
        } else if (parsedQty > 0.0 && (totalInvoiceCostText.toDoubleOrNull() ?: 0.0) > 0.0) {
            String.format(Locale.US, "%.2f", (totalInvoiceCostText.toDouble() / parsedQty))
        } else "0.00"

        QuickCreateProductDialog(
            prefilledBarcode = notFoundBarcode.ifBlank { searchQuery.trim() },
            prefilledCostPrice = calculatedUnitCost,
            currencySymbol = currencySymbol,
            onDismiss = { showQuickCreateDialog = false },
            onProductCreated = { newProd, packQty ->
                selectedProduct = newProd
                matchedPackQty = packQty
                matchedBarcodeStr = newProd.barcode
                isSelectingProduct = false
                searchQuery = ""
                // Preserve user-entered quantity if customized (e.g. 55 in screenshot)
                if (quantityText.isBlank() || quantityText == "10") {
                    quantityText = packQty.toString()
                }
                unitCostText = String.format(Locale.US, "%.2f", newProd.costPriceInCents / 100.0)

                if (onCreateProductWithBarcode != null) {
                    onCreateProductWithBarcode(
                        newProd.name,
                        newProd.sku,
                        newProd.barcode,
                        newProd.costPriceInCents,
                        newProd.sellingPriceInCents,
                        newProd.unit,
                        packQty
                    ) {}
                }
                successToast = "Created and selected '${newProd.name}'!"
            }
        )
    }

    // --- Internal Code128 Label Sticker Dialog ---
    if (showInternalLabelDialog && selectedProduct != null) {
        InternalBarcodeLabelDialog(
            product = selectedProduct!!,
            onDismiss = { showInternalLabelDialog = false }
        )
    }
}
