# Atlas Core Prompts - Copy & Paste Templates

## Quick Reference
Copy these prompts directly. Replace `[PLACEHOLDERS]` with your specific details.

---

## 🚨 OPERATOR MODE - Automated Workflows

### Fix a Bug (Fully Automated with Context)
```bash
# This command automatically creates story, injects context, and guides the fix
python atlas/fix_bug.py "App crashes when clicking import button"
```

### Create Feature Story (Automated)
```bash
# This command automatically creates story, injects context, and guides implementation
python3 atlas/create_feature.py "Add dark mode toggle to settings"
```

These operator scripts automatically:
- Create properly formatted Atlas stories
- Inject relevant context and checklists
- Provide step-by-step workflow guidance
- Ensure Atlas standards are followed

---

## 📝 Story Creation Prompts

### Create Story from Requirements
```
Create a complete Atlas user story for the following requirement:

[PASTE YOUR REQUIREMENTS HERE]

Follow the Atlas story template with INVEST criteria, acceptance criteria using Given/When/Then format, success metrics, technical requirements, and risk analysis. Include evidence requirements and mark the story as READY when complete.
```

### Create Story from Bug Report
```
Convert this bug report into a complete Atlas bug story:

[PASTE BUG DETAILS HERE]

Include: severity level, reproduction steps, expected vs actual behavior, impact analysis, root cause hypothesis, proposed fix approach, and verification steps. Follow the Atlas bug report template.
```

### Create Epic from Vision
```
Create an Atlas epic for this initiative:

[PASTE VISION/INITIATIVE HERE]

Break down into multiple user stories (5-8), identify dependencies, define success criteria, include stakeholder analysis, and create a high-level implementation roadmap. Follow the Atlas epic template.
```

### Enhance Existing Story
```
Review and enhance this story to meet Atlas standards:

[PASTE STORY PATH OR CONTENT HERE]

Ensure it has: clear acceptance criteria, measurable success metrics, complete technical requirements, identified risks with mitigations, and evidence requirements. Add any missing sections.
```

---

## 🐛 Bug Fixing Prompts

### Fix Bug with Story
```
Fix the bug described in story: [STORY PATH/NUMBER]

Follow the Atlas troubleshooting process:
1. Reproduce the issue
2. Identify root cause
3. Implement fix with tests
4. Verify all existing tests pass
5. Document the solution
6. Update story with resolution
```

### Debug Crash/Error
```
Debug and fix this error:

Error: [PASTE ERROR MESSAGE]
Stack trace: [PASTE STACK TRACE]
Context: [WHEN IT HAPPENS]

Use systematic debugging to identify root cause, implement a proper fix (not just a workaround), add tests to prevent regression, and handle edge cases.
```

### Performance Issue
```
Investigate and fix this performance issue:

[DESCRIBE PERFORMANCE PROBLEM]

Profile the code, identify bottlenecks, implement optimizations, measure improvements, and ensure no functionality is broken. Document before/after metrics.
```

---

## 💻 Development Prompts

### Implement Story (Full-Stack)
```
Implement story: [STORY PATH/NUMBER]

Work through all acceptance criteria systematically:
1. Review the story requirements
2. Implement frontend components (if applicable)
3. Implement backend logic (if applicable)
4. Write comprehensive tests (>80% coverage)
5. Ensure all acceptance criteria pass
6. Update documentation
7. Mark story as COMPLETE with evidence
```

### Frontend Development
```
Implement the UI components for story: [STORY PATH/NUMBER]

Focus on:
- Responsive design (mobile-first)
- Accessibility (WCAG 2.1 AA)
- Performance (Core Web Vitals)
- Cross-browser compatibility
- Component reusability
- Design system adherence

Include unit tests and integration tests for all components.
```

### Backend Development
```
Implement the backend for story: [STORY PATH/NUMBER]

Requirements:
- RESTful API design (or GraphQL if specified)
- Input validation and error handling
- Security best practices
- Database optimization
- Comprehensive logging
- API documentation
- Unit and integration tests

Ensure <200ms response times for 95th percentile.
```

