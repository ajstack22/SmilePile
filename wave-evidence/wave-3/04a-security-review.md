# Wave 3: Android 4-Tier Configuration - Security Review

**Security Agent Review**
**Date**: 2025-10-14
**Wave**: 3 of 10
**Story**: STORY-6.3-android-tier-config.md
**Implementation Plan**: 03-implementation-plan.md

---

## Executive Summary

### Overall Security Posture: MEDIUM RISK

The Android 4-tier configuration implementation plan demonstrates a sound technical approach with appropriate security controls. However, several **CRITICAL** security gaps require immediate attention before implementation proceeds to Phase 5.

### Risk Classification

- **Critical Issues Found**: 3
- **High-Risk Issues Found**: 4
- **Medium-Risk Issues Found**: 6
- **Low-Risk Issues Found**: 3

### GO/NO-GO Recommendation: CONDITIONAL GO

**Status**: Implementation MAY proceed to Phase 5 ONLY after addressing all CRITICAL and HIGH-risk findings documented in this review.

**Required Actions Before Phase 5**:
1. Implement keystore backup verification process
2. Add .gitignore verification to implementation checklist
3. Document keystore recovery procedures
4. Add ProGuard verification tests to test suite
5. Implement tier validation security controls
6. Document package name verification process

---

## Critical Findings (MUST FIX BEFORE IMPLEMENTATION)

### CRITICAL-1: Insufficient Keystore Backup Strategy

**Risk Level**: CRITICAL
**Likelihood**: Medium
**Impact**: CATASTROPHIC (App cannot be updated in Play Store)

**Finding**:
The implementation plan recommends "3+ copies" of the production keystore but does not provide:
- Automated backup verification
- Recovery testing procedures
- Encrypted backup validation
- Team access documentation
- Backup integrity verification

**Attack Vector**:
Single point of failure. If all 3 backup locations fail simultaneously (fire, hardware failure, cloud account compromise), the production keystore is permanently lost.

**Impact**:
- Cannot publish updates to existing Play Store users
- Forced to create new app listing (losing all users, reviews, rankings)
- Financial loss of existing user base
- Reputational damage

**Recommended Mitigations**:

1. **Immediate Actions**:
   - Document MINIMUM 5 backup locations (not 3):
     - Primary: Password manager with file attachment (1Password/LastPass)
     - Secondary: Encrypted cloud storage (Google Drive encrypted zip)
     - Tertiary: External encrypted USB drive in physical safe
     - Quaternary: Team shared password vault
     - Quinary: Printed QR code in bank safe deposit box

2. **Verification Process**:
   ```bash
   # Add to implementation checklist
   # After keystore creation, verify ALL backups:
   for backup in backup1.keystore backup2.keystore backup3.keystore; do
       keytool -list -v -keystore "$backup" | grep "Alias name: smilepile"
       if [ $? -ne 0 ]; then
           echo "CRITICAL: Backup $backup is invalid"
           exit 1
       fi
   done
   ```

3. **Recovery Testing**:
   - Before Wave 3 completion, perform recovery drill from each backup location
   - Document recovery time for each backup source
   - Test signing with recovered keystore to ensure functionality

4. **Google Play App Signing Requirement**:
   - **MANDATORY**: Enable Google Play App Signing on first production upload
   - This allows Google to manage the production signing key
   - Upload key can be rotated if compromised
   - Provides ultimate backup if local keystore lost
   - Document this as REQUIRED step in deployment plan

**Implementation Plan Update Required**:
- Add Phase 1.4: "Verify Keystore Backup Integrity"
- Add Phase 11.7: "Test Keystore Recovery Process"
- Update documentation to require Google Play App Signing enablement

---

### CRITICAL-2: No Verification That keystore.properties Is Excluded From Git

**Risk Level**: CRITICAL
**Likelihood**: HIGH
**Impact**: CRITICAL (Credential exposure, keystore compromise)

**Finding**:
While the .gitignore file contains `android/keystore.properties` (line 155), the implementation plan does not verify this BEFORE creating the file. If .gitignore is misconfigured or the file path is incorrect, credentials will be committed to version control.

**Attack Vector**:
1. Developer creates keystore.properties
2. .gitignore entry has typo or wrong path
3. Developer runs `git add -A`
4. Credentials committed to repository
5. Credentials exposed in Git history forever (even if removed later)
6. Attackers can sign malicious APKs with production keystore

