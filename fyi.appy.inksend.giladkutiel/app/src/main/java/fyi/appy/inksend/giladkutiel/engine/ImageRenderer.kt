package fyi.appy.inksend.giladkutiel.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.icu.text.BreakIterator
import android.icu.util.ULocale
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import fyi.appy.inksend.giladkutiel.data.model.RenderPlan
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Renders text into a styled 512×512 PNG via native Canvas/StaticLayout, writes it to the
 * app's cache dir, and returns a FileProvider content URI.
 *
 * Layout, per todo.txt's "Generate the image" section:
 *  - the background is always a 2-stop diagonal gradient (never a flat fill);
 *  - the text sits at the **top**, aligned to the start — left for LTR, right for RTL —
 *    which `ALIGN_NORMAL` + the first-strong direction heuristic give for free;
 *  - up to three emojis sit in a strip along the **bottom**, centered horizontally;
 *  - the font size is binary-searched to fill the space above the emoji strip, wrapping
 *    between words but never breaking one, and clamped between [MIN_FONT_SIZE_PX] and
 *    [MAX_FONT_SIZE_PX] so it is never microscopic or absurdly large.
 */
object ImageRenderer {

    private const val CANVAS_SIZE_PX = 512
    private const val PADDING_PX = 44
    private const val MIN_FONT_SIZE_PX = 15f
    private const val MAX_FONT_SIZE_PX = 300f
    private const val FONT_SIZE_SEARCH_STEPS = 22

    /** Fraction of the square reserved at the bottom for the emoji strip when there is one. */
    private const val EMOJI_BAND_RATIO = 0.22f
    private const val TEXT_TO_EMOJI_GAP_PX = 14f

    private val typefaceCache = ConcurrentHashMap<String, Typeface>()

    fun renderBitmap(context: Context, text: String, plan: RenderPlan): Bitmap {
        val contentWidth = CANVAS_SIZE_PX - PADDING_PX * 2
        val hasEmoji = plan.emojis.isNotEmpty()
        val emojiBandPx = if (hasEmoji) CANVAS_SIZE_PX * EMOJI_BAND_RATIO else 0f
        val textMaxHeight = (CANVAS_SIZE_PX - PADDING_PX * 2 - emojiBandPx -
            if (hasEmoji) TEXT_TO_EMOJI_GAP_PX else 0f).toInt().coerceAtLeast(1)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColorOrDefault(plan.textColorHex, Color.WHITE)
            typeface = typefaceFor(context, plan.fontAssetPath)
        }
        val layout = buildFittedLayout(text, textPaint, contentWidth, textMaxHeight)

        val bitmap = Bitmap.createBitmap(CANVAS_SIZE_PX, CANVAS_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val startColor = parseColorOrDefault(plan.gradientStartHex, Color.DKGRAY)
        val endColor = parseColorOrDefault(plan.gradientEndHex, startColor)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 0f, CANVAS_SIZE_PX.toFloat(), CANVAS_SIZE_PX.toFloat(),
                startColor, endColor, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, CANVAS_SIZE_PX.toFloat(), CANVAS_SIZE_PX.toFloat(), bgPaint)

        // Text block: anchored to the top padding, start-aligned (RTL respected).
        canvas.save()
        canvas.translate(PADDING_PX.toFloat(), PADDING_PX.toFloat())
        layout.draw(canvas)
        canvas.restore()

        if (hasEmoji) drawEmojiStrip(canvas, plan.emojis)

        return bitmap
    }

    /** Draws up to three emojis centered horizontally in the bottom strip. */
    private fun drawEmojiStrip(canvas: Canvas, emojis: List<String>) {
        val joined = emojis.take(3).joinToString("  ")
        val maxWidth = CANVAS_SIZE_PX - PADDING_PX * 2
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT
            textSize = CANVAS_SIZE_PX * EMOJI_BAND_RATIO * 0.62f
        }
        while (paint.measureText(joined) > maxWidth && paint.textSize > 16f) {
            paint.textSize -= 2f
        }
        val x = (CANVAS_SIZE_PX - paint.measureText(joined)) / 2f
        val baselineY = CANVAS_SIZE_PX - PADDING_PX - paint.descent()
        canvas.drawText(joined, x, baselineY, paint)
    }

    /**
     * Builds the largest-fitting [StaticLayout] for [text] within [maxWidth]×[maxHeight],
     * choosing the font size via [findFillingFontSizePx] and wrapping at that size.
     */
    internal fun buildFittedLayout(
        text: String,
        textPaint: TextPaint,
        maxWidth: Int,
        maxHeight: Int,
    ): StaticLayout {
        textPaint.textSize = findFillingFontSizePx(text, textPaint, maxWidth, maxHeight)
        return newLayout(text, textPaint, maxWidth)
    }

    private fun newLayout(text: String, textPaint: TextPaint, maxWidth: Int): StaticLayout =
        StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(true)
            .build()

    /**
     * Binary-searches the largest text size (px) whose wrapped layout fits within
     * [maxWidth]×[maxHeight] *and* whose widest unbreakable unit still fits on one line — a
     * plain height check alone would accept a size that only fits by breaking a word, since
     * Android's line breaker splits a run wider than the line rather than overflow it.
     */
    private fun findFillingFontSizePx(
        text: String,
        textPaint: TextPaint,
        maxWidth: Int,
        maxHeight: Int,
    ): Float {
        var low = MIN_FONT_SIZE_PX
        var high = MAX_FONT_SIZE_PX
        var best = low
        repeat(FONT_SIZE_SEARCH_STEPS) {
            val mid = (low + high) / 2f
            textPaint.textSize = mid
            val layout = newLayout(text, textPaint, maxWidth)
            val fits = layout.height <= maxHeight &&
                widestUnbreakableWidth(text, textPaint) <= maxWidth
            if (fits) {
                best = mid
                low = mid
            } else {
                high = mid
            }
        }
        return best
    }

    /**
     * Widest rendered width, at [textPaint]'s current size, of any single unit in [text] that
     * Android's line breaker will never split. The units are the spans between consecutive
     * [BreakIterator.getLineInstance] boundaries — for space-delimited scripts each is a word,
     * for scripts written without spaces (Chinese, Japanese, Thai, …) each is a
     * character/cluster, so such input auto-fits rather than being shrunk as one long word.
     */
    private fun widestUnbreakableWidth(text: String, textPaint: TextPaint): Float {
        val iterator = BreakIterator.getLineInstance(ULocale.ROOT).apply { setText(text) }
        var widest = 0f
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val span = text.substring(start, end).trimEnd()
            if (span.isNotEmpty()) widest = maxOf(widest, textPaint.measureText(span))
            start = end
            end = iterator.next()
        }
        return widest
    }

    fun generateStyledImageUri(context: Context, text: String, plan: RenderPlan): Uri {
        val bitmap = renderBitmap(context, text, plan)

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

    /** Loads (and caches) a bundled asset typeface, falling back to the system bold face. */
    internal fun typefaceFor(context: Context, assetPath: String): Typeface =
        typefaceCache.getOrPut(assetPath) {
            runCatching { Typeface.createFromAsset(context.assets, assetPath) }
                .getOrDefault(Typeface.DEFAULT_BOLD)
        }

    private fun parseColorOrDefault(hex: String, default: Int): Int =
        try {
            Color.parseColor(hex)
        } catch (_: IllegalArgumentException) {
            default
        }
}
