package com.smilepile.data.models

import com.smilepile.data.entities.PhotoEntity
import com.smilepile.security.MetadataEncryption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class for working with encrypted photo metadata
 * Provides easy access to encrypted/decrypted photo data
 */
data class PhotoMetadata(
    val id: String,
    val uri: String,
    val categoryId: Long,
    val timestamp: Long,

    // Decrypted child data (sensitive)
    val childName: String? = null,
    val childAge: Int? = null,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val milestone: String? = null,
    val location: String? = null,
    val customFields: Map<String, String> = emptyMap()
) {

    companion object {
        /**
         * Creates PhotoMetadata from PhotoEntity by decrypting sensitive data
         * @param entity The PhotoEntity with encrypted data
         * @param metadataEncryption Encryption handler
         * @return PhotoMetadata with decrypted sensitive fields
         */
        suspend fun fromEntity(
            entity: PhotoEntity,
            metadataEncryption: MetadataEncryption
        ): PhotoMetadata = withContext(Dispatchers.IO) {

            // Decrypt individual fields
            val childName = metadataEncryption.decryptString(entity.encryptedChildName)
            val notes = metadataEncryption.decryptString(entity.encryptedNotes)
            val milestone = metadataEncryption.decryptString(entity.encryptedMilestone)
            val location = metadataEncryption.decryptString(entity.encryptedLocation)
            val tags = metadataEncryption.decryptTags(entity.encryptedTags)

            // Decrypt age (stored as encrypted string, convert back to Int)
            val childAge = try {
                metadataEncryption.decryptString(entity.encryptedChildAge)?.toIntOrNull()
            } catch (e: Exception) {
                null
            }

            // If there's a full metadata blob, try to decrypt and merge
            val fullMetadata = try {
                entity.encryptedMetadata?.let {
                    metadataEncryption.decryptMetadata(it)
                }
            } catch (e: Exception) {
                null
            }

            PhotoMetadata(
                id = entity.id,
                uri = entity.uri,
                categoryId = entity.categoryId,
                timestamp = entity.timestamp,
                childName = childName ?: fullMetadata?.childName,
                childAge = childAge ?: fullMetadata?.childAge,
                notes = notes ?: fullMetadata?.notes,
                tags = if (tags.isNotEmpty()) tags else fullMetadata?.tags ?: emptyList(),
                milestone = milestone ?: fullMetadata?.milestone,
                location = location ?: fullMetadata?.location,
                customFields = fullMetadata?.customFields ?: emptyMap()
            )
        }

        /**
         * Creates a PhotoMetadata with just basic (unencrypted) fields
         * Useful when encryption is disabled or data is not sensitive
         */
        fun fromEntityBasic(entity: PhotoEntity): PhotoMetadata {
            return PhotoMetadata(
                id = entity.id,
                uri = entity.uri,
                categoryId = entity.categoryId,
                timestamp = entity.timestamp
            )
        }
    }

    /**
     * Converts PhotoMetadata to PhotoEntity by encrypting sensitive data
     * @param metadataEncryption Encryption handler
     * @return PhotoEntity with encrypted sensitive fields
     */
    suspend fun toEntity(metadataEncryption: MetadataEncryption): PhotoEntity = withContext(Dispatchers.IO) {

        // Encrypt individual fields
        val encryptedChildName = metadataEncryption.encryptString(childName)
        val encryptedChildAge = childAge?.let {
            metadataEncryption.encryptString(it.toString())
        }
        val encryptedNotes = metadataEncryption.encryptString(notes)
        val encryptedTags = metadataEncryption.encryptTags(tags)
        val encryptedMilestone = metadataEncryption.encryptString(milestone)
        val encryptedLocation = metadataEncryption.encryptString(location)

        // Create full metadata blob for complex data
        val encryptedMetadata = if (customFields.isNotEmpty() ||
            childName != null || childAge != null || notes != null ||
            tags.isNotEmpty() || milestone != null || location != null) {

            metadataEncryption.createEncryptedMetadata(
                childName = childName,
                childAge = childAge,
                notes = notes,
                tags = tags,
                milestone = milestone,
                location = location,
                customFields = customFields
            )
        } else null

        PhotoEntity(
            id = id,
            uri = uri,
            categoryId = categoryId,
            timestamp = timestamp,
            encryptedChildName = encryptedChildName,
            encryptedChildAge = encryptedChildAge,
            encryptedNotes = encryptedNotes,
            encryptedTags = encryptedTags,
            encryptedMilestone = encryptedMilestone,
            encryptedLocation = encryptedLocation,
            encryptedMetadata = encryptedMetadata
        )
    }

    /**
     * Converts to PhotoEntity without encryption (for basic photos without sensitive data)
     */
    fun toEntityBasic(): PhotoEntity {
        return PhotoEntity(
            id = id,
            uri = uri,
            categoryId = categoryId,
            timestamp = timestamp
        )
    }

    /**
     * Returns true if this photo has any sensitive metadata that should be encrypted
     */
    fun hasSensitiveData(): Boolean {
        return childName != null ||
               childAge != null ||
               notes != null ||
               tags.isNotEmpty() ||
               milestone != null ||
               location != null ||
               customFields.isNotEmpty()
    }

    /**
     * Creates a copy with updated sensitive data
     */
    fun updateMetadata(
        childName: String? = this.childName,
        childAge: Int? = this.childAge,
        notes: String? = this.notes,
        tags: List<String> = this.tags,
        milestone: String? = this.milestone,
        location: String? = this.location,
        customFields: Map<String, String> = this.customFields
    ): PhotoMetadata {
        return copy(
            childName = childName,
            childAge = childAge,
            notes = notes,
            tags = tags,
            milestone = milestone,
            location = location,
            customFields = customFields
        )
    }
}