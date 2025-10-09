# iOS Backup/Restore Manual Test Plan

**Version**: 1.0
**Date**: 2025-10-08
**Feature**: iOS Backup/Restore UI Integration
**Target Build**: v25.10.08.001+
**Tester**: [Name]
**Test Date**: [Date]

---

## Table of Contents

1. [Pre-Test Setup](#1-pre-test-setup)
2. [Test Scenarios Overview](#2-test-scenarios-overview)
3. [P0 - Critical Path Tests](#3-p0---critical-path-tests-must-pass)
4. [P1 - Important Tests](#4-p1---important-tests-should-pass)
5. [P2 - Edge Case Tests](#5-p2---edge-case-tests-nice-to-verify)
6. [Acceptance Criteria Validation](#6-acceptance-criteria-validation)
7. [Bug Report Template](#7-bug-report-template)
8. [Sign-off Checklist](#8-sign-off-checklist)

---

## 1. Pre-Test Setup

### 1.1 Device Requirements

**iOS Simulator** (Minimum):
- iOS 16.0 or higher
- Device: iPhone 16 Pro (recommended)
- Biometric: Enabled (Face ID in simulator)
- Storage: At least 5GB free space

**Physical Device** (Recommended for P1/P2 tests):
- iPhone with iOS 16.0+
- Face ID or Touch ID enabled
- At least 5GB free storage
- iCloud Drive configured (for ShareSheet testing)

### 1.2 App Installation

1. Build SmilePile iOS app:
   ```bash
   cd /Users/adamstack/SmilePile/ios
   xcodebuild -scheme SmilePile -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 16 Pro'
   ```

2. Install on simulator or device
3. Complete onboarding flow
4. Grant photo library permissions when prompted

### 1.3 Test Data Preparation

#### Scenario 1: Small Library (10 Photos)

1. Prepare 10 photos on your computer (any format: JPG, PNG, HEIC)
2. Import to iOS Photos app:
   - Simulator: Drag photos to simulator Photos app
   - Device: AirDrop or sync via Finder
3. Open SmilePile
4. Import the 10 photos using the import flow
5. Assign photos to 2-3 categories:
   - 5 photos to "Family"
   - 3 photos to "Friends"
   - 2 photos to "Vacation"

**Expected Library State**: 10 photos, 3 categories

---

#### Scenario 2: Medium Library (100 Photos)

1. Prepare 100 photos (can use duplicates with renamed files)
2. Import all 100 photos to SmilePile
3. Assign photos to 5-7 categories:
   - 30 photos to "Family"
   - 25 photos to "Friends"
   - 20 photos to "Vacation"
   - 15 photos to "Work"
   - 10 photos to "Hobbies"

**Expected Library State**: 100 photos, 5 categories

---

#### Scenario 3: Large Library (500 Photos)

**Note**: Only required for P1-3 test

1. Use automated script or batch import 500 photos
2. Distribute across 10+ categories
3. This tests performance and progress calculation

**Expected Library State**: 500 photos, 10+ categories

---

### 1.4 Android Backup for Cross-Platform Testing

**Obtaining Android Backup**:

1. If you have Android device:
   - Install SmilePile Android app
   - Import 10 photos with 2-3 categories
   - Export backup via Settings > Export Data
   - Transfer `smilepile_backup_YYYYMMDD_HHMMSS.zip` to iOS device

2. If no Android device available:
   - Request pre-created Android backup from development team
   - File should be named `smilepile_backup_android_test.zip`
   - Transfer to iOS device via AirDrop or Files app

**Expected Backup Contents**:
- 10 photos
- 2-3 categories
- Valid `metadata.json`

---

### 1.5 Pre-Test Checklist

- [ ] iOS device/simulator ready with iOS 16.0+
- [ ] SmilePile app installed and onboarded
- [ ] Photo library permissions granted
- [ ] Test data prepared (10 photos, 100 photos)
- [ ] Android backup file available for P1-6
- [ ] At least 5GB free storage available
- [ ] Biometric authentication enabled and working
- [ ] Note-taking app ready for documenting results

---

## 2. Test Scenarios Overview

### Priority Definitions

**P0 - Critical Path (MUST PASS)**:
- Core functionality that MUST work for production release
- Failure blocks release
- 100% pass rate required

**P1 - Important (SHOULD PASS)**:
- Important scenarios that should work
- Failure requires bug fix but may not block release
- 80% pass rate required

**P2 - Edge Cases (NICE TO VERIFY)**:
- Unusual scenarios, error handling
- Failure is acceptable if handled gracefully
- No minimum pass rate

---

### Test Execution Order

1. Complete all P0 tests first
2. If P0 tests pass, proceed to P1 tests
3. If time allows, execute P2 tests
4. Document all failures immediately
5. Take screenshots of any unexpected behavior

---

### Estimated Time

- **P0 Tests**: 45-60 minutes
- **P1 Tests**: 60-90 minutes
- **P2 Tests**: 30-45 minutes
- **Total**: 2.5 - 3 hours

---

## 3. P0 - Critical Path Tests (MUST PASS)

---

### P0-1: Export with 10 Photos

**Test ID**: P0-1
**Description**: Verify basic export functionality with small library
**Pre-conditions**:
- 10 photos in library
- 3 categories assigned
**Priority**: P0 - CRITICAL

**Steps to Execute**:

1. Open SmilePile app
2. Navigate to Settings screen
3. Scroll to "Data Management" section
4. Tap "Export Data" button
5. Complete biometric authentication when prompted (Face ID/Touch ID)
6. Observe export progress dialog
7. When ShareSheet appears, tap "Save to Files"
8. Select "On My iPhone" > "Downloads" folder
9. Tap "Save"
10. Dismiss any confirmation messages
11. Open Files app > Downloads folder
12. Verify exported file exists: `smilepile_backup_YYYYMMDD_HHMMSS.zip`

**Expected Results**:
- [ ] Biometric authentication prompt appears
- [ ] Export progress dialog appears within 500ms
- [ ] Progress dialog shows:
  - Title: "Exporting Data"
  - Operation text (e.g., "Collecting categories...", "Copying photos...")
  - Progress percentage updates smoothly
  - Counter like "Progress: 5/13" or similar
- [ ] Progress updates at least every 5 photos
- [ ] Export completes in under 30 seconds
- [ ] ShareSheet appears after completion
- [ ] File saved successfully to Downloads folder
- [ ] File size is reasonable (approximately 2-5MB for 10 photos)
- [ ] Filename follows format: `smilepile_backup_YYYYMMDD_HHMMSS.zip`

**Actual Results**: [Tester to fill in]

**Pass/Fail**: [ ] PASS [ ] FAIL

**Screenshots**: [Attach if needed]

**Notes**: [Any observations]

---

### P0-2: Import Valid Backup

**Test ID**: P0-2
**Description**: Verify basic import functionality
**Pre-conditions**:
- Successfully completed P0-1 (export created)
- Exported backup file available in Downloads folder
**Priority**: P0 - CRITICAL

**Steps to Execute**:

1. Open SmilePile app
2. Navigate to Settings screen
3. Tap "Import Data" button
4. Complete biometric authentication when prompted
5. Document picker appears filtered to .zip files
6. Navigate to "On My iPhone" > "Downloads"
7. Select the backup file created in P0-1
8. Observe "Validating backup..." message
9. When confirmation dialog appears, note the counts displayed
10. Tap "Restore" button
11. Observe import progress dialog
12. When "Import Complete" success dialog appears, tap "OK"
13. Navigate to main photo gallery
14. Verify all photos are present
15. Check categories are intact

**Expected Results**:
- [ ] Biometric authentication prompt appears
- [ ] Document picker shows only .zip files
- [ ] "Validating backup..." message appears
- [ ] Validation completes within 2-3 seconds
- [ ] Confirmation dialog shows:
  - Title: "Restore Backup?"
  - Message with counts: "10 photos, 3 categories" (or similar)
  - "Cancel" button
  - "Restore" button
- [ ] Import progress dialog appears with:
  - Title: "Importing Data"
  - Operation text (e.g., "Restoring categories...", "Importing photos...")
  - Progress percentage
  - Counter showing progress
- [ ] Import completes in under 60 seconds
- [ ] Success dialog shows: "Import Complete" with summary
- [ ] All 10 photos appear in gallery
- [ ] All 3 categories are present
- [ ] Photo-category assignments preserved
- [ ] No duplicate photos created (MERGE strategy)

**Actual Results**: [Tester to fill in]

**Pass/Fail**: [ ] PASS [ ] FAIL

**Screenshots**: [Attach if needed]

**Notes**: [Any observations]

---

### P0-3: Biometric Authentication Works

**Test ID**: P0-3
**Description**: Verify biometric authentication protects export/import
**Pre-conditions**:
- Device has Face ID or Touch ID enabled
- App has biometric permission granted
**Priority**: P0 - CRITICAL

**Test 3A: Export with Successful Authentication**

**Steps**:
1. Navigate to Settings > Export Data
2. When Face ID/Touch ID prompt appears, authenticate successfully
3. Observe operation proceeds

**Expected Results**:
- [ ] Biometric prompt appears immediately
- [ ] Prompt shows reason: "Authenticate to access backup/restore"
- [ ] On success, export progress begins
- [ ] No error messages

**Actual Results**: [Tester to fill in]

**Pass/Fail**: [ ] PASS [ ] FAIL

---

**Test 3B: Export with Failed Authentication**

**Steps**:
1. Navigate to Settings > Export Data
2. When Face ID/Touch ID prompt appears, fail authentication (wrong face/finger, or tap Cancel)
3. Observe behavior

**Expected Results**:
- [ ] Biometric prompt appears
- [ ] On failure/cancel, operation does NOT proceed
- [ ] Returns to Settings screen gracefully
- [ ] No export dialog appears
- [ ] No error dialog (cancellation is silent)

**Actual Results**: [Tester to fill in]

**Pass/Fail**: [ ] PASS [ ] FAIL

---

**Test 3C: Import with Authentication**

**Steps**:
1. Navigate to Settings > Import Data
2. Authenticate successfully when prompted
3. Verify document picker appears

**Expected Results**:
- [ ] Biometric prompt appears before document picker
- [ ] On success, document picker opens
- [ ] File selection proceeds normally

**Actual Results**: [Tester to fill in]

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Any observations]

---

### P0-4: Progress Displays Correctly

**Test ID**: P0-4
**Description**: Verify progress calculation is accurate and updates smoothly
**Pre-conditions**:
- 10 photos in library (from P0-1 setup)
**Priority**: P0 - CRITICAL

**Steps to Execute**:

1. Start export operation
2. Watch progress dialog carefully
3. Note progress percentage values as they update
4. Record approximate update frequency
5. Verify progress reaches 100% when operation completes

**Expected Results**:
- [ ] Progress starts at 0% or low value (under 10%)
- [ ] Progress updates visibly (not frozen)
- [ ] Updates occur at least every 5 photos
- [ ] Progress increases monotonically (never decreases)
- [ ] Progress reaches 100% when operation completes
- [ ] Counter shows realistic values (e.g., "13/13" not "100/100")
- [ ] No sudden jumps (e.g., 10% -> 90%)
- [ ] Operation text changes during phases:
  - "Collecting categories..."
  - "Copying photos..."
  - "Creating backup archive..."
- [ ] Progress dialog remains responsive (not frozen)

**Actual Results**: [Tester to fill in]

**Progress Values Observed**:
- Start: ____%
- After 3 photos: ____%
- After 6 photos: ____%
- After 9 photos: ____%
- Completion: ____%

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Any observations about smoothness, freezing, or calculation accuracy]

---

### P0-5: Error Handling for Invalid Backup

**Test ID**: P0-5
**Description**: Verify app handles invalid backup files gracefully
**Pre-conditions**:
- Create an invalid ZIP file (see setup below)
**Priority**: P0 - CRITICAL

**Setup Invalid Backup File**:

**Option A: Non-ZIP File**
1. Open Notes app on iOS
2. Create text file: "This is not a backup file"
3. Save as `fake_backup.zip` (Files app)

**Option B: Empty ZIP**
1. Use any ZIP utility to create empty ZIP file
2. Name it `empty_backup.zip`

**Steps to Execute**:

1. Navigate to Settings > Import Data
2. Complete biometric authentication
3. In document picker, select the invalid backup file
4. Observe validation behavior
5. Note error message displayed
6. Tap "OK" to dismiss error
7. Verify app returns to Settings screen

**Expected Results**:
- [ ] "Validating backup..." message appears
- [ ] Validation fails within 2-3 seconds
- [ ] Error alert appears with:
  - Title: "Import Error" or similar
  - Message describing issue (e.g., "Invalid backup file", "Corrupted backup")
  - "OK" button to dismiss
- [ ] Error message is user-friendly (not technical)
- [ ] No crash or freeze
- [ ] After dismissal, returns to Settings screen
- [ ] Can retry with different file
- [ ] No partial data imported
- [ ] No changes to existing library

**Actual Results**: [Tester to fill in]

**Error Message Displayed**: [Exact text]

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Any observations]

---

## 4. P1 - Important Tests (SHOULD PASS)

---

### P1-1: Large Backup (100+ Photos)

**Test ID**: P1-1
**Description**: Verify export/import handles larger libraries efficiently
**Pre-conditions**:
- 100 photos in library
- 5-7 categories assigned
**Priority**: P1 - IMPORTANT

**Steps to Execute**:

**Export Phase**:
1. Navigate to Settings > Export Data
2. Complete biometric authentication
3. Start timer when progress dialog appears
4. Observe progress updates
5. Stop timer when ShareSheet appears
6. Save backup file to Downloads
7. Note file size

**Import Phase**:
8. Clear app data or use fresh install (optional)
9. Navigate to Settings > Import Data
10. Select the 100-photo backup file
11. Confirm import
12. Start timer when progress dialog appears
13. Stop timer when success dialog appears
14. Verify all 100 photos imported

**Expected Results**:
- [ ] Export completes in under 60 seconds (target: 30 seconds)
- [ ] Progress updates smoothly without freezing
- [ ] Progress percentage accurate (not stuck at 100%)
- [ ] File size reasonable (~50-150MB depending on photo resolution)
- [ ] Import completes in under 2 minutes (target: 60 seconds)
- [ ] Import progress shows accurate counts
- [ ] All 100 photos imported successfully
- [ ] All categories preserved
- [ ] Photo-category assignments intact
- [ ] No crashes or memory warnings
- [ ] App remains responsive during operations

**Actual Results**: [Tester to fill in]

**Performance Metrics**:
- Export time: _____ seconds
- Import time: _____ seconds
- File size: _____ MB
- Photos imported: _____ / 100
- Categories imported: _____ / _____

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Any performance issues, freezing, or memory warnings]

---

### P1-2: Background App During Export

**Test ID**: P1-2
**Description**: Verify export continues when app is backgrounded
**Pre-conditions**:
- 50+ photos in library (use 100-photo setup if available)
**Priority**: P1 - IMPORTANT

**Steps to Execute**:

1. Navigate to Settings > Export Data
2. Complete biometric authentication
3. When progress dialog appears and reaches ~20%, immediately swipe up to home screen (background app)
4. Wait 10 seconds
5. Re-open SmilePile app
6. Observe behavior

**Expected Results - Success Path**:
- [ ] Background task registered successfully
- [ ] Export continues in background
- [ ] On return, export completed or still in progress
- [ ] ShareSheet appears if completed
- [ ] No error dialog
- [ ] Backup file created successfully

**Expected Results - Interrupted Path** (acceptable if background task fails):
- [ ] Export interrupted when backgrounded
- [ ] On return, error dialog appears:
  - Message: "Operation interrupted. The system stopped the operation to save battery. Please try again."
  - "OK" button to dismiss
- [ ] No partial backup file left behind
- [ ] Can retry export successfully

**Actual Results**: [Tester to fill in]

**Which Path Occurred**: [ ] Success Path [ ] Interrupted Path

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Behavior observed]

---

### P1-3: Cancel Operation Mid-Way

**Test ID**: P1-3
**Description**: Verify user can cancel operations at appropriate points
**Pre-conditions**:
- Fresh app state
**Priority**: P1 - IMPORTANT

**Test 3A: Cancel Document Picker**

**Steps**:
1. Navigate to Settings > Import Data
2. Authenticate successfully
3. Document picker appears
4. Tap "Cancel" in picker
5. Observe behavior

**Expected Results**:
- [ ] Document picker dismisses
- [ ] Returns to Settings screen
- [ ] No error dialog
- [ ] No import occurs
- [ ] App state unchanged

**Actual Results**: [Tester to fill in]

**Pass/Fail**: [ ] PASS [ ] FAIL

---

**Test 3B: Cancel Import Confirmation**

**Steps**:
1. Navigate to Settings > Import Data
2. Authenticate and select valid backup file
3. Confirmation dialog appears showing counts
4. Tap "Cancel" button
5. Observe behavior

**Expected Results**:
- [ ] Confirmation dialog dismisses
- [ ] Returns to Settings screen
- [ ] No import occurs
- [ ] No error dialog
- [ ] App state unchanged
- [ ] Can attempt import again

**Actual Results**: [Tester to fill in]

**Pass/Fail**: [ ] PASS [ ] FAIL

---

**Test 3C: ShareSheet Dismissal**

**Steps**:
1. Navigate to Settings > Export Data
2. Complete export successfully
3. ShareSheet appears
4. Swipe down to dismiss ShareSheet (or tap outside)
5. Observe behavior

**Expected Results**:
- [ ] ShareSheet dismisses
- [ ] Returns to Settings screen
- [ ] No error dialog
- [ ] Temporary export file cleaned up (within 1 hour)
- [ ] Can export again successfully

**Actual Results**: [Tester to fill in]

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Any observations]

---

### P1-4: Orphaned Temp File Cleanup

**Test ID**: P1-4
**Description**: Verify temp files are cleaned up after crashes or force-quits
**Pre-conditions**:
- Developer ability to inspect app's Documents directory
**Priority**: P1 - IMPORTANT

**Steps to Execute**:

1. Start export operation
2. When progress reaches ~50%, force-quit the app:
   - Swipe up to app switcher
   - Swipe up on SmilePile to close
3. Wait 5 seconds
4. Re-open SmilePile app
5. Allow app to fully launch
6. Inspect app's temp directory:
   - Use Xcode > Devices > Container > Download Container
   - Or use Files app if accessible
7. Look for files matching pattern: `backup_temp_*` or `restore_temp_*`
8. Note their creation timestamps
9. Wait 1 hour
10. Re-check temp directory

**Expected Results**:
- [ ] Orphaned temp file present immediately after relaunch (acceptable)
- [ ] Cleanup occurs on app launch (within 1 minute of opening app)
- [ ] Files older than 1 hour are deleted
- [ ] No temp files remain after cleanup
- [ ] Active operations' temp files are preserved
- [ ] No crash during cleanup
- [ ] No user-visible errors

**Actual Results**: [Tester to fill in]

**Temp Files Found**:
- Immediately after relaunch: [count] files
- After 1 hour: [count] files
- File names: [list]

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Any issues with cleanup]

---

### P1-5: Multiple Imports of Same Backup (MERGE Strategy)

**Test ID**: P1-5
**Description**: Verify MERGE strategy prevents duplicate photos
**Pre-conditions**:
- Valid backup file with 10 photos, 3 categories
- App already has 5 of those 10 photos (from previous import)
**Priority**: P1 - IMPORTANT

**Steps to Execute**:

1. Import backup file (first import)
2. Note photo count in gallery: should be 10 photos
3. Navigate to Settings > Import Data again
4. Select the SAME backup file
5. Confirm import
6. Wait for import to complete
7. Check photo count in gallery
8. Check for duplicate photos

**Expected Results**:
- [ ] Second import completes successfully
- [ ] Photo count remains 10 (not 20)
- [ ] No duplicate photos created
- [ ] Existing photos preserved (not overwritten)
- [ ] Categories remain 3 (not duplicated)
- [ ] Photo-category assignments intact
- [ ] Success message shows "0 photos imported" or similar

**Actual Results**: [Tester to fill in]

**Photo Counts**:
- Before first import: _____
- After first import: _____
- After second import: _____

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Any observations about duplicate handling]

