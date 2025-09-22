# Atlas Iteration Workflow

## The Core Problem

**We were treating the workflow as linear when it should be iterative.**

## The Wrong Way (What Happened)

```
Research → Develop → Review (fails) → Give up → Manual fix
                                          ↑
                                    ❌ STOPPED HERE
```

This violates Atlas principles by:
- Abandoning the workflow at first failure
- Not iterating to fix issues
- Taking manual shortcuts outside the system

## The Right Way (Iteration Until Success)

```
┌──────────────────────────────────────────────────────────────┐
│                        ORCHESTRATOR                          │
│                   (Manages iteration loop)                   │
└────────┬─────────────────────────────────────────────────────┘
         │
         ▼
    [Research]
         │
         ▼
    [Create Stories]
         │
         ▼
┌────────────────────────────────────────┐
│          ITERATION LOOP                 │
│                                         │
│  ┌─→ [Develop] ──→ [Build] ─┐          │
│  │        ↓           ↓      │          │
│  │      Fails?     Success?  │          │
│  │        ↓           ↓      │          │
│  │    Fix Errors   [Test]    │          │
│  │        ↑           ↓      │          │
│  │        └──────── Fails?   │          │
│  │                    ↓      │          │
│  │              [Adversarial │          │
│  │                Review]    │          │
│  │                    ↓      │          │
│  │                Issues? ───┘          │
│  │                    ↓                 │
│  │                   No                 │
│  └──────────────────────────────────────┤
│                      ↓                  │
│                   SUCCESS               │
└─────────────────────────────────────────┘
```

## Iteration Rules

### 1. Build Must Pass Before Review
```python
while not build_passing:
    agent = spawn_developer("Fix compilation errors")
    result = agent.execute()
    build_passing = verify_build()
# Only NOW proceed to review
```

### 2. Review Issues Trigger New Development Cycle
```python
review_result = adversarial_review()
while review_result.has_issues():
    agent = spawn_developer(f"Fix issues: {review_result.issues}")
    agent.execute()

    # Must pass build again
    while not verify_build():
        fix_agent = spawn_developer("Fix new compilation errors")
        fix_agent.execute()

    # Review again
    review_result = adversarial_review()
```

### 3. Never Exit the Loop Until Success
```python
success = False
attempts = 0
max_attempts = 10  # Prevent infinite loops

while not success and attempts < max_attempts:
    develop()

    if not build_passes():
        fix_build_errors()
        continue

    if not tests_pass():
        fix_test_failures()
        continue

    review = adversarial_review()
    if not review.passes():
        fix_review_issues()
        continue

    success = True
    attempts += 1
```

## Common Iteration Scenarios

### Scenario 1: Build Failure After Development
```
Developer writes code → Build fails → Fix agent spawned → Build passes → Continue
```

### Scenario 2: Review Finds Issues
```
Code builds → Review finds bugs → Developer fixes → Build fails (due to fix) →
Fix build → Build passes → Review again → Pass
```

### Scenario 3: Test Failures
```
Code builds → Tests fail → Developer fixes → Build passes → Tests pass →
Review → Pass
```

## Orchestrator's Iteration Responsibilities

The orchestrator MUST:

1. **Track iteration state**
```python
iteration_state = {
    'attempt': 1,
    'build_status': 'pending',
    'test_status': 'pending',
    'review_status': 'pending',
    'issues': []
}
```

2. **Spawn appropriate agents for each failure**
```python
if iteration_state['build_status'] == 'failed':
    spawn_agent('developer', 'Fix compilation errors')
elif iteration_state['test_status'] == 'failed':
    spawn_agent('developer', 'Fix failing tests')
elif iteration_state['review_status'] == 'failed':
    spawn_agent('developer', f"Address review issues: {iteration_state['issues']}")
```

3. **Communicate iteration status**
```
📊 Iteration Status: Attempt 3
- Build: ✅ Passing
- Tests: ✅ Passing
- Review: 🔄 In Progress
```

## The Iteration Mindset

### Wrong Mindset
"The review found issues, so the story failed. I'll fix it manually."

### Right Mindset
"The review found issues, which is good! Now I'll spawn agents to fix them, verify the build still passes, and review again. We iterate until perfect."

## Success Criteria

A story is ONLY complete when ALL of these pass in sequence:
1. ✅ Code compiles without errors
2. ✅ Tests pass
3. ✅ Adversarial review passes
4. ✅ Final build verification passes
5. ✅ Compliance check passes

If ANY fail, we iterate.

## Anti-Patterns to Avoid

❌ **Giving up after first failure** - Always iterate
❌ **Skipping build verification** - Always compile
❌ **Manual fixes outside workflow** - Always use agents
❌ **Proceeding with known issues** - Always fix first
❌ **Ignoring review feedback** - Always address

## The Golden Rule of Iteration

**"If it's not perfect, iterate until it is."**

No shortcuts. No manual fixes. No giving up.

## Implementation in Orchestrator

```python
def execute_story_with_iteration(story_id):
    """Execute a story with proper iteration"""

    max_iterations = 10
    iteration = 0

    while iteration < max_iterations:
        iteration += 1
        print(f"📊 Iteration {iteration}")

        # Develop
        dev_result = spawn_developer(story_id)

        # Verify build
        if not verify_build():
            spawn_fixer("compilation errors")
            continue

        # Test
        if not run_tests():
            spawn_fixer("test failures")
            continue

        # Review
        review = adversarial_review(story_id)
        if not review.passes():
            spawn_fixer(review.issues)
            continue

        # Success!
        print(f"✅ Story {story_id} complete after {iteration} iterations")
        return True

    print(f"⚠️ Max iterations reached for {story_id}")
    return False
```

Remember: **Iteration is not failure - it's the path to quality.**