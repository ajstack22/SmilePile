# Wave 4: Security Hardening & Final Polish (Weeks 7-8)

## CRITICAL: You are now the Atlas Orchestrator
You coordinate development through specialized agents. NEVER implement directly. Your role is to orchestrate Wave 4 of the SmilePile refactor, implementing security best practices based on Manylla's zero-knowledge approach while keeping it simple.

## Project Context
- **Current Issues**: Alpha security library, SHA-256 for PINs, deprecated APIs
- **Philosophy**: "Use proven libraries, not custom crypto"
- **Key Focus**: Protect child metadata, not photos (photos accessible in MediaStore)
- **Goal**: Production-ready security without over-engineering

## Wave 4 Objectives
1. Upgrade to stable Android security libraries
2. Implement proper PIN encryption with Android Keystore
3. Add biometric authentication (optional enhancement)
4. Fix ID conversion issue (simple approach)
5. Final polish and deprecated API updates

## Atlas Orchestration Commands

### Phase 1: Initialize Wave
```bash
# Resume project context
python3 00_orchestrator_context.py resume
python3 00_orchestrator_context.py objective "Implement production security and final polish"

# Create security stories
python3 02_create_story.py story "Upgrade to stable security-crypto library" --priority critical
python3 02_create_story.py story "Implement Android Keystore for PIN" --priority critical
python3 02_create_story.py story "Add biometric authentication option" --priority high
python3 02_create_story.py story "Fix ID conversion pattern" --priority high
python3 02_create_story.py story "Update deprecated APIs" --priority medium

# Start workflow
python3 03_adversarial_workflow.py start WAVE4
```

### Phase 2: Parallel Agent Execution

#### Security Analysis Agents (Spawn 3 in parallel - Hour 1)
```
Agent 1: "Audit current security implementation using 01_research.py"
Tasks:
- Review SecurePreferencesManager implementation
- Find all uses of alpha security-crypto
- Document current PIN hashing approach
- List all sensitive data needing protection

Agent 2: "Research Android security best practices using 01_research.py"
Tasks:
- Android Keystore implementation patterns
- EncryptedSharedPreferences usage
- Biometric authentication setup
- Key generation and storage

Agent 3: "Find and document deprecated APIs using 01_research.py"
Tasks:
- Locate onBackPressed usage
- Find deprecated system UI flags
- Document migration paths
- Identify quick wins vs complex migrations
```

#### Security Implementation Agents (Spawn 4 in parallel - Hours 2-5)

```
Agent 1: "Upgrade security dependencies and implement Android Keystore"
Task: Replace alpha library with stable, implement proper encryption

Dependencies to update in build.gradle.kts:
- androidx.security:security-crypto:1.1.0-alpha06 → 1.0.0 (stable)

Implementation:
class SecureStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "smilepile_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePin(pin: String) {
        // No custom hashing - let Android handle it
        encryptedPrefs.edit().putString("user_pin", pin).apply()
    }

    fun verifyPin(input: String): Boolean {
        val stored = encryptedPrefs.getString("user_pin", null)
        return stored == input  // Comparison happens on encrypted data
    }
}

Agent 2: "Implement biometric authentication"
Task: Add fingerprint/face unlock as optional PIN alternative

Implementation:
class BiometricManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int, errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    // Fall back to PIN
                    onError()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Try again or fall back to PIN
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Parent Mode")
            .setSubtitle("Use your fingerprint or face")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

Agent 3: "Fix ID conversion issue"
Task: Remove dangerous hashCode() conversion

Current problem in PhotoRepositoryImpl.kt:
id = this.id.hashCode().toLong()  // DANGEROUS

Simple fix approach:
// Option 1: Use path as unique identifier
class PhotoRepositoryImpl {
    private fun PhotoEntity.toPhoto(): Photo {
        return Photo(
            id = 0L,  // Let domain layer handle its own IDs
            path = this.uri,  // This is the real unique ID
            categoryId = this.categoryId.toLongOrNull() ?: 0L,
            // ... rest of mapping
        )
    }
}

// Option 2: Maintain mapping table
class PhotoIdMapper {
    private val entityToPhoto = mutableMapOf<String, Long>()
    private var nextId = 1L

    fun getPhotoId(entityId: String): Long {
        return entityToPhoto.getOrPut(entityId) { nextId++ }
    }
}

Agent 4: "Update deprecated APIs"
Task: Fix deprecated onBackPressed and system UI flags

Fix onBackPressed in MainActivity.kt:
// OLD (Deprecated)
override fun onBackPressed() {
    if (isKidsMode) {
        // Block back
    } else {
        super.onBackPressed()
    }
}

// NEW (OnBackPressedCallback)
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (modeManager.isKidsMode()) {
                    // Block or show PIN dialog
                } else {
                    finish()
                }
            }
        })
    }
}

Fix system UI flags:
// OLD (Deprecated)
window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

// NEW (WindowInsetsController)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    window.insetsController?.hide(WindowInsets.Type.statusBars())
} else {
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
}
```