---

### P1-6: Cross-Platform Import (Android → iOS)

**Test ID**: P1-6
**Description**: Verify iOS can import Android-created backups
**Pre-conditions**:
- Android backup file available (see Pre-Test Setup 1.4)
- Fresh iOS app state or separate test library
**Priority**: P1 - IMPORTANT

**Steps to Execute**:

1. Obtain Android backup file (see setup instructions)
2. Transfer backup file to iOS device (AirDrop, Files app, etc.)
3. Open SmilePile iOS app
4. Navigate to Settings > Import Data
5. Select Android backup file
6. Observe validation
7. Confirm import
8. Wait for completion
9. Verify all photos imported
10. Verify all categories present
11. Check photo-category assignments
12. Compare to Android device if available

**Expected Results**:
- [ ] Validation succeeds (Android backup recognized)
- [ ] Confirmation shows correct counts
- [ ] Import completes without errors
- [ ] All photos from Android backup appear in iOS app
- [ ] All categories from Android backup present
- [ ] Photo-category assignments preserved
- [ ] Photo quality/resolution intact
- [ ] Thumbnails generated correctly
- [ ] No missing metadata
- [ ] No corruption or data loss

**Actual Results**: [Tester to fill in]

**Data Integrity**:
- Android photos: _____ | iOS imported: _____
- Android categories: _____ | iOS imported: _____
- Photo quality: [ ] Same [ ] Different (describe: _______)

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Any compatibility issues]

