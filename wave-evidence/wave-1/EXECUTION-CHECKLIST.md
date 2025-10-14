# Wave 1 Foundation Setup - Execution Checklist

**Status**: 🚧 IN PROGRESS
**Started**: 2025-10-14
**Estimated Time**: 6-8 hours over 5-7 days

---

## Overview

This checklist guides you through executing Wave 1 Foundation Setup for SmilePile. Each task has detailed procedures in `/wave-evidence/wave-1/06-implementation-results.md`.

**IMPORTANT**: SmilePile inherits StackMap's accounts (Apple Team 84W9WSYQQB, Google Play Console). Some steps may already be complete.

---

## Phase 5A: Account Verification (30 minutes)

### Task 1: Verify Apple Developer Account
- [x] Visit https://developer.apple.com/account
- [x] Confirm Team 84W9WSYQQB is active
- [x] Verify membership expiration date: __________
- [x] Confirm account status: Active ✅ / Expired ❌

### Task 2: Verify App Store Connect
- [x] Visit https://appstoreconnect.apple.com
- [ ] Confirm can access "My Apps" section
- [ ] Check existing apps (StackMap may already be listed)
- [ ] Confirm SmilePile app exists: YES ✅ / NO ❌ (if NO, continue to Phase 5B)

### Task 3: Verify Google Play Console
- [ ] Visit https://play.google.com/console
- [ ] Confirm account is active and paid
- [ ] Check existing apps (StackMap may already be listed)
- [ ] Confirm SmilePile app exists: YES ✅ / NO ❌ (if NO, continue to Phase 5B)

**Checkpoint**: All accounts accessible? If YES ✅, continue. If NO ❌, troubleshoot access issues.

---

## Phase 5B: App Creation (1-2 hours)

**Skip this phase if SmilePile apps already exist in both stores.**

### Task 4: Create SmilePile in App Store Connect (iOS)
- [ ] Navigate to App Store Connect → My Apps → "+"
- [ ] Select "New App"
- [ ] Fill in details:
  - Platform: iOS ✅
  - Name: "SmilePile"
  - Primary Language: English (U.S.)
  - Bundle ID: com.smilepile (select or create)
  - SKU: com.smilepile.ios
  - User Access: Full Access
- [ ] Click "Create"
- [ ] **Document App ID**: __________
- [ ] **Document Bundle ID**: __________

### Task 5: Create SmilePile in Play Console (Android)
- [ ] Navigate to Play Console → All apps → "Create app"
- [ ] Fill in details:
  - App name: "SmilePile"
  - Default language: English (United States)
  - App or game: App
  - Free or paid: Free
  - User program policies: Accept ✅
  - US export laws: Accept ✅
- [ ] Click "Create app"
- [ ] **Document Package Name**: com.smilepile
- [ ] **Document App ID**: __________

**Checkpoint**: Both apps created? If YES ✅, continue. If NO ❌, resolve issues.

---

## Phase 5C: Testing Track Configuration (1 hour)

### Task 6: Configure TestFlight Internal Testing (iOS)
- [ ] Navigate to App Store Connect → SmilePile → TestFlight
- [ ] Click on "Internal Testing" (if not exists, create group)
- [ ] Group name: "Internal Testers" or "SmilePile Team"
- [ ] Add internal testers (email addresses):
  - [ ] ____________________ (your email)
  - [ ] ____________________ (team member)
  - [ ] ____________________ (additional)
- [ ] Enable automatic distribution: YES ✅ / NO ❌
- [ ] **Document Group Name**: __________

### Task 7: Configure TestFlight External Testing (iOS)
- [ ] Navigate to TestFlight → External Testing
- [ ] Create group: "Beta Testers" or "SmilePile Beta"
- [ ] Add test information (required for first external build):
  - Beta App Description: __________
  - Feedback Email: __________
  - Marketing URL (optional): __________
  - Privacy Policy URL (optional): __________
- [ ] **DO NOT add testers yet** (wait for first build)
- [ ] **Document Group Name**: __________

### Task 8: Configure Play Console Internal Testing (Android)
- [ ] Navigate to Play Console → SmilePile → Testing → Internal testing
- [ ] Click "Create new release"
- [ ] Release name: "Internal Testing Track"
- [ ] Add testers:
  - Click "Create email list"
  - List name: "SmilePile Internal Testers"
  - Add emails:
    - [ ] ____________________ (your email)
    - [ ] ____________________ (team member)
