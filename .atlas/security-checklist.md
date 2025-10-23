# SmilePile Security Checklist

## Authentication & Authorization

### Biometric Authentication
- [ ] BiometricManager properly initialized and configured
- [ ] Biometric authentication available check performed
- [ ] Fallback to PIN/password when biometrics unavailable
- [ ] BiometricPrompt.PromptInfo configured with proper title/subtitle
- [ ] Biometric authentication errors handled gracefully

### Kids Mode Protection
- [ ] Kids Mode PIN enforcement active
- [ ] Parent Mode requires authentication to exit Kids Mode
- [ ] Kids Mode restrictions properly applied to UI
- [ ] PIN complexity requirements enforced (minimum length)
- [ ] Failed PIN attempt tracking and lockout

### Session Management
- [ ] InactivityManager monitoring user activity
- [ ] Session timeout configured (default 5 minutes)
- [ ] Automatic lock on inactivity
- [ ] Re-authentication required after timeout
- [ ] Background app state triggers security lock

## Photo & Metadata Protection

### Photo Data Security
- [ ] MetadataEncryption service encrypting sensitive metadata
- [ ] Photo IDs (PHAsset.localIdentifier/Uri.toString) used instead of file paths
- [ ] No photo file paths exposed in logs or UI
- [ ] Category associations encrypted in storage
- [ ] Photo deletion properly removes all associated data

### Demo Mode Isolation
- [ ] Demo photos clearly marked and isolated
- [ ] Demo mode data cannot mix with real photos
- [ ] Demo mode exit clears all demo data
- [ ] Demo photos stored in separate resource directory

## Secure Storage

### Android Secure Storage
- [ ] SecurePreferencesManager using EncryptedSharedPreferences
- [ ] MasterKey properly generated and managed
- [ ] AES256_GCM encryption for SharedPreferences
- [ ] No sensitive data in regular SharedPreferences
- [ ] Encryption keys stored in Android Keystore

### iOS Secure Storage
- [ ] Keychain Services API used for sensitive data
- [ ] kSecAttrAccessibleWhenUnlockedThisDeviceOnly access control
- [ ] Keychain items properly cleaned on app uninstall
- [ ] Biometric-protected keychain items configured
- [ ] No sensitive data in UserDefaults

### Database Security
- [ ] Room database encryption enabled (if applicable)
- [ ] SQLCipher integration configured (if used)
- [ ] Database file permissions restricted
- [ ] Sensitive queries not logged
- [ ] Database backups encrypted

## Activity Security

### Android Activity Protection
- [ ] MainActivity extends SecureActivity base class
- [ ] All sensitive screens extend SecureActivity
- [ ] FLAG_SECURE set to prevent screenshots
- [ ] Content hidden in recent apps switcher
- [ ] Screen recording prevention active

### Screen Security
- [ ] Sensitive content blurred/hidden when app backgrounded
- [ ] Screenshot prevention in Kids Mode
- [ ] Screen timeout triggers lock screen
- [ ] Sensitive dialogs marked as secure
- [ ] WebView screenshot prevention (if applicable)

## Input Validation

### Photo Import Validation
- [ ] File type validation (JPEG, PNG, HEIC)
- [ ] File size limits enforced
- [ ] Image integrity verification
- [ ] EXIF data sanitization
- [ ] Malformed file rejection

### User Input Validation
- [ ] Category name length limits
- [ ] Special character sanitization
- [ ] SQL injection prevention
- [ ] XSS prevention in any web views
- [ ] PIN format validation (digits only)

## Data Protection

### Backup & Export Security
- [ ] Backup files encrypted before export
- [ ] Encryption key derived from user PIN/password
- [ ] Backup integrity verification (checksum/signature)
- [ ] Secure backup file deletion after transfer
- [ ] Import validation before data restoration

