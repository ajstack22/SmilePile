import SwiftUI

/// Kids Mode Gallery Screen - Simplified photo viewing for children
/// Matches Android KidsModeGalleryScreen behavior exactly
struct KidsModeGalleryView: View {
    @ObservedObject var viewModel: KidsModeViewModel
    @State private var selectedPhotoIndex: Int?
    @State private var showFullscreenViewer = false
    @State private var dragOffset: CGSize = .zero
    @State private var lastSwipeTime = Date.distantPast

    // Configuration matching Android
    private let swipeThreshold: CGFloat = 100 // 100px threshold as per Android
    private let swipeDebounceInterval: TimeInterval = 0.3 // 300ms debounce

    // Filter photos by selected category
    private var displayedPhotos: [Photo] {
        guard let selectedCategory = viewModel.selectedCategory else {
            return []
        }
        return viewModel.getPhotosForCategory(selectedCategory.id)
    }

    var body: some View {
        ZStack {
            // Main content
            VStack(spacing: 0) {
                // Category filter at top (floating bar style)
                if !viewModel.categories.isEmpty {
                    CategoryFilterView(
                        categories: viewModel.categories,
                        selectedCategory: viewModel.selectedCategory,
                        onCategorySelected: { category in
                            viewModel.selectCategory(category)
                        },
                        onExitKidsMode: {
                            viewModel.requestModeToggle()
                        }
                    )
                    .zIndex(1)
                }

                // Photo grid or empty state
                if displayedPhotos.isEmpty {
                    KidsEmptyGalleryView()
                } else {
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            ForEach(Array(displayedPhotos.enumerated()), id: \.element.id) { index, photo in
                                PhotoGridItem(photo: photo)
                                    .id(photo.id)
                                    .onTapGesture {
                                        selectedPhotoIndex = index
                                        showFullscreenViewer = true
                                        viewModel.setFullscreen(true)
                                    }
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.top, 8)
                        .padding(.bottom, 16)
                    }
                }
            }
            .gesture(swipeGesture)

            // Fullscreen photo viewer overlay
            if showFullscreenViewer, let initialIndex = selectedPhotoIndex {
                KidsPhotoViewer(
                    photosByCategory: createPhotosByCategory(),
                    categories: viewModel.categories,
                    selectedCategory: viewModel.selectedCategory,
                    initialPhotoIndex: initialIndex,
                    onDismiss: {
                        showFullscreenViewer = false
                        viewModel.setFullscreen(false)
                    },
                    onCategoryChange: { category in
                        viewModel.selectCategory(category) // This already shows the toast internally
                        // When category changes in fullscreen, update to first photo of new category
                        let newPhotos = viewModel.getPhotosForCategory(category.id)
                        if !newPhotos.isEmpty {
                            selectedPhotoIndex = 0
                        }
                    }
                )
                .transition(.opacity)
                .zIndex(2)
            }

            // Note: Category toasts are now handled by the centralized ToastManager
            // which is integrated at the root level with .toastOverlay()
        }
        .onAppear {
            // Load real data from repositories
            Task {
                await viewModel.loadData()

                // Select first category after loading if none selected
                if viewModel.selectedCategory == nil && !viewModel.categories.isEmpty {
                    viewModel.selectCategory(viewModel.categories[0])
                }
            }
        }
    }

    // MARK: - Helper Methods

    private func createPhotosByCategory() -> [Int64: [Photo]] {
        var result: [Int64: [Photo]] = [:]
        for category in viewModel.categories {
            result[category.id] = viewModel.getPhotosForCategory(category.id)
        }
        return result
    }

    // MARK: - Swipe Gesture

    private var swipeGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                dragOffset = value.translation
            }
            .onEnded { value in
                let horizontalDrag = value.translation.width
                let currentTime = Date()

                // Check debounce timing
                guard currentTime.timeIntervalSince(lastSwipeTime) >= swipeDebounceInterval else {
                    dragOffset = .zero
                    return
                }

                // Check swipe threshold and direction
                if abs(horizontalDrag) > swipeThreshold {
                    lastSwipeTime = currentTime

                    if horizontalDrag < -swipeThreshold {
                        // Swipe left - next category
                        viewModel.navigateToNextCategory()
                    } else if horizontalDrag > swipeThreshold {
                        // Swipe right - previous category
                        viewModel.navigateToPreviousCategory()
                    }
                }

                dragOffset = .zero
            }
    }
}

// MARK: - Photo Grid Item

private struct PhotoGridItem: View {
    let photo: Photo

    var body: some View {
        // Use the same AsyncImageView component as parent gallery for consistent loading
        AsyncImageView(photo: photo, contentMode: .fill)
            .aspectRatio(4/3, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Empty Gallery View

private struct KidsEmptyGalleryView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "camera.fill")
                .font(.system(size: 72))
                .foregroundColor(.secondary)

            Text("No photos yet!")
                .font(.title)
                .fontWeight(.semibold)
                .foregroundColor(.secondary)

            Text("Ask a parent to add some photos")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// Note: CategoryToastOverlay removed - now using centralized ToastManager