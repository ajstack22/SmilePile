package com.smilepile.data.repository

import com.smilepile.data.dao.PhotoDao
import com.smilepile.data.entities.PhotoEntity
import com.smilepile.data.models.Photo
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import java.util.UUID

/**
 * Unit tests for PhotoRepositoryImpl
 * Tests CRUD operations, data flows, error scenarios, and data transformations
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class PhotoRepositoryImplTest {

    private lateinit var photoDao: PhotoDao
    private lateinit var repository: PhotoRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        Dispatchers.setMain(testDispatcher)
        photoDao = mockk(relaxed = true)
        repository = PhotoRepositoryImpl(photoDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // Helper function to create test Photo
    private fun createTestPhoto(
        id: Long = 0L,
        path: String = "content://test/photo/1",
        categoryId: Long = 1L,
        createdAt: Long = System.currentTimeMillis()
    ): Photo {
        return Photo(
            id = id,
            path = path,
            categoryId = categoryId,
            name = path.substringAfterLast("/").substringBeforeLast("."),
            isFromAssets = false,
            createdAt = createdAt,
            fileSize = 1024L,
            width = 1920,
            height = 1080
        )
    }

    // Helper function to create test PhotoEntity
    private fun createTestPhotoEntity(
        id: String = UUID.randomUUID().toString(),
        uri: String = "content://test/photo/1",
        categoryId: Long = 1L,
        timestamp: Long = System.currentTimeMillis()
    ): PhotoEntity {
        return PhotoEntity(
            id = id,
            uri = uri,
            categoryId = categoryId,
            timestamp = timestamp
        )
    }

    // ===== Insert Operations Tests =====

    @Test
    fun `insertPhoto success with valid category`() = runTest {
        // Given
        val photo = createTestPhoto(categoryId = 1L)
        coEvery { photoDao.insert(any()) } returns 1L

        // When
        val result = repository.insertPhoto(photo)

        // Then
        assertEquals(1L, result)
        coVerify { photoDao.insert(any()) }
    }

    @Test
    fun `insertPhoto throws exception for invalid category`() = runTest {
        // Given
        val photo = createTestPhoto(categoryId = 0L)

        // When & Then
        try {
            repository.insertPhoto(photo)
            fail("Expected PhotoRepositoryException")
        } catch (e: PhotoRepositoryException) {
            assertTrue(e.message?.contains("valid category") == true)
        }

        coVerify(exactly = 0) { photoDao.insert(any()) }
    }

    @Test
    fun `insertPhoto handles database exception`() = runTest {
        // Given
        val photo = createTestPhoto(categoryId = 1L)
        coEvery { photoDao.insert(any()) } throws RuntimeException("Database error")

        // When & Then
        try {
            repository.insertPhoto(photo)
            fail("Expected PhotoRepositoryException")
        } catch (e: PhotoRepositoryException) {
            assertTrue(e.message?.contains("Failed to insert photo") == true)
        }
    }

    @Test
    fun `insertPhotos success with valid categories`() = runTest {
        // Given
        val photos = listOf(
            createTestPhoto(categoryId = 1L, path = "content://test/photo/1"),
            createTestPhoto(categoryId = 2L, path = "content://test/photo/2")
        )
        coEvery { photoDao.insertAll(any()) } returns listOf(1L, 2L)

        // When
        repository.insertPhotos(photos)

        // Then
        coVerify { photoDao.insertAll(any()) }
    }

    @Test
    fun `insertPhotos throws exception for photos with invalid categories`() = runTest {
        // Given
        val photos = listOf(
            createTestPhoto(categoryId = 1L),
            createTestPhoto(categoryId = 0L) // Invalid category
        )

        // When & Then
        try {
            repository.insertPhotos(photos)
            fail("Expected PhotoRepositoryException")
        } catch (e: PhotoRepositoryException) {
            assertTrue(e.message?.contains("valid categories") == true)
        }

        coVerify(exactly = 0) { photoDao.insertAll(any()) }
    }

    // ===== Update Operations Tests =====

    @Test
    fun `updatePhoto success`() = runTest {
        // Given
        val photo = createTestPhoto(id = 123L, path = "content://test/photo/1")
        val photoEntity = createTestPhotoEntity(uri = photo.path)
        coEvery { photoDao.getByUri(photo.path) } returns photoEntity
        coEvery { photoDao.update(any()) } returns 1

        // When
        repository.updatePhoto(photo)

        // Then
        coVerify { photoDao.update(any()) }
    }

    @Test
    fun `updatePhoto throws exception when photo not found`() = runTest {
        // Given
        val photo = createTestPhoto(id = 123L)
        coEvery { photoDao.getByUri(photo.path) } returns null
        coEvery { photoDao.update(any()) } returns 0

        // When & Then
        try {
            repository.updatePhoto(photo)
            fail("Expected PhotoRepositoryException")
        } catch (e: PhotoRepositoryException) {
            assertTrue(e.message?.contains("not found for update") == true)
        }
    }

    // ===== Delete Operations Tests =====

    @Test
    fun `deletePhoto success`() = runTest {
        // Given
        val photo = createTestPhoto(id = 123L, path = "content://test/photo/1")
        val photoEntity = createTestPhotoEntity(uri = photo.path)
        coEvery { photoDao.getByUri(photo.path) } returns photoEntity
        coEvery { photoDao.delete(any()) } returns 1

        // When
        repository.deletePhoto(photo)

        // Then
        coVerify { photoDao.delete(any()) }
    }

    @Test
    fun `deletePhoto throws exception when photo not found`() = runTest {
        // Given
        val photo = createTestPhoto(id = 123L)
        coEvery { photoDao.getByUri(photo.path) } returns null
        coEvery { photoDao.delete(any()) } returns 0

        // When & Then
        try {
            repository.deletePhoto(photo)
            fail("Expected PhotoRepositoryException")
        } catch (e: PhotoRepositoryException) {
            assertTrue(e.message?.contains("not found for deletion") == true)
        }
    }

    @Test
    fun `deletePhotoById success`() = runTest {
        // Given
        val uri = "content://test/photo/123"
        val photoEntity = createTestPhotoEntity(uri = uri)

        // Generate the expected ID using the same logic as the repository
        val expectedId = generateStableIdFromUri(uri)

        coEvery { photoDao.getAll() } returns flowOf(listOf(photoEntity))
        coEvery { photoDao.deleteById(photoEntity.id) } returns 1

        // When
        repository.deletePhotoById(expectedId)

        // Then
        coVerify { photoDao.deleteById(photoEntity.id) }
    }

    // Helper function to match repository's ID generation logic
    private fun generateStableIdFromUri(uri: String): Long {
        val hash = uri.hashCode()
        val length = uri.length
        return (kotlin.math.abs(hash).toLong() shl 16) or (length.toLong() and 0xFFFF)
    }

    @Test
    fun `deletePhotoById throws exception when photo not found`() = runTest {
        // Given
        val photoId = 999L
        coEvery { photoDao.getAll() } returns flowOf(emptyList())

        // When & Then
        try {
            repository.deletePhotoById(photoId)
            fail("Expected PhotoRepositoryException")
        } catch (e: PhotoRepositoryException) {
            assertTrue(e.message?.contains("not found for deletion") == true)
        }
    }

    // ===== Query Operations Tests =====

    @Test
    fun `getPhotoById returns photo when found`() = runTest {
        // Given
        val uri = "content://test/photo/123"
        val photoEntity = createTestPhotoEntity(uri = uri)
        val expectedId = generateStableIdFromUri(uri)

        coEvery { photoDao.getAll() } returns flowOf(listOf(photoEntity))

        // When
        val result = repository.getPhotoById(expectedId)

        // Then
        assertNotNull(result)
        assertEquals("123", result?.name)
    }

    @Test
    fun `getPhotoById returns null when not found`() = runTest {
        // Given
        val photoId = 999L
        coEvery { photoDao.getAll() } returns flowOf(emptyList())

        // When
        val result = repository.getPhotoById(photoId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getPhotoByPath returns photo when found`() = runTest {
        // Given
        val path = "content://test/photo/123"
        val photoEntity = createTestPhotoEntity(uri = path)
        coEvery { photoDao.getByUri(path) } returns photoEntity

        // When
        val result = repository.getPhotoByPath(path)

        // Then
        assertNotNull(result)
        assertEquals(path, result?.path)
    }

    @Test
    fun `getPhotoByPath returns null when not found`() = runTest {
        // Given
        val path = "content://test/photo/999"
        coEvery { photoDao.getByUri(path) } returns null

        // When
        val result = repository.getPhotoByPath(path)

        // Then
        assertNull(result)
    }

    @Test
    fun `getPhotosByCategory returns list of photos`() = runTest {
        // Given
        val categoryId = 1L
        val photoEntities = listOf(
            createTestPhotoEntity(uri = "content://test/photo/1", categoryId = categoryId),
            createTestPhotoEntity(uri = "content://test/photo/2", categoryId = categoryId)
        )
        coEvery { photoDao.getByCategory(categoryId) } returns flowOf(photoEntities)

        // When
        val result = repository.getPhotosByCategory(categoryId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.categoryId == categoryId })
    }

    @Test
    fun `getAllPhotos returns all photos`() = runTest {
        // Given
        val photoEntities = listOf(
            createTestPhotoEntity(uri = "content://test/photo/1"),
            createTestPhotoEntity(uri = "content://test/photo/2"),
            createTestPhotoEntity(uri = "content://test/photo/3")
        )
        coEvery { photoDao.getAll() } returns flowOf(photoEntities)

        // When
        val result = repository.getAllPhotos()

        // Then
        assertEquals(3, result.size)
    }

    // ===== Flow Operations Tests =====

    @Test
    fun `getPhotosByCategoryFlow emits photos correctly`() = runTest {
        // Given
        val categoryId = 1L
        val photoEntities = listOf(
            createTestPhotoEntity(uri = "content://test/photo/1", categoryId = categoryId)
        )
        coEvery { photoDao.getByCategory(categoryId) } returns flowOf(photoEntities)

        // When
        val result = repository.getPhotosByCategoryFlow(categoryId).first()

        // Then
        assertEquals(1, result.size)
        assertEquals(categoryId, result[0].categoryId)
    }

    @Test
    fun `getAllPhotosFlow emits all photos`() = runTest {
        // Given
        val photoEntities = listOf(
            createTestPhotoEntity(uri = "content://test/photo/1"),
            createTestPhotoEntity(uri = "content://test/photo/2")
        )
        coEvery { photoDao.getAll() } returns flowOf(photoEntities)

        // When
        val result = repository.getAllPhotosFlow().first()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getPhotosInCategoriesFlow combines multiple categories`() = runTest {
        // Given
        val categoryIds = listOf(1L, 2L)
        val photosCategory1 = listOf(createTestPhotoEntity(uri = "content://test/photo/1", categoryId = 1L))
        val photosCategory2 = listOf(createTestPhotoEntity(uri = "content://test/photo/2", categoryId = 2L))

        coEvery { photoDao.getByCategory(1L) } returns flowOf(photosCategory1)
        coEvery { photoDao.getByCategory(2L) } returns flowOf(photosCategory2)

        // When
        val result = repository.getPhotosInCategoriesFlow(categoryIds).first()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getPhotosInCategoriesFlow returns empty for empty category list`() = runTest {
        // When
        val result = repository.getPhotosInCategoriesFlow(emptyList()).first()

        // Then
        assertTrue(result.isEmpty())
    }

    // ===== Bulk Operations Tests =====

    @Test
    fun `deletePhotosByCategory deletes all photos in category`() = runTest {
        // Given
        val categoryId = 1L
        coEvery { photoDao.deleteByCategory(categoryId) } returns 5

        // When
        repository.deletePhotosByCategory(categoryId)

        // Then
        coVerify { photoDao.deleteByCategory(categoryId) }
    }

    @Test
    fun `getPhotoCount returns correct count`() = runTest {
        // Given
        val photoEntities = List(10) { createTestPhotoEntity(uri = "content://test/photo/$it") }
        coEvery { photoDao.getAll() } returns flowOf(photoEntities)

        // When
        val result = repository.getPhotoCount()

        // Then
        assertEquals(10, result)
    }

    @Test
    fun `getPhotoCategoryCount returns correct count for category`() = runTest {
        // Given
        val categoryId = 1L
        coEvery { photoDao.getPhotoCountByCategory(categoryId) } returns 7

        // When
        val result = repository.getPhotoCategoryCount(categoryId)

        // Then
        assertEquals(7, result)
    }

    // ===== Remove from Library Operations Tests =====

    @Test
    fun `removeFromLibrary removes photo successfully`() = runTest {
        // Given
        val photo = createTestPhoto(id = 123L, path = "content://test/photo/1")
        coEvery { photoDao.deleteByUri(photo.path) } returns 1

        // When
        repository.removeFromLibrary(photo)

        // Then
        coVerify { photoDao.deleteByUri(photo.path) }
    }

    @Test
    fun `removeFromLibrary throws exception when photo not found`() = runTest {
        // Given
        val photo = createTestPhoto(id = 123L, path = "content://test/photo/999")
        coEvery { photoDao.deleteByUri(photo.path) } returns 0

        // When & Then
        try {
            repository.removeFromLibrary(photo)
            fail("Expected PhotoRepositoryException")
        } catch (e: PhotoRepositoryException) {
            assertTrue(e.message?.contains("not found for removal from library") == true)
        }
    }

    @Test
    fun `removeFromLibraryById removes photo successfully`() = runTest {
        // Given
        val uri = "content://test/photo/123"
        val photoEntity = createTestPhotoEntity(uri = uri)
        val expectedId = generateStableIdFromUri(uri)

        coEvery { photoDao.getAll() } returns flowOf(listOf(photoEntity))
        coEvery { photoDao.deleteById(photoEntity.id) } returns 1

        // When
        repository.removeFromLibraryById(expectedId)

        // Then
        coVerify { photoDao.deleteById(photoEntity.id) }
    }

    // ===== Data Transformation Tests =====

    @Test
    fun `photo to entity conversion preserves data`() = runTest {
        // Given
        val photo = createTestPhoto(
            id = 0L,
            path = "content://test/photo/new",
            categoryId = 2L,
            createdAt = 1000L
        )
        coEvery { photoDao.insert(any()) } answers {
            val entity = firstArg<PhotoEntity>()
            assertEquals("content://test/photo/new", entity.uri)
            assertEquals(2L, entity.categoryId)
            assertEquals(1000L, entity.timestamp)
            1L
        }

        // When
        repository.insertPhoto(photo)

        // Then
        coVerify { photoDao.insert(any()) }
    }

    @Test
    fun `entity to photo conversion generates correct ID`() = runTest {
        // Given
        val uri = "content://test/photo/unique"
        val photoEntity = createTestPhotoEntity(uri = uri)
        coEvery { photoDao.getByUri(uri) } returns photoEntity

        // When
        val result1 = repository.getPhotoByPath(uri)
        val result2 = repository.getPhotoByPath(uri)

        // Then
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(result1?.id, result2?.id) // Same URI should generate same ID
    }

    // ===== Error Handling Tests =====

    @Test
    fun `handles database connection error gracefully`() = runTest {
        // Given
        coEvery { photoDao.getAll() } throws RuntimeException("Database connection lost")

        // When & Then
        try {
            repository.getAllPhotos()
            fail("Expected PhotoRepositoryException")
        } catch (e: PhotoRepositoryException) {
            assertTrue(e.message?.contains("Failed to get all photos") == true)
        }
    }

    @Test
    fun `getPhotosInCategories handles empty result`() = runTest {
        // Given
        val categoryIds = listOf(99L, 100L)
        coEvery { photoDao.getByCategory(99L) } returns flowOf(emptyList())
        coEvery { photoDao.getByCategory(100L) } returns flowOf(emptyList())

        // When
        val result = repository.getPhotosInCategories(categoryIds)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPhotosInCategories returns empty list for empty input`() = runTest {
        // When
        val result = repository.getPhotosInCategories(emptyList())

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getPhotosInCategories removes duplicates`() = runTest {
        // Given
        val photo1 = createTestPhotoEntity(id = "id1", uri = "content://test/photo/1")
        val photo2 = createTestPhotoEntity(id = "id2", uri = "content://test/photo/2")

        coEvery { photoDao.getByCategory(1L) } returns flowOf(listOf(photo1, photo2))
        coEvery { photoDao.getByCategory(2L) } returns flowOf(listOf(photo1)) // photo1 appears in both

        // When
        val result = repository.getPhotosInCategories(listOf(1L, 2L))

        // Then
        assertEquals(2, result.size) // Should have 2 unique photos
    }
}