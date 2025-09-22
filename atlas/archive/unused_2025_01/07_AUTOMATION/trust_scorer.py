#!/usr/bin/env python3
"""
Atlas Trust Scorer v2.1
Copyright 2024 Atlas Framework

Calculates developer trust scores based on historical performance metrics
to enable risk-based review scoping and adaptive quality processes.
"""

import json
import logging
import datetime
import statistics
from typing import Dict, List, Set, Optional, Any, Tuple
from dataclasses import dataclass, asdict
from pathlib import Path
from enum import Enum
import sqlite3
import hashlib

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class ReviewOutcome(Enum):
    """Possible outcomes of a review."""
    PASS = "pass"
    PASS_WITH_CONDITIONS = "pass_with_conditions"
    CONDITIONAL_PASS = "conditional_pass"
    SOFT_REJECT = "soft_reject"
    DEBT_ACCEPTED = "debt_accepted"
    REJECT = "reject"
    BLOCKED = "blocked"


class TrustCategory(Enum):
    """Trust score categories."""
    EXPERT = "expert"         # 90-100
    PROFICIENT = "proficient" # 75-89
    DEVELOPING = "developing" # 60-74
    NOVICE = "novice"        # 40-59
    RISK = "risk"            # 0-39


@dataclass
class ReviewRecord:
    """Represents a historical review record."""
    review_id: str
    developer_id: str
    timestamp: datetime.datetime
    outcome: ReviewOutcome
    cycles_count: int
    quality_score: float
    issue_count: int
    fix_time_hours: Optional[float] = None
    component_areas: List[str] = None
    change_magnitude: float = 0.0

    def __post_init__(self):
        if self.component_areas is None:
            self.component_areas = []


@dataclass
class DeveloperMetrics:
    """Aggregated metrics for a developer."""
    developer_id: str
    total_reviews: int
    pass_rate: float
    average_cycles: float
    average_quality_score: float
    consistency_score: float
    improvement_trend: float
    specialization_areas: List[str]
    recent_performance: float


@dataclass
class TrustScore:
    """Complete trust score for a developer."""
    developer_id: str
    final_score: float
    category: TrustCategory
    component_scores: Dict[str, float]
    confidence: float
    trend: str  # improving, stable, declining
    last_updated: datetime.datetime
    review_count: int
    recommendations: List[str]


