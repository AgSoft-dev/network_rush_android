package com.triviamap.domain

import com.triviamap.data.model.GeoJsonParser
import com.triviamap.domain.sprint.ChallengeGenerator
import com.triviamap.domain.sprint.ChallengeType
import com.triviamap.domain.sprint.Side
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class ChallengeGeneratorTest {
    private val lines = GeoJsonParser.parseData(File("src/main/assets/strasbourg_stations.json").readText())

    @Test fun invariantsHoldOverManySeedsAndLevels() {
        for (seed in 0 until 40) {
            val gen = ChallengeGenerator(Random(seed))
            for (level in 1..40) {
                val c = gen.generate(level, lines)
                assertEquals("tiles are a permutation", c.correctOrder.toSet(), c.tiles.toSet())
                assertEquals(c.correctOrder.size, c.tiles.size)
                assertTrue("at least 2 tiles (seed=$seed level=$level)", c.tiles.size >= 2)
                assertNotEquals("never starts solved", c.correctOrder, c.tiles)
                assertEquals(c.correctOrder.map { it.id }.toSet().size, c.correctOrder.size)
                if (c.type == ChallengeType.SPEED_BURST) assertEquals(3, c.tiles.size)
                if (c.type == ChallengeType.CLASSIFY) {
                    assertNotNull(c.line2)
                    assertEquals(c.correctOrder.map { it.id }.toSet(), c.correctSides.keys)
                    assertTrue(c.correctSides.values.all { it in Side.LINE_1..Side.LINE_2 })
                }
            }
        }
    }

    @Test fun everyDifficultyAndLevelProducesSolvableChallenges() {
        for (d in com.triviamap.domain.model.Difficulty.values()) {
            val gen = ChallengeGenerator(Random(11))
            for (level in 1..80) {
                val c = gen.generate(level, lines, d)
                assertTrue(c.isSolvedBy(c.correctOrder, c.correctSides))
                assertTrue(c.tiles.size in 2..10)
                c.directions.forEach { assertTrue(it.toward.isNotBlank()) }
                if (c.type == ChallengeType.CLASSIFY) {
                    val cols = c.columnOrders.values.flatten().map { it.id }
                    assertEquals(c.correctOrder.map { it.id }.sorted(), cols.sorted())
                }
            }
        }
    }

    @Test fun weightedStationsComeUpMoreOften() {
        val target = lines.first { it.id == "A" }.stations[10].id
        val weights = mapOf(target to 200.0)
        fun hits(w: Map<String, Double>): Int {
            val gen = ChallengeGenerator(Random(3))
            return (0 until 400).count { i ->
                val c = gen.generate(8, lines.filter { it.id == "A" }, com.triviamap.domain.model.Difficulty.MEDIUM, w, Random(i), avoidRepeats = false)
                c.tiles.any { it.id == target }
            }
        }
        assertTrue(hits(weights) > hits(emptyMap()) + 100)
    }

    @Test fun consecutiveChallengesAreRarelyIdentical() {
        val gen = ChallengeGenerator(Random(5))
        val keys = (0 until 60).map { i -> gen.generate(8, lines, com.triviamap.domain.model.Difficulty.MEDIUM).let { "${it.line.id}${it.correctOrder.map { s -> s.id }}" } }
        val repeats = keys.zipWithNext().count { (a, b) -> a == b }
        assertEquals(0, repeats)
    }

    @Test fun sameSeedGivesSameChallenge() {
        val a = ChallengeGenerator(Random(7)).generate(12, lines)
        val b = ChallengeGenerator(Random(7)).generate(12, lines)
        assertEquals(a, b)
    }

    @Test fun solvedByCorrectOrder() {
        val c = ChallengeGenerator(Random(1)).generate(1, lines)
        assertTrue(c.isSolvedBy(c.correctOrder, c.correctSides))
        assertFalse(c.isSolvedBy(c.tiles, c.correctSides))
    }

    @Test fun stageOneUsesFullLineWhenTooFewHubs() {
        val single = lines.take(1).map { it.copy(stations = it.stations.map { s -> s.copy(lines = listOf(it.id)) }) }
        val c = ChallengeGenerator(Random(3)).generate(1, single)
        assertTrue(c.tiles.size >= 3)
    }

    @Test fun degenerateNetworksFallBackToReorder() {
        val one = lines.take(1)
        val gen = ChallengeGenerator(Random(5))
        for (level in 1..60) assertNotNull(gen.generate(level, one))
    }
}
