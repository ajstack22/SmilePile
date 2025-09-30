import SwiftUI

/// Category selection view for photo import with multi-select and quick assignment
struct CategorySelectionView: View {
    @ObservedObject var categoryManager: CategoryManager
    @Binding var selectedCategoryIds: Set<Int64>
    @Binding var isPresented: Bool
    @State private var searchText = ""
    @State private var showCreateCategory = false
    @State private var newCategoryName = ""
    @State private var newCategoryColor = "#4CAF50"

    // Configuration
    private let columns = [
        GridItem(.adaptive(minimum: 100), spacing: 12)
    ]

    var body: some View {
        NavigationStack {
            ZStack {
                Color(UIColor.systemGroupedBackground)
                    .ignoresSafeArea()

                VStack(spacing: 0) {
                    // Search bar
                    searchBar

                    if categoryManager.isLoading {
                        loadingView
                    } else if filteredCategories.isEmpty {
                        emptyStateView
                    } else {
                        categoryGrid
                    }
                }
            }
            .navigationTitle("Select Categories")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        isPresented = false
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        isPresented = false
                    }
                    .fontWeight(.semibold)
                    .disabled(selectedCategoryIds.isEmpty)
                }
            }
            .sheet(isPresented: $showCreateCategory) {
                CreateCategorySheet(
                    categoryManager: categoryManager,
                    isPresented: $showCreateCategory,
                    onCategoryCreated: { category in
                        selectedCategoryIds.insert(category.id)
                    }
                )
            }
        }
    }

    // MARK: - Subviews

    private var searchBar: some View {
        VStack(spacing: 0) {
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)

                TextField("Search categories", text: $searchText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())

                if !searchText.isEmpty {
                    Button(action: { searchText = "" }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding()

            // Quick actions bar
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    // Select All button
                    QuickActionChip(
                        title: "Select All",
                        icon: "checkmark.square.fill",
                        color: .blue
                    ) {
                        selectAllCategories()
                    }

                    // Clear button
                    if !selectedCategoryIds.isEmpty {
                        QuickActionChip(
                            title: "Clear (\(selectedCategoryIds.count))",
                            icon: "xmark.square.fill",
                            color: .red
                        ) {
                            selectedCategoryIds.removeAll()
                        }
                    }

                    // Add Category button
                    QuickActionChip(
                        title: "New Category",
                        icon: "plus.circle.fill",
                        color: .green
                    ) {
                        showCreateCategory = true
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 12)
            }

            Divider()
        }
        .background(Color(UIColor.systemBackground))
    }

    private var categoryGrid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(filteredCategories) { categoryWithCount in
                    CategorySelectionCard(
                        category: categoryWithCount.category,
                        photoCount: categoryWithCount.photoCount,
                        isSelected: selectedCategoryIds.contains(categoryWithCount.category.id),
                        onTap: {
                            toggleCategory(categoryWithCount.category.id)
                        }
                    )
                }
            }
            .padding()
        }
    }

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
            Text("Loading categories...")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Image(systemName: "square.stack.badge.plus")
                .font(.system(size: 60))
                .foregroundColor(.secondary)

            VStack(spacing: 8) {
                Text("No Categories")
                    .font(.title2)
                    .fontWeight(.semibold)

                Text("Create your first category to organize photos")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }

            Button(action: { showCreateCategory = true }) {
                Label("Create Category", systemImage: "plus.circle.fill")
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.accentColor)
                    .cornerRadius(25)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }

    // MARK: - Computed Properties

    private var filteredCategories: [CategoryWithCount] {
        if searchText.isEmpty {
            return categoryManager.categoriesWithCounts
        }
        return categoryManager.searchCategories(query: searchText)
    }

    // MARK: - Methods

    private func toggleCategory(_ categoryId: Int64) {
        if selectedCategoryIds.contains(categoryId) {
            selectedCategoryIds.remove(categoryId)
        } else {
            selectedCategoryIds.insert(categoryId)
        }
    }

    private func selectAllCategories() {
        selectedCategoryIds = Set(filteredCategories.map { $0.category.id })
    }
}

// MARK: - Supporting Views

struct CategorySelectionCard: View {
    let category: Category
    let photoCount: Int
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                // Icon and selection indicator
                ZStack {
                    Circle()
                        .fill(Color(hex: category.colorHex ?? "#4CAF50") ?? .green)
                        .frame(width: 60, height: 60)

                    if let iconName = category.iconResource {
                        Image(systemName: iconName)
                            .font(.system(size: 28))
                            .foregroundColor(.white)
                    } else {
                        Text(String(category.displayName.prefix(1)))
                            .font(.system(size: 28, weight: .semibold))
                            .foregroundColor(.white)
                    }

                    // Selection checkmark
                    if isSelected {
                        Circle()
                            .fill(Color.white)
                            .frame(width: 24, height: 24)
                            .overlay(
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 24))
                                    .foregroundColor(.green)
                            )
                            .offset(x: 24, y: -24)
                    }
                }

                // Category name
                Text(category.displayName)
                    .font(.caption)
                    .fontWeight(.medium)
                    .lineLimit(1)
                    .foregroundColor(.primary)

                // Photo count
                Text("\(photoCount) photos")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            .padding(8)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(UIColor.secondarySystemGroupedBackground))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(isSelected ? Color.accentColor : Color.clear, lineWidth: 2)
                    )
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct QuickActionChip: View {
    let title: String
    let icon: String
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: icon)
                .font(.footnote)
                .fontWeight(.medium)
                .foregroundColor(color)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    Capsule()
                        .fill(color.opacity(0.15))
                )
        }
    }
}

