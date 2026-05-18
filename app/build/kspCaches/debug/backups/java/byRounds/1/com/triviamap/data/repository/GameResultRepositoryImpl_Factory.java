package com.triviamap.data.repository;

import com.triviamap.data.local.dao.GameResultDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class GameResultRepositoryImpl_Factory implements Factory<GameResultRepositoryImpl> {
  private final Provider<GameResultDao> daoProvider;

  public GameResultRepositoryImpl_Factory(Provider<GameResultDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public GameResultRepositoryImpl get() {
    return newInstance(daoProvider.get());
  }

  public static GameResultRepositoryImpl_Factory create(Provider<GameResultDao> daoProvider) {
    return new GameResultRepositoryImpl_Factory(daoProvider);
  }

  public static GameResultRepositoryImpl newInstance(GameResultDao dao) {
    return new GameResultRepositoryImpl(dao);
  }
}
