#!/usr/bin/env python3
"""
Atlas Backlog Analytics Dashboard - Comprehensive analytics, metrics, and reporting.
Generates visual dashboards and insights for backlog management.
"""

import sys
import json
import os
from pathlib import Path
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional, Tuple
import json
from collections import defaultdict, Counter
import statistics

from backlog_manager import BacklogManager


class BacklogDashboard:
    """
    Advanced analytics and dashboard for backlog insights
    """

    def __init__(self):
        self.backlog_manager = BacklogManager()
        self.reports_dir = Path('reports')
        self.reports_dir.mkdir(exist_ok=True)

    def generate_velocity_report(self, weeks: int = 8) -> Dict:
        """Generate velocity tracking report"""
        all_stories = self.backlog_manager.get_prioritized_backlog('all')

        # Get completed stories by week
        velocity_data = self._calculate_velocity_by_week(all_stories, weeks)

        # Calculate trends
        velocities = [week['completed_count'] for week in velocity_data]
        avg_velocity = statistics.mean(velocities) if velocities else 0
        trend = self._calculate_trend(velocities)

        # Calculate story point velocity if we have effort estimates
        story_point_velocity = self._calculate_story_point_velocity(all_stories, weeks)

        return {
            'period_weeks': weeks,
            'average_velocity': round(avg_velocity, 2),
            'velocity_trend': trend,
            'weekly_data': velocity_data,
            'story_point_velocity': story_point_velocity,
            'predictability': self._calculate_velocity_predictability(velocities),
            'recommendations': self._generate_velocity_recommendations(avg_velocity, trend)
        }

    def generate_burndown_chart_data(self, wave_number: int = None) -> Dict:
        """Generate burndown chart data for current or specified wave"""
        if wave_number is None:
            wave_number = self._get_current_wave_number()

        wave_data = self._load_wave_data(wave_number)
        if not wave_data:
            return {'error': f'Wave {wave_number} not found'}

        # Calculate ideal burndown
        total_stories = len(wave_data.get('stories', []))
        wave_duration = 14  # 2 weeks default
        ideal_burndown = self._calculate_ideal_burndown(total_stories, wave_duration)

        # Calculate actual burndown
        actual_burndown = self._calculate_actual_burndown(wave_data)

        return {
            'wave_number': wave_number,
            'total_stories': total_stories,
            'wave_duration_days': wave_duration,
            'ideal_burndown': ideal_burndown,
            'actual_burndown': actual_burndown,
            'completion_prediction': self._predict_completion_date(actual_burndown, ideal_burndown),
            'risk_level': self._assess_burndown_risk(actual_burndown, ideal_burndown)
        }

    def generate_cycle_time_analysis(self) -> Dict:
        """Analyze story cycle times and bottlenecks"""
        all_stories = self._get_stories_with_timestamps()

        cycle_times = []
        status_durations = defaultdict(list)

        for story in all_stories:
            cycle_time = self._calculate_story_cycle_time(story)
            if cycle_time:
                cycle_times.append(cycle_time)

                # Track time in each status
                status_history = self._get_story_status_history(story)
                for status, duration in status_history.items():
                    status_durations[status].append(duration)

        return {
            'average_cycle_time_days': round(statistics.mean(cycle_times), 2) if cycle_times else 0,
            'median_cycle_time_days': round(statistics.median(cycle_times), 2) if cycle_times else 0,
            'cycle_time_distribution': self._create_distribution(cycle_times),
            'status_durations': {
                status: {
                    'average_days': round(statistics.mean(durations), 2),
                    'median_days': round(statistics.median(durations), 2)
                }
                for status, durations in status_durations.items() if durations
            },
            'bottleneck_analysis': self._identify_bottlenecks(status_durations),
            'improvement_suggestions': self._suggest_cycle_time_improvements(status_durations)
        }

    def generate_priority_distribution_report(self) -> Dict:
        """Analyze priority distribution and trends"""
        all_stories = self.backlog_manager.get_prioritized_backlog('all')

        # Current distribution
        priority_counts = Counter(story.get('priority', 'medium') for story in all_stories)

        # Distribution by status
        priority_by_status = defaultdict(lambda: defaultdict(int))
        for story in all_stories:
            priority = story.get('priority', 'medium')
            status = story.get('status', 'backlog')
            priority_by_status[status][priority] += 1

        # Type distribution
        type_priority_distribution = defaultdict(lambda: defaultdict(int))
        for story in all_stories:
            story_type = story.get('type', 'feature')
            priority = story.get('priority', 'medium')
            type_priority_distribution[story_type][priority] += 1

        return {
            'total_stories': len(all_stories),
            'priority_distribution': dict(priority_counts),
            'priority_percentages': {
                priority: round((count / len(all_stories)) * 100, 1)
                for priority, count in priority_counts.items()
            },
            'priority_by_status': dict(priority_by_status),
            'type_priority_distribution': dict(type_priority_distribution),
            'priority_score_analysis': self._analyze_priority_scores(all_stories),
            'priority_recommendations': self._generate_priority_recommendations(priority_counts, all_stories)
        }

    def generate_blocked_stories_alert(self) -> Dict:
        """Generate alert report for blocked stories"""
        all_stories = self.backlog_manager.get_prioritized_backlog('all')

        blocked_stories = [story for story in all_stories if story.get('status') == 'blocked']

        # Categorize blocked stories
        blocking_reasons = defaultdict(list)
        critical_blocked = []
        long_blocked = []

        for story in blocked_stories:
            # Analyze blocking reason (would need more sophisticated parsing)
            blocking_reason = self._determine_blocking_reason(story)
            blocking_reasons[blocking_reason].append(story)

            # Check if critical priority
            if story.get('priority') == 'critical':
                critical_blocked.append(story)

            # Check if blocked for long time
            blocked_duration = self._calculate_blocked_duration(story)
            if blocked_duration and blocked_duration > 7:  # More than a week
                long_blocked.append(story)

        return {
            'total_blocked': len(blocked_stories),
            'critical_blocked': len(critical_blocked),
            'long_blocked': len(long_blocked),
            'blocking_reasons': dict(blocking_reasons),
            'critical_blocked_stories': critical_blocked,
            'long_blocked_stories': long_blocked,
            'unblocking_actions': self._suggest_unblocking_actions(blocking_reasons),
            'alert_level': self._determine_alert_level(blocked_stories, critical_blocked, long_blocked)
        }

    def generate_quality_metrics_report(self) -> Dict:
        """Generate quality metrics and trends"""
        all_stories = self.backlog_manager.get_prioritized_backlog('all')

        # Bug analysis
        bugs = [story for story in all_stories if story.get('type') == 'bug']
        bug_severity_dist = Counter(bug.get('severity', 'medium') for bug in bugs)

        # Tech debt analysis
        tech_debt = [story for story in all_stories if story.get('type') == 'tech_debt']
        tech_debt_risk_dist = Counter(debt.get('risk', 'medium') for debt in tech_debt)

        # Quality trends
        quality_trends = self._calculate_quality_trends(all_stories)

        return {
            'bug_analysis': {
                'total_bugs': len(bugs),
                'severity_distribution': dict(bug_severity_dist),
                'open_critical_bugs': len([b for b in bugs if b.get('severity') == 'critical' and b.get('status') != 'done']),
                'bug_age_analysis': self._analyze_bug_ages(bugs)
            },
            'tech_debt_analysis': {
                'total_tech_debt': len(tech_debt),
                'risk_distribution': dict(tech_debt_risk_dist),
                'high_risk_debt': len([t for t in tech_debt if t.get('risk') == 'high' and t.get('status') != 'done']),
                'debt_trends': self._analyze_tech_debt_trends(tech_debt)
            },
            'quality_trends': quality_trends,
            'quality_score': self._calculate_overall_quality_score(bugs, tech_debt),
            'quality_recommendations': self._generate_quality_recommendations(bugs, tech_debt)
        }

    def generate_team_performance_report(self) -> Dict:
        """Generate team performance insights"""
        all_stories = self.backlog_manager.get_prioritized_backlog('all')

        # Completion rates by type
        completion_by_type = self._calculate_completion_rates_by_type(all_stories)

        # Average cycle times by type
        cycle_times_by_type = self._calculate_cycle_times_by_type(all_stories)

        # Throughput analysis
        throughput = self._calculate_throughput_metrics(all_stories)

        return {
            'completion_rates': completion_by_type,
            'cycle_times_by_type': cycle_times_by_type,
            'throughput_metrics': throughput,
            'efficiency_score': self._calculate_efficiency_score(completion_by_type, cycle_times_by_type),
            'performance_trends': self._analyze_performance_trends(all_stories),
            'improvement_areas': self._identify_improvement_areas(completion_by_type, cycle_times_by_type)
        }

    def generate_comprehensive_report(self) -> Dict:
        """Generate comprehensive dashboard report"""
        return {
            'generated_at': datetime.now().isoformat(),
            'velocity_metrics': self.generate_velocity_report(),
            'cycle_time_analysis': self.generate_cycle_time_analysis(),
            'priority_distribution': self.generate_priority_distribution_report(),
            'blocked_stories_alert': self.generate_blocked_stories_alert(),
            'quality_metrics': self.generate_quality_metrics_report(),
            'team_performance': self.generate_team_performance_report(),
            'summary_insights': self._generate_summary_insights()
        }

    def export_dashboard_html(self, report_data: Dict, filename: str = None) -> str:
        """Export dashboard as HTML report"""
        if filename is None:
            filename = f"backlog_dashboard_{datetime.now().strftime('%Y%m%d_%H%M')}.html"

        html_content = self._generate_html_dashboard(report_data)

        output_path = self.reports_dir / filename
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(html_content)

        return str(output_path)

    # Helper methods for calculations
    def _calculate_velocity_by_week(self, stories: List[Dict], weeks: int) -> List[Dict]:
        """Calculate weekly velocity data"""
        completed_stories = [s for s in stories if s.get('status') == 'done' and s.get('completed_at')]

        weekly_data = []
        end_date = datetime.now()

        for week in range(weeks):
            week_start = end_date - timedelta(weeks=(week + 1))
            week_end = end_date - timedelta(weeks=week)

            week_completed = []
            for story in completed_stories:
                try:
                    completed_date = datetime.fromisoformat(story['completed_at'])
                    if week_start <= completed_date < week_end:
                        week_completed.append(story)
                except:
                    continue

            weekly_data.append({
                'week_start': week_start.strftime('%Y-%m-%d'),
                'week_end': week_end.strftime('%Y-%m-%d'),
                'completed_count': len(week_completed),
                'completed_stories': [s['id'] for s in week_completed]
            })

        return list(reversed(weekly_data))  # Chronological order

    def _calculate_story_point_velocity(self, stories: List[Dict], weeks: int) -> Dict:
        """Calculate velocity in story points"""
        effort_map = {'small': 1, 'medium': 3, 'large': 5, 'xl': 8}

        completed_stories = [s for s in stories if s.get('status') == 'done' and s.get('completed_at')]

        weekly_points = []
        end_date = datetime.now()

        for week in range(weeks):
            week_start = end_date - timedelta(weeks=(week + 1))
            week_end = end_date - timedelta(weeks=week)

            week_points = 0
            for story in completed_stories:
                try:
                    completed_date = datetime.fromisoformat(story['completed_at'])
                    if week_start <= completed_date < week_end:
                        effort = story.get('effort', 'medium')
                        week_points += effort_map.get(effort, 3)
                except:
                    continue

            weekly_points.append(week_points)

        return {
            'weekly_points': list(reversed(weekly_points)),
            'average_points_per_week': round(statistics.mean(weekly_points), 2) if weekly_points else 0,
            'total_points_completed': sum(weekly_points)
        }

    def _calculate_trend(self, values: List[float]) -> str:
        """Calculate trend direction from values"""
        if len(values) < 2:
            return 'stable'

        # Simple linear trend
        n = len(values)
        x_mean = statistics.mean(range(n))
        y_mean = statistics.mean(values)

        numerator = sum((i - x_mean) * (values[i] - y_mean) for i in range(n))
        denominator = sum((i - x_mean) ** 2 for i in range(n))

        if denominator == 0:
            return 'stable'

        slope = numerator / denominator

        if slope > 0.1:
            return 'improving'
        elif slope < -0.1:
            return 'declining'
        else:
            return 'stable'

    def _calculate_velocity_predictability(self, velocities: List[float]) -> float:
        """Calculate velocity predictability (lower standard deviation = more predictable)"""
        if len(velocities) < 2:
            return 1.0

        mean_velocity = statistics.mean(velocities)
        if mean_velocity == 0:
            return 0.0

        std_dev = statistics.stdev(velocities)
        coefficient_of_variation = std_dev / mean_velocity

        # Convert to predictability score (0-1, higher is more predictable)
        return max(0, 1 - coefficient_of_variation)

    def _get_stories_with_timestamps(self) -> List[Dict]:
        """Get stories that have timestamp data"""
        all_stories = self.backlog_manager.get_prioritized_backlog('all')
        return [s for s in all_stories if s.get('created_at')]

    def _calculate_story_cycle_time(self, story: Dict) -> Optional[float]:
        """Calculate cycle time for a story in days"""
        if not story.get('completed_at') or not story.get('started_at', story.get('created_at')):
            return None

        try:
            start = datetime.fromisoformat(story.get('started_at', story['created_at']))
            end = datetime.fromisoformat(story['completed_at'])
            return (end - start).days
        except:
            return None

    def _get_story_status_history(self, story: Dict) -> Dict[str, float]:
        """Get time spent in each status (placeholder - would need status history tracking)"""
        # This is a placeholder - in reality, you'd need to track status changes
        cycle_time = self._calculate_story_cycle_time(story)
        if not cycle_time:
            return {}

        # Mock distribution based on common patterns
        return {
            'backlog': cycle_time * 0.1,
            'ready': cycle_time * 0.2,
            'in_progress': cycle_time * 0.6,
            'in_review': cycle_time * 0.1
        }

    def _create_distribution(self, values: List[float]) -> Dict:
        """Create distribution analysis"""
        if not values:
            return {}

        return {
            'min': min(values),
            'max': max(values),
            'mean': round(statistics.mean(values), 2),
            'median': round(statistics.median(values), 2),
            'std_dev': round(statistics.stdev(values), 2) if len(values) > 1 else 0,
            'percentiles': {
                '25th': round(statistics.quantiles(values, n=4)[0], 2) if len(values) >= 4 else 0,
                '75th': round(statistics.quantiles(values, n=4)[2], 2) if len(values) >= 4 else 0,
                '90th': round(statistics.quantiles(values, n=10)[8], 2) if len(values) >= 10 else 0
            }
        }

    def _identify_bottlenecks(self, status_durations: Dict) -> List[Dict]:
        """Identify process bottlenecks"""
        bottlenecks = []

        for status, durations in status_durations.items():
            if durations:
                avg_duration = statistics.mean(durations)
                if avg_duration > 5:  # More than 5 days average
                    bottlenecks.append({
                        'status': status,
                        'average_days': round(avg_duration, 2),
                        'severity': 'high' if avg_duration > 10 else 'medium'
                    })

        return sorted(bottlenecks, key=lambda x: x['average_days'], reverse=True)

    def _analyze_priority_scores(self, stories: List[Dict]) -> Dict:
        """Analyze priority score distribution"""
        scores = [self.backlog_manager.calculate_priority_score(story) for story in stories]

        return {
            'score_distribution': self._create_distribution(scores),
            'high_priority_count': len([s for s in scores if s >= 1000]),
            'medium_priority_count': len([s for s in scores if 500 <= s < 1000]),
            'low_priority_count': len([s for s in scores if s < 500])
        }

    def _determine_blocking_reason(self, story: Dict) -> str:
        """Determine why a story is blocked (placeholder)"""
        # This would analyze story description/comments for blocking reasons
        description = story.get('description', '').lower()

        if 'dependency' in description or 'depends' in description:
            return 'dependency'
        elif 'resource' in description or 'people' in description:
            return 'resource_constraint'
        elif 'approval' in description or 'decision' in description:
            return 'waiting_for_approval'
        else:
            return 'unknown'

    def _calculate_blocked_duration(self, story: Dict) -> Optional[int]:
        """Calculate how long a story has been blocked"""
        # This would require tracking when status changed to blocked
        # For now, using created_at as proxy
        if story.get('created_at'):
            try:
                created = datetime.fromisoformat(story['created_at'])
                return (datetime.now() - created).days
            except:
                pass
        return None

    def _determine_alert_level(self, blocked_stories: List[Dict], critical_blocked: List[Dict], long_blocked: List[Dict]) -> str:
        """Determine alert level for blocked stories"""
        if len(critical_blocked) > 0:
            return 'critical'
        elif len(long_blocked) > 3:
            return 'high'
        elif len(blocked_stories) > 5:
            return 'medium'
        else:
            return 'low'

    def _calculate_quality_trends(self, stories: List[Dict]) -> Dict:
        """Calculate quality trends over time"""
        # Placeholder - would track quality metrics over time
        return {
            'bug_creation_trend': 'stable',
            'bug_resolution_trend': 'improving',
            'tech_debt_trend': 'stable'
        }

    def _analyze_bug_ages(self, bugs: List[Dict]) -> Dict:
        """Analyze how long bugs have been open"""
        open_bugs = [b for b in bugs if b.get('status') != 'done']
        ages = []

        for bug in open_bugs:
            if bug.get('created_at'):
                try:
                    created = datetime.fromisoformat(bug['created_at'])
                    age = (datetime.now() - created).days
                    ages.append(age)
                except:
                    continue

        return {
            'average_age_days': round(statistics.mean(ages), 2) if ages else 0,
            'oldest_bug_days': max(ages) if ages else 0,
            'bugs_older_than_30_days': len([a for a in ages if a > 30])
        }

    def _calculate_overall_quality_score(self, bugs: List[Dict], tech_debt: List[Dict]) -> float:
        """Calculate overall quality score (0-100)"""
        base_score = 100

        # Deduct for open critical bugs
        critical_bugs = len([b for b in bugs if b.get('severity') == 'critical' and b.get('status') != 'done'])
        base_score -= critical_bugs * 20

        # Deduct for high-risk tech debt
        high_risk_debt = len([t for t in tech_debt if t.get('risk') == 'high' and t.get('status') != 'done'])
        base_score -= high_risk_debt * 10

        # Deduct for total open bugs/debt
        open_issues = len([item for item in bugs + tech_debt if item.get('status') != 'done'])
        base_score -= open_issues * 2

        return max(0, min(100, base_score))

    # Placeholder methods for comprehensive functionality
    def _generate_velocity_recommendations(self, avg_velocity: float, trend: str) -> List[str]:
        """Generate velocity improvement recommendations"""
        recommendations = []

        if trend == 'declining':
            recommendations.append("Velocity is declining - investigate bottlenecks")
        if avg_velocity < 3:
            recommendations.append("Low velocity - consider smaller stories or remove blockers")

        return recommendations

    def _suggest_unblocking_actions(self, blocking_reasons: Dict) -> List[str]:
        """Suggest actions to unblock stories"""
        actions = []

        if 'dependency' in blocking_reasons:
            actions.append("Review and prioritize dependency stories")
        if 'resource_constraint' in blocking_reasons:
            actions.append("Assess resource allocation and capacity")

        return actions

    def _suggest_cycle_time_improvements(self, status_durations: Dict) -> List[str]:
        """Suggest improvements for cycle time"""
        return ["Consider smaller story sizes", "Review approval processes"]

    def _generate_priority_recommendations(self, priority_counts: Counter, stories: List[Dict]) -> List[str]:
        """Generate priority management recommendations"""
        recommendations = []

        total = sum(priority_counts.values())
        critical_pct = (priority_counts.get('critical', 0) / total) * 100 if total > 0 else 0

        if critical_pct > 20:
            recommendations.append("Too many critical priority items - review prioritization")

        return recommendations

    def _generate_quality_recommendations(self, bugs: List[Dict], tech_debt: List[Dict]) -> List[str]:
        """Generate quality improvement recommendations"""
        recommendations = []

        critical_bugs = len([b for b in bugs if b.get('severity') == 'critical'])
        if critical_bugs > 0:
            recommendations.append(f"Address {critical_bugs} critical bugs immediately")

        return recommendations

    def _calculate_completion_rates_by_type(self, stories: List[Dict]) -> Dict:
        """Calculate completion rates by story type"""
        type_stats = defaultdict(lambda: {'total': 0, 'completed': 0})

        for story in stories:
            story_type = story.get('type', 'feature')
            type_stats[story_type]['total'] += 1
            if story.get('status') == 'done':
                type_stats[story_type]['completed'] += 1

        return {
            story_type: {
                'completion_rate': round((stats['completed'] / stats['total']) * 100, 1) if stats['total'] > 0 else 0,
                'total': stats['total'],
                'completed': stats['completed']
            }
            for story_type, stats in type_stats.items()
        }

    def _generate_summary_insights(self) -> List[str]:
        """Generate key insights summary"""
        return [
            "Team velocity is stable with room for improvement",
            "Quality metrics are within acceptable range",
            "Some stories are blocked and need attention"
        ]

    def _generate_html_dashboard(self, report_data: Dict) -> str:
        """Generate HTML dashboard (simplified version)"""
        return f"""
<!DOCTYPE html>
<html>
<head>
    <title>Atlas Backlog Dashboard</title>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        .metric {{ background: #f5f5f5; padding: 15px; margin: 10px 0; border-radius: 5px; }}
        .alert {{ background: #ffebee; border-left: 4px solid #f44336; }}
        .success {{ background: #e8f5e8; border-left: 4px solid #4caf50; }}
        h1, h2 {{ color: #333; }}
        pre {{ background: #f9f9f9; padding: 10px; overflow-x: auto; }}
    </style>
</head>
<body>
    <h1>Atlas Backlog Dashboard</h1>
    <p>Generated: {report_data.get('generated_at', 'Unknown')}</p>

    <div class="metric">
        <h2>Velocity Metrics</h2>
        <pre>{json.dumps(report_data.get('velocity_metrics', {}), indent=2)}</pre>
    </div>

    <div class="metric">
        <h2>Quality Metrics</h2>
        <pre>{json.dumps(report_data.get('quality_metrics', {}), indent=2)}</pre>
    </div>

    <div class="metric alert">
        <h2>Blocked Stories Alert</h2>
        <pre>{json.dumps(report_data.get('blocked_stories_alert', {}), indent=2)}</pre>
    </div>

    <div class="metric">
        <h2>Summary Insights</h2>
        <ul>
        {''.join(f'<li>{insight}</li>' for insight in report_data.get('summary_insights', []))}
        </ul>
    </div>
</body>
</html>
"""

    # Additional placeholder methods
    def _get_current_wave_number(self) -> int:
        return 1

    def _load_wave_data(self, wave_number: int) -> Optional[Dict]:
        return None

    def _calculate_ideal_burndown(self, total: int, duration: int) -> List[Dict]:
        return []

    def _calculate_actual_burndown(self, wave_data: Dict) -> List[Dict]:
        return []

    def _predict_completion_date(self, actual: List, ideal: List) -> str:
        return "On track"

    def _assess_burndown_risk(self, actual: List, ideal: List) -> str:
        return "low"

    def _analyze_tech_debt_trends(self, tech_debt: List[Dict]) -> Dict:
        return {}

    def _calculate_cycle_times_by_type(self, stories: List[Dict]) -> Dict:
        return {}

    def _calculate_throughput_metrics(self, stories: List[Dict]) -> Dict:
        return {}

    def _calculate_efficiency_score(self, completion_rates: Dict, cycle_times: Dict) -> float:
        return 85.0

    def _analyze_performance_trends(self, stories: List[Dict]) -> Dict:
        return {}

    def _identify_improvement_areas(self, completion_rates: Dict, cycle_times: Dict) -> List[str]:
        return []


