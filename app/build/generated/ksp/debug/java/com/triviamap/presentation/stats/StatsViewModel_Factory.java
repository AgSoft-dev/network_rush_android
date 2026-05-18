package com.triviamap.presentation.stats;

import com.triviamap.domain.usecase.GetBestScoresUseCase;
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
public final class StatsViewModel_Factory implements Factory<StatsViewModel> {
  private final Provider<GetBestScoresUseCase> getBestScoresProvider;

  public StatsViewModel_Factory(Provider<GetBestScoresUseCase> getBestScoresProvider) {
    this.getBestScoresProvider = getBestScoresProvider;
  }

  @Override
  public StatsViewModel get() {
    return newInstance(getBestScoresProvider.get());
  }

  public static StatsViewModel_Factory create(
      Provider<GetBestScoresUseCase> getBestScoresProvider) {
    return new StatsViewModel_Factory(getBestScoresProvider);
  }

  public static StatsViewModel newInstance(GetBestScoresUseCase getBestScores) {
    return new StatsViewModel(getBestScores);
  }
}
