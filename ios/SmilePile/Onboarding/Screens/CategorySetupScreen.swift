import SwiftUI

struct CategorySetupScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator
    @State private var newCategoryName = ""
    @State private var selectedColor = "#4CAF50"
    @State private var showingColorPicker = false

    // Predefined categories for quick setup
    let suggestedCategories = [
        ("Family", "#FF6B6B", "👨‍👩‍👧‍👦"),
        ("Friends", "#4ECDC4", "👫"),
        ("Vacation", "#45B7D1", "🏖️"),
        ("Pets", "#96CEB4", "🐾"),
        ("Fun", "#FFEAA7", "🎉"),
        ("School", "#DDA0DD", "🎒")
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
            VStack(spacing: 8) {
                Text("Create Categories")
                    .font(.title2)
                    .fontWeight(.bold)

                Text("Organize your photos into colorful categories")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding()

            ScrollView {
                VStack(spacing: 20) {
                    // Quick add suggestions
                    if !suggestedCategories.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Quick Add")
                                .font(.headline)
                                .padding(.horizontal)

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(suggestedCategories, id: \.0) { category in
                                        // Only show if not already added
                                        if !coordinator.onboardingData.categories.contains(where: { $0.name == category.0 }) {
                                            SuggestedCategoryCard(
                                                name: category.0,
                                                colorHex: category.1,
                                                icon: category.2,
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

                    // Custom category creation
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Create Custom")
                            .font(.headline)
                            .padding(.horizontal)

                        VStack(spacing: 16) {
                            // Name input
                            HStack {
                                TextField("Category name", text: $newCategoryName)
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
                                                Circle()
                                                    .stroke(selectedColor == color ? Color.primary : Color.clear, lineWidth: 3)
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
                                Text("Add Category")
                                    .font(.subheadline)
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(
                                        newCategoryName.isEmpty ?
                                        Color.gray.opacity(0.3) :
                                        Color(red: 1.0, green: 0.42, blue: 0.42)
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

                    // Created categories
                    if !coordinator.onboardingData.categories.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Text("Your Categories")
                                    .font(.headline)

                                Spacer()

                                Text("\(coordinator.onboardingData.categories.count)/5")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .padding(.horizontal)

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
                    Text("Add at least one category to continue")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Button(action: {
                    coordinator.navigateToNext()
                }) {
                    Text("Continue")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(
                            coordinator.onboardingData.categories.isEmpty ?
                            Color.gray.opacity(0.3) :
                            Color(red: 1.0, green: 0.42, blue: 0.42)
                        )
                        .cornerRadius(12)
                }
                .disabled(coordinator.onboardingData.categories.isEmpty)
            }
            .padding()
        }
    }

    private func addCategory(name: String, colorHex: String, icon: String?) {
        guard coordinator.onboardingData.categories.count < 5 else { return }

        let category = TempCategory(
            name: name,
            colorHex: colorHex,
            icon: icon
        )
        coordinator.onboardingData.categories.append(category)
    }

    private func addCustomCategory() {
        guard !newCategoryName.isEmpty else { return }
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
    let icon: String
    let onAdd: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(hex: colorHex).opacity(0.2))
                    .frame(width: 80, height: 80)

                Text(icon)
                    .font(.largeTitle)
            }

            Text(name)
                .font(.caption)
                .fontWeight(.medium)

            Button(action: onAdd) {
                Image(systemName: "plus.circle.fill")
                    .foregroundColor(Color(hex: colorHex))
                    .font(.title2)
            }
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
                .frame(width: 12, height: 12)

            if let icon = category.icon {
                Text(icon)
                    .font(.title3)
            }

            Text(category.name)
                .font(.body)

            Spacer()

            Button(action: onRemove) {
                Image(systemName: "xmark.circle.fill")
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