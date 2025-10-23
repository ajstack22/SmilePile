# SmilePile Photo Gallery Implementation Research

## Executive Summary

SmilePile uses distinctly different gallery approaches for iOS and Android:
- **iOS**: Vertical **stack layout** (vertical scrolling, one photo per row, full-width)
- **Android**: Vertical **stack layout** (vertical scrolling, one photo per row, full-width)

Both platforms currently use **stack-based layouts** rather than traditional grids, with single-column, full-width photo display. However, they employ different image loading and caching strategies optimized for their respective ecosystems.

---

## iOS Gallery View Implementation

### Main Files
- **Gallery View**: `/Users/adamstack/SmilePile/ios/SmilePile/Views/OptimizedPhotoGalleryView.swift` (632 lines)
- **View Model**: `/Users/adamstack/SmilePile/ios/SmilePile/ViewModels/PhotoGalleryViewModel.swift` (410 lines)
- **Thumbnail Component**: `/Users/adamstack/SmilePile/ios/SmilePile/Views/Components/PhotoThumbnailView.swift` (234 lines)
- **Image Cache**: `/Users/adamstack/SmilePile/ios/SmilePile/Utils/OptimizedImageCache.swift` (370 lines)
- **Kids Gallery**: `/Users/adamstack/SmilePile/ios/SmilePile/Views/KidsMode/KidsModeGalleryView.swift`
- **Data Model**: `/Users/adamstack/SmilePile/ios/SmilePile/Models/Photo.swift` (183 lines)

### Gallery Layout Architecture

#### Stack-Based Display (Parent Mode)
**File**: `OptimizedPhotoGalleryView.swift:51-76`
```swift
private struct OptimizedPhotoStackView: View {
    var body: some View {
        if photos.isEmpty {
            OptimizedEmptyPhotoStackState()
        } else {
            ScrollView {
                LazyVStack(spacing: 12) {  // Vertical stack with 12pt spacing
                    ForEach(photos) { photo in
                        OptimizedPhotoStackItem(...)
                    }
                }
                .padding(16)
            }
        }
    }
}
```

**Key Specifications**:
- **Layout Type**: Vertical stack (LazyVStack)
- **Spacing**: 12 points between photos
- **Padding**: 16 points on all sides
- **Aspect Ratio**: 4:3 (OptimizedPhotoGalleryView.swift:144)
- **Scrolling**: Vertical, full-width items

#### Individual Photo Item
**File**: `OptimizedPhotoGalleryView.swift:79-150`
```swift
private struct OptimizedPhotoStackItem: View {
    var body: some View {
        VStack(spacing: 0) {
            if FileManager.default.fileExists(atPath: photo.path) {
                AsyncImage(url: URL(fileURLWithPath: photo.path)) { phase in
                    switch phase {
                    case .empty:
                        Rectangle()
                            .fill(Color.gray.opacity(0.2))
                            .overlay(ProgressView())
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(4/3, contentMode: .fill)
                            .clipped()
                    case .failure(let error):
                        // Error state display
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(4/3, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
```

**Rendering Details**:
- **Width**: Full width (maxWidth: .infinity)
- **Aspect Ratio**: 4:3
- **Corner Radius**: 12 points
- **Image Loading**: AsyncImage with file URL
- **Content Mode**: .fill (crops to aspect ratio)
- **Loading State**: Gray placeholder with ProgressView spinner
- **Error State**: Exclamation icon + "Failed to load" text

### Kids Mode Gallery Layout

**File**: `KidsModeGalleryView.swift:43-63`
```swift
if displayedPhotos.isEmpty {
    KidsEmptyGalleryView()
} else {
    ScrollView {
        LazyVStack(spacing: 12) {
            ForEach(Array(displayedPhotos.enumerated()), id: \.element.id) { index, photo in
                PhotoGridItem(photo: photo)
                    .id(photo.id)
                    .onTapGesture { ... }
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, 16)
    }
}
```

**Identical Layout** to parent mode but with slightly different padding (8pt top vs 16pt all sides).

### Image Loading and Caching - iOS

