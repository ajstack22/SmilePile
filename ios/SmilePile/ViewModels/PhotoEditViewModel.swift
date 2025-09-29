import SwiftUI
import UIKit
import Combine
import Photos
import ImageIO

// MARK: - Extensions
extension TimeInterval {
    func toInt64() -> Int64 {
        return Int64(self)
    }
}

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
    var sourcePhotoId: Int64? // Store the original photo ID for gallery edits
    var isProcessed: Bool = false
    var wasEdited: Bool = false
}

/// Result of photo editing
struct PhotoEditResult {
    let image: UIImage
    let categoryId: Int64
    let sourcePath: String?
    let sourceURL: URL?
    let sourcePhotoId: Int64? // Original photo ID for updates
    let wasEdited: Bool
}

/// Edit mode to distinguish between imports and gallery edits
enum EditMode {
    case importMode  // Editing photos being imported
    case gallery    // Editing existing photos from gallery
}

/// View model for photo editing operations with batch support
@MainActor
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
    @Published var applyRotationToAll = false
    @Published var batchRotation: CGFloat = 0

    // UI State
    var progressText: String {
        guard !editQueue.isEmpty else { return "" }
        return "\(currentIndex + 1) / \(editQueue.count)"
    }

    private var editMode: EditMode = .importMode

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
        storageManager: StorageManager? = nil
    ) {
        self.photoRepository = photoRepository
        self.categoryRepository = categoryRepository
        self.storageManager = storageManager ?? StorageManager.shared

        loadCategories()
    }

    // MARK: - Initialization

    /// Initialize editor with photos to edit
    func initializeEditor(photos: [Photo]? = nil, imageURLs: [URL]? = nil, categoryId: Int64 = 1) {
        Task { @MainActor in
            isLoading = true
            editQueue.removeAll()
            processedResults.removeAll()

            // Determine edit mode - gallery if we have Photo objects, import if we have URLs
            editMode = (photos != nil) ? .gallery : .importMode
            print("📝 PhotoEdit: Initialized with mode: \(editMode), photos: \(photos?.count ?? 0), urls: \(imageURLs?.count ?? 0)")

            // Load from existing photos
            if let photos = photos {
                for photo in photos {
                    if let data = try? Data(contentsOf: URL(fileURLWithPath: photo.path)),
                       let image = UIImage(data: data) {
                        // Apply EXIF rotation automatically
                        let normalizedImage = imageProcessor.normalizeImageOrientation(image) ?? image

                        // Apply any additional EXIF rotation if needed
                        let exifRotation = getExifRotation(from: data)
                        let rotatedImage = exifRotation != 0 ?
                            (imageProcessor.rotateImage(normalizedImage, degrees: CGFloat(exifRotation)) ?? normalizedImage) :
                            normalizedImage

                        editQueue.append(PhotoEditItem(
                            image: rotatedImage,
                            originalImage: rotatedImage,
                            categoryId: photo.categoryId,
                            sourcePath: photo.path,
                            sourcePhotoId: photo.id
                        ))
                    }
                }
            }

            // Load from URLs (new imports)
            if let urls = imageURLs {
                for url in urls {
                    if let data = try? Data(contentsOf: url),
                       let image = UIImage(data: data) {
                        // Apply EXIF rotation automatically
                        let normalizedImage = imageProcessor.normalizeImageOrientation(image) ?? image

                        // Apply any additional EXIF rotation if needed
                        let exifRotation = getExifRotation(from: data)
                        let rotatedImage = exifRotation != 0 ?
                            (imageProcessor.rotateImage(normalizedImage, degrees: CGFloat(exifRotation)) ?? normalizedImage) :
                            normalizedImage

                        editQueue.append(PhotoEditItem(
                            image: rotatedImage,
                            originalImage: rotatedImage,
                            categoryId: categoryId,
                            sourceURL: url
                        ))
                    }
                }
            }

            // For gallery mode with single photo, use its existing category
            if editMode == .gallery && photos?.count == 1 {
                if let photo = photos?.first {
                    selectedCategory = categories.first { $0.id == photo.categoryId }
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

    // MARK: - Cleanup

    func cancelEditing() {
        // Clean up resources before dismissing
        editQueue.removeAll()
        processedResults.removeAll()
        currentPhoto = nil
        previewImage = nil
        isComplete = false
        showCropOverlay = false
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
            // Don't clear cropRect here - let applyCurrentPhoto handle it
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
        guard var current = currentPhoto else {
            print("❌ PhotoEdit: No current photo to update category")
            return
        }
        let oldCategoryId = current.categoryId
        current.categoryId = category.id
        editQueue[currentIndex] = current
        currentPhoto = current
        print("✅ PhotoEdit: Updated category from \(oldCategoryId) to \(category.id) for photo at index \(currentIndex)")
    }

    // MARK: - Batch Operations

    func skipCurrentPhoto() {
        // Mark as processed but not edited
        if var current = currentPhoto {
            current.isProcessed = true
            current.wasEdited = false
            editQueue[currentIndex] = current

            // Still save to results (may have category change)
            processedResults.append(PhotoEditResult(
                image: current.originalImage,
                categoryId: current.categoryId,
                sourcePath: current.sourcePath,
                sourceURL: current.sourceURL,
                sourcePhotoId: current.sourcePhotoId,
                wasEdited: false
            ))
        }

        if hasMorePhotos {
            currentIndex += 1
            loadCurrentPhoto()
        } else {
            finishEditing()
        }
    }

    func applyCurrentPhoto() {
        guard var current = currentPhoto else { return }

        // Check if photo was edited before applying changes
        let wasEdited = (current.rotation != 0 || current.cropRect != nil)

        // Apply crop if there's a crop rect
        if let cropRect = current.cropRect {
            if let croppedImage = imageProcessor.cropImage(current.image, to: cropRect) {
                current.image = croppedImage
                current.cropRect = nil // Clear crop rect after applying
            }
        }

        // Mark as processed with edits
        current.isProcessed = true
        current.wasEdited = wasEdited
        editQueue[currentIndex] = current

        // Save to results
        let result = PhotoEditResult(
            image: current.image,
            categoryId: current.categoryId,
            sourcePath: current.sourcePath,
            sourceURL: current.sourceURL,
            sourcePhotoId: current.sourcePhotoId,
            wasEdited: current.wasEdited
        )
        processedResults.append(result)
        print("📝 PhotoEdit: Added to results - categoryId: \(result.categoryId), photoId: \(result.sourcePhotoId ?? 0), wasEdited: \(result.wasEdited), path: \(result.sourcePath ?? "nil")")

        // Hide crop overlay if it's showing
        showCropOverlay = false

        if hasMorePhotos {
            currentIndex += 1
            loadCurrentPhoto()
        } else {
            finishEditing()
        }
    }

    func applyToAll() {
        guard let current = currentPhoto else { return }

        // Store rotation to apply to all
        let rotationToApply = current.rotation
        applyRotationToAll = rotationToApply != 0
        batchRotation = rotationToApply

        // Apply current rotation to all remaining photos
        if rotationToApply != 0 {
            for i in currentIndex..<editQueue.count {
                var item = editQueue[i]
                item.rotation = rotationToApply

                if let rotated = imageProcessor.rotateImage(item.originalImage, degrees: item.rotation) {
                    item.image = rotated
                    item.wasEdited = true
                }
                item.isProcessed = true
                editQueue[i] = item
            }
        }

        // Process all photos
        for i in currentIndex..<editQueue.count {
            let item = editQueue[i]
            processedResults.append(PhotoEditResult(
                image: item.image,
                categoryId: item.categoryId,
                sourcePath: item.sourcePath,
                sourceURL: item.sourceURL,
                sourcePhotoId: item.sourcePhotoId,
                wasEdited: item.wasEdited
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
            // Check if we're editing an existing photo or importing a new one
            if let photoId = result.sourcePhotoId, editMode == .gallery {
                // Save edited image back to the same path if image was edited
                if result.wasEdited, let sourcePath = result.sourcePath {
                    if let imageData = result.image.jpegData(compressionQuality: 0.9) {
                        do {
                            try imageData.write(to: URL(fileURLWithPath: sourcePath))
                        } catch {
                            print("Failed to overwrite photo: \(error)")
                        }
                    }
                }

                // Always update photo in database (category might have changed even if image wasn't edited)
                do {
                    if let existingPhoto = try await photoRepository.getPhotoById(photoId) {
                        print("📝 PhotoEdit: Found existing photo ID \(photoId) with categoryId: \(existingPhoto.categoryId), updating to: \(result.categoryId)")

                        // Create a new photo with updated values (since Photo uses let)
                        let updatedPhoto = Photo(
                            id: existingPhoto.id,
                            path: existingPhoto.path,
                            categoryId: result.categoryId,  // Always use the new categoryId
                            name: existingPhoto.name,
                            isFromAssets: existingPhoto.isFromAssets,
                            createdAt: result.wasEdited ? Date().timeIntervalSince1970.toInt64() : existingPhoto.createdAt,
                            fileSize: result.wasEdited ? Int64(result.image.jpegData(compressionQuality: 0.9)?.count ?? 0) : existingPhoto.fileSize,
                            width: result.wasEdited ? Int(result.image.size.width * result.image.scale) : existingPhoto.width,
                            height: result.wasEdited ? Int(result.image.size.height * result.image.scale) : existingPhoto.height
                        )
                        try await photoRepository.updatePhoto(updatedPhoto)
                        savedPhotos.append(updatedPhoto)
                        print("✅ PhotoEdit: Successfully updated photo in database with categoryId: \(updatedPhoto.categoryId)")
                    } else {
                        print("❌ PhotoEdit: Could not find photo with ID \(photoId)")
                    }
                } catch {
                    print("❌ PhotoEdit: Failed to update photo in repository: \(error)")
                }
            } else {
                // New import - create a new file
                let fileName = "edited_\(Date().timeIntervalSince1970)_\(savedPhotos.count).jpg"
                let documentsDirectory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let photosDirectory = documentsDirectory.appendingPathComponent("photos")
                try? FileManager.default.createDirectory(at: photosDirectory, withIntermediateDirectories: true)
                let savedPath = photosDirectory.appendingPathComponent(fileName).path

                // Save to internal storage
                if let imageData = result.image.jpegData(compressionQuality: 0.9) {
                    do {
                        try imageData.write(to: URL(fileURLWithPath: savedPath))

                        // Create photo record
                        let photo = Photo(
                            path: savedPath,
                            categoryId: result.categoryId,
                            fileSize: Int64(imageData.count),
                            width: Int(result.image.size.width * result.image.scale),
                            height: Int(result.image.size.height * result.image.scale)
                        )

                        // Save to repository
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
                            height: photo.height
                        )
                        savedPhotos.append(savedPhoto)
                    } catch {
                        print("Failed to save new photo: \(error)")
                    }
                }
            }
        }

        return savedPhotos
    }

    private func finishEditing() {
        print("🏁 PhotoEdit: Finishing editing with \(processedResults.count) processed photos")
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

    // MARK: - EXIF Helpers

    private func getExifRotation(from imageData: Data) -> Int {
        guard let source = CGImageSourceCreateWithData(imageData as CFData, nil),
              let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [String: Any],
              let orientation = properties[kCGImagePropertyOrientation as String] as? Int else {
            return 0
        }

        // Convert EXIF orientation to rotation degrees
        switch orientation {
        case 3: return 180  // Upside down
        case 6: return 90   // 90 CW
        case 8: return 270  // 90 CCW
        default: return 0
        }
    }
}