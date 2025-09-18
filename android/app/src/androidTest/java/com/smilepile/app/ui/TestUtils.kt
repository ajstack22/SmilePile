package com.smilepile.app.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Utility class for SmilePile UI testing
 *
 * Provides common helper functions and custom matchers for testing
 * child-friendly UI components and accessibility features.
 */
object TestUtils {

    /**
     * Get the application context for testing
     */
    fun getContext(): Context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Get the application resources for testing
     */
    fun getResources(): Resources = getContext().resources

    /**
     * Convert dp to pixels using the current display metrics
     */
    fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().displayMetrics)
    }

    /**
     * Convert sp to pixels using the current display metrics
     */
    fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().displayMetrics)
    }

    /**
     * Convert pixels to dp using the current display metrics
     */
    fun pxToDp(px: Float): Float {
        return px / getResources().displayMetrics.density
    }

    /**
     * Calculate the contrast ratio between two colors
     * Used to verify accessibility compliance
     */
    fun calculateContrastRatio(foreground: Int, background: Int): Double {
        val foregroundLuminance = calculateLuminance(foreground)
        val backgroundLuminance = calculateLuminance(background)

        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)

        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Calculate the relative luminance of a color
     * Based on WCAG 2.1 guidelines
     */
    private fun calculateLuminance(color: Int): Double {
        val red = Color.red(color) / 255.0
        val green = Color.green(color) / 255.0
        val blue = Color.blue(color) / 255.0

        val r = if (red <= 0.03928) red / 12.92 else Math.pow((red + 0.055) / 1.055, 2.4)
        val g = if (green <= 0.03928) green / 12.92 else Math.pow((green + 0.055) / 1.055, 2.4)
        val b = if (blue <= 0.03928) blue / 12.92 else Math.pow((blue + 0.055) / 1.055, 2.4)

        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * Calculate the color difference between two colors
     * Used to ensure sufficient color differentiation
     */
    fun calculateColorDifference(color1: Int, color2: Int): Double {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        return Math.sqrt(
            Math.pow((r2 - r1).toDouble(), 2.0) +
            Math.pow((g2 - g1).toDouble(), 2.0) +
            Math.pow((b2 - b1).toDouble(), 2.0)
        )
    }

    /**
     * Custom matcher to check if a view has minimum touch target size
     */
    fun hasMinimumTouchTarget(minSizePx: Float): Matcher<View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has minimum touch target size of ${pxToDp(minSizePx)}dp")
            }

            override fun matchesSafely(view: View): Boolean {
                return view.width >= minSizePx && view.height >= minSizePx
            }
        }
    }

    /**
     * Custom matcher to check if a TextView has minimum text size
     */
    fun hasMinimumTextSize(minTextSizePx: Float): Matcher<View> {
        return object : BoundedMatcher<View, TextView>(TextView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has minimum text size of ${pxToDp(minTextSizePx)}sp")
            }

            override fun matchesSafely(textView: TextView): Boolean {
                return textView.textSize >= minTextSizePx
            }
        }
    }

    /**
     * Custom matcher to check if a view has sufficient contrast ratio
     */
    fun hasSufficientContrast(backgroundColor: Int, minRatio: Double = 4.5): Matcher<View> {
        return object : BoundedMatcher<View, TextView>(TextView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has sufficient contrast ratio of at least $minRatio:1")
            }

            override fun matchesSafely(textView: TextView): Boolean {
                val textColor = textView.currentTextColor
                val contrastRatio = calculateContrastRatio(textColor, backgroundColor)
                return contrastRatio >= minRatio
            }
        }
    }

    /**
     * Custom matcher to check if a view is child-friendly sized
     * (larger than typical adult UI elements)
     */
    fun isChildFriendlySized(): Matcher<View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("is child-friendly sized (larger than typical adult UI)")
            }

            override fun matchesSafely(view: View): Boolean {
                val minChildFriendlySize = dpToPx(48f) // Larger than standard 44dp
                return view.width >= minChildFriendlySize && view.height >= minChildFriendlySize
            }
        }
    }

    /**
     * Verify WCAG AAA compliance for text contrast
     */
    fun isWCAGAAACompliant(textColor: Int, backgroundColor: Int): Boolean {
        val contrastRatio = calculateContrastRatio(textColor, backgroundColor)
        return contrastRatio >= 7.0 // WCAG AAA standard
    }

    /**
     * Verify WCAG AA compliance for text contrast
     */
    fun isWCAGAACompliant(textColor: Int, backgroundColor: Int): Boolean {
        val contrastRatio = calculateContrastRatio(textColor, backgroundColor)
        return contrastRatio >= 4.5 // WCAG AA standard
    }

    /**
     * Verify WCAG AA compliance for large text contrast
     */
    fun isWCAGAALargeTextCompliant(textColor: Int, backgroundColor: Int): Boolean {
        val contrastRatio = calculateContrastRatio(textColor, backgroundColor)
        return contrastRatio >= 3.0 // WCAG AA standard for large text
    }

    /**
     * Check if a color is bright enough for children's eyes
     * Avoids colors that are too dark or too low contrast
     */
    fun isChildFriendlyColor(color: Int): Boolean {
        val luminance = calculateLuminance(color)
        // Avoid very dark colors that might be hard for children to see
        return luminance > 0.1
    }

    /**
     * Measure the time taken to execute a UI action
     * Useful for performance testing
     */
    inline fun measureActionTime(action: () -> Unit): Long {
        val startTime = System.currentTimeMillis()
        action()
        val endTime = System.currentTimeMillis()
        return endTime - startTime
    }

    /**
     * Constants for child-friendly UI testing
     */
    object ChildFriendlyConstants {
        const val MIN_TOUCH_TARGET_DP = 64f
        const val MIN_TEXT_SIZE_SP = 24f
        const val MIN_BUTTON_TEXT_SIZE_SP = 20f
        const val MIN_CAPTION_TEXT_SIZE_SP = 18f
        const val MAX_RESPONSE_TIME_MS = 100L
        const val MIN_CONTRAST_RATIO_AA = 4.5
        const val MIN_CONTRAST_RATIO_AAA = 7.0
        const val MIN_CONTRAST_RATIO_LARGE_TEXT = 3.0
        const val MAX_ANIMATION_DURATION_MS = 500L
        const val MIN_COLOR_DIFFERENCE = 100.0
    }
}