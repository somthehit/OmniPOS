package com.omnipos.shift

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val timeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("hh:mm a, dd MMM").withZone(ZoneId.systemDefault())

fun formatTime(epochMs: Long): String = timeFmt.format(Instant.ofEpochMilli(epochMs))

private val GreenOk = Color(0xFF1B8A4B)

@Composable
private fun ErrorText(message: String?) {
    if (message != null) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun AmountRow(label: String, value: String, bold: Boolean = false, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

// ===================== Entry gate (login / Register tab) =====================

@Composable
fun ShiftGateScreen(
    vm: ShiftGateViewModel,
    onResume: (ShiftEntity) -> Unit,
    onOpenShift: () -> Unit,
    onCloseShift: (ShiftEntity) -> Unit,
) {
    val gate by vm.gate.collectAsState()
    var showPin by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { vm.refresh() }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val g = gate) {
            null -> CircularProgressIndicator()
            is ShiftGate.Resume -> GateCard(
                "Shift in progress",
                "Opened ${formatTime(g.shift.openedAt)}" +
                    if (g.needsPinTakeover) "\nThis shift was started on another device. PIN needed to take over." else "",
                "Continue shift",
            ) { if (g.needsPinTakeover) showPin = true else onResume(g.shift) }

            is ShiftGate.StaleMustClose -> GateCard(
                "Previous shift is still open",
                "Opened ${formatTime(g.shift.openedAt)}. Close it before you can sell.",
                "Close previous shift",
            ) { onCloseShift(g.shift) }

            is ShiftGate.NeedsOpen -> GateCard(
                "No open shift",
                g.expectedOpening?.let { "Previous closing float: ${formatNpr(it)}" } ?: "Count your opening cash to start.",
                "Open shift",
            ) { onOpenShift() }

            is ShiftGate.TerminalInUse -> GateCard("Terminal in use", "${g.byCashierName} has an open shift on this terminal. Ask an admin for a handover.", null) {}
            is ShiftGate.CashierOpenElsewhere -> GateCard("Shift open elsewhere", "You have an open shift on terminal ${g.terminalId}.", null) {}
            is ShiftGate.Blocked -> GateCard("Cannot open a new shift", g.reason, null) {}
        }
    }

    val resume = gate as? ShiftGate.Resume
    if (showPin && resume != null) {
        PinDialog(
            error = pinError,
            onConfirm = { p ->
                scope.launch {
                    if (vm.verifyPin(p)) { showPin = false; pinError = null; onResume(resume.shift) } else pinError = "Wrong PIN."
                }
            },
            onDismiss = { showPin = false },
        )
    }
}

@Composable
private fun GateCard(title: String, body: String, action: String?, onAction: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body)
            if (action != null) Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(action) }
        }
    }
}

// ===================== Open shift =====================

@Composable
fun OpenShiftScreen(vm: OpenShiftViewModel, onDone: () -> Unit) {
    val ui by vm.ui.collectAsState()
    LaunchedEffect(ui.done) { if (ui.done) onDone() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Open shift", style = MaterialTheme.typography.headlineSmall)
        Text("Count the cash that is in the drawer right now.")
        ui.expectedOpening?.let { Text("Previous closing float: ${formatNpr(it)}", style = MaterialTheme.typography.bodyMedium) }

        DenominationCounter(ui.denominations, ui.counts, vm::setCount, totalLabel = "Opening cash")

        if (ui.gap != 0L) {
            SectionCard("Difference from previous closing float") {
                AmountRow("Difference", formatNpr(ui.gap), bold = true, color = MaterialTheme.colorScheme.error)
                OutlinedTextField(
                    value = ui.gapReason, onValueChange = vm::setGapReason,
                    label = { Text("Reason (required)") }, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (ui.total == 0L) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = ui.zeroConfirmed, onCheckedChange = vm::setZeroConfirmed)
                Text("Opening cash is Rs 0. I confirm.")
            }
        }
        ErrorText(ui.error)
        Button(onClick = vm::requestStart, enabled = !ui.submitting, modifier = Modifier.fillMaxWidth()) {
            Text("Start shift (${formatNpr(ui.total)})")
        }
    }
    if (ui.showPin) PinDialog(error = ui.error, busy = ui.submitting, onConfirm = vm::confirmWithPin, onDismiss = vm::dismissPin)
}

