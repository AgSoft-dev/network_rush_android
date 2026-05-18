package com.triviamap.domain.usecase;

import com.triviamap.domain.repository.TramLineRepository;
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
public final class GetAllLinesUseCase_Factory implements Factory<GetAllLinesUseCase> {
  private final Provider<TramLineRepository> repoProvider;

  public GetAllLinesUseCase_Factory(Provider<TramLineRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public GetAllLinesUseCase get() {
    return newInstance(repoProvider.get());
  }

  public static GetAllLinesUseCase_Factory create(Provider<TramLineRepository> repoProvider) {
    return new GetAllLinesUseCase_Factory(repoProvider);
  }

  public static GetAllLinesUseCase newInstance(TramLineRepository repo) {
    return new GetAllLinesUseCase(repo);
  }
}
