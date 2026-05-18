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
public final class GetLineResultsUseCase_Factory implements Factory<GetLineResultsUseCase> {
  private final Provider<GameResultRepository> repoProvider;

  public GetLineResultsUseCase_Factory(Provider<GameResultRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public GetLineResultsUseCase get() {
    return newInstance(repoProvider.get());
  }

  public static GetLineResultsUseCase_Factory create(Provider<GameResultRepository> repoProvider) {
    return new GetLineResultsUseCase_Factory(repoProvider);
  }

  public static GetLineResultsUseCase newInstance(GameResultRepository repo) {
    return new GetLineResultsUseCase(repo);
  }
}
