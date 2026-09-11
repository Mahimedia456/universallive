package com.universallive.app.features.connections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.UlCard
import com.universallive.app.components.UlPrimaryButton
import com.universallive.app.components.UlSecondaryButton
import com.universallive.app.components.UlStatusBadge
import com.universallive.app.components.UlTextField
import com.universallive.app.components.UniversalLiveBrand
import com.universallive.app.integration.ConnectionTestResult
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.integration.StreamingConnection
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.AppBackground
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
import kotlinx.coroutines.launch

/**
 * Locked UI specification for:
 * - Phase 10: Connections
 * - Phase 11: Add Destination
 * - Phase 12: Destination Details / Edit
 *
 * Platform OAuth/account linking is intentionally not faked here. Until the backend
 * adds platform-specific OAuth APIs, YouTube/Facebook/Twitch/TikTok use the existing
 * secure RTMP/RTMPS credential contract. The UI boundary is already isolated so the
 * OAuth implementation can replace only the connection action later.
 */

private data class PlatformSpec(
    val key: String,
    val title: String,
    val subtitle: String,
    val defaultServer: String = "",
)

private val platformCatalog = listOf(
    PlatformSpec("youtube", "YouTube", "Stream to your YouTube channel", "rtmps://a.rtmps.youtube.com/live2"),
    PlatformSpec("facebook", "Facebook", "Stream to a profile or page", "rtmps://live-api-s.facebook.com:443/rtmp/"),
    PlatformSpec("twitch", "Twitch", "Stream to your Twitch channel", "rtmp://live.twitch.tv/app"),
    PlatformSpec("tiktok", "TikTok", "For accounts with encoder / LIVE access"),
    PlatformSpec("custom_rtmp", "Custom RTMP", "Connect any RTMP or RTMPS destination"),
)

