import SwiftUI

struct CategoryManagementView: View {
    @StateObject private var viewModel = CategoryManagementViewModel()
    @State private var pulseAnimation = false
    @EnvironmentObject var kidsModeViewModel: KidsModeViewModel

    var body: some View {
        VStack(spacing: 0) {
            // App Header with consistent design
            AppHeaderComponent(
                onViewModeClick: {
                    kidsModeViewModel.toggleKidsMode()
                },
                showViewModeButton: true
            )

            // Main content
            ZStack {
                // Category content
                if viewModel.categoriesWithCounts.isEmpty && !viewModel.isLoading {
                    emptyState
                } else {
                    categoryList
                }

                if viewModel.isLoading {
                    loadingOverlay
                }

                // Floating add button
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        addButton
                            .padding(.trailing, 16)
                            .padding(.bottom, 16)
                    }
                }
            }
            .sheet(isPresented: $viewModel.showAddDialog) {
                AddCategorySheet(
                    category: viewModel.editingCategory,
                    onSave: { displayName, colorHex in
                        if viewModel.editingCategory != nil {
                            viewModel.updateCategory(
                                displayName: displayName,
                                colorHex: colorHex
                            )
                        } else {
                            viewModel.addCategory(
                                displayName: displayName,
                                colorHex: colorHex
                            )
                        }
                    },
                    onCancel: {
                        viewModel.showAddDialog = false
                        viewModel.editingCategory = nil
                    }
                )
            }
            .alert("Delete Category", isPresented: $viewModel.showDeleteConfirmation) {
                deleteAlert
            } message: {
                Text(viewModel.deletionMessage)
            }
            .alert("Error", isPresented: .constant(viewModel.errorMessage != nil)) {
                Button("OK") {
                    viewModel.errorMessage = nil
                }
            } message: {
                if let error = viewModel.errorMessage {
                    Text(error)
                }
            }
        }
    }

    @ViewBuilder
    private var categoryList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(viewModel.categoriesWithCounts, id: \.category.id) { categoryWithCount in
                    CategoryManagementCard(
                        categoryWithCount: categoryWithCount,
                        onEdit: { category in
                            viewModel.showEditCategoryDialog(category)
                        },
                        onDelete: { category in
                            viewModel.requestDeleteCategory(category)
                        }
                    )
                }
            }
            .padding()
        }
    }

    @ViewBuilder
    private var emptyState: some View {
        VStack(spacing: 20) {
            Image(systemName: "folder.badge.plus")
                .font(.system(size: 64))
                .foregroundColor(.gray)

            Text("No Categories")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Tap the + button to create your first category")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Button(action: {
                viewModel.showAddCategoryDialog()
            }) {
                Text("Create First Category")
                    .fontWeight(.medium)
            }
            .buttonStyle(.borderedProminent)
            .padding(.top, 8)
        }
    }

    @ViewBuilder
    private var loadingOverlay: some View {
        Color.black.opacity(0.3)
            .ignoresSafeArea()
            .overlay(
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle())
                    .scaleEffect(1.5)
            )
    }

    @ViewBuilder
    private var addButton: some View {
        Button(action: {
            viewModel.showAddCategoryDialog()
        }) {
            ZStack {
                // Square FAB with rounded corners to match gallery
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(red: 255/255, green: 102/255, blue: 0/255)) // SmilePile orange #FF6600
                    .frame(width: 56, height: 56)
                    .shadow(color: Color.black.opacity(0.3), radius: 4, x: 0, y: 4)

                Image(systemName: "plus")
                    .font(.system(size: 24, weight: .medium))
                    .foregroundColor(.white)
            }
        }
        .scaleEffect(viewModel.hasPulseFAB && pulseAnimation ? 1.1 : 1.0)
        .animation(
            viewModel.hasPulseFAB ? Animation.easeInOut(duration: 1.0).repeatForever(autoreverses: true) : .default,
            value: pulseAnimation
        )
        .onAppear {
            if viewModel.hasPulseFAB {
                pulseAnimation = true
            }
        }
    }

    @ViewBuilder
    private var deleteAlert: some View {
        Group {
            if let categoryWithCount = viewModel.categoriesWithCounts.first(where: {
                $0.category.id == viewModel.categoryToDelete?.id
            }), categoryWithCount.photoCount > 0 {
                Button("Delete Category and Photos", role: .destructive) {
                    viewModel.confirmDeleteCategory(deletePhotos: true)
                }
                Button("Cancel", role: .cancel) {
                    viewModel.cancelDelete()
                }
            } else {
                Button("Delete", role: .destructive) {
                    viewModel.confirmDeleteCategory()
                }
                Button("Cancel", role: .cancel) {
                    viewModel.cancelDelete()
                }
            }
        }
    }
}

#Preview {
    CategoryManagementView()
}