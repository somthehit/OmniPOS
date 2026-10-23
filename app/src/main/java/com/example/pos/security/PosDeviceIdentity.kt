package com.example.pos.security

import android.content.Context

/**
 * Unified Terminal & Device Identity Coordinator for OmniPOS.
 *
 * Integrates:
 * 1. Hardware KeyStore EC Key (TEE / StrongBox) - Cryptographic primary identifier & request signer.
 * 2. Widevine Hardware DRM ID - Survives reinstallation.
 * 3. Settings.Secure.ANDROID_ID - Reliable app-scoped fallback.
 * 4. Human-readable terminal labeling for UI conflict dialogs and server logs.
 */
object PosDeviceIdentity {

    /**
     * Resolves the strongest hardware or platform identifier available on this terminal:
     * 1. Hardware KeyStore TEE / StrongBox SHA-256 fingerprint.
     * 2. Widevine Hardware DRM device unique ID.
     * 3. Settings.Secure.ANDROID_ID.
     */
    fun getDeviceIdentifier(context: Context): String {
        return try {
            DeviceIdentifierManager.getHardwareDeviceId(context)
        } catch (_: Exception) {
            DrmDeviceIdentifier.getWidevineDeviceId()
                ?: DeviceInfoHelper.getDisplayDeviceId(context)
        }
    }

    /**
     * Returns a human-friendly POS terminal label for user-facing conflict alerts and shift tracking.
     * Example: "Sunmi V2 (E4A8F2)"
     */
    fun getDisplayTerminalName(context: Context): String {
        return DeviceInfoHelper.getFormattedTerminalLabel(context)
    }

    /**
     * Signs an open shift challenge with the hardware private key inside TEE / StrongBox.
     * Used by backend authentication to verify physical terminal possession.
     */
    fun signShiftChallenge(context: Context, cashierId: String, timestamp: Long): String {
        val payload = "OPEN_SHIFT:$cashierId:$timestamp".toByteArray(Charsets.UTF_8)
        return DeviceIdentifierManager.signPayload(context, payload)
    }
}
