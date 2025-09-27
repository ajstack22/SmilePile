import SwiftUI

/// Fullscreen photo viewer for Kids Mode with vertical and horizontal paging
/// Matches Android ZoomedPhotoOverlay exactly
struct KidsPhotoViewer: View {
    let photos: [Photo]
    let categories: [Category]
    let selectedCategory: Category?
    let initialPhotoIndex: Int
    let onDismiss: () -> Void
    let onCategoryChange: (Category) -> Void

    @State private var currentPhotoIndex: Int
    @State private var currentCategoryIndex: Int
    @State private var opacity: Double = 0
    @State private var scale: CGFloat = 0.9
    @State private var verticalOffset: CGFloat = 0
    @State private var horizontalOffset: CGFloat = 0
    @State private var isDragging = false

    // Swipe thresholds
    private let dismissThreshold: CGFloat = 100
    private let categorySwipeThreshold: CGFloat = 100
    private let photoSwipeThreshold: CGFloat = 50

    init(photos: [Photo],
         categories: [Category],
         selectedCategory: Category?,
         initialPhotoIndex: Int,
         onDismiss: @escaping () -> Void,
         onCategoryChange: @escaping (Category) -> Void) {
        self.photos = photos
        self.categories = categories
        self.selectedCategory = selectedCategory
        self.initialPhotoIndex = initialPhotoIndex
        self.onDismiss = onDismiss
        self.onCategoryChange = onCategoryChange

        // Initialize state
        _currentPhotoIndex = State(initialValue: initialPhotoIndex)

        // Find current category index
        let categoryIndex = categories.firstIndex(where: { $0.id == selectedCategory?.id }) ?? 0
        _currentCategoryIndex = State(initialValue: categoryIndex)
    }

    var body: some View {
        ZStack {
            // Black background
            Color.black
                .ignoresSafeArea()
                .opacity(opacity)

            // Photo pager
            if !photos.isEmpty && currentPhotoIndex < photos.count {
                TabView(selection: $currentPhotoIndex) {
                    ForEach(Array(photos.enumerated()), id: \.element.id) { index, photo in
                        PhotoPageView(photo: photo)
                            .tag(index)
                            .scaleEffect(scale)
                            .offset(y: verticalOffset)
                            .gesture(combinedGesture)
                    }
                }
                .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))
                .ignoresSafeArea()
                .opacity(opacity)
            } else {
                // Empty state for category
                VStack(spacing: 16) {
                    Image(systemName: "photo")
                        .font(.system(size: 72))
                        .foregroundColor(.white.opacity(0.5))

                    if let category = selectedCategory {
                        Text("No photos in \(category.displayName)")
                            .font(.title2)
                            .foregroundColor(.white.opacity(0.8))
                    }
                }
                .onTapGesture {
                    onDismiss()
                }
            }
        }
        .statusBarHidden(true)
        .onAppear {
            withAnimation(.easeOut(duration: 0.3)) {
                opacity = 1
                scale = 1
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Photo viewer")
        .accessibilityHint("Swipe vertically to browse photos, horizontally to change categories, tap to dismiss")
    }

    // MARK: - Gestures

    private var combinedGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                isDragging = true

                // Track vertical offset for photo navigation
                if abs(value.translation.height) > abs(value.translation.width) {
                    verticalOffset = value.translation.height * 0.5 // Dampen the effect
                }

                // Track horizontal offset for category navigation
                else {
                    horizontalOffset = value.translation.width
                }
            }
            .onEnded { value in
                isDragging = false

                // Vertical swipe - navigate photos
                if abs(value.translation.height) > abs(value.translation.width) {
                    if value.translation.height < -photoSwipeThreshold && currentPhotoIndex < photos.count - 1 {
                        // Swipe up - next photo
                        withAnimation {
                            currentPhotoIndex += 1
                            verticalOffset = 0
                        }
                    } else if value.translation.height > photoSwipeThreshold && currentPhotoIndex > 0 {
                        // Swipe down - previous photo
                        withAnimation {
                            currentPhotoIndex -= 1
                            verticalOffset = 0
                        }
                    } else if abs(value.translation.height) > dismissThreshold {
                        // Large swipe - dismiss viewer
                        dismissViewer()
                    } else {
                        // Snap back
                        withAnimation {
                            verticalOffset = 0
                        }
                    }
                }

                // Horizontal swipe - navigate categories
                else if abs(value.translation.width) > categorySwipeThreshold {
                    if value.translation.width < -categorySwipeThreshold {
                        // Swipe left - next category
                        navigateToNextCategory()
                    } else {
                        // Swipe right - previous category
                        navigateToPreviousCategory()
                    }
                    withAnimation {
                        horizontalOffset = 0
                    }
                } else {
                    // Snap back
                    withAnimation {
                        horizontalOffset = 0
                    }
                }
            }
            .simultaneously(with: TapGesture()
                .onEnded { _ in
                    dismissViewer()
                }
            )
    }

    // MARK: - Navigation

    private func navigateToNextCategory() {
        let nextIndex = (currentCategoryIndex + 1) % categories.count
        currentCategoryIndex = nextIndex
        let newCategory = categories[nextIndex]
        onCategoryChange(newCategory)

        // Reset to first photo of new category
        currentPhotoIndex = 0
    }

    private func navigateToPreviousCategory() {
        let prevIndex = currentCategoryIndex > 0 ? currentCategoryIndex - 1 : categories.count - 1
        currentCategoryIndex = prevIndex
        let newCategory = categories[prevIndex]
        onCategoryChange(newCategory)

        // Reset to first photo of new category
        currentPhotoIndex = 0
    }

    private func dismissViewer() {
        withAnimation(.easeIn(duration: 0.2)) {
            opacity = 0
            scale = 0.9
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            onDismiss()
        }
    }
}

