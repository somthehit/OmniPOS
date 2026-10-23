package com.example.pos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.local.CustomerEntity
import com.example.pos.data.model.CartItem
import com.example.pos.data.model.TaxMode
import com.example.pos.domain.CartCalculator
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosRose
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800

@Composable
fun CartPanel(
    cartItems: List<CartItem>,
    orderDiscountPercent: Double,
    selectedCustomer: CustomerEntity?,
    currencySymbol: String,
    taxMode: TaxMode,
    onQuantityChange: (String, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onParkOrder: () -> Unit,
    onSelectCustomerClick: () -> Unit,
    onRemoveCustomerClick: () -> Unit,
    onDiscountSelect: (Double) -> Unit,
    onCheckoutClick: () -> Unit,
    onAddCustomerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalPosStrings.current
    val summary = remember(cartItems, orderDiscountPercent, taxMode) {
        CartCalculator.calculateSummary(cartItems, orderDiscountPercent, taxMode)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.testTag("pos_cart_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            // Cart Header with Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = strings.cartTitle,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${strings.cartTitle} (${cartItems.sumOf { it.quantity }})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    if (cartItems.isNotEmpty()) {
                        IconButton(
                            onClick = onParkOrder,
                            modifier = Modifier.size(32.dp).testTag("park_order_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PauseCircle,
                                contentDescription = strings.holdOrder,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onClearCart,
                            modifier = Modifier.size(32.dp).testTag("clear_cart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = strings.clearCart,
                                tint = PosRose,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Attachment Badge
            if (selectedCustomer != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().testTag("attached_customer_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = selectedCustomer.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selectedCustomer.currentDueInCents > 0) {
                                    Text(
                                        text = "${strings.creditDue}: ${CartCalculator.formatMoney(selectedCustomer.currentDueInCents, currencySymbol)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PosRose
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onRemoveCustomerClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.PersonRemove, contentDescription = strings.removeCustomer, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PosOutlineButton(
                        text = strings.attachCustomer,
                        icon = Icons.Default.Person,
                        shape = RoundedCornerShape(10.dp),
                        height = 38.dp,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attach_customer_button"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        onClick = onSelectCustomerClick
                    )

                    PosCompactPillButton(
                        text = "+ ${strings.add}",
                        icon = Icons.Default.PersonAdd,
                        gradient = PosButtonGradients.Emerald,
                        height = 38.dp,
                        testTag = "new_customer_cart_button",
                        onClick = onAddCustomerClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cart Items Scrollable List
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.cartTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = strings.emptyCartMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("cart_items_list")
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        CartItemRow(
                            item = item,
                            currencySymbol = currencySymbol,
                            onQuantityChange = { delta -> onQuantityChange(item.product.id, delta) },
                            onRemove = { onRemoveItem(item.product.id) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Order Discount Quick Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${strings.discount}:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.0, 5.0, 10.0, 15.0).forEach { disc ->
                        val isSelected = orderDiscountPercent == disc
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onDiscountSelect(disc) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (disc == 0.0) strings.none else "${disc.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Totals Breakdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.subtotal, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                    Text(CartCalculator.formatMoney(summary.subtotalInCents, currencySymbol), style = MaterialTheme.typography.bodySmall)
                }
                if (summary.orderDiscountInCents > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.discount, style = MaterialTheme.typography.bodySmall, color = PosRose)
                        Text("-${CartCalculator.formatMoney(summary.orderDiscountInCents, currencySymbol)}", style = MaterialTheme.typography.bodySmall, color = PosRose)
                    }
                }
                if (summary.taxTotalInCents > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${strings.tax} (${taxMode.name})", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        Text(CartCalculator.formatMoney(summary.taxTotalInCents, currencySymbol), style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.grandTotal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = CartCalculator.formatMoney(summary.grandTotalInCents, currencySymbol),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Charge / Pay Action Button
            PosGradientButton(
                text = strings.payNow.uppercase(),
                subtitle = "${strings.total}: ${CartCalculator.formatMoney(summary.grandTotalInCents, currencySymbol)}",
                onClick = onCheckoutClick,
                enabled = cartItems.isNotEmpty(),
                gradient = PosButtonGradients.Emerald,
                icon = Icons.Default.ShoppingCart,
                iconBadge = true,
                shape = RoundedCornerShape(14.dp),
                height = 54.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_charge_button")
            )
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    currencySymbol: String,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("cart_item_${item.product.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                    text = "${CartCalculator.formatMoney(item.unitPriceInCents, currencySymbol)} / ${item.product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Stepper
        val isNepaliRow = LocalPosStrings.current is com.example.pos.ui.i18n.NepaliStrings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onQuantityChange(-1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, contentDescription = if (isNepaliRow) "घटाउनुहोस्" else "Decrease", modifier = Modifier.size(16.dp))
            }

            Text(
                text = "${item.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onQuantityChange(1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = if (isNepaliRow) "बढाउनुहोस्" else "Increase", modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Line Total
        Text(
            text = CartCalculator.formatMoney(item.totalInCents, currencySymbol),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Line Void Button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PosRose.copy(alpha = 0.12f))
                .testTag("void_line_${item.product.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = if (isNepaliRow) "सामान हटाउनुहोस्" else "Line Void",
                tint = PosRose,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
