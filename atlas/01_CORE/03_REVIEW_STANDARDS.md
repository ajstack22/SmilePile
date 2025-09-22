# 03: Adversarial Review Standards

These standards define the rules for the Adversarial Peer Review process. The primary goal of the reviewer is to find flaws and enforce quality, not to be helpful or polite.

## 1. The Adversarial Mindset

-   **Assume Everything is Broken**: The reviewer must start with the assumption that the implementation is flawed until proven otherwise by verifiable evidence.
-   **Try to Break It**: Go beyond the happy path. Test edge cases, empty data, massive data sets, rapid clicks, offline mode, and other hostile scenarios.
-   **Verify All Claims**: Do not trust any claims made by the developer. Run all commands yourself and test the feature independently on all required platforms.
-   **Reject Promises**: A promise to "fix it later" is an automatic rejection. The work must be 100% complete and correct *now*.

## 2. Evidence Requirements

A submission for review is incomplete without evidence. The developer must provide a clear report that includes:

-   **Requirement-Evidence Mapping**: Each requirement from the work package must be listed, followed by evidence (screenshot, GIF, log output) that it has been completed.
-   **Validation Command Output**: The output of all required validation commands (`lint`, `test`, `build`) must be included.
-   **Platform Testing Confirmation**: Explicit confirmation that the feature has been tested on all required platforms (e.g., "Tested on Chrome, iOS 16 Simulator, and Pixel 6 Emulator").

## 3. Reviewer's Checklist

In addition to verifying the developer's evidence, the reviewer must perform their own checks:

-   [ ] Does the code violate any core architectural rules?
-   [ ] Does the code violate any code quality standards?
-   [ ] Does the UI/UX violate the design system?
-   [ ] Does the change introduce any performance regressions?
-   [ ] Is the accompanying documentation (e.g., release notes) accurate and complete?
-   [ ] Is the implementation clear and maintainable, or is it overly clever?

## 4. Conflict Resolution

Disputes during the review process are handled by deferring to the framework's principles:

-   **Dispute**: "The requirements were unclear."
    -   **Resolution**: The PM clarifies the requirements. The work is rejected until it meets the clarified requirements.
-   **Dispute**: "This is good enough."
    -   **Resolution**: Requirements are binary (100% complete or not done). There is no "good enough."
-   **Dispute**: "A platform limitation prevents this."
    -   **Resolution**: A workaround must be found, or the limitation must be formally documented and approved by the PM and other stakeholders.
