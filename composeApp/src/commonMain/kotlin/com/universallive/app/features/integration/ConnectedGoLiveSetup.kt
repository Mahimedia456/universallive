package com.universallive.app.features.integration

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.AppScaffold
import com.universallive.app.components.UlPrimaryButton
import com.universallive.app.components.UlSecondaryButton
import com.universallive.app.components.UlTextField
import com.universallive.app.features.batch4.BroadcastCard
import com.universallive.app.features.batch4.BroadcastPage
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.integration.PublishConfig
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.capture.CaptureMode
import com.universallive.app.streaming.facecam.FacecamState
import com.universallive.app.streaming.model.StreamFps
import com.universallive.app.streaming.model.StreamOrientation
import com.universallive.app.streaming.model.StreamResolution
import com.universallive.app.streaming.overlays.OverlayState
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.*
import kotlinx.coroutines.launch

@Composable
fun ConnectedStreamDetailsScreen(
    state: MobileIntegrationState,
    sceneState: SceneState,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    LaunchedEffect(Unit) {
        state.refreshConnections()
        if (state.membership == null) state.refreshAccount()
    }

    val selected = state.selectedLiveConnectionId?.let { id ->
        state.connections.firstOrNull { it.id == id }
    }

    AppScaffold(
        title = "Go Live",
        selected = AppDestination.GoLive,
        onDestinationChanged = onDestination,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Ready to broadcast", color = AppText, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(
                "Set your stream details, choose a real saved destination and run pre-flight before Android starts capture.",
                color = AppTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            state.error?.let {
                BroadcastCard("Action required", it, "FIX", AppWarning)
            }

            UlTextField(state.liveTitle, { state.liveTitle = it.take(100) }, "Stream title")
            UlTextField(state.liveDescription, { state.liveDescription = it.take(500) }, "Description", placeholder = "Optional")

            BroadcastCard("Privacy") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Public", "Unlisted", "Private").forEach { option ->
                        FilterChip(
                            selected = state.livePrivacy == option,
                            onClick = { state.livePrivacy = option },
                            label = { Text(option) },
                        )
                    }
                }
            }

            BroadcastCard(
                "Scene",
                "${sceneState.activeScene.name} • current local broadcast scene",
                status = "READY",
                onClick = { onDestination(AppDestination.Scenes) },
            )

            if (selected != null && selected.readyToPublish) {
                BroadcastCard(
                    "Destination",
                    "${selected.displayName} • ${selected.platform.replace("_", " ")}",
                    status = "READY",
                    statusColor = AppSuccess,
                    onClick = { onRoute(AppRoute.DestinationSelection) },
                )
            } else {
                BroadcastCard(
                    "Destination required",
                    if (state.connections.isEmpty()) "No saved destinations yet." else "Choose a destination with valid RTMP credentials.",
                    status = "SETUP",
                    statusColor = AppWarning,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onRoute(AppRoute.Connections) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppText),
                ) { Text("Manage", fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { onRoute(AppRoute.AddConnection) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimary, contentColor = Color.White),
                ) { Text("Add Destination", fontWeight = FontWeight.Bold) }
            }

            UlPrimaryButton(
                "Continue to Destination",
                onClick = { onRoute(AppRoute.DestinationSelection) },
                enabled = state.liveTitle.isNotBlank(),
            )
        }
    }
}

@Composable
fun ConnectedDestinationSelectionScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { state.refreshConnections() }
    val membership = state.membership
    val ready = state.connections.filter { it.readyToPublish && it.isEnabled }

    BroadcastPage(
        title = "Choose Destination",
        subtitle = "Select the channel that will receive this device broadcast.",
        onBack = onBack,
    ) {
        state.error?.let { BroadcastCard("Could not load destination", it, "RETRY", AppWarning) }

        if (state.accountLoading && state.connections.isEmpty()) {
            BroadcastCard("Loading", "Fetching your saved destinations…")
        }

        if (!state.accountLoading && ready.isEmpty()) {
            BroadcastCard(
                "No ready destinations",
                "Add a channel and save its RTMP/RTMPS server plus stream key before going live.",
                "SETUP",
                AppWarning,
            )
            UlPrimaryButton("Add Destination", onClick = { onRoute(AppRoute.AddConnection) })
            UlSecondaryButton("Manage Connections", onClick = { onRoute(AppRoute.Connections) })
        } else {
            ready.forEach { item ->
                val selected = state.selectedLiveConnectionId == item.id
                BroadcastCard(
                    title = item.displayName,
                    subtitle = "${item.platform.replace("_", " ").uppercase()} • ${if (item.isDefault) "Default • " else ""}ready to publish",
                    status = if (selected) "SELECTED" else "READY",
                    statusColor = if (selected) AppPrimary else AppSuccess,
                    onClick = { state.chooseLiveConnection(item.id) },
                )
            }

            BroadcastCard(
                membership?.planName ?: "Membership",
                "Plan allowance: up to ${membership?.maxSimultaneousDestinations ?: 1} simultaneous destination(s). This native direct-publish pipeline starts one RTMP output from the phone; extra channels remain saved and manageable.",
                membership?.badge,
                AppPrimary,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onRoute(AppRoute.Connections) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppText),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Manage", fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { onRoute(AppRoute.AddConnection) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Add New", fontWeight = FontWeight.Bold) }
            }

            UlPrimaryButton(
                "Next: Stream Quality",
                onClick = { onRoute(AppRoute.StreamQuality) },
                enabled = state.selectedLiveConnectionId != null,
            )
        }
    }
}