---

## 5. P2 - Edge Case Tests (NICE TO VERIFY)

---

### P2-1: Insufficient Storage

**Test ID**: P2-1
**Description**: Verify graceful handling when device storage is full
**Pre-conditions**:
- Device with limited storage (or simulated)
- Large backup file (100+ photos, ~100MB)
**Priority**: P2 - EDGE CASE

**Setup**:
1. Fill device storage to near capacity (leave <50MB free)
2. Prepare large backup for import

**Steps to Execute**:

1. Navigate to Settings > Import Data
2. Select large backup file
3. Confirm import
4. Observe behavior

**Expected Results**:
- [ ] Import starts
- [ ] Fails gracefully when storage exhausted
- [ ] Error alert appears with:
  - Title: "Import Error" or "Insufficient Storage"
  - Message: User-friendly explanation of storage issue
  - "OK" button
- [ ] No crash
- [ ] Partial import rolled back (no incomplete data)
- [ ] Temp files cleaned up
- [ ] App remains functional

**Actual Results**: [Tester to fill in]

**Error Message**: [Exact text]

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Behavior observed]

---

### P2-2: Force Quit During Operation

**Test ID**: P2-2
**Description**: Verify app recovers from force-quit during export/import
**Pre-conditions**:
- 50+ photos in library
**Priority**: P2 - EDGE CASE

