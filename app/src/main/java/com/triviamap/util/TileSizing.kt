package com.triviamap.util

/**
 * Vertical geometry of the Sprint tile list: tiles shrink so the whole puzzle fits on screen
 * without scrolling (scrolling while dragging is error-prone), but never below a comfortable
 * touch target. Scrolling only remains as a fallback for very small screens or large font scales.
 *
 * Pure Kotlin (values in dp) so it can be unit-tested without a Compose/Android host.
 */
object TileSizing {
    const val MAX_TILE_DP = 64f
    const val MIN_TILE_DP = 48f
    const val GAP_DP = 8f
    /** Still leaves 2 dp of air under the 4 dp plate edge drawn below each tile. */
    const val COMPACT_GAP_DP = 6f

    data class Layout(val tileDp: Float, val gapDp: Float, val scrolls: Boolean)

    fun layout(availableDp: Float, count: Int): Layout {
        if (count <= 0) return Layout(MAX_TILE_DP, GAP_DP, scrolls = false)
        if (heightOf(count, MAX_TILE_DP, GAP_DP) <= availableDp) return Layout(MAX_TILE_DP, GAP_DP, scrolls = false)
        val fitted = (availableDp - (count - 1) * COMPACT_GAP_DP) / count
        val tile = fitted.coerceIn(MIN_TILE_DP, MAX_TILE_DP)
        return Layout(tile, COMPACT_GAP_DP, scrolls = heightOf(count, tile, COMPACT_GAP_DP) > availableDp + 0.5f)
    }

    private fun heightOf(count: Int, tile: Float, gap: Float) = count * tile + (count - 1) * gap
}
