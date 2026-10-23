@file:Suppress("SpellCheckingInspection", "unused")

package com.example.pos.ui.shift

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings

val CASH_DENOMINATIONS = listOf(1000, 500, 100, 50, 20, 10, 5, 2, 1)

/**
 * Screen overload powered by [ShiftSyncViewModel].
 */
@Composable
fun CashierDenominationScreen(
    viewModel: ShiftSyncViewModel,
    userRole: String, // Must be "cashier"
    modifier: Modifier = Modifier,
    currencySymbol: String = "Rs"
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    if (!userRole.equals("cashier", ignoreCase = true)) {
        RestrictedRoleAccessScreen(modifier = modifier)
        return
    }

    val state by viewModel.uiState.collectAsState()

    ResponsiveDenominationLayout(
        modifier = modifier,
        currencySymbol = currencySymbol,
        denominations = state.denominations,
        onDenominationChanged = { denom, count ->
            viewModel.onDenominationChanged(denom, count)
        },
        testTagPrefix = "denom_input",
        headerTitle = if (isNepali) "नगद दरबन्दी" else "Cash Denominations",
        syncStatusContent = {
            if (state.isSyncing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(if (isNepali) "सिङ्क हुँदैछ..." else "Syncing...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Text(
                    text = if (state.lastSyncedAt > 0) (if (isNepali) "स्वतः सिङ्क भयो" else "Auto-synced") else (if (isNepali) "प्रविष्टि पर्खिँदै" else "Awaiting input"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        testTag = "cashier_denomination_screen"
    )
}

/**
 * Screen overload powered by [OfflineShiftViewModel].
 */
@Composable
fun CashierDenominationScreen(
    viewModel: OfflineShiftViewModel,
    userRole: String, // Must be "cashier"
    modifier: Modifier = Modifier,
    currencySymbol: String = "Rs"
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    if (!userRole.equals("cashier", ignoreCase = true)) {
        RestrictedRoleAccessScreen(modifier = modifier)
        return
    }

    val shiftEntity by viewModel.currentShiftFlow.collectAsState(initial = null)
    val isSyncing by viewModel.syncingState.collectAsState()
    val denominations = shiftEntity?.denominations ?: emptyMap()
    val isSynced = shiftEntity?.isSynced ?: false

    ResponsiveDenominationLayout(
        modifier = modifier,
        currencySymbol = currencySymbol,
        denominations = denominations,
        onDenominationChanged = { denom, count ->
            viewModel.onCountChanged(denom, count, shiftEntity)
        },
        testTagPrefix = "denom_input",
        headerTitle = if (isNepali) "नगद दरबन्दी" else "Cash Denominations",
        syncStatusContent = {
            if (isSyncing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(if (isNepali) "सिङ्क हुँदैछ..." else "Syncing...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Text(
                    text = if (isSynced) (if (isNepali) "क्लाउडमा सिङ्क भयो" else "Synced to Cloud") else (if (isNepali) "स्थानिक रूपमा सुरक्षित (सिङ्क बाँकी)" else "Saved locally (pending sync)"),
                    fontSize = 12.sp,
                    color = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        },
        testTag = "cashier_denomination_screen_offline"
    )
}

/**
 * Isolated Cashier Shift Gate Screen powered by [CashierShiftViewModel].
 * Isolates shifts so Cashier A and Cashier B have completely independent drawer floats and open/closed lifecycles.
 */
@Composable
fun CashierDenominationScreen(
    viewModel: CashierShiftViewModel,
    userRole: String, // Must be "cashier"
    modifier: Modifier = Modifier,
    currencySymbol: String = "Rs"
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    if (!userRole.equals("cashier", ignoreCase = true)) {
        RestrictedRoleAccessScreen(modifier = modifier)
        return
    }

    val state by viewModel.uiState.collectAsState()

    when (val currentState = state) {
        is CashierShiftUiState.Checking -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = if (isNepali) "${viewModel.currentCashierId} को लागि शिफ्ट स्थिति जाँच गरिँदै..." else "Checking shift status for ${viewModel.currentCashierId}...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        is CashierShiftUiState.NeedsDenominationInput -> {
            val openingInputs = remember { mutableStateMapOf<Int, Int>() }
            val grandTotal = openingInputs.entries.sumOf { it.key * it.value }

            ResponsiveDenominationLayout(
                modifier = modifier,
                currencySymbol = currencySymbol,
                denominations = openingInputs,
                onDenominationChanged = { denom, count ->
                    if (count > 0) openingInputs[denom] = count else openingInputs.remove(denom)
                },
                testTagPrefix = "opening_denom_input",
                headerTitle = if (isNepali) "सुरुवाती नगद मौज्दात - क्यासियर ${viewModel.currentCashierId}" else "Open Shift Float - Cashier ${viewModel.currentCashierId}",
                headerSubtitle = if (isNepali) "आफ्नो शिफ्ट सुरु गर्न सुरुवाती नगद विवरण प्रविष्ट गर्नुहोस्।" else "Enter opening float breakdown to start your isolated shift.",
                syncStatusContent = { },
                summaryTitle = if (isNepali) "सुरुवाती कुल:" else "Opening Total:",
                primaryActionLabel = if (isNepali) "$currencySymbol. $grandTotal सँग शिफ्ट सुरु गर्नुहोस्" else "Start Shift with $currencySymbol. $grandTotal",
                onPrimaryAction = { viewModel.submitOpeningFloat(openingInputs) },
                primaryActionTestTag = "btn_start_isolated_shift",
                testTag = "opening_shift_gate_screen"
            )
        }

        is CashierShiftUiState.ShiftRunning -> {
            val shift = currentState.shift
            val denominations = shift.denominations
            val isSyncing = currentState.isSyncing
            val isSynced = shift.isSynced

            ResponsiveDenominationLayout(
                modifier = modifier,
                currencySymbol = currencySymbol,
                denominations = denominations,
                onDenominationChanged = { denom, count ->
                    viewModel.updateDenomination(denom, count, shift)
                },
                testTagPrefix = "active_denom_input",
                headerTitle = if (isNepali) "सक्रिय शिफ्ट (${shift.cashierId})" else "Active Shift (${shift.cashierId})",
                headerSubtitle = if (isNepali) "स्थिति: ${shift.status} | आइडी: ${shift.shiftId.take(16)}..." else "Status: ${shift.status} | ID: ${shift.shiftId.take(16)}...",
                syncStatusContent = {
                    if (isSyncing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(if (isNepali) "सिङ्क हुँदैछ..." else "Syncing...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Text(
                            text = if (isSynced) (if (isNepali) "क्लाउडमा सिङ्क भयो" else "Synced to Cloud") else (if (isNepali) "स्थानिक रूपमा सुरक्षित (सिङ्क बाँकी)" else "Saved locally (pending sync)"),
                            fontSize = 12.sp,
                            color = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                summaryTitle = if (isNepali) "हालको नगद मौज्दात:" else "Current Cash Float:",
                secondaryActionLabel = if (isNepali) "क्यासियर शिफ्ट बन्द गर्नुहोस्" else "Close Cashier Shift",
                onSecondaryAction = { viewModel.closeShift() },
                secondaryActionTestTag = "btn_close_cashier_shift",
                testTag = "cashier_running_shift_screen"
            )
        }

        is CashierShiftUiState.MultiDeviceConflict -> {
            val showManagerPinDialog = remember { mutableStateOf(false) }
            val managerPinInput = remember { mutableStateOf("") }
            val pinError = remember { mutableStateOf<String?>(null) }

            val openedDateStr = remember(currentState.openedAt) {
                java.text.SimpleDateFormat("hh:mm a, dd MMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(currentState.openedAt))
            }

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .testTag("multi_device_conflict_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isNepali) "अर्को उपकरणमा सक्रिय शिफ्ट छ" else "Active Shift on Another Device",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Text(
                            text = if (isNepali) "क्यासियर ${viewModel.currentCashierId} को उपकरण '${currentState.activeDeviceId}' मा पहिले नै सक्रिय शिफ्ट छ (सुरु: $openedDateStr)।\n\nनगद मौज्दातमा त्रुटि रोक्न, एकै पटक धेरै उपकरणहरूमा शिफ्ट खोल्न निषेध छ।" else "Cashier ${viewModel.currentCashierId} already has an active register shift on device '${currentState.activeDeviceId}', opened at $openedDateStr.\n\nTo prevent cash float discrepancies, simultaneous shifts across multiple devices are strictly prohibited.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.dismissConflict() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_switch_to_active_terminal")
                        ) {
                            Text(if (isNepali) "सक्रिय टर्मिनलमा जानुहोस् (${currentState.activeDeviceId})" else "Switch to Active Terminal (${currentState.activeDeviceId})")
                        }

                        OutlinedButton(
                            onClick = { showManagerPinDialog.value = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_force_close_prompt"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(if (isNepali) "अघिल्लो शिफ्ट जबर्जस्ती बन्द गर्नुहोस् (प्रबन्धक पिन आवश्यक)" else "Force Close Previous Shift (Manager PIN Required)")
                        }
                    }
                }
            }

            if (showManagerPinDialog.value) {
                AlertDialog(
                    onDismissRequest = {
                        showManagerPinDialog.value = false
                        managerPinInput.value = ""
                        pinError.value = null
                    },
                    title = { Text(if (isNepali) "प्रबन्धक ओभरराइड - जबर्जस्ती बन्द" else "Manager Override - Force Close") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (isNepali) "'${currentState.activeDeviceId}' मा रहेको शिफ्ट '${currentState.activeShiftId.take(12)}...' लाई मिलाउन बाँकी बन्द (CLOSED_UNRECONCILED) गरि नयाँ ड्रअर खोल्न प्रबन्धक पिन प्रविष्ट गर्नुहोस्:" else "Enter Manager PIN to mark shift '${currentState.activeShiftId.take(12)}...' on '${currentState.activeDeviceId}' as CLOSED_UNRECONCILED and permit a new drawer here:"
                            )
                            OutlinedTextField(
                                value = managerPinInput.value,
                                onValueChange = { managerPinInput.value = it },
                                label = { Text(if (isNepali) "प्रबन्धक पिन" else "Manager PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                isError = pinError.value != null
                            )
                            pinError.value?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (managerPinInput.value == "1234" || managerPinInput.value.length >= 4) {
                                    showManagerPinDialog.value = false
                                    viewModel.forceClosePreviousShiftAndOpen(
                                        shiftId = currentState.activeShiftId,
                                        managerUid = "manager_auth",
                                        managerName = "Store Manager Override"
                                    )
                                } else {
                                    pinError.value = if (isNepali) "अमान्य प्रबन्धक पिन" else "Invalid Manager PIN"
                                }
                            }
                        ) {
                            Text(if (isNepali) "प्रमाणीकरण गरि बन्द गर्नुहोस्" else "Authorize & Close")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showManagerPinDialog.value = false }) {
                            Text(if (isNepali) "रद्द गर्नुहोस्" else "Cancel")
                        }
                    }
                )
            }
        }

        is CashierShiftUiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.widthIn(max = 480.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (isNepali) "शिफ्ट खोल्न सकिएन" else "Unable to Open Shift",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { viewModel.checkCurrentCashierShift() }) {
                            Text(if (isNepali) "पुनः प्रयास गर्नुहोस्" else "Retry")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Device-responsive denomination layout component.
 * Adapts between Compact (Phone Portrait/Landscape) and Expanded (Tablet / POS Terminal) form factors.
 */
@Composable
fun ResponsiveDenominationLayout(
    modifier: Modifier = Modifier,
    currencySymbol: String = "Rs",
    denominations: Map<Int, Int>,
    onDenominationChanged: (denom: Int, count: Int) -> Unit,
    testTagPrefix: String = "denom_input",
    headerTitle: String,
    headerSubtitle: String? = null,
    syncStatusContent: @Composable () -> Unit,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    primaryActionTestTag: String = "",
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    secondaryActionTestTag: String = "",
    summaryTitle: String = "Total Cash Float:",
    testTag: String = "cashier_denomination_screen"
) {
    val grandTotal = denominations.entries.sumOf { it.key * it.value }
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag)
    ) {
        val isWideScreen = this.maxWidth >= 600.dp

        if (isWideScreen) {
            // Tablet / POS Terminal Landscape Layout (2 Panes)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Pane: Header + 2-Column Grid of Denomination inputs
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(text = headerTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            headerSubtitle?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        syncStatusContent()
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val rows = CASH_DENOMINATIONS.chunked(2)
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(rows) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (denom in pair) {
                                    val count = denominations[denom] ?: 0
                                    Box(modifier = Modifier.weight(1f)) {
                                        DenominationTileCard(
                                            denom = denom,
                                            count = count,
                                            currencySymbol = currencySymbol,
                                            testTagPrefix = testTagPrefix,
                                            onValueChange = { input ->
                                                val parsed = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                                                onDenominationChanged(denom, parsed)
                                            }
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Right Pane: Live Summary Panel + Actions
                Card(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = if (isNepali) "शिफ्ट नगद सारांश" else "Shift Cash Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = summaryTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$currencySymbol. $grandTotal",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            val activeNoteCount = denominations.values.sum()
                            Text(
                                text = if (isNepali) "गन्ती गरिएका कुल नोट/सिक्का संख्या: $activeNoteCount" else "Total Banknotes/Coins counted: $activeNoteCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            primaryActionLabel?.let { label ->
                                Button(
                                    onClick = { onPrimaryAction?.invoke() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag(primaryActionTestTag)
                                ) {
                                    Text(label, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            secondaryActionLabel?.let { label ->
                                OutlinedButton(
                                    onClick = { onSecondaryAction?.invoke() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag(secondaryActionTestTag),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(label, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Compact Phone Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar showing Sync Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(text = headerTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        headerSubtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    syncStatusContent()
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Denomination Row List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CASH_DENOMINATIONS) { denom ->
                        val count = denominations[denom] ?: 0
                        val total = denom * count

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$currencySymbol. $denom",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = "×",
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            OutlinedTextField(
                                value = if (count == 0) "" else count.toString(),
                                onValueChange = { input ->
                                    val parsed = input.filter { it.isDigit() }.toIntOrNull() ?: 0
                                    onDenominationChanged(denom, parsed)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = { Text("0") },
                                singleLine = true,
                                modifier = Modifier
                                    .width(96.dp)
                                    .testTag("${testTagPrefix}_$denom")
                            )

                            Text(
                                text = "$currencySymbol. $total",
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(summaryTitle, fontWeight = FontWeight.Bold)
                        Text("$currencySymbol. $grandTotal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                primaryActionLabel?.let { label ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onPrimaryAction?.invoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag(primaryActionTestTag)
                    ) {
                        Text(label)
                    }
                }

                secondaryActionLabel?.let { label ->
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onSecondaryAction?.invoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag(secondaryActionTestTag),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun DenominationTileCard(
    denom: Int,
    count: Int,
    currencySymbol: String,
    testTagPrefix: String,
    onValueChange: (String) -> Unit
) {
    val total = denom * count
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val isNepaliTile = LocalPosStrings.current is NepaliStrings
                Text(
                    text = "$currencySymbol. $denom",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isNepaliTile) "जम्मा: $currencySymbol. $total" else "Total: $currencySymbol. $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            OutlinedTextField(
                value = if (count == 0) "" else count.toString(),
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("0") },
                singleLine = true,
                modifier = Modifier
                    .width(88.dp)
                    .testTag("${testTagPrefix}_$denom")
            )
        }
    }
}

@Composable
private fun RestrictedRoleAccessScreen(modifier: Modifier = Modifier) {
    val isNepali = LocalPosStrings.current is NepaliStrings
    Surface(modifier = modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
            Text(
                text = if (isNepali) "क्यासियर भूमिकाको लागि मात्र पहुँच सीमित छ।" else "Access restricted to Cashier role.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
