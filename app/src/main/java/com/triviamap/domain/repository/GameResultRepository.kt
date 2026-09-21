package com.triviamap.domain.repository

import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GameMode
import com.triviamap.domain.model.GameResult
import kotlinx.coroutines.flow.Flow

interface GameResultRepository {
    fun getResultsForLine(lineId: String): Flow<List<GameResult>>
    /** Best result per (mode, difficulty). */
    fun getBestScores(): Flow<List<GameResult>>
    /** Most recent Sprint results (normal + daily), newest first. */
    fun recentSprintResults(limit: Int): Flow<List<GameResult>>
    /** First daily-challenge result of the given day, if any. */
    fun dailyResult(epochDay: Long): Flow<GameResult?>
    suspend fun getHighScore(mode: GameMode, difficulty: Difficulty): Int
    suspend fun saveResult(result: GameResult)
    suspend fun clearResults()
}