### API Integration
```
Create API integration for: [SERVICE/FEATURE]

Implement:
1. Client SDK/wrapper
2. Authentication handling
3. Error handling and retries
4. Rate limiting
5. Response caching where appropriate
6. Comprehensive tests with mocks
7. Usage documentation with examples
```

---

## 🧪 Testing Prompts

### Write Tests for Story
```
Write comprehensive tests for story: [STORY PATH/NUMBER]

Include:
- Unit tests for all functions/methods
- Integration tests for workflows
- Edge case testing
- Error condition testing
- Performance tests if applicable
- Achieve >80% code coverage
- Follow Atlas testing standards
```

### Test Driven Development
```
Implement story [STORY PATH/NUMBER] using TDD:

1. Write failing tests for each acceptance criterion
2. Implement minimum code to pass tests
3. Refactor for quality
4. Repeat for each requirement
5. Ensure 90%+ test coverage
6. Document test scenarios
```

### E2E Test Suite
```
Create end-to-end tests for: [FEATURE/WORKFLOW]

Cover:
- Happy path scenarios
- Alternative flows
- Error conditions
- Browser compatibility
- Mobile responsiveness
- Performance under load
Use Atlas E2E testing framework and patterns.
```

---

## 👀 Code Review Prompts

### Review Pull Request
```
Review the code changes in: [PR PATH/NUMBER]

Check for:
- Functionality correctness
- Code quality and maintainability
- Security vulnerabilities
- Performance implications
- Test coverage
- Documentation updates

Provide Atlas review verdict (PASS, CONDITIONAL_PASS, REQUEST_CHANGES, or BLOCKED) with detailed feedback.
```

### Security Review
```
Perform security review on: [CODE/PR PATH]

Identify:
- Injection vulnerabilities (SQL, XSS, etc.)
- Authentication/authorization issues
- Sensitive data exposure
- Security misconfigurations
- Dependency vulnerabilities

Provide severity ratings and remediation steps.
```

### Performance Review
```
Review performance implications of: [CODE/PR PATH]

Analyze:
- Algorithm complexity
- Database query optimization
- Caching opportunities
- Memory usage
- Network calls
- Bundle size impact

Suggest optimizations with expected improvements.
```

---

## 🔄 Refactoring Prompts

### Technical Debt Reduction
```
Refactor [CODE PATH/COMPONENT] to reduce technical debt:

1. Identify code smells
2. Apply SOLID principles
3. Improve testability
4. Enhance readability
5. Update documentation
6. Ensure all tests pass
7. Measure improvement metrics

Maintain 100% backward compatibility.
```

### Pattern Implementation
```
Refactor [CODE PATH] to implement [PATTERN NAME] pattern:

Requirements:
- Maintain existing functionality
- Improve code organization
- Enhance testability
- Document the pattern usage
- Update tests as needed
- Show before/after comparison
```

---

## 🚀 Deployment Prompts

### Prepare Release
```
Prepare release for story/stories: [STORY NUMBERS]

Complete:
1. Verify all acceptance criteria met
2. Run full test suite
3. Update version numbers
4. Generate changelog
5. Update documentation
6. Create release notes
7. Tag release in git
8. Prepare rollback plan
```

### Deploy to Production
```
Deploy [FEATURE/VERSION] to production:

Follow Atlas deployment process:
1. Pre-deployment checks
2. Database migrations (if any)
3. Deploy to staging first
4. Run smoke tests
5. Deploy to production (blue-green/canary)
6. Monitor metrics and logs
7. Run production validation
8. Update status dashboard
```

---

## 📚 Documentation Prompts

### Document Feature
```
Create comprehensive documentation for: [FEATURE/COMPONENT]

Include:
- Overview and purpose
- Architecture/design decisions
- API reference
- Usage examples
- Configuration options
- Troubleshooting guide
- Performance considerations
- Migration guide (if applicable)
```

