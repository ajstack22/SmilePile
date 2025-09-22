#!/usr/bin/env python3
"""
Atlas Dependency Graph Generator v2.1
Copyright 2024 Atlas Framework

Generates and analyzes dependency graphs for parallel execution planning.
Identifies parallelization opportunities and calculates optimal execution waves.
"""

import json
import logging
import datetime
from typing import Dict, List, Set, Optional, Tuple, Any
from dataclasses import dataclass, asdict
from pathlib import Path
import networkx as nx
from enum import Enum

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class DependencyType(Enum):
    """Types of dependencies between tasks."""
    BLOCKS = "blocks"           # Hard blocking dependency
    REQUIRES = "requires"       # Soft dependency (can be parallel with coordination)
    CONFLICTS = "conflicts"     # Cannot run simultaneously
    FOLLOWS = "follows"         # Sequential ordering preference
    USES = "uses"              # Shares resources but can be coordinated


class TaskType(Enum):
    """Types of tasks for parallel execution."""
    RESEARCH = "research"
    ANALYSIS = "analysis"
    DESIGN = "design"
    IMPLEMENTATION = "implementation"
    TESTING = "testing"
    REVIEW = "review"
    DEPLOYMENT = "deployment"
    DOCUMENTATION = "documentation"


@dataclass
class Task:
    """Represents a task in the dependency graph."""
    id: str
    name: str
    task_type: TaskType
    estimated_duration: int  # minutes
    required_agents: int = 1
    resources_needed: List[str] = None
    files_modified: List[str] = None
    files_read: List[str] = None

    def __post_init__(self):
        if self.resources_needed is None:
            self.resources_needed = []
        if self.files_modified is None:
            self.files_modified = []
        if self.files_read is None:
            self.files_read = []


@dataclass
class Dependency:
    """Represents a dependency between two tasks."""
    from_task: str
    to_task: str
    dependency_type: DependencyType
    reason: str
    weight: float = 1.0  # Strength of dependency (0.1 = weak, 1.0 = strong)


@dataclass
class ExecutionWave:
    """Represents a wave of parallel execution."""
    wave_number: int
    tasks: List[str]
    estimated_duration: int
    required_agents: int
    resources_used: Set[str]
    conflicts_resolved: List[str]


