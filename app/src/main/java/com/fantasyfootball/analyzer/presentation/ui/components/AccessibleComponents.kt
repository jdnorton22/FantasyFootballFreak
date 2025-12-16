package com.fantasyfootball.analyzer.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fantasyfootball.analyzer.presentation.ui.theme.*

/**
 * Accessible button with proper touch feedback and semantic properties
 */
@Composable
fun AccessibleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription?.let { this.contentDescription = it }
            if (!enabled) {
                disabled()
            }
        },
        enabled = enabled,
        colors = colors,
        content = content
    )
}

/**
 * Accessible card with proper touch feedback and semantic grouping
 */
@Composable
fun AccessibleCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple()
            ) { onClick() }
            .semantics(mergeDescendants = true) {
                contentDescription?.let { this.contentDescription = it }
                if (enabled) {
                    role = Role.Button
                } else {
                    disabled()
                }
            }
    } else {
        modifier.semantics(mergeDescendants = true) {
            contentDescription?.let { this.contentDescription = it }
        }
    }

    Card(
        modifier = cardModifier,
        elevation = elevation,
        colors = colors,
        content = content
    )
}

/**
 * Status indicator with proper color contrast and accessibility
 */
@Composable
fun StatusIndicator(
    status: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val (backgroundColor, textColor) = when (status.uppercase()) {
        "HEALTHY", "ACTIVE" -> StatusHealthy to Color.White
        "QUESTIONABLE", "Q" -> StatusQuestionable to Color.White
        "DOUBTFUL", "D" -> StatusDoubtful to Color.White
        "OUT", "O", "IR" -> StatusOut to Color.White
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .semantics {
                contentDescription?.let { this.contentDescription = it }
                    ?: run { this.contentDescription = "Player status: $status" }
            },
        color = backgroundColor,
        contentColor = textColor
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Performance rating indicator with accessible colors and descriptions
 */
@Composable
fun PerformanceRating(
    rating: Double,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val (color, label) = when {
        rating >= 8.0 -> PerformanceExcellent to "Excellent"
        rating >= 6.0 -> PerformanceGood to "Good"
        rating >= 4.0 -> PerformanceAverage to "Average"
        else -> PerformancePoor to "Poor"
    }

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Performance rating: ${String.format("%.1f", rating)} out of 10, $label"
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = RoundedCornerShape(6.dp),
            color = color
        ) {}
        
        Text(
            text = String.format("%.1f", rating),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        if (showLabel) {
            Text(
                text = "($label)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Accessible icon button with proper touch target and feedback
 */
@Composable
fun AccessibleIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors()
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp) // Minimum touch target size
            .semantics {
                this.contentDescription = contentDescription
                if (!enabled) {
                    disabled()
                }
            },
        enabled = enabled,
        colors = colors
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Already provided in semantics
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Accessible text field with proper labels and error states
 */
@Composable
fun AccessibleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = label
                    if (isError && errorMessage != null) {
                        error(errorMessage)
                    }
                }
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Loading indicator with accessible announcements
 */
@Composable
fun AccessibleLoadingIndicator(
    message: String = "Loading",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics {
            contentDescription = message
            liveRegion = LiveRegionMode.Polite
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Accessible chip with proper selection states
 */
@Composable
fun AccessibleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple()
            )
            .semantics {
                contentDescription = if (selected) "$text, selected" else "$text, not selected"
                role = Role.Tab
                if (!enabled) {
                    disabled()
                }
            },
        color = backgroundColor,
        contentColor = textColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

/**
 * Accessible progress bar with semantic information
 */
@Composable
fun AccessibleProgressBar(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = true
) {
    val percentage = (progress * 100).toInt()
    
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $percentage percent"
            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            if (showPercentage) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}