# CLAUDE.md Preamble - Atlas Framework Integration

## ⚠️ CRITICAL: Add This Section to the TOP of Every Project's CLAUDE.md File

Copy the section below to the beginning of your project's CLAUDE.md file to ensure Atlas Framework is recognized and used from the first interaction.

---

# 🚀 ATLAS FRAMEWORK PROJECT - CONTEXT INJECTION ACTIVE

## IMPORTANT: This Project Uses Atlas Framework 2.2

This project is equipped with the **Atlas Framework** - an intelligent development workflow system with **automatic context injection** for all development tasks.

### How Atlas Works Here

1. **Automatic Context Detection**: When you spawn agents or work on tasks, the Atlas system automatically provides the right documentation and context based on the task type.

2. **Zero Manual Steps**: You don't need to read files or load context manually - it's injected automatically when you use the Task tool.

3. **Intelligent Task Recognition**: The system detects what you're doing (story creation, bug fixing, development, etc.) and provides appropriate context.

### Atlas Directory Location
```
./atlas/                    # Atlas Framework root
├── 01_CORE/               # Core standards and prompts
├── 02_WORKFLOWS/          # Development workflows
├── 03_AGENTS/             # Agent specializations
├── 07_AUTOMATION/         # Automation scripts including context injector
├── 09_STORIES/            # Project stories and requirements
└── config/                # Context manifests and mappings
```

### Primary Development Workflow

For ALL development tasks, follow this process:

1. **Story First**: Every change starts with a story
   - Bug fixes need bug stories
   - Features need feature stories
   - Technical debt needs debt stories

2. **Use Atlas Prompts**: Find ready-to-use prompts in `atlas/01_CORE/PROMPTS.md`

3. **Natural Agent Spawning**: Just use Task() normally - context is auto-injected
   ```python
   Task("Create story for [feature]", details, "story-writer")  # Auto-gets story context
   Task("Fix [bug description]", details, "bug-fixer")         # Auto-gets debug context
   Task("Implement [story]", details, "general-purpose")       # Auto-detects needed context
   ```

### Quick Command Reference

**Validate Atlas System**:
```bash
python3 atlas/07_AUTOMATION/task_context_integration.py validate
```

**Test Context Injection**:
```bash
python3 atlas/test_context_injection.py
```

**Check Available Profiles**:
```bash
python3 atlas/07_AUTOMATION/enhanced_context_injector.py list
```

### Context Profiles Available

The following context profiles are automatically selected based on task:

- `story_creation` - Requirements, user stories, epics
- `troubleshooting` - Bug fixing, debugging, error resolution
- `ui_development` - Frontend, components, UX
- `backend_development` - APIs, services, databases
- `testing` - Unit tests, integration tests, E2E
- `code_review` - Quality assessment, feedback
- `deployment` - Releases, rollouts, DevOps
- `documentation` - Guides, API docs, README
- `research` - Investigation, analysis, feasibility
- `orchestration` - Multi-agent coordination

### Feature Modifiers

Automatically detected from task descriptions:
- **Authentication**: auth, login, oauth, jwt, password
- **Performance**: optimize, slow, speed, latency
- **Database**: database, sql, migration, schema
- **Security**: security, encryption, vulnerability
- **API**: api, rest, graphql, endpoint

### Working with Stories

**Find Stories**:
```bash
ls atlas/09_STORIES/features/  # Feature stories
ls atlas/09_STORIES/bugs/      # Bug stories
```

**Story Naming Convention**:
- Features: `ATLAS-XXX-description.md`
- Bugs: `BUG-XXX-description.md`
- Epics: `EPIC-XXX-description.md`

### Development Commands

**Full Feature Delivery** (copy from `atlas/01_CORE/PROMPTS.md`):
```
Deliver the [FEATURE] feature end-to-end using the full Atlas workflow:
Story: atlas/09_STORIES/features/[STORY-FILE]
Execute all phases: Research → Story Creation → Development → Review → Deploy-Dev
```

### Critical Context Files

