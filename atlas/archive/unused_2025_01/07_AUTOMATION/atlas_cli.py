#!/usr/bin/env python3
"""
Atlas CLI - Unified interface for all backlog operations
"""

import argparse
import json
import sys
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Optional

# Import automation modules
sys.path.append(str(Path(__file__).parent))
from backlog_automation import BacklogAutomation, StoryStatus, StoryType
from story_generator import StoryGenerator


class AtlasCLI:
    """Unified CLI for Atlas operations"""

    def __init__(self):
        self.project_root = Path.cwd()
        self.automation = BacklogAutomation(self.project_root)
        self.generator = StoryGenerator(self.project_root)

    def story_create(self, args):
        """Create a new story"""
        if args.template:
            # Generate from template
            variables = json.loads(args.variables) if args.variables else {}
            story_data = self.generator.generate_from_template(args.template, variables)

            # Create story
            story_type = StoryType[args.type.upper()] if args.type else StoryType.FEATURE
            story = self.automation.create_story(
                title=story_data.get('title', args.title),
                story_type=story_type,
                estimate=story_data.get('estimate', args.estimate),
                labels=story_data.get('labels', []),
                acceptance_criteria=story_data.get('acceptance_criteria', []),
                implementation_notes=story_data.get('implementation_notes')
            )
        else:
            # Create directly
            story_type = StoryType[args.type.upper()]
            story = self.automation.create_story(
                title=args.title,
                story_type=story_type,
                estimate=args.estimate,
                wave=args.wave,
                epic=args.epic,
                labels=args.labels.split(',') if args.labels else []
            )

        print(f"✅ Created {story.id}: {story.title}")
        print(f"   Type: {story.type.value}, Estimate: {story.estimate}, Priority: {story.priority}")

    def story_update(self, args):
        """Update a story"""
        updates = {}

        if args.status:
            updates['status'] = StoryStatus[args.status.upper()]
        if args.priority:
            updates['priority'] = args.priority
        if args.estimate:
            updates['estimate'] = args.estimate
        if args.wave:
            updates['wave'] = args.wave
        if args.assigned:
            updates['assigned_to'] = args.assigned

        updated = self.automation.bulk_update_stories([args.story_id], updates)
        if updated:
            print(f"✅ Updated {args.story_id}")

    def story_transition(self, args):
        """Transition story status"""
        success = self.automation.manage_story_lifecycle(args.story_id, args.action)
        if not success:
            print(f"❌ Failed to {args.action} {args.story_id}")

    def backlog_scan(self, args):
        """Scan for completed stories"""
        completed = self.automation.auto_complete_stories()
        print(f"Scanned codebase for completion evidence")

        if args.auto_complete and completed:
            print(f"✅ Auto-completed {len(completed)} stories")

    def backlog_prioritize(self, args):
        """Prioritize backlog"""
        stories = self.automation.prioritize_backlog()
        print(f"✅ Prioritized {len(stories)} stories")

        # Show top stories
        print("\nTop Priority Stories:")
        for story in stories[:args.top]:
            print(f"  {story.priority:3d} - [{story.id}] {story.title}")

    def backlog_report(self, args):
        """Generate backlog report"""
        report = self.automation.export_markdown_report()

        if args.output:
            output_file = Path(args.output)
            with open(output_file, 'w') as f:
                f.write(report)
            print(f"✅ Report saved to {output_file}")
        else:
            print(report)

    def wave_plan(self, args):
        """Plan a wave"""
        velocity = args.velocity
        if velocity == 'auto':
            velocity = self.automation.calculate_velocity()
            print(f"Using calculated velocity: {velocity:.1f} points/sprint")
        else:
            velocity = int(velocity) if velocity else None

        wave_stories = self.automation.plan_wave(args.stories, velocity)

        print(f"\n📋 Wave Plan ({len(wave_stories)} stories):")
        total_points = 0
        for story in wave_stories:
            print(f"  [{story.id}] {story.title} ({story.estimate} pts)")
            total_points += story.estimate

        print(f"\nTotal points: {total_points}")

    def wave_assign(self, args):
        """Assign stories to wave"""
        story_ids = args.story_ids.split(',')
        updates = {'wave': args.wave}

        updated = self.automation.bulk_update_stories(story_ids, updates)
        print(f"✅ Assigned {len(updated)} stories to Wave {args.wave}")

    def epic_decompose(self, args):
        """Decompose epic into stories"""
        stories = self.generator.generate_epic_stories(args.epic_type, args.entity)

        print(f"\n📦 Epic Decomposition ({len(stories)} stories):")
        for story in stories:
            print(f"  - {story['title']} ({story['type']}, {story['estimate']} pts)")

        if args.create:
            print("\nCreating stories...")
            for story_data in stories:
                story_type = StoryType[story_data['type'].upper()]
                story = self.automation.create_story(
                    title=story_data['title'],
                    story_type=story_type,
                    estimate=story_data['estimate'],
                    epic=args.epic_type
                )
                print(f"  ✅ Created {story.id}")

    def quality_check(self, args):
        """Check quality and suggest improvements"""
        analytics = self.automation.generate_analytics()

        print("\n🔍 Quality Check")
        print("=" * 40)

        # Check velocity
        velocity = analytics['velocity']
        if velocity < 15:
            print(f"⚠️  Low velocity: {velocity:.1f} pts/sprint")
        else:
            print(f"✅ Good velocity: {velocity:.1f} pts/sprint")

        # Check cycle time
        cycle_time = analytics['cycle_time']
        if cycle_time > 14:
            print(f"⚠️  High cycle time: {cycle_time:.1f} days")
        else:
            print(f"✅ Good cycle time: {cycle_time:.1f} days")

        # Check blocked stories
        blocked = analytics['blocked_stories']
        if blocked:
            print(f"⚠️  {len(blocked)} blocked stories: {', '.join(blocked)}")

        # Suggest missing stories
        if args.create_fix_stories:
            all_stories = list(self.automation.stories.values())
            suggestions = self.generator.analyze_and_suggest(
                [{'id': s.id, 'title': s.title, 'type': s.type.value,
                  'labels': s.labels} for s in all_stories]
            )

            if suggestions:
                print(f"\n📝 Creating {len(suggestions)} suggested stories...")
                for suggestion in suggestions:
                    if suggestion['suggestion'] == 'missing_tests':
                        for test_story in suggestion['recommended_stories']:
                            story = self.automation.create_story(
                                title=test_story['title'],
                                story_type=StoryType.TECH_DEBT,
                                estimate=test_story['estimate'],
                                dependencies=test_story['dependencies'],
                                labels=test_story['labels']
                            )
                            print(f"  ✅ Created {story.id}: {story.title}")

    def stats(self, args):
        """Show backlog statistics"""
        analytics = self.automation.generate_analytics()

        print("\n📊 Backlog Statistics")
        print("=" * 40)
        print(f"Total Stories: {analytics['total_stories']}")
        print(f"Velocity: {analytics['velocity']:.1f} pts/sprint")
        print(f"Cycle Time: {analytics['cycle_time']:.1f} days")

        print("\nBy Status:")
        for status, count in analytics['by_status'].items():
            bar = '█' * min(count, 20)
            print(f"  {status:12} {count:3} {bar}")

        print("\nBy Type:")
        for story_type, count in analytics['by_type'].items():
            bar = '█' * min(count, 20)
            print(f"  {story_type:12} {count:3} {bar}")

    def search(self, args):
        """Search stories"""
        results = []
        query = args.query.lower()

        for story in self.automation.stories.values():
            if (query in story.title.lower() or
                query in story.id.lower() or
                any(query in label for label in story.labels)):
                results.append(story)

        print(f"\n🔍 Found {len(results)} matching stories:")
        for story in results[:20]:
            status_icon = "✅" if story.status == StoryStatus.DONE else "🔄"
            print(f"  {status_icon} [{story.id}] {story.title} ({story.status.value})")

