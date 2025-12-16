package com.fantasyfootball.analyzer.di

import com.fantasyfootball.analyzer.presentation.utils.ErrorHandlingService
import com.fantasyfootball.analyzer.presentation.utils.LoadingStateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing error handling and loading state management dependencies.
 * Implements requirement 1.4: Comprehensive error handling and user feedback infrastructure.
 */
@Module
@InstallIn(SingletonComponent::class)
object ErrorHandlingModule {
    
    /**
     * Provides singleton ErrorHandlingService for centralized error management
     */
    @Provides
    @Singleton
    fun provideErrorHandlingService(): ErrorHandlingService {
        return ErrorHandlingService()
    }
    
    /**
     * Provides LoadingStateManager for managing loading states across the application
     * Note: This is provided as a singleton to allow sharing loading states across ViewModels if needed
     */
    @Provides
    @Singleton
    fun provideLoadingStateManager(): LoadingStateManager {
        return LoadingStateManager()
    }
}