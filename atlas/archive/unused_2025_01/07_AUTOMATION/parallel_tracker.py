#!/usr/bin/env python3
"""
Atlas Parallel Execution Tracker - Monitor and optimize parallel agent execution
Helps orchestrator track parallel work and identify optimization opportunities
"""

import sys
import json
from pathlib import Path
from datetime import datetime
from collections import defaultdict

class ParallelTracker:
    """
    Tracks parallel agent execution and provides optimization insights
    """

    def __init__(self):
        self.orchestrator_dir = Path('.atlas/orchestrator')
        self.workflow_dir = Path('.atlas/workflow')
        self.context_file = self.orchestrator_dir / 'context.json'

    def track_parallel_spawn(self, agents):
        """
        Track a batch of parallel agent spawns

        Args:
            agents: List of agent dictionaries with type and mission

        Returns:
            Tracking confirmation with batch ID
        """
        batch_id = f"BATCH_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        tracking_file = self.orchestrator_dir / f"parallel_{batch_id}.json"

        batch_data = {
            'batch_id': batch_id,
            'spawned_at': datetime.now().isoformat(),
            'agent_count': len(agents),
            'agents': agents,
            'status': 'running',
            'optimization_score': self._calculate_optimization_score(agents)
        }

        with open(tracking_file, 'w') as f:
            json.dump(batch_data, f, indent=2)

        return {
            'action': 'track_parallel_spawn',
            'batch_id': batch_id,
            'agents_spawned': len(agents),
            'parallel_execution': True,
            'optimization_score': batch_data['optimization_score']
        }

    def _calculate_optimization_score(self, agents):
        """Calculate how well optimized the parallel execution is"""
        score = 100

        # Penalize if too few agents (not using parallelism)
        if len(agents) < 3:
            score -= 30

        # Penalize if too many agents (coordination overhead)
        if len(agents) > 8:
            score -= 20

        # Check for good task distribution
        agent_types = [a.get('type', '') for a in agents]
        unique_types = len(set(agent_types))

        # Reward diversity of agent types
        if unique_types >= 3:
            score += 10

        # Check for duplicate missions (might be redundant)
        missions = [a.get('mission', '') for a in agents]
        if len(missions) != len(set(missions)):
            score -= 15  # Duplicate work detected

        return max(0, min(100, score))

    def suggest_parallelization(self, current_task):
        """
        Suggest how to parallelize a task

        Args:
            current_task: Description of the task to parallelize

        Returns:
            Parallelization suggestions
        """
        suggestions = {
            'task': current_task,
            'parallel_opportunities': [],
            'recommended_agents': []
        }

        # Common parallelization patterns
        if 'research' in current_task.lower():
            suggestions['parallel_opportunities'] = [
                'Research technical implementation patterns',
                'Research similar projects/libraries',
                'Research best practices',
                'Research performance considerations',
                'Research security implications'
            ]
            suggestions['recommended_agents'] = 5

        elif 'implement' in current_task.lower() or 'build' in current_task.lower():
            suggestions['parallel_opportunities'] = [
                'Build data models/database layer',
                'Create UI components/layouts',
                'Implement business logic',
                'Setup configuration/dependencies',
                'Write initial tests'
            ]
            suggestions['recommended_agents'] = 4

        elif 'test' in current_task.lower():
            suggestions['parallel_opportunities'] = [
                'Run unit tests',
                'Execute integration tests',
                'Perform UI/E2E tests',
                'Check performance metrics',
                'Validate security'
            ]
            suggestions['recommended_agents'] = 3

        elif 'review' in current_task.lower():
            suggestions['parallel_opportunities'] = [
                'Code quality review',
                'Security review',
                'Performance review',
                'Documentation review'
            ]
            suggestions['recommended_agents'] = 3

        else:
            # Generic parallelization
            suggestions['parallel_opportunities'] = [
                'Analyze requirements',
                'Design solution',
                'Implement core functionality',
                'Write tests',
                'Update documentation'
            ]
            suggestions['recommended_agents'] = 3

        suggestions['speedup_estimate'] = f"{suggestions['recommended_agents']}x faster than sequential"

        return suggestions

    def get_parallel_status(self):
        """
        Get status of all parallel executions

        Returns:
            Current parallel execution status
        """
        parallel_files = list(self.orchestrator_dir.glob('parallel_*.json'))

        if not parallel_files:
            return {
                'status': 'no_parallel_executions',
                'suggestion': 'Use parallel execution to speed up development'
            }

        # Load all parallel batches
        batches = []
        for file in sorted(parallel_files)[-10:]:  # Last 10 batches
            with open(file, 'r') as f:
                batches.append(json.load(f))

        # Calculate statistics
        total_agents = sum(b['agent_count'] for b in batches)
        avg_agents_per_batch = total_agents / len(batches) if batches else 0
        avg_optimization_score = sum(b.get('optimization_score', 0) for b in batches) / len(batches) if batches else 0

        # Get current running batch
        running_batches = [b for b in batches if b.get('status') == 'running']

        return {
            'action': 'get_parallel_status',
            'total_batches': len(batches),
            'total_agents_spawned': total_agents,
            'average_agents_per_batch': round(avg_agents_per_batch, 1),
            'average_optimization_score': round(avg_optimization_score, 1),
            'currently_running': len(running_batches),
            'recent_batches': [
                {
                    'batch_id': b['batch_id'],
                    'agents': b['agent_count'],
                    'optimization_score': b.get('optimization_score', 0)
                }
                for b in batches[-5:]
            ],
            'optimization_tips': self._get_optimization_tips(avg_agents_per_batch, avg_optimization_score)
        }

    def _get_optimization_tips(self, avg_agents, avg_score):
        """Generate optimization tips based on current usage"""
        tips = []

        if avg_agents < 3:
            tips.append("🔴 Increase parallel execution - spawn 3-5 agents for better speed")
        elif avg_agents > 8:
            tips.append("🟡 Too many parallel agents - reduce to 5-6 for better coordination")
        else:
            tips.append("✅ Good parallel execution balance")

        if avg_score < 50:
            tips.append("🔴 Low optimization score - check for duplicate work")
        elif avg_score < 75:
            tips.append("🟡 Room for optimization - diversify agent types")
        else:
            tips.append("✅ Well-optimized parallel execution")

        return tips

    def generate_parallel_report(self):
        """
        Generate detailed parallel execution report

        Returns:
            Formatted report of parallel execution efficiency
        """
        output = []
        output.append("=" * 80)
        output.append("              PARALLEL EXECUTION REPORT")
        output.append("=" * 80)
        output.append("")

        status = self.get_parallel_status()

        if status.get('status') == 'no_parallel_executions':
            output.append("⚠️  No parallel executions tracked yet")
            output.append("")
            output.append("Start using parallel execution:")
            output.append("1. Spawn multiple research agents simultaneously")
            output.append("2. Run tests in parallel")
            output.append("3. Build independent components concurrently")
            return "\n".join(output)

        # Summary stats
        output.append("📊 SUMMARY")
        output.append("-" * 40)
        output.append(f"Total Batches: {status['total_batches']}")
        output.append(f"Total Agents Spawned: {status['total_agents_spawned']}")
        output.append(f"Avg Agents per Batch: {status['average_agents_per_batch']}")
        output.append(f"Optimization Score: {status['average_optimization_score']}/100")
        output.append("")

        # Recent batches
        output.append("🚀 RECENT PARALLEL BATCHES")
        output.append("-" * 40)
        for batch in status.get('recent_batches', []):
            score_icon = "🟢" if batch['optimization_score'] > 75 else "🟡" if batch['optimization_score'] > 50 else "🔴"
            output.append(f"{score_icon} {batch['batch_id']}: {batch['agents']} agents (score: {batch['optimization_score']})")
        output.append("")

        # Optimization tips
        output.append("💡 OPTIMIZATION TIPS")
        output.append("-" * 40)
        for tip in status.get('optimization_tips', []):
            output.append(tip)
        output.append("")

        # Speed comparison
        output.append("⚡ SPEED COMPARISON")
        output.append("-" * 40)
        avg_agents = status['average_agents_per_batch']
        output.append(f"Sequential execution: 1x speed (baseline)")
        output.append(f"Your parallel execution: ~{avg_agents:.1f}x speed")
        output.append(f"Optimal (5 agents): ~4x speed")
        output.append("")

        # Recommendations
        output.append("📋 RECOMMENDATIONS")
        output.append("-" * 40)
        output.append("• Always spawn 3-5 agents when possible")
        output.append("• Ensure agents have different tasks (no duplicates)")
        output.append("• Mix agent types (research, build, test)")
        output.append("• Monitor optimization scores")

        output.append("\n" + "=" * 80)
        return "\n".join(output)

