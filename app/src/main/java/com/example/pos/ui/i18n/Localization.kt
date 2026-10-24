package com.example.pos.ui.i18n

import androidx.compose.runtime.compositionLocalOf

/**
 * Supported application languages.
 */
enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    NEPALI("ne", "Nepali", "नेपाली");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

/**
 * Universal localized string definitions for the OmniPOS application.
 * Every user-visible string must be defined here — no hardcoded strings in UI composables.
 */
interface PosStrings {
    // ── Navigation ─────────────────────────────────────────────────────────────
    val navRegister: String
    val navInventory: String
    val navCustomers: String
    val navReports: String
    val navSettings: String

    // ── Common / Global ─────────────────────────────────────────────────────────
    val newSale: String
    val transactionComplete: String
    val receiptReady: String
    val printEscPos: String
    val printBluetoothToast: String
    val optional: String
    val splashDbInit: String
    val splashVerifyingRbac: String
    val splashOrgReady: String
    val splashStoreSetupRequired: String
    val splashSubtitle: String
    val splashBadge: String
    val splashFooter: String

    // ── Top Bar ─────────────────────────────────────────────────────────────────
    val shiftOpen: String
    val shiftClosed: String
    val shiftClosingDraft: String
    val lockRegister: String
    val switchOperator: String
    val syncOnline: String
    val syncOffline: String
    val syncing: String
    val tillInfo: (till: String, staff: String) -> String

    // ── Language Switcher ───────────────────────────────────────────────────────
    val language: String
    val languageSubtitle: String
    val selectLanguage: String
    val english: String
    val nepali: String
    val currentLanguageLabel: String
    val languageChangedNotice: String
    val switchLanguagePrompt: String

    // ── POS Register Screen ─────────────────────────────────────────────────────
    val searchProducts: String
    val allCategories: String
    val scanBarcode: String
    val cartTitle: String
    val clearCart: String
    val lineVoid: String
    val voidAll: String
    val scanOrSearchPrompt: String
    val holdOrder: String
    val emptyCartMessage: String
    val subtotal: String
    val discount: String
    val tax: String
    val grandTotal: String
    val payNow: String
    val quickCash: String
    val itemsInCart: (count: Int) -> String
    val unitPrice: String
    val qty: String
    val stockLeft: (count: Int) -> String
    val outOfStock: String
    val customer: String
    val attachCustomer: String
    val removeCustomer: String
    val noCustomer: String
    val parkedOrders: String
    val noParkedOrders: String
    val resumeOrder: String
    val deleteOrder: String
    val parkedAt: (time: String) -> String
    val changeAmount: String
    val amountTendered: String
    val change: String
    val checkout: String
    val paymentType: String
    val splitPayment: String
    val addPayment: String
    val completePayment: String
    val totalToPay: (amount: String) -> String
    val cashTendered: (currency: String) -> String
    val quickCashLabel: String
    val exactAmount: (currency: String, amount: String) -> String
    val changeReturn: String
    val remainingDue: String
    val cancelButton: String
    val completeSale: String
    val cardTerminalReady: String
    val cardAmount: (amount: String) -> String
    val qrWalletReady: String
    val qrScanInstruction: String
    val attachCustomerForCredit: String
    val attachCustomerButton: String
    val customerLabel: (name: String) -> String
    val creditLimitLabel: String
    val existingDueLabel: String
    val availableCreditLabel: String
    val creditLimitExceeded: (available: String) -> String
    val creditDueTab: String
    val qrWalletTab: String

    // ── Inventory Screen ────────────────────────────────────────────────────────
    val branchInventory: String
    val receiveStock: String
    val lowStockAlert: String
    val searchInventory: String
    val costPrice: String
    val sellingPrice: String
    val reorderLevel: String
    val barcode: String
    val category: String
    val sku: String
    val stockLevel: String
    val addProduct: String
    val editProduct: String
    val deleteProduct: String
    val productName: String
    val productDetails: String
    val stockMovement: String
    val movementIn: String
    val movementOut: String
    val movementAdjust: String
    val movementWastage: String
    val reason: String
    val quantity: String
    val noProducts: String
    val noCategory: String

    // ── Customer & Ledger Screen ────────────────────────────────────────────────
    val customersTitle: String
    val addCustomer: String
    val searchCustomers: String
    val phone: String
    val balance: String
    val creditDue: String
    val settleAccount: String
    val totalReceivable: String
    val totalPayable: String
    val netBalance: String
    val ledgerHistory: String
    val recordPayment: String
    val customerName: String
    val creditLimit: String
    val noCustomers: String
    val noLedgerEntries: String
    val debit: String
    val credit: String
    val editCustomer: String
    val deleteCustomer: String

    // ── Reports Screen ──────────────────────────────────────────────────────────
    val reportsTitle: String
    val salesSummary: String
    val totalRevenue: String
    val totalOrders: String
    val paymentMethods: String
    val cash: String
    val card: String
    val qrPayment: String
    val averageSale: String
    val vatCollected: String
    val grossProfit: String
    val topProducts: String
    val topCustomers: String
    val salesByPeriod: String
    val noSalesData: String
    val fiscalYearReport: String
    val quarterlyBreakdown: String
    val invoiceCount: String
    val revenue: String
    val vat: String
    val salesDetail: String
    val invoiceNumber: String
    val saleDate: String
    val saleItems: String
    val printReceipt: String
    val shareReceipt: String
    val productPerformance: String
    val unitsSold: String
    val revenueAmount: String
    val restockUrgency: String
    val daysLeft: String
    val allShifts: String
    val shiftHistory: String
    val openingCash: String
    val closingCash: String
    val variance: String
    val zReport: String
    val auditLog: String
    val auditEvent: String
    val auditUser: String
    val auditTime: String
    val filterByPeriod: String
    val today: String
    val yesterday: String
    val last7Days: String
    val last30Days: String
    val customRange: String
    val allTime: String
    val from: String
    val to: String

    // ── Settings Screen ─────────────────────────────────────────────────────────
    val settingsTitle: String
    val settingsSubtitle: String
    val profileCardTitle: String
    val storeCardTitle: String
    val staffCardTitle: String
    val salesCardTitle: String
    val inventoryCardTitle: String
    val hardwareCardTitle: String
    val securityCardTitle: String
    val backupCardTitle: String
    val saveSettings: String
    val logout: String
    val cancel: String
    val confirm: String
    val delete: String
    val edit: String
    val save: String
    val close: String
    val done: String
    val loading: String
    val error: String
    val success: String
    val warning: String
    val yes: String
    val no: String
    val storeName: String
    val storeAddress: String
    val receiptHeader: String
    val receiptFooter: String
    val taxId: String
    val taxMode: String
    val taxModeInclusive: String
    val taxModeExclusive: String
    val taxModeNone: String
    val defaultTaxRate: String
    val autoLockInterval: String
    val autoLogoutInterval: String
    val biometricLogin: String
    val biometricLoginDesc: String
    val changePin: String
    val backupEmail: String
    val lastBackupAt: String
    val backupNow: String
    val inviteStaff: String
    val inviteCode: String
    val inviteExpiry: String
    val manualSync: String
    val syncNow: String
    val syncStatus: String
    val lastSyncTime: String
    val printerSearch: String
    val printerConnect: String
    val printerConnected: String
    val printerDisconnected: String
    val testPrint: String
    val bluetoothPermissionRequired: String
    val fiscalYearManagement: String
    val addFiscalYear: String
    val closeFiscalYear: String
    val fiscalYearName: String
    val fiscalYearStart: String
    val fiscalYearEnd: String
    val fiscalYearActive: String
    val fiscalYearClosed: String
    val fiscalYearsGroup: String
    val manage: String
    val invoiceFormat: String
    val invoicesIssued: String
    val nextInvoicePreview: String
    val activePeriodSubtitle: String
    val createNewFiscalYear: String
    val fiscalYearInstruction: String
    val fiscalYearNameWithExample: String
    val backToList: String
    val saveFiscalYear: String
    val configuredFiscalYears: (count: Int) -> String
    val addFiscalYearShort: String
    val searchSettingsPlaceholder: String
    val editProfile: String
    val editStoreInfo: String
    val currencySymbol: String
    val taxCalculationMode: String
    val notSet: String
    val noActiveFiscalYear: String
    val notConfigured: String
    val nepaliBsCycle: String
    val firstInvoicePreview: String
    val invoicePrefixLabel: String
    val setActive: String
    val reopen: String
    val billsIssuedCount: (count: Long) -> String
    val staffMembersCount: (count: Int) -> String
    val acceptedPaymentMethods: String
    val cashPaymentMethod: String
    val cardPaymentMethod: String
    val qrPaymentMethod: String
    val creditPaymentMethod: String
    val checkoutPolicies: String
    val maxDiscountLimit: String
    val autoPrintReceiptSwitch: String
    val saveCheckoutSetup: String
    val lowStockThresholdLabel: String
    val negativeStockPolicy: String
    val stockBlock: String
    val stockWarn: String
    val stockAllow: String
    val saveInventoryRules: String
    val thermalPrinterTitle: String
    val printerSubtitle: String
    val scanPrinters: String
    val scanningPrinters: String
    val connectedPrinterTitle: String
    val printerPairedDisconnected: String
    val noPrinterConnected: String
    val pairPrinterHint: String
    val disconnect: String
    val pairDevice: String
    val connectNow: String
    val detectedPrintersCount: (count: Int) -> String
    val allBluetoothDevicesCount: (count: Int) -> String
    val showAllPairedDevices: String
    val showPrintersOnly: String
    val statusPaired: String
    val statusNearby: String
    val statusActive: String
    val btnReconnect: String
    val btnConnect: String
    val scanningNearbyPrinters: String
    val noPrintersFound: String
    val pairPrinterInstructions: String
    val bluetoothSettingsButton: String
    val printerAddressLabel: String
    val paperWidth: String
    val autoPrintCheckoutTitle: String
    val autoPrintCheckoutDesc: String
    val cashDrawerPulseTitle: String
    val cashDrawerPulseDesc: String
    val barcodeScannerSetup: String
    val barcodeScannerMode: String
    val kickDrawer: String
    val autoLockTimerTitle: String
    val autoLockTimerDesc: String
    val biometricUnlockTitle: String
    val biometricUnlockDesc: String
    val biometricLoginTitle: String
    val autoLogoutTimerTitle: String
    val autoLogoutTimerDesc: String
    val enforceStrongPinTitle: String
    val enforceStrongPinDesc: String
    val customTimer: String
    val customTimerWithInterval: (interval: String) -> String
    val saveSecuritySettings: String
    val customAutoLockTimer: String
    val customLockMinutesDesc: String
    val minutesExample: String
    val customAutoLogoutTimer: String
    val customLogoutMinutesDesc: String
    val logoutMinutesExample: String
    val auditLogsGroupTitle: String
    val auditTrailTitle: (count: Int) -> String
    val auditTrailDesc: String
    val viewAuditLogs: String
    val fullBackupTitle: String
    val fullBackupDesc: String
    val linkedBackupGmail: String
    val noBackupEmailLinked: String
    val linkGmail: String
    val changeGmail: String
    val sendBackupToGmail: String
    val exportLocalJson: String
    val restoreBackupJson: String
    val firestoreSyncGroupTitle: String
    val firestoreCollectionsTitle: String
    val firestoreSyncDesc: String
    val activeStoreIdLabel: String
    val syncStatusMessageLabel: String
    val pendingOutboxLabel: String
    val pendingRecordsCount: (count: Int) -> String
    val collectionsAutoSynced: String
    val syncAllCollections: String
    val fullCloudSync: String
    val logoutFromPos: String
    val editUserProfile: String
    val saveChanges: String
    val changeSecurityPin: String
    val changePinInstruction: String
    val currentPin: String
    val newPin: String
    val updatePin: String
    val securityAuditLogs: String
    val filterLogsPlaceholder: String
    val noAuditLogsFound: String
    val switchTillOperator: String
    val switchOperatorInstruction: String
    val operatorPin: String
    val verifyAndSwitch: String
    val editStoreProfile: String
    val saveStoreSettings: String
    val removeStaffTitle: String
    val removeStaffConfirm: (name: String, email: String) -> String
    val removeStaff: String
    val linkGmailTitle: String
    val linkGmailSubtitle: String
    val gmailAddress: String
    val useSignedInGoogleAccount: (email: String) -> String
    val saveAndLink: String
    val restoreDatabaseTitle: String
    val restoreDatabaseConfirmDesc: (storeName: String) -> String
    val backupMetadata: String
    val totalRecordsProceed: (count: Int) -> String
    val confirmAndRestore: String
    val restoring: String

    // ── Staff Member Form ───────────────────────────────────────────────────────
    val addStaffMember: String
    val addStaffSubtitle: String
    val staffFullName: String
    val staffEmail: String
    val staffPhone: String
    val staffRole: String
    val staffAccessPin: String
    val generatePin: String
    val roleCashierTitle: String
    val roleCashierBadge: String
    val roleCashierDesc: String
    val roleStorekeeperTitle: String
    val roleStorekeeperBadge: String
    val roleStorekeeperDesc: String
    val roleManagerTitle: String
    val roleManagerBadge: String
    val roleManagerDesc: String
    val roleAdminTitle: String
    val roleAdminBadge: String
    val roleAdminDesc: String
    val roleOwnerTitle: String
    val roleOwnerBadge: String
    val roleOwnerDesc: String
    val saveStaffButton: String
    val staffPersonalInfoSection: String
    val staffNamePlaceholder: String
    val staffNameHelper: String
    val staffEmailHelper: String
    val staffPhoneHelper: (label: String) -> String
    val staffRolesAvailable: (count: Int) -> String
    val staffPinLabel: String
    val staffPinPlaceholder: String
    val staffPinHideTip: String
    val staffPinShowTip: String
    val staffPinHelper: String
    val staffValidPinReady: String
    val staffPinUsage: String
    val staffRoleSubtitle: (role: String) -> String
    val staffNameErrorShort: String
    val staffEmailInvalid: String
    val staffEmailDuplicate: String
    val staffPinLengthError: String
    val staffAvatarFallback: String
    val staffAvatarEmailFallback: (pinLen: String) -> String

