package com.fantasyfootball.analyzer.presentation.ui.accessibility

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Accessibility utilities for ensuring WCAG compliance
 */
object AccessibilityUtils {
    
    // WCAG 2.1 minimum touch target size
    val MinimumTouchTargetSize = 48.dp
    
    // WCAG 2.1 contrast ratios
    const val MinimumContrastRatio = 4.5f
    const val EnhancedContrastRatio = 7.0f
    const val LargeTextContrastRatio = 3.0f
    
    /**
     * Calculate contrast ratio between two colors
     * Based on WCAG 2.1 guidelines
     */
    fun calculateContrastRatio(foreground: Color, background: Color): Float {
        val foregroundLuminance = foreground.luminance()
        val backgroundLuminance = background.luminance()
        
        val lighter = max(foregroundLuminance, backgroundLuminance)
        val darker = min(foregroundLuminance, backgroundLuminance)
        
        return (lighter + 0.05f) / (darker + 0.05f)
    }
    
    /**
     * Check if color combination meets WCAG AA standards
     */
    fun meetsWCAGAA(foreground: Color, background: Color, isLargeText: Boolean = false): Boolean {
        val contrastRatio = calculateContrastRatio(foreground, background)
        return if (isLargeText) {
            contrastRatio >= LargeTextContrastRatio
        } else {
            contrastRatio >= MinimumContrastRatio
        }
    }
    
    /**
     * Check if color combination meets WCAG AAA standards
     */
    fun meetsWCAGAAA(foreground: Color, background: Color, isLargeText: Boolean = false): Boolean {
        val contrastRatio = calculateContrastRatio(foreground, background)
        return if (isLargeText) {
            contrastRatio >= MinimumContrastRatio
        } else {
            contrastRatio >= EnhancedContrastRatio
        }
    }
    
    /**
     * Ensure minimum touch target size for interactive elements
     */
    @Composable
    fun Modifier.minimumTouchTarget(): Modifier {
        return this.size(MinimumTouchTargetSize)
    }
    
    /**
     * Add semantic properties for screen readers
     */
    fun Modifier.accessibleClickable(
        label: String,
        action: String? = null,
        enabled: Boolean = true
    ): Modifier {
        return this.semantics {
            contentDescription = label
            action?.let { 
                // Custom semantic property for action description
                this[ActionDescriptionKey] = it
            }
            if (!enabled) {
                disabled()
            }
        }
    }
    
    /**
     * Add heading semantics for proper navigation
     */
    fun Modifier.accessibleHeading(level: Int = 1): Modifier {
        return this.semantics {
            this[HeadingLevelKey] = level
        }
    }
    
    /**
     * Add list semantics for proper navigation
     */
    fun Modifier.accessibleList(itemCount: Int): Modifier {
        return this.semantics {
            this[ListItemCountKey] = itemCount
        }
    }
    
    /**
     * Add progress semantics
     */
    fun Modifier.accessibleProgress(
        current: Float,
        range: ClosedFloatingPointRange<Float> = 0f..1f,
        description: String? = null
    ): Modifier {
        return this.semantics {
            progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(current, range)
            description?.let { contentDescription = it }
        }
    }
    
