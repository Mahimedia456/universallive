package com.universallive.app.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.AppScaffold
import com.universallive.app.components.StreamCard
import com.universallive.app.navigation.AppDestination
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.capture.PublishStatus
import com.universallive.app.streaming.preview.LiveOutputPreview
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.*

@Composable
fun HomeScreen(
    streamState: StreamConfigState,
    captureController: CaptureController,
    onDestinationChanged: (AppDestination) -> Unit,
) {
    val config = streamState.config
    val snap = captureController.snapshot

    AppScaffold(
        title = "Broadcast Studio",
        selected = AppDestination.Home,
        onDestinationChanged = onDestinationChanged,
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))

            if (snap.isActive || snap.isPublishing || snap.publishStatus == PublishStatus.ERROR) {
                LiveBroadcastContainer(captureController) { onDestinationChanged(AppDestination.GoLive) }
            } else {
                Column(
                    Modifier.fillMaxWidth().background(AppSurface, RoundedCornerShape(24.dp))
                        .border(BorderStroke(1.dp, AppBorder), RoundedCornerShape(24.dp)).padding(20.dp)
                ) {
                    Text("READY TO BROADCAST", color = AppSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("Go live anywhere", color = AppText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Your screen, game audio, microphone, facecam and overlays — one broadcast studio.", color = AppTextMuted, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { onDestinationChanged(AppDestination.GoLive) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = androidx.compose.ui.graphics.Color.White, disabledContentColor = androidx.compose.ui.graphics.Color.White),
                    ) { Text("OPEN LIVE STUDIO", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Current stream profile", color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StreamCard("Video", config.videoSummary, Modifier.weight(1f))
                StreamCard("Bitrate", config.bitrateLabel, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StreamCard("Microphone", if (config.microphoneEnabled) "ON" else "OFF", Modifier.weight(1f))
                StreamCard("Game audio", if (config.internalAudioEnabled) "ON" else "OFF", Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun LiveBroadcastContainer(controller: CaptureController, openStudio: () -> Unit) {
    val snap = controller.snapshot
    val targetKbps = snap.encoderBitrateKbps.coerceAtLeast(1)
    val actualKbps = (snap.networkBitrateBps / 1000L).toInt()
    val ratio = if (targetKbps > 0) actualKbps.toFloat() / targetKbps else 0f
    val health = when {
        snap.publishStatus != PublishStatus.LIVE -> if (snap.isPublishing) "CONNECTING" else "LOCAL"
        ratio >= .80f -> "EXCELLENT"
        ratio >= .60f -> "GOOD"
        ratio >= .35f -> "DEGRADED"
        else -> "POOR"
    }
    val liveLabel = when (snap.publishStatus) {
        PublishStatus.LIVE -> "LIVE"
        PublishStatus.CONNECTING -> "CONNECTING"
        PublishStatus.RECONNECTING -> "RECONNECTING"
        PublishStatus.ERROR -> "ERROR"
        else -> "CAPTURING"
    }

    Column(
        Modifier.fillMaxWidth().background(AppSurface, RoundedCornerShape(24.dp))
            .border(BorderStroke(1.dp, if (snap.publishStatus == PublishStatus.LIVE) AppLive else AppBorder), RoundedCornerShape(24.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (snap.publishStatus == PublishStatus.LIVE) AppLive else AppSurfaceRaised, shape = RoundedCornerShape(50)) {
                Text("●  $liveLabel", color = AppText, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(snap.publishTarget.ifBlank { "UniversalLive output" }, color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(snap.publishMessage, color = AppTextMuted, fontSize = 11.sp, maxLines = 1)
            }
            TextButton(onClick = openStudio) { Text("STUDIO", fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(12.dp))
        LiveOutputPreview(
            Modifier.fillMaxWidth().aspectRatio(
                if (snap.encoderWidth > 0 && snap.encoderHeight > 0) snap.encoderWidth.toFloat() / snap.encoderHeight else 16f / 9f
            ).background(AppBackground, RoundedCornerShape(16.dp)).border(1.dp, AppBorder, RoundedCornerShape(16.dp))
        )

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LiveMetric("HEALTH", health, Modifier.weight(1f))
            LiveMetric("VIDEO", "${snap.publishedVideoFrames} frames", Modifier.weight(1f))
            LiveMetric("AUDIO", "${snap.publishedAudioFrames} frames", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LiveMetric("NETWORK", if (actualKbps > 0) "$actualKbps Kbps" else "Waiting", Modifier.weight(1f))
            LiveMetric("FACECAM", if (snap.facecamActive) "ACTIVE" else if (controller.requestedFacecamEnabled) "WAITING" else "OFF", Modifier.weight(1f))
            LiveMetric("SCENE", snap.activeSceneName, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LiveMetric("SOURCE", snap.captureMode, Modifier.weight(1f))
            LiveMetric("ENCODER", if (snap.encodedVideoFrames > 0) "RUNNING" else "WAITING", Modifier.weight(1f))
            LiveMetric("INGEST", if (snap.publishedVideoFrames > 0) "RECEIVING" else if (snap.isPublishing) "HANDSHAKE" else "OFF", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().background(AppBackground, RoundedCornerShape(12.dp)).padding(10.dp)) {
            Text("LIVE ACTIVITY", color = AppTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(snap.message, color = AppText, fontSize = 11.sp, maxLines = 2)
            Text(snap.publishMessage, color = AppTextMuted, fontSize = 10.sp, maxLines = 2)
            Text(snap.audioMessage, color = AppTextMuted, fontSize = 10.sp, maxLines = 2)
        }
    }
}

@Composable
private fun LiveMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(AppSurfaceRaised, RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(label, color = AppTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(value, color = AppText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
