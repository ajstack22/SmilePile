import SwiftUI
import UniformTypeIdentifiers

struct PhotoGalleryView: View {
    @EnvironmentObject var kidsModeViewModel: KidsModeViewModel
    @State private var selectedCategory: Category?
    @State private var categories: [Category] = []
    @State private var selectedPhotos: [Photo] = []
    @State private var showingPhotoEditor = false
    @State private var showingImportPicker = false
    @State private var importedImageURLs: [URL] = []
    @State private var allPhotos: [Photo] = []

    private let repository = CategoryRepositoryImpl()
    private let photoRepository = PhotoRepositoryImpl()

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // AppHeader with SmilePile logo and eye button
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
                                PhotoThumbnail(photo: photo)
                                    .onTapGesture {
                                        selectedPhotos = [photo]
                                        showingPhotoEditor = true
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
                    showingImportPicker = true
                },
                isPulsing: allPhotos.isEmpty,
                bottomPadding: 49 // Standard iOS tab bar height
            )
        }
        .onAppear {
            loadCategories()
            loadPhotos()
        }
        .sheet(isPresented: $showingPhotoEditor) {
            if !selectedPhotos.isEmpty {
                PhotoEditorView(photos: selectedPhotos) { editedPhotos in
                    // Handle edited photos
                    showingPhotoEditor = false
                }
            }
        }
        .fileImporter(
            isPresented: $showingImportPicker,
            allowedContentTypes: [.image],
            allowsMultipleSelection: true
        ) { result in
            switch result {
            case .success(let urls):
                importedImageURLs = urls
                selectedPhotos = urls.map { url in
                    Photo(
                        path: url.absoluteString,
                        categoryId: selectedCategory?.id ?? 0
                    )
                }
                showingPhotoEditor = true
            case .failure(let error):
                print("Import error: \(error)")
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
                allPhotos = try await photoRepository.getAllPhotos()
            } catch {
                print("Failed to load photos: \(error)")
                allPhotos = []
            }
        }
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