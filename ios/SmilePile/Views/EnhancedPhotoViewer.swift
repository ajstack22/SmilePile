import SwiftUI
import UIKit

// MARK: - Enhanced Photo Viewer
/// Full-screen photo viewer with zoom, swipe navigation, and metadata display
struct EnhancedPhotoViewer: View {
    @EnvironmentObject var viewModel: KidsModeViewModel
    @Binding var isPresented: Bool
    let initialPhotoIndex: Int

    @State private var currentCategoryIndex: Int = 0
    @State private var currentPhotoIndices: [Int64: Int] = [:] // Track photo index per category
    @State private var showMetadata = false
    @State private var currentScale: CGFloat = 1.0
    @State private var currentOffset: CGSize = .zero
    @State private var isDragging = false

    // Gesture state tracking
    @GestureState private var magnificationScale: CGFloat = 1.0
    @GestureState private var dragOffset: CGSize = .zero

    // Animation namespace for smooth transitions
    @Namespace private var animationNamespace

    // iPad-specific sizing
    private var isIPad: Bool {
        UIDevice.current.userInterfaceIdiom == .pad
    }

    private var metadataHeight: CGFloat {
        isIPad ? 200 : 150
    }

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Black background
                Color.black
                    .ignoresSafeArea()
                    .accessibilityHidden(true)

                // Main content
                if !viewModel.categories.isEmpty {
                    photoViewerContent(geometry: geometry)
                } else {
                    emptyStateView
                }

