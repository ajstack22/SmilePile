import SwiftUI

/// Horizontal scrollable category filter for photo gallery
struct CategoryFilterView: View {
    @ObservedObject var categoryManager: CategoryManager
    @Binding var selectedCategory: Category?
    @State private var scrollViewProxy: ScrollViewProxy?

    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        // All Photos chip
                        CategoryFilterChip(
                            title: "All Photos",
                            icon: "photo.stack",
                            color: .blue,
                            photoCount: totalPhotoCount,
                            isSelected: selectedCategory == nil,
                            onTap: {
                                withAnimation(.spring()) {
                                    selectedCategory = nil
                                }
                            }
                        )
                        .id("all")

                        // Category chips
                        ForEach(categoryManager.categoriesWithCounts) { categoryWithCount in
                            CategoryFilterChip(
                                title: categoryWithCount.category.displayName,
                                icon: categoryWithCount.category.iconResource,
                                color: categoryWithCount.category.color,
                                photoCount: categoryWithCount.photoCount,
                                isSelected: selectedCategory?.id == categoryWithCount.category.id,
                                onTap: {
                                    withAnimation(.spring()) {
                                        if selectedCategory?.id == categoryWithCount.category.id {
                                            selectedCategory = nil
                                        } else {
                                            selectedCategory = categoryWithCount.category
                                        }
                                    }
                                }
                            )
                            .id(categoryWithCount.category.id)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                }
                .background(
                    Color(UIColor.systemBackground)
                        .shadow(color: Color.black.opacity(0.05), radius: 2, y: 2)
                )
                .onAppear {
                    scrollViewProxy = proxy
                }
                .onChange(of: selectedCategory) { newValue in
                    withAnimation {
                        if let category = newValue {
                            proxy.scrollTo(category.id, anchor: .center)
                        } else {
                            proxy.scrollTo("all", anchor: .leading)
                        }
                    }
                }
            }

            // Active filter indicator
            if let selectedCategory = selectedCategory {
                ActiveFilterBar(
                    category: selectedCategory,
                    photoCount: getPhotoCount(for: selectedCategory),
                    onClear: {
                        withAnimation(.spring()) {
                            self.selectedCategory = nil
                        }
                    }
                )
            }
        }
    }

    private var totalPhotoCount: Int {
        categoryManager.categoriesWithCounts.reduce(0) { $0 + $1.photoCount }
    }

    private func getPhotoCount(for category: Category) -> Int {
        categoryManager.categoriesWithCounts
            .first(where: { $0.category.id == category.id })?
            .photoCount ?? 0
    }
}

// MARK: - Filter Chip

struct CategoryFilterChip: View {
    let title: String
    let icon: String?
    let color: Color
    let photoCount: Int
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                // Icon or initial
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 14, weight: .semibold))
                } else {
                    Text(String(title.prefix(1)))
                        .font(.system(size: 14, weight: .bold))
                }

                // Title
                Text(title)
                    .font(.system(size: 14, weight: .medium))

                // Photo count badge
                Text("(\(photoCount))")
                    .font(.caption2)
                    .fontWeight(.medium)
                    .opacity(0.8)
            }
            .foregroundColor(isSelected ? .white : color)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(isSelected ? color : color.opacity(0.15))
            )
            .overlay(
                Capsule()
                    .stroke(color, lineWidth: isSelected ? 0 : 1)
            )
            .scaleEffect(isSelected ? 1.05 : 1.0)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Active Filter Bar

struct ActiveFilterBar: View {
    let category: Category
    let photoCount: Int
    let onClear: () -> Void

    var body: some View {
        HStack {
            HStack(spacing: 8) {
                Circle()
                    .fill(category.color)
                    .frame(width: 8, height: 8)

                Text("Filtering: \(category.displayName)")
                    .font(.caption)
                    .fontWeight(.medium)

                Text("• \(photoCount) photos")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Button(action: onClear) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 16))
                    .foregroundColor(.secondary)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
        .background(
            Color(UIColor.secondarySystemBackground)
        )
        .transition(.move(edge: .top).combined(with: .opacity))
    }
}

// MARK: - Multi-Select Category Filter

struct MultiSelectCategoryFilterView: View {
    @ObservedObject var categoryManager: CategoryManager
    @Binding var selectedCategoryIds: Set<Int64>
    @State private var showAllCategories = false

    private let visibleCategoryCount = 5

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                Text("Filter by Categories")
                    .font(.headline)

                Spacer()

