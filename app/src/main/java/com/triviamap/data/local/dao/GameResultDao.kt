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
        SELECT r.* FROM game_results r
        JOIN (
            SELECT mode, difficulty, MAX(score) AS best
            FROM game_results
            GROUP BY mode, difficulty
        ) m ON r.mode = m.mode AND r.difficulty = m.difficulty AND r.score = m.best
        GROUP BY r.mode, r.difficulty
        ORDER BY r.score DESC
    """)
    fun getBestScores(): Flow<List<GameResultEntity>>

    @Query("SELECT * FROM game_results WHERE mode IN (:modes) ORDER BY timestampMs DESC LIMIT :limit")
    fun recent(modes: List<String>, limit: Int): Flow<List<GameResultEntity>>

    @Query("SELECT * FROM game_results WHERE mode = :mode AND timestampMs >= :fromMs AND timestampMs < :toMs ORDER BY timestampMs ASC LIMIT 1")
    fun firstBetween(mode: String, fromMs: Long, toMs: Long): Flow<GameResultEntity?>

    @Query("SELECT MAX(score) FROM game_results WHERE mode = :mode AND difficulty = :difficulty")
    suspend fun getHighScore(mode: String, difficulty: String): Int?

    @Query("DELETE FROM game_results")
    suspend fun clearAll()
}
