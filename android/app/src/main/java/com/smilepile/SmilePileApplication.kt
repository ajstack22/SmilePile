package com.smilepile

import android.app.Application
import androidx.lifecycle.lifecycleScope
import com.smilepile.data.database.SmilePileDatabase
import com.smilepile.data.repository.CategoryRepository
import com.smilepile.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SmilePileApplication : Application() {

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var database: SmilePileDatabase

    @Inject
    lateinit var categoryRepository: CategoryRepository

    // Application-scoped coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SmilePileApplication
            private set

        fun getThemeManager(): ThemeManager = instance.themeManager

        fun getDatabase(): SmilePileDatabase = instance.database
    }
}