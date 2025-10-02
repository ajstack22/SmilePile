# Test Coverage Tracking

## Overview

Test coverage is automatically tracked during each deployment via `deploy_qual.sh`. Coverage reports are generated for both Android and iOS platforms.

## Current Coverage Status

### Android
- **Target**: 30% instruction coverage
- **Current**: 4% overall (68% for repository layer)
- **Report Location**: `android/app/build/reports/jacoco/jacocoDebugTestReport/html/index.html`

### iOS
- **Target**: 30% code coverage
- **Current**: Tests compile, coverage being measured
- **Report Location**: `ios/test_results_*.xcresult`

## Viewing Coverage Reports

### Android (JaCoCo)

1. **After deployment**, open the HTML report:
   ```bash
   open android/app/build/reports/jacoco/jacocoDebugTestReport/html/index.html
   ```

2. **Manual generation**:
   ```bash
   cd android
   ./gradlew testDebugUnitTest jacocoDebugTestReport
   ```

3. **Key metrics shown**:
   - Instruction coverage (primary metric)
   - Branch coverage
   - Line coverage
   - Method coverage
   - Package-level breakdown

### iOS (Xcode Coverage)

1. **View coverage from xcresult bundle**:
   ```bash
   xcrun xccov view --report ios/test_results_*.xcresult
   ```

2. **Generate detailed JSON report**:
   ```bash
   xcrun xccov view --report --json ios/test_results_*.xcresult > coverage.json
   ```

3. **View in Xcode**:
   - Open the `.xcresult` bundle in Xcode
   - Go to Test navigator → Select test run → Coverage tab

## Coverage Targets by Package

### Android - High Priority (0-16% → 30%)

| Package | Current | Target | Tests Created |
|---------|---------|--------|---------------|
| `com.smilepile.ui.viewmodels` | 0% | 30% | ✅ 112 tests |
| `com.smilepile.data.repository` | 68% | 70% | ✅ 65 tests |
| `com.smilepile.data.backup` | 0% | 25% | ⏳ Pending |
| `com.smilepile.storage` | 0% | 30% | ⏳ Pending |
| `com.smilepile.security` | 0% | 30% | ⏳ Pending |

### iOS - High Priority (Unknown → 30%)

| Component | Target | Tests Status |
|-----------|--------|--------------|
| PINManager | 40% | ✅ Enhanced |
| StorageManager | 35% | ✅ Enhanced |
| BackupManager | 30% | ⚠️ Needs API fixes |
| CategoryRepository | 35% | ✅ Enhanced |
| SafeThumbnailGenerator | 40% | ✅ Enhanced |

## Deployment Integration

Coverage reports are automatically generated when running:

```bash
./deploy/deploy_qual.sh
```

### Key Features:

1. **Automatic Report Generation**: Coverage reports are created alongside test execution
2. **Non-Blocking**: Test failures don't block deployment (for now), allowing iteration
3. **Coverage Extraction**: Script attempts to display coverage percentage in logs
4. **Timestamped Results**: iOS results bundles include timestamps for tracking over time

### Skip Coverage/Tests

If you need to deploy without running tests:

```bash
SKIP_TESTS=true ./deploy/deploy_qual.sh
```

## Improving Coverage

### Adding New Tests

1. **Android ViewModels** (highest impact):
   - Location: `android/app/src/test/java/com/smilepile/ui/viewmodels/`
   - Use MockK for mocking
   - Use `runTest` for coroutines
   - Test state flows and user actions

2. **Android Repositories**:
   - Location: `android/app/src/test/java/com/smilepile/data/repository/`
   - Mock DAOs with MockK
   - Test CRUD operations and flows

3. **iOS Core Components**:
   - Location: `ios/SmilePileTests/`
   - Use async/await patterns
   - Test error handling and edge cases

### Test Templates

See [`TEST-COVERAGE-PLAN.md`](TEST-COVERAGE-PLAN.md) for test templates and examples.

## Tracking Progress

### Historical Tracking

1. Coverage reports are generated on each deployment
2. Android reports are overwritten (store historical snapshots manually if needed)
3. iOS xcresult bundles are timestamped automatically

### Recommended Workflow

1. Run deployment: `./deploy/deploy_qual.sh android`
2. Check coverage report
3. Identify low-coverage packages
4. Add tests to increase coverage
5. Repeat until 30% target achieved

## Next Steps

1. **Fix ViewModel test mocking** - Many tests fail due to mock setup issues
2. **Fix BackupManagerTests** - Update Photo model references
3. **Add storage/backup tests** - High line count, low coverage packages
4. **Target 30% overall** - Focus on high-impact areas first

## Resources

- [JaCoCo Documentation](https://www.jacoco.org/jacoco/)
- [Xcode Code Coverage](https://developer.apple.com/library/archive/documentation/DeveloperTools/Conceptual/testing_with_xcode/chapters/07-code_coverage.html)
- [MockK Documentation](https://mockk.io/)
- [Test Coverage Plan](TEST-COVERAGE-PLAN.md)
