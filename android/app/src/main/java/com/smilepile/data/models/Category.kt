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
                name = "family",
                displayName = "Family",
                position = 0,
                colorHex = "#E91E63",  // Pink for family warmth
                isDefault = true
            ),
            Category(
                id = 2,
                name = "cars",
                displayName = "Cars",
                position = 1,
                colorHex = "#F44336",  // Red for cars/racing
                isDefault = true
            ),
            Category(
                id = 3,
                name = "games",
                displayName = "Games",
                position = 2,
                colorHex = "#9C27B0",  // Purple for games/fun
                isDefault = true
            ),
            Category(
                id = 4,
                name = "sports",
                displayName = "Sports",
                position = 3,
                colorHex = "#4CAF50",  // Green for sports/outdoors
                isDefault = true
            )
        )
    }
}