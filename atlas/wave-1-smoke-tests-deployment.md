# Wave 1: Smoke Tests & Deployment Setup (Weeks 1-2)

## CRITICAL: You are now the Atlas Orchestrator
You coordinate development through specialized agents. NEVER implement directly. Use the Atlas framework's parallel execution capabilities and automation scripts. Your role is to orchestrate Wave 1 of the SmilePile refactor based on lessons from StackMap and Manylla teams.

## Project Context
- **App**: SmilePile (Android photo gallery for kids)
- **Current State**: Zero tests, 17 TODOs, 1000+ line files
- **Goal**: Implement pragmatic smoke tests and one-command deployment
- **Philosophy**: "Ship working features, not perfect architecture"

## Wave 1 Objectives
1. Create 3 critical integration tests (not unit tests)
2. Build one-command deployment script
3. Set up validation automation
4. Establish TODO/debug log limits

## Atlas Orchestration Commands

### Phase 1: Initialize Wave
```bash
# Start orchestration
python3 00_orchestrator_context.py new "SmilePile Wave 1 Testing"
python3 00_orchestrator_context.py objective "Implement smoke tests and deployment automation"

# Create stories for tracking
python3 02_create_story.py story "Implement 3 critical integration tests" --priority high
python3 02_create_story.py story "Create deployment validation script" --priority high
python3 02_create_story.py story "Set up one-command deployment" --priority high

# Start workflow
python3 03_adversarial_workflow.py start WAVE1
```

### Phase 2: Parallel Agent Execution

**CRITICAL**: Spawn ALL agents simultaneously for maximum efficiency!

#### Research Agents (Spawn 3 in parallel)
```
Agent 1: "Research existing test setup and dependencies using 01_research.py"
- Analyze android/app/build.gradle.kts for test configuration
- Find any existing test files or patterns
- Document testing dependencies already available

Agent 2: "Analyze critical user flows using 01_research.py"
- Map the complete photo lifecycle flow
- Identify Kids Mode safety checkpoints
- Document data persistence patterns

Agent 3: "Research Android deployment best practices using 01_research.py"
- Study gradlew commands for release builds
- Investigate ProGuard/R8 configuration
- Find APK optimization opportunities
```

#### Development Agents (Spawn 4 in parallel after research)
```
Agent 1: "Create integration test for photo lifecycle"
Task: Implement test for complete flow: add photo → assign category → view → remove from library
Location: android/app/src/androidTest/java/com/smilepile/
Use: @Test annotation, AndroidJUnit4 runner

Agent 2: "Create Kids Mode safety test"
Task: Test that Kids Mode blocks: settings access, photo deletion, category management
Verify: All protective measures work correctly

Agent 3: "Create data persistence test"
Task: Test photos survive app restart, categories persist, mode state maintained
Include: Kill and restart app simulation

Agent 4: "Build smilepile_deploy.sh script"
Task: Create deployment validation script with:
- APK size check (<20MB)
- Debug log count (max 5 Log.d statements)
- TODO count check (max 20)
- Console output validation
```

### Phase 3: Validation & Integration

#### Validation Agent
```
Agent: "Validate all implementations"
Tasks:
- Run all 3 integration tests
- Execute deployment script
- Verify one-command deployment works
- Generate evidence screenshots
```

## Success Criteria & Evidence

### Required Evidence
```bash
# Collect evidence
python3 03_adversarial_workflow.py execute evidence --type test
python3 03_adversarial_workflow.py execute evidence --type build
```

Evidence must include:
1. ✅ All 3 integration tests passing
2. ✅ Deployment script execution log
3. ✅ APK size verification
4. ✅ TODO count under 20
5. ✅ Debug log count under 5

### Validation Commands
```bash
# Run tests
./gradlew connectedAndroidTest

# Run deployment script
./scripts/smilepile_deploy.sh

# One-command deployment test
./scripts/deploy.sh
```

## Implementation Guidelines

### Integration Test Structure
```kotlin
@RunWith(AndroidJUnit4::class)
class SmilePileSmokeTests {

    @Test
    fun testCompletePhotoLifecycle() {
        // Add photo from test resources
        // Assign to category
        // Switch to Kids Mode
        // Verify photo visible
        // Switch to Parent Mode
        // Remove from library
        // Verify photo gone from app (but still in MediaStore)
    }

    @Test
    fun testKidsModeSafety() {
        // Enable Kids Mode
        // Attempt to access settings (should fail)
        // Attempt to delete photo (should fail)
        // Verify no management options visible
    }

    @Test
    fun testDataPersistence() {
        // Add test photos and categories
        // Set specific mode state
        // Restart app
        // Verify all data intact
    }
}
```

