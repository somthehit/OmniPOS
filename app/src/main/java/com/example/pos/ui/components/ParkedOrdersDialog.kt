package com.example.pos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pos.data.local.HeldOrderEntity
import com.example.pos.domain.CartCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.pos.ui.i18n.LocalPosStrings

@Composable
fun ParkedOrdersDialog(
    heldOrders: List<HeldOrderEntity>,
    currencySymbol: String,
    onResumeOrder: (HeldOrderEntity) -> Unit,
    onDeleteOrder: (HeldOrderEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalPosStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().testTag("parked_orders_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${strings.parkedOrders} (${heldOrders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = strings.close)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (heldOrders.isEmpty()) {
                    Text(strings.noParkedOrders, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(heldOrders, key = { it.id }) { order ->
                            val dateStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(order.heldAt))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(order.customerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("${strings.parkedAt(dateStr)} • ${CartCalculator.formatMoney(order.totalInCents, currencySymbol)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onDeleteOrder(order) },
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFEE2E2))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = strings.deleteOrder, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                        }
                                        PosCompactPillButton(
                                            text = strings.resumeOrder,
                                            icon = Icons.Default.PlayArrow,
                                            gradient = PosButtonGradients.Emerald,
                                            onClick = { onResumeOrder(order) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
