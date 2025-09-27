package com.smilepile.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for Photo data storage
 * Represents a photo stored in the SmilePile database
 *
 * Note: Photo URI remains unencrypted to maintain compatibility with Android MediaStore
 * and allow photos to remain accessible in the device Gallery app.
 * Only sensitive child-related metadata is encrypted.
 */
@Entity(tableName = "photo_entities")
data class PhotoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    // UNENCRYPTED: Photo URI must remain accessible for MediaStore compatibility
    @ColumnInfo(name = "uri")
    val uri: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    // ENCRYPTED: Child's name (sensitive data)
    @ColumnInfo(name = "encrypted_child_name")
    val encryptedChildName: String? = null,

    // ENCRYPTED: Child's age (sensitive data)
    @ColumnInfo(name = "encrypted_child_age")
    val encryptedChildAge: String? = null,

    // ENCRYPTED: Personal notes about the photo (sensitive data)
    @ColumnInfo(name = "encrypted_notes")
    val encryptedNotes: String? = null,

    // ENCRYPTED: Tags for photo organization (potentially sensitive)
    @ColumnInfo(name = "encrypted_tags")
    val encryptedTags: String? = null,

    // ENCRYPTED: Milestone information (sensitive data)
    @ColumnInfo(name = "encrypted_milestone")
    val encryptedMilestone: String? = null,

    // ENCRYPTED: Location information (sensitive data)
    @ColumnInfo(name = "encrypted_location")
    val encryptedLocation: String? = null,

    // ENCRYPTED: Full metadata blob for complex data (sensitive data)
    @ColumnInfo(name = "encrypted_metadata")
    val encryptedMetadata: String? = null
)