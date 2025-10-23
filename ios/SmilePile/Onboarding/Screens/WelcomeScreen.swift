import SwiftUI

struct WelcomeScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator
    @Environment(\.typography) var typography: Typography
    @Environment(\.horizontalSizeClass) var sizeClass
    @State private var showPrivacyPolicy = false

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
                    .font(typography.bodyLarge)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }

            Spacer()

            // Features list
            Group {
                if sizeClass == .regular {
                    HStack {
                        Spacer()
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
                        .frame(maxWidth: 600)
                        Spacer()
                    }
                    .padding(.horizontal, 40)
                } else {
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
                }
            }

            Spacer()

            // Buttons
            VStack(spacing: 12) {
                // Get Started button (primary action)
                Button(action: {
                    coordinator.navigateToNext()
                }) {
                    Text("Start Fresh")
                        .font(typography.bodyLarge)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: 400)
                        .frame(height: 56)
                        .frame(maxWidth: .infinity)
                        .background(Color.smilePileBlue)
                        .cornerRadius(12)
                }

                // Try Demo button (secondary action)
                Button(action: {
                    coordinator.enterDemoMode()
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 14))
                        Text("Try Demo")
                            .font(typography.bodyMedium)
                            .fontWeight(.medium)
                    }
                    .foregroundColor(.smilePileBlue)
                    .frame(maxWidth: 400)
                    .frame(height: 48)
                    .frame(maxWidth: .infinity)
                }
            }
            .padding(.horizontal, 40)

            // Privacy Policy Link
            Button(action: {
                showPrivacyPolicy = true
            }) {
                Text("Privacy Policy")
                    .font(typography.bodySmall)
                    .foregroundColor(.secondary)
                    .underline()
            }
            .padding(.top, 8)
            .padding(.bottom, 50)
        }
        .sheet(isPresented: $showPrivacyPolicy) {
            PrivacyPolicyView()
        }
    }
}

struct FeatureRow: View {
    let icon: String
    let iconColor: Color
    let title: String
    let description: String
    @Environment(\.typography) var typography: Typography

    var body: some View {
        HStack(alignment: .top, spacing: 16) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(iconColor)
                .frame(width: 30)

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(typography.bodyMedium)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)

                Text(description)
                    .font(typography.bodySmall)
                    .foregroundColor(.secondary)
            }
        }
    }
}