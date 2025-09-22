#!/usr/bin/env python3
"""
Atlas Task Aggregator - Intelligently combines results from parallel agents
Reduces orchestrator's work in synthesizing multiple agent outputs
"""

import sys
import json
from pathlib import Path
from datetime import datetime
from collections import defaultdict

class TaskAggregator:
    """
    Aggregates and synthesizes results from parallel agent execution
    """

    def __init__(self):
        self.results_dir = Path('.atlas/agent_results')
        self.results_dir.mkdir(parents=True, exist_ok=True)

    def store_agent_result(self, agent_id, role, task, result):
        """
        Store individual agent result for later aggregation

        Args:
            agent_id: Unique agent identifier
            role: Agent role (researcher, developer, etc.)
            task: Task description
            result: Agent's output

        Returns:
            Storage confirmation
        """
        timestamp = datetime.now().isoformat()
        result_file = self.results_dir / f"{agent_id}_{timestamp.replace(':', '-')}.json"

        result_data = {
            'agent_id': agent_id,
            'role': role,
            'task': task,
            'result': result,
            'timestamp': timestamp
        }

        with open(result_file, 'w') as f:
            json.dump(result_data, f, indent=2)

        return {
            'action': 'store_result',
            'agent_id': agent_id,
            'file': str(result_file)
        }

    def aggregate_research_results(self, agent_ids):
        """
        Combine research findings from multiple researcher agents

        Returns:
            Unified research summary with:
            - Common patterns found by multiple agents
            - Unique insights from each agent
            - Confidence scores based on agreement
        """
        results = self._load_results(agent_ids)

        aggregated = {
            'total_agents': len(results),
            'common_findings': {},
            'unique_findings': {},
            'recommendations': [],
            'confidence_scores': {}
        }

        # Extract findings from each agent
        all_findings = defaultdict(list)
        for r in results:
            if r['role'] != 'researcher':
                continue

            agent_findings = r.get('result', {})
            if isinstance(agent_findings, dict):
                for key, value in agent_findings.items():
                    all_findings[key].append({
                        'agent': r['agent_id'],
                        'value': value
                    })

        # Identify common vs unique findings
        for key, values in all_findings.items():
            if len(values) > 1:
                # Multiple agents found this
                aggregated['common_findings'][key] = values
                aggregated['confidence_scores'][key] = len(values) / len(results)
            else:
                # Only one agent found this
                aggregated['unique_findings'][key] = values[0]
                aggregated['confidence_scores'][key] = 1 / len(results)

        # Generate recommendations based on confidence
        for finding, confidence in aggregated['confidence_scores'].items():
            if confidence > 0.66:
                aggregated['recommendations'].append(f"HIGH confidence: {finding}")
            elif confidence > 0.33:
                aggregated['recommendations'].append(f"MEDIUM confidence: {finding}")
            else:
                aggregated['recommendations'].append(f"LOW confidence: {finding} (verify further)")

        return aggregated

    def aggregate_test_results(self, agent_ids):
        """
        Combine test results from multiple testing agents

        Returns:
            Unified test report with:
            - Overall pass/fail status
            - Coverage metrics
            - Failed tests needing attention
        """
        results = self._load_results(agent_ids)

        aggregated = {
            'total_test_suites': len(results),
            'overall_status': 'passing',
            'total_tests': 0,
            'passed': 0,
            'failed': 0,
            'coverage': [],
            'failures': []
        }

        for r in results:
            if r['role'] != 'tester':
                continue

            test_result = r.get('result', {})
            if isinstance(test_result, dict):
                aggregated['total_tests'] += test_result.get('total', 0)
                aggregated['passed'] += test_result.get('passed', 0)
                aggregated['failed'] += test_result.get('failed', 0)

                if test_result.get('coverage'):
                    aggregated['coverage'].append(test_result['coverage'])

                if test_result.get('failures'):
                    aggregated['failures'].extend(test_result['failures'])

        # Set overall status
        if aggregated['failed'] > 0:
            aggregated['overall_status'] = 'failing'

        # Calculate average coverage
        if aggregated['coverage']:
            aggregated['average_coverage'] = sum(aggregated['coverage']) / len(aggregated['coverage'])

        return aggregated

    def aggregate_development_results(self, agent_ids):
        """
        Combine development work from multiple developer agents

        Returns:
            Summary of:
            - Files created/modified
            - Components built
            - Integration points needing attention
        """
        results = self._load_results(agent_ids)

        aggregated = {
            'total_developers': len(results),
            'files_created': [],
            'files_modified': [],
            'components': [],
            'integration_needed': [],
            'potential_conflicts': []
        }

        modified_files = defaultdict(list)

        for r in results:
            if r['role'] != 'developer':
                continue

            dev_result = r.get('result', {})
            if isinstance(dev_result, dict):
                # Track file operations
                for file in dev_result.get('created', []):
                    aggregated['files_created'].append({
                        'file': file,
                        'agent': r['agent_id']
                    })

                for file in dev_result.get('modified', []):
                    modified_files[file].append(r['agent_id'])
                    aggregated['files_modified'].append({
                        'file': file,
                        'agent': r['agent_id']
                    })

                # Track components
                if dev_result.get('component'):
                    aggregated['components'].append({
                        'name': dev_result['component'],
                        'agent': r['agent_id']
                    })

        # Check for potential conflicts
        for file, agents in modified_files.items():
            if len(agents) > 1:
                aggregated['potential_conflicts'].append({
                    'file': file,
                    'modified_by': agents,
                    'severity': 'high'
                })

        return aggregated

    def create_execution_summary(self, batch_id):
        """
        Create a complete summary of a parallel execution batch

        Returns:
            Executive summary for orchestrator
        """
        batch_results = list(self.results_dir.glob(f"*{batch_id}*.json"))

        summary = {
            'batch_id': batch_id,
            'agent_count': len(batch_results),
            'timestamp': datetime.now().isoformat(),
            'by_role': defaultdict(int),
            'status': 'complete',
            'key_findings': [],
            'next_actions': []
        }

        # Categorize by role
        for result_file in batch_results:
            with open(result_file, 'r') as f:
                data = json.load(f)
                summary['by_role'][data['role']] += 1

        # Generate next actions based on roles
        if summary['by_role']['researcher'] > 0:
            summary['next_actions'].append("Review research findings and make implementation decisions")

        if summary['by_role']['developer'] > 0:
            summary['next_actions'].append("Run integration tests on new components")

        if summary['by_role']['tester'] > 0:
            summary['next_actions'].append("Address failing tests before proceeding")

        return summary

    def _load_results(self, agent_ids):
        """Load results for specific agents"""
        results = []
        for agent_id in agent_ids:
            # Find the most recent result for this agent
            agent_files = list(self.results_dir.glob(f"{agent_id}_*.json"))
            if agent_files:
                latest = max(agent_files, key=lambda f: f.stat().st_mtime)
                with open(latest, 'r') as f:
                    results.append(json.load(f))
        return results

    def suggest_next_parallel_batch(self, completed_roles):
        """
        Suggest what agents to spawn next based on what just completed

        Args:
            completed_roles: List of roles that just finished

        Returns:
            Suggested next batch of parallel agents
        """
        suggestions = []

        # Define role progression patterns
        progressions = {
            'researcher': ['developer', 'tester'],
            'developer': ['tester', 'reviewer'],
            'tester': ['debugger', 'documenter'],
            'reviewer': ['developer'],  # Fix issues found
            'debugger': ['developer', 'tester'],
            'documenter': []  # Usually final step
        }

        next_roles = set()
        for role in completed_roles:
            next_roles.update(progressions.get(role, []))

        # Create suggested batch
        role_tasks = {
            'developer': "Implement based on research findings",
            'tester': "Write and run tests for new code",
            'reviewer': "Review code for quality and standards",
            'debugger': "Debug failing tests",
            'documenter': "Update documentation"
        }

        for role in next_roles:
            suggestions.append({
                'role': role,
                'task': role_tasks.get(role, f"Perform {role} tasks"),
                'parallel': True
            })

        return {
            'suggested_batch': suggestions,
            'batch_size': len(suggestions),
            'estimated_speedup': f"{min(len(suggestions), 5)}x"
        }

