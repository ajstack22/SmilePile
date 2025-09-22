# Wave 3: Feature Completion - Photo Deletion & Import/Export (Weeks 5-6)

## CRITICAL: You are now the Atlas Orchestrator
You coordinate development through specialized agents. NEVER implement directly. Your role is to orchestrate Wave 3 of the SmilePile refactor, implementing core features based on StackMap and Manylla's lessons: simple solutions that work.

## Project Context
- **Current State**: 17 incomplete TODOs, no photo deletion, no backup system
- **Philosophy**: "Simple solutions that work > complex solutions that might work"
- **Key Constraint**: Local-only app, no cloud sync, no sharing
- **Goal**: Photo library management + local backup/restore

## Wave 3 Objectives
1. Photo deletion (remove from app library only, keep in MediaStore)
2. Local export (JSON backup of metadata + categories)
3. Local import (restore from JSON backup)
4. Clean up high-priority TODOs

## Atlas Orchestration Commands

### Phase 1: Initialize Wave
```bash
# Resume project context
python3 00_orchestrator_context.py resume
python3 00_orchestrator_context.py objective "Implement photo deletion and local import/export"

# Create feature stories
python3 02_create_story.py story "Implement photo library removal (not deletion)" --priority critical
python3 02_create_story.py story "Create local JSON export functionality" --priority high
python3 02_create_story.py story "Create local JSON import functionality" --priority high
python3 02_create_story.py story "Clean up critical TODOs" --priority medium

# Start workflow
python3 03_adversarial_workflow.py start WAVE3
```

### Phase 2: Parallel Agent Execution

#### Research Agents (Spawn 3 in parallel - Hour 1)
```
Agent 1: "Research current photo storage architecture using 01_research.py"
Tasks:
- Understand PhotoRepository implementation
- Map relationship between app database and MediaStore
- Document how photos are currently referenced
- Identify deletion impact points

Agent 2: "Analyze TODO comments and prioritize using 01_research.py"
Tasks:
- Find all 17 TODOs in codebase
- Categorize by criticality
- Identify which can be closed with "won't fix"
- Create priority list for completion

Agent 3: "Research Android backup/restore patterns using 01_research.py"
Tasks:
- Study Android backup best practices
- Research JSON serialization for Kotlin data classes
- Find optimal file locations for backups
- Document permission requirements
```

#### Feature Implementation Agents (Spawn 4 in parallel - Hours 2-5)

```
Agent 1: "Implement photo library removal"
Task: Remove photos from app without deleting from device
Implementation:
- Add removeFromLibrary() to PhotoRepository
- Only delete from app database, not MediaStore
- Maintain referential integrity with categories
- No undo needed (per requirements)

Code location: PhotoRepositoryImpl.kt
Key pattern:
suspend fun removeFromLibrary(photoId: Long) {
    // Remove from app database only
    photoDao.deleteById(photoId)
    // Photo remains in MediaStore - user still has it
}

Agent 2: "Create export functionality"
Task: Export app data to JSON backup
Features:
- Export all categories with metadata
- Export photo references (not actual photos)
- Include app settings and preferences
- Save to Downloads or app-specific directory

Structure:
{
  "version": 1,
  "exportDate": "2024-01-01T12:00:00Z",
  "categories": [...],
  "photos": [
    {
      "id": 1,
      "mediaStoreUri": "content://media/external/images/media/12345",
      "categoryId": 2,
      "addedDate": "2024-01-01T10:00:00Z",
      "metadata": {...}
    }
  ],
  "settings": {...}
}

Agent 3: "Create import functionality"
Task: Restore app data from JSON backup
Features:
- Version checking for compatibility
- Merge or replace options
- Validate MediaStore URIs still exist
- Handle missing photos gracefully
- Progress indication for large imports

Key considerations:
- Photos that no longer exist in MediaStore are skipped
- Categories always imported
- Duplicate detection based on MediaStore URI

Agent 4: "Fix critical TODOs"
Task: Address top 5 most critical TODOs
Priority fixes:
1. Category initialization on first launch
2. Error handling in photo import flow
3. Null safety in PhotoGalleryViewModel
4. Add progress indicators where marked TODO
5. Complete unfinished validation logic
```

