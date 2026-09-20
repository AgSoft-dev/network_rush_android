package com.triviamap.presentation.gameplay

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triviamap.domain.model.*
import com.triviamap.domain.repository.GameResultRepository
import com.triviamap.domain.repository.LinesState
import com.triviamap.domain.repository.TramLineRepository
import com.triviamap.domain.repository.UserPreferencesRepository
import com.triviamap.domain.usecase.SaveGameResultUseCase
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

enum class ChallengeType {
    REORDER,     // Single line, reorder stations
    CLASSIFY,    // Two lines, drag left/right/center AND reorder
    SPEED_BURST  // Rapid fire 3-tile challenge with per-tile timer
}

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
    private val saveResult: SaveGameResultUseCase,
    private val resultRepository: GameResultRepository,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val difficulty: Difficulty = Difficulty.valueOf(
        checkNotNull(savedStateHandle["difficulty"])
    )

    private val maxTimeMs: Long = when(difficulty) {
        Difficulty.EASY -> 60_000L
        Difficulty.MEDIUM -> 45_000L
        Difficulty.HARD -> 30_000L
    }

    private val _state = MutableStateFlow(SprintUiState(
        difficulty = difficulty,
        timeLeftMs = maxTimeMs
    ))
    val state: StateFlow<SprintUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var burstTimerJob: Job? = null
    private var allLines: List<TramLine> = emptyList()

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
                        gameStartedAt = SystemClock.elapsedRealtime()
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
        pausedAt = SystemClock.elapsedRealtime()
        burstTimerJob?.cancel()
    }

    fun resume() {
        if (!paused) return
        paused = false
        val pausedFor = SystemClock.elapsedRealtime() - pausedAt
        _state.update { it.copy(challengeStartedAt = it.challengeStartedAt + pausedFor) }
        if (_state.value.challengeType == ChallengeType.SPEED_BURST) startBurstTimer()
    }

    private fun getStage(level: Int): Int = when (level) {
        in 1..5 -> 1
        in 6..10 -> 2
        in 11..15 -> 3
        in 16..20 -> 4
        else -> 5
    }

    private fun generateChallenge() {
        if (allLines.isEmpty() || finished) return
        
        val level = _state.value.level
        val stage = getStage(level)
        
        var type = when (stage) {
            1, 2 -> ChallengeType.REORDER
            3 -> if (level % 4 == 0) ChallengeType.CLASSIFY else ChallengeType.REORDER
            4 -> if (level % 2 == 0) ChallengeType.CLASSIFY else ChallengeType.REORDER
            else -> {
                val rand = (1..10).random()
                when {
                    rand > 8 -> ChallengeType.SPEED_BURST
                    rand > 4 -> ChallengeType.CLASSIFY
                    else -> ChallengeType.REORDER
                }
            }
        }

        if (type == ChallengeType.CLASSIFY && allLines.size < 2) type = ChallengeType.REORDER

        when (type) {
            ChallengeType.REORDER -> generateReorderChallenge(stage)
            ChallengeType.CLASSIFY -> generateClassifyChallenge(stage)
            ChallengeType.SPEED_BURST -> generateSpeedBurstChallenge()
        }
        
        _state.update { it.copy(
            challengeStartedAt = SystemClock.elapsedRealtime(),
            stage = stage
        ) }
        
        if (timerJob == null) startTimer()

        if (type == ChallengeType.SPEED_BURST) startBurstTimer() else stopBurstTimer()
    }

    private fun generateReorderChallenge(stage: Int) {
        val line = allLines.random()
        val bounds = GeoBounds.from(line.geometry.ifEmpty { line.stations.map { it.position } })
        
        val count = when (stage) {
            1 -> (3..4).random()
            2 -> 5
            3 -> 6
            4 -> (6..7).random()
            else -> (7..8).random()
        }

        val isForward = if (stage == 1) true else (0..1).random() == 0
        // Stage 1: interchange hubs + terminus only (falls back to the full line if too few)
        val availableStations = (if (stage == 1) {
            line.stations.filterIndexed { i, s -> s.lines.size > 1 || i == 0 || i == line.stations.lastIndex }
        } else line.stations).let { if (it.size >= 3) it else line.stations }

        val sequenceRaw = if (availableStations.size <= count) {
            availableStations
        } else {
            val start = (0..(availableStations.size - count)).random()
            availableStations.subList(start, start + count)
        }
        val correctOrder = if (isForward) sequenceRaw else sequenceRaw.reversed()

        _state.update {
            it.copy(
                challengeType = ChallengeType.REORDER,
                line = line,
                line2 = null,
                bounds = bounds,
                correctOrder = correctOrder,
                currentTiles = correctOrder.shuffled(),
                isForward = isForward,
                phase = GamePhase.Drawing
            )
        }
    }

    private fun generateClassifyChallenge(stage: Int) {
        val line1 = allLines.random()
        val intersectingLines = allLines.filter { other ->
            other.id != line1.id && other.stations.any { s -> line1.stations.any { s1 -> s1.id == s.id } }
        }
        val line2 = intersectingLines.randomOrNull() ?: allLines.filter { it.id != line1.id }.random()
        
        val totalCount = if (stage <= 4) (5..6).random() else (7..8).random()
        val maxHubs = if (stage <= 4) 1 else 3
        
        val stations1 = line1.stations.filter { s -> line2.stations.none { it.id == s.id } }.shuffled().take(totalCount / 2)
        val stations2 = line2.stations.filter { s -> line1.stations.none { it.id == s.id } }.shuffled().take(totalCount / 2)
        val hubs = line1.stations.filter { s1 -> line2.stations.any { s2 -> s2.id == s1.id } }.shuffled().take(maxHubs)
        
        val combinedStationsRaw = (stations1 + stations2 + hubs).distinctBy { it.id }.take(totalCount)
        
        val isForward = (0..1).random() == 0
        val correctOrder = combinedStationsRaw.sortedBy { s -> 
            val idx1 = line1.stations.indexOfFirst { it.id == s.id }.let { if (it == -1) 999 else it }
            val idx2 = line2.stations.indexOfFirst { it.id == s.id }.let { if (it == -1) 999 else it }
            minOf(idx1, idx2)
        }.let { if (isForward) it else it.reversed() }

        val correctSides = correctOrder.associate { station ->
            val in1 = line1.stations.any { it.id == station.id }
            val in2 = line2.stations.any { it.id == station.id }
            val side = when {
                in1 && in2 -> 0  // HUB -> Center
                in1 -> -1       // Line 1 -> Left
                else -> 1       // Line 2 -> Right
            }
            station.id to side
        }

        _state.update {
            it.copy(
                challengeType = ChallengeType.CLASSIFY,
                line = line1,
                line2 = line2,
                correctOrder = correctOrder,
                currentTiles = correctOrder.shuffled(),
                stationSides = correctOrder.associate { it.id to 0 },
                correctSides = correctSides,
                isForward = isForward,
                phase = GamePhase.Drawing
            )
        }
    }

    private fun generateSpeedBurstChallenge() {
        val line = allLines.random()
        val count = 3
        val start = (0..(line.stations.size - count)).random()
        val sequence = line.stations.subList(start, start + count)
        
        _state.update {
            it.copy(
                challengeType = ChallengeType.SPEED_BURST,
                line = line,
                line2 = null,
                correctOrder = sequence,
                currentTiles = sequence.shuffled(),
                isForward = true,
                phase = GamePhase.Drawing
            )
        }
    }

    private fun startBurstTimer() {
        burstTimerJob?.cancel()
        burstTimerJob = viewModelScope.launch {
            val limit = 6000L
            val start = SystemClock.elapsedRealtime()
            while (true) {
                val progress = (SystemClock.elapsedRealtime() - start).toFloat() / limit
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
        val penalty = 3000L
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
            var lastUpdate = SystemClock.elapsedRealtime()
            while (!finished && _state.value.timeLeftMs > 0) {
                delay(50)
                val now = SystemClock.elapsedRealtime()
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
        
        saveResult(GameResult(
            lineId = "SPRINT",
            mode = GameMode.STATION_SPRINT,
            difficulty = difficulty,
            score = finalState.score,
            stationOrderScore = accuracy,
            pathAccuracyScore = 0f,
            completionScore = 0f,
            speedBonusScore = 0f,
            durationMs = SystemClock.elapsedRealtime() - gameStartedAt,
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
            timeLeftMs = (it.timeLeftMs - SKIP_PENALTY_MS).coerceAtLeast(0),
            combo = 0
        ) }
        generateChallenge()
    }

    fun submit() {
        val currentState = _state.value
        if (finished || paused || currentState.showFeedback || currentState.phase !is GamePhase.Drawing) return

        _state.update { it.copy(totalSubmissions = it.totalSubmissions + 1) }
        
        val isVerticalCorrect = currentState.currentTiles == currentState.correctOrder
        val isHorizontalCorrect = if (currentState.challengeType == ChallengeType.CLASSIFY) {
            currentState.stationSides == currentState.correctSides
        } else true
        
        val isCorrect = isVerticalCorrect && isHorizontalCorrect
        
        if (isCorrect) {
            handleCorrectAnswer()
        } else {
            val stage = getStage(currentState.level)
            val basePenalty = 5_000L
            val timePenalty = basePenalty + (stage - 1) * 1000L
            
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
        val timeTakenMs = SystemClock.elapsedRealtime() - currentState.challengeStartedAt

        val newCombo = currentState.combo + 1
        val stage = getStage(currentState.level)

        val baseGain = when(difficulty) {
            Difficulty.EASY -> 12_000L - (stage - 1) * 1000L
            Difficulty.MEDIUM -> 10_000L - (stage - 1) * 1000L
            Difficulty.HARD -> 8_000L - (stage - 1) * 500L
        }.coerceAtLeast(4000L)

        val comboBonus = (newCombo * 1000L).coerceAtMost(5000L)
        val typeBonus = when(currentState.challengeType) {
            ChallengeType.CLASSIFY -> 5000L
            ChallengeType.SPEED_BURST -> 2000L
            else -> 0L
        }
        val timeGain = baseGain + comboBonus + typeBonus

        val basePoints = (500 + currentState.level * 50) * (if (currentState.challengeType == ChallengeType.CLASSIFY) 2 else 1)
        val speedFactor = (1.5f - (timeTakenMs / 20_000f)).coerceIn(1.0f, 1.5f)
        val comboFactor = 1f + (newCombo * 0.1f).coerceAtMost(1.0f)
        val pointsGain = (basePoints * speedFactor * comboFactor).toInt()

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

    private companion object {
        const val SKIP_PENALTY_MS = 4_000L
    }
}