**Test 2A: Force Quit During Export**

**Steps**:
1. Start export operation
2. When progress reaches ~30-40%, force-quit app
3. Re-open app
4. Navigate to Settings
5. Attempt new export

**Expected Results**:
- [ ] App relaunches normally
- [ ] No crash or error on relaunch
- [ ] Previous export aborted (no partial backup file)
- [ ] Temp files cleaned up on relaunch
- [ ] New export can be started successfully
- [ ] New export completes normally

**Actual Results**: [Tester to fill in]

**Pass/Fail**: [ ] PASS [ ] FAIL

---

**Test 2B: Force Quit During Import**

**Steps**:
1. Start import operation
2. When progress reaches ~30-40%, force-quit app
3. Re-open app
4. Check photo gallery
5. Attempt new import

**Expected Results**:
- [ ] App relaunches normally
- [ ] No crash or error on relaunch
- [ ] Previous import rolled back (no partial photos)
- [ ] Photo count unchanged from before import
- [ ] Temp files cleaned up
- [ ] New import can be started
- [ ] New import completes successfully

**Actual Results**: [Tester to fill in]

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Any data corruption or inconsistencies]

---

### P2-3: Empty Backup (0 Photos)

**Test ID**: P2-3
**Description**: Verify handling of backup with no photos
**Pre-conditions**:
- Create backup from app with 0 photos, only categories
**Priority**: P2 - EDGE CASE

