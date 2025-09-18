package com.smilepile.app

import org.junit.runner.RunWith
import org.junit.runners.Suite
import com.smilepile.app.data.database.DatabasePerformanceTest
import com.smilepile.app.data.database.LargeCollectionTest
import com.smilepile.app.performance.PhotoLoadingPerformanceTest
import com.smilepile.app.performance.SessionStabilityTest
import com.smilepile.app.performance.MemoryManagementTest

/**
 * Test suite for SmilePile database and performance validation.
 *
 * This suite validates the following requirements:
 * - F0006: Room database queries complete in <50ms
 * - Support for 100+ photos per category
 * - F0002: Photo loading time <500ms
 * - Extended session stability (30+ minutes)
 * - Memory management for large collections
 *
 * Run this suite to validate all database and performance requirements.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    DatabasePerformanceTest::class,
    LargeCollectionTest::class,
    PhotoLoadingPerformanceTest::class,
    SessionStabilityTest::class,
    MemoryManagementTest::class
)
class SmilePileTestSuite