package com.triviamap.domain

import com.triviamap.data.model.GeoJsonParser
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.sprint.ChallengeGenerator
import com.triviamap.domain.sprint.DailyChallenge
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class DailyChallengeTest {
    private val lines = GeoJsonParser.parseData(File("src/main/assets/strasbourg_stations.json").readText())

    private fun daily(day: Long, level: Int, index: Int) =
        ChallengeGenerator(Random(999)).generate(level, lines, Difficulty.MEDIUM, emptyMap(), DailyChallenge.random(day, index))

    @Test fun sameDayAndIndexGiveTheSameChallengeWhateverTheGeneratorState() {
        assertEquals(daily(20_000, 7, 30), daily(20_000, 7, 30))
    }

    @Test fun differentDaysGiveDifferentChallenges() {
        val distinct = (20_000L..20_030L).map { daily(it, 7, 28).correctOrder.map { s -> s.id } }.toSet()
        assertTrue("only ${distinct.size} distinct challenges over 31 days", distinct.size > 20)
    }
}
