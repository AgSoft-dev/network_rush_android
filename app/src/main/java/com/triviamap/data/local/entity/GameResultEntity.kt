package com.triviamap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_results")
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val lineId: String,
    val mode: String,                // GameMode.name()
    val difficulty: String,          // Difficulty.name()
    val score: Int,
    val stationOrderScore: Float,
    val pathAccuracyScore: Float,
    val completionScore: Float,
    val speedBonusScore: Float,
    val durationMs: Long,
    val timestampMs: Long,
    val level: Int = 0,
    val maxCombo: Int = 0,
    val accuracy: Float = 0f,
    val answerLog: String = "",
    val playerPathJson: String       // JSON-encoded List<GeoPoint>
)
