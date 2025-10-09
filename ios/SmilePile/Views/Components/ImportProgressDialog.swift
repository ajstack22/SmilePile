import SwiftUI

struct ImportProgressDialog: View {
    @ObservedObject var viewModel: BackupViewModel

    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)

            Text("Importing Data")
                .font(.headline)

            Text(viewModel.importMessage)
                .font(.caption)
                .foregroundColor(.secondary)

            if viewModel.importProgress > 0 {
                Text("Progress: \(Int(viewModel.importProgress * 100))%")
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