    // ── Shift Management ────────────────────────────────────────────────────────
    val shiftInProgress: String
    val shiftOpenedAt: (time: String) -> String
    val shiftTakeoverNeeded: String
    val continueShift: String
    val previousShiftOpen: String
    val previousShiftOpenDesc: (time: String) -> String
    val closePreviousShift: String
    val noOpenShift: String
    val previousClosingFloat: (amount: String) -> String
    val countOpeningCash: String
    val openShiftButton: String
    val terminalInUse: String
    val terminalInUseDesc: (cashier: String) -> String
    val shiftOpenElsewhere: String
    val shiftOpenElsewhereDesc: (terminal: String) -> String
    val cannotOpenShift: String
    val openShiftTitle: String
    val countCashInDrawer: String
    val openingCashLabel: String
    val differenceFromClosing: String
    val differenceAmount: String
    val reasonRequired: String
    val openingCashZeroConfirm: String
    val startShift: (amount: String) -> String
    val enterPinToTakeover: String
    val wrongPin: String

    // Active shift
    val activeShiftTitle: String
    val cashierTerminalSince: (cashier: String, terminal: String, time: String) -> String
    val unsyncedTransactions: (count: Int) -> String
    val salesByPaymentType: String
    val cashSales: String
    val onlineQrCard: String
    val creditSales: String
    val creditCollectionsCash: String
    val returnsSection: String
    val creditNotesIssued: String
    val cashRefunded: String
    val drawerSection: String
    val expectedDrawerCash: String
    val blindCloseHidden: String
    val cashMovementsSection: String
    val cashInOut: String
    val closeShift: String

    // Cash movement sheet
    val cashMovementTitle: String
    val amountRs: String
    val saveButton: String

    // Close shift wizard
    val closeShiftWizardTitle: (step: Int, total: Int, stepName: String) -> String
    val stepSummary: String
    val stepCountCash: String
    val stepOnlineCredit: String
    val stepReview: String
    val nextButton: String
    val backButton: String
    val blindCloseOn: String
    val openingCashRow: String
    val cashSalesRow: String
    val creditCollectionsCashRow: String
    val paidInRow: String
    val cashRefundsRow: String
    val paidOutRow: String
    val safeDropsRow: String
    val expectedCashRow: String
    val onlineQrCardSection: String
    val noOnlinePayments: String
    val otherSection: String
    val unsyncedTransactionsNote: (count: Int) -> String
    val countEveryNoteAndCoin: String
    val countedCashLabel: String
    val noOnlineToReconcile: String
    val posRecorded: String
    val providerConfirmed: String
    val noProviderConnection: String
    val amountReceivedRs: String
    val transactionReference: String
    val mismatch: String
    val creditNotesRefundsSection: String
    val cashRefundReduceDrawer: String
    val appliedToCustomerBalance: String
    val refundedViaBankWallet: String
    val creditNotesRedeemedAsTender: String
    val creditNoteDrawerNote: String
    val reviewExpected: String
    val reviewCounted: String
    val varianceShort: String
    val varianceOver: String
    val varianceExact: String
    val reasonForDifference: String
    val varianceAboveLimit: String
    val onlineMismatchesSection: String
    val none: String
    val cashHandoverSection: String
    val closingFloatLabel: String
    val depositToSafeBank: String
    val closingFloatNote: String

    // Shift closed
    val shiftClosedTitle: String
    val shiftFlagged: String
    val shiftApproved: String
    val shiftWaitingReview: String
    val countedCash: String
    val closingFloat: String
    val deposit: String
    val savedOfflineNote: String
    val shareZReport: String
    val doneButton: String

    // ── Admin Shift Screen ──────────────────────────────────────────────────────
    val adminShiftTitle: String
    val adminShiftSubtitle: String
    val shiftStatusAll: String
    val shiftStatusOpen: String
    val shiftStatusClosed: String
    val shiftStatusReconciled: String
    val shiftStatusFlagged: String
    val shiftStatusApproved: String
    val shiftStatusForced: String
    val shiftCashier: String
    val shiftTerminal: String
    val shiftOpened: String
    val shiftClosed2: String
    val shiftSales: String
    val shiftVariance2: String
    val approveShift: String
    val flagShift: String
    val forceClose: String
    val reopenShift: String
    val adjustShift: String
    val forceCloseReason: String
    val adjustmentAmount: String
    val adjustmentReason: String
    val noShiftsFound: String
    val shiftDetails: String
    val paymentBreakdown: String

    // ── Cashier Lock Dialog ──────────────────────────────────────────────────────
    val registerLocked: String
    val operatorLabel: (name: String) -> String
    val incorrectPin: String
    val unlockWithFingerprint: String
    val unlockTerminalTitle: String
    val unlockTerminalSubtitle: (name: String) -> String
    val unlockTerminalDesc: String
    val usePinButton: String

    // ── Role Switcher Dialog ─────────────────────────────────────────────────────
    val switchOperatorTitle: String
    val switchOperatorSubtitle: String
    val noOtherStaff: String
    val noOtherStaffDesc: String
    val selectOperatorPrompt: String
    val enterOperatorPin: (name: String) -> String
    val pinPlaceholder: String
    val defaultPinHint: String
    val confirmSwitch: String
    val pleaseEnterPin: String
    val currentBadge: String
    val registeredStoreStaff: String

    // ── Auth Screen ─────────────────────────────────────────────────────────────
    val welcomeTitle: String
    val welcomeSubtitle: String
    val signInWithGoogle: String
    val signInWithEmail: String
    val joinWithCode: String
    val emailAddress: String
    val password: String
    val forgotPassword: String
    val createAccount: String
    val signIn: String
    val orContinueWith: String
    val termsAndPrivacy: String
    val agreeToTerms: String
    val confirmDetails: String
    val fullName: String
    val optionalPhone: String
    val continueButton: String
    val createOrJoin: String
    val createMyBusiness: String
    val joinExistingBusiness: String
    val businessDetails: String
    val businessName: String
    val legalName: String
    val businessType: String
    val country: String
    val currency: String
    val timezone: String
    val firstBranchAndTill: String
    val branchName: String
    val branchAddress: String
    val tillName: String
    val reviewAndCreate: String
    val creatingBusiness: String
    val setPinAndBiometrics: String
    val enterPin: String
    val confirmPin: String
    val pinMismatch: String
    val enableBiometrics: String
    val orgPicker: String
    val selectOrganisation: String
    val invitationEntry: String
    val enterInvitationCode: String
    val redeemCode: String
    val accessRemoved: String
    val accessRemovedDesc: String
    val sessionExpired: String
    val resetPassword: String
    val resetPasswordSent: String

    // ── Common / Shared ─────────────────────────────────────────────────────────
    val search: String
    val filter: String
    val sort: String
    val refresh: String
    val print: String
    val share: String
    val export: String
    val import: String
    val add: String
    val remove: String
    val apply: String
    val clear: String
    val ok: String
    val back: String
    val next: String
    val submit: String
    val sending: String
    val connecting: String
    val disconnecting: String
    val online: String
    val offline: String
    val pending: String
    val active: String
    val inactive: String
    val status: String
    val date: String
    val time: String
    val amount: String
    val total: String
    val note: String
    val name: String
    val email: String
    val address: String
    val type: String
    val noData: String
    val tryAgain: String
    val permissionDenied: String
    val unknownError: String
    val accessDenied: String
    val restrictedScreen: (dest: String) -> String
    val roleNotAuthorized: (role: String) -> String
    val returnToAuthorizedArea: String
}

// ═══════════════════════════════════════════════════════════════════════════════
// ENGLISH STRINGS
// ═══════════════════════════════════════════════════════════════════════════════
object EnglishStrings : PosStrings {
    override val navRegister = "Register"
    override val navInventory = "Inventory"
    override val navCustomers = "Customers"
    override val navReports = "Reports"
    override val navSettings = "Settings"

    override val newSale = "New Sale"
    override val transactionComplete = "Transaction Complete"
    override val receiptReady = "Receipt generated & stock ledger updated"
    override val printEscPos = "Print ESC/POS"
    override val printBluetoothToast = "Bluetooth ESC/POS thermal printer triggered (58mm/80mm)"
    override val optional = "Optional"
    override val splashDbInit = "Initializing secure offline database..."
    override val splashVerifyingRbac = "Verifying security credentials & RBAC..."
    override val splashOrgReady = "Existing organization found. Ready."
    override val splashStoreSetupRequired = "Setup required for new store."
    override val splashSubtitle = "Next-Gen Retail POS & Multi-Tenant Terminal"
    override val splashBadge = "Role-Based Security • Offline SQLite Engine"
    override val splashFooter = "ENTERPRISE OFFLINE-FIRST ARCHITECTURE"

    override val shiftOpen = "Shift OPEN"
    override val shiftClosed = "Shift CLOSED"
    override val shiftClosingDraft = "Closing Draft"
    override val lockRegister = "Lock Terminal"
    override val switchOperator = "Switch Operator"
    override val syncOnline = "Online"
    override val syncOffline = "Offline"
    override val syncing = "Syncing..."
    override val tillInfo = { till: String, staff: String -> "Till $till • $staff" }

    override val language = "Language & Localization"
    override val languageSubtitle = "Select your preferred application display language"
    override val selectLanguage = "Choose Language"
    override val english = "English"
    override val nepali = "Nepali (नेपाली)"
    override val currentLanguageLabel = "Active Language"
    override val languageChangedNotice = "Language changed successfully!"
    override val switchLanguagePrompt = "Switch between English and Nepali anytime"

    override val searchProducts = "Search products..."
    override val allCategories = "All Categories"
    override val scanBarcode = "Scan Barcode"
    override val cartTitle = "Current Cart"
    override val clearCart = "Clear Cart"
    override val lineVoid = "Void Line"
    override val voidAll = "Void Ticket"
    override val scanOrSearchPrompt = "Scan barcode or search product name / SKU..."
    override val holdOrder = "Hold Order"
    override val emptyCartMessage = "Cart is empty. Tap items or scan barcodes to begin."
    override val subtotal = "Subtotal"
    override val discount = "Discount"
    override val tax = "Tax (VAT)"
    override val grandTotal = "Grand Total"
    override val payNow = "Charge & Checkout"
    override val quickCash = "Exact Cash"
    override val itemsInCart = { count: Int -> "$count items" }
    override val unitPrice = "Rate"
    override val qty = "Qty"
    override val stockLeft = { count: Int -> "$count left" }
    override val outOfStock = "Out of Stock"
    override val customer = "Customer"
    override val attachCustomer = "Attach Customer"
    override val removeCustomer = "Remove Customer"
    override val noCustomer = "No customer attached"
    override val parkedOrders = "Parked Orders"
    override val noParkedOrders = "No parked orders"
    override val resumeOrder = "Resume"
    override val deleteOrder = "Delete"
    override val parkedAt = { time: String -> "Parked at $time" }
    override val changeAmount = "Change"
    override val amountTendered = "Amount Tendered"
    override val change = "Change Due"
    override val checkout = "Checkout"
    override val paymentType = "Payment Method"
    override val splitPayment = "Split Payment"
    override val addPayment = "Add Payment"
    override val completePayment = "Complete Payment"
    override val totalToPay = { amount: String -> "Total to Pay: $amount" }
    override val cashTendered = { currency: String -> "Cash Tendered ($currency)" }
    override val quickCashLabel = "Quick Cash:"
    override val exactAmount = { currency: String, amount: String -> "Exact ($currency$amount)" }
    override val changeReturn = "Change Return: "
    override val remainingDue = "Remaining Due: "
    override val cancelButton = "Cancel"
    override val completeSale = "Complete Sale"
    override val cardTerminalReady = "Ready for EMV Chip / Contactless Tap"
    override val cardAmount = { amount: String -> "Amount: $amount" }
    override val qrWalletReady = "Dynamic UPI / Digital Wallet QR Ready"
    override val qrScanInstruction = "Customer scans QR code to verify payment"
    override val attachCustomerForCredit = "Please attach a customer for credit sale"
    override val attachCustomerButton = "Attach Customer"
    override val customerLabel = { name: String -> "Customer: $name" }
    override val creditLimitLabel = "Credit Limit:"
    override val existingDueLabel = "Existing Due:"
    override val availableCreditLabel = "Available Credit:"
    override val creditLimitExceeded = { available: String -> "⚠️ Credit Limit Exceeded! Available Limit: $available" }
    override val creditDueTab = "Credit Due"
    override val qrWalletTab = "QR / App"

    override val branchInventory = "Branch Inventory"
    override val receiveStock = "+ Receive Stock"
    override val lowStockAlert = "Low Stock"
    override val searchInventory = "Search items or barcodes..."
    override val costPrice = "Cost Price"
    override val sellingPrice = "Selling Price"
    override val reorderLevel = "Reorder Point"
    override val barcode = "Barcode"
    override val category = "Category"
    override val sku = "SKU"
    override val stockLevel = "Stock Level"
    override val addProduct = "Add Product"
    override val editProduct = "Edit Product"
    override val deleteProduct = "Delete Product"
    override val productName = "Product Name"
    override val productDetails = "Product Details"
    override val stockMovement = "Stock Movement"
    override val movementIn = "Stock In (Purchase)"
    override val movementOut = "Stock Out"
    override val movementAdjust = "Adjustment"
    override val movementWastage = "Wastage / Loss"
    override val reason = "Reason"
    override val quantity = "Quantity"
    override val noProducts = "No products found"
    override val noCategory = "Uncategorised"

    override val customersTitle = "Customer Ledger"
    override val addCustomer = "+ New Customer"
    override val searchCustomers = "Search by name or phone..."
    override val phone = "Phone"
    override val balance = "Balance"
    override val creditDue = "Credit Outstanding"
    override val settleAccount = "Settle Ledger"
    override val totalReceivable = "Total Receivable"
    override val totalPayable = "Total Payable"
    override val netBalance = "Net Balance"
    override val ledgerHistory = "Ledger History"
    override val recordPayment = "Record Payment"
    override val customerName = "Customer Name"
    override val creditLimit = "Credit Limit"
    override val noCustomers = "No customers yet"
    override val noLedgerEntries = "No ledger entries"
    override val debit = "Debit"
    override val credit = "Store Credit"
    override val editCustomer = "Edit Customer"
    override val deleteCustomer = "Delete Customer"

