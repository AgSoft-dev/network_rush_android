package com.triviamap.util

/**
 * Horizontal geometry for the CLASSIFY challenge's three tile columns (line 1 / hub / line 2).
 *
 * Pure Kotlin so it can be unit-tested without a Compose/Android host: a naive
 * `width * (1 - tileFraction) / 2` shift pushes the outer columns flush against the
 * screen edge (0 margin), which lands them in Android's edge-swipe back-gesture zone —
 * horizontal drags starting there get stolen by the system before Compose ever sees them,
 * while vertical (reorder) drags are unaffected since the gesture recognizer only
 * intercepts predominantly-horizontal motion. Clamping the shift keeps a safety margin.
 */
object ClassifyLayout {
    /** Android's default gesture-navigation edge-swipe zone is ~24dp wide. */
    const val DEFAULT_EDGE_MARGIN_DP = 24f

    /**
     * Distance (px) an outer-column tile is offset from the centered ("hub") position.
     * Guarantees the outer tile's edge stays at least [edgeMarginPx] away from the
     * screen edge, and never goes negative even on a width too narrow to fit the margin.
     */
    fun shiftPx(listWidthPx: Float, tileFraction: Float, edgeMarginPx: Float): Float {
        val tileWidthPx = listWidthPx * tileFraction
        val naiveShiftPx = listWidthPx * (1f - tileFraction) / 2f
        val maxShiftPx = (listWidthPx - tileWidthPx) / 2f - edgeMarginPx
        return naiveShiftPx.coerceAtMost(maxShiftPx).coerceAtLeast(0f)
    }
}
