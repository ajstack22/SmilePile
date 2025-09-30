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
                .font(.nunito(fontSize, weight: .heavy))

            Text("P")
                .foregroundColor(.smilePileGreen)
                .font(.nunito(fontSize, weight: .heavy))

            Text("i")
                .foregroundColor(.smilePileBlue)
                .font(.nunito(fontSize, weight: .heavy))

            Text("l")
                .foregroundColor(.smilePileOrange)
                .font(.nunito(fontSize, weight: .heavy))

            Text("e")
                .foregroundColor(.smilePilePink)
                .font(.nunito(fontSize, weight: .heavy))
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