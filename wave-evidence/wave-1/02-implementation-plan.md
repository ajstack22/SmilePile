# Wave 1 Implementation Plan - Foundation & Account Setup

**Story**: STORY-6.1-foundation-setup.md
**Wave**: 1 of 10
**Timeline**: 5-7 calendar days, 8-12 hours active work
**Status**: Ready to Execute
**Owner**: Adam Stack (primary)

---

## Overview

This plan establishes all foundational accounts, certificates, and credentials required for the 4-tier deployment system (QUAL → STAGE → BETA → PROD). No shortcuts on security. Triple backup redundancy for all critical assets.

---

## 1. Prerequisites Verification

### Day 1 - Hour 1: Account Access Verification

**Apple Developer Account**:
```bash
# Verify Team ID is accessible
# Navigate to: https://developer.apple.com/account
# Expected Team ID: 84W9WSYQQB
# Expected Team Name: Adam Stack
```

**Google Account**:
```bash
# Verify Google account for Play Console
# Check if account exists: https://play.google.com/console
# If not exists: Prepare $25 USD payment method (one-time fee)
```

**Password Manager**:
- Verify access to secure password manager (1Password, Bitwarden, etc.)
- Prepare to store: API keys, keystore passwords, service account JSONs
- Enable 2FA if not already enabled

**Backup Storage**:
- External encrypted drive (physical backup)
- Cloud storage with encryption (iCloud/Google Drive)
- Team shared vault (password manager with team features)

**Development Environment**:
```bash
# Verify Xcode installation
xcodebuild -version
# Expected: Xcode 14.0+ on macOS

# Verify Android tooling
java -version  # Java 17+
keytool -help  # Java keytool for keystore generation

# Verify git is clean
cd /Users/adamstack/SmilePile
git status
# Expected: No uncommitted secrets
```

---

## 2. Apple Setup (Days 1-3)

### Day 1 - Hour 2: App Store Connect Configuration

**Step 1: Verify Developer Account**
```
URL: https://developer.apple.com/account
Action: Log in with Apple ID
Verify: Team ID shows 84W9WSYQQB
Verify: Membership status is Active
Verify: Expiration date documented (renews annually, $99/year)
```

**Step 2: Create App in App Store Connect**
```
URL: https://appstoreconnect.apple.com/apps
Navigation: My Apps → + (Plus icon) → New App

Form Fields:
- Platform: iOS
- App Name: SmilePile
- Primary Language: English (U.S.)
- Bundle ID: com.smilepile (select from dropdown)
- SKU: smilepile-ios-001
- User Access: Full Access

Click: Create
Expected Result: App page opens with "Prepare for Submission" status
```

**Step 3: Configure App Metadata**
```
Navigation: App Information (sidebar)

Required Fields:
- Category: Primary = Photo & Video, Secondary = Utilities
- Content Rights: [Your Name] owns exclusive rights
- Age Rating: Complete questionnaire (likely 4+)
- App Privacy: Data types collected (document for Wave 10)

Save Changes
```

### Day 1 - Hour 3: TestFlight Configuration

**Internal Testing Group**
```
Navigation: TestFlight → iOS → Internal Testing

Action: Create Group
Group Name: SmilePile Internal Team
Auto-distribute builds: Enabled
Add Testers:
  - Adam Stack (primary)
  - [Backup team member email]

Expected Result: Group created, testers invited via email
Note: Testers must accept invite (check email)
```

**External Testing Group**
```
Navigation: TestFlight → iOS → External Testing

Action: Create Group
Group Name: SmilePile Beta Testers
Public Link: Disabled (use invite-only initially)
Auto-distribute builds: Disabled (manual control)
Add Testers: (can be added later before Wave 8)

Test Information (required before first external build):
- Beta App Description: [Prepare 1-2 sentences]
- Feedback Email: [Support email address]
- What to Test: [Testing instructions - prepare for Wave 8]

Expected Result: Group configured, ready for first build submission
```

### Day 2 - Hour 4: App Store Connect API Key

**Generate API Key**
```
URL: https://appstoreconnect.apple.com/access/api
Navigation: Users and Access → Keys (tab)

Action: Generate API Key
Key Name: SmilePile Fastlane Automation
Access: App Manager (minimum required role)
  Alternative: Admin (if App Manager insufficient)

Click: Generate

IMMEDIATELY DOWNLOAD:
File: AuthKey_XXXXXXXXXX.p8
Document Key ID: (e.g., ABC123XYZ4)
Document Issuer ID: (UUID format, shown on Keys page)

Storage Path: ~/app-store-connect-api-keys/SmilePile-AuthKey.p8
Permissions: chmod 600 ~/app-store-connect-api-keys/SmilePile-AuthKey.p8
```

**Backup API Key**
```bash
# Backup 1: Password manager
# Store as secure note with:
#   - Key ID
#   - Issuer ID
#   - File contents (base64 encoded if needed)

# Backup 2: Encrypted cloud storage
cp ~/app-store-connect-api-keys/SmilePile-AuthKey.p8 \
   ~/iCloud/SmilePile-Credentials/AuthKey.p8.backup

# Backup 3: External drive
cp ~/app-store-connect-api-keys/SmilePile-AuthKey.p8 \
   /Volumes/EncryptedBackup/SmilePile/AuthKey.p8
```

