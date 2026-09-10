package com.universallive.app.features.batch5

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.capture.PublishStatus
import com.universallive.app.theme.*

@Composable
fun NetworkDegradedScreen(
    captureController: CaptureController,
    onBack: () -> Unit,
) {
    val snap = captureController.snapshot
    val bitrate = snap.networkBitrateBps / 1000L

    Batch5Page(
        title = "Connection Unstable",
        subtitle = "Universal Live is keeping the broadcast active.",
        onBack = onBack,
    ) {
        StateCard(
            "Network degraded",
            "The stream is still running. Universal Live will keep publishing while the network recovers.",
            "WARNING",
            AppWarning,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Bitrate", if (bitrate > 0) "${bitrate} Kbps" else "Low", Modifier.weight(1f))
            StatChip("Status", snap.publishStatus.name, Modifier.weight(1f))
        }

        StateCard(
            "Recommended action",
            "Keep the app running and move to a stronger Wi-Fi or mobile data connection. Avoid changing resolution during an active stream.",
        )

        UlPrimaryButton("Continue Streaming", onClick = onBack)
        UlSecondaryButton("Open Live Stats", onClick = onBack)
    }
}

@Composable
fun ReconnectingScreen(
    captureController: CaptureController,
    onBack: () -> Unit,
) {
    val snap = captureController.snapshot

    Batch5Page(
        title = "Reconnecting",
        subtitle = "Your broadcast has not been intentionally ended.",
        onBack = onBack,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(AppSurface, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = AppPrimary)
                Spacer(Modifier.height(14.dp))
                Text("Reconnecting ingest", color = AppText, fontSize = 18.sp)
            }
        }

        StateCard(
            "Destination",
            snap.publishTarget.ifBlank { "Active RTMP destination" },
            snap.publishStatus.name,
            AppWarning,
        )

        StateCard(
            "What happens now",
            "Universal Live retries the destination while preserving the current capture session.",
        )

        UlPrimaryButton("Keep Reconnecting", onClick = onBack)
    }
}

@Composable
fun DestinationFailureScreen(onBack: () -> Unit) {
    Batch5Page(
        title = "Destination Problem",
        subtitle = "Healthy destinations can continue while one destination recovers.",
        onBack = onBack,
    ) {
        StateCard("YouTube", "Publishing normally.", "LIVE", AppSuccess)
        StateCard("Twitch", "Connection lost. Authentication or ingest route may need recovery.", "FAILED", AppLive)

        UlPrimaryButton("Retry Failed Destination", onClick = {})
        UlSecondaryButton("Continue Other Destinations", onClick = onBack)
    }
}

@Composable
fun StreamInterruptedScreen(
    captureController: CaptureController,
    onBack: () -> Unit,
) {
    val snap = captureController.snapshot

    Batch5Page(
        title = "Stream Interrupted",
        subtitle = "Recover the capture path without hiding the live state.",
        onBack = onBack,
    ) {
        StateCard(
            "Capture interruption",
            snap.message.ifBlank { "Screen capture, audio route or the operating system interrupted the session." },
            "ACTION NEEDED",
            AppWarning,
        )
        StateCard("Screen Capture", "Re-authorize the operating-system capture session if it ended.")
        StateCard("Audio Route", snap.audioMessage.ifBlank { "Check microphone and device audio routing." })

        UlPrimaryButton("Recover Stream", onClick = onBack)
        UlSecondaryButton("Return to Live", onClick = onBack)
    }
}

@Composable
fun ResilienceHubScreen(
    captureController: CaptureController,
    onNetwork: () -> Unit,
    onReconnect: () -> Unit,
    onDestinationFailure: () -> Unit,
    onInterrupted: () -> Unit,
    onBack: () -> Unit,
) {
    Batch5Page(
        title = "Live Recovery",
        subtitle = "Production recovery states for active broadcasts.",
        onBack = onBack,
    ) {
        StateCard(
            "Current publish state",
            captureController.snapshot.publishMessage,
            captureController.snapshot.publishStatus.name,
            when (captureController.snapshot.publishStatus) {
                PublishStatus.LIVE -> AppSuccess
                PublishStatus.RECONNECTING -> AppWarning
                PublishStatus.ERROR -> AppLive
                else -> AppPrimary
            },
        )
        StateCard("Unstable Network", "Bitrate drops or packet delivery becomes unstable.", onClick = onNetwork)
        StateCard("Reconnecting", "RTMP ingest connection is being restored.", onClick = onReconnect)
        StateCard("Destination Failure", "One destination fails while other destinations can remain live.", onClick = onDestinationFailure)
        StateCard("Capture Interrupted", "MediaProjection, audio route or app interruption requires recovery.", onClick = onInterrupted)
    }
}
