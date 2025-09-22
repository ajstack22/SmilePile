#!/usr/bin/env python3
"""
Atlas Differential Reviewer v2.1
Copyright 2024 Atlas Framework

Analyzes code changes and determines optimal review scope through intelligent
differential analysis and change impact assessment.
"""

import json
import logging
import datetime
import subprocess
import re
from typing import Dict, List, Set, Optional, Any, Tuple
from dataclasses import dataclass, asdict
from pathlib import Path
from enum import Enum
import hashlib

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class ChangeType(Enum):
    """Types of changes that can be detected."""
    ADDED = "added"
    MODIFIED = "modified"
    DELETED = "deleted"
    RENAMED = "renamed"
    COPIED = "copied"


class ImpactLevel(Enum):
    """Levels of impact a change can have."""
    NONE = "none"
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    CRITICAL = "critical"


class ReviewScope(Enum):
    """Scope levels for reviews."""
    SKIP = "skip"
    AUTOMATED_ONLY = "automated_only"
    SPOT_CHECK = "spot_check"
    TARGETED = "targeted"
    FULL = "full"
    ENHANCED = "enhanced"


@dataclass
class FileChange:
    """Represents a change to a single file."""
    path: str
    change_type: ChangeType
    lines_added: int
    lines_removed: int
    complexity_delta: float = 0.0
    functions_modified: List[str] = None
    imports_changed: List[str] = None
    api_changes: List[str] = None

    def __post_init__(self):
        if self.functions_modified is None:
            self.functions_modified = []
        if self.imports_changed is None:
            self.imports_changed = []
        if self.api_changes is None:
            self.api_changes = []

    @property
    def change_magnitude(self) -> float:
        """Calculate magnitude of change (0.0 to 1.0)."""
        total_lines = max(self.lines_added + self.lines_removed, 1)
        # Normalize by typical file size and complexity
        magnitude = min(total_lines / 200.0, 1.0)
        if self.complexity_delta > 0:
            magnitude *= (1.0 + self.complexity_delta / 10.0)
        return magnitude


@dataclass
class ComponentImpact:
    """Represents impact on a system component."""
    component_name: str
    impact_level: ImpactLevel
    affected_files: List[str]
    risk_factors: List[str]
    confidence: float = 0.0


@dataclass
class ReviewScopeRecommendation:
    """Represents recommended review scope."""
    overall_scope: ReviewScope
    component_scopes: Dict[str, ReviewScope]
    focus_areas: List[str]
    skip_areas: List[str]
    estimated_time_minutes: int
    rationale: str


@dataclass
class DifferentialAnalysis:
    """Complete differential analysis result."""
    base_commit: str
    target_commit: str
    file_changes: List[FileChange]
    component_impacts: List[ComponentImpact]
    scope_recommendation: ReviewScopeRecommendation
    metadata: Dict[str, Any]


