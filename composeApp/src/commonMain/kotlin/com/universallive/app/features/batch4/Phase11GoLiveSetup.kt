package com.universallive.app.features.batch4

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.capture.CaptureMode
import com.universallive.app.streaming.connections.RtmpProfilesState
import com.universallive.app.streaming.facecam.FacecamState
import com.universallive.app.streaming.model.StreamFps
import com.universallive.app.streaming.model.StreamOrientation
import com.universallive.app.streaming.model.StreamResolution
import com.universallive.app.streaming.overlays.OverlayState
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.*

@Composable
fun StreamDetailsV2Screen(
    sceneState: SceneState,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    var title by remember { mutableStateOf("Tonight's Live Session") }
    var description by remember { mutableStateOf("") }
    var privacy by remember { mutableStateOf("Public") }

    AppScaffold(
        title = "Go Live",
        selected = AppDestination.GoLive,
        onDestinationChanged = onDestination,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Ready to Go Live",
                color = AppText,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Set up your stream and share your world.",
                color = AppTextSecondary,
                fontSize = 13.sp,
            )

            UlTextField(title, { title = it.take(100) }, "Stream title")
            UlTextField(description, { description = it.take(500) }, "Description", placeholder = "Optional")

            BroadcastCard("Privacy") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Public", "Unlisted", "Private").forEach {
                        FilterChip(
                            selected = privacy == it,
                            onClick = { privacy = it },
                            label = { Text(it) },
                        )
                    }
                }
            }

            BroadcastCard(
                "Scene",
                "${sceneState.activeScene.name} • current broadcast scene",
                status = "READY",
                onClick = { onDestination(AppDestination.Scenes) },
            )

            UlPrimaryButton(
                "Next: Destinations",
                onClick = { onRoute(AppRoute.DestinationSelection) },
            )
        }
    }
}

@Composable
fun DestinationSelectionScreen(
    profilesState: RtmpProfilesState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val profiles = profilesState.profiles
    var selectedId by remember {
        mutableStateOf(profiles.firstOrNull { it.enabled }?.id)
    }
    var showUpgrade by remember { mutableStateOf(false) }

    BroadcastPage(
        title = "Destinations",
        subtitle = "Choose where this broadcast should go live.",
        onBack = onBack,
    ) {
        if (profiles.isEmpty()) {
            BroadcastCard(
                "No connections",
                "Connect YouTube, Facebook or a Custom RTMP destination first.",
                "SETUP",
                AppWarning,
            )
        } else {
            profiles.forEach { profile ->
                val selected = selectedId == profile.id
                BroadcastCard(
                    title = profile.name,
                    subtitle = "${profile.platform.label} • ${if (profile.isValid) "Connection available" else "Needs setup"}",
                    status = if (selected) "SELECTED" else null,
                    statusColor = AppPrimary,
                    onClick = { selectedId = profile.id },
                )
            }
        }

        BroadcastCard(
            "Free plan",
            "Connect, edit and manage multiple channels on Free. Free limits only simultaneous LIVE output to one destination.",
        )

        TextButton(onClick = { showUpgrade = true }) {
            Text("+ Manage another destination", color = AppPrimary, fontWeight = FontWeight.SemiBold)
        }

        UlPrimaryButton(
            "Next: Quality",
            onClick = { onRoute(AppRoute.StreamQuality) },
            enabled = selectedId != null,
        )
    }

    if (showUpgrade) {
        AlertDialog(
            onDismissRequest = { showUpgrade = false },
            containerColor = AppSurfaceRaised,
            title = { Text("Multistream with Creator", color = AppText) },
            text = {
                Text(
                    "Your connected channels stay available. Upgrade only when you want to broadcast to more than one destination simultaneously.",
                    color = AppTextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { showUpgrade = false }) {
                    Text("View Plans", color = AppPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgrade = false }) {
                    Text("Not Now", color = AppTextSecondary)
                }
            },
        )
    }
}

@Composable
fun StreamQualityV2Screen(
    streamState: StreamConfigState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val config = streamState.config

    BroadcastPage(
        title = "Stream Quality",
        subtitle = "Choose a stable output for your network.",
        onBack = onBack,
    ) {
        BroadcastCard("Quick Preset") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = false,
                    onClick = {
                        streamState.setResolution(StreamResolution.P720)
                        streamState.setFps(StreamFps.FPS30)
                        streamState.setBitrateKbps(4500)
                    },
                    label = { Text("Auto") },
                )
                FilterChip(
                    selected = config.resolution == StreamResolution.P720,
                    onClick = { streamState.setResolution(StreamResolution.P720) },
                    label = { Text("720p") },
                )
                FilterChip(
                    selected = config.resolution == StreamResolution.P1080,
                    onClick = { streamState.setResolution(StreamResolution.P1080) },
                    label = { Text("1080p") },
                )
            }
        }

        BroadcastCard("Frame Rate") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamFps.entries.forEach {
                    FilterChip(
                        selected = config.fps == it,
                        onClick = { streamState.setFps(it) },
                        label = { Text("${it.value} fps") },
                    )
                }
            }
        }

        BroadcastCard("Bitrate", "${config.bitrateLabel} target") {
            Slider(
                value = config.bitrateKbps.toFloat(),
                onValueChange = {
                    streamState.setBitrateKbps(
                        ((it / 500f).toInt() * 500).coerceIn(2000, 12000),
                    )
                },
                valueRange = 2000f..12000f,
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
        }

        BroadcastCard("Orientation") {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StreamOrientation.entries.forEach {
                    FilterChip(
                        selected = config.orientation == it,
                        onClick = { streamState.setOrientation(it) },
                        label = { Text(it.label, fontSize = 11.sp) },
                    )
                }
            }
        }

        BroadcastCard(
            "Recommended",
            "${config.resolution.label} • ${config.fps.value} fps • ${config.bitrateLabel}",
            "READY",
        )

        UlPrimaryButton(
            "Next: Pre-flight",
            onClick = { onRoute(AppRoute.Preflight) },
        )
    }
}

