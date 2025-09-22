#!/usr/bin/env python3
"""
Context Injection Performance Benchmarking Suite
Validates that the auto-context injection system meets performance requirements
"""

import os
import sys
import json
import time
import statistics
import asyncio
from pathlib import Path
from typing import Dict, List, Any, Tuple
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
import hashlib

class ContextInjectionBenchmark:
    """
    Performance benchmarking for the context injection system

    Target Requirements:
    - <500ms injection speed per agent
    - <1s for 10 parallel agents
    - 80% cache hit rate
    - <50ms context detection time
    """

    def __init__(self):
        self.atlas_dir = Path(__file__).parent.parent
        self.results_dir = self.atlas_dir / '.atlas' / 'benchmark_results'
        self.results_dir.mkdir(parents=True, exist_ok=True)

        # Import the components to benchmark
        from task_context_integration import TaskContextIntegration
        from enhanced_context_injector import EnhancedContextInjector

        manifest_path = self.atlas_dir / 'config' / 'context_manifest.json'
        config_path = self.atlas_dir / 'config' / 'agent_context_mapping.yaml'

        self.context_integration = TaskContextIntegration(
            manifest_path=str(manifest_path),
            config_path=str(config_path)
        )
        self.context_injector = EnhancedContextInjector(str(manifest_path))

        self.test_scenarios = self._generate_test_scenarios()

    def _generate_test_scenarios(self) -> List[Dict]:
        """Generate diverse test scenarios for benchmarking"""
        scenarios = []

        # Basic agent types
        agent_types = [
            'general-purpose', 'story-writer', 'bug-fixer',
            'ui-developer', 'backend-developer', 'tester',
            'reviewer', 'deployer', 'researcher', 'documenter'
        ]

        # Task descriptions with different complexities
        descriptions = {
            'simple': 'Fix a bug',
            'medium': 'Create user authentication with OAuth support',
            'complex': 'Implement real-time websocket communication with error handling and reconnection logic for mobile devices',
            'keyword_heavy': 'Debug and fix the critical performance issue causing slow database queries in the authentication api endpoint'
        }

        # Generate scenarios
        for agent_type in agent_types:
            for complexity, description in descriptions.items():
                scenarios.append({
                    'agent_type': agent_type,
                    'description': description,
                    'complexity': complexity,
                    'prompt': f"Task prompt for {description}",
                    'expected_profile': self._get_expected_profile(agent_type, description)
                })

        return scenarios

    def _get_expected_profile(self, agent_type: str, description: str) -> str:
        """Determine expected profile for validation"""
        # Direct mapping
        profile_map = {
            'story-writer': 'story_creation',
            'bug-fixer': 'troubleshooting',
            'ui-developer': 'ui_development',
            'backend-developer': 'backend_development',
            'tester': 'testing',
            'reviewer': 'code_review',
            'deployer': 'deployment',
            'researcher': 'research',
            'documenter': 'documentation'
        }

        if agent_type != 'general-purpose':
            return profile_map.get(agent_type, 'orchestration')

        # For general-purpose, check keywords
        desc_lower = description.lower()
        if any(kw in desc_lower for kw in ['bug', 'fix', 'debug', 'error']):
            return 'troubleshooting'
        elif any(kw in desc_lower for kw in ['story', 'requirement']):
            return 'story_creation'
        elif any(kw in desc_lower for kw in ['ui', 'frontend']):
            return 'ui_development'
        elif any(kw in desc_lower for kw in ['api', 'backend', 'database']):
            return 'backend_development'
        elif any(kw in desc_lower for kw in ['test', 'spec']):
            return 'testing'

        return 'orchestration'

    def benchmark_single_injection(self, scenario: Dict) -> Dict:
        """Benchmark a single context injection"""
        start_time = time.time()

        # Create task parameters
        task_params = {
            'description': scenario['description'],
            'prompt': scenario['prompt'],
            'subagent_type': scenario['agent_type']
        }

        # Measure injection
        try:
            result = self.context_integration.intercept_task_tool(task_params)
            injection_time = time.time() - start_time

            # Validate results
            success = '_context_metadata' in result
            if success:
                metadata = result['_context_metadata']
                profile_correct = metadata['profile'] == scenario['expected_profile']
                tokens = metadata['tokens']
            else:
                profile_correct = False
                tokens = 0

            return {
                'scenario': scenario,
                'injection_time': injection_time,
                'success': success,
                'profile_correct': profile_correct,
                'tokens': tokens,
                'metadata': result.get('_context_metadata', {})
            }

        except Exception as e:
            return {
                'scenario': scenario,
                'injection_time': time.time() - start_time,
                'success': False,
                'error': str(e)
            }

    def benchmark_sequential(self, num_iterations: int = 100) -> Dict:
        """Benchmark sequential context injections"""
        print(f"\n🏃 Running sequential benchmark ({num_iterations} iterations)...")

        results = []
        injection_times = []
        success_count = 0
        profile_accuracy = 0

        for i in range(num_iterations):
            scenario = self.test_scenarios[i % len(self.test_scenarios)]
            result = self.benchmark_single_injection(scenario)

            results.append(result)
            injection_times.append(result['injection_time'])

            if result['success']:
                success_count += 1
                if result.get('profile_correct'):
                    profile_accuracy += 1

        # Calculate statistics
        stats = {
            'total_iterations': num_iterations,
            'success_rate': success_count / num_iterations,
            'profile_accuracy': profile_accuracy / max(1, success_count),
            'injection_times': {
                'mean': statistics.mean(injection_times),
                'median': statistics.median(injection_times),
                'stdev': statistics.stdev(injection_times) if len(injection_times) > 1 else 0,
                'min': min(injection_times),
                'max': max(injection_times),
                'p95': sorted(injection_times)[int(len(injection_times) * 0.95)] if injection_times else 0,
                'p99': sorted(injection_times)[int(len(injection_times) * 0.99)] if injection_times else 0
            },
            'meets_500ms_target': statistics.mean(injection_times) < 0.5
        }

        return {
            'type': 'sequential',
            'stats': stats,
            'results': results[:10]  # Include first 10 for inspection
        }

    def benchmark_parallel(self, num_agents: int = 10, num_rounds: int = 10) -> Dict:
        """Benchmark parallel agent spawning"""
        print(f"\n🚀 Running parallel benchmark ({num_agents} agents, {num_rounds} rounds)...")

        round_times = []
        all_injection_times = []
        success_counts = []

        for round_num in range(num_rounds):
            start_time = time.time()

            # Create tasks for parallel execution
            with ThreadPoolExecutor(max_workers=num_agents) as executor:
                futures = []
                for i in range(num_agents):
                    scenario = self.test_scenarios[i % len(self.test_scenarios)]
                    future = executor.submit(self.benchmark_single_injection, scenario)
                    futures.append(future)

                # Collect results
                round_results = []
                round_success = 0
                for future in as_completed(futures):
                    result = future.result()
                    round_results.append(result)
                    all_injection_times.append(result['injection_time'])
                    if result['success']:
                        round_success += 1

                round_time = time.time() - start_time
                round_times.append(round_time)
                success_counts.append(round_success)

        # Calculate statistics
        stats = {
            'num_agents': num_agents,
            'num_rounds': num_rounds,
            'round_times': {
                'mean': statistics.mean(round_times),
                'median': statistics.median(round_times),
                'min': min(round_times),
                'max': max(round_times)
            },
            'individual_injection_times': {
                'mean': statistics.mean(all_injection_times),
                'median': statistics.median(all_injection_times),
                'max': max(all_injection_times)
            },
            'success_rate': sum(success_counts) / (num_agents * num_rounds),
            'meets_1s_target': statistics.mean(round_times) < 1.0
        }

        return {
            'type': 'parallel',
            'stats': stats
        }

    def benchmark_cache_performance(self, num_unique: int = 20, repeats_per_unique: int = 5) -> Dict:
        """Benchmark cache hit rates and performance"""
        print(f"\n💾 Running cache benchmark ({num_unique} unique, {repeats_per_unique} repeats each)...")

        unique_scenarios = self.test_scenarios[:num_unique]
        first_access_times = []
        cached_access_times = []
        cache_hits = 0
        total_cached_accesses = 0

        # Clear cache first
        if hasattr(self.context_injector, 'file_cache'):
            self.context_injector.file_cache.clear()

        for scenario in unique_scenarios:
            # First access (cache miss)
            result = self.benchmark_single_injection(scenario)
            first_access_times.append(result['injection_time'])

            # Repeated accesses (should hit cache)
            for _ in range(repeats_per_unique - 1):
                cache_result = self.benchmark_single_injection(scenario)
                cached_access_times.append(cache_result['injection_time'])
                total_cached_accesses += 1

                # Check if it was faster (indicates cache hit)
                if cache_result['injection_time'] < result['injection_time'] * 0.5:
                    cache_hits += 1

        # Calculate statistics
        cache_hit_rate = cache_hits / max(1, total_cached_accesses)
        speedup_factor = statistics.mean(first_access_times) / max(0.001, statistics.mean(cached_access_times))

        stats = {
            'num_unique_scenarios': num_unique,
            'repeats_per_unique': repeats_per_unique,
            'cache_hit_rate': cache_hit_rate,
            'speedup_factor': speedup_factor,
            'first_access_times': {
                'mean': statistics.mean(first_access_times),
                'median': statistics.median(first_access_times)
            },
            'cached_access_times': {
                'mean': statistics.mean(cached_access_times) if cached_access_times else 0,
                'median': statistics.median(cached_access_times) if cached_access_times else 0
            },
            'meets_80_percent_target': cache_hit_rate >= 0.8
        }

        return {
            'type': 'cache',
            'stats': stats
        }

    def benchmark_detection_accuracy(self) -> Dict:
        """Benchmark keyword detection accuracy"""
        print(f"\n🎯 Running detection accuracy benchmark...")

        test_cases = [
            # (description, agent_type, expected_profile)
            ('Fix the login crash bug', 'general-purpose', 'troubleshooting'),
            ('Create user story for dashboard', 'general-purpose', 'story_creation'),
            ('Build React component for charts', 'general-purpose', 'ui_development'),
            ('Implement REST API endpoints', 'general-purpose', 'backend_development'),
            ('Write unit tests for auth', 'general-purpose', 'testing'),
            ('Review pull request changes', 'general-purpose', 'code_review'),
            ('Research performance bottlenecks', 'general-purpose', 'research'),
            ('Deploy to production environment', 'general-purpose', 'deployment'),
            ('Update API documentation', 'general-purpose', 'documentation'),
            ('Coordinate feature implementation', 'general-purpose', 'orchestration')
        ]

        correct_detections = 0
        detection_times = []

        for description, agent_type, expected_profile in test_cases:
            start_time = time.time()

            # Test detection
            profile, confidence, method = self.context_integration._determine_context_profile(
                agent_type,
                description,
                ''
            )

            detection_time = time.time() - start_time
            detection_times.append(detection_time)

            if profile == expected_profile:
                correct_detections += 1

        accuracy = correct_detections / len(test_cases)

        stats = {
            'total_test_cases': len(test_cases),
            'correct_detections': correct_detections,
            'accuracy': accuracy,
            'detection_times': {
                'mean': statistics.mean(detection_times),
                'max': max(detection_times),
                'meets_50ms_target': statistics.mean(detection_times) < 0.05
            },
            'meets_95_percent_target': accuracy >= 0.95
        }

        return {
            'type': 'detection_accuracy',
            'stats': stats
        }

    def run_full_benchmark(self) -> Dict:
        """Run complete benchmark suite"""
        print("\n" + "="*60)
        print("🔬 Atlas Context Injection Performance Benchmark")
        print("="*60)

        results = {
            'timestamp': datetime.now().isoformat(),
            'benchmarks': {}
        }

        # Run all benchmarks
        results['benchmarks']['sequential'] = self.benchmark_sequential(100)
        results['benchmarks']['parallel'] = self.benchmark_parallel(10, 10)
        results['benchmarks']['cache'] = self.benchmark_cache_performance(20, 5)
        results['benchmarks']['detection'] = self.benchmark_detection_accuracy()

        # Generate summary
        results['summary'] = self._generate_summary(results['benchmarks'])

        # Save results
        result_file = self.results_dir / f"benchmark_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(result_file, 'w') as f:
            json.dump(results, f, indent=2, default=str)

        print(f"\n💾 Results saved to: {result_file}")

        return results

    def _generate_summary(self, benchmarks: Dict) -> Dict:
        """Generate summary of benchmark results"""
        summary = {
            'all_targets_met': True,
            'targets': {}
        }

        # Check sequential target (<500ms)
        seq_meets_target = benchmarks['sequential']['stats']['meets_500ms_target']
        summary['targets']['sequential_injection_<500ms'] = seq_meets_target
        if not seq_meets_target:
            summary['all_targets_met'] = False

        # Check parallel target (<1s for 10 agents)
        parallel_meets_target = benchmarks['parallel']['stats']['meets_1s_target']
        summary['targets']['parallel_10_agents_<1s'] = parallel_meets_target
        if not parallel_meets_target:
            summary['all_targets_met'] = False

        # Check cache target (80% hit rate)
        cache_meets_target = benchmarks['cache']['stats']['meets_80_percent_target']
        summary['targets']['cache_hit_rate_>80%'] = cache_meets_target
        if not cache_meets_target:
            summary['all_targets_met'] = False

        # Check detection accuracy target (95%)
        detection_meets_target = benchmarks['detection']['stats']['meets_95_percent_target']
        summary['targets']['detection_accuracy_>95%'] = detection_meets_target
        if not detection_meets_target:
            summary['all_targets_met'] = False

        # Check detection speed target (<50ms)
        detection_speed_target = benchmarks['detection']['stats']['detection_times']['meets_50ms_target']
        summary['targets']['detection_speed_<50ms'] = detection_speed_target
        if not detection_speed_target:
            summary['all_targets_met'] = False

        # Overall metrics
        summary['metrics'] = {
            'avg_injection_time': benchmarks['sequential']['stats']['injection_times']['mean'],
            'p95_injection_time': benchmarks['sequential']['stats']['injection_times']['p95'],
            'parallel_completion_time': benchmarks['parallel']['stats']['round_times']['mean'],
            'cache_hit_rate': benchmarks['cache']['stats']['cache_hit_rate'],
            'cache_speedup': benchmarks['cache']['stats']['speedup_factor'],
            'detection_accuracy': benchmarks['detection']['stats']['accuracy']
        }

        return summary

    def print_results(self, results: Dict):
        """Print formatted benchmark results"""
        print("\n" + "="*60)
        print("📊 Benchmark Results Summary")
        print("="*60)

        summary = results['summary']

        # Overall status
        if summary['all_targets_met']:
            print("\n✅ ALL PERFORMANCE TARGETS MET!")
        else:
            print("\n⚠️ Some performance targets not met:")

        # Target status
        print("\nPerformance Targets:")
        for target, met in summary['targets'].items():
            status = "✅" if met else "❌"
            print(f"  {status} {target.replace('_', ' ')}")

        # Key metrics
        print("\nKey Metrics:")
        metrics = summary['metrics']
        print(f"  • Average injection time: {metrics['avg_injection_time']*1000:.1f}ms")
        print(f"  • P95 injection time: {metrics['p95_injection_time']*1000:.1f}ms")
        print(f"  • Parallel completion (10 agents): {metrics['parallel_completion_time']:.2f}s")
        print(f"  • Cache hit rate: {metrics['cache_hit_rate']:.1%}")
        print(f"  • Cache speedup: {metrics['cache_speedup']:.1f}x")
        print(f"  • Detection accuracy: {metrics['detection_accuracy']:.1%}")

        print("\n" + "="*60)


