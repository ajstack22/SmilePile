# SmilePile Agent Assignments - Specialized Roles

## Agent Pool Configuration

### Required Agents
- **Backend Developer Agent**: 2 instances
- **UI Developer Agent**: 2 instances
- **Performance Reviewer Agent**: 1 instance
- **Security Reviewer Agent**: 1 instance

Total Agent Pool: 6 specialized agents (3 concurrent maximum)

## Backend Developer Agent

### Profile
```yaml
Agent Type: Backend Developer
Expertise: Android architecture, Kotlin, databases, file systems
Trust Score: 0.85 (for differential reviews)
Parallel Capable: Yes
```

### Assigned Tasks

#### Wave 1
- **Task 1.1: Build System Setup** (2 hours)
  ```
  Objectives:
  - Configure Gradle with proper dependency management
  - Set up Kotlin coroutines
  - Configure ProGuard/R8 rules
  - Implement build flavors (debug/release)

  Success Criteria:
  - Clean build with no warnings
  - All dependencies resolved
  - Build time < 30 seconds
  ```

- **Task 1.2: Data Models Design** (2 hours)
  ```
  Objectives:
  - Design Photo entity class
  - Design Category entity class
  - Create repository interfaces
  - Define data relationships

  Success Criteria:
  - Type-safe data models
  - Proper nullable handling
  - Repository pattern implemented
  ```

#### Wave 2
- **Task 2.1: Storage Implementation** (3 hours)
  ```
  Objectives:
  - Implement Room database or SQLite
  - Create DAOs for Photo and Category
  - Set up SharedPreferences helper
  - Design file storage structure

  Success Criteria:
  - Database migrations configured
  - CRUD operations tested
  - File I/O error handling
  ```

- **Task 2.2: Photo Management System** (3 hours)
  ```
  Objectives:
  - Implement photo repository
  - Asset photo loading logic
  - Imported photo management
  - Path resolution system

  Success Criteria:
  - Photos load from both sources
  - Unique ID generation
  - Proper cleanup on deletion
  ```

- **Task 2.3: Category Management** (2 hours)
  ```
  Objectives:
  - Initialize default categories
  - Category CRUD operations
  - Photo-category associations
  - Position/ordering logic

  Success Criteria:
  - Default categories created on first launch
  - Categories persist across sessions
  - Proper cascade deletion
  ```

#### Wave 5
- **Task 5.2: Import Pipeline** (2 hours)
  ```
  Objectives:
  - Photo picker integration
  - Content URI resolution
  - File copying to internal storage
  - Database record creation

  Success Criteria:
  - Multiple photo import works
  - Handles various image formats
  - Progress tracking accurate
  ```

#### Wave 6
- **Task 6.3: Error Handling** (2 hours)
  ```
  Objectives:
  - Global exception handling
  - Database corruption recovery
  - Storage space monitoring
  - Graceful degradation

  Success Criteria:
  - No unhandled exceptions
  - User-friendly error messages
  - Recovery mechanisms tested
  ```

### Agent Prompt Template
```markdown
You are a Backend Developer Agent specializing in Android development with Kotlin.

Focus Areas:
- Clean architecture patterns (MVVM/MVI)
- Efficient data persistence
- Coroutines and async operations
- Memory management
- File system operations

Quality Standards:
- Null safety enforcement
- Proper exception handling
- Unit test coverage > 80%
- Clear separation of concerns
```

## UI Developer Agent

### Profile
```yaml
Agent Type: UI Developer
Expertise: Material Design, Compose/XML layouts, animations, UX
Trust Score: 0.80
Parallel Capable: Yes
```

### Assigned Tasks

#### Wave 1
- **Task 1.3: Theme System Architecture** (3 hours)
  ```
  Objectives:
  - Create Light, Dark, Rainbow themes
  - Theme switching mechanism
  - Dynamic color support
  - Theme persistence

  Success Criteria:
  - All UI elements themed properly
  - Smooth theme transitions
  - Themes persist across restarts
  ```

#### Wave 3
- **Task 3.1: Navigation Architecture** (2 hours)
  ```
  Objectives:
  - Single Activity setup
  - Navigation component configuration
  - Fragment transitions
  - Deep linking support

  Success Criteria:
  - Back navigation works correctly
  - State preserved on rotation
  - No memory leaks
  ```

- **Task 3.2: Base UI Components** (3 hours)
  ```
  Objectives:
  - Custom photo view widget
  - Grid layout managers
  - Touch gesture recognizers
  - Loading indicators

  Success Criteria:
  - Reusable components
  - Consistent styling
  - Smooth animations
  ```

#### Wave 4
- **Task 4.1: Category Selection Screen** (3 hours)
  ```
  Objectives:
  - Grid view implementation
  - Category cards with previews
  - Touch feedback animations
  - Child-friendly sizing

  Success Criteria:
  - Touch targets > 48dp
  - Visual feedback on press
  - Smooth scrolling
  ```

- **Task 4.2: Photo Gallery** (4 hours)
  ```
  Objectives:
  - ViewPager2 or similar for swiping
  - Fullscreen immersive mode
  - Smooth transitions
  - Gesture navigation

  Success Criteria:
  - 60fps scrolling
  - No image tearing
  - Responsive to swipes
  ```

- **Task 4.3: Child Mode UX** (2 hours)
  ```
  Objectives:
  - Simplified navigation
  - Visual-first interface
  - Protection from accidental exits
  - Age-appropriate interactions

  Success Criteria:
  - No text-heavy interfaces
  - Clear visual hierarchy
  - Intuitive for ages 3-7
  ```

