package com.example.pos.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// build.gradle(.kts):  implementation("com.google.zxing:core:3.5.3")

object Ean13 {
    fun checkDigit(first12: String): Int {
        require(first12.length == 12 && first12.all(Char::isDigit)) { "Need exactly 12 digits" }
        val sum = first12.mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 1 else 3 }.sum()
        return (10 - sum % 10) % 10
    }

    fun isValid(code: String): Boolean =
        code.length == 13 && code.all(Char::isDigit) && checkDigit(code.take(12)) == (code.last() - '0')

    /**
     * In-store code for items without a manufacturer barcode.
     * GS1 prefixes 20-29 are reserved for internal use, so this can never collide with a real product barcode.
     * Keep `sequence` unique per tenant (DB sequence) and keep a unique index on the barcode column.
     */
    fun internal(sequence: Long): String {
        require(sequence in 0..9_999_999_999L)
        val body = "20" + sequence.toString().padStart(10, '0')
        return body + checkDigit(body)
    }
}

object BarcodeRenderer {
    private val writer = MultiFormatWriter()

    // ZXing counts the quiet zone as a TOTAL for both sides. 20 => 10 modules each side (Code128 minimum).
    private val hints = mapOf(EncodeHintType.MARGIN to 20)

    /** Valid 13-digit EAN => EAN_13, anything else (SKU text, short numbers) => CODE_128. */
    fun formatFor(code: String): BarcodeFormat =
        if (Ean13.isValid(code)) BarcodeFormat.EAN_13 else BarcodeFormat.CODE_128

    /** Width in modules including the quiet zone. */
    private fun naturalModules(code: String, format: BarcodeFormat): Int =
        writer.encode(code, format, 0, 0, hints).width

    /**
     * Real barcode bitmap. Every module is an INTEGER number of pixels, so bars stay sharp and scannable.
     * Thermal printers at 203 dpi: use modulePx >= 2.
     */
    fun barcode(
        code: String,
        format: BarcodeFormat = formatFor(code),
        modulePx: Int = 3,
        heightPx: Int = 120,
    ): Bitmap {
        val natural = naturalModules(code, format)
        val m = writer.encode(code, format, natural * modulePx, heightPx, hints)
        val w = m.width
        val h = m.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) pixels[row + x] = if (m[x, y]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Full price barcode label layout (matches standard retail sticker layout):
     * - Left side: Prominent boxed price display (PRICE / Rs, big price numbers)
     * - Right side: Item Name & SKU header, scannable barcode graphics, and barcode number underneath.
     */
    fun label(
        name: String,
        sku: String,
        code: String,
        priceText: String = "",
        currencySymbol: String = "Rs",
        widthPx: Int = 400,
        heightPx: Int = 200,
        format: BarcodeFormat = formatFor(code),
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val s = widthPx / 400f
        val pad = 10f * s

        // 1. LEFT SIDE PRICE BOX (Prominent price badge like reference image)
        val priceBoxWidth = 125f * s
        val priceBoxRect = RectF(pad, pad, pad + priceBoxWidth, heightPx - pad)

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * s
        }
        canvas.drawRect(priceBoxRect, boxPaint)

        val priceBoxCenterX = priceBoxRect.centerX()

        // Price Header "PRICE / Rs"
        val priceHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 15f * s
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val cleanCurrency = if (currencySymbol.isNotBlank()) currencySymbol.replace(".", "").trim() else "Rs"
        canvas.drawText("PRICE / $cleanCurrency", priceBoxCenterX, priceBoxRect.top + 22f * s, priceHeaderPaint)

        // Large Price Value "155.00" or "155"
        val cleanPrice = if (priceText.isNotBlank()) priceText.replace(currencySymbol, "").trim() else "0.00"
        val priceValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = if (cleanPrice.length > 6) 22f * s else 30f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(cleanPrice, priceBoxCenterX, priceBoxRect.centerY() + 12f * s, priceValuePaint)

        // 2. RIGHT SIDE DETAILS & BARCODE
        val rightLeft = priceBoxRect.right + 12f * s
        val rightWidth = widthPx - pad - rightLeft
        val rightCenterX = rightLeft + rightWidth / 2f

        // Top Header Text: ITEM NAME/SKU: Name / SKU
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 13f * s
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
        val rawHeader = "ITEM NAME/SKU: $name / $sku"
        val shownHeader = TextUtils.ellipsize(rawHeader, headerPaint, rightWidth, TextUtils.TruncateAt.END).toString()
        canvas.drawText(shownHeader, rightLeft, pad + 16f * s, headerPaint)

        // Human-readable barcode number at bottom
        val numPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 16f * s
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        val numberBaselineY = heightPx - pad - 2f * s
        canvas.drawText(code, rightCenterX, numberBaselineY, numPaint)

        // Barcode Graphic in center
        val barTopY = pad + 24f * s
        val barBottomY = numberBaselineY - numPaint.textSize - 2f * s
        val barHeight = (barBottomY - barTopY).toInt().coerceAtLeast(30)

        val natural = naturalModules(code, format)
        val modulePx = ((rightWidth).toInt() / natural).coerceAtLeast(1)
        val bars = barcode(code, format, modulePx, barHeight)
        canvas.drawBitmap(bars, (rightCenterX - bars.width / 2f).coerceAtLeast(rightLeft), barTopY, null)

        return bmp
    }
}

