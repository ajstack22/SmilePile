#!/usr/bin/env python3
"""
Atlas Troubleshooting Script - Designed to be called by Claude
Systematic debugging and root cause analysis for production issues
"""

import sys
import json
import os
from pathlib import Path
from datetime import datetime
import subprocess

class Troubleshooter:
    """
    Manages systematic troubleshooting and debugging processes
    """

    def __init__(self):
        self.incident_dir = Path('.atlas/incidents')
        self.incident_dir.mkdir(parents=True, exist_ok=True)
        self.current_incident_file = self.incident_dir / 'current.json'
        self.current_incident = self.load_current_incident()

    def load_current_incident(self):
        """Load current incident"""
        if self.current_incident_file.exists():
            with open(self.current_incident_file, 'r') as f:
                return json.load(f)
        return {}

    def save_current_incident(self):
        """Save current incident"""
        with open(self.current_incident_file, 'w') as f:
            json.dump(self.current_incident, f, indent=2)

    def start_incident(self, title, severity='medium', affected_components=None):
        """
        Start a new incident investigation

        Args:
            title: Incident title
            severity: Incident severity (critical, high, medium, low)
            affected_components: List of affected components

        Returns:
            Incident initialization details
        """
        incident_id = f"INC{datetime.now().strftime('%Y%m%d%H%M%S')}"

        self.current_incident = {
            'incident_id': incident_id,
            'title': title,
            'severity': severity,
            'affected_components': affected_components or [],
            'status': 'investigating',
            'started_at': datetime.now().isoformat(),
            'timeline': [],
            'hypotheses': [],
            'findings': [],
            'root_cause': None,
            'resolution': None,
            'post_mortem': {}
        }

        # Add initial timeline entry
        self.add_timeline_entry('Incident reported', f"Title: {title}, Severity: {severity}")

        self.save_current_incident()

        return {
            'action': 'start_incident',
            'incident_id': incident_id,
            'title': title,
            'severity': severity,
            'status': 'investigating',
            'next_steps': [
                'Gather initial data',
                'Form hypotheses',
                'Test hypotheses',
                'Identify root cause',
                'Implement fix'
            ]
        }

    def add_timeline_entry(self, event, details=None):
        """Add entry to incident timeline"""
        if not self.current_incident:
            return {'error': 'No active incident'}

        entry = {
            'timestamp': datetime.now().isoformat(),
            'event': event,
            'details': details or ''
        }

        self.current_incident['timeline'].append(entry)
        self.save_current_incident()

        return {
            'action': 'add_timeline_entry',
            'entry': entry
        }

    def collect_diagnostics(self):
        """
        Collect diagnostic information

        Returns:
            Diagnostic data collection results
        """
        if not self.current_incident:
            return {'error': 'No active incident'}

        diagnostics = {
            'system_metrics': self._collect_system_metrics(),
            'application_logs': self._analyze_logs(),
            'running_processes': self._get_running_processes(),
            'disk_usage': self._get_disk_usage(),
            'environment': self._get_environment_info()
        }

        self.current_incident['diagnostics'] = diagnostics
        self.add_timeline_entry('Diagnostics collected', 'Real system data gathered')
        self.save_current_incident()

        return {
            'action': 'collect_diagnostics',
            'incident_id': self.current_incident['incident_id'],
            'diagnostics': diagnostics,
            'collection_time': datetime.now().isoformat()
        }

    def _collect_system_metrics(self):
        """Collect real system metrics"""
        metrics = {}

        try:
            # Get CPU usage (macOS/Linux)
            result = subprocess.run(['top', '-l', '1', '-n', '0'],
                                  capture_output=True, text=True, timeout=5)
            if result.returncode == 0:
                for line in result.stdout.split('\n'):
                    if 'CPU usage' in line:
                        metrics['cpu_info'] = line.strip()
                        break
        except:
            metrics['cpu_info'] = 'Unable to collect'

        try:
            # Get memory info
            result = subprocess.run(['vm_stat'], capture_output=True, text=True, timeout=5)
            if result.returncode == 0:
                metrics['memory_info'] = result.stdout[:500]  # First 500 chars
        except:
            metrics['memory_info'] = 'Unable to collect'

        return metrics

    def _analyze_logs(self):
        """Analyze application logs if available"""
        log_data = {
            'searched_locations': [],
            'found_logs': [],
            'recent_errors': []
        }

        # Common log locations
        log_locations = [
            Path('logs'),
            Path('/var/log'),
            Path('.logs'),
            Path('*.log')
        ]

        for location in log_locations:
            log_data['searched_locations'].append(str(location))
            if location.exists() and location.is_dir():
                for log_file in location.glob('*.log'):
                    log_data['found_logs'].append(str(log_file))
                    # Read last few lines for errors
                    try:
                        with open(log_file, 'r') as f:
                            lines = f.readlines()[-50:]  # Last 50 lines
                            for line in lines:
                                if 'ERROR' in line or 'CRITICAL' in line:
                                    log_data['recent_errors'].append(line.strip()[:200])
                    except:
                        continue

        return log_data

    def _get_running_processes(self):
        """Get list of running processes"""
        try:
            result = subprocess.run(['ps', 'aux'], capture_output=True, text=True, timeout=5)
            if result.returncode == 0:
                lines = result.stdout.split('\n')[:20]  # First 20 processes
                return {'top_processes': lines}
        except:
            return {'error': 'Unable to get process list'}

    def _get_disk_usage(self):
        """Get disk usage information"""
        try:
            result = subprocess.run(['df', '-h'], capture_output=True, text=True, timeout=5)
            if result.returncode == 0:
                return {'disk_usage': result.stdout}
        except:
            return {'error': 'Unable to get disk usage'}

    def _get_environment_info(self):
        """Get environment information"""
        return {
            'platform': sys.platform,
            'python_version': sys.version,
            'working_directory': os.getcwd(),
            'environment_variables': {k: v for k, v in os.environ.items()
                                     if 'PATH' not in k and 'SECRET' not in k.upper()
                                     and 'KEY' not in k.upper() and 'TOKEN' not in k.upper()}
        }


    def add_hypothesis(self, hypothesis, reasoning=None):
        """
        Add a hypothesis about the root cause

        Args:
            hypothesis: The hypothesis statement
            reasoning: Reasoning behind the hypothesis

        Returns:
            Updated hypotheses list
        """
        if not self.current_incident:
            return {'error': 'No active incident'}

        hyp = {
            'id': f"H{len(self.current_incident['hypotheses']) + 1:02d}",
            'statement': hypothesis,
            'reasoning': reasoning or '',
            'status': 'untested',
            'added_at': datetime.now().isoformat()
        }

        self.current_incident['hypotheses'].append(hyp)
        self.add_timeline_entry('Hypothesis added', hypothesis)
        self.save_current_incident()

        return {
            'action': 'add_hypothesis',
            'hypothesis': hyp,
            'total_hypotheses': len(self.current_incident['hypotheses'])
        }

    def test_hypothesis(self, hypothesis_id, test_description, result):
        """
        Test a hypothesis

        Args:
            hypothesis_id: ID of the hypothesis to test
            test_description: Description of the test performed
            result: Test result (confirmed, refuted, inconclusive)

        Returns:
            Test results
        """
        if not self.current_incident:
            return {'error': 'No active incident'}

        # Find hypothesis
        hypothesis = None
        for hyp in self.current_incident['hypotheses']:
            if hyp['id'] == hypothesis_id:
                hypothesis = hyp
                break

        if not hypothesis:
            return {'error': f'Hypothesis {hypothesis_id} not found'}

        # Record test
        test = {
            'hypothesis_id': hypothesis_id,
            'test_description': test_description,
            'result': result,
            'tested_at': datetime.now().isoformat()
        }

        hypothesis['status'] = result
        hypothesis['test'] = test

        self.add_timeline_entry(f'Hypothesis {hypothesis_id} tested', f'Result: {result}')
        self.save_current_incident()

        return {
            'action': 'test_hypothesis',
            'hypothesis_id': hypothesis_id,
            'test': test,
            'result': result
        }

    def identify_root_cause(self, description, evidence=None):
        """
        Identify the root cause

        Args:
            description: Root cause description
            evidence: Supporting evidence

        Returns:
            Root cause identification
        """
        if not self.current_incident:
            return {'error': 'No active incident'}

        root_cause = {
            'description': description,
            'evidence': evidence or [],
            'identified_at': datetime.now().isoformat(),
            'confirmed_hypotheses': [h['id'] for h in self.current_incident['hypotheses'] if h['status'] == 'confirmed']
        }

        self.current_incident['root_cause'] = root_cause
        self.current_incident['status'] = 'root_cause_identified'
        self.add_timeline_entry('Root cause identified', description)
        self.save_current_incident()

        return {
            'action': 'identify_root_cause',
            'incident_id': self.current_incident['incident_id'],
            'root_cause': root_cause,
            'next_step': 'Implement resolution'
        }

    def implement_resolution(self, resolution_description, steps_taken=None):
        """
        Implement resolution

        Args:
            resolution_description: Description of the resolution
            steps_taken: List of steps taken

        Returns:
            Resolution implementation details
        """
        if not self.current_incident:
            return {'error': 'No active incident'}

        resolution = {
            'description': resolution_description,
            'steps_taken': steps_taken or [],
            'implemented_at': datetime.now().isoformat(),
            'verification': {
                'metrics_restored': False,
                'errors_resolved': False,
                'performance_normal': False
            }
        }

        self.current_incident['resolution'] = resolution
        self.current_incident['status'] = 'resolving'
        self.add_timeline_entry('Resolution implemented', resolution_description)
        self.save_current_incident()

        return {
            'action': 'implement_resolution',
            'incident_id': self.current_incident['incident_id'],
            'resolution': resolution,
            'next_step': 'Verify resolution'
        }

    def verify_resolution(self, verification_checks):
        """
        Verify the resolution worked

        Args:
            verification_checks: Dictionary of verification check results

        Returns:
            Verification results
        """
        if not self.current_incident:
            return {'error': 'No active incident'}

        if 'resolution' not in self.current_incident:
            return {'error': 'No resolution to verify'}

        self.current_incident['resolution']['verification'] = verification_checks

        # Check if all verifications passed
        all_passed = all(verification_checks.values())

        if all_passed:
            self.current_incident['status'] = 'resolved'
            self.add_timeline_entry('Resolution verified', 'All checks passed')
        else:
            self.current_incident['status'] = 'resolution_failed'
            self.add_timeline_entry('Resolution verification failed', f'Failed checks: {[k for k, v in verification_checks.items() if not v]}')

        self.save_current_incident()

        return {
            'action': 'verify_resolution',
            'incident_id': self.current_incident['incident_id'],
            'verification': verification_checks,
            'status': 'resolved' if all_passed else 'resolution_failed',
            'all_checks_passed': all_passed
        }

    def complete_incident(self, lessons_learned=None):
        """
        Complete the incident

        Args:
            lessons_learned: List of lessons learned

        Returns:
            Incident completion summary
        """
        if not self.current_incident:
            return {'error': 'No active incident'}

        self.current_incident['status'] = 'completed'
        self.current_incident['completed_at'] = datetime.now().isoformat()

        # Calculate metrics
        start_time = datetime.fromisoformat(self.current_incident['started_at'])
        end_time = datetime.now()
        duration_minutes = (end_time - start_time).total_seconds() / 60

        # Generate post-mortem
        self.current_incident['post_mortem'] = {
            'incident_id': self.current_incident['incident_id'],
            'title': self.current_incident['title'],
            'severity': self.current_incident['severity'],
            'duration_minutes': round(duration_minutes, 2),
            'timeline_events': len(self.current_incident['timeline']),
            'hypotheses_tested': len(self.current_incident['hypotheses']),
            'root_cause': self.current_incident.get('root_cause', {}).get('description', 'Unknown'),
            'resolution': self.current_incident.get('resolution', {}).get('description', 'Unknown'),
            'lessons_learned': lessons_learned or [],
            'action_items': self._generate_action_items()
        }

        summary = {
            'action': 'complete_incident',
            'incident_id': self.current_incident['incident_id'],
            'status': 'completed',
            'post_mortem': self.current_incident['post_mortem']
        }

        # Archive incident
        archive_file = self.incident_dir / f"{self.current_incident['incident_id']}.json"
        with open(archive_file, 'w') as f:
            json.dump(self.current_incident, f, indent=2)

        # Clear current incident
        self.current_incident = {}
        self.save_current_incident()

        return summary

    def _generate_action_items(self):
        """Generate action items based on incident"""
        action_items = []

        if self.current_incident.get('severity') == 'critical':
            action_items.append('Review and improve monitoring for early detection')

        if self.current_incident.get('root_cause'):
            action_items.append('Implement preventive measures for identified root cause')

        if len(self.current_incident.get('hypotheses', [])) > 5:
            action_items.append('Improve diagnostic procedures for faster root cause identification')

        return action_items

    def get_status(self):
        """
        Get current incident status

        Returns:
            Current incident status
        """
        if not self.current_incident:
            return {
                'status': 'no_active_incident',
                'recent_incidents': self._get_recent_incidents()
            }

        return {
            'action': 'get_status',
            'incident_id': self.current_incident.get('incident_id'),
            'title': self.current_incident.get('title'),
            'severity': self.current_incident.get('severity'),
            'status': self.current_incident.get('status'),
            'duration_minutes': self._calculate_duration(),
            'timeline_events': len(self.current_incident.get('timeline', [])),
            'hypotheses': {
                'total': len(self.current_incident.get('hypotheses', [])),
                'tested': len([h for h in self.current_incident.get('hypotheses', []) if h.get('status') != 'untested'])
            },
            'root_cause_identified': self.current_incident.get('root_cause') is not None,
            'resolution_implemented': self.current_incident.get('resolution') is not None
        }

    def _calculate_duration(self):
        """Calculate incident duration"""
        if not self.current_incident or 'started_at' not in self.current_incident:
            return 0

        start_time = datetime.fromisoformat(self.current_incident['started_at'])
        return round((datetime.now() - start_time).total_seconds() / 60, 2)

    def _get_recent_incidents(self):
        """Get list of recent incidents"""
        incidents = []
        for file in sorted(self.incident_dir.glob('INC*.json'))[-5:]:
            with open(file, 'r') as f:
                data = json.load(f)
                incidents.append({
                    'incident_id': data.get('incident_id'),
                    'title': data.get('title'),
                    'severity': data.get('severity'),
                    'status': data.get('status')
                })
        return incidents