// MARK: - Photo Page View

private struct PhotoPageView: View {
    let photo: Photo
    @State private var imageScale: CGFloat = 1
    @State private var imageOffset: CGSize = .zero
    @State private var lastScale: CGFloat = 1

    var body: some View {
        GeometryReader { geometry in
            AsyncImage(url: URL(fileURLWithPath: photo.path)) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: geometry.size.width, height: geometry.size.height)
                        .scaleEffect(imageScale)
                        .offset(imageOffset)
                        .gesture(magnificationGesture)
                        .accessibilityLabel("Photo \(photo.displayName)")

                case .failure:
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 48))
                            .foregroundColor(.white.opacity(0.5))

                        Text("Failed to load photo")
                            .foregroundColor(.white.opacity(0.5))
                    }
                    .frame(width: geometry.size.width, height: geometry.size.height)

                case .empty:
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .frame(width: geometry.size.width, height: geometry.size.height)

                @unknown default:
                    EmptyView()
                }
            }
        }
        .ignoresSafeArea()
    }

    // MARK: - Pinch to Zoom

    private var magnificationGesture: some Gesture {
        MagnificationGesture()
            .onChanged { value in
                imageScale = lastScale * value
            }
            .onEnded { value in
                withAnimation(.spring()) {
                    let newScale = lastScale * value

                    // Limit scale between 1x and 5x
                    if newScale < 1 {
                        imageScale = 1
                        lastScale = 1
                        imageOffset = .zero
                    } else if newScale > 5 {
                        imageScale = 5
                        lastScale = 5
                    } else {
                        imageScale = newScale
                        lastScale = newScale
                    }

                    // Reset offset if zoomed out
                    if imageScale == 1 {
                        imageOffset = .zero
                    }
                }
            }
            .simultaneously(with: DragGesture()
                .onChanged { value in
                    if imageScale > 1 {
                        imageOffset = CGSize(
                            width: imageOffset.width + value.translation.width,
                            height: imageOffset.height + value.translation.height
                        )
                    }
                }
            )
    }
}