    override val reportsTitle = "Reports & Analytics"
    override val salesSummary = "Sales Summary"
    override val totalRevenue = "Total Revenue"
    override val totalOrders = "Completed Sales"
    override val paymentMethods = "Payment Breakdown"
    override val cash = "Cash"
    override val card = "Card"
    override val qrPayment = "QR Code"
    override val averageSale = "Average Sale"
    override val vatCollected = "VAT Collected"
    override val grossProfit = "Gross Profit"
    override val topProducts = "Top Products"
    override val topCustomers = "Top Customers"
    override val salesByPeriod = "Sales by Period"
    override val noSalesData = "No sales data for this period"
    override val fiscalYearReport = "Fiscal Year"
    override val quarterlyBreakdown = "Quarterly Breakdown"
    override val invoiceCount = "Invoices"
    override val revenue = "Revenue"
    override val vat = "VAT"
    override val salesDetail = "Sales Detail"
    override val invoiceNumber = "Invoice #"
    override val saleDate = "Date"
    override val saleItems = "Items"
    override val printReceipt = "Print Receipt"
    override val shareReceipt = "Share Receipt"
    override val productPerformance = "Product Performance"
    override val unitsSold = "Units Sold"
    override val revenueAmount = "Revenue"
    override val restockUrgency = "Restock Urgency"
    override val daysLeft = "Days Left"
    override val allShifts = "All Shifts"
    override val shiftHistory = "Shift History"
    override val openingCash = "Opening Cash"
    override val closingCash = "Closing Cash"
    override val variance = "Variance"
    override val zReport = "Z-Report"
    override val auditLog = "Audit Log"
    override val auditEvent = "Event"
    override val auditUser = "User"
    override val auditTime = "Time"
    override val filterByPeriod = "Filter by Period"
    override val today = "Today"
    override val yesterday = "Yesterday"
    override val last7Days = "Last 7 Days"
    override val last30Days = "Last 30 Days"
    override val customRange = "Custom Range"
    override val allTime = "All Time"
    override val from = "From"
    override val to = "To"

    override val settingsTitle = "Settings"
    override val settingsSubtitle = "पसल विवरण, भाषा, हार्डवेयर, सुरक्षा, र क्लाउड ब्याकअप व्यवस्थापन गर्नुहोस्"
    override val profileCardTitle = "हालको अपरेटर प्रोफाइल"
    override val storeCardTitle = "पसल र व्यवसाय विवरण"
    override val staffCardTitle = "कर्मचारी र काउन्टर अपरेटर व्यवस्थापन"
    override val salesCardTitle = "बिक्री र चेकआउट सेटिङ्स (Checkout)"
    override val inventoryCardTitle = "मौज्दात नियम र स्टक अलर्ट (Inventory)"
    override val hardwareCardTitle = "प्रिन्टर र POS हार्डवेयर (Hardware)"
    override val securityCardTitle = "सुरक्षा, PIN र अटो-लक (Security)"
    override val backupCardTitle = "क्लाउड र लोकल डाटा ब्याकअप (Backup)"
    override val saveSettings = "सेटिङ्स सेभ गर्नुहोस्"
    override val logout = "लगआउट गर्नुहोस्"
    override val cancel = "Cancel"
    override val confirm = "Confirm"
    override val delete = "Delete"
    override val edit = "Edit"
    override val save = "Save"
    override val close = "Close"
    override val done = "Done"
    override val loading = "Loading..."
    override val error = "Error"
    override val success = "Success"
    override val warning = "Warning"
    override val yes = "Yes"
    override val no = "No"
    override val storeName = "Store Name"
    override val storeAddress = "Store Address"
    override val receiptHeader = "Receipt Header"
    override val receiptFooter = "Receipt Footer"
    override val taxId = "Tax ID (PAN/VAT)"
    override val taxMode = "Tax Mode"
    override val taxModeInclusive = "Inclusive (price includes tax)"
    override val taxModeExclusive = "Exclusive (tax added on top)"
    override val taxModeNone = "None (no tax)"
    override val defaultTaxRate = "Default Tax Rate (%)"
    override val autoLockInterval = "Auto-Lock After"
    override val autoLogoutInterval = "Auto-Logout After"
    override val biometricLogin = "Biometric Login"
    override val biometricLoginDesc = "Use fingerprint or face to unlock the terminal"
    override val changePin = "Change PIN"
    override val backupEmail = "Backup Email"
    override val lastBackupAt = "Last Backup"
    override val backupNow = "Backup Now"
    override val inviteStaff = "Invite Staff"
    override val inviteCode = "Invitation Code"
    override val inviteExpiry = "Expires"
    override val manualSync = "Manual Sync"
    override val syncNow = "Sync Now"
    override val syncStatus = "Sync Status"
    override val lastSyncTime = "Last Synced"
    override val printerSearch = "Searching for printers..."
    override val printerConnect = "Connect"
    override val printerConnected = "जोडिएको छ (Connected)"
    override val printerDisconnected = "सम्पर्क टुट्यो (Disconnected)"
    override val testPrint = "नमुना प्रिन्ट (Test Print)"
    override val bluetoothPermissionRequired = "प्रिन्टर खोज्न ब्लुटुथ अनुमति आवश्यक छ"
    override val fiscalYearManagement = "आर्थिक वर्ष व्यवस्थापन (Fiscal Year)"
    override val addFiscalYear = "नयाँ आर्थिक वर्ष थप्नुहोस्"
    override val closeFiscalYear = "आर्थिक वर्ष बन्द गर्नुहोस्"
    override val fiscalYearName = "आर्थिक वर्षको नाम"
    override val fiscalYearStart = "सुरु मिति"
    override val fiscalYearEnd = "अन्त्य मिति"
    override val fiscalYearActive = "चालु (Active)"
    override val fiscalYearClosed = "बन्द (Closed)"
    override val fiscalYearsGroup = "आर्थिक वर्ष र इनभ्वाइसिङ (Invoicing)"
    override val manage = "व्यवस्थापन"
    override val invoiceFormat = "इनभ्वाइसको ढाँचा"
    override val invoicesIssued = "काटिएका बिलहरू"
    override val nextInvoicePreview = "अर्को बिल नम्बर"
    override val activePeriodSubtitle = "चालु अवधि र बिलको सिरिज"
    override val createNewFiscalYear = "नयाँ आर्थिक वर्ष सिर्जना गर्नुहोस्"
    override val fiscalYearInstruction = "नेपाली आर्थिक वर्ष (जस्तै २०८१/८२) र बिलको सिरिज प्रिफिक्स राख्नुहोस्।"
    override val fiscalYearNameWithExample = "आर्थिक वर्षको नाम (जस्तै २०८१/८२)"
    override val backToList = "सूचीमा फर्कनुहोस्"
    override val saveFiscalYear = "आर्थिक वर्ष सेभ गर्नुहोस्"
    override val configuredFiscalYears = { count: Int -> "तयार गरिएका आर्थिक वर्षहरू ($count)" }
    override val addFiscalYearShort = "+ आर्थिक वर्ष थप्नुहोस्"
    override val searchSettingsPlaceholder = "सेटिङ्स, हार्डवेयर, सुरक्षा खोज्नुहोस्..."
    override val editProfile = "प्रोफाइल सच्याउनुहोस्"
    override val editStoreInfo = "पसलको विवरण सच्याउनुहोस्"
    override val currencySymbol = "मुद्रा चिन्ह (Currency Symbol)"
    override val taxCalculationMode = "कर हिसाब गर्ने तरिका (Tax Mode)"
    override val notSet = "सेट गरिएको छैन"
    override val noActiveFiscalYear = "कुनै चालु आर्थिक वर्ष छैन"
    override val notConfigured = "सेटअप बाँकी"
    override val nepaliBsCycle = "नेपाली विक्रम संवत (साउन १ - असार मसान्त)"
    override val firstInvoicePreview = "पहिलो बिलको नमुना:"
    override val invoicePrefixLabel = "बिल प्रिफिक्स (जस्तै INV)"
    override val setActive = "चालु (Active) बनाउनुहोस्"
    override val reopen = "पुन: खोल्नुहोस् (Reopen)"
    override val billsIssuedCount = { count: Long -> "$count बिल काटियो" }
    override val staffMembersCount = { count: Int -> "कर्मचारीहरू ($count)" }
    override val acceptedPaymentMethods = "स्वीकृत भुक्तानी माध्यमहरू"
    override val cashPaymentMethod = "नगद र फिर्ता हिसाब (Cash Drawer)"
    override val cardPaymentMethod = "कार्ड मेसिन (Visa / Mastercard)"
    override val qrPaymentMethod = "डिजिटल क्युआर र वालेट (eSewa / Khalti)"
    override val creditPaymentMethod = "उधारो खाता (Store Credit)"
    override val checkoutPolicies = "बिलिङ नीति र नियमहरू"
    override val maxDiscountLimit = "क्यासियरले दिनपाउने अधिकतम छुट (%)"
    override val autoPrintReceiptSwitch = "बिल काटेपछि आफैँ प्रिन्ट गर्ने (Auto-Print)"
    override val saveCheckoutSetup = "चेकआउट सेटिङ्स सेभ गर्नुहोस्"
    override val lowStockThresholdLabel = "न्यून मौज्दात अलर्ट मात्रा (Low Stock Alert)"
    override val negativeStockPolicy = "सामान नहुँदा (Negative Stock) बिक्री नीति"
    override val stockBlock = "रोक्ने (BLOCK)"
    override val stockWarn = "चेतावनी (WARN)"
    override val stockAllow = "दिने (ALLOW)"
    override val saveInventoryRules = "इन्भेन्टरी नियमहरू सेभ गर्नुहोस्"
    override val thermalPrinterTitle = "थर्मल रिसिप्ट प्रिन्टर (Thermal Printer)"
    override val printerSubtitle = "ब्लुटुथ र USB OTG डाइरेक्ट ESC/POS"
    override val scanPrinters = "प्रिन्टर खोज्नुहोस् (Scan)"
    override val scanningPrinters = "खोज्दै..."
    override val connectedPrinterTitle = "जोडिएको थर्मल प्रिन्टर"
    override val printerPairedDisconnected = "प्रिन्टर पेयर भएको छ (तर जोडिएको छैन)"
    override val noPrinterConnected = "कुनै थर्मल प्रिन्टर जोडिएको छैन"
    override val pairPrinterHint = "मोबाइलको ब्लुटुथ सेटिङ्समा गएर प्रिन्टर पेयर गर्नुहोस्"
    override val disconnect = "विच्छेद गर्नुहोस् (Disconnect)"
    override val pairDevice = "पेयर गर्नुहोस् (Pair)"
    override val connectNow = "अहिले जोड्नुहोस् (Connect)"
    override val detectedPrintersCount = { count: Int -> "भेटिएका थर्मल प्रिन्टरहरू ($count):" }
    override val allBluetoothDevicesCount = { count: Int -> "सबै ब्लुटुथ डिभाइसहरू ($count):" }
    override val showAllPairedDevices = "सबै पेयर भएका डिभाइस देखाउनुहोस्"
    override val showPrintersOnly = "प्रिन्टर मात्र देखाउनुहोस्"
    override val statusPaired = "पेयर भएको"
    override val statusNearby = "नजिकै भएको"
    override val statusActive = "सक्रिय"
    override val btnReconnect = "पुन: जोड्नुहोस्"
    override val btnConnect = "जोड्नुहोस्"
    override val scanningNearbyPrinters = "वरपरका ब्लुटुथ थर्मल प्रिन्टरहरू खोज्दै..."
    override val noPrintersFound = "कुनै प्रिन्टर भेटिएन।"
    override val pairPrinterInstructions = "ब्लुटुथ सेटिङ्समा गएर प्रिन्टर पेयर गर्नुहोस्, अनि 'खोज्नुहोस्' थिच्नुहोस्।"
    override val bluetoothSettingsButton = "ब्लुटुथ सेटिङ्स खोल्नुहोस्"
    override val printerAddressLabel = "प्रिन्टरको नाम / MAC Address"
    override val paperWidth = "कागजको चौडाइ (Paper Width)"
    override val autoPrintCheckoutTitle = "अटो-प्रिन्ट (Auto-Print)"
    override val autoPrintCheckoutDesc = "बिक्री पूरा भएपछि आफैँ बिल प्रिन्ट गर्ने"
    override val cashDrawerPulseTitle = "क्यास ड्रअर खोल्ने (Pulse)"
    override val cashDrawerPulseDesc = "बिल छाप्दा RJ11 क्यास ड्रअर आफैँ खोल्ने"
    override val barcodeScannerSetup = "बारकोड स्क्यानर सेटअप"
    override val barcodeScannerMode = "बारकोड स्क्यानर इमुलेसन मोड (Emulation Mode)"
    override val kickDrawer = "क्यास ड्रअर खोल्नुहोस्"
    override val autoLockTimerTitle = "अटो-लक टाइमर (Auto-Lock Timer)"
    override val autoLockTimerDesc = "केही समय चलाइएन भने सुरक्षित लक स्क्रिनमा जाने"
    override val biometricUnlockTitle = "फिंगरप्रिन्ट अनलक (Biometric)"
    override val biometricUnlockDesc = "फिंगरप्रिन्ट वा फेस आइडीले रजिस्टर अनलक गर्ने"
    override val biometricLoginTitle = "फिंगरप्रिन्ट लगइन (Biometric Login)"
    override val autoLogoutTimerTitle = "लगइन सेसन टाइमआउट (Auto-Logout)"
    override val autoLogoutTimerDesc = "लामो समय नचलाउँदा आफैँ लगआउट हुने"
    override val enforceStrongPinTitle = "बलियो PIN अनिवार्य गर्ने"
    override val enforceStrongPinDesc = "कमजोर PIN (जस्तै 1234, 0000) राख्न नदिने"
    override val customTimer = "+ अनुकूल समय राख्नुहोस्"
    override val customTimerWithInterval = { interval: String -> "अनुकूल ($interval)" }
    override val saveSecuritySettings = "सुरक्षा सेटिङ्स सेभ गर्नुहोस्"
    override val customAutoLockTimer = "अनुकूल अटो-लक समय (Custom Auto-Lock)"
    override val customLockMinutesDesc = "कति मिनेट नचलाउँदा लक गर्ने, समय राख्नुहोस्:"
    override val minutesExample = "मिनेट (जस्तै ३, ७, १२)"
    override val customAutoLogoutTimer = "अनुकूल लगआउट समय (Custom Auto-Logout)"
    override val customLogoutMinutesDesc = "कति मिनेट नचलाउँदा लगआउट गर्ने, समय राख्नुहोस्:"
    override val logoutMinutesExample = "मिनेट (जस्तै ४५, ९०, १२०)"
    override val auditLogsGroupTitle = "सिस्टम अडिट र लगहरू (Audit Logs)"
    override val auditTrailTitle = { count: Int -> "अडिट ट्रेल र गतिविधि लग ($count)" }
    override val auditTrailDesc = "सुरक्षा रेकर्डहरू र सेटिङ्स परिवर्तनको लग"
    override val viewAuditLogs = "अडिट लग हेर्नुहोस्"
    override val fullBackupTitle = "सम्पूर्ण डाटाबेस ब्याकअप (Full Backup)"
    override val fullBackupDesc = "सबै सामान, ग्राहक, उधारो खाता, बिक्री रेकर्ड, र सेटिङ्सलाई जिमेलमा सुरक्षित पठाउनुहोस् वा रिस्टोर गर्नुहोस्।"
    override val linkedBackupGmail = "जोडिएको ब्याकअप जिमेल (Linked Gmail)"
    override val noBackupEmailLinked = "ब्याकअपको लागि कुनै जिमेल जोडिएको छैन"
    override val linkGmail = "जिमेल जोड्नुहोस्"
    override val changeGmail = "जिमेल परिवर्तन गर्नुहोस्"
    override val sendBackupToGmail = "जिमेलमा सुरक्षित ब्याकअप पठाउनुहोस्"
    override val exportLocalJson = "लोकल JSON ब्याकअप सेभ गर्नुहोस्"
    override val restoreBackupJson = "ब्याकअप JSON बाट रिस्टोर गर्नुहोस्"
    override val firestoreSyncGroupTitle = "क्लाउड फायरस्टोर सिंक्रोनाइजेसन (Cloud Sync)"
    override val firestoreCollectionsTitle = "फायरबेस फायरस्टोर संरचना (Firebase Schema)"
    override val firestoreSyncDesc = "सबै १९ कलेक्सनहरू (ग्राहक, सामान, बिक्री, खाता आदि) सुरक्षित रूपमा क्लाउड फायरस्टोरमा सिंक गर्नुहोस्।"
    override val activeStoreIdLabel = "चालु पसलको आइडी (Store ID):"
    override val syncStatusMessageLabel = "सिंक अवस्था (Sync Status):"
    override val pendingOutboxLabel = "बाँकी सिंक रेकर्ड (Pending Queue):"
    override val pendingRecordsCount = { count: Int -> "$count रेकर्ड" }
    override val collectionsAutoSynced = "१९ कलेक्सनहरू अटो-सिंक भए:"
    override val syncAllCollections = "सबै कलेक्सन फायरबेसमा सिंक गर्नुहोस्"
    override val fullCloudSync = "फुल क्लाउड सिंक (Full Sync)"
    override val logoutFromPos = "POS बाट लगआउट गर्नुहोस्"
    override val editUserProfile = "युजर प्रोफाइल सच्याउनुहोस्"
    override val saveChanges = "परिवर्तनहरू सेभ गर्नुहोस्"
    override val changeSecurityPin = "सुरक्षा PIN परिवर्तन गर्नुहोस्"
    override val changePinInstruction = "४ वा सोभन्दा बढी अङ्कको नयाँ PIN राख्नुहोस्। (1234 वा 0000 जस्ता सजिला PIN राख्न पाइँदैन।)"
    override val currentPin = "पुरानो PIN"
    override val newPin = "नयाँ PIN (कम्तिमा ४ अङ्क)"
    override val updatePin = "PIN अपडेट गर्नुहोस्"
    override val securityAuditLogs = "सुरक्षा र अडिट लगहरू"
    override val filterLogsPlaceholder = "लगहरू फिल्टर गर्नुहोस्..."
    override val noAuditLogsFound = "कुनै अडिट लग रेकर्ड भेटिएन।"
    override val switchTillOperator = "काउन्टर अपरेटर परिवर्तन गर्नुहोस्"
    override val switchOperatorInstruction = "आउने कर्मचारी छान्नुहोस् र उहाँको ४-अङ्कको PIN राख्नुहोस्:"
    override val operatorPin = "अपरेटरको PIN"
    override val verifyAndSwitch = "प्रमाणित गरी परिवर्तन गर्नुहोस्"
    override val editStoreProfile = "पसल विवरण सम्पादन"
    override val saveStoreSettings = "पसल सेटिङ्स सुरक्षित गर्नुहोस्"
    override val removeStaffTitle = "कर्मचारी हटाउने?"
    override val removeStaffConfirm = { name: String, email: String -> "के तपाईं निश्चित रूपमा $name ($email) लाई हटाउन चाहनुहुन्छ? उहाँको पहुँच तुरुन्त बन्द हुनेछ।" }
    override val removeStaff = "Remove Staff"
    override val linkGmailTitle = "Link Gmail for Cloud Backups"
    override val linkGmailSubtitle = "Enter the Gmail address where your encrypted store database backups will be sent."
    override val gmailAddress = "Gmail Address"
    override val useSignedInGoogleAccount = { email: String -> "Use Signed-in Google Account ($email)" }
    override val saveAndLink = "Save & Link"
    override val restoreDatabaseTitle = "Restore Database from Backup?"
    override val restoreDatabaseConfirmDesc = { storeName: String -> "You are about to restore data into store '$storeName'. This will merge and update records with the contents of this backup file." }
    override val backupMetadata = "Backup Metadata:"
    override val totalRecordsProceed = { count: Int -> "Total records to import: $count. Proceed?" }
    override val confirmAndRestore = "Confirm & Restore"
    override val restoring = "Restoring..."

