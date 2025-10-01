package com.smilepile.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage manager that uses Android Keystore for encryption
 * Provides secure encryption/decryption with proper key management
 */
@Singleton
class SecureStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "SmilePileSecureKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    init {
        generateOrGetSecretKey()
    }

    /**
     * Generate or retrieve the secret key from Android Keystore
     */
    private fun generateOrGetSecretKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            generateSecretKey()
        }
    }

    /**
     * Generate a new secret key in Android Keystore
     */
    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false) // Set to true if you want to require device authentication
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypt data using Android Keystore
     */
    suspend fun encrypt(data: String): String = withContext(Dispatchers.IO) {
        try {
            val secretKey = generateOrGetSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))

            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encryptedData.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)

            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt data", e)
        }
    }

    /**
     * Decrypt data using Android Keystore
     */
    suspend fun decrypt(encryptedData: String): String = withContext(Dispatchers.IO) {
        try {
            val secretKey = generateOrGetSecretKey()
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)

            // Extract IV and encrypted data
            val iv = ByteArray(GCM_IV_LENGTH)
            val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            System.arraycopy(combined, iv.size, encrypted, 0, encrypted.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)

            val decryptedData = cipher.doFinal(encrypted)
            String(decryptedData, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt data", e)
        }
    }

    /**
     * Generate cryptographically secure salt
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(32) // 256 bits
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Hash password with salt using PBKDF2
     */
    fun hashPasswordWithSalt(password: String, salt: ByteArray, iterations: Int = 10000): String {
        return try {
            val spec = javax.crypto.spec.PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                256 // 256-bit hash
            )
            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = factory.generateSecret(spec).encoded

            // Combine salt and hash for storage
            val combined = ByteArray(salt.size + hash.size)
            System.arraycopy(salt, 0, combined, 0, salt.size)
            System.arraycopy(hash, 0, combined, salt.size, hash.size)

            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            throw SecurityException("Failed to hash password", e)
        }
    }

    /**
     * Verify password against stored hash
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        return try {
            val combined = Base64.decode(storedHash, Base64.DEFAULT)

            // Extract salt and hash directly from combined array
            val salt = combined.copyOfRange(0, 32)
            val hash = combined.copyOfRange(32, combined.size)

            // Hash input password with extracted salt
            val spec = javax.crypto.spec.PBEKeySpec(
                password.toCharArray(),
                salt,
                10000,
                256
            )
            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val inputHash = factory.generateSecret(spec).encoded

            // Compare hashes
            hash.contentEquals(inputHash)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear all keys from Android Keystore (for testing/reset)
     */
    fun clearKeys() {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (e: Exception) {
            // Log error but don't throw - key might not exist
        }
    }

    /**
     * Check if the secure storage is properly initialized
     */
    fun isInitialized(): Boolean {
        return try {
            keyStore.containsAlias(KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }
}