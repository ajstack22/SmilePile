# Wave 2: Component Decomposition (Weeks 3-4)

## CRITICAL: You are now the Atlas Orchestrator
You coordinate development through specialized agents. NEVER implement directly. Use the Atlas framework's parallel execution capabilities. Your role is to orchestrate Wave 2 of the SmilePile refactor, transforming monolithic UI files into modular components.

## Project Context
- **Current Problem**: PhotoGalleryScreen.kt has 1,013 lines (unmaintainable)
- **Other Large Files**: ParentalSettingsScreen (745 lines), ParentalLockScreen (664 lines)
- **Goal**: Break into <250 line components using Orchestrator pattern
- **Key Lesson**: "Extract by data flow boundaries, not visual boundaries"

## Wave 2 Objectives
1. Decompose PhotoGalleryScreen into orchestrator + 5 components
2. Extract universal dialog patterns (60% code reduction)
3. Create reusable component library
4. Maintain Kids Mode / Parent Mode separation

## Atlas Orchestration Commands

### Phase 1: Initialize Wave
```bash
# Resume project context
python3 00_orchestrator_context.py resume
python3 00_orchestrator_context.py objective "Decompose monolithic UI into modular components"

# Create stories
python3 02_create_story.py story "Decompose PhotoGalleryScreen using Orchestrator pattern" --priority critical
python3 02_create_story.py story "Extract universal dialog components" --priority high
python3 02_create_story.py story "Create reusable UI component library" --priority high

# Start workflow
python3 03_adversarial_workflow.py start WAVE2
```

### Phase 2: Parallel Agent Execution

**CRITICAL**: Maximum parallelization - these components are independent!

#### Analysis Agents (Spawn 3 in parallel - Hour 1)
```
Agent 1: "Analyze PhotoGalleryScreen data flows using 01_research.py"
Tasks:
- Map all state management patterns
- Identify data flow boundaries
- Document component dependencies
- List all UI operations (CRUD, navigation, etc.)

Agent 2: "Audit dialog patterns across codebase using 01_research.py"
Tasks:
- Find all dialog implementations
- Identify common patterns (add, edit, delete, confirm)
- Calculate potential code reduction
- Document dialog state management

Agent 3: "Research component communication patterns using 01_research.py"
Tasks:
- Analyze current prop drilling
- Find shared state requirements
- Document event flow patterns
- Identify reusable hooks potential
```

#### Decomposition Agents (Spawn 5 in parallel - Hours 2-4)
```
Agent 1: "Extract PhotoGridComponent from PhotoGalleryScreen"
Task: Create standalone photo grid display component
Target: ~200 lines
Responsibilities:
- Display photo grid
- Handle photo clicks
- Lazy loading optimization
- Grid layout management

Agent 2: "Extract CategoryFilterComponent"
Task: Create category filtering UI component
Target: ~80 lines
Responsibilities:
- Display category chips
- Handle category selection
- Manage filter state
- Visual feedback

Agent 3: "Extract SelectionToolbarComponent"
Task: Create photo selection toolbar
Target: ~100 lines
Responsibilities:
- Multi-select mode
- Bulk operations UI
- Selection count display
- Action buttons

Agent 4: "Create UniversalCrudDialog component"
Task: One dialog for all add/edit/delete operations
Target: ~150 lines
Features:
- Mode-based rendering (add/edit/delete)
- Consistent styling
- Loading states
- Error handling

Agent 5: "Create PhotoGalleryOrchestrator"
Task: Main orchestrator component
Target: ~150 lines
Responsibilities:
- Coordinate child components
- Manage shared state
- Handle navigation
- Mode switching (Kids/Parent)
```

#### Refactoring Agents (Spawn 3 in parallel - Hours 5-6)
```
Agent 1: "Update PhotoGalleryScreen to use new components"
Task: Refactor main screen to use extracted components
- Replace inline code with component imports
- Wire up orchestrator pattern
- Maintain all functionality

Agent 2: "Create component library structure"
Task: Organize extracted components
Structure:
components/
├── gallery/
│   ├── PhotoGridComponent.kt
│   ├── CategoryFilterComponent.kt
│   └── SelectionToolbarComponent.kt
├── dialogs/
│   ├── UniversalCrudDialog.kt
│   ├── ConfirmationDialog.kt
│   └── PhotoImportDialog.kt
└── shared/
    ├── LoadingIndicator.kt
    └── EmptyState.kt

Agent 3: "Apply same pattern to ParentalSettingsScreen"
Task: Quick decomposition of second largest file
- Extract settings sections
- Create reusable preference components
- Target: <300 lines for main file
```

### Phase 3: Validation & Testing

#### Test Agent
```
Agent: "Validate decomposed components"
Tasks:
- Verify all functionality preserved
- Test Kids Mode restrictions still work
- Validate Parent Mode features
- Performance testing (scrolling, loading)
- Generate before/after metrics
```

## Implementation Guidelines

### The Orchestrator Pattern (from StackMap team)
```kotlin
// PhotoGalleryOrchestrator.kt (~150 lines)
@Composable
fun PhotoGalleryOrchestrator(
    viewModel: PhotoGalleryViewModel = hiltViewModel(),
    modeViewModel: AppModeViewModel = hiltViewModel(),
    onPhotoClick: (Photo) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val modeState by modeViewModel.uiState.collectAsState()

    // Orchestrate child components
    Column {
        if (modeState.isParentMode) {
            SelectionToolbarComponent(
                selectedCount = uiState.selectedPhotos.size,
                onDelete = viewModel::deleteSelected,
                onShare = viewModel::shareSelected
            )
        }

        CategoryFilterComponent(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = viewModel::selectCategory
        )

        PhotoGridComponent(
            photos = uiState.filteredPhotos,
            onPhotoClick = onPhotoClick,
            isSelectionMode = uiState.isSelectionMode
        )
    }

    // Dialogs managed centrally
    if (uiState.showCrudDialog) {
        UniversalCrudDialog(
            mode = uiState.crudMode,
            onDismiss = viewModel::closeCrudDialog,
            onConfirm = viewModel::processCrudAction
        )
    }
}
```

