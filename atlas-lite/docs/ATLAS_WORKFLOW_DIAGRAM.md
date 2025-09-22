# Atlas Workflow Visualization

## The 7-Phase Atlas Workflow

```mermaid
flowchart TD
    Start([Start New Task]) --> Init[Initialize Workflow]
    Init --> P1{Phase 1: Research}

    %% Phase 1: Research
    P1 --> R1[Locate Relevant Files]
    R1 --> R2[Understand Dependencies]
    R2 --> R3[Map Data Flows]
    R3 --> C1{Checkpoint: Research Complete?}
    C1 -->|No| P1
    C1 -->|Yes| P2{Phase 2: Story Creation}

    %% Phase 2: Story Creation
    P2 --> S1[Define Acceptance Criteria]
    S1 --> S2[Write User Story]
    S2 --> S3[Document Requirements]
    S3 --> C2{Checkpoint: Story Ready?}
    C2 -->|No| P2
    C2 -->|Yes| P3{Phase 3: Planning}

    %% Phase 3: Planning
    P3 --> PL1[Break Down Tasks]
    PL1 --> PL2[Identify File Changes]
    PL2 --> PL3[Estimate Complexity]
    PL3 --> C3{Checkpoint: Plan Approved?}
    C3 -->|No| P3
    C3 -->|Yes| P4{Phase 4: Adversarial Review}

    %% Phase 4: Adversarial Review
    P4 --> A1[Find Edge Cases]
    A1 --> A2[Challenge Assumptions]
    A2 --> A3[Identify Risks]
    A3 --> C4{Checkpoint: Issues Addressed?}
    C4 -->|No| P4
    C4 -->|Yes| P5{Phase 5: Implementation}

    %% Phase 5: Implementation
    P5 --> I1[Write Code]
    I1 --> I2[Follow Conventions]
    I2 --> I3[Create Components]
    I3 --> C5{Checkpoint: Implementation Done?}
    C5 -->|No| P5
    C5 -->|Yes| P6{Phase 6: Testing}

    %% Phase 6: Testing
    P6 --> T1[Run Tests]
    T1 --> T2[Verify Functionality]
    T2 --> T3[Check Performance]
    T3 --> C6{Checkpoint: Tests Pass?}
    C6 -->|No| P5
    C6 -->|Yes| P7{Phase 7: Validation}

    %% Phase 7: Validation
    P7 --> V1[Check Acceptance Criteria]
    V1 --> V2[Verify Requirements Met]
    V2 --> V3[Document Evidence]
    V3 --> C7{Checkpoint: All Criteria Met?}
    C7 -->|No| P5
    C7 -->|Yes| End([Task Complete])

    %% Styling
    classDef phaseStyle fill:#e1f5fe,stroke:#01579b,stroke-width:3px,color:#000
    classDef checkpointStyle fill:#fff3e0,stroke:#e65100,stroke-width:2px,color:#000
    classDef actionStyle fill:#f3e5f5,stroke:#4a148c,stroke-width:1px,color:#000
    classDef terminalStyle fill:#c8e6c9,stroke:#1b5e20,stroke-width:3px,color:#000

    class P1,P2,P3,P4,P5,P6,P7 phaseStyle
    class C1,C2,C3,C4,C5,C6,C7 checkpointStyle
    class R1,R2,R3,S1,S2,S3,PL1,PL2,PL3,A1,A2,A3,I1,I2,I3,T1,T2,T3,V1,V2,V3 actionStyle
    class Start,End terminalStyle
```

## Parallel Execution Pattern

```mermaid
flowchart LR
    Start([Wave Start]) --> Phase[Identify Phase Tasks]
    Phase --> Split{Parallelizable?}

    Split -->|Yes| P1[Task 1]
    Split -->|Yes| P2[Task 2]
    Split -->|Yes| P3[Task 3]

    P1 --> Wait[Wait for All]
    P2 --> Wait
    P3 --> Wait

    Split -->|No| Sequential[Run Sequentially]
    Sequential --> Wait

    Wait --> Checkpoint{Checkpoint}
    Checkpoint -->|Pass| Next[Next Phase]
    Checkpoint -->|Fail| Retry[Fix & Retry]
    Retry --> Split

    Next --> Complete([Phase Complete])

    %% Styling
    classDef parallel fill:#bbdefb,stroke:#1565c0,stroke-width:2px
    classDef sequential fill:#ffccbc,stroke:#bf360c,stroke-width:2px
    classDef control fill:#fff9c4,stroke:#f57f17,stroke-width:2px

    class P1,P2,P3 parallel
    class Sequential sequential
    class Wait,Checkpoint control
```

## Evidence-Driven Development Flow

