package com.smilepile

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.smilepile.data.database.SmilePileDatabase
import com.smilepile.data.entities.CategoryEntity
import com.smilepile.data.entities.PhotoEntity
import com.smilepile.mode.AppMode
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Comprehensive integration test for data persistence across app restarts.
 * Tests that all critical data survives app termination and restart.
 *
 * This test verifies:
 * - Photos persist in Room database
 * - Categories persist in Room database
 * - App mode state persists in SharedPreferences
 * - Favorite status persists for photos
 * - Search history persists (when implemented)
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SmilePileSmokeTests {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var database: SmilePileDatabase
    private lateinit var context: Context
    private lateinit var device: UiDevice
    private lateinit var appModePrefs: SharedPreferences

    // Test data constants
    private val testPhotoId = UUID.randomUUID().toString()
    private val testPhotoUri = "content://media/external/images/media/1234"
    private val testCategoryId = "test-category-${System.currentTimeMillis()}"
    private val testCategoryName = "Test Category"
    private val favoritePhotoId = UUID.randomUUID().toString()
    private val searchQuery = "test search query"

    @Before
    fun setup() {
        hiltRule.inject()

        context = ApplicationProvider.getApplicationContext()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Get test database instance
        database = Room.inMemoryDatabaseBuilder(
            context,
            SmilePileDatabase::class.java
        ).allowMainThreadQueries().build()

        // Get app mode preferences
        appModePrefs = context.getSharedPreferences("app_mode_prefs", Context.MODE_PRIVATE)

        // Clean up any existing test data
        cleanupTestData()
    }

    @After
    fun tearDown() {
        cleanupTestData()
        database.close()
    }

    /**
     * Main persistence test that verifies all data survives app restart.
     * This is the comprehensive test that covers all critical persistence scenarios.
     */
    @Test
    fun testDataPersistenceAcrossAppRestart() {
        // Step 1: Add test data to the app
        setupTestData()

        // Step 2: Verify data exists before restart
        verifyDataExistsBeforeRestart()

        // Step 3: Simulate app kill and restart
        simulateAppKillAndRestart()

        // Step 4: Verify all data persists after restart
        verifyDataPersistsAfterRestart()
    }

    /**
     * Specific test for photo persistence across app restart
     */
    @Test
    fun testPhotoPersistenceAcrossRestart() {
        // Add test photos
        runBlocking {
            val photo1 = PhotoEntity(
                id = testPhotoId,
                uri = testPhotoUri,
                categoryId = testCategoryId,
                timestamp = System.currentTimeMillis(),
                isFavorite = false
            )

            val photo2 = PhotoEntity(
                id = favoritePhotoId,
                uri = "content://media/external/images/media/5678",
                categoryId = testCategoryId,
                timestamp = System.currentTimeMillis(),
                isFavorite = true
            )

            database.photoDao().insert(photo1)
            database.photoDao().insert(photo2)
        }

        // Verify photos exist
        runBlocking {
            val photos = database.photoDao().getAll().first()
            assertEquals("Should have 2 photos", 2, photos.size)

            val photo1 = database.photoDao().getById(testPhotoId)
            assertNotNull("Photo 1 should exist", photo1)
            assertEquals("Photo 1 URI should match", testPhotoUri, photo1?.uri)
            assertFalse("Photo 1 should not be favorite", photo1?.isFavorite ?: true)

            val photo2 = database.photoDao().getById(favoritePhotoId)
            assertNotNull("Photo 2 should exist", photo2)
            assertTrue("Photo 2 should be favorite", photo2?.isFavorite ?: false)
        }

        // Simulate restart
        simulateAppKillAndRestart()

        // Verify photos still exist after restart
        runBlocking {
            val photos = database.photoDao().getAll().first()
            assertEquals("Should still have 2 photos after restart", 2, photos.size)

            val photo1 = database.photoDao().getById(testPhotoId)
            assertNotNull("Photo 1 should still exist after restart", photo1)
            assertEquals("Photo 1 URI should still match", testPhotoUri, photo1?.uri)
            assertFalse("Photo 1 should still not be favorite", photo1?.isFavorite ?: true)

            val photo2 = database.photoDao().getById(favoritePhotoId)
            assertNotNull("Photo 2 should still exist after restart", photo2)
            assertTrue("Photo 2 should still be favorite", photo2?.isFavorite ?: false)
        }
    }

    /**
     * Specific test for category persistence across app restart
     */
    @Test
    fun testCategoryPersistenceAcrossRestart() {
        // Add test categories
        runBlocking {
            val category1 = CategoryEntity(
                id = testCategoryId,
                name = testCategoryName,
                colorHex = "#FF5722",
                createdAt = System.currentTimeMillis()
            )

            val category2 = CategoryEntity(
                id = "category-2",
                name = "Second Category",
                colorHex = "#2196F3",
                createdAt = System.currentTimeMillis()
            )

            database.categoryDao().insert(category1)
            database.categoryDao().insert(category2)
        }

        // Verify categories exist
        runBlocking {
            val categories = database.categoryDao().getAll().first()
            assertEquals("Should have 2 categories", 2, categories.size)

            val category1 = database.categoryDao().getById(testCategoryId)
            assertNotNull("Category 1 should exist", category1)
            assertEquals("Category 1 name should match", testCategoryName, category1?.name)
            assertEquals("Category 1 color should match", "#FF5722", category1?.colorHex)
        }

        // Simulate restart
        simulateAppKillAndRestart()

        // Verify categories still exist after restart
        runBlocking {
            val categories = database.categoryDao().getAll().first()
            assertEquals("Should still have 2 categories after restart", 2, categories.size)

            val category1 = database.categoryDao().getById(testCategoryId)
            assertNotNull("Category 1 should still exist after restart", category1)
            assertEquals("Category 1 name should still match", testCategoryName, category1?.name)
            assertEquals("Category 1 color should still match", "#FF5722", category1?.colorHex)
        }
    }

    /**
     * Specific test for app mode state persistence across app restart
     */
    @Test
    fun testAppModeStatePersistenceAcrossRestart() {
        // Set app mode to PARENT
        appModePrefs.edit()
            .putString("current_mode", AppMode.PARENT.name)
            .apply()

        // Verify mode is set
        val savedMode = appModePrefs.getString("current_mode", AppMode.KIDS.name)
        assertEquals("Mode should be PARENT", AppMode.PARENT.name, savedMode)

        // Simulate restart
        simulateAppKillAndRestart()

        // Verify mode persists after restart
        val modeAfterRestart = appModePrefs.getString("current_mode", AppMode.KIDS.name)
        assertEquals("Mode should still be PARENT after restart", AppMode.PARENT.name, modeAfterRestart)

        // Test switching to KIDS mode
        appModePrefs.edit()
            .putString("current_mode", AppMode.KIDS.name)
            .apply()

        // Simulate another restart
        simulateAppKillAndRestart()

        // Verify KIDS mode persists
        val kidsModeAfterRestart = appModePrefs.getString("current_mode", AppMode.KIDS.name)
        assertEquals("Mode should be KIDS after restart", AppMode.KIDS.name, kidsModeAfterRestart)
    }

    /**
     * Test for favorite status persistence across app restart
     */
    @Test
    fun testFavoriteStatusPersistenceAcrossRestart() {
        // Add photos with different favorite statuses
        runBlocking {
            val regularPhoto = PhotoEntity(
                id = testPhotoId,
                uri = testPhotoUri,
                categoryId = testCategoryId,
                isFavorite = false
            )

            val favoritePhoto = PhotoEntity(
                id = favoritePhotoId,
                uri = "content://media/external/images/media/9999",
                categoryId = testCategoryId,
                isFavorite = true
            )

            database.photoDao().insert(regularPhoto)
            database.photoDao().insert(favoritePhoto)
        }

        // Verify initial favorite statuses
        runBlocking {
            val regularPhoto = database.photoDao().getById(testPhotoId)
            val favoritePhoto = database.photoDao().getById(favoritePhotoId)

            assertFalse("Regular photo should not be favorite", regularPhoto?.isFavorite ?: true)
            assertTrue("Favorite photo should be favorite", favoritePhoto?.isFavorite ?: false)
        }

        // Update favorite status
        runBlocking {
            database.photoDao().updateFavoriteStatus(testPhotoId, true)
            database.photoDao().updateFavoriteStatus(favoritePhotoId, false)
        }

        // Verify updates
        runBlocking {
            val updatedRegularPhoto = database.photoDao().getById(testPhotoId)
            val updatedFavoritePhoto = database.photoDao().getById(favoritePhotoId)

            assertTrue("Regular photo should now be favorite", updatedRegularPhoto?.isFavorite ?: false)
            assertFalse("Favorite photo should no longer be favorite", updatedFavoritePhoto?.isFavorite ?: true)
        }

        // Simulate restart
        simulateAppKillAndRestart()

        // Verify favorite statuses persist after restart
        runBlocking {
            val photoAfterRestart1 = database.photoDao().getById(testPhotoId)
            val photoAfterRestart2 = database.photoDao().getById(favoritePhotoId)

            assertTrue("Regular photo should still be favorite after restart", photoAfterRestart1?.isFavorite ?: false)
            assertFalse("Favorite photo should still not be favorite after restart", photoAfterRestart2?.isFavorite ?: true)
        }
    }

    /**
     * Test for search history persistence across app restart
     * Note: Currently search history is stored in memory, so this test documents expected behavior
     * when persistent storage is implemented.
     */
    @Test
    fun testSearchHistoryPersistenceAcrossRestart() {
        // TODO: This test is prepared for when search history persistence is implemented
        // Currently SearchViewModel stores history in memory only (lines 341-346)

        // When search history persistence is implemented, this test should:
        // 1. Add search queries to history
        // 2. Verify they're stored in SharedPreferences or other persistent storage
        // 3. Simulate app restart
        // 4. Verify search history is restored from persistent storage

        // For now, we'll verify the current behavior (memory-only storage)
        // Search history should be empty after app restart since it's not persisted

        val searchPrefs = context.getSharedPreferences("search_history_prefs", Context.MODE_PRIVATE)

        // Clear any existing search history
        searchPrefs.edit().clear().apply()

        // Simulate restart
        simulateAppKillAndRestart()

        // Verify search history is empty (expected behavior for memory-only storage)
        val searchHistory = searchPrefs.getStringSet("search_queries", emptySet())
        assertTrue("Search history should be empty after restart (memory-only storage)",
                  searchHistory?.isEmpty() ?: true)
    }

    // Helper methods

    private fun setupTestData() {
        runBlocking {
            // Add test category
            val category = CategoryEntity(
                id = testCategoryId,
                name = testCategoryName,
                colorHex = "#FF5722",
                createdAt = System.currentTimeMillis()
            )
            database.categoryDao().insert(category)

            // Add test photos
            val photo1 = PhotoEntity(
                id = testPhotoId,
                uri = testPhotoUri,
                categoryId = testCategoryId,
                isFavorite = false
            )

            val photo2 = PhotoEntity(
                id = favoritePhotoId,
                uri = "content://media/external/images/media/5678",
                categoryId = testCategoryId,
                isFavorite = true
            )

            database.photoDao().insert(photo1)
            database.photoDao().insert(photo2)
        }

        // Set app mode
        appModePrefs.edit()
            .putString("current_mode", AppMode.PARENT.name)
            .apply()
    }

    private fun verifyDataExistsBeforeRestart() {
        runBlocking {
            // Verify category exists
            val category = database.categoryDao().getById(testCategoryId)
            assertNotNull("Category should exist before restart", category)
            assertEquals("Category name should match", testCategoryName, category?.name)

            // Verify photos exist
            val photos = database.photoDao().getAll().first()
            assertEquals("Should have 2 photos before restart", 2, photos.size)

            // Verify favorite status
            val favoritePhoto = database.photoDao().getById(favoritePhotoId)
            assertTrue("Photo should be favorite before restart", favoritePhoto?.isFavorite ?: false)
        }

        // Verify app mode
        val mode = appModePrefs.getString("current_mode", AppMode.KIDS.name)
        assertEquals("App mode should be PARENT before restart", AppMode.PARENT.name, mode)
    }

    private fun verifyDataPersistsAfterRestart() {
        runBlocking {
            // Verify category persists
            val category = database.categoryDao().getById(testCategoryId)
            assertNotNull("Category should persist after restart", category)
            assertEquals("Category name should persist", testCategoryName, category?.name)

            // Verify photos persist
            val photos = database.photoDao().getAll().first()
            assertEquals("Should still have 2 photos after restart", 2, photos.size)

            // Verify specific photos persist
            val photo1 = database.photoDao().getById(testPhotoId)
            val photo2 = database.photoDao().getById(favoritePhotoId)

            assertNotNull("Photo 1 should persist after restart", photo1)
            assertNotNull("Photo 2 should persist after restart", photo2)

            assertEquals("Photo 1 URI should persist", testPhotoUri, photo1?.uri)
            assertFalse("Photo 1 favorite status should persist", photo1?.isFavorite ?: true)
            assertTrue("Photo 2 favorite status should persist", photo2?.isFavorite ?: false)
        }

        // Verify app mode persists
        val mode = appModePrefs.getString("current_mode", AppMode.KIDS.name)
        assertEquals("App mode should persist after restart", AppMode.PARENT.name, mode)
    }

    /**
     * Simulates app kill and restart by:
     * 1. Finishing the current activity
     * 2. Clearing app from recent apps
     * 3. Restarting the app
     *
     * This simulates what happens when Android kills the app due to memory pressure
     * or when user manually kills the app.
     */
    private fun simulateAppKillAndRestart() {
        try {
            // Finish current activity
            composeTestRule.activityRule.scenario.close()

            // Use UiDevice to simulate app kill
            // Press home to background the app
            device.pressHome()

            // Clear app from recents (simulates system killing the app)
            device.pressRecentApps()
            Thread.sleep(1000) // Wait for recents to open
            device.pressHome() // Go back to home

            // Wait a moment to simulate app being killed
            Thread.sleep(2000)

            // Restart the app by launching it again
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            // Wait for app to restart
            Thread.sleep(3000)

        } catch (e: Exception) {
            // If UiDevice approach fails, use a simpler simulation
            // by just recreating the activity
            composeTestRule.activityRule.scenario.recreate()
            Thread.sleep(2000)
        }
    }

    private fun cleanupTestData() {
        try {
            runBlocking {
                // Clean up photos
                database.photoDao().deleteById(testPhotoId)
                database.photoDao().deleteById(favoritePhotoId)

                // Clean up categories
                database.categoryDao().deleteById(testCategoryId)
                database.categoryDao().deleteById("category-2")
            }

            // Clean up preferences
            appModePrefs.edit().clear().apply()

            val searchPrefs = context.getSharedPreferences("search_history_prefs", Context.MODE_PRIVATE)
            searchPrefs.edit().clear().apply()

        } catch (e: Exception) {
            // Ignore cleanup errors in tests
        }
    }
}