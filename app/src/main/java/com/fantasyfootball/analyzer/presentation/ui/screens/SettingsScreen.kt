package com.fantasyfootball.analyzer.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fantasyfootball.analyzer.data.network.NetworkConnectivityManager
import com.fantasyfootball.analyzer.presentation.viewmodel.SettingsViewModel

/**
 * Settings screen for configuring data usage controls and cellular network optimization.
 * 
 * Requirements addressed:
 * - 6.4: Settings screen with data usage control options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Network Status Card
            NetworkStatusCard(
                networkType = uiState.currentNetworkType,
                isConnected = uiState.isConnected,
                onRefresh = viewModel::refreshNetworkStatus
            )
            
            // Data Usage Controls Section
            DataUsageControlsSection(
                cellularDataEnabled = uiState.cellularDataEnabled,
                warnOnCellular = uiState.warnOnCellular,
                onCellularDataEnabledChange = viewModel::setCellularDataEnabled,
                onWarnOnCellularChange = viewModel::setWarnOnCellular
            )
        }
    }
}

@Composable
private fun NetworkStatusCard(
    networkType: NetworkConnectivityManager.ConnectionType,
    isConnected: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Network Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                
                Column {
                    Text(
                        text = when (networkType) {
                            NetworkConnectivityManager.ConnectionType.WIFI -> "WiFi"
                            NetworkConnectivityManager.ConnectionType.CELLULAR -> "Cellular"
                            NetworkConnectivityManager.ConnectionType.ETHERNET -> "Ethernet"
                            NetworkConnectivityManager.ConnectionType.UNKNOWN -> "Unknown"
                            NetworkConnectivityManager.ConnectionType.NONE -> "No Connection"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = if (isConnected) "Connected" else "Disconnected",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DataUsageControlsSection(
    cellularDataEnabled: Boolean,
    warnOnCellular: Boolean,
    onCellularDataEnabledChange: (Boolean) -> Unit,
    onWarnOnCellularChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Data Usage Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Enable/Disable Cellular Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Use Cellular Data",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Allow app to use cellular data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = cellularDataEnabled,
                    onCheckedChange = onCellularDataEnabledChange
                )
            }
            
            if (cellularDataEnabled) {
                Divider()
                
                // Warn on Cellular
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Warn on Cellular",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Show warnings when using cellular data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(
                        checked = warnOnCellular,
                        onCheckedChange = onWarnOnCellularChange
                    )
                }
            }
        }
    }
}