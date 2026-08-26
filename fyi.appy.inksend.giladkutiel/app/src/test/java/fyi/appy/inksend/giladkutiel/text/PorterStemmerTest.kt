package fyi.appy.inksend.giladkutiel.text

import org.junit.Assert.assertEquals
import org.junit.Test

class PorterStemmerTest {

    private val stemmer = PorterStemmer()

    @Test
    fun `classic Porter examples`() {
        // Straight from Porter's own sample vocabulary.
        assertEquals("caress", stemmer.stem("caresses"))
        assertEquals("poni", stemmer.stem("ponies"))
        assertEquals("ti", stemmer.stem("ties"))
        assertEquals("caress", stemmer.stem("caress"))
        assertEquals("cat", stemmer.stem("cats"))
        assertEquals("feed", stemmer.stem("feed"))
        assertEquals("agre", stemmer.stem("agreed"))
        assertEquals("plaster", stemmer.stem("plastered"))
        assertEquals("motor", stemmer.stem("motoring"))
        assertEquals("hop", stemmer.stem("hopping"))
        assertEquals("relat", stemmer.stem("relational"))
        assertEquals("ration", stemmer.stem("rational")) // ATIONAL->ATE needs m>0 on "r"; it doesn't fire
    }

    @Test
    fun `inflections of one word collapse to a shared stem`() {
        val laugh = stemmer.stem("laugh")
        assertEquals(laugh, stemmer.stem("laughing"))
        assertEquals(laugh, stemmer.stem("laughed"))
        assertEquals(laugh, stemmer.stem("laughs"))
    }

    @Test
    fun `lower-cases and strips non-letters`() {
        assertEquals(stemmer.stem("running"), stemmer.stem("  RUNNING!!  "))
        assertEquals(stemmer.stem("cats"), stemmer.stem("Cats,"))
    }

    @Test
    fun `very short words are returned unchanged`() {
        assertEquals("is", stemmer.stem("is"))
        assertEquals("a", stemmer.stem("a"))
        assertEquals("", stemmer.stem("!!"))
    }

    @Test
    fun `stemming is idempotent`() {
        for (word in listOf("celebrations", "motivated", "happiness", "grateful", "believing", "peaceful")) {
            val once = stemmer.stem(word)
            assertEquals("re-stemming '$word' -> '$once' should be stable", once, stemmer.stem(once))
        }
    }
}