class GitDiffAnalyzer:
    """Analyzes git diffs to extract change information."""

    def __init__(self, repo_path: str = "."):
        self.repo_path = Path(repo_path)

    def analyze_diff(self, base_commit: str, target_commit: str) -> Dict[str, Any]:
        """Analyze git diff between two commits."""
        try:
            # Get file changes
            file_changes = self._get_file_changes(base_commit, target_commit)

            # Get detailed diff information
            detailed_changes = []
            for file_path, change_type in file_changes.items():
                file_change = self._analyze_file_change(
                    file_path, change_type, base_commit, target_commit
                )
                detailed_changes.append(file_change)

            return {
                "file_changes": detailed_changes,
                "summary": self._generate_summary(detailed_changes),
                "commit_info": self._get_commit_info(base_commit, target_commit)
            }

        except Exception as e:
            logger.error(f"Error analyzing git diff: {e}")
            raise

    def _get_file_changes(self, base_commit: str, target_commit: str) -> Dict[str, ChangeType]:
        """Get list of changed files with their change types."""
        cmd = [
            "git", "diff", "--name-status",
            f"{base_commit}..{target_commit}"
        ]

        result = subprocess.run(
            cmd, cwd=self.repo_path, capture_output=True, text=True
        )

        if result.returncode != 0:
            raise RuntimeError(f"Git diff failed: {result.stderr}")

        file_changes = {}
        for line in result.stdout.strip().split('\n'):
            if not line:
                continue

            parts = line.split('\t')
            if len(parts) >= 2:
                status = parts[0]
                file_path = parts[1]

                if status.startswith('A'):
                    change_type = ChangeType.ADDED
                elif status.startswith('M'):
                    change_type = ChangeType.MODIFIED
                elif status.startswith('D'):
                    change_type = ChangeType.DELETED
                elif status.startswith('R'):
                    change_type = ChangeType.RENAMED
                elif status.startswith('C'):
                    change_type = ChangeType.COPIED
                else:
                    change_type = ChangeType.MODIFIED

                file_changes[file_path] = change_type

        return file_changes

    def _analyze_file_change(self, file_path: str, change_type: ChangeType,
                           base_commit: str, target_commit: str) -> FileChange:
        """Analyze detailed changes for a specific file."""

        if change_type == ChangeType.ADDED:
            lines_added, lines_removed = self._count_lines_added_file(file_path, target_commit)
        elif change_type == ChangeType.DELETED:
            lines_added, lines_removed = 0, self._count_lines_deleted_file(file_path, base_commit)
        else:
            lines_added, lines_removed = self._count_line_changes(
                file_path, base_commit, target_commit
            )

        # Analyze function changes for code files
        functions_modified = []
        imports_changed = []
        api_changes = []

        if self._is_code_file(file_path):
            functions_modified = self._get_modified_functions(
                file_path, base_commit, target_commit
            )
            imports_changed = self._get_import_changes(
                file_path, base_commit, target_commit
            )
            api_changes = self._detect_api_changes(
                file_path, base_commit, target_commit
            )

        return FileChange(
            path=file_path,
            change_type=change_type,
            lines_added=lines_added,
            lines_removed=lines_removed,
            functions_modified=functions_modified,
            imports_changed=imports_changed,
            api_changes=api_changes
        )

    def _count_line_changes(self, file_path: str, base_commit: str,
                          target_commit: str) -> Tuple[int, int]:
        """Count lines added and removed for a file."""
        cmd = [
            "git", "diff", "--numstat",
            f"{base_commit}..{target_commit}", "--", file_path
        ]

        result = subprocess.run(
            cmd, cwd=self.repo_path, capture_output=True, text=True
        )

        if result.returncode != 0 or not result.stdout.strip():
            return 0, 0

        parts = result.stdout.strip().split('\t')
        if len(parts) >= 2:
            try:
                added = int(parts[0]) if parts[0] != '-' else 0
                removed = int(parts[1]) if parts[1] != '-' else 0
                return added, removed
            except ValueError:
                return 0, 0

        return 0, 0

    def _count_lines_added_file(self, file_path: str, commit: str) -> Tuple[int, int]:
        """Count lines in a newly added file."""
        cmd = ["git", "show", f"{commit}:{file_path}"]
        result = subprocess.run(
            cmd, cwd=self.repo_path, capture_output=True, text=True
        )

        if result.returncode == 0:
            lines = len(result.stdout.splitlines())
            return lines, 0
        return 0, 0

    def _count_lines_deleted_file(self, file_path: str, commit: str) -> int:
        """Count lines in a deleted file."""
        cmd = ["git", "show", f"{commit}:{file_path}"]
        result = subprocess.run(
            cmd, cwd=self.repo_path, capture_output=True, text=True
        )

        if result.returncode == 0:
            return len(result.stdout.splitlines())
        return 0

    def _is_code_file(self, file_path: str) -> bool:
        """Check if file is a code file that should be analyzed for functions."""
        code_extensions = {'.py', '.js', '.ts', '.java', '.cpp', '.c', '.h',
                          '.rb', '.go', '.rs', '.php', '.cs', '.kt', '.swift'}
        return Path(file_path).suffix.lower() in code_extensions

    def _get_modified_functions(self, file_path: str, base_commit: str,
                              target_commit: str) -> List[str]:
        """Get list of functions that were modified."""
        # This is a simplified implementation
        # In practice, you'd use AST parsing or language-specific tools
        functions = []

        try:
            cmd = ["git", "diff", f"{base_commit}..{target_commit}", "--", file_path]
            result = subprocess.run(
                cmd, cwd=self.repo_path, capture_output=True, text=True
            )

            if result.returncode == 0:
                # Simple pattern matching for function definitions
                function_patterns = [
                    r'def\s+(\w+)\s*\(',  # Python
                    r'function\s+(\w+)\s*\(',  # JavaScript
                    r'(\w+)\s*\([^)]*\)\s*{',  # Java/C++/etc
                ]

                for line in result.stdout.splitlines():
                    if line.startswith('+') or line.startswith('-'):
                        for pattern in function_patterns:
                            match = re.search(pattern, line)
                            if match:
                                func_name = match.group(1)
                                if func_name not in functions:
                                    functions.append(func_name)

        except Exception as e:
            logger.warning(f"Could not analyze functions in {file_path}: {e}")

        return functions

    def _get_import_changes(self, file_path: str, base_commit: str,
                          target_commit: str) -> List[str]:
        """Get list of import changes."""
        imports = []

        try:
            cmd = ["git", "diff", f"{base_commit}..{target_commit}", "--", file_path]
            result = subprocess.run(
                cmd, cwd=self.repo_path, capture_output=True, text=True
            )

            if result.returncode == 0:
                import_patterns = [
                    r'import\s+(\S+)',  # Python/JavaScript
                    r'from\s+(\S+)\s+import',  # Python
                    r'#include\s+[<"]([^>"]+)[>"]',  # C/C++
                ]

                for line in result.stdout.splitlines():
                    if line.startswith('+') or line.startswith('-'):
                        for pattern in import_patterns:
                            match = re.search(pattern, line)
                            if match:
                                import_name = match.group(1)
                                if import_name not in imports:
                                    imports.append(import_name)

        except Exception as e:
            logger.warning(f"Could not analyze imports in {file_path}: {e}")

        return imports

    def _detect_api_changes(self, file_path: str, base_commit: str,
                          target_commit: str) -> List[str]:
        """Detect API changes (method signatures, endpoints, etc)."""
        api_changes = []

        # This is a simplified implementation
        # In practice, you'd use language-specific AST analysis
        try:
            cmd = ["git", "diff", f"{base_commit}..{target_commit}", "--", file_path]
            result = subprocess.run(
                cmd, cwd=self.repo_path, capture_output=True, text=True
            )

            if result.returncode == 0:
                # Look for common API patterns
                api_patterns = [
                    r'@app\.route\(["\']([^"\']+)["\']',  # Flask routes
                    r'@RequestMapping\(["\']([^"\']+)["\']',  # Spring routes
                    r'router\.(get|post|put|delete)\(["\']([^"\']+)["\']',  # Express routes
                ]

                for line in result.stdout.splitlines():
                    if line.startswith('+') or line.startswith('-'):
                        for pattern in api_patterns:
                            match = re.search(pattern, line)
                            if match:
                                if len(match.groups()) > 1:
                                    api_change = f"{match.group(1)} {match.group(2)}"
                                else:
                                    api_change = match.group(1)
                                if api_change not in api_changes:
                                    api_changes.append(api_change)

        except Exception as e:
            logger.warning(f"Could not analyze API changes in {file_path}: {e}")

        return api_changes

    def _generate_summary(self, file_changes: List[FileChange]) -> Dict[str, Any]:
        """Generate summary statistics for changes."""
        total_files = len(file_changes)
        total_lines_added = sum(fc.lines_added for fc in file_changes)
        total_lines_removed = sum(fc.lines_removed for fc in file_changes)

        change_types = {}
        for fc in file_changes:
            change_type = fc.change_type.value
            change_types[change_type] = change_types.get(change_type, 0) + 1

        return {
            "total_files_changed": total_files,
            "total_lines_added": total_lines_added,
            "total_lines_removed": total_lines_removed,
            "change_types": change_types,
            "change_magnitude": sum(fc.change_magnitude for fc in file_changes),
            "has_api_changes": any(fc.api_changes for fc in file_changes),
            "has_function_changes": any(fc.functions_modified for fc in file_changes)
        }

    def _get_commit_info(self, base_commit: str, target_commit: str) -> Dict[str, Any]:
        """Get commit information."""
        try:
            # Get commit messages
            cmd = [
                "git", "log", "--oneline", f"{base_commit}..{target_commit}"
            ]
            result = subprocess.run(
                cmd, cwd=self.repo_path, capture_output=True, text=True
            )

            commit_messages = result.stdout.strip().split('\n') if result.stdout.strip() else []

            return {
                "base_commit": base_commit,
                "target_commit": target_commit,
                "commit_count": len(commit_messages),
                "commit_messages": commit_messages
            }

        except Exception as e:
            logger.warning(f"Could not get commit info: {e}")
            return {
                "base_commit": base_commit,
                "target_commit": target_commit,
                "commit_count": 0,
                "commit_messages": []
            }


