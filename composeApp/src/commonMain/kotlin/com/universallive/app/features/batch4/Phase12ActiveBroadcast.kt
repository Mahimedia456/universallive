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
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.integration.DeviceStreamTelemetry
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.capture.PublishStatus
import com.universallive.app.streaming.facecam.FacecamState
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.streaming.preview.LiveOutputPreview
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.*
import kotlinx.coroutines.delay

@Composable
fun LiveBroadcastV2Screen(
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    sceneState: SceneState,
    integrationState: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    val snap = captureController.snapshot
    val config = streamState.config
    val live = snap.publishStatus == PublishStatus.LIVE
    val bitrateKbps = (snap.networkBitrateBps / 1000L).coerceAtLeast(0L)

    LaunchedEffect(integrationState.activeBroadcast?.id, snap.publishStatus, snap.status) {
        if (integrationState.activeBroadcast != null &&
            (snap.isActive || snap.isPublishing)
        ) {
            while (true) {
                integrationState.heartbeat(
                    DeviceStreamTelemetry(
                        connectionId = integrationState.selectedLiveConnectionId,
                        bitrateKbps = bitrateKbps.toInt().takeIf { it > 0 },
                        targetBitrateKbps = config.bitrateKbps,
                        encoderBitrateKbps = snap.encoderMeasuredBitrateKbps.takeIf { it > 0 },
                        rtmpUploadKbps = bitrateKbps.toInt().takeIf { it > 0 },
                        fps = snap.encodedFpsActual.takeIf { it > 0.0 }
                            ?: snap.encoderFps.toDouble().takeIf { it > 0.0 }
                            ?: config.fps.value.toDouble(),
                        encodedFps = snap.encodedFpsActual.takeIf { it > 0.0 },
                        sentFps = snap.sentFpsActual.takeIf { it > 0.0 },
                        droppedFrames = snap.droppedFrames,
                        publishedVideoFrames = snap.publishedVideoFrames,
                        publishedAudioFrames = snap.publishedAudioFrames,
                        encoderWidth = snap.encoderWidth.takeIf { it > 0 },
                        encoderHeight = snap.encoderHeight.takeIf { it > 0 },
                        encoderName = snap.encoderName.takeIf { it.isNotBlank() },
                        networkStatus = snap.publishStatus.name.lowercase(),
                        publishStatus = snap.publishStatus.name.lowercase(),
                        audioStatus = snap.audioMessage,
                        rtmpQueueDepth = snap.rtmpQueueDepth,
                        socketWriteLatencyMs = snap.socketWriteLatencyMs,
                        publisherEnqueueLatencyMs = snap.publisherEnqueueLatencyMs,
                        lastVideoPacketAgeMs = snap.lastVideoPacketAgeMs,
                        lastAudioPacketAgeMs = snap.lastAudioPacketAgeMs,
                        keyframeIntervalMs = snap.keyframeIntervalMs,
                        videoPtsMonotonic = snap.videoPtsMonotonic,
                        audioPtsMonotonic = snap.audioPtsMonotonic,
                        reconnectCount = snap.reconnectCount,
                        publisherInstanceId = snap.publisherInstanceId.takeIf { it.isNotBlank() },
                    )
                )
                delay(15_000)
            }
        }
    }

    AppScaffold(
        title = "Live",
        selected = AppDestination.GoLive,
        onDestinationChanged = onDestination,
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppLive.copy(alpha = .10f))
                    .border(1.dp, AppLive.copy(alpha = .55f), RoundedCornerShape(16.dp))
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AppLive),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (live) "LIVE" else snap.publishStatus.name,
                    color = AppLive,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (bitrateKbps > 0) "${bitrateKbps} Kbps" else config.bitrateLabel,
                    color = AppText,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(
                        if (snap.encoderWidth > 0 && snap.encoderHeight > 0) {
                            snap.encoderWidth.toFloat() / snap.encoderHeight.toFloat()
                        } else {
                            16f / 9f
                        },
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppBackgroundSecondary)
                    .border(1.dp, AppPrimary.copy(alpha = .55f), RoundedCornerShape(20.dp)),
            ) {
                if (snap.compositorActive) {
                    LiveOutputPreview(Modifier.fillMaxSize())
                } else {
                    Column(
                        Modifier.align(Alignment.Center).padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "DIRECT SCREEN CAPTURE",
                            color = AppPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Android is sending the device screen directly to the H.264 encoder.",
                            color = AppTextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }

                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(AppLive.copy(alpha = .9f))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "LIVE",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    Triple("Mic", config.microphoneEnabled, AppRoute.AudioMixer),
                    Triple("Audio", config.internalAudioEnabled, AppRoute.AudioMixer),
                    Triple("Face Cam", facecamState.config.enabled, AppRoute.LiveControls),
                    Triple("Scenes", true, AppRoute.LiveSceneSwitcher),
                    Triple("Chat", true, AppRoute.LiveChat),
                ).forEach { item ->
                    OutlinedButton(
                        onClick = { onRoute(item.third) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (item.second) AppPrimary else AppBorder,
                        ),
                    ) {
                        Text(
                            item.first,
                            color = if (item.second) AppPrimary else AppTextMuted,
                            fontSize = 9.sp,
                        )
                    }
                }
            }

            BroadcastCard(
                "Destinations",
                snap.publishTarget.ifBlank { "Configured RTMP destination" },
                status = if (live) "LIVE" else snap.publishStatus.name,
                statusColor = if (live) AppSuccess else AppWarning,
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricTile(
                    "Bitrate",
                    if (bitrateKbps > 0) "${bitrateKbps}K" else config.bitrateLabel,
                    Modifier.weight(1f),
                )
                MetricTile(
                    "FPS",
                    (snap.encoderFps.takeIf { it > 0 } ?: config.fps.value).toString(),
                    Modifier.weight(1f),
                )
                MetricTile(
                    "Frames",
                    snap.publishedVideoFrames.toString(),
                    Modifier.weight(1f),
                )
            }

            UlSecondaryButton(
                "View Live Statistics",
                onClick = { onRoute(AppRoute.LiveStats) },
            )

            Button(
                onClick = { onRoute(AppRoute.EndStreamConfirmation) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = androidx.compose.ui.graphics.Color.White, disabledContentColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    "End Stream",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun LiveControlsScreen(
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val config = streamState.config
    var adaptive by remember { mutableStateOf(captureController.adaptiveBitrateEnabled) }

    BroadcastPage(
        title = "Live Controls",
        subtitle = "Safe controls available during broadcast.",
        onBack = onBack,
    ) {
        BroadcastCard("Audio") {
            ToggleRow(
                "Microphone",
                "Updates the desired microphone state.",
                config.microphoneEnabled,
            ) {
                streamState.setMicrophoneEnabled(it)
                captureController.configureAudio(
                    it,
                    streamState.config.internalAudioEnabled,
                )
            }
            ToggleRow(
                "Device audio",
                "Supported game/media playback.",
                config.internalAudioEnabled,
            ) {
                streamState.setInternalAudioEnabled(it)
                captureController.configureAudio(
                    streamState.config.microphoneEnabled,
                    it,
                )
            }
        }

        BroadcastCard("Face Camera") {
            ToggleRow(
                "Face Camera",
                "Show or hide the configured camera source.",
                facecamState.config.enabled,
            ) { facecamState.setEnabled(it) }
        }

        BroadcastCard("Runtime") {
            ToggleRow(
                "Adaptive bitrate",
                "Allow runtime encoder bitrate changes.",
                adaptive,
            ) {
                adaptive = it
                captureController.setAdaptiveBitrateEnabled(it)
            }
            TextButton(onClick = { onRoute(AppRoute.LiveStats) }) {
                Text("Open stream statistics", color = AppPrimary)
            }
            TextButton(onClick = { onRoute(AppRoute.LiveRecovery) }) {
                Text("Open live recovery", color = AppWarning)
            }
        }

        BroadcastCard(
            "Locked while live",
            "Resolution and encoder mode remain locked to protect the current stream session.",
        )
    }
}

@Composable
fun LiveSceneSwitcherScreen(
    sceneState: SceneState,
    captureController: CaptureController,
    onBack: () -> Unit,
) {
    BroadcastPage(
        title = "Switch Scene",
        subtitle = "Tap a scene to transition safely.",
        onBack = onBack,
    ) {
        sceneState.scenes.forEach { scene ->
            val selected = scene.id == sceneState.activeSceneId
            BroadcastCard(
                title = scene.name,
                subtitle = "${scene.layerIds.size} layer(s)",
                status = if (selected) "LIVE" else null,
                statusColor = if (selected) AppPrimary else AppTextMuted,
                onClick = {
                    sceneState.activate(scene.id)
                    captureController.updateSceneLive(
                        sceneName = scene.name,
                        payload = "",
                    )
                },
            )
        }

        Text(
            "A selected scene is immediately forwarded to the active compositor when supported.",
            color = AppTextMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
fun LiveChatScreen(onBack: () -> Unit) {
    var message by remember { mutableStateOf("") }

    BroadcastPage(
        title = "Live Chat",
        subtitle = "Destination-aware conversation.",
        onBack = onBack,
    ) {
        BroadcastCard("YouTube", "Live chat connected", "LIVE") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("alex_live  Great quality!", color = AppText)
                Text("maria_streams  Nice setup.", color = AppTextSecondary)
                Text("moderator  Welcome to the stream.", color = AppPrimary)
            }
        }

        UlTextField(
            value = message,
            onValueChange = { message = it },
            label = "Message",
            placeholder = "Chat posting will use platform API when connected",
        )

        UlPrimaryButton(
            "Send",
            onClick = { message = "" },
            enabled = message.isNotBlank(),
        )

        Text(
            "Platform chat APIs are wired during backend integration. This screen already defines the final mobile UX.",
            color = AppTextMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
fun LiveStatsV2Screen(
    streamState: StreamConfigState,
    captureController: CaptureController,
    onBack: () -> Unit,
) {
    val snap = captureController.snapshot
    val config = streamState.config
    val networkKbps = snap.networkBitrateBps / 1000L
    val encodedMb = snap.encodedVideoBytes / (1024f * 1024f)

    BroadcastPage(
        title = "Live Statistics",
        subtitle = "Current encoder and destination health.",
        onBack = onBack,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricTile(
                "Current Bitrate",
                if (networkKbps > 0) "${networkKbps} Kbps" else "Waiting",
                Modifier.weight(1f),
                if (networkKbps > 0) AppPrimary else AppTextMuted,
            )
            MetricTile(
                "Target",
                config.bitrateLabel,
                Modifier.weight(1f),
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricTile(
                "FPS",
                (snap.encoderFps.takeIf { it > 0 } ?: config.fps.value).toString(),
                Modifier.weight(1f),
            )
            MetricTile(
                "Video Frames",
                snap.publishedVideoFrames.toString(),
                Modifier.weight(1f),
            )
        }

        BroadcastCard("Encoder") {
            Text(
                snap.encoderName.ifBlank { "Waiting for hardware encoder" },
                color = AppText,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${snap.encoderWidth} × ${snap.encoderHeight}",
                color = AppTextSecondary,
                fontSize = 12.sp,
            )
            Text(
                "Encoded: ${"%.1f".format(encodedMb)} MB",
                color = AppTextSecondary,
                fontSize = 12.sp,
            )
        }

        BroadcastCard(
            "Network",
            snap.publishMessage,
            status = snap.publishStatus.name,
            statusColor = when (snap.publishStatus) {
                PublishStatus.LIVE -> AppSuccess
                PublishStatus.RECONNECTING -> AppWarning
                PublishStatus.ERROR -> AppLive
                else -> AppPrimary
            },
        )

        BroadcastCard("Audio") {
            Text(
                snap.audioMessage,
                color = AppTextSecondary,
                fontSize = 12.sp,
            )
            Text(
                "Audio frames: ${snap.publishedAudioFrames}",
                color = AppTextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
fun EndStreamConfirmationScreen(
    captureController: CaptureController,
    onContinue: () -> Unit,
    onEnded: () -> Unit,
) {
    BroadcastPage(
        title = "End Broadcast?",
        subtitle = "Your active destination will stop receiving the stream.",
        onBack = onContinue,
    ) {
        BroadcastCard(
            "Current stream",
            captureController.snapshot.publishTarget.ifBlank { "Live broadcast" },
            status = captureController.snapshot.publishStatus.name,
            statusColor = AppLive,
        )

        UlPrimaryButton(
            "Continue Streaming",
            onClick = onContinue,
        )

        Button(
            onClick = {
                captureController.stop()
                onEnded()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = androidx.compose.ui.graphics.Color.White, disabledContentColor = androidx.compose.ui.graphics.Color.White),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("End Stream", fontWeight = FontWeight.Bold)
        }
    }
}
