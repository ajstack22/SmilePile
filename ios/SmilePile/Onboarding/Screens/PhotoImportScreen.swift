import SwiftUI
import PhotosUI

struct PhotoImportScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator
    @State private var selectedPhotos: [PhotosPickerItem] = []
    @State private var importedImages: [ImportedImageData] = []
    @State private var isLoadingPhotos = false
    @State private var showPhotoPicker = false

    struct ImportedImageData: Identifiable {
        let id: String
        let image: UIImage
        var categoryId: UUID?
    }

    let maxPhotos = 5

    var body: some View {
        VStack(spacing: 0) {
            // Instructions
            VStack(spacing: 8) {
                Text("Add Your First Photos")
                    .font(.title2)
                    .fontWeight(.bold)

                Text("Select up to \(maxPhotos) photos to get started")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                Text("You can always add more later")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()

            if importedImages.isEmpty {
                // Empty state
                VStack(spacing: 20) {
                    Spacer()

                    Image(systemName: "photo.on.rectangle.angled")
                        .font(.system(size: 80))
                        .foregroundColor(.gray.opacity(0.3))

                    Text("No photos selected yet")
                        .font(.headline)
                        .foregroundColor(.secondary)

                    PhotosPicker(
                        selection: $selectedPhotos,
                        maxSelectionCount: maxPhotos,
                        selectionBehavior: .ordered,
                        matching: .images
                    ) {
                        Label("Select Photos", systemImage: "photo.fill")
                            .font(.headline)
                            .foregroundColor(.white)
                            .padding()
                            .frame(width: 200)
                            .background(Color(red: 0.3, green: 0.7, blue: 1.0))
                            .cornerRadius(12)
                    }
                    .onChange(of: selectedPhotos) { items in
                        Task {
                            await loadPhotos(from: items)
                        }
                    }

                    Spacer()
                }
            } else {
                // Photos selected
                ScrollView {
                    VStack(spacing: 16) {
                        // Photo grid
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 100))], spacing: 16) {
                            ForEach(importedImages) { imageData in
                                PhotoThumbnail(
                                    imageData: imageData,
                                    categories: coordinator.onboardingData.categories,
                                    onCategoryChanged: { newCategoryId in
                                        updatePhotoCategory(imageId: imageData.id, categoryId: newCategoryId)
                                    },
                                    onRemove: {
                                        removePhoto(imageData)
                                    }
                                )
                            }
                        }
                        .padding()

                        // Add more photos button
                        if importedImages.count < maxPhotos {
                            PhotosPicker(
                                selection: $selectedPhotos,
                                maxSelectionCount: maxPhotos - importedImages.count,
                                selectionBehavior: .ordered,
                                matching: .images
                            ) {
                                HStack {
                                    Image(systemName: "plus.circle")
                                    Text("Add More Photos")
                                }
                                .font(.subheadline)
                                .foregroundColor(Color(red: 0.3, green: 0.7, blue: 1.0))
                            }
                            .onChange(of: selectedPhotos) { items in
                                Task {
                                    await loadPhotos(from: items)
                                }
                            }
                        }
                    }
                }

                // Category assignment reminder
                if importedImages.contains(where: { $0.categoryId == nil }) {
                    HStack {
                        Image(systemName: "info.circle")
                            .foregroundColor(.blue)

                        Text("Tap photos to assign categories")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(Color.blue.opacity(0.1))
                    )
                    .padding(.horizontal)
                }
            }

            // Continue button
            VStack(spacing: 16) {
                Button(action: {
                    saveImportedPhotos()
                    coordinator.navigateToNext()
                }) {
                    Text(importedImages.isEmpty ? "Skip for Now" : "Continue")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(red: 1.0, green: 0.42, blue: 0.42))
                        .cornerRadius(12)
                }
            }
            .padding()
        }
        .overlay(
            isLoadingPhotos ? ProgressView()
                .scaleEffect(1.5)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.black.opacity(0.3))
                : nil
        )
    }

    private func loadPhotos(from items: [PhotosPickerItem]) async {
        isLoadingPhotos = true
        defer { isLoadingPhotos = false }

        for item in items {
            // Check if we already have this photo
            if importedImages.contains(where: { $0.id == item.itemIdentifier }) {
                continue
            }

            // Load the image
            if let data = try? await item.loadTransferable(type: Data.self),
               let image = UIImage(data: data) {
                let imageData = ImportedImageData(
                    id: item.itemIdentifier ?? UUID().uuidString,
                    image: image,
                    categoryId: coordinator.onboardingData.categories.first?.id
                )
                importedImages.append(imageData)
            }
        }

        selectedPhotos = []
    }

    private func updatePhotoCategory(imageId: String, categoryId: UUID?) {
        if let index = importedImages.firstIndex(where: { $0.id == imageId }) {
            importedImages[index].categoryId = categoryId
        }
    }

    private func removePhoto(_ imageData: ImportedImageData) {
        importedImages.removeAll { $0.id == imageData.id }
    }

    private func saveImportedPhotos() {
        coordinator.onboardingData.importedPhotos = importedImages.map { imageData in
            ImportedPhoto(
                id: imageData.id,
                categoryId: imageData.categoryId,
                assetIdentifier: imageData.id
            )
        }
    }
}