**Setup**:
1. Fresh install or clear all photos from app
2. Create 2-3 categories (no photos assigned)
3. Export backup
4. Result: backup with 0 photos, only categories

**Steps to Execute**:

1. Navigate to Settings > Import Data
2. Select empty backup file (0 photos)
3. Observe validation and confirmation
4. Confirm import
5. Observe result

**Expected Results**:
- [ ] Validation succeeds
- [ ] Confirmation shows "0 photos, X categories"
- [ ] Import proceeds
- [ ] Import completes successfully
- [ ] Success message: "0 photos imported successfully" or similar
- [ ] Categories imported correctly
- [ ] No crash or error

**Actual Results**: [Tester to fill in]

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Any issues]

---

### P2-4: Corrupted ZIP File

**Test ID**: P2-4
**Description**: Verify handling of partially corrupted ZIP
**Pre-conditions**:
- Valid backup file
- Hex editor or ZIP corruption tool
**Priority**: P2 - EDGE CASE

**Setup Corrupted ZIP**:

**Option A: Manual Corruption**
1. Take valid backup ZIP file
2. Open in hex editor
3. Change random bytes in middle of file
4. Save as `corrupted_backup.zip`

**Option B: Truncated ZIP**
1. Take valid backup ZIP file
2. Use command: `head -c 5000 valid_backup.zip > truncated_backup.zip`
3. Result: incomplete ZIP file

**Steps to Execute**:

1. Navigate to Settings > Import Data
2. Select corrupted backup file
3. Observe validation behavior
4. Note error message

**Expected Results**:
- [ ] Validation detects corruption
- [ ] Error alert appears:
  - Title: "Import Error"
  - Message: "Corrupted backup - file is damaged or incomplete" or similar
  - "OK" button
- [ ] No crash
- [ ] No partial import
- [ ] App remains functional
- [ ] Can retry with different file

**Actual Results**: [Tester to fill in]

**Error Message**: [Exact text]

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [How corruption was detected]

---

### P2-5: Very Large Backup (500+ Photos)

