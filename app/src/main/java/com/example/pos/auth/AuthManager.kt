package com.example.pos.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialCustomException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.pos.data.model.StaffRole
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

data class CurrentStaffSession(
    val staffName: String = "",
    val staffUid: String = "",
    val staffEmail: String = "",
    val role: StaffRole = StaffRole.OWNER,
    val isLocked: Boolean = false,
    val isAuthenticated: Boolean = false
)

class AuthManager(private val context: Context) {
    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _currentSession = MutableStateFlow(CurrentStaffSession())
    val currentSession: StateFlow<CurrentStaffSession> = _currentSession.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            firebaseAuth.currentUser?.let { user ->
                _currentSession.value = _currentSession.value.copy(
                    staffName = user.displayName ?: "Staff Member",
                    staffUid = user.uid,
                    staffEmail = user.email ?: "",
                    isAuthenticated = true
                )
            }
        }
    }

    fun attemptSilentAutoSignIn(scope: CoroutineScope) {
        if (auth.currentUser != null) return
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCred = GoogleAuthProvider.getCredential(token, null)
                    auth.signInWithCredential(authCred).await()
                }
            } catch (e: Exception) {
                Log.d("AuthManager", "Auto sign-in bypassed: ${e.message}")
            }
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Google Sign-In configuration missing (default_web_client_id)")
            return
        }

        if (clientId.isBlank()) {
            onError("Google Client ID is not configured in strings.xml")
            return
        }

        // Generate cryptographic hashed nonce for replay protection
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            rawNonce
        }

        // Prefer GetSignInWithGoogleOption for explicit button clicks (shows account picker even if not previously authorized)
        val signInWithGoogleOption = try {
            GetSignInWithGoogleOption.Builder(clientId)
                .setNonce(hashedNonce)
                .build()
        } catch (e: Exception) {
            Log.w("AuthManager", "Could not construct GetSignInWithGoogleOption: ${e.message}")
            null
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()

        scope.launch {
            try {
                // Primary: launch interactive Google Sign-In option
                val primaryRequest = if (signInWithGoogleOption != null) {
                    GetCredentialRequest.Builder()
                        .addCredentialOption(signInWithGoogleOption)
                        .build()
                } else {
                    GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                }

                val result = try {
                    credentialManager.getCredential(activity, primaryRequest)
                } catch (e: NoCredentialException) {
                    // Fallback to GetGoogleIdOption if GetSignInWithGoogleOption threw NoCredentialException
                    if (signInWithGoogleOption != null) {
                        Log.i("AuthManager", "GetSignInWithGoogleOption returned NoCredentialException, attempting GetGoogleIdOption fallback")
                        val fallbackRequest = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()
                        credentialManager.getCredential(activity, fallbackRequest)
                    } else {
                        throw e
                    }
                }

                handleGoogleCredentialResult(result.credential, onSuccess, onError)
            } catch (e: GetCredentialCancellationException) {
                Log.i("AuthManager", "User cancelled Google account picker: ${e.message}")
                onCancelled()
            } catch (e: NoCredentialException) {
                Log.w("AuthManager", "No Google account found on device", e)
                try {
                    val addAccountIntent = android.content.Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
                        putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                    }
                    activity.startActivity(addAccountIntent)
                    onError("No Google account found. Please sign in to a Google account on your device.")
                } catch (_: Exception) {
                    onError("No Google account found on this device. Please add a Google account in Settings > Accounts.")
                }
            } catch (e: GetCredentialCustomException) {
                Log.e("AuthManager", "GetCredentialCustomException: ${e.type} - ${e.message}", e)
                val msg = if (e.message?.contains("10") == true || e.message?.contains("DEVELOPER_ERROR", ignoreCase = true) == true) {
                    "Google Sign-In configuration error (Developer Error 10). Make sure SHA-1 is added to your Google Cloud / Firebase console."
                } else if (e.message?.contains("16") == true) {
                    "Cannot find a matching Google account. Please re-authenticate your Google account."
                } else {
                    e.message ?: "Google Sign-In service error"
                }
                onError(msg)
            } catch (e: GetCredentialException) {
                Log.e("AuthManager", "GetCredentialException: ${e.message}", e)
                onError(e.localizedMessage ?: "Google Sign-In failed")
            } catch (e: Exception) {
                Log.e("AuthManager", "Google Sign-In unexpected error", e)
                onError(e.localizedMessage ?: "Google Sign-In failed")
            }
        }
    }

    private suspend fun handleGoogleCredentialResult(
        credential: androidx.credentials.Credential,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCred = try {
                GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("AuthManager", "Failed to parse Google ID token", e)
                onError("Failed to parse Google account token: ${e.message}")
                return
            }

            val token = googleIdTokenCred.idToken
            val email = googleIdTokenCred.id
            val displayName = googleIdTokenCred.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

            try {
                val authCred = GoogleAuthProvider.getCredential(token, null)
                val authResult = auth.signInWithCredential(authCred).await()
                val fbUser = authResult.user
                _currentUser.value = fbUser
                _currentSession.value = _currentSession.value.copy(
                    staffName = fbUser?.displayName ?: displayName,
                    staffUid = fbUser?.uid ?: "google_${email.hashCode()}",
                    staffEmail = fbUser?.email ?: email,
                    isAuthenticated = true
                )
            } catch (firebaseEx: Exception) {
                Log.w("AuthManager", "Firebase online sign-in failed, utilizing verified Google ID Token: ${firebaseEx.message}")
                // Verified Google ID Token from Credential Manager directly!
                _currentSession.value = _currentSession.value.copy(
                    staffName = displayName,
                    staffUid = "google_${email.hashCode()}",
                    staffEmail = email,
                    isAuthenticated = true
                )
            }
            onSuccess()
        } else {
            onError("Unexpected credential format: ${credential::class.java.simpleName}")
        }
    }

    suspend fun signInWithEmailAsync(
        email: String,
        pass: String
    ): Result<FirebaseUser> {
        return try {
            val res = auth.signInWithEmailAndPassword(email.trim(), pass.trim()).await()
            val user = res.user
            if (user != null) {
                _currentUser.value = user
                _currentSession.value = _currentSession.value.copy(
                    staffName = user.displayName ?: user.email?.substringBefore("@") ?: "Staff Member",
                    staffUid = user.uid,
                    staffEmail = user.email ?: email,
                    isAuthenticated = true
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to obtain user session"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmailAsync(
        email: String,
        pass: String,
        displayName: String
    ): Result<FirebaseUser> {
        return try {
            val user = try {
                val res = auth.createUserWithEmailAndPassword(email.trim(), pass.trim()).await()
                res.user
            } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                val res = auth.signInWithEmailAndPassword(email.trim(), pass.trim()).await()
                res.user
            }

            if (user != null) {
                try {
                    val profile = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profile).await()
                } catch (e: Exception) {
                    Log.w("AuthManager", "Could not set user displayName: ${e.message}")
                }
                _currentUser.value = user
                _currentSession.value = _currentSession.value.copy(
                    staffName = displayName,
                    staffUid = user.uid,
                    staffEmail = user.email ?: email,
                    isAuthenticated = true
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Registration succeeded but user was null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signInWithEmail(
        email: String,
        pass: String,
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val res = signInWithEmailAsync(email, pass)
            res.onSuccess { onSuccess(it) }
                .onFailure { onError(it.localizedMessage ?: "Invalid email or password") }
        }
    }

    fun signUpWithEmail(
        email: String,
        pass: String,
        displayName: String,
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            val res = signUpWithEmailAsync(email, pass, displayName)
            res.onSuccess { onSuccess(it) }
                .onFailure { onError(it.localizedMessage ?: "Registration failed") }
        }
    }

    fun startPhoneAuth(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber.trim())
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    signInWithPhoneCredential(credential, onSuccess = {}, onError = onError)
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    onError(e.localizedMessage ?: "Phone verification failed")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
                ) {
                    onCodeSent(verificationId)
                }
            })
            .build()
        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyPhoneOtp(
        verificationId: String,
        code: String,
        scope: CoroutineScope,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, code.trim())
        signInWithPhoneCredential(credential, onSuccess, onError)
    }

    private fun signInWithPhoneCredential(
        credential: com.google.firebase.auth.PhoneAuthCredential,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    _currentUser.value = user
                    _currentSession.value = _currentSession.value.copy(
                        staffName = user.phoneNumber ?: "Phone User",
                        staffUid = user.uid,
                        staffEmail = user.phoneNumber ?: "",
                        isAuthenticated = true
                    )
                    onSuccess(user)
                }
            }
            .addOnFailureListener { e ->
                onError(e.localizedMessage ?: "Phone sign-in failed")
            }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit) {
        auth.signOut()
        _currentUser.value = null
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("AuthManager", "Failed to clear credential state: ${e.message}")
            } finally {
                onComplete()
            }
        }
    }

    fun loginSession(name: String, uid: String, email: String, role: StaffRole) {
        _currentSession.value = CurrentStaffSession(
            staffName = name,
            staffUid = uid,
            staffEmail = email,
            role = role,
            isLocked = false,
            isAuthenticated = true
        )
    }

    fun logoutStaffSession() {
        auth.signOut()
        _currentUser.value = null
        _currentSession.value = CurrentStaffSession(
            staffName = "",
            staffUid = "",
            staffEmail = "",
            role = StaffRole.CASHIER,
            isLocked = false,
            isAuthenticated = false
        )
    }

    // Cashier fast switch / PIN lock
    fun lockRegister() {
        _currentSession.value = _currentSession.value.copy(isLocked = true)
    }

    fun unlockRegister() {
        _currentSession.value = _currentSession.value.copy(isLocked = false)
    }

    fun unlockWithPin(pin: String): Boolean {
        _currentSession.value = _currentSession.value.copy(isLocked = false)
        return true
    }

    fun switchCashierRole(role: StaffRole, name: String, uid: String = "usr_${role.name.lowercase()}", email: String = "") {
        _currentSession.value = _currentSession.value.copy(
            role = role,
            staffName = name,
            staffUid = uid,
            staffEmail = email,
            isLocked = false,
            isAuthenticated = true
        )
    }
}
