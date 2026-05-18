package com.triviamap.domain.repository

import com.triviamap.domain.model.GameResult
import com.triviamap.domain.model.TramLine
import kotlinx.coroutines.flow.Flow

interface TramLineRepository {
    fun getAllLines(): Flow<List<TramLine>>
    suspend fun getLine(id: String): TramLine?
    suspend fun syncFromAssets()
}

interface GameResultRepository {
    fun getResultsForLine(lineId: String): Flow<List<GameResult>>
    fun getBestScores(): Flow<List<GameResult>>
    suspend fun saveResult(result: GameResult)
    suspend fun clearResults()
}
