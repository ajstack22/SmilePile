#!/usr/bin/env python3
"""
Atlas Deploy Dev Script - Deploy to iOS and Android Emulators with Quality Checks
Runs comprehensive quality checks before deploying to emulators

Usage:
    python deploy_dev.py                    # Run all checks and deploy to both platforms
    python deploy_dev.py --android-only     # Deploy only to Android
    python deploy_dev.py --ios-only         # Deploy only to iOS
    python deploy_dev.py --skip-tests       # Skip tests (dangerous!)
    python deploy_dev.py --skip-lint        # Skip lint checks
    python deploy_dev.py --force            # Deploy even if checks fail
    python deploy_dev.py --quick            # Skip all checks (tests + lint)
"""

import sys
import os
import subprocess
import json
import time
from pathlib import Path
from datetime import datetime
import argparse

class DevDeployment:
    """
    Handles development deployments to emulators with comprehensive quality checks
    """

    def __init__(self):
        self.project_root = Path.cwd()
        self.results = {
            'timestamp': datetime.now().isoformat(),
            'environment': 'development',
            'unit_tests': {'status': 'pending', 'details': {}},
            'integration_tests': {'status': 'pending', 'details': {}},
            'lint': {'status': 'pending', 'details': {}},
            'build_quality': {'status': 'pending', 'details': {}},
            'android': {'status': 'pending', 'details': {}},
            'ios': {'status': 'pending', 'details': {}},
            'quality_score': 0
        }

    def run_command(self, command, cwd=None, check=True):
        """Execute a shell command and return result"""
        print(f"🔧 Executing: {command}")
        try:
            result = subprocess.run(
                command,
                shell=True,
                cwd=cwd or self.project_root,
                capture_output=True,
                text=True,
                check=check
            )
            return {
                'success': result.returncode == 0,
                'stdout': result.stdout,
                'stderr': result.stderr,
                'returncode': result.returncode
            }
        except subprocess.CalledProcessError as e:
            return {
                'success': False,
                'stdout': e.stdout if hasattr(e, 'stdout') else '',
                'stderr': e.stderr if hasattr(e, 'stderr') else str(e),
                'returncode': e.returncode if hasattr(e, 'returncode') else 1
            }

    def detect_project_type(self):
        """Detect the type of project (React Native, Flutter, Native Android, etc)"""
        if (self.project_root / 'package.json').exists():
            with open(self.project_root / 'package.json', 'r') as f:
                package = json.load(f)
                if 'react-native' in package.get('dependencies', {}):
                    return 'react-native'
                if 'expo' in package.get('dependencies', {}):
                    return 'expo'

        if (self.project_root / 'pubspec.yaml').exists():
            return 'flutter'

        if (self.project_root / 'android' / 'gradlew').exists():
            return 'android-native'

        if (self.project_root / 'ios' / 'Podfile').exists():
            return 'ios-native'

        return 'unknown'

    def run_unit_tests(self):
        """Run unit tests with coverage reporting"""
        print("\n🧪 Running Unit Tests...")
        project_type = self.detect_project_type()

        test_commands = {
            'react-native': 'npm test -- --coverage',
            'expo': 'npm test -- --coverage',
            'flutter': 'flutter test --coverage',
            'android-native': './gradlew test jacocoTestReport',
            'ios-native': 'xcodebuild test -scheme YourScheme -destination "platform=iOS Simulator"'
        }

        # Handle Android native projects (like SmilePile)
        if project_type == 'android-native':
            android_dir = self.project_root / 'android'
            if android_dir.exists():
                result = self.run_command('./gradlew test', cwd=android_dir)

                # Try to extract test results
                test_report_dir = android_dir / 'app' / 'build' / 'reports' / 'tests'
                test_count = 0
                if test_report_dir.exists():
                    # Count test files (basic metric)
                    test_count = len(list(test_report_dir.glob('**/*.html')))

                self.results['unit_tests'] = {
                    'status': 'passed' if result['success'] else 'failed',
                    'test_count': test_count,
                    'details': result
                }
                return result['success']

        # Handle other project types
        command = test_commands.get(project_type)
        if command:
            result = self.run_command(command)
            self.results['unit_tests'] = {
                'status': 'passed' if result['success'] else 'failed',
                'details': result
            }
            return result['success']

        print("⚠️  No tests found or unknown project type")
        self.results['unit_tests'] = {
            'status': 'skipped',
            'details': {'message': 'No tests found or unknown project type'}
        }
        return True  # Don't block deployment if no tests found

    def run_integration_tests(self):
        """Run integration/instrumentation tests"""
        print("\n🔗 Running Integration Tests...")
        project_type = self.detect_project_type()

        if project_type == 'android-native':
            android_dir = self.project_root / 'android'
            if android_dir.exists() and self.check_android_emulator():
                print("  📱 Running instrumented tests on emulator...")
                result = self.run_command('./gradlew connectedAndroidTest', cwd=android_dir, check=False)
                self.results['integration_tests'] = {
                    'status': 'passed' if result['success'] else 'failed',
                    'details': result
                }
                return result['success']
            else:
                print("  ⏭️  Skipping - emulator not available")
                self.results['integration_tests'] = {'status': 'skipped'}
                return True

        print("  ⏭️  No integration tests configured")
        self.results['integration_tests'] = {'status': 'skipped'}
        return True

    def run_lint_checks(self):
        """Run comprehensive lint and static analysis"""
        print("\n🔍 Running Code Quality Checks...")
        project_type = self.detect_project_type()

        lint_passed = True
        warnings = 0
        errors = 0

        if project_type == 'android-native':
            android_dir = self.project_root / 'android'
            if android_dir.exists():
                # Run Android lint
                print("  📋 Running Android Lint...")
                result = self.run_command('./gradlew lint', cwd=android_dir, check=False)

                # Parse lint results
                lint_report = android_dir / 'app' / 'build' / 'reports' / 'lint-results-debug.html'
                if lint_report.exists():
                    with open(lint_report, 'r') as f:
                        content = f.read()
                        # Basic parsing - count warnings and errors
                        warnings = content.count('Warning')
                        errors = content.count('Error')

                if result['success'] and errors == 0:
                    print(f"  ✅ Lint passed ({warnings} warnings)")
                else:
                    print(f"  ❌ Lint failed ({errors} errors, {warnings} warnings)")
                    lint_passed = False

                self.results['lint'] = {
                    'status': 'passed' if lint_passed else 'failed',
                    'warnings': warnings,
                    'errors': errors,
                    'details': result
                }
                return lint_passed

        elif project_type in ['react-native', 'expo']:
            # Run ESLint for JavaScript projects
            result = self.run_command('npm run lint', check=False)
            self.results['lint'] = {
                'status': 'passed' if result['success'] else 'failed',
                'details': result
            }
            return result['success']

        elif project_type == 'flutter':
            # Run Flutter analyze
            result = self.run_command('flutter analyze', check=False)
            self.results['lint'] = {
                'status': 'passed' if result['success'] else 'failed',
                'details': result
            }
            return result['success']

        print("  ⏭️  No lint configuration found")
        self.results['lint'] = {'status': 'skipped'}
        return True

    def check_build_quality(self):
        """Analyze build size and performance metrics"""
        print("\n📊 Analyzing Build Quality...")

        quality_metrics = {
            'apk_size': None,
            'method_count': None,
            'resource_count': None
        }

        project_type = self.detect_project_type()
        if project_type == 'android-native':
            android_dir = self.project_root / 'android'
            apk_path = android_dir / 'app' / 'build' / 'outputs' / 'apk' / 'debug' / 'app-debug.apk'

            if apk_path.exists():
                # Check APK size
                size_mb = apk_path.stat().st_size / (1024 * 1024)
                quality_metrics['apk_size'] = f"{size_mb:.2f} MB"
                print(f"  📦 APK Size: {quality_metrics['apk_size']}")

                # Run APK analyzer if available
                analyzer_result = self.run_command(f'aapt dump badging {apk_path} | grep package', check=False)
                if analyzer_result['success']:
                    print("  ✅ APK structure valid")

                # Size warnings
                if size_mb > 100:
                    print(f"  ⚠️  Warning: APK size exceeds 100MB")
                elif size_mb > 50:
                    print(f"  ℹ️  Note: Consider optimizing APK size")

        self.results['build_quality'] = {
            'status': 'analyzed',
            'metrics': quality_metrics
        }
        return True

    def calculate_quality_score(self):
        """Calculate overall quality score based on all checks"""
        score = 100

        # Deduct points for failures
        if self.results['unit_tests']['status'] == 'failed':
            score -= 30
        elif self.results['unit_tests']['status'] == 'skipped':
            score -= 10

        if self.results['integration_tests']['status'] == 'failed':
            score -= 20
        elif self.results['integration_tests']['status'] == 'skipped':
            score -= 5

        if self.results['lint']['status'] == 'failed':
            score -= 25
        elif self.results['lint']['status'] == 'skipped':
            score -= 5

        # Deduct for lint warnings
        warnings = self.results['lint'].get('warnings', 0)
        if warnings > 50:
            score -= 10
        elif warnings > 20:
            score -= 5

        self.results['quality_score'] = max(0, score)
        return score

    def check_android_emulator(self):
        """Check if Android emulator is running"""
        result = self.run_command('adb devices', check=False)
        if result['success'] and 'emulator' in result['stdout']:
            return True
        return False

    def check_ios_simulator(self):
        """Check if iOS simulator is running"""
        result = self.run_command('xcrun simctl list devices | grep "Booted"', check=False)
        if result['success'] and result['stdout'].strip():
            return True
        return False

    def deploy_android(self):
        """Deploy to Android emulator"""
        print("\n🤖 Deploying to Android...")

        if not self.check_android_emulator():
            print("⚠️  No Android emulator detected. Starting one...")
            # Try to start an emulator (you may need to adjust the AVD name)
            self.run_command('emulator -avd Pixel_9_API_35 &', check=False)
            time.sleep(10)  # Wait for emulator to start

            if not self.check_android_emulator():
                print("❌ Failed to start Android emulator")
                self.results['android']['status'] = 'failed'
                self.results['android']['details'] = {'error': 'No emulator available'}
                return False

        project_type = self.detect_project_type()

        # Build and deploy based on project type
        if project_type == 'react-native':
            result = self.run_command('npx react-native run-android')
        elif project_type == 'expo':
            result = self.run_command('expo run:android')
        elif project_type == 'flutter':
            result = self.run_command('flutter run -d android')
        elif project_type == 'android-native':
            android_dir = self.project_root / 'android'
            if android_dir.exists():
                # Build APK
                print("📦 Building APK...")
                build_result = self.run_command('./gradlew assembleDebug', cwd=android_dir)
                if not build_result['success']:
                    self.results['android']['status'] = 'failed'
                    self.results['android']['details'] = build_result
                    return False

                # Find and install APK
                apk_path = android_dir / 'app' / 'build' / 'outputs' / 'apk' / 'debug' / 'app-debug.apk'
                if apk_path.exists():
                    print("📲 Installing APK...")
                    result = self.run_command(f'adb install -r {apk_path}')

                    if result['success']:
                        # Try to launch the app
                        print("🚀 Launching app...")
                        # Try to get package name from AndroidManifest or gradle
                        launch_result = self.run_command('adb shell monkey -p com.smilepile.app -c android.intent.category.LAUNCHER 1', check=False)

                        self.results['android']['status'] = 'deployed'
                        self.results['android']['details'] = {
                            'install': result,
                            'launch': launch_result
                        }
                        return True
                else:
                    print("❌ APK not found")
                    self.results['android']['status'] = 'failed'
                    self.results['android']['details'] = {'error': 'APK not found'}
                    return False
        else:
            print("❌ Unknown project type for Android deployment")
            self.results['android']['status'] = 'skipped'
            return False

        self.results['android']['status'] = 'deployed' if result['success'] else 'failed'
        self.results['android']['details'] = result
        return result['success']

    def deploy_ios(self):
        """Deploy to iOS simulator"""
        print("\n🍎 Deploying to iOS...")

        # Check if we're on macOS
        if sys.platform != 'darwin':
            print("⚠️  iOS deployment only available on macOS")
            self.results['ios']['status'] = 'skipped'
            self.results['ios']['details'] = {'error': 'Not on macOS'}
            return False

        if not self.check_ios_simulator():
            print("⚠️  No iOS simulator detected. Starting one...")
            # Try to start a simulator
            self.run_command('open -a Simulator', check=False)
            time.sleep(5)  # Wait for simulator to start

            if not self.check_ios_simulator():
                print("❌ Failed to start iOS simulator")
                self.results['ios']['status'] = 'failed'
                self.results['ios']['details'] = {'error': 'No simulator available'}
                return False

        project_type = self.detect_project_type()

        # Build and deploy based on project type
        if project_type == 'react-native':
            result = self.run_command('npx react-native run-ios')
        elif project_type == 'expo':
            result = self.run_command('expo run:ios')
        elif project_type == 'flutter':
            result = self.run_command('flutter run -d ios')
        elif project_type == 'ios-native':
            # For native iOS, we need to build with xcodebuild
            result = self.run_command('xcodebuild -scheme YourScheme -destination "platform=iOS Simulator"')
        else:
            print("❌ No iOS support for this project type")
            self.results['ios']['status'] = 'skipped'
            self.results['ios']['details'] = {'error': 'No iOS support'}
            return False

        self.results['ios']['status'] = 'deployed' if result['success'] else 'failed'
        self.results['ios']['details'] = result
        return result['success']

    def print_summary(self):
        """Print comprehensive deployment summary with quality metrics"""
        print("\n" + "="*60)
        print("📊 DEVELOPMENT DEPLOYMENT SUMMARY")
        print("="*60)

        # Quality Checks Section
        print("\n🔍 Quality Checks:")

        # Unit Tests
        unit_status = self.results['unit_tests']['status']
        unit_icon = "✅" if unit_status == 'passed' else "❌" if unit_status == 'failed' else "⏭️"
        test_count = self.results['unit_tests'].get('test_count', 0)
        if test_count > 0:
            print(f"{unit_icon} Unit Tests: {unit_status.upper()} ({test_count} tests)")
        else:
            print(f"{unit_icon} Unit Tests: {unit_status.upper()}")

        # Integration Tests
        int_status = self.results['integration_tests']['status']
        int_icon = "✅" if int_status == 'passed' else "❌" if int_status == 'failed' else "⏭️"
        print(f"{int_icon} Integration Tests: {int_status.upper()}")

        # Lint
        lint_status = self.results['lint']['status']
        lint_icon = "✅" if lint_status == 'passed' else "❌" if lint_status == 'failed' else "⏭️"
        warnings = self.results['lint'].get('warnings', 0)
        errors = self.results['lint'].get('errors', 0)
        if lint_status != 'skipped':
            print(f"{lint_icon} Lint: {lint_status.upper()} (E:{errors} W:{warnings})")
        else:
            print(f"{lint_icon} Lint: {lint_status.upper()}")

        # Build Quality
        if 'build_quality' in self.results and self.results['build_quality'].get('metrics'):
            metrics = self.results['build_quality']['metrics']
            if metrics.get('apk_size'):
                print(f"📦 Build Size: {metrics['apk_size']}")

        # Quality Score
        score = self.results.get('quality_score', 0)
        score_icon = "🏆" if score >= 90 else "✅" if score >= 70 else "⚠️" if score >= 50 else "❌"
        print(f"\n{score_icon} Quality Score: {score}/100")

        # Deployment Section
        print("\n🚀 Deployment Status:")

        # Android
        android_status = self.results['android']['status']
        android_icon = "✅" if android_status == 'deployed' else "❌" if android_status == 'failed' else "⏭️"
        print(f"{android_icon} Android: {android_status.upper()}")

        # iOS
        ios_status = self.results['ios']['status']
        ios_icon = "✅" if ios_status == 'deployed' else "❌" if ios_status == 'failed' else "⏭️"
        print(f"{ios_icon} iOS: {ios_status.upper()}")

        print("="*60)

        # Quality recommendations
        if score < 70:
            print("\n⚠️  Quality Issues Detected:")
            if self.results['unit_tests']['status'] == 'failed':
                print("  • Fix failing unit tests")
            if self.results['lint']['status'] == 'failed':
                print("  • Address lint errors")
            if warnings > 20:
                print(f"  • Reduce lint warnings (current: {warnings})")

        # Save results
        results_file = self.project_root / '.atlas' / 'dev_deployment.json'
        results_file.parent.mkdir(parents=True, exist_ok=True)
        with open(results_file, 'w') as f:
            json.dump(self.results, f, indent=2)
        print(f"\n📁 Results saved to {results_file}")

    def run(self, skip_tests=False, skip_lint=False, android_only=False, ios_only=False, force=False, quick=False):
        """Main deployment flow with comprehensive quality checks"""
        print("\n🚀 ATLAS DEV DEPLOYMENT WITH QUALITY CHECKS")
        print("="*60)
        print(f"📍 Project: {self.project_root}")
        print(f"📱 Type: {self.detect_project_type()}")
        print("="*60)

        all_passed = True

        # Quick mode skips all checks
        if quick:
            print("\n⚡ Quick mode - skipping all quality checks")
            skip_tests = True
            skip_lint = True

        # Run quality checks unless skipped
        if not skip_tests:
            # Unit tests
            unit_passed = self.run_unit_tests()
            if not unit_passed:
                all_passed = False

            # Integration tests (non-blocking)
            self.run_integration_tests()
        else:
            print("\n⚠️  Skipping tests")
            self.results['unit_tests']['status'] = 'skipped'
            self.results['integration_tests']['status'] = 'skipped'

        # Lint checks
        if not skip_lint:
            lint_passed = self.run_lint_checks()
            if not lint_passed:
                all_passed = False
        else:
            print("\n⚠️  Skipping lint checks")
            self.results['lint']['status'] = 'skipped'

        # Always check build quality
        self.check_build_quality()

        # Calculate quality score
        quality_score = self.calculate_quality_score()

        # Quality gate
        if not all_passed and not force:
            print("\n❌ Quality checks failed! Deployment cancelled.")
            print(f"📊 Quality Score: {quality_score}/100")
            print("\nOptions:")
            print("  • Fix the issues and try again (recommended)")
            print("  • Use --force to deploy anyway (not recommended)")
            print("  • Use --quick for rapid iteration (skips all checks)")
            self.print_summary()
            return False

        if quality_score < 50 and not force:
            print(f"\n⚠️  Low quality score: {quality_score}/100")
            print("Consider addressing quality issues before deployment.")
            if not force:
                response = input("Continue anyway? (y/n): ").lower().strip()
                if response != 'y':
                    print("Deployment cancelled.")
                    self.print_summary()
                    return False

        # Deploy to platforms
        print("\n🎯 Starting deployment...")
        success = True

        if not ios_only:
            android_success = self.deploy_android()
            success = success and android_success

        if not android_only:
            ios_success = self.deploy_ios()
            # Don't fail overall if iOS fails on non-Mac
            if sys.platform == 'darwin':
                success = success and ios_success

        self.print_summary()

        # Final quality reminder
        if quality_score < 70:
            print("\n💡 Tip: Run without --quick flag to see all quality issues")

        return success

def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description='Deploy to iOS and Android emulators with quality checks')
    parser.add_argument('--skip-tests', action='store_true', help='Skip running tests')
    parser.add_argument('--skip-lint', action='store_true', help='Skip lint checks')
    parser.add_argument('--android-only', action='store_true', help='Deploy only to Android')
    parser.add_argument('--ios-only', action='store_true', help='Deploy only to iOS')
    parser.add_argument('--force', action='store_true', help='Deploy even if quality checks fail')
    parser.add_argument('--quick', action='store_true', help='Quick mode - skip all quality checks')

    args = parser.parse_args()

    deployer = DevDeployment()
    success = deployer.run(
        skip_tests=args.skip_tests,
        skip_lint=args.skip_lint,
        android_only=args.android_only,
        ios_only=args.ios_only,
        force=args.force,
        quick=args.quick
    )

    sys.exit(0 if success else 1)

if __name__ == '__main__':
    main()