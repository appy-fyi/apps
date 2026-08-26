package fyi.appy.inksend.giladkutiel.engine

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fyi.appy.inksend.giladkutiel.data.model.StyleConfig
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageRendererTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun bitmapIsAlwaysAFixed512SquareRegardlessOfTextLength() {
        val config = StyleConfig()
        val shortBitmap = ImageRenderer.renderBitmap(context, "Hi", config)
        val longText = "This is a much longer message that should wrap across several lines " +
            "of styled text without overflowing the rendered bitmap boundaries."
        val longBitmap = ImageRenderer.renderBitmap(context, longText, config)

        assertTrue("short bitmap should be a 512x512 square", shortBitmap.width == 512 && shortBitmap.height == 512)
        assertTrue("long bitmap should be a 512x512 square", longBitmap.width == 512 && longBitmap.height == 512)
    }

    @Test
    fun invalidHexColorsFallBackInsteadOfCrashing() {
        val config = StyleConfig(textColorHex = "not-a-color", backgroundColorHex = "also-bad")
        val bitmap = ImageRenderer.renderBitmap(context, "Fallback check", config)

        assertTrue(bitmap.width > 0 && bitmap.height > 0)
    }

    @Test
    fun rendersSuccessfullyWithAndWithoutAnEmojiBadge() {
        val withEmoji = ImageRenderer.renderBitmap(context, "Hello", StyleConfig(emoji = "✨"))
        val withoutEmoji = ImageRenderer.renderBitmap(context, "Hello", StyleConfig(emoji = ""))

        assertTrue(withEmoji.width == 512 && withEmoji.height == 512)
        assertTrue(withoutEmoji.width == 512 && withoutEmoji.height == 512)
    }

    @Test
    fun spacelessScriptAutoFitsInsteadOfTreatingTheWholeSentenceAsOneWord() {
        // A long run of Han characters with no spaces. The old whitespace-split fit check
        // treated the entire run as a single unbreakable "word" and shrank the font until
        // that whole run fit on one line — bottoming out near the 12px floor. The
        // BreakIterator-based check knows each character is a valid break opportunity, so
        // the text should now wrap across lines at a legible size.
        val config = StyleConfig()
        val chinese = "这是一段没有任何空格的中文文字用来测试自动缩放是否会把整段句子当作一个无法换行的单词来处理"
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(config.font.typefaceName, Typeface.BOLD)
        }
        val density = context.resources.displayMetrics.density
        val paddingPx = (config.paddingDp * density).toInt().coerceIn(0, 512 / 4)
        val maxContentSize = (512 - paddingPx * 2).coerceAtLeast(1)

        val layout = ImageRenderer.buildFittedLayout(chinese, textPaint, maxContentSize)

        assertTrue("spaceless text should wrap across multiple lines", layout.lineCount > 1)
        assertTrue(
            "every rendered line should stay within the content width",
            (0 until layout.lineCount).all { layout.getLineWidth(it) <= maxContentSize + 1f },
        )
        assertTrue(
            "font should not be shrunk near the minimum (was ${layout.paint.textSize}px)",
            layout.paint.textSize > 18f,
        )
        val bitmap = ImageRenderer.renderBitmap(context, chinese, config)
        assertTrue("render still produces a 512x512 square", bitmap.width == 512 && bitmap.height == 512)
    }

    @Test
    fun longTextWrapsOnlyAtSpacesNotMidWord() {
        val config = StyleConfig()
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
