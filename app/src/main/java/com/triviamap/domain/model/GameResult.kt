package com.triviamap.domain.model

/**
 * A recorded game session result.
 */
data class GameResult(
    val id: Long = 0L,
    val lineId: String,
    val mode: GameMode = GameMode.TRACE_NETWORK,
    val difficulty: Difficulty,
    val score: Int,
    val stationOrderScore: Float,   // 0..1
    val pathAccuracyScore: Float,   // 0..1
    val completionScore: Float,     // 0..1
    val speedBonusScore: Float,     // 0..1
    val durationMs: Long,
    val timestampMs: Long = System.currentTimeMillis(),
    val playerPath: List<GeoPoint> = emptyList(),
    /** Sprint: last level reached. */
    val level: Int = 0,
    /** Sprint: best combo streak. */
    val maxCombo: Int = 0,
    /** Sprint: correct submissions / total submissions (0..1). */
    val accuracy: Float = 0f,
    /** Sprint: one char per answer (G correct, R wrong, S skipped) for the shareable recap. */
    val answerLog: String = ""
)
