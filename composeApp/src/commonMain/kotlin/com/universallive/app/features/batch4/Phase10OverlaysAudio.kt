package com.universallive.app.features.batch4

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.theme.*

@Composable
fun ChatOverlayEditorScreen(onBack: () -> Unit) {
    var backgroundOpacity by remember { mutableStateOf(.34f) }
    var textSize by remember { mutableStateOf(.42f) }
    var messageLimit by remember { mutableStateOf(6f) }

    BroadcastPage(
        title = "Chat Overlay",
        subtitle = "Style chat without covering important content.",
        onBack = onBack,
    ) {
        BroadcastCard("Preview", "Sample destination chat") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(AppBackgroundSecondary, RoundedCornerShape(14.dp))
                    .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("CreatorOne  Great stream!", color = AppText, fontSize = 12.sp)
                Text("Viewer24  That was close.", color = AppTextSecondary, fontSize = 12.sp)
                Text("Moderator  Welcome everyone.", color = AppPrimary, fontSize = 12.sp)
            }
        }

        BroadcastCard("Appearance") {
            Text("Background opacity", color = AppTextSecondary)
            Slider(
                value = backgroundOpacity,
                onValueChange = { backgroundOpacity = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            Text("Text size", color = AppTextSecondary)
            Slider(
                value = textSize,
                onValueChange = { textSize = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
        }

        BroadcastCard("Messages") {
            Text("Message limit: ${messageLimit.toInt()}", color = AppText)
            Slider(
                value = messageLimit,
                onValueChange = { messageLimit = it },
                valueRange = 3f..12f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            Text("Message duration: 12 seconds", color = AppTextSecondary, fontSize = 12.sp)
            Text("Position: Bottom left", color = AppTextSecondary, fontSize = 12.sp)
        }

        UlPrimaryButton("Apply Chat Overlay", onClick = onBack)
    }
}

@Composable
fun AlertEditorScreen(onBack: () -> Unit) {
    var follow by remember { mutableStateOf(true) }
    var subscribe by remember { mutableStateOf(true) }
    var sound by remember { mutableStateOf(true) }

    BroadcastPage(
        title = "Alert Overlay",
        subtitle = "Configure creator-event alerts.",
        onBack = onBack,
    ) {
        BroadcastCard("Alert types") {
            ToggleRow("Follow", "Show supported follow alerts.", follow) { follow = it }
            ToggleRow("Subscribe", "Show supported subscription alerts.", subscribe) { subscribe = it }
        }

        BroadcastCard("Layout", "Compact • Top center") {
            Text("Image: Default creator alert", color = AppTextSecondary, fontSize = 12.sp)
            Text("Text: Event + creator name", color = AppTextSecondary, fontSize = 12.sp)
            Text("Duration: 5 seconds", color = AppTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            ToggleRow("Sound", "Play alert audio when supported.", sound) { sound = it }
        }

        UlSecondaryButton("Test Alert", onClick = {})
        UlPrimaryButton("Save Alert", onClick = onBack)
    }
}

@Composable
fun GoalOverlayEditorScreen(onBack: () -> Unit) {
    var title by remember { mutableStateOf("Road to 1,000") }
    var goal by remember { mutableStateOf("1000") }
    var current by remember { mutableStateOf("620") }

    BroadcastPage(
        title = "Goal Overlay",
        subtitle = "Show creator progress during the broadcast.",
        onBack = onBack,
    ) {
        UlTextField(title, { title = it }, "Goal title")
        UlTextField(goal, { goal = it.filter(Char::isDigit) }, "Goal value")
        UlTextField(current, { current = it.filter(Char::isDigit) }, "Current value")

        BroadcastCard("Preview", "620 / 1000") {
            LinearProgressIndicator(
                progress = { .62f },
                modifier = Modifier.fillMaxWidth(),
                color = AppPrimary,
                trackColor = AppSurfaceInteractive,
            )
            Spacer(Modifier.height(6.dp))
            Text(title, color = AppText, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }

        BroadcastCard("Appearance") {
            Text("Style: Minimal cyan", color = AppTextSecondary, fontSize = 12.sp)
            Text("Position: Top center", color = AppTextSecondary, fontSize = 12.sp)
        }

        UlPrimaryButton("Apply Goal", onClick = onBack)
    }
}

@Composable
fun AudioMixerV2Screen(
    microphoneEnabled: Boolean,
    deviceAudioEnabled: Boolean,
    onMicrophoneChanged: (Boolean) -> Unit,
    onDeviceAudioChanged: (Boolean) -> Unit,
    onAdvanced: () -> Unit,
    onBack: () -> Unit,
) {
    var micLevel by remember { mutableStateOf(.78f) }
    var deviceLevel by remember { mutableStateOf(.82f) }
    var alertsLevel by remember { mutableStateOf(.7f) }

    BroadcastPage(
        title = "Audio Mixer",
        subtitle = "Balance every active audio source.",
        onBack = onBack,
    ) {
        BroadcastCard(
            title = "Microphone",
            status = if (microphoneEnabled) "ON" else "MUTED",
            statusColor = if (microphoneEnabled) AppSuccess else AppTextMuted,
        ) {
            Slider(
                value = micLevel,
                onValueChange = { micLevel = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            ToggleRow(
                "Microphone active",
                "Voice input",
                microphoneEnabled,
                onMicrophoneChanged,
            )
        }

        BroadcastCard(
            title = "Device / Game Audio",
            status = if (deviceAudioEnabled) "ON" else "MUTED",
            statusColor = if (deviceAudioEnabled) AppSuccess else AppTextMuted,
        ) {
            Slider(
                value = deviceLevel,
                onValueChange = { deviceLevel = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            ToggleRow(
                "Device audio active",
                "Supported app and game playback",
                deviceAudioEnabled,
                onDeviceAudioChanged,
            )
        }

        BroadcastCard("Alerts", status = "ON") {
            Slider(
                value = alertsLevel,
                onValueChange = { alertsLevel = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
        }

        UlSecondaryButton("Advanced Audio", onClick = onAdvanced)
    }
}

@Composable
fun AudioAdvancedScreen(onBack: () -> Unit) {
    var gain by remember { mutableStateOf(.54f) }
    var noiseSuppression by remember { mutableStateOf(true) }
    var echoCancellation by remember { mutableStateOf(true) }
    var monitoring by remember { mutableStateOf(false) }

    BroadcastPage(
        title = "Advanced Audio",
        subtitle = "Platform-safe microphone and monitoring controls.",
        onBack = onBack,
    ) {
        BroadcastCard("Microphone input", "Default device microphone", "ACTIVE")

        BroadcastCard("Gain") {
            Slider(
                value = gain,
                onValueChange = { gain = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            Text("${(gain * 100).toInt()}%", color = AppTextSecondary, fontSize = 12.sp)
        }

        BroadcastCard("Processing") {
            ToggleRow(
                "Noise suppression",
                "Reduce continuous background noise when supported.",
                noiseSuppression,
            ) { noiseSuppression = it }
            ToggleRow(
                "Echo cancellation",
                "Reduce speaker-to-microphone echo when supported.",
                echoCancellation,
            ) { echoCancellation = it }
            ToggleRow(
                "Monitoring",
                "Monitor audio only when the platform route permits it.",
                monitoring,
            ) { monitoring = it }
        }

        BroadcastCard(
            "Device audio",
            "Level is controlled in the Audio Mixer. Internal playback capture depends on platform support.",
        )

        UlPrimaryButton("Save Audio Settings", onClick = onBack)
    }
}
