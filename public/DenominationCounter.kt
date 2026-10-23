package com.omnipos.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * One counter for opening, closing and admin force-close.
 * The total is derived from the rows and is READ-ONLY: nobody can type a total.
 */
@Composable
fun DenominationCounter(
    denominations: List<Int>,
    counts: Map<Int, Int>,
    onCountChange: (denomination: Int, count: Int) -> Unit,
    modifier: Modifier = Modifier,
    totalLabel: String = "Total cash",
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        denominations.forEach { d ->
            val c = counts[d] ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Rs ${formatNpr(d * 100L, withSymbol = false)}  ×", modifier = Modifier.width(92.dp), fontWeight = FontWeight.Medium)
                IconButton(onClick = { onCountChange(d, (c - 1).coerceAtLeast(0)) }, enabled = c > 0) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease $d")
                }
                OutlinedTextField(
                    value = if (c == 0) "" else c.toString(),
                    onValueChange = { onCountChange(d, it.filter(Char::isDigit).take(6).toIntOrNull() ?: 0) },
                    placeholder = { Text("0", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(76.dp),
                )
                IconButton(onClick = { onCountChange(d, c + 1) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase $d")
                }
                Text(
                    formatNpr(d.toLong() * c * 100L),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(totalLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatNpr(ShiftMath.sumDenominations(counts)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** If OmniPOS already has a PIN dialog, use that instead and delete this one. */
@Composable
fun PinDialog(
    title: String = "Enter PIN to confirm",
    error: String? = null,
    busy: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(pin) }, enabled = pin.length >= 4 && !busy) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}
