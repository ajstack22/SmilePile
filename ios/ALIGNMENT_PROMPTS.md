# iOS-Android Feature Alignment Implementation Prompts

## Overview
These prompts guide the implementation of Android feature parity in the iOS SmilePile application using the Atlas workflow methodology.

---

## 1. Kids Mode (Display Mode) Alignment

### Task Description
Align iOS "Display Mode" with Android's Kids Mode functionality, ensuring consistent behavior for child-safe photo viewing.

### Atlas Workflow Command
```bash
python3 atlas/core/atlas_workflow.py feature "Align iOS Kids Mode with Android implementation for child-safe photo viewing"
```

### Implementation Requirements
- **Research Phase**: Analyze Android's `KidsModeGalleryScreen.kt` implementation
- **Story Creation**:
  - Kids Mode should show only photos from selected categories
  - Fullscreen immersive viewing without UI controls
  - Swipe gestures for navigation
  - Exit only through parental PIN/biometric
- **Implementation Focus**:
  - Port category filtering logic from Android
  - Implement fullscreen photo viewer
  - Add gesture recognizers for swiping
  - Integrate with authentication system

### Key Android Files to Reference
- `android/app/src/main/java/com/smilepile/ui/screens/KidsModeGalleryScreen.kt`
- `android/app/src/main/java/com/smilepile/ui/viewmodels/ParentalControlsViewModel.kt`

---

## 2. Import/Export Functionality

### Task Description
Implement ZIP-based photo backup and restore functionality matching Android's implementation.

### Atlas Workflow Command
```bash
python3 atlas/core/atlas_workflow.py feature "Implement ZIP import/export for photo backup matching Android functionality"
```

### Implementation Requirements
- **Research Phase**: Study Android's `StorageManager.kt` ZIP handling
- **Story Creation**:
  - Export photos to encrypted ZIP with metadata
  - Import ZIP files preserving categories and dates
  - Progress indicators during operations
  - Error handling and recovery
- **Implementation Focus**:
  - Use iOS ZIPFoundation or similar
  - Implement AES encryption for secure backups
  - Preserve photo metadata in JSON manifest
  - Handle large file operations efficiently

### Key Android Files to Reference
- `android/app/src/main/java/com/smilepile/storage/StorageManager.kt`
- ZIP structure: photos + metadata.json

---

## 3. PIN Authentication Implementation

### Task Description
Implement 4-digit PIN authentication system for parental controls and app security.

### Atlas Workflow Command
```bash
python3 atlas/core/atlas_workflow.py feature "Implement 4-digit PIN authentication matching Android security features"
```

### Implementation Requirements
- **Research Phase**: Analyze Android's `SecurePreferencesManager.kt`
- **Story Creation**:
  - PIN setup during onboarding
  - Secure storage using Keychain
  - PIN verification for sensitive operations
  - Failed attempt tracking and lockout
- **Implementation Focus**:
  - Use iOS Keychain Services for secure storage
  - Implement PIN entry UI with number pad
  - Add attempt limiting (5 failed = 30min lockout)
  - Support PIN reset with security questions

### Key Android Files to Reference
- `android/app/src/main/java/com/smilepile/security/SecurePreferencesManager.kt`
- `android/app/src/main/java/com/smilepile/ui/screens/ParentalLockScreen.kt`

---

## 4. Biometric Authentication

### Task Description
Implement Face ID and Touch ID support matching Android's biometric authentication.

### Atlas Workflow Command
```bash
python3 atlas/core/atlas_workflow.py feature "Add biometric authentication (Face ID/Touch ID) matching Android implementation"
```

### Implementation Requirements
- **Research Phase**: Study Android's `BiometricManager.kt`
- **Story Creation**:
  - Face ID/Touch ID enrollment
  - Fallback to PIN when biometrics fail
  - Settings toggle for biometric preference
  - Secure enclave integration
- **Implementation Focus**:
  - Use LocalAuthentication framework
  - Handle all biometric error cases
  - Implement smooth fallback to PIN
  - Store biometric preference securely

### Key Android Files to Reference
- `android/app/src/main/java/com/smilepile/security/BiometricManager.kt`
- `android/app/src/main/java/com/smilepile/security/SecureActivity.kt`

