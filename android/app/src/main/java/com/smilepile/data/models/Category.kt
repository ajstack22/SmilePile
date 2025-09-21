package com.smilepile.data.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "position")
    val position: Int,

    @ColumnInfo(name = "icon_resource")
    val iconResource: String? = null,

    @ColumnInfo(name = "color_hex")
    val colorHex: String? = null,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {

    companion object {
        fun getDefaultCategories(): List<Category> = listOf(
            Category(
                id = 1,
                name = "animals",
                displayName = "Animals",
                position = 0,
                colorHex = "#4CAF50",
                isDefault = true
            ),
            Category(
                id = 2,
                name = "nature",
                displayName = "Nature",
                position = 1,
                colorHex = "#2196F3",
                isDefault = true
            ),
            Category(
                id = 3,
                name = "fun",
                displayName = "Fun",
                position = 2,
                colorHex = "#FF9800",
                isDefault = true
            )
        )
    }
}