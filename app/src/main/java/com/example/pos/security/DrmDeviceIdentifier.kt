package com.example.pos.security

import android.media.MediaDrm
import android.os.Build
import android.util.Base64
import java.util.UUID

/**
 * Hardware DRM Device Identifier (Widevine).
 *
 * Retrieves the OEM hardware-bound unique identifier via Widevine DRM.
 * This ID survives application uninstallation and reinstallation without
 * requiring privileged runtime permissions (such as READ_PHONE_STATE).
 */
object DrmDeviceIdentifier {

    // Widevine Modular DRM UUID
    private val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d82323387521L)

    /**
     * Queries the Widevine hardware DRM engine for PROPERTY_DEVICE_UNIQUE_ID.
     * Returns a URL-safe Base64 string, or null if Widevine DRM is not supported on this device.
     */
    fun getWidevineDeviceId(): String? {
        var mediaDrm: MediaDrm? = null
        return try {
            mediaDrm = MediaDrm(WIDEVINE_UUID)
            val deviceIdBytes = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            Base64.encodeToString(deviceIdBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        } catch (_: Exception) {
            null
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mediaDrm?.close()
            } else {
                @Suppress("DEPRECATION")
                mediaDrm?.release()
            }
        }
    }
}