**Update secrets file**
```bash
# Create or update deploy/secrets/production.env
cat >> /Users/adamstack/SmilePile/deploy/secrets/production.env <<EOF
# App Store Connect API Key
ASC_KEY_ID=ABC123XYZ4
ASC_ISSUER_ID=XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
ASC_KEY_PATH=~/app-store-connect-api-keys/SmilePile-AuthKey.p8
EOF

chmod 600 /Users/adamstack/SmilePile/deploy/secrets/production.env
```

### Day 3 - Hour 5: Provisioning Profiles Verification

**Check Code Signing Identities**
```bash
# Verify distribution certificate exists
security find-identity -v -p codesigning

# Expected output includes:
# 1) BEF0174CA3AF3F07F3061DAC7B49E7AAE8497F21 "Apple Distribution: Adam Stack (84W9WSYQQB)"
# 2) 93C263A54CA2D345807A56347A94249952B7BDF6 "Apple Development: Adam Stack (95FF8KMNS4)"

# Document certificate expiration dates
security find-certificate -c "Apple Distribution: Adam Stack" -p | \
  openssl x509 -noout -dates
```

**Automatic Signing Configuration**
```bash
# Verify Xcode project uses automatic signing
cd /Users/adamstack/SmilePile/ios
grep -A 5 "CODE_SIGN_STYLE" SmilePile.xcodeproj/project.pbxproj

# Expected: Automatic signing enabled
# Xcode will generate provisioning profiles automatically
# No manual profile download required
```

---

## 3. Google Setup (Days 1-3)

### Day 1 - Hour 1: Play Console Account Setup

**Create or Verify Account**
```
URL: https://play.google.com/console/signup
Note: Use Google account for business/development

If New Account:
  - Complete registration form
  - Pay $25 USD one-time fee
  - Expected approval: 1-2 business days
  - Check email for approval notification

If Existing Account:
  - Verify access to console
  - Proceed to app creation
```

### Day 2 - Hour 6: Create App in Play Console

**Create Application** (after account approved)
```
URL: https://play.google.com/console/developers/
Navigation: All apps → Create app

Form Fields:
- App name: SmilePile
- Default language: English (United States)
- App or game: App
- Free or paid: Free
- Declarations:
  ☑ Developer Program Policies
  ☑ US export laws

Click: Create app
Expected Result: App dashboard opens
```

**Configure App Details**
```
Navigation: Dashboard → Set up your app

Store Presence:
- App category: Photography
- Store listing contact: [Support email]

App Access:
- All functionality available without restrictions: Yes

Ads:
- Contains ads: No (or Yes if applicable)

Content Rating:
- Complete questionnaire (Wave 10)

Target Audience:
- Target age: All ages (or specific if Kids Mode requires)

Save Draft
```

### Day 2 - Hour 7: Configure Testing Tracks

**Internal Testing Track**
```
Navigation: Testing → Internal testing

Action: Create new release (placeholder)
Release Name: Internal Testing - Initial Setup
Countries: United States (expand later)

Testers:
- Create email list: "Internal Testers"
- Add emails:
  - [Adam Stack email]
  - [Backup team member email]

Do NOT upload build yet (no build ready)
Save as Draft

Expected Result: Track configured, ready for first build
```

**Closed Testing Track**
```
Navigation: Testing → Closed testing

Action: Create new release (placeholder)
Release Name: Closed Beta Testing
Countries: All countries

Testers:
- Create email list: "Beta Testers"
- No emails yet (add before Wave 8)

Save as Draft

Expected Result: Track configured, ready for beta builds
```

**Open Testing Track**
```
Navigation: Testing → Open testing

Action: Review configuration (DO NOT activate)
Note: Will be used for public beta (optional, Wave 9+)
No action required in Wave 1
```

### Day 3 - Hour 8: Play App Signing Enrollment

**Enroll in Play App Signing** (CRITICAL)
```
Navigation: Release → Setup → App signing

Status: Check if already enrolled
If NOT enrolled:

Option 1 (Recommended): Upload existing keystore
  - Generate keystore first (see Section 4)
  - Upload: smilepile-production.keystore
  - Google encrypts and stores production key
  - You keep upload key for future releases

Option 2: Let Google generate key
  - Google creates and manages production key
  - You download upload key
  - Easier but less control

Recommendation: Option 1 for full control

After enrollment:
  - Document App signing certificate fingerprints:
    - SHA-1: [shown in console]
    - SHA-256: [shown in console]
  - Store in password manager
```

### Day 3 - Hour 9: Service Account Creation

**Enable Google Play Developer API**
```
URL: https://console.cloud.google.com/apis/library
Navigation: Select or create Google Cloud project

Project Setup:
- Create new project: "SmilePile Play Console API"
- Enable API: Search "Google Play Android Developer API"
- Click: Enable

Expected: API enabled, ready for service account
```

