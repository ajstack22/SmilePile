# Wave 1 Foundation Setup - Action Items

**Date**: 2025-10-14
**Status**: 95% Complete

---

## Critical Actions (BEFORE WAVE 2)

Must complete before starting Wave 2:

### 1. Fix File Permissions (2 minutes)

**Issue**: Two credential files have incorrect permissions (644 instead of 600)

```bash
# Fix API key
chmod 600 ~/app-store-connect-api-keys/AuthKey_RAGW8S622J.p8

# Fix keystore
chmod 600 ~/keystores/smilepile-upload.keystore

# Verify corrections
ls -l ~/app-store-connect-api-keys/*.p8
# Expected: -rw------- (600)

ls -l ~/keystores/*.keystore
# Expected: -rw------- (600)
```

**Why Critical**: Files currently readable by other system users (security risk)

**Time Required**: 2 minutes

**Owner**: User

**Deadline**: Before Wave 2 start

---

### 2. Verify Keystore Alias (1 minute)

**Issue**: Need to confirm keystore alias for documentation

```bash
keytool -list -v -keystore ~/keystores/smilepile-upload.keystore
# Enter password when prompted
# Look for "Alias name:" in output
```

**Expected Alias**: smilepile-upload

**Time Required**: 1 minute

**Owner**: User

**Deadline**: Before Wave 2 start

---

### 3. Document Password Location (1 minute)

**Issue**: Confirm keystore passwords are stored in password manager

**Action**:
1. Open password manager
2. Verify secure note exists: "SmilePile Android Keystore"
3. Confirm it contains:
   - Keystore store password
   - Keystore key password
   - Keystore alias
   - SHA-256 fingerprint

**Time Required**: 1 minute

**Owner**: User

**Deadline**: Before Wave 2 start

---

## Total Critical Actions Time: 5 minutes

After completing these 3 items, Wave 2 can begin immediately.

---

## Important Actions (Don't Block Wave 2)

Can be completed in parallel with Waves 2-4:

### 4. Verify Apps in App Store Connect (30 minutes)

**Action**:
1. Log into https://appstoreconnect.apple.com
2. Navigate to "My Apps"
3. Check if "SmilePile" app exists
4. If not exists: Create app using Wave 1 documentation
5. Document App ID

**Required For**: Wave 5 (Fastlane automation)

**Deadline**: Before Wave 5 start

---

### 5. Verify Apps in Play Console (30 minutes)

**Action**:
1. Log into https://play.google.com/console
2. Navigate to "All apps"
3. Check if "SmilePile" app exists
4. If not exists: Create app using Wave 1 documentation
5. Document Application ID

**Required For**: Wave 5 (Fastlane automation)

**Deadline**: Before Wave 5 start

---

### 6. Configure TestFlight Groups (30 minutes)

**Action**:
1. In App Store Connect → SmilePile → TestFlight
2. Create "Internal Testing" group
3. Create "External Testing" group
4. Add test information (description, feedback email)
5. Document group names and invite links

**Required For**: Wave 5 (first build upload)

**Deadline**: Before first TestFlight build upload

---

### 7. Configure Play Console Testing Tracks (30 minutes)

**Action**:
1. In Play Console → SmilePile → Testing
2. Configure "Internal testing" track
3. Configure "Closed testing" track
4. Create tester email lists
5. Document track URLs

**Required For**: Wave 5 (first build upload)

**Deadline**: Before first Play Console build upload

---

### 8. Test Credentials with Fastlane (15 minutes)

**Action**:
```bash
# Test API key
cd ios
fastlane pilot list

# Test service account
cd ../android
fastlane supply init
```

**Required For**: Wave 5 (automation verification)

**Deadline**: Before Wave 5 implementation

---

## Total Important Actions Time: ~2 hours

These can be done anytime before Wave 5. Do NOT block Wave 2-4 on these items.

---

## Nice-to-Have Actions (Can Defer)

Optional items that can be completed anytime:

### 9. Complete Backup Verification (30 minutes)

**Action**:
1. Verify keystore backup in iCloud Drive
2. Verify keystore backup in password manager
3. Verify keystore backup in external location
4. Test restoration from each location
5. Document SHA-256 checksums

**Benefit**: Ensures disaster recovery capability

**Deadline**: Before production release (Wave 10)

---

### 10. Create Team Access Matrix (15 minutes)

**Action**:
1. Document who has access to Apple Developer
2. Document who has access to Play Console
3. Document who has access to password manager
4. Define backup deployer
5. Share credentials securely

**Benefit**: Reduces bus factor

**Deadline**: Before production release (Wave 10)

---

## Summary

**Critical (Block Wave 2)**: 3 items, 5 minutes total
**Important (Don't Block Wave 2)**: 5 items, ~2 hours total
**Nice-to-Have (Defer)**: 2 items, ~45 minutes total

---

## Immediate Next Steps

1. Run permission fixes (2 minutes)
2. Verify keystore alias (1 minute)
3. Check password manager (1 minute)
4. **Begin Wave 2** (iOS Tier Configuration)

---

**Action Items Clear. Ready to Complete and Proceed to Wave 2.**
