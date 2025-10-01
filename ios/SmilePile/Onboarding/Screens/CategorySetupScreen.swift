import SwiftUI

struct CategorySetupScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator
    @State private var newCategoryName = ""
    @State private var selectedColor = "#4CAF50"

    // Predefined categories for quick setup (only 3 to match Android)
    let suggestedCategories: [(String, String, String?)] = [
        ("Family", "#FF6B6B", nil),  // No emoji
        ("Friends", "#4ECDC4", nil),
        ("Fun", "#FFEAA7", nil)
    ]

    // Color palette
    let colorOptions = [
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
        "#FFEAA7", "#DDA0DD", "#FFA07A", "#98D8C8",
        "#F7DC6F", "#BB8FCE", "#85C1E2", "#F8B739"
    ]

    var body: some View {
        VStack(spacing: 0) {
            // Top icon and instructions
            VStack(spacing: 8) {
                Image(systemName: "square.stack")
                    .font(.system(size: 48))
                    .foregroundColor(.smilePileOrange)

                Spacer().frame(height: 8)

                Text("Organize your photos into colorful piles")
                    .font(.custom("Nunito-Regular", size: 14))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(.top, 16)
            .padding(.bottom, 24)

            ScrollView {
                VStack(spacing: 24) {
                    // SECTION 1: Your Piles (shown when categories exist)
                    if !coordinator.onboardingData.categories.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("Your Piles")
                                    .font(.custom("Nunito-SemiBold", size: 18))

                                Spacer()

                                Text("\(coordinator.onboardingData.categories.count)/5")
                                    .font(.custom("Nunito-Regular", size: 14))
                                    .foregroundColor(.secondary)
                            }

                            VStack(spacing: 8) {
                                ForEach(coordinator.onboardingData.categories, id: \.id) { category in
                                    CreatedCategoryRow(
                                        category: category,
                                        onRemove: {
                                            removeCategory(category)
                                        }
                                    )
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }

                    // SECTION 2: Create Your Own
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Create Your Own")
                            .font(.custom("Nunito-Medium", size: 16))
                            .foregroundColor(.secondary)

                        // Text field with inline + button
                        HStack(spacing: 0) {
                            TextField("Custom pile name", text: $newCategoryName)
                                .padding(.leading, 16)
                                .padding(.vertical, 12)
                                .background(Color.clear)

                            // Inline + button as trailing icon
                            Button(action: {
                                addCustomCategory()
                            }) {
                                Image(systemName: "plus")
                                    .font(.system(size: 20))
                                    .foregroundColor(
                                        (newCategoryName.isNotEmpty && coordinator.onboardingData.categories.count < 5) ?
                                        Color(hex: "#2196F3") :
                                        Color.gray.opacity(0.4)
                                    )
                                    .frame(width: 44, height: 44)
                            }
                            .disabled(newCategoryName.isEmpty || coordinator.onboardingData.categories.count >= 5)
                        }
                        .overlay(
                            RoundedRectangle(cornerRadius: 4)
                                .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                        )

                        // Color picker - always visible horizontal scroll
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 12) {
                                ForEach(colorOptions, id: \.self) { color in
                                    Circle()
                                        .fill(Color(hex: color))
                                        .frame(width: 44, height: 44)
                                        .overlay(
                                            ZStack {
                                                Circle()
                                                    .stroke(
                                                        selectedColor == color ? Color(hex: "#2196F3") : Color.gray.opacity(0.3),
                                                        lineWidth: selectedColor == color ? 3 : 1
                                                    )
                                                if selectedColor == color {
                                                    Image(systemName: "checkmark")
                                                        .font(.system(size: 24))
                                                        .foregroundColor(.white)
                                                }
                                            }
                                        )
                                        .onTapGesture {
                                            selectedColor = color
                                        }
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 16)

                    // SECTION 3: Or Quick Add
                    if !suggestedCategories.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Or Quick Add")
                                .font(.custom("Nunito-Medium", size: 16))
                                .foregroundColor(.secondary)

                            VStack(spacing: 8) {
                                ForEach(suggestedCategories, id: \.0) { category in
                                    // Only show if not already added
                                    if !coordinator.onboardingData.categories.contains(where: { $0.name == category.0 }) {
                                        SuggestedCategoryCard(
                                            name: category.0,
                                            colorHex: category.1,
                                            onAdd: {
                                                addCategory(name: category.0, colorHex: category.1, icon: category.2)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
                .padding(.vertical)
            }

            // Continue button
            VStack(spacing: 8) {
                if coordinator.onboardingData.categories.isEmpty {
                    Text("Add at least one pile to continue")
                        .font(.custom("Nunito-Regular", size: 12))
                        .foregroundColor(.secondary)
                }

                Button(action: {
                    coordinator.navigateToNext()
                }) {
                    Text("Continue")
                        .font(.custom("Nunito-Bold", size: 18))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            coordinator.onboardingData.categories.isEmpty ?
                            Color.gray.opacity(0.3) :
                            Color(hex: "#2196F3")
                        )
                        .cornerRadius(8)
                }
                .disabled(coordinator.onboardingData.categories.isEmpty)
            }
            .padding(16)
        }
    }

    private func addCategory(name: String, colorHex: String, icon: String?) {
        // Enforce maximum of 5 piles
        guard coordinator.onboardingData.categories.count < 5 else {
            return
        }

        let category = TempCategory(
            name: name,
            colorHex: colorHex,
            icon: icon
        )
        coordinator.onboardingData.categories.append(category)
    }

    private func addCustomCategory() {
        guard !newCategoryName.isEmpty else { return }
        guard coordinator.onboardingData.categories.count < 5 else {
            return
        }
        addCategory(name: newCategoryName, colorHex: selectedColor, icon: nil)
        newCategoryName = ""
    }

    private func removeCategory(_ category: TempCategory) {
        coordinator.onboardingData.categories.removeAll { $0.id == category.id }
    }
}

// MARK: - Suggested Category Card (Vertical, Full Width)
struct SuggestedCategoryCard: View {
    let name: String
    let colorHex: String
    let onAdd: () -> Void

    var body: some View {
        Button(action: onAdd) {
            HStack(spacing: 12) {
                // Color dot on left
                HStack(spacing: 8) {
                    Circle()
                        .fill(Color(hex: colorHex))
                        .frame(width: 16, height: 16)

                    Text(name)
                        .font(.custom("Nunito-Medium", size: 14))
                        .foregroundColor(.primary)
                }

                Spacer()

                // + icon on right
                Image(systemName: "plus")
                    .font(.system(size: 20))
                    .foregroundColor(Color(hex: colorHex))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(hex: colorHex).opacity(0.15))
            )
        }
    }
}

// MARK: - Created Category Row
struct CreatedCategoryRow: View {
    let category: TempCategory
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(Color(hex: category.colorHex))
                .frame(width: 16, height: 16)

            if let icon = category.icon {
                Text(icon)
                    .font(.custom("Nunito-Regular", size: 20))
            }

            Text(category.name)
                .font(.custom("Nunito-Regular", size: 16))
                .foregroundColor(.primary)

            Spacer()

            Button(action: onRemove) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 24))
                    .foregroundColor(.gray.opacity(0.5))
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(hex: category.colorHex).opacity(0.1))
        )
    }
}

// MARK: - String Helper
extension String {
    var isNotEmpty: Bool {
        !self.isEmpty
    }
}
