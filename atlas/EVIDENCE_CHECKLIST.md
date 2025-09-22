# SmilePile Evidence Templates & Validation Checklists

## Evidence Collection Framework

### Automated Evidence Collection
```yaml
Collection Points:
  - Pre-task: Environment validation
  - During-task: Progress snapshots
  - Post-task: Completion artifacts
  - Review-ready: Comprehensive package

Storage Structure:
  /evidence/SMILE-001/
    ├── wave-1/
    │   ├── task-1.1/
    │   │   ├── build-logs/
    │   │   ├── dependency-tree/
    │   │   └── validation-results/
    │   └── ...
    ├── performance/
    ├── security/
    └── ui-screenshots/
```

## Phase 1: Infrastructure Evidence

### Task 1.1: Build System Setup
#### Evidence Requirements
- [ ] `build.gradle` files (app and project level)
- [ ] Successful build log
- [ ] Dependency tree output
- [ ] Build time metrics
- [ ] ProGuard/R8 configuration
- [ ] Lint report (0 errors required)

#### Validation Checklist
```bash
✓ Gradle wrapper version >= 8.0
✓ Kotlin version >= 1.9.0
✓ Material Design Components >= 1.9.0
✓ AndroidX dependencies consistent
✓ Build completes without warnings
✓ All modules compile successfully
✓ Test configuration present
```

#### Collection Command
```bash
./gradlew clean assembleDebug --info > evidence/build-log.txt
./gradlew dependencies > evidence/dependency-tree.txt
./gradlew lint > evidence/lint-report.html
```

### Task 1.2: Data Models Design
#### Evidence Requirements
- [ ] Entity class definitions
- [ ] Repository interfaces
- [ ] Data relationship diagram
- [ ] Unit test files
- [ ] Code coverage report

#### Validation Checklist
```kotlin
✓ Photo entity has required fields
✓ Category entity properly defined
✓ Nullable types used appropriately
✓ Repository pattern implemented
✓ No data class warnings
✓ Serialization configured
```

### Task 1.3: Theme System
#### Evidence Requirements
- [ ] Theme XML resources
- [ ] Color palette definitions
- [ ] Style inheritance diagram
- [ ] Theme switching demo video
- [ ] Screenshots of each theme

#### Validation Checklist
```xml
✓ Light theme complete
✓ Dark theme complete
✓ Rainbow theme complete
✓ All components styled
✓ Dynamic color support (API 31+)
✓ Theme persistence tested
```

## Phase 2: Data Layer Evidence

### Task 2.1: Storage Implementation
#### Evidence Requirements
- [ ] Database schema export
- [ ] Migration scripts
- [ ] DAO implementation
- [ ] Database inspector screenshots
- [ ] CRUD operation logs

#### Validation Checklist
```sql
✓ Tables created correctly
✓ Indexes defined for queries
✓ Foreign keys configured
✓ Migration from version 1->2 tested
✓ Thread safety verified
✓ Database encryption (optional)
```

#### Test Evidence
```kotlin
@Test fun testDatabaseCreation()
@Test fun testPhotoInsertion()
@Test fun testCategoryQuery()
@Test fun testCascadeDelete()
@Test fun testMigration()
```

### Task 2.2: Photo Management
#### Evidence Requirements
- [ ] Photo repository implementation
- [ ] Asset loading demonstration
- [ ] Import functionality proof
- [ ] Memory profiling results
- [ ] Performance benchmarks

#### Validation Checklist
```
✓ Assets load successfully
✓ Imported photos persist
✓ Unique IDs generated
✓ Path resolution works
✓ Deletion cleans up files
✓ No memory leaks detected
```

## Phase 3: UI Foundation Evidence

### Task 3.1: Navigation Architecture
#### Evidence Requirements
- [ ] Navigation graph XML
- [ ] Fragment lifecycle logs
- [ ] Back stack demonstration
- [ ] Configuration change handling
- [ ] Deep link testing results

#### Validation Checklist
```
✓ Single Activity pattern
✓ Navigation component integrated
✓ Safe Args configured
✓ Back navigation correct
✓ State survives rotation
✓ No fragment leaks
```

### Task 3.3: Image Loading System
#### Evidence Requirements
- [ ] Memory profiling graphs
- [ ] Loading time measurements
- [ ] Cache hit rate statistics
- [ ] OOM crash reports (should be 0)
- [ ] Bitmap pool utilization

#### Performance Benchmarks
```yaml
Target Metrics:
  - First image load: < 500ms
  - Cached image load: < 50ms
  - Memory usage: < 50MB for 20 images
  - Cache hit rate: > 80%
  - Scroll jank: < 1%
```

## Phase 4: Child Mode Evidence

### Task 4.1: Category Selection Screen
#### Evidence Requirements
- [ ] Screenshot of category grid
- [ ] Touch target size verification
- [ ] Accessibility scanner report
- [ ] User testing video (optional)
- [ ] Performance metrics

#### UX Validation Checklist
```
✓ All touch targets >= 48dp
✓ Visual feedback on press
✓ Categories clearly distinguishable
✓ Smooth scrolling (60fps)
✓ Child-friendly colors/icons
✓ No text required to navigate
```

### Task 4.2: Photo Gallery
#### Evidence Requirements
- [ ] Gallery navigation video
- [ ] FPS counter screenshot
- [ ] Memory usage during swipe
- [ ] Gesture recognition logs
- [ ] Fullscreen mode proof

