package com.triviamap.presentation.gameplay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triviamap.domain.model.*
import com.triviamap.domain.repository.GameResultRepository
import com.triviamap.domain.repository.LinesState
import com.triviamap.domain.repository.TramLineRepository
import com.triviamap.domain.repository.UserPreferencesRepository
import com.triviamap.domain.sprint.Challenge
import com.triviamap.domain.sprint.ChallengeGenerator
import com.triviamap.domain.sprint.ChallengeType
import com.triviamap.domain.sprint.Side
import com.triviamap.domain.sprint.SprintRules
import com.triviamap.util.TimeSource
import com.triviamap.util.GeoBounds
import com.triviamap.util.ScoreBreakdown
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.roundToInt

data class SprintUiState(
    val phase: GamePhase = GamePhase.Loading,
    val challengeType: ChallengeType = ChallengeType.REORDER,
    val correctOrder: List<Station> = emptyList(),
    val currentTiles: List<Station> = emptyList(),
    /** For CLASSIFY: station ID to current horizontal side (-1 left, 0 center, 1 right) */
    val stationSides: Map<String, Int> = emptyMap(),
    /** For CLASSIFY: station ID to target horizontal side */
    val correctSides: Map<String, Int> = emptyMap(),
    val line: TramLine? = null,
    val line2: TramLine? = null,
    val bounds: GeoBounds? = null,
    val score: Int = 0,
    val timeLeftMs: Long = 0,
    val level: Int = 1,
    val stage: Int = 1,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val isForward: Boolean = true,
    /** Visual feedback triggers */
    val lastTimeGain: Int = 0,
    val showFeedback: Boolean = false,
    val isCorrectFeedback: Boolean = true,
    val feedbackTrigger: Int = 0,
    val lastTimePenalty: Int = 0,
    /** True when the bundled line data could not be loaded. */
    val loadFailed: Boolean = false,
    
    /** Stats for scoring improvements */
    val challengeStartedAt: Long = 0L,
    val correctSubmissions: Int = 0,
    val totalSubmissions: Int = 0,
    val correctClassifyCount: Int = 0,

    /** Cumulative session path for the map */
    val sessionStations: List<Station> = emptyList(),

    /** Speed Burst local timer (0..1.0) */
    val burstTimer: Float = 0f,

    /** Result data */
    val isNewRecord: Boolean = false,
    val streak: Int = 0
)

