#!/usr/bin/env python3
"""
Production Monitoring and Hardening for Context Injection System
Implements circuit breakers, monitoring, alerting, and graceful degradation
"""

import json
import time
import logging
import threading
from pathlib import Path
from typing import Dict, Any, Optional, List, Callable
from datetime import datetime, timedelta
from collections import deque, defaultdict
from enum import Enum

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class CircuitState(Enum):
    """Circuit breaker states"""
    CLOSED = "closed"  # Normal operation
    OPEN = "open"      # Failing, reject calls
    HALF_OPEN = "half_open"  # Testing recovery


class CircuitBreaker:
    """
    Circuit breaker pattern for fault tolerance
    Prevents cascading failures by failing fast
    """

    def __init__(
        self,
        name: str,
        failure_threshold: int = 5,
        recovery_timeout: int = 60,
        expected_exception: type = Exception
    ):
        self.name = name
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.expected_exception = expected_exception

        self.failure_count = 0
        self.last_failure_time = None
        self.state = CircuitState.CLOSED
        self._lock = threading.Lock()

    def call(self, func: Callable, *args, **kwargs) -> Any:
        """Execute function with circuit breaker protection"""
        with self._lock:
            if self.state == CircuitState.OPEN:
                if self._should_attempt_reset():
                    self.state = CircuitState.HALF_OPEN
                else:
                    raise Exception(f"Circuit breaker {self.name} is OPEN")

        try:
            result = func(*args, **kwargs)
            self._on_success()
            return result
        except self.expected_exception as e:
            self._on_failure()
            raise e

    def _on_success(self):
        """Handle successful call"""
        with self._lock:
            self.failure_count = 0
            if self.state == CircuitState.HALF_OPEN:
                self.state = CircuitState.CLOSED
                logger.info(f"Circuit breaker {self.name} recovered to CLOSED")

    def _on_failure(self):
        """Handle failed call"""
        with self._lock:
            self.failure_count += 1
            self.last_failure_time = time.time()

            if self.failure_count >= self.failure_threshold:
                self.state = CircuitState.OPEN
                logger.warning(f"Circuit breaker {self.name} opened after {self.failure_count} failures")

    def _should_attempt_reset(self) -> bool:
        """Check if enough time has passed to try recovery"""
        return (
            self.last_failure_time and
            time.time() - self.last_failure_time >= self.recovery_timeout
        )

    def get_state(self) -> Dict:
        """Get current circuit breaker state"""
        return {
            'name': self.name,
            'state': self.state.value,
            'failure_count': self.failure_count,
            'last_failure': self.last_failure_time
        }


class MetricsCollector:
    """
    Collects and aggregates metrics for monitoring
    """

    def __init__(self, window_size: int = 300):  # 5-minute window
        self.window_size = window_size
        self.metrics = defaultdict(lambda: deque(maxlen=1000))
        self.counters = defaultdict(int)
        self.alerts = deque(maxlen=100)
        self._lock = threading.Lock()

    def record_metric(self, name: str, value: float, tags: Optional[Dict] = None):
        """Record a metric value"""
        with self._lock:
            timestamp = time.time()
            self.metrics[name].append({
                'timestamp': timestamp,
                'value': value,
                'tags': tags or {}
            })

    def increment_counter(self, name: str, value: int = 1):
        """Increment a counter metric"""
        with self._lock:
            self.counters[name] += value

    def get_metric_stats(self, name: str) -> Dict:
        """Get statistics for a metric over the window"""
        with self._lock:
            current_time = time.time()
            window_start = current_time - self.window_size

            # Filter to window
            values = [
                m['value'] for m in self.metrics[name]
                if m['timestamp'] >= window_start
            ]

            if not values:
                return {'count': 0}

            return {
                'count': len(values),
                'mean': sum(values) / len(values),
                'min': min(values),
                'max': max(values),
                'sum': sum(values)
            }

    def get_counter(self, name: str) -> int:
        """Get counter value"""
        with self._lock:
            return self.counters[name]

    def check_alert_conditions(self) -> List[Dict]:
        """Check for alert conditions and return triggered alerts"""
        alerts = []

        # Check injection failure rate
        total_injections = self.get_counter('injections.total')
        failed_injections = self.get_counter('injections.failed')

        if total_injections > 100:  # Minimum sample size
            failure_rate = failed_injections / total_injections
            if failure_rate > 0.1:  # 10% threshold
                alerts.append({
                    'severity': 'high',
                    'metric': 'injection_failure_rate',
                    'value': failure_rate,
                    'threshold': 0.1,
                    'message': f"Injection failure rate {failure_rate:.1%} exceeds 10%"
                })

        # Check injection latency
        latency_stats = self.get_metric_stats('injection.latency')
        if latency_stats.get('count', 0) > 10:
            p95_latency = self._calculate_percentile('injection.latency', 0.95)
            if p95_latency > 1.0:  # 1 second threshold
                alerts.append({
                    'severity': 'medium',
                    'metric': 'injection_latency_p95',
                    'value': p95_latency,
                    'threshold': 1.0,
                    'message': f"P95 injection latency {p95_latency:.2f}s exceeds 1s"
                })

        # Check cache hit rate
        cache_hits = self.get_counter('cache.hits')
        cache_misses = self.get_counter('cache.misses')
        total_cache = cache_hits + cache_misses

        if total_cache > 50:  # Minimum sample size
            cache_hit_rate = cache_hits / total_cache
            if cache_hit_rate < 0.5:  # 50% threshold
                alerts.append({
                    'severity': 'low',
                    'metric': 'cache_hit_rate',
                    'value': cache_hit_rate,
                    'threshold': 0.5,
                    'message': f"Cache hit rate {cache_hit_rate:.1%} below 50%"
                })

        # Record alerts
        for alert in alerts:
            alert['timestamp'] = datetime.now().isoformat()
            self.alerts.append(alert)
            logger.warning(f"Alert triggered: {alert['message']}")

        return alerts

    def _calculate_percentile(self, metric: str, percentile: float) -> float:
        """Calculate percentile for a metric"""
        with self._lock:
            current_time = time.time()
            window_start = current_time - self.window_size

            values = sorted([
                m['value'] for m in self.metrics[metric]
                if m['timestamp'] >= window_start
            ])

            if not values:
                return 0

            index = int(len(values) * percentile)
            return values[min(index, len(values) - 1)]

    def export_metrics(self) -> Dict:
        """Export all metrics for monitoring dashboard"""
        return {
            'timestamp': datetime.now().isoformat(),
            'counters': dict(self.counters),
            'metrics': {
                name: self.get_metric_stats(name)
                for name in self.metrics.keys()
            },
            'recent_alerts': list(self.alerts)
        }


