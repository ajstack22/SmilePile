#!/usr/bin/env python3
"""
Atlas iOS Workflow Orchestrator
Complete workflow for iOS development: Research → Story → Plan → Adversarial → Implement → Test → Validate
Adapted for iOS/Swift development with Xcode project structure
"""

import sys
import json
import subprocess
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Optional

class AtlasIOSWorkflow:
    """iOS-specific workflow orchestrator with checkpoint management"""

    def __init__(self, task_type: str, description: str):
        self.task_type = task_type
        self.description = description
        self.project_root = Path(__file__).parent.parent
        self.ios_dir = Path(__file__).parent
        self.story_id = self._generate_story_id()
        self.story_path = self._get_story_path()
        self.current_phase = 0
        self.phases = [
            "RESEARCH",
            "STORY_CREATION",
            "PLANNING",
            "ADVERSARIAL_REVIEW",
            "IMPLEMENTATION",
            "TESTING",
            "VALIDATION"
        ]
        self.ios_specific_checks = self._get_ios_checks()

    def _generate_story_id(self) -> str:
        """Generate appropriate story ID for iOS tasks"""
        if self.task_type == "bug":
            return f"iOS-BUG-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
        else:
            stories_dir = self.ios_dir / 'stories'
            stories_dir.mkdir(parents=True, exist_ok=True)
            story_num = len(list(stories_dir.glob('iOS-*.md'))) + 1
            return f"iOS-{story_num:03d}"

    def _get_story_path(self) -> Path:
        """Get path for iOS story file"""
        subdir = 'bugs' if self.task_type == 'bug' else 'features'
        stories_dir = self.ios_dir / 'stories' / subdir
        stories_dir.mkdir(parents=True, exist_ok=True)
        title = self.description[:50].replace(' ', '-').lower()
        filename = f"{self.story_id}-{title}.md"
        return stories_dir / filename

    def _get_ios_checks(self) -> Dict:
        """Get iOS-specific checks and validations"""
        return {
            "build_commands": [
                "xcodebuild -project SmilePile.xcodeproj -scheme SmilePile -sdk iphonesimulator build",
                "xcodebuild test -project SmilePile.xcodeproj -scheme SmilePileTests -sdk iphonesimulator"
            ],
            "key_directories": [
                "SmilePile/Views",
                "SmilePile/ViewModels",
                "SmilePile/Models",
                "SmilePile/Security",
                "SmilePile/Data"
            ],
            "config_files": [
                "project.yml",
                "Info.plist",
                "SmilePile.xcodeproj/project.pbxproj"
            ],
            "test_patterns": [
                "*Tests.swift",
                "*UITests.swift"
            ]
        }

    def print_workflow(self):
        """Print the complete iOS workflow with checkpoints"""

        print(f"""
╔══════════════════════════════════════════════════════════════════════╗
║                    ATLAS iOS WORKFLOW ORCHESTRATOR                   ║
║                  With Checkpoint Confirmation                        ║
╚══════════════════════════════════════════════════════════════════════╝

📱 PLATFORM: iOS/Swift
📋 TASK TYPE: {self.task_type.upper()}
📝 DESCRIPTION: {self.description}
📁 STORY ID: {self.story_id}
📍 STORY PATH: {self.story_path}

══════════════════════════════════════════════════════════════════════

WORKFLOW INSTRUCTIONS:
1. Complete each phase thoroughly
2. After each phase, ask for confirmation
3. Show your work at each checkpoint
4. Wait for approval before proceeding

══════════════════════════════════════════════════════════════════════

PHASE 1: RESEARCH (iOS Components)
─────────────────────────────────
□ Locate all relevant Swift files
□ Check SwiftUI Views in SmilePile/Views/
□ Review ViewModels in SmilePile/ViewModels/
□ Examine Models and Data structures
□ Check Info.plist for capabilities
□ Review existing Security implementations
□ Identify Core Data models if applicable
□ Document iOS-specific dependencies

Key iOS Directories to Check:
{chr(10).join(f'  • {d}' for d in self.ios_specific_checks["key_directories"])}

When complete, ask: "iOS research phase complete. I found [X Swift files/components].
Ready to review my findings?"

══════════════════════════════════════════════════════════════════════

PHASE 2: STORY CREATION (iOS Feature)
─────────────────────────────────────
□ Create story file: {self.story_path}
□ Write problem/feature statement
□ Define iOS-specific acceptance criteria
□ List SwiftUI/UIKit requirements
□ Document iOS version compatibility
□ Note any device-specific considerations
□ Define success metrics for iOS

Story Template:
```markdown
# {self.story_id}: {self.description}

## Platform Requirements
- iOS Version: 15.0+
- Device Types: iPhone/iPad
- Frameworks: SwiftUI, Combine, Core Data

## Acceptance Criteria
- [ ] Feature works on all supported iOS versions
- [ ] UI follows iOS Human Interface Guidelines
- [ ] Accessibility support implemented
- [ ] Dark mode support verified

## Implementation Notes
- SwiftUI components to create/modify
- ViewModels to update
- Data models affected
```

When complete, ask: "iOS story created with [X acceptance criteria].
Ready for me to show you the story?"

══════════════════════════════════════════════════════════════════════

PHASE 3: PLANNING (iOS Implementation)
──────────────────────────────────────
□ List all Swift files to modify
□ Identify SwiftUI views to create/update
□ Plan ViewModel changes
□ Define @Published properties needed
□ Check for necessary iOS permissions
□ Plan Core Data changes if needed
□ Consider navigation flow changes
□ Estimate Xcode project updates

Implementation Checklist:
- Views: Which SwiftUI views need changes?
- ViewModels: What state management is needed?
- Models: Any new data structures?
- Navigation: Changes to app flow?
- Assets: New images or resources?
- Localization: String updates needed?

When complete, ask: "iOS implementation plan ready with [X file changes].
Ready to review the plan?"

══════════════════════════════════════════════════════════════════════

PHASE 4: ADVERSARIAL REVIEW (iOS Edge Cases)
────────────────────────────────────────────
□ Check iOS version compatibility issues
□ Review memory management concerns
□ Consider device rotation handling
□ Check for keyboard avoidance issues
□ Review navigation edge cases
□ Test with Dynamic Type (accessibility)
□ Consider offline/online transitions
□ Check for race conditions in Combine
□ Review state restoration scenarios
□ Consider iPad vs iPhone differences

iOS-Specific Concerns:
- App lifecycle handling
- Background mode behavior
- Push notification interactions
- Deep linking scenarios
- Widget update triggers
- App extension impacts

When complete, ask: "iOS adversarial review found [X potential issues].
Ready to see what I found?"

══════════════════════════════════════════════════════════════════════

PHASE 5: IMPLEMENTATION (iOS Development)
─────────────────────────────────────────
□ Follow the approved iOS plan
□ Update SwiftUI views systematically
□ Implement ViewModels with @Published
□ Add proper error handling
□ Update Info.plist if needed
□ Add necessary iOS permissions
□ Update localization files
□ Test in iOS Simulator

Implementation Order:
1. Data models first
2. ViewModels next
3. SwiftUI views last
4. Navigation updates
5. Asset additions

After each file, ask: "Updated [filename.swift]. [X of Y files complete].
Continue with next file?"

══════════════════════════════════════════════════════════════════════

PHASE 6: TESTING (iOS Verification)
───────────────────────────────────
□ Run Xcode build: xcodebuild build
□ Execute unit tests: xcodebuild test
□ Test on iOS Simulator (iPhone & iPad)
□ Verify different iOS versions
□ Check landscape/portrait modes
□ Test with Dynamic Type enabled
□ Verify Dark/Light mode switching
□ Test memory usage in Instruments
□ Check accessibility with VoiceOver

Test Commands:
```bash
# Build for simulator
xcodebuild -project SmilePile.xcodeproj -scheme SmilePile -sdk iphonesimulator build

# Run tests
xcodebuild test -project SmilePile.xcodeproj -scheme SmilePileTests -sdk iphonesimulator

# Deploy to simulator
xcrun simctl install booted [app_path]
```

When complete, ask: "iOS testing complete. [X tests passed].
Ready to see test results?"

══════════════════════════════════════════════════════════════════════

PHASE 7: VALIDATION (iOS Acceptance)
────────────────────────────────────
□ Verify all iOS acceptance criteria met
□ Confirm UI follows HIG guidelines
□ Check accessibility compliance
□ Validate on multiple device sizes
□ Update story with iOS evidence
□ Document Swift/SwiftUI changes
□ Create screenshots for reference
□ Mark story as COMPLETE

Final Checks:
- App runs without crashes
- No memory leaks detected
- Performance acceptable
- UI responsive on all devices
- Follows iOS best practices

When complete, ask: "All iOS requirements validated.
Ready to close this story?"

══════════════════════════════════════════════════════════════════════

⚠️ iOS-SPECIFIC RULES:
- DO use SwiftUI over UIKit when possible
- DO follow iOS Human Interface Guidelines
- DO test on both iPhone and iPad
- DO check memory management
- DO verify iOS version compatibility
- DO NOT skip Xcode build verification
- DO NOT ignore accessibility
- DO show simulator screenshots when relevant

Start with PHASE 1: RESEARCH now.
After completing research, ask for confirmation before proceeding.
""")

    def execute_build(self) -> bool:
        """Execute iOS build to verify compilation"""
        try:
            for cmd in self.ios_specific_checks["build_commands"]:
                print(f"Executing: {cmd}")
                result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
                if result.returncode != 0:
                    print(f"Build failed: {result.stderr}")
                    return False
            return True
        except Exception as e:
            print(f"Build error: {e}")
            return False

    def check_ios_structure(self) -> Dict:
        """Verify iOS project structure"""
        structure_report = {
            "directories": {},
            "config_files": {},
            "missing": []
        }

        for dir_path in self.ios_specific_checks["key_directories"]:
            full_path = self.ios_dir / dir_path
            if full_path.exists():
                structure_report["directories"][dir_path] = len(list(full_path.glob("*.swift")))
            else:
                structure_report["missing"].append(dir_path)

        for config_file in self.ios_specific_checks["config_files"]:
            full_path = self.ios_dir / config_file
            structure_report["config_files"][config_file] = full_path.exists()

        return structure_report

