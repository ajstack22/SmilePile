import SwiftUI
import Photos

// MARK: - Onboarding Navigation Steps
enum OnboardingStep: Int, CaseIterable {
    case welcome = 0
    case categories = 1
    case photoImport = 2
    case pinSetup = 3
    case complete = 4

    var title: String {
        switch self {
        case .welcome:
            return "Welcome"
        case .categories:
            return "Create Piles"
        case .photoImport:
            return "Add Photos"
        case .pinSetup:
            return "PIN Setup"
        case .complete:
            return "All Set!"
        }
    }

    var canSkip: Bool {
        switch self {
        case .photoImport, .pinSetup:
            return true
        default:
            return false
        }
    }
}

// MARK: - Temporary Onboarding State
struct OnboardingData {
    var categories: [TempCategory] = []
    var importedPhotos: [ImportedPhoto] = []
    var pinCode: String? = nil
    var skipPIN: Bool = false
}

struct TempCategory {
    let id = UUID()
    var name: String
    var colorHex: String
    var icon: String?
}

struct ImportedPhoto {
    let id: String // PHAsset identifier
    var categoryId: UUID?
    var assetIdentifier: String
}

// MARK: - Onboarding Coordinator
class OnboardingCoordinator: ObservableObject {
    @Published var currentStep: OnboardingStep = .welcome
    @Published var navigationHistory: [OnboardingStep] = []
    @Published var onboardingData = OnboardingData()
    @Published var isComplete = false
    @Published var showError = false
    @Published var errorMessage = ""

    // Progress tracking removed - iOS doesn't have progress bar per Android spec

    // Navigation
    func navigateToNext() {
        // Validate current step before proceeding
        guard validateCurrentStep() else { return }

        navigationHistory.append(currentStep)

        switch currentStep {
        case .welcome:
            currentStep = .categories
        case .categories:
            currentStep = .pinSetup  // Skip directly to PIN (photoImport removed from flow)
        case .photoImport:
            currentStep = .pinSetup  // Keep for compatibility but not used in flow
        case .pinSetup:
            completeOnboarding()
        case .complete:
            break
        }
    }

    func navigateBack() {
        guard !navigationHistory.isEmpty else { return }
        currentStep = navigationHistory.removeLast()
    }

    func skip() {
        guard currentStep.canSkip else { return }

        switch currentStep {
        case .photoImport:
            // PhotoImport no longer in flow, but keep for compatibility
            navigationHistory.append(currentStep)
            currentStep = .pinSetup
        case .pinSetup:
            onboardingData.skipPIN = true
            completeOnboarding()
        default:
            break
        }
    }

    // Validation
    private func validateCurrentStep() -> Bool {
        switch currentStep {
        case .categories:
            // Must have at least one category
            if onboardingData.categories.isEmpty {
                showError(message: "Please create at least one category")
                return false
            }
            return true
        case .photoImport:
            // Photos are optional, always valid
            return true
        case .pinSetup:
            // If user chose to set PIN, validate it's entered
            if !onboardingData.skipPIN && (onboardingData.pinCode?.isEmpty ?? true) {
                showError(message: "Please enter a PIN or skip this step")
                return false
            }
            return true
        default:
            return true
        }
    }

