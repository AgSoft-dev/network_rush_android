package com.triviamap.domain

import com.triviamap.domain.model.GeoPoint
import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine
import com.triviamap.domain.progress.*
import org.junit.Assert.*
import org.junit.Test

class ProgressTest {

    @Test fun levelsAreMonotonicAndBoundariesExact() {
        assertEquals(1, Progression.levelFor(0).level)
        assertEquals(1, Progression.levelFor(99).level)
        assertEquals(2, Progression.levelFor(100).level)
        assertEquals(3, Progression.levelFor(300).level)
        var last = 0
        for (xp in 0..20_000 step 37) {
            val l = Progression.levelFor(xp).level
            assertTrue(l >= last); last = l
            val p = Progression.levelFor(xp)
            assertTrue(p.xpIntoLevel in 0 until p.xpForNext)
        }
    }

    @Test fun titlesAdvanceEveryThreeLevels() {
        assertEquals("Passenger", Progression.levelFor(0).title)
        assertNotEquals(Progression.levelFor(0).title, Progression.levelFor(Progression.xpToReach(4)).title)
    }

    @Test fun dailyRunsGiveBonusXp() {
        assertEquals(Progression.xpForRun(100, 5_000, false) + Progression.DAILY_BONUS_XP, Progression.xpForRun(100, 5_000, true))
    }

    @Test fun masteryNeedsThreeInARowAndResetsOnMiss() {
        var s = StationStat("x")
        repeat(2) { s = s.record(true, 1) }
        assertFalse(s.isMastered)
        s = s.record(true, 2)
        assertTrue(s.isMastered)
        s = s.record(false, 3)
        assertFalse(s.isMastered)
        assertEquals(4, s.attempts); assertEquals(3, s.correct)
    }

    @Test fun weightsFavourUnseenAndMissedStations() {
        val unseen = StationStat.weight(null)
        val missed = StationStat.weight(StationStat("a").record(false, 0).record(false, 0))
        val ok = StationStat.weight(StationStat("a").record(true, 0).record(true, 0))
        val mastered = StationStat.weight(StationStat("a").record(true, 0).record(true, 0).record(true, 0))
        assertTrue(missed > unseen)
        assertTrue(unseen > ok)
        assertTrue(ok > mastered)
    }

    private fun st(id: String) = Station(id, id.uppercase(), GeoPoint(0.0, 0.0), listOf("A"))
    private val line = TramLine("A", "Ligne A", 0xFF000000, geometry = emptyList(), stations = listOf("a", "b", "c").map(::st))

    @Test fun lineMasteryAndMostMissed() {
        val stats = mapOf(
            "a" to StationStat("a", 4, 4, 4),
            "b" to StationStat("b", 4, 1, 0),
            "c" to StationStat("c", 1, 0, 0)
        )
        val m = ProgressStats.lineMastery(listOf(line), stats).single()
        assertEquals(1, m.mastered); assertEquals(3, m.seen); assertEquals(3, m.total)
        // "c" has too few attempts to be ranked; "a" is perfect
        assertEquals(listOf("b"), ProgressStats.mostMissed(listOf(line), stats).map { it.station.id })
        assertFalse(ProgressStats.hasMasteredLine(listOf(line), stats))
        val all = stats.mapValues { (id, _) -> StationStat(id, 3, 3, 3) }
        assertTrue(ProgressStats.hasMasteredLine(listOf(line), all))
    }

    @Test fun badgesAreEvaluatedFromTheRunContext() {
        val base = Badges.Context(0, 0, 3, 1, 12, false, 10, false)
        assertEquals(setOf(Badges.FIRST_RUN), Badges.evaluate(base))
        val big = Badges.evaluate(base.copy(classifySolved = 10, maxCombo = 10, level = 21, streak = 7, hourOfDay = 23, isDaily = true, totalPlacements = 200, hasMasteredLine = true))
        assertEquals(Badges.all.map { it.id }.toSet(), big)
    }

    @Test fun shareTextIsCompactAndTruncated() {
        val t = ShareText.build(true, "MEDIUM", 20_000, 14, 12_340, 9, "GGRS" + "G".repeat(40))
        assertTrue(t.contains("Daily 2024-10-04"))
        assertTrue(t.contains("Level 14"))
        assertTrue(t.contains("🟩🟩🟥⬜"))
        assertTrue(t.endsWith("…"))
    }
}
