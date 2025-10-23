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
            Button(action: {
                showExitConfirmation = true
            }) {
                Text("Exit Demo")
                    .font(typography.bodyMedium)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color(red: 156/255, green: 39/255, blue: 176/255))
                    .cornerRadius(8)
                    .shadow(color: Color.black.opacity(0.2), radius: 4, x: 0, y: 2)
            }
            .disabled(isExiting)
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
