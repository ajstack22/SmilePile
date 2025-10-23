import SwiftUI

/// Fullscreen photo viewer for Kids Mode with vertical and horizontal paging
/// Matches Android ZoomedPhotoOverlay: HorizontalPager (categories) → VerticalPager (photos)
struct KidsPhotoViewer: View {
    let photosByCategory: [Int64: [Photo]]
    let categories: [Category]
    let selectedCategory: Category?
    let initialPhotoIndex: Int
    let onDismiss: () -> Void
    let onCategoryChange: (Category) -> Void

    @State private var currentPhotoIndex: Int
    @State private var currentCategoryIndex: Int
    @State private var lastNotifiedCategoryIndex: Int
    @State private var opacity: Double = 0
    @State private var scale: CGFloat = 0.9

    // MARK: - Gesture State Management

    enum GestureState: Equatable {
        case idle
        case zooming
        case panning
        case draggingVertical
        case swipingHorizontal
    }

    @State private var activeGesture: GestureState = .idle

    init(photosByCategory: [Int64: [Photo]],
         categories: [Category],
         selectedCategory: Category?,
         initialPhotoIndex: Int,
         onDismiss: @escaping () -> Void,
         onCategoryChange: @escaping (Category) -> Void) {
        self.photosByCategory = photosByCategory
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
        _lastNotifiedCategoryIndex = State(initialValue: categoryIndex)
    }

