package com.smilepile.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage manager for parental controls and child safety settings
 * Uses EncryptedSharedPreferences with Android Keystore for secure data storage
 * Implements proper password hashing with salt using PBKDF2
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorageManager
) {
    companion object {
        private const val PREFS_NAME = "smilepile_secure_prefs"
        private const val KEY_PARENTAL_PIN = "parental_pin"
        private const val KEY_PARENTAL_PATTERN = "parental_pattern"
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_PATTERN_ENABLED = "pattern_enabled"
        private const val KEY_KID_SAFE_MODE = "kid_safe_mode"
        private const val KEY_DELETE_PROTECTION_ENABLED = "delete_protection_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LAST_FAILED_ATTEMPT = "last_failed_attempt"

        private const val MAX_FAILED_ATTEMPTS = 5
        private const val COOLDOWN_DURATION_MS = 30000L // 30 seconds
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // State flows for reactive updates
    private val _kidSafeModeEnabled = MutableStateFlow(getKidSafeModeEnabled())
    val kidSafeModeEnabled: Flow<Boolean> = _kidSafeModeEnabled.asStateFlow()

    private val _deleteProtectionEnabled = MutableStateFlow(getDeleteProtectionEnabled())
    val deleteProtectionEnabled: Flow<Boolean> = _deleteProtectionEnabled.asStateFlow()

    /**
     * PIN Management
     */
    fun setPIN(pin: String) {
        val salt = secureStorage.generateSalt()
        val hashedPin = secureStorage.hashPasswordWithSalt(pin, salt)
        encryptedPrefs.edit()
            .putString(KEY_PARENTAL_PIN, hashedPin)
            .putBoolean(KEY_PIN_ENABLED, true)
            .apply()
    }

    fun validatePIN(pin: String): Boolean {
        val storedHash = encryptedPrefs.getString(KEY_PARENTAL_PIN, null) ?: return false
        return secureStorage.verifyPassword(pin, storedHash)
    }

    fun isPINEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_PIN_ENABLED, false)
    }

    fun clearPIN() {
        encryptedPrefs.edit()
            .remove(KEY_PARENTAL_PIN)
            .putBoolean(KEY_PIN_ENABLED, false)
            .apply()
    }

    /**
     * Pattern Management
     */
    fun setPattern(pattern: List<Int>) {
        val patternString = pattern.joinToString(",")
        val salt = secureStorage.generateSalt()
        val hashedPattern = secureStorage.hashPasswordWithSalt(patternString, salt)
        encryptedPrefs.edit()
            .putString(KEY_PARENTAL_PATTERN, hashedPattern)
            .putBoolean(KEY_PATTERN_ENABLED, true)
            .apply()
    }

    fun validatePattern(pattern: List<Int>): Boolean {
        val storedHash = encryptedPrefs.getString(KEY_PARENTAL_PATTERN, null) ?: return false
        val patternString = pattern.joinToString(",")
        return secureStorage.verifyPassword(patternString, storedHash)
    }

    fun isPatternEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_PATTERN_ENABLED, false)
    }

    fun clearPattern() {
        encryptedPrefs.edit()
            .remove(KEY_PARENTAL_PATTERN)
            .putBoolean(KEY_PATTERN_ENABLED, false)
            .apply()
    }

    /**
     * Authentication State
     */
    fun hasParentalLock(): Boolean {
        return isPINEnabled() || isPatternEnabled()
    }

    /**
     * Failed Attempts Management
     */
    fun recordFailedAttempt() {
        val currentAttempts = getFailedAttempts()
        encryptedPrefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, currentAttempts + 1)
            .putLong(KEY_LAST_FAILED_ATTEMPT, System.currentTimeMillis())
            .apply()
    }

    fun resetFailedAttempts() {
        encryptedPrefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_LAST_FAILED_ATTEMPT)
            .apply()
    }

    fun getFailedAttempts(): Int {
        return encryptedPrefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    fun isInCooldown(): Boolean {
        if (getFailedAttempts() < MAX_FAILED_ATTEMPTS) return false

        val lastFailedAttempt = encryptedPrefs.getLong(KEY_LAST_FAILED_ATTEMPT, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastFailedAttempt) < COOLDOWN_DURATION_MS
    }

    fun getRemainingCooldownTime(): Long {
        if (!isInCooldown()) return 0

        val lastFailedAttempt = encryptedPrefs.getLong(KEY_LAST_FAILED_ATTEMPT, 0)
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFailedAttempt
        return maxOf(0, COOLDOWN_DURATION_MS - elapsed)
    }

    /**
     * Child Safety Settings
     */
    fun setKidSafeModeEnabled(enabled: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(KEY_KID_SAFE_MODE, enabled)
            .apply()
        _kidSafeModeEnabled.value = enabled
    }

    fun getKidSafeModeEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_KID_SAFE_MODE, true) // Default to true for safety
    }


    fun setDeleteProtectionEnabled(enabled: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(KEY_DELETE_PROTECTION_ENABLED, enabled)
            .apply()
        _deleteProtectionEnabled.value = enabled
    }

    fun getDeleteProtectionEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_DELETE_PROTECTION_ENABLED, true) // Default to true for safety
    }

    /**
     * Biometric Authentication Settings
     */
    fun setBiometricEnabled(enabled: Boolean) {
        encryptedPrefs.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    fun getBiometricEnabled(): Boolean {
        return encryptedPrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false) // Default to false, user must explicitly enable
    }


    /**
     * Clear all parental control settings (for debugging/reset)
     */
    fun clearAllSettings() {
        encryptedPrefs.edit().clear().apply()
        secureStorage.clearKeys()

        // Reset state flows to default values
        _kidSafeModeEnabled.value = true
        _deleteProtectionEnabled.value = true
    }

    /**
     * Get summary of current security settings
     */
    fun getSecuritySummary(): SecuritySummary {
        return SecuritySummary(
            hasPIN = isPINEnabled(),
            hasPattern = isPatternEnabled(),
            biometricEnabled = getBiometricEnabled(),
            kidSafeModeEnabled = getKidSafeModeEnabled(),
            deleteProtectionEnabled = getDeleteProtectionEnabled(),
            failedAttempts = getFailedAttempts(),
            isInCooldown = isInCooldown()
        )
    }
}

/**
 * Data class representing the current security configuration
 */
data class SecuritySummary(
    val hasPIN: Boolean,
    val hasPattern: Boolean,
    val biometricEnabled: Boolean,
    val kidSafeModeEnabled: Boolean,
    val deleteProtectionEnabled: Boolean,
    val failedAttempts: Int,
    val isInCooldown: Boolean
)