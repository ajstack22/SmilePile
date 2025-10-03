# iOS Feature Implementation: Data Storage & Database

## ⚠️ ADVERSARIAL REVIEW CORRECTIONS ⚠️

**CRITICAL CORRECTIONS MADE** (Atlas Review - 2025-09-25):
- **FIXED**: DAO method counts - PhotoDao: 31 methods (verified actual count), CategoryDao: 18 methods (verified actual count)
- **FIXED**: Repository method counts - PhotoRepository: 21 methods (verified actual count), CategoryRepository: 10 methods (verified actual count)
- **CLARIFIED**: Domain models vs Entity models - Photo domain model has NO encrypted fields, only PhotoEntity has encrypted fields
- **VERIFIED**: All other claims accurate (DB version v5, encrypted fields count, default categories, storage paths)

## Context & Requirements

The Android SmilePile app uses a sophisticated data storage architecture built on Room database with the Repository pattern. The implementation handles two primary entities (Photos and Categories) with comprehensive CRUD operations, encrypted sensitive data, internal file storage, and reactive data flows. This analysis provides the complete blueprint for implementing equivalent functionality in iOS using Core Data.

### Android Implementation Architecture Overview

The Android data layer consists of:
- **Room Database**: SQLite abstraction with version 5, migration support
- **Entity Layer**: PhotoEntity and CategoryEntity with encrypted sensitive fields
- **DAO Layer**: Comprehensive query methods with Flow-based reactive streams
- **Repository Pattern**: Clean separation of data access with domain models
- **Storage Manager**: Internal file storage with thumbnail generation
- **Security**: Selective encryption of child-related sensitive data

## Research Phase

### Android Implementation Analysis

#### Database Schema

**PhotoEntity Table (`photo_entities`)**
```kotlin
@Entity(tableName = "photo_entities")
data class PhotoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val uri: String,                    // UNENCRYPTED - MediaStore compatibility
    val categoryId: Long,               // Foreign key to categories
    val timestamp: Long,                // Creation timestamp
    val isFavorite: Boolean = false,    // Favorite status

    // ENCRYPTED FIELDS - Sensitive child data
    val encryptedChildName: String? = null,     // Child's name
    val encryptedChildAge: String? = null,      // Child's age
    val encryptedNotes: String? = null,         // Personal notes
    val encryptedTags: String? = null,          // Organizational tags
    val encryptedMilestone: String? = null,     // Milestone information
    val encryptedLocation: String? = null,      // Location data
    val encryptedMetadata: String? = null       // Additional metadata blob
)
```

**CategoryEntity Table (`category_entities`)**
```kotlin
@Entity(tableName = "category_entities")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val displayName: String,            // User-facing name
    val colorHex: String,              // Category color
    val position: Int = 0,             // Display order
    val isDefault: Boolean = false,     // System default category
    val createdAt: Long               // Creation timestamp
)
```

**Foreign Key Relationships**
- PhotoEntity.categoryId → CategoryEntity.id (CASCADE DELETE)

#### Room Database Configuration

**SmilePileDatabase.kt**
```kotlin
@Database(
    entities = [PhotoEntity::class, CategoryEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SmilePileDatabase : RoomDatabase()
```

**Migration Strategy (v4→v5)**
- Converted CategoryEntity ID from String to Long with auto-increment
- Handles both numeric and non-numeric legacy IDs
- Fallback to destructive migration for other version jumps

**Type Converters**
```kotlin
class Converters {
    @TypeConverter fun fromTimestamp(value: Long?): Date?
    @TypeConverter fun dateToTimestamp(date: Date?): Long?
    @TypeConverter fun fromStringList(value: String?): List<String>
    @TypeConverter fun stringListToString(list: List<String>?): String?
}
```

#### DAO Methods and Queries

**PhotoDao - Complete Method List**
```kotlin
// CRUD Operations
suspend fun insert(photo: PhotoEntity): Long
suspend fun insertAll(photos: List<PhotoEntity>): List<Long>
suspend fun update(photo: PhotoEntity): Int
suspend fun delete(photo: PhotoEntity): Int
suspend fun deleteById(photoId: String): Int
suspend fun deleteByUri(uri: String): Int

// Retrieval Operations
fun getAll(): Flow<List<PhotoEntity>>
suspend fun getById(photoId: String): PhotoEntity?
suspend fun getByUri(uri: String): PhotoEntity?
fun getByIdFlow(photoId: String): Flow<PhotoEntity?>

// Category-based Operations
fun getByCategory(categoryId: Long): Flow<List<PhotoEntity>>
suspend fun getPhotoCountByCategory(categoryId: Long): Int
fun getPhotoCountByCategoryFlow(categoryId: Long): Flow<Int>
suspend fun deleteByCategory(categoryId: Long): Int

// Favorites
fun getFavorites(): Flow<List<PhotoEntity>>
suspend fun updateFavoriteStatus(photoId: String, isFavorite: Boolean): Int

// Search and Filtering
fun searchPhotos(searchQuery: String): Flow<List<PhotoEntity>>
fun searchPhotosInCategory(searchQuery: String, categoryId: Long): Flow<List<PhotoEntity>>
fun getPhotosByDateRange(startDate: Long, endDate: Long): Flow<List<PhotoEntity>>
fun getPhotosByDateRangeAndCategory(startDate: Long, endDate: Long, categoryId: Long): Flow<List<PhotoEntity>>
fun searchPhotosWithFilters(searchQuery: String, startDate: Long, endDate: Long, favoritesOnly: Boolean?, categoryId: Long): Flow<List<PhotoEntity>>

// Encrypted Data Queries
fun getPhotosWithEncryptedData(): Flow<List<PhotoEntity>>
suspend fun getPhotosWithEncryptedDataCount(): Int
```

**CategoryDao - Complete Method List**
```kotlin
// CRUD Operations
suspend fun insert(category: CategoryEntity): Long
suspend fun insertAll(categories: List<CategoryEntity>): List<Long>
suspend fun update(category: CategoryEntity): Int
suspend fun delete(category: CategoryEntity): Int
suspend fun deleteById(categoryId: Long): Int

// Retrieval Operations
fun getAll(): Flow<List<CategoryEntity>>
fun getAllByDisplayName(): Flow<List<CategoryEntity>>
suspend fun getById(categoryId: Long): CategoryEntity?
fun getByIdFlow(categoryId: Long): Flow<CategoryEntity?>

// Name-based Operations
suspend fun getByDisplayName(displayName: String): CategoryEntity?
fun getByDisplayNameFlow(displayName: String): Flow<CategoryEntity?>
suspend fun existsByDisplayName(displayName: String): Boolean
suspend fun existsByDisplayNameExcludingId(displayName: String, excludeId: Long): Boolean

// Utility Operations
suspend fun getCount(): Int
fun getCountFlow(): Flow<Int>
fun searchByDisplayName(query: String): Flow<List<CategoryEntity>>
suspend fun updateDisplayName(categoryId: Long, newDisplayName: String): Int
suspend fun updateColor(categoryId: Long, newColorHex: String): Int
```

