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
    fun longTextWrapsIntoMultipleLinesWithoutOverflowingBounds() {
        val config = TextStyleConfig()
        val shortBitmap = ImageRenderer.renderBitmap(context, "Hi", config)
        val longText = "This is a much longer message that should wrap across several lines " +
            "of styled text without overflowing the rendered bitmap boundaries."
        val longBitmap = ImageRenderer.renderBitmap(context, longText, config)

        assertTrue("long text should render a taller bitmap", longBitmap.height > shortBitmap.height)
        assertTrue("width should stay within the max canvas bound", longBitmap.width <= 800 * context.resources.displayMetrics.density + 1)
    }

    @Test
    fun shortTextRendersATightFitBitmapNotTheFullMaxWidth() {
        val config = TextStyleConfig()
        val bitmap = ImageRenderer.renderBitmap(context, "Hi", config)
        val maxWidthPx = (800 * context.resources.displayMetrics.density).toInt()

        assertTrue("short text should not need the full max canvas width", bitmap.width < maxWidthPx)
    }

    @Test
    fun invalidHexColorsFallBackInsteadOfCrashing() {
        val config = TextStyleConfig(textColorHex = "not-a-color", backgroundColorHex = "also-bad")
        val bitmap = ImageRenderer.renderBitmap(context, "Fallback check", config)

        assertTrue(bitmap.width > 0 && bitmap.height > 0)
    }
}
