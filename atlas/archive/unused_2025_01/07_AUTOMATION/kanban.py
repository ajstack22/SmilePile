#!/usr/bin/env python3
"""
Atlas Kanban Board - Visual backlog and progress tracking
Provides clear visibility of work status without hassle
"""

import sys
import json
from pathlib import Path
from datetime import datetime
from collections import defaultdict

class KanbanBoard:
    """
    Generates visual kanban board from Atlas stories and bugs
    """

    def __init__(self):
        self.stories_dir = Path('stories')
        self.bugs_dir = Path('bugs')
        self.metadata_file = Path('.atlas/stories_metadata.json')
        self.load_all_items()

    def load_all_items(self):
        """Load all stories and bugs"""
        self.items = []

        # Load metadata if exists
        if self.metadata_file.exists():
            with open(self.metadata_file, 'r') as f:
                metadata = json.load(f)
                self.items.extend(metadata.get('stories', []))
                self.items.extend(metadata.get('bugs', []))
                self.items.extend(metadata.get('epics', []))

    def generate_kanban_board(self, format='text'):
        """
        Generate kanban board visualization

        Args:
            format: 'text', 'markdown', or 'json'

        Returns:
            Formatted kanban board
        """
        # Group items by status
        columns = defaultdict(list)

        # Define kanban columns
        status_map = {
            'backlog': '📋 BACKLOG',
            'ready': '🎯 READY',
            'in_progress': '🚧 IN PROGRESS',
            'in_review': '👀 IN REVIEW',
            'testing': '🧪 TESTING',
            'done': '✅ DONE',
            'blocked': '🚫 BLOCKED'
        }

        # Sort items into columns
        for item in self.items:
            status = item.get('status', 'backlog')
            columns[status].append(item)

        if format == 'markdown':
            return self._generate_markdown_board(columns, status_map)
        elif format == 'json':
            return self._generate_json_board(columns)
        else:
            return self._generate_text_board(columns, status_map)

    def _generate_text_board(self, columns, status_map):
        """Generate text-based kanban board"""
        output = []
        output.append("=" * 80)
        output.append("                         ATLAS KANBAN BOARD")
        output.append("=" * 80)
        output.append("")

        # Calculate stats
        total_items = len(self.items)
        done_items = len(columns.get('done', []))
        in_progress = len(columns.get('in_progress', []))
        blocked = len(columns.get('blocked', []))

        # Progress bar
        if total_items > 0:
            progress_pct = (done_items / total_items) * 100
            progress_bar = self._create_progress_bar(progress_pct)
            output.append(f"Progress: {progress_bar} {done_items}/{total_items} ({progress_pct:.1f}%)")
        else:
            output.append("Progress: No items yet")

        # Status summary
        output.append(f"🚧 Active: {in_progress}  |  ✅ Done: {done_items}  |  🚫 Blocked: {blocked}")
        output.append("")
        output.append("-" * 80)

        # Display columns
        for status in ['backlog', 'ready', 'in_progress', 'in_review', 'testing', 'done', 'blocked']:
            items = columns.get(status, [])
            header = status_map.get(status, status.upper())

            output.append(f"\n{header} ({len(items)})")
            output.append("-" * 40)

            if items:
                # Sort by priority
                priority_order = {'critical': 0, 'high': 1, 'medium': 2, 'low': 3}
                sorted_items = sorted(items,
                                    key=lambda x: priority_order.get(x.get('priority', 'medium'), 2))

                for item in sorted_items[:10]:  # Show max 10 per column
                    item_type = item.get('type', 'story')
                    item_id = item.get('id', 'Unknown')
                    title = item.get('title', 'Untitled')[:50]
                    priority = item.get('priority', 'medium')

                    # Icons
                    type_icon = {'story': '📝', 'bug': '🐛', 'epic': '📚'}.get(item_type, '📄')
                    priority_icon = {'critical': '🔴', 'high': '🟠', 'medium': '🟡', 'low': '🟢'}.get(priority, '')

                    output.append(f"  {type_icon} [{item_id}] {priority_icon} {title}")

                if len(items) > 10:
                    output.append(f"  ... and {len(items) - 10} more")
            else:
                output.append("  (empty)")

        output.append("\n" + "=" * 80)
        return "\n".join(output)

    def _generate_markdown_board(self, columns, status_map):
        """Generate markdown kanban board"""
        output = []
        output.append("# 📊 Atlas Kanban Board\n")
        output.append(f"*Generated: {datetime.now().strftime('%Y-%m-%d %H:%M')}*\n")

        # Stats
        total = len(self.items)
        done = len(columns.get('done', []))
        progress = (done / total * 100) if total > 0 else 0

        output.append("## 📈 Progress Overview\n")
        output.append(f"- **Total Items**: {total}")
        output.append(f"- **Completed**: {done} ({progress:.1f}%)")
        output.append(f"- **In Progress**: {len(columns.get('in_progress', []))}")
        output.append(f"- **Blocked**: {len(columns.get('blocked', []))}\n")

        # Board
        output.append("## 🎯 Board\n")
        output.append("| Backlog | Ready | In Progress | Review | Testing | Done |")
        output.append("|---------|-------|-------------|--------|---------|------|")

        # Find max items in any column
        max_items = max(len(columns.get(s, [])) for s in ['backlog', 'ready', 'in_progress', 'in_review', 'testing', 'done'])

        for i in range(min(max_items, 15)):  # Show max 15 rows
            row = []
            for status in ['backlog', 'ready', 'in_progress', 'in_review', 'testing', 'done']:
                items = columns.get(status, [])
                if i < len(items):
                    item = items[i]
                    row.append(f"[{item['id']}] {item.get('title', '')[:20]}")
                else:
                    row.append("")
            output.append("| " + " | ".join(row) + " |")

        return "\n".join(output)

    def _generate_json_board(self, columns):
        """Generate JSON kanban data"""
        board = {
            'generated_at': datetime.now().isoformat(),
            'statistics': {
                'total_items': len(self.items),
                'by_status': {status: len(items) for status, items in columns.items()},
                'by_type': {},
                'by_priority': {}
            },
            'columns': {}
        }

        # Count by type and priority
        type_counts = defaultdict(int)
        priority_counts = defaultdict(int)

        for item in self.items:
            type_counts[item.get('type', 'story')] += 1
            priority_counts[item.get('priority', 'medium')] += 1

        board['statistics']['by_type'] = dict(type_counts)
        board['statistics']['by_priority'] = dict(priority_counts)

        # Add column data
        for status, items in columns.items():
            board['columns'][status] = [
                {
                    'id': item.get('id'),
                    'type': item.get('type'),
                    'title': item.get('title'),
                    'priority': item.get('priority')
                }
                for item in items
            ]

        return board

    def _create_progress_bar(self, percentage, width=30):
        """Create ASCII progress bar"""
        filled = int(width * percentage / 100)
        bar = '█' * filled + '░' * (width - filled)
        return f"[{bar}]"

    def generate_backlog(self):
        """
        Generate prioritized backlog view

        Returns:
            Prioritized list of work items
        """
        backlog = []

        # Priority order
        priority_order = {'critical': 0, 'high': 1, 'medium': 2, 'low': 3}

        # Filter items not done
        pending_items = [item for item in self.items
                        if item.get('status') not in ['done', 'closed']]

        # Sort by priority and status
        sorted_items = sorted(pending_items,
                            key=lambda x: (
                                priority_order.get(x.get('priority', 'medium'), 2),
                                0 if x.get('status') == 'in_progress' else 1
                            ))

        output = []
        output.append("=" * 80)
        output.append("                      PRIORITIZED BACKLOG")
        output.append("=" * 80)
        output.append("")

        current_priority = None
        for item in sorted_items:
            priority = item.get('priority', 'medium')

            # Add priority header
            if priority != current_priority:
                current_priority = priority
                icon = {'critical': '🔴', 'high': '🟠', 'medium': '🟡', 'low': '🟢'}.get(priority, '')
                output.append(f"\n{icon} {priority.upper()} PRIORITY")
                output.append("-" * 40)

            # Item details
            status_icon = {
                'in_progress': '🚧',
                'blocked': '🚫',
                'ready': '🎯',
                'backlog': '📋'
            }.get(item.get('status', 'backlog'), '❓')

            output.append(f"{status_icon} [{item['id']}] {item.get('title', 'Untitled')}")
            if item.get('description'):
                output.append(f"    {item['description'][:60]}...")

        output.append("\n" + "=" * 80)
        return "\n".join(output)

    def update_item_status(self, item_id, new_status):
        """
        Update status of an item (delegates to 02_create_story.py)

        Args:
            item_id: Story/bug ID
            new_status: New status

        Returns:
            Update result
        """
        import subprocess

        try:
            result = subprocess.run(
                ['python3', '02_create_story.py', 'update', item_id, new_status],
                capture_output=True,
                text=True
            )
            return json.loads(result.stdout)
        except Exception as e:
            return {'error': str(e)}

    def get_metrics(self):
        """
        Calculate productivity metrics

        Returns:
            Metrics dictionary
        """
        metrics = {
            'velocity': self._calculate_velocity(),
            'cycle_time': self._calculate_cycle_time(),
            'wip_limit_status': self._check_wip_limits(),
            'bottlenecks': self._identify_bottlenecks()
        }

        return metrics

    def _calculate_velocity(self):
        """Calculate completion velocity"""
        done_items = [item for item in self.items if item.get('status') == 'done']

        # Group by week
        weeks = defaultdict(int)
        for item in done_items:
            if 'completed_at' in item:
                date = datetime.fromisoformat(item['completed_at'])
                week = date.strftime('%Y-W%U')
                weeks[week] += 1

        if weeks:
            avg_velocity = sum(weeks.values()) / len(weeks)
            return {
                'average_per_week': round(avg_velocity, 1),
                'last_week': list(weeks.values())[-1] if weeks else 0
            }
        return {'average_per_week': 0, 'last_week': 0}

    def _calculate_cycle_time(self):
        """Calculate average cycle time"""
        # This would need timestamp tracking
        return {'average_days': 'N/A', 'note': 'Requires timestamp tracking'}

    def _check_wip_limits(self):
        """Check work-in-progress limits"""
        in_progress = len([item for item in self.items if item.get('status') == 'in_progress'])

        # Recommended WIP limit
        recommended_wip = 3

        return {
            'current_wip': in_progress,
            'recommended_limit': recommended_wip,
            'status': 'OK' if in_progress <= recommended_wip else 'EXCEEDED'
        }

    def _identify_bottlenecks(self):
        """Identify process bottlenecks"""
        status_counts = defaultdict(int)
        for item in self.items:
            status_counts[item.get('status', 'backlog')] += 1

        bottlenecks = []
        if status_counts.get('in_review', 0) > 3:
            bottlenecks.append('Review queue building up')
        if status_counts.get('blocked', 0) > 2:
            bottlenecks.append('Multiple items blocked')
        if status_counts.get('testing', 0) > 5:
            bottlenecks.append('Testing bottleneck')

        return bottlenecks if bottlenecks else ['No bottlenecks detected']

