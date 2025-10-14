# Secrets and Credentials Management

Complete guide to generating, storing, and managing secrets for 4-tier deployments across iOS, Android, and Web platforms.

## Overview

Proper secrets management is critical for secure deployments. This guide covers what credentials you need, how to generate them, where to store them, and how to share them securely with your team.

## Critical Principle: Never Commit Secrets

**NEVER commit these to git:**
- Keystores (`.keystore` files)
- Keystore passwords
- Provisioning profiles (if not using fastlane match)
- Private keys (`.p8`, `.p12` files)
- Service account JSON files
- API keys
- Environment variable files (`.env`)

**Always add to .gitignore:**
```bash
# iOS
*.mobileprovision
*.p12
*.p8
*.cer

# Android
*.keystore
keystore.properties
play-store-credentials.json

# Environment
.env
.env.*

# Fastlane
fastlane/report.xml
fastlane/Preview.html
fastlane/screenshots
fastlane/test_output
```

## Required Secrets by Platform

### iOS Secrets

| Secret | Purpose | Generation | Storage |
|--------|---------|------------|---------|
| Apple ID | App Store Connect access | Existing account | Password manager |
| App-Specific Password | CLI authentication | appleid.apple.com | Keychain / password manager |
| App Store Connect API Key | Automated uploads | App Store Connect | Secure file storage |
| Distribution Certificate | Code signing | Xcode / fastlane match | Keychain / match repo |
| Provisioning Profiles | App distribution | Xcode / fastlane match | Match repo |
| fastlane match passphrase | Encrypts certificates | When initializing match | Password manager |

### Android Secrets

| Secret | Purpose | Generation | Storage |
|--------|---------|------------|---------|
| Production Keystore | App signing | keytool | Secure file storage + backup |
| Keystore Password | Keystore access | During keytool generation | Password manager |
| Key Alias | Key identifier | During keytool generation | Password manager |
| Key Password | Key access | During keytool generation | Password manager |
| Upload Keystore | Play Console uploads | keytool | Secure file storage + backup |
| Service Account JSON | Play Console API | Play Console | Secure file storage |

### Web Secrets

| Secret | Purpose | Generation | Storage |
|--------|---------|------------|---------|
| Deployment SSH Keys | Server access | ssh-keygen | ~/.ssh/ (local), server authorized_keys |
| API Keys | Third-party services | Service provider | Environment variables |
| Environment Variables | Runtime config | Manual creation | .env files (not committed) |

## iOS Secrets Setup

### 1. Apple ID and App-Specific Password

**Generate app-specific password:**
1. Visit https://appleid.apple.com
2. Sign in with your Apple ID
3. Security → App-Specific Passwords
4. Click "Generate"
5. Label: "[YOUR_APP] Fastlane"
6. Copy password (format: `xxxx-xxxx-xxxx-xxxx`)

**Store in keychain:**
```bash
# Fastlane will prompt and store automatically on first use
fastlane deliver

# Or manually store in keychain
security add-generic-password -a "your@email.com" -s "fastlane" -w "xxxx-xxxx-xxxx-xxxx"
```

### 2. App Store Connect API Key

