import SwiftUI

/// Optimized thumbnail view with loading states and memory-efficient rendering
struct PhotoThumbnailView: View {
    let photo: Photo
    @StateObject private var loader = ThumbnailLoader()
    @State private var isVisible = false
    @Environment(\.horizontalSizeClass) var sizeClass

    // Configuration
    private let cornerRadius: CGFloat = 8
    private let animationDuration: Double = 0.2

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Background placeholder
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(Color.gray.opacity(0.2))

                // Thumbnail image
                if let image = loader.image {
                    Image(uiImage: image)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: geometry.size.width, height: geometry.size.height)
                        .clipped()
                        .transition(.opacity)
                        .animation(.easeInOut(duration: animationDuration), value: loader.image)
                } else {
                    // Loading placeholder
                    if loader.isLoading {
                        ProgressView()
                            .scaleEffect(0.5)
                            .frame(width: geometry.size.width, height: geometry.size.height)
                    } else if loader.error != nil {
                        // Error state
                        Image(systemName: "exclamationmark.triangle")
                            .foregroundColor(.gray)
                            .font(.system(size: min(geometry.size.width, geometry.size.height) * 0.3))
                    } else {
                        // Empty placeholder
                        Image(systemName: "photo")
                            .foregroundColor(.gray.opacity(0.5))
                            .font(.system(size: min(geometry.size.width, geometry.size.height) * 0.3))
                    }
                }

                // Category indicator (optional)
                if photo.categoryId > 0 {
                    VStack {
                        Spacer()
                        HStack {
                            Spacer()
                            Circle()
                                .fill(categoryColor)
                                .frame(width: 8, height: 8)
                                .padding(4)
                        }
                    }
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            .contentShape(Rectangle()) // For tap gesture
        }
        .aspectRatio(1, contentMode: .fit)
        .onAppear {
            isVisible = true
            loadThumbnail()
        }
        .onDisappear {
            isVisible = false
            loader.cancel()
        }
        .onChange(of: photo.id) { _ in
            loadThumbnail()
        }
    }

    private var categoryColor: Color {
        // Map category to color
        switch photo.categoryId {
        case 1: return Color(hex: "#E91E63") ?? .pink
        case 2: return Color(hex: "#F44336") ?? .red
        case 3: return Color(hex: "#9C27B0") ?? .purple
        case 4: return Color(hex: "#4CAF50") ?? .green
        default: return .gray
        }
    }

    private func loadThumbnail() {
        guard isVisible else { return }

        let thumbnailSize: ThumbnailSize = sizeClass == .regular ? .medium : .small
        loader.load(photo: photo, size: thumbnailSize)
    }
}

// MARK: - Thumbnail Loader
@MainActor
class ThumbnailLoader: ObservableObject {
    @Published var image: UIImage?
    @Published var isLoading = false
    @Published var error: Error?

    private var loadTask: Task<Void, Never>?
    private let imageCache = OptimizedImageCache.shared
    private let repository = PhotoRepositoryImpl()

    deinit {
        loadTask?.cancel()
    }

    func load(photo: Photo, size: ThumbnailSize) {
        // Cancel previous load
        cancel()

        // Reset state
        error = nil

        // Start loading
        loadTask = Task {
            await loadThumbnail(photo: photo, size: size)
        }
    }

    func cancel() {
        loadTask?.cancel()
        loadTask = nil
        isLoading = false
    }

    private func loadThumbnail(photo: Photo, size: ThumbnailSize) async {
        isLoading = true
        defer { isLoading = false }

        let cacheKey = "\(photo.id)_\(size)"

        // Check cache first
        if let cached = await imageCache.image(for: cacheKey) {
            self.image = cached
            return
        }

        // Load from disk
        do {
            // Get thumbnail path
            let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            let thumbnailsDir = documentsURL.appendingPathComponent("thumbnails")

            let photoURL = URL(fileURLWithPath: photo.path)
            let thumbnailFileName = "thumb_\(photoURL.lastPathComponent)"
            let thumbnailURL = thumbnailsDir.appendingPathComponent(thumbnailFileName)

            // Load image
            if let loadedImage = await imageCache.loadImage(from: thumbnailURL, cacheKey: cacheKey) {
                // Only update if not cancelled
                if !Task.isCancelled {
                    self.image = loadedImage
                }
            } else {
                // Try to generate thumbnail if missing
                await generateThumbnailIfNeeded(photo: photo, size: size)
            }
        } catch {
            if !Task.isCancelled {
                self.error = error
            }
        }
    }

    private func generateThumbnailIfNeeded(photo: Photo, size: ThumbnailSize) async {
        // Check if original photo exists
        let photoURL = URL(fileURLWithPath: photo.path)
        guard FileManager.default.fileExists(atPath: photoURL.path) else {
            self.error = ThumbnailError.photoNotFound
            return
        }

        // Generate thumbnail
        let generator = SafeThumbnailGenerator()
        do {
            let thumbnailData = try await generator.generateThumbnail(
                from: photoURL,
                targetSize: size.pixelSize
            )

            if let thumbnailImage = UIImage(data: thumbnailData) {
                // Cache the generated thumbnail
                let cacheKey = "\(photo.id)_\(size)"
                await imageCache.store(thumbnailImage, for: cacheKey)

                if !Task.isCancelled {
                    self.image = thumbnailImage
                }
            }
        } catch {
            if !Task.isCancelled {
                self.error = error
            }
        }
    }
}

// MARK: - Errors
enum ThumbnailError: LocalizedError {
    case photoNotFound
    case thumbnailGenerationFailed

    var errorDescription: String? {
        switch self {
        case .photoNotFound:
            return "Photo file not found"
        case .thumbnailGenerationFailed:
            return "Failed to generate thumbnail"
        }
    }
}

// MARK: - Preview Support
struct PhotoThumbnailView_Previews: PreviewProvider {
    static var previews: some View {
        let samplePhoto = Photo(
            id: 1,
            path: "/sample/path.jpg",
            categoryId: 1,
            name: "Sample Photo"
        )

        PhotoThumbnailView(photo: samplePhoto)
            .frame(width: 120, height: 120)
            .previewLayout(.sizeThatFits)
    }
}