package com.smilepile.security

import kotlinx.coroutines.flow.Flow

/**
 * Interface for secure storage manager used in parental controls and child safety settings
 * Enables dependency injection and testing with fake implementations
 */
interface ISecurePreferencesManager {
    // State Flows for reactive updates
    val kidSafeModeEnabled: Flow<Boolean>
    val deleteProtectionEnabled: Flow<Boolean>

    // PIN Management
    fun setPIN(pin: String)
    fun validatePIN(pin: String): Boolean
    fun isPINEnabled(): Boolean
    fun clearPIN()

    // Pattern Management
    fun setPattern(pattern: List<Int>)
    fun validatePattern(pattern: List<Int>): Boolean
    fun isPatternEnabled(): Boolean
    fun clearPattern()

    // Authentication State
    fun hasParentalLock(): Boolean

    // Failed Attempts Management
    fun recordFailedAttempt()
    fun resetFailedAttempts()
    fun getFailedAttempts(): Int
    fun isInCooldown(): Boolean
    fun getRemainingCooldownTime(): Long

    // Child Safety Settings
    fun setKidSafeModeEnabled(enabled: Boolean)
    fun getKidSafeModeEnabled(): Boolean
    fun setDeleteProtectionEnabled(enabled: Boolean)
    fun getDeleteProtectionEnabled(): Boolean

    // Biometric Authentication Settings
    fun setBiometricEnabled(enabled: Boolean)
    fun getBiometricEnabled(): Boolean

    // Reset/Debug
    fun clearAllSettings()
    fun getSecuritySummary(): SecuritySummary
}