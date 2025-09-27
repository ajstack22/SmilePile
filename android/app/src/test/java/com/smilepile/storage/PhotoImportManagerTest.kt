package com.smilepile.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.smilepile.security.CircuitBreaker
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Unit tests for PhotoImportManager
 */
@RunWith(RobolectricTestRunner::class)
class PhotoImportManagerTest {

    private lateinit var context: Context
    private lateinit var storageManager: StorageManager
    private lateinit var photoImportManager: PhotoImportManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storageManager = mockk(relaxed = true)
        photoImportManager = PhotoImportManager(context, storageManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test import single photo success`() = runBlocking {
        // Given
        val uri = mockk<Uri>()
        val testPhotoData = ByteArray(1024) { it.toByte() }

        mockkStatic("android.graphics.BitmapFactory")
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } returns mockk<Bitmap>(relaxed = true)

        coEvery { context.contentResolver.openInputStream(uri) } returns ByteArrayInputStream(testPhotoData)
        coEvery { context.contentResolver.getType(uri) } returns "image/jpeg"

        // When
        val result = photoImportManager.importPhoto(uri)

        // Then
        assertTrue(result is ImportResult.Success)
        val successResult = result as ImportResult.Success
        assertNotNull(successResult.photoPath)
        assertNotNull(successResult.thumbnailPath)
        assertNotNull(successResult.fileName)
        assertTrue(successResult.fileSize > 0)
    }

    @Test
    fun `test duplicate detection works`() = runBlocking {
        // Given
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        val testPhotoData = ByteArray(1024) { 42 } // Same data for duplicate

        mockkStatic("android.graphics.BitmapFactory")
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } returns mockk<Bitmap>(relaxed = true)

        coEvery { context.contentResolver.openInputStream(uri1) } returns ByteArrayInputStream(testPhotoData)
        coEvery { context.contentResolver.openInputStream(uri2) } returns ByteArrayInputStream(testPhotoData)
        coEvery { context.contentResolver.getType(any()) } returns "image/jpeg"

        // When - Import first photo
        val result1 = photoImportManager.importPhoto(uri1)

        // Then
        assertTrue(result1 is ImportResult.Success)

        // When - Try to import duplicate
        val result2 = photoImportManager.importPhoto(uri2)

        // Then
        assertTrue(result2 is ImportResult.Duplicate)
    }

    @Test
    fun `test batch import with progress tracking`() = runBlocking {
        // Given
        val uris = listOf(
            mockk<Uri>(),
            mockk<Uri>(),
            mockk<Uri>()
        )
        val progressValues = mutableListOf<Float>()

        mockkStatic("android.graphics.BitmapFactory")
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } returns mockk<Bitmap>(relaxed = true)

        uris.forEachIndexed { index, uri ->
            val testData = ByteArray(1024) { (index * 10 + it).toByte() }
            coEvery { context.contentResolver.openInputStream(uri) } returns ByteArrayInputStream(testData)
            coEvery { context.contentResolver.getType(uri) } returns "image/jpeg"
        }

        // When
        val results = photoImportManager.importPhotosWithProgress(
            uris = uris,
            onProgress = { progress -> progressValues.add(progress) }
        ).toList()

        // Then
        assertEquals(3, results.size)
        assertTrue(progressValues.isNotEmpty())
        assertEquals(1.0f, progressValues.last(), 0.01f)
        assertTrue(results.all { it is ImportResult.Success })
    }

    @Test
    fun `test unsupported format rejection`() = runBlocking {
        // Given
        val uri = mockk<Uri>()
        coEvery { context.contentResolver.getType(uri) } returns "image/gif"

        // When
        val result = photoImportManager.importPhoto(uri)

        // Then
        assertTrue(result is ImportResult.Error)
        val error = result as ImportResult.Error
        assertTrue(error.message.contains("Unsupported format"))
    }

    @Test
    fun `test max batch size enforcement`() = runBlocking {
        // Given
        val uris = List(51) { mockk<Uri>() } // Exceeds max of 50
        val results = mutableListOf<ImportResult>()

        // When
        photoImportManager.importPhotosWithProgress(uris) { }.collect {
            results.add(it)
        }

        // Then
        assertEquals(1, results.size)
        assertTrue(results[0] is ImportResult.Error)
        assertTrue((results[0] as ImportResult.Error).message.contains("Cannot import more than"))
    }

    @Test
    fun `test metadata extraction`() {
        // Given
        val extractor = PhotoMetadataExtractor()
        val testExifData = ByteArray(1024) // Mock EXIF data

        // When
        val metadata = ByteArrayInputStream(testExifData).use {
            extractor.extractMetadata(it)
        }

        // Then
        assertNotNull(metadata)
        // Metadata will have default values since we're using mock data
        assertEquals(1, metadata.orientation) // Default orientation
    }

    @Test
    fun `test photo optimization`() {
        // Given
        val optimizer = PhotoOptimizer()
        val testImageData = ByteArray(1024) { it.toByte() }

        mockkStatic("android.graphics.BitmapFactory")
        val mockBitmap = mockk<Bitmap>(relaxed = true) {
            every { width } returns 4000
            every { height } returns 3000
        }
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } returns mockBitmap

        // When
        val result = ByteArrayInputStream(testImageData).use {
            optimizer.optimizePhoto(it, 2048, 90)
        }

        // Then
        assertNotNull(result)
        assertEquals(mockBitmap, result)
    }

    @Test
    fun `test duplicate detector hash calculation`() {
        // Given
        val detector = DuplicateDetector()
        val data1 = ByteArray(100) { 1 }
        val data2 = ByteArray(100) { 2 }
        val data3 = ByteArray(100) { 1 } // Same as data1

        // When
        val hash1 = detector.calculateHash(data1)
        val hash2 = detector.calculateHash(data2)
        val hash3 = detector.calculateHash(data3)

        // Then
        assertEquals(64, hash1.length) // SHA-256 produces 64 hex characters
        assertNotEquals(hash1, hash2)
        assertEquals(hash1, hash3) // Same data produces same hash
    }

    @Test
    fun `test import statistics tracking`() = runBlocking {
        // Given
        val uri = mockk<Uri>()
        val testPhotoData = ByteArray(1024) { it.toByte() }

        mockkStatic("android.graphics.BitmapFactory")
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } returns mockk<Bitmap>(relaxed = true)

        coEvery { context.contentResolver.openInputStream(uri) } returns ByteArrayInputStream(testPhotoData)
        coEvery { context.contentResolver.getType(uri) } returns "image/jpeg"

        // When
        photoImportManager.importPhoto(uri)
        val stats = photoImportManager.getImportStatistics()

        // Then
        assertEquals(1, stats.totalImported)
        assertEquals(0, stats.duplicatesDetected)
        assertEquals("CLOSED", stats.circuitBreakerState)
    }
}