import SwiftUI
import Photos
import PhotosUI
import CoreData

struct ContentView: View {
    @StateObject private var kidsModeViewModel = KidsModeViewModel()
    @State private var showPINEntry = false
    @State private var tapCount = 0
    @State private var lastTapTime = Date()

    var body: some View {
        ZStack {
            if kidsModeViewModel.isKidsMode {
                // Kids Mode View
                KidsModeView(viewModel: kidsModeViewModel)
                    .ignoresSafeArea()
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
                    .ignoresSafeArea(edges: .top)
            }
        }
        .ignoresSafeArea()
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

    var body: some View {
        ZStack {
            Color(UIColor.systemBackground)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // AppHeader with category filter
                AppHeaderComponent(
                    onViewModeClick: {
                        // Switch to grid view or other view mode
                    },
                    showViewModeButton: false // Hide in Kids Mode
                ) {
                    if !viewModel.categories.isEmpty {
                        categoryFilterBar
                    }
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
        .fullScreenCover(isPresented: $showFullscreen) {
            if let index = selectedPhotoIndex {
                PhotoViewerView(
                    photos: filteredPhotos,
                    initialIndex: index,
                    viewModel: viewModel,
                    isPresented: $showFullscreen
                )
            }
        }
        .onAppear {
            // Select first category if none selected
            if viewModel.selectedCategory == nil && !viewModel.categories.isEmpty {
                viewModel.selectedCategory = viewModel.categories.first
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
    }

    private var filteredPhotos: [Photo] {
        viewModel.getPhotosForCategory(viewModel.selectedCategory?.id)
    }
}

// MARK: - Parent Mode View
struct ParentModeView: View {
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            PhotoGalleryView()
                .tabItem {
                    Label("Gallery", systemImage: "photo.on.rectangle")
                }
                .tag(0)

            CategoryManagementView()
                .tabItem {
                    Label("Categories", systemImage: "folder")
                }
                .tag(1)

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gear")
                }
                .tag(2)
        }
    }
}

// MARK: - Settings View (temporary placeholder)
struct SettingsView: View {
    @StateObject private var kidsModeViewModel = KidsModeViewModel()
    @State private var showPINSetup = false
    @State private var showPINChange = false

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
        RoundedRectangle(cornerRadius: 16)
            .fill(Color.gray.opacity(0.2))
            .aspectRatio(4/3, contentMode: .fit)
            .overlay(
                Text("📷") // Placeholder for actual photo
                    .font(.system(size: 48))
            )
            .onTapGesture(perform: onTap)
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

            // Close button
            VStack {
                HStack {
                    Spacer()
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

    var body: some View {
        ZStack {
            Color.black

            // Placeholder for actual photo
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.gray.opacity(0.3))
                .aspectRatio(4/3, contentMode: .fit)
                .overlay(
                    VStack {
                        Text("📷")
                            .font(.system(size: 72))
                        Text(photo.path)
                            .foregroundColor(.white.opacity(0.5))
                    }
                )
                .padding()
        }
    }
}

#Preview {
    ContentView()
}