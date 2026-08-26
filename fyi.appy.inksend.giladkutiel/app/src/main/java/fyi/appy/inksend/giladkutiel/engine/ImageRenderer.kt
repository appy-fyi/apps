package fyi.appy.inksend.giladkutiel.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import fyi.appy.inksend.giladkutiel.data.model.TextStyleConfig
import java.io.File
import java.io.FileOutputStream

/**
 * Renders arbitrary text into a styled PNG bitmap using native Canvas/StaticLayout,
 * writes it to the app's cache dir, and returns a FileProvider content URI for it.
 */
object ImageRenderer {

    private const val CANVAS_SIZE_PX = 512
    private const val MIN_FONT_SIZE_PX = 12f
    private const val MAX_FONT_SIZE_PX = 320f
    private const val FONT_SIZE_SEARCH_STEPS = 20

    fun renderBitmap(context: Context, text: String, config: TextStyleConfig): Bitmap {
        val density = context.resources.displayMetrics.density
        val paddingPx = (config.paddingDp * density).toInt().coerceIn(0, CANVAS_SIZE_PX / 4)
        val maxContentSize = (CANVAS_SIZE_PX - paddingPx * 2).coerceAtLeast(1)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColorOrDefault(config.textColorHex, Color.WHITE)
            typeface = Typeface.create(config.font.typefaceName, Typeface.BOLD)
        }

        // Always render onto a fixed 512x512 square, so font size is the only free
        // variable: binary-search the largest size whose wrapped layout still fits.
        textPaint.textSize = findFillingFontSizePx(text, textPaint, maxContentSize, maxContentSize)

        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, maxContentSize)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(true)
            .build()

        val bitmap = Bitmap.createBitmap(CANVAS_SIZE_PX, CANVAS_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            if (config.isGradientEnabled) {
                shader = LinearGradient(
                    0f, 0f, CANVAS_SIZE_PX.toFloat(), CANVAS_SIZE_PX.toFloat(),
                    parseColorOrDefault(config.backgroundColorHex, Color.DKGRAY),
                    parseColorOrDefault(config.gradientEndColorHex, Color.DKGRAY),
                    Shader.TileMode.CLAMP,
                )
            } else {
                color = parseColorOrDefault(config.backgroundColorHex, Color.DKGRAY)
            }
        }

        // Full square, no border radius.
        canvas.drawRect(0f, 0f, CANVAS_SIZE_PX.toFloat(), CANVAS_SIZE_PX.toFloat(), bgPaint)

        // Center the text block both horizontally and vertically inside the square.
        val xOffset = paddingPx.toFloat()
        val yOffset = ((CANVAS_SIZE_PX - staticLayout.height) / 2f).coerceAtLeast(0f)
        canvas.save()
        canvas.translate(xOffset, yOffset)
        staticLayout.draw(canvas)
        canvas.restore()

        return bitmap
    }

    /** Binary-searches the largest text size (in px) whose wrapped layout fits within [maxWidth]x[maxHeight]. */
    private fun findFillingFontSizePx(text: String, textPaint: TextPaint, maxWidth: Int, maxHeight: Int): Float {
        var low = MIN_FONT_SIZE_PX
        var high = MAX_FONT_SIZE_PX
        var best = low
        repeat(FONT_SIZE_SEARCH_STEPS) {
            val mid = (low + high) / 2f
            textPaint.textSize = mid
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, textPaint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(true)
                .build()
            if (layout.height <= maxHeight) {
                best = mid
                low = mid
            } else {
                high = mid
            }
        }
        return best
    }

    fun generateStyledImageUri(context: Context, text: String, config: TextStyleConfig): Uri {
        val bitmap = renderBitmap(context, text, config)

        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val imageFile = File(imagesDir, "styled_text_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile,
        )
    }

    private fun parseColorOrDefault(hex: String, default: Int): Int =
        try {
            Color.parseColor(hex)
        } catch (_: IllegalArgumentException) {
            default
        }
}
