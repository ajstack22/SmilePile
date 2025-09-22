#!/usr/bin/env python3
"""
Atlas Compliance Checker - Validates that work follows Atlas standards
Ensures agents and developers follow naming, priority, and process standards
"""

import sys
import json
import re
from pathlib import Path
from datetime import datetime

class ComplianceChecker:
    """
    Validates Atlas standard compliance across the project
    """

    def __init__(self):
        self.atlas_dir = Path('.atlas')
        self.features_dir = Path('features')
        self.bugs_dir = Path('bugs')
        self.tech_debt_dir = Path('tech_debt')
        self.epics_dir = Path('epics')
        self.violations = []
        self.warnings = []
        self.passed = []

    def check_naming_convention(self):
        """
        Check if items follow F####, B####, T####, E### naming
        """
        print("Checking naming conventions...")

        # Check features
        if self.features_dir.exists():
            for file in self.features_dir.glob('*.md'):
                if not re.match(r'^F\d{4}\.md$', file.name):
                    self.violations.append({
                        'type': 'naming',
                        'severity': 'high',
                        'file': str(file),
                        'issue': f'Feature file {file.name} should be F####.md format'
                    })
                else:
                    self.passed.append(f'✓ Feature {file.name} naming correct')

        # Check bugs
        if self.bugs_dir.exists():
            for file in self.bugs_dir.glob('*.md'):
                if not re.match(r'^B\d{4}\.md$', file.name):
                    self.violations.append({
                        'type': 'naming',
                        'severity': 'high',
                        'file': str(file),
                        'issue': f'Bug file {file.name} should be B####.md format'
                    })
                else:
                    self.passed.append(f'✓ Bug {file.name} naming correct')

        # Check tech debt
        if self.tech_debt_dir.exists():
            for file in self.tech_debt_dir.glob('*.md'):
                if not re.match(r'^T\d{4}\.md$', file.name):
                    self.violations.append({
                        'type': 'naming',
                        'severity': 'high',
                        'file': str(file),
                        'issue': f'Tech debt file {file.name} should be T####.md format'
                    })
                else:
                    self.passed.append(f'✓ Tech debt {file.name} naming correct')

        # Check epics
        if self.epics_dir.exists():
            for file in self.epics_dir.glob('*.md'):
                if not re.match(r'^E\d{3}\.md$', file.name):
                    self.violations.append({
                        'type': 'naming',
                        'severity': 'high',
                        'file': str(file),
                        'issue': f'Epic file {file.name} should be E###.md format'
                    })
                else:
                    self.passed.append(f'✓ Epic {file.name} naming correct')

    def check_priority_usage(self):
        """
        Check if priorities are valid and WSJF scoring is used
        """
        print("Checking priority standards...")

        valid_priorities = ['critical', 'high', 'medium', 'low']

        metadata_file = self.atlas_dir / 'backlog_metadata.json'
        if metadata_file.exists():
            with open(metadata_file, 'r') as f:
                metadata = json.load(f)

            all_items = (
                metadata.get('features', []) +
                metadata.get('stories', []) +
                metadata.get('bugs', []) +
                metadata.get('tech_debt', []) +
                metadata.get('epics', [])
            )

            for item in all_items:
                # Check priority validity
                priority = item.get('priority')
                if priority and priority not in valid_priorities:
                    self.violations.append({
                        'type': 'priority',
                        'severity': 'medium',
                        'item': item.get('id'),
                        'issue': f"Invalid priority '{priority}' - must be: {valid_priorities}"
                    })
                elif priority:
                    self.passed.append(f"✓ {item.get('id')} has valid priority: {priority}")

                # Check for WSJF scoring on non-critical items
                if priority in ['high', 'medium', 'low']:
                    if not any(k in item for k in ['business_value', 'time_criticality', 'risk_reduction']):
                        self.warnings.append({
                            'type': 'priority',
                            'severity': 'low',
                            'item': item.get('id'),
                            'issue': 'Consider adding WSJF scoring (business_value, time_criticality, risk_reduction)'
                        })

    def check_workflow_states(self):
        """
        Check if status values are valid
        """
        print("Checking workflow states...")

        valid_statuses = ['backlog', 'ready', 'in_progress', 'in_review', 'testing', 'done', 'blocked']

        metadata_file = self.atlas_dir / 'backlog_metadata.json'
        if metadata_file.exists():
            with open(metadata_file, 'r') as f:
                metadata = json.load(f)

            all_items = (
                metadata.get('features', []) +
                metadata.get('stories', []) +
                metadata.get('bugs', []) +
                metadata.get('tech_debt', []) +
                metadata.get('epics', [])
            )

            for item in all_items:
                status = item.get('status')
                if status and status not in valid_statuses:
                    self.violations.append({
                        'type': 'workflow',
                        'severity': 'high',
                        'item': item.get('id'),
                        'issue': f"Invalid status '{status}' - must be: {valid_statuses}"
                    })
                else:
                    self.passed.append(f"✓ {item.get('id')} has valid status: {status}")

    def check_parallel_execution(self):
        """
        Check if parallel execution is being utilized
        """
        print("Checking parallel execution patterns...")

        parallel_dir = self.atlas_dir / 'orchestrator'
        if parallel_dir.exists():
            parallel_files = list(parallel_dir.glob('parallel_*.json'))

            if len(parallel_files) == 0:
                self.warnings.append({
                    'type': 'performance',
                    'severity': 'medium',
                    'issue': 'No parallel execution tracked - consider spawning 3-5 agents simultaneously'
                })
            else:
                # Check recent parallel batches
                recent_batches = []
                for file in sorted(parallel_files)[-5:]:
                    with open(file, 'r') as f:
                        batch = json.load(f)
                        agent_count = batch.get('agent_count', 0)

                        if agent_count < 3:
                            self.warnings.append({
                                'type': 'performance',
                                'severity': 'low',
                                'batch': batch.get('batch_id'),
                                'issue': f'Only {agent_count} agents spawned - optimal is 3-5'
                            })
                        elif agent_count > 8:
                            self.warnings.append({
                                'type': 'performance',
                                'severity': 'low',
                                'batch': batch.get('batch_id'),
                                'issue': f'{agent_count} agents spawned - may have coordination overhead'
                            })
                        else:
                            self.passed.append(f"✓ Batch {batch.get('batch_id')} has optimal {agent_count} agents")

    def check_test_coverage(self):
        """
        Check for test file existence
        """
        print("Checking test coverage...")

        # Look for test directories
        test_dirs = ['tests', 'test', '__tests__', 'spec']
        test_found = False

        for test_dir_name in test_dirs:
            test_dir = Path(test_dir_name)
            if test_dir.exists() and test_dir.is_dir():
                test_files = list(test_dir.glob('**/*.py')) + \
                           list(test_dir.glob('**/*.js')) + \
                           list(test_dir.glob('**/*.ts'))

                if test_files:
                    test_found = True
                    self.passed.append(f"✓ Found {len(test_files)} test files in {test_dir_name}/")
                    break

        if not test_found:
            self.warnings.append({
                'type': 'quality',
                'severity': 'high',
                'issue': 'No test directory found - Atlas requires 80% test coverage'
            })

    def check_documentation(self):
        """
        Check for required documentation
        """
        print("Checking documentation...")

        required_docs = {
            'README.md': 'Project readme',
            'ARCHITECTURE.md': 'Architecture documentation',
            'CLAUDE.md': 'Orchestrator instructions'
        }

        atlas_root = Path(__file__).parent.parent

        for doc, description in required_docs.items():
            doc_path = atlas_root / doc
            if doc_path.exists():
                self.passed.append(f"✓ {description} exists: {doc}")
            else:
                self.warnings.append({
                    'type': 'documentation',
                    'severity': 'medium',
                    'issue': f'Missing {description}: {doc}'
                })

    def generate_report(self):
        """
        Generate compliance report
        """
        report = {
            'timestamp': datetime.now().isoformat(),
            'summary': {
                'violations': len(self.violations),
                'warnings': len(self.warnings),
                'passed': len(self.passed),
                'compliance_score': self._calculate_score()
            },
            'violations': self.violations,
            'warnings': self.warnings,
            'passed': self.passed,
            'recommendations': self._get_recommendations()
        }

        return report

    def _calculate_score(self):
        """Calculate overall compliance score"""
        total_checks = len(self.violations) + len(self.warnings) + len(self.passed)
        if total_checks == 0:
            return 0

        # Violations worth -10, warnings -2, passed +10
        score = (len(self.passed) * 10 - len(self.violations) * 10 - len(self.warnings) * 2)
        max_score = total_checks * 10

        percentage = max(0, min(100, (score / max_score) * 100)) if max_score > 0 else 0
        return round(percentage, 1)

    def _get_recommendations(self):
        """Get actionable recommendations based on findings"""
        recommendations = []

        if any(v['type'] == 'naming' for v in self.violations):
            recommendations.append("🔴 Fix naming violations immediately - use backlog_manager.py to recreate items")

        if any(w['type'] == 'performance' for w in self.warnings):
            recommendations.append("🟡 Increase parallel execution - spawn 3-5 agents simultaneously")

        if any(w['type'] == 'quality' for w in self.warnings):
            recommendations.append("🟡 Add test coverage - Atlas requires 80% minimum")

        if len(self.violations) == 0:
            recommendations.append("✅ Great compliance! Keep following Atlas standards")

        return recommendations

    def format_report(self, report):
        """
        Format report for display
        """
        output = []
        output.append("=" * 80)
        output.append("                    ATLAS COMPLIANCE REPORT")
        output.append("=" * 80)
        output.append(f"Generated: {report['timestamp']}")
        output.append("")

        # Summary
        summary = report['summary']
        score = summary['compliance_score']
        score_icon = "🟢" if score >= 80 else "🟡" if score >= 60 else "🔴"

        output.append(f"{score_icon} COMPLIANCE SCORE: {score}%")
        output.append(f"✅ Passed: {summary['passed']}")
        output.append(f"⚠️  Warnings: {summary['warnings']}")
        output.append(f"❌ Violations: {summary['violations']}")
        output.append("")

        # Violations (if any)
        if report['violations']:
            output.append("❌ VIOLATIONS (Must Fix)")
            output.append("-" * 40)
            for v in report['violations']:
                output.append(f"• [{v['severity'].upper()}] {v['issue']}")
            output.append("")

        # Warnings (if any)
        if report['warnings']:
            output.append("⚠️  WARNINGS (Should Fix)")
            output.append("-" * 40)
            for w in report['warnings']:
                output.append(f"• {w['issue']}")
            output.append("")

        # Recommendations
        output.append("📋 RECOMMENDATIONS")
        output.append("-" * 40)
        for rec in report['recommendations']:
            output.append(rec)
        output.append("")

        # Quick fixes
        if score < 80:
            output.append("⚡ QUICK FIXES")
            output.append("-" * 40)
            output.append("1. Run: python3 backlog_manager.py migrate  # Fix naming")
            output.append("2. Run: python3 atlas_context.py batch  # Enable parallel execution")
            output.append("3. Run: python3 02_create_story.py feature  # Use correct prefixes")

        output.append("")
        output.append("=" * 80)

        return "\n".join(output)

