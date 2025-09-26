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
            photoEntity.id = photo.id == 0 ? UUID().uuidString : String(photo.id)
            photoEntity.uri = photo.path
            photoEntity.categoryId = photo.categoryId
            photoEntity.timestamp = photo.createdAt
            photoEntity.isFavorite = photo.isFavorite

            try self.coreDataStack.saveContext(context)
            self.logger.debug("Photo inserted successfully: \(photoEntity.id ?? "")")
            return photo.categoryId
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
                photoEntity.id = photo.id == 0 ? UUID().uuidString : String(photo.id)
                photoEntity.uri = photo.path
                photoEntity.categoryId = photo.categoryId
                photoEntity.timestamp = photo.createdAt
                photoEntity.isFavorite = photo.isFavorite
            }

            try self.coreDataStack.saveContext(context)
            self.logger.debug("Inserted \(photos.count) photos successfully")
        }
    }

    func updatePhoto(_ photo: Photo) async throws {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
            request.predicate = NSPredicate(format: "id == %@", String(photo.id))
            request.fetchLimit = 1

            guard let photoEntity = try context.fetch(request).first else {
                throw PhotoRepositoryError.notFound("Photo not found with id: \(photo.id)")
            }

            photoEntity.uri = photo.path
            photoEntity.categoryId = photo.categoryId
            photoEntity.timestamp = photo.createdAt
            photoEntity.isFavorite = photo.isFavorite

            try self.coreDataStack.saveContext(context)
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
            return entities.compactMap { self.entityToPhoto($0) }
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
            return entities.compactMap { self.entityToPhoto($0) }
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

    // MARK: - Search and Filtering

    func searchPhotos(_ searchQuery: String) -> AnyPublisher<[Photo], Error> {
        let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
        request.predicate = NSPredicate(format: "uri CONTAINS[cd] %@", searchQuery)
        request.sortDescriptors = [NSSortDescriptor(keyPath: \PhotoEntity.timestamp, ascending: false)]

        return coreDataStack.viewContext.publisher(for: request)
            .map { entities in
                entities.compactMap { self.entityToPhoto($0) }
            }
            .mapError { error in
                PhotoRepositoryError.fetchFailed("Failed to search photos: \(error.localizedDescription)")
            }
            .eraseToAnyPublisher()
    }

    func searchPhotosInCategory(_ searchQuery: String, categoryId: Int64) -> AnyPublisher<[Photo], Error> {
        let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
        request.predicate = NSCompoundPredicate(andPredicateWithSubpredicates: [
            NSPredicate(format: "uri CONTAINS[cd] %@", searchQuery),
            NSPredicate(format: "categoryId == %ld", categoryId)
        ])
        request.sortDescriptors = [NSSortDescriptor(keyPath: \PhotoEntity.timestamp, ascending: false)]

        return coreDataStack.viewContext.publisher(for: request)
            .map { entities in
                entities.compactMap { self.entityToPhoto($0) }
            }
            .mapError { error in
                PhotoRepositoryError.fetchFailed("Failed to search photos in category: \(error.localizedDescription)")
            }
            .eraseToAnyPublisher()
    }

    func getPhotosByDateRange(startDate: Int64, endDate: Int64) -> AnyPublisher<[Photo], Error> {
        let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
        request.predicate = NSPredicate(format: "timestamp BETWEEN %@", [startDate, endDate] as NSArray)
        request.sortDescriptors = [NSSortDescriptor(keyPath: \PhotoEntity.timestamp, ascending: false)]

        return coreDataStack.viewContext.publisher(for: request)
            .map { entities in
                entities.compactMap { self.entityToPhoto($0) }
            }
            .mapError { error in
                PhotoRepositoryError.fetchFailed("Failed to fetch photos by date range: \(error.localizedDescription)")
            }
            .eraseToAnyPublisher()
    }

    func getPhotosByDateRangeAndCategory(startDate: Int64, endDate: Int64, categoryId: Int64) -> AnyPublisher<[Photo], Error> {
        let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
        request.predicate = NSCompoundPredicate(andPredicateWithSubpredicates: [
            NSPredicate(format: "timestamp BETWEEN %@", [startDate, endDate] as NSArray),
            NSPredicate(format: "categoryId == %ld", categoryId)
        ])
        request.sortDescriptors = [NSSortDescriptor(keyPath: \PhotoEntity.timestamp, ascending: false)]

        return coreDataStack.viewContext.publisher(for: request)
            .map { entities in
                entities.compactMap { self.entityToPhoto($0) }
            }
            .mapError { error in
                PhotoRepositoryError.fetchFailed("Failed to fetch photos by date range and category: \(error.localizedDescription)")
            }
            .eraseToAnyPublisher()
    }

    func searchPhotosWithFilters(
        searchQuery: String,
        startDate: Int64,
        endDate: Int64,
        favoritesOnly: Bool?,
        categoryId: Int64?
    ) -> AnyPublisher<[Photo], Error> {
        let request = NSFetchRequest<PhotoEntity>(entityName: "PhotoEntity")
        var predicates: [NSPredicate] = []

        // Text search
        if !searchQuery.isEmpty {
            predicates.append(NSPredicate(format: "uri CONTAINS[cd] %@", searchQuery))
        }

        // Date range
        if startDate > 0 || endDate < Int64.max {
            predicates.append(NSPredicate(format: "timestamp BETWEEN %@", [startDate, endDate] as NSArray))
        }

        // Favorites
        if let favoritesOnly = favoritesOnly, favoritesOnly {
            predicates.append(NSPredicate(format: "isFavorite == TRUE"))
        }

        // Category
        if let categoryId = categoryId, categoryId > 0 {
            predicates.append(NSPredicate(format: "categoryId == %ld", categoryId))
        }

        if !predicates.isEmpty {
            request.predicate = NSCompoundPredicate(andPredicateWithSubpredicates: predicates)
        }

        request.sortDescriptors = [NSSortDescriptor(keyPath: \PhotoEntity.timestamp, ascending: false)]

        return coreDataStack.viewContext.publisher(for: request)
            .map { entities in
                entities.compactMap { self.entityToPhoto($0) }
            }
            .mapError { error in
                PhotoRepositoryError.fetchFailed("Failed to search photos with filters: \(error.localizedDescription)")
            }
            .eraseToAnyPublisher()
    }

    // MARK: - Private Helpers

    private func entityToPhoto(_ entity: PhotoEntity) -> Photo? {
        guard let id = entity.id, let uri = entity.uri else {
            logger.warning("Photo entity missing required fields")
            return nil
        }

        let photoId = Int64(id) ?? 0
        let url = URL(fileURLWithPath: uri)
        let name = url.deletingPathExtension().lastPathComponent

        return Photo(
            id: photoId,
            path: uri,
            categoryId: entity.categoryId,
            name: name,
            isFromAssets: false,
            createdAt: entity.timestamp,
            fileSize: 0, // Will be calculated when needed
            width: 0,    // Will be calculated when needed
            height: 0,   // Will be calculated when needed
            isFavorite: entity.isFavorite
        )
    }
}