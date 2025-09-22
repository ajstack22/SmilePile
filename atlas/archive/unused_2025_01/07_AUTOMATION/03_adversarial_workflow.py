#!/usr/bin/env python3
"""
Atlas Adversarial Workflow Script - Designed to be called by Claude
Executes quality-gated development workflow with aggressive peer review
"""

import sys
import json
import os
from pathlib import Path
from datetime import datetime
import subprocess

class AdversarialWorkflow:
    """
    Manages adversarial development workflow with quality gates
    """

    def __init__(self):
        self.workflow_dir = Path('.atlas/workflow')
        self.workflow_dir.mkdir(parents=True, exist_ok=True)
        self.session_file = self.workflow_dir / 'current_session.json'
        self.session = self.load_session()

    def load_session(self):
        """Load current workflow session"""
        if self.session_file.exists():
            with open(self.session_file, 'r') as f:
                return json.load(f)
        return {}

    def save_session(self):
        """Save workflow session"""
        with open(self.session_file, 'w') as f:
            json.dump(self.session, f, indent=2)

    def start_workflow(self, story_id, developer='Developer', reviewer='Reviewer'):
        """
        Start adversarial workflow for a story

        Args:
            story_id: ID of the story to implement
            developer: Developer agent name
            reviewer: Reviewer agent name

        Returns:
            Workflow initialization details
        """
        workflow_id = f"WF{datetime.now().strftime('%Y%m%d%H%M%S')}"

        self.session = {
            'workflow_id': workflow_id,
            'story_id': story_id,
            'developer': developer,
            'reviewer': reviewer,
            'status': 'started',
            'phase': 'planning',
            'started_at': datetime.now().isoformat(),
            'phases_completed': [],
            'review_cycles': 0,
            'quality_scores': [],
            'evidence': {}
        }

        self.save_session()

        return {
            'action': 'start_workflow',
            'workflow_id': workflow_id,
            'story_id': story_id,
            'status': 'started',
            'phase': 'planning',
            'next_steps': [
                'Analyze story requirements',
                'Create implementation plan',
                'Begin development'
            ],
            'agents': {
                'developer': developer,
                'reviewer': reviewer
            }
        }

    def execute_phase(self, phase_name, execution_data=None):
        """
        Execute a workflow phase

        Args:
            phase_name: Name of the phase to execute
            execution_data: Data from phase execution

        Returns:
            Phase execution results
        """
        if not self.session:
            return {
                'error': 'No active workflow',
                'suggestion': 'Start a workflow first with "start" command'
            }

        phase_result = {
            'phase': phase_name,
            'executed_at': datetime.now().isoformat(),
            'data': execution_data or {},
            'status': 'completed'
        }

        # Phase-specific logic
        if phase_name == 'planning':
            phase_result['next_phase'] = 'implementation'
            phase_result['checklist'] = [
                'Requirements analyzed',
                'Technical approach defined',
                'Test strategy outlined'
            ]

        elif phase_name == 'implementation':
            phase_result['next_phase'] = 'review'
            phase_result['checklist'] = [
                'Code implemented',
                'Unit tests written',
                'Self-review completed'
            ]

        elif phase_name == 'review':
            self.session['review_cycles'] += 1
            # Simulate quality score
            quality_score = execution_data.get('quality_score', 0) if execution_data else 0
            self.session['quality_scores'].append(quality_score)

            if quality_score >= 80:
                phase_result['next_phase'] = 'testing'
                phase_result['review_passed'] = True
            else:
                phase_result['next_phase'] = 'implementation'
                phase_result['review_passed'] = False
                phase_result['feedback'] = execution_data.get('feedback', []) if execution_data else []

        elif phase_name == 'testing':
            phase_result['next_phase'] = 'integration'
            phase_result['checklist'] = [
                'Unit tests pass',
                'Integration tests pass',
                'Edge cases covered'
            ]

        elif phase_name == 'integration':
            phase_result['next_phase'] = 'completion'
            phase_result['checklist'] = [
                'Code merged',
                'Documentation updated',
                'Deployment ready'
            ]

        # Update session
        self.session['phase'] = phase_result.get('next_phase', 'completed')
        self.session['phases_completed'].append(phase_result)

        if phase_name in self.session.get('evidence', {}):
            self.session['evidence'][phase_name] = execution_data

        self.save_session()

        return {
            'action': 'execute_phase',
            'phase_result': phase_result,
            'workflow_id': self.session['workflow_id'],
            'current_phase': self.session['phase'],
            'review_cycles': self.session['review_cycles']
        }

    def submit_review(self, review_type, findings, quality_score=None):
        """
        Submit review results

        Args:
            review_type: Type of review (code, design, security, etc.)
            findings: List of review findings
            quality_score: Numerical quality score (0-100)

        Returns:
            Review submission results
        """
        if not self.session:
            return {
                'error': 'No active workflow'
            }

        review = {
            'type': review_type,
            'findings': findings,
            'quality_score': quality_score,
            'submitted_at': datetime.now().isoformat(),
            'reviewer': self.session.get('reviewer', 'Unknown')
        }

        if 'reviews' not in self.session:
            self.session['reviews'] = []

        self.session['reviews'].append(review)

        # Determine if review passes
        review_passed = quality_score >= 80 if quality_score else len(findings) == 0

        # If review fails, go back to implementation
        if not review_passed:
            self.session['phase'] = 'implementation'
            action_required = 'Address review findings and resubmit'
        else:
            action_required = 'Proceed to next phase'

        self.save_session()

        return {
            'action': 'submit_review',
            'review': review,
            'review_passed': review_passed,
            'action_required': action_required,
            'total_reviews': len(self.session['reviews']),
            'average_quality': sum(r.get('quality_score', 0) for r in self.session['reviews']) / len(self.session['reviews']) if self.session['reviews'] else 0
        }

    def complete_workflow(self, final_evidence=None):
        """
        Complete the workflow

        Args:
            final_evidence: Final evidence of completion

        Returns:
            Workflow completion summary
        """
        if not self.session:
            return {
                'error': 'No active workflow'
            }

        self.session['status'] = 'completed'
        self.session['completed_at'] = datetime.now().isoformat()
        self.session['final_evidence'] = final_evidence or {}

        # Calculate metrics
        start_time = datetime.fromisoformat(self.session['started_at'])
        end_time = datetime.now()
        duration_hours = (end_time - start_time).total_seconds() / 3600

        summary = {
            'action': 'complete_workflow',
            'workflow_id': self.session['workflow_id'],
            'story_id': self.session['story_id'],
            'status': 'completed',
            'metrics': {
                'duration_hours': round(duration_hours, 2),
                'phases_completed': len(self.session['phases_completed']),
                'review_cycles': self.session['review_cycles'],
                'average_quality': sum(self.session['quality_scores']) / len(self.session['quality_scores']) if self.session['quality_scores'] else 0,
                'final_quality': self.session['quality_scores'][-1] if self.session['quality_scores'] else 0
            },
            'evidence': self.session.get('evidence', {}),
            'reviews': self.session.get('reviews', [])
        }

        # Archive session
        archive_file = self.workflow_dir / f"completed_{self.session['workflow_id']}.json"
        with open(archive_file, 'w') as f:
            json.dump(self.session, f, indent=2)

        # Clear current session
        self.session = {}
        self.save_session()

        return summary

    def get_status(self):
        """
        Get current workflow status

        Returns:
            Current workflow status
        """
        if not self.session:
            return {
                'status': 'no_active_workflow'
            }

        return {
            'action': 'get_status',
            'workflow_id': self.session['workflow_id'],
            'story_id': self.session['story_id'],
            'current_phase': self.session['phase'],
            'status': self.session['status'],
            'phases_completed': [p['phase'] for p in self.session['phases_completed']],
            'review_cycles': self.session['review_cycles'],
            'quality_scores': self.session['quality_scores'],
            'last_activity': self.session['phases_completed'][-1]['executed_at'] if self.session['phases_completed'] else self.session['started_at']
        }

    def generate_report(self):
        """
        Generate workflow report

        Returns:
            Detailed workflow report
        """
        if not self.session:
            return {
                'error': 'No active workflow'
            }

        report = {
            'title': f"Adversarial Workflow Report - {self.session['workflow_id']}",
            'story_id': self.session['story_id'],
            'status': self.session['status'],
            'timeline': {
                'started': self.session['started_at'],
                'current_phase': self.session['phase'],
                'phases_completed': self.session['phases_completed']
            },
            'quality_metrics': {
                'review_cycles': self.session['review_cycles'],
                'quality_scores': self.session['quality_scores'],
                'average_quality': sum(self.session['quality_scores']) / len(self.session['quality_scores']) if self.session['quality_scores'] else 0
            },
            'reviews': self.session.get('reviews', []),
            'recommendations': self._generate_recommendations()
        }

        return report

    def _generate_recommendations(self):
        """Generate recommendations based on workflow data"""
        recommendations = []

        if self.session['review_cycles'] > 3:
            recommendations.append("High review cycles indicate need for better initial planning")

        if self.session['quality_scores'] and self.session['quality_scores'][-1] < 90:
            recommendations.append("Consider additional quality improvements before deployment")

        if len(self.session['phases_completed']) < 3:
            recommendations.append("Ensure all critical phases are completed")

        return recommendations