// ===================== Active shift =====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveShiftScreen(vm: ActiveShiftViewModel, onCloseShift: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val movements by vm.movements.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    val shift = ui.shift

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (shift == null) { CircularProgressIndicator(); return@Column }

        SectionCard("Shift OPEN") {
            Text("${shift.cashierName} · Terminal ${shift.terminalId} · since ${formatTime(shift.openedAt)}")
            AmountRow("Opening cash", formatNpr(shift.openingCash))
            if (ui.unsyncedWarning) Text("${ui.sales.unsyncedCount} transactions not synced yet", color = MaterialTheme.colorScheme.tertiary)
        }
        SectionCard("Sales by payment type") {
            AmountRow("Cash sales", formatNpr(ui.sales.cashSales))
            AmountRow("Online / QR / card", formatNpr(ui.sales.posByMethod.values.sum()))
            AmountRow("Credit sales", formatNpr(ui.sales.creditSales))
            AmountRow("Credit collections (cash)", formatNpr(ui.sales.creditCollectionsCash))
        }
        SectionCard("Returns") {
            AmountRow("Credit notes issued", "${ui.sales.creditNote.issuedCount} · ${formatNpr(ui.sales.creditNote.issuedTotal)}")
            AmountRow("Cash refunded (leaves drawer)", formatNpr(ui.sales.cashRefunds))
        }
        SectionCard("Drawer") {
            val expected = ui.expectedPreview
            if (expected != null) AmountRow("Expected drawer cash", formatNpr(expected), bold = true)
            else Text("Expected cash is hidden until you submit your count (blind close).")
        }
        if (movements.isNotEmpty()) {
            SectionCard("Cash movements") {
                movements.forEach { AmountRow("${it.type.replace('_', ' ')} · ${it.reason}", formatNpr(it.amount)) }
            }
        }
        ErrorText(ui.error)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showSheet = true }, modifier = Modifier.weight(1f)) { Text("Cash in / out") }
            Button(onClick = onCloseShift, modifier = Modifier.weight(1f)) { Text("Close shift") }
        }
    }

    if (showSheet) {
        CashMovementSheet(
            error = ui.error,
            onDismiss = { showSheet = false; vm.clearError() },
            onSave = { type, amount, reason -> vm.addMovement(type, amount, reason) { showSheet = false } },
        )
    }
}

