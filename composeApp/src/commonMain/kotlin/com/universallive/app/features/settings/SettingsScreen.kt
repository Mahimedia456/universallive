package com.universallive.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.AppScaffold
import com.universallive.app.navigation.AppDestination
import com.universallive.app.streaming.connections.RtmpProfilesState
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.*

@Composable
fun SettingsScreen(
    streamState: StreamConfigState,
    profilesState: RtmpProfilesState,
    onDestinationChanged: (AppDestination) -> Unit,
) {
    val config = streamState.config

    AppScaffold(
        title = "Settings",
        selected = AppDestination.Settings,
        onDestinationChanged = onDestinationChanged,
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))

            SettingsSection("Streaming connections", "Manage every platform and custom ingest endpoint from one place.") {
                SettingValue("Saved destinations", profilesState.profiles.size.toString())
                SettingValue("Enabled for broadcast", profilesState.activeProfiles.size.toString())
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onDestinationChanged(AppDestination.Connections) },
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimarySoft, contentColor = AppText, disabledContentColor = AppText.copy(alpha = .70f)),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("MANAGE CONNECTIONS", color = AppBackground, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(8.dp))
                Text("YouTube • Facebook • TikTok • Custom RTMP/RTMPS", color = AppTextMuted, fontSize = 11.sp)
            }

            Spacer(Modifier.height(14.dp))

            SettingsSection("Default stream profile", "These values are used when you open Live Studio.") {
                SettingValue("Video", config.videoSummary)
                SettingValue("Bitrate", config.bitrateLabel)
                SettingValue("Orientation", config.orientation.label)
                SettingValue("Microphone", if (config.microphoneEnabled) "Enabled" else "Disabled")
                SettingValue("Internal audio", if (config.internalAudioEnabled) "Enabled" else "Disabled")
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = streamState::reset,
                    colors = ButtonDefaults.buttonColors(containerColor = AppSurfaceRaised, contentColor = AppText, disabledContentColor = AppText.copy(alpha = .70f)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("RESET STREAM DEFAULTS", color = AppText, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))
            SettingsSection("App", "Local broadcast tools and device configuration.") {
                SettingValue("Version", "0.30.0")
                SettingValue("Broadcast engine", "Android native")
                SettingValue("Credential storage", "Encrypted")
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(22.dp))
            .border(BorderStroke(1.dp, AppBorder), RoundedCornerShape(22.dp))
            .padding(18.dp),
    ) {
        Text(title, color = AppText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = AppTextMuted, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(13.dp))
        content()
    }
}

@Composable
private fun SettingValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, color = AppTextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = AppText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
