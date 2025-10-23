# SmilePile Development Conventions

## Project Overview

SmilePile is a dual-platform (iOS/Android) photo management application featuring Kids Mode functionality with robust parental controls, security features, and category-based organization. The application strictly follows a minimalist development philosophy that prioritizes explicit requirements over speculative features.

### Core Features
- Photo management with category organization
- Kids Mode with parental controls
- Biometric/PIN/Pattern authentication
- Photo backup and restore
- 4-tier deployment system (QUAL, STAGE, BETA, PROD)
- Secure metadata encryption

## CRITICAL: Development Philosophy

### ABSOLUTE RULES - NEVER VIOLATE
1. **DO EXACTLY WHAT'S ASKED** - Nothing more, nothing less
2. **NEVER CREATE FILES** - Edit existing files only (unless explicitly requested)
3. **FOLLOW EXISTING PATTERNS** - Check neighboring files first, copy their style
4. **NO SPECULATIVE CODING** - Every line must directly support a requested requirement

### FORBIDDEN ACTIONS - AUTOMATIC REJECTION
- **NO search/filter features** unless explicitly requested
- **NO favorites/bookmarks** unless explicitly requested
- **NO sorting/ordering** unless explicitly requested
- **NO "nice to have" additions**
- **NO new dependencies** without permission
- **NO emoji** in code or commits
- **NO comments** unless requested
- **NO anticipating future needs**
- **NO foundation for unrequested features**

## State Management

### iOS State Management
```swift
// ViewModels use @Published for state
@MainActor
class ExampleViewModel: ObservableObject {
    @Published var state: String = ""
    @Published var items: [Item] = []
}

// Views use @StateObject/@ObservedObject
struct ExampleView: View {
    @StateObject private var viewModel = ExampleViewModel()
}
```

### Android State Management
```kotlin
// ViewModels use StateFlow with Hilt
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val manager: ExampleManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()
}

// Composables observe state
@Composable
fun ExampleScreen(viewModel: ExampleViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
}
```

## Naming Conventions

### iOS Naming
- **Views**: `PhotoGalleryView`, `SettingsView` (suffix with View)
- **ViewModels**: `KidsModeViewModel`, `CategoryViewModel` (suffix with ViewModel)
- **Managers**: `CategoryManager`, `BiometricManager` (suffix with Manager)
- **Models**: `Photo`, `Category` (no suffix)
- **Extensions**: `Color+Hex.swift`, `View+Extensions.swift`

### Android Naming
- **Screens**: `MainScreen`, `SettingsScreen` (suffix with Screen)
- **ViewModels**: `AppModeViewModel`, `PhotoGalleryViewModel` (suffix with ViewModel)
- **Managers**: `BiometricManager`, `SecurePreferencesManager` (suffix with Manager)
- **Models**: `Photo`, `Category` (no suffix)
- **State**: `AppModeUiState`, `PhotoGalleryUiState` (suffix with UiState)

## Code Organization

### iOS Directory Structure
```
ios/SmilePile/
├── Data/
│   ├── CoreData/         # Core Data models and migration
│   ├── Managers/         # Business logic managers
│   ├── Models/           # Data models
│   └── Storage/          # Storage management
├── Security/             # Authentication and security
├── Views/               # SwiftUI views
│   ├── Components/      # Reusable UI components
│   └── Screens/         # Main screens
├── ViewModels/          # View models
├── Managers/            # App-level managers
└── Utils/               # Utilities and extensions
```

### Android Directory Structure
```
android/app/src/main/java/com/smilepile/
├── data/
│   ├── backup/          # Backup and restore
│   ├── database/        # Room database
│   ├── models/          # Data models
│   └── repository/      # Data repositories
├── security/            # Security and authentication
├── ui/
│   ├── components/      # Reusable composables
│   ├── screens/         # Main screens
│   └── viewmodels/      # View models
├── managers/            # Business logic managers
└── di/                  # Dependency injection
```

## Code Quality Standards

