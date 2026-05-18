package com.triviamap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.triviamap.domain.model.Difficulty

@Entity(tableName = "game_results")
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val lineId: String,
    val difficulty: String,          // Difficulty.name()
    val score: Int,
    val stationOrderScore: Float,
    val pathAccuracyScore: Float,
    val completionScore: Float,
    val speedBonusScore: Float,
    val durationMs: Long,
    val timestampMs: Long,
    val playerPathJson: String       // JSON-encoded List<GeoPoint>
)
