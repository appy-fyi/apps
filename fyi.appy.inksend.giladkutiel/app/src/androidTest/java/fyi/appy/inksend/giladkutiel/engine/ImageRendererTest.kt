package fyi.appy.inksend.giladkutiel.engine

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
}
