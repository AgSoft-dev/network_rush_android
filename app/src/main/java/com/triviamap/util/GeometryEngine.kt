package com.triviamap.util

import com.triviamap.domain.model.GeoPoint
import kotlin.math.*

// ---------------------------------------------------------------------------
// Projection helpers
// ---------------------------------------------------------------------------

/**
 * Projects a GeoPoint into a normalised [0,1] x [0,1] plane given a
 * bounding box. Y increases downward (screen convention).
 */
fun GeoPoint.toNormalized(bounds: GeoBounds): Pair<Float, Float> {
    val nx = ((x - bounds.minX) / bounds.width).toFloat()
    val ny = ((y - bounds.minY) / bounds.height).toFloat()
    return nx to ny
}

data class GeoBounds(
    val minX: Double,
    val maxX: Double,
    val minY: Double,
    val maxY: Double
) {
    /** Never zero, so projections cannot divide by zero on degenerate (flat) lines. */
    val width: Double get() = (maxX - minX).coerceAtLeast(MIN_SPAN)
    val height: Double get() = (maxY - minY).coerceAtLeast(MIN_SPAN)

    companion object {
        private const val MIN_SPAN = 1e-6

        /** Bounding box grown by [padX] / [padY] (fractions of the span) to leave room for UI. */
        fun from(points: List<GeoPoint>, padX: Double = 0.10, padY: Double = 0.25): GeoBounds {
            require(points.isNotEmpty()) { "Cannot compute bounds of empty list" }
            var minX = Double.MAX_VALUE; var maxX = -Double.MAX_VALUE
            var minY = Double.MAX_VALUE; var maxY = -Double.MAX_VALUE
            for (p in points) {
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
            }
            val dX = (maxX - minX) * padX
            val dY = (maxY - minY) * padY
            return GeoBounds(minX - dX, maxX + dX, minY - dY, maxY + dY)
        }
    }
}

// ---------------------------------------------------------------------------
// Ramer–Douglas–Peucker line simplification
// ---------------------------------------------------------------------------

fun simplifyPolyline(points: List<GeoPoint>, epsilon: Double = 1.0): List<GeoPoint> {
    if (points.size <= 2) return points
    var maxDist = 0.0
    var maxIdx = 0
    for (i in 1 until points.size - 1) {
        val d = perpendicularDistance(points[i], points.first(), points.last())
        if (d > maxDist) { maxDist = d; maxIdx = i }
    }
    return if (maxDist > epsilon) {
        val left = simplifyPolyline(points.subList(0, maxIdx + 1), epsilon)
        val right = simplifyPolyline(points.subList(maxIdx, points.size), epsilon)
        left.dropLast(1) + right
    } else {
        listOf(points.first(), points.last())
    }
}

private fun perpendicularDistance(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
    val dx = b.x - a.x; val dy = b.y - a.y
    val len2 = dx * dx + dy * dy
    if (len2 == 0.0) return euclideanDistance(p, a)
    val t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / len2
    val proj = GeoPoint(a.x + t * dx, a.y + t * dy)
    return euclideanDistance(p, proj)
}

// ---------------------------------------------------------------------------
// Euclidean distance
// ---------------------------------------------------------------------------

fun euclideanDistance(a: GeoPoint, b: GeoPoint): Double {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

// ---------------------------------------------------------------------------
// Discrete Fréchet distance
// ---------------------------------------------------------------------------

fun discreteFrechetDistance(p: List<GeoPoint>, q: List<GeoPoint>): Double {
    if (p.isEmpty() || q.isEmpty()) return Double.MAX_VALUE
    val m = p.size; val n = q.size
    // Iterative DP (no recursion: safe for long polylines)
    val ca = Array(m) { DoubleArray(n) }
    for (i in 0 until m) for (j in 0 until n) {
        val d = euclideanDistance(p[i], q[j])
        ca[i][j] = when {
            i == 0 && j == 0 -> d
            i == 0 -> maxOf(ca[0][j - 1], d)
            j == 0 -> maxOf(ca[i - 1][0], d)
            else -> maxOf(minOf(ca[i - 1][j], ca[i - 1][j - 1], ca[i][j - 1]), d)
        }
    }
    return ca[m - 1][n - 1]
}

// ---------------------------------------------------------------------------
// Station snapping
// ---------------------------------------------------------------------------

/**
 * Returns the index of the nearest station within [radius] of [point],
 * or -1 if none.
 */
fun findNearestStation(
    point: GeoPoint,
    stations: List<com.triviamap.domain.model.Station>,
    radius: Double = 30.0
): Int {
    var bestIdx = -1
    var bestDist = radius
    stations.forEachIndexed { idx, s ->
        val d = euclideanDistance(point, s.position)
        if (d < bestDist) { bestDist = d; bestIdx = idx }
    }
    return bestIdx
}

// ---------------------------------------------------------------------------
// Bézier curve smoothing for drawing
// ---------------------------------------------------------------------------

data class CubicSegment(
    val p0x: Float, val p0y: Float,
    val c1x: Float, val c1y: Float,
    val c2x: Float, val c2y: Float,
    val p1x: Float, val p1y: Float
)

fun smoothPath(points: List<Pair<Float, Float>>): List<CubicSegment> {
    if (points.size < 2) return emptyList()
    val result = mutableListOf<CubicSegment>()
    for (i in 0 until points.size - 1) {
        val p0 = points[i]; val p1 = points[i + 1]
        val prevX = if (i > 0) points[i - 1].first else p0.first
        val prevY = if (i > 0) points[i - 1].second else p0.second
        val nextX = if (i + 2 < points.size) points[i + 2].first else p1.first
        val nextY = if (i + 2 < points.size) points[i + 2].second else p1.second
        val alpha = 0.25f
        val c1x = p0.first + (p1.first - prevX) * alpha
        val c1y = p0.second + (p1.second - prevY) * alpha
        val c2x = p1.first - (nextX - p0.first) * alpha
        val c2y = p1.second - (nextY - p0.second) * alpha
        result.add(CubicSegment(p0.first, p0.second, c1x, c1y, c2x, c2y, p1.first, p1.second))
    }
    return result
}