#### Image Loading Mechanism
**Primary Method**: Native SwiftUI `AsyncImage` with file URLs
- **File**: `OptimizedPhotoGalleryView.swift:92`
- Direct file path loading: `URL(fileURLWithPath: photo.path)`
- No external library dependencies (pure SwiftUI)

#### Optimized Image Cache (Custom Implementation)
**File**: `/Users/adamstack/SmilePile/ios/SmilePile/Utils/OptimizedImageCache.swift` (370 lines)

**Architecture**:
- **Thread Safety**: Swift actor-based (concurrent access safe)
- **Storage**: NSCache with delegate for eviction callbacks
- **Configuration**:
  - Max cache size: 100MB (Configuration.maxCacheSize)
  - Max items: 200 items (Configuration.maxItemCount)
  - Cache expiration: 1 hour (Configuration.cacheExpirationSeconds)

**Cache Entry Structure** (OptimizedImageCache.swift:20-29):
```swift
private struct CacheEntry {
    let image: UIImage
    let cost: Int              // Bytes used
    let timestamp: Date        // For expiration
    let accessCount: Int       // For LRU eviction
}
```

**Key Methods**:
- `image(for key: String)` - Retrieve cached image with expiration check
- `store(_ image: UIImage, for key: String, cost: Int?)` - Store with LRU eviction
- `loadImage(from url: URL, cacheKey: String?)` - Load from disk with caching
- `prefetchImages(for photoIds:)` - Batch preload with task grouping
- `clearCache()` - Full cache clear with immediate memory release

**Memory Management**:
- **LRU Eviction**: Evicts least recently used entries when cache exceeds limits
- **Memory Warnings**: Aggressive cleanup (75% removal) on UIApplication.didReceiveMemoryWarningNotification
- **Available Memory Check**: Skips caching if system has < 100MB free memory
- **Autoreleasepool**: Used for batch operations to ensure immediate memory release

**Statistics Tracking** (OptimizedImageCache.swift:280-288):
```swift
func getCacheStats() -> CacheStatistics {
    return CacheStatistics(
        itemCount: cacheEntries.count,
        totalSize: currentMemoryUsage,
        hitRate: cacheHits > 0 ? Double(cacheHits) / Double(cacheHits + cacheMisses) : 0,
        hits: cacheHits,
        misses: cacheMisses
    )
}
```

#### Thumbnail Loading - PhotoThumbnailView
**File**: `/Users/adamstack/SmilePile/ios/SmilePile/Views/Components/PhotoThumbnailView.swift` (234 lines)

**ThumbnailLoader Class** (PhotoThumbnailView.swift:99-203):
```swift
@MainActor
class ThumbnailLoader: ObservableObject {
    @Published var image: UIImage?
    @Published var isLoading = false
    @Published var error: Error?
    
    func load(photo: Photo, size: ThumbnailSize) { ... }
    func cancel() { ... }
}
```

**Thumbnail Sizes** (OptimizedImageCache.swift:350-370):
- **Small**: 100x100 pixels (grid view)
- **Medium**: 200x200 pixels (list view)
- **Large**: 400x400 pixels (preview)

**Loading Process**:
1. Check cache first (cache key: "\(photo.id)_\(size)")
2. Load from disk if cached
3. Generate thumbnail if missing using SafeThumbnailGenerator
4. Store in cache for reuse

### Photo Gallery ViewModel - Performance Optimizations
**File**: `/Users/adamstack/SmilePile/ios/SmilePile/ViewModels/PhotoGalleryViewModel.swift` (410 lines)

#### Virtual Scrolling Implementation
**Configuration** (PhotoGalleryViewModel.swift:20-27):
```swift
struct Configuration {
    static let preloadRowCount = 3
    static let itemsPerRow = 3
    static let itemsPerRowIPad = 5
    static let virtualScrollBuffer = 20    // Items to preload above/below visible area
    static let memoryWarningThreshold = 80 // MB
    static let scrollDebounceDelay: TimeInterval = 0.1
}
```

**Note**: Despite itemsPerRow=3 configuration, actual gallery uses LazyVStack (single column). Configuration appears to be legacy or for alternative layouts.

