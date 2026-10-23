package com.example.pos.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat

/**
 * Universal Hardware Barcode Scanner Listener for POS Terminals & Handhelds.
 *
 * Supports two primary modes used in real-world retail & grocery environments:
 * 1. HID Keyboard Emulation Mode:
 *    - USB & Bluetooth laser barcode guns (Honeywell, Zebra, Inateck, Netum, etc.)
 *    - Intercepts rapid character bursts (< 70ms per key) followed by an ENTER key.
 *    - Prevents raw barcode numbers from polluting actively focused text fields.
 *
 * 2. Enterprise POS Hardware Broadcast Mode:
 *    - Dedicated POS terminals (Sunmi, Zebra DataWedge, Honeywell, Newland, Urovo).
 *    - Listens to manufacturer barcode broadcast intents.
 *
 * Provides immediate acoustic (beep) and haptic (vibration) feedback upon successful scan.
 */
class HardwareBarcodeScanner(
    private val context: Context,
    private val maxKeystrokeIntervalMs: Long = 75L,
    private val minBarcodeLength: Int = 3,
    private val enableAudioFeedback: Boolean = true,
    private val enableHapticFeedback: Boolean = true,
    private val onBarcodeScanned: (barcode: String) -> Unit
) {
    private val tag = "HardwareScanner"

    // HID Key Accumulator
    private val buffer = StringBuilder()
    private var lastKeystrokeTime = 0L
    private var isBurstModeActive = false

    // Feedback generators
    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator? = initVibrator()

    private var isListening = false

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
        } catch (e: Exception) {
            Log.w(tag, "Could not initialize ToneGenerator: ${e.message}")
        }
    }

    private fun initVibrator(): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    // Enterprise Broadcast Receiver for Android POS terminals
    private val enterpriseBarcodeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            val barcode = extractBarcodeFromIntent(intent)
            if (!barcode.isNullOrBlank()) {
                Log.d(tag, "Barcode received via enterprise broadcast: $barcode")
                notifyScanSuccess(barcode.trim())
            }
        }
    }

    /**
     * Extracts barcode string from common enterprise Android POS broadcast extras.
     */
    private fun extractBarcodeFromIntent(intent: Intent): String? {
        val possibleExtras = listOf(
            // Sunmi POS
            "data",
            // Zebra DataWedge
            "com.symbol.datawedge.data_string",
            // Honeywell
            "barcode_data",
            // Newland & Urovo
            "value",
            "barcode",
            "scannerdata",
            "scan_result",
            "text"
        )

        for (extra in possibleExtras) {
            val value = intent.getStringExtra(extra)
            if (!value.isNullOrBlank()) {
                return value
            }
        }

        // Check byte array payload if string is not present
        val byteData = intent.getByteArrayExtra("data")
        if (byteData != null && byteData.isNotEmpty()) {
            return String(byteData).trim()
        }

        return null
    }

    /**
     * Start listening for enterprise scanner broadcast intents.
     */
    fun startListening() {
        if (isListening) return

        val filter = IntentFilter().apply {
            // Sunmi
            addAction("com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED")
            // Zebra DataWedge
            addAction("com.symbol.datawedge.api.RESULT_ACTION")
            addAction("com.symbol.datawedge.api.ACTION_SOFTSCANTRIGGER")
            // Honeywell
            addAction("com.honeywell.decode.intent.action.EDIT_DATA")
            // Newland
            addAction("nlscan.action.SCANNER_RESULT")
            // Urovo
            addAction("urovo.rcv.POST_SCAN")
            // Generic Android Barcode Scanners
            addAction("android.intent.action.SCANRESULT")
            addAction("com.android.server.scannerservice.broadcast")
        }

        try {
            ContextCompat.registerReceiver(
                context,
                enterpriseBarcodeReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            isListening = true
            Log.d(tag, "Enterprise barcode broadcast receiver registered.")
        } catch (e: Exception) {
            Log.w(tag, "Failed to register enterprise barcode receiver: ${e.message}")
        }
    }

    /**
     * Stop listening to enterprise broadcasts.
     */
    fun stopListening() {
        if (!isListening) return
        try {
            context.unregisterReceiver(enterpriseBarcodeReceiver)
            isListening = false
            Log.d(tag, "Enterprise barcode broadcast receiver unregistered.")
        } catch (e: Exception) {
            Log.w(tag, "Failed to unregister receiver: ${e.message}")
        }
    }

    /**
     * Processes key events from Activity.dispatchKeyEvent.
     *
     * @param event The key event dispatched to the window.
     * @return true if the event was consumed by the hardware scanner; false to allow standard propagation.
     */
    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            // If burst mode is currently active from a barcode scan, consume up actions too
            return isBurstModeActive
        }

        val currentTime = System.currentTimeMillis()
        val interval = currentTime - lastKeystrokeTime

        // If time between keystrokes exceeds the threshold, this is not a rapid burst -> reset buffer
        if (buffer.isNotEmpty() && interval > maxKeystrokeIntervalMs) {
            buffer.clear()
            isBurstModeActive = false
        }
        lastKeystrokeTime = currentTime

        // Check for barcode terminator (Enter key)
        if (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            val scannedCode = buffer.toString().trim()
            val wasValid = scannedCode.length >= minBarcodeLength

            buffer.clear()
            isBurstModeActive = false

            if (wasValid) {
                Log.d(tag, "Hardware HID barcode captured: $scannedCode")
                notifyScanSuccess(scannedCode)
                return true // Consume enter event
            }
            return false
        }

        // Convert key code to character using device key map
        val unicodeChar = event.keyCharacterMap.get(event.keyCode, event.metaState)
        if (unicodeChar != 0 && !Character.isISOControl(unicodeChar)) {
            val char = unicodeChar.toChar()
            buffer.append(char)

            // When consecutive keys arrive faster than human typing, engage burst mode
            if (buffer.length >= 2 && interval <= maxKeystrokeIntervalMs) {
                isBurstModeActive = true
            }

            // Consume characters if hardware scanner burst is detected to avoid leaking into inputs
            return isBurstModeActive
        }

        return false
    }

    /**
     * Plays acoustic beep and haptic feedback, then dispatches the scanned barcode.
     */
    private fun notifyScanSuccess(barcode: String) {
        if (enableAudioFeedback) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
            } catch (e: Exception) {
                Log.w(tag, "Tone playback error: ${e.message}")
            }
        }

        if (enableHapticFeedback && vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(40)
                }
            } catch (e: Exception) {
                Log.w(tag, "Haptic feedback error: ${e.message}")
            }
        }

        onBarcodeScanned(barcode)
    }

    /**
     * Release system resources when activity is destroyed.
     */
    fun release() {
        stopListening()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}
