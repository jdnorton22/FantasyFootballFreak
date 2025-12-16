package com.fantasyfootball.analyzer.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Comprehensive error handling and user feedback components.
 * Implements requirements 1.4 and 2.4 for proper error handling and user messaging.
 */

/**
 * Error severity levels for appropriate styling and user messaging
 */
enum class ErrorSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Error types for specific handling and recovery options
 */
enum class ErrorType {
    NETWORK_UNAVAILABLE,
    API_RATE_LIMIT,
    INSUFFICIENT_DATA,
    CACHE_EXPIRED,
    INVALID_INPUT,
    UNKNOWN_ERROR
}

/**
 * Data class representing an error with context and recovery options
 */
data class AppError(
    val type: ErrorType,
    val severity: ErrorSeverity,
    val title: String,
    val message: String,
    val technicalDetails: String? = null,
    val canRetry: Boolean = true,
    val retryAction: (() -> Unit)? = null,
    val alternativeAction: (() -> Unit)? = null,
    val alternativeActionLabel: String? = null
)

/**
 * Comprehensive error display component with recovery options
 * Implements requirement 1.4: User-friendly error recovery options
 */
@Composable
fun ErrorDisplay(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerColor = when (error.severity) {
        ErrorSeverity.INFO -> MaterialTheme.colorScheme.primaryContainer
        ErrorSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        ErrorSeverity.ERROR -> MaterialTheme.colorScheme.errorContainer
        ErrorSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
    }
    
    val contentColor = when (error.severity) {
        ErrorSeverity.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
        ErrorSeverity.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        ErrorSeverity.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        ErrorSeverity.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
    }
    
    val icon = when (error.type) {
        ErrorType.NETWORK_UNAVAILABLE -> Icons.Default.Settings
        ErrorType.API_RATE_LIMIT -> Icons.Default.Settings
        ErrorType.INSUFFICIENT_DATA -> Icons.Default.Info
        ErrorType.CACHE_EXPIRED -> Icons.Default.Refresh
        ErrorType.INVALID_INPUT -> Icons.Default.Warning
        ErrorType.UNKNOWN_ERROR -> Icons.Default.Warning
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Dismiss button
                onDismiss?.let {
                    IconButton(
                        onClick = it,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Error message
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
            
            // Technical details (expandable)
            error.technicalDetails?.let { details ->
                var showDetails by remember { mutableStateOf(false) }
                
                TextButton(
                    onClick = { showDetails = !showDetails },
                    colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                ) {
                    Text(
                        text = if (showDetails) "Hide Details" else "Show Details",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Icon(
                        imageVector = if (showDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                if (showDetails) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = details,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Retry button
                if (error.canRetry && (onRetry != null || error.retryAction != null)) {
                    Button(
                        onClick = { onRetry?.invoke() ?: error.retryAction?.invoke() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = contentColor,
                            contentColor = containerColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry")
                    }
                }
                
                // Alternative action button
                if (error.alternativeAction != null && error.alternativeActionLabel != null) {
                    OutlinedButton(
                        onClick = error.alternativeAction,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = contentColor
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(contentColor)
                        )
                    ) {
                        Text(error.alternativeActionLabel)
                    }
                }
            }
        }
    }
}

/**
 * Loading state component with progress indicators
 * Implements requirement 1.4: Loading states and progress indicators
 */
@Composable
fun LoadingDisplay(
    message: String = "Loading...",
    showProgress: Boolean = false,
    progress: Float = 0f,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showProgress && progress > 0f) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Insufficient data fallback component
 * Implements requirement 2.4: Fallback mechanisms for insufficient historical data
 */
@Composable
fun InsufficientDataDisplay(
    title: String = "Limited Data Available",
    message: String,
    fallbackData: @Composable (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            // Show fallback data if available
            fallbackData?.invoke()
            
            // Refresh option
            onRefresh?.let {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = it,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

/**
 * Offline mode indicator
 * Implements requirement 3.2: Offline functionality indicators
 */
@Composable
fun OfflineModeIndicator(
    message: String = "Using cached data. Connect to internet for latest information.",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            
            onRetry?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Data freshness indicator
 * Implements requirement 3.3: Data freshness validation
 */
@Composable
fun DataFreshnessIndicator(
    isFresh: Boolean,
    lastUpdated: Long? = null,
    onRefresh: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val timeAgo = lastUpdated?.let { timestamp ->
        val now = System.currentTimeMillis()
        val diffMinutes = (now - timestamp) / (1000 * 60)
        when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
            else -> "${diffMinutes / 1440}d ago"
        }
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = if (isFresh) Icons.Default.CheckCircle else Icons.Default.Settings,
            contentDescription = null,
            tint = if (isFresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = if (isFresh) "Up to date" else timeAgo ?: "Data may be outdated",
            style = MaterialTheme.typography.labelSmall,
            color = if (isFresh) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
        )
        
        if (!isFresh && onRefresh != null) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Empty state component for when no data is available
 */
@Composable
fun EmptyStateDisplay(
    icon: ImageVector = Icons.Default.Search,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}