#### Performance Evidence
```bash
# Capture systrace
adb shell atrace --async_start -a com.smilepile
# Swipe through 20 photos
adb shell atrace --async_stop > evidence/gallery-trace.html

# Check for jank
grep "Skipped" evidence/logcat.txt | wc -l  # Should be 0
```

## Phase 5: Parent Mode Evidence

### Task 5.1: Parent Mode Security
#### Evidence Requirements
- [ ] Authentication flow video
- [ ] Math question examples
- [ ] Session timeout demonstration
- [ ] Failed attempt handling
- [ ] Security audit report

#### Security Validation Checklist
```
✓ No hardcoded passwords
✓ Math questions randomized
✓ Session expires after timeout
✓ Child cannot bypass
✓ No sensitive data in logs
✓ Secure storage used
```

### Task 5.2: Import Pipeline
#### Evidence Requirements
- [ ] Photo picker screenshots
- [ ] Permission request flow
- [ ] Import progress video
- [ ] File copy verification
- [ ] Database update logs

#### Import Test Scenarios
```yaml
Test Cases:
  - Single photo import
  - Batch import (10+ photos)
  - Mixed format import (JPG, PNG)
  - Large file handling (>10MB)
  - Permission denial recovery
  - Storage full scenario
```

## Phase 6: Polish Evidence

### Task 6.1: Performance Optimization
#### Evidence Requirements
- [ ] Before/after profiling comparison
- [ ] Startup time measurements
- [ ] Memory usage graphs
- [ ] Battery usage report
- [ ] Network usage (should be minimal)

#### Performance Targets
```yaml
Metrics:
  App Startup:
    Cold start: < 2000ms
    Warm start: < 1000ms
    Hot start: < 500ms

  Memory:
    Initial: < 50MB
    After 5 min use: < 100MB
    After gallery view: < 150MB

  Battery:
    Idle drain: < 1%/hour
    Active use: < 10%/hour

  Frame Rate:
    UI: 60fps consistent
    Scroll: < 1% jank
    Animations: 60fps
```

### Task 6.2: Accessibility
#### Evidence Requirements
- [ ] Accessibility scanner results
- [ ] TalkBack navigation video
- [ ] Content description audit
- [ ] Color contrast report
- [ ] Touch target measurements

#### Accessibility Checklist
```
✓ All images have descriptions
✓ Color contrast >= 4.5:1
✓ Touch targets >= 48dp
✓ Focus order logical
✓ Screen reader compatible
✓ No accessibility errors
```

## Automated Evidence Collection Script

```python
#!/usr/bin/env python3
# atlas_evidence_collector.py

import os
import subprocess
import json
from datetime import datetime

class EvidenceCollector:
    def __init__(self, workflow_id, task_id):
        self.workflow_id = workflow_id
        self.task_id = task_id
        self.evidence_dir = f"evidence/{workflow_id}/{task_id}"
        os.makedirs(self.evidence_dir, exist_ok=True)

    def collect_build_evidence(self):
        """Collect build-related evidence"""
        subprocess.run([
            "./gradlew", "clean", "assembleDebug", "--info"
        ], capture_output=True, text=True)

    def collect_test_evidence(self):
        """Run and collect test results"""
        subprocess.run([
            "./gradlew", "test", "--continue"
        ], capture_output=True)

    def collect_lint_evidence(self):
        """Run lint and collect report"""
        subprocess.run([
            "./gradlew", "lint"
        ], capture_output=True)

    def collect_performance_evidence(self):
        """Collect performance metrics"""
        # Memory info
        subprocess.run([
            "adb", "shell", "dumpsys", "meminfo", "com.smilepile"
        ], capture_output=True)

    def generate_report(self):
        """Generate evidence summary report"""
        report = {
            "workflow_id": self.workflow_id,
            "task_id": self.task_id,
            "timestamp": datetime.now().isoformat(),
            "evidence_collected": os.listdir(self.evidence_dir)
        }

        with open(f"{self.evidence_dir}/summary.json", "w") as f:
            json.dump(report, f, indent=2)

# Usage
collector = EvidenceCollector("SMILE-001", "task-1.1")
collector.collect_build_evidence()
collector.collect_test_evidence()
collector.generate_report()
```

## Review Verdict Criteria

### PASS Criteria
- All evidence requirements met
- All validation checklists complete
- Performance targets achieved
- No critical issues found

### PASS_WITH_MINOR Criteria
- 95% of evidence requirements met
- Minor issues documented
- Performance within 10% of targets
- Plan for minor fixes provided

### CONDITIONAL_PASS Criteria
- 90% of evidence requirements met
- Major features working
- Performance degradation < 20%
- Clear remediation timeline

### NEEDS_CHANGES Criteria
- Missing critical evidence
- Validation failures
- Performance targets missed
- Blocking issues identified

### BLOCKED Criteria
- Fundamental architecture issues
- Security vulnerabilities
- Data loss scenarios
- Child safety concerns

## Evidence Validation Commands

```bash
# Validate all evidence for a task
python atlas.py evidence validate SMILE-001 --task 1.1

# Generate evidence report
python atlas.py evidence report SMILE-001 --format html

# Check evidence completeness
python atlas.py evidence check SMILE-001 --phase 2

# Archive evidence for review
python atlas.py evidence archive SMILE-001 --checkpoint 1

# Compare evidence across iterations
python atlas.py evidence diff SMILE-001 --from v1 --to v2
```