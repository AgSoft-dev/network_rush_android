package com.triviamap.presentation.gameplay;

import androidx.lifecycle.SavedStateHandle;
import com.triviamap.domain.repository.GameResultRepository;
import com.triviamap.domain.repository.TramLineRepository;
import com.triviamap.domain.repository.UserPreferencesRepository;
import com.triviamap.domain.usecase.SaveGameResultUseCase;
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
public final class SprintViewModel_Factory implements Factory<SprintViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<TramLineRepository> lineRepositoryProvider;

  private final Provider<SaveGameResultUseCase> saveResultProvider;

  private final Provider<GameResultRepository> resultRepositoryProvider;

  private final Provider<UserPreferencesRepository> userPrefsProvider;

  public SprintViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<TramLineRepository> lineRepositoryProvider,
      Provider<SaveGameResultUseCase> saveResultProvider,
      Provider<GameResultRepository> resultRepositoryProvider,
      Provider<UserPreferencesRepository> userPrefsProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.lineRepositoryProvider = lineRepositoryProvider;
    this.saveResultProvider = saveResultProvider;
    this.resultRepositoryProvider = resultRepositoryProvider;
    this.userPrefsProvider = userPrefsProvider;
  }

  @Override
  public SprintViewModel get() {
    return newInstance(savedStateHandleProvider.get(), lineRepositoryProvider.get(), saveResultProvider.get(), resultRepositoryProvider.get(), userPrefsProvider.get());
  }

  public static SprintViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<TramLineRepository> lineRepositoryProvider,
      Provider<SaveGameResultUseCase> saveResultProvider,
      Provider<GameResultRepository> resultRepositoryProvider,
      Provider<UserPreferencesRepository> userPrefsProvider) {
    return new SprintViewModel_Factory(savedStateHandleProvider, lineRepositoryProvider, saveResultProvider, resultRepositoryProvider, userPrefsProvider);
  }

  public static SprintViewModel newInstance(SavedStateHandle savedStateHandle,
      TramLineRepository lineRepository, SaveGameResultUseCase saveResult,
      GameResultRepository resultRepository, UserPreferencesRepository userPrefs) {
    return new SprintViewModel(savedStateHandle, lineRepository, saveResult, resultRepository, userPrefs);
  }
}
