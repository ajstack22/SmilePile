#!/usr/bin/env python3
"""
Atlas Iteration Manager - Enforces iterative development with validation gates
Ensures each iteration is complete before allowing progression
"""

import json
import os
import subprocess
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Tuple, Optional

class IterationManager:
    """
    Manages iterative development flow with strict validation gates
    """

    def __init__(self):
        self.atlas_dir = Path(__file__).parent.parent
        self.iteration_file = self.atlas_dir / '.atlas' / 'iteration_state.json'
        self.iteration_file.parent.mkdir(parents=True, exist_ok=True)
        self.state = self._load_state()

    def _load_state(self) -> Dict:
        """Load current iteration state"""
        if self.iteration_file.exists():
            with open(self.iteration_file) as f:
                return json.load(f)
        return {
            'current_iteration': 0,
            'phase': 'not_started',
            'iterations': [],
            'test_coverage': 0,
            'last_successful_build': None,
            'working_baseline': None
        }

    def _save_state(self):
        """Save iteration state"""
        with open(self.iteration_file, 'w') as f:
            json.dump(self.state, f, indent=2, default=str)

    def start_project(self, project_name: str, total_iterations: int) -> Dict:
        """Initialize a new iterative project"""
        self.state = {
            'project': project_name,
            'current_iteration': 0,
            'total_iterations': total_iterations,
            'phase': 'mws',  # Minimal Working System
            'iterations': [],
            'test_coverage': 0,
            'last_successful_build': None,
            'working_baseline': None,
            'started_at': datetime.now().isoformat()
        }
        self._save_state()

        return {
            'action': 'project_started',
            'project': project_name,
            'next_step': 'Create Minimal Working System (MWS)',
            'requirements': {
                'must_compile': True,
                'must_run': True,
                'must_have_one_test': True,
                'must_be_demoable': True
            },
            'reminder': 'Start with the absolute simplest thing that works'
        }

    def validate_current_phase(self) -> Tuple[bool, Dict]:
        """
        Validate if current phase can proceed to next
        Returns (can_proceed, details)
        """
        phase = self.state.get('phase', 'not_started')
        iteration = self.state.get('current_iteration', 0)

        validations = {
            'build_passes': self._check_build(),
            'tests_pass': self._check_tests(),
            'coverage_adequate': self._check_coverage(),
            'documentation_exists': self._check_documentation(),
            'demo_completed': self._check_demo_flag(),
            'baseline_committed': self._check_git_status()
        }

        # Determine if we can proceed
        if phase == 'mws':
            required = ['build_passes', 'tests_pass', 'baseline_committed']
        elif phase == 'development':
            required = ['build_passes', 'tests_pass', 'coverage_adequate',
                       'documentation_exists', 'baseline_committed']
        elif phase == 'integration':
            required = ['build_passes', 'tests_pass', 'demo_completed']
        else:
            required = []

        all_passed = all(validations[r] for r in required)

        return all_passed, {
            'phase': phase,
            'iteration': iteration,
            'validations': validations,
            'can_proceed': all_passed,
            'failed_checks': [k for k in required if not validations[k]],
            'next_phase': self._get_next_phase() if all_passed else None
        }

    def _check_build(self) -> bool:
        """Check if the project builds successfully"""
        # Try common build commands
        build_commands = [
            './gradlew build',
            'npm run build',
            'yarn build',
            'make',
            'cargo build',
            'go build ./...',
            'python -m py_compile **/*.py'
        ]

        for cmd in build_commands:
            try:
                result = subprocess.run(
                    cmd,
                    shell=True,
                    capture_output=True,
                    text=True,
                    timeout=60
                )
                if result.returncode == 0:
                    self.state['last_successful_build'] = datetime.now().isoformat()
                    self._save_state()
                    return True
            except:
                continue

        return False

    def _check_tests(self) -> bool:
        """Check if tests pass"""
        test_commands = [
            './gradlew test',
            'npm test',
            'yarn test',
            'pytest',
            'go test ./...',
            'cargo test'
        ]

        for cmd in test_commands:
            try:
                result = subprocess.run(
                    cmd,
                    shell=True,
                    capture_output=True,
                    text=True,
                    timeout=120
                )
                if result.returncode == 0:
                    return True
            except:
                continue

        return False

    def _check_coverage(self) -> bool:
        """Check if test coverage meets minimum for iteration"""
        iteration = self.state.get('current_iteration', 0)
        current_coverage = self.state.get('test_coverage', 0)

        # Expected coverage by iteration
        expected_coverage = {
            0: 10,   # MWS
            1: 20,
            2: 30,
            3: 40,
            4: 50,
            5: 60,
            6: 70,
            7: 80
        }

        min_coverage = expected_coverage.get(iteration, 80)
        return current_coverage >= min_coverage

    def _check_documentation(self) -> bool:
        """Check if iteration documentation exists"""
        iteration = self.state.get('current_iteration', 0)
        doc_file = self.atlas_dir / 'iterations' / f'iteration_{iteration:03d}.md'
        return doc_file.exists()

    def _check_demo_flag(self) -> bool:
        """Check if demo was marked complete"""
        current_iteration = self.state.get('iterations', [])
        if current_iteration:
            latest = current_iteration[-1]
            return latest.get('demo_completed', False)
        return False

    def _check_git_status(self) -> bool:
        """Check if changes are committed"""
        try:
            result = subprocess.run(
                'git status --porcelain',
                shell=True,
                capture_output=True,
                text=True
            )
            # Clean status means everything is committed
            return len(result.stdout.strip()) == 0
        except:
            return False

    def _get_next_phase(self) -> str:
        """Determine next phase based on current state"""
        phase = self.state.get('phase', 'not_started')

        if phase == 'mws':
            return 'iteration_1'
        elif phase.startswith('iteration_'):
            current_num = int(phase.split('_')[1])
            total = self.state.get('total_iterations', 10)
            if current_num < total:
                return f'iteration_{current_num + 1}'
            else:
                return 'finalization'
        else:
            return 'complete'

    def proceed_to_next(self, demo_notes: str = None) -> Dict:
        """Move to next iteration after validation passes"""
        can_proceed, validation = self.validate_current_phase()

        if not can_proceed:
            return {
                'action': 'blocked',
                'reason': 'Validation failed',
                'failed_checks': validation['failed_checks'],
                'fix_required': self._get_fix_instructions(validation['failed_checks'])
            }

        # Record current iteration completion
        self.state['iterations'].append({
            'number': self.state['current_iteration'],
            'completed_at': datetime.now().isoformat(),
            'test_coverage': self.state.get('test_coverage', 0),
            'demo_notes': demo_notes
        })

        # Move to next iteration
        next_phase = validation['next_phase']
        self.state['phase'] = next_phase
        self.state['current_iteration'] += 1
        self._save_state()

        return {
            'action': 'proceeded',
            'from_phase': validation['phase'],
            'to_phase': next_phase,
            'next_iteration': self.state['current_iteration'],
            'next_steps': self._get_iteration_instructions(next_phase)
        }

    def _get_fix_instructions(self, failed_checks: List[str]) -> Dict:
        """Get specific instructions for fixing failed checks"""
        fixes = {}

        if 'build_passes' in failed_checks:
            fixes['build_passes'] = {
                'issue': 'Build is failing',
                'action': 'Fix compilation errors',
                'commands': [
                    'Run build command and review errors',
                    'Fix syntax and type errors',
                    'Resolve dependency issues'
                ]
            }

        if 'tests_pass' in failed_checks:
            fixes['tests_pass'] = {
                'issue': 'Tests are failing',
                'action': 'Fix failing tests',
                'commands': [
                    'Run test suite',
                    'Fix broken tests',
                    'Update tests for new code'
                ]
            }

        if 'coverage_adequate' in failed_checks:
            fixes['coverage_adequate'] = {
                'issue': 'Test coverage too low',
                'action': 'Add more tests',
                'current': self.state.get('test_coverage', 0),
                'required': self._get_required_coverage()
            }

        if 'documentation_exists' in failed_checks:
            fixes['documentation_exists'] = {
                'issue': 'Missing iteration documentation',
                'action': 'Create iteration docs',
                'template': self._get_doc_template()
            }

        if 'baseline_committed' in failed_checks:
            fixes['baseline_committed'] = {
                'issue': 'Uncommitted changes',
                'action': 'Commit your changes',
                'commands': [
                    'git add .',
                    'git commit -m "Iteration X: Feature description"'
                ]
            }

        return fixes

    def _get_required_coverage(self) -> int:
        """Get required coverage for current iteration"""
        iteration = self.state.get('current_iteration', 0)
        coverage_map = {
            0: 10, 1: 20, 2: 30, 3: 40, 4: 50,
            5: 60, 6: 70, 7: 80
        }
        return coverage_map.get(iteration, 80)

    def _get_doc_template(self) -> str:
        """Get documentation template for iteration"""
        return """# Iteration {iteration}: {feature_name}

## What Was Added
- Feature description
- Key components

## How It Works
- Technical explanation
- Integration points

## Tests Added
- Unit tests
- Integration tests
- Coverage metrics

## API/Interface
- New methods/endpoints
- Parameters
- Return values

## Dependencies
- External libraries
- Internal components

## Known Limitations
- Current constraints
- Future improvements needed
"""

    def _get_iteration_instructions(self, phase: str) -> Dict:
        """Get specific instructions for next iteration"""
        if phase.startswith('iteration_'):
            num = int(phase.split('_')[1])
            return {
                'phase': f'Iteration {num}',
                'focus': self._get_iteration_focus(num),
                'reminder': self._get_iteration_reminder(num),
                'checklist': [
                    'Research the feature approach',
                    'Write tests first (TDD)',
                    'Implement the feature',
                    'Integrate with existing code',
                    'Update documentation',
                    'Run all tests',
                    'Demo the feature',
                    'Commit working code'
                ]
            }
        elif phase == 'finalization':
            return {
                'phase': 'Finalization',
                'focus': 'Polish and optimization',
                'checklist': [
                    'Full integration testing',
                    'Performance optimization',
                    'Documentation review',
                    'Final test coverage check',
                    'Deployment preparation'
                ]
            }
        else:
            return {
                'phase': phase,
                'status': 'Complete or unknown phase'
            }

    def _get_iteration_focus(self, num: int) -> str:
        """Get suggested focus for iteration number"""
        focuses = {
            1: "Core functionality - the main use case",
            2: "User interaction - basic UI/UX",
            3: "Data persistence - save/load state",
            4: "Error handling - graceful failures",
            5: "Advanced features - nice-to-haves",
            6: "Performance - optimization",
            7: "Polish - final touches"
        }
        return focuses.get(num, "Refinement and improvement")

    def _get_iteration_reminder(self, num: int) -> str:
        """Get reminder for iteration"""
        reminders = {
            1: "Keep it simple - just make the core feature work",
            2: "Focus on usability - can users actually use this?",
            3: "Ensure data integrity - don't lose user data",
            4: "Handle edge cases - what could go wrong?",
            5: "Add value - what would delight users?",
            6: "Make it fast - where are the bottlenecks?",
            7: "Perfect the details - polish matters"
        }
        return reminders.get(num, "Build on what works")

    def get_current_context(self) -> Dict:
        """Get full context for current iteration"""
        return {
            'project': self.state.get('project', 'Unknown'),
            'iteration': self.state.get('current_iteration', 0),
            'phase': self.state.get('phase', 'not_started'),
            'test_coverage': self.state.get('test_coverage', 0),
            'iterations_completed': len(self.state.get('iterations', [])),
            'last_build': self.state.get('last_successful_build'),
            'working_baseline': self.state.get('working_baseline'),
            'next_steps': self._get_iteration_instructions(self.state.get('phase'))
        }

    def mark_demo_complete(self, notes: str = None) -> Dict:
        """Mark that demo was completed for current iteration"""
        if not self.state.get('iterations'):
            self.state['iterations'] = []

        current = {
            'number': self.state.get('current_iteration', 0),
            'demo_completed': True,
            'demo_notes': notes,
            'timestamp': datetime.now().isoformat()
        }

        # Update or append
        if self.state['iterations'] and \
           self.state['iterations'][-1].get('number') == current['number']:
            self.state['iterations'][-1].update(current)
        else:
            self.state['iterations'].append(current)

        self._save_state()

        return {
            'action': 'demo_marked_complete',
            'iteration': current['number'],
            'notes': notes
        }

    def update_coverage(self, coverage: float) -> Dict:
        """Update test coverage metrics"""
        old_coverage = self.state.get('test_coverage', 0)
        self.state['test_coverage'] = coverage
        self._save_state()

        required = self._get_required_coverage()

        return {
            'action': 'coverage_updated',
            'old_coverage': old_coverage,
            'new_coverage': coverage,
            'required_coverage': required,
            'meets_requirement': coverage >= required,
            'improvement': coverage - old_coverage
        }

