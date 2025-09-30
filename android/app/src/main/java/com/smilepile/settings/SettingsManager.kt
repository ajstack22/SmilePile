package com.smilepile.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized settings manager using DataStore for persistent user preferences
 * Provides type-safe access to all app settings with automatic migration support
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SETTINGS_NAME = "smilepile_settings"

        // Kids Mode Settings
        private val KEY_KIDS_MODE_ENABLED = booleanPreferencesKey("kids_mode_enabled")
        private val KEY_KIDS_MODE_PIN = stringPreferencesKey("kids_mode_pin")
        private val KEY_KIDS_MODE_PIN_ENABLED = booleanPreferencesKey("kids_mode_pin_enabled")

        // Gallery View Preferences
        private val KEY_GRID_SIZE = intPreferencesKey("gallery_grid_size")
        private val KEY_SORT_ORDER = stringPreferencesKey("gallery_sort_order")
        private val KEY_SHOW_HIDDEN = booleanPreferencesKey("show_hidden_photos")
        private val KEY_SHOW_DATES = booleanPreferencesKey("show_photo_dates")

        // Theme Preferences
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors_enabled")

        // Photo Quality Settings
        private val KEY_UPLOAD_QUALITY = stringPreferencesKey("upload_quality")
        private val KEY_THUMBNAIL_QUALITY = stringPreferencesKey("thumbnail_quality")
        private val KEY_AUTO_OPTIMIZE = booleanPreferencesKey("auto_optimize_storage")

        // Auto-backup Preferences
        private val KEY_AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        private val KEY_BACKUP_WIFI_ONLY = booleanPreferencesKey("backup_wifi_only")
        private val KEY_BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        private val KEY_LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")

        // Import/Export Preferences
        private val KEY_DEFAULT_CATEGORY = stringPreferencesKey("default_import_category")
        private val KEY_AUTO_CATEGORIZE = booleanPreferencesKey("auto_categorize_imports")
        private val KEY_PRESERVE_METADATA = booleanPreferencesKey("preserve_photo_metadata")

        // Notification Settings
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_BACKUP_NOTIFICATIONS = booleanPreferencesKey("backup_notifications")
        private val KEY_MEMORY_NOTIFICATIONS = booleanPreferencesKey("memory_notifications")

        // Security Settings
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_LOCK_TIMEOUT = intPreferencesKey("lock_timeout_minutes")

        // Performance Settings
        private val KEY_CACHE_SIZE_MB = intPreferencesKey("cache_size_mb")
        private val KEY_PRELOAD_IMAGES = booleanPreferencesKey("preload_adjacent_images")
        private val KEY_ANIMATION_SPEED = floatPreferencesKey("animation_speed")

        // App State
        private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        private val KEY_APP_VERSION = intPreferencesKey("app_version")
        private val KEY_MIGRATION_VERSION = intPreferencesKey("migration_version")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // Default Values
        const val DEFAULT_GRID_SIZE = 3
        const val DEFAULT_CACHE_SIZE_MB = 100
        const val DEFAULT_LOCK_TIMEOUT = 5
        const val DEFAULT_ANIMATION_SPEED = 1.0f
    }

    // Create DataStore instance
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_NAME)

    /**
     * Gallery Sort Order Options
     */
    enum class SortOrder {
        DATE_NEWEST,
        DATE_OLDEST,
        NAME_ASC,
        NAME_DESC,
        SIZE_LARGEST,
        SIZE_SMALLEST
    }

    /**
     * Photo Quality Options
     */
    enum class PhotoQuality {
        ORIGINAL,
        HIGH,
        MEDIUM,
        LOW
    }

    /**
     * Backup Frequency Options
     */
    enum class BackupFrequency {
        MANUAL,
        HOURLY,
        DAILY,
        WEEKLY,
        MONTHLY
    }

    /**
     * Theme Mode Options
     */
    enum class ThemeMode {
        SYSTEM,
        LIGHT,
        DARK
    }

    // Kids Mode Settings

    suspend fun setKidsModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_KIDS_MODE_ENABLED] = enabled
        }
    }

    fun getKidsModeEnabled(): Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_KIDS_MODE_ENABLED] ?: true
        }

    suspend fun setKidsModePIN(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_KIDS_MODE_PIN] = pin
            preferences[KEY_KIDS_MODE_PIN_ENABLED] = true
        }
    }

    suspend fun clearKidsModePIN() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_KIDS_MODE_PIN)
            preferences[KEY_KIDS_MODE_PIN_ENABLED] = false
        }
    }

    fun getKidsModePIN(): Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_KIDS_MODE_PIN]
        }

    fun isKidsModePINEnabled(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_KIDS_MODE_PIN_ENABLED] ?: false
        }

    // Gallery View Preferences

    suspend fun setGridSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GRID_SIZE] = size.coerceIn(2, 5)
        }
    }

    fun getGridSize(): Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_GRID_SIZE] ?: DEFAULT_GRID_SIZE
        }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SORT_ORDER] = order.name
        }
    }

    fun getSortOrder(): Flow<SortOrder> = context.dataStore.data
        .map { preferences ->
            val orderName = preferences[KEY_SORT_ORDER] ?: SortOrder.DATE_NEWEST.name
            try {
                SortOrder.valueOf(orderName)
            } catch (e: IllegalArgumentException) {
                SortOrder.DATE_NEWEST
            }
        }

    suspend fun setShowHiddenPhotos(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_HIDDEN] = show
        }
    }

    fun getShowHiddenPhotos(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SHOW_HIDDEN] ?: false
        }

    suspend fun setShowPhotoDates(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_DATES] = show
        }
    }

    fun getShowPhotoDates(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SHOW_DATES] ?: true
        }

    // Theme Preferences

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    fun getThemeMode(): Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val modeName = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }

    suspend fun setDynamicColorsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_COLORS] = enabled
        }
    }

    fun getDynamicColorsEnabled(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DYNAMIC_COLORS] ?: true
        }

    // Photo Quality Settings

    suspend fun setUploadQuality(quality: PhotoQuality) {
        context.dataStore.edit { preferences ->
            preferences[KEY_UPLOAD_QUALITY] = quality.name
        }
    }

    fun getUploadQuality(): Flow<PhotoQuality> = context.dataStore.data
        .map { preferences ->
            val qualityName = preferences[KEY_UPLOAD_QUALITY] ?: PhotoQuality.HIGH.name
            try {
                PhotoQuality.valueOf(qualityName)
            } catch (e: IllegalArgumentException) {
                PhotoQuality.HIGH
            }
        }

    suspend fun setThumbnailQuality(quality: PhotoQuality) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THUMBNAIL_QUALITY] = quality.name
        }
    }

    fun getThumbnailQuality(): Flow<PhotoQuality> = context.dataStore.data
        .map { preferences ->
            val qualityName = preferences[KEY_THUMBNAIL_QUALITY] ?: PhotoQuality.MEDIUM.name
            try {
                PhotoQuality.valueOf(qualityName)
            } catch (e: IllegalArgumentException) {
                PhotoQuality.MEDIUM
            }
        }

    suspend fun setAutoOptimizeStorage(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_OPTIMIZE] = enabled
        }
    }

    fun getAutoOptimizeStorage(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_AUTO_OPTIMIZE] ?: false
        }

    // Auto-backup Preferences

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_BACKUP_ENABLED] = enabled
        }
    }

    fun getAutoBackupEnabled(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_AUTO_BACKUP_ENABLED] ?: false
        }

    suspend fun setBackupWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BACKUP_WIFI_ONLY] = wifiOnly
        }
    }

    fun getBackupWifiOnly(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_BACKUP_WIFI_ONLY] ?: true
        }

    suspend fun setBackupFrequency(frequency: BackupFrequency) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BACKUP_FREQUENCY] = frequency.name
        }
    }

    fun getBackupFrequency(): Flow<BackupFrequency> = context.dataStore.data
        .map { preferences ->
            val frequencyName = preferences[KEY_BACKUP_FREQUENCY] ?: BackupFrequency.DAILY.name
            try {
                BackupFrequency.valueOf(frequencyName)
            } catch (e: IllegalArgumentException) {
                BackupFrequency.DAILY
            }
        }

    suspend fun setLastBackupTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_BACKUP_TIME] = timestamp
        }
    }

    fun getLastBackupTime(): Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_LAST_BACKUP_TIME] ?: 0L
        }

    // Import/Export Preferences

    suspend fun setDefaultCategory(categoryId: String?) {
        context.dataStore.edit { preferences ->
            if (categoryId != null) {
                preferences[KEY_DEFAULT_CATEGORY] = categoryId
            } else {
                preferences.remove(KEY_DEFAULT_CATEGORY)
            }
        }
    }

    fun getDefaultCategory(): Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEFAULT_CATEGORY]
        }

    suspend fun setAutoCategorize(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_CATEGORIZE] = enabled
        }
    }

    fun getAutoCategorize(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_AUTO_CATEGORIZE] ?: false
        }

    suspend fun setPreserveMetadata(preserve: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PRESERVE_METADATA] = preserve
        }
    }

    fun getPreserveMetadata(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_PRESERVE_METADATA] ?: true
        }

    // Notification Settings

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    fun getNotificationsEnabled(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
        }

    suspend fun setBackupNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BACKUP_NOTIFICATIONS] = enabled
        }
    }

    fun getBackupNotifications(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_BACKUP_NOTIFICATIONS] ?: true
        }

    suspend fun setMemoryNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MEMORY_NOTIFICATIONS] = enabled
        }
    }

    fun getMemoryNotifications(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_MEMORY_NOTIFICATIONS] ?: false
        }

    // Security Settings

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    fun getBiometricEnabled(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] ?: false
        }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_LOCK_ENABLED] = enabled
        }
    }

    fun getAppLockEnabled(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_APP_LOCK_ENABLED] ?: false
        }

    suspend fun setLockTimeout(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOCK_TIMEOUT] = minutes.coerceIn(0, 60)
        }
    }

    fun getLockTimeout(): Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_LOCK_TIMEOUT] ?: DEFAULT_LOCK_TIMEOUT
        }

    // Performance Settings

    suspend fun setCacheSize(sizeMB: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CACHE_SIZE_MB] = sizeMB.coerceIn(50, 500)
        }
    }

    fun getCacheSize(): Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_CACHE_SIZE_MB] ?: DEFAULT_CACHE_SIZE_MB
        }

    suspend fun setPreloadImages(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PRELOAD_IMAGES] = enabled
        }
    }

    fun getPreloadImages(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_PRELOAD_IMAGES] ?: true
        }

    suspend fun setAnimationSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ANIMATION_SPEED] = speed.coerceIn(0.5f, 2.0f)
        }
    }

    fun getAnimationSpeed(): Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_ANIMATION_SPEED] ?: DEFAULT_ANIMATION_SPEED
        }

    // App State

    suspend fun setFirstLaunch(firstLaunch: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FIRST_LAUNCH] = firstLaunch
        }
    }

    fun isFirstLaunch(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_FIRST_LAUNCH] ?: true
        }

    // Onboarding Settings
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    fun hasCompletedOnboarding(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: false
        }

    suspend fun setAppVersion(version: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_VERSION] = version
        }
    }

    fun getAppVersion(): Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_APP_VERSION] ?: 0
        }

    suspend fun setMigrationVersion(version: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MIGRATION_VERSION] = version
        }
    }

    fun getMigrationVersion(): Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_MIGRATION_VERSION] ?: 0
        }

    // Batch Operations

    /**
     * Export all settings as a map for backup
     */
    suspend fun exportSettings(): Map<String, Any> {
        val preferences = context.dataStore.data.first()
        val settings = mutableMapOf<String, Any>()

        preferences.asMap().forEach { (key, value) ->
            settings[key.name] = value
        }

        return settings
    }

    /**
     * Import settings from a backup
     * @param settings Map of setting keys to values
     * @param overwrite If true, replaces all existing settings. If false, merges with existing.
     */
    suspend fun importSettings(settings: Map<String, Any>, overwrite: Boolean = false) {
        context.dataStore.edit { preferences ->
            if (overwrite) {
                preferences.clear()
            }

            settings.forEach { (key, value) ->
                when (key) {
                    // Boolean preferences
                    KEY_KIDS_MODE_ENABLED.name,
                    KEY_KIDS_MODE_PIN_ENABLED.name,
                    KEY_SHOW_HIDDEN.name,
                    KEY_SHOW_DATES.name,
                    KEY_DYNAMIC_COLORS.name,
                    KEY_AUTO_OPTIMIZE.name,
                    KEY_AUTO_BACKUP_ENABLED.name,
                    KEY_BACKUP_WIFI_ONLY.name,
                    KEY_AUTO_CATEGORIZE.name,
                    KEY_PRESERVE_METADATA.name,
                    KEY_NOTIFICATIONS_ENABLED.name,
                    KEY_BACKUP_NOTIFICATIONS.name,
                    KEY_MEMORY_NOTIFICATIONS.name,
                    KEY_BIOMETRIC_ENABLED.name,
                    KEY_APP_LOCK_ENABLED.name,
                    KEY_PRELOAD_IMAGES.name,
                    KEY_FIRST_LAUNCH.name -> {
                        if (value is Boolean) {
                            preferences[booleanPreferencesKey(key)] = value
                        }
                    }

                    // String preferences
                    KEY_KIDS_MODE_PIN.name,
                    KEY_SORT_ORDER.name,
                    KEY_THEME_MODE.name,
                    KEY_UPLOAD_QUALITY.name,
                    KEY_THUMBNAIL_QUALITY.name,
                    KEY_BACKUP_FREQUENCY.name,
                    KEY_DEFAULT_CATEGORY.name -> {
                        if (value is String) {
                            preferences[stringPreferencesKey(key)] = value
                        }
                    }

                    // Integer preferences
                    KEY_GRID_SIZE.name,
                    KEY_LOCK_TIMEOUT.name,
                    KEY_CACHE_SIZE_MB.name,
                    KEY_APP_VERSION.name,
                    KEY_MIGRATION_VERSION.name -> {
                        if (value is Int) {
                            preferences[intPreferencesKey(key)] = value
                        }
                    }

                    // Long preferences
                    KEY_LAST_BACKUP_TIME.name -> {
                        if (value is Long) {
                            preferences[longPreferencesKey(key)] = value
                        }
                    }

                    // Float preferences
                    KEY_ANIMATION_SPEED.name -> {
                        if (value is Float) {
                            preferences[floatPreferencesKey(key)] = value
                        }
                    }
                }
            }
        }
    }

    /**
     * Clear all settings and reset to defaults
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
            // Set critical defaults
            preferences[KEY_FIRST_LAUNCH] = false
            preferences[KEY_KIDS_MODE_ENABLED] = true
        }
    }

    /**
     * Migrate settings from SharedPreferences to DataStore
     * This should be called once on app upgrade
     */
    suspend fun migrateFromSharedPreferences(sharedPrefs: android.content.SharedPreferences) {
        val allPrefs = sharedPrefs.all
        val settings = mutableMapOf<String, Any>()

        // Map old SharedPreferences keys to new DataStore keys
        allPrefs.forEach { (key, value) ->
            when (key) {
                "theme_mode" -> settings[KEY_THEME_MODE.name] = value.toString()
                "kid_safe_mode" -> settings[KEY_KIDS_MODE_ENABLED.name] = value as? Boolean ?: true
                "parental_pin" -> settings[KEY_KIDS_MODE_PIN.name] = value.toString()
                "pin_enabled" -> settings[KEY_KIDS_MODE_PIN_ENABLED.name] = value as? Boolean ?: false
                "biometric_enabled" -> settings[KEY_BIOMETRIC_ENABLED.name] = value as? Boolean ?: false
                // Add more mappings as needed
            }
        }

        if (settings.isNotEmpty()) {
            importSettings(settings, overwrite = false)
        }
    }
}