data class LabelSizePreset(
    val name: String,
    val widthMm: Float,
    val heightMm: Float,
    val widthPx: Int = (widthMm * 8f).toInt(),
    val heightPx: Int = (heightMm * 8f).toInt()
)

val DEFAULT_LABEL_PRESETS = listOf(
    LabelSizePreset("50×30 mm (Std)", 50f, 30f, 400, 240),
    LabelSizePreset("50×25 mm (Compact)", 50f, 25f, 400, 200),
    LabelSizePreset("40×30 mm (Square)", 40f, 30f, 320, 240),
    LabelSizePreset("38×25 mm (Mini Tag)", 38f, 25f, 304, 200),
    LabelSizePreset("58×40 mm (Large Shelf)", 58f, 40f, 464, 320)
)

/** Drop-in replacement for the fake bars in the "Internal Code128 label" dialog. */
@Composable
fun BarcodeLabelPreview(
    name: String,
    sku: String,
    code: String,
    priceText: String = "",
    currencySymbol: String = "Rs",
    widthPx: Int = 400,
    heightPx: Int = 240,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, name, sku, code, priceText, currencySymbol, widthPx, heightPx) {
        value = withContext(Dispatchers.Default) {
            BarcodeRenderer.label(name, sku, code, priceText, currencySymbol, widthPx = widthPx, heightPx = heightPx)
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Barcode label for $name",
            modifier = modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
            filterQuality = FilterQuality.None,        // keep bar edges hard on screen
        )
    }
}

/**
 * One page per label at the exact label size (default 50x30 mm), `copies` pages.
 * Typical use: copies = quantity received in the stock-in.
 * Render the label bitmap at 800x480 for this path.
 */
fun buildLabelPdf(
    context: Context,
    label: Bitmap,
    copies: Int,
    labelWidthMm: Float = 50f,
    labelHeightMm: Float = 30f,
): File {
    val w = (labelWidthMm / 25.4f * 72f).toInt()       // PDF points
    val h = (labelHeightMm / 25.4f * 72f).toInt()
    val paint = Paint().apply { isFilterBitmap = false }
    val doc = PdfDocument()
    repeat(copies.coerceIn(1, 500)) { i ->
        val page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, i + 1).create())
        page.canvas.drawBitmap(label, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), paint)
        doc.finishPage(page)
    }
    val file = File(context.cacheDir, "labels_${System.currentTimeMillis()}.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    return file
}