**Create Service Account**
```
URL: https://console.cloud.google.com/iam-admin/serviceaccounts
Navigation: IAM & Admin → Service Accounts

Action: Create Service Account
Service account name: smilepile-play-console-deploy
Service account ID: smilepile-play-console-deploy
Description: Fastlane automation for Play Console uploads

Click: Create and Continue

Grant role: (skip for now, grant in Play Console)

Click: Done
```

**Generate Service Account JSON Key**
```
Navigation: Service Accounts → smilepile-play-console-deploy

Action: Keys → Add Key → Create new key
Key type: JSON

Click: Create
Download: smilepile-play-console-XXXXXXX.json

IMMEDIATELY:
- Rename: play-console-service-account.json
- Move to: ~/play-console-credentials/
- Permissions: chmod 600 ~/play-console-credentials/play-console-service-account.json
```

**Grant Service Account Access in Play Console**
```
URL: https://play.google.com/console/developers/
Navigation: Users and permissions → Invite new users

Email: smilepile-play-console-deploy@PROJECT_ID.iam.gserviceaccount.com
Role: Release manager (for deployment automation)
  Permissions includes: Manage releases, manage testing tracks

App access: SmilePile (select app)

Click: Invite user

Expected Result: Service account can upload builds via API
```

**Store Service Account Credentials**
```bash
# Update deploy/secrets/production.env
cat >> /Users/adamstack/SmilePile/deploy/secrets/production.env <<EOF

# Play Console Service Account
PLAY_CONSOLE_JSON_PATH=~/play-console-credentials/play-console-service-account.json
PLAY_CONSOLE_PACKAGE_NAME=com.smilepile
EOF

# Backup service account JSON
cp ~/play-console-credentials/play-console-service-account.json \
   ~/iCloud/SmilePile-Credentials/play-console-service-account.json.backup

cp ~/play-console-credentials/play-console-service-account.json \
   /Volumes/EncryptedBackup/SmilePile/play-console-service-account.json
```

---

## 4. Android Keystore Generation (Day 3) - CRITICAL

### Hour 10: Production Keystore

**CRITICAL: Keystore loss = cannot update app in Play Store**

**Generate Production Keystore**
```bash
# Create keystores directory
mkdir -p ~/keystores
chmod 700 ~/keystores

# Generate production keystore (HIGHEST SECURITY)
keytool -genkeypair -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -alias smilepile-release \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass "$(openssl rand -base64 32)" \
  -keypass "$(openssl rand -base64 32)"

# Interactive prompts (answer carefully):
# What is your first and last name? [Your Name or Company Name]
# What is the name of your organizational unit? [Development Team]
# What is the name of your organization? [SmilePile]
# What is the name of your City or Locality? [City]
# What is the name of your State or Province? [State]
# What is the two-letter country code? [US]

# STOP: DO NOT PROCEED UNTIL PASSWORDS ARE SECURELY STORED
```

**IMMEDIATELY Store Passwords**
```bash
# Extract passwords from previous command output
# OR use this secure method:

# Generate strong passwords
STORE_PASS=$(openssl rand -base64 32)
KEY_PASS=$(openssl rand -base64 32)

# Regenerate with known passwords
keytool -genkeypair -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -alias smilepile-release \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS"

# IMMEDIATELY store in password manager:
# Entry: SmilePile Production Keystore
#   - Keystore Password: $STORE_PASS
#   - Key Password: $KEY_PASS
#   - Alias: smilepile-release
#   - Location: ~/keystores/smilepile-production.keystore
#   - Created: 2025-10-13
#   - Expires: 2052 (27 years)

# VERIFY passwords work
keytool -list -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS"

# Expected output: Certificate fingerprints (SHA1, SHA256)
# Document these fingerprints in password manager
```

**Triple Backup Strategy**
```bash
# Backup 1: Encrypted cloud storage
cp ~/keystores/smilepile-production.keystore \
   ~/iCloud/SmilePile-Credentials/smilepile-production.keystore.backup
# OR
cp ~/keystores/smilepile-production.keystore \
   ~/Google\ Drive/SmilePile-Credentials/smilepile-production.keystore.backup

# Backup 2: External encrypted drive
cp ~/keystores/smilepile-production.keystore \
   /Volumes/EncryptedBackup/SmilePile/smilepile-production.keystore

# Backup 3: Password manager (for small files)
# Upload .keystore file as attachment to secure note

# VERIFY backups exist
ls -lh ~/iCloud/SmilePile-Credentials/smilepile-production.keystore.backup
ls -lh /Volumes/EncryptedBackup/SmilePile/smilepile-production.keystore

# VERIFY file integrity
shasum -a 256 ~/keystores/smilepile-production.keystore
shasum -a 256 ~/iCloud/SmilePile-Credentials/smilepile-production.keystore.backup
# SHA256 hashes MUST match exactly
```

### Hour 11: QUAL Keystore (Optional)

