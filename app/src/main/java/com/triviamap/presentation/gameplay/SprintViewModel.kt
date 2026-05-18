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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SprintUiState(
    val phase: GamePhase = GamePhase.Loading,
    /** The original correct sequence of stations */
    val correctOrder: List<Station> = emptyList(),
    /** The current order of tiles as manipulated by the player */
    val currentTiles: List<Station> = emptyList(),
    val line: TramLine? = null,
    val bounds: GeoBounds? = null,
    val score: Int = 0,
    val elapsedMs: Long = 0,
    val difficulty: Difficulty = Difficulty.MEDIUM
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

    private val _state = MutableStateFlow(SprintUiState(difficulty = difficulty))
    val state: StateFlow<SprintUiState> = _state.asStateFlow()

    private var startTimeMs = 0L

    init {
        generateChallenge()
    }

    private fun generateChallenge() = viewModelScope.launch {
        getAllLines().collectLatest { lines ->
            if (lines.isEmpty()) return@collectLatest
            
            // Pick a random line to focus on
            val line = lines.random()
            val bounds = GeoBounds.from(line.geometry)
            
            val sequence = when (difficulty) {
                Difficulty.EASY -> {
                    // 5 sequential stations
                    val start = (0..(line.stations.size - 5).coerceAtLeast(0)).random()
                    line.stations.subList(start, (start + 5).coerceAtMost(line.stations.size))
                }
                Difficulty.MEDIUM -> {
                    // 7 random stations from the line, in their correct relative order
                    line.stations.shuffled().take(7).sortedBy { s -> line.stations.indexOfFirst { it.id == s.id } }
                }
                Difficulty.HARD -> {
                    // 10 random stations from the line, in their correct relative order
                    line.stations.shuffled().take(10).sortedBy { s -> line.stations.indexOfFirst { it.id == s.id } }
                }
            }

            _state.update {
                it.copy(
                    line = line,
                    bounds = bounds,
                    correctOrder = sequence,
                    currentTiles = sequence.shuffled(),
                    phase = GamePhase.Drawing
                )
            }
            startTimeMs = System.currentTimeMillis()
            startTimer()
        }
    }

    private fun startTimer() = viewModelScope.launch {
        while (_state.value.phase is GamePhase.Drawing) {
            delay(100)
            _state.update { it.copy(elapsedMs = System.currentTimeMillis() - startTimeMs) }
        }
    }

    fun moveTile(fromIndex: Int, toIndex: Int) {
        if (_state.value.phase !is GamePhase.Drawing) return
        _state.update { state ->
            val list = state.currentTiles.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            state.copy(currentTiles = list)
        }
    }

    fun submit() {
        val currentState = _state.value
        if (currentState.phase !is GamePhase.Drawing) return
        
        _state.update { it.copy(phase = GamePhase.Validating) }
        
        viewModelScope.launch {
            val isCorrect = currentState.currentTiles == currentState.correctOrder
            val duration = currentState.elapsedMs
            
            // Scoring
            val baseScore = if (isCorrect) 800 else 0
            val timeBonus = if (isCorrect) {
                // Perfect score if under 10 seconds, then decreases
                (200 - (duration / 1000).toInt() * 10).coerceIn(0, 200)
            } else 0
            val finalScore = baseScore + timeBonus

            saveResult(GameResult(
                lineId = currentState.line?.id ?: "SPRINT",
                mode = GameMode.STATION_SPRINT,
                difficulty = difficulty,
                score = finalScore,
                stationOrderScore = if (isCorrect) 1f else 0f,
                pathAccuracyScore = 0f,
                completionScore = if (isCorrect) 1f else 0f,
                speedBonusScore = timeBonus / 200f,
                durationMs = duration
            ))

            delay(800)
            _state.update { it.copy(score = finalScore, phase = GamePhase.Finished(ScoreBreakdown(if (isCorrect) 1f else 0f, 1f, timeBonus / 200f, finalScore))) }
        }
    }
}
