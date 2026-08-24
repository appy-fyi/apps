package fyi.appy.inksend.giladkutiel.font.opentype

import kotlin.math.hypot

/** A point in font design units (1000 units per em, y increasing upward, baseline at y=0). */
data class DesignPoint(val x: Int, val y: Int)

/**
 * Converts a hand-drawn stroke (an ordered list of design-space points, one
 * glyph may have several strokes — e.g. the dot and stem of "i") into a
 * closed outline contour by expanding each polyline into a fixed-width
 * ribbon: an offset point on each side of every sample, plus a small fan of
 * points at both ends so open strokes get rounded caps instead of squared-off
 * ends. All points are on-curve — the ribbon is inherently polygonal, so no
 * curve fitting is needed to describe it faithfully.
 */
object StrokeToOutline {
    private const val CAP_SEGMENTS = 4

    fun expand(stroke: List<DesignPoint>, halfWidth: Float): List<DesignPoint> {
        val points = dedupe(stroke)
        if (points.size < 2) {
            return points.firstOrNull()?.let { circle(it, halfWidth) } ?: emptyList()
        }

        val left = ArrayList<DesignPoint>()
        val right = ArrayList<DesignPoint>()
        for (i in points.indices) {
            val normal = vertexNormal(points, i)
            val (nx, ny) = normal
            val p = points[i]
            left.add(DesignPoint((p.x + nx * halfWidth).toInt(), (p.y + ny * halfWidth).toInt()))
            right.add(DesignPoint((p.x - nx * halfWidth).toInt(), (p.y - ny * halfWidth).toInt()))
        }

        val endCap = arc(points.last(), left.last(), right.last(), halfWidth)
        val startCap = arc(points.first(), right.first(), left.first(), halfWidth)

        val outline = ArrayList<DesignPoint>()
        outline.addAll(left)
        outline.addAll(endCap)
        outline.addAll(right.asReversed())
        outline.addAll(startCap)
        return dedupe(outline)
    }

    private fun dedupe(points: List<DesignPoint>): List<DesignPoint> {
        val result = ArrayList<DesignPoint>()
        for (p in points) {
            if (result.isEmpty() || hypot((p.x - result.last().x).toDouble(), (p.y - result.last().y).toDouble()) > 0.5) {
                result.add(p)
            }
        }
        return result
    }

    private fun vertexNormal(points: List<DesignPoint>, index: Int): Pair<Float, Float> {
        val prev = points.getOrNull(index - 1)
        val next = points.getOrNull(index + 1)
        val dirs = ArrayList<Pair<Float, Float>>()
        if (prev != null) dirs.add(direction(prev, points[index]))
        if (next != null) dirs.add(direction(points[index], next))
        if (dirs.isEmpty()) return 0f to 0f
        val avgDx = dirs.sumOf { it.first.toDouble() }.toFloat() / dirs.size
        val avgDy = dirs.sumOf { it.second.toDouble() }.toFloat() / dirs.size
        val len = hypot(avgDx.toDouble(), avgDy.toDouble()).toFloat().let { if (it < 1e-4f) 1f else it }
        // Perpendicular (rotate direction 90 degrees).
        return (-avgDy / len) to (avgDx / len)
    }

    private fun direction(a: DesignPoint, b: DesignPoint): Pair<Float, Float> {
        val dx = (b.x - a.x).toFloat()
        val dy = (b.y - a.y).toFloat()
        val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().let { if (it < 1e-4f) 1f else it }
        return (dx / len) to (dy / len)
    }

    /** A small fan of points sweeping from [from] to [to] around [center], approximating a round cap. */
    private fun arc(center: DesignPoint, from: DesignPoint, to: DesignPoint, radius: Float): List<DesignPoint> {
        val startAngle = kotlin.math.atan2((from.y - center.y).toDouble(), (from.x - center.x).toDouble())
        val endAngle = kotlin.math.atan2((to.y - center.y).toDouble(), (to.x - center.x).toDouble())
        var delta = endAngle - startAngle
        while (delta <= 0) delta += 2 * Math.PI
        val result = ArrayList<DesignPoint>()
        for (i in 1 until CAP_SEGMENTS) {
            val angle = startAngle + delta * (i.toDouble() / CAP_SEGMENTS)
            result.add(
                DesignPoint(
                    (center.x + radius * kotlin.math.cos(angle)).toInt(),
                    (center.y + radius * kotlin.math.sin(angle)).toInt(),
                ),
            )
        }
        return result
    }

    private fun circle(center: DesignPoint, radius: Float): List<DesignPoint> {
        val segments = 8
        return (0 until segments).map { i ->
            val angle = 2 * Math.PI * i / segments
            DesignPoint(
                (center.x + radius * kotlin.math.cos(angle)).toInt(),
                (center.y + radius * kotlin.math.sin(angle)).toInt(),
            )
        }
    }
}