    // Completion
    private func completeOnboarding() {
        Task { @MainActor in
            do {
                // Save categories
                let categoryRepo = CategoryRepositoryImpl.shared
                for (index, tempCategory) in onboardingData.categories.enumerated() {
                    let category = Category(
                        id: Int64(index + 1),
                        name: tempCategory.name.lowercased().replacingOccurrences(of: " ", with: "_"),
                        displayName: tempCategory.name,
                        position: index,
                        iconResource: tempCategory.icon,
                        colorHex: tempCategory.colorHex,
                        isDefault: false,
                        createdAt: Int64(Date().timeIntervalSince1970 * 1000)
                    )
                    _ = try await categoryRepo.insertCategory(category)
                }

                // Import photos if any
                if !onboardingData.importedPhotos.isEmpty {
                    let photoRepo = PhotoRepositoryImpl()
                    for importedPhoto in onboardingData.importedPhotos {
                        // Fetch the PHAsset and save to app storage
                        await importPhoto(importedPhoto, using: photoRepo)
                    }
                }

                // Set up PIN if provided
                if let pin = onboardingData.pinCode, !pin.isEmpty {
                    try PINManager.shared.setPIN(pin)
                }

                // Mark onboarding as complete using SettingsManager
                let settings = SettingsManager.shared
                settings.onboardingCompleted = true
                settings.firstLaunch = false

                // Navigate to complete screen
                currentStep = .complete
                isComplete = true

                // Delay before dismissing
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    self.dismissOnboarding()
                }

            } catch {
                showError(message: "Failed to save onboarding data: \(error.localizedDescription)")
            }
        }
    }

    private func importPhoto(_ photo: ImportedPhoto, using repo: PhotoRepositoryImpl) async {
        let fetchOptions = PHFetchOptions()
        let results = PHAsset.fetchAssets(withLocalIdentifiers: [photo.assetIdentifier], options: fetchOptions)

        guard let asset = results.firstObject else { return }

        // Request image data
        let manager = PHImageManager.default()
        let options = PHImageRequestOptions()
        options.isSynchronous = false
        options.deliveryMode = .highQualityFormat

        await withCheckedContinuation { continuation in
            manager.requestImageDataAndOrientation(for: asset, options: options) { data, _, _, info in
                guard let imageData = data else {
                    continuation.resume()
                    return
                }

                // Save to documents directory
                let fileName = "\(UUID().uuidString).jpg"
                let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
                let imagePath = documentsPath.appendingPathComponent(fileName)

                do {
                    try imageData.write(to: imagePath)

                    // Create Photo object
                    let categoryId = Int64(photo.categoryId?.uuidString.hashValue ?? 1)
                    let photoObj = Photo(
                        path: imagePath.path,
                        categoryId: categoryId,
                        name: fileName,
                        isFromAssets: false,
                        createdAt: Int64(Date().timeIntervalSince1970 * 1000),
                        fileSize: Int64(imageData.count),
                        width: 0,
                        height: 0
                    )

                    Task {
                        _ = try await repo.insertPhoto(photoObj)
                    }
                } catch {
                    print("Failed to save photo: \(error)")
                }

                continuation.resume()
            }
        }
    }

    private func dismissOnboarding() {
        // This will be handled by the parent view
        NotificationCenter.default.post(name: .onboardingComplete, object: nil)
    }

    private func showError(message: String) {
        errorMessage = message
        showError = true
    }

    // MARK: - Demo Mode

    func enterDemoMode() {
        Task { @MainActor in
            do {
                // Set demo mode flags
                let settings = SettingsManager.shared
                settings.isDemoMode = true
                settings.demoModeEntered = true
                settings.demoModeEntryCount += 1

                // Mark onboarding as complete
                settings.onboardingCompleted = true
                settings.firstLaunch = false

                // Load demo data if needed
                try await loadDemoDataIfNeeded()

                // Navigate to complete screen
                currentStep = .complete
                isComplete = true

                // Delay before dismissing
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    self.dismissOnboarding()
                }

            } catch {
                showError(message: "Failed to enter demo mode: \(error.localizedDescription)")
            }
        }
    }

    private func loadDemoDataIfNeeded() async throws {
        let photoRepo = PhotoRepositoryImpl()
        let categoryRepo = CategoryRepositoryImpl.shared

        // Check if demo data already exists
        let existingPhotos = try await photoRepo.getAllPhotos()
        let demoPhotos = existingPhotos.filter { $0.isFromAssets }

        if !demoPhotos.isEmpty {
            print("Demo data already exists (\(demoPhotos.count) photos), skipping load")
            return
        }

        print("Loading demo data...")

        // Load categories first
        var loadedCategories: [Category] = []
        for (index, categoryData) in DemoData.categories.enumerated() {
            let category = Category(
                id: 0, // Auto-generate
                name: categoryData.name,
                displayName: categoryData.displayName,
                position: categoryData.position,
                iconResource: categoryData.icon,
                colorHex: categoryData.colorHex,
                isDefault: false,
                isDemoCategory: true,
                createdAt: Int64(Date().timeIntervalSince1970 * 1000)
            )

            let categoryId = try await categoryRepo.insertCategory(category)
            let insertedCategory = Category(
                id: categoryId,
                name: category.name,
                displayName: category.displayName,
                position: category.position,
                iconResource: category.iconResource,
                colorHex: category.colorHex,
                isDefault: category.isDefault,
                isDemoCategory: category.isDemoCategory,
                createdAt: category.createdAt
            )
            loadedCategories.append(insertedCategory)
            print("Created demo category: \(categoryData.displayName) (id: \(categoryId))")
        }

        // Load first 10 photos immediately (high priority)
        let priorityPhotos = Array(DemoData.photoMetadata.prefix(10))
        for photoMeta in priorityPhotos {
            try await loadDemoPhoto(photoMeta, from: loadedCategories, using: photoRepo)
        }

        // Load remaining photos in background
        let remainingPhotos = Array(DemoData.photoMetadata.dropFirst(10))
        if !remainingPhotos.isEmpty {
            Task.detached {
                for photoMeta in remainingPhotos {
                    do {
                        try await self.loadDemoPhoto(photoMeta, from: loadedCategories, using: photoRepo)
                    } catch {
                        print("Warning: Failed to load demo photo \(photoMeta.assetName): \(error)")
                    }
                }
                print("Background demo photo loading complete")
            }
        }

        print("Demo data loading initiated (10 photos loaded, \(remainingPhotos.count) loading in background)")
    }

    private func loadDemoPhoto(_ photoMeta: DemoData.PhotoMetadata, from categories: [Category], using photoRepo: PhotoRepositoryImpl) async throws {
        guard let categoryId = DemoData.getCategoryId(for: photoMeta.categoryName, from: categories) else {
            print("Warning: Category not found for \(photoMeta.categoryName)")
            return
        }

        // Load image from Assets
        guard let image = UIImage(named: photoMeta.assetName) else {
            print("Warning: Asset not found: \(photoMeta.assetName)")
            return
        }

        guard let imageData = image.jpegData(compressionQuality: 0.9) else {
            print("Warning: Failed to convert image to JPEG data: \(photoMeta.assetName)")
            return
        }

        // Save to Documents directory
        let documentsPath = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        let fileName = "\(photoMeta.assetName).jpg"
        let imagePath = documentsPath.appendingPathComponent(fileName)

        do {
            try imageData.write(to: imagePath)
        } catch {
            print("Warning: Failed to save demo photo \(fileName): \(error)")
            return
        }

        // Create Photo object
        let photo = Photo(
            id: 0,
            path: imagePath.path,
            categoryId: categoryId,
            name: photoMeta.assetName,
            isFromAssets: true,
            createdAt: Int64(photoMeta.date.timeIntervalSince1970 * 1000),
            fileSize: Int64(imageData.count),
            width: Int(image.size.width),
            height: Int(image.size.height)
        )

        _ = try await photoRepo.insertPhoto(photo)
        print("Loaded demo photo: \(photoMeta.assetName) -> category \(categoryId)")
    }
}

extension Notification.Name {
    static let onboardingComplete = Notification.Name("onboardingComplete")
}