#### Repository Pattern Implementation

**Domain Models (Photo.kt, Category.kt)**
```kotlin
// Photo domain model with validation and computed properties (NO ENCRYPTED FIELDS)
@Parcelize
@Entity(tableName = "photos")
data class Photo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val categoryId: Long,
    val name: String,
    val isFromAssets: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val fileSize: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val isFavorite: Boolean = false
) : Parcelable {
    val displayName: String get() = name.ifEmpty { path.substringAfterLast("/").substringBeforeLast(".") }
    val isValid: Boolean get() = path.isNotEmpty() && categoryId > 0
}

// NOTE: Photo domain model is SEPARATE from PhotoEntity database entity
// PhotoEntity contains encrypted fields, Photo domain model does NOT

// Category domain model with default data
@Parcelize
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val displayName: String,
    val position: Int,
    val iconResource: String? = null,
    val colorHex: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {
    companion object {
        fun getDefaultCategories(): List<Category> = listOf(
            Category(id = 1, name = "family", displayName = "Family", position = 0, colorHex = "#E91E63", isDefault = true),
            Category(id = 2, name = "cars", displayName = "Cars", position = 1, colorHex = "#F44336", isDefault = true),
            Category(id = 3, name = "games", displayName = "Games", position = 2, colorHex = "#9C27B0", isDefault = true),
            Category(id = 4, name = "sports", displayName = "Sports", position = 3, colorHex = "#4CAF50", isDefault = true)
        )
    }
}
```

**Repository Interfaces**
```kotlin
interface PhotoRepository {
    suspend fun insertPhoto(photo: Photo): Long
    suspend fun insertPhotos(photos: List<Photo>)
    suspend fun updatePhoto(photo: Photo)
    suspend fun deletePhoto(photo: Photo)
    suspend fun deletePhotoById(photoId: Long)
    suspend fun getPhotoById(photoId: Long): Photo?
    suspend fun getPhotoByPath(path: String): Photo?
    suspend fun getPhotosByCategory(categoryId: Long): List<Photo>
    fun getPhotosByCategoryFlow(categoryId: Long): Flow<List<Photo>>
    suspend fun getAllPhotos(): List<Photo>
    fun getAllPhotosFlow(): Flow<List<Photo>>
    suspend fun deletePhotosByCategory(categoryId: Long)
    suspend fun getPhotoCount(): Int
    suspend fun getPhotoCategoryCount(categoryId: Long): Int
    suspend fun removeFromLibrary(photo: Photo)
    suspend fun removeFromLibraryById(photoId: Long)
    fun searchPhotos(searchQuery: String): Flow<List<Photo>>
    fun searchPhotosInCategory(searchQuery: String, categoryId: Long): Flow<List<Photo>>
    fun getPhotosByDateRange(startDate: Long, endDate: Long): Flow<List<Photo>>
    fun getPhotosByDateRangeAndCategory(startDate: Long, endDate: Long, categoryId: Long): Flow<List<Photo>>
    fun searchPhotosWithFilters(searchQuery: String, startDate: Long, endDate: Long, favoritesOnly: Boolean?, categoryId: Long?): Flow<List<Photo>>
}

interface CategoryRepository {
    suspend fun insertCategory(category: Category): Long
    suspend fun insertCategories(categories: List<Category>)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun getCategoryById(categoryId: Long): Category?
    suspend fun getAllCategories(): List<Category>
    fun getAllCategoriesFlow(): Flow<List<Category>>
    suspend fun getCategoryByName(name: String): Category?
    suspend fun initializeDefaultCategories()
    suspend fun getCategoryCount(): Int
}
```

#### File Storage Structure

**StorageManager Implementation**
```kotlin
@Singleton
class StorageManager @Inject constructor(@ApplicationContext private val context: Context) {
    // Storage Configuration
    private val PHOTOS_DIR = "photos"                    // Internal photos directory
    private val THUMBNAILS_DIR = "thumbnails"           // Internal thumbnails directory
    private val THUMBNAIL_SIZE = 300                     // Thumbnail dimensions (px)
    private val THUMBNAIL_QUALITY = 85                   // JPEG quality for thumbnails
    private val MAX_PHOTO_SIZE = 2048                    // Max photo dimension (px)
    private val PHOTO_QUALITY = 90                       // JPEG quality for photos

    // Directory Management
    private val photosDir: File by lazy { File(context.filesDir, PHOTOS_DIR).apply { if (!exists()) mkdirs() } }
    private val thumbnailsDir: File by lazy { File(context.filesDir, THUMBNAILS_DIR).apply { if (!exists()) mkdirs() } }

    // Core Operations
    suspend fun importPhoto(sourceUri: Uri): StorageResult?
    suspend fun importPhotos(sourceUris: List<Uri>): List<StorageResult>
    suspend fun savePhotoToInternalStorage(bitmap: Bitmap, filename: String): File?
    suspend fun deletePhoto(photoPath: String): Boolean
    suspend fun calculateStorageUsage(): StorageUsage
    suspend fun cleanupOrphanedThumbnails(): Int
    suspend fun getAllInternalPhotos(): List<String>
    suspend fun copyPhotoToInternalStorage(sourceFile: File): StorageResult?
    suspend fun migrateExternalPhotosToInternal(externalPhotoPaths: List<String>): List<StorageResult>
    fun getThumbnailPath(photoPath: String): String?
    fun getAvailableSpace(): Long
    fun hasEnoughSpace(estimatedSize: Long = 10MB): Boolean
    fun isInternalStoragePath(photoPath: String): Boolean
}

data class StorageResult(val photoPath: String, val thumbnailPath: String?, val fileName: String, val fileSize: Long)
data class StorageUsage(val totalBytes: Long = 0, val photoBytes: Long = 0, val thumbnailBytes: Long = 0, val photoCount: Int = 0)
```

#### Migration Strategies

**Database Migration v4→v5**
- Handles CategoryEntity ID conversion from String to Long
- Preserves data integrity with SQL transformations
- Supports both numeric and alphanumeric legacy IDs
- Fallback to destructive migration for complex cases

