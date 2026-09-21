package com.triviamap.domain.progress

/** How well the player knows one station (updated after every Sprint answer). */
data class StationStat(
    val stationId: String,
    val attempts: Int = 0,
    val correct: Int = 0,
    /** Consecutive correct placements. */
    val streak: Int = 0,
    val lastSeenMs: Long = 0L
) {
    val accuracy: Float get() = if (attempts == 0) 0f else correct.toFloat() / attempts
    val isMastered: Boolean get() = streak >= MASTERY_STREAK

    fun record(placedCorrectly: Boolean, nowMs: Long) = copy(
        attempts = attempts + 1,
        correct = correct + if (placedCorrectly) 1 else 0,
        streak = if (placedCorrectly) streak + 1 else 0,
        lastSeenMs = nowMs
    )

    companion object {
        const val MASTERY_STREAK = 3

        /**
         * Draw weight for the challenge generator: unseen stations and stations the player
         * misses come up more often, mastered ones less.
         */
        fun weight(stat: StationStat?): Double = when {
            stat == null || stat.attempts == 0 -> 2.0
            stat.isMastered -> 0.5
            else -> 1.0 + 3.0 * (1.0 - stat.accuracy)
        }
    }
}
