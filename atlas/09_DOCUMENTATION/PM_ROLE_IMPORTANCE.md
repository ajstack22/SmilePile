# The Critical PM Role in Atlas

## What Went Wrong in SmilePile

The orchestrator created stories itself instead of spawning a PM agent. This violates Atlas principles:

**Wrong (What Happened):**
```
Orchestrator reads requirements → Orchestrator creates stories → Orchestrator spawns developers
```

**Right (What Should Happen):**
```
Orchestrator reads requirements → Spawns PM → PM creates stories → Orchestrator spawns researchers
```

## Why PM is Essential

### 1. Separation of Concerns
- **Orchestrator**: Coordinates and manages workflow
- **PM**: Defines WHAT to build
- **Developers**: Implement HOW to build

The orchestrator should NEVER define requirements - only coordinate agents who do.

### 2. Requirements Expertise
The PM agent specializes in:
- Breaking down complex requirements
- Creating clear acceptance criteria
- Prioritizing based on business value
- Using proper story formats (F####, B####, T####)

### 3. Unbiased Story Creation
PM agents start fresh without implementation bias. They focus purely on user needs and business value.

## The PM Workflow

### Step 1: Orchestrator Spawns PM
```python
# Get PM context
context = atlas_context.template("pm", "Create comprehensive backlog from requirements")

# Spawn PM agent with Task tool
Task("pm", context)
```

### Step 2: PM Creates Structured Backlog
The PM agent will:
```bash
# Create epics
python3 02_create_story.py epic "User Authentication" --priority high

# Break into features
python3 02_create_story.py feature "Login Screen" --priority critical
python3 02_create_story.py feature "Password Reset" --priority high
python3 02_create_story.py feature "Remember Me" --priority medium

# Define acceptance criteria for each
```

### Step 3: PM Returns Story IDs
```json
{
  "created_epics": ["E001"],
  "created_features": ["F0001", "F0002", "F0003", "F0004"],
  "created_bugs": [],
  "total_stories": 5,
  "high_priority_count": 2
}
```

### Step 4: Orchestrator Uses Stories for Development
Now the orchestrator has a proper backlog to execute, created by a specialist.

## PM Agent Capabilities

### Tools Available
- `Read` - To analyze requirements docs
- `Bash` - To run `02_create_story.py`

### Constraints
- CANNOT implement anything
- MUST use standard formats (F####, B####)
- MUST include acceptance criteria
- MUST assign appropriate priorities

### Output Format
Structured list of created stories with IDs and priorities

## Example PM Agent Prompt

```
You are a Product Manager for the Atlas framework.

Your task: Analyze the SmilePile requirements and create a comprehensive backlog.

You must:
1. Create 1-3 epics for major features
2. Break each epic into 3-7 user stories
3. Use python3 02_create_story.py to create each item
4. Assign priorities based on user value
5. Include clear acceptance criteria

Requirements: [SmilePile_Reqs.md content]

Start by creating the main epic, then break it down into features.
```

## The Correct Wave Structure

```
Wave 0: Product Management (1 agent)
├─ PM agent creates backlog
└─ Returns story IDs

Wave 1: Research (5 agents parallel)
├─ Based on stories from PM
└─ Research implementation patterns

Wave 2: Development (4 agents parallel)
├─ Implement stories created by PM
└─ Follow researched patterns

Wave 3: Testing (3 agents parallel)
├─ Test against PM's acceptance criteria
└─ Validate requirements met

Wave 4: Review (2 agents parallel)
├─ Verify PM's stories were completed
└─ Check acceptance criteria
```

## Red Flags That PM Was Skipped

1. ❌ Orchestrator using `02_create_story.py` directly
2. ❌ No PM agent in execution history
3. ❌ Stories created without agent context
4. ❌ Requirements translated directly to code
5. ❌ No acceptance criteria defined

## Green Flags PM Was Used

1. ✅ PM agent spawned in Wave 0
2. ✅ Clear story IDs (F####, B####) in backlog
3. ✅ Acceptance criteria in story files
4. ✅ Stories created before research begins
5. ✅ PM agent output in execution logs

## Enforcement in Atlas

### In 00_Prompt.md:
"Wave 0 MUST be a PM agent who creates the backlog. NEVER create stories yourself."

### In orchestrator workflow:
```python
def execute_project(requirements):
    # Wave 0: ALWAYS start with PM
    pm_agent = spawn_agent("pm", "Create backlog from requirements")
    stories = pm_agent.execute()

    # Only then proceed to research
    research_agents = spawn_parallel_researchers(stories)
    # ...
```

## The Cost of Skipping PM

Without PM:
- Requirements get misinterpreted
- No clear acceptance criteria
- Developers guess at priorities
- Testing doesn't know what "done" means
- Review has no baseline

With PM:
- Clear stories with acceptance criteria
- Proper prioritization
- Developers know exactly what to build
- Testing has clear success metrics
- Review validates against PM's criteria

## Summary

The PM role is NOT optional. It's the critical first step that defines what success looks like. The orchestrator coordinates HOW to achieve it, but the PM defines WHAT to achieve.

Never let the orchestrator create stories directly. Always spawn a PM agent first.