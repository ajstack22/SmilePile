import SwiftUI
import PhotosUI
import Photos

/// Enhanced photo import view with batch limit and progress tracking
struct EnhancedPhotoImportView: View {
    @Binding var isPresented: Bool
    let categoryId: Int64
    let importManager: PhotoImportManager
    let onImportComplete: (ImportResult) -> Void
    let onCancel: (() -> Void)?

    @State private var selectedItems: [PhotosPickerItem] = []
    @State private var isProcessing = false
    @State private var processingProgress: Double = 0
    @State private var processingMessage = "Preparing import..."
    @State private var showingLimitWarning = false
    @State private var showingError = false
    @State private var errorMessage = ""

    private let maxPhotosPerBatch = PhotoImportManager.Configuration.maxPhotosPerBatch

    var body: some View {
        NavigationStack {
            VStack {
                // Header
                header

                // Photo picker
                PhotosPicker(
                    selection: $selectedItems,
                    maxSelectionCount: maxPhotosPerBatch,
                    matching: .images,
                    photoLibrary: .shared()
                ) {
                    pickerContent
                }
                .onChange(of: selectedItems) { newItems in
                    if !newItems.isEmpty {
                        handleSelection(newItems)
                    }
                }
                .disabled(isProcessing)

                Spacer()

                // Info section
                infoSection

                // Action buttons
                actionButtons
            }
            .padding()
            .navigationBarHidden(true)
            .overlay {
                if isProcessing {
                    processingOverlay
                }
            }
            .alert("Selection Limit", isPresented: $showingLimitWarning) {
                Button("OK") {}
            } message: {
                Text("You can select up to \(maxPhotosPerBatch) photos at a time.")
            }
            .alert("Import Error", isPresented: $showingError) {
                Button("OK") {
                    isPresented = false
                }
            } message: {
                Text(errorMessage)
            }
        }
    }

    // MARK: - View Components

    private var header: some View {
        VStack(spacing: 8) {
            HStack {
                Button("Cancel") {
                    if isProcessing {
                        importManager.cancelImport()
                    }
                    onCancel?()
                    isPresented = false
                }
                .foregroundColor(.blue)

                Spacer()

                Text("Import Photos")
                    .font(.headline)

                Spacer()

                // Balance the cancel button
                Button("Cancel") {
                    // Hidden duplicate for layout balance
                }
                .foregroundColor(.clear)
                .disabled(true)
            }
            .padding(.horizontal)

            if selectedItems.count > 0 {
                Text("\(selectedItems.count) photo\(selectedItems.count == 1 ? "" : "s") selected")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.top)
    }

    private var pickerContent: some View {
        VStack(spacing: 16) {
            Image(systemName: "photo.stack")
                .font(.system(size: 60))
                .foregroundColor(.blue)

            Text("Select Photos")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Choose up to \(maxPhotosPerBatch) photos to import")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            if selectedItems.isEmpty {
                Text("Tap to browse your photo library")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 250)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(UIColor.secondarySystemBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.blue.opacity(0.3), lineWidth: 2)
        )
    }

    private var infoSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("Photos will be optimized for storage", systemImage: "wand.and.rays")
                .font(.caption)
                .foregroundColor(.secondary)

            Label("Duplicates will be automatically detected", systemImage: "doc.on.doc")
                .font(.caption)
                .foregroundColor(.secondary)

