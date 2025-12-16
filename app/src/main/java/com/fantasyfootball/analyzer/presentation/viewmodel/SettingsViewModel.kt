package com.fantasyfootball.analyzer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.fantasyfootball.analyzer.data.network.NetworkConnectivityManager
import com.fantasyfootball.analyzer.data.preferences.DataUsagePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the settings screen, managing data usage preferences and network state.
 * 
 * Requirements addressed:
 * - 6.4: Settings screen with data usage control options
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataUsagePreferences: DataUsagePreferences,
    private val networkConnectivityManager: NetworkConnectivityManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadCurrentSettings()
    }
    
    /**
     * Loads current settings and updates UI state.
     */
    private fun loadCurrentSettings() {
        val currentNetworkType = networkConnectivityManager.getCurrentConnectionType()
        val isConnected = networkConnectivityManager.isNetworkAvailable()
        
        _uiState.value = _uiState.value.copy(
            cellularDataEnabled = dataUsagePreferences.isCellularDataEnabled,
            warnOnCellular = dataUsagePreferences.warnOnCellular,
            currentNetworkType = currentNetworkType,
            isConnected = isConnected
        )
    }
    
    /**
     * Updates cellular data enabled setting.
     */
    fun setCellularDataEnabled(enabled: Boolean) {
        dataUsagePreferences.isCellularDataEnabled = enabled
        _uiState.value = _uiState.value.copy(cellularDataEnabled = enabled)
    }
    
    /**
     * Updates warn on cellular setting.
     */
    fun setWarnOnCellular(warn: Boolean) {
        dataUsagePreferences.warnOnCellular = warn
        _uiState.value = _uiState.value.copy(warnOnCellular = warn)
    }
    
    /**
     * Refreshes network status.
     */
    fun refreshNetworkStatus() {
        loadCurrentSettings()
    }
}

/**
 * UI state for the settings screen.
 */
data class SettingsUiState(
    val cellularDataEnabled: Boolean = true,
    val warnOnCellular: Boolean = true,
    val currentNetworkType: NetworkConnectivityManager.ConnectionType = NetworkConnectivityManager.ConnectionType.NONE,
    val isConnected: Boolean = false
)