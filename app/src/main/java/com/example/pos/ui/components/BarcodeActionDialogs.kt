package com.example.pos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Print
import kotlinx.coroutines.launch
import com.example.pos.data.local.ProductEntity
import com.example.pos.domain.BarcodeUtils
import com.example.pos.domain.CartCalculator
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import androidx.compose.foundation.layout.widthIn
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.Slate900
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import java.util.Locale

@Composable
fun BarcodeNotFoundDialog(
    scannedBarcode: String,
    onDismiss: () -> Unit,
    onCreateNewItem: (barcode: String) -> Unit,
    onLinkToExisting: (barcode: String) -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PosRose.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = PosRose, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(if (isNepali) "बारकोड फेला परेन" else "Barcode Not Found", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = scannedBarcode,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isNepali) "यो बारकोड आपको सूचीमा दर्ता भएको छैन। अगाडि बढ्न कार्य चयन गर्नुहोस्:" else "This barcode is not yet registered in your catalog. Choose an action to continue without losing your flow:",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            PosGradientButton(
                text = if (isNepali) "नयाँ सामान दर्ता गर्नुहोस्" else "Create New Item",
                icon = Icons.Default.Add,
                iconBadge = false,
                height = 44.dp,
                onClick = {
                    onDismiss()
                    onCreateNewItem(scannedBarcode)
                }
            )
        },
        dismissButton = {
            PosOutlineButton(
                text = if (isNepali) "मौजूदा सामानसँग जोड्नुहोस्" else "Link Existing Item",
                icon = Icons.Default.Link,
                height = 44.dp,
                onClick = {
                    onDismiss()
                    onLinkToExisting(scannedBarcode)
                }
            )
        }
    )
}

