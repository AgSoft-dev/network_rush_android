package com.triviamap.domain.usecase

import com.triviamap.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserPrefsUseCase @Inject constructor(
    private val repo: UserPreferencesRepository
) {
    val dailyStreak: Flow<Int> = repo.dailyStreak
    val earnedBadges: Flow<Set<String>> = repo.earnedBadges
}

class UpdateStreakUseCase @Inject constructor(
    private val repo: UserPreferencesRepository
) {
    suspend operator fun invoke() = repo.updateStreak()
}

class EarnBadgeUseCase @Inject constructor(
    private val repo: UserPreferencesRepository
) {
    suspend operator fun invoke(badgeId: String) = repo.earnBadge(badgeId)
}