**Test ID**: P2-5
**Description**: Stress test with very large library
**Pre-conditions**:
- 500+ photos in library (see Pre-Test Setup 1.3)
- Patient tester (this will take time)
**Priority**: P2 - EDGE CASE

**Steps to Execute**:

1. Start export with 500+ photos
2. Monitor progress carefully
3. Note memory usage if possible (Xcode Instruments)
4. Time the operation
5. Observe ShareSheet appearance
6. Save backup file
7. Note file size
8. Import the backup on fresh install
9. Time the import operation
10. Verify photo count matches

**Expected Results**:
- [ ] Export completes (may take 2-5 minutes)
- [ ] Progress updates smoothly (not frozen)
- [ ] Progress percentage accurate throughout (reaches 100% at completion)
- [ ] No memory warnings or crashes
- [ ] ShareSheet appears successfully
- [ ] File size reasonable (~500MB-2GB depending on photo quality)
- [ ] Import completes (may take 5-10 minutes)
- [ ] All 500 photos imported successfully
- [ ] No data corruption
- [ ] App remains stable

**Actual Results**: [Tester to fill in]

**Performance Metrics**:
- Photo count: _____
- Export time: _____ minutes _____ seconds
- File size: _____ MB / GB
- Import time: _____ minutes _____ seconds
- Photos imported: _____ / 500
- Memory warnings: [ ] Yes [ ] No
- Crashes: [ ] Yes [ ] No

**Pass/Fail**: [ ] PASS [ ] FAIL

**Notes**: [Any performance degradation, freezing, or issues]

---

## 6. Acceptance Criteria Validation

This section maps test cases to the 33 acceptance criteria from the user story.

### Export Functionality (AC-1 to AC-5)

| AC | Requirement | Test Coverage | Status |
|----|-------------|---------------|--------|
| AC-1 | User can tap "Export Data" button | P0-1 step 4 | [ ] |
| AC-2 | Export progress dialog shows operation, spinner, counter | P0-1 steps 6, P0-4 | [ ] |
| AC-3 | Export creates timestamped ZIP file | P0-1 step 12 | [ ] |
| AC-4 | ShareSheet appears with save/share options | P0-1 steps 7-8 | [ ] |
| AC-5 | Export includes all data (photos, categories, settings) | P0-2 steps 14-15 | [ ] |

### Import Functionality (AC-6 to AC-13)

| AC | Requirement | Test Coverage | Status |
|----|-------------|---------------|--------|
| AC-6 | User can tap "Import Data" button | P0-2 step 3 | [ ] |
| AC-7 | Document picker shows .zip files only | P0-2 step 5 | [ ] |
| AC-8 | Validation occurs with progress indicator | P0-2 step 8 | [ ] |
| AC-9 | Confirmation dialog shows photo/category counts | P0-2 step 9 | [ ] |
| AC-10 | Invalid backup shows clear error | P0-5 steps 5-6 | [ ] |
| AC-11 | Import progress dialog shows operation details | P0-2 step 11 | [ ] |
| AC-12 | Import uses MERGE strategy (no duplicates) | P1-5 | [ ] |
| AC-13 | Success dialog shows import summary | P0-2 step 12 | [ ] |

### Progress Feedback (AC-14 to AC-17)

| AC | Requirement | Test Coverage | Status |
|----|-------------|---------------|--------|
| AC-14 | Export progress updates every 5 photos | P0-4 | [ ] |
| AC-15 | Import progress updates every 5 photos | P0-2 step 11 | [ ] |
| AC-16 | Progress dialogs are modal (cannot dismiss) | P0-1 step 6, P0-2 step 11 | [ ] |
| AC-17 | Progress messages specific to operation | P0-4 | [ ] |

### Error Handling (AC-18 to AC-22)

| AC | Requirement | Test Coverage | Status |
|----|-------------|---------------|--------|
| AC-18 | Export errors show alert with OK button | P2-1 (storage error) | [ ] |
| AC-19 | Import errors show alert with OK button | P0-5, P2-4 | [ ] |
| AC-20 | Validation errors are specific | P0-5, P2-4 | [ ] |
| AC-21 | Storage errors are clear | P2-1 | [ ] |
| AC-22 | All errors allow retry | P0-5 step 7 | [ ] |

### Cross-Platform Compatibility (AC-23 to AC-26)

| AC | Requirement | Test Coverage | Status |
|----|-------------|---------------|--------|
| AC-23 | iOS backups import on Android | Manual (not iOS testing) | N/A |
| AC-24 | Android backups import on iOS | P1-6 | [ ] |
| AC-25 | Backup format is ZIP with metadata.json | P0-1 (file inspection) | [ ] |
| AC-26 | Backup validation checks format, version | P0-5, P2-4 | [ ] |

### User Experience (AC-27 to AC-33)

| AC | Requirement | Test Coverage | Status |
|----|-------------|---------------|--------|
| AC-27 | Export < 30s for 100 photos | P1-1 (export phase) | [ ] |
| AC-28 | Import < 60s for 100 photos | P1-1 (import phase) | [ ] |
| AC-29 | Progress appears within 500ms | P0-1 step 6, P0-4 | [ ] |
| AC-30 | Dialogs have titles, dismissible when safe | All progress/alert tests | [ ] |
| AC-31 | Temp export file cleaned up after sharing | P1-4 | [ ] |
| AC-32 | User can cancel document picker without error | P1-3 Test 3A | [ ] |
| AC-33 | User can cancel import confirmation without error | P1-3 Test 3B | [ ] |

