import SwiftUI
import CoreData
import Combine
import os.log

class CategoryManagementViewModel: ObservableObject {
    @Published var categoriesWithCounts: [CategoryWithCount] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showAddDialog = false
    @Published var editingCategory: Category?
    @Published var categoryToDelete: Category?
    @Published var showDeleteConfirmation = false

    private let repository: CategoryRepository
    private let photoRepository: PhotoRepository
    private let coreDataStack: CoreDataStack
    private var cancellables = Set<AnyCancellable>()
    private let logger = Logger(subsystem: "com.smilepile", category: "CategoryManagementViewModel")

    init(
        repository: CategoryRepository? = nil,
        photoRepository: PhotoRepository = PhotoRepositoryImpl(),
        coreDataStack: CoreDataStack = CoreDataStack.shared
    ) {
        self.repository = repository ?? CategoryRepositoryImpl.shared
        self.photoRepository = photoRepository
        self.coreDataStack = coreDataStack

        // Load categories with retry logic
        Task { @MainActor in
            await loadCategoriesWithRetry()
        }
        setupCategoryObserver()
    }

    private func setupCategoryObserver() {
        repository.getAllCategoriesFlow()
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    if case .failure(let error) = completion {
                        self?.errorMessage = error.localizedDescription
                    }
                },
                receiveValue: { [weak self] _ in
                    self?.loadCategoriesWithCounts()
                }
            )
            .store(in: &cancellables)
    }

    func loadCategoriesWithCounts() {
        Task { @MainActor in
            isLoading = true
            do {
                let categories = try await repository.getAllCategories()

                // DON'T auto-initialize - user may need to complete onboarding first
                if categories.isEmpty {
                    logger.warning("No categories found - may need to complete onboarding")
                    errorMessage = "Please complete onboarding to create categories"
                    isLoading = false
                    return
                }

                var categoriesWithCounts: [CategoryWithCount] = []

                for category in categories {
                    let photoCount = try await photoRepository.getPhotoCountByCategory(categoryId: category.id)
                    categoriesWithCounts.append(CategoryWithCount(
                        category: category,
                        photoCount: photoCount
                    ))
                }

                self.categoriesWithCounts = categoriesWithCounts.sorted { $0.category.position < $1.category.position }
                isLoading = false
            } catch {
                logger.error("Failed to load categories: \(error.localizedDescription)")
                errorMessage = "Failed to load categories: \(error.localizedDescription)"
                isLoading = false
            }
        }
    }

    @MainActor
    private func loadCategoriesWithRetry() async {
        // Initial load
        loadCategoriesWithCounts()

        // Retry if no categories after short delay
        if categoriesWithCounts.isEmpty {
            try? await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
            loadCategoriesWithCounts()
        }
    }


    func showAddCategoryDialog() {
        editingCategory = nil
        showAddDialog = true
    }

    func showEditCategoryDialog(_ category: Category) {
        editingCategory = category
        showAddDialog = true
    }

    func addCategory(displayName: String, colorHex: String) {
        Task { @MainActor in
            isLoading = true
            errorMessage = nil

            guard !displayName.trimmingCharacters(in: .whitespaces).isEmpty else {
                errorMessage = "Category name cannot be empty"
                isLoading = false
                return
            }

            do {
                if let _ = try await repository.getCategoryByName(displayName) {
                    errorMessage = "Pile '\(displayName)' already exists"
                    isLoading = false
                    return
                }

                let count = try await repository.getCategoryCount()
                let category = Category(
                    id: Int64(Date().timeIntervalSince1970 * 1000),
                    name: displayName.lowercased().replacingOccurrences(of: " ", with: "_"),
                    displayName: displayName.trimmingCharacters(in: .whitespaces),
                    position: count,
                    colorHex: colorHex,
                    isDefault: false
                )

                _ = try await repository.insertCategory(category)
                showAddDialog = false
                loadCategoriesWithCounts()
            } catch {
                errorMessage = "Failed to add category: \(error.localizedDescription)"
            }
            isLoading = false
        }
    }

    func updateCategory(displayName: String, colorHex: String) {
        guard let category = editingCategory else { return }

        Task { @MainActor in
            isLoading = true
            errorMessage = nil

            guard !displayName.trimmingCharacters(in: .whitespaces).isEmpty else {
                errorMessage = "Category name cannot be empty"
                isLoading = false
                return
            }

            do {
                if displayName != category.displayName,
                   let _ = try await repository.getCategoryByName(displayName) {
                    errorMessage = "Pile '\(displayName)' already exists"
                    isLoading = false
                    return
                }

                let updatedCategory = Category(
                    id: category.id,
                    name: displayName.lowercased().replacingOccurrences(of: " ", with: "_"),
                    displayName: displayName.trimmingCharacters(in: .whitespaces),
                    position: category.position,
                    colorHex: colorHex,
                    isDefault: category.isDefault,
                    createdAt: category.createdAt
                )

                try await repository.updateCategory(updatedCategory)
                showAddDialog = false
                editingCategory = nil
                loadCategoriesWithCounts()
            } catch {
                errorMessage = "Failed to update category: \(error.localizedDescription)"
            }
            isLoading = false
        }
    }

    func requestDeleteCategory(_ category: Category) {
        categoryToDelete = category
        showDeleteConfirmation = true
    }

    func confirmDeleteCategory(deletePhotos: Bool = false) {
        guard let category = categoryToDelete else { return }

        Task { @MainActor in
            isLoading = true
            errorMessage = nil

            do {
                let categoryCount = try await repository.getCategoryCount()
                if categoryCount <= 1 {
                    errorMessage = "Cannot delete the last remaining category"
                    isLoading = false
                    showDeleteConfirmation = false
                    return
                }

                if deletePhotos {
                    try await photoRepository.deletePhotosByCategory(category.id)
                }

                try await repository.deleteCategory(category)
                showDeleteConfirmation = false
                categoryToDelete = nil
                loadCategoriesWithCounts()
            } catch {
                errorMessage = "Failed to delete category: \(error.localizedDescription)"
            }
            isLoading = false
        }
    }

    func cancelDelete() {
        showDeleteConfirmation = false
        categoryToDelete = nil
    }

    var hasPulseFAB: Bool {
        categoriesWithCounts.isEmpty
    }

    var deletionMessage: String {
        guard let category = categoryToDelete,
              let categoryWithCount = categoriesWithCounts.first(where: { $0.category.id == category.id }) else {
            return "Are you sure you want to delete this category?"
        }

        if categoryWithCount.photoCount > 0 {
            return "This category contains \(categoryWithCount.photoCount) photo\(categoryWithCount.photoCount == 1 ? "" : "s"). Deleting it will also delete all associated photos. Are you sure?"
        } else {
            return "Are you sure you want to delete '\(category.displayName)'?"
        }
    }
}