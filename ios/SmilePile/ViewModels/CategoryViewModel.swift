import Foundation
import SwiftUI
import Combine
import os.log

/// ViewModel for comprehensive category operations and state management
@MainActor
class CategoryViewModel: ObservableObject {
    // MARK: - Published Properties

    // Category data
    @Published var categories: [Category] = []
    @Published var categoriesWithCounts: [CategoryWithCount] = []

    // Selection states
    @Published var selectedCategory: Category?
    @Published var selectedCategoryIds: Set<Int64> = []
    @Published var selectedPhotoIds: Set<Int64> = []

    // UI states
    @Published var isLoading = false
    @Published var isBatchProcessing = false
    @Published var errorMessage: String?
    @Published var successMessage: String?

    // Search and filter
    @Published var searchQuery = ""
    @Published var showEmptyCategories = true
    @Published var sortOption: SortOption = .position

    // Drag and drop
    @Published var draggedCategory: Category?
    @Published var draggedPhotos: [Photo] = []
    @Published var dropTargetCategory: Category?

    // Edit states
    @Published var editingCategory: Category?
    @Published var showAddCategorySheet = false
    @Published var showDeleteConfirmation = false
    @Published var categoryToDelete: Category?

    // Batch operation states
    @Published var batchOperationProgress: Double = 0
    @Published var batchOperationStatus: String?

    // MARK: - Private Properties

    private let categoryManager: CategoryManager
    private let photoRepository: PhotoRepository
    private var cancellables = Set<AnyCancellable>()
    private let logger = Logger(subsystem: "com.smilepile", category: "CategoryViewModel")

    // MARK: - Enums

    enum SortOption: String, CaseIterable {
        case position = "Position"
        case name = "Name"
        case photoCount = "Photo Count"
        case dateCreated = "Date Created"

        var systemImage: String {
            switch self {
            case .position: return "list.number"
            case .name: return "textformat"
            case .photoCount: return "photo.stack"
            case .dateCreated: return "calendar"
            }
        }
    }

    // MARK: - Initialization

    init(categoryManager: CategoryManager? = nil,
         photoRepository: PhotoRepository? = nil) {
        self.categoryManager = categoryManager ?? CategoryManager()
        self.photoRepository = photoRepository ?? PhotoRepositoryImpl()

        setupBindings()
        Task {
            await loadCategories()
        }
    }

    // MARK: - Setup

    private func setupBindings() {
        // Observe category manager updates
        categoryManager.$categoriesWithCounts
            .receive(on: DispatchQueue.main)
            .sink { [weak self] categoriesWithCounts in
                self?.handleCategoriesUpdate(categoriesWithCounts)
            }
            .store(in: &cancellables)

        // Observe search query changes
        $searchQuery
            .debounce(for: .milliseconds(300), scheduler: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.applyFiltersAndSort()
            }
            .store(in: &cancellables)

        // Observe sort option changes
        $sortOption
            .sink { [weak self] _ in
                self?.applyFiltersAndSort()
            }
            .store(in: &cancellables)
    }

    // MARK: - Public Methods - Data Loading

    func loadCategories() async {
        isLoading = true
        errorMessage = nil

        await categoryManager.loadCategories()
        applyFiltersAndSort()

        isLoading = false
    }

    func refreshCategories() async {
        await loadCategories()
    }

    // MARK: - Category Operations

    func createCategory(name: String, colorHex: String? = nil, icon: String? = nil) async {
        do {
            let category = try await categoryManager.createCategory(
                name: name,
                colorHex: colorHex,
                icon: icon
            )

            successMessage = "Created category: \(category.displayName)"
            await loadCategories()
        } catch {
            errorMessage = error.localizedDescription
            logger.error("Failed to create category: \(error)")
        }
    }

    func updateCategory(_ category: Category, name: String? = nil, colorHex: String? = nil, icon: String? = nil) async {
        do {
            try await categoryManager.updateCategory(
                category,
                displayName: name,
                colorHex: colorHex,
                icon: icon
            )

            successMessage = "Updated category: \(category.displayName)"
            await loadCategories()
        } catch {
            errorMessage = error.localizedDescription
            logger.error("Failed to update category: \(error)")
        }
    }

    func deleteCategory(_ category: Category, movePhotosTo: Category? = nil) async {
        do {
            try await categoryManager.deleteCategory(category, movePhotosTo: movePhotosTo)
            successMessage = "Deleted category: \(category.displayName)"

            // Clear selection if deleted category was selected
            if selectedCategory?.id == category.id {
                selectedCategory = nil
            }
            selectedCategoryIds.remove(category.id)

            await loadCategories()
        } catch {
            errorMessage = error.localizedDescription
            logger.error("Failed to delete category: \(error)")
        }
    }

