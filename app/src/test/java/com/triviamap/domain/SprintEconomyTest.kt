package com.triviamap.domain

import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.sprint.ChallengeType
import com.triviamap.domain.sprint.SprintRules
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Simulates a whole run with the real rules to check the balance targets:
 * a run is always finite and survival grows with skill.
 */
class SprintEconomyTest {

    private class Player(val speed: Double, val accuracy: Double)

    private fun run(difficulty: Difficulty, player: Player, seed: Int): Pair<Double, Int> {
        val rnd = Random(seed)
        var timeLeft = SprintRules.maxTimeMs(difficulty) / 1000.0
        var clock = 0.0
        var level = 1
        var combo = 0
        while (timeLeft > 0 && clock < 5_000) {
            val tiles = SprintRules.tileCount(level, rnd)
            val stage = SprintRules.stage(level)
            val type = if (stage >= 3 && level % 3 == 0) ChallengeType.CLASSIFY else ChallengeType.REORDER
            val solve = (if (type == ChallengeType.CLASSIFY) 3.0 + 2.0 * tiles else 2.0 + 1.3 * tiles) *
                player.speed * (0.8 + 0.4 * rnd.nextDouble())
            clock += solve; timeLeft -= solve
            if (timeLeft <= 0) break
            val acc = (player.accuracy - 0.015 * (stage - 1) - 0.01 * maxOf(0, level - 20)).coerceAtLeast(0.5)
            if (rnd.nextDouble() < acc) {
                combo++
                val r = SprintRules.correctAnswerReward(difficulty, level, type, tiles, combo, (solve * 1000).toLong())
                timeLeft = minOf(SprintRules.maxTimeMs(difficulty) / 1000.0, timeLeft + r.timeGainMs / 1000.0)
                level++
            } else {
                combo = 0
                timeLeft -= SprintRules.wrongAnswerPenaltyMs(level, 0.5f) / 1000.0
            }
        }
        return clock to level
    }

    private fun median(difficulty: Difficulty, p: Player) =
        (0 until 300).map { run(difficulty, p, it).first }.sorted()[150]

    private val casual = Player(1.5, 0.75)
    private val average = Player(1.0, 0.85)
    private val expert = Player(0.7, 0.95)

    @Test fun averagePlayerSurvivesAboutAMinuteAndAHalfOnMedium() {
        val m = median(Difficulty.MEDIUM, average)
        assertTrue("median=$m", m in 60.0..140.0)
    }

    @Test fun survivalGrowsWithSkill() {
        for (d in Difficulty.values()) {
            assertTrue(median(d, casual) < median(d, average))
            assertTrue(median(d, average) < median(d, expert))
        }
    }

    @Test fun harderDifficultiesAreShorter() {
        assertTrue(median(Difficulty.HARD, average) < median(Difficulty.MEDIUM, average))
        assertTrue(median(Difficulty.MEDIUM, average) < median(Difficulty.EASY, average))
    }

    @Test fun aRunAlwaysEndsEvenForAPerfectFastPlayer() {
        val perfect = Player(0.5, 1.0)
        for (d in Difficulty.values()) {
            for (seed in 0 until 20) {
                val (clock, _) = run(d, perfect, seed)
                assertTrue("$d seed=$seed lasted ${clock}s", clock < 1_500)
            }
        }
    }
}
