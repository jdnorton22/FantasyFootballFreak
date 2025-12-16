package com.fantasyfootball.analyzer.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fantasyfootball.analyzer.presentation.ui.theme.*
import com.fantasyfootball.analyzer.presentation.ui.accessibility.AccessibilityUtils

/**
 * Comprehensive showcase of Material Design 3 components with accessibility features
 * Demonstrates proper implementation of WCAG guidelines and Material Design principles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDesignShowcase(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics {
                contentDescription = "Material Design 3 component showcase with accessibility features"
            },
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Material Design 3 Showcase",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "Material Design 3 Showcase heading"
                }
            )
        }
        
        item {
            ColorSchemeSection()
        }
        
        item {
            ButtonsSection()
        }
        
        item {
            CardsSection()
        }
        
        item {
            StatusIndicatorsSection()
        }
        
        item {
            AccessibilityFeaturesSection()
        }
        
        item {
            InteractiveElementsSection()
        }
        
        item {
            TypographySection()
        }
    }
}

@Composable
private fun ColorSchemeSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Color Scheme",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorSwatch(
                color = MaterialTheme.colorScheme.primary,
                onColor = MaterialTheme.colorScheme.onPrimary,
                label = "Primary"
            )
            ColorSwatch(
                color = MaterialTheme.colorScheme.secondary,
                onColor = MaterialTheme.colorScheme.onSecondary,
                label = "Secondary"
            )
            ColorSwatch(
                color = MaterialTheme.colorScheme.tertiary,
                onColor = MaterialTheme.colorScheme.onTertiary,
                label = "Tertiary"
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorSwatch(
                color = MaterialTheme.colorScheme.error,
                onColor = MaterialTheme.colorScheme.onError,
                label = "Error"
            )
            ColorSwatch(
                color = MaterialTheme.colorScheme.surface,
                onColor = MaterialTheme.colorScheme.onSurface,
                label = "Surface"
            )
            ColorSwatch(
                color = MaterialTheme.colorScheme.surfaceVariant,
                onColor = MaterialTheme.colorScheme.onSurfaceVariant,
                label = "Surface Variant"
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    onColor: Color,
    label: String
) {
    val contrastRatio = AccessibilityUtils.calculateContrastRatio(onColor, color)
    val meetsWCAG = AccessibilityUtils.meetsWCAGAA(onColor, color)
    
    Card(
        modifier = Modifier
            .size(80.dp)
            .semantics {
                contentDescription = "$label color swatch, contrast ratio ${String.format("%.1f", contrastRatio)}, ${if (meetsWCAG) "meets" else "does not meet"} WCAG AA standards"
            },
        colors = CardDefaults.cardColors(
            containerColor = color,
            contentColor = onColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format("%.1f", contrastRatio),
                style = MaterialTheme.typography.labelSmall
            )
            if (meetsWCAG) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ButtonsSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Buttons with Accessibility",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccessibleButton(
                onClick = { },
                contentDescription = "Primary action button"
            ) {
                Text("Primary")
            }
            
            OutlinedButton(
                onClick = { },
                modifier = Modifier.semantics {
                    contentDescription = "Secondary action button"
                }
            ) {
                Text("Outlined")
            }
            
            TextButton(
                onClick = { },
                modifier = Modifier.semantics {
                    contentDescription = "Tertiary action button"
                }
            ) {
                Text("Text")
            }
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnhancedButton(
                onClick = { },
                loading = false,
                contentDescription = "Enhanced button with animations"
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enhanced")
            }
            
            EnhancedButton(
                onClick = { },
                loading = true,
                contentDescription = "Loading button demonstration"
            ) {
                Text("Loading")
            }
        }
    }
}

@Composable
private fun CardsSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Cards with Touch Feedback",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        AccessibleCard(
            onClick = { },
            contentDescription = "Interactive card with player information"
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Interactive Card",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "This card has proper touch feedback and accessibility support",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        EnhancedCard(
            onClick = { },
            contentDescription = "Enhanced card with press animations"
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Enhanced Card",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "This card includes press animations and enhanced visual feedback",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusIndicatorsSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Status Indicators",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIndicator(
                status = "Healthy",
                contentDescription = "Player is healthy and available to play"
            )
            StatusIndicator(
                status = "Questionable",
                contentDescription = "Player's availability is questionable"
            )
            StatusIndicator(
                status = "Out",
                contentDescription = "Player is out and unavailable to play"
            )
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PerformanceRating(
                rating = 8.5,
                showLabel = true
            )
            PerformanceRating(
                rating = 6.2,
                showLabel = true
            )
            PerformanceRating(
                rating = 3.1,
                showLabel = true
            )
        }
    }
}

@Composable
private fun AccessibilityFeaturesSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Accessibility Features",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        AccessibleTextField(
            value = "",
            onValueChange = { },
            label = "Accessible Text Field",
            placeholder = "Enter text here",
            errorMessage = null
        )
        
        AccessibleProgressBar(
            progress = 0.75f,
            label = "Loading Progress",
            showPercentage = true
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccessibleChip(
                text = "Selected",
                selected = true,
                onClick = { }
            )
            AccessibleChip(
                text = "Unselected",
                selected = false,
                onClick = { }
            )
        }
    }
}

@Composable
private fun InteractiveElementsSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Interactive Elements",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccessibleIconButton(
                onClick = { },
                icon = Icons.Default.Favorite,
                contentDescription = "Add to favorites"
            )
            AccessibleIconButton(
                onClick = { },
                icon = Icons.Default.Share,
                contentDescription = "Share player information"
            )
            AccessibleIconButton(
                onClick = { },
                icon = Icons.Default.Settings,
                contentDescription = "Open settings menu"
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnhancedChip(
                text = "QB",
                selected = true,
                onClick = { },
                icon = Icons.Default.Person
            )
            EnhancedChip(
                text = "RB",
                selected = false,
                onClick = { },
                icon = Icons.Default.Person
            )
            EnhancedChip(
                text = "WR",
                selected = false,
                onClick = { },
                icon = Icons.Default.Person
            )
        }
    }
}

@Composable
private fun TypographySection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Typography Scale",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Display Large",
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = "Headline Large",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Title Large",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Body Large - This is the main body text used throughout the application. It maintains proper line height and spacing for optimal readability.",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Body Medium - Secondary body text with slightly smaller size but maintaining readability standards.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Label Large",
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = "Label Small",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MaterialDesignShowcasePreview() {
    FantasyFootballAnalyzerTheme {
        MaterialDesignShowcase()
    }
}