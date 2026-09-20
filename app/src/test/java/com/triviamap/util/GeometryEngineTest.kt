package com.triviamap.util

import com.triviamap.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryEngineTest {
    @Test fun frechetOfIdenticalPathsIsZero() {
        val p = listOf(GeoPoint(0.0, 0.0), GeoPoint(1.0, 1.0), GeoPoint(2.0, 0.0))
        assertEquals(0.0, discreteFrechetDistance(p, p), 1e-9)
    }

    @Test fun frechetDoesNotOverflowTheStackOnLongPaths() {
        val p = (0 until 3000).map { GeoPoint(it.toDouble(), 0.0) }
        val q = p.map { GeoPoint(it.x, 1.0) }
        assertEquals(1.0, discreteFrechetDistance(p, q), 1e-9)
    }

    @Test fun flatBoundsDoNotDivideByZero() {
        val b = GeoBounds.from(listOf(GeoPoint(0.0, 5.0), GeoPoint(10.0, 5.0)))
        val (nx, ny) = GeoPoint(5.0, 5.0).toNormalized(b)
        assertTrue(nx.isFinite() && ny.isFinite())
    }
}
