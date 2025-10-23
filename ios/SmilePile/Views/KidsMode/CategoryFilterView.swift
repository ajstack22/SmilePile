import SwiftUI

/// Category filter component for Kids Mode
/// Matches Android CategoryFilterComponentKidsMode exactly
struct CategoryFilterView: View {
    let categories: [Category]
    let selectedCategory: Category?
    let onCategorySelected: (Category) -> Void
    let onExitKidsMode: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            // Filter chips (scrollable)
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

            // Lock button (fixed on right side)
            Button(action: onExitKidsMode) {
                Image(systemName: "lock.fill")
                    .font(.system(size: 24))
                    .foregroundColor(.white)
                    .frame(width: 48, height: 48)
                    .background(
                        ZStack {
                            Circle()
                                .fill(.ultraThinMaterial)
                            Circle()
                                .fill(Color.red.opacity(0.8))
                        }
                    )
                    .clipShape(Circle())
                    .shadow(color: .black.opacity(0.2), radius: 4, x: 0, y: 2)
            }
            .padding(.trailing, 8)
            .padding(.leading, 8)
            .accessibilityLabel("Exit Kids Mode")
        }
        .padding(.top, 50) // Fixed padding below Dynamic Island (matches AppHeaderComponent)
        .padding(.vertical, 8)
        .background(
            Color(UIColor.systemBackground)
                .ignoresSafeArea(edges: .top) // Background extends under safe area
        )
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
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

    // Match Android: white in dark mode, black in light mode at 10% opacity
    private var backgroundColor: Color {
        if isSelected {
            return isDarkMode ? Color.white.opacity(0.1) : Color.black.opacity(0.1)
        } else {
            return Color.clear
        }
    }

    // Match Android: primary label when selected, secondary when not
    private var textColor: Color {
        if isSelected {
            return Color(UIColor.label) // Full contrast text
        } else {
            return Color(UIColor.secondaryLabel) // Muted text
        }
    }

    // Match Android: solid border when selected, 30% opacity when not
    private var borderColor: Color {
        if isDarkMode {
            return isSelected ? Color.white : Color.white.opacity(0.3)
        } else {
            return isSelected ? Color.black : Color.black.opacity(0.3)
        }
    }

    // Match Android: Bold in dark mode (all chips), Medium when selected in light mode
    private var fontWeight: Font.Weight {
        if isDarkMode {
            return .bold
        } else if isSelected {
            return .medium
        } else {
            return .regular
        }
    }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 8) {
                // Category color dot (12pt diameter)
                Circle()
                    .fill(category.color)
                    .frame(width: 12, height: 12)
                    .overlay(
                        Circle()
                            .stroke(
                                isDarkMode ? Color.white.opacity(0.3) : Color.black.opacity(0.3),
                                lineWidth: 1
                            )
                    )

                // Category text
                Text(category.displayName)
                    .font(.system(size: 14, weight: fontWeight))
                    .foregroundColor(textColor)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(backgroundColor)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(borderColor, lineWidth: 1)
                    )
            )
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel("\(category.displayName) category")
        .accessibilityHint(isSelected ? "Currently selected" : "Tap to select")
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}