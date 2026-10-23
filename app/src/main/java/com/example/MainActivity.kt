package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.pos.domain.HardwareBarcodeScanner
import com.example.pos.ui.OmniPosApp
import com.example.pos.ui.PosViewModel
import com.example.ui.theme.OmniPosTheme

class MainActivity : FragmentActivity() {

    private val posViewModel: PosViewModel by viewModels()
    private lateinit var hardwareBarcodeScanner: HardwareBarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Universal Hardware Barcode Scanner Listener
        hardwareBarcodeScanner = HardwareBarcodeScanner(
            context = this,
            enableAudioFeedback = true,
            enableHapticFeedback = true,
            onBarcodeScanned = { barcode ->
                posViewModel.onHardwareBarcodeScanned(barcode)
            }
        )

        setContent {
            OmniPosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    OmniPosApp(viewModel = posViewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        hardwareBarcodeScanner.startListening()
    }

    override fun onStop() {
        super.onStop()
        hardwareBarcodeScanner.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        hardwareBarcodeScanner.release()
    }

    /**
     * Intercepts hardware keystroke bursts from USB / Bluetooth HID barcode scanners
     * before they reach individual focusable UI elements.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::hardwareBarcodeScanner.isInitialized && hardwareBarcodeScanner.dispatchKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
