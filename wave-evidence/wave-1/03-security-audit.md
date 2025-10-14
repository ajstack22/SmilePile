# Wave 1 Security Audit Report - Foundation Setup

**Audit Date**: 2025-10-13
**Auditor**: Security Agent (Atlas Phase 4)
**Scope**: STORY-6.1 Wave 1 Foundation Setup Implementation Plan
**Status**: COMPREHENSIVE SECURITY REVIEW COMPLETE

---

## Executive Summary

This security audit identifies **9 CRITICAL vulnerabilities**, **14 HIGH-risk issues**, and **11 MEDIUM-risk issues** in the Wave 1 Foundation Setup implementation plan. The plan has strong security awareness with comprehensive .gitignore patterns and existing security infrastructure, but contains dangerous gaps in credential lifecycle management, backup security, and disaster recovery procedures.

### Critical Finding Summary

- **CRITICAL-01**: Plaintext keystore passwords in keystore.properties (600 permissions insufficient)
- **CRITICAL-02**: No encryption for credential backup files
- **CRITICAL-03**: Weak password generation allows predictable passwords
- **CRITICAL-04**: Service account JSON has excessive permissions (Release Manager)
- **CRITICAL-05**: No verification of App Store Connect API key permissions before storage
- **CRITICAL-06**: API keys transmitted via environment variables (process inspection risk)
- **CRITICAL-07**: No keystore password rotation strategy
- **CRITICAL-08**: Git history audit incomplete (missing blob search)
- **CRITICAL-09**: Single point of failure (Adam Stack) for all credentials

### Risk Score: 72/100 (HIGH RISK)

**Recommendation**: DO NOT proceed with Wave 1 until CRITICAL vulnerabilities are mitigated.

---

## 1. CRITICAL VULNERABILITIES (Must Fix Before Implementation)

### CRITICAL-01: Plaintext Keystore Passwords in keystore.properties

**Severity**: CRITICAL
**Category**: Credential Storage
**Location**: Implementation Plan Section 4, Hour 12 (lines 583-610)

**Description**:
The implementation plan stores Android keystore passwords in plaintext in `android/keystore.properties`:
```
release.storePassword=PRODUCTION_STORE_PASSWORD_HERE
release.keyPassword=PRODUCTION_KEY_PASSWORD_HERE
```

While the file has 600 permissions (owner read/write only), this is insufficient protection because:
1. Any process running as the user can read the file
2. Backup systems may copy the file without preserving permissions
3. IDE indexing may cache the file contents
4. Malware running as the user has full access
5. Developer machines may be compromised via supply chain attacks

**Impact**:
- Attacker with machine access can extract production keystore passwords
- Compromised keystore allows signing malicious APKs as SmilePile
- Users would install malicious updates believing they're legitimate
- Catastrophic loss of user trust and potential legal liability

**Likelihood**: HIGH (developer machines are frequent attack targets)

**Mitigation**:
1. **Immediate**: Use encrypted storage for keystore passwords
   ```bash
   # Option 1: Use macOS Keychain (recommended for macOS development)
   security add-generic-password -a "${USER}" -s "smilepile-release-store" \
     -w "PASSWORD_HERE" -T ""

   # Retrieve in build script:
   STORE_PASS=$(security find-generic-password -a "${USER}" \
     -s "smilepile-release-store" -w)
   ```

2. **Option 2**: Encrypt keystore.properties with GPG
   ```bash
   # Encrypt (after creating keystore.properties)
   gpg --encrypt --recipient your-key-id android/keystore.properties
   rm android/keystore.properties

   # Decrypt before builds (in deploy script)
   gpg --decrypt android/keystore.properties.gpg > android/keystore.properties
   chmod 600 android/keystore.properties
   ```

3. **Option 3**: Use environment variables loaded from encrypted secrets
   ```bash
   # Load from encrypted file
   eval $(gpg --decrypt deploy/secrets/production.env.gpg)

   # Build with env vars (no keystore.properties needed)
   ./gradlew assembleRelease \
     -Pandroid.injected.signing.store.password="$ANDROID_KEYSTORE_PASSWORD"
   ```

4. **Update build.gradle.kts** to support encrypted credential sources:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file(System.getenv("KEYSTORE_PATH")
                   ?: rootProject.file("../keystores/smilepile-production.keystore"))
               storePassword = System.getenv("KEYSTORE_PASSWORD")
                   ?: loadFromKeychain("smilepile-release-store")
               keyAlias = System.getenv("KEY_ALIAS") ?: "smilepile-release"
               keyPassword = System.getenv("KEY_PASSWORD")
                   ?: loadFromKeychain("smilepile-release-key")
           }
       }
   }
   ```

**Verification**:
```bash
# Verify passwords are NOT in plaintext
grep -r "password" android/ --include="*.properties" | grep -v "example"
# Expected: No results or only encrypted references

# Verify keychain storage works
security find-generic-password -a "${USER}" -s "smilepile-release-store" -w
# Expected: Password output

# Verify build works with keychain
./gradlew assembleRelease
# Expected: Successful build without plaintext passwords
```

---

### CRITICAL-02: No Encryption for Credential Backup Files

**Severity**: CRITICAL
**Category**: Backup Security
**Location**: Implementation Plan Section 4, Hours 10-11 (lines 521-543)

**Description**:
The backup strategy stores keystores and credentials in multiple locations without encryption:
```bash
# Backup 1: Encrypted cloud storage
cp ~/keystores/smilepile-production.keystore \
   ~/iCloud/SmilePile-Credentials/smilepile-production.keystore.backup

# Backup 2: External encrypted drive
cp ~/keystores/smilepile-production.keystore \
   /Volumes/EncryptedBackup/SmilePile/smilepile-production.keystore
