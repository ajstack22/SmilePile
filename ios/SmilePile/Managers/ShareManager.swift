import Foundation
import UIKit
import SwiftUI
import Photos

/// Manager class for sharing photos through native iOS share sheet.
/// Supports single and multiple photo sharing via UIActivityViewController.
class ShareManager: ObservableObject {

    static let shared = ShareManager()

    private init() {}

    /// Share a single photo through the native iOS share sheet
    /// - Parameters:
    ///   - photo: The Photo object to share
    ///   - sourceView: The view to present the share sheet from (for iPad popover)
    func sharePhoto(_ photo: Photo, from sourceView: UIView? = nil) {
        guard let image = loadImage(from: photo) else {
            print("Failed to load image for sharing: \(photo.displayName)")
            return
        }

        let items: [Any] = [image]
        presentShareSheet(with: items, from: sourceView)
    }

    /// Share multiple photos through the native iOS share sheet
    /// - Parameters:
    ///   - photos: Array of Photo objects to share
    ///   - sourceView: The view to present the share sheet from (for iPad popover)
    func sharePhotos(_ photos: [Photo], from sourceView: UIView? = nil) {
        let images = photos.compactMap { loadImage(from: $0) }

        guard !images.isEmpty else {
            print("No valid images to share")
            return
        }

        presentShareSheet(with: images, from: sourceView)
    }

    /// Load UIImage from a Photo object
    /// - Parameter photo: The Photo object
    /// - Returns: UIImage if successfully loaded, nil otherwise
    private func loadImage(from photo: Photo) -> UIImage? {
        // Check if it's from assets
        if photo.isFromAssets {
            // Load from assets bundle
            return UIImage(named: photo.path)
        } else {
            // Load from file path
            let fileURL = URL(fileURLWithPath: photo.path)

            // Check if file exists
            guard FileManager.default.fileExists(atPath: fileURL.path) else {
                print("File does not exist at path: \(photo.path)")
                return nil
            }

            // Load image data
            guard let imageData = try? Data(contentsOf: fileURL),
                  let image = UIImage(data: imageData) else {
                print("Failed to load image data from: \(photo.path)")
                return nil
            }

            return image
        }
    }

    /// Present the UIActivityViewController
    /// - Parameters:
    ///   - items: Array of items to share
    ///   - sourceView: The view to present from (for iPad popover)
    private func presentShareSheet(with items: [Any], from sourceView: UIView? = nil) {
        let activityViewController = UIActivityViewController(
            activityItems: items,
            applicationActivities: nil
        )

        // Exclude certain activity types if needed
        activityViewController.excludedActivityTypes = [
            .assignToContact,
            .addToReadingList,
            .openInIBooks
        ]

        // Get the current window scene
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first,
              let rootViewController = window.rootViewController else {
            print("Unable to get root view controller for presenting share sheet")
            return
        }

        // Get the top most view controller
        let viewController = topMostViewController(from: rootViewController)

        // Configure for iPad (popover presentation)
        if let popoverController = activityViewController.popoverPresentationController {
            if let sourceView = sourceView {
                popoverController.sourceView = sourceView
                popoverController.sourceRect = sourceView.bounds
            } else {
                // Default to center of screen if no source view provided
                popoverController.sourceView = viewController.view
                popoverController.sourceRect = CGRect(
                    x: viewController.view.bounds.midX,
                    y: viewController.view.bounds.midY,
                    width: 0,
                    height: 0
                )
            }
        }

        // Present the share sheet
        viewController.present(activityViewController, animated: true)
    }

    /// Find the topmost view controller
    /// - Parameter viewController: The root view controller
    /// - Returns: The topmost presented view controller
    private func topMostViewController(from viewController: UIViewController) -> UIViewController {
        if let presented = viewController.presentedViewController {
            return topMostViewController(from: presented)
        }

        if let navigation = viewController as? UINavigationController,
           let visible = navigation.visibleViewController {
            return topMostViewController(from: visible)
        }

        if let tab = viewController as? UITabBarController,
           let selected = tab.selectedViewController {
            return topMostViewController(from: selected)
        }

        return viewController
    }
}

// MARK: - SwiftUI Integration

/// SwiftUI wrapper for presenting the share sheet
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(
            activityItems: items,
            applicationActivities: nil
        )

        controller.excludedActivityTypes = [
            .assignToContact,
            .addToReadingList,
            .openInIBooks
        ]

        return controller
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {
        // No updates needed
    }
}

/// SwiftUI View Modifier for easy sharing
extension View {
    /// Present a share sheet with the given items
    /// - Parameters:
    ///   - isPresented: Binding to control presentation
    ///   - items: Items to share
    func shareSheet(isPresented: Binding<Bool>, items: [Any]) -> some View {
        self.sheet(isPresented: isPresented) {
            if !items.isEmpty {
                ShareSheetView(items: items)
            }
        }
    }
}

/// SwiftUI wrapper view for ShareSheet - renamed to avoid conflict
struct ShareSheetViewInternal: View {
    let items: [Any]

    var body: some View {
        ShareSheet(items: items)
            .edgesIgnoringSafeArea(.all)
    }
}

// MARK: - Photo Sharing Extensions

extension ShareManager {
    /// Share photos from a SwiftUI context
    /// - Parameters:
    ///   - photos: Array of photos to share
    ///   - completion: Optional completion handler
    func sharePhotosFromSwiftUI(_ photos: [Photo], completion: (() -> Void)? = nil) {
        DispatchQueue.main.async {
            self.sharePhotos(photos)
            completion?()
        }
    }

    /// Share a single photo from SwiftUI context
    /// - Parameters:
    ///   - photo: The photo to share
    ///   - completion: Optional completion handler
    func sharePhotoFromSwiftUI(_ photo: Photo, completion: (() -> Void)? = nil) {
        DispatchQueue.main.async {
            self.sharePhoto(photo)
            completion?()
        }
    }
}