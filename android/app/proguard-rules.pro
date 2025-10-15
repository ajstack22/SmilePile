# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Uncomment this to preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to hide the original source file name.
-renamesourcefileattribute SourceFile

# Keep data models
-keep class com.smilepile.data.models.** { *; }

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Parcelize
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Material Components
-keep class com.google.android.material.** { *; }
-keep class androidx.** { *; }

# Keep custom views
-keep class com.smilepile.ui.views.** { *; }

# ============================================================================
# Wave 3: Android 4-Tier Configuration
# ============================================================================

# Protect BuildConfig tier detection fields
# Without these rules, R8 may optimize away BUILD_TYPE_ENV constant
-keep class com.smilepile.BuildConfig {
    public static final java.lang.String BUILD_TYPE_ENV;
    public static final java.lang.String APPLICATION_ID;
    public static final java.lang.String VERSION_NAME;
    public static final int VERSION_CODE;
}

# Protect custom BuildConfig module
# Ensures tier detection methods remain accessible
-keep class com.smilepile.config.BuildConfig { *; }