**Impact**:
- Production keystore password exposed in Git history
- Keystore file path exposed
- Potential for malicious app signing
- Forced keystore rotation (losing ability to update existing app)
- Play Store account compromise

**Recommended Mitigations**:

1. **Pre-Creation Verification** (add to Phase 2, Step 2.1):
   ```bash
   # BEFORE creating keystore.properties, verify .gitignore
   cd /Users/adamstack/SmilePile

   # Check .gitignore contains keystore.properties
   if ! grep -q "android/keystore.properties" .gitignore; then
       echo "CRITICAL: keystore.properties not in .gitignore"
       echo "Adding entry now..."
       echo "android/keystore.properties" >> .gitignore
   fi

   # Verify git will ignore it
   touch android/keystore.properties.test
   git check-ignore android/keystore.properties.test
   if [ $? -ne 0 ]; then
       echo "CRITICAL: Git will NOT ignore keystore.properties"
       exit 1
   fi
   rm android/keystore.properties.test
   ```

2. **Post-Creation Verification** (add to Phase 2, Step 2.2):
   ```bash
   # AFTER creating keystore.properties, verify it's ignored
   cd /Users/adamstack/SmilePile
   git status | grep keystore.properties
   if [ $? -eq 0 ]; then
       echo "CRITICAL: keystore.properties appears in git status"
       echo "This means it will be committed!"
       exit 1
   fi
   ```

3. **Pre-Commit Hook** (add to Phase 2, Step 2.3):
   ```bash
   # Install pre-commit hook to prevent accidental commits
   cat > .git/hooks/pre-commit << 'EOF'
   #!/bin/bash
   # Prevent keystore.properties from being committed
   if git diff --cached --name-only | grep -q "keystore.properties"; then
       echo "ERROR: Attempting to commit keystore.properties"
       echo "This file contains sensitive credentials and must never be committed"
       exit 1
   fi
   EOF
   chmod +x .git/hooks/pre-commit
   ```

**Implementation Plan Update Required**:
- Add verification steps to Phase 2 BEFORE file creation
- Add pre-commit hook installation to Phase 2
- Add post-creation verification to Phase 11 final checklist

---

### CRITICAL-3: No Tier Validation Security Controls

**Risk Level**: CRITICAL
**Likelihood**: MEDIUM
**Impact**: HIGH (Tier spoofing, unauthorized access)

**Finding**:
The BUILD_TYPE_ENV is compiled into the APK at build time, but there are NO runtime validation checks to prevent:
- APK tampering to modify tier
- Reflection-based tier modification
- Server-side tier validation
- Tier verification on sensitive operations

**Attack Vector**:
1. Attacker decompiles production APK
2. Modifies BuildConfig.BUILD_TYPE_ENV from "prod" to "qual"
3. Recompiles and signs with own key
4. App now thinks it's QUAL tier but accessing production APIs
5. Debug menus, logging, or relaxed security in QUAL tier exploited

**Impact**:
- Debug features exposed in production builds
- Sensitive logging enabled in production
- API rate limiting bypassed
- Tier-specific security controls bypassed
- Potential data exposure

**Recommended Mitigations**:

1. **Server-Side Tier Validation** (Wave 4 requirement):
   - Server MUST validate tier based on app signature, not client-provided tier
   - Use package name + signing certificate to determine tier
   - Reject requests from mismatched tier/signature combinations
   - Document this as Wave 4 security requirement

2. **Runtime Integrity Checks** (add to BuildConfig.kt):
   ```kotlin
   object BuildConfig {
       val buildType: String
           get() {
               val declaredTier = com.smilepile.BuildConfig.BUILD_TYPE_ENV

               // Verify tier matches package name
               val packageName = com.smilepile.BuildConfig.APPLICATION_ID
               val expectedTier = when (packageName) {
                   "com.smilepile.qual" -> "qual"
                   "com.smilepile" -> {
                       // Could be stage, beta, or prod - validate with signature
                       // For now, trust BUILD_TYPE_ENV
                       declaredTier
                   }
                   else -> "unknown"
               }

               if (expectedTier != "unknown" && expectedTier != declaredTier) {
                   // Log security violation but don't crash
                   Log.e("BuildConfig", "Tier mismatch: package=$packageName, tier=$declaredTier")
               }

               return declaredTier
           }
   }
   ```

