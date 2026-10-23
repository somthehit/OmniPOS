package com.example.pos.security

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * Helper for human-readable secondary device tagging, display labels, and ANDROID_ID fallback.
 *
 * Settings.Secure.ANDROID_ID is scoped per app signing key and user profile on Android 8.0 (API 26)+.
 * It persists across app updates, restarts, and reinstalls on the same physical terminal.
 */
object DeviceInfoHelper {

    /**
     * Retrieves Settings.Secure.ANDROID_ID.
     */
    @SuppressLint("HardwareIds")
    fun getDisplayDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN_DEVICE"
    }

    /**
     * Returns a human-friendly POS terminal label for UI alerts, server logs, and shift conflict dialogs.
     * Example: "Sunmi V2 (E4A8F2)" or "Pixel Tablet (7B912C)"
     */
    fun getFormattedTerminalLabel(context: Context): String {
        val rawId = getDisplayDeviceId(context)
        val shortId = if (rawId.length >= 6) rawId.takeLast(6).uppercase() else rawId.uppercase()
        val model = Build.MODEL ?: "POS Terminal"
        return "$model ($shortId)"
    }
}
