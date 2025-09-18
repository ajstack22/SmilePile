package com.smilepile

import android.app.Application
import com.smilepile.database.SmilePileDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * SmilePile Application class
 *
 * Initializes database and core components for optimal performance
 */
@HiltAndroidApp
class SmilePileApplication : Application() {

    // Application scope for background operations
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize logging (only in debug builds)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize database in background
        initializeDatabase()

        Timber.d("SmilePile Application initialized")
    }

    /**
     * Initialize database asynchronously for better app startup performance
     */
    private fun initializeDatabase() {
        applicationScope.launch {
            try {
                // Get database instance to trigger initialization
                val database = SmilePileDatabase.getDatabase(this@SmilePileApplication)

                // Perform database health check
                val isHealthy = database.checkDatabaseHealth()
                if (isHealthy) {
                    Timber.d("Database initialized successfully")
                } else {
                    Timber.w("Database health check failed")
                }

                // Pre-warm database connection for better first-query performance
                database.categoryDao().getAllActiveCategories()

            } catch (e: Exception) {
                Timber.e(e, "Database initialization failed")
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Close database connection
        SmilePileDatabase.closeDatabase()
    }
}