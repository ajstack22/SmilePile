package com.smilepile.ui.theme

import android.app.Activity
import android.content.Context
import androidx.core.content.ContextCompat
import com.smilepile.R

enum class AppTheme(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    RAINBOW("rainbow");

    companion object {
        fun fromValue(value: String): AppTheme {
            return values().find { it.value == value } ?: LIGHT
        }
    }
}

class ThemeManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun getCurrentTheme(): AppTheme {
        val themeValue = prefs.getString("current_theme", AppTheme.LIGHT.value) ?: AppTheme.LIGHT.value
        return AppTheme.fromValue(themeValue)
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("current_theme", theme.value).apply()
    }

    fun applyTheme(activity: Activity) {
        when (getCurrentTheme()) {
            AppTheme.LIGHT -> activity.setTheme(R.style.Theme_SmilePile_Light)
            AppTheme.DARK -> activity.setTheme(R.style.Theme_SmilePile_Dark)
            AppTheme.RAINBOW -> activity.setTheme(R.style.Theme_SmilePile_Rainbow)
        }
    }

    fun getPrimaryColor(): Int {
        return when (getCurrentTheme()) {
            AppTheme.LIGHT -> ContextCompat.getColor(context, R.color.light_primary)
            AppTheme.DARK -> ContextCompat.getColor(context, R.color.dark_primary)
            AppTheme.RAINBOW -> ContextCompat.getColor(context, R.color.rainbow_primary)
        }
    }

    fun getBackgroundColor(): Int {
        return when (getCurrentTheme()) {
            AppTheme.LIGHT -> ContextCompat.getColor(context, R.color.light_background)
            AppTheme.DARK -> ContextCompat.getColor(context, R.color.dark_background)
            AppTheme.RAINBOW -> ContextCompat.getColor(context, R.color.rainbow_background)
        }
    }
}