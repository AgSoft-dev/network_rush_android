package com.triviamap.presentation.gameplay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GeoPoint
import com.triviamap.domain.model.Station
import com.triviamap.domain.model.TramLine
import com.triviamap.domain.usecase.GetAllLinesUseCase
import com.triviamap.domain.usecase.SaveGameResultUseCase
import com.triviamap.domain.model.GameResult
import com.triviamap.util.GeoBounds
import com.triviamap.util.ScoreBreakdown
import com.triviamap.util.ScoringEngine
import com.triviamap.util.findNearestStation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GamePhase {
    object Loading   : GamePhase()
    object Drawing   : GamePhase()
    object Validating: GamePhase()
    data class Finished(val breakdown: ScoreBreakdown) : GamePhase()
}

data class GameplayUiState(
    val allLines: List<TramLine> = emptyList(),
    val activeLineId: String? = null,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val phase: GamePhase = GamePhase.Loading,
    /** Map of LineID -> List of GeoPoints drawn for that line */
    val playerPaths: Map<String, List<GeoPoint>> = emptyMap(),
    /** Map of LineID -> List of Stations visited for that line */
    val visitedStations: Map<String, List<Station>> = emptyMap(),
    val elapsedMs: Long = 0L,
    val bounds: GeoBounds? = null,
    /** Station index currently being snapped in the active line */
    val snapIndex: Int = -1
) {
    val activeLine: TramLine? get() = allLines.find { it.id == activeLineId }
}

@HiltViewModel
class GameplayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllLines: GetAllLinesUseCase,
    private val saveResult: SaveGameResultUseCase
) : ViewModel() {

    private val difficulty: Difficulty = Difficulty.valueOf(
        checkNotNull(savedStateHandle["difficulty"])
    )

    private val _state = MutableStateFlow(GameplayUiState(difficulty = difficulty))
    val state: StateFlow<GameplayUiState> = _state.asStateFlow()

    private var startTimeMs = 0L
    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        loadAllLines()
    }

    private fun loadAllLines() = viewModelScope.launch {
        getAllLines().collectLatest { lines ->
            if (lines.isEmpty()) return@collectLatest
            
            val allPoints = lines.flatMap { it.geometry }
            val bounds = GeoBounds.from(allPoints)
            
            _state.update {
                it.copy(
                    allLines = lines,
                    activeLineId = lines.firstOrNull()?.id,
                    bounds = bounds,
                    phase = GamePhase.Drawing
                )
            }
            if (timerJob == null) startTimer()
        }
    }

    private fun startTimer() {
        startTimeMs = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(500)
                _state.update { it.copy(elapsedMs = System.currentTimeMillis() - startTimeMs) }
            }
        }
    }

    fun setActiveLine(lineId: String) {
        _state.update { it.copy(activeLineId = lineId, snapIndex = -1) }
    }

    /** Called on every touch move with a new point. */
    fun onDraw(point: GeoPoint) {
        val activeId = _state.value.activeLineId ?: return
        val activeLine = _state.value.activeLine ?: return
        if (_state.value.phase !is GamePhase.Drawing) return

        // Station snap detection within the ACTIVE line
        val snapIdx = findNearestStation(point, activeLine.stations, radius = 30.0)
        
        _state.update { state ->
            val currentPath = state.playerPaths[activeId] ?: emptyList()
            val newPaths = state.playerPaths + (activeId to (currentPath + point))
            
            val currentVisited = state.visitedStations[activeId] ?: emptyList()
            val newVisited = if (snapIdx >= 0) {
                val station = activeLine.stations[snapIdx]
                if (currentVisited.none { it.id == station.id }) {
                    state.visitedStations + (activeId to (currentVisited + station))
                } else state.visitedStations
            } else state.visitedStations

            state.copy(
                playerPaths = newPaths,
                visitedStations = newVisited,
                snapIndex = snapIdx
            )
        }
    }

    fun onDrawEnd() {
        if (_state.value.phase !is GamePhase.Drawing) return
    }

    /** Player presses Validate. */
    fun validate() {
        val state = _state.value
        timerJob?.cancel()
        _state.update { it.copy(phase = GamePhase.Validating) }

        viewModelScope.launch {
            val elapsed = System.currentTimeMillis() - startTimeMs
            
            // Compute combined score across all lines
            val breakdown = ScoringEngine.computeGlobal(
                playerPaths      = state.playerPaths,
                allReferenceLines = state.allLines,
                visitedStations  = state.visitedStations,
                durationMs       = elapsed,
                difficulty       = state.difficulty
            )
            
            saveResult(
                GameResult(
                    lineId              = "ALL",
                    difficulty          = state.difficulty,
                    score               = breakdown.total,
                    stationOrderScore   = breakdown.stationOrder,
                    pathAccuracyScore   = 0f, // Removed accuracy scoring
                    completionScore     = breakdown.completion,
                    speedBonusScore     = breakdown.speedBonus,
                    durationMs          = elapsed,
                    playerPath          = emptyList()
                )
            )
            delay(400)
            _state.update { it.copy(phase = GamePhase.Finished(breakdown)) }
        }
    }

    fun clearDrawing() {
        val activeId = _state.value.activeLineId ?: return
        _state.update {
            it.copy(
                playerPaths = it.playerPaths + (activeId to emptyList()),
                visitedStations = it.visitedStations + (activeId to emptyList()),
                snapIndex = -1
            )
        }
    }
}
