# SmilePile Security Architecture

## Overview
SmilePile implements defense-in-depth security to protect children's photos and personal information.

## Security Layers

### 1. Data Protection
- **Encryption at Rest**: Android Keystore for key management
- **SecureStorageManager**: Handles all encrypted storage operations
- **MetadataEncryption**: Encrypts sensitive child information

### 2. Access Control
- **PIN/Pattern Lock**: Parent-only access to settings
- **Biometric Authentication**: Face ID/Touch ID support
- **Kids Mode**: Restricted access for children
- **Inactivity Timeout**: Auto-lock after 5 minutes

### 3. File Operations Security
- **Circuit Breaker Pattern**: Prevents cascading failures
- **Atomic Operations**: All-or-nothing file moves
- **Validation Checks**: File integrity verification
- **Error Recovery**: Automatic cleanup on failure

## Threat Model

### Assets to Protect
1. Children's photos
2. Personal metadata (names, ages, notes)
3. Parental access controls
4. App configuration

### Threat Vectors
1. **Unauthorized Access**
   - Mitigation: PIN/biometric authentication
2. **Data Leakage**
   - Mitigation: Encryption, secure preferences
3. **File Corruption**
   - Mitigation: Atomic operations, validation
4. **Malicious Import**
   - Mitigation: File validation, size limits

## Security Components

### SecurePreferencesManager
- Manages encrypted SharedPreferences
- Handles PIN/pattern storage
- Controls security settings

### SecureStorageManager
- Android Keystore integration
- AES-GCM encryption
- Key rotation support

### InactivityManager
- Session timeout handling
- Activity monitoring
- Auto-lock functionality

### CircuitBreaker
- Resilient file operations
- Failure threshold management
- Automatic recovery

## Security Best Practices

1. **Never log sensitive data**
2. **Always validate file operations**
3. **Use atomic operations for critical paths**
4. **Implement proper error handling**
5. **Regular security audits via SonarCloud**

## Compliance

- COPPA compliant (children's data protection)
- No network transmission of photos
- Local-only data storage
- Parental consent through PIN/biometric

## Incident Response

1. **Detection**: Error monitoring and logging
2. **Containment**: Circuit breaker prevents cascading failures
3. **Recovery**: Automatic cleanup and state restoration
4. **Analysis**: Error reports for debugging

## Security Testing

- Unit tests for all security components
- Integration tests for authentication flows
- SonarCloud security scanning
- Manual penetration testing (planned)

---
*Last Updated: September 26, 2025*
*Version: 1.0*