package com.example.pos.domain

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

enum class PrinterInterfaceType {
    BLUETOOTH,
    USB,
    AUTO
}

data class DiscoveredPrinter(
    val name: String,
    val address: String, // MAC address for Bluetooth, Device name/ID for USB
    val type: PrinterInterfaceType,
    val isBonded: Boolean = true
)

sealed interface PrintResult {
    data class Success(val message: String) : PrintResult
    data class Error(val reason: String, val throwable: Throwable? = null) : PrintResult
    data object PermissionDenied : PrintResult
    data object DeviceNotFound : PrintResult
}

/**
 * Enterprise Thermal Printer Manager for Bluetooth SPP and USB OTG Thermal ESC/POS Printers.
 */
class ThermalPrinterManager(private val context: Context) {

    private val tag = "ThermalPrinterManager"
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var discoveryReceiver: BroadcastReceiver? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        @Suppress("DEPRECATION")
        bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    private val usbManager: UsbManager? by lazy {
        context.getSystemService(Context.USB_SERVICE) as? UsbManager
    }

    /**
     * Checks whether runtime Bluetooth connect permission is granted.
     */
    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Checks whether runtime Bluetooth scan permission is granted.
     */
    fun hasBluetoothScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Checks if Bluetooth adapter is enabled on the device.
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Extracts a MAC address formatted as XX:XX:XX:XX:XX:XX from strings like "MPT-II (00:11:22:33:44:55)".
     */
    fun extractMacAddress(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val macRegex = Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})")
        val match = macRegex.find(input)
        if (match != null) return match.value.uppercase()
        val bonded = getAvailablePrinters().find { it.name.equals(input.trim(), ignoreCase = true) }
        return bonded?.address
    }

    /**
     * Filters out non-printer devices (earbuds, smartphones, watches, computers, vehicles).
     * Recognizes thermal printers via BluetoothClass IMAGING / Printer or ESC/POS naming patterns.
     */
    /**
     * Filters out non-printer devices.
     * Printers report Bluetooth major class IMAGING (0x0600), minor PRINTER (0x0680).
     * Many cheap thermal printers report the wrong class, so also fall back to
     * a regex name match (print|pos|mpt|rpp|xp-|bt-|mtp).
     */
    @SuppressLint("MissingPermission")
    fun isPotentialPrinter(device: BluetoothDevice): Boolean {
        val devName = try {
            if (hasBluetoothPermission()) device.name?.trim() else null
        } catch (_: Exception) {
            null
        } ?: ""

        val c = try {
            device.bluetoothClass
        } catch (_: Exception) {
            null
        }

        // 1. Printer Bluetooth Class (Major IMAGING 0x0600 or Device Class PRINTER 0x0680)
        val isPrinterClass = (c?.majorDeviceClass == BluetoothClass.Device.Major.IMAGING) ||
                (c?.deviceClass == 0x0680) ||
                (c?.deviceClass == 1664)

        // 2. Fallback Printer Name Regex
        val printerRegex = Regex("print|pos|mpt|rpp|xp-|bt-|mtp|zj|thermal|receipt|esc", RegexOption.IGNORE_CASE)
        val isPrinterName = printerRegex.containsMatchIn(devName)

        // 3. Audio / Phone / Wearable / Consumer Device Filter
        val nonPrinterMajor = c?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                c?.majorDeviceClass == BluetoothClass.Device.Major.PHONE ||
                c?.majorDeviceClass == BluetoothClass.Device.Major.WEARABLE ||
                c?.majorDeviceClass == BluetoothClass.Device.Major.TOY ||
                c?.majorDeviceClass == BluetoothClass.Device.Major.HEALTH

        val nonPrinterNameRegex = Regex("buds|airpod|headset|earphone|headphone|speaker|audio|watch|band|galaxy|redmi|xiaomi|realme|oppo|vivo|iphone", RegexOption.IGNORE_CASE)
        val isNonPrinterName = nonPrinterNameRegex.containsMatchIn(devName)

        if ((nonPrinterMajor || isNonPrinterName) && !devName.contains("print", ignoreCase = true) && !devName.contains("pos", ignoreCase = true)) {
            return false
        }

        // Must match either printer class or printer name pattern
        return isPrinterClass || isPrinterName
    }

    /**
     * Returns a list of bonded (paired) Bluetooth printers and connected USB thermal printers.
     * Non-printer devices (earbuds, phones, smartwatches) are filtered out.
     */
    @SuppressLint("MissingPermission")
    fun getAvailablePrinters(showAll: Boolean = false): List<DiscoveredPrinter> {
        val list = mutableListOf<DiscoveredPrinter>()

        // 1. Discover Paired Bluetooth Printers
        if (hasBluetoothPermission() && bluetoothAdapter != null && bluetoothAdapter?.isEnabled == true) {
            try {
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
                pairedDevices?.forEach { device ->
                    if (showAll || isPotentialPrinter(device)) {
                        val devName = device.name ?: "Bluetooth Thermal Printer"
                        val devAddress = device.address
                        list.add(
                            DiscoveredPrinter(
                                name = devName,
                                address = devAddress,
                                type = PrinterInterfaceType.BLUETOOTH,
                                isBonded = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to read paired Bluetooth devices: ${e.message}")
            }
        }

        // 2. Discover Connected USB Printers
        usbManager?.let { manager ->
            try {
                val deviceList: Map<String, UsbDevice> = manager.deviceList
                for ((name, device) in deviceList) {
                    val isPrinter = isUsbPrinter(device)
                    if (isPrinter) {
                        val displayName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            device.productName ?: device.deviceName ?: name
                        } else {
                            device.deviceName ?: name
                        }
                        list.add(
                            DiscoveredPrinter(
                                name = displayName,
                                address = device.deviceName,
                                type = PrinterInterfaceType.USB
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to inspect USB devices: ${e.message}")
            }
        }

        return list
    }

    /**
     * Checks if a USB device has interface class 7 (Printer).
     */
    private fun isUsbPrinter(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) return true
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
        }
        return false
    }

    /**
     * Dispatches raw ESC/POS byte array to configured printer or auto-detects first available printer.
     */
    suspend fun print(
        bytes: ByteArray,
        targetAddress: String? = null,
        preferredType: PrinterInterfaceType = PrinterInterfaceType.AUTO
    ): PrintResult = withContext(Dispatchers.IO) {
        val available = getAvailablePrinters()

        if (available.isEmpty()) {
            return@withContext PrintResult.DeviceNotFound
        }

        // 1. Resolve Target Printer
        val selectedPrinter: DiscoveredPrinter? = if (!targetAddress.isNullOrBlank()) {
            val cleanTargetMac = extractMacAddress(targetAddress)
            available.find { p ->
                (cleanTargetMac != null && p.address.equals(cleanTargetMac, ignoreCase = true)) ||
                p.address.equals(targetAddress, ignoreCase = true) ||
                p.name.equals(targetAddress, ignoreCase = true)
            }
        } else {
            // Auto heuristic: Prefer known printer naming patterns
            available.find { p ->
                val lower = p.name.lowercase()
                lower.contains("print") || lower.contains("pos") || lower.contains("mpt") ||
                        lower.contains("rpp") || lower.contains("sunmi") || lower.contains("thermal") ||
                        lower.contains("xp-") || p.type == PrinterInterfaceType.USB
            }
        }

        if (selectedPrinter == null) {
            return@withContext PrintResult.DeviceNotFound
        }

        when (selectedPrinter.type) {
            PrinterInterfaceType.BLUETOOTH -> printViaBluetooth(selectedPrinter.address, bytes)
            PrinterInterfaceType.USB -> printViaUsb(selectedPrinter.address, bytes)
            PrinterInterfaceType.AUTO -> {
                val btResult = printViaBluetooth(selectedPrinter.address, bytes)
                if (btResult is PrintResult.Success) btResult else printViaUsb(selectedPrinter.address, bytes)
            }
        }
    }

    /**
     * Actively scans for nearby discoverable Bluetooth devices using Android BluetoothAdapter discovery.
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery(
        onPrinterFound: (DiscoveredPrinter) -> Unit,
        onFinished: () -> Unit
    ) {
        if (!hasBluetoothScanPermission() || !hasBluetoothPermission()) {
            onFinished()
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            onFinished()
            return
        }

        stopDiscovery()

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        if (device != null && isPotentialPrinter(device)) {
                            val name = try {
                                if (hasBluetoothPermission()) device.name ?: "Nearby Thermal Printer" else "Nearby Thermal Printer"
                            } catch (_: Exception) {
                                "Nearby Thermal Printer"
                            }
                            val isBonded = device.bondState == BluetoothDevice.BOND_BONDED
                            onPrinterFound(
                                DiscoveredPrinter(
                                    name = name,
                                    address = device.address,
                                    type = PrinterInterfaceType.BLUETOOTH,
                                    isBonded = isBonded
                                )
                            )
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        stopDiscovery()
                        onFinished()
                    }
                }
            }
        }

        discoveryReceiver = receiver
        context.registerReceiver(receiver, filter)
        adapter.startDiscovery()
    }

    /**
     * Cancels any ongoing Bluetooth discovery and unregisters the discovery receiver.
     */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            if (hasBluetoothScanPermission()) {
                bluetoothAdapter?.cancelDiscovery()
            }
        } catch (_: Exception) {}
        discoveryReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
            discoveryReceiver = null
        }
    }

    /**
     * Verifies and tests connection to a Bluetooth printer or USB printer.
     * Uses multi-tier fallback (Standard UUID -> Reflection Channel 1 -> Insecure RFCOMM).
     */
    @SuppressLint("MissingPermission")
    suspend fun connectPrinter(targetAddress: String): PrintResult = withContext(Dispatchers.IO) {
        val cleanMac = extractMacAddress(targetAddress) ?: targetAddress
        if (!BluetoothAdapter.checkBluetoothAddress(cleanMac)) {
            val usbDevice = getAvailablePrinters().find { it.address == targetAddress && it.type == PrinterInterfaceType.USB }
            if (usbDevice != null) {
                return@withContext PrintResult.Success("USB Printer detected: ${usbDevice.name}")
            }
            return@withContext PrintResult.Error("Invalid Bluetooth MAC address: $targetAddress")
        }

        if (!hasBluetoothPermission()) {
            return@withContext PrintResult.PermissionDenied
        }

        val adapter = bluetoothAdapter ?: return@withContext PrintResult.Error("Bluetooth hardware unavailable on device")
        if (!adapter.isEnabled) {
            return@withContext PrintResult.Error("Bluetooth is turned off. Please enable Bluetooth.")
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device = adapter.getRemoteDevice(cleanMac)
            val devName = try {
                if (hasBluetoothPermission()) device.name ?: cleanMac else cleanMac
            } catch (_: Exception) {
                cleanMac
            }

            if (!isPotentialPrinter(device)) {
                return@withContext PrintResult.Error("Device '$devName' is an audio or phone device, not a thermal printer.")
            }

            withTimeout(8000L) {
                try {
                    adapter.cancelDiscovery()
                } catch (_: Exception) {}

                socket = openSocketWithFallback(device)
                outputStream = socket?.outputStream
                // Send ESC @ (Init printer command) to verify communication
                val initCmd = byteArrayOf(0x1B, 0x40)
                outputStream?.write(initCmd)
                outputStream?.flush()
            }

            Log.i(tag, "Successfully verified connection to Bluetooth printer $devName ($cleanMac)")
            PrintResult.Success("Connected to $devName successfully!")
        } catch (e: Exception) {
            Log.e(tag, "Failed to connect to Bluetooth printer $cleanMac: ${e.message}", e)
            val msg = when {
                e.message?.contains("closed", ignoreCase = true) == true ||
                e.message?.contains("refused", ignoreCase = true) == true -> "Connection refused. Please ensure printer is powered on and in range."
                e.message?.contains("timeout", ignoreCase = true) == true -> "Connection timed out. Check printer battery and distance."
                else -> e.message ?: "Failed to connect to Bluetooth printer"
            }
            PrintResult.Error(msg, e)
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Establishes a Bluetooth RFCOMM socket with robust multi-tier fallback:
     * 1. Standard SPP UUID (00001101-0000-1000-8000-00805F9B34FB)
     * 2. Reflection channel 1 fallback (essential for most portable 58mm/80mm POS printers)
     * 3. Insecure RFCOMM socket fallback
     */
    @SuppressLint("MissingPermission")
    private fun openSocketWithFallback(device: BluetoothDevice): BluetoothSocket {
        var socket: BluetoothSocket? = null
        var lastException: Exception? = null

        // Attempt 1: Standard SPP UUID
        try {
            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket.connect()
            if (socket.isConnected) return socket
        } catch (e: Exception) {
            lastException = e
            Log.w(tag, "Standard SPP UUID connect failed: ${e.message}, trying channel 1 reflection fallback...")
            try { socket?.close() } catch (_: Exception) {}
        }

        // Attempt 2: Channel 1 reflection fallback
        try {
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            socket = method.invoke(device, 1) as? BluetoothSocket
            socket?.connect()
            if (socket != null && socket.isConnected) {
                Log.i(tag, "Connected to printer via RFCOMM Channel 1 fallback.")
                return socket
            }
        } catch (e: Exception) {
            lastException = e
            Log.w(tag, "Reflection channel 1 connect failed: ${e.message}, trying insecure SPP...")
            try { socket?.close() } catch (_: Exception) {}
        }

        // Attempt 3: Insecure RFCOMM fallback
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(sppUuid)
            socket.connect()
            if (socket.isConnected) {
                Log.i(tag, "Connected to printer via Insecure SPP fallback.")
                return socket
            }
        } catch (e: Exception) {
            lastException = e
            Log.e(tag, "Insecure SPP connect also failed: ${e.message}")
            try { socket?.close() } catch (_: Exception) {}
        }

        throw lastException ?: IOException("Failed to connect to Bluetooth printer ${device.address}")
    }

    /**
     * Prints raw bytes via Bluetooth Serial Port Profile (SPP) with multi-tier fallback.
     */
    @SuppressLint("MissingPermission")
    private suspend fun printViaBluetooth(macAddress: String, bytes: ByteArray): PrintResult = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission()) {
            return@withContext PrintResult.PermissionDenied
        }

        val adapter = bluetoothAdapter ?: return@withContext PrintResult.Error("Bluetooth hardware not available on this device")
        if (!adapter.isEnabled) {
            return@withContext PrintResult.Error("Bluetooth is currently turned off")
        }

        val cleanMac = extractMacAddress(macAddress) ?: macAddress
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device = adapter.getRemoteDevice(cleanMac)
            val devName = try {
                if (hasBluetoothPermission()) device.name ?: cleanMac else cleanMac
            } catch (_: Exception) {
                cleanMac
            }

            if (!isPotentialPrinter(device)) {
                return@withContext PrintResult.Error("Device '$devName' is an audio or phone device, not a thermal printer.")
            }

            // Connect with 10-second timeout
            withTimeout(10000L) {
                try {
                    adapter.cancelDiscovery()
                } catch (_: Exception) {}

                socket = openSocketWithFallback(device)
                outputStream = socket?.outputStream
                outputStream?.write(bytes)
                outputStream?.flush()
            }

            Log.i(tag, "Successfully printed ${bytes.size} ESC/POS bytes to $devName ($cleanMac)")
            PrintResult.Success("Receipt printed successfully via Bluetooth ($devName)")
        } catch (e: Exception) {
            Log.e(tag, "Bluetooth thermal print error: ${e.message}", e)
            PrintResult.Error("Bluetooth print failed: ${e.message ?: "Could not connect to printer"}", e)
        } finally {
            try {
                outputStream?.close()
            } catch (_: IOException) {}
            try {
                socket?.close()
            } catch (_: IOException) {}
        }
    }

    /**
     * Prints raw bytes via USB OTG / Direct Host Cable.
     */
    private suspend fun printViaUsb(deviceName: String, bytes: ByteArray): PrintResult = withContext(Dispatchers.IO) {
        val manager = usbManager ?: return@withContext PrintResult.Error("USB Host manager unavailable")
        val device = manager.deviceList.values.find { it.deviceName == deviceName }
            ?: return@withContext PrintResult.DeviceNotFound

        if (!manager.hasPermission(device)) {
            return@withContext PrintResult.Error("USB permission not granted for ${device.deviceName}")
        }

        var connection: UsbDeviceConnection? = null
        var usbInterface: UsbInterface? = null
        var outEndpoint: UsbEndpoint? = null

        try {
            // Find OUT Bulk Endpoint
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.direction == UsbConstants.USB_DIR_OUT && ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        usbInterface = iface
                        outEndpoint = ep
                        break
                    }
                }
                if (outEndpoint != null) break
            }

            if (usbInterface == null || outEndpoint == null) {
                return@withContext PrintResult.Error("No USB printer output endpoint found on ${device.deviceName}")
            }

            connection = manager.openDevice(device)
                ?: return@withContext PrintResult.Error("Failed to open USB device connection")

            val claimed = connection.claimInterface(usbInterface, true)
            if (!claimed) {
                return@withContext PrintResult.Error("Failed to claim USB printer interface")
            }

            // Transfer in chunks of 4096 bytes
            var offset = 0
            val chunkSize = 4096
            while (offset < bytes.size) {
                val length = (bytes.size - offset).coerceAtMost(chunkSize)
                val chunk = bytes.copyOfRange(offset, offset + length)
                val transferred = connection.bulkTransfer(outEndpoint, chunk, chunk.size, 5000)
                if (transferred < 0) {
                    return@withContext PrintResult.Error("USB bulk transfer interrupted at byte $offset")
                }
                offset += length
            }

            Log.i(tag, "Successfully printed ${bytes.size} ESC/POS bytes to USB printer ${device.deviceName}")
            PrintResult.Success("Receipt printed successfully via USB (${device.deviceName})")
        } catch (e: Exception) {
            Log.e(tag, "USB print error: ${e.message}", e)
            PrintResult.Error("USB print failed: ${e.message}", e)
        } finally {
            try {
                if (usbInterface != null) connection?.releaseInterface(usbInterface)
                connection?.close()
            } catch (_: Exception) {}
        }
    }
}
