import CoreData
import Combine
import Foundation
import os.log

final class CategoryRepositoryImpl: CategoryRepository {
    static let shared = CategoryRepositoryImpl()

    private let coreDataStack: CoreDataStack
    private let logger = Logger(subsystem: "com.smilepile", category: "CategoryRepository")
    private var cancellables = Set<AnyCancellable>()
    private var initializationTask: Task<Void, Error>?
    private var isInitialized = false

    init(coreDataStack: CoreDataStack = CoreDataStack.shared) {
        self.coreDataStack = coreDataStack

        // Start initialization immediately
        self.initializationTask = Task {
            do {
                try await self.initializeDefaultCategories()
                self.isInitialized = true
            } catch {
                self.logger.error("Failed to initialize default categories: \(error)")
                throw error
            }
        }
    }

    // Ensure initialization is complete before operations
    private func ensureInitialized() async throws {
        if let task = initializationTask {
            try await task.value
        }
    }

    // MARK: - CRUD Operations

    func insertCategory(_ category: Category) async throws -> Int64 {
        guard !category.displayName.isEmpty else {
            throw CategoryRepositoryError.invalidInput("Category display name cannot be empty")
        }

        // Check for duplicate names
        if try await getCategoryByName(category.displayName) != nil {
            throw CategoryRepositoryError.duplicateName("Category with name '\(category.displayName)' already exists")
        }

        return try await coreDataStack.performBackgroundTask { context in
            let categoryEntity = CategoryEntity(context: context)
            categoryEntity.id = category.id == 0 ? Int64(Date().timeIntervalSince1970 * 1000) : category.id
            categoryEntity.name = category.name
            categoryEntity.displayName = category.displayName
            categoryEntity.colorHex = category.colorHex ?? "#4CAF50"
            categoryEntity.position = Int32(category.position)
            categoryEntity.isDefault = category.isDefault
            categoryEntity.createdAt = category.createdAt

            try self.coreDataStack.saveContext(context)
            self.logger.debug("Category inserted successfully: \(categoryEntity.displayName ?? "")")
            return categoryEntity.id
        }
    }

    func insertCategories(_ categories: [Category]) async throws {
        guard !categories.isEmpty else {
            throw CategoryRepositoryError.invalidInput("Categories array cannot be empty")
        }

        // Check for duplicate names
        let displayNames = categories.map { $0.displayName }
        let uniqueNames = Set(displayNames)
        if uniqueNames.count != displayNames.count {
            throw CategoryRepositoryError.duplicateName("Duplicate category names found in the array")
        }

        try await coreDataStack.performBackgroundTask { context in
            for category in categories {
                let categoryEntity = CategoryEntity(context: context)
                categoryEntity.id = category.id == 0 ? Int64(Date().timeIntervalSince1970 * 1000) + Int64.random(in: 0...1000) : category.id
                categoryEntity.name = category.name
                categoryEntity.displayName = category.displayName
                categoryEntity.colorHex = category.colorHex ?? "#4CAF50"
                categoryEntity.position = Int32(category.position)
                categoryEntity.isDefault = category.isDefault
                categoryEntity.createdAt = category.createdAt
            }

            try self.coreDataStack.saveContext(context)
            self.logger.debug("Inserted \(categories.count) categories successfully")
        }
    }