class ChangeImpactAssessor:
    """Assesses the impact of changes on system components."""

    def __init__(self, component_config_path: Optional[str] = None):
        self.component_mapping = self._load_component_mapping(component_config_path)

    def _load_component_mapping(self, config_path: Optional[str]) -> Dict[str, Dict[str, Any]]:
        """Load component mapping configuration."""
        default_mapping = {
            "authentication": {
                "paths": ["**/auth/**", "**/login/**", "**/security/**"],
                "keywords": ["auth", "login", "password", "token", "session"],
                "criticality": "high"
            },
            "database": {
                "paths": ["**/models/**", "**/db/**", "**/migrations/**"],
                "keywords": ["db", "database", "model", "schema", "migration"],
                "criticality": "high"
            },
            "api": {
                "paths": ["**/api/**", "**/controllers/**", "**/routes/**"],
                "keywords": ["api", "endpoint", "route", "controller"],
                "criticality": "medium"
            },
            "ui": {
                "paths": ["**/ui/**", "**/components/**", "**/views/**"],
                "keywords": ["ui", "component", "view", "template"],
                "criticality": "medium"
            },
            "testing": {
                "paths": ["**/test/**", "**/tests/**", "**/*test*"],
                "keywords": ["test", "spec", "mock"],
                "criticality": "low"
            },
            "documentation": {
                "paths": ["**/docs/**", "**/*.md", "**/*.rst"],
                "keywords": ["doc", "readme", "guide"],
                "criticality": "low"
            }
        }

        if config_path and Path(config_path).exists():
            try:
                with open(config_path, 'r') as f:
                    user_config = json.load(f)
                default_mapping.update(user_config)
            except Exception as e:
                logger.warning(f"Could not load component config: {e}")

        return default_mapping

    def assess_impact(self, diff_analysis: Dict[str, Any]) -> List[ComponentImpact]:
        """Assess impact of changes on system components."""
        file_changes = diff_analysis["file_changes"]
        component_impacts = []

        for component_name, component_config in self.component_mapping.items():
            impact = self._assess_component_impact(component_name, component_config, file_changes)
            if impact.impact_level != ImpactLevel.NONE:
                component_impacts.append(impact)

        return component_impacts

    def _assess_component_impact(self, component_name: str, component_config: Dict[str, Any],
                               file_changes: List[FileChange]) -> ComponentImpact:
        """Assess impact on a specific component."""
        affected_files = []
        risk_factors = []
        impact_score = 0.0

        for file_change in file_changes:
            if self._file_affects_component(file_change.path, component_config):
                affected_files.append(file_change.path)

                # Calculate impact based on change characteristics
                file_impact = self._calculate_file_impact(file_change, component_config)
                impact_score += file_impact

                # Add risk factors
                if file_change.api_changes:
                    risk_factors.append("API changes detected")
                if file_change.change_magnitude > 0.5:
                    risk_factors.append("Large change magnitude")
                if file_change.change_type in [ChangeType.ADDED, ChangeType.DELETED]:
                    risk_factors.append(f"File {file_change.change_type.value}")

        # Determine impact level
        if not affected_files:
            impact_level = ImpactLevel.NONE
        elif impact_score < 0.2:
            impact_level = ImpactLevel.LOW
        elif impact_score < 0.5:
            impact_level = ImpactLevel.MEDIUM
        elif impact_score < 0.8:
            impact_level = ImpactLevel.HIGH
        else:
            impact_level = ImpactLevel.CRITICAL

        # Adjust for component criticality
        criticality = component_config.get("criticality", "medium")
        if criticality == "high" and impact_level != ImpactLevel.NONE:
            if impact_level == ImpactLevel.LOW:
                impact_level = ImpactLevel.MEDIUM
            elif impact_level == ImpactLevel.MEDIUM:
                impact_level = ImpactLevel.HIGH

        confidence = min(len(affected_files) / 5.0, 1.0)  # More files = higher confidence

        return ComponentImpact(
            component_name=component_name,
            impact_level=impact_level,
            affected_files=affected_files,
            risk_factors=list(set(risk_factors)),
            confidence=confidence
        )

    def _file_affects_component(self, file_path: str, component_config: Dict[str, Any]) -> bool:
        """Check if a file affects a component."""
        import fnmatch

        # Check path patterns
        for pattern in component_config.get("paths", []):
            if fnmatch.fnmatch(file_path, pattern):
                return True

        # Check keywords in path
        path_lower = file_path.lower()
        for keyword in component_config.get("keywords", []):
            if keyword.lower() in path_lower:
                return True

        return False

    def _calculate_file_impact(self, file_change: FileChange,
                             component_config: Dict[str, Any]) -> float:
        """Calculate the impact score for a file change on a component."""
        base_impact = file_change.change_magnitude

        # Multiply by criticality
        criticality_multiplier = {
            "low": 0.5,
            "medium": 1.0,
            "high": 1.5
        }.get(component_config.get("criticality", "medium"), 1.0)

        # Add bonuses for specific change types
        if file_change.api_changes:
            base_impact += 0.3
        if file_change.functions_modified:
            base_impact += 0.2
        if file_change.change_type == ChangeType.DELETED:
            base_impact += 0.4

        return base_impact * criticality_multiplier


