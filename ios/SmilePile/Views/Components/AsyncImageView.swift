import SwiftUI
import UIKit
import os.log

// MARK: - Image Cache
class ImageCache {
    static let shared = ImageCache()
    private let cache = NSCache<NSString, UIImage>()
    private let logger = Logger(subsystem: "com.smilepile", category: "ImageCache")

    private init() {
        cache.countLimit = 100 // Maximum number of images
        cache.totalCostLimit = 100 * 1024 * 1024 // 100MB
    }

    func image(for path: String) -> UIImage? {
        return cache.object(forKey: path as NSString)
    }

    func setImage(_ image: UIImage, for path: String) {
        let cost = Int(image.size.width * image.size.height * 4) // Approximate memory cost
        cache.setObject(image, forKey: path as NSString, cost: cost)
    }

    func removeImage(for path: String) {
        cache.removeObject(forKey: path as NSString)
    }

    func clearCache() {
        cache.removeAllObjects()
    }
}

// MARK: - Async Image View
struct AsyncImageView: View {
    let photo: Photo
    let contentMode: ContentMode
    let showPlaceholder: Bool

    @State private var image: UIImage?
    @State private var isLoading = false
    @State private var loadFailed = false

    private let storageManager = StorageManager.shared
    private let imageProcessor = ImageProcessor()
    private let logger = Logger(subsystem: "com.smilepile", category: "AsyncImageView")

    init(
        photo: Photo,
        contentMode: ContentMode = .fit,
        showPlaceholder: Bool = true
    ) {
        self.photo = photo
        self.contentMode = contentMode
        self.showPlaceholder = showPlaceholder
    }

    var body: some View {
        ZStack {
            if let image = image {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: contentMode)
                    .clipped()
            } else if isLoading {
                loadingView
            } else if loadFailed {
                errorView
            } else if showPlaceholder {
                placeholderView
            }
        }
        .onAppear {
            loadImage()
        }
        .onDisappear {
            // Cancel any ongoing loading if needed
        }
        .onChange(of: photo.path) { _ in
            // Reload if photo path changes
            image = nil
            loadFailed = false
            loadImage()
        }
    }

    private var loadingView: some View {
        ZStack {
            Color.gray.opacity(0.1)
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
                .scaleEffect(0.8)
        }
    }

    private var placeholderView: some View {
        ZStack {
            Color.gray.opacity(0.1)
            Image(systemName: "photo")
                .font(.largeTitle)
                .foregroundColor(.gray.opacity(0.3))
        }
    }

    private var errorView: some View {
        ZStack {
            Color.gray.opacity(0.1)
            VStack(spacing: 8) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.title2)
                    .foregroundColor(.gray.opacity(0.4))
                Text("Failed to load")
                    .font(.caption2)
                    .foregroundColor(.gray.opacity(0.4))
            }
        }
    }

    private func loadImage() {
        // Check if already loading
        guard !isLoading else { return }

        // Check cache first
        if let cachedImage = ImageCache.shared.image(for: photo.path) {
            self.image = cachedImage
            return
        }

        isLoading = true
        loadFailed = false

        Task {
            await loadImageAsync()
        }
    }

    @MainActor
    private func loadImageAsync() async {
        defer { isLoading = false }

        // Try to load the image
        if let loadedImage = await loadImageFromPath(photo.path) {
            self.image = loadedImage
            // Cache the loaded image
            ImageCache.shared.setImage(loadedImage, for: photo.path)
        } else {
            loadFailed = true
            logger.error("Failed to load image from path: \(photo.path)")
        }
    }

    private func loadImageFromPath(_ path: String) async -> UIImage? {
        // Handle different path types
        let url: URL

        if path.hasPrefix("file://") || path.hasPrefix("/") {
            // Local file path
            url = URL(fileURLWithPath: path.replacingOccurrences(of: "file://", with: ""))
        } else if path.hasPrefix("http://") || path.hasPrefix("https://") {
            // Remote URL (not typically used in SmilePile but handle it)
            guard let remoteURL = URL(string: path) else { return nil }
            return await loadRemoteImage(from: remoteURL)
        } else {
            // Assume it's a relative path or filename in documents directory
            let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            url = documentsPath.appendingPathComponent(path)
        }

        // Check if file exists
        guard FileManager.default.fileExists(atPath: url.path) else {
            logger.warning("Image file does not exist at path: \(url.path)")
            return nil
        }

        // Load image data
        do {
            let data = try Data(contentsOf: url)
            guard let image = UIImage(data: data) else {
                logger.error("Failed to create UIImage from data at: \(url.path)")
                return nil
            }
            return image
        } catch {
            logger.error("Failed to load image data from \(url.path): \(error.localizedDescription)")
            return nil
        }
    }

    private func loadRemoteImage(from url: URL) async -> UIImage? {
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            return UIImage(data: data)
        } catch {
            logger.error("Failed to load remote image from \(url): \(error.localizedDescription)")
            return nil
        }
    }
}

// MARK: - Thumbnail Image View
struct ThumbnailImageView: View {
    let photo: Photo
    let size: CGFloat

    @State private var thumbnailImage: UIImage?
    @State private var isLoading = false

    private let storageManager = StorageManager.shared
    private let logger = Logger(subsystem: "com.smilepile", category: "ThumbnailImageView")

    var body: some View {
        ZStack {
            if let image = thumbnailImage {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: size, height: size)
                    .clipped()
            } else {
                // Use regular AsyncImageView as fallback
                AsyncImageView(photo: photo, contentMode: .fill)
                    .frame(width: size, height: size)
                    .clipped()
            }
        }
        .onAppear {
            loadThumbnail()
        }
    }

    private func loadThumbnail() {
        guard !isLoading else { return }

        // Check if we have a thumbnail path
        if let thumbnailPath = storageManager.getThumbnailPath(for: photo.path) {
            isLoading = true

            Task {
                await loadThumbnailImage(from: thumbnailPath)
            }
        }
    }

    @MainActor
    private func loadThumbnailImage(from path: String) async {
        defer { isLoading = false }

        let url = URL(fileURLWithPath: path)

        guard FileManager.default.fileExists(atPath: url.path) else {
            logger.warning("Thumbnail does not exist at: \(url.path)")
            return
        }

        do {
            let data = try Data(contentsOf: url)
            thumbnailImage = UIImage(data: data)
        } catch {
            logger.error("Failed to load thumbnail: \(error.localizedDescription)")
        }
    }
}

// MARK: - Photo Card View with Real Images
struct PhotoCardWithImage: View {
    let photo: Photo
    let onTap: () -> Void

    var body: some View {
        AsyncImageView(photo: photo, contentMode: .fill)
            .aspectRatio(4/3, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Color.gray.opacity(0.2), lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
            .contentShape(Rectangle())
            .onTapGesture(perform: onTap)
    }
}

// MARK: - Photo Grid Thumbnail with Real Images
struct PhotoGridThumbnail: View {
    let photo: Photo
    let size: CGFloat

    var body: some View {
        ThumbnailImageView(photo: photo, size: size)
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: 4))
            .overlay(
                RoundedRectangle(cornerRadius: 4)
                    .stroke(Color.gray.opacity(0.1), lineWidth: 0.5)
            )
    }
}