def main():
    """
    Entry point for parallel tracker

    Usage:
        python3 parallel_tracker.py status                    # Show parallel execution status
        python3 parallel_tracker.py report                    # Generate detailed report
        python3 parallel_tracker.py suggest "task"            # Get parallelization suggestions
        python3 parallel_tracker.py track '[agents_json]'     # Track parallel spawn
    """
    tracker = ParallelTracker()

    args = sys.argv[1:]

    if not args or args[0] == 'status':
        result = tracker.get_parallel_status()
        print(json.dumps(result, indent=2))

    elif args[0] == 'report':
        print(tracker.generate_parallel_report())

    elif args[0] == 'suggest' and len(args) > 1:
        task = ' '.join(args[1:])
        suggestions = tracker.suggest_parallelization(task)
        print(json.dumps(suggestions, indent=2))

    elif args[0] == 'track' and len(args) > 1:
        try:
            agents = json.loads(args[1])
            result = tracker.track_parallel_spawn(agents)
            print(json.dumps(result, indent=2))
        except json.JSONDecodeError:
            print(json.dumps({'error': 'Invalid JSON for agents list'}, indent=2))

    elif args[0] == '--help':
        print("""
Atlas Parallel Execution Tracker

Commands:
  parallel_tracker.py status          Show parallel execution status
  parallel_tracker.py report          Generate efficiency report
  parallel_tracker.py suggest "task"  Get parallelization suggestions
  parallel_tracker.py track '[json]'  Track parallel agent spawn

Examples:
  python3 parallel_tracker.py suggest "implement user authentication"
  python3 parallel_tracker.py report
  python3 parallel_tracker.py track '[{"type":"researcher","mission":"research patterns"}]'

The tracker helps optimize parallel agent execution for maximum speed.
""")
    else:
        print(f"Unknown command: {args[0]}")
        print("Use --help for usage")

if __name__ == "__main__":
    main()