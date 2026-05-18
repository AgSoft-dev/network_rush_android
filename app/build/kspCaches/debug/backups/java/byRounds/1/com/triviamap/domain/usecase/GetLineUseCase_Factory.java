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
public final class GetLineUseCase_Factory implements Factory<GetLineUseCase> {
  private final Provider<TramLineRepository> repoProvider;

  public GetLineUseCase_Factory(Provider<TramLineRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public GetLineUseCase get() {
    return newInstance(repoProvider.get());
  }

  public static GetLineUseCase_Factory create(Provider<TramLineRepository> repoProvider) {
    return new GetLineUseCase_Factory(repoProvider);
  }

  public static GetLineUseCase newInstance(TramLineRepository repo) {
    return new GetLineUseCase(repo);
  }
}
