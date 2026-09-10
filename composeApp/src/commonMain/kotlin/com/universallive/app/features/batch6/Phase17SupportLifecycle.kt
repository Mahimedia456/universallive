package com.universallive.app.features.batch6

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.*

@Composable
fun HelpCenterScreen(
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    SettingsPage("Help Center", "Find answers and recovery guidance.", onBack) {
        UlTextField(query, { query = it }, "Search help", placeholder = "Search Universal Live")

        listOf(
            "Getting Started",
            "Going Live",
            "Connections",
            "Audio",
            "Video",
            "Subscriptions",
            "Account",
        ).forEach {
            SettingCard(it, "Open help articles and guides.", onClick = {})
        }

        SettingCard(
            "Streaming Troubleshooting",
            "Black screen, no audio, connection failures and stream lag.",
            onClick = { onRoute(AppRoute.Troubleshooting) },
        )
        SettingCard(
            "Contact Support",
            "Send a support request with optional diagnostics.",
            onClick = { onRoute(AppRoute.ContactSupport) },
        )
        SettingCard(
            "Legal & Privacy",
            "Policies, terms, licenses and app version.",
            onClick = { onRoute(AppRoute.LegalPrivacy) },
        )
    }
}

@Composable
fun TroubleshootingScreen(onBack: () -> Unit) {
    SettingsPage("Streaming Troubleshooting", "Compact recovery steps for common issues.", onBack) {
        SettingCard("Cannot Start Stream", "Verify destination, capture permission and encoder readiness.", "CHECK", AppWarning)
        SettingCard("No Microphone", "Check microphone permission, mute state and active audio route.", "CHECK", AppWarning)
        SettingCard("No Device Audio", "Internal audio depends on Android/iOS app and platform support.", "CHECK", AppWarning)
        SettingCard("Black Screen", "Confirm Entire Screen capture was selected and verify protected content is not being captured.", "CHECK", AppWarning)
        SettingCard("Connection Failed", "Test the destination, authentication and RTMP/RTMPS server.", "CHECK", AppWarning)
        SettingCard("Stream Lagging", "Use Stream Readiness, reduce bitrate if network capacity is unstable, and monitor device thermals.", "CHECK", AppWarning)
    }
}

@Composable
fun ContactSupportScreen(onBack: () -> Unit) {
    var category by remember { mutableStateOf("Streaming") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var diagnostics by remember { mutableStateOf(true) }

    SettingsPage("Contact Support", "Send a clear support request.", onBack) {
        SettingCard("Issue Category") {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Streaming", "Account", "Billing").forEach {
                    FilterChip(
                        selected = category == it,
                        onClick = { category = it },
                        label = { Text(it, fontSize = 11.sp) },
                    )
                }
            }
        }

        UlTextField(subject, { subject = it }, "Subject")
        UlTextField(description, { description = it }, "Description", placeholder = "Describe the issue")

        SettingCard("Diagnostics") {
            ToggleSetting(
                "Include diagnostic summary",
                "Share non-secret app/stream status to help diagnose the issue.",
                diagnostics,
            ) { diagnostics = it }
        }

        UlPrimaryButton(
            "Submit Support Request",
            onClick = {},
            enabled = subject.isNotBlank() && description.isNotBlank(),
        )
    }
}

@Composable
fun LegalPrivacyScreen(onBack: () -> Unit) {
    SettingsPage("Legal & Privacy", "Universal Live policies and application information.", onBack) {
        SettingCard("Privacy Policy", "How Universal Live handles account, streaming and diagnostics data.", onClick = {})
        SettingCard("Terms of Service", "Terms governing use of Universal Live.", onClick = {})
        SettingCard("Acceptable Use", "Broadcast and platform usage expectations.", onClick = {})
        SettingCard("Open Source Licenses", "Third-party software notices.", onClick = {})
        SettingCard("App Version", "Universal Live mobile • development build")
    }
}

@Composable
fun DeleteAccountScreen(onBack: () -> Unit) {
    var confirmed by remember { mutableStateOf(false) }

    SettingsPage("Delete Account", "This action is intentionally difficult to perform accidentally.", onBack) {
        SettingCard(
            "Permanent deletion",
            "Deleting your account will remove backend-synced profile data, saved connections, scenes, activity and subscription-linked app records where legally permitted.",
            "DANGER",
            AppLive,
        )

        SettingCard("Before deletion") {
            Text("• Active streams must be ended.", color = AppTextSecondary, fontSize = 12.sp)
            Text("• Store subscriptions should be managed in Apple/Google billing.", color = AppTextSecondary, fontSize = 12.sp)
            Text("• Re-authentication will be required.", color = AppTextSecondary, fontSize = 12.sp)
        }

        Row(Modifier.fillMaxWidth()) {
            Checkbox(
                checked = confirmed,
                onCheckedChange = { confirmed = it },
                colors = CheckboxDefaults.colors(checkedColor = AppLive),
            )
            Text(
                "I understand this permanently deletes my Universal Live account.",
                color = AppText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = {},
            enabled = confirmed,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppLive,
                contentColor = AppText,
                disabledContainerColor = AppSurfaceRaised,
                disabledContentColor = AppText.copy(alpha = .70f),
            ),
        ) {
            Text("Delete My Account", fontWeight = FontWeight.Bold)
        }
    }
}
