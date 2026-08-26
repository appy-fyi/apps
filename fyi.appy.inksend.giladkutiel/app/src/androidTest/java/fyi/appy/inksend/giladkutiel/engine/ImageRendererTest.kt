package fyi.appy.inksend.giladkutiel.engine

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fyi.appy.inksend.giladkutiel.data.model.TextStyleConfig
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageRendererTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun bitmapIsAlwaysAFixed512SquareRegardlessOfTextLength() {
        val config = TextStyleConfig()
        val shortBitmap = ImageRenderer.renderBitmap(context, "Hi", config)
        val longText = "This is a much longer message that should wrap across several lines " +
            "of styled text without overflowing the rendered bitmap boundaries."
        val longBitmap = ImageRenderer.renderBitmap(context, longText, config)

        assertTrue("short bitmap should be a 512x512 square", shortBitmap.width == 512 && shortBitmap.height == 512)
        assertTrue("long bitmap should be a 512x512 square", longBitmap.width == 512 && longBitmap.height == 512)
    }

    @Test
    fun invalidHexColorsFallBackInsteadOfCrashing() {
        val config = TextStyleConfig(textColorHex = "not-a-color", backgroundColorHex = "also-bad")
        val bitmap = ImageRenderer.renderBitmap(context, "Fallback check", config)

        assertTrue(bitmap.width > 0 && bitmap.height > 0)
    }

    @Test
    fun rendersSuccessfullyWithAndWithoutAnEmojiBadge() {
        val withEmoji = ImageRenderer.renderBitmap(context, "Hello", TextStyleConfig(emoji = "✨"))
        val withoutEmoji = ImageRenderer.renderBitmap(context, "Hello", TextStyleConfig(emoji = ""))

        assertTrue(withEmoji.width == 512 && withEmoji.height == 512)
        assertTrue(withoutEmoji.width == 512 && withoutEmoji.height == 512)
    }

    @Test
    fun longTextWrapsOnlyAtSpacesNotMidWord() {
        val config = TextStyleConfig()
        val text = "The quick brown fox jumps over the lazy dog while wandering through " +
            "a sunlit meadow full of wildflowers and tall grass."
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(config.font.typefaceName, Typeface.BOLD)
        }
        val density = context.resources.displayMetrics.density
        val paddingPx = (config.paddingDp * density).toInt().coerceIn(0, 512 / 4)
        val maxContentSize = (512 - paddingPx * 2).coerceAtLeast(1)

        val layout = ImageRenderer.buildFittedLayout(text, textPaint, maxContentSize)

        for (lineIndex in 0 until layout.lineCount) {
            val lineEnd = layout.getLineEnd(lineIndex)
            if (lineEnd in 1 until text.length) {
                val brokeMidWord = !text[lineEnd - 1].isWhitespace() && !text[lineEnd].isWhitespace()
                assertTrue("line $lineIndex broke in the middle of a word", !brokeMidWord)
            }
        }
    }
}
