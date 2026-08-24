package fyi.appy.inksend.giladkutiel.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import fyi.appy.inksend.giladkutiel.data.db.StylePresetEntity
import fyi.appy.inksend.giladkutiel.font.FontResolver
import kotlin.math.ceil

/**
 * Renders typed text as a styled PNG using the active [StylePresetEntity]'s
 * font/color/background — the image handed to WhatsApp by the one-tap send
 * action in the keyboard panel.
 */
object TextImageRenderer {
    private const val PADDING_DP = 32f
    private const val TEXT_SIZE_SP = 42f
    private const val MAX_WIDTH_DP = 800f

    fun render(context: Context, text: String, style: StylePresetEntity): Bitmap {
        val density = context.resources.displayMetrics.density
        val paddingPx = PADDING_DP * density
        val maxWidthPx = (MAX_WIDTH_DP * density).toInt()

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = FontResolver.resolve(context, style.fontFamily)
            textSize = TEXT_SIZE_SP * density
            color = parseColor(style.textColorHex)
        }

        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, maxWidthPx)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .build()

        val textWidth = (0 until layout.lineCount).maxOf { ceil(layout.getLineWidth(it)).toInt() }
            .coerceAtLeast(1)
        val width = textWidth + (paddingPx * 2).toInt()
        val height = layout.height + (paddingPx * 2).toInt()

        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawBackground(canvas, width, height, style)

        canvas.save()
        canvas.translate(paddingPx, paddingPx)
        layout.draw(canvas)
        canvas.restore()

        return bitmap
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int, style: StylePresetEntity) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (style.backgroundType == "gradient" && style.backgroundColorHex2.isNotEmpty()) {
            paint.shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                parseColor(style.backgroundColorHex),
                parseColor(style.backgroundColorHex2),
                Shader.TileMode.CLAMP,
            )
        } else {
            paint.color = parseColor(style.backgroundColorHex)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    private fun parseColor(hex: String): Int =
        runCatching { Color.parseColor(hex) }.getOrDefault(Color.BLACK)
}
