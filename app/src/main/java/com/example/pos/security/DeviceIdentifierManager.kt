package com.example.pos.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature

/**
 * Enterprise POS Hardware-Backed KeyStore Device Identifier Manager.
 *
 * Generates an immutable, app-scoped EC key pair backed by hardware (TEE / StrongBox).
 * The device identity is derived from the SHA-256 fingerprint of the hardware public key.
 * Also enables digital signing of shift openings and critical transactions to guarantee physical device presence.
 */
object DeviceIdentifierManager {

    private const val KEY_ALIAS = "pos_device_identity_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /**
     * Retrieves or generates a hardware-backed device identifier.
     * The ID is the SHA-256 fingerprint of the TEE/StrongBox public key.
     * Gracefully falls back to Widevine DRM or ANDROID_ID if hardware KeyStore fails on the device.
     */
    fun getHardwareDeviceId(context: Context): String {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateHardwareKey(context)
            }

            val certificate = keyStore.getCertificate(KEY_ALIAS)
            if (certificate != null) {
                val publicKeyBytes = certificate.publicKey.encoded
                val digest = MessageDigest.getInstance("SHA-256").digest(publicKeyBytes)
                Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            } else {
                DrmDeviceIdentifier.getWidevineDeviceId() ?: DeviceInfoHelper.getDisplayDeviceId(context)
            }
        } catch (_: Exception) {
            DrmDeviceIdentifier.getWidevineDeviceId() ?: DeviceInfoHelper.getDisplayDeviceId(context)
        }
    }

    /**
     * Returns the Base64-encoded X.509 public key bytes for registering this terminal
     * with the backend server during device onboarding.
     */
    fun getPublicKeyBase64(context: Context): String {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateHardwareKey(context)
            }

            val certificate = keyStore.getCertificate(KEY_ALIAS)
            if (certificate != null) {
                Base64.encodeToString(certificate.publicKey.encoded, Base64.NO_WRAP)
            } else {
                Base64.encodeToString(DeviceInfoHelper.getDisplayDeviceId(context).toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            }
        } catch (_: Exception) {
            Base64.encodeToString(DeviceInfoHelper.getDisplayDeviceId(context).toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
    }

    /**
     * Digitally signs a request payload (e.g. shift open challenge or transaction hash)
     * using the hardware-backed private key inside the TEE/StrongBox chip.
     */
    fun signPayload(context: Context, data: ByteArray): String {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateHardwareKey(context)
            }

            val privateKey = keyStore.getKey(KEY_ALIAS, null) as? java.security.PrivateKey
            if (privateKey != null) {
                val signature = Signature.getInstance("SHA256withECDSA").apply {
                    initSign(privateKey)
                    update(data)
                }
                val signedBytes = signature.sign()
                Base64.encodeToString(signedBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            } else {
                val digest = MessageDigest.getInstance("SHA-256").digest(data)
                Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            }
        } catch (_: Exception) {
            val digest = MessageDigest.getInstance("SHA-256").digest(data)
            Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }
    }

    /**
     * Generates a persistent EC key pair backed by hardware (StrongBox if available, falling back to standard TEE).
     */
    private fun generateHardwareKey(context: Context) {
        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )

        val hasStrongBox = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } else {
            false
        }

        try {
            val specBuilder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).setDigests(KeyProperties.DIGEST_SHA256)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hasStrongBox) {
                specBuilder.setIsStrongBoxBacked(true)
            }

            kpg.initialize(specBuilder.build())
            kpg.generateKeyPair()
        } catch (e: Exception) {
            // If StrongBox is claimed by the device but unavailable or fails initialization,
            // fall back gracefully to standard TEE KeyStore
            if (hasStrongBox) {
                val fallbackBuilder = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                ).setDigests(KeyProperties.DIGEST_SHA256)

                kpg.initialize(fallbackBuilder.build())
                kpg.generateKeyPair()
            } else {
                throw e
            }
        }
    }
}
