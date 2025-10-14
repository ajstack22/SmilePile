# Wave 1 Implementation Results - Foundation Setup

**Story**: STORY-6.1-foundation-setup.md
**Wave**: 1 of 10
**Status**: MANUAL IMPLEMENTATION REQUIRED
**Date**: 2025-10-14
**Implementation Type**: Administrative (Browser-based Account Setup)

---

## Executive Summary

This document provides a comprehensive guide for completing Wave 1 Foundation Setup. Since this wave consists primarily of **administrative tasks requiring browser access and manual account interactions**, this document serves as a detailed handoff guide for human execution.

**Key Context**: SmilePile inherits StackMap's proven deployment infrastructure, including:
- Active Apple Developer Account (Team 84W9WSYQQB)
- Active Google Play Console Account
- Proven macOS Keychain security pattern (CRITICAL-06 resolved)

**Implementation Approach**: Follow StackMap's proven patterns, adapted for SmilePile.

---

## Implementation Status Overview

### What This Document Provides

1. **Detailed Step-by-Step Procedures** - Exact instructions for all manual setup tasks
2. **Verification Checklists** - How to validate each step was completed correctly
3. **Security Best Practices** - Based on StackMap's proven patterns
4. **Troubleshooting Guides** - Common issues and resolutions
5. **Handoff Documentation Templates** - What to document for team knowledge

### What Requires Human Action

All tasks in this wave require human interaction:
- Browser-based account configuration
- Manual credential generation
- Password manager data entry
- Physical backup verification

**Estimated Time**: 8-12 hours active work over 5-7 calendar days

---

## Phase 5A: Account Verification (30 minutes)

### Task 5A-1: Verify Apple Developer Account Access

**Objective**: Confirm access to Apple Developer Account and document status.

**Procedure**:
```
1. Navigate to: https://developer.apple.com/account
2. Sign in with Apple ID credentials
3. Verify the following information:
   - Team ID: 84W9WSYQQB (must match exactly)
   - Team Name: Adam Stack (or organization name)
   - Membership Status: Active
   - Membership Type: Apple Developer Program
   - Expiration Date: [Document the date]
4. Take screenshot of account overview page
5. Save screenshot as: ~/SmilePile-Deployment/screenshots/apple-developer-status.png
```

**Verification**:
- [ ] Can access developer.apple.com/account
- [ ] Team ID matches 84W9WSYQQB
- [ ] Membership shows "Active"
- [ ] Expiration date is documented
- [ ] Screenshot saved for records

**Expected Result**: Access confirmed, account active, expiration documented.

**If Issues Occur**:
- Access Denied: Check Apple ID credentials, verify account membership
- Expired Membership: Renew membership ($99/year)
- Wrong Team: Verify logged in with correct Apple ID

---

### Task 5A-2: Verify App Store Connect Access

**Objective**: Confirm access to App Store Connect for app management.

**Procedure**:
```
1. Navigate to: https://appstoreconnect.apple.com
2. Sign in with same Apple ID
3. Verify you can see:
   - "My Apps" section
   - Team selection dropdown (should show Team 84W9WSYQQB)
   - Access to "Users and Access" section
4. Navigate to: Users and Access → Keys (API Keys tab)
5. Verify you have permission to create API keys
6. Take screenshot of main dashboard
7. Save screenshot as: ~/SmilePile-Deployment/screenshots/app-store-connect-access.png
```

**Verification**:
- [ ] Can access appstoreconnect.apple.com
- [ ] "My Apps" section visible
- [ ] Can navigate to Users and Access
- [ ] Can view Keys section (API key management)
- [ ] Screenshot saved

**Expected Result**: Full access to App Store Connect, can manage apps and API keys.

**If Issues Occur**:
- Access Denied: Check account role, may need Account Holder to grant access
- Limited Access: Verify you have "Admin" or "App Manager" role
- Can't See Keys: Need admin privileges to manage API keys

---

### Task 5A-3: Verify Google Play Console Access

**Objective**: Confirm access to Google Play Console or identify if account creation needed.

**Procedure**:
```
1. Navigate to: https://play.google.com/console
2. Sign in with Google account
3. Determine current status:

   SCENARIO A: Account Already Exists (StackMap's account)
   - Can see "All apps" dashboard
   - Document: Account email, organization name
   - Verify: Payment status is "Active"
   - Take screenshot of console dashboard

   SCENARIO B: Account Needs Creation
   - Redirected to account creation page
   - Note: $25 USD one-time registration fee required
   - Note: 1-2 day approval time after payment
   - DO NOT proceed with account creation yet (coordinate timing)
4. Save screenshot as: ~/SmilePile-Deployment/screenshots/play-console-access.png
```

**Verification**:
- [ ] Can access play.google.com/console
- [ ] Account status determined (existing or needs creation)
- [ ] If existing: Payment status verified
- [ ] Screenshot saved

**Expected Result**: Access status confirmed, next steps identified.

**If Issues Occur**:
- Account Suspended: Contact Google Play support
- Payment Failed: Update payment method
- Access Denied: Verify correct Google account, check permissions

---

### Documentation Template: Account Status

Create file: `~/SmilePile-Deployment/docs/account-status.md`

```markdown
# SmilePile Account Status - Wave 1

**Date Verified**: 2025-10-14

## Apple Developer Account
- **Account Email**: [your-email@domain.com]
- **Team ID**: 84W9WSYQQB
- **Team Name**: [Team Name]
- **Membership Status**: Active
- **Membership Type**: Apple Developer Program
- **Expiration Date**: [YYYY-MM-DD]
- **Annual Renewal Cost**: $99 USD
- **Access Level**: [Admin/Account Holder]

## App Store Connect
- **Access Confirmed**: Yes
- **Can Create Apps**: Yes
- **Can Manage API Keys**: Yes
- **Can Manage TestFlight**: Yes

## Google Play Console
- **Account Status**: [Existing/Needs Creation]
- **Account Email**: [google-account@domain.com]
- **Organization Name**: [If applicable]
- **Payment Status**: [Active/Pending]
- **Registration Fee Paid**: [Yes/No]
- **Approval Status**: [Approved/Pending]

## Next Steps
- [ ] Create SmilePile app in App Store Connect
- [ ] Create SmilePile app in Play Console (after approval if new)
- [ ] Configure testing tracks
- [ ] Generate credentials
```

---

## Phase 5B: App Creation (1-2 hours)

### Task 5B-1: Create SmilePile App in App Store Connect

**Objective**: Create new app listing for SmilePile in App Store Connect.

**Prerequisites**:
- ✅ App Store Connect access verified
- ✅ Bundle ID decided: `com.smilepile.SmilePile` (or `com.smilepile`)

**Procedure**:
```
1. Navigate to: https://appstoreconnect.apple.com/apps
2. Click: "My Apps" (left sidebar)
3. Click: "+" button (top-left corner) → "New App"

4. Fill in App Information form:

   Platform: iOS

   App Name: SmilePile
   (Note: Must be unique in App Store, will show to users)

   Primary Language: English (U.S.)

   Bundle ID:
   - If "com.smilepile" exists in dropdown: Select it
   - If not exists: Click "Register new Bundle ID"
     → Navigate to developer.apple.com
     → Certificates, IDs & Profiles
     → Identifiers → "+" button
     → App IDs → Continue
     → Description: SmilePile
     → Bundle ID: com.smilepile
     → Capabilities: (none needed for now)
     → Register
     → Return to App Store Connect
     → Refresh Bundle ID dropdown
     → Select com.smilepile

   SKU: com.smilepile.ios
   (Internal identifier, not visible to users)

   User Access: Full Access
   (All team members can access)

5. Click: "Create"

6. Wait for app creation (usually instant)

7. You should see: App page with "Prepare for Submission" status

8. Take screenshot of app page
9. Save screenshot as: ~/SmilePile-Deployment/screenshots/app-store-connect-app-created.png
```

**Verification**:
- [ ] App appears in "My Apps" list
- [ ] App name is "SmilePile"
- [ ] Bundle ID is correct (com.smilepile or com.smilepile.SmilePile)
- [ ] Status shows "Prepare for Submission"
- [ ] Screenshot saved

**Expected Result**: SmilePile app created, ready for metadata configuration.

**If Issues Occur**:
- Bundle ID Taken: Use alternative like com.smilepile.app or com.yourdomain.smilepile
- Name Conflict: App Store may require different public name if "SmilePile" taken
- Permission Denied: Verify account role has app creation permissions

---

### Task 5B-2: Configure Basic App Metadata (App Store Connect)

**Objective**: Set up minimum required metadata for app.

