import SwiftUI
import PhotosUI
import Photos

struct PhotoGalleryView: View {
    @EnvironmentObject var kidsModeViewModel: KidsModeViewModel
    @State private var selectedCategory: Category?
    @State private var categories: [Category] = []
    @State private var selectedPhotos: [Photo] = []
    @State private var showingPhotoEditor = false
    @State private var showingPhotoPicker = false
    @State private var isSelectionMode = false
    @State private var selectedForSharing: Set<Int64> = []
    @State private var showingShareSheet = false
    @State private var showingPermissionError = false
    @State private var permissionErrorMessage = ""
    @State private var allPhotos: [Photo] = []
    @State private var isLoadingPhotos = false
    @State private var importProgress: Double = 0
    @State private var importMessage: String = ""
    @State private var showImportError = false
    @State private var importErrorMessage = ""
    @State private var showImportSuccess = false
    @State private var importSuccessMessage = ""

    private let repository = CategoryRepositoryImpl()
    private let photoRepository = PhotoRepositoryImpl()
    @StateObject private var permissionManager = PhotoLibraryPermissionManager.shared
    // Temporarily using PhotoImportCoordinator until we can add files to Xcode project
    @State private var photoImportCoordinator: PhotoImportCoordinator?

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // Selection toolbar when in selection mode
                if isSelectionMode {
                    selectionToolbar
                }

                // AppHeader with SmilePile logo and eye button (hide in selection mode)
                if !isSelectionMode {
                    AppHeaderComponent(
                    onViewModeClick: {
                        print("DEBUG: Eye button tapped in PhotoGalleryView")
                        print("DEBUG: Current isKidsMode = \(kidsModeViewModel.isKidsMode)")
                        kidsModeViewModel.toggleKidsMode()
                        print("DEBUG: After toggle isKidsMode = \(kidsModeViewModel.isKidsMode)")
                    },
                        showViewModeButton: true
                    ) {
                        if !categories.isEmpty {
                            categoryFilterBar
                        }
                    }
                }