struct PhotoThumbnail: View {
    let imageData: PhotoImportScreen.ImportedImageData
    let categories: [TempCategory]
    let onCategoryChanged: (UUID?) -> Void
    let onRemove: () -> Void
    @State private var showCategoryPicker = false

    var assignedCategory: TempCategory? {
        categories.first { $0.id == imageData.categoryId }
    }

    var body: some View {
        VStack(spacing: 4) {
            ZStack(alignment: .topTrailing) {
                // Photo
                Image(uiImage: imageData.image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 100, height: 100)
                    .clipped()
                    .cornerRadius(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(
                                assignedCategory != nil ?
                                Color(hex: assignedCategory!.colorHex) :
                                Color.gray.opacity(0.3),
                                lineWidth: 2
                            )
                    )
                    .onTapGesture {
                        showCategoryPicker = true
                    }

                // Remove button
                Button(action: onRemove) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title3)
                        .foregroundColor(.white)
                        .background(Circle().fill(Color.black.opacity(0.5)))
                }
                .offset(x: 8, y: -8)
            }

            // Category badge
            if let category = assignedCategory {
                HStack(spacing: 4) {
                    Circle()
                        .fill(Color(hex: category.colorHex))
                        .frame(width: 8, height: 8)

                    Text(category.name)
                        .font(.caption2)
                        .lineLimit(1)
                }
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(
                    Capsule()
                        .fill(Color(hex: category.colorHex).opacity(0.2))
                )
            } else {
                Text("Tap to assign")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .sheet(isPresented: $showCategoryPicker) {
            CategoryPickerSheet(
                categories: categories,
                selectedCategoryId: imageData.categoryId,
                onSelect: { categoryId in
                    onCategoryChanged(categoryId)
                    showCategoryPicker = false
                }
            )
        }
    }
}

struct CategoryPickerSheet: View {
    let categories: [TempCategory]
    let selectedCategoryId: UUID?
    let onSelect: (UUID?) -> Void
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationView {
            VStack {
                List {
                    // No category option
                    HStack {
                        Text("No Category")
                            .foregroundColor(.secondary)

                        Spacer()

                        if selectedCategoryId == nil {
                            Image(systemName: "checkmark")
                                .foregroundColor(.blue)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onSelect(nil)
                    }

                    // Category options
                    ForEach(categories, id: \.id) { category in
                        HStack {
                            Circle()
                                .fill(Color(hex: category.colorHex))
                                .frame(width: 12, height: 12)

                            Text(category.name)

                            Spacer()

                            if selectedCategoryId == category.id {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.blue)
                            }
                        }
                        .contentShape(Rectangle())
                        .onTapGesture {
                            onSelect(category.id)
                        }
                    }
                }
            }
            .navigationTitle("Select Category")
            .navigationBarItems(trailing: Button("Done") { dismiss() })
        }
    }
}