**Generate QUAL Keystore** (for local testing)
```bash
# Use weaker security for QUAL (not critical if lost)
keytool -genkeypair -v \
  -keystore ~/keystores/smilepile-qual.keystore \
  -alias smilepile-qual \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -storepass "qual_keystore_password" \
  -keypass "qual_key_password"

# Answer same organizational questions

# Store credentials (less critical than production)
# Update deploy/secrets/quality.env (create if needed)
```

**Alternative: Use Android Debug Keystore**
```bash
# Debug keystore location (auto-generated by Android Studio)
ls ~/.android/debug.keystore

# Debug keystore details (known defaults):
# Alias: androiddebugkey
# Store password: android
# Key password: android

# For QUAL builds, debug keystore is acceptable
# No need to generate separate QUAL keystore
```

### Hour 12: Create keystore.properties

**Create Android keystore configuration**
```bash
cat > /Users/adamstack/SmilePile/android/keystore.properties <<EOF
# Android Keystore Configuration
# DO NOT COMMIT TO GIT

# Production Release Keystore
release.storeFile=~/keystores/smilepile-production.keystore
release.storePassword=PRODUCTION_STORE_PASSWORD_HERE
release.keyPassword=PRODUCTION_KEY_PASSWORD_HERE
release.keyAlias=smilepile-release

# QUAL Keystore (optional, or use debug)
qual.storeFile=~/.android/debug.keystore
qual.storePassword=android
qual.keyPassword=android
qual.keyAlias=androiddebugkey
EOF

# Secure permissions
chmod 600 /Users/adamstack/SmilePile/android/keystore.properties

# VERIFY it's gitignored
cd /Users/adamstack/SmilePile
git status android/keystore.properties
# Expected: file NOT shown (ignored)

# If shown in git status, add to .gitignore:
echo "android/keystore.properties" >> .gitignore
```

**Document Keystore Metadata**
```bash
# Extract certificate fingerprints
keytool -list -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -storepass "PRODUCTION_STORE_PASSWORD" \
  | grep -A 5 "Certificate fingerprints"

# Document in password manager:
# SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
# SHA256: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX

# Verify keystore validity period
keytool -list -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -storepass "PRODUCTION_STORE_PASSWORD" \
  | grep "Valid from"

# Expected: ~27 years validity (until ~2052)
```

---

## 5. Security Audit (Day 4)

### Hour 13: Git History & .gitignore Verification

**Verify .gitignore excludes secrets**
```bash
cd /Users/adamstack/SmilePile

# Check current .gitignore includes:
grep -E "(keystore|\.jks|\.p8|\.p12|secrets/)" .gitignore

# Expected entries:
# *.jks
# *.keystore
# secrets/
# android/keystore.properties
# *.p8
# *.p12

# If missing, add them:
cat >> .gitignore <<EOF

# Additional security exclusions
android/keystore.properties
*.mobileprovision
play-console-credentials/
app-store-connect-api-keys/
EOF
```

**Audit git history for secrets**
```bash
# Search for accidentally committed keystores
git log --all --full-history --source -- "*.keystore" "*.jks"
# Expected: No results

# Search for accidentally committed API keys
git log --all --full-history --source -- "*.p8" "*.p12"
# Expected: No results

# Search for accidentally committed secrets directory
git log --all --full-history -- "deploy/secrets/*.env"
# Expected: Only example.env (if any)

# Search for password patterns in commit history
git grep -i "password\s*=" $(git rev-list --all)
# Expected: Only examples or placeholders, no real passwords

# Search for API key patterns
git grep -E "(AuthKey_|AIza|AKIA)" $(git rev-list --all)
# Expected: No results

# If any secrets found in history, IMMEDIATELY:
# 1. Rotate compromised credentials
# 2. Use git-filter-repo or BFG to remove from history
# 3. Force push cleaned history (coordinate with team)
```

**File Permissions Audit**
```bash
# Verify secrets directory permissions
ls -ld /Users/adamstack/SmilePile/deploy/secrets/
# Expected: drwx------ (700)

# Verify secrets file permissions
ls -l /Users/adamstack/SmilePile/deploy/secrets/*.env
# Expected: -rw------- (600)

# Fix if incorrect:
chmod 700 /Users/adamstack/SmilePile/deploy/secrets/
chmod 600 /Users/adamstack/SmilePile/deploy/secrets/*.env

# Verify keystore permissions
ls -l ~/keystores/*.keystore
# Expected: -rw------- (600)

chmod 600 ~/keystores/*.keystore

# Verify API key permissions
ls -l ~/app-store-connect-api-keys/*.p8
# Expected: -rw------- (600)

chmod 600 ~/app-store-connect-api-keys/*.p8
```

