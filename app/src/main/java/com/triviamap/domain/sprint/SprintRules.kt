package com.triviamap.domain.sprint

import com.triviamap.domain.model.Difficulty
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Station Sprint balancing rules (no Android, no clock, no randomness).
 *
 * Economy: time gained scales with the puzzle size but decays with the level, so a run is
 * always finite (see SprintEconomyTest for the survival targets).
 */
object SprintRules {

    const val SKIP_PENALTY_MS = 4_000L
    const val MAX_SKIPS = 3

    /** Daily challenge: fixed number of questions, one attempt each, difficulty ramps up question by question. */
    const val DAILY_QUESTIONS = 8
    private const val DAILY_LEVEL_STEP = 3

    /** Generator level of the daily question at [index] (0-based): stages 1,1,2,2,3,4,4,5. */
    fun dailyLevel(index: Int): Int = 1 + DAILY_LEVEL_STEP * index
    const val BURST_TIMEOUT_PENALTY_MS = 3_000L

    private const val LEVEL_DECAY = 0.03
    private const val COMBO_BONUS_MS = 400L
    private const val COMBO_BONUS_STEPS = 5

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

    /** Time to answer a rapid-fire challenge before it counts as a failure. */
    fun burstLimitMs(level: Int): Long = when (stage(level)) {
        1, 2 -> 8_000L
        3 -> 7_000L
        4 -> 6_500L
        else -> 6_000L
    }

    /** Base penalty grows with the stage; it is halved when the answer was almost right. */
    fun wrongAnswerPenaltyMs(level: Int, misplacedFraction: Float = 1f): Long {
        val base = 4_000L + (stage(level) - 1) * 500L
        return (base * (0.5 + 0.5 * misplacedFraction.coerceIn(0f, 1f))).toLong()
    }

    /** How many tiles a level should use (stage 5 keeps growing up to 10). */
    fun tileCount(level: Int, random: kotlin.random.Random): Int = when (stage(level)) {
        1 -> random.nextInt(3, 5)
        2 -> 5
        3 -> 6
        4 -> random.nextInt(6, 8)
        else -> min(10, 7 + (level - 21) / 6 + random.nextInt(0, 2))
    }

    data class Reward(val timeGainMs: Long, val points: Int)

    fun correctAnswerReward(
        difficulty: Difficulty,
        level: Int,
        type: ChallengeType,
        tileCount: Int,
        newCombo: Int,
        timeTakenMs: Long
    ): Reward {
        val (perTileMs, floor) = when (difficulty) {
            Difficulty.EASY -> 1_300L to 0.40
            Difficulty.MEDIUM -> 1_050L to 0.35
            Difficulty.HARD -> 950L to 0.30
        }
        val levelFactor = max(floor, 1.0 - LEVEL_DECAY * (level - 1))
        val comboBonus = min(newCombo, COMBO_BONUS_STEPS) * COMBO_BONUS_MS
        val typeBonus = when (type) {
            ChallengeType.CLASSIFY -> 3_500L
            ChallengeType.SPEED_BURST -> 1_500L
            ChallengeType.REORDER -> 0L
        }
        val timeGain = (perTileMs * tileCount * levelFactor).toLong() + comboBonus + typeBonus

        val basePoints = (500 + level * 50) * (if (type == ChallengeType.CLASSIFY) 2 else 1)
        // Speed is judged against the size of the puzzle, not in absolute seconds
        val expectedMs = 2_000f + 1_300f * tileCount
        val speedFactor = (2.0f - 0.7f * timeTakenMs / expectedMs).coerceIn(1.0f, 2.0f)
        val comboFactor = 1f + 0.1f * min(newCombo, 20)
        return Reward(timeGain, (basePoints * speedFactor * comboFactor).toInt())
    }
}
