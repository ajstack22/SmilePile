import SwiftUI
import Combine
import Photos
import os.log

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
private struct OptimizedPhotoStackView: View {
    let photos: [Photo]
    let onPhotoClick: (Photo) -> Void
    let onEditClick: ((Photo) -> Void)?
    let onDeleteClick: ((Photo) -> Void)?

    var body: some View {
        if photos.isEmpty {
            OptimizedEmptyPhotoStackState()
        } else {
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(photos) { photo in
                        OptimizedPhotoStackItem(
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

private struct OptimizedPhotoStackItem: View {
    let photo: Photo
    let onPhotoClick: () -> Void
    let onEditClick: (() -> Void)?
    let onDeleteClick: (() -> Void)?

    var body: some View {
        VStack(spacing: 0) {
            // Photo card
            Group {
                // Check if photo exists before trying to display
                if FileManager.default.fileExists(atPath: photo.path) {
                    AsyncImage(url: URL(fileURLWithPath: photo.path)) { phase in
                        switch phase {
                        case .empty:
                            Rectangle()
                                .fill(Color.gray.opacity(0.2))
                                .overlay(ProgressView())
                        case .success(let image):
                            image
                                .resizable()
                                .aspectRatio(4/3, contentMode: .fill)
                                .clipped()
                        case .failure(let error):
                            Rectangle()
                                .fill(Color.gray.opacity(0.2))
                                .overlay(
                                    VStack {
                                        Image(systemName: "exclamationmark.triangle")
                                            .foregroundColor(.gray)
                                        Text("Failed to load")
                                            .font(.caption)
                                            .foregroundColor(.gray)
                                    }
                                )
                                .onAppear {
                                    let logger = Logger(subsystem: "com.smilepile", category: "PhotoStackItem")
                                    logger.error("Failed to load photo at path: \(photo.path, privacy: .public)")
                                    logger.error("Error: \(error.localizedDescription, privacy: .public)")
                                }
                        @unknown default:
                            EmptyView()
                        }
                    }
                } else {
                    // Photo file doesn't exist
                    Rectangle()
                        .fill(Color.gray.opacity(0.2))
                        .overlay(
                            VStack {
                                Image(systemName: "photo.slash")
                                    .foregroundColor(.gray)
                                Text("Photo not found")
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }
                        )
                        .onAppear {
                            let logger = Logger(subsystem: "com.smilepile", category: "PhotoStackItem")
                            logger.error("Photo file not found at path: \(photo.path, privacy: .public)")
                        }
                }
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(4/3, contentMode: .fit)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .onTapGesture { onPhotoClick() }
        }
        .padding(.vertical, 4)
    }
}

private struct OptimizedEmptyPhotoStackState: View {
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
    @State private var pendingEditPhotos: [Photo] = []

    // Performance tracking
    @State private var scrollOffset: CGFloat = 0
    @State private var containerHeight: CGFloat = 0
    @State private var categories: [Category] = []

    // Stack layout - no grid needed

    private let photoRepository = PhotoRepositoryImpl()
    private let repository = CategoryRepositoryImpl.shared
    @StateObject private var permissionManager = PhotoLibraryPermissionManager.shared
    private let storage = SimplePhotoStorage.shared
    private let logger = Logger(subsystem: "com.smilepile", category: "OptimizedPhotoGalleryView")

    var body: some View {
        ZStack {
            mainContentView
            floatingActionButton

            if showingDebugInfo {
                debugOverlay
            }
        }
        .task {
            await initializeView()
        }
        .sheet(isPresented: $showingPhotoEditor) {
            photoEditorSheet
        }
        .onChange(of: showingPhotoEditor) { newValue in
            handlePhotoEditorDismissal(newValue)
        }
        .sheet(isPresented: $showingCategorySelection) {
            categorySelectionSheet
        }
        .fullScreenCover(isPresented: $showingPhotoPicker) {
            photoPickerView
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
        OptimizedPhotoStackView(
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
        .gesture(
            DragGesture()
                .onEnded { value in
                    handleSwipeGesture(value)
                }
        )
    }

    private var categoryFilterBar: some View {
        Group {
            if categories.isEmpty {
                // Show loading or placeholder when no categories
                HStack {
                    Text("Loading categories...")
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                }
                .frame(maxWidth: .infinity)
                .background(Color(UIColor.secondarySystemBackground))
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        // Category chips - a category must always be selected
                        ForEach(categories) { category in
                    CategoryChip(
                        displayName: category.displayName,
                        colorHex: category.colorHex ?? "#4CAF50",
                        isSelected: viewModel.selectedCategory?.id == category.id,
                        onTap: {
                            logger.info("Category tapped: \(category.displayName, privacy: .public)")
                            viewModel.selectedCategory = category
                            logger.info("Selected category is now: \(viewModel.selectedCategory?.displayName ?? "None", privacy: .public)")
                        }
                        )
                    }
                }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }
            }
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

            if viewModel.selectedCategory == nil {
                Text("Select a category above")
                    .font(.title2)
                    .fontWeight(.semibold)

                Text("Then tap + to add photos")
                    .foregroundColor(.secondary)
            } else {
                Text("No photos in \(viewModel.selectedCategory?.displayName ?? "this category")")
                    .font(.title2)
                    .fontWeight(.semibold)

                Text("Tap the + button to add photos")
                    .foregroundColor(.secondary)
            }
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
        logger.info("Starting to load categories...")
        do {
            categories = try await repository.getAllCategories()
            logger.info("Loaded \(categories.count, privacy: .public) categories")

            // DON'T auto-initialize categories - let onboarding handle it for first-time users
            if categories.isEmpty {
                logger.info("No categories found - onboarding may not be complete yet")
                // Use empty array - onboarding will create categories
            }

            // Debug: log all category names
            for category in categories {
                logger.debug("Category: \(category.displayName, privacy: .public) (id: \(category.id, privacy: .public))")
            }

            // Don't auto-select a category - let user choose when adding photos
            logger.info("Categories loaded, no auto-selection to ensure user picks category")
        } catch {
            logger.error("Failed to load categories: \(error.localizedDescription, privacy: .public)")

            // DON'T auto-initialize - onboarding will handle category creation
            logger.info("Error loading categories - may be first launch, onboarding will create them")
            categories = []  // Empty array - onboarding will populate
        }
    }

    private func deletePhoto(_ photo: Photo) async {
        do {
            try await photoRepository.deletePhoto(photo)
            await viewModel.loadPhotos()
        } catch {
            logger.error("Failed to delete photo: \(error.localizedDescription, privacy: .public)")
        }
    }

    private func handleAddPhotosButtonTap() {
        logAddPhotosAttempt()

        if viewModel.selectedCategory == nil {
            showCategorySelection()
        } else {
            proceedWithPhotoSelection()
        }
    }

    private func logAddPhotosAttempt() {
        logger.info("Add photos button tapped")
        logger.info("Selected category: \(viewModel.selectedCategory?.displayName ?? "None", privacy: .public)")
        logger.info("Categories available: \(categories.count, privacy: .public)")
    }

    private func showCategorySelection() {
        logger.info("No category selected, showing selection sheet")
        showingCategorySelection = true
    }

    private func proceedWithPhotoSelection() {
        logger.info("Category selected: \(viewModel.selectedCategory!.displayName, privacy: .public), opening photo picker")
        permissionManager.checkCurrentAuthorizationStatus()
        handlePhotoLibraryPermission()
    }

    private func handlePhotoLibraryPermission() {
        switch permissionManager.authorizationStatus {
        case .notDetermined, .authorized, .limited:
            showingPhotoPicker = true
        case .denied:
            showPermissionError(message: "Photo library access is required to add photos. Please enable it in Settings.")
        case .restricted:
            showPermissionError(message: "Photo library access is restricted on this device.")
        @unknown default:
            showingPhotoPicker = true
        }
    }

    private func showPermissionError(message: String) {
        permissionErrorMessage = message
        showingPermissionError = true
    }

    private func handleSelectedPhotos(_ photos: [Photo]) {
        // Navigate to photo editor with selected photos (matching Android behavior)
        logger.info("Opening photo editor with \(photos.count, privacy: .public) photos")

        // Store photos directly to preserve their IDs for proper category updates
        pendingEditPhotos = photos
        showingPhotoEditor = true
    }

    private func handleEditedPhotos(_ photos: [Photo]) {
        Task {
            await viewModel.refreshPhotos()
        }
    }

    // MARK: - Extracted Views

    private var mainContentView: some View {
        VStack(spacing: 0) {
            AppHeaderComponent(
                onViewModeClick: {
                    kidsModeViewModel.toggleKidsMode()
                },
                showViewModeButton: true
            ) {
                categoryFilterBar
            }

            contentBasedOnState
        }
        .background(Color(UIColor.systemBackground))
    }

    private var contentBasedOnState: some View {
        Group {
            if viewModel.isLoading && viewModel.photos.isEmpty {
                loadingView
            } else if viewModel.photos.isEmpty {
                emptyStateView
            } else {
                photoStackView
            }
        }
    }

    private var floatingActionButton: some View {
        FloatingActionButton(
            action: handleAddPhotosButtonTap,
            isPulsing: viewModel.photos.isEmpty,
            backgroundColor: Color(red: 74/255, green: 144/255, blue: 226/255),
            iconName: "photo.badge.plus"
        )
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
        .padding(.trailing, 16)
        .padding(.bottom, 16)
    }

    @ViewBuilder
    private var photoEditorSheet: some View {
        if !selectedPhotos.isEmpty {
            PhotoEditView(photos: selectedPhotos, initialCategoryId: viewModel.selectedCategory?.id ?? 1)
        } else if !pendingEditPhotos.isEmpty {
            PhotoEditView(photos: pendingEditPhotos, initialCategoryId: viewModel.selectedCategory?.id ?? 1)
        }
    }

    private var categorySelectionSheet: some View {
        CategorySelectionSheet(
            categories: categories,
            onSelectCategory: handleCategorySelection,
            onCancel: {
                showingCategorySelection = false
            }
        )
    }

    private var photoPickerView: some View {
        EnhancedPhotoPickerView(
            isPresented: $showingPhotoPicker,
            categoryId: viewModel.selectedCategory?.id ?? 1,
            onPhotosSelected: handleSelectedPhotos,
            onCancel: {}
        )
    }

    // MARK: - Helper Methods

    private func initializeView() async {
        storage.checkStorageHealth()
        await loadCategories()
        await viewModel.loadPhotos()
    }

    private func handlePhotoEditorDismissal(_ isShowing: Bool) {
        if !isShowing {
            selectedPhotos = []
            pendingEditPhotos = []
            Task {
                await viewModel.refreshPhotos()
            }
        }
    }

    private func handleCategorySelection(_ category: Category) {
        viewModel.selectedCategory = category
        showingCategorySelection = false
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            showingPhotoPicker = true
        }
    }

    private func handleSwipeGesture(_ gesture: DragGesture.Value) {
        let swipeThreshold: CGFloat = 150
        guard !categories.isEmpty else { return }

        let currentIndex = getCurrentCategoryIndex()

        if gesture.translation.width > swipeThreshold {
            handleSwipeRight(currentIndex: currentIndex)
        } else if gesture.translation.width < -swipeThreshold {
            handleSwipeLeft(currentIndex: currentIndex)
        }
    }

    private func getCurrentCategoryIndex() -> Int {
        guard let selectedCategory = viewModel.selectedCategory else { return -1 }
        return categories.firstIndex(where: { $0.id == selectedCategory.id }) ?? -1
    }

    private func handleSwipeRight(currentIndex: Int) {
        if currentIndex > 0 {
            viewModel.selectedCategory = categories[currentIndex - 1]
            logCategorySwipe()
        } else if currentIndex == -1 && !categories.isEmpty {
            viewModel.selectedCategory = categories.last
            logCategorySwipe()
        }
    }

    private func handleSwipeLeft(currentIndex: Int) {
        if currentIndex >= 0 && currentIndex < categories.count - 1 {
            viewModel.selectedCategory = categories[currentIndex + 1]
            logCategorySwipe()
        } else if currentIndex == -1 && !categories.isEmpty {
            viewModel.selectedCategory = categories.first
            logCategorySwipe()
        }
    }

    private func logCategorySwipe() {
        logger.info("Swiped to category: \(viewModel.selectedCategory?.displayName ?? "None", privacy: .public)")
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