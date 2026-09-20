package com.triviamap.domain.repository

import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GameMode
import com.triviamap.domain.model.GameResult
import com.triviamap.domain.model.TramLine
import kotlinx.coroutines.flow.Flow

sealed interface LinesState {
    object Loading : LinesState
    data class Loaded(val lines: List<TramLine>) : LinesState
    data class Error(val cause: Throwable) : LinesState
}

interface TramLineRepository {
    /** Emits [LinesState.Loading] until the first [load] completes. */
    val state: Flow<LinesState>
    fun getAllLines(): Flow<List<TramLine>>
    suspend fun getLine(id: String): TramLine?

    /** Loads (or reloads after an error) the bundled data. Safe to call concurrently. */
    suspend fun load()
}

interface GameResultRepository {
    fun getResultsForLine(lineId: String): Flow<List<GameResult>>
    /** Best result per (mode, difficulty). */
    fun getBestScores(): Flow<List<GameResult>>
    suspend fun getHighScore(mode: GameMode, difficulty: Difficulty): Int
    suspend fun saveResult(result: GameResult)
    suspend fun clearResults()
}
