# Sprint 6 Android Implementation Plan

## Overview
This document provides detailed Android implementation specifications for Sprint 6 stories, following SmilePile's existing architecture patterns using Kotlin, Jetpack Compose, Room, and Hilt dependency injection.

---

## STORY-6.1: Backup Deletion Tracking

### Priority: P0 - Critical

### Implementation Files

#### 1. Database Schema Updates

**File:** `/android/app/src/main/java/com/smilepile/data/entities/DeletionTrackingEntity.kt`
```kotlin
package com.smilepile.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.smilepile.data.converters.MapTypeConverter

@Entity(tableName = "deletion_tracking")
data class DeletionTrackingEntity(
    @PrimaryKey
    val id: String,
    val itemId: String,
    val itemType: ItemType,
    val deletedAt: Long,
    val purgeAfter: Long,
    val metadata: Map<String, Any>,
    val isPurged: Boolean = false
)

enum class ItemType {
    PHOTO,
    CATEGORY
}
```

#### 2. DAO Implementation

**File:** `/android/app/src/main/java/com/smilepile/data/dao/DeletionTrackingDao.kt`
```kotlin
package com.smilepile.data.dao

import androidx.room.*
import com.smilepile.data.entities.DeletionTrackingEntity
import com.smilepile.data.entities.ItemType
import kotlinx.coroutines.flow.Flow

@Dao
interface DeletionTrackingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletion(deletion: DeletionTrackingEntity)

    @Query("SELECT * FROM deletion_tracking WHERE isPurged = 0")
    suspend fun getActiveDeletedItems(): List<DeletionTrackingEntity>

    @Query("SELECT * FROM deletion_tracking WHERE deletedAt > :timestamp AND isPurged = 0")
    suspend fun getDeletedItemsAfter(timestamp: Long): List<DeletionTrackingEntity>

    @Query("UPDATE deletion_tracking SET isPurged = 1 WHERE purgeAfter < :currentTime")
    suspend fun purgeExpiredDeletions(currentTime: Long): Int

    @Query("SELECT * FROM deletion_tracking WHERE itemType = :type ORDER BY deletedAt DESC")
    fun observeDeletionsByType(type: ItemType): Flow<List<DeletionTrackingEntity>>

    @Delete
    suspend fun removeDeletion(deletion: DeletionTrackingEntity)
}
```

#### 3. Repository Updates

**File:** `/android/app/src/main/java/com/smilepile/data/repository/DeletionTrackingRepository.kt`
```kotlin
package com.smilepile.data.repository

import com.smilepile.data.dao.DeletionTrackingDao
import com.smilepile.data.entities.DeletionTrackingEntity
import com.smilepile.data.entities.ItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeletionTrackingRepository @Inject constructor(
    private val deletionTrackingDao: DeletionTrackingDao
) {
    companion object {
        private const val DELETION_RETENTION_DAYS = 30
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }

    suspend fun trackDeletion(
        itemId: String,
        itemType: ItemType,
        metadata: Map<String, Any> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val deletion = DeletionTrackingEntity(
            id = UUID.randomUUID().toString(),
            itemId = itemId,
            itemType = itemType,
            deletedAt = currentTime,
            purgeAfter = currentTime + (DELETION_RETENTION_DAYS * MILLIS_PER_DAY),
            metadata = metadata,
            isPurged = false
        )
        deletionTrackingDao.insertDeletion(deletion)
    }

    suspend fun getDeletedPhotosAfter(timestamp: Long): List<DeletionTrackingEntity> =
        withContext(Dispatchers.IO) {
            deletionTrackingDao.getDeletedItemsAfter(timestamp)
                .filter { it.itemType == ItemType.PHOTO }
        }

    suspend fun getDeletedCategoriesAfter(timestamp: Long): List<DeletionTrackingEntity> =
        withContext(Dispatchers.IO) {
            deletionTrackingDao.getDeletedItemsAfter(timestamp)
                .filter { it.itemType == ItemType.CATEGORY }
        }

    suspend fun purgeExpiredDeletions() = withContext(Dispatchers.IO) {
        deletionTrackingDao.purgeExpiredDeletions(System.currentTimeMillis())
    }

    fun observeDeletionHistory(itemType: ItemType): Flow<List<DeletionTrackingEntity>> =
        deletionTrackingDao.observeDeletionsByType(itemType)
}
```

#### 4. BackupManager Integration

**Update:** `/android/app/src/main/java/com/smilepile/data/backup/BackupManager.kt`

At lines 1183-1185, replace TODOs with:
```kotlin
// Track deletions for incremental backup
deletedPhotos = deletionTrackingRepository.getDeletedPhotosAfter(
    lastBackupTimestamp
).map { deletion ->
    DeletedItem(
        id = deletion.itemId,
        deletedAt = deletion.deletedAt,
        metadata = deletion.metadata
    )
},
deletedCategories = deletionTrackingRepository.getDeletedCategoriesAfter(
    lastBackupTimestamp
).map { deletion ->
    DeletedItem(
        id = deletion.itemId,
        deletedAt = deletion.deletedAt,
        metadata = deletion.metadata
    )
}
```

#### 5. WorkManager for Scheduled Purging

