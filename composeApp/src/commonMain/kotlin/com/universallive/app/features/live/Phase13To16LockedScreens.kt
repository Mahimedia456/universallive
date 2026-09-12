package com.universallive.app.features.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.UlPrimaryButton
import com.universallive.app.components.UlSecondaryButton
import com.universallive.app.components.UlTextField
import com.universallive.app.components.UniversalLiveBrand
import com.universallive.app.integration.DeviceStreamTelemetry
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.integration.PublishConfig
import com.universallive.app.integration.StreamingConnection
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.permissions.PermissionGrantState
import com.universallive.app.permissions.PermissionSetupController
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.capture.CaptureMode
import com.universallive.app.streaming.capture.VideoSourceMode
import com.universallive.app.streaming.capture.PublishStatus
import com.universallive.app.streaming.facecam.FacecamState
import com.universallive.app.streaming.model.StreamFps
import com.universallive.app.streaming.model.StreamOrientation
import com.universallive.app.streaming.model.StreamResolution
import com.universallive.app.streaming.overlays.OverlayState
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.streaming.preview.LiveOutputPreview
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.AppBackground
import com.universallive.app.theme.AppBackgroundSecondary
import com.universallive.app.theme.AppBorder
import com.universallive.app.theme.AppLive
import com.universallive.app.theme.AppPrimary
import com.universallive.app.theme.AppSuccess
import com.universallive.app.theme.AppSurface
import com.universallive.app.theme.AppSurfaceInteractive
import com.universallive.app.theme.AppSurfaceRaised
import com.universallive.app.theme.AppText
import com.universallive.app.theme.AppTextMuted
import com.universallive.app.theme.AppTextSecondary
import com.universallive.app.theme.AppWarning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Locked UI implementation for the approved Phase 13-16 storyboard.
 *
 * Phase 13: Go Live Setup
 * Phase 14: Stream Preflight
 * Phase 15: Active Live
 * Phase 16: Disconnect / Recovery
 *
 * Important backend boundary:
 * The current Android native publisher has one direct RTMP output. The UI therefore
 * selects one real publish destination instead of pretending that additional platforms
 * are already receiving the same native stream. Multi-platform relay/OAuth can plug into
 * this boundary later without redesigning these screens.
 */

private val PhaseSurface = Color(0xFF071116)
private val PhaseSurfaceRaised = Color(0xFF0A1820)
private val FacebookBlue = Color(0xFF1877F2)
private val YouTubeRed = Color(0xFFFF2035)
private val TwitchPurple = Color(0xFF9146FF)
private val TikTokCyan = Color(0xFF25F4EE)

@Composable
private fun LockedLivePage(
    phase: String,
    title: String,
    subtitle: String,
    step: Int? = null,
    totalSteps: Int? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = PhaseSurface,
                    border = BorderStroke(1.dp, AppBorder),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("‹", color = AppText, fontSize = 28.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
            }
            UniversalLiveBrand(compact = true, showTagline = false)
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    phase.uppercase(),
                    color = AppPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.8.sp,
                )
                Spacer(Modifier.weight(1f))
                if (step != null && totalSteps != null) {
                    Text("$step of $totalSteps", color = AppTextMuted, fontSize = 11.sp)
                }
            }
            if (step != null && totalSteps != null) {
                Spacer(Modifier.height(8.dp))
                PhaseStepProgress(step, totalSteps)
            }
            Spacer(Modifier.height(14.dp))
            Text(title, color = AppText, fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(subtitle, color = AppTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PhaseStepProgress(step: Int, total: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(total) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (index < step) AppPrimary else AppSurfaceInteractive),
            )
        }
    }
}

@Composable
private fun LockedCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(17.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) AppPrimary.copy(alpha = .07f) else PhaseSurface)
            .border(1.dp, if (selected) AppPrimary.copy(alpha = .78f) else AppBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(15.dp),
        content = content,
    )
}

@Composable
private fun PlatformBadge(platform: String, size: Int = 44) {
    val key = platform.lowercase()
    val background = when (key) {
        "youtube" -> YouTubeRed
        "facebook" -> FacebookBlue
        "twitch" -> TwitchPurple
        "tiktok" -> Color(0xFF071014)
        else -> Color(0xFF1A2A33)
    }
    val foreground = when (key) {
        "tiktok" -> TikTokCyan
        else -> Color.White
    }
    val glyph = when (key) {
        "youtube" -> "▶"
        "facebook" -> "f"
        "twitch" -> "T"
        "tiktok" -> "♪"
        else -> "RTMP"
    }
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(background)
            .border(1.dp, Color.White.copy(alpha = .09f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            color = foreground,
            fontSize = if (key == "custom_rtmp") 8.sp else (size / 2.35f).sp,
            fontWeight = FontWeight.Black,
        )
    }
}

private fun platformLabel(platform: String): String = when (platform.lowercase()) {
    "youtube" -> "YouTube"
    "facebook" -> "Facebook"
    "twitch" -> "Twitch"
    "tiktok" -> "TikTok"
    else -> "Custom RTMP"
}

@Composable
private fun DestinationRow(
    connection: StreamingConnection,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    LockedCard(selected = selected, onClick = onSelect) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformBadge(connection.platform)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(connection.displayName, color = AppText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${platformLabel(connection.platform)} • ${if (connection.readyToPublish) "Connected" else "Setup required"}",
                    color = if (connection.readyToPublish) AppSuccess else AppWarning,
                    fontSize = 11.sp,
                )
            }
            Switch(
                checked = selected,
                onCheckedChange = { onSelect() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppBackground,
                    checkedTrackColor = AppPrimary,
                ),
            )
        }
    }
}