**Procedure**:
```
1. From SmilePile app page, click: "App Information" (left sidebar)

2. Set Primary Category:
   - Primary Category: Photo & Video
   - Secondary Category: Utilities (optional)

3. Set Content Rights:
   - Select: "No, it does not contain, show, or access third-party content"
   - OR if applicable: Select appropriate option and provide attribution

4. Click: "Save" (top-right)

5. Navigate to: "Pricing and Availability"
   - Price: Free (select from dropdown)
   - Availability: All countries/regions (default)
   - Click: "Save"

6. Navigate to: "Age Rating"
   - Click: "Edit" next to Age Rating
   - Complete questionnaire:
     * Unrestricted Web Access: No
     * Gambling and Contests: No
     * Made For Kids: No (unless applicable)
     * [Answer all questions based on app features]
   - Click: "Done"
   - Note the assigned rating (likely 4+)

7. Take screenshots of each configured section

8. Navigate to: "App Privacy" (will configure in Wave 10, but note it's required)
   - Status: "Not Started" is okay for now
   - This will be completed before first production submission
```

**Verification**:
- [ ] Category set to "Photo & Video"
- [ ] Price set to "Free"
- [ ] Age rating assigned
- [ ] App Information saved successfully

**Expected Result**: Basic metadata configured, app ready for builds.

---

### Task 5B-3: Create SmilePile App in Play Console

**Objective**: Create new app listing for SmilePile in Google Play Console.

**Prerequisites**:
- ✅ Play Console account active and approved
- ✅ Package name decided: `com.smilepile`

**Procedure**:
```
1. Navigate to: https://play.google.com/console/developers/

2. Click: "All apps" (left sidebar)

3. Click: "Create app" button (top-right)

4. Fill in App Details form:

   App name: SmilePile
   (User-facing name in Play Store)

   Default language: English (United States)

   App or game: App

   Free or paid: Free

   Declarations:
   ☑ I confirm this app complies with Google Play's Developer Program Policies
   ☑ I confirm this app complies with US export laws

   (Read and understand these policies before checking)

5. Click: "Create app"

6. Wait for app creation (usually instant)

7. You should see: App dashboard with setup checklist

8. Take screenshot of app dashboard
9. Save as: ~/SmilePile-Deployment/screenshots/play-console-app-created.png

10. Note the Package Name assigned
    - Should be: com.smilepile
    - This CANNOT be changed later
    - Verify it matches Android project's applicationId
```

**Verification**:
- [ ] App appears in "All apps" list
- [ ] App name is "SmilePile"
- [ ] Package name is "com.smilepile"
- [ ] App dashboard accessible
- [ ] Screenshot saved

**Expected Result**: SmilePile app created in Play Console, dashboard accessible.

**If Issues Occur**:
- Package Name Conflict: Choose alternative like com.smilepile.app
- Account Not Approved: Wait for Google approval email (1-2 days)
- Payment Required: Verify $25 registration fee was paid and processed

---

### Task 5B-4: Configure Basic App Details (Play Console)

**Objective**: Set up minimum required app information.

**Procedure**:
```
1. From SmilePile app dashboard, click: "Set up your app" (if prompted)

2. Navigate to: Dashboard → Store presence → Main store listing

3. Fill in required fields:

   App name: SmilePile
   (Confirm pre-filled value)

   Short description: [Prepare 80 characters max]
   Example: "Organize and cherish your family photos with SmilePile"

   Full description: [Prepare 4000 characters max]
   (Will refine in Wave 10, for now use placeholder)
   Example:
   "SmilePile helps families organize, protect, and enjoy their precious photo memories.
   Features include photo organization, backup capabilities, and a fun Kids Mode.
   More features coming soon!"

   App icon: [Optional for now, required before publishing]
   Feature graphic: [Optional for now, required before publishing]
   Screenshots: [Optional for now, required before publishing]

4. Click: "Save" (bottom of page)

5. Navigate to: Dashboard → Store presence → Store settings

   App category: Photography
   Store listing contact email: [your-support-email@domain.com]

6. Click: "Save"

7. Navigate to: Dashboard → Policy → App content

   App access: All functionality is available without restrictions
   (Or configure restricted content if applicable)

   Ads: Does your app contain ads?
   - Select: No (or Yes if applicable)

   Content ratings: [Will complete in Wave 10]
   - Status: "Not started" is okay for now

   Target audience: [Will complete in Wave 10]
   - Status: "Not started" is okay for now

8. Click: "Save" for each section

9. Take screenshots of each configured section
```

**Verification**:
- [ ] App name confirmed
- [ ] Short and full descriptions saved
- [ ] Category set to "Photography"
- [ ] Contact email configured
- [ ] Store settings saved

**Expected Result**: Basic app details configured, ready for testing track setup.

---

### Documentation Template: App Configuration

Create file: `~/SmilePile-Deployment/docs/app-configuration.md`

```markdown
# SmilePile App Configuration

**Date Created**: 2025-10-14

## App Store Connect (iOS)

### App Identity
- **App Name**: SmilePile
- **Bundle ID**: com.smilepile
- **SKU**: com.smilepile.ios
- **Apple ID**: [Numeric app ID from App Store Connect]

### Metadata
- **Primary Category**: Photo & Video
- **Secondary Category**: Utilities
- **Price**: Free
- **Age Rating**: 4+ (or as assigned)

### URLs
- **App Store Connect URL**: https://appstoreconnect.apple.com/apps/[APP_ID]
- **Developer Portal URL**: https://developer.apple.com/account/resources/identifiers/bundleId/edit/[BUNDLE_ID]

## Google Play Console (Android)

### App Identity
- **App Name**: SmilePile
- **Package Name**: com.smilepile
- **Application ID**: [From Play Console dashboard]

### Metadata
- **Category**: Photography
- **Default Language**: English (United States)
- **Content Rating**: [To be completed in Wave 10]
- **Price**: Free

### URLs
- **Play Console URL**: https://play.google.com/console/developers/[DEV_ID]/app/[APP_ID]
- **Internal Testing URL**: [Will be generated after first build upload]

## Build Configuration

### iOS
```swift
// Info.plist values
CFBundleIdentifier: com.smilepile
CFBundleName: SmilePile
CFBundleDisplayName: SmilePile
```

### Android
```gradle
// build.gradle values
applicationId "com.smilepile"
```

## Important Notes
- Bundle ID / Package Name CANNOT be changed after first production release
- App names can be changed, but may require App Store/Play Store review
- SKU is internal identifier, not visible to users
```

---

## Phase 5C: Testing Track Configuration (1 hour)

### Task 5C-1: Configure TestFlight Internal Testing (iOS)

**Objective**: Set up internal testing group for team members.

**Procedure**:
```
1. Navigate to: App Store Connect → SmilePile app → TestFlight tab

2. In left sidebar, click: "Internal Testing"

3. Click: "+" button to create new group

4. Configure Internal Testing Group:

   Group Name: SmilePile Internal Team

   Enable Automatic Distribution:
   ☑ Automatically distribute new builds to testers
   (This ensures team gets builds immediately after upload)

   Add Testers:
   - Click: "+" next to Testers
   - Select: Internal testers from your team
   - Add at least 2 testers:
     * Adam Stack (primary)
     * [Backup team member]

   Note: Internal testers must be added as Users in App Store Connect first
   If tester not in list:
   → Go to Users and Access → Add user → Invite
   → Role: Developer or App Manager
   → Return to TestFlight → Add tester

5. Click: "Save"

6. Verify group shows:
   - Group Name: SmilePile Internal Team
   - Tester Count: 2+ (or your team size)
   - Status: Ready for builds

7. Take screenshot of group configuration
8. Save as: ~/SmilePile-Deployment/screenshots/testflight-internal-group.png
```

**Verification**:
- [ ] Internal Testing group created
- [ ] Group name is "SmilePile Internal Team"
- [ ] Automatic distribution enabled
- [ ] At least 2 testers added
- [ ] Testers receive invite emails
- [ ] Screenshot saved

**Expected Result**: Internal testing configured, team ready to receive builds.

**If Issues Occur**:
- Can't Add Tester: Verify user exists in Users and Access section
- Tester Not Receiving Email: Check spam folder, verify email address correct
- Permission Denied: Verify your account has TestFlight management permissions

---

### Task 5C-2: Configure TestFlight External Testing (iOS)

**Objective**: Set up external testing group for beta testers.

**Procedure**:
```
1. In TestFlight tab, click: "External Testing" (left sidebar)

2. Click: "+" button to create new group

3. Configure External Testing Group:

   Group Name: SmilePile Beta Testers

   Public Link:
   - For now: Disabled (use invite-only)
   - Can enable later for wider beta testing

   Enable Automatic Distribution:
   - For now: Disabled (manual control over beta releases)
   - Recommended: Keep manual until stable beta builds

   Add Testers:
   - For now: Empty (no external testers yet)
   - Will add testers before Wave 8 (Beta tier deployment)

   Test Information (REQUIRED for first external build):

   Beta App Description:
   [Prepare 1-3 sentences describing what the app does]
   Example: "SmilePile is a family photo organization app that helps you
   protect and cherish your precious memories. Features include automatic
   photo organization, secure backup, and a delightful Kids Mode."

   Feedback Email:
   [Your support email for beta feedback]
   Example: support@smilepile.com or beta@yourdomain.com

   What to Test:
   [Will complete before first external build, for now use placeholder]
   Example: "Please test photo upload, organization features, and Kids Mode.
   Report any crashes or unusual behavior to the feedback email."

4. Click: "Save"

5. Note: Group status will show "Waiting for Build"
   - This is expected - no builds uploaded yet
   - First build will require App Review (1-2 days)

6. Take screenshot of group configuration
7. Save as: ~/SmilePile-Deployment/screenshots/testflight-external-group.png
```

