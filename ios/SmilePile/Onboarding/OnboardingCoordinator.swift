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
            return "Create Categories"
        case .photoImport:
            return "Add Photos"
        case .pinSetup:
            return "Security Setup"
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

    // Progress tracking
    var progress: Double {
        // Adjusted for 4-screen flow (excluding photoImport)
        switch currentStep {
        case .welcome:
            return 0.0
        case .categories:
            return 0.33
        case .photoImport:
            return 0.5  // Should not be reached in normal flow
        case .pinSetup:
            return 0.66
        case .complete:
            return 1.0
        }
    }

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
}

extension Notification.Name {
    static let onboardingComplete = Notification.Name("onboardingComplete")
}