class DependencyGraphAnalyzer:
    """Analyzes task dependencies and generates optimal execution plans."""

    def __init__(self):
        self.graph = nx.DiGraph()
        self.tasks: Dict[str, Task] = {}
        self.dependencies: List[Dependency] = {}

    def add_task(self, task: Task) -> None:
        """Add a task to the dependency graph."""
        self.tasks[task.id] = task
        self.graph.add_node(task.id, **asdict(task))
        logger.info(f"Added task: {task.id} ({task.name})")

    def add_dependency(self, dependency: Dependency) -> None:
        """Add a dependency between tasks."""
        if dependency.from_task not in self.tasks:
            raise ValueError(f"Task {dependency.from_task} not found")
        if dependency.to_task not in self.tasks:
            raise ValueError(f"Task {dependency.to_task} not found")

        self.dependencies.append(dependency)

        # Only add graph edge for blocking dependencies
        if dependency.dependency_type in [DependencyType.BLOCKS, DependencyType.REQUIRES]:
            self.graph.add_edge(
                dependency.from_task,
                dependency.to_task,
                **asdict(dependency)
            )

        logger.info(f"Added dependency: {dependency.from_task} -> {dependency.to_task} ({dependency.dependency_type.value})")

    def detect_circular_dependencies(self) -> List[List[str]]:
        """Detect circular dependencies in the graph."""
        try:
            cycles = list(nx.simple_cycles(self.graph))
            if cycles:
                logger.warning(f"Found {len(cycles)} circular dependencies")
                for i, cycle in enumerate(cycles):
                    logger.warning(f"Cycle {i+1}: {' -> '.join(cycle + [cycle[0]])}")
            return cycles
        except nx.NetworkXError as e:
            logger.error(f"Error detecting cycles: {e}")
            return []

    def calculate_critical_path(self) -> Tuple[List[str], int]:
        """Calculate the critical path through the dependency graph."""
        if not self.tasks:
            return [], 0

        # Add duration as node weight
        for task_id, task in self.tasks.items():
            self.graph.nodes[task_id]['duration'] = task.estimated_duration

        try:
            # Find longest path (critical path)
            longest_path = nx.dag_longest_path(self.graph, weight='duration')
            total_duration = nx.dag_longest_path_length(self.graph, weight='duration')

            logger.info(f"Critical path: {' -> '.join(longest_path)}")
            logger.info(f"Critical path duration: {total_duration} minutes")

            return longest_path, total_duration
        except nx.NetworkXError as e:
            logger.error(f"Error calculating critical path: {e}")
            return [], 0

    def identify_parallelization_opportunities(self) -> Dict[str, List[str]]:
        """Identify tasks that can run in parallel."""
        opportunities = {}

        # Get topological ordering
        try:
            topo_order = list(nx.topological_sort(self.graph))
        except nx.NetworkXError:
            logger.error("Cannot create topological order - circular dependencies exist")
            return opportunities

        # Group tasks by level (distance from start)
        levels = {}
        for task_id in topo_order:
            # Calculate the maximum distance from any root
            if not list(self.graph.predecessors(task_id)):
                levels[task_id] = 0
            else:
                max_level = max(levels.get(pred, 0) for pred in self.graph.predecessors(task_id))
                levels[task_id] = max_level + 1

        # Group by level for parallel execution
        level_groups = {}
        for task_id, level in levels.items():
            if level not in level_groups:
                level_groups[level] = []
            level_groups[level].append(task_id)

        # Check for conflicts within each level
        for level, task_group in level_groups.items():
            if len(task_group) > 1:
                # Filter out conflicting tasks
                parallel_tasks = self._filter_conflicts(task_group)
                if len(parallel_tasks) > 1:
                    opportunities[f"level_{level}"] = parallel_tasks

        return opportunities

    def _filter_conflicts(self, tasks: List[str]) -> List[str]:
        """Remove conflicting tasks from a parallel group."""
        conflicts = []

        # Check for file conflicts
        file_writers = {}
        for task_id in tasks:
            task = self.tasks[task_id]
            for file_path in task.files_modified:
                if file_path in file_writers:
                    conflicts.append((task_id, file_writers[file_path]))
                else:
                    file_writers[file_path] = task_id

        # Check for resource conflicts
        resource_users = {}
        for task_id in tasks:
            task = self.tasks[task_id]
            for resource in task.resources_needed:
                if resource in resource_users:
                    conflicts.append((task_id, resource_users[resource]))
                else:
                    resource_users[resource] = task_id

        # Check explicit conflicts from dependencies
        for dep in self.dependencies:
            if (dep.dependency_type == DependencyType.CONFLICTS and
                dep.from_task in tasks and dep.to_task in tasks):
                conflicts.append((dep.from_task, dep.to_task))

        # Remove conflicting tasks (keep the one with higher priority/earlier in topo order)
        to_remove = set()
        for task1, task2 in conflicts:
            # Simple heuristic: remove the task with longer duration (more complex)
            if self.tasks[task1].estimated_duration > self.tasks[task2].estimated_duration:
                to_remove.add(task1)
            else:
                to_remove.add(task2)

        return [task for task in tasks if task not in to_remove]

    def generate_execution_waves(self, max_agents: int = 5) -> List[ExecutionWave]:
        """Generate optimal execution waves for parallel processing."""
        waves = []
        remaining_tasks = set(self.tasks.keys())
        wave_number = 0

        while remaining_tasks:
            wave_number += 1

            # Find tasks that can start (no unfinished dependencies)
            ready_tasks = []
            for task_id in remaining_tasks:
                dependencies_met = True
                for pred in self.graph.predecessors(task_id):
                    if pred in remaining_tasks:
                        dependencies_met = False
                        break
                if dependencies_met:
                    ready_tasks.append(task_id)

            if not ready_tasks:
                logger.error(f"No ready tasks found with {len(remaining_tasks)} remaining - possible circular dependency")
                break

            # Filter conflicts and optimize for agent count
            parallel_tasks = self._filter_conflicts(ready_tasks)
            wave_tasks = self._optimize_wave_for_agents(parallel_tasks, max_agents)

            # Calculate wave metrics
            total_duration = max(self.tasks[task_id].estimated_duration for task_id in wave_tasks)
            total_agents = sum(self.tasks[task_id].required_agents for task_id in wave_tasks)
            used_resources = set()
            for task_id in wave_tasks:
                used_resources.update(self.tasks[task_id].resources_needed)

            wave = ExecutionWave(
                wave_number=wave_number,
                tasks=wave_tasks,
                estimated_duration=total_duration,
                required_agents=min(total_agents, max_agents),
                resources_used=used_resources,
                conflicts_resolved=[]
            )

            waves.append(wave)
            remaining_tasks -= set(wave_tasks)

            logger.info(f"Wave {wave_number}: {len(wave_tasks)} tasks, {total_duration}min, {total_agents} agents")

        return waves

    def _optimize_wave_for_agents(self, tasks: List[str], max_agents: int) -> List[str]:
        """Optimize task selection for available agent count."""
        if not tasks:
            return []

        # Sort by priority (shorter tasks first for better parallelization)
        sorted_tasks = sorted(tasks, key=lambda t: (
            self.tasks[t].estimated_duration,
            self.tasks[t].required_agents
        ))

        selected_tasks = []
        used_agents = 0

        for task_id in sorted_tasks:
            task = self.tasks[task_id]
            if used_agents + task.required_agents <= max_agents:
                selected_tasks.append(task_id)
                used_agents += task.required_agents
            elif not selected_tasks:  # Always include at least one task
                selected_tasks.append(task_id)
                break

        return selected_tasks

    def calculate_speedup_potential(self, max_agents: int = 5) -> Dict[str, float]:
        """Calculate potential speedup from parallelization."""
        # Sequential execution time
        sequential_time = sum(task.estimated_duration for task in self.tasks.values())

        # Parallel execution time with waves
        waves = self.generate_execution_waves(max_agents)
        parallel_time = sum(wave.estimated_duration for wave in waves)

        # Critical path time (theoretical minimum)
        _, critical_path_time = self.calculate_critical_path()

        return {
            "sequential_time": sequential_time,
            "parallel_time": parallel_time,
            "critical_path_time": critical_path_time,
            "speedup_factor": sequential_time / parallel_time if parallel_time > 0 else 1.0,
            "efficiency": (sequential_time / parallel_time) / max_agents if parallel_time > 0 else 0.0,
            "theoretical_max_speedup": sequential_time / critical_path_time if critical_path_time > 0 else 1.0
        }

    def export_graphviz(self, output_path: str) -> None:
        """Export dependency graph as GraphViz DOT format."""
        try:
            # Create a new graph with better layout attributes
            viz_graph = nx.DiGraph()

            # Add nodes with attributes
            for task_id, task in self.tasks.items():
                label = f"{task.name}\\n({task.estimated_duration}min)"
                color = self._get_task_color(task.task_type)
                viz_graph.add_node(task_id,
                                 label=label,
                                 shape="box",
                                 style="filled",
                                 fillcolor=color)

            # Add edges
            for dep in self.dependencies:
                if dep.dependency_type in [DependencyType.BLOCKS, DependencyType.REQUIRES]:
                    style = "solid" if dep.dependency_type == DependencyType.BLOCKS else "dashed"
                    viz_graph.add_edge(dep.from_task, dep.to_task,
                                     style=style,
                                     label=dep.dependency_type.value)

            # Export
            from networkx.drawing.nx_agraph import write_dot
            write_dot(viz_graph, output_path)
            logger.info(f"Exported dependency graph to {output_path}")

        except ImportError:
            logger.error("graphviz not available - install with: pip install pygraphviz")
        except Exception as e:
            logger.error(f"Error exporting graph: {e}")

    def _get_task_color(self, task_type: TaskType) -> str:
        """Get color for task type in visualization."""
        colors = {
            TaskType.RESEARCH: "lightblue",
            TaskType.ANALYSIS: "lightgreen",
            TaskType.DESIGN: "lightyellow",
            TaskType.IMPLEMENTATION: "lightcoral",
            TaskType.TESTING: "lightpink",
            TaskType.REVIEW: "lightgray",
            TaskType.DEPLOYMENT: "lightcyan",
            TaskType.DOCUMENTATION: "wheat"
        }
        return colors.get(task_type, "white")

    def export_mermaid(self, output_path: str) -> None:
        """Export dependency graph as Mermaid diagram."""
        lines = ["graph TD"]

        # Add nodes
        for task_id, task in self.tasks.items():
            safe_id = task_id.replace("-", "_").replace(" ", "_")
            lines.append(f"    {safe_id}[\"{task.name}\\n{task.estimated_duration}min\"]")

        # Add edges
        for dep in self.dependencies:
            if dep.dependency_type in [DependencyType.BLOCKS, DependencyType.REQUIRES]:
                from_id = dep.from_task.replace("-", "_").replace(" ", "_")
                to_id = dep.to_task.replace("-", "_").replace(" ", "_")
                arrow = "-->" if dep.dependency_type == DependencyType.BLOCKS else "-..->"
                lines.append(f"    {from_id} {arrow} {to_id}")

        # Add styling
        lines.extend([
            "",
            "classDef research fill:#e1f5fe",
            "classDef implementation fill:#fce4ec",
            "classDef testing fill:#f3e5f5",
            "classDef review fill:#e8f5e8"
        ])

        with open(output_path, 'w') as f:
            f.write('\n'.join(lines))

        logger.info(f"Exported Mermaid diagram to {output_path}")

    def save_analysis_report(self, output_path: str, max_agents: int = 5) -> None:
        """Save comprehensive analysis report."""
        report = {
            "generated_at": datetime.datetime.now().isoformat(),
            "tasks_count": len(self.tasks),
            "dependencies_count": len(self.dependencies),
            "circular_dependencies": self.detect_circular_dependencies(),
            "critical_path": {
                "path": self.calculate_critical_path()[0],
                "duration": self.calculate_critical_path()[1]
            },
            "parallelization_opportunities": self.identify_parallelization_opportunities(),
            "execution_waves": [asdict(wave) for wave in self.generate_execution_waves(max_agents)],
            "speedup_analysis": self.calculate_speedup_potential(max_agents),
            "tasks": {task_id: asdict(task) for task_id, task in self.tasks.items()},
            "dependencies": [asdict(dep) for dep in self.dependencies]
        }

        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2, default=str)

        logger.info(f"Saved analysis report to {output_path}")


