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
public final class GetBestScoresUseCase_Factory implements Factory<GetBestScoresUseCase> {
  private final Provider<GameResultRepository> repoProvider;

  public GetBestScoresUseCase_Factory(Provider<GameResultRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public GetBestScoresUseCase get() {
    return newInstance(repoProvider.get());
  }

  public static GetBestScoresUseCase_Factory create(Provider<GameResultRepository> repoProvider) {
    return new GetBestScoresUseCase_Factory(repoProvider);
  }

  public static GetBestScoresUseCase newInstance(GameResultRepository repo) {
    return new GetBestScoresUseCase(repo);
  }
}
