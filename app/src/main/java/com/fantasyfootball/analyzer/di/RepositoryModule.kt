package com.fantasyfootball.analyzer.di

import android.content.Context
import android.content.SharedPreferences
import com.fantasyfootball.analyzer.data.cache.CacheManager
import com.fantasyfootball.analyzer.data.local.dao.PlayerDao
import com.fantasyfootball.analyzer.data.remote.ESPNApiService
import com.fantasyfootball.analyzer.data.repository.PlayerRepositoryImpl
import com.fantasyfootball.analyzer.data.search.FuzzySearchEngine
import com.fantasyfootball.analyzer.data.search.SearchHistoryManager
import com.fantasyfootball.analyzer.domain.repository.PlayerRepository
import com.fantasyfootball.analyzer.domain.usecase.MatchupAnalyzer
import com.fantasyfootball.analyzer.domain.usecase.MatchupAnalyzerImpl
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository dependencies.
 * Provides cache manager and repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindPlayerRepository(
        playerRepositoryImpl: PlayerRepositoryImpl
    ): PlayerRepository
    
    @Binds
    @Singleton
    abstract fun bindMatchupAnalyzer(
        matchupAnalyzerImpl: MatchupAnalyzerImpl
    ): MatchupAnalyzer
    
    companion object {
        @Provides
        @Singleton
        fun provideCacheManager(playerDao: PlayerDao): CacheManager {
            return CacheManager(playerDao)
        }
        
        @Provides
        @Singleton
        fun provideFuzzySearchEngine(): FuzzySearchEngine {
            return FuzzySearchEngine()
        }
        
        @Provides
        @Singleton
        fun provideSearchHistoryManager(
            @ApplicationContext context: Context,
            gson: Gson
        ): SearchHistoryManager {
            return SearchHistoryManager(context, gson)
        }
        

    }
}