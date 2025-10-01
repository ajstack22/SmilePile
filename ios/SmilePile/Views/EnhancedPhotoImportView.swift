// Temporary stub - EnhancedPhotoImportView has broken dependencies
// TODO: Fix PhotoImportManager integration

import SwiftUI

struct EnhancedPhotoImportView: View {
    @Binding var isPresented: Bool
    let categoryId: Int64
    let onImportComplete: (ImportResult) -> Void
    let onCancel: (() -> Void)?

    var body: some View {
        Text("Enhanced import temporarily unavailable")
            .onAppear {
                isPresented = false
                onCancel?()
            }
    }
}
