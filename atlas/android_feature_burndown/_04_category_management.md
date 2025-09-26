# iOS Feature Implementation: Category Management

## Context & Requirements

The Android SmilePile app implements a comprehensive Category Management system that allows **Parent Mode users only** to organize photos into custom categories with color-coded visual indicators. This system serves as the foundation for photo organization and filtering throughout the application.

**CRITICAL ACCESS RESTRICTION**: Category Management is only accessible in Parent Mode. The bottom navigation that includes Categories is hidden in Kids Mode, making the CategoryManagementScreen completely inaccessible to child users.

### Android Implementation Analysis

#### Category Data Model Structure

**Primary Category Entity (CategoryEntity.kt)**:
```kotlin
@Entity(tableName = "category_entities")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val displayName: String,        // Human-readable name (e.g., "Family")
    val colorHex: String,          // Color code (e.g., "#E91E63")
    val position: Int = 0,         // Display ordering
    val isDefault: Boolean = false, // Whether it's a system default
    val createdAt: Long = System.currentTimeMillis()
)
```

**Domain Model (Category.kt)**:
```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,              // Normalized name (e.g., "family")
    val displayName: String,       // Human-readable name (e.g., "Family")
    val position: Int,             // Display ordering
    val iconResource: String? = null, // Optional icon (not implemented)
    val colorHex: String? = null,  // Color code
    val isDefault: Boolean = false, // Whether it's a system default
    val createdAt: Long = System.currentTimeMillis()
)
```

#### Default Categories Configuration

The system initializes with four default categories on first launch:

```kotlin
fun getDefaultCategories(): List<Category> = listOf(
    Category(
        id = 1, name = "family", displayName = "Family",
        position = 0, colorHex = "#E91E63", isDefault = true  // Pink for family warmth
    ),
    Category(
        id = 2, name = "cars", displayName = "Cars",
        position = 1, colorHex = "#F44336", isDefault = true  // Red for cars/racing
    ),
    Category(
        id = 3, name = "games", displayName = "Games",
        position = 2, colorHex = "#9C27B0", isDefault = true  // Purple for games/fun
    ),
    Category(
        id = 4, name = "sports", displayName = "Sports",
        position = 3, colorHex = "#4CAF50", isDefault = true  // Green for sports/outdoors
    )
)
```

#### Photo-Category Relationship

Photos are linked to categories via foreign key relationship:

```kotlin
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE  // Delete photos when category deleted
        )
    ]
)
data class Photo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val categoryId: Long,  // Links to Category.id
    val name: String,
    // ... other fields
)
```

#### Repository Layer - CRUD Operations

**CategoryDao Operations**:
- `insert(category: CategoryEntity): Long` - Create new category
- `update(category: CategoryEntity): Int` - Update existing category
- `delete(category: CategoryEntity): Int` - Delete category
- `getAll(): Flow<List<CategoryEntity>>` - Get all categories (reactive)
- `getById(categoryId: Long): CategoryEntity?` - Get specific category
- `getByDisplayName(displayName: String): CategoryEntity?` - Find by name
- `existsByDisplayName(displayName: String): Boolean` - Check for duplicates
- `getCount(): Int` - Total category count

**CategoryRepository Interface**:
- Business logic layer handling domain model conversions
- Validation and error handling
- Default category initialization
- Name normalization (display name → normalized name)

#### Validation Rules & Business Logic

1. **Category Creation Validation**:
   - Display name cannot be blank
   - Display names must be unique (case-insensitive)
   - Auto-generates normalized name: `displayName.trim().lowercase().replace(" ", "_")`
   - Assigns next available position

2. **Category Deletion Protection**:
   - Cannot delete the last remaining category (minimum 1 required)
   - If category has photos: offers option to delete photos or cancel
   - Uses CASCADE foreign key to delete related photos if confirmed

3. **Duplicate Detection**:
   - Checks display name uniqueness during create/update
   - Excludes current category when updating (allows same name)

4. **Position Management**:
   - New categories get next highest position
   - Position determines display order in UI

## Research Phase

### UI Components Architecture

#### 1. Category Management Screen (CategoryManagementScreen.kt)

**NAVIGATION RESTRICTION**: This screen is only accessible via bottom navigation in Parent Mode. Kids Mode users cannot access category management functionality.

