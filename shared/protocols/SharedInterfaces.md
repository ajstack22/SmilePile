# Shared Interface Definitions for SmilePile Cross-Platform Features

## Core Protocols and Interfaces

### 1. Deletion Tracking Protocol

#### Kotlin/Android Interface
```kotlin
package com.smilepile.shared.protocols

import kotlinx.serialization.Serializable

/**
 * Core interface for deletion tracking across platforms
 */
interface IDeletionManager {
    suspend fun trackDeletion(item: DeletableItem): DeletionRecord
    suspend fun syncDeletions(records: List<DeletionRecord>): SyncResult
    suspend fun purgeExpired(): PurgeResult
    suspend fun restoreDeletion(recordId: String): RestoreResult
}

/**
 * Base interface for items that can be deleted
 */
interface DeletableItem {
    val id: String
    val type: ItemType
    val metadata: Map<String, Any>
}

/**
 * Serializable deletion record for cross-platform compatibility
 */
@Serializable
data class DeletionRecord(
    val recordId: String,
    val itemId: String,
    val itemType: ItemType,
    val deletedAt: Long,
    val purgeAfter: Long,
    val deletedBy: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val isPurged: Boolean = false,
    val cascadedDeletions: List<String> = emptyList()
)

@Serializable
enum class ItemType {
    PHOTO,
    CATEGORY,
    PHOTO_BATCH,
    CATEGORY_CASCADE;

    fun toIOS(): String = this.name
}

@Serializable
data class SyncResult(
    val synced: Int,
    val conflicts: Int,
    val failed: Int,
    val resolution: ConflictResolution? = null
)

@Serializable
data class PurgeResult(
    val purgedCount: Int,
    val freedSpace: Long,
    val errors: List<String> = emptyList()
)
```

#### Swift/iOS Protocol
```swift
import Foundation

/// Core protocol for deletion tracking across platforms
protocol DeletionManagerProtocol {
    func trackDeletion(item: any DeletableItem) async throws -> DeletionRecord
    func syncDeletions(records: [DeletionRecord]) async throws -> SyncResult
    func purgeExpired() async throws -> PurgeResult
    func restoreDeletion(recordId: String) async throws -> RestoreResult
}

/// Base protocol for items that can be deleted
protocol DeletableItem {
    var id: String { get }
    var type: ItemType { get }
    var metadata: [String: Any] { get }
}

/// Codable deletion record for cross-platform compatibility
struct DeletionRecord: Codable {
    let recordId: String
    let itemId: String
    let itemType: ItemType
    let deletedAt: Int64
    let purgeAfter: Int64
    let deletedBy: String?
    let metadata: [String: String]
    let isPurged: Bool
    let cascadedDeletions: [String]

    enum CodingKeys: String, CodingKey {
        case recordId, itemId, itemType, deletedAt, purgeAfter
        case deletedBy, metadata, isPurged, cascadedDeletions
    }
}

enum ItemType: String, Codable {
    case photo = "PHOTO"
    case category = "CATEGORY"
    case photoBatch = "PHOTO_BATCH"
    case categoryCascade = "CATEGORY_CASCADE"

    var androidValue: String {
        return self.rawValue
    }
}

struct SyncResult: Codable {
    let synced: Int
    let conflicts: Int
    let failed: Int
    let resolution: ConflictResolution?
}
```

### 2. Reward System Protocol

