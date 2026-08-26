package fyi.appy.inksend.giladkutiel.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextLengthEvaluatorTest {

    @Test
    fun `length below minimum is out of bounds`() {
        assertFalse(TextLengthEvaluator.isWithinBounds(trimmedLength = 2, minLength = 3, maxLength = 280))
    }

    @Test
    fun `length above maximum is out of bounds`() {
        assertFalse(TextLengthEvaluator.isWithinBounds(trimmedLength = 281, minLength = 3, maxLength = 280))
    }

    @Test
    fun `length exactly at minimum is within bounds`() {
        assertTrue(TextLengthEvaluator.isWithinBounds(trimmedLength = 3, minLength = 3, maxLength = 280))
    }

    @Test
    fun `length exactly at maximum is within bounds`() {
        assertTrue(TextLengthEvaluator.isWithinBounds(trimmedLength = 280, minLength = 3, maxLength = 280))
    }

    @Test
    fun `length in the middle of the range is within bounds`() {
        assertTrue(TextLengthEvaluator.isWithinBounds(trimmedLength = 50, minLength = 3, maxLength = 280))
    }
}
