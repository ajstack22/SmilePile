import SwiftUI

/// Centralized error handling for the app
struct ErrorHandling {

    // MARK: - Error Types
    enum AppError: LocalizedError {
        case photoLibraryAccess(reason: String)
        case photoProcessing(reason: String)
        case storage(reason: String)
        case network(reason: String)
        case unknown(Error?)

        var errorDescription: String? {
            switch self {
            case .photoLibraryAccess(let reason):
                return "Photo Library Error: \(reason)"
            case .photoProcessing(let reason):
                return "Photo Processing Error: \(reason)"
            case .storage(let reason):
                return "Storage Error: \(reason)"
            case .network(let reason):
                return "Network Error: \(reason)"
            case .unknown(let error):
                return error?.localizedDescription ?? "An unknown error occurred"
            }
        }

        var recoverySuggestion: String? {
            switch self {
            case .photoLibraryAccess:
                return "Please check your photo library permissions in Settings."
            case .photoProcessing:
                return "Try selecting fewer photos or photos with smaller file sizes."
            case .storage:
                return "Please free up some storage space and try again."
            case .network:
                return "Please check your internet connection and try again."
            case .unknown:
                return "Please try again. If the problem persists, restart the app."
            }
        }

        var isRetryable: Bool {
            switch self {
            case .photoLibraryAccess:
                return false // Requires settings change
            case .photoProcessing, .storage, .network, .unknown:
                return true
            }
        }
    }

    // MARK: - Error Alert Model
    struct ErrorAlert: Identifiable {
        let id = UUID()
        let error: AppError
        let retryAction: (() -> Void)?
        let dismissAction: (() -> Void)?

        var title: String {
            "Error"
        }

        var message: String {
            var text = error.errorDescription ?? "An error occurred"
            if let suggestion = error.recoverySuggestion {
                text += "\n\n\(suggestion)"
            }
            return text
        }
    }
}

// MARK: - Error Alert View Modifier

struct ErrorAlertModifier: ViewModifier {
    @Binding var errorAlert: ErrorHandling.ErrorAlert?

    func body(content: Content) -> some View {
        content
            .alert(item: $errorAlert) { alert in
                Alert(
                    title: Text(alert.title),
                    message: Text(alert.message),
                    primaryButton: .default(Text(alert.error.isRetryable ? "Retry" : "OK")) {
                        if alert.error.isRetryable {
                            alert.retryAction?()
                        } else {
                            alert.dismissAction?()
                        }
                        errorAlert = nil
                    },
                    secondaryButton: .cancel {
                        alert.dismissAction?()
                        errorAlert = nil
                    }
                )
            }
    }
}

extension View {
    func errorAlert(_ errorAlert: Binding<ErrorHandling.ErrorAlert?>) -> some View {
        modifier(ErrorAlertModifier(errorAlert: errorAlert))
    }
}

// MARK: - Error Recovery Manager

class ErrorRecoveryManager: ObservableObject {
    @Published var currentError: ErrorHandling.ErrorAlert?

    func handleError(
        _ error: Error,
        retryAction: (() -> Void)? = nil,
        dismissAction: (() -> Void)? = nil
    ) {
        let appError: ErrorHandling.AppError

        // Categorize the error
        if let photoError = error as? PhotoAssetProcessor.ProcessingError {
            switch photoError {
            case .iCloudDownloadRequired:
                appError = .photoProcessing(reason: "Photos need to be downloaded from iCloud. Please ensure you're connected to the internet.")
            case .insufficientMemory:
                appError = .photoProcessing(reason: "Not enough memory to process photos. Try selecting fewer photos.")
            case .unsupportedFormat:
                appError = .photoProcessing(reason: "Some photos are in an unsupported format.")
            default:
                appError = .photoProcessing(reason: photoError.localizedDescription)
            }
        } else if (error as NSError).domain == "NSCocoaErrorDomain" {
            let code = (error as NSError).code
            if code == NSFileWriteOutOfSpaceError || code == NSFileWriteNoPermissionError {
                appError = .storage(reason: "Unable to save photos. Check available storage.")
            } else {
                appError = .unknown(error)
            }
        } else {
            appError = .unknown(error)
        }

        DispatchQueue.main.async {
            self.currentError = ErrorHandling.ErrorAlert(
                error: appError,
                retryAction: retryAction,
                dismissAction: dismissAction
            )
        }
    }

    func clearError() {
        currentError = nil
    }
}

// MARK: - User-Friendly Error Messages

extension Error {
    var userFriendlyMessage: String {
        if let appError = self as? ErrorHandling.AppError {
            return appError.errorDescription ?? "An error occurred"
        } else if let localizedError = self as? LocalizedError {
            return localizedError.errorDescription ?? localizedDescription
        } else {
            return localizedDescription
        }
    }

    var isRetryable: Bool {
        if let appError = self as? ErrorHandling.AppError {
            return appError.isRetryable
        }
        // Default to retryable for unknown errors
        return true
    }
}