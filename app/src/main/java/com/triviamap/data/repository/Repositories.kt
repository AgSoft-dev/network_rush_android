package com.triviamap.data.repository

import android.content.Context
import com.google.gson.Gson
import com.triviamap.data.local.dao.GameResultDao
import com.triviamap.data.local.entity.GameResultEntity
import com.triviamap.data.model.GeoJsonParser
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GameResult
import com.triviamap.domain.model.GeoPoint
import com.triviamap.domain.model.TramLine
import com.triviamap.domain.repository.GameResultRepository
import com.triviamap.domain.repository.TramLineRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TramLineRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TramLineRepository {

    private val _lines = MutableStateFlow<List<TramLine>>(emptyList())

    override fun getAllLines(): Flow<List<TramLine>> = _lines.asStateFlow()

    override suspend fun getLine(id: String): TramLine? =
        _lines.value.find { it.id == id }

    override suspend fun syncFromAssets() = withContext(Dispatchers.IO) {
        val stationsJson = context.assets.open("strasbourg_stations.json")
            .bufferedReader().readText()
        val tramLines = GeoJsonParser.parseData(stationsJson)
        _lines.value = tramLines
    }
}

@Singleton
class GameResultRepositoryImpl @Inject constructor(
    private val dao: GameResultDao
) : GameResultRepository {

    private val gson = Gson()

    override fun getResultsForLine(lineId: String): Flow<List<GameResult>> =
        dao.getResultsForLine(lineId).map { list -> list.map { it.toDomain(gson) } }

    override fun getBestScores(): Flow<List<GameResult>> =
        dao.getBestScores().map { list -> list.map { it.toDomain(gson) } }

    override suspend fun saveResult(result: GameResult) {
        dao.insert(result.toEntity(gson))
    }

    override suspend fun clearResults() = dao.clearAll()

    private fun GameResultEntity.toDomain(gson: Gson) = GameResult(
        id = id,
        lineId = lineId,
        difficulty = Difficulty.valueOf(difficulty),
        score = score,
        stationOrderScore = stationOrderScore,
        pathAccuracyScore = pathAccuracyScore,
        completionScore = completionScore,
        speedBonusScore = speedBonusScore,
        durationMs = durationMs,
        timestampMs = timestampMs,
        playerPath = gson.fromJson(playerPathJson, Array<GeoPointDto>::class.java)
            .map { GeoPoint(it.x, it.y) }
    )

    private fun GameResult.toEntity(gson: Gson) = GameResultEntity(
        id = id,
        lineId = lineId,
        difficulty = difficulty.name,
        score = score,
        stationOrderScore = stationOrderScore,
        pathAccuracyScore = pathAccuracyScore,
        completionScore = completionScore,
        speedBonusScore = speedBonusScore,
        durationMs = durationMs,
        timestampMs = timestampMs,
        playerPathJson = gson.toJson(playerPath.map { GeoPointDto(it.x, it.y) })
    )

    private data class GeoPointDto(val x: Double, val y: Double)
}