    override val addStaffMember = "Add Staff Member"
    override val addStaffSubtitle = "Assign operator credentials & store access role"
    override val staffFullName = "Full Name"
    override val staffEmail = "Email Address"
    override val staffPhone = "Phone Number"
    override val staffRole = "Staff Role & Access Level"
    override val staffAccessPin = "Access PIN (4-6 digits)"
    override val generatePin = "Auto PIN"
    override val roleCashierTitle = "Cashier"
    override val roleCashierBadge = "Frontline POS"
    override val roleCashierDesc = "Checkout, sales, discounts & till operations"
    override val roleStorekeeperTitle = "Storekeeper"
    override val roleStorekeeperBadge = "Inventory"
    override val roleStorekeeperDesc = "Stock receiving, barcodes & inventory adjustments"
    override val roleManagerTitle = "Manager"
    override val roleManagerBadge = "Supervisor"
    override val roleManagerDesc = "Refunds, credit ledger, shift close & reports"
    override val roleAdminTitle = "Admin"
    override val roleAdminBadge = "Full Control"
    override val roleAdminDesc = "Full store settings, team control & cloud sync"
    override val roleOwnerTitle = "Co-Owner / सञ्चालक"
    override val roleOwnerBadge = "Owner Rights"
    override val roleOwnerDesc = "Unrestricted ownership, banking, licenses & store deletion"
    override val saveStaffButton = "Add Staff Member"
    override val staffPersonalInfoSection = "1. Personal Information / व्यक्तिगत विवरण"
    override val staffNamePlaceholder = "e.g. Som Thehit"
    override val staffNameHelper = "Official name shown on customer receipts & audits"
    override val staffEmailHelper = "Used for till operator login & invitation matching"
    override val staffPhoneHelper = { _: String -> "Mobile contact for SMS alerts & manager contact" }
    override val staffRolesAvailable = { count: Int -> "$count Roles Available" }
    override val staffPinLabel = "Numeric PIN (4 to 6 Digits) *"
    override val staffPinPlaceholder = "e.g. 1234 or 8921"
    override val staffPinHideTip = "Hide PIN"
    override val staffPinShowTip = "Show PIN"
    override val staffPinHelper = "Enter 4-6 digits for quick lock-screen unlocking"
    override val staffValidPinReady = "Valid PIN ready for operator switch"
    override val staffPinUsage = "Operators can quickly switch shifts, lock till during breaks, and sign sales using this PIN."
    override val staffRoleSubtitle = { role: String -> "Role: $role" }
    override val staffNameErrorShort = "Full name must be at least 2 characters"
    override val staffEmailInvalid = "Please enter a valid email address"
    override val staffEmailDuplicate = "A staff member with this email already exists"
    override val staffPinLengthError = "PIN must be between 4 and 6 digits"
    override val staffAvatarFallback = "New Operator"
    override val staffAvatarEmailFallback = { pinLen: String -> "Email not set • PIN: $pinLen" }

    // Shift Gate
    override val shiftInProgress = "Shift in progress"
    override val shiftOpenedAt = { time: String -> "Opened $time" }
    override val shiftTakeoverNeeded = "This shift was started on another device. PIN needed to take over."
    override val continueShift = "Continue shift"
    override val previousShiftOpen = "Previous shift is still open"
    override val previousShiftOpenDesc = { time: String -> "Opened $time. Close it before you can sell." }
    override val closePreviousShift = "Close previous shift"
    override val noOpenShift = "No open shift"
    override val previousClosingFloat = { amount: String -> "Previous closing float: $amount" }
    override val countOpeningCash = "Count your opening cash to start."
    override val openShiftButton = "Open shift"
    override val terminalInUse = "Terminal in use"
    override val terminalInUseDesc = { cashier: String -> "$cashier has an open shift on this terminal. Ask an admin for a handover." }
    override val shiftOpenElsewhere = "Shift open elsewhere"
    override val shiftOpenElsewhereDesc = { terminal: String -> "You have an open shift on terminal $terminal." }
    override val cannotOpenShift = "Cannot open a new shift"
    override val openShiftTitle = "Open shift"
    override val countCashInDrawer = "Count the cash that is in the drawer right now."
    override val openingCashLabel = "Opening cash"
    override val differenceFromClosing = "Difference from previous closing float"
    override val differenceAmount = "Difference"
    override val reasonRequired = "Reason (required)"
    override val openingCashZeroConfirm = "Opening cash is Rs 0. I confirm."
    override val startShift = { amount: String -> "Start shift ($amount)" }
    override val enterPinToTakeover = "Enter PIN to take over shift"
    override val wrongPin = "Wrong PIN."

    // Active Shift
    override val activeShiftTitle = "Shift OPEN"
    override val cashierTerminalSince = { cashier: String, terminal: String, time: String -> "$cashier · Terminal $terminal · since $time" }
    override val unsyncedTransactions = { count: Int -> "$count transactions not synced yet" }
    override val salesByPaymentType = "Sales by payment type"
    override val cashSales = "Cash sales"
    override val onlineQrCard = "Online / QR / card"
    override val creditSales = "Credit sales"
    override val creditCollectionsCash = "Credit collections (cash)"
    override val returnsSection = "Returns"
    override val creditNotesIssued = "Credit notes issued"
    override val cashRefunded = "Cash refunded (leaves drawer)"
    override val drawerSection = "Drawer"
    override val expectedDrawerCash = "Expected drawer cash"
    override val blindCloseHidden = "Expected cash is hidden until you submit your count (blind close)."
    override val cashMovementsSection = "Cash movements"
    override val cashInOut = "Cash in / out"
    override val closeShift = "Close shift"

    override val cashMovementTitle = "Cash movement"
    override val amountRs = "Amount (Rs)"
    override val saveButton = "Save"

    // Close shift wizard
    override val closeShiftWizardTitle = { step: Int, total: Int, stepName: String -> "Step $step of $total · $stepName" }
    override val stepSummary = "Summary"
    override val stepCountCash = "Count cash"
    override val stepOnlineCredit = "Online & credit notes"
    override val stepReview = "Review"
    override val nextButton = "Next"
    override val backButton = "Back"
    override val blindCloseOn = "Blind close is on: cash figures stay hidden until you submit your count."
    override val openingCashRow = "Opening cash"
    override val cashSalesRow = "+ Cash sales"
    override val creditCollectionsCashRow = "+ Credit collections (cash)"
    override val paidInRow = "+ Paid in"
    override val cashRefundsRow = "− Cash refunds"
    override val paidOutRow = "− Paid out"
    override val safeDropsRow = "− Safe drops"
    override val expectedCashRow = "Expected cash"
    override val onlineQrCardSection = "Online / QR / card (POS recorded)"
    override val noOnlinePayments = "No online payments this shift."
    override val otherSection = "Other"
    override val unsyncedTransactionsNote = { count: Int -> "$count transactions are not synced yet." }
    override val countEveryNoteAndCoin = "Count every note and coin in the drawer."
    override val countedCashLabel = "Counted cash"
    override val noOnlineToReconcile = "No online payments to reconcile."
    override val posRecorded = "POS recorded"
    override val providerConfirmed = "Provider confirmed (verified)"
    override val noProviderConnection = "No provider connection: your entry is saved as \"Declared (unverified)\"."
    override val amountReceivedRs = "Amount received (Rs)"
    override val transactionReference = "Transaction reference"
    override val mismatch = "Mismatch"
    override val creditNotesRefundsSection = "Credit notes & refunds (from system records)"
    override val cashRefundReduceDrawer = "Cash refunded (reduces drawer cash)"
    override val appliedToCustomerBalance = "Applied to customer balance"
    override val refundedViaBankWallet = "Refunded via bank / wallet"
    override val creditNotesRedeemedAsTender = "Credit notes redeemed as payment"
    override val creditNoteDrawerNote = "A credit note alone does not reduce the drawer. Only the cash refund does."
    override val reviewExpected = "Expected"
    override val reviewCounted = "Counted"
    override val varianceShort = "Variance (short)"
    override val varianceOver = "Variance (over)"
    override val varianceExact = "Variance"
    override val reasonForDifference = "Reason for the difference (required)"
    override val varianceAboveLimit = "This is above the allowed limit. The shift will be flagged for admin review."
    override val onlineMismatchesSection = "Online mismatches"
    override val none = "None"
    override val cashHandoverSection = "Cash handover"
    override val closingFloatLabel = "Closing float left in drawer (Rs)"
    override val depositToSafeBank = "Deposit to safe / bank"
    override val closingFloatNote = "The closing float becomes the next shift's expected opening cash."

    // Shift Closed
    override val shiftClosedTitle = "Shift closed"
    override val shiftFlagged = "Flagged for admin review."
    override val shiftApproved = "Approved."
    override val shiftWaitingReview = "Waiting for admin review."
    override val countedCash = "Counted cash"
    override val closingFloat = "Closing float"
    override val deposit = "Deposit"
    override val savedOfflineNote = "Saved on this device. It will sync when you are online."
    override val shareZReport = "Share / print Z-report"
    override val doneButton = "Done"

    // Admin Shift
    override val adminShiftTitle = "Shift Management"
    override val adminShiftSubtitle = "Review, approve, flag, or reconcile cashier shifts"
    override val shiftStatusAll = "All"
    override val shiftStatusOpen = "Open"
    override val shiftStatusClosed = "Closed"
    override val shiftStatusReconciled = "Reconciled"
    override val shiftStatusFlagged = "Flagged"
    override val shiftStatusApproved = "Approved"
    override val shiftStatusForced = "Force-Closed"
    override val shiftCashier = "Cashier"
    override val shiftTerminal = "Terminal"
    override val shiftOpened = "Opened"
    override val shiftClosed2 = "Closed"
    override val shiftSales = "Sales"
    override val shiftVariance2 = "Variance"
    override val approveShift = "Approve"
    override val flagShift = "Flag for Review"
    override val forceClose = "Force Close"
    override val reopenShift = "Reopen"
    override val adjustShift = "Adjust"
    override val forceCloseReason = "Reason for force closing"
    override val adjustmentAmount = "Adjustment Amount (Rs)"
    override val adjustmentReason = "Reason for adjustment"
    override val noShiftsFound = "No shifts found"
    override val shiftDetails = "Shift Details"
    override val paymentBreakdown = "Payment Breakdown"

