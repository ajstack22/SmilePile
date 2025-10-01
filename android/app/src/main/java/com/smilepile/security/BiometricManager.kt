package com.smilepile.security

import android.content.Context
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages biometric authentication for parental controls
 * Provides fingerprint/face unlock with PIN fallback
 * Integrates with SecurePreferencesManager for preference storage
 */
@Singleton
class BiometricManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferencesManager: SecurePreferencesManager
) {
    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    }

    /**
     * Check if biometric authentication is available on the device
     */
    fun isBiometricAvailable(): BiometricAvailability {
        return when (AndroidBiometricManager.from(context).canAuthenticate(AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            AndroidBiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            AndroidBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            AndroidBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            AndroidBiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
            AndroidBiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.UNSUPPORTED
            AndroidBiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricAvailability.UNKNOWN
            else -> BiometricAvailability.UNKNOWN
        }
    }

    /**
     * Check if user has enabled biometric authentication
     */
    fun isBiometricEnabled(): Boolean {
        return securePreferencesManager.getBiometricEnabled() && isBiometricAvailable() == BiometricAvailability.AVAILABLE
    }

    /**
     * Enable or disable biometric authentication
     */
    fun setBiometricEnabled(enabled: Boolean) {
        securePreferencesManager.setBiometricEnabled(enabled)
    }

    /**
     * Authenticate using biometrics
     * Returns BiometricResult indicating success, failure, or cancellation
     */
    suspend fun authenticateWithBiometrics(
        activity: FragmentActivity,
        title: String = "Parental Authentication",
        subtitle: String = "Use your fingerprint or face to unlock parental settings",
        description: String = "This protects your child safety settings"
    ): BiometricResult = suspendCancellableCoroutine { continuation ->

        val executor = ContextCompat.getMainExecutor(context)
        val callback = createAuthenticationCallback(continuation)
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = createPromptInfo(title, subtitle, description)

        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resume(BiometricResult.ERROR)
            }
        }
    }

    private fun createAuthenticationCallback(
        continuation: CancellableContinuation<BiometricResult>
    ): BiometricPrompt.AuthenticationCallback {
        return object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (continuation.isActive) {
                    continuation.resume(mapErrorCodeToResult(errorCode))
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                if (continuation.isActive) {
                    continuation.resume(BiometricResult.SUCCESS)
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Don't complete the continuation here - let user try again
            }
        }
    }

    private fun mapErrorCodeToResult(errorCode: Int): BiometricResult {
        return when (errorCode) {
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_CANCELED -> BiometricResult.USER_CANCELED
            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricResult.LOCKED_OUT
            BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricResult.NO_BIOMETRICS_ENROLLED
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE -> BiometricResult.HARDWARE_UNAVAILABLE
            else -> BiometricResult.ERROR
        }
    }

    private fun createPromptInfo(title: String, subtitle: String, description: String): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText("Use PIN Instead")
            .setAllowedAuthenticators(AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
    }

    /**
     * Check if biometric authentication should be offered as primary method
     * Only if both available and user has enabled it
     */
    fun shouldOfferBiometricFirst(): Boolean {
        return isBiometricEnabled() && securePreferencesManager.hasParentalLock()
    }

    /**
     * Get a user-friendly message for biometric availability status
     */
    fun getBiometricStatusMessage(): String {
        return when (isBiometricAvailable()) {
            BiometricAvailability.AVAILABLE -> "Biometric authentication is available"
            BiometricAvailability.NO_HARDWARE -> "This device doesn't support biometric authentication"
            BiometricAvailability.HARDWARE_UNAVAILABLE -> "Biometric authentication is temporarily unavailable"
            BiometricAvailability.NOT_ENROLLED -> "No fingerprint or face unlock is set up on this device"
            BiometricAvailability.SECURITY_UPDATE_REQUIRED -> "A security update is required for biometric authentication"
            BiometricAvailability.UNSUPPORTED -> "Biometric authentication is not supported"
            BiometricAvailability.UNKNOWN -> "Biometric authentication status unknown"
        }
    }
}

/**
 * Represents the availability status of biometric authentication
 */
enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NOT_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED,
    UNKNOWN
}

/**
 * Represents the result of a biometric authentication attempt
 */
enum class BiometricResult {
    SUCCESS,
    USER_CANCELED,
    LOCKED_OUT,
    NO_BIOMETRICS_ENROLLED,
    HARDWARE_UNAVAILABLE,
    ERROR
}