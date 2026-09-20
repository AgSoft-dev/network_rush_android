package com.triviamap.domain.sprint

import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine

enum class ChallengeType {
    REORDER,     // Single line, reorder stations
    CLASSIFY,    // Two lines, drag left/right/center AND reorder
    SPEED_BURST  // Rapid fire 3-tile challenge with per-tile timer
}

/** Horizontal placement of a tile in a CLASSIFY challenge. */
object Side {
    const val LINE_1 = -1
    const val HUB = 0
    const val LINE_2 = 1
}

data class Challenge(
    val type: ChallengeType,
    val line: TramLine,
    val line2: TramLine? = null,
    val correctOrder: List<Station>,
    val tiles: List<Station>,
    val isForward: Boolean,
    /** CLASSIFY only: station id to expected [Side]. */
    val correctSides: Map<String, Int> = emptyMap()
) {
    fun isSolvedBy(orderedTiles: List<Station>, sides: Map<String, Int>): Boolean =
        orderedTiles == correctOrder && (type != ChallengeType.CLASSIFY || sides == correctSides)
}
