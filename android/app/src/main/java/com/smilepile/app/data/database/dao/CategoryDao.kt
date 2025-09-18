package com.smilepile.app.data.database.dao

import androidx.room.*
import com.smilepile.app.data.database.entities.Category
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Category entity.
 * Contains all database queries for categories used in performance testing.
 */
@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE name = :name")
    suspend fun getCategoryByName(name: String): Category?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Insert
    suspend fun insertCategories(categories: List<Category>): List<Long>

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("UPDATE categories SET photoCount = :count WHERE id = :categoryId")
    suspend fun updatePhotoCount(categoryId: Long, count: Int)

    // Flow-based queries for reactive UI
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    // Performance testing queries
    @Query("SELECT * FROM categories WHERE photoCount > :minPhotoCount")
    suspend fun getCategoriesWithMinPhotos(minPhotoCount: Int): List<Category>

    @Query("SELECT * FROM categories ORDER BY photoCount DESC LIMIT :limit")
    suspend fun getTopCategoriesByPhotoCount(limit: Int): List<Category>
}