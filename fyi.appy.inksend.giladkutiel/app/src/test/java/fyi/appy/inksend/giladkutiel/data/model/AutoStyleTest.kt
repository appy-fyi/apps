package fyi.appy.inksend.giladkutiel.data.model

import fyi.appy.inksend.giladkutiel.text.Sentiment
import fyi.appy.inksend.giladkutiel.text.SentimentAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AutoStyleTest {

    // --- intent detection -----------------------------------------------------

    @Test
    fun `keyword stems select their intent`() {
        assertEquals(Intent.FUNNY, AutoStyle.detectIntent("haha that was hilarious"))
        assertEquals(Intent.SAD, AutoStyle.detectIntent("feeling so sad today, been crying"))
        assertEquals(Intent.ROMANTIC, AutoStyle.detectIntent("you are my darling"))
        assertEquals(Intent.INFORMATIVE, AutoStyle.detectIntent("quick reminder about the agenda"))
        // Inflected form resolves through the shared Porter stem ("celebrating" -> "celebr").
        assertEquals(Intent.CELEBRATORY, AutoStyle.detectIntent("we are celebrating tonight"))
    }

    @Test
    fun `phrases outweigh a competing single keyword`() {
        assertEquals(Intent.GRATEFUL, AutoStyle.detectIntent("thank you, though i hate waiting"))
        assertEquals(Intent.MOTIVATIONAL, AutoStyle.detectIntent("you got this, don't quit now"))
    }

    @Test
    fun `emoji alone is enough`() {
        assertEquals(Intent.ROMANTIC, AutoStyle.detectIntent("goodnight ❤️"))
        assertEquals(Intent.CELEBRATORY, AutoStyle.detectIntent("we made it 🎉"))
    }

    @Test
    fun `detection is case-insensitive and whole-token`() {
        assertEquals(Intent.FUNNY, AutoStyle.detectIntent("HAHA SO FUNNY"))
        // "misinformative" must not fire the "informative" trigger.
        assertNull(AutoStyle.detectIntent("that claim is misinformative"))
    }

    @Test
    fun `no keyword match returns null`() {
        assertNull(AutoStyle.detectIntent("asdfjkl qwerty zxcvbnm"))
        assertNull(AutoStyle.detectIntent(""))
        assertNull(AutoStyle.detectIntent("   "))
    }

    // --- sentiment fallback -------------------------------------------------------

    @Test
    fun `resolveIntent falls back to a sentiment-mapped intent when no keyword matches`() {
        val positive = "this is absolutely wonderful and I am so glad"
        assertNull(AutoStyle.detectIntent(positive))
        assertEquals(Sentiment.POSITIVE, SentimentAnalyzer.analyze(positive))
        repeat(20) { seed ->
            assertTrue(AutoStyle.resolveIntent(positive, Random(seed)) in SENTIMENT_INTENTS.getValue(Sentiment.POSITIVE))
        }

        val negative = "this is terrible and awful and I feel hopeless"
        assertNull(AutoStyle.detectIntent(negative))
        repeat(20) { seed ->
            assertTrue(AutoStyle.resolveIntent(negative, Random(seed)) in SENTIMENT_INTENTS.getValue(Sentiment.NEGATIVE))
        }
    }

    @Test
    fun `every sentiment maps to at least two intents so the fallback always resolves`() {
        Sentiment.entries.forEach { sentiment ->
            assertTrue(SENTIMENT_INTENTS.getValue(sentiment).size >= 2)
        }
    }

    // --- structural guarantees --------------------------------------------------

    @Test
    fun `there are exactly ten intents`() {
        assertEquals(10, Intent.entries.size)
    }

    @Test
    fun `each intent has at least two English and two Hebrew fonts and four gradients`() {
        Intent.entries.forEach { intent ->
            assertTrue("$intent english fonts", intent.englishFonts.size >= 2)
            assertTrue("$intent hebrew fonts", intent.hebrewFonts.size >= 2)
            assertTrue("$intent gradients", intent.gradients.size >= 4)
            // Hebrew fonts must actually be Hebrew-capable faces.
            assertTrue(
                "$intent hebrew fonts must be Hebrew-capable",
                intent.hebrewFonts.all { it in HEBREW_CAPABLE },
            )
        }
    }

    // --- planFor --------------------------------------------------------------

    @Test
    fun `planFor picks a font from the intent's list for the text's script`() {
        val english = AutoStyle.planFor("haha this is hilarious", "haha this is hilarious", Random(1))
        assertTrue(english.fontAssetPath in Intent.FUNNY.englishFonts.map { it.assetPath })

        val hebrewOriginal = "חחח זה ממש מצחיק"
        val plan = AutoStyle.planFor(hebrewOriginal, "haha this is so funny", Random(1))
        assertTrue(plan.fontAssetPath in Intent.FUNNY.hebrewFonts.map { it.assetPath })
    }

    @Test
    fun `planFor gradient and text colour come from the resolved intent`() {
        val plan = AutoStyle.planFor("you are my darling", "you are my darling", Random(3))
        assertTrue(Gradient(plan.gradientStartHex, plan.gradientEndHex) in Intent.ROMANTIC.gradients)
        assertTrue(plan.textColorHex == "#1E1E2E" || plan.textColorHex == "#FFFFFF")
    }

    @Test
    fun `planFor emojis come from the keywords, with the ink-drop fallback`() {
        val withEmoji = AutoStyle.planFor("happy birthday, lets party", "happy birthday, lets party", Random(0))
        assertTrue(withEmoji.emojis.isNotEmpty())
        assertTrue(withEmoji.emojis.size <= 3)
        assertNotEquals(listOf(EmojiLexicon.DEFAULT), withEmoji.emojis)

        val noEmoji = AutoStyle.planFor("zxqw plok mnbv", "zxqw plok mnbv", Random(0))
        assertEquals(listOf(EmojiLexicon.DEFAULT), noEmoji.emojis)
    }

    @Test
    fun `planFor is deterministic for a fixed seed`() {
        val a = AutoStyle.planFor("congrats on the new job", "congrats on the new job", Random(42))
        val b = AutoStyle.planFor("congrats on the new job", "congrats on the new job", Random(42))
        assertEquals(a, b)
    }

    // --- button hint --------------------------------------------------------------

    @Test
    fun `buttonHintFor mirrors the detected intent and is stable`() {
        val hint = AutoStyle.buttonHintFor("haha this is hilarious")
        assertEquals(Intent.FUNNY.previewEmoji, hint?.emoji)
        assertEquals(Intent.FUNNY.gradients.first().startHex, hint?.backgroundColorHex)
        repeat(10) { assertEquals(hint, AutoStyle.buttonHintFor("haha this is hilarious")) }
    }

    @Test
    fun `buttonHintFor is null only when there is nothing to go on`() {
        assertNull(AutoStyle.buttonHintFor("asdfjkl qwerty zxcvbnm"))
        // A clearly positive line with no keyword still previews something.
        assertNotNull(AutoStyle.buttonHintFor("this is absolutely wonderful and amazing"))
    }

    private companion object {
        val HEBREW_CAPABLE = setOf(
            BundledFont.HEEBO, BundledFont.FRANK_RUHL, BundledFont.SUEZ_ONE,
            BundledFont.SECULAR_ONE, BundledFont.RUBIK,
        )
    }
}
