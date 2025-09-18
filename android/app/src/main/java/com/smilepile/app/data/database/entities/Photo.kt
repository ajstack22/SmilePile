package com.smilepile.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Photo entity for testing database performance.
 * Represents a photo in the SmilePile application.
 */
@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val categoryId: Long,
    val albumId: Long? = null,
    val dateCreated: Date,
    val dateTaken: Date? = null,
    val fileSize: Long,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false
)