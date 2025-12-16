package com.fantasyfootball.analyzer.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.fantasyfootball.analyzer.data.local.dao.PlayerDao
import com.fantasyfootball.analyzer.data.local.entity.Player
import com.fantasyfootball.analyzer.data.local.entity.PlayerStats
import com.fantasyfootball.analyzer.data.local.entity.MatchupData

/**
 * Room database configuration for the Fantasy Football Analyzer app.
 * Manages local storage of player data, statistics, and matchup information.
 */
@Database(
    entities = [
        Player::class,
        PlayerStats::class,
        MatchupData::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun playerDao(): PlayerDao
    
    companion object {
        const val DATABASE_NAME = "fantasy_football_analyzer_db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}