def main():
    """Main CLI entry point"""
    parser = argparse.ArgumentParser(
        description='Atlas CLI - Unified backlog management',
        formatter_class=argparse.RawDescriptionHelpFormatter
    )

    subparsers = parser.add_subparsers(dest='command', help='Commands')

    # Story commands
    story_parser = subparsers.add_parser('story', help='Story management')
    story_sub = story_parser.add_subparsers(dest='subcommand')

    # story create
    create_parser = story_sub.add_parser('create', help='Create story')
    create_parser.add_argument('--title', required=True, help='Story title')
    create_parser.add_argument('--type', default='feature',
                              choices=['feature', 'bug', 'tech_debt', 'optimization'])
    create_parser.add_argument('--estimate', type=int, default=3)
    create_parser.add_argument('--template', help='Use template')
    create_parser.add_argument('--variables', help='Template variables (JSON)')
    create_parser.add_argument('--wave', type=int, help='Assign to wave')
    create_parser.add_argument('--epic', help='Epic name')
    create_parser.add_argument('--labels', help='Comma-separated labels')

    # story update
    update_parser = story_sub.add_parser('update', help='Update story')
    update_parser.add_argument('story_id', help='Story ID')
    update_parser.add_argument('--status', help='New status')
    update_parser.add_argument('--priority', type=int, help='New priority')
    update_parser.add_argument('--estimate', type=int, help='New estimate')
    update_parser.add_argument('--wave', type=int, help='New wave')
    update_parser.add_argument('--assigned', help='Assigned to')

    # story transition
    transition_parser = story_sub.add_parser('transition', help='Transition story')
    transition_parser.add_argument('story_id', help='Story ID')
    transition_parser.add_argument('action',
                                  choices=['start', 'submit', 'approve', 'complete', 'block', 'unblock'])

    # Backlog commands
    backlog_parser = subparsers.add_parser('backlog', help='Backlog operations')
    backlog_sub = backlog_parser.add_subparsers(dest='subcommand')

    # backlog scan
    scan_parser = backlog_sub.add_parser('scan', help='Scan for completions')
    scan_parser.add_argument('--auto-complete', action='store_true',
                           help='Auto-complete stories')

    # backlog prioritize
    prioritize_parser = backlog_sub.add_parser('prioritize', help='Prioritize backlog')
    prioritize_parser.add_argument('--top', type=int, default=10,
                                  help='Show top N stories')

    # backlog report
    report_parser = backlog_sub.add_parser('report', help='Generate report')
    report_parser.add_argument('--output', help='Output file')

    # Wave commands
    wave_parser = subparsers.add_parser('wave', help='Wave planning')
    wave_sub = wave_parser.add_subparsers(dest='subcommand')

    # wave plan
    plan_parser = wave_sub.add_parser('plan', help='Plan wave')
    plan_parser.add_argument('--stories', type=int, default=6,
                           help='Number of stories')
    plan_parser.add_argument('--velocity', default='auto',
                           help='Velocity (points or "auto")')

    # wave assign
    assign_parser = wave_sub.add_parser('assign', help='Assign to wave')
    assign_parser.add_argument('wave', type=int, help='Wave number')
    assign_parser.add_argument('story_ids', help='Comma-separated story IDs')

    # Epic commands
    epic_parser = subparsers.add_parser('epic', help='Epic management')
    epic_sub = epic_parser.add_subparsers(dest='subcommand')

    # epic decompose
    decompose_parser = epic_sub.add_parser('decompose', help='Decompose epic')
    decompose_parser.add_argument('epic_type',
                                 choices=['authentication', 'crud', 'performance',
                                        'testing', 'mobile_optimization'])
    decompose_parser.add_argument('--entity', help='Entity name for CRUD')
    decompose_parser.add_argument('--create', action='store_true',
                                 help='Create stories')

    # Quality commands
    quality_parser = subparsers.add_parser('quality', help='Quality checks')
    quality_sub = quality_parser.add_subparsers(dest='subcommand')

    # quality check
    check_parser = quality_sub.add_parser('check', help='Run quality check')
    check_parser.add_argument('--create-fix-stories', action='store_true',
                            help='Create stories to fix issues')

    # Stats command
    stats_parser = subparsers.add_parser('stats', help='Show statistics')

    # Search command
    search_parser = subparsers.add_parser('search', help='Search stories')
    search_parser.add_argument('query', help='Search query')

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        return

    cli = AtlasCLI()

    # Route to appropriate handler
    if args.command == 'story':
        if args.subcommand == 'create':
            cli.story_create(args)
        elif args.subcommand == 'update':
            cli.story_update(args)
        elif args.subcommand == 'transition':
            cli.story_transition(args)

    elif args.command == 'backlog':
        if args.subcommand == 'scan':
            cli.backlog_scan(args)
        elif args.subcommand == 'prioritize':
            cli.backlog_prioritize(args)
        elif args.subcommand == 'report':
            cli.backlog_report(args)

    elif args.command == 'wave':
        if args.subcommand == 'plan':
            cli.wave_plan(args)
        elif args.subcommand == 'assign':
            cli.wave_assign(args)

    elif args.command == 'epic':
        if args.subcommand == 'decompose':
            cli.epic_decompose(args)

    elif args.command == 'quality':
        if args.subcommand == 'check':
            cli.quality_check(args)

    elif args.command == 'stats':
        cli.stats(args)

    elif args.command == 'search':
        cli.search(args)

if __name__ == '__main__':
    main()