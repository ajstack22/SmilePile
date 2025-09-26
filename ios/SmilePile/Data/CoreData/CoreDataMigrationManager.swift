import CoreData
import os.log

/// Manages CoreData migrations with safety checks and rollback capability
final class CoreDataMigrationManager {

    // MARK: - Properties

    private let logger = Logger(subsystem: "com.smilepile", category: "CoreDataMigration")
    private let modelName = "SmilePile"
    private let storeType = NSSQLiteStoreType

    // Backup configuration
    private let maxBackups = 3
    private let backupDirectory: URL

    // MARK: - Initialization

    init() {
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        self.backupDirectory = documentsURL.appendingPathComponent("CoreDataBackups", isDirectory: true)

        // Create backup directory
        try? FileManager.default.createDirectory(at: backupDirectory, withIntermediateDirectories: true)
    }

    // MARK: - Public Methods

    /// Perform safe migration with backup and rollback capability
    func performSafeMigration(for container: NSPersistentContainer) async throws {
        let storeURL = defaultStoreURL()

        // Check if migration is needed
        guard isMigrationNeeded(at: storeURL, for: container.managedObjectModel) else {
            logger.info("No migration needed")
            return
        }

        logger.info("Migration needed - starting safe migration process")

        // Create backup before migration
        let backupURL = try createBackup(of: storeURL)
        logger.info("Created backup at: \(backupURL.lastPathComponent)")

        do {
            // Attempt migration
            try await migrate(from: storeURL, to: container)
            logger.info("Migration completed successfully")

            // Clean old backups
            cleanOldBackups()
        } catch {
            logger.error("Migration failed: \(error.localizedDescription)")

            // Attempt rollback
            try rollback(from: backupURL, to: storeURL)
            logger.info("Rollback completed")

            throw MigrationError.migrationFailed(error.localizedDescription)
        }
    }

    /// Check if migration is needed
    func isMigrationNeeded(at storeURL: URL, for model: NSManagedObjectModel) -> Bool {
        guard FileManager.default.fileExists(atPath: storeURL.path) else {
            // No existing store, no migration needed
            return false
        }

        do {
            let metadata = try NSPersistentStoreCoordinator.metadataForPersistentStore(
                ofType: storeType,
                at: storeURL,
                options: nil
            )

            return !model.isConfiguration(withName: nil, compatibleWithStoreMetadata: metadata)
        } catch {
            logger.error("Failed to check migration status: \(error.localizedDescription)")
            return false
        }
    }

    /// Create new model version for PhotoImportSession entity
    func createUpdatedModel() -> NSManagedObjectModel {
        guard let modelURL = Bundle.main.url(forResource: modelName, withExtension: "momd"),
              let model = NSManagedObjectModel(contentsOf: modelURL) else {
            fatalError("Failed to load CoreData model")
        }

        // Check if PhotoImportSession entity exists
        if model.entitiesByName["PhotoImportSession"] == nil {
            // Add PhotoImportSession entity
            addPhotoImportSessionEntity(to: model)
        }

        return model
    }

    // MARK: - Private Migration Methods

    private func migrate(from sourceURL: URL, to container: NSPersistentContainer) async throws {
        let coordinator = container.persistentStoreCoordinator

        // Configure migration options
        let options: [String: Any] = [
            NSMigratePersistentStoresAutomaticallyOption: true,
            NSInferMappingModelAutomaticallyOption: true,
            NSSQLitePragmasOption: ["journal_mode": "WAL"],
            NSPersistentHistoryTrackingKey: true,
            NSPersistentStoreRemoteChangeNotificationPostOptionKey: true
        ]

        // Load store with migration
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            do {
                _ = try coordinator.addPersistentStore(
                    ofType: storeType,
                    configurationName: nil,
                    at: sourceURL,
                    options: options
                )
                continuation.resume()
            } catch {
                continuation.resume(throwing: error)
            }
        }

