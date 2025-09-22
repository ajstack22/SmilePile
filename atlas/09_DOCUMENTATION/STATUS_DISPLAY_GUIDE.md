# Atlas Orchestrator Status Display Guide

## Overview

The orchestrator now provides **real-time visual progress tracking** during parallel agent execution, making it easy to see exactly what's happening at any point in the operation.

## Key Features

### 1. Progress Bars
- **Overall Progress**: Main operation progress `[████████░░░░░░░░] 50%`
- **Agent Progress**: Individual agent status `[▓▓▓▓▓▓░░░░] 30%`
- **Wave Indicator**: Execution waves `[✅ → 🔄 → ⭕]`

### 2. Time Tracking
- Elapsed time since operation start
- ETA based on completion rate
- Timestamps for recent activities

### 3. Agent Monitoring
- Real-time status (pending, running, completed, failed)
- Individual progress percentages
- Task descriptions
- Parallelization speedup metrics

### 4. Wave Execution
- Visual wave progression
- Agents per wave
- Wave completion tracking

## Usage Patterns

### Starting an Operation

```bash
# Initialize with total steps and agent list
python3 orchestrator_status.py start "Build Authentication Feature" \
  --steps 20 \
  --agents '[
    {"id":"AGT001","role":"researcher","task":"Research auth patterns"},
    {"id":"AGT002","role":"researcher","task":"Find JWT examples"},
    {"id":"AGT003","role":"developer","task":"Implement auth model"}
  ]'
```

### Regular Updates (Every 30-60 seconds)

```bash
# Show current status
python3 orchestrator_status.py show

# Update phase
python3 orchestrator_status.py update \
  --phase "Development" \
  --message "Research complete, building components"

# Update specific agent
python3 orchestrator_status.py agent AGT001 running --progress 75
```

### Wave-Based Execution

```bash
# Wave 1: Research (5 agents)
python3 orchestrator_status.py wave 1 3 '[
  {"id":"AGT001","role":"researcher"},
  {"id":"AGT002","role":"researcher"},
  {"id":"AGT003","role":"researcher"},
  {"id":"AGT004","role":"researcher"},
  {"id":"AGT005","role":"researcher"}
]'

# Wave 2: Development (4 agents)
python3 orchestrator_status.py wave 2 3 '[
  {"id":"AGT006","role":"developer"},
  {"id":"AGT007","role":"developer"},
  {"id":"AGT008","role":"developer"},
  {"id":"AGT009","role":"developer"}
]'

# Wave 3: Testing (3 agents)
python3 orchestrator_status.py wave 3 3 '[
  {"id":"AGT010","role":"tester"},
  {"id":"AGT011","role":"reviewer"},
  {"id":"AGT012","role":"documenter"}
]'
```

### Completion

```bash
# Mark operation complete
python3 orchestrator_status.py complete \
  --summary "Authentication feature implemented and tested successfully"
```

## Display Elements Explained

```
================================================================================
🎯 ORCHESTRATOR STATUS: Building Feature X          <- Operation name
================================================================================
⏱️  Elapsed: 2m 34s                                 <- Time tracking
📍 Phase: Wave 2/3                                  <- Current phase

Overall Progress: [████████░░░░░░░░░░░░] 40.0%      <- Main progress bar

Execution Waves: [✅ → 🔄 → ⭕]                      <- Wave visualization
Agents in current wave: 4                          <- Current parallelism

🤖 AGENT STATUS:                                    <- Individual agents
----------------------------------------
  🔄 AGT003 (developer): [████░░░░] 40%            <- Running agent
     Task: Implement user authentication model...
  ✅ Completed: 2 agents                            <- Completed count
  ⏳ Pending: 3 agents                              <- Waiting to start

📋 RECENT ACTIVITY:                                 <- Event log
----------------------------------------
  [14:32:45] Research phase complete
  [14:33:12] Starting development wave

📊 METRICS:                                         <- Performance stats
----------------------------------------
  Agents: 2/9 completed, 2 running
  ETA: 4m 15s                                      <- Estimated completion
  Parallelization: 2x speedup                      <- Efficiency metric
================================================================================
```

## Icons & Symbols

