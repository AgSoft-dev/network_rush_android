package com.triviamap.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * v1 was never exported (no 1.json), so v1 databases are created with raw SQL mirroring the
 * POC GameResultEntity (commit 7d1bb73). Optionally with the `mode` column, which some dev
 * installs already had before the 1->2 migration ran.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TriviaMapDatabase::class.java
    )

    @After
    fun cleanup() {
        ctx.deleteDatabase(dbName)
    }

    private fun createV1(withMode: Boolean, fill: (SupportSQLiteDatabase) -> Unit) {
        ctx.deleteDatabase(dbName)
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                val modeCol = if (withMode) "`mode` TEXT NOT NULL DEFAULT 'TRACE_NETWORK', " else ""
                db.execSQL(
                    "CREATE TABLE `game_results` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`lineId` TEXT NOT NULL, $modeCol`difficulty` TEXT NOT NULL, `score` INTEGER NOT NULL, " +
                        "`stationOrderScore` REAL NOT NULL, `pathAccuracyScore` REAL NOT NULL, " +
                        "`completionScore` REAL NOT NULL, `speedBonusScore` REAL NOT NULL, " +
                        "`durationMs` INTEGER NOT NULL, `timestampMs` INTEGER NOT NULL, `playerPathJson` TEXT NOT NULL)"
                )
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val h = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(ctx).name(dbName).callback(callback).build()
        )
        fill(h.writableDatabase)
        h.close()
    }

    private fun insertV1(
        db: SupportSQLiteDatabase, id: Long, line: String, mode: String?, diff: String,
        score: Int, order: Double, path: Double, completion: Double, speed: Double, ts: Long
    ) {
        val cols = if (mode != null) "id,lineId,mode,difficulty" else "id,lineId,difficulty"
        val vals = if (mode != null) "$id,'$line','$mode','$diff'" else "$id,'$line','$diff'"
        db.execSQL(
            "INSERT INTO game_results ($cols,score,stationOrderScore,pathAccuracyScore,completionScore," +
                "speedBonusScore,durationMs,timestampMs,playerPathJson) VALUES ($vals,$score,$order,$path," +
                "$completion,$speed,0,$ts,'[]')"
        )
    }

    private fun seed(db: SupportSQLiteDatabase, withMode: Boolean) {
        // Sprint rows: completionScore = level, speedBonusScore = maxCombo, stationOrderScore = accuracy
        insertV1(db, 1, "SPRINT", if (withMode) "STATION_SPRINT" else null, "NORMAL", 1200, 0.85, 0.0, 7.0, 12.0, 1000)
        insertV1(db, 2, "SPRINT", if (withMode) "STATION_SPRINT" else null, "NORMAL", 3400, 0.95, 0.0, 12.0, 30.0, 2000)
        // Trace rows keep their scores untouched
        insertV1(db, 3, "A", if (withMode) "TRACE_NETWORK" else null, "EASY", 800, 0.6, 0.7, 0.9, 0.3, 3000)
        insertV1(db, 4, "B", if (withMode) "TRACE_NETWORK" else null, "HARD", 500, 0.4, 0.5, 0.8, 0.1, 4000)
    }

    private fun assertV2Data(db: SupportSQLiteDatabase) {
        db.query(
            "SELECT id, mode, level, maxCombo, accuracy, completionScore, speedBonusScore, score " +
                "FROM game_results ORDER BY id"
        ).use { c ->
            assertEquals(4, c.count)
            c.moveToNext()
            assertEquals(1L, c.getLong(0)); assertEquals("STATION_SPRINT", c.getString(1))
            assertEquals(7, c.getInt(2)); assertEquals(12, c.getInt(3)); assertEquals(0.85, c.getDouble(4), 1e-6)
            assertEquals(1200, c.getInt(7))
            c.moveToNext()
            assertEquals("STATION_SPRINT", c.getString(1))
            assertEquals(12, c.getInt(2)); assertEquals(30, c.getInt(3)); assertEquals(0.95, c.getDouble(4), 1e-6)
            c.moveToNext()
            assertEquals("TRACE_NETWORK", c.getString(1))
            assertEquals(0, c.getInt(2)); assertEquals(0, c.getInt(3)); assertEquals(0.0, c.getDouble(4), 0.0)
            assertEquals(0.9, c.getDouble(5), 1e-6); assertEquals(0.3, c.getDouble(6), 1e-6)
            c.moveToNext()
            assertEquals("TRACE_NETWORK", c.getString(1)); assertEquals(500, c.getInt(7))
        }
    }

    @Test
    fun migrate1To2_backfillsSprintColumns() {
        createV1(withMode = false) { seed(it, false) }
        helper.runMigrationsAndValidate(dbName, 2, true, TriviaMapDatabase.MIGRATION_1_2).use(::assertV2Data)
    }

    @Test
    fun migrate1To2_whenModeColumnAlreadyExists() {
        createV1(withMode = true) { seed(it, true) }
        helper.runMigrationsAndValidate(dbName, 2, true, TriviaMapDatabase.MIGRATION_1_2).use(::assertV2Data)
    }

    @Test
    fun migrate2To3_preservesRowsAndCreatesNewSchema() {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                "INSERT INTO game_results (id,lineId,mode,difficulty,score,stationOrderScore,pathAccuracyScore," +
                    "completionScore,speedBonusScore,durationMs,timestampMs,level,maxCombo,accuracy,playerPathJson) " +
                    "VALUES (1,'SPRINT','STATION_SPRINT','NORMAL',1500,0.9,0,0,0,0,1000,5,8,0.9,'[]')," +
                    "(2,'A','TRACE_NETWORK','EASY',700,0.5,0.5,0.5,0.5,9000,2000,0,0,0,'[[1,2]]')"
            )
        }
        helper.runMigrationsAndValidate(dbName, 3, true, TriviaMapDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT id, score, level, maxCombo, playerPathJson, answerLog FROM game_results ORDER BY id").use { c ->
                assertEquals(2, c.count)
                c.moveToNext(); assertEquals(1500, c.getInt(1)); assertEquals(5, c.getInt(2)); assertEquals(8, c.getInt(3))
                assertEquals("", c.getString(5))
                c.moveToNext(); assertEquals("[[1,2]]", c.getString(4))
            }
            db.execSQL("INSERT INTO station_stats VALUES ('S1', 3, 2, 1, 5000)")
            db.query("SELECT COUNT(*) FROM station_stats").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        }
    }

    @Test
    fun migrate1To3_chained() {
        createV1(withMode = false) { seed(it, false) }
        helper.runMigrationsAndValidate(
            dbName, 3, true, TriviaMapDatabase.MIGRATION_1_2, TriviaMapDatabase.MIGRATION_2_3
        ).use { db ->
            db.query("SELECT COUNT(*) FROM game_results WHERE answerLog = ''").use { c ->
                c.moveToFirst(); assertEquals(4, c.getInt(0))
            }
            db.query("SELECT level, maxCombo FROM game_results WHERE id = 2").use { c ->
                c.moveToFirst(); assertEquals(12, c.getInt(0)); assertEquals(30, c.getInt(1))
            }
        }
    }

    @Test
    fun finalDatabase_opensMigratedFileAndDaosRead() = runBlocking {
        createV1(withMode = false) { seed(it, false) }
        val db = Room.databaseBuilder(ctx, TriviaMapDatabase::class.java, dbName)
            .addMigrations(TriviaMapDatabase.MIGRATION_1_2, TriviaMapDatabase.MIGRATION_2_3)
            .build()
        try {
            val dao = db.gameResultDao()
            val sprint = dao.recent(listOf("STATION_SPRINT"), 10).first()
            assertEquals(2, sprint.size)
            val latest = sprint.first()
            assertEquals(12, latest.level); assertEquals(30, latest.maxCombo)
            assertEquals(0.95f, latest.accuracy, 1e-6f); assertEquals("", latest.answerLog)
            assertEquals(3400, dao.getHighScore("STATION_SPRINT", "NORMAL"))
            assertEquals(800, dao.getHighScore("TRACE_NETWORK", "EASY"))
            // one best row per (mode, difficulty): SPRINT/NORMAL, TRACE/EASY, TRACE/HARD
            assertEquals(3, dao.getBestScores().first().size)
            assertTrue(db.stationStatsDao().observeAll().first().isEmpty())
        } finally {
            db.close()
        }
    }
}
