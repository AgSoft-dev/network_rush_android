package com.triviamap.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileSizingTest {

    @Test
    fun fewTilesKeepFullSize() {
        val l = TileSizing.layout(availableDp = 470f, count = 4)
        assertEquals(TileSizing.MAX_TILE_DP, l.tileDp, 0.01f)
        assertEquals(TileSizing.GAP_DP, l.gapDp, 0.01f)
        assertFalse(l.scrolls)
    }

    @Test
    fun eightTilesShrinkToFitATypicalPhone() {
        val available = 470f
        val l = TileSizing.layout(available, count = 8)
        assertTrue(l.tileDp in TileSizing.MIN_TILE_DP..TileSizing.MAX_TILE_DP)
        assertTrue(l.tileDp < TileSizing.MAX_TILE_DP)
        assertFalse(l.scrolls)
        assertTrue(8 * l.tileDp + 7 * l.gapDp <= available + 0.01f)
    }

    @Test
    fun smallScreenFallsBackToScrollingAtMinimumSize() {
        val l = TileSizing.layout(availableDp = 300f, count = 8)
        assertEquals(TileSizing.MIN_TILE_DP, l.tileDp, 0.01f)
        assertTrue(l.scrolls)
    }

    @Test
    fun exactFitAtMinimumDoesNotScroll() {
        val available = 8 * TileSizing.MIN_TILE_DP + 7 * TileSizing.COMPACT_GAP_DP
        val l = TileSizing.layout(available, count = 8)
        assertEquals(TileSizing.MIN_TILE_DP, l.tileDp, 0.01f)
        assertFalse(l.scrolls)
    }

    @Test
    fun emptyOrSingleTile() {
        assertFalse(TileSizing.layout(100f, 0).scrolls)
        val one = TileSizing.layout(100f, 1)
        assertEquals(TileSizing.MAX_TILE_DP, one.tileDp, 0.01f)
        assertFalse(one.scrolls)
    }
}
