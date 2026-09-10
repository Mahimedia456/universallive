package com.universallive.app.features.batch2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.*

@Composable
fun HomeV2Screen(
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    AppScaffold("Home", AppDestination.Home, onDestination) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier
                        .background(Brush.verticalGradient(listOf(AppSurfaceRaised, AppBackgroundSecondary)))
                        .padding(20.dp),
                ) {
                    Text("GO LIVE ANYWHERE", color = AppPrimary, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Ready to stream", color = AppText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("Share your story with the world.", color = AppTextSecondary)
                    Spacer(Modifier.height(18.dp))
                    UlPrimaryButton("Go Live", onClick = { onDestination(AppDestination.GoLive) })
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Network" to "Excellent", "Microphone" to "Ready", "Scene" to "Ready", "Destination" to "Connected").forEach {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("✓", color = AppSuccess, fontWeight = FontWeight.Bold)
                                Text(it.first, color = AppText, fontSize = 10.sp)
                                Text(it.second, color = AppTextSecondary, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(onClick = { onRoute(AppRoute.StreamReadiness) }, color = AppSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(15.dp)) {
                        Text("Stream Readiness", color = AppText, fontWeight = FontWeight.Bold)
                        Text("All systems ready", color = AppSuccess, fontSize = 12.sp)
                    }
                }
                Surface(onClick = { onRoute(AppRoute.Notifications) }, color = AppSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(15.dp)) {
                        Text("Notifications", color = AppText, fontWeight = FontWeight.Bold)
                        Text("2 unread", color = AppPrimary, fontSize = 12.sp)
                    }
                }
            }

            InfoCard("Current Scene", "Main Camera • 1080p • 30 fps")
            InfoCard("Default Destination", "YouTube • Public", "CONNECTED")
            InfoCard("Stream Quality", "1080p • 30 fps • 6.8 Mbps")
            InfoCard("Recent Stream", "Product Launch • 1h 24m • Completed")
            InfoCard(
                "Current Plan",
                "Free • One simultaneous live destination",
                "FREE",
                AppPrimary,
                onClick = { onRoute(AppRoute.Plans) },
            )
        }
    }
}

@Composable
fun StreamReadinessScreen(onBack: () -> Unit) {
    Batch2Page("Stream Readiness", "Check every broadcast-critical system before going live.", onBack) {
        InfoCard("Pre-flight checks", "Universal Live verifies the capture, audio and destination path before broadcast.")
        listOf(
            "Network" to "Excellent",
            "Microphone" to "Ready",
            "Device Audio" to "Ready",
            "Screen Capture" to "Ready",
            "Destination" to "Connected",
            "Scene" to "Ready",
        ).forEach { StepStatus(it.first, it.second) }
        UlPrimaryButton("Continue to Go Live", onClick = {})
    }
}

@Composable
fun QuickGoLiveScreen(onBack: () -> Unit) {
    var title by remember { mutableStateOf("Tonight's Live Session") }
    Batch2Page("Quick Go Live", "Confirm the essentials, then continue to preflight.", onBack) {
        UlTextField(title, { title = it }, "Stream title")
        InfoCard("Scene", "Main Camera • 1080p • 30 fps")
        InfoCard("Destination", "YouTube • Public", "READY")
        InfoCard("Quality", "Auto • Recommended")
        UlPrimaryButton("Continue to Preflight", onClick = {})
        UlSecondaryButton("Open Advanced Setup", onClick = {})
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit, onDetail: () -> Unit) {
    Batch2Page("Notifications", "Connection, stream, subscription and system updates.", onBack) {
        InfoCard("YouTube connected", "Your YouTube destination is healthy and ready for your next stream.", "NEW", AppPrimary, onDetail)
        InfoCard("Stream completed", "Your previous broadcast ended successfully and is available in Activity.", "NEW", AppPrimary, onDetail)
        InfoCard("Creator plan", "Review Creator features when you need simultaneous multi-destination streaming.", onClick = onDetail)
    }
}

@Composable
fun NotificationDetailScreen(onBack: () -> Unit) {
    Batch2Page("Notification", "Destination update", onBack) {
        InfoCard("YouTube connection healthy", "Universal Live verified your destination. No action is required.")
        Text("Just now", color = AppTextMuted, fontSize = 12.sp)
        UlPrimaryButton("View Connection", onClick = {})
    }
}
