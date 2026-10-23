package com.example.pos.domain

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HardwareBarcodeScannerTest {

    private lateinit var context: Context
    private var lastScannedCode: String? = null
    private lateinit var scanner: HardwareBarcodeScanner

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        lastScannedCode = null
        scanner = HardwareBarcodeScanner(
            context = context,
            maxKeystrokeIntervalMs = 80L,
            minBarcodeLength = 3,
            enableAudioFeedback = false,
            enableHapticFeedback = false,
            onBarcodeScanned = { barcode ->
                lastScannedCode = barcode
            }
        )
    }

    @Test
    fun `test rapid HID key burst ending with Enter triggers barcode scanned`() {
        val testBarcode = "8901030383742"

        // Simulate rapid keystrokes from laser scanner
        for (char in testBarcode) {
            val keyCode = KeyEvent.keyCodeFromString("KEYCODE_$char")
            val event = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            scanner.dispatchKeyEvent(event)
        }

        // Send Enter terminator
        val enterEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
        val consumed = scanner.dispatchKeyEvent(enterEvent)

        assertTrue("Enter key should be consumed by barcode scanner", consumed)
        assertEquals("Barcode should match scanned string", testBarcode, lastScannedCode)
    }

    @Test
    fun `test slow human typing resets buffer and does not emit barcode`() {
        // First character
        val event1 = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_1)
        scanner.dispatchKeyEvent(event1)

        // Wait to exceed burst threshold (simulate slow human typing)
        Thread.sleep(120)

        // Second character
        val event2 = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_2)
        scanner.dispatchKeyEvent(event2)

        // Enter key
        val enterEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
        val consumed = scanner.dispatchKeyEvent(enterEvent)

        // Since buffer had only "2" (length < minBarcodeLength), it should not be consumed as a barcode
        assertFalse(consumed)
        assertEquals(null, lastScannedCode)
    }

    @Test
    fun `test enterprise broadcast intent triggers barcode scanned`() {
        scanner.startListening()

        val broadcastIntent = Intent("com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED").apply {
            putExtra("data", "1234567890128")
        }
        context.sendBroadcast(broadcastIntent)
        ShadowLooper.idleMainLooper()

        assertEquals("1234567890128", lastScannedCode)

        scanner.stopListening()
    }
}