    // Cashier Lock Dialog
    override val registerLocked = "Register Locked"
    override val operatorLabel = { name: String -> "Operator: $name" }
    override val incorrectPin = "Incorrect PIN (Default: 1234)"
    override val unlockWithFingerprint = "Unlock with Fingerprint"
    override val unlockTerminalTitle = "Unlock Terminal"
    override val unlockTerminalSubtitle = { name: String -> "Operator: $name" }
    override val unlockTerminalDesc = "Scan your fingerprint to unlock register"
    override val usePinButton = "Use PIN"

    // Role Switcher Dialog
    override val switchOperatorTitle = "Switch Operator"
    override val switchOperatorSubtitle = "Authenticated till handover"
    override val noOtherStaff = "No other team staff registered."
    override val noOtherStaffDesc = "You can add cashiers and managers in Settings > Team."
    override val selectOperatorPrompt = "Select operator to hand over this terminal:"
    override val enterOperatorPin = { name: String -> "Enter ${name}'s PIN" }
    override val pinPlaceholder = "4-digit PIN (Default: 1234)"
    override val defaultPinHint = "Default operator PIN is 1234"
    override val confirmSwitch = "Confirm Switch"
    override val pleaseEnterPin = "Please enter operator PIN."
    override val currentBadge = "CURRENT"
    override val registeredStoreStaff = "Registered Store Staff"

    // Auth
    override val welcomeTitle = "Welcome to OmniPOS"
    override val welcomeSubtitle = "Your complete point-of-sale solution"
    override val signInWithGoogle = "Sign in with Google"
    override val signInWithEmail = "Sign in with Email"
    override val joinWithCode = "Join with Invitation Code"
    override val emailAddress = "Email Address"
    override val password = "Password"
    override val forgotPassword = "Forgot Password?"
    override val createAccount = "Create Account"
    override val signIn = "Sign In"
    override val orContinueWith = "or continue with"
    override val termsAndPrivacy = "Terms & Privacy Policy"
    override val agreeToTerms = "I agree to the Terms of Service and Privacy Policy"
    override val confirmDetails = "Confirm your details"
    override val fullName = "Full Name"
    override val optionalPhone = "Phone (optional)"
    override val continueButton = "Continue"
    override val createOrJoin = "What do you want to do?"
    override val createMyBusiness = "Create my business"
    override val joinExistingBusiness = "Join an existing business"
    override val businessDetails = "Business details"
    override val businessName = "Business Name"
    override val legalName = "Legal Name (optional)"
    override val businessType = "Business Type"
    override val country = "Country"
    override val currency = "Currency"
    override val timezone = "Timezone"
    override val firstBranchAndTill = "First branch and till"
    override val branchName = "Branch Name"
    override val branchAddress = "Branch Address"
    override val tillName = "Till / Register Name"
    override val reviewAndCreate = "Review & Create"
    override val creatingBusiness = "Creating your business..."
    override val setPinAndBiometrics = "Set PIN & Biometrics"
    override val enterPin = "Enter PIN (4-6 digits)"
    override val confirmPin = "Confirm PIN"
    override val pinMismatch = "PINs do not match"
    override val enableBiometrics = "Enable biometric unlock"
    override val orgPicker = "Select Organisation"
    override val selectOrganisation = "Choose which business to open"
    override val invitationEntry = "Enter Invitation Code"
    override val enterInvitationCode = "Invitation code"
    override val redeemCode = "Redeem Code"
    override val accessRemoved = "Access Removed"
    override val accessRemovedDesc = "Your membership has been revoked or suspended. Contact the business owner."
    override val sessionExpired = "Session Expired"
    override val resetPassword = "Reset Password"
    override val resetPasswordSent = "Password reset email sent. Check your inbox."

    // Common
    override val search = "Search"
    override val filter = "Filter"
    override val sort = "Sort"
    override val refresh = "Refresh"
    override val print = "Print"
    override val share = "Share"
    override val export = "Export"
    override val import = "Import"
    override val add = "Add"
    override val remove = "Remove"
    override val apply = "Apply"
    override val clear = "Clear"
    override val ok = "OK"
    override val back = "Back"
    override val next = "Next"
    override val submit = "Submit"
    override val sending = "Sending..."
    override val connecting = "Connecting..."
    override val disconnecting = "Disconnecting..."
    override val online = "Online"
    override val offline = "Offline"
    override val pending = "Pending"
    override val active = "Active"
    override val inactive = "Inactive"
    override val status = "Status"
    override val date = "Date"
    override val time = "Time"
    override val amount = "Amount"
    override val total = "Total"
    override val note = "Note"
    override val name = "Name"
    override val email = "Email"
    override val address = "Address"
    override val type = "Type"
    override val noData = "No data available"
    override val tryAgain = "Try Again"
    override val permissionDenied = "Permission denied"
    override val unknownError = "An unknown error occurred"
    override val accessDenied = "Access Denied"
    override val restrictedScreen = { dest: String -> "Restricted Screen ($dest)" }
    override val roleNotAuthorized = { role: String -> "Your current role ($role) does not possess authorization to view this section. Please contact your Store Administrator or switch staff account." }
    override val returnToAuthorizedArea = "Return to Authorized Area"
}

// ═══════════════════════════════════════════════════════════════════════════════
// NEPALI STRINGS  (नेपाली)
// ═══════════════════════════════════════════════════════════════════════════════
object NepaliStrings : PosStrings {
    override val navRegister = "बिलिङ"
    override val navInventory = "मौज्दात"
    override val navCustomers = "ग्राहकहरू"
    override val navReports = "रिपोर्टहरू"
    override val navSettings = "सेटिङ्स"

    override val newSale = "नयाँ बिक्री सुरु गर्नुहोस्"
    override val transactionComplete = "कारोबार सम्पन्न भयो"
    override val receiptReady = "रसिद तयार भयो र मौज्दात खाता अद्यावधिक गरियो"
    override val printEscPos = "थर्मल प्रिन्ट (ESC/POS)"
    override val printBluetoothToast = "ब्लुटुथ थर्मल प्रिन्टर पठाइयो"
    override val optional = "ऐच्छिक"
    override val splashDbInit = "सुरक्षित अफलाइन डाटाबेस सुरु गर्दै..."
    override val splashVerifyingRbac = "सुरक्षा प्रमाण र पदहरू रुजु गर्दै..."
    override val splashOrgReady = "अवस्थित संस्था भेटियो। तयार छ।"
    override val splashStoreSetupRequired = "नयाँ पसलको लागि सेटअप आवश्यक छ।"
    override val splashSubtitle = "आधुनिक खुद्रा बिलिङ तथा बहु-शाखा पीओएस टर्मिनल"
    override val splashBadge = "पद-आधारित सुरक्षा • अफलाइन एसक्यूलाइट इन्जिन"
    override val splashFooter = "इन्टरप्राइज अफलाइन-फर्स्ट प्रणाली"

    override val shiftOpen = "शिफ्ट चालू"
    override val shiftClosed = "शिफ्ट बन्द"
    override val shiftClosingDraft = "बन्द प्रक्रिया"
    override val lockRegister = "काउन्टर लक"
    override val switchOperator = "कर्मचारी परिवर्तन"
    override val syncOnline = "अनलाइन"
    override val syncOffline = "अफलाइन"
    override val syncing = "सिङ्क हुँदैछ..."
    override val tillInfo = { till: String, staff: String -> "काउन्टर $till • $staff" }

    override val language = "भाषा र स्थानियकरण"
    override val languageSubtitle = "प्रणालीको प्रदर्शन भाषा छनौट गर्नुहोस्"
    override val selectLanguage = "भाषा छनौट गर्नुहोस्"
    override val english = "अंग्रेजी (English)"
    override val nepali = "नेपाली (Nepali)"
    override val currentLanguageLabel = "सक्रिय भाषा"
    override val languageChangedNotice = "भाषा सफलतापूर्वक परिवर्तन भयो!"
    override val switchLanguagePrompt = "अंग्रेजी र नेपाली भाषा बीच जुनसुकै बेला परिवर्तन गर्नुहोस्"

    override val searchProducts = "सामान खोज्नुहोस्..."
    override val allCategories = "सबै वर्गहरू"
    override val scanBarcode = "बारकोड स्क्यान"
    override val cartTitle = "बिल कार्ट"
    override val clearCart = "कार्ट खाली गर्नुहोस्"
    override val lineVoid = "लाइन खारेज"
    override val voidAll = "टिकट खारेज"
    override val scanOrSearchPrompt = "सामान थप्न बारकोड स्क्यान गर्नुहोस् वा नाम/SKU खोज्नुहोस्..."
    override val holdOrder = "अर्डर होल्ड"
    override val emptyCartMessage = "कार्ट खाली छ। सामान थप्न स्क्यान वा छनौट गर्नुहोस्।"
    override val subtotal = "उप-जम्मा"
    override val discount = "छुट"
    override val tax = "कर (VAT)"
    override val grandTotal = "कुल जम्मा"
    override val payNow = "भुक्तानी गर्नुहोस्"
    override val quickCash = "नगद भुक्तानी"
    override val itemsInCart = { count: Int -> "$count सामानहरू" }
    override val unitPrice = "दर"
    override val qty = "परिमाण"
    override val stockLeft = { count: Int -> "$count बाँकी" }
    override val outOfStock = "मौज्दात सकियो"
    override val customer = "ग्राहक"
    override val attachCustomer = "ग्राहक जोड्नुहोस्"
    override val removeCustomer = "ग्राहक हटाउनुहोस्"
    override val noCustomer = "कुनै ग्राहक जोडिएको छैन"
    override val parkedOrders = "होल्ड अर्डरहरू"
    override val noParkedOrders = "कुनै होल्ड अर्डर छैन"
    override val resumeOrder = "फिर्ता खोल्नुहोस्"
    override val deleteOrder = "मेटाउनुहोस्"
    override val parkedAt = { time: String -> "$time मा होल्ड गरिएको" }
    override val changeAmount = "फिर्ता रकम"
    override val amountTendered = "दिएको रकम"
    override val change = "फिर्ता पाइने"
    override val checkout = "भुक्तानी"
    override val paymentType = "भुक्तानी माध्यम"
    override val splitPayment = "विभाजित भुक्तानी"
    override val addPayment = "भुक्तानी थप्नुहोस्"
    override val completePayment = "भुक्तानी पूरा गर्नुहोस्"
    override val totalToPay = { amount: String -> "तिर्नुपर्ने कुल रकम: $amount" }
    override val cashTendered = { currency: String -> "प्राप्त नगद ($currency)" }
    override val quickCashLabel = "छिटो नगद:"
    override val exactAmount = { currency: String, amount: String -> "ठ्याक्कै ($currency$amount)" }
    override val changeReturn = "फिर्ता रकम: "
    override val remainingDue = "बाँकी लिनुपर्ने: "
    override val cancelButton = "रद्द गर्नुहोस्"
    override val completeSale = "बिक्री पूरा गर्नुहोस्"
    override val cardTerminalReady = "कार्ड पोस मेशिन तयार छ (EMV / Contactless)"
    override val cardAmount = { amount: String -> "रकम: $amount" }
    override val qrWalletReady = "डिजिटल क्युआर कोड तयार छ (Fonepay / Wallet)"
    override val qrScanInstruction = "ग्राहकले QR स्क्यान गरी भुक्तानी गर्न दिनुहोस्"
    override val attachCustomerForCredit = "उधारो बिक्रीको लागि ग्राहक छान्नुहोस्"
    override val attachCustomerButton = "ग्राहक छान्नुहोस्"
    override val customerLabel = { name: String -> "ग्राहक: $name" }
    override val creditLimitLabel = "ऋण सीमा:"
    override val existingDueLabel = "साविक बाँकी (Due):"
    override val availableCreditLabel = "उपलब्ध सिमा:"
    override val creditLimitExceeded = { available: String -> "⚠️ ऋण सीमा नाघियो! उपलब्ध सीमा: $available" }
    override val creditDueTab = "उधारो"
    override val qrWalletTab = "QR / वालेट"

    override val branchInventory = "शाखा मौज्दात"
    override val receiveStock = "+ स्टक दाखिला"
    override val lowStockAlert = "कम मौज्दात"
    override val searchInventory = "सामान वा बारकोड खोज्नुहोस्..."
    override val costPrice = "लागत मूल्य"
    override val sellingPrice = "बिक्री मूल्य"
    override val reorderLevel = "पुनःअर्डर सीमा"
    override val barcode = "बारकोड"
    override val category = "वर्ग"
    override val sku = "SKU कोड"
    override val stockLevel = "मौज्दात स्तर"
    override val addProduct = "सामान थप्नुहोस्"
    override val editProduct = "सामान सम्पादन"
    override val deleteProduct = "सामान मेटाउनुहोस्"
    override val productName = "सामानको नाम"
    override val productDetails = "सामानको विवरण"
    override val stockMovement = "मौज्दात आवागमन"
    override val movementIn = "स्टक आगमन (खरिद)"
    override val movementOut = "स्टक निर्गमन"
    override val movementAdjust = "मौज्दात समायोजन"
    override val movementWastage = "नोक्सान / बर्बादी"
    override val reason = "कारण"
    override val quantity = "मात्रा"
    override val noProducts = "कुनै सामान भेटिएन"
    override val noCategory = "वर्गीकरण नगरिएको"

    override val customersTitle = "ग्राहक खाता"
    override val addCustomer = "+ नयाँ ग्राहक"
    override val searchCustomers = "ग्राहकको नाम वा फोन खोज्नुहोस्..."
    override val phone = "फोन"
    override val balance = "बाँकी रकम"
    override val creditDue = "उधारो रकम"
    override val settleAccount = "हिसाब चुक्ता"
    override val totalReceivable = "कुल पाउनुपर्ने"
    override val totalPayable = "कुल दिनुपर्ने"
    override val netBalance = "खुद मौज्दात"
    override val ledgerHistory = "खाता इतिहास"
    override val recordPayment = "भुक्तानी दर्ता"
    override val customerName = "ग्राहकको नाम"
    override val creditLimit = "उधारो सीमा"
    override val noCustomers = "कुनै ग्राहक छैनन्"
    override val noLedgerEntries = "कुनै खाता प्रविष्टि छैन"
    override val debit = "डेबिट"
    override val credit = "उधारो"
    override val editCustomer = "ग्राहक सम्पादन"
    override val deleteCustomer = "ग्राहक मेटाउनुहोस्"