@HiltViewModel
class SprintViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lineRepository: TramLineRepository,
    private val resultRepository: GameResultRepository,
    private val userPrefs: UserPreferencesRepository,
    private val generator: ChallengeGenerator,
    private val time: TimeSource
) : ViewModel() {

    private val difficulty: Difficulty = Difficulty.valueOf(
        checkNotNull(savedStateHandle["difficulty"])
    )

    private val maxTimeMs: Long = SprintRules.maxTimeMs(difficulty)

    private val _state = MutableStateFlow(SprintUiState(
        difficulty = difficulty,
        timeLeftMs = maxTimeMs
    ))
    val state: StateFlow<SprintUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var burstTimerJob: Job? = null
    private var allLines: List<TramLine> = emptyList()
    private var current: Challenge? = null

    /** Set once the run is over; every state-mutating entry point bails out afterwards. */
    private var finished = false
    private var paused = false
    private var pausedAt = 0L
    private var gameStartedAt = 0L

    init {
        loadLines()
    }

    private fun loadLines() = viewModelScope.launch {
        _state.update { it.copy(loadFailed = false) }
        launch { lineRepository.load() }
        lineRepository.state.first { it !is LinesState.Loading }.let { loaded ->
            when (loaded) {
                is LinesState.Loaded -> {
                    // Sprint needs at least 3 stations per line (speed burst window)
                    allLines = loaded.lines.filter { it.stations.size >= 3 }
                    if (allLines.isEmpty()) _state.update { it.copy(loadFailed = true) }
                    else if (!finished && timerJob == null) {
                        gameStartedAt = time.elapsedMs()
                        generateChallenge()
                    }
                }
                is LinesState.Error -> _state.update { it.copy(loadFailed = true) }
                LinesState.Loading -> Unit
            }
        }
    }

    fun retryLoad() {
        if (allLines.isEmpty()) loadLines()
    }

    /** Called when the screen leaves the foreground: freezes every timer. */
    fun pause() {
        if (finished || paused || timerJob == null) return
        paused = true
        pausedAt = time.elapsedMs()
        burstTimerJob?.cancel()
    }

    fun resume() {
        if (!paused) return
        paused = false
        val pausedFor = time.elapsedMs() - pausedAt
        _state.update { it.copy(challengeStartedAt = it.challengeStartedAt + pausedFor) }
        if (_state.value.challengeType == ChallengeType.SPEED_BURST) startBurstTimer()
    }

    private fun generateChallenge() {
        if (allLines.isEmpty() || finished) return

        val level = _state.value.level
        val challenge = generator.generate(level, allLines)
        val geometry = challenge.line.geometry.ifEmpty { challenge.line.stations.map { it.position } }

        _state.update {
            it.copy(
                challengeType = challenge.type,
                line = challenge.line,
                line2 = challenge.line2,
                bounds = GeoBounds.from(geometry),
                correctOrder = challenge.correctOrder,
                currentTiles = challenge.tiles,
                stationSides = challenge.tiles.associate { s -> s.id to Side.HUB },
                correctSides = challenge.correctSides,
                isForward = challenge.isForward,
                phase = GamePhase.Drawing,
                challengeStartedAt = time.elapsedMs(),
                stage = SprintRules.stage(level)
            )
        }
        current = challenge

        if (timerJob == null) startTimer()
        if (challenge.type == ChallengeType.SPEED_BURST) startBurstTimer() else stopBurstTimer()
    }

    private fun startBurstTimer() {
        burstTimerJob?.cancel()
        burstTimerJob = viewModelScope.launch {
            val limit = SprintRules.BURST_LIMIT_MS
            val start = time.elapsedMs()
            while (true) {
                val progress = (time.elapsedMs() - start).toFloat() / limit
                if (progress >= 1f) {
                    _state.update { it.copy(burstTimer = 1f) }
                    handleBurstTimeout()
                    break
                }
                _state.update { it.copy(burstTimer = progress) }
                delay(30)
            }
        }
    }

    private fun stopBurstTimer() {
        burstTimerJob?.cancel()
        _state.update { it.copy(burstTimer = 0f) }
    }

    private fun handleBurstTimeout() {
        if (finished) return
        val penalty = SprintRules.BURST_TIMEOUT_PENALTY_MS
        viewModelScope.launch {
            _state.update { it.copy(
                timeLeftMs = (it.timeLeftMs - penalty).coerceAtLeast(0),
                combo = 0,
                showFeedback = true,
                isCorrectFeedback = false,
                lastTimePenalty = (penalty / 1000).toInt(),
                feedbackTrigger = it.feedbackTrigger + 1
            ) }
            delay(700)
            _state.update { it.copy(showFeedback = false) }
            generateChallenge()
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            var lastUpdate = time.elapsedMs()
            while (!finished && _state.value.timeLeftMs > 0) {
                delay(50)
                val now = time.elapsedMs()
                if (paused) { lastUpdate = now; continue }
                val delta = now - lastUpdate
                lastUpdate = now
                _state.update { it.copy(timeLeftMs = (it.timeLeftMs - delta).coerceAtLeast(0)) }
            }
            endGame()
        }
    }

    private fun endGame() = viewModelScope.launch {
        if (finished) return@launch
        finished = true
        val finalState = _state.value
        stopBurstTimer()
        _state.update { it.copy(phase = GamePhase.Validating, showFeedback = false) }

        // Comparison for Record
        val previousHigh = resultRepository.getHighScore(GameMode.STATION_SPRINT, difficulty)
        val isNewRecord = finalState.score > previousHigh && previousHigh > 0
        
        val accuracy = if (finalState.totalSubmissions > 0) {
            finalState.correctSubmissions.toFloat() / finalState.totalSubmissions
        } else 0f
        
        resultRepository.saveResult(GameResult(
            lineId = "SPRINT",
            mode = GameMode.STATION_SPRINT,
            difficulty = difficulty,
            score = finalState.score,
            stationOrderScore = accuracy,
            pathAccuracyScore = 0f,
            completionScore = 0f,
            speedBonusScore = 0f,
            durationMs = time.elapsedMs() - gameStartedAt,
            level = finalState.level,
            maxCombo = finalState.maxCombo,
            accuracy = accuracy
        ))

        // Update daily streak
        userPrefs.updateStreak()
        val currentStreak = userPrefs.dailyStreak.first()
        
        // Award theme badges
        if (finalState.correctClassifyCount >= 10) userPrefs.earnBadge("hub_expert")
        
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (hour >= 22 || hour <= 4) userPrefs.earnBadge("night_rider")

        delay(800)
        _state.update { it.copy(
            isNewRecord = isNewRecord,
            streak = currentStreak,
            phase = GamePhase.Finished(
                // Sprint only uses accuracy + total; level / maxCombo travel in SprintUiState
                ScoreBreakdown(
                    stationOrder = accuracy,
                    completion = 0f,
                    speedBonus = 0f,
                    total = finalState.score
                )
            )
        ) }
    }

    fun moveTile(fromIndex: Int, toIndex: Int) {
        if (finished || paused || _state.value.phase !is GamePhase.Drawing) return
        _state.update { state ->
            val list = state.currentTiles.toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices) return@update state
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            state.copy(currentTiles = list)
        }
    }

    fun setStationSide(stationId: String, side: Int) {
        if (finished || paused || _state.value.phase !is GamePhase.Drawing) return
        _state.update { it.copy(stationSides = it.stationSides + (stationId to side)) }
    }

    /** Skipping costs time and the combo: it must never be better than answering. */
    fun skipQuestion() {
        if (finished || paused || _state.value.showFeedback || _state.value.phase !is GamePhase.Drawing) return
        stopBurstTimer()
        _state.update { it.copy(
            totalSubmissions = it.totalSubmissions + 1,
            timeLeftMs = (it.timeLeftMs - SprintRules.SKIP_PENALTY_MS).coerceAtLeast(0),
            combo = 0
        ) }
        generateChallenge()
    }

    fun submit() {
        val currentState = _state.value
        if (finished || paused || currentState.showFeedback || currentState.phase !is GamePhase.Drawing) return

        _state.update { it.copy(totalSubmissions = it.totalSubmissions + 1) }
        
        val isCorrect = current?.isSolvedBy(currentState.currentTiles, currentState.stationSides) ?: false
        
        if (isCorrect) {
            handleCorrectAnswer()
        } else {
            val timePenalty = SprintRules.wrongAnswerPenaltyMs(currentState.level)
            
            viewModelScope.launch {
                _state.update { it.copy(
                    timeLeftMs = (it.timeLeftMs - timePenalty).coerceAtLeast(0),
                    combo = 0,
                    showFeedback = true,
                    isCorrectFeedback = false,
                    lastTimePenalty = (timePenalty / 1000).toInt(),
                    feedbackTrigger = it.feedbackTrigger + 1
                ) }
                delay(700)
                _state.update { it.copy(showFeedback = false) }
            }
        }
    }

    private fun handleCorrectAnswer() {
        val currentState = _state.value
        val timeTakenMs = time.elapsedMs() - currentState.challengeStartedAt

        val newCombo = currentState.combo + 1
        val reward = SprintRules.correctAnswerReward(difficulty, currentState.level, currentState.challengeType, newCombo, timeTakenMs)
        val timeGain = reward.timeGainMs
        val pointsGain = reward.points

        viewModelScope.launch {
            stopBurstTimer()
            _state.update { state ->
                val newTimeLeft = (state.timeLeftMs + timeGain).coerceAtMost(maxTimeMs)
                val actualGainMs = newTimeLeft - state.timeLeftMs
                val actualSecondsGained = (actualGainMs / 1000f).roundToInt()

                state.copy(
                    score = state.score + pointsGain,
                    timeLeftMs = newTimeLeft,
                    lastTimeGain = actualSecondsGained,
                    level = state.level + 1,
                    combo = newCombo,
                    maxCombo = maxOf(state.maxCombo, newCombo),
                    correctSubmissions = state.correctSubmissions + 1,
                    correctClassifyCount = if (state.challengeType == ChallengeType.CLASSIFY) state.correctClassifyCount + 1 else state.correctClassifyCount,
                    showFeedback = true,
                    isCorrectFeedback = true,
                    feedbackTrigger = state.feedbackTrigger + 1,
                    sessionStations = state.sessionStations + state.correctOrder
                )
            }
            delay(700)
            _state.update { it.copy(showFeedback = false) }
            generateChallenge()
        }
    }

}
