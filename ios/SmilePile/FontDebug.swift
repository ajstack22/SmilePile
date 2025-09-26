import SwiftUI

struct FontDebugView: View {
    @State private var fontStatus = "Loading..."

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Font Registration Status:")
                .font(.headline)
            Text(fontStatus)
                .font(.caption)
                .foregroundColor(.gray)

            Divider()

            Text("Available Nunito Fonts:")
                .font(.headline)

            ScrollView {
                VStack(alignment: .leading, spacing: 5) {
                    ForEach(UIFont.familyNames.sorted(), id: \.self) { family in
                        if family.lowercased().contains("nunito") {
                            VStack(alignment: .leading) {
                                Text("✅ \(family)")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(.green)
                                ForEach(UIFont.fontNames(forFamilyName: family), id: \.self) { font in
                                    Text("  → \(font)")
                                        .font(.caption2)
                                }
                            }
                            .padding(.bottom, 5)
                        }
                    }
                }
            }

            Divider()

            Text("SmilePile Logo Test:")
                .font(.headline)

            VStack(spacing: 15) {
                // Test with direct font names
                Text("SmilePile")
                    .font(.custom("Nunito-ExtraBold", size: 32))
                    .foregroundColor(Color(red: 1.0, green: 191/255, blue: 0))

                Text("SmilePile")
                    .font(.custom("Nunito-Bold", size: 32))
                    .foregroundColor(Color(red: 1.0, green: 191/255, blue: 0))

                Text("SmilePile")
                    .font(.custom("Nunito-Black", size: 32))
                    .foregroundColor(Color(red: 1.0, green: 191/255, blue: 0))

                // Test with Font extension
                Text("SmilePile")
                    .font(.nunito(.black, size: 32))
                    .foregroundColor(Color(red: 1.0, green: 191/255, blue: 0))

                // Compare with system font
                Text("SmilePile (System)")
                    .font(.system(size: 32, weight: .black, design: .default))
                    .foregroundColor(.gray)
            }

            Spacer()
        }
        .padding()
        .onAppear {
            var status = ""

            // Check for Nunito fonts
            var foundNunito = false
            for family in UIFont.familyNames {
                if family.lowercased().contains("nunito") {
                    foundNunito = true
                    status += "Found Nunito family: \(family)\n"
                    for font in UIFont.fontNames(forFamilyName: family) {
                        status += "  • \(font)\n"
                    }
                }
            }

            if !foundNunito {
                status = "❌ No Nunito fonts found! Attempting to register..."
                // Force re-registration
                _ = FontManager.shared
            } else {
                status = "✅ Nunito fonts are registered and available"
            }

            fontStatus = status
        }
    }
}