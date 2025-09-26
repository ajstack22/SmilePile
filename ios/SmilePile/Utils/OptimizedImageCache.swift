import Foundation
import UIKit
import os.log

/// Thread-safe image cache using Swift actors for concurrent access
/// Provides memory-efficient caching with automatic cleanup
actor OptimizedImageCache {
    static let shared = OptimizedImageCache()

    // MARK: - Configuration
    struct Configuration {
        static let maxCacheSize: Int = 100_000_000 // 100MB
        static let maxItemCount: Int = 200
        static let defaultThumbnailCost: Int = 50_000 // 50KB
        static let preloadBatchSize: Int = 10
        static let cacheExpirationSeconds: TimeInterval = 3600 // 1 hour
    }

    // MARK: - Cache Entry
    private struct CacheEntry {
        let image: UIImage
        let cost: Int
        let timestamp: Date
        let accessCount: Int

        var isExpired: Bool {
            Date().timeIntervalSince(timestamp) > Configuration.cacheExpirationSeconds
        }
    }

    // MARK: - Properties
    private let cache = NSCache<NSString, UIImage>()
    private var cacheEntries: [String: CacheEntry] = [:]
    private var loadingTasks: [String: Task<UIImage?, Never>] = [:]
    private let logger = Logger(subsystem: "com.smilepile", category: "ImageCache")
    private var memoryWarningObserver: NSObjectProtocol?
    private var currentMemoryUsage: Int = 0

    // Statistics
    private var cacheHits: Int = 0
    private var cacheMisses: Int = 0

    // MARK: - Initialization
    init() {
        setupCache()
        observeMemoryWarnings()
    }

    private func setupCache() {
        cache.countLimit = Configuration.maxItemCount
        cache.totalCostLimit = Configuration.maxCacheSize
        cache.evictsObjectsWithDiscardedContent = true

        // Set delegate for eviction callbacks
        cache.delegate = CacheDelegate { [weak self] key in
            Task {
                await self?.handleEviction(key: key)
            }
        }
    }

    private func observeMemoryWarnings() {
        memoryWarningObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.didReceiveMemoryWarningNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task {
                await self?.handleMemoryWarning()
            }
        }
    }

    // MARK: - Cache Operations

    /// Get image from cache
    func image(for key: String) -> UIImage? {
        // Check if entry exists and is not expired
        if let entry = cacheEntries[key], !entry.isExpired {
            cacheHits += 1

            // Update access count
            cacheEntries[key] = CacheEntry(
                image: entry.image,
                cost: entry.cost,
                timestamp: entry.timestamp,
                accessCount: entry.accessCount + 1
            )

            return cache.object(forKey: key as NSString)
        }

        // Remove expired entry
        if cacheEntries[key] != nil {
            removeImage(for: key)
        }

        cacheMisses += 1
        return nil
    }

    /// Store image in cache
    func store(_ image: UIImage, for key: String, cost: Int? = nil) {
        let imageCost = cost ?? calculateImageCost(image)

        // Check if cache would exceed limits
        if currentMemoryUsage + imageCost > Configuration.maxCacheSize {
            evictLeastRecentlyUsed()
        }

        // Store in cache
        cache.setObject(image, forKey: key as NSString, cost: imageCost)

        // Store metadata
        cacheEntries[key] = CacheEntry(
            image: image,
            cost: imageCost,
            timestamp: Date(),
            accessCount: 0
        )

        currentMemoryUsage += imageCost

        logger.debug("Cached image: \(key) (cost: \(imageCost) bytes)")
    }

    /// Remove image from cache
    func removeImage(for key: String) {
        if let entry = cacheEntries[key] {
            currentMemoryUsage -= entry.cost
            cacheEntries.removeValue(forKey: key)
            cache.removeObject(forKey: key as NSString)
        }
    }

    /// Clear entire cache
    func clearCache() {
        cache.removeAllObjects()
        cacheEntries.removeAll()
        loadingTasks.forEach { $0.value.cancel() }
        loadingTasks.removeAll()
        currentMemoryUsage = 0

        logger.info("Cache cleared")
    }

    // MARK: - Async Loading

    /// Load image from URL with caching
    func loadImage(from url: URL, cacheKey: String? = nil) async -> UIImage? {
        let key = cacheKey ?? url.absoluteString

        // Check cache first
        if let cachedImage = image(for: key) {
            return cachedImage
        }

        // Check if already loading
        if let existingTask = loadingTasks[key] {
            return await existingTask.value
        }

        // Create new loading task
        let task = Task { () -> UIImage? in
            do {
                let data = try Data(contentsOf: url)
                if let image = UIImage(data: data) {
                    await self.store(image, for: key)
                    return image
                }
            } catch {
                logger.error("Failed to load image from \(url): \(error.localizedDescription)")
            }
            return nil
        }

        loadingTasks[key] = task
        let image = await task.value
        loadingTasks.removeValue(forKey: key)

        return image
    }

    /// Preload multiple images
    func preloadImages(urls: [URL]) async {
        await withTaskGroup(of: Void.self) { group in
            for url in urls.prefix(Configuration.preloadBatchSize) {
                group.addTask {
                    _ = await self.loadImage(from: url)
                }
            }
        }
    }

    /// Cancel loading for a specific key
    func cancelLoading(for key: String) {
        loadingTasks[key]?.cancel()
        loadingTasks.removeValue(forKey: key)
    }

    // MARK: - Memory Management

    private func handleMemoryWarning() {
        logger.warning("Memory warning received - reducing cache")

        // Remove least recently used entries
        let entriesToRemove = Int(Double(cacheEntries.count) * 0.5) // Remove 50%
        evictLeastRecentlyUsed(count: entriesToRemove)
    }

    private func handleEviction(key: String) {
        if let entry = cacheEntries[key] {
            currentMemoryUsage -= entry.cost
            cacheEntries.removeValue(forKey: key)
        }
    }

    private func evictLeastRecentlyUsed(count: Int = 1) {
        let sortedEntries = cacheEntries.sorted { entry1, entry2 in
            // Sort by access count and timestamp
            if entry1.value.accessCount == entry2.value.accessCount {
                return entry1.value.timestamp < entry2.value.timestamp
            }
            return entry1.value.accessCount < entry2.value.accessCount
        }

        for (key, _) in sortedEntries.prefix(count) {
            removeImage(for: key)
        }
    }

    // MARK: - Utility

    private func calculateImageCost(_ image: UIImage) -> Int {
        let pixelCount = Int(image.size.width * image.scale * image.size.height * image.scale)
        return pixelCount * 4 // 4 bytes per pixel (RGBA)
    }

    /// Get cache statistics
    func getCacheStats() -> CacheStatistics {
        return CacheStatistics(
            itemCount: cacheEntries.count,
            totalSize: currentMemoryUsage,
            hitRate: cacheHits > 0 ? Double(cacheHits) / Double(cacheHits + cacheMisses) : 0,
            hits: cacheHits,
            misses: cacheMisses
        )
    }

    /// Check if cache has image for key
    func hasImage(for key: String) -> Bool {
        return image(for: key) != nil
    }

    /// Get current memory usage
    func getMemoryUsage() -> Int {
        return currentMemoryUsage
    }

    /// Prefetch images for visible and adjacent rows
    func prefetchImages(for photoIds: [String], urlProvider: @escaping (String) async -> URL?) async {
        await withTaskGroup(of: Void.self) { group in
            for photoId in photoIds.prefix(Configuration.preloadBatchSize) {
                // Skip if already cached
                if hasImage(for: photoId) { continue }

                group.addTask {
                    if let url = await urlProvider(photoId) {
                        _ = await self.loadImage(from: url, cacheKey: photoId)
                    }
                }
            }
        }
    }
}

