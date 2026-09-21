package com.triviamap.domain.repository

import com.triviamap.domain.progress.StationStat
import kotlinx.coroutines.flow.Flow

interface StationStatsRepository {
    /** Stats by station id. */
    val stats: Flow<Map<String, StationStat>>

    /** Records, per station id, whether the tile was placed correctly in an answer. */
    suspend fun record(outcomes: Map<String, Boolean>)

    suspend fun snapshot(): Map<String, StationStat>
}
