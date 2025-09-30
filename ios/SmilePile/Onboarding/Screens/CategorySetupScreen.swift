import SwiftUI

struct CategorySetupScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator
    @State private var newCategoryName = ""
    @State private var selectedColor = "#4CAF50"
    @State private var showingColorPicker = false

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
            // Instructions
            VStack(spacing: 16) {
                Image(systemName: "square.stack")
                    .font(.system(size: 48))
                    .foregroundColor(.smilePileOrange)
                    .padding(.bottom, 16)

                Text("Create Piles")
                    .font(.nunito(22, weight: .bold))

                Text("Organize your photos into colorful piles")
                    .font(.nunito(14, weight: .regular))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding()

            ScrollView {
                VStack(spacing: 20) {
                    // Quick add suggestions (hide when at max capacity)
                    if !suggestedCategories.isEmpty && coordinator.onboardingData.categories.count < 5 {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Or Quick Add")
                                .font(.nunito(16, weight: .medium))
                                .padding(.horizontal)

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
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
                                .padding(.horizontal)
                            }
                        }
                    }

                    // Custom category creation (hide when at max capacity)
                    if coordinator.onboardingData.categories.count < 5 {
                        VStack(alignment: .leading, spacing: 12) {
                        Text("Create Your Own")
                            .font(.nunito(16, weight: .medium))
                            .padding(.horizontal)

                        VStack(spacing: 16) {
                            // Name input
                            HStack {
                                TextField("Pile name", text: $newCategoryName)
                                    .textFieldStyle(RoundedBorderTextFieldStyle())

                                // Color picker button
                                Button(action: {
                                    showingColorPicker.toggle()
                                }) {
                                    Circle()
                                        .fill(Color(hex: selectedColor))
                                        .frame(width: 40, height: 40)
                                        .overlay(
                                            Circle()
                                                .stroke(Color.gray.opacity(0.3), lineWidth: 2)
                                        )
                                }
                            }

                            // Color palette (shown when picker is active)
                            if showingColorPicker {
                                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 6), spacing: 12) {
                                    ForEach(colorOptions, id: \.self) { color in
                                        Circle()
                                            .fill(Color(hex: color))
                                            .frame(width: 44, height: 44)
                                            .overlay(
                                                ZStack {
                                                    if selectedColor == color {
                                                        Circle()
                                                            .stroke(Color.smilePileBlue, lineWidth: 3)
                                                        Image(systemName: "checkmark")
                                                            .font(.system(size: 20))
                                                            .foregroundColor(.white)
                                                    }
                                                }
                                            )
                                            .onTapGesture {
                                                selectedColor = color
                                                showingColorPicker = false
                                            }
                                    }
                                }
                                .padding(.vertical, 8)
                            }

                            // Add button
                            Button(action: {
                                addCustomCategory()
                            }) {
                                Text("Add Pile")
                                    .font(.nunito(16, weight: .medium))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(
                                        newCategoryName.isEmpty ?
                                        Color.gray.opacity(0.3) :
                                        Color.smilePileBlue
                                    )
                                    .cornerRadius(8)
                            }
                            .disabled(newCategoryName.isEmpty)
                        }
                        .padding()
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color.gray.opacity(0.1))
                        )
                        .padding(.horizontal)
                    }
                    }

                    // Created categories
                    if !coordinator.onboardingData.categories.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("Your Piles")
                                    .font(.nunito(18, weight: .semibold))

                                Spacer()

                                Text("\(coordinator.onboardingData.categories.count)/5")
                                    .font(.nunito(14, weight: .regular))
                                    .foregroundColor(.secondary)
                            }
                            .padding(.horizontal)

                            // Show max reached message
                            if coordinator.onboardingData.categories.count >= 5 {
                                Text("Maximum of 5 piles reached")
                                    .font(.nunito(12, weight: .regular))
                                    .foregroundColor(.secondary)
                                    .padding(.horizontal)
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
                            .padding(.horizontal)
                        }
                    }
                }
                .padding(.vertical)
            }

            // Continue button
            VStack(spacing: 16) {
                if coordinator.onboardingData.categories.isEmpty {
                    Text("Add at least one pile to continue")
                        .font(.nunito(12, weight: .regular))
                        .foregroundColor(.secondary)
                }

                Button(action: {
                    coordinator.navigateToNext()
                }) {
                    Text("Continue")
                        .font(.nunito(18, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            coordinator.onboardingData.categories.isEmpty ?
                            Color.gray.opacity(0.3) :
                            Color.smilePileBlue
                        )
                        .cornerRadius(12)
                }
                .disabled(coordinator.onboardingData.categories.isEmpty)
            }
            .padding()
        }
    }

    private func addCategory(name: String, colorHex: String, icon: String?) {
        // Enforce maximum of 5 piles
        guard coordinator.onboardingData.categories.count < 5 else {
            // Could add an alert here if needed
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
            // Maximum reached, could show alert
            return
        }
        addCategory(name: newCategoryName, colorHex: selectedColor, icon: nil)
        newCategoryName = ""
        showingColorPicker = false
    }

    private func removeCategory(_ category: TempCategory) {
        coordinator.onboardingData.categories.removeAll { $0.id == category.id }
    }
}

struct SuggestedCategoryCard: View {
    let name: String
    let colorHex: String
    let onAdd: () -> Void

    var body: some View {
        Button(action: onAdd) {
            HStack {
                Image(systemName: "plus")
                    .font(.system(size: 20))
                    .foregroundColor(Color(hex: colorHex))

                Text(name)
                    .font(.nunito(16, weight: .medium))
                    .foregroundColor(.primary)
            }
            .padding(.horizontal, 20)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(hex: colorHex).opacity(0.15))
            )
        }
    }
}

struct CreatedCategoryRow: View {
    let category: TempCategory
    let onRemove: () -> Void

    var body: some View {
        HStack {
            Circle()
                .fill(Color(hex: category.colorHex))
                .frame(width: 16, height: 16)

            if let icon = category.icon {
                Text(icon)
                    .font(.nunito(20, weight: .regular))
            }

            Text(category.name)
                .font(.nunito(16, weight: .regular))
                .foregroundColor(.primary)

            Spacer()

            Button(action: onRemove) {
                Image(systemName: "xmark.circle.fill")
                    .font(.system(size: 24))
                    .foregroundColor(.gray.opacity(0.5))
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(hex: category.colorHex).opacity(0.1))
        )
    }
}