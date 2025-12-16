package com.fantasyfootball.analyzer.presentation.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.fantasyfootball.analyzer.data.remote.ESPNApiDiagnostic
import com.fantasyfootball.analyzer.data.remote.DiagnosticResult
import com.fantasyfootball.analyzer.data.remote.EndpointTest
import com.fantasyfootball.analyzer.data.remote.TestStatus
import com.fantasyfootball.analyzer.presentation.ui.theme.FantasyFootballAnalyzerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Diagnostic activity for testing ESPN API connectivity.
 * Helps identify and resolve data connection issues.
 */
@AndroidEntryPoint
class DiagnosticActivity : ComponentActivity() {
    
    @Inject
    lateinit var diagnostic: ESPNApiDiagnostic
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FantasyFootballAnalyzerTheme {
                DiagnosticScreen(
                    onRunDiagnostic = { runDiagnostic() }
                )
            }
        }
    }
    
    private fun runDiagnostic() {
        lifecycleScope.launch {
            try {
                Log.i("DiagnosticActivity", "Starting ESPN API diagnostic...")
                val result = diagnostic.runDiagnostics()
                Log.i("DiagnosticActivity", "Diagnostic completed: ${result.overallStatus}")
                
                // Log detailed results
                result.endpointTests.forEach { test ->
                    Log.i("DiagnosticActivity", 
                        "Endpoint: ${test.endpoint} - Status: ${test.status} - Code: ${test.responseCode}")
                    if (test.responseBody != null) {
                        Log.d("DiagnosticActivity", "Response: ${test.responseBody}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("DiagnosticActivity", "Diagnostic failed", e)
            }
        }
    }
}

@Composable
fun DiagnosticScreen(
    onRunDiagnostic: () -> Unit
) {
    var diagnosticResult by remember { mutableStateOf<DiagnosticResult?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ESPN API Diagnostic Tool",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Button(
            onClick = {
                isRunning = true
                onRunDiagnostic()
            },
            enabled = !isRunning,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isRunning) "Running Diagnostic..." else "Run ESPN API Diagnostic")
        }
        
        diagnosticResult?.let { result ->
            DiagnosticResults(result = result)
        }
    }
}

@Composable
fun DiagnosticResults(result: DiagnosticResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Diagnostic Results",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Overall Status: ${result.overallStatus}",
                style = MaterialTheme.typography.bodyLarge,
                color = when (result.overallStatus) {
                    TestStatus.SUCCESS -> Color.Green
                    TestStatus.FAILURE -> Color.Red
                    TestStatus.ERROR -> Color(0xFFFF9800)
                },
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn {
                items(result.endpointTests) { test ->
                    EndpointTestItem(test = test)
                }
            }
        }
    }
}

@Composable
fun EndpointTestItem(test: EndpointTest) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = test.endpoint,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Status: ${test.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (test.status) {
                        TestStatus.SUCCESS -> Color.Green
                        TestStatus.FAILURE -> Color.Red
                        TestStatus.ERROR -> Color(0xFFFF9800)
                    }
                )
                
                Text(
                    text = "Code: ${test.responseCode}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (test.responseMessage.isNotEmpty()) {
                Text(
                    text = "Message: ${test.responseMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            if (!test.responseBody.isNullOrEmpty()) {
                Text(
                    text = "Response: ${test.responseBody.take(100)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}