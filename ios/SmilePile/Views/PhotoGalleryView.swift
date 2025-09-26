import SwiftUI
import UniformTypeIdentifiers

struct PhotoGalleryView: View {
    @State private var selectedCategory: Category?
    @State private var categories: [Category] = []
    @State private var isKidsMode = false
    @State private var selectedPhotos: [Photo] = []
    @State private var showingPhotoEditor = false
    @State private var showingImportPicker = false
    @State private var importedImageURLs: [URL] = []

    private let repository = CategoryRepositoryImpl()
    private let photoRepository = PhotoRepositoryImpl()

    var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // AppHeader with SmilePile logo and eye button
                AppHeaderComponent(
                    onViewModeClick: {
                        isKidsMode.toggle()
                    },
                    showViewModeButton: true
                ) {
                    if !categories.isEmpty {
                        categoryFilterBar
                    }
                }

                // Photo Grid
                ScrollView {
                    LazyVGrid(columns: [
                        GridItem(.flexible()),
                        GridItem(.flexible()),
                        GridItem(.flexible())
                    ], spacing: 2) {
                        ForEach(mockPhotos) { photo in
                            PhotoThumbnail(photo: photo)
                                .onTapGesture {
                                    selectedPhotos = [photo]
                                    showingPhotoEditor = true
                                }
                        }
                    }
                    .padding(.bottom, 80) // Space for FAB
                }
            }
            .ignoresSafeArea(edges: .top)

            // Floating Action Button
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Button(action: {
                        showingImportPicker = true
                    }) {
                        Image(systemName: "plus")
                            .font(.title.weight(.semibold))
                            .foregroundColor(.white)
                            .frame(width: 56, height: 56)
                            .background(Color(hex: "#E91E63") ?? .pink)
                            .clipShape(Circle())
                            .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 4)
                    }
                    .padding()
                }
            }
        }
        .onAppear {
            loadCategories()
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

    // Mock photos for testing
    private var mockPhotos: [Photo] {
        return (1...12).map { index in
            Photo(
                path: "photo_\(index)",
                categoryId: selectedCategory?.id ?? 0
            )
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
}

struct PhotoThumbnail: View {
    let photo: Photo

    var body: some View {
        Rectangle()
            .fill(Color.gray.opacity(0.3))
            .aspectRatio(1, contentMode: .fit)
            .overlay(
                Image(systemName: "photo")
                    .font(.largeTitle)
                    .foregroundColor(.gray.opacity(0.5))
            )
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