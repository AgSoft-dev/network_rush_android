package com.triviamap.di

import android.content.Context
import androidx.room.Room
import com.triviamap.data.local.dao.GameResultDao
import com.triviamap.data.local.database.TriviaMapDatabase
import com.triviamap.data.repository.GameResultRepositoryImpl
import com.triviamap.data.repository.TramLineRepositoryImpl
import com.triviamap.data.repository.UserPreferencesRepositoryImpl
import com.triviamap.domain.repository.GameResultRepository
import com.triviamap.domain.repository.TramLineRepository
import com.triviamap.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): TriviaMapDatabase =
        Room.databaseBuilder(ctx, TriviaMapDatabase::class.java, "triviamap.db")
            .addMigrations(TriviaMapDatabase.MIGRATION_1_2, TriviaMapDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideGameResultDao(db: TriviaMapDatabase): GameResultDao = db.gameResultDao()

    @Provides
    fun provideStationStatsDao(db: TriviaMapDatabase): com.triviamap.data.local.dao.StationStatsDao = db.stationStatsDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTramLineRepo(impl: TramLineRepositoryImpl): TramLineRepository

    @Binds
    @Singleton
    abstract fun bindGameResultRepo(impl: GameResultRepositoryImpl): GameResultRepository

    @Binds
    @Singleton
    abstract fun bindStationStatsRepo(impl: com.triviamap.data.repository.StationStatsRepositoryImpl): com.triviamap.domain.repository.StationStatsRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepo(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindSupportRepo(impl: com.triviamap.data.monetization.BillingSupportRepository): com.triviamap.domain.monetization.SupportRepository

    @Binds
    @Singleton
    abstract fun bindAdsController(impl: com.triviamap.data.monetization.AdsControllerImpl): com.triviamap.domain.monetization.AdsController
}

@Module
@InstallIn(SingletonComponent::class)
object RuntimeModule {
    @Provides
    fun provideRandom(): kotlin.random.Random = kotlin.random.Random.Default

    @Provides
    fun provideClock(): java.time.Clock = java.time.Clock.systemDefaultZone()

    @Provides
    fun provideTimeSource(): com.triviamap.util.TimeSource = com.triviamap.util.TimeSource.System
}