#### Visible Range Calculation
**File**: `PhotoGalleryViewModel.swift:221-246`
```swift
private func updateVisibleRange(scrollOffset: CGFloat, containerHeight: CGFloat) {
    let itemHeight: CGFloat = UIScreen.main.bounds.width / CGFloat(itemsPerRow)
    let rowHeight = itemHeight + 2  // Including spacing
    
    let firstVisibleRow = max(0, Int(scrollOffset / rowHeight))
    let visibleRows = Int(ceil(containerHeight / rowHeight)) + 1
    
    visibleRange = firstIndex..<(lastIndex + 1)
    preloadRange = preloadFirst..<(preloadLast + 1)
    
    // Update visible photos for virtual scrolling
    visiblePhotos = Array(filteredPhotos[visibleRange])
}
```

#### Smart Preloading
**File**: `PhotoGalleryViewModel.swift:261-289`
- Preloads photos in visible range + buffer (20 items)
- Skips preloading during rapid scrolling (velocity > 1000 pts/s)
- Batch loading with withTaskGroup
- Automatic cancellation outside visible range

#### Memory Management
**File**: `PhotoGalleryViewModel.swift:320-394`
- Real-time memory monitoring (1-second intervals)
- Aggressive cache reduction on memory warnings (75% eviction)
- Scroll velocity tracking to avoid preloading during fast scrolling
- Selective cache clearing for non-visible items

### Photo Data Model - iOS
**File**: `/Users/adamstack/SmilePile/ios/SmilePile/Models/Photo.swift` (61 lines)

```swift
public struct Photo: Identifiable, Codable, Equatable {
    public let id: Int64
    public let path: String
    public let categoryId: Int64
    public let name: String
    public let isFromAssets: Bool
    public let createdAt: Int64
    public let fileSize: Int64
    public let width: Int          // Image dimensions
    public let height: Int
    
    public var displayName: String {
        if !name.isEmpty {
            return name
        }
        let url = URL(fileURLWithPath: path)
        return url.deletingPathExtension().lastPathComponent
    }
}
```

**Metadata Shown in Gallery**: None visible in main gallery view. Dimensions stored but not displayed in list view.

---

## Android Gallery View Implementation

### Main Files
- **Gallery Screen**: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/screens/PhotoGalleryScreen.kt` (680 lines)
- **View Model**: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/viewmodels/PhotoGalleryViewModel.kt` (461 lines)
- **Photo Stack Component**: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/components/gallery/PhotoStackComponent.kt` (206 lines)
- **Enhanced Photo Item**: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/components/EnhancedPhotoGridItem.kt` (172 lines)
- **Data Model**: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/data/models/Photo.kt` (58 lines)

### Gallery Layout Architecture

#### Stack-Based Display (Parent Mode)
**File**: `PhotoStackComponent.kt:39-100`
```kotlin
@Composable
fun PhotoStackComponent(
    photos: List<Photo>,
    selectedPhotos: Set<Long> = emptySet(),
    isSelectionMode: Boolean = false,
    showEditActions: Boolean = false,
    onPhotoClick: (Photo) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    if (photos.isEmpty()) {
        EmptyPhotoStackState(modifier = modifier)
    } else {
        PhotoStack(...)
    }
}