These files contain essential project context:
- `atlas/README.md` - Atlas framework overview
- `atlas/01_CORE/PROMPTS.md` - Copy-paste development prompts
- `atlas/02_WORKFLOWS/00_ORCHESTRATION_PROCESS.md` - How to coordinate work
- `atlas/config/context_manifest.json` - Context configuration
- `atlas/config/agent_context_mapping.yaml` - Agent type mappings

### Performance Expectations

- Context injection: <500ms per agent
- Automatic profile selection: 95% accuracy
- Token efficiency: 50% reduction vs manual
- Cache hit rate: >80% for repeated tasks

### Troubleshooting

If context isn't being injected:
1. Run validation: `python3 atlas/07_AUTOMATION/task_context_integration.py validate`
2. Check agent type is recognized
3. Verify Atlas directory exists at `./atlas/`

### DO NOT SKIP ATLAS

⚠️ **IMPORTANT**: This project's quality and velocity depend on Atlas Framework. Always:
- Start with stories
- Use Atlas workflows
- Let context injection work automatically
- Follow the established patterns

The Atlas Framework is here to make development faster and more consistent. Trust the process.

---

## Project-Specific Information

[Continue with your project-specific CLAUDE.md content below...]

---

# Additional CLAUDE.md Integration Tips

## For Project Maintainers

### Initial Setup (One Time)

After copying Atlas to your project:

1. **Copy Atlas Framework**:
```bash
cp -r /path/to/atlas ./atlas
```

2. **Add to .gitignore** (optional, if you don't want Atlas in repo):
```bash
echo "atlas/" >> .gitignore
```

3. **Create/Update CLAUDE.md**:
```bash
# If CLAUDE.md doesn't exist
cp atlas/01_CORE/CLAUDE_MD_PREAMBLE.md CLAUDE.md

# If CLAUDE.md exists
cat atlas/01_CORE/CLAUDE_MD_PREAMBLE.md > CLAUDE.md.tmp
cat CLAUDE.md >> CLAUDE.md.tmp
mv CLAUDE.md.tmp CLAUDE.md
```

4. **Validate Installation**:
```bash
python3 atlas/test_context_injection.py
```

### Customization Points

You can customize Atlas for your project by editing:

1. **Context Manifest** (`atlas/config/context_manifest.json`):
   - Add project-specific documentation to profiles
   - Adjust token budgets
   - Add new profiles for your domain

2. **Agent Mappings** (`atlas/config/agent_context_mapping.yaml`):
   - Add custom agent types
   - Modify keyword detection rules
   - Add project-specific features

3. **Project Stories** (`atlas/09_STORIES/`):
   - Maintain your project's stories here
   - Follow the naming convention
   - Keep stories up-to-date

### Best Practices

1. **Every Session Starts with Validation**:
   - First command should validate Atlas is working
   - Ensures context injection is operational

2. **Story-Driven Development**:
   - No code without a story
   - Stories provide context for all agents

3. **Trust Automatic Context**:
   - Don't manually specify files to read
   - Let the system detect and inject

4. **Use Specialized Agents**:
   - `story-writer` for requirements
   - `bug-fixer` for issues
   - `ui-developer` for frontend
   - `backend-developer` for APIs
   - Let general-purpose detect from description

### Monitoring Atlas Performance

Track these metrics weekly:
- Story completion rate
- Bug fix turnaround time
- Test coverage trends
- Context injection accuracy
- Agent success rates

### Updating Atlas

To update to a newer Atlas version:
```bash
# Backup current
mv atlas atlas.backup

# Copy new version
cp -r /path/to/new/atlas ./atlas

# Merge custom configurations
cp atlas.backup/config/*.json atlas/config/
cp atlas.backup/config/*.yaml atlas/config/
cp -r atlas.backup/09_STORIES/* atlas/09_STORIES/

# Validate
python3 atlas/test_context_injection.py
```

---

## Quick Checklist for New Projects

- [ ] Atlas directory copied to project root
- [ ] CLAUDE.md updated with Atlas preamble
- [ ] Validation script runs successfully
- [ ] First story created in atlas/09_STORIES
- [ ] Team briefed on Atlas workflow
- [ ] Context injection tested with sample task
- [ ] Project-specific customizations documented

Remember: Atlas is designed to make development faster and more consistent. The initial setup time is repaid many times over through improved velocity and quality.