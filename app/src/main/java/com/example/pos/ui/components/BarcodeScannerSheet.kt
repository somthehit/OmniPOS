@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
package com.example.pos.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.pos.ui.components.PosButtonGradients
import com.example.pos.ui.components.PosCompactPillButton
import com.example.pos.ui.components.PosDestructiveButton
import com.example.pos.ui.components.PosGradientButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.pos.domain.BarcodeUtils
import com.example.ui.theme.PosEmerald
import com.example.ui.theme.PosRose
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.example.pos.ui.i18n.LocalPosStrings
import com.example.pos.ui.i18n.NepaliStrings
import java.util.concurrent.Executors

data class ScannedHistoryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val barcode: String,
    val format: String,
    val timestamp: Long = System.currentTimeMillis()
)

@androidx.camera.core.ExperimentalGetImage
@Composable
fun BarcodeScannerSheet(
    isContinuousModeDefault: Boolean = false,
    onDismiss: () -> Unit,
    onBarcodeDetected: (barcode: String, isContinuous: Boolean) -> Unit,
    onUndoLastScan: (() -> Unit)? = null
) {
    val localContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val strings = LocalPosStrings.current
    val isNepali = strings is NepaliStrings

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(localContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isContinuousMode by remember { mutableStateOf(isContinuousModeDefault) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControlRef by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var showManualInput by remember { mutableStateOf(false) }
    var manualBarcodeInput by remember { mutableStateOf("") }
    var warningToast by remember { mutableStateOf<String?>(null) }

    // History list for continuous mode undo
    val scanHistory = remember { mutableStateListOf<ScannedHistoryEntry>() }

    // Sound feedback
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            null
        }
    }

    val turnOffTorch: () -> Unit = remember(localContext) {
        {
            isTorchOn = false
            try {
                cameraControlRef?.cameraControl?.enableTorch(false)
            } catch (e: Exception) {
                Log.e("BarcodeScanner", "Failed to disable torch via CameraControl", e)
            }
            try {
                val cameraManager = localContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                cameraManager?.cameraIdList?.forEach { id ->
                    try {
                        val characteristics = cameraManager.getCameraCharacteristics(id)
                        val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                        if (hasFlash) {
                            cameraManager.setTorchMode(id, false)
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            } catch (e: Exception) {
                // Ignore fallback error
            }
        }
    }

    val closeAndDismiss: () -> Unit = remember(onDismiss, turnOffTorch) {
        {
            turnOffTorch()
            try {
                cameraProviderRef?.unbindAll()
            } catch (e: Exception) {
                Log.e("BarcodeScanner", "Error unbinding camera on dismiss", e)
            }
            onDismiss()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                isTorchOn = false
                cameraControlRef?.cameraControl?.enableTorch(false)
            } catch (e: Exception) {
                // Ignore
            }
            try {
                cameraProviderRef?.unbindAll()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                val cameraManager = localContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                cameraManager?.cameraIdList?.forEach { id ->
                    try {
                        val characteristics = cameraManager.getCameraCharacteristics(id)
                        val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                        if (hasFlash) {
                            cameraManager.setTorchMode(id, false)
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
            try {
                cameraExecutor.shutdown()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                toneGenerator?.release()
            } catch (e: Exception) {
                // Ignore cleanup error
            }
        }
    }

    // Debounce tracker (~1200ms debounce as required)
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedTimestamp by remember { mutableStateOf(0L) }

    fun processBarcode(rawCode: String) {
        val code = rawCode.trim()
        if (code.isEmpty()) return

        val now = System.currentTimeMillis()
        if (code == lastScannedCode && (now - lastScannedTimestamp) < 1200L) {
            return // Debounce repeat scan
        }
        lastScannedCode = code
        lastScannedTimestamp = now

        // Checksum verification
        val validation = BarcodeUtils.validateBarcode(code)
        if (validation.warning != null) {
            warningToast = validation.warning
        } else {
            warningToast = null
        }

        // Haptic & Beep feedback
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (e: Exception) {
            Log.e("BarcodeScanner", "Feedback error", e)
        }

        // Record in history
        scanHistory.add(0, ScannedHistoryEntry(barcode = code, format = validation.format))

        onBarcodeDetected(code, isContinuousMode)

        if (!isContinuousMode) {
            closeAndDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = closeAndDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("barcode_scanner_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PosEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = PosEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isNepali) "बारकोड स्क्यानर" else "Barcode Scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isNepali) "EAN-13, EAN-8, UPC, Code128 र QR समर्थित" else "Supports EAN-13, EAN-8, UPC, Code128 & QR",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showManualInput = !showManualInput }) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = if (isNepali) "म्यानुअल कोड प्रविष्टि" else "Manual Code Entry",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = closeAndDismiss) {
                        Icon(Icons.Default.Close, contentDescription = if (isNepali) "बन्द गर्नुहोस्" else "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Continuous Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isNepali) "निरन्तर दाखिला मोड" else "Continuous Receive Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isNepali) "प्रत्येक दोहोरिएको स्क्यानले ब्याच परिमाणमा +प्याक मात्रा थप्छ" else "Each repeat scan adds +packQty to batch quantity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isContinuousMode,
                    onCheckedChange = { isContinuousMode = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = PosEmerald, checkedTrackColor = PosEmerald.copy(alpha = 0.3f))
                )
            }

            // Warning Banner on Checksum Failure
            AnimatedVisibility(visible = warningToast != null) {
                warningToast?.let { warn ->
                    Surface(
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "⚠ $warn",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Manual Code Input Section (fallback & handheld trigger)
            AnimatedVisibility(visible = showManualInput || !hasCameraPermission) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualBarcodeInput,
                            onValueChange = { manualBarcodeInput = it },
                            placeholder = { Text(if (isNepali) "बारकोड टाइप वा पेस्ट गर्नुहोस्..." else "Type or paste barcode...") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        PosCompactPillButton(
                            text = if (isNepali) "प्रविष्ट गर्नुहोस्" else "Enter",
                            gradient = PosButtonGradients.Emerald,
                            onClick = {
                                if (manualBarcodeInput.isNotBlank()) {
                                    processBarcode(manualBarcodeInput)
                                    manualBarcodeInput = ""
                                }
                            }
                        )
                    }
                }
            }

            // Half-Height Camera View Finder (Height ~280dp)
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    cameraProviderRef = cameraProvider
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }

                                    val options = BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(
                                            Barcode.FORMAT_EAN_13,
                                            Barcode.FORMAT_EAN_8,
                                            Barcode.FORMAT_UPC_A,
                                            Barcode.FORMAT_UPC_E,
                                            Barcode.FORMAT_CODE_128,
                                            Barcode.FORMAT_QR_CODE
                                        )
                                        .build()

                                    val scanner = BarcodeScanning.getClient(options)

                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                            scanner.process(image)
                                                .addOnSuccessListener { barcodes ->
                                                    for (barcode in barcodes) {
                                                        val raw = barcode.rawValue
                                                        if (!raw.isNullOrBlank()) {
                                                            processBarcode(raw)
                                                            break
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    cameraProvider.unbindAll()
                                    val camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                    cameraControlRef = camera

                                } catch (e: Exception) {
                                    Log.e("BarcodeScanner", "Camera init failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        onRelease = {
                            try {
                                turnOffTorch()
                                cameraProviderRef?.unbindAll()
                            } catch (e: Exception) {
                                // Ignore cleanup error
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Target Framing Box with Green Accent
                    Box(
                        modifier = Modifier
                            .size(width = 240.dp, height = 130.dp)
                            .align(Alignment.Center)
                            .border(3.dp, PosEmerald, RoundedCornerShape(14.dp))
                    )

                    // Torch Flash Toggle in Top-Right
                    IconButton(
                        onClick = {
                            val newTorch = !isTorchOn
                            isTorchOn = newTorch
                            cameraControlRef?.cameraControl?.enableTorch(newTorch)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (isTorchOn) Color(0xFFFACC15) else Color.White
                        )
                    }

                    // Framing Guidance Text at Bottom
                    Text(
                        text = if (isNepali) "हरियो फ्रेम भित्र बारकोड वा QR देखाउनुहोस्" else "Point at barcode or QR inside green frame",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (isNepali) "क्यामेरा अनुमति आवश्यक छ" else "Camera Permission Required", fontWeight = FontWeight.Bold)
                        Text(
                            if (isNepali) "बारकोड सिधै स्क्यान गर्न क्यामेरा अनुमति दिनुहोस्, वा तल म्यानुअल/ह्यान्डहेल्ड स्क्यानर प्रयोग गर्नुहोस्।" else "Enable camera permission to scan barcodes directly, or use keyboard wedge / handheld scanner below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PosGradientButton(
                            text = if (isNepali) "अनुमति दिनुहोस्" else "Grant Permission",
                            gradient = PosButtonGradients.Emerald,
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                        )
                    }
                }
            }

            // Continuous Mode Scan History & Undo Bar
            if (isContinuousMode && scanHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNepali) "स्क्यान गरिएको (${scanHistory.size} सामान):" else "Scanned (${scanHistory.size} items):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (onUndoLastScan != null) {
                        PosDestructiveButton(
                            text = if (isNepali) "पछिल्लो रद्द" else "Undo Last",
                            icon = Icons.AutoMirrored.Filled.Undo,
                            onClick = {
                                if (scanHistory.isNotEmpty()) {
                                    scanHistory.removeAt(0)
                                    onUndoLastScan()
                                }
                            }
                        )
                    }
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(scanHistory, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PosEmerald.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PosEmerald.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${item.barcode} (${item.format})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = PosEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
