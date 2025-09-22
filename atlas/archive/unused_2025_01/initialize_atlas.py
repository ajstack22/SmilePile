#!/usr/bin/env python3
"""
Atlas Framework Initialization Script
Ensures maximum likelihood of LLM using Atlas processes by:
1. Setting up CLAUDE.md with strong enforcement
2. Creating .cursorrules for Cursor IDE
3. Adding .github/copilot-instructions.md for GitHub Copilot
4. Setting up git hooks for process enforcement
5. Creating multiple touchpoints that remind/force Atlas usage
"""

import os
import sys
import json
import shutil
import subprocess
from pathlib import Path
from datetime import datetime

class AtlasInitializer:
    def __init__(self, project_path=None):
        self.atlas_source = Path(__file__).parent
        self.project_path = Path(project_path) if project_path else Path.cwd()
        self.atlas_target = self.project_path / 'atlas'
        self.backup_dir = self.project_path / '.atlas_backups'
        self.timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')

        # Track initialization steps
        self.steps_completed = []
        self.warnings = []

    def initialize(self):
        """Run complete Atlas initialization"""
        print("\n" + "="*60)
        print("🚀 ATLAS FRAMEWORK INITIALIZATION")
        print("="*60)
        print(f"Project: {self.project_path}")
        print(f"Atlas Source: {self.atlas_source}\n")

        # Run all initialization steps
        steps = [
            ("Copying Atlas Framework", self.copy_atlas_framework),
            ("Setting up CLAUDE.md", self.setup_claude_md),
            ("Creating .cursorrules", self.create_cursor_rules),
            ("Setting up GitHub Copilot instructions", self.setup_copilot_instructions),
            ("Creating .clinerules", self.create_cline_rules),
            ("Setting up VS Code settings", self.setup_vscode_settings),
            ("Creating git hooks", self.setup_git_hooks),
            ("Adding README badges", self.add_readme_badges),
            ("Creating Atlas shortcuts", self.create_shortcuts),
            ("Setting up AI instructions", self.create_ai_instructions),
            ("Validating installation", self.validate_installation),
        ]

        for description, step_func in steps:
            print(f"\n► {description}...")
            try:
                step_func()
                self.steps_completed.append(description)
                print(f"  ✅ {description} completed")
            except Exception as e:
                self.warnings.append(f"{description}: {str(e)}")
                print(f"  ⚠️  {description} failed: {e}")

        # Final report
        self.print_final_report()

    def copy_atlas_framework(self):
        """Copy Atlas framework to project"""
        if self.atlas_target.exists():
            # Backup existing
            backup_path = self.backup_dir / f"atlas_{self.timestamp}"
            backup_path.parent.mkdir(exist_ok=True)
            shutil.move(str(self.atlas_target), str(backup_path))
            print(f"  Backed up existing Atlas to {backup_path}")

        # Copy Atlas
        shutil.copytree(self.atlas_source, self.atlas_target)

        # Create stories directories
        (self.atlas_target / '09_STORIES' / 'features').mkdir(parents=True, exist_ok=True)
        (self.atlas_target / '09_STORIES' / 'bugs').mkdir(parents=True, exist_ok=True)
        (self.atlas_target / '09_STORIES' / 'epics').mkdir(parents=True, exist_ok=True)

    def setup_claude_md(self):
        """Setup CLAUDE.md with strong Atlas enforcement"""
        claude_md_path = self.project_path / 'CLAUDE.md'

        # Enhanced preamble with stronger enforcement
        preamble = '''# ⚠️ MANDATORY: ATLAS FRAMEWORK ACTIVE - DO NOT BYPASS ⚠️

## 🛑 STOP: READ THIS FIRST - ATLAS IS REQUIRED

This project **REQUIRES** the Atlas Framework for ALL development tasks.
**Attempting to bypass Atlas will result in poor quality and rejected work.**

### ❌ NEVER DO THIS:
- Write code without a story
- Fix bugs without creating bug stories
- Skip the Atlas workflow
- Read files manually when Atlas can inject context
- Create your own process when Atlas has one

### ✅ ALWAYS DO THIS:
1. Check for existing stories: `ls atlas/09_STORIES/`
2. Create stories for ALL work
3. Use Atlas prompts from `atlas/01_CORE/PROMPTS.md`
4. Let context injection work automatically
5. Follow the workflow for your task type

## ATLAS ENFORCEMENT RULES

### Rule 1: Story-First Development
```python
# WRONG - Never do this:
"Let me fix that bug directly..."

# RIGHT - Always do this:
"First, I'll create a bug story for this issue..."
```

### Rule 2: Use Atlas Task Types
```python
# These are your ONLY options for agents:
Task("...", "...", "story-writer")      # For requirements
Task("...", "...", "bug-fixer")         # For bugs
Task("...", "...", "ui-developer")      # For UI
Task("...", "...", "backend-developer") # For backend
Task("...", "...", "tester")            # For tests
Task("...", "...", "general-purpose")   # Auto-detects
```

### Rule 3: Atlas Commands Are Primary
```bash
# First command in ANY session:
python3 atlas/07_AUTOMATION/task_context_integration.py validate

# Before ANY development:
ls atlas/09_STORIES/features/  # Check existing stories
```

## WHY ATLAS IS MANDATORY

1. **Context Injection**: You get the right documentation automatically
2. **Process Compliance**: Following proven workflows ensures quality
3. **Consistency**: Every feature follows the same high standard
4. **Velocity**: No time wasted searching for information
5. **Quality Gates**: Built-in review and validation

## QUICK START COMMANDS

### For Every Session:
```bash
# 1. Validate Atlas is working
python3 atlas/test_context_injection.py

# 2. Check for existing work
ls atlas/09_STORIES/features/
ls atlas/09_STORIES/bugs/

# 3. Use Atlas prompts
cat atlas/01_CORE/PROMPTS.md
```

### For New Work:
```bash
# Always start with a story
"Create a story for [describe the work needed]"

# Then implement
"Implement story atlas/09_STORIES/features/[STORY-ID].md"
```

## ATLAS DIRECTORY STRUCTURE
```
./atlas/                    # ← Framework root (DO NOT MODIFY)
├── 01_CORE/               # Standards and prompts
├── 02_WORKFLOWS/          # Required workflows
├── 03_AGENTS/             # Agent specifications
├── 07_AUTOMATION/         # Context injection system
├── 09_STORIES/            # ALL work starts here
└── config/                # System configuration
```

## CONTEXT PROFILES (AUTO-SELECTED)

The system automatically selects the right context:
- `story_creation` → Requirements and stories
- `troubleshooting` → Bug fixing
- `ui_development` → Frontend work
- `backend_development` → API/service work
- `testing` → Test creation
- `code_review` → Quality assessment
- `deployment` → Release processes

## ENFORCEMENT NOTICES

⚠️ **Git Hooks Active**: Commits without stories may be rejected
⚠️ **Review Required**: All code must pass Atlas review standards
⚠️ **Documentation Mandatory**: Stories must be complete before coding
⚠️ **Process Tracking**: All work is tracked through Atlas

## YOUR FIRST TASK

If this is your first time in this project:
1. Run: `python3 atlas/test_context_injection.py`
2. Review: `cat atlas/01_CORE/PROMPTS.md`
3. Check: `ls atlas/09_STORIES/`
4. Start: "Create a story for [your first task]"

---

## Project-Specific Information

'''

        # Backup existing CLAUDE.md if it exists
        if claude_md_path.exists():
            backup_path = self.backup_dir / f"CLAUDE.md_{self.timestamp}"
            backup_path.parent.mkdir(exist_ok=True)

            # Read existing content
            with open(claude_md_path, 'r') as f:
                existing_content = f.read()

            # Save backup
            with open(backup_path, 'w') as f:
                f.write(existing_content)

            # Skip Atlas section if it exists
            if '# ⚠️ MANDATORY: ATLAS FRAMEWORK ACTIVE' not in existing_content:
                # Prepend Atlas section
                with open(claude_md_path, 'w') as f:
                    f.write(preamble)
                    f.write(existing_content)
            else:
                print("  Atlas section already in CLAUDE.md")
        else:
            # Create new CLAUDE.md
            with open(claude_md_path, 'w') as f:
                f.write(preamble)
                f.write("\n[Add your project-specific information here]\n")

    def create_cursor_rules(self):
        """Create .cursorrules file for Cursor IDE"""
        cursor_rules = '''# Atlas Framework Enforcement for Cursor

You are working in an Atlas Framework project. You MUST follow these rules:

## MANDATORY REQUIREMENTS

1. **NEVER write code without a story**
   - Check atlas/09_STORIES/ for existing stories
   - Create new stories before implementing

2. **ALWAYS use Atlas workflows**
   - Story creation → Development → Review → Deploy
   - No shortcuts or custom processes

3. **USE Atlas prompts**
   - Reference atlas/01_CORE/PROMPTS.md
   - Copy and adapt proven patterns

4. **SPAWN agents with proper types**
   - story-writer, bug-fixer, ui-developer, backend-developer, tester
   - Let context injection work automatically

5. **FOLLOW the established patterns**
   - Check existing code for patterns
   - Maintain consistency

## WORKSPACE STRUCTURE

```
atlas/
├── 01_CORE/PROMPTS.md      # ← Use these prompts
├── 02_WORKFLOWS/           # ← Follow these workflows
├── 09_STORIES/            # ← All work starts here
└── config/                # ← Don't modify
```

## BEFORE STARTING ANY TASK

1. Run: `python3 atlas/test_context_injection.py`
2. Check: `ls atlas/09_STORIES/`
3. Create story if needed
4. Follow Atlas workflow

## ENFORCEMENT

- Git hooks will check for story references
- Reviews require Atlas compliance
- Non-compliant work will be rejected

Remember: Atlas makes development faster and more consistent. Trust the process.
'''

        cursor_rules_path = self.project_path / '.cursorrules'
        with open(cursor_rules_path, 'w') as f:
            f.write(cursor_rules)

    def setup_copilot_instructions(self):
        """Setup GitHub Copilot instructions"""
        copilot_dir = self.project_path / '.github'
        copilot_dir.mkdir(exist_ok=True)

        copilot_instructions = '''# GitHub Copilot Instructions - Atlas Framework

## Project Requirements

This project uses the Atlas Framework. All development MUST follow Atlas workflows.

### Required Process

1. **Stories First**: Every change needs a story in atlas/09_STORIES/
2. **Use Atlas Patterns**: Follow patterns in existing code
3. **Context Aware**: Atlas provides context automatically
4. **Quality Standards**: Follow Atlas review standards

### Key Directories

- `atlas/01_CORE/PROMPTS.md` - Development prompts
- `atlas/02_WORKFLOWS/` - Required workflows
- `atlas/09_STORIES/` - Project stories

### Copilot Suggestions Should

- Reference existing stories
- Follow Atlas patterns
- Use established workflows
- Maintain consistency

### Never Suggest

- Skipping story creation
- Bypassing Atlas workflows
- Custom processes
- Direct file reading when context injection exists

## Command Patterns

```bash
# Validate Atlas
python3 atlas/test_context_injection.py

# Check stories
ls atlas/09_STORIES/features/

# Use prompts
cat atlas/01_CORE/PROMPTS.md
```

Atlas Framework ensures quality and consistency. All suggestions should reinforce Atlas usage.
'''

        copilot_path = copilot_dir / 'copilot-instructions.md'
        with open(copilot_path, 'w') as f:
            f.write(copilot_instructions)

    def create_cline_rules(self):
        """Create .clinerules for Cline/Claude Dev"""
        cline_rules = '''# Atlas Framework - Cline/Claude Dev Rules

## CRITICAL: This is an Atlas Framework Project

You are Claude Dev/Cline working in an Atlas project. You MUST:

### 1. ALWAYS Check for Stories First
Before any development:
```bash
ls atlas/09_STORIES/features/
ls atlas/09_STORIES/bugs/
```

### 2. NEVER Skip Story Creation
If no story exists for the task, create one first:
"Create a story for [task description]"

### 3. USE Atlas Prompts
Reference `atlas/01_CORE/PROMPTS.md` for all standard operations

### 4. FOLLOW Atlas Workflows
- Research → Story → Development → Review → Deploy
- No shortcuts allowed

### 5. Let Context Injection Work
When spawning agents, use proper types:
- story-writer
- bug-fixer
- ui-developer
- backend-developer
- tester

## Project Structure

```
atlas/              # Framework (don't modify)
├── 01_CORE/       # Standards & prompts
├── 02_WORKFLOWS/  # Required processes
├── 09_STORIES/    # All work starts here
└── config/        # System config
```

## Validation Commands

```bash
# Start of session
python3 atlas/test_context_injection.py

# Check stories
ls atlas/09_STORIES/

# View prompts
cat atlas/01_CORE/PROMPTS.md
```

## Remember

- Stories come first
- Atlas workflows are mandatory
- Context injection is automatic
- Quality standards are enforced

Trust the Atlas process - it's here to help.
'''

        cline_rules_path = self.project_path / '.clinerules'
        with open(cline_rules_path, 'w') as f:
            f.write(cline_rules)

    def setup_vscode_settings(self):
        """Add Atlas settings to VS Code"""
        vscode_dir = self.project_path / '.vscode'
        vscode_dir.mkdir(exist_ok=True)

        settings_path = vscode_dir / 'settings.json'

        atlas_settings = {
            "files.exclude": {
                "**/.atlas_backups": True,
                "**/atlas/.atlas": True
            },
            "search.exclude": {
                "**/atlas/07_AUTOMATION/__pycache__": True,
                "**/atlas/.atlas/context_cache": True
            },
            "terminal.integrated.env.osx": {
                "ATLAS_PROJECT": "true"
            },
            "terminal.integrated.env.linux": {
                "ATLAS_PROJECT": "true"
            },
            "terminal.integrated.env.windows": {
                "ATLAS_PROJECT": "true"
            },
            "workbench.startupEditor": "readme",
            "files.associations": {
                "*.md": "markdown",
                "CLAUDE.md": "markdown"
            }
        }

        if settings_path.exists():
            with open(settings_path, 'r') as f:
                existing = json.load(f)
            # Merge settings
            for key, value in atlas_settings.items():
                if key not in existing:
                    existing[key] = value
                elif isinstance(value, dict) and isinstance(existing[key], dict):
                    existing[key].update(value)
            settings = existing
        else:
            settings = atlas_settings

        with open(settings_path, 'w') as f:
            json.dump(settings, f, indent=2)

    def setup_git_hooks(self):
        """Setup git hooks to enforce Atlas usage"""
        git_dir = self.project_path / '.git'
        if not git_dir.exists():
            self.warnings.append("Not a git repository - skipping hooks")
            return

        hooks_dir = git_dir / 'hooks'
        hooks_dir.mkdir(exist_ok=True)

        # Pre-commit hook
        pre_commit = '''#!/bin/bash
# Atlas Framework Pre-commit Hook

echo "🔍 Atlas Framework: Checking commit compliance..."

# Check if commit message references a story
commit_msg=$(git log -1 --pretty=%B)

# Story ID patterns
if [[ ! "$commit_msg" =~ (ATLAS-[0-9]+|BUG-[0-9]+|EPIC-[0-9]+|WIP|DRAFT) ]]; then
    echo "⚠️  WARNING: Commit doesn't reference an Atlas story"
    echo "   Stories should be referenced as: ATLAS-XXX, BUG-XXX, or EPIC-XXX"
    echo "   Use 'WIP:' prefix for work in progress"
    echo ""
    read -p "   Continue without story reference? (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Commit cancelled. Please create a story first:"
        echo "   Run: 'Create a story for [your changes]'"
        exit 1
    fi
fi

echo "✅ Atlas Framework: Commit check passed"
'''

        pre_commit_path = hooks_dir / 'pre-commit'
        with open(pre_commit_path, 'w') as f:
            f.write(pre_commit)
        os.chmod(pre_commit_path, 0o755)

        # Prepare-commit-msg hook
        prepare_msg = '''#!/bin/bash
# Atlas Framework Commit Message Enhancer

commit_msg_file=$1
commit_source=$2

# Add Atlas hint to commit message template
if [ -z "$commit_source" ]; then
    echo "" >> "$commit_msg_file"
    echo "# Atlas Framework Reminder:" >> "$commit_msg_file"
    echo "# - Reference story: ATLAS-XXX, BUG-XXX, or EPIC-XXX" >> "$commit_msg_file"
    echo "# - Check stories: ls atlas/09_STORIES/" >> "$commit_msg_file"
    echo "# - Create story first if needed" >> "$commit_msg_file"
fi
'''

        prepare_msg_path = hooks_dir / 'prepare-commit-msg'
        with open(prepare_msg_path, 'w') as f:
            f.write(prepare_msg)
        os.chmod(prepare_msg_path, 0o755)

    def add_readme_badges(self):
        """Add Atlas badges to README"""
        readme_path = self.project_path / 'README.md'

        if not readme_path.exists():
            # Create README if it doesn't exist
            with open(readme_path, 'w') as f:
                f.write(f"# {self.project_path.name}\n\n")

        with open(readme_path, 'r') as f:
            content = f.read()

        # Check if badges already exist
        if 'Atlas Framework' not in content:
            badges = '''
<!-- Atlas Framework Badges -->
![Atlas Framework](https://img.shields.io/badge/Atlas_Framework-v2.2-blue?style=for-the-badge)
![Context Injection](https://img.shields.io/badge/Context_Injection-Active-green?style=for-the-badge)
![Story Driven](https://img.shields.io/badge/Story_Driven-Development-orange?style=for-the-badge)

'''

            # Add after title or at beginning
            lines = content.split('\n')
            if lines and lines[0].startswith('#'):
                # Insert after title
                lines.insert(1, badges)
            else:
                # Insert at beginning
                lines.insert(0, badges)

            with open(readme_path, 'w') as f:
                f.write('\n'.join(lines))

    def create_shortcuts(self):
        """Create shell shortcuts for common Atlas commands"""
        shortcuts = '''#!/bin/bash
# Atlas Framework Shortcuts

# Get the atlas directory (relative to where shortcuts are sourced)
ATLAS_DIR="$(dirname "${BASH_SOURCE[0]}")/atlas"

# Validate Atlas installation
alias atlas-validate="cd '$ATLAS_DIR' && python3 test_context_injection.py && cd - > /dev/null"

# Check stories
alias atlas-stories="ls -la '$ATLAS_DIR/09_STORIES/features/' '$ATLAS_DIR/09_STORIES/bugs/' 2>/dev/null || echo 'No stories yet'"

# Show Atlas prompts
alias atlas-prompts="cat '$ATLAS_DIR/01_CORE/PROMPTS.md'"

# Test context injection
alias atlas-test="cd '$ATLAS_DIR' && python3 07_AUTOMATION/task_context_integration.py validate && cd - > /dev/null"

# List context profiles
alias atlas-profiles="cd '$ATLAS_DIR' && python3 07_AUTOMATION/enhanced_context_injector.py list && cd - > /dev/null"

# Atlas help
alias atlas-help='echo "Atlas Commands:
  atlas-validate  : Validate Atlas installation
  atlas-stories   : List all stories
  atlas-prompts   : Show development prompts
  atlas-test      : Test context injection
  atlas-profiles  : List available context profiles
  atlas-help      : Show this help"'

echo "🚀 Atlas Framework shortcuts loaded!"
echo "Type 'atlas-help' for available commands"
'''

        # Create shortcuts file
        shortcuts_path = self.project_path / 'atlas_shortcuts.sh'
        with open(shortcuts_path, 'w') as f:
            f.write(shortcuts)
        os.chmod(shortcuts_path, 0o755)

        # Create activation hint
        activation_hint = '''
# Add to your shell profile (.bashrc, .zshrc, etc.):
source ./atlas_shortcuts.sh
'''

        hint_path = self.project_path / 'ATLAS_SHORTCUTS_README.txt'
        with open(hint_path, 'w') as f:
            f.write(activation_hint)

    def create_ai_instructions(self):
        """Create universal AI instructions file"""
        ai_instructions = '''# AI Assistant Instructions - Atlas Framework Project

## FOR ALL AI ASSISTANTS (Claude, GPT, Copilot, Cursor, Cline, etc.)

This project uses the Atlas Framework. You MUST follow these rules:

### ABSOLUTE REQUIREMENTS

1. **Story-First Development**
   - Check `atlas/09_STORIES/` for existing stories
   - Create stories before ANY development
   - Reference story IDs in all work

2. **Use Atlas Workflows**
   ```
   Research → Story Creation → Development → Review → Deploy
   ```
   No exceptions, no shortcuts.

3. **Atlas Prompts Are Primary**
   - Use prompts from `atlas/01_CORE/PROMPTS.md`
   - Don't create custom processes

4. **Context Injection Is Automatic**
   - Don't manually read files
   - Use proper agent types
   - Trust the system

### ENFORCEMENT MECHANISMS

- Git hooks check for story references
- Review standards are enforced
- Non-compliant work is rejected
- Process tracking is active

### KEY COMMANDS

```bash
# Validate Atlas
python3 atlas/test_context_injection.py

# Check stories
ls atlas/09_STORIES/

# View prompts
cat atlas/01_CORE/PROMPTS.md
```

### AGENT TYPES

- `story-writer` - Requirements and stories
- `bug-fixer` - Bug fixes and debugging
- `ui-developer` - Frontend development
- `backend-developer` - Backend/API development
- `tester` - Test creation
- `general-purpose` - Auto-detects from description

### WHY THIS MATTERS

1. **Quality**: Consistent, high-quality output
2. **Velocity**: No wasted time on process
3. **Context**: Always have the right information
4. **Standards**: Meet project requirements
5. **Tracking**: All work is traceable

## REMEMBER

Atlas is not optional. It's the foundation of this project's development process. Trust it, use it, benefit from it.
'''

        ai_path = self.project_path / 'AI_INSTRUCTIONS.md'
        with open(ai_path, 'w') as f:
            f.write(ai_instructions)

    def validate_installation(self):
        """Validate the Atlas installation"""
        # Change to project directory for validation
        original_dir = os.getcwd()
        os.chdir(self.project_path)

        try:
            result = subprocess.run(
                ['python3', 'atlas/test_context_injection.py'],
                capture_output=True,
                text=True,
                timeout=30
            )

            if "ALL TESTS PASSED" in result.stdout:
                print("  All validation tests passed!")
            else:
                self.warnings.append("Some validation tests failed")
        except Exception as e:
            self.warnings.append(f"Validation failed: {e}")
        finally:
            os.chdir(original_dir)

    def print_final_report(self):
        """Print final initialization report"""
        print("\n" + "="*60)
        print("📊 ATLAS INITIALIZATION COMPLETE")
        print("="*60)

        print(f"\n✅ Steps Completed ({len(self.steps_completed)}):")
        for step in self.steps_completed:
            print(f"   • {step}")

        if self.warnings:
            print(f"\n⚠️  Warnings ({len(self.warnings)}):")
            for warning in self.warnings:
                print(f"   • {warning}")

        print("\n📁 Created/Modified Files:")
        files = [
            "CLAUDE.md - LLM instructions with enforcement",
            ".cursorrules - Cursor IDE integration",
            ".github/copilot-instructions.md - GitHub Copilot rules",
            ".clinerules - Cline/Claude Dev rules",
            ".vscode/settings.json - VS Code configuration",
            ".git/hooks/pre-commit - Git enforcement hook",
            "atlas_shortcuts.sh - Shell command shortcuts",
            "AI_INSTRUCTIONS.md - Universal AI rules"
        ]
        for f in files:
            print(f"   • {f}")

        print("\n🚀 NEXT STEPS:")
        print("1. Source the shortcuts: source ./atlas_shortcuts.sh")
        print("2. Validate Atlas: atlas-validate")
        print("3. Review prompts: atlas-prompts")
        print("4. Create first story: 'Create a story for [your next feature]'")

        print("\n💡 QUICK TEST:")
        print("Run this command to verify everything works:")
        print("  python3 atlas/test_context_injection.py")

        print("\n📖 DOCUMENTATION:")
        print("  • Setup Guide: atlas/SETUP_INSTRUCTIONS.md")
        print("  • Prompts: atlas/01_CORE/PROMPTS.md")
        print("  • Workflows: atlas/02_WORKFLOWS/")

        print("\n" + "="*60)
        print("Atlas Framework is now deeply integrated into your project.")
        print("Every AI assistant will be guided to use Atlas workflows.")
        print("="*60 + "\n")


def main():
    """Main entry point"""
    import argparse

    parser = argparse.ArgumentParser(
        description='Initialize Atlas Framework in a project'
    )
    parser.add_argument(
        'project_path',
        nargs='?',
        default='.',
        help='Path to project (default: current directory)'
    )
    parser.add_argument(
        '--force',
        action='store_true',
        help='Force initialization even if Atlas exists'
    )

    args = parser.parse_args()

    # Check if running from Atlas directory
    if Path(args.project_path).resolve() == Path(__file__).parent:
        print("❌ Error: Cannot initialize Atlas in the Atlas source directory.")
        print("   Please run from your project directory or specify a project path:")
        print("   python3 /path/to/atlas/initialize_atlas.py")
        sys.exit(1)

    # Initialize
    initializer = AtlasInitializer(args.project_path)

    try:
        initializer.initialize()
    except KeyboardInterrupt:
        print("\n\n⚠️  Initialization interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Initialization failed: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()