**Generate API key:**
1. App Store Connect → Users and Access → Keys
2. Click "+" to create new key
3. Name: "[YOUR_APP] Deployment"
4. Access: Admin or App Manager
5. Click "Generate"
6. Download `.p8` file (ONLY AVAILABLE ONCE!)
7. Record Key ID (e.g., `ABCD1234EF`)
8. Record Issuer ID (e.g., `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)

**Store securely:**
```bash
# Create secure directory
mkdir -p ~/app-store-connect-api-keys
chmod 700 ~/app-store-connect-api-keys

# Move downloaded key
mv ~/Downloads/AuthKey_ABCD1234EF.p8 ~/app-store-connect-api-keys/
chmod 600 ~/app-store-connect-api-keys/AuthKey_ABCD1234EF.p8

# Record Key ID and Issuer ID in password manager
```

**Use in Fastfile:**
```ruby
app_store_connect_api_key(
  key_id: "ABCD1234EF",
  issuer_id: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  key_filepath: "~/app-store-connect-api-keys/AuthKey_ABCD1234EF.p8"
)
```

### 3. Distribution Certificate and Provisioning Profiles

**Option 1: fastlane match (Recommended for teams)**

Initialize match:
```bash
cd ios
fastlane match init

# Choose storage: git (recommended)
# Git URL: https://github.com/[YOUR_ORG]/[YOUR_APP]-certificates.git (private repo!)
# Passphrase: [STRONG_PASSPHRASE] (record in password manager)
```

Generate certificates:
```bash
# Development (for QUAL)
fastlane match development

# App Store (for STAGE/BETA/PROD)
fastlane match appstore

# Enter passphrase when prompted
# Certificates and profiles stored in git repo (encrypted)
```

**Store passphrase securely:**
- Password manager entry: "fastlane match passphrase for [YOUR_APP]"
- Share with team via secure channel (1Password, LastPass, etc.)

**Option 2: Manual Xcode management**

Let Xcode handle automatically:
1. Xcode → Settings → Accounts → Add Apple ID
2. Project → Signing & Capabilities → Automatically manage signing
3. Select team
4. Xcode generates certificates and profiles automatically

Certificates stored in Keychain Access:
```bash
# Export certificate for backup
# Keychain Access → My Certificates → Right-click → Export
# Save as .p12 with strong password
# Store .p12 file and password securely
```

### 4. iOS Secrets Checklist

- [ ] Apple ID stored in password manager
- [ ] App-specific password generated and stored
- [ ] App Store Connect API Key downloaded and stored securely
- [ ] Key ID and Issuer ID recorded
- [ ] Distribution certificate generated (match or manual)
- [ ] Provisioning profiles generated (match or manual)
- [ ] fastlane match passphrase stored (if using match)
- [ ] Certificate backup created (.p12 export)

## Android Secrets Setup

### 1. Production Keystore

**Generate keystore:**
```bash
keytool -genkey -v -keystore [YOUR_APP]-production.keystore \
  -alias [YOUR_APP]-production-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Prompts:
# - Keystore password: [STRONG_PASSWORD] (record in password manager!)
# - Re-enter password
# - What is your first and last name? [Your Name]
# - What is the name of your organizational unit? [Your Team/Company]
# - What is the name of your organization? [Your Company]
# - What is the name of your City or Locality? [City]
# - What is the name of your State or Province? [State]
# - What is the two-letter country code for this unit? [US]
# - Is CN=... correct? [yes]
# - Key password: [SAME_AS_KEYSTORE_PASSWORD or different] (record separately!)
```

**Store keystore securely:**
```bash
# Create secure directory
mkdir -p ~/keystores
chmod 700 ~/keystores

# Move keystore
mv [YOUR_APP]-production.keystore ~/keystores/
chmod 600 ~/keystores/[YOUR_APP]-production.keystore

# Backup to multiple locations:
# - Encrypted cloud storage (1Password, Google Drive with encryption)
# - External hard drive (encrypted)
# - Team secure storage (if applicable)
```

**Record keystore details in password manager:**
```
Entry: [YOUR_APP] Android Production Keystore
File location: ~/keystores/[YOUR_APP]-production.keystore
Keystore password: [PASSWORD]
Key alias: [YOUR_APP]-production-key
Key password: [PASSWORD]
```

### 2. Upload Keystore (Recommended with Play App Signing)

If using Google Play App Signing, generate separate upload keystore:

```bash
keytool -genkey -v -keystore [YOUR_APP]-upload.keystore \
  -alias [YOUR_APP]-upload-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Use different passwords from production keystore
# Store in ~/keystores/ and backup
```

**Enroll in Play App Signing:**
1. Play Console → Your app → Release → Setup → App signing
2. "Let Google create and manage your app signing key" (recommended)
3. Upload your production keystore or let Google generate
4. Google manages production key, you use upload key for all uploads

**Benefits:**
- Production key secure with Google
- Rotate upload key if compromised
- Same production signature across releases

### 3. QUAL Keystore (Optional)

**Option 1: Separate QUAL keystore**
```bash
keytool -genkey -v -keystore [YOUR_APP]-qual.keystore \
  -alias [YOUR_APP]-qual-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

**Option 2: Use debug keystore (simpler)**
```bash
# Default location: ~/.android/debug.keystore
# Alias: androiddebugkey
# Store password: android
# Key password: android

# No additional setup needed
```

### 4. keystore.properties

Create `android/keystore.properties` (add to .gitignore!):

```properties
# Production keystore (STAGE/BETA/PROD)
PROD_STORE_FILE=../../../keystores/[YOUR_APP]-production.keystore
PROD_STORE_PASSWORD=[PASSWORD]
PROD_KEY_ALIAS=[YOUR_APP]-production-key
PROD_KEY_PASSWORD=[PASSWORD]

# QUAL keystore (or omit if using debug)
QUAL_STORE_FILE=../../../keystores/[YOUR_APP]-qual.keystore
QUAL_STORE_PASSWORD=[PASSWORD]
QUAL_KEY_ALIAS=[YOUR_APP]-qual-key
QUAL_KEY_PASSWORD=[PASSWORD]
```

**Security alternatives:**
- Environment variables (better for CI/CD)
- Encrypted keystore.properties (decrypt during build)
- Secret management service (AWS Secrets Manager, Vault)

### 5. Play Console Service Account

**Create service account:**
1. Play Console → Setup → API access
2. Link Google Cloud project (if not already linked)
3. "Create new service account" → Follow link to Google Cloud Console
4. Google Cloud Console → IAM & Admin → Service Accounts
5. Create Service Account:
   - Name: "[YOUR_APP] Fastlane"
   - ID: `[YOUR_APP]-fastlane`
   - Role: Service Account User
6. Create key → JSON → Download
7. Return to Play Console → Grant access
   - Select service account
   - Role: Release Manager
   - Add user

**Store JSON securely:**
```bash
# Move to secure location
mv ~/Downloads/[YOUR_APP]-xxxxx.json android/fastlane/play-store-credentials.json
chmod 600 android/fastlane/play-store-credentials.json

# Backup to encrypted storage
# Add to .gitignore
echo "play-store-credentials.json" >> android/fastlane/.gitignore
```

### 6. Android Secrets Checklist

- [ ] Production keystore generated
- [ ] Production keystore password recorded in password manager
- [ ] Production keystore backed up to 3+ locations
- [ ] Upload keystore generated (if using Play App Signing)
- [ ] QUAL keystore generated or debug keystore located
- [ ] keystore.properties created and added to .gitignore
- [ ] Play Console service account created
- [ ] Service account JSON downloaded and stored securely
- [ ] Service account granted "Release Manager" role

## Web Secrets Setup

### 1. Deployment SSH Keys

**Generate SSH key:**
```bash
ssh-keygen -t ed25519 -C "[YOUR_APP]-deployment"

# Save to: ~/.ssh/[YOUR_APP]_deploy_key
# Passphrase: [STRONG_PASSPHRASE] (optional but recommended)
```

**Add public key to server:**
```bash
# Copy public key
cat ~/.ssh/[YOUR_APP]_deploy_key.pub

# SSH to server
ssh user@your-server.com

# Add to authorized_keys
echo "[PUBLIC_KEY]" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

**Use in deployment script:**
```bash
# Deploy with specific key
ssh -i ~/.ssh/[YOUR_APP]_deploy_key user@your-server.com "cd /path/to/app && git pull"
```

### 2. Environment Variables

Create `.env` files for each tier (add to .gitignore!):

**.env.qual**:
```
REACT_APP_BUILD_TYPE=qual
REACT_APP_API_ENDPOINT=https://[YOUR_DOMAIN]/qual/api
REACT_APP_ANALYTICS_KEY=[QUAL_ANALYTICS_KEY]
```

**.env.stage**:
```
REACT_APP_BUILD_TYPE=stage
REACT_APP_API_ENDPOINT=https://[YOUR_DOMAIN]/stage/api
REACT_APP_ANALYTICS_KEY=[STAGE_ANALYTICS_KEY]
```

**.env.beta**:
```
REACT_APP_BUILD_TYPE=beta
REACT_APP_API_ENDPOINT=https://[YOUR_DOMAIN]/beta/api
REACT_APP_ANALYTICS_KEY=[BETA_ANALYTICS_KEY]
```

**.env.prod**:
```
REACT_APP_BUILD_TYPE=prod
REACT_APP_API_ENDPOINT=https://[YOUR_DOMAIN]/api
REACT_APP_ANALYTICS_KEY=[PROD_ANALYTICS_KEY]
```

**Store API keys in password manager:**
- Entry per service (Analytics, Maps, etc.)
- Note which tier uses which key
- Share with team securely

### 3. Web Secrets Checklist

- [ ] Deployment SSH key generated
- [ ] Public key added to server authorized_keys
- [ ] Private key backed up securely
- [ ] .env files created for each tier
- [ ] .env files added to .gitignore
- [ ] API keys stored in password manager
- [ ] SSL certificates configured on server

## Sharing Secrets with Team

### Password Manager (Recommended)

**Use team password manager:**
- 1Password Teams
- LastPass Teams
- Bitwarden Organizations
- Dashlane Business

**Create shared vault:**
1. Vault name: "[YOUR_APP] Deployment Secrets"
2. Add entries for all secrets
3. Grant access to team members who need deployment access

**Entry organization:**
```
[YOUR_APP] Deployment Secrets/
├── iOS/
│   ├── Apple ID
│   ├── App-Specific Password
│   ├── App Store Connect API Key
│   └── fastlane match passphrase
├── Android/
│   ├── Production Keystore Password
│   ├── Production Key Alias & Password
│   ├── Upload Keystore Password
│   └── Play Console Service Account
├── Web/
│   ├── Deployment SSH Key Passphrase
│   ├── QUAL Environment Variables
│   ├── STAGE Environment Variables
│   ├── BETA Environment Variables
│   └── PROD Environment Variables
└── Third-Party Services/
    ├── Analytics API Keys
    ├── Maps API Keys
    └── Other Service Keys
```

### Secure File Sharing

**For keystores and certificates:**
- Encrypted zip files (password shared separately via phone/video call)
- Secure file transfer (Magic Wormhole, Tresorit, SpiderOak)
- Team encrypted storage (Dropbox Business, Google Drive with client-side encryption)

**DO NOT:**
- Email keystores or certificates unencrypted
- Share passwords in Slack/Discord/Teams
- Commit secrets to git
- Store in unencrypted cloud storage

## Environment Variables for CI/CD

For automated deployments, use environment variables instead of files.

### GitHub Actions

**Set secrets in GitHub:**
1. Repo → Settings → Secrets → Actions
2. Add secrets:
   - `IOS_P8_KEY` (base64 encoded .p8 file)
   - `IOS_KEY_ID`
   - `IOS_ISSUER_ID`
   - `ANDROID_KEYSTORE` (base64 encoded keystore)
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
   - `PLAY_SERVICE_ACCOUNT_JSON` (entire JSON content)

**Use in workflow:**
```yaml
- name: Decode Android keystore
  run: |
    echo "${{ secrets.ANDROID_KEYSTORE }}" | base64 --decode > android/app/release.keystore

- name: Deploy Android
  env:
    KEYSTORE_PASSWORD: ${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.ANDROID_KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.ANDROID_KEY_PASSWORD }}
  run: |
    cd android && fastlane stage_android
