package com.example.pos.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pos.data.local.ShiftEntity
import com.example.pos.data.model.CreditNoteRecord
import com.example.pos.data.model.DenominationItem
import com.example.pos.data.model.OnlineProviderDeclaration
import com.example.pos.data.model.ShiftCloseDraft
import com.example.pos.data.model.ShiftLiveMetrics
import com.example.pos.domain.CartCalculator
import com.example.pos.domain.ShiftFinancialEngine
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosSky
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShiftDialog(
    activeShift: ShiftEntity?,
    liveMetrics: ShiftLiveMetrics? = null,
    expectedOpeningFloat: Long = 0L,
    currencySymbol: String = "Rs",
    isStale: Boolean = false,
    onDismiss: () -> Unit,
    onStartShift: (Long) -> Unit,
    onStartShiftAdvanced: ((Long, Long, String, List<DenominationItem>) -> Unit)? = null,
    onEndShift: (Long, Long, Int, String) -> Unit,
    onEndShiftAdvanced: ((ShiftCloseDraft) -> Unit)? = null,
    onSaveCloseDraft: ((ShiftCloseDraft) -> Unit)? = null,
    onAddCashMovement: (String, Long, String) -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings
    var showCloseWizard by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 620.dp)
                .padding(vertical = 12.dp)
                .testTag("shift_management_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (activeShift != null) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (activeShift != null) Icons.Default.PointOfSale else Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (activeShift != null) PosEmerald else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (activeShift != null) (if (isNepali) "चालू काउन्टर शिफ्ट" else "Active Register Shift") else (if (isNepali) "नयाँ शिफ्ट खोल्नुहोस्" else "Open New Shift"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeShift != null) (if (isNepali) "काउन्टर #१ • ${activeShift.cashierName}" else "Register #1 • ${activeShift.cashierName}") else (if (isNepali) "क्यासियर प्रारम्भिक नगद र काउन्टर सेटअप" else "Cashier Float & Register Setup"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stale Shift Blocking Banner
                val shiftIsStale = isStale || (liveMetrics?.isStale == true) || (activeShift != null && ShiftFinancialEngine.isShiftStale(activeShift.openedAt, activeShift.businessDate, System.currentTimeMillis()))
                if (shiftIsStale && activeShift != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PosRose.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosRose.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = PosRose,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isNepali) "म्याद सकिएको शिफ्ट फेला पर्यो (>१२ घण्टा / अघिल्लो मिति)" else "STALE SHIFT DETECTED (>12H / PRIOR DATE)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PosRose
                                )
                                Text(
                                    text = if (isNepali) "बिक्री काउन्टर लक गरिएको छ। नयाँ शिफ्ट खोल्नु अघि यो शिफ्ट हिसाब मिलान गरी बन्द गर्नुपर्छ।" else "Register sales are locked. You must reconcile drawer cash and close this shift before opening a new one.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (activeShift == null) {
                    // Open Shift Flow (Denomination Counter, Opening Gap calculation)
                    OpenShiftFlow(
                        expectedOpeningFloat = expectedOpeningFloat,
                        currencySymbol = currencySymbol,
                        onStartShift = { floatCents ->
                            if (onStartShiftAdvanced != null) {
                                onStartShiftAdvanced(floatCents, expectedOpeningFloat, "", emptyList())
                            } else {
                                onStartShift(floatCents)
                            }
                        },
                        onStartShiftAdvanced = onStartShiftAdvanced
                    )
                } else if (showCloseWizard) {
                    // 4-Step Close Shift Wizard
                    CloseShiftWizard(
                        shift = activeShift,
                        liveMetrics = liveMetrics,
                        currencySymbol = currencySymbol,
                        onCancel = { showCloseWizard = false },
                        onSaveDraft = { draft ->
                            onSaveCloseDraft?.invoke(draft)
                            showCloseWizard = false
                        },
                        onFinalizeClose = { draft ->
                            if (onEndShiftAdvanced != null) {
                                onEndShiftAdvanced(draft)
                            } else {
                                val onlineTotal = draft.onlineDeclarations.sumOf { it.declaredAmountInCents }
                                val onlineSlips = draft.onlineDeclarations.sumOf { it.slipCount }
                                onEndShift(draft.countedCashInCents, onlineTotal, onlineSlips, draft.closingNotes)
                            }
                            showCloseWizard = false
                        }
                    )
                } else {
                    // Active Shift Flow & Live Metrics
                    ActiveShiftFlow(
                        activeShift = activeShift,
                        liveMetrics = liveMetrics,
                        currencySymbol = currencySymbol,
                        isStale = shiftIsStale,
                        onOpenCloseWizard = { showCloseWizard = true },
                        onAddCashMovement = onAddCashMovement
                    )
                }
            }
        }
    }
}

