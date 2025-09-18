package com.smilepile.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Category entity for testing database performance.
 * Represents a category that can contain multiple photos.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val colorCode: String? = null,
    val dateCreated: Date,
    val photoCount: Int = 0,
    val isDefault: Boolean = false
)