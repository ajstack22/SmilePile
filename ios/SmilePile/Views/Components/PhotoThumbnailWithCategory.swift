import SwiftUI

/// Photo thumbnail view with category indicator and multi-select support
struct PhotoThumbnailWithCategory: View {
    let photo: Photo
    let category: Category?
    let isSelected: Bool
    let editMode: SwiftUI.EditMode
    let onTap: () -> Void
    let onLongPress: () -> Void

    @State private var thumbnailImage: UIImage?
    @State private var isLoading = true
    @StateObject private var imageLoader = ThumbnailImageLoader()

    private let thumbnailSize: CGFloat = 100

    var body: some View {
        ZStack(alignment: .topTrailing) {
            // Main thumbnail
            thumbnailContent
                .onTapGesture {
                    onTap()
                }
                .onLongPressGesture {
                    onLongPress()
                }

            // Category indicator
            if let category = category {
                categoryIndicator(category)
            }

            // Selection overlay
            if editMode == .active {
                selectionOverlay
            }

        }
        .frame(width: thumbnailSize, height: thumbnailSize)
        .cornerRadius(8)
        .task {
            await loadThumbnail()
        }
    }

    // MARK: - Thumbnail Content

    @ViewBuilder
    private var thumbnailContent: some View {
        if let image = thumbnailImage {
            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: thumbnailSize, height: thumbnailSize)
                .clipped()
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(isSelected ? Color.accentColor : Color.clear, lineWidth: 3)
                )
        } else if isLoading {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(UIColor.secondarySystemFill))
                .overlay(
                    ProgressView()
                        .scaleEffect(0.5)
                )
        } else {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(UIColor.tertiarySystemFill))
                .overlay(
                    Image(systemName: "photo")
                        .font(.title2)
                        .foregroundColor(.secondary)
                )
        }
    }

    // MARK: - Category Indicator

    @ViewBuilder
    private func categoryIndicator(_ category: Category) -> some View {
        HStack(spacing: 0) {
            // Category color bar
            Rectangle()
                .fill(category.color)
                .frame(width: 4)

            // Category icon or initial
            ZStack {
                Rectangle()
                    .fill(category.color.opacity(0.9))

                if let iconName = category.iconResource {
                    Image(systemName: iconName)
                        .font(.caption2)
                        .foregroundColor(.white)
                } else {
                    Text(String(category.displayName.prefix(1)))
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                }
            }
            .frame(width: 20)
        }
        .frame(width: 24, height: 24)
        .cornerRadius(4, corners: [.bottomLeft])
        .offset(x: 4, y: -4)
    }

    // MARK: - Selection Overlay

    @ViewBuilder
    private var selectionOverlay: some View {
        ZStack(alignment: .topLeading) {
            // Semi-transparent overlay when in edit mode
            if editMode == .active && !isSelected {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.black.opacity(0.2))
            }

            // Selection checkmark
            Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                .font(.title3)
                .foregroundColor(isSelected ? .accentColor : .white)
                .background(
                    Circle()
                        .fill(Color.black.opacity(0.5))
                        .padding(-2)
                )
                .padding(4)
        }
    }


    // MARK: - Load Thumbnail

    private func loadThumbnail() async {
        isLoading = true

        // Get thumbnail from storage
        thumbnailImage = await imageLoader.loadThumbnail(for: photo)

        isLoading = false
    }
}

// MARK: - Photo Grid with Categories

struct PhotoGridWithCategories: View {
    @ObservedObject var photoGalleryViewModel: PhotoGalleryViewModel
    @ObservedObject var categoryManager: CategoryManager
    @State private var selectedPhotoIds: Set<Int64> = []
    @State private var editMode: SwiftUI.EditMode = .inactive
    @State private var showCategorySelection = false
    @State private var showPhotoDetail: Photo?

    private let columns = [
        GridItem(.adaptive(minimum: 100), spacing: 2)
    ]

