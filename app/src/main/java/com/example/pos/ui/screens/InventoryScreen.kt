package com.example.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.pos.ui.components.PosButtonGradients
import com.example.pos.ui.components.PosCompactPillButton
import com.example.pos.ui.components.PosGradientButton
import com.example.pos.ui.components.PosOutlineButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pos.data.local.ProductEntity
import com.example.pos.data.model.MovementType
import com.example.pos.domain.CartCalculator
import com.example.pos.ui.PosViewModel
import com.example.pos.ui.components.ProductDetailDialog
import com.example.pos.ui.components.StockInFormDialog
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InventoryScreen(viewModel: PosViewModel) {
    val strings = LocalPosStrings.current
    val products by viewModel.products.collectAsState()
    val movements by viewModel.stockMovements.collectAsState()
    val org by viewModel.organization.collectAsState()
    val currency = org?.currencySymbol ?: "$"

    var selectedTab by remember { mutableStateOf(0) } // 0: Products Stock, 1: Ledger History
    var selectedProductForAdjustment by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedProductForStockIn by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedProductForDetail by remember { mutableStateOf<ProductEntity?>(null) }
    var showReceiveDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("inventory_screen")
    ) {
        // Top Actions Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = strings.branchInventory,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (strings is NepaliStrings) "दाखिला खाता र वास्तविक मौज्दात स्थिति" else "Inbound stock-in ledger & real-time stock balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            PosGradientButton(
                text = strings.receiveStock,
                icon = Icons.Default.Add,
                iconBadge = true,
                gradient = PosButtonGradients.Emerald,
                shape = RoundedCornerShape(12.dp),
                height = 44.dp,
                testTag = "receive_stock_button",
                onClick = {
                    selectedProductForStockIn = null
                    showReceiveDialog = true
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(if (strings is NepaliStrings) "मौज्दात सूची (${products.size})" else "Stock On Hand (${products.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(if (strings is NepaliStrings) "मौज्दात विवरण (${movements.size})" else "Movement Ledger (${movements.size})") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (strings is NepaliStrings) "नाम, SKU वा बारकोडबाट खोज्नुहोस्..." else "Search by name, SKU or barcode...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PosPrimary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = PosRose)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inventory_search_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        val filteredProducts = remember(products, searchQuery) {
            if (searchQuery.isBlank()) products else products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.sku.contains(searchQuery, ignoreCase = true) ||
                it.barcode.contains(searchQuery, ignoreCase = true)
            }
        }
        val filteredMovements = remember(movements, searchQuery) {
            if (searchQuery.isBlank()) movements else movements.filter { move ->
                val prod = products.find { it.id == move.productId }
                prod != null && (prod.name.contains(searchQuery, ignoreCase = true) ||
                                 prod.sku.contains(searchQuery, ignoreCase = true) ||
                                 prod.barcode.contains(searchQuery, ignoreCase = true))
            }
        }

        if (selectedTab == 0) {
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PosEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Inventory, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Products in Catalog Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Receive supplier stock to populate your inventory ledger with PO tracking and cost prices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    selectedProductForStockIn = null
                                    showReceiveDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PosEmerald),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("empty_receive_stock_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Receive First Inbound Stock")
                            }
                        }
                    }
                }
            } else {
                // Product Stock List
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val stockQty = remember(movements, product.id) {
                            movements.filter { it.productId == product.id }.sumOf { it.quantityDelta }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProductForDetail = product }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Top Row: Product Name & Live Stock Count Badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "SKU: ${product.sku}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B)
                                            )
                                            if (product.barcode.isNotBlank()) {
                                                Text(
                                                    text = "• Barcode: ${product.barcode}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF64748B),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    // Live Stock Quantity Pill Badge
                                    val (badgeBg, badgeColor, badgeText) = when {
                                        stockQty <= 0 -> Triple(
                                            Color(0xFFFEE2E2),
                                            PosRose,
                                            if (stockQty == 0) "0 ${product.unit} (Out of Stock)" else "$stockQty ${product.unit} (Negative)"
                                        )
                                        stockQty <= product.minStockAlert -> Triple(
                                            Color(0xFFFFFBEB),
                                            PosAmber,
                                            "$stockQty ${product.unit} (Low Stock)"
                                        )
                                        else -> Triple(
                                            Color(0xFFECFDF5),
                                            PosEmerald,
                                            "$stockQty ${product.unit} In Stock"
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeBg,
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                // Bottom Row: Cost/Sell Prices & Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column {
                                            Text("Cost", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            Text(
                                                CartCalculator.formatMoney(product.costPriceInCents, currency),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Slate700
                                            )
                                        }
                                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFFE2E8F0)))
                                        Column {
                                            Text("Sell", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            Text(
                                                CartCalculator.formatMoney(product.sellingPriceInCents, currency),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PosPrimary
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PosCompactPillButton(
                                            text = "Stock In",
                                            icon = Icons.Default.Add,
                                            gradient = PosButtonGradients.Emerald,
                                            height = 34.dp,
                                            testTag = "stock_in_btn_${product.sku}",
                                            onClick = {
                                                selectedProductForStockIn = product
                                                showReceiveDialog = true
                                            }
                                        )

                                        PosOutlineButton(
                                            text = "Adjust",
                                            height = 34.dp,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            testTag = "adjust_stock_btn_${product.sku}",
                                            onClick = { selectedProductForAdjustment = product }
                                        )

                                        PosOutlineButton(
                                            text = if (strings is NepaliStrings) "बारकोड" else "Barcode",
                                            icon = Icons.Default.QrCode,
                                            height = 34.dp,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            testTag = "barcode_btn_${product.sku}",
                                            onClick = { selectedProductForDetail = product }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Movement Ledger List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredMovements, key = { it.id }) { move ->
                    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(move.createdAt))
                    val isPositive = move.quantityDelta > 0
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${move.type.name} • ${move.referenceId.ifBlank { "Direct" }}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("$dateStr by ${move.createdBy.ifBlank { "Staff" }}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            }
                            Text(
                                text = "${if (isPositive) "+" else ""}${move.quantityDelta} units",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositive) PosEmerald else PosRose
                            )
                        }
                    }
                }
            }
        }
    }

    // Receive Stock-In Dialog
    if (showReceiveDialog) {
        StockInFormDialog(
            products = products,
            preSelectedProduct = selectedProductForStockIn,
            stockMovements = movements,
            currencySymbol = currency,
            onDismiss = {
                showReceiveDialog = false
                selectedProductForStockIn = null
            },
            onSubmit = { prodId, milliUnits, costCents, supplier, invoice, batch, expiry, notes ->
                viewModel.recordStockIn(
                    productId = prodId,
                    quantityMilliUnits = milliUnits,
                    unitCostInCents = costCents,
                    supplierName = supplier,
                    invoiceRef = invoice,
                    batchNumber = batch,
                    expiryDate = expiry,
                    notes = notes,
                    onSuccess = {
                        showReceiveDialog = false
                        selectedProductForStockIn = null
                    }
                )
            },
            onSaveAndNext = { prodId, milliUnits, costCents, supplier, invoice, batch, expiry, notes ->
                viewModel.recordStockIn(
                    productId = prodId,
                    quantityMilliUnits = milliUnits,
                    unitCostInCents = costCents,
                    supplierName = supplier,
                    invoiceRef = invoice,
                    batchNumber = batch,
                    expiryDate = expiry,
                    notes = notes,
                    onSuccess = {
                        selectedProductForStockIn = null
                    }
                )
            },
            onLookupBarcode = { code, onResult -> viewModel.lookupBarcode(code, onResult) },
            onLinkBarcode = { itemId, code, packQty, onSuccess -> viewModel.linkBarcodeToItem(itemId, code, packQty, onSuccess) },
            onCreateProductWithBarcode = { name, sku, barcode, cost, sell, unit, packQty, onSuccess ->
                viewModel.createProductWithBarcode(name, sku, barcode, cost, sell, unit, packQty, onSuccess)
            }
        )
    }

    // Stock Adjustment Dialog
    selectedProductForAdjustment?.let { prod ->
        var adjDeltaText by remember { mutableStateOf("-1") }
        var adjReason by remember { mutableStateOf("Damaged Goods in Transit") }

        Dialog(onDismissRequest = { selectedProductForAdjustment = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Adjust Stock: ${prod.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = adjDeltaText,
                        onValueChange = { adjDeltaText = it },
                        label = { Text("Delta Quantity (+ or -)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adjReason,
                        onValueChange = { adjReason = it },
                        label = { Text("Reason (Damage / Loss / Count Variance)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PosOutlineButton(
                            text = "Cancel",
                            shape = RoundedCornerShape(12.dp),
                            height = 44.dp,
                            onClick = { selectedProductForAdjustment = null }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        PosGradientButton(
                            text = "Apply Adjustment",
                            icon = Icons.Default.Check,
                            iconBadge = false,
                            gradient = PosButtonGradients.Amber,
                            shape = RoundedCornerShape(12.dp),
                            height = 44.dp,
                            onClick = {
                                val delta = adjDeltaText.toIntOrNull() ?: 0
                                viewModel.adjustStock(prod.id, delta, adjReason)
                                selectedProductForAdjustment = null
                            }
                        )
                    }
                }
            }
        }
    }

    // Product Detail & Printable Barcode Dialog
    selectedProductForDetail?.let { prod ->
        val stockQty = remember(movements, prod.id) {
            movements.filter { it.productId == prod.id }.sumOf { it.quantityDelta }
        }
        ProductDetailDialog(
            product = prod,
            stockQty = stockQty,
            currencySymbol = currency,
            onDismiss = { selectedProductForDetail = null }
        )
    }
}
