package com.triviamap.presentation.lineselection;

import com.triviamap.domain.usecase.GetAllLinesUseCase;
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
public final class LineSelectionViewModel_Factory implements Factory<LineSelectionViewModel> {
  private final Provider<GetAllLinesUseCase> getAllLinesProvider;

  public LineSelectionViewModel_Factory(Provider<GetAllLinesUseCase> getAllLinesProvider) {
    this.getAllLinesProvider = getAllLinesProvider;
  }

  @Override
  public LineSelectionViewModel get() {
    return newInstance(getAllLinesProvider.get());
  }

  public static LineSelectionViewModel_Factory create(
      Provider<GetAllLinesUseCase> getAllLinesProvider) {
    return new LineSelectionViewModel_Factory(getAllLinesProvider);
  }

  public static LineSelectionViewModel newInstance(GetAllLinesUseCase getAllLines) {
    return new LineSelectionViewModel(getAllLines);
  }
}
