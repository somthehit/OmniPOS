package com.example.pos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pos.util.BiometricHelper
import com.example.pos.util.findFragmentActivity
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose

@Composable
fun CashierLockDialog(
    staffName: String,
    biometricEnabled: Boolean = false,
    onUnlock: (String) -> Boolean,
    onBiometricUnlock: () -> Unit = {},
) {
    val context = LocalContext.current
    val strings = LocalPosStrings.current
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val isBiometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }
    val canUseBiometric = biometricEnabled && isBiometricAvailable

    // Trigger biometric prompt on dialog open
    LaunchedEffect(Unit) {
        if (canUseBiometric) {
            val activity = context.findFragmentActivity()
            if (activity != null) {
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    title = strings.unlockTerminalTitle,
                    subtitle = strings.unlockTerminalSubtitle(staffName),
                    description = strings.unlockTerminalDesc,
                    negativeButtonText = strings.usePinButton,
                    onSuccess = { onBiometricUnlock() }
                )
            }
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 440.dp)
                .testTag("cashier_lock_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(strings.registerLocked, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = strings.operatorLabel(staffName),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // PIN dots indicator
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in 0 until 4) {
                        val isFilled = i < pin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> PosRose
                                        isFilled -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    }
                                )
                        )
                    }
                }

                if (isError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(strings.incorrectPin, style = MaterialTheme.typography.labelSmall, color = PosRose)
                }

                if (canUseBiometric) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            val activity = context.findFragmentActivity()
                            if (activity != null) {
                                BiometricHelper.showBiometricPrompt(
                                    activity = activity,
                                    title = strings.unlockTerminalTitle,
                                    subtitle = strings.unlockTerminalSubtitle(staffName),
                                    description = strings.unlockTerminalDesc,
                                    negativeButtonText = strings.usePinButton,
                                    onSuccess = { onBiometricUnlock() }
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PosPrimary),
                        border = BorderStroke(1.dp, PosPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .testTag("biometric_unlock_button")
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = strings.unlockWithFingerprint, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.unlockWithFingerprint, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Keypad grid
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                for (row in keys) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        for (key in row) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        isError = false
                                        when (key) {
                                            "C" -> pin = ""
                                            "DEL" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            else -> {
                                                if (pin.length < 4) {
                                                    val newPin = pin + key
                                                    pin = newPin
                                                    if (newPin.length == 4) {
                                                        val ok = onUnlock(newPin)
                                                        if (!ok) {
                                                            isError = true
                                                            pin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                                } else {
                                    Text(key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
