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
                    VStack {
                        // Aspect ratio selector (only when crop is active) - positioned at top like Android
                        if viewModel.showCropOverlay {
                            AspectRatioSelector(
                                selectedRatio: $viewModel.selectedAspectRatio,
                                onSelect: { ratio in
                                    viewModel.applyAspectRatio(ratio)
                                    // Update the binding for the crop overlay
                                    if let photo = viewModel.currentPhoto {
                                        cropRect = viewModel.currentPhoto?.cropRect ?? CGRect(origin: .zero, size: photo.image.size)
                                    }
                                }
                            )
                            .padding(.top, 16)
                            .zIndex(1)  // Ensure it's above the image
                        }

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
                                            // Don't auto-close, let user manually apply
                                        },
                                        onCancel: {
                                            viewModel.showCropOverlay = false
                                        }
                                    ) : nil
                                )
                        }
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
                    print("📝 PhotoEditView: isComplete changed to true, starting save...")
                    Task {
                        let savedPhotos = await viewModel.saveAllProcessedPhotos()
                        print("✅ PhotoEditView: Saved \(savedPhotos.count) photos, now dismissing...")
                        dismiss()
                    }
                }
            }
            .alert("Remove Photo?", isPresented: $showDeleteAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Remove", role: .destructive) {
                    viewModel.deleteCurrentPhoto()
                }
            } message: {
                Text("This photo will be removed from the gallery.")
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
            Button(action: {
                // Safe dismiss with cleanup
                viewModel.cancelEditing()
                dismiss()
            }) {
                Image(systemName: "xmark")
                    .font(.title2)
                    .foregroundColor(.white)
                    .frame(width: 44, height: 44)
            }

            Spacer()

            // Match Android's "Edit Photo • X / Y" format
            Text("Edit Photo • \(viewModel.progressText)")
                .font(.headline)
                .fontWeight(.semibold)
                .foregroundColor(.white)

            Spacer()

            // Placeholder for balance
            Color.clear
                .frame(width: 44, height: 44)
        }
        .padding()
        .background(
            Color.black.opacity(0.7)
        )
    }

    private var bottomToolbar: some View {
        VStack(spacing: 0) {
            // Edit tools - Match Android's 56pt touch targets (hide when cropping)
            if !viewModel.showCropOverlay {
                HStack {
                Spacer()

                // Pile - Blue color
                Button(action: { showCategoryPicker = true }) {
                    VStack(spacing: 4) {
                        Image(systemName: "square.stack")
                            .font(.system(size: 24))
                        Text("Pile")
                            .font(.caption2)
                    }
                    .foregroundColor(Color.smilePileBlue)
                    .frame(width: 56, height: 56)
                }

                Spacer()

                // Rotate - White color
                Button(action: { viewModel.rotatePhoto() }) {
                    VStack(spacing: 4) {
                        Image(systemName: "rotate.right")
                            .font(.system(size: 24))
                        Text("Rotate")
                            .font(.caption2)
                    }
                    .foregroundColor(.white)
                    .frame(width: 56, height: 56)
                }

                Spacer()

                // Crop - Orange when active
                Button(action: {
                    viewModel.showCropOverlay.toggle()
                    if viewModel.showCropOverlay {
                        // Initialize crop rect to full image if not already set
                        if let photo = viewModel.currentPhoto {
                            cropRect = viewModel.currentPhoto?.cropRect ?? CGRect(origin: .zero, size: photo.image.size)
                            // Apply the default aspect ratio
                            viewModel.applyAspectRatio(viewModel.selectedAspectRatio)
                        }
                    } else {
                        // When hiding crop overlay, don't apply - let user use Apply button
                        viewModel.showCropOverlay = false
                    }
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: "crop")
                            .font(.system(size: 24))
                        Text("Crop")
                            .font(.caption2)
                    }
                    .foregroundColor(viewModel.showCropOverlay ? .orange : .white)
                    .frame(width: 56, height: 56)
                }

                Spacer()

                // Delete - System red color
                Button(action: { showDeleteAlert = true }) {
                    VStack(spacing: 4) {
                        Image(systemName: "trash")
                            .font(.system(size: 24))
                        Text("Delete")
                            .font(.caption2)
                    }
                    .foregroundColor(.red)
                    .frame(width: 56, height: 56)
                }

                Spacer()
                }
                .padding(.vertical, 8)
            }

            // Action buttons - Match Android layout
            HStack(spacing: 16) {
                // Skip/Cancel button
                Button(action: {
                    if viewModel.editQueue.count == 1 {
                        // Cancel - safe dismiss
                        viewModel.cancelEditing()
                        dismiss()
                    } else {
                        // Skip to next photo
                        viewModel.skipCurrentPhoto()
                    }
                }) {
                    Text(viewModel.editQueue.count == 1 ? "Cancel" : "Skip")
                        .font(.body)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(Color.white, lineWidth: 1)
                        )
                        .foregroundColor(.white)
                }
                .padding(.trailing, 8)

                // Apply button with checkmark
                Button(action: { viewModel.applyCurrentPhoto() }) {
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 18))
                        Text("Apply")
                            .font(.body)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(8)
                }
                .padding(.leading, 8)
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)

            // Apply to all option - Only shows for rotation
            if viewModel.canApplyToAll {
                Button(action: { viewModel.applyToAll() }) {
                    Text("Apply rotation to all remaining photos")
                        .font(.body)
                        .foregroundColor(Color.white.opacity(0.7))
                }
                .padding(.top, 8)
            }
        }
        .padding(.vertical)
        .background(
            Color.black.opacity(0.9)
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
                    // Radio button style to match Android
                    Image(systemName: category.id == selectedCategory?.id ? "circle.inset.filled" : "circle")
                        .foregroundColor(category.id == selectedCategory?.id ? .blue : .gray)
                        .font(.system(size: 20))

                    Text(category.displayName)
                        .font(.body)
                        .foregroundColor(.primary)

                    Spacer()
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    onSelect(category)
                    dismiss()  // Auto-dismiss after selection like Android
                }
                .padding(.vertical, 4)
            }
            .navigationTitle("Select Category")
            .navigationBarTitleDisplayMode(.inline)
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
        // Match Android's FilterChip style
        HStack(spacing: 12) {
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
                        .font(.system(size: 14, weight: .medium))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(selectedRatio == ratio ? Color.white : Color.clear)
                        .foregroundColor(selectedRatio == ratio ? .black : .white)
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(selectedRatio == ratio ? Color.clear : Color.white.opacity(0.5), lineWidth: 1)
                        )
                        .cornerRadius(20)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(Color.black.opacity(0.7))
        .cornerRadius(20)
    }
}

#Preview {
    PhotoEditView()
}