### Universal Dialog Pattern (from Manylla team)
```kotlin
// UniversalCrudDialog.kt (~150 lines)
@Composable
fun UniversalCrudDialog(
    mode: CrudMode,
    entity: Any? = null,
    onDismiss: () -> Unit,
    onConfirm: (Any) -> Unit
) {
    AlertDialog(
        title = {
            Text(when(mode) {
                CrudMode.ADD -> "Add ${entity.type}"
                CrudMode.EDIT -> "Edit ${entity.type}"
                CrudMode.DELETE -> "Confirm Delete"
            })
        },
        text = {
            when(mode) {
                CrudMode.ADD, CrudMode.EDIT -> EntityForm(entity)
                CrudMode.DELETE -> Text("Are you sure?")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(entity) }) {
                Text(when(mode) {
                    CrudMode.DELETE -> "Delete"
                    else -> "Save"
                })
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### Component Extraction Rules
```kotlin
// GOOD: Extracted by data flow boundary
@Composable
fun PhotoGridComponent(
    photos: List<Photo>,        // Data input
    onPhotoClick: (Photo) -> Unit  // Event output
) {
    // Self-contained grid logic
}

// BAD: Extracted by visual appearance
@Composable
fun TopHalfOfScreen(
    viewModel: PhotoGalleryViewModel  // Still coupled to parent
) {
    // Depends on parent's state
}
```

## Success Criteria & Evidence

### Required Metrics
```bash
# Before decomposition
python3 01_research.py --topic "file sizes" --type metrics

# After decomposition
python3 03_adversarial_workflow.py execute evidence --type refactor
```

Evidence must show:
1. ✅ PhotoGalleryScreen.kt reduced from 1,013 → ~150 lines
2. ✅ No component exceeds 250 lines
3. ✅ All functionality preserved
4. ✅ Kids Mode safety maintained
5. ✅ Performance unchanged or improved

### File Size Validation
```bash
# Check all component sizes
find app/src/main/java/com/smilepile/ui/components -name "*.kt" -exec wc -l {} \; | sort -rn

# Verify no file > 250 lines
for file in app/src/main/java/com/smilepile/ui/**/*.kt; do
    lines=$(wc -l < "$file")
    if [ $lines -gt 250 ]; then
        echo "❌ $file has $lines lines (exceeds 250)"
        exit 1
    fi
done
```

## Key Lessons Applied

### From StackMap Team:
✅ **Orchestrator pattern** - Central coordination, distributed logic
✅ **Data flow boundaries** - Extract based on data, not visuals
✅ **200-line rule** - Enforced strictly
✅ **Reusable hooks** - Extract logic to custom hooks

### From Manylla Team:
✅ **Universal dialogs** - One dialog, multiple modes
✅ **60% code reduction** - Through proper abstraction
✅ **Component library** - Organized, reusable structures
✅ **Simple over complex** - No over-engineering

## Parallel Execution Timeline

```
Hour 1: Analysis (3 agents) - Understand current structure
Hours 2-4: Decomposition (5 agents) - Extract components
Hours 5-6: Refactoring (3 agents) - Wire everything together
Hour 7: Validation (1 agent) - Verify functionality
Total: 7 hours (vs 28 hours sequential)
```

## Common Pitfalls to Avoid

❌ **Prop Drilling Hell**
```kotlin
// BAD: Passing everything through props
<Component1
    prop1={x} prop2={y} prop3={z}
    onEvent1={a} onEvent2={b} onEvent3={c}
/>

// GOOD: Use ViewModel/State management
<Component1 /> // Gets what it needs from ViewModel
```

❌ **Premature Abstraction**
```kotlin
// BAD: Creating abstractions for single-use cases
abstract class AbstractPhotoDisplayer<T: PhotoLike>

// GOOD: Simple, direct implementation
@Composable fun PhotoGrid(photos: List<Photo>)
```

❌ **Visual-Based Extraction**
```kotlin
// BAD: Components based on screen position
TopSection(), MiddleSection(), BottomSection()

// GOOD: Components based on functionality
CategoryFilter(), PhotoGrid(), SelectionToolbar()
```

## Final Checklist

Before marking Wave 2 complete:
- [ ] PhotoGalleryScreen < 200 lines
- [ ] 5+ extracted components
- [ ] Universal dialog pattern implemented
- [ ] Component library structure created
- [ ] All functionality preserved
- [ ] Kids Mode safety verified
- [ ] Parent Mode features working
- [ ] No component > 250 lines
- [ ] Performance metrics collected

## Next Wave Preview
Wave 3 will implement photo deletion (library removal only) and local import/export functionality using the newly modularized components.

---

**REMEMBER**: You are the Orchestrator. Maximum parallelization is key - these components don't depend on each other, so spawn all agents simultaneously. The 5 decomposition agents should work in parallel for maximum efficiency.

**START COMMAND**: Copy this entire file and paste to your LLM to begin Wave 2 execution.