                if !selectedCategoryIds.isEmpty {
                    Button("Clear") {
                        withAnimation {
                            selectedCategoryIds.removeAll()
                        }
                    }
                    .font(.caption)
                    .foregroundColor(.accentColor)
                }
            }
            .padding(.horizontal)

            // Category chips
            FlowLayout(spacing: 8) {
                // Show limited categories or all
                let categoriesToShow = showAllCategories
                    ? categoryManager.categoriesWithCounts
                    : Array(categoryManager.categoriesWithCounts.prefix(visibleCategoryCount))

                ForEach(categoriesToShow) { categoryWithCount in
                    MultiSelectCategoryChip(
                        category: categoryWithCount.category,
                        photoCount: categoryWithCount.photoCount,
                        isSelected: selectedCategoryIds.contains(categoryWithCount.category.id),
                        onTap: {
                            withAnimation(.spring()) {
                                if selectedCategoryIds.contains(categoryWithCount.category.id) {
                                    selectedCategoryIds.remove(categoryWithCount.category.id)
                                } else {
                                    selectedCategoryIds.insert(categoryWithCount.category.id)
                                }
                            }
                        }
                    )
                }

                // Show more/less button
                if categoryManager.categoriesWithCounts.count > visibleCategoryCount {
                    Button(action: {
                        withAnimation {
                            showAllCategories.toggle()
                        }
                    }) {
                        HStack(spacing: 4) {
                            Text(showAllCategories ? "Show Less" : "Show More")
                                .font(.caption)
                            Image(systemName: showAllCategories ? "chevron.up" : "chevron.down")
                                .font(.caption2)
                        }
                        .foregroundColor(.accentColor)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(
                            Capsule()
                                .fill(Color.accentColor.opacity(0.1))
                        )
                    }
                }
            }
            .padding(.horizontal)

            // Selected categories summary
            if !selectedCategoryIds.isEmpty {
                SelectedCategoriesSummary(
                    categoryManager: categoryManager,
                    selectedIds: selectedCategoryIds
                )
            }
        }
        .padding(.vertical, 8)
        .background(Color(UIColor.systemBackground))
    }
}

struct MultiSelectCategoryChip: View {
    let category: Category
    let photoCount: Int
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                // Selection indicator
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 14))
                    .foregroundColor(isSelected ? .white : category.color)

                // Category name
                Text(category.displayName)
                    .font(.system(size: 13, weight: .medium))

                // Photo count
                Text("\(photoCount)")
                    .font(.caption2)
                    .fontWeight(.semibold)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(
                        Capsule()
                            .fill(isSelected ? Color.white.opacity(0.3) : category.color.opacity(0.2))
                    )
            }
            .foregroundColor(isSelected ? .white : .primary)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                Capsule()
                    .fill(isSelected ? category.color : Color(UIColor.secondarySystemFill))
            )
            .overlay(
                Capsule()
                    .stroke(category.color, lineWidth: isSelected ? 0 : 1)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct SelectedCategoriesSummary: View {
    @ObservedObject var categoryManager: CategoryManager
    let selectedIds: Set<Int64>

    private var selectedCategories: [Category] {
        categoryManager.categories.filter { selectedIds.contains($0.id) }
    }

    private var totalPhotos: Int {
        categoryManager.categoriesWithCounts
            .filter { selectedIds.contains($0.category.id) }
            .reduce(0) { $0 + $1.photoCount }
    }

    var body: some View {
        HStack {
            // Category dots
            HStack(spacing: -4) {
                ForEach(Array(selectedCategories.prefix(5)), id: \.id) { category in
                    Circle()
                        .fill(category.color)
                        .frame(width: 20, height: 20)
                        .overlay(
                            Circle()
                                .stroke(Color.white, lineWidth: 2)
                        )
                }

                if selectedCategories.count > 5 {
                    Circle()
                        .fill(Color.gray)
                        .frame(width: 20, height: 20)
                        .overlay(
                            Text("+\(selectedCategories.count - 5)")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(.white)
                        )
                        .overlay(
                            Circle()
                                .stroke(Color.white, lineWidth: 2)
                        )
                }
            }

            Text("\(selectedCategories.count) categories • \(totalPhotos) photos")
                .font(.caption)
                .foregroundColor(.secondary)

            Spacer()
        }
        .padding(.horizontal)
        .padding(.vertical, 8)
        .background(Color(UIColor.secondarySystemBackground))
    }
}

// MARK: - Flow Layout

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(
            in: proposal.width ?? 0,
            subviews: subviews,
            spacing: spacing
        )
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(
            in: bounds.width,
            subviews: subviews,
            spacing: spacing
        )

        for (index, subview) in subviews.enumerated() {
            subview.place(
                at: CGPoint(
                    x: bounds.minX + result.positions[index].x,
                    y: bounds.minY + result.positions[index].y
                ),
                proposal: ProposedViewSize(result.sizes[index])
            )
        }
    }

    struct FlowResult {
        var size: CGSize = .zero
        var positions: [CGPoint] = []
        var sizes: [CGSize] = []

        init(in width: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var currentX: CGFloat = 0
            var currentY: CGFloat = 0
            var lineHeight: CGFloat = 0

            for subview in subviews {
                let size = subview.sizeThatFits(.unspecified)

                if currentX + size.width > width && currentX > 0 {
                    currentX = 0
                    currentY += lineHeight + spacing
                    lineHeight = 0
                }

                positions.append(CGPoint(x: currentX, y: currentY))
                sizes.append(size)

                lineHeight = max(lineHeight, size.height)
                currentX += size.width + spacing
            }

            self.size = CGSize(width: width, height: currentY + lineHeight)
        }
    }
}

// MARK: - Previews

struct CategoryFilterView_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            CategoryFilterView(
                categoryManager: CategoryManager(),
                selectedCategory: .constant(nil)
            )

            Spacer()

            MultiSelectCategoryFilterView(
                categoryManager: CategoryManager(),
                selectedCategoryIds: .constant(Set())
            )
        }
    }
}