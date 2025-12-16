package com.fantasyfootball.analyzer.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages user preferences for data usage controls on cellular networks.
 * Provides settings for limiting data consumption when not on WiFi.
 * 
 * Requirements addressed:
 * - 6.4: User options to limit data usage on cellular networks
 */
@Singleton
class DataUsagePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "data_usage_preferences"
        private const val KEY_CELLULAR_DATA_ENABLED = "cellular_data_enabled"
        private const val KEY_WARN_ON_CELLULAR = "warn_on_cellular"
        
        // Default values
        private const val DEFAULT_CELLULAR_DATA_ENABLED = true
        private const val DEFAULT_WARN_ON_CELLULAR = true
    }
    
    /**
     * Whether data usage on cellular networks is enabled.
     */
    var isCellularDataEnabled: Boolean
        get() = preferences.getBoolean(KEY_CELLULAR_DATA_ENABLED, DEFAULT_CELLULAR_DATA_ENABLED)
        set(value) = preferences.edit().putBoolean(KEY_CELLULAR_DATA_ENABLED, value).apply()
    
    /**
     * Whether to show warnings when using cellular data.
     */
    var warnOnCellular: Boolean
        get() = preferences.getBoolean(KEY_WARN_ON_CELLULAR, DEFAULT_WARN_ON_CELLULAR)
        set(value) = preferences.edit().putBoolean(KEY_WARN_ON_CELLULAR, value).apply()
}