**Verification**:
- [ ] External Testing group created
- [ ] Group name is "SmilePile Beta Testers"
- [ ] Public link disabled (for now)
- [ ] Test information completed
- [ ] Feedback email configured
- [ ] Screenshot saved

**Expected Result**: External testing configured, ready for first beta build in Wave 8.

---

### Task 5C-3: Configure Play Console Internal Testing Track

**Objective**: Set up internal testing track for team Android builds.

**Procedure**:
```
1. Navigate to: Play Console → SmilePile app → Testing → Internal testing

2. Click: "Create new release" button

3. Configure Release (Placeholder Setup):

   Release name: Internal Testing - Wave 1 Setup
   (This is a placeholder release, no build uploaded yet)

   Release notes: [Optional for internal track]
   Example: "Internal testing track configured. First build coming soon."

   App bundles:
   - Status: None uploaded yet (expected)
   - Will upload first build in Wave 5 (Fastlane automation)

   Countries/regions:
   - Select: United States (default)
   - Can expand to more countries later

   Testers:
   - Click: "Testers" tab
   - Click: "Create email list"
   - List name: "Internal Testers"
   - Add email addresses:
     * [Your primary email]
     * [Backup team member email]
   - Click: "Save changes"

4. DO NOT click "Start rollout to Internal testing" yet
   (No build to rollout)

5. Click: "Save" (keep as draft)

6. Verify configuration shows:
   - Track: Internal testing
   - Status: Draft (no active release yet)
   - Testers: 2+ in "Internal Testers" list

7. Take screenshot of track configuration
8. Save as: ~/SmilePile-Deployment/screenshots/play-console-internal-track.png
```

**Verification**:
- [ ] Internal testing track configured
- [ ] Tester email list created
- [ ] At least 2 testers added
- [ ] Release saved as draft
- [ ] Screenshot saved

**Expected Result**: Internal testing track ready for first build upload.

---

### Task 5C-4: Configure Play Console Closed Testing Track

**Objective**: Set up closed testing track for beta testers.

**Procedure**:
```
1. Navigate to: Play Console → SmilePile app → Testing → Closed testing

2. Click: "Create new release" button

3. Configure Release (Placeholder Setup):

   Release name: Closed Beta - Wave 1 Setup

   Release notes: [Will complete before Wave 8]
   Example placeholder: "Beta testing track configured."

   App bundles:
   - Status: None uploaded yet (expected)
   - Will upload first beta build in Wave 8

   Countries/regions:
   - Select: All countries (for wider beta testing)

   Testers:
   - Click: "Testers" tab
   - Click: "Create email list"
   - List name: "Beta Testers"
   - Emails: Empty for now (will add before Wave 8)
   - Click: "Save changes"

4. DO NOT click "Start rollout to Closed testing" yet
   (No build to rollout)

5. Click: "Save" (keep as draft)

6. Verify configuration shows:
   - Track: Closed testing
   - Status: Draft
   - Tester list: "Beta Testers" (empty, ready for additions)

7. Take screenshot of track configuration
8. Save as: ~/SmilePile-Deployment/screenshots/play-console-closed-track.png
```

**Verification**:
- [ ] Closed testing track configured
- [ ] Beta tester list created (empty)
- [ ] Release saved as draft
- [ ] All countries selected
- [ ] Screenshot saved

**Expected Result**: Closed testing track ready for beta builds in Wave 8.

---

### Documentation Template: Testing Track Configuration

Create file: `~/SmilePile-Deployment/docs/testing-tracks.md`

```markdown
# SmilePile Testing Track Configuration

**Date Configured**: 2025-10-14

## iOS - TestFlight

### Internal Testing
- **Group Name**: SmilePile Internal Team
- **Automatic Distribution**: Enabled
- **Testers**: 2+ team members
- **Status**: Ready for builds
- **First Build Upload**: Wave 5 (Fastlane automation)

**Tester Invite Link**: [Generated after first build upload]
**Installation**: TestFlight app required (auto-prompted on first build)

### External Testing
- **Group Name**: SmilePile Beta Testers
- **Public Link**: Disabled (invite-only)
- **Automatic Distribution**: Disabled (manual control)
- **Beta App Description**: [Configured]
- **Feedback Email**: [Your email]
- **Status**: Ready for builds, requires App Review
- **First Build Upload**: Wave 8 (Beta tier)

**Important**: First external build requires 1-2 day App Review.

## Android - Play Console

### Internal Testing Track
- **Track Type**: Internal testing
- **Tester List Name**: Internal Testers
- **Testers**: 2+ team members
- **Countries**: United States
- **Status**: Configured, draft release created
- **First Build Upload**: Wave 5 (Fastlane automation)

**Tester Access**: Opt-in link generated after first release activated

### Closed Testing Track
- **Track Type**: Closed testing
- **Tester List Name**: Beta Testers
- **Testers**: None yet (add before Wave 8)
- **Countries**: All countries
- **Status**: Configured, draft release created
- **First Build Upload**: Wave 8 (Beta tier)

**Tester Access**: Opt-in link generated after first release activated

## Testing Workflow

### Wave 5 (QUAL/STAGE Tiers)
1. Upload internal builds via Fastlane automation
2. Internal testers receive builds automatically (iOS) or via opt-in (Android)
3. Test core functionality, gather feedback
4. Iterate on issues before beta release

### Wave 8 (BETA Tier)
1. Upload beta builds to external/closed tracks
2. iOS: Requires App Review (1-2 days)
3. Android: Immediate availability to closed testers
4. Gather feedback from wider audience
5. Iterate on issues before production release

### Wave 10 (PROD Tier)
1. Submit to App Store and Play Store for public release
2. Full review process (3-7 days typical)
3. Production release to all users
```

---

## Phase 5D: Credential Setup (2-3 hours)

### Task 5D-1: Generate App Store Connect API Key

**Objective**: Create API key for Fastlane automation of iOS builds.

**Procedure**:
```
1. Navigate to: https://appstoreconnect.apple.com/access/api

2. Click: "Users and Access" (top navigation)

3. Click: "Keys" tab

4. Click: "+" button (Generate API Key)

5. Configure API Key:

   Name: SmilePile Fastlane Automation

   Access: App Manager
   (This is minimum role required for TestFlight uploads)

   Alternative: Admin (if App Manager insufficient)
   Note: Admin gives broader access, use if deployment issues occur

6. Click: "Generate"

7. IMMEDIATELY download the key file:
   - File name format: AuthKey_XXXXXXXXXX.p8
   - CRITICAL: This can ONLY be downloaded ONCE
   - If you lose this file, you must revoke key and generate new one

8. Document key details (keep in secure location):
   - Key ID: [10-character alphanumeric, shown on keys page]
   - Issuer ID: [UUID format, shown at top of keys page]
   - Key file name: AuthKey_XXXXXXXXXX.p8
   - Download date: 2025-10-14
   - Role: App Manager (or Admin)

9. Take screenshot of Keys page (showing key created)
10. Save as: ~/SmilePile-Deployment/screenshots/app-store-api-key.png
```

**Storage Procedure**:
```bash
# Create secure directory for API keys
mkdir -p ~/app-store-connect-api-keys
chmod 700 ~/app-store-connect-api-keys

# Move downloaded key to secure location
mv ~/Downloads/AuthKey_*.p8 ~/app-store-connect-api-keys/SmilePile-AuthKey.p8

# Set strict permissions (owner read-only)
chmod 600 ~/app-store-connect-api-keys/SmilePile-AuthKey.p8

# Verify permissions
ls -la ~/app-store-connect-api-keys/SmilePile-AuthKey.p8
# Expected: -rw------- (600)
```

**Backup Procedure**:
```bash
# Backup 1: Encrypted cloud storage
cp ~/app-store-connect-api-keys/SmilePile-AuthKey.p8 \
   ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/AuthKey.p8.backup

# Backup 2: Password Manager
# Upload SmilePile-AuthKey.p8 as secure document attachment
# Include Key ID and Issuer ID in secure note

# Backup 3: External encrypted drive (if available)
# cp ~/app-store-connect-api-keys/SmilePile-AuthKey.p8 \
#    /Volumes/EncryptedBackup/SmilePile/AuthKey.p8

# Verify backup integrity
shasum -a 256 ~/app-store-connect-api-keys/SmilePile-AuthKey.p8
# Record SHA256 hash in password manager
```

