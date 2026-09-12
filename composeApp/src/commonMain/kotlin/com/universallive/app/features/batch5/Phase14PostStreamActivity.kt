package com.universallive.app.features.batch5

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.theme.*

@Composable
fun StreamProcessingScreen(
    onSummary: () -> Unit,
    onHome: () -> Unit,
) {
    Batch5Page(
        title = "Finishing Your Stream",
        subtitle = "Finalizing the local broadcast summary.",
    ) {
        StateCard(
            "Processing",
            "Universal Live is finalizing stream metrics and activity data.",
            "PROCESSING",
            AppPrimary,
        ) {
            LinearProgressIndicator(
                progress = { .76f },
                modifier = Modifier.fillMaxWidth(),
                color = AppPrimary,
                trackColor = AppSurfaceInteractive,
            )
        }

        StateCard(
            "You can continue",
            "Your completed broadcast will appear here after its stream summary is finalized.",
        )

        UlPrimaryButton("View Stream Summary", onClick = onSummary)
        UlSecondaryButton("Go Home", onClick = onHome)
    }
}

@Composable
fun StreamSummaryScreen(
    captureController: CaptureController,
    onPerformance: () -> Unit,
    onDone: () -> Unit,
) {
    val snap = captureController.snapshot
    val bitrate = snap.networkBitrateBps / 1000L

    Batch5Page(
        title = "Stream Summary",
        subtitle = "Broadcast ended",
    ) {
        StateCard(
            "Completed",
            snap.publishTarget.ifBlank { "Universal Live broadcast" },
            "ENDED",
            AppSuccess,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Resolution", "${snap.encoderWidth}×${snap.encoderHeight}", Modifier.weight(1f))
            StatChip("FPS", snap.encoderFps.toString(), Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Bitrate", if (bitrate > 0) "${bitrate}K" else "—", Modifier.weight(1f))
            StatChip("Frames", snap.publishedVideoFrames.toString(), Modifier.weight(1f))
        }

        StateCard("Connection Quality", snap.publishMessage.ifBlank { "Session completed." })

        UlPrimaryButton("Done", onClick = onDone)
        UlSecondaryButton("View Performance", onClick = onPerformance)
    }
}

@Composable
fun StreamPerformanceScreen(
    captureController: CaptureController,
    onBack: () -> Unit,
) {
    val snap = captureController.snapshot
    val encodedMb = snap.encodedVideoBytes / (1024f * 1024f)

    Batch5Page(
        title = "Performance",
        subtitle = "Technical broadcast metrics.",
        onBack = onBack,
    ) {
        StateCard("Bitrate Timeline", "Bitrate history appears when telemetry samples are available for this broadcast.") {
            LinearProgressIndicator(
                progress = { .72f },
                modifier = Modifier.fillMaxWidth(),
                color = AppPrimary,
                trackColor = AppSurfaceInteractive,
            )
        }
        StateCard("Frame Delivery", "${snap.publishedVideoFrames} video frames published.")
        StateCard("Encoded Video", "${encodedMb.toInt()} MB encoded during the latest local session.")
        StateCard("Audio", "${snap.publishedAudioFrames} AAC frames published.")
        StateCard("Dropped Frames", "Dropped-frame telemetry appears here when a live session reports encoder health data.")
    }
}

@Composable
fun ActivityV2Screen(
    onRoute: (AppRoute) -> Unit,
    onDestinationChanged: (AppDestination) -> Unit,
) {
    AppScaffold(
        title = "Activity",
        selected = AppDestination.Activity,
        onDestinationChanged = onDestinationChanged,
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Past broadcasts",
                color = AppText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Review completed, interrupted and failed sessions.",
                color = AppTextSecondary,
                fontSize = 13.sp,
            )

            StateCard(
                "Tonight's Live Session",
                "Today • 1h 24m • YouTube",
                "COMPLETED",
                AppSuccess,
                onClick = { onRoute(AppRoute.ActivityDetail) },
            )
            StateCard(
                "Gameplay Test",
                "Yesterday • 18m • Custom RTMP",
                "INTERRUPTED",
                AppWarning,
                onClick = { onRoute(AppRoute.ActivityDetail) },
            )
            StateCard(
                "Encoder Test",
                "Sep 08 • 4m • YouTube",
                "FAILED",
                AppLive,
                onClick = { onRoute(AppRoute.ActivityDetail) },
            )
        }
    }
}

@Composable
fun ActivityDetailScreen(onBack: () -> Unit) {
    Batch5Page(
        title = "Stream Detail",
        subtitle = "Tonight's Live Session",
        onBack = onBack,
    ) {
        StateCard("Status", "Session completed successfully.", "COMPLETED", AppSuccess)
        StateCard("Scene", "Main")
        StateCard("Quality", "1080p • 30 fps")
        StateCard("Destination", "YouTube")
        StateCard("Duration", "1h 24m")
        StateCard("Connection Health", "Stable for most of the broadcast.")
        UlSecondaryButton("View Performance", onClick = {})
    }
}
