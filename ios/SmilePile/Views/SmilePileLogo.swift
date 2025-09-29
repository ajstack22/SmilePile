import SwiftUI

// SmilePile brand colors - EXACT match from Android
private let smileGolden = Color(hex: "#FFBF00") ?? Color.orange  // Golden/Orange for "Smile"
private let pileGreen = Color(hex: "#4CAF50") ?? Color.green    // Green for "P"
private let pileBlue = Color(hex: "#2196F3") ?? Color.blue     // Blue for "i"
private let pileBoldOrange = Color(hex: "#FF6600") ?? Color.orange // Bold orange for "l"
private let pilePink = Color(hex: "#E86082") ?? Color.pink     // Updated pink for "e"

/// SmilePile logo component with icon and multicolored text
/// Displays the 5-smiley icon alongside "SmilePile" text where each letter has different colors
struct SmilePileLogo: View {
    let iconSize: CGFloat
    let fontSize: CGFloat
    let showIcon: Bool

    init(
        iconSize: CGFloat = 32,
        fontSize: CGFloat = 24,
        showIcon: Bool = true
    ) {
        self.iconSize = iconSize
        self.fontSize = fontSize
        self.showIcon = showIcon
    }

    var body: some View {
        HStack(spacing: 8) {
            // Logo icon
            if showIcon {
                Image("SmilePileLogo")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: iconSize, height: iconSize)
            }

            // Multicolored text with shadow for outline effect
            HStack(spacing: 0) {
                // "Smile" in golden
                Text("Smile")
                    .font(.custom("Nunito-ExtraBold", size: fontSize))
                    .foregroundColor(smileGolden)
                    // Subtle crisp shadow effect
                    .shadow(color: Color.black.opacity(0.5), radius: 1, x: 1, y: 1)
                    .shadow(color: Color.black.opacity(0.3), radius: 0, x: 1, y: 1)

                // "P" in green
                Text("P")
                    .font(.custom("Nunito-ExtraBold", size: fontSize))
                    .foregroundColor(pileGreen)
                    // Subtle crisp shadow effect
                    .shadow(color: Color.black.opacity(0.5), radius: 1, x: 1, y: 1)
                    .shadow(color: Color.black.opacity(0.3), radius: 0, x: 1, y: 1)

                // "i" in blue
                Text("i")
                    .font(.custom("Nunito-ExtraBold", size: fontSize))
                    .foregroundColor(pileBlue)
                    // Subtle crisp shadow effect
                    .shadow(color: Color.black.opacity(0.5), radius: 1, x: 1, y: 1)
                    .shadow(color: Color.black.opacity(0.3), radius: 0, x: 1, y: 1)

                // "l" in orange
                Text("l")
                    .font(.custom("Nunito-ExtraBold", size: fontSize))
                    .foregroundColor(pileBoldOrange)
                    // Subtle crisp shadow effect
                    .shadow(color: Color.black.opacity(0.5), radius: 1, x: 1, y: 1)
                    .shadow(color: Color.black.opacity(0.3), radius: 0, x: 1, y: 1)

                // "e" in pink
                Text("e")
                    .font(.custom("Nunito-ExtraBold", size: fontSize))
                    .foregroundColor(pilePink)
                    // Subtle crisp shadow effect
                    .shadow(color: Color.black.opacity(0.5), radius: 1, x: 1, y: 1)
                    .shadow(color: Color.black.opacity(0.3), radius: 0, x: 1, y: 1)
            }
        }
    }
}

/// Compact version of SmilePile logo for smaller spaces
struct SmilePileLogoCompact: View {
    var body: some View {
        SmilePileLogo(
            iconSize: 24,
            fontSize: 18
        )
    }
}

#Preview {
    VStack(spacing: 20) {
        SmilePileLogo()
        SmilePileLogoCompact()
        AppHeaderComponent(
            onViewModeClick: {},
            showViewModeButton: true
        )
    }
    .padding()
}