**Verification**:
- [ ] API key generated in App Store Connect
- [ ] Key file downloaded (AuthKey_*.p8)
- [ ] Key ID documented
- [ ] Issuer ID documented
- [ ] Key file moved to secure location
- [ ] File permissions set to 600
- [ ] Backed up to at least 2 locations
- [ ] SHA256 hash documented

**Expected Result**: API key generated, securely stored, backed up.

**If Issues Occur**:
- Can't Generate Key: Verify account has Admin role
- Lost Key File: Revoke key in App Store Connect, generate new one
- Wrong Permissions: Re-run chmod 600 command

---

### Task 5D-2: Generate Android Production Keystore

**Objective**: Create Android signing keystore for production releases.

**CRITICAL WARNING**:
- If you lose this keystore, you CANNOT update your app in Play Store
- This is the single most important file for Android deployment
- Triple backup strategy is MANDATORY

**Procedure**:
```bash
# Create secure keystores directory
mkdir -p ~/keystores
chmod 700 ~/keystores

# Generate production keystore
keytool -genkeypair -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -alias smilepile-release \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=SmilePile, OU=Development, O=SmilePile, L=San Francisco, ST=California, C=US"

# When prompted for passwords:
# Store password: [Generate strong password, 20+ characters]
# Key password: [Generate strong password, 20+ characters, different from store]
#
# IMMEDIATELY store both passwords in password manager BEFORE continuing
# Do NOT proceed until passwords are securely stored
```

**Password Generation**:
```bash
# Option 1: Generate strong random passwords
openssl rand -base64 32
# Copy output to password manager as "SmilePile Keystore Store Password"

openssl rand -base64 32
# Copy output to password manager as "SmilePile Keystore Key Password"

# Option 2: Use password manager's generator
# - Length: 20+ characters
# - Include: uppercase, lowercase, numbers, special characters
# - Store in secure note titled "SmilePile Android Keystore"
```

**Document Keystore Metadata**:
```bash
# Extract certificate fingerprints
keytool -list -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -storepass "[YOUR_STORE_PASSWORD]" \
  | grep -A 5 "Certificate fingerprints"

# Expected output:
# SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
# SHA256: XX:XX:XX:XX:...

# Document in password manager:
# - Keystore alias: smilepile-release
# - SHA1 fingerprint: [from output above]
# - SHA256 fingerprint: [from output above]
# - Creation date: 2025-10-14
# - Validity: 27 years (until ~2052)
# - Location: ~/keystores/smilepile-production.keystore
```

**Verification**:
```bash
# Verify keystore is valid
keytool -list -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -storepass "[YOUR_STORE_PASSWORD]"

# Should show:
# - Alias name: smilepile-release
# - Creation date: [Today's date]
# - Entry type: PrivateKeyEntry
# - Certificate chain length: 1
# - Certificate[1]: Valid from ... until [~2052]

# Verify file permissions
ls -la ~/keystores/smilepile-production.keystore
# Expected: -rw------- (600)

# If not 600, fix it:
chmod 600 ~/keystores/smilepile-production.keystore
```

**Verification Checklist**:
- [ ] Keystore file created at ~/keystores/smilepile-production.keystore
- [ ] Alias is "smilepile-release"
- [ ] Store password is 20+ characters
- [ ] Key password is 20+ characters
- [ ] Both passwords stored in password manager
- [ ] SHA1 and SHA256 fingerprints documented
- [ ] File permissions are 600
- [ ] Can list keystore with passwords (verification successful)

**Expected Result**: Production keystore generated, passwords stored securely.

**If Issues Occur**:
- keytool: command not found: Ensure Java JDK is installed (java -version)
- Invalid keystore format: Verify command syntax, try again
- Lost passwords: Cannot recover - must generate new keystore

---

### Task 5D-3: Backup Android Keystore (CRITICAL)

**Objective**: Create triple redundancy backup of production keystore.

**CRITICAL**: Complete ALL three backups before proceeding.

**Backup Location 1: Encrypted Cloud Storage**
```bash
# Create credentials directory in iCloud Drive
mkdir -p ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/

# Copy keystore to iCloud
cp ~/keystores/smilepile-production.keystore \
   ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/smilepile-production.keystore.backup

# Wait for iCloud sync (check Finder → iCloud Drive)
# Verify file uploaded to cloud

# Calculate and record checksum
shasum -a 256 ~/keystores/smilepile-production.keystore
# Record SHA256 hash in password manager as "Keystore Backup Checksum"
```

**Backup Location 2: Password Manager**
```
1. Open password manager (1Password, Bitwarden, LastPass, etc.)

2. Create new secure note: "SmilePile Android Keystore"

3. Add the following information:
   - Keystore Store Password: [paste from earlier]
   - Keystore Key Password: [paste from earlier]
   - Keystore Alias: smilepile-release
   - SHA1 Fingerprint: [from earlier documentation]
   - SHA256 Fingerprint: [from earlier documentation]
   - Creation Date: 2025-10-14
   - Validity: Until ~2052
   - Backup Checksum: [SHA256 from above]

4. Attach keystore file:
   - Click "Add File" or "Attach Document"
   - Select: ~/keystores/smilepile-production.keystore
   - Upload (may take 10-30 seconds)
   - Verify file attached successfully

5. Save secure note

6. Test retrieval:
   - Download attached file to ~/Downloads/
   - Verify checksum matches:
     shasum -a 256 ~/Downloads/smilepile-production.keystore
   - Delete downloaded copy:
     rm ~/Downloads/smilepile-production.keystore
```

**Backup Location 3: External Encrypted Drive (if available)**
```bash
# If you have external encrypted drive:
# 1. Connect drive
# 2. Mount encrypted volume
# 3. Create SmilePile directory

mkdir -p /Volumes/EncryptedBackup/SmilePile/

# Copy keystore
cp ~/keystores/smilepile-production.keystore \
   /Volumes/EncryptedBackup/SmilePile/smilepile-production.keystore

# Verify checksum matches
shasum -a 256 /Volumes/EncryptedBackup/SmilePile/smilepile-production.keystore
# Compare with original checksum

# Safely eject drive
# Store drive in secure physical location
```

**Alternative Backup 3: Encrypted Disk Image**
```bash
# If no external drive, create encrypted disk image
hdiutil create \
  -size 10m \
  -encryption AES-256 \
  -volname "SmilePile Keystore Backup" \
  -fs HFS+ \
  ~/SmilePile-Keystore-Backup.dmg

# When prompted, set encryption password (20+ chars)
# Store this password in password manager

# Mount the encrypted image
hdiutil attach ~/SmilePile-Keystore-Backup.dmg

# Copy keystore to mounted volume
cp ~/keystores/smilepile-production.keystore \
   /Volumes/SmilePile\ Keystore\ Backup/

# Unmount
hdiutil detach /Volumes/SmilePile\ Keystore\ Backup/

# Store .dmg file in cloud backup
cp ~/SmilePile-Keystore-Backup.dmg \
   ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/
```

**Verification Checklist**:
- [ ] Backup 1: iCloud Drive copy exists and synced
- [ ] Backup 2: Password manager has attached keystore file
- [ ] Backup 3: External drive or encrypted DMG backup exists
- [ ] All backups verified with SHA256 checksum (matches original)
- [ ] Backup locations documented in password manager
- [ ] Can retrieve keystore from at least 2 backup locations

**Expected Result**: Keystore backed up to 3+ locations, all checksums verified.

**If Issues Occur**:
- iCloud Not Syncing: Check iCloud storage space, verify iCloud Drive enabled
- Password Manager Upload Fails: Check file size limits, try alternative manager
- External Drive Not Available: Use encrypted DMG alternative

---

### Task 5D-4: Create Google Play Service Account

**Objective**: Create service account for Fastlane automation of Android builds.

**Prerequisites**:
- ✅ Google Cloud Console access
- ✅ Play Console account active

**Part A: Enable Google Play Developer API**

**Procedure**:
```
1. Navigate to: https://console.cloud.google.com/

2. Create or select Google Cloud project:

   Option A: Create new project
   - Click: "Select a project" dropdown (top navigation)
   - Click: "New Project"
   - Project name: SmilePile Play Console API
   - Organization: [Your organization or None]
   - Location: [Your organization or No organization]
   - Click: "Create"
   - Wait for project creation (10-30 seconds)

   Option B: Use existing StackMap project
   - Click: "Select a project" dropdown
   - Select: Existing project
   - This reuses StackMap's infrastructure

3. Enable Google Play Developer API:
   - Navigation: APIs & Services → Library
   - Search: "Google Play Android Developer API"
   - Click: "Google Play Android Developer API" result
   - Click: "Enable"
   - Wait for API enablement (10-30 seconds)

4. Verify API enabled:
   - Navigation: APIs & Services → Dashboard
   - Should see: "Google Play Android Developer API" in list
   - Status: Enabled

5. Take screenshot of enabled API
6. Save as: ~/SmilePile-Deployment/screenshots/play-developer-api-enabled.png
```

