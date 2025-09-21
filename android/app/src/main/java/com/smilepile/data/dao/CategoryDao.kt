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
    suspend fun deleteById(categoryId: String): Int

    /**
     * Get all categories from the database as a reactive Flow
     * @return Flow of list of all categories, ordered by creation time ascending
     */
    @Query("SELECT * FROM category_entities ORDER BY created_at ASC")
    fun getAll(): Flow<List<CategoryEntity>>

    /**
     * Get all categories ordered by name as a reactive Flow
     * @return Flow of list of all categories, ordered by name alphabetically
     */
    @Query("SELECT * FROM category_entities ORDER BY name ASC")
    fun getAllByName(): Flow<List<CategoryEntity>>

    /**
     * Get a category by its ID
     * @param categoryId The ID of the category to retrieve
     * @return The category entity if found, null otherwise
     */
    @Query("SELECT * FROM category_entities WHERE id = :categoryId")
    suspend fun getById(categoryId: String): CategoryEntity?

    /**
     * Get a category by its ID as a reactive Flow
     * @param categoryId The ID of the category to retrieve
     * @return Flow of the category entity
     */
    @Query("SELECT * FROM category_entities WHERE id = :categoryId")
    fun getByIdFlow(categoryId: String): Flow<CategoryEntity?>

    /**
     * Get a category by its name
     * @param name The name of the category to retrieve
     * @return The category entity if found, null otherwise
     */
    @Query("SELECT * FROM category_entities WHERE name = :name COLLATE NOCASE")
    suspend fun getByName(name: String): CategoryEntity?

    /**
     * Get a category by its name as a reactive Flow
     * @param name The name of the category to retrieve
     * @return Flow of the category entity
     */
    @Query("SELECT * FROM category_entities WHERE name = :name COLLATE NOCASE")
    fun getByNameFlow(name: String): Flow<CategoryEntity?>

    /**
     * Check if a category with the given name already exists
     * @param name The name to check
     * @return True if a category with this name exists, false otherwise
     */
    @Query("SELECT COUNT(*) > 0 FROM category_entities WHERE name = :name COLLATE NOCASE")
    suspend fun existsByName(name: String): Boolean

    /**
     * Check if a category with the given name exists (excluding a specific ID)
     * Useful for checking duplicates when updating a category
     * @param name The name to check
     * @param excludeId The ID to exclude from the check
     * @return True if a category with this name exists (excluding the specified ID), false otherwise
     */
    @Query("SELECT COUNT(*) > 0 FROM category_entities WHERE name = :name COLLATE NOCASE AND id != :excludeId")
    suspend fun existsByNameExcludingId(name: String, excludeId: String): Boolean

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
     * Search categories by name containing the given query
     * @param query The search query
     * @return Flow of list of categories matching the search query
     */
    @Query("SELECT * FROM category_entities WHERE name LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<CategoryEntity>>

    /**
     * Update the name of a category
     * @param categoryId The ID of the category to update
     * @param newName The new name for the category
     * @return Number of rows affected
     */
    @Query("UPDATE category_entities SET name = :newName WHERE id = :categoryId")
    suspend fun updateName(categoryId: String, newName: String): Int

    /**
     * Update the color of a category
     * @param categoryId The ID of the category to update
     * @param newColorHex The new color hex value for the category
     * @return Number of rows affected
     */
    @Query("UPDATE category_entities SET color_hex = :newColorHex WHERE id = :categoryId")
    suspend fun updateColor(categoryId: String, newColorHex: String): Int
}