def main():
    """CLI interface for benchmarking"""
    import argparse

    parser = argparse.ArgumentParser(description='Context Injection Performance Benchmark')
    parser.add_argument('--quick', action='store_true', help='Run quick benchmark (reduced iterations)')
    parser.add_argument('--sequential-only', action='store_true', help='Run only sequential benchmark')
    parser.add_argument('--parallel-only', action='store_true', help='Run only parallel benchmark')
    parser.add_argument('--cache-only', action='store_true', help='Run only cache benchmark')
    parser.add_argument('--detection-only', action='store_true', help='Run only detection benchmark')

    args = parser.parse_args()

    benchmark = ContextInjectionBenchmark()

    if args.sequential_only:
        results = {'benchmarks': {'sequential': benchmark.benchmark_sequential(20 if args.quick else 100)}}
    elif args.parallel_only:
        results = {'benchmarks': {'parallel': benchmark.benchmark_parallel(5 if args.quick else 10, 3 if args.quick else 10)}}
    elif args.cache_only:
        results = {'benchmarks': {'cache': benchmark.benchmark_cache_performance(5 if args.quick else 20, 3 if args.quick else 5)}}
    elif args.detection_only:
        results = {'benchmarks': {'detection': benchmark.benchmark_detection_accuracy()}}
    else:
        # Run full benchmark
        if args.quick:
            print("Running quick benchmark (reduced iterations)...")
            # Override with smaller numbers for quick test
            results = {
                'timestamp': datetime.now().isoformat(),
                'benchmarks': {
                    'sequential': benchmark.benchmark_sequential(20),
                    'parallel': benchmark.benchmark_parallel(5, 3),
                    'cache': benchmark.benchmark_cache_performance(5, 3),
                    'detection': benchmark.benchmark_detection_accuracy()
                }
            }
            results['summary'] = benchmark._generate_summary(results['benchmarks'])
        else:
            results = benchmark.run_full_benchmark()

    benchmark.print_results(results)


if __name__ == '__main__':
    main()