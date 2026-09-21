package com.triviamap.domain

import com.triviamap.domain.model.GeoPoint
import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine
import com.triviamap.domain.sprint.Challenge
import com.triviamap.domain.sprint.ChallengeType
import com.triviamap.domain.sprint.Side
import org.junit.Assert.*
import org.junit.Test

class ChallengeTest {
    private fun st(id: String) = Station(id, id, GeoPoint(0.0, 0.0))
    private val s = ('a'..'h').map { st(it.toString()) }
    private val lineA = TramLine("A", "Ligne A", 0xFFFF0000, geometry = emptyList(), stations = s)

    private fun reorder(count: Int = 5, forward: Boolean = true): Challenge {
        val order = s.take(count).let { if (forward) it else it.reversed() }
        return Challenge(ChallengeType.REORDER, lineA, null, order, order.reversed(), forward)
    }

    @Test fun solvedOnlyByExactOrder() {
        val c = reorder()
        assertTrue(c.isSolvedBy(c.correctOrder, emptyMap()))
        assertFalse(c.isSolvedBy(c.tiles, emptyMap()))
    }

    @Test fun oneTileInTheWrongPlaceOnlyMarksThatTile() {
        val c = reorder(5)
        // a b c d e -> tile "a" moved to the end: only "a" is out of place
        val moved = listOf(s[1], s[2], s[3], s[4], s[0])
        assertEquals(setOf("a"), c.misplaced(moved, emptyMap()))
    }

    @Test fun fullyReversedMarksAllButOne() {
        val c = reorder(5)
        assertEquals(4, c.misplaced(c.correctOrder.reversed(), emptyMap()).size)
    }

    @Test fun directionNamesTheTerminusInTheOrderingDirection() {
        assertEquals("h", reorder(forward = true).directions.single().toward)
        assertEquals("a", reorder(forward = false).directions.single().toward)
    }

    private fun classify(): Challenge {
        // left column: a b, hub: c, right column: x y  (columns ordered independently)
        val x = st("x"); val y = st("y")
        val left = listOf(s[0], s[1]); val hub = listOf(s[2]); val right = listOf(x, y)
        val sides = mapOf("a" to Side.LINE_1, "b" to Side.LINE_1, "c" to Side.HUB, "x" to Side.LINE_2, "y" to Side.LINE_2)
        return Challenge(
            ChallengeType.CLASSIFY, lineA, lineA.copy(id = "B"),
            correctOrder = left + hub + right, tiles = right + hub + left, isForward = true,
            correctSides = sides,
            columnOrders = mapOf(Side.LINE_1 to left, Side.HUB to hub, Side.LINE_2 to right)
        )
    }

    @Test fun classifyOnlyChecksOrderInsideEachColumn() {
        val c = classify()
        // Vertical interleaving of the columns does not matter
        val interleaved = listOf(st("x"), s[0], s[2], st("y"), s[1])
        assertTrue(c.isSolvedBy(interleaved, c.correctSides))
    }

    @Test fun classifyWrongSideIsMisplaced() {
        val c = classify()
        val sides = c.correctSides + ("c" to Side.LINE_1)
        assertTrue("c" in c.misplaced(c.correctOrder, sides))
        assertFalse(c.isSolvedBy(c.correctOrder, sides))
    }

    @Test fun classifyWrongOrderInsideAColumn() {
        val c = classify()
        val swapped = listOf(s[1], s[0], s[2], st("x"), st("y"))
        assertEquals(1, c.misplaced(swapped, c.correctSides).size)
    }
}