def main():
    """
    Entry point for compliance checking

    Usage:
        python3 compliance_check.py              # Run all checks
        python3 compliance_check.py validate     # Same as above
        python3 compliance_check.py naming       # Check naming only
        python3 compliance_check.py priority     # Check priorities only
        python3 compliance_check.py --json       # Output as JSON
    """
    checker = ComplianceChecker()

    args = sys.argv[1:]

    # Determine what to check
    check_all = True
    output_json = False

    if '--json' in args:
        output_json = True
        args.remove('--json')

    if not args or args[0] in ['validate', 'all']:
        check_all = True
    elif args[0] == 'naming':
        checker.check_naming_convention()
        check_all = False
    elif args[0] == 'priority':
        checker.check_priority_usage()
        check_all = False
    elif args[0] == 'workflow':
        checker.check_workflow_states()
        check_all = False
    elif args[0] == 'parallel':
        checker.check_parallel_execution()
        check_all = False
    elif args[0] == 'test':
        checker.check_test_coverage()
        check_all = False
    elif args[0] == 'docs':
        checker.check_documentation()
        check_all = False

    if check_all:
        checker.check_naming_convention()
        checker.check_priority_usage()
        checker.check_workflow_states()
        checker.check_parallel_execution()
        checker.check_test_coverage()
        checker.check_documentation()

    # Generate report
    report = checker.generate_report()

    # Output report
    if output_json:
        print(json.dumps(report, indent=2))
    else:
        print(checker.format_report(report))

        # Exit with error code if violations
        if report['summary']['violations'] > 0:
            sys.exit(1)

if __name__ == "__main__":
    main()