import SwiftUI

struct PhotoEditView: View {
    @StateObject private var viewModel = PhotoEditViewModel()
    @Environment(\.dismiss) private var dismiss
    @State private var showCategoryPicker = false
    @State private var showDeleteAlert = false
    @State private var cropRect = CGRect.zero

    let photos: [Photo]?
    let imageURLs: [URL]?
    let initialCategoryId: Int64

    init(photos: [Photo]? = nil, imageURLs: [URL]? = nil, initialCategoryId: Int64 = 1) {
        self.photos = photos
        self.imageURLs = imageURLs
        self.initialCategoryId = initialCategoryId
    }

    var body: some View {
        NavigationView {
            ZStack {
                // Black background
                Color.black.ignoresSafeArea()

                if viewModel.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else if let previewImage = viewModel.previewImage {
                    // Photo display
                    GeometryReader { geometry in
                        Image(uiImage: previewImage)
                            .resizable()
                            .scaledToFit()
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .overlay(
                                // Crop overlay when active
                                viewModel.showCropOverlay ?
                                CropOverlayView(
                                    cropRect: $cropRect,
                                    imageSize: previewImage.size,
                                    onComplete: { rect in
                                        viewModel.updateCropRect(rect)
                                        viewModel.applyCrop()
                                        viewModel.showCropOverlay = false
                                    },
                                    onCancel: {
                                        viewModel.showCropOverlay = false
                                    }
                                ) : nil
                            )
                    }
                }

                // Error message
                if let error = viewModel.errorMessage {
                    VStack {
                        Text("Error")
                            .font(.title2)
                            .foregroundColor(.white)
                        Text(error)
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding()
                        Button("Skip") {
                            viewModel.skipCurrentPhoto()
                        }
                        .foregroundColor(.orange)
                    }
                    .padding()
                    .background(Color.black.opacity(0.8))
                    .cornerRadius(12)
                }

                // Toolbar overlays
                if !viewModel.showCropOverlay {
                    VStack {
                        // Top toolbar
                        topToolbar

                        Spacer()

                        // Bottom toolbar
                        bottomToolbar
                    }
                }
            }
            .navigationBarHidden(true)
            .onAppear {
                viewModel.initializeEditor(
                    photos: photos,
                    imageURLs: imageURLs,
                    categoryId: initialCategoryId
                )
                if let firstPhoto = viewModel.currentPhoto {
                    cropRect = CGRect(origin: .zero, size: firstPhoto.image.size)
                }
            }
            .onChange(of: viewModel.isComplete) { isComplete in
                if isComplete {
                    Task {
                        _ = await viewModel.saveAllProcessedPhotos()
                        dismiss()
                    }
                }
            }
            .alert("Delete Photo?", isPresented: $showDeleteAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Delete", role: .destructive) {
                    viewModel.deleteCurrentPhoto()
                }
            } message: {
                Text("This photo will be permanently deleted.")
            }
            .sheet(isPresented: $showCategoryPicker) {
                CategoryPickerView(
                    categories: viewModel.categories,
                    selectedCategory: viewModel.selectedCategory,
                    onSelect: { category in
                        viewModel.updateCategory(category)
                        showCategoryPicker = false
                    }
                )
            }
        }
    }

    private var topToolbar: some View {
        HStack {
            Button(action: { dismiss() }) {
                Image(systemName: "xmark")
                    .font(.title2)
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
            }

            Spacer()

            Text(viewModel.progressText)
                .font(.headline)
                .foregroundColor(.white)

            Spacer()

            // Placeholder for balance
            Color.clear
                .frame(width: 44, height: 44)
        }
        .padding()
        .background(
            LinearGradient(
                colors: [Color.black, Color.black.opacity(0)],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }

    private var bottomToolbar: some View {
        VStack(spacing: 0) {
            // Aspect ratio selector (only when crop is active)
            if viewModel.showCropOverlay {
                AspectRatioSelector(
                    selectedRatio: $viewModel.selectedAspectRatio,
                    onSelect: { ratio in
                        viewModel.applyAspectRatio(ratio)
                    }
                )
                .padding(.bottom, 10)
            }

            // Edit tools
            HStack(spacing: 30) {
                // Category
                Button(action: { showCategoryPicker = true }) {
                    VStack(spacing: 4) {
                        Image(systemName: "folder")
                            .font(.system(size: 24))
                        Text(viewModel.selectedCategory?.displayName ?? "Category")
                            .font(.caption2)
                    }
                    .foregroundColor(.white)
                }

                // Rotate
                Button(action: { viewModel.rotatePhoto() }) {
                    VStack(spacing: 4) {
                        Image(systemName: "rotate.right")
                            .font(.system(size: 24))
                        Text("Rotate")
                            .font(.caption2)
                    }
                    .foregroundColor(.white)
                }

                // Crop
                Button(action: {
                    viewModel.showCropOverlay.toggle()
                    if viewModel.showCropOverlay, let photo = viewModel.currentPhoto {
                        cropRect = CGRect(origin: .zero, size: photo.image.size)
                    }
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: "crop")
                            .font(.system(size: 24))
                        Text("Crop")
                            .font(.caption2)
                    }
                    .foregroundColor(viewModel.showCropOverlay ? .orange : .white)
                }

                // Delete
                Button(action: { showDeleteAlert = true }) {
                    VStack(spacing: 4) {
                        Image(systemName: "trash")
                            .font(.system(size: 24))
                        Text("Delete")
                            .font(.caption2)
                    }
                    .foregroundColor(.white)
                }
            }
            .padding(.vertical, 12)

            // Action buttons
            HStack(spacing: 20) {
                if viewModel.editQueue.count > 1 {
                    Button(action: { viewModel.skipCurrentPhoto() }) {
                        Text("Skip")
                            .font(.body)
                            .padding(.horizontal, 30)
                            .padding(.vertical, 12)
                            .background(Color.gray.opacity(0.3))
                            .foregroundColor(.white)
                            .cornerRadius(25)
                    }
                }

                Button(action: { viewModel.applyCurrentPhoto() }) {
                    Text(viewModel.hasMorePhotos ? "Apply" : "Done")
                        .font(.body)
                        .fontWeight(.medium)
                        .padding(.horizontal, 30)
                        .padding(.vertical, 12)
                        .background(Color.orange)
                        .foregroundColor(.white)
                        .cornerRadius(25)
                }

                if viewModel.canApplyToAll {
                    Button(action: { viewModel.applyToAll() }) {
                        Text("Apply to All")
                            .font(.body)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(25)
                    }
                }
            }
            .padding(.top, 10)
        }
        .padding()
        .background(
            LinearGradient(
                colors: [Color.black.opacity(0), Color.black],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
}

// MARK: - Category Picker View

struct CategoryPickerView: View {
    let categories: [Category]
    let selectedCategory: Category?
    let onSelect: (Category) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            List(categories) { category in
                HStack {
                    Circle()
                        .fill(category.color)
                        .frame(width: 24, height: 24)

                    Text(category.displayName)
                        .foregroundColor(.primary)

                    Spacer()

                    if category.id == selectedCategory?.id {
                        Image(systemName: "checkmark")
                            .foregroundColor(.blue)
                    }
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    onSelect(category)
                }
            }
            .navigationTitle("Select Category")
            .navigationBarItems(
                trailing: Button("Cancel") { dismiss() }
            )
        }
    }
}

// MARK: - Aspect Ratio Selector

struct AspectRatioSelector: View {
    @Binding var selectedRatio: ImageProcessor.AspectRatio
    let onSelect: (ImageProcessor.AspectRatio) -> Void

    var body: some View {
        HStack(spacing: 20) {
            ForEach([
                ("Free", ImageProcessor.AspectRatio.free),
                ("1:1", ImageProcessor.AspectRatio.square),
                ("4:3", ImageProcessor.AspectRatio.standard),
                ("16:9", ImageProcessor.AspectRatio.wide)
            ], id: \.0) { label, ratio in
                Button(action: {
                    selectedRatio = ratio
                    onSelect(ratio)
                }) {
                    Text(label)
                        .font(.system(size: 14))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(selectedRatio == ratio ? Color.orange : Color.gray.opacity(0.3))
                        .foregroundColor(.white)
                        .cornerRadius(15)
                }
            }
        }
    }
}

#Preview {
    PhotoEditView()
}