**Acceptance Criteria Summary**:
- Total AC: 33
- Covered by tests: 30
- Not applicable to iOS: 1 (AC-23)
- Pass rate: _____ / 30 (_____ %)

---

## 7. Bug Report Template

Use this template to document any failures encountered during testing.

---

### Bug Report Form

**Bug ID**: BUG-[sequential number]
**Test ID**: [e.g., P0-1, P1-3]
**Date Found**: [Date]
**Tester**: [Your name]
**Severity**: [ ] Critical [ ] High [ ] Medium [ ] Low

---

**Summary**: [One-line description]

**Steps to Reproduce**:
1. [Step 1]
2. [Step 2]
3. [Step 3]

**Expected Result**: [What should happen]

**Actual Result**: [What actually happened]

**Screenshots/Videos**: [Attach or reference]

**Device Information**:
- Device: [e.g., iPhone 16 Pro simulator]
- iOS Version: [e.g., iOS 16.5]
- App Version: [e.g., v25.10.08.001]
- Storage Available: [e.g., 4.2GB]
- Biometric Setup: [ ] Face ID [ ] Touch ID [ ] None

**Console Logs**: [Relevant error messages from Xcode console]

**Reproducibility**:
- [ ] Always (100%)
- [ ] Often (50-99%)
- [ ] Sometimes (10-49%)
- [ ] Rarely (<10%)

**Workaround**: [If any]

**Additional Notes**: [Any other relevant information]

---

### Example Bug Report

**Bug ID**: BUG-001
**Test ID**: P0-4
**Date Found**: 2025-10-08
**Tester**: Jane Doe
**Severity**: [X] Critical

**Summary**: Export progress freezes at 100% when exporting 150 photos

**Steps to Reproduce**:
1. Load 150 photos into SmilePile library
2. Navigate to Settings > Export Data
3. Authenticate and start export
4. Watch progress dialog

**Expected Result**: Progress updates smoothly from 0% to 100%, reaching 100% when export completes

**Actual Result**: Progress reaches 100% after exporting ~100 photos, then appears frozen for 1-2 minutes while remaining photos are processed. ShareSheet eventually appears, but user thinks app is frozen.

**Screenshots/Videos**: [screenshot_frozen_progress.png]

**Device Information**:
- Device: iPhone 16 Pro simulator
- iOS Version: iOS 16.5
- App Version: v25.10.08.001
- Storage Available: 8.5GB
- Biometric Setup: [X] Face ID

**Console Logs**:
```
[BackupManager] totalItems hardcoded to 100, processedItems = 100
[BackupManager] Processing photo 101/150...
[BackupViewModel] Progress: 100.0% (no update displayed)
```

**Reproducibility**:
- [X] Always (100%)

**Workaround**: None. Operation completes successfully, but UX is poor.

**Additional Notes**: This appears to be CRITICAL-1 from the code review - progress calculation hardcoded to 100 items.

---

## 8. Sign-off Checklist

### 8.1 Pre-Production Sign-Off

Complete this checklist before approving the feature for production release.

#### P0 Tests (All Must Pass)

- [ ] P0-1: Export with 10 photos - PASS
- [ ] P0-2: Import valid backup - PASS
- [ ] P0-3: Biometric authentication works - PASS
  - [ ] 3A: Successful authentication - PASS
  - [ ] 3B: Failed authentication - PASS
  - [ ] 3C: Import authentication - PASS
- [ ] P0-4: Progress displays correctly - PASS
- [ ] P0-5: Error handling for invalid backup - PASS

**P0 Pass Rate**: _____ / 5 (100% required)

---

#### P1 Tests (At Least 80% Must Pass)

- [ ] P1-1: Large backup (100+ photos) - PASS/FAIL
- [ ] P1-2: Background app during export - PASS/FAIL
- [ ] P1-3: Cancel operations - PASS/FAIL
  - [ ] 3A: Cancel document picker - PASS/FAIL
  - [ ] 3B: Cancel import confirmation - PASS/FAIL
  - [ ] 3C: ShareSheet dismissal - PASS/FAIL
- [ ] P1-4: Orphaned temp file cleanup - PASS/FAIL
- [ ] P1-5: Multiple imports (MERGE strategy) - PASS/FAIL
- [ ] P1-6: Cross-platform (Android → iOS) - PASS/FAIL

**P1 Pass Rate**: _____ / 6 (minimum 5/6 = 83%)

---

#### P2 Tests (Optional, But Should Handle Gracefully)

- [ ] P2-1: Insufficient storage - Handled gracefully
- [ ] P2-2: Force quit during operation - Handled gracefully
  - [ ] 2A: Force quit during export
  - [ ] 2B: Force quit during import
- [ ] P2-3: Empty backup (0 photos) - Handled gracefully
- [ ] P2-4: Corrupted ZIP file - Handled gracefully
- [ ] P2-5: Very large backup (500+ photos) - Handled gracefully

**P2 Tests Executed**: _____ / 5

---

### 8.2 Critical Checks

#### No Critical Bugs

- [ ] No crashes encountered during any test
- [ ] No data loss scenarios observed
- [ ] No security vulnerabilities exposed
- [ ] All error messages are user-friendly
- [ ] Progress calculation accurate for all library sizes tested

#### Cross-Platform Compatibility

