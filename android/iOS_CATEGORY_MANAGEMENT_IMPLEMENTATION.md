# iOS Category Management Implementation Prompt

## Priority 2: iOS Feature Parity - Category Management

### Executive Summary
Implement comprehensive category management functionality in the iOS app to achieve feature parity with the Android implementation. The iOS app currently has basic category infrastructure but lacks the full CRUD operations and UI polish present in the Android version.

---

## 1. CONTEXT AND CURRENT STATE

### Current iOS Implementation Status
✅ **Existing Infrastructure:**
- Basic Category data model (Core Data entity)
- CategoryViewModel with partial functionality
- CategoryManagementView (basic UI, limited features)
- CategoryRepository interface and implementation
- Basic category display components (CategoryChip, CategoryFilterView)

❌ **Missing Features (compared to Android):**
- Category reordering/position management
- Batch operations (merge categories, bulk photo assignment)
- Advanced search and filtering
- Drag and drop support
- Photo count display in management interface
- Category color picker with predefined colors
- Icon selection for categories
- Empty state handling with proper messaging
- Loading states and error handling
- Delete confirmation with photo handling options
- Category statistics and analytics

### Android Feature Reference
The Android implementation includes:
- **Full CRUD Operations**: Create, Read, Update, Delete with validation
- **Visual Customization**: Color selection from predefined palette, icon assignment
- **Bulk Operations**: Merge categories, batch photo assignments
- **Reordering**: Manual position adjustment with up/down controls
- **Search/Filter**: Real-time search with highlighting
- **Photo Management**: Move photos between categories, delete with photos option
- **Statistics**: Category usage analytics, photo distribution
- **UI Polish**: Loading states, empty states, error dialogs, success messages

---

## 2. SPECIFIC REQUIREMENTS AND ACCEPTANCE CRITERIA

### Functional Requirements

#### FR1: Category Creation
- User can create new categories with custom names
- System validates unique category names (case-insensitive)
- User can select from predefined color palette
- User can optionally select an icon
- New categories appear immediately in the list
- Position is automatically assigned (end of list)

#### FR2: Category Editing
- User can edit existing category display name
- User can change category color
- User can change category icon
- Changes are validated and saved immediately
- UI reflects changes without page reload

#### FR3: Category Deletion
- User can delete non-default categories
- System prevents deletion of last remaining category
- If category contains photos, user chooses:
  - Delete category and all photos
  - Move photos to another category
- Confirmation dialog shows photo count and consequences

#### FR4: Category Reordering
- User can enter reorder mode
- Up/down buttons to adjust position
- Changes persist across app sessions
- Visual feedback during reordering

#### FR5: Search and Filter
- Real-time search by category name
- Filter to show/hide empty categories
- Sort options: Position, Name, Photo Count, Date Created
- Search state persists during session

#### FR6: Batch Operations
- Select multiple categories for bulk actions
- Merge selected categories into one
- Bulk assign photos to categories
- Clear selection functionality

#### FR7: Photo Assignment
- Drag photos to categories
- Select photos and assign to category
- Move all photos from one category to another
- Visual feedback during operations

### Non-Functional Requirements

#### NFR1: Performance
- Category list loads in < 500ms
- Search results update in < 100ms
- Reordering responds immediately to user input
- Support 100+ categories without performance degradation

#### NFR2: User Experience
- All actions provide immediate visual feedback
- Error messages are clear and actionable
- Success confirmations are non-intrusive
- Empty states provide guidance

#### NFR3: Data Integrity
- Category operations are atomic
- Photo assignments maintain referential integrity
- Failed operations roll back completely
- No orphaned photos after category deletion

---

## 3. TECHNICAL IMPLEMENTATION GUIDANCE

### Architecture Overview
```
┌─────────────────────────────────────────┐
│         SwiftUI Views                   │
│  (CategoryManagementView + Components)  │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│       CategoryViewModel                  │
│    (State Management & Business Logic)   │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│        CategoryManager                   │
│    (Domain Logic & Coordination)         │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│      CategoryRepository                  │
│    (Data Access & Persistence)           │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         Core Data                        │
│    (CategoryEntity + Relationships)      │
└──────────────────────────────────────────┘
```

### Implementation Steps

#### Step 1: Enhance Core Data Model
```swift
// Update CategoryEntity with missing fields
extension CategoryEntity {
    @NSManaged public var position: Int32
    @NSManaged public var iconResource: String?
    @NSManaged public var isDefault: Bool
    @NSManaged public var createdAt: Date
}
```