```

Problems:
1. iCloud is NOT encrypted for individual files (only in transit)
2. "EncryptedBackup" drive encryption status is assumed, not verified
3. No file-level encryption means:
   - Apple can access iCloud backups via legal requests
   - Compromised iCloud account exposes keystores
   - Drive encryption can be bypassed if drive is mounted
   - Backup sync services may cache unencrypted copies

**Impact**:
- iCloud compromise = production keystore stolen
- Legal subpoena to Apple = keystore exposed
- Lost/stolen external drive = keystore compromised (if drive not actually encrypted)
- Supply chain attack on backup provider = mass credential theft

**Likelihood**: MEDIUM-HIGH (iCloud accounts are high-value targets)

**Mitigation**:
1. **Encrypt all backup files individually**:
   ```bash
   # Generate encryption key (store in password manager)
   BACKUP_KEY=$(openssl rand -base64 32)

   # Backup 1: Encrypted cloud storage
   openssl enc -aes-256-cbc -salt \
     -in ~/keystores/smilepile-production.keystore \
     -out ~/iCloud/SmilePile-Credentials/smilepile-production.keystore.enc \
     -pass pass:"$BACKUP_KEY"

   # Verify encryption
   file ~/iCloud/SmilePile-Credentials/smilepile-production.keystore.enc
   # Expected: "data" (not "Java KeyStore")

   # Backup 2: External drive (also encrypted)
   openssl enc -aes-256-cbc -salt \
     -in ~/keystores/smilepile-production.keystore \
     -out /Volumes/EncryptedBackup/SmilePile/smilepile-production.keystore.enc \
     -pass pass:"$BACKUP_KEY"

   # Backup 3: Password manager (encrypted by default)
   # Upload .keystore.enc file or base64 encoded encrypted file
   ```

2. **Verify drive encryption before backup**:
   ```bash
   # macOS: Verify APFS encryption
   diskutil info /Volumes/EncryptedBackup | grep "Encrypted"
   # Expected: "Encrypted: Yes"

   # If not encrypted, refuse backup
   if ! diskutil info /Volumes/EncryptedBackup | grep -q "Encrypted: Yes"; then
     echo "ERROR: Backup drive is not encrypted. Refusing to backup."
     exit 1
   fi
   ```

3. **Create encrypted backup script**:
   ```bash
   # deploy/scripts/backup_keystore.sh
   #!/bin/bash
   KEYSTORE_PATH="$1"
   BACKUP_DIR="$2"
   ENCRYPTION_KEY="$3"  # From password manager

   if [ ! -f "$KEYSTORE_PATH" ]; then
     echo "ERROR: Keystore not found: $KEYSTORE_PATH"
     exit 1
   fi

   # Create encrypted backup with timestamp
   TIMESTAMP=$(date +%Y%m%d_%H%M%S)
   openssl enc -aes-256-cbc -salt \
     -in "$KEYSTORE_PATH" \
     -out "$BACKUP_DIR/smilepile-production-$TIMESTAMP.keystore.enc" \
     -pass pass:"$ENCRYPTION_KEY"

   # Verify backup integrity
   openssl enc -aes-256-cbc -d \
     -in "$BACKUP_DIR/smilepile-production-$TIMESTAMP.keystore.enc" \
     -pass pass:"$ENCRYPTION_KEY" | \
     shasum -a 256 | \
     diff - <(shasum -a 256 "$KEYSTORE_PATH")

   echo "Encrypted backup created: $BACKUP_DIR/smilepile-production-$TIMESTAMP.keystore.enc"
   ```

4. **Document decryption procedure**:
   ```bash
   # Restore from encrypted backup
   openssl enc -aes-256-cbc -d \
     -in smilepile-production.keystore.enc \
     -out smilepile-production.keystore \
     -pass pass:"$BACKUP_KEY"

   chmod 600 smilepile-production.keystore
   ```

**Verification**:
```bash
# Verify backups are encrypted
file ~/iCloud/SmilePile-Credentials/*.enc
# Expected: "data" (not recognizable format)

# Verify restoration works
openssl enc -aes-256-cbc -d \
  -in backup.keystore.enc \
  -out restored.keystore \
  -pass pass:"TEST_KEY"

keytool -list -keystore restored.keystore
# Expected: Certificate details displayed

# Verify SHA256 matches original
shasum -a 256 original.keystore restored.keystore
# Expected: Matching hashes
```

---

### CRITICAL-03: Weak Password Generation Allows Predictable Passwords

**Severity**: CRITICAL
**Category**: Credential Generation
**Location**: Implementation Plan Section 4, Hour 10 (lines 486-498)

**Description**:
The implementation plan uses `openssl rand -base64 32` for password generation, which is executed in user's shell and may have these problems:

1. **Predictable randomness** if system entropy is low (fresh VM, container)
2. **Password stored in shell history** if not properly handled
3. **No complexity requirements** (base64 may not include special characters)
4. **No verification** that generated password meets minimum security standards

Example from plan:
```bash
STORE_PASS=$(openssl rand -base64 32)
KEY_PASS=$(openssl rand -base64 32)
```

If `/dev/urandom` entropy is low (common in VMs and containers), these passwords may be predictable.

**Impact**:
- Weak passwords can be brute-forced within days (GPU attacks)
- Predictable PRNG state allows precomputing password spaces
- Attacker with keystore + weak password = signing capability
- Compromised signing = malicious app distribution

**Likelihood**: MEDIUM (requires specific conditions but high impact)

**Mitigation**:
1. **Use strong password generation with verification**:
   ```bash
   # Generate strong password with mixed character types
   generate_strong_password() {
     local length="${1:-32}"
     local password

     # Use /dev/random for critical passwords (blocks until sufficient entropy)
     # OR use /dev/urandom with entropy check
     if [ $(cat /proc/sys/kernel/random/entropy_avail) -lt 1000 ]; then
       echo "ERROR: Insufficient entropy for password generation" >&2
       echo "Available entropy: $(cat /proc/sys/kernel/random/entropy_avail)" >&2
       return 1
     fi

     # Generate password with multiple character classes
     password=$(LC_ALL=C tr -dc 'A-Za-z0-9!@#$%^&*()_+-=[]{}|;:,.<>?' < /dev/urandom | head -c "$length")

     # Verify complexity requirements
     if ! echo "$password" | grep -q '[A-Z]'; then
       echo "ERROR: Password missing uppercase letter" >&2
       return 1
     fi
     if ! echo "$password" | grep -q '[a-z]'; then
       echo "ERROR: Password missing lowercase letter" >&2
       return 1
     fi
     if ! echo "$password" | grep -q '[0-9]'; then
       echo "ERROR: Password missing digit" >&2
       return 1
     fi
     if ! echo "$password" | grep -q '[^A-Za-z0-9]'; then
       echo "ERROR: Password missing special character" >&2
       return 1
     fi

     echo "$password"
   }

   # Generate passwords
   STORE_PASS=$(generate_strong_password 40)
   KEY_PASS=$(generate_strong_password 40)

   # Verify passwords are different
   if [ "$STORE_PASS" = "$KEY_PASS" ]; then
     echo "ERROR: Generated identical passwords (PRNG failure)"
     exit 1
   fi
   ```

2. **Avoid shell history leaks**:
   ```bash
   # Disable history for sensitive commands
   set +o history

   STORE_PASS=$(generate_strong_password 40)
   KEY_PASS=$(generate_strong_password 40)

   # IMMEDIATELY store in password manager (manual step)
   echo "CRITICAL: Store these passwords in password manager NOW:"
   echo "Store Password: $STORE_PASS"
   echo "Key Password: $KEY_PASS"
   read -p "Press ENTER after passwords are securely stored..."

   # Clear variables
   unset STORE_PASS
   unset KEY_PASS

   set -o history
   ```

3. **Alternative: Use password manager's generator**:
   ```bash
   # 1Password CLI example (more secure)
   STORE_PASS=$(op generate -r "A-Za-z0-9!@#$%^&*" 40)
   KEY_PASS=$(op generate -r "A-Za-z0-9!@#$%^&*" 40)

   # Immediately store in vault
   op item create \
     --category=password \
     --title="SmilePile Production Keystore" \
     --vault="SmilePile Credentials" \
     storePassword="$STORE_PASS" \
     keyPassword="$KEY_PASS"
   ```

4. **Measure password entropy**:
   ```bash
   # Calculate Shannon entropy
   calculate_entropy() {
     local password="$1"
     echo "$password" | awk '
       BEGIN { ent=0 }
       {
         len=length($0)
         for (i=1; i<=len; i++) {
           c=substr($0,i,1)
           freq[c]++
         }
         for (c in freq) {
           p=freq[c]/len
           ent-=p*log(p)/log(2)
         }
         print ent " bits per character"
         print ent*len " total bits"
       }
     '
   }

   # Minimum 128 bits for production keystore
   calculate_entropy "$STORE_PASS"
   ```

**Verification**:
```bash
# Verify password strength
echo "$STORE_PASS" | grep -E '^.{32,}$'  # Length >= 32
echo "$STORE_PASS" | grep -E '[A-Z]'    # Uppercase
echo "$STORE_PASS" | grep -E '[a-z]'    # Lowercase
echo "$STORE_PASS" | grep -E '[0-9]'    # Digit
echo "$STORE_PASS" | grep -E '[^A-Za-z0-9]'  # Special char

# Verify passwords are different
[ "$STORE_PASS" != "$KEY_PASS" ] && echo "PASS: Passwords are unique"

# Verify entropy
calculate_entropy "$STORE_PASS" | grep "total bits" | awk '{print $1 >= 128}'
# Expected: 1 (true)
```

---

### CRITICAL-04: Service Account JSON Has Excessive Permissions

**Severity**: CRITICAL
**Category**: Access Control
**Location**: Implementation Plan Section 3, Hour 9 (lines 412-426)

**Description**:
The plan grants the Play Console service account "Release Manager" role, which includes:
- Manage all releases across all tracks
- Promote releases between tracks
- Access production release keys
- Modify app content
- Read user metrics and analytics

This violates the Principle of Least Privilege. The automation only needs:
- Upload AAB to specific testing tracks
- Read upload status

**Impact**:
- Compromised service account can push malicious updates to production
- Attacker can promote untested builds to production instantly
- No human approval required for production releases
- Single JSON file compromise = full production control

**Likelihood**: MEDIUM (service account JSON is stored in multiple locations)

**Mitigation**:
1. **Create separate service accounts per environment**:
   ```bash
   # Internal Testing service account
   gcloud iam service-accounts create smilepile-internal-testing \
     --display-name="SmilePile Internal Testing Uploads"

   # Closed Testing service account
   gcloud iam service-accounts create smilepile-closed-testing \
     --display-name="SmilePile Closed Testing Uploads"

   # Production service account (manual use only)
   gcloud iam service-accounts create smilepile-production-deploy \
     --display-name="SmilePile Production Releases"
   ```

2. **Grant minimal permissions in Play Console**:
   ```
   Internal Testing Account:
   - Role: Create Custom Role with ONLY:
     - Upload app bundles to Internal Testing
     - Read upload status

   Closed Testing Account:
   - Role: Create Custom Role with ONLY:
     - Upload app bundles to Closed Testing
     - Read upload status

   Production Account:
   - Role: Release Manager (but NOT stored in deployment system)
     - Requires manual human approval to use
     - JSON key stored only in password manager
     - Never exposed to CI/CD
   ```

3. **Implement custom IAM role with minimal permissions**:
   ```yaml
   # custom-play-console-upload-role.yaml
   title: SmilePile Internal Testing Upload
   description: Minimal permissions for internal testing uploads
   stage: GA
   includedPermissions:
   - androidpublisher.tracks.create
   - androidpublisher.tracks.update
   - androidpublisher.edits.commit
   - androidpublisher.edits.insert
   - androidpublisher.edits.validate
   ```

4. **Separate credentials by tier**:
   ```bash
   # deploy/secrets/staging.env
   PLAY_CONSOLE_JSON_PATH=~/play-console-credentials/internal-testing.json

   # deploy/secrets/production.env (manual only)
   # PLAY_CONSOLE_JSON_PATH=~/play-console-credentials/production.json
   # NOTE: Production deploys require manual approval
   ```

5. **Rotate service accounts quarterly**:
   ```bash
   # Add to calendar: Every 90 days
   # 1. Create new service account JSON key
   # 2. Test with staging deployment
   # 3. Update deploy/secrets/*.env
   # 4. Delete old service account key
   # 5. Document rotation in audit log
   ```

**Verification**:
```bash
# Verify service account has minimal permissions
gcloud projects get-iam-policy PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:smilepile-internal-testing@*"
# Expected: Custom role with limited permissions only

# Verify production account is NOT in deployment secrets
grep -r "production.*service.*account" deploy/secrets/
# Expected: No results (production manual only)

# Test that internal testing account CANNOT access production
fastlane supply --track production --json_key internal-testing.json
# Expected: Permission denied error
```

---

### CRITICAL-05: No Verification of App Store Connect API Key Permissions

**Severity**: CRITICAL
**Category**: Access Control
**Location**: Implementation Plan Section 2, Hour 4 (lines 142-193)

**Description**:
The plan generates an App Store Connect API key with "App Manager" role but doesn't verify:
1. Actual permissions granted to the key
2. Key expiration date
3. Key revocation status
4. Which apps the key can access

The "App Manager" role includes:
- Upload builds to all apps in account
- Modify app metadata
- Manage TestFlight testers
- Access sales and analytics
- Manage app pricing

This is excessive for automated builds which only need:
- Upload builds to TestFlight
- Manage Internal Testing testers

**Impact**:
- Compromised API key can modify any app in the account
- Attacker can upload malicious builds to all apps
- Production app metadata can be changed
- Sales data can be exfiltrated

**Likelihood**: MEDIUM (API key stored in multiple locations)

**Mitigation**:
1. **Create app-specific API keys** (if supported):
   ```
   App Store Connect → Users and Access → Keys

   Key Name: SmilePile TestFlight Upload
   Access: Developer (NOT App Manager)
   App Access: SmilePile only (restrict to single app)

   Permissions needed:
   - Access TestFlight (read/write)
   - Upload builds (write)
   ```

2. **If app-specific keys not available, create separate keys per environment**:
   ```
   Key 1: SmilePile Internal Testing
   - Role: Developer
   - Used by: deploy_stage.sh

   Key 2: SmilePile Beta Testing
   - Role: Developer
   - Used by: deploy_beta.sh

   Key 3: SmilePile Production
   - Role: App Manager (required for production)
   - Used by: Manual deployment only (NOT automated)
   ```

3. **Verify API key permissions after creation**:
   ```bash
   # Test API key capabilities
   fastlane pilot list \
     --api_key_path "$ASC_KEY_PATH" \
     --api_key "$ASC_KEY_ID" \
     --issuer_id "$ASC_ISSUER_ID" \
     --verbose

   # Expected: List of TestFlight groups (success)

   # Test that key CANNOT access production
   fastlane deliver --verify_only \
     --api_key_path "$ASC_KEY_PATH" \
     --api_key "$ASC_KEY_ID" \
     --issuer_id "$ASC_ISSUER_ID"

   # Expected: Permission denied (if Developer role)
   ```

4. **Set API key expiration reminder**:
   ```bash
   # App Store Connect API keys don't expire by default
   # But should be rotated every 90 days

   # Add calendar reminder
   echo "Rotate App Store Connect API keys" | \
     calendar add --date $(date -v+90d "+%Y-%m-%d")
   ```

5. **Document which operations each key can perform**:
   ```markdown
   # deploy/docs/API_KEY_PERMISSIONS.md

   ## SmilePile TestFlight Upload Key
   - Key ID: ABC123XYZ4
   - Role: Developer
   - Can: Upload builds, manage internal testers
   - Cannot: Access production, modify pricing, view analytics
   - Rotation: Every 90 days
   - Last rotated: 2025-10-13
   - Next rotation: 2026-01-11
   ```

**Verification**:
```bash
# Verify key is limited to TestFlight
fastlane pilot list --api_key_path "$ASC_KEY_PATH"
# Expected: Success

fastlane deliver --api_key_path "$ASC_KEY_PATH" --verify_only
# Expected: Permission denied (Developer role cannot access production)

# Verify key metadata
cat "$ASC_KEY_PATH" | openssl asn1parse
# Expected: Valid P8 format

# Set up rotation reminder
grep "Rotate.*API.*key" ~/.calendar
# Expected: Reminder scheduled
```

---

### CRITICAL-06: API Keys Transmitted via Environment Variables

**Severity**: CRITICAL
**Category**: Credential Transmission
**Location**: Implementation Plan Section 6, Hour 14 (lines 776-806)

**Description**:
The plan loads API keys and credentials into environment variables for testing:
```bash
export ASC_KEY_ID="ABC123XYZ4"
export ASC_ISSUER_ID="XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
export ASC_KEY_PATH="~/app-store-connect-api-keys/SmilePile-AuthKey.p8"
```

Environment variables are visible to:
1. Any process running as the same user (`ps e` shows env vars)
2. Core dumps (if process crashes)
3. Process monitoring tools (Activity Monitor, top, htop)
4. Debugging tools (lldb, gdb can inspect process memory)
5. Malware with ptrace capabilities

**Impact**:
- Malware can scrape API keys from running processes
- Core dumps may contain credentials
- Shared systems expose credentials to other users
- Debugging sessions leak credentials in logs

**Likelihood**: MEDIUM-HIGH (environment variable attacks are common)

**Mitigation**:
1. **Pass credentials via secure file descriptors**:
   ```bash
   # Create temporary credential file with restricted permissions
   CRED_FILE=$(mktemp)
   chmod 600 "$CRED_FILE"

   cat > "$CRED_FILE" <<EOF
   ASC_KEY_ID=ABC123XYZ4
   ASC_ISSUER_ID=XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
   ASC_KEY_PATH=~/app-store-connect-api-keys/SmilePile-AuthKey.p8
   EOF

   # Pass via file descriptor (not visible in ps)
   exec 3< "$CRED_FILE"
   rm "$CRED_FILE"  # Delete immediately, still accessible via fd 3

   # Read credentials in script
   while IFS='=' read -r -u 3 key value; do
     export "$key"="$value"
   done

   # Close file descriptor
   exec 3<&-
   ```

2. **Use fastlane environment variables with caution**:
   ```bash
   # Better: Pass credentials via command-line arguments
   # (Still visible in ps, but shorter exposure window)
   fastlane pilot list \
     --api_key_path "$(cat deploy/secrets/production.env | grep ASC_KEY_PATH | cut -d= -f2)" \
     --api_key "$(cat deploy/secrets/production.env | grep ASC_KEY_ID | cut -d= -f2)" \
     --issuer_id "$(cat deploy/secrets/production.env | grep ASC_ISSUER_ID | cut -d= -f2)"
   ```

3. **Best: Use fastlane's App Store Connect authentication file**:
   ```bash
   # Create App Store Connect API key JSON
   cat > ~/.fastlane/asc_key.json <<EOF
   {
     "key_id": "ABC123XYZ4",
     "issuer_id": "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX",
     "key": "$(cat ~/app-store-connect-api-keys/SmilePile-AuthKey.p8)"
   }
   EOF
   chmod 600 ~/.fastlane/asc_key.json

   # Use in Fastfile (no environment variables)
   lane :deploy_testflight do
     api_key = app_store_connect_api_key(
       key_id: ENV['ASC_KEY_ID'],
       issuer_id: ENV['ASC_ISSUER_ID'],
       key_filepath: ENV['ASC_KEY_PATH']
     )

     pilot(
       api_key: api_key,
       skip_waiting_for_build_processing: true
     )
   end
   ```

4. **Clear environment variables after use**:
   ```bash
   # After deployment completes
   unset ASC_KEY_ID
   unset ASC_ISSUER_ID
   unset ASC_KEY_PATH
   unset PLAY_CONSOLE_JSON_PATH
   unset ANDROID_KEYSTORE_PASSWORD
   unset ANDROID_KEY_PASSWORD
   ```

5. **Monitor for credential leaks in process listings**:
   ```bash
   # Add to security checks
   if ps e | grep -i "password\|key\|secret"; then
     echo "WARNING: Credentials visible in process listing"
     exit 1
   fi
   ```

**Verification**:
```bash
# Verify credentials are NOT visible in process listing
ps e | grep fastlane | grep -i "password\|key_id\|issuer"
# Expected: No matches

# Verify credentials are NOT in core dumps
ulimit -c 0  # Disable core dumps
ulimit -c    # Verify
# Expected: 0

# Verify environment is cleared after deployment
env | grep -i "asc\|play\|keystore"
# Expected: No matches after deployment completes
```

---

### CRITICAL-07: No Keystore Password Rotation Strategy

**Severity**: CRITICAL
**Category**: Credential Lifecycle
**Location**: Missing from Implementation Plan

**Description**:
The implementation plan generates keystore passwords once but never rotates them. Android keystores can have their passwords changed without regenerating the keystore, but the plan doesn't document this capability or establish a rotation schedule.

Problems:
1. **Passwords never expire** - Increases risk of compromise over time
2. **No rotation procedure** - If password is suspected compromised, no documented recovery
3. **No audit trail** - No record of when passwords were last changed
4. **No access revocation** - When team members leave, password remains unchanged

**Impact**:
- Compromised password remains valid indefinitely
- Former team members retain access to keystore
- Compliance failures (PCI-DSS requires password rotation)
- Audit findings during security reviews

**Likelihood**: MEDIUM (passwords WILL need rotation eventually)

**Mitigation**:
1. **Document keystore password rotation procedure**:
   ```bash
   # deploy/docs/KEYSTORE_PASSWORD_ROTATION.md

   ## Rotating Keystore Passwords

   ### When to Rotate
   - Every 90 days (calendar reminder)
   - When team member with access leaves
   - When password compromise suspected
   - After security incident

   ### Rotation Procedure

   1. Generate new strong password:
      STORE_PASS_NEW=$(generate_strong_password 40)
      KEY_PASS_NEW=$(generate_strong_password 40)

   2. Change keystore password:
      keytool -storepasswd \
        -keystore ~/keystores/smilepile-production.keystore \
        -storepass "$STORE_PASS_OLD" \
        -new "$STORE_PASS_NEW"

   3. Change key password:
      keytool -keypasswd \
        -keystore ~/keystores/smilepile-production.keystore \
        -alias smilepile-release \
        -storepass "$STORE_PASS_NEW" \
        -keypass "$KEY_PASS_OLD" \
        -new "$KEY_PASS_NEW"

   4. Verify new passwords work:
      keytool -list \
        -keystore ~/keystores/smilepile-production.keystore \
        -storepass "$STORE_PASS_NEW" \
        -keypass "$KEY_PASS_NEW"

   5. Update password manager:
      - Store new passwords
      - Add note: "Rotated on YYYY-MM-DD"
      - Keep old passwords for 30 days (rollback)

   6. Update encrypted backups:
      ./deploy/scripts/backup_keystore.sh \
        ~/keystores/smilepile-production.keystore \
        ~/iCloud/SmilePile-Credentials/ \
        "$BACKUP_ENCRYPTION_KEY"

   7. Test deployment with new passwords:
      ./deploy/deploy_qual.sh --skip-tests

   8. Document rotation:
      echo "$(date): Rotated keystore passwords" >> \
        deploy/docs/ROTATION_AUDIT_LOG.md

   9. Set next rotation reminder:
      calendar add "Rotate keystore passwords" $(date -v+90d)
   ```

2. **Create rotation automation script**:
   ```bash
   # deploy/scripts/rotate_keystore_password.sh
   #!/bin/bash
   set -euo pipefail

   KEYSTORE_PATH="${1:-~/keystores/smilepile-production.keystore}"
   ALIAS="${2:-smilepile-release}"

   echo "=== Keystore Password Rotation ==="
   echo "Keystore: $KEYSTORE_PATH"
   echo "Alias: $ALIAS"
   echo ""

   # Prompt for old passwords
   read -sp "Enter current store password: " STORE_PASS_OLD
   echo ""
   read -sp "Enter current key password: " KEY_PASS_OLD
   echo ""

   # Verify old passwords work
   if ! keytool -list -keystore "$KEYSTORE_PATH" \
        -storepass "$STORE_PASS_OLD" &>/dev/null; then
     echo "ERROR: Current store password is incorrect"
     exit 1
   fi

   # Generate new passwords
   echo "Generating new passwords..."
   STORE_PASS_NEW=$(generate_strong_password 40)
   KEY_PASS_NEW=$(generate_strong_password 40)

   # Change passwords
   echo "Rotating store password..."
   keytool -storepasswd \
     -keystore "$KEYSTORE_PATH" \
     -storepass "$STORE_PASS_OLD" \
     -new "$STORE_PASS_NEW"

   echo "Rotating key password..."
   keytool -keypasswd \
     -keystore "$KEYSTORE_PATH" \
     -alias "$ALIAS" \
     -storepass "$STORE_PASS_NEW" \
     -keypass "$KEY_PASS_OLD" \
     -new "$KEY_PASS_NEW"

   # Verify new passwords
   if ! keytool -list -keystore "$KEYSTORE_PATH" \
        -storepass "$STORE_PASS_NEW" &>/dev/null; then
     echo "ERROR: Password rotation failed"
     echo "CRITICAL: Keystore may be corrupted"
     exit 1
   fi

   echo ""
   echo "SUCCESS: Passwords rotated successfully"
   echo ""
   echo "NEXT STEPS:"
   echo "1. Store new passwords in password manager"
   echo "   Store Password: $STORE_PASS_NEW"
   echo "   Key Password: $KEY_PASS_NEW"
   echo "2. Update deploy/secrets/production.env"
   echo "3. Create new encrypted backups"
   echo "4. Test deployment: ./deploy/deploy_qual.sh --skip-tests"
   echo "5. Set calendar reminder for next rotation (90 days)"
   ```

3. **Set up calendar reminders**:
   ```bash
   # Add to deployment completion checklist
   echo "Set calendar reminder for keystore password rotation (90 days)" >> \
     wave-evidence/wave-1/completion-checklist.md
   ```

4. **Implement access revocation procedure**:
   ```markdown
   # deploy/docs/TEAM_OFFBOARDING.md

   ## When Team Member Leaves

   1. Immediate (same day):
      - Revoke App Store Connect access
      - Revoke Play Console access
      - Remove from password manager shared vault

   2. Within 24 hours:
      - Rotate all keystore passwords
      - Rotate App Store Connect API keys
      - Rotate Play Console service account keys
      - Update all deployment secrets

   3. Within 1 week:
      - Audit access logs for unusual activity
      - Review all credentials they had access to
      - Document offboarding in security log
   ```

**Verification**:
```bash
# Verify rotation procedure exists
ls -lh deploy/docs/KEYSTORE_PASSWORD_ROTATION.md
# Expected: File exists

# Verify rotation script works
./deploy/scripts/rotate_keystore_password.sh --test-mode
# Expected: Dry-run success

# Verify calendar reminder is set
grep "Rotate.*keystore.*password" ~/.calendar
# Expected: Reminder scheduled for 90 days

# Verify old passwords don't work after rotation
keytool -list -keystore ~/keystores/smilepile-production.keystore \
  -storepass "$OLD_PASSWORD"
# Expected: Error (password incorrect)
```

---

### CRITICAL-08: Git History Audit Incomplete (Missing Blob Search)

**Severity**: CRITICAL
**Category**: Git Security
**Location**: Implementation Plan Section 5, Hour 13 (lines 666-691)

**Description**:
The git history audit searches for file paths but not file contents in git blobs:

```bash
# Searches for committed files
git log --all --full-history --source -- "*.keystore" "*.jks"

# Does NOT search for secrets in blob contents
# Missing: git grep searching ALL commits
```

This misses secrets that were:
1. Committed in files with generic names (secrets.txt, config.json)
2. Embedded in code comments
3. Committed and then renamed
4. In commits that deleted the file

**Impact**:
- Secrets in git history remain accessible indefinitely
- Anyone with repository access can find historical secrets
- GitHub search can expose secrets
- Forked repositories contain secret history

**Likelihood**: MEDIUM (secrets are often accidentally committed)

**Mitigation**:
1. **Search git blob contents, not just file paths**:
   ```bash
   # deploy/scripts/audit_git_history.sh
   #!/bin/bash

   echo "=== Comprehensive Git History Secret Scan ==="

   # 1. Search for keystore files
   echo "Searching for committed keystores..."
   git log --all --full-history --source -- "*.keystore" "*.jks" "*.p12" "*.p8"

   # 2. Search blob contents for passwords
   echo "Searching for password patterns in all commits..."
   git grep -i "password\s*=" $(git rev-list --all) | \
     grep -v "example" | \
     grep -v "placeholder" | \
     grep -v "change_me"

   # 3. Search for API key patterns
   echo "Searching for API key patterns..."
   git grep -E "(AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{35}|AuthKey_[A-Z0-9]{10})" \
     $(git rev-list --all)

   # 4. Search for private keys
   echo "Searching for private key headers..."
   git grep "BEGIN.*PRIVATE KEY" $(git rev-list --all)

   # 5. Search for service account JSON
   echo "Searching for service account patterns..."
   git grep "service_account" $(git rev-list --all) | \
     grep -i "private_key\|client_email"

   # 6. Search for keystore passwords
   echo "Searching for keystore password patterns..."
   git grep -E "storePassword|keyPassword" $(git rev-list --all) | \
     grep -v "example" | \
     grep -v "\$\{" | \  # Ignore variable references
     grep -v "change_me"

   # 7. Check for secrets in commit messages
   echo "Searching commit messages for secrets..."
   git log --all --grep="password" --grep="api.key" --grep="secret" -i

   echo ""
   echo "=== Scan Complete ==="
   echo "Review output above for any exposed secrets"
   ```

2. **Use dedicated secret scanning tools**:
   ```bash
   # Install gitleaks
   brew install gitleaks  # macOS

   # Scan entire history
   gitleaks detect --source . --verbose --report-path gitleaks-report.json

   # Check results
   if [ -f gitleaks-report.json ]; then
     FINDINGS=$(jq 'length' gitleaks-report.json)
     if [ "$FINDINGS" -gt 0 ]; then
       echo "ERROR: Found $FINDINGS potential secrets in git history"
       jq '.[].Description' gitleaks-report.json
       exit 1
     fi
   fi
   ```

3. **If secrets found, rewrite history**:
   ```bash
   # WARNING: Requires coordination with all team members
   # Creates new commit hashes, breaking all forks/clones

   # Option 1: BFG Repo-Cleaner (recommended)
   brew install bfg

   bfg --delete-files "*.keystore" \
       --delete-files "*.p8" \
       --delete-files "*service-account.json" \
       --replace-text passwords.txt \
       SmilePile.git

   # Option 2: git-filter-repo
   pip install git-filter-repo

   git filter-repo --path-match "*.keystore" --invert-paths
   git filter-repo --replace-text <(echo "password=.*==>password=REDACTED")

   # After rewriting
   git reflog expire --expire=now --all
   git gc --prune=now --aggressive

   # Force push (DANGEROUS - coordinate with team)
   git push --force --all origin
   git push --force --tags origin
   ```

4. **Set up pre-commit hooks**:
   ```bash
   # Install pre-commit framework
   pip install pre-commit

   # .pre-commit-config.yaml
   repos:
   - repo: https://github.com/gitleaks/gitleaks
     rev: v8.18.0
     hooks:
     - id: gitleaks

   - repo: https://github.com/Yelp/detect-secrets
     rev: v1.4.0
     hooks:
     - id: detect-secrets
       args: ['--baseline', '.secrets.baseline']

   # Install hooks
   pre-commit install

   # Test
   echo "password=secret123" > test.txt
   git add test.txt
   git commit -m "test"
   # Expected: Commit blocked by pre-commit hook
   ```

5. **GitHub secret scanning**:
   ```bash
   # Enable in GitHub repository settings
   # Settings → Security → Code security and analysis
   # Enable: Secret scanning
   # Enable: Push protection

   # This prevents pushes containing secrets
   ```

**Verification**:
```bash
# Run comprehensive scan
./deploy/scripts/audit_git_history.sh > /tmp/git_audit.log 2>&1

# Check for findings
if grep -q "password.*=.*[^change_me]" /tmp/git_audit.log; then
  echo "ERROR: Found potential secrets in git history"
  cat /tmp/git_audit.log
  exit 1
fi

# Verify pre-commit hooks installed
ls -la .git/hooks/pre-commit
# Expected: File exists and is executable

# Test pre-commit hook
echo "AKIA1234567890123456" > test_secret.txt
git add test_secret.txt
git commit -m "test"
# Expected: Commit blocked with "AWS Access Key detected"
```

---

### CRITICAL-09: Single Point of Failure (Adam Stack)

**Severity**: CRITICAL
**Category**: Business Continuity
**Location**: Throughout Implementation Plan

**Description**:
The entire Wave 1 plan assumes Adam Stack (primary) has sole access to:
1. Apple Developer account (Team ID 84W9WSYQQB)
2. Google Play Console account (if created with his email)
3. Password manager with all credentials
4. Physical backup drives
5. Keystore generation and password knowledge

Risks:
- **Vacation/illness**: Deployments blocked if Adam unavailable
- **Account compromise**: Single email compromise = total access
- **Departure**: Credential handoff process undefined
- **Disaster**: If Adam loses access, recovery undefined
- **Scalability**: Cannot onboard new developers without Adam

**Impact**:
- Deployment system unusable if Adam unavailable
- Security incidents cannot be responded to quickly
- Team growth blocked (cannot safely share access)
- Regulatory compliance failures (separation of duties)
- Vendor lock-in to Adam's personal accounts

**Likelihood**: HIGH (team member unavailability is inevitable)

**Mitigation**:
1. **Establish secondary account administrator**:
   ```markdown
   # deploy/docs/TEAM_ACCESS_MATRIX.md

   ## Account Access Matrix

   | Resource | Primary | Secondary | Emergency |
   |----------|---------|-----------|-----------|
   | Apple Developer Account | Adam Stack (Admin) | [Team Member] (Admin) | [External Contact] |
   | App Store Connect | Adam Stack (Admin) | [Team Member] (Developer) | [External Contact] |
   | Play Console | Adam Stack (Owner) | [Team Member] (Admin) | [External Contact] |
   | Password Manager | Adam Stack (Owner) | [Team Member] (Admin) | Recovery Key in Bank Vault |
   | Keystore Backups | Adam Stack | [Team Member] | [External Contact] |
   | API Keys | Adam Stack | [Team Member] | Documented in Emergency Procedures |
   ```

2. **Create emergency access procedures**:
   ```bash
   # deploy/docs/EMERGENCY_ACCESS.md

   ## Emergency Credential Access

   ### Scenario: Primary unavailable (vacation, illness)

   1. Secondary admin uses shared password manager vault
   2. Access keystores from encrypted backup locations
   3. Decrypt credentials using backup encryption key
   4. Proceed with deployment following standard procedures

   ### Scenario: Primary account compromised

   1. Secondary admin immediately rotates all credentials:
      - Revoke App Store Connect API keys
      - Revoke Play Console service accounts
      - Rotate keystore passwords
      - Generate new API keys
   2. Audit access logs for unauthorized activity
   3. Update all deployment secrets
   4. Document incident in security log

   ### Scenario: Primary departed (resignation, termination)

   1. Within 24 hours:
      - Transfer Apple Developer account ownership
      - Transfer Play Console ownership
      - Transfer password manager vault ownership
      - Rotate ALL credentials (assume compromised)
   2. Within 1 week:
      - Audit all access during transition
      - Review and update team access matrix
      - Update emergency contact information

   ### Scenario: Complete credential loss (disaster)

   1. Immediate:
      - Contact Apple Developer Support (Team ID: 84W9WSYQQB)
      - Contact Google Play Support (package: com.smilepile)
   2. Restore from backups:
      - Location 1: Encrypted cloud storage
      - Location 2: External encrypted drive
      - Location 3: Password manager vault
   3. If backups unavailable:
      - For iOS: Request new distribution certificate
      - For Android: If using Play App Signing, request upload key reset
      - For Android: If NOT using Play App Signing = CATASTROPHIC
   ```

3. **Share critical access immediately (Wave 1 Week 1)**:
   ```bash
   # After keystore generation (same day)

   # 1. Add secondary admin to Apple Developer account
   #    App Store Connect → Users and Access → Add User
   #    Email: [secondary@example.com]
   #    Role: Admin

   # 2. Add secondary admin to Play Console
   #    Play Console → Users and permissions → Invite new users
   #    Email: [secondary@example.com]
   #    Role: Admin

   # 3. Share password manager vault
   #    1Password: Shared vault "SmilePile Credentials"
   #    Add user: [secondary@example.com]
   #    Permissions: Can manage vault

   # 4. Share backup access
   #    Cloud backup: Share iCloud folder or use team storage
   #    External drive: Provide physical access or duplicate
   #    Document locations in shared team wiki
   ```

4. **Implement separation of duties**:
   ```markdown
   # deploy/docs/DEPLOYMENT_ROLES.md

   ## Deployment Role Matrix

   | Action | Primary | Approver | Secondary |
   |--------|---------|----------|-----------|
   | QUAL deploy | Any developer | None | N/A |
   | STAGE deploy | Developer | Tech Lead | N/A |
   | BETA deploy | Tech Lead | Product Manager | N/A |
   | PROD deploy | Tech Lead | Product Manager + CTO | Required |

   ## Credential Access Roles

   | Credential | Generate | Approve | Store | Use |
   |------------|----------|---------|-------|-----|
   | Keystore | Primary | Security Team | Both | Automated |
   | API Keys | Primary | Security Team | Both | Automated |
   | Service Accounts | Primary | Security Team | Both | Automated |
   | Production passwords | Primary | Secondary | Both | Manual Only |
   ```

5. **Test emergency procedures quarterly**:
   ```bash
   # deploy/scripts/test_emergency_access.sh
   #!/bin/bash

   echo "=== Emergency Access Drill ==="
   echo "Testing secondary access to all systems"

   # Test 1: Secondary can access password manager
   echo "1. Testing password manager access..."
   # [Secondary performs this test]

   # Test 2: Secondary can restore from backups
   echo "2. Testing backup restoration..."
   # [Secondary performs this test]

   # Test 3: Secondary can perform deployment
   echo "3. Testing deployment capability..."
   # [Secondary performs QUAL deployment]

   # Test 4: Emergency contacts respond
   echo "4. Testing emergency contact reachability..."
   # [Send test message to emergency contacts]

   echo ""
   echo "=== Drill Complete ==="
   echo "Document results in deploy/docs/EMERGENCY_DRILL_LOG.md"
   ```

**Verification**:
```bash
# Verify secondary has access
# [Secondary performs these checks]

# 1. Can log into App Store Connect
open "https://appstoreconnect.apple.com"
# Expected: Login successful, can see SmilePile app

# 2. Can log into Play Console
open "https://play.google.com/console"
# Expected: Login successful, can see SmilePile app

# 3. Can access password manager
op vault list | grep "SmilePile Credentials"
# Expected: Vault visible

# 4. Can restore from backup
cp ~/iCloud/SmilePile-Credentials/smilepile-production.keystore.enc /tmp/
openssl enc -aes-256-cbc -d -in /tmp/smilepile-production.keystore.enc -out /tmp/restored.keystore
keytool -list -keystore /tmp/restored.keystore
# Expected: Certificate details displayed

# 5. Can perform deployment
./deploy/deploy_qual.sh --skip-tests
# Expected: Successful deployment
```

---

## 2. HIGH-RISK ISSUES (Should Fix Before Implementation)

### HIGH-01: No Monitoring for Unauthorized Access Attempts

**Severity**: HIGH
**Category**: Monitoring & Detection
**Location**: Missing from Implementation Plan

**Description**:
The plan creates credentials and stores them securely, but has no monitoring for:
1. Failed login attempts to App Store Connect
2. Failed API key authentication attempts
3. Unauthorized access to Play Console
4. Keystore access attempts (file system monitoring)
5. Unusual deployment patterns

**Impact**:
- Compromised credentials go undetected
- Attackers have unlimited time to exploit access
- No forensic evidence after security incidents
- Cannot detect credential stuffing or brute force attacks

**Likelihood**: MEDIUM (monitoring is often overlooked until after incident)

**Mitigation**:
1. **Enable App Store Connect notifications**:
   ```
   App Store Connect → Account → Notifications
   Enable:
   - New user added
   - API key created
   - API key revoked
   - Unusual activity
   ```

2. **Enable Play Console security alerts**:
   ```
   Play Console → Settings → Email preferences
   Enable:
   - Security alerts
   - Unusual activity
   - Account changes
   ```

3. **Monitor API key usage**:
   ```bash
   # deploy/scripts/monitor_api_usage.sh
   #!/bin/bash

   # Check App Store Connect API usage
   fastlane pilot list \
     --api_key_path "$ASC_KEY_PATH" 2>&1 | \
     tee -a /var/log/smilepile/asc_access.log

   # Alert on failures
   if [ ${PIPESTATUS[0]} -ne 0 ]; then
     echo "ALERT: App Store Connect API authentication failed" | \
       mail -s "SmilePile Security Alert" security@example.com
   fi
   ```

4. **File system monitoring for keystore access**:
   ```bash
   # Install fswatch (macOS)
   brew install fswatch

   # Monitor keystore directory
   fswatch -0 ~/keystores/ | while read -d "" event; do
     echo "$(date): Keystore access: $event" >> /var/log/smilepile/keystore_access.log

     # Alert on unexpected access
     if ! pgrep -f "gradle\|deploy_.*\.sh" > /dev/null; then
       echo "ALERT: Unexpected keystore access outside deployment" | \
         mail -s "SmilePile Security Alert" security@example.com
     fi
   done &
   ```

5. **Centralized security logging**:
   ```bash
   # Send all security logs to centralized service
   # Option 1: Splunk, Datadog, New Relic
   # Option 2: Simple: Send to secure S3 bucket

   aws s3 cp /var/log/smilepile/keystore_access.log \
     s3://smilepile-security-logs/$(date +%Y%m%d)/keystore_access.log
   ```

**Verification**:
```bash
# Verify monitoring is active
pgrep -f "fswatch.*keystores"
# Expected: Process ID

# Verify log rotation configured
ls -lh /var/log/smilepile/*.log
# Expected: Log files present

# Test alert system
touch ~/keystores/test_alert
# Expected: Alert email received
```

---

### HIGH-02: Backup Restoration Not Tested Under Realistic Conditions

**Severity**: HIGH
**Category**: Disaster Recovery
**Location**: Implementation Plan Section 6, Hour 15 (lines 829-900)

**Description**:
The backup restoration testing is performed immediately after backup creation, on the same machine, with the same user. This doesn't test realistic disaster scenarios:

1. Restoration on a different machine (hardware failure)
2. Restoration by a different user (original user unavailable)
3. Restoration from aged backups (bitrot, corruption)
4. Restoration under time pressure (production incident)
5. Partial backup corruption (one backup source compromised)

**Impact**:
- False confidence in backup reliability
- Discover backup issues during actual disaster
- Extended downtime during recovery
- Potential permanent credential loss

**Likelihood**: MEDIUM (backup failures are common, often discovered too late)

**Mitigation**:
1. **Test restoration on clean machine**:
   ```bash
   # Spin up fresh VM or use team member's machine
   # Do NOT have original keystore present

   # Simulate disaster: Original machine inaccessible
   # Test 1: Restore from iCloud
   # Test 2: Restore from external drive (physically shipped)
   # Test 3: Restore from password manager (download attachment)

   # Verify: Can sign APK successfully
   ```

2. **Test restoration by secondary user**:
   ```bash
   # Secondary user (not primary) performs restoration
   # Verifies:
   # - Access to backup locations
   # - Decryption key access
   # - Password manager access
   # - Understanding of procedure

   # Document any difficulties encountered
   ```

3. **Test aged backup restoration**:
   ```bash
   # Wait 30 days after initial backup
   # Test restoration from each backup location
   # Verify:
   # - File corruption (SHA256 hash matches)
   # - Cloud storage retention policy hasn't deleted backup
   # - External drive still readable
   # - Password manager still accessible
   ```

4. **Time the restoration process**:
   ```bash
   # Document in EMERGENCY_RECOVERY.md:
   # - iCloud restore: X minutes
   # - External drive restore: Y minutes
   # - Password manager restore: Z minutes

   # Set RTO (Recovery Time Objective)
   # Target: Keystore restored within 30 minutes
   ```

5. **Test partial backup corruption**:
   ```bash
   # Scenario: iCloud backup is corrupted
   # Can we recover from external drive only?

   # Simulate corruption
   mv ~/iCloud/SmilePile-Credentials/backup.keystore.enc backup.keystore.enc.corrupted

   # Attempt restoration
   # Expected: External drive restore successful
   # Document: Single backup loss doesn't prevent recovery
   ```

**Verification**:
```bash
# Verify restoration time is documented
grep "Restoration time" deploy/docs/EMERGENCY_RECOVERY.md
# Expected: Time estimates for each backup source

# Verify secondary can perform restoration
# [Secondary user performs restoration test]
# Expected: Successful restoration within target time

# Verify aged backups are tested
ls -lh deploy/docs/BACKUP_TEST_LOG.md
# Expected: Log entries for monthly backup tests
```

---

### HIGH-03: No Rate Limiting or Abuse Prevention for API Keys

**Severity**: HIGH
**Category**: Access Control
**Location**: Missing from Implementation Plan

**Description**:
API keys for App Store Connect and Play Console have no rate limiting or abuse prevention controls. If keys are compromised, attackers can:
1. Upload unlimited builds to exhaust TestFlight quota
2. Spam Play Console with malicious builds
3. Exfiltrate user analytics at unlimited rate
4. Trigger expensive operations repeatedly

**Impact**:
- Denial of service (TestFlight quota exhausted)
- Financial costs (Play Console API quotas)
- Account suspension (abuse detection)
- Difficult to detect low-and-slow exfiltration

**Likelihood**: MEDIUM (requires key compromise, but impact is severe)

**Mitigation**:
1. **Implement client-side rate limiting**:
   ```bash
   # deploy/lib/rate_limiter.sh
   #!/bin/bash

   RATE_LIMIT_FILE="/tmp/smilepile_api_rate_limit"
   MAX_CALLS_PER_HOUR=10

   check_rate_limit() {
     local api_name="$1"
     local current_time=$(date +%s)
     local window_start=$((current_time - 3600))

     # Read recent API calls
     if [ -f "$RATE_LIMIT_FILE" ]; then
       local call_count=$(awk -v start="$window_start" \
         '$1 >= start && $2 == "'"$api_name"'"' \
         "$RATE_LIMIT_FILE" | wc -l)

       if [ "$call_count" -ge "$MAX_CALLS_PER_HOUR" ]; then
         echo "ERROR: Rate limit exceeded for $api_name"
         echo "Maximum $MAX_CALLS_PER_HOUR calls per hour"
         exit 1
       fi
     fi

     # Log this API call
     echo "$current_time $api_name" >> "$RATE_LIMIT_FILE"

     # Clean up old entries
     awk -v start="$window_start" '$1 >= start' \
       "$RATE_LIMIT_FILE" > "$RATE_LIMIT_FILE.tmp"
     mv "$RATE_LIMIT_FILE.tmp" "$RATE_LIMIT_FILE"
   }

   # Usage in deployment script
   check_rate_limit "testflight_upload"
   fastlane pilot upload ...
   ```

2. **Monitor API usage patterns**:
   ```bash
   # Deploy alert for unusual API activity
   # Normal pattern: 1-2 deployments per day
   # Alert threshold: >5 deployments per hour

   detect_unusual_activity() {
     local call_count=$(tail -100 "$RATE_LIMIT_FILE" | wc -l)
     if [ "$call_count" -gt 5 ]; then
       echo "ALERT: Unusual API activity detected" | \
         mail -s "SmilePile Security Alert" security@example.com
     fi
   }
   ```

3. **Implement circuit breaker pattern**:
   ```bash
   # If multiple API calls fail, stop making calls
   FAILURE_THRESHOLD=3
   FAILURE_COUNT_FILE="/tmp/smilepile_api_failures"

   api_call_with_circuit_breaker() {
     local failure_count=0
     if [ -f "$FAILURE_COUNT_FILE" ]; then
       failure_count=$(cat "$FAILURE_COUNT_FILE")
     fi

     if [ "$failure_count" -ge "$FAILURE_THRESHOLD" ]; then
       echo "ERROR: Circuit breaker open (too many failures)"
       echo "Manual intervention required"
       exit 1
     fi

     # Make API call
     if ! fastlane pilot upload ...; then
       echo $((failure_count + 1)) > "$FAILURE_COUNT_FILE"
       exit 1
     fi

     # Success: Reset failure count
     echo 0 > "$FAILURE_COUNT_FILE"
   }
   ```

4. **Set up quotas in cloud provider** (if applicable):
   ```bash
   # Google Cloud: Set quota alerts
   gcloud monitoring policies create \
     --notification-channels=CHANNEL_ID \
     --display-name="Play Console API Quota Alert" \
     --condition-threshold-value=80 \
     --condition-threshold-comparison=COMPARISON_GT \
     --condition-threshold-duration=300s
   ```

**Verification**:
```bash
# Test rate limiting
for i in {1..15}; do
  ./deploy/deploy_qual.sh
done
# Expected: 11th call blocked by rate limiter

# Test circuit breaker
# Simulate failures, verify circuit opens
# Expected: After 3 failures, subsequent calls blocked

# Verify monitoring alerts configured
grep "rate.*limit\|circuit.*breaker" deploy/scripts/*.sh
# Expected: Rate limiting code present
```

---

### HIGH-04: Keystore Alias Name Hardcoded (Prevents Key Rotation)

**Severity**: HIGH
**Category**: Credential Lifecycle
**Location**: Implementation Plan Section 4, Hour 10 (lines 455-517)

**Description**:
The keystore is generated with a hardcoded alias `smilepile-release`:
```bash
keytool -genkeypair -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -alias smilepile-release \
  ...
```

Problems:
1. **Cannot rotate keys** without changing build configuration
2. **Alias name reveals purpose** (information leakage)
3. **Single key in keystore** (no version management)
4. **No rollback capability** if new key is compromised

Android keystores can contain multiple keys with different aliases, enabling key rotation without changing the keystore file.

**Impact**:
- Key compromise requires full keystore regeneration
- Cannot gradually roll out new signing key
- Build configuration breaks if alias changes
- No versioning of signing keys

**Likelihood**: MEDIUM (key rotation is eventual necessity)

**Mitigation**:
1. **Use versioned alias names**:
   ```bash
   # Generate initial keystore with versioned alias
   ALIAS_VERSION="v1"
   KEYSTORE_ALIAS="smilepile-release-$ALIAS_VERSION"

   keytool -genkeypair -v \
     -keystore ~/keystores/smilepile-production.keystore \
     -alias "$KEYSTORE_ALIAS" \
     -keyalg RSA \
     -keysize 4096 \
     -validity 10000

   # Store alias version in metadata
   echo "CURRENT_KEYSTORE_ALIAS=$KEYSTORE_ALIAS" >> \
     deploy/secrets/keystore-metadata.env
   ```

2. **Support key rotation in build configuration**:
   ```kotlin
   // android/app/build.gradle.kts
   android {
       signingConfigs {
           create("release") {
               storeFile = file(System.getenv("KEYSTORE_PATH")
                   ?: rootProject.file("../keystores/smilepile-production.keystore"))
               storePassword = System.getenv("KEYSTORE_PASSWORD")
               // DYNAMIC alias from environment
               keyAlias = System.getenv("KEYSTORE_ALIAS") ?: "smilepile-release-v1"
               keyPassword = System.getenv("KEY_PASSWORD")
           }
       }
   }
   ```

3. **Document key rotation procedure**:
   ```bash
   # deploy/docs/KEYSTORE_KEY_ROTATION.md

   ## Rotating Signing Key (Not Keystore Password)

   ### When to Rotate
   - Key compromise suspected
   - Cryptographic weakness discovered
   - Compliance requirement

   ### Rotation Procedure

   1. Generate new key in SAME keystore:
      keytool -genkeypair -v \
        -keystore ~/keystores/smilepile-production.keystore \
        -alias smilepile-release-v2 \
        -keyalg RSA \
        -keysize 4096 \
        -validity 10000 \
        -storepass "$STORE_PASS"

   2. Update build configuration:
      export KEYSTORE_ALIAS="smilepile-release-v2"
      # Update deploy/secrets/production.env

   3. Build and test with new key:
      ./gradlew assembleRelease
      # Verify APK signature

   4. Gradual rollout:
      - Week 1: Deploy to QUAL with new key
      - Week 2: Deploy to STAGE with new key
      - Week 3: Deploy to BETA with new key
      - Week 4: Deploy to PROD with new key

   5. Keep old key for 90 days:
      # Emergency rollback capability
      # After 90 days, delete old key:
      keytool -delete \
        -keystore ~/keystores/smilepile-production.keystore \
        -alias smilepile-release-v1 \
        -storepass "$STORE_PASS"

   Note: Play App Signing complicates this.
   If enrolled, Google manages the production key.
   This procedure applies to upload key rotation only.
   ```

4. **List all keys in keystore for auditing**:
   ```bash
   # Audit current keystore contents
   keytool -list -v \
     -keystore ~/keystores/smilepile-production.keystore \
     -storepass "$STORE_PASS" | \
     grep "Alias name:"

   # Expected output:
   # Alias name: smilepile-release-v1
   # Alias name: smilepile-release-v2 (after rotation)
   ```

**Verification**:
```bash
# Verify dynamic alias support in build
grep "KEYSTORE_ALIAS" android/app/build.gradle.kts
# Expected: Environment variable used for alias

# Verify multiple keys can coexist
keytool -list -keystore ~/keystores/smilepile-production.keystore
# Expected: Multiple aliases listed

# Test build with different alias
export KEYSTORE_ALIAS="smilepile-release-v2"
./gradlew assembleRelease
# Expected: Successful build
```

---

### HIGH-05: No Verification of Play App Signing Enrollment Status

**Severity**: HIGH
**Category**: Keystore Security
**Location**: Implementation Plan Section 3, Hour 8 (lines 335-362)

**Description**:
The plan mentions Play App Signing enrollment as "CRITICAL" but doesn't verify enrollment status after setup, and doesn't document what to do if enrollment fails. Play App Signing status determines disaster recovery procedures:

- **If enrolled**: Google has production key, can reset upload key
- **If NOT enrolled**: Upload key loss = permanent app update lockout

**Impact**:
- Incorrect disaster recovery procedures if enrollment status unknown
- Team believes recovery possible when it's not
- Critical time wasted during actual disaster
- Potential permanent inability to update app

**Likelihood**: MEDIUM (enrollment is manual and can be skipped accidentally)

**Mitigation**:
1. **Verify Play App Signing enrollment after setup**:
   ```bash
   # deploy/scripts/verify_play_app_signing.sh
   #!/bin/bash

   PACKAGE_NAME="com.smilepile"

   echo "=== Verifying Play App Signing Status ==="

   # Check via Play Console UI (manual verification)
   echo "1. Navigate to: https://play.google.com/console/developers/*/app/$PACKAGE_NAME/app-signing"
   echo "2. Verify status: App signing is enabled"
   echo ""
   read -p "Is Play App Signing enabled? (yes/no): " response

   if [ "$response" != "yes" ]; then
     echo "ERROR: Play App Signing NOT enabled"
     echo "CRITICAL: Keystore loss will be catastrophic"
     echo ""
     echo "ACTION REQUIRED:"
     echo "1. Enroll in Play App Signing IMMEDIATELY"
     echo "2. Upload production keystore to Google"
     echo "3. Verify enrollment successful"
     exit 1
   fi

   # Document status
   cat >> deploy/docs/PLAY_APP_SIGNING_STATUS.md <<EOF
   # Play App Signing Status

   **Date Verified**: $(date +%Y-%m-%d)
   **Status**: Enrolled
   **Package**: $PACKAGE_NAME
   **Upload Certificate SHA-1**: [Copy from Play Console]
   **Upload Certificate SHA-256**: [Copy from Play Console]
   **App Signing Certificate SHA-1**: [Copy from Play Console]
   **App Signing Certificate SHA-256**: [Copy from Play Console]

   ## Recovery Procedures

   ### If Upload Key Lost
   1. Contact Play Console Support
   2. Request upload key reset
   3. Generate new upload key
   4. Submit public certificate to Google
   5. Wait for approval (1-3 days)

   ### If App Signing Key Lost
   - NOT POSSIBLE: Google manages this key
   - No action required by developer
   EOF

   echo ""
   echo "SUCCESS: Play App Signing verified and documented"
   ```

2. **Compare certificate fingerprints**:
   ```bash
   # Local keystore fingerprint
   LOCAL_SHA256=$(keytool -list -v \
     -keystore ~/keystores/smilepile-production.keystore \
     -storepass "$STORE_PASS" | \
     grep "SHA256:" | head -1 | awk '{print $2}')

   echo "Local keystore SHA256: $LOCAL_SHA256"
   echo ""
   echo "Compare with Play Console → App Signing → Upload certificate"
   echo "SHA-256 values MUST match"
   read -p "Do SHA-256 values match? (yes/no): " match

   if [ "$match" != "yes" ]; then
     echo "ERROR: Certificate mismatch"
     echo "Local keystore does NOT match Play Console upload certificate"
     exit 1
   fi
   ```

3. **Document different scenarios**:
   ```markdown
   # deploy/docs/KEYSTORE_DISASTER_RECOVERY.md

   ## Scenario 1: Play App Signing Enrolled (CURRENT)

   ### Upload Key Lost
   - **Impact**: MEDIUM - Cannot deploy until new key approved
   - **Recovery**: Request upload key reset from Google (1-3 days)
   - **Procedure**: See PLAY_APP_SIGNING_STATUS.md

   ### App Signing Key Lost
   - **Impact**: NONE - Google manages this key
   - **Recovery**: No action needed

   ## Scenario 2: Play App Signing NOT Enrolled (CATASTROPHIC)

   ### Production Key Lost
   - **Impact**: CATASTROPHIC - Cannot update app EVER
   - **Recovery**: NONE - Must publish new app with different package name
   - **Prevention**: ENROLL IN PLAY APP SIGNING IMMEDIATELY

   ## Current Status
   - **Enrolled**: YES (verified $(date +%Y-%m-%d))
   - **Recovery Option**: Upload key reset available
   ```

4. **Set reminder to verify enrollment quarterly**:
   ```bash
   # Add to calendar
   calendar add "Verify Play App Signing enrollment status" \
     $(date -v+90d "+%Y-%m-%d")
   ```

**Verification**:
```bash
# Verify enrollment documented
ls -lh deploy/docs/PLAY_APP_SIGNING_STATUS.md
# Expected: File exists with enrollment details

