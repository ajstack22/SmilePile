import SwiftUI
import Photos
import PhotosUI
import CoreData

// Test view to verify edge-to-edge display
struct EdgeToEdgeTestView: View {
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Red bars to show if there are any black bars
                VStack(spacing: 0) {
                    // Top indicator
                    Rectangle()
                        .fill(Color.red)
                        .frame(height: 50)

                    Spacer()

                    // Bottom indicator
                    Rectangle()
                        .fill(Color.red)
                        .frame(height: 50)
                }
                .ignoresSafeArea(.all)

                // Show dimensions
                VStack {
                    Text("Edge-to-Edge Test")
                        .font(.title)
                        .padding()

                    Text("Width: \(Int(geometry.size.width))")
                    Text("Height: \(Int(geometry.size.height))")

                    Text("\nUIScreen Bounds:")
                    Text("Width: \(Int(UIScreen.main.bounds.width))")
                    Text("Height: \(Int(UIScreen.main.bounds.height))")

                    Text("\nSafe Area:")
                    Text("Top: \(Int(geometry.safeAreaInsets.top))")
                    Text("Bottom: \(Int(geometry.safeAreaInsets.bottom))")
                    Text("Leading: \(Int(geometry.safeAreaInsets.leading))")
                    Text("Trailing: \(Int(geometry.safeAreaInsets.trailing))")

                    if let window = UIApplication.shared.connectedScenes
                        .compactMap({ $0 as? UIWindowScene })
                        .first?.windows.first {
                        Text("\nWindow Safe Area:")
                        Text("Top: \(Int(window.safeAreaInsets.top))")
                        Text("Bottom: \(Int(window.safeAreaInsets.bottom))")
                    }
                }
                .foregroundColor(.white)
                .background(Color.blue.opacity(0.7))
                .cornerRadius(10)
                .padding()
            }
            .background(Color.green)
        }
        .ignoresSafeArea(.all)
    }
}

struct ContentView: View {
    @StateObject private var kidsModeViewModel = KidsModeViewModel()
    @State private var showPINEntry = false
    @State private var tapCount = 0
    @State private var lastTapTime = Date()

    var body: some View {
        ZStack {
            // Background color to fill entire screen
            Color(UIColor.systemBackground)
                .ignoresSafeArea()

            if kidsModeViewModel.isKidsMode {
                // Kids Mode View
                KidsModeView(viewModel: kidsModeViewModel)
                    .ignoresSafeArea()
                    .persistentSystemOverlays(.hidden) // Hide home indicator in Kids Mode only
                    .overlay(alignment: .topTrailing) {
                        // Invisible 3-tap area in top-right corner for exiting Kids Mode
                        Color.clear
                            .frame(width: 100, height: 100)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                handleTripleTap()
                            }
                            .padding(.top, 50) // Account for status bar
                    }
            } else {
                // Parent Mode View
                ParentModeView()
                    .ignoresSafeArea()
                    .persistentSystemOverlays(.visible) // Keep home indicator visible in Parent Mode
            }
        }
        .ignoresSafeArea()
        .environmentObject(kidsModeViewModel) // Share view model with child views
        .toastOverlay() // Toast system now integrated
        .sheet(isPresented: $showPINEntry) {
            PINEntryView(
                isPresented: $showPINEntry,
                mode: .validate,
                onSuccess: { _ in
                    kidsModeViewModel.exitKidsMode(authenticated: true)
                },
                onCancel: {
                    // User cancelled PIN entry
                }
            )
        }
        .sheet(isPresented: $kidsModeViewModel.requiresPINAuth) {
            PINEntryView(
                isPresented: $kidsModeViewModel.requiresPINAuth,
                mode: .validate,
                onSuccess: { _ in
                    kidsModeViewModel.exitKidsMode(authenticated: true)
                },
                onCancel: {
                    kidsModeViewModel.requiresPINAuth = false
                }
            )
        }
    }

    private func handleTripleTap() {
        let now = Date()

        // Reset tap count if more than 1 second has passed
        if now.timeIntervalSince(lastTapTime) > 1.0 {
            tapCount = 0
        }

        tapCount += 1
        lastTapTime = now

        if tapCount >= 3 {
            tapCount = 0
            // Check if PIN is enabled
            if PINManager.shared.isPINEnabled() {
                showPINEntry = true
            } else {
                // No PIN set, switch directly
                kidsModeViewModel.exitKidsMode(authenticated: true)
            }
        }
    }
}

