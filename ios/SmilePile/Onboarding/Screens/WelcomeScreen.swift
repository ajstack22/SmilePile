import SwiftUI

struct WelcomeScreen: View {
    @ObservedObject var coordinator: OnboardingCoordinator

    var body: some View {
        VStack(spacing: 40) {
            Spacer()

            // Logo
            VStack(spacing: 20) {
                Image(systemName: "photo.stack.fill")
                    .font(.system(size: 80))
                    .foregroundColor(Color(red: 1.0, green: 0.42, blue: 0.42))

                Text("SmilePile")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                Text("A safe and fun photo gallery for kids")
                    .font(.title3)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }

            Spacer()

            // Features list
            VStack(alignment: .leading, spacing: 20) {
                FeatureRow(
                    icon: "folder.fill",
                    title: "Organize photos",
                    description: "Create colorful categories"
                )

                FeatureRow(
                    icon: "photo.fill",
                    title: "Import memories",
                    description: "Add your favorite photos"
                )

                FeatureRow(
                    icon: "lock.fill",
                    title: "Stay secure",
                    description: "Optional PIN protection"
                )
            }
            .padding(.horizontal, 40)

            Spacer()

            // Get Started button
            Button(action: {
                coordinator.navigateToNext()
            }) {
                Text("Get Started")
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color(red: 1.0, green: 0.42, blue: 0.42))
                    .cornerRadius(12)
            }
            .padding(.horizontal, 40)
            .padding(.bottom, 50)
        }
    }
}

struct FeatureRow: View {
    let icon: String
    let title: String
    let description: String

    var body: some View {
        HStack(spacing: 15) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(Color(red: 1.0, green: 0.42, blue: 0.42))
                .frame(width: 30)

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)

                Text(description)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}