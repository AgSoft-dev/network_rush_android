package com.triviamap.presentation.gameplay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triviamap.domain.model.*
import com.triviamap.domain.usecase.GetAllLinesUseCase
import com.triviamap.domain.usecase.SaveGameResultUseCase
import com.triviamap.util.GeoBounds
import com.triviamap.util.ScoreBreakdown
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChallengeType {
    REORDER,     // Single line, reorder stations
    CLASSIFY     // Two lines, drag left/right/center AND reorder
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
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val combo: Int = 0,
    val isForward: Boolean = true,
    /** Visual feedback triggers */
    val lastTimeGain: Int = 0,
    val showFeedback: Boolean = false,
    val isCorrectFeedback: Boolean = true,
    val feedbackTrigger: Int = 0 
)

@HiltViewModel
class SprintViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllLines: GetAllLinesUseCase,
    private val saveResult: SaveGameResultUseCase
) : ViewModel() {

    private val difficulty: Difficulty = Difficulty.valueOf(
        checkNotNull(savedStateHandle["difficulty"])
    )

    private val _state = MutableStateFlow(SprintUiState(
        difficulty = difficulty,
        timeLeftMs = when(difficulty) {
            Difficulty.EASY -> 60_000L
            Difficulty.MEDIUM -> 45_000L
            Difficulty.HARD -> 30_000L
        }
    ))
    val state: StateFlow<SprintUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var allLines: List<TramLine> = emptyList()

    init {
        loadLines()
    }

    private fun loadLines() = viewModelScope.launch {
        getAllLines().collectLatest { lines ->
            if (lines.isEmpty()) return@collectLatest
            allLines = lines
            generateChallenge()
        }
    }

    private fun generateChallenge() {
        if (allLines.isEmpty()) return
        
        val level = _state.value.level
        // Force CLASSIFY in HARD mode for testing, otherwise every 3 levels
        val type = if (difficulty == Difficulty.HARD) ChallengeType.CLASSIFY else {
            if (level > 2 && level % 3 == 0) ChallengeType.CLASSIFY else ChallengeType.REORDER
        }

        if (type == ChallengeType.REORDER) {
            generateReorderChallenge()
        } else {
            generateClassifyChallenge()
        }
        
        if (timerJob == null) startTimer()
    }

    private fun generateReorderChallenge() {
        val line = allLines.random()
        val bounds = GeoBounds.from(line.geometry)
        val level = _state.value.level
        
        // Strictly limit to 8 tiles max to ensure fit without scrolling
        val count = when (difficulty) {
            Difficulty.EASY -> (3 + (level / 4)).coerceAtMost(6)
            Difficulty.MEDIUM -> (5 + (level / 3)).coerceAtMost(7)
            Difficulty.HARD -> (6 + (level / 2)).coerceAtMost(8)
        }

        val isForward = (0..1).random() == 0
        val sequenceRaw = if (line.stations.size <= count) {
            line.stations
        } else {
            val start = (0..(line.stations.size - count)).random()
            line.stations.subList(start, start + count)
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

    private fun generateClassifyChallenge() {
        val line1 = allLines.random()
        // Try to pick two lines that actually share stations (hubs)
        val intersectingLines = allLines.filter { other ->
            other.id != line1.id && other.stations.any { s -> line1.stations.any { s1 -> s1.id == s.id } }
        }
        val line2 = intersectingLines.randomOrNull() ?: allLines.filter { it.id != line1.id }.random()
        
        // Limit total stations to 8 to avoid scroll
        val stations1 = line1.stations.filter { s -> line2.stations.none { it.id == s.id } }.shuffled().take(3)
        val stations2 = line2.stations.filter { s -> line1.stations.none { it.id == s.id } }.shuffled().take(3)
        val hubs = line1.stations.filter { s1 -> line2.stations.any { s2 -> s2.id == s1.id } }.take(2)
        
        val combinedStationsRaw = (stations1 + stations2 + hubs).distinctBy { it.id }.take(8)
        
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

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            var lastUpdate = System.currentTimeMillis()
            while (_state.value.timeLeftMs > 0) {
                delay(50) 
                val now = System.currentTimeMillis()
                val delta = now - lastUpdate
                lastUpdate = now
                
                _state.update { 
                    val newTime = (it.timeLeftMs - delta).coerceAtLeast(0)
                    if (newTime <= 0) {
                        it.copy(timeLeftMs = 0, phase = GamePhase.Validating)
                    } else {
                        it.copy(timeLeftMs = newTime)
                    }
                }
            }
            endGame()
        }
    }

    private fun endGame() = viewModelScope.launch {
        val finalState = _state.value
        _state.update { it.copy(phase = GamePhase.Validating) }
        
        saveResult(GameResult(
            lineId = "SPRINT",
            mode = GameMode.STATION_SPRINT,
            difficulty = difficulty,
            score = finalState.score,
            stationOrderScore = 1f,
            pathAccuracyScore = 0f,
            completionScore = finalState.level.toFloat(),
            speedBonusScore = 0f,
            durationMs = 0L
        ))
        
        delay(800)
        _state.update { it.copy(phase = GamePhase.Finished(ScoreBreakdown(1f, 1f, 0f, finalState.score))) }
    }

    fun moveTile(fromIndex: Int, toIndex: Int) {
        if (_state.value.phase !is GamePhase.Drawing) return
        _state.update { state ->
            val list = state.currentTiles.toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices) return@update state
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            state.copy(currentTiles = list)
        }
    }

    fun setStationSide(stationId: String, side: Int) {
        if (_state.value.phase !is GamePhase.Drawing) return
        _state.update { it.copy(stationSides = it.stationSides + (stationId to side)) }
    }

    fun submit() {
        val currentState = _state.value
        if (currentState.phase !is GamePhase.Drawing) return
        
        val isVerticalCorrect = currentState.currentTiles == currentState.correctOrder
        val isHorizontalCorrect = if (currentState.challengeType == ChallengeType.CLASSIFY) {
            currentState.stationSides == currentState.correctSides
        } else true
        
        val isCorrect = isVerticalCorrect && isHorizontalCorrect
        
        if (isCorrect) {
            val newCombo = currentState.combo + 1
            val baseGain = when(difficulty) {
                Difficulty.EASY -> 12_000L
                Difficulty.MEDIUM -> 10_000L
                Difficulty.HARD -> 8_000L
            }
            val comboBonus = (newCombo * 1000L).coerceAtMost(5000L)
            val typeBonus = if (currentState.challengeType == ChallengeType.CLASSIFY) 5000L else 0L
            val timeGain = baseGain + comboBonus + typeBonus
            
            val pointsGain = (currentState.level * 100) * (if (currentState.challengeType == ChallengeType.CLASSIFY) 2 else 1) * (1 + newCombo / 5)
            
            viewModelScope.launch {
                _state.update { it.copy(
                    score = it.score + pointsGain,
                    timeLeftMs = it.timeLeftMs + timeGain,
                    lastTimeGain = (timeGain / 1000).toInt(),
                    level = it.level + 1,
                    combo = newCombo,
                    showFeedback = true,
                    isCorrectFeedback = true,
                    feedbackTrigger = it.feedbackTrigger + 1
                ) }
                delay(700)
                _state.update { it.copy(showFeedback = false) }
                generateChallenge()
            }
        } else {
            val timePenalty = 5_000L
            viewModelScope.launch {
                _state.update { it.copy(
                    timeLeftMs = (it.timeLeftMs - timePenalty).coerceAtLeast(0),
                    combo = 0,
                    showFeedback = true,
                    isCorrectFeedback = false,
                    feedbackTrigger = it.feedbackTrigger + 1
                ) }
                delay(700)
                _state.update { it.copy(showFeedback = false) }
            }
        }
    }
}
