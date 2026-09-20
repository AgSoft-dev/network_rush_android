package com.triviamap.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.triviamap.data.local.dao.GameResultDao;
import com.triviamap.data.local.dao.GameResultDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TriviaMapDatabase_Impl extends TriviaMapDatabase {
  private volatile GameResultDao _gameResultDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `game_results` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `lineId` TEXT NOT NULL, `mode` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `score` INTEGER NOT NULL, `stationOrderScore` REAL NOT NULL, `pathAccuracyScore` REAL NOT NULL, `completionScore` REAL NOT NULL, `speedBonusScore` REAL NOT NULL, `durationMs` INTEGER NOT NULL, `timestampMs` INTEGER NOT NULL, `level` INTEGER NOT NULL, `maxCombo` INTEGER NOT NULL, `accuracy` REAL NOT NULL, `playerPathJson` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4c33be71d31239e3a56d01f48eddee0d')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `game_results`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsGameResults = new HashMap<String, TableInfo.Column>(15);
        _columnsGameResults.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("lineId", new TableInfo.Column("lineId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("mode", new TableInfo.Column("mode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("difficulty", new TableInfo.Column("difficulty", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("score", new TableInfo.Column("score", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("stationOrderScore", new TableInfo.Column("stationOrderScore", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("pathAccuracyScore", new TableInfo.Column("pathAccuracyScore", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("completionScore", new TableInfo.Column("completionScore", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("speedBonusScore", new TableInfo.Column("speedBonusScore", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("durationMs", new TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("timestampMs", new TableInfo.Column("timestampMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("level", new TableInfo.Column("level", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("maxCombo", new TableInfo.Column("maxCombo", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("accuracy", new TableInfo.Column("accuracy", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameResults.put("playerPathJson", new TableInfo.Column("playerPathJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGameResults = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGameResults = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGameResults = new TableInfo("game_results", _columnsGameResults, _foreignKeysGameResults, _indicesGameResults);
        final TableInfo _existingGameResults = TableInfo.read(db, "game_results");
        if (!_infoGameResults.equals(_existingGameResults)) {
          return new RoomOpenHelper.ValidationResult(false, "game_results(com.triviamap.data.local.entity.GameResultEntity).\n"
                  + " Expected:\n" + _infoGameResults + "\n"
                  + " Found:\n" + _existingGameResults);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "4c33be71d31239e3a56d01f48eddee0d", "4dd0788937fbd0da86a69e1c49c1449c");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "game_results");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `game_results`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(GameResultDao.class, GameResultDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public GameResultDao gameResultDao() {
    if (_gameResultDao != null) {
      return _gameResultDao;
    } else {
      synchronized(this) {
        if(_gameResultDao == null) {
          _gameResultDao = new GameResultDao_Impl(this);
        }
        return _gameResultDao;
      }
    }
  }
}
