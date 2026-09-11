package com.universallive.app.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.integration.StreamingConnection
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.permissions.PermissionGrantState
import com.universallive.app.permissions.PermissionSetupController
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.capture.PublishStatus
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.*

@Composable
fun ConnectedHomeScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    captureController: CaptureController,
    sceneState: SceneState,
    permissions: PermissionSetupController,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    LaunchedEffect(Unit) {
        state.refreshHomeDashboard()
        state.refreshConnections()
        state.refreshScenes()
        state.refreshNotifications()
    }

    val config = streamState.config
    val activeSnapshot = captureController.snapshot
    val defaultDestination = state.connections.firstOrNull { it.isDefault && it.readyToPublish && it.isEnabled }
        ?: state.connections.firstOrNull { it.readyToPublish && it.isEnabled }
    val sceneName = state.selectedScene?.name ?: sceneState.activeScene.name
    val micReady = !config.microphoneEnabled || permissions.snapshot.microphone == PermissionGrantState.GRANTED
    val destinationReady = defaultDestination != null
    val setupReady = state.onboarding?.permissionEducationCompleted == true
    val ready = destinationReady && micReady && setupReady
    val unread = state.notifications.count { !it.isRead }

    AppScaffold(
        title = "Home",
        selected = AppDestination.Home,
        onDestinationChanged = onDestination,
    ) {
        if (activeSnapshot.isPublishing || activeSnapshot.publishStatus == PublishStatus.LIVE) {
            ActiveStreamBanner(
                target = activeSnapshot.publishTarget.ifBlank { defaultDestination?.displayName ?: "Live destination" },
                onOpen = { onRoute(AppRoute.LiveBroadcast) },
            )
            Spacer(Modifier.height(12.dp))
        }

        HomeHero(
            ready = ready,
            destination = defaultDestination,
            resolution = config.resolution.label,
            fps = config.fps.value,
            bitrate = config.bitrateKbps,
            onPrimary = {
                if (destinationReady) onDestination(AppDestination.GoLive)
                else onRoute(AppRoute.Connections)
            },
        )

        Spacer(Modifier.height(14.dp))
        Text("Stream readiness", color = AppText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(9.dp))
        ReadinessGrid(
            destination = if (destinationReady) "Connected" else "Missing",
            microphone = if (micReady) "Ready" else "Permission needed",
            scene = sceneName,
            quality = "${config.fps.value} FPS • ${config.bitrateKbps} Kbps",
            onClick = { onRoute(AppRoute.StreamReadiness) },
        )

        Spacer(Modifier.height(18.dp))
        DefaultSetupCard(
            destination = defaultDestination,
            scene = sceneName,
            onConnections = { onRoute(AppRoute.Connections) },
            onStudio = { onDestination(AppDestination.Scenes) },
            onGoLive = {
                if (destinationReady) onDestination(AppDestination.GoLive)
                else onRoute(AppRoute.Connections)
            },
        )

        Spacer(Modifier.height(18.dp))
        Text("Quick actions", color = AppText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("LINK", "Connections", "Manage platforms", Modifier.weight(1f)) { onRoute(AppRoute.Connections) }
            QuickAction("STU", "Studio", "Scenes & sources", Modifier.weight(1f)) { onDestination(AppDestination.Scenes) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("AUD", "Audio", "Mic & device audio", Modifier.weight(1f)) { onRoute(AppRoute.AudioMixer) }
            QuickAction("QTY", "Stream quality", "${config.fps.value} FPS • ${config.bitrateKbps}K", Modifier.weight(1f)) { onRoute(AppRoute.StreamQuality) }
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Recent activity", color = AppText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onDestination(AppDestination.Activity) }) {
                Text("View all", color = AppPrimary, fontWeight = FontWeight.Bold)
            }
        }

        if (state.notifications.isEmpty()) {
            UlCard {
                Text("No recent updates", color = AppText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Connection tests, stream events and important account updates will appear here.", color = AppTextMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        } else {
            state.notifications.take(4).forEach { item ->
                ActivityRow(item.title, item.body, if (item.isRead) AppTextMuted else AppPrimary)
                Spacer(Modifier.height(8.dp))
            }
        }

        if (unread > 0) {
            Spacer(Modifier.height(4.dp))
            UlSecondaryButton(
                text = "$unread unread notification${if (unread == 1) "" else "s"}",
                onClick = { onRoute(AppRoute.Notifications) },
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HomeHero(
    ready: Boolean,
    destination: StreamingConnection?,
    resolution: String,
    fps: Int,
    bitrate: Int,
    onPrimary: () -> Unit,
) {
    val stateColor = if (ready) AppSuccess else AppLive
    val title = if (ready) "You're all set!" else "Connect a destination to go live"
    val body = if (ready) "Your stream setup is ready when you are." else "Add at least one ready streaming destination before starting a broadcast."

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(AppSurfaceRaised, AppSurface, AppBackgroundSecondary)))
            .border(1.dp, AppPrimary.copy(alpha = .18f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        UlStatusBadge(if (ready) "READY TO GO LIVE" else "NOT READY", stateColor)
        Spacer(Modifier.height(14.dp))
        Text(title, color = AppText, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text(body, color = AppTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)

        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(145.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(AppPrimary.copy(alpha = .18f), AppSurface, AppBackground),
                    ),
                )
                .border(1.dp, AppBorder, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (ready) "LIVE" else "+", color = AppPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text(destination?.displayName ?: "Add your first destination", color = AppText, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip(resolution, "Resolution", Modifier.weight(1f))
            MetricChip("$fps FPS", "Frame rate", Modifier.weight(1f))
            MetricChip("$bitrate Kbps", "Bitrate", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        UlPrimaryButton(if (ready) "Go Live" else "Add Destination", onClick = onPrimary)
    }
}

@Composable
private fun MetricChip(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(AppBackground.copy(alpha = .72f))
            .border(1.dp, AppBorder, RoundedCornerShape(13.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = AppText, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
        Text(label, color = AppTextMuted, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun ReadinessGrid(
    destination: String,
    microphone: String,
    scene: String,
    quality: String,
    onClick: () -> Unit,
) {
    UlCard(Modifier.clickable(onClick = onClick)) {
        HomeStatusRow("Network", "Backend reachable", AppSuccess)
        HomeStatusRow("Microphone", microphone, if (microphone == "Ready") AppSuccess else AppWarning)
        HomeStatusRow("Scene", scene, AppSuccess)
        HomeStatusRow("Destination", destination, if (destination == "Connected") AppSuccess else AppWarning)
        HomeStatusRow("Stream quality", quality, AppSuccess)
    }
}

@Composable
private fun HomeStatusRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, color = AppText, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text(value, color = AppTextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun DefaultSetupCard(
    destination: StreamingConnection?,
    scene: String,
    onConnections: () -> Unit,
    onStudio: () -> Unit,
    onGoLive: () -> Unit,
) {
    UlCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Default destination", color = AppTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(destination?.displayName ?: "Not connected", color = AppText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(destination?.platform?.replace("_", " ")?.uppercase() ?: "Choose a platform", color = if (destination != null) AppSuccess else AppWarning, fontSize = 11.sp)
            }
            TextButton(onClick = onConnections) { Text("Edit", color = AppPrimary, fontWeight = FontWeight.Bold) }
        }
        Divider(color = AppBorder, modifier = Modifier.padding(vertical = 13.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Current scene", color = AppTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(scene, color = AppText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            TextButton(onClick = onStudio) { Text("Change", color = AppPrimary, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(14.dp))
        UlPrimaryButton(if (destination != null) "Go Live" else "Connect a Destination", onClick = onGoLive)
    }
}

@Composable
private fun QuickAction(icon: String, title: String, body: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AppSurface)
            .border(1.dp, AppPrimary.copy(alpha = .28f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(AppPrimary.copy(alpha = .11f)), contentAlignment = Alignment.Center) {
            Text(icon, color = AppPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = AppText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(body, color = AppTextMuted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun ActivityRow(title: String, body: String, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = AppText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(body, color = AppTextMuted, fontSize = 11.sp, maxLines = 2)
        }
    }
}

@Composable
private fun ActiveStreamBanner(target: String, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        color = AppLive.copy(alpha = .12f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppLive.copy(alpha = .35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(AppLive))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("LIVE NOW", color = AppLive, fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text(target, color = AppText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Text("OPEN", color = AppText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun Phase09StreamReadinessScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    permissions: PermissionSetupController,
    sceneState: SceneState,
    onBack: () -> Unit,
    onGoLive: () -> Unit,
) {
    LaunchedEffect(Unit) {
        state.refreshConnections()
        state.refreshScenes()
    }
    val config = streamState.config
    val destination = state.connections.firstOrNull { it.readyToPublish && it.isEnabled }
    val micReady = !config.microphoneEnabled || permissions.snapshot.microphone == PermissionGrantState.GRANTED
    val screenReady = state.onboarding?.permissionEducationCompleted == true
    val ready = destination != null && micReady && screenReady

    Column(
        Modifier.fillMaxSize().background(AppBackground).systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Surface(
            onClick = onBack,
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(13.dp),
            color = AppSurface,
            border = BorderStroke(1.dp, AppBorder),
        ) { Box(contentAlignment = Alignment.Center) { Text("‹", color = AppText, fontSize = 28.sp) } }
        Spacer(Modifier.height(20.dp))
        Text("Stream readiness", color = AppText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Check the systems Universal Live depends on before starting your broadcast.", color = AppTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(18.dp))
        UlStatusBadge(if (ready) "READY TO GO LIVE" else "ACTION NEEDED", if (ready) AppSuccess else AppWarning)
        Spacer(Modifier.height(16.dp))
        UlCard {
            HomeStatusRow("Account / backend", if (state.session != null) "Connected" else "Session unavailable", if (state.session != null) AppSuccess else AppWarning)
            HomeStatusRow("Microphone", if (micReady) "Ready" else "Permission needed", if (micReady) AppSuccess else AppWarning)
            HomeStatusRow("Device audio", if (config.internalAudioEnabled) "Enabled at capture" else "Off", if (config.internalAudioEnabled) AppSuccess else AppTextMuted)
            HomeStatusRow("Screen capture", if (screenReady) "Ready at Go Live" else "Review permissions", if (screenReady) AppSuccess else AppWarning)
            HomeStatusRow("Destination", destination?.displayName ?: "Missing", if (destination != null) AppSuccess else AppWarning)
            HomeStatusRow("Scene", state.selectedScene?.name ?: sceneState.activeScene.name, AppSuccess)
            HomeStatusRow("Quality", "${config.resolution.label} • ${config.fps.value} FPS • ${config.bitrateKbps} Kbps", AppSuccess)
        }
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = AppLive, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Continue to Go Live", onClick = onGoLive, enabled = ready)
        if (!ready) {
            Spacer(Modifier.height(9.dp))
            Text("Resolve the items marked as needing attention before going live.", color = AppTextMuted, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
