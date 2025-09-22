#!/usr/bin/env python3
"""
Atlas Workflow Validator v2.0

This module provides validation capabilities for the Atlas workflow state machine,
including rule validation, artifact checking, and quality gate evaluation.
"""

import json
import logging
import datetime
import re
from typing import Dict, List, Optional, Any, Union
from pathlib import Path
from dataclasses import dataclass
from enum import Enum

from workflow_state_machine import (
    WorkflowPhase, WorkflowState, IssueSeverity, WorkflowIssue, WorkflowContext
)


class ValidationResult(Enum):
    """Validation result types."""
    PASS = "pass"
    PASS_WITH_CONDITIONS = "pass_with_conditions"
    CONDITIONAL_PASS = "conditional_pass"
    SOFT_REJECT = "soft_reject"
    DEBT_ACCEPTED = "debt_accepted"
    FAIL = "fail"
    WARNING = "warning"
    SKIP = "skip"
    BLOCKED = "blocked"


@dataclass
class ValidationRule:
    """Represents a validation rule."""
    id: str
    name: str
    description: str
    phase: WorkflowPhase
    severity: IssueSeverity
    rule_type: str  # artifact, quality_gate, business_rule, compliance
    condition: str  # Condition expression or function name
    parameters: Dict[str, Any]
    enabled: bool = True

    def to_dict(self) -> Dict[str, Any]:
        """Convert rule to dictionary."""
        return {
            'id': self.id,
            'name': self.name,
            'description': self.description,
            'phase': self.phase.value,
            'severity': self.severity.value,
            'rule_type': self.rule_type,
            'condition': self.condition,
            'parameters': self.parameters,
            'enabled': self.enabled
        }


@dataclass
class ValidationReport:
    """Validation report for a workflow phase."""
    phase: WorkflowPhase
    story_id: str
    validation_time: datetime.datetime
    overall_result: ValidationResult
    rules_passed: int
    rules_failed: int
    rules_warnings: int
    rules_skipped: int
    issues_found: List[WorkflowIssue]
    recommendations: List[str]
    artifacts_checked: Dict[str, bool]
    quality_metrics: Dict[str, Any]

    def to_dict(self) -> Dict[str, Any]:
        """Convert report to dictionary."""
        return {
            'phase': self.phase.value,
            'story_id': self.story_id,
            'validation_time': self.validation_time.isoformat(),
            'overall_result': self.overall_result.value,
            'rules_passed': self.rules_passed,
            'rules_failed': self.rules_failed,
            'rules_warnings': self.rules_warnings,
            'rules_skipped': self.rules_skipped,
            'issues_found': [issue.to_dict() for issue in self.issues_found],
            'recommendations': self.recommendations,
            'artifacts_checked': self.artifacts_checked,
            'quality_metrics': self.quality_metrics
        }


