package com.universallive.app.features.batch2

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.universallive.app.components.*
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.*

/**
 * Secondary connection support screens only.
 * Primary destination list/add/edit/detail/RTMP screens live in features/integration
 * and are backed by MobileIntegrationState + the Universal Live backend.
 */
@Composable
fun PlatformAuthorizationScreen(onRoute: (AppRoute) -> Unit, onBack: () -> Unit) {
    Batch2Page(
        "Authorize Platform",
        "Continue through the platform authorization flow required for publishing.",
        onBack,
    ) {
        InfoCard("Secure authorization", "Only permissions required for channel access and live publishing are requested.")
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
    Batch2Page("Select Channel", "Choose the channel Universal Live should authorize for publishing.", onBack) {
        listOf(
            "Universal Live Creator" to "@universallive",
            "Gaming Channel" to "@ul-gaming",
        ).forEachIndexed { index, item ->
            InfoCard(
                item.first,
                item.second,
                if (selected == index) "SELECTED" else null,
                AppPrimary,
            ) { selected = index }
        }
        UlPrimaryButton("Use This Channel", onClick = {})
    }
}

@Composable
fun ConnectionTestScreen(onBack: () -> Unit) {
    Batch2Page("Connection Test", "Checking the publish route without exposing stream credentials.", onBack) {
        StepStatus("Service connection", "Passed")
        StepStatus("Credentials", "Configured")
        StepStatus("Destination", "Ready")
        InfoCard("Connection ready", "This destination is ready for a live broadcast.", "READY")
        UlPrimaryButton("Done", onClick = onBack)
    }
}

@Composable
fun DisconnectConfirmationScreen(onBack: () -> Unit) {
    Batch2Page("Remove Destination?", "This removes the saved destination from Universal Live only.", onBack) {
        InfoCard("Confirm removal", "Your external platform account or channel will not be deleted.")
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppLive,
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = .70f),
            ),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text("Remove Destination") }
        UlSecondaryButton("Keep Destination", onClick = onBack)
    }
}

@Composable
fun ConnectionTroubleshootingScreen(onBack: () -> Unit) {
    Batch2Page("Connection Problem", "Check common destination issues without exposing secrets.", onBack) {
        InfoCard("Authentication expired", "Reconnect the platform account to refresh authorization.", "FIX")
        InfoCard("Invalid stream key", "Replace the key from the platform live control room.", "FIX", AppWarning)
        InfoCard("Server unavailable", "Retry when the ingest endpoint becomes reachable.", "RETRY", AppWarning)
        InfoCard("Network issue", "Check device connectivity and run Stream Readiness.", "CHECK", AppWarning)
    }
}