**Key Features**:
- FAB with pulse animation when no categories exist
- LazyColumn displaying categories with photo counts
- Loading states and error handling
- Empty state with call-to-action
- Category cards with edit/delete actions
- AppHeaderComponent with Kids Mode toggle button

**State Management**:
```kotlin
val categoriesWithCounts by viewModel.categoriesWithCountsFlow.collectAsState()
val isLoading by viewModel.isLoading.collectAsState()
val error by viewModel.error.collectAsState()
val showAddDialog by viewModel.showAddDialog.collectAsState()
val editingCategory by viewModel.editingCategory.collectAsState()
```

#### 2. Add/Edit Category Dialog (AddCategoryDialog.kt)

**Features**:
- Text field for category display name
- Color picker with 12 predefined colors
- Live preview of category chip
- Validation feedback
- Separate modes for Add vs Edit

**Color Selection Grid**:
```kotlin
val PREDEFINED_COLORS = listOf(
    "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
    "#F44336", "#FF5722", "#795548", "#607D8B",
    "#E91E63", "#009688", "#FFEB3B", "#3F51B5"
)
```

**VERIFIED IMPLEMENTATION**: The color picker displays 12 predefined colors in a 6-column grid within a LazyVerticalGrid. Each color is selectable with visual feedback (check icon when selected).

#### 3. Category Chip Components (CategoryChip.kt)

**Three Variants**:
1. **CategoryChip** - Basic display with color indicator
2. **SelectableCategoryChip** - With selection state and photo count badge
3. **CategoryColorIndicator** - Standalone color circle

**Visual Design**:
- Rounded rectangle shape (16dp radius)
- Color indicator circle (12dp-14dp diameter)
- Material3 design system colors
- Border changes based on selection state

#### 4. Delete Confirmation Dialogs

**Two Types**:
1. **Empty Category**: Simple confirmation dialog
2. **Category with Photos**: Warning dialog with options:
   - Delete category and all photos
   - Cancel operation

### Data Flow Architecture

#### ViewModel Layer (CategoryViewModel.kt)

**State Management**:
```kotlin
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    // UI State
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _showAddDialog = MutableStateFlow(false)
    private val _editingCategory = MutableStateFlow<Category?>(null)

    // Data Flows
    val categories: StateFlow<List<Category>>
    val categoriesWithCounts: StateFlow<List<CategoryWithCount>>
}
```

**Key Operations**:
- `addCategory(displayName: String, colorHex: String)` - Create new category
- `updateCategory(category: Category, displayName: String, colorHex: String)` - Update existing
- `deleteCategory(category: Category, deletePhotos: Boolean)` - Delete with photo handling
- `refreshCategoriesWithCounts()` - Reload data with photo counts

**CategoryWithCount Data Class**:
```kotlin
data class CategoryWithCount(
    val category: Category,
    val photoCount: Int
)
```

## Story Creation

### User Stories

**IMPORTANT**: All category management user stories apply only to Parent Mode users, as the Category Management screen is not accessible in Kids Mode.

1. **As a parent user, I want to see default categories when I first open the app**
   - GIVEN: Fresh app installation in Parent Mode
   - WHEN: App launches for the first time
   - THEN: Four default categories are created (Family, Cars, Games, Sports)

2. **As a parent user, I want to create custom categories to organize my photos**
   - GIVEN: I'm in the Category Management screen (Parent Mode only)
   - WHEN: I tap the add button and enter a category name and color
   - THEN: A new category is created and appears in the list

3. **As a parent user, I want to edit existing categories**
   - GIVEN: I have existing categories (Parent Mode)
   - WHEN: I tap edit on a category and modify its name or color
   - THEN: The category is updated with the new information

4. **As a parent user, I want to see how many photos are in each category**
   - GIVEN: Categories with photos assigned (Parent Mode)
   - WHEN: I view the category management screen
   - THEN: Each category shows its photo count

5. **As a parent user, I want to delete empty categories I no longer need**
   - GIVEN: A category with no photos (Parent Mode)
   - WHEN: I tap delete and confirm
   - THEN: The category is removed from the system

6. **As a parent user, I want to be warned before deleting categories with photos**
   - GIVEN: A category containing photos (Parent Mode)
   - WHEN: I try to delete it
   - THEN: I see a warning and can choose to delete photos or cancel