    // Cashier Lock Dialog
    override val registerLocked = "काउन्टर लक"
    override val operatorLabel = { name: String -> "कर्मचारी: $name" }
    override val incorrectPin = "गलत PIN (पूर्वनिर्धारित: 1234)"
    override val unlockWithFingerprint = "औठाछाप मार्फत खोल्नुहोस्"
    override val unlockTerminalTitle = "काउन्टर अनलक"
    override val unlockTerminalSubtitle = { name: String -> "कर्मचारी: $name" }
    override val unlockTerminalDesc = "काउन्टर खोल्न औठाछाप स्क्यान गर्नुहोस्"
    override val usePinButton = "PIN प्रयोग गर्नुहोस्"

    // Role Switcher Dialog
    override val switchOperatorTitle = "कर्मचारी परिवर्तन"
    override val switchOperatorSubtitle = "प्रमाणित काउन्टर हस्तान्तरण"
    override val noOtherStaff = "अन्य कुनै कर्मचारी दर्ता छैन।"
    override val noOtherStaffDesc = "सेटिङ्स > टिममा क्यासियर र व्यवस्थापक थप्न सकिन्छ।"
    override val selectOperatorPrompt = "यो काउन्टर हस्तान्तरण गर्न कर्मचारी छनौट गर्नुहोस्:"
    override val enterOperatorPin = { name: String -> "${name}को PIN हाल्नुहोस्" }
    override val pinPlaceholder = "४ अङ्कको PIN (पूर्वनिर्धारित: 1234)"
    override val defaultPinHint = "पूर्वनिर्धारित कर्मचारी PIN: 1234"
    override val confirmSwitch = "परिवर्तन पुष्टि गर्नुहोस्"
    override val pleaseEnterPin = "कर्मचारीको PIN हाल्नुहोस्।"
    override val currentBadge = "हाल"
    override val registeredStoreStaff = "दर्ता गरिएको कर्मचारी"

    override val reportsTitle = "कारोबार रिपोर्ट"
    override val salesSummary = "बिक्री सारांश"
    override val totalRevenue = "कुल आम्दानी"
    override val totalOrders = "सम्पन्न बिक्री"
    override val paymentMethods = "भुक्तानी माध्यम विवरण"
    override val cash = "नगद"
    override val card = "कार्ड"
    override val qrPayment = "क्युआर (QR)"
    override val averageSale = "औसत बिक्री"
    override val vatCollected = "संकलित VAT"
    override val grossProfit = "कुल नाफा"
    override val topProducts = "सर्वाधिक बिक्री सामान"
    override val topCustomers = "प्रमुख ग्राहकहरू"
    override val salesByPeriod = "अवधिअनुसार बिक्री"
    override val noSalesData = "यस अवधिमा कुनै बिक्री छैन"
    override val fiscalYearReport = "आर्थिक वर्ष"
    override val quarterlyBreakdown = "त्रैमासिक विवरण"
    override val invoiceCount = "बिजक संख्या"
    override val revenue = "आम्दानी"
    override val vat = "VAT"
    override val salesDetail = "बिक्री विवरण"
    override val invoiceNumber = "बिजक नं."
    override val saleDate = "मिति"
    override val saleItems = "सामानहरू"
    override val printReceipt = "रसिद छाप्नुहोस्"
    override val shareReceipt = "रसिद पठाउनुहोस्"
    override val productPerformance = "सामान प्रदर्शन"
    override val unitsSold = "बिक्री एकाइ"
    override val revenueAmount = "आम्दानी"
    override val restockUrgency = "पुन: भण्डारण आवश्यकता"
    override val daysLeft = "बाँकी दिन"
    override val allShifts = "सबै शिफ्ट"
    override val shiftHistory = "शिफ्ट इतिहास"
    override val openingCash = "प्रारम्भिक नगद"
    override val closingCash = "अन्तिम नगद"
    override val variance = "फरक"
    override val zReport = "Z-रिपोर्ट"
    override val auditLog = "अडिट लग"
    override val auditEvent = "घटना"
    override val auditUser = "कर्मचारी"
    override val auditTime = "समय"
    override val filterByPeriod = "अवधिले छान्नुहोस्"
    override val today = "आज"
    override val yesterday = "हिजो"
    override val last7Days = "७ दिन"
    override val last30Days = "३० दिन"
    override val customRange = "आफ्नै अवधि"
    override val allTime = "सबै"
    override val from = "देखि"
    override val to = "सम्म"

    override val settingsTitle = "प्रणाली सेटिङ्स"
    override val settingsSubtitle = "पसल, भाषा, हार्डवेयर, सुरक्षा र ब्याकअप व्यवस्थापन"
    override val profileCardTitle = "सक्रिय कर्मचारी विवरण"
    override val storeCardTitle = "पसल तथा व्यापार विवरण"
    override val staffCardTitle = "कर्मचारी व्यवस्थापन"
    override val salesCardTitle = "बिक्री र बिलिङ नियमहरू"
    override val inventoryCardTitle = "मौज्दात नियम तथा अलर्टहरू"
    override val hardwareCardTitle = "प्रिन्टर तथा स्क्यानर हार्डवेयर"
    override val securityCardTitle = "सुरक्षा, पिन कोड र स्वतः लक"
    override val backupCardTitle = "क्लाउड तथा स्थानीय डाटा ब्याकअप"
    override val saveSettings = "सेटिङ्स सुरक्षित गर्नुहोस्"
    override val logout = "लगआउट"
    override val cancel = "रद्द गर्नुहोस्"
    override val confirm = "निश्चित गर्नुहोस्"
    override val delete = "मेटाउनुहोस्"
    override val edit = "सम्पादन"
    override val save = "सुरक्षित गर्नुहोस्"
    override val close = "बन्द गर्नुहोस्"
    override val done = "सम्पन्न"
    override val loading = "लोड हुँदैछ..."
    override val error = "त्रुटि"
    override val success = "सफल"
    override val warning = "सावधानी"
    override val yes = "हो"
    override val no = "होइन"
    override val storeName = "पसलको नाम"
    override val storeAddress = "पसलको ठेगाना"
    override val receiptHeader = "रसिद शीर्षक"
    override val receiptFooter = "रसिद पादटीका"
    override val taxId = "कर नम्बर (PAN/VAT)"
    override val taxMode = "कर प्रकार"
    override val taxModeInclusive = "समावेशी (मूल्यमा कर जोडिएको)"
    override val taxModeExclusive = "बाह्य (मूल्यमाथि कर थपिन्छ)"
    override val taxModeNone = "कर छैन"
    override val defaultTaxRate = "पूर्वनिर्धारित कर दर (%)"
    override val autoLockInterval = "स्वतः लक अवधि"
    override val autoLogoutInterval = "स्वतः लगआउट अवधि"
    override val biometricLogin = "बायोमेट्रिक लगिन"
    override val biometricLoginDesc = "काउन्टर अनलक गर्न औंठाछाप वा अनुहार प्रयोग गर्नुहोस्"
    override val changePin = "पिन परिवर्तन"
    override val backupEmail = "ब्याकअप इमेल"
    override val lastBackupAt = "अन्तिम ब्याकअप"
    override val backupNow = "अहिलै ब्याकअप"
    override val inviteStaff = "कर्मचारी आमन्त्रण"
    override val inviteCode = "आमन्त्रण कोड"
    override val inviteExpiry = "म्याद"
    override val manualSync = "म्यानुअल सिङ्क"
    override val syncNow = "अहिलै सिङ्क गर्नुहोस्"
    override val syncStatus = "सिङ्क अवस्था"
    override val lastSyncTime = "अन्तिम सिङ्क"
    override val printerSearch = "प्रिन्टर खोज्दैछ..."
    override val printerConnect = "जोड्नुहोस्"
    override val printerConnected = "जोडिएको"
    override val printerDisconnected = "विच्छेद"
    override val testPrint = "परीक्षण प्रिन्ट"
    override val bluetoothPermissionRequired = "प्रिन्टर खोज्न ब्लुटुथ अनुमति आवश्यक छ"
    override val fiscalYearManagement = "आर्थिक वर्ष व्यवस्थापन"
    override val addFiscalYear = "आर्थिक वर्ष थप्नुहोस्"
    override val closeFiscalYear = "आर्थिक वर्ष बन्द गर्नुहोस्"
    override val fiscalYearName = "आर्थिक वर्षको नाम"
    override val fiscalYearStart = "शुरू मिति"
    override val fiscalYearEnd = "अन्त्य मिति"
    override val fiscalYearActive = "सक्रिय"
    override val fiscalYearClosed = "बन्द"
    override val fiscalYearsGroup = "आर्थिक वर्ष र बिलिङ (Fiscal Years)"
    override val manage = "व्यवस्थापन"
    override val invoiceFormat = "बिल ढाँचा (Invoice Pattern)"
    override val invoicesIssued = "जारी बिल संख्या"
    override val nextInvoicePreview = "अर्को बिल (Next Invoice)"
    override val activePeriodSubtitle = "आ.व. र बिलिङ नम्बरिङ नियन्त्रण"
    override val createNewFiscalYear = "नयाँ आर्थिक वर्ष सिर्जना गर्नुहोस्"
    override val fiscalYearInstruction = "नेपाली आ.व. ढाँचा (उदाहरण: 2084/85) र बिल उपसर्ग (Prefix) प्रविष्ट गर्नुहोस्।"
    override val fiscalYearNameWithExample = "आर्थिक वर्षको नाम (e.g. 2084/85)"
    override val backToList = "फर्कनुहोस्"
    override val saveFiscalYear = "सुरक्षित गर्नुहोस्"
    override val configuredFiscalYears = { count: Int -> "कन्फिगर गरिएका आर्थिक वर्षहरू ($count)" }
    override val addFiscalYearShort = "+ नयाँ आ.व."
    override val searchSettingsPlaceholder = "सेटिङ्स, हार्डवेयर, सुरक्षा खोज्नुहोस्..."
    override val editProfile = "प्रोफाइल सम्पादन"
    override val editStoreInfo = "पसल विवरण सम्पादन"
    override val currencySymbol = "मुद्रा चिन्ह"
    override val taxCalculationMode = "कर गणना विधि"
    override val notSet = "सेट गरिएको छैन"
    override val noActiveFiscalYear = "कुनै सक्रिय आ.व. छैन"
    override val notConfigured = "कन्फिगर गरिएको छैन"
    override val nepaliBsCycle = "नेपाली वि.सं. चक्र (साउन १ - असार मसान्त)"
    override val firstInvoicePreview = "पहिलो बिल पूर्वावलोकन:"
    override val invoicePrefixLabel = "बिल उपसर्ग (Prefix जस्तै INV)"
    override val setActive = "सक्रिय बनाउनुहोस्"
    override val reopen = "पुनः खोल्नुहोस्"
    override val billsIssuedCount = { count: Long -> "$count जारी गरिएका बिलहरू" }
    override val staffMembersCount = { count: Int -> "कर्मचारीहरू ($count)" }
    override val acceptedPaymentMethods = "स्वीकृत भुक्तानी विधिहरू"
    override val cashPaymentMethod = "नगद भुक्तानी र फिर्ता हिसाब (Cash Drawer)"
    override val cardPaymentMethod = "कार्ड टर्मिनल (Visa / Mastercard)"
    override val qrPaymentMethod = "डिजिटल क्यूआर र वालेटहरू (eSewa / Khalti / Fonepay)"
    override val creditPaymentMethod = "उधारो खाता (Customer Due / Khata Ledger)"
    override val checkoutPolicies = "चेकआउट नीति र रसिद नोटहरू"
    override val maxDiscountLimit = "अधिकतम क्यासियर छुट सीमा (%)"
    override val autoPrintReceiptSwitch = "बिक्रीपछि स्वतः रसिद प्रिन्ट गर्नुहोस्"
    override val saveCheckoutSetup = "चेकआउट सेटिङ्स सुरक्षित गर्नुहोस्"
    override val lowStockThresholdLabel = "न्यूनतम मौज्दात अलर्ट सीमा (एकाइ संख्या)"
    override val negativeStockPolicy = "ऋणात्मक मौज्दात बिक्री नीति (Negative Stock)"
    override val stockBlock = "रोक्नुहोस् (बिक्री निषेध)"
    override val stockWarn = "सचेत गर्नुहोस् (चेतावनी)"
    override val stockAllow = "अनुमति दिनुहोस्"
    override val saveInventoryRules = "मौज्दात नियम सुरक्षित गर्नुहोस्"
    override val thermalPrinterTitle = "थर्मल रसिद प्रिन्टर"
    override val printerSubtitle = "ब्लुटुथ SPP र USB OTG डायरेक्ट ESC/POS"
    override val scanPrinters = "उपकरण खोज्नुहोस्"
    override val scanningPrinters = "खोज्दैछ..."
    override val connectedPrinterTitle = "थर्मल प्रिन्टर जोडिएको छ"
    override val printerPairedDisconnected = "प्रिन्टर पेयर भएको (विच्छेद)"
    override val noPrinterConnected = "कुनै प्रिन्टर जोडिएको छैन"
    override val pairPrinterHint = "एन्ड्रोइड ब्लुटुथ सेटिङ्समा थर्मल प्रिन्टर जोडा मिलाउनुहोस्"
    override val disconnect = "विच्छेद गर्नुहोस्"
    override val pairDevice = "उपकरण जोड्नुहोस्"
    override val connectNow = "अहिलै जोड्नुहोस्"
    override val detectedPrintersCount = { count: Int -> "पत्ता लागेका थर्मल प्रिन्टरहरू ($count):" }
    override val allBluetoothDevicesCount = { count: Int -> "सबै ब्लुटुथ उपकरणहरू ($count):" }
    override val showAllPairedDevices = "सबै जोडिएका उपकरण देखाउनुहोस्"
    override val showPrintersOnly = "प्रिन्टरहरू मात्र देखाउनुहोस्"
    override val statusPaired = "जोडिएको"
    override val statusNearby = "नजिकै"
    override val statusActive = "सक्रिय"
    override val btnReconnect = "पुनः जोड्नुहोस्"
    override val btnConnect = "जोड्नुहोस्"
    override val scanningNearbyPrinters = "नजिकैका ब्लुटुथ थर्मल प्रिन्टरहरू खोज्दैछ..."
    override val noPrintersFound = "कुनै प्रिन्टर फेला परेन।"
    override val pairPrinterInstructions = "ब्लुटुथ सेटिङ्समा प्रिन्टर जोड्नुहोस्, अनि स्क्यान थिच्नुहोस्।"
    override val bluetoothSettingsButton = "ब्लुटुथ सेटिङ्स"
    override val printerAddressLabel = "सक्रिय प्रिन्टर नाम / MAC ठेगाना"
    override val paperWidth = "रोल कागजको चौडाइ"
    override val autoPrintCheckoutTitle = "चेकआउटमा स्वतः प्रिन्ट"
    override val autoPrintCheckoutDesc = "बिक्री सम्पन्न हुनासाथ स्वतः रसिद छाप्नुहोस्"
    override val cashDrawerPulseTitle = "क्यास ड्रअर स्वतः खोल्ने पल्स"
    override val cashDrawerPulseDesc = "चेकआउट गर्दा RJ11 ड्रअर पिन ट्रिगर गर्नुहोस्"
    override val barcodeScannerSetup = "बारकोड स्क्यानर सेटअप"
    override val barcodeScannerMode = "बारकोड स्क्यानर इम्युलेसन मोड"
    override val kickDrawer = "घर्रा खोल्नुहोस्"
    override val autoLockTimerTitle = "टर्मिनल स्वतः लक समय"
    override val autoLockTimerDesc = "केही समय प्रयोग नभए काउन्टरलाई द्रुत-पिन स्क्रिनमा लक गर्नुहोस्"
    override val biometricUnlockTitle = "बायोमेट्रिक टर्मिनल अनलक"
    override val biometricUnlockDesc = "उपकरणको औंठाछाप सेन्सरद्वारा काउन्टर अनलक गर्नुहोस्"
    override val biometricLoginTitle = "बायोमेट्रिक लगिन प्रमाणीकरण"
    override val autoLogoutTimerTitle = "लगिन सेसन समय समाप्ति (स्वतः लगआउट)"
    override val autoLogoutTimerDesc = "धेरै समय निष्क्रिय रहेमा स्वतः लगिन स्क्रिनमा फर्कनुहोस्"
    override val enforceStrongPinTitle = "बलियो र फरक पिन अनिवार्य गर्नुहोस्"
    override val enforceStrongPinDesc = "कमजोर पूर्वनिर्धारित पिनहरू (जस्तै १२३४, ००००) निषेध गर्नुहोस्"
    override val customTimer = "+ अनुकूलित समय"
    override val customTimerWithInterval = { interval: String -> "अनुकूलित ($interval)" }
    override val saveSecuritySettings = "सुरक्षा सेटिङ्स सुरक्षित गर्नुहोस्"
    override val customAutoLockTimer = "अनुकूलित स्वतः लक समय"
    override val customLockMinutesDesc = "काउन्टर लक हुनुअघि निष्क्रिय रहने मिनेट प्रविष्ट गर्नुहोस्:"
    override val minutesExample = "मिनेट (जस्तै ३, ७, १२)"
    override val customAutoLogoutTimer = "अनुकूलित लगिन सेसन समय"
    override val customLogoutMinutesDesc = "स्वतः लगआउट हुनुअघि निष्क्रिय रहने मिनेट प्रविष्ट गर्नुहोस्:"
    override val logoutMinutesExample = "मिनेट (जस्तै ४५, ९०, १२०)"
    override val auditLogsGroupTitle = "प्रणाली अडिट तथा लगहरू"
    override val auditTrailTitle = { count: Int -> "अडिट तथा गतिविधि लग ($count)" }
    override val auditTrailDesc = "डाटाबेस सुरक्षा रेकर्ड र सेटिङ्स लगहरू"
    override val viewAuditLogs = "लगहरू हेर्नुहोस्"
    override val fullBackupTitle = "पसलको सम्पूर्ण डाटाबेस ब्याकअप"
    override val fullBackupDesc = "सबै उत्पादन, बारकोड, ग्राहक, उधारो खाता, बिक्री इतिहास, शिफ्ट र सेटिङ्स सुरक्षित जिमेलमा पठाउनुहोस् वा पुनःस्थापना गर्नुहोस्।"
    override val linkedBackupGmail = "जोडिएको ब्याकअप जिमेल"
    override val noBackupEmailLinked = "क्लाउड ब्याकअपका लागि अहिलेसम्म कुनै जिमेल जोडिएको छैन"
    override val linkGmail = "जिमेल जोड्नुहोस्"
    override val changeGmail = "जिमेल परिवर्तन"
    override val sendBackupToGmail = "जिमेलमा सुरक्षित ब्याकअप पठाउनुहोस्"
    override val exportLocalJson = "स्थानीय JSON ब्याकअप निकाल्नुहोस्"
    override val restoreBackupJson = "JSON ब्याकअपबाट पुनःस्थापना"
    override val firestoreSyncGroupTitle = "क्लाउड फायरस्टोर डाटाबेस सिङ्क्रोनाइजेसन"
    override val firestoreCollectionsTitle = "फायरबेस फायरस्टोर संरचना र संग्रहहरू"
    override val firestoreSyncDesc = "सबै १९ वटा संग्रहहरू (ग्राहक, उत्पादन, बिक्री, खाता, शिफ्ट, स्टक आदि) क्लाउड फायरस्टोरमा सिङ्क गर्नुहोस्।"
    override val activeStoreIdLabel = "सक्रिय पसल आइडी:"
    override val syncStatusMessageLabel = "सिङ्क स्थिति सन्देश:"
    override val pendingOutboxLabel = "बाँकी सिङ्क क्यु:"
    override val pendingRecordsCount = { count: Int -> "$count रेकर्ड(हरू)" }
    override val collectionsAutoSynced = "१९ संग्रहहरू स्वतः सिङ्क:"
    override val syncAllCollections = "सबै संग्रह फायरबेसमा सिङ्क गर्नुहोस्"
    override val fullCloudSync = "पूर्ण क्लाउड सिङ्क"
    override val logoutFromPos = "POS बाट लगआउट गर्नुहोस्"
    override val editUserProfile = "कर्मचारी प्रोफाइल सम्पादन"
    override val saveChanges = "परिवर्तनहरू सुरक्षित गर्नुहोस्"
    override val changeSecurityPin = "सुरक्षा पिन परिवर्तन"
    override val changePinInstruction = "कम्तीमा ४ अंकको फरक सुरक्षा पिन छान्नुहोस्। '1234' वा '0000' जस्ता कमजोर पिन निषेध छन्।"
    override val currentPin = "हालको पिन"
    override val newPin = "नयाँ पिन (कम्तीमा ४ अंक)"
    override val updatePin = "पिन अद्यावधिक गर्नुहोस्"
    override val securityAuditLogs = "सुरक्षा तथा अडिट लगहरू"
    override val filterLogsPlaceholder = "कार्य वा कर्मचारी अनुसार खोज्नुहोस्..."
    override val noAuditLogsFound = "कुनै अडिट लग भेटिएन।"
    override val switchTillOperator = "काउन्टर अपरेटर परिवर्तन"
    override val switchOperatorInstruction = "आउने कर्मचारी छान्नुहोस् र उहाँको ४ अंकको पिन प्रविष्ट गर्नुहोस्:"
    override val operatorPin = "अपरेटर पिन"
    override val verifyAndSwitch = "जाँच गरी परिवर्तन गर्नुहोस्"
    override val editStoreProfile = "पसल विवरण सम्पादन"
    override val saveStoreSettings = "पसल सेटिङ्स सुरक्षित गर्नुहोस्"
    override val removeStaffTitle = "कर्मचारी हटाउने?"
    override val removeStaffConfirm = { name: String, email: String -> "के तपाईं निश्चित रूपमा $name ($email) लाई हटाउन चाहनुहुन्छ? उहाँको पहुँच तुरुन्त बन्द हुनेछ।" }
    override val removeStaff = "कर्मचारी हटाउनुहोस्"
    override val linkGmailTitle = "क्लाउड ब्याकअपका लागि जिमेल जोड्नुहोस्"
    override val linkGmailSubtitle = "इन्क्रिप्टेड स्टोर डाटाबेस ब्याकअप पठाइने जिमेल ठेगाना प्रविष्ट गर्नुहोस्।"
    override val gmailAddress = "जिमेल ठेगाना"
    override val useSignedInGoogleAccount = { email: String -> "लगइन भएको गुगल खाता प्रयोग गर्नुहोस् ($email)" }
    override val saveAndLink = "सुरक्षित गरी जोड्नुहोस्"
    override val restoreDatabaseTitle = "ब्याकअपबाट डाटा पुनःस्थापना गर्ने?"
    override val restoreDatabaseConfirmDesc = { storeName: String -> "तपाईं '$storeName' पसलमा डाटा पुनःस्थापना गर्न लाग्नुभएको छ। यसले यस ब्याकअप फाइलका सामग्रीहरू थप र अद्यावधिक गर्नेछ।" }
    override val backupMetadata = "ब्याकअप विवरण:"
    override val totalRecordsProceed = { count: Int -> "आयात गरिने कुल रेकर्डहरू: $count। अघि बढ्ने?" }
    override val confirmAndRestore = "पुष्टि गरी पुनःस्थापना गर्नुहोस्"
    override val restoring = "पुनःस्थापना हुँदैछ..."

