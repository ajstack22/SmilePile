import SwiftUI

struct ExportProgressDialog: View {
    @ObservedObject var viewModel: BackupViewModel

    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)

            Text("Exporting Data")
                .font(.headline)

            Text("Creating backup with photos. This may take a moment...")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)

            Text(viewModel.exportMessage)
                .font(.caption)
                .foregroundColor(.secondary)

            if viewModel.exportProgress > 0 {
                Text("Progress: \(Int(viewModel.exportProgress * 100))%")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(30)
        .background(Color(UIColor.systemBackground))
        .cornerRadius(16)
        .interactiveDismissDisabled()
    }
}
