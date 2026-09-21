package com.triviamap.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.triviamap.data.local.entity.StationStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StationStatsDao {
    @Query("SELECT * FROM station_stats")
    fun observeAll(): Flow<List<StationStatEntity>>

    @Query("SELECT * FROM station_stats WHERE stationId IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<StationStatEntity>

    @Upsert
    suspend fun upsert(stats: List<StationStatEntity>)

    @Query("DELETE FROM station_stats")
    suspend fun clear()
}
