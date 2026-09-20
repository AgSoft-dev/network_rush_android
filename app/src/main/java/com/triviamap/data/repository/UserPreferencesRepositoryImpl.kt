package com.triviamap.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.triviamap.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private val clock: Clock = Clock.systemDefaultZone()

    private object Keys {
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val LAST_PLAYED_TIMESTAMP = longPreferencesKey("last_played_timestamp") // legacy
        val LAST_PLAYED_EPOCH_DAY = longPreferencesKey("last_played_epoch_day")
        val EARNED_BADGES = stringSetPreferencesKey("earned_badges")
    }

    override val dailyStreak: Flow<Int> = context.dataStore.data.map { it[Keys.DAILY_STREAK] ?: 0 }
    
    override val lastPlayedEpochDay: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_PLAYED_EPOCH_DAY] ?: -1L }
    
    override val earnedBadges: Flow<Set<String>> = context.dataStore.data.map { it[Keys.EARNED_BADGES] ?: emptySet() }

    override suspend fun updateStreak() = updateStreak(LocalDate.now(clock))

    /** Calendar-day based: same day = no change, next day = +1, anything else = reset to 1. */
    internal suspend fun updateStreak(today: LocalDate) {
        context.dataStore.edit { prefs ->
            val todayEpoch = today.toEpochDay()
            val lastEpoch = prefs[Keys.LAST_PLAYED_EPOCH_DAY] ?: -1L
            val currentStreak = prefs[Keys.DAILY_STREAK] ?: 0

            when {
                lastEpoch == todayEpoch -> Unit
                lastEpoch >= 0 && todayEpoch - lastEpoch == 1L -> prefs[Keys.DAILY_STREAK] = currentStreak + 1
                else -> prefs[Keys.DAILY_STREAK] = 1
            }
            prefs[Keys.LAST_PLAYED_EPOCH_DAY] = todayEpoch
        }
    }

    override suspend fun earnBadge(badgeId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.EARNED_BADGES] ?: emptySet()
            prefs[Keys.EARNED_BADGES] = current + badgeId
        }
    }
}