        // Verify migration
        try verifyMigration(for: container)
    }

    private func verifyMigration(for container: NSPersistentContainer) throws {
        let context = container.viewContext

        // Test basic fetch operations
        let photoFetch = NSFetchRequest<NSFetchRequestResult>(entityName: "PhotoEntity")
        photoFetch.fetchLimit = 1
        _ = try context.fetch(photoFetch)

        let categoryFetch = NSFetchRequest<NSFetchRequestResult>(entityName: "CategoryEntity")
        categoryFetch.fetchLimit = 1
        _ = try context.fetch(categoryFetch)

        // Test PhotoImportSession if it exists
        if container.managedObjectModel.entitiesByName["PhotoImportSession"] != nil {
            let sessionFetch = NSFetchRequest<NSFetchRequestResult>(entityName: "PhotoImportSession")
            sessionFetch.fetchLimit = 1
            _ = try context.fetch(sessionFetch)
        }

        logger.info("Migration verification passed")
    }

    // MARK: - Backup Methods

    private func createBackup(of storeURL: URL) throws -> URL {
        let timestamp = DateFormatter.backupDateFormatter.string(from: Date())
        let backupName = "SmilePile_backup_\(timestamp).sqlite"
        let backupURL = backupDirectory.appendingPathComponent(backupName)

        // Copy main database file
        try FileManager.default.copyItem(at: storeURL, to: backupURL)

        // Copy WAL and SHM files if they exist
        let walURL = storeURL.appendingPathExtension("wal")
        let shmURL = storeURL.appendingPathExtension("shm")

        if FileManager.default.fileExists(atPath: walURL.path) {
            try FileManager.default.copyItem(
                at: walURL,
                to: backupURL.appendingPathExtension("wal")
            )
        }

        if FileManager.default.fileExists(atPath: shmURL.path) {
            try FileManager.default.copyItem(
                at: shmURL,
                to: backupURL.appendingPathExtension("shm")
            )
        }

        logger.info("Backup created: \(backupName)")
        return backupURL
    }

    private func rollback(from backupURL: URL, to storeURL: URL) throws {
        // Remove corrupted store
        try? FileManager.default.removeItem(at: storeURL)
        try? FileManager.default.removeItem(at: storeURL.appendingPathExtension("wal"))
        try? FileManager.default.removeItem(at: storeURL.appendingPathExtension("shm"))

        // Restore from backup
        try FileManager.default.copyItem(at: backupURL, to: storeURL)

        // Restore WAL and SHM if they exist
        let backupWAL = backupURL.appendingPathExtension("wal")
        let backupSHM = backupURL.appendingPathExtension("shm")

        if FileManager.default.fileExists(atPath: backupWAL.path) {
            try FileManager.default.copyItem(
                at: backupWAL,
                to: storeURL.appendingPathExtension("wal")
            )
        }

        if FileManager.default.fileExists(atPath: backupSHM.path) {
            try FileManager.default.copyItem(
                at: backupSHM,
                to: storeURL.appendingPathExtension("shm")
            )
        }

        logger.info("Rollback completed from backup")
    }

    private func cleanOldBackups() {
        do {
            let backupFiles = try FileManager.default.contentsOfDirectory(
                at: backupDirectory,
                includingPropertiesForKeys: [.creationDateKey],
                options: .skipsHiddenFiles
            )

            let sqliteBackups = backupFiles.filter { $0.pathExtension == "sqlite" }
                .sorted { url1, url2 in
                    let date1 = (try? url1.resourceValues(forKeys: [.creationDateKey]).creationDate) ?? Date.distantPast
                    let date2 = (try? url2.resourceValues(forKeys: [.creationDateKey]).creationDate) ?? Date.distantPast
                    return date1 > date2
                }

            // Keep only the most recent backups
            if sqliteBackups.count > maxBackups {
                for backupURL in sqliteBackups[maxBackups...] {
                    try FileManager.default.removeItem(at: backupURL)
                    try? FileManager.default.removeItem(at: backupURL.appendingPathExtension("wal"))
                    try? FileManager.default.removeItem(at: backupURL.appendingPathExtension("shm"))
                    logger.debug("Removed old backup: \(backupURL.lastPathComponent)")
                }
            }
        } catch {
            logger.error("Failed to clean old backups: \(error.localizedDescription)")
        }
    }

    // MARK: - Entity Creation

    private func addPhotoImportSessionEntity(to model: NSManagedObjectModel) {
        let entity = NSEntityDescription()
        entity.name = "PhotoImportSession"
        entity.managedObjectClassName = "PhotoImportSession"

        // Add attributes
        let attributes: [(name: String, type: NSAttributeType, isOptional: Bool)] = [
            ("sessionId", .stringAttributeType, false),
            ("startedAt", .dateAttributeType, false),
            ("lastUpdatedAt", .dateAttributeType, false),
            ("status", .stringAttributeType, false),
            ("totalPhotos", .integer32AttributeType, false),
            ("processedPhotos", .integer32AttributeType, false),
            ("failedPhotos", .integer32AttributeType, false),
            ("pendingPhotoURLs", .stringAttributeType, true),
            ("processedPhotoIds", .stringAttributeType, true),
            ("failedPhotoURLs", .stringAttributeType, true),
            ("categoryId", .integer64AttributeType, false),
            ("errorMessage", .stringAttributeType, true)
        ]

        for (name, type, isOptional) in attributes {
            let attribute = NSAttributeDescription()
            attribute.name = name
            attribute.attributeType = type
            attribute.isOptional = isOptional
            entity.properties.append(attribute)
        }

        // Add entity to model
        model.entities.append(entity)

        logger.info("Added PhotoImportSession entity to model")
    }

    // MARK: - Helper Methods

    private func defaultStoreURL() -> URL {
        let storeDirectory = NSPersistentContainer.defaultDirectoryURL()
        return storeDirectory.appendingPathComponent("\(modelName).sqlite")
    }

    // MARK: - Migration Error

    enum MigrationError: LocalizedError {
        case migrationFailed(String)
        case backupFailed(String)
        case rollbackFailed(String)
        case verificationFailed(String)

        var errorDescription: String? {
            switch self {
            case .migrationFailed(let message):
                return "Migration failed: \(message)"
            case .backupFailed(let message):
                return "Backup failed: \(message)"
            case .rollbackFailed(let message):
                return "Rollback failed: \(message)"
            case .verificationFailed(let message):
                return "Verification failed: \(message)"
            }
        }
    }
}

// MARK: - DateFormatter Extension

private extension DateFormatter {
    static let backupDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd_HHmmss"
        return formatter
    }()
}