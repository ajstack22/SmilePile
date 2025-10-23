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
                .font(.custom("Nunito", size: fontSize).weight(.heavy))
                .if(showShadow) { view in
                    view.shadow(color: Color.black.opacity(0.5), radius: 1, x: 1, y: 1)
                        .shadow(color: Color.black.opacity(0.3), radius: 0, x: 1, y: 1)
                }

            Text("P")
                .foregroundColor(.smilePileGreen)
                .font(.custom("Nunito", size: fontSize).weight(.heavy))
                .if(showShadow) { view in
                    view.shadow(color: Color.black.opacity(0.5), radius: 1, x: 1, y: 1)
                        .shadow(color: Color.black.opacity(0.3), radius: 0, x: 1, y: 1)
                }

            Text("i")
                .foregroundColor(.smilePileBlue)
                .font(.custom("Nunito", size: fontSize).weight(.heavy))
                .if(showShadow) { view in
                    view.shadow(color: Color.black.opacity(0.5), radius: 1, x: 1, y: 1)
                        .shadow(color: Color.black.opacity(0.3), radius: 0, x: 1, y: 1)
                }

            Text("l")
                .foregroundColor(.smilePileOrange)
                .font(.custom("Nunito", size: fontSize).weight(.heavy))
                .if(showShadow) { view in
                    view.shadow(color: Color.black.opacity(0.5), radius: 1, x: 1, y: 1)
                        .shadow(color: Color.black.opacity(0.3), radius: 0, x: 1, y: 1)
                }

            Text("e")
                .foregroundColor(.smilePilePink)
                .font(.custom("Nunito", size: fontSize).weight(.heavy))
                .if(showShadow) { view in
                    view.shadow(color: Color.black.opacity(0.5), radius: 1, x: 1, y: 1)
                        .shadow(color: Color.black.opacity(0.3), radius: 0, x: 1, y: 1)
                }
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