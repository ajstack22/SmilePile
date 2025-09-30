import SwiftUI
import PhotosUI

/// Comprehensive batch categorization view for efficient photo organization
struct BatchCategorizationView: View {
    @StateObject private var viewModel = BatchCategorizationViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ZStack {
                if viewModel.photos.isEmpty {
                    emptyStateView
                } else {
                    mainContent
                }

                if viewModel.isProcessing {
                    processingOverlay
                }
            }
            .navigationTitle("Batch Categorization")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                toolbarContent
            }
            .sheet(isPresented: $viewModel.showPhotosPicker) {
                PhotosPicker(
                    selection: $viewModel.selectedPhotos,
                    maxSelectionCount: 100,
                    matching: .images
                ) {
                    Text("Select Photos")
                }
            }
            .alert(isPresented: $viewModel.showAlert) {
                Alert(
                    title: Text(viewModel.alertTitle),
                    message: Text(viewModel.alertMessage),
                    dismissButton: .default(Text("OK"))
                )
            }
        }
    }

    // MARK: - Main Content

    private var mainContent: some View {
        VStack(spacing: 0) {
            // Stats bar
            statsBar

            // Quick actions
            quickActionsBar

            // Photo grid with categories
            ScrollView {
                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 100), spacing: 2)],
                    spacing: 2
                ) {
                    ForEach(viewModel.photos) { photo in
                        BatchPhotoCard(
                            photo: photo,
                            category: viewModel.getCategory(for: photo),
                            isSelected: viewModel.selectedPhotoIds.contains(photo.id),
                            onTap: {
                                viewModel.togglePhotoSelection(photo.id)
                            }
                        )
                    }
                }
                .padding(2)
            }

            // Category assignment bar
            if !viewModel.selectedPhotoIds.isEmpty {
                categoryAssignmentBar
            }
        }
    }

    // MARK: - Stats Bar

    private var statsBar: some View {
        HStack {
            VStack(alignment: .leading) {
                Text("\(viewModel.photos.count) Photos")
                    .font(.headline)
                Text("\(viewModel.categorizedCount) categorized")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Progress indicator
            if viewModel.categorizedCount > 0 {
                CircularProgressView(
                    progress: Double(viewModel.categorizedCount) / Double(viewModel.photos.count),
                    color: .green
                )
                .frame(width: 50, height: 50)
            }
        }
        .padding()
        .background(Color(UIColor.secondarySystemBackground))
    }

    // MARK: - Quick Actions Bar

    private var quickActionsBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                QuickActionButton(
                    title: "Select All",
                    icon: "checkmark.square.fill",
                    color: .blue
                ) {
                    viewModel.selectAllPhotos()
                }

                QuickActionButton(
                    title: "Uncategorized",
                    icon: "questionmark.square.stack",
                    color: .orange
                ) {
                    viewModel.selectUncategorizedPhotos()
                }

                QuickActionButton(
                    title: "By Date",
                    icon: "calendar",
                    color: .purple
                ) {
                    viewModel.groupByDate()
                }

                QuickActionButton(
                    title: "Auto Categorize",
                    icon: "wand.and.stars",
                    color: .green
                ) {
                    Task {
                        await viewModel.autoCategorize()
                    }
                }

                if !viewModel.selectedPhotoIds.isEmpty {
                    QuickActionButton(
                        title: "Clear (\(viewModel.selectedPhotoIds.count))",
                        icon: "xmark.square.fill",
                        color: .red
                    ) {
                        viewModel.clearSelection()
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
    }

    // MARK: - Category Assignment Bar

    private var categoryAssignmentBar: some View {
        VStack(spacing: 12) {
            // Selected photos info
            HStack {
                Text("\(viewModel.selectedPhotoIds.count) photos selected")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Spacer()

                Button("Clear") {
                    viewModel.clearSelection()
                }
                .font(.caption)
            }
            .padding(.horizontal)

            // Category chips
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(viewModel.categories) { category in
                        CategoryAssignmentChip(
                            category: category,
                            onTap: {
                                Task {
                                    await viewModel.assignSelectedToCategory(category)
                                }
                            }
                        )
                    }

                    // Add new category
                    Button(action: {
                        viewModel.showCreateCategory = true
                    }) {
                        Label("New", systemImage: "plus.circle")
                            .font(.caption)
                            .foregroundColor(.accentColor)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(
                                Capsule()
                                    .stroke(Color.accentColor, lineWidth: 1)
                            )
                    }
                }
                .padding(.horizontal)
            }

            // Action buttons
            HStack(spacing: 16) {
                Button(action: {
                    Task {
                        await viewModel.applyBatchAssignment()
                    }
                }) {
                    Text("Apply to Selected")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color.accentColor)
                        .cornerRadius(10)
                }
            }
            .padding(.horizontal)
        }
        .padding(.vertical)
        .background(
            Color(UIColor.systemBackground)
                .shadow(color: .black.opacity(0.1), radius: 5, y: -2)
        )
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Image(systemName: "photo.stack.fill")
                .font(.system(size: 72))
                .foregroundStyle(
                    LinearGradient(
                        colors: [.blue, .purple],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            Text("No Photos to Categorize")
                .font(.title2)
                .fontWeight(.bold)

            Text("Import photos to start organizing them into categories")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            Button(action: {
                viewModel.showPhotosPicker = true
            }) {
                Label("Import Photos", systemImage: "photo.badge.plus")
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.accentColor)
                    .cornerRadius(25)
            }
        }
        .padding()
    }

    // MARK: - Processing Overlay

    private var processingOverlay: some View {
        Color.black.opacity(0.5)
            .ignoresSafeArea()
            .overlay(
                VStack(spacing: 20) {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(1.5)

                    Text(viewModel.processingStatus)
                        .font(.headline)
                        .foregroundColor(.white)

                    if viewModel.processingProgress > 0 {
                        ProgressView(value: viewModel.processingProgress)
                            .progressViewStyle(LinearProgressViewStyle(tint: .white))
                            .frame(width: 200)

                        Text("\(Int(viewModel.processingProgress * 100))%")
                            .font(.caption)
                            .foregroundColor(.white)
                    }
                }
                .padding(32)
                .background(Color.black.opacity(0.8))
                .cornerRadius(16)
            )
    }

    // MARK: - Toolbar

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .navigationBarLeading) {
            Button("Cancel") {
                dismiss()
            }
        }

        ToolbarItem(placement: .navigationBarTrailing) {
            Menu {
                Button(action: {
                    viewModel.sortBy(.date)
                }) {
                    Label("Sort by Date", systemImage: "calendar")
                }

                Button(action: {
                    viewModel.sortBy(.name)
                }) {
                    Label("Sort by Name", systemImage: "textformat")
                }

                Button(action: {
                    viewModel.sortBy(.size)
                }) {
                    Label("Sort by Size", systemImage: "doc")
                }

                Divider()

                Button(action: {
                    viewModel.showOnlyUncategorized.toggle()
                }) {
                    Label(
                        viewModel.showOnlyUncategorized ? "Show All" : "Show Uncategorized",
                        systemImage: "questionmark.square.stack"
                    )
                }
            } label: {
                Image(systemName: "ellipsis.circle")
            }
        }
    }
}

