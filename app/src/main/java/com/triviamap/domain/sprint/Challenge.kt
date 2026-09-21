package com.triviamap.domain.sprint

import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine

enum class ChallengeType {
    REORDER,     // Single line, reorder stations
    CLASSIFY,    // Two lines: sort stations into line / hub columns AND order each column
    SPEED_BURST  // Rapid fire 3-tile challenge with per-tile timer
}

/** Horizontal placement of a tile in a CLASSIFY challenge. */
object Side {
    const val LINE_1 = -1
    const val HUB = 0
    const val LINE_2 = 1
}

/** "Follow line A toward Illkirch": the direction the player must respect. */
data class Direction(val lineId: String, val lineName: String, val color: Long, val toward: String)

data class Challenge(
    val type: ChallengeType,
    val line: TramLine,
    val line2: TramLine? = null,
    /** Canonical solution (for CLASSIFY: line 1 column, hub column, line 2 column). */
    val correctOrder: List<Station>,
    val tiles: List<Station>,
    val isForward: Boolean,
    /** CLASSIFY only: station id to expected [Side]. */
    val correctSides: Map<String, Int> = emptyMap(),
    /** CLASSIFY only: expected top-to-bottom order inside each column. */
    val columnOrders: Map<Int, List<Station>> = emptyMap()
) {
    val directions: List<Direction>
        get() = listOfNotNull(line, line2).map { l ->
            val terminus = if (isForward) l.stations.last() else l.stations.first()
            Direction(l.id, l.name, l.color, terminus.name)
        }

    /**
     * Ids of tiles that are not where they should be. Tiles that belong to a longest
     * correctly-ordered subsequence count as placed, so moving one tile wrongly does not
     * mark all its neighbours as wrong.
     */
    fun misplaced(orderedTiles: List<Station>, sides: Map<String, Int>): Set<String> {
        if (type != ChallengeType.CLASSIFY) return outOfPlace(orderedTiles, correctOrder)

        fun sideOf(s: Station) = sides[s.id] ?: Side.HUB
        val wrong = orderedTiles.filter { sideOf(it) != correctSides[it.id] }.mapTo(HashSet()) { it.id }
        for ((side, expected) in columnOrders) {
            val actual = orderedTiles.filter { sideOf(it) == side && correctSides[it.id] == side }
            wrong += outOfPlace(actual, expected)
        }
        return wrong
    }

    fun isSolvedBy(orderedTiles: List<Station>, sides: Map<String, Int>): Boolean =
        misplaced(orderedTiles, sides).isEmpty()

    private fun outOfPlace(actual: List<Station>, expected: List<Station>): Set<String> {
        val index = expected.withIndex().associate { it.value.id to it.index }
        val seq = actual.map { index[it.id] ?: return actual.mapTo(HashSet()) { s -> s.id } }
        val keep = longestIncreasingSubsequence(seq)
        return actual.filterIndexed { i, _ -> i !in keep }.mapTo(HashSet()) { it.id }
    }

    /** Indices (into [seq]) of one longest strictly increasing subsequence. */
    private fun longestIncreasingSubsequence(seq: List<Int>): Set<Int> {
        if (seq.isEmpty()) return emptySet()
        val len = IntArray(seq.size) { 1 }
        val prev = IntArray(seq.size) { -1 }
        for (i in seq.indices) for (j in 0 until i) {
            if (seq[j] < seq[i] && len[j] + 1 > len[i]) { len[i] = len[j] + 1; prev[i] = j }
        }
        var best = 0
        for (i in seq.indices) if (len[i] > len[best]) best = i
        val out = HashSet<Int>()
        var cur = best
        while (cur != -1) { out += cur; cur = prev[cur] }
        return out
    }
}
