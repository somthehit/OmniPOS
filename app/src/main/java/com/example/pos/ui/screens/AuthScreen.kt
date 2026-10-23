package com.example.pos.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.pos.ui.components.PosButtonGradients
import com.example.pos.ui.components.PosGradientButton
import com.example.pos.ui.components.PosGoogleButton
import com.example.pos.ui.components.PosOutlineButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.data.model.StaffRole
import com.example.pos.data.model.TaxMode
import com.example.pos.ui.AuthMode
import com.example.pos.ui.PosViewModel
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosPrimary
import com.example.ui.theme.PosRose
import com.example.ui.theme.PosSky
import com.example.ui.theme.Slate900
import androidx.compose.material.icons.filled.Fingerprint
import com.example.pos.util.BiometricHelper
import com.example.pos.util.findFragmentActivity
import com.example.pos.data.local.UserEntity
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import com.example.pos.ui.i18n.AppLanguage

data class CurrencyOption(
    val code: String,
    val name: String,
    val symbol: String
) {
    val displayLabel: String get() = "$code - $name ($symbol)"
}

val supportedCurrencies = listOf(
    CurrencyOption("NPR", "Nepalese Rupee", "Rs."),
    CurrencyOption("USD", "US Dollar", "$")
)

enum class SignInMethod {
    EMAIL_PASSWORD,
    PHONE_NUMBER,
    PASSWORD_PIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: PosViewModel) {
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val registeredUsers by viewModel.users.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    val isBiometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }
    val isBiometricLoginEnabled = (storeSettings?.loginBiometricEnabled ?: false) && isBiometricAvailable
    var selectedBiometricUser by remember(registeredUsers) { mutableStateOf<UserEntity?>(registeredUsers.firstOrNull()) }
    var isUserDropdownExpanded by remember { mutableStateOf(false) }


    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Sign-In Method: Email/Password vs Phone OTP
    var signInMethod by remember { mutableStateOf(SignInMethod.EMAIL_PASSWORD) }

    // Email/Password Form State - No mock defaults
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Phone Auth Form State
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }

    // Organization Registration Form State - No mock defaults
    var storeName by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(supportedCurrencies[0]) }
    var currencySymbol by remember { mutableStateOf(selectedCurrency.symbol) }
    var isCurrencyDropdownExpanded by remember { mutableStateOf(false) }
    var taxMode by remember { mutableStateOf(TaxMode.EXCLUSIVE) }
    var registerFullName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPin by remember { mutableStateOf("") }
    var isRegisterPasswordVisible by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFFF8FAFC),
        modifier = Modifier
            .fillMaxSize()
            .testTag("auth_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFF8FAFC),
                            Color(0xFFF1F5F9),
                            Color(0xFFE2E8F0).copy(alpha = 0.5f)
                        )
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered Responsive Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Language Switcher in Auth Screen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .clickable { viewModel.toggleLanguage() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.NEPALI) "🇳🇵 नेपाली" else "🇬🇧 English",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // App Brand Header
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = PosPrimary.copy(alpha = 0.35f))
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.linearGradient(listOf(PosPrimary, PosSky))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PointOfSale,
                                contentDescription = "POS Logo",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isNepali) "ओम्नीपस (OmniPOS)" else "OmniPOS",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Slate900
                        )

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = PosPrimary.copy(alpha = 0.08f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = if (isNepali) "स्मार्ट क्लाउड र अफलाइन बिलिङ" else "SMART CLOUD & OFFLINE POS",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PosPrimary,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Text(
                            text = if (uiState.authMode == AuthMode.LOGIN)
                                (if (isNepali) "काउन्टरमा पहुँच र पसल सञ्चालन गर्न साइन इन गर्नुहोस्" else "Sign in to access your register and manage store operations")
                            else
                                (if (isNepali) "पसलको विवरण, कर नियम र व्यवस्थापक खाता स्थापना गर्नुहोस्" else "Set up your store profile, tax rules, and master owner account"),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // Feedback / Error Banner
                        AnimatedVisibility(visible = errorMessage != null || statusMessage != null) {
                            val isError = errorMessage != null
                            val bannerText = errorMessage ?: statusMessage ?: ""
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isError) Color(0xFFFEF2F2) else Color(0xFFF0FDF4))
                                    .border(
                                        1.dp,
                                        if (isError) PosRose.copy(alpha = 0.4f) else PosEmerald.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isError) Icons.Default.Lock else Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isError) PosRose else PosEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = bannerText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isError) PosRose else Color(0xFF065F46),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Top Mode Pill Switcher (Sign In vs Register Store)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isLogin = uiState.authMode == AuthMode.LOGIN
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isLogin) Color.White else Color.Transparent)
                                    .then(if (isLogin) Modifier.shadow(2.dp, RoundedCornerShape(10.dp)) else Modifier)
                                    .clickable {
                                        errorMessage = null
                                        statusMessage = null
                                        viewModel.setAuthMode(AuthMode.LOGIN)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (isLogin) PosPrimary else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.signIn,
                                        fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isLogin) PosPrimary else Color(0xFF64748B)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!isLogin) Color.White else Color.Transparent)
                                    .then(if (!isLogin) Modifier.shadow(2.dp, RoundedCornerShape(10.dp)) else Modifier)
                                    .clickable {
                                        errorMessage = null
                                        statusMessage = null
                                        viewModel.setAuthMode(AuthMode.REGISTER)
                                    }
                                    .testTag("register_org_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = if (!isLogin) PosPrimary else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isNepali) "पसल दर्ता" else "Register Store",
                                        fontWeight = if (!isLogin) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (!isLogin) PosPrimary else Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Crossfade(targetState = uiState.authMode, label = "auth_screen_crossfade") { mode ->
                if (mode == AuthMode.LOGIN) {
                    // ==========================================
                    // SINGLE SIGN IN SCREEN (EMAIL, PHONE, GOOGLE)
                    // ==========================================
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Quick Biometric Staff Login Card (if enabled and device supports biometric)
                        if (isBiometricLoginEnabled && registeredUsers.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .testTag("biometric_login_card")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(PosEmerald.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Fingerprint",
                                                tint = PosEmerald,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isNepali) "द्रुत बायोमेट्रिक लगइन" else "Fast Biometric Sign-In",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Slate900
                                            )
                                            val target = selectedBiometricUser ?: registeredUsers.firstOrNull()
                                            Text(
                                                text = if (target != null) {
                                                    "${if (isNepali) "कर्मचारी" else "Operator"}: ${target.fullName} (${target.role.name})"
                                                } else {
                                                    if (isNepali) "साइन इन गर्न औंठाछाप छुनुहोस्" else "Touch fingerprint sensor to sign in"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    if (registeredUsers.size > 1) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        ExposedDropdownMenuBox(
                                            expanded = isUserDropdownExpanded,
                                            onExpandedChange = { isUserDropdownExpanded = !isUserDropdownExpanded },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = (selectedBiometricUser ?: registeredUsers.firstOrNull())?.let { "${it.fullName} (${it.role.name})" } ?: (if (isNepali) "कर्मचारी छनौट गर्नुहोस्" else "Select Operator"),
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUserDropdownExpanded) },
                                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = PosEmerald,
                                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                                )
                                            )
                                            ExposedDropdownMenu(
                                                expanded = isUserDropdownExpanded,
                                                onDismissRequest = { isUserDropdownExpanded = false }
                                            ) {
                                                registeredUsers.forEach { u ->
                                                    DropdownMenuItem(
                                                        text = { Text("${u.fullName} (${u.role.name})", style = MaterialTheme.typography.bodySmall) },
                                                        onClick = {
                                                            selectedBiometricUser = u
                                                            isUserDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    PosGradientButton(
                                        text = if (isNepali) "औंठाछापबाट साइन इन गर्नुहोस्" else "Sign In with Fingerprint",
                                        icon = Icons.Default.Fingerprint,
                                        gradient = PosButtonGradients.Emerald,
                                        onClick = {
                                            val activity = context.findFragmentActivity()
                                            val target = selectedBiometricUser ?: registeredUsers.firstOrNull()
                                            if (activity != null && target != null) {
                                                BiometricHelper.showBiometricPrompt(
                                                    activity = activity,
                                                    title = if (isNepali) "कर्मचारी बायोमेट्रिक साइन इन" else "Staff Biometric Sign In",
                                                    subtitle = "${if (isNepali) "कर्मचारी" else "Operator"}: ${target.fullName}",
                                                    description = if (isNepali) "काउन्टर खोल्न औंठाछाप सेन्सर छुनुहोस्" else "Touch fingerprint sensor to unlock and enter POS",
                                                    negativeButtonText = strings.cancel,
                                                    onSuccess = {
                                                        viewModel.loginWithBiometric(target) {}
                                                    },
                                                    onError = { err ->
                                                        errorMessage = err
                                                    }
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(46.dp)
                                    )
                                }
                            }
                        }

                        // Method Selector: Modern Segmented Pill Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isEmailSelected = signInMethod == SignInMethod.EMAIL_PASSWORD
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isEmailSelected) Color.White else Color.Transparent)
                                    .then(
                                        if (isEmailSelected) Modifier.shadow(1.5.dp, RoundedCornerShape(9.dp)) else Modifier
                                    )
                                    .clickable {
                                        signInMethod = SignInMethod.EMAIL_PASSWORD
                                        errorMessage = null
                                        statusMessage = null
                                    }
                                    .testTag("tab_email_login"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = if (isEmailSelected) PosPrimary else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isNepali) "इमेल / पिन" else "Email / PIN",
                                        fontWeight = if (isEmailSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isEmailSelected) PosPrimary else Color(0xFF64748B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            val isPhoneSelected = signInMethod == SignInMethod.PHONE_NUMBER
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isPhoneSelected) Color.White else Color.Transparent)
                                    .then(
                                        if (isPhoneSelected) Modifier.shadow(1.5.dp, RoundedCornerShape(9.dp)) else Modifier
                                    )
                                    .clickable {
                                        signInMethod = SignInMethod.PHONE_NUMBER
                                        errorMessage = null
                                        statusMessage = null
                                    }
                                    .testTag("tab_phone_login"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = if (isPhoneSelected) PosPrimary else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isNepali) "फोन ओटिपी" else "Phone OTP",
                                        fontWeight = if (isPhoneSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isPhoneSelected) PosPrimary else Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (signInMethod == SignInMethod.EMAIL_PASSWORD) {
                            // --- Email / Password Mode ---
                            Text(
                                text = if (isNepali) "खाता प्रमाणहरू" else "Account Credentials",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = loginEmail,
                                onValueChange = {
                                    loginEmail = it
                                    errorMessage = null
                                },
                                label = { Text(if (isNepali) "कर्मचारी इमेल वा युजरनेम" else "Email or Staff Username") },
                                placeholder = { Text("admin@store.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PosPrimary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PosPrimary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = {
                                    loginPassword = it
                                    errorMessage = null
                                },
                                label = { Text(if (isNepali) "सुरक्षा पिन वा पासवर्ड" else "Security PIN or Password") },
                                placeholder = { Text("••••••••") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PosPrimary) },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isNepali) "पासवर्ड देखाउनुहोस्/लुकाउनुहोस्" else "Toggle password"
                                        )
                                    }
                                },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    isLoading = true
                                    viewModel.loginWithCredentials(
                                        emailOrUsername = loginEmail,
                                        pinOrSecret = loginPassword,
                                        onSuccess = { isLoading = false },
                                        onError = {
                                            isLoading = false
                                            errorMessage = it
                                        }
                                    )
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PosPrimary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            PosGradientButton(
                                text = if (isNepali) "काउन्टरमा साइन इन गर्नुहोस्" else "Sign In to POS",
                                onClick = {
                                    if (loginEmail.isBlank()) {
                                        errorMessage = if (isNepali) "कृपया आफ्नो कर्मचारी इमेल वा युजरनेम राख्नुहोस्" else "Please enter your staff email or username"
                                    } else {
                                        isLoading = true
                                        errorMessage = null
                                        viewModel.loginWithCredentials(
                                            emailOrUsername = loginEmail,
                                            pinOrSecret = loginPassword,
                                            onSuccess = { isLoading = false },
                                            onError = {
                                                isLoading = false
                                                errorMessage = it
                                            }
                                        )
                                    }
                                },
                                gradient = PosButtonGradients.Primary,
                                icon = Icons.Default.LockOpen,
                                isLoading = isLoading,
                                enabled = !isLoading,
                                height = 50.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_submit_button")
                            )

                            if (isBiometricLoginEnabled && registeredUsers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                PosOutlineButton(
                                    text = if (isNepali) "औंठाछापबाट साइन इन गर्नुहोस्" else "Sign In with Fingerprint",
                                    icon = Icons.Default.Fingerprint,
                                    onClick = {
                                        val activity = context.findFragmentActivity()
                                        val target = selectedBiometricUser ?: registeredUsers.firstOrNull()
                                        if (activity != null && target != null) {
                                            BiometricHelper.showBiometricPrompt(
                                                activity = activity,
                                                title = if (isNepali) "कर्मचारी बायोमेट्रिक साइन इन" else "Staff Biometric Sign In",
                                                subtitle = "${if (isNepali) "कर्मचारी" else "Operator"}: ${target.fullName}",
                                                description = if (isNepali) "काउन्टर खोल्न औंठाछाप सेन्सर छुनुहोस्" else "Touch fingerprint sensor to unlock and enter POS",
                                                negativeButtonText = strings.cancel,
                                                onSuccess = {
                                                    viewModel.loginWithBiometric(target) {}
                                                },
                                                onError = { err ->
                                                    errorMessage = err
                                                }
                                            )
                                        }
                                    },
                                    height = 48.dp,
                                    modifier = Modifier.fillMaxWidth().testTag("login_biometric_secondary_button")
                                )
                            }
                        } else {
                            // --- Phone Number Authentication Mode ---
                            Text(
                                text = if (isNepali) "फोन प्रमाणीकरण" else "Phone Verification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    phoneNumber = it
                                    errorMessage = null
                                },
                                label = { Text(if (isNepali) "फोन नम्बर (देश कोड सहित)" else "Phone Number (with Country Code)") },
                                placeholder = { Text(if (isNepali) "+९७७ ९८००००००००" else "+1 555-0199 or +977 9800000000") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PosPrimary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("phone_number_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PosPrimary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (verificationId == null) {
                                PosGradientButton(
                                    text = if (isNepali) "एसएमएस प्रमाणीकरण कोड पठाउनुहोस्" else "Send SMS Verification Code",
                                    onClick = {
                                        val activity = context as? Activity
                                        if (activity != null && phoneNumber.isNotBlank()) {
                                            isLoading = true
                                            errorMessage = null
                                            viewModel.startPhoneAuth(
                                                phoneNumber = phoneNumber,
                                                activity = activity,
                                                onCodeSent = { id ->
                                                    isLoading = false
                                                    verificationId = id
                                                    statusMessage = if (isNepali) "$phoneNumber मा एसएमएस प्रमाणीकरण कोड पठाइयो" else "SMS verification code sent to $phoneNumber"
                                                },
                                                onError = {
                                                    isLoading = false
                                                    errorMessage = it
                                                }
                                            )
                                        } else {
                                            errorMessage = if (isNepali) "कृपया मान्य फोन नम्बर राख्नुहोस्" else "Please enter a valid phone number"
                                        }
                                    },
                                    gradient = PosButtonGradients.Primary,
                                    icon = Icons.Default.Sms,
                                    isLoading = isLoading,
                                    enabled = !isLoading,
                                    height = 50.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("send_sms_button")
                                )
                            } else {
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = {
                                        if (it.length <= 6) otpCode = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (isNepali) "६-अङ्कको एसएमएस प्रमाणीकरण कोड" else "6-Digit SMS Verification Code") },
                                    placeholder = { Text("123456") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PosEmerald) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PosEmerald,
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC)
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                PosGradientButton(
                                    text = if (isNepali) "प्रमाणित गरी साइन इन गर्नुहोस्" else "Verify & Sign In",
                                    onClick = {
                                        if (otpCode.length == 6 && verificationId != null) {
                                            isLoading = true
                                            errorMessage = null
                                            viewModel.verifyPhoneOtp(
                                                verificationId = verificationId!!,
                                                code = otpCode,
                                                onSuccess = { isLoading = false },
                                                onError = {
                                                    isLoading = false
                                                    errorMessage = it
                                                }
                                            )
                                        } else {
                                            errorMessage = if (isNepali) "कृपया ६-अङ्कको कोड प्रविष्ट गर्नुहोस्" else "Please enter the 6-digit code"
                                        }
                                    },
                                    gradient = PosButtonGradients.Emerald,
                                    icon = Icons.Default.Check,
                                    isLoading = isLoading,
                                    enabled = !isLoading,
                                    height = 50.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("verify_otp_button")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Divider with OR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                            Text(
                                text = if (isNepali) "  वा  " else "  OR  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        PosGoogleButton(
                            text = if (isNepali) "गुगलबाट अगाडि बढ्नुहोस्" else "Continue with Google",
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    isLoading = true
                                    errorMessage = null
                                    viewModel.loginWithGoogle(
                                        activity = activity,
                                        onSuccess = { isLoading = false },
                                        onError = {
                                            isLoading = false
                                            errorMessage = it
                                        },
                                        onCancelled = {
                                            isLoading = false
                                        }
                                    )
                                }
                            },
                            isLoading = isLoading,
                            enabled = !isLoading,
                            height = 50.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_login_button")
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Bottom Action: Register Organization
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isNepali) "नयाँ पसल वा व्यवसाय दर्ता गर्न चाहनुहुन्छ?" else "New store or business?",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                            TextButton(
                                onClick = {
                                    errorMessage = null
                                    statusMessage = null
                                    viewModel.setAuthMode(AuthMode.REGISTER)
                                },
                                modifier = Modifier.testTag("register_org_button")
                            ) {
                                Text(
                                    if (isNepali) "पसल दर्ता गर्नुहोस्" else "Register Organization",
                                    color = PosPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // REGISTER ORGANIZATION SCREEN
                    // ==========================================
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.setAuthMode(AuthMode.LOGIN) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isNepali) "साइन इनमा फर्कनुहोस्" else "Back to Sign In")
                            }
                            Text(
                                text = if (isNepali) "नयाँ व्यवसाय / पसल दर्ता" else "Register New Organization",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        }

                        Text(
                            text = if (isNepali) "तपाईंको पसलको नाम, कर सेटिङहरू मिलाउनुहोस् र मुख्य व्यवस्थापक खाता खोल्नुहोस्।" else "Set up your store name, tax settings, and create your owner account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                        )

                        // Quick Register with Google Option
                        PosGoogleButton(
                            text = if (isNepali) "गुगलबाट दर्ता गर्नुहोस्" else "Register with Google",
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    isLoading = true
                                    errorMessage = null
                                    viewModel.loginWithGoogle(
                                        activity = activity,
                                        customStoreName = storeName,
                                        customTaxId = taxId,
                                        customCurrencySymbol = currencySymbol,
                                        customTaxMode = taxMode,
                                        onSuccess = { isLoading = false },
                                        onError = {
                                            isLoading = false
                                            errorMessage = it
                                        },
                                        onCancelled = {
                                            isLoading = false
                                        }
                                    )
                                }
                            },
                            isLoading = isLoading,
                            enabled = !isLoading,
                            height = 50.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_register_button")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                            Text(
                                text = if (isNepali) "वा म्यानुअल विवरण भर्नुहोस्" else "OR SET UP MANUALLY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        }

                        // Store Information Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(PosPrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Business, contentDescription = null, tint = PosPrimary, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isNepali) "१. पसलको विवरण" else "1. Store Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                                }

                                OutlinedTextField(
                                    value = storeName,
                                    onValueChange = { storeName = it },
                                    label = { Text(if (isNepali) "पसल / व्यवसायको नाम *" else "Store / Business Name *") },
                                    placeholder = { Text(if (isNepali) "जस्तै: काठमाडौँ कफी हाउस" else "e.g. Kathmandu Coffee Co.") },
                                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = PosPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PosPrimary,
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                OutlinedTextField(
                                    value = taxId,
                                    onValueChange = { taxId = it },
                                    label = { Text(if (isNepali) "कर / प्यान / भ्याट नम्बर (ऐच्छिक)" else "Tax / VAT ID (Optional)") },
                                    placeholder = { Text(if (isNepali) "जस्तै: ६०१२३४५६७" else "e.g. 601234567") },
                                    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, tint = PosPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PosPrimary,
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                // Currency Dropdown Selection
                                ExposedDropdownMenuBox(
                                    expanded = isCurrencyDropdownExpanded,
                                    onExpandedChange = { isCurrencyDropdownExpanded = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedCurrency.displayLabel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(if (isNepali) "पसलको मुद्रा *" else "Store Currency *") },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(PosPrimary.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = selectedCurrency.symbol,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PosPrimary,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCurrencyDropdownExpanded)
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isCurrencyDropdownExpanded,
                                        onDismissRequest = { isCurrencyDropdownExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        supportedCurrencies.forEach { option ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${option.code} - ${option.name}",
                                                            fontWeight = if (option.code == selectedCurrency.code) FontWeight.Bold else FontWeight.Normal,
                                                            color = Slate900
                                                        )
                                                        Text(
                                                            text = option.symbol,
                                                            fontWeight = FontWeight.Bold,
                                                            color = PosPrimary,
                                                            fontSize = 15.sp
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    selectedCurrency = option
                                                    currencySymbol = option.symbol
                                                    isCurrencyDropdownExpanded = false
                                                },
                                                leadingIcon = {
                                                    if (option.code == selectedCurrency.code) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = PosPrimary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                // Tax Mode: Exclusive vs Inclusive Segmented Cards
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isNepali) "कर गणना विधि *" else "Tax Calculation Mode *",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate900
                                        )
                                        Text(
                                            text = if (taxMode == TaxMode.EXCLUSIVE) (if (isNepali) "बिल बनाउँदा थपिने (Exclusive)" else "Added at Checkout") else (if (isNepali) "मूल्यमै समावेश (Inclusive)" else "Included in Price"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (taxMode == TaxMode.EXCLUSIVE) PosPrimary else PosEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Exclusive Tax Card
                                        val isExclusive = taxMode == TaxMode.EXCLUSIVE
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { taxMode = TaxMode.EXCLUSIVE },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isExclusive) PosPrimary.copy(alpha = 0.08f) else Color.White
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (isExclusive) 2.dp else 1.dp,
                                                color = if (isExclusive) PosPrimary else Color(0xFFCBD5E1)
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (isNepali) "थपिने (Exclusive)" else "Exclusive",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (isExclusive) PosPrimary else Slate900
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isExclusive) PosPrimary else Color(0xFFE2E8F0)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isExclusive) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (isNepali) "खुद मूल्य + भ्याट" else "Net Price + Tax",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isExclusive) PosPrimary else Color(0xFF64748B)
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = if (isNepali) "बिलिङ गर्दा कर थपिन्छ (रु १०० + १३% = रु ११३)।" else "Tax calculated & added on top at checkout ($10.00 + 10% = $11.00 total).",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B),
                                                    lineHeight = 13.sp
                                                )
                                            }
                                        }

                                        // Inclusive Tax Card
                                        val isInclusive = taxMode == TaxMode.INCLUSIVE
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { taxMode = TaxMode.INCLUSIVE },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isInclusive) PosEmerald.copy(alpha = 0.08f) else Color.White
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (isInclusive) 2.dp else 1.dp,
                                                color = if (isInclusive) PosEmerald else Color(0xFFCBD5E1)
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (isNepali) "समावेश (Inclusive)" else "Inclusive",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (isInclusive) PosEmerald else Slate900
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isInclusive) PosEmerald else Color(0xFFE2E8F0)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isInclusive) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (isNepali) "कुल / भ्याट समावेश" else "Gross / VAT Included",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isInclusive) PosEmerald else Color(0xFF64748B)
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = if (isNepali) "सामानको मूल्यमै सम्पूर्ण कर समावेश हुन्छ। रसिदमा भ्याट छुट्ट्याएर देखाइन्छ।" else "Shelf price includes all taxes. Receipts show extracted VAT without extra charge.",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF64748B),
                                                    lineHeight = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Owner Account Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(PosEmerald.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = PosEmerald, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isNepali) "२. मुख्य व्यवस्थापक (Owner) खाता" else "3. Master Admin Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Slate900)
                                }

                                OutlinedTextField(
                                    value = registerFullName,
                                    onValueChange = { registerFullName = it },
                                    label = { Text(if (isNepali) "मालिकको पूरा नाम *" else "Owner Full Name *") },
                                    placeholder = { Text(if (isNepali) "जस्तै: राम श्रेष्ठ" else "e.g. John Doe") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PosEmerald) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PosEmerald,
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                OutlinedTextField(
                                    value = registerEmail,
                                    onValueChange = { registerEmail = it },
                                    label = { Text(if (isNepali) "व्यवस्थापक इमेल *" else "Admin Work Email *") },
                                    placeholder = { Text("owner@store.com") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PosEmerald) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PosEmerald,
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )

                                OutlinedTextField(
                                    value = registerPin,
                                    onValueChange = { registerPin = it },
                                    label = { Text(if (isNepali) "मास्टर पिन वा पासवर्ड *" else "Master PIN or Password *") },
                                    placeholder = { Text(if (isNepali) "६-अङ्कको पिन वा पासवर्ड" else "6-digit PIN or password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PosEmerald) },
                                    trailingIcon = {
                                        IconButton(onClick = { isRegisterPasswordVisible = !isRegisterPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isRegisterPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isNepali) "पासवर्ड देखाउनुहोस्/लुकाउनुहोस्" else "Toggle password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (isRegisterPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PosEmerald,
                                        unfocusedBorderColor = Color(0xFFCBD5E1),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        PosGradientButton(
                            text = if (isNepali) "पसल दर्ता सम्पन्न गर्नुहोस्" else "Complete Store Registration",
                            onClick = {
                                if (storeName.isBlank() || registerFullName.isBlank() || registerEmail.isBlank()) {
                                    errorMessage = if (isNepali) "कृपया सबै आवश्यक विवरणहरू भर्नुहोस् (पसलको नाम, पूरा नाम, इमेल)।" else "Please fill in all required fields (Store Name, Full Name, Email)."
                                } else {
                                    isLoading = true
                                    errorMessage = null
                                    viewModel.registerNewStore(
                                        storeName = storeName,
                                        taxId = taxId,
                                        currencySymbol = currencySymbol,
                                        taxMode = taxMode,
                                        userName = registerFullName,
                                        email = registerEmail,
                                        role = StaffRole.OWNER,
                                        pin = registerPin,
                                        onSuccess = { isLoading = false },
                                        onError = {
                                            isLoading = false
                                            errorMessage = it
                                        }
                                    )
                                }
                            },
                            gradient = PosButtonGradients.Emerald,
                            icon = Icons.Default.Check,
                            isLoading = isLoading,
                            enabled = !isLoading,
                            height = 50.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_submit_button")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isNepali) "पहिले नै पसल खाता छ?" else "Already have a store account?", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            TextButton(onClick = { viewModel.setAuthMode(AuthMode.LOGIN) }) {
                                Text(if (isNepali) "साइन इन गर्नुहोस्" else "Sign In", color = PosPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}
}