```mermaid
flowchart TB
    subgraph "Evidence Throughout Development"
        Start([Begin Task]) --> Research[Research Phase]
        Research --> RE[📄 research-phase.md]

        RE --> Story[Story Creation]
        Story --> SE[📄 story-ATLAS-XXX.md]

        SE --> Plan[Planning]
        Plan --> PE[📄 implementation-plan.md]

        PE --> Review[Adversarial Review]
        Review --> RVE[📄 review-findings.md]

        RVE --> Implement[Implementation]
        Implement --> IE[📄 implementation-log.md]

        IE --> Test[Testing]
        Test --> TE[📄 test-results.md]

        TE --> Validate[Validation]
        Validate --> VE[📄 validation-report.md]

        VE --> Final[Final Report]
        Final --> FE[📄 WAVE-X-FINAL-REPORT.md]

        FE --> End([Complete])
    end

    %% Styling
    classDef evidence fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px,color:#000
    classDef phase fill:#e3f2fd,stroke:#1976d2,stroke-width:2px,color:#000

    class RE,SE,PE,RVE,IE,TE,VE,FE evidence
    class Research,Story,Plan,Review,Implement,Test,Validate phase
```

## Component Decomposition Strategy

```mermaid
flowchart TD
    Monolith[Monolithic File<br/>1000+ lines] --> Analyze{Analyze Structure}

    Analyze --> Identify[Identify Components]

    Identify --> UI[UI Components<br/>~150 lines each]
    Identify --> Logic[Business Logic<br/>~200 lines each]
    Identify --> Orchestrator[Orchestrator<br/>~150 lines]
    Identify --> Utils[Utilities<br/>~100 lines each]

    UI --> Check{All < 250 lines?}
    Logic --> Check
    Orchestrator --> Check
    Utils --> Check

    Check -->|No| Decompose[Further Decompose]
    Decompose --> Identify

    Check -->|Yes| Refactor[Refactored Structure<br/>5-8 focused files]

    Refactor --> Validate[Validate Functionality]
    Validate --> Complete([Decomposition Complete])

    %% Styling
    classDef problem fill:#ffcdd2,stroke:#c62828,stroke-width:3px
    classDef solution fill:#c8e6c9,stroke:#2e7d32,stroke-width:3px
    classDef component fill:#e1bee7,stroke:#6a1b9a,stroke-width:2px

    class Monolith problem
    class Refactor,Complete solution
    class UI,Logic,Orchestrator,Utils component
```

## Mode-Aware Architecture Flow

```mermaid
stateDiagram-v2
    [*] --> AppStart: Launch App

    AppStart --> KidsMode: Default Start
    AppStart --> CheckSavedMode: Load Preferences

    CheckSavedMode --> KidsMode: Saved = Kids
    CheckSavedMode --> ParentMode: Saved = Parent

    state KidsMode {
        [*] --> KidsGallery
        KidsGallery --> ViewPhoto
        ViewPhoto --> KidsGallery
        KidsGallery --> FilterCategories
        FilterCategories --> KidsGallery
        KidsGallery --> RequestParentMode: Mode Switch FAB
    }

    state ParentMode {
        [*] --> PhotoGallery
        PhotoGallery --> AddPhotos
        PhotoGallery --> DeletePhotos
        PhotoGallery --> Settings
        Settings --> PINManagement
        Settings --> PhotoGallery
        PhotoGallery --> EditCategories
        PhotoGallery --> SwitchToKids: Kids Mode FAB
    }

    RequestParentMode --> PINAuth: Request PIN
    PINAuth --> ParentMode: Success
    PINAuth --> KidsMode: Failed/Cancel

    SwitchToKids --> KidsMode: Immediate

    ParentMode --> SaveMode: Mode Changed
    KidsMode --> SaveMode: Mode Changed
    SaveMode --> [*]: Persist Preference
```

## Success Metrics Dashboard

```mermaid
graph LR
    subgraph "Wave Metrics"
        W1[Wave 1<br/>8 hours]
        W2[Wave 2<br/>6 hours]
        W3[Wave 3<br/>6 hours]
        W4[Wave 4<br/>5 hours]
        W5[Wave 5<br/>4 hours]
        W6[Wave 6<br/>4 hours]
        W7[Wave 7<br/>4 hours]
    end

    subgraph "Quality Metrics"
        Q1[Zero Rollbacks]
        Q2[100% Acceptance]
        Q3[Decreasing Bugs]
    end

    subgraph "Efficiency Gains"
        E1[3-5x Speed]
        E2[80% Less Code]
        E3[60% Less Review Time]
    end

    W1 --> W2 --> W3 --> W4 --> W5 --> W6 --> W7

    W7 --> Q1
    W7 --> Q2
    W7 --> Q3

    Q1 --> E1
    Q2 --> E2
    Q3 --> E3

    %% Styling
    classDef wave fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    classDef quality fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef efficiency fill:#e8f5e9,stroke:#388e3c,stroke-width:3px

    class W1,W2,W3,W4,W5,W6,W7 wave
    class Q1,Q2,Q3 quality
    class E1,E2,E3 efficiency
```

## Key Takeaways

### The Workflow Works Because:
1. **Enforced Checkpoints** - Can't skip ahead
2. **Evidence at Each Phase** - Not just at the end
3. **Adversarial Review** - Catches issues early
4. **Parallel Execution** - 3-5x speed improvement
5. **Pragmatic Limits** - 250 lines, 30% coverage

### The Anti-Patterns to Avoid:
- ❌ Skipping research to "save time"
- ❌ Writing code before planning
- ❌ Batching evidence at the end
- ❌ Complex automation over simple scripts
- ❌ Perfect over practical

### The Success Formula:
**Structure + Pragmatism + Parallel Execution + Evidence = Success**