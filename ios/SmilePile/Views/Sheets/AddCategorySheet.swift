import SwiftUI

struct AddCategorySheet: View {
    let category: Category?
    let onSave: (String, String) -> Void
    let onCancel: () -> Void

    @State private var displayName: String
    @State private var selectedColorHex: String
    @State private var showDuplicateError = false
    @FocusState private var isNameFieldFocused: Bool

    private let predefinedColors = ColorPickerGrid.defaultColors

    init(
        category: Category?,
        onSave: @escaping (String, String) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.category = category
        self.onSave = onSave
        self.onCancel = onCancel

        _displayName = State(initialValue: category?.displayName ?? "")
        _selectedColorHex = State(initialValue: category?.colorHex ?? ColorPickerGrid.defaultColors.first!)
    }

    var isEditMode: Bool {
        category != nil
    }

    var isValid: Bool {
        !displayName.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Pile Name")
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)

                        TextField("Enter pile name", text: $displayName)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .focused($isNameFieldFocused)
                            .onSubmit {
                                if isValid {
                                    handleSave()
                                }
                            }

                        if showDuplicateError {
                            Text("A pile with this name already exists")
                                .font(.caption)
                                .foregroundColor(.red)
                        }
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Pile Color")
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)

                        ColorPickerGrid(selectedColor: $selectedColorHex)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Preview")
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)

                        if !displayName.trimmingCharacters(in: .whitespaces).isEmpty {
                            HStack {
                                CategoryChip(
                                    displayName: displayName,
                                    colorHex: selectedColorHex,
                                    isSelected: true
                                )

                                Spacer()
                            }
                        } else {
                            HStack {
                                Text("Enter pile name to see preview")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                    .italic()

                                Spacer()
                            }
                        }
                    }

                    if category?.isDefault == true {
                        HStack {
                            Image(systemName: "info.circle")
                                .foregroundColor(.orange)

                            Text("This is a default pile. Only the color can be changed.")
                                .font(.caption)
                                .foregroundColor(.secondary)

                            Spacer()
                        }
                        .padding()
                        .background(Color.orange.opacity(0.1))
                        .cornerRadius(8)
                    }

                    Spacer(minLength: 40)
                }
                .padding()
            }
            .navigationTitle(isEditMode ? "Edit Pile" : "Add Pile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        onCancel()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(isEditMode ? "Save" : "Add") {
                        handleSave()
                    }
                    .fontWeight(.semibold)
                    .disabled(!isValid)
                }
            }
        }
        .onAppear {
            isNameFieldFocused = true
        }
    }

    private func handleSave() {
        let trimmedName = displayName.trimmingCharacters(in: .whitespaces)
        guard !trimmedName.isEmpty else { return }

        onSave(trimmedName, selectedColorHex)
    }
}

#Preview("Add Mode") {
    AddCategorySheet(
        category: nil,
        onSave: { name, color in
            print("Adding category: \(name) with color \(color)")
        },
        onCancel: {
            print("Cancelled")
        }
    )
}

#Preview("Edit Mode") {
    AddCategorySheet(
        category: Category(
            id: 1,
            name: "family",
            displayName: "Family",
            position: 0,
            colorHex: "#E91E63",
            isDefault: true
        ),
        onSave: { name, color in
            print("Updating category: \(name) with color \(color)")
        },
        onCancel: {
            print("Cancelled")
        }
    )
}