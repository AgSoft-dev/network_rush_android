package com.triviamap.domain.progress

import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GameMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the results screen shows about the run that just ended. */
data class RunSummary(
    val mode: GameMode,
    val difficulty: Difficulty,
    val score: Int,
    val level: Int,
    val maxCombo: Int,
    val accuracy: Float,
    val isNewRecord: Boolean,
    val streak: Int,
    val xpEarned: Int,
    val levelBefore: PlayerLevel,
    val levelAfter: PlayerLevel,
    val newBadges: List<Badge>,
    val answerLog: String,
    val epochDay: Long,
    /** Run duration (the daily's tie-breaker). */
    val durationMs: Long = 0L
)

/** In-memory hand-off between the game screen and the results screen. */
@Singleton
class RunSummaryHolder @Inject constructor() {
    private val _summary = MutableStateFlow<RunSummary?>(null)
    val summary: StateFlow<RunSummary?> = _summary.asStateFlow()

    fun publish(summary: RunSummary) { _summary.value = summary }
}