def main():
    """
    Entry point when Claude runs this script

    Usage:
        python 05_troubleshoot.py start "Issue title" --severity critical --components "api,database"
        python 05_troubleshoot.py diagnose
        python 05_troubleshoot.py hypothesis "Database connection pool exhausted" --reasoning "High connection count"
        python 05_troubleshoot.py test H01 "Increased pool size" confirmed
        python 05_troubleshoot.py root-cause "Connection leak in API handler"
        python 05_troubleshoot.py resolve "Fixed connection cleanup" --steps "Added finally block|Updated connection manager"
        python 05_troubleshoot.py verify --metrics-restored true --errors-resolved true
        python 05_troubleshoot.py complete --lessons "Need connection monitoring|Add pool metrics"
        python 05_troubleshoot.py status
    """
    troubleshooter = Troubleshooter()

    # Parse command line arguments
    args = sys.argv[1:]

    if not args:
        # Return usage information
        usage = {
            'script': '05_troubleshoot.py',
            'description': 'Incident troubleshooting and debugging tool',
            'commands': [
                {
                    'command': 'start "title" --severity [critical|high|medium|low] --components "list"',
                    'description': 'Start new incident'
                },
                {
                    'command': 'diagnose',
                    'description': 'Collect diagnostics'
                },
                {
                    'command': 'hypothesis "statement" --reasoning "reason"',
                    'description': 'Add hypothesis'
                },
                {
                    'command': 'test [hypothesis_id] "test description" [confirmed|refuted|inconclusive]',
                    'description': 'Test hypothesis'
                },
                {
                    'command': 'root-cause "description"',
                    'description': 'Identify root cause'
                },
                {
                    'command': 'resolve "description" --steps "step1|step2"',
                    'description': 'Implement resolution'
                },
                {
                    'command': 'verify --metrics-restored [true|false] --errors-resolved [true|false]',
                    'description': 'Verify resolution'
                },
                {
                    'command': 'complete --lessons "lesson1|lesson2"',
                    'description': 'Complete incident'
                },
                {
                    'command': 'status',
                    'description': 'Get incident status'
                }
            ]
        }
        print(json.dumps(usage, indent=2))
        return

    command = args[0]

    if command == 'start':
        title = args[1] if len(args) > 1 else 'Unknown Issue'
        severity = 'medium'
        components = []

        # Parse optional arguments
        if '--severity' in args:
            idx = args.index('--severity')
            if idx + 1 < len(args):
                severity = args[idx + 1]

        if '--components' in args:
            idx = args.index('--components')
            if idx + 1 < len(args):
                components = args[idx + 1].split(',')

        result = troubleshooter.start_incident(title, severity, components)
        print(json.dumps(result, indent=2))

    elif command == 'diagnose':
        result = troubleshooter.collect_diagnostics()
        print(json.dumps(result, indent=2))

    elif command == 'hypothesis':
        statement = args[1] if len(args) > 1 else 'Unknown hypothesis'
        reasoning = None

        if '--reasoning' in args:
            idx = args.index('--reasoning')
            if idx + 1 < len(args):
                reasoning = args[idx + 1]

        result = troubleshooter.add_hypothesis(statement, reasoning)
        print(json.dumps(result, indent=2))

    elif command == 'test':
        if len(args) < 4:
            print(json.dumps({'error': 'Usage: test [hypothesis_id] "test description" [result]'}, indent=2))
            return

        hypothesis_id = args[1]
        test_description = args[2]
        result = args[3]

        result = troubleshooter.test_hypothesis(hypothesis_id, test_description, result)
        print(json.dumps(result, indent=2))

    elif command == 'root-cause':
        description = args[1] if len(args) > 1 else 'Unknown root cause'
        result = troubleshooter.identify_root_cause(description)
        print(json.dumps(result, indent=2))

    elif command == 'resolve':
        description = args[1] if len(args) > 1 else 'Resolution implemented'
        steps = []

        if '--steps' in args:
            idx = args.index('--steps')
            if idx + 1 < len(args):
                steps = args[idx + 1].split('|')

        result = troubleshooter.implement_resolution(description, steps)
        print(json.dumps(result, indent=2))

    elif command == 'verify':
        checks = {}

        # Parse verification checks
        i = 1
        while i < len(args):
            if args[i].startswith('--'):
                key = args[i][2:].replace('-', '_')
                if i + 1 < len(args):
                    checks[key] = args[i + 1].lower() == 'true'
                    i += 2
                else:
                    i += 1
            else:
                i += 1

        result = troubleshooter.verify_resolution(checks)
        print(json.dumps(result, indent=2))

    elif command == 'complete':
        lessons = []

        if '--lessons' in args:
            idx = args.index('--lessons')
            if idx + 1 < len(args):
                lessons = args[idx + 1].split('|')

        result = troubleshooter.complete_incident(lessons)
        print(json.dumps(result, indent=2))

    elif command == 'status':
        result = troubleshooter.get_status()
        print(json.dumps(result, indent=2))

    else:
        print(json.dumps({'error': f'Unknown command: {command}'}, indent=2))

if __name__ == "__main__":
    main()