#### Kotlin/Android Interface
```kotlin
package com.smilepile.shared.protocols

import kotlinx.serialization.Serializable

/**
 * Core reward system interface
 */
interface IRewardEngine {
    suspend fun checkRewards(activity: UserActivity): List<Reward>
    suspend fun awardReward(reward: Reward): AwardResult
    suspend fun getProgress(): RewardProgress
    suspend fun syncProgress(progress: RewardProgress): SyncResult
}

/**
 * Base reward definition
 */
interface IReward {
    val id: String
    val type: RewardType
    val name: String
    val description: String
    val points: Int
    val iconResource: String
    val requirements: List<Requirement>
}

@Serializable
data class Reward(
    override val id: String,
    override val type: RewardType,
    override val name: String,
    override val description: String,
    override val points: Int,
    override val iconResource: String,
    override val requirements: List<Requirement> = emptyList(),
    val rarity: RewardRarity = RewardRarity.COMMON,
    val category: RewardCategory = RewardCategory.GENERAL
) : IReward

@Serializable
enum class RewardType {
    STICKER,
    BADGE,
    ACHIEVEMENT,
    MILESTONE,
    STREAK,
    SPECIAL
}

@Serializable
enum class RewardRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
}

@Serializable
data class RewardProgress(
    val userId: String,
    val totalPoints: Int = 0,
    val totalStickers: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val unlockedRewards: List<UnlockedReward> = emptyList(),
    val statistics: UserStatistics = UserStatistics()
)

@Serializable
data class UnlockedReward(
    val rewardId: String,
    val unlockedAt: Long,
    val platform: String
)
```

#### Swift/iOS Protocol
```swift
import Foundation

/// Core reward system protocol
protocol RewardEngineProtocol {
    func checkRewards(activity: UserActivity) async throws -> [Reward]
    func awardReward(_ reward: Reward) async throws -> AwardResult
    func getProgress() async throws -> RewardProgress
    func syncProgress(_ progress: RewardProgress) async throws -> SyncResult
}

/// Base reward protocol
protocol RewardProtocol {
    var id: String { get }
    var type: RewardType { get }
    var name: String { get }
    var description: String { get }
    var points: Int { get }
    var iconResource: String { get }
    var requirements: [Requirement] { get }
}

struct Reward: Codable, RewardProtocol {
    let id: String
    let type: RewardType
    let name: String
    let description: String
    let points: Int
    let iconResource: String
    let requirements: [Requirement]
    let rarity: RewardRarity
    let category: RewardCategory
}

enum RewardType: String, Codable {
    case sticker = "STICKER"
    case badge = "BADGE"
    case achievement = "ACHIEVEMENT"
    case milestone = "MILESTONE"
    case streak = "STREAK"
    case special = "SPECIAL"
}

struct RewardProgress: Codable {
    let userId: String
    var totalPoints: Int = 0
    var totalStickers: Int = 0
    var currentStreak: Int = 0
    var longestStreak: Int = 0
    var unlockedRewards: [UnlockedReward] = []
    var statistics: UserStatistics = UserStatistics()
}
```

### 3. Time Tracking Protocol

#### Kotlin/Android Interface
```kotlin
package com.smilepile.shared.protocols

import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDate

/**
 * Time tracking and limit management interface
 */
interface ITimeTracker {
    suspend fun startSession(): SessionInfo
    suspend fun pauseSession(): Duration
    suspend fun getUsageToday(): Duration
    suspend fun setDailyLimit(minutes: Int): ConfigResult
    suspend fun checkTimeLimit(): LimitStatus
    suspend fun overrideLimit(pin: String): OverrideResult
}

@Serializable
data class TimeLimitConfig(
    val userId: String,
    val dailyLimitMinutes: Int,
    val warningThresholds: List<Int> = listOf(1, 5, 15),
    val enforcementMode: EnforcementMode = EnforcementMode.STRICT,
    val schedule: WeeklySchedule? = null,
    val graceExtensionMinutes: Int = 0
)

@Serializable
enum class EnforcementMode {
    STRICT,      // Immediate lock
    WARNING,     // Show warnings only
    FLEXIBLE     // Allow grace period
}

@Serializable
data class DailyUsageRecord(
    val date: String, // ISO date string for cross-platform
    val totalSeconds: Int,
    val limitSeconds: Int,
    val sessions: List<UsageSession>,
    val warnings: List<WarningEvent> = emptyList(),
    val overrides: List<OverrideEvent> = emptyList(),
    val limitReached: Boolean = false
)

@Serializable
data class UsageSession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val durationSeconds: Int = 0,
    val wasInterrupted: Boolean = false
)

@Serializable
data class LimitStatus(
    val isWithinLimit: Boolean,
    val remainingSeconds: Int,
    val nextWarningAt: Int? = null,
    val canOverride: Boolean
)
```

