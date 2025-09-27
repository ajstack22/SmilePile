# SmilePile

Android and iOS photo viewing application with advanced security and parental controls.

## Security Features (Sprint 6)

### Backup Encryption
- **PBKDF2 Encryption**: Backup data protected with 600,000 PBKDF2 iterations
- **AES-256-GCM**: Military-grade encryption for all backup files
- **Device Integration**: Automatically uses device PIN/pattern when available
- **Secure Key Management**: Encryption keys never stored in plaintext
- **Fallback Authentication**: User password option when device lock unavailable

### Kids Mode Security
- **Time Manipulation Protection**: NTP verification prevents clock tampering
- **Reward Integrity**: Cryptographic signing prevents reward system abuse
- **Rate Limiting**: Anti-farming protections (10 rewards/minute maximum)
- **Nonce Tracking**: Replay attack prevention
- **Secure Time Tracking**: Tamper-resistant daily usage accumulation

## Parental Controls

### Time Management
- Daily time limits with persistent tracking across sessions
- 5-minute and 1-minute warnings before limit reached
- Parent override capability with PIN authentication
- Midnight reset with background/foreground awareness
- Visual countdown timer and usage statistics

### Reward System
- Virtual sticker collection for positive reinforcement
- Achievement badges for milestone completion
- Daily streak tracking for consistent usage
- Parent-controlled reward settings and thresholds
- Anti-tampering cryptographic validation

## Configuration Notes

### Security Settings
- Backup encryption automatically enabled with device lock
- Manual password setup required for devices without PIN/pattern
- Security status visible in app settings
- Encryption can be disabled by advanced users (not recommended)

### Kids Mode Setup
- Time limits configurable from 15 minutes to 2 hours
- Reward system can be enabled/disabled per parent preference
- Achievement thresholds customizable for different age groups
- Parent PIN required for all configuration changes

## Technical Requirements
- Minimum Android API level: 23 (Android 6.0)
- Minimum iOS version: 14.0
- Network access required for NTP time verification
- Secure storage for encryption keys and reward data
