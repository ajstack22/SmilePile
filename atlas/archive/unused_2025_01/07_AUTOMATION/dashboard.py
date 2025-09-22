#!/usr/bin/env python3
"""
Atlas Dashboard - Complete project visibility at a glance
Shows what's done, what's in progress, and what's next
"""

import sys
import json
from pathlib import Path
from datetime import datetime, timedelta
import subprocess

class ProjectDashboard:
    """
    Unified dashboard showing all project metrics and status
    """

    def __init__(self):
        self.orchestrator_dir = Path('.atlas/orchestrator')
        self.stories_metadata = Path('.atlas/stories_metadata.json')
        self.releases_dir = Path('.atlas/releases')

    def generate_dashboard(self):
        """
        Generate comprehensive project dashboard

        Returns:
            Complete project status dashboard
        """
        output = []
        output.append("=" * 100)
        output.append("                                 ATLAS PROJECT DASHBOARD")
        output.append("=" * 100)
        output.append(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        output.append("")

        # Get project info
        project_info = self._get_project_info()
        if project_info:
            output.append(f"🚀 PROJECT: {project_info.get('project_name', 'Unknown')}")
            output.append(f"📅 Started: {project_info.get('start_date', 'Unknown')[:10]}")
            output.append(f"🎯 Phase: {project_info.get('phase', 'Unknown')}")
            output.append("")

        # Get work items status
        work_status = self._get_work_status()
        if work_status:
            output.append("📊 WORK STATUS")
            output.append("-" * 50)

            # Progress bar
            total = work_status['total']
            done = work_status['done']
            in_progress = work_status['in_progress']
            blocked = work_status['blocked']

            if total > 0:
                progress = (done / total) * 100
                bar = self._create_progress_bar(progress)
                output.append(f"Progress: {bar} {done}/{total} ({progress:.1f}%)")

            output.append(f"🚧 In Progress: {in_progress}  |  ✅ Done: {done}  |  🚫 Blocked: {blocked}")
            output.append("")

        # Current sprint/iteration
        output.append("🏃 CURRENT SPRINT")
        output.append("-" * 50)
        current_items = self._get_current_sprint_items()
        if current_items:
            for item in current_items[:5]:  # Top 5 items
                status_icon = {
                    'in_progress': '🚧',
                    'in_review': '👀',
                    'testing': '🧪',
                    'blocked': '🚫'
                }.get(item.get('status'), '📋')
                output.append(f"{status_icon} [{item['id']}] {item['title'][:60]}")
        else:
            output.append("No items in current sprint")
        output.append("")

        # Recent completions
        output.append("✅ RECENT COMPLETIONS")
        output.append("-" * 50)
        recent_done = self._get_recent_completions()
        if recent_done:
            for item in recent_done[:5]:
                output.append(f"✓ [{item['id']}] {item['title'][:60]}")
        else:
            output.append("No recent completions")
        output.append("")

        # Upcoming work
        output.append("📋 NEXT UP")
        output.append("-" * 50)
        upcoming = self._get_upcoming_items()
        if upcoming:
            for item in upcoming[:5]:
                priority_icon = {
                    'critical': '🔴',
                    'high': '🟠',
                    'medium': '🟡',
                    'low': '🟢'
                }.get(item.get('priority', 'medium'), '')
                output.append(f"{priority_icon} [{item['id']}] {item['title'][:60]}")
        else:
            output.append("No items in backlog")
        output.append("")

        # Key metrics
        metrics = self._calculate_metrics()
        output.append("📈 KEY METRICS")
        output.append("-" * 50)
        output.append(f"Velocity: {metrics['velocity']} items/week")
        output.append(f"WIP Limit: {metrics['wip_current']}/{metrics['wip_recommended']}")
        output.append(f"Blocked Items: {metrics['blocked_count']}")
        output.append(f"Review Queue: {metrics['review_queue']}")
        output.append("")

        # Health indicators
        output.append("🏥 HEALTH CHECK")
        output.append("-" * 50)
        health = self._check_project_health()
        for indicator in health:
            output.append(f"{indicator['icon']} {indicator['message']}")
        output.append("")

        # Quick actions
        output.append("⚡ QUICK ACTIONS")
        output.append("-" * 50)
        output.append("• View kanban: python3 kanban.py")
        output.append("• View backlog: python3 kanban.py backlog")
        output.append("• Update status: python3 02_create_story.py update [ID] [status]")
        output.append("• Add story: python3 02_create_story.py story \"Title\"")
        output.append("• Start workflow: python3 03_adversarial_workflow.py start [ID]")

        output.append("\n" + "=" * 100)
        return "\n".join(output)

    def _get_project_info(self):
        """Get project information from orchestrator context"""
        context_file = self.orchestrator_dir / 'context.json'
        if context_file.exists():
            with open(context_file, 'r') as f:
                return json.load(f)
        return None

    def _get_work_status(self):
        """Get work items status summary"""
        if not self.stories_metadata.exists():
            return None

        with open(self.stories_metadata, 'r') as f:
            metadata = json.load(f)

        all_items = (metadata.get('stories', []) +
                    metadata.get('bugs', []) +
                    metadata.get('epics', []))

        status = {
            'total': len(all_items),
            'done': len([i for i in all_items if i.get('status') == 'done']),
            'in_progress': len([i for i in all_items if i.get('status') == 'in_progress']),
            'blocked': len([i for i in all_items if i.get('status') == 'blocked']),
            'ready': len([i for i in all_items if i.get('status') == 'ready']),
            'backlog': len([i for i in all_items if i.get('status') == 'backlog'])
        }

        return status

    def _get_current_sprint_items(self):
        """Get items currently being worked on"""
        if not self.stories_metadata.exists():
            return []

        with open(self.stories_metadata, 'r') as f:
            metadata = json.load(f)

        all_items = (metadata.get('stories', []) +
                    metadata.get('bugs', []))

        # Items in active statuses
        active_statuses = ['in_progress', 'in_review', 'testing', 'blocked']
        active_items = [i for i in all_items if i.get('status') in active_statuses]

        # Sort by priority
        priority_order = {'critical': 0, 'high': 1, 'medium': 2, 'low': 3}
        active_items.sort(key=lambda x: priority_order.get(x.get('priority', 'medium'), 2))

        return active_items

    def _get_recent_completions(self):
        """Get recently completed items"""
        if not self.stories_metadata.exists():
            return []

        with open(self.stories_metadata, 'r') as f:
            metadata = json.load(f)

        all_items = (metadata.get('stories', []) +
                    metadata.get('bugs', []))

        done_items = [i for i in all_items if i.get('status') == 'done']

        # Sort by completion date if available, otherwise by ID
        done_items.sort(key=lambda x: x.get('completed_at', x.get('id', '')), reverse=True)

        return done_items[:5]

    def _get_upcoming_items(self):
        """Get next items in backlog"""
        if not self.stories_metadata.exists():
            return []

        with open(self.stories_metadata, 'r') as f:
            metadata = json.load(f)

        all_items = (metadata.get('stories', []) +
                    metadata.get('bugs', []))

        # Items ready to work on
        ready_statuses = ['ready', 'backlog']
        ready_items = [i for i in all_items if i.get('status') in ready_statuses]

        # Sort by priority
        priority_order = {'critical': 0, 'high': 1, 'medium': 2, 'low': 3}
        ready_items.sort(key=lambda x: priority_order.get(x.get('priority', 'medium'), 2))

        return ready_items

    def _calculate_metrics(self):
        """Calculate key project metrics"""
        metrics = {
            'velocity': 0,
            'wip_current': 0,
            'wip_recommended': 3,
            'blocked_count': 0,
            'review_queue': 0
        }

        if self.stories_metadata.exists():
            with open(self.stories_metadata, 'r') as f:
                metadata = json.load(f)

            all_items = (metadata.get('stories', []) +
                        metadata.get('bugs', []))

            # Calculate metrics
            metrics['wip_current'] = len([i for i in all_items if i.get('status') == 'in_progress'])
            metrics['blocked_count'] = len([i for i in all_items if i.get('status') == 'blocked'])
            metrics['review_queue'] = len([i for i in all_items if i.get('status') == 'in_review'])

            # Simple velocity (items completed in last 7 days)
            done_items = [i for i in all_items if i.get('status') == 'done']
            metrics['velocity'] = len(done_items) // max(1, len(done_items) // 7) if done_items else 0

        return metrics

    def _check_project_health(self):
        """Check project health indicators"""
        health = []
        metrics = self._calculate_metrics()

        # WIP limit check
        if metrics['wip_current'] > metrics['wip_recommended']:
            health.append({
                'icon': '⚠️',
                'message': f"WIP limit exceeded ({metrics['wip_current']}/{metrics['wip_recommended']})"
            })
        else:
            health.append({
                'icon': '✅',
                'message': f"WIP limit healthy ({metrics['wip_current']}/{metrics['wip_recommended']})"
            })

        # Blocked items check
        if metrics['blocked_count'] > 2:
            health.append({
                'icon': '🚨',
                'message': f"{metrics['blocked_count']} items blocked - needs attention"
            })
        elif metrics['blocked_count'] > 0:
            health.append({
                'icon': '⚠️',
                'message': f"{metrics['blocked_count']} item(s) blocked"
            })

        # Review queue check
        if metrics['review_queue'] > 3:
            health.append({
                'icon': '⚠️',
                'message': f"Review queue building up ({metrics['review_queue']} items)"
            })

        # Velocity check
        if metrics['velocity'] == 0:
            health.append({
                'icon': '⚠️',
                'message': "No recent completions - check progress"
            })
        else:
            health.append({
                'icon': '✅',
                'message': f"Velocity: {metrics['velocity']} items/week"
            })

        return health

    def _create_progress_bar(self, percentage, width=40):
        """Create ASCII progress bar"""
        filled = int(width * percentage / 100)
        bar = '█' * filled + '░' * (width - filled)
        return f"[{bar}]"

    def generate_daily_standup(self):
        """
        Generate daily standup report

        Returns:
            Standup format report
        """
        output = []
        output.append("=" * 80)
        output.append("                       DAILY STANDUP REPORT")
        output.append("=" * 80)
        output.append(f"Date: {datetime.now().strftime('%Y-%m-%d')}")
        output.append("")

        # Yesterday's completions
        output.append("📅 YESTERDAY (Completed)")
        output.append("-" * 40)
        recent = self._get_recent_completions()
        if recent:
            for item in recent[:3]:
                output.append(f"✓ [{item['id']}] {item['title']}")
        else:
            output.append("No completions yesterday")
        output.append("")

        # Today's focus
        output.append("🎯 TODAY (In Progress)")
        output.append("-" * 40)
        current = self._get_current_sprint_items()
        in_progress = [i for i in current if i.get('status') == 'in_progress']
        if in_progress:
            for item in in_progress[:5]:
                output.append(f"• [{item['id']}] {item['title']}")
        else:
            output.append("No items in progress")
        output.append("")

        # Blockers
        output.append("🚫 BLOCKERS")
        output.append("-" * 40)
        blocked = [i for i in current if i.get('status') == 'blocked']
        if blocked:
            for item in blocked:
                output.append(f"⚠️ [{item['id']}] {item['title']}")
        else:
            output.append("No blockers")
        output.append("")

        # Next up
        output.append("➡️ NEXT UP")
        output.append("-" * 40)
        upcoming = self._get_upcoming_items()
        if upcoming:
            for item in upcoming[:3]:
                output.append(f"• [{item['id']}] {item['title']}")
        output.append("")

        return "\n".join(output)

def main():
    """
    Entry point for dashboard

    Usage:
        python3 dashboard.py          # Show main dashboard
        python3 dashboard.py standup  # Show daily standup format
    """
    dashboard = ProjectDashboard()

    args = sys.argv[1:]

    if not args:
        print(dashboard.generate_dashboard())
    elif args[0] == 'standup':
        print(dashboard.generate_daily_standup())
    elif args[0] == '--help':
        print("""
Atlas Project Dashboard - Complete Visibility

Commands:
  dashboard.py         Show complete project dashboard
  dashboard.py standup Show daily standup report

The dashboard provides:
  • Project status overview
  • Work in progress tracking
  • Sprint/iteration status
  • Key metrics and velocity
  • Health indicators
  • Quick action commands
""")
    else:
        print(f"Unknown command: {args[0]}")
        print("Use --help for usage")

if __name__ == "__main__":
    main()