### Deployment Script (smilepile_deploy.sh)
```bash
#!/bin/bash
set -e

echo "🚀 SmilePile Deployment Validation"

# Check APK size
MAX_SIZE=20971520  # 20MB in bytes
APK_PATH="app/build/outputs/apk/release/app-release.apk"

if [ -f "$APK_PATH" ]; then
    SIZE=$(stat -f%z "$APK_PATH" 2>/dev/null || stat --format=%s "$APK_PATH")
    if [ $SIZE -gt $MAX_SIZE ]; then
        echo "❌ APK too large: $(($SIZE / 1048576))MB > 20MB"
        exit 1
    fi
    echo "✅ APK size OK: $(($SIZE / 1048576))MB"
fi

# Check debug logs
DEBUG_COUNT=$(grep -r "Log\.d" app/src/main/java --include="*.kt" | wc -l)
if [ $DEBUG_COUNT -gt 5 ]; then
    echo "❌ Too many debug logs: $DEBUG_COUNT (max 5)"
    exit 1
fi
echo "✅ Debug logs OK: $DEBUG_COUNT"

# Check TODO count
TODO_COUNT=$(grep -r "TODO" app/src/main --include="*.kt" --include="*.java" | wc -l)
if [ $TODO_COUNT -gt 20 ]; then
    echo "❌ Too many TODOs: $TODO_COUNT (max 20)"
    exit 1
fi
echo "✅ TODO count OK: $TODO_COUNT"

# Run smoke tests
echo "🧪 Running smoke tests..."
./gradlew connectedAndroidTest --info

echo "✅ All validation checks passed!"
```

### One-Command Deploy (deploy.sh)
```bash
#!/bin/bash
set -e

echo "📦 SmilePile One-Command Deployment"

# Clean
./gradlew clean

# Run validation
./scripts/smilepile_deploy.sh

# Build release
./gradlew assembleRelease
./gradlew bundleRelease

# Generate deployment notes
echo "📝 Deployment Notes:" > RELEASE_NOTES.txt
echo "- Date: $(date)" >> RELEASE_NOTES.txt
echo "- Commit: $(git rev-parse HEAD)" >> RELEASE_NOTES.txt
echo "- TODO Count: $(grep -r "TODO" app/src/main | wc -l)" >> RELEASE_NOTES.txt

echo "✅ Ready for Play Store upload!"
echo "📦 APK: app/build/outputs/apk/release/app-release.apk"
echo "📦 AAB: app/build/outputs/bundle/release/app-release.aab"
```

## Key Lessons from StackMap & Manylla

### What TO DO:
✅ **Only 3 critical tests** - More tests = more maintenance
✅ **Test actual user flows** - Not theoretical edge cases
✅ **30% coverage target** - Not 80% (diminishing returns)
✅ **Skip test option** - For emergency deploys
✅ **Integration over unit tests** - 80% of value from integration tests

### What NOT TO DO:
❌ Mock everything - Test real interactions
❌ Complex test frameworks - Keep it simple
❌ 100% coverage - Waste of time
❌ Test every function - Test critical paths only

## Parallel Execution Timeline

```
Hour 1-2: Research Phase (3 agents in parallel)
Hour 3-6: Development Phase (4 agents in parallel)
Hour 7-8: Validation & Integration
Total: 8 hours (vs 24 hours sequential)
```

## Final Checklist

Before marking Wave 1 complete:
- [ ] 3 integration tests pass on device/emulator
- [ ] smilepile_deploy.sh runs without errors
- [ ] One-command deploy.sh produces valid APK/AAB
- [ ] TODO count under 20
- [ ] Debug log count under 5
- [ ] APK size under 20MB
- [ ] Evidence collected and documented

## Next Wave Preview
Wave 2 will focus on component decomposition, breaking down the 1000+ line PhotoGalleryScreen into manageable components using the Orchestrator pattern.

---

**REMEMBER**: You are the Orchestrator. Spawn agents to do the work. Use Atlas scripts for coordination. Never implement directly. Maximize parallel execution for 3-5x speed improvement.

**START COMMAND**: Copy this entire file and paste to your LLM to begin Wave 1 execution.