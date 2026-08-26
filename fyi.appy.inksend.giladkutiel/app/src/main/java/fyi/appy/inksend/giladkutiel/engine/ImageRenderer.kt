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
import android.text.TextPaint
import androidx.core.content.FileProvider
import fyi.appy.inksend.giladkutiel.data.model.FontChoice
import fyi.appy.inksend.giladkutiel.data.model.StyleConfig
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

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

    /** How many non-whitespace chars of the text to sample when checking font coverage. */
    private const val TYPEFACE_SAMPLE_CHARS = 32

    fun renderBitmap(context: Context, text: String, config: StyleConfig): Bitmap {
        val density = context.resources.displayMetrics.density
        val paddingPx = (config.paddingDp * density).toInt().coerceIn(0, CANVAS_SIZE_PX / 4)
        val maxContentSize = (CANVAS_SIZE_PX - paddingPx * 2).coerceAtLeast(1)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColorOrDefault(config.textColorHex, Color.WHITE)
            typeface = resolveTypeface(config.font, text)
        }

        // Always render onto a fixed 512x512 square, so font size is the only free
        // variable: binary-search the largest size whose wrapped layout still fits.
        val staticLayout = buildFittedLayout(text, textPaint, maxContentSize)

        val bitmap = Bitmap.createBitmap(CANVAS_SIZE_PX, CANVAS_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // The background is always a 2-stop diagonal gradient — never a flat fill. A style
        // that defines no explicit end colour (isGradientEnabled == false) still gets one,
        // derived as a gently shade-shifted variant of its start colour, so every render
        // has some depth.
        val startColor = parseColorOrDefault(config.backgroundColorHex, Color.DKGRAY)
        val endColor =
            if (config.isGradientEnabled) parseColorOrDefault(config.gradientEndColorHex, startColor)
            else shadeShift(startColor)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, 0f, CANVAS_SIZE_PX.toFloat(), CANVAS_SIZE_PX.toFloat(),
                startColor, endColor, Shader.TileMode.CLAMP,
            )
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
     * Draws the style's emoji badge horizontally centered near the top edge, on top of the
     * main text. Uses the default typeface rather than [config]'s font — Android resolves
     * emoji glyphs through the system-wide font fallback chain regardless of typeface family,
     * so this reliably picks up the system's (vector, on modern devices) color emoji font
     * instead of risking tofu boxes from a font family that doesn't declare emoji coverage.
     */
    private fun drawEmojiBadge(canvas: Canvas, emoji: String) {
        val emojiPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT
            textSize = EMOJI_SIZE_PX
        }
        val baselineY = EMOJI_MARGIN_PX - emojiPaint.ascent()
        val centeredX = (CANVAS_SIZE_PX - emojiPaint.measureText(emoji)) / 2f
        canvas.drawText(emoji, centeredX, baselineY, emojiPaint)
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
     * [maxWidth]x[maxHeight] *and* whose widest unbreakable unit still fits on one line — a
     * plain height check alone would happily accept a size that only fits by breaking a unit
     * mid-way, since Android's line breaker splits a run that's wider than the line rather
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
            val fits = layout.height <= maxHeight && widestUnbreakableWidth(text, textPaint) <= maxWidth
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
     * Android's line breaker will never split across lines. The units are the spans between
     * consecutive [BreakIterator.getLineInstance] boundaries — the same break opportunities
     * [StaticLayout] uses internally. For space-delimited scripts (Latin, Cyrillic, Arabic,
     * Hebrew, Devanagari, …) each span is a word; for scripts written without spaces between
     * words (Chinese, Japanese, Thai, Lao, Khmer) the spans are single characters/clusters,
     * so those inputs are no longer auto-fit as if the whole sentence were one long word.
     * Trailing whitespace in a span is dropped — the line breaker hangs it past the edge.
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

    fun generateStyledImageUri(context: Context, text: String, config: StyleConfig): Uri {
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

    /**
     * A subtly shifted version of [color] for the gradient's second stop when a style
     * defines no explicit end colour: dark colours are lightened and light colours are
     * darkened, each blended ~18% toward white/black. The hue is preserved, so the result
     * reads as depth rather than a second colour.
     */
    private fun shadeShift(color: Int): Int {
        val luminance =
            (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f
        val target = if (luminance < 0.5f) 255 else 0
        val amount = 0.18f
        fun mix(channel: Int) = (channel + (target - channel) * amount).toInt().coerceIn(0, 255)
        return Color.rgb(mix(Color.red(color)), mix(Color.green(color)), mix(Color.blue(color)))
    }

    /**
     * Resolves [font] to a concrete [Typeface] for [text], guarding the two families that
     * ship no non-Latin faces. Android's `serif` and `sans-serif` fall back across the full
     * Noto Serif / Noto Sans set and render every bundled script well, but `cursive` (a
     * Latin-only display face) and `monospace` silently fall back to a plain sans-serif face
     * for scripts they don't cover — so Hindi, CJK, Thai, Arabic, etc. text picked out in
     * the "Cursive" look would render as flat Noto Sans with none of the intended character.
     *
     * When the chosen family measures the same as plain sans-serif for [text] — i.e. it has
     * already silently fallen back and is contributing nothing — this swaps in a
     * coverage-complete family with a compatible flavour: Cursive -> Serif (the most
     * expressive shape that still covers the script), Monospace -> Sans Serif. Latin text,
     * where the decorative family does render distinctly, is left untouched.
     */
    internal fun resolveTypeface(font: FontChoice, text: String): Typeface {
        val chosen = Typeface.create(font.typefaceName, Typeface.BOLD)
        if (font == FontChoice.SERIF || font == FontChoice.SANS_SERIF) return chosen

        val sample = buildString {
            for (ch in text) {
                if (!ch.isWhitespace()) append(ch)
                if (length >= TYPEFACE_SAMPLE_CHARS) break
            }
        }
        if (sample.isEmpty()) return chosen

        val neutral = Typeface.create(FontChoice.SANS_SERIF.typefaceName, Typeface.BOLD)
        val rendersDistinctly = abs(widthWith(chosen, sample) - widthWith(neutral, sample)) > 0.5f
        if (rendersDistinctly) return chosen

        val substitute = if (font == FontChoice.CURSIVE) FontChoice.SERIF else FontChoice.SANS_SERIF
        return Typeface.create(substitute.typefaceName, Typeface.BOLD)
    }

    /** Total advance width of [sample] at a fixed size with [typeface], for coverage comparison. */
    private fun widthWith(typeface: Typeface, sample: String): Float =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 64f
        }.measureText(sample)
}