def load_tasks_from_config(config_path: str) -> DependencyGraphAnalyzer:
    """Load tasks and dependencies from configuration file."""
    analyzer = DependencyGraphAnalyzer()

    try:
        with open(config_path, 'r') as f:
            config = json.load(f)

        # Load tasks
        for task_data in config.get('tasks', []):
            task = Task(
                id=task_data['id'],
                name=task_data['name'],
                task_type=TaskType(task_data['task_type']),
                estimated_duration=task_data['estimated_duration'],
                required_agents=task_data.get('required_agents', 1),
                resources_needed=task_data.get('resources_needed', []),
                files_modified=task_data.get('files_modified', []),
                files_read=task_data.get('files_read', [])
            )
            analyzer.add_task(task)

        # Load dependencies
        for dep_data in config.get('dependencies', []):
            dependency = Dependency(
                from_task=dep_data['from_task'],
                to_task=dep_data['to_task'],
                dependency_type=DependencyType(dep_data['dependency_type']),
                reason=dep_data['reason'],
                weight=dep_data.get('weight', 1.0)
            )
            analyzer.add_dependency(dependency)

        logger.info(f"Loaded {len(analyzer.tasks)} tasks and {len(analyzer.dependencies)} dependencies")
        return analyzer

    except Exception as e:
        logger.error(f"Error loading configuration: {e}")
        raise


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Atlas Dependency Graph Analyzer")
    parser.add_argument("config", help="Path to task configuration JSON file")
    parser.add_argument("--max-agents", type=int, default=5, help="Maximum number of parallel agents")
    parser.add_argument("--output-dir", default="./output", help="Output directory for reports")
    parser.add_argument("--format", choices=["json", "dot", "mermaid", "all"], default="all",
                      help="Output format")

    args = parser.parse_args()

    # Create output directory
    output_dir = Path(args.output_dir)
    output_dir.mkdir(exist_ok=True)

    # Load and analyze
    analyzer = load_tasks_from_config(args.config)

    # Check for issues
    cycles = analyzer.detect_circular_dependencies()
    if cycles:
        print(f"WARNING: Found {len(cycles)} circular dependencies!")
        for cycle in cycles:
            print(f"  Cycle: {' -> '.join(cycle)}")

    # Generate outputs
    if args.format in ["json", "all"]:
        analyzer.save_analysis_report(output_dir / "dependency_analysis.json", args.max_agents)

    if args.format in ["dot", "all"]:
        analyzer.export_graphviz(str(output_dir / "dependency_graph.dot"))

    if args.format in ["mermaid", "all"]:
        analyzer.export_mermaid(str(output_dir / "dependency_graph.mmd"))

    # Print summary
    speedup = analyzer.calculate_speedup_potential(args.max_agents)
    print(f"\n=== Dependency Analysis Summary ===")
    print(f"Tasks: {len(analyzer.tasks)}")
    print(f"Dependencies: {len(analyzer.dependencies)}")
    print(f"Sequential time: {speedup['sequential_time']} minutes")
    print(f"Parallel time: {speedup['parallel_time']} minutes")
    print(f"Speedup factor: {speedup['speedup_factor']:.2f}x")
    print(f"Efficiency: {speedup['efficiency']:.1%}")
    print(f"Theoretical max speedup: {speedup['theoretical_max_speedup']:.2f}x")