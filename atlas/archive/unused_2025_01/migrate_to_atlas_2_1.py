#!/usr/bin/env python3
"""
Atlas 2.1 Migration Script

This script consolidates the Atlas Framework into a single, unified system by:
- Removing duplicate processes and components
- Reorganizing directory structure
- Consolidating workflows into single definitive versions
- Updating all cross-references
- Creating a clean, unified Atlas 2.1 system

Usage: python migrate_to_atlas_2_1.py [--dry-run] [--backup]
"""

import os
import shutil
import json
import re
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Tuple
import argparse


class AtlasMigrator:
    def __init__(self, atlas_root: str, dry_run: bool = False, backup: bool = True):
        self.atlas_root = Path(atlas_root)
        self.dry_run = dry_run
        self.backup = backup
        self.migration_report = {
            "timestamp": datetime.now().isoformat(),
            "actions": [],
            "errors": [],
            "warnings": []
        }

        # Define the new consolidated structure
        self.new_structure = {
            "workflows": "All workflow documentation (was 03_PROCESSES)",
            "automation": "All Python scripts (was 07_AUTOMATION & 07_SCRIPTS_AND_AUTOMATION)",
            "templates": "All templates (was 05_TEMPLATES & 06_TEMPLATES)",
            "metrics": "Metrics and dashboards (was 04_METRICS)",
            "agents": "Agent specifications (was 08_AGENT_TYPES)",
            "standards": "Standards and agreements (was 01_STANDARDS_AND_AGREEMENTS)",
            "roles": "Role definitions (was 02_ROLES)",
            "integrations": "External integrations (was 10_INTEGRATIONS)",
            "_deprecated": "Archive of old components"
        }

        # Files to be retired/consolidated
        self.consolidation_map = {
            "03_PROCESSES/03_ADVERSARIAL_WORKFLOW.md": {
                "action": "merge_into",
                "target": "workflows/ATLAS_WORKFLOW.md",
                "reason": "Merge best practices into unified workflow"
            },
            "03_PROCESSES/04_ENHANCED_WORKFLOW.md": {
                "action": "rename_to",
                "target": "workflows/ATLAS_WORKFLOW.md",
                "reason": "This becomes THE Atlas workflow"
            },
            "03_PROCESSES/05_ENHANCED_REVIEW_PROCESS.md": {
                "action": "rename_to",
                "target": "workflows/REVIEW_PROCESS.md",
                "reason": "Remove 'enhanced' - this is THE review process"
            },
            "03_PROCESSES/08_SMART_REVIEW.md": {
                "action": "merge_into",
                "target": "workflows/REVIEW_PROCESS.md",
                "reason": "Merge smart review into main review process"
            },
            "03_PROCESSES/07_PARALLEL_EXECUTION.md": {
                "action": "merge_into",
                "target": "workflows/ATLAS_WORKFLOW.md",
                "reason": "Parallel execution is default, not optional"
            }
        }

        # Python scripts to consolidate
        self.script_consolidation = {
            "07_AUTOMATION": "automation/core/",
            "07_SCRIPTS_AND_AUTOMATION": "automation/processes/"
        }

    def log_action(self, action_type: str, message: str, details: Dict = None):
        """Log an action to the migration report"""
        entry = {
            "type": action_type,
            "message": message,
            "timestamp": datetime.now().isoformat()
        }
        if details:
            entry["details"] = details

        if action_type == "error":
            self.migration_report["errors"].append(entry)
        elif action_type == "warning":
            self.migration_report["warnings"].append(entry)
        else:
            self.migration_report["actions"].append(entry)

        print(f"[{action_type.upper()}] {message}")

    def create_backup(self):
        """Create backup of current Atlas structure"""
        if not self.backup:
            return

        backup_dir = self.atlas_root.parent / f"atlas_backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        if not self.dry_run:
            try:
                shutil.copytree(self.atlas_root, backup_dir)
                self.log_action("backup", f"Created backup at {backup_dir}")
            except Exception as e:
                self.log_action("error", f"Failed to create backup: {e}")
                return False
        else:
            self.log_action("backup", f"Would create backup at {backup_dir}")

        return True

    def create_new_structure(self):
        """Create the new consolidated directory structure"""
        self.log_action("structure", "Creating new directory structure")

        for dir_name, description in self.new_structure.items():
            new_dir = self.atlas_root / dir_name

            if not self.dry_run:
                new_dir.mkdir(exist_ok=True)
                # Create README in each directory
                readme_content = f"# {dir_name.title()}\n\n{description}\n"
                (new_dir / "README.md").write_text(readme_content)

            self.log_action("create", f"Created directory: {dir_name} - {description}")

    def consolidate_processes(self):
        """Consolidate process documentation"""
        self.log_action("consolidate", "Consolidating process documentation")

        processes_dir = self.atlas_root / "03_PROCESSES"
        workflows_dir = self.atlas_root / "workflows"

        if not processes_dir.exists():
            self.log_action("warning", "03_PROCESSES directory not found")
            return

        # Handle special consolidations first
        for source_file, config in self.consolidation_map.items():
            source_path = self.atlas_root / source_file

            if not source_path.exists():
                self.log_action("warning", f"Source file not found: {source_file}")
                continue

            target_path = self.atlas_root / config["target"]

            if config["action"] == "rename_to":
                self._move_file(source_path, target_path, config["reason"])
            elif config["action"] == "merge_into":
                self._merge_file_content(source_path, target_path, config["reason"])

        # Move remaining process files
        for md_file in processes_dir.glob("*.md"):
            if not any(str(md_file).endswith(Path(cf).name) for cf in self.consolidation_map.keys()):
                target = workflows_dir / md_file.name
                self._move_file(md_file, target, "Standard process migration")

    def consolidate_automation(self):
        """Consolidate automation scripts"""
        self.log_action("consolidate", "Consolidating automation scripts")

        automation_dir = self.atlas_root / "automation"

        # Consolidate from both automation directories
        for old_dir, new_subdir in self.script_consolidation.items():
            old_path = self.atlas_root / old_dir
            new_path = automation_dir / new_subdir.split("/")[0]

            if old_path.exists():
                if not self.dry_run:
                    new_path.mkdir(parents=True, exist_ok=True)

                for py_file in old_path.glob("*.py"):
                    target = new_path / py_file.name
                    self._move_file(py_file, target, f"Consolidating from {old_dir}")

    def consolidate_templates(self):
        """Consolidate template directories"""
        self.log_action("consolidate", "Consolidating templates")

        templates_dir = self.atlas_root / "templates"

        # Merge template directories
        template_sources = ["05_TEMPLATES", "06_TEMPLATES", "06_CHECKLISTS"]

        for source_dir in template_sources:
            source_path = self.atlas_root / source_dir
            if source_path.exists():
                for file_path in source_path.rglob("*"):
                    if file_path.is_file():
                        relative_path = file_path.relative_to(source_path)
                        target = templates_dir / relative_path
                        self._move_file(file_path, target, f"Template consolidation from {source_dir}")

    def migrate_other_directories(self):
        """Migrate other directories to new structure"""
        migrations = {
            "01_STANDARDS_AND_AGREEMENTS": "standards",
            "02_ROLES": "roles",
            "04_METRICS": "metrics",
            "08_AGENT_TYPES": "agents",
            "10_INTEGRATIONS": "integrations"
        }

        for old_name, new_name in migrations.items():
            old_path = self.atlas_root / old_name
            new_path = self.atlas_root / new_name

            if old_path.exists():
                if not self.dry_run:
                    if new_path.exists():
                        shutil.rmtree(new_path)
                    shutil.move(str(old_path), str(new_path))

                self.log_action("migrate", f"Moved {old_name} to {new_name}")

    def archive_deprecated(self):
        """Move deprecated components to _deprecated folder"""
        deprecated_dir = self.atlas_root / "_deprecated"

        if not self.dry_run:
            deprecated_dir.mkdir(exist_ok=True)

        # List of directories/files to deprecate
        deprecated_items = [
            "04_PLATFORMS", "05_LEARNINGS", "09_LLM_OPTIMIZATION", "11_PARALLEL_PATTERNS",
            "component_docs", "interfaces", "iterations",
            "PARALLEL_EXECUTION_ENFORCEMENT.md", "VERSION_STANDARD.md"
        ]

        for item in deprecated_items:
            item_path = self.atlas_root / item
            if item_path.exists():
                target = deprecated_dir / item
                if not self.dry_run:
                    if item_path.is_dir():
                        shutil.move(str(item_path), str(target))
                    else:
                        shutil.move(str(item_path), str(target))

                self.log_action("archive", f"Archived {item} to _deprecated")

    def create_unified_cli(self):
        """Create the unified Atlas CLI tool"""
        self.log_action("create", "Creating unified Atlas CLI")

        cli_content = '''#!/usr/bin/env python3
"""
Atlas Framework - Unified CLI

The single entry point for all Atlas operations.
No more confusion between multiple scripts - this is THE way to use Atlas.

Usage:
    python atlas.py workflow start <feature_id>
    python atlas.py workflow status
    python atlas.py review submit <feature_id>
    python atlas.py review status
    python atlas.py validate
    python atlas.py dashboard
    python atlas.py metrics
"""

import sys
import argparse
from pathlib import Path

# Add automation directory to path
sys.path.insert(0, str(Path(__file__).parent / "automation" / "core"))
sys.path.insert(0, str(Path(__file__).parent / "automation" / "processes"))

def main():
    parser = argparse.ArgumentParser(description="Atlas Framework - Unified CLI")
    subparsers = parser.add_subparsers(dest="command", help="Available commands")

    # Workflow commands
    workflow_parser = subparsers.add_parser("workflow", help="Workflow operations")
    workflow_subparsers = workflow_parser.add_subparsers(dest="workflow_action")

    start_parser = workflow_subparsers.add_parser("start", help="Start a workflow")
    start_parser.add_argument("feature_id", help="Feature ID to start")

    workflow_subparsers.add_parser("status", help="Show workflow status")

    # Review commands
    review_parser = subparsers.add_parser("review", help="Review operations")
    review_subparsers = review_parser.add_subparsers(dest="review_action")

    submit_parser = review_subparsers.add_parser("submit", help="Submit for review")
    submit_parser.add_argument("feature_id", help="Feature ID to review")

    review_subparsers.add_parser("status", help="Show review status")

    # Other commands
    subparsers.add_parser("validate", help="Validate Atlas configuration")
    subparsers.add_parser("dashboard", help="Open Atlas dashboard")
    subparsers.add_parser("metrics", help="Show Atlas metrics")

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return

    # Route to appropriate handler
    if args.command == "workflow":
        handle_workflow(args)
    elif args.command == "review":
        handle_review(args)
    elif args.command == "validate":
        handle_validate(args)
    elif args.command == "dashboard":
        handle_dashboard(args)
    elif args.command == "metrics":
        handle_metrics(args)

def handle_workflow(args):
    """Handle workflow commands"""
    if args.workflow_action == "start":
        print(f"Starting workflow for {args.feature_id}")
        # Import and use workflow orchestrator
        try:
            from parallel_orchestrator import ParallelOrchestrator
            orchestrator = ParallelOrchestrator()
            orchestrator.start_workflow(args.feature_id)
        except ImportError:
            print("Workflow orchestrator not found. Please check automation setup.")

    elif args.workflow_action == "status":
        print("Showing workflow status")
        try:
            from workflow_state_machine import WorkflowStateMachine
            state_machine = WorkflowStateMachine()
            state_machine.show_status()
        except ImportError:
            print("Workflow state machine not found. Please check automation setup.")

def handle_review(args):
    """Handle review commands"""
    if args.review_action == "submit":
        print(f"Submitting {args.feature_id} for review")
        try:
            from differential_reviewer import DifferentialReviewer
            reviewer = DifferentialReviewer()
            reviewer.submit_for_review(args.feature_id)
        except ImportError:
            print("Review system not found. Please check automation setup.")

    elif args.review_action == "status":
        print("Showing review status")

def handle_validate(args):
    """Handle validation"""
    print("Validating Atlas configuration")
    try:
        from workflow_validator import WorkflowValidator
        validator = WorkflowValidator()
        validator.validate_all()
    except ImportError:
        print("Validator not found. Please check automation setup.")

def handle_dashboard(args):
    """Handle dashboard"""
    print("Opening Atlas dashboard")
    try:
        from dashboard import Dashboard
        dashboard = Dashboard()
        dashboard.open()
    except ImportError:
        print("Dashboard not found. Please check automation setup.")

def handle_metrics(args):
    """Handle metrics"""
    print("Showing Atlas metrics")

if __name__ == "__main__":
    main()
'''

        cli_path = self.atlas_root / "atlas.py"
        if not self.dry_run:
            cli_path.write_text(cli_content)
            cli_path.chmod(0o755)  # Make executable

        self.log_action("create", "Created unified Atlas CLI at atlas.py")

    def update_documentation(self):
        """Update documentation to reflect new structure"""
        self.log_action("update", "Updating documentation")

        # Create main README
        readme_content = '''# Atlas Framework

The unified development workflow framework. No versions, no confusion - just the definitive way to build great software.

## Quick Start

1. **Initialize a workflow**: `python atlas.py workflow start F001`
2. **Submit for review**: `python atlas.py review submit F001`
3. **Check status**: `python atlas.py workflow status`

## Structure

- `workflows/` - All workflow documentation
- `automation/` - All Python scripts and tools
- `templates/` - All templates and checklists
- `metrics/` - Metrics and dashboards
- `agents/` - Agent specifications
- `standards/` - Standards and agreements
- `roles/` - Role definitions
- `integrations/` - External integrations

## The Atlas Way

Atlas provides **one clear path** for each task:

- **Workflow**: Follow `workflows/ATLAS_WORKFLOW.md`
- **Review**: Use `workflows/REVIEW_PROCESS.md`
- **Validation**: Run `python atlas.py validate`
- **Metrics**: Check `python atlas.py metrics`

No "enhanced" versions, no alternatives - just the right way to do things.

## Migration from Previous Versions

If you're migrating from an older Atlas version, run:
```bash
python migrate_to_atlas_2_1.py --backup
```

This will consolidate everything into the unified structure.
'''

        readme_path = self.atlas_root / "README.md"
        if not self.dry_run:
            readme_path.write_text(readme_content)

        self.log_action("create", "Created unified README.md")

        # Create Quick Start guide
        quickstart_content = '''# Atlas Quick Start

Get productive with Atlas in 5 minutes.

## Single Path Setup

1. **Start here**: `python atlas.py workflow start <feature_id>`
2. **That's it** - Atlas handles the rest

## Commands You Need

- `python atlas.py workflow start F001` - Start working on F001
- `python atlas.py workflow status` - See what's happening
- `python atlas.py review submit F001` - Submit F001 for review
- `python atlas.py validate` - Check everything is working
- `python atlas.py dashboard` - Open the visual dashboard

## The Only Workflow

1. Start a feature: `workflow start`
2. Build it (Atlas guides you)
3. Submit for review: `review submit`
4. Deploy (Atlas handles it)

No choices to make, no configuration needed. Just build.
'''

        quickstart_path = self.atlas_root / "QUICK_START.md"
        if not self.dry_run:
            quickstart_path.write_text(quickstart_content)

        self.log_action("create", "Created QUICK_START.md")

    def _move_file(self, source: Path, target: Path, reason: str):
        """Move a file with logging"""
        if not self.dry_run:
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(source), str(target))

        self.log_action("move", f"Moved {source.name} to {target}: {reason}")

    def _merge_file_content(self, source: Path, target: Path, reason: str):
        """Merge content from source into target file"""
        if not source.exists():
            self.log_action("warning", f"Source file for merge not found: {source}")
            return

        if not self.dry_run:
            target.parent.mkdir(parents=True, exist_ok=True)

            # Read source content
            source_content = source.read_text()

            # If target exists, append; otherwise create
            if target.exists():
                existing_content = target.read_text()
                merged_content = f"{existing_content}\n\n<!-- MERGED FROM {source.name} -->\n{source_content}"
            else:
                merged_content = source_content

            target.write_text(merged_content)

        self.log_action("merge", f"Merged {source.name} into {target.name}: {reason}")

    def update_cross_references(self):
        """Update cross-references in documentation"""
        self.log_action("update", "Updating cross-references")

        # Define reference mappings
        reference_updates = {
            r'03_PROCESSES/': 'workflows/',
            r'07_AUTOMATION/': 'automation/core/',
            r'07_SCRIPTS_AND_AUTOMATION/': 'automation/processes/',
            r'05_TEMPLATES/': 'templates/',
            r'06_TEMPLATES/': 'templates/',
            r'08_AGENT_TYPES/': 'agents/',
            r'04_ENHANCED_WORKFLOW\.md': 'ATLAS_WORKFLOW.md',
            r'05_ENHANCED_REVIEW_PROCESS\.md': 'REVIEW_PROCESS.md',
            r'enhanced workflow': 'Atlas workflow',
            r'Enhanced workflow': 'Atlas workflow',
            r'smart review': 'review process',
            r'Smart review': 'Review process',
        }

        # Update all markdown files
        for md_file in self.atlas_root.rglob("*.md"):
            if "_deprecated" in str(md_file):
                continue

            if not self.dry_run:
                try:
                    content = md_file.read_text()
                    updated_content = content

                    for old_ref, new_ref in reference_updates.items():
                        updated_content = re.sub(old_ref, new_ref, updated_content)

                    if updated_content != content:
                        md_file.write_text(updated_content)
                        self.log_action("update", f"Updated references in {md_file.name}")

                except Exception as e:
                    self.log_action("error", f"Failed to update {md_file}: {e}")

    def save_migration_report(self):
        """Save the migration report"""
        report_path = self.atlas_root / "migration_report.json"

        if not self.dry_run:
            with open(report_path, 'w') as f:
                json.dump(self.migration_report, f, indent=2)

        # Print summary
        print("\n" + "="*50)
        print("ATLAS 2.1 MIGRATION SUMMARY")
        print("="*50)
        print(f"Actions completed: {len(self.migration_report['actions'])}")
        print(f"Warnings: {len(self.migration_report['warnings'])}")
        print(f"Errors: {len(self.migration_report['errors'])}")

        if self.migration_report['errors']:
            print("\nErrors encountered:")
            for error in self.migration_report['errors']:
                print(f"  - {error['message']}")

        print(f"\nDetailed report saved to: {report_path}")
        print("\nAtlas 2.1 consolidation complete!")
        print("Use 'python atlas.py' for all operations going forward.")

    def run_migration(self):
        """Run the complete migration process"""
        print("Starting Atlas 2.1 Migration...")
        print(f"Root directory: {self.atlas_root}")
        print(f"Dry run: {self.dry_run}")
        print(f"Create backup: {self.backup}")
        print()

        steps = [
            ("Creating backup", self.create_backup),
            ("Creating new structure", self.create_new_structure),
            ("Consolidating processes", self.consolidate_processes),
            ("Consolidating automation", self.consolidate_automation),
            ("Consolidating templates", self.consolidate_templates),
            ("Migrating other directories", self.migrate_other_directories),
            ("Archiving deprecated items", self.archive_deprecated),
            ("Creating unified CLI", self.create_unified_cli),
            ("Updating documentation", self.update_documentation),
            ("Updating cross-references", self.update_cross_references),
            ("Saving migration report", self.save_migration_report)
        ]

        for step_name, step_func in steps:
            print(f"\n{step_name}...")
            try:
                result = step_func()
                if result is False:  # Explicit failure
                    self.log_action("error", f"Step failed: {step_name}")
                    break
            except Exception as e:
                self.log_action("error", f"Step failed: {step_name} - {e}")
                break


def main():
    parser = argparse.ArgumentParser(description="Migrate Atlas to unified 2.1 structure")
    parser.add_argument("--dry-run", action="store_true",
                       help="Show what would be done without making changes")
    parser.add_argument("--no-backup", action="store_true",
                       help="Skip creating backup (not recommended)")
    parser.add_argument("--root", default=".",
                       help="Atlas root directory (default: current directory)")

    args = parser.parse_args()

    migrator = AtlasMigrator(
        atlas_root=args.root,
        dry_run=args.dry_run,
        backup=not args.no_backup
    )

    migrator.run_migration()


if __name__ == "__main__":
    main()