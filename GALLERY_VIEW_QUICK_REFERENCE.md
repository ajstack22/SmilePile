# SmilePile Gallery View - Quick Reference

## Key File Locations

### iOS
| Component | File | Lines |
|-----------|------|-------|
| Main Gallery | `ios/SmilePile/Views/OptimizedPhotoGalleryView.swift` | 632 |
| Gallery ViewModel | `ios/SmilePile/ViewModels/PhotoGalleryViewModel.swift` | 410 |
| Thumbnail View | `ios/SmilePile/Views/Components/PhotoThumbnailView.swift` | 234 |
| Image Cache | `ios/SmilePile/Utils/OptimizedImageCache.swift` | 370 |
| Kids Gallery | `ios/SmilePile/Views/KidsMode/KidsModeGalleryView.swift` | - |
| Photo Model | `ios/SmilePile/Models/Photo.swift` | 61 |

### Android
| Component | File | Lines |
|-----------|------|-------|
| Gallery Screen | `android/app/src/main/java/com/smilepile/ui/screens/PhotoGalleryScreen.kt` | 680 |
| Gallery ViewModel | `android/app/src/main/java/com/smilepile/ui/viewmodels/PhotoGalleryViewModel.kt` | 461 |
| Photo Stack Component | `android/app/src/main/java/com/smilepile/ui/components/gallery/PhotoStackComponent.kt` | 206 |
| Enhanced Photo Item | `android/app/src/main/java/com/smilepile/ui/components/EnhancedPhotoGridItem.kt` | 172 |
| Photo Model | `android/app/src/main/java/com/smilepile/data/models/Photo.kt` | 58 |

---

## Layout Specifications (Both Platforms Identical)

```
Layout Type:      Vertical Stack (LazyVStack / LazyColumn)
Columns:          1 (full-width)
Spacing:          12pt / 12dp
Padding:          16pt / 16dp
Aspect Ratio:     4:3
Corner Radius:    12pt / 12dp
Scrolling:        Vertical
```

---

## Image Loading & Caching

### iOS
- **Library**: Native SwiftUI AsyncImage (no external dependencies)
- **Source**: File URL (file://)
- **Caching**: Custom OptimizedImageCache actor
  - Max size: 100MB
  - Max items: 200
  - Expiration: 1 hour
  - Strategy: LRU with access tracking
  - Memory monitoring: 1-second intervals

### Android
- **Library**: Coil 2.5.0
- **Source**: File path (File object)
- **Caching**: Coil built-in (memory + disk)
  - Memory cache key: "photo_${id}"
  - Disk cache key: "photo_${id}"
  - Crossfade: 300ms

---

## Thumbnail Sizes (iOS)

- **Small**: 100x100 pixels (grid view)
- **Medium**: 200x200 pixels (list view)
- **Large**: 400x400 pixels (preview)

---

## Performance Features

### iOS
- Virtual scrolling with 20-item buffer
- Smart velocity detection (skip preload if >1000 pts/s)
- Real-time memory monitoring
- Aggressive cache cleanup on memory warnings (75% removal)
- Task grouping for batch preloading

### Android
- LazyColumn automatic optimization
- Coil memory reuse
- Shimmer loading animation
- Error state handling
- Scale animations (0.95 on press)

---

## Photo Metadata Stored (Both Platforms)

```
- id: Long
- path: String
- categoryId: Long
- name: String
- isFromAssets: Boolean (demo mode flag)
- createdAt: Long (timestamp ms)
- fileSize: Long
- width: Int (image dimension)
- height: Int (image dimension)
- displayName: String (computed from name or path)
- isValid: Boolean (computed)
```

**Displayed in Gallery**: None of the above metadata is shown in the gallery view.

---

## Photo Item Rendering

### iOS (OptimizedPhotoGalleryView.swift:79-150)
```swift
.frame(maxWidth: .infinity)
.aspectRatio(4/3, contentMode: .fit)
.clipShape(RoundedRectangle(cornerRadius: 12))

// States:
// Loading: Gray placeholder + ProgressView
// Success: Image with aspect ratio .fill + clipped
// Error: Exclamation icon + "Failed to load" text
```

### Android (PhotoStackComponent.kt:104-172)
```kotlin
.fillMaxWidth()
.aspectRatio(4f / 3f)
.clip(RoundedCornerShape(12.dp))

// States:
// Loading: Shimmer animation
// Success: Image with ContentScale.Crop
// Error: ImageLoadError composable
```

---

## Selection Mode Differences

### iOS
- Selection indicator: Appears in top bar
- UI: Title bar shows "Edit" / "Done" / "Select"
- Selection state: title bar indicates mode

### Android
- Selection indicator: Checkbox in top-left of card
- UI: Bottom app bar with action buttons (Edit, Move, Delete, Share)
- Selection state: Card background changes to primaryContainer

---

## Virtual Scrolling Configuration (iOS Only)

```swift
Configuration {
    preloadRowCount = 3
    itemsPerRow = 3 (unused - stack is single column)
    itemsPerRowIPad = 5 (unused)
    virtualScrollBuffer = 20
    memoryWarningThreshold = 80 MB
    scrollDebounceDelay = 0.1s
}
```

---

## Kids Mode Gallery

### iOS (KidsModeGalleryView.swift:43-63)
- Same LazyVStack layout
- Same 4:3 aspect ratio
- Padding: 16pt horizontal, 8pt top, 16pt bottom
- Tap opens fullscreen viewer

### Android (PhotoGalleryScreen.kt)
- Same LazyColumn layout
- Same 4:3 aspect ratio
- Uses PhotoStackComponent with same configuration

---

## Code References for Common Operations

### iOS - Load Photos
File: `OptimizedPhotoGalleryView.swift:207-209`
```swift
.task {
    await initializeView()
}
```

### iOS - Handle Memory Warning
File: `OptimizedPhotoGalleryView.swift:237-241`
```swift
.onReceive(NotificationCenter.default.publisher(for: UIApplication.didReceiveMemoryWarningNotification)) { _ in
    Task {
        await viewModel.clearCache()
    }
}
```

### Android - Filter Photos by Category
File: `PhotoGalleryViewModel.kt:168-194`
```kotlin
val photos: StateFlow<List<Photo>> = _selectedCategoryIds
    .flatMapLatest { categoryIds ->
        when {
            categoryIds.isEmpty() -> photoRepository.getAllPhotosFlow()
            categoryIds.size == 1 -> photoRepository.getPhotosByCategoryFlow(categoryIds.first())
            else -> photoRepository.getPhotosInCategoriesFlow(categoryIds.toList())
        }
    }
```

---

## Known Configurations

### Cache Limits
- **iOS**: 100MB total, 200 items max
- **Android**: Coil defaults (varies by device)

### Image Quality Settings
- **iOS**: Generated on-demand using SafeThumbnailGenerator
- **Android**: Coil handles bitmap optimization

### Rotation/Scaling
- **iOS**: .fill content mode, clipped to rounded rectangle
- **Android**: ContentScale.Crop to rectangle

---

## Performance Tuning Points

### iOS Optimization Levels
1. Scroll detection
2. Virtual range calculation
3. Preload buffer management
4. Memory monitoring
5. Cache eviction

### Android Optimization Levels
1. Coil memory cache
2. Coil disk cache
3. Crossfade animation
4. LazyColumn layout efficiency
5. Compose recomposition optimization

