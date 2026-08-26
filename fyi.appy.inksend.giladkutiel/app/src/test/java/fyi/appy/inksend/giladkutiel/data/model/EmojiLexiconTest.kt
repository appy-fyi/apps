package fyi.appy.inksend.giladkutiel.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiLexiconTest {

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