# Verify certificate fingerprints match
grep "SHA-256" deploy/docs/PLAY_APP_SIGNING_STATUS.md
# Expected: SHA-256 listed for upload and app signing certificates

# Test verification script
./deploy/scripts/verify_play_app_signing.sh
# Expected: Script confirms enrollment
```

---

### HIGH-06: No Code Signing Certificate Expiration Monitoring

**Severity**: HIGH
**Category**: Certificate Management
**Location**: Implementation Plan Section 2, Hour 5 (lines 195-221)

**Description**:
The plan documents certificate expiration dates but has no automated monitoring or alerts. Apple distribution certificates expire after 1 year. If certificate expires:
1. Cannot deploy to TestFlight
2. Cannot submit to App Store
3. Must generate new certificate
4. Must update provisioning profiles
5. Potential days of deployment downtime

**Impact**:
- Production deployment blocked if certificate expires
- Emergency hotfixes cannot be deployed
- Revenue loss during downtime
- Team scrambles to renew certificate under pressure

**Likelihood**: HIGH (certificate expiration is guaranteed, monitoring is essential)

**Mitigation**:
1. **Extract and monitor certificate expiration**:
   ```bash
   # deploy/scripts/check_certificate_expiration.sh
   #!/bin/bash

   echo "=== Checking Certificate Expiration Dates ==="

   # iOS Distribution Certificate
   DIST_CERT_EXPIRY=$(security find-certificate \
     -c "Apple Distribution: Adam Stack" -p | \
     openssl x509 -noout -enddate | cut -d= -f2)

   echo "iOS Distribution Certificate expires: $DIST_CERT_EXPIRY"

   # Convert to Unix timestamp
   EXPIRY_TS=$(date -j -f "%b %d %H:%M:%S %Y %Z" \
     "$DIST_CERT_EXPIRY" "+%s" 2>/dev/null)
   NOW_TS=$(date +%s)
   DAYS_REMAINING=$(( (EXPIRY_TS - NOW_TS) / 86400 ))

   echo "Days until expiration: $DAYS_REMAINING"

   # Alert thresholds
   if [ "$DAYS_REMAINING" -lt 7 ]; then
     echo "CRITICAL: Certificate expires in less than 7 days!"
     echo "Renew certificate IMMEDIATELY"
     mail -s "CRITICAL: iOS Certificate Expiring Soon" \
       security@example.com <<< "Certificate expires in $DAYS_REMAINING days"
     exit 1
   elif [ "$DAYS_REMAINING" -lt 30 ]; then
     echo "WARNING: Certificate expires in less than 30 days"
     echo "Schedule certificate renewal"
     mail -s "WARNING: iOS Certificate Expiring" \
       security@example.com <<< "Certificate expires in $DAYS_REMAINING days"
   elif [ "$DAYS_REMAINING" -lt 90 ]; then
     echo "NOTICE: Certificate expires in less than 90 days"
     echo "Add certificate renewal to upcoming sprint"
   else
     echo "OK: Certificate valid for $DAYS_REMAINING days"
   fi

   # Document in deployment metadata
   cat >> deploy/history/certificate_checks.log <<EOF
   $(date +%Y-%m-%d): iOS Distribution Certificate - $DAYS_REMAINING days remaining
   EOF
   ```

2. **Add certificate check to deployment scripts**:
   ```bash
   # In deploy/lib/ios_deploy.sh

   check_certificate_validity() {
     ./deploy/scripts/check_certificate_expiration.sh

     # Exit if certificate expires soon
     if [ $? -eq 1 ]; then
       echo "ERROR: Cannot deploy with expiring certificate"
       exit 1
     fi
   }

   # Call before every iOS deployment
   check_certificate_validity
   ```

3. **Set up calendar reminders**:
   ```bash
   # After certificate generation/renewal

   # Get expiry date
   EXPIRY_DATE=$(security find-certificate \
     -c "Apple Distribution: Adam Stack" -p | \
     openssl x509 -noout -enddate | cut -d= -f2)

   # Calendar reminders
   calendar add "CRITICAL: Renew iOS Distribution Certificate" \
     $(date -j -f "%b %d %H:%M:%S %Y %Z" "$EXPIRY_DATE" -v-7d "+%Y-%m-%d")

   calendar add "WARNING: iOS certificate expires in 30 days" \
     $(date -j -f "%b %d %H:%M:%S %Y %Z" "$EXPIRY_DATE" -v-30d "+%Y-%m-%d")

   calendar add "NOTICE: iOS certificate expires in 90 days" \
     $(date -j -f "%b %d %H:%M:%S %Y %Z" "$EXPIRY_DATE" -v-90d "+%Y-%m-%d")
   ```

4. **Document renewal procedure**:
   ```markdown
   # deploy/docs/CERTIFICATE_RENEWAL.md

   ## iOS Distribution Certificate Renewal

   ### When to Renew
   - 30 days before expiration (recommended)
   - 7 days before expiration (critical)
   - NEVER let certificate expire

   ### Renewal Procedure

   1. Generate new certificate request:
      - Keychain Access → Certificate Assistant → Request Certificate
      - Save to disk: CertificateSigningRequest.certSigningRequest

   2. Create new certificate in Apple Developer:
      - developer.apple.com → Certificates
      - Create new Distribution certificate
      - Upload CertificateSigningRequest.certSigningRequest
      - Download: distribution.cer

   3. Install certificate:
      - Double-click distribution.cer
      - Verify appears in Keychain Access

   4. Update provisioning profiles:
      - Xcode → Preferences → Accounts → Download Manual Profiles
      - Or: developer.apple.com → Profiles → Regenerate profiles

   5. Verify new certificate works:
      - Xcode → Product → Archive
      - Upload to TestFlight

   6. Revoke old certificate (after verification):
      - developer.apple.com → Certificates → Revoke old certificate

   7. Update documentation:
      - Document new expiration date
      - Set new calendar reminders
   ```

5. **Automated daily expiration check** (cron job or GitHub Actions):
   ```yaml
   # .github/workflows/certificate-check.yml
   name: Certificate Expiration Check

   on:
     schedule:
       - cron: '0 10 * * *'  # Daily at 10am

   jobs:
     check-certificates:
       runs-on: macos-latest
       steps:
         - uses: actions/checkout@v3

         - name: Check Certificate Expiration
           run: ./deploy/scripts/check_certificate_expiration.sh

         - name: Notify on Expiration
           if: failure()
           uses: 8398a7/action-slack@v3
           with:
             status: ${{ job.status }}
             text: 'iOS Distribution Certificate expiring soon!'
             webhook_url: ${{ secrets.SLACK_WEBHOOK }}
   ```

**Verification**:
```bash
# Verify expiration check script works
./deploy/scripts/check_certificate_expiration.sh
# Expected: Output showing days remaining