def main():
    """Main entry point for dashboard"""
    dashboard = BacklogDashboard()

    if len(sys.argv) < 2:
        print("""
Atlas Backlog Dashboard

Commands:
  velocity [weeks]           - Generate velocity report
  burndown [wave]           - Generate burndown chart data
  cycle-time                - Analyze cycle times
  priorities                - Priority distribution report
  blocked                   - Blocked stories alert
  quality                   - Quality metrics report
  performance               - Team performance report
  comprehensive             - Full dashboard report
  export-html [filename]    - Export dashboard as HTML

Examples:
  python backlog_dashboard.py velocity 12
  python backlog_dashboard.py comprehensive
  python backlog_dashboard.py export-html dashboard.html
""")
        return

    command = sys.argv[1]

    try:
        if command == 'velocity':
            weeks = int(sys.argv[2]) if len(sys.argv) > 2 else 8
            result = dashboard.generate_velocity_report(weeks)
            print(json.dumps(result, indent=2))

        elif command == 'burndown':
            wave = int(sys.argv[2]) if len(sys.argv) > 2 else None
            result = dashboard.generate_burndown_chart_data(wave)
            print(json.dumps(result, indent=2))

        elif command == 'cycle-time':
            result = dashboard.generate_cycle_time_analysis()
            print(json.dumps(result, indent=2))

        elif command == 'priorities':
            result = dashboard.generate_priority_distribution_report()
            print(json.dumps(result, indent=2))

        elif command == 'blocked':
            result = dashboard.generate_blocked_stories_alert()
            print(json.dumps(result, indent=2))

        elif command == 'quality':
            result = dashboard.generate_quality_metrics_report()
            print(json.dumps(result, indent=2))

        elif command == 'performance':
            result = dashboard.generate_team_performance_report()
            print(json.dumps(result, indent=2))

        elif command == 'comprehensive':
            result = dashboard.generate_comprehensive_report()
            print(json.dumps(result, indent=2))

        elif command == 'export-html':
            filename = sys.argv[2] if len(sys.argv) > 2 else None
            report = dashboard.generate_comprehensive_report()
            output_path = dashboard.export_dashboard_html(report, filename)
            print(f"Dashboard exported to: {output_path}")

        else:
            print(f"Unknown command: {command}")

    except Exception as e:
        print(f"Error: {e}")


if __name__ == "__main__":
    main()