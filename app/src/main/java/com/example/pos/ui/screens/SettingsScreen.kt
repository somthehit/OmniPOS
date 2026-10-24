package com.example.pos.ui.screens
import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.example.pos.util.SecurityTimerUtils
import com.example.pos.util.BiometricHelper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.local.FiscalYearEntity
import com.example.pos.data.local.UserEntity
import com.example.pos.data.model.StaffRole
import com.example.pos.data.model.TaxMode
import com.example.pos.domain.FiscalYearHelper
import com.example.pos.ui.PosViewModel
import com.example.pos.ui.i18n.NepaliStrings
import com.example.pos.ui.components.PosButtonGradients
import com.example.pos.ui.components.PosCompactPillButton
import com.example.pos.ui.components.PosDestructiveButton
import com.example.pos.ui.components.PosGradientButton
import com.example.pos.ui.components.PosOutlineButton
import com.example.pos.ui.components.StaffFormDialog
import com.example.pos.ui.i18n.AppLanguage
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.ui.theme.PosAmber
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosSky
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BiometricTarget {
    TERMINAL_UNLOCK,
    LOGIN_AUTH
}

@Composable
fun SettingsScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val strings = LocalPosStrings.current
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val org by viewModel.organization.collectAsState()
    val session by viewModel.currentSession.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val rawUsers by viewModel.users.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val discoveredPrinters by viewModel.discoveredPrinters.collectAsState()
    val isScanningPrinters by viewModel.isScanningPrinters.collectAsState()
    val isConnectingPrinter by viewModel.isConnectingPrinter.collectAsState()
    val printerConnectionStatus by viewModel.printerConnectionStatus.collectAsState()
    val fiscalYears by viewModel.fiscalYears.collectAsState()
    val activeFiscalYear by viewModel.activeFiscalYear.collectAsState()

    val bluetoothPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        arrayOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.all { it }
        if (granted) {
            viewModel.scanPrinters()
        } else {
            viewModel.setAlert("Bluetooth permissions are required to scan and connect printers.")
        }
    }

    val openBluetoothSettings: () -> Unit = {
        try {
            val intent = Intent(AndroidSettings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            viewModel.setAlert("Could not open Bluetooth settings: ${e.message}")
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.scanPrinters()
    }

    val realUsers = remember(rawUsers) {
        rawUsers.distinctBy { staff ->
            if (staff.email.isNotBlank()) staff.email.trim().lowercase() else staff.id
        }
    }

    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    // Dialog states
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditStoreDialog by remember { mutableStateOf(false) }
    var showFiscalYearDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showSwitchOperatorDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }
    var showLinkGmailDialog by remember { mutableStateOf(false) }
    var pendingRestoreParsed by remember { mutableStateOf<com.example.pos.domain.ParsedBackup?>(null) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var backupStatusIsError by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (content.isNullOrBlank()) {
                    backupStatusMessage = "Selected file is empty."
                    backupStatusIsError = true
                } else {
                    val targetOrgId = org?.id ?: "default_org"
                    val parsed = com.example.pos.domain.DataBackupManager.parseBackupJson(content, targetOrgId)
                    pendingRestoreJson = content
                    pendingRestoreParsed = parsed
                }
            } catch (e: Exception) {
                backupStatusMessage = "Invalid backup file: ${e.message}"
                backupStatusIsError = true
            }
        }
    }

    // --- Real Database Backed State Holders ---
    // Sales & Checkout Setup State
    var allowCash by remember(storeSettings) { mutableStateOf(storeSettings?.allowCash ?: true) }
    var allowCard by remember(storeSettings) { mutableStateOf(storeSettings?.allowCard ?: true) }
    var allowQr by remember(storeSettings) { mutableStateOf(storeSettings?.allowQr ?: true) }
    var allowCredit by remember(storeSettings) { mutableStateOf(storeSettings?.allowCredit ?: true) }
    var maxDiscountPercent by remember(storeSettings) { mutableStateOf(storeSettings?.maxDiscountPercent?.toString() ?: "15") }
    var receiptHeaderNote by remember(storeSettings) { mutableStateOf(storeSettings?.receiptHeaderNote ?: "Thank you for shopping!") }
    var receiptFooterNote by remember(storeSettings) { mutableStateOf(storeSettings?.receiptFooterNote ?: "Please come again!") }
    var autoPrintReceipt by remember(storeSettings) { mutableStateOf(storeSettings?.autoPrintReceipt ?: true) }

    // Inventory Rules State
    var lowStockThreshold by remember(storeSettings) { mutableStateOf(storeSettings?.lowStockThreshold?.toString() ?: "5") }
    var negativeStockBehavior by remember(storeSettings) { mutableStateOf(storeSettings?.negativeStockBehavior ?: "BLOCK") }

    // Security & Lock State
    var autoLockInterval by remember(storeSettings) { mutableStateOf(storeSettings?.autoLockInterval ?: "5 minutes") }
    var autoLogoutInterval by remember(storeSettings) { mutableStateOf(storeSettings?.autoLogoutInterval ?: "Never") }
    var biometricUnlockEnabled by remember(storeSettings) { mutableStateOf(storeSettings?.biometricEnabled ?: false) }
    var loginBiometricEnabled by remember(storeSettings) { mutableStateOf(storeSettings?.loginBiometricEnabled ?: false) }
    var enforceStrongPin by remember(storeSettings) { mutableStateOf(storeSettings?.enforceStrongPin ?: true) }

    var pendingBiometricTarget by remember { mutableStateOf<BiometricTarget?>(null) }
    var biometricPinInput by remember { mutableStateOf("") }
    var biometricPinError by remember { mutableStateOf<String?>(null) }

    var showCustomLockTimerDialog by remember { mutableStateOf(false) }
    var customLockMinutesInput by remember { mutableStateOf("") }
    var showCustomLogoutTimerDialog by remember { mutableStateOf(false) }
    var customLogoutMinutesInput by remember { mutableStateOf("") }

    // Devices & Hardware State
    val rawPrinterName = storeSettings?.printerName.orEmpty()
    val sanitizedPrinterName = remember(rawPrinterName) {
        if (rawPrinterName.contains("Epson Thermal Receipt Printer", ignoreCase = true) ||
            rawPrinterName.contains("Redmi", ignoreCase = true) ||
            rawPrinterName.contains("Buds", ignoreCase = true) ||
            rawPrinterName.contains("Galaxy", ignoreCase = true) ||
            rawPrinterName.contains("TS 12", ignoreCase = true)
        ) "" else rawPrinterName
    }
    var printerName by remember(sanitizedPrinterName) { mutableStateOf(sanitizedPrinterName) }
    var showAllBluetoothDevices by remember { mutableStateOf(false) }
    var printerConnectionType by remember(storeSettings) { mutableStateOf(storeSettings?.printerConnectionType ?: "BLUETOOTH") }
    var paperWidthMm by remember(storeSettings) { mutableStateOf(storeSettings?.paperWidthMm?.toString() ?: "80") }
    var scannerMode by remember(storeSettings) { mutableStateOf(storeSettings?.scannerMode ?: "USB HID Keyboard Emulation Mode") }
    var cashDrawerTrigger by remember(storeSettings) { mutableStateOf(storeSettings?.cashDrawerTrigger ?: true) }

    val roleColor = when (session.role) {
        StaffRole.OWNER -> PosEmerald
        StaffRole.ADMIN -> PosPrimary
        StaffRole.MANAGER -> PosSky
        StaffRole.STOREKEEPER -> PosAmber
        StaffRole.CASHIER -> Color(0xFF8B5CF6)
    }

    // Role-Aware Access Flags
    val isOwnerOrAdmin = session.role == StaffRole.OWNER || session.role == StaffRole.ADMIN
    val isManagerOrAbove = isOwnerOrAdmin || session.role == StaffRole.MANAGER
    val isStorekeeperOrAbove = isOwnerOrAdmin || session.role == StaffRole.STOREKEEPER

    fun matchesFilter(text: String): Boolean =
        searchQuery.isBlank() || text.contains(searchQuery.trim(), ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        // Top Header
        Column {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Text(
                text = strings.settingsSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(strings.searchSettingsPlaceholder, color = Color(0xFF64748B), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PosPrimary) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text(strings.clear, fontSize = 11.sp, color = PosRose, fontWeight = FontWeight.Bold)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PosPrimary,
                unfocusedBorderColor = Color(0xFFCBD5E1)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 1. SIGNED-IN PROFILE & ACCOUNT CARD
        // ==========================================
        if (matchesFilter("profile user account pin name email phone session")) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().testTag("current_operator_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(roleColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = session.staffName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = roleColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = session.staffName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(roleColor)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = session.role.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                Text(
                                    text = session.staffEmail.ifBlank { "terminal_operator@omnipos" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = PosPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PosOutlineButton(
                            text = strings.changePin,
                            icon = Icons.Default.Lock,
                            onClick = { showChangePinDialog = true },
                            modifier = Modifier.weight(1f)
                        )

                        PosOutlineButton(
                            text = strings.switchOperator,
                            icon = Icons.Default.People,
                            onClick = { showSwitchOperatorDialog = true },
                            modifier = Modifier.weight(1f)
                        )

                        PosGradientButton(
                            text = strings.lockRegister,
                            icon = Icons.Default.Lock,
                            gradient = PosButtonGradients.Dark,
                            onClick = { viewModel.authManager.lockRegister() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // GROUP: LANGUAGE & LOCALIZATION (All Users)
        // ==========================================
        if (matchesFilter("language localization nepali english भाषा स्थानियकरण")) {
            SettingsGroupHeader(title = strings.language, icon = Icons.Default.Language)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().testTag("language_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.selectLanguage,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = strings.languageSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PosPrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.NEPALI) "🇳🇵 नेपाली" else "🇬🇧 English",
                                style = MaterialTheme.typography.labelSmall,
                                color = PosPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // English Option Card
                    val isEnglish = currentLanguage == AppLanguage.ENGLISH
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEnglish) PosPrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC)
                        ),
                        border = BorderStroke(
                            width = if (isEnglish) 2.dp else 1.dp,
                            color = if (isEnglish) PosPrimary else Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setLanguage(AppLanguage.ENGLISH) }
                            .testTag("lang_option_english")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isEnglish) PosPrimary.copy(alpha = 0.2f) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🇬🇧", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = strings.english,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEnglish) PosPrimary else Slate900
                                    )
                                    Text(
                                        text = strings.switchLanguagePrompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            RadioButton(
                                selected = isEnglish,
                                onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Nepali Option Card
                    val isNepali = currentLanguage == AppLanguage.NEPALI
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNepali) PosPrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC)
                        ),
                        border = BorderStroke(
                            width = if (isNepali) 2.dp else 1.dp,
                            color = if (isNepali) PosPrimary else Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setLanguage(AppLanguage.NEPALI) }
                            .testTag("lang_option_nepali")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isNepali) PosPrimary.copy(alpha = 0.2f) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🇳🇵", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "नेपाली (Nepali)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNepali) PosPrimary else Slate900
                                    )
                                    Text(
                                        text = "बिलिङ, मौज्दात, ग्राहक र सेटिङ्सका लागि नेपाली भाषा प्रयोग गर्नुहोस्",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            RadioButton(
                                selected = isNepali,
                                onClick = { viewModel.setLanguage(AppLanguage.NEPALI) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // GROUP: BUSINESS & STORE PROFILE (Owner, Admin)
        // ==========================================
        if (isOwnerOrAdmin && matchesFilter("business store organization tax currency VAT address branch location")) {
            SettingsGroupHeader(title = strings.storeCardTitle, icon = Icons.Default.Store)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().testTag("business_info_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = org?.name ?: "OmniPOS Store",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "${strings.taxId}: ${org?.taxId ?: strings.notSet}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                        }

                        PosOutlineButton(
                            text = strings.editStoreInfo,
                            icon = Icons.Default.Edit,
                            onClick = { showEditStoreDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(strings.currencySymbol, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Text(org?.currencySymbol ?: "Rs", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Slate900)
                        }
                        Column {
                            Text(strings.taxCalculationMode, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Text(org?.taxMode?.name ?: "EXCLUSIVE", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PosPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // GROUP: FISCAL YEARS & INVOICING (Owner, Admin)
        // ==========================================
        if (isOwnerOrAdmin && matchesFilter("fiscal year invoice tax vat bill number series ird billing आर्थिक वर्ष")) {
            SettingsGroupHeader(title = strings.fiscalYearsGroup, icon = Icons.Default.CalendarMonth)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().testTag("fiscal_years_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (activeFiscalYear != null) "FY ${activeFiscalYear?.name}" else "No Active Fiscal Year",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (activeFiscalYear != null && !activeFiscalYear!!.isClosed) PosEmerald else PosAmber)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (activeFiscalYear == null) "NOT CONFIGURED" else if (activeFiscalYear!!.isClosed) "CLOSED" else "ACTIVE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            val dateSpan = if (activeFiscalYear != null) {
                                val sFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                "${sFormat.format(Date(activeFiscalYear!!.startDate))} → ${sFormat.format(Date(activeFiscalYear!!.endDate))}"
                            } else "Nepali BS cycle (Shrawan 1 - Ashadh end)"
                            Text(
                                text = dateSpan,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }

                        PosOutlineButton(
                            text = strings.manage,
                            icon = Icons.Default.CalendarMonth,
                            onClick = { showFiscalYearDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(strings.invoiceFormat, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            val prefix = activeFiscalYear?.invoicePrefix ?: "INV"
                            val fyName = activeFiscalYear?.name ?: "81/82"
                            Text(
                                text = "$prefix-$fyName-XXXXX",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PosPrimary
                            )
                        }
                        Column {
                            Text(strings.invoicesIssued, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Text(
                                text = "${activeFiscalYear?.currentInvoiceSequence ?: 0} bills",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }
                        Column {
                            Text(strings.nextInvoicePreview, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            val nextInvoiceNo = if (activeFiscalYear != null) {
                                FiscalYearHelper.formatFiscalInvoice(activeFiscalYear!!.invoicePrefix, activeFiscalYear!!.name, activeFiscalYear!!.currentInvoiceSequence + 1)
                            } else "INV-81/82-00001"
                            Text(
                                text = nextInvoiceNo,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = PosEmerald
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // GROUP: TEAM & STAFF (Owner, Admin)
        // ==========================================
        if (isOwnerOrAdmin && matchesFilter("team staff invite role permission pin cashier manager")) {
            SettingsGroupHeader(title = strings.staffCardTitle, icon = Icons.Default.People)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.staffMembersCount(realUsers.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )

                        PosCompactPillButton(
                            text = strings.addStaffMember,
                            icon = Icons.Default.PersonAdd,
                            gradient = PosButtonGradients.Primary,
                            onClick = { showAddUserDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    realUsers.forEach { staff ->
                        val isSelf = staff.id == session.staffUid || staff.email == session.staffEmail
                        val userRoleColor = when (staff.role) {
                            StaffRole.OWNER -> PosEmerald
                            StaffRole.ADMIN -> PosPrimary
                            StaffRole.MANAGER -> PosSky
                            StaffRole.STOREKEEPER -> PosAmber
                            StaffRole.CASHIER -> Color(0xFF8B5CF6)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(staff.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Slate900)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(userRoleColor)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(staff.role.name, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(staff.email, style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569), fontSize = 11.sp)
                            }

                            if (!isSelf) {
                                IconButton(onClick = { userToDelete = staff }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = PosRose, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // GROUP: SALES & CHECKOUT SETUP (Owner, Admin, Manager)
        // ==========================================
        if (isManagerOrAbove && matchesFilter("sales checkout payment cash card qr credit discount receipt auto print")) {
            SettingsGroupHeader(title = strings.salesCardTitle, icon = Icons.Default.Payments)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.acceptedPaymentMethods, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.cashPaymentMethod, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                        Switch(
                            checked = allowCash,
                            onCheckedChange = { allowCash = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PosEmerald)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.cardPaymentMethod, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                        Switch(
                            checked = allowCard,
                            onCheckedChange = { allowCard = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PosEmerald)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.qrPaymentMethod, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                        Switch(
                            checked = allowQr,
                            onCheckedChange = { allowQr = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PosEmerald)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.creditPaymentMethod, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                        Switch(
                            checked = allowCredit,
                            onCheckedChange = { allowCredit = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PosEmerald)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Text(strings.checkoutPolicies, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)

                    OutlinedTextField(
                        value = maxDiscountPercent,
                        onValueChange = { maxDiscountPercent = it },
                        label = { Text(strings.maxDiscountLimit) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = receiptHeaderNote,
                        onValueChange = { receiptHeaderNote = it },
                        label = { Text(strings.receiptHeader) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = receiptFooterNote,
                        onValueChange = { receiptFooterNote = it },
                        label = { Text(strings.receiptFooter) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.autoPrintReceiptSwitch, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                        Switch(
                            checked = autoPrintReceipt,
                            onCheckedChange = { autoPrintReceipt = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PosPrimary)
                        )
                    }

                    PosGradientButton(
                        text = strings.saveCheckoutSetup,
                        icon = Icons.Default.Save,
                        gradient = PosButtonGradients.Primary,
                        onClick = {
                            val maxDisc = maxDiscountPercent.toIntOrNull() ?: 15
                            viewModel.saveSalesAndCheckoutSettings(
                                allowCash = allowCash,
                                allowCard = allowCard,
                                allowQr = allowQr,
                                allowCredit = allowCredit,
                                maxDiscountPercent = maxDisc,
                                receiptHeaderNote = receiptHeaderNote,
                                receiptFooterNote = receiptFooterNote,
                                autoPrintReceipt = autoPrintReceipt
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // GROUP: INVENTORY RULES (Owner, Admin, Storekeeper)
        // ==========================================
        if (isStorekeeperOrAbove && matchesFilter("inventory stock low threshold negative block warn allow")) {
            SettingsGroupHeader(title = strings.inventoryCardTitle, icon = Icons.Default.Inventory)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = lowStockThreshold,
                        onValueChange = { lowStockThreshold = it },
                        label = { Text(strings.lowStockThresholdLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(strings.negativeStockPolicy, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Slate900)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(
                        "BLOCK" to strings.stockBlock,
                        "WARN" to strings.stockWarn,
                        "ALLOW" to strings.stockAllow
                    ).forEach { (option, label) ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (negativeStockBehavior == option) 1.5.dp else 1.dp,
                                        color = if (negativeStockBehavior == option) PosRose else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { negativeStockBehavior = option },
                                color = if (negativeStockBehavior == option) PosRose.copy(alpha = 0.08f) else Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = negativeStockBehavior == option,
                                        onClick = { negativeStockBehavior = option }
                                    )
                                    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    PosGradientButton(
                        text = strings.saveInventoryRules,
                        icon = Icons.Default.Save,
                        gradient = PosButtonGradients.Amber,
                        onClick = {
                            val threshold = lowStockThreshold.toIntOrNull() ?: 5
                            viewModel.saveInventoryRules(
                                lowStockThreshold = threshold,
                                negativeStockBehavior = negativeStockBehavior
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // GROUP: DEVICES & HARDWARE (Owner, Admin)
        // ==========================================
        if (isOwnerOrAdmin && matchesFilter("devices hardware printer thermal paper 58mm 80mm scanner cash drawer test")) {
            SettingsGroupHeader(title = strings.hardwareCardTitle, icon = Icons.Default.Devices)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.thermalPrinterTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                            Text(strings.printerSubtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        }

                        OutlinedButton(
                            onClick = {
                                if (viewModel.printerManager.hasBluetoothPermission()) {
                                    viewModel.scanPrinters(showAll = showAllBluetoothDevices)
                                } else {
                                    bluetoothPermissionLauncher.launch(bluetoothPermissions)
                                }
                            },
                            enabled = !isScanningPrinters,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            if (isScanningPrinters) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = PosPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.scanningPrinters, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.scanPrinters, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 1. Live Connection Status Card
                    val isPrinterConfigured = printerName.isNotBlank()
                    val isConnected = isPrinterConfigured && (printerConnectionStatus?.startsWith("Connected") == true)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isConnected) Color(0xFFF0FDF4) else if (isPrinterConfigured) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isConnected) PosEmerald.copy(alpha = 0.5f) else if (isPrinterConfigured) PosPrimary.copy(alpha = 0.4f) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isConnected) PosEmerald.copy(alpha = 0.15f)
                                                else if (isPrinterConfigured) PosPrimary.copy(alpha = 0.12f)
                                                else Color(0xFFE2E8F0)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = if (isConnected) PosEmerald else if (isPrinterConfigured) PosPrimary else Color(0xFF64748B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = when {
                                                    isConnected -> "Connected Thermal Printer"
                                                    isPrinterConfigured -> "Printer Paired (Disconnected)"
                                                    else -> "No Thermal Printer Connected"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = when {
                                                    isConnected -> PosEmerald
                                                    isPrinterConfigured -> PosPrimary
                                                    else -> Slate900
                                                }
                                            )
                                            if (isConnectingPrinter) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = PosPrimary)
                                            }
                                        }
                                        Text(
                                            text = if (isPrinterConfigured) printerName else "Pair your thermal receipt printer in Android Bluetooth settings",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (isPrinterConfigured) {
                                    OutlinedButton(
                                        onClick = {
                                            printerName = ""
                                            viewModel.disconnectPrinter()
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PosRose)
                                    ) {
                                        Text("Disconnect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = openBluetoothSettings,
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pair Device", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (isConnected) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    PosOutlineButton(
                                        text = "Test Print Receipt",
                                        icon = Icons.Default.Print,
                                        onClick = {
                                            viewModel.printTestReceipt { _, msg ->
                                                viewModel.setAlert(msg)
                                            }
                                        },
                                        height = 32.dp
                                    )
                                }
                            } else if (isPrinterConfigured) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = { viewModel.connectBluetoothPrinter(printerName) },
                                        enabled = !isConnectingPrinter,
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PosPrimary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(if (isConnectingPrinter) "Connecting..." else "Connect Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Discovered & Paired Printers Header with Filter Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showAllBluetoothDevices) "All Bluetooth Devices (${discoveredPrinters.size}):" else "Detected Thermal Printers (${discoveredPrinters.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PosPrimary
                        )

                        TextButton(
                            onClick = {
                                showAllBluetoothDevices = !showAllBluetoothDevices
                                viewModel.scanPrinters(showAll = showAllBluetoothDevices)
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (showAllBluetoothDevices) strings.showPrintersOnly else strings.showAllPairedDevices,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    if (discoveredPrinters.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            discoveredPrinters.forEach { p ->
                                val isCurrentTarget = printerName.contains(p.address, ignoreCase = true) || printerName.contains(p.name, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrentTarget) PosPrimary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isCurrentTarget) PosPrimary else Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = if (p.type == com.example.pos.domain.PrinterInterfaceType.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Usb,
                                                contentDescription = null,
                                                tint = if (isCurrentTarget) PosPrimary else Color(0xFF64748B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate900)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (p.isBonded) Color(0xFFEFF6FF) else Color(0xFFFEF3C7)
                                                    ) {
                                                        Text(
                                                            text = if (p.isBonded) "Paired" else "Nearby",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (p.isBonded) PosPrimary else PosAmber,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text("${p.type.name} • ${p.address}", fontSize = 10.sp, color = Color(0xFF64748B))
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (isCurrentTarget && isConnected) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = PosEmerald
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("Active", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.connectBluetoothPrinter(p.address, p.name)
                                                },
                                                enabled = !isConnectingPrinter,
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = if (isCurrentTarget) Color(0xFF0F172A) else PosPrimary),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(if (isCurrentTarget) "Reconnect" else "Connect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isScanningPrinters) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PosPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning for nearby Bluetooth thermal printers...", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    } else {
                        // Empty State Guide Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Print, contentDescription = null, tint = PosAmber, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("No printers found.", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate900)
                                }
                                Text(
                                    text = "Pair your printer in Bluetooth settings, then tap Scan.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = openBluetoothSettings,
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PosAmber),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Bluetooth Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.scanPrinters(showAll = showAllBluetoothDevices) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Manual Device Input & Quick Connect
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = printerName,
                            onValueChange = { printerName = it },
                            label = { Text(strings.printerAddressLabel) },
                            placeholder = { Text("e.g. MPT-II, POS-58, 00:11:22:33:44:55") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        if (printerName.isNotBlank()) {
                            PosGradientButton(
                                text = strings.btnConnect,
                                onClick = {
                                    viewModel.connectBluetoothPrinter(printerName)
                                },
                                isLoading = isConnectingPrinter,
                                enabled = !isConnectingPrinter,
                                gradient = PosButtonGradients.Primary,
                                height = 48.dp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.paperWidth, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("58", "80").forEach { width ->
                                OutlinedButton(
                                    onClick = { paperWidthMm = width },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = if (paperWidthMm == width) ButtonDefaults.outlinedButtonColors(containerColor = PosPrimary.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text("${width}mm (${if (width == "58") "32 cols" else "48 cols"})", fontWeight = FontWeight.Bold, color = if (paperWidthMm == width) PosPrimary else Color(0xFF475569), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Print on Checkout", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                            Text("Print receipt automatically when sale completes", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = autoPrintReceipt,
                            onCheckedChange = { autoPrintReceipt = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PosEmerald)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Cash Drawer Auto-Open Pulse", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E293B))
                            Text("Kick RJ11 drawer pin during checkout", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = cashDrawerTrigger,
                            onCheckedChange = { cashDrawerTrigger = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PosEmerald)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Text(strings.barcodeScannerSetup, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)

                    OutlinedTextField(
                        value = scannerMode,
                        onValueChange = { scannerMode = it },
                        label = { Text(strings.barcodeScannerMode) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PosOutlineButton(
                            text = strings.testPrint,
                            icon = Icons.Default.Print,
                            onClick = {
                                viewModel.testPrinterHardware { msg ->
                                    viewModel.setAlert(msg)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        PosOutlineButton(
                            text = strings.kickDrawer,
                            icon = Icons.Default.Payments,
                            onClick = {
                                viewModel.kickCashDrawer { msg ->
                                    viewModel.setAlert(msg)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        PosGradientButton(
                            text = strings.save,
                            icon = Icons.Default.Save,
                            gradient = PosButtonGradients.Primary,
                            onClick = {
                                val width = paperWidthMm.toIntOrNull() ?: 80
                                viewModel.saveHardwareSettings(
                                    printerName = printerName,
                                    printerConnectionType = printerConnectionType,
                                    paperWidthMm = width,
                                    scannerMode = scannerMode,
                                    cashDrawerTrigger = cashDrawerTrigger
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // GROUP: SECURITY & TERMINAL LOCK
        // ==========================================
        if (matchesFilter("security pin biometrics lock timer strong pin timeout logout")) {
            SettingsGroupHeader(title = strings.securityCardTitle, icon = Icons.Default.Security)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // 1. TERMINAL AUTO-LOCK TIMER
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = PosPrimary, modifier = Modifier.size(16.dp))
                                    Text(strings.autoLockTimerTitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Slate900)
                                }
                                Text(strings.autoLockTimerDesc, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PosPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = autoLockInterval,
                                    fontWeight = FontWeight.Bold,
                                    color = PosPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Presets Row + Custom Timer
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            items(SecurityTimerUtils.AUTO_LOCK_PRESETS) { preset ->
                                val isSelected = autoLockInterval.equals(preset, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PosPrimary else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) PosPrimary else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { autoLockInterval = preset }
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Custom Timer Option Chip
                            item {
                                val isCustom = !SecurityTimerUtils.AUTO_LOCK_PRESETS.any { it.equals(autoLockInterval, ignoreCase = true) }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCustom) PosPrimary else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isCustom) PosPrimary else Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            customLockMinutesInput = autoLockInterval.replace(Regex("""[^0-9]"""), "")
                                            showCustomLockTimerDialog = true
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Custom",
                                            tint = if (isCustom) Color.White else Color(0xFF334155),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isCustom) strings.customTimerWithInterval(autoLockInterval) else strings.customTimer,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCustom) Color.White else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // 2. BIOMETRIC TERMINAL UNLOCK
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PosEmerald.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(strings.biometricUnlockTitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Slate900)
                                Text(strings.biometricUnlockDesc, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                            }
                        }
                        Switch(
                            checked = biometricUnlockEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    pendingBiometricTarget = BiometricTarget.TERMINAL_UNLOCK
                                    biometricPinInput = ""
                                    biometricPinError = null
                                } else {
                                    biometricUnlockEnabled = false
                                    viewModel.saveSecuritySettings(
                                        autoLockInterval = autoLockInterval,
                                        biometricEnabled = false,
                                        enforceStrongPin = enforceStrongPin,
                                        autoLogoutInterval = autoLogoutInterval,
                                        loginBiometricEnabled = loginBiometricEnabled
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PosEmerald)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // 3. BIOMETRIC QUICK LOGIN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PosPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = PosPrimary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(strings.biometricLoginTitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Slate900)
                                Text(strings.biometricLoginDesc, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                            }
                        }
                        Switch(
                            checked = loginBiometricEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    pendingBiometricTarget = BiometricTarget.LOGIN_AUTH
                                    biometricPinInput = ""
                                    biometricPinError = null
                                } else {
                                    loginBiometricEnabled = false
                                    viewModel.saveSecuritySettings(
                                        autoLockInterval = autoLockInterval,
                                        biometricEnabled = biometricUnlockEnabled,
                                        enforceStrongPin = enforceStrongPin,
                                        autoLogoutInterval = autoLogoutInterval,
                                        loginBiometricEnabled = false
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PosPrimary)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // 4. LOGIN SESSION TIMEOUT / AUTO-LOGOUT TIMER
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                    Text(strings.autoLogoutTimerTitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Slate900)
                                }
                                Text(strings.autoLogoutTimerDesc, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF64748B).copy(alpha = 0.1f),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = autoLogoutInterval,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Logout Presets Row + Custom Timer
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            items(SecurityTimerUtils.AUTO_LOGOUT_PRESETS) { preset ->
                                val isSelected = autoLogoutInterval.equals(preset, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Slate900 else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) Slate900 else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { autoLogoutInterval = preset }
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Custom Logout Timer Option Chip
                            item {
                                val isCustom = !SecurityTimerUtils.AUTO_LOGOUT_PRESETS.any { it.equals(autoLogoutInterval, ignoreCase = true) }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCustom) Slate900 else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isCustom) Slate900 else Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            customLogoutMinutesInput = autoLogoutInterval.replace(Regex("""[^0-9]"""), "")
                                            showCustomLogoutTimerDialog = true
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Custom",
                                            tint = if (isCustom) Color.White else Color(0xFF334155),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isCustom) strings.customTimerWithInterval(autoLogoutInterval) else strings.customTimer,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCustom) Color.White else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // 5. ENFORCE STRONG PINS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.enforceStrongPinTitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Slate900)
                            Text(strings.enforceStrongPinDesc, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                        }
                        Switch(
                            checked = enforceStrongPin,
                            onCheckedChange = { enforceStrongPin = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PosPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    PosGradientButton(
                        text = strings.saveSecuritySettings,
                        icon = Icons.Default.Save,
                        gradient = PosButtonGradients.Dark,
                        onClick = {
                            viewModel.saveSecuritySettings(
                                autoLockInterval = autoLockInterval,
                                biometricEnabled = biometricUnlockEnabled,
                                enforceStrongPin = enforceStrongPin,
                                autoLogoutInterval = autoLogoutInterval,
                                loginBiometricEnabled = loginBiometricEnabled
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Custom Lock Timer Dialog
        if (showCustomLockTimerDialog) {
            AlertDialog(
                onDismissRequest = { showCustomLockTimerDialog = false },
                title = { Text("Custom Auto-Lock Timer", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter minutes of inactivity before terminal locks:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = customLockMinutesInput,
                            onValueChange = { customLockMinutesInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Minutes (e.g. 3, 7, 12)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val min = customLockMinutesInput.toIntOrNull()
                            if (min != null && min > 0) {
                                autoLockInterval = if (min == 1) "1 minute" else "$min minutes"
                            }
                            showCustomLockTimerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PosPrimary)
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomLockTimerDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Custom Logout Timer Dialog
        if (showCustomLogoutTimerDialog) {
            AlertDialog(
                onDismissRequest = { showCustomLogoutTimerDialog = false },
                title = { Text("Custom Login Session Timeout", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter minutes of inactivity before automatic logout:", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = customLogoutMinutesInput,
                            onValueChange = { customLogoutMinutesInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Minutes (e.g. 45, 90, 120)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val min = customLogoutMinutesInput.toIntOrNull()
                            if (min != null && min > 0) {
                                autoLogoutInterval = if (min >= 60 && min % 60 == 0) {
                                    val hrs = min / 60
                                    if (hrs == 1) "1 hour" else "$hrs hours"
                                } else {
                                    "$min minutes"
                                }
                            }
                            showCustomLogoutTimerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomLogoutTimerDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Biometric Activation PIN Verification Dialog
        if (pendingBiometricTarget != null) {
            val target = pendingBiometricTarget!!
            val targetLabel = if (target == BiometricTarget.TERMINAL_UNLOCK) "Biometric Terminal Unlock" else "Biometric Login Authentication"

            AlertDialog(
                onDismissRequest = {
                    pendingBiometricTarget = null
                    biometricPinInput = ""
                    biometricPinError = null
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PosEmerald.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Verify PIN to Activate Biometric", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "To enable $targetLabel, verify your identity by entering your current security PIN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )

                        OutlinedTextField(
                            value = biometricPinInput,
                            onValueChange = {
                                biometricPinInput = it
                                biometricPinError = null
                            },
                            label = { Text("Current Security PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("biometric_verify_pin_input")
                        )

                        if (biometricPinError != null) {
                            Text(
                                text = biometricPinError!!,
                                color = PosRose,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    PosGradientButton(
                        text = "Verify & Activate",
                        icon = Icons.Default.Check,
                        gradient = PosButtonGradients.Emerald,
                        onClick = {
                            if (biometricPinInput.isBlank()) {
                                biometricPinError = "Please enter your current PIN"
                                return@PosGradientButton
                            }

                            val isPinValid = viewModel.verifyCurrentPin(biometricPinInput)
                            if (!isPinValid) {
                                biometricPinError = "Incorrect PIN. Biometric is not active."
                            } else {
                                val isAvailable = BiometricHelper.isBiometricAvailable(context)
                                if (!isAvailable) {
                                    biometricPinError = "No biometric sensor or enrolled fingerprint found on this device."
                                } else {
                                    val newUnlock = if (target == BiometricTarget.TERMINAL_UNLOCK) true else biometricUnlockEnabled
                                    val newLogin = if (target == BiometricTarget.LOGIN_AUTH) true else loginBiometricEnabled

                                    biometricUnlockEnabled = newUnlock
                                    loginBiometricEnabled = newLogin

                                    viewModel.saveSecuritySettings(
                                        autoLockInterval = autoLockInterval,
                                        biometricEnabled = newUnlock,
                                        enforceStrongPin = enforceStrongPin,
                                        autoLogoutInterval = autoLogoutInterval,
                                        loginBiometricEnabled = newLogin
                                    )
                                    viewModel.recordAuditLog(
                                        "BIOMETRIC_ACTIVATED",
                                        "Activated $targetLabel by operator ${session.staffName}"
                                    )
                                    viewModel.setAlert("$targetLabel activated successfully!")
                                    pendingBiometricTarget = null
                                    biometricPinInput = ""
                                    biometricPinError = null
                                }
                            }
                        }
                    )
                },
                dismissButton = {
                    PosOutlineButton(
                        text = "Cancel",
                        onClick = {
                            pendingBiometricTarget = null
                            biometricPinInput = ""
                            biometricPinError = null
                        }
                    )
                }
            )
        }

        // ==========================================
        // GROUP: AUDIT TRAIL & SYSTEM LOGS
        // ==========================================
        if (isOwnerOrAdmin && matchesFilter("data audit trail activity log history security events")) {
            SettingsGroupHeader(title = strings.auditLogsGroupTitle, icon = Icons.Default.Storage)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.auditTrailTitle(auditLogs.size), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Slate900)
                            Text(strings.auditTrailDesc, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                        }

                        PosCompactPillButton(
                            text = strings.viewAuditLogs,
                            icon = Icons.Default.History,
                            gradient = PosButtonGradients.Primary,
                            onClick = { showAuditLogsDialog = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // GROUP: DATA BACKUP & RESTORE (GMAIL)
        // ==========================================
        if (isOwnerOrAdmin && matchesFilter("data backup restore export gmail cloud sync google drive json")) {
            SettingsGroupHeader(title = strings.backupCardTitle, icon = Icons.Default.CloudSync)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.fullBackupTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = strings.fullBackupDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }

                    // Linked Gmail Account Box
                    val linkedEmail = storeSettings?.backupEmail.orEmpty()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (linkedEmail.isNotBlank()) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                        border = BorderStroke(
                            1.dp,
                            if (linkedEmail.isNotBlank()) PosEmerald.copy(alpha = 0.4f) else PosAmber.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (linkedEmail.isNotBlank()) PosEmerald.copy(alpha = 0.15f) else PosAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = if (linkedEmail.isNotBlank()) PosEmerald else PosAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = strings.linkedBackupGmail,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (linkedEmail.isNotBlank()) PosEmerald else PosAmber
                                        ) {
                                            Text(
                                                text = if (linkedEmail.isNotBlank()) strings.statusActive else strings.notConfigured,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = linkedEmail.ifBlank { strings.noBackupEmailLinked },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (linkedEmail.isNotBlank()) Slate900 else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            PosCompactPillButton(
                                text = if (linkedEmail.isNotBlank()) strings.changeGmail else strings.linkGmail,
                                icon = Icons.Default.Email,
                                gradient = if (linkedEmail.isNotBlank()) PosButtonGradients.Dark else PosButtonGradients.Primary,
                                onClick = { showLinkGmailDialog = true }
                            )
                        }
                    }

                    // Metadata & Quick Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val lastBackup = storeSettings?.lastBackupAt
                            val lastBackupFormatted = if (lastBackup != null && lastBackup > 0L) {
                                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastBackup))
                            } else strings.notSet

                            Text(
                                text = "${strings.lastBackupAt}: $lastBackupFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PosPrimary.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = "${products.size} Products",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PosPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PosEmerald.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = strings.staffMembersCount(customers.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PosEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Status Message Feedback
                    backupStatusMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (backupStatusIsError) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, if (backupStatusIsError) PosRose else PosEmerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (backupStatusIsError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (backupStatusIsError) PosRose else PosEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (backupStatusIsError) PosRose else Color(0xFF166534),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                IconButton(
                                    onClick = { backupStatusMessage = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Text("✕", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PosGradientButton(
                            text = strings.sendBackupToGmail,
                            icon = Icons.Default.Email,
                            gradient = PosButtonGradients.Primary,
                            onClick = {
                                viewModel.exportAndShareBackup(context) { file ->
                                    backupStatusMessage = "Backup ready: ${file.name} (${file.length() / 1024} KB). Dispatched to Gmail."
                                    backupStatusIsError = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        PosOutlineButton(
                            text = strings.restoreBackupJson,
                            icon = Icons.Default.Storage,
                            onClick = {
                                filePickerLauncher.launch("application/json")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ==========================================
        // GROUP: CLOUD FIRESTORE DATABASE SYNCHRONIZATION
        // ==========================================
        if (isOwnerOrAdmin && matchesFilter("data cloud sync firestore database collections fields 19 schema table firebase")) {
            SettingsGroupHeader(title = strings.firestoreSyncGroupTitle, icon = Icons.Default.CloudSync)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.firestoreCollectionsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            val statusBg = when (syncState.status) {
                                com.example.pos.data.repository.SyncConnectionStatus.ONLINE -> PosEmerald.copy(alpha = 0.15f)
                                com.example.pos.data.repository.SyncConnectionStatus.SYNCING -> PosPrimary.copy(alpha = 0.15f)
                                com.example.pos.data.repository.SyncConnectionStatus.ERROR -> PosRose.copy(alpha = 0.15f)
                                com.example.pos.data.repository.SyncConnectionStatus.OFFLINE -> Color(0xFFF1F5F9)
                            }
                            val statusColor = when (syncState.status) {
                                com.example.pos.data.repository.SyncConnectionStatus.ONLINE -> PosEmerald
                                com.example.pos.data.repository.SyncConnectionStatus.SYNCING -> PosPrimary
                                com.example.pos.data.repository.SyncConnectionStatus.ERROR -> PosRose
                                com.example.pos.data.repository.SyncConnectionStatus.OFFLINE -> Color(0xFF64748B)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = statusBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Text(
                                        text = syncState.status.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }
                        }

                        Text(
                            text = strings.firestoreSyncDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }

                    // Organization ID and Sync Status detail
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.activeStoreIdLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                Text(org?.id ?: "N/A", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate900)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.syncStatusMessageLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                Text(syncState.message, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = PosPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.pendingOutboxLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                Text(strings.pendingRecordsCount(syncState.pendingCount), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (syncState.pendingCount > 0) PosAmber else PosEmerald)
                            }
                        }
                    }

                    // Collections summary tags
                    Text(strings.collectionsAutoSynced, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Slate900)
                    val collectionsList = listOf(
                        "customers (Khata & Due)" to PosEmerald,
                        "customer_ledger" to PosEmerald,
                        "products" to PosPrimary,
                        "categories" to PosPrimary,
                        "sales" to PosSky,
                        "sale_items" to PosSky,
                        "sale_payments" to PosSky,
                        "item_barcodes" to Color(0xFF8B5CF6),
                        "stock_movements" to Color(0xFF8B5CF6),
                        "shifts" to PosAmber,
                        "cash_movements" to PosAmber,
                        "organizations" to Slate900,
                        "branches" to Slate900,
                        "registers" to Slate900,
                        "staff_members" to Slate900,
                        "users" to Slate900,
                        "store_settings" to Color(0xFF64748B),
                        "audit_logs" to Color(0xFF64748B),
                        "held_orders" to Color(0xFF64748B)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            collectionsList.chunked(3).forEach { chunk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    chunk.forEach { (name, color) ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = color.copy(alpha = 0.08f),
                                            border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = color,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    repeat(3 - chunk.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PosGradientButton(
                            text = strings.syncAllCollections,
                            icon = Icons.Default.CloudSync,
                            gradient = PosButtonGradients.Emerald,
                            onClick = {
                                viewModel.syncAllCollectionsToFirestore { success, msg ->
                                    backupStatusMessage = msg
                                    backupStatusIsError = !success
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        PosOutlineButton(
                            text = strings.fullCloudSync,
                            icon = Icons.Default.Refresh,
                            onClick = {
                                viewModel.triggerCloudSync { success, msg ->
                                    backupStatusMessage = msg
                                    backupStatusIsError = !success
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }


        // ==========================================
        // FOOTER: PERSISTENT LOG OUT
        // ==========================================
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PosDestructiveButton(
                text = "Log Out from POS",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("persistent_logout_button")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "OmniPOS Core Engine v1.0.4 (Build 42)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF475569),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // =========================================================================
    // DIALOGS
    // =========================================================================

    // 1. Edit User Profile Dialog
    if (showEditProfileDialog) {
        var profileName by remember { mutableStateOf(session.staffName) }
        var profileEmail by remember { mutableStateOf(session.staffEmail) }
        var profilePhone by remember { mutableStateOf("") }
        var profileError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PosPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit User Profile", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it; profileError = null },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = profileEmail,
                        onValueChange = { profileEmail = it; profileError = null },
                        label = { Text("Email Address") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = profilePhone,
                        onValueChange = { profilePhone = it; profileError = null },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (profileError != null) {
                        Text(profileError!!, color = PosRose, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                PosGradientButton(
                    text = "Save Changes",
                    icon = Icons.Default.Save,
                    gradient = PosButtonGradients.Primary,
                    onClick = {
                        if (profileName.isBlank()) {
                            profileError = "Name cannot be empty"
                        } else {
                            viewModel.updateUserProfile(
                                userId = session.staffUid,
                                fullName = profileName,
                                email = profileEmail,
                                phone = profilePhone,
                                onSuccess = { showEditProfileDialog = false },
                                onError = { profileError = it }
                            )
                        }
                    }
                )
            },
            dismissButton = {
                PosOutlineButton(
                    text = "Cancel",
                    onClick = { showEditProfileDialog = false }
                )
            }
        )
    }

    // 2. Change Security PIN Dialog (No Weak Default PIN Enforced)
    if (showChangePinDialog) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = PosPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Security PIN", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose a unique 4+ digit security PIN. Default PINs like '1234' or '0000' are disallowed.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))

                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { currentPin = it; pinError = null },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it; pinError = null },
                        label = { Text("New PIN (min 4 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it; pinError = null },
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError != null) {
                        Text(pinError!!, color = PosRose, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                PosGradientButton(
                    text = "Update PIN",
                    icon = Icons.Default.Lock,
                    gradient = PosButtonGradients.Primary,
                    onClick = {
                        if (newPin != confirmPin) {
                            pinError = "New PIN and Confirm PIN do not match"
                        } else {
                            viewModel.updateUserPin(
                                userId = session.staffUid,
                                currentPin = currentPin,
                                newPin = newPin,
                                onSuccess = { showChangePinDialog = false },
                                onError = { pinError = it }
                            )
                        }
                    }
                )
            },
            dismissButton = {
                PosOutlineButton(
                    text = "Cancel",
                    onClick = { showChangePinDialog = false }
                )
            }
        )
    }

    // 3. System Audit Trail Log Dialog
    if (showAuditLogsDialog) {
        var logSearch by remember { mutableStateOf("") }
        val filteredLogs = remember(auditLogs, logSearch) {
            if (logSearch.isBlank()) auditLogs
            else auditLogs.filter { log ->
                log.action.contains(logSearch, ignoreCase = true) ||
                        log.details.contains(logSearch, ignoreCase = true) ||
                        log.actorName.contains(logSearch, ignoreCase = true)
            }
        }
        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showAuditLogsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = PosPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Security & Audit Logs", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.height(380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = logSearch,
                        onValueChange = { logSearch = it },
                        placeholder = { Text("Filter logs by action, actor...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (filteredLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No audit log records found.", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredLogs) { log ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(PosPrimary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(log.action, style = MaterialTheme.typography.labelSmall, color = PosPrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                            }
                                            Text(dateFormat.format(Date(log.createdAt)), style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 10.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(log.details, style = MaterialTheme.typography.bodySmall, color = Slate900, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("By ${log.actorName} (${log.actorRole.name})", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                PosOutlineButton(
                    text = "Close",
                    onClick = { showAuditLogsDialog = false }
                )
            }
        )
    }

    // 4. Switch Operator Dialog
    if (showSwitchOperatorDialog) {
        var selectedStaff by remember { mutableStateOf(realUsers.firstOrNull()) }
        var enteredPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSwitchOperatorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, tint = PosPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch Till Operator", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select incoming staff member and enter their 4-digit PIN:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))

                    realUsers.forEach { staff ->
                        val isSelected = selectedStaff?.id == staff.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PosPrimary else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedStaff = staff
                                    error = null
                                },
                            color = if (isSelected) PosPrimary.copy(alpha = 0.08f) else Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(staff.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(staff.role.name, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = PosPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it
                            error = null
                        },
                        label = { Text("Operator PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (error != null) {
                        Text(error!!, color = PosRose, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                PosGradientButton(
                    text = "Verify & Switch",
                    icon = Icons.Default.People,
                    gradient = PosButtonGradients.Primary,
                    onClick = {
                        val target = selectedStaff
                        if (target != null && enteredPin.isNotBlank()) {
                            viewModel.verifyAndSwitchOperator(
                                targetUser = target,
                                enteredPin = enteredPin,
                                onSuccess = { showSwitchOperatorDialog = false },
                                onError = { error = it }
                            )
                        } else {
                            error = "Please select a staff member and enter PIN"
                        }
                    }
                )
            },
            dismissButton = {
                PosOutlineButton(
                    text = "Cancel",
                    onClick = { showSwitchOperatorDialog = false }
                )
            }
        )
    }

    // 5. Edit Store Dialog
    if (showEditStoreDialog) {
        var storeName by remember { mutableStateOf(org?.name ?: "") }
        var taxId by remember { mutableStateOf(org?.taxId ?: "") }
        var currency by remember { mutableStateOf(org?.currencySymbol ?: "Rs") }
        var taxMode by remember { mutableStateOf(org?.taxMode ?: TaxMode.EXCLUSIVE) }

        AlertDialog(
            onDismissRequest = { showEditStoreDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, contentDescription = null, tint = PosPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Store Profile", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Store Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = taxId,
                        onValueChange = { taxId = it },
                        label = { Text("Tax Registration / VAT ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("Currency Symbol") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Tax Calculation Mode", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaxMode.entries.forEach { mode ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (taxMode == mode) 1.5.dp else 1.dp,
                                        color = if (taxMode == mode) PosPrimary else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { taxMode = mode },
                                color = if (taxMode == mode) PosPrimary.copy(alpha = 0.08f) else Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = taxMode == mode, onClick = { taxMode = mode })
                                    Text(mode.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                PosGradientButton(
                    text = "Save Store Settings",
                    icon = Icons.Default.Store,
                    gradient = PosButtonGradients.Primary,
                    onClick = {
                        viewModel.updateStoreSettings(storeName, taxId, currency, taxMode)
                        showEditStoreDialog = false
                    }
                )
            },
            dismissButton = {
                PosOutlineButton(
                    text = "Cancel",
                    onClick = { showEditStoreDialog = false }
                )
            }
        )
    }

    // 6. Add Staff User Dialog
    if (showAddUserDialog) {
        StaffFormDialog(
            existingUsers = realUsers,
            onDismiss = { showAddUserDialog = false },
            onSaveStaff = { fullName, email, phone, role, pin ->
                viewModel.addStaffUser(fullName, email, phone, role, pin)
                showAddUserDialog = false
            }
        )
    }

    // 7. Delete Staff Confirmation Dialog
    userToDelete?.let { staff ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Remove Staff Member?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove ${staff.fullName} (${staff.email})? They will lose access immediately.") },
            confirmButton = {
                PosDestructiveButton(
                    text = "Remove Staff",
                    icon = Icons.Default.Delete,
                    onClick = {
                        viewModel.removeStaffUser(staff.id)
                        userToDelete = null
                    }
                )
            },
            dismissButton = {
                PosOutlineButton(
                    text = "Cancel",
                    onClick = { userToDelete = null }
                )
            }
        )
    }

    // 8. Link Gmail Account Dialog
    if (showLinkGmailDialog) {
        var emailInput by remember(storeSettings, user) {
            mutableStateOf(storeSettings?.backupEmail?.ifBlank { user?.email.orEmpty() } ?: user?.email.orEmpty())
        }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showLinkGmailDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = PosPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link Gmail for Cloud Backups", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the Gmail address where your encrypted store database backups will be sent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; errorMsg = null },
                        label = { Text("Gmail Address") },
                        placeholder = { Text("store.backups@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val authEmail = user?.email
                    if (!authEmail.isNullOrBlank() && !authEmail.equals(emailInput, ignoreCase = true)) {
                        OutlinedButton(
                            onClick = {
                                emailInput = authEmail
                                errorMsg = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use Signed-in Google Account ($authEmail)", fontSize = 11.sp)
                        }
                    }

                    if (errorMsg != null) {
                        Text(errorMsg!!, color = PosRose, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                PosGradientButton(
                    text = "Save & Link",
                    icon = Icons.Default.Email,
                    gradient = PosButtonGradients.Primary,
                    onClick = {
                        val clean = emailInput.trim().lowercase()
                        if (clean.isBlank() || !clean.contains("@") || !clean.contains(".")) {
                            errorMsg = "Please enter a valid email address (e.g., yourname@gmail.com)"
                        } else {
                            viewModel.linkBackupEmail(clean) {
                                showLinkGmailDialog = false
                                backupStatusMessage = "Linked Gmail: $clean"
                                backupStatusIsError = false
                            }
                        }
                    }
                )
            },
            dismissButton = {
                PosOutlineButton(
                    text = "Cancel",
                    onClick = { showLinkGmailDialog = false }
                )
            }
        )
    }

    // 9. Confirm Restore Database Dialog
    pendingRestoreParsed?.let { parsed ->
        AlertDialog(
            onDismissRequest = {
                if (!isRestoring) {
                    pendingRestoreParsed = null
                    pendingRestoreJson = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = PosAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Database from Backup?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val exportDateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(parsed.exportedAt))
                    Text(
                        "You are about to restore data into store '${org?.name}'. This will merge and update records with the contents of this backup file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Backup Metadata:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("• Source Store: ${parsed.orgName}", style = MaterialTheme.typography.bodySmall)
                            Text("• Exported At: $exportDateStr", style = MaterialTheme.typography.bodySmall)
                            Text("• Products: ${parsed.counts.productsCount} items (${parsed.counts.barcodesCount} barcodes)", style = MaterialTheme.typography.bodySmall)
                            Text("• Categories: ${parsed.counts.categoriesCount}", style = MaterialTheme.typography.bodySmall)
                            Text("• Customers: ${parsed.counts.customersCount} accounts (${parsed.counts.customerLedgerCount} ledger entries)", style = MaterialTheme.typography.bodySmall)
                            Text("• Sales History: ${parsed.counts.salesCount} sales (${parsed.counts.saleItemsCount} items, ${parsed.counts.paymentsCount} payments)", style = MaterialTheme.typography.bodySmall)
                            Text("• Operations: ${parsed.counts.shiftsCount} shifts, ${parsed.counts.cashMovementsCount} cash logs, ${parsed.counts.stockMovementsCount} stock records", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Text(
                        "Total records to import: ${parsed.counts.totalRecords}. Proceed?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = PosPrimary
                    )
                }
            },
            confirmButton = {
                PosGradientButton(
                    text = if (isRestoring) "Restoring..." else "Confirm & Restore",
                    icon = Icons.Default.Storage,
                    gradient = PosButtonGradients.Emerald,
                    isLoading = isRestoring,
                    enabled = !isRestoring,
                    onClick = {
                        val raw = pendingRestoreJson
                        if (raw != null) {
                            isRestoring = true
                            viewModel.restoreDatabaseFromBackup(
                                jsonString = raw,
                                onSuccess = { counts ->
                                    isRestoring = false
                                    pendingRestoreParsed = null
                                    pendingRestoreJson = null
                                    backupStatusMessage = "Successfully restored ${counts.totalRecords} records (${counts.productsCount} products, ${counts.customersCount} customers)!"
                                    backupStatusIsError = false
                                },
                                onError = { err ->
                                    isRestoring = false
                                    backupStatusMessage = err
                                    backupStatusIsError = true
                                }
                            )
                        }
                    }
                )
            },
            dismissButton = {
                if (!isRestoring) {
                    PosOutlineButton(
                        text = "Cancel",
                        onClick = {
                            pendingRestoreParsed = null
                            pendingRestoreJson = null
                        }
                    )
                }
            }
        )
    }

    // Fiscal Year Management Dialog
    if (showFiscalYearDialog) {
        var showAddForm by remember { mutableStateOf(false) }
        var newFyName by remember { mutableStateOf("") }
        var newFyPrefix by remember { mutableStateOf("INV") }
        var addError by remember { mutableStateOf<String?>(null) }
        val sFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

        Dialog(
            onDismissRequest = { showFiscalYearDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFAFAFA),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(max = 520.dp)
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header Row with Title and Dismiss
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PosPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = PosPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = strings.fiscalYearManagement,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = strings.activePeriodSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = { showFiscalYearDialog = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showAddForm) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.createNewFiscalYear,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Slate900
                                        )
                                        TextButton(
                                            onClick = { showAddForm = false; addError = null },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = strings.cancel,
                                                color = Color(0xFF64748B),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Text(
                                        text = strings.fiscalYearInstruction,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )

                                    OutlinedTextField(
                                        value = newFyName,
                                        onValueChange = { newFyName = it; addError = null },
                                        label = { Text(strings.fiscalYearNameWithExample) },
                                        placeholder = { Text("e.g. 2084/85") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = newFyPrefix,
                                        onValueChange = { newFyPrefix = it; addError = null },
                                        label = { Text("Invoice Prefix (e.g. INV)") },
                                        placeholder = { Text("e.g. INV") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    if (newFyName.isNotBlank()) {
                                        val sampleInvoice = FiscalYearHelper.formatFiscalInvoice(newFyPrefix.ifBlank { "INV" }, newFyName, 1)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFECFDF5),
                                            border = BorderStroke(1.dp, PosEmerald.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = PosEmerald,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "First Invoice Preview:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF065F46),
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = sampleInvoice,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = PosEmerald
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (addError != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PosRose.copy(alpha = 0.1f),
                                            border = BorderStroke(1.dp, PosRose.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = addError!!,
                                                color = PosRose,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PosOutlineButton(
                                            text = strings.backToList,
                                            onClick = { showAddForm = false; addError = null },
                                            modifier = Modifier.weight(1f)
                                        )
                                        PosGradientButton(
                                            text = strings.saveFiscalYear,
                                            icon = Icons.Default.Save,
                                            gradient = PosButtonGradients.Emerald,
                                            onClick = {
                                                if (newFyName.isBlank()) {
                                                    addError = "Fiscal year name is required"
                                                } else {
                                                    val orgId = org?.id ?: "default_org"
                                                    val nowMs = System.currentTimeMillis()
                                                    val oneYearMs = 365L * 24 * 60 * 60 * 1000L
                                                    val cleanName = newFyName.trim()
                                                    val fyCode = cleanName.takeLast(5).ifBlank { cleanName }
                                                    val newEntity = FiscalYearEntity(
                                                        id = "fy_${cleanName.replace("/", "_")}",
                                                        orgId = orgId,
                                                        name = cleanName,
                                                        code = fyCode,
                                                        startDate = nowMs,
                                                        endDate = nowMs + oneYearMs,
                                                        isCurrent = false,
                                                        isClosed = false,
                                                        invoicePrefix = newFyPrefix.trim().ifBlank { "INV" },
                                                        currentInvoiceSequence = 0L,
                                                        createdAt = nowMs
                                                    )
                                                    viewModel.saveFiscalYear(newEntity)
                                                    newFyName = ""
                                                    showAddForm = false
                                                }
                                            },
                                            modifier = Modifier.weight(1.4f)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Fiscal Year List Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.configuredFiscalYears(fiscalYears.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.weight(1f)
                                )
                                PosCompactPillButton(
                                    text = strings.addFiscalYearShort,
                                    icon = Icons.Default.Add,
                                    gradient = PosButtonGradients.Primary,
                                    onClick = { showAddForm = true; addError = null }
                                )
                            }

                            fiscalYears.forEach { fy ->
                                val isCurrent = fy.isCurrent
                                val isClosed = fy.isClosed
                                val nextInv = FiscalYearHelper.formatFiscalInvoice(fy.invoicePrefix, fy.name, fy.currentInvoiceSequence + 1)

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = if (isCurrent) PosEmerald else if (isClosed) Color(0xFFE2E8F0) else Color(0xFFCBD5E1)
                                    ),
                                    shadowElevation = if (isCurrent) 2.dp else 0.5.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        // Header Row of the Card
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "FY ${fy.name}",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = Slate900
                                                )
                                                when {
                                                    isCurrent -> {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFECFDF5),
                                                            border = BorderStroke(1.dp, PosEmerald.copy(alpha = 0.3f))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(6.dp)
                                                                        .clip(CircleShape)
                                                                        .background(PosEmerald)
                                                                )
                                                                Text(
                                                                    text = "ACTIVE",
                                                                    color = PosEmerald,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                    isClosed -> {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color(0xFFF1F5F9),
                                                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                                        ) {
                                                            Text(
                                                                text = "CLOSED",
                                                                color = Color(0xFF64748B),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Action Buttons for this Fiscal Year
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (!isCurrent && !isClosed) {
                                                    PosCompactPillButton(
                                                        text = "Set Active",
                                                        icon = Icons.Default.Check,
                                                        gradient = PosButtonGradients.Emerald,
                                                        onClick = { viewModel.switchActiveFiscalYear(fy.id) }
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { viewModel.toggleCloseFiscalYear(fy) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isClosed) Icons.Default.LockOpen else Icons.Default.Lock,
                                                        contentDescription = if (isClosed) "Reopen" else "Close",
                                                        tint = if (isClosed) PosEmerald else Color(0xFF94A3B8),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Dates & Bills Count Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${sFormat.format(Date(fy.startDate))} → ${sFormat.format(Date(fy.endDate))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B),
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "${fy.currentInvoiceSequence} bills issued",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Full-Width Invoicing Sequence Banner (Prevents any text wrapping)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCurrent) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isCurrent) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Series:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF64748B),
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = "${fy.invoicePrefix}-${fy.name}-XXXXX",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF334155),
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "Next:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isCurrent) Color(0xFF047857) else Color(0xFF64748B),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = nextInv,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = if (isCurrent) PosEmerald else Color(0xFF334155),
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showFiscalYearDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF1F5F9),
                            contentColor = Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = strings.close,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = PosPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Slate900
        )
    }
}
