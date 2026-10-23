package com.example.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.pos.ui.components.PosButtonGradients
import com.example.pos.ui.components.PosCompactPillButton
import com.example.pos.ui.components.PosDestructiveButton
import com.example.pos.ui.components.PosGradientButton
import com.example.pos.ui.components.PosOutlineButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.local.CustomerEntity
import com.example.pos.data.local.ProductEntity
import com.example.pos.data.model.CartItem
import com.example.pos.data.model.StaffRole
import com.example.pos.data.model.TaxMode
import com.example.pos.domain.CartCalculator
import com.example.pos.ui.PosViewModel
import com.example.pos.ui.components.BarcodeScannerSheet
import com.example.pos.ui.components.CustomerFormDialog
import com.example.pos.ui.components.StockInFormDialog
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun PosRegisterScreen(viewModel: PosViewModel) {
    val strings = LocalPosStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val allProducts by viewModel.products.collectAsState()
    val allCustomers by viewModel.customers.collectAsState()
    val heldOrders by viewModel.heldOrders.collectAsState()
    val stockMovements by viewModel.stockMovements.collectAsState()
    val org by viewModel.organization.collectAsState()
    val currency = org?.currencySymbol ?: "$"
    val taxMode = org?.taxMode ?: TaxMode.EXCLUSIVE

    // Filter products: Only filter when search query is typed
    val searchMatchingProducts = remember(allProducts, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) {
            emptyList()
        } else {
            allProducts.filter { product ->
                product.name.contains(uiState.searchQuery, ignoreCase = true) ||
                        product.sku.contains(uiState.searchQuery, ignoreCase = true) ||
                        product.barcode.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }

    var showStockInDialog by remember { mutableStateOf(false) }
    var showCustomerPickerDialog by remember { mutableStateOf(false) }
    var showCustomerFormDialog by remember { mutableStateOf(false) }
    var showBarcodeScannerSheet by remember { mutableStateOf(false) }
    var showVoidCartConfirmDialog by remember { mutableStateOf(false) }

    val summary = remember(uiState.cartItems, uiState.orderDiscountPercent, taxMode) {
        CartCalculator.calculateSummary(uiState.cartItems, uiState.orderDiscountPercent, taxMode)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("pos_register_screen")) {
        val isTablet = this.maxWidth >= 760.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(12.dp)
        ) {
            // ==========================================
            // TOP BAR: SCANNER & SEARCH ACTION HEADER
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = strings.scanOrSearchPrompt,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = PosPrimary
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = PosRose)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("catalog_search_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Hardware Scanner Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PosEmerald.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosEmerald.copy(alpha = 0.3f)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gun Ready",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PosEmerald,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Camera Barcode Scanner Trigger Button
                IconButton(
                    onClick = { showBarcodeScannerSheet = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .testTag("barcode_scanner_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = strings.scanBarcode,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Parked Orders Counter Badge Button
                IconButton(
                    onClick = { viewModel.openParkedOrdersDialog() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF3C7))
                        .testTag("parked_orders_badge_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (heldOrders.isNotEmpty()) {
                                Badge(containerColor = Color(0xFFF59E0B)) { Text("${heldOrders.size}") }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Parked Orders",
                            tint = Color(0xFFB45309)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val isNepali = strings is NepaliStrings
            val currentSession by viewModel.currentSession.collectAsState()
            val canPerformStockIn = currentSession.role == StaffRole.OWNER ||
                currentSession.role == StaffRole.ADMIN ||
                currentSession.role == StaffRole.MANAGER ||
                currentSession.role == StaffRole.STOREKEEPER

            // Quick Operator Action Bar: Customer Attach & Stock In
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val customer = uiState.selectedCustomer
                if (customer != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attached_customer_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PosPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (customer.currentDueInCents != 0L) {
                                        val isDue = customer.currentDueInCents > 0L
                                        Text(
                                            text = if (isDue) {
                                                if (isNepali) "बाँकी: ${CartCalculator.formatMoney(customer.currentDueInCents, currency)}" else "Due: ${CartCalculator.formatMoney(customer.currentDueInCents, currency)}"
                                            } else {
                                                if (isNepali) "अग्रिम: ${CartCalculator.formatMoney(-customer.currentDueInCents, currency)}" else "Adv: ${CartCalculator.formatMoney(-customer.currentDueInCents, currency)}"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDue) PosRose else PosEmerald,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { viewModel.selectCustomer(null) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.PersonRemove, contentDescription = "Detach", tint = PosRose, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    PosOutlineButton(
                        text = if (isNepali) "ग्राहक छान्नुहोस्" else "Attach Customer",
                        icon = Icons.Default.Person,
                        onClick = { showCustomerPickerDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attach_customer_button")
                    )
                }

                if (canPerformStockIn) {
                    PosOutlineButton(
                        text = if (isNepali) "+ मौज्दात थप्नुहोस्" else "+ Stock In",
                        icon = Icons.Default.Inventory2,
                        onClick = { showStockInDialog = true },
                        modifier = Modifier.weight(if (customer != null) 0.6f else 1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // MAIN BODY: SEARCH RESULTS OR CART LEDGER
            // ==========================================
            if (uiState.searchQuery.isNotBlank()) {
                // 1. PRODUCT SEARCH MATCHES VIEW
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Matching Products (${searchMatchingProducts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        TextButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Text("Back to Cart Ticket", color = PosPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (searchMatchingProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No products match '${uiState.searchQuery}'", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
                                Text("Check the spelling or scan an item with your barcode gun", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("product_search_results_list")
                        ) {
                            items(searchMatchingProducts, key = { it.id }) { product ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addToCart(product)
                                            viewModel.updateSearchQuery("")
                                        }
                                        .testTag("search_result_${product.sku}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = product.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (product.barcode.isNotBlank()) {
                                                Text(
                                                    text = "Barcode: ${product.barcode}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF64748B),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "SKU: ${product.sku}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = CartCalculator.formatMoney(product.sellingPriceInCents, currency),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PosPrimary
                                                )
                                                Text(
                                                    text = "/ ${product.unit}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Button(
                                                onClick = {
                                                    viewModel.addToCart(product)
                                                    viewModel.updateSearchQuery("")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PosPrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add to Cart", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 2. ACTIVE REGISTER CART & LINE-ITEM LEDGER VIEW
                if (uiState.cartItems.isEmpty()) {
                    // Empty Ready State
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(PosPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = PosPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Register Ready for Next Customer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )

                            Text(
                                text = "Scan items using physical barcode gun / camera, or type product name above to add to ticket.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showBarcodeScannerSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PosPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Camera Scanner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                if (heldOrders.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { viewModel.openParkedOrdersDialog() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Recall Parked (${heldOrders.size})", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Non-Empty Active Transaction Ledger
                    if (isTablet) {
                        // Tablet Side-by-Side: Line-item table on Left (62%), Totals & Charge on Right (38%)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left Pane: Line Items Table with Line Void buttons
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(0.62f)
                                    .fillMaxHeight()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    RegisterTicketHeader(
                                        itemCount = uiState.cartItems.size,
                                        unitCount = uiState.cartItems.sumOf { it.quantity },
                                        onHoldTicket = { viewModel.parkCurrentCart(uiState.selectedCustomer?.name ?: "Guest") },
                                        onVoidTicket = { showVoidCartConfirmDialog = true }
                                    )

                                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 8.dp))

                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("cart_items_list"),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(uiState.cartItems, key = { it.product.id }) { item ->
                                            RegisterLineItemRow(
                                                item = item,
                                                currency = currency,
                                                onQuantityChange = { delta -> viewModel.updateCartItemQuantity(item.product.id, delta) },
                                                onVoidLine = { viewModel.voidLineItem(item.product.id) }
                                            )
                                            HorizontalDivider(color = Color(0xFFF1F5F9))
                                        }
                                    }
                                }
                            }

                            // Right Pane: Totals & Charge
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(0.38f)
                                    .fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Order Summary",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )

                                        // Discount selector
                                        Text("Order Discount:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(0.0, 5.0, 10.0, 15.0).forEach { disc ->
                                                val isSelected = uiState.orderDiscountPercent == disc
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) PosPrimary else Color(0xFFF1F5F9),
                                                    modifier = Modifier
                                                        .clickable { viewModel.setOrderDiscount(disc) }
                                                        .padding(vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (disc == 0.0) "None" else "${disc.toInt()}%",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else Slate700,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFFE2E8F0))

                                        // Financial breakdown
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(strings.subtotal, color = Color(0xFF64748B), fontSize = 13.sp)
                                            Text(CartCalculator.formatMoney(summary.subtotalInCents, currency), fontWeight = FontWeight.SemiBold)
                                        }

                                        if (summary.orderDiscountInCents > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(strings.discount, color = PosRose, fontSize = 13.sp)
                                                Text("-${CartCalculator.formatMoney(summary.orderDiscountInCents, currency)}", color = PosRose, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (summary.taxTotalInCents > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Tax (${taxMode.name})", color = Color(0xFF64748B), fontSize = 13.sp)
                                                Text(CartCalculator.formatMoney(summary.taxTotalInCents, currency), fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFFE2E8F0))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(strings.grandTotal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                                            Text(
                                                text = CartCalculator.formatMoney(summary.grandTotalInCents, currency),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = PosEmerald
                                            )
                                        }
                                    }

                                    // Charge Button
                                    PosGradientButton(
                                        text = if (isNepali) "भुक्तानी" else "CHARGE",
                                        subtitle = CartCalculator.formatMoney(summary.grandTotalInCents, currency),
                                        icon = Icons.Default.ShoppingCart,
                                        gradient = PosButtonGradients.Emerald,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkout_charge_button"),
                                        onClick = { viewModel.openCheckoutDialog() }
                                    )
                                }
                            }
                        }
                    } else {
                        // Phone Handheld Stack Layout
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                RegisterTicketHeader(
                                    itemCount = uiState.cartItems.size,
                                    unitCount = uiState.cartItems.sumOf { it.quantity },
                                    onHoldTicket = { viewModel.parkCurrentCart(uiState.selectedCustomer?.name ?: "Guest") },
                                    onVoidTicket = { showVoidCartConfirmDialog = true }
                                )

                                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 6.dp))

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cart_items_list"),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(uiState.cartItems, key = { it.product.id }) { item ->
                                        RegisterLineItemRow(
                                            item = item,
                                            currency = currency,
                                            onQuantityChange = { delta -> viewModel.updateCartItemQuantity(item.product.id, delta) },
                                            onVoidLine = { viewModel.voidLineItem(item.product.id) }
                                        )
                                        HorizontalDivider(color = Color(0xFFF1F5F9))
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 6.dp))

                                // Totals & Charge Bottom Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val totalUnits = uiState.cartItems.sumOf { it.quantity }
                                    val bottomUnitsText = if (isNepali) "$totalUnits वटा सामान" else if (totalUnits == 1) "1 item" else "$totalUnits items"
                                    Column {
                                        Text(bottomUnitsText, fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text(
                                            text = CartCalculator.formatMoney(summary.grandTotalInCents, currency),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PosEmerald
                                        )
                                    }

                                    PosGradientButton(
                                        text = if (isNepali) "भुक्तानी" else "Charge",
                                        icon = Icons.Default.ShoppingCart,
                                        gradient = PosButtonGradients.Emerald,
                                        modifier = Modifier.testTag("checkout_charge_button"),
                                        onClick = { viewModel.openCheckoutDialog() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // DIALOGS & OVERLAYS
        // ==========================================

        // Void Entire Ticket Confirmation Dialog
        if (showVoidCartConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showVoidCartConfirmDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = PosRose) },
                title = { Text(strings.voidAll) },
                text = { Text("Are you sure you want to void this entire ticket? All scanned items will be cleared.") },
                confirmButton = {
                    PosDestructiveButton(
                        text = "Confirm Void",
                        icon = Icons.Default.Delete,
                        onClick = {
                            viewModel.clearCart()
                            showVoidCartConfirmDialog = false
                        }
                    )
                },
                dismissButton = {
                    PosOutlineButton(
                        text = "Cancel",
                        onClick = { showVoidCartConfirmDialog = false }
                    )
                }
            )
        }

        // Countertop Stock-In Dialog
        if (showStockInDialog) {
            StockInFormDialog(
                products = allProducts,
                stockMovements = stockMovements,
                currencySymbol = currency,
                onDismiss = { showStockInDialog = false },
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
                        onSuccess = { showStockInDialog = false }
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
                        onSuccess = { /* Dialog stays open for next item */ }
                    )
                },
                onLookupBarcode = { code, onResult -> viewModel.lookupBarcode(code, onResult) },
                onLinkBarcode = { itemId, code, packQty, onSuccess -> viewModel.linkBarcodeToItem(itemId, code, packQty, onSuccess) },
                onCreateProductWithBarcode = { name, sku, barcode, cost, sell, unit, packQty, onSuccess ->
                    viewModel.createProductWithBarcode(name, sku, barcode, cost, sell, unit, packQty, onSuccess)
                }
            )
        }

        // Camera Barcode Scanner Sheet for Cart
        if (showBarcodeScannerSheet) {
            @OptIn(androidx.camera.core.ExperimentalGetImage::class)
            BarcodeScannerSheet(
                isContinuousModeDefault = true,
                onDismiss = { showBarcodeScannerSheet = false },
                onBarcodeDetected = { scannedCode, _ ->
                    viewModel.handleScanToCart(scannedCode)
                }
            )
        }

        // Countertop Customer Form Dialog
        if (showCustomerFormDialog) {
            CustomerFormDialog(
                currencySymbol = currency,
                onDismiss = { showCustomerFormDialog = false },
                onSubmit = { name, phone, email, creditLimit, termsDays, graceDays, notes, openingBal, openingType, asOfDate ->
                    viewModel.createCustomer(
                        name = name,
                        phone = phone,
                        email = email,
                        creditLimitInCents = creditLimit,
                        paymentTermsDays = termsDays,
                        gracePeriodDays = graceDays,
                        notes = notes,
                        openingBalanceInCents = openingBal,
                        openingBalanceType = openingType,
                        asOfDate = asOfDate,
                        autoSelectForCart = true,
                        onSuccess = { showCustomerFormDialog = false }
                    )
                }
            )
        }

        // Quick Customer Picker Modal
        if (showCustomerPickerDialog) {
            CustomerPickerDialog(
                customers = allCustomers,
                currency = currency,
                onDismiss = { showCustomerPickerDialog = false },
                onSelectCustomer = { cust ->
                    viewModel.selectCustomer(cust)
                    showCustomerPickerDialog = false
                },
                onAddNewCustomer = {
                    showCustomerPickerDialog = false
                    showCustomerFormDialog = true
                }
            )
        }
    }
}

@Composable
private fun RegisterTicketHeader(
    itemCount: Int,
    unitCount: Int,
    onHoldTicket: () -> Unit,
    onVoidTicket: () -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    val itemLabel = if (isNepali) "$itemCount सामान" else if (itemCount == 1) "1 item" else "$itemCount items"
    val unitLabel = if (isNepali) "$unitCount वटा" else if (unitCount == 1) "1 unit" else "$unitCount units"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = if (isNepali) "बिल कार्ट ($itemLabel, $unitLabel)" else "Cart Ticket ($itemLabel, $unitLabel)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isNepali) "स्क्यान गरिएका सामानहरू बिलिङका लागि तयार छन्" else "Scanned products ready for billing",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosOutlineButton(
                text = if (isNepali) "होल्ड" else "Hold",
                icon = Icons.Default.PauseCircle,
                contentColor = Color(0xFFD97706),
                modifier = Modifier.testTag("park_order_button"),
                onClick = onHoldTicket
            )

            PosDestructiveButton(
                text = if (isNepali) "रद्द" else strings.voidAll,
                icon = Icons.Default.Delete,
                modifier = Modifier.testTag("clear_cart_button"),
                onClick = onVoidTicket
            )
        }
    }
}

@Composable
private fun RegisterLineItemRow(
    item: CartItem,
    currency: String,
    onQuantityChange: (Int) -> Unit,
    onVoidLine: () -> Unit
) {
    val strings = LocalPosStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("cart_item_${item.product.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Product Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.product.barcode.isNotBlank()) {
                    Text(
                        text = "${item.product.barcode} • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
                Text(
                    text = "${CartCalculator.formatMoney(item.unitPriceInCents, currency)} / ${item.product.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Stepper: [-] Qty [+]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .clickable { onQuantityChange(-1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp), tint = Slate700)
            }

            Text(
                text = "${item.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Slate900,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .clickable { onQuantityChange(1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp), tint = Slate700)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Line Total
        Text(
            text = CartCalculator.formatMoney(item.totalInCents, currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Slate900
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Distinct LINE VOID Button
        PosDestructiveButton(
            text = strings.lineVoid,
            icon = Icons.Default.Delete,
            modifier = Modifier.testTag("void_line_${item.product.id}"),
            onClick = onVoidLine
        )
    }
}

@Composable
fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onSelectCustomer: (CustomerEntity) -> Unit,
    onAddNewCustomer: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers else {
            customers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Select Customer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Attach to current ticket",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    PosCompactPillButton(
                        text = "+ New",
                        icon = Icons.Default.PersonAdd,
                        gradient = PosButtonGradients.Emerald,
                        onClick = onAddNewCustomer
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or phone...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PosPrimary) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // List
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("No customers found", fontWeight = FontWeight.Bold, color = Slate900, fontSize = 13.sp)
                            Text("Click '+ New' to create a customer", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { cust ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCustomer(cust) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(PosPrimary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cust.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = PosPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(cust.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Slate900)
                                            if (cust.phone.isNotBlank()) {
                                                Text(cust.phone, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                                            }
                                        }
                                    }

                                    // Balance badge
                                    val due = cust.currentDueInCents
                                    if (due > 0L) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEE2E2)
                                        ) {
                                            Text(
                                                text = "Due: ${CartCalculator.formatMoney(due, currency)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PosRose,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    } else if (due < 0L) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFECFDF5)
                                        ) {
                                            Text(
                                                text = "Adv: ${CartCalculator.formatMoney(-due, currency)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PosEmerald,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Settled",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Close Button
                PosOutlineButton(
                    text = "Close",
                    icon = Icons.Default.Clear,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