#### Integration Agents (Spawn 2 in parallel - Hour 6)

```
Agent 1: "Add UI for deletion and backup features"
Tasks:
- Add "Remove from Library" option in Parent Mode
- Create Export/Import section in Settings
- Add confirmation dialogs
- Implement progress indicators
- Ensure Kids Mode can't access these features

Agent 2: "Create comprehensive feature tests"
Tasks:
- Test photo removal workflow
- Test export creates valid JSON
- Test import handles all edge cases
- Test Kids Mode restrictions maintained
- Verify MediaStore photos untouched
```

### Phase 3: Validation & Evidence Collection

```
Agent: "Validate all features and collect evidence"
Tasks:
- Execute photo removal and verify MediaStore intact
- Perform full export and validate JSON structure
- Test import with various scenarios (empty, partial, full)
- Count remaining TODOs (must be < 20)
- Generate before/after screenshots
```

## Implementation Details

### Photo Library Removal (NOT Deletion)
```kotlin
// PhotoRepositoryImpl.kt
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao,
    private val context: Context
) : PhotoRepository {

    override suspend fun removeFromLibrary(photoId: Long) = withContext(ioDispatcher) {
        try {
            // Only remove from app's database
            photoDao.deleteById(photoId)

            // Log for debugging (will be removed per debug limit)
            Log.i("PhotoRepository", "Removed photo $photoId from library")

            // Photo remains in: MediaStore, Google Photos, Gallery app, etc.
            // User hasn't lost anything
        } catch (e: Exception) {
            // Handle gracefully
            throw PhotoRemovalException("Failed to remove photo: ${e.message}")
        }
    }

    // NO deleteFromDevice() method - we never touch MediaStore deletions
}
```

### Export Implementation
```kotlin
// BackupManager.kt
@Singleton
class BackupManager @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val categoryRepository: CategoryRepository,
    private val gson: Gson
) {
    fun exportToJson(): String {
        val backup = BackupData(
            version = BACKUP_VERSION,
            exportDate = System.currentTimeMillis(),
            categories = categoryRepository.getAllCategories(),
            photos = photoRepository.getAllPhotos().map { photo ->
                PhotoBackup(
                    mediaStoreUri = photo.path,
                    categoryId = photo.categoryId,
                    metadata = PhotoMetadata(
                        addedDate = photo.createdAt,
                        isFavorite = photo.isFavorite
                    )
                )
            },
            settings = getAppSettings()
        )

        return gson.toJson(backup)
    }

    suspend fun saveBackupToFile(): File {
        val json = exportToJson()
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "smilepile_backup_${System.currentTimeMillis()}.json"
        )
        file.writeText(json)
        return file
    }
}
```

### Import Implementation
```kotlin
// BackupManager.kt
suspend fun importFromJson(json: String, strategy: ImportStrategy) {
    val backup = gson.fromJson(json, BackupData::class.java)

    // Version compatibility check
    if (backup.version > BACKUP_VERSION) {
        throw IncompatibleBackupException("Backup version too new")
    }

    // Import categories first (they're independent)
    when (strategy) {
        ImportStrategy.REPLACE -> {
            categoryRepository.deleteAll()
            categoryRepository.insertAll(backup.categories)
        }
        ImportStrategy.MERGE -> {
            backup.categories.forEach { category ->
                categoryRepository.insertOrUpdate(category)
            }
        }
    }

    // Import photos (check if they still exist)
    backup.photos.forEach { photoBackup ->
        if (mediaStoreExists(photoBackup.mediaStoreUri)) {
            photoRepository.addPhoto(
                Photo(
                    path = photoBackup.mediaStoreUri,
                    categoryId = photoBackup.categoryId,
                    createdAt = photoBackup.metadata.addedDate,
                    isFavorite = photoBackup.metadata.isFavorite
                )
            )
        } else {
            Log.w("BackupManager", "Skipping missing photo: ${photoBackup.mediaStoreUri}")
        }
    }

    // Restore settings
    restoreAppSettings(backup.settings)
}

private fun mediaStoreExists(uri: String): Boolean {
    return try {
        context.contentResolver.openInputStream(Uri.parse(uri))?.close()
        true
    } catch (e: Exception) {
        false
    }
}
```

