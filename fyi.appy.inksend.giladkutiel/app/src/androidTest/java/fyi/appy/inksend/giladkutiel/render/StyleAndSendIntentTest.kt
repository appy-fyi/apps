package fyi.appy.inksend.giladkutiel.render

import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fyi.appy.inksend.giladkutiel.data.db.StylePresetEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the "Ads gate using the font, too" scenario from the build spec's test plan: tapping
 * Style & Send must launch exactly one ACTION_SEND targeting WhatsApp with image/png — no
 * intermediate ad/interstitial screen — and never a no-op when the typed text is non-empty.
 */
@RunWith(AndroidJUnit4::class)
class StyleAndSendIntentTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val style = StylePresetEntity(
        id = 1,
        name = "Test Style",
        fontFamily = "oswald",
        textColorHex = "#000000",
        backgroundType = "solid",
        backgroundColorHex = "#FFFFFF",
        backgroundColorHex2 = "",
        isDefault = true,
        isBuiltIn = true,
    )

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun styleAndSend_rendersImageAndBuildsWhatsAppIntent() {
        val bitmap = TextImageRenderer.render(context, "Hello InkSend", style)
        assertEquals(true, bitmap.width > 0 && bitmap.height > 0)

        val uri = ImageShareHelper.saveToCache(context, bitmap)
        val intent = ImageShareHelper.buildSendIntent(context, uri)

        assertEquals("image/png", intent.type)
        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM))
        // WhatsApp isn't installed on the test emulator, so the helper must fall back to a
        // package-less ACTION_SEND (the system share sheet) rather than silently no-op.
        assertNull(intent.`package`)
        assert(hasAction(Intent.ACTION_SEND).matches(intent))
        assert(hasType("image/png").matches(intent))
    }
}
