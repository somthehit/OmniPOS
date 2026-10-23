package com.example.pos.domain

import java.util.Locale

data class BarcodeValidationResult(
    val isValid: Boolean,
    val format: String,
    val checksumValid: Boolean,
    val warning: String? = null
)

object BarcodeUtils {

    /**
     * Calculates the EAN-13 check digit for the first 12 digits using modulo-10.
     */
    fun calculateEan13Checksum(first12Digits: String): Int {
        val digits = first12Digits.filter { it.isDigit() }.map { it.digitToInt() }
        var sum = 0
        for (i in 0 until minOf(12, digits.size)) {
            sum += if (i % 2 == 0) digits[i] * 1 else digits[i] * 3
        }
        return (10 - (sum % 10)) % 10
    }

    /**
     * Validates EAN-13 barcodes using the standard modulo-10 algorithm.
     * Odd positions (1st, 3rd, ...) have weight 1, even positions (2nd, 4th, ...) have weight 3.
     */
    fun validateEan13(code: String): Boolean {
        if (code.length != 13 || !code.all { it.isDigit() }) return false
        val digits = code.map { it.digitToInt() }
        val expectedCheckDigit = calculateEan13Checksum(code.substring(0, 12))
        return digits[12] == expectedCheckDigit
    }

    /**
     * Validates EAN-8 barcodes using the standard modulo-10 algorithm.
     * Odd positions (1st, 3rd, 5th, 7th) have weight 3, even positions (2nd, 4th, 6th) have weight 1.
     */
    fun validateEan8(code: String): Boolean {
        if (code.length != 8 || !code.all { it.isDigit() }) return false
        val digits = code.map { it.digitToInt() }
        var sum = 0
        for (i in 0 until 7) {
            sum += if (i % 2 == 0) digits[i] * 3 else digits[i] * 1
        }
        val expectedCheckDigit = (10 - (sum % 10)) % 10
        return digits[7] == expectedCheckDigit
    }

    /**
     * Validates UPC-A barcodes (12 digits).
     * Odd positions have weight 3, even positions have weight 1.
     */
    fun validateUpcA(code: String): Boolean {
        if (code.length != 12 || !code.all { it.isDigit() }) return false
        val digits = code.map { it.digitToInt() }
        var sum = 0
        for (i in 0 until 11) {
            sum += if (i % 2 == 0) digits[i] * 3 else digits[i] * 1
        }
        val expectedCheckDigit = (10 - (sum % 10)) % 10
        return digits[11] == expectedCheckDigit
    }

    /**
     * Comprehensive barcode format and checksum check.
     * Detects format (EAN-13, EAN-8, UPC-A, UPC-E, Code128, QR) and warns on checksum failure
     * to prevent optical misreads from polluting inventory.
     */
    fun validateBarcode(rawCode: String): BarcodeValidationResult {
        val trimmed = rawCode.trim()
        if (trimmed.isEmpty()) {
            return BarcodeValidationResult(false, "UNKNOWN", false, "Barcode is empty")
        }

        return when {
            trimmed.length == 13 && trimmed.all { it.isDigit() } -> {
                val ok = validateEan13(trimmed)
                BarcodeValidationResult(
                    isValid = ok,
                    format = "EAN-13",
                    checksumValid = ok,
                    warning = if (!ok) "Checksum mismatch on EAN-13 ($trimmed). Verify camera read." else null
                )
            }
            trimmed.length == 8 && trimmed.all { it.isDigit() } -> {
                val ok = validateEan8(trimmed)
                BarcodeValidationResult(
                    isValid = ok,
                    format = "EAN-8",
                    checksumValid = ok,
                    warning = if (!ok) "Checksum mismatch on EAN-8 ($trimmed). Verify camera read." else null
                )
            }
            trimmed.length == 12 && trimmed.all { it.isDigit() } -> {
                val ok = validateUpcA(trimmed)
                BarcodeValidationResult(
                    isValid = ok,
                    format = "UPC-A",
                    checksumValid = ok,
                    warning = if (!ok) "Checksum mismatch on UPC-A ($trimmed). Verify camera read." else null
                )
            }
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
                BarcodeValidationResult(true, "QR-URL", true)
            }
            trimmed.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' } -> {
                BarcodeValidationResult(true, "Code128", true)
            }
            else -> {
                BarcodeValidationResult(true, "Generic", true)
            }
        }
    }

    /**
     * Generates an internal Code128 barcode for items that do not have manufacturer barcodes.
     * Uses the product SKU or generates a clean standardized alphanumeric string.
     */
    fun generateInternalCode128(sku: String): String {
        val sanitized = sku.trim().filter { it.isLetterOrDigit() || it == '-' }.uppercase(Locale.US)
        return if (sanitized.isNotBlank()) {
            if (sanitized.startsWith("INT-")) sanitized else "INT-$sanitized"
        } else {
            "INT-${System.currentTimeMillis().toString().takeLast(8)}"
        }
    }
}
