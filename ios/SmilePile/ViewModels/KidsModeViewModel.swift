import Foundation
import SwiftUI
import Combine

@MainActor
class KidsModeViewModel: ObservableObject {
    @Published var isKidsMode = false
    @Published var isFullscreen = false
    @Published var selectedCategory: Category?
    @Published var photos: [Photo] = []
    @Published var categories: [Category] = []
    @Published var requiresPINAuth = false
    // Toast properties removed - now using centralized ToastManager

    // Swipe navigation properties
    @Published var lastCategorySwipeTime = Date.distantPast
    private let swipeDebounceInterval: TimeInterval = 0.3 // 300ms
    let swipeThreshold: CGFloat = 150 // 150px threshold

    // Repositories for loading real data
    private let photoRepository: PhotoRepository
    private let categoryRepository: CategoryRepository
    private var cancellables = Set<AnyCancellable>()

    init(photoRepository: PhotoRepository = PhotoRepositoryImpl(),
         categoryRepository: CategoryRepository = CategoryRepositoryImpl.shared) {
        self.photoRepository = photoRepository
        self.categoryRepository = categoryRepository
    }

    // Load real photos and categories from repositories
    func loadData() async {
        do {
            // Load categories
            categories = try await categoryRepository.getAllCategories()

            // Load all photos
            photos = try await photoRepository.getAllPhotos()

            // Select first category by default if none selected
            if selectedCategory == nil && !categories.isEmpty {
                selectedCategory = categories.first
            }
        } catch {
            print("Error loading data: \(error)")
            // Fall back to empty arrays on error
            categories = []
            photos = []
        }
    }


    func toggleKidsMode() {
        if isKidsMode {
            // Exiting Kids Mode requires PIN
            requiresPINAuth = true
        } else {
            // Entering Kids Mode doesn't require PIN
            isKidsMode = true
        }
    }

    func requestModeToggle() {
        // Request mode toggle - triggers PIN/biometric authentication flow
        requiresPINAuth = true
    }

    func exitKidsMode(authenticated: Bool) {
        if authenticated {
            isKidsMode = false
            isFullscreen = false
            requiresPINAuth = false
        }
    }

    func onPhotoViewed(_ photo: Photo) {
        // Photo viewed - currently no tracking needed
    }

    func setFullscreen(_ fullscreen: Bool) {
        isFullscreen = fullscreen
    }

    func selectCategory(_ category: Category) {
        selectedCategory = category
        showCategoryToast(category)
    }

    // MARK: - Swipe Navigation

    func canSwipeCategory() -> Bool {
        return Date().timeIntervalSince(lastCategorySwipeTime) >= swipeDebounceInterval
    }

    func navigateToPreviousCategory() {
        guard canSwipeCategory(),
              let currentCategory = selectedCategory,
              let currentIndex = categories.firstIndex(where: { $0.id == currentCategory.id }) else { return }

        let previousIndex = currentIndex > 0 ? currentIndex - 1 : categories.count - 1
        lastCategorySwipeTime = Date()
        selectCategory(categories[previousIndex])
    }

    func navigateToNextCategory() {
        guard canSwipeCategory(),
              let currentCategory = selectedCategory,
              let currentIndex = categories.firstIndex(where: { $0.id == currentCategory.id }) else { return }

        let nextIndex = (currentIndex + 1) % categories.count
        lastCategorySwipeTime = Date()
        selectCategory(categories[nextIndex])
    }

    func getPhotosForCategory(_ categoryId: Int64?) -> [Photo] {
        guard let categoryId = categoryId else {
            return photos
        }
        return photos.filter { $0.categoryId == categoryId }
    }

    func showCategoryToast(_ category: Category) {
        // Use centralized ToastManager for category toasts
        ToastManager.shared.showCategoryToast(category)
    }
}