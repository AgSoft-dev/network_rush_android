package com.triviamap.domain.model

/**
 * A geographic coordinate or screen-space point (x, y).
 */
data class GeoPoint(val x: Double, val y: Double)

/**
 * A named tram station.
 */
data class Station(
    val id: String,
    val name: String,
    val position: GeoPoint,
    val lines: List<String> = emptyList()
)

/**
 * A full tram line definition with its polyline geometry and ordered station list.
 */
data class TramLine(
    val id: String,
    val name: String,          // e.g. "A", "B", "C"
    val color: Long,           // 0xAARRGGBB
    val textColor: Long = 0xFFFFFFFFL,
    val geometry: List<GeoPoint>,
    val stations: List<Station>
)

enum class GameMode {
    TRACE_NETWORK,
    STATION_SPRINT
}

/**
 * Difficulty level for a game session.
 */
enum class Difficulty {
    EASY,    // names + positions + ghost hint
    MEDIUM,  // positions only, no hint
    HARD     // positions only, timed, accuracy penalties
}

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
    val accuracy: Float = 0f
)
