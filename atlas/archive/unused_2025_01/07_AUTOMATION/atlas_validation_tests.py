#!/usr/bin/env python3
"""
Atlas 2.1 Validation Tests
Copyright 2024 Atlas Framework

Comprehensive validation tests for Atlas 2.1 components including parallel execution,
graduated review system, and smart re-review process.
"""

import unittest
import json
import tempfile
import datetime
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock

# Import Atlas components
from dependency_graph import (
    DependencyGraphAnalyzer, Task, TaskType, Dependency, DependencyType
)
from parallel_orchestrator import (
    ParallelOrchestrator, Agent, AgentType
)
from review_decision_matrix import (
    ReviewDecisionEngine, ReviewIssue, IssueSeverity, IssueCategory
)
from differential_reviewer import (
    DifferentialReviewer, GitDiffAnalyzer, ChangeImpactAssessor
)
from trust_scorer import (
    TrustScoreCalculator, ReviewRecord, ReviewOutcome
)
from pre_check_runner import (
    PreCheckRunner, CodeQualityChecker, SecurityChecker
)


class TestDependencyGraphAnalyzer(unittest.TestCase):
    """Test the dependency graph analysis functionality."""

    def setUp(self):
        self.analyzer = DependencyGraphAnalyzer()

    def test_add_task(self):
        """Test adding tasks to the dependency graph."""
        task = Task(
            id="test_task",
            name="Test Task",
            task_type=TaskType.IMPLEMENTATION,
            estimated_duration=30
        )

        self.analyzer.add_task(task)
        self.assertIn("test_task", self.analyzer.tasks)
        self.assertEqual(self.analyzer.tasks["test_task"].name, "Test Task")

    def test_add_dependency(self):
        """Test adding dependencies between tasks."""
        task1 = Task("task1", "Task 1", TaskType.RESEARCH, 20)
        task2 = Task("task2", "Task 2", TaskType.IMPLEMENTATION, 40)

        self.analyzer.add_task(task1)
        self.analyzer.add_task(task2)

        dependency = Dependency(
            from_task="task1",
            to_task="task2",
            dependency_type=DependencyType.BLOCKS,
            reason="Implementation needs research"
        )

        self.analyzer.add_dependency(dependency)
        self.assertIn(dependency, self.analyzer.dependencies)

    def test_circular_dependency_detection(self):
        """Test detection of circular dependencies."""
        # Create circular dependency: A -> B -> C -> A
        tasks = [
            Task("A", "Task A", TaskType.RESEARCH, 20),
            Task("B", "Task B", TaskType.DESIGN, 30),
            Task("C", "Task C", TaskType.IMPLEMENTATION, 40)
        ]

        for task in tasks:
            self.analyzer.add_task(task)

        dependencies = [
            Dependency("A", "B", DependencyType.BLOCKS, "A blocks B"),
            Dependency("B", "C", DependencyType.BLOCKS, "B blocks C"),
            Dependency("C", "A", DependencyType.BLOCKS, "C blocks A")  # Creates cycle
        ]

        for dep in dependencies:
            self.analyzer.add_dependency(dep)

        cycles = self.analyzer.detect_circular_dependencies()
        self.assertGreater(len(cycles), 0, "Should detect circular dependency")

    def test_critical_path_calculation(self):
        """Test critical path calculation."""
        # Create linear dependency chain
        tasks = [
            Task("start", "Start", TaskType.RESEARCH, 10),
            Task("middle", "Middle", TaskType.DESIGN, 20),
            Task("end", "End", TaskType.IMPLEMENTATION, 30)
        ]

        for task in tasks:
            self.analyzer.add_task(task)

        dependencies = [
            Dependency("start", "middle", DependencyType.BLOCKS, "Sequential"),
            Dependency("middle", "end", DependencyType.BLOCKS, "Sequential")
        ]

        for dep in dependencies:
            self.analyzer.add_dependency(dep)

        path, duration = self.analyzer.calculate_critical_path()
        self.assertEqual(len(path), 3)
        self.assertEqual(duration, 60)  # 10 + 20 + 30

    def test_wave_generation(self):
        """Test execution wave generation."""
        # Create tasks with mixed dependencies
        tasks = [
            Task("research1", "Research 1", TaskType.RESEARCH, 20),
            Task("research2", "Research 2", TaskType.RESEARCH, 15),
            Task("design", "Design", TaskType.DESIGN, 30),
            Task("impl", "Implementation", TaskType.IMPLEMENTATION, 45)
        ]

        for task in tasks:
            self.analyzer.add_task(task)

        # research1 and research2 can be parallel, then design, then implementation
        dependencies = [
            Dependency("research1", "design", DependencyType.BLOCKS, "Research needed"),
            Dependency("research2", "design", DependencyType.BLOCKS, "Research needed"),
            Dependency("design", "impl", DependencyType.BLOCKS, "Design needed")
        ]

        for dep in dependencies:
            self.analyzer.add_dependency(dep)

        waves = self.analyzer.generate_execution_waves(max_agents=3)

        # Should have 3 waves: [research1, research2], [design], [impl]
        self.assertEqual(len(waves), 3)
        self.assertEqual(len(waves[0].tasks), 2)  # Both research tasks
        self.assertEqual(len(waves[1].tasks), 1)  # Design task
        self.assertEqual(len(waves[2].tasks), 1)  # Implementation task

    def test_speedup_calculation(self):
        """Test parallel speedup calculation."""
        # Add some tasks
        tasks = [
            Task("task1", "Task 1", TaskType.RESEARCH, 30),
            Task("task2", "Task 2", TaskType.RESEARCH, 30),
            Task("task3", "Task 3", TaskType.IMPLEMENTATION, 60)
        ]

        for task in tasks:
            self.analyzer.add_task(task)

        # task1 and task2 can be parallel, task3 depends on both
        dependencies = [
            Dependency("task1", "task3", DependencyType.BLOCKS, "Research needed"),
            Dependency("task2", "task3", DependencyType.BLOCKS, "Research needed")
        ]

        for dep in dependencies:
            self.analyzer.add_dependency(dep)

        speedup = self.analyzer.calculate_speedup_potential(max_agents=2)

        # Sequential: 30 + 30 + 60 = 120 minutes
        # Parallel: max(30, 30) + 60 = 90 minutes
        # Speedup: 120/90 = 1.33x
        self.assertEqual(speedup['sequential_time'], 120)
        self.assertEqual(speedup['parallel_time'], 90)
        self.assertAlmostEqual(speedup['speedup_factor'], 1.33, places=2)