#### Metadata Encryption Agent (Hour 6)
```
Agent: "Implement selective metadata encryption"
Task: Encrypt sensitive child data while keeping photos accessible

Implementation pattern from Manylla:
@Entity
data class PhotoEntity(
    val id: String,
    val mediaStoreUri: String,  // NOT encrypted - need for loading
    val encryptedMetadata: ByteArray  // Encrypted blob
)

data class PhotoMetadata(
    val childName: String?,
    val notes: String?,
    val tags: List<String>
)

class MetadataEncryption @Inject constructor(
    private val secureStorage: SecureStorageManager
) {
    fun encryptMetadata(metadata: PhotoMetadata): ByteArray {
        val json = gson.toJson(metadata)
        return secureStorage.encrypt(json.toByteArray())
    }

    fun decryptMetadata(encrypted: ByteArray): PhotoMetadata {
        val json = String(secureStorage.decrypt(encrypted))
        return gson.fromJson(json, PhotoMetadata::class.java)
    }
}
```

### Phase 3: Polish & Validation

```
Agent: "Final polish and comprehensive security validation"
Tasks:
- Add screenshot prevention in Parent Mode settings
- Implement app timeout (return to Kids Mode after inactivity)
- Run security validation tests
- Verify biometric fallback to PIN works
- Test encrypted metadata round-trip
- Collect security evidence
```

## Security Implementation Guidelines

### Android Keystore Best Practices
```kotlin
// DO: Use Android's built-in encryption
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

// DON'T: Roll your own crypto
fun hashPin(pin: String): String {
    return MessageDigest.getInstance("SHA-256")... // NO!
}
```

### Biometric Implementation
```kotlin
// Check availability first
fun isBiometricAvailable(): Boolean {
    val biometricManager = BiometricManager.from(context)
    return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
        BiometricManager.BIOMETRIC_SUCCESS -> true
        else -> false
    }
}

// Always provide PIN fallback
if (isBiometricAvailable() && userPrefersBiometric) {
    authenticateWithBiometric()
} else {
    authenticateWithPin()
}
```

### Screenshot Prevention (Sensitive Screens)
```kotlin
// In Parent Mode settings where PIN is visible
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Prevent screenshots in sensitive screens
    if (isParentModeSettings) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}
```

### App Timeout Implementation
```kotlin
class InactivityManager @Inject constructor(
    private val modeManager: ModeManager
) {
    private var lastInteraction = System.currentTimeMillis()
    private val TIMEOUT_MS = 5 * 60 * 1000  // 5 minutes

    fun onUserInteraction() {
        lastInteraction = System.currentTimeMillis()
    }

    fun checkTimeout() {
        if (System.currentTimeMillis() - lastInteraction > TIMEOUT_MS) {
            if (modeManager.isParentMode()) {
                modeManager.setMode(AppMode.KIDS)  // Auto-return to Kids Mode
            }
        }
    }
}
```

## Success Criteria & Evidence

### Security Validation
```bash
# Dependency security check
./gradlew dependencyCheckAnalyze

# Lint security rules
./gradlew lint

# Custom security validation
python3 03_adversarial_workflow.py execute evidence --type security
```

Evidence must show:
1. ✅ No alpha/beta security libraries
2. ✅ PIN stored with Android Keystore encryption
3. ✅ Biometric authentication working with PIN fallback
4. ✅ Metadata encrypted, photos still accessible
5. ✅ No deprecated API warnings
6. ✅ ID conversion issue fixed (no hashCode)
7. ✅ Screenshot prevention in sensitive screens
8. ✅ Auto-timeout to Kids Mode working

