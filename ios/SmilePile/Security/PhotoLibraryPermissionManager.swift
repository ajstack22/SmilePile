import Photos
import SwiftUI

/// Manages photo library permissions with comprehensive state handling
class PhotoLibraryPermissionManager: ObservableObject {

    // MARK: - Published Properties
    @Published private(set) var authorizationStatus: PHAuthorizationStatus = .notDetermined
    @Published private(set) var isAuthorized: Bool = false
    @Published private(set) var errorMessage: String?
    @Published private(set) var isCheckingPermission: Bool = false

    // MARK: - Singleton
    static let shared = PhotoLibraryPermissionManager()

    private init() {
        checkCurrentAuthorizationStatus()
    }

    // MARK: - Permission Status

    /// Check current authorization status without prompting
    func checkCurrentAuthorizationStatus() {
        let status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        updateAuthorizationStatus(status)
    }

    /// Request photo library permission if needed
    @MainActor
    func requestPermission() async -> Bool {
        isCheckingPermission = true
        errorMessage = nil

        defer {
            isCheckingPermission = false
        }

        // Check current status first
        let currentStatus = PHPhotoLibrary.authorizationStatus(for: .readWrite)

        switch currentStatus {
        case .authorized, .limited:
            updateAuthorizationStatus(currentStatus)
            return true

        case .denied, .restricted:
            updateAuthorizationStatus(currentStatus)
            setErrorForStatus(currentStatus)
            return false

        case .notDetermined:
            // Request permission
            let status = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
            updateAuthorizationStatus(status)

            if status == .denied || status == .restricted {
                setErrorForStatus(status)
                return false
            }

            return status == .authorized || status == .limited

        @unknown default:
            updateAuthorizationStatus(currentStatus)
            errorMessage = "Unknown authorization status. Please check your settings."
            return false
        }
    }

    /// Handle limited photo selection (iOS 14+)
    @MainActor
    func presentLimitedLibraryPicker(from viewController: UIViewController? = nil) {
        guard authorizationStatus == .limited else { return }

        PHPhotoLibrary.shared().presentLimitedLibraryPicker(
            from: viewController ?? UIApplication.shared.windows.first?.rootViewController ?? UIViewController()
        )
    }

    // MARK: - Private Methods

    private func updateAuthorizationStatus(_ status: PHAuthorizationStatus) {
        DispatchQueue.main.async { [weak self] in
            self?.authorizationStatus = status
            self?.isAuthorized = (status == .authorized || status == .limited)
        }
    }

    private func setErrorForStatus(_ status: PHAuthorizationStatus) {
        switch status {
        case .denied:
            errorMessage = "Photo library access denied. Please enable access in Settings > SmilePile > Photos."
        case .restricted:
            errorMessage = "Photo library access is restricted. This may be due to parental controls or device management."
        case .limited:
            errorMessage = nil // Limited access is acceptable
        default:
            errorMessage = nil
        }
    }

    // MARK: - Helper Methods

    /// Open app settings for user to change permissions
    func openAppSettings() {
        guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else { return }

        if UIApplication.shared.canOpenURL(settingsUrl) {
            UIApplication.shared.open(settingsUrl)
        }
    }

    /// Get user-friendly permission status message
    func getStatusMessage() -> String {
        switch authorizationStatus {
        case .notDetermined:
            return "Photo library access not yet requested"
        case .restricted:
            return "Photo library access is restricted"
        case .denied:
            return "Photo library access denied"
        case .authorized:
            return "Full photo library access granted"
        case .limited:
            return "Limited photo library access granted"
        @unknown default:
            return "Unknown permission status"
        }
    }

    /// Check if we should show permission prompt
    var shouldShowPermissionPrompt: Bool {
        authorizationStatus == .notDetermined
    }

    /// Check if we should show settings prompt
    var shouldShowSettingsPrompt: Bool {
        authorizationStatus == .denied
    }

    /// Check if we have any level of access
    var hasAnyAccess: Bool {
        authorizationStatus == .authorized || authorizationStatus == .limited
    }
}

// MARK: - SwiftUI View Modifier for Permission Handling

struct PhotoLibraryPermissionModifier: ViewModifier {
    @StateObject private var permissionManager = PhotoLibraryPermissionManager.shared
    @State private var showingPermissionAlert = false
    @State private var showingSettingsAlert = false

    let onAuthorized: () -> Void
    let onDenied: (() -> Void)?

    func body(content: Content) -> some View {
        content
            .onAppear {
                checkPermissions()
            }
            .alert("Photo Library Access Required", isPresented: $showingPermissionAlert) {
                Button("Allow Access") {
                    Task {
                        let authorized = await permissionManager.requestPermission()
                        if authorized {
                            onAuthorized()
                        } else if permissionManager.authorizationStatus == .denied {
                            showingSettingsAlert = true
                        }
                    }
                }
                Button("Cancel", role: .cancel) {
                    onDenied?()
                }
            } message: {
                Text("SmilePile needs access to your photos to let you select and organize them for your child.")
            }
            .alert("Permission Denied", isPresented: $showingSettingsAlert) {
                Button("Open Settings") {
                    permissionManager.openAppSettings()
                }
                Button("Cancel", role: .cancel) {
                    onDenied?()
                }
            } message: {
                Text("Photo library access was denied. You can enable it in Settings.")
            }
    }

    private func checkPermissions() {
        permissionManager.checkCurrentAuthorizationStatus()

        if permissionManager.shouldShowPermissionPrompt {
            showingPermissionAlert = true
        } else if permissionManager.hasAnyAccess {
            onAuthorized()
        } else if permissionManager.shouldShowSettingsPrompt {
            showingSettingsAlert = true
        } else {
            onDenied?()
        }
    }
}

extension View {
    func requirePhotoLibraryPermission(
        onAuthorized: @escaping () -> Void,
        onDenied: (() -> Void)? = nil
    ) -> some View {
        modifier(PhotoLibraryPermissionModifier(
            onAuthorized: onAuthorized,
            onDenied: onDenied
        ))
    }
}