### Pre-Change Checklist
1. Read existing code patterns in neighboring files
2. Check current dependencies (package.json/Podfile/build.gradle)
3. Match exact formatting and style
4. Verify no unrequested features are being added

### Post-Change Checklist
1. **iOS**: Run `xcodebuild` to verify build
2. **Android**: Run `./gradlew build` to verify build
3. **Both**: Check `git diff` before confirming completion
4. **Deploy**: Use `./deploy/deploy_qual.sh` for deployments

### Testing Requirements
- Tests MUST run via `deploy_qual.sh`
- Fix failing tests, don't skip them
- User must explicitly say "skip tests" for any override

## Error Handling

### iOS Error Handling
```swift
// Use Result type or throw errors
func loadPhotos() async throws -> [Photo] {
    do {
        return try await photoRepository.getAllPhotos()
    } catch {
        print("Error loading photos: \(error)")
        throw error
    }
}
```

### Android Error Handling
```kotlin
// Use sealed classes for results
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val exception: Exception) : Result<T>()
}

// Handle in ViewModels
viewModelScope.launch {
    when (val result = repository.loadPhotos()) {
        is Result.Success -> _uiState.value = uiState.value.copy(photos = result.data)
        is Result.Error -> _uiState.value = uiState.value.copy(error = result.exception.message)
    }
}
```

## Comments & Documentation

### STRICT RULES
- **NO COMMENTS** unless explicitly requested by user
- **NO EMOJI** in code, comments, or commit messages
- **NO TODO comments** without user request
- Keep code self-documenting through clear naming

### When Comments Are Requested
```swift
// iOS: Brief, factual comments only
func processPhoto(_ photo: Photo) { // Process photo for display
    // Implementation
}
```

```kotlin
// Android: Brief, factual comments only
fun processPhoto(photo: Photo) { // Process photo for display
    // Implementation
}
```

## Platform-Specific Rules

### iOS Specific
- Use SwiftUI exclusively (no UIKit unless existing)
- Photo IDs: Use `PHAsset.localIdentifier`
- Build configurations via xcconfig files
- Schemes: SmilePile Qual/Stage/Beta/Prod
- Bundle IDs: `app.smilepile.qual` (QUAL), `app.smilepile` (others)

### Android Specific
- Use Kotlin with Jetpack Compose
- Photo IDs: Use `Uri.toString()`
- Dependency injection via Hilt
- Build flavors: qual, stage, beta, prod
- Application IDs: `com.smilepile.qual` (QUAL), `com.smilepile` (others)

## Security Requirements

### Authentication
- Biometric authentication (fingerprint/face)
- PIN authentication (4-6 digits)
- Pattern authentication (Android)
- Secure storage for credentials

### Data Security
- Metadata encryption for sensitive information
- Secure preferences for settings
- Keychain/Keystore for credentials
- No plaintext storage of sensitive data

### Kids Mode Security
- Require authentication to exit Kids Mode
- Prevent accidental mode switching
- Secure category management
- Protected settings access

## Data Flow

### Photo Handling
```
User Selection → Repository → ViewModel → UI State → View/Composable
```

### Category Management
```
Category CRUD → Manager → Repository → Database → ViewModel → UI
```

### State Updates
```
User Action → ViewModel → State Update → UI Recomposition
```

## Anti-Patterns - CRITICAL SECTION

### NEVER DO THESE
1. **Adding Unrequested Features**
   ```swift
   // BAD - Adding search without request
   @Published var searchText = ""
   func filterPhotos(by search: String) { ... }

   // GOOD - Only requested functionality
   @Published var photos: [Photo] = []
   ```

2. **Using Emoji**
   ```kotlin
   // BAD
   fun loadPhotos() { // 📷 Load photos

   // GOOD
   fun loadPhotos() {
   ```

3. **Speculative Coding**
   ```swift
   // BAD - "Foundation" for future features
   protocol Searchable { }
   extension Photo: Searchable { }

   // GOOD - Only what's needed now
   struct Photo { }
   ```

