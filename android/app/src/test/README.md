# SmilePile Database and Performance Tests

This directory contains comprehensive unit tests for validating SmilePile's database performance and system requirements.

## Test Requirements Covered

### F0006 - Database Performance
- **Requirement**: Room database queries complete in <50ms
- **Test File**: `DatabasePerformanceTest.kt`
- **Coverage**: Category/Photo insert, query, bulk operations, complex queries, search performance

### Large Collection Support
- **Requirement**: Support for 100+ photos per category
- **Test File**: `LargeCollectionTest.kt`
- **Coverage**: 100+ photos per category, 500+ photos scalability, multiple large categories, pagination

### F0002 - Photo Loading Performance
- **Requirement**: Photo loading time <500ms
- **Test File**: `PhotoLoadingPerformanceTest.kt`
- **Coverage**: Single photo loading, multiple photos, pagination, concurrent loading, search, metadata loading

### Session Stability
- **Requirement**: Extended session stability (30+ minutes)
- **Test File**: `SessionStabilityTest.kt`
- **Coverage**: Continuous operations, concurrent access, performance degradation, data integrity, resource cleanup

### Memory Management
- **Requirement**: Memory management for large collections
- **Test File**: `MemoryManagementTest.kt`
- **Coverage**: Large collection memory usage, leak prevention, concurrent operations, pagination efficiency, bulk operations cleanup

## Test Structure

```
/test/java/com/smilepile/app/
├── SmilePileTestSuite.kt              # Main test suite runner
├── data/database/
│   ├── entities/
│   │   ├── Photo.kt                   # Photo entity for testing
│   │   ├── Category.kt                # Category entity for testing
│   │   └── Album.kt                   # Album entity for testing
│   ├── dao/
│   │   ├── PhotoDao.kt                # Photo data access object
│   │   ├── CategoryDao.kt             # Category data access object
│   │   └── AlbumDao.kt                # Album data access object
│   ├── TestSmilePileDatabase.kt       # Test database configuration
│   ├── DatabasePerformanceTest.kt     # F0006 database performance tests
│   └── LargeCollectionTest.kt         # Large collection support tests
└── performance/
    ├── PhotoLoadingPerformanceTest.kt # F0002 photo loading tests
    ├── SessionStabilityTest.kt        # Extended session stability tests
    └── MemoryManagementTest.kt        # Memory management tests
```

## Running the Tests

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Suite
```bash
./gradlew test --tests "com.smilepile.app.SmilePileTestSuite"
```

### Run Individual Test Classes
```bash
# Database performance tests (F0006)
./gradlew test --tests "com.smilepile.app.data.database.DatabasePerformanceTest"

# Large collection tests
./gradlew test --tests "com.smilepile.app.data.database.LargeCollectionTest"

# Photo loading performance tests (F0002)
./gradlew test --tests "com.smilepile.app.performance.PhotoLoadingPerformanceTest"

# Session stability tests
./gradlew test --tests "com.smilepile.app.performance.SessionStabilityTest"

# Memory management tests
./gradlew test --tests "com.smilepile.app.performance.MemoryManagementTest"
```

### Run Specific Test Methods
```bash
# Test database query performance
./gradlew test --tests "com.smilepile.app.data.database.DatabasePerformanceTest.testPhotoQueryByCategoryPerformance"

# Test 100+ photos support
./gradlew test --tests "com.smilepile.app.data.database.LargeCollectionTest.testSupport100PhotosPerCategory"

# Test photo loading performance
./gradlew test --tests "com.smilepile.app.performance.PhotoLoadingPerformanceTest.testMultiplePhotoLoadingPerformance"
```

## Test Data

All tests use in-memory Room databases with generated test data:
- **Categories**: Test categories with descriptive names
- **Photos**: Mock photos with realistic metadata (file sizes, dates, tags)
- **Albums**: Test albums for organizational testing

## Performance Thresholds

The tests validate the following performance thresholds:
- **Database queries**: <50ms (F0006)
- **Photo loading**: <500ms (F0002)
- **Single photo loading**: <100ms
- **Bulk operations**: <200ms for 100 items
- **Memory usage**: <50% increase for large collections
- **Session stability**: Sustained operations over time

## Test Environment

- **Database**: In-memory Room database
- **Threading**: Main thread queries allowed for testing
- **Memory**: Runtime memory monitoring
- **Garbage Collection**: Forced GC for memory tests

## Interpreting Results

### Success Criteria
- All operations complete within time thresholds
- Memory usage remains bounded
- No memory leaks detected
- Data integrity maintained
- Error rates <5% under stress conditions

### Failure Analysis
- Check console output for timing measurements
- Review memory usage snapshots
- Verify test data setup
- Examine concurrent operation results

## Dependencies

These tests require the following dependencies (already included in build.gradle.kts):
- Room testing library
- Kotlin coroutines test
- JUnit 4
- AndroidX Test

## Notes

- Tests use realistic data patterns and sizes
- Session stability tests are shortened for unit testing but represent extended usage
- Memory tests include garbage collection simulation
- Performance measurements account for test environment overhead