def main():
    """
    Entry point when Claude runs this script

    Usage:
        python 03_adversarial_workflow.py start S001 --developer Dev --reviewer Rev
        python 03_adversarial_workflow.py execute planning
        python 03_adversarial_workflow.py review --type code --score 85 --findings "Minor issues"
        python 03_adversarial_workflow.py status
        python 03_adversarial_workflow.py complete
        python 03_adversarial_workflow.py report
    """
    workflow = AdversarialWorkflow()

    # Parse command line arguments
    args = sys.argv[1:]

    if not args:
        # Return usage information
        usage = {
            'script': '03_adversarial_workflow.py',
            'description': 'Adversarial development workflow manager',
            'commands': [
                {
                    'command': 'start [story_id] --developer [name] --reviewer [name]',
                    'description': 'Start workflow for a story'
                },
                {
                    'command': 'execute [phase_name]',
                    'description': 'Execute workflow phase'
                },
                {
                    'command': 'review --type [type] --score [0-100] --findings "findings"',
                    'description': 'Submit review results'
                },
                {
                    'command': 'status',
                    'description': 'Get workflow status'
                },
                {
                    'command': 'complete',
                    'description': 'Complete workflow'
                },
                {
                    'command': 'report',
                    'description': 'Generate workflow report'
                }
            ]
        }
        print(json.dumps(usage, indent=2))
        return

    command = args[0]

    if command == 'start':
        story_id = args[1] if len(args) > 1 else 'S001'
        developer = 'Developer'
        reviewer = 'Reviewer'

        # Parse optional arguments
        if '--developer' in args:
            idx = args.index('--developer')
            if idx + 1 < len(args):
                developer = args[idx + 1]

        if '--reviewer' in args:
            idx = args.index('--reviewer')
            if idx + 1 < len(args):
                reviewer = args[idx + 1]

        result = workflow.start_workflow(story_id, developer, reviewer)
        print(json.dumps(result, indent=2))

    elif command == 'execute':
        phase_name = args[1] if len(args) > 1 else 'planning'

        # Parse execution data if provided
        execution_data = {}
        if '--data' in args:
            idx = args.index('--data')
            if idx + 1 < len(args):
                try:
                    execution_data = json.loads(args[idx + 1])
                except:
                    execution_data = {'raw_data': args[idx + 1]}

        result = workflow.execute_phase(phase_name, execution_data)
        print(json.dumps(result, indent=2))

    elif command == 'review':
        review_type = 'code'
        quality_score = 0
        findings = []

        # Parse review arguments
        i = 1
        while i < len(args):
            if args[i] == '--type' and i + 1 < len(args):
                review_type = args[i + 1]
                i += 2
            elif args[i] == '--score' and i + 1 < len(args):
                quality_score = int(args[i + 1])
                i += 2
            elif args[i] == '--findings' and i + 1 < len(args):
                findings = args[i + 1].split('|')
                i += 2
            else:
                i += 1

        result = workflow.submit_review(review_type, findings, quality_score)
        print(json.dumps(result, indent=2))

    elif command == 'status':
        result = workflow.get_status()
        print(json.dumps(result, indent=2))

    elif command == 'complete':
        final_evidence = {}
        if '--evidence' in args:
            idx = args.index('--evidence')
            if idx + 1 < len(args):
                try:
                    final_evidence = json.loads(args[idx + 1])
                except:
                    final_evidence = {'evidence': args[idx + 1]}

        result = workflow.complete_workflow(final_evidence)
        print(json.dumps(result, indent=2))

    elif command == 'report':
        result = workflow.generate_report()
        print(json.dumps(result, indent=2))

    else:
        print(json.dumps({'error': f'Unknown command: {command}'}, indent=2))

if __name__ == "__main__":
    main()