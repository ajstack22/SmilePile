import CoreData
import Combine
import os.log

final class CoreDataStack: ObservableObject {
    static let shared = CoreDataStack()

    private let logger = Logger(subsystem: "com.smilepile", category: "CoreData")

    lazy var persistentContainer: NSPersistentContainer = {
        let container = NSPersistentContainer(name: "SmilePile")

        // Configure for WAL journaling mode for better performance
        if let storeURL = container.persistentStoreDescriptions.first?.url {
            container.persistentStoreDescriptions.first?.setValue("WAL" as NSObject,
                                                                 forPragmaNamed: "journal_mode")
        }

        // Enable automatic migration
        container.persistentStoreDescriptions.forEach { storeDescription in
            storeDescription.setOption(true as NSNumber,
                                       forKey: NSMigratePersistentStoresAutomaticallyOption)
            storeDescription.setOption(true as NSNumber,
                                       forKey: NSInferMappingModelAutomaticallyOption)
        }

        container.loadPersistentStores { [weak self] (storeDescription, error) in
            if let error = error as NSError? {
                self?.logger.critical("Failed to load Core Data stack: \(error), \(error.userInfo)")
                fatalError("Unresolved Core Data error \(error), \(error.userInfo)")
            }

            self?.logger.info("Core Data stack loaded successfully at: \(storeDescription.url?.absoluteString ?? "unknown")")
        }

        container.viewContext.automaticallyMergesChangesFromParent = true

        // Set merge policy to handle conflicts
        container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy

        return container
    }()

    var viewContext: NSManagedObjectContext {
        persistentContainer.viewContext
    }

    func newBackgroundContext() -> NSManagedObjectContext {
        let context = persistentContainer.newBackgroundContext()
        context.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        return context
    }

    func save() {
        let context = persistentContainer.viewContext

        if context.hasChanges {
            do {
                try context.save()
                logger.debug("Core Data context saved successfully")
            } catch {
                let nsError = error as NSError
                logger.error("Failed to save Core Data context: \(nsError), \(nsError.userInfo)")
                fatalError("Unresolved Core Data save error \(nsError), \(nsError.userInfo)")
            }
        }
    }

    func saveContext(_ context: NSManagedObjectContext) throws {
        if context.hasChanges {
            do {
                try context.save()
                logger.debug("Background context saved successfully")
            } catch {
                logger.error("Failed to save background context: \(error.localizedDescription)")
                throw error
            }
        }
    }

    func performBackgroundTask<T>(_ block: @escaping (NSManagedObjectContext) throws -> T) async throws -> T {
        try await withCheckedThrowingContinuation { continuation in
            persistentContainer.performBackgroundTask { context in
                context.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy

                do {
                    let result = try block(context)
                    continuation.resume(returning: result)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    // MARK: - Migration Support

    func migrateIfNeeded() async throws {
        // Check current model version
        let metadata = try NSPersistentStoreCoordinator.metadataForPersistentStore(
            ofType: NSSQLiteStoreType,
            at: storeURL,
            options: nil
        )

        let currentModel = persistentContainer.managedObjectModel

        if !currentModel.isConfiguration(withName: nil, compatibleWithStoreMetadata: metadata) {
            logger.info("Core Data migration needed")
            try await performMigration()
        } else {
            logger.info("Core Data model is up to date")
        }
    }

    private func performMigration() async throws {
        // Implement custom migration logic here if lightweight migration fails
        // For now, we rely on automatic lightweight migration
        logger.info("Performing Core Data migration...")
    }

    // MARK: - Helper Methods

    private var storeURL: URL {
        let storeDirectory = NSPersistentContainer.defaultDirectoryURL()
        return storeDirectory.appendingPathComponent("SmilePile.sqlite")
    }

    func resetDatabase() throws {
        guard let storeURL = persistentContainer.persistentStoreDescriptions.first?.url else {
            throw CoreDataError.storeNotFound
        }

        let coordinator = persistentContainer.persistentStoreCoordinator

        for store in coordinator.persistentStores {
            try coordinator.remove(store)
        }

        try FileManager.default.removeItem(at: storeURL)

        // Reload the store
        persistentContainer.loadPersistentStores { [weak self] (_, error) in
            if let error = error {
                self?.logger.critical("Failed to reload store after reset: \(error.localizedDescription)")
            }
        }
    }

    // MARK: - Batch Operations

    func batchDelete<T: NSManagedObject>(_ entityType: T.Type, predicate: NSPredicate? = nil) async throws -> Int {
        let entityName = String(describing: entityType)
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: entityName)
        fetchRequest.predicate = predicate

        let deleteRequest = NSBatchDeleteRequest(fetchRequest: fetchRequest)
        deleteRequest.resultType = .resultTypeCount

        return try await performBackgroundTask { context in
            let result = try context.execute(deleteRequest) as? NSBatchDeleteResult
            return result?.result as? Int ?? 0
        }
    }

    // MARK: - Statistics

    func entityCount<T: NSManagedObject>(_ entityType: T.Type, predicate: NSPredicate? = nil) async throws -> Int {
        let entityName = String(describing: entityType)
        let fetchRequest = NSFetchRequest<T>(entityName: entityName)
        fetchRequest.predicate = predicate

        return try await performBackgroundTask { context in
            try context.count(for: fetchRequest)
        }
    }
}

// MARK: - Core Data Errors

enum CoreDataError: LocalizedError {
    case storeNotFound
    case migrationFailed(String)
    case saveFailed(String)
    case fetchFailed(String)
    case invalidEntity(String)

    var errorDescription: String? {
        switch self {
        case .storeNotFound:
            return "Core Data store not found"
        case .migrationFailed(let message):
            return "Migration failed: \(message)"
        case .saveFailed(let message):
            return "Save failed: \(message)"
        case .fetchFailed(let message):
            return "Fetch failed: \(message)"
        case .invalidEntity(let message):
            return "Invalid entity: \(message)"
        }
    }
}