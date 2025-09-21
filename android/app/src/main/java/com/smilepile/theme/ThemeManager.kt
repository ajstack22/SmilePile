package com.smilepile.theme

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for app-wide theme state
 */
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(loadThemePreference())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private fun loadThemePreference(): Boolean {
        return prefs.getBoolean("is_dark_mode", false)
    }

    fun toggleTheme() {
        val newTheme = !_isDarkMode.value
        _isDarkMode.value = newTheme
        saveThemePreference(newTheme)
    }

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        saveThemePreference(isDark)
    }

    private fun saveThemePreference(isDarkMode: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", isDarkMode).apply()
    }
}