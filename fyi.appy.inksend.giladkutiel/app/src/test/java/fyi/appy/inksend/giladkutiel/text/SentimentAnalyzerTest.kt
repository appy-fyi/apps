package fyi.appy.inksend.giladkutiel.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SentimentAnalyzerTest {

    @Test
    fun `clearly positive text is positive`() {
        assertEquals(Sentiment.POSITIVE, SentimentAnalyzer.analyze("I love this, it is wonderful and amazing!"))
        assertEquals(Sentiment.POSITIVE, SentimentAnalyzer.analyze("so happy and grateful today"))
        assertEquals(Sentiment.POSITIVE, SentimentAnalyzer.analyze("this is the best news, I am thrilled"))
    }

    @Test
    fun `clearly negative text is negative`() {
        assertEquals(Sentiment.NEGATIVE, SentimentAnalyzer.analyze("I hate this, it is terrible and awful"))
        assertEquals(Sentiment.NEGATIVE, SentimentAnalyzer.analyze("feeling so sad and lonely and depressed"))
        assertEquals(Sentiment.NEGATIVE, SentimentAnalyzer.analyze("this is the worst, I am furious"))
    }

    @Test
    fun `text with no valence words is neutral`() {
        assertEquals(Sentiment.NEUTRAL, SentimentAnalyzer.analyze("the meeting is at three on the fourth floor"))
        assertEquals(Sentiment.NEUTRAL, SentimentAnalyzer.analyze(""))
        assertEquals(Sentiment.NEUTRAL, SentimentAnalyzer.analyze("asdfgh qwerty zxcvbn"))
    }

    @Test
    fun `negation flips a positive statement away from positive`() {
        assertNotEquals(Sentiment.POSITIVE, SentimentAnalyzer.analyze("this is not good at all"))
        assertNotEquals(Sentiment.POSITIVE, SentimentAnalyzer.analyze("I do not love this"))
    }

    @Test
    fun `an intensifier keeps polarity but strengthens it`() {
        // Both positive; the assertion is just that "very" doesn't break classification.
        assertEquals(Sentiment.POSITIVE, SentimentAnalyzer.analyze("good"))
        assertEquals(Sentiment.POSITIVE, SentimentAnalyzer.analyze("very very good"))
    }
}
