package com.universallive.app.features.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.*

@Composable
private fun SetupPage(
    step: String,
    title: String,
    body: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(Modifier.weight(1f))
            UlStatusBadge(step, AppPrimary)
        }
        Spacer(Modifier.height(30.dp))
        UlSectionHeader("Studio setup", title, body)
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun CreatorSetupScreen(onNavigate: (AppRoute) -> Unit) {
    val options = listOf("Gaming", "IRL", "Tutorials", "Creative", "Other")
    var selected by remember { mutableStateOf("Gaming") }

    SetupPage(
        step = "1 / 5",
        title = "What do you create?",
        body = "We'll use this to prepare practical studio defaults. You can change it later.",
        onBack = { onNavigate(AppRoute.AccountCreatedSuccess) },
    ) {
        options.forEach { option ->
            Surface(
                onClick = { selected = option },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(UlRadius.card),
                color = if (selected == option) AppSurfaceInteractive else AppSurface,
                border = BorderStroke(1.dp, if (selected == option) AppPrimary else AppBorder),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (selected == option) AppPrimary.copy(alpha = .16f) else AppSurfaceRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(option.take(1), color = if (selected == option) AppPrimary else AppTextMuted, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(option, color = AppText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = selected == option,
                        onClick = { selected = option },
                        colors = RadioButtonDefaults.colors(selectedColor = AppPrimary),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        UlPrimaryButton("Continue", onClick = { onNavigate(AppRoute.PermissionHub) })
        TextButton(onClick = { onNavigate(AppRoute.PermissionHub) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Skip for now", color = AppTextMuted)
        }
    }
}

@Composable
fun PermissionHubScreen(onNavigate: (AppRoute) -> Unit) {
    SetupPage(
        step = "2 / 5",
        title = "Studio permissions",
        body = "Universal Live requests only the device permissions needed for broadcasting.",
        onBack = { onNavigate(AppRoute.CreatorSetup) },
    ) {
        PermissionRow("Microphone", "Voice commentary and creator audio", "Required")
        PermissionRow("Camera", "Optional face cam while broadcasting", "Optional")
        PermissionRow("Screen Capture", "Your gameplay or mobile screen", "At Go Live")
        PermissionRow("Notifications", "Live status and recovery alerts", "Recommended")
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Set Up Permissions", onClick = { onNavigate(AppRoute.MicCameraPermission) })
    }
}

@Composable
private fun PermissionRow(title: String, body: String, status: String) {
    UlCard(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(AppPrimary.copy(alpha = .11f)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = AppText, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(body, color = AppTextMuted, fontSize = 12.sp)
            }
            Text(status, color = AppPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MicCameraPermissionScreen(onNavigate: (AppRoute) -> Unit) {
    var micEnabled by remember { mutableStateOf(false) }
    var cameraEnabled by remember { mutableStateOf(false) }

    SetupPage(
        step = "3 / 5",
        title = "Microphone & camera",
        body = "Prepare your creator inputs now. The operating system permission dialog will appear when implementation is connected.",
        onBack = { onNavigate(AppRoute.PermissionHub) },
    ) {
        PermissionToggle(
            title = "Microphone",
            body = "Use your microphone in broadcasts.",
            checked = micEnabled,
            onCheckedChange = { micEnabled = it },
        )
        Spacer(Modifier.height(10.dp))
        PermissionToggle(
            title = "Camera",
            body = "Enable front or rear face cam.",
            checked = cameraEnabled,
            onCheckedChange = { cameraEnabled = it },
        )
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Continue", onClick = { onNavigate(AppRoute.ScreenCaptureEducation) })
    }
}

@Composable
private fun PermissionToggle(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    UlCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = AppText, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(body, color = AppTextMuted, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppBackground,
                    checkedTrackColor = AppPrimary,
                    uncheckedThumbColor = AppTextMuted,
                    uncheckedTrackColor = AppSurfaceInteractive,
                ),
            )
        }
    }
}

@Composable
fun ScreenCaptureEducationScreen(onNavigate: (AppRoute) -> Unit) {
    SetupPage(
        step = "4 / 5",
        title = "Share your screen safely",
        body = "When you start a broadcast, Android or iOS will show its own secure screen-capture confirmation.",
        onBack = { onNavigate(AppRoute.MicCameraPermission) },
    ) {
        UlCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlowNode("PHONE")
                Text("→", color = AppPrimary, fontSize = 22.sp)
                FlowNode("LIVE")
                Text("→", color = AppPrimary, fontSize = 22.sp)
                FlowNode("AUDIENCE")
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Universal Live does not bypass system privacy controls. Screen capture starts only after you approve the operating-system prompt.",
                color = AppTextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Continue", onClick = { onNavigate(AppRoute.StudioReady) })
        TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Learn More", color = AppPrimary)
        }
    }
}

@Composable
private fun FlowNode(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurfaceInteractive)
            .border(1.dp, AppPrimary.copy(alpha = .25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = AppText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StudioReadyScreen(onNavigate: (AppRoute) -> Unit) {
    Box(
        Modifier.fillMaxSize().background(AppBackground).systemBarsPadding().padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        UlCard {
            UlStatusBadge("STUDIO READY")
            Spacer(Modifier.height(18.dp))
            UlSectionHeader(
                "Setup complete",
                "Your studio is ready",
                "Your mobile broadcast workspace is prepared. Connect a channel next or explore the studio.",
            )
            Spacer(Modifier.height(18.dp))
            listOf("Creator profile", "Permissions", "Screen capture guidance").forEach { item ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AppSuccess))
                    Spacer(Modifier.width(10.dp))
                    Text(item, color = AppTextSecondary)
                }
            }
            Spacer(Modifier.height(20.dp))
            UlPrimaryButton(
                "Connect My First Channel",
                onClick = { onNavigate(AppRoute.Main(AppDestination.Connections)) },
            )
            Spacer(Modifier.height(10.dp))
            UlSecondaryButton(
                "Explore Studio",
                onClick = { onNavigate(AppRoute.Main(AppDestination.Scenes)) },
            )
        }
    }
}
