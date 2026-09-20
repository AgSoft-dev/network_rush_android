package com.triviamap.domain.sprint

import com.triviamap.domain.model.Difficulty

/** Pure Station Sprint balancing rules (no Android, no clock, no randomness). */
object SprintRules {

    const val SKIP_PENALTY_MS = 4_000L
    const val BURST_LIMIT_MS = 6_000L
    const val BURST_TIMEOUT_PENALTY_MS = 3_000L

    fun maxTimeMs(difficulty: Difficulty): Long = when (difficulty) {
        Difficulty.EASY -> 60_000L
        Difficulty.MEDIUM -> 45_000L
        Difficulty.HARD -> 30_000L
    }

    fun stage(level: Int): Int = when (level) {
        in 1..5 -> 1
        in 6..10 -> 2
        in 11..15 -> 3
        in 16..20 -> 4
        else -> 5
    }

    fun wrongAnswerPenaltyMs(level: Int): Long = 5_000L + (stage(level) - 1) * 1_000L

    data class Reward(val timeGainMs: Long, val points: Int)

    fun correctAnswerReward(
        difficulty: Difficulty,
        level: Int,
        type: ChallengeType,
        newCombo: Int,
        timeTakenMs: Long
    ): Reward {
        val stage = stage(level)
        val baseGain = when (difficulty) {
            Difficulty.EASY -> 12_000L - (stage - 1) * 1000L
            Difficulty.MEDIUM -> 10_000L - (stage - 1) * 1000L
            Difficulty.HARD -> 8_000L - (stage - 1) * 500L
        }.coerceAtLeast(4000L)
        val comboBonus = (newCombo * 1000L).coerceAtMost(5000L)
        val typeBonus = when (type) {
            ChallengeType.CLASSIFY -> 5000L
            ChallengeType.SPEED_BURST -> 2000L
            ChallengeType.REORDER -> 0L
        }

        val basePoints = (500 + level * 50) * (if (type == ChallengeType.CLASSIFY) 2 else 1)
        val speedFactor = (1.5f - (timeTakenMs / 20_000f)).coerceIn(1.0f, 1.5f)
        val comboFactor = 1f + (newCombo * 0.1f).coerceAtMost(1.0f)
        return Reward(baseGain + comboBonus + typeBonus, (basePoints * speedFactor * comboFactor).toInt())
    }
}