    override val addStaffMember = "नयाँ कर्मचारी थप्नुहोस्"
    override val addStaffSubtitle = "कर्मचारीको विवरण र जिम्मेवारी पद निर्धारण गर्नुहोस्"
    override val staffFullName = "पूरा नाम"
    override val staffEmail = "इमेल ठेगाना"
    override val staffPhone = "सम्पर्क फोन नम्बर"
    override val staffRole = "कर्मचारीको पद र जिम्मेवारी"
    override val staffAccessPin = "पहुँच पिन (४-६ अंक)"
    override val generatePin = "स्वतः पिन"
    override val roleCashierTitle = "क्यासियर"
    override val roleCashierBadge = "काउन्टर बिक्री"
    override val roleCashierDesc = "काउन्टर बिलिङ, छुट, रसिद र नगद कारोबार"
    override val roleStorekeeperTitle = "स्टोरकिपर"
    override val roleStorekeeperBadge = "मौज्दात / सामान"
    override val roleStorekeeperDesc = "स्टक दाखिला, बारकोड र मौज्दात गणना"
    override val roleManagerTitle = "प्रबन्धक"
    override val roleManagerBadge = "सुपरभाइजर"
    override val roleManagerDesc = "फिर्ता, उधारो खाता, शिफ्ट बन्द र रिपोर्ट"
    override val roleAdminTitle = "एडमिन"
    override val roleAdminBadge = "पूर्ण नियन्त्रण"
    override val roleAdminDesc = "सम्पूर्ण सेटिङ्स, कर्मचारी र ब्याकअप नियन्त्रण"
    override val roleOwnerTitle = "सह-सञ्चालक / मालिक"
    override val roleOwnerBadge = "मालिक अधिकार"
    override val roleOwnerDesc = "पूर्ण स्वामित्व, बैंकिङ, इजाजतपत्र र पसल व्यवस्थापन"
    override val saveStaffButton = "कर्मचारी सुरक्षित गर्नुहोस्"
    override val staffPersonalInfoSection = "१. व्यक्तिगत विवरण"
    override val staffNamePlaceholder = "जस्तै: रमेश थापा"
    override val staffNameHelper = "ग्राहक रसिद र अडिट रिपोर्टमा देखिने आधिकारिक नाम"
    override val staffEmailHelper = "काउन्टर लगइन र निमन्त्रणाको लागि प्रयोग गरिने"
    override val staffPhoneHelper = { _: String -> "एसएमएस अलर्ट र प्रबन्धक सम्पर्कको लागि मोबाइल नम्बर" }
    override val staffRolesAvailable = { count: Int -> "$count पदहरू उपलब्ध" }
    override val staffPinLabel = "अङ्कीय पिन (४ देखि ६ अङ्क) *"
    override val staffPinPlaceholder = "जस्तै: १२३४ वा ८९२१"
    override val staffPinHideTip = "पिन लुकाउनुहोस्"
    override val staffPinShowTip = "पिन देखाउनुहोस्"
    override val staffPinHelper = "काउन्टर तत्काल अनलक गर्न ४-६ अङ्क राख्नुहोस्"
    override val staffValidPinReady = "मान्य पिन तयार छ"
    override val staffPinUsage = "कर्मचारीहरूले यो पिन प्रयोग गरी सिफ्ट फेर्न, खाजाको समयमा काउन्टर लक गर्न र बिक्री सम्पन्न गर्न सक्नेछन्।"
    override val staffRoleSubtitle = { role: String -> "पद: $role" }
    override val staffNameErrorShort = "पूरा नाम कम्तीमा २ अक्षरको हुनुपर्छ"
    override val staffEmailInvalid = "कृपया मान्य इमेल ठेगाना प्रविष्ट गर्नुहोस्"
    override val staffEmailDuplicate = "यो इमेल भएको कर्मचारी पहिले नै दर्ता छ"
    override val staffPinLengthError = "पिन ४ देखि ६ अङ्क बीचको हुनुपर्छ"
    override val staffAvatarFallback = "नयाँ कर्मचारी"
    override val staffAvatarEmailFallback = { pinLen: String -> "इमेल सेट गरिएको छैन • पिन: $pinLen" }

    // Shift Gate
    override val shiftInProgress = "शिफ्ट चालू छ"
    override val shiftOpenedAt = { time: String -> "$time मा खोलिएको" }
    override val shiftTakeoverNeeded = "यो शिफ्ट अर्को उपकरणमा सुरु भएको थियो। जिम्मा लिन पिन आवश्यक छ।"
    override val continueShift = "शिफ्ट जारी राख्नुहोस्"
    override val previousShiftOpen = "अघिल्लो शिफ्ट अझै खुला छ"
    override val previousShiftOpenDesc = { time: String -> "$time मा खोलिएको। बिक्री गर्नु अघि बन्द गर्नुहोस्।" }
    override val closePreviousShift = "अघिल्लो शिफ्ट बन्द गर्नुहोस्"
    override val noOpenShift = "खुला शिफ्ट छैन"
    override val previousClosingFloat = { amount: String -> "अघिल्लो बन्द नगद: $amount" }
    override val countOpeningCash = "शुरू गर्न आफ्नो प्रारम्भिक नगद गन्नुहोस्।"
    override val openShiftButton = "शिफ्ट खोल्नुहोस्"
    override val terminalInUse = "काउन्टर अर्कोले प्रयोग गर्दैछ"
    override val terminalInUseDesc = { cashier: String -> "$cashier को यस काउन्टरमा खुला शिफ्ट छ। हस्तान्तरणका लागि एडमिनलाई सोध्नुहोस्।" }
    override val shiftOpenElsewhere = "शिफ्ट अन्यत्र खुला छ"
    override val shiftOpenElsewhereDesc = { terminal: String -> "तपाईंको शिफ्ट काउन्टर $terminal मा खुला छ।" }
    override val cannotOpenShift = "नयाँ शिफ्ट खोल्न सकिँदैन"
    override val openShiftTitle = "शिफ्ट खोल्नुहोस्"
    override val countCashInDrawer = "अहिले दराजमा भएको नगद गन्नुहोस्।"
    override val openingCashLabel = "प्रारम्भिक नगद"
    override val differenceFromClosing = "अघिल्लो बन्द नगदसँग फरक"
    override val differenceAmount = "फरक रकम"
    override val reasonRequired = "कारण (अनिवार्य)"
    override val openingCashZeroConfirm = "प्रारम्भिक नगद रु. ० छ। म निश्चित गर्छु।"
    override val startShift = { amount: String -> "शिफ्ट शुरू गर्नुहोस् ($amount)" }
    override val enterPinToTakeover = "शिफ्ट जिम्मा लिन पिन हाल्नुहोस्"
    override val wrongPin = "पिन गलत छ।"

