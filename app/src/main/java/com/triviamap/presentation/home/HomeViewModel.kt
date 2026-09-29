package com.triviamap.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triviamap.domain.model.GameResult
import com.triviamap.domain.progress.PlayerLevel
import com.triviamap.domain.progress.Progression
import com.triviamap.domain.repository.GameResultRepository
import com.triviamap.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val level: PlayerLevel = Progression.levelFor(0),
    val streak: Int = 0,
    val epochDay: Long = 0L,
    /** First daily-challenge result of today, if the player already played it. */
    val dailyToday: GameResult? = null,
    val isSupporter: Boolean = false,
    val showBanner: Boolean = false,
    /** Non-null while the Play update banner should be shown. */
    val update: com.triviamap.domain.update.UpdateState? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    results: GameResultRepository,
    ads: com.triviamap.domain.monetization.AdsController,
    private val updates: com.triviamap.domain.update.AppUpdateChecker,
    clock: Clock
) : ViewModel() {

    // Re-evaluated every minute so the daily card flips at midnight
    private val today = flow {
        while (true) {
            emit(LocalDate.now(clock).toEpochDay())
            delay(60_000)
        }
    }.distinctUntilChanged()

    private val progress = combine(
        prefs.xp,
        prefs.dailyStreak,
        prefs.lastPlayedEpochDay,
        today.flatMapLatest { day -> results.dailyResult(day).let { f -> combine(f, flow { emit(day) }) { r, d -> r to d } } }
    ) { xp, streak, lastDay, (daily, day) ->
        // A streak whose last day is older than yesterday is already broken, even if not yet reset on disk
        val liveStreak = if (lastDay >= 0 && day - lastDay <= 1) streak else 0
        HomeUiState(Progression.levelFor(xp), liveStreak, day, daily)
    }

    private val updateBanner = combine(updates.state, prefs.dismissedUpdateVersion) { state, dismissed ->
        state.takeIf { com.triviamap.domain.update.UpdatePolicy.shouldShow(it, dismissed) }
    }

    val state = combine(progress, prefs.isSupporter, ads.canRequestAds, updateBanner) { ui, supporter, canAds, update ->
        ui.copy(
            isSupporter = supporter,
            showBanner = com.triviamap.domain.monetization.MonetizationPolicy.shouldShowBanner(supporter, canAds),
            update = update
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun startUpdate(activity: android.app.Activity) = updates.startUpdate(activity)

    fun installUpdate() = updates.install()

    fun dismissUpdate() {
        val available = updates.state.value as? com.triviamap.domain.update.UpdateState.Available ?: return
        viewModelScope.launch { prefs.setDismissedUpdateVersion(available.versionCode) }
    }
}