---

## 5. Photo Addition Flow

### Task Description
Implement photo import from camera roll and camera capture matching Android's flow.

### Atlas Workflow Command
```bash
python3 atlas/core/atlas_workflow.py feature "Implement photo addition from camera and gallery matching Android workflow"
```

### Implementation Requirements
- **Research Phase**: Analyze Android's `PhotoImportViewModel.kt`
- **Story Creation**:
  - Multi-select from photo library
  - Direct camera capture
  - Automatic category assignment
  - Batch import with progress
- **Implementation Focus**:
  - Use PHPickerViewController for photo selection
  - Implement UIImagePickerController for camera
  - Add category selection during import
  - Handle permissions properly

### Key Android Files to Reference
- `android/app/src/main/java/com/smilepile/ui/viewmodels/PhotoImportViewModel.kt`
- `android/app/src/main/java/com/smilepile/data/repository/PhotoRepositoryImpl.kt`

---

## 6. Photo Editing Features

### Task Description
Implement photo editing capabilities (crop, rotate, category change, delete) matching Android.

### Atlas Workflow Command
```bash
python3 atlas/core/atlas_workflow.py feature "Add photo editing features (crop, rotate, recategorize, delete) matching Android"
```

### Implementation Requirements
- **Research Phase**: Study Android's photo editing implementation
- **Story Creation**:
  - Crop with aspect ratio presets
  - 90-degree rotation
  - Category reassignment
  - Safe deletion with confirmation
  - Edit on import or from gallery
- **Implementation Focus**:
  - Use Core Image for transformations
  - Implement gesture-based cropping UI
  - Add undo/redo capability
  - Save edits non-destructively

### Key Android Files to Reference
- `android/app/src/main/java/com/smilepile/ui/screens/PhotoEditScreen.kt`
- `android/app/src/main/java/com/smilepile/ui/viewmodels/PhotoEditViewModel.kt`
- `android/app/src/main/java/com/smilepile/utils/ImageProcessor.kt`

---

## 7. Category Management Modals

### Task Description
Align iOS category creation/editing modals with Android design (excluding preview pane).

### Atlas Workflow Command
```bash
python3 atlas/core/atlas_workflow.py feature "Align category management modals with Android design patterns"
```

### Implementation Requirements
- **Research Phase**: Analyze Android's `CategoryManagementScreen.kt`
- **Story Creation**:
  - Modal presentation for create/edit
  - Name and emoji selection
  - Color theme selection
  - Validation and error handling
  - Remove preview pane from both platforms
- **Implementation Focus**:
  - Use SwiftUI sheets for modals
  - Implement emoji picker
  - Add color palette selector
  - Match Android's form layout

### Key Android Files to Reference
- `android/app/src/main/java/com/smilepile/ui/screens/CategoryManagementScreen.kt`
- Remove preview functionality from Android version

---

## Execution Plan

### Priority Order
1. **PIN Authentication** - Foundation for security
2. **Biometric Authentication** - Builds on PIN
3. **Kids Mode** - Core feature requiring auth
4. **Photo Addition** - Essential functionality
5. **Photo Editing** - Enhanced user experience
6. **Import/Export** - Data management
7. **Category Modals** - UI consistency

### Parallel Execution Opportunities
- PIN and Biometric auth can be developed in parallel after PIN UI
- Photo addition and editing can be worked on simultaneously
- Category modals can be updated while other work progresses

### Testing Requirements
- Unit tests for security components (PIN/Biometric)
- Integration tests for photo workflows
- UI tests for Kids Mode transitions
- Performance tests for import/export

### Evidence Collection
- Screenshot comparisons iOS vs Android
- Feature parity checklist
- Security audit results
- Performance benchmarks

---

## Component Structure Guidelines

Following Atlas patterns:
- Keep ViewModels under 200 lines
- Extract business logic to service classes
- Use dependency injection for testability
- Implement proper error handling
- Add comprehensive logging

## Success Metrics
- 100% feature parity with Android
- <2 second response for all operations
- Zero security vulnerabilities
- Consistent UI/UX across platforms