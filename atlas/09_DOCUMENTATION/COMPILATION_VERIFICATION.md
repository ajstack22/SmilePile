# Atlas Compilation Verification Requirements

## CRITICAL RULE: Code Must Compile Before Task Completion

**NO agent can mark their task complete without verifying their code compiles and runs.**

## The Problem We're Solving

Agents were:
- ❌ Writing code without compiling it
- ❌ Marking tasks complete with broken code
- ❌ Passing broken code to review
- ❌ Creating cascading failures

This violates the fundamental principle: **Working Software**

## Mandatory Verification Steps

### For Developer Agents

Every developer agent MUST:

1. **Write the code**
2. **Compile/Build it**
3. **Fix any errors**
4. **Verify it runs**
5. **Only then mark complete**

### Platform-Specific Build Commands

#### Android (Kotlin/Java)
```bash
# MUST run before marking complete
./gradlew build
./gradlew assembleDebug

# If build fails, agent MUST fix and retry
```

#### iOS (Swift)
```bash
# MUST run before marking complete
xcodebuild -project App.xcodeproj -scheme App -destination 'platform=iOS Simulator,name=iPhone 14'

# If build fails, agent MUST fix and retry
```

#### Web (React/Vue/Angular)
```bash
# MUST run before marking complete
npm run build
# or
yarn build

# If build fails, agent MUST fix and retry
```

#### Python
```bash
# MUST run before marking complete
python -m py_compile *.py
python -m pytest

# If syntax errors, agent MUST fix and retry
```

#### Go
```bash
# MUST run before marking complete
go build ./...
go test ./...

# If build fails, agent MUST fix and retry
```

## The Iteration Loop

```
┌─────────────────────────────────────────────────────┐
│                  ORCHESTRATOR                        │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
        Spawn Developer
              │
              ▼
┌─────────────────────────────────────────────────────┐
│            DEVELOPER AGENT                           │
│                                                      │
│  1. Write code                                      │
│  2. Run build command ──────┐                       │
│  3. Build successful? ←─────┤                       │
│     ├─ NO: Fix errors ──────┘                       │
│     └─ YES: Continue                                │
│  4. Run basic tests                                 │
│  5. Tests pass? ─────────────┐                      │
│     ├─ NO: Fix code ─────────┘                      │
│     └─ YES: Mark complete                           │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
        Spawn Reviewer
              │
              ▼
┌─────────────────────────────────────────────────────┐
│          ADVERSARIAL REVIEWER                        │
│                                                      │
│  1. Review code                                     │
│  2. Run build command                               │
│  3. Run tests                                       │
│  4. Find issues? ────────────┐                      │
│     ├─ YES: Report issues ───┼─→ Back to Developer  │
│     └─ NO: Approve ──────────┘                      │
└─────────────────────────────────────────────────────┘
```

## Required Agent Instructions Update

### Developer Agent Context Must Include:

```python
# In atlas_context.py, developer role MUST have:

'constraints': [
    'MUST compile/build code before marking complete',
    'MUST fix all compilation errors',
    'MUST verify code runs',
    'NEVER mark task complete with broken code',
    # ... existing constraints
]

'workflow': [
    '1. Write code implementation',
    '2. Run build command for platform',
    '3. If build fails: fix errors and retry',
    '4. Run basic smoke test',
    '5. Only mark complete when build passes'
]
```

## Orchestrator Responsibilities

The orchestrator MUST:

1. **Include build verification in agent prompts**
```python
"Implement the user authentication module.
IMPORTANT: You must compile and verify your code works before marking complete.
Use './gradlew build' for Android, 'npm run build' for web, etc."
```

2. **Check for build failures in agent results**
```python
if "build failed" in agent_result or "compilation error" in agent_result:
    # Spawn new agent to fix
    # Do NOT proceed to review
```

3. **Enforce iteration until builds pass**
```python
while not build_passing:
    spawn_agent_to_fix()
    check_build_status()
```

## The Iteration Pattern

### WRONG (What happened):
```
Develop → Review (finds issues) → Orchestrator gives up → Manual fix
```

### RIGHT (What should happen):
```
Develop → Build Fails → Fix → Build Passes → Review → Issues Found
    ↑                                                         ↓
    └──────────────────── Fix Issues ←───────────────────────┘
```

## Build Verification Checklist

Before ANY story/feature can be marked complete:

- [ ] All code compiles without errors
- [ ] All code runs without crashes
- [ ] Basic smoke tests pass
- [ ] Adversarial review passes
- [ ] Final build verification passes

## Script Integration

### Add to 03_adversarial_workflow.py:
```python
def verify_build(self, story_id):
    """
    Verify code actually builds before review

    Returns:
        - build_status: pass/fail
        - errors: List of compilation errors
    """
    # Detect project type
    # Run appropriate build command
    # Return results
```

### Add to task_aggregator.py:
```python
def aggregate_build_results(self, agent_ids):
    """
    Combine build verification results

    Returns:
        - overall_build_status
        - failing_components
        - error_summary
    """
```

## Platform Detection

Agents should detect and use the right build command:

```python
def detect_build_command():
    """Auto-detect build system"""

    if Path('gradlew').exists():
        return './gradlew build'
    elif Path('package.json').exists():
        return 'npm run build'
    elif Path('Makefile').exists():
        return 'make'
    elif Path('go.mod').exists():
        return 'go build ./...'
    elif Path('Cargo.toml').exists():
        return 'cargo build'
    elif Path('pom.xml').exists():
        return 'mvn compile'
    else:
        return None
```

## Enforcement in Orchestrator Prompt

Add to 00_Prompt.md:

"CRITICAL: Every developer agent MUST verify their code compiles before marking complete. If an agent reports compilation errors, spawn a new agent to fix them. NEVER proceed to review with broken code."

## Common Build Commands Reference

| Platform | Build Command | Test Command |
|----------|--------------|--------------|
| Android | `./gradlew build` | `./gradlew test` |
| iOS | `xcodebuild` | `xcodebuild test` |
| React | `npm run build` | `npm test` |
| Vue | `npm run build` | `npm run test` |
| Angular | `ng build` | `ng test` |
| Python | `python -m py_compile` | `pytest` |
| Go | `go build ./...` | `go test ./...` |
| Rust | `cargo build` | `cargo test` |
| Java | `mvn compile` | `mvn test` |
| .NET | `dotnet build` | `dotnet test` |

## The Golden Rule

**"If it doesn't compile, it's not done."**

No exceptions. No shortcuts. No manual fixes outside the workflow.