# Verify deployment scripts call check
grep "check_certificate" deploy/lib/ios_deploy.sh
# Expected: Function call present

# Verify calendar reminders set
grep "certificate" ~/.calendar | grep "Renew"
# Expected: Reminder entries

# Verify GitHub Actions workflow present
ls -lh .github/workflows/certificate-check.yml
# Expected: File exists
```

---

### HIGH-07: Service Account JSON Stored Without Access Control

**Severity**: HIGH
**Category**: Access Control
**Location**: Implementation Plan Section 3, Hour 9 (lines 398-444)

**Description**:
The service account JSON is stored at `~/play-console-credentials/play-console-service-account.json` with 600 permissions (owner read/write). While this prevents other users on the same machine from reading it, it doesn't:

1. **Restrict which processes** can read it (any process as the user can)
2. **Audit access** (no logging of who/what accessed the file)
3. **Prevent exfiltration** (malware running as user can copy it)
4. **Control sharing** (user can accidentally email/share it)
5. **Expire access** (no automatic revocation after time period)

**Impact**:
- Malware can exfiltrate service account JSON
- Accidental sharing in support tickets
- No forensic trail of access
- Cannot revoke access without deleting file

**Likelihood**: MEDIUM (600 permissions are common practice but insufficient)

**Mitigation**:
1. **Use macOS extended attributes for ACLs**:
   ```bash
   # More restrictive than 600 permissions
   chmod 600 ~/play-console-credentials/play-console-service-account.json

   # Add ACL to restrict access to specific processes
   # Only gradle and deploy scripts can read
   xattr -w com.apple.security.app-sandbox true \
     ~/play-console-credentials/play-console-service-account.json

   # Prevent copying/moving
   chflags uchg ~/play-console-credentials/play-console-service-account.json
   # uchg = immutable, cannot be modified or deleted
   ```

2. **Use encrypted volume for credentials**:
   ```bash
   # Create encrypted sparse bundle
   hdiutil create -size 10m -encryption AES-256 \
     -volname "SmilePile Credentials" \
     -fs HFS+ ~/SmilePile-Credentials.sparsebundle

   # Mount with password
   hdiutil attach ~/SmilePile-Credentials.sparsebundle

   # Store credentials on encrypted volume
   cp play-console-service-account.json \
     /Volumes/SmilePile\ Credentials/

   # Unmount after use
   hdiutil detach /Volumes/SmilePile\ Credentials

   # Update deployment scripts to mount/unmount
   # deploy/lib/android_deploy.sh
   mount_credentials() {
     if ! mount | grep -q "SmilePile Credentials"; then
       hdiutil attach ~/SmilePile-Credentials.sparsebundle -quiet
     fi
   }

   unmount_credentials() {
     hdiutil detach /Volumes/SmilePile\ Credentials -quiet
   }

   # Usage
   mount_credentials
   fastlane supply --json_key "/Volumes/SmilePile Credentials/service-account.json"
   unmount_credentials
   ```

3. **Implement file access auditing**:
   ```bash
   # Enable audit logging for credential file
   sudo audit -e  # Enable audit subsystem

   # Add audit rule for credential file
   sudo praudit -l | grep play-console-service-account.json

   # Monitor access in real-time
   sudo tail -f /var/audit/current | \
     grep play-console-service-account.json
   ```

4. **Use short-lived credentials via OAuth2**:
   ```bash
   # Instead of long-lived service account JSON,
   # use OAuth2 with refresh tokens

   # Initial authentication (manual, one-time)
   gcloud auth application-default login

   # This creates short-lived access tokens
   # Automatically refreshed by Google client libraries
   # No long-lived JSON file needed

   # Update fastlane to use ADC (Application Default Credentials)
   # Fastfile
   lane :deploy_play_store do
     supply(
       # Don't specify json_key, uses ADC
       track: 'internal'
     )
   end
   ```

5. **Rotate service account keys quarterly**:
   ```bash
   # Add to calendar reminder
   calendar add "Rotate Play Console service account key" \
     $(date -v+90d "+%Y-%m-%d")

   # Rotation procedure
   # 1. Create new service account key
   # 2. Test with staging deployment
   # 3. Update production secrets
   # 4. Delete old key from Google Cloud Console
   # 5. Update backups
   ```

**Verification**:
```bash
# Verify file permissions
ls -l ~/play-console-credentials/play-console-service-account.json
# Expected: -rw------- (600)

