# Atlas Process-Script Alignment

## Complete Process-Script Mapping

Every process has a script, and every script implements a process. The numbering reflects the natural development workflow order.

| # | Process | Script | Purpose |
|---|---------|--------|---------|
| 00 | ORCHESTRATION_PROCESS | orchestrator_context.py | Meta-process: coordinates all others |
| 01 | RESEARCH_PROCESS | research.py | Investigate before building |
| 02 | STORY_CREATION_PROCESS | create_story.py | Define requirements from research |
| 03 | ADVERSARIAL_WORKFLOW | adversarial_workflow.py | Build with quality gates |
| 04 | RELEASE_DEPLOYMENT_PROCESS | release_deployment.py | Deploy to production |
| 05 | TROUBLESHOOTING_PROCESS | troubleshoot.py | Fix production issues |
| 06 | REPOSITORY_UPDATE_PROCESS | update_repository.py | Documentation & presentation |

## Natural Development Flow

The numbering follows the logical sequence of software development:

```
00_ORCHESTRATION (Always Active - Coordinates Everything)
        |
        ↓
01_RESEARCH → 02_STORY_CREATION → 03_ADVERSARIAL_WORKFLOW → 04_RELEASE_DEPLOYMENT
                                                                        |
                                                                        ↓
                                                            05_TROUBLESHOOTING (if issues)
                                                                        |
                                                                        ↓
                                                            06_REPOSITORY_UPDATE
```

## Directory Structure

```
atlas/
├── 03_PROCESSES/                          # Process documentation
│   ├── 00_ORCHESTRATION_PROCESS.md        # Meta-coordination process
│   ├── 01_RESEARCH_PROCESS.md             # Investigation process
│   ├── 02_STORY_CREATION_PROCESS.md       # Requirements process
│   ├── 03_ADVERSARIAL_WORKFLOW.md         # Development process
│   ├── 04_RELEASE_DEPLOYMENT_PROCESS.md   # Deployment process
│   ├── 05_TROUBLESHOOTING_PROCESS.md      # Debugging process
│   └── 06_REPOSITORY_UPDATE_PROCESS.md    # Documentation process
│
└── 07_SCRIPTS_AND_AUTOMATION/             # Executable scripts
    ├── 00_orchestrator_context.py         # Implements orchestration
    ├── 01_research.py                      # Implements research
    ├── 02_create_story.py                  # Implements story creation
    ├── 03_adversarial_workflow.py          # Implements development
    ├── 04_release_deployment.py            # Implements deployment
    ├── 05_troubleshoot.py                  # Implements troubleshooting
    └── 06_update_repository.py             # Implements documentation
```

## The Pattern

Each process document (`03_PROCESSES/`) contains:
- **Overview**: What the process accomplishes
- **When to Use**: Trigger conditions
- **Process Script**: Which script implements it
- **Process Owner**: Which role executes it
- **Phases**: Step-by-step process
- **Script Details**: Command usage and options
- **Success Metrics**: How to measure effectiveness
- **Integration Points**: How it connects to other processes

Each script (`07_SCRIPTS_AND_AUTOMATION/`) generates:
- Structured prompts for Claude Code
- Role-specific instructions
- Phase-based execution
- State management
- Evidence requirements
- Success criteria

## Key Principles

### 1. Process = Script
Every process is implemented as an executable script. No process exists without automation.

### 2. Orchestrator Coordinates
The Orchestrator never implements directly. All work is delegated to specialized agents.

### 3. State Persistence
All processes maintain state for continuity across sessions.

### 4. Evidence-Based
Every process requires evidence of completion at each phase.

### 5. Quality Gates
Processes enforce quality through mandatory checkpoints.

## Usage Examples

### Starting a New Feature (Following Process Order)
```bash
# 00. Start orchestration (optional for simple features)
python 00_orchestrator_context.py new

# 01. Research the feature space
python 01_research.py "feature requirements" --quick

# 02. Create the story based on research
python 02_create_story.py feature

# 03. Execute development workflow
python 03_adversarial_workflow.py stories/S001.md --claude

# 04. Release to production
python 04_release_deployment.py --type minor --claude

# 05. Troubleshoot if issues arise
python 05_troubleshoot.py bugs/B001.md --claude

# 06. Update documentation
python 06_update_repository.py --claude
```

### Fixing a Production Bug
```bash
# 00. Resume orchestration
python 00_orchestrator_context.py resume

# 02. Create bug report
python 02_create_story.py bug

# 05. Troubleshoot systematically
python 05_troubleshoot.py bugs/B001.md --claude

# 03. Review fix through adversarial workflow
python 03_adversarial_workflow.py stories/B001-fix.md --claude

# 04. Deploy hotfix
python 04_release_deployment.py --hotfix
```

### Major Initiative (Long-Running)
```bash
# Week 1: Research and Planning
# 00. Start orchestration
python 00_orchestrator_context.py new
python 00_orchestrator_context.py objective "Migrate to microservices"

# 01. Deep research
python 01_research.py "microservices migration" --full

# 02. Create epic and stories
python 02_create_story.py epic

# Week 2-4: Implementation
# 00. Continue orchestration
python 00_orchestrator_context.py resume

# 03. Multiple development cycles
python 03_adversarial_workflow.py stories/S001.md --claude
python 03_adversarial_workflow.py stories/S002.md --claude

# 04. Phased releases
python 04_release_deployment.py --type minor --claude

# Week 5: Polish
# 06. Update all documentation
python 06_update_repository.py --claude
```

## The Power of Alignment

Traditional approach:
- Processes are documents
- Humans interpret differently
- Quality varies by individual
- Knowledge lost when people leave

Atlas approach:
- Processes are code
- Claude executes consistently
- Quality is guaranteed
- Knowledge persists forever

This alignment means:
- **100% process compliance** - Can't skip steps
- **100% evidence collection** - Automatically captured
- **100% continuity** - State preserved
- **100% scalability** - Unlimited parallel execution

Every process is not just documented but executable, not just planned but enforceable, not just theoretical but practical.