### TODO Cleanup Strategy
```kotlin
// Priority 1: Fix critical safety issues
// Before:
// TODO: Add null check here
photoList = repository.getPhotos()  // Could crash

// After:
photoList = repository.getPhotos() ?: emptyList()

// Priority 2: Complete partial implementations
// Before:
// TODO: Implement category initialization
// (empty)

// After:
private fun initializeDefaultCategories() {
    if (categoryRepository.getCount() == 0) {
        categoryRepository.insertAll(defaultCategories)
    }
}

// Priority 3: Close "won't fix" TODOs
// Before:
// TODO: Add voice search

// After:
// REMOVED - Feature not needed for MVP (per requirements)
```

## Success Criteria & Evidence

### Required Evidence
```bash
# Test photo removal
python3 03_adversarial_workflow.py execute evidence --type feature --name "photo-removal"

# Test export/import
python3 03_adversarial_workflow.py execute evidence --type feature --name "backup-restore"

# TODO count validation
./scripts/smilepile_deploy.sh  # Must show < 20 TODOs
```

Evidence must include:
1. ✅ Photo removed from app but visible in Gallery app
2. ✅ Export creates valid JSON file
3. ✅ Import successfully restores data
4. ✅ TODO count reduced to < 20
5. ✅ Kids Mode cannot access deletion/backup features

## Key Lessons Applied

### From StackMap:
✅ **No complex undo** - Simple removal is enough
✅ **Test real flows** - Not edge cases
✅ **Feature flags** - Can disable if issues arise

### From Manylla:
✅ **Local-first** - No cloud complexity
✅ **Progressive enhancement** - Basic backup now, enhance later
✅ **Simple JSON** - No complex serialization

## Common Pitfalls to Avoid

❌ **Don't delete from MediaStore**
```kotlin
// NEVER DO THIS
contentResolver.delete(mediaStoreUri, null, null)  // NO!

// ONLY DO THIS
photoDao.deleteById(photoId)  // Remove from app only
```

❌ **Don't over-engineer backup**
```kotlin
// BAD: Complex incremental backup system
// GOOD: Simple full backup to JSON
```

❌ **Don't implement undo**
```kotlin
// NOT NEEDED: Complex undo/redo system
// SUFFICIENT: Simple removal (photos still on device)
```

## Parallel Execution Timeline

```
Hour 1: Research (3 agents)
Hours 2-5: Implementation (4 agents)
Hour 6: Integration (2 agents)
Hour 7: Validation
Total: 7 hours (vs 28 hours sequential)
```

## Final Checklist

Before marking Wave 3 complete:
- [ ] Photo removal working (app only, not MediaStore)
- [ ] Export creates valid JSON backup
- [ ] Import restores categories and photos
- [ ] Import handles missing photos gracefully
- [ ] TODO count < 20 (verified by deploy script)
- [ ] Kids Mode can't access these features
- [ ] Parent Mode has all controls
- [ ] No data loss during operations
- [ ] Tests pass for all features

## Next Wave Preview
Wave 4 will focus on security hardening with Android Keystore, encrypted metadata, and final polish including biometric authentication options.

---

**REMEMBER**: Keep it simple. No undo needed. No cloud sync. No sharing. Just local backup and library management. The photos are never truly deleted from the device.

**START COMMAND**: Copy this entire file and paste to your LLM to begin Wave 3 execution.