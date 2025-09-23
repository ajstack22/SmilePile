# SmilePile Selective Metadata Encryption System

This document explains the selective metadata encryption system implemented for Wave 4 of the SmilePile app. The system encrypts sensitive child-related data while keeping photos accessible in the device Gallery.

## Architecture Overview

The encryption system consists of several key components:

### Core Classes

1. **MetadataEncryption** - Handles selective encryption of sensitive metadata
2. **PhotoMetadata** - Helper class for working with encrypted photo data
3. **SecurePhotoRepository** - Repository with encryption/decryption capabilities
4. **Enhanced PhotoEntity** - Database entity with encrypted fields

### Key Features

- **Selective Encryption**: Only sensitive child data is encrypted, not photo URIs
- **Gallery Compatibility**: Photos remain accessible in Android Gallery app
- **Android Keystore**: Uses hardware-backed encryption keys when available
- **Performance Optimized**: Basic operations don't require decryption
- **Fallback Safe**: Graceful handling of decryption failures

## What Gets Encrypted vs. Unencrypted

### ✅ UNENCRYPTED (Accessible)
- Photo URI (content://media/... paths)
- Category ID
- Timestamp
- Favorite status
- Photo ID

### 🔒 ENCRYPTED (Sensitive Data)
- Child's name
- Child's age
- Personal notes
- Tags
- Milestone information
- Location data
- Custom fields

## Usage Examples

### 1. Adding a Photo with Sensitive Data

```kotlin
class PhotoViewModel @Inject constructor(
    private val securePhotoRepository: SecurePhotoRepository
) {

    fun addChildPhoto(photoUri: String, categoryId: String) {
        viewModelScope.launch {
            val photoMetadata = PhotoMetadata(
                uri = photoUri,
                categoryId = categoryId,
                // Sensitive data - will be encrypted automatically
                childName = "Emma Johnson",
                childAge = 5,
                notes = "First day of school!",
                tags = listOf("school", "milestone", "excited"),
                milestone = "Started kindergarten",
                location = "Elementary School"
            )

            // Automatically encrypts sensitive fields
            securePhotoRepository.insertSecurePhoto(photoMetadata)
        }
    }
}
```

### 2. Loading Photos for Gallery View (Fast)

```kotlin
// Use this for gallery thumbnails - no decryption overhead
fun loadGalleryPhotos(categoryId: String) {
    securePhotoRepository.getBasicPhotosByCategory(categoryId)
        .collect { photos ->
            // Photos contain URI, timestamp, favorite status
            // No sensitive data, very fast loading
            updateUI(photos)
        }
}
```

### 3. Loading Photos with Sensitive Data (Slower)

```kotlin
// Use this when you need to show/edit child information
fun loadPhotosWithMetadata(categoryId: String) {
    securePhotoRepository.getSecurePhotosByCategory(categoryId)
        .collect { photos ->
            // Photos contain decrypted child names, notes, etc.
            // Slower due to decryption, use only when needed
            updateDetailedUI(photos)
        }
}
```

### 4. Updating Child Information

```kotlin
fun updateChildInfo(photoId: String, newName: String, newNotes: String) {
    viewModelScope.launch {
        // Get current metadata with decryption
        val photo = securePhotoRepository.getSecurePhotoById(photoId)

        // Update sensitive fields
        val updatedPhoto = photo?.updateMetadata(
            childName = newName,
            notes = newNotes
        )

        // Save with re-encryption
        updatedPhoto?.let {
            securePhotoRepository.updateSecurePhoto(it)
        }
    }
}
```

## Performance Considerations

### Fast Operations (No Encryption)
- Loading gallery thumbnails
- Updating favorite status
- Basic photo operations
- Filtering by date/category

### Slower Operations (With Encryption)
- Loading photos with child data
- Searching by child name
- Updating sensitive metadata
- Detailed photo views

## Security Best Practices

### 1. Use Appropriate Loading Method
```kotlin
// ✅ Good: Fast gallery loading
securePhotoRepository.getBasicPhotosByCategory(categoryId)

// ❌ Avoid: Unnecessary decryption for thumbnails
securePhotoRepository.getSecurePhotosByCategory(categoryId)
```

### 2. Handle Encryption Failures Gracefully
```kotlin
try {
    val metadata = securePhotoRepository.getSecurePhotoById(photoId)
} catch (e: SecurityException) {
    // Encryption failed - show basic photo info only
    val basicMetadata = securePhotoRepository.getBasicPhotoById(photoId)
}
```

### 3. Validate Encryption on App Start
```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Validate encryption system is working
        lifecycleScope.launch {
            val isValid = securePhotoRepository.validateEncryption()
            if (!isValid) {
                // Handle encryption system failure
                showEncryptionError()
            }
        }
    }
}
```

## Database Schema

The PhotoEntity includes both encrypted and unencrypted fields:

```kotlin
@Entity(tableName = "photo_entities")
data class PhotoEntity(
    // Unencrypted fields
    val id: String,
    val uri: String,                    // Accessible for MediaStore
    val categoryId: String,
    val timestamp: Long,
    val isFavorite: Boolean,

    // Encrypted fields
    val encryptedChildName: String?,    // AES-256 encrypted
    val encryptedChildAge: String?,     // AES-256 encrypted
    val encryptedNotes: String?,        // AES-256 encrypted
    val encryptedTags: String?,         // AES-256 encrypted
    val encryptedMilestone: String?,    // AES-256 encrypted
    val encryptedLocation: String?,     // AES-256 encrypted
    val encryptedMetadata: String?      // AES-256 encrypted JSON blob
)
```

## Integration with Existing Code

### Step 1: Inject SecurePhotoRepository
```kotlin
@HiltViewModel
class YourViewModel @Inject constructor(
    private val securePhotoRepository: SecurePhotoRepository
) : ViewModel()
```

### Step 2: Replace Photo Operations
```kotlin
// Old way
photoRepository.insertPhoto(photoEntity)

// New way with encryption
securePhotoRepository.insertSecurePhoto(photoMetadata)
```

### Step 3: Update UI Layer
```kotlin
// Choose appropriate loading method based on needs
if (needSensitiveData) {
    securePhotoRepository.getSecurePhotosByCategory(categoryId)
} else {
    securePhotoRepository.getBasicPhotosByCategory(categoryId)
}
```

## Testing

Run the included unit tests to verify encryption functionality:

```bash
./gradlew test
```

Key test files:
- `MetadataEncryptionTest.kt` - Tests core encryption functionality
- `PhotoMetadataTest.kt` - Tests photo data conversion and encryption

## Troubleshooting

### Common Issues

1. **Encryption Validation Fails**
   - Ensure Android Keystore is available
   - Check device security settings
   - Verify app has proper permissions

2. **Slow Performance**
   - Use basic loading for gallery views
   - Only decrypt when sensitive data is needed
   - Consider caching decrypted data temporarily

3. **Data Loss on Decryption**
   - Encryption system returns safe defaults on failure
   - Check logs for SecurityException details
   - Verify data wasn't corrupted in storage

### Debug Logging

Enable encryption debug logs by adding to your Application class:

```kotlin
if (BuildConfig.DEBUG) {
    // Enable detailed encryption logging
    Log.d("Encryption", "System initialized successfully")
}
```

## Migration from Existing Data

If you have existing photos without encryption, they will continue to work normally. New sensitive data will be encrypted automatically. To encrypt existing data:

```kotlin
fun migrateExistingPhotos() {
    viewModelScope.launch {
        val allPhotos = photoRepository.getAllPhotos()
        allPhotos.forEach { photo ->
            // Convert to PhotoMetadata and re-save with encryption
            val metadata = PhotoMetadata.fromEntityBasic(photo)
            securePhotoRepository.updateSecurePhoto(metadata)
        }
    }
}
```

## Security Notes

- Encryption keys are stored in Android Keystore (hardware-backed when available)
- Photos remain accessible in Gallery app (URIs not encrypted)
- Sensitive metadata is protected with AES-256-GCM encryption
- System gracefully handles encryption failures
- No sensitive data is logged or exposed in crash reports