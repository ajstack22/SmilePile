import SwiftUI

/// Photo stack component matching Android's vertical stack layout
struct PhotoStackView: View {
    let photos: [Photo]
    let onPhotoClick: (Photo) -> Void
    let onEditClick: ((Photo) -> Void)?
    let onDeleteClick: ((Photo) -> Void)?

    var body: some View {
        if photos.isEmpty {
            EmptyPhotoStackState()
        } else {
            PhotoStack(
                photos: photos,
                onPhotoClick: onPhotoClick,
                onEditClick: onEditClick,
                onDeleteClick: onDeleteClick
            )
        }
    }
}

private struct PhotoStack: View {
    let photos: [Photo]
    let onPhotoClick: (Photo) -> Void
    let onEditClick: ((Photo) -> Void)?
    let onDeleteClick: ((Photo) -> Void)?

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(photos) { photo in
                    PhotoStackItem(
                        photo: photo,
                        onPhotoClick: { onPhotoClick(photo) },
                        onEditClick: { onEditClick?(photo) },
                        onDeleteClick: { onDeleteClick?(photo) }
                    )
                }
            }
            .padding(16)
        }
    }
}

private struct PhotoStackItem: View {
    let photo: Photo
    let onPhotoClick: () -> Void
    let onEditClick: (() -> Void)?
    let onDeleteClick: (() -> Void)?

    private var photoURL: URL? {
        // Handle both absolute and relative paths
        if photo.path.starts(with: "/") {
            // Absolute path
            return URL(fileURLWithPath: photo.path)
        } else {
            // Relative path - prepend documents directory
            let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
            return documentsURL.appendingPathComponent(photo.path)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            // Photo card
            AsyncImage(url: photoURL) { phase in
                switch phase {
                case .empty:
                    Rectangle()
                        .fill(Color.gray.opacity(0.2))
                        .overlay(
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle())
                        )
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(maxHeight: 300)
                        .clipped()
                case .failure(let error):
                    Rectangle()
                        .fill(Color.gray.opacity(0.2))
                        .overlay(
                            VStack(spacing: 8) {
                                Image(systemName: "exclamationmark.triangle")
                                    .foregroundColor(.gray)
                                    .font(.title2)
                                Text("Failed to load image")
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }
                        )
                        .onAppear {
                            // Log error for debugging
                            print("Failed to load photo at path: \(photo.path), error: \(error)")
                        }
                @unknown default:
                    EmptyView()
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 300)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .onTapGesture {
                onPhotoClick()
            }

            // Action buttons for parent mode
            if onEditClick != nil || onDeleteClick != nil {
                HStack(spacing: 16) {
                    if let edit = onEditClick {
                        Button(action: edit) {
                            Label("Edit", systemImage: "pencil")
                                .font(.caption)
                        }
                        .buttonStyle(.bordered)
                    }

                    if let delete = onDeleteClick {
                        Button(action: delete) {
                            Label("Delete", systemImage: "trash")
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                        .buttonStyle(.bordered)
                        .tint(.red)
                    }

                    Spacer()
                }
                .padding(.top, 8)
            }
        }
        .padding(.vertical, 4)
    }
}

private struct EmptyPhotoStackState: View {
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "photo.stack")
                .font(.system(size: 64))
                .foregroundColor(.gray)

            Text("No photos in this category")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Add photos to get started")
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }
}