import CoreData
import Combine
import Foundation
import os.log

final class PhotoRepositoryImpl: PhotoRepository {
    private let coreDataStack: CoreDataStack
    private let logger = Logger(subsystem: "com.smilepile", category: "PhotoRepository")
    private var cancellables = Set<AnyCancellable>()

    init(coreDataStack: CoreDataStack = CoreDataStack.shared) {
        self.coreDataStack = coreDataStack
    }

    // MARK: - CRUD Operations

    func insertPhoto(_ photo: Photo) async throws -> Int64 {
        guard photo.categoryId > 0 else {
            throw PhotoRepositoryError.invalidCategory("Photo must have a valid category. Category ID: \(photo.categoryId)")
        }

        return try await coreDataStack.performBackgroundTask { context in
            let photoEntity = PhotoEntity(context: context)
            // Use PhotoIDGenerator for consistent ID generation
            let photoId = photo.id == 0 ? PhotoIDGenerator.generateUniqueID() : photo.id
            photoEntity.id = String(photoId)
            photoEntity.uri = photo.path
            photoEntity.categoryId = photo.categoryId
            photoEntity.timestamp = photo.createdAt

            try self.coreDataStack.saveContext(context)
            self.logger.info("Photo inserted successfully with ID: \(photoId), path: \(photo.path), categoryId: \(photo.categoryId)")
            return photoId // Return the actual photo ID, not categoryId
        }
    }

    func insertPhotos(_ photos: [Photo]) async throws {
        let invalidPhotos = photos.filter { $0.categoryId <= 0 }
        guard invalidPhotos.isEmpty else {
            throw PhotoRepositoryError.invalidCategory("All photos must have valid categories. \(invalidPhotos.count) photos have invalid category IDs.")
        }

        try await coreDataStack.performBackgroundTask { context in
            for photo in photos {
                let photoEntity = PhotoEntity(context: context)
                // Use PhotoIDGenerator for consistent ID generation
                let photoId = photo.id == 0 ? PhotoIDGenerator.generateUniqueID() : photo.id
                photoEntity.id = String(photoId)
                photoEntity.uri = photo.path
                photoEntity.categoryId = photo.categoryId
                photoEntity.timestamp = photo.createdAt
            }

            try self.coreDataStack.saveContext(context)
            self.logger.info("Inserted \(photos.count) photos successfully")
        }
    }

