package com.example.pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.example.pos.data.local.CustomerEntity
import com.example.pos.data.local.CustomerLedgerEntity
import com.example.pos.data.local.SaleEntity
import com.example.pos.data.local.SaleItemEntity
import com.example.pos.data.model.PaymentType
import com.example.pos.domain.CartCalculator
import com.example.pos.ui.PosNavDestination
import com.example.pos.ui.PosViewModel
import com.example.pos.ui.components.CustomerFormDialog
import com.example.pos.ui.components.PosButtonGradients
import com.example.pos.ui.components.PosCompactPillButton
import com.example.pos.ui.components.PosGradientButton
import com.example.pos.ui.components.PosOutlineButton
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerLedgerScreen(viewModel: PosViewModel) {
    val strings = LocalPosStrings.current
    val customers by viewModel.customers.collectAsState()
    val org by viewModel.organization.collectAsState()
    val currency = org?.currencySymbol ?: "$"

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableStateOf(0) } // 0: All, 1: With Debt, 2: Clear

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var selectedCustomerForLedger by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForCreditIn by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForCreditOut by remember { mutableStateOf<CustomerEntity?>(null) }

    // Synchronize selected customer with latest StateFlow update
    val activeCustomer = selectedCustomerForLedger?.let { sel ->
        customers.find { it.id == sel.id } ?: sel
    }

    // Filter customers
    val filteredCustomers = remember(customers, searchQuery, selectedFilterIndex) {
        customers.filter { cust ->
            val matchesQuery = searchQuery.isBlank() ||
                cust.name.contains(searchQuery, ignoreCase = true) ||
                cust.phone.contains(searchQuery, ignoreCase = true) ||
                cust.email.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilterIndex) {
                1 -> cust.currentDueInCents > 0
                2 -> cust.currentDueInCents == 0L
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    // Totals
    val totalCustomers = customers.size
    val totalOutstandingDebt = remember(customers) { customers.sumOf { it.currentDueInCents } }
    val totalCreditLimit = remember(customers) { customers.sumOf { it.creditLimitInCents } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("customer_ledger_screen")
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = strings.customersTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (strings is NepaliStrings) "ग्राहक उधारो सीमा र बाँकी हिसाबकिताब खाता" else "Store credit limits & debt ledgers",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                PosCompactPillButton(
                    text = strings.addCustomer,
                    icon = Icons.Default.PersonAdd,
                    gradient = PosButtonGradients.Emerald,
                    height = 36.dp,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag("add_customer_button"),
                    onClick = { showAddCustomerDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Metrics Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Customers
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PosPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = PosPrimary, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Customers", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Text("$totalCustomers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Total Outstanding Due
                Card(
                    modifier = Modifier.weight(1.3f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalOutstandingDebt > 0) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (totalOutstandingDebt > 0) PosRose.copy(alpha = 0.15f) else PosEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (totalOutstandingDebt > 0) PosRose else PosEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Total Outstanding", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Text(
                                text = CartCalculator.formatMoney(totalOutstandingDebt, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (totalOutstandingDebt > 0) PosRose else PosEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Total Credit Limit
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PosEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Total Limit", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Text(
                                text = CartCalculator.formatMoney(totalCreditLimit, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Filter Chips
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customer name, phone, or email...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilterIndex == 0,
                    onClick = { selectedFilterIndex = 0 },
                    label = { Text("All ($totalCustomers)") },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PosPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedFilterIndex == 1,
                    onClick = { selectedFilterIndex = 1 },
                    label = { Text("Has Outstanding Debt (${customers.count { it.currentDueInCents > 0 }})") },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PosRose,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedFilterIndex == 2,
                    onClick = { selectedFilterIndex = 2 },
                    label = { Text("Zero Due / Clear (${customers.count { it.currentDueInCents == 0L }})") },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PosEmerald,
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PosEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No Matching Customers" else "No Customers Registered Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Try searching for a different name, phone, or reset filters." else "Add customers to assign credit limits, track ledger balances, and issue store credit at the counter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (searchQuery.isBlank() && selectedFilterIndex == 0) {
                                Spacer(modifier = Modifier.height(18.dp))
                                PosCompactPillButton(
                                    text = if (strings is NepaliStrings) "पहिलो ग्राहक थप्नुहोस्" else "Add Your First Customer",
                                    icon = Icons.Default.Add,
                                    gradient = PosButtonGradients.Emerald,
                                    height = 40.dp,
                                    fontSize = 13.sp,
                                    modifier = Modifier.testTag("empty_add_customer_btn"),
                                    onClick = { showAddCustomerDialog = true }
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            currency = currency,
                            onViewLedger = { selectedCustomerForLedger = customer },
                            onCreditIn = { customerForCreditIn = customer },
                            onCreditOut = { customerForCreditOut = customer },
                            onSelectForCart = {
                                viewModel.selectCustomer(customer)
                                viewModel.navigateTo(PosNavDestination.REGISTER)
                                viewModel.setAlert("Attached ${customer.name} to POS Cart")
                            }
                        )
                    }
                }
            }
        }
    }

    // New Customer Registration Dialog
    if (showAddCustomerDialog) {
        CustomerFormDialog(
            currencySymbol = currency,
            onDismiss = { showAddCustomerDialog = false },
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
                    onSuccess = { showAddCustomerDialog = false }
                )
            }
        )
    }

    // Customer Detailed Ledger & Transaction History Modal
    activeCustomer?.let { cust ->
        CustomerDetailLedgerDialog(
            customer = cust,
            currency = currency,
            viewModel = viewModel,
            onDismiss = { selectedCustomerForLedger = null },
            onCreditIn = { customerForCreditIn = cust },
            onCreditOut = { customerForCreditOut = cust },
            onSelectForCart = {
                viewModel.selectCustomer(cust)
                viewModel.navigateTo(PosNavDestination.REGISTER)
                viewModel.setAlert("Attached ${cust.name} to POS Cart")
                selectedCustomerForLedger = null
            }
        )
    }

    // Credit IN Dialog (Receive Debt Repayment)
    customerForCreditIn?.let { cust ->
        CreditInDialog(
            customer = cust,
            currency = currency,
            onDismiss = { customerForCreditIn = null },
            onConfirm = { amountCents, payType, notes ->
                viewModel.receiveCustomerDebtPayment(
                    customerId = cust.id,
                    amountInCents = amountCents,
                    paymentType = payType,
                    notes = notes,
                    onComplete = { customerForCreditIn = null }
                )
            }
        )
    }

    // Credit OUT Dialog (Issue Credit / Increase Debt)
    customerForCreditOut?.let { cust ->
        CreditOutDialog(
            customer = cust,
            currency = currency,
            onDismiss = { customerForCreditOut = null },
            onConfirm = { amountCents, notes ->
                viewModel.giveCustomerCredit(
                    customerId = cust.id,
                    amountInCents = amountCents,
                    notes = notes,
                    onComplete = { customerForCreditOut = null }
                )
            }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: CustomerEntity,
    currency: String,
    onViewLedger: () -> Unit,
    onCreditIn: () -> Unit,
    onCreditOut: () -> Unit,
    onSelectForCart: () -> Unit
) {
    val availableCredit = (customer.creditLimitInCents - customer.currentDueInCents).coerceAtLeast(0L)
    val utilization = if (customer.creditLimitInCents > 0) {
        (customer.currentDueInCents.toFloat() / customer.creditLimitInCents.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val initials = customer.name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { "C" }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewLedger() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Customer Info & Due Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (customer.currentDueInCents > 0) PosRose.copy(alpha = 0.15f) else PosEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (customer.currentDueInCents > 0) PosRose else PosEmerald
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val contactText = listOf(customer.phone, customer.email).filter { it.isNotBlank() }.joinToString(" • ")
                        if (contactText.isNotBlank()) {
                            Text(
                                text = contactText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Balance Badge: To Receive (>0), To Give (<0), or Clear (==0)
                when {
                    customer.currentDueInCents > 0 -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF2F2))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "To Receive: ${CartCalculator.formatMoney(customer.currentDueInCents, currency)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PosRose,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "पैसा लिन बाँकी",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PosRose.copy(alpha = 0.85f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    customer.currentDueInCents < 0 -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "To Give: ${CartCalculator.formatMoney(-customer.currentDueInCents, currency)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "पैसा दिन बाँकी (Advance)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1D4ED8),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFECFDF5))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "✓ Clear (0.00)",
                                style = MaterialTheme.typography.labelMedium,
                                color = PosEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Credit Limit & Utilization
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Credit Limit: ${CartCalculator.formatMoney(customer.creditLimitInCents, currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Available: ${CartCalculator.formatMoney(availableCredit, currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (availableCredit == 0L) PosRose else PosEmerald
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { utilization },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        utilization >= 0.9f -> PosRose
                        utilization >= 0.6f -> Color(0xFFF59E0B)
                        else -> PosEmerald
                    },
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Ledger / History
                PosOutlineButton(
                    text = "View Ledger",
                    icon = Icons.Default.History,
                    height = 36.dp,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    onClick = onViewLedger
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Credit In (Receive Payment) button if has due
                    if (customer.currentDueInCents > 0) {
                        PosCompactPillButton(
                            text = "Receive Due",
                            icon = Icons.Default.ArrowDownward,
                            gradient = PosButtonGradients.Emerald,
                            height = 36.dp,
                            fontSize = 12.sp,
                            onClick = onCreditIn
                        )
                    } else {
                        // Give Credit
                        PosOutlineButton(
                            text = "+ Credit Out",
                            height = 36.dp,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            onClick = onCreditOut
                        )
                    }

                    // Attach to Cart
                    PosOutlineButton(
                        text = "Cart",
                        icon = Icons.Default.ShoppingCart,
                        height = 36.dp,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        onClick = onSelectForCart
                    )
                }
            }
        }
    }
}

private enum class CustomerTxType {
    SALE_INVOICE,
    SALE_CREDIT,
    DEBT_REPAYMENT,
    CREDIT_GIVEN,
    OPENING_BALANCE
}

private data class CustomerUiTransaction(
    val id: String,
    val timestamp: Long,
    val type: CustomerTxType,
    val title: String,
    val subtitle: String,
    val note: String = "",
    val amountInCents: Long,
    val balanceAfterInCents: Long?,
    val isCreditDueImpact: Boolean,
    val paymentBadges: List<String> = emptyList(),
    val itemsCount: Int = 0,
    val sale: SaleEntity? = null,
    val saleItems: List<SaleItemEntity> = emptyList()
)

@Composable
private fun CustomerDetailLedgerDialog(
    customer: CustomerEntity,
    currency: String,
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onCreditIn: () -> Unit,
    onCreditOut: () -> Unit,
    onSelectForCart: () -> Unit
) {
    val allCustomers by viewModel.customers.collectAsState()
    val activeCust = remember(allCustomers, customer) {
        allCustomers.find { it.id == customer.id } ?: customer
    }

    val ledgerEntries: List<CustomerLedgerEntity> by viewModel.getCustomerLedger(activeCust.id).collectAsState(initial = emptyList())
    val customerSales: List<SaleEntity> by viewModel.getCustomerSales(activeCust.id).collectAsState(initial = emptyList())
    val allSalePayments by viewModel.salePayments.collectAsState()
    val allSaleItems by viewModel.saleItems.collectAsState()

    val availableCredit = (activeCust.creditLimitInCents - activeCust.currentDueInCents).coerceAtLeast(0L)
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val isNepali = LocalPosStrings.current is NepaliStrings

    var selectedFilterIndex by remember { mutableStateOf(0) } // 0: All, 1: Invoices, 2: Khata / Credit

    val allTransactions = remember(customerSales, ledgerEntries, allSalePayments, allSaleItems) {
        val list = mutableListOf<CustomerUiTransaction>()

        // 1. All POS Sales for this customer
        customerSales.forEach { sale ->
            val payments = allSalePayments.filter { it.saleId == sale.id }
            val items = allSaleItems.filter { it.saleId == sale.id }
            val isCredit = payments.any { it.paymentType == PaymentType.CREDIT_DUE }
            val matchingLedger = ledgerEntries.find { it.saleId == sale.id }
            val badges = if (payments.isNotEmpty()) payments.map { it.paymentType.name } else listOf("PAID")
            val totalItems = if (items.isNotEmpty()) items.sumOf { it.quantity } else 1

            list.add(
                CustomerUiTransaction(
                    id = "sale_${sale.id}",
                    timestamp = sale.createdAt,
                    type = if (isCredit) CustomerTxType.SALE_CREDIT else CustomerTxType.SALE_INVOICE,
                    title = "Invoice ${sale.invoiceNo}",
                    subtitle = if (isCredit) "POS Credit Purchase (बाँकी खरिद)" else "POS Sale (${badges.joinToString()})",
                    note = matchingLedger?.notes ?: "$totalItems items",
                    amountInCents = sale.grandTotalInCents,
                    balanceAfterInCents = matchingLedger?.balanceAfterInCents,
                    isCreditDueImpact = isCredit,
                    paymentBadges = badges,
                    itemsCount = totalItems,
                    sale = sale,
                    saleItems = items
                )
            )
        }

        // 2. All Customer Ledger entries (Credit IN, Credit OUT, Opening Balance, etc.)
        ledgerEntries.forEach { entry ->
            // Skip if this ledger entry is already merged with a sales invoice
            if (entry.saleId != null && customerSales.any { it.id == entry.saleId }) {
                return@forEach
            }

            val isOpeningReceivable = entry.entryType == "OPENING_BALANCE_RECEIVABLE"
            val isOpeningAdvance = entry.entryType == "OPENING_BALANCE_ADVANCE"
            val isRepayment = entry.entryType == "DEBT_REPAYMENT" || entry.amountInCents < 0
            val isSaleCredit = entry.entryType == "SALE_CREDIT"
            val isCreditGiven = entry.entryType == "CREDIT_GIVEN" || entry.entryType == "CREDIT_OUT"

            val txType = when {
                isOpeningReceivable || isOpeningAdvance -> CustomerTxType.OPENING_BALANCE
                isRepayment -> CustomerTxType.DEBT_REPAYMENT
                isSaleCredit -> CustomerTxType.SALE_CREDIT
                isCreditGiven -> CustomerTxType.CREDIT_GIVEN
                else -> if (entry.amountInCents < 0) CustomerTxType.DEBT_REPAYMENT else CustomerTxType.CREDIT_GIVEN
            }

            val title = when {
                isOpeningReceivable -> if (isNepali) "प्रारम्भिक बाँकी (लिन बाँकी)" else "Opening Balance (To Receive)"
                isOpeningAdvance -> if (isNepali) "प्रारम्भिक बाँकी (दिन बाँकी)" else "Opening Balance (To Give)"
                isRepayment -> if (isNepali) "भुक्तानी प्राप्त (Credit IN)" else "Payment Received (Credit IN)"
                isSaleCredit -> if (isNepali) "उधारो बिक्री (Credit Sale)" else "POS Sale on Credit"
                isCreditGiven -> if (isNepali) "उधारो निकासा (Credit OUT)" else "Credit Issued (Credit OUT)"
                else -> entry.notes.ifBlank { "Khata Entry" }
            }

            list.add(
                CustomerUiTransaction(
                    id = "ledger_${entry.id}",
                    timestamp = entry.createdAt,
                    type = txType,
                    title = title,
                    subtitle = entry.notes.ifBlank { "Khata Record" },
                    note = entry.notes,
                    amountInCents = kotlin.math.abs(entry.amountInCents),
                    balanceAfterInCents = entry.balanceAfterInCents,
                    isCreditDueImpact = txType != CustomerTxType.SALE_INVOICE
                )
            )
        }

        list.sortedByDescending { it.timestamp }
    }

    val displayedTransactions = remember(allTransactions, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> allTransactions.filter { it.type == CustomerTxType.SALE_INVOICE || it.type == CustomerTxType.SALE_CREDIT }
            2 -> allTransactions.filter { it.type != CustomerTxType.SALE_INVOICE }
            else -> allTransactions
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 760.dp)
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = activeCust.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = listOf(activeCust.phone, activeCust.email).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // KPI Balance Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Balance Card (To Receive / To Give / Clear)
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    activeCust.currentDueInCents > 0 -> Color(0xFFFEF2F2)
                                    activeCust.currentDueInCents < 0 -> Color(0xFFEFF6FF)
                                    else -> Color(0xFFECFDF5)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = when {
                                        activeCust.currentDueInCents > 0 -> if (isNepali) "पैसा लिन बाँकी" else "To Receive"
                                        activeCust.currentDueInCents < 0 -> if (isNepali) "पैसा दिन बाँकी" else "To Give"
                                        else -> if (isNepali) "खाता बक्यौता" else "Account Balance"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CartCalculator.formatMoney(
                                        if (activeCust.currentDueInCents < 0) -activeCust.currentDueInCents else activeCust.currentDueInCents,
                                        currency
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        activeCust.currentDueInCents > 0 -> PosRose
                                        activeCust.currentDueInCents < 0 -> Color(0xFF2563EB)
                                        else -> PosEmerald
                                    }
                                )
                            }
                        }

                        // Credit Limit
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(if (isNepali) "ऋण सीमा" else "Credit Limit", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CartCalculator.formatMoney(activeCust.creditLimitInCents, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Available Credit
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(if (isNepali) "उपलब्ध सीमा" else "Available Credit", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CartCalculator.formatMoney(availableCredit, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (availableCredit == 0L) PosRose else PosEmerald
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Credit Action Buttons (IN & OUT)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Credit IN (Receive Payment / Clear Debt)
                        PosCompactPillButton(
                            text = if (isNepali) "↓ पैसा आयो (IN)" else "↓ Credit IN",
                            icon = Icons.Default.ArrowDownward,
                            gradient = PosButtonGradients.Emerald,
                            height = 42.dp,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                            onClick = onCreditIn
                        )

                        // Credit OUT (Give Store Credit / Add Debt)
                        PosCompactPillButton(
                            text = if (isNepali) "↑ उधारो दियो (OUT)" else "↑ Credit OUT",
                            icon = Icons.Default.ArrowUpward,
                            gradient = PosButtonGradients.Amber,
                            height = 42.dp,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                            onClick = onCreditOut
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ledger & Transactions Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, contentDescription = null, tint = PosPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isNepali) "कारोबार इतिहास" else "Transaction Record History",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${displayedTransactions.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val allCount = allTransactions.size
                        val salesCount = allTransactions.count { it.type == CustomerTxType.SALE_INVOICE || it.type == CustomerTxType.SALE_CREDIT }
                        val creditCount = allTransactions.count { it.type != CustomerTxType.SALE_INVOICE }

                        FilterChip(
                            selected = selectedFilterIndex == 0,
                            onClick = { selectedFilterIndex = 0 },
                            label = { Text(if (isNepali) "सबै ($allCount)" else "All ($allCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PosPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = PosPrimary
                            )
                        )
                        FilterChip(
                            selected = selectedFilterIndex == 1,
                            onClick = { selectedFilterIndex = 1 },
                            label = { Text(if (isNepali) "बिलहरू ($salesCount)" else "Invoices ($salesCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PosPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = PosPrimary
                            )
                        )
                        FilterChip(
                            selected = selectedFilterIndex == 2,
                            onClick = { selectedFilterIndex = 2 },
                            label = { Text(if (isNepali) "खाता हिसाब ($creditCount)" else "Khata / Credit ($creditCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PosPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = PosPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Timeline entries
                    if (displayedTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = when (selectedFilterIndex) {
                                        1 -> if (isNepali) "${activeCust.name} को कुनै बिल छैन" else "No Sales Invoices for ${activeCust.name}"
                                        2 -> if (isNepali) "कुनै खाता हिसाव रेकर्ड छैन" else "No Khata / Credit Records"
                                        else -> if (isNepali) "कुनै कारोबार इतिहास छैन" else "No Transaction History"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isNepali) "नयाँ बिक्री दर्ता गर्न 'बिलमा जोड्नुहोस्' थिच्नुहोस् वा 'पैसा आयो' / 'उधारो दियो' प्रयोग गर्नुहोस्।" else "Tap 'Attach to POS Cart' below to record a sale/purchase for ${activeCust.name}, or use 'Credit IN' / 'Credit OUT' to record payments.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(displayedTransactions, key = { it.id }) { tx ->
                                CustomerTransactionCard(
                                    tx = tx,
                                    currency = currency,
                                    dateFormat = dateFormat,
                                    onReprint = { sale -> viewModel.reprintReceipt(sale) }
                                )
                            }
                        }
                    }
                }

                // Sticky Bottom Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PosOutlineButton(
                            text = if (isNepali) "बिलमा जोड्नुहोस्" else "Attach to POS Cart",
                            icon = Icons.Default.ShoppingCart,
                            height = 42.dp,
                            shape = RoundedCornerShape(10.dp),
                            onClick = onSelectForCart
                        )

                        PosOutlineButton(
                            text = if (isNepali) "सम्पन्न" else "Done",
                            icon = Icons.Default.Check,
                            height = 42.dp,
                            shape = RoundedCornerShape(10.dp),
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerTransactionCard(
    tx: CustomerUiTransaction,
    currency: String,
    dateFormat: SimpleDateFormat,
    onReprint: (SaleEntity) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when (tx.type) {
                                    CustomerTxType.SALE_INVOICE -> PosEmerald.copy(alpha = 0.15f)
                                    CustomerTxType.SALE_CREDIT -> PosAmber.copy(alpha = 0.15f)
                                    CustomerTxType.DEBT_REPAYMENT -> PosEmerald.copy(alpha = 0.15f)
                                    CustomerTxType.CREDIT_GIVEN -> PosRose.copy(alpha = 0.15f)
                                    CustomerTxType.OPENING_BALANCE -> Color(0xFF2563EB).copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (tx.type) {
                                CustomerTxType.SALE_INVOICE -> Icons.Default.Receipt
                                CustomerTxType.SALE_CREDIT -> Icons.AutoMirrored.Filled.ReceiptLong
                                CustomerTxType.DEBT_REPAYMENT -> Icons.Default.ArrowDownward
                                CustomerTxType.CREDIT_GIVEN -> Icons.Default.ArrowUpward
                                CustomerTxType.OPENING_BALANCE -> Icons.Default.AccountBalanceWallet
                            },
                            contentDescription = null,
                            tint = when (tx.type) {
                                CustomerTxType.SALE_INVOICE -> PosEmerald
                                CustomerTxType.SALE_CREDIT -> PosAmber
                                CustomerTxType.DEBT_REPAYMENT -> PosEmerald
                                CustomerTxType.CREDIT_GIVEN -> PosRose
                                CustomerTxType.OPENING_BALANCE -> Color(0xFF2563EB)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = tx.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = dateFormat.format(Date(tx.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )

                        // Badges / notes row
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tx.paymentBadges.isNotEmpty()) {
                                tx.paymentBadges.forEach { badge ->
                                    val isCreditBadge = badge == "CREDIT_DUE"
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isCreditBadge) PosAmber.copy(alpha = 0.15f) else PosEmerald.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isCreditBadge) "CREDIT" else badge.replace("_", " "),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCreditBadge) PosAmber else PosEmerald,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            if (tx.itemsCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${tx.itemsCount} ${if (tx.itemsCount == 1) "item" else "items"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (tx.note.isNotBlank() && tx.note != tx.subtitle && tx.paymentBadges.isEmpty()) {
                                Text(
                                    text = tx.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val sign = when (tx.type) {
                        CustomerTxType.DEBT_REPAYMENT -> "- "
                        CustomerTxType.CREDIT_GIVEN -> "+ "
                        CustomerTxType.SALE_CREDIT -> "+ "
                        else -> ""
                    }
                    val amountColor = when (tx.type) {
                        CustomerTxType.DEBT_REPAYMENT -> PosEmerald
                        CustomerTxType.CREDIT_GIVEN -> PosRose
                        CustomerTxType.SALE_CREDIT -> PosAmber
                        CustomerTxType.OPENING_BALANCE -> Color(0xFF2563EB)
                        CustomerTxType.SALE_INVOICE -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(
                        text = "$sign${CartCalculator.formatMoney(tx.amountInCents, currency)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )

                    if (tx.balanceAfterInCents != null) {
                        Text(
                            text = "Bal: ${CartCalculator.formatMoney(tx.balanceAfterInCents, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }

                    if (tx.sale != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        PosCompactPillButton(
                            text = "Bill",
                            icon = Icons.Default.Receipt,
                            gradient = PosButtonGradients.Primary,
                            height = 24.dp,
                            fontSize = 10.sp,
                            onClick = { onReprint(tx.sale) }
                        )
                    }
                }
            }

            // Purchased items preview and expandable itemization
            if (tx.saleItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Purchased: " + tx.saleItems.joinToString(", ") { "${it.productName} ×${it.quantity}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tx.saleItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.productName} × ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = CartCalculator.formatMoney(item.lineTotalInCents, currency),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditInDialog(
    customer: CustomerEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (amountCents: Long, paymentType: PaymentType, notes: String) -> Unit
) {
    var payAmountText by remember { mutableStateOf(if (customer.currentDueInCents > 0) (customer.currentDueInCents / 100.0).toString() else "100.00") }
    var payType by remember { mutableStateOf(PaymentType.CASH) }
    var noteText by remember { mutableStateOf("Full Debt Repayment") }

    val fullDueCents = customer.currentDueInCents

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PosEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Credit IN (Receive Payment)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(customer.name, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Outstanding Due Notice
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Current Outstanding Due", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        Text(
                            text = CartCalculator.formatMoney(fullDueCents, currency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PosRose
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount
                OutlinedTextField(
                    value = payAmountText,
                    onValueChange = { payAmountText = it },
                    label = { Text("Payment Amount ($currency)") },
                    leadingIcon = { Text(currency, fontWeight = FontWeight.Bold, color = PosEmerald, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Amount Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (fullDueCents > 0) {
                        val fullDueDollars = (fullDueCents / 100.0).toString()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (payAmountText == fullDueDollars) PosEmerald else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    payAmountText = fullDueDollars
                                    noteText = "Full Debt Repayment"
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Full Due (${CartCalculator.formatMoney(fullDueCents, currency)})", style = MaterialTheme.typography.labelSmall, color = if (payAmountText == fullDueDollars) Color.White else MaterialTheme.colorScheme.onSurface)
                        }

                        val halfDueDollars = ((fullDueCents / 2) / 100.0).toString()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (payAmountText == halfDueDollars) PosEmerald else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    payAmountText = halfDueDollars
                                    noteText = "Partial Debt Repayment (50%)"
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("50% Due", style = MaterialTheme.typography.labelSmall, color = if (payAmountText == halfDueDollars) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    listOf("100.00", "250.00", "500.00", "1000.00").forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (payAmountText == preset) PosEmerald else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { payAmountText = preset }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("$currency $preset", style = MaterialTheme.typography.labelSmall, color = if (payAmountText == preset) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Method
                Text("Payment Method", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        PaymentType.CASH to "Cash",
                        PaymentType.CARD to "Card",
                        PaymentType.QR_WALLET to "Bank / QR / Wallet"
                    ).forEach { (type, label) ->
                        val isSel = payType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) PosPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { payType = type }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note / Reference
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Payment Note / Reference") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PosOutlineButton(
                        text = "Cancel",
                        shape = RoundedCornerShape(10.dp),
                        height = 42.dp,
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    PosGradientButton(
                        text = "Post Payment",
                        subtitle = "Receive $currency${payAmountText.ifBlank { "0.00" }}",
                        icon = Icons.Default.Payment,
                        gradient = PosButtonGradients.Emerald,
                        shape = RoundedCornerShape(12.dp),
                        height = 44.dp,
                        onClick = {
                            val amountCents = ((payAmountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                            if (amountCents > 0) {
                                onConfirm(amountCents, payType, noteText)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditOutDialog(
    customer: CustomerEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (amountCents: Long, notes: String) -> Unit
) {
    var creditAmountText by remember { mutableStateOf("500.00") }
    var noteText by remember { mutableStateOf("Store credit purchase") }

    val availableCredit = (customer.creditLimitInCents - customer.currentDueInCents).coerceAtLeast(0L)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PosRose.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = PosRose, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Credit OUT (Give Credit)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(customer.name, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Credit Limit Notice
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Remaining Available Credit", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Text(
                                text = CartCalculator.formatMoney(availableCredit, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (availableCredit == 0L) PosRose else PosEmerald
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Credit Limit", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Text(
                                text = CartCalculator.formatMoney(customer.creditLimitInCents, currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount
                OutlinedTextField(
                    value = creditAmountText,
                    onValueChange = { creditAmountText = it },
                    label = { Text("Credit Amount to Issue ($currency)") },
                    leadingIcon = { Text(currency, fontWeight = FontWeight.Bold, color = PosRose, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (availableCredit > 0) {
                        val maxAvailDollars = (availableCredit / 100.0).toString()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (creditAmountText == maxAvailDollars) PosRose else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    creditAmountText = maxAvailDollars
                                    noteText = "Max available credit issued"
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Max Available (${CartCalculator.formatMoney(availableCredit, currency)})", style = MaterialTheme.typography.labelSmall, color = if (creditAmountText == maxAvailDollars) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    listOf("250.00", "500.00", "1000.00", "2500.00").forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (creditAmountText == preset) PosRose else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { creditAmountText = preset }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("$currency $preset", style = MaterialTheme.typography.labelSmall, color = if (creditAmountText == preset) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note / Reason
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Reason / Goods Description / Memo") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PosOutlineButton(
                        text = "Cancel",
                        shape = RoundedCornerShape(10.dp),
                        height = 42.dp,
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    PosGradientButton(
                        text = "Issue Credit",
                        subtitle = "Charge $currency${creditAmountText.ifBlank { "0.00" }}",
                        icon = Icons.Default.ArrowUpward,
                        gradient = PosButtonGradients.Amber,
                        shape = RoundedCornerShape(12.dp),
                        height = 44.dp,
                        onClick = {
                            val amountCents = ((creditAmountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                            if (amountCents > 0) {
                                onConfirm(amountCents, noteText)
                            }
                        }
                    )
                }
            }
        }
    }
}