    func mergeCategories(_ sources: [Category], into target: Category) async {
        isBatchProcessing = true
        batchOperationStatus = "Merging categories..."

        do {
            try await categoryManager.mergeCategories(sources, into: target)
            successMessage = "Merged \(sources.count) categories into \(target.displayName)"

            // Clear selections
            selectedCategoryIds.removeAll()

            await loadCategories()
        } catch {
            errorMessage = error.localizedDescription
            logger.error("Failed to merge categories: \(error)")
        }

        isBatchProcessing = false
        batchOperationStatus = nil
    }

    // MARK: - Photo Assignment Operations

    func assignPhotosToCategory(_ photoIds: [Int64], categoryId: Int64) async {
        isBatchProcessing = true
        batchOperationProgress = 0
        batchOperationStatus = "Assigning photos to category..."

        do {
            try await categoryManager.assignPhotosToCategory(photoIds, categoryId: categoryId)
            successMessage = "Assigned \(photoIds.count) photos to category"

            // Clear photo selections
            selectedPhotoIds.removeAll()

            await loadCategories()
        } catch {
            errorMessage = error.localizedDescription
            logger.error("Failed to assign photos: \(error)")
        }

        isBatchProcessing = false
        batchOperationProgress = 0
        batchOperationStatus = nil
    }

    func movePhotosToCategory(from sourceCategory: Category, to targetCategory: Category) async {
        isBatchProcessing = true
        batchOperationStatus = "Moving photos between categories..."

        do {
            try await categoryManager.movePhotosFromCategory(sourceCategory, to: targetCategory)
            successMessage = "Moved photos from \(sourceCategory.displayName) to \(targetCategory.displayName)"
            await loadCategories()
        } catch {
            errorMessage = error.localizedDescription
            logger.error("Failed to move photos: \(error)")
        }

        isBatchProcessing = false
        batchOperationStatus = nil
    }

    // MARK: - Selection Management

    func selectCategory(_ category: Category) {
        if selectedCategory?.id == category.id {
            selectedCategory = nil
        } else {
            selectedCategory = category
        }
    }

    func toggleCategorySelection(_ categoryId: Int64) {
        if selectedCategoryIds.contains(categoryId) {
            selectedCategoryIds.remove(categoryId)
        } else {
            selectedCategoryIds.insert(categoryId)
        }
    }

    func selectAllCategories() {
        selectedCategoryIds = Set(categories.map { $0.id })
    }

    func clearCategorySelections() {
        selectedCategoryIds.removeAll()
    }

    func togglePhotoSelection(_ photoId: Int64) {
        if selectedPhotoIds.contains(photoId) {
            selectedPhotoIds.remove(photoId)
        } else {
            selectedPhotoIds.insert(photoId)
        }
    }

    func clearPhotoSelections() {
        selectedPhotoIds.removeAll()
    }

    // MARK: - Drag and Drop Support

    func startDraggingCategory(_ category: Category) {
        draggedCategory = category
    }

    func startDraggingPhotos(_ photos: [Photo]) {
        draggedPhotos = photos
    }

    func setDropTarget(_ category: Category?) {
        dropTargetCategory = category
    }

    func handleDrop() async {
        guard let target = dropTargetCategory else { return }

        // Handle category drop (reordering)
        if let draggedCategory = draggedCategory {
            await reorderCategories(moving: draggedCategory, to: target)
        }

        // Handle photo drop (assignment)
        if !draggedPhotos.isEmpty {
            let photoIds = draggedPhotos.map { $0.id }
            await assignPhotosToCategory(photoIds, categoryId: target.id)
        }

        // Clear drag state
        draggedCategory = nil
        draggedPhotos = []
        dropTargetCategory = nil
    }

    func cancelDrag() {
        draggedCategory = nil
        draggedPhotos = []
        dropTargetCategory = nil
    }

    // MARK: - Reordering

    func reorderCategories(moving source: Category, to target: Category) async {
        var reordered = categories

        guard let sourceIndex = reordered.firstIndex(where: { $0.id == source.id }),
              let targetIndex = reordered.firstIndex(where: { $0.id == target.id }) else {
            return
        }

        reordered.remove(at: sourceIndex)
        reordered.insert(source, at: targetIndex)

        do {
            try await categoryManager.reorderCategories(reordered)
            await loadCategories()
        } catch {
            errorMessage = error.localizedDescription
            logger.error("Failed to reorder categories: \(error)")
        }
    }