- 🎯 **Target**: Main operation
- ⏱️ **Timer**: Elapsed time
- 📍 **Pin**: Current phase
- 🤖 **Robot**: Agent section
- 🔄 **Spinning**: Running agent
- ✅ **Check**: Completed
- ⏳ **Hourglass**: Pending
- ❌ **Cross**: Failed
- 📋 **Clipboard**: Activity log
- 📊 **Chart**: Metrics

## Best Practices

### 1. Update Frequency
- **Major events**: Always update (phase changes, completions)
- **Regular intervals**: Every 30-60 seconds
- **User visibility**: Before long-running operations

### 2. Progress Granularity
```python
# Good: Meaningful progress increments
0% -> 25% (Research complete)
25% -> 50% (Development complete)
50% -> 75% (Testing complete)
75% -> 100% (Documentation complete)

# Bad: Too granular
0% -> 1% -> 2% -> 3% (noisy)
```

### 3. Message Clarity
```python
# Good: Specific and actionable
"Authentication module compiled successfully"
"3 of 5 test suites passed"
"Database migration complete"

# Bad: Vague
"Processing..."
"Working on it"
"Almost done"
```

### 4. Error Handling
```bash
# If agent fails
python3 orchestrator_status.py agent AGT001 failed
python3 orchestrator_status.py update --message "AGT001 failed: Retrying with fix"

# Retry with new agent
python3 orchestrator_status.py agent AGT013 running --progress 0
```

## Integration with Other Tools

### With Parallel Tracking
```bash
# Track parallel spawn
python3 parallel_tracker.py track '[agents_json]'

# Update status display
python3 orchestrator_status.py update --message "Parallel batch spawned"
```

### With Task Aggregator
```bash
# As agents complete
python3 orchestrator_status.py agent AGT001 completed --progress 100

# Store their results
python3 task_aggregator.py store AGT001 researcher "task" 'results'

# Update display
python3 orchestrator_status.py update --message "Results aggregated"
```

### With Compliance Check
```bash
# After wave completes
python3 compliance_check.py validate

# Update status with results
python3 orchestrator_status.py update \
  --phase "Validation" \
  --message "Compliance check: 95% passed"
```

## Example: Complete Operation Flow

```bash
# 1. Start operation
python3 orchestrator_status.py start "Implement User Management" --steps 30

# 2. Wave 1: Research (0-5 minutes)
python3 orchestrator_status.py wave 1 4 '[research_agents]'
python3 orchestrator_status.py agent AGT001 running --progress 25
python3 orchestrator_status.py agent AGT001 running --progress 50
python3 orchestrator_status.py agent AGT001 completed --progress 100

# 3. Wave 2: Development (5-15 minutes)
python3 orchestrator_status.py wave 2 4 '[dev_agents]'
python3 orchestrator_status.py update --phase "Building components"
# ... periodic updates ...

# 4. Wave 3: Testing (15-20 minutes)
python3 orchestrator_status.py wave 3 4 '[test_agents]'
python3 orchestrator_status.py update --phase "Running test suites"
# ... periodic updates ...

# 5. Wave 4: Documentation (20-25 minutes)
python3 orchestrator_status.py wave 4 4 '[doc_agents]'
python3 orchestrator_status.py update --phase "Updating documentation"

# 6. Complete
python3 orchestrator_status.py complete --summary "User management feature complete: 15 components, 95% test coverage"
```

## Tips for Users

1. **Look for the progress bar** - Quick visual of overall status
2. **Check wave indicator** - Shows execution phase
3. **Monitor agent counts** - Confirms parallelization is working
4. **Watch ETA** - Helps set expectations
5. **Review recent activity** - Understand what just happened

## Troubleshooting

### Display not updating?
```bash
# Manually refresh
python3 orchestrator_status.py show

# Check status file
cat .atlas/orchestrator/current_status.json
```

### Progress seems stuck?
```bash
# Check individual agents
python3 orchestrator_status.py show | grep "running"

# Force phase update
python3 orchestrator_status.py update --phase "Investigating issue"
```

### Need to restart?
```bash
# Complete current operation
python3 orchestrator_status.py complete --summary "Aborted"

# Start fresh
python3 orchestrator_status.py start "New Operation" --steps 10
```

## Summary

The status display system provides:
- **Real-time visibility** into parallel agent execution
- **Progress tracking** at operation, wave, and agent levels
- **Performance metrics** to verify efficiency
- **Event logging** for audit trail
- **ETA calculation** for planning

This ensures users always know what the orchestrator is doing and how long it will take!