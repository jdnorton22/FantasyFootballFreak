package com.fantasyfootball.analyzer.di

import android.content.Context
import com.fantasyfootball.analyzer.data.network.NetworkConnectivityManager
import com.fantasyfootball.analyzer.data.preferences.DataUsagePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for data usage and network connectivity components.
 * 
 * Requirements addressed:
 * - 6.4: Dependency injection for data usage controls and cellular network optimization
 */
@Module
@InstallIn(SingletonComponent::class)
object DataUsageModule {
    
    @Provides
    @Singleton
    fun provideDataUsagePreferences(
        @ApplicationContext context: Context
    ): DataUsagePreferences {
        return DataUsagePreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideNetworkConnectivityManager(
        @ApplicationContext context: Context
    ): NetworkConnectivityManager {
        return NetworkConnectivityManager(context)
    }
}