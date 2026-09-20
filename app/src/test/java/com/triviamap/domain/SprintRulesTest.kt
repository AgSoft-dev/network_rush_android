package com.triviamap.domain

import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.sprint.ChallengeType
import com.triviamap.domain.sprint.SprintRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SprintRulesTest {

    @Test fun stagesFollowLevelBrackets() {
        assertEquals(1, SprintRules.stage(1)); assertEquals(1, SprintRules.stage(5))
        assertEquals(2, SprintRules.stage(6)); assertEquals(4, SprintRules.stage(20))
        assertEquals(5, SprintRules.stage(21)); assertEquals(5, SprintRules.stage(500))
    }

    @Test fun wrongAnswerPenaltyGrowsWithStage() {
        assertEquals(5_000L, SprintRules.wrongAnswerPenaltyMs(1))
        assertEquals(9_000L, SprintRules.wrongAnswerPenaltyMs(25))
    }

    @Test fun skipIsNeverBetterThanAnswering() {
        val reward = SprintRules.correctAnswerReward(Difficulty.HARD, 25, ChallengeType.REORDER, 1, 30_000)
        assertTrue(reward.timeGainMs > 0)
        assertTrue(SprintRules.SKIP_PENALTY_MS > 0)
    }

    @Test fun fastAnswersScoreMoreThanSlowOnes() {
        val fast = SprintRules.correctAnswerReward(Difficulty.MEDIUM, 3, ChallengeType.REORDER, 1, 2_000)
        val slow = SprintRules.correctAnswerReward(Difficulty.MEDIUM, 3, ChallengeType.REORDER, 1, 40_000)
        assertTrue(fast.points > slow.points)
    }

    @Test fun classifyIsWorthDoubleAndGivesBonusTime() {
        val r = SprintRules.correctAnswerReward(Difficulty.MEDIUM, 1, ChallengeType.REORDER, 1, 30_000)
        val c = SprintRules.correctAnswerReward(Difficulty.MEDIUM, 1, ChallengeType.CLASSIFY, 1, 30_000)
        assertEquals(r.points * 2, c.points)
        assertEquals(r.timeGainMs + 5_000L, c.timeGainMs)
    }
}