    var body: some View {
        ZStack {
            // Black background
            Color.black
                .ignoresSafeArea()
                .opacity(opacity)

            // Nested TabView structure: Horizontal (categories) → Vertical (photos)
            if !categories.isEmpty {
                // Outer TabView: Horizontal paging for categories
                TabView(selection: $currentCategoryIndex) {
                    ForEach(Array(categories.enumerated()), id: \.element.id) { categoryIndex, category in
                        // Get photos for this category
                        let categoryPhotos = getPhotosForCategory(category)

                        if categoryPhotos.isEmpty {
                            // Empty state
                            VStack(spacing: 16) {
                                Image(systemName: "photo")
                                    .font(.system(size: 72))
                                    .foregroundColor(.white.opacity(0.5))

                                Text("No photos in \(category.displayName)")
                                    .font(.title2)
                                    .foregroundColor(.white.opacity(0.8))
                            }
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .onTapGesture {
                                dismissViewer()
                            }
                            .tag(categoryIndex)
                        } else {
                            // Inner component: Vertical paging for photos
                            VerticalPhotoPagerView(
                                photos: categoryPhotos,
                                currentPhotoIndex: $currentPhotoIndex,
                                activeGesture: $activeGesture,
                                onDismiss: dismissViewer
                            )
                            .tag(categoryIndex)
                        }
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .ignoresSafeArea()
                .opacity(opacity)
            } else {
                // No categories at all
                VStack(spacing: 16) {
                    Image(systemName: "photo")
                        .font(.system(size: 72))
                        .foregroundColor(.white.opacity(0.5))

                    Text("No categories available")
                        .font(.title2)
                        .foregroundColor(.white.opacity(0.8))
                }
                .onTapGesture {
                    onDismiss()
                }
            }
        }
        .statusBarHidden(true)
        .persistentSystemOverlays(.hidden)
        .defersSystemGestures(on: .all)
        .onAppear {
            withAnimation(.easeOut(duration: 0.3)) {
                opacity = 1
                scale = 1
            }
        }
        .onChange(of: currentCategoryIndex) { newIndex in
            // Category changed via horizontal swipe - only notify if actually changed
            guard newIndex != lastNotifiedCategoryIndex else { return }
            guard newIndex >= 0 && newIndex < categories.count else { return }

            lastNotifiedCategoryIndex = newIndex
            let newCategory = categories[newIndex]
            onCategoryChange(newCategory)
            // Reset to first photo when category changes
            currentPhotoIndex = 0
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Photo viewer")
        .accessibilityHint("Swipe up or down to browse photos, left or right to change categories, tap to dismiss")
    }

    // MARK: - Helper Methods

    private func getPhotosForCategory(_ category: Category) -> [Photo] {
        return photosByCategory[category.id] ?? []
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

// MARK: - Vertical Photo Pager View

private struct VerticalPhotoPagerView: View {
    let photos: [Photo]
    @Binding var currentPhotoIndex: Int
    @Binding var activeGesture: KidsPhotoViewer.GestureState
    let onDismiss: () -> Void

    @State private var dragOffset: CGFloat = 0
    @State private var isDragging: Bool = false

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Render current photo and adjacent photos for smooth transitions
                ForEach(Array(photos.enumerated()), id: \.element.id) { index, photo in
                    if shouldRenderPhoto(index) {
                        PhotoPageView(
                            photo: photo,
                            activeGesture: $activeGesture
                        )
                        .offset(y: offsetForPhoto(index, screenHeight: geometry.size.height))
                        .opacity(opacityForPhoto(index))
                    }
                }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .simultaneousGesture(verticalDragGesture(screenHeight: geometry.size.height))
            .onTapGesture {
                if activeGesture == .idle {
                    onDismiss()
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityHint("Swipe up for next photo, down for previous photo, tap to dismiss")
    }

    // MARK: - Helper Methods

    private func shouldRenderPhoto(_ index: Int) -> Bool {
        // Only render current photo and adjacent photos for performance
        return abs(index - currentPhotoIndex) <= 1
    }

    private func offsetForPhoto(_ index: Int, screenHeight: CGFloat) -> CGFloat {
        let baseOffset = CGFloat(index - currentPhotoIndex) * screenHeight
        return baseOffset + dragOffset
    }

    private func opacityForPhoto(_ index: Int) -> Double {
        if index == currentPhotoIndex {
            return 1.0
        }
        if abs(index - currentPhotoIndex) == 1 && abs(dragOffset) > 50 {
            return 0.5
        }
        return 0.0
    }

    // MARK: - Vertical Drag Gesture

    private func verticalDragGesture(screenHeight: CGFloat) -> some Gesture {
        DragGesture(minimumDistance: 20)
            .onChanged { value in
                // Don't allow vertical photo navigation if zoomed or panning
                guard activeGesture == .idle || activeGesture == .draggingVertical else {
                    return
                }

                // Only respond to primarily vertical drags
                let horizontalAmount = abs(value.translation.width)
                let verticalAmount = abs(value.translation.height)

                // Check if this is a vertical drag BEFORE locking gesture state
                if verticalAmount > horizontalAmount * 1.5 {
                    // Lock gesture state
                    if activeGesture == .idle {
                        activeGesture = .draggingVertical
                    }

                    isDragging = true
                    dragOffset = value.translation.height
                }
            }
            .onEnded { value in
                defer { activeGesture = .idle }

                guard isDragging else { return }

                let verticalVelocity = value.predictedEndTranslation.height - value.translation.height
                let threshold = screenHeight * 0.3

                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                    // Swipe down (show previous photo)
                    if value.translation.height > threshold || verticalVelocity > 500 {
                        if currentPhotoIndex > 0 {
                            currentPhotoIndex -= 1
                        }
                    }
                    // Swipe up (show next photo)
                    else if value.translation.height < -threshold || verticalVelocity < -500 {
                        if currentPhotoIndex < photos.count - 1 {
                            currentPhotoIndex += 1
                        }
                    }

                    // Reset drag state
                    dragOffset = 0
                    isDragging = false
                }
            }
    }
}

// MARK: - Photo Page View

private struct PhotoPageView: View {
    let photo: Photo
    @Binding var activeGesture: KidsPhotoViewer.GestureState
    @State private var imageScale: CGFloat = 1
    @State private var imageOffset: CGSize = .zero
    @State private var lastScale: CGFloat = 1
    @State private var lastPanOffset: CGSize = .zero

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
                        .simultaneousGesture(magnificationGesture)
                        .simultaneousGesture(imageScale > 1 ? panGesture : nil)
                        .allowsHitTesting(true)
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
                // Lock gesture state to prevent conflicts
                if activeGesture == .idle {
                    activeGesture = .zooming
                }

                guard activeGesture == .zooming else { return }

                let newScale = lastScale * value
                // Clamp between 1x and 5x during gesture to prevent snap-back
                imageScale = min(max(newScale, 1.0), 5.0)
            }
            .onEnded { value in
                defer { activeGesture = .idle } // Always release lock

                let finalScale = lastScale * value

                withAnimation(.spring()) {
                    // Clamp final scale between 1x and 5x
                    imageScale = min(max(finalScale, 1.0), 5.0)
                    lastScale = imageScale

                    // Reset offsets if zoomed out
                    if imageScale == 1 {
                        imageOffset = .zero
                        lastPanOffset = .zero
                    }
                }
            }
    }

    private var panGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                // Lock gesture state for panning if zoomed in
                if imageScale > 1 {
                    if activeGesture == .idle {
                        activeGesture = .panning
                    }

                    guard activeGesture == .panning || activeGesture == .zooming else { return }
                    // Use relative offset to avoid exponential accumulation
                    imageOffset = CGSize(
                        width: lastPanOffset.width + value.translation.width,
                        height: lastPanOffset.height + value.translation.height
                    )
                }
            }
            .onEnded { _ in
                if imageScale > 1 {
                    // Save final offset for next pan gesture
                    lastPanOffset = imageOffset
                }
                if activeGesture == .panning {
                    activeGesture = .idle
                }
            }
    }
}