    func updatePhoto(_ photo: Photo) async throws {
        print("📝 PhotoRepository: updatePhoto called with id: \(photo.id), categoryId: \(photo.categoryId), path: \(photo.path)")
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            request.predicate = NSPredicate(format: "id == %@", String(photo.id))
            request.fetchLimit = 1

            guard let photoEntity = try context.fetch(request).first else {
                print("❌ PhotoRepository: Photo not found with id: \(photo.id)")
                throw PhotoRepositoryError.notFound("Photo not found with id: \(photo.id)")
            }

            let oldCategoryId = photoEntity.categoryId
            photoEntity.uri = photo.path
            photoEntity.categoryId = photo.categoryId
            photoEntity.timestamp = photo.createdAt

            try self.coreDataStack.saveContext(context)
            print("✅ PhotoRepository: Photo updated successfully - id: \(photo.id), categoryId changed from \(oldCategoryId) to \(photo.categoryId)")
            self.logger.debug("Photo updated successfully: \(photo.id)")
        }
    }

    func deletePhoto(_ photo: Photo) async throws {
        try await deletePhotoById(photo.id)
    }

    func deletePhotoById(_ photoId: Int64) async throws {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            request.predicate = NSPredicate(format: "id == %@", String(photoId))

            let photos = try context.fetch(request)
            for photo in photos {
                context.delete(photo)
            }

            try self.coreDataStack.saveContext(context)
            self.logger.debug("Photo deleted successfully: \(photoId)")
        }
    }

    // MARK: - Retrieval Operations

    func getPhotoById(_ photoId: Int64) async throws -> Photo? {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            request.predicate = NSPredicate(format: "id == %@", String(photoId))
            request.fetchLimit = 1

            guard let entity = try context.fetch(request).first else {
                return nil
            }

            return self.entityToPhoto(entity)
        }
    }

    func getPhotoByPath(_ path: String) async throws -> Photo? {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            request.predicate = NSPredicate(format: "uri == %@", path)
            request.fetchLimit = 1

            guard let entity = try context.fetch(request).first else {
                return nil
            }

            return self.entityToPhoto(entity)
        }
    }

    func getAllPhotos() async throws -> [Photo] {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            request.sortDescriptors = [NSSortDescriptor(keyPath: \PhotoEntity.timestamp, ascending: false)]

            let entities = try context.fetch(request)
            let photos = entities.compactMap { self.entityToPhoto($0) }
            self.logger.info("Retrieved \(photos.count) total photos from database")
            return photos
        }
    }

    func getAllPhotosFlow() -> AnyPublisher<[Photo], Error> {
        let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
        request.sortDescriptors = [NSSortDescriptor(keyPath: \PhotoEntity.timestamp, ascending: false)]

        return coreDataStack.viewContext.publisher(for: request)
            .map { entities in
                entities.compactMap { self.entityToPhoto($0) }
            }
            .mapError { error in
                PhotoRepositoryError.fetchFailed("Failed to fetch photos: \(error.localizedDescription)")
            }
            .eraseToAnyPublisher()
    }

    // MARK: - Category Operations

    func getPhotosByCategory(_ categoryId: Int64) async throws -> [Photo] {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            request.predicate = NSPredicate(format: "categoryId == %ld", categoryId)
            request.sortDescriptors = [NSSortDescriptor(keyPath: \PhotoEntity.timestamp, ascending: false)]

            let entities = try context.fetch(request)
            let photos = entities.compactMap { self.entityToPhoto($0) }
            self.logger.info("Retrieved \(photos.count) photos for category \(categoryId)")
            return photos
        }
    }

    func getPhotosByCategoryFlow(_ categoryId: Int64) -> AnyPublisher<[Photo], Error> {
        let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
        request.predicate = NSPredicate(format: "categoryId == %ld", categoryId)
        request.sortDescriptors = [NSSortDescriptor(keyPath: \PhotoEntity.timestamp, ascending: false)]

        return coreDataStack.viewContext.publisher(for: request)
            .map { entities in
                entities.compactMap { self.entityToPhoto($0) }
            }
            .mapError { error in
                PhotoRepositoryError.fetchFailed("Failed to fetch photos by category: \(error.localizedDescription)")
            }
            .eraseToAnyPublisher()
    }

    func deletePhotosByCategory(_ categoryId: Int64) async throws {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            request.predicate = NSPredicate(format: "categoryId == %ld", categoryId)

            let photos = try context.fetch(request)
            for photo in photos {
                context.delete(photo)
            }

            try self.coreDataStack.saveContext(context)
            self.logger.debug("Deleted \(photos.count) photos from category: \(categoryId)")
        }
    }

    func getPhotoCategoryCount(_ categoryId: Int64) async throws -> Int {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            request.predicate = NSPredicate(format: "categoryId == %ld", categoryId)
            return try context.count(for: request)
        }
    }

    func getPhotoCountByCategory(categoryId: Int64) async throws -> Int {
        return try await getPhotoCategoryCount(categoryId)
    }

    // MARK: - Utility Operations

    func getPhotoCount() async throws -> Int {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            return try context.count(for: request)
        }
    }

    func removeFromLibrary(_ photo: Photo) async throws {
        try await deletePhoto(photo)
    }

    func removeFromLibraryById(_ photoId: Int64) async throws {
        try await deletePhotoById(photoId)
    }

    // MARK: - Cleanup Operations

    func cleanupOrphanedPhotos() async throws -> Int {
        var deletedCount = 0

        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            let entities = try context.fetch(request)

            for entity in entities {
                if let uri = entity.uri {
                    // Fix the path first
                    let fixedPath: String
                    if uri.contains("/Containers/Data/Application/") {
                        if let documentsRange = uri.range(of: "/Documents/") {
                            let relativePath = String(uri[documentsRange.upperBound...])
                            let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                            fixedPath = documentsURL.appendingPathComponent(relativePath).path
                        } else {
                            fixedPath = uri
                        }
                    } else {
                        fixedPath = uri
                    }

                    // Check if the file exists
                    if !FileManager.default.fileExists(atPath: fixedPath) {
                        context.delete(entity)
                        deletedCount += 1
                        self.logger.info("Deleting orphaned photo record: \(uri)")
                    }
                } else {
                    // Delete entities with no URI
                    context.delete(entity)
                    deletedCount += 1
                }
            }

            if deletedCount > 0 {
                try self.coreDataStack.saveContext(context)
                self.logger.info("Cleaned up \(deletedCount) orphaned photo records")
            }
        }

        return deletedCount
    }

    // MARK: - Private Helpers

    private func entityToPhoto(_ entity: PhotoEntity) -> Photo? {
        guard let id = entity.id, let uri = entity.uri else {
            logger.warning("Photo entity missing required fields")
            return nil
        }

        // Convert string ID to Int64, handling both numeric strings and UUIDs
        let photoId: Int64
        if let numericId = Int64(id) {
            photoId = numericId
        } else {
            // Fallback for existing UUID-based IDs
            photoId = PhotoIDGenerator.idFromUUID(id)
            logger.debug("Converted UUID \(id) to numeric ID \(photoId)")
        }

        // Fix path if it points to an old app container
        let fixedPath: String
        if uri.contains("/Containers/Data/Application/") {
            // Extract the relative path after Documents/
            if let documentsRange = uri.range(of: "/Documents/") {
                let relativePath = String(uri[documentsRange.upperBound...])
                // Get current documents directory
                let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                fixedPath = documentsURL.appendingPathComponent(relativePath).path
                logger.debug("Fixed old container path: \(uri) -> \(fixedPath)")
            } else {
                // If we can't extract a relative path, use the original
                fixedPath = uri
            }
        } else {
            fixedPath = uri
        }

        let url = URL(fileURLWithPath: fixedPath)
        let name = url.deletingPathExtension().lastPathComponent

        return Photo(
            id: photoId,
            path: fixedPath,
            categoryId: entity.categoryId,
            name: name,
            isFromAssets: false,
            createdAt: entity.timestamp,
            fileSize: 0, // Will be calculated when needed
            width: 0,    // Will be calculated when needed
            height: 0    // Will be calculated when needed
        )
    }

    func getTotalPhotoCount() async throws -> Int {
        return try await coreDataStack.performBackgroundTask { context in
            let request: NSFetchRequest<PhotoEntity> = PhotoEntity.fetchRequest()
            return try context.count(for: request)
        }
    }
}