def main():
    """
    Entry point for task aggregation

    Usage:
        python3 task_aggregator.py store [agent_id] [role] "task" 'result_json'
        python3 task_aggregator.py aggregate research [agent_ids]
        python3 task_aggregator.py aggregate tests [agent_ids]
        python3 task_aggregator.py summary [batch_id]
        python3 task_aggregator.py suggest [completed_roles]
    """
    aggregator = TaskAggregator()

    args = sys.argv[1:]

    if not args:
        usage = {
            'script': 'task_aggregator.py',
            'description': 'Aggregate results from parallel agents',
            'commands': [
                {
                    'command': 'store [agent_id] [role] "task" \'result\'',
                    'description': 'Store agent result'
                },
                {
                    'command': 'aggregate research [agent_ids]',
                    'description': 'Aggregate research findings'
                },
                {
                    'command': 'aggregate tests [agent_ids]',
                    'description': 'Aggregate test results'
                },
                {
                    'command': 'aggregate dev [agent_ids]',
                    'description': 'Aggregate development work'
                },
                {
                    'command': 'summary [batch_id]',
                    'description': 'Create batch summary'
                },
                {
                    'command': 'suggest [roles]',
                    'description': 'Suggest next parallel batch'
                }
            ]
        }
        print(json.dumps(usage, indent=2))
        return

    command = args[0]

    if command == 'store' and len(args) >= 5:
        agent_id = args[1]
        role = args[2]
        task = args[3]
        result = json.loads(args[4])
        output = aggregator.store_agent_result(agent_id, role, task, result)
        print(json.dumps(output, indent=2))

    elif command == 'aggregate' and len(args) >= 3:
        agg_type = args[1]
        agent_ids = args[2].split(',')

        if agg_type == 'research':
            output = aggregator.aggregate_research_results(agent_ids)
        elif agg_type == 'tests':
            output = aggregator.aggregate_test_results(agent_ids)
        elif agg_type == 'dev':
            output = aggregator.aggregate_development_results(agent_ids)
        else:
            output = {'error': f'Unknown aggregation type: {agg_type}'}

        print(json.dumps(output, indent=2))

    elif command == 'summary' and len(args) >= 2:
        batch_id = args[1]
        summary = aggregator.create_execution_summary(batch_id)
        print(json.dumps(summary, indent=2))

    elif command == 'suggest' and len(args) >= 2:
        roles = args[1].split(',')
        suggestions = aggregator.suggest_next_parallel_batch(roles)
        print(json.dumps(suggestions, indent=2))

    else:
        print(json.dumps({'error': f'Invalid command: {command}'}, indent=2))

if __name__ == "__main__":
    main()