import Foundation
import SwiftUI
import Combine
import os.log

/// Optimized view model for photo gallery with virtual scrolling and smart preloading
@MainActor
class PhotoGalleryViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var photos: [Photo] = []
    @Published var visiblePhotos: [Photo] = []
    @Published var isLoading = false
    @Published var loadingProgress: Double = 0
    @Published var currentScrollOffset: CGFloat = 0
    @Published var selectedCategory: Category?
    @Published var errorMessage: String?
    @Published var memoryUsageMB: Int = 0

    // MARK: - Configuration
    struct Configuration {
        static let preloadRowCount = 3
        static let itemsPerRow = 3
        static let itemsPerRowIPad = 5
        static let virtualScrollBuffer = 20 // Number of items above/below visible area
        static let memoryWarningThreshold = 80 // MB
        static let scrollDebounceDelay: TimeInterval = 0.1
    }

    // MARK: - Private Properties
    private let repository: PhotoRepository
    private let imageCache: OptimizedImageCache
    private let storageManager: StorageManager
    private var loadingTasks: [String: Task<UIImage?, Never>] = [:]
    private var preloadingTasks: Set<String> = []
    private let logger = Logger(subsystem: "com.smilepile", category: "PhotoGalleryViewModel")

    // Performance tracking
    private var lastScrollTime = Date()
    private var scrollVelocity: CGFloat = 0
    private var isRapidScrolling = false

    // Virtual scrolling
    private var visibleRange: Range<Int> = 0..<0
    private var preloadRange: Range<Int> = 0..<0

    // Memory management
    private var memoryTimer: Timer?
    private let storage = SimplePhotoStorage.shared

    // Cancellables
    private var cancellables = Set<AnyCancellable>()

    // MARK: - Computed Properties
    var filteredPhotos: [Photo] {
        let filtered: [Photo]
        if let selectedCategory = selectedCategory {
            filtered = photos.filter { $0.categoryId == selectedCategory.id }
        } else {
            filtered = photos
        }
        logger.debug("Filtering photos: Total=\(self.photos.count), Category=\(self.selectedCategory?.displayName ?? "None"), Filtered=\(filtered.count)")

        // Additional debug logging for first few photos
        if !filtered.isEmpty {
            logger.debug("First filtered photo: path=\(filtered[0].path), categoryId=\(filtered[0].categoryId)")
        }

        return filtered
    }

    var itemsPerRow: Int {
        return UIDevice.current.userInterfaceIdiom == .pad
            ? Configuration.itemsPerRowIPad
            : Configuration.itemsPerRow
    }

    var cacheStatistics: CacheStatistics? {
        get async {
            await imageCache.getCacheStats()
        }
    }

    // MARK: - Initialization
    init(repository: PhotoRepository? = nil,
         imageCache: OptimizedImageCache? = nil,
         storageManager: StorageManager? = nil) {
        self.repository = repository ?? PhotoRepositoryImpl()
        self.imageCache = imageCache ?? .shared
        self.storageManager = storageManager ?? StorageManager.shared

        setupMemoryMonitoring()
    }

    deinit {
        memoryTimer?.invalidate()
        loadingTasks.values.forEach { $0.cancel() }
    }

    // MARK: - Public Methods

    /// Load all photos from repository
    func loadPhotos() async {
        isLoading = true
        loadingProgress = 0
        errorMessage = nil
        defer { isLoading = false }

        do {
            // Clean up orphaned photos first
            let deletedCount = try await repository.cleanupOrphanedPhotos()
            if deletedCount > 0 {
                logger.info("Cleaned up \(deletedCount) orphaned photo records")
            }

            // Start loading
            logger.info("Loading photos from repository")
            let startTime = Date()

            self.photos = try await repository.getAllPhotos()

            let loadTime = Date().timeIntervalSince(startTime)
            logger.info("Loaded \(self.photos.count) photos in \(String(format: "%.2f", loadTime))s")

            // Debug: log first few photos
            for (index, photo) in self.photos.prefix(3).enumerated() {
                logger.debug("Photo \(index + 1): path=\(photo.path), categoryId=\(photo.categoryId), exists=\(FileManager.default.fileExists(atPath: photo.path))")
            }

            // Set initial visible range
            updateVisibleRange(scrollOffset: 0, containerHeight: UIScreen.main.bounds.height)

            // Preload initial thumbnails
            await preloadInitialThumbnails()

        } catch {
            logger.error("Failed to load photos: \(error.localizedDescription)")
            errorMessage = "Failed to load photos"
        }
    }

    /// Refresh photos from repository
    func refreshPhotos() async {
        await loadPhotos()
    }

    /// Handle scroll position changes for virtual scrolling
    func handleScroll(offset: CGFloat, containerHeight: CGFloat) {
        currentScrollOffset = offset

        // Calculate scroll velocity
        let currentTime = Date()
        let timeDelta = currentTime.timeIntervalSince(lastScrollTime)
        if timeDelta > 0 {
            scrollVelocity = abs(offset - currentScrollOffset) / CGFloat(timeDelta)
            isRapidScrolling = scrollVelocity > 1000
        }
        lastScrollTime = currentTime

        // Update visible range
        updateVisibleRange(scrollOffset: offset, containerHeight: containerHeight)

        // Preload adjacent items if not rapidly scrolling
        if !isRapidScrolling {
            Task {
                await preloadAdjacentThumbnails()
            }
        }
    }

    /// Load thumbnail for specific photo
    func loadThumbnailIfNeeded(for photo: Photo) {
        let cacheKey = thumbnailCacheKey(for: photo, size: .small)

        // Skip if already cached or loading
        Task {
            if await imageCache.hasImage(for: cacheKey) { return }
        }
        if loadingTasks[cacheKey] != nil { return }

        // Start loading task
        let task = Task {
            return await loadThumbnail(for: photo, size: .small)
        }
        loadingTasks[cacheKey] = task
    }

    /// Cancel thumbnail loading for specific photo
    func cancelThumbnailLoad(for photo: Photo) {
        let cacheKey = thumbnailCacheKey(for: photo, size: .small)
        loadingTasks[cacheKey]?.cancel()
        loadingTasks.removeValue(forKey: cacheKey)

        Task {
            await imageCache.cancelLoading(for: cacheKey)
        }
    }

    /// Get thumbnail image for photo
    func getThumbnail(for photo: Photo, size: ThumbnailSize = .small) async -> UIImage? {
        let cacheKey = thumbnailCacheKey(for: photo, size: size)

        // Check cache first
        if let cached = await imageCache.image(for: cacheKey) {
            return cached
        }

        // Load from disk
        return await loadThumbnail(for: photo, size: size)
    }

    /// Clear image cache
    func clearCache() async {
        await imageCache.clearCache()
        loadingTasks.values.forEach { $0.cancel() }
        loadingTasks.removeAll()
        preloadingTasks.removeAll()
    }

    // MARK: - Private Methods

    private func updateVisibleRange(scrollOffset: CGFloat, containerHeight: CGFloat) {
        let itemHeight: CGFloat = UIScreen.main.bounds.width / CGFloat(itemsPerRow)
        let rowHeight = itemHeight + 2 // Including spacing

        let firstVisibleRow = max(0, Int(scrollOffset / rowHeight))
        let visibleRows = Int(ceil(containerHeight / rowHeight)) + 1
        let lastVisibleRow = min(
            (filteredPhotos.count - 1) / itemsPerRow,
            firstVisibleRow + visibleRows
        )

        let firstIndex = firstVisibleRow * itemsPerRow
        let lastIndex = min(filteredPhotos.count - 1, (lastVisibleRow + 1) * itemsPerRow - 1)

        visibleRange = firstIndex..<(lastIndex + 1)

        // Calculate preload range
        let preloadFirst = max(0, firstIndex - Configuration.virtualScrollBuffer)
        let preloadLast = min(filteredPhotos.count - 1, lastIndex + Configuration.virtualScrollBuffer)
        preloadRange = preloadFirst..<(preloadLast + 1)

        // Update visible photos for virtual scrolling
        visiblePhotos = Array(filteredPhotos[visibleRange])

        logger.debug("Visible range: \(self.visibleRange), Preload range: \(self.preloadRange)")
    }

    private func preloadInitialThumbnails() async {
        let initialCount = min(30, filteredPhotos.count)
        let initialPhotos = Array(filteredPhotos.prefix(initialCount))

        await withTaskGroup(of: Void.self) { group in
            for photo in initialPhotos {
                group.addTask {
                    _ = await self.loadThumbnail(for: photo, size: .small)
                }
            }
        }
    }

    private func preloadAdjacentThumbnails() async {
        guard !preloadRange.isEmpty else { return }

        let photosToPreload = Array(filteredPhotos[preloadRange])
        let photoIds = photosToPreload.map { thumbnailCacheKey(for: $0, size: .small) }

        // Filter out already loading items
        let newPhotoIds = photoIds.filter { !preloadingTasks.contains($0) }

        guard !newPhotoIds.isEmpty else { return }

        // Mark as loading
        newPhotoIds.forEach { preloadingTasks.insert($0) }

        await imageCache.prefetchImages(for: newPhotoIds) { [weak self] photoId in
            guard let self = self else { return nil }

            // Find photo by cache key
            if let photo = photosToPreload.first(where: {
                self.thumbnailCacheKey(for: $0, size: .small) == photoId
            }) {
                return await self.thumbnailURL(for: photo, size: .small)
            }
            return nil
        }

        // Remove from loading set
        newPhotoIds.forEach { preloadingTasks.remove($0) }
    }

    private func loadThumbnail(for photo: Photo, size: ThumbnailSize) async -> UIImage? {
        let cacheKey = thumbnailCacheKey(for: photo, size: size)

        // Check cache again (might have been loaded by another task)
        if let cached = await imageCache.image(for: cacheKey) {
            return cached
        }

        // Get thumbnail URL
        guard let url = await thumbnailURL(for: photo, size: size) else {
            logger.error("No thumbnail URL for photo: \(photo.id)")
            return nil
        }

        // Load from disk
        return await imageCache.loadImage(from: url, cacheKey: cacheKey)
    }

    private func thumbnailURL(for photo: Photo, size: ThumbnailSize) async -> URL? {
        // Use SimplePhotoStorage to get the correct thumbnail URL
        return storage.thumbnailURL(for: photo)
    }

    private func thumbnailCacheKey(for photo: Photo, size: ThumbnailSize) -> String {
        return "\(photo.id)_\(size)"
    }

    // MARK: - Memory Management

    private func setupMemoryMonitoring() {
        // Monitor memory every second
        memoryTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            Task { @MainActor in
                await self?.updateMemoryUsage()
            }
        }

        // Listen for memory warnings
        NotificationCenter.default.publisher(for: UIApplication.didReceiveMemoryWarningNotification)
            .sink { [weak self] _ in
                Task { @MainActor in
                    await self?.handleMemoryWarning()
                }
            }
            .store(in: &cancellables)
    }

    private func updateMemoryUsage() async {
        // Get memory usage (simplified since we're not using thumbnailGenerator)
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size) / 4

        let result = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: Int(count)) {
                task_info(mach_task_self_, task_flavor_t(MACH_TASK_BASIC_INFO), $0, &count)
            }
        }

        if result == KERN_SUCCESS {
            memoryUsageMB = Int(info.resident_size / 1024 / 1024)
        }

        // Warn if memory usage is high
        if memoryUsageMB > Configuration.memoryWarningThreshold {
            logger.warning("High memory usage: \(self.memoryUsageMB)MB")
            await reduceMemoryPressure()
        }
    }

    private func handleMemoryWarning() async {
        logger.warning("Received memory warning")

        // Clear cache for non-visible items
        await clearNonVisibleCache()

        // Cancel preloading tasks
        preloadingTasks.removeAll()

        // Force garbage collection by clearing some data
        if !isRapidScrolling {
            await imageCache.clearCache()
        }
    }

    private func reduceMemoryPressure() async {
        // Cancel non-essential tasks
        for (key, task) in loadingTasks {
            if !visiblePhotos.contains(where: { thumbnailCacheKey(for: $0, size: .small) == key }) {
                task.cancel()
                loadingTasks.removeValue(forKey: key)
            }
        }

        // Clear cache for items outside visible range
        await clearNonVisibleCache()
    }

    private func clearNonVisibleCache() async {
        _ = visiblePhotos.map { thumbnailCacheKey(for: $0, size: .small) }

        // This would require extending ImageCache to support selective clearing
        // For now, we'll rely on the cache's own eviction policy
        logger.debug("Clearing cache for non-visible items")
    }

    // MARK: - Performance Metrics

    func getPerformanceMetrics() async -> PerformanceMetrics {
        let cacheStats = await imageCache.getCacheStats()

        return PerformanceMetrics(
            photoCount: photos.count,
            visibleCount: visiblePhotos.count,
            cacheHitRate: cacheStats.hitRate,
            memoryUsageMB: memoryUsageMB,
            isRapidScrolling: isRapidScrolling,
            scrollVelocity: scrollVelocity
        )
    }
}

// MARK: - Performance Metrics
struct PerformanceMetrics {
    let photoCount: Int
    let visibleCount: Int
    let cacheHitRate: Double
    let memoryUsageMB: Int
    let isRapidScrolling: Bool
    let scrollVelocity: CGFloat

    var description: String {
        """
        Photos: \(photoCount) (visible: \(visibleCount))
        Cache Hit Rate: \(String(format: "%.1f%%", cacheHitRate * 100))
        Memory: \(memoryUsageMB)MB
        Scrolling: \(isRapidScrolling ? "Fast" : "Normal") (\(Int(scrollVelocity)) pts/s)
        """
    }
}