**Part B: Create Service Account**

**Procedure**:
```
1. Navigate to: IAM & Admin → Service Accounts
   (In Google Cloud Console for your project)

2. Click: "Create Service Account"

3. Configure Service Account (Step 1):

   Service account name: smilepile-play-console-deploy
   (This generates service account ID automatically)

   Service account ID: smilepile-play-console-deploy
   (Should auto-fill from name)

   Service account description:
   "Fastlane automation for SmilePile Play Console build uploads and release management"

4. Click: "Create and Continue"

5. Grant this service account access to project (Step 2):
   - Role: (Leave blank for now, will grant in Play Console)
   - Click: "Continue"

6. Grant users access to this service account (Step 3):
   - Leave blank (optional step)
   - Click: "Done"

7. You should see: Service account in list
   - Name: smilepile-play-console-deploy
   - Email: smilepile-play-console-deploy@PROJECT_ID.iam.gserviceaccount.com

8. Take screenshot of service account created
9. Save as: ~/SmilePile-Deployment/screenshots/service-account-created.png
```

**Part C: Generate Service Account JSON Key**

**Procedure**:
```
1. Click on service account name: smilepile-play-console-deploy

2. Navigate to: "Keys" tab

3. Click: "Add Key" → "Create new key"

4. Select key type: JSON

5. Click: "Create"

6. IMMEDIATELY download JSON file:
   - File name format: PROJECT_NAME-XXXXXXX.json
   - Saved to: ~/Downloads/
   - CRITICAL: Store this file securely immediately

7. Rename and move file:
```

```bash
# Create secure directory
mkdir -p ~/play-console-credentials
chmod 700 ~/play-console-credentials

# Rename and move downloaded key
mv ~/Downloads/*-*.json ~/play-console-credentials/smilepile-play-console-service-account.json

# Set strict permissions
chmod 600 ~/play-console-credentials/smilepile-play-console-service-account.json

# Verify permissions
ls -la ~/play-console-credentials/
# Expected: drwx------ (700) for directory
# Expected: -rw------- (600) for JSON file
```

**Part D: Grant Service Account Access in Play Console**

**Procedure**:
```
1. Navigate to: https://play.google.com/console/developers/

2. Go to: Setup → API access (left sidebar)

3. Find section: "Service accounts"

4. You should see your service account listed:
   - Email: smilepile-play-console-deploy@PROJECT_ID.iam.gserviceaccount.com
   - Status: Not linked yet

   If NOT listed:
   - Click: "Choose a project to link" (under Service accounts section)
   - Select: Your Google Cloud project
   - Click: "Link project"
   - Wait for linking (may take 1-2 minutes)
   - Refresh page
   - Service account should now appear

5. Grant access to service account:
   - Click: "Manage Play Console permissions" (next to service account)
   - OR click: "Grant access" if service account not yet configured

6. Configure permissions:

   App permissions:
   - Click: "Add app"
   - Select: SmilePile
   - Click: "Apply"

   Account permissions:
   ☐ Admin (do NOT select - too broad)
   ☑ Release apps to testing tracks
   ☑ Release apps to production
   ☐ View app information and download bulk reports (not needed)
   ☐ Reply to reviews (not needed)
   ☐ Manage store presence (not needed)

   NOTE: Only select MINIMUM required permissions
   - Release apps to testing tracks (for internal/closed testing)
   - Release apps to production (for production releases)

7. Click: "Invite user" or "Save changes"

8. Verify service account shows:
   - Status: Active
   - Apps: SmilePile
   - Permissions: Release apps to testing tracks, Release apps to production

9. Take screenshot of service account permissions
10. Save as: ~/SmilePile-Deployment/screenshots/service-account-permissions.png
```

**Backup Service Account JSON**:
```bash
# Backup 1: Encrypted cloud storage
cp ~/play-console-credentials/smilepile-play-console-service-account.json \
   ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/play-console-service-account.json.backup

# Backup 2: Password Manager
# Upload JSON file as secure document attachment
# Title: "SmilePile Play Console Service Account"
# Include service account email in notes

# Calculate checksum
shasum -a 256 ~/play-console-credentials/smilepile-play-console-service-account.json
# Record checksum in password manager
```

**Verification Checklist**:
- [ ] Google Play Developer API enabled
- [ ] Service account created (smilepile-play-console-deploy)
- [ ] Service account JSON key generated and downloaded
- [ ] JSON file moved to ~/play-console-credentials/
- [ ] File permissions set to 600
- [ ] Service account linked in Play Console
- [ ] Permissions granted: Release to testing and production
- [ ] Service account has access to SmilePile app
- [ ] Backed up to at least 2 locations
- [ ] SHA256 checksum documented

**Expected Result**: Service account created, configured, ready for Fastlane automation.

**If Issues Occur**:
- API Not Enabling: Verify project selected, check billing enabled (no charges for this API)
- Can't Create Service Account: Verify you have "Project Editor" or "Owner" role in Cloud project
- Service Account Not Appearing in Play Console: Wait 2-3 minutes, refresh page, verify project linked
- Permission Denied: Verify you are account owner in Play Console

---

### Task 5D-5: Configure macOS Keychain Credential Storage

**Objective**: Store credentials in macOS Keychain following StackMap's proven pattern.

**CRITICAL**: This task will be fully implemented in Wave 5 when Fastlane is configured. For now, document the plan.

**Why macOS Keychain**:
- Resolved CRITICAL-06 security finding
- Credentials encrypted at rest by macOS
- No plaintext credentials in files or environment
- Consistent with iOS signing (already uses Keychain)
- Proven pattern from StackMap

**What Will Happen in Wave 5**:
```bash
# Fastlane lane will prompt for and store credentials
cd android
bundle exec fastlane store_credentials_in_keychain

# This will interactively prompt for:
# 1. Keystore store password
# 2. Keystore key password
# 3. Service account JSON path

# Credentials will be stored in macOS Keychain with service names:
# - com.smilepile.keystore.store_password
# - com.smilepile.keystore.key_password
# - com.smilepile.play_console.json_path

# Retrieval during deployment (automatic):
# Fastlane helper lanes retrieve from Keychain:
# - get_keystore_store_password
# - get_keystore_key_password
# - get_google_play_credentials_path
```

**Current Action (Wave 1)**:
Document that credentials will be stored in macOS Keychain in Wave 5. Do NOT create keystore.properties file.

**Documentation**:
Create file: `~/SmilePile-Deployment/docs/credential-storage-plan.md`

```markdown
# Credential Storage Plan - macOS Keychain

**Pattern**: Inherited from StackMap (CRITICAL-06 resolved)

## Overview

SmilePile uses macOS Keychain to securely store all deployment credentials. This eliminates the need for plaintext credential files and follows iOS's existing security model.

## Implementation Timeline

### Wave 1 (Foundation Setup) - CURRENT
- Generate keystores and API keys
- Store passwords in password manager (temporary)
- Document Keychain storage plan
- Do NOT create keystore.properties file

### Wave 5 (Fastlane Automation)
- Implement fastlane lane: store_credentials_in_keychain
- Implement helper lanes: get_keystore_store_password, etc.
- Migrate credentials from password manager to Keychain
- Test retrieval and deployment with Keychain credentials

## Credentials Stored in Keychain

### Android Credentials
- **Keystore store password**: Retrieved during build signing
- **Keystore key password**: Retrieved during build signing
- **Service account JSON path**: Retrieved during Play Console upload

### iOS Credentials
- **Already using Keychain**: iOS code signing uses Keychain by default
- **Certificates**: Stored in login keychain
- **Provisioning profiles**: Managed by Xcode, reference Keychain certificates

### Not Stored in Keychain
- **Keystore file itself**: Stored at ~/keystores/ (file, not password)
- **Service account JSON file**: Stored at ~/play-console-credentials/ (file)
- **API key .p8 file**: Stored at ~/app-store-connect-api-keys/ (file)

Note: Keychain stores passwords/secrets, not files. Files are stored in secure directories with 600 permissions.

## Security Benefits

1. **Encrypted at rest**: macOS Keychain uses hardware encryption
2. **Access control**: Only keychain owner can retrieve credentials
3. **No plaintext**: Credentials never written to shell config or properties files
4. **Audit trail**: Keychain access logged by macOS
5. **Platform consistency**: Same pattern for iOS and Android

## Migration Path (Wave 5)

### Before Migration
- Credentials in password manager
- Manual entry required for deployments
- No automation possible

### After Migration
- Credentials in macOS Keychain
- Fastlane retrieves automatically during deployment
- Full automation enabled
- Password manager backup retained for disaster recovery

## Verification (Wave 5)

After implementing Keychain storage:
```bash
# Verify credentials stored
security find-generic-password -s "com.smilepile.keystore.store_password" -w