    var body: some View {
        VStack(spacing: 0) {
            // Category filter bar
            if !categoryManager.categories.isEmpty {
                CategoryFilterView(
                    categories: categoryManager.categories,
                    selectedCategory: photoGalleryViewModel.selectedCategory,
                    onCategorySelected: { category in
                        photoGalleryViewModel.selectedCategory = category
                    }
                )
            }

            // Selection toolbar
            if editMode == .active {
                selectionToolbar
            }

            // Photo grid
            ScrollView {
                LazyVGrid(columns: columns, spacing: 2) {
                    ForEach(photoGalleryViewModel.filteredPhotos) { photo in
                        PhotoThumbnailWithCategory(
                            photo: photo,
                            category: getCategory(for: photo),
                            isSelected: selectedPhotoIds.contains(photo.id),
                            editMode: editMode,
                            onTap: {
                                handlePhotoTap(photo)
                            },
                            onLongPress: {
                                handlePhotoLongPress(photo)
                            }
                        )
                        .transition(.scale.combined(with: .opacity))
                    }
                }
                .padding(2)
            }
            .environment(\.editMode, $editMode)
            .sheet(isPresented: $showCategorySelection) {
                BatchCategoryAssignmentView(
                    categoryManager: categoryManager,
                    selectedPhotoIds: selectedPhotoIds,
                    isPresented: $showCategorySelection,
                    onComplete: {
                        selectedPhotoIds.removeAll()
                        editMode = .inactive
                    }
                )
            }
            .sheet(item: $showPhotoDetail) { photo in
                PhotoDetailView(photo: photo, category: getCategory(for: photo))
            }
        }
    }

    // MARK: - Selection Toolbar

    private var selectionToolbar: some View {
        HStack {
            Text("\(selectedPhotoIds.count) selected")
                .font(.caption)
                .foregroundColor(.secondary)

            Spacer()

            HStack(spacing: 16) {
                // Assign category button
                Button(action: {
                    showCategorySelection = true
                }) {
                    Label("Categorize", systemImage: "square.stack.badge.plus")
                        .font(.caption)
                }
                .disabled(selectedPhotoIds.isEmpty)


                // Clear selection
                Button(action: {
                    selectedPhotoIds.removeAll()
                    editMode = .inactive
                }) {
                    Label("Clear", systemImage: "xmark")
                        .font(.caption)
                }
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
        .background(Color(UIColor.secondarySystemBackground))
        .transition(.move(edge: .top).combined(with: .opacity))
    }

    // MARK: - Helper Methods

    private func getCategory(for photo: Photo) -> Category? {
        categoryManager.categories.first { $0.id == photo.categoryId }
    }

    private func handlePhotoTap(_ photo: Photo) {
        if editMode == .active {
            togglePhotoSelection(photo)
        } else {
            showPhotoDetail = photo
        }
    }

    private func handlePhotoLongPress(_ photo: Photo) {
        if editMode == .inactive {
            editMode = .active
            selectedPhotoIds.insert(photo.id)
        }
    }

    private func togglePhotoSelection(_ photo: Photo) {
        if selectedPhotoIds.contains(photo.id) {
            selectedPhotoIds.remove(photo.id)
        } else {
            selectedPhotoIds.insert(photo.id)
        }
    }

}

// MARK: - Batch Category Assignment View

struct BatchCategoryAssignmentView: View {
    @ObservedObject var categoryManager: CategoryManager
    let selectedPhotoIds: Set<Int64>
    @Binding var isPresented: Bool
    let onComplete: () -> Void

    @State private var selectedCategoryId: Int64?
    @State private var isAssigning = false

    var body: some View {
        NavigationStack {
            VStack {
                // Header info
                VStack(spacing: 8) {
                    Image(systemName: "photo.stack")
                        .font(.largeTitle)
                        .foregroundColor(.accentColor)

                    Text("Assign \(selectedPhotoIds.count) photos to category")
                        .font(.headline)

                    Text("Select a category to organize your photos")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding()

                // Category list
                List(categoryManager.categoriesWithCounts) { categoryWithCount in
                    CategorySelectionRow(
                        category: categoryWithCount.category,
                        photoCount: categoryWithCount.photoCount,
                        isSelected: selectedCategoryId == categoryWithCount.category.id,
                        onTap: {
                            selectedCategoryId = categoryWithCount.category.id
                        }
                    )
                }
                .listStyle(InsetGroupedListStyle())
            }
            .navigationTitle("Select Category")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        isPresented = false
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Assign") {
                        Task {
                            await assignPhotos()
                        }
                    }
                    .fontWeight(.semibold)
                    .disabled(selectedCategoryId == nil || isAssigning)
                }
            }
            .disabled(isAssigning)
            .overlay {
                if isAssigning {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                        .overlay {
                            ProgressView("Assigning...")
                                .padding(20)
                                .background(Color(UIColor.systemBackground))
                                .cornerRadius(10)
                        }
                }
            }
        }
    }