// MARK: - Kids Mode View
struct KidsModeView: View {
    @ObservedObject var viewModel: KidsModeViewModel
    @State private var selectedPhotoIndex: Int?
    @State private var showFullscreen = false
    @State private var showPINEntry = false

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                Color(UIColor.systemBackground)
                    .ignoresSafeArea(.all)

                VStack(spacing: 0) {
                    // Respect top safe area for camera/notch
                    Color.clear
                        .frame(height: geometry.safeAreaInsets.top)

                    // Kids Mode: No header, just category filter at top
                    if !viewModel.categories.isEmpty {
                        categoryFilterBar
                            .background(Color(UIColor.systemBackground))
                    }

                    // Photo Gallery
                    if viewModel.photos.isEmpty {
                        EmptyGalleryView()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        photoGallery
                    }
                }
            }
        }
        .ignoresSafeArea(.container, edges: .bottom) // Only ignore bottom safe area for tab bar area
        .fullScreenCover(isPresented: $showFullscreen) {
            if let index = selectedPhotoIndex {
                EnhancedPhotoViewer(
                    isPresented: $showFullscreen,
                    initialPhotoIndex: index
                )
                .environmentObject(viewModel)
            }
        }
        .gesture(
            LongPressGesture(minimumDuration: 3.0)
                .onEnded { _ in
                    // Check if PIN is enabled
                    if PINManager.shared.isPINEnabled() {
                        showPINEntry = true
                    } else {
                        // No PIN set, exit directly
                        viewModel.exitKidsMode(authenticated: true)
                    }
                }
        )
        .sheet(isPresented: $showPINEntry) {
            PINEntryView(
                isPresented: $showPINEntry,
                mode: .validate,
                onSuccess: { _ in
                    viewModel.exitKidsMode(authenticated: true)
                },
                onCancel: {
                    // User cancelled PIN entry
                }
            )
        }
        .onAppear {
            // Always select first category in Kids Mode (never show "All Photos")
            if !viewModel.categories.isEmpty {
                let firstCategory = viewModel.categories.first!
                viewModel.selectCategory(firstCategory)
                // Show initial toast after a brief delay to ensure view is ready
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    ToastManager.shared.showCategoryToast(firstCategory)
                }
            }
        }
    }

    private var categoryFilterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(viewModel.categories) { category in
                    CategoryChip(
                        displayName: category.displayName,
                        colorHex: category.colorHex ?? "#4CAF50",
                        isSelected: viewModel.selectedCategory?.id == category.id,
                        onTap: {
                            viewModel.selectCategory(category)
                            ToastManager.shared.showCategoryToast(category)
                        }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
    }

    private var photoGallery: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(Array(filteredPhotos.enumerated()), id: \.element.id) { index, photo in
                    PhotoCard(photo: photo) {
                        selectedPhotoIndex = index
                        showFullscreen = true
                    }
                }
            }
            .padding()
        }
        .gesture(
            DragGesture()
                .onEnded { value in
                    // Horizontal swipe detection with 150px threshold
                    if abs(value.translation.width) > viewModel.swipeThreshold && abs(value.translation.height) < 100 {
                        if value.translation.width > 0 {
                            viewModel.navigateToPreviousCategory()
                        } else {
                            viewModel.navigateToNextCategory()
                        }
                    }
                }
        )
    }

    private var filteredPhotos: [Photo] {
        viewModel.getPhotosForCategory(viewModel.selectedCategory?.id)
    }
}

// MARK: - Parent Mode View
struct ParentModeView: View {
    @State private var selectedTab = 0
    @AppStorage("useOptimizedGallery") private var useOptimizedGallery = true

