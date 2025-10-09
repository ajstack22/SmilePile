import SwiftUI

struct ImportProgressDialog: View {
    @ObservedObject var viewModel: BackupViewModel
    @Environment(\.typography) var typography

    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .scaleEffect(1.5)

            Text("Importing Data")
                .font(typography.headlineSmall)

            Text(viewModel.importMessage)
                .font(typography.labelMedium)
                .foregroundColor(.secondary)

            if viewModel.importProgress > 0 {
                Text("Progress: \(Int(viewModel.importProgress * 100))%")
                    .font(typography.labelMedium)
                    .foregroundColor(.secondary)
            }
        }
        .padding(30)
        .background(Color(UIColor.systemBackground))
        .cornerRadius(16)
        .interactiveDismissDisabled()
    }
}
