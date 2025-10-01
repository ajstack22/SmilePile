# Test Coverage Improvement Plan
**Target: 30% Coverage for iOS and Android**

## Current Status

### Android
- **Current Coverage**: 12% (instruction coverage)
- **Target**: 30%
- **Gap**: +18 percentage points needed

### iOS
- **Current Coverage**: Unknown (tests don't compile)
- **Target**: 30%
- **Status**: Tests exist but reference deleted classes

---

## Coverage Analysis by Package (Android)

### ✅ Already Good (>30%)
- `com.smilepile.data.models`: 78% ✅
- `com.smilepile.theme`: 56% ✅
- `com.smilepile.data.entities`: 47% ✅
- `com.smilepile.storage`: 32% ✅
- `com.smilepile.security`: 31% ✅

### 🎯 Focus Areas (0-16% - High Impact)
1. **com.smilepile.data.backup**: 16% (largest package - 2,038 lines)
2. **com.smilepile.ui.viewmodels**: 0% (1,634 lines)
3. **com.smilepile.data.repository**: 0% (398 lines)
4. **com.smilepile.settings**: 0% (364 lines)
5. **com.smilepile.operations**: 0% (160 lines)

---

## Strategy to Reach 30%

### Phase 1: Fix iOS Tests (2-3 hours)
**Goal**: Get iOS tests compiling and running

**Issues to Fix**:
1. ~~`PhotoImportCoordinator` doesn't exist~~ - Remove or replace
2. Check for other missing classes in test files
3. Update test dependencies and imports

**iOS Test Files**:
- `PhotoImportSafetyTests.swift` - References non-existent coordinator
- `CategoryRepositoryTests.swift` - Check if valid
- `PINManagerTests.swift` - Check if valid
- `PhotoRepositoryTests.swift` - Check if valid
- `Core/Storage/StorageManagerTests.swift` - Check if valid
- `Core/Storage/ImageProcessorTests.swift` - Check if valid
- `Core/Data/CoreDataStackTests.swift` - Check if valid
- `Core/DI/DIContainerTests.swift` - Check if valid

**Actions**:
```swift
// Option 1: Remove PhotoImportCoordinator references
// Option 2: Create a PhotoImportSession wrapper if needed
// Option 3: Test SafeThumbnailGenerator and StorageManager directly
```

### Phase 2: Add Android ViewModel Tests (4-5 hours)
**Goal**: Bring ViewModels from 0% → 30%

**Priority ViewModels** (test business logic):
1. `PhotoGalleryViewModel` - Photo loading, filtering, selection
2. `CategoryViewModel` - Category CRUD operations
3. `BackupViewModel` - Backup/restore logic
4. `PhotoEditViewModel` - Edit operations
5. `SettingsViewModel` - Settings management

**Test Template**:
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU], manifest = Config.NONE)
class PhotoGalleryViewModelTest {

    @Test
    fun `loadPhotos should emit photos when repository succeeds`() = runTest {
        // Given
        val mockRepo = mock<PhotoRepository>()
        val viewModel = PhotoGalleryViewModel(mockRepo)

        // When
        viewModel.loadPhotos()

        // Then
        val photos = viewModel.photosFlow.value
        assertTrue(photos.isNotEmpty())
    }
}
```

### Phase 3: Add Android Repository Tests (2-3 hours)
**Goal**: Bring Repositories from 0% → 30%

**Priority Repositories**:
1. `PhotoRepositoryImpl` - Photo CRUD
2. `CategoryRepositoryImpl` - Category CRUD
3. Test data layer operations

**Test Template**:
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class PhotoRepositoryImplTest {

    @Test
    fun `insertPhoto should save photo to database`() = runTest {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val repository = PhotoRepositoryImpl(db.photoDao())

        // When
        val photo = Photo(name = "test.jpg", path = "/path/test.jpg")
        repository.insertPhoto(photo)

        // Then
        val photos = repository.getAllPhotos()
        assertEquals(1, photos.size)
    }
}
```

### Phase 4: Add iOS Core Tests (3-4 hours)
**Goal**: Achieve 30% coverage for iOS

**Priority Areas**:
1. `PINManager` - PIN validation logic
2. `StorageManager` - Photo storage operations
3. `BackupManager` - Backup/restore logic
4. `CategoryRepository` - Category operations
5. `SafeThumbnailGenerator` - Thumbnail generation

**Test Template**:
```swift
@testable import SmilePile
import XCTest

final class PINManagerTests: XCTestCase {
    var pinManager: PINManager!

    override func setUp() async throws {
        pinManager = PINManager()
    }

    func testSetPIN_validPIN_succeeds() async throws {
        let result = try await pinManager.setPIN("1234")
        XCTAssertTrue(result)
    }

    func testValidatePIN_correctPIN_succeeds() async throws {
        try await pinManager.setPIN("1234")
        let isValid = try await pinManager.validatePIN("1234")
        XCTAssertTrue(isValid)
    }
}
```

---

## Estimated Coverage Gains

### Android
| Area | Current | Target | Gain |
|------|---------|--------|------|
| ViewModels | 0% | 30% | +30% |
| Repositories | 0% | 30% | +30% |
| Backup | 16% | 30% | +14% |
| Overall | 12% | **~30%** | **+18%** |

### iOS
| Area | Current | Target |
|------|---------|--------|
| PINManager | ~0% | 40% |
| StorageManager | ~0% | 35% |
| BackupManager | ~0% | 30% |
| CategoryRepository | ~0% | 35% |
| Overall | **~0%** | **~30%** |

---

## Implementation Order

1. ✅ **Fix iOS test compilation** (0.5 hours)
   - Remove/fix PhotoImportCoordinator references
   - Verify all test files compile

2. **Add 5 Android ViewModel Tests** (4 hours)
   - PhotoGalleryViewModel
   - CategoryViewModel
   - BackupViewModel
   - PhotoEditViewModel
   - SettingsViewModel

3. **Add 2 Android Repository Tests** (2 hours)
   - PhotoRepositoryImpl
   - CategoryRepositoryImpl

4. **Add 5 iOS Core Tests** (3 hours)
   - PINManager
   - StorageManager
   - BackupManager
   - CategoryRepository
   - SafeThumbnailGenerator

5. **Measure & Adjust** (1 hour)
   - Run coverage reports
   - Add more tests if needed to hit 30%

**Total Estimated Time**: 10-12 hours

---

## Success Criteria

- ✅ Android instruction coverage ≥ 30%
- ✅ iOS code coverage ≥ 30%
- ✅ All tests passing
- ✅ No flaky tests
- ✅ Tests run in CI/CD pipeline

---

## Next Steps

1. **Immediate**: Fix iOS test compilation errors
2. **Short-term**: Add ViewModel tests (biggest impact)
3. **Medium-term**: Add Repository and iOS tests
4. **Ongoing**: Maintain 30% coverage as code grows
