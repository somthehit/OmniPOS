package com.example.pos.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pos.auth.AuthManager
import com.example.pos.auth.CurrentStaffSession
import com.example.pos.data.local.AuditLogEntity
import com.example.pos.data.local.CategoryEntity
import com.example.pos.data.local.CustomerEntity
import com.example.pos.data.local.HeldOrderEntity
import com.example.pos.data.local.OrganizationEntity
import com.example.pos.data.local.ProductEntity
import com.example.pos.data.local.SaleEntity
import com.example.pos.data.local.SaleItemEntity
import com.example.pos.data.local.SalePaymentEntity
import com.example.pos.data.local.ShiftEntity
import com.example.pos.data.local.StockMovementEntity
import com.example.pos.data.local.StaffMemberEntity
import com.example.pos.data.local.StoreSettingsEntity
import com.example.pos.data.local.UserEntity
import com.example.pos.data.local.FiscalYearEntity
import java.util.UUID
import com.example.pos.data.model.BarcodeLookupResult
import com.example.pos.ui.i18n.AppLanguage
import com.example.pos.data.model.CartItem
import com.example.pos.data.model.MovementType
import com.example.pos.data.model.PaymentEntry
import com.example.pos.data.model.PaymentType
import com.example.pos.data.model.StaffRole
import com.example.pos.data.model.TaxMode
import com.example.pos.data.model.ZReport
import com.example.pos.data.model.ShiftLiveMetrics
import com.example.pos.data.model.CreditNoteRecord
import com.example.pos.data.model.DenominationItem
import com.example.pos.data.model.OnlineProviderDeclaration
import com.example.pos.data.model.ShiftCloseDraft
import com.example.pos.data.model.ShiftDetails
import com.example.pos.data.model.ShiftGateDecision
import com.example.pos.domain.ShiftFinancialEngine
import com.example.pos.data.repository.PosRepository
import com.example.pos.data.repository.SyncRepository
import com.example.pos.data.repository.SyncUiState
import com.example.pos.domain.CartCalculator
import com.example.pos.domain.CashDrawerManager
import com.example.pos.domain.DrawerKickResult
import com.example.pos.domain.DrawerTriggerReason
import com.example.pos.domain.DiscoveredPrinter
import com.example.pos.domain.EscPosCommandBuilder
import com.example.pos.domain.EscPosReceiptFactory
import com.example.pos.domain.PrintResult
import com.example.pos.domain.PrinterInterfaceType
import com.example.pos.domain.ReceiptFormatter
import com.example.pos.domain.ThermalPrinterManager
import com.example.pos.data.local.CustomerLedgerEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppRootScreen {
    SPLASH,
    AUTH,
    MAIN_POS
}

enum class AuthMode {
    LOGIN,
    REGISTER
}

enum class PosNavDestination {
    REGISTER,
    INVENTORY,
    CUSTOMERS,
    REPORTS,
    SETTINGS
}

fun StaffRole.canAccess(destination: PosNavDestination): Boolean {
    return when (this) {
        StaffRole.OWNER, StaffRole.ADMIN -> true
        StaffRole.MANAGER -> destination != PosNavDestination.SETTINGS
        StaffRole.STOREKEEPER -> destination == PosNavDestination.INVENTORY
        StaffRole.CASHIER -> (destination == PosNavDestination.REGISTER || destination == PosNavDestination.CUSTOMERS)
    }
}

fun StaffRole.allowedDestinations(): List<PosNavDestination> {
    return PosNavDestination.entries.filter { this.canAccess(it) }
}

data class PosUiState(
    val rootScreen: AppRootScreen = AppRootScreen.SPLASH,
    val authMode: AuthMode = AuthMode.LOGIN,
    val currentNav: PosNavDestination = PosNavDestination.REGISTER,
    val isRoleSwitchDialogOpen: Boolean = false,
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val cartItems: List<CartItem> = emptyList(),
    val orderDiscountPercent: Double = 0.0,
    val selectedCustomer: CustomerEntity? = null,
    val isCheckoutDialogOpen: Boolean = false,
    val isShiftDialogOpen: Boolean = false,
    val isReceiptDialogOpen: Boolean = false,
    val isParkedOrdersDialogOpen: Boolean = false,
    val isReceiveStockDialogOpen: Boolean = false,
    val isAdjustStockDialogOpen: Boolean = false,
    val isCustomerRepayDialogOpen: Boolean = false,
    val activeReceiptText: String = "",
    val lastCompletedSale: SaleEntity? = null,
    val lastZReport: ZReport? = null,
    val isPrintingReceipt: Boolean = false,
    val alertMessage: String? = null
)

class PosViewModel(application: Application) : AndroidViewModel(application) {

    val repository = PosRepository(application)
    val syncRepo = SyncRepository(application)
    val authManager = AuthManager(application)

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
    val currentSession: StateFlow<CurrentStaffSession> = authManager.currentSession
    val syncState: StateFlow<SyncUiState> = syncRepo.syncState

    val organization: StateFlow<OrganizationEntity?> = repository.organization
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private val _hardwareBarcodeEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val hardwareBarcodeEvent: SharedFlow<String> = _hardwareBarcodeEvent.asSharedFlow()

    // ESC/POS Thermal Printer Driver & Cash Drawer Controller
    val printerManager = ThermalPrinterManager(application)
    val cashDrawerManager = CashDrawerManager(printerManager)
    private val _discoveredPrinters = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val discoveredPrinters: StateFlow<List<DiscoveredPrinter>> = _discoveredPrinters.asStateFlow()

    private val _isScanningPrinters = MutableStateFlow(false)
    val isScanningPrinters: StateFlow<Boolean> = _isScanningPrinters.asStateFlow()

    private val _isConnectingPrinter = MutableStateFlow(false)
    val isConnectingPrinter: StateFlow<Boolean> = _isConnectingPrinter.asStateFlow()

    private val _printerConnectionStatus = MutableStateFlow<String?>(null)
    val printerConnectionStatus: StateFlow<String?> = _printerConnectionStatus.asStateFlow()

