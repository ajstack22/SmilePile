#!/usr/bin/env python3
"""
Atlas Workflow State Machine v2.0

This module implements a state machine for the Atlas enhanced workflow process,
managing transitions between phases, validation rules, and automation triggers.
"""

import json
import logging
import datetime
from enum import Enum
from typing import Dict, List, Optional, Set, Any, Callable
from dataclasses import dataclass, asdict
from pathlib import Path


class WorkflowPhase(Enum):
    """Enumeration of all possible workflow phases."""
    REQUIREMENTS_VALIDATION = "requirements_validation"
    DEPENDENCY_ANALYSIS = "dependency_analysis"
    WAVE_GENERATION = "wave_generation"
    PARALLEL_EXECUTION = "parallel_execution"
    DESIGN_REVIEW = "design_review"
    IMPLEMENTATION = "implementation"
    ADVERSARIAL_REVIEW = "adversarial_review"
    DEPLOYMENT = "deployment"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


class WorkflowState(Enum):
    """Enumeration of states within each phase."""
    PENDING = "pending"
    IN_PROGRESS = "in_progress"
    PARALLEL_ACTIVE = "parallel_active"     # Multiple tasks running in parallel
    WAVE_WAITING = "wave_waiting"           # Waiting for wave dependencies
    REVIEW = "review"
    CONDITIONAL_REVIEW = "conditional_review"  # Smart re-review in progress
    REWORK = "rework"
    BLOCKED = "blocked"
    APPROVED = "approved"
    PASS_WITH_CONDITIONS = "pass_with_conditions"  # Graduated review verdict
    CONDITIONAL_PASS = "conditional_pass"    # Graduated review verdict
    SOFT_REJECT = "soft_reject"             # Graduated review verdict
    DEBT_ACCEPTED = "debt_accepted"         # Graduated review verdict
    REJECTED = "rejected"


class IssueSeverity(Enum):
    """Issue severity levels for workflow validation."""
    BLOCKED = "blocked"
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"


@dataclass
class WorkflowIssue:
    """Represents an issue found during workflow validation."""
    id: str
    severity: IssueSeverity
    phase: WorkflowPhase
    title: str
    description: str
    created_at: datetime.datetime
    assigned_to: Optional[str] = None
    resolved_at: Optional[datetime.datetime] = None
    resolution: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        """Convert issue to dictionary for serialization."""
        return asdict(self)


@dataclass
class WorkflowMetrics:
    """Metrics for workflow phase performance."""
    phase: WorkflowPhase
    start_time: datetime.datetime
    end_time: Optional[datetime.datetime] = None
    cycle_count: int = 1
    issues_found: int = 0
    quality_score: Optional[float] = None
    duration_minutes: Optional[int] = None

    def to_dict(self) -> Dict[str, Any]:
        """Convert metrics to dictionary for serialization."""
        return asdict(self)


@dataclass
class ParallelExecutionState:
    """State information for parallel execution."""
    current_wave: int
    total_waves: int
    active_tasks: List[str]
    completed_tasks: List[str]
    failed_tasks: List[str]
    agents_allocated: int
    estimated_completion: Optional[datetime.datetime] = None

    def to_dict(self) -> Dict[str, Any]:
        """Convert parallel state to dictionary for serialization."""
        return asdict(self)


@dataclass
class ReviewCondition:
    """Represents a condition for graduated review verdicts."""
    condition_id: str
    description: str
    deadline: datetime.datetime
    verification_method: str
    responsible_party: str
    completed: bool = False
    completion_date: Optional[datetime.datetime] = None

    def to_dict(self) -> Dict[str, Any]:
        """Convert condition to dictionary for serialization."""
        return asdict(self)


@dataclass
class TechnicalDebtItem:
    """Represents accepted technical debt."""
    debt_id: str
    description: str
    rationale: str
    estimated_effort_hours: float
    priority: str
    target_sprint: Optional[str] = None
    created_at: Optional[datetime.datetime] = None

    def to_dict(self) -> Dict[str, Any]:
        """Convert debt item to dictionary for serialization."""
        return asdict(self)


