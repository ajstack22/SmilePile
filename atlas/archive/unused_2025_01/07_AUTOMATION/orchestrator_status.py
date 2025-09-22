#!/usr/bin/env python3
"""
Atlas Orchestrator Status Display - Real-time progress visualization
Shows what's happening during parallel agent execution with progress bars
"""

import sys
import json
from pathlib import Path
from datetime import datetime, timedelta
from collections import defaultdict
import time

class OrchestratorStatus:
    """
    Provides real-time status display for orchestrator activities
    """

    def __init__(self):
        self.status_file = Path('.atlas/orchestrator/current_status.json')
        self.status_file.parent.mkdir(parents=True, exist_ok=True)
        self.start_time = datetime.now()

    def start_operation(self, operation_name, total_steps=None, agents=None):
        """
        Initialize a new operation with progress tracking

        Args:
            operation_name: What the orchestrator is doing
            total_steps: Total number of steps (for progress bar)
            agents: List of agents being spawned

        Returns:
            Status display
        """
        status = {
            'operation': operation_name,
            'status': 'in_progress',
            'started_at': datetime.now().isoformat(),
            'total_steps': total_steps or 0,
            'completed_steps': 0,
            'current_phase': 'initializing',
            'agents': agents or [],
            'agent_statuses': {},
            'messages': []
        }

        # Initialize agent statuses
        if agents:
            for agent in agents:
                status['agent_statuses'][agent.get('id', f'agent_{len(status["agent_statuses"])}')] = {
                    'role': agent.get('role', 'unknown'),
                    'task': agent.get('task', ''),
                    'status': 'pending',
                    'progress': 0
                }

        self._save_status(status)
        return self._format_display(status)

    def update_progress(self, completed_steps=None, current_phase=None, message=None):
        """
        Update operation progress

        Args:
            completed_steps: Number of steps completed
            current_phase: Current phase description
            message: Status message to display

        Returns:
            Updated status display
        """
        status = self._load_status()

        if completed_steps is not None:
            status['completed_steps'] = completed_steps

        if current_phase:
            status['current_phase'] = current_phase

        if message:
            status['messages'].append({
                'time': datetime.now().isoformat(),
                'text': message
            })
            # Keep only last 5 messages
            status['messages'] = status['messages'][-5:]

        self._save_status(status)
        return self._format_display(status)

    def update_agent_status(self, agent_id, agent_status='running', progress=None):
        """
        Update individual agent status

        Args:
            agent_id: Agent identifier
            agent_status: Status (pending, running, completed, failed)
            progress: Progress percentage (0-100)

        Returns:
            Updated display
        """
        status = self._load_status()

        if agent_id in status['agent_statuses']:
            status['agent_statuses'][agent_id]['status'] = agent_status
            if progress is not None:
                status['agent_statuses'][agent_id]['progress'] = progress

            # Update completed steps based on agent completions
            completed_agents = sum(1 for a in status['agent_statuses'].values()
                                 if a['status'] == 'completed')
            total_agents = len(status['agent_statuses'])

            if total_agents > 0:
                status['completed_steps'] = int((completed_agents / total_agents) * (status['total_steps'] or 100))

        self._save_status(status)
        return self._format_display(status)

    def show_wave_progress(self, wave_number, wave_total, wave_agents):
        """
        Display progress for wave-based execution

        Args:
            wave_number: Current wave number
            wave_total: Total number of waves
            wave_agents: Agents in current wave

        Returns:
            Wave progress display
        """
        status = self._load_status()

        status['wave_info'] = {
            'current': wave_number,
            'total': wave_total,
            'agents_in_wave': len(wave_agents)
        }

        status['current_phase'] = f"Wave {wave_number}/{wave_total}"

        # Calculate overall progress based on waves
        wave_progress = ((wave_number - 1) / wave_total) * 100
        status['wave_progress'] = wave_progress

        self._save_status(status)
        return self._format_display(status)

    def _format_display(self, status):
        """
        Format status as a beautiful text display

        Returns:
            Formatted status string
        """
        lines = []

        # Header
        lines.append("=" * 80)
        lines.append(f"🎯 ORCHESTRATOR STATUS: {status.get('operation', 'Unknown Operation')}")
        lines.append("=" * 80)

        # Time elapsed
        if status.get('started_at'):
            start = datetime.fromisoformat(status['started_at'])
            elapsed = datetime.now() - start
            lines.append(f"⏱️  Elapsed: {self._format_duration(elapsed)}")

        # Current phase
        phase = status.get('current_phase', 'Unknown')
        lines.append(f"📍 Phase: {phase}")
        lines.append("")

        # Main progress bar
        if status.get('total_steps'):
            progress = self._create_progress_bar(
                status.get('completed_steps', 0),
                status['total_steps'],
                width=50
            )
            percentage = (status.get('completed_steps', 0) / status['total_steps']) * 100
            lines.append(f"Overall Progress: {progress} {percentage:.1f}%")
            lines.append("")

        # Wave information
        if 'wave_info' in status:
            wave = status['wave_info']
            wave_progress = self._create_wave_indicator(wave['current'], wave['total'])
            lines.append(f"Execution Waves: {wave_progress}")
            lines.append(f"Agents in current wave: {wave['agents_in_wave']}")
            lines.append("")

        # Agent statuses
        if status.get('agent_statuses'):
            lines.append("🤖 AGENT STATUS:")
            lines.append("-" * 40)

            # Group by status
            by_status = defaultdict(list)
            for agent_id, agent_info in status['agent_statuses'].items():
                by_status[agent_info['status']].append((agent_id, agent_info))

            # Show running agents first
            if 'running' in by_status:
                for agent_id, info in by_status['running']:
                    progress_bar = self._create_mini_progress_bar(info.get('progress', 0))
                    lines.append(f"  🔄 {agent_id} ({info['role']}): {progress_bar} {info.get('progress', 0)}%")
                    lines.append(f"     Task: {info['task'][:50]}...")

            # Show completed agents
            if 'completed' in by_status:
                lines.append(f"  ✅ Completed: {len(by_status['completed'])} agents")

            # Show pending agents
            if 'pending' in by_status:
                lines.append(f"  ⏳ Pending: {len(by_status['pending'])} agents")

            # Show failed agents
            if 'failed' in by_status:
                for agent_id, info in by_status['failed']:
                    lines.append(f"  ❌ {agent_id} ({info['role']}): FAILED")

            lines.append("")

        # Recent messages
        if status.get('messages'):
            lines.append("📋 RECENT ACTIVITY:")
            lines.append("-" * 40)
            for msg in status['messages'][-3:]:  # Show last 3 messages
                time_str = datetime.fromisoformat(msg['time']).strftime('%H:%M:%S')
                lines.append(f"  [{time_str}] {msg['text']}")
            lines.append("")

        # Performance metrics
        if status.get('agent_statuses'):
            completed = sum(1 for a in status['agent_statuses'].values() if a['status'] == 'completed')
            total = len(status['agent_statuses'])
            running = sum(1 for a in status['agent_statuses'].values() if a['status'] == 'running')

            lines.append("📊 METRICS:")
            lines.append("-" * 40)
            lines.append(f"  Agents: {completed}/{total} completed, {running} running")

            # Calculate estimated time remaining
            if completed > 0 and status.get('started_at'):
                start = datetime.fromisoformat(status['started_at'])
                elapsed = datetime.now() - start
                avg_time = elapsed / completed
                remaining_agents = total - completed
                eta = avg_time * remaining_agents
                lines.append(f"  ETA: {self._format_duration(eta)}")

            # Show parallelization efficiency
            if running > 0:
                lines.append(f"  Parallelization: {running}x speedup")

        lines.append("")
        lines.append("=" * 80)

        return "\n".join(lines)

    def _create_progress_bar(self, current, total, width=50):
        """Create ASCII progress bar"""
        if total == 0:
            return "[" + "?" * width + "]"

        filled = int((current / total) * width)
        bar = "█" * filled + "░" * (width - filled)
        return f"[{bar}]"

    def _create_mini_progress_bar(self, percentage, width=20):
        """Create small progress bar for agents"""
        filled = int((percentage / 100) * width)
        bar = "▓" * filled + "░" * (width - filled)
        return f"[{bar}]"

    def _create_wave_indicator(self, current, total):
        """Create wave progress indicator"""
        if total == 0:
            return "No waves"

        indicators = []
        for i in range(1, total + 1):
            if i < current:
                indicators.append("✅")
            elif i == current:
                indicators.append("🔄")
            else:
                indicators.append("⭕")

        return f"[{' → '.join(indicators)}]"

    def _format_duration(self, duration):
        """Format timedelta as human-readable string"""
        total_seconds = int(duration.total_seconds())
        hours = total_seconds // 3600
        minutes = (total_seconds % 3600) // 60
        seconds = total_seconds % 60

        if hours > 0:
            return f"{hours}h {minutes}m {seconds}s"
        elif minutes > 0:
            return f"{minutes}m {seconds}s"
        else:
            return f"{seconds}s"

    def _save_status(self, status):
        """Save status to file"""
        with open(self.status_file, 'w') as f:
            json.dump(status, f, indent=2)

    def _load_status(self):
        """Load status from file"""
        if self.status_file.exists():
            with open(self.status_file, 'r') as f:
                return json.load(f)
        return {}

    def complete_operation(self, summary=None):
        """
        Mark operation as complete

        Args:
            summary: Final summary message

        Returns:
            Completion display
        """
        status = self._load_status()
        status['status'] = 'completed'
        status['completed_at'] = datetime.now().isoformat()
        status['completed_steps'] = status.get('total_steps', 0)

        if summary:
            status['summary'] = summary

        # Mark all agents as completed
        for agent_id in status.get('agent_statuses', {}):
            if status['agent_statuses'][agent_id]['status'] != 'failed':
                status['agent_statuses'][agent_id]['status'] = 'completed'
                status['agent_statuses'][agent_id]['progress'] = 100

        self._save_status(status)

        # Create completion display
        lines = []
        lines.append("=" * 80)
        lines.append("✅ OPERATION COMPLETE!")
        lines.append("=" * 80)

        if status.get('started_at'):
            start = datetime.fromisoformat(status['started_at'])
            total_time = datetime.now() - start
            lines.append(f"Total time: {self._format_duration(total_time)}")

        if status.get('agent_statuses'):
            total_agents = len(status['agent_statuses'])
            completed = sum(1 for a in status['agent_statuses'].values() if a['status'] == 'completed')
            failed = sum(1 for a in status['agent_statuses'].values() if a['status'] == 'failed')

            lines.append(f"Agents: {completed}/{total_agents} succeeded, {failed} failed")

        if summary:
            lines.append(f"\nSummary: {summary}")

        lines.append("=" * 80)

        return "\n".join(lines)

