# Atlas Framework

The unified development workflow framework. No versions, no confusion - just the definitive way to build great software.

## Quick Start

1. **Initialize a workflow**: `python atlas.py workflow start F001`
2. **Submit for review**: `python atlas.py review submit F001`
3. **Check status**: `python atlas.py workflow status`

## Structure

- `workflows/` - All workflow documentation
- `automation/` - All Python scripts and tools
- `templates/` - All templates and checklists
- `metrics/` - Metrics and dashboards
- `agents/` - Agent specifications
- `standards/` - Standards and agreements
- `roles/` - Role definitions
- `integrations/` - External integrations

## The Atlas Way

Atlas provides **one clear path** for each task:

- **Workflow**: Follow `workflows/ATLAS_WORKFLOW.md`
- **Review**: Use `workflows/REVIEW_PROCESS.md`
- **Validation**: Run `python atlas.py validate`
- **Metrics**: Check `python atlas.py metrics`

No "enhanced" versions, no alternatives - just the right way to do things.

## Migration from Previous Versions

If you're migrating from an older Atlas version, run:
```bash
python migrate_to_atlas_2_1.py --backup
```

This will consolidate everything into the unified structure.