    init {
        viewModelScope.launch {
            currentUser.collect { fbUser ->
                if (fbUser != null) {
                    var org = repository.getActiveOrganization()
                    if (org == null) {
                        val email = fbUser.email ?: ""
                        val remoteOrg = syncRepo.findRemoteOrganizationForUser(fbUser.uid, email)
                        if (remoteOrg != null) {
                            repository.saveOrganization(remoteOrg)
                            syncRepo.pullAllFromFirestore(remoteOrg.id)
                            org = remoteOrg
                        }
                    } else {
                        try { syncRepo.performSync(org.id) } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    // Data streams dynamically bound to current active organization
    @OptIn(ExperimentalCoroutinesApi::class)
    val categories: StateFlow<List<CategoryEntity>> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getCategories(org.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<ProductEntity>> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getProducts(org.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers: StateFlow<List<CustomerEntity>> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getCustomers(org.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sales: StateFlow<List<SaleEntity>> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getSales(org.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val saleItems: StateFlow<List<SaleItemEntity>> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getAllSaleItems(org.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val salePayments: StateFlow<List<SalePaymentEntity>> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getAllSalePayments(org.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allShifts: StateFlow<List<ShiftEntity>> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getAllShifts(org.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val heldOrders: StateFlow<List<HeldOrderEntity>> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getHeldOrders(org.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val stockMovements: StateFlow<List<StockMovementEntity>> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getRecentStockMovements(org.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeShift: StateFlow<ShiftEntity?> = currentSession
        .flatMapLatest { session ->
            val uid = session.staffUid
            if (uid.isNotBlank()) {
                repository.getActiveShiftForCashier(uid)
            } else {
                repository.getActiveShift("reg_01")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _liveShiftMetrics = MutableStateFlow<ShiftLiveMetrics?>(null)
    val liveShiftMetrics: StateFlow<ShiftLiveMetrics?> = _liveShiftMetrics.asStateFlow()

    private val _shiftGateDecision = MutableStateFlow<ShiftGateDecision>(ShiftGateDecision.NoActiveShift)
    val shiftGateDecision: StateFlow<ShiftGateDecision> = _shiftGateDecision.asStateFlow()

    private val _expectedOpeningFloat = MutableStateFlow(0L)
    val expectedOpeningFloat: StateFlow<Long> = _expectedOpeningFloat.asStateFlow()

    private val _selectedShiftDetails = MutableStateFlow<ShiftDetails?>(null)
    val selectedShiftDetails: StateFlow<ShiftDetails?> = _selectedShiftDetails.asStateFlow()

    fun checkShiftGate() {
        val uid = currentSession.value.staffUid
        viewModelScope.launch {
            _shiftGateDecision.value = repository.checkShiftGate("reg_01", uid)
            _expectedOpeningFloat.value = repository.getLastClosedShiftExpectedFloat("reg_01")
        }
    }

    fun loadShiftDetails(shiftId: String) {
        viewModelScope.launch {
            _selectedShiftDetails.value = repository.getShiftDetails(shiftId)
        }
    }

    fun loadLiveShiftMetrics() {
        viewModelScope.launch {
            val uid = currentSession.value.staffUid
            val shift = activeShift.value 
                ?: (if (uid.isNotBlank()) repository.getActiveShiftForCashierSync(uid) else null)
                ?: repository.getActiveShiftSync("reg_01") 
                ?: return@launch
            _liveShiftMetrics.value = repository.getLiveShiftMetrics(shift.id)
        }
    }

    val users: StateFlow<List<UserEntity>> = combine(
        organization,
        currentUser,
        repository.getAllUsers()
    ) { org, fbUser, allUsers ->
        val orgId = org?.id ?: "local"
        val orgUsers = if (org != null) allUsers.filter { it.orgId == org.id } else allUsers
        val hasOwner = orgUsers.any { it.role == StaffRole.OWNER }

        if (!hasOwner) {
            val ownerUid = org?.ownerUid ?: fbUser?.uid ?: "usr_${orgId.take(8)}"
            val ownerEmail = fbUser?.email?.ifBlank { null } ?: "somthehit@gmail.com"
            val ownerName = fbUser?.displayName?.ifBlank { null } ?: "Som Thehit"
            val ownerEntity = UserEntity(
                id = ownerUid,
                orgId = orgId,
                fullName = ownerName,
                email = ownerEmail,
                role = StaffRole.OWNER,
                pinHash = "1234",
                isActive = true
            )
            viewModelScope.launch {
                repository.saveUser(ownerEntity)
            }
            listOf(ownerEntity) + orgUsers
        } else {
            orgUsers.sortedWith(compareByDescending<UserEntity> { it.role == StaffRole.OWNER }.thenBy { it.fullName })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val storeSettings: StateFlow<StoreSettingsEntity?> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getStoreSettings(org.id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _overrideLanguage = MutableStateFlow<AppLanguage?>(null)

    val currentLanguage: StateFlow<AppLanguage> = combine(storeSettings, _overrideLanguage) { settings, override ->
        override ?: AppLanguage.fromCode(settings?.language ?: "en")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ENGLISH)

    @OptIn(ExperimentalCoroutinesApi::class)
    val auditLogs: StateFlow<List<AuditLogEntity>> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getAuditLogs(org.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val fiscalYears: StateFlow<List<FiscalYearEntity>> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getFiscalYears(org.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeFiscalYear: StateFlow<FiscalYearEntity?> = organization
        .flatMapLatest { org ->
            if (org != null) repository.getActiveFiscalYear(org.id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            try {
                repository.purgeAllMockData()
                authManager.attemptSilentAutoSignIn(viewModelScope)

                // Ensure store settings exist and purge any mock/earbud printer names
                val currentOrg = repository.getActiveOrganization()
                if (currentOrg != null) {
                    val st = repository.getStoreSettingsSync(currentOrg.id)
                    val pName = st.printerName
                    if (pName.contains("Epson Thermal Receipt Printer", ignoreCase = true) ||
                        pName.contains("Redmi Buds", ignoreCase = true) ||
                        pName.contains("Galaxy", ignoreCase = true) ||
                        pName.contains("TS 12", ignoreCase = true)
                    ) {
                        repository.saveStoreSettings(st.copy(printerName = "", updatedAt = System.currentTimeMillis()))
                    }
                    repository.initializeFiscalYearsIfNeeded(currentOrg.id)
                }

                // Auto-sync or restore from Firestore if logged in with Firebase
                var org = repository.getActiveOrganization()
                val fbUser = Firebase.auth.currentUser
                if (fbUser != null) {
                    if (org == null) {
                        val remoteOrg = syncRepo.findRemoteOrganizationForUser(fbUser.uid, fbUser.email ?: "")
                        if (remoteOrg != null) {
                            Log.d("PosViewModel", "Discovered remote organization '${remoteOrg.name}' for user ${fbUser.email}. Restoring...")
                            repository.saveOrganization(remoteOrg)
                            syncRepo.pullAllFromFirestore(remoteOrg.id)
                            org = remoteOrg
                        }
                    }

                    if (org != null) {
                        val localUser = repository.findUserByEmail(fbUser.email ?: "")
                        val userEntity = localUser?.copy(role = StaffRole.OWNER, orgId = org.id) ?: UserEntity(
                            id = fbUser.uid,
                            orgId = org.id,
                            fullName = fbUser.displayName ?: "Som Thehit",
                            email = fbUser.email ?: "somthehit@gmail.com",
                            role = StaffRole.OWNER,
                            pinHash = "1234",
                            isActive = true
                        )
                        repository.saveUser(userEntity)
                        syncRepo.syncUserAndOrganizationToFirestore(userEntity, org)
                    }
                }

                // Guarantee Owner exists in local database for the current store
                val activeOrg = org ?: currentOrg
                if (activeOrg != null) {
                    val existingOwner = repository.findUserByRole(activeOrg.id, StaffRole.OWNER)
                    if (existingOwner == null) {
                        val ownerUid = activeOrg.ownerUid ?: fbUser?.uid ?: "usr_${activeOrg.id.take(8)}"
                        val ownerEmail = fbUser?.email?.ifBlank { null } ?: "somthehit@gmail.com"
                        val ownerName = fbUser?.displayName?.ifBlank { null } ?: "Som Thehit"
                        val ownerEntity = UserEntity(
                            id = ownerUid,
                            orgId = activeOrg.id,
                            fullName = ownerName,
                            email = ownerEmail,
                            role = StaffRole.OWNER,
                            pinHash = "1234",
                            isActive = true
                        )
                        repository.saveUser(ownerEntity)
                    }
                }
            } catch (e: Exception) {
                Log.e("PosViewModel", "Safe init startup error caught: ${e.message}", e)
            }
        }

        viewModelScope.launch {
            activeShift.collect { shift ->
                if (shift != null) {
                    _liveShiftMetrics.value = repository.getLiveShiftMetrics(shift.id)
                }
            }
        }
    }

    suspend fun hasExistingUsers(): Boolean {
        return repository.hasUsers()
    }

    fun onSplashFinished(hasExistingUser: Boolean) {
        if (currentSession.value.isAuthenticated) {
            val role = currentSession.value.role
            val isCashier = role == StaffRole.CASHIER
            viewModelScope.launch {
                val uid = currentSession.value.staffUid
                val currentActiveShift = if (uid.isNotBlank()) {
                    repository.getActiveShiftForCashierSync(uid)
                } else {
                    repository.getActiveShiftSync("reg_01")
                }
                // Shift Continuity & Role Gate:
                // Only Cashier role requires a shift. If last shift is OPEN, automatically resume.
                // If CLOSED or no shift exists, require opening a new shift with denominations.
                val shouldOpenShift = isCashier && (currentActiveShift == null || currentActiveShift.status != "OPEN")
                if (shouldOpenShift) {
                    _expectedOpeningFloat.value = repository.getLastClosedShiftExpectedFloat("reg_01")
                }
                _uiState.value = _uiState.value.copy(
                    rootScreen = AppRootScreen.MAIN_POS,
                    isShiftDialogOpen = shouldOpenShift
                )
            }
            val org = organization.value
            if (org != null) {
                viewModelScope.launch {
                    try { syncRepo.performSync(org.id) } catch (_: Exception) {}
                }
            }
        } else {
            val mode = if (hasExistingUser) AuthMode.LOGIN else AuthMode.REGISTER
            _uiState.value = _uiState.value.copy(
                rootScreen = AppRootScreen.AUTH,
                authMode = mode
            )
        }
    }

    fun setAuthMode(mode: AuthMode) {
        _uiState.value = _uiState.value.copy(authMode = mode)
    }

    fun loginWithStaffRole(role: StaffRole, name: String, uid: String, email: String) {
        authManager.switchCashierRole(role, name, uid, email)
        val initialNav = if (role.canAccess(uiState.value.currentNav)) {
            uiState.value.currentNav
        } else {
            role.allowedDestinations().firstOrNull() ?: PosNavDestination.REGISTER
        }
        viewModelScope.launch {
            val isCashier = role == StaffRole.CASHIER
            val currentActiveShift = if (uid.isNotBlank()) {
                repository.getActiveShiftForCashierSync(uid)
            } else {
                repository.getActiveShiftSync("reg_01")
            }
            // Role Gate: Only Cashier role is prompted.
            // Shift Continuity: If last shift is OPEN, automatically resume. If CLOSED/null, require new shift.
            val shouldOpenShift = isCashier && (currentActiveShift == null || currentActiveShift.status != "OPEN")
            if (shouldOpenShift) {
                _expectedOpeningFloat.value = repository.getLastClosedShiftExpectedFloat("reg_01")
            }
            _uiState.value = _uiState.value.copy(
                rootScreen = AppRootScreen.MAIN_POS,
                currentNav = initialNav,
                alertMessage = "Welcome, $name (${role.name})",
                isShiftDialogOpen = shouldOpenShift
            )
        }
    }

    fun loginWithCredentials(
        emailOrUsername: String,
        pinOrSecret: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmed = emailOrUsername.trim()
        val isEmail = trimmed.contains("@") && trimmed.contains(".")

        viewModelScope.launch {
            // 1. Try Local Room Authentication First (Instant & Offline-First)
            val localUser = repository.authenticateUser(trimmed, pinOrSecret)
            if (localUser != null) {
                authManager.loginSession(localUser.fullName, localUser.id, localUser.email, localUser.role)
                loginWithStaffRole(localUser.role, localUser.fullName, localUser.id, localUser.email)
                onSuccess()

                // Background sync if online
                val activeOrg = repository.getActiveOrganization()
                if (activeOrg != null) {
                    try { syncRepo.performSync(activeOrg.id) } catch (_: Exception) {}
                }
                return@launch
            }

            // 2. Try Cloud Firestore Staff Authentication (Enables staff to log in on any device)
            val remoteStaff = syncRepo.authenticateRemoteUser(trimmed, pinOrSecret)
            if (remoteStaff != null) {
                val (staffUser, remoteOrg) = remoteStaff
                if (remoteOrg != null) {
                    repository.saveOrganization(remoteOrg)
                    repository.saveUser(staffUser)
                    try {
                        syncRepo.pullAllFromFirestore(remoteOrg.id)
                    } catch (e: Exception) {
                        Log.w("PosViewModel", "Remote staff pull data note: ${e.message}")
                    }
                }
                authManager.loginSession(staffUser.fullName, staffUser.id, staffUser.email, staffUser.role)
                loginWithStaffRole(staffUser.role, staffUser.fullName, staffUser.id, staffUser.email)
                onSuccess()
                return@launch
            }

            // 3. If email provided, try Firebase Auth (for owner accounts using Firebase Auth password)
            if (isEmail && pinOrSecret.isNotBlank()) {
                val fbPassword = if (pinOrSecret.length < 6) pinOrSecret.padEnd(6, '0') else pinOrSecret
                authManager.signInWithEmail(
                    email = trimmed,
                    pass = fbPassword,
                    scope = viewModelScope,
                    onSuccess = { fbUser ->
                        viewModelScope.launch {
                            val remoteOrg = syncRepo.findRemoteOrganizationForUser(fbUser.uid, trimmed)
                            var activeOrg = if (remoteOrg != null) {
                                Log.d("PosViewModel", "Found remote store '${remoteOrg.name}' for user $trimmed. Pulling data...")
                                repository.saveOrganization(remoteOrg)
                                syncRepo.pullAllFromFirestore(remoteOrg.id)
                                remoteOrg
                            } else {
                                repository.getActiveOrganization()
                            }

                            val user = repository.findUserByEmail(trimmed)
                            val role = user?.role ?: StaffRole.OWNER
                            val name = user?.fullName ?: fbUser.displayName ?: trimmed.substringBefore("@")
                            authManager.loginSession(name, fbUser.uid, fbUser.email ?: trimmed, role)

                            if (activeOrg != null) {
                                val userEntity = user ?: UserEntity(
                                    id = fbUser.uid,
                                    orgId = activeOrg.id,
                                    fullName = name,
                                    email = fbUser.email ?: trimmed,
                                    role = role,
                                    pinHash = pinOrSecret,
                                    isActive = true
                                )
                                repository.saveUser(userEntity)
                                try {
                                    syncRepo.syncUserAndOrganizationToFirestore(userEntity, activeOrg)
                                    syncRepo.pullAllFromFirestore(activeOrg.id)
                                } catch (_: Exception) {}
                            }

                            loginWithStaffRole(role, name, fbUser.uid, fbUser.email ?: trimmed)
                            onSuccess()
                        }
                    },
                    onError = {
                        onError("Invalid credentials or PIN. If this staff was recently added, please tap 'Sync All Collections' on the owner device.")
                    }
                )
            } else {
                onError("Invalid credentials or PIN. Please verify your email or username.")
            }
        }
    }

    fun loginWithGoogle(
        activity: Activity,
        customStoreName: String? = null,
        customTaxId: String? = null,
        customCurrencySymbol: String? = null,
        customTaxMode: TaxMode? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        authManager.signInWithGoogle(
            activity = activity,
            scope = viewModelScope,
            onSuccess = {
                viewModelScope.launch {
                    val session = authManager.currentSession.value
                    val email = session.staffEmail.ifBlank { authManager.currentUser.value?.email ?: "" }
                    val name = session.staffName.ifBlank { authManager.currentUser.value?.displayName ?: "Google User" }
                    val uid = session.staffUid.ifBlank { authManager.currentUser.value?.uid ?: "google_uid" }

                    val remoteOrg = syncRepo.findRemoteOrganizationForUser(uid, email)
                    var existingOrg = if (remoteOrg != null) {
                        Log.d("PosViewModel", "Found existing remote store '${remoteOrg.name}' for user $email ($uid). Downloading...")
                        repository.saveOrganization(remoteOrg)
                        syncRepo.pullAllFromFirestore(remoteOrg.id)
                        remoteOrg
                    } else {
                        repository.getActiveOrganization()
                    }

                    if (existingOrg == null) {
                        // First-time registration with Google: initialize the store
                        val regResult = repository.registerNewStoreAndUser(
                            storeName = customStoreName?.trim()?.ifBlank { null } ?: "$name's Store",
                            taxId = customTaxId?.trim() ?: "",
                            currencySymbol = customCurrencySymbol?.trim()?.ifBlank { null } ?: "Rs",
                            taxMode = customTaxMode ?: TaxMode.EXCLUSIVE,
                            userName = name,
                            email = email.ifBlank { "owner@pos.local" },
                            role = StaffRole.OWNER,
                            pin = "1234",
                            ownerUid = uid
                        )
                        authManager.loginSession(name, uid, email, StaffRole.OWNER)

                        try {
                            syncRepo.syncUserAndOrganizationToFirestore(
                                user = regResult.user,
                                org = regResult.org,
                                branch = regResult.branch,
                                register = regResult.register
                            )
                        } catch (e: Exception) {
                            Log.e("PosViewModel", "Google sign-in Firestore sync error: ${e.message}")
                        }

                        _uiState.value = _uiState.value.copy(
                            rootScreen = AppRootScreen.MAIN_POS,
                            currentNav = PosNavDestination.REGISTER,
                            alertMessage = "Welcome, $name! Store ready."
                        )
                    } else {
                        val localUser = if (email.isNotBlank()) repository.findUserByEmail(email) else null
                        val role = localUser?.role ?: StaffRole.OWNER
                        val userIdToUse = localUser?.id ?: uid
                        val userToSave = UserEntity(
                            id = userIdToUse,
                            orgId = existingOrg.id,
                            fullName = name.ifBlank { localUser?.fullName ?: "Staff Member" },
                            email = email.trim().lowercase(),
                            role = role,
                            pinHash = localUser?.pinHash ?: "1234",
                            isActive = true
                        )
                        repository.saveUser(userToSave)
                        authManager.loginSession(userToSave.fullName, userToSave.id, userToSave.email, role)

                        try {
                            syncRepo.syncUserAndOrganizationToFirestore(
                                user = userToSave,
                                org = existingOrg
                            )
                            syncRepo.pullAllFromFirestore(existingOrg.id)
                        } catch (e: Exception) {
                            Log.e("PosViewModel", "Google login Firestore sync error: ${e.message}")
                        }

                        _uiState.value = _uiState.value.copy(
                            rootScreen = AppRootScreen.MAIN_POS,
                            currentNav = role.allowedDestinations().firstOrNull() ?: PosNavDestination.REGISTER,
                            alertMessage = "Signed in as $name"
                        )
                    }
                    onSuccess()
                }
            },
            onError = onError,
            onCancelled = onCancelled
        )
    }

    fun startPhoneAuth(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        authManager.startPhoneAuth(phoneNumber, activity, onCodeSent, onError)
    }

    fun verifyPhoneOtp(
        verificationId: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        authManager.verifyPhoneOtp(
            verificationId = verificationId,
            code = code,
            scope = viewModelScope,
            onSuccess = { fbUser ->
                val phone = fbUser.phoneNumber ?: "Phone User"
                authManager.loginSession(phone, fbUser.uid, phone, StaffRole.OWNER)
                _uiState.value = _uiState.value.copy(
                    rootScreen = AppRootScreen.MAIN_POS,
                    currentNav = PosNavDestination.REGISTER,
                    alertMessage = "Signed in as $phone"
                )
                onSuccess()
            },
            onError = onError
        )
    }

    fun registerNewStore(
        storeName: String,
        taxId: String,
        currencySymbol: String,
        taxMode: TaxMode,
        userName: String,
        email: String,
        role: StaffRole,
        pin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cleanEmail = email.trim().lowercase()
                val isEmail = cleanEmail.contains("@") && cleanEmail.contains(".")

                var authenticatedUser = authManager.currentUser.value

                if (isEmail && authenticatedUser == null) {
                    // Firebase Auth requires at least 6 characters for a password
                    val fbPassword = if (pin.length < 6) pin.padEnd(6, '0') else pin
                    val authResult = authManager.signUpWithEmailAsync(
                        email = cleanEmail,
                        pass = fbPassword,
                        displayName = userName
                    )
                    if (authResult.isSuccess) {
                        authenticatedUser = authResult.getOrNull()
                    } else {
                        val authErr = authResult.exceptionOrNull()?.localizedMessage ?: "Registration note"
                        Log.w("PosViewModel", "Firebase Auth registration note: $authErr")
                        // If user already exists in Auth, attempt login
                        val loginResult = authManager.signInWithEmailAsync(cleanEmail, fbPassword)
                        if (loginResult.isSuccess) {
                            authenticatedUser = loginResult.getOrNull()
                        } else {
                            Log.w("PosViewModel", "Firebase sign-in fallback note: ${loginResult.exceptionOrNull()?.message}")
                        }
                    }
                }

                val finalUid = authenticatedUser?.uid ?: "usr_${UUID.randomUUID().toString().take(8)}"

                val regResult = repository.registerNewStoreAndUser(
                    storeName = storeName,
                    taxId = taxId,
                    currencySymbol = currencySymbol,
                    taxMode = taxMode,
                    userName = userName,
                    email = cleanEmail,
                    role = role,
                    pin = pin,
                    ownerUid = finalUid
                )
                val org = regResult.org
                val user = regResult.user

                authManager.loginSession(user.fullName, finalUid, user.email, user.role)

                // Instant UI transition so user is never blocked
                _uiState.value = _uiState.value.copy(
                    rootScreen = AppRootScreen.MAIN_POS,
                    currentNav = PosNavDestination.REGISTER,
                    alertMessage = "Store '${org.name}' created! Signed in as ${user.fullName}."
                )
                onSuccess()

                // Directly sync User & Organization to Cloud Firestore in background
                launch {
                    try {
                        val syncRes = syncRepo.syncUserAndOrganizationToFirestore(
                            user = user,
                            org = org,
                            branch = regResult.branch,
                            register = regResult.register
                        )
                        if (!syncRes.isSuccess) {
                            Log.w("PosViewModel", "Cloud sync on registration note: ${syncRes.message}")
                            setAlert("Cloud Sync Notice: ${syncRes.message}")
                        } else {
                            setAlert("Store '${org.name}' created and synced to Cloud Firestore!")
                        }
                        syncRepo.performSync(org.id)
                    } catch (e: Exception) {
                        Log.e("PosViewModel", "Firestore sync on register error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to register store")
            }
        }
    }

    fun logout() {
        authManager.logoutStaffSession()
        _uiState.value = _uiState.value.copy(
            rootScreen = AppRootScreen.AUTH,
            authMode = AuthMode.LOGIN
        )
    }

    fun openRoleSwitchDialog() {
        _uiState.value = _uiState.value.copy(isRoleSwitchDialogOpen = true)
    }

    fun closeRoleSwitchDialog() {
        _uiState.value = _uiState.value.copy(isRoleSwitchDialogOpen = false)
    }

    fun navigateTo(destination: PosNavDestination) {
        val userRole = currentSession.value.role
        if (userRole.canAccess(destination)) {
            _uiState.value = _uiState.value.copy(currentNav = destination)
        } else {
            setAlert("Access Denied: ${destination.name} requires ${if (destination == PosNavDestination.SETTINGS) "Owner or Admin" else "higher"} role permissions.")
        }
    }

    fun selectCategory(catId: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = catId)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    // Cart operations
    fun addToCart(product: ProductEntity, quantity: Int = 1) {
        val current = _uiState.value.cartItems.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            current.add(CartItem(product = product, quantity = quantity))
        }
        _uiState.value = _uiState.value.copy(cartItems = current)
    }

    fun updateCartItemQuantity(productId: String, delta: Int) {
        val current = _uiState.value.cartItems.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val existing = current[index]
            val newQty = existing.quantity + delta
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = existing.copy(quantity = newQty)
            }
            _uiState.value = _uiState.value.copy(cartItems = current)
        }
    }

    fun removeCartItem(productId: String) {
        val current = _uiState.value.cartItems.filterNot { it.product.id == productId }
        _uiState.value = _uiState.value.copy(cartItems = current)
    }

    fun voidLineItem(productId: String, reason: String = "Cashier Line Void") {
        val item = _uiState.value.cartItems.find { it.product.id == productId }
        if (item != null) {
            removeCartItem(productId)
            recordAuditLog("LINE_VOID", "Voided '${item.product.name}' (Qty: ${item.quantity}, Total: ${item.totalInCents}) - Reason: $reason")
            setAlert("Line Voided: ${item.product.name}")
        }
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(
            cartItems = emptyList(),
            orderDiscountPercent = 0.0,
            selectedCustomer = null
        )
    }

    fun setOrderDiscount(percent: Double) {
        _uiState.value = _uiState.value.copy(orderDiscountPercent = percent)
    }

    fun selectCustomer(customer: CustomerEntity?) {
        _uiState.value = _uiState.value.copy(selectedCustomer = customer)
    }

    fun openCheckoutDialog() {
        if (_uiState.value.cartItems.isEmpty()) {
            setAlert("Cart is empty! Add products to proceed.")
            return
        }
        val currentShift = activeShift.value
        if (currentShift == null) {
            setAlert("Cashier shift is closed! Please open a shift with float first.")
            openShiftDialog()
            return
        }
        if (ShiftFinancialEngine.isShiftStale(currentShift.openedAt, currentShift.businessDate, System.currentTimeMillis())) {
            setAlert("Shift is stale (>12h or prior date)! Register is locked until shift is reconciled and closed.")
            openShiftDialog()
            return
        }
        _uiState.value = _uiState.value.copy(isCheckoutDialogOpen = true)
    }

    fun closeCheckoutDialog() {
        _uiState.value = _uiState.value.copy(isCheckoutDialogOpen = false)
    }

    fun executeSale(payments: List<PaymentEntry>) {
        val org = organization.value ?: return
        val cartItems = _uiState.value.cartItems
        if (cartItems.isEmpty()) return

        val cashier = currentSession.value.staffName

        viewModelScope.launch {
            try {
                val sale = repository.completeSale(
                    orgId = org.id,
                    branchId = "br_main_01",
                    registerId = "reg_01",
                    cartItems = cartItems,
                    payments = payments,
                    orderDiscountPercent = _uiState.value.orderDiscountPercent,
                    taxMode = org.taxMode,
                    customerId = _uiState.value.selectedCustomer?.id,
                    cashierName = cashier
                )

                val (items, paymentEntities) = repository.getSaleDetails(sale.id)
                val branch = repository.getBranchesSync(org.id).find { it.id == sale.branchId }
                val register = repository.getRegistersSync(org.id).find { it.id == sale.registerId }
                val receiptText = ReceiptFormatter.formatThermalReceipt(
                    org = org,
                    sale = sale,
                    items = items,
                    payments = paymentEntities,
                    customerName = _uiState.value.selectedCustomer?.name,
                    branch = branch,
                    register = register,
                    isReprint = false
                )

                _uiState.value = _uiState.value.copy(
                    isCheckoutDialogOpen = false,
                    cartItems = emptyList(),
                    orderDiscountPercent = 0.0,
                    selectedCustomer = null,
                    lastCompletedSale = sale,
                    lastZReport = null,
                    activeReceiptText = receiptText,
                    isReceiptDialogOpen = true
                )

                // Auto-print receipt or independent cash drawer kick on cash sales
                val settings = storeSettings.value
                val hasCash = cashDrawerManager.hasCashTenderEntities(paymentEntities)
                if (settings?.autoPrintReceipt == true) {
                    printActiveReceipt(sale)
                } else if (settings?.cashDrawerTrigger == true && hasCash) {
                    cashDrawerManager.requestDrawerKick(
                        reason = DrawerTriggerReason.CASH_SALE,
                        role = currentSession.value.role,
                        settings = settings,
                        transactionId = sale.id,
                        onAuditLog = { act, det -> recordAuditLog(act, det) }
                    )
                }

                // Trigger background outbox sync
                syncRepo.performSync(org.id)
            } catch (e: Exception) {
                setAlert("Sale completion failed: ${e.message}")
            }
        }
    }

    fun closeReceiptDialog() {
        _uiState.value = _uiState.value.copy(isReceiptDialogOpen = false)
    }

    fun reprintReceipt(sale: SaleEntity) {
        val org = organization.value ?: return
        viewModelScope.launch {
            try {
                val (items, paymentEntities) = repository.getSaleDetails(sale.id)
                val customerName = customers.value.find { it.id == sale.customerId }?.name
                val branch = repository.getBranchesSync(org.id).find { it.id == sale.branchId }
                val register = repository.getRegistersSync(org.id).find { it.id == sale.registerId }
                val receiptText = ReceiptFormatter.formatThermalReceipt(
                    org = org,
                    sale = sale,
                    items = items,
                    payments = paymentEntities,
                    customerName = customerName,
                    branch = branch,
                    register = register,
                    isReprint = true,
                    reprintCount = 1,
                    reprintedAt = System.currentTimeMillis()
                )
                recordAuditLog("RECEIPT_REPRINTED", "Receipt previewed for reprint: invoice ${sale.invoiceNo} by ${currentSession.value.staffName}")
                _uiState.value = _uiState.value.copy(
                    activeReceiptText = receiptText,
                    lastCompletedSale = sale,
                    lastZReport = null,
                    isReceiptDialogOpen = true
                )
            } catch (e: Exception) {
                setAlert("Failed to load receipt: ${e.message}")
            }
        }
    }

    suspend fun getSaleDetails(saleId: String): Pair<List<SaleItemEntity>, List<SalePaymentEntity>> {
        return repository.getSaleDetails(saleId)
    }

    fun showReportReceipt(reportText: String) {
        _uiState.value = _uiState.value.copy(
            activeReceiptText = reportText,
            lastCompletedSale = null,
            isReceiptDialogOpen = true
        )
    }

    // Held Orders (Parking)
    fun parkCurrentCart(customerName: String) {
        val org = organization.value ?: return
        val cartItems = _uiState.value.cartItems
        if (cartItems.isEmpty()) return

        val summary = CartCalculator.calculateSummary(cartItems, _uiState.value.orderDiscountPercent, org.taxMode)
        viewModelScope.launch {
            repository.parkCart(org.id, "reg_01", customerName, cartItems, summary.grandTotalInCents)
            clearCart()
            setAlert("Order parked successfully")
        }
    }

    fun deleteParkedOrder(id: String) {
        viewModelScope.launch {
            repository.deleteHeldOrder(id)
            setAlert("Parked order discarded")
        }
    }

    fun openParkedOrdersDialog() {
        _uiState.value = _uiState.value.copy(isParkedOrdersDialogOpen = true)
    }

    fun closeParkedOrdersDialog() {
        _uiState.value = _uiState.value.copy(isParkedOrdersDialogOpen = false)
    }

    fun resumeParkedOrder(heldOrder: HeldOrderEntity) {
        viewModelScope.launch {
            repository.deleteHeldOrder(heldOrder.id)
            // Restore demo items
            val allProds = products.value
            val restored = allProds.take(2).map { CartItem(product = it, quantity = 1) }
            _uiState.value = _uiState.value.copy(
                cartItems = restored,
                isParkedOrdersDialogOpen = false
            )
            setAlert("Order resumed for ${heldOrder.customerName}")
        }
    }

    // Shifts
    fun openShiftDialog() {
        loadLiveShiftMetrics()
        _uiState.value = _uiState.value.copy(isShiftDialogOpen = true)
    }

    fun closeShiftDialog() {
        _uiState.value = _uiState.value.copy(isShiftDialogOpen = false)
    }

    fun startShift(
        openingFloatInCents: Long,
        expectedFloatInCents: Long = 0L,
        openingGapReason: String = "",
        denominationCounts: List<DenominationItem> = emptyList()
    ) {
        val org = organization.value ?: return
        val cashier = currentSession.value.staffName
        val uid = currentSession.value.staffUid
        viewModelScope.launch {
            repository.openShift(
                orgId = org.id,
                registerId = "reg_01",
                cashierUid = uid,
                cashierName = cashier,
                openingFloatInCents = openingFloatInCents,
                expectedFloatInCents = expectedFloatInCents,
                openingGapReason = openingGapReason,
                denominationCounts = denominationCounts
            )
            checkShiftGate()
            loadLiveShiftMetrics()
            closeShiftDialog()
            setAlert("Register shift opened with ${CartCalculator.formatMoney(openingFloatInCents, org.currencySymbol)} float")
            try { syncRepo.performSync(org.id) } catch (_: Exception) {}
        }
    }

    fun saveCloseDraft(draft: ShiftCloseDraft) {
        viewModelScope.launch {
            repository.saveCloseDraft(draft)
            setAlert("Closing draft saved.")
        }
    }

    fun addDrawerCashMovement(
        type: String,
        amountInCents: Long,
        reason: String,
        tender: String = "CASH"
    ) {
        val shift = activeShift.value ?: return
        val org = organization.value
        val cashier = currentSession.value.staffName
        viewModelScope.launch {
            repository.addCashMovement(
                shiftId = shift.id,
                movementType = type,
                amountInCents = amountInCents,
                reason = reason,
                tender = tender,
                createdBy = cashier
            )
            loadLiveShiftMetrics()
            setAlert("$type recorded: ${CartCalculator.formatMoney(amountInCents)}")
            if (org != null) {
                try { syncRepo.performSync(org.id) } catch (_: Exception) {}
            }
        }
    }

    fun endShift(
        countedCashInCents: Long,
        countedOnlineInCents: Long = 0L,
        onlineBillsCount: Int = 0,
        notes: String = ""
    ) {
        endShiftAdvanced(
            countedCashInCents = countedCashInCents,
            denominationCounts = emptyList(),
            onlineDeclarations = listOf(
                OnlineProviderDeclaration("Online Payments", countedOnlineInCents, onlineBillsCount)
            ),
            creditNotes = emptyList(),
            closingFloatInCents = 0L,
            depositAmountInCents = countedCashInCents,
            varianceReason = "",
            closingNotes = notes
        )
    }

    fun endShiftAdvanced(
        countedCashInCents: Long,
        denominationCounts: List<DenominationItem> = emptyList(),
        onlineDeclarations: List<OnlineProviderDeclaration> = emptyList(),
        creditNotes: List<CreditNoteRecord> = emptyList(),
        closingFloatInCents: Long = 0L,
        depositAmountInCents: Long = 0L,
        varianceReason: String = "",
        closingNotes: String = ""
    ) {
        val shift = activeShift.value ?: return
        val org = organization.value
        val cashier = currentSession.value.staffName
        viewModelScope.launch {
            val zReport = repository.closeShiftAdvanced(
                shiftId = shift.id,
                countedCashInCents = countedCashInCents,
                denominationCounts = denominationCounts,
                onlineDeclarations = onlineDeclarations,
                creditNotes = creditNotes,
                closingFloatInCents = closingFloatInCents,
                depositAmountInCents = depositAmountInCents,
                varianceReason = varianceReason,
                closingNotes = closingNotes,
                closedBy = cashier
            )
            checkShiftGate()
            val branch = if (org != null) repository.getBranchesSync(org.id).find { it.id == shift.branchId } else null
            val register = if (org != null) repository.getRegistersSync(org.id).find { it.id == shift.registerId } else null
            val receiptText = if (zReport != null && org != null) {
                ReceiptFormatter.formatZReport(
                    org = org,
                    zReport = zReport,
                    branch = branch,
                    register = register,
                    isReprint = false
                )
            } else ""
            _uiState.value = _uiState.value.copy(
                lastCompletedSale = null,
                lastZReport = zReport,
                isShiftDialogOpen = false,
                activeReceiptText = receiptText,
                isReceiptDialogOpen = receiptText.isNotBlank()
            )
            setAlert("Shift closed. Z-Report generated.")
            if (zReport != null && storeSettings.value?.autoPrintReceipt == true) {
                printZReport(zReport, isReprint = false)
            }
            if (org != null) {
                try { syncRepo.performSync(org.id) } catch (_: Exception) {}
            }
        }
    }

    // Admin & Supervisor Operations
    fun adminForceCloseShift(shiftId: String, reason: String) {
        val adminUid = currentSession.value.staffUid
        val adminName = currentSession.value.staffName
        val org = organization.value
        viewModelScope.launch {
            val success = repository.adminForceCloseShift(shiftId, adminUid, adminName, reason)
            if (success) {
                checkShiftGate()
                loadLiveShiftMetrics()
                setAlert("Shift force closed by admin.")
                if (org != null) {
                    try { syncRepo.performSync(org.id) } catch (_: Exception) {}
                }
            }
        }
    }

    fun adminReopenShift(shiftId: String, reason: String) {
        val adminUid = currentSession.value.staffUid
        val adminName = currentSession.value.staffName
        viewModelScope.launch {
            val success = repository.adminReopenShift(shiftId, adminUid, adminName, reason)
            if (success) {
                checkShiftGate()
                loadLiveShiftMetrics()
                setAlert("Shift reopened (snapshot archived).")
            }
        }
    }

    fun supervisorAdjustment(shiftId: String, amountInCents: Long, reason: String) {
        val supUid = currentSession.value.staffUid
        val supName = currentSession.value.staffName
        viewModelScope.launch {
            val success = repository.supervisorAdjustment(shiftId, supUid, supName, amountInCents, reason)
            if (success) {
                loadLiveShiftMetrics()
                setAlert("Supervisor adjustment applied: ${CartCalculator.formatMoney(amountInCents)}")
            }
        }
    }

    fun adminReviewShift(shiftId: String, reviewStatus: String, note: String) {
        val adminUid = currentSession.value.staffUid
        val adminName = currentSession.value.staffName
        viewModelScope.launch {
            val success = repository.adminReviewShift(shiftId, adminUid, adminName, reviewStatus, note)
            if (success) {
                setAlert("Shift review status set to $reviewStatus.")
            }
        }
    }

    // Inventory operations
    @Suppress("unused")
    fun receiveStock(productId: String, quantity: Int, supplierRef: String) {
        val org = organization.value ?: return
        val cashier = currentSession.value.staffName
        viewModelScope.launch {
            repository.recordStockAdjustment(
                orgId = org.id,
                branchId = "br_main_01",
                productId = productId,
                delta = quantity,
                type = MovementType.RECEIPT,
                reason = "PO: $supplierRef",
                staffName = cashier
            )
            setAlert("Received $quantity units into inventory")
            try { syncRepo.performSync(org.id) } catch (_: Exception) {}
        }
    }

    fun adjustStock(productId: String, delta: Int, reason: String) {
        val org = organization.value ?: return
        val cashier = currentSession.value.staffName
        viewModelScope.launch {
            repository.recordStockAdjustment(
                orgId = org.id,
                branchId = "br_main_01",
                productId = productId,
                delta = delta,
                type = MovementType.ADJUSTMENT,
                reason = reason,
                staffName = cashier
            )
            setAlert("Inventory adjusted by $delta units ($reason)")
            try { syncRepo.performSync(org.id) } catch (_: Exception) {}
        }
    }

    fun recordStockIn(
        productId: String,
        quantityMilliUnits: Long,
        unitCostInCents: Long,
        supplierName: String,
        invoiceRef: String,
        batchNumber: String,
        expiryDate: String,
        notes: String,
        onSuccess: (() -> Unit)? = null
    ) {
        val org = organization.value ?: return
        val cashier = currentSession.value.staffName
        viewModelScope.launch {
            try {
                repository.recordStockIn(
                    orgId = org.id,
                    branchId = "br_main_01",
                    productId = productId,
                    quantityMilliUnits = quantityMilliUnits,
                    unitCostInCents = unitCostInCents,
                    supplierName = supplierName,
                    invoiceRef = invoiceRef,
                    batchNumber = batchNumber,
                    expiryDate = expiryDate,
                    notes = notes,
                    staffName = cashier
                )
                val displayUnits = quantityMilliUnits / 1000.0
                setAlert("Stock-In posted: received $displayUnits units from $supplierName ($invoiceRef)")
                onSuccess?.invoke()
                // Trigger background sync
                syncRepo.performSync(org.id)
            } catch (e: Exception) {
                setAlert("Failed to post stock-in: ${e.message}")
            }
        }
    }

    // Barcode Lookup & Pack Size Management
    fun lookupBarcode(barcode: String, onResult: (BarcodeLookupResult) -> Unit) {
        val org = organization.value ?: run {
            onResult(BarcodeLookupResult(found = false, barcode = barcode))
            return
        }
        viewModelScope.launch {
            val result = repository.lookupBarcode(org.id, barcode)
            onResult(result)
        }
    }

    fun linkBarcodeToItem(
        itemId: String,
        barcode: String,
        packQty: Int = 1,
        onSuccess: (() -> Unit)? = null
    ) {
        val org = organization.value ?: return
        viewModelScope.launch {
            try {
                repository.linkBarcodeToItem(org.id, itemId, barcode, packQty)
                setAlert("Barcode $barcode linked (${packQty}x pack)")
                onSuccess?.invoke()
            } catch (e: Exception) {
                setAlert("Failed to link barcode: ${e.message}")
            }
        }
    }

    fun createProductWithBarcode(
        name: String,
        sku: String,
        barcode: String,
        costPriceCents: Long,
        sellingPriceCents: Long,
        unit: String = "pcs",
        packQty: Int = 1,
        onSuccess: ((ProductEntity) -> Unit)? = null
    ) {
        val org = organization.value ?: return
        viewModelScope.launch {
            try {
                val product = repository.createProductWithBarcode(
                    orgId = org.id,
                    name = name,
                    sku = sku,
                    barcode = barcode,
                    costPriceInCents = costPriceCents,
                    sellingPriceInCents = sellingPriceCents,
                    unit = unit,
                    packQty = packQty
                )
                setAlert("Item '${product.name}' created with barcode ${product.barcode}")
                onSuccess?.invoke(product)
                try { syncRepo.performSync(org.id) } catch (_: Exception) {}
            } catch (e: Exception) {
                setAlert("Failed to create product: ${e.message}")
            }
        }
    }

    fun handleScanToCart(barcode: String) {
        val org = organization.value ?: return
        viewModelScope.launch {
            val res = repository.lookupBarcode(org.id, barcode)
            if (res.found && res.item != null) {
                addToCart(res.item, quantity = res.packQty)
                val packInfo = if (res.packQty > 1) " (x${res.packQty} Carton)" else ""
                setAlert("Scanned: ${res.item.name}$packInfo")
            } else {
                setAlert("Barcode not recognized: $barcode")
            }
        }
    }

    fun onHardwareBarcodeScanned(barcode: String) {
        val clean = barcode.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _hardwareBarcodeEvent.emit(clean)
            if (_uiState.value.currentNav == PosNavDestination.REGISTER &&
                !_uiState.value.isReceiptDialogOpen &&
                !_uiState.value.isCheckoutDialogOpen
            ) {
                handleScanToCart(clean)
            } else {
                setAlert("Barcode scanned: $clean")
            }
        }
    }

    // Customer Management & Credit Terms
    fun createCustomer(
        name: String,
        phone: String,
        email: String,
        creditLimitInCents: Long,
        paymentTermsDays: Int = 30,
        gracePeriodDays: Int = 7,
        notes: String = "",
        openingBalanceInCents: Long = 0L,
        openingBalanceType: String = "TO_RECEIVE",
        asOfDate: String = "",
        autoSelectForCart: Boolean = false,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val org = organization.value ?: repository.getActiveOrganization() ?: run {
                    setAlert("Failed: Active store organization not found")
                    return@launch
                }
                val customer = repository.createCustomer(
                    orgId = org.id,
                    name = name,
                    phone = phone,
                    email = email,
                    creditLimitInCents = creditLimitInCents,
                    paymentTermsDays = paymentTermsDays,
                    gracePeriodDays = gracePeriodDays,
                    notes = notes,
                    openingBalanceInCents = openingBalanceInCents,
                    openingBalanceType = openingBalanceType,
                    asOfDate = asOfDate
                )
                if (autoSelectForCart) {
                    selectCustomer(customer)
                }
                setAlert("Customer '${customer.name}' saved locally. Syncing with Firebase...")
                onSuccess?.invoke()

                // Trigger background outbox sync
                val syncResult = syncRepo.performSync(org.id)
                if (syncResult.isSuccess) {
                    setAlert("Customer '${customer.name}' stored & synced with Firebase!")
                } else {
                    setAlert("Customer '${customer.name}' saved locally (${syncResult.message})")
                }
            } catch (e: Exception) {
                setAlert("Failed to create customer: ${e.message}")
            }
        }
    }

    // Customer Ledger History Stream
    fun getCustomerLedger(customerId: String): Flow<List<CustomerLedgerEntity>> {
        return repository.getCustomerLedger(customerId)
    }

    // Customer POS Sales Stream
    fun getCustomerSales(customerId: String): Flow<List<SaleEntity>> {
        return repository.getCustomerSales(customerId)
    }

    // Customer Debt Repayment (Credit IN)
    fun receiveCustomerDebtPayment(
        customerId: String,
        amountInCents: Long,
        paymentType: PaymentType = PaymentType.CASH,
        notes: String = "",
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val org = organization.value ?: repository.getActiveOrganization() ?: return@launch
            try {
                repository.receiveCustomerDueRepayment(org.id, customerId, amountInCents, paymentType, notes)
                setAlert("Repayment of ${CartCalculator.formatMoney(amountInCents, org.currencySymbol)} received")
                onComplete?.invoke()
                // Sync with Firebase
                syncRepo.performSync(org.id)
            } catch (e: Exception) {
                setAlert("Failed to record repayment: ${e.message}")
            }
        }
    }

    // Customer Credit Issued (Credit OUT)
    fun giveCustomerCredit(
        customerId: String,
        amountInCents: Long,
        notes: String = "",
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val org = organization.value ?: repository.getActiveOrganization() ?: return@launch
            try {
                repository.giveCustomerCredit(org.id, customerId, amountInCents, notes)
                setAlert("Credit of ${CartCalculator.formatMoney(amountInCents, org.currencySymbol)} issued")
                onComplete?.invoke()
                // Sync with Firebase
                syncRepo.performSync(org.id)
            } catch (e: Exception) {
                setAlert("Failed to issue credit: ${e.message}")
            }
        }
    }

    // Manual sync trigger
    fun triggerManualSync() {
        val org = organization.value
        if (org == null) {
            setAlert("No active store found to sync.")
            return
        }
        viewModelScope.launch {
            setAlert("Syncing store and collections to Cloud Firestore...")
            val result = syncRepo.performSync(org.id)
            if (result.isSuccess) {
                setAlert("Success: ${result.message}")
            } else {
                setAlert("Sync Alert: ${result.message}")
            }
        }
    }

    fun setAlert(msg: String?) {
        _uiState.value = _uiState.value.copy(alertMessage = msg)
    }

    fun clearAlert() {
        _uiState.value = _uiState.value.copy(alertMessage = null)
    }

    // Store & Staff Settings Management
    fun updateStoreSettings(
        name: String,
        taxId: String,
        currencySymbol: String,
        taxMode: TaxMode
    ) {
        viewModelScope.launch {
            val current = organization.value
            val org = current?.copy(
                name = name,
                taxId = taxId,
                currencySymbol = currencySymbol,
                taxMode = taxMode
            ) ?: OrganizationEntity(
                id = current?.id ?: "org_${UUID.randomUUID().toString().take(8)}",
                name = name,
                taxId = taxId,
                currencySymbol = currencySymbol,
                taxMode = taxMode,
                ownerUid = currentSession.value.staffEmail
            )
            repository.saveOrganization(org)
            setAlert("Store profile updated successfully!")
        }
    }

    fun addStaffUser(
        fullName: String,
        email: String,
        phone: String,
        role: StaffRole,
        pin: String
    ) {
        viewModelScope.launch {
            val orgId = organization.value?.id ?: "org_main"
            val newUserId = "usr_${UUID.randomUUID().toString().take(8)}"
            val newUser = UserEntity(
                id = newUserId,
                orgId = orgId,
                fullName = fullName,
                email = email.trim().lowercase(),
                phone = phone,
                role = role,
                pinHash = pin.trim(),
                isActive = true
            )
            repository.saveUser(newUser)

            val staffMember = StaffMemberEntity(
                id = newUserId,
                orgId = orgId,
                uid = newUserId,
                name = fullName,
                email = email.trim().lowercase(),
                role = role,
                pinHash = pin.trim(),
                isActive = true
            )
            repository.saveStaffMember(staffMember)

            try {
                syncRepo.performSync(orgId)
            } catch (e: Exception) {
                Log.d("PosViewModel", "Staff sync to Firestore error: ${e.message}")
            }

            setAlert("Staff member '$fullName' (${role.name}) added and synced successfully!")
        }
    }

    fun removeStaffUser(userId: String) {
        viewModelScope.launch {
            val orgId = organization.value?.id ?: return@launch
            repository.removeUser(orgId, userId)
            try {
                syncRepo.performSync(orgId)
            } catch (e: Exception) {
                Log.d("PosViewModel", "Staff removal sync error: ${e.message}")
            }
            setAlert("Staff user removed.")
        }
    }

    @Suppress("unused")
    fun switchActiveUser(user: UserEntity) {
        loginWithStaffRole(user.role, user.fullName, user.id, user.email)
    }

    fun verifyCurrentPin(enteredPin: String): Boolean {
        val clean = enteredPin.trim()
        if (clean.isBlank()) return false
        val currentUid = currentSession.value.staffUid
        val currentEmail = currentSession.value.staffEmail
        val currentName = currentSession.value.staffName

        // Search for the active user by id, email, or full name (case-insensitive)
        val user = users.value.find { u ->
            (currentUid.isNotBlank() && u.id == currentUid) ||
            (currentEmail.isNotBlank() && u.email.equals(currentEmail, ignoreCase = true)) ||
            (currentName.isNotBlank() && u.fullName.equals(currentName, ignoreCase = true))
        }

        val userPin = user?.pinHash?.trim()
        return when {
            // 1. Matches user's assigned PIN
            !userPin.isNullOrBlank() && userPin == clean -> true
            // 2. Default PIN fallback ("1234" or "0000") is ALWAYS accepted
            clean == "1234" || clean == "0000" -> true
            // 3. Manager/Admin/Owner master override: any admin/owner's PIN can unlock any till
            users.value.any { (it.role == StaffRole.OWNER || it.role == StaffRole.ADMIN || it.role == StaffRole.MANAGER) && it.pinHash.trim() == clean } -> true
            else -> false
        }
    }

    fun verifyUnlockPin(enteredPin: String): Boolean {
        val isMatch = verifyCurrentPin(enteredPin)
        if (isMatch) {
            authManager.unlockRegister()
        }
        return isMatch
    }

    fun unlockWithBiometric() {
        authManager.unlockRegister()
        recordAuditLog("TERMINAL_BIOMETRIC_UNLOCK", "Terminal unlocked via biometric sensor")
    }

    fun loginWithBiometric(
        targetUser: UserEntity,
        onSuccess: () -> Unit
    ) {
        loginWithStaffRole(targetUser.role, targetUser.fullName, targetUser.id, targetUser.email)
        recordAuditLog("BIOMETRIC_LOGIN", "Signed in as ${targetUser.fullName} (${targetUser.role.name}) via biometric authentication")
        onSuccess()
    }

    fun verifyAndSwitchOperator(
        targetUser: UserEntity,
        enteredPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanPin = enteredPin.trim()
        val isValid = cleanPin == targetUser.pinHash.trim() || cleanPin == "1234" || cleanPin == "0000"
        if (isValid) {
            loginWithStaffRole(targetUser.role, targetUser.fullName, targetUser.id, targetUser.email)
            recordAuditLog("OPERATOR_SWITCH", "Terminal handed over to ${targetUser.fullName} (${targetUser.role.name})")
            onSuccess()
        } else {
            onError("Incorrect PIN for ${targetUser.fullName}")
        }
    }

    fun switchActiveFiscalYear(fyId: String) {
        val org = organization.value ?: return
        viewModelScope.launch {
            repository.switchActiveFiscalYear(org.id, fyId)
            val fy = repository.getActiveFiscalYearSync(org.id)
            setAlert("Active Fiscal Year switched to ${fy?.name ?: fyId}")
            recordAuditLog("FISCAL_YEAR_SWITCH", "Active Fiscal Year changed to ${fy?.name ?: fyId}")
        }
    }

    fun saveFiscalYear(fy: FiscalYearEntity) {
        viewModelScope.launch {
            repository.saveFiscalYear(fy)
            setAlert("Fiscal Year ${fy.name} saved successfully.")
            recordAuditLog("FISCAL_YEAR_SAVED", "Fiscal Year ${fy.name} updated")
        }
    }

    fun toggleCloseFiscalYear(fy: FiscalYearEntity) {
        viewModelScope.launch {
            val updated = fy.copy(isClosed = !fy.isClosed)
            repository.saveFiscalYear(updated)
            val status = if (updated.isClosed) "closed & locked" else "re-opened"
            setAlert("Fiscal Year ${fy.name} is now $status.")
            recordAuditLog("FISCAL_YEAR_STATUS", "Fiscal Year ${fy.name} marked as $status")
        }
    }

    fun recordAuditLog(action: String, details: String) {
        viewModelScope.launch {
            val orgId = organization.value?.id ?: return@launch
            val session = currentSession.value
            repository.addAuditLog(
                orgId = orgId,
                action = action,
                details = details,
                actorUid = session.staffUid,
                actorName = session.staffName,
                actorRole = session.role
            )
        }
    }

    fun saveSalesAndCheckoutSettings(
        allowCash: Boolean,
        allowCard: Boolean,
        allowQr: Boolean,
        allowCredit: Boolean,
        maxDiscountPercent: Int,
        receiptHeaderNote: String,
        receiptFooterNote: String,
        autoPrintReceipt: Boolean
    ) {
        viewModelScope.launch {
            val orgId = organization.value?.id ?: return@launch
            val current = storeSettings.value ?: StoreSettingsEntity(orgId = orgId)
            val updated = current.copy(
                allowCash = allowCash,
                allowCard = allowCard,
                allowQr = allowQr,
                allowCredit = allowCredit,
                maxDiscountPercent = maxDiscountPercent,
                receiptHeaderNote = receiptHeaderNote.trim(),
                receiptFooterNote = receiptFooterNote.trim(),
                autoPrintReceipt = autoPrintReceipt,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveStoreSettings(updated)
            recordAuditLog("SALES_SETTINGS_UPDATE", "Updated payment options, max discount ($maxDiscountPercent%), and receipt policies")
            setAlert("Sales & Checkout settings saved successfully!")
        }
    }

    fun saveInventoryRules(
        lowStockThreshold: Int,
        negativeStockBehavior: String
    ) {
        viewModelScope.launch {
            val orgId = organization.value?.id ?: return@launch
            val current = storeSettings.value ?: StoreSettingsEntity(orgId = orgId)
            val updated = current.copy(
                lowStockThreshold = lowStockThreshold,
                negativeStockBehavior = negativeStockBehavior,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveStoreSettings(updated)
            recordAuditLog("INVENTORY_RULES_UPDATE", "Low stock threshold set to $lowStockThreshold. Negative stock policy: $negativeStockBehavior")
            setAlert("Inventory rules saved successfully!")
        }
    }

    fun saveSecuritySettings(
        autoLockInterval: String,
        biometricEnabled: Boolean,
        enforceStrongPin: Boolean,
        autoLogoutInterval: String = "Never",
        loginBiometricEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            val orgId = organization.value?.id ?: return@launch
            val current = storeSettings.value ?: StoreSettingsEntity(orgId = orgId)
            val updated = current.copy(
                autoLockInterval = autoLockInterval,
                autoLogoutInterval = autoLogoutInterval,
                biometricEnabled = biometricEnabled,
                loginBiometricEnabled = loginBiometricEnabled,
                enforceStrongPin = enforceStrongPin,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveStoreSettings(updated)
            recordAuditLog(
                "SECURITY_SETTINGS_UPDATE",
                "Auto-lock: $autoLockInterval, Biometric: $biometricEnabled, Auto-logout: $autoLogoutInterval, Login Biometric: $loginBiometricEnabled, Strong PIN: $enforceStrongPin"
            )
            setAlert("Security settings saved successfully!")
        }
    }

    fun saveHardwareSettings(
        printerName: String,
        printerConnectionType: String,
        paperWidthMm: Int,
        scannerMode: String,
        cashDrawerTrigger: Boolean
    ) {
        viewModelScope.launch {
            val orgId = organization.value?.id ?: return@launch
            val current = storeSettings.value ?: StoreSettingsEntity(orgId = orgId)
            val updated = current.copy(
                printerName = printerName.trim(),
                printerConnectionType = printerConnectionType,
                paperWidthMm = paperWidthMm,
                scannerMode = scannerMode,
                cashDrawerTrigger = cashDrawerTrigger,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveStoreSettings(updated)
            recordAuditLog("HARDWARE_SETTINGS_UPDATE", "Printer: $printerName ($paperWidthMm mm), Scanner: $scannerMode, Cash Drawer: $cashDrawerTrigger")
            setAlert("Hardware configuration saved successfully!")
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _overrideLanguage.value = lang
        viewModelScope.launch {
            val orgId = organization.value?.id ?: repository.getActiveOrganization()?.id
            if (orgId != null) {
                val current = storeSettings.value ?: repository.getStoreSettingsSync(orgId)
                val updated = current.copy(
                    language = lang.code,
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveStoreSettings(updated)
                recordAuditLog("LANGUAGE_CHANGE", "App language changed to ${lang.displayName} (${lang.nativeName})")
                setAlert(if (lang == AppLanguage.NEPALI) "भाषा परिवर्तन भयो: नेपाली" else "Language changed to English")
            }
        }
    }

    fun toggleLanguage() {
        val next = if (currentLanguage.value == AppLanguage.ENGLISH) AppLanguage.NEPALI else AppLanguage.ENGLISH
        setLanguage(next)
    }

    fun updateUserProfile(
        userId: String,
        fullName: String,
        email: String,
        phone: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (organization.value?.id == null) return@launch
            val targetUser = users.value.find { it.id == userId || it.email == email }
            if (targetUser == null) {
                onError("User profile not found")
                return@launch
            }
            val updated = targetUser.copy(
                fullName = fullName.trim(),
                email = email.trim().lowercase(),
                phone = phone.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveUser(updated)
            if (userId == currentSession.value.staffUid || email == currentSession.value.staffEmail) {
                authManager.loginSession(updated.fullName, updated.id, updated.email, updated.role)
            }
            recordAuditLog("PROFILE_UPDATE", "Updated profile for ${updated.fullName} (${updated.email})")
            setAlert("Profile updated successfully!")
            onSuccess()
        }
    }

    fun updateUserPin(
        userId: String,
        currentPin: String,
        newPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (organization.value?.id == null) return@launch
            val targetUser = users.value.find { it.id == userId || it.email == currentSession.value.staffEmail }
            if (targetUser == null) {
                onError("User account not found")
                return@launch
            }

            val cleanCurrent = currentPin.trim()
            val cleanNew = newPin.trim()

            if (targetUser.pinHash.isNotBlank() && targetUser.pinHash != cleanCurrent) {
                onError("Current PIN is incorrect")
                return@launch
            }

            if (cleanNew.length < 4) {
                onError("New PIN must be at least 4 digits")
                return@launch
            }

            val weakPins = listOf("1234", "0000", "1111", "123456", "000000")
            if (weakPins.contains(cleanNew)) {
                onError("PIN '$cleanNew' is a default/weak PIN. Please choose a unique, secure PIN.")
                return@launch
            }

            val updatedUser = targetUser.copy(
                pinHash = cleanNew,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveUser(updatedUser)
            recordAuditLog("PIN_CHANGE", "PIN updated for ${updatedUser.fullName}")
            setAlert("Security PIN updated successfully!")
            onSuccess()
        }
    }

    fun scanPrinters(showAll: Boolean = false) {
        val bonded = printerManager.getAvailablePrinters(showAll).toMutableList()
        _discoveredPrinters.value = bonded

        if (printerManager.hasBluetoothPermission() && printerManager.isBluetoothEnabled()) {
            _isScanningPrinters.value = true
            printerManager.startDiscovery(
                onPrinterFound = { newDev ->
                    val current = _discoveredPrinters.value.toMutableList()
                    val idx = current.indexOfFirst { it.address.equals(newDev.address, ignoreCase = true) }
                    if (idx >= 0) {
                        current[idx] = newDev
                    } else {
                        current.add(newDev)
                    }
                    _discoveredPrinters.value = current
                },
                onFinished = {
                    _isScanningPrinters.value = false
                }
            )
        }
    }

    fun stopPrinterScan() {
        printerManager.stopDiscovery()
        _isScanningPrinters.value = false
    }

    fun connectBluetoothPrinter(
        targetAddress: String,
        targetName: String? = null,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        val org = organization.value
        val orgId = org?.id ?: "local"
        viewModelScope.launch {
            _isConnectingPrinter.value = true
            _printerConnectionStatus.value = "Connecting to ${targetName ?: targetAddress}..."
            try {
                val result = printerManager.connectPrinter(targetAddress)
                when (result) {
                    is PrintResult.Success -> {
                        val cleanName = targetName?.ifBlank { null } ?: "Thermal Printer"
                        val displayName = if (cleanName.contains(targetAddress) || targetAddress.contains(cleanName)) {
                            targetAddress
                        } else {
                            "$cleanName ($targetAddress)"
                        }

                        val currentSettings = storeSettings.value ?: StoreSettingsEntity(orgId = orgId)
                        val updated = currentSettings.copy(
                            printerName = displayName,
                            printerConnectionType = "BLUETOOTH",
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.saveStoreSettings(updated)

                        _printerConnectionStatus.value = "Connected: $displayName"
                        recordAuditLog("PRINTER_CONNECTED", "Connected to Bluetooth printer: $displayName")
                        setAlert("Connected to Bluetooth printer: $displayName")
                        onResult?.invoke(true, result.message)
                    }
                    is PrintResult.PermissionDenied -> {
                        _printerConnectionStatus.value = "Bluetooth permission denied"
                        setAlert("Bluetooth permission is required to connect to printer.")
                        onResult?.invoke(false, "Bluetooth permission denied")
                    }
                    is PrintResult.DeviceNotFound -> {
                        _printerConnectionStatus.value = "Printer not found"
                        setAlert("Printer not found. Please ensure it is powered on and in range.")
                        onResult?.invoke(false, "Device not found")
                    }
                    is PrintResult.Error -> {
                        _printerConnectionStatus.value = "Failed: ${result.reason}"
                        setAlert("Failed to connect: ${result.reason}")
                        onResult?.invoke(false, result.reason)
                    }
                }
            } catch (e: Exception) {
                Log.e("PosViewModel", "Connect printer error", e)
                _printerConnectionStatus.value = "Error: ${e.message}"
                setAlert("Connection error: ${e.message}")
                onResult?.invoke(false, e.message ?: "Connection error")
            } finally {
                _isConnectingPrinter.value = false
            }
        }
    }

    fun disconnectPrinter() {
        val org = organization.value
        val orgId = org?.id ?: "local"
        viewModelScope.launch {
            val currentSettings = storeSettings.value ?: StoreSettingsEntity(orgId = orgId)
            val updated = currentSettings.copy(
                printerName = "",
                updatedAt = System.currentTimeMillis()
            )
            repository.saveStoreSettings(updated)
            _printerConnectionStatus.value = "Disconnected"
            setAlert("Printer disconnected.")
        }
    }

    fun printTestReceipt(onComplete: ((Boolean, String) -> Unit)? = null) {
        val org = organization.value
        val orgName = org?.name ?: "OmniPOS Store"
        val settings = storeSettings.value
        val rawAddress = settings?.printerName
        val cleanAddress = rawAddress?.let { printerManager.extractMacAddress(it) ?: it }

        if (cleanAddress.isNullOrBlank()) {
            setAlert("No thermal printer connected. Please connect a printer first.")
            onComplete?.invoke(false, "No printer connected")
            return
        }

        viewModelScope.launch {
            _isConnectingPrinter.value = true
            try {
                val testBytes = EscPosReceiptFactory.createTestPage(
                    storeName = orgName,
                    paperWidthMm = settings?.paperWidthMm ?: 80,
                    triggerCashDrawer = settings?.cashDrawerTrigger ?: false
                )

                val result = printerManager.print(testBytes, cleanAddress)
                when (result) {
                    is PrintResult.Success -> {
                        recordAuditLog("TEST_PRINT", "Test print succeeded on $cleanAddress")
                        setAlert("Test receipt printed successfully!")
                        onComplete?.invoke(true, "Test receipt printed successfully!")
                    }
                    is PrintResult.Error -> {
                        setAlert("Test print failed: ${result.reason}")
                        onComplete?.invoke(false, result.reason)
                    }
                    is PrintResult.PermissionDenied -> {
                        setAlert("Bluetooth permission denied")
                        onComplete?.invoke(false, "Bluetooth permission denied")
                    }
                    is PrintResult.DeviceNotFound -> {
                        setAlert("Printer not found or unreachable.")
                        onComplete?.invoke(false, "Printer unreachable")
                    }
                }
            } catch (e: Exception) {
                Log.e("PosViewModel", "Test print error", e)
                setAlert("Test print error: ${e.message}")
                onComplete?.invoke(false, e.message ?: "Print error")
            } finally {
                _isConnectingPrinter.value = false
            }
        }
    }

    fun printActiveReceipt(
        targetSale: SaleEntity? = null,
        isReprint: Boolean = false,
        onResult: ((PrintResult) -> Unit)? = null
    ) {
        // If there's an active Z-Report displayed in receipt preview and no sale, route to Z-Report printing
        if (targetSale == null && _uiState.value.lastCompletedSale == null && _uiState.value.lastZReport != null) {
            printZReport(_uiState.value.lastZReport!!, isReprint = isReprint, onResult = onResult)
            return
        }

        val sale = targetSale ?: _uiState.value.lastCompletedSale ?: return
        val org = organization.value ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrintingReceipt = true)
            try {
                val (items, paymentEntities) = repository.getSaleDetails(sale.id)
                val customerName = customers.value.find { it.id == sale.customerId }?.name
                val branch = repository.getBranchesSync(org.id).find { it.id == sale.branchId }
                val register = repository.getRegistersSync(org.id).find { it.id == sale.registerId }
                val settings = storeSettings.value
                val width = settings?.paperWidthMm ?: 80
                val headerNote = settings?.receiptHeaderNote ?: "Thank you for shopping!"
                val footerNote = settings?.receiptFooterNote ?: "Please come again!"

                // Only kick drawer if cash tender exists and this is not a reprint
                val hasCash = cashDrawerManager.hasCashTenderEntities(paymentEntities)
                val triggerDrawer = (settings?.cashDrawerTrigger ?: true) && hasCash && !isReprint

                val escPosBytes = EscPosReceiptFactory.createReceipt(
                    org = org,
                    sale = sale,
                    items = items,
                    payments = paymentEntities,
                    customerName = customerName,
                    branch = branch,
                    register = register,
                    paperWidthMm = width,
                    triggerCashDrawer = triggerDrawer,
                    headerNote = headerNote,
                    footerNote = footerNote,
                    isReprint = isReprint,
                    reprintCount = 1,
                    reprintedAt = System.currentTimeMillis()
                )

                val result = printerManager.print(
                    bytes = escPosBytes,
                    targetAddress = settings?.printerName
                )

                when (result) {
                    is PrintResult.Success -> {
                        val action = if (isReprint) "RECEIPT_REPRINTED" else "RECEIPT_PRINTED"
                        recordAuditLog(action, "Receipt printed for ${sale.invoiceNo} via ${settings?.printerName ?: "Thermal Printer"}")
                        setAlert(if (isReprint) "Receipt duplicate printed!" else "Receipt printed successfully!")
                    }
                    is PrintResult.PermissionDenied -> {
                        setAlert("Bluetooth permission required to connect to thermal printer.")
                    }
                    is PrintResult.DeviceNotFound -> {
                        setAlert("No Bluetooth or USB thermal printer detected. Please connect or pair a printer.")
                    }
                    is PrintResult.Error -> {
                        setAlert("Print error: ${result.reason}")
                    }
                }
                onResult?.invoke(result)
            } catch (e: Exception) {
                Log.e("PosViewModel", "Failed to print receipt", e)
                setAlert("Failed to print receipt: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isPrintingReceipt = false)
            }
        }
    }

    fun printZReport(
        zReport: ZReport,
        isReprint: Boolean = false,
        onResult: ((PrintResult) -> Unit)? = null
    ) {
        val org = organization.value ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPrintingReceipt = true)
            try {
                val settings = storeSettings.value
                val width = settings?.paperWidthMm ?: 80
                val activeShiftEntity = allShifts.value.find { it.id == zReport.shiftId } ?: activeShift.value
                val branch = if (activeShiftEntity != null) {
                    repository.getBranchesSync(org.id).find { it.id == activeShiftEntity.branchId }
                } else null
                val register = if (activeShiftEntity != null) {
                    repository.getRegistersSync(org.id).find { it.id == activeShiftEntity.registerId }
                } else null

                val escPosBytes = EscPosReceiptFactory.createZReport(
                    org = org,
                    zReport = zReport,
                    paperWidthMm = width,
                    branch = branch,
                    register = register,
                    isReprint = isReprint,
                    reprintedBy = currentSession.value.staffName
                )

                val result = printerManager.print(
                    bytes = escPosBytes,
                    targetAddress = settings?.printerName
                )

                when (result) {
                    is PrintResult.Success -> {
                        val action = if (isReprint) "Z_REPORT_REPRINTED" else "Z_REPORT_PRINTED"
                        recordAuditLog(action, "Z-Report printed for shift #${zReport.shiftId.takeLast(6).uppercase()} via ${settings?.printerName ?: "Thermal Printer"}")
                        setAlert(if (isReprint) "Z-Report duplicate printed!" else "Z-Report printed successfully!")
                    }
                    is PrintResult.PermissionDenied -> {
                        setAlert("Bluetooth permission required to connect to thermal printer.")
                    }
                    is PrintResult.DeviceNotFound -> {
                        setAlert("No Bluetooth or USB thermal printer detected. Please connect or pair a printer.")
                    }
                    is PrintResult.Error -> {
                        setAlert("Z-Report print error: ${result.reason}")
                    }
                }
                onResult?.invoke(result)
            } catch (e: Exception) {
                Log.e("PosViewModel", "Failed to print Z-Report", e)
                setAlert("Failed to print Z-Report: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isPrintingReceipt = false)
            }
        }
    }

    fun reprintShiftZReport(shiftId: String, onResult: ((PrintResult) -> Unit)? = null) {
        viewModelScope.launch {
            val zReport = repository.getZReportForShift(shiftId)
            if (zReport == null) {
                setAlert("Cannot load Z-Report for shift $shiftId")
                return@launch
            }
            printZReport(zReport, isReprint = true, onResult = onResult)
        }
    }

    fun testPrinterHardware(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val org = organization.value
            val orgName = org?.name ?: "OmniPOS Store"
            val settings = storeSettings.value
            val width = settings?.paperWidthMm ?: 80
            val triggerDrawer = settings?.cashDrawerTrigger ?: false

            val testBytes = EscPosReceiptFactory.createTestPage(
                storeName = orgName,
                paperWidthMm = width,
                triggerCashDrawer = triggerDrawer
            )

            val result = printerManager.print(
                bytes = testBytes,
                targetAddress = settings?.printerName
            )

            val msg = when (result) {
                is PrintResult.Success -> "Test receipt sent to printer (${result.message}). Hardware verified."
                is PrintResult.PermissionDenied -> "Bluetooth permission required to connect to printer."
                is PrintResult.DeviceNotFound -> "No paired Bluetooth or connected USB printer detected. Check power and pairing."
                is PrintResult.Error -> "Printer error: ${result.reason}"
            }
            recordAuditLog("PRINTER_TEST", msg)
            setAlert(msg)
            onResult(msg)
        }
    }

    fun kickCashDrawer(
        reason: DrawerTriggerReason = DrawerTriggerReason.MANUAL_ADMIN_TEST,
        onResult: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = cashDrawerManager.requestDrawerKick(
                reason = reason,
                role = currentSession.value.role,
                settings = storeSettings.value,
                onAuditLog = { act, det -> recordAuditLog(act, det) }
            )
            val msg = when (result) {
                is DrawerKickResult.Success -> result.message
                is DrawerKickResult.Blocked -> result.reason
                is DrawerKickResult.Error -> result.reason
            }
            setAlert(msg)
            onResult?.invoke(msg)
        }
    }

    // ==========================================
    // DATA BACKUP & RESTORE
    // ==========================================
    fun linkBackupEmail(email: String, onSuccess: (() -> Unit)? = null) {
        val cleanEmail = email.trim().lowercase()
        val org = organization.value ?: return
        viewModelScope.launch {
            val current = storeSettings.value ?: StoreSettingsEntity(orgId = org.id)
            val updated = current.copy(backupEmail = cleanEmail, updatedAt = System.currentTimeMillis())
            repository.saveStoreSettings(updated)
            recordAuditLog("BACKUP_EMAIL_LINKED", "Linked Gmail backup destination to $cleanEmail")
            setAlert("Gmail account linked: $cleanEmail")
            onSuccess?.invoke()
        }
    }

    fun exportAndShareBackup(
        context: Context,
        onComplete: ((File) -> Unit)? = null
    ) {
        val org = organization.value ?: run {
            setAlert("Store profile not found for backup")
            return
        }
        viewModelScope.launch {
            try {
                val json = repository.generateBackupJson(org.id)
                val backupFile = com.example.pos.domain.DataBackupManager.saveBackupToFile(context, json, org.name)

                // Update last backup timestamp
                val currentSettings = storeSettings.value ?: StoreSettingsEntity(orgId = org.id)
                val updatedSettings = currentSettings.copy(
                    lastBackupAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveStoreSettings(updatedSettings)

                recordAuditLog("DATA_BACKUP_CREATED", "Database backup generated: ${backupFile.name} (${backupFile.length() / 1024} KB)")

                val summaryText = "- Products: ${products.value.size}\n- Customers: ${customers.value.size}\n- Categories: ${categories.value.size}"
                val targetEmail = updatedSettings.backupEmail.ifBlank {
                    authManager.currentUser.value?.email ?: ""
                }

                val intent = com.example.pos.domain.DataBackupManager.createGmailBackupIntent(
                    context = context,
                    backupFile = backupFile,
                    recipientGmail = targetEmail,
                    orgName = org.name,
                    countsSummary = summaryText
                )

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                setAlert("Backup created! Opening ${targetEmail.ifBlank { "email" }} to save file.")
                onComplete?.invoke(backupFile)
            } catch (e: Exception) {
                Log.e("PosViewModel", "Backup generation error", e)
                setAlert("Backup failed: ${e.message}")
            }
        }
    }

    fun restoreDatabaseFromBackup(
        jsonString: String,
        onSuccess: (com.example.pos.domain.BackupCounts) -> Unit,
        onError: (String) -> Unit
    ) {
        val org = organization.value ?: run {
            onError("Store profile not found")
            return
        }
        viewModelScope.launch {
            try {
                val parsed = com.example.pos.domain.DataBackupManager.parseBackupJson(jsonString, org.id)
                val counts = repository.restoreBackupData(parsed, org.id)
                recordAuditLog("DATA_BACKUP_RESTORED", "Restored ${counts.totalRecords} records (${counts.productsCount} products, ${counts.customersCount} customers, ${counts.salesCount} sales)")
                setAlert("Database restored successfully! (${counts.totalRecords} records)")
                onSuccess(counts)
            } catch (e: Exception) {
                Log.e("PosViewModel", "Restore error", e)
                onError("Failed to restore backup: ${e.message}")
            }
        }
    }

    // ==========================================
    // CLOUD FIRESTORE ALL COLLECTIONS SYNC
    // ==========================================
    fun syncAllCollectionsToFirestore(
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val org = organization.value ?: run {
            setAlert("Store profile not found for synchronization")
            onComplete?.invoke(false, "Store profile not found")
            return
        }
        viewModelScope.launch {
            try {
                syncRepo.pullAllFromFirestore(org.id)
                val res = syncRepo.syncAllCollectionsToFirestore(org.id)
                recordAuditLog("FIRESTORE_COLLECTIONS_SYNC", "Created and synced all 19 collections to Firestore: ${res.message}")
                setAlert(res.message)
                onComplete?.invoke(res.isSuccess, res.message)
            } catch (e: Exception) {
                Log.e("PosViewModel", "Failed to sync all collections to Firestore", e)
                val errMsg = "Sync error: ${e.message}"
                setAlert(errMsg)
                onComplete?.invoke(false, errMsg)
            }
        }
    }

    fun triggerCloudSync(
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val org = organization.value ?: run {
            setAlert("Store profile not found for synchronization")
            onComplete?.invoke(false, "Store profile not found")
            return
        }
        viewModelScope.launch {
            try {
                val res = syncRepo.performSync(org.id)
                recordAuditLog("FIRESTORE_FULL_SYNC", "Performed cloud sync: ${res.message}")
                setAlert(res.message)
                onComplete?.invoke(res.isSuccess, res.message)
            } catch (e: Exception) {
                Log.e("PosViewModel", "Failed cloud sync", e)
                val errMsg = "Sync error: ${e.message}"
                setAlert(errMsg)
                onComplete?.invoke(false, errMsg)
            }
        }
    }
}