@dataclass
class WorkflowContext:
    """Context information for workflow execution."""
    story_id: str
    feature_id: str
    sprint_id: str
    assignee: str
    reviewer: str
    created_at: datetime.datetime
    priority: str = "medium"
    complexity: str = "medium"
    risk_level: str = "medium"
    # Atlas 2.1 additions
    parallel_execution: Optional[ParallelExecutionState] = None
    trust_score: Optional[float] = None
    review_conditions: List[ReviewCondition] = None
    technical_debt: List[TechnicalDebtItem] = None
    differential_analysis: Optional[Dict[str, Any]] = None

    def __post_init__(self):
        if self.review_conditions is None:
            self.review_conditions = []
        if self.technical_debt is None:
            self.technical_debt = []

    def to_dict(self) -> Dict[str, Any]:
        """Convert context to dictionary for serialization."""
        return asdict(self)


class WorkflowStateMachine:
    """
    State machine for Atlas Enhanced Workflow v2.0.

    Manages workflow phases, state transitions, validation rules,
    and automation triggers.
    """

    def __init__(self, storage_path: Optional[Path] = None):
        """
        Initialize the workflow state machine.

        Args:
            storage_path: Path to store workflow state data
        """
        self.storage_path = storage_path or Path("./workflow_data")
        self.storage_path.mkdir(exist_ok=True)

        # Current workflow state
        self.current_phase = WorkflowPhase.REQUIREMENTS_VALIDATION
        self.current_state = WorkflowState.PENDING
        self.context: Optional[WorkflowContext] = None

        # Workflow history and metrics
        self.phase_history: List[WorkflowMetrics] = []
        self.issues: List[WorkflowIssue] = []
        self.state_history: List[Dict[str, Any]] = []

        # Configuration
        self.validation_rules = self._load_validation_rules()
        self.transition_handlers: Dict[str, Callable] = {}

        # Setup logging
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)

    def _load_validation_rules(self) -> Dict[str, Dict[str, Any]]:
        """Load validation rules for each phase."""
        return {
            WorkflowPhase.REQUIREMENTS_VALIDATION.value: {
                "required_artifacts": ["requirements_doc", "acceptance_criteria"],
                "quality_gates": {
                    "requirements_completeness": 95.0,
                    "business_value_defined": True,
                    "dependencies_identified": True
                },
                "max_duration_hours": 48,
                "auto_approve_conditions": {
                    "all_criteria_met": True,
                    "stakeholder_approval": True
                }
            },
            WorkflowPhase.DESIGN_REVIEW.value: {
                "required_artifacts": ["design_doc", "api_specs", "architecture_diagrams"],
                "quality_gates": {
                    "design_completeness": 90.0,
                    "security_review_passed": True,
                    "performance_requirements_defined": True
                },
                "max_duration_hours": 72,
                "reviewers_required": ["technical_lead", "solution_architect"]
            },
            WorkflowPhase.IMPLEMENTATION.value: {
                "required_artifacts": ["source_code", "unit_tests", "documentation"],
                "quality_gates": {
                    "test_coverage": 80.0,
                    "code_quality_score": 85.0,
                    "security_scan_passed": True
                },
                "max_duration_hours": 168,  # 1 week
                "auto_validation": ["build_success", "test_suite_pass"]
            },
            WorkflowPhase.ADVERSARIAL_REVIEW.value: {
                "required_artifacts": ["test_results", "performance_metrics", "security_report"],
                "quality_gates": {
                    "functionality_score": 90.0,
                    "performance_score": 85.0,
                    "security_score": 95.0
                },
                "max_duration_hours": 48,
                "escalation_triggers": ["blocked_issues", "multiple_cycles"]
            },
            WorkflowPhase.DEPLOYMENT.value: {
                "required_artifacts": ["deployment_plan", "rollback_procedure", "monitoring_setup"],
                "quality_gates": {
                    "smoke_tests_passed": True,
                    "monitoring_active": True,
                    "rollback_tested": True
                },
                "max_duration_hours": 24,
                "auto_actions": ["notify_stakeholders", "update_documentation"]
            }
        }

    def start_workflow(self, context: WorkflowContext) -> bool:
        """
        Start a new workflow instance.

        Args:
            context: Workflow context information

        Returns:
            bool: True if workflow started successfully
        """
        try:
            self.context = context
            self.current_phase = WorkflowPhase.REQUIREMENTS_VALIDATION
            self.current_state = WorkflowState.PENDING

            # Initialize metrics
            metrics = WorkflowMetrics(
                phase=self.current_phase,
                start_time=datetime.datetime.now()
            )
            self.phase_history.append(metrics)

            # Record state change
            self._record_state_change(
                action="workflow_started",
                details=f"Started workflow for {context.story_id}"
            )

            self.logger.info(f"Workflow started for {context.story_id}")
            self._save_state()
            return True

        except Exception as e:
            self.logger.error(f"Failed to start workflow: {e}")
            return False

    def transition_to_phase(self, target_phase: WorkflowPhase, force: bool = False) -> bool:
        """
        Transition to a new workflow phase.

        Args:
            target_phase: The phase to transition to
            force: Force transition even if validation fails

        Returns:
            bool: True if transition was successful
        """
        if not self._can_transition_to_phase(target_phase) and not force:
            self.logger.warning(f"Cannot transition to {target_phase.value}: validation failed")
            return False

        try:
            # Complete current phase
            current_metrics = self._get_current_phase_metrics()
            if current_metrics:
                current_metrics.end_time = datetime.datetime.now()
                current_metrics.duration_minutes = int(
                    (current_metrics.end_time - current_metrics.start_time).total_seconds() / 60
                )

            # Start new phase
            self.current_phase = target_phase
            self.current_state = WorkflowState.PENDING

            # Initialize new phase metrics
            new_metrics = WorkflowMetrics(
                phase=target_phase,
                start_time=datetime.datetime.now()
            )
            self.phase_history.append(new_metrics)

            # Record transition
            self._record_state_change(
                action="phase_transition",
                details=f"Transitioned to {target_phase.value}"
            )

            # Execute transition handler if available
            handler_key = f"{self.current_phase.value}_entry"
            if handler_key in self.transition_handlers:
                self.transition_handlers[handler_key](self)

            self.logger.info(f"Transitioned to phase: {target_phase.value}")
            self._save_state()
            return True

        except Exception as e:
            self.logger.error(f"Failed to transition to {target_phase.value}: {e}")
            return False

    def update_state(self, new_state: WorkflowState, reason: str = "") -> bool:
        """
        Update the current state within the phase.

        Args:
            new_state: The new state to transition to
            reason: Reason for state change

        Returns:
            bool: True if state update was successful
        """
        if not self._can_transition_to_state(new_state):
            self.logger.warning(f"Cannot transition to state {new_state.value}")
            return False

        try:
            old_state = self.current_state
            self.current_state = new_state

            # Update metrics if needed
            current_metrics = self._get_current_phase_metrics()
            if current_metrics and new_state == WorkflowState.REVIEW:
                current_metrics.cycle_count += 1

            # Record state change
            self._record_state_change(
                action="state_change",
                details=f"Changed from {old_state.value} to {new_state.value}",
                reason=reason
            )

            self.logger.info(f"State updated: {old_state.value} -> {new_state.value}")
            self._save_state()
            return True

        except Exception as e:
            self.logger.error(f"Failed to update state: {e}")
            return False

    def add_issue(self, issue: WorkflowIssue) -> str:
        """
        Add an issue to the current workflow.

        Args:
            issue: The issue to add

        Returns:
            str: Issue ID
        """
        issue.phase = self.current_phase
        self.issues.append(issue)

        # Update metrics
        current_metrics = self._get_current_phase_metrics()
        if current_metrics:
            current_metrics.issues_found += 1

        # Check for escalation triggers
        self._check_escalation_triggers()

        self.logger.info(f"Added {issue.severity.value} issue: {issue.title}")
        self._save_state()
        return issue.id

    def resolve_issue(self, issue_id: str, resolution: str) -> bool:
        """
        Resolve an issue.

        Args:
            issue_id: ID of the issue to resolve
            resolution: Resolution description

        Returns:
            bool: True if issue was resolved
        """
        for issue in self.issues:
            if issue.id == issue_id:
                issue.resolved_at = datetime.datetime.now()
                issue.resolution = resolution

                self.logger.info(f"Resolved issue {issue_id}: {resolution}")
                self._save_state()
                return True

        self.logger.warning(f"Issue {issue_id} not found")
        return False

    def get_open_issues(self, severity: Optional[IssueSeverity] = None) -> List[WorkflowIssue]:
        """
        Get open issues, optionally filtered by severity.

        Args:
            severity: Optional severity filter

        Returns:
            List[WorkflowIssue]: List of open issues
        """
        open_issues = [issue for issue in self.issues if issue.resolved_at is None]

        if severity:
            open_issues = [issue for issue in open_issues if issue.severity == severity]

        return open_issues

    def validate_phase_completion(self) -> Dict[str, Any]:
        """
        Validate if current phase can be completed.

        Returns:
            Dict[str, Any]: Validation results
        """
        phase_rules = self.validation_rules.get(self.current_phase.value, {})
        results = {
            "can_proceed": True,
            "issues": [],
            "quality_gates": {},
            "artifacts_check": {}
        }

        # Check for blocking issues
        blocking_issues = self.get_open_issues(IssueSeverity.BLOCKED)
        if blocking_issues:
            results["can_proceed"] = False
            results["issues"].append(f"Found {len(blocking_issues)} blocking issues")

        # Check required artifacts
        required_artifacts = phase_rules.get("required_artifacts", [])
        for artifact in required_artifacts:
            # This would integrate with actual artifact checking
            results["artifacts_check"][artifact] = self._check_artifact_exists(artifact)
            if not results["artifacts_check"][artifact]:
                results["can_proceed"] = False
                results["issues"].append(f"Missing required artifact: {artifact}")

        # Check quality gates
        quality_gates = phase_rules.get("quality_gates", {})
        for gate, threshold in quality_gates.items():
            current_value = self._get_quality_metric(gate)
            results["quality_gates"][gate] = {
                "current": current_value,
                "threshold": threshold,
                "passed": self._evaluate_quality_gate(gate, current_value, threshold)
            }

            if not results["quality_gates"][gate]["passed"]:
                results["can_proceed"] = False
                results["issues"].append(f"Quality gate failed: {gate}")

        return results

    def get_workflow_status(self) -> Dict[str, Any]:
        """
        Get comprehensive workflow status.

        Returns:
            Dict[str, Any]: Complete workflow status
        """
        open_issues_by_severity = {}
        for severity in IssueSeverity:
            open_issues_by_severity[severity.value] = len(self.get_open_issues(severity))

        current_metrics = self._get_current_phase_metrics()

        return {
            "context": self.context.to_dict() if self.context else None,
            "current_phase": self.current_phase.value,
            "current_state": self.current_state.value,
            "phase_duration_minutes": self._get_phase_duration_minutes(),
            "cycle_count": current_metrics.cycle_count if current_metrics else 0,
            "open_issues": open_issues_by_severity,
            "total_issues": len(self.issues),
            "phase_history": [metrics.to_dict() for metrics in self.phase_history],
            "can_proceed": self.validate_phase_completion()["can_proceed"],
            "next_valid_phases": self._get_next_valid_phases()
        }

    def generate_workflow_report(self) -> str:
        """
        Generate a comprehensive workflow report.

        Returns:
            str: Formatted workflow report
        """
        status = self.get_workflow_status()

        report = f"""
Atlas Workflow Report
====================

Story ID: {status['context']['story_id'] if status['context'] else 'N/A'}
Current Phase: {status['current_phase']}
Current State: {status['current_state']}
Duration: {status['phase_duration_minutes']} minutes

Issue Summary:
- Blocked: {status['open_issues']['blocked']}
- High: {status['open_issues']['high']}
- Medium: {status['open_issues']['medium']}
- Low: {status['open_issues']['low']}

Phase History:
"""

        for phase_metrics in status['phase_history']:
            report += f"- {phase_metrics['phase']}: {phase_metrics.get('duration_minutes', 'ongoing')} min"
            if phase_metrics.get('cycle_count', 1) > 1:
                report += f" ({phase_metrics['cycle_count']} cycles)"
            report += "\n"

        if not status['can_proceed']:
            report += "\n⚠️ Cannot proceed to next phase - issues must be resolved\n"
        else:
            report += "\n✅ Ready to proceed to next phase\n"

        return report

    def register_transition_handler(self, event: str, handler: Callable) -> None:
        """
        Register a handler for transition events.

        Args:
            event: Event name (e.g., 'design_review_entry')
            handler: Callable to handle the event
        """
        self.transition_handlers[event] = handler

    def _can_transition_to_phase(self, target_phase: WorkflowPhase) -> bool:
        """Check if transition to target phase is allowed."""
        # Define valid phase transitions
        valid_transitions = {
            WorkflowPhase.REQUIREMENTS_VALIDATION: [WorkflowPhase.DESIGN_REVIEW, WorkflowPhase.FAILED],
            WorkflowPhase.DESIGN_REVIEW: [WorkflowPhase.IMPLEMENTATION, WorkflowPhase.REQUIREMENTS_VALIDATION, WorkflowPhase.FAILED],
            WorkflowPhase.IMPLEMENTATION: [WorkflowPhase.ADVERSARIAL_REVIEW, WorkflowPhase.DESIGN_REVIEW, WorkflowPhase.FAILED],
            WorkflowPhase.ADVERSARIAL_REVIEW: [WorkflowPhase.DEPLOYMENT, WorkflowPhase.IMPLEMENTATION, WorkflowPhase.FAILED],
            WorkflowPhase.DEPLOYMENT: [WorkflowPhase.COMPLETED, WorkflowPhase.FAILED],
        }

        allowed_phases = valid_transitions.get(self.current_phase, [])

        if target_phase not in allowed_phases:
            return False

        # Additional validation based on current state and issues
        if self.current_state == WorkflowState.BLOCKED:
            return target_phase == WorkflowPhase.FAILED

        # Check for blocking issues
        if self.get_open_issues(IssueSeverity.BLOCKED):
            return target_phase == WorkflowPhase.FAILED

        return True

    def _can_transition_to_state(self, target_state: WorkflowState) -> bool:
        """Check if transition to target state is allowed."""
        # Define valid state transitions
        valid_transitions = {
            WorkflowState.PENDING: [WorkflowState.IN_PROGRESS, WorkflowState.BLOCKED],
            WorkflowState.IN_PROGRESS: [WorkflowState.REVIEW, WorkflowState.BLOCKED, WorkflowState.APPROVED],
            WorkflowState.REVIEW: [WorkflowState.APPROVED, WorkflowState.REWORK, WorkflowState.REJECTED, WorkflowState.BLOCKED],
            WorkflowState.REWORK: [WorkflowState.IN_PROGRESS, WorkflowState.REVIEW, WorkflowState.BLOCKED],
            WorkflowState.BLOCKED: [WorkflowState.IN_PROGRESS, WorkflowState.REVIEW],
            WorkflowState.APPROVED: [],  # Terminal state for phase
            WorkflowState.REJECTED: [WorkflowState.REWORK, WorkflowState.IN_PROGRESS]
        }

        return target_state in valid_transitions.get(self.current_state, [])

    def _get_current_phase_metrics(self) -> Optional[WorkflowMetrics]:
        """Get metrics for current phase."""
        for metrics in reversed(self.phase_history):
            if metrics.phase == self.current_phase:
                return metrics
        return None

    def _get_phase_duration_minutes(self) -> int:
        """Get duration of current phase in minutes."""
        current_metrics = self._get_current_phase_metrics()
        if not current_metrics:
            return 0

        end_time = current_metrics.end_time or datetime.datetime.now()
        return int((end_time - current_metrics.start_time).total_seconds() / 60)

    def _get_next_valid_phases(self) -> List[str]:
        """Get list of valid next phases."""
        next_phases = []
        for phase in WorkflowPhase:
            if self._can_transition_to_phase(phase):
                next_phases.append(phase.value)
        return next_phases

    def _record_state_change(self, action: str, details: str, reason: str = "") -> None:
        """Record a state change in history."""
        entry = {
            "timestamp": datetime.datetime.now().isoformat(),
            "action": action,
            "phase": self.current_phase.value,
            "state": self.current_state.value,
            "details": details,
            "reason": reason
        }
        self.state_history.append(entry)

    def _check_artifact_exists(self, artifact: str) -> bool:
        """Check if required artifact exists."""
        # This would integrate with actual artifact storage/checking
        # For now, return True as placeholder
        return True

    def _get_quality_metric(self, metric: str) -> Any:
        """Get current value of quality metric."""
        # This would integrate with actual quality measurement systems
        # For now, return placeholder values
        placeholder_values = {
            "requirements_completeness": 95.0,
            "test_coverage": 85.0,
            "code_quality_score": 88.0,
            "functionality_score": 92.0,
            "performance_score": 87.0,
            "security_score": 96.0
        }
        return placeholder_values.get(metric, True)

    def _evaluate_quality_gate(self, gate: str, current_value: Any, threshold: Any) -> bool:
        """Evaluate if quality gate passes."""
        if isinstance(threshold, bool):
            return current_value == threshold
        elif isinstance(threshold, (int, float)):
            return current_value >= threshold
        else:
            return current_value == threshold

    def _check_escalation_triggers(self) -> None:
        """Check if escalation is needed based on current issues."""
        blocking_issues = self.get_open_issues(IssueSeverity.BLOCKED)
        high_issues = self.get_open_issues(IssueSeverity.HIGH)

        current_metrics = self._get_current_phase_metrics()
        cycle_count = current_metrics.cycle_count if current_metrics else 1

        # Escalation triggers
        if blocking_issues:
            self.logger.warning(f"Escalation: {len(blocking_issues)} blocking issues found")

        if len(high_issues) > 3:
            self.logger.warning(f"Escalation: {len(high_issues)} high severity issues")

        if cycle_count > 3:
            self.logger.warning(f"Escalation: Phase has {cycle_count} review cycles")

    def _save_state(self) -> None:
        """Save current workflow state to persistent storage."""
        state_data = {
            "current_phase": self.current_phase.value,
            "current_state": self.current_state.value,
            "context": self.context.to_dict() if self.context else None,
            "phase_history": [metrics.to_dict() for metrics in self.phase_history],
            "issues": [issue.to_dict() for issue in self.issues],
            "state_history": self.state_history,
            "last_updated": datetime.datetime.now().isoformat()
        }

        if self.context:
            filename = f"workflow_{self.context.story_id}.json"
            filepath = self.storage_path / filename

            with open(filepath, 'w') as f:
                json.dump(state_data, f, indent=2, default=str)

    def load_state(self, story_id: str) -> bool:
        """
        Load workflow state from persistent storage.

        Args:
            story_id: Story ID to load

        Returns:
            bool: True if state loaded successfully
        """
        try:
            filename = f"workflow_{story_id}.json"
            filepath = self.storage_path / filename

            if not filepath.exists():
                self.logger.warning(f"No saved state found for {story_id}")
                return False

            with open(filepath, 'r') as f:
                state_data = json.load(f)

            # Restore state
            self.current_phase = WorkflowPhase(state_data["current_phase"])
            self.current_state = WorkflowState(state_data["current_state"])

            if state_data["context"]:
                context_data = state_data["context"]
                self.context = WorkflowContext(
                    story_id=context_data["story_id"],
                    feature_id=context_data["feature_id"],
                    sprint_id=context_data["sprint_id"],
                    assignee=context_data["assignee"],
                    reviewer=context_data["reviewer"],
                    created_at=datetime.datetime.fromisoformat(context_data["created_at"]),
                    priority=context_data.get("priority", "medium"),
                    complexity=context_data.get("complexity", "medium"),
                    risk_level=context_data.get("risk_level", "medium")
                )

            # Restore metrics
            self.phase_history = []
            for metrics_data in state_data["phase_history"]:
                metrics = WorkflowMetrics(
                    phase=WorkflowPhase(metrics_data["phase"]),
                    start_time=datetime.datetime.fromisoformat(metrics_data["start_time"]),
                    cycle_count=metrics_data.get("cycle_count", 1),
                    issues_found=metrics_data.get("issues_found", 0),
                    quality_score=metrics_data.get("quality_score"),
                    duration_minutes=metrics_data.get("duration_minutes")
                )
                if metrics_data.get("end_time"):
                    metrics.end_time = datetime.datetime.fromisoformat(metrics_data["end_time"])
                self.phase_history.append(metrics)

            # Restore issues
            self.issues = []
            for issue_data in state_data["issues"]:
                issue = WorkflowIssue(
                    id=issue_data["id"],
                    severity=IssueSeverity(issue_data["severity"]),
                    phase=WorkflowPhase(issue_data["phase"]),
                    title=issue_data["title"],
                    description=issue_data["description"],
                    created_at=datetime.datetime.fromisoformat(issue_data["created_at"]),
                    assigned_to=issue_data.get("assigned_to"),
                    resolution=issue_data.get("resolution")
                )
                if issue_data.get("resolved_at"):
                    issue.resolved_at = datetime.datetime.fromisoformat(issue_data["resolved_at"])
                self.issues.append(issue)

            self.state_history = state_data.get("state_history", [])

            self.logger.info(f"Loaded workflow state for {story_id}")
            return True

        except Exception as e:
            self.logger.error(f"Failed to load state for {story_id}: {e}")
            return False


def main():
    """Example usage of the WorkflowStateMachine."""
    # Initialize state machine
    sm = WorkflowStateMachine()

    # Create workflow context
    context = WorkflowContext(
        story_id="S001",
        feature_id="F001",
        sprint_id="SP-2024-10",
        assignee="developer@company.com",
        reviewer="lead@company.com",
        created_at=datetime.datetime.now(),
        priority="high",
        complexity="medium",
        risk_level="low"
    )

    # Start workflow
    if sm.start_workflow(context):
        print("Workflow started successfully")

        # Simulate workflow progression
        sm.update_state(WorkflowState.IN_PROGRESS)

        # Add an issue
        issue = WorkflowIssue(
            id="ISS001",
            severity=IssueSeverity.MEDIUM,
            phase=sm.current_phase,
            title="Missing business value definition",
            description="Requirements need clearer business value statement",
            created_at=datetime.datetime.now()
        )
        sm.add_issue(issue)

        # Get status
        status = sm.get_workflow_status()
        print(f"Current phase: {status['current_phase']}")
        print(f"Open issues: {status['open_issues']}")

        # Generate report
        report = sm.generate_workflow_report()
        print(report)


if __name__ == "__main__":
    main()