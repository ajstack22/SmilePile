# Parallel Execution Enforcement

## The SmilePile Failure: NO Parallel Agents

**Evidence from `.atlas/orchestrator/context.json`:**
```json
"active_agents": []  // EMPTY! No parallel execution!
```

This is a **critical violation** of Atlas principles.

## MANDATORY: 3-5 Agents Per Wave

### The Problem We're Solving

The SmilePile orchestrator ran **sequentially** (1 agent at a time) instead of **parallel** (3-5 agents simultaneously).

**Result**:
- 5x slower execution
- Wasted time
- Poor Atlas implementation

## Enforcement Rules

### Rule 1: MINIMUM 3 Agents Per Wave

**WRONG (Sequential):**
```python
spawn_agent("research", "Research ViewPager2")
# Wait for completion...
spawn_agent("research", "Research Room")
# Wait for completion...
spawn_agent("research", "Research Coil")
```
**Time**: 3 × 20min = 60 minutes ❌

**RIGHT (Parallel):**
```python
# Spawn ALL AT ONCE using Task tool
spawn_agent("research", "Research ViewPager2")
spawn_agent("research", "Research Room")
spawn_agent("research", "Research Coil")
```
**Time**: 1 × 20min = 20 minutes ✅

### Rule 2: Track Parallel Execution

**MUST create tracking file:**
```bash
python3 parallel_tracker.py track '[
  {"id":"AGT001","role":"researcher","task":"ViewPager2"},
  {"id":"AGT002","role":"researcher","task":"Room DB"},
  {"id":"AGT003","role":"researcher","task":"Coil"}
]'
```

### Rule 3: Verify Parallelism

**Check for parallel evidence:**
```bash
# Should see multiple parallel batch files
ls .atlas/orchestrator/parallel_*.json

# Should show 3+ agents in active_agents
cat .atlas/orchestrator/context.json | grep active_agents
```

## Orchestrator Parallel Patterns

### Wave 1: Research (PARALLEL)
```python
# MUST spawn 3-5 researchers SIMULTANEOUSLY
agents = [
    {"role": "researcher", "task": "ViewPager2 patterns"},
    {"role": "researcher", "task": "Room database setup"},
    {"role": "researcher", "task": "Coil image loading"},
    {"role": "researcher", "task": "Gesture handling"},
    {"role": "researcher", "task": "Category management"}
]

# Use Task tool 5 TIMES IN SAME MESSAGE
for agent in agents:
    Task(agent)  # All in ONE response, not separate
```

### Wave 2: Development (PARALLEL)
```python
# MUST spawn 3-4 developers SIMULTANEOUSLY
agents = [
    {"role": "developer", "task": "Database layer"},
    {"role": "developer", "task": "UI components"},
    {"role": "developer", "task": "Image handling"},
    {"role": "developer", "task": "Navigation"}
]

# Again, ALL IN ONE RESPONSE
for agent in agents:
    Task(agent)
```

## Detection: How to Spot Sequential Execution

### Red Flags 🚩
1. **Empty `active_agents`** in context.json
2. **No `parallel_*.json` files** in .atlas/orchestrator/
3. **No parallel tracker calls** in execution
4. **Status shows "1 agent running"** instead of "5 agents running"
5. **Takes 5x longer** than expected

### Green Flags ✅
1. **`active_agents` has 3-5 entries**
2. **Multiple `parallel_BATCH_*.json` files**
3. **Status shows "5 agents running in parallel"**
4. **Optimization score > 75**
5. **Completes in expected timeframe**

## The Correct First Response

When orchestrator sees SmilePile requirements, it should:

```
🚀 Starting SmilePile development with PARALLEL execution.

Spawning Wave 1: 5 Research Agents (SIMULTANEOUSLY):

[Uses Task tool 5 times IN THE SAME RESPONSE]
- AGT001: Researching ViewPager2
- AGT002: Researching Room database
- AGT003: Researching Coil
- AGT004: Researching gestures
- AGT005: Researching categories

[Status should show:]
📊 Progress: Wave 1/4 | 5 agents running | Parallelization: 5x
```

NOT:
```
Starting research...
[Uses Task tool once]
AGT001 researching...
[Waits...]
Now researching next item...
[Uses Task tool again]
```

## Verification Script

Add to orchestrator workflow:

```python
def verify_parallel_execution():
    """Ensure we're actually running in parallel"""

    context = load_context()
    active_count = len(context.get('active_agents', []))

    if active_count < 3:
        print("⚠️ WARNING: Only {} agents running!".format(active_count))
        print("Atlas requires 3-5 parallel agents!")
        return False

    print("✅ {} agents running in parallel".format(active_count))
    return True
```

## Update to 00_Prompt.md

Add this warning:

```
⚠️ CRITICAL PARALLEL REQUIREMENT ⚠️

You MUST spawn 3-5 agents PER WAVE using the Task tool.
All agents in a wave must be spawned IN THE SAME RESPONSE.

WRONG: Spawn agent, wait, spawn another
RIGHT: Spawn 5 agents simultaneously in one response

If .atlas/orchestrator/context.json shows "active_agents": [],
you have FAILED to execute in parallel!
```

## The Cost of Sequential Execution

### SmilePile Timeline (Sequential) - What Happened:
- Research: 5 agents × 20min = 100 minutes
- Development: 4 agents × 30min = 120 minutes
- Testing: 3 agents × 20min = 60 minutes
- **Total: 280 minutes (4.7 hours)**

### SmilePile Timeline (Parallel) - What Should Happen:
- Research: 5 agents parallel = 20 minutes
- Development: 4 agents parallel = 30 minutes
- Testing: 3 agents parallel = 20 minutes
- **Total: 70 minutes (1.2 hours)**

**4x faster with proper parallel execution!**

## Summary

The SmilePile orchestrator **completely failed** to use parallel execution, violating core Atlas principles.

**Never again** should we see:
- Empty `active_agents`
- No parallel tracking
- Sequential execution
- 1 agent at a time

**Always** ensure:
- 3-5 agents per wave
- All spawned in same response
- Parallel tracking active
- Verification of parallelism

Parallel execution is not optional - it's **MANDATORY**.