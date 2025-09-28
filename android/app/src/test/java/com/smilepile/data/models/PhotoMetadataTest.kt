package com.smilepile.data.models

import android.content.Context
import com.smilepile.data.entities.PhotoEntity
import com.smilepile.security.MetadataEncryption
import com.smilepile.security.SecureStorageManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for PhotoMetadata class
 * Tests conversion between PhotoMetadata and PhotoEntity with encryption
 */
class PhotoMetadataTest {

    @MockK(relaxed = true)
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var secureStorageManager: SecureStorageManager

    private lateinit var metadataEncryption: MetadataEncryption

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        // Mock SecureStorageManager encryption/decryption for testing
        coEvery { secureStorageManager.encrypt(any()) } answers {
            "encrypted_" + firstArg<String>()
        }
        coEvery { secureStorageManager.decrypt(any()) } answers {
            val encrypted = firstArg<String>()
            if (encrypted.startsWith("encrypted_")) {
                encrypted.removePrefix("encrypted_")
            } else {
                encrypted
            }
        }

        // Initialize MetadataEncryption with mocked SecureStorageManager
        metadataEncryption = MetadataEncryption(secureStorageManager)
    }

    @Test
    fun `test PhotoMetadata to PhotoEntity conversion with encryption`() = runBlocking {
        // Arrange
        val photoMetadata = PhotoMetadata(
            id = "test_photo_id",
            uri = "content://media/external/images/media/12345",
            categoryId = 1L,
            timestamp = System.currentTimeMillis(),
            childName = "Sophie Williams",
            childAge = 4,
            notes = "Playing with puppies at the park",
            tags = listOf("animals", "outdoor", "fun", "puppies"),
            milestone = "First time petting a dog",
            location = "Central Park Dog Run",
            customFields = mapOf("weather" to "sunny", "mood" to "excited")
        )

        // Act
        val photoEntity = photoMetadata.toEntity(metadataEncryption)

        // Assert
        assertEquals(photoMetadata.id, photoEntity.id)
        assertEquals(photoMetadata.uri, photoEntity.uri)
        assertEquals(photoMetadata.categoryId, photoEntity.categoryId)
        assertEquals(photoMetadata.timestamp, photoEntity.timestamp)

        // Encrypted fields should not be null
        assertNotNull(photoEntity.encryptedChildName)
        assertNotNull(photoEntity.encryptedChildAge)
        assertNotNull(photoEntity.encryptedNotes)
        assertNotNull(photoEntity.encryptedTags)
        assertNotNull(photoEntity.encryptedMilestone)
        assertNotNull(photoEntity.encryptedLocation)
        assertNotNull(photoEntity.encryptedMetadata)
    }

    @Test
    fun `test PhotoEntity to PhotoMetadata conversion with decryption`() = runBlocking {
        // Arrange - First create encrypted entity
        val originalMetadata = PhotoMetadata(
            id = "test_photo_id_2",
            uri = "content://media/external/images/media/67890",
            categoryId = 2L,
            timestamp = System.currentTimeMillis(),
            childName = "Marcus Chen",
            childAge = 6,
            notes = "Collecting leaves for school project",
            tags = listOf("nature", "school", "learning", "autumn"),
            milestone = "First nature collection",
            location = "Maple Grove Trail"
        )

        val photoEntity = originalMetadata.toEntity(metadataEncryption)

        // Act
        val decryptedMetadata = PhotoMetadata.fromEntity(photoEntity, metadataEncryption)

        // Assert
        assertEquals(originalMetadata.id, decryptedMetadata.id)
        assertEquals(originalMetadata.uri, decryptedMetadata.uri)
        assertEquals(originalMetadata.categoryId, decryptedMetadata.categoryId)
        assertEquals(originalMetadata.timestamp, decryptedMetadata.timestamp)
        assertEquals(originalMetadata.childName, decryptedMetadata.childName)
        assertEquals(originalMetadata.childAge, decryptedMetadata.childAge)
        assertEquals(originalMetadata.notes, decryptedMetadata.notes)
        assertEquals(originalMetadata.tags, decryptedMetadata.tags)
        assertEquals(originalMetadata.milestone, decryptedMetadata.milestone)
        assertEquals(originalMetadata.location, decryptedMetadata.location)
    }

    @Test
    fun `test PhotoMetadata without sensitive data`() = runBlocking {
        // Arrange
        val basicMetadata = PhotoMetadata(
            id = "basic_photo_id",
            uri = "content://media/external/images/media/11111",
            categoryId = 3L,
            timestamp = System.currentTimeMillis()
        )

        // Act
        val hasData = basicMetadata.hasSensitiveData()
        val basicEntity = basicMetadata.toEntityBasic()

        // Assert
        assertFalse("Should not have sensitive data", hasData)
        assertEquals(basicMetadata.id, basicEntity.id)
        assertEquals(basicMetadata.uri, basicEntity.uri)
        assertEquals(basicMetadata.categoryId, basicEntity.categoryId)
        assertEquals(basicMetadata.timestamp, basicEntity.timestamp)

        // All encrypted fields should be null
        assertNull(basicEntity.encryptedChildName)
        assertNull(basicEntity.encryptedChildAge)
        assertNull(basicEntity.encryptedNotes)
        assertNull(basicEntity.encryptedTags)
        assertNull(basicEntity.encryptedMilestone)
        assertNull(basicEntity.encryptedLocation)
        assertNull(basicEntity.encryptedMetadata)
    }

    @Test
    fun `test PhotoMetadata from basic PhotoEntity`() {
        // Arrange
        val basicEntity = PhotoEntity(
            id = "basic_entity_id",
            uri = "content://media/external/images/media/22222",
            categoryId = 4L,
            timestamp = System.currentTimeMillis()
        )

        // Act
        val basicMetadata = PhotoMetadata.fromEntityBasic(basicEntity)

        // Assert
        assertEquals(basicEntity.id, basicMetadata.id)
        assertEquals(basicEntity.uri, basicMetadata.uri)
        assertEquals(basicEntity.categoryId, basicMetadata.categoryId)
        assertEquals(basicEntity.timestamp, basicMetadata.timestamp)

        // All sensitive fields should be empty/null
        assertNull(basicMetadata.childName)
        assertNull(basicMetadata.childAge)
        assertNull(basicMetadata.notes)
        assertTrue(basicMetadata.tags.isEmpty())
        assertNull(basicMetadata.milestone)
        assertNull(basicMetadata.location)
        assertTrue(basicMetadata.customFields.isEmpty())
    }

    @Test
    fun `test hasSensitiveData detection`() {
        // Test with no sensitive data
        val noDataMetadata = PhotoMetadata(
            id = "test_id",
            uri = "test_uri",
            categoryId = 5L,
            timestamp = 123456789L
        )
        assertFalse(noDataMetadata.hasSensitiveData())

        // Test with child name
        val withNameMetadata = noDataMetadata.copy(childName = "Test Child")
        assertTrue(withNameMetadata.hasSensitiveData())

        // Test with child age
        val withAgeMetadata = noDataMetadata.copy(childAge = 5)
        assertTrue(withAgeMetadata.hasSensitiveData())

        // Test with notes
        val withNotesMetadata = noDataMetadata.copy(notes = "Test notes")
        assertTrue(withNotesMetadata.hasSensitiveData())

        // Test with tags
        val withTagsMetadata = noDataMetadata.copy(tags = listOf("tag1", "tag2"))
        assertTrue(withTagsMetadata.hasSensitiveData())

        // Test with milestone
        val withMilestoneMetadata = noDataMetadata.copy(milestone = "Test milestone")
        assertTrue(withMilestoneMetadata.hasSensitiveData())

        // Test with location
        val withLocationMetadata = noDataMetadata.copy(location = "Test location")
        assertTrue(withLocationMetadata.hasSensitiveData())

        // Test with custom fields
        val withCustomFieldsMetadata = noDataMetadata.copy(customFields = mapOf("key" to "value"))
        assertTrue(withCustomFieldsMetadata.hasSensitiveData())
    }

    @Test
    fun `test updateMetadata function`() {
        // Arrange
        val originalMetadata = PhotoMetadata(
            id = "update_test_id",
            uri = "test_uri",
            categoryId = 5L,
            timestamp = 123456789L,
            childName = "Original Name",
            childAge = 5,
            notes = "Original notes"
        )

        // Act
        val updatedMetadata = originalMetadata.updateMetadata(
            childName = "Updated Name",
            childAge = 6,
            notes = "Updated notes",
            tags = listOf("new", "tags"),
            milestone = "New milestone"
        )

        // Assert
        assertEquals("Updated Name", updatedMetadata.childName)
        assertEquals(6, updatedMetadata.childAge)
        assertEquals("Updated notes", updatedMetadata.notes)
        assertEquals(listOf("new", "tags"), updatedMetadata.tags)
        assertEquals("New milestone", updatedMetadata.milestone)

        // Non-sensitive fields should remain the same
        assertEquals(originalMetadata.id, updatedMetadata.id)
        assertEquals(originalMetadata.uri, updatedMetadata.uri)
        assertEquals(originalMetadata.categoryId, updatedMetadata.categoryId)
        assertEquals(originalMetadata.timestamp, updatedMetadata.timestamp)
    }

    @Test
    fun `test round trip encryption with complex data`() = runBlocking {
        // Arrange
        val complexMetadata = PhotoMetadata(
            id = "complex_test_id",
            uri = "content://media/external/images/media/99999",
            categoryId = 6L,
            timestamp = System.currentTimeMillis(),
            childName = "Elena Rodriguez-Smith",
            childAge = 8,
            notes = "Birthday party with friends and family. Had so much fun with the piñata and cake! 🎂🎉",
            tags = listOf("birthday", "celebration", "friends", "family", "piñata", "cake", "fun", "special"),
            milestone = "8th birthday celebration",
            location = "Grandma's house backyard",
            customFields = mapOf(
                "theme" to "unicorn",
                "guests" to "12",
                "weather" to "perfect",
                "photographer" to "Uncle Mike",
                "duration" to "4 hours",
                "favorite_activity" to "treasure hunt"
            )
        )

        // Act - Full round trip
        val entity = complexMetadata.toEntity(metadataEncryption)
        val decryptedMetadata = PhotoMetadata.fromEntity(entity, metadataEncryption)

        // Assert
        assertEquals(complexMetadata.id, decryptedMetadata.id)
        assertEquals(complexMetadata.uri, decryptedMetadata.uri)
        assertEquals(complexMetadata.categoryId, decryptedMetadata.categoryId)
        assertEquals(complexMetadata.timestamp, decryptedMetadata.timestamp)
        assertEquals(complexMetadata.childName, decryptedMetadata.childName)
        assertEquals(complexMetadata.childAge, decryptedMetadata.childAge)
        assertEquals(complexMetadata.notes, decryptedMetadata.notes)
        assertEquals(complexMetadata.tags, decryptedMetadata.tags)
        assertEquals(complexMetadata.milestone, decryptedMetadata.milestone)
        assertEquals(complexMetadata.location, decryptedMetadata.location)
        assertEquals(complexMetadata.customFields, decryptedMetadata.customFields)
    }
}