class TestParallelOrchestrator(unittest.TestCase):
    """Test parallel orchestration functionality."""

    def setUp(self):
        self.orchestrator = ParallelOrchestrator(max_workers=3)

    def test_agent_management(self):
        """Test adding and managing agents."""
        agent = Agent(
            id="test_agent",
            agent_type=AgentType.DEVELOPER,
            capabilities=[TaskType.IMPLEMENTATION, TaskType.TESTING]
        )

        self.orchestrator.add_agent(agent)
        self.assertIn("test_agent", self.orchestrator.agents)

    def test_agent_selection(self):
        """Test selecting appropriate agents for tasks."""
        # Add agents with different capabilities
        researcher = Agent("researcher", AgentType.RESEARCHER, [TaskType.RESEARCH])
        developer = Agent("developer", AgentType.DEVELOPER, [TaskType.IMPLEMENTATION])

        self.orchestrator.add_agent(researcher)
        self.orchestrator.add_agent(developer)

        # Test finding appropriate agent
        research_task = Task("research", "Research", TaskType.RESEARCH, 20)
        impl_task = Task("impl", "Implementation", TaskType.IMPLEMENTATION, 40)

        research_agent = self.orchestrator.find_available_agent(research_task)
        impl_agent = self.orchestrator.find_available_agent(impl_task)

        self.assertEqual(research_agent.id, "researcher")
        self.assertEqual(impl_agent.id, "developer")

    @patch('time.sleep')  # Mock sleep to speed up tests
    def test_task_execution(self):
        """Test individual task execution."""
        self.orchestrator.load_default_agents()

        task = Task("test_task", "Test Task", TaskType.IMPLEMENTATION, 5)
        self.orchestrator.dependency_analyzer.add_task(task)
        self.orchestrator.task_executions["test_task"] = Mock()
        self.orchestrator.task_executions["test_task"].status = Mock()

        # Mock the execution to avoid actual work
        with patch.object(self.orchestrator, '_simulate_task_execution') as mock_sim:
            mock_sim.return_value = {"task_id": "test_task", "status": "completed"}

            result = self.orchestrator.execute_task("test_task")
            self.assertIsNotNone(result)


