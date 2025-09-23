package com.smilepile.data.dao

import androidx.room.*
import com.smilepile.data.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Category operations
 * Provides CRUD operations for CategoryEntity with Room database
 */
@Dao
interface CategoryDao {

    /**
     * Insert a new category into the database
     * @param category The category entity to insert
     * @return The row ID of the inserted category
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    /**
     * Insert multiple categories into the database
     * @param categories List of category entities to insert
     * @return List of row IDs of the inserted categories
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

    /**
     * Update an existing category in the database
     * @param category The category entity to update
     * @return Number of rows affected
     */
    @Update
    suspend fun update(category: CategoryEntity): Int

    /**
     * Delete a category from the database
     * @param category The category entity to delete
     * @return Number of rows affected
     */
    @Delete
    suspend fun delete(category: CategoryEntity): Int

    /**
     * Delete a category by its ID
     * @param categoryId The ID of the category to delete
     * @return Number of rows affected
     */
    @Query("DELETE FROM category_entities WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long): Int

    /**
     * Get all categories from the database as a reactive Flow
     * @return Flow of list of all categories, ordered by creation time ascending
     */
    @Query("SELECT * FROM category_entities ORDER BY created_at ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    /**
     * Get all categories ordered by display name as a reactive Flow
     * @return Flow of list of all categories, ordered by display name alphabetically
     */
    @Query("SELECT * FROM category_entities ORDER BY display_name ASC")
    fun getAllByDisplayName(): Flow<List<CategoryEntity>>

    /**
     * Get a category by its ID
     * @param categoryId The ID of the category to retrieve
     * @return The category entity if found, null otherwise
     */
    @Query("SELECT * FROM category_entities WHERE id = :categoryId")
    suspend fun getById(categoryId: Long): CategoryEntity?

    /**
     * Get a category by its ID as a reactive Flow
     * @param categoryId The ID of the category to retrieve
     * @return Flow of the category entity
     */
    @Query("SELECT * FROM category_entities WHERE id = :categoryId")
    fun getByIdFlow(categoryId: Long): Flow<CategoryEntity?>

    /**
     * Get a category by its display name
     * @param displayName The display name of the category to retrieve
     * @return The category entity if found, null otherwise
     */
    @Query("SELECT * FROM category_entities WHERE display_name = :displayName COLLATE NOCASE")
    suspend fun getByDisplayName(displayName: String): CategoryEntity?

    /**
     * Get a category by its display name as a reactive Flow
     * @param displayName The display name of the category to retrieve
     * @return Flow of the category entity
     */
    @Query("SELECT * FROM category_entities WHERE display_name = :displayName COLLATE NOCASE")
    fun getByDisplayNameFlow(displayName: String): Flow<CategoryEntity?>

    /**
     * Check if a category with the given display name already exists
     * @param displayName The display name to check
     * @return True if a category with this display name exists, false otherwise
     */
    @Query("SELECT COUNT(*) > 0 FROM category_entities WHERE display_name = :displayName COLLATE NOCASE")
    suspend fun existsByDisplayName(displayName: String): Boolean

    /**
     * Check if a category with the given display name exists (excluding a specific ID)
     * Useful for checking duplicates when updating a category
     * @param displayName The display name to check
     * @param excludeId The ID to exclude from the check
     * @return True if a category with this display name exists (excluding the specified ID), false otherwise
     */
    @Query("SELECT COUNT(*) > 0 FROM category_entities WHERE display_name = :displayName COLLATE NOCASE AND id != :excludeId")
    suspend fun existsByDisplayNameExcludingId(displayName: String, excludeId: Long): Boolean

    /**
     * Get the total count of categories
     * @return The total number of categories
     */
    @Query("SELECT COUNT(*) FROM category_entities")
    suspend fun getCount(): Int

    /**
     * Get the total count of categories as a reactive Flow
     * @return Flow of the total number of categories
     */
    @Query("SELECT COUNT(*) FROM category_entities")
    fun getCountFlow(): Flow<Int>

    /**
     * Search categories by display name containing the given query
     * @param query The search query
     * @return Flow of list of categories matching the search query
     */
    @Query("SELECT * FROM category_entities WHERE display_name LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY display_name ASC")
    fun searchByDisplayName(query: String): Flow<List<CategoryEntity>>

    /**
     * Update the display name of a category
     * @param categoryId The ID of the category to update
     * @param newDisplayName The new display name for the category
     * @return Number of rows affected
     */
    @Query("UPDATE category_entities SET display_name = :newDisplayName WHERE id = :categoryId")
    suspend fun updateDisplayName(categoryId: Long, newDisplayName: String): Int

    /**
     * Update the color of a category
     * @param categoryId The ID of the category to update
     * @param newColorHex The new color hex value for the category
     * @return Number of rows affected
     */
    @Query("UPDATE category_entities SET color_hex = :newColorHex WHERE id = :categoryId")
    suspend fun updateColor(categoryId: Long, newColorHex: String): Int
}