#!/bin/bash

# Wave 7: Kids Mode Redesign - Atlas Orchestration Script
# This script orchestrates the parallel execution of Wave 7 tasks

set -e

echo "🚀 Starting Wave 7: Kids Mode Redesign"
echo "======================================"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to run task with status
run_task() {
    local phase=$1
    local task=$2
    local description=$3

    echo -e "${BLUE}[Phase $phase]${NC} $description..."
    eval "$task"
    echo -e "${GREEN}✓${NC} $description completed"
}

# Function to run parallel tasks
run_parallel() {
    local phase=$1
    shift
    echo -e "\n${YELLOW}=== Phase $phase: Starting parallel execution ===${NC}"

    # Run tasks in background
    for task in "$@"; do
        eval "$task" &
    done

    # Wait for all background tasks
    wait
    echo -e "${GREEN}=== Phase $phase: All parallel tasks completed ===${NC}\n"
}

# Phase 1: Mode Management Infrastructure
echo -e "\n${YELLOW}=== Phase 1: Mode Management Infrastructure ===${NC}"

# Create ModeManager
cat > mode_manager_task.sh << 'EOF'
echo "Creating ModeManager..."
mkdir -p app/src/main/java/com/smilepile/mode
cat > app/src/main/java/com/smilepile/mode/ModeManager.kt << 'KOTLIN'
package com.smilepile.mode

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppMode {
    KIDS,
    PARENT
}

@Singleton
class ModeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("app_mode_prefs", Context.MODE_PRIVATE)

    private val _currentMode = MutableStateFlow(loadMode())
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

    private fun loadMode(): AppMode {
        val modeString = prefs.getString("current_mode", AppMode.KIDS.name)
        return AppMode.valueOf(modeString ?: AppMode.KIDS.name)
    }

    fun setMode(mode: AppMode) {
        _currentMode.value = mode
        saveMode(mode)
    }

    fun toggleMode() {
        val newMode = if (_currentMode.value == AppMode.KIDS) {
            AppMode.PARENT
        } else {
            AppMode.KIDS
        }
        setMode(newMode)
    }

    private fun saveMode(mode: AppMode) {
        prefs.edit().putString("current_mode", mode.name).apply()
    }

    fun isKidsMode(): Boolean = _currentMode.value == AppMode.KIDS
    fun isParentMode(): Boolean = _currentMode.value == AppMode.PARENT
}
KOTLIN
echo "✓ ModeManager created"
EOF

# Create AppModeViewModel
cat > mode_viewmodel_task.sh << 'EOF'
echo "Creating AppModeViewModel..."
cat > app/src/main/java/com/smilepile/ui/viewmodels/AppModeViewModel.kt << 'KOTLIN'
package com.smilepile.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smilepile.mode.AppMode
import com.smilepile.mode.ModeManager
import com.smilepile.security.SecurePreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppModeUiState(
    val currentMode: AppMode = AppMode.KIDS,
    val isTransitioning: Boolean = false,
    val requiresPinAuth: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AppModeViewModel @Inject constructor(
    private val modeManager: ModeManager,
    private val securePreferences: SecurePreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppModeUiState())
    val uiState: StateFlow<AppModeUiState> = _uiState.asStateFlow()

    init {
        observeModeChanges()
    }

    private fun observeModeChanges() {
        viewModelScope.launch {
            modeManager.currentMode.collect { mode ->
                _uiState.value = _uiState.value.copy(currentMode = mode)
            }
        }
    }

    fun requestModeToggle() {
        val currentMode = _uiState.value.currentMode

        if (currentMode == AppMode.KIDS && securePreferences.isPINEnabled()) {
            // Require PIN to enter parent mode
            _uiState.value = _uiState.value.copy(requiresPinAuth = true)
        } else {
            // No PIN required or switching to kids mode
            performModeToggle()
        }
    }

    fun validatePinAndToggle(pin: String): Boolean {
        if (securePreferences.validatePIN(pin)) {
            performModeToggle()
            _uiState.value = _uiState.value.copy(requiresPinAuth = false)
            return true
        } else {
            _uiState.value = _uiState.value.copy(
                error = "Incorrect PIN"
            )
            return false
        }
    }

    fun cancelPinAuth() {
        _uiState.value = _uiState.value.copy(
            requiresPinAuth = false,
            error = null
        )
    }

    private fun performModeToggle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTransitioning = true)
            modeManager.toggleMode()
            _uiState.value = _uiState.value.copy(isTransitioning = false)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
KOTLIN
echo "✓ AppModeViewModel created"
EOF

# Run Phase 1 tasks in parallel
chmod +x mode_manager_task.sh mode_viewmodel_task.sh
run_parallel 1 "./mode_manager_task.sh" "./mode_viewmodel_task.sh"

# Phase 2: PIN Relocation
echo -e "\n${YELLOW}=== Phase 2: PIN Relocation ===${NC}"
echo "Moving PIN management to Settings screen..."
echo "✓ PIN relocation planned (will update SettingsScreen.kt)"

# Phase 3: Kids Mode UI
echo -e "\n${YELLOW}=== Phase 3: Kids Mode UI Implementation ===${NC}"
echo "Creating simplified Kids Mode interface..."
echo "✓ Kids Mode UI planned (will create KidsModeGallery.kt)"

# Phase 4: FAB Redesign
echo -e "\n${YELLOW}=== Phase 4: FAB System Redesign ===${NC}"
echo "Implementing dual FAB system..."
echo "✓ FAB redesign planned (will update PhotoGalleryScreen.kt)"

# Phase 5: Navigation Updates
echo -e "\n${YELLOW}=== Phase 5: Navigation Updates ===${NC}"
echo "Updating navigation for mode-aware routing..."
echo "✓ Navigation updates planned (will update AppNavigation.kt)"

# Evidence collection
echo -e "\n${YELLOW}=== Collecting Evidence ===${NC}"
cat > atlas/wave-7-evidence.md << 'EOF'
# Wave 7 Implementation Evidence

## Phase 1: Mode Management ✓
- Created: `ModeManager.kt` - Singleton for app-wide mode state
- Created: `AppModeViewModel.kt` - ViewModel for mode transitions
- Feature: Mode persistence in SharedPreferences
- Feature: PIN authentication integration

## Phase 2: PIN Relocation (Pending)
- Task: Move PIN settings to main Settings screen
- Task: Remove ParentalSettingsScreen
- Task: Simplify to PIN-only (remove pattern)

## Phase 3: Kids Mode UI (Pending)
- Task: Create simplified gallery for kids
- Task: Hide management features
- Task: Implement deletion protection

## Phase 4: FAB Redesign (Pending)
- Task: Primary FAB for mode toggle
- Task: Secondary FAB for photo import (parent mode)
- Task: PIN dialog for mode switching

## Phase 5: Navigation (Pending)
- Task: Mode-based navigation guards
- Task: Update bottom navigation visibility

## Testing Checklist
- [ ] Kids Mode launches by default
- [ ] PIN required for parent mode
- [ ] No delete in kids mode
- [ ] Settings hidden in kids mode
- [ ] Mode persists on restart
EOF

echo -e "${GREEN}✓${NC} Evidence collected: atlas/wave-7-evidence.md"

# Cleanup
rm -f mode_manager_task.sh mode_viewmodel_task.sh

echo -e "\n${GREEN}🎉 Wave 7 Phase 1 Complete!${NC}"
echo "Next steps: Run Phase 2-5 implementation"