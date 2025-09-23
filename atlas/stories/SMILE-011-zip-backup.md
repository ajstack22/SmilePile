# SMILE-011: Internal Photo Storage with ZIP Export/Import

## Story Overview
As a SmilePile user, I want to create complete backups of my app data including photos so that I can transfer my data to a new device or restore after reinstalling the app.

## Acceptance Criteria
- [ ] Photos are stored exclusively in internal storage for privacy
- [ ] Export creates a ZIP file containing metadata.json and all photos
- [ ] Import restores complete app state from ZIP file
- [ ] Backward compatibility with v1 JSON backups maintained
- [ ] Export completes in < 30 seconds for 100 photos
- [ ] Progress indicators show during export/import operations
- [ ] Security protections prevent ZIP bombs and path traversal attacks

## Technical Requirements
- Implement ZipUtils for secure ZIP operations
- Update BackupManager to support ZIP format (v2)
- Consolidate all storage to internal only
- Create comprehensive test coverage
- Maintain Material 3 design standards

## Implementation Phases
1. Analysis & Research
2. Core Implementation (ZipUtils, BackupManager)
3. UI Integration (Settings screen)
4. Storage Consolidation
5. Testing & Validation

## Success Metrics
- Zero data loss during export/import
- Performance within acceptable limits
- All tests passing
- No security vulnerabilities