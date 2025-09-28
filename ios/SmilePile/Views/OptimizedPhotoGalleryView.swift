import SwiftUI
import Combine
import Photos

/// Optimized photo gallery with virtual scrolling and memory-efficient loading
// MARK: - Category Selection Sheet
struct CategorySelectionSheet: View {
    let categories: [Category]
    let onSelectCategory: (Category) -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                Text("Select a Category")
                    .font(.headline)
                    .padding()

                List(categories) { category in
                    Button(action: {
                        onSelectCategory(category)
                    }) {
                        HStack {
                            Circle()
                                .fill(Color(hex: category.colorHex ?? "#4CAF50") ?? Color.green)
                                .frame(width: 24, height: 24)

                            Text(category.displayName)
                                .foregroundColor(.primary)

                            Spacer()
                        }
                        .padding(.vertical, 8)
                    }
                }
                .listStyle(PlainListStyle())
            }
            .navigationBarItems(
                leading: Button("Cancel") {
                    onCancel()
                }
            )
        }
    }
}

// MARK: - Photo Stack View
struct PhotoStackView: View {
    let photos: [Photo]
    let onPhotoClick: (Photo) -> Void
    let onEditClick: ((Photo) -> Void)?
    let onDeleteClick: ((Photo) -> Void)?

    var body: some View {
        if photos.isEmpty {
            EmptyPhotoStackState()
        } else {
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(photos) { photo in
                        PhotoStackItem(
                            photo: photo,
                            onPhotoClick: { onPhotoClick(photo) },
                            onEditClick: { onEditClick?(photo) },
                            onDeleteClick: { onDeleteClick?(photo) }
                        )
                    }
                }
                .padding(16)
            }
        }
    }
}

struct PhotoStackItem: View {
    let photo: Photo
    let onPhotoClick: () -> Void
    let onEditClick: (() -> Void)?
    let onDeleteClick: (() -> Void)?

    var body: some View {
        VStack(spacing: 0) {
            // Photo card
            AsyncImage(url: URL(fileURLWithPath: photo.path)) { phase in
                switch phase {
                case .empty:
                    Rectangle()
                        .fill(Color.gray.opacity(0.2))
                        .overlay(ProgressView())
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(maxHeight: 300)
                        .clipped()
                case .failure(_):
                    Rectangle()
                        .fill(Color.gray.opacity(0.2))
                        .overlay(
                            Image(systemName: "exclamationmark.triangle")
                                .foregroundColor(.gray)
                        )
                @unknown default:
                    EmptyView()
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 300)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .onTapGesture { onPhotoClick() }
        }
        .padding(.vertical, 4)
    }
}

struct EmptyPhotoStackState: View {
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "photo.stack")
                .font(.system(size: 64))
                .foregroundColor(.gray)
            Text("No photos in this category")
                .font(.title2)
                .fontWeight(.semibold)
            Text("Add photos to get started")
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}

struct OptimizedPhotoGalleryView: View {
    @StateObject private var viewModel = PhotoGalleryViewModel()
    @EnvironmentObject var kidsModeViewModel: KidsModeViewModel
    @Environment(\.horizontalSizeClass) var sizeClass

    // UI State
    @State private var selectedPhotos: [Photo] = []
    @State private var showingPhotoEditor = false
    @State private var showingPhotoPicker = false
    @State private var showingCategorySelection = false
    @State private var showingPermissionError = false
    @State private var permissionErrorMessage = ""
    @State private var showingDebugInfo = false

    // Performance tracking
    @State private var scrollOffset: CGFloat = 0
    @State private var containerHeight: CGFloat = 0
    @State private var categories: [Category] = []

    // Stack layout - no grid needed

    private let photoRepository = PhotoRepositoryImpl()
    private let repository = CategoryRepositoryImpl.shared
    @StateObject private var permissionManager = PhotoLibraryPermissionManager.shared

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // Header with categories
                AppHeaderComponent(
                    onViewModeClick: {
                        kidsModeViewModel.toggleKidsMode()
                    },
                    showViewModeButton: true
                ) {
                    if !viewModel.photos.isEmpty {
                        categoryFilterBar
                    }
                }

