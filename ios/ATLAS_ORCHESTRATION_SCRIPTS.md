# Atlas Orchestration Scripts for iOS-Android Alignment

## Master Orchestration Script

```bash
#!/bin/bash
# ios-alignment-orchestration.sh
# Master script to run all alignment tasks using Atlas workflow

set -e

echo "Starting iOS-Android Feature Alignment"
echo "======================================"

# Create evidence directory
mkdir -p ios/alignment-evidence

# Run each feature through Atlas workflow
features=(
    "PIN Authentication System"
    "Biometric Authentication"
    "Kids Mode Display"
    "Photo Import Flow"
    "Photo Editing Features"
    "Import Export Functionality"
    "Category Management Modals"
)

for feature in "${features[@]}"; do
    echo ""
    echo "Processing: $feature"
    echo "-------------------"

    # Generate sanitized feature name for files
    feature_file=$(echo "$feature" | tr '[:upper:]' '[:lower:]' | tr ' ' '-')

    # Run Atlas workflow
    python3 atlas/core/atlas_workflow.py feature \
        "iOS alignment: $feature matching Android implementation" \
        --output "ios/alignment-evidence/${feature_file}"

    # Checkpoint after each feature
    python3 atlas/core/atlas_checkpoint.py \
        --phase "implementation" \
        --feature "$feature" \
        --evidence "ios/alignment-evidence/${feature_file}"
done

# Generate final report
python3 atlas/core/atlas_workflow.py validate \
    --input "ios/alignment-evidence" \
    --output "ios/alignment-evidence/final-report.md"

echo ""
echo "Alignment Complete!"
echo "Evidence available in: ios/alignment-evidence/"
```

---

## Individual Feature Scripts

### 1. PIN Authentication Script

```bash
#!/bin/bash
# wave-pin-authentication.sh

echo "Wave: PIN Authentication Implementation"
echo "======================================="

# Research Phase
python3 atlas/core/atlas_research.py \
    --android-source "android/app/src/main/java/com/smilepile/security" \
    --ios-target "ios/SmilePile/Security" \
    --output "ios/alignment-evidence/pin-research.md"

# Story Creation
python3 atlas/core/atlas_story.py \
    --title "PIN Authentication for iOS" \
    --acceptance-criteria "
        - 4-digit PIN entry
        - Keychain storage
        - Failed attempt tracking
        - Lockout after 5 attempts
    " \
    --output "ios/alignment-evidence/pin-story.md"

# Implementation with adversarial review
python3 atlas/core/atlas_adversarial.py \
    --story "ios/alignment-evidence/pin-story.md" \
    --review-focus "security,usability"

# Generate implementation
echo "Implementing PIN components..."
cat > ios/SmilePile/Security/PINManager.swift << 'EOF'
// Implementation based on Android SecurePreferencesManager
import Foundation
import Security

class PINManager {
    // Implementation details from workflow
}
EOF

# Validation
python3 atlas/core/atlas_checkpoint.py \
    --validate "pin-authentication" \
    --tests "ios/SmilePileTests/PINManagerTests.swift"
```

### 2. Kids Mode Script

```bash
#!/bin/bash
# wave-kids-mode.sh

echo "Wave: Kids Mode Implementation"
echo "=============================="

# Research Android implementation
echo "Researching Android Kids Mode..."
python3 atlas/core/atlas_research.py \
    --analyze "android/app/src/main/java/com/smilepile/ui/screens/KidsModeGalleryScreen.kt" \
    --extract "features,ui-patterns,navigation" \
    --output "ios/alignment-evidence/kids-mode-research.md"

# Create implementation story
python3 atlas/core/atlas_story.py \
    --epic "Kids Mode Feature Parity" \
    --tasks "
        1. Fullscreen photo viewer
        2. Swipe navigation
        3. Category filtering
        4. Exit authentication
    " \
    --output "ios/alignment-evidence/kids-mode-story.md"

# Parallel implementation
python3 atlas/core/atlas_workflow.py parallel \
    --tasks "viewer,navigation,filtering,auth" \
    --output "ios/SmilePile/KidsMode/"

# Integration testing
echo "Running Kids Mode integration tests..."
xcodebuild test \
    -scheme SmilePile \
    -testPlan KidsModeTests
```

### 3. Photo Editing Script

```bash
#!/bin/bash
# wave-photo-editing.sh

echo "Wave: Photo Editing Features"
echo "============================"

# Analyze Android photo editing
python3 atlas/core/atlas_research.py \
    --components "
        android/app/src/main/java/com/smilepile/ui/screens/PhotoEditScreen.kt
        android/app/src/main/java/com/smilepile/utils/ImageProcessor.kt
    " \
    --output "ios/alignment-evidence/photo-editing-research.md"

# Design iOS implementation
python3 atlas/core/atlas_story.py \
    --feature "Photo Editing Capabilities" \
    --requirements "
        - Crop with presets
        - Rotate 90 degrees
        - Category change
        - Delete with confirmation
    " \
    --architecture "MVVM" \
    --output "ios/alignment-evidence/photo-editing-story.md"

# Generate components
echo "Generating photo editing components..."

# Create view model
cat > ios/SmilePile/ViewModels/PhotoEditViewModel.swift << 'EOF'
// Photo editing view model matching Android
import SwiftUI
import CoreImage

class PhotoEditViewModel: ObservableObject {
    // Implementation from workflow
}
EOF

# Create edit view
cat > ios/SmilePile/Views/PhotoEditView.swift << 'EOF'
// Photo editing UI
import SwiftUI

struct PhotoEditView: View {
    // UI implementation
}
EOF

# Run validation
python3 atlas/core/atlas_checkpoint.py \
    --feature "photo-editing" \
    --validate-against "android/app/src/main/java/com/smilepile/ui/screens/PhotoEditScreen.kt"
```

