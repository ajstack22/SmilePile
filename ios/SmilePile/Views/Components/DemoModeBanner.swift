import SwiftUI

struct DemoModeBanner: View {
    @Environment(\.typography) var typography
    @ObservedObject var settingsManager: SettingsManager
    @State private var showExitConfirmation = false
    @State private var isExiting = false
    @State private var showError = false
    @State private var errorMessage = ""

    var body: some View {
        if settingsManager.isDemoMode {
            VStack(spacing: 0) {
                HStack(spacing: 12) {
                    Image(systemName: "star.fill")
                        .font(typography.bodyMedium)
                        .foregroundColor(.white)

                    Text("Demo Mode - Viewing Jamie's Photos")
                        .font(typography.bodyLarge)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)

                    Spacer()

                    Button(action: {
                        showExitConfirmation = true
                    }) {
                        Text("Exit")
                            .font(typography.bodyMedium)
                            .fontWeight(.bold)
                            .foregroundColor(Color(red: 156/255, green: 39/255, blue: 176/255))
                            .padding(.horizontal, 16)
                            .padding(.vertical, 6)
                            .background(Color.white)
                            .cornerRadius(6)
                    }
                    .disabled(isExiting)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(Color(red: 156/255, green: 39/255, blue: 176/255))
            }
            .alert("Exit Demo Mode?", isPresented: $showExitConfirmation) {
                Button("Cancel", role: .cancel) {}
                Button("Exit Demo", role: .destructive) {
                    exitDemoMode()
                }
            } message: {
                Text("This will remove all demo photos and categories. You can try demo mode again later.")
            }
            .alert("Error", isPresented: $showError) {
                Button("OK", role: .cancel) {
                    showError = false
                }
            } message: {
                Text(errorMessage)
            }
        }
    }

    private func exitDemoMode() {
        Task { @MainActor in
            do {
                isExiting = true

                // Delete all photos where isFromAssets = true
                let photoRepo = PhotoRepositoryImpl()
                let allPhotos = try await photoRepo.getAllPhotos()
                let demoPhotos = allPhotos.filter { $0.isFromAssets }

                for photo in demoPhotos {
                    // Delete photo file
                    let fileURL = URL(fileURLWithPath: photo.path)
                    try? FileManager.default.removeItem(at: fileURL)

                    // Delete from database
                    try await photoRepo.deletePhoto(photo)
                }

                // Delete demo categories
                let categoryRepo = CategoryRepositoryImpl.shared
                try await categoryRepo.deleteDemoCategories()

                // Set demo mode to false
                settingsManager.isDemoMode = false

                isExiting = false

            } catch {
                isExiting = false
                errorMessage = "Failed to exit demo mode: \(error.localizedDescription)"
                showError = true
            }
        }
    }
}
