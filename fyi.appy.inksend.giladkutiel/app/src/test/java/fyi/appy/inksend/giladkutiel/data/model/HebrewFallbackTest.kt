package fyi.appy.inksend.giladkutiel.data.model

import fyi.appy.inksend.giladkutiel.text.Sentiment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HebrewFallbackTest {

    @Test
    fun `raw Hebrew keywords map to an intent`() {
        assertEquals(Intent.ROMANTIC, HebrewFallback.keywordIntent("אבא אוהב אותך"))
        assertEquals(Intent.CALM, HebrewFallback.keywordIntent("יעלי לילה טוב"))
        assertEquals(Intent.FUNNY, HebrewFallback.keywordIntent("חחח זה ממש מצחיק"))
        assertEquals(Intent.GRATEFUL, HebrewFallback.keywordIntent("תודה רבה על הכל"))
        assertEquals(Intent.SAD, HebrewFallback.keywordIntent("אני ממש עצוב היום"))
    }

    @Test
    fun `matching is substring so clitics and suffixes do not block it`() {
        assertEquals(Intent.ROMANTIC, HebrewFallback.keywordIntent("שאוהבת אותך תמיד"))
        assertEquals(Intent.CELEBRATORY, HebrewFallback.keywordIntent("שנחגוג יחד"))
    }

    @Test
    fun `unrecognised Hebrew returns null`() {
        assertNull(HebrewFallback.keywordIntent("המכונית חונה ברחוב ליד הבניין"))
        assertNull(HebrewFallback.keywordIntent(""))
    }

    @Test
    fun `coarse Hebrew sentiment`() {
        assertEquals(Sentiment.POSITIVE, HebrewFallback.sentiment("יום נהדר ומדהים"))
        assertEquals(Sentiment.NEGATIVE, HebrewFallback.sentiment("הכל נורא וגרוע"))
        assertNull(HebrewFallback.sentiment("המכונית חונה ברחוב"))
    }

    @Test
    fun `emoji fallback follows the keyword hits`() {
        assertTrue("❤️" in HebrewFallback.emojis("אבא אוהב אותך"))
        assertTrue("🌙" in HebrewFallback.emojis("לילה טוב"))
        assertTrue(HebrewFallback.emojis("בלה בלה בלה").isEmpty())
        assertTrue(HebrewFallback.emojis("אוהב לחגוג ומתרגש").size <= 3)
    }

    @Test
    fun `the concept word map covers everyday Hebrew, clitics included`() {
        // Plain messages that carry no strong mood but do name a concept.
        assertTrue("☕" in HebrewFallback.emojis("בא לי קפה"))
        assertTrue("🚗" in HebrewFallback.emojis("אני בדרך אליך"))          // בדרך
        assertTrue("📅" in HebrewFallback.emojis("נתראה מחר"))              // מחר
        assertTrue("💼" in HebrewFallback.emojis("הולך לעבודה"))            // clitic: לעבודה -> עבוד
        assertTrue("🏠" in HebrewFallback.emojis("אני בבית"))              // clitic: בבית -> בית
        assertTrue("🐶" in HebrewFallback.emojis("הכלב צריך לצאת"))         // clitic: הכלב -> כלב
        assertTrue(HebrewFallback.emojis("נתראה מחר בעבודה").size in 1..3)
    }
}
