package com.example.pos.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import com.example.pos.data.local.UserEntity
import com.example.pos.data.model.StaffRole
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosSky
import com.example.ui.theme.Slate900

@Composable
fun RoleSwitcherDialog(
    currentRole: StaffRole,
    users: List<UserEntity>,
    onVerifyAndSwitch: (targetUser: UserEntity, pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedUser by remember { mutableStateOf<UserEntity?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    val strings = LocalPosStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .widthIn(max = 480.dp)
            .testTag("role_switcher_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.switchOperatorTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.switchOperatorSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (users.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.noOtherStaff,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = strings.noOtherStaffDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                } else {
                    Text(
                        text = strings.selectOperatorPrompt,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    users.forEach { user ->
                        val isSelected = selectedUser?.id == user.id
                        val roleColor = when (user.role) {
                            StaffRole.OWNER -> PosEmerald
                            StaffRole.ADMIN -> PosPrimary
                            StaffRole.MANAGER -> PosSky
                            StaffRole.STOREKEEPER -> PosAmber
                            StaffRole.CASHIER -> Color(0xFF8B5CF6)
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) roleColor else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedUser = user
                                    enteredPin = ""
                                    pinError = null
                                },
                            color = if (isSelected) roleColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(roleColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.fullName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = roleColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.fullName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(roleColor)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (user.role == StaffRole.OWNER) (if (strings is com.example.pos.ui.i18n.NepaliStrings) "★ मालिक" else "★ OWNER") else user.role.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (user.role == currentRole) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFE2E8F0))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = strings.currentBadge,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF475569)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = user.email.ifBlank { strings.registeredStoreStaff },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = if (strings is com.example.pos.ui.i18n.NepaliStrings) "चयन गरिएको" else "Selected",
                                        tint = roleColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    selectedUser?.let { target ->
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                enteredPin = it
                                pinError = null
                            },
                            label = { Text(strings.enterOperatorPin(target.fullName)) },
                            placeholder = { Text(strings.pinPlaceholder) },
                            supportingText = { Text(strings.defaultPinHint, fontSize = 10.sp, color = Color(0xFF64748B)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PosPrimary) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = pinError != null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinError != null) {
                            Text(pinError!!, color = PosRose, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedUser != null) {
                PosGradientButton(
                    text = strings.confirmSwitch,
                    subtitle = "${strings.staffRole}: ${selectedUser?.role?.name}",
                    icon = Icons.Default.Security,
                    iconBadge = false,
                    gradient = PosButtonGradients.Primary,
                    shape = RoundedCornerShape(12.dp),
                    height = 46.dp,
                    onClick = {
                        if (enteredPin.isBlank()) {
                            pinError = strings.pleaseEnterPin
                            return@PosGradientButton
                        }
                        onVerifyAndSwitch(
                            selectedUser!!,
                            enteredPin,
                            { onDismiss() },
                            { err -> pinError = err }
                        )
                    }
                )
            }
        },
        dismissButton = {
            PosOutlineButton(
                text = strings.cancel,
                shape = RoundedCornerShape(12.dp),
                height = 46.dp,
                onClick = onDismiss
            )
        }
    )
}
