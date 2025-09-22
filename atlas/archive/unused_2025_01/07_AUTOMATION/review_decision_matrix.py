#!/usr/bin/env python3
"""
Atlas Review Decision Matrix v2.1
Copyright 2024 Atlas Framework

Implements graduated review severity levels and intelligent decision making
for Atlas review processes. Supports nuanced verdicts beyond simple pass/fail.
"""

import json
import logging
import datetime
from typing import Dict, List, Set, Optional, Any, Tuple
from dataclasses import dataclass, asdict
from pathlib import Path
from enum import Enum

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class ReviewVerdict(Enum):
    """Enhanced review verdicts with graduated severity."""
    PASS = "pass"                           # Full approval, ready to proceed
    PASS_WITH_CONDITIONS = "pass_with_conditions"  # Approved with post-merge requirements
    CONDITIONAL_PASS = "conditional_pass"   # Approved if conditions met within timeframe
    SOFT_REJECT = "soft_reject"            # Partial acceptance, specific rework needed
    DEBT_ACCEPTED = "debt_accepted"        # Technical debt explicitly documented
    REJECT = "reject"                      # Full rejection, significant rework required
    BLOCKED = "blocked"                    # Critical issues, development must stop


class ReviewType(Enum):
    """Types of reviews in the Atlas process."""
    DESIGN_REVIEW = "design_review"
    CODE_REVIEW = "code_review"
    SECURITY_REVIEW = "security_review"
    PERFORMANCE_REVIEW = "performance_review"
    UX_REVIEW = "ux_review"
    INTEGRATION_REVIEW = "integration_review"
    DEPLOYMENT_REVIEW = "deployment_review"


class IssueSeverity(Enum):
    """Issue severity levels."""
    BLOCKED = "blocked"
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"
    INFO = "info"


class IssueCategory(Enum):
    """Categories of review issues."""
    FUNCTIONAL = "functional"
    SECURITY = "security"
    PERFORMANCE = "performance"
    MAINTAINABILITY = "maintainability"
    COMPLIANCE = "compliance"
    USABILITY = "usability"
    DOCUMENTATION = "documentation"
    TESTING = "testing"


@dataclass
class ReviewIssue:
    """Represents an issue found during review."""
    id: str
    title: str
    description: str
    severity: IssueSeverity
    category: IssueCategory
    location: str  # File, function, or component
    suggested_fix: Optional[str] = None
    effort_estimate_hours: Optional[float] = None
    blocking_reason: Optional[str] = None
    can_be_deferred: bool = False
    requires_expert_review: bool = False
    affects_critical_path: bool = False


@dataclass
class ConditionalRequirement:
    """Represents a condition that must be met for conditional approval."""
    id: str
    description: str
    deadline_hours: int
    verification_method: str  # automated_test, manual_check, expert_review
    responsible_party: str
    escalation_contact: str


@dataclass
class TechnicalDebtItem:
    """Represents accepted technical debt."""
    id: str
    description: str
    rationale: str
    estimated_effort_hours: float
    priority: str  # high, medium, low
    target_resolution_sprint: Optional[str] = None
    impact_assessment: str = ""
    monitoring_required: bool = False


@dataclass
class ReviewDecision:
    """Represents the final review decision."""
    verdict: ReviewVerdict
    summary: str
    issues_found: List[ReviewIssue]
    conditions: List[ConditionalRequirement] = None
    technical_debt: List[TechnicalDebtItem] = None
    estimated_fix_effort_hours: float = 0.0
    recommended_next_steps: List[str] = None
    escalation_required: bool = False
    follow_up_review_needed: bool = False

    def __post_init__(self):
        if self.conditions is None:
            self.conditions = []
        if self.technical_debt is None:
            self.technical_debt = []
        if self.recommended_next_steps is None:
            self.recommended_next_steps = []