    /**
     * Validate theme colors for accessibility compliance
     */
    @Composable
    fun validateThemeAccessibility(): AccessibilityReport {
        val colorScheme = MaterialTheme.colorScheme
        val issues = mutableListOf<AccessibilityIssue>()
        
        // Check primary color combinations
        if (!meetsWCAGAA(colorScheme.onPrimary, colorScheme.primary)) {
            issues.add(
                AccessibilityIssue(
                    severity = AccessibilitySeverity.HIGH,
                    description = "Primary color combination does not meet WCAG AA standards",
                    recommendation = "Increase contrast between primary and onPrimary colors"
                )
            )
        }
        
        // Check secondary color combinations
        if (!meetsWCAGAA(colorScheme.onSecondary, colorScheme.secondary)) {
            issues.add(
                AccessibilityIssue(
                    severity = AccessibilitySeverity.HIGH,
                    description = "Secondary color combination does not meet WCAG AA standards",
                    recommendation = "Increase contrast between secondary and onSecondary colors"
                )
            )
        }
        
        // Check surface color combinations
        if (!meetsWCAGAA(colorScheme.onSurface, colorScheme.surface)) {
            issues.add(
                AccessibilityIssue(
                    severity = AccessibilitySeverity.HIGH,
                    description = "Surface color combination does not meet WCAG AA standards",
                    recommendation = "Increase contrast between surface and onSurface colors"
                )
            )
        }
        
        // Check error color combinations
        if (!meetsWCAGAA(colorScheme.onError, colorScheme.error)) {
            issues.add(
                AccessibilityIssue(
                    severity = AccessibilitySeverity.CRITICAL,
                    description = "Error color combination does not meet WCAG AA standards",
                    recommendation = "Increase contrast between error and onError colors"
                )
            )
        }
        
        return AccessibilityReport(
            isCompliant = issues.isEmpty(),
            issues = issues,
            wcagLevel = if (issues.isEmpty()) WCAGLevel.AA else WCAGLevel.NONE
        )
    }
}

// Custom semantic property keys
val ActionDescriptionKey = SemanticsPropertyKey<String>("ActionDescription")
val HeadingLevelKey = SemanticsPropertyKey<Int>("HeadingLevel")
val ListItemCountKey = SemanticsPropertyKey<Int>("ListItemCount")

// Extension functions for semantic properties
var SemanticsPropertyReceiver.actionDescription by ActionDescriptionKey
var SemanticsPropertyReceiver.headingLevel by HeadingLevelKey
var SemanticsPropertyReceiver.listItemCount by ListItemCountKey

/**
 * Data classes for accessibility reporting
 */
data class AccessibilityReport(
    val isCompliant: Boolean,
    val issues: List<AccessibilityIssue>,
    val wcagLevel: WCAGLevel
)

data class AccessibilityIssue(
    val severity: AccessibilitySeverity,
    val description: String,
    val recommendation: String
)

enum class AccessibilitySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class WCAGLevel {
    NONE, A, AA, AAA
}

/**
 * Accessibility testing helpers
 */
object AccessibilityTesting {
    
    /**
     * Test color contrast for a given color scheme
     */
    fun testColorContrast(
        foreground: Color,
        background: Color,
        context: String
    ): AccessibilityTestResult {
        val contrastRatio = AccessibilityUtils.calculateContrastRatio(foreground, background)
        val meetsAA = AccessibilityUtils.meetsWCAGAA(foreground, background)
        val meetsAAA = AccessibilityUtils.meetsWCAGAAA(foreground, background)
        
        return AccessibilityTestResult(
            testName = "Color Contrast - $context",
            passed = meetsAA,
            details = "Contrast ratio: ${String.format("%.2f", contrastRatio)}, " +
                    "WCAG AA: ${if (meetsAA) "PASS" else "FAIL"}, " +
                    "WCAG AAA: ${if (meetsAAA) "PASS" else "FAIL"}",
            recommendation = if (!meetsAA) {
                "Increase contrast ratio to at least ${AccessibilityUtils.MinimumContrastRatio}"
            } else null
        )
    }
    
    /**
     * Test touch target size
     */
    fun testTouchTargetSize(size: Dp, context: String): AccessibilityTestResult {
        val meetsMinimum = size >= AccessibilityUtils.MinimumTouchTargetSize
        
        return AccessibilityTestResult(
            testName = "Touch Target Size - $context",
            passed = meetsMinimum,
            details = "Size: $size, Minimum required: ${AccessibilityUtils.MinimumTouchTargetSize}",
            recommendation = if (!meetsMinimum) {
                "Increase touch target size to at least ${AccessibilityUtils.MinimumTouchTargetSize}"
            } else null
        )
    }
}

data class AccessibilityTestResult(
    val testName: String,
    val passed: Boolean,
    val details: String,
    val recommendation: String? = null
)