// MARK: - Batch Categorization View Model

@MainActor
class BatchCategorizationViewModel: ObservableObject {
    @Published var photos: [Photo] = []
    @Published var categories: [Category] = []
    @Published var selectedPhotoIds: Set<Int64> = []
    @Published var selectedPhotos: [PhotosPickerItem] = []

    @Published var isProcessing = false
    @Published var processingStatus = ""
    @Published var processingProgress: Double = 0

    @Published var showPhotosPicker = false
    @Published var showCreateCategory = false
    @Published var showAlert = false
    @Published var alertTitle = ""
    @Published var alertMessage = ""

    @Published var showOnlyUncategorized = false

    private let categoryManager = CategoryManager()
    private let photoRepository = PhotoRepositoryImpl()

    enum SortOption {
        case date, name, size
    }

    init() {
        Task {
            await loadData()
        }
    }

    // MARK: - Data Loading

    func loadData() async {
        do {
            photos = try await photoRepository.getAllPhotos()
            await categoryManager.loadCategories()
            categories = categoryManager.categories
        } catch {
            showError("Failed to load data", message: error.localizedDescription)
        }
    }

    // MARK: - Selection Management

    func togglePhotoSelection(_ photoId: Int64) {
        if selectedPhotoIds.contains(photoId) {
            selectedPhotoIds.remove(photoId)
        } else {
            selectedPhotoIds.insert(photoId)
        }
    }

    func selectAllPhotos() {
        selectedPhotoIds = Set(photos.map { $0.id })
    }

    func selectUncategorizedPhotos() {
        let uncategorized = photos.filter { photo in
            !categories.contains { $0.id == photo.categoryId }
        }
        selectedPhotoIds = Set(uncategorized.map { $0.id })
    }

    func clearSelection() {
        selectedPhotoIds.removeAll()
    }

    // MARK: - Categorization

    func assignSelectedToCategory(_ category: Category) async {
        guard !selectedPhotoIds.isEmpty else { return }

        isProcessing = true
        processingStatus = "Assigning to \(category.displayName)..."

        do {
            try await categoryManager.assignPhotosToCategory(
                Array(selectedPhotoIds),
                categoryId: category.id
            )

            await loadData()
            clearSelection()

            showSuccess(
                "Categorization Complete",
                message: "\(selectedPhotoIds.count) photos assigned to \(category.displayName)"
            )
        } catch {
            showError("Categorization Failed", message: error.localizedDescription)
        }

        isProcessing = false
    }

    func applyBatchAssignment() async {
        // Implementation for batch assignment
    }

