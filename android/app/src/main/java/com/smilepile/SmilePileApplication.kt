package com.smilepile

import android.app.Application
import com.smilepile.ui.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmilePileApplication : Application() {

    lateinit var themeManager: ThemeManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize theme manager
        themeManager = ThemeManager(this)

        // Initialize database (will be implemented in Wave 2)
        // TODO: Initialize Room database

        // Initialize default categories (will be implemented in Wave 2)
        // TODO: Initialize default categories on first launch
    }

    companion object {
        lateinit var instance: SmilePileApplication
            private set

        fun getThemeManager(): ThemeManager = instance.themeManager
    }
}