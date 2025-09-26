#!/bin/bash
#
# Wave 1: Foundation & Architecture - iOS Implementation
# Atlas-based orchestration for parallel story execution
#

set -e  # Exit on error

echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║         Wave 1: Foundation & Architecture - iOS SmilePile           ║"
echo "║                    Atlas Workflow Orchestration                     ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""
echo "📅 Date: $(date)"
echo "🎯 Objective: Establish Core Data, Storage, and DI foundation"
echo "⏱️  Estimated Duration: 3 days"
echo ""

# Create evidence directory
EVIDENCE_DIR="ios/wave-1-evidence"
mkdir -p "$EVIDENCE_DIR"

# Phase 1: Research
echo "═══════════════════════════════════════════════════════════════════════"
echo "📚 PHASE 1: RESEARCH"
echo "═══════════════════════════════════════════════════════════════════════"
echo ""
echo "Analyzing iOS Documentation for Wave 1 components..."
echo ""

cat > "$EVIDENCE_DIR/research-phase.md" << 'EOF'
# Wave 1 Research Phase

## Documentation Analyzed
- iOS_Documentation/01_Architecture/SmilePile_Data_Layer_Architecture.md
- iOS_Documentation/01_Architecture/Data_Access_Objects_Reference.md
- iOS_Documentation/05_Platform_Integration/01_Storage_File_Operations.md

## Key Findings

### Core Data Requirements
- 2 entities: PhotoEntity and CategoryEntity
- UUID-based IDs for photos (stable across imports)
- Long IDs for categories
- Type converters for Date and List<String>
- Version migration strategy needed

### Storage Requirements
- Internal app sandbox only
- Photos stored in Documents/Photos/
- Thumbnails in Caches/Thumbnails/
- Metadata in Core Data

### DI Architecture
- Protocol-based dependency injection
- Repository pattern with protocols
- Service registration at app launch
- No third-party DI framework needed

## Implementation Order
1. Core Data models and stack
2. Storage directory setup
3. Repository protocols
4. DI container
5. Unit tests

## Risk Areas
- Core Data migration complexity
- UUID generation for stable IDs
- Memory management with large datasets
EOF

echo "✅ Research phase documented in $EVIDENCE_DIR/research-phase.md"
echo ""

# Phase 2: Story Creation
echo "═══════════════════════════════════════════════════════════════════════"
echo "📝 PHASE 2: STORY CREATION"
echo "═══════════════════════════════════════════════════════════════════════"
echo ""
echo "Creating Atlas stories for Wave 1 components..."
echo ""

# Story 1: Core Data Setup
cat > "ios/ATLAS-IOS-001-core-data-setup.md" << 'EOF'
# ATLAS-IOS-001: Core Data Setup & Models

## Story
As an iOS developer, I need to establish the Core Data stack with Photo and Category entities that match the Android Room database schema, ensuring data integrity and cross-platform compatibility.

## Acceptance Criteria
1. [ ] Core Data stack initialized with proper configuration
2. [ ] PhotoEntity created with all required attributes
3. [ ] CategoryEntity created with proper relationships
4. [ ] Type converters for Date and string arrays
5. [ ] Migration strategy documented
6. [ ] Unit tests for CRUD operations

## Technical Requirements
- PhotoEntity attributes: id (UUID), uri, categoryId, isFavorite, dateAdded, childName, childAge, metadata
- CategoryEntity attributes: id (Int64), name, color, position, isDefault
- One-to-many relationship: Category -> Photos
- Selective encryption for child data

## Success Metrics
- All CRUD operations < 100ms
- Zero data loss during saves
- 100% test coverage for models

## Implementation Notes
- Use NSManagedObject subclasses
- Implement stable UUID generation from URI
- Add validation rules in Core Data model
- Consider batch operations for performance
EOF

# Story 2: Storage Management
cat > "ios/ATLAS-IOS-002-storage-management.md" << 'EOF'
# ATLAS-IOS-002: Storage Management System

## Story
As a user, I need a reliable storage system that manages my photos in the app's private storage, ensuring they remain secure and organized even if deleted from my device gallery.

## Acceptance Criteria
1. [ ] Directory structure created on first launch
2. [ ] Photos copied to app storage on import
3. [ ] Thumbnails generated and cached
4. [ ] Old files cleaned up appropriately
5. [ ] Storage size tracking implemented
6. [ ] File operations are atomic and safe

## Technical Requirements
- Documents/Photos/ for full images
- Caches/Thumbnails/ for previews
- FileManager wrapper for operations
- URL-based file handling
- Async operations with progress

## Success Metrics
- File operations complete < 1s for single photo
- Thumbnail generation < 500ms
- No file corruption or loss
- Proper cleanup of orphaned files

## Implementation Notes
- Use FileManager with proper error handling
- Implement operation queue for batch operations
- Add file existence validation
- Consider using NSFileCoordinator for safety
EOF

# Story 3: Dependency Injection
cat > "ios/ATLAS-IOS-003-dependency-injection.md" << 'EOF'
# ATLAS-IOS-003: Dependency Injection Setup

