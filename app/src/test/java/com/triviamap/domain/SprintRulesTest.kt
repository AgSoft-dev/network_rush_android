package com.triviamap.domain

import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.sprint.ChallengeType
import com.triviamap.domain.sprint.SprintRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SprintRulesTest {

    @Test fun stagesFollowLevelBrackets() {
        assertEquals(1, SprintRules.stage(1)); assertEquals(1, SprintRules.stage(5))
        assertEquals(2, SprintRules.stage(6)); assertEquals(4, SprintRules.stage(20))
        assertEquals(5, SprintRules.stage(21)); assertEquals(5, SprintRules.stage(500))
    }

    @Test fun wrongAnswerPenaltyGrowsWithStageAndMisplacedFraction() {
        assertEquals(4_000L, SprintRules.wrongAnswerPenaltyMs(1, 1f))
        assertEquals(6_000L, SprintRules.wrongAnswerPenaltyMs(25, 1f))
        assertTrue(SprintRules.wrongAnswerPenaltyMs(1, 0.2f) < SprintRules.wrongAnswerPenaltyMs(1, 1f))
        assertEquals(2_000L, SprintRules.wrongAnswerPenaltyMs(1, 0f))
    }

    @Test fun timeGainShrinksWithLevelButNeverDisappears() {
        fun gain(level: Int) = SprintRules.correctAnswerReward(Difficulty.MEDIUM, level, ChallengeType.REORDER, 6, 1, 8_000).timeGainMs
        assertTrue(gain(1) > gain(10))
        assertTrue(gain(10) > gain(20))
        assertEquals("floor reached", gain(60), gain(200))
        assertTrue(gain(200) > 0)
    }

    @Test fun biggerPuzzlesGiveMoreTime() {
        val small = SprintRules.correctAnswerReward(Difficulty.MEDIUM, 3, ChallengeType.REORDER, 3, 1, 5_000)
        val big = SprintRules.correctAnswerReward(Difficulty.MEDIUM, 3, ChallengeType.REORDER, 8, 1, 5_000)
        assertTrue(big.timeGainMs > small.timeGainMs)
    }

    @Test fun fastAnswersScoreMoreThanSlowOnes() {
        val fast = SprintRules.correctAnswerReward(Difficulty.MEDIUM, 3, ChallengeType.REORDER, 5, 1, 2_000)
        val slow = SprintRules.correctAnswerReward(Difficulty.MEDIUM, 3, ChallengeType.REORDER, 5, 1, 40_000)
        assertTrue(fast.points > slow.points)
    }

    @Test fun comboMultiplierGoesUpToTriple() {
        fun pts(combo: Int) = SprintRules.correctAnswerReward(Difficulty.MEDIUM, 3, ChallengeType.REORDER, 5, combo, 60_000).points
        assertTrue(pts(10) > pts(1))
        assertEquals(pts(20), pts(40))
    }

    @Test fun classifyIsWorthDoubleAndGivesBonusTime() {
        val r = SprintRules.correctAnswerReward(Difficulty.MEDIUM, 1, ChallengeType.REORDER, 6, 1, 60_000)
        val c = SprintRules.correctAnswerReward(Difficulty.MEDIUM, 1, ChallengeType.CLASSIFY, 6, 1, 60_000)
        assertEquals(r.points * 2, c.points)
        assertEquals(r.timeGainMs + 3_500L, c.timeGainMs)
    }

    @Test fun burstGetsTighterWithLevel() {
        assertTrue(SprintRules.burstLimitMs(1) > SprintRules.burstLimitMs(12))
        assertTrue(SprintRules.burstLimitMs(12) > SprintRules.burstLimitMs(30))
    }

    @Test fun tileCountKeepsGrowingInStageFiveUpToTen() {
        val r = Random(1)
        assertTrue((1..200).all { SprintRules.tileCount(it, r) in 3..10 })
        assertEquals(10, SprintRules.tileCount(500, r))
        assertTrue(SprintRules.tileCount(21, Random(1)) <= 8)
    }
}