#### Step 2: Complete CategoryManager Implementation
- Add missing methods for reordering
- Implement merge functionality
- Add batch photo operations
- Implement category statistics

#### Step 3: Enhance CategoryViewModel
- Add sorting and filtering logic
- Implement drag and drop handlers
- Add batch selection state
- Implement progress tracking for bulk operations

#### Step 4: Build UI Components
- Create CategoryEditDialog (matching Android)
- Enhance CategoryManagementCard with reorder controls
- Add ColorPickerView with predefined colors
- Create IconPickerView for category icons
- Implement drag and drop visual feedback

#### Step 5: Implement Advanced Features
- Search with debouncing
- Batch selection UI
- Statistics dashboard
- Export/import categories

### Key Code Patterns

#### Pattern 1: Predefined Colors (match Android)
```swift
struct CategoryColors {
    static let predefinedColors = [
        "#E91E63", // Pink
        "#F44336", // Red
        "#9C27B0", // Purple
        "#4CAF50", // Green
        "#2196F3", // Blue
        "#FF9800", // Orange
        "#795548", // Brown
        "#607D8B", // Blue Grey
        "#009688", // Teal
        "#FFEB3B", // Yellow
        "#3F51B5", // Indigo
        "#FF5722"  // Deep Orange
    ]
}
```

#### Pattern 2: Category Validation
```swift
func validateCategoryName(_ name: String, excludingId: Int64? = nil) throws {
    // Check empty
    guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
        throw CategoryError.emptyName
    }

    // Check duplicate
    let normalized = name.lowercased().replacingOccurrences(of: " ", with: "_")
    if let existing = repository.getCategoryByNormalizedName(normalized),
       existing.id != excludingId {
        throw CategoryError.duplicateName(name)
    }
}
```

#### Pattern 3: Atomic Operations
```swift
func mergeCategories(_ sources: [Category], into target: Category) async throws {
    let context = persistentContainer.viewContext

    try await context.perform {
        // Begin transaction
        context.undoManager?.beginUndoGrouping()

        do {
            // Move all photos
            for source in sources {
                let photos = fetchPhotosForCategory(source.id)
                for photo in photos {
                    photo.categoryId = target.id
                }
                // Delete source category
                context.delete(source.entity)
            }

            try context.save()
            context.undoManager?.endUndoGrouping()
        } catch {
            context.rollback()
            throw error
        }
    }
}
```

---

## 4. UI/UX REQUIREMENTS

### Visual Design Requirements

#### Color Scheme
- Match Android Material Design colors where applicable
- Use iOS system colors for consistency
- Ensure sufficient contrast for accessibility

#### Layout Specifications
- **Category Card**:
  - Height: 72pt minimum
  - Padding: 16pt horizontal, 12pt vertical
  - Corner radius: 12pt
  - Shadow: 0.5pt blur, 10% opacity

- **Color Picker**:
  - Circle size: 40pt
  - Selected border: 3pt accent color
  - Grid layout: 6 columns

- **Reorder Controls**:
  - Button size: 44pt touch target
  - Icon size: 24pt
  - Disabled opacity: 0.3

### Interaction Patterns

#### Gestures
- **Tap**: Select/deselect items
- **Long Press**: Enter selection mode
- **Swipe**: Quick delete action (with confirmation)
- **Drag**: Reorder categories or assign photos

#### Animations
- **Card Selection**: Scale to 0.95, spring animation
- **Reorder**: Smooth position transitions, 0.3s duration
- **Delete**: Fade out with scale, 0.2s duration
- **Color Change**: Cross-fade transition

#### Feedback
- **Loading**: Skeleton screens for initial load
- **Success**: Brief toast message (2s duration)
- **Error**: Alert dialog with recovery options
- **Progress**: Linear progress bar for batch operations

### Accessibility Requirements
- All interactive elements have accessibility labels
- VoiceOver announces category photo counts
- Color selections include text labels for color blind users
- Minimum touch targets of 44x44pt
- Support for Dynamic Type

---

## 5. TESTING REQUIREMENTS

### Unit Tests
```swift
// CategoryManagerTests.swift
func testCreateCategoryWithDuplicateName()
func testDeleteLastCategory()
func testMergeCategoriesPhotoCount()
func testReorderCategoryPositions()
func testCategoryNameNormalization()
```

### Integration Tests
```swift
// CategoryWorkflowTests.swift
func testCreateEditDeleteFlow()
func testBatchPhotoAssignment()
func testCategoryMergeWorkflow()
func testSearchAndFilterPersistence()
```