    private func assignPhotos() async {
        guard let categoryId = selectedCategoryId else { return }

        isAssigning = true

        do {
            try await categoryManager.assignPhotosToCategory(
                Array(selectedPhotoIds),
                categoryId: categoryId
            )
            onComplete()
            isPresented = false
        } catch {
            // Handle error
        }

        isAssigning = false
    }
}

struct CategorySelectionRow: View {
    let category: Category
    let photoCount: Int
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                // Category icon
                ZStack {
                    Circle()
                        .fill(category.color)
                        .frame(width: 40, height: 40)

                    if let iconName = category.iconResource {
                        Image(systemName: iconName)
                            .font(.body)
                            .foregroundColor(.white)
                    } else {
                        Text(String(category.displayName.prefix(1)))
                            .font(.body)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                    }
                }

                // Category info
                VStack(alignment: .leading) {
                    Text(category.displayName)
                        .font(.headline)
                        .foregroundColor(.primary)

                    Text("\(photoCount) photos")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                // Selection indicator
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.title2)
                        .foregroundColor(.accentColor)
                }
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Photo Detail View

struct PhotoDetailView: View {
    let photo: Photo
    let category: Category?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Photo image placeholder
                    Rectangle()
                        .fill(Color.gray.opacity(0.3))
                        .aspectRatio(1, contentMode: .fit)
                        .overlay(
                            Image(systemName: "photo")
                                .font(.largeTitle)
                                .foregroundColor(.secondary)
                        )

                    // Photo info
                    VStack(alignment: .leading, spacing: 12) {
                        if let category = category {
                            HStack {
                                Circle()
                                    .fill(category.color)
                                    .frame(width: 12, height: 12)
                                Text(category.displayName)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                        }

                        Text(photo.displayName)
                            .font(.title2)
                            .fontWeight(.semibold)

                        HStack {
                            Label(photo.formattedFileSize, systemImage: "doc")
                            Spacer()
                            Label("\(photo.width) × \(photo.height)", systemImage: "aspectratio")
                        }
                        .font(.caption)
                        .foregroundColor(.secondary)
                    }
                    .padding()
                }
            }
            .navigationTitle("Photo Details")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Supporting Components

class ThumbnailImageLoader: ObservableObject {
    func loadThumbnail(for photo: Photo) async -> UIImage? {
        // Simplified thumbnail loading
        // In production, this would load from the actual photo path
        return nil
    }
}

extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

struct RoundedCorner: Shape {
    var radius: CGFloat = 0
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

// MARK: - Preview

struct PhotoThumbnailWithCategory_Previews: PreviewProvider {
    static var previews: some View {
        PhotoGridWithCategories(
            photoGalleryViewModel: PhotoGalleryViewModel(),
            categoryManager: CategoryManager()
        )
    }
}