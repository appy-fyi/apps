package fyi.appy.inksend.giladkutiel.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EmojiLexiconTest {

    @Test
    fun `the stem to emoji relation is many-to-many`() {
        // one stem -> several emojis
        val night = EmojiLexicon.STEM_TO_EMOJIS.getValue("night")
        val moon = EmojiLexicon.STEM_TO_EMOJIS.getValue("moon")
        assertTrue("night carries several emojis, got $night", night.size >= 3)
        assertTrue("moon carries several emojis, got $moon", moon.size >= 3)
        // several stems -> one shared emoji
        assertTrue("🌙" in night && "🌙" in moon)
    }

    @Test
    fun `without an RNG selection is deterministic and keeps each stem's primary emoji`() {
        val primary = EmojiLexicon.STEM_TO_EMOJIS.getValue("night").first()
        repeat(5) {
            assertEquals(listOf(primary), EmojiLexicon.select(listOf("night"), "night", max = 1))
        }
    }

    @Test
    fun `with an RNG a rich stem varies its pick and can fill the strip alone`() {
        val night = EmojiLexicon.STEM_TO_EMOJIS.getValue("night")
        val seen = (0 until 40)
            .map { seed -> EmojiLexicon.select(listOf("night"), "x", max = 1, random = Random(seed)).single() }
            .toSet()
        assertTrue("expected varied picks across seeds, got $seen", seen.size >= 2)
        assertTrue("every pick must come from the night bucket", seen.all { it in night })

        val strip = EmojiLexicon.select(listOf("night"), "x", max = 3, random = Random(1))
        assertEquals(3, strip.size)
        assertEquals(3, strip.toSet().size)
        assertTrue(strip.all { it in night })
    }

    @Test
    fun `has at least 100 distinct emojis`() {
        assertTrue(
            "only ${EmojiLexicon.ALL_EMOJIS.size} distinct emojis",
            EmojiLexicon.ALL_EMOJIS.size >= 100,
        )
        assertEquals(EmojiLexicon.ALL_EMOJIS.size, EmojiLexicon.ALL_EMOJIS.toSet().size)
    }

    @Test
    fun `keyword stems resolve to their emoji`() {
        assertTrue("🤣" in EmojiLexicon.select("this is so funny"))
        assertTrue("😍" in EmojiLexicon.select("sending you all my love"))
        assertTrue("🎂" in EmojiLexicon.select("happy birthday to you"))
        // Inflected forms hit the same entry via the shared Porter stem.
        assertTrue("🎉" in EmojiLexicon.select("we are celebrating tonight"))
    }

    @Test
    fun `a question about eating resolves to a food emoji, not the ink drop`() {
        val picked = EmojiLexicon.select("Do you want to eat something?")
        assertEquals("🍽️", picked.first())
        assertTrue("🍽️" in picked)
    }

    @Test
    fun `selects at most three, in first-seen order`() {
        val picked = EmojiLexicon.select("love the cake music dance party")
        assertTrue(picked.size <= 3)
        assertEquals("😍", picked.first())
    }

    @Test
    fun `falls back to the ink drop when nothing matches`() {
        assertEquals(listOf(EmojiLexicon.DEFAULT), EmojiLexicon.select("asdfgh qwerty zxcvbn"))
        assertEquals(listOf(EmojiLexicon.DEFAULT), EmojiLexicon.select(""))
    }

    @Test
    fun `an emoji already in the text is kept`() {
        val picked = EmojiLexicon.select("great work 🚀 keep going")
        assertTrue("🚀" in picked)
    }
}
