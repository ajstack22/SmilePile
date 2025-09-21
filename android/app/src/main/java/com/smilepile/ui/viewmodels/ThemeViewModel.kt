package com.smilepile.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.smilepile.theme.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel for managing app-wide theme state
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeManager: ThemeManager
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = themeManager.isDarkMode

    fun toggleTheme() {
        themeManager.toggleTheme()
    }

    fun setDarkMode(isDark: Boolean) {
        themeManager.setDarkMode(isDark)
    }
}