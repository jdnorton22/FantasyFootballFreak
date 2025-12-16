package com.fantasyfootball.analyzer.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages network connectivity state and provides information about connection type.
 * Specifically detects cellular vs WiFi connections for data usage optimization.
 * 
 * Requirements addressed:
 * - 6.4: Cellular network detection for data usage controls
 */
@Singleton
class NetworkConnectivityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Types of network connections.
     */
    enum class ConnectionType {
        WIFI,
        CELLULAR,
        ETHERNET,
        UNKNOWN,
        NONE
    }
    
    /**
     * Gets the current network connection type.
     */
    fun getCurrentConnectionType(): ConnectionType {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        return if (networkCapabilities != null) {
            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
                else -> ConnectionType.UNKNOWN
            }
        } else {
            ConnectionType.NONE
        }
    }
    
    /**
     * Checks if the current connection is cellular.
     */
    fun isCellularConnection(): Boolean {
        return getCurrentConnectionType() == ConnectionType.CELLULAR
    }
    
    /**
     * Checks if the current connection is WiFi.
     */
    fun isWiFiConnection(): Boolean {
        return getCurrentConnectionType() == ConnectionType.WIFI
    }
    
    /**
     * Checks if network is available for data operations.
     */
    fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}