@Composable
private fun SetupValueRow(
    label: String,
    value: String,
    statusColor: Color = AppPrimary,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(PhaseSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(15.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(statusColor))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = AppTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(value, color = AppText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        if (onClick != null) Text("›", color = AppPrimary, fontSize = 22.sp)
    }
}

@Composable
private fun CheckRow(
    label: String,
    detail: String,
    ok: Boolean,
    warning: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PhaseSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = when {
            ok -> AppSuccess
            warning -> AppWarning
            else -> AppTextMuted
        }
        Box(
            Modifier.size(27.dp).clip(CircleShape).background(color.copy(alpha = .14f)).border(1.dp, color.copy(alpha = .35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (ok) "✓" else if (warning) "!" else "•", color = color, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = AppText, fontWeight = FontWeight.SemiBold)
            Text(detail, color = AppTextMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    LockedCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = AppText, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = AppTextMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(checkedThumbColor = AppBackground, checkedTrackColor = AppPrimary),
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Phase 13 - Go Live Setup
// -----------------------------------------------------------------------------

@Composable
fun Phase13GoLiveOverviewScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    sceneState: SceneState,
    facecamState: FacecamState,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    LaunchedEffect(Unit) {
        state.refreshConnections()
        if (state.membership == null) state.refreshAccount()
    }
    val selected = state.selectedLiveConnectionId?.let { id -> state.connections.firstOrNull { it.id == id } }
    val cfg = streamState.config

    LockedLivePage(
        phase = "GO LIVE SETUP",
        title = "Go Live Setup",
        subtitle = "Configure your stream and get ready to go live.",
        step = 1,
        totalSteps = 6,
    ) {
        SetupValueRow(
            "Selected destination",
            selected?.displayName ?: "Choose a streaming destination",
            if (selected?.readyToPublish == true) AppSuccess else AppWarning,
        ) { onRoute(AppRoute.DestinationSelection) }
        Spacer(Modifier.height(10.dp))
        SetupValueRow("Stream title", state.liveTitle.ifBlank { "Add your stream title" }) { onRoute(AppRoute.StreamInfo) }
        Spacer(Modifier.height(10.dp))
        SetupValueRow("Category", state.liveCategory) { onRoute(AppRoute.StreamInfo) }
        Spacer(Modifier.height(10.dp))
        SetupValueRow("Privacy", state.livePrivacy) { onRoute(AppRoute.StreamInfo) }
        Spacer(Modifier.height(10.dp))
        SetupValueRow("Current scene", sceneState.activeScene.name) { onDestination(AppDestination.Scenes) }
        Spacer(Modifier.height(10.dp))
        SetupValueRow("Stream settings", "${cfg.resolution.label} • ${cfg.fps.value} FPS • ${cfg.bitrateKbps} Kbps") { onRoute(AppRoute.StreamQuality) }
        Spacer(Modifier.height(10.dp))
        SetupValueRow(
            "Audio & camera",
            "Mic ${if (cfg.microphoneEnabled) "ON" else "OFF"} • Game audio ${if (cfg.internalAudioEnabled) "ON" else "OFF"} • Facecam ${if (facecamState.config.enabled) "ON" else "OFF"}",
        ) { onRoute(AppRoute.AudioCameraSetup) }

        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Continue", onClick = { onRoute(AppRoute.DestinationSelection) })
    }
}

@Composable
fun Phase13DestinationSelectionScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { state.refreshConnections() }
    val ready = state.connections.filter { it.isEnabled && it.readyToPublish }

    LockedLivePage(
        phase = "GO LIVE SETUP",
        title = "Select Destinations",
        subtitle = "Choose one or more connected platforms. Your plan limit is enforced before preflight.",
        step = 2,
        totalSteps = 6,
        onBack = onBack,
    ) {
        if (ready.isEmpty()) {
            LockedCard {
                Text("No ready destination", color = AppText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Connect YouTube, Facebook, Twitch, TikTok or a Custom RTMP destination first.", color = AppTextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            UlPrimaryButton("Add Destination", onClick = { onRoute(AppRoute.AddConnection) })
        } else {
            ready.forEachIndexed { index, connection ->
                DestinationRow(
                    connection = connection,
                    selected = connection.id in state.selectedLiveConnectionIds,
                    onSelect = { state.toggleLiveConnection(connection.id) },
                )
                if (index != ready.lastIndex) Spacer(Modifier.height(9.dp))
            }

            Spacer(Modifier.height(14.dp))
            LockedCard {
                val maxDestinations = (state.membership?.maxSimultaneousDestinations ?: 1).coerceAtLeast(1)
                Text("Multi-destination live", color = AppPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    "${state.selectedLiveConnectionIds.size} selected • plan limit $maxDestinations. Universal Live publishes the same encoded program to each validated RTMP destination.",
                    color = AppTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
            state.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = AppWarning, fontSize = 11.sp) }
            Spacer(Modifier.height(16.dp))
            UlPrimaryButton(
                "Next",
                onClick = { onRoute(AppRoute.StreamInfo) },
                enabled = state.selectedLiveConnectionIds.isNotEmpty(),
            )
        }
    }
}

@Composable
fun Phase13StreamInfoScreen(
    state: MobileIntegrationState,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    LockedLivePage(
        phase = "GO LIVE SETUP",
        title = "Stream Info",
        subtitle = "Add the details that identify your live broadcast.",
        step = 3,
        totalSteps = 6,
        onBack = onBack,
    ) {
        UlTextField(state.liveTitle, { state.liveTitle = it.take(100) }, "Stream title")
        Spacer(Modifier.height(10.dp))
        UlTextField(state.liveDescription, { state.liveDescription = it.take(500) }, "Description", placeholder = "Optional")
        Spacer(Modifier.height(14.dp))

        Text("Category", color = AppText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Gaming", "IRL", "Talk").forEach { option ->
                FilterChip(
                    selected = state.liveCategory == option,
                    onClick = { state.liveCategory = option },
                    label = { Text(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Public", "Unlisted", "Private").forEach { option ->
                FilterChip(
                    selected = state.livePrivacy == option,
                    onClick = { state.livePrivacy = option },
                    label = { Text(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Next", onClick = onNext, enabled = state.liveTitle.isNotBlank())
    }
}

@Composable
fun Phase13StreamSettingsScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val config = streamState.config
    val maxResolution = state.membership?.maxResolution?.lowercase() ?: "720p"
    val allow1080 = maxResolution.contains("1080") || maxResolution.contains("1440") || maxResolution.contains("4k")

    LockedLivePage(
        phase = "GO LIVE SETUP",
        title = "Stream Settings",
        subtitle = "Optimize quality before the encoder starts.",
        step = 4,
        totalSteps = 6,
        onBack = onBack,
    ) {
        Text("Resolution", color = AppText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(StreamResolution.P720, StreamResolution.P1080).forEach { option ->
                val enabled = option != StreamResolution.P1080 || allow1080
                FilterChip(
                    selected = config.resolution == option,
                    onClick = { if (enabled) streamState.setResolution(option) },
                    enabled = enabled,
                    label = { Text(option.label) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(15.dp))

        Text("Frame rate", color = AppText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(StreamFps.FPS30, StreamFps.FPS60).forEach { option ->
                FilterChip(
                    selected = config.fps == option,
                    onClick = { streamState.setFps(option) },
                    label = { Text("${option.value} FPS") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(15.dp))

        Text("Bitrate", color = AppText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(4500, 6000, 6800).forEach { kbps ->
                FilterChip(
                    selected = config.bitrateKbps == kbps,
                    onClick = { streamState.setBitrateKbps(kbps) },
                    label = { Text("$kbps") },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(15.dp))

        Text("Orientation", color = AppText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(StreamOrientation.Landscape, StreamOrientation.Portrait, StreamOrientation.Auto).forEach { orientation ->
                FilterChip(
                    selected = config.orientation == orientation,
                    onClick = { streamState.setOrientation(orientation) },
                    label = { Text(orientation.label) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        LockedCard {
            Text("Recommended mobile baseline", color = AppPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("1080p • 30 FPS • 6800 Kbps is the current Universal Live quality target when your plan and connection support it.", color = AppTextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        UlPrimaryButton("Next", onClick = onNext)
    }
}

@Composable
fun Phase13AudioCameraScreen(
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    permissions: PermissionSetupController,
    onRoute: (AppRoute) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val cfg = streamState.config
    val permissionSnapshot = permissions.snapshot

    LockedLivePage(
        phase = "GO LIVE SETUP",
        title = "Audio & Camera",
        subtitle = "Choose the sources that will be included in your stream.",
        step = 5,
        totalSteps = 6,
        onBack = onBack,
    ) {
        Text("Main video source", color = AppText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(VideoSourceMode.SCREEN, VideoSourceMode.CAMERA).forEach { source ->
                FilterChip(
                    selected = captureController.requestedVideoSource == source,
                    onClick = {
                        captureController.setVideoSource(source)
                        if (source == VideoSourceMode.CAMERA) streamState.setInternalAudioEnabled(false)
                    },
                    label = { Text(source.label) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (captureController.requestedVideoSource == VideoSourceMode.CAMERA) {
            LockedCard {
                Text("Camera Live", color = AppPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("The selected front or back camera becomes the full broadcast scene. Device playback audio is disabled; microphone audio remains available.", color = AppTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
        SettingToggle(
            "Microphone",
            if (permissionSnapshot.microphone == PermissionGrantState.GRANTED) "Microphone permission ready" else "Permission is required when enabled",
            cfg.microphoneEnabled,
        ) { streamState.setMicrophoneEnabled(it) }
        Spacer(Modifier.height(9.dp))
        SettingToggle(
            "Game / device audio",
            if (captureController.requestedVideoSource == VideoSourceMode.CAMERA) "Available for screen/game capture" else "Capture supported app playback audio",
            cfg.internalAudioEnabled && captureController.requestedVideoSource == VideoSourceMode.SCREEN,
        ) {
            if (captureController.requestedVideoSource == VideoSourceMode.SCREEN) streamState.setInternalAudioEnabled(it)
        }
        Spacer(Modifier.height(9.dp))
        SettingToggle(
            if (captureController.requestedVideoSource == VideoSourceMode.CAMERA) "Camera lens" else "Face camera",
            if (permissionSnapshot.camera == PermissionGrantState.GRANTED) "Camera permission ready" else "Camera permission required",
            if (captureController.requestedVideoSource == VideoSourceMode.CAMERA) true else facecamState.config.enabled,
        ) { if (captureController.requestedVideoSource == VideoSourceMode.SCREEN) facecamState.setEnabled(it) }

        if (cfg.microphoneEnabled && permissionSnapshot.microphone != PermissionGrantState.GRANTED) {
            Spacer(Modifier.height(11.dp))
            UlSecondaryButton("Grant Microphone Permission", onClick = permissions::requestMicrophone)
        }
        if ((facecamState.config.enabled || captureController.requestedVideoSource == VideoSourceMode.CAMERA) && permissionSnapshot.camera != PermissionGrantState.GRANTED) {
            Spacer(Modifier.height(9.dp))
            UlSecondaryButton("Grant Camera Permission", onClick = permissions::requestCamera)
        }

        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Next", onClick = onNext)
    }
}

@Composable
fun Phase13SetupReviewScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    permissions: PermissionSetupController,
    onStartPreflight: () -> Unit,
    onBack: () -> Unit,
) {
    val connection = state.selectedLiveConnectionId?.let { id -> state.connections.firstOrNull { it.id == id } }
    val cfg = streamState.config
    val micOk = !cfg.microphoneEnabled || permissions.snapshot.microphone == PermissionGrantState.GRANTED
    val cameraRequired = facecamState.config.enabled || captureController.requestedVideoSource == VideoSourceMode.CAMERA
    val cameraOk = !cameraRequired || permissions.snapshot.camera == PermissionGrantState.GRANTED
    val ready = connection?.readyToPublish == true && state.liveTitle.isNotBlank() && micOk && cameraOk

    LockedLivePage(
        phase = "GO LIVE SETUP",
        title = "Ready to Go Live?",
        subtitle = "Review the setup before Universal Live runs real preflight checks.",
        step = 6,
        totalSteps = 6,
        onBack = onBack,
    ) {
        CheckRow("Destinations", if (state.selectedLiveConnectionIds.size > 1) "${state.selectedLiveConnectionIds.size} destinations selected" else connection?.displayName ?: "No destination selected", state.selectedLiveConnectionIds.isNotEmpty() && connection?.readyToPublish == true, warning = connection == null)
        Spacer(Modifier.height(8.dp))
        CheckRow("Stream info", "${state.liveCategory} • ${state.livePrivacy}", state.liveTitle.isNotBlank())
        Spacer(Modifier.height(8.dp))
        CheckRow("Quality", "${cfg.resolution.label} • ${cfg.fps.value} FPS • ${cfg.bitrateKbps} Kbps", true)
        Spacer(Modifier.height(8.dp))
        CheckRow("Microphone", if (cfg.microphoneEnabled) "Enabled" else "Disabled", micOk, warning = !micOk)
        Spacer(Modifier.height(8.dp))
        CheckRow("Camera", if (captureController.requestedVideoSource == VideoSourceMode.CAMERA) "Primary camera source" else if (facecamState.config.enabled) "Facecam enabled" else "Facecam off", cameraOk, warning = !cameraOk)
        Spacer(Modifier.height(8.dp))
        if (captureController.requestedVideoSource == VideoSourceMode.SCREEN) CheckRow("Screen capture", "Android system consent is requested only when the broadcast starts", permissions.snapshot.screenCaptureEducationComplete, warning = !permissions.snapshot.screenCaptureEducationComplete) else CheckRow("Video source", "Camera scene selected", true)

        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Start Preflight", onClick = onStartPreflight, enabled = ready)
    }
}

// -----------------------------------------------------------------------------
// Phase 14 - Stream Preflight
// -----------------------------------------------------------------------------

@Composable
fun Phase14PreflightStartScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    LockedLivePage(
        phase = "STREAM PREFLIGHT",
        title = "Stream Preflight",
        subtitle = "We'll check the real publish route, device setup and selected destination before going live.",
        onBack = onBack,
    ) {
        LockedCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.size(86.dp).clip(CircleShape).background(AppPrimary.copy(alpha = .08f)).border(1.dp, AppPrimary.copy(alpha = .55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = AppPrimary, fontSize = 40.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(15.dp))
                Text("Preflight Check", color = AppText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Destination • permissions • encoder config • backend session readiness", color = AppTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Begin Checks", onClick = onNext)
    }
}

@Composable
fun Phase14NetworkCheckScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var checked by remember { mutableStateOf(false) }
    var ok by remember { mutableStateOf(false) }

    fun runCheck() {
        checked = false
        scope.launch {
            val cfg = streamState.config
            val publishReady = state.prepareSelectedPublishConfig()
            ok = publishReady && state.runStreamPreflight(
                width = cfg.resolution.width,
                height = cfg.resolution.height,
                fps = cfg.fps.value,
                bitrateKbps = cfg.bitrateKbps,
                microphoneEnabled = cfg.microphoneEnabled,
                internalAudioEnabled = cfg.internalAudioEnabled,
                orientation = cfg.orientation.label,
            )
            checked = true
        }
    }

    LaunchedEffect(Unit) { runCheck() }

    LockedLivePage(
        phase = "STREAM PREFLIGHT",
        title = "Checking Connection",
        subtitle = "Verify that the backend can resolve a valid publish route for your selected destination.",
        onBack = onBack,
    ) {
        LockedCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (!checked) CircularProgressIndicator(color = AppPrimary)
                else Box(
                    Modifier.size(70.dp).clip(CircleShape).background((if (ok) AppSuccess else AppWarning).copy(alpha = .12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (ok) "✓" else "!", color = if (ok) AppSuccess else AppWarning, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(if (!checked) "Checking publish route..." else if (ok) "Publish route is ready" else "Publish route needs attention", color = AppText, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        CheckRow("Backend", if (ok) "Publish route + server preflight passed" else state.error ?: "Unable to pass backend preflight", ok, warning = checked && !ok)
        Spacer(Modifier.height(8.dp))
        CheckRow("Target bitrate", "${streamState.config.bitrateKbps} Kbps", true)
        Spacer(Modifier.height(8.dp))
        CheckRow("Network readiness", if (ok) "Backend route is reachable. Actual RTMP upload throughput is measured once the publisher connects." else "Resolve the backend publish route before continuing.", ok, warning = !ok)
        Spacer(Modifier.height(16.dp))
        if (!ok && checked) UlSecondaryButton("Test Again", onClick = { runCheck() })
        Spacer(Modifier.height(if (!ok && checked) 9.dp else 0.dp))
        UlPrimaryButton("Continue", onClick = onNext, enabled = checked && ok)
    }
}

@Composable
fun Phase14DeviceCheckScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    permissions: PermissionSetupController,
    onRoute: (AppRoute) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val cfg = streamState.config
    val snapshot = permissions.snapshot
    val micOk = !cfg.microphoneEnabled || snapshot.microphone == PermissionGrantState.GRANTED
    val cameraRequired = facecamState.config.enabled || captureController.requestedVideoSource == VideoSourceMode.CAMERA
    val camOk = !cameraRequired || snapshot.camera == PermissionGrantState.GRANTED
    val screenRequired = captureController.requestedVideoSource == VideoSourceMode.SCREEN
    val screenEducation = !screenRequired || snapshot.screenCaptureEducationComplete
    val allOk = micOk && camOk && screenEducation

    LockedLivePage(
        phase = "STREAM PREFLIGHT",
        title = "Checking Devices",
        subtitle = "Verify the actual permissions required by the stream configuration.",
        onBack = onBack,
    ) {
        CheckRow("Microphone", if (cfg.microphoneEnabled) "Required for this stream" else "Not requested", micOk, warning = !micOk)
        Spacer(Modifier.height(8.dp))
        CheckRow("Device audio", if (captureController.requestedVideoSource == VideoSourceMode.CAMERA) "Disabled for camera-only live" else if (cfg.internalAudioEnabled) "Requested when Android playback capture supports the current app" else "Disabled", true)
        Spacer(Modifier.height(8.dp))
        CheckRow("Camera", if (captureController.requestedVideoSource == VideoSourceMode.CAMERA) "Primary camera source" else if (facecamState.config.enabled) "Facecam enabled" else "Facecam is off", camOk, warning = !camOk)
        Spacer(Modifier.height(8.dp))
        CheckRow("Screen capture", if (screenRequired) "System MediaProjection consent will appear when you tap Go Live" else "Not required for camera live", screenEducation, warning = !screenEducation)

        if (!micOk) {
            Spacer(Modifier.height(10.dp))
            UlSecondaryButton("Allow Microphone", onClick = permissions::requestMicrophone)
        }
        if (!camOk) {
            Spacer(Modifier.height(9.dp))
            UlSecondaryButton("Allow Camera", onClick = permissions::requestCamera)
        }
        if (screenRequired && !screenEducation) {
            Spacer(Modifier.height(9.dp))
            UlSecondaryButton(
                "Acknowledge Screen Capture",
                onClick = {
                    permissions.markScreenCaptureEducationComplete()
                    scope.launch { state.acknowledgePermission(screenCapture = true) }
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        UlPrimaryButton("Continue", onClick = onNext, enabled = allOk)
    }
}

@Composable
fun Phase14DestinationCheckScreen(
    state: MobileIntegrationState,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val selected = state.selectedLiveConnectionId?.let { id -> state.connections.firstOrNull { it.id == id } }
    val selectedIds = state.selectedLiveConnectionIds
    val configReady = state.preparedPublishConfigs.size == selectedIds.size && selectedIds.isNotEmpty()
    val ok = selectedIds.isNotEmpty() && selectedIds.all { id -> state.connections.firstOrNull { it.id == id }?.readyToPublish == true } && configReady

    LockedLivePage(
        phase = "STREAM PREFLIGHT",
        title = "Testing Destinations",
        subtitle = "Confirm every selected publish route before the native publisher starts.",
        onBack = onBack,
    ) {
        if (selected != null) {
            LockedCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(selected.platform, 50)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(selected.displayName, color = AppText, fontWeight = FontWeight.Bold)
                        Text(platformLabel(selected.platform), color = AppTextSecondary, fontSize = 12.sp)
                    }
                    Text(if (ok) "Ready" else "Check", color = if (ok) AppSuccess else AppWarning, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        CheckRow("Credentials", if (selected?.credentialConfigured == true) "Secure RTMP credentials are stored" else "Credentials missing", selected?.credentialConfigured == true, warning = selected?.credentialConfigured != true)
        Spacer(Modifier.height(8.dp))
        CheckRow("Publish configuration", if (configReady) "Short-lived publish configuration prepared" else "Run connection check again", configReady, warning = !configReady)
        Spacer(Modifier.height(8.dp))
        CheckRow("Destination enabled", if (selected?.isEnabled == true) "Destination can be used" else "Destination is disabled", selected?.isEnabled == true, warning = selected?.isEnabled != true)

        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Continue", onClick = onNext, enabled = ok)
    }
}

@Composable
fun Phase14PreflightSuccessScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    captureController: CaptureController,
    permissions: PermissionSetupController,
    onGoLive: () -> Unit,
    onBack: () -> Unit,
) {
    val connection = state.selectedLiveConnectionId?.let { id -> state.connections.firstOrNull { it.id == id } }
    val cfg = streamState.config
    val ready = connection?.readyToPublish == true &&
        state.preparedPublishConfigs.isNotEmpty() &&
        state.streamPreflight?.ready == true &&
        (captureController.requestedVideoSource == VideoSourceMode.CAMERA || permissions.snapshot.screenCaptureEducationComplete)

    LockedLivePage(
        phase = "STREAM PREFLIGHT",
        title = "Preflight Complete!",
        subtitle = "Everything required by the current native streaming path is ready.",
        onBack = onBack,
    ) {
        LockedCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape).background(AppSuccess.copy(alpha = .12f)).border(1.dp, AppSuccess.copy(alpha = .45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = AppSuccess, fontSize = 44.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(12.dp))
                Text("All systems ready", color = AppSuccess, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        CheckRow("Destination", connection?.displayName ?: "Missing", connection?.readyToPublish == true)
        Spacer(Modifier.height(8.dp))
        CheckRow("Backend preflight", if (state.streamPreflight?.ready == true) "Passed • short-lived go-live authorization ready" else "Run preflight again", state.streamPreflight?.ready == true)
        Spacer(Modifier.height(8.dp))
        CheckRow("Video", "${cfg.resolution.label} • ${cfg.fps.value} FPS • ${cfg.bitrateKbps} Kbps", true)
        Spacer(Modifier.height(8.dp))
        CheckRow("Audio", "Mic ${if (cfg.microphoneEnabled) "ON" else "OFF"} • Game audio ${if (cfg.internalAudioEnabled) "ON" else "OFF"}", true)
        Spacer(Modifier.height(8.dp))
        CheckRow("Android capture", "System consent will be requested next", permissions.snapshot.screenCaptureEducationComplete)
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Go Live Now", onClick = onGoLive, enabled = ready)
    }
}

@Composable
fun Phase14GoingLiveScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    overlayState: OverlayState,
    sceneState: SceneState,
    onLive: () -> Unit,
    onBack: () -> Unit,
) {
    var starting by remember { mutableStateOf(false) }
    var startAttempt by remember { mutableIntStateOf(0) }
    val publishConfig = state.preparedPublishConfig
    val publishConfigs = state.preparedPublishConfigs

    fun canStart() = publishConfigs.isNotEmpty() && state.selectedLiveConnectionIds.isNotEmpty()

    LaunchedEffect(startAttempt) {
        if (canStart()) {
            starting = true
            val ids = state.selectedLiveConnectionIds.toList()
            if (ids.isNotEmpty() && state.beginBroadcast(state.liveTitle, ids, state.selectedScene?.id)) {
                prepareLockedCapture(
                    publishConfigs = publishConfigs,
                    streamState = streamState,
                    captureController = captureController,
                    facecamState = facecamState,
                    overlayState = overlayState,
                    sceneState = sceneState,
                )
                captureController.start()
                delay(350)
                onLive()
            }
            starting = false
        }
    }

    LockedLivePage(
        phase = "STREAM PREFLIGHT",
        title = if (starting) "Going Live..." else if (state.error == null) "Preparing Broadcast" else "Could Not Start",
        subtitle = "Creating the backend session and handing the real RTMP publish configuration to Android.",
        onBack = if (starting) null else onBack,
    ) {
        LockedCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (starting || state.loading) CircularProgressIndicator(color = AppPrimary)
                else Box(
                    Modifier.size(70.dp).clip(CircleShape).background(AppWarning.copy(alpha = .10f)),
                    contentAlignment = Alignment.Center,
                ) { Text("!", color = AppWarning, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(14.dp))
                Text(if (starting) "Initialize • Connect • Broadcast" else state.error ?: "Waiting", color = AppText, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
        if (!starting && state.error != null) {
            Spacer(Modifier.height(14.dp))
            UlPrimaryButton("Try Again", onClick = { startAttempt++ })
        }
    }
}

// -----------------------------------------------------------------------------
// Phase 15 - Active Live
// -----------------------------------------------------------------------------

@Composable
fun Phase15ActiveLiveScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    sceneState: SceneState,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    val snap = captureController.snapshot
    val cfg = streamState.config
    val live = snap.publishStatus == PublishStatus.LIVE
    val bitrate = (snap.networkBitrateBps / 1000L).coerceAtLeast(0L)
    val connection = state.selectedLiveConnectionId?.let { id -> state.connections.firstOrNull { it.id == id } }
    var elapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(snap.publishStatus) {
        if (state.activeBroadcast != null) {
            state.reportPublisherState(
                snap.toDeviceTelemetry(
                    connectionId = state.selectedLiveConnectionId,
                    targetBitrateKbps = cfg.bitrateKbps,
                )
            )
        }
        when (snap.publishStatus) {
            PublishStatus.RECONNECTING -> onRoute(AppRoute.Reconnecting)
            PublishStatus.ERROR, PublishStatus.DISCONNECTED -> if (snap.isActive) onRoute(AppRoute.LiveRecovery)
            else -> Unit
        }
    }

    LaunchedEffect(state.activeBroadcast?.id, snap.publishStatus, snap.status) {
        if (state.activeBroadcast != null && (snap.isActive || snap.isPublishing)) {
            while (true) {
                state.heartbeat(
                    snap.toDeviceTelemetry(
                        connectionId = state.selectedLiveConnectionId,
                        targetBitrateKbps = cfg.bitrateKbps,
                    )
                )
                delay(15_000)
            }
        }
    }

    LaunchedEffect(snap.isActive || snap.isPublishing) {
        while (snap.isActive || snap.isPublishing) {
            delay(1000)
            elapsed++
        }
    }

    LockedLivePage(
        phase = "LIVE CONTROL ROOM",
        title = "Live Control",
        subtitle = "Your broadcast remains the priority until the stream ends.",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AppLive,
            ) {
                Text("LIVE", color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(formatElapsed(elapsed), color = AppText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(if (live) "Connected" else snap.publishStatus.name, color = if (live) AppSuccess else AppWarning, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (snap.encoderWidth > 0 && snap.encoderHeight > 0) snap.encoderWidth.toFloat() / snap.encoderHeight.toFloat() else 16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .background(AppBackgroundSecondary)
                .border(1.dp, AppPrimary.copy(alpha = .55f), RoundedCornerShape(18.dp)),
        ) {
            if (snap.compositorActive) {
                LiveOutputPreview(Modifier.fillMaxSize())
            } else {
                Column(Modifier.align(Alignment.Center).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DIRECT SCREEN CAPTURE", color = AppPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Android is publishing the selected device screen directly.", color = AppTextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (connection != null) {
            LockedCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(connection.platform, 42)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(connection.displayName, color = AppText, fontWeight = FontWeight.Bold)
                        Text(platformLabel(connection.platform), color = AppTextMuted, fontSize = 11.sp)
                    }
                    Text(if (live) "LIVE" else snap.publishStatus.name, color = if (live) AppLive else AppWarning, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            LiveActionButton("Mic", cfg.microphoneEnabled) { onRoute(AppRoute.LiveControls) }
            LiveActionButton("Camera", facecamState.config.enabled) { onRoute(AppRoute.LiveControls) }
            LiveActionButton("Scene", true) { onRoute(AppRoute.LiveSceneSwitcher) }
            LiveActionButton("More", true) { onRoute(AppRoute.LiveControls) }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox("Bitrate", if (bitrate > 0) "${bitrate}K" else cfg.bitrateLabel, Modifier.weight(1f))
            MetricBox("FPS", (snap.encoderFps.takeIf { it > 0 } ?: cfg.fps.value).toString(), Modifier.weight(1f))
            MetricBox("Quality", cfg.resolution.label, Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UlSecondaryButton("Live Stats", onClick = { onRoute(AppRoute.LiveStats) }, modifier = Modifier.weight(1f))
            UlSecondaryButton("Health", onClick = { onRoute(AppRoute.LiveHealth) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        UlSecondaryButton("Live Destinations", onClick = { onRoute(AppRoute.LiveDestinations) })
        Spacer(Modifier.height(9.dp))
        Button(
            onClick = { onRoute(AppRoute.EndStreamConfirmation) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
        ) { Text("End Live", color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun RowScope.LiveActionButton(label: String, active: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(50.dp),
        contentPadding = PaddingValues(horizontal = 3.dp),
        border = BorderStroke(1.dp, if (active) AppPrimary.copy(alpha = .75f) else AppBorder),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(label, color = if (active) AppPrimary else AppTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricBox(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = AppText) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PhaseSurfaceRaised)
            .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(label.uppercase(), color = AppTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun Phase15LiveStatsScreen(
    streamState: StreamConfigState,
    captureController: CaptureController,
    onBack: () -> Unit,
) {
    val snap = captureController.snapshot
    val cfg = streamState.config
    val networkKbps = snap.networkBitrateBps / 1000L
    val encodedMb = snap.encodedVideoBytes / (1024f * 1024f)

    LockedLivePage(
        phase = "LIVE CONTROL ROOM",
        title = "Live Stats",
        subtitle = "Real encoder and publisher telemetry from this device.",
        onBack = onBack,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox("Current bitrate", if (networkKbps > 0) "$networkKbps Kbps" else "Waiting", Modifier.weight(1f), if (networkKbps > 0) AppPrimary else AppTextMuted)
            MetricBox("Target", cfg.bitrateLabel, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox("Encoded FPS", if (snap.encodedFpsActual > 0.0) formatMetric(snap.encodedFpsActual) else "Waiting", Modifier.weight(1f))
            MetricBox("Sent FPS", if (snap.sentFpsActual > 0.0) formatMetric(snap.sentFpsActual) else "Waiting", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        SetupValueRow("Encoder", snap.encoderName.ifBlank { "Hardware encoder starting" })
        Spacer(Modifier.height(8.dp))
        SetupValueRow("Dimensions", if (snap.encoderWidth > 0) "${snap.encoderWidth} × ${snap.encoderHeight}" else "${cfg.resolution.width} × ${cfg.resolution.height}")
        Spacer(Modifier.height(8.dp))
        SetupValueRow("Encoded video", "${encodedMb.toInt()} MB")
        Spacer(Modifier.height(8.dp))
        SetupValueRow("Measured encoder bitrate", if (snap.encoderMeasuredBitrateKbps > 0) "${snap.encoderMeasuredBitrateKbps} Kbps" else "Waiting")
        Spacer(Modifier.height(8.dp))
        SetupValueRow("Last video packet", snap.lastVideoPacketAgeMs?.let { "${it} ms ago" } ?: "Unknown")
        Spacer(Modifier.height(8.dp))
        SetupValueRow("Last audio packet", snap.lastAudioPacketAgeMs?.let { "${it} ms ago" } ?: "Unknown")
        Spacer(Modifier.height(8.dp))
        SetupValueRow("Keyframe interval", snap.keyframeIntervalMs?.let { "${it} ms" } ?: "Waiting")
        Spacer(Modifier.height(8.dp))
        SetupValueRow("PTS monotonic", "Video ${snap.videoPtsMonotonic?.toString() ?: "unknown"} • Audio ${snap.audioPtsMonotonic?.toString() ?: "unknown"}")
        Spacer(Modifier.height(8.dp))
        SetupValueRow("RTMP queue", snap.rtmpQueueDepth?.toString() ?: "Not exposed by publisher library")
        Spacer(Modifier.height(8.dp))
        SetupValueRow("Socket write latency", snap.socketWriteLatencyMs?.let { "${it} ms" } ?: "Not exposed by publisher library")
        Spacer(Modifier.height(8.dp))
        SetupValueRow("Publisher enqueue", snap.publisherEnqueueLatencyMs?.let { "${it} ms" } ?: "Waiting")
        Spacer(Modifier.height(8.dp))
        SetupValueRow("Reconnects", snap.reconnectCount.toString())
        Spacer(Modifier.height(8.dp))
        SetupValueRow("Audio frames", snap.publishedAudioFrames.toString())
    }
}

@Composable
fun Phase15LiveChatScreen(onBack: () -> Unit) {
    LockedLivePage(
        phase = "LIVE CONTROL ROOM",
        title = "Live Chat",
        subtitle = "Platform chat lives behind platform-specific APIs and is not faked in the current backend.",
        onBack = onBack,
    ) {
        LockedCard {
            Text("Chat API not connected yet", color = AppText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                "When YouTube, Facebook, Twitch and TikTok OAuth/chat APIs are added, their messages will appear here without changing this screen structure.",
                color = AppTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
        Spacer(Modifier.height(14.dp))
        UlSecondaryButton("Return to Live", onClick = onBack)
    }
}

@Composable
fun Phase15LiveControlsScreen(
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val cfg = streamState.config
    var adaptive by remember { mutableStateOf(captureController.adaptiveBitrateEnabled) }

    LockedLivePage(
        phase = "LIVE CONTROL ROOM",
        title = "Live Controls",
        subtitle = "Safe controls that can be changed without rebuilding the active encoder session.",
        onBack = onBack,
    ) {
        SettingToggle("Microphone", "Toggle live microphone capture", cfg.microphoneEnabled) {
            streamState.setMicrophoneEnabled(it)
            captureController.configureAudio(it, streamState.config.internalAudioEnabled)
        }
        Spacer(Modifier.height(8.dp))
        SettingToggle("Game audio", "Toggle supported Android playback audio", cfg.internalAudioEnabled) {
            streamState.setInternalAudioEnabled(it)
            captureController.configureAudio(streamState.config.microphoneEnabled, it)
        }
        Spacer(Modifier.height(8.dp))
        SettingToggle("Face camera", "Show or hide the configured facecam source", facecamState.config.enabled) {
            facecamState.setEnabled(it)
        }
        Spacer(Modifier.height(8.dp))
        SettingToggle("Adaptive bitrate", "Allow the native publisher to change bitrate at runtime", adaptive) {
            adaptive = it
            captureController.setAdaptiveBitrateEnabled(it)
        }
        Spacer(Modifier.height(12.dp))
        UlSecondaryButton("Switch Scene", onClick = { onRoute(AppRoute.LiveSceneSwitcher) })
        Spacer(Modifier.height(8.dp))
        UlSecondaryButton("Stream Health", onClick = { onRoute(AppRoute.LiveHealth) })
        Spacer(Modifier.height(8.dp))
        UlSecondaryButton("Recovery Options", onClick = { onRoute(AppRoute.LiveRecovery) })
    }
}

@Composable
fun Phase15StreamHealthScreen(
    streamState: StreamConfigState,
    captureController: CaptureController,
    onBack: () -> Unit,
) {
    val cfg = streamState.config
    val snap = captureController.snapshot
    val bitrate = snap.networkBitrateBps / 1000L
    val threshold = (cfg.bitrateKbps * .70).toLong()
    val good = snap.publishStatus == PublishStatus.LIVE && (bitrate == 0L || bitrate >= threshold)
    val healthColor = if (good) AppSuccess else if (snap.publishStatus == PublishStatus.RECONNECTING) AppWarning else AppLive
    val healthLabel = if (good) "Excellent" else if (snap.publishStatus == PublishStatus.RECONNECTING) "Recovering" else "Needs attention"

    LockedLivePage(
        phase = "LIVE CONTROL ROOM",
        title = "Stream Health",
        subtitle = "Live health is calculated from actual publisher and encoder state.",
        onBack = onBack,
    ) {
        LockedCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(healthColor))
                Spacer(Modifier.width(9.dp))
                Text(healthLabel, color = healthColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (good) .92f else if (snap.publishStatus == PublishStatus.RECONNECTING) .48f else .20f },
                modifier = Modifier.fillMaxWidth(),
                color = healthColor,
                trackColor = AppSurfaceInteractive,
            )
        }
        Spacer(Modifier.height(10.dp))
        CheckRow("Publisher", snap.publishMessage, snap.publishStatus == PublishStatus.LIVE, warning = snap.publishStatus == PublishStatus.RECONNECTING)
        Spacer(Modifier.height(8.dp))
        CheckRow("Video encoder", snap.encoderName.ifBlank { "Starting" }, snap.encoderName.isNotBlank(), warning = snap.encoderName.isBlank())
        Spacer(Modifier.height(8.dp))
        CheckRow("Audio", snap.audioMessage, !cfg.microphoneEnabled && !cfg.internalAudioEnabled || snap.microphoneActive || snap.internalAudioActive, warning = cfg.microphoneEnabled || cfg.internalAudioEnabled)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricBox("Network", if (bitrate > 0) "$bitrate Kbps" else "Waiting", Modifier.weight(1f))
            MetricBox("FPS", (snap.encoderFps.takeIf { it > 0 } ?: cfg.fps.value).toString(), Modifier.weight(1f))
        }
    }
}

@Composable
fun Phase15LiveDestinationsScreen(
    state: MobileIntegrationState,
    captureController: CaptureController,
    onManage: () -> Unit,
    onBack: () -> Unit,
) {
    val activeId = state.selectedLiveConnectionId

    LockedLivePage(
        phase = "LIVE CONTROL ROOM",
        title = "Live Destinations",
        subtitle = "See which saved destination is receiving the current native publish session.",
        onBack = onBack,
    ) {
        state.connections.filter { it.isEnabled }.forEachIndexed { index, connection ->
            val active = connection.id == activeId
            LockedCard(selected = active) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(connection.platform, 45)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(connection.displayName, color = AppText, fontWeight = FontWeight.Bold)
                        Text(platformLabel(connection.platform), color = AppTextMuted, fontSize = 11.sp)
                    }
                    Text(
                        if (active) captureController.snapshot.publishStatus.name else "SAVED",
                        color = if (active) AppLive else AppTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            if (index != state.connections.lastIndex) Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(14.dp))
        LockedCard {
            Text("Why only one LIVE badge?", color = AppPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("The current Android publisher is direct-to-one-RTMP. The future backend relay will activate simultaneous destination badges here.", color = AppTextMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.height(14.dp))
        UlSecondaryButton("Manage Destinations", onClick = onManage)
    }
}

// -----------------------------------------------------------------------------
// Phase 16 - Disconnect / Recovery
// -----------------------------------------------------------------------------

@Composable
fun Phase16EndStreamConfirmScreen(
    captureController: CaptureController,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
) {
    LockedLivePage(
        phase = "STREAM RECOVERY",
        title = "End Stream?",
        subtitle = "End the native RTMP publisher and close the backend broadcast session.",
        onBack = onCancel,
    ) {
        LockedCard {
            Text("Current destination", color = AppTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(captureController.snapshot.publishTarget.ifBlank { "Active broadcast" }, color = AppText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(captureController.snapshot.publishStatus.name, color = AppLive, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onEnd,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
        ) { Text("End Stream", color = Color.White, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(9.dp))
        UlSecondaryButton("Cancel", onClick = onCancel)
    }
}

@Composable
fun Phase16EndingScreen(
    state: MobileIntegrationState,
    captureController: CaptureController,
    onEnded: () -> Unit,
) {
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        captureController.stop()
        state.endBroadcast()
        delay(500)
        done = true
        onEnded()
    }

    LockedLivePage(
        phase = "STREAM RECOVERY",
        title = "Stream Ending...",
        subtitle = "Stopping the publisher and finalizing the backend session.",
    ) {
        LockedCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (!done) CircularProgressIndicator(color = AppPrimary)
                else Text("✓", color = AppSuccess, fontSize = 40.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                Text(if (done) "Stream ended" else "Closing stream safely", color = AppText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun Phase16RecoveryHubScreen(
    captureController: CaptureController,
    onReconnect: () -> Unit,
    onEnd: () -> Unit,
    onReturn: () -> Unit,
) {
    val snap = captureController.snapshot

    LockedLivePage(
        phase = "STREAM RECOVERY",
        title = "Connection Lost",
        subtitle = "Universal Live keeps the broadcast state visible while the publisher recovers.",
        onBack = onReturn,
    ) {
        LockedCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(AppLive.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Text("!", color = AppLive, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(snap.publishStatus.name, color = AppLive, fontWeight = FontWeight.Bold)
                    Text(snap.publishMessage, color = AppTextSecondary, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        CheckRow("Capture session", if (snap.isActive) "Still active" else "Capture stopped", snap.isActive, warning = !snap.isActive)
        Spacer(Modifier.height(8.dp))
        CheckRow("Audio route", snap.audioMessage, snap.microphoneActive || snap.internalAudioActive || (!snap.microphoneRequested && !snap.internalAudioRequested), warning = snap.microphoneRequested || snap.internalAudioRequested)
        Spacer(Modifier.height(8.dp))
        CheckRow("Destination", snap.publishTarget.ifBlank { "Current RTMP target" }, snap.publishStatus == PublishStatus.LIVE, warning = true)
        Spacer(Modifier.height(16.dp))
        UlPrimaryButton("Reconnect", onClick = onReconnect)
        Spacer(Modifier.height(9.dp))
        UlSecondaryButton("End Stream", onClick = onEnd)
    }
}

@Composable
fun Phase16ReconnectingScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    overlayState: OverlayState,
    sceneState: SceneState,
    onRecovered: () -> Unit,
    onFailed: () -> Unit,
    onCancel: () -> Unit,
) {
    var seconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val recoveryReady = state.recoverBroadcast(captureController.snapshot.publisherInstanceId.ifBlank { null })
        if (!recoveryReady) {
            onFailed()
            return@LaunchedEffect
        }
        if (!captureController.snapshot.isActive) {
            val configs = state.preparedPublishConfigs.ifEmpty {
                state.preparedPublishConfig?.let { listOf(it) } ?: emptyList()
            }
            if (configs.isEmpty()) {
                onFailed()
                return@LaunchedEffect
            }
            prepareLockedCapture(
                publishConfigs = configs,
                streamState = streamState,
                captureController = captureController,
                facecamState = facecamState,
                overlayState = overlayState,
                sceneState = sceneState,
            )
            captureController.start()
        }

        while (seconds < 30) {
            when (captureController.snapshot.publishStatus) {
                PublishStatus.LIVE -> {
                    state.reportPublisherState(
                        captureController.snapshot.toDeviceTelemetry(
                            connectionId = state.selectedLiveConnectionId,
                            targetBitrateKbps = streamState.config.bitrateKbps,
                        )
                    )
                    onRecovered()
                    return@LaunchedEffect
                }
                PublishStatus.ERROR -> {
                    state.reportPublisherState(
                        captureController.snapshot.toDeviceTelemetry(
                            connectionId = state.selectedLiveConnectionId,
                            targetBitrateKbps = streamState.config.bitrateKbps,
                        )
                    )
                }
                else -> Unit
            }
            delay(1000)
            seconds++
        }
        if (captureController.snapshot.publishStatus != PublishStatus.LIVE) onFailed()
    }

    LockedLivePage(
        phase = "STREAM RECOVERY",
        title = "Reconnecting...",
        subtitle = "The native RTMP publisher is being given time to restore its ingest connection.",
        onBack = onCancel,
    ) {
        LockedCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = AppPrimary)
                Spacer(Modifier.height(14.dp))
                Text("Checking publisher state", color = AppText, fontWeight = FontWeight.Bold)
                Text("${seconds}s", color = AppPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        CheckRow("Network", captureController.snapshot.publishMessage, captureController.snapshot.publishStatus == PublishStatus.LIVE, warning = true)
        Spacer(Modifier.height(8.dp))
        CheckRow("Destination", captureController.snapshot.publishTarget.ifBlank { "RTMP target" }, true)
        Spacer(Modifier.height(12.dp))
        UlSecondaryButton("Cancel", onClick = onCancel)
    }
}

@Composable
fun Phase16RecoveredScreen(
    captureController: CaptureController,
    onReturn: () -> Unit,
    onStats: () -> Unit,
) {
    LockedLivePage(
        phase = "STREAM RECOVERY",
        title = "Stream Recovered",
        subtitle = "The publisher is back online.",
    ) {
        LockedCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(84.dp).clip(CircleShape).background(AppSuccess.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                    Text("✓", color = AppSuccess, fontSize = 42.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(12.dp))
                Text("You're back online", color = AppSuccess, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(captureController.snapshot.publishTarget.ifBlank { "Destination restored" }, color = AppTextSecondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        UlPrimaryButton("Return to Live", onClick = onReturn)
        Spacer(Modifier.height(9.dp))
        UlSecondaryButton("View Stats", onClick = onStats)
    }
}

@Composable
fun Phase16RecoveryFailedScreen(
    captureController: CaptureController,
    onRetry: () -> Unit,
    onEnd: () -> Unit,
) {
    LockedLivePage(
        phase = "STREAM RECOVERY",
        title = "Recovery Failed",
        subtitle = "The publisher did not return to LIVE within the recovery window.",
    ) {
        LockedCard {
            Text("Stream error", color = AppLive, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(captureController.snapshot.publishMessage, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text("The capture session is not silently marked as recovered. Retry or end the session.", color = AppTextMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(16.dp))
        UlPrimaryButton("Try Again", onClick = onRetry)
        Spacer(Modifier.height(9.dp))
        UlSecondaryButton("End Session", onClick = onEnd)
    }
}


private fun com.universallive.app.streaming.capture.CaptureSnapshot.toDeviceTelemetry(
    connectionId: String?,
    targetBitrateKbps: Int,
): DeviceStreamTelemetry = DeviceStreamTelemetry(
    connectionId = connectionId,
    bitrateKbps = (networkBitrateBps / 1000L).toInt().takeIf { it > 0 },
    targetBitrateKbps = targetBitrateKbps,
    encoderBitrateKbps = encoderMeasuredBitrateKbps.takeIf { it > 0 },
    rtmpUploadKbps = (networkBitrateBps / 1000L).toInt().takeIf { it > 0 },
    fps = encodedFpsActual.takeIf { it > 0.0 } ?: encoderFps.toDouble().takeIf { it > 0.0 },
    encodedFps = encodedFpsActual.takeIf { it > 0.0 },
    sentFps = sentFpsActual.takeIf { it > 0.0 },
    droppedFrames = droppedFrames,
    publishedVideoFrames = publishedVideoFrames,
    publishedAudioFrames = publishedAudioFrames,
    encoderWidth = encoderWidth.takeIf { it > 0 },
    encoderHeight = encoderHeight.takeIf { it > 0 },
    encoderName = encoderName.takeIf { it.isNotBlank() },
    networkStatus = publishStatus.name.lowercase(),
    publishStatus = publishStatus.name.lowercase(),
    audioStatus = audioMessage,
    rtmpQueueDepth = rtmpQueueDepth,
    socketWriteLatencyMs = socketWriteLatencyMs,
    publisherEnqueueLatencyMs = publisherEnqueueLatencyMs,
    lastVideoPacketAgeMs = lastVideoPacketAgeMs,
    lastAudioPacketAgeMs = lastAudioPacketAgeMs,
    keyframeIntervalMs = keyframeIntervalMs,
    videoPtsMonotonic = videoPtsMonotonic,
    audioPtsMonotonic = audioPtsMonotonic,
    reconnectCount = reconnectCount,
    publisherInstanceId = publisherInstanceId.takeIf { it.isNotBlank() },
)

private fun formatMetric(value: Double): String = ((value * 10.0).toInt() / 10.0).toString()

private fun formatElapsed(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

private fun prepareLockedCapture(
    publishConfigs: List<PublishConfig>,
    streamState: StreamConfigState,
    captureController: CaptureController,
    facecamState: FacecamState,
    overlayState: OverlayState,
    sceneState: SceneState,
) {
    val config = streamState.config
    captureController.configureAudio(config.microphoneEnabled, config.internalAudioEnabled && captureController.requestedVideoSource == VideoSourceMode.SCREEN)
    captureController.configureVideo(
        width = config.resolution.width,
        height = config.resolution.height,
        fps = config.fps.value,
        bitrateKbps = config.bitrateKbps,
        orientation = config.orientation.label,
    )
    captureController.configurePublishTargets(
        publishConfigs.map { cfg ->
            com.universallive.app.streaming.capture.PublishTargetConfig(
                serverUrl = cfg.serverUrl,
                streamKey = cfg.streamKey,
                targetName = cfg.displayName,
            )
        }
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
