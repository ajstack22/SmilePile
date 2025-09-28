import SwiftUI

// Preview for Security Section
struct SecuritySectionPreview: View {
    @StateObject private var securityViewModel = SecuritySettingsViewModel()
    @State private var biometricEnabled = true

    var body: some View {
        NavigationView {
            Form {
                // Security Section - Green accent color matching Android
                SettingsSection(
                    title: "Security",
                    titleColor: Color(red: 76/255, green: 175/255, blue: 80/255) // #4CAF50
                ) {
                    VStack(spacing: 0) {
                        // PIN Setup/Change button - Always visible
                        SettingsActionItem(
                            title: "Set PIN",
                            subtitle: "Set a PIN to protect Parent Mode access",
                            icon: "lock.fill",
                            iconColor: Color(red: 76/255, green: 175/255, blue: 80/255),
                            action: {
                                print("Set PIN tapped")
                            }
                        )

                        // Example with PIN already set
                        SettingsActionItem(
                            title: "Change PIN",
                            subtitle: "PIN protection is enabled for Parent Mode",
                            icon: "lock.fill",
                            iconColor: Color(red: 76/255, green: 175/255, blue: 80/255),
                            action: {
                                print("Change PIN tapped")
                            }
                        )

                        // Biometric Authentication Toggle
                        SettingsSwitchItem(
                            title: "Biometric Authentication",
                            subtitle: "Use Face ID for parental controls",
                            icon: "faceid",
                            iconColor: Color(red: 76/255, green: 175/255, blue: 80/255),
                            isOn: $biometricEnabled,
                            isEnabled: true
                        )

                        // Parental Controls Access
                        SettingsActionItem(
                            title: "Parental Controls",
                            subtitle: "Access child safety settings and preferences",
                            icon: "figure.and.child.holdinghands",
                            iconColor: Color(red: 76/255, green: 175/255, blue: 80/255),
                            action: {
                                print("Parental Controls tapped")
                            }
                        )

                        // Remove PIN option
                        SettingsActionItem(
                            title: "Remove PIN",
                            subtitle: "Remove PIN protection from Parent Mode",
                            icon: "lock.open.fill",
                            iconColor: Color(red: 76/255, green: 175/255, blue: 80/255),
                            action: {
                                print("Remove PIN tapped")
                            }
                        )
                    }
                }
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)
            }
            .navigationTitle("Security Settings Preview")
        }
    }
}

#Preview {
    SecuritySectionPreview()
}