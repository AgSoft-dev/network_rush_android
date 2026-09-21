package com.triviamap.domain.model

enum class GameMode {
    TRACE_NETWORK,
    STATION_SPRINT,
    /** Station Sprint with the same challenges for everyone on a given day. */
    DAILY_SPRINT
}

/**
 * Difficulty level for a game session.
 */
enum class Difficulty {
    EASY,    // Sprint: 60 s clock, contiguous segments, forward direction for longer
    MEDIUM,  // Sprint: 45 s clock, some sparse (non-contiguous) sets
    HARD     // Sprint: 30 s clock, reverse direction from the start, mostly sparse sets
}