class ReviewScopeOptimizer:
    """Optimizes review scope based on change impact and trust scores."""

    def determine_scope(self, component_impacts: List[ComponentImpact],
                      trust_score: float = 0.5,
                      context: Optional[Dict[str, Any]] = None) -> ReviewScopeRecommendation:
        """Determine optimal review scope."""

        if context is None:
            context = {}

        # Calculate overall risk
        overall_risk = self._calculate_overall_risk(component_impacts, context)

        # Determine scope based on risk and trust
        overall_scope = self._determine_overall_scope(overall_risk, trust_score)

        # Determine component-specific scopes
        component_scopes = {}
        focus_areas = []
        skip_areas = []

        for impact in component_impacts:
            component_scope = self._determine_component_scope(impact, trust_score)
            component_scopes[impact.component_name] = component_scope

            if component_scope in [ReviewScope.FULL, ReviewScope.ENHANCED]:
                focus_areas.append(impact.component_name)
            elif component_scope in [ReviewScope.SKIP, ReviewScope.AUTOMATED_ONLY]:
                skip_areas.append(impact.component_name)

        # Estimate review time
        estimated_time = self._estimate_review_time(overall_scope, component_impacts)

        # Generate rationale
        rationale = self._generate_rationale(overall_scope, overall_risk, trust_score)

        return ReviewScopeRecommendation(
            overall_scope=overall_scope,
            component_scopes=component_scopes,
            focus_areas=focus_areas,
            skip_areas=skip_areas,
            estimated_time_minutes=estimated_time,
            rationale=rationale
        )

    def _calculate_overall_risk(self, component_impacts: List[ComponentImpact],
                              context: Dict[str, Any]) -> float:
        """Calculate overall risk score (0.0 to 1.0)."""
        if not component_impacts:
            return 0.0

        # Base risk from component impacts
        impact_scores = {
            ImpactLevel.NONE: 0.0,
            ImpactLevel.LOW: 0.2,
            ImpactLevel.MEDIUM: 0.4,
            ImpactLevel.HIGH: 0.7,
            ImpactLevel.CRITICAL: 1.0
        }

        weighted_risk = 0.0
        total_weight = 0.0

        for impact in component_impacts:
            risk = impact_scores[impact.impact_level]
            weight = impact.confidence
            weighted_risk += risk * weight
            total_weight += weight

        base_risk = weighted_risk / total_weight if total_weight > 0 else 0.0

        # Adjust for context
        context_multiplier = 1.0
        if context.get("release_deadline") == "urgent":
            context_multiplier *= 0.8  # Reduce review scope for urgent releases
        if context.get("has_breaking_changes"):
            context_multiplier *= 1.3  # Increase for breaking changes

        return min(base_risk * context_multiplier, 1.0)

    def _determine_overall_scope(self, risk: float, trust_score: float) -> ReviewScope:
        """Determine overall review scope based on risk and trust."""

        # Risk-trust matrix
        if trust_score >= 0.9:  # Very high trust
            if risk < 0.3:
                return ReviewScope.AUTOMATED_ONLY
            elif risk < 0.6:
                return ReviewScope.SPOT_CHECK
            else:
                return ReviewScope.TARGETED
        elif trust_score >= 0.75:  # High trust
            if risk < 0.2:
                return ReviewScope.SPOT_CHECK
            elif risk < 0.5:
                return ReviewScope.TARGETED
            else:
                return ReviewScope.FULL
        elif trust_score >= 0.6:  # Medium trust
            if risk < 0.2:
                return ReviewScope.TARGETED
            elif risk < 0.4:
                return ReviewScope.FULL
            else:
                return ReviewScope.ENHANCED
        else:  # Low trust
            if risk < 0.3:
                return ReviewScope.FULL
            else:
                return ReviewScope.ENHANCED

    def _determine_component_scope(self, impact: ComponentImpact,
                                 trust_score: float) -> ReviewScope:
        """Determine review scope for a specific component."""
        # Component-specific scoping
        if impact.impact_level == ImpactLevel.NONE:
            return ReviewScope.SKIP
        elif impact.impact_level == ImpactLevel.LOW and trust_score > 0.8:
            return ReviewScope.AUTOMATED_ONLY
        elif impact.impact_level == ImpactLevel.CRITICAL:
            return ReviewScope.ENHANCED
        elif impact.impact_level == ImpactLevel.HIGH:
            return ReviewScope.FULL if trust_score < 0.7 else ReviewScope.TARGETED
        else:  # MEDIUM
            return ReviewScope.TARGETED if trust_score < 0.8 else ReviewScope.SPOT_CHECK

    def _estimate_review_time(self, scope: ReviewScope,
                            component_impacts: List[ComponentImpact]) -> int:
        """Estimate review time in minutes."""
        base_times = {
            ReviewScope.SKIP: 0,
            ReviewScope.AUTOMATED_ONLY: 5,
            ReviewScope.SPOT_CHECK: 15,
            ReviewScope.TARGETED: 45,
            ReviewScope.FULL: 120,
            ReviewScope.ENHANCED: 180
        }

        base_time = base_times.get(scope, 60)

        # Adjust for number of affected components
        component_count = len([i for i in component_impacts if i.impact_level != ImpactLevel.NONE])
        time_multiplier = 1.0 + (component_count - 1) * 0.3

        return int(base_time * time_multiplier)

    def _generate_rationale(self, scope: ReviewScope, risk: float,
                          trust_score: float) -> str:
        """Generate human-readable rationale for the scope decision."""
        trust_desc = "high" if trust_score > 0.8 else "medium" if trust_score > 0.6 else "low"
        risk_desc = "high" if risk > 0.6 else "medium" if risk > 0.3 else "low"

        rationale = f"Recommended {scope.value} review based on {risk_desc} risk and {trust_desc} developer trust. "

        if scope == ReviewScope.AUTOMATED_ONLY:
            rationale += "Changes are low-risk and developer has high trust score."
        elif scope == ReviewScope.SPOT_CHECK:
            rationale += "Limited review needed due to good trust score and moderate changes."
        elif scope == ReviewScope.TARGETED:
            rationale += "Focus review on changed components and their integration points."
        elif scope == ReviewScope.FULL:
            rationale += "Comprehensive review recommended due to significant changes or moderate trust."
        elif scope == ReviewScope.ENHANCED:
            rationale += "Enhanced review with additional verification due to high risk or low trust."

        return rationale


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Atlas Differential Reviewer")
    parser.add_argument("base_commit", help="Base commit for comparison")
    parser.add_argument("target_commit", help="Target commit for comparison")
    parser.add_argument("--trust-score", type=float, default=0.75,
                       help="Developer trust score (0.0-1.0)")
    parser.add_argument("--repo-path", default=".", help="Repository path")
    parser.add_argument("--output", help="Output file for analysis results")
    parser.add_argument("--component-config", help="Component mapping configuration file")

    args = parser.parse_args()

    try:
        # Initialize components
        git_analyzer = GitDiffAnalyzer(args.repo_path)
        impact_assessor = ChangeImpactAssessor(args.component_config)
        scope_optimizer = ReviewScopeOptimizer()

        # Perform analysis
        logger.info(f"Analyzing changes from {args.base_commit} to {args.target_commit}")

        diff_analysis = git_analyzer.analyze_diff(args.base_commit, args.target_commit)
        component_impacts = impact_assessor.assess_impact(diff_analysis)
        scope_recommendation = scope_optimizer.determine_scope(
            component_impacts, args.trust_score
        )

        # Create full analysis result
        analysis = DifferentialAnalysis(
            base_commit=args.base_commit,
            target_commit=args.target_commit,
            file_changes=diff_analysis["file_changes"],
            component_impacts=component_impacts,
            scope_recommendation=scope_recommendation,
            metadata={
                "analysis_timestamp": datetime.datetime.now().isoformat(),
                "trust_score": args.trust_score,
                "summary": diff_analysis["summary"]
            }
        )

        # Output results
        if args.output:
            with open(args.output, 'w') as f:
                json.dump(asdict(analysis), f, indent=2, default=str)
            logger.info(f"Analysis saved to {args.output}")

        # Print summary
        print(f"\n=== Differential Review Analysis ===")
        print(f"Files changed: {len(analysis.file_changes)}")
        print(f"Components affected: {len(analysis.component_impacts)}")
        print(f"Recommended scope: {scope_recommendation.overall_scope.value}")
        print(f"Estimated time: {scope_recommendation.estimated_time_minutes} minutes")
        print(f"Focus areas: {', '.join(scope_recommendation.focus_areas)}")
        print(f"Rationale: {scope_recommendation.rationale}")

    except Exception as e:
        logger.error(f"Analysis failed: {e}")
        exit(1)