import SwiftUI
import PhotosUI

/// SwiftUI wrapper for PHPickerViewController with enhanced configuration
struct PhotoPickerView: UIViewControllerRepresentable {

    // MARK: - Configuration
    struct Configuration {
        var selectionLimit: Int = 0  // 0 = unlimited
        var filter: PHPickerFilter? = .images
        var preferredAssetRepresentationMode: PHPickerConfiguration.AssetRepresentationMode = .automatic
        var allowsMultipleSelection: Bool = true
        var isOrdered: Bool = true

        static var `default`: Configuration {
            Configuration()
        }

        static var singleSelection: Configuration {
            Configuration(selectionLimit: 1, allowsMultipleSelection: false)
        }
    }

    // MARK: - Properties
    @Binding var isPresented: Bool
    let configuration: Configuration
    let onSelection: ([PHPickerResult]) -> Void
    let onCancel: (() -> Void)?

    // MARK: - Initialization
    init(
        isPresented: Binding<Bool>,
        configuration: Configuration = .default,
        onSelection: @escaping ([PHPickerResult]) -> Void,
        onCancel: (() -> Void)? = nil
    ) {
        self._isPresented = isPresented
        self.configuration = configuration
        self.onSelection = onSelection
        self.onCancel = onCancel
    }

    // MARK: - UIViewControllerRepresentable
    func makeUIViewController(context: Context) -> PHPickerViewController {
        var config = PHPickerConfiguration(photoLibrary: .shared())
        config.selectionLimit = configuration.selectionLimit
        config.filter = configuration.filter
        config.preferredAssetRepresentationMode = configuration.preferredAssetRepresentationMode

        if #available(iOS 15.0, *) {
            config.selection = configuration.isOrdered ? .ordered : .default
        }

        let picker = PHPickerViewController(configuration: config)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {
        // No updates needed
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    // MARK: - Coordinator
    class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let parent: PhotoPickerView

        init(_ parent: PhotoPickerView) {
            self.parent = parent
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            parent.isPresented = false

            if results.isEmpty {
                parent.onCancel?()
            } else {
                parent.onSelection(results)
            }
        }
    }
}

/// Enhanced photo picker view with permission handling and error recovery
struct EnhancedPhotoPickerView: View {
    @Binding var isPresented: Bool
    @StateObject private var permissionManager = PhotoLibraryPermissionManager.shared
    @State private var showingPicker = false
    @State private var showingError = false
    @State private var errorMessage = ""
    @State private var isProcessing = false
    @State private var processingProgress: Double = 0

    let categoryId: Int64
    let onPhotosSelected: ([Photo]) -> Void
    let onCancel: (() -> Void)?

    private let processor = PhotoAssetProcessor()

    var body: some View {
        ZStack {
            Color.clear
                .requirePhotoLibraryPermission(
                    onAuthorized: {
                        showingPicker = true
                    },
                    onDenied: {
                        isPresented = false
                        onCancel?()
                    }
                )

            if isProcessing {
                ProcessingOverlay(progress: processingProgress)
            }
        }
        .sheet(isPresented: $showingPicker) {
            PhotoPickerView(
                isPresented: $showingPicker,
                configuration: .default,
                onSelection: { results in
                    handleSelection(results)
                },
                onCancel: {
                    isPresented = false
                    onCancel?()
                }
            )
        }
        .alert("Error", isPresented: $showingError) {
            Button("OK") {
                isPresented = false
            }
        } message: {
            Text(errorMessage)
        }
    }

    private func handleSelection(_ results: [PHPickerResult]) {
        Task {
            isProcessing = true
            processingProgress = 0

            do {
                let photos = try await processor.processPickerResults(
                    results,
                    categoryId: categoryId
                ) { progress in
                    processingProgress = progress
                }

                await MainActor.run {
                    isProcessing = false
                    isPresented = false
                    onPhotosSelected(photos)
                }
            } catch {
                await MainActor.run {
                    isProcessing = false
                    errorMessage = error.localizedDescription
                    showingError = true
                }
            }
        }
    }
}

/// Processing overlay view
private struct ProcessingOverlay: View {
    let progress: Double

    var body: some View {
        ZStack {
            Color.black.opacity(0.5)
                .ignoresSafeArea()

            VStack(spacing: 20) {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    .scaleEffect(1.5)

                Text("Processing Photos...")
                    .font(.headline)
                    .foregroundColor(.white)

                if progress > 0 {
                    ProgressView(value: progress)
                        .progressViewStyle(LinearProgressViewStyle(tint: .white))
                        .frame(width: 200)

                    Text("\(Int(progress * 100))%")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.8))
                }
            }
            .padding(40)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(Color.black.opacity(0.8))
            )
        }
    }
}

/// Limited library selection prompt
struct LimitedLibraryPrompt: View {
    @StateObject private var permissionManager = PhotoLibraryPermissionManager.shared

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "photo.on.rectangle.angled")
                .font(.system(size: 48))
                .foregroundColor(.orange)

            Text("Limited Photo Access")
                .font(.headline)

            Text("You've granted limited access to photos. You can select more photos or grant full access.")
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            HStack(spacing: 12) {
                Button("Select More Photos") {
                    permissionManager.presentLimitedLibraryPicker()
                }
                .buttonStyle(.bordered)

                Button("Grant Full Access") {
                    permissionManager.openAppSettings()
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(UIColor.secondarySystemBackground))
        )
        .padding()
    }
}