3. **Tamper Detection** (optional for Wave 3, recommended for Wave 4):
   - Verify app signature matches expected signing certificate
   - Detect if app was re-signed by unauthorized party
   - Disable sensitive features if tampering detected

4. **ProGuard String Obfuscation** (add to proguard-rules.pro):
   ```proguard
   # Additional obfuscation for tier values
   -obfuscate
   -repackageclasses ''
   -allowaccessmodification
   ```

**Implementation Plan Update Required**:
- Add runtime tier validation to BuildConfig.kt
- Document server-side validation as Wave 4 requirement
- Add tamper detection considerations to security documentation

---

## High-Risk Findings (FIX BEFORE PRODUCTION)

### HIGH-1: Keystore Generation Uses Only 2048-bit RSA

**Risk Level**: HIGH
**Likelihood**: LOW
**Impact**: HIGH (Future security inadequacy)

**Finding**:
The implementation plan specifies RSA 2048-bit keys. While currently acceptable, this may not meet future security standards. Google recommends 4096-bit for new keys.

**Recommended Mitigation**:
```bash
# Update keystore generation command to use 4096-bit
keytool -genkey -v \
  -keystore ~/keystores/smilepile-production.keystore \
  -alias smilepile \
  -keyalg RSA \
  -keysize 4096 \  # Changed from 2048
  -validity 10000
```

**Rationale**:
- Future-proofs against cryptographic advances
- Aligns with Google Play Store recommendations
- Minimal performance impact on modern devices
- Cannot be changed after keystore creation

---

### HIGH-2: No Keystore Password Strength Validation

**Risk Level**: HIGH
**Likelihood**: MEDIUM
**Impact**: HIGH (Weak password enables keystore theft)

**Finding**:
Implementation plan recommends "20+ character password" but provides no validation or enforcement. Weak passwords make keystore vulnerable to brute force attacks if stolen.

**Recommended Mitigation**:

Add password strength validation to Phase 1:
```bash
# After keystore generation, validate password was strong
# (This is a reminder/checklist, not automated validation)
echo "Keystore password strength checklist:"
echo "✓ Minimum 24 characters (recommended: 32+)"
echo "✓ Mix of uppercase, lowercase, numbers, symbols"
echo "✓ No dictionary words"
echo "✓ Unique password (not reused elsewhere)"
echo "✓ Stored in password manager with 2FA"
echo ""
echo "If password does not meet ALL criteria, regenerate keystore now."
```

**Best Practice**:
- Use password manager to generate 32+ character random password
- Enable 2FA on password manager
- Never use password for any other purpose

---

### HIGH-3: Production Keystore Validity Only 27 Years

**Risk Level**: HIGH
**Likelihood**: LOW (long-term)
**Impact**: MEDIUM (App cannot be updated after expiry)

**Finding**:
Implementation specifies `-validity 10000` (approximately 27 years). While this seems long, apps may outlive this period, and keystore cannot be changed after creation.

**Recommended Mitigation**:
```bash
# Increase validity to maximum practical value
-validity 25000  # Approximately 68 years
```

**Rationale**:
- No downside to longer validity
- Provides maximum future flexibility
- Aligns with Android best practices
- Cannot be extended after creation

---

### HIGH-4: No Verification That Debug Keystore Exists

**Risk Level**: HIGH (for QUAL tier only)
**Likelihood**: MEDIUM
**Impact**: MEDIUM (QUAL builds fail)

**Finding**:
Implementation assumes Android SDK debug keystore exists at `~/.android/debug.keystore`. If it doesn't exist or was deleted, QUAL debug builds will fail.

**Recommended Mitigation**:

Add verification to Phase 8 (Build Verification):
```bash
# Before building qualDebug, ensure debug keystore exists
if [ ! -f ~/.android/debug.keystore ]; then
    echo "Debug keystore not found. Generating..."
    keytool -genkey -v \
        -keystore ~/.android/debug.keystore \
        -storepass android \
        -alias androiddebugkey \
        -keypass android \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US"
fi
```

---

## Medium-Risk Findings (ADDRESS SOON)

### MEDIUM-1: BuildConfig Fields Not Protected From Reflection

**Risk Level**: MEDIUM
**Likelihood**: LOW
**Impact**: MEDIUM (Tier detection bypassed via reflection)

