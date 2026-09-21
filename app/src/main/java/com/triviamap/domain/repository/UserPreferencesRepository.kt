package com.triviamap.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val dailyStreak: Flow<Int>
    /** Epoch day (device time zone) of the last played game, or -1. */
    val lastPlayedEpochDay: Flow<Long>
    val earnedBadges: Flow<Set<String>>
    /** Drag handles on the left edge instead of the right. */
    val leftHanded: Flow<Boolean>
    /** Vibrations on drag, success and failure. */
    val hapticsEnabled: Flow<Boolean>
    /** Accumulated experience points. */
    val xp: Flow<Int>
    
    suspend fun updateStreak()
    suspend fun earnBadge(badgeId: String)
    suspend fun setLeftHanded(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun addXp(amount: Int)
}