# Test Fastlane retrieval
cd android
bundle exec fastlane test_keychain_credentials

# Expected: Credentials retrieved successfully, no manual entry required
```

## Backup and Recovery

### Primary Storage
- macOS Keychain (encrypted, hardware-backed)

### Backup Storage (for disaster recovery)
- Password manager: Contains all passwords
- If Keychain corrupted/lost: Re-run store_credentials_in_keychain
- Retrieve passwords from password manager, store in Keychain again

### Bus Factor Mitigation
- Primary deployer: Has credentials in Keychain
- Backup deployer: Run store_credentials_in_keychain on their machine
- Passwords retrieved from shared password manager vault
```

**Verification**:
- [ ] Documentation created explaining Keychain storage plan
- [ ] Wave 5 migration path documented
- [ ] No keystore.properties file created (correct - using Keychain instead)
- [ ] Backup passwords still in password manager (for Wave 5 migration)

**Expected Result**: Keychain storage pattern documented, ready to implement in Wave 5.

---

## Phase 5E: Security Audit (1 hour)

### Task 5E-1: Verify No Credentials in Git

**Objective**: Ensure no credentials are committed to git repository.

**Procedure**:
```bash
cd /Users/adamstack/SmilePile

# Check current git status for credential files
git status | grep -E "(keystore|\.p8|\.json|credentials|secrets)"
# Expected: No output (all credential files ignored)

# Verify .gitignore includes credential patterns
cat .gitignore | grep -E "(keystore|credentials|\.p8|\.json|secrets)"

# Expected patterns in .gitignore:
# *.keystore
# google-services.json (if using Firebase)
# secrets/
# *.p8
# play-console-credentials/
# app-store-connect-api-keys/

# Check for any staged credential files
git diff --cached --name-only | grep -E "(keystore|\.p8|\.json)"
# Expected: No output

# Search git history for accidentally committed credentials
git log --all --full-history -- "*.keystore" "*.jks"
# Expected: No results

git log --all --full-history -- "*.p8" "*.p12"
# Expected: No results

# Search for password strings in commit history
git grep -i "password" $(git rev-list --all) | grep -v "example" | head -20
# Expected: Only examples, placeholders, or comments

# Search for API key patterns
git grep -E "(AuthKey_|AIza|AKIA)" $(git rev-list --all)
# Expected: No results
```

**Update .gitignore if needed**:
```bash
# Check if credential patterns are missing
MISSING_PATTERNS=""

grep -q "keystore.properties" .gitignore || MISSING_PATTERNS="$MISSING_PATTERNS\nandroid/keystore.properties"
grep -q "play-console-credentials" .gitignore || MISSING_PATTERNS="$MISSING_PATTERNS\nplay-console-credentials/"
grep -q "app-store-connect-api-keys" .gitignore || MISSING_PATTERNS="$MISSING_PATTERNS\napp-store-connect-api-keys/"

if [ -n "$MISSING_PATTERNS" ]; then
  echo "Adding missing patterns to .gitignore"
  cat >> .gitignore <<'EOF'

# SmilePile Deployment Credentials - NEVER COMMIT
android/keystore.properties
play-console-credentials/
app-store-connect-api-keys/
*.keystore
*.p8
*.p12
*.mobileprovision
EOF

  git add .gitignore
  git commit -m "security: Add credential patterns to .gitignore"
fi
```

**Verification**:
- [ ] git status shows no credential files
- [ ] .gitignore includes all credential patterns
- [ ] No credentials in git history
- [ ] No API keys in commit messages or code
- [ ] .gitignore updated and committed (if needed)

**Expected Result**: Repository clean of all credentials, .gitignore configured correctly.

---

### Task 5E-2: Verify File Permissions

**Objective**: Ensure all credential files have restrictive permissions.

**Procedure**:
```bash
# Check directories
echo "=== Checking directory permissions ==="
ls -ld ~/keystores 2>/dev/null || echo "~/keystores not found (expected if not created yet)"
ls -ld ~/play-console-credentials 2>/dev/null || echo "~/play-console-credentials not found"
ls -ld ~/app-store-connect-api-keys 2>/dev/null || echo "~/app-store-connect-api-keys not found"
ls -ld /Users/adamstack/SmilePile/deploy/secrets 2>/dev/null || echo "deploy/secrets not found"

# Expected: drwx------ (700) for all directories

# Check credential files
echo "=== Checking credential file permissions ==="
ls -l ~/keystores/*.keystore 2>/dev/null || echo "No keystores found"
ls -l ~/play-console-credentials/*.json 2>/dev/null || echo "No service account JSON found"
ls -l ~/app-store-connect-api-keys/*.p8 2>/dev/null || echo "No API keys found"
ls -l /Users/adamstack/SmilePile/deploy/secrets/*.env 2>/dev/null || echo "No secrets files found"

# Expected: -rw------- (600) for all files

# Fix permissions if incorrect
echo "=== Fixing permissions ==="
chmod 700 ~/keystores 2>/dev/null || true
chmod 700 ~/play-console-credentials 2>/dev/null || true
chmod 700 ~/app-store-connect-api-keys 2>/dev/null || true
chmod 700 /Users/adamstack/SmilePile/deploy/secrets 2>/dev/null || true

chmod 600 ~/keystores/*.keystore 2>/dev/null || true
chmod 600 ~/play-console-credentials/*.json 2>/dev/null || true
chmod 600 ~/app-store-connect-api-keys/*.p8 2>/dev/null || true
chmod 600 /Users/adamstack/SmilePile/deploy/secrets/*.env 2>/dev/null || true

# Verify after fixing
echo "=== Verification after fix ==="
ls -ld ~/keystores ~/play-console-credentials ~/app-store-connect-api-keys 2>/dev/null
ls -l ~/keystores/*.keystore ~/play-console-credentials/*.json ~/app-store-connect-api-keys/*.p8 2>/dev/null
```

**Verification**:
- [ ] All credential directories have 700 permissions (drwx------)
- [ ] All credential files have 600 permissions (-rw-------)
- [ ] Only owner can read/write credential files
- [ ] Group and others have no access

**Expected Result**: All credentials protected with restrictive file permissions.

---

### Task 5E-3: Create Security Documentation

**Objective**: Document security best practices and backup procedures.

Create file: `~/SmilePile-Deployment/docs/security-procedures.md`

```markdown
# SmilePile Security Procedures

**Last Updated**: 2025-10-14
**Owner**: Adam Stack

## Credential Storage Locations

### Production Credentials (NEVER COMMIT TO GIT)

#### Android Keystore
- **Location**: ~/keystores/smilepile-production.keystore
- **Alias**: smilepile-release
- **Permissions**: 600 (-rw-------)
- **Backup Locations**:
  1. iCloud Drive: ~/Library/Mobile Documents/com~apple~CloudDocs/SmilePile-Credentials/
  2. Password Manager: [Your password manager] - Secure note with file attachment
  3. [External backup location if applicable]
- **SHA256 Checksum**: [Record from backup verification]

#### App Store Connect API Key
- **Location**: ~/app-store-connect-api-keys/SmilePile-AuthKey.p8
- **Key ID**: [10-character ID]
- **Issuer ID**: [UUID]
- **Permissions**: 600 (-rw-------)
- **Backup Locations**:
  1. iCloud Drive: ~/Library/Mobile Documents/com~apple~CloudDocs/SmilePile-Credentials/
  2. Password Manager: Secure note with file attachment
- **Role**: App Manager (or Admin)

#### Play Console Service Account
- **Location**: ~/play-console-credentials/smilepile-play-console-service-account.json
- **Service Account Email**: smilepile-play-console-deploy@PROJECT_ID.iam.gserviceaccount.com
- **Permissions**: 600 (-rw-------)
- **Backup Locations**:
  1. iCloud Drive: ~/Library/Mobile Documents/com~apple~CloudDocs/SmilePile-Credentials/
  2. Password Manager: Secure document
- **Play Console Permissions**: Release apps to testing tracks, Release apps to production

#### Keystore Passwords
- **Location**: macOS Keychain (Wave 5), Password Manager (Wave 1 temporary)
- **Store Password**: 20+ characters, mixed case/numbers/symbols
- **Key Password**: 20+ characters, different from store password
- **Password Manager Entry**: "SmilePile Android Keystore Passwords"

## Security Best Practices

### Credential Handling
1. **NEVER commit credentials to git** - Always verify with `git status` before committing
2. **NEVER share credentials via email or Slack** - Use password manager sharing features
3. **NEVER store credentials in plaintext files** - Use macOS Keychain (Wave 5)
4. **ALWAYS verify file permissions** - Credentials must be 600, directories 700
5. **ALWAYS backup immediately** - Triple redundancy for keystores

### Access Control
- **Primary Deployer**: Adam Stack - has all credentials in Keychain and password manager
- **Backup Deployer**: [Name if applicable] - should also configure Keychain access
- **Credential Sharing**: Use password manager's secure sharing features only
- **Revocation**: If team member leaves, rotate service account JSON key

### Git Repository Protection
- Keep .gitignore up to date with credential patterns
- Run security audit quarterly: `git grep -i password | grep -v example`
- Consider using git-secrets: `brew install git-secrets && git secrets --install`
- Pre-commit hook can prevent accidental commits (optional, Wave 10)

### Keystore Protection (CRITICAL)
Android keystore loss = cannot update app. Protect at all costs:
1. **Triple backup** - Minimum 3 independent backup locations
2. **Verify backups quarterly** - Test restoration from each location
3. **Never delete original** - Until new keystore verified working
4. **Use Play App Signing** - Google keeps encrypted backup of production key

## Backup Verification Procedures

### Quarterly Backup Verification (Every 90 days)

**Checklist**:
```bash
# 1. Verify iCloud backup exists and is readable
ls -lh ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/
# Expected: keystore, API key, service account JSON

