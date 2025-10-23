//
//  PrivacyPolicyView.swift
//  SmilePile
//
//  Embedded privacy policy view for offline access
//

import SwiftUI

struct PrivacyPolicyView: View {
    @Environment(\.dismiss) var dismiss
    @Environment(\.typography) var typography

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    privacyPolicyContent
                }
                .padding()
            }
            .navigationTitle("Privacy Policy")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
        }
    }

    private var privacyPolicyContent: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Last Updated
            Text("Last Updated: October 2, 2025")
                .font(typography.bodySmall)
                .foregroundColor(.secondary)

            // Online version note
            VStack(alignment: .leading, spacing: 8) {
                Text("📱 Viewing Offline Version")
                    .font(typography.bodyMedium)
                    .fontWeight(.semibold)
                Text("For the most current version, visit smilepile.app/privacy")
                    .font(typography.bodySmall)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color.secondary.opacity(0.1))
            .cornerRadius(8)

            Divider()

            // Introduction
            SectionHeader(title: "Introduction")
            BodyText("SmilePile is a family photo organizer developed by StackMap. We are committed to protecting your privacy and the privacy of your children. This Privacy Policy explains what information we collect, how we use it, and your rights regarding your data.")
            BodyTextBold("SmilePile is designed with privacy as our top priority.") +
            BodyText(" All photos and data are stored locally on your device, and we do not sync to any cloud service or collect personal information.")

            // Information We Collect
            SectionHeader(title: "Information We Collect")

            SubsectionHeader(title: "Photos You Choose to Import")
            BulletList(items: [
                "When you select photos to organize in SmilePile, they are copied to the app's local storage on your device",
                "These photos never leave your device",
                "We do not upload photos to any cloud service or remote server",
                "You retain complete control and ownership of all your photos"
            ])

            SubsectionHeader(title: "Device Information (For Crash Reports Only)")
            BulletList(items: [
                "Device model (e.g., \"iPhone 14\" or \"Samsung Galaxy S23\")",
                "Operating system version (e.g., \"iOS 18.0\" or \"Android 14\")",
                "App version number",
                "This information helps us fix bugs and improve the app",
                "No personal information is included"
            ])

            SubsectionHeader(title: "What We DON'T Collect")
            BulletList(items: [
                "We do NOT collect your name, email, phone number, or any personal identifiers",
                "We do NOT track your location",
                "We do NOT monitor how you use the app",
                "We do NOT use analytics or tracking software",
                "We do NOT share any information with third parties",
                "We do NOT create user accounts or require login"
            ])

            // How We Use Information
            SectionHeader(title: "How We Use Information")
            BodyTextBold("Local Storage Only:")
            BulletList(items: [
                "All data is stored locally on your device using iOS Photos app integration (iOS) or local storage (Android)",
                "No cloud sync or remote server storage",
                "No sharing with third parties",
                "No advertising or marketing use"
            ])

            // Data Storage & Security
            SectionHeader(title: "Data Storage & Security")
            BodyTextBold("Where Your Data Lives:")
            BulletList(items: [
                "iOS: Photos are accessed through your device's Photos app and stored in app-specific local storage",
                "Android: Photos are stored in local device storage",
                "No Remote Servers: SmilePile does not maintain any remote servers or databases",
                "Device Security: Your data is protected by your device's security features (passcode, biometrics)"
            ])

            // Third-Party Services
            SectionHeader(title: "Third-Party Services")
            BodyTextBold("SmilePile does not use any third-party services, analytics, or tracking.") +
            BodyText(" We do not integrate with:")
            BulletList(items: [
                "Analytics services (no Google Analytics, Firebase, etc.)",
                "Advertising networks",
                "Social media platforms",
                "Cloud storage providers",
                "Data brokers or tracking companies"
            ])

            // Children's Privacy
            SectionHeader(title: "Children's Privacy (COPPA Compliance)")

            SubsectionHeader(title: "Age Restrictions")
            BodyText("SmilePile is designed for users ") +
            BodyTextBold("ages 13 and older") +
            BodyText(", or for younger users with parental supervision. We comply with the Children's Online Privacy Protection Act (COPPA).")

            SubsectionHeader(title: "Data Collection from Children")
            BodyTextBold("We do not knowingly collect personal information from children under 13.") +
            BodyText(" Because SmilePile operates entirely on-device with no data transmission:")
            BulletList(items: [
                "No personal information is collected from any users, including children",
                "No user accounts are created",
                "No data is sent to our servers",
                "Parents have complete control over all app data on the device"
            ])

            SubsectionHeader(title: "Enhanced Parental Rights (2025 FTC Amendments)")
            BodyText("Under COPPA and recent FTC amendments, parents have the following rights:")
            BulletList(items: [
                "Right to Access: All data is stored locally on your device - simply open the app to access it",
                "Right to Delete: Parents can delete all data by going to Settings → Clear All Data, or by uninstalling the app",
                "Right to Opt-Out: No data collection occurs, so no opt-out is necessary",
                "Right to Information: This privacy policy provides complete transparency about our practices"
            ])

            SubsectionHeader(title: "Parental Consent")
            BodyText("Because SmilePile does not collect any personal information or transmit data off-device, parental consent for data collection is not required. However, we recommend parents:")
            BulletList(items: [
                "Review this privacy policy",
                "Understand that all photos remain on the device",
                "Supervise children's use of the app",
                "Use device-level parental controls as appropriate"
            ])

            // Your Rights
            SectionHeader(title: "Your Rights")

            SubsectionHeader(title: "Right to Access Data")
            BodyText("All your data is stored on your device. You can access it anytime by opening the SmilePile app.")

            SubsectionHeader(title: "Right to Delete Data")
            BodyText("You can delete all SmilePile data by:")
            BulletList(items: [
                "Going to Settings → Clear All Data in the app (permanently deletes all app data)",
                "Uninstalling the app from your device (removes app and all associated data)"
            ])
            BodyTextBold("Note: ") +
            BodyText("Deleting data from SmilePile does not delete photos from your device's photo library - those remain untouched.")

            SubsectionHeader(title: "Right to Export")
            BodyText("Your photos remain in your device's photo library and can be exported using your device's native export features.")

            // Data Retention
            SectionHeader(title: "Data Retention")
            BodyTextBold("SmilePile retains no data on remote servers.") +
            BodyText(" All data exists only on your device for as long as:")
            BulletList(items: [
                "The app is installed on your device, OR",
                "Until you manually delete data via Settings → Clear All Data"
            ])

            // International Users
            SectionHeader(title: "International Users")
            BodyText("SmilePile operates entirely on-device regardless of your location. No data crosses international borders because no data leaves your device.")
            BodyText("For users in the European Union, SmilePile's local-only approach means we are not a data controller under GDPR, as no personal data is processed by us.")

            // Changes to This Policy
            SectionHeader(title: "Changes to This Policy")
            BodyText("We may update this Privacy Policy from time to time. When we make changes:")
            BulletList(items: [
                "We will update the \"Last Updated\" date at the top of this policy",
                "Significant changes will be announced via app update notes",
                "Continued use of the app after changes constitutes acceptance of the updated policy"
            ])
            BodyText("We encourage you to review this policy periodically.")

            // Contact Us
            SectionHeader(title: "Contact Us")
            BodyText("If you have questions about this Privacy Policy or SmilePile's privacy practices, please contact us:")
            VStack(alignment: .leading, spacing: 4) {
                BodyTextBold("Email: ") + BodyText("support@stackmap.app")
                BodyTextBold("Company: ") + BodyText("StackMap")
                BodyTextBold("Response Time: ") + BodyText("We typically respond within 1-2 business days")
            }

            // Summary
            SectionHeader(title: "Summary")
            BodyTextBold("In Plain Language:")
            BulletList(items: [
                "✅ Your photos stay on your device",
                "✅ No cloud storage or sync",
                "✅ No tracking or analytics",
                "✅ No personal information collected",
                "✅ No third-party services",
                "✅ COPPA compliant for children's privacy",
                "✅ You have complete control over your data"
            ])
            BodyText("SmilePile is designed to give you peace of mind about your family's digital privacy.")
        }
    }
}

// MARK: - Text Components

private extension PrivacyPolicyView {
    func SectionHeader(title: String) -> some View {
        Text(title)
            .font(typography.headlineMedium)
            .fontWeight(.bold)
            .foregroundColor(.primary)
            .padding(.top, 8)
    }

    func SubsectionHeader(title: String) -> some View {
        Text(title)
            .font(typography.titleMedium)
            .fontWeight(.semibold)
            .foregroundColor(.primary)
            .padding(.top, 4)
    }

    func BodyText(_ text: String) -> Text {
        Text(text)
            .font(typography.bodyMedium)
            .foregroundColor(.primary)
    }

    func BodyTextBold(_ text: String) -> Text {
        Text(text)
            .font(typography.bodyMedium)
            .fontWeight(.bold)
            .foregroundColor(.primary)
    }

    func BulletList(items: [String]) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            ForEach(items, id: \.self) { item in
                HStack(alignment: .top, spacing: 8) {
                    Text("•")
                        .font(typography.bodyMedium)
                        .foregroundColor(.secondary)
                    Text(item)
                        .font(typography.bodyMedium)
                        .foregroundColor(.primary)
                }
            }
        }
    }
}

#Preview {
    PrivacyPolicyView()
}
