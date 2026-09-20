package com.triviamap.domain.model

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