- [ ] Android backup successfully imported on iOS (P1-6 passed)
- [ ] All photos imported without corruption
- [ ] All categories preserved
- [ ] Photo-category assignments intact

#### Performance Requirements Met

- [ ] Export 100 photos: < 60 seconds (target: 30s)
- [ ] Import 100 photos: < 2 minutes (target: 60s)
- [ ] Progress feedback appears within 500ms
- [ ] UI remains responsive during operations

#### Security Requirements Met

- [ ] Biometric authentication required for export/import
- [ ] Backup files have appropriate permissions
- [ ] Temp files cleaned up properly
- [ ] No security settings exposed in metadata (verify metadata.json)

---

### 8.3 Build Quality

- [ ] Build version tested: _____________
- [ ] No compiler warnings observed
- [ ] No runtime errors in console (except expected validation errors)
- [ ] Memory usage acceptable (no warnings during testing)
- [ ] Battery usage acceptable (no excessive drain)

---

### 8.4 Documentation

- [ ] All test cases executed and documented
- [ ] All bugs filed with proper details
- [ ] Screenshots attached for visual bugs
- [ ] Performance metrics recorded
- [ ] AC validation table completed

---

### 8.5 Final Approval

**Tester Signature**: _________________________
**Date**: _________________________

**Test Summary**:
- P0 Pass Rate: _____ / 5 (_____ %)
- P1 Pass Rate: _____ / 6 (_____ %)
- P2 Tests Executed: _____ / 5
- Critical Bugs Found: _____
- High Bugs Found: _____
- Medium Bugs Found: _____

**Recommendation**:
- [ ] APPROVE - Ready for production
- [ ] APPROVE WITH MINOR ISSUES - Deploy with known issues documented
- [ ] REJECT - Critical bugs must be fixed before production

**Comments**: [Tester notes]

---

**Product Manager Approval**: _________________________
**Date**: _________________________

---

## Appendix A: Quick Reference

### Test Execution Shortcuts

**Minimum Viable Testing** (1 hour):
- Execute all P0 tests only
- Document any failures
- Sign-off if all P0 pass

**Standard Testing** (2-3 hours):
- Execute all P0 tests
- Execute all P1 tests
- Execute P2 tests as time allows
- Document all results

**Comprehensive Testing** (4+ hours):
- Execute all tests (P0, P1, P2)
- Test on both simulator and physical device
- Performance profiling with Instruments
- Memory leak detection
- Full cross-platform validation

---

### Common Issues & Solutions

**Issue**: Biometric prompt doesn't appear
**Solution**: Check Settings > Face ID & Passcode, ensure SmilePile has permission

**Issue**: Document picker shows no files
**Solution**: Ensure backup ZIP is in accessible location (Downloads, iCloud Drive)

**Issue**: Export takes very long time
**Solution**: Normal for large libraries; ensure adequate storage and wait patiently

**Issue**: Import fails with "Invalid backup"
**Solution**: Verify ZIP is valid, not corrupted; try re-exporting from source

**Issue**: Progress appears frozen
**Solution**: May be CRITICAL-1 bug; note behavior and file bug report

---

### File Inspection Commands

**Extract ZIP to inspect metadata.json**:
1. Transfer backup ZIP to Mac
2. Right-click > Open With > Archive Utility
3. Open extracted folder
4. Open `metadata.json` in text editor
5. Verify structure:
   - Contains "photos" array
   - Contains "categories" array
   - Contains "settings" object
   - Does NOT contain "securitySettings" (CRITICAL-3 fix)

**Check temp directory on simulator**:
```bash
xcrun simctl get_app_container booted com.smilepile.SmilePile data
cd Documents
ls -la *temp*
```

---

## Appendix B: Test Data Templates

### Metadata.json Structure (Expected)

```json
{
  "version": "1.0",
  "exportDate": "2025-10-08T14:30:52Z",
  "photos": [
    {
      "id": "photo_001",
      "filename": "photo_001.jpg",
      "thumbnail": "photo_001-thumb.jpg",
      "createdAt": "2025-10-01T10:00:00Z",
      "categories": ["category_001", "category_002"]
    }
  ],
  "categories": [
    {
      "id": "category_001",
      "name": "Family",
      "color": "#FF6B6B",
      "createdAt": "2025-09-15T09:00:00Z"
    }
  ],
  "settings": {
    "isDarkMode": false
  }
}
```

**Security Check**: Ensure NO "securitySettings" field exists

---

## Appendix C: Performance Benchmarks

Expected performance targets based on implementation summary:

| Library Size | Export Time (Target) | Import Time (Target) |
|--------------|----------------------|----------------------|
| 10 photos    | < 5 seconds          | < 10 seconds         |
| 50 photos    | < 15 seconds         | < 30 seconds         |
| 100 photos   | < 30 seconds         | < 60 seconds         |
| 200 photos   | < 60 seconds         | < 2 minutes          |
| 500 photos   | < 3 minutes          | < 5 minutes          |

**Note**: Actual times depend on:
- Photo resolution (HEIC/JPG/PNG)
- Device performance (simulator vs. physical device)
- Available storage (SSD vs. slower storage)
- Concurrent processes

---

## Document History

| Version | Date       | Author      | Changes               |
|---------|------------|-------------|-----------------------|
| 1.0     | 2025-10-08 | Claude Code | Initial test plan     |

---

**End of Test Plan**