7. **As a parent user, I cannot delete the last remaining category**
   - GIVEN: Only one category remains (Parent Mode)
   - WHEN: I try to delete it
   - THEN: I see an error message preventing the deletion

8. **As a parent user, I want visual feedback when creating categories**
   - GIVEN: No categories exist (Parent Mode)
   - WHEN: I view the category screen
   - THEN: The add button pulses to draw attention

9. **As a parent user, I want to prevent duplicate category names**
   - GIVEN: Existing categories (Parent Mode)
   - WHEN: I try to create a category with an existing name
   - THEN: I see a validation error

10. **As any user, I want consistent color-coded category visualization**
    - GIVEN: Categories with assigned colors
    - WHEN: I view categories throughout the app (both modes)
    - THEN: Each category displays with its chosen color consistently

### Technical Requirements

#### Data Persistence
- Core Data model for CategoryEntity with attributes: id, displayName, colorHex, position, isDefault, createdAt
- Relationship mapping to Photo entities via categoryId foreign key
- Migration support for future schema changes

#### Default Data Initialization
- App startup check for existing categories
- Automatic creation of default categories on first launch
- Maintain default category flag for system categories

#### Validation & Business Rules
- Display name uniqueness validation
- Minimum one category requirement
- Cascade delete behavior for photos when category deleted
- Position-based ordering system

#### UI Components Required
- Category management list screen
- Add/edit category modal/sheet
- Color picker component
- Category chip components
- Delete confirmation dialogs
- Empty state handling
- Loading states

## Implementation Blueprint

### iOS Core Data Model

```swift
// CategoryEntity.swift
@objc(CategoryEntity)
public class CategoryEntity: NSManagedObject {
    @NSManaged public var id: UUID
    @NSManaged public var displayName: String
    @NSManaged public var colorHex: String
    @NSManaged public var position: Int32
    @NSManaged public var isDefault: Bool
    @NSManaged public var createdAt: Date
    @NSManaged public var photos: NSSet?

    // Computed properties
    var normalizedName: String {
        return displayName.lowercased().replacingOccurrences(of: " ", with: "_")
    }
}

extension CategoryEntity {
    static func createDefaultCategories(in context: NSManagedObjectContext) -> [CategoryEntity] {
        let defaultCategories = [
            (displayName: "Family", colorHex: "#E91E63", position: 0),
            (displayName: "Cars", colorHex: "#F44336", position: 1),
            (displayName: "Games", colorHex: "#9C27B0", position: 2),
            (displayName: "Sports", colorHex: "#4CAF50", position: 3)
        ]

        return defaultCategories.map { data in
            let category = CategoryEntity(context: context)
            category.id = UUID()
            category.displayName = data.displayName
            category.colorHex = data.colorHex
            category.position = Int32(data.position)
            category.isDefault = true
            category.createdAt = Date()
            return category
        }
    }
}
```

### SwiftUI Views Structure

#### 1. Category Management Screen

```swift
// CategoryManagementView.swift
struct CategoryManagementView: View {
    @StateObject private var viewModel = CategoryManagementViewModel()
    @State private var showingAddCategory = false
    @State private var editingCategory: CategoryEntity?
    @State private var categoryToDelete: CategoryEntity?

    var body: some View {
        NavigationView {
            ZStack {
                categoryList

                if viewModel.isLoading {
                    loadingOverlay
                }
            }
            .navigationTitle("Categories")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    addCategoryButton
                }
            }
            .sheet(isPresented: $showingAddCategory) {
                AddCategorySheet(
                    category: editingCategory,
                    onSave: { displayName, colorHex in
                        if let category = editingCategory {
                            viewModel.updateCategory(category, displayName: displayName, colorHex: colorHex)
                        } else {
                            viewModel.addCategory(displayName: displayName, colorHex: colorHex)
                        }
                        showingAddCategory = false
                        editingCategory = nil
                    },
                    onCancel: {
                        showingAddCategory = false
                        editingCategory = nil
                    }
                )
            }
            .alert("Delete Category", isPresented: .constant(categoryToDelete != nil)) {
                deleteCategoryAlert
            }
        }
    }

    private var categoryList: some View {
        LazyVGrid(columns: [GridItem(.flexible())], spacing: 12) {
            ForEach(viewModel.categoriesWithCounts, id: \.category.id) { categoryWithCount in
                CategoryManagementCard(
                    categoryWithCount: categoryWithCount,
                    onEdit: { category in
                        editingCategory = category
                        showingAddCategory = true
                    },
                    onDelete: { category in
                        categoryToDelete = category
                    }
                )
            }
        }
        .padding()
    }
}
```

