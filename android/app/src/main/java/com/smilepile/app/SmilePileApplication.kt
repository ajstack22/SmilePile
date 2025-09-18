package com.smilepile.app

import android.app.Application

/**
 * Application class for SmilePile app.
 * Handles application-level initialization and provides global access to core components.
 */
class SmilePileApplication : Application() {

    /**
     * Database will be initialized later when Room setup is complete
     */
    // val database: SmilePileDatabase by lazy { ... } // Commented out for initial build

    override fun onCreate() {
        super.onCreate()

        // Initialize application-level components here
        // This is a good place to initialize logging, crash reporting, etc.

        // For production apps, you might want to initialize:
        // - Crashlytics
        // - Analytics
        // - Dependency injection framework
        // - Background work managers
    }
}