class WorkflowValidator:
    """
    Comprehensive validator for Atlas workflow phases.

    Validates artifacts, quality gates, business rules, and compliance
    requirements for each workflow phase.
    """

    def __init__(self, config_path: Optional[Path] = None):
        """
        Initialize the workflow validator.

        Args:
            config_path: Path to validation configuration file
        """
        self.config_path = config_path or Path("./validation_config.json")
        self.validation_rules: Dict[str, List[ValidationRule]] = {}
        self.quality_thresholds: Dict[str, Dict[str, Any]] = {}
        self.artifact_patterns: Dict[str, str] = {}

        # Setup logging
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)

        # Load configuration
        self._load_configuration()

        # Initialize validation functions
        self.validation_functions = self._register_validation_functions()

    def _load_configuration(self) -> None:
        """Load validation configuration from file."""
        try:
            if self.config_path.exists():
                with open(self.config_path, 'r') as f:
                    config = json.load(f)
                    self._parse_configuration(config)
            else:
                self._load_default_configuration()
                self._save_configuration()

        except Exception as e:
            self.logger.error(f"Failed to load configuration: {e}")
            self._load_default_configuration()

    def _parse_configuration(self, config: Dict[str, Any]) -> None:
        """Parse configuration dictionary into validation rules."""
        # Parse validation rules
        rules_config = config.get('validation_rules', {})
        for phase_name, phase_rules in rules_config.items():
            try:
                phase = WorkflowPhase(phase_name)
                self.validation_rules[phase_name] = []

                for rule_data in phase_rules:
                    rule = ValidationRule(
                        id=rule_data['id'],
                        name=rule_data['name'],
                        description=rule_data['description'],
                        phase=phase,
                        severity=IssueSeverity(rule_data['severity']),
                        rule_type=rule_data['rule_type'],
                        condition=rule_data['condition'],
                        parameters=rule_data.get('parameters', {}),
                        enabled=rule_data.get('enabled', True)
                    )
                    self.validation_rules[phase_name].append(rule)

            except ValueError as e:
                self.logger.warning(f"Invalid phase or severity in config: {e}")

        # Parse quality thresholds
        self.quality_thresholds = config.get('quality_thresholds', {})

        # Parse artifact patterns
        self.artifact_patterns = config.get('artifact_patterns', {})

    def _load_default_configuration(self) -> None:
        """Load default validation configuration."""
        self.validation_rules = {
            WorkflowPhase.REQUIREMENTS_VALIDATION.value: [
                ValidationRule(
                    id="REQ001",
                    name="Requirements Document Exists",
                    description="Requirements document must be present",
                    phase=WorkflowPhase.REQUIREMENTS_VALIDATION,
                    severity=IssueSeverity.BLOCKED,
                    rule_type="artifact",
                    condition="artifact_exists",
                    parameters={"artifact_name": "requirements_doc"}
                ),
                ValidationRule(
                    id="REQ002",
                    name="Acceptance Criteria Defined",
                    description="All user stories must have acceptance criteria",
                    phase=WorkflowPhase.REQUIREMENTS_VALIDATION,
                    severity=IssueSeverity.HIGH,
                    rule_type="business_rule",
                    condition="acceptance_criteria_complete",
                    parameters={"completeness_threshold": 95.0}
                ),
                ValidationRule(
                    id="REQ003",
                    name="Business Value Quantified",
                    description="Business value must be clearly defined and quantified",
                    phase=WorkflowPhase.REQUIREMENTS_VALIDATION,
                    severity=IssueSeverity.MEDIUM,
                    rule_type="business_rule",
                    condition="business_value_defined",
                    parameters={}
                )
            ],
            WorkflowPhase.DESIGN_REVIEW.value: [
                ValidationRule(
                    id="DES001",
                    name="Design Document Complete",
                    description="Technical design document must be complete",
                    phase=WorkflowPhase.DESIGN_REVIEW,
                    severity=IssueSeverity.BLOCKED,
                    rule_type="artifact",
                    condition="artifact_exists",
                    parameters={"artifact_name": "design_doc"}
                ),
                ValidationRule(
                    id="DES002",
                    name="API Specifications Defined",
                    description="API contracts and specifications must be defined",
                    phase=WorkflowPhase.DESIGN_REVIEW,
                    severity=IssueSeverity.HIGH,
                    rule_type="artifact",
                    condition="artifact_exists",
                    parameters={"artifact_name": "api_specs"}
                ),
                ValidationRule(
                    id="DES003",
                    name="Security Review Passed",
                    description="Design must pass security review",
                    phase=WorkflowPhase.DESIGN_REVIEW,
                    severity=IssueSeverity.HIGH,
                    rule_type="quality_gate",
                    condition="security_review_passed",
                    parameters={}
                )
            ],
            WorkflowPhase.IMPLEMENTATION.value: [
                ValidationRule(
                    id="IMP001",
                    name="Code Coverage Threshold",
                    description="Code coverage must meet minimum threshold",
                    phase=WorkflowPhase.IMPLEMENTATION,
                    severity=IssueSeverity.HIGH,
                    rule_type="quality_gate",
                    condition="coverage_threshold",
                    parameters={"minimum_coverage": 80.0}
                ),
                ValidationRule(
                    id="IMP002",
                    name="Security Scan Clean",
                    description="Security scan must show no critical vulnerabilities",
                    phase=WorkflowPhase.IMPLEMENTATION,
                    severity=IssueSeverity.BLOCKED,
                    rule_type="quality_gate",
                    condition="security_scan_clean",
                    parameters={"max_critical": 0, "max_high": 1}
                ),
                ValidationRule(
                    id="IMP003",
                    name="Build Success",
                    description="Build must complete successfully",
                    phase=WorkflowPhase.IMPLEMENTATION,
                    severity=IssueSeverity.BLOCKED,
                    rule_type="quality_gate",
                    condition="build_success",
                    parameters={}
                )
            ],
            WorkflowPhase.ADVERSARIAL_REVIEW.value: [
                ValidationRule(
                    id="REV001",
                    name="Functionality Score",
                    description="Functionality must meet minimum score",
                    phase=WorkflowPhase.ADVERSARIAL_REVIEW,
                    severity=IssueSeverity.HIGH,
                    rule_type="quality_gate",
                    condition="quality_score_threshold",
                    parameters={"metric": "functionality_score", "minimum": 90.0}
                ),
                ValidationRule(
                    id="REV002",
                    name="Performance Benchmarks",
                    description="Performance must meet benchmarks",
                    phase=WorkflowPhase.ADVERSARIAL_REVIEW,
                    severity=IssueSeverity.HIGH,
                    rule_type="quality_gate",
                    condition="performance_benchmarks",
                    parameters={"max_response_time": 500, "min_throughput": 100}
                ),
                ValidationRule(
                    id="REV003",
                    name="User Acceptance",
                    description="User acceptance testing must pass",
                    phase=WorkflowPhase.ADVERSARIAL_REVIEW,
                    severity=IssueSeverity.MEDIUM,
                    rule_type="quality_gate",
                    condition="user_acceptance_score",
                    parameters={"minimum_score": 4.0}
                )
            ],
            WorkflowPhase.DEPLOYMENT.value: [
                ValidationRule(
                    id="DEP001",
                    name="Deployment Plan Approved",
                    description="Deployment plan must be reviewed and approved",
                    phase=WorkflowPhase.DEPLOYMENT,
                    severity=IssueSeverity.BLOCKED,
                    rule_type="artifact",
                    condition="artifact_exists",
                    parameters={"artifact_name": "deployment_plan"}
                ),
                ValidationRule(
                    id="DEP002",
                    name="Rollback Procedure Tested",
                    description="Rollback procedure must be tested",
                    phase=WorkflowPhase.DEPLOYMENT,
                    severity=IssueSeverity.HIGH,
                    rule_type="business_rule",
                    condition="rollback_tested",
                    parameters={}
                ),
                ValidationRule(
                    id="DEP003",
                    name="Monitoring Setup",
                    description="Production monitoring must be configured",
                    phase=WorkflowPhase.DEPLOYMENT,
                    severity=IssueSeverity.HIGH,
                    rule_type="quality_gate",
                    condition="monitoring_configured",
                    parameters={}
                )
            ]
        }

        self.quality_thresholds = {
            "test_coverage": {"minimum": 80.0, "target": 90.0},
            "code_quality_score": {"minimum": 85.0, "target": 95.0},
            "functionality_score": {"minimum": 90.0, "target": 98.0},
            "performance_score": {"minimum": 85.0, "target": 95.0},
            "security_score": {"minimum": 95.0, "target": 100.0},
            "user_satisfaction": {"minimum": 4.0, "target": 4.5}
        }

        self.artifact_patterns = {
            "requirements_doc": r".*requirements.*\.(md|doc|docx|pdf)$",
            "design_doc": r".*design.*\.(md|doc|docx|pdf)$",
            "api_specs": r".*api.*\.(yaml|yml|json|md)$",
            "source_code": r".*\.(py|java|js|ts|go|rb|php|cs)$",
            "unit_tests": r".*test.*\.(py|java|js|ts|go|rb|php|cs)$",
            "deployment_plan": r".*deploy.*\.(md|doc|docx|pdf|yaml|yml)$"
        }

    def _save_configuration(self) -> None:
        """Save current configuration to file."""
        try:
            config = {
                'validation_rules': {},
                'quality_thresholds': self.quality_thresholds,
                'artifact_patterns': self.artifact_patterns
            }

            # Convert rules to serializable format
            for phase_name, rules in self.validation_rules.items():
                config['validation_rules'][phase_name] = [rule.to_dict() for rule in rules]

            with open(self.config_path, 'w') as f:
                json.dump(config, f, indent=2)

        except Exception as e:
            self.logger.error(f"Failed to save configuration: {e}")

    def _register_validation_functions(self) -> Dict[str, callable]:
        """Register validation functions."""
        return {
            'artifact_exists': self._validate_artifact_exists,
            'acceptance_criteria_complete': self._validate_acceptance_criteria,
            'business_value_defined': self._validate_business_value,
            'security_review_passed': self._validate_security_review,
            'coverage_threshold': self._validate_coverage_threshold,
            'security_scan_clean': self._validate_security_scan,
            'build_success': self._validate_build_success,
            'quality_score_threshold': self._validate_quality_score,
            'performance_benchmarks': self._validate_performance_benchmarks,
            'user_acceptance_score': self._validate_user_acceptance,
            'rollback_tested': self._validate_rollback_tested,
            'monitoring_configured': self._validate_monitoring_configured
        }

    def validate_phase(
        self,
        phase: WorkflowPhase,
        story_id: str,
        context: Optional[WorkflowContext] = None,
        artifact_path: Optional[Path] = None
    ) -> ValidationReport:
        """
        Validate a workflow phase.

        Args:
            phase: Workflow phase to validate
            story_id: Story ID being validated
            context: Optional workflow context
            artifact_path: Path to artifacts directory

        Returns:
            ValidationReport: Comprehensive validation report
        """
        self.logger.info(f"Validating phase {phase.value} for story {story_id}")

        validation_time = datetime.datetime.now()
        issues_found = []
        recommendations = []
        artifacts_checked = {}
        quality_metrics = {}

        rules_passed = 0
        rules_failed = 0
        rules_warnings = 0
        rules_skipped = 0

        # Get rules for this phase
        phase_rules = self.validation_rules.get(phase.value, [])

        for rule in phase_rules:
            if not rule.enabled:
                rules_skipped += 1
                continue

            try:
                # Execute validation function
                validation_func = self.validation_functions.get(rule.condition)
                if not validation_func:
                    self.logger.warning(f"Validation function not found: {rule.condition}")
                    rules_skipped += 1
                    continue

                result = validation_func(rule, story_id, context, artifact_path)

                if result == ValidationResult.PASS:
                    rules_passed += 1
                elif result == ValidationResult.FAIL:
                    rules_failed += 1
                    # Create issue for failed rule
                    issue = WorkflowIssue(
                        id=f"{rule.id}_{story_id}_{int(validation_time.timestamp())}",
                        severity=rule.severity,
                        phase=phase,
                        title=rule.name,
                        description=rule.description,
                        created_at=validation_time
                    )
                    issues_found.append(issue)
                elif result == ValidationResult.WARNING:
                    rules_warnings += 1
                    # Create warning issue
                    issue = WorkflowIssue(
                        id=f"{rule.id}_{story_id}_{int(validation_time.timestamp())}",
                        severity=IssueSeverity.LOW,
                        phase=phase,
                        title=f"Warning: {rule.name}",
                        description=rule.description,
                        created_at=validation_time
                    )
                    issues_found.append(issue)
                else:
                    rules_skipped += 1

            except Exception as e:
                self.logger.error(f"Error validating rule {rule.id}: {e}")
                rules_failed += 1

        # Determine overall result
        if rules_failed > 0:
            overall_result = ValidationResult.FAIL
        elif rules_warnings > 0:
            overall_result = ValidationResult.WARNING
        else:
            overall_result = ValidationResult.PASS

        # Generate recommendations
        recommendations = self._generate_recommendations(phase, issues_found)

        # Create validation report
        report = ValidationReport(
            phase=phase,
            story_id=story_id,
            validation_time=validation_time,
            overall_result=overall_result,
            rules_passed=rules_passed,
            rules_failed=rules_failed,
            rules_warnings=rules_warnings,
            rules_skipped=rules_skipped,
            issues_found=issues_found,
            recommendations=recommendations,
            artifacts_checked=artifacts_checked,
            quality_metrics=quality_metrics
        )

        self.logger.info(f"Validation complete: {overall_result.value}")
        return report

    def validate_transition(
        self,
        from_phase: WorkflowPhase,
        to_phase: WorkflowPhase,
        story_id: str
    ) -> ValidationReport:
        """
        Validate if transition between phases is allowed.

        Args:
            from_phase: Current phase
            to_phase: Target phase
            story_id: Story ID

        Returns:
            ValidationReport: Validation report for transition
        """
        self.logger.info(f"Validating transition from {from_phase.value} to {to_phase.value}")

        # First validate current phase completion
        current_phase_report = self.validate_phase(from_phase, story_id)

        # Check if current phase validation allows transition
        if current_phase_report.overall_result == ValidationResult.FAIL:
            # Check if any blocking issues exist
            blocking_issues = [
                issue for issue in current_phase_report.issues_found
                if issue.severity == IssueSeverity.BLOCKED
            ]

            if blocking_issues:
                # Create transition validation report
                transition_issue = WorkflowIssue(
                    id=f"TRANS_{story_id}_{int(datetime.datetime.now().timestamp())}",
                    severity=IssueSeverity.BLOCKED,
                    phase=from_phase,
                    title="Transition Blocked",
                    description=f"Cannot transition to {to_phase.value} due to blocking issues",
                    created_at=datetime.datetime.now()
                )

                return ValidationReport(
                    phase=from_phase,
                    story_id=story_id,
                    validation_time=datetime.datetime.now(),
                    overall_result=ValidationResult.FAIL,
                    rules_passed=0,
                    rules_failed=1,
                    rules_warnings=0,
                    rules_skipped=0,
                    issues_found=[transition_issue],
                    recommendations=["Resolve blocking issues before attempting transition"],
                    artifacts_checked={},
                    quality_metrics={}
                )

        return current_phase_report

    # Validation function implementations
    def _validate_artifact_exists(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate that required artifact exists."""
        artifact_name = rule.parameters.get('artifact_name')
        if not artifact_name:
            return ValidationResult.SKIP

        # If artifact path is provided, check for actual files
        if artifact_path and artifact_path.exists():
            pattern = self.artifact_patterns.get(artifact_name, f"*{artifact_name}*")
            matching_files = list(artifact_path.glob(pattern))
            return ValidationResult.PASS if matching_files else ValidationResult.FAIL

        # Otherwise, simulate artifact check (placeholder)
        # In real implementation, this would check actual artifact storage
        return ValidationResult.PASS

    def _validate_acceptance_criteria(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate acceptance criteria completeness."""
        threshold = rule.parameters.get('completeness_threshold', 95.0)

        # Placeholder: In real implementation, this would check actual acceptance criteria
        # For now, simulate based on story complexity
        if context and context.complexity == "high":
            completeness = 85.0  # High complexity might have lower initial completeness
        else:
            completeness = 95.0

        return ValidationResult.PASS if completeness >= threshold else ValidationResult.FAIL

    def _validate_business_value(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate business value definition."""
        # Placeholder: Check if business value is quantified
        # In real implementation, would check requirements document
        return ValidationResult.PASS

    def _validate_security_review(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate security review completion."""
        # Placeholder: Check security review status
        # In real implementation, would check review system
        return ValidationResult.PASS

    def _validate_coverage_threshold(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate code coverage threshold."""
        minimum_coverage = rule.parameters.get('minimum_coverage', 80.0)

        # Placeholder: Get actual coverage from test reports
        # In real implementation, would parse coverage reports
        current_coverage = 87.0  # Simulated value

        return ValidationResult.PASS if current_coverage >= minimum_coverage else ValidationResult.FAIL

    def _validate_security_scan(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate security scan results."""
        max_critical = rule.parameters.get('max_critical', 0)
        max_high = rule.parameters.get('max_high', 1)

        # Placeholder: Get actual scan results
        # In real implementation, would parse security scan reports
        critical_issues = 0
        high_issues = 0

        if critical_issues > max_critical or high_issues > max_high:
            return ValidationResult.FAIL
        return ValidationResult.PASS

    def _validate_build_success(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate build success."""
        # Placeholder: Check build status
        # In real implementation, would check CI/CD system
        build_success = True  # Simulated value
        return ValidationResult.PASS if build_success else ValidationResult.FAIL

    def _validate_quality_score(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate quality score threshold."""
        metric = rule.parameters.get('metric')
        minimum = rule.parameters.get('minimum', 85.0)

        # Placeholder: Get actual quality metrics
        # In real implementation, would get from quality measurement system
        quality_scores = {
            'functionality_score': 92.0,
            'performance_score': 88.0,
            'security_score': 96.0
        }

        current_score = quality_scores.get(metric, 0.0)
        return ValidationResult.PASS if current_score >= minimum else ValidationResult.FAIL

    def _validate_performance_benchmarks(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate performance benchmarks."""
        max_response_time = rule.parameters.get('max_response_time', 500)
        min_throughput = rule.parameters.get('min_throughput', 100)

        # Placeholder: Get actual performance metrics
        # In real implementation, would get from performance testing results
        current_response_time = 350  # ms
        current_throughput = 120  # req/sec

        if current_response_time > max_response_time or current_throughput < min_throughput:
            return ValidationResult.FAIL
        return ValidationResult.PASS

    def _validate_user_acceptance(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate user acceptance score."""
        minimum_score = rule.parameters.get('minimum_score', 4.0)

        # Placeholder: Get actual user acceptance score
        # In real implementation, would get from user testing results
        current_score = 4.2

        return ValidationResult.PASS if current_score >= minimum_score else ValidationResult.FAIL

    def _validate_rollback_tested(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate rollback procedure testing."""
        # Placeholder: Check if rollback procedure has been tested
        # In real implementation, would check deployment test results
        rollback_tested = True
        return ValidationResult.PASS if rollback_tested else ValidationResult.FAIL

    def _validate_monitoring_configured(
        self,
        rule: ValidationRule,
        story_id: str,
        context: Optional[WorkflowContext],
        artifact_path: Optional[Path]
    ) -> ValidationResult:
        """Validate monitoring configuration."""
        # Placeholder: Check if monitoring is properly configured
        # In real implementation, would check monitoring system
        monitoring_configured = True
        return ValidationResult.PASS if monitoring_configured else ValidationResult.FAIL

    def _generate_recommendations(
        self,
        phase: WorkflowPhase,
        issues: List[WorkflowIssue]
    ) -> List[str]:
        """Generate recommendations based on validation issues."""
        recommendations = []

        if not issues:
            recommendations.append(f"✅ {phase.value} validation passed - ready to proceed")
            return recommendations

        # Group issues by severity
        blocked_issues = [i for i in issues if i.severity == IssueSeverity.BLOCKED]
        high_issues = [i for i in issues if i.severity == IssueSeverity.HIGH]
        medium_issues = [i for i in issues if i.severity == IssueSeverity.MEDIUM]

        if blocked_issues:
            recommendations.append(f"🚫 {len(blocked_issues)} blocking issue(s) must be resolved before proceeding")

        if high_issues:
            recommendations.append(f"⚠️ {len(high_issues)} high severity issue(s) should be addressed")

        if medium_issues:
            recommendations.append(f"📋 {len(medium_issues)} medium severity issue(s) can be addressed or tracked as technical debt")

        # Phase-specific recommendations
        if phase == WorkflowPhase.REQUIREMENTS_VALIDATION:
            recommendations.append("💡 Consider stakeholder review session to clarify requirements")
        elif phase == WorkflowPhase.DESIGN_REVIEW:
            recommendations.append("💡 Schedule architecture review with senior engineers")
        elif phase == WorkflowPhase.IMPLEMENTATION:
            recommendations.append("💡 Run additional code quality checks and peer reviews")
        elif phase == WorkflowPhase.ADVERSARIAL_REVIEW:
            recommendations.append("💡 Consider additional testing or performance optimization")
        elif phase == WorkflowPhase.DEPLOYMENT:
            recommendations.append("💡 Verify production readiness checklist completion")

        return recommendations

    def generate_validation_summary(self, reports: List[ValidationReport]) -> str:
        """Generate a summary of multiple validation reports."""
        if not reports:
            return "No validation reports available"

        total_rules = sum(r.rules_passed + r.rules_failed + r.rules_warnings for r in reports)
        total_passed = sum(r.rules_passed for r in reports)
        total_failed = sum(r.rules_failed for r in reports)
        total_warnings = sum(r.rules_warnings for r in reports)
        total_issues = sum(len(r.issues_found) for r in reports)

        success_rate = (total_passed / total_rules * 100) if total_rules > 0 else 0

        summary = f"""
Atlas Validation Summary
========================

Overall Statistics:
- Total Rules Evaluated: {total_rules}
- Rules Passed: {total_passed} ({success_rate:.1f}%)
- Rules Failed: {total_failed}
- Warnings: {total_warnings}
- Total Issues Found: {total_issues}

Phase Results:
"""

        for report in reports:
            status_icon = "✅" if report.overall_result == ValidationResult.PASS else "❌"
            summary += f"{status_icon} {report.phase.value}: {report.overall_result.value}\n"

        if total_failed > 0:
            summary += "\n⚠️ Workflow cannot proceed until failed validations are addressed"
        elif total_warnings > 0:
            summary += "\n⚠️ Workflow can proceed but warnings should be reviewed"
        else:
            summary += "\n✅ All validations passed - workflow ready to proceed"

        return summary


def main():
    """Example usage of WorkflowValidator."""
    validator = WorkflowValidator()

    # Create context
    context = WorkflowContext(
        story_id="S001",
        feature_id="F001",
        sprint_id="SP-2024-10",
        assignee="developer@company.com",
        reviewer="lead@company.com",
        created_at=datetime.datetime.now(),
        complexity="medium"
    )

    # Validate different phases
    phases_to_validate = [
        WorkflowPhase.REQUIREMENTS_VALIDATION,
        WorkflowPhase.DESIGN_REVIEW,
        WorkflowPhase.IMPLEMENTATION
    ]

    reports = []
    for phase in phases_to_validate:
        report = validator.validate_phase(phase, "S001", context)
        reports.append(report)
        print(f"\n{phase.value} Validation:")
        print(f"Result: {report.overall_result.value}")
        print(f"Issues Found: {len(report.issues_found)}")
        if report.recommendations:
            print("Recommendations:")
            for rec in report.recommendations:
                print(f"  - {rec}")

    # Generate overall summary
    summary = validator.generate_validation_summary(reports)
    print(summary)


if __name__ == "__main__":
    main()