#### 2. Add/Edit Category Sheet

```swift
// AddCategorySheet.swift
struct AddCategorySheet: View {
    let category: CategoryEntity?
    let onSave: (String, String) -> Void
    let onCancel: () -> Void

    @State private var displayName: String
    @State private var selectedColorHex: String

    private let predefinedColors = [
        "#4CAF50", "#2196F3", "#FF9800", "#9C27B0",
        "#F44336", "#FF5722", "#795548", "#607D8B",
        "#E91E63", "#009688", "#FFEB3B", "#3F51B5"
    ]

    init(category: CategoryEntity?, onSave: @escaping (String, String) -> Void, onCancel: @escaping () -> Void) {
        self.category = category
        self.onSave = onSave
        self.onCancel = onCancel

        _displayName = State(initialValue: category?.displayName ?? "")
        _selectedColorHex = State(initialValue: category?.colorHex ?? predefinedColors.first!)
    }

    var body: some View {
        NavigationView {
            Form {
                Section("Category Details") {
                    TextField("Category Name", text: $displayName)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                }

                Section("Category Color") {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 6), spacing: 10) {
                        ForEach(predefinedColors, id: \.self) { colorHex in
                            ColorSelectionButton(
                                colorHex: colorHex,
                                isSelected: selectedColorHex == colorHex,
                                onSelect: { selectedColorHex = colorHex }
                            )
                        }
                    }
                }

                Section("Preview") {
                    if !displayName.trimmingCharacters(in: .whitespaces).isEmpty {
                        CategoryChip(
                            displayName: displayName,
                            colorHex: selectedColorHex,
                            isSelected: true
                        )
                    } else {
                        Text("Enter category name to see preview")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle(category != nil ? "Edit Category" : "Add Category")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel", action: onCancel)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(category != nil ? "Update" : "Add") {
                        onSave(displayName.trimmingCharacters(in: .whitespaces), selectedColorHex)
                    }
                    .disabled(displayName.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }
}
```

#### 3. Category Chip Component

```swift
// CategoryChip.swift
struct CategoryChip: View {
    let displayName: String
    let colorHex: String
    let isSelected: Bool
    let photoCount: Int?
    let onTap: (() -> Void)?

    init(displayName: String, colorHex: String, isSelected: Bool = false, photoCount: Int? = nil, onTap: (() -> Void)? = nil) {
        self.displayName = displayName
        self.colorHex = colorHex
        self.isSelected = isSelected
        self.photoCount = photoCount
        self.onTap = onTap
    }

    var body: some View {
        Button(action: { onTap?() }) {
            HStack(spacing: 8) {
                // Color indicator
                Circle()
                    .fill(Color(hex: colorHex) ?? .primary)
                    .frame(width: 12, height: 12)
                    .overlay(
                        Circle()
                            .stroke(Color.primary.opacity(0.3), lineWidth: 1)
                    )

                // Category name
                Text(displayName)
                    .font(.system(size: 14, weight: isSelected ? .medium : .regular))
                    .foregroundColor(isSelected ? .primary : .secondary)

                // Photo count badge
                if let count = photoCount, count > 0 {
                    Text("\(count)")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.secondary.opacity(0.1))
                        .clipShape(Capsule())
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(isSelected ? Color.primary.opacity(0.1) : Color.clear)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(isSelected ? Color.primary : Color.primary.opacity(0.3), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .buttonStyle(PlainButtonStyle())
    }
}
```

### ViewModel Implementation

