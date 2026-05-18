package com.triviamap;

import com.triviamap.domain.repository.TramLineRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<TramLineRepository> tramLineRepositoryProvider;

  public MainActivity_MembersInjector(Provider<TramLineRepository> tramLineRepositoryProvider) {
    this.tramLineRepositoryProvider = tramLineRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<TramLineRepository> tramLineRepositoryProvider) {
    return new MainActivity_MembersInjector(tramLineRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectTramLineRepository(instance, tramLineRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.triviamap.MainActivity.tramLineRepository")
  public static void injectTramLineRepository(MainActivity instance,
      TramLineRepository tramLineRepository) {
    instance.tramLineRepository = tramLineRepository;
  }
}
