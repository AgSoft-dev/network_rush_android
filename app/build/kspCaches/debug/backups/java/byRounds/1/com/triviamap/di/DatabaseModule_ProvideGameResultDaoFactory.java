package com.triviamap.di;

import com.triviamap.data.local.dao.GameResultDao;
import com.triviamap.data.local.database.TriviaMapDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DatabaseModule_ProvideGameResultDaoFactory implements Factory<GameResultDao> {
  private final Provider<TriviaMapDatabase> dbProvider;

  public DatabaseModule_ProvideGameResultDaoFactory(Provider<TriviaMapDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public GameResultDao get() {
    return provideGameResultDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideGameResultDaoFactory create(
      Provider<TriviaMapDatabase> dbProvider) {
    return new DatabaseModule_ProvideGameResultDaoFactory(dbProvider);
  }

  public static GameResultDao provideGameResultDao(TriviaMapDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideGameResultDao(db));
  }
}
