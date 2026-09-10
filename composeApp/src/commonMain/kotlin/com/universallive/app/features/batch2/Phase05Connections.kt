package com.universallive.app.features.batch2

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.universallive.app.components.*
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.*

@Composable
fun ConnectionsV2Screen(onRoute: (AppRoute) -> Unit, onBack: () -> Unit) {
    Batch2Page("Connections", "Connect the places where you broadcast.", onBack) {
        InfoCard("YouTube Main", "Primary creator channel • Default destination", "CONNECTED", onClick = { onRoute(AppRoute.ConnectionDetail) })
        InfoCard("Facebook Live", "Connected account • Available", "CONNECTED", onClick = { onRoute(AppRoute.ConnectionDetail) })
        InfoCard("Custom RTMP", "No server configured yet", "SET UP", AppPrimary, onClick = { onRoute(AppRoute.CustomRtmp) })
        UlPrimaryButton("Add Connection", onClick = { onRoute(AppRoute.AddConnection) })
        Text("Free plan: connect multiple channels, broadcast to one destination at a time.", color = AppTextMuted)
    }
}

@Composable
fun AddConnectionScreen(onRoute: (AppRoute) -> Unit, onBack: () -> Unit) {
    Batch2Page("Add Connection", "Choose a platform or connect a custom RTMP destination.", onBack) {
        InfoCard("YouTube", "Connect your YouTube channel.", onClick = { onRoute(AppRoute.PlatformAuthorization) })
        InfoCard("Twitch", "Connect a Twitch channel.", onClick = { onRoute(AppRoute.PlatformAuthorization) })
        InfoCard("Facebook Live", "Connect a Facebook destination.", onClick = { onRoute(AppRoute.PlatformAuthorization) })
        InfoCard("Custom RTMP", "Any compatible RTMP or RTMPS ingest server.", onClick = { onRoute(AppRoute.CustomRtmp) })
    }
}

@Composable
fun PlatformAuthorizationScreen(onRoute: (AppRoute) -> Unit, onBack: () -> Unit) {
    Batch2Page("Authorize Platform", "Universal Live only requests permissions required to publish and manage the selected channel.", onBack) {
        InfoCard("Secure authorization", "You will continue to the platform's official authorization flow.")
        StepStatus("Identity", "Required")
        StepStatus("Channel access", "Required")
        StepStatus("Live publishing", "Required")
        UlPrimaryButton("Continue to Platform", onClick = { onRoute(AppRoute.ChannelPicker) })
        UlSecondaryButton("Cancel", onClick = onBack)
    }
}

@Composable
fun ChannelPickerScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf(0) }
    Batch2Page("Select Channel", "Choose which channel Universal Live should use.", onBack) {
        listOf("Universal Live Creator" to "@universallive", "Gaming Channel" to "@ul-gaming").forEachIndexed { index, item ->
            InfoCard(item.first, item.second, if (selected == index) "SELECTED" else null, AppPrimary) { selected = index }
        }
        UlPrimaryButton("Use This Channel", onClick = {})
    }
}

@Composable
fun CustomRtmpScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("Custom RTMP") }
    var server by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    Batch2Page("Custom RTMP", "Add a secure custom ingest destination.", onBack) {
        UlTextField(name, { name = it }, "Connection name")
        UlTextField(server, { server = it }, "Server URL", placeholder = "rtmps://...")
        UlTextField(key, { key = it }, "Stream key", visualTransformation = PasswordVisualTransformation())
        Text("Stream keys are treated as secrets and should never be displayed publicly.", color = AppTextMuted)
        UlSecondaryButton("Test Connection", onClick = {})
        UlPrimaryButton("Save Connection", onClick = {})
    }
}

@Composable
fun ConnectionDetailScreen(onRoute: (AppRoute) -> Unit, onBack: () -> Unit) {
    Batch2Page("YouTube Main", "Connection detail", onBack) {
        InfoCard("Connected", "Destination is healthy and ready to publish.", "CONNECTED")
        StepStatus("Connection health", "Excellent")
        StepStatus("Default destination", "Yes")
        StepStatus("Last verified", "Just now")
        UlPrimaryButton("Test Connection", onClick = { onRoute(AppRoute.ConnectionTest) })
        UlSecondaryButton("Edit Connection", onClick = { onRoute(AppRoute.EditConnection) })
        UlSecondaryButton("Troubleshoot", onClick = { onRoute(AppRoute.ConnectionTroubleshooting) })
        TextButton(onClick = { onRoute(AppRoute.DisconnectConfirmation) }) { Text("Disconnect", color = AppLive) }
    }
}

@Composable
fun EditConnectionScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("YouTube Main") }
    var default by remember { mutableStateOf(true) }
    Batch2Page("Edit Connection", "Update destination preferences.", onBack) {
        UlTextField(name, { name = it }, "Connection name")
        InfoCard("Platform", "YouTube • OAuth connected", "CONNECTED")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Default destination", color = AppText)
            Switch(checked = default, onCheckedChange = { default = it })
        }
        UlPrimaryButton("Save Changes", onClick = {})
    }
}

@Composable
fun ConnectionTestScreen(onBack: () -> Unit) {
    Batch2Page("Connection Test", "Checking the complete publish route.", onBack) {
        StepStatus("Resolving server", "Passed")
        StepStatus("Authenticating", "Passed")
        StepStatus("Testing publish route", "Passed")
        StepStatus("Destination readiness", "Ready")
        InfoCard("Connection ready", "This destination is ready for a live broadcast.", "READY")
        UlPrimaryButton("Done", onClick = onBack)
    }
}

@Composable
fun DisconnectConfirmationScreen(onBack: () -> Unit) {
    Batch2Page("Disconnect YouTube?", "Removing this connection will not delete the channel or platform account.", onBack) {
        InfoCard("YouTube Main", "This destination will be removed from Universal Live.")
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = AppText, disabledContentColor = AppText.copy(alpha = .70f)),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text("Disconnect") }
        UlSecondaryButton("Keep Connected", onClick = onBack)
    }
}

@Composable
fun ConnectionTroubleshootingScreen(onBack: () -> Unit) {
    Batch2Page("Connection Problem", "Diagnose common destination issues without exposing secrets.", onBack) {
        InfoCard("Authentication expired", "Reconnect the platform account to refresh authorization.", "FIX")
        InfoCard("Invalid stream key", "Update the key from the platform's live control room.", "FIX", AppWarning)
        InfoCard("Server unavailable", "Retry when the ingest endpoint becomes reachable.", "RETRY", AppWarning)
        InfoCard("Network issue", "Check the device connection and run Stream Readiness.", "CHECK", AppWarning)
    }
}