class ReviewDecisionEngine:
    """Engine for making intelligent review decisions based on issues and context."""

    def __init__(self, config_path: Optional[str] = None):
        self.config = self._load_config(config_path)
        self.decision_rules = self._initialize_decision_rules()

    def _load_config(self, config_path: Optional[str]) -> Dict[str, Any]:
        """Load configuration for decision making."""
        default_config = {
            "severity_thresholds": {
                "blocked_threshold": 1,  # Any blocked issue = blocked verdict
                "reject_threshold": 3,   # 3+ high severity = reject
                "soft_reject_threshold": 5,  # 5+ medium severity = soft reject
                "conditional_pass_max_effort": 8,  # Max 8 hours of conditions
                "debt_acceptance_max_effort": 16  # Max 16 hours of debt
            },
            "category_weights": {
                "security": 2.0,     # Security issues weighted higher
                "functional": 1.8,   # Functional issues critical
                "performance": 1.5,  # Performance moderately important
                "compliance": 2.0,   # Compliance critical
                "maintainability": 1.0,  # Standard weight
                "usability": 1.2,    # Slightly above standard
                "documentation": 0.8,  # Lower priority
                "testing": 1.3       # Important for quality
            },
            "conditional_pass_criteria": {
                "max_conditions": 5,
                "max_total_effort_hours": 8,
                "max_deadline_hours": 72,
                "requires_automated_verification": True
            },
            "debt_acceptance_criteria": {
                "max_debt_items": 3,
                "max_total_effort_hours": 16,
                "requires_explicit_rationale": True,
                "requires_target_sprint": True
            }
        }

        if config_path and Path(config_path).exists():
            try:
                with open(config_path, 'r') as f:
                    user_config = json.load(f)
                default_config.update(user_config)
            except Exception as e:
                logger.warning(f"Failed to load config from {config_path}: {e}")

        return default_config

    def _initialize_decision_rules(self) -> Dict[str, Any]:
        """Initialize decision rules based on configuration."""
        return {
            "auto_block_conditions": [
                lambda issues: any(issue.severity == IssueSeverity.BLOCKED for issue in issues),
                lambda issues: any(issue.category == IssueCategory.SECURITY and
                                 issue.severity == IssueSeverity.HIGH for issue in issues),
                lambda issues: any(issue.affects_critical_path and
                                 issue.severity in [IssueSeverity.HIGH, IssueSeverity.BLOCKED]
                                 for issue in issues)
            ],
            "auto_reject_conditions": [
                lambda issues: self._count_weighted_severity(issues, IssueSeverity.HIGH) >= 3,
                lambda issues: self._count_issues_by_category(issues, IssueCategory.FUNCTIONAL) >= 5,
                lambda issues: self._calculate_total_effort(issues) > 40
            ],
            "soft_reject_conditions": [
                lambda issues: self._count_weighted_severity(issues, IssueSeverity.MEDIUM) >= 5,
                lambda issues: self._count_issues_by_category(issues, IssueCategory.PERFORMANCE) >= 3,
                lambda issues: len([i for i in issues if not i.can_be_deferred]) > 8
            ],
            "conditional_pass_eligible": [
                lambda issues: all(issue.severity != IssueSeverity.BLOCKED for issue in issues),
                lambda issues: self._calculate_total_effort(issues) <= 8,
                lambda issues: all(issue.can_be_deferred or issue.effort_estimate_hours <= 4
                                 for issue in issues)
            ],
            "debt_acceptance_eligible": [
                lambda issues: all(issue.severity in [IssueSeverity.LOW, IssueSeverity.MEDIUM]
                                 for issue in issues),
                lambda issues: all(issue.category in [IssueCategory.MAINTAINABILITY,
                                                     IssueCategory.DOCUMENTATION,
                                                     IssueCategory.TESTING] for issue in issues),
                lambda issues: self._calculate_total_effort(issues) <= 16
            ]
        }

    def make_review_decision(self, issues: List[ReviewIssue],
                           review_type: ReviewType,
                           context: Optional[Dict[str, Any]] = None) -> ReviewDecision:
        """Make an intelligent review decision based on issues and context."""

        if not issues:
            return ReviewDecision(
                verdict=ReviewVerdict.PASS,
                summary="No issues found. Review passed.",
                issues_found=[]
            )

        # Sort issues by severity and impact
        sorted_issues = self._prioritize_issues(issues)

        # Apply decision rules in order of priority
        if self._should_block(sorted_issues):
            return self._create_blocked_decision(sorted_issues)

        if self._should_reject(sorted_issues):
            return self._create_reject_decision(sorted_issues)

        if self._should_soft_reject(sorted_issues):
            return self._create_soft_reject_decision(sorted_issues)

        if self._can_conditional_pass(sorted_issues):
            return self._create_conditional_pass_decision(sorted_issues)

        if self._can_accept_as_debt(sorted_issues):
            return self._create_debt_accepted_decision(sorted_issues)

        # Default to pass with conditions if effort is manageable
        if self._calculate_total_effort(sorted_issues) <= 4:
            return self._create_pass_with_conditions_decision(sorted_issues)

        # Fall back to soft reject
        return self._create_soft_reject_decision(sorted_issues)

    def _prioritize_issues(self, issues: List[ReviewIssue]) -> List[ReviewIssue]:
        """Sort issues by priority (severity, category weight, critical path impact)."""
        def priority_score(issue: ReviewIssue) -> float:
            severity_scores = {
                IssueSeverity.BLOCKED: 100,
                IssueSeverity.HIGH: 50,
                IssueSeverity.MEDIUM: 25,
                IssueSeverity.LOW: 10,
                IssueSeverity.INFO: 1
            }

            category_weight = self.config["category_weights"].get(issue.category.value, 1.0)
            critical_path_bonus = 20 if issue.affects_critical_path else 0

            return (severity_scores.get(issue.severity, 1) * category_weight +
                   critical_path_bonus)

        return sorted(issues, key=priority_score, reverse=True)

    def _should_block(self, issues: List[ReviewIssue]) -> bool:
        """Check if review should be blocked."""
        return any(condition(issues) for condition in self.decision_rules["auto_block_conditions"])

    def _should_reject(self, issues: List[ReviewIssue]) -> bool:
        """Check if review should be rejected."""
        return any(condition(issues) for condition in self.decision_rules["auto_reject_conditions"])

    def _should_soft_reject(self, issues: List[ReviewIssue]) -> bool:
        """Check if review should be soft rejected."""
        return any(condition(issues) for condition in self.decision_rules["soft_reject_conditions"])

    def _can_conditional_pass(self, issues: List[ReviewIssue]) -> bool:
        """Check if review can be conditionally passed."""
        return all(condition(issues) for condition in self.decision_rules["conditional_pass_eligible"])

    def _can_accept_as_debt(self, issues: List[ReviewIssue]) -> bool:
        """Check if issues can be accepted as technical debt."""
        return all(condition(issues) for condition in self.decision_rules["debt_acceptance_eligible"])

    def _count_weighted_severity(self, issues: List[ReviewIssue], severity: IssueSeverity) -> int:
        """Count issues of given severity with category weighting."""
        count = 0
        for issue in issues:
            if issue.severity == severity:
                weight = self.config["category_weights"].get(issue.category.value, 1.0)
                count += weight
        return int(count)

    def _count_issues_by_category(self, issues: List[ReviewIssue], category: IssueCategory) -> int:
        """Count issues in a specific category."""
        return len([issue for issue in issues if issue.category == category])

    def _calculate_total_effort(self, issues: List[ReviewIssue]) -> float:
        """Calculate total estimated effort to fix all issues."""
        return sum(issue.effort_estimate_hours or 2.0 for issue in issues)

    def _create_blocked_decision(self, issues: List[ReviewIssue]) -> ReviewDecision:
        """Create a blocked review decision."""
        blocking_issues = [issue for issue in issues if issue.severity == IssueSeverity.BLOCKED]

        return ReviewDecision(
            verdict=ReviewVerdict.BLOCKED,
            summary=f"Review blocked due to {len(blocking_issues)} critical issues. "
                   f"Development must stop until resolved.",
            issues_found=issues,
            escalation_required=True,
            recommended_next_steps=[
                "Stop all development on affected components",
                "Immediate root cause analysis required",
                "Emergency escalation to technical lead",
                "Create detailed remediation plan within 24 hours"
            ]
        )

    def _create_reject_decision(self, issues: List[ReviewIssue]) -> ReviewDecision:
        """Create a reject review decision."""
        high_severity_count = len([i for i in issues if i.severity == IssueSeverity.HIGH])
        total_effort = self._calculate_total_effort(issues)

        return ReviewDecision(
            verdict=ReviewVerdict.REJECT,
            summary=f"Review rejected due to {high_severity_count} high-severity issues. "
                   f"Estimated {total_effort:.1f} hours of rework required.",
            issues_found=issues,
            estimated_fix_effort_hours=total_effort,
            follow_up_review_needed=True,
            recommended_next_steps=[
                "Address all high and medium severity issues",
                "Focus on functional and security issues first",
                "Request re-review after fixes are implemented",
                "Consider breaking down large changes into smaller parts"
            ]
        )

    def _create_soft_reject_decision(self, issues: List[ReviewIssue]) -> ReviewDecision:
        """Create a soft reject decision with partial acceptance."""
        acceptable_issues = [i for i in issues if i.severity in [IssueSeverity.LOW, IssueSeverity.INFO]]
        must_fix_issues = [i for i in issues if i not in acceptable_issues]

        # Convert acceptable issues to technical debt
        debt_items = []
        for issue in acceptable_issues:
            if issue.category in [IssueCategory.MAINTAINABILITY, IssueCategory.DOCUMENTATION]:
                debt_items.append(TechnicalDebtItem(
                    id=f"debt_{issue.id}",
                    description=issue.description,
                    rationale=f"Acceptable technical debt from review - {issue.category.value} issue",
                    estimated_effort_hours=issue.effort_estimate_hours or 2.0,
                    priority="medium" if issue.severity == IssueSeverity.MEDIUM else "low"
                ))

        return ReviewDecision(
            verdict=ReviewVerdict.SOFT_REJECT,
            summary=f"Partial acceptance: {len(must_fix_issues)} issues must be fixed, "
                   f"{len(debt_items)} items accepted as technical debt.",
            issues_found=must_fix_issues,
            technical_debt=debt_items,
            estimated_fix_effort_hours=self._calculate_total_effort(must_fix_issues),
            follow_up_review_needed=True,
            recommended_next_steps=[
                "Fix high and medium priority issues",
                "Technical debt items added to backlog",
                "Partial re-review required for fixed issues",
                "Consider impact on release timeline"
            ]
        )

    def _create_conditional_pass_decision(self, issues: List[ReviewIssue]) -> ReviewDecision:
        """Create a conditional pass decision."""
        conditions = []

        for issue in issues:
            if issue.effort_estimate_hours and issue.effort_estimate_hours <= 4:
                condition = ConditionalRequirement(
                    id=f"condition_{issue.id}",
                    description=f"Fix {issue.title}: {issue.description}",
                    deadline_hours=min(72, max(8, int(issue.effort_estimate_hours * 2))),
                    verification_method="automated_test" if issue.category == IssueCategory.TESTING
                                     else "manual_check",
                    responsible_party="development_team",
                    escalation_contact="technical_lead"
                )
                conditions.append(condition)

        return ReviewDecision(
            verdict=ReviewVerdict.CONDITIONAL_PASS,
            summary=f"Conditionally approved with {len(conditions)} conditions. "
                   f"Fix within {max(c.deadline_hours for c in conditions)} hours.",
            issues_found=issues,
            conditions=conditions,
            estimated_fix_effort_hours=self._calculate_total_effort(issues),
            follow_up_review_needed=False,
            recommended_next_steps=[
                "Address all conditions before merge",
                "Automated verification will track progress",
                "Escalate if conditions cannot be met within deadline",
                "Proceed with merge once all conditions satisfied"
            ]
        )

    def _create_debt_accepted_decision(self, issues: List[ReviewIssue]) -> ReviewDecision:
        """Create a debt accepted decision."""
        debt_items = []

        for issue in issues:
            debt_items.append(TechnicalDebtItem(
                id=f"debt_{issue.id}",
                description=issue.description,
                rationale=f"Accepted as technical debt - {issue.category.value} improvement",
                estimated_effort_hours=issue.effort_estimate_hours or 2.0,
                priority="medium" if issue.severity == IssueSeverity.MEDIUM else "low",
                impact_assessment=f"Low impact {issue.category.value} issue",
                monitoring_required=issue.category in [IssueCategory.PERFORMANCE, IssueCategory.SECURITY]
            ))

        return ReviewDecision(
            verdict=ReviewVerdict.DEBT_ACCEPTED,
            summary=f"Approved with {len(debt_items)} technical debt items. "
                   f"Total debt: {sum(d.estimated_effort_hours for d in debt_items):.1f} hours.",
            issues_found=issues,
            technical_debt=debt_items,
            follow_up_review_needed=False,
            recommended_next_steps=[
                "Technical debt items added to product backlog",
                "Monitor debt accumulation in future sprints",
                "Schedule debt reduction activities",
                "Proceed with feature delivery"
            ]
        )

    def _create_pass_with_conditions_decision(self, issues: List[ReviewIssue]) -> ReviewDecision:
        """Create a pass with conditions decision for minor issues."""
        conditions = []

        # Create post-merge fix requirements
        for issue in issues:
            if issue.severity in [IssueSeverity.LOW, IssueSeverity.MEDIUM]:
                condition = ConditionalRequirement(
                    id=f"postmerge_{issue.id}",
                    description=f"Post-merge fix: {issue.description}",
                    deadline_hours=168,  # 1 week
                    verification_method="manual_check",
                    responsible_party="development_team",
                    escalation_contact="team_lead"
                )
                conditions.append(condition)

        return ReviewDecision(
            verdict=ReviewVerdict.PASS_WITH_CONDITIONS,
            summary=f"Approved with {len(conditions)} post-merge conditions. "
                   f"Minor issues to be addressed after merge.",
            issues_found=issues,
            conditions=conditions,
            estimated_fix_effort_hours=self._calculate_total_effort(issues),
            follow_up_review_needed=False,
            recommended_next_steps=[
                "Proceed with merge",
                "Address post-merge conditions within deadlines",
                "Track condition completion in project management tool",
                "Include fixes in next sprint planning"
            ]
        )

    def validate_decision(self, decision: ReviewDecision) -> List[str]:
        """Validate that a review decision is consistent and actionable."""
        warnings = []

        # Check verdict consistency
        if decision.verdict == ReviewVerdict.BLOCKED and not decision.escalation_required:
            warnings.append("Blocked verdict should require escalation")

        if decision.verdict == ReviewVerdict.CONDITIONAL_PASS and not decision.conditions:
            warnings.append("Conditional pass verdict should have conditions")

        if decision.verdict == ReviewVerdict.DEBT_ACCEPTED and not decision.technical_debt:
            warnings.append("Debt accepted verdict should have technical debt items")

        # Check effort estimates
        if decision.estimated_fix_effort_hours > 40:
            warnings.append(f"High effort estimate ({decision.estimated_fix_effort_hours}h) - consider breaking down")

        # Check condition deadlines
        for condition in decision.conditions:
            if condition.deadline_hours > 168:  # 1 week
                warnings.append(f"Long condition deadline ({condition.deadline_hours}h) - consider shorter timeframe")

        # Check technical debt accumulation
        total_debt_hours = sum(debt.estimated_effort_hours for debt in decision.technical_debt)
        if total_debt_hours > 20:
            warnings.append(f"High technical debt ({total_debt_hours}h) - consider addressing some issues now")

        return warnings

    def export_decision_report(self, decision: ReviewDecision, output_path: str) -> None:
        """Export detailed decision report."""
        report = {
            "generated_at": datetime.datetime.now().isoformat(),
            "decision": asdict(decision),
            "validation_warnings": self.validate_decision(decision),
            "metadata": {
                "total_issues": len(decision.issues_found),
                "severity_breakdown": self._get_severity_breakdown(decision.issues_found),
                "category_breakdown": self._get_category_breakdown(decision.issues_found),
                "estimated_total_effort": decision.estimated_fix_effort_hours,
                "conditions_count": len(decision.conditions),
                "debt_items_count": len(decision.technical_debt)
            }
        }

        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2, default=str)

        logger.info(f"Decision report exported to {output_path}")

    def _get_severity_breakdown(self, issues: List[ReviewIssue]) -> Dict[str, int]:
        """Get breakdown of issues by severity."""
        breakdown = {}
        for severity in IssueSeverity:
            breakdown[severity.value] = len([i for i in issues if i.severity == severity])
        return breakdown

    def _get_category_breakdown(self, issues: List[ReviewIssue]) -> Dict[str, int]:
        """Get breakdown of issues by category."""
        breakdown = {}
        for category in IssueCategory:
            breakdown[category.value] = len([i for i in issues if i.category == category])
        return breakdown


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Atlas Review Decision Engine")
    parser.add_argument("--demo", action="store_true", help="Run demo with sample issues")
    parser.add_argument("--config", help="Path to configuration file")
    parser.add_argument("--output", default="./review_decision.json", help="Output file path")

    args = parser.parse_args()

    engine = ReviewDecisionEngine(args.config)

    if args.demo:
        # Create sample issues for demo
        sample_issues = [
            ReviewIssue(
                id="SEC001",
                title="SQL Injection Vulnerability",
                description="User input not properly sanitized in login endpoint",
                severity=IssueSeverity.HIGH,
                category=IssueCategory.SECURITY,
                location="src/auth/login.py:45",
                effort_estimate_hours=4.0,
                affects_critical_path=True
            ),
            ReviewIssue(
                id="PERF002",
                title="Slow Database Query",
                description="User search query takes 3+ seconds",
                severity=IssueSeverity.MEDIUM,
                category=IssueCategory.PERFORMANCE,
                location="src/search/user_search.py:12",
                effort_estimate_hours=2.5,
                can_be_deferred=True
            ),
            ReviewIssue(
                id="DOC003",
                title="Missing API Documentation",
                description="New endpoints lack OpenAPI documentation",
                severity=IssueSeverity.LOW,
                category=IssueCategory.DOCUMENTATION,
                location="src/api/",
                effort_estimate_hours=1.5,
                can_be_deferred=True
            )
        ]

        decision = engine.make_review_decision(sample_issues, ReviewType.SECURITY_REVIEW)

        print(f"\n=== Review Decision Demo ===")
        print(f"Verdict: {decision.verdict.value}")
        print(f"Summary: {decision.summary}")
        print(f"Issues: {len(decision.issues_found)}")
        print(f"Conditions: {len(decision.conditions)}")
        print(f"Technical Debt: {len(decision.technical_debt)}")
        print(f"Estimated Effort: {decision.estimated_fix_effort_hours:.1f} hours")

        # Export detailed report
        engine.export_decision_report(decision, args.output)
        print(f"Detailed report saved to {args.output}")

    else:
        print("Use --demo to run demo, or implement your own issue loading logic")