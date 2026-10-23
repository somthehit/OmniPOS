package com.example.pos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pos.data.local.CustomerEntity
import com.example.pos.data.model.PaymentEntry
import com.example.pos.data.model.PaymentType
import com.example.pos.domain.CartCalculator
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import com.example.pos.ui.screens.CustomerPickerDialog
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.Slate900
import java.util.Locale

/**
 * Smart dynamic quick cash helper algorithm for Nepali currency notes (10, 20, 50, 100, 500, 1000).
 * Generates logical tender suggestions strictly greater than totalToPay.
 */
fun getSmartQuickCashAmounts(totalInCents: Long): List<Long> {
    if (totalInCents <= 0L) return emptyList()
    val totalRupees = totalInCents / 100L
    val remainderCents = totalInCents % 100L
    val baseRupees = if (remainderCents > 0L) totalRupees + 1L else totalRupees

    val suggestions = mutableSetOf<Long>()
    val standardNotes = listOf(10L, 20L, 50L, 100L, 500L, 1000L, 2000L, 5000L)

    val round10 = ((baseRupees + 9L) / 10L) * 10L
    val round50 = ((baseRupees + 49L) / 50L) * 50L
    val round100 = ((baseRupees + 99L) / 100L) * 100L
    val round500 = ((baseRupees + 499L) / 500L) * 500L
    val round1000 = ((baseRupees + 999L) / 1000L) * 1000L

    val candidates = listOf(round10, round50, round100, round500, round1000)
    for (candidateNote in candidates) {
        if (candidateNote > baseRupees && suggestions.size < 4) {
            suggestions.add(candidateNote)
        }
    }

    for (note in standardNotes) {
        if (note > baseRupees && suggestions.size < 5) {
            suggestions.add(note)
        }
    }

    return suggestions.sorted().take(4)
}

