import SwiftUI

/// Category filter component for Kids Mode
/// Matches Android CategoryFilterComponentKidsMode exactly
struct CategoryFilterView: View {
    let categories: [Category]
    let selectedCategory: Category?
    let onCategorySelected: (Category) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(categories) { category in
                    KidsCategoryChip(
                        category: category,
                        isSelected: selectedCategory?.id == category.id,
                        onTap: {
                            // Always select a category, never allow nil (no "All Photos" state)
                            onCategorySelected(category)
                        }
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .frame(height: 56) // Consistent height for the filter bar
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Category filters")
    }
}

// MARK: - Category Chip

private struct KidsCategoryChip: View {
    let category: Category
    let isSelected: Bool
    let onTap: () -> Void

    @Environment(\.colorScheme) var colorScheme

    private var isDarkMode: Bool {
        colorScheme == .dark
    }

    private var backgroundColor: Color {
        if isSelected {
            return category.color
        } else if isDarkMode {
            return Color.black
        } else {
            return Color(UIColor.secondarySystemBackground)
        }
    }

    private var textColor: Color {
        if isSelected || isDarkMode {
            return .white
        } else {
            return category.color
        }
    }

    private var borderColor: Color {
        isSelected ? Color.clear : category.color
    }

    private var fontWeight: Font.Weight {
        if isDarkMode {
            return .heavy
        } else if isSelected {
            return .bold
        } else {
            return .regular
        }
    }

    var body: some View {
        Button(action: onTap) {
            Text(category.displayName)
                .font(.system(size: 14, weight: fontWeight))
                .foregroundColor(textColor)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(
                    Capsule()
                        .fill(backgroundColor)
                        .overlay(
                            Capsule()
                                .stroke(borderColor, lineWidth: 1.5)
                        )
                )
                .shadow(color: isSelected ? .black.opacity(0.1) : .clear, radius: 1, y: 1)
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel("\(category.displayName) category")
        .accessibilityHint(isSelected ? "Currently selected" : "Tap to select")
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}