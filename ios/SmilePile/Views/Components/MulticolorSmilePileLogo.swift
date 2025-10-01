import SwiftUI

struct MulticolorSmilePileLogo: View {
    let fontSize: CGFloat
    let showShadow: Bool

    init(fontSize: CGFloat = 36, showShadow: Bool = true) {
        self.fontSize = fontSize
        self.showShadow = showShadow
    }

    var body: some View {
        HStack(spacing: 0) {
            Text("Smile")
                .foregroundColor(.smilePileYellow)
                .font(.custom("Nunito-ExtraBold", size: fontSize))

            Text("P")
                .foregroundColor(.smilePileGreen)
                .font(.custom("Nunito-ExtraBold", size: fontSize))

            Text("i")
                .foregroundColor(.smilePileBlue)
                .font(.custom("Nunito-ExtraBold", size: fontSize))

            Text("l")
                .foregroundColor(.smilePileOrange)
                .font(.custom("Nunito-ExtraBold", size: fontSize))

            Text("e")
                .foregroundColor(.smilePilePink)
                .font(.custom("Nunito-ExtraBold", size: fontSize))
        }
        .if(showShadow) { view in
            view.shadow(
                color: .black.opacity(0.9),
                radius: 6,
                x: 4,
                y: 4
            )
        }
    }
}

// Helper modifier
extension View {
    @ViewBuilder
    func `if`<Transform: View>(_ condition: Bool, transform: (Self) -> Transform) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
}

#Preview {
    VStack(spacing: 40) {
        // Default size with shadow
        MulticolorSmilePileLogo()

        // Larger size with shadow
        MulticolorSmilePileLogo(fontSize: 48)

        // Small size without shadow
        MulticolorSmilePileLogo(fontSize: 24, showShadow: false)
    }
    .padding()
    .background(Color.gray.opacity(0.1))
}