**File:** `/android/app/src/main/java/com/smilepile/workers/DeletionPurgeWorker.kt`
```kotlin
package com.smilepile.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smilepile.data.repository.DeletionTrackingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DeletionPurgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val deletionTrackingRepository: DeletionTrackingRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val purgedCount = deletionTrackingRepository.purgeExpiredDeletions()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

### Testing Approach

```kotlin
// Unit Test Example
@Test
fun `trackDeletion should create deletion record with correct retention period`() = runTest {
    // Given
    val photoId = "test-photo-123"
    val currentTime = System.currentTimeMillis()

    // When
    repository.trackDeletion(photoId, ItemType.PHOTO)

    // Then
    val deletions = repository.getDeletedPhotosAfter(currentTime - 1000)
    assertTrue(deletions.isNotEmpty())
    assertEquals(photoId, deletions.first().itemId)
    assertTrue(deletions.first().purgeAfter > currentTime + 29 * 24 * 60 * 60 * 1000L)
}
```

---

## STORY-6.2: Backup PIN/Pattern Encryption

### Priority: P0 - Critical

### Implementation Files

#### 1. Encryption Service

**File:** `/android/app/src/main/java/com/smilepile/security/BackupEncryption.kt`
```kotlin
package com.smilepile.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupEncryption @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH = 128
        private const val ITERATIONS = 10000
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val BACKUP_KEY_ALIAS = "SmilePileBackupKey"
    }

    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    sealed class BackupCredential {
        data class DevicePin(val pin: String) : BackupCredential()
        data class DevicePattern(val pattern: String) : BackupCredential()
        data class UserPassword(val password: String) : BackupCredential()

        fun toCharArray(): CharArray = when (this) {
            is DevicePin -> pin.toCharArray()
            is DevicePattern -> pattern.toCharArray()
            is UserPassword -> password.toCharArray()
        }
    }

    data class EncryptedBackup(
        val encryptedData: ByteArray,
        val salt: ByteArray,
        val iv: ByteArray,
        val algorithm: String = ALGORITHM,
        val iterations: Int = ITERATIONS
    )

    fun hasDeviceLock(): Boolean = keyguardManager.isDeviceSecure

    suspend fun encryptBackup(
        data: ByteArray,
        credential: BackupCredential
    ): EncryptedBackup {
        val salt = generateSalt()
        val iv = generateIV()
        val key = deriveKey(credential, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val encryptedData = cipher.doFinal(data)

        return EncryptedBackup(
            encryptedData = encryptedData,
            salt = salt,
            iv = iv
        )
    }

    suspend fun decryptBackup(
        encryptedBackup: EncryptedBackup,
        credential: BackupCredential
    ): ByteArray {
        val key = deriveKey(credential, encryptedBackup.salt)

        val cipher = Cipher.getInstance(encryptedBackup.algorithm)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH, encryptedBackup.iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        return cipher.doFinal(encryptedBackup.encryptedData)
    }

    private fun deriveKey(credential: BackupCredential, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(
            credential.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec)
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun generateIV(): ByteArray {
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        return iv
    }

    // Store encryption key in Android KeyStore for additional security
    private fun storeKeyInKeyStore(key: SecretKey) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keyGenSpec = KeyGenParameterSpec.Builder(
                BACKUP_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(300)
                .build()

            keyGenerator.init(keyGenSpec)
            keyGenerator.generateKey()
        }
    }
}
```

#### 2. BackupManager Encryption Integration

**Update:** `/android/app/src/main/java/com/smilepile/data/backup/BackupManager.kt`

At line 1244, replace TODO with:
```kotlin
// Implement encryption when device lock is present
private suspend fun encryptBackupIfRequired(
    backupData: ByteArray,
    options: BackupOptions
): ByteArray {
    if (!options.useEncryption || !backupEncryption.hasDeviceLock()) {
        return backupData
    }

    val credential = when {
        options.encryptionPassword != null ->
            BackupEncryption.BackupCredential.UserPassword(options.encryptionPassword)
        backupEncryption.hasDeviceLock() ->
            requestDeviceCredential() // Implement UI flow
        else -> return backupData
    }

    val encryptedBackup = backupEncryption.encryptBackup(backupData, credential)

    // Wrap encrypted data in JSON envelope
    val envelope = BackupEnvelope(
        version = CURRENT_BACKUP_VERSION,
        encrypted = true,
        encryption = EncryptionMetadata(
            algorithm = encryptedBackup.algorithm,
            salt = Base64.encodeToString(encryptedBackup.salt, Base64.NO_WRAP),
            iv = Base64.encodeToString(encryptedBackup.iv, Base64.NO_WRAP),
            iterations = encryptedBackup.iterations
        ),
        data = Base64.encodeToString(encryptedBackup.encryptedData, Base64.NO_WRAP),
        checksum = calculateSHA256(encryptedBackup.encryptedData)
    )

    return json.encodeToString(envelope).toByteArray()
}
```

#### 3. UI for Encryption Settings

**File:** `/android/app/src/main/java/com/smilepile/ui/components/backup/EncryptionSettingsDialog.kt`
```kotlin
@Composable
fun EncryptionSettingsDialog(
    isDeviceLockAvailable: Boolean,
    onDismiss: () -> Unit,
    onPasswordSet: (String) -> Unit,
    onUseDeviceLock: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPasswordError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup Encryption") },
        text = {
            Column {
                if (isDeviceLockAvailable) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUseDeviceLock() },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Use device PIN/Pattern")
                        }
                    }

                    Text(
                        "OR",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        showPasswordError = false
                    },
                    label = { Text("Backup Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = showPasswordError,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        showPasswordError = false
                    },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = showPasswordError,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showPasswordError) {
                    Text(
                        "Passwords don't match or are too short (min 8 characters)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (password == confirmPassword && password.length >= 8) {
                        onPasswordSet(password)
                    } else {
                        showPasswordError = true
                    }
                }
            ) {
                Text("Set Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### Testing Approach

```kotlin
@Test
fun `encryptBackup should produce different output for same input with different IVs`() = runTest {
    // Given
    val data = "test data".toByteArray()
    val credential = BackupEncryption.BackupCredential.UserPassword("testPassword123")

    // When
    val encrypted1 = backupEncryption.encryptBackup(data, credential)
    val encrypted2 = backupEncryption.encryptBackup(data, credential)

    // Then
    assertNotEquals(encrypted1.encryptedData, encrypted2.encryptedData)
    assertNotEquals(encrypted1.iv, encrypted2.iv)
}
```

---

## STORY-6.4: Kids Mode Time Limits Enhancement

### Priority: P1 - Important

### Implementation Files

#### 1. Daily Time Tracker Service

**File:** `/android/app/src/main/java/com/smilepile/kidsmode/DailyTimeTracker.kt`
```kotlin
package com.smilepile.kidsmode

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyTimeTracker @Inject constructor(
    private val context: Context,
    private val sharedPreferences: SharedPreferences,
    private val notificationManager: KidsNotificationManager
) {

    data class TimeState(
        val dailyUsageMillis: Long = 0,
        val dailyLimitMillis: Long = 30 * 60 * 1000, // 30 min default
        val sessionStartTime: Long = 0,
        val isTimeLimitReached: Boolean = false,
        val remainingMillis: Long = 30 * 60 * 1000,
        val warningsShown: Set<WarningType> = emptySet()
    )

    enum class WarningType {
        FIVE_MINUTE,
        ONE_MINUTE,
        THIRTY_SECOND
    }

    private val _timeState = MutableStateFlow(TimeState())
    val timeState: StateFlow<TimeState> = _timeState.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    private var sessionStartTime: Long = 0

    init {
        loadDailyUsage()
        resetIfNewDay()
    }

    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        _timeState.value = _timeState.value.copy(sessionStartTime = sessionStartTime)
        scheduleUpdates()
    }

    fun pauseSession() {
        updateRunnable?.let { handler.removeCallbacks(it) }

        if (sessionStartTime > 0) {
            val sessionDuration = System.currentTimeMillis() - sessionStartTime
            val newTotalUsage = _timeState.value.dailyUsageMillis + sessionDuration

            _timeState.value = _timeState.value.copy(
                dailyUsageMillis = newTotalUsage,
                sessionStartTime = 0
            )

            saveDailyUsage()
        }
    }

    private fun scheduleUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateUsage()
                handler.postDelayed(this, 1000) // Update every second
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun updateUsage() {
        val currentSession = if (sessionStartTime > 0) {
            System.currentTimeMillis() - sessionStartTime
        } else 0

        val totalUsage = _timeState.value.dailyUsageMillis + currentSession
        val remaining = _timeState.value.dailyLimitMillis - totalUsage

        _timeState.value = _timeState.value.copy(
            remainingMillis = remaining.coerceAtLeast(0)
        )

        when {
            remaining <= 0 && !_timeState.value.isTimeLimitReached -> {
                onTimeLimitReached()
            }
            remaining in 1..30_000 &&
                WarningType.THIRTY_SECOND !in _timeState.value.warningsShown -> {
                showWarning(WarningType.THIRTY_SECOND, "30 seconds remaining!")
            }
            remaining in 30_001..60_000 &&
                WarningType.ONE_MINUTE !in _timeState.value.warningsShown -> {
                showWarning(WarningType.ONE_MINUTE, "1 minute remaining!")
            }
            remaining in 60_001..300_000 &&
                WarningType.FIVE_MINUTE !in _timeState.value.warningsShown -> {
                showWarning(WarningType.FIVE_MINUTE, "5 minutes remaining!")
            }
        }
    }

    private fun onTimeLimitReached() {
        _timeState.value = _timeState.value.copy(isTimeLimitReached = true)
        pauseSession()
        notificationManager.showTimeLimitReached()
    }

    private fun showWarning(type: WarningType, message: String) {
        val newWarnings = _timeState.value.warningsShown + type
        _timeState.value = _timeState.value.copy(warningsShown = newWarnings)
        notificationManager.showTimeWarning(message)
    }

    fun setDailyLimit(minutes: Int) {
        val limitMillis = minutes * 60 * 1000L
        _timeState.value = _timeState.value.copy(dailyLimitMillis = limitMillis)

        sharedPreferences.edit()
            .putLong("daily_limit_millis", limitMillis)
            .apply()
    }

    fun getRemainingTimeFormatted(): String {
        val remaining = _timeState.value.remainingMillis
        val minutes = (remaining / 60_000).toInt()
        val seconds = ((remaining % 60_000) / 1000).toInt()
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun getUsagePercentage(): Int {
        val state = _timeState.value
        return if (state.dailyLimitMillis > 0) {
            ((state.dailyUsageMillis * 100) / state.dailyLimitMillis).toInt()
                .coerceIn(0, 100)
        } else 0
    }

    private fun resetIfNewDay() {
        val lastUsageDate = sharedPreferences.getLong("last_usage_date", 0)
        val today = LocalDate.now().toEpochDay()

        if (lastUsageDate != today) {
            _timeState.value = TimeState(
                dailyLimitMillis = sharedPreferences.getLong(
                    "daily_limit_millis",
                    30 * 60 * 1000
                )
            )
            saveDailyUsage()
        }
    }

    private fun loadDailyUsage() {
        val today = LocalDate.now().toEpochDay()
        val savedDate = sharedPreferences.getLong("last_usage_date", 0)

        if (savedDate == today) {
            _timeState.value = TimeState(
                dailyUsageMillis = sharedPreferences.getLong("daily_usage_millis", 0),
                dailyLimitMillis = sharedPreferences.getLong("daily_limit_millis", 30 * 60 * 1000)
            )
        }
    }

    private fun saveDailyUsage() {
        sharedPreferences.edit()
            .putLong("daily_usage_millis", _timeState.value.dailyUsageMillis)
            .putLong("last_usage_date", LocalDate.now().toEpochDay())
            .apply()
    }
}
```

#### 2. Kids Mode Screen Integration

**Update:** `/android/app/src/main/java/com/smilepile/ui/screens/KidsModeGalleryScreen.kt`

Add time tracking UI:
```kotlin
@Composable
fun TimeRemainingOverlay(
    timeTracker: DailyTimeTracker,
    modifier: Modifier = Modifier
) {
    val timeState by timeTracker.timeState.collectAsState()

    if (!timeState.isTimeLimitReached) {
        Card(
            modifier = modifier
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    timeState.remainingMillis <= 60_000 -> Color.Red.copy(alpha = 0.9f)
                    timeState.remainingMillis <= 300_000 -> Color(0xFFFFA500).copy(alpha = 0.9f)
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = "Time remaining",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = timeTracker.getRemainingTimeFormatted(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TimeLimitReachedScreen(
    onParentOverride: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                "Time's Up!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Your daily SmilePile time is over.\nSee you tomorrow! 😊",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = onParentOverride
            ) {
                Text("Parent Override")
            }
        }
    }
}
```

### Testing Approach

```kotlin
@Test
fun `daily usage should accumulate across multiple sessions`() = runTest {
    // Given
    val tracker = DailyTimeTracker(context, preferences, notificationManager)

    // When - First session
    tracker.startSession()
    advanceTimeBy(5 * 60 * 1000) // 5 minutes
    tracker.pauseSession()

    // Second session
    tracker.startSession()
    advanceTimeBy(3 * 60 * 1000) // 3 minutes

    // Then
    val state = tracker.timeState.value
    assertEquals(8 * 60 * 1000, state.dailyUsageMillis + (System.currentTimeMillis() - state.sessionStartTime))
}
```

---

## STORY-6.5: Kids Mode Reward System

### Priority: P1 - Important

### Implementation Files

#### 1. Reward Engine

**File:** `/android/app/src/main/java/com/smilepile/kidsmode/rewards/RewardEngine.kt`
```kotlin
package com.smilepile.kidsmode.rewards

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import com.smilepile.data.repository.RewardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardEngine @Inject constructor(
    private val context: Context,
    private val repository: RewardRepository,
    private val preferences: SharedPreferences,
    private val animationManager: RewardAnimationManager
) {

    data class RewardProgress(
        val totalStickers: Int = 0,
        val currentStreak: Int = 0,
        val longestStreak: Int = 0,
        val photosViewed: Int = 0,
        val achievements: List<Achievement> = emptyList(),
        val lastRewardAt: Long = 0,
        val todayStickers: Int = 0,
        val viewCounterForNextSticker: Int = 0
    )

    data class Sticker(
        val id: String,
        val name: String,
        val imageRes: Int,
        val requiredIndex: Int,
        val isCollected: Boolean = false,
        val collectedAt: Long? = null,
        val rarity: StickerRarity = StickerRarity.COMMON
    )

    enum class StickerRarity {
        COMMON, RARE, EPIC, LEGENDARY
    }

    data class Achievement(
        val id: String,
        val name: String,
        val description: String,
        val iconRes: Int,
        val requiredValue: Int,
        val currentValue: Int = 0,
        val isUnlocked: Boolean = false,
        val unlockedAt: Long? = null
    )

    companion object {
        private const val PHOTOS_PER_STICKER = 10
        private const val MAX_DAILY_STICKERS = 20

        // Achievement IDs
        const val FIRST_STICKER = "first_sticker"
        const val STICKER_COLLECTOR = "sticker_collector_25"
        const val STICKER_MASTER = "sticker_master_50"
        const val STICKER_LEGEND = "sticker_legend_100"
        const val WEEK_STREAK = "week_streak"
        const val MONTH_STREAK = "month_streak"
        const val CATEGORY_EXPLORER = "category_explorer"
        const val SPEED_VIEWER = "speed_viewer"
    }

    private val _progress = MutableStateFlow(RewardProgress())
    val progress: StateFlow<RewardProgress> = _progress.asStateFlow()

    private val _showRewardAnimation = MutableStateFlow<Sticker?>(null)
    val showRewardAnimation: StateFlow<Sticker?> = _showRewardAnimation.asStateFlow()

    private var viewCounter = 0
    private var sessionCategories = mutableSetOf<String>()

    init {
        loadProgress()
        checkDailyReset()
        updateStreak()
    }

    fun onPhotoViewed(photoId: String, categoryId: String) {
        viewCounter++
        sessionCategories.add(categoryId)

        val currentProgress = _progress.value
        _progress.value = currentProgress.copy(
            photosViewed = currentProgress.photosViewed + 1,
            viewCounterForNextSticker = viewCounter
        )

        // Check for sticker reward
        if (viewCounter >= PHOTOS_PER_STICKER &&
            currentProgress.todayStickers < MAX_DAILY_STICKERS) {
            awardSticker()
            viewCounter = 0
        }

        // Check achievements
        checkAchievements()
    }

    private fun awardSticker() {
        val currentProgress = _progress.value
        val newTotal = currentProgress.totalStickers + 1
        val todayStickers = currentProgress.todayStickers + 1

        // Select sticker based on rarity system
        val sticker = selectStickerByRarity(newTotal)

        _progress.value = currentProgress.copy(
            totalStickers = newTotal,
            todayStickers = todayStickers,
            lastRewardAt = System.currentTimeMillis(),
            viewCounterForNextSticker = 0
        )

        // Trigger animation
        _showRewardAnimation.value = sticker
        animationManager.playRewardSound()
        animationManager.triggerHapticFeedback()

        // Check milestone achievements
        checkStickerMilestones(newTotal)

        saveProgress()
    }

    private fun selectStickerByRarity(stickerNumber: Int): Sticker {
        val rarityRoll = (0..100).random()
        val rarity = when {
            rarityRoll <= 60 -> StickerRarity.COMMON
            rarityRoll <= 85 -> StickerRarity.RARE
            rarityRoll <= 95 -> StickerRarity.EPIC
            else -> StickerRarity.LEGENDARY
        }

        return repository.getRandomStickerByRarity(rarity).copy(
            isCollected = true,
            collectedAt = System.currentTimeMillis()
        )
    }

    private fun checkStickerMilestones(totalStickers: Int) {
        when (totalStickers) {
            1 -> unlockAchievement(FIRST_STICKER)
            25 -> unlockAchievement(STICKER_COLLECTOR)
            50 -> unlockAchievement(STICKER_MASTER)
            100 -> unlockAchievement(STICKER_LEGEND)
        }
    }

    private fun checkAchievements() {
        val progress = _progress.value

        // Category explorer - view all categories in one session
        if (sessionCategories.size >= 5) {
            unlockAchievement(CATEGORY_EXPLORER)
        }

        // Streak achievements
        when (progress.currentStreak) {
            7 -> unlockAchievement(WEEK_STREAK)
            30 -> unlockAchievement(MONTH_STREAK)
        }

        // Speed viewer - 50 photos in 5 minutes
        checkSpeedViewerAchievement()
    }

    private fun unlockAchievement(achievementId: String) {
        val currentAchievements = _progress.value.achievements.toMutableList()
        val achievement = repository.getAchievement(achievementId)

        if (!achievement.isUnlocked) {
            val unlockedAchievement = achievement.copy(
                isUnlocked = true,
                unlockedAt = System.currentTimeMillis()
            )

            currentAchievements.removeAll { it.id == achievementId }
            currentAchievements.add(unlockedAchievement)

            _progress.value = _progress.value.copy(
                achievements = currentAchievements
            )

            animationManager.showAchievementUnlocked(unlockedAchievement)
            saveProgress()
        }
    }

    fun getStickerCollection(): List<Sticker> {
        return repository.getAllStickers().map { sticker ->
            sticker.copy(
                isCollected = _progress.value.totalStickers >= sticker.requiredIndex
            )
        }
    }

    fun getProgressToNextSticker(): Float {
        return (_progress.value.viewCounterForNextSticker.toFloat() / PHOTOS_PER_STICKER)
            .coerceIn(0f, 1f)
    }

    private fun updateStreak() {
        val lastUsageDate = preferences.getLong("last_reward_date", 0)
        val today = LocalDate.now().toEpochDay()
        val yesterday = today - 1

        val currentStreak = when (lastUsageDate) {
            yesterday -> _progress.value.currentStreak + 1
            today -> _progress.value.currentStreak
            else -> 1
        }

        val longestStreak = maxOf(currentStreak, _progress.value.longestStreak)

        _progress.value = _progress.value.copy(
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )

        preferences.edit()
            .putLong("last_reward_date", today)
            .apply()
    }

    private fun checkDailyReset() {
        val lastResetDate = preferences.getLong("last_reset_date", 0)
        val today = LocalDate.now().toEpochDay()

        if (lastResetDate != today) {
            _progress.value = _progress.value.copy(
                todayStickers = 0,
                viewCounterForNextSticker = 0
            )

            preferences.edit()
                .putLong("last_reset_date", today)
                .apply()
        }
    }

    private fun loadProgress() {
        _progress.value = repository.loadRewardProgress()
    }

    private fun saveProgress() {
        repository.saveRewardProgress(_progress.value)
    }
}
```

#### 2. Sticker Collection UI

**File:** `/android/app/src/main/java/com/smilepile/ui/screens/StickerCollectionScreen.kt`
```kotlin
package com.smilepile.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smilepile.kidsmode.rewards.RewardEngine

@Composable
fun StickerCollectionScreen(
    rewardEngine: RewardEngine,
    onStickerClick: (RewardEngine.Sticker) -> Unit,
    onBack: () -> Unit
) {
    val progress by rewardEngine.progress.collectAsState()
    val stickers = rewardEngine.getStickerCollection()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Sticker Collection",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress Header
            RewardProgressHeader(
                progress = progress,
                progressToNextSticker = rewardEngine.getProgressToNextSticker()
            )

            // Sticker Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stickers) { sticker ->
                    StickerItem(
                        sticker = sticker,
                        onClick = { onStickerClick(sticker) }
                    )
                }
            }
        }
    }
}

@Composable
fun RewardProgressHeader(
    progress: RewardEngine.RewardProgress,
    progressToNextSticker: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Stars,
                    value = progress.totalStickers.toString(),
                    label = "Total Stickers"
                )
                StatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = progress.currentStreak.toString(),
                    label = "Day Streak"
                )
                StatItem(
                    icon = Icons.Default.EmojiEvents,
                    value = progress.achievements.count { it.isUnlocked }.toString(),
                    label = "Achievements"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress to next sticker
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Next Sticker",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${(progressToNextSticker * 10).toInt()}/10 photos",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = progressToNextSticker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun StickerItem(
    sticker: RewardEngine.Sticker,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (sticker.isCollected) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        )
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (sticker.isCollected) {
                    when (sticker.rarity) {
                        RewardEngine.StickerRarity.LEGENDARY ->
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                            )
                        RewardEngine.StickerRarity.EPIC ->
                            Color(0xFF9C27B0)
                        RewardEngine.StickerRarity.RARE ->
                            Color(0xFF2196F3)
                        RewardEngine.StickerRarity.COMMON ->
                            MaterialTheme.colorScheme.primaryContainer
                    }
                } else {
                    Color.Gray.copy(alpha = 0.3f)
                }
            )
            .clickable(enabled = sticker.isCollected) { onClick() }
            .scale(animatedScale),
        contentAlignment = Alignment.Center
    ) {
        if (sticker.isCollected) {
            Image(
                painter = painterResource(id = sticker.imageRes),
                contentDescription = sticker.name,
                modifier = Modifier.size(60.dp)
            )
        } else {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(30.dp),
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun RewardAnimationOverlay(
    sticker: RewardEngine.Sticker,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(0.5f) }
    var rotation by remember { mutableStateOf(0f) }

    LaunchedEffect(key1 = sticker) {
        animate(
            initialValue = 0.5f,
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { value, _ ->
            scale = value
        }

        animate(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = tween(1000)
        ) { value, _ ->
            rotation = value
        }

        delay(2000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale)
                    .rotate(rotation)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when (sticker.rarity) {
                            RewardEngine.StickerRarity.LEGENDARY ->
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                )
                            RewardEngine.StickerRarity.EPIC ->
                                Color(0xFF9C27B0)
                            RewardEngine.StickerRarity.RARE ->
                                Color(0xFF2196F3)
                            RewardEngine.StickerRarity.COMMON ->
                                MaterialTheme.colorScheme.primary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = sticker.imageRes),
                    contentDescription = sticker.name,
                    modifier = Modifier.size(100.dp)
                )
            }

            Text(
                text = "You earned a ${sticker.name}!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when (sticker.rarity) {
                    RewardEngine.StickerRarity.LEGENDARY -> "⭐ LEGENDARY ⭐"
                    RewardEngine.StickerRarity.EPIC -> "✨ EPIC ✨"
                    RewardEngine.StickerRarity.RARE -> "💎 RARE 💎"
                    RewardEngine.StickerRarity.COMMON -> "Nice!"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = when (sticker.rarity) {
                    RewardEngine.StickerRarity.LEGENDARY -> Color(0xFFFFD700)
                    RewardEngine.StickerRarity.EPIC -> Color(0xFFE040FB)
                    RewardEngine.StickerRarity.RARE -> Color(0xFF40C4FF)
                    RewardEngine.StickerRarity.COMMON -> Color.White
                }
            )
        }
    }
}
```

### Testing Approach

```kotlin
@Test
fun `should award sticker after viewing 10 photos`() = runTest {
    // Given
    val rewardEngine = RewardEngine(context, repository, preferences, animationManager)

    // When
    repeat(10) { index ->
        rewardEngine.onPhotoViewed("photo_$index", "category_1")
    }

    // Then
    val progress = rewardEngine.progress.value
    assertEquals(1, progress.totalStickers)
    assertEquals(0, progress.viewCounterForNextSticker)
}

@Test
fun `should track streak across consecutive days`() = runTest {
    // Given
    val rewardEngine = RewardEngine(context, repository, preferences, animationManager)

    // When - simulate usage on consecutive days
    preferences.edit().putLong("last_reward_date", LocalDate.now().toEpochDay() - 1).apply()
    rewardEngine.updateStreak()

    // Then
    val progress = rewardEngine.progress.value
    assertEquals(2, progress.currentStreak)
}
```

---

## STORY-6.6: Performance Enhancements

### Priority: P2 - Enhancement

### Implementation Files

#### 1. Progressive Thumbnail Generator

**File:** `/android/app/src/main/java/com/smilepile/optimization/ProgressiveThumbnailGenerator.kt`
```kotlin
package com.smilepile.optimization

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressiveThumbnailGenerator @Inject constructor(
    private val context: Context
) {

    enum class ThumbnailSize(val pixels: Int, val quality: Int) {
        MICRO(50, 70),      // Grid view icons
        SMALL(150, 80),     // List views
        MEDIUM(400, 85),    // Preview mode
        LARGE(800, 90)      // Full screen preview
    }

    private val cacheDir = File(context.cacheDir, "thumbnails").apply {
        if (!exists()) mkdirs()
    }

    suspend fun generateAllThumbnails(
        sourceFile: File,
        photoId: String
    ): Map<ThumbnailSize, File> = withContext(Dispatchers.IO) {
        ThumbnailSize.values().map { size ->
            async {
                size to generateThumbnail(sourceFile, photoId, size)
            }
        }.awaitAll().toMap()
    }

    private suspend fun generateThumbnail(
        source: File,
        photoId: String,
        size: ThumbnailSize
    ): File = withContext(Dispatchers.IO) {
        val outputFile = getThumbnailFile(photoId, size)

        // Skip if already exists
        if (outputFile.exists() && outputFile.length() > 0) {
            return@withContext outputFile
        }

        // Generate thumbnail using WebP format
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ThumbnailUtils.createImageThumbnail(
                source,
                Size(size.pixels, size.pixels),
                null
            )
        } else {
            decodeSampledBitmap(source, size.pixels)
        }

        // Save as WebP with size-appropriate quality
        outputFile.outputStream().use { out ->
            bitmap.compress(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                },
                size.quality,
                out
            )
        }

        bitmap.recycle()
        outputFile
    }

    private fun decodeSampledBitmap(file: File, targetSize: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(file.path, options)

        // Calculate sample size
        options.inSampleSize = calculateInSampleSize(
            options.outWidth,
            options.outHeight,
            targetSize
        )

        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565 // Save memory

        return BitmapFactory.decodeFile(file.path, options)
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqSize: Int
    ): Int {
        var inSampleSize = 1

        if (height > reqSize || width > reqSize) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqSize &&
                   (halfWidth / inSampleSize) >= reqSize) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    fun getThumbnailFile(photoId: String, size: ThumbnailSize): File {
        return File(cacheDir, "${photoId}_${size.name}.webp")
    }

    fun getThumbnailForViewSize(photoId: String, viewWidth: Int): File? {
        val size = when {
            viewWidth <= 100 -> ThumbnailSize.MICRO
            viewWidth <= 300 -> ThumbnailSize.SMALL
            viewWidth <= 600 -> ThumbnailSize.MEDIUM
            else -> ThumbnailSize.LARGE
        }

        return getThumbnailFile(photoId, size).takeIf { it.exists() }
    }

    suspend fun cleanupUnusedThumbnails(activePhotoIds: Set<String>) =
        withContext(Dispatchers.IO) {
            cacheDir.listFiles()?.forEach { file ->
                val photoId = file.nameWithoutExtension.substringBeforeLast('_')
                if (photoId !in activePhotoIds) {
                    file.delete()
                }
            }
        }
}
```

#### 2. WebP Migration Service

**File:** `/android/app/src/main/java/com/smilepile/optimization/WebPMigrationService.kt`
```kotlin
package com.smilepile.optimization

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.smilepile.data.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebPMigrationService @Inject constructor(
    private val context: Context,
    private val photoRepository: PhotoRepository
) {

    data class MigrationProgress(
        val totalPhotos: Int = 0,
        val processedPhotos: Int = 0,
        val migratedPhotos: Int = 0,
        val savedBytes: Long = 0,
        val failedPhotos: Int = 0,
        val isRunning: Boolean = false,
        val currentPhoto: String = ""
    )

    data class MigrationResult(
        val migratedCount: Int,
        val savedBytes: Long,
        val averageSavingPercent: Int,
        val failures: List<String>
    )

    private val _migrationProgress = MutableStateFlow(MigrationProgress())
    val migrationProgress: StateFlow<MigrationProgress> = _migrationProgress.asStateFlow()

    suspend fun migrateAllPhotosToWebP(
        quality: Int = 85,
        deleteOriginals: Boolean = true
    ): MigrationResult = withContext(Dispatchers.IO) {

        val photos = photoRepository.getAllPhotos()
        val failures = mutableListOf<String>()
        var totalSavedBytes = 0L
        var migratedCount = 0

        _migrationProgress.value = MigrationProgress(
            totalPhotos = photos.size,
            isRunning = true
        )

        photos.forEachIndexed { index, photo ->
            // Skip if already WebP
            if (photo.path.endsWith(".webp", ignoreCase = true)) {
                _migrationProgress.value = _migrationProgress.value.copy(
                    processedPhotos = index + 1
                )
                return@forEachIndexed
            }

            _migrationProgress.value = _migrationProgress.value.copy(
                currentPhoto = photo.title,
                processedPhotos = index + 1
            )

            try {
                val result = convertPhotoToWebP(
                    photo,
                    quality,
                    deleteOriginals
                )

                if (result.success) {
                    migratedCount++
                    totalSavedBytes += result.savedBytes

                    // Update photo path in database
                    photoRepository.updatePhotoPath(photo.id, result.newPath)

                    _migrationProgress.value = _migrationProgress.value.copy(
                        migratedPhotos = migratedCount,
                        savedBytes = totalSavedBytes
                    )
                } else {
                    failures.add(photo.id)
                    _migrationProgress.value = _migrationProgress.value.copy(
                        failedPhotos = failures.size
                    )
                }
            } catch (e: Exception) {
                failures.add(photo.id)
                _migrationProgress.value = _migrationProgress.value.copy(
                    failedPhotos = failures.size
                )
            }
        }

        _migrationProgress.value = _migrationProgress.value.copy(
            isRunning = false
        )

        return@withContext MigrationResult(
            migratedCount = migratedCount,
            savedBytes = totalSavedBytes,
            averageSavingPercent = if (migratedCount > 0) {
                ((totalSavedBytes.toFloat() / (totalSavedBytes + photos.sumOf {
                    File(it.path).length()
                })) * 100).toInt()
            } else 0,
            failures = failures
        )
    }

    private data class ConversionResult(
        val success: Boolean,
        val newPath: String = "",
        val savedBytes: Long = 0,
        val error: String? = null
    )

    private fun convertPhotoToWebP(
        photo: com.smilepile.data.models.Photo,
        quality: Int,
        deleteOriginal: Boolean
    ): ConversionResult {
        val sourceFile = File(photo.path)

        if (!sourceFile.exists()) {
            return ConversionResult(
                success = false,
                error = "Source file not found"
            )
        }

        val webpFile = File(
            sourceFile.parent,
            "${sourceFile.nameWithoutExtension}.webp"
        )

        return try {
            // Decode original image
            val options = BitmapFactory.Options().apply {
                // Use RGB_565 to save memory if image has no transparency
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = BitmapFactory.decodeFile(sourceFile.path, options)
                ?: return ConversionResult(
                    success = false,
                    error = "Failed to decode image"
                )

            val originalSize = sourceFile.length()

            // Convert to WebP
            webpFile.outputStream().use { out ->
                val compressed = bitmap.compress(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        Bitmap.CompressFormat.WEBP
                    },
                    quality,
                    out
                )

                if (!compressed) {
                    bitmap.recycle()
                    return ConversionResult(
                        success = false,
                        error = "Failed to compress to WebP"
                    )
                }
            }

            bitmap.recycle()

            val newSize = webpFile.length()
            val savedBytes = originalSize - newSize

            // Only keep WebP if it's actually smaller
            if (savedBytes > 0) {
                if (deleteOriginal) {
                    sourceFile.delete()
                }

                ConversionResult(
                    success = true,
                    newPath = webpFile.path,
                    savedBytes = savedBytes
                )
            } else {
                // WebP is larger, keep original
                webpFile.delete()
                ConversionResult(
                    success = false,
                    error = "WebP version is larger than original"
                )
            }
        } catch (e: Exception) {
            ConversionResult(
                success = false,
                error = e.message
            )
        }
    }

    fun estimateStorageSavings(): Long {
        // Estimate 30-40% savings on average
        val photos = photoRepository.getAllPhotos()
        val totalSize = photos.sumOf { File(it.path).length() }
        return (totalSize * 0.35).toLong()
    }
}
```

#### 3. Optimized Image Loader with Coil

**File:** `/android/app/src/main/java/com/smilepile/optimization/OptimizedImageLoader.kt`
```kotlin
package com.smilepile.optimization

import android.content.Context
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OptimizedImageLoader @Inject constructor(
    private val context: Context,
    private val thumbnailGenerator: ProgressiveThumbnailGenerator
) {

    private val memoryCache = MemoryCache.Builder(context)
        .maxSizePercent(0.25) // 25% of available memory
        .build()

    private val diskCache = DiskCache.Builder()
        .directory(File(context.cacheDir, "image_cache"))
        .maxSizeBytes(500L * 1024 * 1024) // 500MB
        .build()

    @Composable
    fun OptimizedAsyncImage(
        photoId: String,
        photoPath: String,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        contentScale: ContentScale = ContentScale.Crop,
        viewWidthPx: Int? = null
    ) {
        val context = LocalContext.current

        // Select appropriate thumbnail based on view size
        val imagePath = viewWidthPx?.let {
            thumbnailGenerator.getThumbnailForViewSize(photoId, it)?.path
        } ?: photoPath

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imagePath)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCacheKey(photoId)
                .diskCacheKey(photoId)
                .size(Size.ORIGINAL)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }

    fun preloadImages(photoIds: List<String>, size: ProgressiveThumbnailGenerator.ThumbnailSize) {
        photoIds.forEach { photoId ->
            val thumbnailFile = thumbnailGenerator.getThumbnailFile(photoId, size)
            if (thumbnailFile.exists()) {
                val request = ImageRequest.Builder(context)
                    .data(thumbnailFile)
                    .memoryCacheKey(photoId)
                    .diskCacheKey(photoId)
                    .build()

                // Trigger preload
                context.imageLoader.enqueue(request)
            }
        }
    }

    fun clearMemoryCache() {
        memoryCache.clear()
    }

    fun trimDiskCache() {
        diskCache.clear()
    }
}
```

### Testing Approach

```kotlin
// Performance Benchmark Test
@Test
fun `thumbnail generation should complete within time limit`() = runTest {
    // Given
    val sourceFile = createTestImageFile(2000, 2000) // 2000x2000 test image
    val generator = ProgressiveThumbnailGenerator(context)

    // When
    val startTime = System.currentTimeMillis()
    val thumbnails = generator.generateAllThumbnails(sourceFile, "test-photo")
    val duration = System.currentTimeMillis() - startTime

    // Then
    assertTrue(duration < 1000) // Should complete in under 1 second
    assertEquals(4, thumbnails.size) // All sizes generated
}

@Test
fun `WebP conversion should reduce file size by at least 30%`() = runTest {
    // Given
    val migrationService = WebPMigrationService(context, photoRepository)
    val originalFile = createTestJpegFile()
    val originalSize = originalFile.length()

    // When
    val result = migrationService.convertPhotoToWebP(
        photo = testPhoto,
        quality = 85,
        deleteOriginal = false
    )

    // Then
    assertTrue(result.success)
    val savedPercent = (result.savedBytes.toFloat() / originalSize) * 100
    assertTrue(savedPercent >= 30)
}
```

---

## Database Migration Strategy

**File:** `/android/app/src/main/java/com/smilepile/data/database/migrations/Migration_X_to_Y.kt`

```kotlin
val MIGRATION_X_TO_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add deletion tracking table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS deletion_tracking (
                id TEXT PRIMARY KEY NOT NULL,
                itemId TEXT NOT NULL,
                itemType TEXT NOT NULL,
                deletedAt INTEGER NOT NULL,
                purgeAfter INTEGER NOT NULL,
                metadata TEXT,
                isPurged INTEGER NOT NULL DEFAULT 0
            )
        """)

        // Add indexes for performance
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_deletion_tracking_deletedAt
            ON deletion_tracking (deletedAt)
        """)

        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_deletion_tracking_itemType
            ON deletion_tracking (itemType)
        """)

        // Add reward tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS reward_progress (
                userId TEXT PRIMARY KEY NOT NULL,
                totalStickers INTEGER NOT NULL DEFAULT 0,
                currentStreak INTEGER NOT NULL DEFAULT 0,
                longestStreak INTEGER NOT NULL DEFAULT 0,
                photosViewed INTEGER NOT NULL DEFAULT 0,
                achievements TEXT NOT NULL,
                lastRewardAt INTEGER NOT NULL DEFAULT 0,
                todayStickers INTEGER NOT NULL DEFAULT 0
            )
        """)

        // Add daily usage tracking table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS daily_usage (
                date INTEGER PRIMARY KEY NOT NULL,
                usageMillis INTEGER NOT NULL DEFAULT 0,
                limitMillis INTEGER NOT NULL DEFAULT 1800000,
                warningsShown INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}
```

---

## Integration Testing Strategy

### Test Coverage Requirements
- Unit Tests: >90% coverage for business logic
- Integration Tests: Cover all user flows
- Performance Tests: Benchmark critical operations
- UI Tests: Compose UI testing for new screens

### Key Test Scenarios

1. **Deletion Tracking**
   - Delete photo → Verify tracking record created
   - Restore from backup → Verify deletions applied
   - 30-day purge → Verify automatic cleanup

2. **Encryption**
   - Device with PIN → Automatic encryption
   - No device lock → Password prompt
   - Wrong credentials → Restore fails gracefully

3. **Time Limits**
   - Accumulation across sessions
   - Warning notifications
   - Daily reset at midnight

4. **Rewards**
   - Sticker earning mechanics
   - Achievement unlocking
   - Progress persistence

5. **Performance**
   - Thumbnail generation speed
   - WebP conversion savings
   - Memory usage under limits

---

## Deployment Checklist

- [ ] Database migrations tested on upgrade path
- [ ] ProGuard rules updated for new classes
- [ ] Feature flags configured for gradual rollout
- [ ] Analytics events added for tracking
- [ ] Crashlytics monitoring configured
- [ ] A/B testing variants defined
- [ ] Documentation updated
- [ ] QA test plan executed
- [ ] Performance benchmarks passed
- [ ] Security review completed

---

## Risk Mitigation

1. **Data Loss Prevention**
   - Implement database backups before migrations
   - Add rollback mechanisms for critical operations
   - Extensive testing on various Android versions

2. **Performance Degradation**
   - Profile on low-end devices (2GB RAM)
   - Implement feature toggles for resource-intensive features
   - Monitor with Firebase Performance

3. **Security Vulnerabilities**
   - Regular security audits of encryption implementation
   - Penetration testing for backup encryption
   - Code obfuscation for sensitive logic

4. **User Experience Issues**
   - Beta testing with target audience (parents and kids)
   - Gradual feature rollout with monitoring
   - Quick iteration based on user feedback

---

## Success Metrics

- Deletion tracking: 100% accuracy, <50ms overhead
- Encryption adoption: 80% of users with device lock
- Time limits: 60% of parents configure limits
- Rewards engagement: 70% daily interaction rate
- Performance: 30% reduction in load times, 35% storage savings

---

## Notes

- All implementations follow SmilePile's existing MVVM architecture
- Use Hilt for dependency injection throughout
- Compose UI for all new screens
- Room database for persistence
- Coroutines for async operations
- Follow Material Design 3 guidelines