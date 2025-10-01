import SwiftUI

struct WelcomeScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator

    var body: some View {
        VStack(spacing: 20) {
            Spacer()

            // Logo and title section
            VStack(spacing: 12) {
                // SmilePile logo icon only (sharp rendering)
                Image("SmilePileLogo")
                    .resizable()
                    .renderingMode(.original)
                    .interpolation(.high)
                    .antialiased(true)
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 100, height: 100)

                // Multicolor app name (separate component with shadow)
                MulticolorSmilePileLogo(fontSize: 36, showShadow: true)

                // Tagline
                Text("A safe and fun photo gallery for EVERYONE")
                    .font(.nunito(18, weight: .regular))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }

            Spacer()

            // Features list
            VStack(alignment: .leading, spacing: 24) {
                FeatureRow(
                    icon: "square.stack",
                    iconColor: .smilePileYellow,
                    title: "Organize photos into piles",
                    description: "Create colorful piles for your photos"
                )

                FeatureRow(
                    icon: "arrow.up.left.and.arrow.down.right",
                    iconColor: .smilePileOrange,
                    title: "Distraction-free mode",
                    description: "Good for kids (and everyone else)"
                )

                FeatureRow(
                    icon: "lock.fill",
                    iconColor: .smilePileGreen,
                    title: "Optional PIN protection",
                    description: "Prevent inadvertent changes"
                )
            }
            .padding(.horizontal, 40)

            Spacer()

            // Get Started button
            Button(action: {
                coordinator.navigateToNext()
            }) {
                Text("Get Started")
                    .font(.nunito(18, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(Color.smilePileBlue)
                    .cornerRadius(12)
            }
            .padding(.horizontal, 40)
            .padding(.bottom, 50)
        }
    }
}

struct FeatureRow: View {
    let icon: String
    let iconColor: Color
    let title: String
    let description: String

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(iconColor)
                .frame(width: 30)

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.nunito(16, weight: .semibold))
                    .foregroundColor(.primary)

                Text(description)
                    .font(.nunito(14, weight: .regular))
                    .foregroundColor(.secondary)
            }
        }
    }
}