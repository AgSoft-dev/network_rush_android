package com.triviamap.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val dailyStreak: Flow<Int>
    /** Epoch day (device time zone) of the last played game, or -1. */
    val lastPlayedEpochDay: Flow<Long>
    val earnedBadges: Flow<Set<String>>
    
    suspend fun updateStreak()
    suspend fun earnBadge(badgeId: String)
}
