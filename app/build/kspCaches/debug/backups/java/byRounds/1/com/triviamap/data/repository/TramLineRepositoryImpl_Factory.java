package com.triviamap.data.repository;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class TramLineRepositoryImpl_Factory implements Factory<TramLineRepositoryImpl> {
  private final Provider<Context> contextProvider;

  public TramLineRepositoryImpl_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TramLineRepositoryImpl get() {
    return newInstance(contextProvider.get());
  }

  public static TramLineRepositoryImpl_Factory create(Provider<Context> contextProvider) {
    return new TramLineRepositoryImpl_Factory(contextProvider);
  }

  public static TramLineRepositoryImpl newInstance(Context context) {
    return new TramLineRepositoryImpl(context);
  }
}
