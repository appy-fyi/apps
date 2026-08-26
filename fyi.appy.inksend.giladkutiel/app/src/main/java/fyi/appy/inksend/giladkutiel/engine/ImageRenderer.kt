package fyi.appy.inksend.giladkutiel.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
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

    private const val MAX_CANVAS_WIDTH_DP = 800
    private const val MIN_CANVAS_WIDTH_DP = 250

    fun renderBitmap(context: Context, text: String, config: TextStyleConfig): Bitmap {
        val density = context.resources.displayMetrics.density
        val paddingPx = (config.paddingDp * density).toInt()
        val cornerRadiusPx = config.cornerRadiusDp * density

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColorOrDefault(config.textColorHex, Color.WHITE)
            textSize = config.fontSizeSp * density
            typeface = Typeface.create(config.font.typefaceName, Typeface.BOLD)
        }

        val maxCanvasWidthPx = (MAX_CANVAS_WIDTH_DP * density).toInt()
        val minCanvasWidthPx = (MIN_CANVAS_WIDTH_DP * density).toInt()
        val maxContentWidth = (maxCanvasWidthPx - paddingPx * 2).coerceAtLeast(1)

        // Measure the text's own ideal width first so short strings get a tight-fit
        // box instead of always filling the max canvas width.
        val desiredWidth = Layout.getDesiredWidth(text, textPaint).toInt().coerceIn(1, maxContentWidth)

        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, desiredWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(true)
            .build()

        val contentWidth = desiredWidth
        val contentHeight = staticLayout.height

        val finalWidth = (contentWidth + paddingPx * 2).coerceAtLeast(minCanvasWidthPx)
        val finalHeight = contentHeight + paddingPx * 2

        val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            if (config.isGradientEnabled) {
                shader = LinearGradient(
                    0f, 0f, finalWidth.toFloat(), finalHeight.toFloat(),
                    parseColorOrDefault(config.backgroundColorHex, Color.DKGRAY),
                    parseColorOrDefault(config.gradientEndColorHex, Color.DKGRAY),
                    Shader.TileMode.CLAMP,
                )
            } else {
                color = parseColorOrDefault(config.backgroundColorHex, Color.DKGRAY)
            }
        }

        val rect = RectF(0f, 0f, finalWidth.toFloat(), finalHeight.toFloat())
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, bgPaint)

        // Center the (possibly narrower-than-canvas) text block inside the canvas.
        val xOffset = (finalWidth - contentWidth) / 2f
        canvas.save()
        canvas.translate(xOffset, paddingPx.toFloat())
        staticLayout.draw(canvas)
        canvas.restore()

        return bitmap
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
