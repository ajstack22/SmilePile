package com.smilepile.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles selective metadata encryption for child-related data
 * while keeping photo URIs unencrypted for MediaStore access
 */
@Singleton
class MetadataEncryption @Inject constructor(
    private val secureStorageManager: SecureStorageManager
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Encrypted metadata for child-related information
     * This data is sensitive and should be encrypted
     */
    @Serializable
    data class EncryptedMetadata(
        val childName: String? = null,
        val childAge: Int? = null,
        val notes: String? = null,
        val tags: List<String> = emptyList(),
        val milestone: String? = null,
        val location: String? = null,
        val customFields: Map<String, String> = emptyMap()
    )

    /**
     * Encrypts sensitive metadata while keeping photo URI accessible
     * @param metadata The sensitive metadata to encrypt
     * @return Base64 encoded encrypted metadata string
     */
    suspend fun encryptMetadata(metadata: EncryptedMetadata): String = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(metadata)
            secureStorageManager.encrypt(jsonString)
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt metadata", e)
        }
    }

    /**
     * Decrypts sensitive metadata
     * @param encryptedData Base64 encoded encrypted metadata string
     * @return Decrypted metadata object
     */
    suspend fun decryptMetadata(encryptedData: String): EncryptedMetadata = withContext(Dispatchers.IO) {
        try {
            val jsonString = secureStorageManager.decrypt(encryptedData)
            json.decodeFromString<EncryptedMetadata>(jsonString)
        } catch (e: Exception) {
            // Return empty metadata if decryption fails to prevent crashes
            EncryptedMetadata()
        }
    }

    /**
     * Encrypts a simple string value (for individual fields)
     * @param value The string to encrypt
     * @return Encrypted string or null if input is null/empty
     */
    suspend fun encryptString(value: String?): String? = withContext(Dispatchers.IO) {
        if (value.isNullOrBlank()) return@withContext null
        try {
            secureStorageManager.encrypt(value)
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt string", e)
        }
    }

    /**
     * Decrypts a simple string value
     * @param encryptedValue The encrypted string
     * @return Decrypted string or null if input is null or decryption fails
     */
    suspend fun decryptString(encryptedValue: String?): String? = withContext(Dispatchers.IO) {
        if (encryptedValue.isNullOrBlank()) return@withContext null
        try {
            secureStorageManager.decrypt(encryptedValue)
        } catch (e: Exception) {
            // Return null if decryption fails instead of crashing
            null
        }
    }

    /**
     * Encrypts a list of tags
     * @param tags List of tags to encrypt
     * @return Encrypted tags string or null if list is empty
     */
    suspend fun encryptTags(tags: List<String>): String? = withContext(Dispatchers.IO) {
        if (tags.isEmpty()) return@withContext null
        try {
            val jsonString = json.encodeToString(tags)
            secureStorageManager.encrypt(jsonString)
        } catch (e: Exception) {
            throw SecurityException("Failed to encrypt tags", e)
        }
    }

    /**
     * Decrypts a list of tags
     * @param encryptedTags Encrypted tags string
     * @return Decrypted list of tags or empty list if decryption fails
     */
    suspend fun decryptTags(encryptedTags: String?): List<String> = withContext(Dispatchers.IO) {
        if (encryptedTags.isNullOrBlank()) return@withContext emptyList()
        try {
            val jsonString = secureStorageManager.decrypt(encryptedTags)
            json.decodeFromString<List<String>>(jsonString)
        } catch (e: Exception) {
            // Return empty list if decryption fails
            emptyList()
        }
    }

    /**
     * Creates encrypted metadata from individual components
     * @param childName Child's name (sensitive)
     * @param childAge Child's age (sensitive)
     * @param notes Additional notes (sensitive)
     * @param tags List of tags (sensitive)
     * @param milestone Milestone information (sensitive)
     * @param location Location information (sensitive)
     * @param customFields Additional custom fields (sensitive)
     * @return Encrypted metadata string
     */
    suspend fun createEncryptedMetadata(
        childName: String? = null,
        childAge: Int? = null,
        notes: String? = null,
        tags: List<String> = emptyList(),
        milestone: String? = null,
        location: String? = null,
        customFields: Map<String, String> = emptyMap()
    ): String {
        val metadata = EncryptedMetadata(
            childName = childName,
            childAge = childAge,
            notes = notes,
            tags = tags,
            milestone = milestone,
            location = location,
            customFields = customFields
        )
        return encryptMetadata(metadata)
    }

    /**
     * Validates if the encryption system is working properly
     * @return true if encryption/decryption test passes
     */
    suspend fun validateEncryption(): Boolean = withContext(Dispatchers.IO) {
        try {
            val testData = EncryptedMetadata(
                childName = "Test Child",
                tags = listOf("test", "validation"),
                notes = "This is a test note"
            )

            val encrypted = encryptMetadata(testData)
            val decrypted = decryptMetadata(encrypted)

            decrypted.childName == testData.childName &&
            decrypted.tags == testData.tags &&
            decrypted.notes == testData.notes
        } catch (e: Exception) {
            false
        }
    }
}