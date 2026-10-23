package com.example.pos.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import com.example.pos.ui.components.PosButtonGradients
import com.example.pos.ui.components.PosGradientButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.pos.data.model.StaffRole
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pos.domain.CartCalculator
import com.example.pos.ui.components.CashierLockDialog
import com.example.pos.ui.components.CheckoutDialog
import com.example.pos.ui.components.ParkedOrdersDialog
import com.example.pos.ui.components.PosTopBar
import androidx.compose.runtime.CompositionLocalProvider
import com.example.pos.ui.components.ReceiptDialog
import com.example.pos.ui.components.RoleSwitcherDialog
import com.example.pos.ui.components.ShiftDialog
import com.example.pos.ui.i18n.AppLanguage
import com.example.pos.ui.i18n.EnglishStrings
import com.example.pos.ui.i18n.LocalAppLanguage
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import com.example.pos.ui.screens.AuthScreen
import com.example.pos.ui.screens.CustomerLedgerScreen
import com.example.pos.ui.screens.InventoryScreen
import com.example.pos.ui.screens.PosRegisterScreen
import com.example.pos.ui.screens.ReportsScreen
import com.example.pos.ui.screens.SettingsScreen
import com.example.pos.ui.screens.SplashScreen
import com.example.ui.theme.PosRose

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.example.pos.util.SecurityTimerUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun OmniPosApp(viewModel: PosViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val org by viewModel.organization.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val liveShiftMetrics by viewModel.liveShiftMetrics.collectAsState()
    val staffSession by viewModel.currentSession.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val heldOrders by viewModel.heldOrders.collectAsState()
    val registeredUsers by viewModel.users.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val strings = if (currentLanguage == AppLanguage.NEPALI) NepaliStrings else EnglishStrings
    val snackbarHostState = remember { SnackbarHostState() }

    // Dynamic Inactivity Monitor: Enforces terminal auto-lock and auto-logout timers
    LaunchedEffect(staffSession.isAuthenticated, staffSession.isLocked, storeSettings) {
        if (!staffSession.isAuthenticated) return@LaunchedEffect

        val lockMillis = SecurityTimerUtils.parseIntervalToMillis(storeSettings?.autoLockInterval ?: "5 minutes")
        val logoutMillis = SecurityTimerUtils.parseIntervalToMillis(storeSettings?.autoLogoutInterval ?: "Never")

        while (isActive) {
            delay(1000L)
            val idleTime = System.currentTimeMillis() - lastInteractionTime

            // 1. Auto-Lock terminal on inactivity
            if (lockMillis > 0L && idleTime >= lockMillis && !staffSession.isLocked) {
                viewModel.authManager.lockRegister()
            }

            // 2. Auto-Logout session on prolonged inactivity
            if (logoutMillis > 0L && idleTime >= logoutMillis) {
                viewModel.logout()
                break
            }
        }
    }

    CompositionLocalProvider(
        LocalAppLanguage provides currentLanguage,
        LocalPosStrings provides strings
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    }
                }
        ) {
            LaunchedEffect(uiState.alertMessage) {
                uiState.alertMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearAlert()
                }
            }

        // Top-level Navigation Gate: SPLASH -> AUTH (Login/Register) -> MAIN_POS
        AnimatedContent(
            targetState = uiState.rootScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "root_navigation",
        ) { rootScreen ->
            when (rootScreen) {
                AppRootScreen.SPLASH -> {
                    SplashScreen(
                        viewModel = viewModel,
                        onSplashFinished = { hasExistingUser ->
                            viewModel.onSplashFinished(hasExistingUser)
                        }
                    )
                }
                AppRootScreen.AUTH -> {
                    AuthScreen(viewModel = viewModel)
                }
                AppRootScreen.MAIN_POS -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isTabletLandscape = this.maxWidth >= 840.dp

                        if (isTabletLandscape) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.testTag("pos_bottom_navigation")
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (staffSession.role.canAccess(PosNavDestination.REGISTER)) {
                                        NavigationRailItem(
                                            selected = uiState.currentNav == PosNavDestination.REGISTER,
                                            onClick = { viewModel.navigateTo(PosNavDestination.REGISTER) },
                                            icon = { Icon(Icons.Default.PointOfSale, contentDescription = strings.navRegister) },
                                            label = { Text(strings.navRegister) },
                                            modifier = Modifier.testTag("nav_register")
                                        )
                                    }
                                    if (staffSession.role.canAccess(PosNavDestination.INVENTORY)) {
                                        NavigationRailItem(
                                            selected = uiState.currentNav == PosNavDestination.INVENTORY,
                                            onClick = { viewModel.navigateTo(PosNavDestination.INVENTORY) },
                                            icon = { Icon(Icons.Default.Inventory2, contentDescription = strings.navInventory) },
                                            label = { Text(strings.navInventory) },
                                            modifier = Modifier.testTag("nav_inventory")
                                        )
                                    }
                                    if (staffSession.role.canAccess(PosNavDestination.CUSTOMERS)) {
                                        NavigationRailItem(
                                            selected = uiState.currentNav == PosNavDestination.CUSTOMERS,
                                            onClick = { viewModel.navigateTo(PosNavDestination.CUSTOMERS) },
                                            icon = { Icon(Icons.Default.People, contentDescription = strings.navCustomers) },
                                            label = { Text(strings.navCustomers) },
                                            modifier = Modifier.testTag("nav_customers")
                                        )
                                    }
                                    if (staffSession.role.canAccess(PosNavDestination.REPORTS)) {
                                        NavigationRailItem(
                                            selected = uiState.currentNav == PosNavDestination.REPORTS,
                                            onClick = { viewModel.navigateTo(PosNavDestination.REPORTS) },
                                            icon = { Icon(Icons.Default.Assessment, contentDescription = strings.navReports) },
                                            label = { Text(strings.navReports) },
                                            modifier = Modifier.testTag("nav_reports")
                                        )
                                    }
                                    if (staffSession.role.canAccess(PosNavDestination.SETTINGS)) {
                                        NavigationRailItem(
                                            selected = uiState.currentNav == PosNavDestination.SETTINGS,
                                            onClick = { viewModel.navigateTo(PosNavDestination.SETTINGS) },
                                            icon = { Icon(Icons.Default.Settings, contentDescription = strings.navSettings) },
                                            label = { Text(strings.navSettings) },
                                            modifier = Modifier.testTag("nav_settings")
                                        )
                                    }
                                }

                                Scaffold(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    topBar = {
                                        PosTopBar(
                                            org = org,
                                            shift = activeShift,
                                            staffSession = staffSession,
                                            syncState = syncState,
                                            currentLanguage = currentLanguage,
                                            onLanguageToggle = { viewModel.toggleLanguage() },
                                            onShiftClick = { viewModel.openShiftDialog() },
                                            onSyncClick = { viewModel.triggerManualSync() },
                                            onLockClick = { viewModel.authManager.lockRegister() },
                                            onSwitchRoleClick = { viewModel.openRoleSwitchDialog() },
                                            onLogoutClick = { viewModel.logout() }
                                        )
                                    },
                                    snackbarHost = { SnackbarHost(snackbarHostState) },
                                    modifier = Modifier.weight(1f)
                                ) { innerPadding ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                    ) {
                                        if (staffSession.role.canAccess(uiState.currentNav)) {
                                            when (uiState.currentNav) {
                                                PosNavDestination.REGISTER -> PosRegisterScreen(viewModel)
                                                PosNavDestination.INVENTORY -> InventoryScreen(viewModel)
                                                PosNavDestination.CUSTOMERS -> CustomerLedgerScreen(viewModel)
                                                PosNavDestination.REPORTS -> ReportsScreen(viewModel)
                                                PosNavDestination.SETTINGS -> SettingsScreen(viewModel)
                                            }
                                        } else {
                                            RoleAccessDeniedScreen(
                                                currentRoleName = staffSession.role.name,
                                                destinationName = uiState.currentNav.name,
                                                onReturnToAllowed = {
                                                    val fallback = staffSession.role.allowedDestinations().firstOrNull() ?: PosNavDestination.REGISTER
                                                    viewModel.navigateTo(fallback)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Scaffold(
                                containerColor = MaterialTheme.colorScheme.background,
                                topBar = {
                                    PosTopBar(
                                        org = org,
                                        shift = activeShift,
                                        staffSession = staffSession,
                                        syncState = syncState,
                                        currentLanguage = currentLanguage,
                                        onLanguageToggle = { viewModel.toggleLanguage() },
                                        onShiftClick = { viewModel.openShiftDialog() },
                                        onSyncClick = { viewModel.triggerManualSync() },
                                        onLockClick = { viewModel.authManager.lockRegister() },
                                        onSwitchRoleClick = { viewModel.openRoleSwitchDialog() },
                                        onLogoutClick = { viewModel.logout() }
                                    )
                                },
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.testTag("pos_bottom_navigation")
                                    ) {
                                        if (staffSession.role.canAccess(PosNavDestination.REGISTER)) {
                                            NavigationBarItem(
                                                selected = uiState.currentNav == PosNavDestination.REGISTER,
                                                onClick = { viewModel.navigateTo(PosNavDestination.REGISTER) },
                                                icon = { Icon(Icons.Default.PointOfSale, contentDescription = strings.navRegister) },
                                                label = { Text(strings.navRegister) },
                                                modifier = Modifier.testTag("nav_register")
                                            )
                                        }
                                        if (staffSession.role.canAccess(PosNavDestination.INVENTORY)) {
                                            NavigationBarItem(
                                                selected = uiState.currentNav == PosNavDestination.INVENTORY,
                                                onClick = { viewModel.navigateTo(PosNavDestination.INVENTORY) },
                                                icon = { Icon(Icons.Default.Inventory2, contentDescription = strings.navInventory) },
                                                label = { Text(strings.navInventory) },
                                                modifier = Modifier.testTag("nav_inventory")
                                            )
                                        }
                                        if (staffSession.role.canAccess(PosNavDestination.CUSTOMERS)) {
                                            NavigationBarItem(
                                                selected = uiState.currentNav == PosNavDestination.CUSTOMERS,
                                                onClick = { viewModel.navigateTo(PosNavDestination.CUSTOMERS) },
                                                icon = { Icon(Icons.Default.People, contentDescription = strings.navCustomers) },
                                                label = { Text(strings.navCustomers) },
                                                modifier = Modifier.testTag("nav_customers")
                                            )
                                        }
                                        if (staffSession.role.canAccess(PosNavDestination.REPORTS)) {
                                            NavigationBarItem(
                                                selected = uiState.currentNav == PosNavDestination.REPORTS,
                                                onClick = { viewModel.navigateTo(PosNavDestination.REPORTS) },
                                                icon = { Icon(Icons.Default.Assessment, contentDescription = strings.navReports) },
                                                label = { Text(strings.navReports) },
                                                modifier = Modifier.testTag("nav_reports")
                                            )
                                        }
                                        if (staffSession.role.canAccess(PosNavDestination.SETTINGS)) {
                                            NavigationBarItem(
                                                selected = uiState.currentNav == PosNavDestination.SETTINGS,
                                                onClick = { viewModel.navigateTo(PosNavDestination.SETTINGS) },
                                                icon = { Icon(Icons.Default.Settings, contentDescription = strings.navSettings) },
                                                label = { Text(strings.navSettings) },
                                                modifier = Modifier.testTag("nav_settings")
                                            )
                                        }
                                    }
                                },
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                modifier = Modifier.fillMaxSize()
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    if (staffSession.role.canAccess(uiState.currentNav)) {
                                        when (uiState.currentNav) {
                                            PosNavDestination.REGISTER -> PosRegisterScreen(viewModel)
                                            PosNavDestination.INVENTORY -> InventoryScreen(viewModel)
                                            PosNavDestination.CUSTOMERS -> CustomerLedgerScreen(viewModel)
                                            PosNavDestination.REPORTS -> ReportsScreen(viewModel)
                                            PosNavDestination.SETTINGS -> SettingsScreen(viewModel)
                                        }
                                    } else {
                                        RoleAccessDeniedScreen(
                                            currentRoleName = staffSession.role.name,
                                            destinationName = uiState.currentNav.name,
                                            onReturnToAllowed = {
                                                val fallback = staffSession.role.allowedDestinations().firstOrNull() ?: PosNavDestination.REGISTER
                                                viewModel.navigateTo(fallback)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    // Role Switcher Modal (Authenticated Handover)
    if (uiState.isRoleSwitchDialogOpen) {
        RoleSwitcherDialog(
            currentRole = staffSession.role,
            users = registeredUsers,
            onVerifyAndSwitch = { targetUser, pin, onSuccess, onError ->
                viewModel.verifyAndSwitchOperator(targetUser, pin, onSuccess, onError)
            },
            onDismiss = { viewModel.closeRoleSwitchDialog() }
        )
    }

    // Checkout Modal
    if (uiState.isCheckoutDialogOpen) {
        val currency = org?.currencySymbol ?: "$"
        val taxMode = org?.taxMode ?: com.example.pos.data.model.TaxMode.EXCLUSIVE
        val summary = remember(uiState.cartItems, uiState.orderDiscountPercent, taxMode) {
            CartCalculator.calculateSummary(uiState.cartItems, uiState.orderDiscountPercent, taxMode)
        }

        val allCustomers by viewModel.customers.collectAsState()

        CheckoutDialog(
            grandTotalInCents = summary.grandTotalInCents,
            currencySymbol = currency,
            selectedCustomer = uiState.selectedCustomer,
            customers = allCustomers,
            onDismiss = { viewModel.closeCheckoutDialog() },
            onCompletePayment = { payments -> viewModel.executeSale(payments) },
            onSelectCustomer = { cust -> viewModel.selectCustomer(cust) }
        )
    }

    // Receipt Modal
    if (uiState.isReceiptDialogOpen) {
        ReceiptDialog(
            receiptText = uiState.activeReceiptText,
            onDismiss = { viewModel.closeReceiptDialog() },
            onPrint = { viewModel.printActiveReceipt() },
            isPrinting = uiState.isPrintingReceipt
        )
    }

    // Shift Management Modal
    if (uiState.isShiftDialogOpen) {
        val currency = org?.currencySymbol ?: "Rs"
        val expectedFloat = viewModel.expectedOpeningFloat.collectAsState().value
        val isCashier = staffSession.role == StaffRole.CASHIER

        // Role Gate: Only Cashier is prompted to open a new shift. Non-cashiers bypass opening flow.
        if (activeShift == null && !isCashier) {
            LaunchedEffect(Unit) {
                viewModel.closeShiftDialog()
            }
        } else {
            ShiftDialog(
                activeShift = activeShift,
                liveMetrics = liveShiftMetrics,
                expectedOpeningFloat = expectedFloat,
                currencySymbol = currency,
                isStale = liveShiftMetrics?.isStale ?: false,
                onDismiss = { viewModel.closeShiftDialog() },
            onStartShift = { floatCents -> viewModel.startShift(floatCents) },
            onStartShiftAdvanced = { floatCents, expFloat, gapReason, denoms ->
                viewModel.startShift(floatCents, expFloat, gapReason, denoms)
            },
            onEndShift = { countedCash, countedOnline, onlineCount, notes ->
                viewModel.endShift(countedCash, countedOnline, onlineCount, notes)
            },
            onEndShiftAdvanced = { draft ->
                viewModel.endShiftAdvanced(
                    countedCashInCents = draft.countedCashInCents,
                    denominationCounts = draft.denominationCounts,
                    onlineDeclarations = draft.onlineDeclarations,
                    creditNotes = draft.creditNotes,
                    closingFloatInCents = draft.closingFloatInCents,
                    depositAmountInCents = draft.depositAmountInCents,
                    varianceReason = draft.varianceReason,
                    closingNotes = draft.closingNotes
                )
            },
            onSaveCloseDraft = { draft ->
                viewModel.saveCloseDraft(draft)
            },
            onAddCashMovement = { type, amt, reason -> viewModel.addDrawerCashMovement(type, amt, reason) }
        )
        }
    }

    // Parked Orders Modal
    if (uiState.isParkedOrdersDialogOpen) {
        val currency = org?.currencySymbol ?: "$"
        ParkedOrdersDialog(
            heldOrders = heldOrders,
            currencySymbol = currency,
            onResumeOrder = { viewModel.resumeParkedOrder(it) },
            onDeleteOrder = { viewModel.deleteParkedOrder(it.id) },
            onDismiss = { viewModel.closeParkedOrdersDialog() }
        )
    }

        // Cashier Lock Screen
        if (staffSession.isLocked) {
            CashierLockDialog(
                staffName = staffSession.staffName,
                biometricEnabled = storeSettings?.biometricEnabled ?: false,
                onUnlock = { pin -> viewModel.verifyUnlockPin(pin) },
                onBiometricUnlock = { viewModel.unlockWithBiometric() }
            )
        }
        }
    }
}

@Composable
private fun RoleAccessDeniedScreen(
    currentRoleName: String,
    destinationName: String,
    onReturnToAllowed: () -> Unit
) {
    val strings = LocalPosStrings.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 500.dp)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PosRose.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = strings.accessDenied,
                    tint = PosRose,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = strings.restrictedScreen(destinationName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.roleNotAuthorized(currentRoleName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            PosGradientButton(
                text = strings.returnToAuthorizedArea,
                gradient = PosButtonGradients.Primary,
                onClick = onReturnToAllowed
            )
        }
    }
}
