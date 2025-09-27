# Technical Debt Register

## Sprint 3 - September 26, 2025

### TD-001: PhotoImportSafetyTest Mock Configuration
**Priority:** Medium
**Component:** Android Testing
**Impact:** Test coverage gaps for photo import safety features

#### Issue Description
5 tests in PhotoImportSafetyTest are failing due to MockK configuration issues with:
- Static method mocking (Uri.fromFile)
- Coroutine suspension function mocking
- JaCoCo instrumentation conflicts with MockK

#### Failing Tests
1. testCorruptedPhotoHandling
2. testConcurrentImports
3. testImportWithFilePermissionIssue
4. testImportRecoveryAfterFailure
5. testThumbnailGenerationFailureDoesNotBlockImport

#### Root Cause
MockK has issues when:
- Mocking Android system classes (Uri)
- Working with JaCoCo instrumentation
- Handling coroutine contexts in tests

#### Recommended Solution
1. Refactor tests to use test doubles instead of mocks for Uri
2. Consider using Robolectric for Android system class testing
3. Separate unit tests from integration tests

#### Workaround
The actual StorageManager implementation has been secured with:
- Circuit breaker pattern for resilience
- Proper error handling
- File operation validation

These safety measures are in place despite test failures.

#### Acceptance Criteria for Resolution
- [ ] All 6 PhotoImportSafetyTest tests passing
- [ ] No MockK exceptions in test execution
- [ ] Tests run successfully with JaCoCo enabled

---

### TD-002: JaCoCo Coverage Reports Not Generating
**Priority:** High
**Component:** Build Configuration
**Impact:** Cannot verify test coverage metrics

#### Issue Description
JaCoCo is configured but reports are not being generated at:
`/android/app/build/reports/jacoco/jacocoDebugTestReport/`

#### Root Cause
Potential issues:
- Task dependency ordering
- Instrumentation conflicts
- Missing source sets configuration

#### Recommended Solution
1. Review gradle task dependencies
2. Ensure testDebugUnitTest runs before jacocoDebugTestReport
3. Verify source sets are correctly configured

---

### TD-003: SonarCloud Quality Gate Configuration
**Priority:** High
**Component:** CI/CD
**Impact:** Quality gate failing despite improvements

#### Issue Description
SonarCloud Quality Gate shows FAILED status even though:
- Critical security issues fixed
- 83% tests passing
- Code quality improved

#### Recommended Solution
1. Review quality gate thresholds
2. Configure project-specific quality profiles
3. Set realistic coverage targets for MVP

---

## Resolution Plan

### Sprint 4 - Week 1
- Allocate 1 day for test infrastructure improvements
- Implement Robolectric for system class testing
- Fix JaCoCo configuration

### Sprint 5
- Complete test coverage to 90%
- Resolve all technical debt items
- Implement automated quality gates