#!/usr/bin/env python3
"""
Test script to verify checklist integration with context injection
"""

import sys
import json
from pathlib import Path

# Add automation directory to path
script_dir = Path(__file__).parent
if script_dir.name == 'atlas':
    automation_path = script_dir / '07_AUTOMATION'
else:
    automation_path = Path.cwd() / 'atlas' / '07_AUTOMATION'

sys.path.insert(0, str(automation_path))

from task_context_integration import TaskContextIntegration
from enhanced_context_injector import EnhancedContextInjector

def test_checklist_formatting():
    """Test that checklists are properly formatted"""
    print("\n" + "="*60)
    print("TEST 1: Checklist Formatting")
    print("="*60)

    injector = EnhancedContextInjector()

    # Test story creation checklist
    checklist = injector._format_checklist('story_creation')

    print("\nStory Creation Checklist:")
    print("-" * 40)
    print(checklist)

    # Verify structure
    assert "✅ Verification Checklist" in checklist
    assert "Required Checks:" in checklist
    assert "[ ]" in checklist  # Checkbox format
    assert "User story follows" in checklist

    print("\n✅ PASSED: Checklist formatted correctly")
    return True

def test_checklist_in_context():
    """Test that checklists are included in injected context"""
    print("\n" + "="*60)
    print("TEST 2: Checklist in Context Injection")
    print("="*60)

    injector = EnhancedContextInjector()

    # Build context with checklist
    result = injector.build_context(task='story_creation', verbose=False)

    context = result['context']
    metadata = result['metadata']

    print(f"\nContext size: {len(context)} chars")
    print(f"Token estimate: {metadata['total_tokens']}")
    print(f"Checklist included: {metadata.get('checklist_included', False)}")

    # Verify checklist is in context
    assert "✅ Verification Checklist" in context
    assert "Story Creation Checklist" in context
    assert metadata.get('checklist_included') == True

    # Count checklist items
    checklist_items = context.count("[ ]")
    print(f"Checklist items found: {checklist_items}")
    assert checklist_items >= 10  # Story creation has 10 items

    print("\n✅ PASSED: Checklist included in context")
    return True

def test_agent_gets_checklist():
    """Test that agents receive checklists via Task interception"""
    print("\n" + "="*60)
    print("TEST 3: Agent Receives Checklist")
    print("="*60)

    integration = TaskContextIntegration()

    # Simulate story-writer agent task
    params = {
        'description': 'Create user story for payment processing',
        'prompt': 'Users need to process payments securely',
        'subagent_type': 'story-writer'
    }

    result = integration.intercept_task_tool(params)

    print(f"\nProfile: {result['_context_metadata']['profile']}")
    print(f"Tokens: {result['_context_metadata']['tokens']}")
    print(f"Checklist included: {result['_context_metadata'].get('checklist_included', False)}")

    # Verify checklist in prompt
    assert "✅ Verification Checklist" in result['prompt']
    assert "Story Creation Checklist" in result['prompt']
    assert result['_context_metadata'].get('checklist_included') == True

    # Count requirements
    required_count = result['prompt'].count("Required Checks:")
    print(f"Required sections found: {required_count}")

    print("\n✅ PASSED: Agent receives checklist in context")
    return True

def test_different_profiles_get_different_checklists():
    """Test that different profiles get their specific checklists"""
    print("\n" + "="*60)
    print("TEST 4: Profile-Specific Checklists")
    print("="*60)

    integration = TaskContextIntegration()

    test_cases = [
        ('story-writer', 'story_creation', 'Story Creation Checklist'),
        ('bug-fixer', 'troubleshooting', 'Bug Fix Checklist'),
        ('ui-developer', 'ui_development', 'UI Development Checklist'),
        ('backend-developer', 'backend_development', 'Backend Development Checklist'),
        ('tester', 'testing', 'Test Creation Checklist'),
    ]

    for agent_type, expected_profile, expected_checklist in test_cases:
        params = {
            'description': f'Task for {agent_type}',
            'prompt': 'Task details',
            'subagent_type': agent_type
        }

        result = integration.intercept_task_tool(params)

        assert result['_context_metadata']['profile'] == expected_profile
        assert expected_checklist in result['prompt']
        print(f"{agent_type} → {expected_checklist} ✅")

    print("\n✅ PASSED: All profiles get correct checklists")
    return True

def test_checklist_performance():
    """Test that adding checklists doesn't impact performance"""
    print("\n" + "="*60)
    print("TEST 5: Checklist Performance Impact")
    print("="*60)

    import time

    integration = TaskContextIntegration()

    times_with_checklist = []
    times_without_checklist = []

    # Measure with checklists
    for i in range(5):
        params = {
            'description': f'Test task {i}',
            'prompt': 'Test prompt',
            'subagent_type': 'story-writer'
        }

        start = time.time()
        result = integration.intercept_task_tool(params)
        elapsed = time.time() - start
        times_with_checklist.append(elapsed)

    # Temporarily disable checklists (by clearing them)
    injector = integration.injector
    original_checklists = injector.checklists
    injector.checklists = {}

    # Measure without checklists
    for i in range(5):
        params = {
            'description': f'Test task {i}',
            'prompt': 'Test prompt',
            'subagent_type': 'story-writer'
        }

        start = time.time()
        result = integration.intercept_task_tool(params)
        elapsed = time.time() - start
        times_without_checklist.append(elapsed)

    # Restore checklists
    injector.checklists = original_checklists

    avg_with = sum(times_with_checklist) / len(times_with_checklist)
    avg_without = sum(times_without_checklist) / len(times_without_checklist)
    overhead = (avg_with - avg_without) * 1000

    print(f"Average with checklists: {avg_with*1000:.1f}ms")
    print(f"Average without checklists: {avg_without*1000:.1f}ms")
    print(f"Overhead: {overhead:.1f}ms")

    # Ensure overhead is minimal (< 10ms)
    assert overhead < 10, f"Checklist overhead too high: {overhead:.1f}ms"

    print("\n✅ PASSED: Checklist overhead is minimal")
    return True

def run_all_tests():
    """Run all checklist integration tests"""
    print("\n" + "="*60)
    print("ATLAS CHECKLIST INTEGRATION TEST SUITE")
    print("="*60)

    tests = [
        test_checklist_formatting,
        test_checklist_in_context,
        test_agent_gets_checklist,
        test_different_profiles_get_different_checklists,
        test_checklist_performance,
    ]

    passed = 0
    failed = 0

    for test in tests:
        try:
            if test():
                passed += 1
        except Exception as e:
            print(f"❌ FAILED: {e}")
            import traceback
            traceback.print_exc()
            failed += 1

    print("\n" + "="*60)
    print("FINAL RESULTS")
    print("="*60)
    print(f"Passed: {passed}/{len(tests)}")
    print(f"Failed: {failed}/{len(tests)}")

    if failed == 0:
        print("\n🎉 ALL CHECKLIST TESTS PASSED!")
        print("Checklists are successfully integrated into context injection!")
    else:
        print(f"\n⚠️  {failed} tests failed. See details above.")

    return failed == 0

if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)