**Create secrets README**
```bash
cat > /Users/adamstack/SmilePile/deploy/secrets/README.md <<EOF
# Secrets Directory

This directory contains sensitive credentials and is excluded from git.

**Files in this directory**:
- \`quality.env\` - QUAL tier secrets
- \`staging.env\` - STAGE tier secrets
- \`production.env\` - PROD tier secrets
- \`example.env\` - Template (safe to commit)

**Security Requirements**:
- All files: 600 permissions (rw-------)
- Directory: 700 permissions (drwx------)
- Never commit actual credentials to git
- Store master copies in password manager

**Backup Locations**:
- Password manager: [Name of password manager]
- Encrypted cloud: ~/iCloud/SmilePile-Credentials/
- External drive: /Volumes/EncryptedBackup/SmilePile/

**Access Requests**:
Contact: Adam Stack (primary)
Emergency: [Backup team member]
EOF

# DO NOT commit secrets README if it contains sensitive paths
# OR commit only if paths are generic
```

---

## 6. Validation (Day 5)

### Hour 14: Test API Keys and Service Accounts

**Install fastlane** (temporary for testing)
```bash
# Install via Homebrew
brew install fastlane

# OR via Bundler (better for consistency)
cd /Users/adamstack/SmilePile
cat > Gemfile <<EOF
source "https://rubygems.org"

gem "fastlane"
EOF

bundle install
```

**Test App Store Connect API Key**
```bash
# Set environment variables
export ASC_KEY_ID="ABC123XYZ4"
export ASC_ISSUER_ID="XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
export ASC_KEY_PATH="~/app-store-connect-api-keys/SmilePile-AuthKey.p8"

# Test with fastlane pilot (TestFlight CLI)
fastlane pilot list \
  --api_key_path "$ASC_KEY_PATH" \
  --api_key "$ASC_KEY_ID" \
  --issuer_id "$ASC_ISSUER_ID"

# Expected output: List of TestFlight groups (or "No builds found" if no builds yet)
# Success: API key is valid and working
# Failure: Check Key ID, Issuer ID, or key file path
```

**Test Play Console Service Account**
```bash
# Set environment variables
export PLAY_CONSOLE_JSON_PATH="~/play-console-credentials/play-console-service-account.json"

# Test with fastlane supply (Play Console CLI)
cd /Users/adamstack/SmilePile/android
fastlane supply init \
  --json_key "$PLAY_CONSOLE_JSON_PATH" \
  --package_name "com.smilepile"

# Expected output: "Successfully connected to Google Play"
# Success: Service account is valid and has correct permissions
# Failure: Check JSON file path, service account permissions in Play Console
```

**Test Android Keystore**
```bash
# Build debug APK with keystore
cd /Users/adamstack/SmilePile/android
./gradlew assembleDebug

# If successful, test with actual keystore signing
# (Full signing configuration added in Wave 3)
# For now, just verify keystore is readable:

keytool -list -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -storepass "PRODUCTION_STORE_PASSWORD" \
  | head -20

# Expected: Certificate details shown
# Success: Keystore is readable and valid
```

### Hour 15: Test Keystore Restoration

**CRITICAL: Test restoration from EACH backup location**

**Test Backup 1: Cloud Storage**
```bash
# Simulate keystore loss (DO NOT delete original until verified!)
mv ~/keystores/smilepile-production.keystore ~/keystores/smilepile-production.keystore.original

# Restore from cloud backup
cp ~/iCloud/SmilePile-Credentials/smilepile-production.keystore.backup \
   ~/keystores/smilepile-production.keystore

# Verify restored keystore is identical
shasum -a 256 ~/keystores/smilepile-production.keystore
shasum -a 256 ~/keystores/smilepile-production.keystore.original
# SHA256 MUST match

# Test restored keystore works
keytool -list -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -storepass "PRODUCTION_STORE_PASSWORD"

# SUCCESS: Restore from cloud backup verified
# Restore original
mv ~/keystores/smilepile-production.keystore.original ~/keystores/smilepile-production.keystore
```

**Test Backup 2: External Drive**
```bash
# Repeat restoration test from external drive
mv ~/keystores/smilepile-production.keystore ~/keystores/smilepile-production.keystore.original

cp /Volumes/EncryptedBackup/SmilePile/smilepile-production.keystore \
   ~/keystores/smilepile-production.keystore

# Verify hash matches
shasum -a 256 ~/keystores/smilepile-production.keystore
shasum -a 256 ~/keystores/smilepile-production.keystore.original

# Test keystore works
keytool -list -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -storepass "PRODUCTION_STORE_PASSWORD"

# SUCCESS: Restore from external drive verified
# Restore original
mv ~/keystores/smilepile-production.keystore.original ~/keystores/smilepile-production.keystore
```

**Test Backup 3: Password Manager**
```bash
# Download keystore from password manager attachment
# Verify hash matches original
shasum -a 256 ~/Downloads/smilepile-production.keystore
shasum -a 256 ~/keystores/smilepile-production.keystore
# MUST match

# SUCCESS: All backup locations verified
# Delete downloaded copy
rm ~/Downloads/smilepile-production.keystore
```

**Document restoration success**
```bash
# Record restoration times in password manager:
# Backup 1 (Cloud): ~30 seconds
# Backup 2 (External Drive): ~1 minute (if drive not mounted)
# Backup 3 (Password Manager): ~2 minutes (download + verify)

# Set quarterly reminder to verify backups
# Calendar reminder: "Verify SmilePile keystore backups" (every 90 days)
```