# Verify immutable flag set
ls -lO ~/play-console-credentials/play-console-service-account.json
# Expected: uchg flag present

# Verify encrypted volume in use
mount | grep "SmilePile Credentials"
# Expected: Encrypted volume mounted during deployment

# Verify access auditing enabled
sudo praudit -l | grep play-console-service-account.json
# Expected: Audit entries showing access
```

---

### HIGH-08: No Protection Against Man-in-the-Middle During API Communication

**Severity**: HIGH
**Category**: Network Security
**Location**: Missing from Implementation Plan

**Description**:
The plan uses fastlane to communicate with App Store Connect and Play Console APIs, but doesn't verify:
1. TLS certificate pinning for API endpoints
2. TLS version requirements (TLS 1.2 minimum)
3. Certificate validation during API calls
4. Protection against proxy-based MITM attacks

If developer machine is compromised with MITM proxy (e.g., Charles Proxy, Burp Suite), API credentials can be intercepted during transmission.

**Impact**:
- API keys intercepted during authentication
- Build artifacts intercepted during upload
- Attacker can inject malicious builds
- Credentials exposed on compromised networks

**Likelihood**: MEDIUM (MITM attacks common on public WiFi, conferences)

**Mitigation**:
1. **Verify TLS certificate pinning in fastlane**:
   ```ruby
   # Fastfile
   require 'openssl'

   before_all do |lane|
     # Verify TLS settings
     OpenSSL::SSL::SSLContext::DEFAULT_PARAMS[:verify_mode] = OpenSSL::SSL::VERIFY_PEER
     OpenSSL::SSL::SSLContext::DEFAULT_PARAMS[:min_version] = :TLS1_2

     # Certificate pinning for App Store Connect
     # (fastlane handles this internally, verify enabled)
     ENV['FASTLANE_VERIFY_SSL'] = 'true'
   end
   ```

2. **Detect and block proxy configurations**:
   ```bash
   # deploy/scripts/check_network_security.sh
   #!/bin/bash

   echo "=== Checking Network Security ==="

   # Check for HTTP/HTTPS proxy environment variables
   if [ -n "$HTTP_PROXY" ] || [ -n "$HTTPS_PROXY" ] || \
      [ -n "$http_proxy" ] || [ -n "$https_proxy" ]; then
     echo "WARNING: Proxy configuration detected"
     echo "HTTP_PROXY: ${HTTP_PROXY:-not set}"
     echo "HTTPS_PROXY: ${HTTPS_PROXY:-not set}"
     echo ""
     echo "Proxy intercepts API communication"
     read -p "Proceed with deployment? (yes/no): " response
     if [ "$response" != "yes" ]; then
       echo "Deployment cancelled due to proxy configuration"
       exit 1
     fi
   fi

   # Check for system-wide proxy (macOS)
   if networksetup -getwebproxy "Wi-Fi" | grep -q "Enabled: Yes"; then
     echo "WARNING: System web proxy is enabled"
     echo "Disable proxy for secure API communication"
     exit 1
   fi

   # Verify DNS resolution for API endpoints
   echo "Verifying DNS resolution..."

   # App Store Connect
   ASC_IP=$(dig +short api.appstoreconnect.apple.com | tail -1)
   if [ -z "$ASC_IP" ]; then
     echo "ERROR: Cannot resolve api.appstoreconnect.apple.com"
     exit 1
   fi
   echo "App Store Connect API: $ASC_IP"

   # Play Console
   PLAY_IP=$(dig +short androidpublisher.googleapis.com | tail -1)
   if [ -z "$PLAY_IP" ]; then
     echo "ERROR: Cannot resolve androidpublisher.googleapis.com"
     exit 1
   fi
   echo "Play Console API: $PLAY_IP"

   echo ""
   echo "Network security checks passed"
   ```

3. **Require secure network for production deployments**:
   ```bash
   # deploy/deploy_prod.sh

   # Verify network security before production deployment
   if [ "$TIER" = "prod" ]; then
     ./deploy/scripts/check_network_security.sh

     # Additionally, require wired connection for production
     if ! ifconfig en0 | grep -q "status: active"; then
       echo "ERROR: Production deployments require wired network connection"
       echo "WiFi networks are vulnerable to MITM attacks"
       exit 1
     fi
   fi
   ```

4. **Monitor for TLS errors in fastlane output**:
   ```bash
   # Wrap fastlane calls with TLS error detection
   run_fastlane_securely() {
     local output=$(mktemp)

     # Run fastlane, capture output
     fastlane "$@" 2>&1 | tee "$output"
     local exit_code=${PIPESTATUS[0]}

     # Check for TLS-related errors
     if grep -qi "certificate.*verify.*failed\|ssl.*error\|tls.*error" "$output"; then
       echo "ERROR: TLS/SSL error detected during API communication"
       echo "Possible man-in-the-middle attack"
       rm "$output"
       exit 1
     fi

     rm "$output"
     return $exit_code
   }

   # Usage
   run_fastlane_securely pilot upload
   ```

5. **Document secure deployment network requirements**:
   ```markdown
   # deploy/docs/NETWORK_SECURITY.md

   ## Network Security Requirements

   ### Allowed Networks for Deployment
   - Corporate wired network (preferred)
   - Trusted home network
   - VPN-protected connections

   ### Prohibited Networks
   - Public WiFi (coffee shops, airports, conferences)
   - Unsecured wireless networks
   - Networks with mandatory proxy configuration
   - Hotel/guest networks

   ### Pre-Deployment Checklist
   - [ ] Connected to trusted network
   - [ ] No HTTP/HTTPS proxy configured
   - [ ] VPN enabled (if on untrusted network)
   - [ ] TLS verification enabled
   - [ ] Certificate warnings = deployment abort
   ```

**Verification**:
```bash
# Verify network security check runs
./deploy/scripts/check_network_security.sh
# Expected: Pass on trusted network