```swift
// CategoryManagementViewModel.swift
class CategoryManagementViewModel: ObservableObject {
    @Published var categoriesWithCounts: [CategoryWithCount] = []
    @Published var isLoading = false
    @Published var errorMessage: String?

    private let coreDataManager = CoreDataManager.shared
    private var cancellables = Set<AnyCancellable>()

    struct CategoryWithCount: Identifiable {
        let id = UUID()
        let category: CategoryEntity
        let photoCount: Int
    }

    init() {
        loadCategoriesWithCounts()
        setupDefaultCategoriesIfNeeded()
    }

    func loadCategoriesWithCounts() {
        isLoading = true

        // Fetch categories with photo counts
        let request: NSFetchRequest<CategoryEntity> = CategoryEntity.fetchRequest()
        request.sortDescriptors = [NSSortDescriptor(keyPath: \CategoryEntity.position, ascending: true)]

        do {
            let categories = try coreDataManager.context.fetch(request)
            let categoriesWithCounts = categories.map { category in
                let photoCount = (category.photos?.count) ?? 0
                return CategoryWithCount(category: category, photoCount: photoCount)
            }

            DispatchQueue.main.async {
                self.categoriesWithCounts = categoriesWithCounts
                self.isLoading = false
            }
        } catch {
            DispatchQueue.main.async {
                self.errorMessage = "Failed to load categories: \(error.localizedDescription)"
                self.isLoading = false
            }
        }
    }

    func addCategory(displayName: String, colorHex: String) {
        guard !displayName.trimmingCharacters(in: .whitespaces).isEmpty else {
            errorMessage = "Category name cannot be empty"
            return
        }

        // Check for duplicates
        if categoryExists(with: displayName) {
            errorMessage = "Category '\(displayName)' already exists"
            return
        }

        let context = coreDataManager.context
        let category = CategoryEntity(context: context)
        category.id = UUID()
        category.displayName = displayName.trimmingCharacters(in: .whitespaces)
        category.colorHex = colorHex
        category.position = Int32(getNextPosition())
        category.isDefault = false
        category.createdAt = Date()

        saveContext()
    }

    func updateCategory(_ category: CategoryEntity, displayName: String, colorHex: String) {
        guard !displayName.trimmingCharacters(in: .whitespaces).isEmpty else {
            errorMessage = "Category name cannot be empty"
            return
        }

        // Check for duplicates (excluding current category)
        if displayName.trimmingCharacters(in: .whitespaces) != category.displayName &&
           categoryExists(with: displayName) {
            errorMessage = "Category '\(displayName)' already exists"
            return
        }

        category.displayName = displayName.trimmingCharacters(in: .whitespaces)
        category.colorHex = colorHex

        saveContext()
    }

    func deleteCategory(_ category: CategoryEntity, deletePhotos: Bool = false) {
        // Prevent deletion of last category
        if categoriesWithCounts.count <= 1 {
            errorMessage = "Cannot delete the last remaining category"
            return
        }

        let context = coreDataManager.context

        if deletePhotos {
            // Delete associated photos
            if let photos = category.photos {
                for case let photo as PhotoEntity in photos {
                    context.delete(photo)
                }
            }
        }

        context.delete(category)
        saveContext()
    }

    private func setupDefaultCategoriesIfNeeded() {
        let request: NSFetchRequest<CategoryEntity> = CategoryEntity.fetchRequest()

        do {
            let count = try coreDataManager.context.count(for: request)
            if count == 0 {
                let defaultCategories = CategoryEntity.createDefaultCategories(in: coreDataManager.context)
                saveContext()
            }
        } catch {
            errorMessage = "Failed to initialize default categories: \(error.localizedDescription)"
        }
    }

    private func categoryExists(with displayName: String) -> Bool {
        let request: NSFetchRequest<CategoryEntity> = CategoryEntity.fetchRequest()
        request.predicate = NSPredicate(format: "displayName ==[c] %@", displayName)

        do {
            let count = try coreDataManager.context.count(for: request)
            return count > 0
        } catch {
            return false
        }
    }

    private func getNextPosition() -> Int {
        let maxPosition = categoriesWithCounts.map { $0.category.position }.max() ?? -1
        return Int(maxPosition) + 1
    }

    private func saveContext() {
        do {
            try coreDataManager.context.save()
            loadCategoriesWithCounts()
        } catch {
            errorMessage = "Failed to save changes: \(error.localizedDescription)"
        }
    }
}
```

## Validation Checklist

### Core Functionality
- [ ] Default categories (Family, Cars, Games, Sports) loaded on first launch
- [ ] Create category with display name and color selection works
- [ ] Edit existing category (name and color) works
- [ ] Category display shows photo count for each category
- [ ] Delete empty category works with confirmation
- [ ] Delete category with photos shows warning and options
- [ ] Cannot delete last remaining category (validation error shown)

### Data Validation
- [ ] Display name cannot be blank (validation error)
- [ ] Duplicate category names prevented (case-insensitive check)
- [ ] Category names properly normalized for internal storage
- [ ] Colors save correctly and display consistently
- [ ] Position ordering maintained correctly