    func updateCategory(_ category: Category) async throws {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
            request.predicate = NSPredicate(format: "id == %ld", category.id)
            request.fetchLimit = 1

            guard let categoryEntity = try context.fetch(request).first else {
                throw CategoryRepositoryError.notFound("Category not found with id: \(category.id)")
            }

            // Check if trying to modify a default category's essential properties
            if categoryEntity.isDefault && category.displayName != categoryEntity.displayName {
                throw CategoryRepositoryError.defaultCategoryModification("Cannot modify display name of default category")
            }

            // Check for duplicate names (excluding current category)
            let nameCheckRequest = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
            nameCheckRequest.predicate = NSCompoundPredicate(andPredicateWithSubpredicates: [
                NSPredicate(format: "displayName ==[c] %@", category.displayName),
                NSPredicate(format: "id != %ld", category.id)
            ])

            if try context.count(for: nameCheckRequest) > 0 {
                throw CategoryRepositoryError.duplicateName("Category with name '\(category.displayName)' already exists")
            }

            categoryEntity.name = category.name
            categoryEntity.displayName = category.displayName
            categoryEntity.colorHex = category.colorHex ?? categoryEntity.colorHex ?? "#4CAF50"
            categoryEntity.position = Int32(category.position)
            categoryEntity.isDefault = category.isDefault
            categoryEntity.createdAt = category.createdAt

            try self.coreDataStack.saveContext(context)
            self.logger.debug("Category updated successfully: \(category.id)")
        }
    }

    func deleteCategory(_ category: Category) async throws {
        if category.isDefault {
            throw CategoryRepositoryError.defaultCategoryModification("Cannot delete default category")
        }

        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
            request.predicate = NSPredicate(format: "id == %ld", category.id)

            let categories = try context.fetch(request)
            for categoryEntity in categories {
                if categoryEntity.isDefault {
                    throw CategoryRepositoryError.defaultCategoryModification("Cannot delete default category")
                }
                context.delete(categoryEntity)
            }

            try self.coreDataStack.saveContext(context)
            self.logger.debug("Category deleted successfully: \(category.id)")
        }
    }

    // MARK: - Retrieval Operations

    func getCategoryById(_ categoryId: Int64) async throws -> Category? {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
            request.predicate = NSPredicate(format: "id == %ld", categoryId)
            request.fetchLimit = 1

            guard let entity = try context.fetch(request).first else {
                return nil
            }

            return self.entityToCategory(entity)
        }
    }

    func getAllCategories() async throws -> [Category] {
        try await ensureInitialized()

        let categories = try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
            request.sortDescriptors = [NSSortDescriptor(keyPath: \CategoryEntity.position, ascending: true)]

            let entities = try context.fetch(request)
            return entities.compactMap { self.entityToCategory($0) }
        }

        // If no categories exist after initialization, reinitialize
        if categories.isEmpty && self.isInitialized {
            self.logger.warning("No categories found after initialization, retrying...")
            self.isInitialized = false
            try await self.initializeDefaultCategories()
            return try await self.getAllCategories()
        }

        return categories
    }

    func getAllCategoriesFlow() -> AnyPublisher<[Category], Error> {
        let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
        request.sortDescriptors = [NSSortDescriptor(keyPath: \CategoryEntity.createdAt, ascending: true)]

        return coreDataStack.viewContext.publisher(for: request)
            .map { entities in
                entities.compactMap { self.entityToCategory($0) }
            }
            .mapError { error in
                CategoryRepositoryError.fetchFailed("Failed to fetch categories: \(error.localizedDescription)")
            }
            .eraseToAnyPublisher()
    }

    // MARK: - Name Operations

    func getCategoryByName(_ name: String) async throws -> Category? {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
            request.predicate = NSPredicate(format: "displayName ==[c] %@", name)
            request.fetchLimit = 1

            guard let entity = try context.fetch(request).first else {
                return nil
            }

            return self.entityToCategory(entity)
        }
    }

    // MARK: - Initialization

    func initializeDefaultCategories() async throws {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
            let existingCount = try context.count(for: request)

            if existingCount == 0 {
                self.logger.info("Initializing default categories...")

                let defaultCategories = Category.getDefaultCategories()
                for category in defaultCategories {
                    let entity = CategoryEntity(context: context)
                    entity.id = category.id
                    entity.name = category.name
                    entity.displayName = category.displayName
                    entity.colorHex = category.colorHex ?? "#4CAF50"
                    entity.position = Int32(category.position)
                    entity.isDefault = category.isDefault
                    entity.createdAt = category.createdAt
                }

                try self.coreDataStack.saveContext(context)
                self.logger.info("Default categories initialized successfully with \(defaultCategories.count) categories")
                self.isInitialized = true
            } else {
                self.logger.info("Categories already exist (\(existingCount)), skipping initialization")
                self.isInitialized = true
            }
        }
    }

    // MARK: - Utility Operations

    func getCategoryCount() async throws -> Int {
        try await coreDataStack.performBackgroundTask { context in
            let request = NSFetchRequest<CategoryEntity>(entityName: "CategoryEntity")
            return try context.count(for: request)
        }
    }

    // MARK: - Private Helpers

    private func entityToCategory(_ entity: CategoryEntity) -> Category? {
        guard let displayName = entity.displayName else {
            logger.warning("Category entity missing display name")
            return nil
        }

        let name = displayName.lowercased().replacingOccurrences(of: " ", with: "_")

        return Category(
            id: entity.id,
            name: name,
            displayName: displayName,
            position: Int(entity.position),
            iconResource: nil,
            colorHex: entity.colorHex,
            isDefault: entity.isDefault,
            createdAt: entity.createdAt
        )
    }
}