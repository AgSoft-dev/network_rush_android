package com.triviamap.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val dailyStreak: Flow<Int>
    /** Epoch day (device time zone) of the last played game, or -1. */
    val lastPlayedEpochDay: Flow<Long>
    val earnedBadges: Flow<Set<String>>
    /** Vibrations on drag, success and failure. */
    val hapticsEnabled: Flow<Boolean>
    /** Accumulated experience points. */
    val xp: Flow<Int>
    /** True once the player has tipped at least once (hides the home banner). */
    val isSupporter: Flow<Boolean>
    /** versionCode of the Play update the player last dismissed (0 = none). */
    val dismissedUpdateVersion: Flow<Int>

    suspend fun updateStreak()
    suspend fun earnBadge(badgeId: String)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setSupporter(value: Boolean)
    suspend fun addXp(amount: Int)
    suspend fun setDismissedUpdateVersion(versionCode: Int)
}