// MARK: - Cache Delegate
private class CacheDelegate: NSObject, NSCacheDelegate {
    private let evictionHandler: (String) -> Void

    init(evictionHandler: @escaping (String) -> Void) {
        self.evictionHandler = evictionHandler
    }

    func cache(_ cache: NSCache<AnyObject, AnyObject>, willEvictObject obj: Any) {
        // Note: We can't directly get the key here, but we handle it in the actor
    }
}

// MARK: - Cache Statistics
struct CacheStatistics {
    let itemCount: Int
    let totalSize: Int
    let hitRate: Double
    let hits: Int
    let misses: Int

    var formattedSize: String {
        let formatter = ByteCountFormatter()
        formatter.countStyle = .binary
        return formatter.string(fromByteCount: Int64(totalSize))
    }

    var hitRatePercentage: String {
        return String(format: "%.1f%%", hitRate * 100)
    }
}

// MARK: - Thumbnail Size
enum ThumbnailSize {
    case small  // Grid view - 100x100
    case medium // List view - 200x200
    case large  // Preview - 400x400

    var pixelSize: CGFloat {
        switch self {
        case .small:
            return 100
        case .medium:
            return 200
        case .large:
            return 400
        }
    }

    var estimatedCost: Int {
        let pixels = Int(pixelSize * pixelSize)
        return pixels * 4 // 4 bytes per pixel
    }
}