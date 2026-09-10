package com.universallive.app.features.golive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.AppScaffold
import com.universallive.app.navigation.AppDestination
import com.universallive.app.streaming.model.StreamConfigLimits
import com.universallive.app.streaming.model.StreamFps
import com.universallive.app.streaming.model.StreamOrientation
import com.universallive.app.streaming.model.StreamResolution
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.streaming.connections.RtmpProfilesState
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.capture.CaptureStatus
import com.universallive.app.streaming.capture.CaptureMode
import com.universallive.app.streaming.capture.PublishStatus
import com.universallive.app.theme.*
import com.universallive.app.streaming.facecam.FacecamState
import com.universallive.app.streaming.overlays.OverlayState
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.streaming.preview.LiveOutputPreview

@Composable
fun GoLiveScreen(
    streamState: StreamConfigState,
    profilesState: RtmpProfilesState,
    captureController: CaptureController,
    facecamState: FacecamState,
    overlayState: OverlayState,
    sceneState: SceneState,
    onDestinationChanged: (AppDestination) -> Unit,
) {
    val config = streamState.config

    AppScaffold(
        title = "Go Live",
        selected = AppDestination.GoLive,
        onDestinationChanged = onDestinationChanged,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StreamSummaryCard(
                video = config.videoSummary,
                bitrate = config.bitrateLabel,
                orientation = config.orientation.label,
            )

            if (captureController.snapshot.isActive || captureController.snapshot.publishStatus != PublishStatus.IDLE) {
                LiveOutputMonitor(captureController)
            }

            OutlinedButton(
                onClick = { onDestinationChanged(AppDestination.Settings) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    if (profilesState.activeProfiles.isEmpty()) "SETUP STREAM CONNECTION IN SETTINGS" else "${profilesState.activeProfiles.size} DESTINATION READY • MANAGE IN SETTINGS",
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Capture source", color = AppText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CaptureMode.entries.forEach { mode ->
                        SelectChip(
                            label = mode.label,
                            selected = captureController.requestedCaptureMode == mode,
                            onClick = { captureController.setCaptureMode(mode) },
                        )
                    }
                }
                Text(
                    if (captureController.requestedCaptureMode == CaptureMode.ENTIRE_DEVICE)
                        "Broadcast the whole device display. On Android 14+ the system may still show a privacy choice before capture starts."
                    else "Choose a single app or the whole screen in Android's capture dialog.",
                    color = AppTextMuted, fontSize = 11.sp, lineHeight = 16.sp,
                )
            }

            SettingSection("Resolution") {
                StreamResolution.entries.forEach { resolution ->
                    SelectChip(
                        label = resolution.label,
                        selected = config.resolution == resolution,
                        onClick = { streamState.setResolution(resolution) },
                    )
                }
            }

            SettingSection("Frame rate") {
                StreamFps.entries.forEach { fps ->
                    SelectChip(
                        label = "${fps.value} FPS",
                        selected = config.fps == fps,
                        onClick = { streamState.setFps(fps) },
                    )
                }
            }

            SettingSection("Bitrate") {
                StreamConfigLimits.bitrateOptionsKbps.forEach { bitrate ->
                    val label = if (bitrate % 1000 == 0) "${bitrate / 1000} Mbps" else "${bitrate} Kbps"
                    SelectChip(
                        label = label,
                        selected = config.bitrateKbps == bitrate,
                        onClick = { streamState.setBitrateKbps(bitrate) },
                    )
                }
            }

            SettingSection("Orientation") {
                StreamOrientation.entries.forEach { orientation ->
                    SelectChip(
                        label = orientation.label,
                        selected = config.orientation == orientation,
                        onClick = { streamState.setOrientation(orientation) },
                    )
                }
            }

            AudioToggle(
                title = "Microphone",
                subtitle = "Include your voice in the live stream",
                checked = config.microphoneEnabled,
                onCheckedChange = streamState::setMicrophoneEnabled,
            )
            AudioToggle(
                title = "Internal / game audio",
                subtitle = "Capture supported app and game playback audio",
                checked = config.internalAudioEnabled,
                onCheckedChange = streamState::setInternalAudioEnabled,
            )

            FacecamControls(facecamState)

            CaptureStatusCard(captureController)

            StreamHealthCard(captureController)
            AudioToggle(
                title = "Adaptive bitrate",
                subtitle = "Allows runtime encoder bitrate changes when network health degrades",
                checked = captureController.adaptiveBitrateEnabled,
                onCheckedChange = captureController::setAdaptiveBitrateEnabled,
            )

            Spacer(Modifier.height(2.dp))
            Button(
                onClick = {
                    if (captureController.snapshot.isActive) {
                        captureController.stop()
                    } else {
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
                        val activeProfile = profilesState.activeProfiles.firstOrNull()
                        captureController.configurePublish(
                            serverUrl = activeProfile?.serverUrl.orEmpty(),
                            streamKey = activeProfile?.streamKey.orEmpty(),
                            targetName = activeProfile?.name.orEmpty(),
                        )
                        captureController.configureFacecam(
                            enabled = facecamState.config.enabled,
                            lens = facecamState.config.lens.label,
                            shape = facecamState.config.shape.label,
                            x = facecamState.config.x, y = facecamState.config.y,
                            size = facecamState.config.size, mirrored = facecamState.config.mirrored,
                        )
                        val activeIds = sceneState.activeScene.layerIds.toSet()
                        val payload = overlayState.layers.filter { it.id in activeIds && it.enabled }.joinToString("§") { layer ->
                            listOf(layer.kind.name, layer.x, layer.y, layer.width, layer.opacity, layer.text, layer.assetPath)
                                .joinToString("¦") { it.toString().replace("§", " ").replace("¦", " ") }
                        }
                        captureController.configureOverlays(sceneState.activeScene.name, payload)
                        captureController.start()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = AppText, disabledContentColor = AppText.copy(alpha = .70f)),
            ) {
                Text(
                    if (captureController.snapshot.isActive) "STOP SCREEN CAPTURE" else "START SCREEN CAPTURE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                "Phase 12–14 add live scene switching, stream-health telemetry, runtime bitrate control, stronger reconnect policy and background-safe recovery foundations.",
                color = AppTextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}


@Composable
private fun LiveOutputMonitor(controller: CaptureController) {
    val snap = controller.snapshot
    val stage = when {
        snap.status == CaptureStatus.REQUESTING_PERMISSION -> "1/5 PERMISSION"
        snap.status == CaptureStatus.STARTING -> "2/5 STARTING ENGINE"
        snap.status == CaptureStatus.CAPTURING && snap.publishStatus == PublishStatus.CONNECTING -> "3/5 CONNECTING RTMP"
        snap.publishStatus == PublishStatus.RECONNECTING -> "RECOVERING CONNECTION"
        snap.publishStatus == PublishStatus.LIVE -> "5/5 LIVE"
        snap.status == CaptureStatus.CAPTURING -> "4/5 CAPTURE READY"
        snap.status == CaptureStatus.ERROR || snap.publishStatus == PublishStatus.ERROR -> "ERROR"
        else -> "IDLE"
    }
    Column(
        Modifier.fillMaxWidth().background(AppSurface, RoundedCornerShape(20.dp))
            .border(1.dp, if (snap.publishStatus == PublishStatus.LIVE) AppLive else AppBorder, RoundedCornerShape(20.dp)).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stage, color = if (snap.publishStatus == PublishStatus.LIVE) AppLive else AppPrimarySoft, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(snap.publishTarget.ifBlank { "LOCAL PREVIEW" }, color = AppTextMuted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(10.dp))
        LiveOutputPreview(
            Modifier.fillMaxWidth().aspectRatio(
                if (snap.encoderWidth > 0 && snap.encoderHeight > 0) snap.encoderWidth.toFloat() / snap.encoderHeight else 16f / 9f
            ).background(AppBackground, RoundedCornerShape(14.dp)).border(1.dp, AppBorder, RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.height(10.dp))
        Text(snap.message, color = AppText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(snap.publishMessage, color = AppTextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonitorPill("VIDEO", if (snap.compositorActive) "COMPOSITED" else "STARTING", Modifier.weight(1f))
            MonitorPill("FACECAM", if (snap.facecamActive) "ACTIVE" else if (controller.requestedFacecamEnabled) "WAITING" else "OFF", Modifier.weight(1f))
            MonitorPill("SCENE", snap.activeSceneName, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MonitorPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(AppSurfaceRaised, RoundedCornerShape(11.dp)).padding(9.dp)) {
        Text(label, color = AppTextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = AppText, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun StreamHealthCard(controller: CaptureController) {
    val snap = controller.snapshot
    val target = snap.encoderBitrateKbps.coerceAtLeast(1)
    val actual = (snap.networkBitrateBps / 1000L).toInt()
    val ratio = if (target > 0) actual.toFloat() / target else 0f
    val label = when {
        !snap.isPublishing -> "OFFLINE"
        ratio >= .80f -> "EXCELLENT"
        ratio >= .60f -> "GOOD"
        ratio >= .35f -> "DEGRADED"
        else -> "POOR"
    }
    Column(Modifier.fillMaxWidth().background(AppSurface, RoundedCornerShape(18.dp)).border(1.dp, AppBorder, RoundedCornerShape(18.dp)).padding(16.dp)) {
        Text("STREAM HEALTH", color=AppPrimarySoft, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(label, color=if(label=="POOR") AppLive else AppText, fontWeight=FontWeight.Bold)
        Text("Network ${actual} Kbps • target ${target} Kbps", color=AppTextMuted, fontSize=12.sp)
        if (snap.publishStatus == PublishStatus.RECONNECTING) Text("Connection recovery in progress", color=AppTextMuted, fontSize=12.sp)
    }
}

@Composable
private fun StreamSummaryCard(video: String, bitrate: String, orientation: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(22.dp))
            .border(BorderStroke(1.dp, AppBorder), RoundedCornerShape(22.dp))
            .padding(18.dp),
    ) {
        Text("STREAM OUTPUT", color = AppPrimarySoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text(video, color = AppText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("$bitrate • $orientation", color = AppTextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable FlowRowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = AppText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) AppSurfaceRaised else AppSurface, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, if (selected) AppPrimary else AppBorder), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
    ) {
        Text(label, color = if (selected) AppPrimarySoft else AppTextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AudioToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, AppBorder), RoundedCornerShape(16.dp))
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = AppText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = AppTextMuted, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}


@Composable
private fun CaptureStatusCard(controller: CaptureController) {
    val snapshot = controller.snapshot
    val statusLabel = when (snapshot.status) {
        CaptureStatus.IDLE -> "IDLE"
        CaptureStatus.REQUESTING_PERMISSION -> "PERMISSION"
        CaptureStatus.STARTING -> "STARTING"
        CaptureStatus.CAPTURING -> "CAPTURING"
        CaptureStatus.STOPPING -> "STOPPING"
        CaptureStatus.ERROR -> "ERROR"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, AppBorder), RoundedCornerShape(16.dp))
            .padding(15.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SCREEN CAPTURE", color = AppText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(statusLabel, color = if (snapshot.status == CaptureStatus.CAPTURING) AppPrimarySoft else AppTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(snapshot.message, color = AppTextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(3.dp))
        Text("Source: ${snapshot.captureMode}", color = AppTextMuted, fontSize = 10.sp)
        if (snapshot.encodedVideoFrames > 0 || snapshot.encoderName.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("H.264 ENCODER", color = AppText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            if (snapshot.encoderName.isNotBlank()) {
                Text(snapshot.encoderName, color = AppPrimarySoft, fontSize = 11.sp)
            }
            Text(
                "${snapshot.encoderWidth}×${snapshot.encoderHeight} • ${snapshot.encoderFps} FPS • ${snapshot.encoderBitrateKbps} Kbps",
                color = AppTextMuted,
                fontSize = 11.sp,
            )
            Text(
                "Encoded frames: ${snapshot.encodedVideoFrames} • ${snapshot.encodedVideoBytes} bytes",
                color = AppTextMuted,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = AppBorder)
        Spacer(Modifier.height(9.dp))
        Text("AUDIO CAPTURE", color = AppText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(snapshot.audioMessage, color = AppTextMuted, fontSize = 11.sp)
        if (snapshot.microphoneRequested) {
            Spacer(Modifier.height(5.dp))
            Text(
                "Mic: ${if (snapshot.microphoneActive) "ACTIVE" else "WAITING"} • ${snapshot.microphoneBytes} bytes • level ${(snapshot.microphoneLevel * 100).toInt()}%",
                color = if (snapshot.microphoneActive) AppPrimarySoft else AppTextMuted,
                fontSize = 11.sp,
            )
        }
        if (snapshot.internalAudioRequested) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Game audio: ${if (snapshot.internalAudioActive) "ACTIVE" else "WAITING"} • ${snapshot.internalAudioBytes} bytes • level ${(snapshot.internalAudioLevel * 100).toInt()}%",
                color = if (snapshot.internalAudioActive) AppPrimarySoft else AppTextMuted,
                fontSize = 11.sp,
            )
        }
        if (snapshot.audioEncoderName.isNotBlank() || snapshot.encodedAudioFrames > 0) {
            Spacer(Modifier.height(6.dp))
            Text("AAC AUDIO", color = AppText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (snapshot.audioEncoderName.isNotBlank()) {
                Text(snapshot.audioEncoderName, color = AppPrimarySoft, fontSize = 11.sp)
            }
            Text(
                "48 kHz stereo • AAC-LC 160 Kbps • encoded ${snapshot.encodedAudioFrames} frames / ${snapshot.encodedAudioBytes} bytes",
                color = AppTextMuted,
                fontSize = 11.sp,
            )
            if (snapshot.publishedAudioFrames > 0) {
                Text(
                    "Published audio: ${snapshot.publishedAudioFrames} frames • ${snapshot.publishedAudioBytes} bytes",
                    color = AppTextMuted,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = AppBorder)
        Spacer(Modifier.height(9.dp))
        Text("RTMP PUBLISH", color = AppText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        val publishLabel = when (snapshot.publishStatus) {
            PublishStatus.IDLE -> "IDLE"
            PublishStatus.CONNECTING -> "CONNECTING"
            PublishStatus.LIVE -> "LIVE"
            PublishStatus.RECONNECTING -> "RECONNECTING"
            PublishStatus.ERROR -> "ERROR"
            PublishStatus.DISCONNECTED -> "DISCONNECTED"
        }
        Text(
            if (snapshot.publishTarget.isBlank()) publishLabel else "$publishLabel • ${snapshot.publishTarget}",
            color = if (snapshot.publishStatus == PublishStatus.LIVE) AppPrimarySoft else AppTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(snapshot.publishMessage, color = AppTextMuted, fontSize = 11.sp)
        if (snapshot.publishStatus == PublishStatus.LIVE) {
            Text(
                if (snapshot.publishedVideoFrames > 0) "Ingest is receiving decodable video packets." else "Ingest connected; waiting for the first keyframe.",
                color = if (snapshot.publishedVideoFrames > 0) AppSuccess else AppTextMuted,
                fontSize = 10.sp,
            )
        }
        if (snapshot.publishedVideoFrames > 0 || snapshot.networkBitrateBps > 0) {
            Text(
                "Published frames: ${snapshot.publishedVideoFrames} • ${snapshot.publishedVideoBytes} bytes",
                color = AppTextMuted,
                fontSize = 11.sp,
            )
            Text(
                "Network bitrate: ${snapshot.networkBitrateBps / 1000} Kbps",
                color = AppTextMuted,
                fontSize = 11.sp,
            )
        }
    }
}