class TestReviewDecisionEngine(unittest.TestCase):
    """Test graduated review decision making."""

    def setUp(self):
        self.engine = ReviewDecisionEngine()

    def test_pass_decision(self):
        """Test PASS decision for clean code."""
        issues = [
            ReviewIssue(
                id="info1",
                title="Minor style issue",
                description="Consider using f-strings",
                severity=IssueSeverity.INFO,
                category=IssueCategory.MAINTAINABILITY,
                location="file.py:10"
            )
        ]

        decision = self.engine.make_review_decision(issues, Mock())
        self.assertEqual(decision.verdict.value, "pass")

    def test_conditional_pass_decision(self):
        """Test CONDITIONAL_PASS decision for minor issues."""
        issues = [
            ReviewIssue(
                id="med1",
                title="Missing test",
                description="Add unit test for new function",
                severity=IssueSeverity.MEDIUM,
                category=IssueCategory.TESTING,
                location="file.py:50",
                effort_estimate_hours=2.0
            )
        ]

        decision = self.engine.make_review_decision(issues, Mock())
        self.assertIn(decision.verdict.value, ["conditional_pass", "pass_with_conditions"])
        self.assertGreater(len(decision.conditions), 0)

    def test_blocked_decision(self):
        """Test BLOCKED decision for critical issues."""
        issues = [
            ReviewIssue(
                id="blocked1",
                title="Security vulnerability",
                description="SQL injection risk",
                severity=IssueSeverity.BLOCKED,
                category=IssueCategory.SECURITY,
                location="file.py:25"
            )
        ]

        decision = self.engine.make_review_decision(issues, Mock())
        self.assertEqual(decision.verdict.value, "blocked")
        self.assertTrue(decision.escalation_required)

    def test_debt_accepted_decision(self):
        """Test DEBT_ACCEPTED decision for maintainability issues."""
        issues = [
            ReviewIssue(
                id="debt1",
                title="Code complexity",
                description="Function could be refactored",
                severity=IssueSeverity.MEDIUM,
                category=IssueCategory.MAINTAINABILITY,
                location="file.py:100",
                effort_estimate_hours=8.0
            )
        ]

        decision = self.engine.make_review_decision(issues, Mock())
        # This might be debt_accepted or soft_reject depending on thresholds
        self.assertIn(decision.verdict.value, ["debt_accepted", "soft_reject"])


class TestDifferentialReviewer(unittest.TestCase):
    """Test smart differential review functionality."""

    def setUp(self):
        self.reviewer = DifferentialReviewer()

    @patch('subprocess.run')
    def test_file_change_detection(self, mock_run):
        """Test detection of file changes."""
        # Mock git diff output
        mock_run.return_value = Mock(
            returncode=0,
            stdout="M\tsrc/main.py\nA\tsrc/new.py\nD\tsrc/old.py\n"
        )

        git_analyzer = GitDiffAnalyzer()
        changes = git_analyzer._get_file_changes("base", "target")

        self.assertEqual(len(changes), 3)
        self.assertIn("src/main.py", changes)
        self.assertIn("src/new.py", changes)
        self.assertIn("src/old.py", changes)

    def test_component_impact_assessment(self):
        """Test assessment of change impact on components."""
        assessor = ChangeImpactAssessor()

        # Mock change data
        change_data = {
            "file_changes": [
                {"path": "src/auth/login.py", "change_type": "modified"},
                {"path": "src/api/users.py", "change_type": "modified"},
                {"path": "docs/README.md", "change_type": "modified"}
            ]
        }

        impacts = assessor.assess_impact(change_data)

        # Should detect impact on authentication and API components
        component_names = [impact.component_name for impact in impacts]
        self.assertIn("authentication", component_names)
        self.assertIn("api", component_names)


class TestTrustScorer(unittest.TestCase):
    """Test developer trust scoring system."""

    def setUp(self):
        self.calculator = TrustScoreCalculator(":memory:")  # Use in-memory DB for tests

    def test_trust_score_calculation(self):
        """Test basic trust score calculation."""
        # Create sample review records
        records = [
            ReviewRecord(
                review_id="r1",
                developer_id="dev1",
                timestamp=datetime.datetime.now(),
                outcome=ReviewOutcome.PASS,
                cycles_count=1,
                quality_score=90.0,
                issue_count=1
            ),
            ReviewRecord(
                review_id="r2",
                developer_id="dev1",
                timestamp=datetime.datetime.now(),
                outcome=ReviewOutcome.PASS,
                cycles_count=1,
                quality_score=85.0,
                issue_count=2
            )
        ]

        # Save records
        for record in records:
            self.calculator.db.save_review_record(record)

        # Calculate trust score
        trust_score = self.calculator.calculate_trust_score("dev1")

        self.assertIsNotNone(trust_score)
        self.assertGreater(trust_score.final_score, 0.7)  # Should be high for good records
        self.assertEqual(trust_score.developer_id, "dev1")

    def test_trust_category_assignment(self):
        """Test trust category assignment based on score."""
        # Test expert level (high scores)
        expert_records = [
            ReviewRecord(
                review_id=f"r{i}",
                developer_id="expert",
                timestamp=datetime.datetime.now(),
                outcome=ReviewOutcome.PASS,
                cycles_count=1,
                quality_score=95.0,
                issue_count=0
            ) for i in range(5)
        ]

        for record in expert_records:
            self.calculator.db.save_review_record(record)

        expert_score = self.calculator.calculate_trust_score("expert")
        self.assertGreater(expert_score.final_score, 0.85)

        # Test novice level (lower scores)
        novice_records = [
            ReviewRecord(
                review_id=f"r{i}",
                developer_id="novice",
                timestamp=datetime.datetime.now(),
                outcome=ReviewOutcome.REJECT,
                cycles_count=3,
                quality_score=60.0,
                issue_count=5
            ) for i in range(5)
        ]

        for record in novice_records:
            self.calculator.db.save_review_record(record)

        novice_score = self.calculator.calculate_trust_score("novice")
        self.assertLess(novice_score.final_score, 0.7)


