package com.smilepile.mode

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val modeMutex = Mutex()

    private val _currentMode = MutableStateFlow(loadMode())
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

    private fun loadMode(): AppMode {
        val modeString = prefs.getString("current_mode", AppMode.PARENT.name)
        return AppMode.valueOf(modeString ?: AppMode.PARENT.name)
    }

    suspend fun setMode(mode: AppMode) {
        modeMutex.withLock {
            val saveSuccess = saveModeInternal(mode)
            if (!saveSuccess) {
                android.util.Log.e("ModeManager", "Failed to persist mode change - rolling back")
                return@withLock
            }

            _currentMode.value = mode
        }
    }

    suspend fun toggleMode() {
        modeMutex.withLock {
            val currentMode = _currentMode.value
            val newMode = if (currentMode == AppMode.KIDS) {
                AppMode.PARENT
            } else {
                AppMode.KIDS
            }

            val saveSuccess = saveModeInternal(newMode)
            if (!saveSuccess) {
                android.util.Log.e("ModeManager", "Failed to persist mode change - rolling back")
                return@withLock
            }

            _currentMode.value = newMode
        }
    }

    private suspend fun saveModeInternal(mode: AppMode): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                prefs.edit()
                    .putString("current_mode", mode.name)
                    .commit()
            } catch (e: Exception) {
                android.util.Log.e("ModeManager", "Mode persistence failed", e)
                false
            }
        }
    }

    fun isKidsMode(): Boolean = _currentMode.value == AppMode.KIDS
    fun isParentMode(): Boolean = _currentMode.value == AppMode.PARENT
}
