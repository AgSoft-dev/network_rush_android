package com.triviamap.data.repository

import com.triviamap.data.local.dao.StationStatsDao
import com.triviamap.data.local.entity.StationStatEntity
import com.triviamap.domain.progress.StationStat
import com.triviamap.domain.repository.StationStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationStatsRepositoryImpl @Inject constructor(
    private val dao: StationStatsDao,
    private val clock: Clock
) : StationStatsRepository {

    private val mutex = Mutex()

    override val stats: Flow<Map<String, StationStat>> =
        dao.observeAll().map { list -> list.associate { it.stationId to it.toDomain() } }

    override suspend fun snapshot(): Map<String, StationStat> = stats.first()

    override suspend fun record(outcomes: Map<String, Boolean>) {
        if (outcomes.isEmpty()) return
        mutex.withLock {
            val now = clock.millis()
            val current = dao.getByIds(outcomes.keys.toList()).associateBy { it.stationId }
            dao.upsert(outcomes.map { (id, ok) ->
                val before = current[id]?.toDomain() ?: StationStat(id)
                before.record(ok, now).toEntity()
            })
        }
    }

    private fun StationStatEntity.toDomain() = StationStat(stationId, attempts, correct, streak, lastSeenMs)
    private fun StationStat.toEntity() = StationStatEntity(stationId, attempts, correct, streak, lastSeenMs)
}