@Composable
fun ConnectedStreamQualityScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val config = streamState.config
    val maxResolution = state.membership?.maxResolution?.lowercase() ?: "720p"
    val allow1080 = maxResolution.contains("1080") || maxResolution.contains("1440") || maxResolution.contains("4k")

    LaunchedEffect(allow1080) {
        if (!allow1080 && streamState.config.resolution == StreamResolution.P1080) {
            streamState.setResolution(StreamResolution.P720)
            if (streamState.config.bitrateKbps > 6000) streamState.setBitrateKbps(4500)
        }
    }

    BroadcastPage(
        title = "Stream Quality",
        subtitle = "Choose a stable output within your ${state.membership?.planName ?: "current"} plan.",
        onBack = onBack,
    ) {
        BroadcastCard("Quick Preset") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = config.resolution == StreamResolution.P720 && config.fps == StreamFps.FPS30,
                    onClick = {
                        streamState.setResolution(StreamResolution.P720)
                        streamState.setFps(StreamFps.FPS30)
                        streamState.setBitrateKbps(4500)
                    },
                    label = { Text("Stable") },
                )
                FilterChip(
                    selected = config.resolution == StreamResolution.P720,
                    onClick = { streamState.setResolution(StreamResolution.P720) },
                    label = { Text("720p") },
                )
                FilterChip(
                    selected = config.resolution == StreamResolution.P1080,
                    enabled = allow1080,
                    onClick = { streamState.setResolution(StreamResolution.P1080) },
                    label = { Text("1080p") },
                )
            }
        }

        if (!allow1080) {
            BroadcastCard(
                "Plan quality limit",
                "${state.membership?.planName ?: "Free"} currently allows ${state.membership?.maxResolution ?: "720p"}. Upgrade only if you need higher output resolution.",
                "${state.membership?.maxResolution ?: "720p"}",
                AppPrimary,
            )
        }

        BroadcastCard("Frame Rate") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamFps.entries.forEach { fps ->
                    FilterChip(
                        selected = config.fps == fps,
                        onClick = { streamState.setFps(fps) },
                        label = { Text("${fps.value} fps") },
                    )
                }
            }
        }

        BroadcastCard("Bitrate", "${config.bitrateLabel} target") {
            Slider(
                value = config.bitrateKbps.toFloat(),
                onValueChange = {
                    streamState.setBitrateKbps(((it / 500f).toInt() * 500).coerceIn(2000, if (allow1080) 12000 else 6000))
                },
                valueRange = 2000f..(if (allow1080) 12000f else 6000f),
                colors = SliderDefaults.colors(thumbColor = AppPrimary, activeTrackColor = AppPrimary),
            )
        }

        BroadcastCard("Orientation") {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StreamOrientation.entries.forEach { orientation ->
                    FilterChip(
                        selected = config.orientation == orientation,
                        onClick = { streamState.setOrientation(orientation) },
                        label = { Text(orientation.label, fontSize = 11.sp) },
                    )
                }
            }
        }

        BroadcastCard("Output", "${config.resolution.label} • ${config.fps.value} fps • ${config.bitrateLabel}", "READY")
        UlPrimaryButton("Next: Pre-flight", onClick = { onRoute(AppRoute.Preflight) })
    }
}

@Composable
fun ConnectedPreflightScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    sceneState: SceneState,
    captureController: CaptureController,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val connection = state.selectedLiveConnectionId?.let { id -> state.connections.firstOrNull { it.id == id } }
    val config = streamState.config

    LaunchedEffect(connection?.id) {
        if (connection != null) state.prepareSelectedPublishConfig()
    }

    val publishReady = state.preparedPublishConfig?.connectionId == connection?.id &&
        state.preparedPublishConfig?.serverUrl?.isNotBlank() == true &&
        state.preparedPublishConfig?.streamKey?.isNotBlank() == true

    BroadcastPage(
        title = "Pre-flight Check",
        subtitle = "Universal Live validates the real publish route before requesting screen capture.",
        onBack = onBack,
    ) {
        listOf(
            Triple("Network", "Ready for capture", true),
            Triple("Microphone", if (config.microphoneEnabled) "Enabled" else "Off", true),
            Triple("Device Audio", if (config.internalAudioEnabled) "Enabled" else "Off", true),
            Triple("Screen Capture", captureController.requestedCaptureMode.label, true),
            Triple("Scene", sceneState.activeScene.name, true),
            Triple("Destination", connection?.displayName ?: "Not selected", connection != null),
            Triple("Publish credentials", if (state.loading) "Validating encrypted vault…" else if (publishReady) "Validated for this device session" else "Not ready", publishReady),
            Triple("Encoder", "${config.resolution.label} • ${config.fps.value} fps", true),
        ).forEach { item ->
            BroadcastCard(
                title = item.first,
                subtitle = item.second,
                status = if (item.third) "READY" else if (state.loading) "CHECKING" else "FIX",
                statusColor = if (item.third) AppSuccess else AppWarning,
            )
        }

        state.error?.let { BroadcastCard("Cannot start yet", it, "FIX", AppWarning) }

        val retryScope = rememberCoroutineScope()
        if (!publishReady && !state.loading) {
            Button(
                onClick = { retryScope.launch { state.prepareSelectedPublishConfig() } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppSurfaceInteractive, contentColor = Color.White),
            ) { Text("Retry Validation", fontWeight = FontWeight.Bold) }
        }

        UlPrimaryButton(
            "Ready to Go Live",
            onClick = { onRoute(AppRoute.Countdown) },
            enabled = publishReady && !state.loading,
            loading = state.loading,
        )
    }
}

