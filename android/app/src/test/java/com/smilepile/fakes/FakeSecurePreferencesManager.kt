package com.smilepile.fakes

import com.smilepile.security.ISecurePreferencesManager
import com.smilepile.security.SecuritySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of ISecurePreferencesManager for testing
 * Uses in-memory storage instead of EncryptedSharedPreferences
 * No encryption - simple string comparison for PINs and patterns
 */
class FakeSecurePreferencesManager : ISecurePreferencesManager {
    private val storage = mutableMapOf<String, String>()
    private var pinEnabled = false
    private var patternEnabled = false
    private var failedAttempts = 0
    private var lastFailedAttemptTime: Long = 0

    private val _kidSafeModeEnabled = MutableStateFlow(true)
    override val kidSafeModeEnabled: Flow<Boolean> = _kidSafeModeEnabled

    private val _deleteProtectionEnabled = MutableStateFlow(true)
    override val deleteProtectionEnabled: Flow<Boolean> = _deleteProtectionEnabled

    // PIN Management
    override fun setPIN(pin: String) {
        storage["pin"] = pin
        pinEnabled = true
    }

    override fun validatePIN(pin: String): Boolean {
        return storage["pin"] == pin
    }

    override fun isPINEnabled(): Boolean {
        return pinEnabled
    }

    override fun clearPIN() {
        storage.remove("pin")
        pinEnabled = false
    }

    // Pattern Management
    override fun setPattern(pattern: List<Int>) {
        storage["pattern"] = pattern.joinToString(",")
        patternEnabled = true
    }

    override fun validatePattern(pattern: List<Int>): Boolean {
        return storage["pattern"] == pattern.joinToString(",")
    }

    override fun isPatternEnabled(): Boolean {
        return patternEnabled
    }

    override fun clearPattern() {
        storage.remove("pattern")
        patternEnabled = false
    }

    // Authentication State
    override fun hasParentalLock(): Boolean {
        return isPINEnabled() || isPatternEnabled()
    }

    // Failed Attempts Management
    override fun recordFailedAttempt() {
        failedAttempts++
        lastFailedAttemptTime = System.currentTimeMillis()
    }

    override fun resetFailedAttempts() {
        failedAttempts = 0
        lastFailedAttemptTime = 0
    }

    override fun getFailedAttempts(): Int {
        return failedAttempts
    }

    override fun isInCooldown(): Boolean {
        if (failedAttempts < 5) return false
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastFailedAttemptTime) < 30000L // 30 seconds
    }

    override fun getRemainingCooldownTime(): Long {
        if (!isInCooldown()) return 0
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFailedAttemptTime
        return maxOf(0, 30000L - elapsed)
    }

    // Child Safety Settings
    override fun setKidSafeModeEnabled(enabled: Boolean) {
        storage["kidSafeMode"] = enabled.toString()
        _kidSafeModeEnabled.value = enabled
    }

    override fun getKidSafeModeEnabled(): Boolean {
        return storage["kidSafeMode"]?.toBoolean() ?: true
    }

    override fun setDeleteProtectionEnabled(enabled: Boolean) {
        storage["deleteProtection"] = enabled.toString()
        _deleteProtectionEnabled.value = enabled
    }

    override fun getDeleteProtectionEnabled(): Boolean {
        return storage["deleteProtection"]?.toBoolean() ?: true
    }

    // Biometric Authentication Settings
    override fun setBiometricEnabled(enabled: Boolean) {
        storage["biometric"] = enabled.toString()
    }

    override fun getBiometricEnabled(): Boolean {
        return storage["biometric"]?.toBoolean() ?: false
    }

    // Reset/Debug
    override fun clearAllSettings() {
        storage.clear()
        pinEnabled = false
        patternEnabled = false
        failedAttempts = 0
        lastFailedAttemptTime = 0
        _kidSafeModeEnabled.value = true
        _deleteProtectionEnabled.value = true
    }

    override fun getSecuritySummary(): SecuritySummary {
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