```

### GitLab CI

**Set variables in GitLab:**
1. Project → Settings → CI/CD → Variables
2. Add variables (mark as Protected and Masked):
   - `IOS_P8_KEY` (file type)
   - `ANDROID_KEYSTORE` (file type)
   - `PLAY_SERVICE_ACCOUNT_JSON` (file type)
   - All passwords as masked variables

### CircleCI

**Set environment variables:**
1. Project Settings → Environment Variables
2. Add variables (similar to GitHub Actions)

## Secret Rotation

Periodically rotate secrets for security:

### iOS Certificate Rotation

**When to rotate:**
- Certificate expires (1 year)
- Team member leaves
- Security breach

**How to rotate:**
```bash
# Revoke old certificate in Apple Developer Portal
# Generate new certificate
fastlane match nuke distribution  # CAUTION: Deletes all certificates
fastlane match appstore           # Generates new ones
```

### Android Keystore Rotation

**IMPORTANT:** Production keystore cannot be rotated (same key must sign all versions).

**If compromised:**
1. If using Play App Signing: Rotate upload key only
   - Play Console → App signing → Request upload key reset
   - Generate new upload keystore
2. If NOT using Play App Signing: Contact Google Play support
   - May require publishing new app with different package name

**Upload keystore rotation (with Play App Signing):**
1. Generate new upload keystore
2. Play Console → Request upload key reset
3. Upload certificate from new keystore

### API Key Rotation

**When to rotate:**
- Annual security audit
- Team member leaves
- Key exposed

**How to rotate:**
1. Generate new key in service provider
2. Update .env files with new key
3. Deploy updated builds
4. Deactivate old key after verification

## Backup Strategy

### 3-2-1 Backup Rule

**3** copies of data
**2** different media types
**1** offsite backup

**For keystores and certificates:**
1. Original: `~/keystores/` or keychain
2. Encrypted cloud backup (1Password, Google Drive encrypted)
3. External hard drive (encrypted, stored offsite)

**Test backups regularly:**
```bash
# Verify keystore backup
keytool -list -v -keystore [BACKUP_KEYSTORE]
# Enter password, verify details match production
```

## Security Best Practices

1. **Principle of Least Privilege**: Only grant deployment access to team members who need it
2. **Audit Access**: Review who has access to secrets quarterly
3. **Rotate on Departure**: Rotate all secrets when team member leaves
4. **Monitor Usage**: Log all deployment activities
5. **Encrypt at Rest**: Use encrypted storage for all secrets
6. **Secure Transmission**: Only share secrets via encrypted channels
7. **No Screenshots**: Never screenshot secrets (clipboard managers can expose)
8. **Use 2FA**: Enable two-factor authentication on all accounts (Apple ID, Google, etc.)

## Emergency Procedures

### Lost Keystore (Android)

**If not using Play App Signing:**
- Cannot update existing app in Play Store
- Must publish new app with different package name
- Lose all reviews, ratings, download count

**Prevention:** BACKUP KEYSTORES!

**If using Play App Signing:**
- Upload keystore can be rotated
- Production key safe with Google

### Compromised Credentials

**Immediate actions:**
1. Revoke compromised credentials
2. Generate new credentials
3. Update all systems with new credentials
4. Audit access logs for unauthorized use
5. Notify team
6. Document incident

## StackMap Reference

StackMap's secret management approach:
- iOS: fastlane match for certificates (encrypted git repo)
- Android: Keystore in secure location, keystore.properties in .gitignore
- Web: .env files in .gitignore
- Team: 1Password shared vault
- CI/CD: GitHub Actions secrets

See [reference-implementations.md](./reference-implementations.md) for .gitignore patterns and secret handling code.

## Summary Checklist

Before first deployment, ensure:

- [ ] All secrets generated
- [ ] All secrets backed up to 3+ locations
- [ ] All secrets stored in password manager
- [ ] Team members granted appropriate access
- [ ] .gitignore configured to exclude all secrets
- [ ] CI/CD environment variables configured (if applicable)
- [ ] Backup restoration tested
- [ ] Team trained on secret handling procedures

**Time investment:** 2-3 hours for initial setup, well worth it for secure deployments.