class ProductionHardenedIntegration:
    """
    Production-hardened version of context injection with monitoring
    """

    def __init__(self):
        self.atlas_dir = Path(__file__).parent.parent

        # Initialize components with circuit breakers
        self.context_breaker = CircuitBreaker(
            "context_injection",
            failure_threshold=5,
            recovery_timeout=60
        )

        self.detection_breaker = CircuitBreaker(
            "profile_detection",
            failure_threshold=10,
            recovery_timeout=30
        )

        # Initialize metrics
        self.metrics = MetricsCollector()

        # Health check state
        self.healthy = True
        self.last_health_check = time.time()

        # Import base integration
        from task_context_integration import TaskContextIntegration
        self.base_integration = TaskContextIntegration()

        # Start monitoring thread
        self._start_monitoring()

    def intercept_with_monitoring(self, task_params: Dict) -> Dict:
        """
        Intercept with full monitoring and circuit breaking
        """
        start_time = time.time()
        self.metrics.increment_counter('injections.total')

        try:
            # Check circuit breaker
            if self.context_breaker.state == CircuitState.OPEN:
                logger.warning("Circuit breaker open, using fallback")
                self.metrics.increment_counter('circuit_breaker.open')
                return self._fallback_response(task_params)

            # Try injection with circuit breaker
            result = self.context_breaker.call(
                self._monitored_injection,
                task_params
            )

            # Record success metrics
            injection_time = time.time() - start_time
            self.metrics.record_metric('injection.latency', injection_time)
            self.metrics.increment_counter('injections.success')

            # Check if cache was hit
            if result.get('_context_metadata', {}).get('cache_hit'):
                self.metrics.increment_counter('cache.hits')
            else:
                self.metrics.increment_counter('cache.misses')

            return result

        except Exception as e:
            # Record failure metrics
            self.metrics.increment_counter('injections.failed')
            self.metrics.record_metric(
                'injection.error',
                1,
                {'error': str(e)[:100]}
            )

            logger.error(f"Injection failed with monitoring: {e}")
            return self._fallback_response(task_params)

    def _monitored_injection(self, task_params: Dict) -> Dict:
        """Perform injection with monitoring"""
        # Add timeout protection
        import signal

        def timeout_handler(signum, frame):
            raise TimeoutError("Injection timeout")

        # Set 2-second timeout
        if hasattr(signal, 'SIGALRM'):
            signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(2)

        try:
            result = self.base_integration.intercept_task_tool(task_params)

            # Cancel timeout
            if hasattr(signal, 'SIGALRM'):
                signal.alarm(0)

            return result

        except TimeoutError:
            self.metrics.increment_counter('injection.timeout')
            raise

    def _fallback_response(self, task_params: Dict) -> Dict:
        """Generate fallback response when injection fails"""
        logger.info("Using fallback response")

        # Add minimal context marker
        fallback_prompt = f"""[CONTEXT INJECTION UNAVAILABLE - FALLBACK MODE]

Note: Automatic context injection is temporarily unavailable.
Please proceed with the task using general knowledge.

---
{task_params.get('prompt', '')}"""

        return {
            **task_params,
            'prompt': fallback_prompt,
            '_context_metadata': {
                'fallback': True,
                'reason': 'circuit_open' if self.context_breaker.state == CircuitState.OPEN else 'error'
            }
        }

    def _start_monitoring(self):
        """Start background monitoring thread"""
        def monitor():
            while True:
                time.sleep(60)  # Check every minute
                self._perform_health_check()
                alerts = self.metrics.check_alert_conditions()

                if alerts:
                    self._handle_alerts(alerts)

        monitor_thread = threading.Thread(target=monitor, daemon=True)
        monitor_thread.start()

    def _perform_health_check(self):
        """Perform system health check"""
        try:
            # Test basic functionality
            test_params = {
                'description': 'Health check',
                'prompt': 'Test',
                'subagent_type': 'general-purpose'
            }

            result = self.base_integration.intercept_task_tool(test_params)

            if '_context_metadata' in result:
                self.healthy = True
                self.last_health_check = time.time()
            else:
                self.healthy = False

        except Exception as e:
            logger.error(f"Health check failed: {e}")
            self.healthy = False

    def _handle_alerts(self, alerts: List[Dict]):
        """Handle triggered alerts"""
        for alert in alerts:
            if alert['severity'] == 'high':
                logger.error(f"HIGH SEVERITY ALERT: {alert['message']}")
                # Could send to external monitoring service

            elif alert['severity'] == 'medium':
                logger.warning(f"Medium severity alert: {alert['message']}")

            # Auto-remediation for certain conditions
            if alert['metric'] == 'injection_failure_rate' and alert['value'] > 0.2:
                logger.warning("High failure rate detected, clearing caches")
                self._clear_caches()

    def _clear_caches(self):
        """Clear all caches to recover from issues"""
        try:
            cache_dir = self.atlas_dir / '.atlas' / 'context_cache'
            if cache_dir.exists():
                import shutil
                shutil.rmtree(cache_dir)
                cache_dir.mkdir(parents=True, exist_ok=True)
                logger.info("Caches cleared")
        except Exception as e:
            logger.error(f"Failed to clear caches: {e}")

    def get_status(self) -> Dict:
        """Get comprehensive system status"""
        return {
            'healthy': self.healthy,
            'last_health_check': datetime.fromtimestamp(self.last_health_check).isoformat(),
            'circuit_breakers': {
                'context': self.context_breaker.get_state(),
                'detection': self.detection_breaker.get_state()
            },
            'metrics': self.metrics.export_metrics(),
            'performance': {
                'avg_latency': self.metrics.get_metric_stats('injection.latency').get('mean', 0),
                'success_rate': self._calculate_success_rate(),
                'cache_hit_rate': self._calculate_cache_hit_rate()
            }
        }

    def _calculate_success_rate(self) -> float:
        """Calculate current success rate"""
        total = self.metrics.get_counter('injections.total')
        success = self.metrics.get_counter('injections.success')
        return success / max(1, total)

    def _calculate_cache_hit_rate(self) -> float:
        """Calculate current cache hit rate"""
        hits = self.metrics.get_counter('cache.hits')
        misses = self.metrics.get_counter('cache.misses')
        total = hits + misses
        return hits / max(1, total)