# Verify proxy detection works
HTTP_PROXY="http://localhost:8080" ./deploy/scripts/check_network_security.sh
# Expected: Warning about proxy, prompt to proceed

# Verify TLS verification enabled
grep "FASTLANE_VERIFY_SSL" ios/fastlane/Fastfile android/fastlane/Fastfile
# Expected: Set to 'true'

# Test deployment on untrusted network (should fail)
# [Connect to public WiFi]
./deploy/deploy_prod.sh
# Expected: Error about untrusted network
```

---

## 3. MEDIUM-RISK ISSUES (Fix During or After Implementation)

### MEDIUM-01: Backup Verification Schedule Not Enforced

**Severity**: MEDIUM
**Category**: Backup Security
**Location**: Implementation Plan Section 4 (lines 521-543)

**Description**:
The plan creates backups but relies on manual calendar reminders for quarterly verification. No automated verification that:
1. Backups still exist
2. Backup files are not corrupted
3. Backup locations are still accessible
4. Decryption keys still work

**Impact**:
- Discover backup corruption during disaster
- False confidence in backup reliability
- Extended recovery time

**Likelihood**: MEDIUM-LOW (backup verification is often forgotten)

**Mitigation**:
1. **Create automated backup verification script**:
   ```bash
   # deploy/scripts/verify_backups.sh
   #!/bin/bash

   echo "=== Automated Backup Verification ==="

   BACKUP_LOCATIONS=(
     "~/iCloud/SmilePile-Credentials/smilepile-production.keystore.enc"
     "/Volumes/EncryptedBackup/SmilePile/smilepile-production.keystore.enc"
   )

   ORIGINAL_HASH=$(shasum -a 256 ~/keystores/smilepile-production.keystore | awk '{print $1}')

   for backup in "${BACKUP_LOCATIONS[@]}"; do
     echo "Checking: $backup"

     if [ ! -f "$backup" ]; then
       echo "ERROR: Backup not found: $backup"
       continue
     fi

     # Decrypt and verify hash
     BACKUP_HASH=$(openssl enc -aes-256-cbc -d \
       -in "$backup" \
       -pass pass:"$BACKUP_KEY" 2>/dev/null | \
       shasum -a 256 | awk '{print $1}')

     if [ "$BACKUP_HASH" = "$ORIGINAL_HASH" ]; then
       echo "OK: Backup verified"
     else
       echo "ERROR: Backup corrupted or decryption failed"
     fi
   done
   ```

2. **Schedule automated checks via cron**:
   ```bash
   # Run weekly backup verification
   crontab -e
   # Add line:
   0 9 * * 1 /Users/adamstack/SmilePile/deploy/scripts/verify_backups.sh
   ```

**Verification**:
```bash
# Verify automated check scheduled
crontab -l | grep verify_backups
# Expected: Cron entry present
```

---

### MEDIUM-02: No Documented Incident Response Procedure

**Severity**: MEDIUM
**Category**: Disaster Recovery
**Location**: Missing from Implementation Plan

**Description**:
If credentials are compromised, there's no documented procedure for:
1. Immediate response actions
2. Notification escalation path
3. Forensic investigation steps
4. Communication plan (internal/external)

**Impact**:
- Slow response to security incidents
- Inconsistent incident handling
- Potential compliance violations

**Likelihood**: LOW (incidents are rare, but impact is high when they occur)

**Mitigation**:
Create comprehensive incident response plan:
```markdown
# deploy/docs/INCIDENT_RESPONSE.md