class TrustScoreDatabase:
    """Manages persistent storage of trust score data."""

    def __init__(self, db_path: str = "trust_scores.db"):
        self.db_path = db_path
        self._initialize_database()

    def _initialize_database(self):
        """Initialize the trust score database schema."""
        with sqlite3.connect(self.db_path) as conn:
            conn.executescript('''
                CREATE TABLE IF NOT EXISTS review_records (
                    review_id TEXT PRIMARY KEY,
                    developer_id TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    outcome TEXT NOT NULL,
                    cycles_count INTEGER NOT NULL,
                    quality_score REAL NOT NULL,
                    issue_count INTEGER NOT NULL,
                    fix_time_hours REAL,
                    component_areas TEXT,
                    change_magnitude REAL DEFAULT 0.0
                );

                CREATE TABLE IF NOT EXISTS trust_scores (
                    developer_id TEXT PRIMARY KEY,
                    final_score REAL NOT NULL,
                    category TEXT NOT NULL,
                    component_scores TEXT,
                    confidence REAL NOT NULL,
                    trend TEXT NOT NULL,
                    last_updated TEXT NOT NULL,
                    review_count INTEGER NOT NULL,
                    recommendations TEXT
                );

                CREATE INDEX IF NOT EXISTS idx_developer_timestamp
                ON review_records(developer_id, timestamp);
            ''')

    def save_review_record(self, record: ReviewRecord):
        """Save a review record to the database."""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute('''
                INSERT OR REPLACE INTO review_records
                (review_id, developer_id, timestamp, outcome, cycles_count,
                 quality_score, issue_count, fix_time_hours, component_areas, change_magnitude)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                record.review_id,
                record.developer_id,
                record.timestamp.isoformat(),
                record.outcome.value,
                record.cycles_count,
                record.quality_score,
                record.issue_count,
                record.fix_time_hours,
                json.dumps(record.component_areas),
                record.change_magnitude
            ))

    def get_review_records(self, developer_id: str,
                          lookback_days: int = 90) -> List[ReviewRecord]:
        """Get review records for a developer within the lookback period."""
        cutoff_date = datetime.datetime.now() - datetime.timedelta(days=lookback_days)

        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.execute('''
                SELECT review_id, developer_id, timestamp, outcome, cycles_count,
                       quality_score, issue_count, fix_time_hours, component_areas, change_magnitude
                FROM review_records
                WHERE developer_id = ? AND timestamp >= ?
                ORDER BY timestamp DESC
            ''', (developer_id, cutoff_date.isoformat()))

            records = []
            for row in cursor:
                records.append(ReviewRecord(
                    review_id=row[0],
                    developer_id=row[1],
                    timestamp=datetime.datetime.fromisoformat(row[2]),
                    outcome=ReviewOutcome(row[3]),
                    cycles_count=row[4],
                    quality_score=row[5],
                    issue_count=row[6],
                    fix_time_hours=row[7],
                    component_areas=json.loads(row[8]) if row[8] else [],
                    change_magnitude=row[9] if row[9] else 0.0
                ))

            return records

    def save_trust_score(self, trust_score: TrustScore):
        """Save a calculated trust score."""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute('''
                INSERT OR REPLACE INTO trust_scores
                (developer_id, final_score, category, component_scores, confidence,
                 trend, last_updated, review_count, recommendations)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                trust_score.developer_id,
                trust_score.final_score,
                trust_score.category.value,
                json.dumps(trust_score.component_scores),
                trust_score.confidence,
                trust_score.trend,
                trust_score.last_updated.isoformat(),
                trust_score.review_count,
                json.dumps(trust_score.recommendations)
            ))

    def get_trust_score(self, developer_id: str) -> Optional[TrustScore]:
        """Get the latest trust score for a developer."""
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.execute('''
                SELECT developer_id, final_score, category, component_scores, confidence,
                       trend, last_updated, review_count, recommendations
                FROM trust_scores WHERE developer_id = ?
            ''', (developer_id,))

            row = cursor.fetchone()
            if row:
                return TrustScore(
                    developer_id=row[0],
                    final_score=row[1],
                    category=TrustCategory(row[2]),
                    component_scores=json.loads(row[3]),
                    confidence=row[4],
                    trend=row[5],
                    last_updated=datetime.datetime.fromisoformat(row[6]),
                    review_count=row[7],
                    recommendations=json.loads(row[8])
                )
            return None