### Data Sharing Controls
- [ ] Photo sharing excludes sensitive metadata
- [ ] Temporary share files auto-deleted
- [ ] Share intent filters configured
- [ ] No accidental data exposure in share sheets
- [ ] Clipboard protection for sensitive data

## Code Security

### Secret Management
- [ ] No hardcoded API keys or secrets
- [ ] BuildConfig used for configuration values
- [ ] Sensitive strings obfuscated
- [ ] Debug logging disabled in release builds
- [ ] ProGuard/R8 rules properly configured

### Dependency Injection Security
- [ ] Hilt modules properly scoped
- [ ] Security managers singleton-scoped
- [ ] No security object leaks
- [ ] Proper cleanup in onDestroy
- [ ] Thread-safe security operations

## Platform-Specific Security

### iOS Security Requirements
- [ ] NSPhotoLibraryUsageDescription properly configured
- [ ] NSFaceIDUsageDescription set for Face ID
- [ ] LAContext evaluation policy configured
- [ ] Photo library authorization checked
- [ ] Background blur view implemented

### Android Security Requirements
- [ ] USE_BIOMETRIC permission declared
- [ ] Runtime permissions properly requested
- [ ] BiometricManager.Authenticators configured
- [ ] Cryptographic operations use AndroidKeystore
- [ ] Security patches targeting latest API level

## Testing & Validation

### Security Testing
- [ ] Unit tests for SecurityManager
- [ ] Unit tests for BiometricManager
- [ ] Unit tests for encryption/decryption
- [ ] Integration tests for authentication flow
- [ ] UI tests for Kids Mode restrictions

### Penetration Testing Areas
- [ ] Authentication bypass attempts
- [ ] Data extraction from backups
- [ ] Screenshot/recording circumvention
- [ ] PIN brute force protection
- [ ] Memory dump analysis

## Compliance & Privacy

### Data Privacy
- [ ] No network calls for photo data
- [ ] All data stored locally only
- [ ] No third-party analytics in Kids Mode
- [ ] User data deletion capability
- [ ] Clear data retention policy

### Child Safety
- [ ] COPPA compliance for Kids Mode
- [ ] Parental controls enforced
- [ ] Age-appropriate content only
- [ ] No external links in Kids Mode
- [ ] Safe browsing if WebView used

## Deployment Security

### Build Security
- [ ] Release builds use release signing keys
- [ ] Debug builds clearly marked
- [ ] Obfuscation enabled in release
- [ ] Debug logs stripped from release
- [ ] Security-sensitive code not in debug builds

### Distribution Security
- [ ] APK/IPA signature verification
- [ ] Certificate pinning (if applicable)
- [ ] Tamper detection implemented
- [ ] Store listing security review
- [ ] Update mechanism secure

## Security Monitoring

### Runtime Monitoring
- [ ] Crash reporting excludes sensitive data
- [ ] Security event logging (failed auth attempts)
- [ ] Anomaly detection for unusual activity
- [ ] Root/jailbreak detection
- [ ] Debugger detection in release

### Security Updates
- [ ] Regular security dependency updates
- [ ] Security patch monitoring
- [ ] Vulnerability scanning in CI/CD
- [ ] Security advisory subscriptions
- [ ] Incident response plan defined

## Reference Implementation Locations

- **Android Security**: `/Users/adamstack/SmilePile/android/app/src/main/java/com/smilepile/security/`
  - SecurityManager.kt
  - BiometricManager.kt
  - InactivityManager.kt
  - SecureActivity.kt
  - MetadataEncryption.kt
  - SecurePreferencesManager.kt

- **iOS Security**: Check equivalent Swift implementations in iOS project structure

- **Configuration**:
  - Android: `build.gradle.kts`, `AndroidManifest.xml`
  - iOS: `Info.plist`, `Entitlements.plist`

## Notes

- This checklist should be reviewed before each release
- Security items should be validated during Atlas Phase 4 (Security Review)
- Any security exceptions must be documented with justification
- Regular security audits should reference this checklist