#### Wave 5
- **Task 5.3: Import UI Flow** (2 hours)
  ```
  Objectives:
  - Photo selection grid
  - Category assignment dialog
  - Progress indicators
  - Success/error feedback

  Success Criteria:
  - Clear selection states
  - Accurate progress reporting
  - Informative error messages
  ```

#### Wave 6
- **Task 6.2: Accessibility** (2 hours)
  ```
  Objectives:
  - Content descriptions for all images
  - Touch target verification
  - Screen reader support
  - High contrast mode

  Success Criteria:
  - Accessibility scanner passes
  - TalkBack navigation works
  - Color contrast ratios met
  ```

### Agent Prompt Template
```markdown
You are a UI Developer Agent specializing in Android UI/UX development.

Focus Areas:
- Material Design 3 guidelines
- Child-friendly interface design
- Smooth animations and transitions
- Responsive layouts
- Accessibility standards

Quality Standards:
- 60fps performance target
- Touch targets minimum 48dp
- WCAG 2.1 AA compliance
- Consistent visual language
```

## Performance Reviewer Agent

### Profile
```yaml
Agent Type: Performance Reviewer
Expertise: Profiling, optimization, memory management, benchmarking
Trust Score: 0.90
Parallel Capable: Yes
```

### Assigned Tasks

#### Wave 3
- **Task 3.3: Image Loading System** (3 hours)
  ```
  Objectives:
  - Efficient bitmap decoding
  - Memory cache implementation
  - Disk cache if needed
  - Async loading with coroutines

  Success Criteria:
  - No OOM errors
  - Images load < 200ms
  - Smooth scrolling maintained
  - Memory usage optimized
  ```

#### Wave 6
- **Task 6.1: Performance Optimization** (3 hours)
  ```
  Objectives:
  - Profile entire app
  - Optimize slow operations
  - Reduce memory footprint
  - Improve startup time

  Success Criteria:
  - Startup time < 2 seconds
  - No frame drops
  - Memory usage < 100MB
  - Battery efficient
  ```

### Agent Prompt Template
```markdown
You are a Performance Reviewer Agent specializing in Android app optimization.

Focus Areas:
- Memory profiling and leak detection
- CPU usage optimization
- Battery efficiency
- Network optimization
- Rendering performance

Quality Standards:
- 60fps UI rendering
- No memory leaks
- Startup time < 2s
- Jank-free scrolling
- Efficient resource usage

Tools & Techniques:
- Android Studio Profiler
- Systrace analysis
- StrictMode violations
- LeakCanary integration
- Benchmark testing
```

## Security Reviewer Agent

### Profile
```yaml
Agent Type: Security Reviewer
Expertise: Android security, child safety, data protection
Trust Score: 0.95
Parallel Capable: No (thorough review required)
```

### Assigned Tasks

#### Wave 5
- **Task 5.1: Parent Mode Security** (2 hours)
  ```
  Objectives:
  - Implement authentication mechanism
  - Math question generator
  - Session management
  - Child lock implementation

  Success Criteria:
  - Children cannot bypass
  - No hardcoded passwords
  - Session timeout works
  - Secure random generation
  ```

### Agent Prompt Template
```markdown
You are a Security Reviewer Agent specializing in Android app security and child safety.

Focus Areas:
- Authentication mechanisms
- Data protection
- Child safety features
- Permission management
- Secure coding practices

Quality Standards:
- No sensitive data in logs
- Proper input validation
- Secure storage usage
- Child-appropriate content
- COPPA compliance considerations

Security Checklist:
- No hardcoded credentials
- Proper permission requests
- Secure random number generation
- Protected component exposure
- Safe intent handling
```

## Agent Coordination Protocol

### Communication Channels
```yaml
Coordination:
  - Shared artifact repository
  - Dependency status board
  - Conflict resolution queue
  - Review feedback channel

Handoff Protocol:
  - Complete task documentation
  - Updated test results
  - Performance metrics
  - Known issues list
```

### Conflict Resolution
```yaml
Priority Order:
  1. Security Reviewer (highest priority)
  2. Performance Reviewer
  3. Backend Developer
  4. UI Developer

Resolution Steps:
  1. Detect conflict via file monitoring
  2. Queue conflicting changes
  3. Apply priority ordering
  4. Merge non-conflicting changes
  5. Request manual review if needed
```

## Performance Metrics

### Agent Efficiency Targets

| Agent Type | Tasks | Target Time | Actual Time | Efficiency |
|------------|-------|-------------|-------------|------------|
| Backend Developer | 7 | 17h | TBD | TBD |
| UI Developer | 8 | 17h | TBD | TBD |
| Performance Reviewer | 2 | 6h | TBD | TBD |
| Security Reviewer | 1 | 2h | TBD | TBD |

### Quality Metrics

```yaml
Backend Developer:
  - Code coverage: > 80%
  - Crash-free rate: > 99.9%
  - Build success rate: 100%

UI Developer:
  - Frame rate: 60fps
  - Touch target compliance: 100%
  - Theme consistency: 100%

Performance Reviewer:
  - Memory leaks found: Target 0
  - Performance regressions: Target 0
  - Optimization improvements: > 20%

Security Reviewer:
  - Security vulnerabilities: Target 0
  - Child safety violations: Target 0
  - Compliance issues: Target 0
```

## Agent Activation Commands

```bash
# Initialize agent pool
python atlas.py agents init SMILE-001 \
  --backend 2 \
  --ui 2 \
  --performance 1 \
  --security 1

# Assign tasks to agents
python atlas.py agents assign SMILE-001 \
  --config AGENT_ASSIGNMENTS.md

# Monitor agent performance
python atlas.py agents status SMILE-001

# Review agent metrics
python atlas.py agents metrics SMILE-001 \
  --report efficiency,quality

# Rebalance workload if needed
python atlas.py agents rebalance SMILE-001
```