@Composable
fun LinkBarcodeDialog(
    barcodeToLink: String,
    products: List<ProductEntity>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onConfirmLink: (product: ProductEntity, packQty: Int) -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings
    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<ProductEntity?>(null) }
    var packQtyText by remember { mutableStateOf("1") }

    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.sku.contains(searchQuery, ignoreCase = true) ||
                    it.barcode.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, contentDescription = null, tint = PosPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isNepali) "बारकोड सामानसँग जोड्नुहोस्" else "Link Barcode to Item", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isNepali) "जोड्ने बारकोड:" else "Barcode to link:", style = MaterialTheme.typography.bodySmall)
                        Text(barcodeToLink, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Column {
                    Text(if (isNepali) "प्याक साइज / ग्यारेक मात्रा:" else "Pack Size / Multi-pack Quantity:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = packQtyText,
                        onValueChange = { packQtyText = it },
                        label = { Text(if (isNepali) "प्याक मात्रा (प्रति स्क्यान इकाई)" else "Pack Quantity (Units per scan)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            (if (isNepali) "१ (एकल)" else "1 (Single)") to 1,
                            (if (isNepali) "२ (प्याक)" else "6 (Pack)") to 6,
                            (if (isNepali) "१२ (कार्टन)" else "12 (Carton)") to 12,
                            (if (isNepali) "२२ (केस)" else "24 (Case)") to 24
                        ).forEach { (label, qty) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (packQtyText == qty.toString()) PosEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (packQtyText == qty.toString()) androidx.compose.foundation.BorderStroke(1.dp, PosEmerald) else null,
                                modifier = Modifier.clickable { packQtyText = qty.toString() }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Text(if (isNepali) "लक्षित सामान चयन गर्नुहोस्:" else "Select Target Product:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isNepali) "नाम वा SKU खोज्नुहोस्..." else "Search by name or SKU...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filteredProducts.forEach { prod ->
                        val isChosen = selectedItem?.id == prod.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChosen) PosPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = if (isChosen) androidx.compose.foundation.BorderStroke(1.5.dp, PosPrimary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedItem = prod }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(prod.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("SKU: ${prod.sku}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                                }
                                Text(
                                    CartCalculator.formatMoney(prod.sellingPriceInCents, currencySymbol),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            PosGradientButton(
                text = if (isNepali) "जोड्ने पुष्टि गर्नुहोस्" else "Confirm Link",
                icon = Icons.Default.Link,
                iconBadge = false,
                height = 44.dp,
                enabled = selectedItem != null,
                onClick = {
                    val item = selectedItem
                    val qty = packQtyText.toIntOrNull() ?: 1
                    if (item != null) {
                        onConfirmLink(item, qty)
                        onDismiss()
                    }
                }
            )
        },
        dismissButton = {
            PosOutlineButton(
                text = if (isNepali) "रद्द गर्नुहोस्" else "Cancel",
                height = 44.dp,
                onClick = onDismiss
            )
        }
    )
}

@Composable
fun QuickCreateProductDialog(
    prefilledBarcode: String,
    prefilledCostPrice: String = "0.00",
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onProductCreated: (ProductEntity, packQty: Int) -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("SKU-${System.currentTimeMillis().toString().takeLast(6)}") }
    var barcode by remember { mutableStateOf(prefilledBarcode) }
    val initialCost = prefilledCostPrice.toDoubleOrNull() ?: 0.0
    var costPriceText by remember {
        mutableStateOf(if (initialCost > 0.0) prefilledCostPrice else "0.00")
    }
    var sellingPriceText by remember {
        mutableStateOf(
            if (initialCost > 0.0) {
                String.format(Locale.US, "%.2f", initialCost * 1.25)
            } else "0.00"
        )
    }
    var unit by remember { mutableStateOf("pcs") }
    var packQtyText by remember { mutableStateOf("1") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Live profit & margin calculations
    val cost = costPriceText.toDoubleOrNull() ?: 0.0
    val sell = sellingPriceText.toDoubleOrNull() ?: 0.0
    val profit = sell - cost
    val marginPercent = if (sell > 0.0) (profit / sell) * 100.0 else 0.0
    val isProfit = profit >= 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row with Icon, Title, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PosEmerald.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isNepali) "द्रुत नयाँ सामान थप्नुहोस्" else "Quick Add New Product",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = if (isNepali) "आउने स्टकको लागि सामान दर्ता गर्नुहोस्" else "Fast-register item for inbound stock intake",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // 1. Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text(if (isNepali) "सामानको नाम *" else "Product Name *", maxLines = 1, softWrap = false) },
                    placeholder = { Text(if (isNepali) "उदा.: रियल जुस आपल १एल" else "e.g. Real Juice Apple 1L") },
                    leadingIcon = {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. SKU & Barcode in a balanced row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU *", maxLines = 1, softWrap = false) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = { sku = "SKU-${System.currentTimeMillis().toString().takeLast(6)}" }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerate SKU", tint = PosPrimary, modifier = Modifier.size(18.dp))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode", maxLines = 1, softWrap = false) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = { barcode = BarcodeUtils.generateInternalCode128(sku) }
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Generate Code128", tint = PosEmerald, modifier = Modifier.size(18.dp))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Cost Price & Selling Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = {
                            costPriceText = it
                            errorMsg = null
                        },
                        label = { Text(if (isNepali) "लागत मूल्य ($currencySymbol)" else "Cost Price ($currencySymbol)", maxLines = 1, softWrap = false) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = {
                            sellingPriceText = it
                            errorMsg = null
                        },
                        label = { Text(if (isNepali) "बिक्री मूल्य ($currencySymbol)" else "Selling Price ($currencySymbol)", maxLines = 1, softWrap = false) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Markup Preset Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isNepali) "मार्कअप:" else "Markup:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                    listOf(15, 20, 25, 30, 50).forEach { pct ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PosPrimary.copy(alpha = 0.08f))
                                .clickable {
                                    val c = costPriceText.toDoubleOrNull() ?: 0.0
                                    if (c > 0.0) {
                                        sellingPriceText = String.format(Locale.US, "%.2f", c * (1.0 + pct / 100.0))
                                    }
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "+$pct%",
                                style = MaterialTheme.typography.labelSmall,
                                color = PosPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Live Profit & Margin Indicator Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isProfit) PosEmerald.copy(alpha = 0.08f) else PosRose.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, if (isProfit) PosEmerald.copy(alpha = 0.25f) else PosRose.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = if (isProfit) PosEmerald else PosRose,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isNepali) (if (isProfit) "मार्जिन: ${String.format(Locale.US, "%.1f", marginPercent)}%" else "लागतभन्दा कम मूल्यमा बेचिँदैछ!") else (if (isProfit) "Margin: ${String.format(Locale.US, "%.1f", marginPercent)}%" else "Selling below cost!"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isProfit) PosEmerald else PosRose
                            )
                        }
                        Text(
                            text = if (isNepali) "फाइदा: $currencySymbol ${String.format(Locale.US, "%.2f", profit)} / इकाई" else "Profit: $currencySymbol ${String.format(Locale.US, "%.2f", profit)} / unit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfit) PosEmerald else PosRose
                        )
                    }
                }

                // 4. Unit & Pack Qty Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(if (isNepali) "इकाई" else "Unit", maxLines = 1, softWrap = false) },
                        placeholder = { Text(if (isNepali) "पिस, किलो, बक्स" else "pcs, kg, box") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = packQtyText,
                        onValueChange = { packQtyText = it },
                        label = { Text(if (isNepali) "प्याक मात्रा" else "Pack Qty", maxLines = 1, softWrap = false) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Unit Selectors Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("pcs", "kg", "g", "ltr", "ml", "box", "pkt", "dz").forEach { u ->
                        val isSelected = unit.equals(u, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PosEmerald else Color(0xFFF1F5F9))
                                .clickable { unit = u }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = u,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (errorMsg != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PosRose.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, PosRose.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMsg!!,
                            color = PosRose,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PosOutlineButton(
                        text = if (isNepali) "रद्द गर्नुहोस्" else "Cancel",
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )

                    PosGradientButton(
                        text = if (isNepali) "सेभ र चयन गर्नुहोस्" else "Save & Select",
                        icon = Icons.Default.Check,
                        gradient = PosButtonGradients.Emerald,
                        iconBadge = false,
                        height = 46.dp,
                        modifier = Modifier.weight(1.3f),
                        onClick = {
                            if (name.isBlank()) {
                                errorMsg = if (isNepali) "सामानको नाम अनिवार्य छो" else "Product name is required"
                                return@PosGradientButton
                            }
                            val costCents = ((costPriceText.toDoubleOrNull() ?: 0.0) * 100.0).toLong()
                            val sellCents = ((sellingPriceText.toDoubleOrNull() ?: 0.0) * 100.0).toLong()
                            val packQty = packQtyText.toIntOrNull() ?: 1

                            val newProd = ProductEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                orgId = "org_main",
                                categoryId = "cat_default",
                                name = name.trim(),
                                sku = sku.trim(),
                                barcode = barcode.trim(),
                                costPriceInCents = costCents,
                                sellingPriceInCents = sellCents,
                                unit = unit.trim().ifBlank { "pcs" }
                            )
                            onProductCreated(newProd, packQty)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InternalBarcodeLabelDialog(
    product: ProductEntity,
    onDismiss: () -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings
    val barcodeValue = remember(product) {
        if (product.barcode.isNotBlank()) product.barcode else BarcodeUtils.generateInternalCode128(product.sku)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = PosEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isNepali) "आन्तरिक Code128 लेबल" else "Internal Code128 Label", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isNepali) "उत्पादक बारकोड नभएका सामानमा यो मानकीकृत लेबल छाप्नुहोस् वा टाँस्नुहोस्:" else "Print or stick this standardized label on products without manufacturer barcodes:",
                    style = MaterialTheme.typography.bodySmall
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Black),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    BarcodeLabelPreview(
                        name = product.name,
                        sku = product.sku,
                        code = barcodeValue,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            val context = androidx.compose.ui.platform.LocalContext.current
            var isPrinting by remember { mutableStateOf(false) }
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosOutlineButton(
                    text = if (isNepali) "प्रिन्ट (Print)" else "Print",
                    icon = Icons.Default.Print,
                    onClick = {
                        isPrinting = true
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val bmp = BarcodeRenderer.label(product.name, product.sku, barcodeValue)
                                val pdfFile = buildLabelPdf(context, bmp, 1)
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                                    val printAdapter = object : android.print.PrintDocumentAdapter() {
                                        override fun onLayout(
                                            oldAttributes: android.print.PrintAttributes?,
                                            newAttributes: android.print.PrintAttributes,
                                            cancellationSignal: android.os.CancellationSignal?,
                                            callback: LayoutResultCallback,
                                            extras: android.os.Bundle?
                                        ) {
                                            if (cancellationSignal?.isCanceled == true) {
                                                callback.onLayoutCancelled()
                                                return
                                            }
                                            val info = android.print.PrintDocumentInfo.Builder(pdfFile.name)
                                                .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                                .build()
                                            callback.onLayoutFinished(info, true)
                                        }

                                        override fun onWrite(
                                            pages: Array<out android.print.PageRange>,
                                            destination: android.os.ParcelFileDescriptor,
                                            cancellationSignal: android.os.CancellationSignal?,
                                            callback: WriteResultCallback
                                        ) {
                                            try {
                                                java.io.FileInputStream(pdfFile).use { input ->
                                                    java.io.FileOutputStream(destination.fileDescriptor).use { output ->
                                                        input.copyTo(output)
                                                    }
                                                }
                                                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                            } catch (e: Exception) {
                                                callback.onWriteFailed(e.message)
                                            }
                                        }
                                    }
                                    printManager.print("Label_${product.sku}", printAdapter, android.print.PrintAttributes.Builder().build())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isPrinting = false
                            }
                        }
                    }
                )
                PosGradientButton(
                    text = if (isNepali) "सकियो" else "Done",
                    icon = Icons.Default.Check,
                    iconBadge = false,
                    height = 44.dp,
                    onClick = onDismiss
                )
            }
        }
    )
}

