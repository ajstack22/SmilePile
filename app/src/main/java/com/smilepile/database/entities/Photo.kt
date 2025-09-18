package com.smilepile.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity representing a photo within a category
 *
 * Optimized for fast photo loading with proper indexing and foreign key constraints
 */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["category_id"], name = "idx_photo_category"),
        Index(value = ["category_id", "display_order"], name = "idx_photo_category_order"),
        Index(value = ["file_path"], name = "idx_photo_file_path", unique = true),
        Index(value = ["display_order"], name = "idx_photo_display_order")
    ]
)
data class Photo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0,

    @ColumnInfo(name = "metadata")
    val metadata: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long? = null,

    @ColumnInfo(name = "width")
    val width: Int? = null,

    @ColumnInfo(name = "height")
    val height: Int? = null,

    @ColumnInfo(name = "is_cover_image")
    val isCoverImage: Boolean = false
) {
    companion object {
        /**
         * Create a new photo with automatic ordering
         */
        fun create(
            categoryId: Long,
            filePath: String,
            displayOrder: Int = 0,
            metadata: String? = null
        ): Photo {
            return Photo(
                categoryId = categoryId,
                filePath = filePath,
                displayOrder = displayOrder,
                metadata = metadata,
                createdAt = Date()
            )
        }
    }

    /**
     * Create a copy with updated display order
     */
    fun withDisplayOrder(order: Int): Photo {
        return copy(displayOrder = order)
    }

    /**
     * Create a copy with updated metadata
     */
    fun withMetadata(newMetadata: String): Photo {
        return copy(metadata = newMetadata)
    }

    /**
     * Create a copy with updated dimensions
     */
    fun withDimensions(width: Int, height: Int): Photo {
        return copy(width = width, height = height)
    }

    /**
     * Create a copy marked as cover image
     */
    fun asCoverImage(): Photo {
        return copy(isCoverImage = true)
    }

    /**
     * Check if this is a valid image file
     */
    fun isValidImageFile(): Boolean {
        val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return extension in imageExtensions
    }

    /**
     * Get file name without path
     */
    fun getFileName(): String {
        return filePath.substringAfterLast('/')
    }
}