    func autoCategorize() async {
        isProcessing = true
        processingStatus = "Auto-categorizing photos..."
        processingProgress = 0

        // Simulate auto-categorization logic
        for (index, photo) in photos.enumerated() {
            // Analyze photo and assign category based on rules
            // This is a simplified example
            let categoryIndex = index % categories.count
            let category = categories[categoryIndex]

            do {
                try await categoryManager.assignPhotosToCategory(
                    [photo.id],
                    categoryId: category.id
                )

                processingProgress = Double(index + 1) / Double(photos.count)
            } catch {
                // Handle error
            }
        }

        await loadData()
        isProcessing = false

        showSuccess(
            "Auto-Categorization Complete",
            message: "\(photos.count) photos have been organized"
        )
    }

    // MARK: - Grouping

    func groupByDate() {
        // Group photos by date and select groups
        let calendar = Calendar.current
        var dateGroups: [Date: [Photo]] = [:]

        for photo in photos {
            let date = calendar.startOfDay(for: photo.createdDate)
            dateGroups[date, default: []].append(photo)
        }

        // Select photos from today
        if let todayPhotos = dateGroups[calendar.startOfDay(for: Date())] {
            selectedPhotoIds = Set(todayPhotos.map { $0.id })
        }
    }

    // MARK: - Sorting

    func sortBy(_ option: SortOption) {
        switch option {
        case .date:
            photos.sort { $0.createdAt > $1.createdAt }
        case .name:
            photos.sort { $0.displayName < $1.displayName }
        case .size:
            photos.sort { $0.fileSize > $1.fileSize }
        }
    }

    // MARK: - Helpers

    func getCategory(for photo: Photo) -> Category? {
        categories.first { $0.id == photo.categoryId }
    }

    var categorizedCount: Int {
        photos.filter { photo in
            categories.contains { $0.id == photo.categoryId }
        }.count
    }

    private func showError(_ title: String, message: String) {
        alertTitle = title
        alertMessage = message
        showAlert = true
    }

    private func showSuccess(_ title: String, message: String) {
        alertTitle = title
        alertMessage = message
        showAlert = true
    }
}

// MARK: - Supporting Views

struct BatchPhotoCard: View {
    let photo: Photo
    let category: Category?
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            // Photo thumbnail
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.gray.opacity(0.2))
                .aspectRatio(1, contentMode: .fit)
                .overlay(
                    Image(systemName: "photo")
                        .font(.title2)
                        .foregroundColor(.secondary)
                )

            // Category indicator
            if let category = category {
                CategoryBadge(category: category)
                    .offset(x: 4, y: -4)
            }

            // Selection overlay
            if isSelected {
                RoundedRectangle(cornerRadius: 8)
                    .stroke(Color.accentColor, lineWidth: 3)

                Image(systemName: "checkmark.circle.fill")
                    .font(.title3)
                    .foregroundColor(.accentColor)
                    .background(Circle().fill(Color.white))
                    .padding(4)
            }
        }
        .onTapGesture(perform: onTap)
    }
}

struct CategoryBadge: View {
    let category: Category

    var body: some View {
        ZStack {
            Circle()
                .fill(category.color)
                .frame(width: 24, height: 24)

            if let icon = category.iconResource {
                Image(systemName: icon)
                    .font(.caption2)
                    .foregroundColor(.white)
            } else {
                Text(String(category.displayName.prefix(1)))
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
        }
    }
}

struct CategoryAssignmentChip: View {
    let category: Category
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 4) {
                Circle()
                    .fill(category.color)
                    .frame(width: 8, height: 8)

                Text(category.displayName)
                    .font(.caption)
                    .fontWeight(.medium)
            }
            .foregroundColor(.primary)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                Capsule()
                    .fill(category.color.opacity(0.15))
            )
            .overlay(
                Capsule()
                    .stroke(category.color, lineWidth: 1)
            )
        }
    }
}

struct QuickActionButton: View {
    let title: String
    let icon: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.title2)
                Text(title)
                    .font(.caption2)
            }
            .foregroundColor(color)
            .frame(width: 80, height: 60)
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .fill(color.opacity(0.1))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(color.opacity(0.3), lineWidth: 1)
            )
        }
    }
}

private struct BatchCircularProgressView: View {
    let progress: Double
    let color: Color

    var body: some View {
        ZStack {
            Circle()
                .stroke(color.opacity(0.2), lineWidth: 4)

            Circle()
                .trim(from: 0, to: progress)
                .stroke(color, lineWidth: 4)
                .rotationEffect(.degrees(-90))
                .animation(.spring(), value: progress)

            Text("\(Int(progress * 100))%")
                .font(.caption2)
                .fontWeight(.bold)
        }
    }
}

// MARK: - Preview

struct BatchCategorizationView_Previews: PreviewProvider {
    static var previews: some View {
        BatchCategorizationView()
    }
}