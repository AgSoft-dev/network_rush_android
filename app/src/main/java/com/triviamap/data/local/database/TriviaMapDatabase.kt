package com.triviamap.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.triviamap.data.local.dao.GameResultDao
import com.triviamap.data.local.entity.GameResultEntity

@Database(
    entities = [GameResultEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TriviaMapDatabase : RoomDatabase() {
    abstract fun gameResultDao(): GameResultDao
}
