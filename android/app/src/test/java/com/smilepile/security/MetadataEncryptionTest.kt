package com.smilepile.security

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * Unit tests for MetadataEncryption class
 * Tests encryption/decryption of sensitive photo metadata
 */
class MetadataEncryptionTest {

    @Mock
    private lateinit var context: Context

    private lateinit var secureStorageManager: SecureStorageManager
    private lateinit var metadataEncryption: MetadataEncryption

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        // Note: In real tests, you'd need to mock the Android Keystore properly
        // For this example, we're showing the test structure
        secureStorageManager = SecureStorageManager(context)
        metadataEncryption = MetadataEncryption(secureStorageManager)
    }

    @Test
    fun `test encrypt and decrypt metadata`() = runBlocking {
        // Arrange
        val originalMetadata = MetadataEncryption.EncryptedMetadata(
            childName = "Emma Johnson",
            childAge = 5,
            notes = "First day of school!",
            tags = listOf("school", "milestone", "excited"),
            milestone = "Started kindergarten",
            location = "Elementary School",
            customFields = mapOf("teacher" to "Ms. Smith", "grade" to "K")
        )

        // Act
        val encrypted = metadataEncryption.encryptMetadata(originalMetadata)
        val decrypted = metadataEncryption.decryptMetadata(encrypted)

        // Assert
        assertNotNull(encrypted)
        assertTrue(encrypted.isNotEmpty())
        assertEquals(originalMetadata.childName, decrypted.childName)
        assertEquals(originalMetadata.childAge, decrypted.childAge)
        assertEquals(originalMetadata.notes, decrypted.notes)
        assertEquals(originalMetadata.tags, decrypted.tags)
        assertEquals(originalMetadata.milestone, decrypted.milestone)
        assertEquals(originalMetadata.location, decrypted.location)
        assertEquals(originalMetadata.customFields, decrypted.customFields)
    }

    @Test
    fun `test encrypt and decrypt string`() = runBlocking {
        // Arrange
        val originalString = "This is sensitive child information"

        // Act
        val encrypted = metadataEncryption.encryptString(originalString)
        val decrypted = metadataEncryption.decryptString(encrypted)

        // Assert
        assertNotNull(encrypted)
        assertTrue(encrypted!!.isNotEmpty())
        assertEquals(originalString, decrypted)
    }

    @Test
    fun `test encrypt and decrypt tags`() = runBlocking {
        // Arrange
        val originalTags = listOf("birthday", "family", "celebration", "happy")

        // Act
        val encrypted = metadataEncryption.encryptTags(originalTags)
        val decrypted = metadataEncryption.decryptTags(encrypted)

        // Assert
        assertNotNull(encrypted)
        assertTrue(encrypted!!.isNotEmpty())
        assertEquals(originalTags, decrypted)
    }

    @Test
    fun `test encrypt null and empty values`() = runBlocking {
        // Test null string encryption
        val encryptedNull = metadataEncryption.encryptString(null)
        assertNull(encryptedNull)

        // Test empty string encryption
        val encryptedEmpty = metadataEncryption.encryptString("")
        assertNull(encryptedEmpty)

        // Test empty tags encryption
        val encryptedEmptyTags = metadataEncryption.encryptTags(emptyList())
        assertNull(encryptedEmptyTags)
    }

    @Test
    fun `test decrypt invalid data returns safe defaults`() = runBlocking {
        // Test invalid encrypted string
        val decryptedInvalid = metadataEncryption.decryptString("invalid_encrypted_data")
        assertNull(decryptedInvalid)

        // Test invalid encrypted tags
        val decryptedInvalidTags = metadataEncryption.decryptTags("invalid_encrypted_data")
        assertTrue(decryptedInvalidTags.isEmpty())

        // Test invalid encrypted metadata
        val decryptedInvalidMetadata = metadataEncryption.decryptMetadata("invalid_encrypted_data")
        assertEquals(MetadataEncryption.EncryptedMetadata(), decryptedInvalidMetadata)
    }

    @Test
    fun `test create encrypted metadata helper`() = runBlocking {
        // Arrange
        val childName = "Alex Thompson"
        val childAge = 7
        val notes = "Soccer practice today"
        val tags = listOf("sports", "practice", "outdoor")
        val milestone = "Joined soccer team"
        val location = "City Park"
        val customFields = mapOf("team" to "Lions", "position" to "midfielder")

        // Act
        val encrypted = metadataEncryption.createEncryptedMetadata(
            childName = childName,
            childAge = childAge,
            notes = notes,
            tags = tags,
            milestone = milestone,
            location = location,
            customFields = customFields
        )

        val decrypted = metadataEncryption.decryptMetadata(encrypted)

        // Assert
        assertEquals(childName, decrypted.childName)
        assertEquals(childAge, decrypted.childAge)
        assertEquals(notes, decrypted.notes)
        assertEquals(tags, decrypted.tags)
        assertEquals(milestone, decrypted.milestone)
        assertEquals(location, decrypted.location)
        assertEquals(customFields, decrypted.customFields)
    }

    @Test
    fun `test encryption validation`() = runBlocking {
        // Act
        val isValid = metadataEncryption.validateEncryption()

        // Assert
        assertTrue("Encryption validation should pass", isValid)
    }

    @Test
    fun `test empty metadata encryption`() = runBlocking {
        // Arrange
        val emptyMetadata = MetadataEncryption.EncryptedMetadata()

        // Act
        val encrypted = metadataEncryption.encryptMetadata(emptyMetadata)
        val decrypted = metadataEncryption.decryptMetadata(encrypted)

        // Assert
        assertEquals(emptyMetadata, decrypted)
    }
}