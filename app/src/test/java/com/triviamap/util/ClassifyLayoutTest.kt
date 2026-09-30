package com.triviamap.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassifyLayoutTest {

    private val tileFraction = 0.62f
    private val edgeMarginPx = 24f * 3f // ~24dp at a 3x density, in raw px

    @Test
    fun outerColumnStaysClearOfTheScreenEdge() {
        for (widthPx in listOf(600f, 900f, 1080f, 1440f, 2340f)) {
            val shift = ClassifyLayout.shiftPx(widthPx, tileFraction, edgeMarginPx)
            val tileWidth = widthPx * tileFraction
            val outerTileLeftEdge = (widthPx - tileWidth) / 2f - shift
            assertTrue(
                "width=$widthPx: outer tile edge $outerTileLeftEdge must clear the $edgeMarginPx px margin",
                outerTileLeftEdge >= edgeMarginPx - 0.01f
            )
        }
    }

    @Test
    fun neverGoesNegativeOnAnUnreasonablyNarrowScreen() {
        val shift = ClassifyLayout.shiftPx(listWidthPx = 100f, tileFraction, edgeMarginPx)
        assertTrue(shift >= 0f)
    }

    @Test
    fun matchesTheNaiveCenteredShiftWhenThereIsRoomToSpare() {
        val widthPx = 2000f
        val naiveShift = widthPx * (1f - tileFraction) / 2f
        val shift = ClassifyLayout.shiftPx(widthPx, tileFraction, edgeMarginPx)
        assertTrue(shift <= naiveShift)
        assertTrue(shift > 0f)
    }

    @Test
    fun sideFollowsTheDragOffsetAroundHalfAShift() {
        val shift = 126f
        assertEquals(0, ClassifyLayout.sideForOffset(0f, shift))
        assertEquals(0, ClassifyLayout.sideForOffset(shift / 2f - 1f, shift))
        assertEquals(1, ClassifyLayout.sideForOffset(shift / 2f + 1f, shift))
        assertEquals(-1, ClassifyLayout.sideForOffset(-shift / 2f - 1f, shift))
    }

    @Test
    fun withoutShiftEveryTileStaysInTheHub() {
        // What a stale Reorder geometry (shift = 0) used to do to Classify: no column reachable
        assertEquals(0, ClassifyLayout.sideForOffset(0f, 0f))
    }
}
