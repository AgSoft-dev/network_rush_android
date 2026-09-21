package com.triviamap.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.triviamap.data.local.dao.GameResultDao
import com.triviamap.data.local.dao.StationStatsDao
import com.triviamap.data.local.entity.GameResultEntity
import com.triviamap.data.local.entity.StationStatEntity

@Database(
    entities = [GameResultEntity::class, StationStatEntity::class],
    version = 3,
    exportSchema = true
)
abstract class TriviaMapDatabase : RoomDatabase() {
    abstract fun gameResultDao(): GameResultDao
    abstract fun stationStatsDao(): StationStatsDao

    companion object {
        /** Adds explicit Sprint columns (previously smuggled into other score fields). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Early v1 installs predate the `mode` column (v1 was never exported): add it if missing
                val columns = mutableSetOf<String>()
                db.query("PRAGMA table_info(game_results)").use { c ->
                    val nameIdx = c.getColumnIndexOrThrow("name")
                    while (c.moveToNext()) columns += c.getString(nameIdx)
                }
                if ("mode" !in columns) {
                    db.execSQL("ALTER TABLE game_results ADD COLUMN mode TEXT NOT NULL DEFAULT 'TRACE_NETWORK'")
                    db.execSQL("UPDATE game_results SET mode = 'STATION_SPRINT' WHERE lineId = 'SPRINT'")
                }
                db.execSQL("ALTER TABLE game_results ADD COLUMN level INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE game_results ADD COLUMN maxCombo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE game_results ADD COLUMN accuracy REAL NOT NULL DEFAULT 0")
                // v1 Sprint rows: completionScore = level, speedBonusScore = maxCombo, stationOrderScore = accuracy
                db.execSQL(
                    "UPDATE game_results SET level = CAST(completionScore AS INTEGER), " +
                        "maxCombo = CAST(speedBonusScore AS INTEGER), accuracy = stationOrderScore " +
                        "WHERE mode = 'STATION_SPRINT'"
                )
            }
        }

        /** Per-station knowledge stats + the answer recap kept with each result. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE game_results ADD COLUMN answerLog TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS station_stats (" +
                        "stationId TEXT NOT NULL, attempts INTEGER NOT NULL, correct INTEGER NOT NULL, " +
                        "streak INTEGER NOT NULL, lastSeenMs INTEGER NOT NULL, PRIMARY KEY(stationId))"
                )
            }
        }
    }
}