---

## Parallel Execution Strategy

```bash
#!/bin/bash
# parallel-alignment.sh
# Run multiple alignment tasks in parallel

echo "Starting Parallel iOS Alignment"
echo "==============================="

# Define parallel task groups
declare -a auth_tasks=(
    "python3 atlas/core/atlas_workflow.py feature 'PIN authentication'"
    "python3 atlas/core/atlas_workflow.py feature 'Biometric authentication'"
)

declare -a photo_tasks=(
    "python3 atlas/core/atlas_workflow.py feature 'Photo import flow'"
    "python3 atlas/core/atlas_workflow.py feature 'Photo editing features'"
)

declare -a ui_tasks=(
    "python3 atlas/core/atlas_workflow.py feature 'Kids Mode display'"
    "python3 atlas/core/atlas_workflow.py feature 'Category management modals'"
)

# Run auth tasks in parallel
echo "Running authentication tasks..."
for task in "${auth_tasks[@]}"; do
    eval "$task &"
done
wait

# Run photo tasks in parallel
echo "Running photo management tasks..."
for task in "${photo_tasks[@]}"; do
    eval "$task &"
done
wait

# Run UI tasks in parallel
echo "Running UI alignment tasks..."
for task in "${ui_tasks[@]}"; do
    eval "$task &"
done
wait

# Final integration
python3 atlas/core/atlas_workflow.py integrate \
    --components "auth,photos,ui" \
    --output "ios/alignment-evidence/integration-report.md"

echo "Parallel alignment complete!"
```

---

## Validation Script

```bash
#!/bin/bash
# validate-alignment.sh
# Comprehensive validation of iOS-Android feature parity

echo "Validating iOS-Android Alignment"
echo "================================"

# Feature parity checks
features_to_validate=(
    "PIN:authentication:4-digit-entry"
    "Biometric:face-id:touch-id"
    "KidsMode:fullscreen:swipe-nav"
    "Photos:import:camera:gallery"
    "Edit:crop:rotate:delete"
    "Export:zip:encryption"
    "Category:create:edit:delete"
)

for feature in "${features_to_validate[@]}"; do
    IFS=':' read -ra PARTS <<< "$feature"
    name="${PARTS[0]}"

    echo "Validating $name..."

    # Run feature comparison
    python3 atlas/core/atlas_adversarial.py \
        --compare-ios "ios/SmilePile/${name}" \
        --compare-android "android/app/src/main/java/com/smilepile" \
        --output "ios/alignment-evidence/${name}-validation.md"
done

# UI consistency check
echo "Checking UI consistency..."
python3 scripts/ui_comparison.py \
    --ios-screens "ios/SmilePile/Views" \
    --android-screens "android/app/src/main/java/com/smilepile/ui/screens" \
    --output "ios/alignment-evidence/ui-consistency.md"

# Generate final report
python3 scripts/generate_alignment_report.py \
    --evidence "ios/alignment-evidence" \
    --output "iOS_ANDROID_ALIGNMENT_REPORT.md"

echo "Validation complete!"
```

---

## Evidence Collection Template

```bash
#!/bin/bash
# collect-evidence.sh
# Gather evidence for each implementation phase

feature_name="$1"
phase="$2"

mkdir -p "ios/alignment-evidence/${feature_name}/${phase}"

# Collect phase-specific evidence
case $phase in
    "research")
        # Capture research findings
        cp *research*.md "ios/alignment-evidence/${feature_name}/${phase}/"
        ;;
    "implementation")
        # Capture code changes
        git diff --name-only > "ios/alignment-evidence/${feature_name}/${phase}/changed-files.txt"
        git diff > "ios/alignment-evidence/${feature_name}/${phase}/implementation.diff"
        ;;
    "testing")
        # Capture test results
        xcodebuild test -scheme SmilePile \
            -resultBundlePath "ios/alignment-evidence/${feature_name}/${phase}/test-results.xcresult"
        ;;
    "validation")
        # Screenshot comparisons
        xcrun simctl io booted screenshot \
            "ios/alignment-evidence/${feature_name}/${phase}/ios-screenshot.png"
        ;;
esac

# Generate phase report
python3 atlas/core/atlas_checkpoint.py \
    --phase "$phase" \
    --feature "$feature_name" \
    --evidence "ios/alignment-evidence/${feature_name}/${phase}"
```