### UI Tests
```swift
// CategoryManagementUITests.swift
func testEmptyStateNavigation()
func testColorPickerSelection()
func testReorderModeToggle()
func testDeleteConfirmationDialog()
func testSearchResultsUpdate()
```

### Test Data Requirements
- Minimum 10 test categories with varying photo counts
- Categories with special characters in names
- Maximum length category names (50 characters)
- Empty categories for filter testing

### Performance Benchmarks
- Category list with 100 items loads < 500ms
- Search through 100 categories < 100ms
- Reorder operation completes < 50ms
- Batch assign 50 photos < 2s

---

## 6. DEFINITION OF DONE

### Code Complete
- [ ] All CRUD operations functional
- [ ] Reordering fully implemented
- [ ] Search and filter working
- [ ] Batch operations complete
- [ ] Drag and drop functional
- [ ] Color and icon selection working

### Quality Assurance
- [ ] All unit tests passing (>80% coverage)
- [ ] All integration tests passing
- [ ] All UI tests passing
- [ ] No memory leaks detected
- [ ] Performance benchmarks met

### Documentation
- [ ] Code comments for complex logic
- [ ] README updated with new features
- [ ] API documentation complete
- [ ] User guide updated

### Review
- [ ] Code review completed
- [ ] UI/UX review passed
- [ ] Product owner approval received
- [ ] Accessibility audit passed

### Deployment Ready
- [ ] Feature flag configured (if applicable)
- [ ] Analytics events implemented
- [ ] Error tracking configured
- [ ] Rollback plan documented

---

## 7. IMPLEMENTATION PRIORITIES

### Phase 1: Core Functionality (Days 1-2)
1. Complete CategoryManager with all CRUD operations
2. Enhance Core Data model with missing fields
3. Implement basic CategoryManagementView
4. Add create/edit/delete workflows

### Phase 2: Visual Enhancements (Day 3)
1. Implement color picker with predefined colors
2. Add icon selection (optional, can use SF Symbols)
3. Polish UI with proper spacing and animations
4. Add loading and empty states

### Phase 3: Advanced Features (Days 4-5)
1. Implement reordering functionality
2. Add search and filter capabilities
3. Implement batch operations
4. Add drag and drop support

### Phase 4: Testing and Polish (Day 6)
1. Write comprehensive tests
2. Fix bugs and edge cases
3. Performance optimization
4. Accessibility improvements

---

## 8. NOTES FOR DEVELOPER

### Critical Paths
1. **Data Migration**: Ensure existing categories are preserved when adding new fields
2. **Photo Relationships**: Maintain integrity when deleting/merging categories
3. **Position Management**: Ensure positions remain sequential without gaps

### Common Pitfalls to Avoid
- Don't allow deletion of the last category
- Ensure normalized names are unique (case-insensitive)
- Handle Core Data save failures gracefully
- Prevent race conditions in batch operations

### Android Parity Checklist
- [ ] Default categories match Android (Family, Cars, Games, Sports)
- [ ] Color palette identical to Android
- [ ] Delete behavior matches (with photo options)
- [ ] Search is case-insensitive like Android
- [ ] Position-based ordering works the same

### Performance Considerations
- Use `NSFetchedResultsController` for efficient list updates
- Implement pagination if category count > 100
- Cache photo counts to avoid repeated queries
- Use batch operations for bulk updates

### Future Enhancements (Not in Current Scope)
- Cloud sync for categories
- Category templates/presets
- Nested categories/subcategories
- Category sharing between users
- AI-suggested categories based on photos

---

## SUCCESS METRICS

### Quantitative
- 100% feature parity with Android
- <500ms load time for 100 categories
- 0 crashes in category operations
- >95% test coverage

### Qualitative
- Smooth, intuitive user experience
- Consistent with iOS design guidelines
- No data loss during operations
- Clear error messages and recovery paths

---

## REFERENCE IMPLEMENTATION

The Android implementation can be found at:
- `/android/app/src/main/java/com/smilepile/ui/screens/CategoryManagementScreen.kt`
- `/android/app/src/main/java/com/smilepile/ui/viewmodels/CategoryViewModel.kt`
- `/android/app/src/main/java/com/smilepile/managers/CategoryManager.kt`

Use these as reference for feature completeness and behavior consistency.

---

**End of Implementation Prompt**

This prompt should provide another developer with everything needed to implement iOS category management with full feature parity to Android. The implementation should take approximately 5-6 days for a skilled iOS developer.