struct CreateCategorySheet: View {
    @ObservedObject var categoryManager: CategoryManager
    @Binding var isPresented: Bool
    let onCategoryCreated: ((Category) -> Void)?

    @State private var categoryName = ""
    @State private var selectedColor = CategoryManager.Configuration.defaultColors[0]
    @State private var selectedIcon: String? = nil
    @State private var isCreating = false
    @State private var errorMessage: String?

    private let colorColumns = [
        GridItem(.adaptive(minimum: 50), spacing: 12)
    ]

    private let iconColumns = [
        GridItem(.adaptive(minimum: 50), spacing: 12)
    ]

    init(categoryManager: CategoryManager,
         isPresented: Binding<Bool>,
         onCategoryCreated: ((Category) -> Void)? = nil) {
        self.categoryManager = categoryManager
        self._isPresented = isPresented
        self.onCategoryCreated = onCategoryCreated
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Category Name", text: $categoryName)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                } header: {
                    Text("Name")
                }

                Section {
                    LazyVGrid(columns: colorColumns, spacing: 12) {
                        ForEach(CategoryManager.Configuration.defaultColors, id: \.self) { colorHex in
                            ColorSelectionCircle(
                                colorHex: colorHex,
                                isSelected: selectedColor == colorHex,
                                onTap: {
                                    selectedColor = colorHex
                                }
                            )
                        }
                    }
                    .padding(.vertical, 8)
                } header: {
                    Text("Color")
                }

                Section {
                    LazyVGrid(columns: iconColumns, spacing: 12) {
                        ForEach(CategoryManager.Configuration.defaultIcons, id: \.self) { iconName in
                            IconSelectionButton(
                                iconName: iconName,
                                color: Color(hex: selectedColor) ?? .green,
                                isSelected: selectedIcon == iconName,
                                onTap: {
                                    selectedIcon = iconName
                                }
                            )
                        }
                    }
                    .padding(.vertical, 8)
                } header: {
                    Text("Icon (Optional)")
                }

                if let errorMessage = errorMessage {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                            .font(.caption)
                    }
                }
            }
            .navigationTitle("New Category")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        isPresented = false
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Create") {
                        createCategory()
                    }
                    .fontWeight(.semibold)
                    .disabled(categoryName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isCreating)
                }
            }
            .disabled(isCreating)
            .overlay {
                if isCreating {
                    Color.black.opacity(0.3)
                        .ignoresSafeArea()
                        .overlay {
                            ProgressView("Creating...")
                                .padding(20)
                                .background(Color(UIColor.systemBackground))
                                .cornerRadius(10)
                        }
                }
            }
        }
    }

    private func createCategory() {
        let trimmedName = categoryName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedName.isEmpty else { return }

        isCreating = true
        errorMessage = nil

        Task { @MainActor in
            do {
                let category = try await categoryManager.createCategory(
                    name: trimmedName,
                    colorHex: selectedColor,
                    icon: selectedIcon
                )

                onCategoryCreated?(category)
                isPresented = false
            } catch {
                errorMessage = error.localizedDescription
            }
            isCreating = false
        }
    }
}

struct ColorSelectionCircle: View {
    let colorHex: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Circle()
                .fill(Color(hex: colorHex) ?? .gray)
                .frame(width: 44, height: 44)
                .overlay(
                    Circle()
                        .stroke(Color.primary, lineWidth: isSelected ? 3 : 0)
                )
                .overlay(
                    Image(systemName: "checkmark")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .opacity(isSelected ? 1 : 0)
                )
        }
    }
}

struct IconSelectionButton: View {
    let iconName: String
    let color: Color
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(isSelected ? color.opacity(0.2) : Color(UIColor.tertiarySystemFill))
                    .frame(width: 44, height: 44)

                Image(systemName: iconName)
                    .font(.system(size: 20))
                    .foregroundColor(isSelected ? color : .secondary)
            }
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(isSelected ? color : Color.clear, lineWidth: 2)
            )
        }
    }
}

// MARK: - Previews

struct CategorySelectionView_Previews: PreviewProvider {
    static var previews: some View {
        CategorySelectionView(
            categoryManager: CategoryManager(),
            selectedCategoryIds: .constant(Set()),
            isPresented: .constant(true)
        )
    }
}