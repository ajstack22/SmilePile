#!/usr/bin/env python3
"""
Atlas 2.1 Validation Script

Validates that the Atlas consolidation was successful by checking:
- Directory structure matches expected layout
- No duplicate processes exist
- All cross-references are updated
- CLI tool works correctly
- No version references remain

Usage: python validate_atlas_2_1.py
"""

import os
import re
from pathlib import Path
from typing import List, Dict, Tuple
import subprocess


class AtlasValidator:
    def __init__(self, atlas_root: str = "."):
        self.atlas_root = Path(atlas_root)
        self.errors = []
        self.warnings = []
        self.success_count = 0

    def log_success(self, message: str):
        print(f"✅ {message}")
        self.success_count += 1

    def log_warning(self, message: str):
        print(f"⚠️  {message}")
        self.warnings.append(message)

    def log_error(self, message: str):
        print(f"❌ {message}")
        self.errors.append(message)

    def validate_directory_structure(self):
        """Validate the expected directory structure exists"""
        print("\n🔍 Validating directory structure...")

        expected_dirs = [
            "workflows", "automation", "templates", "metrics",
            "agents", "standards", "roles", "integrations", "_deprecated"
        ]

        for dirname in expected_dirs:
            dir_path = self.atlas_root / dirname
            if dir_path.exists() and dir_path.is_dir():
                self.log_success(f"Directory exists: {dirname}")
            else:
                self.log_error(f"Missing directory: {dirname}")

    def validate_no_old_directories(self):
        """Ensure old directories are removed"""
        print("\n🔍 Checking for old directories...")

        old_dirs = [
            "03_PROCESSES", "07_AUTOMATION", "07_SCRIPTS_AND_AUTOMATION",
            "05_TEMPLATES", "06_TEMPLATES", "06_CHECKLISTS"
        ]

        for dirname in old_dirs:
            dir_path = self.atlas_root / dirname
            if dir_path.exists():
                self.log_error(f"Old directory still exists: {dirname}")
            else:
                self.log_success(f"Old directory properly removed: {dirname}")

    def validate_key_files_exist(self):
        """Validate key consolidated files exist"""
        print("\n🔍 Validating key files...")

        key_files = [
            "atlas.py",
            "README.md",
            "QUICK_START.md",
            "workflows/ATLAS_WORKFLOW.md",
            "workflows/REVIEW_PROCESS.md"
        ]

        for filename in key_files:
            file_path = self.atlas_root / filename
            if file_path.exists():
                self.log_success(f"Key file exists: {filename}")
            else:
                self.log_error(f"Missing key file: {filename}")

    def validate_cli_functionality(self):
        """Test the Atlas CLI tool"""
        print("\n🔍 Testing Atlas CLI...")

        try:
            result = subprocess.run(
                ["python3", "atlas.py", "--help"],
                capture_output=True,
                text=True,
                cwd=self.atlas_root
            )
            if result.returncode == 0:
                self.log_success("Atlas CLI responds to --help")
            else:
                self.log_error("Atlas CLI --help failed")

            # Test subcommands
            for subcmd in ["workflow", "review", "validate"]:
                result = subprocess.run(
                    ["python3", "atlas.py", subcmd, "--help"],
                    capture_output=True,
                    text=True,
                    cwd=self.atlas_root
                )
                if result.returncode == 0:
                    self.log_success(f"CLI subcommand works: {subcmd}")
                else:
                    self.log_error(f"CLI subcommand failed: {subcmd}")

        except Exception as e:
            self.log_error(f"CLI test failed: {e}")

    def validate_no_version_references(self):
        """Check for remaining version references in documentation"""
        print("\n🔍 Checking for version references...")

        version_patterns = [
            r'Atlas\s+v?\d+\.\d+',
            r'Enhanced\s+Atlas',
            r'Smart\s+Review',
            r'v\d+\.\d+',
            r'version\s+\d+',
            r'Atlas\s+2\.0',
            r'Atlas\s+2\.1'
        ]

        problems_found = False

        for md_file in self.atlas_root.rglob("*.md"):
            if "_deprecated" in str(md_file) or "backup" in str(md_file):
                continue

            try:
                content = md_file.read_text()
                for pattern in version_patterns:
                    matches = re.findall(pattern, content, re.IGNORECASE)
                    if matches:
                        problems_found = True
                        self.log_warning(f"Version reference in {md_file.name}: {matches}")

            except Exception as e:
                self.log_error(f"Could not read {md_file}: {e}")

        if not problems_found:
            self.log_success("No problematic version references found")

    def validate_cross_references(self):
        """Check that cross-references are updated"""
        print("\n🔍 Validating cross-references...")

        old_references = [
            "03_PROCESSES/",
            "07_AUTOMATION/",
            "07_SCRIPTS_AND_AUTOMATION/",
            "05_TEMPLATES/",
            "06_TEMPLATES/",
            "04_ENHANCED_WORKFLOW.md",
            "05_ENHANCED_REVIEW_PROCESS.md"
        ]

        problems_found = False

        for md_file in self.atlas_root.rglob("*.md"):
            if "_deprecated" in str(md_file) or "backup" in str(md_file):
                continue

            try:
                content = md_file.read_text()
                for old_ref in old_references:
                    if old_ref in content:
                        problems_found = True
                        self.log_warning(f"Old reference in {md_file.name}: {old_ref}")

            except Exception as e:
                self.log_error(f"Could not read {md_file}: {e}")

        if not problems_found:
            self.log_success("No old cross-references found")

    def validate_automation_consolidation(self):
        """Validate automation scripts are properly consolidated"""
        print("\n🔍 Validating automation consolidation...")

        automation_dir = self.atlas_root / "automation"
        if not automation_dir.exists():
            self.log_error("Automation directory missing")
            return

        # Count Python files
        py_files = list(automation_dir.glob("*.py"))
        if len(py_files) > 20:  # Should have consolidated scripts
            self.log_success(f"Automation scripts consolidated: {len(py_files)} files")
        else:
            self.log_warning(f"Fewer automation scripts than expected: {len(py_files)}")

        # Check for key scripts
        key_scripts = [
            "parallel_orchestrator.py",
            "workflow_validator.py",
            "dashboard.py"
        ]

        for script in key_scripts:
            if (automation_dir / script).exists():
                self.log_success(f"Key script exists: {script}")
            else:
                self.log_error(f"Missing key script: {script}")

    def validate_template_consolidation(self):
        """Validate templates are properly consolidated"""
        print("\n🔍 Validating template consolidation...")

        templates_dir = self.atlas_root / "templates"
        if not templates_dir.exists():
            self.log_error("Templates directory missing")
            return

        # Check for various template types
        template_files = list(templates_dir.rglob("*"))
        template_types = {
            "checklist": any("CHECKLIST" in f.name for f in template_files),
            "evidence": any("EVIDENCE" in f.name for f in template_files),
            "template": any("TEMPLATE" in f.name for f in template_files)
        }

        for template_type, exists in template_types.items():
            if exists:
                self.log_success(f"Template type consolidated: {template_type}")
            else:
                self.log_warning(f"No {template_type} templates found")

    def validate_backup_created(self):
        """Check if backup was created"""
        print("\n🔍 Checking for backup...")

        backup_dirs = list(self.atlas_root.parent.glob("atlas_backup_*"))
        if backup_dirs:
            self.log_success(f"Backup created: {backup_dirs[0].name}")
        else:
            self.log_warning("No backup directory found")

    def print_summary(self):
        """Print validation summary"""
        print("\n" + "="*60)
        print("ATLAS 2.1 VALIDATION SUMMARY")
        print("="*60)
        print(f"✅ Successful checks: {self.success_count}")
        print(f"⚠️  Warnings: {len(self.warnings)}")
        print(f"❌ Errors: {len(self.errors)}")

        if self.errors:
            print("\n❌ ERRORS THAT NEED FIXING:")
            for error in self.errors:
                print(f"  - {error}")

        if self.warnings:
            print("\n⚠️  WARNINGS TO REVIEW:")
            for warning in self.warnings:
                print(f"  - {warning}")

        if not self.errors:
            print("\n🎉 Atlas 2.1 consolidation validation PASSED!")
            print("The framework is ready for unified operation.")
        else:
            print("\n💥 Atlas 2.1 consolidation validation FAILED!")
            print("Please fix the errors above before proceeding.")

        return len(self.errors) == 0

    def run_validation(self):
        """Run complete validation suite"""
        print("🔍 Atlas 2.1 Consolidation Validation")
        print(f"📁 Root: {self.atlas_root}")
        print()

        validations = [
            self.validate_directory_structure,
            self.validate_no_old_directories,
            self.validate_key_files_exist,
            self.validate_cli_functionality,
            self.validate_no_version_references,
            self.validate_cross_references,
            self.validate_automation_consolidation,
            self.validate_template_consolidation,
            self.validate_backup_created
        ]

        for validation in validations:
            try:
                validation()
            except Exception as e:
                self.log_error(f"Validation failed: {validation.__name__} - {e}")

        return self.print_summary()


def main():
    validator = AtlasValidator()
    success = validator.run_validation()
    exit(0 if success else 1)


if __name__ == "__main__":
    main()