# Dependency Injection Setup

This directory contains Hilt dependency injection modules for the SmilePile application.

## Modules

### DatabaseModule
- Provides singleton instance of `SmilePileDatabase`
- Provides `PhotoDao` and `CategoryDao` from the database
- Handles Room database configuration

### RepositoryModule
- Binds `PhotoRepository` interface to `PhotoRepositoryImpl`
- Binds `CategoryRepository` interface to `CategoryRepositoryImpl`
- Uses `@Binds` for efficient interface bindings

### DispatcherModule
- Provides qualified `CoroutineDispatcher` instances
- `@IoDispatcher` for database operations
- `@MainDispatcher` for UI operations
- `@DefaultDispatcher` for CPU-intensive tasks

## Usage

To inject repositories in your classes:

```kotlin
@AndroidEntryPoint
class ExampleActivity : AppCompatActivity() {

    @Inject
    lateinit var photoRepository: PhotoRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    // Use repositories in your code...
}
```

Or in ViewModels:

```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    // ViewModel implementation...
}
```

## Configuration

The application is configured with:
- `@HiltAndroidApp` on `SmilePileApplication`
- `@AndroidEntryPoint` on `MainActivity`
- `@Inject` constructors on repository implementations