                // Photo Grid
                ScrollView {
                    if filteredPhotos.isEmpty {
                        EmptyGalleryPlaceholder()
                            .frame(height: 400)
                    } else {
                        LazyVGrid(columns: [
                            GridItem(.flexible()),
                            GridItem(.flexible()),
                            GridItem(.flexible())
                        ], spacing: 2) {
                            ForEach(filteredPhotos) { photo in
                                ZStack(alignment: .topTrailing) {
                                    PhotoThumbnail(photo: photo)
                                        .onTapGesture {
                                            if isSelectionMode {
                                                toggleSelection(for: photo)
                                            } else {
                                                selectedPhotos = [photo]
                                                showingPhotoEditor = true
                                            }
                                        }
                                        .onLongPressGesture {
                                            if !isSelectionMode {
                                                enterSelectionMode(selecting: photo)
                                            }
                                        }

                                    // Selection checkbox overlay
                                    if isSelectionMode {
                                        Image(systemName: selectedForSharing.contains(photo.id) ? "checkmark.circle.fill" : "circle")
                                            .font(.title2)
                                            .foregroundColor(selectedForSharing.contains(photo.id) ? .blue : .white)
                                            .background(Circle().fill(Color.black.opacity(0.5)))
                                            .padding(8)
                                    }
                                }
                            }
                        }
                        .padding(.bottom, 100) // Space for FAB and tab bar
                    }
                }
            }
            .background(Color(UIColor.systemBackground))

            // Floating Action Button with pulse animation when gallery is empty
            FloatingActionButtonContainer(
                action: {
                    handleAddPhotosButtonTap()
                },
                isPulsing: allPhotos.isEmpty,
                bottomPadding: 49 // Standard iOS tab bar height
            )

            // Loading overlay with import progress
            if isLoadingPhotos {
                LoadingOverlay(
                    message: importMessage.isEmpty ? "Loading photos..." : importMessage,
                    progress: importProgress > 0 ? importProgress : nil
                )
            }
        }
        .onAppear {
            // Initialize coordinator if needed
            if photoImportCoordinator == nil {
                photoImportCoordinator = PhotoImportCoordinator.createDefault()
            }
            loadCategories()
            loadPhotos()
        }
        .sheet(isPresented: $showingPhotoEditor) {
            if !selectedPhotos.isEmpty {
                PhotoEditorView(photos: selectedPhotos) { editedPhotos in
                    // Handle edited photos
                    handleEditedPhotos(editedPhotos)
                    showingPhotoEditor = false
                }
            }
        }
        .fullScreenCover(isPresented: $showingPhotoPicker) {
            EnhancedPhotoPickerView(
                isPresented: $showingPhotoPicker,
                categoryId: selectedCategory?.id ?? categories.first?.id ?? 1,
                onPhotosSelected: { photos in
                    handleSelectedPhotos(photos)
                },
                onCancel: {
                    // Handle cancellation if needed
                }
            )
        }
        .alert("Permission Required", isPresented: $showingPermissionError) {
            Button("Open Settings") {
                permissionManager.openAppSettings()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(permissionErrorMessage)
        }
        .alert("Import Error", isPresented: $showImportError) {
            Button("OK") {}
        } message: {
            Text(importErrorMessage)
        }
        .alert("Success", isPresented: $showImportSuccess) {
            Button("OK") {}
        } message: {
            Text(importSuccessMessage)
        }
        .sheet(isPresented: $showingShareSheet) {
            if !selectedForSharing.isEmpty {
                let photosToShare = allPhotos.filter { selectedForSharing.contains($0.id) }
                ShareSheetView(items: photosToShare.compactMap { loadImageForSharing($0) })
            }
        }
    }

    // MARK: - Selection Mode

    private var selectionToolbar: some View {
        HStack {
            Button("Cancel") {
                exitSelectionMode()
            }
            .padding()

            Spacer()

            Text("\(selectedForSharing.count) selected")
                .font(.headline)

            Spacer()

            Button(action: {
                if !selectedForSharing.isEmpty {
                    showingShareSheet = true
                }
            }) {
                Image(systemName: "square.and.arrow.up")
                    .font(.title2)
            }
            .padding()
            .disabled(selectedForSharing.isEmpty)
        }
        .background(Color(UIColor.systemGray5))
    }

    private func toggleSelection(for photo: Photo) {
        if selectedForSharing.contains(photo.id) {
            selectedForSharing.remove(photo.id)
        } else {
            selectedForSharing.insert(photo.id)
        }
    }

    private func enterSelectionMode(selecting photo: Photo) {
        isSelectionMode = true
        selectedForSharing.insert(photo.id)
    }

    private func exitSelectionMode() {
        isSelectionMode = false
        selectedForSharing.removeAll()
    }

    private func loadImageForSharing(_ photo: Photo) -> UIImage? {
        if photo.isFromAssets {
            return UIImage(named: photo.path)
        } else {
            let fileURL = URL(fileURLWithPath: photo.path)
            if let imageData = try? Data(contentsOf: fileURL),
               let image = UIImage(data: imageData) {
                return image
            }
        }
        return nil
    }

    private var categoryFilterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                // All Photos chip
                CategoryChip(
                    displayName: "All Photos",
                    colorHex: "#9E9E9E",
                    isSelected: selectedCategory == nil,
                    onTap: {
                        selectedCategory = nil
                    }
                )

                // Category chips
                ForEach(categories) { category in
                    CategoryChip(
                        displayName: category.displayName,
                        colorHex: category.colorHex ?? "#4CAF50",
                        isSelected: selectedCategory?.id == category.id,
                        onTap: {
                            selectedCategory = category
                        }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }

    private var filteredPhotos: [Photo] {
        if let selectedCategory = selectedCategory {
            return allPhotos.filter { $0.categoryId == selectedCategory.id }
        } else {
            return allPhotos
        }
    }

    private func loadCategories() {
        Task {
            do {
                categories = try await repository.getAllCategories()
            } catch {
                print("Failed to load categories: \(error)")
            }
        }
    }

    private func loadPhotos() {
        Task {
            do {
                isLoadingPhotos = true
                allPhotos = try await photoRepository.getAllPhotos()
                isLoadingPhotos = false
            } catch {
                print("Failed to load photos: \(error)")
                allPhotos = []
                isLoadingPhotos = false
            }
        }
    }

    // MARK: - Photo Handling Methods

    private func handleAddPhotosButtonTap() {
        // Check permission status first
        permissionManager.checkCurrentAuthorizationStatus()

        switch permissionManager.authorizationStatus {
        case .notDetermined:
            // Will be handled by the photo picker view
            showingPhotoPicker = true
        case .authorized, .limited:
            showingPhotoPicker = true
        case .denied:
            permissionErrorMessage = "Photo library access is required to add photos. Please enable it in Settings."
            showingPermissionError = true
        case .restricted:
            permissionErrorMessage = "Photo library access is restricted on this device."
            showingPermissionError = true
        @unknown default:
            showingPhotoPicker = true
        }
    }

    private func handleSelectedPhotos(_ photos: [Photo]) {
        Task {
            isLoadingPhotos = true
            importMessage = "Saving photos..."
            do {
                // Save photos to repository
                for photo in photos {
                    _ = try await photoRepository.insertPhoto(photo)
                }
                // Reload all photos
                allPhotos = try await photoRepository.getAllPhotos()

                // Show success message
                importSuccessMessage = "Successfully imported \(photos.count) photo\(photos.count == 1 ? "" : "s")"
                showImportSuccess = true
            } catch {
                importErrorMessage = "Error saving photos: \(error.localizedDescription)"
                showImportError = true
            }
            isLoadingPhotos = false

            // If we have photos, open editor
            if !photos.isEmpty {
                selectedPhotos = photos
                showingPhotoEditor = true
            }
        }
    }

    private func handleEditedPhotos(_ photos: [Photo]) {
        Task {
            // Refresh the gallery after editing
            do {
                allPhotos = try await photoRepository.getAllPhotos()
            } catch {
                print("Error reloading photos: \(error)")
            }
        }
    }
}