class TestPreCheckRunner(unittest.TestCase):
    """Test automated pre-check system."""

    def setUp(self):
        self.runner = PreCheckRunner()

    def test_code_quality_checker(self):
        """Test code quality checking."""
        checker = CodeQualityChecker()

        # Mock change set with Python file
        change_set = {
            "file_changes": [
                {
                    "path": "test_file.py",
                    "change_type": "modified",
                    "lines_added": 10,
                    "lines_removed": 5
                }
            ]
        }

        # Should run for code changes
        self.assertTrue(checker.should_run(change_set, 0.5))

        # Test with docs-only changes
        docs_change_set = {
            "file_changes": [
                {
                    "path": "README.md",
                    "change_type": "modified",
                    "lines_added": 5,
                    "lines_removed": 0
                }
            ]
        }

        # Should not run for docs-only changes
        self.assertFalse(checker.should_run(docs_change_set, 0.5))

    def test_security_checker(self):
        """Test security checking."""
        checker = SecurityChecker()

        # Security checks should always run
        self.assertTrue(checker.should_run({}, 0.9))
        self.assertTrue(checker.should_run({}, 0.1))

    @patch('tempfile.NamedTemporaryFile')
    def test_secret_detection(self, mock_tempfile):
        """Test detection of secrets in code."""
        checker = SecurityChecker()

        # Create temporary file with secret
        mock_file = Mock()
        mock_file.name = "test_file.py"
        mock_tempfile.return_value.__enter__.return_value = mock_file

        with patch('pathlib.Path.exists', return_value=True), \
             patch('builtins.open', mock_file), \
             patch.object(mock_file, 'read', return_value='password = "secret123"'):

            change_set = {
                "file_changes": [{"path": "test_file.py"}]
            }

            issues = checker._check_secrets(change_set)
            # Should detect the hardcoded password
            self.assertGreater(len(issues), 0)


class TestIntegration(unittest.TestCase):
    """Integration tests for Atlas 2.1 components."""

    def test_parallel_workflow_integration(self):
        """Test integration between dependency analysis and parallel execution."""
        # Create dependency analyzer with sample tasks
        analyzer = DependencyGraphAnalyzer()

        tasks = [
            Task("research1", "Research Auth", TaskType.RESEARCH, 20),
            Task("research2", "Research DB", TaskType.RESEARCH, 15),
            Task("design", "Design System", TaskType.DESIGN, 30),
            Task("implement", "Implement", TaskType.IMPLEMENTATION, 60)
        ]

        for task in tasks:
            analyzer.add_task(task)

        dependencies = [
            Dependency("research1", "design", DependencyType.BLOCKS, "Research needed"),
            Dependency("research2", "design", DependencyType.BLOCKS, "Research needed"),
            Dependency("design", "implement", DependencyType.BLOCKS, "Design needed")
        ]

        for dep in dependencies:
            analyzer.add_dependency(dep)

        # Generate waves
        waves = analyzer.generate_execution_waves(max_agents=3)

        # Verify wave structure
        self.assertEqual(len(waves), 3)

        # First wave should have both research tasks
        self.assertEqual(len(waves[0].tasks), 2)
        self.assertIn("research1", waves[0].tasks)
        self.assertIn("research2", waves[0].tasks)

    def test_review_decision_integration(self):
        """Test integration between differential analysis and graduated review."""
        # Create differential reviewer
        reviewer = DifferentialReviewer()

        # Create review decision engine
        decision_engine = ReviewDecisionEngine()

        # Mock differential analysis result
        mock_analysis = {
            "file_changes": [
                {"path": "src/auth.py", "change_type": "modified"}
            ],
            "impact_assessment": {
                "authentication": "high_impact"
            }
        }

        # Create sample issues
        issues = [
            ReviewIssue(
                id="auth1",
                title="Authentication logic",
                description="Review authentication flow",
                severity=IssueSeverity.MEDIUM,
                category=IssueCategory.SECURITY,
                location="src/auth.py:50"
            )
        ]

        # Make decision
        decision = decision_engine.make_review_decision(issues, Mock())

        # Should be a nuanced decision, not just pass/fail
        self.assertIn(decision.verdict.value, [
            "pass", "pass_with_conditions", "conditional_pass",
            "soft_reject", "debt_accepted"
        ])