    var body: some View {
        TabView(selection: $selectedTab) {
            Group {
                if useOptimizedGallery {
                    OptimizedPhotoGalleryView()
                        .tabItem {
                            Label("Gallery", systemImage: "photo.on.rectangle")
                        }
                        .tag(0)
                } else {
                    PhotoGalleryView()
                        .tabItem {
                            Label("Gallery", systemImage: "photo.on.rectangle")
                        }
                        .tag(0)
                }
            }

            CategoryManagementView()
                .tabItem {
                    Label("Categories", systemImage: "folder")
                }
                .tag(1)

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
                .tag(2)
        }
        .ignoresSafeArea()
        .accentColor(Color(red: 76/255, green: 175/255, blue: 80/255))
    }
}

// MARK: - Settings View (temporary placeholder)
struct SettingsView: View {
    @StateObject private var kidsModeViewModel = KidsModeViewModel()
    @State private var showPINSetup = false
    @State private var showPINChange = false
    @AppStorage("useOptimizedGallery") private var useOptimizedGallery = true
    @AppStorage("showPerformanceOverlay") private var showPerformanceOverlay = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Kids Mode") {
                    Toggle("Kids Mode", isOn: $kidsModeViewModel.isKidsMode)
                        .onChange(of: kidsModeViewModel.isKidsMode) { newValue in
                            if newValue {
                                kidsModeViewModel.toggleKidsMode()
                            } else {
                                kidsModeViewModel.exitKidsMode(authenticated: true)
                            }
                        }
                }

                Section("Performance") {
                    Toggle("Use Optimized Gallery", isOn: $useOptimizedGallery)
                    Toggle("Show Performance Overlay", isOn: $showPerformanceOverlay)

                    HStack {
                        Text("Memory Usage")
                        Spacer()
                        Text("\(MemoryMonitor.shared.currentMemoryUsageMB)MB")
                            .foregroundColor(.secondary)
                    }

                    Button("Clear Image Cache") {
                        Task {
                            await OptimizedImageCache.shared.clearCache()
                        }
                    }
                    .foregroundColor(.red)
                }

                Section("Security") {
                    if PINManager.shared.isPINEnabled() {
                        Button("Change PIN") {
                            showPINChange = true
                        }

                        Button("Remove PIN") {
                            try? PINManager.shared.clearPIN()
                        }
                        .foregroundColor(.red)
                    } else {
                        Button("Set PIN") {
                            showPINSetup = true
                        }
                    }
                }

                Section("About") {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.0")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("Settings")
        }
        .sheet(isPresented: $showPINSetup) {
            PINEntryView(
                isPresented: $showPINSetup,
                mode: .setup,
                onSuccess: { pin in
                    try? PINManager.shared.setPIN(pin)
                },
                onCancel: {}
            )
        }
        .sheet(isPresented: $showPINChange) {
            PINEntryView(
                isPresented: $showPINChange,
                mode: .change,
                onSuccess: { pin in
                    try? PINManager.shared.setPIN(pin)
                },
                onCancel: {}
            )
        }
    }
}

// MARK: - Supporting Views
struct PhotoCard: View {
    let photo: Photo
    let onTap: () -> Void

    var body: some View {
        PhotoCardWithImage(photo: photo, onTap: onTap)
    }
}

