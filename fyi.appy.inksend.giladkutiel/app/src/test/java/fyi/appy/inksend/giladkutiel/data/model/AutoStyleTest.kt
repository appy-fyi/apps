package fyi.appy.inksend.giladkutiel.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AutoStyleTest {

    @Test
    fun `word keyword picks its intent`() {
        assertEquals(Intent.FUNNY, AutoStyle.detectIntent("haha that was hilarious"))
        assertEquals(Intent.SAD, AutoStyle.detectIntent("feeling so sad today, been crying"))
        assertEquals(Intent.ROMANTIC, AutoStyle.detectIntent("you are my darling"))
        assertEquals(Intent.INFORMATIVE, AutoStyle.detectIntent("quick reminder about the agenda"))
    }

    @Test
    fun `phrase keyword picks its intent`() {
        assertEquals(Intent.MOTIVATIONAL, AutoStyle.detectIntent("you got this, don't quit now"))
        assertEquals(Intent.ROMANTIC, AutoStyle.detectIntent("just wanted to say i love you"))
        assertEquals(Intent.GRATEFUL, AutoStyle.detectIntent("thank you so much for everything"))
    }

    @Test
    fun `emoji alone is enough to pick an intent`() {
        assertEquals(Intent.FUNNY, AutoStyle.detectIntent("well ok then 😂"))
        assertEquals(Intent.ROMANTIC, AutoStyle.detectIntent("goodnight ❤️"))
        assertEquals(Intent.CELEBRATORY, AutoStyle.detectIntent("we made it 🎉"))
    }

    @Test
    fun `phrase outweighs a competing single word`() {
        // "thank you" (phrase, GRATEFUL) should beat "hate" (word, ANGRY).
        assertEquals(Intent.GRATEFUL, AutoStyle.detectIntent("thank you, though i hate waiting"))
    }

    @Test
    fun `detection is case insensitive`() {
        assertEquals(Intent.FUNNY, AutoStyle.detectIntent("HAHA SO FUNNY"))
    }

    @Test
    fun `no match falls back to neutral`() {
        assertEquals(Intent.NEUTRAL, AutoStyle.detectIntent("asdfjkl qwerty zxcvbnm"))
        assertEquals(Intent.NEUTRAL, AutoStyle.detectIntent(""))
        assertEquals(Intent.NEUTRAL, AutoStyle.detectIntent("   "))
    }

    @Test
    fun `substring of a keyword does not trigger it`() {
        // "misinformative" contains "informative" but is a different whole token.
        assertEquals(Intent.NEUTRAL, AutoStyle.detectIntent("that claim is misinformative"))
    }

    @Test
    fun `every intent exposes between three and five styles`() {
        Intent.entries.forEach { intent ->
            assertTrue(
                "intent $intent has ${intent.styles.size} styles",
                intent.styles.size in 3..5,
            )
        }
    }

    @Test
    fun `styleFor returns one of the detected intent's styles`() {
        val text = "haha this is hilarious 😂"
        val expected = AutoStyle.detectIntent(text).styles
        repeat(20) { seed ->
            assertTrue(AutoStyle.styleFor(text, Random(seed)) in expected)
        }
    }

    @Test
    fun `styleFor is deterministic for a fixed random seed`() {
        val text = "congrats on the new job 🎉"
        assertEquals(
            AutoStyle.styleFor(text, Random(42)),
            AutoStyle.styleFor(text, Random(42)),
        )
    }
}
