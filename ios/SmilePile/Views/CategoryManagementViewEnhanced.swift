import SwiftUI
import UniformTypeIdentifiers

/// Enhanced Category Management View with drag & drop, batch operations, and multi-select
struct CategoryManagementViewEnhanced: View {
    @StateObject private var viewModel = CategoryViewModel()
    @State private var pulseAnimation = false
    @State private var editMode: EditMode = .inactive
    @State private var showBatchActions = false
    @State private var draggedPhoto: Photo?

    var body: some View {
        NavigationStack {
            ZStack {
                if viewModel.categoriesWithCounts.isEmpty && !viewModel.isLoading {
                    emptyState
                } else {
                    categoryContent
                }

                if viewModel.isLoading {
                    loadingOverlay
                }
            }
            .navigationTitle("Category Management")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                toolbarContent
            }
            .environment(\.editMode, $editMode)
            .sheet(isPresented: $viewModel.showAddCategorySheet) {
                CreateCategorySheet(
                    categoryManager: viewModel.categoryManager,
                    isPresented: $viewModel.showAddCategorySheet,
                    onCategoryCreated: { category in
                        Task {
                            await viewModel.loadCategories()
                        }
                    }
                )
            }
            .confirmationDialog("Delete Category",
                              isPresented: $viewModel.showDeleteConfirmation,
                              titleVisibility: .visible) {
                deleteConfirmationDialog
            }
            .alert(isPresented: .constant(viewModel.errorMessage != nil)) {
                Alert(
                    title: Text("Error"),
                    message: Text(viewModel.errorMessage ?? ""),
                    dismissButton: .default(Text("OK")) {
                        viewModel.dismissMessages()
                    }
                )
            }
            .alert(isPresented: .constant(viewModel.successMessage != nil)) {
                Alert(
                    title: Text("Success"),
                    message: Text(viewModel.successMessage ?? ""),
                    dismissButton: .default(Text("OK")) {
                        viewModel.dismissMessages()
                    }
                )
            }
        }
    }

    // MARK: - Main Content

    @ViewBuilder
    private var categoryContent: some View {
        VStack(spacing: 0) {
            // Search and filter bar
            searchFilterBar

            // Batch actions bar
            if editMode == .active && viewModel.hasSelectedCategories {
                batchActionsBar
            }

            // Category list
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(viewModel.categoriesWithCounts) { categoryWithCount in
                        CategoryCardEnhanced(
                            categoryWithCount: categoryWithCount,
                            isSelected: viewModel.selectedCategoryIds.contains(categoryWithCount.category.id),
                            editMode: editMode,
                            draggedCategory: $viewModel.draggedCategory,
                            dropTargetCategory: $viewModel.dropTargetCategory,
                            onTap: {
                                handleCategoryTap(categoryWithCount.category)
                            },
                            onEdit: { category in
                                viewModel.showEditCategorySheet(category)
                            },
                            onDelete: { category in
                                viewModel.requestDeleteCategory(category)
                            },
                            onDrop: { _ in
                                Task {
                                    await viewModel.handleDrop()
                                }
                            }
                        )
                        .transition(.asymmetric(
                            insertion: .scale.combined(with: .opacity),
                            removal: .scale.combined(with: .opacity)
                        ))
                    }
                }
                .padding()
            }
        }
    }

    // MARK: - Search & Filter Bar

    private var searchFilterBar: some View {
        VStack(spacing: 0) {
            HStack {
                // Search field
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)

                    TextField("Search categories", text: $viewModel.searchQuery)

                    if !viewModel.searchQuery.isEmpty {
                        Button(action: { viewModel.searchQuery = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(8)
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(10)

                // Sort menu
                Menu {
                    ForEach(CategoryViewModel.SortOption.allCases, id: \.self) { option in
                        Button(action: {
                            viewModel.sortOption = option
                        }) {
                            Label(option.rawValue, systemImage: option.systemImage)
                        }
                    }
                } label: {
                    Image(systemName: "arrow.up.arrow.down")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(.accentColor)
                        .padding(8)
                        .background(Color(UIColor.secondarySystemBackground))
                        .clipShape(Circle())
                }
            }
            .padding()

            // Filter chips
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    FilterChip(
                        title: "Show Empty",
                        isSelected: viewModel.showEmptyCategories,
                        onTap: {
                            viewModel.showEmptyCategories.toggle()
                        }
                    )

                    if !viewModel.searchQuery.isEmpty || !viewModel.showEmptyCategories {
                        FilterChip(
                            title: "Clear Filters",
                            isSelected: false,
                            systemImage: "xmark",
                            onTap: {
                                viewModel.searchQuery = ""
                                viewModel.showEmptyCategories = true
                            }
                        )
                    }

                    Spacer()

                    Text(viewModel.filteredCategoriesDescription)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal)
                .padding(.bottom, 8)
            }

            Divider()
        }
        .background(Color(UIColor.systemBackground))
    }

    // MARK: - Batch Actions Bar

    private var batchActionsBar: some View {
        VStack(spacing: 0) {
            HStack {
                Text("\(viewModel.selectedCategoriesCount) selected")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Spacer()

                HStack(spacing: 16) {
                    // Merge button
                    if viewModel.canMergeCategories {
                        Button(action: {
                            showBatchActions = true
                        }) {
                            Label("Merge", systemImage: "arrow.merge")
                                .font(.caption)
                        }
                    }

                    // Delete button
                    Button(action: {
                        Task {
                            await batchDeleteSelectedCategories()
                        }
                    }) {
                        Label("Delete", systemImage: "trash")
                            .font(.caption)
                            .foregroundColor(.red)
                    }

                    // Clear selection
                    Button(action: {
                        viewModel.clearCategorySelections()
                    }) {
                        Label("Clear", systemImage: "xmark")
                            .font(.caption)
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)

            Divider()
        }
        .background(Color(UIColor.secondarySystemBackground))
        .transition(.move(edge: .top).combined(with: .opacity))
        .actionSheet(isPresented: $showBatchActions) {
            batchActionsSheet
        }
    }

    // MARK: - Empty State

    @ViewBuilder
    private var emptyState: some View {
        VStack(spacing: 24) {
            Image(systemName: "folder.badge.plus")
                .font(.system(size: 72))
                .foregroundStyle(
                    LinearGradient(
                        colors: [.blue, .purple],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            VStack(spacing: 12) {
                Text("No Categories")
                    .font(.title)
                    .fontWeight(.bold)

                Text("Create categories to organize your photos")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }

            Button(action: {
                viewModel.showCreateCategorySheet()
            }) {
                Label("Create First Category", systemImage: "plus.circle.fill")
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(
                        LinearGradient(
                            colors: [.blue, .purple],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .cornerRadius(25)
            }
            .scaleEffect(pulseAnimation ? 1.05 : 1.0)
            .animation(
                Animation.easeInOut(duration: 2.0)
                    .repeatForever(autoreverses: true),
                value: pulseAnimation
            )
            .onAppear {
                pulseAnimation = true
            }
        }
        .padding()
    }

    // MARK: - Loading Overlay

    private var loadingOverlay: some View {
        Color.black.opacity(0.4)
            .ignoresSafeArea()
            .overlay(
                VStack(spacing: 16) {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .scaleEffect(1.5)

                    if let status = viewModel.batchOperationStatus {
                        Text(status)
                            .font(.caption)
                            .foregroundColor(.white)
                    }

                    if viewModel.batchOperationProgress > 0 {
                        ProgressView(value: viewModel.batchOperationProgress)
                            .progressViewStyle(LinearProgressViewStyle(tint: .white))
                            .frame(width: 200)
                    }
                }
                .padding(24)
                .background(Color.black.opacity(0.8))
                .cornerRadius(12)
            )
    }

    // MARK: - Toolbar

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .navigationBarLeading) {
            if editMode == .active {
                Button("Select All") {
                    viewModel.selectAllCategories()
                }
            }
        }

        ToolbarItem(placement: .navigationBarTrailing) {
            HStack(spacing: 16) {
                // Edit mode toggle
                Button(editMode == .active ? "Done" : "Select") {
                    withAnimation {
                        editMode = editMode == .active ? .inactive : .active
                        if editMode == .inactive {
                            viewModel.clearCategorySelections()
                        }
                    }
                }

                // Add category button
                Button(action: {
                    viewModel.showCreateCategorySheet()
                }) {
                    Image(systemName: "plus")
                        .font(.title3)
                }
            }
        }
    }

    // MARK: - Delete Confirmation

    @ViewBuilder
    private var deleteConfirmationDialog: some View {
        if let category = viewModel.categoryToDelete {
            Button("Delete Category Only", role: .destructive) {
                Task {
                    await viewModel.confirmDeleteCategory()
                }
            }

            Button("Delete Category and Photos", role: .destructive) {
                Task {
                    await viewModel.confirmDeleteCategory(movePhotosTo: nil)
                }
            }

            Button("Cancel", role: .cancel) {
                viewModel.cancelDelete()
            }
        }
    }

    // MARK: - Batch Actions Sheet

    private var batchActionsSheet: ActionSheet {
        let selectedCategories = viewModel.categories.filter {
            viewModel.selectedCategoryIds.contains($0.id)
        }

        var buttons: [ActionSheet.Button] = []

        // Add merge options for each unselected category
        for category in viewModel.categories where !viewModel.selectedCategoryIds.contains(category.id) {
            buttons.append(.default(Text("Merge into \(category.displayName)")) {
                Task {
                    await viewModel.quickMergeSelectedCategories(into: category)
                    editMode = .inactive
                }
            })
        }

        buttons.append(.cancel())

        return ActionSheet(
            title: Text("Merge \(selectedCategories.count) Categories"),
            message: Text("Select target category"),
            buttons: buttons
        )
    }

    // MARK: - Helper Methods

    private func handleCategoryTap(_ category: Category) {
        if editMode == .active {
            viewModel.toggleCategorySelection(category.id)
        } else {
            viewModel.selectCategory(category)
        }
    }

    private func batchDeleteSelectedCategories() async {
        let selectedCategories = viewModel.categories.filter {
            viewModel.selectedCategoryIds.contains($0.id)
        }

        for category in selectedCategories {
            await viewModel.deleteCategory(category)
        }

        editMode = .inactive
    }
}

// MARK: - Enhanced Category Card with Drag & Drop

struct CategoryCardEnhanced: View {
    let categoryWithCount: CategoryWithCount
    let isSelected: Bool
    let editMode: EditMode
    @Binding var draggedCategory: Category?
    @Binding var dropTargetCategory: Category?
    let onTap: () -> Void
    let onEdit: (Category) -> Void
    let onDelete: (Category) -> Void
    let onDrop: ([Photo]) -> Void

    @State private var isTargeted = false

    var body: some View {
        HStack(spacing: 16) {
            // Selection indicator
            if editMode == .active {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundColor(isSelected ? .accentColor : .secondary)
                    .onTapGesture {
                        onTap()
                    }
            }

            // Category icon
            ZStack {
                Circle()
                    .fill(categoryWithCount.category.color)
                    .frame(width: 50, height: 50)

                if let iconName = categoryWithCount.category.iconResource {
                    Image(systemName: iconName)
                        .font(.system(size: 24))
                        .foregroundColor(.white)
                } else {
                    Text(String(categoryWithCount.category.displayName.prefix(1)))
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)
                }
            }

            // Category info
            VStack(alignment: .leading, spacing: 4) {
                Text(categoryWithCount.category.displayName)
                    .font(.headline)

                HStack(spacing: 8) {
                    Label("\(categoryWithCount.photoCount)", systemImage: "photo")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    if categoryWithCount.category.isDefault {
                        Label("Default", systemImage: "star.fill")
                            .font(.caption)
                            .foregroundColor(.orange)
                    }
                }
            }

            Spacer()

            // Actions
            if editMode == .inactive {
                HStack(spacing: 12) {
                    Button(action: { onEdit(categoryWithCount.category) }) {
                        Image(systemName: "pencil")
                            .font(.body)
                            .foregroundColor(.accentColor)
                    }

                    if !categoryWithCount.category.isDefault {
                        Button(action: { onDelete(categoryWithCount.category) }) {
                            Image(systemName: "trash")
                                .font(.body)
                                .foregroundColor(.red)
                        }
                    }
                }
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(UIColor.secondarySystemGroupedBackground))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(
                            isTargeted ? Color.accentColor : Color.clear,
                            lineWidth: 2
                        )
                )
        )
        .scaleEffect(isTargeted ? 1.02 : 1.0)
        .animation(.spring(response: 0.3), value: isTargeted)
        .onTapGesture {
            if editMode == .inactive {
                onTap()
            }
        }
        .draggable(categoryWithCount.category) {
            CategoryDragPreview(category: categoryWithCount.category)
                .onAppear {
                    draggedCategory = categoryWithCount.category
                }
        }
        .onDrop(of: [UTType.text], isTargeted: $isTargeted) { providers in
            dropTargetCategory = categoryWithCount.category
            onDrop([])
            return true
        }
    }
}

struct CategoryDragPreview: View {
    let category: Category

    var body: some View {
        HStack {
            Circle()
                .fill(category.color)
                .frame(width: 40, height: 40)
                .overlay(
                    Text(String(category.displayName.prefix(1)))
                        .font(.headline)
                        .foregroundColor(.white)
                )

            Text(category.displayName)
                .font(.headline)
        }
        .padding()
        .background(Color(UIColor.systemBackground))
        .cornerRadius(10)
        .shadow(radius: 5)
    }
}

struct FilterChip: View {
    let title: String
    let isSelected: Bool
    var systemImage: String? = nil
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 4) {
                if let systemImage = systemImage {
                    Image(systemName: systemImage)
                        .font(.caption)
                }
                Text(title)
                    .font(.caption)
                    .fontWeight(.medium)
            }
            .foregroundColor(isSelected ? .white : .primary)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                Capsule()
                    .fill(isSelected ? Color.accentColor : Color(UIColor.secondarySystemFill))
            )
        }
    }
}

// MARK: - Preview

struct CategoryManagementViewEnhanced_Previews: PreviewProvider {
    static var previews: some View {
        CategoryManagementViewEnhanced()
    }
}