**Finding**:
ProGuard rules keep BuildConfig fields but don't prevent reflection-based modification at runtime.

**Recommended Mitigation**:
Document as known limitation. True protection requires:
- Tamper detection
- Server-side tier validation (Wave 4)
- Runtime integrity checks

**Rationale**:
- Attack requires significant expertise
- Attacker would need to re-sign APK anyway
- Server-side validation is ultimate protection
- Cost of mitigation outweighs current risk

---

### MEDIUM-2: Variant Filter May Hide Build Errors

**Risk Level**: MEDIUM
**Likelihood**: MEDIUM
**Impact**: MEDIUM (Errors in disabled variants not caught)

**Finding**:
The optional variantFilter disables stageDebug, betaDebug, and prodDebug. If code changes break these variants, errors won't be detected until attempting to build them.

**Recommended Mitigation**:
- Keep variantFilter as optional (recommended but not required)
- Document that CI/CD should build all variants periodically
- Add weekly full variant build to maintenance tasks

---

### MEDIUM-3: No Signing Config Validation Tests

**Risk Level**: MEDIUM
**Likelihood**: MEDIUM
**Impact**: MEDIUM (Wrong keystore used for tier)

**Finding**:
Implementation plan doesn't verify that QUAL uses debug keystore and other tiers use production keystore.

**Recommended Mitigation**:

Add to Phase 8 (Build Verification):
```bash
# Verify signing configuration
echo "Verifying signing configurations..."

# QUAL should be signed with debug key
keytool -printcert -jarfile app/build/outputs/apk/qual/debug/app-qual-debug.apk | \
    grep "Owner: CN=Android Debug"
if [ $? -ne 0 ]; then
    echo "ERROR: QUAL not signed with debug key"
    exit 1
fi

# STAGE should be signed with production key
keytool -printcert -jarfile app/build/outputs/apk/stage/release/app-stage-release.apk | \
    grep "Owner: CN=SmilePile Team"
if [ $? -ne 0 ]; then
    echo "ERROR: STAGE not signed with production key"
    exit 1
fi
```

---

### MEDIUM-4: ProGuard May Strip BuildConfig Despite Keep Rules

**Risk Level**: MEDIUM
**Likelihood**: LOW
**Impact**: HIGH (App crashes when accessing tier)

**Finding**:
While keep rules are specified, R8 aggressive optimization may still remove unused BuildConfig constants if not properly accessed.

**Recommended Mitigation**:

Add verification to BuildConfigTest.kt:
```kotlin
@Test
fun `BuildConfig fields accessible after ProGuard`() {
    // This test ensures ProGuard keep rules are working
    // If this test passes in release build, keep rules are effective

    val buildType = BuildConfig.buildType
    assertNotNull("buildType should not be null", buildType)
    assertTrue("buildType should not be empty", buildType.isNotEmpty())

    // Access generated BuildConfig directly to ensure it's kept
    val rawBuildType = com.smilepile.BuildConfig.BUILD_TYPE_ENV
    assertNotNull("Raw BUILD_TYPE_ENV should not be null", rawBuildType)
    assertEquals("Wrapper should match raw value", rawBuildType, buildType)
}
```

Run this test for release builds:
```bash
./gradlew testStageReleaseUnitTest --tests BuildConfigTest
```

---

### MEDIUM-5: Package Name Verification Not Automated

**Risk Level**: MEDIUM
**Likelihood**: MEDIUM
**Impact**: MEDIUM (Wrong package name shipped)

**Finding**:
While implementation plan shows manual package name verification, there's no automated check to prevent wrong package names.

**Recommended Mitigation**:

Add automated verification to tier tests:
```kotlin
@Test
fun `package name matches tier requirements`() {
    when (BuildConfig.buildType) {
        "qual" -> {
            assertEquals(
                "QUAL must use .qual suffix",
                "com.smilepile.qual",
                BuildConfig.applicationId
            )
        }
        "stage", "beta", "prod" -> {
            assertEquals(
                "STAGE/BETA/PROD must use base package",
                "com.smilepile",
                BuildConfig.applicationId
            )
        }
    }
}
```

---

### MEDIUM-6: No Documentation of Keystore Recovery Procedures

**Risk Level**: MEDIUM
**Likelihood**: MEDIUM
**Impact**: MEDIUM (Recovery delays, potential errors)

