import Foundation
import CoreData
import Combine
import os.log

/// Comprehensive Category Management System with batch operations and photo associations
@MainActor
public class CategoryManager: ObservableObject {
    // MARK: - Published Properties
    @Published public private(set) var categories: [Category] = []
    @Published public private(set) var categoriesWithCounts: [CategoryWithCount] = []
    @Published public private(set) var isLoading = false
    @Published public private(set) var errorMessage: String?
    @Published public var selectedCategories: Set<Int64> = []

    // MARK: - Private Properties
    private let repository: CategoryRepository
    private let photoRepository: PhotoRepository
    private let coreDataStack: CoreDataStack
    private var cancellables = Set<AnyCancellable>()
    private let logger = Logger(subsystem: "com.smilepile", category: "CategoryManager")

    // MARK: - Configuration
    public struct Configuration {
        static let defaultColors = [
            "#E91E63", // Pink
            "#F44336", // Red
            "#9C27B0", // Purple
            "#673AB7", // Deep Purple
            "#3F51B5", // Indigo
            "#2196F3", // Blue
            "#03A9F4", // Light Blue
            "#00BCD4", // Cyan
            "#009688", // Teal
            "#4CAF50", // Green
            "#8BC34A", // Light Green
            "#CDDC39", // Lime
            "#FFEB3B", // Yellow
            "#FFC107", // Amber
            "#FF9800", // Orange
            "#FF5722", // Deep Orange
            "#795548", // Brown
            "#9E9E9E", // Grey
            "#607D8B"  // Blue Grey
        ]

        static let defaultIcons = [
            "star.fill",
            "heart.fill",
            "house.fill",
            "car.fill",
            "airplane",
            "figure.walk",
            "sportscourt.fill",
            "book.fill",
            "music.note",
            "camera.fill",
            "paintbrush.fill",
            "gift.fill",
            "birthday.cake.fill",
            "leaf.fill",
            "pawprint.fill",
            "sun.max.fill",
            "moon.fill",
            "cloud.fill",
            "snowflake"
        ]
    }

    // MARK: - Initialization
    public init(
        repository: CategoryRepository? = nil,
        photoRepository: PhotoRepository? = nil,
        coreDataStack: CoreDataStack? = nil
    ) {
        self.repository = repository ?? CategoryRepositoryImpl()
        self.photoRepository = photoRepository ?? PhotoRepositoryImpl()
        self.coreDataStack = coreDataStack ?? CoreDataStack.shared

        setupObservers()
        Task {
            await loadCategories()
        }
    }

