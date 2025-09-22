#!/usr/bin/env python3
"""
Atlas Pre-Check Runner v2.1
Copyright 2024 Atlas Framework

Executes comprehensive automated quality checks before human review to catch
common issues early and reduce review cycles.
"""

import json
import logging
import datetime
import subprocess
import concurrent.futures
from typing import Dict, List, Set, Optional, Any, Tuple
from dataclasses import dataclass, asdict
from pathlib import Path
from enum import Enum
import time

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class CheckStatus(Enum):
    """Status of a pre-check."""
    PASS = "pass"
    FAIL = "fail"
    WARNING = "warning"
    ERROR = "error"
    SKIPPED = "skipped"


class CheckSeverity(Enum):
    """Severity levels for check results."""
    BLOCKING = "blocking"
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"
    INFO = "info"


@dataclass
class CheckIssue:
    """Represents an issue found during pre-checks."""
    check_name: str
    severity: CheckSeverity
    message: str
    location: Optional[str] = None
    suggestion: Optional[str] = None
    auto_fixable: bool = False


@dataclass
class CheckResult:
    """Result of a single pre-check."""
    check_name: str
    status: CheckStatus
    duration_seconds: float
    issues: List[CheckIssue]
    metadata: Dict[str, Any]
    summary: str

    @property
    def has_blocking_issues(self) -> bool:
        return any(issue.severity == CheckSeverity.BLOCKING for issue in self.issues)

    @property
    def issue_count_by_severity(self) -> Dict[str, int]:
        counts = {severity.value: 0 for severity in CheckSeverity}
        for issue in self.issues:
            counts[issue.severity.value] += 1
        return counts


@dataclass
class PreCheckReport:
    """Complete pre-check execution report."""
    timestamp: datetime.datetime
    overall_status: CheckStatus
    total_duration_seconds: float
    check_results: List[CheckResult]
    summary: Dict[str, Any]
    recommendations: List[str]


class BaseChecker:
    """Base class for all pre-check implementations."""

    def __init__(self, config: Optional[Dict[str, Any]] = None):
        self.config = config or {}
        self.name = self.__class__.__name__.replace("Checker", "").lower()

    def should_run(self, change_set: Dict[str, Any], trust_score: float) -> bool:
        """Determine if this check should run based on changes and trust."""
        # Override in subclasses for specific logic
        return True

    def execute(self, change_set: Dict[str, Any]) -> CheckResult:
        """Execute the check and return results."""
        start_time = time.time()

        try:
            issues = self._perform_check(change_set)
            status = self._determine_status(issues)
            summary = self._generate_summary(issues, status)

        except Exception as e:
            logger.error(f"Check {self.name} failed: {e}")
            issues = [CheckIssue(
                check_name=self.name,
                severity=CheckSeverity.BLOCKING,
                message=f"Check execution failed: {str(e)}",
                suggestion="Fix the underlying issue and re-run checks"
            )]
            status = CheckStatus.ERROR
            summary = f"Check failed with error: {str(e)}"

        duration = time.time() - start_time

        return CheckResult(
            check_name=self.name,
            status=status,
            duration_seconds=duration,
            issues=issues,
            metadata=self._get_metadata(),
            summary=summary
        )

    def _perform_check(self, change_set: Dict[str, Any]) -> List[CheckIssue]:
        """Perform the actual check. Override in subclasses."""
        raise NotImplementedError

    def _determine_status(self, issues: List[CheckIssue]) -> CheckStatus:
        """Determine overall status based on issues found."""
        if not issues:
            return CheckStatus.PASS

        severities = [issue.severity for issue in issues]

        if CheckSeverity.BLOCKING in severities:
            return CheckStatus.FAIL
        elif CheckSeverity.HIGH in severities:
            return CheckStatus.FAIL
        elif CheckSeverity.MEDIUM in severities:
            return CheckStatus.WARNING
        else:
            return CheckStatus.PASS

    def _generate_summary(self, issues: List[CheckIssue], status: CheckStatus) -> str:
        """Generate human-readable summary."""
        if status == CheckStatus.PASS:
            return f"{self.name.title()} check passed with no issues"

        issue_counts = {}
        for issue in issues:
            severity = issue.severity.value
            issue_counts[severity] = issue_counts.get(severity, 0) + 1

        count_str = ", ".join(f"{count} {severity}" for severity, count in issue_counts.items())
        return f"{self.name.title()} check found {count_str} issues"

    def _get_metadata(self) -> Dict[str, Any]:
        """Get additional metadata for the check result."""
        return {
            "check_version": "2.1",
            "config": self.config
        }


