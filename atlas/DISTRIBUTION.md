# Atlas Framework Distribution Guide

## What to Copy

When setting up Atlas in a new project, copy these directories:

### 1. Atlas Framework
```bash
cp -r SmilePile/atlas /path/to/your/project/
```

### 2. Claude Code Agents
```bash
cp -r SmilePile/.claude /path/to/your/project/
```

**Important**: The `.claude/agents/` directory contains the active agent definitions that Claude Code uses. The `atlas/` directory contains documentation and workflow guides.

---

## Project-Specific Customization

After copying, customize these files for your project:

### Required: Create deployment script
Atlas requires a quality deployment script at `./deploy/deploy_qual.sh` (or equivalent).

**Minimum requirements:**
```bash
#!/bin/bash
# Your project's deploy_qual.sh

# 1. Run tests (NEVER skip without user approval)
# 2. Run static analysis (linters, SonarCloud, etc.)
# 3. Build all platforms
# 4. Deploy to local devices/emulators
# 5. Create git commits with proper messages
```

See your project's deployment documentation for specifics.

### Optional: Update story location
Atlas references a story directory. Update if your project uses a different convention:

Default: `backlog/sprint-X/STORY-X.Y-name.md`

Common alternatives:
- `stories/STORY-X.Y-name.md`
- `docs/stories/STORY-X.Y-name.md`
- `.atlas/stories/STORY-X.Y-name.md`

Update references in:
- `atlas/docs/AGENT_WORKFLOW.md`
- Your project's `.claude/agents/*.md` (if they reference story locations)

### Optional: Customize agents
Edit `.claude/agents/*.md` to add project-specific context:
- Project structure
- Coding standards
- Technology stack
- Testing requirements
- Deployment process

---

## Verify Setup

After copying, verify your setup:

### 1. Test Quick Workflow
```
"Change a comment in any file. Use Atlas Quick workflow."
```

Should complete in < 15 minutes with deployment.

### 2. Test Standard Workflow
```
"Add a simple utility function. Use Atlas Standard workflow."
```

Should complete in 30-60 minutes.

### 3. Verify Deployment Script
Ensure `./deploy/deploy_qual.sh` exists and:
- ✅ Runs tests
- ✅ Enforces quality gates
- ✅ Deploys successfully
- ✅ Creates proper git commits

---

## Structure After Setup

Your project should have:

```
your-project/
├── .claude/
│   └── agents/           # Active agent definitions
│       ├── developer.md
│       ├── devops.md
│       ├── peer-reviewer.md
│       ├── product-manager.md
│       └── security.md
├── atlas/
│   ├── docs/             # Workflow documentation
│   ├── templates/        # Story/PR templates
│   ├── examples/         # Reference examples
│   └── core/             # Legacy scripts (optional)
├── deploy/
│   └── deploy_qual.sh    # Your deployment script
└── backlog/              # Or your story directory
    └── sprint-X/
```

---

## What's NOT Included

These are project-specific and must be created by you:

### 1. Deployment Script (`deploy_qual.sh`)
Each project has unique deployment needs. Create your own script that enforces:
- Test execution
- Quality gates
- Build processes
- Git workflow

### 2. Story Directory Structure
Choose your own convention for organizing stories and documentation.

### 3. Agent Customizations
While base agents are provided, customize them for:
- Your tech stack
- Your coding standards
- Your deployment process
- Your project structure

---

## Migration from Existing Projects

If you already have a development process:

### Keep What Works
Atlas is designed to augment, not replace. Keep:
- Your existing deployment scripts
- Your story/ticket system
- Your testing infrastructure
- Your documentation standards

### Add Atlas Layers
Use Atlas to add:
- Structured workflow guidance
- Agent-driven development
- Evidence-based progress tracking
- Workflow tier selection (Quick/Standard/Full)

### Integration Points
Atlas integrates with:
- Any git workflow
- Any CI/CD system (via your deploy script)
- Any issue tracking (via story templates)
- Any testing framework (via deploy script)

---

## Getting Help

If you run into issues:

1. **Check Examples**: `atlas/examples/smilepile-migration/` shows real-world usage
2. **Read Tiers Guide**: `atlas/docs/WORKFLOW_TIERS.md` for workflow selection
3. **Review Workflow**: `atlas/docs/AGENT_WORKFLOW.md` for full 9-phase details
4. **Check Agents**: `.claude/agents/*.md` for agent capabilities

---

## Success Criteria

You've successfully set up Atlas when:

- ✅ Quick workflow completes in < 15 min
- ✅ Standard workflow completes in 30-60 min
- ✅ Full workflow completes in 2-4 hours
- ✅ Deployment script enforces quality gates
- ✅ Tests run automatically before deploy
- ✅ Stories are created and tracked
- ✅ Zero defects escape to production

---

## License

MIT - Use it, modify it, share what works.
