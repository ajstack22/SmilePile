plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.smilepile.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smilepile.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "2025.09.18.003"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        // viewBinding = true // Commented out for initial build
        // compose = true // Commented out for initial build
    }

    // composeOptions {
    //     kotlinCompilerExtensionVersion = "1.5.4"
    // }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Material Design 3
    implementation("com.google.android.material:material:1.11.0")

    // Compose dependencies commented out for initial build
    // implementation("androidx.activity:activity-compose:1.8.2")
    // implementation("androidx.compose.material3:material3:1.1.2")
    // implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    // implementation("androidx.compose.ui:ui")
    // implementation("androidx.compose.ui:ui-graphics")
    // implementation("androidx.compose.ui:ui-tooling-preview")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Room Database 2.6.0 (simplified - no compiler for initial setup)
    val roomVersion = "2.6.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    // ksp("androidx.room:room-compiler:$roomVersion") // Commented out for initial build

    // Glide 4.16.0 (simplified - no compiler for initial setup)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // ksp("com.github.bumptech.glide:compiler:4.16.0") // Commented out for initial build

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    // implementation("androidx.navigation:navigation-compose:2.7.6") // Commented out for initial build

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    // implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0") // Commented out for initial build

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // Compose test dependencies commented out for initial build
    // androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    // androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // debugImplementation("androidx.compose.ui:ui-tooling")
    // debugImplementation("androidx.compose.ui:ui-test-manifest")
}