#### Default Data Initialization

**Category Defaults**
```kotlin
Category.getDefaultCategories(): List<Category> = [
    Category(id: 1, name: "family", displayName: "Family", position: 0, colorHex: "#E91E63", isDefault: true),
    Category(id: 2, name: "cars", displayName: "Cars", position: 1, colorHex: "#F44336", isDefault: true),
    Category(id: 3, name: "games", displayName: "Games", position: 2, colorHex: "#9C27B0", isDefault: true),
    Category(id: 4, name: "sports", displayName: "Sports", position: 3, colorHex: "#4CAF50", isDefault: true)
]
```

### Technical Components

#### All Entity Field Definitions

**PhotoEntity Complete Field List**
- `id: String` (Primary Key, UUID)
- `uri: String` (Unencrypted file path)
- `categoryId: Long` (Foreign Key to CategoryEntity)
- `timestamp: Long` (Creation/import timestamp)
- `isFavorite: Boolean` (Favorite status flag)
- `encryptedChildName: String?` (Encrypted child's name)
- `encryptedChildAge: String?` (Encrypted child's age)
- `encryptedNotes: String?` (Encrypted personal notes)
- `encryptedTags: String?` (Encrypted organizational tags)
- `encryptedMilestone: String?` (Encrypted milestone data)
- `encryptedLocation: String?` (Encrypted location data)
- `encryptedMetadata: String?` (Encrypted metadata blob)

**CategoryEntity Complete Field List**
- `id: Long` (Primary Key, Auto-increment)
- `displayName: String` (User-facing category name)
- `colorHex: String` (Category color in hex format)
- `position: Int` (Display order position)
- `isDefault: Boolean` (System default category flag)
- `createdAt: Long` (Creation timestamp)

#### All DAO Methods with Signatures

**PhotoDao Method Signatures (31 methods total) - VERIFIED ACTUAL COUNT**
```kotlin
// CRUD
@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(photo: PhotoEntity): Long
@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPhoto(photo: PhotoEntity)
@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(photos: List<PhotoEntity>): List<Long>
@Update suspend fun update(photo: PhotoEntity): Int
@Update suspend fun updatePhoto(photo: PhotoEntity)
@Delete suspend fun delete(photo: PhotoEntity): Int
@Query("DELETE FROM photo_entities WHERE id = :photoId") suspend fun deleteById(photoId: String): Int
@Transaction @Query("DELETE FROM photo_entities WHERE uri = :uri") suspend fun deleteByUri(uri: String): Int

// Retrieval
@Query("SELECT * FROM photo_entities ORDER BY timestamp DESC") fun getAll(): Flow<List<PhotoEntity>>
@Query("SELECT * FROM photo_entities WHERE id = :photoId") suspend fun getById(photoId: String): PhotoEntity?
@Query("SELECT * FROM photo_entities WHERE id = :photoId") suspend fun getPhotoById(photoId: String): PhotoEntity?
@Query("SELECT * FROM photo_entities WHERE id = :photoId") fun getByIdFlow(photoId: String): Flow<PhotoEntity?>
@Query("SELECT * FROM photo_entities WHERE uri = :uri") suspend fun getByUri(uri: String): PhotoEntity?

// Category Operations
@Query("SELECT * FROM photo_entities WHERE category_id = :categoryId ORDER BY timestamp DESC") fun getByCategory(categoryId: Long): Flow<List<PhotoEntity>>
@Query("SELECT COUNT(*) FROM photo_entities WHERE category_id = :categoryId") suspend fun getPhotoCountByCategory(categoryId: Long): Int
@Query("SELECT COUNT(*) FROM photo_entities WHERE category_id = :categoryId") fun getPhotoCountByCategoryFlow(categoryId: Long): Flow<Int>
@Query("DELETE FROM photo_entities WHERE category_id = :categoryId") suspend fun deleteByCategory(categoryId: Long): Int

// Favorites
@Query("SELECT * FROM photo_entities WHERE is_favorite = 1 ORDER BY timestamp DESC") fun getFavorites(): Flow<List<PhotoEntity>>
@Query("UPDATE photo_entities SET is_favorite = :isFavorite WHERE id = :photoId") suspend fun updateFavoriteStatus(photoId: String, isFavorite: Boolean): Int

// Search & Filter
@Query("SELECT * FROM photo_entities WHERE uri LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC") fun searchPhotos(searchQuery: String): Flow<List<PhotoEntity>>
@Query("SELECT * FROM photo_entities WHERE uri LIKE '%' || :searchQuery || '%' AND category_id = :categoryId ORDER BY timestamp DESC") fun searchPhotosInCategory(searchQuery: String, categoryId: Long): Flow<List<PhotoEntity>>
@Query("SELECT * FROM photo_entities WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC") fun getPhotosByDateRange(startDate: Long, endDate: Long): Flow<List<PhotoEntity>>
@Query("SELECT * FROM photo_entities WHERE timestamp BETWEEN :startDate AND :endDate AND category_id = :categoryId ORDER BY timestamp DESC") fun getPhotosByDateRangeAndCategory(startDate: Long, endDate: Long, categoryId: Long): Flow<List<PhotoEntity>>
@Query(COMPLEX_FILTER_QUERY) fun searchPhotosWithFilters(searchQuery: String, startDate: Long, endDate: Long, favoritesOnly: Boolean?, categoryId: Long): Flow<List<PhotoEntity>>

// Encrypted Data
@Query(ENCRYPTED_DATA_QUERY) fun getPhotosWithEncryptedData(): Flow<List<PhotoEntity>>
@Query(ENCRYPTED_DATA_COUNT_QUERY) suspend fun getPhotosWithEncryptedDataCount(): Int

// Additional Operations
@Query("SELECT * FROM photo_entities ORDER BY timestamp DESC") fun getAllPhotos(): Flow<List<PhotoEntity>>
@Query("SELECT * FROM photo_entities WHERE category_id = :categoryId ORDER BY timestamp DESC") fun getPhotosByCategory(categoryId: Long): Flow<List<PhotoEntity>>
@Query("DELETE FROM photo_entities WHERE id = :photoId") suspend fun deletePhotoById(photoId: String)
@Query("DELETE FROM photo_entities WHERE category_id = :categoryId") suspend fun deletePhotosByCategory(categoryId: Long)
@Query("SELECT * FROM photo_entities ORDER BY timestamp DESC") suspend fun getAllPhotosSnapshot(): List<PhotoEntity>
```

**CategoryDao Method Signatures (18 methods total) - VERIFIED ACTUAL COUNT**
```kotlin
// CRUD
@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(category: CategoryEntity): Long
@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(categories: List<CategoryEntity>): List<Long>
@Update suspend fun update(category: CategoryEntity): Int
@Delete suspend fun delete(category: CategoryEntity): Int
@Query("DELETE FROM category_entities WHERE id = :categoryId") suspend fun deleteById(categoryId: Long): Int

// Retrieval
@Query("SELECT * FROM category_entities ORDER BY created_at ASC") fun getAll(): Flow<List<CategoryEntity>>
@Query("SELECT * FROM category_entities ORDER BY display_name ASC") fun getAllByDisplayName(): Flow<List<CategoryEntity>>
@Query("SELECT * FROM category_entities WHERE id = :categoryId") suspend fun getById(categoryId: Long): CategoryEntity?
@Query("SELECT * FROM category_entities WHERE id = :categoryId") fun getByIdFlow(categoryId: Long): Flow<CategoryEntity?>

// Name Operations
@Query("SELECT * FROM category_entities WHERE display_name = :displayName COLLATE NOCASE") suspend fun getByDisplayName(displayName: String): CategoryEntity?
@Query("SELECT * FROM category_entities WHERE display_name = :displayName COLLATE NOCASE") fun getByDisplayNameFlow(displayName: String): Flow<CategoryEntity?>
@Query("SELECT COUNT(*) > 0 FROM category_entities WHERE display_name = :displayName COLLATE NOCASE") suspend fun existsByDisplayName(displayName: String): Boolean
@Query("SELECT COUNT(*) > 0 FROM category_entities WHERE display_name = :displayName COLLATE NOCASE AND id != :excludeId") suspend fun existsByDisplayNameExcludingId(displayName: String, excludeId: Long): Boolean

// Utility
@Query("SELECT COUNT(*) FROM category_entities") suspend fun getCount(): Int
@Query("SELECT COUNT(*) FROM category_entities") fun getCountFlow(): Flow<Int>
@Query("SELECT * FROM category_entities WHERE display_name LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY display_name ASC") fun searchByDisplayName(query: String): Flow<List<CategoryEntity>>
@Query("UPDATE category_entities SET display_name = :newDisplayName WHERE id = :categoryId") suspend fun updateDisplayName(categoryId: Long, newDisplayName: String): Int
@Query("UPDATE category_entities SET color_hex = :newColorHex WHERE id = :categoryId") suspend fun updateColor(categoryId: Long, newColorHex: String): Int
```

#### All Repository Methods

**PhotoRepository Methods (21 methods) - VERIFIED ACTUAL COUNT**
- suspend fun insertPhoto(photo: Photo): Long
- suspend fun insertPhotos(photos: List<Photo>)
- suspend fun updatePhoto(photo: Photo)
- suspend fun deletePhoto(photo: Photo)
- suspend fun deletePhotoById(photoId: Long)
- suspend fun getPhotoById(photoId: Long): Photo?
- suspend fun getPhotoByPath(path: String): Photo?
- suspend fun getPhotosByCategory(categoryId: Long): List<Photo>
- fun getPhotosByCategoryFlow(categoryId: Long): Flow<List<Photo>>
- suspend fun getAllPhotos(): List<Photo>
- fun getAllPhotosFlow(): Flow<List<Photo>>
- suspend fun deletePhotosByCategory(categoryId: Long)
- suspend fun getPhotoCount(): Int
- suspend fun getPhotoCategoryCount(categoryId: Long): Int
- suspend fun removeFromLibrary(photo: Photo)
- suspend fun removeFromLibraryById(photoId: Long)
- fun searchPhotos(searchQuery: String): Flow<List<Photo>>
- fun searchPhotosInCategory(searchQuery: String, categoryId: Long): Flow<List<Photo>>
- fun getPhotosByDateRange(startDate: Long, endDate: Long): Flow<List<Photo>>
- fun getPhotosByDateRangeAndCategory(startDate: Long, endDate: Long, categoryId: Long): Flow<List<Photo>>
- fun searchPhotosWithFilters(searchQuery: String, startDate: Long, endDate: Long, favoritesOnly: Boolean?, categoryId: Long?): Flow<List<Photo>>

**CategoryRepository Methods (10 methods) - VERIFIED ACTUAL COUNT**
- suspend fun insertCategory(category: Category): Long
- suspend fun insertCategories(categories: List<Category>)
- suspend fun updateCategory(category: Category)
- suspend fun deleteCategory(category: Category)
- suspend fun getCategoryById(categoryId: Long): Category?
- suspend fun getAllCategories(): List<Category>
- fun getAllCategoriesFlow(): Flow<List<Category>>
- suspend fun getCategoryByName(name: String): Category?
- suspend fun initializeDefaultCategories()
- suspend fun getCategoryCount(): Int

#### File Storage Paths and Organization

**Internal Storage Structure**
```
/data/data/com.smilepile/files/
├── photos/                          # Original photos directory
│   ├── IMG_20240101_120000_abc12345.jpg
│   ├── IMG_20240101_120001_def67890.jpg
│   └── ...
└── thumbnails/                      # Generated thumbnails directory
    ├── thumb_IMG_20240101_120000_abc12345.jpg
    ├── thumb_IMG_20240101_120001_def67890.jpg
    └── ...
```

**File Naming Convention**
- Original Photos: `IMG_YYYYMMDD_HHMMSS_<8-char-uuid>.jpg`
- Thumbnails: `thumb_<original-filename>`
- All files stored in app's private internal storage for security

#### Database Versioning Approach

**Current Version**: 5
**Migration Path**: v4→v5 (CategoryEntity ID type conversion)
**Strategy**: Planned migrations with destructive fallback
**Schema Export**: Disabled for security (exportSchema = false)

## Story Creation

### User Stories

**US-DS-001: Core Data Models**
```
As an iOS developer, I want to create Core Data models equivalent to Android entities
So that I can store photos and categories with the same data structure and relationships
Acceptance Criteria:
- PhotoEntity Core Data model with all 12 fields including encrypted properties
- CategoryEntity Core Data model with all 6 fields
- Foreign key relationship from Photo to Category with cascade delete
- Auto-generated IDs and timestamp defaults
- Validation rules for required fields
```

**US-DS-002: Database Operations**
```
As an iOS developer, I want to implement all CRUD operations equivalent to Android DAOs
So that I can perform the same database operations with consistent behavior
Acceptance Criteria:
- All 31 PhotoDao equivalent methods implemented (VERIFIED ACTUAL COUNT)
- All 18 CategoryDao equivalent methods implemented (VERIFIED ACTUAL COUNT)
- Combine + async/await patterns for reactive data flows
- NSFetchRequest configurations for complex queries
- Error handling with custom repository exceptions
```

**US-DS-003: Repository Pattern**
```
As an iOS developer, I want to implement the Repository pattern with proper abstraction
So that I can maintain clean architecture separation between data and business logic
Acceptance Criteria:
- PhotoRepository protocol with all 21 method signatures (VERIFIED ACTUAL COUNT)
- CategoryRepository protocol with all 10 method signatures (VERIFIED ACTUAL COUNT)
- Repository implementations with entity-to-model mapping
- Dependency injection support for repositories
- Custom exception types for repository errors
```

**US-DS-004: File Storage Management**
```
As an iOS developer, I want to implement photo storage management equivalent to Android StorageManager
So that I can handle photo importing, thumbnail generation, and storage cleanup
Acceptance Criteria:
- Documents directory structure with photos/ and thumbnails/ subdirectories
- Photo import with automatic resizing and thumbnail generation
- Internal storage only for security (no Photos framework storage)
- Storage usage calculation and cleanup operations
- File naming convention matching Android implementation
```

**US-DS-005: Data Migration Support**
```
As an iOS developer, I want to implement Core Data migration support
So that I can handle schema changes and data preservation across app versions
Acceptance Criteria:
- Core Data model versioning system
- Lightweight migration for compatible changes
- Custom migration for complex schema changes
- Migration testing and validation
- Fallback strategies for failed migrations
```

**US-DS-006: Default Data Initialization**
```
As an iOS developer, I want to initialize default categories on first app launch
So that users have the same initial category structure as Android
Acceptance Criteria:
- Default category creation (Family, Cars, Games, Sports)
- Proper color codes and positioning
- One-time initialization check
- Default category preservation during updates
- Migration of custom categories
```

**US-DS-007: Encrypted Sensitive Data**
```
As an iOS developer, I want to implement selective encryption for sensitive photo metadata
So that child-related information remains secure while maintaining photo accessibility
Acceptance Criteria:
- Encryption for child name, age, notes, tags, milestone, location, metadata
- Photo paths remain unencrypted for system compatibility
- Secure key management integration
- Encrypted data querying capabilities
- Performance optimization for encrypted fields
```

**US-DS-008: Search and Filtering**
```
As an iOS developer, I want to implement comprehensive search and filtering capabilities
So that users can find photos using various criteria combinations
Acceptance Criteria:
- Text search across photo paths/names
- Date range filtering with start/end bounds
- Category-based filtering
- Favorites-only filtering
- Combined multi-criteria search with proper predicate logic
- Performance optimization for complex queries
```

### Technical Requirements

#### Core Data Models Needed

**Photo Core Data Entity**
```swift
@NSManaged public var id: String
@NSManaged public var uri: String
@NSManaged public var categoryId: Int64
@NSManaged public var timestamp: Int64
@NSManaged public var isFavorite: Bool
@NSManaged public var encryptedChildName: String?
@NSManaged public var encryptedChildAge: String?
@NSManaged public var encryptedNotes: String?
@NSManaged public var encryptedTags: String?
@NSManaged public var encryptedMilestone: String?
@NSManaged public var encryptedLocation: String?
@NSManaged public var encryptedMetadata: String?
@NSManaged public var category: CategoryEntity?
```

**Category Core Data Entity**
```swift
@NSManaged public var id: Int64
@NSManaged public var displayName: String
@NSManaged public var colorHex: String
@NSManaged public var position: Int32
@NSManaged public var isDefault: Bool
@NSManaged public var createdAt: Int64
@NSManaged public var photos: NSSet?
```

#### Persistence Methods

**Core Data Stack Configuration**
- NSPersistentContainer with model name "SmilePileDataModel"
- SQLite store type with WAL journaling mode
- Automatic migration enabled where possible
- Background context for write operations
- Main context for UI operations

#### Query Requirements

**NSFetchRequest Configurations**
- Equivalent predicates for all Android SQL queries
- Sort descriptors for timestamp/name ordering
- Batch size configuration for performance
- Relationship prefetching to avoid n+1 queries

#### File Management Needs

**iOS Storage Approach**
- Documents directory for user-generated content
- Application Support for app-specific data
- Thumbnail generation using UIKit/Core Graphics
- FileManager extensions for storage operations

## Implementation Blueprint

### iOS Equivalents

#### Room → Core Data Mapping

**Entity Definitions**
```swift
// SmilePileDataModel.xcdatamodeld structure
Entity: Photo
- id: String (Primary Key)
- uri: String (Required)
- categoryId: Integer 64 (Required)
- timestamp: Integer 64 (Default: current timestamp)
- isFavorite: Boolean (Default: NO)
- encryptedChildName: String (Optional)
- encryptedChildAge: String (Optional)
- encryptedNotes: String (Optional)
- encryptedTags: String (Optional)
- encryptedMilestone: String (Optional)
- encryptedLocation: String (Optional)
- encryptedMetadata: String (Optional)

Entity: Category
- id: Integer 64 (Primary Key, Auto-increment)
- displayName: String (Required)
- colorHex: String (Required)
- position: Integer 32 (Default: 0)
- isDefault: Boolean (Default: NO)
- createdAt: Integer 64 (Default: current timestamp)

Relationship: Photo.category → Category (To-One, Delete Rule: Nullify)
Relationship: Category.photos → Photo (To-Many, Delete Rule: Cascade)
```

#### Repository Pattern in Swift

**Protocol Definitions**
```swift
protocol PhotoRepository {
    func insertPhoto(_ photo: Photo) async throws -> Int64
    func insertPhotos(_ photos: [Photo]) async throws
    func updatePhoto(_ photo: Photo) async throws
    func deletePhoto(_ photo: Photo) async throws
    func deletePhotoById(_ photoId: Int64) async throws
    func getPhotoById(_ photoId: Int64) async throws -> Photo?
    func getPhotoByPath(_ path: String) async throws -> Photo?
    func getPhotosByCategory(_ categoryId: Int64) async throws -> [Photo]
    func getPhotosByCategoryPublisher(_ categoryId: Int64) -> AnyPublisher<[Photo], Error>
    func getAllPhotos() async throws -> [Photo]
    func getAllPhotosPublisher() -> AnyPublisher<[Photo], Error>
    func deletePhotosByCategory(_ categoryId: Int64) async throws
    func getPhotoCount() async throws -> Int
    func getPhotoCategoryCount(_ categoryId: Int64) async throws -> Int
    func removeFromLibrary(_ photo: Photo) async throws
    func removeFromLibraryById(_ photoId: Int64) async throws
    func searchPhotos(_ searchQuery: String) -> AnyPublisher<[Photo], Error>
    func searchPhotosInCategory(_ searchQuery: String, categoryId: Int64) -> AnyPublisher<[Photo], Error>
    func getPhotosByDateRange(startDate: Int64, endDate: Int64) -> AnyPublisher<[Photo], Error>
    func getPhotosByDateRangeAndCategory(startDate: Int64, endDate: Int64, categoryId: Int64) -> AnyPublisher<[Photo], Error>
    func searchPhotosWithFilters(searchQuery: String, startDate: Int64, endDate: Int64, favoritesOnly: Bool?, categoryId: Int64?) -> AnyPublisher<[Photo], Error>
}

protocol CategoryRepository {
    func insertCategory(_ category: Category) async throws -> Int64
    func insertCategories(_ categories: [Category]) async throws
    func updateCategory(_ category: Category) async throws
    func deleteCategory(_ category: Category) async throws
    func getCategoryById(_ categoryId: Int64) async throws -> Category?
    func getAllCategories() async throws -> [Category]
    func getAllCategoriesPublisher() -> AnyPublisher<[Category], Error>
    func getCategoryByName(_ name: String) async throws -> Category?
    func initializeDefaultCategories() async throws
    func getCategoryCount() async throws -> Int
}
```

#### File Management Approach

**iOS Storage Manager**
```swift
@MainActor
class StorageManager: ObservableObject {
    private let photosDirectory: URL
    private let thumbnailsDirectory: URL

    private let thumbnailSize: CGFloat = 300
    private let thumbnailQuality: CGFloat = 0.85
    private let maxPhotoSize: CGFloat = 2048
    private let photoQuality: CGFloat = 0.90

    init() throws {
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        photosDirectory = documentsURL.appendingPathComponent("photos")
        thumbnailsDirectory = documentsURL.appendingPathComponent("thumbnails")

        try FileManager.default.createDirectory(at: photosDirectory, withIntermediateDirectories: true)
        try FileManager.default.createDirectory(at: thumbnailsDirectory, withIntermediateDirectories: true)
    }

    func importPhoto(from sourceURL: URL) async throws -> StorageResult
    func importPhotos(from sourceURLs: [URL]) async throws -> [StorageResult]
    func savePhotoToInternalStorage(_ image: UIImage, filename: String) async throws -> URL
    func deletePhoto(at photoPath: String) async throws -> Bool
    func calculateStorageUsage() async throws -> StorageUsage
    func cleanupOrphanedThumbnails() async throws -> Int
    func getAllInternalPhotos() async throws -> [String]
    func getThumbnailPath(for photoPath: String) -> String?
    func getAvailableSpace() throws -> Int64
    func hasEnoughSpace(estimatedSize: Int64) throws -> Bool
    func isInternalStoragePath(_ photoPath: String) -> Bool
}
```

#### UserDefaults for Preferences

**Settings Storage**
```swift
class SmilePileSettings: ObservableObject {
    @AppStorage("defaultCategoriesInitialized") var defaultCategoriesInitialized: Bool = false
    @AppStorage("currentAppMode") var currentAppMode: AppMode = .parentMode
    @AppStorage("parentalLockEnabled") var parentalLockEnabled: Bool = true
    @AppStorage("biometricAuthEnabled") var biometricAuthEnabled: Bool = false
    @AppStorage("lastBackupDate") var lastBackupDate: Date = Date.distantPast
    @AppStorage("storageCleanupEnabled") var storageCleanupEnabled: Bool = true
}
```

### Code Structure

#### Core Data Stack Implementation

```swift
import CoreData
import Combine

class CoreDataStack: ObservableObject {
    static let shared = CoreDataStack()

    lazy var persistentContainer: NSPersistentContainer = {
        let container = NSPersistentContainer(name: "SmilePileDataModel")
        container.loadPersistentStores { _, error in
            if let error = error as NSError? {
                fatalError("Unresolved Core Data error \(error), \(error.userInfo)")
            }
        }
        container.viewContext.automaticallyMergesChangesFromParent = true
        return container
    }()

    var viewContext: NSManagedObjectContext {
        persistentContainer.viewContext
    }

    func newBackgroundContext() -> NSManagedObjectContext {
        persistentContainer.newBackgroundContext()
    }

    func save() {
        let context = persistentContainer.viewContext
        if context.hasChanges {
            do {
                try context.save()
            } catch {
                let nsError = error as NSError
                fatalError("Unresolved Core Data save error \(nsError), \(nsError.userInfo)")
            }
        }
    }

    func saveContext(_ context: NSManagedObjectContext) throws {
        if context.hasChanges {
            try context.save()
        }
    }
}
```

#### Photo Repository Implementation

```swift
import CoreData
import Combine
import UIKit

class PhotoRepositoryImpl: PhotoRepository {
    private let coreDataStack: CoreDataStack
    private let backgroundContext: NSManagedObjectContext

    init(coreDataStack: CoreDataStack = CoreDataStack.shared) {
        self.coreDataStack = coreDataStack
        self.backgroundContext = coreDataStack.newBackgroundContext()
    }

    func insertPhoto(_ photo: Photo) async throws -> Int64 {
        return try await withCheckedThrowingContinuation { continuation in
            backgroundContext.perform {
                do {
                    guard photo.categoryId > 0 else {
                        throw PhotoRepositoryError.invalidCategory("Photo must have a valid category. Category ID: \(photo.categoryId)")
                    }

                    let photoEntity = PhotoEntity(context: self.backgroundContext)
                    photoEntity.id = photo.id == 0 ? UUID().uuidString : String(photo.id)
                    photoEntity.uri = photo.path
                    photoEntity.categoryId = photo.categoryId
                    photoEntity.timestamp = photo.createdAt
                    photoEntity.isFavorite = photo.isFavorite

                    try self.coreDataStack.saveContext(self.backgroundContext)
                    continuation.resume(returning: photo.categoryId)
                } catch {
                    continuation.resume(throwing: PhotoRepositoryError.insertFailed("Failed to insert photo: \(error.localizedDescription)"))
                }
            }
        }
    }

    func insertPhotos(_ photos: [Photo]) async throws {
        return try await withCheckedThrowingContinuation { continuation in
            backgroundContext.perform {
                do {
                    let invalidPhotos = photos.filter { $0.categoryId <= 0 }
                    guard invalidPhotos.isEmpty else {
                        throw PhotoRepositoryError.invalidCategory("All photos must have valid categories. \(invalidPhotos.count) photos have invalid category IDs.")
                    }

                    for photo in photos {
                        let photoEntity = PhotoEntity(context: self.backgroundContext)
                        photoEntity.id = photo.id == 0 ? UUID().uuidString : String(photo.id)
                        photoEntity.uri = photo.path
                        photoEntity.categoryId = photo.categoryId
                        photoEntity.timestamp = photo.createdAt
                        photoEntity.isFavorite = photo.isFavorite
                    }

                    try self.coreDataStack.saveContext(self.backgroundContext)
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: PhotoRepositoryError.insertFailed("Failed to insert photos: \(error.localizedDescription)"))
                }
            }
        }
    }

    func getAllPhotosPublisher() -> AnyPublisher<[Photo], Error> {
        let request: NSFetchRequest<PhotoEntity> = PhotoEntity.fetchRequest()
        request.sortDescriptors = [NSSortDescriptor(keyPath: \PhotoEntity.timestamp, ascending: false)]

        return coreDataStack.viewContext.publisher(for: request)
            .map { entities in
                entities.compactMap { self.entityToPhoto($0) }
            }
            .mapError { error in
                PhotoRepositoryError.fetchFailed("Failed to fetch photos: \(error.localizedDescription)")
            }
            .eraseToAnyPublisher()
    }

    func searchPhotosWithFilters(
        searchQuery: String,
        startDate: Int64,
        endDate: Int64,
        favoritesOnly: Bool?,
        categoryId: Int64?
    ) -> AnyPublisher<[Photo], Error> {
        let request: NSFetchRequest<PhotoEntity> = PhotoEntity.fetchRequest()

        var predicates: [NSPredicate] = []

        // Text search predicate
        if !searchQuery.isEmpty {
            predicates.append(NSPredicate(format: "uri CONTAINS[cd] %@", searchQuery))
        }

        // Date range predicate
        if startDate > 0 || endDate < Int64.max {
            predicates.append(NSPredicate(format: "timestamp BETWEEN %@", [startDate, endDate]))
        }

        // Favorites predicate
        if let favoritesOnly = favoritesOnly {
            predicates.append(NSPredicate(format: "isFavorite == %@", NSNumber(value: favoritesOnly)))
        }

        // Category predicate
        if let categoryId = categoryId, categoryId > 0 {
            predicates.append(NSPredicate(format: "categoryId == %ld", categoryId))
        }

        request.predicate = NSCompoundPredicate(andPredicateWithSubpredicates: predicates)
        request.sortDescriptors = [NSSortDescriptor(keyPath: \PhotoEntity.timestamp, ascending: false)]

        return coreDataStack.viewContext.publisher(for: request)
            .map { entities in
                entities.compactMap { self.entityToPhoto($0) }
            }
            .mapError { error in
                PhotoRepositoryError.fetchFailed("Failed to search photos with filters: \(error.localizedDescription)")
            }
            .eraseToAnyPublisher()
    }

    private func entityToPhoto(_ entity: PhotoEntity) -> Photo? {
        guard let id = entity.id, let uri = entity.uri else { return nil }

        return Photo(
            id: Int64(id) ?? 0,
            path: uri,
            categoryId: entity.categoryId,
            name: URL(fileURLWithPath: uri).deletingPathExtension().lastPathComponent,
            isFromAssets: false,
            createdAt: entity.timestamp,
            fileSize: 0, // Calculate if needed
            width: 0,    // Calculate if needed
            height: 0,   // Calculate if needed
            isFavorite: entity.isFavorite
        )
    }
}
```

#### Category Repository Implementation

```swift
class CategoryRepositoryImpl: CategoryRepository {
    private let coreDataStack: CoreDataStack
    private let backgroundContext: NSManagedObjectContext

    init(coreDataStack: CoreDataStack = CoreDataStack.shared) {
        self.coreDataStack = coreDataStack
        self.backgroundContext = coreDataStack.newBackgroundContext()
    }

    func getAllCategoriesPublisher() -> AnyPublisher<[Category], Error> {
        let request: NSFetchRequest<CategoryEntity> = CategoryEntity.fetchRequest()
        request.sortDescriptors = [NSSortDescriptor(keyPath: \CategoryEntity.createdAt, ascending: true)]

        return coreDataStack.viewContext.publisher(for: request)
            .map { entities in
                entities.compactMap { self.entityToCategory($0) }
            }
            .mapError { error in
                CategoryRepositoryError.fetchFailed("Failed to fetch categories: \(error.localizedDescription)")
            }
            .eraseToAnyPublisher()
    }

    func initializeDefaultCategories() async throws {
        return try await withCheckedThrowingContinuation { continuation in
            backgroundContext.perform {
                do {
                    let request: NSFetchRequest<CategoryEntity> = CategoryEntity.fetchRequest()
                    let existingCount = try self.backgroundContext.count(for: request)

                    if existingCount == 0 {
                        let defaultCategories = Category.getDefaultCategories()
                        for category in defaultCategories {
                            let entity = CategoryEntity(context: self.backgroundContext)
                            entity.id = category.id
                            entity.displayName = category.displayName
                            entity.colorHex = category.colorHex ?? "#4CAF50"
                            entity.position = Int32(category.position)
                            entity.isDefault = category.isDefault
                            entity.createdAt = category.createdAt
                        }

                        try self.coreDataStack.saveContext(self.backgroundContext)
                    }

                    continuation.resume()
                } catch {
                    continuation.resume(throwing: CategoryRepositoryError.initializationFailed("Failed to initialize default categories: \(error.localizedDescription)"))
                }
            }
        }
    }

    private func entityToCategory(_ entity: CategoryEntity) -> Category? {
        guard let displayName = entity.displayName, let colorHex = entity.colorHex else { return nil }

        let name = displayName.lowercased().replacingOccurrences(of: " ", with: "_")

        return Category(
            id: entity.id,
            name: name,
            displayName: displayName,
            position: Int(entity.position),
            iconResource: nil,
            colorHex: colorHex,
            isDefault: entity.isDefault,
            createdAt: entity.createdAt
        )
    }
}
```

#### Custom Error Types

```swift
enum PhotoRepositoryError: LocalizedError {
    case invalidCategory(String)
    case insertFailed(String)
    case updateFailed(String)
    case deleteFailed(String)
    case fetchFailed(String)
    case notFound(String)

    var errorDescription: String? {
        switch self {
        case .invalidCategory(let message),
             .insertFailed(let message),
             .updateFailed(let message),
             .deleteFailed(let message),
             .fetchFailed(let message),
             .notFound(let message):
            return message
        }
    }
}

enum CategoryRepositoryError: LocalizedError {
    case insertFailed(String)
    case updateFailed(String)
    case deleteFailed(String)
    case fetchFailed(String)
    case notFound(String)
    case initializationFailed(String)

    var errorDescription: String? {
        switch self {
        case .insertFailed(let message),
             .updateFailed(let message),
             .deleteFailed(let message),
             .fetchFailed(let message),
             .notFound(let message),
             .initializationFailed(let message):
            return message
        }
    }
}
```

#### Domain Model Extensions

```swift
extension Category {
    static func getDefaultCategories() -> [Category] {
        return [
            Category(
                id: 1,
                name: "family",
                displayName: "Family",
                position: 0,
                iconResource: nil,
                colorHex: "#E91E63",
                isDefault: true
            ),
            Category(
                id: 2,
                name: "cars",
                displayName: "Cars",
                position: 1,
                iconResource: nil,
                colorHex: "#F44336",
                isDefault: true
            ),
            Category(
                id: 3,
                name: "games",
                displayName: "Games",
                position: 2,
                iconResource: nil,
                colorHex: "#9C27B0",
                isDefault: true
            ),
            Category(
                id: 4,
                name: "sports",
                displayName: "Sports",
                position: 3,
                iconResource: nil,
                colorHex: "#4CAF50",
                isDefault: true
            )
        ]
    }
}
```

## Validation Checklist

- [ ] **All entity fields mapped**: PhotoEntity (12 fields) and CategoryEntity (6 fields) Core Data models created with identical field types and constraints
- [ ] **All DAO queries implemented**: 31 PhotoDao methods and 18 CategoryDao methods converted to NSFetchRequest configurations with equivalent predicates
- [ ] **Repository pattern complete**: PhotoRepository (21 methods) and CategoryRepository (10 methods) protocols defined with async/await implementations
- [ ] **File storage working**: Documents directory structure with photos/ and thumbnails/ subdirectories, image processing, and storage management methods
- [ ] **Migration path defined**: Core Data model versioning with migration strategies for schema changes and default data preservation
- [ ] **Default data loaded**: Category initialization with proper color codes, positioning, and one-time setup verification
- [ ] **Entity relationships configured**: Foreign key relationship from Photo to Category with proper cascade delete rules
- [ ] **Reactive data flows**: Combine publishers for all Flow-based Android methods with proper error handling
- [ ] **Search functionality**: Complex predicate configurations for text search, date filtering, category filtering, and combined criteria
- [ ] **Error handling**: Custom repository exception types with descriptive error messages and proper error propagation
- [ ] **Performance optimizations**: Background context for write operations, batch operations, and relationship prefetching
- [ ] **Security integration**: Encrypted field support with proper key management for sensitive child-related data
- [ ] **Storage management**: Photo import, thumbnail generation, storage usage calculation, and cleanup operations
- [ ] **Testing infrastructure**: Unit tests for repository methods, Core Data operations, and migration scenarios

## Atlas Workflow Integration

Use this **EXACT prompt** with Atlas for implementing the complete iOS data storage layer:

```
ATLAS iOS DATA STORAGE IMPLEMENTATION

Implement the complete data storage layer for iOS SmilePile following the Android architecture analyzed in the comprehensive implementation guide. This is a critical foundational feature that must be implemented with 100% functional parity to the Android version.

SCOPE: Complete data storage implementation including Core Data models, repositories, file management, and initialization.

REQUIREMENTS:
1. Create Core Data model (SmilePileDataModel.xcdatamodeld) with:
   - PhotoEntity: 12 fields including encrypted sensitive data fields
   - CategoryEntity: 6 fields with auto-increment ID
   - Relationship: Photo.category → Category (cascade delete)

2. Implement PhotoRepository with all 21 methods:
   - CRUD operations with async/await
   - Category-based queries with Combine publishers
   - Search and filtering with complex NSPredicate logic
   - File path and ID-based operations
   - Batch operations for multiple photos

3. Implement CategoryRepository with all 10 methods:
   - CRUD operations for category management
   - Default category initialization (Family, Cars, Games, Sports)
   - Name-based lookups and validation
   - Reactive data flows with publishers

4. Create StorageManager equivalent with:
   - Documents/photos and Documents/thumbnails directory structure
   - Photo import with automatic resizing (max 2048px, 90% quality)
   - Thumbnail generation (300px square, 85% quality)
   - Storage usage calculation and cleanup operations
   - File naming convention: IMG_YYYYMMDD_HHMMSS_<uuid>.jpg

5. Implement Core Data stack with:
   - NSPersistentContainer configuration
   - Background context for write operations
   - Main context for UI operations
   - Proper error handling and migration support

6. Create domain models (Photo, Category) with:
   - Identical field structure to Android models
   - Default category definitions with exact color codes
   - Computed properties (displayName, isValid)
   - Proper validation rules

7. Error handling with custom types:
   - PhotoRepositoryError with descriptive cases
   - CategoryRepositoryError with operation-specific errors
   - Proper error propagation through async methods

VALIDATION REQUIREMENTS:
- All Android DAO methods must have iOS equivalents
- Default categories must match exactly (names, colors, positions)
- File storage must use internal Documents directory only
- Search queries must support text, date, category, and favorite filters
- Entity relationships must enforce cascade delete behavior
- Migration support must be implemented for future schema changes

ARCHITECTURE CONSTRAINTS:
- Use Repository pattern with protocol abstraction
- Implement reactive data flows with Combine
- Use background contexts for all write operations
- Follow iOS naming conventions and Swift best practices
- Ensure thread safety for Core Data operations

DELIVERABLES:
1. SmilePileDataModel.xcdatamodeld with complete entity definitions
2. PhotoRepository + PhotoRepositoryImpl with full method implementation
3. CategoryRepository + CategoryRepositoryImpl with initialization logic
4. StorageManager with photo and thumbnail management
5. CoreDataStack with proper container configuration
6. Domain models with validation and default data
7. Custom error types with descriptive messaging
8. Unit tests for critical repository operations

This implementation is foundational for all photo management features. Ensure 100% compatibility with Android data structures and behavior. Test thoroughly with multiple photos and categories before marking complete.
```

This prompt provides the exact specifications needed for Atlas to implement the complete iOS data storage layer with full Android compatibility. The implementation will serve as the foundation for all photo management functionality in the iOS version of SmilePile.