def main():
    """
    Entry point for kanban visualization

    Usage:
        python3 kanban.py                    # Show kanban board
        python3 kanban.py --format markdown  # Markdown format
        python3 kanban.py backlog            # Show prioritized backlog
        python3 kanban.py metrics            # Show productivity metrics
        python3 kanban.py update S001 in_progress  # Update item status
    """
    board = KanbanBoard()

    args = sys.argv[1:]

    if not args:
        # Default: show text board
        print(board.generate_kanban_board('text'))
        return

    command = args[0]

    if command == '--format' and len(args) > 1:
        format_type = args[1]
        if format_type == 'json':
            print(json.dumps(board.generate_kanban_board('json'), indent=2))
        else:
            print(board.generate_kanban_board(format_type))

    elif command == 'backlog':
        print(board.generate_backlog())

    elif command == 'metrics':
        metrics = board.get_metrics()
        print(json.dumps(metrics, indent=2))

    elif command == 'update' and len(args) >= 3:
        item_id = args[1]
        new_status = args[2]
        result = board.update_item_status(item_id, new_status)
        print(json.dumps(result, indent=2))

    elif command == '--help':
        print("""
Atlas Kanban Board - Visual Progress Tracking

Commands:
  kanban.py                     Show kanban board (text)
  kanban.py --format markdown   Show board in markdown
  kanban.py --format json       Export board as JSON
  kanban.py backlog            Show prioritized backlog
  kanban.py metrics            Show productivity metrics
  kanban.py update [ID] [status]  Update item status

Status values:
  backlog | ready | in_progress | in_review | testing | done | blocked

Example:
  python3 kanban.py
  python3 kanban.py update S001 in_progress
  python3 kanban.py --format markdown > board.md
""")

    else:
        print(f"Unknown command: {command}")
        print("Use --help for usage information")

if __name__ == "__main__":
    main()