package com.smilepile.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Join entity for many-to-many relationship between Photos and Categories
 * Allows photos to belong to multiple categories
 */
@Entity(
    tableName = "photo_category_join",
    primaryKeys = ["photo_id", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photo_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["photo_id"]),
        Index(value = ["category_id"]),
        Index(value = ["assigned_at"])
    ]
)
data class PhotoCategoryJoin(
    @ColumnInfo(name = "photo_id")
    val photoId: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "assigned_at")
    val assignedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false // Indicates if this is the primary category for the photo
)