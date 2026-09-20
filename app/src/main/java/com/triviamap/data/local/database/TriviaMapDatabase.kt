package com.triviamap.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.triviamap.data.local.dao.GameResultDao
import com.triviamap.data.local.entity.GameResultEntity

@Database(
    entities = [GameResultEntity::class],
    version = 2,
    exportSchema = true
)
abstract class TriviaMapDatabase : RoomDatabase() {
    abstract fun gameResultDao(): GameResultDao

    companion object {
        /** Adds explicit Sprint columns (previously smuggled into other score fields). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
    }
}
