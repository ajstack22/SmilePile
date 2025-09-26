package com.smilepile.theme

import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Theme mode options
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Singleton manager for app-wide theme state
 */
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeModePreference())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(calculateDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private fun loadThemeModePreference(): ThemeMode {
        val modeString = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(modeString ?: ThemeMode.SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    private fun calculateDarkMode(): Boolean {
        return when (_themeMode.value) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkMode()
        }
    }

    private fun isSystemInDarkMode(): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        _isDarkMode.value = calculateDarkMode()
        saveThemeModePreference(mode)
    }

    fun toggleTheme() {
        // When toggling, cycle through: System -> Light -> Dark -> System
        val newMode = when (_themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        setThemeMode(newMode)
    }

    /**
     * Updates the dark mode state when system configuration changes
     */
    fun onConfigurationChanged(newConfig: Configuration) {
        if (_themeMode.value == ThemeMode.SYSTEM) {
            _isDarkMode.value = calculateDarkMode()
        }
    }

    private fun saveThemeModePreference(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
    }
}