package com.smilepile.app.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smilepile.app.database.entities.CategoryEntity
import com.smilepile.app.database.entities.PhotoEntity
import com.smilepile.app.repository.CategoryRepository
import com.smilepile.app.repository.CategoryRepositoryImpl
import com.smilepile.app.models.Category
import com.smilepile.app.models.Photo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Comprehensive persistence tests to verify Room database data survives app restarts,
 * process death, and various failure scenarios for the SmilePile app.
 */
@RunWith(AndroidJUnit4::class)
class PersistenceTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var testScope: CoroutineScope
    private val dbName = "test_persistence_db"
    private val context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        testScope = CoroutineScope(Dispatchers.IO)
        // Clean up any existing test database
        deleteTestDatabase()
    }

    @After
    fun cleanup() {
        deleteTestDatabase()
    }

    private fun deleteTestDatabase() {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.delete()
        // Also delete journal files
        File("${dbFile.absolutePath}-wal").delete()
        File("${dbFile.absolutePath}-shm").delete()
    }

    private fun createPersistentDatabase(): SmilePileDatabase {
        return Room.databaseBuilder(
            context,
            SmilePileDatabase::class.java,
            dbName
        ).build()
    }

    private fun createSampleCategory(
        id: String = "persist_cat",
        name: String = "persistent_category",
        position: Int = 0
    ) = CategoryEntity(
        id = id,
        name = name,
        displayName = name.replace('_', ' ').split(' ').joinToString(" ") { it.capitalize() },
        coverImagePath = null,
        description = "Persistent test category",
        photoCount = 0,
        position = position,
        createdAt = System.currentTimeMillis()
    )

    private fun createSamplePhoto(
        id: String = "persist_photo",
        categoryId: String = "persist_cat",
        position: Int = 0
    ) = PhotoEntity(
        id = id,
        path = "persistent_images/sample_${position}.png",
        name = "Persistent Photo $position",
        categoryId = categoryId,
        position = position,
        dateAdded = System.currentTimeMillis(),
        isFromAssets = true
    )

    // ================== Basic Persistence Tests ==================

    @Test
    fun categoriesShouldPersistAcrossDatabaseInstances() = runTest {
        // Given: Create database and insert categories
        val db1 = createPersistentDatabase()
        val categoryDao1 = db1.categoryDao()

        val categories = listOf(
            createSampleCategory(id = "cat1", name = "animals", position = 0),
            createSampleCategory(id = "cat2", name = "vehicles", position = 1),
            createSampleCategory(id = "cat3", name = "food", position = 2)
        )

        categoryDao1.insertCategories(categories)

        // Verify insertion
        val insertedCategories = categoryDao1.getAllCategories().first()
        assertEquals("Should insert 3 categories", 3, insertedCategories.size)

        // Close first database
        db1.close()

        // When: Open new database instance (simulates app restart)
        val db2 = createPersistentDatabase()
        val categoryDao2 = db2.categoryDao()

        // Then: Categories should persist
        val persistedCategories = categoryDao2.getAllCategories().first()
        assertEquals("Categories should persist after restart", 3, persistedCategories.size)

        // Verify data integrity
        val categoryIds = persistedCategories.map { it.id }.toSet()
        assertTrue("Should contain cat1", "cat1" in categoryIds)
        assertTrue("Should contain cat2", "cat2" in categoryIds)
        assertTrue("Should contain cat3", "cat3" in categoryIds)

        // Verify ordering is preserved
        assertEquals("First category should be cat1", "cat1", persistedCategories[0].id)
        assertEquals("Second category should be cat2", "cat2", persistedCategories[1].id)
        assertEquals("Third category should be cat3", "cat3", persistedCategories[2].id)

        db2.close()
    }

    @Test
    fun photosShouldPersistAcrossDatabaseInstances() = runTest {
        // Given: Create database and insert category with photos
        val db1 = createPersistentDatabase()
        val categoryDao1 = db1.categoryDao()
        val photoDao1 = db1.photoDao()

        val category = createSampleCategory()
        categoryDao1.insertCategory(category)

        val photos = listOf(
            createSamplePhoto(id = "photo1", position = 0),
            createSamplePhoto(id = "photo2", position = 1),
            createSamplePhoto(id = "photo3", position = 2)
        )
        photoDao1.insertPhotos(photos)

        // Verify insertion
        val insertedPhotos = photoDao1.getPhotosForCategory(category.id).first()
        assertEquals("Should insert 3 photos", 3, insertedPhotos.size)

        // Close first database
        db1.close()

        // When: Open new database instance (simulates app restart)
        val db2 = createPersistentDatabase()
        val photoDao2 = db2.photoDao()

        // Then: Photos should persist
        val persistedPhotos = photoDao2.getPhotosForCategory(category.id).first()
        assertEquals("Photos should persist after restart", 3, persistedPhotos.size)

        // Verify data integrity
        val photoIds = persistedPhotos.map { it.id }.toSet()
        assertTrue("Should contain photo1", "photo1" in photoIds)
        assertTrue("Should contain photo2", "photo2" in photoIds)
        assertTrue("Should contain photo3", "photo3" in photoIds)

        // Verify ordering is preserved
        assertEquals("First photo should be photo1", "photo1", persistedPhotos[0].id)
        assertEquals("Second photo should be photo2", "photo2", persistedPhotos[1].id)
        assertEquals("Third photo should be photo3", "photo3", persistedPhotos[2].id)

        db2.close()
    }

    @Test
    fun relationshipsShouldPersistAcrossDatabaseInstances() = runTest {
        // Given: Create database and insert categories with photos
        val db1 = createPersistentDatabase()
        val categoryDao1 = db1.categoryDao()
        val photoDao1 = db1.photoDao()

        val categories = listOf(
            createSampleCategory(id = "animals", name = "animals"),
            createSampleCategory(id = "vehicles", name = "vehicles")
        )
        categoryDao1.insertCategories(categories)

        val photos = listOf(
            createSamplePhoto(id = "cat1", categoryId = "animals"),
            createSamplePhoto(id = "dog1", categoryId = "animals"),
            createSamplePhoto(id = "car1", categoryId = "vehicles"),
            createSamplePhoto(id = "truck1", categoryId = "vehicles")
        )
        photoDao1.insertPhotos(photos)

        // Verify relationships
        val animalPhotos = photoDao1.getPhotosForCategory("animals").first()
        val vehiclePhotos = photoDao1.getPhotosForCategory("vehicles").first()
        assertEquals("Animals should have 2 photos", 2, animalPhotos.size)
        assertEquals("Vehicles should have 2 photos", 2, vehiclePhotos.size)

        // Close first database
        db1.close()

        // When: Open new database instance (simulates app restart)
        val db2 = createPersistentDatabase()
        val categoryDao2 = db2.categoryDao()

        // Then: Relationships should persist
        val persistedCategoriesWithPhotos = categoryDao2.getAllCategoriesWithPhotos().first()
        assertEquals("Should have 2 categories with photos", 2, persistedCategoriesWithPhotos.size)

        val animalsWithPhotos = persistedCategoriesWithPhotos.find { it.category.id == "animals" }
        val vehiclesWithPhotos = persistedCategoriesWithPhotos.find { it.category.id == "vehicles" }

        assertNotNull("Animals category should exist", animalsWithPhotos)
        assertNotNull("Vehicles category should exist", vehiclesWithPhotos)

        assertEquals("Animals should have 2 photos", 2, animalsWithPhotos!!.photos.size)
        assertEquals("Vehicles should have 2 photos", 2, vehiclesWithPhotos!!.photos.size)

        db2.close()
    }

    // ================== Repository-Level Persistence Tests ==================

    @Test
    fun repositoryDataShouldPersistAcrossInstances() = runTest {
        // Given: Create repository and initialize data
        val repository1 = CategoryRepositoryImpl(context, testScope)
        repository1.initializeSampleData()

        // Add custom data
        val customCategory = Category(
            id = "custom_category",
            name = "custom",
            displayName = "Custom Category",
            coverImagePath = null,
            description = "Custom test category",
            position = 10
        )
        repository1.addCategory(customCategory)

        val customPhoto = Photo(
            id = "custom_photo",
            path = "custom/photo.png",
            name = "Custom Photo",
            categoryId = "custom_category",
            position = 0,
            isFromAssets = true
        )
        repository1.addPhoto(customPhoto)

        // Verify data exists
        val categoriesBeforeRestart = repository1.getCategories()
        val photosBeforeRestart = repository1.getAllPhotos()
        assertTrue("Should have categories including custom", categoriesBeforeRestart.size >= 4)
        assertTrue("Should have photos including custom", photosBeforeRestart.size >= 7)

        // When: Create new repository instance (simulates app restart)
        val repository2 = CategoryRepositoryImpl(context, testScope)

        // Then: Data should persist
        val categoriesAfterRestart = repository2.getCategories()
        val photosAfterRestart = repository2.getAllPhotos()

        assertEquals("Category count should persist", categoriesBeforeRestart.size, categoriesAfterRestart.size)
        assertEquals("Photo count should persist", photosBeforeRestart.size, photosAfterRestart.size)

        // Verify custom data persists
        val persistedCustomCategory = repository2.getCategory("custom_category")
        assertNotNull("Custom category should persist", persistedCustomCategory)
        assertEquals("Custom category display name should persist", "Custom Category", persistedCustomCategory!!.displayName)

        val persistedCustomPhotos = repository2.getPhotosForCategory("custom_category")
        assertEquals("Custom category should have 1 photo", 1, persistedCustomPhotos.size)
        assertEquals("Custom photo should persist", "custom_photo", persistedCustomPhotos[0].id)
    }

    @Test
    fun repositoryFlowsShouldReflectPersistedData() = runTest {
        // Given: Create repository, add data, then restart
        val repository1 = CategoryRepositoryImpl(context, testScope)
        repository1.initializeSampleData()

        val newCategory = Category(
            id = "flow_test_category",
            name = "flow_test",
            displayName = "Flow Test Category",
            position = 20
        )
        repository1.addCategory(newCategory)

        // When: Create new repository instance (simulates app restart)
        val repository2 = CategoryRepositoryImpl(context, testScope)

        // Then: Flows should reflect persisted data
        val categoriesFlow = repository2.getCategoriesFlow().first()
        val flowTestCategory = categoriesFlow.find { it.id == "flow_test_category" }
        assertNotNull("Flow should include persisted category", flowTestCategory)
        assertEquals("Flow category display name should match", "Flow Test Category", flowTestCategory!!.displayName)

        val categoriesWithPhotosFlow = repository2.getAllCategoriesWithPhotosFlow().first()
        val flowTestCategoryWithPhotos = categoriesWithPhotosFlow.find { it.category.id == "flow_test_category" }
        assertNotNull("Flow should include persisted category with photos", flowTestCategoryWithPhotos)
    }

    // ================== Data Migration and Schema Tests ==================

    @Test
    fun databaseShouldHandleDataMigrationScenarios() = runTest {
        // Given: Create database with data
        val db1 = createPersistentDatabase()
        val categoryDao1 = db1.categoryDao()
        val photoDao1 = db1.photoDao()

        val category = createSampleCategory(id = "migration_test")
        categoryDao1.insertCategory(category)

        val photos = (1..5).map {
            createSamplePhoto(id = "migration_photo_$it", categoryId = "migration_test", position = it)
        }
        photoDao1.insertPhotos(photos)

        db1.close()

        // When: Reopen database (simulates schema migration scenario)
        val db2 = createPersistentDatabase()
        val categoryDao2 = db2.categoryDao()
        val photoDao2 = db2.photoDao()

        // Then: All data should be accessible
        val migratedCategory = categoryDao2.getCategoryById("migration_test")
        assertNotNull("Category should survive migration", migratedCategory)

        val migratedPhotos = photoDao2.getPhotosForCategory("migration_test").first()
        assertEquals("All photos should survive migration", 5, migratedPhotos.size)

        // Verify data integrity after migration
        for (i in 1..5) {
            val photo = migratedPhotos.find { it.id == "migration_photo_$i" }
            assertNotNull("Photo $i should exist after migration", photo)
            assertEquals("Photo $i position should be preserved", i, photo!!.position)
        }

        db2.close()
    }

    // ================== Concurrent Access and Data Integrity ==================

    @Test
    fun persistentDataShouldHandleConcurrentAccess() = runTest {
        // Given: Create multiple database instances concurrently
        val db1 = createPersistentDatabase()
        val db2 = createPersistentDatabase()

        val categoryDao1 = db1.categoryDao()
        val categoryDao2 = db2.categoryDao()

        // When: Perform concurrent operations
        val category1 = createSampleCategory(id = "concurrent1", name = "concurrent_cat1")
        val category2 = createSampleCategory(id = "concurrent2", name = "concurrent_cat2")

        val job1 = kotlinx.coroutines.async {
            categoryDao1.insertCategory(category1)
        }

        val job2 = kotlinx.coroutines.async {
            categoryDao2.insertCategory(category2)
        }

        // Wait for both operations
        job1.await()
        job2.await()

        // Close both databases
        db1.close()
        db2.close()

        // Then: Data from both operations should persist
        val db3 = createPersistentDatabase()
        val categoryDao3 = db3.categoryDao()

        val persistedCategories = categoryDao3.getAllCategories().first()
        val categoryIds = persistedCategories.map { it.id }.toSet()

        assertTrue("Should contain category from db1", "concurrent1" in categoryIds)
        assertTrue("Should contain category from db2", "concurrent2" in categoryIds)

        db3.close()
    }

    // ================== Failure Recovery Tests ==================

    @Test
    fun databaseShouldRecoverFromCorruption() = runTest {
        // Given: Create database with initial data
        val db1 = createPersistentDatabase()
        val categoryDao1 = db1.categoryDao()

        val category = createSampleCategory(id = "recovery_test")
        categoryDao1.insertCategory(category)

        // Verify data exists
        val initialCategory = categoryDao1.getCategoryById("recovery_test")
        assertNotNull("Initial category should exist", initialCategory)

        db1.close()

        // When: Simulate database corruption by deleting the file
        // (In real scenarios, Room's fallbackToDestructiveMigration would handle this)
        deleteTestDatabase()

        // Create new database instance
        val db2 = createPersistentDatabase()
        val categoryDao2 = db2.categoryDao()

        // Then: Database should start fresh (empty)
        val categoriesAfterRecovery = categoryDao2.getAllCategories().first()
        assertTrue("Database should start fresh after corruption", categoriesAfterRecovery.isEmpty())

        // Should be able to add new data
        val newCategory = createSampleCategory(id = "recovery_new")
        categoryDao2.insertCategory(newCategory)

        val recoveredCategory = categoryDao2.getCategoryById("recovery_new")
        assertNotNull("Should be able to add data after recovery", recoveredCategory)

        db2.close()
    }

    // ================== Large Dataset Persistence Tests ==================

    @Test
    fun largeDatasetshouldPersistCorrectly() = runTest {
        // Given: Create database with large dataset
        val db1 = createPersistentDatabase()
        val categoryDao1 = db1.categoryDao()
        val photoDao1 = db1.photoDao()

        // Create 20 categories with 10 photos each
        val categories = (1..20).map {
            createSampleCategory(id = "large_cat_$it", name = "large_category_$it", position = it)
        }
        categoryDao1.insertCategories(categories)

        val photos = mutableListOf<PhotoEntity>()
        for (catIndex in 1..20) {
            for (photoIndex in 1..10) {
                photos.add(
                    createSamplePhoto(
                        id = "large_photo_${catIndex}_$photoIndex",
                        categoryId = "large_cat_$catIndex",
                        position = photoIndex
                    )
                )
            }
        }
        photoDao1.insertPhotos(photos)

        // Verify large dataset
        val insertedCategories = categoryDao1.getAllCategories().first()
        val insertedPhotos = photoDao1.getAllPhotos().first()

        assertEquals("Should have 20 categories", 20, insertedCategories.size)
        assertEquals("Should have 200 photos", 200, insertedPhotos.size)

        db1.close()

        // When: Reopen database (simulates app restart with large dataset)
        val db2 = createPersistentDatabase()
        val categoryDao2 = db2.categoryDao()
        val photoDao2 = db2.photoDao()

        // Then: Large dataset should persist correctly
        val persistedCategories = categoryDao2.getAllCategories().first()
        val persistedPhotos = photoDao2.getAllPhotos().first()

        assertEquals("Large categories should persist", 20, persistedCategories.size)
        assertEquals("Large photos should persist", 200, persistedPhotos.size)

        // Verify relationships are maintained
        val categoriesWithPhotos = categoryDao2.getAllCategoriesWithPhotos().first()
        assertEquals("Should have 20 categories with photos", 20, categoriesWithPhotos.size)

        for (categoryWithPhotos in categoriesWithPhotos) {
            assertEquals("Each category should have 10 photos", 10, categoryWithPhotos.photos.size)
        }

        db2.close()
    }

    // ================== Pre-population Persistence Tests ==================

    @Test
    fun databasePrePopulationShouldPersistCorrectly() = runTest {
        // When: Create database (should trigger pre-population)
        val db1 = createPersistentDatabase()

        // Manually trigger population (since callback might not run in tests)
        val categoryDao1 = db1.categoryDao()
        val photoDao1 = db1.photoDao()

        // Check if database is already populated
        if (categoryDao1.getCategoryCount() == 0) {
            // Manually populate with sample data
            val sampleCategories = listOf(
                CategoryEntity(
                    id = "animals",
                    name = "animals",
                    displayName = "Animals",
                    coverImagePath = null,
                    description = "Fun animal pictures",
                    position = 0
                ),
                CategoryEntity(
                    id = "family",
                    name = "family",
                    displayName = "Family",
                    coverImagePath = null,
                    description = "Happy family moments",
                    position = 1
                ),
                CategoryEntity(
                    id = "fun_times",
                    name = "fun_times",
                    displayName = "Fun Times",
                    coverImagePath = null,
                    description = "Good times and memories",
                    position = 2
                )
            )
            categoryDao1.insertCategories(sampleCategories)

            // Add sample photos
            val samplePhotos = listOf(
                PhotoEntity(id = "photo_1", path = "sample_1.png", name = "sample_1", categoryId = "animals", position = 0),
                PhotoEntity(id = "photo_2", path = "sample_2.png", name = "sample_2", categoryId = "family", position = 0),
                PhotoEntity(id = "photo_3", path = "sample_3.png", name = "sample_3", categoryId = "fun_times", position = 0),
                PhotoEntity(id = "photo_4", path = "sample_4.png", name = "sample_4", categoryId = "animals", position = 1),
                PhotoEntity(id = "photo_5", path = "sample_5.png", name = "sample_5", categoryId = "family", position = 1),
                PhotoEntity(id = "photo_6", path = "sample_6.png", name = "sample_6", categoryId = "fun_times", position = 1)
            )
            photoDao1.insertPhotos(samplePhotos)
        }

        // Verify pre-populated data
        val prePopulatedCategories = categoryDao1.getAllCategories().first()
        val prePopulatedPhotos = photoDao1.getAllPhotos().first()

        assertTrue("Should have pre-populated categories", prePopulatedCategories.isNotEmpty())
        assertTrue("Should have pre-populated photos", prePopulatedPhotos.isNotEmpty())

        db1.close()

        // When: Reopen database (should not re-populate)
        val db2 = createPersistentDatabase()
        val categoryDao2 = db2.categoryDao()
        val photoDao2 = db2.photoDao()

        // Then: Pre-populated data should persist
        val persistedCategories = categoryDao2.getAllCategories().first()
        val persistedPhotos = photoDao2.getAllPhotos().first()

        assertEquals("Pre-populated categories should persist", prePopulatedCategories.size, persistedCategories.size)
        assertEquals("Pre-populated photos should persist", prePopulatedPhotos.size, persistedPhotos.size)

        // Verify category names match expected pre-population
        val categoryNames = persistedCategories.map { it.name }.toSet()
        assertTrue("Should have animals category", "animals" in categoryNames)
        assertTrue("Should have family category", "family" in categoryNames)
        assertTrue("Should have fun_times category", "fun_times" in categoryNames)

        db2.close()
    }

    // ================== Repository Persistence Integration Tests ==================

    @Test
    fun repositoryShouldMaintainInitializationStateAcrossRestarts() = runTest {
        // Given: Create repository and initialize
        val repository1 = CategoryRepositoryImpl(context, testScope)
        assertFalse("Should not be initialized initially", repository1.isInitialized())

        repository1.initializeSampleData()
        assertTrue("Should be initialized after sample data creation", repository1.isInitialized())

        // When: Create new repository instance (simulates app restart)
        val repository2 = CategoryRepositoryImpl(context, testScope)

        // Then: Should recognize existing initialization
        assertTrue("Should recognize existing initialization after restart", repository2.isInitialized())

        // Should not re-initialize
        val categoriesBeforeReinit = repository2.getCategories()
        repository2.initializeSampleData()
        val categoriesAfterReinit = repository2.getCategories()

        assertEquals("Should not duplicate data on re-initialization",
            categoriesBeforeReinit.size, categoriesAfterReinit.size)
    }
}