- [ ] Save (don't publish yet - no build to upload)
- [ ] **Document Track URL**: __________

### Task 9: Configure Play Console Closed Testing (Android)
- [ ] Navigate to Play Console → SmilePile → Testing → Closed testing
- [ ] Click "Create new track" (if not exists)
- [ ] Track name: "Beta"
- [ ] Create email list: "SmilePile Beta Testers"
- [ ] Save (don't add testers yet)
- [ ] **Document Track Name**: __________

**Checkpoint**: All testing tracks configured? If YES ✅, continue.

---

## Phase 5D: Credential Setup (2-3 hours) - CRITICAL

**⚠️ MOST IMPORTANT PHASE - Take your time and follow procedures exactly**

### Task 10: Generate App Store Connect API Key
- [ ] Navigate to App Store Connect → Users and Access → Keys
- [ ] Click "+" to generate new API key
- [ ] Name: "SmilePile Fastlane Automation"
- [ ] Access: App Manager
- [ ] Click "Generate"
- [ ] **Download AuthKey_XXXXXXXXXX.p8** (only chance to download!)
- [ ] **Document Key ID**: __________
- [ ] **Document Issuer ID**: __________

**Store API Key Securely**:
```bash
# Run these commands:
mkdir -p ~/app-store-connect-api-keys
mv ~/Downloads/AuthKey_*.p8 ~/app-store-connect-api-keys/
chmod 600 ~/app-store-connect-api-keys/AuthKey_*.p8
ls -la ~/app-store-connect-api-keys/
```
- [ ] Commands executed successfully
- [ ] File permissions: -rw------- (600) ✅
- [ ] **Document full path**: __________

### Task 11: Generate Android Production Keystore - CRITICAL

**⚠️ IF THIS KEYSTORE IS LOST, YOUR APP CANNOT BE UPDATED IN PLAY STORE**

```bash
# Create keystores directory
mkdir -p ~/keystores
cd ~/keystores

# Generate strong password (20+ chars, mixed case/numbers/symbols)
# IMPORTANT: Save this password in your password manager NOW!
# Example: Use 1Password to generate 32-char password

# Generate keystore
keytool -genkeypair \
  -v \
  -keystore smilepile-upload.keystore \
  -alias smilepile-upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=SmilePile, OU=Development, O=SmilePile, L=San Francisco, ST=California, C=US"

# When prompted for password, enter your strong password
# Re-enter to confirm
```

**Checklist**:
- [ ] Strong password generated and saved in password manager
- [ ] Keystore generated successfully
- [ ] **Document keystore alias**: smilepile-upload
- [ ] **Document creation date**: __________

**Verify Keystore**:
```bash
keytool -list -v -keystore smilepile-upload.keystore -alias smilepile-upload
```
- [ ] Keystore verified successfully
- [ ] **Document SHA-256 fingerprint**: __________

### Task 12: Backup Keystore (CRITICAL - Triple Redundancy)

**⚠️ BACKUP IMMEDIATELY - THIS IS NOT OPTIONAL**

**Backup Location 1: iCloud Encrypted Folder**
```bash
mkdir -p ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/
cp ~/keystores/smilepile-upload.keystore ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/
```
- [ ] Copied to iCloud
- [ ] Verified file exists in iCloud folder

**Backup Location 2: External Drive (if available) OR 1Password/Bitwarden**
- [ ] Option A: Copied to external encrypted drive: __________
- [ ] Option B: Uploaded to password manager as secure document

**Backup Location 3: Secondary Cloud Storage (Google Drive, Dropbox, etc.)**
- [ ] Copied to: __________
- [ ] Verified upload complete

**Verify All Backups with SHA-256 Checksum**:
```bash
# Get checksum of original
shasum -a 256 ~/keystores/smilepile-upload.keystore

# Compare with each backup location
shasum -a 256 ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/smilepile-upload.keystore
# (repeat for other locations)
```
- [ ] All checksums match original ✅
- [ ] **Document SHA-256**: __________

**Test Keystore Restoration from Backup 1**:
```bash
mkdir -p ~/keystore-test
cp ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/smilepile-upload.keystore ~/keystore-test/
keytool -list -v -keystore ~/keystore-test/smilepile-upload.keystore
rm -rf ~/keystore-test
```
- [ ] Restoration test successful ✅

### Task 13: Create Google Play Service Account

**Step 1: Create Google Cloud Project**
- [ ] Visit https://console.cloud.google.com
- [ ] Click "Select a project" → "New Project"
- [ ] Project name: "SmilePile Deployment"
- [ ] Click "Create"
- [ ] **Document Project ID**: __________

**Step 2: Enable Google Play Developer API**
- [ ] In Google Cloud Console, select "SmilePile Deployment" project
- [ ] Navigate to "APIs & Services" → "Library"
- [ ] Search for "Google Play Developer API"
- [ ] Click "Enable"
- [ ] Wait for API to be enabled ✅

**Step 3: Create Service Account**
- [ ] Navigate to "APIs & Services" → "Credentials"
- [ ] Click "Create Credentials" → "Service Account"
- [ ] Service account name: "smilepile-fastlane-automation"
- [ ] Service account ID: smilepile-fastlane-automation
- [ ] Click "Create and Continue"
- [ ] Grant role: "Service Account User"
- [ ] Click "Done"
- [ ] **Document Service Account Email**: __________

**Step 4: Generate JSON Key**
- [ ] Find the service account in the list
- [ ] Click on it → "Keys" tab
- [ ] Click "Add Key" → "Create new key"
- [ ] Key type: JSON
- [ ] Click "Create"
- [ ] JSON key file downloaded ✅
- [ ] **Document filename**: __________

**Step 5: Grant Permissions in Play Console**
- [ ] Visit https://play.google.com/console
- [ ] Navigate to "Users and permissions"
- [ ] Click "Invite new users"
- [ ] Enter service account email: __________
- [ ] Grant "Release Manager" role (under App permissions)
- [ ] Click "Invite user"
- [ ] Accept invitation in email ✅

**Step 6: Store Service Account JSON Securely**
```bash
# Move JSON to android directory
mv ~/Downloads/smilepile-deployment-bb0ce47cd4d2.json ~/SmilePile/android/smilepile-deployment-bb0ce47cd4d2.json
chmod 600 ~/SmilePile/android/smilepile-deployment-bb0ce47cd4d2.json

# Verify it's in .gitignore
grep -q "smilepile-deployment-bb0ce47cd4d2.json" ~/SmilePile/android/.gitignore || \
  echo "smilepile-deployment-bb0ce47cd4d2.json" >> ~/SmilePile/android/.gitignore
```
- [ ] JSON moved to android/ directory
- [ ] File permissions: -rw------- (600) ✅
- [ ] Added to .gitignore ✅

**Checkpoint**: All credentials generated and backed up? If YES ✅, continue.

---

## Phase 5E: Security Audit (1 hour)

### Task 14: Verify No Credentials in Git

```bash
cd ~/SmilePile

# Check git status for any credential files
git status --porcelain | grep -E "(keystore|credentials|\.p8|\.json)"

# Should return NOTHING (empty result)
```
- [ ] No credential files in git status ✅

**Update .gitignore**:
```bash
# Add SmilePile-specific patterns
cat >> .gitignore << 'EOF'

# SmilePile Deployment Credentials - NEVER COMMIT
android/keystore.properties
android/play-console-credentials.json
app-store-connect-api-keys/
*.p8
*.p12
*.mobileprovision
**/keystores/*.keystore
EOF
```
- [ ] .gitignore updated ✅

**Verify .gitignore Working**:
```bash
# Test that credential files are ignored
git status --porcelain
# Verify android/play-console-credentials.json NOT shown
```
- [ ] Credentials properly ignored ✅

### Task 15: Verify File Permissions

```bash
# All credential files must be readable only by owner (600)
chmod 600 ~/app-store-connect-api-keys/*.p8
chmod 600 ~/SmilePile/android/play-console-credentials.json
chmod 600 ~/keystores/*.keystore

# Verify permissions
ls -la ~/app-store-connect-api-keys/
ls -la ~/SmilePile/android/play-console-credentials.json
ls -la ~/keystores/

# All should show: -rw------- (600)
```
- [ ] All credential files have 600 permissions ✅

### Task 16: Git History Audit (Optional but Recommended)

```bash
# Search for any accidentally committed secrets in history
git log --all --full-history --source -- "*.keystore"
git log --all --full-history --source -- "*.p8"
git log --all --full-history --source -- "*credentials.json"

# Should return nothing if repo is clean
```
- [ ] No secrets found in git history ✅
- [ ] If secrets found: STOP and address immediately ⚠️

**Checkpoint**: All security checks passed? If YES ✅, continue.

---

## Phase 5F: Documentation (1 hour)

### Task 17: Document Credential Locations

Create a secure document in your password manager with this information:

**Apple Credentials**:
- Apple Developer Team ID: 84W9WSYQQB
- App Store Connect App ID: __________
- API Key ID: __________
- API Key Issuer ID: __________
- API Key File Path: ~/app-store-connect-api-keys/AuthKey_XXXXXXXXXX.p8
- TestFlight Internal Group: __________
- TestFlight External Group: __________

**Android Credentials**:
- Play Console Package Name: com.smilepile
- Play Console App ID: __________
- Keystore Path: ~/keystores/smilepile-upload.keystore
- Keystore Alias: smilepile-upload
- Keystore Password: [STORED IN PASSWORD MANAGER]
- Keystore SHA-256: __________
- Service Account Email: __________
- Service Account JSON Path: ~/SmilePile/android/play-console-credentials.json
- Internal Testing Track: __________
- Closed Testing Track: __________

**Backup Locations**:
- Keystore Backup 1: ~/Library/Mobile Documents/com~apple~CloudDocs/SmilePile-Credentials/
- Keystore Backup 2: __________
- Keystore Backup 3: __________
- Last Backup Verification: __________ (update quarterly)

- [ ] Credential documentation complete ✅
- [ ] Stored securely in password manager ✅
- [ ] Team members know how to access (if applicable) ✅

### Task 18: Update Implementation Results

Update `/wave-evidence/wave-1/06-implementation-results.md` with:
- Actual completion times for each phase
- Any issues encountered and resolutions
- Actual app IDs, key IDs, and identifiers
- Confirmation that all validation items are complete

- [ ] Implementation results updated with actual data ✅

---

## Final Validation Checklist

### Accounts (8 items)
- [ ] Apple Developer account verified (Team 84W9WSYQQB)
- [ ] App Store Connect access confirmed
- [ ] Google Play Console access confirmed
- [ ] SmilePile app created in App Store Connect
- [ ] SmilePile app created in Play Console
- [ ] TestFlight Internal Testing group configured
- [ ] TestFlight External Testing group ready
- [ ] Play Console testing tracks configured

### Credentials (13 items)
- [ ] App Store Connect API key generated
- [ ] API key stored with 600 permissions
- [ ] API key backed up securely
- [ ] Android keystore generated (4096-bit RSA)
- [ ] Keystore password: 20+ chars, strong ✅
- [ ] Keystore backed up to 3+ locations
- [ ] All keystore backups verified with SHA-256
- [ ] Keystore restoration tested from Backup 1
- [ ] Play Console service account created
- [ ] Service account JSON generated
- [ ] Service account granted "Release Manager" role
- [ ] Service account JSON stored with 600 permissions
- [ ] Service account JSON added to .gitignore

### Security (8 items)
- [ ] No credentials in git status
- [ ] .gitignore updated with SmilePile patterns
- [ ] All credential files have 600 permissions
- [ ] Git history audit clean (no secrets)
- [ ] Keystore backup restoration tested
- [ ] All checksums verified
- [ ] Team access documented
- [ ] Emergency procedures documented

### Documentation (6 items)
- [ ] All app IDs documented
- [ ] All key IDs documented
- [ ] Credential locations documented in password manager
- [ ] Backup locations documented
- [ ] Implementation results updated
- [ ] Team can access documentation

### Testing (pending Phase 6)
- [ ] App Store Connect API key tested with fastlane
- [ ] Play Console service account tested with fastlane
- [ ] Keystore restoration tested from all backup locations
- [ ] TestFlight access verified
- [ ] Play Console testing access verified

---

## Completion Summary

**Total Items**: 46 validation items
**Completed**: _____ / 46
**Status**:
- [ ] 🟢 ALL COMPLETE (46/46) - Ready for Phase 6
- [ ] 🟡 IN PROGRESS (< 46/46) - Continue working
- [ ] 🔴 BLOCKED - Document blocker: __________

**Timeline**:
- Started: 2025-10-14
- Estimated completion: __________
- Actual completion: __________
- Total time: _____ hours

**Next Steps**:
- [ ] All validation items complete → Proceed to Phase 6 (Testing)
- [ ] Blockers encountered → Document and resolve
- [ ] Questions or issues → Review `/wave-evidence/wave-1/06-implementation-results.md` for detailed procedures

---

## Troubleshooting

**Issue**: Cannot access Apple Developer account
**Solution**: Verify you're logged in with correct Apple ID for Team 84W9WSYQQB

**Issue**: Google Play Console registration pending
**Solution**: Wait 1-2 days for approval, check email for confirmation

**Issue**: Keystore generation fails
**Solution**: Verify Java JDK installed: `java -version` (need Java 8+)

**Issue**: Service account permissions not working
**Solution**: Verify "Release Manager" role granted in Play Console → Users and permissions

**Issue**: TestFlight groups not appearing
**Solution**: Refresh page, may take a few minutes to appear after creation

---

**Document Status**: 🚧 IN PROGRESS
**Last Updated**: 2025-10-14
**Next Review**: After Phase 6 (Testing) completion
