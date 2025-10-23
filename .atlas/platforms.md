# SmilePile Platform-Specific Development Rules

## iOS Platform Rules

### 1. Language & Framework Requirements
- **Primary**: Swift 5.0+ with SwiftUI
- **NO UIKit** unless modifying existing UIKit code
- **Minimum iOS**: 14.0+ (based on armv7 requirement)
- **Device Support**: iPhone and iPad with orientation support

### 2. Architecture Patterns

#### ViewModels
```swift
@MainActor
class ExampleViewModel: ObservableObject {
    @Published var property: Type
    private let repository: Repository

    init(repository: Repository = RepositoryImpl()) {
        self.repository = repository
    }
}
```

#### Managers (Singleton Pattern)
```swift
class SettingsManager: ObservableObject {
    static let shared = SettingsManager()
    private init() { }
}
```

#### Repository Pattern
```swift
protocol PhotoRepository {
    func getAllPhotos() async throws -> [Photo]
    func getPhotoById(_ id: Int64) async throws -> Photo?
}

class PhotoRepositoryImpl: PhotoRepository {
    // Implementation
}
```

### 3. State Management
- `@State` - Local view state only
- `@StateObject` - ViewModel ownership
- `@ObservedObject` - ViewModel references
- `@EnvironmentObject` - Shared app state
- `@Published` - Observable properties in ViewModels
- `Combine` framework for reactive programming

### 4. Data Persistence
- **CoreData**: Primary database (`.xcdatamodeld`)
- **UserDefaults**: App settings via `SettingsManager`
- **Photo IDs**: Always use `PHAsset.localIdentifier`
- **Never store file paths** - use photo library identifiers

### 5. Security Implementation
- **BiometricManager**: Face ID/Touch ID authentication
- **Keychain**: Sensitive data storage
- **PIN Storage**: Encrypted in UserDefaults
- **NSFaceIDUsageDescription**: Required in Info.plist

### 6. File Organization Structure
```
ios/SmilePile/
├── Models/          # Data models
├── Views/           # SwiftUI views
│   ├── Components/  # Reusable UI components
│   ├── KidsMode/    # Kids mode specific views
│   └── Security/    # Security-related views
├── ViewModels/      # Business logic
├── Managers/        # Singleton services
├── Settings/        # Settings management
├── Onboarding/      # Onboarding flow
└── Resources/       # Assets, fonts, etc.
```

### 7. Build Configuration (4-Tier System)
- **Schemes**: SmilePile Qual, SmilePile Stage, SmilePile Beta, SmilePile Prod
- **XCConfig Files**: `Qual.xcconfig`, `Stage.xcconfig`, `Beta.xcconfig`, `Prod.xcconfig`
- **Bundle IDs**:
  - QUAL: `app.smilepile.qual`
  - STAGE/BETA/PROD: `app.smilepile`
- **Build Detection**: Use `BuildConfig.isQual`, `BuildConfig.buildType`

### 8. Testing Commands
```bash
# Build QUAL tier
xcodebuild -project SmilePile.xcodeproj -scheme "SmilePile Qual" -configuration Debug build

# Build PROD tier
xcodebuild -project SmilePile.xcodeproj -scheme "SmilePile Prod" -configuration Release build
```

---

## Android Platform Rules

### 1. Language & Framework Requirements
- **Primary**: Kotlin 1.9+ with Jetpack Compose
- **Minimum Android**: API 24 (Android 7.0)
- **Target Android**: API 35
- **Build System**: Gradle with Kotlin DSL (`build.gradle.kts`)

### 2. Architecture Patterns

#### ViewModels with Hilt
```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
}
```

#### Repository Pattern with Injection
```kotlin
interface PhotoRepository {
    suspend fun getAllPhotos(): List<Photo>
    fun getAllPhotosFlow(): Flow<List<Photo>>
}

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val dao: PhotoDao
) : PhotoRepository {
    // Implementation
}
```

#### Dependency Injection Module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun providePhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository = impl
}
```

### 3. State Management
- **StateFlow**: Primary state emission mechanism
- **MutableStateFlow**: Internal state management
- **collectAsState()**: Compose state collection
- **SavedStateHandle**: ViewModel state restoration
- **Coroutines**: Async operations with `viewModelScope`

### 4. Data Persistence
- **Room Database**: Primary local storage
- **Photo IDs**: Always use `Uri.toString()`
- **SecurePreferencesManager**: Encrypted preferences
- **MetadataEncryption**: Photo metadata security
- **Never store absolute file paths**

### 5. Security Implementation
- **BiometricManager**: Fingerprint/face authentication
- **SecureActivity**: Base class for secure screens
- **SecurePreferencesManager**: Encrypted SharedPreferences
- **SecureStorageManager**: Secure file storage
- **CircuitBreaker**: Rate limiting for security operations
- **InactivityManager**: Auto-lock on inactivity

### 6. Package Organization Structure
```
com.smilepile/
├── data/
│   ├── models/      # Data classes
│   ├── repository/  # Repository interfaces & implementations
│   └── dao/         # Room DAOs
├── ui/
│   ├── screens/     # Composable screens
│   ├── components/  # Reusable composables
│   └── viewmodels/  # ViewModels
├── di/              # Hilt dependency injection
├── security/        # Security implementations
├── managers/        # App-wide managers
├── onboarding/      # Onboarding flow
├── operations/      # Photo operations
└── config/          # App configuration
```

### 7. Build Configuration (4-Tier Flavors)
```kotlin
productFlavors {
    create("qual") {
        applicationIdSuffix = ".qual"
        versionNameSuffix = "-qual"
    }
    create("stage") {
        // No suffix for stage/beta/prod
    }
    create("beta") {
        // Share bundle ID with prod
    }
    create("prod") {
        // Production configuration
    }
}
```

### 8. Testing Commands
```bash
# Build QUAL debug
./gradlew assembleQualDebug

