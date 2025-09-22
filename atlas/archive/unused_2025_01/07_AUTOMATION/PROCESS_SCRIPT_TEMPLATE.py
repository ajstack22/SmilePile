#!/usr/bin/env python3
"""
Atlas [Process Name] Process Script
[Brief description of what this process accomplishes]
Usage: python [process_name].py [options] --claude
"""

import sys
import json
from pathlib import Path
from datetime import datetime

class [ProcessName]Process:
    """
    Implements the [Process Name] process as executable prompts for Claude Code.
    """

    def __init__(self, context=None):
        """Initialize the process with optional context"""
        self.context = context or {}
        self.state_dir = Path('.atlas/[process_name]')
        self.state_dir.mkdir(parents=True, exist_ok=True)
        self.phases = self.define_phases()

    def define_phases(self):
        """Define the phases of this process"""
        return [
            'assessment',     # Understand current state
            'planning',       # Plan the approach
            'execution',      # Execute the plan
            'verification',   # Verify success
            'documentation'   # Document outcomes
        ]

    def generate_assessment_phase(self):
        """Phase 1: Assess the current state"""
        return f"""
===== [PROCESS NAME]: PHASE 1 - ASSESSMENT =====

Objective: Understand the current state and identify what needs to be done.

Use the TodoWrite tool immediately to track these assessment tasks:
1. Analyze current situation
2. Identify stakeholders
3. Gather requirements
4. Document constraints
5. Define success criteria

ASSESSMENT ACTIVITIES:

Use the Task tool with general-purpose agents IN PARALLEL to:
1. Research existing patterns in the codebase
2. Identify similar past implementations
3. Check for potential conflicts
4. Review relevant documentation

Research extensively using Grep and Glob to find:
- Related code patterns
- Configuration files
- Documentation
- Test cases

Think very carefully about:
- What problem are we solving?
- Who is affected?
- What are the risks?
- What defines success?

DELIVERABLES:
- Current state analysis
- Requirements document
- Success criteria
- Risk assessment
"""

    def generate_planning_phase(self):
        """Phase 2: Plan the approach"""
        return f"""
===== [PROCESS NAME]: PHASE 2 - PLANNING =====

Objective: Create a detailed plan for executing this process.

Use the TodoWrite tool to track planning tasks:
1. Define approach
2. Break down into steps
3. Identify dependencies
4. Estimate effort
5. Create timeline

PLANNING CONSIDERATIONS:

Think carefully about:
- What's the optimal sequence of steps?
- What could go wrong?
- What resources are needed?
- What are the checkpoints?

Create a detailed plan including:
- Step-by-step tasks
- Dependencies between tasks
- Risk mitigation strategies
- Rollback procedures
- Success metrics

DELIVERABLES:
- Detailed task list
- Dependency map
- Timeline
- Risk mitigation plan
"""

    def generate_execution_phase(self):
        """Phase 3: Execute the plan"""
        return f"""
===== [PROCESS NAME]: PHASE 3 - EXECUTION =====

Objective: Execute the plan systematically with evidence collection.

Use the TodoWrite tool to track execution:
1. Execute each planned step
2. Document progress
3. Collect evidence
4. Handle issues as they arise
5. Update stakeholders

EXECUTION PROTOCOL:

For each task in the plan:
1. Mark as in_progress in TodoWrite
2. Execute the task
3. Collect evidence (screenshots, logs, metrics)
4. Verify completion
5. Mark as completed
6. Document any deviations

Run quality checks IN PARALLEL:
- Test affected functionality
- Verify no regressions
- Check performance impact
- Validate security posture

Think carefully about:
- Is this going according to plan?
- What adjustments are needed?
- Are we meeting success criteria?

EVIDENCE REQUIRED:
- Before/after comparisons
- Test results
- Performance metrics
- Stakeholder approval
"""

    def generate_verification_phase(self):
        """Phase 4: Verify success"""
        return f"""
===== [PROCESS NAME]: PHASE 4 - VERIFICATION =====

Objective: Verify that the process achieved its goals.

Use the TodoWrite tool to track verification:
1. Check all success criteria
2. Run acceptance tests
3. Verify no regressions
4. Confirm stakeholder satisfaction
5. Document results

VERIFICATION CHECKLIST:

Success Criteria Verification:
□ [Criterion 1] - [How to verify]
□ [Criterion 2] - [How to verify]
□ [Criterion 3] - [How to verify]

Quality Gates:
□ All tests passing
□ No performance degradation
□ Security scans clean
□ Documentation updated
□ Stakeholder sign-off

Think very carefully:
- Have we fully achieved our goals?
- Are there any loose ends?
- What follow-up is needed?

DELIVERABLES:
- Verification report
- Test results
- Metrics comparison
- Stakeholder feedback
"""

    def generate_documentation_phase(self):
        """Phase 5: Document outcomes"""
        return f"""
===== [PROCESS NAME]: PHASE 5 - DOCUMENTATION =====

Objective: Document the process execution for future reference.

Use the TodoWrite tool to track documentation:
1. Create summary report
2. Document lessons learned
3. Update relevant documentation
4. Archive evidence
5. Close out process

DOCUMENTATION REQUIREMENTS:

Summary Report:
- What was accomplished
- How it was done
- Challenges encountered
- Solutions implemented
- Metrics and outcomes

Lessons Learned:
- What went well
- What could be improved
- Recommendations for next time
- Process improvements identified

Updates Needed:
- README updates
- API documentation
- Architecture diagrams
- Runbooks
- Team knowledge base

Think carefully about:
- What would help someone doing this next time?
- What knowledge should be preserved?
- How can the process be improved?

FINAL DELIVERABLES:
- Process execution report
- Updated documentation
- Lessons learned document
- Archived evidence
- Process improvement recommendations
"""

    def generate_complete_process(self):
        """Generate the complete process for Claude to execute"""
        output = [
            f"===== [PROCESS NAME] PROCESS =====",
            f"Context: {json.dumps(self.context, indent=2)}",
            "",
            "You will now execute the [Process Name] process through 5 systematic phases.",
            "Each phase builds on the previous one. Complete each thoroughly before proceeding.",
            "",
            "IMPORTANT INSTRUCTIONS:",
            "1. Use TodoWrite immediately to track all phases and tasks",
            "2. Use Task tool for parallel research and analysis",
            "3. Document all decisions and evidence",
            "4. Think carefully at each decision point",
            "5. Follow the process systematically",
            "",
            "="*70,
            ""
        ]

        # Add all phases
        generators = [
            self.generate_assessment_phase,
            self.generate_planning_phase,
            self.generate_execution_phase,
            self.generate_verification_phase,
            self.generate_documentation_phase
        ]

        for i, generator in enumerate(generators, 1):
            output.append(generator())
            if i < len(generators):
                output.append("")
                output.append("="*70)
                output.append("")
                output.append(f"After completing Phase {i}, continue to Phase {i+1}.")
                output.append("")
                output.append("="*70)
                output.append("")

        output.append("")
        output.append("="*70)
        output.append("PROCESS COMPLETE")
        output.append("")
        output.append("Provide a final summary including:")
        output.append("1. What was accomplished")
        output.append("2. Key decisions made")
        output.append("3. Lessons learned")
        output.append("4. Follow-up actions needed")

        return '\n'.join(output)

    def save_state(self, phase, status, data=None):
        """Save process state for continuity"""
        state = {
            'phase': phase,
            'status': status,
            'timestamp': datetime.now().isoformat(),
            'context': self.context,
            'data': data or {}
        }
        state_file = self.state_dir / 'state.json'
        with open(state_file, 'w') as f:
            json.dump(state, f, indent=2)

    def run(self, mode='claude', phase=None):
        """Execute the process"""
        if mode == 'claude':
            # Generate complete process for Claude
            print(self.generate_complete_process())
            self.save_state('assessment', 'started')
        elif phase:
            # Run specific phase
            phase_generators = {
                1: self.generate_assessment_phase,
                2: self.generate_planning_phase,
                3: self.generate_execution_phase,
                4: self.generate_verification_phase,
                5: self.generate_documentation_phase
            }
            if phase in phase_generators:
                print(phase_generators[phase]())
                self.save_state(self.phases[phase-1], 'in_progress')
            else:
                print(f"Invalid phase: {phase}. Use 1-5.")
        else:
            # Show usage
            print(f"===== [PROCESS NAME] PROCESS =====")
            print(f"")
            print(f"This process helps [what it does]")
            print(f"")
            print(f"Usage:")
            print(f"  python {sys.argv[0]} --claude     # Run complete process")
            print(f"  python {sys.argv[0]} --phase N    # Run specific phase (1-5)")
            print(f"")
            print(f"Phases:")
            for i, phase in enumerate(self.phases, 1):
                print(f"  {i}. {phase.title()}")

def main():
    """Main entry point"""
    # Parse arguments
    mode = 'interactive'
    phase = None
    context = {}

    if '--claude' in sys.argv:
        mode = 'claude'
    elif '--phase' in sys.argv:
        idx = sys.argv.index('--phase')
        if idx + 1 < len(sys.argv):
            phase = int(sys.argv[idx + 1])

    # You can add context parsing here
    # For example: --context key=value

    # Initialize and run process
    process = [ProcessName]Process(context)
    process.run(mode, phase)

if __name__ == "__main__":
    main()