### Hour 16: Verify Git is Clean

**Final security verification**
```bash
cd /Users/adamstack/SmilePile

# Check working directory for secrets
git status

# Expected: No .keystore, .p8, .json files shown
# Expected: deploy/secrets/*.env NOT shown (gitignored)

# Verify keystore.properties is ignored
git status android/keystore.properties
# Expected: file NOT shown

# If shown, add to .gitignore and commit .gitignore update
echo "android/keystore.properties" >> .gitignore
git add .gitignore
git commit -m "security: Ensure keystore.properties is gitignored"

# Final verification: No secrets in repository
git grep -i "password" | grep -v "example"
# Expected: No actual passwords (only examples/placeholders)

git grep -E "(AuthKey_|AIza|AKIA)"
# Expected: No results (no API keys)

# SUCCESS: Repository is clean of secrets
```

---

## 7. Risk Mitigation

### Critical Risk: Keystore Loss

**Prevention**:
- Triple backup strategy (cloud, external drive, password manager)
- Quarterly backup verification reminders
- Document backup locations in team documentation
- Use Play App Signing for Google-managed backup

**If Keystore Lost (AFTER first Play Store upload)**:
1. Check all 3 backup locations
2. If enrolled in Play App Signing: Google has production key, generate new upload key
3. If NOT enrolled: App cannot be updated, requires new Play Store listing (catastrophic)
4. Contact Google Play support if enrolled in Play App Signing

**Recovery Steps**:
```bash
# If enrolled in Play App Signing and upload key lost:
# 1. Request upload key reset from Play Console
# Navigation: Release → Setup → App signing → Request upload key reset

# 2. Generate new upload key
keytool -genkeypair -v \
  -keystore ~/keystores/smilepile-upload-new.keystore \
  -alias smilepile-upload-new \
  -keyalg RSA -keysize 4096 -validity 10000

# 3. Submit public certificate to Google
keytool -export -rfc \
  -keystore ~/keystores/smilepile-upload-new.keystore \
  -alias smilepile-upload-new \
  -file upload_certificate.pem

# 4. Upload to Play Console as instructed
# 5. Update keystore.properties with new keystore
```

### High Risk: Account Delays

**Google Play Console Approval Delay**:
- Mitigation: Submit account creation Day 1 morning
- Fallback: Use internal testing via Android Debug builds while waiting
- Communication: Notify team of potential 1-2 day delay

**Apple Developer Account Issues**:
- Mitigation: Verify access Day 1
- Fallback: Contact Apple Developer Support with Team ID 84W9WSYQQB
- Recovery time: 1-3 days if account issue

### High Risk: API Key or Credential Compromise

**Immediate Response**:
```bash
# 1. IMMEDIATELY revoke compromised credential

# App Store Connect API Key:
# - Navigate: https://appstoreconnect.apple.com/access/api
# - Revoke key: Click ... → Revoke Access
# - Generate new key with different name

# Play Console Service Account:
# - Navigate: https://console.cloud.google.com/iam-admin/serviceaccounts
# - Delete compromised key
# - Generate new JSON key

# Android Keystore:
# - If production keystore compromised: Rotate upload key (if using Play App Signing)
# - If NOT using Play App Signing: Contact Google Play support

# 2. Audit access logs
# App Store Connect: Users and Access → API Keys → [Key] → View Activity
# Play Console: (limited logging, check Cloud Console audit logs)

# 3. Update automation scripts with new credentials
# Update deploy/secrets/production.env
# Test new credentials work

# 4. Document incident
# Record: What was compromised, when, how discovered, actions taken

# 5. Review access permissions
# Ensure only authorized team members have credentials
```

### Medium Risk: Team Access

**Backup Team Member Access**:
- Ensure 2+ team members have access to all accounts
- Document: Who has access to what (App Store, Play Console, password manager)
- Emergency: Document alternative contact if primary unavailable

**Team Access Matrix** (document in password manager or team wiki):
```
Resource                    | Adam Stack | [Backup Member] | Notes
----------------------------|------------|-----------------|-------
Apple Developer Account     | Admin      | Member          | Team ID: 84W9WSYQQB
App Store Connect           | Admin      | Developer       | Can access TestFlight
Play Console                | Owner      | Developer       | Can manage releases
Password Manager            | Admin      | Member          | Shared vault
Keystore Backups            | Yes        | Yes             | Both can restore
```

---

## 8. Timeline & Milestones

**Total Timeline**: 5-7 calendar days
**Active Work**: 8-12 hours
**Waiting Period**: 1-2 days (Google Play Console approval if new account)

### Day 1 (3-4 hours active)
- ✅ Verify Apple Developer account access
- ✅ Create app in App Store Connect
- ✅ Configure TestFlight groups
- ✅ Verify/create Google Play Console account (submit if new)
- ✅ Document account details in password manager

### Day 2 (3-4 hours active)
- ⏳ Wait for Play Console approval (if new account)
- ✅ Generate App Store Connect API key
- ✅ Backup API key to 3 locations
- ✅ Create app in Play Console (if approved)
- ✅ Configure testing tracks