@Composable
fun PreflightV2Screen(
    streamState: StreamConfigState,
    profilesState: RtmpProfilesState,
    sceneState: SceneState,
    captureController: CaptureController,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val profile = profilesState.activeProfiles.firstOrNull()
    val config = streamState.config
    val destinationReady = profile != null

    BroadcastPage(
        title = "Pre-flight Check",
        subtitle = "Make sure every broadcast system is ready.",
        onBack = onBack,
    ) {
        listOf(
            Triple("Network", "Ready", true),
            Triple("Microphone", if (config.microphoneEnabled) "Ready" else "Off", true),
            Triple("Device Audio", if (config.internalAudioEnabled) "Ready" else "Off", true),
            Triple("Screen Capture", captureController.requestedCaptureMode.label, true),
            Triple("Scene", sceneState.activeScene.name, true),
            Triple("Destination", profile?.name ?: "Not configured", destinationReady),
            Triple("Encoder", "${config.resolution.label} • ${config.fps.value} fps", true),
        ).forEach { item ->
            BroadcastCard(
                title = item.first,
                subtitle = item.second,
                status = if (item.third) "READY" else "FIX",
                statusColor = if (item.third) AppSuccess else AppWarning,
            )
        }

        if (!destinationReady) {
            Text(
                "Add a valid RTMP destination before starting the live broadcast.",
                color = AppWarning,
                fontSize = 12.sp,
            )
        }

        UlPrimaryButton(
            "Ready to Go Live",
            onClick = { onRoute(AppRoute.Countdown) },
            enabled = destinationReady,
        )
    }
}

@Composable
fun CountdownScreen(
    streamState: StreamConfigState,
    profilesState: RtmpProfilesState,
    captureController: CaptureController,
    facecamState: FacecamState,
    overlayState: OverlayState,
    sceneState: SceneState,
    onLive: () -> Unit,
    onCancel: () -> Unit,
) {
    var count by remember { mutableStateOf(3) }
    val config = streamState.config

    Box(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding(),
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(132.dp)
                    .clip(CircleShape)
                    .background(AppPrimary.copy(alpha = .08f))
                    .border(2.dp, AppPrimary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    count.toString(),
                    color = AppText,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "Broadcast ready",
                color = AppText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                profilesState.activeProfiles.firstOrNull()?.name ?: "Destination",
                color = AppTextSecondary,
            )

            Spacer(Modifier.height(28.dp))

            if (count > 1) {
                UlPrimaryButton(
                    "Continue • ${count - 1}",
                    onClick = { count-- },
                )
            } else {
                UlPrimaryButton(
                    "START BROADCAST",
                    onClick = {
                        prepareCapture(
                            streamState = streamState,
                            profilesState = profilesState,
                            captureController = captureController,
                            facecamState = facecamState,
                            overlayState = overlayState,
                            sceneState = sceneState,
                        )
                        captureController.start()
                        onLive()
                    },
                )
            }

            Spacer(Modifier.height(10.dp))
            UlSecondaryButton("Cancel", onClick = onCancel)
        }
    }
}

private fun prepareCapture(
    streamState: StreamConfigState,
    profilesState: RtmpProfilesState,
    captureController: CaptureController,
    facecamState: FacecamState,
    overlayState: OverlayState,
    sceneState: SceneState,
) {
    val config = streamState.config
    val activeProfile = profilesState.activeProfiles.firstOrNull()

    captureController.setCaptureMode(CaptureMode.ENTIRE_DEVICE)

    captureController.configureAudio(
        microphone = config.microphoneEnabled,
        internalAudio = config.internalAudioEnabled,
    )

    captureController.configureVideo(
        width = config.resolution.width,
        height = config.resolution.height,
        fps = config.fps.value,
        bitrateKbps = config.bitrateKbps,
        orientation = config.orientation.label,
    )

    captureController.configurePublish(
        serverUrl = activeProfile?.serverUrl.orEmpty(),
        streamKey = activeProfile?.streamKey.orEmpty(),
        targetName = activeProfile?.name.orEmpty(),
    )

    captureController.configureFacecam(
        enabled = facecamState.config.enabled,
        lens = facecamState.config.lens.label,
        shape = facecamState.config.shape.label,
        x = facecamState.config.x,
        y = facecamState.config.y,
        size = facecamState.config.size,
        mirrored = facecamState.config.mirrored,
    )

    val activeIds = sceneState.activeScene.layerIds.toSet()
    val payload = overlayState.layers
        .filter { it.enabled && it.id in activeIds }
        .joinToString("§") { layer ->
            listOf(
                layer.kind.name,
                layer.x,
                layer.y,
                layer.width,
                layer.opacity,
                layer.text,
                layer.assetPath,
            ).joinToString("¦") {
                it.toString()
                    .replace("§", " ")
                    .replace("¦", " ")
            }
        }

    captureController.configureOverlays(
        sceneName = sceneState.activeScene.name,
        payload = payload,
    )
}
