package com.example.pos.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.pos.ui.components.PosButtonGradients
import com.example.pos.ui.components.PosCompactPillButton
import com.example.pos.ui.components.PosGradientButton
import com.example.pos.ui.components.PosOutlineButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pos.data.local.CategoryEntity
import com.example.pos.data.local.CustomerEntity
import com.example.pos.data.local.SaleEntity
import com.example.pos.data.local.SaleItemEntity
import com.example.pos.data.local.SalePaymentEntity
import com.example.pos.data.local.ShiftEntity
import com.example.pos.data.model.PaymentType
import com.example.pos.domain.CartCalculator
import com.example.pos.domain.ReceiptFormatter
import com.example.pos.ui.PosNavDestination
import com.example.pos.ui.PosViewModel
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosSky
import com.example.ui.theme.Slate100
import com.example.pos.data.local.FiscalYearEntity
import com.example.pos.domain.FiscalYearHelper
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportPeriod(val labelEn: String, val labelNe: String) {
    TODAY("Today", "आज"),
    YESTERDAY("Yesterday", "हिजो"),
    THIS_WEEK("Last 7 Days", "७ दिन"),
    THIS_MONTH("Last 30 Days", "३० दिन"),
    FISCAL_YEAR("Fiscal Year", "आर्थिक वर्ष"),
    CUSTOM("Custom...", "अनुकूलित"),
    ALL_TIME("All Time", "सबै")
}

data class QuarterStat(
    val code: String,
    val labelEn: String,
    val labelNe: String,
    val invoicesCount: Int,
    val revenue: Long,
    val vat: Long
)

enum class ConsumableSortMode(val labelEn: String, val labelNe: String) {
    UNITS_SOLD("Volume (Units)", "एकाइ बिक्री"),
    REVENUE("Revenue ($)", "कुल आम्दानी"),
    GROSS_PROFIT("Gross Profit ($)", "नाफा रकम"),
    STOCK_URGENCY("Restock Urgency", "पुन: भण्डारण आवश्यकता"),
    VELOCITY("Sales Velocity", "बिक्री गति")
}

data class ConsumableProductStat(
    val productId: String,
    val productName: String,
    val sku: String,
    val categoryId: String,
    val categoryName: String,
    val unitsSold: Int,
    val totalRevenueInCents: Long,
    val costPriceInCents: Long,
    val grossProfitInCents: Long,
    val currentStock: Int,
    val minStockAlert: Int,
    val unitsPerDay: Double,
    val estimatedDaysLeft: Int?
)

