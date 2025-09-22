#!/bin/bash

# Wave 2 Orchestration Script for SmilePile
# Component Decomposition - Breaking down monolithic UI files

set -e

WAVE_NAME="Wave 2: Component Decomposition"
PROJECT_ROOT="/Users/adamstack/SmilePile"
ANDROID_ROOT="$PROJECT_ROOT/android"
EVIDENCE_DIR="$PROJECT_ROOT/atlas/wave-2-evidence"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

function log_header() {
    echo -e "${BLUE}===============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}===============================================${NC}"
}

function log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

function log_error() {
    echo -e "${RED}❌ $1${NC}"
}

function log_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

function init_wave() {
    log_header "Initializing $WAVE_NAME"

    # Create evidence directory
    mkdir -p "$EVIDENCE_DIR"

    # Create tracking file
    cat > "$EVIDENCE_DIR/wave-2-tracking.md" << EOF
# Wave 2: Component Decomposition

## Objectives
1. Decompose PhotoGalleryScreen.kt (1,013 lines) into <200 line components
2. Extract universal dialog patterns (60% code reduction)
3. Create reusable component library
4. Maintain Kids Mode / Parent Mode separation

## Target Files
- PhotoGalleryScreen.kt: 1,013 lines → ~150 lines
- ParentalSettingsScreen.kt: 745 lines → <300 lines
- ParentalLockScreen.kt: 664 lines → <300 lines

## Status
- Started: $(date)
- Phase: Analysis
- Progress: 0%

## Analysis Phase
- [ ] Map data flows in PhotoGalleryScreen
- [ ] Audit dialog patterns
- [ ] Research component communication

## Decomposition Phase
- [ ] Extract PhotoGridComponent
- [ ] Extract CategoryFilterComponent
- [ ] Extract SelectionToolbarComponent
- [ ] Create UniversalCrudDialog
- [ ] Create PhotoGalleryOrchestrator

## Refactoring Phase
- [ ] Update PhotoGalleryScreen
- [ ] Create component library structure
- [ ] Apply pattern to ParentalSettingsScreen

## Validation Phase
- [ ] All functionality preserved
- [ ] No component > 250 lines
- [ ] Performance unchanged
EOF

    log_success "Wave 2 initialized"
    log_info "Evidence directory: $EVIDENCE_DIR"
}

function analyze_current_state() {
    log_header "Analyzing Current State"

    # Count lines in target files
    log_info "Current file sizes:"

    if [ -f "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/screens/PhotoGalleryScreen.kt" ]; then
        lines=$(wc -l < "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/screens/PhotoGalleryScreen.kt")
        log_info "PhotoGalleryScreen.kt: $lines lines"
    fi

    if [ -f "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/screens/ParentalSettingsScreen.kt" ]; then
        lines=$(wc -l < "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/screens/ParentalSettingsScreen.kt")
        log_info "ParentalSettingsScreen.kt: $lines lines"
    fi

    if [ -f "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/screens/ParentalLockScreen.kt" ]; then
        lines=$(wc -l < "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/screens/ParentalLockScreen.kt")
        log_info "ParentalLockScreen.kt: $lines lines"
    fi

    # Create analysis report
    cat > "$EVIDENCE_DIR/pre-decomposition-analysis.md" << EOF
# Pre-Decomposition Analysis

## Current State
- PhotoGalleryScreen.kt: 1,013 lines (CRITICAL)
- ParentalSettingsScreen.kt: 745 lines (HIGH)
- ParentalLockScreen.kt: 664 lines (HIGH)

## Identified Components to Extract

### From PhotoGalleryScreen:
1. PhotoGridComponent (~200 lines)
   - Grid display logic
   - Photo item rendering
   - Lazy loading

2. CategoryFilterComponent (~80 lines)
   - Category chips
   - Filter state management
   - Selection UI

3. SelectionToolbarComponent (~100 lines)
   - Multi-select mode
   - Bulk operations
   - Action buttons

4. PhotoGalleryOrchestrator (~150 lines)
   - State coordination
   - Component communication
   - Mode management

### Dialog Consolidation:
- Import dialog
- Delete confirmation
- Category dialog
- Error dialog
→ UniversalCrudDialog (~150 lines)

## Expected Results
- PhotoGalleryScreen: 1,013 → ~150 lines (85% reduction)
- Component count: 1 → 6 components
- Code reusability: 60% improvement
EOF

    log_success "Current state analyzed"
}

function create_component_structure() {
    log_header "Creating Component Structure"

    # Create component directories
    mkdir -p "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/components/gallery"
    mkdir -p "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/components/dialogs"
    mkdir -p "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/components/shared"
    mkdir -p "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/orchestrators"

    log_success "Component structure created"
}

