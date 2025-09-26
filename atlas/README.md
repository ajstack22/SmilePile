# Atlas Lite - The Pragmatic Edition

## What This Is

Atlas Lite is a **streamlined, battle-tested version** of the Atlas Framework, distilled from 7+ successful production waves. It contains only the essential components that actually get used - about 20% of the original codebase that delivers 100% of the value.

## The Philosophy

After extensive real-world usage, we discovered that success comes not from complex automation, but from:
- **Simple, enforceable workflows**
- **Pragmatic bash orchestration**
- **Evidence-driven development**
- **Parallel execution patterns**
- **Choosing practical over perfect**

## What's Included

### Core Workflow Scripts (The Foundation)
```
core/
├── atlas_workflow.py      # Main 7-phase workflow enforcer (LEGACY)
├── atlas_research.py      # Phase 1: Research guide (LEGACY)
├── atlas_story.py         # Phase 2: Story creation (LEGACY)
├── atlas_adversarial.py   # Phase 4: Quality review (LEGACY)
└── atlas_checkpoint.py    # Checkpoint management (LEGACY)
```
**Note**: Python scripts are maintained for reference but superseded by agent-driven workflow.

### Agent Definitions (The Intelligence) 🆕
```
agents/
├── researcher.md              # Deep codebase exploration agent
├── product_manager.md         # Story creation & validation agent
├── developer.md               # Planning & implementation agent
├── peer_reviewer.md           # Adversarial review agent
├── qa_tester.md              # Testing & verification agent
├── organizer.md              # Clean-up & documentation agent
└── orchestration_examples.md # Coordination patterns & templates
```

### Templates (The Structure)
```
templates/
├── STORY_TEMPLATE.md           # User story format
├── BUG_REPORT_TEMPLATE.md      # Bug tracking
├── PULL_REQUEST_TEMPLATE.md    # PR standards
└── 01_EVIDENCE_TEMPLATES.md    # Evidence patterns
```

### Documentation (The Wisdom)
```
docs/
├── AGENT_WORKFLOW.md          # Complete agent orchestration guide 🆕
├── WORKFLOW_USAGE.md          # How to use the workflow
├── ATLAS_SUCCESS_PATTERNS.md  # Proven patterns from production
└── README.md                  # This file
```

### Examples (The Implementation)
```
examples/
└── wave-orchestration-example.sh  # Real production orchestration script
```

## Quick Start

### Agent-Driven Workflow (Recommended) 🆕
```
"I need to [TASK DESCRIPTION]. Please orchestrate the Atlas agent workflow,
launching appropriate agents for each phase. Execute in parallel where possible."
```

This orchestrates:
1. **Research** - Parallel agent exploration
2. **Story creation** - Product manager agent defines requirements
3. **Planning** - Developer agents design approach
4. **Adversarial review** - Peer reviewer finds edge cases
5. **Implementation** - Parallel developer agents code
6. **Testing** - QA and review agents verify
7. **Validation** - Product manager confirms criteria
8. **Clean-up** - Organizer agent maintains hygiene 🆕

### Legacy Script Workflow (Reference)
```bash
python3 core/atlas_workflow.py feature "Your task description"
```

## Key Success Patterns

### 1. Parallel Execution with Agents 🆕
Launch multiple agents simultaneously for independent tasks:
```
PARALLEL:
- Researcher Agent 1: Search UI components
- Researcher Agent 2: Search backend logic
- Developer Agent 1: Implement frontend
- Developer Agent 2: Implement backend
```
Result: 3-5x speed improvement through intelligent parallelization

### 2. Evidence-Driven Development
Create evidence throughout, not just at the end:
```
wave-evidence/
├── research-phase.md      # What you discovered
├── implementation-log.md  # What you changed
├── validation-report.md   # What you verified
└── final-report.md       # Metrics & lessons
```

### 3. Pragmatic Over Perfect
- **30% test coverage** (not 80%) - covers critical paths
- **3 smoke tests** (not 50 edge cases) - real user flows
- **Simple bash scripts** (not complex orchestrators)
- **Skip options** for emergencies

### 4. Component Size Limits
- **No file > 250 lines** (hard limit)
- **Target: 150-200 lines** (sweet spot)
- **Extract at 60% duplication** (not 100%)

## What We Removed (And Why)

### ❌ 44+ Python Automation Scripts
- Complex state machines → Simple bash works better
- Web dashboards → Markdown reports are sufficient
- Trust scoring systems → Unnecessary complexity
- Dependency graphs → Manual planning is faster

### ❌ Unused Directories
- 03_AGENTS, 04_METRICS, 08_INTEGRATIONS → Never referenced
- 07_AUTOMATION → 44 scripts that weren't being used

### Why Removal Improved Things
The original Atlas had sophisticated automation that looked impressive but added friction. The streamlined version focuses on what actually gets used daily.

## Real-World Results

From 7 production waves:
- **Wave 1**: 8 hours (vs 24 hours traditional)
- **Zero rollbacks** across all waves
- **100% acceptance criteria** met
- **Decreasing bug discovery** with each wave

## Agent Integration for Claude Code 🆕

### Keeping It Simple
The agent workflow is powerful but must be practical within Claude Code's context limits:

**DO**:
- Use 2-3 agents in parallel maximum per phase
- Keep agent prompts concise and focused
- Save key outputs between phases

**DON'T**:
- Launch 10+ agents simultaneously
- Create massive context hand-offs
- Over-orchestrate simple tasks

### Practical Agent Usage
For most tasks, this simple pattern works best:
```
Phase 1: One researcher agent (comprehensive search)
Phase 2-4: Sequential single agents (story, plan, review)
Phase 5: 2-3 parallel developer agents (split by component)
Phase 6-8: Sequential single agents (test, validate, clean)
```

## The Core Insight

> "The Atlas system succeeds not because of rigid process, but because it combines structure with pragmatism, documentation with action, and sequential phases with parallel execution."

## Migration Guide

### From Original Atlas:
1. Archive your `07_AUTOMATION/` directory
2. Replace with these streamlined scripts
3. Keep your existing stories and evidence
4. Continue using simple bash orchestration

### From Scratch:
1. Start with `atlas_workflow.py`
2. Follow the 7 phases religiously
3. Create evidence as you go
4. Use bash for orchestration

## Best Practices

### DO ✅
- Use `atlas_workflow.py` for everything
- Create evidence throughout the process
- Run parallel tasks when possible
- Keep components under 250 lines
- Choose pragmatic solutions

### DON'T ❌
- Skip phases to save time
- Batch evidence at the end
- Over-engineer the automation
- Create monolithic components
- Pursue perfection over progress

## FAQ

**Q: Why remove so much automation?**
A: It wasn't being used. Simple bash scripts achieved better results.

**Q: Is this production-ready?**
A: Yes. This exact setup has been used for 7+ successful production waves.

**Q: Can I add back complexity?**
A: You can, but ask yourself: is it solving a real problem you're having?

**Q: How do I handle [complex scenario]?**
A: Start simple. Use the workflow. Add complexity only when simple doesn't work.

## Contributing

The best contributions:
- Simplify existing patterns
- Document what works
- Remove what doesn't
- Share evidence of success

## License

MIT - Use it, modify it, share what works.

## The Atlas Lite Promise

**20% of the code. 100% of the value. 0% of the bloat.**

---

*"Perfection is achieved not when there is nothing more to add, but when there is nothing left to take away."*

*Atlas Lite: Accidentally proving that less is more.*