# 2. Verify password manager backup exists
# - Open password manager
# - Navigate to "SmilePile Android Keystore" secure note
# - Verify file attachment present
# - Verify passwords are correct (test keystore access)

# 3. Verify checksums match original
shasum -a 256 ~/keystores/smilepile-production.keystore
shasum -a 256 ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/smilepile-production.keystore.backup
# Expected: Checksums match exactly

# 4. Test keystore restoration
# (Do NOT delete original - test restoration to temporary location)
cp ~/Library/Mobile\ Documents/com~apple~CloudDocs/SmilePile-Credentials/smilepile-production.keystore.backup \
   /tmp/test-restore.keystore
keytool -list -v -keystore /tmp/test-restore.keystore -storepass "[PASSWORD_FROM_PASSWORD_MANAGER]"
# Expected: Keystore details shown, no errors
rm /tmp/test-restore.keystore

# 5. Document verification
# Record in password manager:
# "Last backup verification: 2025-10-14 - All backups valid"
```

**Calendar Reminder**: Set recurring reminder for "SmilePile Keystore Backup Verification" every 90 days.

## Incident Response

### If Credential Compromised

**Immediate Actions**:
1. **Revoke compromised credential immediately**
   - App Store API Key: App Store Connect → Users and Access → Keys → Revoke
   - Service Account: Google Cloud Console → IAM → Service Accounts → Delete key
   - Keystore: If upload keystore (Play App Signing enrolled), request reset from Play Console
2. **Generate new credential**
3. **Update Keychain/password manager with new credential** (Wave 5)
4. **Test new credential works** (`fastlane test_credentials`)
5. **Audit access logs** for unauthorized activity
6. **Document incident**: What was compromised, when discovered, actions taken

### If Keystore Lost

**CRITICAL**: Android keystore loss is catastrophic if not backed up.

**Scenario A: Backups Exist (Expected)**
1. Verify backup locations accessible
2. Choose most recent verified backup
3. Restore to ~/keystores/smilepile-production.keystore
4. Verify checksum matches documented hash
5. Test keystore: `keytool -list -v -keystore ~/keystores/smilepile-production.keystore`
6. Document incident and restoration time

**Scenario B: No Backups (CATASTROPHIC)**
1. If enrolled in Play App Signing: Google has production key, can reset upload key
   - Contact Google Play support immediately
   - Request upload key reset
   - Generate new upload keystore
   - Submit new upload key certificate to Google
   - Update build configuration
2. If NOT enrolled in Play App Signing: App cannot be updated
   - Would require new Play Store listing
   - Loss of all existing users and reviews
   - **Prevention is only solution** - Triple backup is mandatory

### If Primary Deployer Unavailable

**Bus Factor Mitigation**:
1. **Backup deployer** should have:
   - Password manager access (shared vault)
   - Keystore backup access
   - Play Console access (Release Manager role)
   - App Store Connect access (Developer or App Manager role)
2. **Backup deployer setup**:
   - Retrieve keystore from password manager backup
   - Place at ~/keystores/smilepile-production.keystore
   - Run `fastlane store_credentials_in_keychain` (Wave 5)
   - Retrieve passwords from password manager
   - Test deployment to QUAL tier
3. **Emergency contact**: [Primary deployer contact info]

## Security Audit Schedule

### Weekly
- Verify no credentials in `git status` before commits

### Monthly
- Review access logs (App Store Connect activity, Play Console access)
- Verify credential file permissions: `ls -l ~/keystores ~/play-console-credentials ~/app-store-connect-api-keys`

### Quarterly
- Run full backup verification procedure (above)
- Test keystore restoration from each backup location
- Review and update security documentation
- Audit team access (add/remove users as needed)

### Annually
- Rotate service account JSON key (generate new key, revoke old)
- Review App Store Connect API key usage (consider rotation if suspicious activity)
- Update emergency contact information
- Review and update this security documentation

## Contact Information

### Emergency Contacts
- **Primary Deployer**: Adam Stack - [email/phone]
- **Backup Deployer**: [Name] - [email/phone]
- **Password Manager Admin**: [Contact info]

### Support Contacts
- **Apple Developer Support**: https://developer.apple.com/support/
- **Google Play Support**: https://support.google.com/googleplay/android-developer
- **Fastlane Support**: https://docs.fastlane.tools/help/

---

**Document Version**: 1.0
**Review Frequency**: Quarterly
**Next Review Date**: 2025-01-14
```

**Verification**:
- [ ] Security procedures documented
- [ ] Backup locations documented
- [ ] Incident response procedures documented
- [ ] Quarterly verification checklist created
- [ ] Emergency contacts documented

**Expected Result**: Comprehensive security documentation for team reference.

---

## Phase 5F: Documentation (1 hour)

### Final Documentation: Wave 1 Completion Summary

Create file: `~/SmilePile-Deployment/docs/wave-1-completion-summary.md`

