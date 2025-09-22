# Atlas Iteration Methodology: Build-Test-Grow

## Core Principle: Always Working, Always Tested

**Every iteration produces a working, tested system that's slightly better than the last.**

## The Problem We're Solving

The current "build everything then integrate" approach causes:
- ❌ Massive integration failures at the end
- ❌ Untested code accumulating
- ❌ No working system until everything is "done"
- ❌ Test coverage becomes a huge task at the end
- ❌ Debugging nightmares when nothing works together

## The Solution: Incremental Vertical Slices with Tests

### 1. Start with Minimal Working System (MWS)
The smallest possible system that:
- ✅ Compiles
- ✅ Runs
- ✅ Has one basic test
- ✅ Can be demonstrated

**Example for SmilePile:**
```kotlin
// Iteration 0: Display one image
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Just show one hardcoded image
        setContentView(R.layout.activity_main)
    }
}

// Test for Iteration 0
@Test
fun testAppLaunches() {
    val scenario = launchActivity<MainActivity>()
    onView(withId(R.id.imageView)).check(matches(isDisplayed()))
}
```

### 2. Each Iteration Adds ONE Feature + Tests

**The Iteration Formula:**
```
Previous Working Code + One New Feature + Tests for That Feature = New Working System
```

### Iteration Structure

```
┌─────────────────────────────────────────┐
│         Iteration N: Feature Name        │
├─────────────────────────────────────────┤
│ 1. Research (1-2 agents)                │
│    - How to implement                   │
│    - What to test                       │
│                                         │
│ 2. Implement (1 agent)                  │
│    - Write feature code                 │
│    - Write feature tests                │
│    - Verify compilation                 │
│                                         │
│ 3. Integrate (same agent)               │
│    - Merge with main branch             │
│    - Run ALL tests (old + new)         │
│    - Fix any broken tests              │
│                                         │
│ 4. Validate (1 agent)                   │
│    - Run full test suite               │
│    - Check test coverage                │
│    - Demo the feature                  │
│                                         │
│ 5. Commit                               │
│    - Git commit working code + tests   │
│    - Update coverage metrics           │
└─────────────────────────────────────────┘
```

## Test Accumulation Strategy

### Coverage Goals by Iteration
- Iteration 1: 20% coverage (basic smoke tests)
- Iteration 3: 40% coverage (core paths)
- Iteration 5: 60% coverage (main features)
- Iteration 7: 80% coverage (edge cases)
- Final: 85%+ coverage

### Test Types to Add Each Iteration

#### With UI Features
- Unit test for any new classes/methods
- UI test for the new screen/interaction
- Integration test if it touches data

#### With Data Features
- Unit test for data models
- Database test for operations
- Mock tests for repositories

#### With Business Logic
- Unit tests for all logic paths
- Edge case tests
- Error handling tests

## SmilePile Example Progression

### Iteration 0: MWS
**Feature**: Display one hardcoded image
**Tests**: App launches, image displays
**Coverage**: ~10%

### Iteration 1: Basic Swipe
**Feature**: Swipe between 3 hardcoded images
**Tests**:
- Swipe gesture works
- Correct image shows after swipe
- Boundary testing (can't swipe past end)
**Coverage**: ~20%

### Iteration 2: Dynamic Image Loading
**Feature**: Load images from a folder
**Tests**:
- Images load from storage
- Handle missing images
- Memory management test
**Coverage**: ~35%

### Iteration 3: Category Concept
**Feature**: Group images into categories
**Tests**:
- Category data model tests
- Category switching tests
- Empty category handling
**Coverage**: ~50%

### Iteration 4: Database Integration
**Feature**: Store metadata in Room
**Tests**:
- Database CRUD operations
- Migration tests
- Concurrent access tests
**Coverage**: ~65%

### Iteration 5: Import Functionality
**Feature**: Import photos from device
**Tests**:
- File picker integration
- Permission handling
- Import success/failure
**Coverage**: ~80%

## The Parallel Optimization

While maintaining iterative development, we can still parallelize:

```
During Iteration N:
├─ Developer A: Implementing Feature N + Tests
├─ Developer B: Refactoring code from N-1 + Adding more tests
├─ Tester C: Creating test scenarios for N+1
└─ Researcher D: Investigating approach for N+2
```

## Key Rules for Iterations

### The Five Commandments

1. **Thou Shalt Not Break the Build**
   - Every iteration MUST compile
   - Every iteration MUST pass existing tests

2. **Thou Shalt Write Tests First (When Possible)**
   - TDD approach: Write test, watch it fail, make it pass
   - At minimum: Write tests WITH the feature

3. **Thou Shalt Not Skip Tests**
   - No feature is "too simple" to test
   - No iteration is complete without tests

4. **Thou Shalt Accumulate Coverage**
   - Each iteration adds to total coverage
   - Never let coverage decrease

5. **Thou Shalt Demo Each Iteration**
   - If you can't demo it, it's not done
   - Working software over comprehensive documentation

## Benefits of This Approach

### For Development
- ✅ Always have working software
- ✅ Catch integration issues immediately
- ✅ Natural checkpoints for progress
- ✅ Easier debugging (smaller changes)

### For Testing
- ✅ Tests grow organically with code
- ✅ No massive "testing sprint" at end
- ✅ Coverage increases steadily
- ✅ Tests stay relevant to current code

### For Project Management
- ✅ Can demo at any point
- ✅ Clear progress metrics
- ✅ Risk mitigation (always have something)
- ✅ Easy to pivot if requirements change

## Enforcement Mechanisms

### In Orchestrator Prompt
```markdown
You MUST follow iterative development:
1. Start with Minimal Working System
2. Each wave adds ONE feature + tests
3. Never proceed without passing tests
4. Commit only working, tested code
```

### In Developer Agent Context
```python
'constraints': [
    'MUST build on existing working code',
    'MUST write tests for new features',
    'MUST ensure all tests pass',
    'MUST maintain or increase coverage',
    'NEVER commit untested code'
]
```

### In Iteration Manager Script
```python
def can_proceed_to_next_iteration():
    """Check if we can move forward"""
    return (
        build_passes() and
        all_tests_pass() and
        coverage_increased() and
        feature_is_demoed()
    )
```

## Migration Path for Existing Projects

If starting with a non-iterative codebase:

1. **Find the MWS** - What's the smallest part that works?
2. **Add tests to MWS** - Get baseline coverage
3. **Pick smallest feature** - Start iteration cycle
4. **Gradually refactor** - Improve as you iterate

## Success Metrics

- 🎯 Every iteration produces deployable software
- 🎯 Test coverage grows by 10-15% per iteration
- 🎯 Zero "integration hell" moments
- 🎯 Can demo progress daily
- 🎯 New team members can contribute immediately

## Remember

> "Make it work, make it right, make it fast" - Kent Beck

But Atlas says:
> "Make it work (with tests), keep it working (with more tests), make it better (with even more tests)"