def main():
    """CLI interface for iteration manager"""
    import sys

    manager = IterationManager()
    args = sys.argv[1:]

    if not args:
        print(json.dumps({
            'error': 'No command provided',
            'usage': {
                'start': 'iteration_manager.py start "Project Name" 7',
                'validate': 'iteration_manager.py validate',
                'proceed': 'iteration_manager.py proceed "Demo notes"',
                'context': 'iteration_manager.py context',
                'demo': 'iteration_manager.py demo "Notes about demo"',
                'coverage': 'iteration_manager.py coverage 45.5'
            }
        }, indent=2))
        return

    command = args[0]

    if command == 'start' and len(args) >= 3:
        project = args[1]
        iterations = int(args[2])
        result = manager.start_project(project, iterations)
        print(json.dumps(result, indent=2))

    elif command == 'validate':
        can_proceed, details = manager.validate_current_phase()
        details['can_proceed'] = can_proceed
        print(json.dumps(details, indent=2))

    elif command == 'proceed':
        notes = args[1] if len(args) > 1 else None
        result = manager.proceed_to_next(notes)
        print(json.dumps(result, indent=2))

    elif command == 'context':
        result = manager.get_current_context()
        print(json.dumps(result, indent=2))

    elif command == 'demo':
        notes = ' '.join(args[1:]) if len(args) > 1 else None
        result = manager.mark_demo_complete(notes)
        print(json.dumps(result, indent=2))

    elif command == 'coverage' and len(args) >= 2:
        coverage = float(args[1])
        result = manager.update_coverage(coverage)
        print(json.dumps(result, indent=2))

    else:
        print(json.dumps({
            'error': f'Unknown command: {command}'
        }, indent=2))

if __name__ == '__main__':
    main()