@Composable
private fun PhotoStack(...) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(photos) { photo ->
            PhotoStackItem(...)
        }
    }
}
```

**Key Specifications**:
- **Layout Type**: Vertical stack (LazyColumn)
- **Spacing**: 12dp between photos
- **Padding**: 16dp content padding
- **Aspect Ratio**: 4:3 (PhotoStackComponent.kt:151)
- **Scrolling**: Vertical, full-width items

#### Individual Photo Item
**File**: `PhotoStackComponent.kt:104-172`
```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoStackItem(
    photo: Photo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    showEditActions: Boolean,
    onPhotoClick: () -> Unit,
    onPhotoLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = onPhotoClick,
                onLongClick = onPhotoLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(photo.path))
                    .crossfade(true)
                    .build(),
                contentDescription = photo.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp))
            )
            
            // Selection checkbox (if in selection mode)
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onPhotoClick() },
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                )
            }
        }
    }
}
```

**Rendering Details**:
- **Width**: Full width (fillMaxWidth)
- **Aspect Ratio**: 4:3
- **Corner Radius**: 12dp
- **Image Loading**: Coil AsyncImage with File path
- **Content Mode**: ContentScale.Crop (crops to aspect ratio)
- **Card Elevation**: 4dp default, 8dp when selected
- **Scale Animation**: Animated to 0.95 on press
- **Selection Indicator**: Checkbox in top-left corner (selection mode)

### Image Loading and Caching - Android

#### Image Loading Library: Coil
**File**: `android/app/build.gradle.kts`
```gradle
// Image Loading (using Coil for modern Kotlin-first approach)
implementation("io.coil-kt:coil:2.5.0")
implementation("io.coil-kt:coil-compose:2.5.0")
```

**Coil Image Request** (PhotoStackComponent.kt:142-153):
```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(File(photo.path))
        .crossfade(true)        // Smooth fade-in animation
        .memoryCacheKey("photo_${photo.id}")
        .diskCacheKey("photo_${photo.id}")
        .build(),
    contentDescription = photo.displayName,
    contentScale = ContentScale.Crop,
    modifier = Modifier...
)
```

**Coil Features Utilized**:
- **Memory Caching**: Automatic with memoryCacheKey
- **Disk Caching**: Automatic with diskCacheKey
- **Crossfade**: 300ms fade transition on load
- **Efficient Loading**: Optimized for Compose with async loading
- **Configuration**: Version 2.5.0

#### Enhanced Photo Grid Item (Alternative Component)
**File**: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/components/EnhancedPhotoGridItem.kt` (172 lines)

**Coil Image Request Optimization** (EnhancedPhotoGridItem.kt:61-67):
```kotlin
val imageRequest = ImageRequest.Builder(context)
    .data(imageModel)
    .crossfade(true)
    .crossfade(300)              // 300ms fade
    .memoryCacheKey("photo_${photo.id}")
    .diskCacheKey("photo_${photo.id}")
    .build()
```

**Performance Features**:
- Error state handling with ImageLoadError composable
- Shimmer loading animation while loading
- Smooth visibility animations for selection overlay
- Scale animation on press (0.95 scale)

### Photo Gallery ViewModel - Android
**File**: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/ui/viewmodels/PhotoGalleryViewModel.kt` (461 lines)

#### State Management
**File**: `PhotoGalleryViewModel.kt:44-194`
```kotlin
@HiltViewModel
class PhotoGalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val categoryRepository: CategoryRepository,
    private val photoOperationsManager: PhotoOperationsManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel()
```

**Key StateFlows**:
- `photos`: StateFlow of filtered photos based on selected categories
- `categories`: StateFlow of all available categories
- `selectedCategoryIds`: StateFlow of currently selected category filter(s)
- `isLoading`: StateFlow of loading state
- `error`: StateFlow of error messages
- `selectedPhotos`: StateFlow for multi-select mode

#### Photo Filtering Logic
**File**: `PhotoGalleryViewModel.kt:168-194`
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
val photos: StateFlow<List<Photo>> = _selectedCategoryIds
    .flatMapLatest { categoryIds ->
        when {
            categoryIds.isEmpty() -> {
                // No filter - show all photos
                photoRepository.getAllPhotosFlow()
            }
            categoryIds.size == 1 -> {
                // Single category filter
                photoRepository.getPhotosByCategoryFlow(categoryIds.first())
            }
            else -> {
                // Multiple category filter - photos in ANY of the selected categories
                photoRepository.getPhotosInCategoriesFlow(categoryIds.toList())
            }
        }
    }
```