class CodeQualityChecker(BaseChecker):
    """Checks code quality metrics including linting, complexity, and style."""

    def should_run(self, change_set: Dict[str, Any], trust_score: float) -> bool:
        # Always run for code changes, skip for docs-only changes
        code_files = self._get_code_files(change_set.get("file_changes", []))
        return len(code_files) > 0

    def _perform_check(self, change_set: Dict[str, Any]) -> List[CheckIssue]:
        issues = []

        # Get code files
        code_files = self._get_code_files(change_set.get("file_changes", []))

        for file_info in code_files:
            file_path = file_info.get("path", "")

            # Run linting
            issues.extend(self._run_linting(file_path))

            # Check complexity
            issues.extend(self._check_complexity(file_path))

            # Check duplication
            issues.extend(self._check_duplication(file_path))

        return issues

    def _get_code_files(self, file_changes: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Filter for code files only."""
        code_extensions = {'.py', '.js', '.ts', '.java', '.cpp', '.c', '.h', '.rb', '.go'}

        return [
            fc for fc in file_changes
            if Path(fc.get("path", "")).suffix.lower() in code_extensions
            and fc.get("change_type") != "deleted"
        ]

    def _run_linting(self, file_path: str) -> List[CheckIssue]:
        """Run linting checks on a file."""
        issues = []

        try:
            # Python files
            if file_path.endswith('.py'):
                issues.extend(self._run_python_linting(file_path))
            # JavaScript/TypeScript files
            elif file_path.endswith(('.js', '.ts')):
                issues.extend(self._run_javascript_linting(file_path))

        except Exception as e:
            logger.warning(f"Linting failed for {file_path}: {e}")

        return issues

    def _run_python_linting(self, file_path: str) -> List[CheckIssue]:
        """Run Python-specific linting."""
        issues = []

        try:
            # Run flake8
            result = subprocess.run(
                ["flake8", "--format=json", file_path],
                capture_output=True, text=True, timeout=30
            )

            if result.stdout:
                try:
                    flake8_issues = json.loads(result.stdout)
                    for issue in flake8_issues:
                        severity = CheckSeverity.MEDIUM
                        if issue.get("code", "").startswith("E9"):  # Syntax errors
                            severity = CheckSeverity.BLOCKING
                        elif issue.get("code", "").startswith(("E1", "E2")):  # Indentation/whitespace
                            severity = CheckSeverity.LOW

                        issues.append(CheckIssue(
                            check_name="python_linting",
                            severity=severity,
                            message=f"{issue.get('code')}: {issue.get('text')}",
                            location=f"{file_path}:{issue.get('line_number')}:{issue.get('column_number')}",
                            auto_fixable=severity == CheckSeverity.LOW
                        ))
                except json.JSONDecodeError:
                    pass

        except (subprocess.TimeoutExpired, FileNotFoundError):
            logger.warning(f"Could not run flake8 on {file_path}")

        return issues

    def _run_javascript_linting(self, file_path: str) -> List[CheckIssue]:
        """Run JavaScript/TypeScript linting."""
        issues = []

        try:
            # Run ESLint
            result = subprocess.run(
                ["eslint", "--format=json", file_path],
                capture_output=True, text=True, timeout=30
            )

            if result.stdout:
                try:
                    eslint_results = json.loads(result.stdout)
                    for file_result in eslint_results:
                        for message in file_result.get("messages", []):
                            severity_map = {
                                1: CheckSeverity.LOW,    # Warning
                                2: CheckSeverity.HIGH    # Error
                            }

                            issues.append(CheckIssue(
                                check_name="javascript_linting",
                                severity=severity_map.get(message.get("severity", 1), CheckSeverity.MEDIUM),
                                message=f"{message.get('ruleId', 'unknown')}: {message.get('message')}",
                                location=f"{file_path}:{message.get('line')}:{message.get('column')}",
                                auto_fixable=message.get("fix") is not None
                            ))
                except json.JSONDecodeError:
                    pass

        except (subprocess.TimeoutExpired, FileNotFoundError):
            logger.warning(f"Could not run eslint on {file_path}")

        return issues

    def _check_complexity(self, file_path: str) -> List[CheckIssue]:
        """Check code complexity metrics."""
        issues = []

        try:
            if file_path.endswith('.py'):
                # Use radon for Python complexity
                result = subprocess.run(
                    ["radon", "cc", "--json", file_path],
                    capture_output=True, text=True, timeout=30
                )

                if result.stdout:
                    try:
                        complexity_data = json.loads(result.stdout)
                        for file_data in complexity_data.values():
                            for func_data in file_data:
                                complexity = func_data.get("complexity", 0)
                                if complexity > 10:
                                    severity = CheckSeverity.HIGH if complexity > 15 else CheckSeverity.MEDIUM
                                    issues.append(CheckIssue(
                                        check_name="complexity",
                                        severity=severity,
                                        message=f"High complexity ({complexity}) in function {func_data.get('name')}",
                                        location=f"{file_path}:{func_data.get('lineno')}",
                                        suggestion="Consider breaking down into smaller functions"
                                    ))
                    except json.JSONDecodeError:
                        pass

        except (subprocess.TimeoutExpired, FileNotFoundError):
            pass

        return issues

    def _check_duplication(self, file_path: str) -> List[CheckIssue]:
        """Check for code duplication."""
        # This is a simplified implementation
        # In practice, you'd use tools like jscpd or similar
        return []


class SecurityChecker(BaseChecker):
    """Performs security analysis including vulnerability scanning and dependency audit."""

    def should_run(self, change_set: Dict[str, Any], trust_score: float) -> bool:
        # Always run security checks regardless of trust score
        return True

    def _perform_check(self, change_set: Dict[str, Any]) -> List[CheckIssue]:
        issues = []

        # Check for common security patterns
        issues.extend(self._check_security_patterns(change_set))

        # Check dependencies for vulnerabilities
        issues.extend(self._check_dependencies())

        # Check for secrets in code
        issues.extend(self._check_secrets(change_set))

        return issues

    def _check_security_patterns(self, change_set: Dict[str, Any]) -> List[CheckIssue]:
        """Check for common security anti-patterns."""
        issues = []

        dangerous_patterns = [
            (r"exec\(", "Dangerous use of exec()", CheckSeverity.HIGH),
            (r"eval\(", "Dangerous use of eval()", CheckSeverity.HIGH),
            (r"password\s*=\s*['\"][^'\"]+['\"]", "Hardcoded password", CheckSeverity.BLOCKING),
            (r"api[_-]?key\s*=\s*['\"][^'\"]+['\"]", "Hardcoded API key", CheckSeverity.BLOCKING),
            (r"sql\s*=.*\+.*input", "Potential SQL injection", CheckSeverity.HIGH),
        ]

        for file_change in change_set.get("file_changes", []):
            file_path = file_change.get("path", "")

            try:
                if Path(file_path).exists():
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read()

                    for pattern, message, severity in dangerous_patterns:
                        import re
                        matches = re.finditer(pattern, content, re.IGNORECASE)
                        for match in matches:
                            line_num = content[:match.start()].count('\n') + 1
                            issues.append(CheckIssue(
                                check_name="security_patterns",
                                severity=severity,
                                message=message,
                                location=f"{file_path}:{line_num}",
                                suggestion="Review and remove security risk"
                            ))

            except Exception as e:
                logger.warning(f"Could not scan {file_path} for security patterns: {e}")

        return issues

    def _check_dependencies(self) -> List[CheckIssue]:
        """Check dependencies for known vulnerabilities."""
        issues = []

        try:
            # Check Python dependencies with safety
            if Path("requirements.txt").exists():
                result = subprocess.run(
                    ["safety", "check", "--json"],
                    capture_output=True, text=True, timeout=60
                )

                if result.stdout:
                    try:
                        safety_data = json.loads(result.stdout)
                        for vuln in safety_data:
                            issues.append(CheckIssue(
                                check_name="dependency_security",
                                severity=CheckSeverity.HIGH,
                                message=f"Vulnerable dependency: {vuln.get('package')} {vuln.get('installed_version')}",
                                suggestion=f"Update to version {vuln.get('safe_versions', ['latest'])[0]}"
                            ))
                    except json.JSONDecodeError:
                        pass

        except (subprocess.TimeoutExpired, FileNotFoundError):
            logger.warning("Could not run dependency security check")

        return issues

    def _check_secrets(self, change_set: Dict[str, Any]) -> List[CheckIssue]:
        """Check for secrets in code using simple patterns."""
        issues = []

        secret_patterns = [
            (r"-----BEGIN [A-Z ]+ PRIVATE KEY-----", "Private key detected"),
            (r"['\"][A-Za-z0-9+/]{40,}['\"]", "Potential secret token"),
            (r"(?i)password['\"]?\s*[:=]\s*['\"][^'\"]+['\"]", "Hardcoded password"),
        ]

        for file_change in change_set.get("file_changes", []):
            file_path = file_change.get("path", "")

            try:
                if Path(file_path).exists() and not file_path.endswith(('.jpg', '.png', '.gif', '.pdf')):
                    with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                        content = f.read()

                    for pattern, message in secret_patterns:
                        import re
                        if re.search(pattern, content):
                            issues.append(CheckIssue(
                                check_name="secrets_detection",
                                severity=CheckSeverity.BLOCKING,
                                message=message,
                                location=file_path,
                                suggestion="Remove secret and use environment variables or secure storage"
                            ))

            except Exception as e:
                logger.warning(f"Could not scan {file_path} for secrets: {e}")

        return issues


class TestSuiteChecker(BaseChecker):
    """Runs test suites and checks coverage."""

    def should_run(self, change_set: Dict[str, Any], trust_score: float) -> bool:
        # Skip tests for high-trust developers with doc-only changes
        if trust_score > 0.85:
            code_changes = [fc for fc in change_set.get("file_changes", [])
                          if not fc.get("path", "").endswith(('.md', '.txt', '.rst'))]
            return len(code_changes) > 0
        return True

    def _perform_check(self, change_set: Dict[str, Any]) -> List[CheckIssue]:
        issues = []

        # Run unit tests
        issues.extend(self._run_unit_tests())

        # Check test coverage
        issues.extend(self._check_test_coverage(change_set))

        return issues

    def _run_unit_tests(self) -> List[CheckIssue]:
        """Run the unit test suite."""
        issues = []

        try:
            # Try pytest first
            result = subprocess.run(
                ["pytest", "--tb=short", "-v"],
                capture_output=True, text=True, timeout=300  # 5 minutes
            )

            if result.returncode != 0:
                # Parse pytest output for failed tests
                failed_tests = []
                for line in result.stdout.split('\n'):
                    if '::' in line and 'FAILED' in line:
                        failed_tests.append(line.strip())

                for failed_test in failed_tests:
                    issues.append(CheckIssue(
                        check_name="unit_tests",
                        severity=CheckSeverity.HIGH,
                        message=f"Test failed: {failed_test}",
                        suggestion="Fix failing test before proceeding"
                    ))

        except subprocess.TimeoutExpired:
            issues.append(CheckIssue(
                check_name="unit_tests",
                severity=CheckSeverity.HIGH,
                message="Test suite timed out (>5 minutes)",
                suggestion="Optimize test performance or increase timeout"
            ))
        except FileNotFoundError:
            # Try other test runners or skip
            logger.info("pytest not found, skipping unit tests")

        return issues

    def _check_test_coverage(self, change_set: Dict[str, Any]) -> List[CheckIssue]:
        """Check test coverage for changed files."""
        issues = []

        try:
            # Run coverage analysis
            result = subprocess.run(
                ["coverage", "run", "-m", "pytest"],
                capture_output=True, text=True, timeout=300
            )

            if result.returncode == 0:
                # Get coverage report
                coverage_result = subprocess.run(
                    ["coverage", "report", "--format=json"],
                    capture_output=True, text=True, timeout=30
                )

                if coverage_result.stdout:
                    try:
                        coverage_data = json.loads(coverage_result.stdout)
                        overall_coverage = coverage_data.get("totals", {}).get("percent_covered", 0)

                        if overall_coverage < 80:
                            issues.append(CheckIssue(
                                check_name="test_coverage",
                                severity=CheckSeverity.MEDIUM,
                                message=f"Low test coverage: {overall_coverage:.1f}%",
                                suggestion="Add tests to reach 80% coverage"
                            ))

                        # Check individual file coverage
                        for file_path, file_data in coverage_data.get("files", {}).items():
                            coverage_percent = file_data.get("summary", {}).get("percent_covered", 0)
                            if coverage_percent < 70:
                                issues.append(CheckIssue(
                                    check_name="file_coverage",
                                    severity=CheckSeverity.LOW,
                                    message=f"Low coverage in {file_path}: {coverage_percent:.1f}%",
                                    location=file_path,
                                    suggestion="Add tests for uncovered code paths"
                                ))

                    except json.JSONDecodeError:
                        pass

        except (subprocess.TimeoutExpired, FileNotFoundError):
            logger.info("Coverage analysis not available")

        return issues


class PerformanceChecker(BaseChecker):
    """Checks for performance issues and regressions."""

    def should_run(self, change_set: Dict[str, Any], trust_score: float) -> bool:
        # Run for code changes that might affect performance
        perf_sensitive_paths = ['**/api/**', '**/models/**', '**/database/**']
        changed_files = [fc.get("path", "") for fc in change_set.get("file_changes", [])]

        import fnmatch
        for file_path in changed_files:
            for pattern in perf_sensitive_paths:
                if fnmatch.fnmatch(file_path, pattern):
                    return True
        return False

    def _perform_check(self, change_set: Dict[str, Any]) -> List[CheckIssue]:
        issues = []

        # Check for performance anti-patterns
        issues.extend(self._check_performance_patterns(change_set))

        # Run basic performance tests
        issues.extend(self._run_performance_tests())

        return issues

    def _check_performance_patterns(self, change_set: Dict[str, Any]) -> List[CheckIssue]:
        """Check for common performance anti-patterns."""
        issues = []

        perf_patterns = [
            (r"for.*in.*\.objects\.all\(\)", "N+1 query pattern detected"),
            (r"while.*True:", "Potential infinite loop"),
            (r"\.join\(\)", "String concatenation in loop (use list.join instead)"),
            (r"time\.sleep\([0-9]+\)", "Long sleep in code"),
        ]

        for file_change in change_set.get("file_changes", []):
            file_path = file_change.get("path", "")

            try:
                if Path(file_path).exists() and file_path.endswith(('.py', '.js', '.ts')):
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read()

                    for pattern, message in perf_patterns:
                        import re
                        matches = re.finditer(pattern, content, re.IGNORECASE)
                        for match in matches:
                            line_num = content[:match.start()].count('\n') + 1
                            issues.append(CheckIssue(
                                check_name="performance_patterns",
                                severity=CheckSeverity.MEDIUM,
                                message=message,
                                location=f"{file_path}:{line_num}",
                                suggestion="Review for performance optimization"
                            ))

            except Exception as e:
                logger.warning(f"Could not analyze {file_path} for performance: {e}")

        return issues

    def _run_performance_tests(self) -> List[CheckIssue]:
        """Run basic performance tests if available."""
        # This would run performance test suites
        # Implementation depends on specific testing framework
        return []


class PreCheckRunner:
    """Orchestrates execution of all pre-checks."""

    def __init__(self, config_path: Optional[str] = None):
        self.config = self._load_config(config_path)
        self._initialize_checkers()

    def _load_config(self, config_path: Optional[str]) -> Dict[str, Any]:
        """Load configuration for pre-checks."""
        default_config = {
            "parallel_execution": True,
            "timeout_seconds": 600,  # 10 minutes total
            "fail_fast": False,
            "checkers": {
                "code_quality": {"enabled": True},
                "security": {"enabled": True},
                "testing": {"enabled": True},
                "performance": {"enabled": True}
            }
        }

        if config_path and Path(config_path).exists():
            try:
                with open(config_path, 'r') as f:
                    user_config = json.load(f)
                default_config.update(user_config)
            except Exception as e:
                logger.warning(f"Could not load config from {config_path}: {e}")

        return default_config

    def _initialize_checkers(self):
        """Initialize all available checkers."""
        self.checkers = {
            "code_quality": CodeQualityChecker(self.config.get("checkers", {}).get("code_quality")),
            "security": SecurityChecker(self.config.get("checkers", {}).get("security")),
            "testing": TestSuiteChecker(self.config.get("checkers", {}).get("testing")),
            "performance": PerformanceChecker(self.config.get("checkers", {}).get("performance"))
        }

    def run_all_checks(self, change_set: Dict[str, Any],
                      trust_score: float = 0.5) -> PreCheckReport:
        """Run all applicable pre-checks."""
        start_time = time.time()

        # Determine which checks to run
        checks_to_run = []
        for name, checker in self.checkers.items():
            if (self.config.get("checkers", {}).get(name, {}).get("enabled", True) and
                checker.should_run(change_set, trust_score)):
                checks_to_run.append((name, checker))

        logger.info(f"Running {len(checks_to_run)} pre-checks")

        # Execute checks
        if self.config.get("parallel_execution", True) and len(checks_to_run) > 1:
            check_results = self._run_checks_parallel(checks_to_run, change_set)
        else:
            check_results = self._run_checks_sequential(checks_to_run, change_set)

        # Generate report
        total_duration = time.time() - start_time
        overall_status = self._determine_overall_status(check_results)
        summary = self._generate_summary(check_results)
        recommendations = self._generate_recommendations(check_results, trust_score)

        return PreCheckReport(
            timestamp=datetime.datetime.now(),
            overall_status=overall_status,
            total_duration_seconds=total_duration,
            check_results=check_results,
            summary=summary,
            recommendations=recommendations
        )

    def _run_checks_parallel(self, checks_to_run: List[Tuple[str, BaseChecker]],
                           change_set: Dict[str, Any]) -> List[CheckResult]:
        """Run checks in parallel using ThreadPoolExecutor."""
        results = []

        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
            # Submit all checks
            future_to_check = {
                executor.submit(checker.execute, change_set): name
                for name, checker in checks_to_run
            }

            # Collect results
            for future in concurrent.futures.as_completed(future_to_check,
                                                         timeout=self.config.get("timeout_seconds", 600)):
                check_name = future_to_check[future]
                try:
                    result = future.result()
                    results.append(result)
                    logger.info(f"Check {check_name} completed: {result.status.value}")

                    # Fail fast if enabled and blocking issues found
                    if (self.config.get("fail_fast", False) and
                        result.has_blocking_issues):
                        logger.warning(f"Failing fast due to blocking issues in {check_name}")
                        # Cancel remaining futures
                        for f in future_to_check:
                            f.cancel()
                        break

                except Exception as e:
                    logger.error(f"Check {check_name} failed: {e}")
                    results.append(CheckResult(
                        check_name=check_name,
                        status=CheckStatus.ERROR,
                        duration_seconds=0,
                        issues=[CheckIssue(
                            check_name=check_name,
                            severity=CheckSeverity.HIGH,
                            message=f"Check execution failed: {str(e)}"
                        )],
                        metadata={},
                        summary=f"Check failed: {str(e)}"
                    ))

        return results

    def _run_checks_sequential(self, checks_to_run: List[Tuple[str, BaseChecker]],
                             change_set: Dict[str, Any]) -> List[CheckResult]:
        """Run checks sequentially."""
        results = []

        for name, checker in checks_to_run:
            logger.info(f"Running check: {name}")
            result = checker.execute(change_set)
            results.append(result)

            logger.info(f"Check {name} completed: {result.status.value}")

            # Fail fast if enabled and blocking issues found
            if (self.config.get("fail_fast", False) and
                result.has_blocking_issues):
                logger.warning(f"Failing fast due to blocking issues in {name}")
                break

        return results

    def _determine_overall_status(self, check_results: List[CheckResult]) -> CheckStatus:
        """Determine overall status from individual check results."""
        if not check_results:
            return CheckStatus.PASS

        statuses = [result.status for result in check_results]

        if CheckStatus.ERROR in statuses or CheckStatus.FAIL in statuses:
            return CheckStatus.FAIL
        elif CheckStatus.WARNING in statuses:
            return CheckStatus.WARNING
        else:
            return CheckStatus.PASS

    def _generate_summary(self, check_results: List[CheckResult]) -> Dict[str, Any]:
        """Generate summary statistics."""
        total_issues = sum(len(result.issues) for result in check_results)

        issue_counts = {severity.value: 0 for severity in CheckSeverity}
        for result in check_results:
            for issue in result.issues:
                issue_counts[issue.severity.value] += 1

        status_counts = {status.value: 0 for status in CheckStatus}
        for result in check_results:
            status_counts[result.status.value] += 1

        return {
            "checks_run": len(check_results),
            "total_issues": total_issues,
            "issue_counts_by_severity": issue_counts,
            "status_counts": status_counts,
            "total_duration": sum(result.duration_seconds for result in check_results)
        }

    def _generate_recommendations(self, check_results: List[CheckResult],
                                trust_score: float) -> List[str]:
        """Generate actionable recommendations based on results."""
        recommendations = []

        # Count issues by severity
        blocking_count = sum(
            len([i for i in result.issues if i.severity == CheckSeverity.BLOCKING])
            for result in check_results
        )
        high_count = sum(
            len([i for i in result.issues if i.severity == CheckSeverity.HIGH])
            for result in check_results
        )

        if blocking_count > 0:
            recommendations.append(f"Fix {blocking_count} blocking issues before proceeding")

        if high_count > 0:
            recommendations.append(f"Address {high_count} high-severity issues")

        # Trust-based recommendations
        if trust_score < 0.6:
            recommendations.append("Consider enhanced review due to low trust score")
        elif trust_score > 0.85:
            recommendations.append("High trust score allows for reduced review scope")

        # Check-specific recommendations
        auto_fixable = [
            issue for result in check_results for issue in result.issues
            if issue.auto_fixable
        ]
        if auto_fixable:
            recommendations.append(f"{len(auto_fixable)} issues can be auto-fixed")

        return recommendations

    def export_report(self, report: PreCheckReport, output_path: str):
        """Export pre-check report to file."""
        with open(output_path, 'w') as f:
            json.dump(asdict(report), f, indent=2, default=str)

        logger.info(f"Pre-check report exported to {output_path}")


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Atlas Pre-Check Runner")
    parser.add_argument("--config", help="Configuration file path")
    parser.add_argument("--trust-score", type=float, default=0.75,
                       help="Developer trust score (0.0-1.0)")
    parser.add_argument("--output", help="Output file for report")
    parser.add_argument("--change-set", help="JSON file with change set information")

    args = parser.parse_args()

    # Load change set
    change_set = {}
    if args.change_set and Path(args.change_set).exists():
        with open(args.change_set, 'r') as f:
            change_set = json.load(f)

    # Create runner and execute checks
    runner = PreCheckRunner(args.config)
    report = runner.run_all_checks(change_set, args.trust_score)

    # Print summary
    print(f"\n=== Pre-Check Results ===")
    print(f"Overall Status: {report.overall_status.value}")
    print(f"Checks Run: {report.summary['checks_run']}")
    print(f"Total Issues: {report.summary['total_issues']}")
    print(f"Duration: {report.total_duration_seconds:.1f} seconds")

    print(f"\nIssue Breakdown:")
    for severity, count in report.summary['issue_counts_by_severity'].items():
        if count > 0:
            print(f"  {severity}: {count}")

    print(f"\nRecommendations:")
    for recommendation in report.recommendations:
        print(f"  - {recommendation}")

    # Export report
    if args.output:
        runner.export_report(report, args.output)
        print(f"\nDetailed report saved to {args.output}")

    # Exit with error code if checks failed
    if report.overall_status in [CheckStatus.FAIL, CheckStatus.ERROR]:
        exit(1)