    func moveCategory(_ category: Category, up: Bool) async {
        var reordered = categories

        guard let index = reordered.firstIndex(where: { $0.id == category.id }) else {
            return
        }

        let newIndex = up ? index - 1 : index + 1

        guard newIndex >= 0 && newIndex < reordered.count else {
            return
        }

        reordered.swapAt(index, newIndex)

        do {
            try await categoryManager.reorderCategories(reordered)
            await loadCategories()
        } catch {
            errorMessage = error.localizedDescription
            logger.error("Failed to move category: \(error)")
        }
    }

    // MARK: - Filtering and Sorting

    private func applyFiltersAndSort() {
        var filtered = categoryManager.categoriesWithCounts

        // Apply search filter
        if !searchQuery.isEmpty {
            filtered = categoryManager.searchCategories(query: searchQuery)
        }

        // Apply empty category filter
        if !showEmptyCategories {
            filtered = filtered.filter { $0.photoCount > 0 }
        }

        // Apply sorting
        switch sortOption {
        case .position:
            filtered.sort { $0.category.position < $1.category.position }
        case .name:
            filtered.sort { $0.category.displayName < $1.category.displayName }
        case .photoCount:
            filtered.sort { $0.photoCount > $1.photoCount }
        case .dateCreated:
            filtered.sort { $0.category.createdAt > $1.category.createdAt }
        }

        categoriesWithCounts = filtered
        categories = filtered.map { $0.category }
    }

    // MARK: - Statistics

    func getCategoryStatistics() async -> CategoryStatistics? {
        do {
            return try await categoryManager.getCategoryStatistics()
        } catch {
            logger.error("Failed to get category statistics: \(error)")
            return nil
        }
    }

    // MARK: - Computed Properties

    var hasSelectedCategories: Bool {
        !selectedCategoryIds.isEmpty
    }

    var hasSelectedPhotos: Bool {
        !selectedPhotoIds.isEmpty
    }

    var selectedCategoriesCount: Int {
        selectedCategoryIds.count
    }

    var canMergeCategories: Bool {
        selectedCategoryIds.count >= 2
    }

    var canDeleteCategory: Bool {
        categories.count > 1
    }

    var filteredCategoriesDescription: String {
        if searchQuery.isEmpty && showEmptyCategories {
            return "\(categoriesWithCounts.count) categories"
        } else {
            return "\(categoriesWithCounts.count) of \(categoryManager.categoriesWithCounts.count) categories"
        }
    }

    // MARK: - UI Helpers

    func showCreateCategorySheet() {
        editingCategory = nil
        showAddCategorySheet = true
    }

    func showEditCategorySheet(_ category: Category) {
        editingCategory = category
        showAddCategorySheet = true
    }

    func requestDeleteCategory(_ category: Category) {
        categoryToDelete = category
        showDeleteConfirmation = true
    }

    func confirmDeleteCategory(movePhotosTo: Category? = nil) async {
        guard let category = categoryToDelete else { return }

        await deleteCategory(category, movePhotosTo: movePhotosTo)

        categoryToDelete = nil
        showDeleteConfirmation = false
    }

    func cancelDelete() {
        categoryToDelete = nil
        showDeleteConfirmation = false
    }

    func dismissMessages() {
        errorMessage = nil
        successMessage = nil
    }

    // MARK: - Private Methods

    private func handleCategoriesUpdate(_ categoriesWithCounts: [CategoryWithCount]) {
        applyFiltersAndSort()
    }
}

// MARK: - Extensions

extension CategoryViewModel {
    /// Quick action methods for common operations

    func quickAssignSelectedPhotosToCategory(_ category: Category) async {
        let photoIds = Array(selectedPhotoIds)
        await assignPhotosToCategory(photoIds, categoryId: category.id)
    }

    func quickMergeSelectedCategories(into target: Category) async {
        let sources = categories.filter { selectedCategoryIds.contains($0.id) && $0.id != target.id }
        await mergeCategories(sources, into: target)
    }

    func quickDeleteEmptyCategories() async {
        let emptyCategories = categoriesWithCounts
            .filter { $0.photoCount == 0 && !$0.category.isDefault }
            .map { $0.category }

        for category in emptyCategories {
            await deleteCategory(category)
        }
    }
}