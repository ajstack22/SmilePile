import Foundation
import SwiftUI
import Combine

class KidsModeViewModel: ObservableObject {
    @Published var isKidsMode = false
    @Published var isFullscreen = false
    @Published var selectedCategory: Category?
    @Published var photos: [Photo] = []
    @Published var categories: [Category] = []
    @Published var requiresPINAuth = false
    @Published var toastMessage: String?
    @Published var toastColor: Color?

    // Swipe navigation properties
    @Published var lastCategorySwipeTime = Date.distantPast
    private let swipeDebounceInterval: TimeInterval = 0.3 // 300ms
    let swipeThreshold: CGFloat = 150 // 150px threshold


    private var cancellables = Set<AnyCancellable>()

    init() {
        loadMockData()
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
        withAnimation {
            toastMessage = category.displayName
            toastColor = category.color
        }

        // Auto-hide after 2 seconds
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation {
                self.toastMessage = nil
                self.toastColor = nil
            }
        }
    }

    // Mock data for testing
    private func loadMockData() {
        // Create sample categories
        categories = [
            Category(id: 1, name: "family", displayName: "Family", position: 0, colorHex: "#4CAF50"),
            Category(id: 2, name: "vacation", displayName: "Vacation", position: 1, colorHex: "#2196F3"),
            Category(id: 3, name: "pets", displayName: "Pets", position: 2, colorHex: "#FF9800"),
            Category(id: 4, name: "school", displayName: "School", position: 3, colorHex: "#9C27B0")
        ]

        // Create sample photos
        if !categories.isEmpty {
            photos = [
                Photo(path: "sample1", categoryId: categories[0].id),
                Photo(path: "sample2", categoryId: categories[0].id),
                Photo(path: "sample3", categoryId: categories[1].id),
                Photo(path: "sample4", categoryId: categories[2].id),
                Photo(path: "sample5", categoryId: categories[2].id),
                Photo(path: "sample6", categoryId: categories[3].id)
            ]
        }

        // Select first category by default
        selectedCategory = categories.first
    }
}