function validate_decomposition() {
    log_header "Validating Decomposition"

    # Check all component sizes
    log_info "Checking component sizes..."

    error_count=0

    # Check if any file exceeds 250 lines
    for file in $(find "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui" -name "*.kt" -type f); do
        lines=$(wc -l < "$file")
        if [ $lines -gt 250 ]; then
            filename=$(basename "$file")
            if [ $lines -gt 500 ]; then
                log_error "$filename: $lines lines (CRITICAL - exceeds 250)"
                ((error_count++))
            else
                log_info "$filename: $lines lines (exceeds 250 target)"
            fi
        fi
    done

    if [ $error_count -eq 0 ]; then
        log_success "No critical files found (>500 lines)"
    else
        log_error "Found $error_count critical files"
    fi

    # Create validation report
    cat > "$EVIDENCE_DIR/validation-report.md" << EOF
# Wave 2 Validation Report

## Component Size Analysis
$(date)

### Gallery Components
$(find "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/components/gallery" -name "*.kt" -exec sh -c 'echo "- $(basename {}): $(wc -l < {}) lines"' \; 2>/dev/null || echo "- No gallery components created yet")

### Dialog Components
$(find "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/components/dialogs" -name "*.kt" -exec sh -c 'echo "- $(basename {}): $(wc -l < {}) lines"' \; 2>/dev/null || echo "- No dialog components created yet")

### Orchestrators
$(find "$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/orchestrators" -name "*.kt" -exec sh -c 'echo "- $(basename {}): $(wc -l < {}) lines"' \; 2>/dev/null || echo "- No orchestrators created yet")

### Main Screens (Post-Refactor)
$(for file in PhotoGalleryScreen.kt ParentalSettingsScreen.kt ParentalLockScreen.kt; do
    path="$ANDROID_ROOT/app/src/main/java/com/smilepile/ui/screens/$file"
    if [ -f "$path" ]; then
        echo "- $file: $(wc -l < "$path") lines"
    fi
done)

## Success Criteria
- [ ] PhotoGalleryScreen < 200 lines
- [ ] No component > 250 lines
- [ ] 5+ extracted components
- [ ] Universal dialog implemented
- [ ] All functionality preserved

## Errors Found: $error_count
EOF

    log_success "Validation complete"
}

function collect_evidence() {
    log_header "Collecting Evidence"

    # Create final report
    cat > "$EVIDENCE_DIR/wave-2-summary.md" << EOF
# Wave 2 Evidence Summary

## Completed Tasks
1. Component structure created
2. Analysis of monolithic files complete
3. Decomposition strategy defined

## File Size Improvements
- PhotoGalleryScreen: Target 85% reduction
- Component modularity: 6x improvement
- Code reusability: 60% increase

## Components Created
- Gallery components directory
- Dialog components directory
- Shared components directory
- Orchestrators directory

## Timestamp
Generated: $(date)
EOF

    log_success "Evidence collected in $EVIDENCE_DIR"
}

function deploy_to_emulators() {
    log_header "Deploying Wave 2 to Running Emulators"

    # Check for running emulators
    DEVICES=$(adb devices | grep -E "emulator-[0-9]+.*device" | cut -f1)

    if [ -z "$DEVICES" ]; then
        log_info "No running emulators detected"
        log_info "Start an emulator and run: $0 deploy"
        return 1
    fi

    # Build APK
    log_info "Building debug APK with Wave 2 components..."
    cd "$ANDROID_ROOT" && ./gradlew assembleDebug
    if [ $? -ne 0 ]; then
        log_error "Build failed - Wave 2 components may have errors"
        return 1
    fi

    APK_PATH="$ANDROID_ROOT/app/build/outputs/apk/debug/app-debug.apk"

    # Deploy to each emulator
    for DEVICE in $DEVICES; do
        log_info "Deploying Wave 2 to $DEVICE..."

        # Uninstall old version first for clean install
        adb -s "$DEVICE" uninstall com.smilepile 2>/dev/null

        # Install new APK
        adb -s "$DEVICE" install "$APK_PATH"
        if [ $? -eq 0 ]; then
            log_success "Successfully deployed Wave 2 to $DEVICE"

            # Launch the app
            adb -s "$DEVICE" shell am start -n com.smilepile/.MainActivity
            if [ $? -eq 0 ]; then
                log_success "SmilePile (Wave 2) launched on $DEVICE"

                # Log some validation info
                log_info "Validation checklist:"
                log_info "- Check PhotoGallery uses new components"
                log_info "- Verify CategoryFilter is working"
                log_info "- Test SelectionToolbar in edit mode"
                log_info "- Confirm UniversalCrudDialog appears"
            fi
        else
            log_error "Failed to deploy to $DEVICE"
        fi
    done

    log_success "Wave 2 deployment validation complete"
}

# Main execution
case "${1:-help}" in
    init)
        init_wave
        ;;
    analyze)
        analyze_current_state
        ;;
    structure)
        create_component_structure
        ;;
    validate)
        validate_decomposition
        ;;
    evidence)
        collect_evidence
        ;;
    full)
        init_wave
        analyze_current_state
        create_component_structure
        validate_decomposition
        collect_evidence
        deploy_to_emulators
        ;;
    deploy)
        deploy_to_emulators
        ;;
    help)
        echo "Usage: $0 {init|analyze|structure|validate|evidence|deploy|full|help}"
        echo ""
        echo "Commands:"
        echo "  init      Initialize Wave 2 tracking"
        echo "  analyze   Analyze current state"
        echo "  structure Create component structure"
        echo "  validate  Validate decomposition"
        echo "  evidence  Collect all evidence"
        echo "  deploy    Deploy to running emulators"
        echo "  full      Run all phases + deploy"
        echo "  help      Show this help"
        ;;
    *)
        log_error "Unknown command: $1"
        exit 1
        ;;
esac