package com.smilepile.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity representing a photo category
 *
 * Optimized for category browsing performance with proper indexing
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["display_order"], name = "idx_category_display_order"),
        Index(value = ["is_active"], name = "idx_category_active"),
        Index(value = ["is_active", "display_order"], name = "idx_category_active_order")
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "cover_image_path")
    val coverImagePath: String? = null,

    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
) {
    companion object {
        /**
         * Create a new category with automatic ordering
         */
        fun create(
            name: String,
            coverImagePath: String? = null,
            displayOrder: Int = 0
        ): Category {
            return Category(
                name = name,
                coverImagePath = coverImagePath,
                displayOrder = displayOrder,
                createdAt = Date(),
                isActive = true
            )
        }
    }

    /**
     * Create a copy with updated cover image
     */
    fun withCoverImage(imagePath: String): Category {
        return copy(coverImagePath = imagePath)
    }

    /**
     * Create a copy with updated display order
     */
    fun withDisplayOrder(order: Int): Category {
        return copy(displayOrder = order)
    }

    /**
     * Create a copy with updated active status
     */
    fun withActiveStatus(active: Boolean): Category {
        return copy(isActive = active)
    }
}