// MARK: - Loading Overlay

struct LoadingOverlay: View {
    let message: String
    let progress: Double?

    init(message: String = "Loading photos...", progress: Double? = nil) {
        self.message = message
        self.progress = progress
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.4)
                .ignoresSafeArea()

            VStack(spacing: 20) {
                if let progress = progress {
                    // Show determinate progress
                    VStack(spacing: 12) {
                        ProgressView(value: progress)
                            .progressViewStyle(LinearProgressViewStyle(tint: .white))
                            .frame(width: 200)
                            .scaleEffect(1.5, anchor: .center)

                        Text("\(Int(progress * 100))%")
                            .font(.headline)
                            .foregroundColor(.white)
                    }
                } else {
                    // Show indeterminate progress
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(1.2)
                }

                Text(message)
                    .font(.subheadline)
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 250)

                // Memory usage indicator
                if progress != nil {
                    Text("Memory: \(getMemoryUsageText())")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.8))
                }
            }
            .padding(24)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.black.opacity(0.7))
            )
        }
    }

    private func getMemoryUsageText() -> String {
        let memoryMB = StorageManager.shared.getCurrentMemoryUsage()
        return "\(memoryMB) MB"
    }
}

struct EmptyGalleryPlaceholder: View {
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "photo.on.rectangle.angled")
                .font(.system(size: 64))
                .foregroundColor(.gray)

            Text("No photos yet")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Tap the + button to add photos")
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct PhotoThumbnail: View {
    let photo: Photo

    var body: some View {
        AsyncImageView(photo: photo, contentMode: .fill)
            .aspectRatio(1, contentMode: .fill)
            .clipped()
            .cornerRadius(8)
    }
}

struct PhotoEditorView: View {
    let photos: [Photo]
    let onComplete: ([Photo]) -> Void

    var body: some View {
        NavigationView {
            VStack {
                Text("Photo Editor")
                    .font(.largeTitle)
                    .padding()

                Text("\(photos.count) photo(s) selected")
                    .foregroundColor(.secondary)

                Spacer()

                Button("Done") {
                    onComplete(photos)
                }
                .buttonStyle(.borderedProminent)
                .padding()
            }
            .navigationTitle("Edit Photos")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

#Preview {
    PhotoGalleryView()
}