@Composable
private fun ConnectionsPage(
    title: String,
    subtitle: String,
    eyebrow: String,
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
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = AppSurface,
                    border = BorderStroke(1.dp, AppBorder),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("‹", color = AppText, fontSize = 28.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            UniversalLiveBrand(compact = true)
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(eyebrow.uppercase(), color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(7.dp))
            Text(title, color = AppText, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(subtitle, color = AppTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun StateError(state: MobileIntegrationState) {
    val error = state.error ?: return
    Surface(
        color = AppLive.copy(alpha = .10f),
        border = BorderStroke(1.dp, AppLive.copy(alpha = .30f)),
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(error, color = AppText, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(14.dp))
    }
    Spacer(Modifier.height(12.dp))
}

private fun platformName(platform: String): String = platformCatalog.firstOrNull { it.key == platform.lowercase() }?.title ?: "Custom RTMP"

private fun platformColor(platform: String): Color = when (platform.lowercase()) {
    "youtube" -> Color(0xFFFF2D3F)
    "facebook" -> Color(0xFF3B82F6)
    "twitch" -> Color(0xFF9A5CFF)
    "tiktok" -> Color(0xFFF3F7FA)
    else -> AppPrimary
}

private fun platformGlyph(platform: String): String = when (platform.lowercase()) {
    "youtube" -> "▶"
    "facebook" -> "f"
    "twitch" -> "T"
    "tiktok" -> "♪"
    else -> "RTMP"
}

@Composable
private fun PlatformGlyph(platform: String, size: Int = 46) {
    val color = platformColor(platform)
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = if (platform == "tiktok") .08f else .14f))
            .border(1.dp, color.copy(alpha = .34f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            platformGlyph(platform),
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = if (platform.lowercase() == "custom_rtmp") 9.sp else (size / 2.25).sp,
        )
    }
}

private fun connectionStatus(item: StreamingConnection?): Pair<String, Color> = when {
    item == null -> "Not connected" to AppTextMuted
    !item.isEnabled -> "Disabled" to AppTextMuted
    item.readyToPublish -> "Connected" to AppSuccess
    item.credentialConfigured -> "Needs test" to AppWarning
    else -> "Setup required" to AppWarning
}

// -----------------------------------------------------------------------------
// Phase 10 - Connections
// -----------------------------------------------------------------------------

@Composable
fun Phase10ConnectionsScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { state.refreshConnections() }

    val ready = state.connections.count { it.readyToPublish && it.isEnabled }
    val uniqueReadyPlatforms = platformCatalog.count { spec ->
        state.connections.any { it.platform.lowercase() == spec.key && it.readyToPublish && it.isEnabled }
    }
    val maxOutputs = state.membership?.maxSimultaneousDestinations ?: 1

    ConnectionsPage(
        title = "Connections",
        subtitle = "Stream to your world. Manage every destination from one place.",
        eyebrow = "Phase 10 • Link your world",
        onBack = onBack,
    ) {
        StateError(state)

        UlCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(AppPrimary.copy(alpha = .08f))
                        .border(5.dp, AppPrimary.copy(alpha = .78f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$uniqueReadyPlatforms/5", color = AppText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Destinations connected", color = AppText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$ready saved destination${if (ready == 1) "" else "s"} ready • ${state.membership?.planName ?: "Free"} plan",
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Up to $maxOutputs simultaneous live output${if (maxOutputs == 1) "" else "s"}",
                        color = AppPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (state.accountLoading && state.connections.isEmpty()) {
            repeat(5) {
                Box(Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(18.dp)).background(AppSurface))
                Spacer(Modifier.height(9.dp))
            }
        } else {
            platformCatalog.forEach { spec ->
                val item = state.connections.firstOrNull { it.platform.lowercase() == spec.key }
                PlatformConnectionRow(
                    spec = spec,
                    item = item,
                    onClick = {
                        if (item != null) {
                            state.selectedConnection = item
                            onRoute(AppRoute.ConnectionDetail)
                        } else {
                            state.beginConnectionSetup(spec.key)
                            onRoute(AppRoute.CustomRtmp)
                        }
                    },
                )
                Spacer(Modifier.height(9.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        UlPrimaryButton("Add Destination", onClick = { onRoute(AppRoute.AddConnection) })
        Spacer(Modifier.height(10.dp))
        Text(
            "Platform account-linking APIs will be connected in the backend integration phase. Until then, the same secure RTMP/RTMPS workflow remains fully usable.",
            color = AppTextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun PlatformConnectionRow(
    spec: PlatformSpec,
    item: StreamingConnection?,
    onClick: () -> Unit,
) {
    val (status, statusColor) = connectionStatus(item)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlatformGlyph(spec.key)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(spec.title, color = AppText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (item?.isDefault == true) {
                    Spacer(Modifier.width(8.dp))
                    Surface(color = AppPrimary.copy(alpha = .12f), shape = RoundedCornerShape(20.dp)) {
                        Text("DEFAULT", color = AppPrimary, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(6.dp))
                Text(status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (item != null && item.displayName.isNotBlank() && item.displayName != spec.title) {
                    Spacer(Modifier.width(8.dp))
                    Text(item.displayName, color = AppTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text("›", color = AppTextSecondary, fontSize = 24.sp)
    }
}

// -----------------------------------------------------------------------------
// Phase 11 - Add Destination
// -----------------------------------------------------------------------------

@Composable
fun Phase11AddDestinationScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    ConnectionsPage(
        title = "Add Destination",
        subtitle = "Choose a platform to connect and start streaming.",
        eyebrow = "Phase 11 • Connect the right platform",
        onBack = onBack,
    ) {
        StateError(state)

        platformCatalog.forEach { spec ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(AppSurface)
                    .border(1.dp, AppBorder, RoundedCornerShape(18.dp))
                    .clickable {
                        state.beginConnectionSetup(spec.key)
                        onRoute(AppRoute.CustomRtmp)
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlatformGlyph(spec.key, 50)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(spec.title, color = AppText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(spec.subtitle, color = AppTextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Text("›", color = AppTextSecondary, fontSize = 25.sp)
            }
            Spacer(Modifier.height(10.dp))
        }

        UlCard {
            Text("How connection works", color = AppText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                "For now, paste the RTMP/RTMPS server and stream key supplied by the platform. Later, backend OAuth APIs can connect accounts directly without redesigning these screens.",
                color = AppTextMuted,
                fontSize = 11.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
fun Phase11PlatformConnectScreen(
    state: MobileIntegrationState,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val platform = state.pendingConnectionPlatform
    val spec = platformCatalog.firstOrNull { it.key == platform } ?: platformCatalog.last()
    var name by remember(platform) { mutableStateOf(if (platform == "custom_rtmp") "My RTMP Server" else "${spec.title} Main") }
    var server by remember(platform) { mutableStateOf(state.defaultServerUrl(platform).ifBlank { spec.defaultServer }) }
    var streamKey by remember(platform) { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ConnectionsPage(
        title = if (platform == "custom_rtmp") "Custom RTMP" else "${spec.title} Connect",
        subtitle = if (platform == "custom_rtmp") {
            "Connect to any RTMP-compatible streaming service."
        } else {
            "Connect ${spec.title} now with its encoder credentials. Direct account linking will plug into this screen in the backend API phase."
        },
        eyebrow = "Phase 11 • ${spec.title}",
        onBack = onBack,
    ) {
        StateError(state)

        UlCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformGlyph(platform, 56)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(spec.title, color = AppText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(AppWarning))
                        Spacer(Modifier.width(6.dp))
                        Text("Not connected", color = AppTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (platform != "custom_rtmp") {
            Surface(
                color = AppPrimary.copy(alpha = .07f),
                border = BorderStroke(1.dp, AppPrimary.copy(alpha = .25f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Backend-ready connection boundary", color = AppPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "OAuth/account selection will be added later. The secure RTMP path below is functional now and uses the same saved destination model.",
                        color = AppTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        UlTextField(name, { name = it }, "Connection name")
        Spacer(Modifier.height(10.dp))
        UlTextField(server, { server = it }, "Server URL", placeholder = "rtmps://...")
        Spacer(Modifier.height(10.dp))
        UlTextField(
            streamKey,
            { streamKey = it },
            "Stream key",
            placeholder = "Paste your private stream key",
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                TextButton(onClick = { reveal = !reveal }) {
                    Text(if (reveal) "Hide" else "Show", color = AppPrimary, fontWeight = FontWeight.Bold)
                }
            },
        )

        Spacer(Modifier.height(12.dp))
        UlCard {
            Text("Your stream key stays private", color = AppText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Universal Live sends credentials to the backend vault. The key is requested only when this authenticated device prepares a live publish session.",
                color = AppTextMuted,
                fontSize = 11.sp,
                lineHeight = 17.sp,
            )
        }

        Spacer(Modifier.height(15.dp))
        UlSecondaryButton(
            "Validate Fields",
            onClick = { state.clearError() },
        )
        Spacer(Modifier.height(9.dp))
        UlPrimaryButton(
            "Save Destination",
            onClick = {
                scope.launch {
                    if (state.saveRtmpConnection(platform, name, server, streamKey)) {
                        // refreshConnections() inside saveRtmpConnection updates selectedConnection.
                        onSaved()
                    }
                }
            },
            enabled = name.isNotBlank() &&
                (server.startsWith("rtmp://") || server.startsWith("rtmps://")) &&
                streamKey.isNotBlank() &&
                !state.accountLoading,
            loading = state.accountLoading,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "A real destination test is available immediately after save from Destination Details.",
            color = AppTextMuted,
            fontSize = 10.sp,
        )
    }
}

// -----------------------------------------------------------------------------
// Phase 12 - Destination Details / Edit
// -----------------------------------------------------------------------------

@Composable
fun Phase12ConnectionDetailScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val item = state.selectedConnection
    val scope = rememberCoroutineScope()
    var testResult by remember(item?.id) { mutableStateOf<ConnectionTestResult?>(null) }
    var showDelete by remember { mutableStateOf(false) }

    ConnectionsPage(
        title = "Manage Connection",
        subtitle = "View details, verify status and control how this destination is used.",
        eyebrow = "Phase 12 • Destination details",
        onBack = onBack,
    ) {
        StateError(state)
        if (item == null) {
            UlCard {
                Text("No destination selected", color = AppText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Return to Connections and choose a destination.", color = AppTextMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
            UlSecondaryButton("Back to Connections", onClick = onBack)
            return@ConnectionsPage
        }

        val (status, statusColor) = connectionStatus(item)
        UlCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformGlyph(item.platform, 56)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.displayName, color = AppText, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(platformName(item.platform), color = AppTextSecondary, fontSize = 12.sp)
                }
                UlStatusBadge(status.uppercase(), statusColor)
            }
            Spacer(Modifier.height(14.dp))
            ConnectionInfoRow("Default", if (item.isDefault) "Yes" else "No")
            ConnectionInfoRow("Enabled", if (item.isEnabled) "Yes" else "No")
            ConnectionInfoRow("Credentials", if (item.credentialConfigured) "Stored securely" else "Missing")
            item.lastTestedAt?.let { ConnectionInfoRow("Last tested", it) }
            item.lastErrorMessage?.takeIf { it.isNotBlank() }?.let { ConnectionInfoRow("Last issue", it) }
        }

        testResult?.let { result ->
            Spacer(Modifier.height(12.dp))
            Surface(
                color = (if (result.ok) AppSuccess else AppWarning).copy(alpha = .10f),
                border = BorderStroke(1.dp, (if (result.ok) AppSuccess else AppWarning).copy(alpha = .32f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (result.ok) "✓" else "!", color = if (result.ok) AppSuccess else AppWarning, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(if (result.ok) "Connection successful" else "Connection needs attention", color = AppText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            if (result.ok) "Backend verified this saved destination." else "Check the server URL / stream key and test again.",
                            color = AppTextSecondary,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        ManageAction("✎", "Edit connection", "Update name, state or encrypted stream credentials") { onRoute(AppRoute.EditConnection) }
        Spacer(Modifier.height(8.dp))
        ManageAction("≈", "Test connection", "Ask the backend to verify this destination") {
            scope.launch { testResult = state.testSelectedConnection() }
        }
        Spacer(Modifier.height(8.dp))
        if (!item.isDefault) {
            ManageAction("★", "Set as default", "Preselect this destination in Go Live") {
                scope.launch { state.updateSelectedConnection(item.displayName, item.isEnabled, true) }
            }
            Spacer(Modifier.height(8.dp))
        }
        ManageAction(
            "▶",
            "Use for Live",
            if (item.readyToPublish && item.isEnabled) "Open Go Live with this destination selected" else "Test and enable this destination first",
            enabled = item.readyToPublish && item.isEnabled,
        ) {
            state.chooseLiveConnection(item.id)
            onRoute(AppRoute.Main(AppDestination.GoLive))
        }

        Spacer(Modifier.height(12.dp))
        UlCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Enable connection", color = AppText, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text("Disabled destinations stay saved but cannot be used for new streams.", color = AppTextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Spacer(Modifier.width(10.dp))
                Switch(
                    checked = item.isEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { state.updateSelectedConnection(item.displayName, enabled, item.isDefault) }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = AppPrimary, checkedThumbColor = Color.White),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { showDelete = true },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = Color.White),
        ) {
            Text("Delete Connection", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    if (showDelete && item != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = AppSurfaceRaised,
            title = { Text("Delete this connection?", color = AppText) },
            text = {
                Text(
                    "${item.displayName} and its encrypted credentials will be permanently removed. This action cannot be undone.",
                    color = AppTextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        scope.launch { if (state.removeSelectedConnection()) onBack() }
                    },
                ) {
                    Text("Delete Connection", color = AppLive, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel", color = AppText)
                }
            },
        )
    }
}

@Composable
fun Phase12EditConnectionScreen(
    state: MobileIntegrationState,
    onBack: () -> Unit,
) {
    val item = state.selectedConnection
    if (item == null) {
        ConnectionsPage("Edit Destination", "No destination is selected.", "Phase 12 • Edit", onBack) {
            UlSecondaryButton("Back", onClick = onBack)
        }
        return
    }

    var displayName by remember(item.id) { mutableStateOf(item.displayName) }
    var enabled by remember(item.id) { mutableStateOf(item.isEnabled) }
    var isDefault by remember(item.id) { mutableStateOf(item.isDefault) }
    var server by remember(item.id) { mutableStateOf("") }
    var key by remember(item.id) { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ConnectionsPage(
        title = "Edit ${platformName(item.platform)} Destination",
        subtitle = "Update destination settings and streaming preferences.",
        eyebrow = "Phase 12 • Edit connection",
        onBack = onBack,
    ) {
        StateError(state)

        UlCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformGlyph(item.platform, 54)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(platformName(item.platform), color = AppText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(if (item.readyToPublish) AppSuccess else AppWarning))
                        Spacer(Modifier.width(6.dp))
                        Text(if (item.readyToPublish) "Connected" else "Needs test", color = AppTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        UlTextField(displayName, { displayName = it }, "Connection name")
        Spacer(Modifier.height(12.dp))

        UlCard {
            ToggleRow("Enable connection", "Allow this destination to be selected for broadcasts.", enabled) { enabled = it }
            Divider(color = AppBorder, modifier = Modifier.padding(vertical = 10.dp))
            ToggleRow("Default destination", "Preselect this destination when you open Go Live.", isDefault) { isDefault = it }
        }

        Spacer(Modifier.height(12.dp))
        UlCard {
            Text("Replace stream credentials", color = AppText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Optional. Leave both fields empty to keep the credentials already stored in the backend vault.",
                color = AppTextMuted,
                fontSize = 11.sp,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(12.dp))
            UlTextField(server, { server = it }, "New server URL", placeholder = "rtmps://...")
            Spacer(Modifier.height(10.dp))
            UlTextField(
                key,
                { key = it },
                "New stream key",
                placeholder = "Enter a new key only when rotating it",
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    TextButton(onClick = { reveal = !reveal }) {
                        Text(if (reveal) "Hide" else "Show", color = AppPrimary, fontWeight = FontWeight.Bold)
                    }
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        UlPrimaryButton(
            "Save Changes",
            onClick = {
                scope.launch {
                    val replacingCredentials = server.isNotBlank() || key.isNotBlank()
                    val credentialsComplete = server.isNotBlank() && key.isNotBlank()
                    if (!replacingCredentials || credentialsComplete) {
                        if (state.updateSelectedConnection(
                                displayName = displayName,
                                isEnabled = enabled,
                                isDefault = isDefault,
                                serverUrl = server.takeIf { it.isNotBlank() },
                                streamKey = key.takeIf { it.isNotBlank() },
                            )
                        ) onBack()
                    }
                }
            },
            enabled = displayName.isNotBlank() &&
                !state.accountLoading &&
                ((server.isBlank() && key.isBlank()) || (server.isNotBlank() && key.isNotBlank())),
            loading = state.accountLoading,
        )
    }
}

@Composable
private fun ConnectionInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AppTextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = AppText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ManageAction(
    code: String,
    title: String,
    body: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) AppSurface else AppSurface.copy(alpha = .55f))
            .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AppPrimary.copy(alpha = .10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(code, color = if (enabled) AppPrimary else AppTextMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) AppText else AppTextMuted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(body, color = AppTextMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Text("›", color = if (enabled) AppTextSecondary else AppTextMuted, fontSize = 22.sp)
    }
}

@Composable
private fun ToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = AppText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(body, color = AppTextMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AppPrimary, checkedThumbColor = Color.White),
        )
    }
}
