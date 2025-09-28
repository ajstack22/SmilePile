import Foundation
import CoreData
import os.log

/// Handles migration of existing photos from UUID-based IDs to Int64 IDs
final class PhotoIDMigration {

    private let logger = Logger(subsystem: "com.smilepile", category: "PhotoIDMigration")
    private let coreDataStack: CoreDataStack

    init(coreDataStack: CoreDataStack = CoreDataStack.shared) {
        self.coreDataStack = coreDataStack
    }

    /// Check if migration is needed
    func isMigrationNeeded() async -> Bool {
        let context = coreDataStack.newBackgroundContext()

        return await context.perform {
            let fetchRequest = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            fetchRequest.fetchLimit = 1
            fetchRequest.predicate = NSPredicate(format: "id CONTAINS[c] '-'") // UUID contains dashes

            do {
                let count = try context.count(for: fetchRequest)
                return count > 0
            } catch {
                self.logger.error("Failed to check migration status: \(error.localizedDescription)")
                return false
            }
        }
    }

    /// Perform migration of photo IDs from UUID to Int64
    @discardableResult
    func performMigration() async throws -> MigrationResult {
        logger.info("Starting photo ID migration")

        let startTime = Date()
        var migratedCount = 0
        var failedCount = 0
        var errors: [String] = []

        // Use performBackgroundTask for proper transaction handling
        try await coreDataStack.performBackgroundTask { context in
            context.automaticallyMergesChangesFromParent = false
            context.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy

            do {
                // Start transaction
                context.undoManager = UndoManager()

                // Fetch all photos with UUID-based IDs
                let fetchRequest = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
                fetchRequest.predicate = NSPredicate(format: "id CONTAINS[c] '-'")

                let photos = try context.fetch(fetchRequest)
                self.logger.info("Found \(photos.count) photos to migrate")

                // Track all new IDs to ensure no duplicates
                var usedIds = Set<Int64>()

                // First pass: collect all existing numeric IDs
                let existingRequest = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
                existingRequest.predicate = NSPredicate(format: "NOT (id CONTAINS[c] '-')")
                let existingPhotos = try context.fetch(existingRequest)
                for photo in existingPhotos {
                    if let idString = photo.id, let id = Int64(idString) {
                        usedIds.insert(id)
                    }
                }

                // Process all photos in single transaction
                for photo in photos {
                    guard let oldId = photo.id else { continue }

                    // Generate new Int64 ID
                    var newId = PhotoIDGenerator.idFromUUID(oldId)

                    // Ensure no conflicts
                    while usedIds.contains(newId) {
                        newId = PhotoIDGenerator.generateUniqueID()
                        self.logger.debug("ID conflict for \(oldId), regenerating")
                    }

                    usedIds.insert(newId)
                    photo.id = String(newId)
                    migratedCount += 1

                    self.logger.debug("Migrated photo \(oldId) to \(newId)")
                }

                // Attempt to save all changes atomically
                if context.hasChanges {
                    try context.save()
                    self.logger.info("Successfully saved all \(migratedCount) migrations")
                }

            } catch {
                // Rollback on failure
                context.rollback()
                self.logger.error("Migration failed, rolled back: \(error.localizedDescription)")
                throw error
            }
        }

        let duration = Date().timeIntervalSince(startTime)

        let result = MigrationResult(
            totalPhotos: migratedCount + failedCount,
            migratedCount: migratedCount,
            failedCount: failedCount,
            errors: errors,
            duration: duration
        )

        logger.info("Migration completed: \(result.summary)")

        // Save migration completion flag only if successful
        if result.isSuccessful {
            UserDefaults.standard.set(true, forKey: "PhotoIDMigrationCompleted")
            UserDefaults.standard.set(Date(), forKey: "PhotoIDMigrationDate")
        }

        return result
    }

    /// Check if migration has already been completed
    func isMigrationCompleted() -> Bool {
        return UserDefaults.standard.bool(forKey: "PhotoIDMigrationCompleted")
    }

    /// Reset migration status (for testing)
    func resetMigrationStatus() {
        UserDefaults.standard.removeObject(forKey: "PhotoIDMigrationCompleted")
        UserDefaults.standard.removeObject(forKey: "PhotoIDMigrationDate")
    }

    struct MigrationResult {
        let totalPhotos: Int
        let migratedCount: Int
        let failedCount: Int
        let errors: [String]
        let duration: TimeInterval

        var isSuccessful: Bool {
            return failedCount == 0
        }

        var summary: String {
            return """
            Migration Result:
            - Total Photos: \(totalPhotos)
            - Migrated: \(migratedCount)
            - Failed: \(failedCount)
            - Duration: \(String(format: "%.2f", duration)) seconds
            - Success Rate: \(String(format: "%.1f%%", Double(migratedCount) / Double(max(totalPhotos, 1)) * 100))
            """
        }
    }
}

// MARK: - App Launch Integration

extension PhotoIDMigration {

    /// Run migration at app launch if needed
    static func runMigrationIfNeeded() async {
        let migration = PhotoIDMigration()

        // Check if migration has already been completed
        if migration.isMigrationCompleted() {
            migration.logger.debug("Photo ID migration already completed")
            return
        }

        // Check if migration is needed
        let needsMigration = await migration.isMigrationNeeded()
        if !needsMigration {
            migration.logger.debug("No photo ID migration needed")
            UserDefaults.standard.set(true, forKey: "PhotoIDMigrationCompleted")
            return
        }

        // Perform migration
        do {
            let result = try await migration.performMigration()
            if result.isSuccessful {
                migration.logger.info("Photo ID migration completed successfully")
            } else {
                migration.logger.warning("Photo ID migration completed with \(result.failedCount) errors")
            }
        } catch {
            migration.logger.error("Photo ID migration failed: \(error.localizedDescription)")
        }
    }
}