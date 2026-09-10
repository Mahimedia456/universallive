package com.universallive.app.features.integration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

@Composable
fun ConnectedConnectionsScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { state.refreshConnections() }

    com.universallive.app.features.batch2.Batch2Page(
        "Connections",
        "Your saved broadcast destinations are loaded from Universal Live cloud.",
        onBack,
    ) {
        BackendError(state)

        if (state.accountLoading && state.connections.isEmpty()) {
            UlCard { Text("Loading connections…", color = AppTextSecondary) }
        }

        if (!state.accountLoading && state.connections.isEmpty()) {
            UlCard {
                Text("No connections yet", color = AppText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Connect YouTube, Facebook, Twitch or a Custom RTMP destination.", color = AppTextMuted, fontSize = 12.sp)
            }
        }

        state.connections.forEach { item ->
            UlCard(
                modifier = Modifier.clickable {
                    state.selectedConnection = item
                    onRoute(AppRoute.ConnectionDetail)
                }
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(item.displayName, color = AppText, fontWeight = FontWeight.Bold)
                        Text(
                            item.platform.replace("_", " ").uppercase(),
                            color = AppPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    UlStatusBadge(
                        if (item.status == "connected") "CONNECTED" else item.status.uppercase(),
                        if (item.status == "connected") AppSuccess else AppTextMuted,
                    )
                }
                if (item.isDefault) {
                    Spacer(Modifier.height(7.dp))
                    Text("DEFAULT DESTINATION", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        UlPrimaryButton("Add Connection", onClick = { onRoute(AppRoute.AddConnection) })
        Text(
            "Free can manage multiple saved channels. Only simultaneous LIVE output is plan-limited.",
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
    val scope = rememberCoroutineScope()

    com.universallive.app.features.batch2.Batch2Page(
        "Add Connection",
        "Choose the destination you want to save to your account.",
        onBack,
    ) {
        BackendError(state)

        listOf(
            "YouTube" to "youtube",
            "Facebook Live" to "facebook",
            "Twitch" to "twitch",
        ).forEach { item ->
            com.universallive.app.features.batch2.InfoCard(
                item.first,
                "Create the cloud connection now; OAuth authorization is completed when provider credentials are configured.",
                onClick = {
                    scope.launch {
                        if (state.addConnection(item.second, "${item.first} Main", state.connections.isEmpty())) {
                            onRoute(AppRoute.ConnectionDetail)
                        }
                    }
                }
            )
        }

        com.universallive.app.features.batch2.InfoCard(
            "Custom RTMP",
            "Store RTMP server and stream key in the encrypted backend vault.",
            onClick = { onRoute(AppRoute.CustomRtmp) },
        )
    }
}

@Composable
fun ConnectedCustomRtmpScreen(
    state: MobileIntegrationState,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("Custom RTMP") }
    var server by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    com.universallive.app.features.batch2.Batch2Page(
        "Custom RTMP",
        "Credentials are encrypted by the backend before storage.",
        onBack,
    ) {
        BackendError(state)
        UlTextField(name, { name = it }, "Connection name")
        UlTextField(server, { server = it }, "Server URL", placeholder = "rtmps://...")
        UlTextField(key, { key = it }, "Stream key", visualTransformation = PasswordVisualTransformation())
        UlPrimaryButton(
            "Save Secure Connection",
            onClick = {
                scope.launch {
                    if (state.saveCustomRtmp(name, server, key)) onBack()
                }
            },
            enabled = name.isNotBlank() &&
                (server.startsWith("rtmp://") || server.startsWith("rtmps://")) &&
                key.isNotBlank() &&
                !state.accountLoading,
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
    var testResult by remember { mutableStateOf<ConnectionTestResult?>(null) }

    com.universallive.app.features.batch2.Batch2Page(
        item?.displayName ?: "Connection",
        "Cloud destination detail",
        onBack,
    ) {
        BackendError(state)

        if (item == null) {
            UlCard { Text("Select a connection first.", color = AppTextSecondary) }
        } else {
            com.universallive.app.features.batch2.InfoCard(
                item.platform.replace("_", " ").uppercase(),
                "Status: ${item.status}",
                if (item.isDefault) "DEFAULT" else "SAVED",
                AppPrimary,
            )
            testResult?.let {
                com.universallive.app.features.batch2.InfoCard(
                    if (it.ok) "Connection ready" else "Authorization required",
                    "Backend test status: ${it.status}",
                    if (it.ok) "READY" else "CHECK",
                    if (it.ok) AppSuccess else AppWarning,
                )
            }
            UlPrimaryButton(
                "Test Connection",
                onClick = { scope.launch { testResult = state.testSelectedConnection() } },
                enabled = !state.accountLoading,
                loading = state.accountLoading,
            )
            Button(
                onClick = {
                    scope.launch {
                        if (state.removeSelectedConnection()) onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = AppText),
            ) {
                Text("Remove Connection", fontWeight = FontWeight.Bold)
            }
        }
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
                label = "CLOUD STUDIO",
                footer = state.selectedScene?.let { "${it.name} • ${it.aspectRatio}" }
                    ?: "Create your first cloud scene",
            )

            UlCard {
                Text("Cloud Scenes", color = AppText, fontWeight = FontWeight.Bold)
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
            "Create Cloud Scene",
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
                colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = AppText),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Archive Selected Scene", fontWeight = FontWeight.Bold)
            }
        }

        UlSecondaryButton("Open Scene Editor", onClick = { onRoute(AppRoute.SceneEditor) })
    }
}
