package com.triviamap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "station_stats")
data class StationStatEntity(
    @PrimaryKey val stationId: String,
    val attempts: Int,
    val correct: Int,
    val streak: Int,
    val lastSeenMs: Long
)
