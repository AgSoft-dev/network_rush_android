package com.triviamap.data.repository

import android.content.Context
import com.triviamap.data.model.GeoJsonParser
import com.triviamap.domain.model.TramLine
import com.triviamap.domain.repository.LinesState
import com.triviamap.domain.repository.TramLineRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TramLineRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TramLineRepository {

    private val _state = MutableStateFlow<LinesState>(LinesState.Loading)
    private val mutex = Mutex()

    override val state: Flow<LinesState> = _state.asStateFlow()

    override fun getAllLines(): Flow<List<TramLine>> =
        _state.map { (it as? LinesState.Loaded)?.lines ?: emptyList() }

    override suspend fun getLine(id: String): TramLine? =
        (_state.value as? LinesState.Loaded)?.lines?.find { it.id == id }

    override suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (_state.value is LinesState.Loaded) return@withLock
            _state.value = try {
                val json = context.assets.open("strasbourg_stations.json")
                    .bufferedReader().use { it.readText() }
                val lines = GeoJsonParser.parseData(json)
                if (lines.isEmpty()) LinesState.Error(IllegalStateException("No lines in dataset"))
                else LinesState.Loaded(lines)
            } catch (e: Exception) {
                LinesState.Error(e)
            }
        }
    }
}
