package com.triviamap.data.repository

import com.google.gson.Gson
import com.triviamap.data.local.dao.GameResultDao
import com.triviamap.data.local.entity.GameResultEntity
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GameMode
import com.triviamap.domain.model.GameResult
import com.triviamap.domain.model.GeoPoint
import com.triviamap.domain.repository.GameResultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameResultRepositoryImpl @Inject constructor(
    private val dao: GameResultDao
) : GameResultRepository {

    private val gson = Gson()

    override fun getResultsForLine(lineId: String): Flow<List<GameResult>> =
        dao.getResultsForLine(lineId).map { list -> list.map { it.toDomain(gson) } }

    override fun getBestScores(): Flow<List<GameResult>> =
        dao.getBestScores().map { list -> list.map { it.toDomain(gson) } }

    override suspend fun getHighScore(mode: GameMode, difficulty: Difficulty): Int =
        dao.getHighScore(mode.name, difficulty.name) ?: 0

    override suspend fun saveResult(result: GameResult) {
        dao.insert(result.toEntity(gson))
    }

    override suspend fun clearResults() = dao.clearAll()

    private fun GameResultEntity.toDomain(gson: Gson): GameResult {
        val modeEnum = try { GameMode.valueOf(mode) } catch (e: Exception) { GameMode.TRACE_NETWORK }
        val difficultyEnum = try { Difficulty.valueOf(difficulty) } catch (e: Exception) { Difficulty.MEDIUM }
        val path = try {
            if (playerPathJson.isBlank()) emptyList()
            else gson.fromJson(playerPathJson, Array<GeoPointDto>::class.java)?.map { GeoPoint(it.x, it.y) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return GameResult(
            id = id,
            lineId = lineId,
            mode = modeEnum,
            difficulty = difficultyEnum,
            score = score,
            stationOrderScore = stationOrderScore,
            pathAccuracyScore = pathAccuracyScore,
            completionScore = completionScore,
            speedBonusScore = speedBonusScore,
            durationMs = durationMs,
            timestampMs = timestampMs,
            playerPath = path,
            level = level,
            maxCombo = maxCombo,
            accuracy = accuracy
        )
    }

    private fun GameResult.toEntity(gson: Gson) = GameResultEntity(
        id = id,
        lineId = lineId,
        mode = mode.name,
        difficulty = difficulty.name,
        score = score,
        stationOrderScore = stationOrderScore,
        pathAccuracyScore = pathAccuracyScore,
        completionScore = completionScore,
        speedBonusScore = speedBonusScore,
        durationMs = durationMs,
        timestampMs = timestampMs,
        level = level,
        maxCombo = maxCombo,
        accuracy = accuracy,
        playerPathJson = gson.toJson(playerPath.map { GeoPointDto(it.x, it.y) })
    )

    private data class GeoPointDto(val x: Double, val y: Double)
}
