# Handoff Prompt - Test Fixes and Deployment Completion

## Context
You are taking over the SmilePile photo management app development. The previous developer has completed Sprint 5 P0 features (backup, export, sharing) and updated the deployment system. However, there are test failures blocking the deployment.

## Current Status
- **Sprint 5**: P0 features complete (backup, restore, export, sharing)
- **Deployment System**: Updated with StackMap/Manylla build numbering (YY.MM.DD.VVV format)
- **Current Version**: 25.09.26.001
- **Blocker**: Test failures preventing deployment

## Immediate Task: Fix Failing Tests

### Known Test Issues
1. **BackupManagerTest.kt** - Mock initialization issues:
   - `isDarkMode` returns Flow<Boolean> but expects StateFlow<Boolean>
   - Missing parameters for security manager mocks
   - Suspension function call issues

2. **RestoreManagerTest.kt** - Similar mock issues:
   - Theme manager mock expecting wrong method signatures
   - Flow vs StateFlow type mismatches

3. **PhotoImportManagerTest.kt** - 4 failing tests:
   - Import statistics tracking
   - Single photo import
   - Metadata extraction
   - Duplicate detection

### Test Fix Requirements
1. Update all mocks to match current interfaces
2. Fix Flow/StateFlow type mismatches
3. Ensure suspension functions are called correctly
4. Maintain test coverage above 80%

## Deployment Instructions (After Tests Pass)

### CRITICAL: Deployment Policy
- **NEVER** skip tests without explicit user approval
- Tests are **MANDATORY** for all deployments
- Use `deploy_qual.sh` for ALL deployments

### Deployment Command
Once all tests pass, deploy using:
```bash
./deploy/deploy_qual.sh both
```

This will:
1. Update version to next build (25.09.27.001 if new day, or 25.09.26.002 if same day)
2. Run all tests (must pass)
3. Run SonarCloud analysis
4. Build both platforms
5. Deploy to local devices
6. Commit with version in message
7. Tag as v25.09.27.001

## Next Priority Work (After Deployment)

### Option 1: Sprint 5 P1/P2 Features
If continuing Sprint 5, implement:
- Automated backup scheduling
- Incremental backups
- Cloud integration
- Advanced sharing options

### Option 2: Sprint 6 - Performance Optimization
New sprint focusing on:
- Image loading optimization
- Caching improvements
- Memory management
- App startup time

### Option 3: Sprint 6 - Advanced Features
New sprint for:
- AI-powered photo organization
- Face recognition grouping
- Smart albums
- Advanced search

## Key Files and Locations

### Test Files to Fix
- `/Users/adamstack/SmilePile/android/app/src/test/java/com/smilepile/backup/BackupManagerTest.kt`
- `/Users/adamstack/SmilePile/android/app/src/test/java/com/smilepile/backup/RestoreManagerTest.kt`
- `/Users/adamstack/SmilePile/android/app/src/test/java/com/smilepile/storage/PhotoImportManagerTest.kt`

### Deployment System
- `/Users/adamstack/SmilePile/deploy/deploy_qual.sh` - Main deployment script
- `/Users/adamstack/SmilePile/deploy/lib/build_number.sh` - Version management
- `/Users/adamstack/SmilePile/atlas/docs/AGENT_WORKFLOW.md` - Deployment methodology

### Recent Implementations
- `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/data/backup/` - Backup system
- `/Users/adamstack/SmilePile/ios/SmilePile/Data/Backup/` - iOS backup system
- `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/sharing/` - Sharing feature

## Development Guidelines

### Code Standards
- Kotlin for Android, Swift for iOS
- MVVM architecture
- Coroutines/Flow for Android async
- async/await for iOS
- Maintain feature parity between platforms

### Testing Standards
- Unit test all new code
- Maintain 80%+ coverage
- Use Robolectric for Android system classes
- Mock external dependencies properly

### Git Workflow
- Feature branches optional (currently on main)
- Descriptive commit messages
- Use conventional commits (feat:, fix:, docs:, etc.)
- Tag releases with version

## Quick Commands

### Run Tests
```bash
# Android tests
cd android && ./gradlew testDebugUnitTest

# Check specific test
./gradlew testDebugUnitTest --tests "*BackupManagerTest"
```

### Build
```bash
# Android
cd android && ./gradlew assembleDebug

# iOS
cd ios && xcodebuild -project SmilePile.xcodeproj -scheme SmilePile -configuration Debug
```

### Deploy (After Tests Pass)
```bash
# Full deployment with tests
./deploy/deploy_qual.sh both

# Dry run to test
DRY_RUN=true ./deploy/deploy_qual.sh both
```

## Success Criteria
1. All tests passing (100% pass rate)
2. Successful deployment via deploy_qual.sh
3. Version properly incremented
4. Code committed and tagged
5. Ready for next sprint/feature work

## Notes
- User prefers quality over speed
- Never skip tests without explicit permission
- Maintain cross-platform feature parity
- Document all significant changes
- The app is for family photo management with Kids Mode

Good luck! Fix those tests, deploy successfully, and then move on to the next priority work!