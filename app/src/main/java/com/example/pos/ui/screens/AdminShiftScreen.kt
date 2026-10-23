package com.example.pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.pos.data.local.ShiftEntity
import com.example.pos.data.model.ShiftDetails
import com.example.pos.domain.CartCalculator
import com.example.pos.domain.ShiftFinancialEngine
import com.example.pos.ui.PosViewModel
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
fun AdminShiftScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings
    val allShifts by viewModel.allShifts.collectAsState()
    val org by viewModel.organization.collectAsState()
    val currency = org?.currencySymbol ?: "Rs"
    val selectedDetails by viewModel.selectedShiftDetails.collectAsState()

    var statusFilter by remember { mutableStateOf<String?>(null) }
    var viewingShiftId by remember { mutableStateOf<String?>(null) }

    // Dialog state for admin actions
    var actionType by remember { mutableStateOf<String?>(null) } // "APPROVE", "FLAG", "FORCE_CLOSE", "REOPEN", "ADJUST"
    var actionShiftId by remember { mutableStateOf<String?>(null) }
    var actionInputText by remember { mutableStateOf("") }
    var actionAmountText by remember { mutableStateOf("") }

    val filteredShifts = remember(allShifts, statusFilter) {
        if (statusFilter == null) allShifts
        else allShifts.filter { it.status == statusFilter || it.reviewStatus == statusFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("admin_shift_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = strings.adminShiftTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = strings.adminShiftSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                null to "${strings.shiftStatusAll} (${allShifts.size})",
                "OPEN" to strings.shiftStatusOpen,
                "CLOSING" to (if (isNepali) "बन्द प्रक्रिया" else "Closing Draft"),
                "PENDING_REVIEW" to (if (isNepali) "प्रतीक्षित समीक्षा" else "Pending Review"),
                "FLAGGED" to strings.shiftStatusFlagged,
                "APPROVED" to strings.shiftStatusApproved,
                "FORCE_CLOSED" to strings.shiftStatusForced
            ).forEach { (status, label) ->
                val selected = statusFilter == status
                FilterChip(
                    selected = selected,
                    onClick = { statusFilter = status },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Shifts List
        if (filteredShifts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No shifts found matching the selected filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredShifts, key = { it.id }) { shift ->
                    ShiftCard(
                        shift = shift,
                        currencySymbol = currency,
                        onClick = {
                            viewingShiftId = shift.id
                            viewModel.loadShiftDetails(shift.id)
                        },
                        onQuickApprove = {
                            viewModel.adminReviewShift(shift.id, "APPROVED", "Approved by admin verification")
                        },
                        onQuickFlag = {
                            actionType = "FLAG"
                            actionShiftId = shift.id
                            actionInputText = ""
                        },
                        onForceClose = {
                            actionType = "FORCE_CLOSE"
                            actionShiftId = shift.id
                            actionInputText = ""
                        }
                    )
                }
            }
        }
    }

    // Detail Dialog
    if (viewingShiftId != null && selectedDetails != null) {
        val details = selectedDetails!!
        ShiftDetailDialog(
            details = details,
            currencySymbol = currency,
            onDismiss = { viewingShiftId = null },
            onApprove = {
                viewModel.adminReviewShift(details.shift.id, "APPROVED", "Approved after detail inspection")
                viewingShiftId = null
            },
            onFlag = {
                actionType = "FLAG"
                actionShiftId = details.shift.id
                actionInputText = ""
            },
            onForceClose = {
                actionType = "FORCE_CLOSE"
                actionShiftId = details.shift.id
                actionInputText = ""
            },
            onReopen = {
                actionType = "REOPEN"
                actionShiftId = details.shift.id
                actionInputText = ""
            },
            onSupervisorAdjustment = {
                actionType = "ADJUST"
                actionShiftId = details.shift.id
                actionAmountText = ""
                actionInputText = ""
            },
            onPrintZReport = {
                viewModel.reprintShiftZReport(details.shift.id)
            }
        )
    }

    // Action Confirmation Dialogs
    if (actionType != null && actionShiftId != null) {
        AlertDialog(
            onDismissRequest = { actionType = null },
            title = {
                Text(
                    when (actionType) {
                        "FLAG" -> "Flag Shift for Review"
                        "FORCE_CLOSE" -> "Force Close Active Shift"
                        "REOPEN" -> "Reopen Closed Shift"
                        "ADJUST" -> "Apply Supervisor Adjustment"
                        else -> "Confirm Action"
                    }
                )
            },
            text = {
                Column {
                    when (actionType) {
                        "FLAG" -> {
                            Text("Please enter the reason for flagging this shift:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = actionInputText,
                                onValueChange = { actionInputText = it },
                                label = { Text("Flag Note / Variance Audit Reason") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "FORCE_CLOSE" -> {
                            Text("This will immediately force close the terminal shift. Enter the justification:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = actionInputText,
                                onValueChange = { actionInputText = it },
                                label = { Text("Force Close Reason") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "REOPEN" -> {
                            Text("A snapshot of the current closing state will be archived in shift_versions. Enter the reopen reason:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = actionInputText,
                                onValueChange = { actionInputText = it },
                                label = { Text("Reopen Reason") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "ADJUST" -> {
                            Text("Enter the adjustment amount in $currency (use negative for deduction):")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = actionAmountText,
                                onValueChange = { actionAmountText = it },
                                label = { Text("Adjustment Amount ($currency)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = actionInputText,
                                onValueChange = { actionInputText = it },
                                label = { Text("Reason / Drawer Audit Note") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shiftId = actionShiftId!!
                        when (actionType) {
                            "FLAG" -> {
                                viewModel.adminReviewShift(shiftId, "FLAGGED", actionInputText)
                            }
                            "FORCE_CLOSE" -> {
                                viewModel.adminForceCloseShift(shiftId, actionInputText.ifBlank { "Force closed by admin" })
                            }
                            "REOPEN" -> {
                                viewModel.adminReopenShift(shiftId, actionInputText.ifBlank { "Reopened for correction" })
                            }
                            "ADJUST" -> {
                                val cents = ((actionAmountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                                viewModel.supervisorAdjustment(shiftId, cents, actionInputText)
                            }
                        }
                        actionType = null
                        viewingShiftId = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { actionType = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ShiftCard(
    shift: ShiftEntity,
    currencySymbol: String,
    onClick: () -> Unit,
    onQuickApprove: () -> Unit,
    onQuickFlag: () -> Unit,
    onForceClose: () -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val isLive = shift.status == "OPEN" || shift.status == "CLOSING"
    val variance = shift.varianceInCents ?: 0L

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Status badge & ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isLive -> PosEmerald
                                    shift.status == "FORCE_CLOSED" -> PosRose
                                    shift.reviewStatus == "APPROVED" -> PosEmerald
                                    shift.reviewStatus == "FLAGGED" -> PosRose
                                    else -> PosAmber
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isNepali) "शिफ्ट #${shift.id.takeLast(6).uppercase()}" else "Shift #${shift.id.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (shift.version > 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PosSky.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("v${shift.version}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PosSky)
                        }
                    }
                }

                // Review Status Tag
                val statusBg = when (shift.reviewStatus) {
                    "APPROVED" -> PosEmerald.copy(alpha = 0.15f)
                    "FLAGGED" -> PosRose.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val statusColor = when (shift.reviewStatus) {
                    "APPROVED" -> PosEmerald
                    "FLAGGED" -> PosRose
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val statusText = if (isLive) {
                        when (shift.status) {
                            "OPEN" -> strings.shiftStatusOpen
                            else -> shift.status
                        }
                    } else {
                        when (shift.reviewStatus) {
                            "APPROVED" -> strings.shiftStatusApproved
                            "FLAGGED" -> strings.shiftStatusFlagged
                            "PENDING" -> strings.pending
                            else -> shift.reviewStatus
                        }
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${strings.shiftCashier}: ${shift.cashierName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${strings.shiftOpened}: ${dateFormat.format(Date(shift.openedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (shift.closedAt != null) {
                        Text(
                            text = "${strings.shiftClosed2}: ${dateFormat.format(Date(shift.closedAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${strings.openingCash}: ${CartCalculator.formatMoney(shift.openingFloatInCents, currencySymbol)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (shift.countedCashInCents != null) {
                        Text(
                            text = "${strings.countedCash}: ${CartCalculator.formatMoney(shift.countedCashInCents, currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val varColor = if (variance == 0L) PosEmerald else if (variance > 0) PosSky else PosRose
                        Text(
                            text = "${strings.variance}: ${if (variance > 0) "+" else ""}${CartCalculator.formatMoney(variance, currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = varColor
                        )
                    }
                }
            }

            // Quick Actions Row
            if (shift.reviewStatus == "PENDING" && !isLive) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onQuickFlag,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(strings.flagShift, fontSize = 11.sp, color = PosRose)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onQuickApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = PosEmerald),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(strings.approveShift, fontSize = 11.sp)
                    }
                }
            } else if (isLive) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onForceClose,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(strings.forceClose, fontSize = 11.sp, color = PosRose)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftDetailDialog(
    details: ShiftDetails,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onFlag: () -> Unit,
    onForceClose: () -> Unit,
    onReopen: () -> Unit,
    onSupervisorAdjustment: () -> Unit,
    onPrintZReport: () -> Unit = {}
) {
    val shift = details.shift
    val isLive = shift.status == "OPEN" || shift.status == "CLOSING"
    var selectedTab by remember { mutableIntStateOf(0) }
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isNepali) "शिफ्ट #${shift.id.takeLast(6).uppercase()} विवरण" else "Shift #${shift.id.takeLast(6).uppercase()} Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.shiftCashier}: ${shift.cashierName} • ${strings.shiftTerminal} #1",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = strings.close)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Summary, Denominations, Declarations, Audit Events
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(strings.stepSummary, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(strings.stepCountCash, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(strings.onlineQrCard, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text(strings.auditLog, fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content (Scrollable)
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        0 -> SummaryTab(shift = shift, metrics = details.metrics, currencySymbol = currencySymbol)
                        1 -> CashCountsTab(counts = details.cashCounts, currencySymbol = currencySymbol)
                        2 -> OnlineDeclarationsTab(declarations = details.declarations, creditNotes = details.creditNotes, currencySymbol = currencySymbol)
                        3 -> AuditEventsTab(events = details.events, adjustments = details.adjustments, versions = details.versions)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLive) {
                        OutlinedButton(onClick = onForceClose) {
                            Text(strings.forceClose, color = PosRose)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = onPrintZReport) {
                                Icon(Icons.Default.Print, contentDescription = strings.zReport, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.print, fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = onSupervisorAdjustment) {
                                Text(strings.adjustShift, fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = onReopen) {
                                Text(strings.reopenShift, fontSize = 12.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = onFlag) {
                                Text(strings.flagShift, color = PosRose, fontSize = 12.sp)
                            }
                            Button(
                                onClick = onApprove,
                                colors = ButtonDefaults.buttonColors(containerColor = PosEmerald)
                            ) {
                                Text(strings.approveShift, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTab(
    shift: ShiftEntity,
    metrics: com.example.pos.data.model.ShiftLiveMetrics?,
    currencySymbol: String
) {
    val strings = LocalPosStrings.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRow(strings.date, shift.businessDate.ifBlank { "N/A" })
        DetailRow(strings.shiftOpened, dateFormat.format(Date(shift.openedAt)))
        if (shift.closedAt != null) {
            DetailRow(strings.shiftClosed2, dateFormat.format(Date(shift.closedAt)))
        }
        DetailRow(strings.openingCash, CartCalculator.formatMoney(shift.openingFloatInCents, currencySymbol))
        if (shift.openingGapInCents != 0L) {
            DetailRow(strings.differenceAmount, CartCalculator.formatMoney(shift.openingGapInCents, currencySymbol))
            DetailRow(strings.reason, shift.openingGapReason)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        if (shift.expectedCashInCents != null) {
            DetailRow(strings.expectedCashRow, CartCalculator.formatMoney(shift.expectedCashInCents, currencySymbol))
        }
        if (shift.countedCashInCents != null) {
            DetailRow(strings.countedCash, CartCalculator.formatMoney(shift.countedCashInCents, currencySymbol))
        }
        if (shift.varianceInCents != null) {
            val v = shift.varianceInCents
            DetailRow(strings.variance, "${if (v > 0) "+" else ""}${CartCalculator.formatMoney(v, currencySymbol)}")
        }
        if (shift.closingFloatInCents > 0) {
            DetailRow(strings.closingFloat, CartCalculator.formatMoney(shift.closingFloatInCents, currencySymbol))
        }
        if (shift.depositAmountInCents > 0) {
            DetailRow(strings.deposit, CartCalculator.formatMoney(shift.depositAmountInCents, currencySymbol))
        }
        if (shift.varianceReason.isNotBlank()) {
            DetailRow(strings.reasonForDifference, shift.varianceReason)
        }
        if (shift.notes.isNotBlank()) {
            DetailRow(strings.note, shift.notes)
        }
    }
}

@Composable
private fun CashCountsTab(
    counts: List<com.example.pos.data.local.ShiftCashCountEntity>,
    currencySymbol: String
) {
    val strings = LocalPosStrings.current
    if (counts.isEmpty()) {
        Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            counts.forEach { c ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$currencySymbol ${c.denomination}  x  ${c.count}", fontWeight = FontWeight.SemiBold)
                    Text(CartCalculator.formatMoney(c.subtotalInCents, currencySymbol), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OnlineDeclarationsTab(
    declarations: List<com.example.pos.data.local.ShiftDeclarationEntity>,
    creditNotes: List<com.example.pos.data.local.ShiftCreditNoteEntity>,
    currencySymbol: String
) {
    val strings = LocalPosStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(strings.onlineQrCardSection, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        if (declarations.isEmpty()) {
            Text(strings.noOnlinePayments, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        } else {
            declarations.forEach { d ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(d.provider, fontWeight = FontWeight.SemiBold)
                        Text("${d.slipCount} slips", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(CartCalculator.formatMoney(d.declaredAmountInCents, currencySymbol), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (creditNotes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider()
            Text(strings.creditNotesRefundsSection, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            creditNotes.forEach { cn ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(cn.creditNoteNo, fontWeight = FontWeight.SemiBold)
                        Text("${strings.cashRefunded}: ${CartCalculator.formatMoney(cn.cashRefundInCents, currencySymbol)}", fontSize = 11.sp)
                    }
                    Text(CartCalculator.formatMoney(cn.totalAmountInCents, currencySymbol), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AuditEventsTab(
    events: List<com.example.pos.data.local.ShiftEventEntity>,
    adjustments: List<com.example.pos.data.local.ShiftAdjustmentEntity>,
    versions: List<com.example.pos.data.local.ShiftVersionEntity>
) {
    val strings = LocalPosStrings.current
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (events.isEmpty() && adjustments.isEmpty()) {
            Text(strings.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            events.forEach { e ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(e.eventType, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(dateFormat.format(Date(e.createdAt)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (e.actorName.isNotBlank()) {
                        Text("Actor: ${e.actorName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (e.detailsJson.isNotBlank()) {
                        Text(e.detailsJson, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