                // Main content
                if viewModel.isLoading && viewModel.photos.isEmpty {
                    loadingView
                } else if viewModel.photos.isEmpty {
                    emptyStateView
                } else {
                    photoStackView
                }
            }
            .background(Color(UIColor.systemBackground))

            // Floating Action Button - positioned like Categories page
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    FloatingActionButton(
                        action: handleAddPhotosButtonTap,
                        isPulsing: viewModel.photos.isEmpty
                    )
                    .padding(.trailing, 16)
                    .padding(.bottom, 16)
                }
            }

            // Debug overlay (development only)
            if showingDebugInfo {
                debugOverlay
            }
        }
        .task {
            await loadCategories()
            await viewModel.loadPhotos()
        }
        .sheet(isPresented: $showingPhotoEditor) {
            if !selectedPhotos.isEmpty {
                PhotoEditView(photos: selectedPhotos, initialCategoryId: viewModel.selectedCategory?.id ?? 1)
                    .onDisappear {
                        Task {
                            await viewModel.refreshPhotos()
                        }
                    }
            }
        }
        .sheet(isPresented: $showingCategorySelection) {
            CategorySelectionSheet(
                categories: categories,
                onSelectCategory: { category in
                    viewModel.selectedCategory = category
                    showingCategorySelection = false
                    // After selecting category, open photo picker
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                        showingPhotoPicker = true
                    }
                },
                onCancel: {
                    showingCategorySelection = false
                }
            )
        }
        .fullScreenCover(isPresented: $showingPhotoPicker) {
            EnhancedPhotoPickerView(
                isPresented: $showingPhotoPicker,
                categoryId: viewModel.selectedCategory?.id ?? 1,
                onPhotosSelected: handleSelectedPhotos,
                onCancel: {}
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
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didReceiveMemoryWarningNotification)) { _ in
            Task {
                await viewModel.clearCache()
            }
        }
    }

    // MARK: - Views

    private var photoStackView: some View {
        PhotoStackView(
            photos: viewModel.filteredPhotos,
            onPhotoClick: { photo in
                selectedPhotos = [photo]
                showingPhotoEditor = true
            },
            onEditClick: { photo in
                selectedPhotos = [photo]
                showingPhotoEditor = true
            },
            onDeleteClick: { photo in
                Task {
                    await deletePhoto(photo)
                }
            }
        )
        .padding(.bottom, 100) // Space for FAB
    }

    private var categoryFilterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                // Category chips - a category must always be selected
                ForEach(categories) { category in
                    CategoryChip(
                        displayName: category.displayName,
                        colorHex: category.colorHex ?? "#4CAF50",
                        isSelected: viewModel.selectedCategory?.id == category.id,
                        onTap: {
                            viewModel.selectedCategory = category
                        }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }

    private var loadingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)

            if viewModel.loadingProgress > 0 {
                ProgressView(value: viewModel.loadingProgress)
                    .frame(width: 200)

                Text("\(Int(viewModel.loadingProgress * 100))%")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Text("Loading photos...")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var emptyStateView: some View {
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

    private var debugOverlay: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Debug Info")
                .font(.headline)

            Text("Photos: \(viewModel.photos.count)")
            Text("Visible: \(viewModel.visiblePhotos.count)")
            Text("Memory: \(viewModel.memoryUsageMB)MB")
            Text("Scroll: \(Int(scrollOffset))pt")

            Button("Clear Cache") {
                Task {
                    await viewModel.clearCache()
                }
            }
            .buttonStyle(.bordered)

            Button("Close") {
                showingDebugInfo.toggle()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(UIColor.systemBackground))
                .shadow(radius: 5)
        )
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
        .padding()
    }

    // MARK: - Actions

    private func loadCategories() async {
        do {
            categories = try await repository.getAllCategories()

            // If no categories exist, initialize defaults
            if categories.isEmpty {
                print("OptimizedPhotoGalleryView: No categories found, initializing defaults...")
                try await repository.initializeDefaultCategories()
                categories = try await repository.getAllCategories()
            }

            if !categories.isEmpty && viewModel.selectedCategory == nil {
                viewModel.selectedCategory = categories.first
            }
        } catch {
            print("OptimizedPhotoGalleryView: Failed to load categories: \(error)")

            // Use default categories as fallback and save them to CoreData
            let defaultCategories = Category.getDefaultCategories()

            // Try to save defaults to CoreData
            do {
                try await repository.initializeDefaultCategories()
                categories = try await repository.getAllCategories()
            } catch {
                print("OptimizedPhotoGalleryView: Failed to save default categories: \(error)")
                // Use in-memory defaults as last resort
                categories = defaultCategories
            }

            if !categories.isEmpty && viewModel.selectedCategory == nil {
                viewModel.selectedCategory = categories.first
            }
        }
    }

    private func deletePhoto(_ photo: Photo) async {
        do {
            try await photoRepository.deletePhoto(photo)
            await viewModel.loadPhotos()
        } catch {
            print("Failed to delete photo: \(error)")
        }
    }

    private func handleAddPhotosButtonTap() {
        // First check if a category is selected
        if viewModel.selectedCategory == nil {
            // If no category selected, show category selection
            showingCategorySelection = true
        } else {
            // If category is selected, proceed with photo picker
            permissionManager.checkCurrentAuthorizationStatus()

            switch permissionManager.authorizationStatus {
            case .notDetermined, .authorized, .limited:
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
    }

    private func handleSelectedPhotos(_ photos: [Photo]) {
        Task {
            viewModel.isLoading = true
            do {
                for photo in photos {
                    _ = try await photoRepository.insertPhoto(photo)
                }
                await viewModel.loadPhotos()
            } catch {
                print("Error saving photos: \(error)")
            }
            viewModel.isLoading = false

            if !photos.isEmpty {
                selectedPhotos = photos
                showingPhotoEditor = true
            }
        }
    }

    private func handleEditedPhotos(_ photos: [Photo]) {
        Task {
            await viewModel.refreshPhotos()
        }
    }
}

// MARK: - Preference Key for Scroll Tracking
struct ScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

// MARK: - Performance Monitoring View
struct PerformanceMonitorView: View {
    let metrics: PerformanceMetrics

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Label("Performance", systemImage: "speedometer")
                .font(.caption.bold())

            Text(metrics.description)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding(8)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(UIColor.secondarySystemBackground))
        )
    }
}

// MARK: - Preview
struct OptimizedPhotoGalleryView_Previews: PreviewProvider {
    static var previews: some View {
        OptimizedPhotoGalleryView()
            .environmentObject(KidsModeViewModel())
    }
}