class TrustScoreCalculator:
    """Calculates trust scores based on historical review performance."""

    def __init__(self, db_path: str = "trust_scores.db"):
        self.db = TrustScoreDatabase(db_path)

    def calculate_trust_score(self, developer_id: str,
                            lookback_days: int = 90) -> TrustScore:
        """Calculate comprehensive trust score for a developer."""

        # Get historical records
        records = self.db.get_review_records(developer_id, lookback_days)

        if not records:
            return self._create_default_trust_score(developer_id)

        # Calculate component metrics
        metrics = self._calculate_developer_metrics(records)

        # Calculate component scores
        pass_rate_score = self._calculate_pass_rate_score(metrics)
        quality_score = self._calculate_quality_score(metrics)
        consistency_score = self._calculate_consistency_score(records)
        cycle_efficiency_score = self._calculate_cycle_efficiency_score(metrics)
        improvement_score = self._calculate_improvement_score(records)

        # Calculate weighted final score
        final_score = (
            pass_rate_score * 0.35 +      # Primary indicator of quality
            quality_score * 0.25 +        # Average review quality
            cycle_efficiency_score * 0.20 + # Efficiency in iterations
            consistency_score * 0.15 +    # Consistency across reviews
            improvement_score * 0.05      # Recent improvement trend
        )

        # Determine category
        category = self._determine_trust_category(final_score)

        # Calculate component-specific scores
        component_scores = self._calculate_component_scores(records)

        # Calculate confidence based on data quality
        confidence = self._calculate_confidence(records, lookback_days)

        # Determine trend
        trend = self._determine_trend(records)

        # Generate recommendations
        recommendations = self._generate_recommendations(
            metrics, final_score, category, records
        )

        trust_score = TrustScore(
            developer_id=developer_id,
            final_score=final_score,
            category=category,
            component_scores=component_scores,
            confidence=confidence,
            trend=trend,
            last_updated=datetime.datetime.now(),
            review_count=len(records),
            recommendations=recommendations
        )

        # Save to database
        self.db.save_trust_score(trust_score)

        return trust_score

    def _create_default_trust_score(self, developer_id: str) -> TrustScore:
        """Create default trust score for developers with no history."""
        return TrustScore(
            developer_id=developer_id,
            final_score=0.65,  # Conservative default
            category=TrustCategory.DEVELOPING,
            component_scores={},
            confidence=0.0,
            trend="unknown",
            last_updated=datetime.datetime.now(),
            review_count=0,
            recommendations=[
                "No review history available",
                "Start with full reviews to establish baseline",
                "Focus on building consistent quality patterns"
            ]
        )

    def _calculate_developer_metrics(self, records: List[ReviewRecord]) -> DeveloperMetrics:
        """Calculate aggregated metrics from review records."""
        if not records:
            return DeveloperMetrics(
                developer_id="",
                total_reviews=0,
                pass_rate=0.0,
                average_cycles=0.0,
                average_quality_score=0.0,
                consistency_score=0.0,
                improvement_trend=0.0,
                specialization_areas=[],
                recent_performance=0.0
            )

        # Calculate pass rate
        positive_outcomes = {ReviewOutcome.PASS, ReviewOutcome.PASS_WITH_CONDITIONS,
                           ReviewOutcome.CONDITIONAL_PASS, ReviewOutcome.DEBT_ACCEPTED}
        passes = sum(1 for r in records if r.outcome in positive_outcomes)
        pass_rate = passes / len(records)

        # Calculate average cycles
        average_cycles = statistics.mean(r.cycles_count for r in records)

        # Calculate average quality score
        average_quality_score = statistics.mean(r.quality_score for r in records)

        # Calculate consistency (inverse of standard deviation)
        quality_scores = [r.quality_score for r in records]
        if len(quality_scores) > 1:
            quality_std = statistics.stdev(quality_scores)
            consistency_score = max(0.0, 1.0 - quality_std / 100.0)
        else:
            consistency_score = 1.0

        # Determine specialization areas
        component_frequency = {}
        for record in records:
            for component in record.component_areas:
                component_frequency[component] = component_frequency.get(component, 0) + 1

        specialization_areas = [
            component for component, count in component_frequency.items()
            if count >= len(records) * 0.3  # Component appears in 30%+ of reviews
        ]

        # Calculate recent performance (last 30% of reviews)
        recent_count = max(1, len(records) // 3)
        recent_records = records[:recent_count]
        recent_quality = statistics.mean(r.quality_score for r in recent_records)
        recent_performance = recent_quality / 100.0

        # Calculate improvement trend
        if len(records) >= 5:
            # Compare first and last terciles
            tercile_size = len(records) // 3
            early_scores = [r.quality_score for r in records[-tercile_size:]]
            late_scores = [r.quality_score for r in records[:tercile_size]]
            improvement_trend = statistics.mean(late_scores) - statistics.mean(early_scores)
        else:
            improvement_trend = 0.0

        return DeveloperMetrics(
            developer_id=records[0].developer_id,
            total_reviews=len(records),
            pass_rate=pass_rate,
            average_cycles=average_cycles,
            average_quality_score=average_quality_score,
            consistency_score=consistency_score,
            improvement_trend=improvement_trend,
            specialization_areas=specialization_areas,
            recent_performance=recent_performance
        )

    def _calculate_pass_rate_score(self, metrics: DeveloperMetrics) -> float:
        """Calculate score based on pass rate (0.0 to 1.0)."""
        # Pass rate with diminishing returns
        return min(metrics.pass_rate * 1.2, 1.0)

    def _calculate_quality_score(self, metrics: DeveloperMetrics) -> float:
        """Calculate score based on average quality (0.0 to 1.0)."""
        # Normalize quality score from 0-100 to 0-1
        return min(metrics.average_quality_score / 100.0, 1.0)

    def _calculate_consistency_score(self, records: List[ReviewRecord]) -> float:
        """Calculate consistency score based on quality variance (0.0 to 1.0)."""
        if len(records) < 3:
            return 1.0  # Can't measure consistency with few samples

        quality_scores = [r.quality_score for r in records]
        mean_quality = statistics.mean(quality_scores)

        # Calculate coefficient of variation
        if mean_quality > 0:
            std_dev = statistics.stdev(quality_scores)
            cv = std_dev / mean_quality
            # Convert to consistency score (lower CV = higher consistency)
            consistency = max(0.0, 1.0 - cv)
        else:
            consistency = 0.0

        return consistency

    def _calculate_cycle_efficiency_score(self, metrics: DeveloperMetrics) -> float:
        """Calculate score based on review cycle efficiency (0.0 to 1.0)."""
        # Ideal is 1 cycle, score decreases with more cycles
        if metrics.average_cycles <= 1.0:
            return 1.0
        elif metrics.average_cycles <= 2.0:
            return 0.8
        elif metrics.average_cycles <= 3.0:
            return 0.6
        elif metrics.average_cycles <= 4.0:
            return 0.4
        else:
            return 0.2

    def _calculate_improvement_score(self, records: List[ReviewRecord]) -> float:
        """Calculate score based on improvement trend (0.0 to 1.0)."""
        if len(records) < 5:
            return 0.5  # Neutral for insufficient data

        # Calculate trend over time
        timestamps = [r.timestamp.timestamp() for r in reversed(records)]
        quality_scores = [r.quality_score for r in reversed(records)]

        # Simple linear regression slope
        n = len(timestamps)
        sum_x = sum(timestamps)
        sum_y = sum(quality_scores)
        sum_xy = sum(timestamps[i] * quality_scores[i] for i in range(n))
        sum_x2 = sum(x * x for x in timestamps)

        denominator = n * sum_x2 - sum_x * sum_x
        if denominator == 0:
            return 0.5

        slope = (n * sum_xy - sum_x * sum_y) / denominator

        # Normalize slope to 0-1 score
        # Positive slope (improving) gives > 0.5, negative gives < 0.5
        normalized_slope = 0.5 + (slope / 10.0)  # Adjust divisor based on typical slope range
        return max(0.0, min(1.0, normalized_slope))

    def _determine_trust_category(self, score: float) -> TrustCategory:
        """Determine trust category based on final score."""
        if score >= 0.90:
            return TrustCategory.EXPERT
        elif score >= 0.75:
            return TrustCategory.PROFICIENT
        elif score >= 0.60:
            return TrustCategory.DEVELOPING
        elif score >= 0.40:
            return TrustCategory.NOVICE
        else:
            return TrustCategory.RISK

    def _calculate_component_scores(self, records: List[ReviewRecord]) -> Dict[str, float]:
        """Calculate trust scores for specific components."""
        component_scores = {}

        # Group records by component
        component_records = {}
        for record in records:
            for component in record.component_areas:
                if component not in component_records:
                    component_records[component] = []
                component_records[component].append(record)

        # Calculate score for each component
        for component, comp_records in component_records.items():
            if len(comp_records) >= 3:  # Need minimum samples
                comp_metrics = self._calculate_developer_metrics(comp_records)
                component_score = (
                    comp_metrics.pass_rate * 0.4 +
                    comp_metrics.average_quality_score / 100.0 * 0.3 +
                    comp_metrics.consistency_score * 0.3
                )
                component_scores[component] = component_score

        return component_scores

    def _calculate_confidence(self, records: List[ReviewRecord],
                            lookback_days: int) -> float:
        """Calculate confidence in the trust score (0.0 to 1.0)."""
        # Base confidence on number of reviews and recency
        review_count_factor = min(len(records) / 10.0, 1.0)  # 10+ reviews = max confidence

        # Recency factor - more recent reviews increase confidence
        if records:
            days_since_last = (datetime.datetime.now() - records[0].timestamp).days
            recency_factor = max(0.1, 1.0 - days_since_last / lookback_days)
        else:
            recency_factor = 0.0

        # Time span factor - reviews spread over time increase confidence
        if len(records) > 1:
            time_span_days = (records[0].timestamp - records[-1].timestamp).days
            span_factor = min(time_span_days / 30.0, 1.0)  # 30+ days = max confidence
        else:
            span_factor = 0.5

        confidence = (review_count_factor * 0.5 + recency_factor * 0.3 + span_factor * 0.2)
        return confidence

    def _determine_trend(self, records: List[ReviewRecord]) -> str:
        """Determine if performance is improving, stable, or declining."""
        if len(records) < 4:
            return "insufficient_data"

        # Compare recent vs historical performance
        recent_count = max(2, len(records) // 4)
        recent_records = records[:recent_count]
        historical_records = records[recent_count:]

        recent_avg = statistics.mean(r.quality_score for r in recent_records)
        historical_avg = statistics.mean(r.quality_score for r in historical_records)

        diff = recent_avg - historical_avg

        if diff > 5:
            return "improving"
        elif diff < -5:
            return "declining"
        else:
            return "stable"

    def _generate_recommendations(self, metrics: DeveloperMetrics,
                                final_score: float, category: TrustCategory,
                                records: List[ReviewRecord]) -> List[str]:
        """Generate actionable recommendations for the developer."""
        recommendations = []

        # Category-based recommendations
        if category == TrustCategory.EXPERT:
            recommendations.append("Excellent track record - consider mentoring others")
            recommendations.append("Eligible for minimal review scoping")
        elif category == TrustCategory.PROFICIENT:
            recommendations.append("Strong performance - eligible for targeted reviews")
        elif category == TrustCategory.DEVELOPING:
            recommendations.append("Good progress - focus on consistency improvement")
        elif category == TrustCategory.NOVICE:
            recommendations.append("Building skills - benefit from comprehensive reviews")
        else:  # RISK
            recommendations.append("Performance concerns - requires enhanced oversight")

        # Specific improvement areas
        if metrics.pass_rate < 0.7:
            recommendations.append("Focus on improving first-pass quality")

        if metrics.average_cycles > 2.5:
            recommendations.append("Work on reducing review cycles through better preparation")

        if metrics.consistency_score < 0.7:
            recommendations.append("Improve consistency across different types of changes")

        # Component-specific recommendations
        if len(metrics.specialization_areas) > 0:
            recommendations.append(f"Strong in: {', '.join(metrics.specialization_areas)}")

        # Recent performance
        if records and len(records) >= 3:
            recent_quality = statistics.mean(r.quality_score for r in records[:3])
            if recent_quality < metrics.average_quality_score - 10:
                recommendations.append("Recent performance below average - may need support")

        return recommendations

    def get_trust_score(self, developer_id: str,
                       max_age_days: int = 7) -> Optional[TrustScore]:
        """Get cached trust score if recent enough, otherwise calculate new."""
        cached_score = self.db.get_trust_score(developer_id)

        if cached_score:
            age = datetime.datetime.now() - cached_score.last_updated
            if age.days <= max_age_days:
                return cached_score

        # Calculate new score
        return self.calculate_trust_score(developer_id)

    def record_review(self, review_record: ReviewRecord):
        """Record a new review result for future trust score calculations."""
        self.db.save_review_record(review_record)
        logger.info(f"Recorded review {review_record.review_id} for {review_record.developer_id}")

    def export_trust_report(self, developer_id: str, output_path: str):
        """Export comprehensive trust report for a developer."""
        trust_score = self.get_trust_score(developer_id)
        records = self.db.get_review_records(developer_id, 365)  # Full year

        if not trust_score:
            logger.error(f"No trust score found for developer {developer_id}")
            return

        report = {
            "developer_id": developer_id,
            "trust_score": asdict(trust_score),
            "historical_data": {
                "total_reviews": len(records),
                "review_outcomes": {outcome.value: sum(1 for r in records if r.outcome == outcome)
                                  for outcome in ReviewOutcome},
                "quality_trend": [r.quality_score for r in reversed(records[-20:])],  # Last 20
                "cycle_trend": [r.cycles_count for r in reversed(records[-20:])]
            },
            "generated_at": datetime.datetime.now().isoformat()
        }

        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2, default=str)

        logger.info(f"Trust report exported to {output_path}")


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Atlas Trust Score Calculator")
    parser.add_argument("command", choices=["calculate", "record", "report"],
                       help="Command to execute")
    parser.add_argument("--developer-id", required=True, help="Developer ID")
    parser.add_argument("--lookback-days", type=int, default=90,
                       help="Days to look back for calculation")
    parser.add_argument("--output", help="Output file path")
    parser.add_argument("--db-path", default="trust_scores.db", help="Database path")

    # For record command
    parser.add_argument("--review-id", help="Review ID")
    parser.add_argument("--outcome", choices=[o.value for o in ReviewOutcome],
                       help="Review outcome")
    parser.add_argument("--cycles", type=int, help="Number of review cycles")
    parser.add_argument("--quality-score", type=float, help="Quality score (0-100)")
    parser.add_argument("--issue-count", type=int, help="Number of issues found")

    args = parser.parse_args()

    calculator = TrustScoreCalculator(args.db_path)

    if args.command == "calculate":
        trust_score = calculator.calculate_trust_score(args.developer_id, args.lookback_days)

        print(f"\n=== Trust Score for {args.developer_id} ===")
        print(f"Final Score: {trust_score.final_score:.2f}")
        print(f"Category: {trust_score.category.value}")
        print(f"Confidence: {trust_score.confidence:.2f}")
        print(f"Trend: {trust_score.trend}")
        print(f"Reviews: {trust_score.review_count}")
        print(f"Component Scores: {trust_score.component_scores}")
        print(f"Recommendations:")
        for rec in trust_score.recommendations:
            print(f"  - {rec}")

        if args.output:
            with open(args.output, 'w') as f:
                json.dump(asdict(trust_score), f, indent=2, default=str)
            print(f"\nTrust score saved to {args.output}")

    elif args.command == "record":
        if not all([args.review_id, args.outcome, args.cycles,
                   args.quality_score, args.issue_count]):
            print("Error: record command requires --review-id, --outcome, --cycles, "
                  "--quality-score, and --issue-count")
            exit(1)

        record = ReviewRecord(
            review_id=args.review_id,
            developer_id=args.developer_id,
            timestamp=datetime.datetime.now(),
            outcome=ReviewOutcome(args.outcome),
            cycles_count=args.cycles,
            quality_score=args.quality_score,
            issue_count=args.issue_count
        )

        calculator.record_review(record)
        print(f"Review record saved for {args.developer_id}")

    elif args.command == "report":
        if not args.output:
            print("Error: report command requires --output")
            exit(1)

        calculator.export_trust_report(args.developer_id, args.output)
        print(f"Trust report generated: {args.output}")

    else:
        print("Unknown command")