    // MARK: - Setup
    private func setupObservers() {
        // Observe repository changes
        repository.getAllCategoriesFlow()
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    if case .failure(let error) = completion {
                        self?.logger.error("Category flow error: \(error.localizedDescription)")
                        self?.errorMessage = error.localizedDescription
                    }
                },
                receiveValue: { [weak self] categories in
                    Task { @MainActor in
                        await self?.handleCategoriesUpdate(categories)
                    }
                }
            )
            .store(in: &cancellables)
    }

    // MARK: - Public Methods - Core Operations

    /// Load all categories with photo counts
    public func loadCategories() async {
        isLoading = true
        errorMessage = nil

        do {
            let allCategories = try await repository.getAllCategories()
            var categoriesWithCounts: [CategoryWithCount] = []

            for category in allCategories {
                let count = try await photoRepository.getPhotoCountByCategory(categoryId: category.id)
                categoriesWithCounts.append(CategoryWithCount(
                    category: category,
                    photoCount: count
                ))
            }

            self.categories = allCategories
            self.categoriesWithCounts = categoriesWithCounts.sorted { $0.category.position < $1.category.position }

            logger.info("Loaded \(allCategories.count) categories")
        } catch {
            logger.error("Failed to load categories: \(error.localizedDescription)")
            errorMessage = "Failed to load categories"
        }

        isLoading = false
    }

    /// Create a new category
    public func createCategory(
        name: String,
        colorHex: String? = nil,
        icon: String? = nil
    ) async throws -> Category {
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedName.isEmpty else {
            throw CategoryError.invalidName
        }

        // Check for duplicate
        if let _ = try await repository.getCategoryByName(trimmedName) {
            throw CategoryError.duplicateName(trimmedName)
        }

        // Generate safe internal name
        let safeName = trimmedName
            .lowercased()
            .replacingOccurrences(of: " ", with: "_")
            .replacingOccurrences(of: "[^a-z0-9_]", with: "", options: .regularExpression)

        let categoryCount = try await repository.getCategoryCount()

        let category = Category(
            id: Int64(Date().timeIntervalSince1970 * 1000),
            name: safeName,
            displayName: trimmedName,
            position: categoryCount,
            iconResource: icon,
            colorHex: colorHex ?? Configuration.defaultColors.randomElement()!,
            isDefault: false
        )

        _ = try await repository.insertCategory(category)
        logger.info("Created category: \(trimmedName)")

        await loadCategories()
        return category
    }

    /// Update an existing category
    public func updateCategory(
        _ category: Category,
        displayName: String? = nil,
        colorHex: String? = nil,
        icon: String? = nil
    ) async throws {
        var updatedCategory = category

        if let displayName = displayName {
            let trimmedName = displayName.trimmingCharacters(in: .whitespacesAndNewlines)

            guard !trimmedName.isEmpty else {
                throw CategoryError.invalidName
            }

            // Check for duplicate if name changed
            if trimmedName != category.displayName,
               let _ = try await repository.getCategoryByName(trimmedName) {
                throw CategoryError.duplicateName(trimmedName)
            }

            let safeName = trimmedName
                .lowercased()
                .replacingOccurrences(of: " ", with: "_")
                .replacingOccurrences(of: "[^a-z0-9_]", with: "", options: .regularExpression)

            updatedCategory = Category(
                id: category.id,
                name: safeName,
                displayName: trimmedName,
                position: category.position,
                iconResource: icon ?? category.iconResource,
                colorHex: colorHex ?? category.colorHex,
                isDefault: category.isDefault,
                createdAt: category.createdAt
            )
        } else {
            updatedCategory = Category(
                id: category.id,
                name: category.name,
                displayName: category.displayName,
                position: category.position,
                iconResource: icon ?? category.iconResource,
                colorHex: colorHex ?? category.colorHex,
                isDefault: category.isDefault,
                createdAt: category.createdAt
            )
        }

        try await repository.updateCategory(updatedCategory)
        logger.info("Updated category: \(updatedCategory.displayName)")

        await loadCategories()
    }

    /// Delete a category
    public func deleteCategory(_ category: Category, movePhotosTo: Category? = nil) async throws {
        // Prevent deleting the last category
        let categoryCount = try await repository.getCategoryCount()
        if categoryCount <= 1 {
            throw CategoryError.cannotDeleteLastCategory
        }

        // Handle photos in the category
        let photoCount = try await photoRepository.getPhotoCountByCategory(categoryId: category.id)

        if photoCount > 0 {
            if let targetCategory = movePhotosTo {
                // Move photos to target category
                try await movePhotosFromCategory(category, to: targetCategory)
            } else {
                // Delete photos if no target category specified
                try await photoRepository.deletePhotosByCategory(category.id)
            }
        }

        // Delete the category
        try await repository.deleteCategory(category)
        logger.info("Deleted category: \(category.displayName) (photos: \(photoCount))")

        await loadCategories()
    }

    // MARK: - Batch Operations

    /// Assign multiple photos to a category
    public func assignPhotosToCategory(_ photoIds: [Int64], categoryId: Int64) async throws {
        guard !photoIds.isEmpty else { return }

        // Verify category exists
        guard let category = try await repository.getCategoryById(categoryId) else {
            throw CategoryError.categoryNotFound
        }

        var successCount = 0
        var failureCount = 0

        for photoId in photoIds {
            do {
                if let photo = try await photoRepository.getPhotoById(photoId) {
                    let updatedPhoto = Photo(
                        id: photo.id,
                        path: photo.path,
                        categoryId: categoryId,
                        name: photo.name,
                        isFromAssets: photo.isFromAssets,
                        createdAt: photo.createdAt,
                        fileSize: photo.fileSize,
                        width: photo.width,
                        height: photo.height
                    )

                    try await photoRepository.updatePhoto(updatedPhoto)
                    successCount += 1
                }
            } catch {
                logger.error("Failed to assign photo \(photoId) to category: \(error)")
                failureCount += 1
            }
        }

        logger.info("Batch assignment complete: \(successCount) succeeded, \(failureCount) failed for category: \(category.displayName)")

        if failureCount > 0 {
            throw CategoryError.partialBatchFailure(success: successCount, failed: failureCount)
        }
    }

    /// Remove photos from their current categories (uncategorized)
    public func removePhotosFromCategories(_ photoIds: [Int64]) async throws {
        // Get or create uncategorized category
        let uncategorizedId = try await getOrCreateUncategorizedCategory().id
        try await assignPhotosToCategory(photoIds, categoryId: uncategorizedId)
    }

    /// Move all photos from one category to another
    public func movePhotosFromCategory(_ source: Category, to target: Category) async throws {
        let photos = try await photoRepository.getPhotosByCategory(source.id)
        let photoIds = photos.map { $0.id }

        if !photoIds.isEmpty {
            try await assignPhotosToCategory(photoIds, categoryId: target.id)
            logger.info("Moved \(photoIds.count) photos from '\(source.displayName)' to '\(target.displayName)'")
        }
    }

    /// Merge multiple categories into one
    public func mergeCategories(_ sourceCategories: [Category], into targetCategory: Category) async throws {
        for source in sourceCategories {
            guard source.id != targetCategory.id else { continue }

            // Move all photos to target
            try await movePhotosFromCategory(source, to: targetCategory)

            // Delete source category
            try await repository.deleteCategory(source)
        }

        logger.info("Merged \(sourceCategories.count) categories into '\(targetCategory.displayName)'")
        await loadCategories()
    }

    // MARK: - Search and Filtering

    /// Search categories by name
    public func searchCategories(query: String) -> [CategoryWithCount] {
        guard !query.isEmpty else { return categoriesWithCounts }

        let lowercaseQuery = query.lowercased()
        return categoriesWithCounts.filter { categoryWithCount in
            categoryWithCount.category.displayName.lowercased().contains(lowercaseQuery) ||
            categoryWithCount.category.name.lowercased().contains(lowercaseQuery)
        }
    }

    /// Get categories with photos
    public func getCategoriesWithPhotos() -> [CategoryWithCount] {
        return categoriesWithCounts.filter { $0.photoCount > 0 }
    }

    /// Get empty categories
    public func getEmptyCategories() -> [CategoryWithCount] {
        return categoriesWithCounts.filter { $0.photoCount == 0 }
    }

    // MARK: - Special Categories

    /// Get or create the uncategorized category
    public func getOrCreateUncategorizedCategory() async throws -> Category {
        let uncategorizedName = "uncategorized"

        if let existing = try await repository.getCategoryByName(uncategorizedName) {
            return existing
        }

        // Create uncategorized category
        let category = Category(
            id: -1, // Special ID for uncategorized
            name: uncategorizedName,
            displayName: "Uncategorized",
            position: Int.max, // Always last
            iconResource: "square.stack",
            colorHex: "#9E9E9E", // Grey
            isDefault: true
        )

        _ = try await repository.insertCategory(category)
        return category
    }

    /// Initialize default categories if needed
    public func initializeDefaultCategoriesIfNeeded() async throws {
        let existingCount = try await repository.getCategoryCount()

        if existingCount == 0 {
            logger.info("Initializing default categories")

            let defaultCategories = Category.getDefaultCategories()
            try await repository.insertCategories(defaultCategories)

            // Also create uncategorized
            _ = try await getOrCreateUncategorizedCategory()

            await loadCategories()
        }
    }

    // MARK: - Reordering

    /// Reorder categories
    public func reorderCategories(_ reorderedCategories: [Category]) async throws {
        for (index, category) in reorderedCategories.enumerated() {
            if category.position != index {
                let updated = Category(
                    id: category.id,
                    name: category.name,
                    displayName: category.displayName,
                    position: index,
                    iconResource: category.iconResource,
                    colorHex: category.colorHex,
                    isDefault: category.isDefault,
                    createdAt: category.createdAt
                )
                try await repository.updateCategory(updated)
            }
        }

        await loadCategories()
    }

    // MARK: - Selection Management

    /// Toggle category selection
    public func toggleCategorySelection(_ categoryId: Int64) {
        if selectedCategories.contains(categoryId) {
            selectedCategories.remove(categoryId)
        } else {
            selectedCategories.insert(categoryId)
        }
    }

    /// Clear all selections
    public func clearSelections() {
        selectedCategories.removeAll()
    }

    /// Select all categories
    public func selectAllCategories() {
        selectedCategories = Set(categories.map { $0.id })
    }

    // MARK: - Statistics

    /// Get category statistics
    public func getCategoryStatistics() async throws -> CategoryStatistics {
        let totalCategories = categories.count
        let totalPhotos = try await photoRepository.getTotalPhotoCount()
        let categoriesWithPhotos = getCategoriesWithPhotos().count
        let emptyCategories = getEmptyCategories().count

        var largestCategory: CategoryWithCount?
        var averagePhotosPerCategory = 0

        if !categoriesWithCounts.isEmpty {
            largestCategory = categoriesWithCounts.max { $0.photoCount < $1.photoCount }
            let totalInCategories = categoriesWithCounts.reduce(0) { $0 + $1.photoCount }
            averagePhotosPerCategory = categoriesWithPhotos > 0 ? totalInCategories / categoriesWithPhotos : 0
        }

        return CategoryStatistics(
            totalCategories: totalCategories,
            totalPhotos: totalPhotos,
            categoriesWithPhotos: categoriesWithPhotos,
            emptyCategories: emptyCategories,
            largestCategory: largestCategory,
            averagePhotosPerCategory: averagePhotosPerCategory
        )
    }

    // MARK: - Private Methods

    private func handleCategoriesUpdate(_ categories: [Category]) async {
        self.categories = categories
        await loadCategories() // Reload with counts
    }
}

// MARK: - Supporting Types

public struct CategoryWithCount: Identifiable {
    public let category: Category
    public let photoCount: Int

    public var id: Int64 { category.id }
}

public struct CategoryStatistics {
    public let totalCategories: Int
    public let totalPhotos: Int
    public let categoriesWithPhotos: Int
    public let emptyCategories: Int
    public let largestCategory: CategoryWithCount?
    public let averagePhotosPerCategory: Int
}

public enum CategoryError: LocalizedError {
    case invalidName
    case duplicateName(String)
    case categoryNotFound
    case cannotDeleteLastCategory
    case partialBatchFailure(success: Int, failed: Int)

    public var errorDescription: String? {
        switch self {
        case .invalidName:
            return "Category name cannot be empty"
        case .duplicateName(let name):
            return "Category '\(name)' already exists"
        case .categoryNotFound:
            return "Category not found"
        case .cannotDeleteLastCategory:
            return "Cannot delete the last remaining category"
        case .partialBatchFailure(let success, let failed):
            return "Batch operation partially failed: \(success) succeeded, \(failed) failed"
        }
    }
}