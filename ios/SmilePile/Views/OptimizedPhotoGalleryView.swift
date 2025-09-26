import SwiftUI
import Combine
import Photos

/// Optimized photo gallery with virtual scrolling and memory-efficient loading
struct OptimizedPhotoGalleryView: View {
    @StateObject private var viewModel = PhotoGalleryViewModel()
    @EnvironmentObject var kidsModeViewModel: KidsModeViewModel
    @Environment(\.horizontalSizeClass) var sizeClass

    // UI State
    @State private var selectedPhotos: [Photo] = []
    @State private var showingPhotoEditor = false
    @State private var showingPhotoPicker = false
    @State private var showingPermissionError = false
    @State private var permissionErrorMessage = ""
    @State private var showingDebugInfo = false

    // Performance tracking
    @State private var scrollOffset: CGFloat = 0
    @State private var containerHeight: CGFloat = 0

    // Grid configuration
    private var columns: [GridItem] {
        let count = viewModel.itemsPerRow
        return Array(repeating: GridItem(.flexible(), spacing: 2), count: count)
    }

    private let photoRepository = PhotoRepositoryImpl()
    private let repository = CategoryRepositoryImpl()
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
                    optimizedGalleryGrid
                }
            }
            .background(Color(UIColor.systemBackground))

            // Floating Action Button
            FloatingActionButtonContainer(
                action: handleAddPhotosButtonTap,
                isPulsing: viewModel.photos.isEmpty,
                bottomPadding: 49
            )

            // Debug overlay (development only)
            if showingDebugInfo {
                debugOverlay
            }
        }
        .task {
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

    private var optimizedGalleryGrid: some View {
        GeometryReader { geometry in
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 2) {
                        ForEach(viewModel.filteredPhotos) { photo in
                            PhotoThumbnailView(photo: photo)
                                .id(photo.id)
                                .frame(height: geometry.size.width / CGFloat(viewModel.itemsPerRow) - 2)
                                .onTapGesture {
                                    selectedPhotos = [photo]
                                    showingPhotoEditor = true
                                }
                                .onAppear {
                                    viewModel.loadThumbnailIfNeeded(for: photo)
                                }
                                .onDisappear {
                                    viewModel.cancelThumbnailLoad(for: photo)
                                }
                        }
                    }
                    .padding(.horizontal, 2)
                    .padding(.bottom, 100)
                    .background(
                        GeometryReader { scrollGeometry in
                            Color.clear.preference(
                                key: ScrollOffsetPreferenceKey.self,
                                value: scrollGeometry.frame(in: .named("scroll")).minY
                            )
                        }
                    )
                }
                .coordinateSpace(name: "scroll")
                .onPreferenceChange(ScrollOffsetPreferenceKey.self) { value in
                    scrollOffset = -value
                    viewModel.handleScroll(
                        offset: scrollOffset,
                        containerHeight: geometry.size.height
                    )
                }
                .refreshable {
                    await viewModel.refreshPhotos()
                }
                .onAppear {
                    containerHeight = geometry.size.height
                }
            }
        }
    }

    private var categoryFilterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                // All Photos chip
                CategoryChip(
                    displayName: "All Photos",
                    colorHex: "#9E9E9E",
                    isSelected: viewModel.selectedCategory == nil,
                    onTap: {
                        viewModel.selectedCategory = nil
                    }
                )

                // Category chips (would need to be loaded)
                // This is simplified - in real implementation, load categories
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

    private func handleAddPhotosButtonTap() {
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