                // Overlay UI elements
                overlayElements(geometry: geometry)
            }
            .statusBarHidden(true)
            .persistentSystemOverlays(.hidden)
            .onAppear {
                setupInitialState()
            }
        }
    }

    // MARK: - Main Photo Viewer Content
    @ViewBuilder
    private func photoViewerContent(geometry: GeometryProxy) -> some View {
        TabView(selection: $currentCategoryIndex) {
            ForEach(Array(viewModel.categories.enumerated()), id: \.offset) { categoryIndex, category in
                categoryPhotoView(
                    category: category,
                    categoryIndex: categoryIndex,
                    geometry: geometry
                )
                .tag(categoryIndex)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .ignoresSafeArea()
        .onChange(of: currentCategoryIndex) { newIndex in
            handleCategoryChange(newIndex)
        }
    }

    // MARK: - Category Photo View
    private func categoryPhotoView(category: Category, categoryIndex: Int, geometry: GeometryProxy) -> some View {
        let photos = viewModel.getPhotosForCategory(category.id)

        return ZStack {
            if photos.isEmpty {
                emptyCategoryView(category: category)
            } else {
                photosPager(
                    photos: photos,
                    category: category,
                    geometry: geometry
                )
            }
        }
    }

    // MARK: - Photos Pager with Horizontal Swipe
    private func photosPager(photos: [Photo], category: Category, geometry: GeometryProxy) -> some View {
        TabView(selection: Binding(
            get: { currentPhotoIndices[category.id] ?? 0 },
            set: { newValue in
                currentPhotoIndices[category.id] = newValue
                // Reset zoom when switching photos
                withAnimation(.easeInOut(duration: 0.2)) {
                    currentScale = 1.0
                    currentOffset = .zero
                }
            }
        )) {
            ForEach(Array(photos.enumerated()), id: \.offset) { photoIndex, photo in
                ZoomablePhotoView(
                    photo: photo,
                    currentScale: $currentScale,
                    currentOffset: $currentOffset,
                    showMetadata: $showMetadata,
                    isPresented: $isPresented,
                    geometry: geometry
                )
                .tag(photoIndex)
                .accessibilityLabel("Photo \(photoIndex + 1) of \(photos.count): \(photo.displayName)")
                .accessibilityHint("Double tap to zoom, swipe horizontally to navigate between photos")
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
    }

    // MARK: - Overlay Elements
    @ViewBuilder
    private func overlayElements(geometry: GeometryProxy) -> some View {
        VStack {
            // Top navigation bar
            if showMetadata {
                topNavigationBar
                    .transition(.move(edge: .top).combined(with: .opacity))
            }

            Spacer()

            // Bottom metadata and controls
            if showMetadata {
                bottomMetadataView(geometry: geometry)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }

            // Photo index indicator
            if let currentCategory = viewModel.selectedCategory {
                let photos = viewModel.getPhotosForCategory(currentCategory.id)
                let currentIndex = currentPhotoIndices[currentCategory.id] ?? 0

                if !photos.isEmpty {
                    photoIndexIndicator(
                        currentIndex: currentIndex,
                        totalPhotos: photos.count
                    )
                    .padding(.bottom, showMetadata ? metadataHeight + 20 : 20)
                }
            }
        }
        .animation(.easeInOut(duration: 0.3), value: showMetadata)
    }

    // MARK: - Top Navigation Bar
    private var topNavigationBar: some View {
        HStack {
            Button(action: { isPresented = false }) {
                Image(systemName: "xmark.circle.fill")
                    .font(.title2)
                    .foregroundColor(.white)
                    .background(Color.black.opacity(0.5))
                    .clipShape(Circle())
            }
            .accessibilityLabel("Close photo viewer")

            Spacer()

            if let currentCategory = viewModel.selectedCategory {
                Text(currentCategory.displayName)
                    .font(.headline)
                    .foregroundColor(.white)
            }

            Spacer()

            shareButton
        }
        .padding()
        .background(
            LinearGradient(
                colors: [Color.black.opacity(0.7), Color.clear],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    // MARK: - Share Button
    private var shareButton: some View {
        Button(action: shareCurrentPhoto) {
            Image(systemName: "square.and.arrow.up")
                .font(.title2)
                .foregroundColor(.white)
                .background(Color.black.opacity(0.5))
                .clipShape(Circle())
        }
        .accessibilityLabel("Share photo")
    }

    // MARK: - Bottom Metadata View
    private func bottomMetadataView(geometry: GeometryProxy) -> some View {
        VStack(spacing: 12) {
            if let currentCategory = viewModel.selectedCategory,
               let currentIndex = currentPhotoIndices[currentCategory.id],
               let photo = viewModel.getPhotosForCategory(currentCategory.id)[safe: currentIndex] {

                PhotoMetadataOverlay(photo: photo, isIPad: isIPad)
                    .padding(.horizontal)
            }
        }
        .frame(height: metadataHeight)
        .frame(maxWidth: isIPad ? geometry.size.width * 0.6 : .infinity)
        .background(
            LinearGradient(
                colors: [Color.clear, Color.black.opacity(0.7)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    // MARK: - Photo Index Indicator
    private func photoIndexIndicator(currentIndex: Int, totalPhotos: Int) -> some View {
        HStack(spacing: 8) {
            ForEach(0..<totalPhotos, id: \.self) { index in
                Circle()
                    .fill(index == currentIndex ? Color.white : Color.white.opacity(0.5))
                    .frame(width: 8, height: 8)
                    .scaleEffect(index == currentIndex ? 1.2 : 1.0)
                    .animation(.easeInOut(duration: 0.2), value: currentIndex)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color.black.opacity(0.5))
        .cornerRadius(20)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Photo \(currentIndex + 1) of \(totalPhotos)")
    }

    // MARK: - Empty State Views
    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Image(systemName: "photo.stack")
                .font(.system(size: 64))
                .foregroundColor(.gray)

            Text("No photos available")
                .font(.title3)
                .foregroundColor(.gray)

            Button("Close") {
                isPresented = false
            }
            .buttonStyle(.bordered)
        }
        .accessibilityElement(children: .combine)
    }

    private func emptyCategoryView(category: Category) -> some View {
        VStack(spacing: 20) {
            Image(systemName: "photo")
                .font(.system(size: 64))
                .foregroundColor(.gray)

            Text("No photos in \(category.displayName)")
                .font(.title3)
                .foregroundColor(.gray)
        }
        .accessibilityElement(children: .combine)
    }

    // MARK: - Helper Methods
    private func setupInitialState() {
        if let selectedCategory = viewModel.selectedCategory,
           let categoryIndex = viewModel.categories.firstIndex(where: { $0.id == selectedCategory.id }) {
            currentCategoryIndex = categoryIndex
            currentPhotoIndices[selectedCategory.id] = initialPhotoIndex
        }
    }

    private func handleCategoryChange(_ newIndex: Int) {
        guard newIndex < viewModel.categories.count else { return }
        let category = viewModel.categories[newIndex]
        viewModel.selectCategory(category)

        // Initialize photo index for category if needed
        if currentPhotoIndices[category.id] == nil {
            currentPhotoIndices[category.id] = 0
        }

        // Reset zoom when changing categories
        withAnimation(.easeInOut(duration: 0.2)) {
            currentScale = 1.0
            currentOffset = .zero
        }
    }

    private func shareCurrentPhoto() {
        guard let currentCategory = viewModel.selectedCategory,
              let currentIndex = currentPhotoIndices[currentCategory.id],
              let photo = viewModel.getPhotosForCategory(currentCategory.id)[safe: currentIndex] else {
            return
        }

        // Present native share sheet
        let activityController = UIActivityViewController(
            activityItems: [PhotoShareItem(photo: photo)],
            applicationActivities: nil
        )

        // iPad specific positioning
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let window = windowScene.windows.first,
           let rootViewController = window.rootViewController {

            if isIPad {
                activityController.popoverPresentationController?.sourceView = window
                activityController.popoverPresentationController?.sourceRect = CGRect(
                    x: window.bounds.width - 100,
                    y: 100,
                    width: 1,
                    height: 1
                )
            }

            rootViewController.present(activityController, animated: true)
        }
    }
}

// MARK: - Zoomable Photo View
struct ZoomablePhotoView: View {
    let photo: Photo
    @Binding var currentScale: CGFloat
    @Binding var currentOffset: CGSize
    @Binding var showMetadata: Bool
    @Binding var isPresented: Bool
    let geometry: GeometryProxy

    @State private var steadyStateScale: CGFloat = 1.0
    @State private var steadyStateOffset: CGSize = .zero
    @GestureState private var gestureScale: CGFloat = 1.0
    @GestureState private var gestureOffset: CGSize = .zero

    private let minScale: CGFloat = 0.5
    private let maxScale: CGFloat = 4.0
    private let doubleTapScale: CGFloat = 2.5

    var body: some View {
        GeometryReader { imageGeometry in
            AsyncImageView(photo: photo, contentMode: .fit)
                .scaleEffect(currentScale)
                .offset(currentOffset)
                .animation(.interactiveSpring(), value: currentScale)
                .animation(.interactiveSpring(), value: currentOffset)
                .gesture(pinchGesture)
                .gesture(panGesture)
                .onTapGesture(count: 2, perform: handleDoubleTap)
                .onTapGesture(count: 1) {
                    withAnimation(.easeInOut(duration: 0.3)) {
                        showMetadata.toggle()
                    }
                }
                .accessibilityAddTraits(.isImage)
                .accessibilityLabel(photo.displayName)
                .accessibilityHint("Double tap to zoom, single tap to show or hide information")
        }
    }

    // MARK: - Gestures
    private var pinchGesture: some Gesture {
        MagnificationGesture()
            .updating($gestureScale) { value, state, _ in
                state = value
            }
            .onChanged { value in
                let newScale = steadyStateScale * value
                currentScale = min(max(newScale, minScale), maxScale)
            }
            .onEnded { value in
                steadyStateScale = currentScale

                // Snap to bounds if zoomed out too much
                if currentScale < 1.0 {
                    withAnimation(.spring()) {
                        currentScale = 1.0
                        currentOffset = .zero
                        steadyStateScale = 1.0
                        steadyStateOffset = .zero
                    }
                }
            }
    }

    private var panGesture: some Gesture {
        DragGesture()
            .updating($gestureOffset) { value, state, _ in
                if currentScale > 1.0 {
                    state = value.translation
                }
            }
            .onChanged { value in
                if currentScale > 1.0 {
                    currentOffset = CGSize(
                        width: steadyStateOffset.width + value.translation.width,
                        height: steadyStateOffset.height + value.translation.height
                    )
                }
            }
            .onEnded { _ in
                steadyStateOffset = currentOffset
                constrainOffset()
            }
    }

    private func handleDoubleTap() {
        withAnimation(.spring()) {
            if currentScale > 1.0 {
                // Reset to normal
                currentScale = 1.0
                currentOffset = .zero
                steadyStateScale = 1.0
                steadyStateOffset = .zero
            } else {
                // Zoom in
                currentScale = doubleTapScale
                steadyStateScale = doubleTapScale
                // Center on tap point would require more complex calculation
                // For now, just zoom to center
            }
        }
    }

    private func constrainOffset() {
        let maxOffsetX = (geometry.size.width * (currentScale - 1)) / 2
        let maxOffsetY = (geometry.size.height * (currentScale - 1)) / 2

        withAnimation(.spring()) {
            currentOffset.width = min(max(currentOffset.width, -maxOffsetX), maxOffsetX)
            currentOffset.height = min(max(currentOffset.height, -maxOffsetY), maxOffsetY)
            steadyStateOffset = currentOffset
        }
    }
}

// MARK: - Photo Metadata Overlay
struct PhotoMetadataOverlay: View {
    let photo: Photo
    let isIPad: Bool

    private var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Photo name
            Text(photo.displayName)
                .font(isIPad ? .title2 : .headline)
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .lineLimit(1)

            // Date taken
            HStack {
                Image(systemName: "calendar")
                    .font(.caption)
                Text(dateFormatter.string(from: photo.createdDate))
                    .font(.subheadline)
            }
            .foregroundColor(.white.opacity(0.9))

            // Dimensions and size
            HStack(spacing: 20) {
                if photo.width > 0 && photo.height > 0 {
                    HStack {
                        Image(systemName: "aspectratio")
                            .font(.caption)
                        Text("\(photo.width) × \(photo.height)")
                            .font(.subheadline)
                    }
                }

                if photo.fileSize > 0 {
                    HStack {
                        Image(systemName: "doc")
                            .font(.caption)
                        Text(photo.formattedFileSize)
                            .font(.subheadline)
                    }
                }
            }
            .foregroundColor(.white.opacity(0.9))
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.black.opacity(0.6))
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.white.opacity(0.2), lineWidth: 1)
                )
        )
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Photo Share Item
class PhotoShareItem: NSObject, UIActivityItemSource {
    let photo: Photo

    init(photo: Photo) {
        self.photo = photo
        super.init()
    }

    func activityViewControllerPlaceholderItem(_ activityViewController: UIActivityViewController) -> Any {
        return UIImage() // Placeholder
    }

    func activityViewController(_ activityViewController: UIActivityViewController, itemForActivityType activityType: UIActivity.ActivityType?) -> Any? {
        // Load the actual image
        if photo.isFromAssets {
            return UIImage(named: photo.path)
        } else {
            return UIImage(contentsOfFile: photo.path)
        }
    }

    func activityViewController(_ activityViewController: UIActivityViewController, subjectForActivityType activityType: UIActivity.ActivityType?) -> String {
        return photo.displayName
    }
}

// MARK: - Safe Array Extension
extension Array {
    subscript(safe index: Index) -> Element? {
        return indices.contains(index) ? self[index] : nil
    }
}

// MARK: - Preview
#Preview("Photo Viewer") {
    EnhancedPhotoViewer(
        isPresented: .constant(true),
        initialPhotoIndex: 0
    )
    .environmentObject(KidsModeViewModel())
}

#Preview("Photo Viewer - iPad") {
    EnhancedPhotoViewer(
        isPresented: .constant(true),
        initialPhotoIndex: 0
    )
    .environmentObject(KidsModeViewModel())
}