@Composable
fun ConnectedCountdownScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    overlayState: OverlayState,
    sceneState: SceneState,
    onLive: () -> Unit,
    onCancel: () -> Unit,
) {
    var count by remember { mutableStateOf(3) }
    val scope = rememberCoroutineScope()
    val publishConfig = state.preparedPublishConfig

    Box(Modifier.fillMaxSize().background(AppBackground).systemBarsPadding()) {
        Column(
            Modifier.align(Alignment.Center).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(132.dp).clip(CircleShape).background(AppPrimary.copy(alpha = .08f)).border(2.dp, AppPrimary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(count.toString(), color = AppText, fontSize = 62.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(22.dp))
            Text("Broadcast ready", color = AppText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(publishConfig?.displayName ?: "Destination", color = AppTextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("The backend session is created before capture starts.", color = AppTextMuted, fontSize = 11.sp)
            state.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = AppWarning, fontSize = 12.sp)
            }
            Spacer(Modifier.height(28.dp))

            if (count > 1) {
                UlPrimaryButton("Continue • ${count - 1}", onClick = { count-- })
            } else {
                UlPrimaryButton(
                    "START BROADCAST",
                    onClick = {
                        val cfg = publishConfig ?: return@UlPrimaryButton
                        scope.launch {
                            val connectionId = state.selectedLiveConnectionId ?: return@launch
                            if (state.beginBroadcast(state.liveTitle, listOf(connectionId), state.selectedScene?.id)) {
                                prepareConnectedCapture(
                                    publishConfig = cfg,
                                    streamState = streamState,
                                    captureController = captureController,
                                    facecamState = facecamState,
                                    overlayState = overlayState,
                                    sceneState = sceneState,
                                )
                                captureController.start()
                                onLive()
                            }
                        }
                    },
                    enabled = publishConfig != null && !state.loading,
                    loading = state.loading,
                )
            }

            Spacer(Modifier.height(10.dp))
            UlSecondaryButton("Cancel", onClick = onCancel)
        }
    }
}

@Composable
fun ConnectedEndStreamConfirmationScreen(
    state: MobileIntegrationState,
    captureController: CaptureController,
    onContinue: () -> Unit,
    onEnded: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    BroadcastPage(
        title = "End Broadcast?",
        subtitle = "The RTMP publisher and backend broadcast session will both be closed.",
        onBack = onContinue,
    ) {
        BroadcastCard(
            "Current stream",
            captureController.snapshot.publishTarget.ifBlank { "Live broadcast" },
            status = captureController.snapshot.publishStatus.name,
            statusColor = AppLive,
        )
        state.error?.let { BroadcastCard("Backend", it, "CHECK", AppWarning) }
        UlPrimaryButton("Continue Streaming", onClick = onContinue)
        Button(
            onClick = {
                scope.launch {
                    captureController.stop()
                    state.endBroadcast()
                    onEnded()
                }
            },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
        ) { Text("End Stream", fontWeight = FontWeight.Bold) }
    }
}

private fun prepareConnectedCapture(
    publishConfig: PublishConfig,
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    overlayState: OverlayState,
    sceneState: SceneState,
) {
    val config = streamState.config
    captureController.setCaptureMode(CaptureMode.ENTIRE_DEVICE)
    captureController.configureAudio(config.microphoneEnabled, config.internalAudioEnabled)
    captureController.configureVideo(
        width = config.resolution.width,
        height = config.resolution.height,
        fps = config.fps.value,
        bitrateKbps = config.bitrateKbps,
        orientation = config.orientation.label,
    )
    captureController.configurePublish(
        serverUrl = publishConfig.serverUrl,
        streamKey = publishConfig.streamKey,
        targetName = publishConfig.displayName,
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
            listOf(layer.kind.name, layer.x, layer.y, layer.width, layer.opacity, layer.text, layer.assetPath)
                .joinToString("¦") { it.toString().replace("§", " ").replace("¦", " ") }
        }
    captureController.configureOverlays(sceneState.activeScene.name, payload)
}
