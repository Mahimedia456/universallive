package com.universallive.app.features.integration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.integration.*
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.theme.*
import kotlinx.coroutines.launch

@Composable
private fun BackendError(state: MobileIntegrationState) {
    val message = state.error ?: return
    UlCard {
        Text(message, color = AppLive, fontSize = 12.sp)
    }
}

private fun platformSubtitle(item: StreamingConnection): String = when {
    !item.isEnabled -> "Disabled • enable it before broadcasting"
    item.readyToPublish -> "RTMP credentials configured • ready to publish"
    item.credentialConfigured -> "Credentials saved • run a connection test"
    else -> "Stream key setup required"
}

@Composable
fun ConnectedConnectionsScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { state.refreshConnections() }

    com.universallive.app.features.batch2.Batch2Page(
        "Connections",
        "Manage every channel you can publish to. Add, edit, test, select or remove a destination here.",
        onBack,
    ) {
        BackendError(state)

        UlCard {
            Text("Broadcast destinations", color = AppText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                "${state.connections.count { it.readyToPublish }} ready • ${state.connections.size} saved",
                color = AppTextSecondary,
                fontSize = 12.sp,
            )
        }

        if (state.accountLoading && state.connections.isEmpty()) {
            UlCard { Text("Loading your connections…", color = AppTextSecondary) }
        }

        if (!state.accountLoading && state.connections.isEmpty()) {
            UlCard {
                Text("No destinations yet", color = AppText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Add YouTube, Facebook, Twitch, TikTok or any RTMP/RTMPS server. You will enter the stream key supplied by that platform.",
                    color = AppTextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        state.connections.forEach { item ->
            UlCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(item.displayName, color = AppText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.platform.replace("_", " ").uppercase(),
                            color = AppPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    UlStatusBadge(
                        when {
                            !item.isEnabled -> "DISABLED"
                            item.readyToPublish -> "READY"
                            else -> "SETUP"
                        },
                        when {
                            !item.isEnabled -> AppTextMuted
                            item.readyToPublish -> AppSuccess
                            else -> AppWarning
                        },
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(platformSubtitle(item), color = AppTextSecondary, fontSize = 12.sp)
                if (item.isDefault) {
                    Spacer(Modifier.height(5.dp))
                    Text("DEFAULT DESTINATION", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            state.selectedConnection = item
                            onRoute(AppRoute.ConnectionDetail)
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        border = BorderStroke(1.dp, AppBorder),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppText),
                    ) {
                        Text("Manage", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            state.chooseLiveConnection(item.id)
                            onRoute(AppRoute.Main(AppDestination.GoLive))
                        },
                        enabled = item.readyToPublish && item.isEnabled,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppPrimary,
                            contentColor = Color.White,
                            disabledContainerColor = AppSurfaceInteractive,
                            disabledContentColor = AppTextMuted,
                        ),
                    ) {
                        Text(if (item.readyToPublish) "Use for Live" else "Needs Setup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        UlPrimaryButton("Add Destination", onClick = { onRoute(AppRoute.AddConnection) })
        Text(
            "Saving channels is not plan-limited. Your membership controls how many simultaneous outputs the backend may authorize.",
            color = AppTextMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
fun ConnectedAddConnectionScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    com.universallive.app.features.batch2.Batch2Page(
        "Add Destination",
        "Choose a platform, then paste the RTMP/RTMPS server and stream key from its live dashboard.",
        onBack,
    ) {
        BackendError(state)

        listOf(
            Triple("YouTube", "youtube", "YouTube Live Control Room • RTMPS supported"),
            Triple("Facebook Live", "facebook", "Facebook Live Producer • secure RTMPS ingest"),
            Triple("Twitch", "twitch", "Twitch Creator Dashboard • stream key required"),
            Triple("TikTok Live", "tiktok", "For accounts that have LIVE Studio/encoder access"),
            Triple("Custom RTMP", "custom_rtmp", "Any compatible RTMP or RTMPS destination"),
        ).forEach { item ->
            com.universallive.app.features.batch2.InfoCard(
                item.first,
                item.third,
                onClick = {
                    state.beginConnectionSetup(item.second)
                    onRoute(AppRoute.CustomRtmp)
                },
            )
        }
    }
}

@Composable
fun ConnectedCustomRtmpScreen(
    state: MobileIntegrationState,
    onBack: () -> Unit,
) {
    val platform = state.pendingConnectionPlatform
    val label = state.platformLabel(platform)
    var name by remember(platform) { mutableStateOf(if (platform == "custom_rtmp") "Custom RTMP" else "$label Main") }
    var server by remember(platform) { mutableStateOf(state.defaultServerUrl(platform)) }
    var key by remember(platform) { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    com.universallive.app.features.batch2.Batch2Page(
        "Connect $label",
        if (platform == "custom_rtmp") "Add a secure RTMP destination." else "Use the server URL and stream key shown in your $label live/encoder dashboard.",
        onBack,
    ) {
        BackendError(state)

        UlCard {
            Text("1. Open $label live settings", color = AppText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("2. Copy the RTMP/RTMPS server and stream key\n3. Paste both below and save\n4. Test the destination, then select it from Go Live", color = AppTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }

        UlTextField(name, { name = it }, "Destination name")
        UlTextField(server, { server = it }, "Server URL", placeholder = "rtmps://...")
        UlTextField(
            key,
            { key = it },
            "Stream key",
            placeholder = "Paste your private stream key",
            visualTransformation = if (reveal) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
            trailing = {
                TextButton(onClick = { reveal = !reveal }) {
                    Text(if (reveal) "Hide" else "Show", color = AppPrimary)
                }
            },
        )

        Text(
            "Your stream key is encrypted by the backend vault. The app requests it only when the authenticated device starts publishing.",
            color = AppTextMuted,
            fontSize = 11.sp,
        )

        UlPrimaryButton(
            "Save Destination",
            onClick = {
                scope.launch {
                    if (state.saveRtmpConnection(platform, name, server, key)) onBack()
                }
            },
            enabled = name.isNotBlank() &&
                (server.startsWith("rtmp://") || server.startsWith("rtmps://")) &&
                key.isNotBlank() && !state.accountLoading,
            loading = state.accountLoading,
        )
    }
}

@Composable
fun ConnectedConnectionDetailScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val item = state.selectedConnection
    val scope = rememberCoroutineScope()
    var testResult by remember(item?.id) { mutableStateOf<ConnectionTestResult?>(null) }
    var showDelete by remember { mutableStateOf(false) }

    com.universallive.app.features.batch2.Batch2Page(
        item?.displayName ?: "Connection",
        "Destination controls",
        onBack,
    ) {
        BackendError(state)

        if (item == null) {
            UlCard { Text("Select a connection first.", color = AppTextSecondary) }
        } else {
            com.universallive.app.features.batch2.InfoCard(
                item.platform.replace("_", " ").uppercase(),
                platformSubtitle(item),
                when {
                    item.readyToPublish -> "READY"
                    item.credentialConfigured -> "CHECK"
                    else -> "SETUP"
                },
                when {
                    item.readyToPublish -> AppSuccess
                    else -> AppWarning
                },
            )

            testResult?.let {
                com.universallive.app.features.batch2.InfoCard(
                    if (it.ok) "Connection ready" else "Destination needs setup",
                    if (it.ok) "Backend verified the destination configuration." else "Update the RTMP server/key and test again.",
                    if (it.ok) "READY" else "FIX",
                    if (it.ok) AppSuccess else AppWarning,
                )
            }

            UlPrimaryButton(
                "Test Destination",
                onClick = { scope.launch { testResult = state.testSelectedConnection() } },
                enabled = !state.accountLoading,
                loading = state.accountLoading,
            )

            if (item.readyToPublish && item.isEnabled) {
                UlSecondaryButton(
                    "Use This Destination for Live",
                    onClick = {
                        state.chooseLiveConnection(item.id)
                        onRoute(AppRoute.Main(AppDestination.GoLive))
                    },
                )
            }

            UlSecondaryButton("Edit Destination", onClick = { onRoute(AppRoute.EditConnection) })

            Button(
                onClick = { showDelete = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = Color.White),
            ) {
                Text("Delete Destination", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDelete && item != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = AppSurfaceRaised,
            title = { Text("Delete ${item.displayName}?", color = AppText) },
            text = { Text("This removes the saved destination and its encrypted stream credentials from Universal Live.", color = AppTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    scope.launch { if (state.removeSelectedConnection()) onBack() }
                }) { Text("Delete", color = AppLive, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel", color = AppTextSecondary) }
            },
        )
    }
}

@Composable
fun ConnectedEditConnectionScreen(
    state: MobileIntegrationState,
    onBack: () -> Unit,
) {
    val item = state.selectedConnection
    if (item == null) {
        com.universallive.app.features.batch2.Batch2Page("Edit Destination", "No destination selected.", onBack) {
            UlSecondaryButton("Back", onClick = onBack)
        }
        return
    }

    var name by remember(item.id) { mutableStateOf(item.displayName) }
    var enabled by remember(item.id) { mutableStateOf(item.isEnabled) }
    var isDefault by remember(item.id) { mutableStateOf(item.isDefault) }
    var server by remember(item.id) { mutableStateOf("") }
    var key by remember(item.id) { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    com.universallive.app.features.batch2.Batch2Page(
        "Edit Destination",
        "Change display settings or replace the RTMP credentials.",
        onBack,
    ) {
        BackendError(state)
        UlTextField(name, { name = it }, "Destination name")

        UlCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Enabled", color = AppText, fontWeight = FontWeight.Bold)
                    Text("Disabled destinations cannot be selected for a broadcast.", color = AppTextMuted, fontSize = 11.sp)
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Default destination", color = AppText, fontWeight = FontWeight.Bold)
                    Text("Preselected automatically on Go Live.", color = AppTextMuted, fontSize = 11.sp)
                }
                Switch(checked = isDefault, onCheckedChange = { isDefault = it })
            }
        }

        UlCard {
            Text("Replace stream credentials", color = AppText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Optional. Leave both fields empty to keep the current encrypted credentials.", color = AppTextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            UlTextField(server, { server = it }, "New server URL", placeholder = "rtmps://...")
            Spacer(Modifier.height(8.dp))
            UlTextField(key, { key = it }, "New stream key", visualTransformation = PasswordVisualTransformation())
        }

        UlPrimaryButton(
            "Save Changes",
            onClick = {
                scope.launch {
                    if (state.updateSelectedConnection(name, enabled, isDefault, server, key)) onBack()
                }
            },
            enabled = name.isNotBlank() && !state.accountLoading,
            loading = state.accountLoading,
        )
    }
}

@Composable
fun ConnectedStudioHomeScreen(
    state: MobileIntegrationState,
    sceneState: SceneState,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    LaunchedEffect(Unit) { state.refreshScenes() }

    AppScaffold(
        title = "Studio",
        selected = AppDestination.Scenes,
        onDestinationChanged = onDestination,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BackendError(state)

            com.universallive.app.features.batch3.PreviewCanvas(
                label = "BACKEND STUDIO",
                footer = state.selectedScene?.let { "${it.name} • ${it.aspectRatio}" }
                    ?: "Create your first backend-synced scene",
            )

            UlCard {
                Text("Backend-Synced Scenes", color = AppText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "${state.scenes.size} scene(s) synced • local live compositor remains available for Android broadcast",
                    color = AppTextMuted,
                    fontSize = 11.sp,
                )
            }

            UlPrimaryButton("Scene Library", onClick = { onRoute(AppRoute.SceneLibrary) })
            UlSecondaryButton("Edit Local Live Composition", onClick = { onRoute(AppRoute.SceneEditor) })
            UlSecondaryButton("Go Live", onClick = { onDestination(AppDestination.GoLive) })
        }
    }
}

@Composable
fun ConnectedSceneLibraryScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { state.refreshScenes() }

    com.universallive.app.features.batch3.StudioPage(
        title = "Scene Library",
        subtitle = "Scenes below are stored against your authenticated account.",
        onBack = onBack,
    ) {
        BackendError(state)

        state.scenes.forEach { scene ->
            val selected = state.selectedScene?.id == scene.id
            Surface(
                color = if (selected) AppSurfaceInteractive else AppSurface,
                border = BorderStroke(1.dp, if (selected) AppPrimary else AppBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.selectedScene = scene },
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(scene.name, color = AppText, fontWeight = FontWeight.Bold)
                            Text("${scene.aspectRatio} • ${scene.width}×${scene.height}", color = AppTextMuted, fontSize = 11.sp)
                        }
                        if (scene.isDefault) UlStatusBadge("DEFAULT", AppPrimary)
                    }
                }
            }
        }

        UlTextField(newName, { newName = it }, "New scene name", placeholder = "Gaming Scene")
        UlPrimaryButton(
            "Create Backend Scene",
            onClick = {
                scope.launch {
                    if (state.addCloudScene(newName)) newName = ""
                }
            },
            enabled = !state.accountLoading,
            loading = state.accountLoading,
        )

        if (state.selectedScene != null) {
            UlSecondaryButton(
                "Duplicate Selected",
                onClick = { scope.launch { state.duplicateSelectedScene() } },
            )
            Button(
                onClick = { scope.launch { state.removeSelectedScene() } },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = androidx.compose.ui.graphics.Color.White),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Archive Selected Scene", fontWeight = FontWeight.Bold)
            }
        }

        UlSecondaryButton("Open Scene Editor", onClick = { onRoute(AppRoute.SceneEditor) })
    }
}