# Build PROD release
./gradlew assembleProdRelease

# Run tests
./gradlew testQualDebugUnitTest
```

---

## Cross-Platform Consistency Rules

### 1. Photo Handling
- **Both platforms use local IDs, never file paths**:
  - iOS: `PHAsset.localIdentifier`
  - Android: `Uri.toString()`
- **Metadata stored in app database**, not with photos
- **Security layer encrypts sensitive metadata**

### 2. Architecture Alignment
- **MVVM Pattern**: Both platforms use ViewModels
- **Repository Pattern**: Data layer abstraction
- **Dependency Injection**:
  - iOS: Constructor injection with defaults
  - Android: Hilt with @Inject
- **Reactive Programming**:
  - iOS: Combine framework
  - Android: Kotlin Flow

### 3. Feature Parity
- **Kids Mode**: Identical UX on both platforms
- **Security**: Biometric + PIN on both
- **Categories**: Same default categories
- **Photo Operations**: Consistent editing capabilities

### 4. Testing Strategy
- **Unit Tests**: ViewModels and business logic
- **Integration Tests**: Repository and data layer
- **UI Tests**: Critical user flows

### 5. Deployment
- **Version Format**: `YY.MM.DD.VVV` (e.g., 25.10.17.016)
- **Build Script**: `/deploy/deploy_qual.sh` for both platforms
- **4-Tier System**: QUAL → STAGE → BETA → PROD

### 6. Code Style
- **NO emoji in code** (unless user requests)
- **Descriptive variable names**
- **Comments only when complex logic requires explanation**
- **Follow existing patterns** in neighboring files

### 7. Security Requirements
- **Never log sensitive data**
- **Always encrypt user preferences**
- **Biometric authentication for parental controls**
- **PIN fallback when biometrics unavailable**

### 8. Performance Guidelines
- **Lazy loading** for photo galleries
- **Image caching** with memory limits
- **Background processing** for heavy operations
- **Efficient list rendering**:
  - iOS: LazyVGrid/LazyHGrid
  - Android: LazyVerticalGrid/LazyHorizontalGrid

---

## Critical Development Rules

### NEVER Do These
1. **Never create new files** unless explicitly requested
2. **Never add unrequested features** (search, filters, favorites, etc.)
3. **Never use emoji** in code or commits
4. **Never skip tests** without explicit permission
5. **Never store file paths** - use platform photo IDs
6. **Never log sensitive information**
7. **Never hardcode bundle identifiers** in project files

### ALWAYS Do These
1. **Always follow existing patterns** in the codebase
2. **Always use the repository pattern** for data access
3. **Always handle errors gracefully**
4. **Always respect the 4-tier deployment system**
5. **Always encrypt sensitive data**
6. **Always test on both platforms** before deployment
7. **Always use `deploy_qual.sh`** for deployments

### Before Making Changes
1. Read existing code patterns in neighboring files
2. Check current library versions (no new dependencies without permission)
3. Verify feature is explicitly requested
4. Match exact formatting/style

### After Making Changes
1. Run platform-specific build commands
2. Check `git diff` to verify changes
3. Ensure no unintended files modified
4. Verify tests pass

---

## Platform-Specific Gotchas

### iOS Common Issues
- **Full screen issues**: Use `.edgesIgnoringSafeArea(.all)` or `.ignoresSafeArea()`
- **Navigation padding**: Check NavigationStack/NavigationView configuration
- **Color conflicts**: Ensure single Color extension definition
- **CoreData**: Verify `.xcdatamodeld` properly configured

### Android Common Issues
- **Hilt injection**: Ensure all ViewModels have @HiltViewModel
- **Compose state**: Use `collectAsState()` for Flow collection
- **Security**: Extend SecureActivity for sensitive screens
- **Build variants**: Test all 4 tiers during development

---

## Deployment Checklist

### Pre-Deployment
- [ ] All tests passing on both platforms
- [ ] Version numbers updated consistently
- [ ] Build numbers incremented
- [ ] No hardcoded test data
- [ ] Security review completed

### Deployment Process
1. Run `/deploy/deploy_qual.sh` for QUAL tier
2. Test on real devices
3. Progress through tiers: QUAL → STAGE → BETA → PROD
4. Monitor crash reports and analytics

### Post-Deployment
- [ ] Verify app functionality on both platforms
- [ ] Check crash reporting dashboard
- [ ] Monitor user feedback channels
- [ ] Document any issues discovered