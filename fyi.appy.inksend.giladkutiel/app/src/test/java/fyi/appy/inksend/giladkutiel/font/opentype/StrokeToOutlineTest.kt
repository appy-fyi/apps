package fyi.appy.inksend.giladkutiel.font.opentype

import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeToOutlineTest {
    @Test
    fun `expanding a straight stroke produces a closed polygon wider than the stroke width`() {
        val stroke = listOf(DesignPoint(0, 0), DesignPoint(500, 0))
        val outline = StrokeToOutline.expand(stroke, halfWidth = 25f)

        assertTrue("outline should have enough points to form a ribbon", outline.size >= 4)

        val minY = outline.minOf { it.y }
        val maxY = outline.maxOf { it.y }
        // The ribbon must extend roughly halfWidth on both sides of the original y=0 line.
        assertTrue(minY <= -15)
        assertTrue(maxY >= 15)

        val minX = outline.minOf { it.x }
        val maxX = outline.maxOf { it.x }
        assertTrue(minX <= 10)
        assertTrue(maxX >= 490)
    }

    @Test
    fun `a single-point stroke still produces a non-empty rounded blob`() {
        val outline = StrokeToOutline.expand(listOf(DesignPoint(100, 100)), halfWidth = 20f)
        assertTrue(outline.isNotEmpty())
    }
}