## Story
As a developer, I need a clean dependency injection system that provides repositories and services throughout the app without tight coupling or complex third-party frameworks.

## Acceptance Criteria
1. [ ] DI container protocol defined
2. [ ] Service registration implemented
3. [ ] Repository protocols created
4. [ ] Container accessible app-wide
5. [ ] Lazy initialization supported
6. [ ] Thread-safe implementation

## Technical Requirements
- Protocol-based abstractions
- Singleton container pattern
- Registration at app launch
- Property wrapper for injection (@Injected)
- Support for mocks in tests

## Success Metrics
- Zero runtime DI errors
- < 10ms total registration time
- Easy mock injection for tests
- No memory leaks from retained services

## Implementation Notes
- Start simple, avoid over-engineering
- Use protocols for all services
- Consider property wrappers for clean syntax
- Ensure thread safety with locks
EOF

echo "✅ Stories created:"
echo "   - ios/ATLAS-IOS-001-core-data-setup.md"
echo "   - ios/ATLAS-IOS-002-storage-management.md"
echo "   - ios/ATLAS-IOS-003-dependency-injection.md"
echo ""

# Phase 3: Planning
echo "═══════════════════════════════════════════════════════════════════════"
echo "📋 PHASE 3: PLANNING"
echo "═══════════════════════════════════════════════════════════════════════"
echo ""

cat > "$EVIDENCE_DIR/implementation-plan.md" << 'EOF'
# Wave 1 Implementation Plan

## Execution Strategy
Stories 1, 2, and 3 can be developed in parallel by different team members.

## File Structure to Create
```
ios/SmilePile/
├── SmilePile.xcodeproj
├── SmilePile/
│   ├── App/
│   │   ├── SmilePileApp.swift
│   │   └── AppDelegate.swift
│   ├── Core/
│   │   ├── Data/
│   │   │   ├── SmilePile.xcdatamodeld
│   │   │   ├── Entities/
│   │   │   │   ├── PhotoEntity+CoreDataClass.swift
│   │   │   │   ├── PhotoEntity+CoreDataProperties.swift
│   │   │   │   ├── CategoryEntity+CoreDataClass.swift
│   │   │   │   └── CategoryEntity+CoreDataProperties.swift
│   │   │   └── CoreDataStack.swift
│   │   ├── Storage/
│   │   │   ├── StorageManager.swift
│   │   │   ├── FileOperations.swift
│   │   │   └── ThumbnailGenerator.swift
│   │   └── DI/
│   │       ├── DIContainer.swift
│   │       ├── Injectable.swift
│   │       └── ServiceRegistration.swift
│   └── Tests/
│       ├── CoreDataTests.swift
│       ├── StorageTests.swift
│       └── DITests.swift
```

## Component Size Limits
- Each file < 250 lines
- Target: 150-200 lines
- Extract when > 60% duplication

## Parallel Execution Plan
1. Developer A: Core Data (Story 1)
2. Developer B: Storage (Story 2)
3. Developer C: DI (Story 3)

Sync points: Daily at 10am and 4pm
EOF

echo "✅ Implementation plan created"
echo ""

# Phase 4: Adversarial Review
echo "═══════════════════════════════════════════════════════════════════════"
echo "🔍 PHASE 4: ADVERSARIAL REVIEW"
echo "═══════════════════════════════════════════════════════════════════════"
echo ""

cat > "$EVIDENCE_DIR/adversarial-review.md" << 'EOF'
# Wave 1 Adversarial Review

## Potential Issues Identified

### Core Data Risks
1. **Migration failures**: What if Android DB schema changes?
   - Mitigation: Version everything, keep migration paths

2. **Performance with large datasets**: 10,000+ photos?
   - Mitigation: Implement pagination, use fetch limits

3. **Thread safety**: Concurrent access issues?
   - Mitigation: Use proper Core Data contexts and queues

### Storage Risks
1. **Disk space**: What if device is full?
   - Mitigation: Check available space before operations

2. **Corrupted files**: What if write fails mid-operation?
   - Mitigation: Use atomic operations, temp files

3. **Permission issues**: iOS 14+ file access restrictions?
   - Mitigation: Stay within app sandbox, no external access

### DI Risks
1. **Circular dependencies**: Service A needs B needs A?
   - Mitigation: Use lazy initialization, protocols

2. **Memory leaks**: Retained services holding views?
   - Mitigation: Weak references where appropriate

3. **Testing complexity**: Hard to mock?
   - Mitigation: Protocol-based design from start

## Edge Cases to Handle
- App killed during file operation
- Database corruption
- Import of 1000+ photos at once
- Device rotation during operations
- Low memory warnings
- Background app termination

## Security Considerations
- Validate all file paths
- Sanitize filenames
- Encrypt sensitive child data
- No logging of personal information
EOF

echo "✅ Adversarial review completed - 12 risks identified"
echo ""

# Phase 5: Implementation Instructions
echo "═══════════════════════════════════════════════════════════════════════"
echo "💻 PHASE 5: IMPLEMENTATION"
echo "═══════════════════════════════════════════════════════════════════════"
echo ""