### Photo Data Model - Android
**File**: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/data/models/Photo.kt` (58 lines)

```kotlin
@Parcelize
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Photo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "is_from_assets")
    val isFromAssets: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0,

    @ColumnInfo(name = "width")
    val width: Int = 0,

    @ColumnInfo(name = "height")
    val height: Int = 0
) : Parcelable {

    val displayName: String
        get() = name.ifEmpty {
            path.substringAfterLast("/").substringBeforeLast(".")
        }

    val isValid: Boolean
        get() = path.isNotEmpty() && categoryId > 0
}
```

**Metadata Shown in Gallery**: None visible in main gallery view. Dimensions stored but not displayed in list view.

---

## Comparative Analysis

| Aspect | iOS | Android |
|--------|-----|---------|
| **Layout Type** | LazyVStack (vertical stack) | LazyColumn (vertical stack) |
| **Spacing** | 12pt | 12dp |
| **Padding** | 16pt all sides | 16dp content padding |
| **Aspect Ratio** | 4:3 | 4:3 |
| **Corner Radius** | 12pt | 12dp |
| **Image Library** | Native AsyncImage | Coil 2.5.0 |
| **Image Source** | File URL (file://) | File path (File object) |
| **Caching** | Custom OptimizedImageCache (NSCache) | Coil built-in (memory + disk) |
| **Cache Size** | 100MB max, 200 items | Coil default |
| **Thumbnail Sizes** | Small (100px), Medium (200px), Large (400px) | Not explicitly configured |
| **Memory Management** | Active monitoring, LRU eviction, aggressive warning handling | Coil handles automatically |
| **Preloading** | Virtual scrolling with buffer, smart velocity detection | Coil handles automatically |
| **Selection UI** | Title bar appears in selection mode | Bottom app bar with actions |
| **Performance Focus** | Virtual scrolling, memory efficiency | Smooth animations, Compose optimization |

---

## Performance Considerations

### iOS Performance Optimizations

1. **Virtual Scrolling**
   - Maintains visible range + 20-item buffer for preloading
   - Skips preloading during rapid scrolling (>1000 pts/sec)
   - Reduces rendering load by limiting visible items

2. **Memory Management**
   - Real-time memory monitoring (1-second intervals)
   - Aggressive cache cleanup (75% eviction) on memory warnings
   - LRU (Least Recently Used) eviction strategy
   - Skips caching if <100MB system memory available

3. **Image Optimization**
   - Three-tier thumbnail sizing (100px, 200px, 400px)
   - Selective cache based on visibility
   - 1-hour cache expiration to manage stale data
   - Autoreleasepool usage for batch memory release

### Android Performance Optimizations

1. **Coil Library Benefits**
   - Automatic memory and disk caching
   - Efficient bitmap reuse
   - Crossfade animations (300ms)
   - Built-in coroutine support

2. **Compose Optimizations**
   - LazyColumn automatically handles rendering efficiency
   - Smooth animations (press scale 0.95)
   - Shimmer loading animation
   - Error state handling with retry

---

## Key Differences in Detail

### 1. Image Cache Implementation
**iOS**: Custom actor-based cache with detailed tracking
- Entry metadata: cost, timestamp, access count
- Manual eviction strategies
- Memory pressure monitoring
- Hit/miss statistics

**Android**: Delegated to Coil library
- Automatic cache management
- Less manual control
- Simpler, battle-tested solution

### 2. Photo Metadata Storage
Both platforms store:
- Photo dimensions (width, height)
- File size
- Creation timestamp
- Category association
- Asset flag (from demo assets)

**None displayed** in primary gallery view.

### 3. Scrolling Experience
**iOS**: 
- Stack-based with explicit spacing
- Can track and optimize for scroll velocity
- Virtual scrolling visibility calculations

**Android**:
- Stack-based with card elevation changes
- LazyColumn handles optimization internally
- Press animation provides feedback

---

## File Size and Dimensions

### iOS
- Photo dimensions: Stored as Int (width, height)
- File size: Stored as Int64 bytes
- Display: 4:3 aspect ratio, full width container

### Android
- Photo dimensions: Stored as Int (width, height)
- File size: Stored as Long bytes
- Display: 4:3 aspect ratio (4f/3f), full width container

---

## Summary

**Gallery Architecture**: Both platforms use identical **single-column vertical stack layouts** with 4:3 photo aspect ratios, contrary to typical multi-column grid implementations.

**iOS Strengths**:
- Fine-grained memory management
- Virtual scrolling with smart preloading
- Detailed cache statistics and monitoring
- Explicit performance tuning

**Android Strengths**:
- Simplified caching (Coil handles details)
- Smooth Compose animations and transitions
- Built-in error handling and retry logic
- Less code, more functionality

**Consistent UX**: Despite different implementations, both platforms provide:
- Identical 4:3 aspect ratios
- Same vertical spacing (12pt/dp)
- Same corner radius (12pt/dp)
- Identical padding (16pt/dp)
- Similar loading/error states
- Category filtering at top