def main():
    if len(sys.argv) < 3:
        print("""
Atlas iOS Workflow Orchestrator

Usage:
  python3 ios/atlas_ios_workflow.py bug "description of bug"
  python3 ios/atlas_ios_workflow.py feature "description of feature"

Examples:
  python3 ios/atlas_ios_workflow.py bug "App crashes when importing photos"
  python3 ios/atlas_ios_workflow.py feature "Add photo editing capabilities"
        """)
        sys.exit(1)

    task_type = sys.argv[1].lower()
    if task_type not in ['bug', 'feature']:
        print(f"Error: task type must be 'bug' or 'feature', not '{task_type}'")
        sys.exit(1)

    description = ' '.join(sys.argv[2:])

    workflow = AtlasIOSWorkflow(task_type, description)
    workflow.print_workflow()

    # Optional: Check iOS project structure
    if '--check-structure' in sys.argv:
        print("\n" + "="*70)
        print("iOS PROJECT STRUCTURE CHECK")
        print("="*70)
        report = workflow.check_ios_structure()
        print(f"\nDirectories found:")
        for dir_name, swift_count in report["directories"].items():
            print(f"  ✅ {dir_name}: {swift_count} Swift files")

        print(f"\nConfiguration files:")
        for file_name, exists in report["config_files"].items():
            status = "✅" if exists else "❌"
            print(f"  {status} {file_name}")

        if report["missing"]:
            print(f"\n⚠️ Missing directories:")
            for missing in report["missing"]:
                print(f"  ❌ {missing}")

if __name__ == '__main__':
    main()