### Performance Validation
```bash
# Ensure security didn't impact performance
python3 03_adversarial_workflow.py execute evidence --type performance

# Check encryption overhead
- Metadata encryption/decryption < 50ms
- Biometric prompt < 1 second
- No UI lag from security features
```

## Key Lessons Applied

### From Manylla (Zero-Knowledge Security):
✅ **Encrypt metadata, not photos** - Photos accessible, metadata protected
✅ **Client-side only** - No server, no accounts, no leaks
✅ **Use proven libraries** - Android Keystore, not custom crypto
✅ **Simple key management** - No rotation, no complex schemes

### From StackMap (Pragmatic Security):
✅ **Biometric with fallback** - Always have PIN option
✅ **Auto-timeout** - Return to safe state
✅ **Screenshot prevention** - Only where needed
✅ **No over-engineering** - Simple, proven patterns

## Common Security Pitfalls to Avoid

❌ **Custom Encryption**
```kotlin
// NEVER: Write your own encryption
fun encrypt(data: String): String {
    return Base64.encode(data.reversed())  // This is not encryption!
}

// ALWAYS: Use Android Keystore
EncryptedSharedPreferences.create(...)
```

❌ **Storing Sensitive Data in Clear**
```kotlin
// BAD: Plain SharedPreferences
prefs.putString("pin", userPin)

// GOOD: EncryptedSharedPreferences
encryptedPrefs.putString("pin", userPin)
```

❌ **No Fallback Authentication**
```kotlin
// BAD: Biometric only
if (!biometricSuccess) {
    showError("Cannot access")  // User locked out!
}

// GOOD: Always have fallback
if (!biometricSuccess) {
    showPinDialog()  // User can still get in
}
```

## Parallel Execution Timeline

```
Hour 1: Security Analysis (3 agents)
Hours 2-5: Implementation (4 agents)
Hour 6: Metadata Encryption (1 agent)
Hour 7: Polish & Validation (1 agent)
Total: 7 hours (vs 28 hours sequential)
```

## Final Production Checklist

Before marking Wave 4 complete:
- [ ] Security-crypto library upgraded to stable (1.0.0+)
- [ ] Android Keystore properly implemented
- [ ] PIN encryption using EncryptedSharedPreferences
- [ ] Biometric authentication with PIN fallback
- [ ] Metadata encryption working
- [ ] Photos remain accessible in Gallery
- [ ] ID conversion fixed (no hashCode)
- [ ] Deprecated APIs updated
- [ ] Screenshot prevention in Parent settings
- [ ] Auto-timeout to Kids Mode
- [ ] Security scan passing (no high/critical)
- [ ] Performance impact < 50ms
- [ ] All smoke tests still passing

## Final App State After All Waves

### What We Built:
✅ **3 Integration Tests** - Critical paths covered
✅ **One-Command Deployment** - Automated validation
✅ **Modular Components** - No file > 250 lines
✅ **Photo Library Management** - Remove without deletion
✅ **Local Backup/Restore** - Simple JSON export/import
✅ **Production Security** - Android Keystore, biometrics
✅ **TODO Count < 20** - Cleaned up technical debt

### What We Didn't Build (Intentionally):
❌ Cloud sync - Not needed for local app
❌ Photo sharing - Complexity without value
❌ Complex undo - Photos never deleted from device
❌ Voice search - Nice to have, not critical
❌ 80% test coverage - 30% is sufficient

### Metrics Achieved:
- Development time: 8 weeks (reality-based)
- Test coverage: 30% (critical paths)
- File sizes: All < 250 lines
- Security: Production-ready
- Performance: No degradation
- Maintainability: Greatly improved

---

**REMEMBER**: This is the final wave. Focus on security and polish. Don't add new features. Use proven Android security APIs. Test everything. Ship with confidence.

**START COMMAND**: Copy this entire file and paste to your LLM to begin Wave 4 execution.

**CONGRATULATIONS**: After Wave 4, SmilePile will be production-ready with pragmatic solutions based on real-world lessons from StackMap and Manylla teams!