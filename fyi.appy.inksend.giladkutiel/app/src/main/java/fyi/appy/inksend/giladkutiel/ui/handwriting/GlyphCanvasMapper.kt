package fyi.appy.inksend.giladkutiel.ui.handwriting

import androidx.compose.ui.geometry.Offset
import fyi.appy.inksend.giladkutiel.font.opentype.DesignPoint

/**
 * Maps a drawn point on the glyph canvas (pixels, origin top-left) into font
 * design-space units (1000 units/em, y increasing upward, baseline at y=0),
 * given the canvas's pixel size and its baseline guide drawn at 70% of the
 * canvas height (per the spec's `HandwritingFontCreator` UI description).
 */
class GlyphCanvasMapper(private val canvasWidthPx: Float, private val canvasHeightPx: Float) {
    companion object {
        const val BASELINE_FRACTION = 0.7f
        private const val UNITS_PER_EM = 1000
        private const val ASCENT = 800
        private const val DESCENT = 200
        private const val SIDE_BEARING = 60
    }

    fun toDesignPoint(offset: Offset): DesignPoint {
        val xFraction = (offset.x / canvasWidthPx).coerceIn(0f, 1f)
        val yFraction = (offset.y / canvasHeightPx).coerceIn(0f, 1f)

        val designX = SIDE_BEARING + xFraction * (UNITS_PER_EM - 2 * SIDE_BEARING)
        val designY = if (yFraction <= BASELINE_FRACTION) {
            ((BASELINE_FRACTION - yFraction) / BASELINE_FRACTION) * ASCENT
        } else {
            -((yFraction - BASELINE_FRACTION) / (1f - BASELINE_FRACTION)) * DESCENT
        }
        return DesignPoint(designX.toInt(), designY.toInt())
    }

    /** Inverse of [toDesignPoint], used to redraw already-committed strokes on the canvas. */
    fun toOffset(point: DesignPoint): Offset {
        val xFraction = (point.x - SIDE_BEARING) / (UNITS_PER_EM - 2 * SIDE_BEARING).toFloat()
        val yFraction = if (point.y >= 0) {
            BASELINE_FRACTION * (1f - point.y / ASCENT.toFloat())
        } else {
            BASELINE_FRACTION + (-point.y / DESCENT.toFloat()) * (1f - BASELINE_FRACTION)
        }
        return Offset(xFraction * canvasWidthPx, yFraction * canvasHeightPx)
    }
}
