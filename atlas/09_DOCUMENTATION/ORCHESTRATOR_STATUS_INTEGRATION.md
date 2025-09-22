# Orchestrator Status Display Integration

## The Challenge

When the orchestrator (Claude) runs status commands via Bash tool, you see:
```
⏺ Bash(cd /path && python3 orchestrator_status.py ...)
  ⎿ [truncated output]
```

This truncates the beautiful status display. Here's how to properly integrate status updates.

## Solution: Status as Communication

Instead of relying on Bash output display, the orchestrator should:

1. **Run status commands** (they still update the backend)
2. **Explicitly communicate** the status to you in messages
3. **Show progress** at regular intervals

## Proper Integration Pattern

### What You'll See From Orchestrator

Instead of truncated tool output, the orchestrator will message you directly:

```
🎯 **Starting Operation: Build Authentication Feature**

I'm spawning 5 parallel research agents to gather information about authentication patterns. This should take approximately 3-5 minutes.

**Current Status:**
```
================================================================================
🎯 ORCHESTRATOR STATUS: Build Authentication Feature
================================================================================
⏱️  Elapsed: 0m 15s
📍 Phase: Wave 1/3 - Research

Overall Progress: [████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 10%

🤖 Agents Running:
- AGT001 (researcher): Finding OAuth patterns [40%]
- AGT002 (researcher): Researching JWT best practices [60%]
- AGT003 (researcher): Analyzing session management [30%]
- AGT004 (researcher): Security review [50%]
- AGT005 (researcher): Database schemas [20%]

📊 Efficiency: 5x parallel speedup active
⏰ ETA: ~4 minutes remaining
================================================================================
```

### Orchestrator's Status Update Messages

The orchestrator should send these types of status messages:

#### 1. Operation Start
```
🚀 **Initiating: [Operation Name]**

Plan:
- Wave 1: 5 research agents (parallel)
- Wave 2: 4 development agents (parallel)
- Wave 3: 3 testing agents (parallel)

Total estimated time: 15-20 minutes
```

#### 2. Periodic Updates (every 30-60s)
```
📊 **Progress Update**

✅ Wave 1 Complete: Research gathered
🔄 Wave 2 Running: Development (40% complete)
⏳ Wave 3 Pending: Testing

Agents: 6/12 completed | Current: 4 running | ETA: 8 minutes
```

#### 3. Wave Transitions
```
🌊 **Wave 2 Starting: Development Phase**

Based on research findings, spawning 4 developers:
- Auth module implementation
- Database schema creation
- API endpoint development
- UI component building

All running in parallel for maximum efficiency.
```

#### 4. Completion
```
✅ **Operation Complete: Build Authentication Feature**

Results:
- ✓ 12/12 agents succeeded
- ✓ 95% test coverage achieved
- ✓ All compliance checks passed
- ⏱️ Total time: 18m 34s (vs ~60m sequential)

The authentication feature is ready for integration.
```

## Implementation in CLAUDE.md

The orchestrator should follow this pattern:

```python
# 1. Start operation (run command + message user)
bash: python3 orchestrator_status.py start "Build Auth" --steps 20
message: "🚀 Starting authentication build with 3 waves of parallel agents..."

# 2. Spawn agents with Task tool
[Use Task tool to spawn agents]

# 3. Update status (run command + message user)
bash: python3 orchestrator_status.py agent AGT001 running --progress 50
message: "📊 Progress: Research 50% complete, 5 agents running in parallel"

# 4. Periodic status message (every 30-60s)
message: """
📊 **Status Update**
Wave 1: ✅ Complete (5 researchers)
Wave 2: 🔄 Running (4 developers - 60% done)
Wave 3: ⏳ Pending
ETA: 5 minutes
"""

# 5. Completion (run command + message user)
bash: python3 orchestrator_status.py complete --summary "Success"
message: "✅ Authentication feature complete! 12 agents, 18 minutes total."
```

## What Users Actually Want to See

### Minimal Noise Format
```
🔄 Building authentication...
[████████████░░░░░░░░] 60% | Wave 2/3 | 4 agents running | ETA: 5m
```

### Detailed Format (on request or at milestones)
```
📊 Detailed Status:
├─ Research: ✅ Complete (5 agents, 4m 20s)
├─ Development: 🔄 70% (4 agents running)
│  ├─ Auth module: 80%
│  ├─ Database: 60%
│  ├─ API: 75%
│  └─ UI: 65%
└─ Testing: ⏳ Pending (3 agents queued)
```

## Status Check Commands

Users can always check detailed status manually:

```bash
# Quick status
python3 orchestrator_status.py show

# Detailed agent view
python3 orchestrator_status.py show --verbose

# Just the progress bar
python3 orchestrator_status.py show --minimal
```

## Best Practices for Orchestrator

### DO:
- ✅ Send concise status messages every 30-60 seconds
- ✅ Use progress bars in messages
- ✅ Announce wave transitions
- ✅ Show ETA and efficiency metrics
- ✅ Celebrate completions with summary

### DON'T:
- ❌ Rely on Bash tool output for user communication
- ❌ Send status updates too frequently (spammy)
- ❌ Forget to update when phases change
- ❌ Hide failures or issues

## Example Orchestrator Session

```
Orchestrator: 🚀 Starting feature build with 15 parallel agents across 3 waves...

[Orchestrator runs status commands in background]

Orchestrator: Wave 1 complete! Research gathered. Starting development...
[████████░░░░░░░░░░░░] 40% | Wave 2/3 | 4 agents | ETA: 10m

[30 seconds pass]

Orchestrator: Development progressing well:
[██████████████░░░░░░] 70% | Wave 2/3 | 4 agents | ETA: 6m

Orchestrator: 🌊 Wave 3 starting: Testing and validation...

[Time passes]

Orchestrator: ✅ Feature complete!
- Total time: 18m 34s (3.3x faster than sequential)
- All tests passing
- Ready for deployment
```

## The Key Insight

**Status commands update the tracking system, but the orchestrator must explicitly communicate progress to users through messages, not rely on tool output display.**

This ensures users see beautiful, informative progress updates rather than truncated tool invocations!