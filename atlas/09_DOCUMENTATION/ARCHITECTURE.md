# Atlas Architecture: AI Managing AI

## Core Concept

Atlas implements a **layered AI orchestration system** where Claude instances coordinate and validate work through structured Python scripts that provide ground truth.

```
User → Claude (Orchestrator) → Task Tool → Claude Agents → Python Scripts → Real Work
```

## Key Principles

### 1. Scripts Don't Hallucinate
Python scripts provide **honest, factual reporting** about:
- What files exist
- What tests actually pass/fail
- What commands succeed/fail
- Current system state
- Real metrics and diagnostics

### 2. Layered Intelligence
- **Orchestrator Claude**: Strategic thinking, coordination, decision-making
- **Agent Claudes**: Specialized execution, focused tasks
- **Python Scripts**: Ground truth validation, structured processes

### 3. Trust But Verify
- Agents use creativity and intelligence to solve problems
- Scripts verify and validate what actually happened
- No mock data, no simulations - only real results

## How It Works

### Example: Troubleshooting Flow

1. **User to Orchestrator**:
   "The API is timing out in production"

2. **Orchestrator uses Task Tool**:
   ```
   Spawn troubleshooting agent with mission:
   "Debug API timeout issue using 05_troubleshoot.py"
   ```

3. **Agent runs script**:
   ```bash
   python3 05_troubleshoot.py start "API timeout" --severity critical
   python3 05_troubleshoot.py diagnose
   ```

4. **Script returns real data**:
   ```json
   {
     "system_metrics": {
       "cpu_info": "CPU usage: 12.5% user, 8.2% sys",
       "memory_info": "Pages free: 28451"
     },
     "log_analysis": {
       "recent_errors": ["Connection refused: database:5432"]
     }
   }
   ```

5. **Agent interprets and acts**:
   - Recognizes database connection issue
   - Forms hypothesis
   - Tests hypothesis
   - Implements fix

6. **Script validates fix**:
   ```bash
   python3 05_troubleshoot.py verify --metrics-restored true
   ```

## The Scripts (Ground Truth Tools)

### 00_orchestrator_context.py
**Purpose**: Maintains project state across sessions
**Returns**: Real saved state, actual project history
**Not**: Implementing anything, just tracking

### 01_research.py
**Purpose**: Searches actual codebase
**Returns**: Real files found, actual code matches
**Not**: Guessing or inventing code

### 02_create_story.py
**Purpose**: Creates real story/bug files
**Returns**: Actual files created, real paths
**Not**: Simulating story creation

### 03_adversarial_workflow.py
**Purpose**: Tracks workflow state
**Returns**: Real workflow progress, actual review results
**Not**: Faking quality scores

### 04_release_deployment.py
**Purpose**: Runs real build/test commands
**Returns**: Actual test results, real build outputs
**Not**: Mocking deployment success

### 05_troubleshoot.py
**Purpose**: Collects real system diagnostics
**Returns**: Actual CPU/memory stats, real log errors
**Not**: Generating fake metrics

### 06_update_repository.py
**Purpose**: Updates real documentation files
**Returns**: Actual files modified, real content written
**Not**: Pretending to update

## Why This Architecture?

### Problems It Solves:
1. **AI Hallucination**: Scripts can't lie about facts
2. **State Loss**: Scripts persist state between sessions
3. **Validation**: Scripts verify what really happened
4. **Structure**: Scripts enforce consistent processes

### Benefits:
1. **Reliability**: Ground truth from scripts
2. **Scalability**: Multiple agents working in parallel
3. **Traceability**: Everything logged and tracked
4. **Consistency**: Structured processes always followed

## Using Atlas Effectively

### For Simple Tasks:
```
You → Claude → Script → Result
```

### For Complex Projects:
```
You → Orchestrator Claude → Multiple Agents → Multiple Scripts → Coordinated Work
```

### Key Commands:

**Start a project:**
```bash
python3 00_orchestrator_context.py new "My Project"
```

**Research before building:**
```bash
python3 01_research.py --topic "authentication" --type full
```

**Create trackable work:**
```bash
python3 02_create_story.py story "Add login" --priority high
```

**Get real test results:**
```bash
python3 04_release_deployment.py validate
```

**Debug with real data:**
```bash
python3 05_troubleshoot.py diagnose
```

## Remember

- **Scripts report facts** - they don't hallucinate
- **Agents provide intelligence** - they interpret and decide
- **Orchestrator coordinates** - it never implements
- **Everything is tracked** - state persists across sessions

This is AI managing AI with structured validation at every step.