### UI/UX Features
- [ ] Color picker shows 12 predefined colors in grid
- [ ] Category chip shows color indicator and name
- [ ] Live preview in add/edit dialog works
- [ ] Empty state shown when no categories exist
- [ ] Loading states display during operations
- [ ] Error messages shown for validation failures
- [ ] Confirmation dialogs for destructive actions

### Performance & Data
- [ ] Categories load reactively (Core Data observations)
- [ ] Photo counts calculated efficiently
- [ ] Database operations run on background threads
- [ ] UI updates happen on main thread
- [ ] Memory management proper for large category lists

### Edge Cases
- [ ] App handles Core Data migration gracefully
- [ ] Network interruption during operations handled
- [ ] Category deletion with cascade to photos works
- [ ] Concurrent modifications handled properly
- [ ] App state preserved during background/foreground cycles

## Atlas Workflow Integration

Execute this **EXACT** prompt with Atlas for implementing the complete Category Management system in iOS SmilePile:

```
Implement the complete Category Management system for iOS SmilePile based on the detailed Android analysis. This system must include:

**Core Data Model Requirements:**
- CategoryEntity with fields: id (UUID), displayName (String), colorHex (String), position (Int32), isDefault (Bool), createdAt (Date)
- Relationship to PhotoEntity via categoryId foreign key with cascade delete
- Default categories initialization: Family (#E91E63), Cars (#F44336), Games (#9C27B0), Sports (#4CAF50)

**SwiftUI Views to Create:**
1. CategoryManagementView - Main screen with category list, add FAB, empty state
2. AddCategorySheet - Modal for add/edit with name field, color picker grid (12 colors), live preview
3. CategoryChip - Reusable component with color indicator circle, selection states, photo count badge
4. ColorSelectionButton - Interactive color picker item with selection indicator
5. Delete confirmation alerts for empty categories and categories with photos

**ViewModel Implementation:**
- CategoryManagementViewModel with @Published properties for categories, loading states, errors
- CRUD operations: addCategory, updateCategory, deleteCategory with full validation
- CategoryWithCount struct to combine category data with photo counts
- Core Data integration with proper threading and error handling

**Business Logic Requirements:**
- Prevent blank category names with validation feedback
- Enforce unique display names (case-insensitive) with duplicate detection
- Minimum one category rule - prevent deletion of last category
- Auto-generate next position for new categories
- Cascade delete behavior: delete category with photos requires confirmation

**UI/UX Specifications:**
- 12 predefined colors in 6-column grid: #4CAF50, #2196F3, #FF9800, #9C27B0, #F44336, #FF5722, #795548, #607D8B, #E91E63, #009688, #FFEB3B, #3F51B5
- Category chips: rounded rectangles (16pt radius), color indicator circles (12pt), Material Design-inspired styling
- Loading overlays, empty states, error alerts with proper messaging
- Photo count badges on category cards in management view

**Data Flow Architecture:**
- Core Data setup with CategoryEntity and PhotoEntity relationship
- Repository pattern with Core Data manager singleton
- Reactive UI updates using @Published and Core Data observations
- Background thread operations with main thread UI updates
- Proper error handling and user feedback throughout

**Validation Rules:**
- Display name required and trimmed
- Unique names across all categories (case-insensitive)
- Last category deletion prevention with error message
- Cascade delete warning for categories containing photos
- Color hex validation with fallback to default

**Testing Requirements:**
- Unit tests for ViewModel operations and validations
- Core Data model tests for relationships and constraints
- UI tests for category creation, editing, and deletion flows
- Edge case testing: empty states, error conditions, concurrent operations

Ensure complete feature parity with Android implementation including all validation rules, UI states, error handling, and user experience patterns. The implementation must be production-ready with proper error handling, accessibility, and performance optimization.
```

---

**Implementation Priority:** HIGH - Category Management is foundational to the entire photo organization system and must be implemented first before photo import, gallery features, or filtering capabilities.

**Estimated Complexity:** MEDIUM-HIGH - Involves Core Data modeling, complex UI components, validation logic, and integration with photo management system.

**Dependencies:** Core Data setup, basic navigation structure, color utility extensions.

**Testing Focus:** Data validation, CRUD operations, UI state management, cascade delete behavior, default category initialization.