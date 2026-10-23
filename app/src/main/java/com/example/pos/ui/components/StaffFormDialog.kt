package com.example.pos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pos.data.local.UserEntity
import com.example.pos.data.model.StaffRole
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosSky
import com.example.ui.theme.Slate900
import kotlin.random.Random

data class RoleDefinition(
    val role: StaffRole,
    val title: String,
    val nepaliTitle: String,
    val badge: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun StaffFormDialog(
    existingUsers: List<UserEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSaveStaff: (fullName: String, email: String, phone: String, role: StaffRole, pin: String) -> Unit
) {
    val strings = LocalPosStrings.current

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(StaffRole.CASHIER) }
    var pin by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }

    val roleDefinitions = remember(strings) {
        listOf(
            RoleDefinition(
                role = StaffRole.CASHIER,
                title = strings.roleCashierTitle,
                nepaliTitle = "क्यासियर",
                badge = strings.roleCashierBadge,
                description = strings.roleCashierDesc,
                icon = Icons.Default.PointOfSale,
                color = Color(0xFF8B5CF6)
            ),
            RoleDefinition(
                role = StaffRole.STOREKEEPER,
                title = strings.roleStorekeeperTitle,
                nepaliTitle = "स्टोरकिपर",
                badge = strings.roleStorekeeperBadge,
                description = strings.roleStorekeeperDesc,
                icon = Icons.Default.Inventory,
                color = PosAmber
            ),
            RoleDefinition(
                role = StaffRole.MANAGER,
                title = strings.roleManagerTitle,
                nepaliTitle = "प्रबन्धक",
                badge = strings.roleManagerBadge,
                description = strings.roleManagerDesc,
                icon = Icons.Default.Security,
                color = PosSky
            ),
            RoleDefinition(
                role = StaffRole.ADMIN,
                title = strings.roleAdminTitle,
                nepaliTitle = "एडमिन",
                badge = strings.roleAdminBadge,
                description = strings.roleAdminDesc,
                icon = Icons.Default.AdminPanelSettings,
                color = PosPrimary
            ),
            RoleDefinition(
                role = StaffRole.OWNER,
                title = strings.roleOwnerTitle,
                nepaliTitle = "मालिक",
                badge = strings.roleOwnerBadge,
                description = strings.roleOwnerDesc,
                icon = Icons.Default.Security,
                color = PosEmerald
            )
        )
    }

    val activeRoleDef = roleDefinitions.firstOrNull { it.role == selectedRole } ?: roleDefinitions.first()

    // Initials calculation for live avatar preview
    val initials = remember(fullName) {
        val parts = fullName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> ""
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 620.dp)
                .heightIn(max = 760.dp)
                .testTag("add_staff_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PosPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = PosPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = strings.addStaffMember,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.addStaffSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Live Operator Preview Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = activeRoleDef.color.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, activeRoleDef.color.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Circle
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(activeRoleDef.color),
                                contentAlignment = Alignment.Center
                            ) {
                                if (initials.isNotBlank()) {
                                    Text(
                                        text = initials,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (fullName.isNotBlank()) fullName else strings.staffAvatarFallback,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = activeRoleDef.color
                                    ) {
                                        Text(
                                            text = activeRoleDef.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = if (email.isNotBlank()) email else strings.staffAvatarEmailFallback(if (pin.isNotBlank()) "●".repeat(pin.length) else strings.none),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = activeRoleDef.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = activeRoleDef.color,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 2. Personal Information Section
                    Text(
                        text = strings.staffPersonalInfoSection,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (nameError != null && it.isNotBlank()) nameError = null
                        },
                        label = { Text("${strings.staffFullName} *") },
                        placeholder = { Text(strings.staffNamePlaceholder) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PosPrimary)
                        },
                        trailingIcon = {
                            if (fullName.isNotBlank()) {
                                IconButton(onClick = { fullName = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = strings.clear, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        isError = nameError != null,
                        supportingText = {
                            if (nameError != null) {
                                Text(nameError!!, color = PosRose, fontWeight = FontWeight.Bold)
                            } else {
                                Text(strings.staffNameHelper, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Email Address
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailError != null) emailError = null
                        },
                        label = { Text("${strings.staffEmail} *") },
                        placeholder = { Text("ramesh.pos@company.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = PosPrimary)
                        },
                        trailingIcon = {
                            if (email.isNotBlank()) {
                                IconButton(onClick = { email = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = strings.clear, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        isError = emailError != null,
                        supportingText = {
                            if (emailError != null) {
                                Text(emailError!!, color = PosRose, fontWeight = FontWeight.Bold)
                            } else {
                                Text(strings.staffEmailHelper, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Phone Number
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("${strings.staffPhone} (${strings.optional})") },
                        placeholder = { Text("98XXXXXXXX") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = PosPrimary)
                        },
                        trailingIcon = {
                            if (phone.isNotBlank()) {
                                IconButton(onClick = { phone = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = strings.clear, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        supportingText = {
                            Text(strings.staffPhoneHelper(strings.staffPhone), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 3. Role & Permissions Selection Matrix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "2. ${strings.staffRole} *",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.staffRolesAvailable(roleDefinitions.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        roleDefinitions.forEach { rDef ->
                            val isSelected = selectedRole == rDef.role
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedRole = rDef.role },
                                color = if (isSelected) rDef.color.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) rDef.color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) rDef.color else rDef.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = rDef.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else rDef.color,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = rDef.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) rDef.color else MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSelected) rDef.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = rDef.badge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) rDef.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = rDef.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) rDef.color else Color.Transparent)
                                            .border(
                                                1.5.dp,
                                                if (isSelected) rDef.color else MaterialTheme.colorScheme.outlineVariant,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 4. Access PIN Setup Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "3. ${strings.staffAccessPin} *",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Auto-generate PIN Button
                        OutlinedButton(
                            onClick = {
                                val randomPin = (1000 + Random.nextInt(9000)).toString()
                                pin = randomPin
                                isPinVisible = true
                                pinError = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, PosPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = PosPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.generatePin,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PosPrimary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }.take(6)
                            pin = digitsOnly
                            if (pinError != null && digitsOnly.length >= 4) pinError = null
                        },
                        label = { Text(strings.staffPinLabel) },
                        placeholder = { Text(strings.staffPinPlaceholder) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = PosPrimary)
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (pin.isNotBlank()) {
                                    IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                        Icon(
                                            imageVector = if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isPinVisible) strings.staffPinHideTip else strings.staffPinShowTip,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        isError = pinError != null,
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (pinError != null) {
                                    Text(pinError!!, color = PosRose, fontWeight = FontWeight.Bold)
                                } else {
                                    Text(strings.staffPinHelper, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${pin.length}/6", style = MaterialTheme.typography.labelSmall, color = if (pin.length >= 4) PosEmerald else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // PIN Progress Dots Visualizer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 6) {
                            val isFilled = i < pin.length
                            val isRequiredMet = pin.length >= 4
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                             isFilled && isRequiredMet -> PosEmerald
                                             isFilled -> PosAmber
                                             else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (pin.length >= 4) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PosEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.staffValidPinReady,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PosEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Security Info Notice
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = PosPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = strings.staffPinUsage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Sticky Footer Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PosOutlineButton(
                        text = strings.cancel,
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                        height = 48.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    PosGradientButton(
                        text = strings.saveStaffButton,
                        subtitle = strings.staffRoleSubtitle(activeRoleDef.title),
                        onClick = {
                            var hasError = false

                            if (fullName.trim().length < 2) {
                                nameError = strings.staffNameErrorShort
                                hasError = true
                            }

                            val cleanEmail = email.trim().lowercase()
                            if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                emailError = strings.staffEmailInvalid
                                hasError = true
                            } else if (existingUsers.any { it.email.trim().lowercase() == cleanEmail }) {
                                emailError = strings.staffEmailDuplicate
                                hasError = true
                            }

                            if (pin.length < 4) {
                                pinError = strings.staffPinLengthError
                                hasError = true
                            }

                            if (!hasError) {
                                onSaveStaff(fullName.trim(), cleanEmail, phone.trim(), selectedRole, pin.trim())
                                onDismiss()
                            }
                        },
                        gradient = PosButtonGradients.Primary,
                        icon = Icons.Default.PersonAdd,
                        iconBadge = true,
                        shape = RoundedCornerShape(14.dp),
                        height = 48.dp,
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                        testTag = "save_staff_button"
                    )
                }
            }
        }
    }
}
