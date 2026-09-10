package com.universallive.app.features.batch6

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.features.finalpolish.ProfileActionRow
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.streaming.connections.RtmpProfilesState
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.*

@Composable
fun ProfileV2Screen(
    onRoute: (AppRoute) -> Unit,
    onDestinationChanged: (AppDestination) -> Unit,
) {
    AppScaffold(
        title = "Profile",
        selected = AppDestination.Settings,
        onDestinationChanged = onDestinationChanged,
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .background(AppSurfaceRaised, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("UL", color = AppPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Universal Live Creator", color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("@creator", color = AppTextSecondary, fontSize = 12.sp)
                    Text("creator@example.com", color = AppTextMuted, fontSize = 11.sp)
                }
                UlStatusBadge("FREE", AppPrimary)
            }

            ProfileActionRow(
                icon = "C",
                title = "Channel Connections",
                subtitle = "Connect, edit, test or remove YouTube, Facebook, Twitch and Custom RTMP.",
                onClick = { onRoute(AppRoute.Connections) },
            )
            ProfileActionRow(
                icon = "P",
                title = "Plans & Billing",
                subtitle = "Free, Creator or Pro membership and billing controls.",
                badge = "PLAN",
                onClick = { onRoute(AppRoute.Plans) },
            )
            ProfileActionRow(
                icon = "A",
                title = "Account & Security",
                subtitle = "Profile, email, password and sessions.",
                onClick = { onRoute(AppRoute.AccountSecurity) },
            )
            ProfileActionRow(
                icon = "S",
                title = "Streaming Defaults",
                subtitle = "Scene, destination, privacy and orientation.",
                onClick = { onRoute(AppRoute.StreamingDefaults) },
            )
            ProfileActionRow(
                icon = "V",
                title = "Video & Audio Defaults",
                subtitle = "Resolution, FPS, bitrate and microphone defaults.",
                onClick = { onRoute(AppRoute.VideoAudioDefaults) },
            )
            ProfileActionRow(
                icon = "N",
                title = "Appearance & Notifications",
                subtitle = "Theme and notification preferences.",
                onClick = { onRoute(AppRoute.AppearanceNotifications) },
            )
            ProfileActionRow(
                icon = "?",
                title = "Help & Support",
                subtitle = "Guides, troubleshooting and support.",
                onClick = { onRoute(AppRoute.HelpCenter) },
            )
        }
    }
}

@Composable
fun AccountSecurityScreen(
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("Universal Live Creator") }
    var username by remember { mutableStateOf("creator") }
    var email by remember { mutableStateOf("creator@example.com") }

    SettingsPage("Account & Security", "Manage your Universal Live account.", onBack) {
        UlTextField(name, { name = it }, "Full name")
        UlTextField(username, { username = it }, "Username")
        UlTextField(email, { email = it }, "Email")

        SettingCard("Password", "Change your account password.", onClick = {})
        SettingCard("Active Sessions", "Review devices signed into your account.", onClick = {})

        UlPrimaryButton("Save Account", onClick = {})
        UlSecondaryButton("Sign Out", onClick = {})

        Spacer(Modifier.height(10.dp))
        Text("DANGER ZONE", color = AppLive, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        SettingCard(
            "Delete Account",
            "Permanently delete your Universal Live account and backend-synced data.",
            status = "DANGER",
            statusColor = AppLive,
            onClick = { onRoute(AppRoute.DeleteAccount) },
        )
    }
}

@Composable
fun StreamingDefaultsScreen(
    streamState: StreamConfigState,
    profilesState: RtmpProfilesState,
    onBack: () -> Unit,
) {
    val config = streamState.config
    val defaultProfile = profilesState.activeProfiles.firstOrNull()?.name ?: "No default destination"

    SettingsPage("Streaming Defaults", "Choose sensible defaults for new broadcasts.", onBack) {
        SettingCard("Default Scene", "Main")
        SettingCard("Default Destination", defaultProfile)
        SettingCard("Privacy", "Public")
        SettingCard("Orientation", config.orientation.label)
        SettingCard("Quality Preset", "${config.resolution.label} • ${config.fps.value} fps • ${config.bitrateLabel}")
        UlPrimaryButton("Save Streaming Defaults", onClick = {})
    }
}

@Composable
fun VideoAudioDefaultsScreen(
    streamState: StreamConfigState,
    onBack: () -> Unit,
) {
    val config = streamState.config

    SettingsPage("Video & Audio Defaults", "Default capture and encoder preferences.", onBack) {
        SettingCard("Video") {
            Text("Resolution: ${config.resolution.label}", color = AppTextSecondary, fontSize = 12.sp)
            Text("FPS: ${config.fps.value}", color = AppTextSecondary, fontSize = 12.sp)
            Text("Bitrate: ${config.bitrateLabel}", color = AppTextSecondary, fontSize = 12.sp)
        }

        SettingCard("Audio") {
            ToggleSetting(
                "Microphone",
                "Use microphone by default.",
                config.microphoneEnabled,
            ) { streamState.setMicrophoneEnabled(it) }

            ToggleSetting(
                "Device / Game Audio",
                "Capture supported playback audio.",
                config.internalAudioEnabled,
            ) { streamState.setInternalAudioEnabled(it) }
        }

        SettingCard("Advanced Encoder", "Hardware encoder preferences remain collapsed by default.")
        UlPrimaryButton("Save Defaults", onClick = {})
    }
}

@Composable
fun AppearanceNotificationsScreen(onBack: () -> Unit) {
    var systemTheme by remember { mutableStateOf(true) }
    var streamNotifs by remember { mutableStateOf(true) }
    var accountNotifs by remember { mutableStateOf(true) }
    var subscriptionNotifs by remember { mutableStateOf(true) }
    var systemNotifs by remember { mutableStateOf(true) }
    var creatorTips by remember { mutableStateOf(false) }

    SettingsPage("Appearance & Notifications", "Control app appearance and alerts.", onBack) {
        SettingCard("Theme") {
            ToggleSetting(
                "Follow system",
                "Universal Live dark remains the primary visual design.",
                systemTheme,
            ) { systemTheme = it }

            Text(
                if (systemTheme) "System" else "Dark",
                color = AppPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        SettingCard("Notifications") {
            ToggleSetting("Stream", "Live status and stream problems.", streamNotifs) { streamNotifs = it }
            ToggleSetting("Account", "Security and account changes.", accountNotifs) { accountNotifs = it }
            ToggleSetting("Subscription", "Billing and entitlement changes.", subscriptionNotifs) { subscriptionNotifs = it }
            ToggleSetting("System", "Service and maintenance notices.", systemNotifs) { systemNotifs = it }
            ToggleSetting("Creator Tips", "Optional product tips.", creatorTips) { creatorTips = it }
        }

        UlPrimaryButton("Save Preferences", onClick = {})
    }
}