4. **Adding Dependencies**
   ```kotlin
   // BAD - Adding without permission
   implementation("com.awesome:library:1.0")

   // GOOD - Use existing dependencies only
   ```

5. **Creating New Files**
   ```
   // BAD - Creating files without request
   SearchManager.swift
   FilterViewModel.kt

   // GOOD - Edit existing files only
   ```

## Examples

### Good iOS Implementation
```swift
// Follows existing patterns, no extras
@MainActor
class CategoryViewModel: ObservableObject {
    @Published var categories: [Category] = []
    private let repository: CategoryRepository

    init(repository: CategoryRepository = CategoryRepositoryImpl.shared) {
        self.repository = repository
    }

    func loadCategories() async {
        do {
            categories = try await repository.getAllCategories()
        } catch {
            print("Error loading categories: \(error)")
        }
    }
}
```

### Bad iOS Implementation
```swift
// Too many unrequested features
@MainActor
class CategoryViewModel: ObservableObject {
    @Published var categories: [Category] = []
    @Published var searchText = "" // NOT REQUESTED
    @Published var sortOrder = SortOrder.name // NOT REQUESTED
    @Published var favorites: Set<String> = [] // NOT REQUESTED

    // Adds complexity without request
    var filteredCategories: [Category] {
        categories
            .filter { searchText.isEmpty || $0.name.contains(searchText) }
            .sorted { sortOrder.compare($0, $1) }
    }
}
```

### Good Android Implementation
```kotlin
// Follows patterns, minimal implementation
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    fun loadCategories() {
        viewModelScope.launch {
            val categories = repository.getAllCategories()
            _uiState.value = _uiState.value.copy(categories = categories)
        }
    }
}
```

### Bad Android Implementation
```kotlin
// Too many features, violates principles
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {
    // Unrequested features
    val searchQuery = MutableStateFlow("") // NOT REQUESTED
    val sortType = MutableStateFlow(SortType.NAME) // NOT REQUESTED
    val favoriteIds = MutableStateFlow(setOf<String>()) // NOT REQUESTED

    // Complex unrequested logic
    val filteredCategories = combine(
        categories,
        searchQuery,
        sortType,
        favoriteIds
    ) { cats, query, sort, favs ->
        cats.filter { /* complex filtering */ }
            .sortedBy { /* complex sorting */ }
    }
}
```

## Deployment

### Quality Deployment Process
1. Run `./deploy/deploy_qual.sh`
2. Script handles:
   - Build number increment
   - iOS and Android builds
   - Test execution (unless explicitly skipped)
   - SonarQube analysis
   - Git commit and tagging

### Build Commands
```bash
# Deploy to QUAL (most common)
./deploy/deploy_qual.sh

# Deploy specific platform
./deploy/deploy_qual.sh ios
./deploy/deploy_qual.sh android

# Skip tests (requires explicit permission)
SKIP_TESTS=true ./deploy/deploy_qual.sh
```

## Atlas Workflow Integration

All development MUST follow the 9-phase Atlas workflow:

1. **Phase 1**: Research (general-purpose agent)
2. **Phase 2**: Story Creation (product-manager agent)
3. **Phase 3**: Planning (developer agent)
4. **Phase 4**: Security Review (security + peer-reviewer agents)
5. **Phase 5**: Implementation (developer agent)
6. **Phase 6**: Testing (ux-analyst + peer-reviewer agents)
7. **Phase 7**: Validation (product-manager agent)
8. **Phase 8**: Clean-up (general-purpose agent)
9. **Phase 9**: Deployment (devops agent with deploy_qual.sh)

See `/Users/adamstack/SmilePile/atlas/docs/AGENT_WORKFLOW.md` for details.

## Final Reminders

1. **IF SOMETHING WON'T WORK** - Tell the user immediately
2. **NO SHORTCUTS** - Do it right or not at all
3. **CHECK TWICE** - Verify no unrequested features before completion
4. **MATCH PATTERNS** - Always copy existing code style
5. **TEST BUILDS** - Always verify builds compile successfully