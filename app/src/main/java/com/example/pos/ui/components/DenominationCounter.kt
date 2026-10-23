package com.example.pos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.model.DenominationItem
import com.example.pos.domain.CartCalculator
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary

val STANDARD_DENOMINATIONS_LIST = listOf(1000, 500, 100, 50, 20, 10, 5, 2, 1)

/**
 * Enhanced Enterprise Denomination Counter.
 * Key UI/UX improvements:
 * 1. Read-only Hero Summary Card with single-line non-wrapping total and note count indicator.
 * 2. One-tap Quick Float Presets (Rs 0, Rs 1,000, Rs 2,000, Rs 5,000, Rs 10,000, Match Expected).
 * 3. Distinct visual banknote & coin styling (Nepali rupee color accents).
 * 4. High-contrast, unclipped centered stepper input fields using BasicTextField.
 * 5. Instant subtotal calculation per denomination.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DenominationCounter(
    modifier: Modifier = Modifier,
    currencySymbol: String = "Rs",
    expectedOpeningFloat: Long = 0L,
    initialCounts: List<DenominationItem> = emptyList(),
    onCountsChanged: (List<DenominationItem>, Long) -> Unit
) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    var denominations by remember {
        val initialDenoms = STANDARD_DENOMINATIONS_LIST.toMutableList()
        initialCounts.forEach { item ->
            if (!initialDenoms.contains(item.denomination)) {
                initialDenoms.add(item.denomination)
            }
        }
        mutableStateOf(initialDenoms.sortedDescending())
    }

    val countMap = remember {
        mutableStateMapOf<Int, Int>().apply {
            initialCounts.forEach { item ->
                put(item.denomination, item.count)
            }
        }
    }

    var showAddCustomDialog by remember { mutableStateOf(false) }
    var customDenomInput by remember { mutableStateOf("") }
    var customDenomError by remember { mutableStateOf<String?>(null) }

    // Derive grand total strictly from countMap
    val totalCents = remember(countMap.toMap(), denominations) {
        var sum = 0L
        for (denom in denominations) {
            val count = countMap[denom] ?: 0
            sum += (denom.toLong() * count.toLong()) * 100L
        }
        sum
    }

    val totalPieces = remember(countMap.toMap()) {
        countMap.values.sum()
    }

    // Trigger parent callback when counts change
    LaunchedEffect(totalCents, countMap.toMap()) {
        val list = denominations.map { denom ->
            DenominationItem(denom, countMap[denom] ?: 0)
        }
        onCountsChanged(list, totalCents)
    }

    // Smart greedy distributor for quick preset floats
    fun applyPreset(targetFloatCents: Long) {
        countMap.clear()
        if (targetFloatCents <= 0L) return

        var remainingCents = targetFloatCents
        for (denom in denominations) {
            val denomCents = denom.toLong() * 100L
            if (remainingCents >= denomCents) {
                val pieces = (remainingCents / denomCents).toInt()
                countMap[denom] = pieces
                remainingCents %= denomCents
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Total Counted Cash Hero Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 1.dp,
            color = Color.White,
            border = BorderStroke(
                1.5.dp,
                if (totalCents > 0) PosEmerald.copy(alpha = 0.5f) else Color(0xFFBFDBFE)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            if (totalCents > 0) {
                                listOf(Color(0xFFF0FDF4), Color(0xFFF8FAFC))
                            } else {
                                listOf(Color(0xFFF8FAFC), Color(0xFFEFF6FF))
                            }
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Icon and Title
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (totalCents > 0) PosEmerald.copy(alpha = 0.15f)
                                    else PosPrimary.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (totalCents > 0) PosEmerald else PosPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNepali) "जम्मा गनिएको नगद" else "TOTAL COUNTED CASH",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (totalCents > 0) Color(0xFF047857) else Color(0xFF1E3A8A),
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (totalPieces > 0) (if (isNepali) "$totalPieces नोट तथा सिक्का" else "$totalPieces notes & coins") else (if (isNepali) "दरबन्दी अनुसार गणना गरिएको" else "Calculated from denominations"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (totalPieces > 0) PosEmerald else Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = if (totalPieces > 0) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right: Guaranteed Single-Line Formatted Money
                    Text(
                        text = CartCalculator.formatMoney(totalCents, currencySymbol),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        fontSize = if (totalCents >= 10000000L) 17.sp else 19.sp,
                        color = if (totalCents > 0) PosEmerald else PosPrimary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.testTag("denomination_grand_total")
                    )
                }
            }
        }

        // 2. Quick Float Presets (Horizontal Scrollable Pills for Faster Register Setup)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isNepali) "द्रुत चयन:" else "Quick Presets:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 6.dp)
            )

            val presets = remember(expectedOpeningFloat) {
                val list = mutableListOf(0L, 100000L, 200000L, 500000L, 1000000L)
                if (expectedOpeningFloat > 0 && !list.contains(expectedOpeningFloat)) {
                    list.add(1, expectedOpeningFloat)
                }
                list
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { presetCents ->
                    val isSelected = totalCents == presetCents && (totalCents > 0 || totalPieces == 0)
                    val label = if (presetCents == 0L) {
                        if (isNepali) "शून्य (०)" else "Zero (0)"
                    } else if (presetCents == expectedOpeningFloat) {
                        if (isNepali) "अपेक्षित बराबर (${CartCalculator.formatMoney(presetCents, currencySymbol)})" else "Match Expected (${CartCalculator.formatMoney(presetCents, currencySymbol)})"
                    } else {
                        CartCalculator.formatMoney(presetCents, currencySymbol)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFFD1FAE5) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) PosEmerald else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.clickable { applyPreset(presetCents) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(11.dp))
                            }
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PosEmerald else Color(0xFF334155),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 3. Actions: Clear all & Add Custom Denomination
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { countMap.clear() },
                enabled = totalPieces > 0,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (totalPieces > 0) Color(0xFFEF4444) else Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isNepali) "सबै हटाउनुहोस्" else "Clear Counts",
                    fontSize = 12.sp,
                    color = if (totalPieces > 0) Color(0xFFEF4444) else Color(0xFF94A3B8),
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(
                onClick = {
                    customDenomInput = ""
                    customDenomError = null
                    showAddCustomDialog = true
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = PosPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isNepali) "+ अन्य दर थप्नुहोस्" else "Add Denomination", fontSize = 12.sp, color = PosPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        // 4. Denominations List with clean direct numeric inputs (No +/- steppers)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            denominations.forEach { denom ->
                val count = countMap[denom] ?: 0
                val subtotalCents = (denom.toLong() * count.toLong()) * 100L

                // Denomination category colors (Banknotes vs Coins)
                val isCoin = denom <= 5
                val (badgeBg, badgeText, badgeBorder) = when (denom) {
                    1000 -> Triple(Color(0xFFEDE9FE), Color(0xFF5B21B6), Color(0xFFDDD6FE))
                    500 -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), Color(0xFFFDE68A))
                    100 -> Triple(Color(0xFFDCFCE7), Color(0xFF166534), Color(0xFFBBF7D0))
                    50 -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), Color(0xFFBAE6FD))
                    20 -> Triple(Color(0xFFFFEDD5), Color(0xFFC2410C), Color(0xFFFED7AA))
                    10 -> Triple(Color(0xFFCCFBF1), Color(0xFF0F766E), Color(0xFF99F6E4))
                    else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Color(0xFFE2E8F0)) // Coins
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (count > 0) Color(0xFFF0FDF4) else Color(0xFFFAFAFA))
                        .border(
                            1.dp,
                            if (count > 0) PosEmerald.copy(alpha = 0.4f) else Color(0xFFE2E8F0),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Denomination badge (Banknote / Coin pill)
                    Box(
                        modifier = Modifier
                            .width(76.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeBorder, RoundedCornerShape(8.dp))
                            .padding(vertical = 5.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "$currencySymbol $denom",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeText,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (isCoin) {
                                Text("¢", fontSize = 9.sp, color = badgeText.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Direct Numeric Quantity Input Field (No +/- Steppers)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "×",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )

                        var textVal by remember(count) { mutableStateOf(if (count == 0) "" else count.toString()) }
                        Box(
                            modifier = Modifier
                                .width(74.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (count > 0) Color(0xFFECFDF5) else Color.White)
                                .border(
                                    1.dp,
                                    if (count > 0) PosEmerald else Color(0xFFCBD5E1),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            BasicTextField(
                                value = textVal,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }.take(5)
                                    textVal = filtered
                                    val parsed = filtered.toIntOrNull() ?: 0
                                    countMap[denom] = parsed
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    textAlign = TextAlign.End,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (count > 0) PosEmerald else Color(0xFF1E293B)
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                decorationBox = { innerTextField ->
                                    if (textVal.isEmpty()) {
                                        Text(
                                            text = "0",
                                            textAlign = TextAlign.End,
                                            style = TextStyle(
                                                textAlign = TextAlign.End,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 14.sp,
                                                color = Color(0xFF94A3B8)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    // Row Subtotal with monospace font and guaranteed non-wrapping width
                    Box(
                        modifier = Modifier.widthIn(min = 84.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = CartCalculator.formatMoney(subtotalCents, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (count > 0) PosEmerald else Color(0xFF64748B),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    // Dialog to add custom denomination
    if (showAddCustomDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = {
                Text(
                    if (isNepali) "नयाँ दर थप्नुहोस्" else "Add Custom Denomination",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        if (isNepali) "$currencySymbol मा दरको मान प्रविष्ट गर्नुहोस् (जस्तै २५, २५०, २०००):" else "Enter the denomination value in $currencySymbol (e.g. 25, 250, 2000):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customDenomInput,
                        onValueChange = {
                            customDenomInput = it.filter { ch -> ch.isDigit() }
                            customDenomError = null
                        },
                        label = { Text(if (isNepali) "दर ($currencySymbol)" else "Value ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = customDenomError != null,
                        supportingText = {
                            customDenomError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val value = customDenomInput.toIntOrNull()
                        if (value == null || value <= 0) {
                            customDenomError = if (isNepali) "कृपया मान्य सकारात्मक नम्बर प्रविष्ट गर्नुहोस्" else "Please enter a valid positive number"
                        } else if (denominations.contains(value)) {
                            customDenomError = if (isNepali) "$currencySymbol $value पहिले नै उपलब्ध छ" else "$currencySymbol $value already exists"
                        } else {
                            denominations = (denominations + value).sortedDescending()
                            showAddCustomDialog = false
                        }
                    }
                ) {
                    Text(if (isNepali) "थप्नुहोस्" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text(if (isNepali) "रद्द गर्नुहोस्" else "Cancel")
                }
            }
        )
    }
}