## Security Incident Response Plan

### Incident Types
1. Credential Compromise
2. Unauthorized Access
3. Malicious Build Upload
4. Data Breach

### Response Team
- Primary: Adam Stack
- Secondary: [Team Member]
- External: [Security Consultant]

### Immediate Actions (0-1 hour)
1. Revoke compromised credentials
2. Notify security team
3. Document incident timeline
4. Preserve forensic evidence

### Investigation (1-24 hours)
1. Review access logs
2. Identify breach scope
3. Assess impact
4. Determine root cause

### Remediation (1-7 days)
1. Rotate all potentially compromised credentials
2. Update security procedures
3. Deploy fixes
4. Verify no ongoing compromise

### Communication
- Internal: Slack #security channel
- External: security@smilepile.com
- Legal: [Legal team contact]
- Users: [If user data affected]
```

**Verification**:
```bash
# Verify incident response plan exists
ls -lh deploy/docs/INCIDENT_RESPONSE.md
# Expected: File exists
```

---

### MEDIUM-03: Keystore Passwords May Not Meet Play Store Requirements

**Severity**: MEDIUM
**Category**: Compliance
**Location**: Implementation Plan Section 4, Hour 10 (lines 486-498)

**Description**:
Generated passwords may not meet Google Play Store password requirements:
- Minimum length varies
- Special character requirements may change
- Entropy requirements not documented

**Impact**:
- Keystore rejection during Play App Signing enrollment
- Need to regenerate keystore with stronger password
- Delays in Wave 1 completion

**Likelihood**: LOW (Google's requirements are generally met by strong passwords)

**Mitigation**:
1. **Verify against Google's requirements**:
   ```bash
   # Check minimum password length
   # Google requires 6+ characters (very weak)
   # Use 40+ characters for production keystores

   if [ ${#STORE_PASS} -lt 40 ]; then
     echo "ERROR: Password too short (minimum 40 characters)"
     exit 1
   fi
   ```

2. **Document password requirements**:
   ```markdown
   # deploy/docs/KEYSTORE_PASSWORD_REQUIREMENTS.md

   ## Production Keystore Password Requirements

   - Minimum 40 characters
   - Must include: uppercase, lowercase, digit, special character
   - No dictionary words
   - No personal information
   - Unique per keystore
   - Rotated every 90 days
   ```

**Verification**:
```bash
# Verify password meets requirements
echo "$STORE_PASS" | wc -c  # >= 40
echo "$STORE_PASS" | grep -E '[A-Z]'  # Uppercase
echo "$STORE_PASS" | grep -E '[a-z]'  # Lowercase
echo "$STORE_PASS" | grep -E '[0-9]'  # Digit
echo "$STORE_PASS" | grep -E '[^A-Za-z0-9]'  # Special
```

---

### MEDIUM-04: Team Access Matrix Not Audited Regularly

**Severity**: MEDIUM
**Category**: Access Control
**Location**: Implementation Plan Section 1 (lines 72-77)

**Description**:
The plan creates a team access matrix but has no procedure for:
1. Reviewing access quarterly
2. Removing former team members
3. Auditing who has access to what
4. Enforcing least privilege

**Impact**:
- Former team members retain access
- Privilege creep over time
- Compliance failures (SOC2, ISO 27001)

**Likelihood**: MEDIUM (access reviews are commonly overlooked)

**Mitigation**:
1. **Create access review procedure**:
   ```markdown
   # deploy/docs/ACCESS_REVIEW.md

   ## Quarterly Access Review

   **Schedule**: Every 90 days
   **Owner**: Security Lead

   ### Review Checklist
   - [ ] List all users with App Store Connect access
   - [ ] List all users with Play Console access
   - [ ] List all users with password manager access
   - [ ] List all users with repository access
   - [ ] Verify all users are current team members
   - [ ] Remove access for departed team members
   - [ ] Verify role assignments follow least privilege
   - [ ] Document review in access_reviews/YYYY-QN.md
   ```

2. **Set calendar reminder**:
   ```bash
   calendar add "Quarterly access review" $(date -v+90d "+%Y-%m-%d")
   ```

**Verification**:
```bash
# Verify access review documentation exists
ls -lh deploy/docs/ACCESS_REVIEW.md
# Expected: File exists

# Verify calendar reminder set
grep "access review" ~/.calendar
# Expected: Reminder scheduled
```

---

### MEDIUM-05: No Version Control for Secrets Management

**Severity**: MEDIUM
**Category**: Credential Management
**Location**: Missing from Implementation Plan

**Description**:
When credentials are rotated, old versions are deleted. No version history for:
1. Rollback if new credential fails
2. Audit trail of when credentials changed
3. Emergency recovery from old backups

**Impact**:
- Cannot rollback to previous credentials if issues arise
- Lost audit trail for compliance
- Confusion about which credential version is active

**Likelihood**: LOW (credential rollback is rarely needed)

**Mitigation**:
1. **Implement credential versioning**:
   ```bash
   # When rotating credentials, keep previous version for 30 days

   # Old keystore
   mv ~/keystores/smilepile-production.keystore \
      ~/keystores/smilepile-production.keystore.$(date +%Y%m%d).old

   # Generate new keystore
   keytool -genkeypair ...

   # After 30 days, delete old version
   find ~/keystores/ -name "*.old" -mtime +30 -delete
   ```

2. **Document credential versions**:
   ```markdown
   # deploy/docs/CREDENTIAL_VERSION_HISTORY.md

   ## Keystore Versions

   | Date | Version | Reason | Deprecated | Deleted |
   |------|---------|--------|------------|---------|
   | 2025-10-13 | v1 | Initial generation | - | - |
   | 2026-01-13 | v2 | Scheduled rotation | 2026-01-13 | 2026-02-12 |
   ```

**Verification**:
```bash
# Verify credential version history exists
ls -lh deploy/docs/CREDENTIAL_VERSION_HISTORY.md
# Expected: File exists with version tracking
```

---

### MEDIUM-06-11: Additional Medium-Risk Issues

Due to length constraints, summarizing remaining medium-risk issues:

**MEDIUM-06**: No validation of .gitignore effectiveness before first commit
**MEDIUM-07**: API key revocation procedure not tested
**MEDIUM-08**: No monitoring for iOS provisioning profile expiration
**MEDIUM-09**: External drive backup location not verified encrypted
**MEDIUM-10**: No documented process for onboarding new team members to credentials
**MEDIUM-11**: GitHub Actions secrets not configured (mentioned but not verified)

Each requires documentation, testing, or monitoring procedures similar to those detailed above.

---

## 4. APPROVED SECURITY MEASURES (What's Done Well)

### APPROVED-01: Comprehensive .gitignore Configuration

**Location**: `.gitignore` (lines 48-50, 149-151)
**Assessment**: EXCELLENT

The project has comprehensive .gitignore patterns:
```
*.jks
*.keystore
*.p12
*.p8
*.mobileprovision
secrets/
```

This prevents accidental credential commits. Verification shows no secrets in current git status.

---

### APPROVED-02: Triple Backup Strategy for Keystores

**Location**: Implementation Plan Section 4 (lines 521-543)
**Assessment**: STRONG

Three independent backup locations:
1. Encrypted cloud storage (iCloud)
2. External encrypted drive
3. Password manager vault

This provides redundancy against single point of failure. However, requires encryption improvements (see CRITICAL-02).

---

### APPROVED-03: Existing Security Scanning Infrastructure

**Location**: `deploy/scripts/security.sh`
**Assessment**: GOOD

The project already has comprehensive security scanning:
- Dependency vulnerability scanning
- Hardcoded secret detection
- File permission checks
- SSL/TLS validation
- SAST integration (if semgrep installed)

This is strong foundation for deployment security.

---

### APPROVED-04: Secrets Management Documentation

**Location**: `deploy/docs/SECRETS_MANAGEMENT.md`
**Assessment**: GOOD

Existing documentation covers:
- Secret storage locations
- Rotation policies (90-day schedule)
- Security best practices
- Emergency procedures
- GitHub Actions secrets

This demonstrates security awareness and provides good foundation for Wave 1.

---

### APPROVED-05: Play App Signing Enrollment Recommended

**Location**: Implementation Plan Section 3, Hour 8 (lines 335-362)
**Assessment**: EXCELLENT

The plan strongly recommends Play App Signing enrollment, which:
- Lets Google manage production signing key
- Enables upload key reset if lost
- Prevents catastrophic key loss scenario

This is correct recommendation and critical for Android security.

---

### APPROVED-06: File Permission Requirements

**Location**: Implementation Plan (chmod 600 for secrets, chmod 700 for directories)
**Assessment**: GOOD

The plan correctly specifies:
- 600 permissions for all secret files (owner read/write only)
- 700 permissions for secrets directory (owner access only)
- Verification steps to check permissions

While not sufficient alone (see CRITICAL-01), this is correct baseline security.

---

## 5. SUMMARY AND RECOMMENDATIONS

### Critical Vulnerability Summary

| ID | Severity | Issue | Impact | Remediation Priority |
|----|----------|-------|--------|---------------------|
| CRITICAL-01 | 10/10 | Plaintext keystore passwords | Production key compromise | IMMEDIATE |
| CRITICAL-02 | 9/10 | Unencrypted backups | Backup storage compromise | IMMEDIATE |
| CRITICAL-03 | 8/10 | Weak password generation | Brute force attacks | BEFORE Wave 1 |
| CRITICAL-04 | 9/10 | Excessive service account permissions | Unauthorized production access | BEFORE Wave 1 |
| CRITICAL-05 | 8/10 | Unverified API key permissions | Account-wide compromise | BEFORE Wave 1 |
| CRITICAL-06 | 8/10 | Environment variable credential leaks | Process memory attacks | BEFORE Wave 1 |
| CRITICAL-07 | 7/10 | No password rotation strategy | Long-term credential exposure | BEFORE Wave 1 |
| CRITICAL-08 | 9/10 | Incomplete git history audit | Historical secret exposure | IMMEDIATE |
| CRITICAL-09 | 10/10 | Single point of failure | Business continuity failure | IMMEDIATE |

### Recommended Action Plan

#### Phase 1: Immediate (Before Starting Wave 1)

1. **CRITICAL-09**: Add secondary administrator with full access
2. **CRITICAL-08**: Run comprehensive git history audit with blob search
3. **CRITICAL-01**: Implement encrypted credential storage (keychain or GPG)
4. **CRITICAL-02**: Encrypt all backup files individually

#### Phase 2: During Wave 1 Execution

5. **CRITICAL-04**: Create separate service accounts per environment
6. **CRITICAL-05**: Verify and document API key permissions
7. **CRITICAL-03**: Implement strong password generation with verification
8. **CRITICAL-06**: Use secure credential transmission methods
9. **CRITICAL-07**: Document password rotation procedures

#### Phase 3: Post-Wave 1 (Within 30 Days)

10. Fix all HIGH-risk issues (monitoring, rate limiting, certificate expiration)
11. Implement MEDIUM-risk mitigations
12. Test all disaster recovery procedures
13. Schedule quarterly security reviews

### Go/No-Go Recommendation

**RECOMMENDATION**: NO GO until CRITICAL-01, CRITICAL-02, CRITICAL-08, and CRITICAL-09 are mitigated.

**Minimum Required Changes**:
1. Encrypt keystore.properties or use keychain storage
2. Encrypt all backup files with AES-256
3. Run comprehensive git history audit (including blob search)
4. Add secondary administrator to all accounts
5. Document emergency access procedures

**Timeline Impact**: +3-5 days to Wave 1 timeline for critical fixes.

**Risk Assessment After Mitigations**: Acceptable risk level for Wave 1 execution.

---

## 6. APPENDIX A: Security Testing Procedures

### Validation Tests After Mitigations

```bash
# Test 1: Verify no plaintext passwords
grep -r "password.*=" android/ deploy/secrets/ --include="*.properties" --include="*.env"
# Expected: No actual passwords (only variables or encrypted)

# Test 2: Verify backups are encrypted
file ~/iCloud/SmilePile-Credentials/*.enc
# Expected: "data" (not recognizable format)

# Test 3: Verify git history is clean
./deploy/scripts/audit_git_history.sh
# Expected: No secrets found

# Test 4: Verify secondary can access all systems
# [Secondary performs access checks]
# Expected: Full access verified

# Test 5: Verify credential restoration works
./deploy/scripts/test_emergency_access.sh
# Expected: Restoration successful

# Test 6: Verify deployment works with encrypted credentials
./deploy/deploy_qual.sh --skip-tests
# Expected: Successful build and deployment

# Test 7: Verify API key permissions are minimal
fastlane pilot list  # Should work
fastlane deliver     # Should fail (insufficient permissions)
# Expected: TestFlight works, production blocked

# Test 8: Verify service account permissions are minimal
fastlane supply --track internal  # Should work
fastlane supply --track production  # Should fail
# Expected: Internal works, production blocked
```

### Ongoing Security Monitoring

```bash
# Weekly: Verify backups
./deploy/scripts/verify_backups.sh

# Monthly: Check certificate expiration
./deploy/scripts/check_certificate_expiration.sh

# Quarterly: Review team access
./deploy/scripts/review_team_access.sh

# Quarterly: Rotate credentials
./deploy/scripts/rotate_keystore_password.sh
```

---

**END OF SECURITY AUDIT REPORT**

**Auditor**: Security Agent (Atlas Phase 4)
**Date**: 2025-10-13
**Next Review**: After CRITICAL vulnerabilities mitigated
**Status**: COMPREHENSIVE REVIEW COMPLETE - ACTION REQUIRED
