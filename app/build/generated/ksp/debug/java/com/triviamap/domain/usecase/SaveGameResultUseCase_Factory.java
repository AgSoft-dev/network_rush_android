package com.triviamap.domain.usecase;

import com.triviamap.domain.repository.GameResultRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SaveGameResultUseCase_Factory implements Factory<SaveGameResultUseCase> {
  private final Provider<GameResultRepository> repoProvider;

  public SaveGameResultUseCase_Factory(Provider<GameResultRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public SaveGameResultUseCase get() {
    return newInstance(repoProvider.get());
  }

  public static SaveGameResultUseCase_Factory create(Provider<GameResultRepository> repoProvider) {
    return new SaveGameResultUseCase_Factory(repoProvider);
  }

  public static SaveGameResultUseCase newInstance(GameResultRepository repo) {
    return new SaveGameResultUseCase(repo);
  }
}