@Composable
fun ReportsScreen(viewModel: PosViewModel) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sales by viewModel.sales.collectAsState()
    val saleItems by viewModel.saleItems.collectAsState()
    val salePayments by viewModel.salePayments.collectAsState()
    val stockMovements by viewModel.stockMovements.collectAsState()
    val allShifts by viewModel.allShifts.collectAsState()
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val org by viewModel.organization.collectAsState()
    val currency = org?.currencySymbol ?: "$"

    val fiscalYears by viewModel.fiscalYears.collectAsState()
    val activeFiscalYear by viewModel.activeFiscalYear.collectAsState()
    var selectedFiscalYearId by remember { mutableStateOf<String?>(null) }
    val effectiveFiscalYear = remember(fiscalYears, activeFiscalYear, selectedFiscalYearId) {
        fiscalYears.find { it.id == selectedFiscalYearId } ?: activeFiscalYear ?: fiscalYears.firstOrNull()
    }

    val totalToReceive = remember(customers) {
        customers.filter { it.currentDueInCents > 0L }.sumOf { it.currentDueInCents }
    }
    val totalToGive = remember(customers) {
        customers.filter { it.currentDueInCents < 0L }.sumOf { -it.currentDueInCents }
    }
    val netCustomerBalance = remember(totalToReceive, totalToGive) {
        totalToReceive - totalToGive
    }
    val customersWithDue = remember(customers) {
        customers.filter { it.currentDueInCents > 0L }.sortedByDescending { it.currentDueInCents }
    }
    val customersWithAdvance = remember(customers) {
        customers.filter { it.currentDueInCents < 0L }.sortedBy { it.currentDueInCents }
    }

    val productMap = remember(products) { products.associateBy { it.id } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.TODAY) }
    var selectedPaymentType by remember { mutableStateOf("ALL") }
    var selectedCashier by remember { mutableStateOf("ALL") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedPaymentStatus by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Custom Date Range State
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableLongStateOf(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L) }
    var customEndDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Dialog state for viewing full itemized invoice details
    var selectedSaleForDetails by remember { mutableStateOf<SaleEntity?>(null) }
    var detailedItems by remember { mutableStateOf<List<SaleItemEntity>>(emptyList()) }
    var detailedPayments by remember { mutableStateOf<List<SalePaymentEntity>>(emptyList()) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    // Compute Date Boundaries
    val now = System.currentTimeMillis()
    val startOfToday = remember(now) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val startOfYesterday = startOfToday - 24 * 60 * 60 * 1000L
    val endOfYesterday = startOfToday - 1
    val startOfWeek = now - 7L * 24 * 60 * 60 * 1000L
    val startOfMonth = now - 30L * 24 * 60 * 60 * 1000L

    val (startTime, endTime) = remember(selectedPeriod, customStartDate, customEndDate, startOfToday, startOfYesterday, endOfYesterday, startOfWeek, startOfMonth, effectiveFiscalYear) {
        when (selectedPeriod) {
            ReportPeriod.TODAY -> Pair(startOfToday, Long.MAX_VALUE)
            ReportPeriod.YESTERDAY -> Pair(startOfYesterday, endOfYesterday)
            ReportPeriod.THIS_WEEK -> Pair(startOfWeek, Long.MAX_VALUE)
            ReportPeriod.THIS_MONTH -> Pair(startOfMonth, Long.MAX_VALUE)
            ReportPeriod.FISCAL_YEAR -> Pair(effectiveFiscalYear?.startDate ?: 0L, effectiveFiscalYear?.endDate ?: Long.MAX_VALUE)
            ReportPeriod.CUSTOM -> Pair(customStartDate, customEndDate)
            ReportPeriod.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
        }
    }

    val daysInPeriod = remember(startTime, endTime, now) {
        val effectiveEnd = if (endTime == Long.MAX_VALUE) now else endTime
        val diffDays = ((effectiveEnd - startTime) / (24 * 60 * 60 * 1000L)).toInt()
        diffDays.coerceAtLeast(1)
    }

    // Distinct cashiers in database
    val availableCashiers = remember(sales) {
        val distinct = sales.map { it.createdBy.ifBlank { "Staff" } }.distinct().sorted()
        listOf("ALL") + distinct
    }

    // Filter Sales by Date, Search Query, Payment Type, Cashier, and Status
    val filteredSales = remember(sales, startTime, endTime, selectedPaymentType, selectedCashier, selectedPaymentStatus, searchQuery, salePayments, selectedPeriod, effectiveFiscalYear) {
        sales.filter { sale ->
            val inTime = if (selectedPeriod == ReportPeriod.FISCAL_YEAR && effectiveFiscalYear != null) {
                sale.fiscalYear == effectiveFiscalYear.name || (sale.createdAt in startTime..endTime)
            } else {
                sale.createdAt in startTime..endTime
            }
            val matchesSearch = searchQuery.isBlank() ||
                sale.invoiceNo.contains(searchQuery, ignoreCase = true) ||
                sale.createdBy.contains(searchQuery, ignoreCase = true)
            val matchesPayment = if (selectedPaymentType == "ALL") true else {
                val paymentsForSale = salePayments.filter { it.saleId == sale.id }
                paymentsForSale.any { it.paymentType.name == selectedPaymentType }
            }
            val matchesCashier = selectedCashier == "ALL" ||
                sale.createdBy.equals(selectedCashier, ignoreCase = true) ||
                (selectedCashier == "Staff" && sale.createdBy.isBlank())
            val matchesStatus = selectedPaymentStatus == "ALL" ||
                sale.paymentStatus.equals(selectedPaymentStatus, ignoreCase = true)
            inTime && matchesSearch && matchesPayment && matchesCashier && matchesStatus
        }
    }

    // Fiscal Year Tax & Sales Register Computations
    val taxableSales = remember(filteredSales) {
        filteredSales.filter { it.taxInCents > 0L }.sumOf { it.subtotalInCents }
    }
    val taxExemptSales = remember(filteredSales) {
        filteredSales.filter { it.taxInCents == 0L }.sumOf { it.subtotalInCents }
    }
    val firstInvoice = remember(filteredSales) { filteredSales.minByOrNull { it.createdAt }?.invoiceNo }
    val lastInvoice = remember(filteredSales) { filteredSales.maxByOrNull { it.createdAt }?.invoiceNo }

    val quarterlyStats = remember(filteredSales, effectiveFiscalYear) {
        if (effectiveFiscalYear == null) emptyList() else {
            val quarters = FiscalYearHelper.getQuarterDateRanges(effectiveFiscalYear)
            quarters.map { q: FiscalYearHelper.FiscalQuarter ->
                val qSales = filteredSales.filter { it.createdAt in q.startDate..q.endDate }
                QuarterStat(
                    code = q.quarterCode,
                    labelEn = q.labelEn,
                    labelNe = q.labelNe,
                    invoicesCount = qSales.size,
                    revenue = qSales.sumOf { it.grandTotalInCents },
                    vat = qSales.sumOf { it.taxInCents }
                )
            }
        }
    }

    // Key Aggregations for Filtered Sales
    val totalRevenue = filteredSales.sumOf { it.grandTotalInCents }
    val totalOrders = filteredSales.size
    val averageOrderValue = if (totalOrders > 0) totalRevenue / totalOrders else 0L
    val totalTax = filteredSales.sumOf { it.taxInCents }
    val totalDiscounts = filteredSales.sumOf { it.discountInCents }

    // Tender Payments Breakdown
    val filteredSaleIds = remember(filteredSales) { filteredSales.map { it.id }.toSet() }
    val relevantPayments = remember(salePayments, filteredSaleIds) {
        salePayments.filter { it.saleId in filteredSaleIds }
    }
    val cashTotal = relevantPayments.filter { it.paymentType == PaymentType.CASH }.sumOf { it.amountInCents }
    val cardTotal = relevantPayments.filter { it.paymentType == PaymentType.CARD }.sumOf { it.amountInCents }
    val qrTotal = relevantPayments.filter { it.paymentType == PaymentType.QR_WALLET }.sumOf { it.amountInCents }
    val creditTotal = relevantPayments.filter { it.paymentType == PaymentType.CREDIT_DUE }.sumOf { it.amountInCents }
    val totalTenderCollected = (cashTotal + cardTotal + qrTotal + creditTotal).coerceAtLeast(1L)

    // Consumable Products Analytics with live inventory stock velocity
    val relevantSaleItems = remember(saleItems, filteredSaleIds) {
        saleItems.filter { it.saleId in filteredSaleIds }
    }

    val topConsumables = remember(relevantSaleItems, stockMovements, productMap, categoryMap, daysInPeriod) {
        val stockMap = stockMovements.groupBy { it.productId }
            .mapValues { (_, moves) -> moves.sumOf { it.quantityDelta } }

        relevantSaleItems.groupBy { it.productId }
            .map { (prodId, items) ->
                val units = items.sumOf { it.quantity }
                val revenue = items.sumOf { it.lineTotalInCents }
                val prod = productMap[prodId]
                val name = prod?.name ?: items.firstOrNull()?.productName ?: "Item"
                val sku = prod?.sku ?: ""
                val catId = prod?.categoryId ?: ""
                val catName = categoryMap[catId]?.name ?: "General"
                val costPrice = prod?.costPriceInCents ?: 0L
                val totalCost = costPrice * units
                val profit = revenue - totalCost
                val stock = stockMap[prodId] ?: 0
                val minAlert = prod?.minStockAlert ?: 5
                val ratePerDay = units.toDouble() / daysInPeriod.toDouble()
                val daysLeft = if (ratePerDay > 0.05 && stock > 0) (stock / ratePerDay).toInt() else if (stock <= 0) 0 else null

                ConsumableProductStat(
                    productId = prodId,
                    productName = name,
                    sku = sku,
                    categoryId = catId,
                    categoryName = catName,
                    unitsSold = units,
                    totalRevenueInCents = revenue,
                    costPriceInCents = costPrice,
                    grossProfitInCents = profit,
                    currentStock = stock,
                    minStockAlert = minAlert,
                    unitsPerDay = ratePerDay,
                    estimatedDaysLeft = daysLeft
                )
            }
            .sortedByDescending { it.unitsSold }
    }

    val totalCostOfGoodsSold = topConsumables.sumOf { it.costPriceInCents * it.unitsSold }
    val totalGrossProfit = (totalRevenue - totalCostOfGoodsSold).coerceAtLeast(0L)
    val grossProfitMarginPercent = if (totalRevenue > 0) ((totalGrossProfit.toDouble() / totalRevenue.toDouble()) * 100).toInt() else 0

    // Loss Prevention Audit: Line Voids & Abandoned Orders
    val periodLineVoids = remember(auditLogs, startTime, endTime) {
        auditLogs.filter { log ->
            log.createdAt in startTime..endTime && log.action == "LINE_VOID"
        }
    }

    // Hourly Sales Traffic Heatmap
    val hourlyDistribution = remember(filteredSales) {
        val counts = LongArray(24)
        val revs = LongArray(24)
        val cal = Calendar.getInstance()
        filteredSales.forEach { sale ->
            cal.timeInMillis = sale.createdAt
            val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            counts[hour] += 1
            revs[hour] += sale.grandTotalInCents
        }
        (0..23).map { h -> Triple(h, counts[h], revs[h]) }
    }
    val peakHourTriple = remember(hourlyDistribution) {
        hourlyDistribution.maxByOrNull { it.third }
    }

    val periodTitle = when (selectedPeriod) {
        ReportPeriod.TODAY -> if (isNepali) "आज" else "Today"
        ReportPeriod.YESTERDAY -> if (isNepali) "हिजो" else "Yesterday"
        ReportPeriod.THIS_WEEK -> if (isNepali) "पछिल्लो ७ दिन" else "Last 7 Days"
        ReportPeriod.THIS_MONTH -> if (isNepali) "पछिल्लो ३० दिन" else "Last 30 Days"
        ReportPeriod.FISCAL_YEAR -> if (isNepali) "आ.व. ${effectiveFiscalYear?.name ?: "चालु"}" else "FY ${effectiveFiscalYear?.name ?: "Active"}"
        ReportPeriod.CUSTOM -> {
            val sFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            "${sFormat.format(Date(startTime))} - ${sFormat.format(Date(if (endTime == Long.MAX_VALUE) now else endTime))}"
        }
        ReportPeriod.ALL_TIME -> if (isNepali) "सबै समय" else "All Time"
    }

    // Thermal Printer X/Z Report Print Action
    fun printThermalSalesReport() {
        val organizationEntity = org ?: return
        val tenderMap = mapOf(
            "Cash" to cashTotal,
            "Card/POS" to cardTotal,
            "QR Wallet" to qrTotal,
            "Customer Khata (Credit)" to creditTotal
        )
        val consumablesSummary = topConsumables.take(8).map {
            Triple(it.productName, it.unitsSold, it.totalRevenueInCents)
        }
        val reportText = ReceiptFormatter.formatSalesSummaryReport(
            org = organizationEntity,
            periodTitle = periodTitle,
            totalRevenue = totalRevenue,
            totalOrders = totalOrders,
            avgOrderValue = averageOrderValue,
            totalTax = totalTax,
            totalDiscounts = totalDiscounts,
            grossProfit = totalGrossProfit,
            tenderBreakdown = tenderMap,
            topConsumables = consumablesSummary,
            lineVoidsCount = periodLineVoids.size,
            staffName = if (selectedCashier == "ALL") "All Staff" else selectedCashier,
            toReceiveTotal = totalToReceive,
            toGiveTotal = totalToGive
        )
        viewModel.showReportReceipt(reportText)
    }

    // Share Summary Function
    fun shareReportSummary() {
        val text = buildString {
            appendLine("📊 OmniPOS Sales & Business Summary")
            appendLine("🏪 Store: ${org?.name ?: "OmniPOS Store"}")
            appendLine("📅 Period: $periodTitle")
            if (selectedCashier != "ALL") appendLine("👤 Cashier: $selectedCashier")
            appendLine("--------------------------------")
            appendLine("💰 Total Revenue: ${CartCalculator.formatMoney(totalRevenue, currency)}")
            appendLine("🧾 Invoices Count: $totalOrders")
            appendLine("📈 Avg Order Value: ${CartCalculator.formatMoney(averageOrderValue, currency)}")
            appendLine("🏷️ Discounts: ${CartCalculator.formatMoney(totalDiscounts, currency)}")
            appendLine("🏛️ Tax/VAT: ${CartCalculator.formatMoney(totalTax, currency)}")
            appendLine("💵 Gross Profit: ${CartCalculator.formatMoney(totalGrossProfit, currency)} ($grossProfitMarginPercent%)")
            appendLine("--------------------------------")
            appendLine("💳 Payment Tender Breakdown:")
            appendLine("• Cash: ${CartCalculator.formatMoney(cashTotal, currency)}")
            appendLine("• Card: ${CartCalculator.formatMoney(cardTotal, currency)}")
            appendLine("• QR Wallet: ${CartCalculator.formatMoney(qrTotal, currency)}")
            appendLine("• Credit (Khata): ${CartCalculator.formatMoney(creditTotal, currency)}")
            appendLine("--------------------------------")
            appendLine("📒 Customer Khata Outstanding (बाँकी हिसाब):")
            appendLine("• To Receive (पैसा लिन बाँकी): ${CartCalculator.formatMoney(totalToReceive, currency)} (${customersWithDue.size} customers)")
            appendLine("• To Give (पैसा दिन बाँकी): ${CartCalculator.formatMoney(totalToGive, currency)} (${customersWithAdvance.size} customers)")
            appendLine("• Net Khata Balance (खुद लिन बाँकी): ${CartCalculator.formatMoney(netCustomerBalance, currency)}")
            appendLine("--------------------------------")
            if (topConsumables.isNotEmpty()) {
                appendLine("🔥 Top Consumed Products:")
                topConsumables.take(6).forEachIndexed { idx, p ->
                    appendLine("${idx + 1}. ${p.productName} — ${p.unitsSold} units (${CartCalculator.formatMoney(p.totalRevenueInCents, currency)}) [Stock: ${p.currentStock}]")
                }
            }
            if (periodLineVoids.isNotEmpty()) {
                appendLine("--------------------------------")
                appendLine("🛡️ Loss Prevention:")
                appendLine("• Line Voids Count: ${periodLineVoids.size}")
            }
            appendLine("--------------------------------")
            appendLine("Generated by OmniPOS on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
        }
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(intent, "Share Sales Report"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp)
            .testTag("reports_screen")
    ) {
        // Top Header Row with Title, Thermal Print & Share Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Text(
                    text = strings.reportsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isNepali) "पूर्ण वित्तीय विश्लेषण र मौज्दात रिपोर्ट" else "Executive financial analytics & FMCG reports",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PosCompactPillButton(
                    text = if (isNepali) "प्रिन्ट" else "Print",
                    icon = Icons.Default.Print,
                    gradient = PosButtonGradients.Dark,
                    onClick = { printThermalSalesReport() }
                )

                IconButton(
                    onClick = { shareReportSummary() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PosPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Date Range Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReportPeriod.entries.forEach { period ->
                val isSelected = selectedPeriod == period
                val label = if (period == ReportPeriod.CUSTOM && selectedPeriod == ReportPeriod.CUSTOM) {
                    periodTitle
                } else {
                    if (isNepali) period.labelNe else period.labelEn
                }

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (period == ReportPeriod.CUSTOM) {
                            showCustomDateDialog = true
                        } else {
                            selectedPeriod = period
                        }
                    },
                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PosPrimary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Sub-chips for Fiscal Year selection
        AnimatedVisibility(visible = selectedPeriod == ReportPeriod.FISCAL_YEAR && fiscalYears.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(16.dp))
                Text(
                    text = if (isNepali) "आर्थिक वर्ष:" else "Select FY:",
                    fontSize = 12.sp,
                    color = Slate700,
                    fontWeight = FontWeight.SemiBold
                )

                fiscalYears.forEach { fy ->
                    val isFySelected = (fy.id == effectiveFiscalYear?.id)
                    FilterChip(
                        selected = isFySelected,
                        onClick = { selectedFiscalYearId = fy.id },
                        label = {
                            Text(
                                text = "${fy.name}${if (fy.isCurrent) " (Active)" else if (fy.isClosed) " (Closed)" else ""}",
                                fontSize = 11.sp,
                                fontWeight = if (isFySelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PosEmerald,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Secondary Filters Toolbar: Cashier & Payment Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, contentDescription = null, tint = Slate600, modifier = Modifier.size(16.dp))
            Text(if (isNepali) "फिल्टर:" else "Filters:", fontSize = 12.sp, color = Slate600, fontWeight = FontWeight.SemiBold)

            // Cashier filter
            availableCashiers.forEach { cashier ->
                val isSelected = selectedCashier == cashier
                val label = if (cashier == "ALL") (if (isNepali) "सबै क्यासियर" else "All Staff") else cashier
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCashier = cashier },
                    label = { Text(label, fontSize = 11.sp) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Slate800,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Payment Status Filter
            listOf("ALL" to (if (isNepali) "सबै अवस्था" else "All Status"), "COMPLETED" to "Completed", "PENDING_SYNC" to "Pending Sync").forEach { (statusKey, label) ->
                val isSelected = selectedPaymentStatus == statusKey
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedPaymentStatus = statusKey },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PosEmerald,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Navigation
        val tabs = listOf(
            if (isNepali) "सारांश" else "Overview",
            if (isNepali) "बिलहरू (${filteredSales.size})" else "Invoices (${filteredSales.size})",
            if (isNepali) "बाँकी खाता (${customers.count { it.currentDueInCents != 0L }})" else "Khata (${customers.count { it.currentDueInCents != 0L }})",
            if (isNepali) "खपत उत्पादनहरू (${topConsumables.size})" else "Consumables (${topConsumables.size})",
            if (isNepali) "शिफ्ट इतिहास (${allShifts.size})" else "Shifts (${allShifts.size})"
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = { HorizontalDivider(color = Slate100) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) PosPrimary else Slate600
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Contents
        when (selectedTab) {
            0 -> OverviewTabContent(
                totalRevenue = totalRevenue,
                totalOrders = totalOrders,
                averageOrderValue = averageOrderValue,
                totalTax = totalTax,
                totalDiscounts = totalDiscounts,
                grossProfit = totalGrossProfit,
                grossMarginPercent = grossProfitMarginPercent,
                currency = currency,
                cashTotal = cashTotal,
                cardTotal = cardTotal,
                qrTotal = qrTotal,
                creditTotal = creditTotal,
                totalTenderCollected = totalTenderCollected,
                totalToReceive = totalToReceive,
                totalToGive = totalToGive,
                netCustomerBalance = netCustomerBalance,
                customersWithDueCount = customersWithDue.size,
                customersWithAdvanceCount = customersWithAdvance.size,
                onViewCustomerKhata = { selectedTab = 2 },
                topConsumables = topConsumables,
                hourlyDistribution = hourlyDistribution,
                peakHour = peakHourTriple?.first ?: 12,
                lineVoidsCount = periodLineVoids.size,
                onViewAllConsumables = { selectedTab = 3 },
                selectedPeriod = selectedPeriod,
                effectiveFiscalYear = effectiveFiscalYear,
                taxableSales = taxableSales,
                taxExemptSales = taxExemptSales,
                firstInvoice = firstInvoice,
                lastInvoice = lastInvoice,
                quarterlyStats = quarterlyStats,
                isNepali = isNepali
            )
            1 -> InvoicesTabContent(
                sales = filteredSales,
                currency = currency,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                selectedPaymentType = selectedPaymentType,
                onPaymentTypeChange = { selectedPaymentType = it },
                onViewDetails = { sale ->
                    selectedSaleForDetails = sale
                    isLoadingDetails = true
                    scope.launch {
                        val (items, payments) = viewModel.getSaleDetails(sale.id)
                        detailedItems = items
                        detailedPayments = payments
                        isLoadingDetails = false
                    }
                },
                onReprint = { sale -> viewModel.reprintReceipt(sale) },
                isNepali = isNepali
            )
            2 -> CustomerKhataTabContent(
                customers = customers,
                currency = currency,
                onNavigateToCustomers = { viewModel.navigateTo(PosNavDestination.CUSTOMERS) },
                isNepali = isNepali
            )
            3 -> ConsumablesTabContent(
                consumables = topConsumables,
                categories = categories,
                selectedCategoryFilter = selectedCategoryFilter,
                onCategoryFilterChange = { selectedCategoryFilter = it },
                currency = currency,
                totalRevenue = totalRevenue,
                isNepali = isNepali
            )
            4 -> ShiftsTabContent(
                shifts = allShifts,
                currency = currency,
                isNepali = isNepali
            )
        }
    }

    // Itemized Invoice Details Modal
    selectedSaleForDetails?.let { sale ->
        InvoiceDetailsDialog(
            sale = sale,
            items = detailedItems,
            payments = detailedPayments,
            isLoading = isLoadingDetails,
            currency = currency,
            onDismiss = { selectedSaleForDetails = null },
            onReprint = {
                viewModel.reprintReceipt(sale)
                selectedSaleForDetails = null
            },
            isNepali = isNepali
        )
    }

    // Custom Date Range Dialog
    if (showCustomDateDialog) {
        CustomDateRangeDialog(
            currentStart = customStartDate,
            currentEnd = customEndDate,
            onDismiss = { showCustomDateDialog = false },
            onApply = { start, end ->
                customStartDate = start
                customEndDate = end
                selectedPeriod = ReportPeriod.CUSTOM
                showCustomDateDialog = false
            },
            isNepali = isNepali
        )
    }
}

// -----------------------------------------------------------------------------
// TAB 0: OVERVIEW & FINANCIALS
// -----------------------------------------------------------------------------
@Composable
private fun OverviewTabContent(
    totalRevenue: Long,
    totalOrders: Int,
    averageOrderValue: Long,
    totalTax: Long,
    totalDiscounts: Long,
    grossProfit: Long,
    grossMarginPercent: Int,
    currency: String,
    cashTotal: Long,
    cardTotal: Long,
    qrTotal: Long,
    creditTotal: Long,
    totalTenderCollected: Long,
    totalToReceive: Long,
    totalToGive: Long,
    netCustomerBalance: Long,
    customersWithDueCount: Int,
    customersWithAdvanceCount: Int,
    onViewCustomerKhata: () -> Unit,
    topConsumables: List<ConsumableProductStat>,
    hourlyDistribution: List<Triple<Int, Long, Long>>,
    peakHour: Int,
    lineVoidsCount: Int,
    onViewAllConsumables: () -> Unit,
    selectedPeriod: ReportPeriod = ReportPeriod.TODAY,
    effectiveFiscalYear: FiscalYearEntity? = null,
    taxableSales: Long = 0L,
    taxExemptSales: Long = 0L,
    firstInvoice: String? = null,
    lastInvoice: String? = null,
    quarterlyStats: List<QuarterStat> = emptyList(),
    isNepali: Boolean
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 4 KPI Stat Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        title = if (isNepali) "कुल बिक्री" else "Total Revenue",
                        value = CartCalculator.formatMoney(totalRevenue, currency),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        containerColor = Color(0xFFEFF6FF),
                        accentColor = PosPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = if (isNepali) "कुल अर्डर" else "Total Invoices",
                        value = "$totalOrders",
                        icon = Icons.Default.Receipt,
                        containerColor = Color(0xFFECFDF5),
                        accentColor = PosEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        title = if (isNepali) "औसत बिल रकम" else "Avg Order Value",
                        value = CartCalculator.formatMoney(averageOrderValue, currency),
                        icon = Icons.Default.Assessment,
                        containerColor = Color(0xFFF5F3FF),
                        accentColor = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = if (isNepali) "अनुमानित कुल नाफा" else "Est. Gross Profit",
                        value = "${CartCalculator.formatMoney(grossProfit, currency)} ($grossMarginPercent%)",
                        icon = Icons.Default.PointOfSale,
                        containerColor = Color(0xFFFFFBEB),
                        accentColor = PosAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Dedicated Fiscal Year Sales & Tax Register Card (बिक्री खाता)
        if (selectedPeriod == ReportPeriod.FISCAL_YEAR || effectiveFiscalYear != null) {
            item {
                FiscalYearSalesTaxRegisterCard(
                    fiscalYear = effectiveFiscalYear,
                    taxableSales = taxableSales,
                    taxExemptSales = taxExemptSales,
                    totalTax = totalTax,
                    totalRevenue = totalRevenue,
                    totalOrders = totalOrders,
                    firstInvoice = firstInvoice,
                    lastInvoice = lastInvoice,
                    quarterlyStats = quarterlyStats,
                    currency = currency,
                    isNepali = isNepali
                )
            }
        }

        // Customer Khata Balances (To Receive / पैसा लिन बाँकी & To Give / पैसा दिन बाँकी)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFEF2F2), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = PosRose,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isNepali) "ग्राहक खाता बक्यौता (Customer Khata)" else "Customer Khata Outstanding",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = if (isNepali) "लिन बाँकी र दिन बाँकी रकमको स्थिति" else "Receivables & Advances Balance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }
                        }

                        TextButton(
                            onClick = onViewCustomerKhata,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isNepali) "खाता हेर्नुहोस्" else "View Khata",
                                color = PosPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = PosPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. To Receive (लिन बाँकी)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF1F2),
                            border = BorderStroke(1.dp, Color(0xFFFFE4E6))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = PosRose,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isNepali) "पैसा लिन बाँकी" else "To Receive",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PosRose
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CartCalculator.formatMoney(totalToReceive, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PosRose
                                )
                                Text(
                                    text = if (isNepali) "$customersWithDueCount ग्राहकबाट लिनु पर्ने" else "$customersWithDueCount customers owe",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }

                        // 2. To Give (दिन बाँकी)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = PosPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isNepali) "पैसा दिन बाँकी" else "To Give",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PosPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CartCalculator.formatMoney(totalToGive, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PosPrimary
                                )
                                Text(
                                    text = if (isNepali) "$customersWithAdvanceCount ग्राहकलाई अग्रिम/फिर्ता" else "$customersWithAdvanceCount advance/refund",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Net Khata summary bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Slate50
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isNepali) "खुद बक्यौता (Net Receivable):" else "Net Khata Balance:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Slate700
                            )
                            Text(
                                text = CartCalculator.formatMoney(netCustomerBalance, currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netCustomerBalance > 0) PosRose else if (netCustomerBalance < 0) PosPrimary else PosEmerald
                            )
                        }
                    }
                }
            }
        }

        // Secondary Financials: Discounts, Taxes, and Loss Prevention
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isNepali) "कुल दिइएको छुट" else "Discounts Given", style = MaterialTheme.typography.labelSmall, color = Slate600)
                        Text(CartCalculator.formatMoney(totalDiscounts, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PosRose)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isNepali) "कर / भ्याट संकलन" else "Tax / VAT Collected", style = MaterialTheme.typography.labelSmall, color = Slate600)
                        Text(CartCalculator.formatMoney(totalTax, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PosEmerald)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isNepali) "लाइन खारेज (Loss Prev.)" else "Line Voids (Audit)", style = MaterialTheme.typography.labelSmall, color = Slate600)
                        Text(if (isNepali) "$lineVoidsCount रद्द" else "$lineVoidsCount voids", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (lineVoidsCount > 0) PosAmber else Slate700)
                    }
                }
            }
        }

        // Tender Payment Breakdown Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNepali) "भुक्तानी माध्यम सारांश" else "Payment Methods Split",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Icon(Icons.Default.Payments, contentDescription = null, tint = Slate600)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TenderProgressBar(
                        label = if (isNepali) "नगद (Cash)" else "Cash",
                        amount = cashTotal,
                        total = totalTenderCollected,
                        currency = currency,
                        color = PosEmerald
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TenderProgressBar(
                        label = if (isNepali) "कार्ड (Card)" else "Card / POS",
                        amount = cardTotal,
                        total = totalTenderCollected,
                        currency = currency,
                        color = PosPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TenderProgressBar(
                        label = if (isNepali) "डिजिटल वालेट (QR)" else "Digital QR Wallet",
                        amount = qrTotal,
                        total = totalTenderCollected,
                        currency = currency,
                        color = PosSky
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TenderProgressBar(
                        label = if (isNepali) "उधारो बिक्री (Credit / To Receive)" else "Credit Sales (To Receive)",
                        amount = creditTotal,
                        total = totalTenderCollected,
                        currency = currency,
                        color = PosRose
                    )
                }
            }
        }

        // Hourly Sales Traffic Heatmap
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isNepali) "घन्टागत कारोबार गति (Peak Hours)" else "Hourly Sales Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            val peakFormatted = String.format(Locale.getDefault(), "%02d:00 - %02d:00", peakHour, (peakHour + 1) % 24)
                            Text(
                                text = if (isNepali) "उच्चतम व्यापार समय: $peakFormatted" else "Busiest traffic window: $peakFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = PosPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Slate600)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val maxRevenue = hourlyDistribution.maxOfOrNull { it.third }?.coerceAtLeast(1L) ?: 1L
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        hourlyDistribution.filter { it.first in 7..22 }.forEach { (hour, _, rev) ->
                            val heightFraction = (rev.toFloat() / maxRevenue.toFloat()).coerceIn(0.08f, 1f)
                            val isPeak = hour == peakHour && rev > 0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height((60 * heightFraction).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (isPeak) PosEmerald else if (rev > 0) PosPrimary.copy(alpha = 0.7f) else Slate100)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$hour",
                                    fontSize = 9.sp,
                                    fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPeak) PosEmerald else Slate600
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fast Preview of Top 3 Consumable Items
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isNepali) "शीर्ष बिक्री भएका उपभोग्य वस्तुहरू" else "Top Consumed Products",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        TextButton(onClick = onViewAllConsumables) {
                            Text(if (isNepali) "सबै हेर्नुहोस् →" else "View All →", color = PosPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (topConsumables.isEmpty()) {
                        Text(
                            text = if (isNepali) "यस अवधिमा कुनै उत्पादन बिक्री भएको छैन।" else "No items sold in this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate600,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        topConsumables.take(4).forEachIndexed { index, product ->
                            ConsumableProductRow(
                                rank = index + 1,
                                product = product,
                                currency = currency,
                                totalRevenue = totalRevenue,
                                isNepali = isNepali
                            )
                            if (index < 3 && index < topConsumables.size - 1) {
                                HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// FISCAL YEAR SALES & TAX REGISTER (बिक्री खाता)
// -----------------------------------------------------------------------------
@Composable
private fun FiscalYearSalesTaxRegisterCard(
    fiscalYear: FiscalYearEntity?,
    taxableSales: Long,
    taxExemptSales: Long,
    totalTax: Long,
    totalRevenue: Long,
    totalOrders: Int,
    firstInvoice: String?,
    lastInvoice: String?,
    quarterlyStats: List<QuarterStat>,
    currency: String,
    isNepali: Boolean
) {
    if (fiscalYear == null) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PosEmerald.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("fiscal_year_sales_register_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFECFDF5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = PosEmerald,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (isNepali) "आर्थिक वर्ष बिक्री खाता" else "Fiscal Sales & Tax Register",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (fiscalYear.isCurrent) PosEmerald else Slate700)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (fiscalYear.isCurrent) "FY ${fiscalYear.name} (ACTIVE)" else "FY ${fiscalYear.name}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = if (isNepali) "आन्तरिक राजस्व विभाग (IRD) अनुपालन बिक्री र १३% भ्याट हिसाब • ${CartCalculator.formatMoney(totalRevenue, currency)}" else "IRD compliant Sales Register & VAT summary • ${CartCalculator.formatMoney(totalRevenue, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Slate100)
            Spacer(modifier = Modifier.height(14.dp))

            // Main 3-Column Register Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Taxable Sales Box
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFDCFCE7))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isNepali) "करयोग्य बिक्री (Taxable)" else "Taxable Sales",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate700,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CartCalculator.formatMoney(taxableSales, currency),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = PosEmerald
                        )
                        Text(
                            text = if (isNepali) "१३% भ्याट लाग्ने रकम" else "Subject to 13% VAT",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    }
                }

                // VAT Collected Box
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isNepali) "भ्याट संकलन (VAT 13%)" else "VAT (13%) Tax",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate700,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CartCalculator.formatMoney(totalTax, currency),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = PosPrimary
                        )
                        Text(
                            text = if (isNepali) "सरकारलाई दाखिला हुने" else "Payable to Tax Dept",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    }
                }

                // Non-taxable / Exempt Box
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isNepali) "कर छुट बिक्री (Exempt)" else "Tax Exempt Sales",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate700,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CartCalculator.formatMoney(taxExemptSales, currency),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = if (isNepali) "शून्य वा छुट वस्तु" else "Zero-rated / exempt",
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Invoice Sequence & Range Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Slate50,
                border = BorderStroke(1.dp, Slate100)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Slate600, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isNepali) "जारी बिल संख्या: $totalOrders" else "Consecutive Invoices: $totalOrders",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                    }
                    if (!firstInvoice.isNullOrBlank() && !lastInvoice.isNullOrBlank()) {
                        Text(
                            text = "$firstInvoice → $lastInvoice",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PosPrimary
                        )
                    } else {
                        Text(
                            text = "Next: ${FiscalYearHelper.formatFiscalInvoice(fiscalYear.invoicePrefix, fiscalYear.name, fiscalYear.currentInvoiceSequence + 1)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate600
                        )
                    }
                }
            }

            // Quarterly Breakdown Table / Grid
            if (quarterlyStats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isNepali) "त्रैमासिक बिक्री विवरण (Quarterly Breakdown):" else "Quarterly Breakdown (चौमासिक):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Slate700
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quarterlyStats.forEach { q ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate100)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = q.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PosPrimary
                                    )
                                    Text(
                                        text = "${q.invoicesCount} bills",
                                        fontSize = 9.sp,
                                        color = Slate600
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isNepali) q.labelNe else q.labelEn,
                                    fontSize = 9.sp,
                                    color = Slate600,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CartCalculator.formatMoney(q.revenue, currency),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "VAT: ${CartCalculator.formatMoney(q.vat, currency)}",
                                    fontSize = 9.sp,
                                    color = PosEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 1: INVOICES & TRANSACTIONS