    // Active Shift
    override val activeShiftTitle = "शिफ्ट चालू"
    override val cashierTerminalSince = { cashier: String, terminal: String, time: String -> "$cashier · काउन्टर $terminal · $time देखि" }
    override val unsyncedTransactions = { count: Int -> "$count कारोबार सिङ्क भएका छैनन्" }
    override val salesByPaymentType = "भुक्तानी प्रकारअनुसार बिक्री"
    override val cashSales = "नगद बिक्री"
    override val onlineQrCard = "अनलाइन / QR / कार्ड"
    override val creditSales = "उधारो बिक्री"
    override val creditCollectionsCash = "उधारो असुली (नगद)"
    override val returnsSection = "फिर्ता"
    override val creditNotesIssued = "जारी गरिएका क्रेडिट नोट"
    override val cashRefunded = "फिर्ता दिइएको नगद (दराजबाट)"
    override val drawerSection = "दराज"
    override val expectedDrawerCash = "अपेक्षित दराज नगद"
    override val blindCloseHidden = "अन्ध बन्दमा नगद रकम गोप्य राखिन्छ। गणना पेश गरेपछि मात्र देखिन्छ।"
    override val cashMovementsSection = "नगद आवागमन"
    override val cashInOut = "नगद आगमन / निर्गमन"
    override val closeShift = "शिफ्ट बन्द गर्नुहोस्"

    override val cashMovementTitle = "नगद आवागमन"
    override val amountRs = "रकम (रु.)"
    override val saveButton = "सुरक्षित गर्नुहोस्"

    // Close shift wizard
    override val closeShiftWizardTitle = { step: Int, total: Int, stepName: String -> "चरण $step / $total · $stepName" }
    override val stepSummary = "सारांश"
    override val stepCountCash = "नगद गन्नुहोस्"
    override val stepOnlineCredit = "अनलाइन र क्रेडिट नोट"
    override val stepReview = "समीक्षा"
    override val nextButton = "अर्को"
    override val backButton = "पछाडि"
    override val blindCloseOn = "अन्ध बन्द सक्रिय छ: नगद रकम गणना पेश नगरेसम्म लुकाइन्छ।"
    override val openingCashRow = "प्रारम्भिक नगद"
    override val cashSalesRow = "+ नगद बिक्री"
    override val creditCollectionsCashRow = "+ उधारो असुली (नगद)"
    override val paidInRow = "+ भित्र नगद"
    override val cashRefundsRow = "− नगद फिर्ता"
    override val paidOutRow = "− बाहिर नगद"
    override val safeDropsRow = "− सेफमा राखिएको"
    override val expectedCashRow = "अपेक्षित नगद"
    override val onlineQrCardSection = "अनलाइन / QR / कार्ड (काउन्टर दर्ता)"
    override val noOnlinePayments = "यस शिफ्टमा कुनै अनलाइन भुक्तानी छैन।"
    override val otherSection = "अन्य"
    override val unsyncedTransactionsNote = { count: Int -> "$count कारोबार अझै सिङ्क भएका छैनन्।" }
    override val countEveryNoteAndCoin = "दराजमा भएका प्रत्येक नोट र सिक्का गन्नुहोस्।"
    override val countedCashLabel = "गनिएको नगद"
    override val noOnlineToReconcile = "मिलान गर्नुपर्ने कुनै अनलाइन भुक्तानी छैन।"
    override val posRecorded = "काउन्टरमा दर्ता"
    override val providerConfirmed = "प्रदायकबाट पुष्टि (प्रमाणित)"
    override val noProviderConnection = "प्रदायक सम्पर्क छैन: तपाईंको प्रविष्टि 'घोषित (अप्रमाणित)' रूपमा सुरक्षित हुनेछ।"
    override val amountReceivedRs = "प्राप्त रकम (रु.)"
    override val transactionReference = "कारोबार सन्दर्भ"
    override val mismatch = "मेल खाएन"
    override val creditNotesRefundsSection = "क्रेडिट नोट र फिर्ता (प्रणाली रेकर्डबाट)"
    override val cashRefundReduceDrawer = "नगद फिर्ता (दराज नगद घटाउँछ)"
    override val appliedToCustomerBalance = "ग्राहक मौज्दातमा लागू"
    override val refundedViaBankWallet = "बैंक / वालेटबाट फिर्ता"
    override val creditNotesRedeemedAsTender = "भुक्तानीका रूपमा भुनाइएका क्रेडिट नोट"
    override val creditNoteDrawerNote = "क्रेडिट नोटले मात्रै दराज घटाउँदैन। केवल नगद फिर्ताले दराज घटाउँछ।"
    override val reviewExpected = "अपेक्षित"
    override val reviewCounted = "गनिएको"
    override val varianceShort = "फरक (कम)"
    override val varianceOver = "फरक (बढी)"
    override val varianceExact = "फरक"
    override val reasonForDifference = "फरकको कारण (अनिवार्य)"
    override val varianceAboveLimit = "यो अनुमत सीमाभन्दा बढी छ। शिफ्ट एडमिन समीक्षाका लागि चिह्नित हुनेछ।"
    override val onlineMismatchesSection = "अनलाइन मेल नखाएका"
    override val none = "कुनै छैन"
    override val cashHandoverSection = "नगद हस्तान्तरण"
    override val closingFloatLabel = "दराजमा बाँकी राख्ने नगद (रु.)"
    override val depositToSafeBank = "सेफ / बैंकमा जम्मा"
    override val closingFloatNote = "बन्द नगद अर्को शिफ्टको प्रारम्भिक अपेक्षित नगद हुनेछ।"

    // Shift Closed
    override val shiftClosedTitle = "शिफ्ट बन्द भयो"
    override val shiftFlagged = "एडमिन समीक्षाका लागि चिह्नित।"
    override val shiftApproved = "स्वीकृत।"
    override val shiftWaitingReview = "एडमिन समीक्षाको प्रतीक्षामा।"
    override val countedCash = "गनिएको नगद"
    override val closingFloat = "बाँकी नगद"
    override val deposit = "जम्मा रकम"
    override val savedOfflineNote = "यस उपकरणमा सुरक्षित। अनलाइन हुँदा सिङ्क हुनेछ।"
    override val shareZReport = "Z-रिपोर्ट साझा / छाप्नुहोस्"
    override val doneButton = "सम्पन्न"

    // Admin Shift
    override val adminShiftTitle = "शिफ्ट व्यवस्थापन"
    override val adminShiftSubtitle = "क्यासियर शिफ्ट समीक्षा, स्वीकृत, चिह्नित वा मिलान गर्नुहोस्"
    override val shiftStatusAll = "सबै"
    override val shiftStatusOpen = "खुला"
    override val shiftStatusClosed = "बन्द"
    override val shiftStatusReconciled = "मिलान भएको"
    override val shiftStatusFlagged = "चिह्नित"
    override val shiftStatusApproved = "स्वीकृत"
    override val shiftStatusForced = "जबरजस्ती बन्द"
    override val shiftCashier = "क्यासियर"
    override val shiftTerminal = "काउन्टर"
    override val shiftOpened = "खोलिएको"
    override val shiftClosed2 = "बन्द भएको"
    override val shiftSales = "बिक्री"
    override val shiftVariance2 = "फरक"
    override val approveShift = "स्वीकृत गर्नुहोस्"
    override val flagShift = "समीक्षाका लागि चिह्नित"
    override val forceClose = "जबरजस्ती बन्द"
    override val reopenShift = "फेरि खोल्नुहोस्"
    override val adjustShift = "समायोजन"
    override val forceCloseReason = "जबरजस्ती बन्द गर्नुको कारण"
    override val adjustmentAmount = "समायोजन रकम (रु.)"
    override val adjustmentReason = "समायोजनको कारण"
    override val noShiftsFound = "कुनै शिफ्ट भेटिएन"
    override val shiftDetails = "शिफ्ट विवरण"
    override val paymentBreakdown = "भुक्तानी विवरण"

    // Auth
    override val welcomeTitle = "OmniPOS मा स्वागत छ"
    override val welcomeSubtitle = "तपाईंको सम्पूर्ण बिक्री समाधान"
    override val signInWithGoogle = "Google मार्फत साइन इन"
    override val signInWithEmail = "इमेलबाट साइन इन"
    override val joinWithCode = "आमन्त्रण कोडबाट जोड्नुहोस्"
    override val emailAddress = "इमेल ठेगाना"
    override val password = "पासवर्ड"
    override val forgotPassword = "पासवर्ड बिर्सनुभयो?"
    override val createAccount = "खाता बनाउनुहोस्"
    override val signIn = "साइन इन"
    override val orContinueWith = "वा यसबाट जारी राख्नुहोस्"
    override val termsAndPrivacy = "सेवाका शर्त र गोपनीयता नीति"
    override val agreeToTerms = "म सेवाका शर्त र गोपनीयता नीतिसँग सहमत छु"
    override val confirmDetails = "विवरण पुष्टि गर्नुहोस्"
    override val fullName = "पूरा नाम"
    override val optionalPhone = "फोन (ऐच्छिक)"
    override val continueButton = "जारी राख्नुहोस्"
    override val createOrJoin = "तपाईं के गर्न चाहनुहुन्छ?"
    override val createMyBusiness = "आफ्नो व्यापार खोल्नुहोस्"
    override val joinExistingBusiness = "अवस्थित व्यापारमा सामेल हुनुहोस्"
    override val businessDetails = "व्यापार विवरण"
    override val businessName = "व्यापारको नाम"
    override val legalName = "कानुनी नाम (ऐच्छिक)"
    override val businessType = "व्यापार प्रकार"
    override val country = "देश"
    override val currency = "मुद्रा"
    override val timezone = "समय क्षेत्र"
    override val firstBranchAndTill = "पहिलो शाखा र काउन्टर"
    override val branchName = "शाखाको नाम"
    override val branchAddress = "शाखाको ठेगाना"
    override val tillName = "काउन्टरको नाम"
    override val reviewAndCreate = "समीक्षा र सिर्जना"
    override val creatingBusiness = "तपाईंको व्यापार बनाउँदैछ..."
    override val setPinAndBiometrics = "पिन र बायोमेट्रिक सेट गर्नुहोस्"
    override val enterPin = "पिन हाल्नुहोस् (४-६ अंक)"
    override val confirmPin = "पिन पुष्टि गर्नुहोस्"
    override val pinMismatch = "पिन मेल खाएन"
    override val enableBiometrics = "बायोमेट्रिक अनलक सक्षम गर्नुहोस्"
    override val orgPicker = "संस्था छनौट गर्नुहोस्"
    override val selectOrganisation = "कुन व्यापार खोल्ने छनौट गर्नुहोस्"
    override val invitationEntry = "आमन्त्रण कोड हाल्नुहोस्"
    override val enterInvitationCode = "आमन्त्रण कोड"
    override val redeemCode = "कोड भुनाउनुहोस्"
    override val accessRemoved = "पहुँच हटाइयो"
    override val accessRemovedDesc = "तपाईंको सदस्यता खारेज वा निलम्बित गरिएको छ। व्यापार मालिकलाई सम्पर्क गर्नुहोस्।"
    override val sessionExpired = "सत्र समाप्त"
    override val resetPassword = "पासवर्ड रिसेट"
    override val resetPasswordSent = "पासवर्ड रिसेट इमेल पठाइयो। आफ्नो इनबक्स जाँच्नुहोस्।"

    // Common
    override val search = "खोज्नुहोस्"
    override val filter = "छान्नुहोस्"
    override val sort = "क्रमबद्ध गर्नुहोस्"
    override val refresh = "ताजा गर्नुहोस्"
    override val print = "छाप्नुहोस्"
    override val share = "साझा गर्नुहोस्"
    override val export = "निर्यात"
    override val import = "आयात"
    override val add = "थप्नुहोस्"
    override val remove = "हटाउनुहोस्"
    override val apply = "लागू गर्नुहोस्"
    override val clear = "खाली गर्नुहोस्"
    override val ok = "ठिक छ"
    override val back = "पछाडि"
    override val next = "अर्को"
    override val submit = "पेश गर्नुहोस्"
    override val sending = "पठाउँदैछ..."
    override val connecting = "जोड्दैछ..."
    override val disconnecting = "विच्छेद गर्दैछ..."
    override val online = "अनलाइन"
    override val offline = "अफलाइन"
    override val pending = "प्रतीक्षित"
    override val active = "सक्रिय"
    override val inactive = "निष्क्रिय"
    override val status = "अवस्था"
    override val date = "मिति"
    override val time = "समय"
    override val amount = "रकम"
    override val total = "जम्मा"
    override val note = "टिप्पणी"
    override val name = "नाम"
    override val email = "इमेल"
    override val address = "ठेगाना"
    override val type = "प्रकार"
    override val noData = "कुनै डाटा उपलब्ध छैन"
    override val tryAgain = "पुनः प्रयास गर्नुहोस्"
    override val permissionDenied = "अनुमति अस्वीकृत"
    override val unknownError = "अज्ञात त्रुटि भयो"
    override val accessDenied = "पहुँच अस्वीकृत"
    override val restrictedScreen = { dest: String -> "निषेधित खण्ड ($dest)" }
    override val roleNotAuthorized = { role: String -> "तपाईंको हालको पद ($role) लाई यो खण्ड हेर्ने अनुमति छैन। कृपया स्टोर एडमिनसँग सम्पर्क गर्नुहोस् वा कर्मचारी परिवर्तन गर्नुहोस्।" }
    override val returnToAuthorizedArea = "अनुमति भएको खण्डमा फर्कनुहोस्"
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }
val LocalPosStrings = compositionLocalOf<PosStrings> { EnglishStrings }
