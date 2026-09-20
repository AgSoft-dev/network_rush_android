package com.triviamap.domain.model

/** Daily-streak rule on calendar days (epoch days). */
object StreakCalculator {
    /** @param lastEpochDay day of the previous game, or -1 if none. */
    fun next(lastEpochDay: Long, currentStreak: Int, todayEpochDay: Long): Int = when {
        lastEpochDay == todayEpochDay -> currentStreak
        lastEpochDay >= 0 && todayEpochDay - lastEpochDay == 1L -> currentStreak + 1
        else -> 1
    }
}