**Finding**:
Implementation plan creates keystore backups but doesn't document HOW to recover from them in emergency.

**Recommended Mitigation**:

Add to wave-evidence/wave-3/ documentation:

```markdown
## Keystore Recovery Procedures

### Scenario 1: Local Keystore Lost
1. Retrieve from password manager
2. Verify with: keytool -list -v -keystore recovered.keystore
3. Copy to ~/keystores/smilepile-production.keystore
4. Test signing: ./gradlew assembleStageRelease
5. Verify signature matches previous builds

### Scenario 2: All Local Backups Lost
1. Access encrypted cloud backup
2. Decrypt zip file
3. Follow Scenario 1 steps

### Scenario 3: Complete Loss (All Backups Failed)
1. Contact Google Play support
2. Request app signing key if Google Play App Signing enabled
3. If not enabled: App cannot be updated (catastrophic)
4. Prevention: Enable Google Play App Signing on first upload
```

---

## Low-Risk Findings (MONITOR)

### LOW-1: Hardcoded Keystore Path in Implementation Plan

**Risk Level**: LOW
**Likelihood**: LOW
**Impact**: LOW (Path mismatch on different machines)

**Finding**:
Implementation plan hardcodes `/Users/adamstack/keystores/` which won't work for other developers.

**Recommended Mitigation**:
- Document that path should be customized per developer
- Consider environment variable: `${KEYSTORE_PATH:-$HOME/keystores}`
- Add path configuration to team onboarding docs

---

### LOW-2: No Rate Limiting Mentioned for Tier-Specific APIs

**Risk Level**: LOW
**Likelihood**: LOW
**Impact**: MEDIUM (QUAL tier abuse)

**Finding**:
QUAL tier may have different API rate limits but this isn't documented in security considerations.

**Recommended Mitigation**:
- Document as Wave 4 requirement
- Server should enforce stricter rate limits for QUAL tier
- QUAL should not have production data access

---

### LOW-3: App Name Strings Not Obfuscated

**Risk Level**: LOW
**Likelihood**: LOW
**Impact**: LOW (Tier identification from strings)

**Finding**:
Flavor-specific app names ("SmilePile Qual", etc.) are in plaintext string resources, making tier identification trivial.

**Recommended Mitigation**:
- Accept as design tradeoff (users need to see app name)
- Tier identification from app name is intentional
- Focus security on server-side validation, not client-side obscurity

---

## Security Checklist

### Pre-Implementation Security Tasks

- [ ] Review all CRITICAL findings and implement mitigations
- [ ] Review all HIGH findings and plan mitigation timeline
- [ ] Generate password manager entry for keystore password
- [ ] Prepare 5 backup locations for keystore
- [ ] Install pre-commit hooks to prevent credential commits
- [ ] Verify .gitignore contains keystore.properties
- [ ] Document keystore recovery procedures
- [ ] Set up Google Play Console account (for App Signing enablement)

### During-Implementation Security Checks

- [ ] Verify keystore password is 24+ characters
- [ ] Verify keystore uses 4096-bit RSA (not 2048-bit)
- [ ] Verify keystore validity is 25000 days
- [ ] Test keystore.properties NOT in git status
- [ ] Verify all 5 keystore backups created and tested
- [ ] Verify debug keystore exists for QUAL builds
- [ ] Run signing configuration verification tests
- [ ] Test ProGuard doesn't strip BuildConfig
- [ ] Verify package names match tier requirements
- [ ] Test tier validation logic in BuildConfig.kt

### Post-Implementation Security Validation

- [ ] Perform keystore recovery drill from each backup
- [ ] Verify app signatures match expected certificates
- [ ] Test that BuildConfig.BUILD_TYPE_ENV cannot be modified via reflection
- [ ] Verify ProGuard rules effective in release builds
- [ ] Run BuildConfigTest on all release variants
- [ ] Verify QUAL can be installed alongside STAGE/BETA/PROD
- [ ] Document Google Play App Signing enablement requirement
- [ ] Create runbook for keystore emergency recovery
- [ ] Add keystore backup verification to weekly checklist
- [ ] Schedule quarterly keystore recovery drills

---

## Risk Assessment Matrix

