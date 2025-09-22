# 00: Core Principles

These are the foundational, non-negotiable principles that govern all work within the Atlas framework. They are the philosophical bedrock upon which our roles, processes, and standards are built.

## 1. Quality Over Speed

We build for the long term. Rushing to meet a deadline at the expense of quality is a false economy that leads to technical debt, bugs, and user dissatisfaction. Quality is not a feature; it is the foundation.

-   **No shortcuts on data integrity.** Data loss is unacceptable.
-   **Performance requirements are non-negotiable.** A slow feature is a broken feature.
-   **Requirements are binary.** A task is 100% complete or it is not done.

## 2. Evidence-Based Development

Opinions, claims, and assumptions are not valid currency. All work must be supported by verifiable evidence. "It works" is not enough; you must *prove* it works.

-   **Document everything.** Provide screenshots, command outputs, and performance metrics.
-   **Test on all platforms.** Do not assume that a change on one platform will work on another.
-   **Verify independently.** The Peer Reviewer's job is to trust nothing and verify every claim.

## 3. Elimination Over Addition

Complexity is the enemy. True refactoring and centralization involve reducing the number of ways to do something, not just adding a new, "better" way. The goal is to simplify.

-   **Audit before acting.** Understand the existing landscape before proposing a change.
-   **Pick one winner.** When multiple solutions exist, choose one and migrate everything to it.
-   **Delete the alternatives.** The final, most important step of centralization is eliminating the old ways.

## 4. Prevention Over Correction

We strive to catch issues before they reach production. Our processes are designed to be rigorous quality gates, not just a series of steps. Every bug that reaches a user is a failure of the process.

-   **Automate quality checks.** The deployment script is the ultimate guardian of quality.
-   **Learn from every mistake.** Every production bug should result in a new, automated validation check.
-   **The adversarial review is our strongest preventative tool.** It is designed to find flaws early.

## 5. Clarity Over Cleverness

We build software that must be maintained by others. Code should be simple, readable, and easy to understand. A clever, unreadable solution is a liability.

-   **Follow established patterns.** Consistency makes the codebase predictable.
-   **Document the *why*, not the *what*.** Code should explain what it does. Comments should explain why it does it that way.
-   **Keep it simple.** A straightforward solution is always preferable to a complex one.
