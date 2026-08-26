package fyi.appy.inksend.giladkutiel.engine

import android.graphics.Paint
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fyi.appy.inksend.giladkutiel.data.model.RenderPlan
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageRendererTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun plan(
        font: String = "fonts/Comfortaa.ttf",
        start: String = "#5B47E0",
        end: String = "#C9B8FF",
        textColor: String = "#FFFFFF",
        emojis: List<String> = listOf("✨"),
    ) = RenderPlan(font, "sample", start, end, textColor, emojis)

    @Test
    fun bitmapIsAlwaysAFixed512SquareRegardlessOfTextLength() {
        val short = ImageRenderer.renderBitmap(context, "Hi", plan())
        val long = ImageRenderer.renderBitmap(
            context,
            "This is a much longer message that should wrap across several lines of styled " +
                "text without overflowing the rendered bitmap boundaries.",
            plan(),
        )
        assertTrue(short.width == 512 && short.height == 512)
        assertTrue(long.width == 512 && long.height == 512)
    }

    @Test
    fun invalidHexColorsFallBackInsteadOfCrashing() {
        val bitmap = ImageRenderer.renderBitmap(
            context, "Fallback check", plan(start = "not-a-color", end = "also-bad", textColor = "nope"),
        )
        assertTrue(bitmap.width == 512 && bitmap.height == 512)
    }

    @Test
    fun rendersWithAndWithoutAnEmojiStrip() {
        val withEmoji = ImageRenderer.renderBitmap(context, "Hello", plan(emojis = listOf("😂", "🎉", "❤️")))
        val withoutEmoji = ImageRenderer.renderBitmap(context, "Hello", plan(emojis = emptyList()))
        assertTrue(withEmoji.width == 512 && withEmoji.height == 512)
        assertTrue(withoutEmoji.width == 512 && withoutEmoji.height == 512)
    }

    @Test
    fun missingFontAssetFallsBackInsteadOfCrashing() {
        val bitmap = ImageRenderer.renderBitmap(context, "No such font", plan(font = "fonts/DoesNotExist.ttf"))
        assertTrue(bitmap.width == 512 && bitmap.height == 512)
    }

    @Test
    fun longTextWrapsOnlyAtSpacesNotMidWord() {
        val text = "The quick brown fox jumps over the lazy dog while wandering through " +
            "a sunlit meadow full of wildflowers and tall grass."
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ImageRenderer.typefaceFor(context, "fonts/Comfortaa.ttf")
        }
        val layout = ImageRenderer.buildFittedLayout(text, textPaint, 424, 300)

        for (lineIndex in 0 until layout.lineCount) {
            val lineEnd = layout.getLineEnd(lineIndex)
            if (lineEnd in 1 until text.length) {
                val brokeMidWord = !text[lineEnd - 1].isWhitespace() && !text[lineEnd].isWhitespace()
                assertTrue("line $lineIndex broke in the middle of a word", !brokeMidWord)
            }
        }
    }

    @Test
    fun spacelessScriptAutoFitsInsteadOfTreatingTheWholeSentenceAsOneWord() {
        val chinese = "这是一段没有任何空格的中文文字用来测试自动缩放是否会把整段句子当作一个无法换行的单词来处理"
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ImageRenderer.typefaceFor(context, "fonts/Comfortaa.ttf")
        }
        val layout = ImageRenderer.buildFittedLayout(chinese, textPaint, 424, 424)

        assertTrue("spaceless text should wrap across multiple lines", layout.lineCount > 1)
        assertTrue(
            "every rendered line should stay within the content width",
            (0 until layout.lineCount).all { layout.getLineWidth(it) <= 424 + 1f },
        )
        assertTrue("font should not be shrunk near the minimum", layout.paint.textSize > 18f)
    }

    @Test
    fun rtlTextIsAlignedToTheStartEdgeOnTheRight() {
        // A Hebrew line: with ALIGN_NORMAL + the first-strong heuristic its lines should hug
        // the right edge (line right ~ content width), not the left.
        val hebrew = "שלום לכולם זהו טקסט לבדיקה"
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ImageRenderer.typefaceFor(context, "fonts/Heebo.ttf")
        }
        val layout = ImageRenderer.buildFittedLayout(hebrew, textPaint, 424, 300)
        val firstLineRight = layout.getLineRight(0)
        assertTrue(
            "RTL first line should reach the right edge (was $firstLineRight of 424)",
            firstLineRight >= 424 - 2f,
        )
    }
}
