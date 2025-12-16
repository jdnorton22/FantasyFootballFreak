package com.fantasyfootball.analyzer.di

import android.content.Context
import androidx.room.Room
import com.fantasyfootball.analyzer.data.local.AppDatabase
import com.fantasyfootball.analyzer.data.local.dao.PlayerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies.
 * Provides Room database and DAO instances for dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    fun providePlayerDao(database: AppDatabase): PlayerDao {
        return database.playerDao()
    }
}