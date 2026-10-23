package com.example.pos.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.ui.theme.PosEmerald

@Composable
fun ReceiptDialog(
    receiptText: String,
    onDismiss: () -> Unit,
    onPrint: (() -> Unit)? = null,
    isPrinting: Boolean = false
) {
    val context = LocalContext.current
    val strings = LocalPosStrings.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .testTag("receipt_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFD1FAE5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = strings.success,
                        tint = PosEmerald,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.transactionComplete,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.receiptReady,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Receipt Slip Paper Preview (Monospaced)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEFCE8)) // Light thermal paper tint
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("receipt_thermal_preview")
                ) {
                    Text(
                        text = receiptText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF1E293B),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Print & Share Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PosGradientButton(
                        text = strings.printEscPos,
                        onClick = {
                            if (onPrint != null) {
                                onPrint()
                            } else {
                                Toast.makeText(context, strings.printBluetoothToast, Toast.LENGTH_SHORT).show()
                            }
                        },
                        gradient = PosButtonGradients.Primary,
                        icon = Icons.Default.Print,
                        iconBadge = false,
                        isLoading = isPrinting,
                        enabled = !isPrinting,
                        shape = RoundedCornerShape(12.dp),
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                        testTag = "print_receipt_button"
                    )

                    PosOutlineButton(
                        text = strings.share,
                        icon = Icons.Default.Share,
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, receiptText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, strings.shareReceipt))
                        },
                        shape = RoundedCornerShape(12.dp),
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                        testTag = "share_receipt_button"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                PosGradientButton(
                    text = strings.newSale,
                    icon = Icons.Default.CheckCircle,
                    iconBadge = true,
                    onClick = onDismiss,
                    gradient = PosButtonGradients.Emerald,
                    shape = RoundedCornerShape(14.dp),
                    height = 50.dp,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "new_sale_done_button"
                )
            }
        }
    }
}
