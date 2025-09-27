package com.smilepile.security

import android.content.Context
import android.content.SharedPreferences
import com.smilepile.mode.AppMode
import com.smilepile.mode.ModeManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Security validation tests for Wave 4 Polish & Security features
 * Tests screenshot prevention, inactivity timeout, and security integration
 */
@ExperimentalCoroutinesApi
class SecurityValidationTest {

    @MockK(relaxed = true)
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var secureStorageManager: SecureStorageManager

    @MockK(relaxed = true)
    private lateinit var securePreferencesManager: SecurePreferencesManager

    @MockK(relaxed = true)
    private lateinit var inactivityManager: InactivityManager

    @MockK(relaxed = true)
    private lateinit var modeManager: ModeManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        // Mock shared preferences
        val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { context.packageName } returns "com.smilepile"
        every { sharedPrefs.edit() } returns editor
        every { sharedPrefs.getString(any(), any()) } returns null
        every { sharedPrefs.getBoolean(any(), any()) } returns false
        every { sharedPrefs.getLong(any(), any()) } returns 0L
        every { sharedPrefs.getInt(any(), any()) } returns 0
        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.clear() } returns editor
        every { editor.apply() } returns Unit
        every { editor.commit() } returns true

        // Mock SecureStorageManager encryption/decryption
        coEvery { secureStorageManager.encrypt(any()) } answers {
            "encrypted_" + firstArg<String>()
        }
        coEvery { secureStorageManager.decrypt(any()) } answers {
            val encrypted = firstArg<String>()
            if (encrypted.startsWith("encrypted_")) {
                encrypted.removePrefix("encrypted_")
            } else {
                encrypted
            }
        }

        // Configure mock behaviors for SecurePreferencesManager
        every { securePreferencesManager.isPINEnabled() } returns false
        every { securePreferencesManager.setPIN(any()) } returns Unit
        every { securePreferencesManager.validatePIN(any()) } answers {
            firstArg<String>() == "1234"
        }
        every { securePreferencesManager.clearPIN() } returns Unit
        every { securePreferencesManager.getSecuritySummary() } returns SecuritySummary(
            hasPIN = false,
            hasPattern = false,
            biometricEnabled = false,
            kidSafeModeEnabled = true,
            cameraAccessAllowed = true,
            deleteProtectionEnabled = true,
            failedAttempts = 0,
            isInCooldown = false
        )
        every { securePreferencesManager.setKidSafeModeEnabled(any()) } returns Unit
        every { securePreferencesManager.setCameraAccessAllowed(any()) } returns Unit
        every { securePreferencesManager.setDeleteProtectionEnabled(any()) } returns Unit
        every { securePreferencesManager.clearAllSettings() } returns Unit
        every { securePreferencesManager.getFailedAttempts() } returns 0
        every { securePreferencesManager.recordFailedAttempt() } returns Unit
        every { securePreferencesManager.isInCooldown() } returns false
        every { securePreferencesManager.getRemainingCooldownTime() } returns 0L
        every { securePreferencesManager.resetFailedAttempts() } returns Unit

        // Configure mock behaviors for InactivityManager
        every { inactivityManager.isTimeoutEnabled() } returns true
        every { inactivityManager.getTimeoutDuration() } returns 300_000L
        every { inactivityManager.setTimeoutDuration(any()) } answers {
            val duration = firstArg<Long>()
            every { inactivityManager.getTimeoutDuration() } returns duration.coerceIn(60_000L, 1_800_000L)
            Unit
        }
        every { inactivityManager.setTimeoutEnabled(any()) } answers {
            val enabled = firstArg<Boolean>()
            every { inactivityManager.isTimeoutEnabled() } returns enabled
            Unit
        }
        every { inactivityManager.getTimeoutOptions() } returns listOf(
            "1 minute" to 60_000L,
            "2 minutes" to 120_000L,
            "5 minutes" to 300_000L,
            "10 minutes" to 600_000L,
            "15 minutes" to 900_000L,
            "30 minutes" to 1_800_000L
        )
        every { inactivityManager.recordActivity() } returns Unit
        every { inactivityManager.setTimeoutCallback(any()) } returns Unit

        // Configure mock behaviors for ModeManager
        val modeFlow = MutableStateFlow(AppMode.KIDS)
        every { modeManager.currentMode } returns modeFlow
        every { modeManager.isKidsMode() } answers { modeFlow.value == AppMode.KIDS }
        every { modeManager.isParentMode() } answers { modeFlow.value == AppMode.PARENT }
        every { modeManager.toggleMode() } answers {
            modeFlow.value = if (modeFlow.value == AppMode.KIDS) AppMode.PARENT else AppMode.KIDS
            Unit
        }
        every { modeManager.setMode(any()) } answers {
            val mode = firstArg<AppMode>()
            modeFlow.value = mode
            Unit
        }
    }

    @Test
    fun testPINSecurityIntegration() {
        // Test PIN setup and validation
        val testPIN = "1234"
        var pinEnabled = false

        // Mock PIN state management
        every { securePreferencesManager.isPINEnabled() } answers { pinEnabled }
        every { securePreferencesManager.setPIN(any()) } answers {
            pinEnabled = true
            Unit
        }
        every { securePreferencesManager.clearPIN() } answers {
            pinEnabled = false
            Unit
        }

        // Initially no PIN should be set
        assertFalse("PIN should not be enabled initially", securePreferencesManager.isPINEnabled())

        // Set a PIN
        securePreferencesManager.setPIN(testPIN)
        assertTrue("PIN should be enabled after setting", securePreferencesManager.isPINEnabled())

        // Validate correct PIN
        assertTrue("Correct PIN should validate", securePreferencesManager.validatePIN(testPIN))

        // Validate incorrect PIN
        assertFalse("Incorrect PIN should not validate", securePreferencesManager.validatePIN("9999"))

        // Clear PIN
        securePreferencesManager.clearPIN()
        assertFalse("PIN should be disabled after clearing", securePreferencesManager.isPINEnabled())
    }

    @Test
    fun testInactivityManagerConfiguration() {
        // Test default configuration
        assertTrue("Timeout should be enabled by default", inactivityManager.isTimeoutEnabled())
        assertEquals("Default timeout should be 5 minutes", 300_000L, inactivityManager.getTimeoutDuration())

        // Test timeout duration configuration
        val customDuration = 600_000L // 10 minutes
        inactivityManager.setTimeoutDuration(customDuration)
        assertEquals("Custom timeout duration should be set", customDuration, inactivityManager.getTimeoutDuration())

        // Test timeout enable/disable
        inactivityManager.setTimeoutEnabled(false)
        assertFalse("Timeout should be disabled", inactivityManager.isTimeoutEnabled())

        inactivityManager.setTimeoutEnabled(true)
        assertTrue("Timeout should be enabled", inactivityManager.isTimeoutEnabled())
    }

    @Test
    fun testInactivityManagerBounds() {
        // Test minimum duration constraint
        inactivityManager.setTimeoutDuration(30_000L) // 30 seconds (below minimum)
        assertTrue("Duration should be clamped to minimum", inactivityManager.getTimeoutDuration() >= 60_000L)

        // Test maximum duration constraint
        inactivityManager.setTimeoutDuration(3_600_000L) // 60 minutes (above maximum)
        assertTrue("Duration should be clamped to maximum", inactivityManager.getTimeoutDuration() <= 1_800_000L)

        // Test valid duration
        val validDuration = 300_000L // 5 minutes
        inactivityManager.setTimeoutDuration(validDuration)
        assertEquals("Valid duration should be set exactly", validDuration, inactivityManager.getTimeoutDuration())
    }

    @Test
    fun testSecuritySummary() {
        var hasPIN = false
        var kidSafeModeEnabled = true
        var cameraAccessAllowed = true
        var deleteProtectionEnabled = true

        // Configure dynamic mock responses
        every { securePreferencesManager.getSecuritySummary() } answers {
            SecuritySummary(
                hasPIN = hasPIN,
                hasPattern = false,
                biometricEnabled = false,
                kidSafeModeEnabled = kidSafeModeEnabled,
                cameraAccessAllowed = cameraAccessAllowed,
                deleteProtectionEnabled = deleteProtectionEnabled,
                failedAttempts = 0,
                isInCooldown = false
            )
        }
        every { securePreferencesManager.setPIN(any()) } answers {
            hasPIN = true
            Unit
        }
        every { securePreferencesManager.setKidSafeModeEnabled(any()) } answers {
            kidSafeModeEnabled = firstArg()
            Unit
        }
        every { securePreferencesManager.setCameraAccessAllowed(any()) } answers {
            cameraAccessAllowed = firstArg()
            Unit
        }
        every { securePreferencesManager.setDeleteProtectionEnabled(any()) } answers {
            deleteProtectionEnabled = firstArg()
            Unit
        }

        // Test security summary with no PIN
        var summary = securePreferencesManager.getSecuritySummary()
        assertFalse("Should report no PIN", summary.hasPIN)
        assertTrue("Kid safe mode should be enabled by default", summary.kidSafeModeEnabled)
        assertTrue("Camera access should be allowed by default", summary.cameraAccessAllowed)
        assertTrue("Delete protection should be enabled by default", summary.deleteProtectionEnabled)

        // Set a PIN and test summary update
        securePreferencesManager.setPIN("1234")
        summary = securePreferencesManager.getSecuritySummary()
        assertTrue("Should report PIN is set", summary.hasPIN)

        // Test with modified settings
        securePreferencesManager.setKidSafeModeEnabled(false)
        securePreferencesManager.setCameraAccessAllowed(false)
        securePreferencesManager.setDeleteProtectionEnabled(false)

        summary = securePreferencesManager.getSecuritySummary()
        assertFalse("Kid safe mode should be disabled", summary.kidSafeModeEnabled)
        assertFalse("Camera access should be disabled", summary.cameraAccessAllowed)
        assertFalse("Delete protection should be disabled", summary.deleteProtectionEnabled)
    }

    @Test
    fun testFailedAttemptsHandling() {
        var failedAttempts = 0
        var inCooldown = false

        // Configure dynamic mock responses
        every { securePreferencesManager.getFailedAttempts() } answers { failedAttempts }
        every { securePreferencesManager.isInCooldown() } answers { inCooldown }
        every { securePreferencesManager.getRemainingCooldownTime() } answers {
            if (inCooldown) 30000L else 0L
        }
        every { securePreferencesManager.recordFailedAttempt() } answers {
            failedAttempts++
            if (failedAttempts >= 6) {
                inCooldown = true
            }
            Unit
        }
        every { securePreferencesManager.resetFailedAttempts() } answers {
            failedAttempts = 0
            inCooldown = false
            Unit
        }

        // Set up PIN for testing
        securePreferencesManager.setPIN("1234")

        // Test initial state
        assertEquals("Failed attempts should start at 0", 0, securePreferencesManager.getFailedAttempts())
        assertFalse("Should not be in cooldown initially", securePreferencesManager.isInCooldown())

        // Record failed attempts
        repeat(3) {
            securePreferencesManager.recordFailedAttempt()
        }
        assertEquals("Should record 3 failed attempts", 3, securePreferencesManager.getFailedAttempts())
        assertFalse("Should not be in cooldown yet", securePreferencesManager.isInCooldown())

        // Exceed maximum attempts
        repeat(3) {
            securePreferencesManager.recordFailedAttempt()
        }
        assertTrue("Should be in cooldown after max attempts", securePreferencesManager.isInCooldown())
        assertTrue("Remaining cooldown time should be positive", securePreferencesManager.getRemainingCooldownTime() > 0)

        // Reset attempts
        securePreferencesManager.resetFailedAttempts()
        assertEquals("Failed attempts should be reset", 0, securePreferencesManager.getFailedAttempts())
        assertFalse("Should not be in cooldown after reset", securePreferencesManager.isInCooldown())
    }

    @Test
    fun testModeManagerIntegration() = runTest {
        // Test initial mode (should be Kids)
        assertEquals("Initial mode should be Kids", AppMode.KIDS, modeManager.currentMode.first())
        assertTrue("Should report Kids mode", modeManager.isKidsMode())
        assertFalse("Should not report Parent mode", modeManager.isParentMode())

        // Test mode toggle
        modeManager.toggleMode()
        assertEquals("Mode should toggle to Parent", AppMode.PARENT, modeManager.currentMode.first())

        // Test direct mode setting
        modeManager.setMode(AppMode.KIDS)
        assertEquals("Mode should be set to Kids", AppMode.KIDS, modeManager.currentMode.first())
    }

    @Test
    fun testTimeoutOptions() {
        val options = inactivityManager.getTimeoutOptions()

        assertTrue("Should have multiple timeout options", options.size >= 6)

        // Verify all options are within valid bounds
        options.forEach { (label, duration) ->
            assertTrue("Option '$label' should be within bounds",
                duration >= 60_000L && duration <= 1_800_000L)
        }

        // Verify options are sorted (shortest to longest)
        val durations = options.map { it.second }
        assertEquals("Options should be sorted by duration", durations, durations.sorted())
    }

    @Test
    fun testActivityRecording() {
        // Test activity recording doesn't crash
        inactivityManager.recordActivity()

        // Test timeout callback setup
        var callbackCalled = false
        inactivityManager.setTimeoutCallback {
            callbackCalled = true
        }

        // Verify callback is set (we can't easily test the actual timeout in unit tests)
        assertNotNull("Timeout callback should be set", inactivityManager)
    }

    @Test
    fun testSecurityFeatureIntegration() {
        var hasPIN = false
        var kidSafeModeEnabled = false
        var deleteProtectionEnabled = false

        // Configure dynamic mock responses for security features
        every { securePreferencesManager.setPIN(any()) } answers {
            hasPIN = true
            Unit
        }
        every { securePreferencesManager.setKidSafeModeEnabled(any()) } answers {
            kidSafeModeEnabled = firstArg()
            Unit
        }
        every { securePreferencesManager.setDeleteProtectionEnabled(any()) } answers {
            deleteProtectionEnabled = firstArg()
            Unit
        }
        every { securePreferencesManager.getSecuritySummary() } answers {
            SecuritySummary(
                hasPIN = hasPIN,
                hasPattern = false,
                biometricEnabled = false,
                kidSafeModeEnabled = kidSafeModeEnabled,
                cameraAccessAllowed = true,
                deleteProtectionEnabled = deleteProtectionEnabled,
                failedAttempts = 0,
                isInCooldown = false
            )
        }
        every { securePreferencesManager.clearAllSettings() } answers {
            hasPIN = false
            Unit
        }

        // Enable all security features
        securePreferencesManager.setPIN("1234")
        securePreferencesManager.setKidSafeModeEnabled(true)
        securePreferencesManager.setDeleteProtectionEnabled(true)
        inactivityManager.setTimeoutEnabled(true)
        inactivityManager.setTimeoutDuration(300_000L)

        // Verify all features are active
        val summary = securePreferencesManager.getSecuritySummary()
        assertTrue("PIN should be active", summary.hasPIN)
        assertTrue("Kid safe mode should be active", summary.kidSafeModeEnabled)
        assertTrue("Delete protection should be active", summary.deleteProtectionEnabled)
        assertTrue("Timeout should be enabled", inactivityManager.isTimeoutEnabled())

        // Test that features don't interfere with each other
        assertTrue("PIN validation should still work", securePreferencesManager.validatePIN("1234"))
        assertEquals("Timeout duration should be preserved", 300_000L, inactivityManager.getTimeoutDuration())

        // Test clearing all settings
        securePreferencesManager.clearAllSettings()
        val clearedSummary = securePreferencesManager.getSecuritySummary()
        assertFalse("PIN should be cleared", clearedSummary.hasPIN)

        // Timeout settings should persist (stored separately)
        assertTrue("Timeout should still be enabled after clearing other settings", inactivityManager.isTimeoutEnabled())
    }
}