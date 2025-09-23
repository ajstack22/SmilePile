package com.smilepile.security

import android.app.Activity
import android.view.WindowManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smilepile.MainActivity
import com.smilepile.mode.AppMode
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import org.junit.Assert.*

/**
 * Integration tests for SecureActivity screenshot prevention and security features
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SecureActivityIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var inactivityManager: InactivityManager

    @Inject
    lateinit var securePreferencesManager: SecurePreferencesManager

    @Before
    fun setup() {
        hiltRule.inject()

        // Clear any existing security state
        securePreferencesManager.clearAllSettings()

        // Ensure timeout is enabled for testing
        inactivityManager.setTimeoutEnabled(true)
        inactivityManager.setTimeoutDuration(60_000L) // 1 minute for testing
    }

    @Test
    fun testScreenshotPreventionInParentMode() {
        composeTestRule.activity.runOnUiThread {
            // Get the activity window
            val window = composeTestRule.activity.window

            // Initially in Kids Mode - screenshot prevention should be OFF
            val initialFlags = window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE
            assertEquals("Screenshot prevention should be disabled in Kids Mode", 0, initialFlags)

            // Test that we can manually enable it (simulating parent mode)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )

            val secureFlags = window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE
            assertTrue("FLAG_SECURE should be set", secureFlags != 0)

            // Test clearing the flag
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            val clearedFlags = window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE
            assertEquals("FLAG_SECURE should be cleared", 0, clearedFlags)
        }
    }

    @Test
    fun testInactivityManagerIntegration() = runTest {
        // Test that InactivityManager is properly injected and configured
        assertTrue("InactivityManager should be injected", ::inactivityManager.isInitialized)
        assertTrue("Timeout should be enabled", inactivityManager.isTimeoutEnabled())
        assertEquals("Timeout duration should be set to 1 minute", 60_000L, inactivityManager.getTimeoutDuration())

        // Test activity recording
        inactivityManager.recordActivity()

        // Test timeout options
        val options = inactivityManager.getTimeoutOptions()
        assertTrue("Should have timeout options", options.isNotEmpty())

        // Test timeout formatting
        val formatted = inactivityManager.getTimeoutDurationFormatted()
        assertEquals("Should format 1 minute correctly", "1 minutes", formatted)
    }

    @Test
    fun testSecurityPreferencesIntegration() {
        // Test that SecurePreferencesManager is properly injected
        assertTrue("SecurePreferencesManager should be injected", ::securePreferencesManager.isInitialized)

        // Test PIN functionality
        assertFalse("PIN should not be enabled initially", securePreferencesManager.isPINEnabled())

        securePreferencesManager.setPIN("1234")
        assertTrue("PIN should be enabled after setting", securePreferencesManager.isPINEnabled())
        assertTrue("Correct PIN should validate", securePreferencesManager.validatePIN("1234"))
        assertFalse("Incorrect PIN should not validate", securePreferencesManager.validatePIN("5678"))

        // Test security summary
        val summary = securePreferencesManager.getSecuritySummary()
        assertTrue("Security summary should show PIN is set", summary.hasPIN)
        assertTrue("Kid safe mode should be enabled by default", summary.kidSafeModeEnabled)
    }

    @Test
    fun testActivityLifecycleIntegration() {
        // Test that the activity properly handles lifecycle events
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        // The activity should be in a valid state
        assertTrue("Activity should be created", composeTestRule.activity != null)
        assertFalse("Activity should not be finishing", composeTestRule.activity.isFinishing)

        // Test pause/resume cycle
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        // Activity should still be valid
        assertFalse("Activity should not be finishing after lifecycle changes", composeTestRule.activity.isFinishing)
    }

    @Test
    fun testSecurityFlagsApplication() {
        // Test that we can detect and modify security flags
        composeTestRule.activity.runOnUiThread {
            val activity = composeTestRule.activity as SecureActivity
            val window = activity.window

            // Test initial state
            val isSecureInitially = (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0

            // Force enable screenshot prevention
            activity.forceEnableScreenshotPrevention()
            val isSecureAfterForce = activity.isScreenshotPreventionActive()
            assertTrue("Screenshot prevention should be active after forcing", isSecureAfterForce)

            // Test that the flag is actually set
            val flags = window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE
            assertTrue("FLAG_SECURE should be set on window", flags != 0)
        }
    }

    @Test
    fun testUserInteractionTracking() {
        // Test that user interactions are properly tracked
        composeTestRule.activity.runOnUiThread {
            // Simulate user interaction
            composeTestRule.activity.onUserInteraction()

            // This should record activity in the InactivityManager
            // We can't easily verify the internal state, but we can ensure it doesn't crash
        }
    }

    @Test
    fun testSecuritySummaryAccess() {
        composeTestRule.activity.runOnUiThread {
            val activity = composeTestRule.activity as SecureActivity

            // Test that we can access security summary
            val summary = activity.getSecuritySummary()
            assertNotNull("Security summary should not be null", summary)

            // Test default values
            assertFalse("PIN should not be set initially", summary.hasPIN)
            assertTrue("Kid safe mode should be enabled by default", summary.kidSafeModeEnabled)
            assertEquals("Failed attempts should be 0 initially", 0, summary.failedAttempts)
            assertFalse("Should not be in cooldown initially", summary.isInCooldown)
        }
    }

    @Test
    fun testTimeoutConfigurationPersistence() {
        // Test that timeout configuration persists across activity lifecycle

        // Set custom timeout
        inactivityManager.setTimeoutDuration(300_000L) // 5 minutes
        inactivityManager.setTimeoutEnabled(false)

        // Verify settings
        assertEquals("Timeout duration should be set", 300_000L, inactivityManager.getTimeoutDuration())
        assertFalse("Timeout should be disabled", inactivityManager.isTimeoutEnabled())

        // Simulate activity recreation (pause/resume)
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        // Settings should persist
        assertEquals("Timeout duration should persist", 300_000L, inactivityManager.getTimeoutDuration())
        assertFalse("Timeout enabled state should persist", inactivityManager.isTimeoutEnabled())
    }
}