### API Documentation
```
Document the API for: [SERVICE/ENDPOINT]

Include for each endpoint:
- HTTP method and path
- Request parameters and body
- Response format and codes
- Authentication requirements
- Rate limits
- Example requests/responses
- Error scenarios
- SDKs/client libraries
```

---

## 🔍 Research Prompts

### Technical Research
```
Research and provide recommendations for: [TECHNICAL QUESTION/PROBLEM]

Investigate:
1. Current industry best practices
2. Available solutions/libraries
3. Pros and cons of each approach
4. Performance implications
5. Security considerations
6. Team expertise alignment
7. Recommended approach with justification
```

### Feasibility Analysis
```
Analyze feasibility of: [PROPOSED FEATURE/CHANGE]

Evaluate:
- Technical feasibility
- Resource requirements
- Timeline estimates
- Risk assessment
- Alternative approaches
- Dependencies and blockers
- Recommendation (proceed/modify/defer)
```

---

## 🎯 Orchestration Prompts

### Multi-Story Implementation
```
Implement the following stories in optimal order:
[LIST STORY NUMBERS/PATHS]

Analyze dependencies, determine parallel execution opportunities, spawn specialized agents for each story, coordinate handoffs, and track progress. Use Atlas orchestration patterns.
```

### Feature End-to-End
```
Deliver complete feature from story to production:

Story: [STORY PATH/NUMBER]

Coordinate:
1. Story refinement if needed
2. Development (frontend + backend)
3. Testing (unit + integration + E2E)
4. Code review
5. Documentation
6. Deployment preparation
7. Production release

Use parallel agents where possible. Report status at each stage.
```

---

## 🛠️ Troubleshooting Prompts

### System Investigation
```
Investigate issue: [DESCRIBE PROBLEM]

Follow systematic approach:
1. Gather symptoms and error logs
2. Identify affected components
3. Reproduce in isolated environment
4. Form hypothesis about cause
5. Test hypothesis
6. Implement fix if confirmed
7. Document findings and solution
```

### Production Incident
```
Respond to production incident: [DESCRIBE INCIDENT]

Priority actions:
1. Assess impact and severity
2. Implement immediate mitigation
3. Communicate status to stakeholders
4. Identify root cause
5. Implement permanent fix
6. Write incident report
7. Create preventive measures
```

---

## 💡 Pro Tips

### Story Path Examples
- `09_STORIES/features/ATLAS-001-context-injection.md`
- `09_STORIES/bugs/BUG-042-login-timeout.md`
- `stories/2024/sprint-15/USER-EXPORT.md`

### Story Number Examples
- `ATLAS-001`
- `BUG-042`
- `EPIC-007`
- `S-2024-15-03`

### Best Practices
1. **Always provide context**: Include story path/number when available
2. **Be specific**: Replace all placeholders with actual details
3. **Follow process**: Atlas has specific workflows - let them guide you
4. **Verify evidence**: Ensure all acceptance criteria have evidence
5. **Test everything**: No story is complete without tests

---

## 🔄 Workflow Combinations

### Story → Development → Deployment
```
1. First: Create story for [REQUIREMENTS]
2. Then: Implement story [GENERATED STORY NUMBER]
3. Finally: Deploy to production with rollback plan
```

### Bug → Fix → Verify
```
1. First: Create bug story from [ERROR/ISSUE]
2. Then: Fix the bug in story [GENERATED STORY NUMBER]
3. Finally: Verify fix and update documentation
```

### Research → Story → Implementation
```
1. First: Research feasibility of [FEATURE IDEA]
2. Then: Create story based on research findings
3. Finally: Implement using recommended approach
```

---

*Copy, paste, and customize these prompts for consistent, high-quality Atlas development.*
*Remember: The context injection system will automatically provide the right documentation to agents!*