struct EmptyGalleryView: View {
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "camera")
                .font(.system(size: 64))
                .foregroundColor(.gray)

            Text("No photos yet!")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Ask a parent to add some photos")
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct PhotoViewerView: View {
    let photos: [Photo]
    let initialIndex: Int
    @ObservedObject var viewModel: KidsModeViewModel
    @Binding var isPresented: Bool

    @State private var currentIndex: Int
    @State private var dragOffset: CGSize = .zero
    @State private var showingShareSheet = false

    init(photos: [Photo], initialIndex: Int, viewModel: KidsModeViewModel, isPresented: Binding<Bool>) {
        self.photos = photos
        self.initialIndex = initialIndex
        self.viewModel = viewModel
        self._isPresented = isPresented
        self._currentIndex = State(initialValue: initialIndex)
    }

    var body: some View {
        ZStack {
            Color.black
                .edgesIgnoringSafeArea(.all)

            TabView(selection: $currentIndex) {
                ForEach(Array(photos.enumerated()), id: \.offset) { index, photo in
                    PhotoViewerPage(photo: photo)
                        .tag(index)
                }
            }
            .tabViewStyle(PageTabViewStyle(indexDisplayMode: .automatic))
            .gesture(
                DragGesture()
                    .onChanged { value in
                        dragOffset = value.translation
                    }
                    .onEnded { value in
                        // Horizontal swipe for category navigation
                        if abs(value.translation.width) > 100 {
                            if value.translation.width > 0 {
                                // Swipe right - previous category
                                navigateToPreviousCategory()
                            } else {
                                // Swipe left - next category
                                navigateToNextCategory()
                            }
                        }
                        dragOffset = .zero
                    }
            )

            // Top controls
            VStack {
                HStack {
                    // Share button
                    Button(action: {
                        if !photos.isEmpty && currentIndex < photos.count {
                            showingShareSheet = true
                        }
                    }) {
                        Image(systemName: "square.and.arrow.up")
                            .font(.title2)
                            .foregroundColor(.white.opacity(0.9))
                            .padding(10)
                            .background(Circle().fill(Color.black.opacity(0.5)))
                    }
                    .padding()

                    Spacer()

                    // Close button
                    Button(action: {
                        isPresented = false
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title)
                            .foregroundColor(.white.opacity(0.7))
                            .background(Circle().fill(Color.black.opacity(0.5)))
                    }
                    .padding()
                }
                Spacer()
            }
        }
        .sheet(isPresented: $showingShareSheet) {
            if currentIndex < photos.count {
                ShareSheetView(items: [loadImageForSharing(photos[currentIndex])])
            }
        }
    }

    private func loadImageForSharing(_ photo: Photo) -> Any {
        // Try to load the actual image for sharing
        if photo.isFromAssets {
            return UIImage(named: photo.path) ?? UIImage()
        } else {
            let fileURL = URL(fileURLWithPath: photo.path)
            if let imageData = try? Data(contentsOf: fileURL),
               let image = UIImage(data: imageData) {
                return image
            }
        }
        return UIImage()
    }

    private func navigateToPreviousCategory() {
        guard let currentCategory = viewModel.selectedCategory,
              let currentIndex = viewModel.categories.firstIndex(where: { $0.id == currentCategory.id }) else { return }

        let previousIndex = currentIndex > 0 ? currentIndex - 1 : viewModel.categories.count - 1
        viewModel.selectCategory(viewModel.categories[previousIndex])
    }

    private func navigateToNextCategory() {
        guard let currentCategory = viewModel.selectedCategory,
              let currentIndex = viewModel.categories.firstIndex(where: { $0.id == currentCategory.id }) else { return }

        let nextIndex = (currentIndex + 1) % viewModel.categories.count
        viewModel.selectCategory(viewModel.categories[nextIndex])
    }
}

struct PhotoViewerPage: View {
    let photo: Photo
    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    var body: some View {
        ZStack {
            Color.black

            AsyncImageView(photo: photo, contentMode: .fit)
                .scaleEffect(scale)
                .offset(offset)
                .gesture(
                    SimultaneousGesture(
                        MagnificationGesture()
                            .onChanged { value in
                                let delta = value / lastScale
                                lastScale = value
                                scale = min(max(scale * delta, 1), 4)
                            }
                            .onEnded { _ in
                                lastScale = 1.0
                                withAnimation(.spring()) {
                                    if scale < 1 {
                                        scale = 1
                                        offset = .zero
                                    }
                                }
                            },
                        DragGesture()
                            .onChanged { value in
                                if scale > 1 {
                                    offset = CGSize(
                                        width: lastOffset.width + value.translation.width,
                                        height: lastOffset.height + value.translation.height
                                    )
                                }
                            }
                            .onEnded { _ in
                                lastOffset = offset
                            }
                    )
                )
                .onTapGesture(count: 2) {
                    withAnimation(.spring()) {
                        if scale > 1 {
                            scale = 1
                            offset = .zero
                            lastOffset = .zero
                        } else {
                            scale = 2
                        }
                    }
                }
                .padding()
        }
    }
}

#Preview {
    ContentView()
}