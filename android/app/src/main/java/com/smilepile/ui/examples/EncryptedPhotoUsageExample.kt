package com.smilepile.ui.examples

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.data.models.PhotoMetadata
import com.smilepile.data.repository.SecurePhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Example ViewModel showing how to use the selective metadata encryption system
 * Demonstrates secure handling of child photos with encrypted sensitive data
 */
@HiltViewModel
class EncryptedPhotoUsageExample @Inject constructor(
    private val securePhotoRepository: SecurePhotoRepository
) : ViewModel() {

    private val _photos = MutableStateFlow<List<PhotoMetadata>>(emptyList())
    val photos: StateFlow<List<PhotoMetadata>> = _photos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Example 1: Adding a new photo with sensitive child metadata
     * The photo URI remains unencrypted for MediaStore access,
     * but child data is encrypted for privacy
     */
    fun addPhotoWithChildData(
        photoUri: String,
        categoryId: String,
        childName: String,
        childAge: Int,
        notes: String,
        tags: List<String>,
        milestone: String? = null,
        location: String? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val photoMetadata = PhotoMetadata(
                    id = "", // Will be assigned by database
                    uri = photoUri,
                    categoryId = categoryId.toLongOrNull() ?: 1L,
                    timestamp = System.currentTimeMillis(),
                    isFavorite = false,
                    // Sensitive data that will be encrypted
                    childName = childName,
                    childAge = childAge,
                    notes = notes,
                    tags = tags,
                    milestone = milestone,
                    location = location
                )

                // This will automatically encrypt sensitive fields
                val photoId = securePhotoRepository.insertSecurePhoto(photoMetadata)

                // Refresh the photos list
                loadPhotosWithDecryption(categoryId)

                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add photo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Example 2: Adding a basic photo without sensitive data
     * No encryption overhead for simple gallery photos
     */
    fun addBasicPhoto(
        photoUri: String,
        categoryId: String
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val photoMetadata = PhotoMetadata(
                    id = "", // Will be assigned by database
                    uri = photoUri,
                    categoryId = categoryId.toLongOrNull() ?: 1L,
                    timestamp = System.currentTimeMillis(),
                    isFavorite = false
                    // No sensitive data - won't trigger encryption
                )

                // This will store without encryption since no sensitive data
                val photoId = securePhotoRepository.insertSecurePhoto(photoMetadata)

                // Load basic photos for gallery view (faster, no decryption)
                loadPhotosBasic(categoryId)

                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add photo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Example 3: Loading photos for gallery display (basic, fast)
     * Use this when you only need to show photo thumbnails
     * and don't need sensitive metadata
     */
    fun loadPhotosBasic(categoryId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Fast loading without decryption - perfect for gallery views
                securePhotoRepository.getBasicPhotosByCategory(categoryId.toLongOrNull() ?: 1L)
                    .collect { photoList ->
                        _photos.value = photoList
                    }

                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load photos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Example 4: Loading photos with decrypted metadata
     * Use this when you need to display or edit sensitive information
     */
    fun loadPhotosWithDecryption(categoryId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Slower loading with decryption - use when sensitive data needed
                securePhotoRepository.getSecurePhotosByCategory(categoryId.toLongOrNull() ?: 1L)
                    .collect { photoList ->
                        _photos.value = photoList
                    }

                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load photos with metadata: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Example 5: Updating child information for a photo
     */
    fun updateChildInfo(
        photoId: String,
        newChildName: String? = null,
        newNotes: String? = null,
        newTags: List<String>? = null
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Get current photo metadata
                val currentPhoto = securePhotoRepository.getSecurePhotoById(photoId)
                    ?: throw IllegalArgumentException("Photo not found")

                // Update with new sensitive information
                val updatedPhoto = currentPhoto.updateMetadata(
                    childName = newChildName ?: currentPhoto.childName,
                    notes = newNotes ?: currentPhoto.notes,
                    tags = newTags ?: currentPhoto.tags
                )

                // Save with encryption
                securePhotoRepository.updateSecurePhoto(updatedPhoto)

                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update photo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Example 6: Searching photos by child name
     * Note: This requires decrypting all photos, so it's slower
     */
    fun searchPhotosByChildName(childName: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val matchingPhotos = securePhotoRepository.searchByChildName(childName)
                _photos.value = matchingPhotos

                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to search photos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Example 7: Getting photos with sensitive metadata only
     * Useful for privacy audits or data export features
     */
    fun loadPhotosWithSensitiveData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                securePhotoRepository.getPhotosWithChildData()
                    .collect { photoList ->
                        _photos.value = photoList
                    }

                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load photos with sensitive data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Example 8: Quick favorite toggle (no encryption needed)
     * This operates on unencrypted fields, so it's very fast
     */
    fun toggleFavorite(photoId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                // Fast operation - no encryption involved
                securePhotoRepository.updateFavoriteStatus(photoId, isFavorite)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update favorite: ${e.message}"
            }
        }
    }

    /**
     * Example 9: Validate encryption system
     * Call this during app startup to ensure encryption is working
     */
    fun validateEncryption() {
        viewModelScope.launch {
            try {
                val isValid = securePhotoRepository.validateEncryption()
                if (!isValid) {
                    _errorMessage.value = "Encryption system validation failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Encryption validation error: ${e.message}"
            }
        }
    }

    /**
     * Example 10: Get statistics about encrypted data
     */
    fun getEncryptionStats() {
        viewModelScope.launch {
            try {
                val securePhotoCount = securePhotoRepository.getSecurePhotoCount()
                // Use securePhotoCount for analytics or settings display
            } catch (e: Exception) {
                _errorMessage.value = "Failed to get encryption stats: ${e.message}"
            }
        }
    }

    /**
     * Clear any error messages
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Example data class for UI state with encrypted photo handling
 */
data class EncryptedPhotoUiState(
    val photos: List<PhotoMetadata> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showSensitiveData: Boolean = false, // Toggle between basic/secure loading
    val encryptionEnabled: Boolean = true
) {
    /**
     * Helper to determine if we should show sensitive metadata
     */
    fun shouldShowSensitiveData(): Boolean = showSensitiveData && encryptionEnabled

    /**
     * Helper to get display-safe photo data
     */
    fun getDisplayPhotos(): List<PhotoMetadata> {
        return if (shouldShowSensitiveData()) {
            photos // Full metadata with decrypted sensitive data
        } else {
            photos.map { photo ->
                // Strip sensitive data for basic display
                photo.copy(
                    childName = null,
                    childAge = null,
                    notes = null,
                    tags = emptyList(),
                    milestone = null,
                    location = null,
                    customFields = emptyMap()
                )
            }
        }
    }
}