private val ActiveShiftUi.unsyncedWarning: Boolean get() = sales.unsyncedCount > 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashMovementSheet(
    error: String?,
    onDismiss: () -> Unit,
    onSave: (MovementType, String, String) -> Unit,
) {
    var type by remember { mutableStateOf(MovementType.PAID_OUT) }
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Cash movement", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MovementType.entries.forEach {
                    FilterChip(selected = type == it, onClick = { type = it }, label = { Text(it.name.replace('_', ' ')) })
                }
            }
            OutlinedTextField(
                value = amount, onValueChange = { amount = it }, label = { Text("Amount (Rs)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = reason, onValueChange = { reason = it }, label = { Text("Reason (required)") },
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(error)
            Button(onClick = { onSave(type, amount, reason) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}

// ===================== Close shift: 4-step wizard =====================

@Composable
fun CloseShiftScreen(
    vm: CloseShiftViewModel,
    onFinished: () -> Unit,
    onZReport: (ShiftEntity) -> Unit,      // hook to your existing PDF / thermal print code
) {
    val ui by vm.ui.collectAsState()

    ui.closedShift?.let { closed ->
        ShiftClosedContent(closed, onZReport = { onZReport(closed) }, onDone = onFinished)
        return
    }

    Scaffold(
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ui.step > 1) OutlinedButton(onClick = vm::back, modifier = Modifier.weight(1f)) { Text("Back") }
                if (ui.step < 4) Button(onClick = vm::next, enabled = !ui.loading, modifier = Modifier.weight(1f)) { Text("Next") }
                else Button(onClick = vm::requestClose, enabled = !ui.submitting, modifier = Modifier.weight(1f)) { Text("Close shift") }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val titles = listOf("Summary", "Count cash", "Online & credit notes", "Review")
            Text("Step ${ui.step} of 4 · ${titles[ui.step - 1]}", style = MaterialTheme.typography.titleLarge)

            val summary = ui.summary
            if (ui.loading || summary == null) {
                if (!ui.loading) ErrorText(ui.error) else CircularProgressIndicator()
            } else {
                when (ui.step) {
                    1 -> SummaryStep(ui, summary)
                    2 -> CountStep(ui, vm)
                    3 -> DeclarationsStep(ui, summary, vm)
                    else -> ReviewStep(ui, summary, vm)
                }
                ErrorText(ui.error)
            }
        }
    }
    if (ui.showPin) PinDialog("Enter PIN to close shift", ui.error, ui.submitting, vm::confirmWithPin, vm::dismissPin)
}

@Composable
private fun SummaryStep(ui: CloseUi, s: CloseSummary) {
    val t = s.totals
    if (ui.policy.blindClose) {
        Text("Blind close is on: cash figures stay hidden until you submit your count.")
    } else {
        SectionCard("Cash") {
            AmountRow("Opening cash", formatNpr(t.openingCash))
            AmountRow("+ Cash sales", formatNpr(t.cashSales))
            AmountRow("+ Credit collections (cash)", formatNpr(t.creditCollectionsCash))
            AmountRow("+ Paid in", formatNpr(t.paidIn))
            AmountRow("− Cash refunds", formatNpr(t.cashRefunds))
            AmountRow("− Paid out", formatNpr(t.paidOut))
            AmountRow("− Safe drops", formatNpr(t.safeDrops))
            AmountRow("Expected cash", formatNpr(t.expectedCash), bold = true)
        }
    }
    SectionCard("Online / QR / card (POS recorded)") {
        if (s.methodRows.isEmpty()) Text("No online payments this shift.")
        s.methodRows.forEach { AmountRow(it.method.name, formatNpr(it.posAmount)) }
    }
    SectionCard("Other") {
        AmountRow("Credit sales", formatNpr(s.sales.creditSales))
        AmountRow("Credit notes issued", "${s.sales.creditNote.issuedCount} · ${formatNpr(s.sales.creditNote.issuedTotal)}")
        if (s.sales.unsyncedCount > 0) Text("${s.sales.unsyncedCount} transactions are not synced yet.", color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun CountStep(ui: CloseUi, vm: CloseShiftViewModel) {
    Text("Count every note and coin in the drawer.")
    DenominationCounter(ui.policy.denominations, ui.counts, vm::setCount, totalLabel = "Counted cash")
}

@Composable
private fun DeclarationsStep(ui: CloseUi, s: CloseSummary, vm: CloseShiftViewModel) {
    if (s.methodRows.isEmpty()) Text("No online payments to reconcile.")
    s.methodRows.forEach { row ->
        SectionCard(row.method.name) {
            AmountRow("POS recorded", formatNpr(row.posAmount))
            if (row.providerAmount != null) {
                AmountRow("Provider confirmed (verified)", formatNpr(row.providerAmount))
            } else {
                Text("No provider connection: your entry is saved as “Declared (unverified)”.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = ui.declaredText[row.method].orEmpty(), onValueChange = { vm.setDeclared(row.method, it) },
                    label = { Text("Amount received (Rs)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = ui.references[row.method].orEmpty(), onValueChange = { vm.setReference(row.method, it) },
                    label = { Text("Transaction reference") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
            val diff = ui.mismatchFor(row)
            if (diff != null && diff != 0L) {
                AmountRow("Mismatch", formatNpr(diff), bold = true, color = MaterialTheme.colorScheme.error)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReasonCode.entries.forEach {
                        FilterChip(
                            selected = ui.reasonCodes[row.method] == it,
                            onClick = { vm.setReasonCode(row.method, it) },
                            label = { Text(it.name.replace('_', ' ')) },
                        )
                    }
                }
            }
        }
    }
    // Credit notes and refunds: three separate lines. Only the cash portion touches the drawer.
    val cn = s.sales.creditNote
    SectionCard("Credit notes & refunds (from system records)") {
        AmountRow("Credit notes issued", "${cn.issuedCount} · ${formatNpr(cn.issuedTotal)}")
        AmountRow("Cash refunded (reduces drawer cash)", formatNpr(cn.cashRefund), bold = true)
        AmountRow("Applied to customer balance", formatNpr(cn.customerBalance))
        AmountRow("Refunded via bank / wallet", formatNpr(cn.bankRefund))
        AmountRow("Credit notes redeemed as payment", formatNpr(cn.redeemedAsTender))
        Text("A credit note alone does not reduce the drawer. Only the cash refund does.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReviewStep(ui: CloseUi, s: CloseSummary, vm: CloseShiftViewModel) {
    val expected = ui.expected ?: 0L
    val variance = ui.variance ?: 0L
    val varColor = when {
        variance == 0L -> GreenOk
        ShiftMath.exceedsThreshold(variance, expected, ui.policy) -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    SectionCard("Cash") {
        AmountRow("Expected", formatNpr(expected))
        AmountRow("Counted", formatNpr(ui.counted))
        AmountRow(
            if (variance < 0) "Variance (short)" else if (variance > 0) "Variance (over)" else "Variance",
            formatNpr(variance), bold = true, color = varColor,
        )
        AmountRow("Net cash change (closing − opening)", formatNpr(ShiftMath.netCashChange(ui.counted, s.shift.openingCash)))
    }
    if (variance != 0L) {
        OutlinedTextField(
            value = ui.varianceReason, onValueChange = vm::setVarianceReason,
            label = { Text("Reason for the difference (required)") }, modifier = Modifier.fillMaxWidth(),
        )
        if (ShiftMath.exceedsThreshold(variance, expected, ui.policy)) {
            Text("This is above the allowed limit. The shift will be flagged for admin review.", color = MaterialTheme.colorScheme.error)
        }
    }
    SectionCard("Online mismatches") {
        val rows = s.methodRows.filter { (ui.mismatchFor(it) ?: 0L) != 0L }
        if (rows.isEmpty()) Text("None") else rows.forEach { AmountRow(it.method.name, formatNpr(ui.mismatchFor(it) ?: 0L), color = MaterialTheme.colorScheme.error) }
    }
    SectionCard("Cash handover") {
        OutlinedTextField(
            value = ui.closingFloatText, onValueChange = vm::setClosingFloat,
            label = { Text("Closing float left in drawer (Rs)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
        )
        AmountRow("Deposit to safe / bank", formatNpr((ui.counted - ui.closingFloat).coerceAtLeast(0)), bold = true)
        Text("The closing float becomes the next shift's expected opening cash.", style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ShiftClosedContent(shift: ShiftEntity, onZReport: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
        Text("Shift closed", style = MaterialTheme.typography.headlineMedium)
        Text(
            when (shift.statusEnum) {
                ShiftStatus.FLAGGED -> "Flagged for admin review."
                ShiftStatus.APPROVED -> "Approved."
                else -> "Waiting for admin review."
            },
        )
        AmountRow("Counted cash", formatNpr(shift.countedCash ?: 0))
        AmountRow("Closing float", formatNpr(shift.closingFloat ?: 0))
        AmountRow("Deposit", formatNpr(shift.depositAmount ?: 0))
        if (!shift.synced) Text("Saved on this device. It will sync when you are online.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = onZReport, modifier = Modifier.fillMaxWidth()) { Text("Share / print Z-report") }
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
