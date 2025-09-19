package com.smilepile.app.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.smilepile.app.models.Photo

/**
 * Room entity representing a photo within a category in the SmilePile app.
 * Photos can be loaded from assets or potentially other sources in the future.
 */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL  // Changed from CASCADE to prevent data loss
        )
    ],
    indices = [
        Index(value = ["categoryId", "position"]),
        Index(value = ["categoryId"]),
        Index(value = ["isDeleted"])  // New index for soft delete queries
    ]
)
data class PhotoEntity(
    @PrimaryKey
    val id: String,
    val path: String,
    val name: String,
    val categoryId: String?,  // Now nullable to support SET_NULL foreign key
    val position: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val isFromAssets: Boolean = true,
    val isDeleted: Boolean = false,  // Soft delete flag
    val deletedAt: Long? = null      // Timestamp when soft deleted
) {
    /**
     * Validates that the photo has required fields
     */
    fun isValid(): Boolean {
        return id.isNotBlank() && path.isNotBlank() && name.isNotBlank() &&
               (!isDeleted || categoryId != null)  // Allow null categoryId only if not deleted
    }

    /**
     * Gets the full asset path for loading from assets folder
     */
    fun getAssetPath(): String {
        return if (isFromAssets && !path.startsWith("sample_images/")) {
            "sample_images/$path"
        } else {
            path
        }
    }

    /**
     * Converts this entity to a domain model
     */
    fun toDomainModel(): Photo {
        return Photo(
            id = id,
            path = path,
            name = name,
            categoryId = categoryId ?: "", // Provide empty string for null categoryId
            position = position,
            dateAdded = dateAdded,
            isFromAssets = isFromAssets
        )
    }

    companion object {
        /**
         * Creates an entity from a domain model
         */
        fun fromDomainModel(photo: Photo): PhotoEntity {
            return PhotoEntity(
                id = photo.id,
                path = photo.path,
                name = photo.name,
                categoryId = photo.categoryId,
                position = photo.position,
                dateAdded = photo.dateAdded,
                isFromAssets = photo.isFromAssets
            )
        }
    }
}