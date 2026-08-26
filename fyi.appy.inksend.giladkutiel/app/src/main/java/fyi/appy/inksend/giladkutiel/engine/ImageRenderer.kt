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
    private val EMOJI_SIZE_PX = CANVAS_SIZE_PX * 0.14f
    private val EMOJI_MARGIN_PX = CANVAS_SIZE_PX * 0.06f

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
        val staticLayout = buildFittedLayout(text, textPaint, maxContentSize)

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

        if (config.emoji.isNotBlank()) {
            drawEmojiBadge(canvas, config.emoji)
        }

        return bitmap
    }

    /**
     * Draws the style's emoji badge in the top-start corner, on top of the main text.
     * Uses the default typeface rather than [config]'s font — Android resolves emoji
     * glyphs through the system-wide font fallback chain regardless of typeface family,
     * so this reliably picks up the system's (vector, on modern devices) color emoji font
     * instead of risking tofu boxes from a font family that doesn't declare emoji coverage.
     */
    private fun drawEmojiBadge(canvas: Canvas, emoji: String) {
        val emojiPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT
            textSize = EMOJI_SIZE_PX
        }
        val baselineY = EMOJI_MARGIN_PX - emojiPaint.ascent()
        canvas.drawText(emoji, EMOJI_MARGIN_PX, baselineY, emojiPaint)
    }

    /**
     * Builds the largest-fitting [StaticLayout] for [text] within a [maxContentSize] square,
     * choosing the font size via [findFillingFontSizePx] and wrapping at that size.
     */
    internal fun buildFittedLayout(text: String, textPaint: TextPaint, maxContentSize: Int): StaticLayout {
        textPaint.textSize = findFillingFontSizePx(text, textPaint, maxContentSize, maxContentSize)
        return StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, maxContentSize)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(true)
            .build()
    }

    /**
     * Binary-searches the largest text size (in px) whose wrapped layout fits within
     * [maxWidth]x[maxHeight] *and* whose widest single word still fits on one line — a plain
     * height check alone would happily accept a size that only fits by breaking a word
     * mid-way, since Android's line breaker splits a word that's wider than the line rather
     * than overflow it.
     */
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
            val fits = layout.height <= maxHeight && widestWordWidth(text, textPaint) <= maxWidth
            if (fits) {
                best = mid
                low = mid
            } else {
                high = mid
            }
        }
        return best
    }

    /** Widest rendered width, at [textPaint]'s current size, among [text]'s whitespace-delimited words. */
    private fun widestWordWidth(text: String, textPaint: TextPaint): Float =
        text.split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .maxOfOrNull { textPaint.measureText(it) } ?: 0f

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