class PerformanceBenchmarks(unittest.TestCase):
    """Performance benchmarks for Atlas 2.1 components."""

    def test_dependency_analysis_performance(self):
        """Benchmark dependency analysis performance."""
        analyzer = DependencyGraphAnalyzer()

        # Create large task set
        num_tasks = 100
        tasks = [
            Task(f"task_{i}", f"Task {i}", TaskType.IMPLEMENTATION, 30)
            for i in range(num_tasks)
        ]

        start_time = datetime.datetime.now()

        for task in tasks:
            analyzer.add_task(task)

        # Add some dependencies
        for i in range(0, num_tasks - 1, 5):
            dep = Dependency(
                f"task_{i}", f"task_{i+1}",
                DependencyType.BLOCKS, "Sequential"
            )
            analyzer.add_dependency(dep)

        # Generate waves
        waves = analyzer.generate_execution_waves(max_agents=10)

        end_time = datetime.datetime.now()
        duration = (end_time - start_time).total_seconds()

        # Should complete within reasonable time
        self.assertLess(duration, 5.0, "Analysis should complete within 5 seconds")
        self.assertGreater(len(waves), 0, "Should generate execution waves")

    def test_trust_score_calculation_performance(self):
        """Benchmark trust score calculation performance."""
        calculator = TrustScoreCalculator(":memory:")

        # Create large number of review records
        num_records = 500
        records = [
            ReviewRecord(
                review_id=f"r{i}",
                developer_id="test_dev",
                timestamp=datetime.datetime.now() - datetime.timedelta(days=i),
                outcome=ReviewOutcome.PASS if i % 3 == 0 else ReviewOutcome.CONDITIONAL_PASS,
                cycles_count=1 if i % 4 == 0 else 2,
                quality_score=85.0 + (i % 20),
                issue_count=i % 5
            ) for i in range(num_records)
        ]

        # Save all records
        for record in records:
            calculator.db.save_review_record(record)

        start_time = datetime.datetime.now()

        # Calculate trust score
        trust_score = calculator.calculate_trust_score("test_dev")

        end_time = datetime.datetime.now()
        duration = (end_time - start_time).total_seconds()

        # Should calculate quickly even with many records
        self.assertLess(duration, 2.0, "Trust score calculation should be fast")
        self.assertIsNotNone(trust_score)

    def test_parallel_orchestration_performance(self):
        """Benchmark parallel orchestration performance."""
        orchestrator = ParallelOrchestrator(max_workers=5)
        orchestrator.load_default_agents()

        # Create sample configuration for testing
        with tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False) as f:
            config = {
                "tasks": [
                    {
                        "id": f"task_{i}",
                        "name": f"Task {i}",
                        "task_type": "implementation",
                        "estimated_duration": 1,  # Short duration for testing
                        "required_agents": 1,
                        "resources_needed": [],
                        "files_modified": []
                    } for i in range(20)
                ],
                "dependencies": []
            }
            json.dump(config, f)
            config_path = f.name

        try:
            start_time = datetime.datetime.now()

            # Load execution plan
            orchestrator.load_execution_plan(config_path)

            # Execute with mocked task execution
            with patch.object(orchestrator, '_simulate_task_execution') as mock_sim:
                mock_sim.return_value = {"status": "completed"}
                results = orchestrator.execute_parallel_plan()

            end_time = datetime.datetime.now()
            duration = (end_time - start_time).total_seconds()

            # Should orchestrate efficiently
            self.assertLess(duration, 10.0, "Parallel orchestration should be efficient")
            self.assertIsNotNone(results)

        finally:
            Path(config_path).unlink()  # Clean up


if __name__ == '__main__':
    # Run all tests
    unittest.main(verbosity=2)