@Composable
fun ProductDetailDialog(
    product: ProductEntity,
    stockQty: Int,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }

    val barcodeValue = remember(product) {
        if (product.barcode.isNotBlank()) product.barcode else BarcodeUtils.generateInternalCode128(product.sku)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PosPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PosPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "ID: ${product.id}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Product Metrics Summary Cards
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "SKU कोड:" else "SKU Code:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            Text(product.sku, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Slate900)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "बारकोड:" else "Barcode:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            Text(barcodeValue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Slate900)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "लागत मूल्य (Cost):" else "Cost Price:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            Text(CartCalculator.formatMoney(product.costPriceInCents, currencySymbol), fontWeight = FontWeight.SemiBold, color = Slate700)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "बिक्री मूल्य (Sell):" else "Selling Price:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            Text(CartCalculator.formatMoney(product.sellingPriceInCents, currencySymbol), fontWeight = FontWeight.Bold, color = PosPrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "बिक्री कर (Tax Rate):" else "Tax Rate:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            Text("${product.taxRatePercent}%", fontWeight = FontWeight.SemiBold, color = Slate700)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "मौज्दात स्थिति (Stock):" else "Stock On Hand:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            Text("$stockQty ${product.unit}", fontWeight = FontWeight.Bold, color = if (stockQty <= product.minStockAlert) PosRose else PosEmerald)
                        }
                    }
                }

                // Barcode Preview Section
                Text(
                    text = if (isNepali) "स्क्यानयोग्य बारकोड लेबल (Printable Label)" else "Scannable Printable Barcode Label",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color.Black),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BarcodeLabelPreview(
                        name = product.name,
                        sku = product.sku,
                        code = barcodeValue,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PosOutlineButton(
                        text = if (isNepali) "रद्द गर्नुहोस्" else "Cancel",
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PosGradientButton(
                        text = if (isNepali) "प्रिन्ट लेबल (Print)" else "Print Barcode Label",
                        icon = Icons.Default.Print,
                        gradient = PosButtonGradients.Emerald,
                        height = 44.dp,
                        onClick = {
                            isPrinting = true
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val bmp = BarcodeRenderer.label(product.name, product.sku, barcodeValue)
                                    val pdfFile = buildLabelPdf(context, bmp, 1)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                                        val printAdapter = object : android.print.PrintDocumentAdapter() {
                                            override fun onLayout(
                                                oldAttributes: android.print.PrintAttributes?,
                                                newAttributes: android.print.PrintAttributes,
                                                cancellationSignal: android.os.CancellationSignal?,
                                                callback: LayoutResultCallback,
                                                extras: android.os.Bundle?
                                            ) {
                                                if (cancellationSignal?.isCanceled == true) {
                                                    callback.onLayoutCancelled()
                                                    return
                                                }
                                                val info = android.print.PrintDocumentInfo.Builder(pdfFile.name)
                                                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                                    .build()
                                                callback.onLayoutFinished(info, true)
                                            }

                                            override fun onWrite(
                                                pages: Array<out android.print.PageRange>,
                                                destination: android.os.ParcelFileDescriptor,
                                                cancellationSignal: android.os.CancellationSignal?,
                                                callback: WriteResultCallback
                                            ) {
                                                try {
                                                    java.io.FileInputStream(pdfFile).use { input ->
                                                        java.io.FileOutputStream(destination.fileDescriptor).use { output ->
                                                            input.copyTo(output)
                                                        }
                                                    }
                                                    callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                                                } catch (e: Exception) {
                                                    callback.onWriteFailed(e.message)
                                                }
                                            }
                                        }
                                        printManager.print("Label_${product.sku}", printAdapter, android.print.PrintAttributes.Builder().build())
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isPrinting = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