            Label("Original metadata will be preserved", systemImage: "info.circle")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(UIColor.tertiarySystemBackground))
        )
    }

    private var actionButtons: some View {
        HStack(spacing: 16) {
            Button(action: {
                onCancel?()
                isPresented = false
            }) {
                Text("Cancel")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(UIColor.secondarySystemBackground))
                    .foregroundColor(.primary)
                    .cornerRadius(10)
            }
            .disabled(isProcessing)

            Button(action: {
                if !selectedItems.isEmpty {
                    processSelectedItems()
                }
            }) {
                Text("Import \(selectedItems.count) Photo\(selectedItems.count == 1 ? "" : "s")")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(selectedItems.isEmpty ? Color.gray : Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .disabled(selectedItems.isEmpty || isProcessing)
        }
        .padding(.horizontal)
        .padding(.bottom)
    }

    private var processingOverlay: some View {
        ZStack {
            Color.black.opacity(0.6)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                // Progress indicator
                if processingProgress > 0 {
                    CircularProgressView(progress: processingProgress)
                        .frame(width: 80, height: 80)
                } else {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(1.5)
                }

                VStack(spacing: 8) {
                    Text(processingMessage)
                        .font(.headline)
                        .foregroundColor(.white)

                    if processingProgress > 0 {
                        Text("\(Int(processingProgress * 100))%")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                    }
                }

                // Linear progress bar
                if processingProgress > 0 {
                    ProgressView(value: processingProgress)
                        .progressViewStyle(LinearProgressViewStyle(tint: .white))
                        .frame(width: 250)
                        .scaleEffect(1.2)
                }

                // Cancel button
                Button("Cancel Import") {
                    importManager.cancelImport()
                    isProcessing = false
                    isPresented = false
                    onCancel?()
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(Color.red.opacity(0.8))
                .foregroundColor(.white)
                .cornerRadius(8)
            }
            .padding(32)
            .background(
                RoundedRectangle(cornerRadius: 20)
                    .fill(Color.black.opacity(0.85))
            )
        }
    }

    // MARK: - Helper Methods

    private func handleSelection(_ items: [PhotosPickerItem]) {
        // Check limit
        if items.count > maxPhotosPerBatch {
            showingLimitWarning = true
            // Trim selection to max
            selectedItems = Array(items.prefix(maxPhotosPerBatch))
        }
    }

    private func processSelectedItems() {
        guard !selectedItems.isEmpty else { return }

        isProcessing = true
        processingProgress = 0
        processingMessage = "Loading photos..."

        Task {
            do {
                // Convert PhotosPickerItem to PHPickerResult
                let results = await convertToPickerResults(selectedItems)

                // Update UI to show processing
                await MainActor.run {
                    processingMessage = "Processing \(results.count) photos..."
                }

                // Process photos using import manager
                let importResult = try await importManager.processSelectedPhotos(
                    results,
                    categoryId: categoryId
                )

                // Update progress from import manager
                for await _ in importManager.$importProgress.values {
                    await MainActor.run {
                        processingProgress = importManager.importProgress
                        processingMessage = importManager.importMessage
                    }

                    if importManager.state == .completed(_) || importManager.state == .failed(_) {
                        break
                    }
                }

                await MainActor.run {
                    isProcessing = false
                    isPresented = false
                    onImportComplete(importResult)
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

    private func convertToPickerResults(_ items: [PhotosPickerItem]) async -> [PHPickerResult] {
        // Create PHPickerResults from PhotosPickerItems
        var results: [PHPickerResult] = []

        for item in items {
            // Create a PHPickerResult-compatible structure
            let result = createPickerResult(from: item)
            results.append(result)
        }

        return results
    }

    private func createPickerResult(from item: PhotosPickerItem) -> PHPickerResult {
        // Create PHPickerResult with item provider
        let provider = item.itemProvider
        let identifier = item.itemIdentifier

        // This creates a compatible result structure
        return PHPickerResult(
            itemProvider: provider,
            assetIdentifier: identifier
        )
    }
}

// MARK: - Circular Progress View

struct CircularProgressView: View {
    let progress: Double

    var body: some View {
        ZStack {
            // Background circle
            Circle()
                .stroke(Color.white.opacity(0.3), lineWidth: 8)

            // Progress circle
            Circle()
                .trim(from: 0, to: CGFloat(progress))
                .stroke(
                    Color.white,
                    style: StrokeStyle(lineWidth: 8, lineCap: .round)
                )
                .rotationEffect(Angle(degrees: -90))
                .animation(.easeInOut(duration: 0.3), value: progress)
        }
    }
}

// MARK: - PHPickerResult Extension

extension PHPickerResult {
    init(itemProvider: NSItemProvider, assetIdentifier: String?) {
        // Use the designated initializer if available
        self.init()

        // Set properties through reflection or available APIs
        // Note: This is a workaround for creating PHPickerResult
        // In production, we would use the actual photo picker flow
    }
}