### Day 3 (4-5 hours active)
- ✅ Enroll in Play App Signing
- ✅ Create Play Console service account
- ✅ Grant service account permissions
- ✅ Generate Android production keystore (CRITICAL)
- ✅ IMMEDIATELY backup keystore to 3 locations
- ✅ Generate QUAL keystore (optional)
- ✅ Create keystore.properties file
- ✅ Document keystore metadata

### Day 4 (1-2 hours active)
- ✅ Update .gitignore
- ✅ Audit git history for secrets
- ✅ Verify file permissions
- ✅ Create secrets README
- ✅ Document credential locations

### Day 5 (1-2 hours active)
- ✅ Install fastlane (temporary)
- ✅ Test App Store Connect API key
- ✅ Test Play Console service account
- ✅ Test keystore restoration from all 3 backups
- ✅ Verify git repository is clean
- ✅ Complete Wave 1 validation checklist

### Day 6-7 (Buffer)
- Resolve any issues from testing
- Complete documentation
- Team review of credentials and access
- Final security audit

---

## 9. Success Criteria Checklist

### Apple Infrastructure
- [ ] Apple Developer account confirmed active (Team ID: 84W9WSYQQB)
- [ ] App "SmilePile" created in App Store Connect (bundle: com.smilepile)
- [ ] TestFlight Internal Testing group created with 2+ testers
- [ ] TestFlight External Testing group configured
- [ ] App Store Connect API key generated (AuthKey_*.p8)
- [ ] API key backed up to 3 locations
- [ ] API key tested with `fastlane pilot list` (successful)
- [ ] Code signing identities verified in keychain

### Google Infrastructure
- [ ] Google Play Console account active and approved
- [ ] App "SmilePile" created in Play Console (package: com.smilepile)
- [ ] Internal Testing track configured
- [ ] Closed Testing track configured
- [ ] Play App Signing enrolled (production key managed by Google)
- [ ] App signing certificate fingerprints documented
- [ ] Service account created with Release Manager role
- [ ] Service account JSON backed up to 3 locations
- [ ] Service account tested with `fastlane supply init` (successful)

### Android Keystore Security
- [ ] Production keystore generated (4096-bit RSA, 25+ year validity)
- [ ] Keystore password 20+ characters with special characters
- [ ] Keystore alias: smilepile-release
- [ ] Keystore backed up to Location 1: Encrypted cloud storage
- [ ] Keystore backed up to Location 2: External encrypted drive
- [ ] Keystore backed up to Location 3: Password manager vault
- [ ] Backup restoration tested from Location 1 (successful)
- [ ] Backup restoration tested from Location 2 (successful)
- [ ] Backup restoration tested from Location 3 (successful)
- [ ] Keystore SHA256 fingerprint documented
- [ ] keystore.properties file created with correct format
- [ ] keystore.properties verified gitignored

### Credential Management
- [ ] deploy/secrets/quality.env created (if needed for QUAL)
- [ ] deploy/secrets/staging.env created (for STAGE)
- [ ] deploy/secrets/production.env created (for PROD)
- [ ] All secrets files have 600 permissions
- [ ] deploy/secrets/ directory has 700 permissions
- [ ] All credentials stored in password manager with 2FA enabled
- [ ] Team access matrix documented (who has access to what)
- [ ] Emergency access procedures documented

### Security Audit
- [ ] .gitignore updated with all secret patterns
- [ ] Git history audited for committed secrets (none found)
- [ ] android/keystore.properties verified gitignored
- [ ] No .keystore files in git status
- [ ] No .p8 files in git status
- [ ] No service account JSON in git status
- [ ] File permissions verified (600 for secrets, 700 for directories)
- [ ] Repository clean verification: `git grep -i password` (no real passwords)

### Documentation
- [ ] Keystore metadata documented (alias, validity, fingerprints)
- [ ] Backup locations documented in secure team documentation
- [ ] API key details documented (Key ID, Issuer ID, storage path)
- [ ] Service account details documented (email, role, JSON path)
- [ ] Team access matrix created
- [ ] Emergency recovery procedures documented
- [ ] Quarterly backup verification reminder set

### Validation
- [ ] App Store Connect login successful (Adam Stack)
- [ ] App Store Connect login successful (backup team member)
- [ ] Play Console login successful (Adam Stack)
- [ ] Play Console login successful (backup team member)
- [ ] TestFlight Internal group visible and testers added
- [ ] TestFlight External group visible and configured
- [ ] Play Console Internal track accessible
- [ ] Play Console Closed Testing track accessible
- [ ] `fastlane pilot list` command successful (iOS API key works)
- [ ] `fastlane supply init` command successful (Android service account works)
- [ ] Keystore signing verified (keytool -list works with passwords)

---

## 10. Post-Wave 1 Handoff

### Deliverables
1. **Accounts**: App Store Connect and Play Console apps created and configured
2. **Credentials**: All API keys, service accounts, and keystores generated and backed up
3. **Documentation**: Credential locations, backup procedures, team access documented
4. **Validation**: All credentials tested and verified working