// -----------------------------------------------------------------------------
@Composable
private fun InvoicesTabContent(
    sales: List<SaleEntity>,
    currency: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedPaymentType: String,
    onPaymentTypeChange: (String) -> Unit,
    onViewDetails: (SaleEntity) -> Unit,
    onReprint: (SaleEntity) -> Unit,
    isNepali: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter Row
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(if (isNepali) "बिल नम्बर वा क्यासियर खोज्नुहोस्..." else "Search invoice # or cashier...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotBlank()) {
                { IconButton(onClick = { onSearchChange("") }) { Icon(Icons.Default.Close, contentDescription = "Clear") } }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Payment Type Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val paymentFilters = listOf(
                "ALL" to if (isNepali) "सबै भुक्तानी" else "All Payments",
                "CASH" to if (isNepali) "नगद (Cash)" else "Cash",
                "CARD" to if (isNepali) "कार्ड (Card)" else "Card",
                "QR_WALLET" to if (isNepali) "क्युआर (QR)" else "QR Wallet",
                "CREDIT_DUE" to if (isNepali) "उधारो (Credit)" else "Credit"
            )

            paymentFilters.forEach { (typeKey, label) ->
                val isSelected = selectedPaymentType == typeKey
                FilterChip(
                    selected = isSelected,
                    onClick = { onPaymentTypeChange(typeKey) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Slate800,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (sales.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(48.dp), tint = Slate600.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isNepali) "कुनै बिल फेला परेन।" else "No invoices match the filter criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate600
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sales, key = { it.id }) { sale ->
                    val dateFormatted = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(sale.createdAt))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewDetails(sale) }
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
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, tint = PosPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(sale.invoiceNo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                                        Text(dateFormatted, style = MaterialTheme.typography.bodySmall, color = Slate600)
                                    }
                                }

                                Text(
                                    text = CartCalculator.formatMoney(sale.grandTotalInCents, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PosEmerald
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Slate100)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (sale.paymentStatus == "COMPLETED") Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = sale.paymentStatus,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (sale.paymentStatus == "COMPLETED") Color(0xFF166534) else PosRose,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = "Staff: ${sale.createdBy.ifBlank { "Cashier" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate600,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PosOutlineButton(
                                        text = if (isNepali) "विवरण" else "Details",
                                        onClick = { onViewDetails(sale) }
                                    )

                                    PosCompactPillButton(
                                        text = if (isNepali) "प्रिन्ट" else "Receipt",
                                        icon = Icons.Default.Print,
                                        gradient = PosButtonGradients.Dark,
                                        onClick = { onReprint(sale) }
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

// -----------------------------------------------------------------------------
// TAB 2: CUSTOMER KHATA (RECEIVABLES & ADVANCES)
// -----------------------------------------------------------------------------
@Composable
private fun CustomerKhataTabContent(
    customers: List<CustomerEntity>,
    currency: String,
    onNavigateToCustomers: () -> Unit,
    isNepali: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") } // ALL, TO_RECEIVE, TO_GIVE, SETTLED

    val totalToReceive = remember(customers) {
        customers.filter { it.currentDueInCents > 0L }.sumOf { it.currentDueInCents }
    }
    val totalToGive = remember(customers) {
        customers.filter { it.currentDueInCents < 0L }.sumOf { kotlin.math.abs(it.currentDueInCents) }
    }
    val netBalance = totalToReceive - totalToGive
    val toReceiveCount = remember(customers) { customers.count { it.currentDueInCents > 0L } }
    val toGiveCount = remember(customers) { customers.count { it.currentDueInCents < 0L } }
    val settledCount = remember(customers) { customers.count { it.currentDueInCents == 0L } }

    val displayedCustomers = remember(customers, searchQuery, filterMode) {
        customers.filter { c ->
            val matchesSearch = searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.phone.contains(searchQuery, ignoreCase = true) ||
                c.email.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterMode) {
                "TO_RECEIVE" -> c.currentDueInCents > 0L
                "TO_GIVE" -> c.currentDueInCents < 0L
                "SETTLED" -> c.currentDueInCents == 0L
                else -> true
            }
            matchesSearch && matchesFilter
        }.sortedWith(
            compareByDescending<CustomerEntity> { it.currentDueInCents > 0L }
                .thenByDescending { it.currentDueInCents }
                .thenBy { it.name }
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Summary Cards: To Receive, To Give, and Net Balance
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                                text = if (isNepali) "खाता बक्यौता सारांश" else "Khata Balances Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = if (isNepali) "लिन बाँकी (Receivable) र दिन बाँकी (Payable/Advance)" else "Accounts Receivable & Customer Advances",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                        }

                        PosCompactPillButton(
                            text = if (isNepali) "ग्राहक खाता हब" else "Customer Ledger",
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            gradient = PosButtonGradients.Emerald,
                            onClick = onNavigateToCustomers
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // To Receive Mini Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF1F2),
                            border = BorderStroke(1.dp, Color(0xFFFFE4E6))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = PosRose,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isNepali) "पैसा लिन बाँकी" else "To Receive",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PosRose
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CartCalculator.formatMoney(totalToReceive, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PosRose
                                )
                                Text(
                                    text = "$toReceiveCount ${if (isNepali) "ग्राहक" else "customers"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }

                        // To Give Mini Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = PosPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isNepali) "पैसा दिन बाँकी" else "To Give",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PosPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CartCalculator.formatMoney(totalToGive, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PosPrimary
                                )
                                Text(
                                    text = "$toGiveCount ${if (isNepali) "ग्राहक" else "customers"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }

                        // Net Balance Mini Card
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Slate100)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isNepali) "खुद बक्यौता (Net)" else "Net Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CartCalculator.formatMoney(netBalance, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netBalance > 0) PosRose else if (netBalance < 0) PosPrimary else PosEmerald
                                )
                                Text(
                                    text = if (netBalance > 0) {
                                        if (isNepali) "खुद लिनु पर्ने" else "Net Receivable"
                                    } else if (netBalance < 0) {
                                        if (isNepali) "खुद दिनु पर्ने" else "Net Payable"
                                    } else {
                                        if (isNepali) "बराबर" else "Balanced"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Slate600
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search & Filter Row
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (isNepali) "ग्राहकको नाम वा फोन नम्बर खोज्नुहोस्..." else "Search customer name or phone...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate600) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate600)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterMode == "ALL",
                            onClick = { filterMode = "ALL" },
                            label = { Text("${if (isNepali) "सबै" else "All"} (${customers.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Slate800,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        FilterChip(
                            selected = filterMode == "TO_RECEIVE",
                            onClick = { filterMode = "TO_RECEIVE" },
                            label = { Text("🔴 ${if (isNepali) "लिन बाँकी" else "To Receive"} ($toReceiveCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PosRose,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        FilterChip(
                            selected = filterMode == "TO_GIVE",
                            onClick = { filterMode = "TO_GIVE" },
                            label = { Text("🔵 ${if (isNepali) "दिन बाँकी" else "To Give"} ($toGiveCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PosPrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        FilterChip(
                            selected = filterMode == "SETTLED",
                            onClick = { filterMode = "SETTLED" },
                            label = { Text("🟢 ${if (isNepali) "चुक्ता" else "Settled"} ($settledCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PosEmerald,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
            }
        }

        // Customer Cards List
        if (displayedCustomers.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isNepali) "कुनै पनि ग्राहक भेटिएन" else "No customers found for current filter",
                            color = Slate600
                        )
                    }
                }
            }
        } else {
            items(displayedCustomers, key = { it.id }) { customer ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        when {
                                            customer.currentDueInCents > 0L -> Color(0xFFFFF1F2)
                                            customer.currentDueInCents < 0L -> Color(0xFFEFF6FF)
                                            else -> Color(0xFFF1F5F9)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = customer.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        customer.currentDueInCents > 0L -> PosRose
                                        customer.currentDueInCents < 0L -> PosPrimary
                                        else -> Slate700
                                    }
                                )
                            }

                            Column {
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = customer.phone.ifBlank { "No phone" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                                if (customer.email.isNotBlank()) {
                                    Text(
                                        text = customer.email,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate600
                                    )
                                }
                            }
                        }

                        // Due Status & Balance Badge
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    customer.currentDueInCents > 0L -> Color(0xFFFFF1F2)
                                    customer.currentDueInCents < 0L -> Color(0xFFEFF6FF)
                                    else -> Color(0xFFECFDF5)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    val icon = when {
                                        customer.currentDueInCents > 0L -> Icons.Default.ArrowDownward
                                        customer.currentDueInCents < 0L -> Icons.Default.ArrowUpward
                                        else -> null
                                    }
                                    if (icon != null) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (customer.currentDueInCents > 0L) PosRose else PosPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(
                                        text = when {
                                            customer.currentDueInCents > 0L -> if (isNepali) "लिन बाँकी" else "To Receive"
                                            customer.currentDueInCents < 0L -> if (isNepali) "दिन बाँकी" else "To Give"
                                            else -> if (isNepali) "चुक्ता" else "Settled"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            customer.currentDueInCents > 0L -> PosRose
                                            customer.currentDueInCents < 0L -> PosPrimary
                                            else -> PosEmerald
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = CartCalculator.formatMoney(kotlin.math.abs(customer.currentDueInCents), currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    customer.currentDueInCents > 0L -> PosRose
                                    customer.currentDueInCents < 0L -> PosPrimary
                                    else -> PosEmerald
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 3: TOP CONSUMABLE PRODUCTS & FMCG ANALYTICS
// -----------------------------------------------------------------------------
@Composable
private fun ConsumablesTabContent(
    consumables: List<ConsumableProductStat>,
    categories: List<CategoryEntity>,
    selectedCategoryFilter: String,
    onCategoryFilterChange: (String) -> Unit,
    currency: String,
    totalRevenue: Long,
    isNepali: Boolean
) {
    var searchItemText by remember { mutableStateOf("") }
    var stockHealthFilter by remember { mutableStateOf("ALL") } // ALL, LOW, OUT, HEALTHY
    var sortMode by remember { mutableStateOf(ConsumableSortMode.UNITS_SOLD) }

    // Filter and Sort consumables
    val displayedConsumables = remember(consumables, searchItemText, stockHealthFilter, selectedCategoryFilter, sortMode) {
        val filtered = consumables.filter { item ->
            val matchesSearch = searchItemText.isBlank() ||
                item.productName.contains(searchItemText, ignoreCase = true) ||
                item.sku.contains(searchItemText, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == "ALL" || item.categoryId == selectedCategoryFilter
            val matchesHealth = when (stockHealthFilter) {
                "LOW" -> item.currentStock in 1..item.minStockAlert
                "OUT" -> item.currentStock <= 0
                "HEALTHY" -> item.currentStock > item.minStockAlert
                else -> true
            }
            matchesSearch && matchesCategory && matchesHealth
        }

        when (sortMode) {
            ConsumableSortMode.UNITS_SOLD -> filtered.sortedByDescending { it.unitsSold }
            ConsumableSortMode.REVENUE -> filtered.sortedByDescending { it.totalRevenueInCents }
            ConsumableSortMode.GROSS_PROFIT -> filtered.sortedByDescending { it.grossProfitInCents }
            ConsumableSortMode.STOCK_URGENCY -> filtered.sortedBy { it.currentStock }
            ConsumableSortMode.VELOCITY -> filtered.sortedByDescending { it.unitsPerDay }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Sort row
        OutlinedTextField(
            value = searchItemText,
            onValueChange = { searchItemText = it },
            placeholder = { Text(if (isNepali) "उपभोग्य वस्तु वा SKU खोज्नुहोस्..." else "Search consumable items or SKU...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchItemText.isNotBlank()) {
                { IconButton(onClick = { searchItemText = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear") } }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stock Health Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val healthFilters = listOf(
                "ALL" to (if (isNepali) "सबै उपभोग्य वस्तु" else "All Items"),
                "LOW" to (if (isNepali) "⚠️ कम स्टक (Low Stock)" else "⚠️ Low Stock"),
                "OUT" to (if (isNepali) "🚨 सकिएको (Out of Stock)" else "🚨 Out of Stock"),
                "HEALTHY" to (if (isNepali) "✅ पर्याप्त स्टक" else "✅ Healthy Stock")
            )

            healthFilters.forEach { (hKey, label) ->
                val isSelected = stockHealthFilter == hKey
                FilterChip(
                    selected = isSelected,
                    onClick = { stockHealthFilter = hKey },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (hKey == "OUT" || hKey == "LOW") PosRose else Slate800,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }

        if (categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == "ALL",
                    onClick = { onCategoryFilterChange("ALL") },
                    label = { Text(if (isNepali) "सबै वर्ग" else "All Categories", fontSize = 11.sp) },
                    shape = RoundedCornerShape(6.dp)
                )
                categories.forEach { cat ->
                    val isCatSelected = selectedCategoryFilter == cat.id
                    FilterChip(
                        selected = isCatSelected,
                        onClick = { onCategoryFilterChange(cat.id) },
                        label = { Text(cat.name, fontSize = 11.sp) },
                        shape = RoundedCornerShape(6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sorting Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isNepali) "क्रमबद्ध:" else "Sort by:", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.SemiBold)
            ConsumableSortMode.entries.forEach { mode ->
                val isSelected = sortMode == mode
                val label = if (isNepali) mode.labelNe else mode.labelEn
                FilterChip(
                    selected = isSelected,
                    onClick = { sortMode = mode },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PosPrimary,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (displayedConsumables.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isNepali) "यस अवधिमा कुनै वस्तुको बिक्री वा मापदण्ड मिलेन।" else "No consumable products match this filter criteria.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(displayedConsumables, key = { _, item -> item.productId }) { index, item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            ConsumableProductRow(
                                rank = index + 1,
                                product = item,
                                currency = currency,
                                totalRevenue = totalRevenue,
                                isNepali = isNepali
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 3: SHIFT HISTORY & Z-REPORTS
// -----------------------------------------------------------------------------
@Composable
private fun ShiftsTabContent(
    shifts: List<ShiftEntity>,
    currency: String,
    isNepali: Boolean
) {
    if (shifts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (isNepali) "कुनै शिफ्ट इतिहास फेला परेन।" else "No shift sessions recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate600
            )
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shifts, key = { it.id }) { shift ->
                val openTimeStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(shift.openedAt))
                val closeTimeStr = shift.closedAt?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it)) } ?: (if (isNepali) "सक्रिय" else "Active")
                val variance = shift.varianceInCents ?: 0L
                val openingFloat = shift.openingFloatInCents
                val expectedCash = shift.expectedCashInCents ?: 0L
                val countedCash = shift.countedCashInCents ?: 0L

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                                    text = "Shift #${shift.id.takeLast(6).uppercase()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Text(
                                    text = "$openTimeStr → $closeTimeStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (shift.status == "OPEN") Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = shift.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (shift.status == "OPEN") Color(0xFF166534) else Slate700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Slate100)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(if (isNepali) "सुरुवात नगद" else "Opening Float", style = MaterialTheme.typography.labelSmall, color = Slate600)
                                Text(CartCalculator.formatMoney(openingFloat, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text(if (isNepali) "अपेक्षित नगद" else "Expected Cash", style = MaterialTheme.typography.labelSmall, color = Slate600)
                                Text(CartCalculator.formatMoney(expectedCash, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text(if (isNepali) "गणना नगद" else "Counted Cash", style = MaterialTheme.typography.labelSmall, color = Slate600)
                                Text(CartCalculator.formatMoney(countedCash, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (isNepali) "फरक" else "Variance", style = MaterialTheme.typography.labelSmall, color = Slate600)
                                Text(
                                    text = CartCalculator.formatMoney(variance, currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (variance == 0L) PosEmerald else if (variance < 0L) PosRose else PosPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// REUSABLE COMPONENTS
// -----------------------------------------------------------------------------
@Composable
private fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = Slate700, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
        }
    }
}

@Composable
private fun TenderProgressBar(
    label: String,
    amount: Long,
    total: Long,
    currency: String,
    color: Color
) {
    val progress = (amount.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val pctFloat = progress * 100f
    val percentageStr = if (pctFloat % 1f == 0f) {
        "${pctFloat.toInt()}%"
    } else {
        String.format(Locale.US, "%.1f%%", pctFloat)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Slate800)
            Text(
                text = "${CartCalculator.formatMoney(amount, currency)} ($percentageStr)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Slate100,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun ConsumableProductRow(
    rank: Int,
    product: ConsumableProductStat,
    currency: String,
    totalRevenue: Long,
    isNepali: Boolean
) {
    val revenueSharePercent = if (totalRevenue > 0) ((product.totalRevenueInCents.toDouble() / totalRevenue.toDouble()) * 100).toInt() else 0
    val rankColor = when (rank) {
        1 -> Color(0xFFD97706) // Gold
        2 -> Color(0xFF64748B) // Silver
        3 -> Color(0xFFB45309) // Bronze
        else -> PosPrimary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("#$rank", fontWeight = FontWeight.Bold, color = rankColor, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.productName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (product.categoryName.isNotBlank() && product.categoryName != "General") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = Slate100) {
                            Text(product.categoryName, fontSize = 10.sp, color = Slate600, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = "${product.unitsSold} ${if (isNepali) "एकाइ बिक्री" else "units sold"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate700,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    if (product.currentStock <= 0) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFEE2E2)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = PosRose, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    if (isNepali) "मौजदात सकियो" else "Out of Stock",
                                    color = PosRose,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (product.currentStock <= product.minStockAlert) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFFBEB)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = PosAmber, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    if (isNepali) "${product.currentStock} बाँकी (न्यून)" else "${product.currentStock} left (Low)",
                                    color = PosAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (isNepali) "• मौजदात: ${product.currentStock}" else "• Stock: ${product.currentStock}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }

                    // Only show runway when product is STILL in stock to prevent redundant 'Out of Stock' + 'Runway ~0d'
                    if (product.currentStock > 0 && product.estimatedDaysLeft != null && product.estimatedDaysLeft <= 7) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFEE2E2)) {
                            Text(
                                text = if (isNepali) "रनवे: ~${product.estimatedDaysLeft} दिन" else "Runway: ~${product.estimatedDaysLeft}d",
                                color = PosRose,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = CartCalculator.formatMoney(product.totalRevenueInCents, currency),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = PosEmerald
            )
            Text(
                text = if (isNepali) "कुल बिक्रीको $revenueSharePercent%" else "$revenueSharePercent% of store sales",
                style = MaterialTheme.typography.labelSmall,
                color = Slate600
            )
            if (product.grossProfitInCents > 0) {
                Text(
                    text = if (isNepali) "+${CartCalculator.formatMoney(product.grossProfitInCents, currency)} नाफा" else "+${CartCalculator.formatMoney(product.grossProfitInCents, currency)} profit",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ITEMIZED INVOICE DETAILS DIALOG
// -----------------------------------------------------------------------------
@Composable
private fun InvoiceDetailsDialog(
    sale: SaleEntity,
    items: List<SaleItemEntity>,
    payments: List<SalePaymentEntity>,
    isLoading: Boolean,
    currency: String,
    onDismiss: () -> Unit,
    onReprint: () -> Unit,
    isNepali: Boolean
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(sale.createdAt))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(sale.invoiceNo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Slate900)
                        Text(dateStr, style = MaterialTheme.typography.bodySmall, color = Slate600)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Slate100)
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PosPrimary)
                    }
                } else {
                    Text(
                        text = if (isNepali) "बिल वस्तुहरू (${items.size})" else "Purchased Items (${items.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(items) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Slate900)
                                    Text(
                                        text = "${item.quantity} x ${CartCalculator.formatMoney(item.unitPriceInCents, currency)}",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                                Text(
                                    text = CartCalculator.formatMoney(item.lineTotalInCents, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Slate900
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Slate100)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Financial Summary Breakdown
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isNepali) "उप-जम्मा" else "Subtotal", style = MaterialTheme.typography.bodySmall, color = Slate600)
                        Text(CartCalculator.formatMoney(sale.subtotalInCents, currency), style = MaterialTheme.typography.bodySmall, color = Slate900)
                    }
                    if (sale.discountInCents > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "छुट" else "Discount", style = MaterialTheme.typography.bodySmall, color = PosRose)
                            Text("-${CartCalculator.formatMoney(sale.discountInCents, currency)}", style = MaterialTheme.typography.bodySmall, color = PosRose)
                        }
                    }
                    if (sale.taxInCents > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "कर / भ्याट" else "Tax / VAT", style = MaterialTheme.typography.bodySmall, color = Slate600)
                            Text(CartCalculator.formatMoney(sale.taxInCents, currency), style = MaterialTheme.typography.bodySmall, color = Slate900)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isNepali) "कुल जम्मा" else "Grand Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Slate900)
                        Text(CartCalculator.formatMoney(sale.grandTotalInCents, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PosEmerald)
                    }

                    // Payments
                    if (payments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isNepali) "भुक्तानी माध्यम: ${payments.joinToString { it.paymentType.name }}" else "Tender: ${payments.joinToString { it.paymentType.name }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    PosOutlineButton(
                        text = if (isNepali) "बन्द गर्नुहोस्" else "Close",
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PosGradientButton(
                        text = if (isNepali) "रसिद छाप्नुहोस्" else "Reprint Receipt",
                        icon = Icons.Default.Print,
                        gradient = PosButtonGradients.Primary,
                        onClick = onReprint
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CUSTOM DATE RANGE DIALOG
// -----------------------------------------------------------------------------
@Composable
private fun CustomDateRangeDialog(
    currentStart: Long,
    currentEnd: Long,
    onDismiss: () -> Unit,
    onApply: (start: Long, end: Long) -> Unit,
    isNepali: Boolean
) {
    val now = System.currentTimeMillis()
    val initialStartDays = ((now - currentStart) / (24 * 60 * 60 * 1000L)).toInt().coerceIn(0, 365)
    val initialEndDays = ((now - currentEnd) / (24 * 60 * 60 * 1000L)).toInt().coerceIn(0, 365)
    var startDaysAgo by remember { mutableIntStateOf(initialStartDays) }
    var endDaysAgo by remember { mutableIntStateOf(initialEndDays) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (isNepali) "अनुकूलित मिति दायरा" else "Custom Date Range",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
                Text(
                    text = if (isNepali) "रिपोर्ट विश्लेषणको लागि मिति छनोट गर्नुहोस्" else "Select dynamic reporting range or preset windows",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Presets
                Text(if (isNepali) "द्रुत चयन:" else "Quick Presets:", style = MaterialTheme.typography.labelSmall, color = Slate700, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                val presets = listOf(
                    Triple(if (isNepali) "पछिल्लो ३ दिन" else "Last 3 Days", 3, 0),
                    Triple(if (isNepali) "पछिल्लो १४ दिन" else "Last 14 Days", 14, 0),
                    Triple(if (isNepali) "पछिल्लो ६० दिन" else "Last 60 Days", 60, 0),
                    Triple(if (isNepali) "पछिल्लो ९० दिन" else "Last 90 Days", 90, 0)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (label, startAgo, endAgo) ->
                        val isSelected = startDaysAgo == startAgo && endDaysAgo == endAgo
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                startDaysAgo = startAgo
                                endDaysAgo = endAgo
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calculated Date Preview
                val previewStart = now - (startDaysAgo.toLong() * 24 * 60 * 60 * 1000L)
                val previewEnd = now - (endDaysAgo.toLong() * 24 * 60 * 60 * 1000L)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate50),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "सुरुवात मिति:" else "Start Date:", style = MaterialTheme.typography.bodySmall, color = Slate600)
                            Text(dateFormat.format(Date(previewStart)), fontWeight = FontWeight.Bold, color = Slate900)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isNepali) "अन्त्य मिति:" else "End Date:", style = MaterialTheme.typography.bodySmall, color = Slate600)
                            Text(dateFormat.format(Date(previewEnd)), fontWeight = FontWeight.Bold, color = Slate900)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    PosOutlineButton(
                        text = if (isNepali) "रद्द गर्नुहोस्" else "Cancel",
                        onClick = onDismiss
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PosGradientButton(
                        text = if (isNepali) "लागू गर्नुहोस्" else "Apply Range",
                        gradient = PosButtonGradients.Primary,
                        onClick = { onApply(previewStart, previewEnd) }
                    )
                }
            }
        }
    }
}
