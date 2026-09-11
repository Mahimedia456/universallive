package com.universallive.app.features.integration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.integration.DeviceStreamTelemetry
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ConnectedActivityScreen(
    state: MobileIntegrationState,
    onDestination: (AppDestination) -> Unit,
) {
    LaunchedEffect(Unit) { state.refreshHistory() }

    AppScaffold(
        title = "Activity",
        selected = AppDestination.Activity,
        onDestinationChanged = onDestination,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.accountLoading && state.streamHistory.isEmpty()) {
                UlCard { Text("Loading stream history…", color = AppTextSecondary) }
            }

            if (state.streamHistory.isEmpty() && !state.accountLoading) {
                UlCard {
                    Text("No broadcasts yet", color = AppText, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("Completed broadcasts will appear here automatically.", color = AppTextMuted, fontSize = 12.sp)
                }
            }

            state.streamHistory.forEach { item ->
                UlCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title ?: "Universal Live Broadcast", color = AppText, fontWeight = FontWeight.Bold)
                            Text(
                                "${item.status.uppercase()}${item.durationSeconds?.let { " • ${it}s" } ?: ""}",
                                color = AppTextSecondary,
                                fontSize = 11.sp,
                            )
                        }
                        UlStatusBadge(item.status.uppercase(), if (item.status == "completed") AppSuccess else AppPrimary)
                    }
                    if (item.avgBitrateKbps != null || item.avgFps != null) {
                        Spacer(Modifier.height(7.dp))
                        Text(
                            buildString {
                                item.avgBitrateKbps?.let { append("${it} Kbps") }
                                if (item.avgBitrateKbps != null && item.avgFps != null) append(" • ")
                                item.avgFps?.let { append("${it.toInt()} FPS") }
                                if (item.droppedFrames > 0) append(" • ${item.droppedFrames} dropped")
                            },
                            color = AppTextMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectedGoLiveBackendPanel(
    state: MobileIntegrationState,
) {
    val scope = rememberCoroutineScope()
    val membership = state.membership
    val selected = state.selectedConnection

    UlCard {
        Text("BACKEND SESSION", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(
            state.activeBroadcast?.let { "Session ${it.status.uppercase()}" } ?: "Ready to create live session",
            color = AppText,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Plan: ${membership?.planName ?: "Free"} • ${membership?.maxSimultaneousDestinations ?: 1} simultaneous destination(s)",
            color = AppTextSecondary,
            fontSize = 11.sp,
        )
        if (selected != null) {
            Text("Destination: ${selected.displayName}", color = AppTextMuted, fontSize = 11.sp)
        }
    }

    if (state.activeBroadcast == null) {
        UlPrimaryButton(
            "Prepare Backend Session",
            onClick = {
                scope.launch {
                    val ids = listOfNotNull(selected?.id)
                    state.beginBroadcast(
                        title = "Universal Live Broadcast",
                        connectionIds = ids,
                        sceneId = state.selectedScene?.id,
                    )
                }
            },
            enabled = !state.loading,
            loading = state.loading,
        )
    } else {
        Button(
            onClick = { scope.launch { state.endBroadcast() } },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppLive,
                contentColor = AppText,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("End Backend Session", fontWeight = FontWeight.Bold)
        }

        LaunchedEffect(state.activeBroadcast?.id) {
            while (isActive && state.activeBroadcast != null) {
                state.heartbeat(
                    DeviceStreamTelemetry(
                        connectionId = state.selectedLiveConnectionId,
                        networkStatus = "active",
                        publishStatus = "live",
                    )
                )
                delay(15_000)
            }
        }
    }
}

@Composable
fun ConnectedNotificationsScreen(
    state: MobileIntegrationState,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { state.refreshNotifications() }

    com.universallive.app.features.batch6.SettingsPage(
        "Notifications",
        "Account and broadcast notifications from Universal Live backend.",
        onBack,
    ) {
        if (state.notifications.isEmpty() && !state.accountLoading) {
            UlCard { Text("No notifications yet.", color = AppTextSecondary) }
        }

        state.notifications.forEach { item ->
            UlCard {
                Text(item.title, color = AppText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(item.body, color = AppTextSecondary, fontSize = 12.sp)
                if (!item.isRead) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { scope.launch { state.markNotificationRead(item.id) } }) {
                        Text("Mark as read", color = AppPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectedSupportScreen(
    state: MobileIntegrationState,
    onBack: () -> Unit,
) {
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { state.refreshSupportTickets() }

    com.universallive.app.features.batch6.SettingsPage(
        "Support",
        "Create and track support requests against your authenticated account.",
        onBack,
    ) {
        UlTextField(subject, { subject = it }, "Subject")
        UlTextField(description, { description = it }, "Description")
        UlPrimaryButton(
            "Create Support Ticket",
            onClick = {
                scope.launch {
                    if (state.submitSupportTicket("mobile", subject, description)) {
                        subject = ""
                        description = ""
                    }
                }
            },
            enabled = subject.isNotBlank() && description.isNotBlank() && !state.accountLoading,
            loading = state.accountLoading,
        )

        state.supportTickets.forEach { item ->
            UlCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.subject, color = AppText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    UlStatusBadge(item.status.uppercase(), AppPrimary)
                }
                Spacer(Modifier.height(5.dp))
                Text(item.description, color = AppTextSecondary, fontSize = 12.sp)
            }
        }
    }
}
