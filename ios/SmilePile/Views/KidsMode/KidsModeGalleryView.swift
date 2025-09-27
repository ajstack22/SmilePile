import SwiftUI

/// Kids Mode Gallery Screen - Simplified photo viewing for children
/// Matches Android KidsModeGalleryScreen behavior exactly
struct KidsModeGalleryView: View {
    @ObservedObject var viewModel: KidsModeViewModel
    @StateObject private var galleryViewModel = PhotoGalleryViewModel()
    @State private var selectedPhotoIndex: Int?
    @State private var showFullscreenViewer = false
    @State private var dragOffset: CGSize = .zero
    @State private var lastSwipeTime = Date.distantPast

    // Configuration matching Android
    private let swipeThreshold: CGFloat = 100 // 100px threshold as per Android
    private let swipeDebounceInterval: TimeInterval = 0.3 // 300ms debounce

    // Device-specific layout
    private var columns: [GridItem] {
        let columnCount = UIDevice.current.userInterfaceIdiom == .pad ? 5 : 3
        return Array(repeating: GridItem(.flexible(), spacing: 2), count: columnCount)
    }

    // Filter photos by selected category
    private var displayedPhotos: [Photo] {
        guard let selectedCategory = viewModel.selectedCategory else {
            // If no category selected, select first category (mandatory selection)
            if let firstCategory = viewModel.categories.first {
                DispatchQueue.main.async {
                    viewModel.selectCategory(firstCategory)
                }
            }
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
                        }
                    )
                    .background(Color(UIColor.systemBackground))
                    .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
                    .zIndex(1)
                }

                // Photo grid or empty state
                if displayedPhotos.isEmpty {
                    KidsEmptyGalleryView()
                } else {
                    ScrollView {
                        ScrollViewReader { proxy in
                            LazyVGrid(columns: columns, spacing: 2) {
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
                            .padding(.vertical, 8)
                        }
                    }
                }
            }
            .gesture(swipeGesture)

            // Fullscreen photo viewer overlay
            if showFullscreenViewer, let initialIndex = selectedPhotoIndex {
                KidsPhotoViewer(
                    photos: displayedPhotos,
                    categories: viewModel.categories,
                    selectedCategory: viewModel.selectedCategory,
                    initialPhotoIndex: initialIndex,
                    onDismiss: {
                        showFullscreenViewer = false
                        viewModel.setFullscreen(false)
                    },
                    onCategoryChange: { category in
                        viewModel.selectCategory(category)
                        // Show category toast when changed in fullscreen mode
                        viewModel.showCategoryToast(category)
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
            // Initialize with first category if none selected
            if viewModel.selectedCategory == nil && !viewModel.categories.isEmpty {
                viewModel.selectCategory(viewModel.categories[0])
            }

            // Load photos
            Task {
                await galleryViewModel.loadPhotos()
            }
        }
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
        GeometryReader { geometry in
            AsyncImage(url: URL(fileURLWithPath: photo.path)) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: geometry.size.width, height: geometry.size.width * 0.75) // 4:3 aspect ratio
                        .clipped()
                        .clipShape(RoundedRectangle(cornerRadius: 12))

                case .failure:
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.gray.opacity(0.3))
                        .overlay {
                            Image(systemName: "photo")
                                .foregroundColor(.gray)
                        }

                case .empty:
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color.gray.opacity(0.2))
                        .overlay {
                            ProgressView()
                        }

                @unknown default:
                    EmptyView()
                }
            }
        }
        .aspectRatio(4/3, contentMode: .fit) // Maintain 4:3 aspect ratio
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