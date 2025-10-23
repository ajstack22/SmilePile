package com.smilepile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Embedded privacy policy screen for offline access
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrivacyPolicyContent()
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    // Last Updated
    Text(
        text = "Last Updated: October 2, 2025",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Online version note
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📱 Viewing Offline Version",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "For the most current version, visit smilepile.app/privacy",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    HorizontalDivider()

    // Introduction
    SectionHeader("Introduction")
    BodyText("SmilePile is a family photo organizer developed by StackMap. We are committed to protecting your privacy and the privacy of your children. This Privacy Policy explains what information we collect, how we use it, and your rights regarding your data.")
    BoldText("SmilePile is designed with privacy as our top priority.") +
    BodyText(" All photos and data are stored locally on your device, and we do not sync to any cloud service or collect personal information.")

    // Information We Collect
    SectionHeader("Information We Collect")

    SubsectionHeader("Photos You Choose to Import")
    BulletList(
        listOf(
            "When you select photos to organize in SmilePile, they are copied to the app's local storage on your device",
            "These photos never leave your device",
            "We do not upload photos to any cloud service or remote server",
            "You retain complete control and ownership of all your photos"
        )
    )

    SubsectionHeader("Device Information (For Crash Reports Only)")
    BulletList(
        listOf(
            "Device model (e.g., \"iPhone 14\" or \"Samsung Galaxy S23\")",
            "Operating system version (e.g., \"iOS 18.0\" or \"Android 14\")",
            "App version number",
            "This information helps us fix bugs and improve the app",
            "No personal information is included"
        )
    )

    SubsectionHeader("What We DON'T Collect")
    BulletList(
        listOf(
            "We do NOT collect your name, email, phone number, or any personal identifiers",
            "We do NOT track your location",
            "We do NOT monitor how you use the app",
            "We do NOT use analytics or tracking software",
            "We do NOT share any information with third parties",
            "We do NOT create user accounts or require login"
        )
    )

    // How We Use Information
    SectionHeader("How We Use Information")
    BoldText("Local Storage Only:")
    BulletList(
        listOf(
            "All data is stored locally on your device using iOS Photos app integration (iOS) or local storage (Android)",
            "No cloud sync or remote server storage",
            "No sharing with third parties",
            "No advertising or marketing use"
        )
    )

    // Data Storage & Security
    SectionHeader("Data Storage & Security")
    BoldText("Where Your Data Lives:")
    BulletList(
        listOf(
            "iOS: Photos are accessed through your device's Photos app and stored in app-specific local storage",
            "Android: Photos are stored in local device storage",
            "No Remote Servers: SmilePile does not maintain any remote servers or databases",
            "Device Security: Your data is protected by your device's security features (passcode, biometrics)"
        )
    )

    // Third-Party Services
    SectionHeader("Third-Party Services")
    BoldText("SmilePile does not use any third-party services, analytics, or tracking.") +
    BodyText(" We do not integrate with:")
    BulletList(
        listOf(
            "Analytics services (no Google Analytics, Firebase, etc.)",
            "Advertising networks",
            "Social media platforms",
            "Cloud storage providers",
            "Data brokers or tracking companies"
        )
    )

    // Children's Privacy
    SectionHeader("Children's Privacy (COPPA Compliance)")

    SubsectionHeader("Age Restrictions")
    BodyText("SmilePile is designed for users ") +
    BoldText("ages 13 and older") +
    BodyText(", or for younger users with parental supervision. We comply with the Children's Online Privacy Protection Act (COPPA).")

    SubsectionHeader("Data Collection from Children")
    BoldText("We do not knowingly collect personal information from children under 13.") +
    BodyText(" Because SmilePile operates entirely on-device with no data transmission:")
    BulletList(
        listOf(
            "No personal information is collected from any users, including children",
            "No user accounts are created",
            "No data is sent to our servers",
            "Parents have complete control over all app data on the device"
        )
    )

    SubsectionHeader("Enhanced Parental Rights (2025 FTC Amendments)")
    BodyText("Under COPPA and recent FTC amendments, parents have the following rights:")
    BulletList(
        listOf(
            "Right to Access: All data is stored locally on your device - simply open the app to access it",
            "Right to Delete: Parents can delete all data by going to Settings → Clear All Data, or by uninstalling the app",
            "Right to Opt-Out: No data collection occurs, so no opt-out is necessary",
            "Right to Information: This privacy policy provides complete transparency about our practices"
        )
    )

    SubsectionHeader("Parental Consent")
    BodyText("Because SmilePile does not collect any personal information or transmit data off-device, parental consent for data collection is not required. However, we recommend parents:")
    BulletList(
        listOf(
            "Review this privacy policy",
            "Understand that all photos remain on the device",
            "Supervise children's use of the app",
            "Use device-level parental controls as appropriate"
        )
    )

    // Your Rights
    SectionHeader("Your Rights")

    SubsectionHeader("Right to Access Data")
    BodyText("All your data is stored on your device. You can access it anytime by opening the SmilePile app.")

    SubsectionHeader("Right to Delete Data")
    BodyText("You can delete all SmilePile data by:")
    BulletList(
        listOf(
            "Going to Settings → Clear All Data in the app (permanently deletes all app data)",
            "Uninstalling the app from your device (removes app and all associated data)"
        )
    )
    BoldText("Note: ") +
    BodyText("Deleting data from SmilePile does not delete photos from your device's photo library - those remain untouched.")

    SubsectionHeader("Right to Export")
    BodyText("Your photos remain in your device's photo library and can be exported using your device's native export features.")

    // Data Retention
    SectionHeader("Data Retention")
    BoldText("SmilePile retains no data on remote servers.") +
    BodyText(" All data exists only on your device for as long as:")
    BulletList(
        listOf(
            "The app is installed on your device, OR",
            "Until you manually delete data via Settings → Clear All Data"
        )
    )

    // International Users
    SectionHeader("International Users")
    BodyText("SmilePile operates entirely on-device regardless of your location. No data crosses international borders because no data leaves your device.")
    Spacer(modifier = Modifier.height(4.dp))
    BodyText("For users in the European Union, SmilePile's local-only approach means we are not a data controller under GDPR, as no personal data is processed by us.")

    // Changes to This Policy
    SectionHeader("Changes to This Policy")
    BodyText("We may update this Privacy Policy from time to time. When we make changes:")
    BulletList(
        listOf(
            "We will update the \"Last Updated\" date at the top of this policy",
            "Significant changes will be announced via app update notes",
            "Continued use of the app after changes constitutes acceptance of the updated policy"
        )
    )
    BodyText("We encourage you to review this policy periodically.")

    // Contact Us
    SectionHeader("Contact Us")
    BodyText("If you have questions about this Privacy Policy or SmilePile's privacy practices, please contact us:")
    Spacer(modifier = Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val contactText = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Email: ")
            }
            append("support@stackmap.app\n")
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Company: ")
            }
            append("StackMap\n")
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("Response Time: ")
            }
            append("We typically respond within 1-2 business days")
        }
        Text(text = contactText, fontSize = 16.sp)
    }

    // Summary
    SectionHeader("Summary")
    BoldText("In Plain Language:")
    BulletList(
        listOf(
            "✅ Your photos stay on your device",
            "✅ No cloud storage or sync",
            "✅ No tracking or analytics",
            "✅ No personal information collected",
            "✅ No third-party services",
            "✅ COPPA compliant for children's privacy",
            "✅ You have complete control over your data"
        )
    )
    BodyText("SmilePile is designed to give you peace of mind about your family's digital privacy.")
}

// MARK: - Text Components

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SubsectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun BodyText(text: String) = buildAnnotatedString {
    append(text)
}

@Composable
private fun BoldText(text: String) = buildAnnotatedString {
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append(text)
    }
}

@Composable
private operator fun androidx.compose.ui.text.AnnotatedString.plus(other: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        append(this@plus)
        append(other)
    }
}

@Composable
private fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "•",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