#### Swift/iOS Protocol
```swift
import Foundation

/// Time tracking and limit management protocol
protocol TimeTrackerProtocol {
    func startSession() async throws -> SessionInfo
    func pauseSession() async throws -> TimeInterval
    func getUsageToday() async throws -> TimeInterval
    func setDailyLimit(minutes: Int) async throws -> ConfigResult
    func checkTimeLimit() async throws -> LimitStatus
    func overrideLimit(pin: String) async throws -> OverrideResult
}

struct TimeLimitConfig: Codable {
    let userId: String
    let dailyLimitMinutes: Int
    let warningThresholds: [Int]
    let enforcementMode: EnforcementMode
    let schedule: WeeklySchedule?
    let graceExtensionMinutes: Int
}

enum EnforcementMode: String, Codable {
    case strict = "STRICT"
    case warning = "WARNING"
    case flexible = "FLEXIBLE"
}

struct DailyUsageRecord: Codable {
    let date: String // ISO date string
    let totalSeconds: Int
    let limitSeconds: Int
    let sessions: [UsageSession]
    let warnings: [WarningEvent]
    let overrides: [OverrideEvent]
    let limitReached: Bool
}

struct UsageSession: Codable {
    let sessionId: String
    let startTime: Int64
    let endTime: Int64?
    let durationSeconds: Int
    let wasInterrupted: Bool
}
```

### 4. Backup Protocol

#### Kotlin/Android Interface
```kotlin
package com.smilepile.shared.protocols

import kotlinx.serialization.Serializable

/**
 * Unified backup management interface
 */
interface IBackupManager {
    suspend fun createBackup(options: BackupOptions): BackupResult
    suspend fun restoreBackup(backup: UnifiedBackup, options: RestoreOptions): RestoreResult
    suspend fun validateBackup(backup: UnifiedBackup): ValidationResult
    suspend fun migrateBackup(backup: Any, targetVersion: String): UnifiedBackup
}

@Serializable
data class UnifiedBackup(
    val version: String,
    val platform: String,
    val createdAt: Long,
    val appVersion: String,
    val schemaVersion: Int,
    val metadata: BackupMetadata,
    val data: BackupData,
    val checksums: Map<String, String>
)

@Serializable
data class BackupData(
    val categories: List<BackupCategory>,
    val photos: List<BackupPhoto>,
    val settings: BackupSettings,
    val deletionRecords: List<DeletionRecord> = emptyList(),
    val rewardProgress: RewardProgress? = null,
    val timeTracking: TimeTrackingData? = null,
    val achievements: List<UnlockedReward> = emptyList()
)

@Serializable
data class BackupOptions(
    val includePhotos: Boolean = true,
    val includeSettings: Boolean = true,
    val includeDeletions: Boolean = true,
    val includeRewards: Boolean = true,
    val compressionLevel: Int = 6,
    val encrypt: Boolean = false,
    val encryptionKey: String? = null
)

@Serializable
data class RestoreOptions(
    val overwriteExisting: Boolean = false,
    val mergeData: Boolean = false,
    val restoreDeleted: Boolean = false,
    val skipValidation: Boolean = false
)
```