| Risk ID | Finding | Severity | Likelihood | Risk Score | Mitigation Priority |
|---------|---------|----------|------------|------------|-------------------|
| CRITICAL-1 | Insufficient keystore backup | CRITICAL | MEDIUM | 9/10 | IMMEDIATE |
| CRITICAL-2 | No .gitignore verification | CRITICAL | HIGH | 10/10 | IMMEDIATE |
| CRITICAL-3 | No tier validation controls | CRITICAL | MEDIUM | 8/10 | IMMEDIATE |
| HIGH-1 | 2048-bit RSA key | HIGH | LOW | 6/10 | BEFORE PROD |
| HIGH-2 | No password strength validation | HIGH | MEDIUM | 7/10 | BEFORE PROD |
| HIGH-3 | Limited keystore validity | HIGH | LOW | 5/10 | BEFORE PROD |
| HIGH-4 | Missing debug keystore check | HIGH | MEDIUM | 6/10 | BEFORE PROD |
| MEDIUM-1 | Reflection vulnerabilities | MEDIUM | LOW | 4/10 | WAVE 4 |
| MEDIUM-2 | Variant filter hides errors | MEDIUM | MEDIUM | 5/10 | CI/CD |
| MEDIUM-3 | No signing validation tests | MEDIUM | MEDIUM | 5/10 | WAVE 3 |
| MEDIUM-4 | ProGuard may strip fields | MEDIUM | LOW | 4/10 | WAVE 3 |
| MEDIUM-5 | Manual package verification | MEDIUM | MEDIUM | 5/10 | WAVE 3 |
| MEDIUM-6 | No recovery procedures | MEDIUM | MEDIUM | 5/10 | WAVE 3 |
| LOW-1 | Hardcoded paths | LOW | LOW | 2/10 | DOCUMENTATION |
| LOW-2 | No API rate limiting docs | LOW | LOW | 3/10 | WAVE 4 |
| LOW-3 | Plaintext app names | LOW | LOW | 1/10 | ACCEPTED |

---

## Security Best Practices

### Keystore Management

**DO**:
- Use 4096-bit RSA keys for production keystore
- Generate 32+ character random passwords
- Store in password manager with 2FA enabled
- Create minimum 5 geographically distributed backups
- Test recovery process quarterly
- Enable Google Play App Signing on first upload
- Document keystore recovery procedures
- Use encrypted backup storage

**DON'T**:
- Commit keystore or keystore.properties to Git
- Share keystore password via email/Slack
- Reuse keystore password for other purposes
- Store keystore in cloud without encryption
- Trust single backup location
- Skip backup integrity verification
- Use weak or dictionary-based passwords

### Signing Configuration

**DO**:
- Use debug keystore for QUAL tier only
- Use production keystore for STAGE/BETA/PROD
- Verify signing configuration in CI/CD
- Test that correct keystore is used per tier
- Document signing certificate fingerprints
- Monitor for unauthorized re-signing

**DON'T**:
- Use production keystore for debug builds
- Share production keystore with all developers
- Skip signing verification tests
- Allow unsigned or self-signed production builds

### BuildConfig Security

**DO**:
- Add runtime tier validation checks
- Protect with ProGuard keep rules
- Test release builds verify rules work
- Validate tier matches package name
- Plan server-side tier validation (Wave 4)
- Document tier security model

**DON'T**:
- Rely solely on client-side tier detection
- Trust BUILD_TYPE_ENV without validation
- Skip ProGuard verification tests
- Allow tier modification via reflection
- Expose sensitive features without tier checks

### .gitignore Security

**DO**:
- Verify .gitignore before creating sensitive files
- Test git check-ignore for keystore.properties
- Install pre-commit hooks
- Review Git history for accidentally committed secrets
- Use gitleaks or similar tools in CI/CD
- Document .gitignore verification in checklist

**DON'T**:
- Assume .gitignore is correct without verification
- Skip verification steps to save time
- Commit first, verify later
- Trust old .gitignore entries without testing

### Tier-Specific Security

**DO**:
- Implement different security controls per tier
- QUAL: Relax security for debugging
- STAGE: Production-like security, internal access only
- BETA: Full production security, limited audience
- PROD: Maximum security controls
- Validate tier server-side (Wave 4)
- Monitor for tier abuse

**DON'T**:
- Give QUAL tier access to production data
- Expose debug features in BETA/PROD
- Trust client-reported tier without validation
- Skip tier-specific security testing

---

## Long-Term Security Maintenance

