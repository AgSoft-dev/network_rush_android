package com.triviamap.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.triviamap.data.local.entity.GameResultEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GameResultDao_Impl implements GameResultDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GameResultEntity> __insertionAdapterOfGameResultEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public GameResultDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGameResultEntity = new EntityInsertionAdapter<GameResultEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `game_results` (`id`,`lineId`,`mode`,`difficulty`,`score`,`stationOrderScore`,`pathAccuracyScore`,`completionScore`,`speedBonusScore`,`durationMs`,`timestampMs`,`level`,`maxCombo`,`accuracy`,`playerPathJson`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GameResultEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getLineId());
        statement.bindString(3, entity.getMode());
        statement.bindString(4, entity.getDifficulty());
        statement.bindLong(5, entity.getScore());
        statement.bindDouble(6, entity.getStationOrderScore());
        statement.bindDouble(7, entity.getPathAccuracyScore());
        statement.bindDouble(8, entity.getCompletionScore());
        statement.bindDouble(9, entity.getSpeedBonusScore());
        statement.bindLong(10, entity.getDurationMs());
        statement.bindLong(11, entity.getTimestampMs());
        statement.bindLong(12, entity.getLevel());
        statement.bindLong(13, entity.getMaxCombo());
        statement.bindDouble(14, entity.getAccuracy());
        statement.bindString(15, entity.getPlayerPathJson());
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM game_results";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final GameResultEntity result,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGameResultEntity.insert(result);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GameResultEntity>> getResultsForLine(final String lineId) {
    final String _sql = "SELECT * FROM game_results WHERE lineId = ? ORDER BY timestampMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, lineId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"game_results"}, new Callable<List<GameResultEntity>>() {
      @Override
      @NonNull
      public List<GameResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLineId = CursorUtil.getColumnIndexOrThrow(_cursor, "lineId");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfStationOrderScore = CursorUtil.getColumnIndexOrThrow(_cursor, "stationOrderScore");
          final int _cursorIndexOfPathAccuracyScore = CursorUtil.getColumnIndexOrThrow(_cursor, "pathAccuracyScore");
          final int _cursorIndexOfCompletionScore = CursorUtil.getColumnIndexOrThrow(_cursor, "completionScore");
          final int _cursorIndexOfSpeedBonusScore = CursorUtil.getColumnIndexOrThrow(_cursor, "speedBonusScore");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfTimestampMs = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMs");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfMaxCombo = CursorUtil.getColumnIndexOrThrow(_cursor, "maxCombo");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfPlayerPathJson = CursorUtil.getColumnIndexOrThrow(_cursor, "playerPathJson");
          final List<GameResultEntity> _result = new ArrayList<GameResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GameResultEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLineId;
            _tmpLineId = _cursor.getString(_cursorIndexOfLineId);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final String _tmpDifficulty;
            _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final float _tmpStationOrderScore;
            _tmpStationOrderScore = _cursor.getFloat(_cursorIndexOfStationOrderScore);
            final float _tmpPathAccuracyScore;
            _tmpPathAccuracyScore = _cursor.getFloat(_cursorIndexOfPathAccuracyScore);
            final float _tmpCompletionScore;
            _tmpCompletionScore = _cursor.getFloat(_cursorIndexOfCompletionScore);
            final float _tmpSpeedBonusScore;
            _tmpSpeedBonusScore = _cursor.getFloat(_cursorIndexOfSpeedBonusScore);
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final long _tmpTimestampMs;
            _tmpTimestampMs = _cursor.getLong(_cursorIndexOfTimestampMs);
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpMaxCombo;
            _tmpMaxCombo = _cursor.getInt(_cursorIndexOfMaxCombo);
            final float _tmpAccuracy;
            _tmpAccuracy = _cursor.getFloat(_cursorIndexOfAccuracy);
            final String _tmpPlayerPathJson;
            _tmpPlayerPathJson = _cursor.getString(_cursorIndexOfPlayerPathJson);
            _item = new GameResultEntity(_tmpId,_tmpLineId,_tmpMode,_tmpDifficulty,_tmpScore,_tmpStationOrderScore,_tmpPathAccuracyScore,_tmpCompletionScore,_tmpSpeedBonusScore,_tmpDurationMs,_tmpTimestampMs,_tmpLevel,_tmpMaxCombo,_tmpAccuracy,_tmpPlayerPathJson);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<GameResultEntity>> getBestScores() {
    final String _sql = "\n"
            + "        SELECT r.* FROM game_results r\n"
            + "        JOIN (\n"
            + "            SELECT mode, difficulty, MAX(score) AS best\n"
            + "            FROM game_results\n"
            + "            GROUP BY mode, difficulty\n"
            + "        ) m ON r.mode = m.mode AND r.difficulty = m.difficulty AND r.score = m.best\n"
            + "        GROUP BY r.mode, r.difficulty\n"
            + "        ORDER BY r.score DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"game_results"}, new Callable<List<GameResultEntity>>() {
      @Override
      @NonNull
      public List<GameResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLineId = CursorUtil.getColumnIndexOrThrow(_cursor, "lineId");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfStationOrderScore = CursorUtil.getColumnIndexOrThrow(_cursor, "stationOrderScore");
          final int _cursorIndexOfPathAccuracyScore = CursorUtil.getColumnIndexOrThrow(_cursor, "pathAccuracyScore");
          final int _cursorIndexOfCompletionScore = CursorUtil.getColumnIndexOrThrow(_cursor, "completionScore");
          final int _cursorIndexOfSpeedBonusScore = CursorUtil.getColumnIndexOrThrow(_cursor, "speedBonusScore");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfTimestampMs = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMs");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfMaxCombo = CursorUtil.getColumnIndexOrThrow(_cursor, "maxCombo");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfPlayerPathJson = CursorUtil.getColumnIndexOrThrow(_cursor, "playerPathJson");
          final List<GameResultEntity> _result = new ArrayList<GameResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GameResultEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpLineId;
            _tmpLineId = _cursor.getString(_cursorIndexOfLineId);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final String _tmpDifficulty;
            _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final float _tmpStationOrderScore;
            _tmpStationOrderScore = _cursor.getFloat(_cursorIndexOfStationOrderScore);
            final float _tmpPathAccuracyScore;
            _tmpPathAccuracyScore = _cursor.getFloat(_cursorIndexOfPathAccuracyScore);
            final float _tmpCompletionScore;
            _tmpCompletionScore = _cursor.getFloat(_cursorIndexOfCompletionScore);
            final float _tmpSpeedBonusScore;
            _tmpSpeedBonusScore = _cursor.getFloat(_cursorIndexOfSpeedBonusScore);
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final long _tmpTimestampMs;
            _tmpTimestampMs = _cursor.getLong(_cursorIndexOfTimestampMs);
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpMaxCombo;
            _tmpMaxCombo = _cursor.getInt(_cursorIndexOfMaxCombo);
            final float _tmpAccuracy;
            _tmpAccuracy = _cursor.getFloat(_cursorIndexOfAccuracy);
            final String _tmpPlayerPathJson;
            _tmpPlayerPathJson = _cursor.getString(_cursorIndexOfPlayerPathJson);
            _item = new GameResultEntity(_tmpId,_tmpLineId,_tmpMode,_tmpDifficulty,_tmpScore,_tmpStationOrderScore,_tmpPathAccuracyScore,_tmpCompletionScore,_tmpSpeedBonusScore,_tmpDurationMs,_tmpTimestampMs,_tmpLevel,_tmpMaxCombo,_tmpAccuracy,_tmpPlayerPathJson);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getHighScore(final String mode, final String difficulty,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT MAX(score) FROM game_results WHERE mode = ? AND difficulty = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, mode);
    _argIndex = 2;
    _statement.bindString(_argIndex, difficulty);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