```markdown
# Wave 1 Completion Summary

**Wave**: 1 of 10 - Foundation Setup
**Date Completed**: [DATE]
**Owner**: Adam Stack
**Status**: COMPLETE

## Objectives Achieved

Wave 1 established all foundational accounts, credentials, and security infrastructure required for professional 4-tier deployment system (QUAL → STAGE → BETA → PROD).

## Accounts Configured

### Apple Developer & App Store Connect
- ✅ Apple Developer account verified active (Team 84W9WSYQQB)
- ✅ SmilePile app created in App Store Connect
- ✅ Bundle ID: com.smilepile (or variant)
- ✅ TestFlight Internal Testing group configured
- ✅ TestFlight External Testing group configured
- ✅ App Store Connect API key generated and backed up
- ✅ API key tested and verified working

### Google Play Console
- ✅ Play Console account verified active
- ✅ SmilePile app created in Play Console
- ✅ Package name: com.smilepile
- ✅ Internal testing track configured
- ✅ Closed testing track configured
- ✅ Play Console service account created
- ✅ Service account granted Release Manager permissions
- ✅ Service account tested and verified working

## Credentials Generated

### Android Keystore (CRITICAL)
- ✅ Production keystore generated (4096-bit RSA)
- ✅ Keystore alias: smilepile-release
- ✅ Strong passwords generated (20+ characters)
- ✅ Passwords stored in password manager
- ✅ Keystore backed up to 3+ locations:
  1. iCloud Drive (encrypted cloud storage)
  2. Password manager (secure document attachment)
  3. [Third backup location]
- ✅ All backups verified with SHA256 checksums
- ✅ Backup restoration tested successfully
- ✅ Keystore metadata documented (SHA1/SHA256 fingerprints)

### App Store Connect API Key
- ✅ API key generated (AuthKey_*.p8)
- ✅ Key ID and Issuer ID documented
- ✅ API key backed up to 2+ locations
- ✅ File permissions set to 600
- ✅ API key tested with `fastlane pilot list`

### Play Console Service Account
- ✅ Service account created (smilepile-play-console-deploy)
- ✅ Service account JSON key generated
- ✅ JSON backed up to 2+ locations
- ✅ File permissions set to 600
- ✅ Minimum permissions granted (Release Manager only)
- ✅ Service account tested with `fastlane supply init`

## Security Measures Implemented

### Git Repository Protection
- ✅ .gitignore updated with all credential patterns
- ✅ Git history audited for accidentally committed secrets
- ✅ No credentials found in git status or history
- ✅ Repository verified clean of all credentials

### File Permissions
- ✅ All credential directories: 700 permissions (drwx------)
- ✅ All credential files: 600 permissions (-rw-------)
- ✅ Owner-only access verified

### Backup Redundancy
- ✅ Android keystore: Triple redundancy (3+ backup locations)
- ✅ API key: Dual redundancy (2+ backup locations)
- ✅ Service account JSON: Dual redundancy (2+ backup locations)
- ✅ All backups verified with checksums
- ✅ Restoration procedures tested and documented

### Security Documentation
- ✅ Security procedures documented
- ✅ Backup verification procedures documented
- ✅ Incident response procedures documented
- ✅ Quarterly audit schedule established
- ✅ Emergency contacts documented

## Credential Storage Plan

**Pattern**: macOS Keychain (following StackMap's proven approach)

**Wave 1**: Credentials in password manager (temporary, secure)
**Wave 5**: Credentials migrated to macOS Keychain (automated retrieval)

**No keystore.properties file created** - Using Keychain instead (CRITICAL-06 resolved)

## Documentation Created

1. **account-status.md** - Apple and Google account details
2. **app-configuration.md** - App metadata and IDs
3. **testing-tracks.md** - TestFlight and Play Console track configuration
4. **credential-storage-plan.md** - macOS Keychain implementation plan
5. **security-procedures.md** - Security best practices and incident response
6. **wave-1-completion-summary.md** - This document

## Testing & Verification

### Accounts Verified
- [x] App Store Connect login successful
- [x] TestFlight groups visible and configured
- [x] Play Console login successful
- [x] Testing tracks visible and configured

### Credentials Verified
- [x] API key works: `fastlane pilot list` successful
- [x] Service account works: `fastlane supply init` successful
- [x] Keystore readable: `keytool -list` successful
- [x] Keystore restoration tested from all backup locations

### Security Verified
- [x] No credentials in git status
- [x] No credentials in git history
- [x] .gitignore configured correctly
- [x] File permissions correct (600/700)
- [x] Backups verified with checksums

## Success Metrics

- **Security**: ✅ Zero credentials in git history (verified)
- **Redundancy**: ✅ 100% keystore recoverability from any backup location
- **Independence**: ✅ Minimum 2 team members can access all accounts (if backup member configured)
- **Completeness**: ✅ All checklist items completed
- **Auditability**: ✅ Complete documentation trail for all credentials
- **Testability**: ✅ Credentials tested with Fastlane commands
- **No Blockers**: ✅ Wave 2 can begin immediately

## Known Issues / Limitations

### None (Expected Result)

If any issues occurred during setup, document here:
- Issue description
- Impact on deployment
- Resolution or workaround
- Follow-up required (if any)

## Next Steps

### Immediate (Wave 2)
- Configure iOS tier system (xcconfig files, schemes)
- Set up QUAL, STAGE, BETA, PROD tiers for iOS
- Configure build variants per tier

### Upcoming (Wave 3)
- Configure Android tier system (product flavors, build types)
- Set up signing configurations per tier
- Integrate with existing build system

### Later (Wave 5)
- Implement Fastlane automation
- Migrate credentials to macOS Keychain
- Implement `store_credentials_in_keychain` lane
- Test automated deployments to TestFlight and Play Console

### Before Production (Wave 10)
- Complete App Store metadata (screenshots, descriptions)
- Complete Play Store metadata (graphics, content rating)
- Submit for production review
- Final security audit

## Team Handoff

### Access Required for Wave 2+
- SmilePile codebase access (git repository)
- Development environment (Xcode, Android Studio)
- Build tools (Node.js, React Native CLI)

### No Additional Account Access Needed
- All accounts configured in Wave 1
- Credentials ready for automation in Wave 5
- No external dependencies for Wave 2-4

### Backup Deployer Setup (Optional but Recommended)
If configuring backup deployer:
1. Grant Play Console access (Release Manager role)
2. Grant App Store Connect access (Developer or App Manager role)
3. Share password manager vault
4. Provide keystore backup access
5. Schedule test deployment to verify access

## Timeline

**Original Estimate**: 5-7 calendar days, 8-12 hours active work
**Actual Time**: [Record actual time spent]

### Breakdown by Phase
- Phase A (Account Verification): [time]
- Phase B (App Creation): [time]
- Phase C (Testing Track Configuration): [time]
- Phase D (Credential Setup): [time]
- Phase E (Security Audit): [time]
- Phase F (Documentation): [time]

## Lessons Learned

Document any insights or recommendations for future waves:
- What went well
- What was challenging
- What could be improved
- Recommendations for Wave 2+

---

**Wave 1 Status**: COMPLETE ✅
**Ready for Wave 2**: YES
**Blockers**: NONE
**Owner**: Adam Stack
**Date**: [Completion date]
```

---

## Validation Checklist: Wave 1 Complete

Before marking Wave 1 as complete, verify ALL items:

### Accounts
- [ ] Apple Developer account active (Team 84W9WSYQQB verified)
- [ ] App Store Connect app created (SmilePile)
- [ ] TestFlight Internal Testing group configured (2+ testers)
- [ ] TestFlight External Testing group configured
- [ ] Play Console account active
- [ ] Play Console app created (SmilePile, package: com.smilepile)
- [ ] Play Console Internal Testing track configured
- [ ] Play Console Closed Testing track configured

### Credentials
- [ ] App Store Connect API key generated
- [ ] API key backed up to 2+ locations
- [ ] API key tested successfully
- [ ] Android production keystore generated (4096-bit RSA)
- [ ] Keystore backed up to 3+ locations
- [ ] All keystore backups verified with checksums
- [ ] Keystore restoration tested from all backup locations
- [ ] Keystore passwords stored in password manager
- [ ] Play Console service account created
- [ ] Service account JSON generated
- [ ] Service account JSON backed up to 2+ locations
- [ ] Service account permissions configured (Release Manager)
- [ ] Service account tested successfully

### Security
- [ ] .gitignore updated with credential patterns
- [ ] Git history audited (no secrets found)
- [ ] No credentials in git status
- [ ] All credential files have 600 permissions
- [ ] All credential directories have 700 permissions
- [ ] macOS Keychain storage plan documented (Wave 5 implementation)
- [ ] Security procedures documented
- [ ] Incident response procedures documented
- [ ] Backup verification procedures documented

### Documentation
- [ ] account-status.md created
- [ ] app-configuration.md created
- [ ] testing-tracks.md created
- [ ] credential-storage-plan.md created
- [ ] security-procedures.md created
- [ ] wave-1-completion-summary.md created
- [ ] All screenshots saved for records
- [ ] Emergency contacts documented
- [ ] Backup locations documented

### Testing & Verification
- [ ] Can log into App Store Connect
- [ ] Can log into Play Console
- [ ] TestFlight groups visible
- [ ] Play Console testing tracks visible
- [ ] `fastlane pilot list` command successful
- [ ] `fastlane supply init` command successful
- [ ] `keytool -list` command successful (keystore readable)
- [ ] Keystore restoration successful from Backup 1
- [ ] Keystore restoration successful from Backup 2
- [ ] Keystore restoration successful from Backup 3

---

## Manual Steps Summary

Since I cannot execute browser-based or interactive tasks, here's what requires human action:

### Required Manual Actions (Human Required)

1. **Phase 5A**: Open browsers, log into accounts, verify access, take screenshots
2. **Phase 5B**: Create apps in consoles, configure metadata (all browser-based)
3. **Phase 5C**: Configure testing groups and tracks (all browser-based)
4. **Phase 5D**: Generate API keys and service accounts (browser + command line)
5. **Phase 5E**: Run security audit commands (command line available)
6. **Phase 5F**: Create final documentation (can be assisted)

### What I Can Help With

- Running command-line security audits
- Creating documentation files
- Verifying git status
- Generating scripts for automation
- Providing troubleshooting guidance

---

## Estimated Timeline

Based on StackMap's infrastructure inheritance:

- **Phase 5A (Account Verification)**: 30 minutes
- **Phase 5B (App Creation)**: 1-2 hours
- **Phase 5C (Testing Track Configuration)**: 1 hour
- **Phase 5D (Credential Setup)**: 2-3 hours
- **Phase 5E (Security Audit)**: 1 hour
- **Phase 5F (Documentation)**: 1 hour

**Total**: 6-8 hours active work over 5-7 calendar days (allowing for Google Play approval if needed)

---

## Issues & Resolutions

### None Yet (Template)

If issues occur during implementation, document here:

**Issue**: [Description]
**Impact**: [How it affects deployment]
**Resolution**: [What was done to fix it]
**Prevention**: [How to avoid in future]

---

## Blockers

### None (Expected)

All prerequisites met:
- ✅ Apple Developer account active
- ✅ Google Play Console account accessible
- ✅ Password manager available
- ✅ Development machine ready

---

## Next Wave: Wave 2 - iOS Tier Configuration

**Objective**: Configure iOS build tiers (QUAL, STAGE, BETA, PROD)

**Prerequisites**:
- ✅ Wave 1 complete (this wave)
- ✅ iOS development environment set up
- ✅ Xcode installed

**Timeline**: 2-3 hours
**Type**: Code configuration (xcconfig files, schemes)

---

**Document Version**: 1.0
**Created**: 2025-10-14
**Status**: READY FOR MANUAL EXECUTION
**Owner**: Adam Stack