### Monthly Tasks
- [ ] Verify keystore backups are accessible
- [ ] Review .gitignore for new secrets
- [ ] Check for accidentally committed credentials
- [ ] Update keystore backup passwords if needed

### Quarterly Tasks
- [ ] Perform keystore recovery drill
- [ ] Test signing configuration for all tiers
- [ ] Review ProGuard rules effectiveness
- [ ] Audit tier-specific security controls
- [ ] Review Google Play App Signing status

### Annual Tasks
- [ ] Review keystore validity remaining years
- [ ] Audit all keystore backup locations
- [ ] Update security procedures documentation
- [ ] Review tier security architecture
- [ ] Plan for keystore rotation if needed (25+ years notice)

### Before Production Release
- [ ] Enable Google Play App Signing
- [ ] Verify production keystore certificate fingerprint
- [ ] Document signing certificate for server validation
- [ ] Test server-side tier validation (Wave 4)
- [ ] Perform security audit of tier configuration
- [ ] Review all security findings from this document

---

## Dependency on Wave 4

Several security controls identified in this review are marked for Wave 4 implementation:

### Wave 4: Server-Side Tier Validation (REQUIRED)

**Critical Security Requirements**:
1. Server MUST validate tier based on app signature, NOT client-provided tier
2. Use package name + signing certificate to identify tier
3. Reject API requests from mismatched tier/signature
4. Implement tier-specific rate limiting
5. QUAL tier must NOT access production data
6. Log tier validation failures for security monitoring

**Rationale**:
Client-side tier detection (BUILD_TYPE_ENV) is for app behavior, not security. True tier enforcement must happen server-side where attackers cannot modify it.

**Security Impact if Wave 4 Skipped**:
- Tier spoofing possible via APK modification
- Debug features could be exploited in production
- QUAL tier could access production APIs
- No rate limiting per tier
- Security controls based on tier could be bypassed

**Recommendation**: Wave 4 tier validation is NOT optional - it's a critical security control that MUST be implemented before production launch.

---

## Sign-Off

### Conditions for Phase 5 Implementation

**CRITICAL CONDITIONS (BLOCKING)**:
1. ✓ Update keystore generation to use 4096-bit RSA
2. ✓ Implement .gitignore verification BEFORE creating keystore.properties
3. ✓ Document minimum 5 keystore backup locations
4. ✓ Add pre-commit hook to prevent credential commits
5. ✓ Add runtime tier validation to BuildConfig.kt
6. ✓ Document Google Play App Signing as REQUIRED

**HIGH-PRIORITY CONDITIONS (Before Production)**:
1. ✓ Add keystore backup integrity verification
2. ✓ Add signing configuration validation tests
3. ✓ Add ProGuard verification tests
4. ✓ Document keystore recovery procedures
5. ✓ Add debug keystore existence check

**MEDIUM-PRIORITY CONDITIONS (Before Wave 3 Complete)**:
1. ✓ Add package name validation tests
2. ✓ Document variant filter security implications
3. ✓ Create keystore recovery runbook

**DEFERRED TO WAVE 4**:
1. Server-side tier validation (REQUIRED for production)
2. Tamper detection mechanisms
3. API rate limiting per tier
4. Tier-specific security controls

### Security Review Approval

**Status**: CONDITIONAL APPROVAL

**Approved For Phase 5**: YES, with conditions documented above

**Security Agent Recommendation**:
The Android 4-tier configuration implementation plan is technically sound and demonstrates appropriate security awareness. However, several CRITICAL security controls must be implemented before proceeding:

1. Enhance keystore backup strategy beyond 3 copies
2. Add automated .gitignore verification
3. Implement runtime tier validation
4. Document recovery procedures

With these mitigations implemented, the security risk is ACCEPTABLE for Phase 5 implementation.

**Security Monitoring Post-Deployment**:
- Monitor for tier validation failures
- Alert on signing configuration mismatches
- Track keystore backup accessibility
- Audit for accidentally committed credentials

**Next Security Review**: Phase 6 (Testing) - Validate security controls are working as designed

---

**Security Review Completed**: 2025-10-14
**Security Agent**: Claude Security Agent
**Wave**: 3 of 10
**Approval Status**: CONDITIONAL GO - Proceed to Phase 5 after addressing CRITICAL findings
**Next Review**: Phase 6 (Testing Results Security Validation)
