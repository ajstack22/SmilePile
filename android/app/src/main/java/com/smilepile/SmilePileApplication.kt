package com.smilepile

import android.app.Application
import androidx.lifecycle.lifecycleScope
import com.smilepile.data.database.SmilePileDatabase
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.ui.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SmilePileApplication : Application() {

    lateinit var themeManager: ThemeManager
        private set

    @Inject
    lateinit var database: SmilePileDatabase

    @Inject
    lateinit var categoryRepository: CategoryRepository

    // Application-scoped coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize theme manager
        themeManager = ThemeManager(this)

        // Initialize database and default categories
        initializeDatabase()
    }

    /**
     * Initialize database and create default categories if needed
     */
    private fun initializeDatabase() {
        applicationScope.launch {
            try {
                // Initialize default categories on first launch
                categoryRepository.initializeDefaultCategories()
            } catch (e: Exception) {
                // Log error but don't crash the app
                android.util.Log.e("SmilePileApp", "Failed to initialize database", e)
            }
        }
    }

    companion object {
        lateinit var instance: SmilePileApplication
            private set

        fun getThemeManager(): ThemeManager = instance.themeManager

        fun getDatabase(): SmilePileDatabase = instance.database
    }
}