package com.triviamap.presentation.gameplay;

import androidx.lifecycle.SavedStateHandle;
import com.triviamap.domain.usecase.GetAllLinesUseCase;
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
public final class BlitzViewModel_Factory implements Factory<BlitzViewModel> {
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<GetAllLinesUseCase> getAllLinesProvider;

  private final Provider<SaveGameResultUseCase> saveResultProvider;

  public BlitzViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<GetAllLinesUseCase> getAllLinesProvider,
      Provider<SaveGameResultUseCase> saveResultProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.getAllLinesProvider = getAllLinesProvider;
    this.saveResultProvider = saveResultProvider;
  }

  @Override
  public BlitzViewModel get() {
    return newInstance(savedStateHandleProvider.get(), getAllLinesProvider.get(), saveResultProvider.get());
  }

  public static BlitzViewModel_Factory create(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<GetAllLinesUseCase> getAllLinesProvider,
      Provider<SaveGameResultUseCase> saveResultProvider) {
    return new BlitzViewModel_Factory(savedStateHandleProvider, getAllLinesProvider, saveResultProvider);
  }

  public static BlitzViewModel newInstance(SavedStateHandle savedStateHandle,
      GetAllLinesUseCase getAllLines, SaveGameResultUseCase saveResult) {
    return new BlitzViewModel(savedStateHandle, getAllLines, saveResult);
  }
}