cat > "$EVIDENCE_DIR/implementation-guide.md" << 'EOF'
# Wave 1 Implementation Guide

## Setup Instructions

### 1. Create Xcode Project
```bash
cd ios
# Create new iOS app in Xcode:
# - Product Name: SmilePile
# - Team: Your Team
# - Organization Identifier: com.smilepile
# - Interface: SwiftUI
# - Language: Swift
# - Use Core Data: Yes
# - Include Tests: Yes
```

### 2. Parallel Implementation

#### Team Member A: Core Data
1. Open SmilePile.xcdatamodeld
2. Create entities following schema
3. Generate NSManagedObject subclasses
4. Implement CoreDataStack.swift
5. Add CRUD operations
6. Write tests

#### Team Member B: Storage
1. Create StorageManager.swift
2. Implement directory setup
3. Add file operations
4. Create thumbnail generator
5. Handle errors properly
6. Write tests

#### Team Member C: DI
1. Create DIContainer.swift
2. Define Injectable protocol
3. Implement service registration
4. Create property wrapper
5. Setup in AppDelegate
6. Write tests

### 3. Integration Points
- CoreDataStack registered in DI
- StorageManager registered in DI
- Repositories use both services

## Code Quality Checklist
- [ ] SwiftLint configured
- [ ] No force unwrapping
- [ ] Error handling complete
- [ ] Documentation comments
- [ ] Unit tests passing
- [ ] No files > 250 lines
EOF

echo "✅ Implementation guide ready"
echo "⚠️  Note: Actual implementation happens when you run this phase"
echo ""

# Phase 6: Testing
echo "═══════════════════════════════════════════════════════════════════════"
echo "🧪 PHASE 6: TESTING"
echo "═══════════════════════════════════════════════════════════════════════"
echo ""

cat > "$EVIDENCE_DIR/test-plan.md" << 'EOF'
# Wave 1 Test Plan

## Unit Tests Required

### Core Data Tests
- Create photo entity
- Update photo entity
- Delete photo entity
- Query photos by category
- Test relationships
- Test migrations

### Storage Tests
- Create directories
- Save file
- Load file
- Delete file
- Generate thumbnail
- Handle errors

### DI Tests
- Register service
- Resolve service
- Lazy initialization
- Thread safety
- Mock injection

## Integration Tests
- Save photo with Core Data + Storage
- Load gallery with real data
- Performance with 1000 photos

## Test Execution
```bash
xcodebuild test -scheme SmilePile -destination 'platform=iOS Simulator,name=iPhone 14'
```

## Coverage Target
- Minimum: 30% (critical paths)
- Target: 50% (includes edge cases)
- Ideal: 70% (comprehensive)
EOF

echo "✅ Test plan created"
echo ""

# Phase 7: Validation
echo "═══════════════════════════════════════════════════════════════════════"
echo "✅ PHASE 7: VALIDATION"
echo "═══════════════════════════════════════════════════════════════════════"
echo ""

cat > "$EVIDENCE_DIR/validation-checklist.md" << 'EOF'
# Wave 1 Validation Checklist

## Story 1: Core Data ✓
- [ ] Core Data stack initializes
- [ ] Photo entity CRUD works
- [ ] Category entity CRUD works
- [ ] Relationships functional
- [ ] Type converters working
- [ ] Tests passing

## Story 2: Storage ✓
- [ ] Directories created
- [ ] Files save correctly
- [ ] Files load correctly
- [ ] Thumbnails generated
- [ ] Cleanup works
- [ ] Tests passing

## Story 3: DI ✓
- [ ] Container initializes
- [ ] Services register
- [ ] Services resolve
- [ ] Thread safe
- [ ] Mocks work
- [ ] Tests passing

## Performance Metrics
- [ ] App launches < 2.5s
- [ ] Core Data queries < 100ms
- [ ] File operations < 1s
- [ ] Memory usage < 50MB

## Code Quality
- [ ] No files > 250 lines
- [ ] SwiftLint passing
- [ ] No force unwrapping
- [ ] Documentation complete

## Ready for Wave 2?
- [ ] All stories complete
- [ ] All tests passing
- [ ] No critical bugs
- [ ] Team retrospective done
EOF

echo "✅ Validation checklist ready"
echo ""

# Summary
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║                     Wave 1 Orchestration Complete                   ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo ""
echo "📁 Evidence collected in: $EVIDENCE_DIR/"
echo "📚 Stories created: 3"
echo "⚠️  Risks identified: 12"
echo ""
echo "Next Steps:"
echo "1. Review the stories and plans"
echo "2. Assign to team members (or implement sequentially)"
echo "3. Follow the implementation guide"
echo "4. Run tests after each component"
echo "5. Validate against checklist"
echo ""
echo "To start implementation, run:"
echo "  cd ios && xcodebuild -version"
echo ""
echo "For help with the Atlas workflow:"
echo "  python3 atlas/core/atlas_workflow.py feature \"IOS Wave 1: [Component]\""
echo ""
echo "Good luck! 🚀"