@Composable
fun CheckoutDialog(
    grandTotalInCents: Long,
    currencySymbol: String,
    selectedCustomer: CustomerEntity?,
    customers: List<CustomerEntity> = emptyList(),
    onDismiss: () -> Unit,
    onCompletePayment: (List<PaymentEntry>) -> Unit,
    onSelectCustomer: ((CustomerEntity) -> Unit)? = null,
    onAddNewCustomer: (() -> Unit)? = null
) {
    val isNepali = LocalPosStrings.current is  NepaliStrings
    var selectedTab by remember { mutableStateOf(PaymentType.CASH) }
    var showCustomerPickerInDialog by remember { mutableStateOf(false) }

    // Clean cash input initialization (never raw 160.0)
    var cashTenderedText by remember(grandTotalInCents) {
        mutableStateOf(
            if (grandTotalInCents % 100L == 0L) {
                (grandTotalInCents / 100L).toString()
            } else {
                String.format(Locale.US, "%.2f", grandTotalInCents / 100.0)
            }
        )
    }

    // Optional partial cash text on Credit tab
    var creditPartialCashText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        val strings = LocalPosStrings.current
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .testTag("checkout_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Text(
                    text = strings.completePayment,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = strings.totalToPay(CartCalculator.formatMoney(grandTotalInCents, currencySymbol)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Method Selector
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    PaymentType.entries.forEach { type ->
                        Tab(
                            selected = selectedTab == type,
                            onClick = { selectedTab = type },
                            text = {
                                Text(
                                    text = when (type) {
                                        PaymentType.CASH -> strings.cash
                                        PaymentType.CARD -> strings.card
                                        PaymentType.QR_WALLET -> strings.qrWalletTab
                                        PaymentType.CREDIT_DUE -> strings.creditDueTab
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedTab == type) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val tenderedCents = ((cashTenderedText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                val isCashSufficient = tenderedCents >= grandTotalInCents
                val cashShortfallCents = (grandTotalInCents - tenderedCents).coerceAtLeast(0L)
                val cashChangeCents = CartCalculator.calculateChange(tenderedCents, grandTotalInCents)

                val customerAvailableCredit = selectedCustomer?.let {
                    (it.creditLimitInCents - it.currentDueInCents).coerceAtLeast(0L)
                } ?: 0L

                val isSplitCashCreditValid = (isCashSufficient || tenderedCents <= 0L) || (selectedCustomer != null && cashShortfallCents <= customerAvailableCredit)

                // Tab Content
                when (selectedTab) {
                    PaymentType.CASH -> {
                        Column {
                            OutlinedTextField(
                                value = cashTenderedText,
                                onValueChange = { cashTenderedText = it },
                                label = { Text(strings.cashTendered(currencySymbol)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    if (cashTenderedText.isNotEmpty()) {
                                        IconButton(onClick = { cashTenderedText = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = if (isNepali) "खाली गर्नुहोस्" else "Clear", tint = PosRose)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cash_tendered_input"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Smart Dynamic Quick Cash Chips (Nepali Note Denominations)
                            Text(
                                text = strings.quickCashLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            val exactAmountRupees = if (grandTotalInCents % 100L == 0L) (grandTotalInCents / 100L).toString() else String.format(Locale.US, "%.2f", grandTotalInCents / 100.0)
                            val smartSuggestions = remember(grandTotalInCents) { getSmartQuickCashAmounts(grandTotalInCents) }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                // First Chip: Exact
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, PosPrimary.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable { cashTenderedText = exactAmountRupees }
                                    ) {
                                        Text(
                                            text = strings.exactAmount(currencySymbol, exactAmountRupees),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                // Smart Suggestion Chips
                                items(smartSuggestions) { amtRupees ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.clickable { cashTenderedText = amtRupees.toString() }
                                    ) {
                                        Text(
                                            text = "$currencySymbol. $amtRupees",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Change Return / Remaining Due Card with proper spacing
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCashSufficient) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                                ),
                                border = BorderStroke(1.dp, if (isCashSufficient) PosEmerald.copy(alpha = 0.4f) else PosRose.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isCashSufficient) strings.changeReturn else strings.remainingDue,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCashSufficient) Color(0xFF065F46) else PosRose
                                    )
                                    Text(
                                        text = CartCalculator.formatMoney(
                                            if (isCashSufficient) cashChangeCents else (grandTotalInCents - tenderedCents),
                                            currencySymbol
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isCashSufficient) PosEmerald else PosRose
                                    )
                                }
                            }
                        }
                    }

                    PaymentType.CARD -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = strings.card,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.cardTerminalReady,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = strings.cardAmount(CartCalculator.formatMoney(grandTotalInCents, currencySymbol)),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    PaymentType.QR_WALLET -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = strings.qrPayment,
                                tint = PosEmerald,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.qrWalletReady,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = strings.qrScanInstruction,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    PaymentType.CREDIT_DUE -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (selectedCustomer == null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                    border = BorderStroke(1.dp, PosRose.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = PosRose, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = strings.attachCustomerForCredit,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = PosRose
                                            )
                                        }

                                        PosCompactPillButton(
                                            text = strings.attachCustomerButton,
                                            icon = Icons.Default.Person,
                                            gradient = PosButtonGradients.Emerald,
                                            onClick = { showCustomerPickerInDialog = true }
                                        )
                                    }
                                }
                            } else {
                                val availableCredit = (selectedCustomer.creditLimitInCents - selectedCustomer.currentDueInCents).coerceAtLeast(0L)
                                val isOverLimit = grandTotalInCents > availableCredit

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isOverLimit) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, if (isOverLimit) PosRose.copy(alpha = 0.4f) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = strings.customerLabel(selectedCustomer.name),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(strings.creditLimitLabel, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                                            Text(CartCalculator.formatMoney(selectedCustomer.creditLimitInCents, currencySymbol), fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(strings.existingDueLabel, style = MaterialTheme.typography.bodySmall, color = PosRose)
                                            Text(CartCalculator.formatMoney(selectedCustomer.currentDueInCents, currencySymbol), fontWeight = FontWeight.Bold, color = PosRose)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(strings.availableCreditLabel, style = MaterialTheme.typography.bodySmall, color = if (isOverLimit) PosRose else PosEmerald)
                                            Text(CartCalculator.formatMoney(availableCredit, currencySymbol), fontWeight = FontWeight.Bold, color = if (isOverLimit) PosRose else PosEmerald)
                                        }

                                        if (isOverLimit) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = strings.creditLimitExceeded(CartCalculator.formatMoney(availableCredit, currencySymbol)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PosRose,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Validation
                val isCreditTabValid = selectedTab != PaymentType.CREDIT_DUE || (selectedCustomer != null && customerAvailableCredit >= (grandTotalInCents - ((creditPartialCashText.toDoubleOrNull() ?: 0.0) * 100).toLong().coerceIn(0L, grandTotalInCents)))
                val isCashTabValid = selectedTab != PaymentType.CASH || isCashSufficient || (selectedCustomer != null && isSplitCashCreditValid)
                val isFormValid = isCashTabValid && isCreditTabValid

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PosOutlineButton(
                        text = strings.cancelButton,
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        height = 50.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    PosGradientButton(
                        text = strings.completeSale,
                        subtitle = "${CartCalculator.formatMoney(grandTotalInCents, currencySymbol)} • ${
                            when (selectedTab) {
                                PaymentType.CASH -> strings.cash
                                PaymentType.CARD -> strings.card
                                PaymentType.QR_WALLET -> strings.qrPayment
                                PaymentType.CREDIT_DUE -> strings.creditDueTab
                            }
                        }",
                        onClick = {
                            val payments = when (selectedTab) {
                                PaymentType.CASH -> {
                                    if (isCashSufficient) {
                                        listOf(
                                            PaymentEntry(
                                                type = PaymentType.CASH,
                                                amountInCents = grandTotalInCents,
                                                changeGivenInCents = cashChangeCents
                                            )
                                        )
                                    } else {
                                        listOf(
                                            PaymentEntry(
                                                type = PaymentType.CASH,
                                                amountInCents = tenderedCents,
                                                changeGivenInCents = 0L
                                            ),
                                            PaymentEntry(
                                                type = PaymentType.CREDIT_DUE,
                                                amountInCents = cashShortfallCents
                                            )
                                        )
                                    }
                                }
                                PaymentType.CARD -> {
                                    listOf(
                                        PaymentEntry(
                                            type = PaymentType.CARD,
                                            amountInCents = grandTotalInCents,
                                            referenceNo = "EMV-${System.currentTimeMillis().toString().takeLast(4)}"
                                        )
                                    )
                                }
                                PaymentType.QR_WALLET -> {
                                    listOf(
                                        PaymentEntry(
                                            type = PaymentType.QR_WALLET,
                                            amountInCents = grandTotalInCents,
                                            referenceNo = "QR-${System.currentTimeMillis().toString().takeLast(6)}"
                                        )
                                    )
                                }
                                PaymentType.CREDIT_DUE -> {
                                    val creditCashPaidCents = ((creditPartialCashText.toDoubleOrNull() ?: 0.0) * 100).toLong().coerceIn(0L, grandTotalInCents)
                                    val creditAddedCents = grandTotalInCents - creditCashPaidCents
                                    if (creditCashPaidCents > 0L) {
                                        listOf(
                                            PaymentEntry(
                                                type = PaymentType.CASH,
                                                amountInCents = creditCashPaidCents,
                                                changeGivenInCents = 0L
                                            ),
                                            PaymentEntry(
                                                type = PaymentType.CREDIT_DUE,
                                                amountInCents = creditAddedCents
                                            )
                                        )
                                    } else {
                                        listOf(
                                            PaymentEntry(
                                                type = PaymentType.CREDIT_DUE,
                                                amountInCents = grandTotalInCents
                                            )
                                        )
                                    }
                                }
                            }
                            onCompletePayment(payments)
                        },
                        enabled = isFormValid,
                        gradient = PosButtonGradients.Emerald,
                        icon = Icons.Default.CheckCircle,
                        iconBadge = true,
                        shape = RoundedCornerShape(14.dp),
                        height = 50.dp,
                        testTag = "confirm_payment_button"
                    )
                }
            }
        }
    }

    if (showCustomerPickerInDialog) {
        CustomerPickerDialog(
            customers = customers,
            currency = currencySymbol,
            onDismiss = { showCustomerPickerInDialog = false },
            onSelectCustomer = { customerItem ->
                onSelectCustomer?.invoke(customerItem)
                showCustomerPickerInDialog = false
            },
            onAddNewCustomer = {
                showCustomerPickerInDialog = false
                onAddNewCustomer?.invoke()
            }
        )
    }
}