### Next Steps
- **Wave 2**: iOS tier configuration (xcconfig files, schemes)
- **Wave 3**: Android tier configuration (product flavors, signing configs)
- **Wave 4**: JavaScript BUILD_TYPE integration
- **Wave 5**: Fastlane automation setup

### Blockers Removed
- ✅ Can proceed with iOS TestFlight builds (API key ready)
- ✅ Can proceed with Android signing configuration (keystore ready)
- ✅ Can proceed with Play Console uploads (service account ready)
- ✅ No external dependencies blocking Wave 2-10

### Team Communication
- Share credential locations with authorized team members
- Schedule backup verification quarterly
- Document any issues encountered during Wave 1
- Review security procedures with team

---

## Appendix A: Quick Reference Commands

### Keystore Management
```bash
# List keystore contents
keytool -list -v -keystore ~/keystores/smilepile-production.keystore

# Extract SHA256 fingerprint
keytool -list -v -keystore ~/keystores/smilepile-production.keystore | grep SHA256

# Verify keystore password
keytool -list -keystore ~/keystores/smilepile-production.keystore -storepass "PASSWORD"

# Export certificate
keytool -export -rfc -keystore ~/keystores/smilepile-production.keystore \
  -alias smilepile-release -file certificate.pem
```

### Git Security Audit
```bash
# Search for keystore files in history
git log --all --full-history --source -- "*.keystore" "*.jks"

# Search for API keys in history
git log --all --full-history --source -- "*.p8" "*.p12"

# Search for passwords in all commits
git grep -i "password" $(git rev-list --all)

# Verify file is gitignored
git check-ignore -v android/keystore.properties
```

### Backup Verification
```bash
# Compare file hashes
shasum -a 256 ~/keystores/smilepile-production.keystore
shasum -a 256 ~/iCloud/SmilePile-Credentials/smilepile-production.keystore.backup

# List all backup locations
ls -lh ~/keystores/smilepile-production.keystore
ls -lh ~/iCloud/SmilePile-Credentials/smilepile-production.keystore.backup
ls -lh /Volumes/EncryptedBackup/SmilePile/smilepile-production.keystore
```

### Fastlane Testing
```bash
# Test iOS API key
fastlane pilot list \
  --api_key_path "$ASC_KEY_PATH" \
  --api_key "$ASC_KEY_ID" \
  --issuer_id "$ASC_ISSUER_ID"

# Test Android service account
fastlane supply init \
  --json_key "$PLAY_CONSOLE_JSON_PATH" \
  --package_name "com.smilepile"
```

---

## Appendix B: Troubleshooting

### Issue: Apple API Key Not Working
```bash
# Verify file exists and is readable
ls -lh ~/app-store-connect-api-keys/SmilePile-AuthKey.p8

# Verify Key ID and Issuer ID are correct
# Navigation: https://appstoreconnect.apple.com/access/api
# Compare Key ID and Issuer ID with what's in deploy/secrets/production.env

# Verify API key has correct permissions
# Role must be: App Manager or Admin

# Test with explicit parameters
fastlane pilot list \
  --api_key_path "$(pwd)/SmilePile-AuthKey.p8" \
  --api_key "KEY_ID" \
  --issuer_id "ISSUER_ID" \
  --verbose
```

### Issue: Play Console Service Account Not Working
```bash
# Verify JSON file is valid
cat ~/play-console-credentials/play-console-service-account.json | jq .

# Verify service account email
cat ~/play-console-credentials/play-console-service-account.json | jq -r .client_email

# Verify service account has permissions in Play Console
# Navigation: https://play.google.com/console/developers/
# Users and permissions → Check service account is listed with Release Manager role

# Test with explicit parameters
fastlane supply init \
  --json_key "$(pwd)/play-console-service-account.json" \
  --package_name "com.smilepile" \
  --verbose
```

### Issue: Keystore Password Not Working
```bash
# Verify password stored correctly in password manager
# Copy password to clipboard and test:

keytool -list -keystore ~/keystores/smilepile-production.keystore \
  -storepass "PASTE_PASSWORD_HERE"

# If password is incorrect:
# - Check password manager for typos
# - Verify correct entry (production vs qual)
# - Try restoring from backup location
# - If all backups fail: CRITICAL ISSUE, escalate immediately
```

### Issue: Git Showing Secrets in Status
```bash
# Verify .gitignore includes pattern
cat .gitignore | grep "keystore.properties"

# Add if missing
echo "android/keystore.properties" >> .gitignore

# Check if file is already tracked
git ls-files | grep "keystore.properties"

# If tracked, remove from tracking (but keep local file)
git rm --cached android/keystore.properties
git add .gitignore
git commit -m "security: Remove keystore.properties from git tracking"
```

---

**Wave 1 Implementation Plan Complete**
**Ready for Execution**: Yes
**Estimated Completion**: Day 5-7
**Blockers**: None identified
**Next Wave**: iOS xcconfig configuration (Wave 2)
