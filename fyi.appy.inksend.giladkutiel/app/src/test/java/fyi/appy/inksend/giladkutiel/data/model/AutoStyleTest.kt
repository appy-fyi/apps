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
        assertEquals(Intent.ROMANTIC, AutoStyle.detectIntent("see you later ❤️"))
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
    fun `good night resolves to CALM once translated`() {
        assertEquals(Intent.CALM, AutoStyle.detectIntent("yaeli, good night"))
        assertEquals(Intent.CALM, AutoStyle.detectIntent("sweet dreams"))
    }

    @Test
    fun `untranslated Hebrew still styles by mood via the fallback`() {
        // The exact messages from the bug report: translation never ran, so the raw Hebrew
        // reaches resolveIntent as originalText == translatedText.
        repeat(10) { seed ->
            assertEquals(
                Intent.ROMANTIC,
                AutoStyle.resolveIntent("אבא אוהב אותך", "אבא אוהב אותך", Random(seed)),
            )
            assertEquals(
                Intent.CALM,
                AutoStyle.resolveIntent("יעלי לילה טוב", "יעלי לילה טוב", Random(seed)),
            )
        }
        // A translation that DID land still wins over the Hebrew fallback.
        assertEquals(
            Intent.CELEBRATORY,
            AutoStyle.resolveIntent("מזל טוב", "congrats on the new job", Random(0)),
        )
    }

    @Test
    fun `planFor for untranslated Hebrew uses Hebrew fonts, mood gradient and fitting emoji`() {
        val plan = AutoStyle.planFor("אבא אוהב אותך", "אבא אוהב אותך", Random(2))
        assertTrue(plan.fontAssetPath in Intent.ROMANTIC.hebrewFonts.map { it.assetPath })
        assertTrue(Gradient(plan.gradientStartHex, plan.gradientEndHex) in Intent.ROMANTIC.gradients)
        assertNotEquals(listOf(EmojiLexicon.DEFAULT), plan.emojis)
    }

    @Test
    fun `buttonHintFor previews untranslated Hebrew instead of falling back to the ink drop`() {
        assertEquals(Intent.ROMANTIC.previewEmoji, AutoStyle.buttonHintFor("אבא אוהב אותך")?.emoji)
        assertEquals(Intent.CALM.previewEmoji, AutoStyle.buttonHintFor("יעלי לילה טוב")?.emoji)
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
    fun `planFor emojis come from the keywords when there is a match`() {
        val withEmoji = AutoStyle.planFor("happy birthday, lets party", "happy birthday, lets party", Random(0))
        assertTrue(withEmoji.emojis.isNotEmpty())
        assertTrue(withEmoji.emojis.size <= 3)
        assertNotEquals(listOf(EmojiLexicon.DEFAULT), withEmoji.emojis)
    }

    @Test
    fun `planFor never renders the bare ink drop - it falls back to the resolved intent`() {
        // No keyword, no emoji, gibberish: the lexicon yields only DEFAULT, so planFor must
        // substitute the resolved intent's emoji set rather than ship the ink drop.
        val plan = AutoStyle.planFor("zxqw plok mnbv", "zxqw plok mnbv", Random(0))
        assertNotEquals(listOf(EmojiLexicon.DEFAULT), plan.emojis)
        assertTrue(plan.emojis.isNotEmpty())
        assertTrue(INTENT_EMOJIS.values.any { it == plan.emojis })
    }

    @Test
    fun `planFor gives almost every real message a non-default emoji`() {
        val messages = listOf(
            // English — plenty of these name no lexicon concept
            "on my way", "sounds good", "call me later", "where are you?", "running late",
            "no worries", "see you tomorrow", "let me check and get back to you", "ok got it",
            "that is hilarious", "i love you so much", "feeling really down today",
            "congratulations on the new job", "please note the meeting moved", "cant wait for this",
            "thank you so much", "you got this, keep pushing", "take it easy tonight",
            "happy birthday!", "the dog needs a walk",
            // Hebrew — untranslated (translation hasn't landed)
            "אבא אוהב אותך", "יעלי לילה טוב", "אני בדרך", "נתראה מחר בעבודה", "תודה רבה על הכל",
            "בא לי קפה", "מזל טוב על העבודה החדשה", "חחח זה ממש מצחיק", "אני ממש עצוב היום",
            "כל הכבוד, תמשיך ככה", "פגישה נדחתה למחר", "יום הולדת שמח", "הכלב צריך לצאת",
            "מתגעגע אליך", "הכל בסדר גמור",
        )
        val default = listOf(EmojiLexicon.DEFAULT)
        val misses = messages.filter { msg ->
            AutoStyle.planFor(msg, msg, Random(0)).emojis == default
        }
        assertTrue("ink-drop fallback still reached for: $misses", misses.isEmpty())
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
