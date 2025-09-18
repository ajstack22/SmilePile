package com.smilepile.app.ui

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom test runner for SmilePile UI tests
 *
 * Provides additional configuration and setup for running
 * child-friendly UI and accessibility tests.
 */
class SmilePileTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, SmilePileTestApplication::class.java.name, context)
    }
}

/**
 * Test application class for UI testing
 *
 * Provides a clean test environment for running UI tests
 * without interference from production application logic.
 */
class SmilePileTestApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize test-specific configurations if needed
        setupTestEnvironment()
    }

    private fun setupTestEnvironment() {
        // Configure test-specific settings
        // This could include disabling animations, setting up mock data, etc.

        // For UI testing, we might want to disable animations to make tests more reliable
        try {
            // Note: In a real implementation, you might want to disable animations here
            // through system settings or other mechanisms
        } catch (e: Exception) {
            // Handle any setup exceptions gracefully
        }
    }
}