def main():
    """
    Entry point for status display

    Usage:
        python3 orchestrator_status.py start "operation" --steps 10 --agents '[...]'
        python3 orchestrator_status.py update --phase "Building" --message "Created auth module"
        python3 orchestrator_status.py agent AGT001 running --progress 50
        python3 orchestrator_status.py wave 2 5 '[agents]'
        python3 orchestrator_status.py complete --summary "All tasks finished"
        python3 orchestrator_status.py show  # Display current status
    """
    status_manager = OrchestratorStatus()

    args = sys.argv[1:]

    if not args:
        usage = {
            'script': 'orchestrator_status.py',
            'description': 'Real-time orchestrator status display',
            'commands': [
                {
                    'command': 'start "operation" --steps N --agents \'[...]\'',
                    'description': 'Start new operation tracking'
                },
                {
                    'command': 'update --phase "phase" --message "msg"',
                    'description': 'Update operation progress'
                },
                {
                    'command': 'agent [id] [status] --progress N',
                    'description': 'Update agent status'
                },
                {
                    'command': 'wave [current] [total] \'[agents]\'',
                    'description': 'Show wave progress'
                },
                {
                    'command': 'complete --summary "msg"',
                    'description': 'Mark operation complete'
                },
                {
                    'command': 'show',
                    'description': 'Display current status'
                }
            ]
        }
        print(json.dumps(usage, indent=2))
        return

    command = args[0]

    if command == 'start':
        operation = args[1] if len(args) > 1 else "Unknown Operation"
        steps = None
        agents = None

        for i in range(2, len(args)):
            if args[i] == '--steps' and i + 1 < len(args):
                steps = int(args[i + 1])
            elif args[i] == '--agents' and i + 1 < len(args):
                agents = json.loads(args[i + 1])

        display = status_manager.start_operation(operation, steps, agents)
        print(display)

    elif command == 'update':
        phase = None
        message = None
        completed = None

        for i in range(1, len(args)):
            if args[i] == '--phase' and i + 1 < len(args):
                phase = args[i + 1]
            elif args[i] == '--message' and i + 1 < len(args):
                message = args[i + 1]
            elif args[i] == '--completed' and i + 1 < len(args):
                completed = int(args[i + 1])

        display = status_manager.update_progress(completed, phase, message)
        print(display)

    elif command == 'agent' and len(args) >= 3:
        agent_id = args[1]
        agent_status = args[2]
        progress = None

        for i in range(3, len(args)):
            if args[i] == '--progress' and i + 1 < len(args):
                progress = int(args[i + 1])

        display = status_manager.update_agent_status(agent_id, agent_status, progress)
        print(display)

    elif command == 'wave' and len(args) >= 4:
        current = int(args[1])
        total = int(args[2])
        agents = json.loads(args[3])
        display = status_manager.show_wave_progress(current, total, agents)
        print(display)

    elif command == 'complete':
        summary = None
        for i in range(1, len(args)):
            if args[i] == '--summary' and i + 1 < len(args):
                summary = args[i + 1]

        display = status_manager.complete_operation(summary)
        print(display)

    elif command == 'show':
        status = status_manager._load_status()
        if status:
            print(status_manager._format_display(status))
        else:
            print("No active operation")

    else:
        print(f"Unknown command: {command}")

if __name__ == "__main__":
    main()