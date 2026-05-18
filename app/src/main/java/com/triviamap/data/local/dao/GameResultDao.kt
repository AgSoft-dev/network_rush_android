package com.triviamap.data.local.dao

import androidx.room.*
import com.triviamap.data.local.entity.GameResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: GameResultEntity)

    @Query("SELECT * FROM game_results WHERE lineId = :lineId ORDER BY timestampMs DESC")
    fun getResultsForLine(lineId: String): Flow<List<GameResultEntity>>

    @Query("""
        SELECT * FROM game_results 
        WHERE id IN (
            SELECT id FROM game_results 
            GROUP BY lineId 
            HAVING MAX(score)
        )
        ORDER BY score DESC
    """)
    fun getBestScores(): Flow<List<GameResultEntity>>

    @Query("DELETE FROM game_results")
    suspend fun clearAll()
}
