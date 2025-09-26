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
        NavigationView {
            VStack {
                if !isKidsMode {
                    categoryFilterBar
                }

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
                }

                Spacer()
            }
            .navigationTitle("SmilePile")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        showingImportPicker = true
                    }) {
                        Image(systemName: "plus.circle")
                            .font(.title2)
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        isKidsMode.toggle()
                    }) {
                        HStack {
                            Image(systemName: isKidsMode ? "face.smiling" : "person.fill")
                            Text(isKidsMode ? "Kids" : "Parent")
                                .font(.caption)
                        }
                    }
                }
            }
        }
        .onAppear {
            loadCategories()
        }
        .fullScreenCover(isPresented: $showingPhotoEditor) {
            PhotoEditView(
                photos: selectedPhotos.isEmpty ? nil : selectedPhotos,
                imageURLs: importedImageURLs.isEmpty ? nil : importedImageURLs,
                initialCategoryId: selectedCategory?.id ?? 1
            )
            .onDisappear {
                selectedPhotos = []
                importedImageURLs = []
                // Reload photos after editing
                loadCategories()
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
                showingPhotoEditor = true
            case .failure(let error):
                print("Import failed: \(error)")
            }
        }
    }

    @ViewBuilder
    private var categoryFilterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                CategoryChip(
                    displayName: "All",
                    colorHex: "#808080",
                    isSelected: selectedCategory == nil,
                    onTap: {
                        selectedCategory = nil
                    }
                )

                ForEach(categories, id: \.id) { category in
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
            .padding(.horizontal)
            .padding(.vertical, 8)
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

    // Temporary mock data for testing
    private var mockPhotos: [Photo] {
        (1...12).map { index in
            Photo(
                id: Int64(index),
                path: "/mock/photo\(index).jpg",
                categoryId: Int64((index % 4) + 1),
                name: "Photo \(index)"
            )
        }
    }
}

struct PhotoThumbnail: View {
    let photo: Photo

    var body: some View {
        Rectangle()
            .fill(Color.gray.opacity(0.2))
            .aspectRatio(1, contentMode: .fit)
            .overlay(
                VStack {
                    Image(systemName: "photo")
                        .font(.largeTitle)
                        .foregroundColor(.gray.opacity(0.5))
                    Text(photo.displayName)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
            )
    }
}

#Preview {
    PhotoGalleryView()
}