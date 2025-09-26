import SwiftUI
import UIKit
import Combine
import Photos

/// Represents a photo in the editing queue
struct PhotoEditItem: Identifiable {
    let id = UUID()
    var image: UIImage
    var originalImage: UIImage
    var rotation: CGFloat = 0
    var cropRect: CGRect?
    var categoryId: Int64
    var sourcePath: String?
    var sourceURL: URL?
}

/// Result of photo editing
struct PhotoEditResult {
    let image: UIImage
    let categoryId: Int64
    let sourcePath: String?
}

/// View model for photo editing operations with batch support
class PhotoEditViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var currentPhoto: PhotoEditItem?
    @Published var previewImage: UIImage?
    @Published var isSaving = false
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showCropOverlay = false
    @Published var selectedAspectRatio = ImageProcessor.AspectRatio.free
    @Published var categories: [Category] = []
    @Published var selectedCategory: Category?

    // Batch processing
    @Published var editQueue: [PhotoEditItem] = []
    @Published var currentIndex = 0
    @Published var isComplete = false

    // UI State
    var progressText: String {
        guard !editQueue.isEmpty else { return "" }
        return "\(currentIndex + 1) / \(editQueue.count)"
    }

    var canApplyToAll: Bool {
        guard let current = currentPhoto else { return false }
        return current.rotation != 0 && currentIndex < editQueue.count - 1
    }

    var hasMorePhotos: Bool {
        currentIndex < editQueue.count - 1
    }

    // MARK: - Private Properties
    private let photoRepository: PhotoRepository
    private let categoryRepository: CategoryRepository
    private let storageManager: StorageManager
    private let imageProcessor = ImageProcessor()
    private var cancellables = Set<AnyCancellable>()
    private var processedResults: [PhotoEditResult] = []

    init(
        photoRepository: PhotoRepository = PhotoRepositoryImpl(),
        categoryRepository: CategoryRepository = CategoryRepositoryImpl(),
        storageManager: StorageManager = .shared
    ) {
        self.photoRepository = photoRepository
        self.categoryRepository = categoryRepository
        self.storageManager = storageManager

        loadCategories()
    }

    // MARK: - Initialization

    /// Initialize editor with photos to edit
    func initializeEditor(photos: [Photo]? = nil, imageURLs: [URL]? = nil, categoryId: Int64 = 1) {
        Task { @MainActor in
            isLoading = true
            editQueue.removeAll()
            processedResults.removeAll()

            // Load from existing photos
            if let photos = photos {
                for photo in photos {
                    if let data = try? Data(contentsOf: URL(fileURLWithPath: photo.path)),
                       let image = UIImage(data: data) {
                        let normalizedImage = imageProcessor.normalizeImageOrientation(image) ?? image
                        editQueue.append(PhotoEditItem(
                            image: normalizedImage,
                            originalImage: normalizedImage,
                            categoryId: photo.categoryId,
                            sourcePath: photo.path
                        ))
                    }
                }
            }

            // Load from URLs (new imports)
            if let urls = imageURLs {
                for url in urls {
                    if let data = try? Data(contentsOf: url),
                       let image = UIImage(data: data) {
                        let normalizedImage = imageProcessor.normalizeImageOrientation(image) ?? image
                        editQueue.append(PhotoEditItem(
                            image: normalizedImage,
                            originalImage: normalizedImage,
                            categoryId: categoryId,
                            sourceURL: url
                        ))
                    }
                }
            }

            // Load first photo
            if !editQueue.isEmpty {
                currentIndex = 0
                loadCurrentPhoto()
            }

            isLoading = false
        }
    }

    // MARK: - Photo Loading

    private func loadCurrentPhoto() {
        guard currentIndex >= 0 && currentIndex < editQueue.count else { return }

        currentPhoto = editQueue[currentIndex]
        updatePreviewImage()

        // Set selected category
        if let photo = currentPhoto,
           let category = categories.first(where: { $0.id == photo.categoryId }) {
            selectedCategory = category
        }
    }

    private func updatePreviewImage() {
        guard let current = currentPhoto else { return }

        // Create memory-efficient preview
        previewImage = imageProcessor.createPreviewImage(current.image) ?? current.image
    }

    // MARK: - Editing Operations

    func rotatePhoto() {
        guard var current = currentPhoto else { return }

        // Rotate by 90 degrees
        current.rotation = (current.rotation + 90).truncatingRemainder(dividingBy: 360)

        // Apply rotation to image
        if let rotated = imageProcessor.rotateImage(current.originalImage, degrees: current.rotation) {
            current.image = rotated
            editQueue[currentIndex] = current
            currentPhoto = current
            updatePreviewImage()
        }
    }

    func updateCropRect(_ rect: CGRect) {
        guard var current = currentPhoto else { return }
        current.cropRect = rect
        editQueue[currentIndex] = current
        currentPhoto = current
    }

    func applyCrop() {
        guard var current = currentPhoto,
              let cropRect = current.cropRect else { return }

        if let cropped = imageProcessor.cropImage(current.image, to: cropRect) {
            current.image = cropped
            current.cropRect = nil // Reset crop rect after applying
            editQueue[currentIndex] = current
            currentPhoto = current
            updatePreviewImage()
        }
    }

    func applyAspectRatio(_ ratio: ImageProcessor.AspectRatio) {
        selectedAspectRatio = ratio
        guard let current = currentPhoto else { return }

        let cropRect = imageProcessor.calculateAspectRatioCrop(
            for: current.image.size,
            aspectRatio: ratio
        )
        updateCropRect(cropRect)
    }

    func updateCategory(_ category: Category) {
        selectedCategory = category
        guard var current = currentPhoto else { return }
        current.categoryId = category.id
        editQueue[currentIndex] = current
        currentPhoto = current
    }

    // MARK: - Batch Operations

    func skipCurrentPhoto() {
        if hasMorePhotos {
            currentIndex += 1
            loadCurrentPhoto()
        } else {
            finishEditing()
        }
    }

    func applyCurrentPhoto() {
        guard let current = currentPhoto else { return }

        // Save to results
        processedResults.append(PhotoEditResult(
            image: current.image,
            categoryId: current.categoryId,
            sourcePath: current.sourcePath
        ))

        if hasMorePhotos {
            currentIndex += 1
            loadCurrentPhoto()
        } else {
            finishEditing()
        }
    }

    func applyToAll() {
        guard let current = currentPhoto else { return }

        // Apply current rotation to all remaining photos
        if current.rotation != 0 {
            for i in currentIndex..<editQueue.count {
                var item = editQueue[i]
                item.rotation = current.rotation

                if let rotated = imageProcessor.rotateImage(item.originalImage, degrees: item.rotation) {
                    item.image = rotated
                    editQueue[i] = item
                }
            }
        }

        // Process all photos
        for i in currentIndex..<editQueue.count {
            let item = editQueue[i]
            processedResults.append(PhotoEditResult(
                image: item.image,
                categoryId: item.categoryId,
                sourcePath: item.sourcePath
            ))
        }

        finishEditing()
    }

    func deleteCurrentPhoto() {
        guard currentIndex < editQueue.count else { return }

        // Remove from queue
        editQueue.remove(at: currentIndex)

        if editQueue.isEmpty {
            finishEditing()
        } else if currentIndex >= editQueue.count {
            currentIndex = editQueue.count - 1
            loadCurrentPhoto()
        } else {
            loadCurrentPhoto()
        }
    }

    // MARK: - Save Operations

    func saveAllProcessedPhotos() async -> [Photo] {
        var savedPhotos: [Photo] = []

        for result in processedResults {
            let fileName = "\(UUID().uuidString).jpg"
            let savedPath = fileName // Simplified for now - actual storage manager needs update

            // Create photo record
            let photo = Photo(
                path: savedPath,
                categoryId: result.categoryId,
                fileSize: Int64(result.image.jpegData(compressionQuality: 0.9)?.count ?? 0),
                width: Int(result.image.size.width * result.image.scale),
                height: Int(result.image.size.height * result.image.scale)
            )

            // Save to repository
            do {
                let photoId = try await photoRepository.insertPhoto(photo)
                var savedPhoto = photo
                savedPhoto = Photo(
                    id: photoId,
                    path: photo.path,
                    categoryId: photo.categoryId,
                    name: photo.name,
                    isFromAssets: photo.isFromAssets,
                    createdAt: photo.createdAt,
                    fileSize: photo.fileSize,
                    width: photo.width,
                    height: photo.height,
                    isFavorite: photo.isFavorite
                )
                savedPhotos.append(savedPhoto)
            } catch {
                print("Failed to save photo to repository: \(error)")
            }
        }

        return savedPhotos
    }

    private func finishEditing() {
        isComplete = true
    }

    private func loadCategories() {
        Task { @MainActor in
            do {
                categories = try await categoryRepository.getAllCategories()
                if let firstCategory = categories.first {
                    selectedCategory = firstCategory
                }
            } catch {
                print("Failed to load categories: \(error)")
                // Use default categories as fallback
                categories = Category.getDefaultCategories()
                selectedCategory = categories.first
            }
        }
    }

    func getProcessedResults() -> [PhotoEditResult] {
        return processedResults
    }
}