#### Swift/iOS Protocol
```swift
import Foundation

/// Unified backup management protocol
protocol BackupManagerProtocol {
    func createBackup(options: BackupOptions) async throws -> BackupResult
    func restoreBackup(_ backup: UnifiedBackup, options: RestoreOptions) async throws -> RestoreResult
    func validateBackup(_ backup: UnifiedBackup) async throws -> ValidationResult
    func migrateBackup(_ backup: Any, targetVersion: String) async throws -> UnifiedBackup
}

struct UnifiedBackup: Codable {
    let version: String
    let platform: String
    let createdAt: Int64
    let appVersion: String
    let schemaVersion: Int
    let metadata: BackupMetadata
    let data: BackupData
    let checksums: [String: String]
}

struct BackupData: Codable {
    let categories: [BackupCategory]
    let photos: [BackupPhoto]
    let settings: BackupSettings
    let deletionRecords: [DeletionRecord]
    let rewardProgress: RewardProgress?
    let timeTracking: TimeTrackingData?
    let achievements: [UnlockedReward]
}
```

## Shared Constants and Enums

### Platform-Agnostic Constants
```kotlin
// Kotlin
object SharedConstants {
    const val BACKUP_VERSION = "2.0"
    const val SCHEMA_VERSION = 2
    const val DELETION_RETENTION_DAYS = 30
    const val DEFAULT_TIME_LIMIT_MINUTES = 30
    const val PHOTOS_PER_STICKER = 10
    const val ENCRYPTION_ITERATIONS = 10000
}
```

```swift
// Swift
struct SharedConstants {
    static let backupVersion = "2.0"
    static let schemaVersion = 2
    static let deletionRetentionDays = 30
    static let defaultTimeLimitMinutes = 30
    static let photosPerSticker = 10
    static let encryptionIterations = 10000
}
```

## JSON Schema Definitions

### Unified JSON Schemas
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "definitions": {
    "DeletionRecord": {
      "type": "object",
      "required": ["recordId", "itemId", "itemType", "deletedAt", "purgeAfter"],
      "properties": {
        "recordId": { "type": "string", "format": "uuid" },
        "itemId": { "type": "string" },
        "itemType": { "enum": ["PHOTO", "CATEGORY", "PHOTO_BATCH", "CATEGORY_CASCADE"] },
        "deletedAt": { "type": "integer", "minimum": 0 },
        "purgeAfter": { "type": "integer", "minimum": 0 },
        "deletedBy": { "type": ["string", "null"] },
        "metadata": { "type": "object" },
        "isPurged": { "type": "boolean" },
        "cascadedDeletions": { "type": "array", "items": { "type": "string" } }
      }
    },
    "RewardProgress": {
      "type": "object",
      "required": ["userId", "totalPoints", "totalStickers"],
      "properties": {
        "userId": { "type": "string", "format": "uuid" },
        "totalPoints": { "type": "integer", "minimum": 0 },
        "totalStickers": { "type": "integer", "minimum": 0 },
        "currentStreak": { "type": "integer", "minimum": 0 },
        "longestStreak": { "type": "integer", "minimum": 0 },
        "unlockedRewards": {
          "type": "array",
          "items": { "$ref": "#/definitions/UnlockedReward" }
        }
      }
    },
    "DailyUsageRecord": {
      "type": "object",
      "required": ["date", "totalSeconds", "limitSeconds", "sessions"],
      "properties": {
        "date": { "type": "string", "format": "date" },
        "totalSeconds": { "type": "integer", "minimum": 0 },
        "limitSeconds": { "type": "integer", "minimum": 0 },
        "sessions": {
          "type": "array",
          "items": { "$ref": "#/definitions/UsageSession" }
        },
        "limitReached": { "type": "boolean" }
      }
    }
  }
}
```

## Implementation Guidelines

### 1. Platform-Specific Adapters
Each platform should implement adapters that convert between platform-specific types and shared protocol types.

### 2. Serialization Strategy
- Use Kotlinx Serialization for Kotlin/Android
- Use Codable for Swift/iOS
- Ensure JSON output is identical for cross-platform compatibility

### 3. Version Negotiation
Both platforms must implement version detection and negotiation to ensure compatibility.

### 4. Error Handling
Define common error codes and messages for consistent cross-platform error handling.

### 5. Testing Requirements
- Unit tests for each protocol implementation
- Integration tests for cross-platform data exchange
- Compatibility tests for version migration