/**
 * Open Shift Flow with DenominationCounter and Opening Gap explanation.
 */
@Composable
private fun OpenShiftFlow(
    expectedOpeningFloat: Long,
    currencySymbol: String,
    onStartShift: (Long) -> Unit,
    onStartShiftAdvanced: ((Long, Long, String, List<DenominationItem>) -> Unit)?
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings
    var denominationCounts by remember { mutableStateOf(emptyList<DenominationItem>()) }
    var totalCountedCash by remember { mutableStateOf(0L) }
    var gapReason by remember { mutableStateOf("") }
    var showZeroConfirmDialog by remember { mutableStateOf(false) }

    val gapCents = totalCountedCash - expectedOpeningFloat

    Column {
        if (expectedOpeningFloat > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isNepali) "अघिल्लो शिफ्टको बन्द नगद" else "PREVIOUS SHIFT CLOSING FLOAT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isNepali) "काउन्टरमा छोडिएको अपेक्षित नगद" else "Expected cash left in drawer",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = CartCalculator.formatMoney(expectedOpeningFloat, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text(
            text = if (isNepali) "काउन्टरमा भएको प्रारम्भिक नगद गन्नुहोस्:" else "Count the opening cash float in the drawer:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Denomination Counter
        DenominationCounter(
            currencySymbol = currencySymbol,
            expectedOpeningFloat = expectedOpeningFloat,
            onCountsChanged = { items, total ->
                denominationCounts = items
                totalCountedCash = total
            }
        )

        // Opening Gap Alert if counted != expected
        if (expectedOpeningFloat > 0 && gapCents != 0L) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PosAmber.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosAmber.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isNepali) "प्रारम्भिक नगदमा फरक:" else "Opening Float Discrepancy:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${if (gapCents > 0) "+" else ""}${CartCalculator.formatMoney(gapCents, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (gapCents > 0) PosSky else PosRose
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = gapReason,
                        onValueChange = { gapReason = it },
                        label = { Text(if (isNepali) "नगद फरक हुनुको कारण *" else "Reason for float gap *") },
                        placeholder = { Text(if (isNepali) "जस्तै: खुद्रा पैसा थपिएको, आकस्मिक खर्च" else "e.g. Added emergency change, petty cash replenishment") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Open Shift Action Button
        val isGapValid = (expectedOpeningFloat == 0L || gapCents == 0L || gapReason.isNotBlank())
        Button(
            onClick = {
                if (totalCountedCash == 0L) {
                    showZeroConfirmDialog = true
                } else {
                    if (onStartShiftAdvanced != null) {
                        onStartShiftAdvanced(totalCountedCash, expectedOpeningFloat, gapReason, denominationCounts)
                    } else {
                        onStartShift(totalCountedCash)
                    }
                }
            },
            enabled = isGapValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = PosEmerald,
                disabledContainerColor = PosEmerald.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (totalCountedCash == 0L) {
                    if (isNepali) "शिफ्ट खोल्नुहोस् (शून्य मौज्दात)" else "Open Shift (Zero Float)"
                } else {
                    if (isNepali) "${CartCalculator.formatMoney(totalCountedCash, currencySymbol)} सहित शिफ्ट खोल्नुहोस्" else "Open Shift with ${CartCalculator.formatMoney(totalCountedCash, currencySymbol)} Float"
                },
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    if (showZeroConfirmDialog) {
        val strings = LocalPosStrings.current
        val isNepali = strings is NepaliStrings
        AlertDialog(
            onDismissRequest = { showZeroConfirmDialog = false },
            title = { Text(if (isNepali) "शून्य मौज्दातमा शिफ्ट खोल्ने?" else "Open Shift with Zero Float?") },
            text = {
                Text(if (isNepali) "तपाईं काउन्टरमा $currencySymbol ०.०० नगद मौज्दात राखी शिफ्ट खोल्दै हुनुहुन्छ। के तपाईं ग्राहकलाई फिर्ता दिन काउन्टरमा नगद नभएकोमा निश्चित हुनुहुन्छ?" else "You are opening the register shift with $currencySymbol 0.00 cash float. Are you sure you have no cash in the drawer to provide customer change?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showZeroConfirmDialog = false
                        if (onStartShiftAdvanced != null) {
                            onStartShiftAdvanced(0L, expectedOpeningFloat, gapReason, denominationCounts)
                        } else {
                            onStartShift(0L)
                        }
                    }
                ) {
                    Text(if (isNepali) "हो, ० मौज्दातमा खोल्नुहोस्" else "Yes, Open with 0 Float")
                }
            },
            dismissButton = {
                TextButton(onClick = { showZeroConfirmDialog = false }) {
                    Text(if (isNepali) "रद्द गर्नुहोस्" else "Cancel")
                }
            }
        )
    }
}

/**
 * Active Shift Flow: Live Sales Metrics & Cash Movements.
 */
@Composable
private fun ActiveShiftFlow(
    activeShift: ShiftEntity,
    liveMetrics: ShiftLiveMetrics?,
    currencySymbol: String,
    isStale: Boolean,
    onOpenCloseWizard: () -> Unit,
    onAddCashMovement: (String, Long, String) -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    var showMovementDialog by remember { mutableStateOf(false) }
    var movementType by remember { mutableStateOf("PAY_IN") } // PAY_IN, PAY_OUT, SAFE_DROP
    var movementAmountText by remember { mutableStateOf("") }
    var movementReasonText by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val openingFloatCents = activeShift.openingFloatInCents
    val cashSalesCents = liveMetrics?.cashSalesInCents ?: 0L
    val payInCents = liveMetrics?.payInInCents ?: 0L
    val payOutCents = liveMetrics?.payOutInCents ?: 0L
    val safeDropCents = liveMetrics?.safeDropInCents ?: 0L
    val creditCollectionsCents = liveMetrics?.creditCollectionInCents ?: 0L

    val expectedCashCents = liveMetrics?.expectedCashInCents
        ?: ShiftFinancialEngine.calculateExpectedCash(
            openingFloatInCents = openingFloatCents,
            cashSalesInCents = cashSalesCents,
            cashCreditCollectionsInCents = creditCollectionsCents,
            paidInInCents = payInCents,
            cashRefundsInCents = 0L,
            paidOutInCents = payOutCents,
            safeDropsInCents = safeDropCents
        )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Shift Info Header Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PosEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeShift.cashierName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = PosEmerald
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(activeShift.cashierName, fontWeight = FontWeight.Bold)
                            Text(
                                if (isNepali) "सुरु: ${dateFormat.format(Date(activeShift.openedAt))}" else "Opened: ${dateFormat.format(Date(activeShift.openedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PosEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isNepali) "अवस्था: ${activeShift.status}" else "STATUS: ${activeShift.status}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosEmerald
                        )
                    }
                }
            }
        }

        // Expected Cash in Drawer Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isNepali) "काउन्टरमा हुनुपर्ने नगद" else "EXPECTED CASH IN DRAWER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isNepali) "मौज्दात + बिक्री + दाखिला - खर्च - सुरक्षित जम्मा" else "Float + Sales + In - Out - Safe Drops",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = CartCalculator.formatMoney(expectedCashCents, currencySymbol),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                // Breakdown Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(if (isNepali) "मौज्दात" else "Float", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(CartCalculator.formatMoney(openingFloatCents, currencySymbol), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text(if (isNepali) "नगद बिक्री" else "Cash Sales", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(CartCalculator.formatMoney(cashSalesCents, currencySymbol), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PosEmerald)
                    }
                    Column {
                        Text(if (isNepali) "दाखिला / खर्च" else "Pay In / Out", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+${CartCalculator.formatMoney(payInCents, currencySymbol)} / -${CartCalculator.formatMoney(payOutCents, currencySymbol)}", fontSize = 12.sp)
                    }
                    if (safeDropCents > 0) {
                        Column {
                            Text(if (isNepali) "सुरक्षित जम्मा" else "Safe Drops", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-${CartCalculator.formatMoney(safeDropCents, currencySymbol)}", fontSize = 12.sp, color = PosAmber)
                        }
                    }
                }
            }
        }

        // Live Tender Breakdown
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (isNepali) "भुक्तानी माध्यम विवरण (बिक्री)" else "Tender Breakdown (Sales)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (isNepali) "कार्ड बिक्री:" else "Card Sales:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CartCalculator.formatMoney(liveMetrics?.cardSalesInCents ?: 0L, currencySymbol), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (isNepali) "QR / वालेट बिक्री:" else "QR / UPI Sales:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CartCalculator.formatMoney(liveMetrics?.qrSalesInCents ?: 0L, currencySymbol), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (isNepali) "उधारो / खाता बिक्री:" else "Credit / Khata Sales:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(CartCalculator.formatMoney(liveMetrics?.creditSalesInCents ?: 0L, currencySymbol), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (isNepali) "कुल बिल संख्या:" else "Total Invoices:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${liveMetrics?.salesCount ?: 0}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Movement Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    movementType = "PAY_IN"
                    movementAmountText = ""
                    movementReasonText = ""
                    showMovementDialog = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isNepali) "+ नगद दाखिला" else "+ Pay In", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    movementType = "PAY_OUT"
                    movementAmountText = ""
                    movementReasonText = ""
                    showMovementDialog = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isNepali) "- नगद खर्च" else "- Pay Out", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    movementType = "SAFE_DROP"
                    movementAmountText = ""
                    movementReasonText = ""
                    showMovementDialog = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isNepali) "सुरक्षित जम्मा" else "Safe Drop", fontSize = 12.sp, color = PosAmber)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Close Shift Button (Launches 4-Step Wizard)
        Button(
            onClick = onOpenCloseWizard,
            colors = ButtonDefaults.buttonColors(containerColor = PosRose),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("start_close_shift_wizard_button")
        ) {
            Icon(Icons.Default.LockClock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isNepali) "शिफ्ट बन्द तथा नगद मिलान" else "Close Shift & Reconcile Drawer", fontWeight = FontWeight.Bold)
        }
    }

    // Cash Movement Dialog
    if (showMovementDialog) {
        AlertDialog(
            onDismissRequest = { showMovementDialog = false },
            title = {
                Text(
                    when (movementType) {
                        "PAY_IN" -> if (isNepali) "नगद दाखिला दर्ता" else "Record Cash Pay-In"
                        "PAY_OUT" -> if (isNepali) "नगद खर्च दर्ता" else "Record Cash Pay-Out"
                        "SAFE_DROP" -> if (isNepali) "सुरक्षित जम्मा दर्ता" else "Record Safe Drop"
                        else -> if (isNepali) "नगद कारोबार" else "Cash Movement"
                    }
                )
            },
            text = {
                Column {
                    Text(
                        when (movementType) {
                            "PAY_IN" -> if (isNepali) "काउन्टरमा थप नगद दाखिला गर्नुहोस् (जस्तै खुद्रा पैसा थप)।" else "Add cash to the register drawer (e.g. change float addition)."
                            "PAY_OUT" -> if (isNepali) "खर्च वा सानो भुक्तानीको लागि काउन्टरबाट नगद निकाल्नुहोस्।" else "Remove cash for expenses or petty cash."
                            "SAFE_DROP" -> if (isNepali) "काउन्टरको अतिरिक्त नगद सुरक्षित लकरमा सार्नुहोस्।" else "Transfer excess cash from drawer to secure safe."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = movementAmountText,
                        onValueChange = { movementAmountText = it },
                        label = { Text(if (isNepali) "रकम ($currencySymbol)" else "Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = movementReasonText,
                        onValueChange = { movementReasonText = it },
                        label = { Text(if (isNepali) "कारण / विवरण *" else "Reason / Description *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountCents = ((movementAmountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        if (amountCents > 0 && movementReasonText.isNotBlank()) {
                            onAddCashMovement(movementType, amountCents, movementReasonText)
                            showMovementDialog = false
                        }
                    }
                ) {
                    Text(if (isNepali) "पुष्टि गर्नुहोस्" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMovementDialog = false }) {
                    Text(if (isNepali) "रद्द गर्नुहोस्" else "Cancel")
                }
            }
        )
    }
}

/**
 * 4-Step Resumable Close Shift Wizard.
 */
@Composable
private fun CloseShiftWizard(
    shift: ShiftEntity,
    liveMetrics: ShiftLiveMetrics?,
    currencySymbol: String,
    onCancel: () -> Unit,
    onSaveDraft: (ShiftCloseDraft) -> Unit,
    onFinalizeClose: (ShiftCloseDraft) -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    var step by remember { mutableIntStateOf(1) } // 1: Summary, 2: Cash Count, 3: Declarations, 4: Review

    // Wizard draft state
    var countedCashCents by remember { mutableStateOf(shift.countedCashInCents ?: 0L) }
    var denominationCounts by remember { mutableStateOf(emptyList<DenominationItem>()) }
    var blindCloseEnabled by remember { mutableStateOf(true) }

    // Online declarations
    val onlineDeclarations = remember {
        mutableStateListOf(
            OnlineProviderDeclaration("eSewa", 0L, 0),
            OnlineProviderDeclaration("Khalti", 0L, 0),
            OnlineProviderDeclaration("FonePay / QR", 0L, 0),
            OnlineProviderDeclaration("Card (POS)", 0L, 0),
            OnlineProviderDeclaration("Bank Transfer", 0L, 0)
        )
    }

    // Credit notes
    val creditNotes = remember { mutableStateListOf<CreditNoteRecord>() }
    var closingFloatCents by remember { mutableStateOf(shift.openingFloatInCents) }
    var varianceReason by remember { mutableStateOf(shift.varianceReason) }
    var closingNotes by remember { mutableStateOf(shift.notes) }

    // Supervisor override PIN
    var supervisorPin by remember { mutableStateOf("") }
    var supervisorPinError by remember { mutableStateOf<String?>(null) }

    val openingFloatCents = shift.openingFloatInCents
    val cashSalesCents = liveMetrics?.cashSalesInCents ?: 0L
    val payInCents = liveMetrics?.payInInCents ?: 0L
    val payOutCents = liveMetrics?.payOutInCents ?: 0L
    val safeDropCents = liveMetrics?.safeDropInCents ?: 0L
    val creditCollectionCents = liveMetrics?.creditCollectionInCents ?: 0L

    val creditNoteCashRefunds = creditNotes.sumOf { it.cashRefundInCents }
    val expectedCashCents = ShiftFinancialEngine.calculateExpectedCash(
        openingFloatInCents = openingFloatCents,
        cashSalesInCents = cashSalesCents,
        cashCreditCollectionsInCents = creditCollectionCents,
        paidInInCents = payInCents,
        cashRefundsInCents = creditNoteCashRefunds,
        paidOutInCents = payOutCents,
        safeDropsInCents = safeDropCents
    )

    val cashVarianceCents = ShiftFinancialEngine.calculateCashVariance(countedCashCents, expectedCashCents)
    val varianceStatus = ShiftFinancialEngine.checkVarianceThreshold(cashVarianceCents)
    val depositAmountCents = (countedCashCents - closingFloatCents).coerceAtLeast(0L)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Wizard Progress Bar (Steps 1 to 4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val stepLabels = if (isNepali) listOf("१. सारांश", "२. नगद गणना", "३. घोषणा", "४. समीक्षा") else listOf("1. Summary", "2. Cash Count", "3. Declarations", "4. Review")
            stepLabels.forEachIndexed { idx, label ->
                val stepNum = idx + 1
                val isCurrent = step == stepNum
                val isDone = step > stepNum

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isDone -> PosEmerald
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        } else {
                            Text(
                                text = "$stepNum",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = label.substringAfter(". "),
                        fontSize = 10.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Step Content
        when (step) {
            1 -> {
                // STEP 1: Shift Summary
                Text(if (isNepali) "चरण १: शिफ्ट सारांश" else "Step 1: Shift Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "कर्मचारी:" else "Cashier:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(shift.cashierName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "सुरुवाती नगद:" else "Opening Float:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CartCalculator.formatMoney(openingFloatCents, currencySymbol), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "कुल बिलहरू:" else "Total Invoices:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${liveMetrics?.salesCount ?: 0}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "कुल बिक्री:" else "Gross Sales:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CartCalculator.formatMoney(liveMetrics?.salesTotalInCents ?: 0L, currencySymbol), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            2 -> {
                // STEP 2: Cash Count (Blind Close option)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (isNepali) "चरण २: काउन्टर नगद गणना" else "Step 2: Cash Drawer Count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (blindCloseEnabled) (if (isNepali) "अप्रत्यक्ष बन्द सक्रिय (हुनुपर्ने नगद लुकाइएको)" else "Blind close enabled (expected cash hidden)") else (if (isNepali) "हुनुपर्ने नगद देखाइएको" else "Expected cash displayed"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isNepali) "अप्रत्यक्ष" else "Blind", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = blindCloseEnabled,
                            onCheckedChange = { blindCloseEnabled = it }
                        )
                    }
                }

                if (!blindCloseEnabled) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isNepali) "काउन्टरमा हुनुपर्ने नगद:" else "Expected Cash in Drawer:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(CartCalculator.formatMoney(expectedCashCents, currencySymbol), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                DenominationCounter(
                    currencySymbol = currencySymbol,
                    initialCounts = denominationCounts,
                    onCountsChanged = { items, total ->
                        denominationCounts = items
                        countedCashCents = total
                    }
                )
            }
            3 -> {
                // STEP 3: Online Declarations & Credit Notes
                Text(if (isNepali) "चरण ३: घोषणा र हिसाब मिलान" else "Step 3: Declarations & Credit Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (isNepali) "डिजिटल वालेट र कार्ड मेसिनका रसिदहरू मिलान गर्नुहोस्:" else "Reconcile slips from digital wallets and card terminals:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                onlineDeclarations.forEachIndexed { index, decl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(decl.provider, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.width(110.dp))

                        var slipsText by remember(decl.slipCount) { mutableStateOf(if (decl.slipCount > 0) decl.slipCount.toString() else "") }
                        OutlinedTextField(
                            value = slipsText,
                            onValueChange = { input ->
                                slipsText = input.filter { it.isDigit() }
                                val slips = slipsText.toIntOrNull() ?: 0
                                onlineDeclarations[index] = decl.copy(slipCount = slips)
                            },
                            placeholder = { Text(if (isNepali) "० रसिद" else "0 slips", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(80.dp)
                        )

                        var amtText by remember(decl.declaredAmountInCents) {
                            mutableStateOf(if (decl.declaredAmountInCents > 0) String.format(Locale.US, "%.2f", decl.declaredAmountInCents / 100.0) else "")
                        }
                        OutlinedTextField(
                            value = amtText,
                            onValueChange = { input ->
                                amtText = input
                                val cents = ((input.toDoubleOrNull() ?: 0.0) * 100).toLong()
                                onlineDeclarations[index] = decl.copy(declaredAmountInCents = cents)
                            },
                            placeholder = { Text("0.00", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
            }
            4 -> {
                // STEP 4: Review, Variance check, Closing Float & Confirmation
                Text(if (isNepali) "चरण ४: हिसाब मिलान र समीक्षा" else "Step 4: Reconciliation & Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "हुनुपर्ने नगद:" else "Expected Cash:", fontSize = 13.sp)
                            Text(CartCalculator.formatMoney(expectedCashCents, currencySymbol), fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "गनिएको नगद:" else "Counted Cash:", fontSize = 13.sp)
                            Text(CartCalculator.formatMoney(countedCashCents, currencySymbol), fontWeight = FontWeight.Bold)
                        }

                        // Variance Pill
                        val varianceBg = when (varianceStatus) {
                            ShiftFinancialEngine.VarianceStatus.UNDER_TOLERANCE -> PosEmerald.copy(alpha = 0.15f)
                            ShiftFinancialEngine.VarianceStatus.WARN_REASON_REQUIRED -> PosAmber.copy(alpha = 0.15f)
                            ShiftFinancialEngine.VarianceStatus.HARD_BLOCK_SUPERVISOR_REQUIRED -> PosRose.copy(alpha = 0.15f)
                        }
                        val varianceTextColor = when (varianceStatus) {
                            ShiftFinancialEngine.VarianceStatus.UNDER_TOLERANCE -> PosEmerald
                            ShiftFinancialEngine.VarianceStatus.WARN_REASON_REQUIRED -> PosAmber
                            ShiftFinancialEngine.VarianceStatus.HARD_BLOCK_SUPERVISOR_REQUIRED -> PosRose
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = varianceBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isNepali) "नगद फरक:" else "Cash Variance:", fontWeight = FontWeight.Bold, color = varianceTextColor)
                                Text(
                                    "${if (cashVarianceCents > 0) "+" else ""}${CartCalculator.formatMoney(cashVarianceCents, currencySymbol)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = varianceTextColor
                                )
                            }
                        }
                    }
                }

                // If variance requires reason
                if (varianceStatus != ShiftFinancialEngine.VarianceStatus.UNDER_TOLERANCE) {
                    OutlinedTextField(
                        value = varianceReason,
                        onValueChange = { varianceReason = it },
                        label = { Text(if (isNepali) "नगद फरक हुनुको स्पष्टीकरण *" else "Variance Explanation Reason *") },
                        placeholder = { Text(if (isNepali) "घटी वा बढी हुनुको कारण खुलाउनुहोस्" else "Explain discrepancy (shortage / overage reason)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // If variance requires supervisor override PIN
                if (varianceStatus == ShiftFinancialEngine.VarianceStatus.HARD_BLOCK_SUPERVISOR_REQUIRED) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PosRose.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosRose.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                if (isNepali) "सुपरभाइजर स्वीकृति आवश्यक (फरक > ${ShiftFinancialEngine.HARD_BLOCK_VARIANCE_CENTS / 100})" else "SUPERVISOR OVERRIDE REQUIRED (Variance > ${ShiftFinancialEngine.HARD_BLOCK_VARIANCE_CENTS / 100})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosRose
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = supervisorPin,
                                onValueChange = {
                                    supervisorPin = it.filter { ch -> ch.isDigit() }.take(6)
                                    supervisorPinError = null
                                },
                                label = { Text(if (isNepali) "सुपरभाइजर पिन" else "Supervisor PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                isError = supervisorPinError != null,
                                supportingText = {
                                    supervisorPinError?.let { Text(it, color = PosRose) }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Closing Float to keep in drawer & Bank Deposit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var floatText by remember(closingFloatCents) {
                        mutableStateOf(String.format(Locale.US, "%.2f", closingFloatCents / 100.0))
                    }
                    OutlinedTextField(
                        value = floatText,
                        onValueChange = { input ->
                            floatText = input
                            closingFloatCents = ((input.toDoubleOrNull() ?: 0.0) * 100).toLong()
                        },
                        label = { Text(if (isNepali) "काउन्टरमा राख्ने नगद" else "Keep in Drawer") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = CartCalculator.formatMoney(depositAmountCents, currencySymbol),
                        onValueChange = {},
                        label = { Text(if (isNepali) "सुरक्षित/बैंक दाखिला" else "Safe/Bank Drop") },
                        readOnly = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Notes
                OutlinedTextField(
                    value = closingNotes,
                    onValueChange = { closingNotes = it },
                    label = { Text(if (isNepali) "बन्द टिप्पणी" else "Closing Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Navigation & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 1) {
                OutlinedButton(onClick = { step-- }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isNepali) "फर्कनुहोस्" else "Back")
                }
            } else {
                TextButton(onClick = onCancel) {
                    Text(if (isNepali) "रद्द गर्नुहोस्" else "Cancel")
                }
            }

            // Save Draft button
            OutlinedButton(
                onClick = {
                    val draft = ShiftCloseDraft(
                        shiftId = shift.id,
                        countedCashInCents = countedCashCents,
                        denominationCounts = denominationCounts,
                        onlineDeclarations = onlineDeclarations.toList(),
                        creditNotes = creditNotes.toList(),
                        closingFloatInCents = closingFloatCents,
                        depositAmountInCents = depositAmountCents,
                        varianceReason = varianceReason,
                        closingNotes = closingNotes
                    )
                    onSaveDraft(draft)
                }
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isNepali) "ड्राफ्ट सुरक्षित गर्नुहोस्" else "Save Draft")
            }

            if (step < 4) {
                Button(onClick = { step++ }) {
                    Text(if (isNepali) "अर्को" else "Next")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            } else {
                // Finalize Close button
                val canFinalize = when (varianceStatus) {
                    ShiftFinancialEngine.VarianceStatus.UNDER_TOLERANCE -> true
                    ShiftFinancialEngine.VarianceStatus.WARN_REASON_REQUIRED -> varianceReason.isNotBlank()
                    ShiftFinancialEngine.VarianceStatus.HARD_BLOCK_SUPERVISOR_REQUIRED -> varianceReason.isNotBlank() && supervisorPin.length >= 4
                }

                Button(
                    onClick = {
                        val draft = ShiftCloseDraft(
                            shiftId = shift.id,
                            countedCashInCents = countedCashCents,
                            denominationCounts = denominationCounts,
                            onlineDeclarations = onlineDeclarations.toList(),
                            creditNotes = creditNotes.toList(),
                            closingFloatInCents = closingFloatCents,
                            depositAmountInCents = depositAmountCents,
                            varianceReason = varianceReason,
                            closingNotes = closingNotes
                        )
                        onFinalizeClose(draft)
                    },
                    enabled = canFinalize,
                    colors = ButtonDefaults.buttonColors(containerColor = PosRose)
                ) {
                    Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isNepali) "सम्पन्न गर्नुहोस् र Z-रिपोर्ट छाप्नुहोस्" else "Finalize & Print Z-Report")
                }
            }
        }
    }
}
