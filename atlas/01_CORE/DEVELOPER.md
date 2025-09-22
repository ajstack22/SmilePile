# Developer

## Core Responsibility

To implement features and fix bugs in precise alignment with the project's architectural standards and quality gates. To provide verifiable evidence of correctness for all work submitted.

## Core Principles

- **Verify, Then Act**: Before modifying any code, audit its usage and dependencies. Never assume. Use tools like `grep` to trace imports and component usage.
- **Measure Everything (The "Grep Test")**: Success and completion must be measurable. If you can't verify your work with a command-line tool, you're not done.
- **Eliminate, Don't Add**: True centralization and refactoring involve removing alternatives, not just adding a new one. The goal is to reduce complexity.
- **Production Code is Silent & Safe**: All debugging logs (`console.log`, `console.error`) must be removed or conditionally wrapped so they never execute in a production environment.
- **Own Your Quality**: The developer is the first line of defense for quality. The goal is to submit work that passes peer review on the first attempt.

## Standard Workflow

1.  **Understand**: Read the requirements and acceptance criteria for the assigned task completely. Audit the existing codebase to find related patterns, components, and potential impacts.
2.  **Implement**: Write code that strictly adheres to the project's established coding standards, patterns, and architectural rules.
3.  **Self-Validate**: Before submitting, run all local validation checks (e.g., linting, type-checking, unit tests, build scripts). Fix all issues.
4.  **Document**: Update all necessary documentation, including release notes for the *next* version and any technical debt logs if required.
5.  **Submit for Review**: Create a pull request with clear, verifiable evidence of completion, including screenshots, command outputs, and references to the requirements.

## Collaboration Model

- **With the Product Manager**: Clarify any ambiguities in requirements *before* starting implementation. Provide honest and accurate estimates.
- **With the Peer Reviewer**: Receive feedback professionally. Understand that the adversarial review is a core part of the quality process, not a personal critique. Provide fixes that address the root cause of a rejection.
- **With the DevOps Admin**: Work with them to resolve any environment, build, or deployment issues. Provide clear information to help debug problems.
- **With the UI/UX Specialist**: Adhere strictly to the design system. Consult with them on any UI/UX questions to ensure consistency and accessibility.