# Global production instance
_production_integration = None


def get_production_integration() -> ProductionHardenedIntegration:
    """Get or create production integration instance"""
    global _production_integration
    if _production_integration is None:
        _production_integration = ProductionHardenedIntegration()
    return _production_integration


def main():
    """CLI for monitoring and management"""
    import argparse

    parser = argparse.ArgumentParser(description='Context Injection Monitor')
    parser.add_argument('command', choices=['status', 'metrics', 'alerts', 'test'])

    args = parser.parse_args()

    integration = get_production_integration()

    if args.command == 'status':
        status = integration.get_status()
        print(json.dumps(status, indent=2, default=str))

    elif args.command == 'metrics':
        metrics = integration.metrics.export_metrics()
        print(json.dumps(metrics, indent=2, default=str))

    elif args.command == 'alerts':
        alerts = integration.metrics.check_alert_conditions()
        if alerts:
            print("Active Alerts:")
            for alert in alerts:
                print(f"  [{alert['severity'].upper()}] {alert['message']}")
        else:
            print("No active alerts")

    elif args.command == 'test':
        # Test with monitoring
        test_params = {
            'description': 'Test task with monitoring',
            'prompt': 'Test prompt',
            'subagent_type': 'general-purpose'
        }

        result = integration.intercept_with_monitoring(test_params)

        if result.get('_context_metadata', {}).get('fallback'):
            print("❌ Test failed - fallback mode active")
        else:
            print("✅ Test passed - injection successful")

        print(f"\nSuccess rate: {integration._calculate_success_rate():.1%}")
        print(f"Cache hit rate: {integration._calculate_cache_hit_rate():.1%}")


if __name__ == '__main__':
    main()