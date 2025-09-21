package com.smilepile.mode

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppMode {
    KIDS,
    PARENT
}

@Singleton
class ModeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("app_mode_prefs", Context.MODE_PRIVATE)

    private val _currentMode = MutableStateFlow(loadMode())
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

    private fun loadMode(): AppMode {
        val modeString = prefs.getString("current_mode", AppMode.KIDS.name)
        return AppMode.valueOf(modeString ?: AppMode.KIDS.name)
    }

    fun setMode(mode: AppMode) {
        _currentMode.value = mode
        saveMode(mode)
    }

    fun toggleMode() {
        val newMode = if (_currentMode.value == AppMode.KIDS) {
            AppMode.PARENT
        } else {
            AppMode.KIDS
        }
        setMode(newMode)
    }

    private fun saveMode(mode: AppMode) {
        prefs.edit().putString("current_mode", mode.name).apply()
    }

    fun isKidsMode(): Boolean = _currentMode.value == AppMode.KIDS
    fun isParentMode(): Boolean = _currentMode.value == AppMode.PARENT
}
