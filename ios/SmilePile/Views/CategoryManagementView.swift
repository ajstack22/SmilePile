import SwiftUI

struct CategoryManagementView: View {
    @StateObject private var viewModel = CategoryManagementViewModel()
    @State private var pulseAnimation = false

    var body: some View {
        NavigationView {
            ZStack {
                if viewModel.categoriesWithCounts.isEmpty && !viewModel.isLoading {
                    emptyState
                } else {
                    categoryList
                }

                if viewModel.isLoading {
                    loadingOverlay
                }
            }
            .navigationTitle("Categories")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    addButton
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
            if viewModel.hasPulseFAB {
                Image(systemName: "plus.circle.fill")
                    .font(.title2)
                    .foregroundColor(.accentColor)
                    .scaleEffect(pulseAnimation ? 1.2 : 1.0)
                    .animation(
                        Animation.easeInOut(duration: 1.0)
                            .repeatForever(autoreverses: true),
                        value: pulseAnimation
                    )
                    .onAppear {
                        pulseAnimation = true
                    }
            } else {
                Image(systemName: "plus")
                    .font(.title3)
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