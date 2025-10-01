#!/bin/bash
# Fix pre-existing broken files so we can test onboarding

cd "$(dirname "$0")"

echo "Backing up broken files and creating stubs..."

# Backup broken files
for file in \
  "SmilePile/Data/Backup/ZipUtils.swift" \
  "SmilePile/Views/EnhancedPhotoImportView.swift"
do
  if [ -f "$file" ]; then
    mv "$file" "$file.broken"
    echo "Backed up: $file"
  fi
done

# Create ZipUtils stub
cat > SmilePile/Data/Backup/ZipUtils.swift << 'EOF'
// Temporary stub - ZipUtils has compilation errors
// TODO: Fix Archive framework generic parameter inference

import Foundation

class ZipUtils {
    static func createZip(from: URL, to: URL, progress: ((Double) -> Void)?) async throws {
        throw NSError(domain: "ZipUtils", code: -1, userInfo: [NSLocalizedDescriptionKey: "Not implemented"])
    }

    static func extractZip(from: URL, to: URL, progress: ((Double) -> Void)?) async throws {
        throw NSError(domain: "ZipUtils", code: -1, userInfo: [NSLocalizedDescriptionKey: "Not implemented"])
    }
}
EOF

# Create EnhancedPhotoImportView stub
cat > SmilePile/Views/EnhancedPhotoImportView.swift << 'EOF'
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
EOF

echo "Done! Now try building:"
echo "  xcodebuild -project SmilePile.xcodeproj -scheme SmilePile -sdk iphonesimulator build"
