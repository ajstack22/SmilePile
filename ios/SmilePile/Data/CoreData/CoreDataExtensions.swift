import CoreData
import Foundation

// MARK: - NSManagedObject Extensions

extension NSManagedObject {
    static var entityName: String {
        return String(describing: self)
    }
}

// MARK: - NSManagedObjectContext Extensions

extension NSManagedObjectContext {
    func saveIfNeeded() throws {
        guard hasChanges else { return }
        try save()
    }

    func fetchFirst<T: NSManagedObject>(_ entityType: T.Type, predicate: NSPredicate? = nil) throws -> T? {
        let request = NSFetchRequest<T>(entityName: entityType.entityName)
        request.predicate = predicate
        request.fetchLimit = 1
        return try fetch(request).first
    }

    func fetchAll<T: NSManagedObject>(_ entityType: T.Type, predicate: NSPredicate? = nil, sortDescriptors: [NSSortDescriptor]? = nil) throws -> [T] {
        let request = NSFetchRequest<T>(entityName: entityType.entityName)
        request.predicate = predicate
        request.sortDescriptors = sortDescriptors
        return try fetch(request)
    }

    func count<T: NSManagedObject>(_ entityType: T.Type, predicate: NSPredicate? = nil) throws -> Int {
        let request = NSFetchRequest<T>(entityName: entityType.entityName)
        request.predicate = predicate
        return try count(for: request)
    }

    func deleteAll<T: NSManagedObject>(_ entityType: T.Type, predicate: NSPredicate? = nil) throws {
        let request = NSFetchRequest<T>(entityName: entityType.entityName)
        request.predicate = predicate

        let objects = try fetch(request)
        objects.forEach { delete($0) }
    }
}

// MARK: - NSFetchRequest Extensions

extension NSFetchRequest {
    @objc func withPredicate(_ predicate: NSPredicate) -> Self {
        self.predicate = predicate
        return self
    }

    @objc func withSortDescriptors(_ sortDescriptors: [NSSortDescriptor]) -> Self {
        self.sortDescriptors = sortDescriptors
        return self
    }

    @objc func withLimit(_ limit: Int) -> Self {
        self.fetchLimit = limit
        return self
    }

    @objc func withBatchSize(_ batchSize: Int) -> Self {
        self.fetchBatchSize = batchSize
        return self
    }
}

// MARK: - Sort Descriptor Helpers

extension NSSortDescriptor {
    static func timestamp(ascending: Bool = false) -> NSSortDescriptor {
        NSSortDescriptor(key: "timestamp", ascending: ascending)
    }

    static func createdAt(ascending: Bool = true) -> NSSortDescriptor {
        NSSortDescriptor(key: "createdAt", ascending: ascending)
    }

    static func displayName(ascending: Bool = true) -> NSSortDescriptor {
        NSSortDescriptor(key: "displayName", ascending: ascending, selector: #selector(NSString.caseInsensitiveCompare(_:)))
    }

    static func position(ascending: Bool = true) -> NSSortDescriptor {
        NSSortDescriptor(key: "position", ascending: ascending)
    }
}

// MARK: - Migration Helpers

extension NSPersistentStoreCoordinator {
    func destroyStore(at url: URL) throws {
        do {
            try destroyPersistentStore(at: url, type: .sqlite, options: nil)
        } catch {
            throw CoreDataError.migrationFailed("Failed to destroy store at \(url): \(error.localizedDescription)")
        }
    }

    func replaceStore(at targetURL: URL, withStoreAt sourceURL: URL) throws {
        do {
            try replacePersistentStore(at: targetURL, destinationOptions: nil,
                                      withPersistentStoreFrom: sourceURL,
                                      sourceOptions: nil,
                                      type: .sqlite)
        } catch {
            throw CoreDataError.migrationFailed("Failed to replace store: \(error.localizedDescription)")
        }
    }
}

// MARK: - Batch Update Helpers

extension NSBatchUpdateRequest {
    static func update<T: NSManagedObject>(_ entityType: T.Type, predicate: NSPredicate? = nil, with properties: [String: Any]) -> NSBatchUpdateRequest {
        let request = NSBatchUpdateRequest(entity: T.entity())
        request.predicate = predicate
        request.propertiesToUpdate = properties
        request.resultType = .updatedObjectIDsResultType
        return request
    }
}

// MARK: - Fetch Request Result Type

enum FetchRequestResultType {
    case managedObject
    case managedObjectID
    case dictionary
    case count

    var nsValue: NSFetchRequestResultType {
        switch self {
        case .managedObject:
            return .managedObjectResultType
        case .managedObjectID:
            return .managedObjectIDResultType
        case .dictionary:
            return .dictionaryResultType
        case .count:
            return .countResultType
        }
    }
}