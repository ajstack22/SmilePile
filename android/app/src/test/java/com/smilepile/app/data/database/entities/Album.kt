package com.smilepile.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Album entity for testing database performance.
 * Represents an album that can contain multiple photos.
 */
@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverPhotoId: Long? = null,
    val dateCreated: Date,
    val photoCount: Int = 0,
    val isShared: Boolean = false
)