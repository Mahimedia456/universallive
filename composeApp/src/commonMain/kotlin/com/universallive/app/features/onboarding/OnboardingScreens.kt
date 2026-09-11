package com.universallive.app.features.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.permissions.PermissionGrantState
import com.universallive.app.permissions.PermissionSetupController
import com.universallive.app.theme.*
import kotlinx.coroutines.launch

@Composable
private fun PermissionPage(
    step: Int,
    title: String,
    body: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AppBackground, AppBackgroundSecondary, AppBackground),
                ),
            )
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
            Text("$step of 6", color = AppTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("PERMISSIONS", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { step / 6f },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = AppPrimary,
            trackColor = AppSurfaceInteractive,
        )
        Spacer(Modifier.height(30.dp))
        Text(title, color = AppText, fontSize = 29.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
        Spacer(Modifier.height(9.dp))
        Text(body, color = AppTextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun HeroPermissionIcon(symbol: String) {
    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(AppPrimary.copy(alpha = .08f))
            .border(1.dp, AppPrimary.copy(alpha = .34f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AppPrimary.copy(alpha = .12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, color = AppPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PermissionBenefit(icon: String, title: String, body: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(AppPrimary.copy(alpha = .1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = AppPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = AppText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(body, color = AppTextMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

private fun permissionLabel(state: PermissionGrantState, optional: Boolean = false): String = when (state) {
    PermissionGrantState.GRANTED -> "Enabled"
    PermissionGrantState.DENIED -> if (optional) "Not enabled" else "Needs attention"
    PermissionGrantState.NOT_REQUIRED -> "Ready"
    PermissionGrantState.UNKNOWN -> if (optional) "Optional" else "Not set"
}

private fun permissionColor(state: PermissionGrantState): Color = when (state) {
    PermissionGrantState.GRANTED, PermissionGrantState.NOT_REQUIRED -> AppSuccess
    PermissionGrantState.DENIED -> AppWarning
    PermissionGrantState.UNKNOWN -> AppTextMuted
}

@Composable
fun PermissionHubScreen(
    state: MobileIntegrationState,
    permissions: PermissionSetupController,
    onNavigate: (AppRoute) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snap = permissions.snapshot

    PermissionPage(
        step = 1,
        title = "Set up device permissions",
        body = "This is the one-time device setup for your creator account. Android permission dialogs open directly from this page; you can change optional permissions later in Settings.",
        onBack = { onNavigate(AppRoute.CreatorSetupComplete) },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            HeroPermissionIcon("✓")
        }
        Spacer(Modifier.height(20.dp))

        UlCard {
            Text("Notifications", color = AppText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text("Live status, reconnect and background-stream alerts.", color = AppTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UlStatusBadge(permissionLabel(snap.notifications, optional = true), permissionColor(snap.notifications))
                Spacer(Modifier.weight(1f))
                if (snap.notifications != PermissionGrantState.GRANTED && snap.notifications != PermissionGrantState.NOT_REQUIRED) {
                    TextButton(onClick = permissions::requestNotifications) { Text("Enable", color = AppPrimary) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        UlCard {
            Text("Microphone", color = AppText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text("Needed only when you want voice commentary or microphone audio.", color = AppTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UlStatusBadge(permissionLabel(snap.microphone, optional = true), permissionColor(snap.microphone))
                Spacer(Modifier.weight(1f))
                if (snap.microphone != PermissionGrantState.GRANTED) {
                    TextButton(onClick = permissions::requestMicrophone) { Text("Enable", color = AppPrimary) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        UlCard {
            Text("Camera", color = AppText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text("Optional. Enable it for facecam or camera scenes.", color = AppTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UlStatusBadge(permissionLabel(snap.camera, optional = true), permissionColor(snap.camera))
                Spacer(Modifier.weight(1f))
                if (snap.camera != PermissionGrantState.GRANTED) {
                    TextButton(onClick = permissions::requestCamera) { Text("Enable", color = AppPrimary) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        UlCard {
            Text("Screen capture", color = AppText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(5.dp))
            Text(
                "Android's protected screen-capture consent is requested only when you actually tap Go Live. Universal Live does not start screen recording during onboarding.",
                color = AppTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.height(18.dp))
        OnboardingInlineError(state.error, state::clearError)
        UlPrimaryButton(
            text = "Finish Setup",
            onClick = {
                permissions.markScreenCaptureEducationComplete()
                scope.launch {
                    if (
                        state.completePermissionSetup(
                            notificationsAcknowledged = snap.notifications != PermissionGrantState.UNKNOWN,
                            microphoneAcknowledged = snap.microphone != PermissionGrantState.UNKNOWN,
                            cameraAcknowledged = snap.camera != PermissionGrantState.UNKNOWN,
                        )
                    ) {
                        onNavigate(AppRoute.Main(AppDestination.Home))
                    }
                }
            },
            loading = state.accountLoading,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "You can continue even if optional permissions are denied. Required permissions are checked again before Go Live.",
            color = AppTextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OnboardingInlineError(message: String?, onDismiss: () -> Unit) {
    if (message.isNullOrBlank()) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AppWarning.copy(alpha = .09f),
        border = BorderStroke(1.dp, AppWarning.copy(alpha = .35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Dismiss", color = AppWarning, fontSize = 11.sp) }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
fun NotificationPermissionScreen(
    state: MobileIntegrationState,
    permissions: PermissionSetupController,
    onNavigate: (AppRoute) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val status = permissions.snapshot.notifications

    PermissionPage(
        step = 2,
        title = "Enable notifications",
        body = "Get live-status notifications so you can return to your stream quickly when Universal Live is running in the background.",
        onBack = { onNavigate(AppRoute.PermissionHub) },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { HeroPermissionIcon("!") }
        Spacer(Modifier.height(20.dp))
        UlCard {
            Text("Live session alerts", color = AppText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Text("Stream status, reconnect alerts and the persistent Android LIVE notification use this permission where the operating system requires it.", color = AppTextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(14.dp))
            UlStatusBadge(permissionLabel(status), permissionColor(status))
        }
        Spacer(Modifier.height(14.dp))
        if (status != PermissionGrantState.GRANTED && status != PermissionGrantState.NOT_REQUIRED) {
            UlPrimaryButton("Allow Notifications", onClick = permissions::requestNotifications)
            Spacer(Modifier.height(10.dp))
        }
        UlSecondaryButton(
            if (status == PermissionGrantState.UNKNOWN) "Not Now" else "Continue",
            onClick = {
                scope.launch {
                    if (state.acknowledgePermission(notification = true)) {
                        onNavigate(AppRoute.MicrophonePermission)
                    }
                }
            },
        )
    }
}

@Composable
fun MicrophonePermissionScreen(
    state: MobileIntegrationState,
    permissions: PermissionSetupController,
    onNavigate: (AppRoute) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val status = permissions.snapshot.microphone

    PermissionPage(
        step = 3,
        title = "Allow microphone access",
        body = "Use your microphone for commentary, talk streams and live voice. You can still mute it at any time while broadcasting.",
        onBack = { onNavigate(AppRoute.NotificationPermission) },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { HeroPermissionIcon("MIC") }
        Spacer(Modifier.height(20.dp))
        UlCard {
            PermissionBenefit("VOX", "Voice commentary", "Mix your microphone with supported device audio.")
            PermissionBenefit("CTL", "Always under your control", "Mute or disable the microphone before or during a stream.")
            Spacer(Modifier.height(6.dp))
            UlStatusBadge(permissionLabel(status), permissionColor(status))
        }
        Spacer(Modifier.height(16.dp))
        if (status != PermissionGrantState.GRANTED) {
            UlPrimaryButton("Allow Microphone", onClick = permissions::requestMicrophone)
            Spacer(Modifier.height(10.dp))
        }
        UlSecondaryButton(
            if (status == PermissionGrantState.UNKNOWN) "Continue Without Microphone" else "Continue",
            onClick = {
                scope.launch {
                    if (state.acknowledgePermission(microphone = true)) {
                        onNavigate(AppRoute.CameraPermission)
                    }
                }
            },
        )
    }
}

@Composable
fun CameraPermissionScreen(
    state: MobileIntegrationState,
    permissions: PermissionSetupController,
    onNavigate: (AppRoute) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val status = permissions.snapshot.camera

    PermissionPage(
        step = 4,
        title = "Camera access is optional",
        body = "Enable camera access if you want facecam or camera scenes. Screen and game streaming works without camera access.",
        onBack = { onNavigate(AppRoute.MicrophonePermission) },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { HeroPermissionIcon("CAM") }
        Spacer(Modifier.height(20.dp))
        UlCard {
            PermissionBenefit("FC", "Facecam", "Use the front or rear camera as a scene source.")
            PermissionBenefit("OPT", "Optional", "Skip this now and enable it later from Settings.")
            Spacer(Modifier.height(6.dp))
            UlStatusBadge(permissionLabel(status, optional = true), permissionColor(status))
        }
        Spacer(Modifier.height(16.dp))
        if (status != PermissionGrantState.GRANTED) {
            UlPrimaryButton("Allow Camera", onClick = permissions::requestCamera)
            Spacer(Modifier.height(10.dp))
        }
        UlSecondaryButton(
            if (status == PermissionGrantState.UNKNOWN || status == PermissionGrantState.DENIED) "Skip Camera" else "Continue",
            onClick = {
                scope.launch {
                    if (state.acknowledgePermission(camera = true)) {
                        onNavigate(AppRoute.ScreenCaptureEducation)
                    }
                }
            },
        )
    }
}

@Composable
fun ScreenCaptureEducationScreen(
    state: MobileIntegrationState,
    permissions: PermissionSetupController,
    onNavigate: (AppRoute) -> Unit,
) {
    val scope = rememberCoroutineScope()

    PermissionPage(
        step = 5,
        title = "Allow screen & audio capture",
        body = "Android and iOS protect screen capture with a system confirmation. Universal Live asks for that consent when you actually start a stream.",
        onBack = { onNavigate(AppRoute.CameraPermission) },
    ) {
        UlCard {
            Text("What happens at Go Live", color = AppText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(14.dp))
            listOf(
                "1" to "Tap Start Live after preflight.",
                "2" to "Approve the operating-system screen capture prompt.",
                "3" to "Choose entire device or the supported app/screen option.",
                "4" to "Universal Live starts capture only after your approval.",
            ).forEach { (n, text) ->
                Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(AppPrimary.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                        Text(n, color = AppPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(text, color = AppTextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f), lineHeight = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        UlCard {
            Text("Privacy first", color = AppText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Universal Live cannot bypass the system capture dialog and does not start screen recording during onboarding.", color = AppTextMuted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton(
            "I Understand - Continue",
            onClick = {
                permissions.markScreenCaptureEducationComplete()
                scope.launch {
                    if (state.acknowledgePermission(screenCapture = true)) {
                        onNavigate(AppRoute.PermissionsComplete)
                    }
                }
            },
            loading = state.accountLoading,
        )
    }
}

@Composable
fun PermissionsCompleteScreen(
    state: MobileIntegrationState,
    permissions: PermissionSetupController,
    onNavigate: (AppRoute) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snap = permissions.snapshot

    PermissionPage(
        step = 6,
        title = "Your permissions are ready",
        body = "Setup is complete. You can change optional permissions later, and screen-capture consent will still be requested securely when you go live.",
        onBack = { onNavigate(AppRoute.ScreenCaptureEducation) },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { HeroPermissionIcon("✓") }
        Spacer(Modifier.height(20.dp))
        UlCard {
            PermissionSummaryRow("Microphone", permissionLabel(snap.microphone), permissionColor(snap.microphone))
            PermissionSummaryRow("Notifications", permissionLabel(snap.notifications), permissionColor(snap.notifications))
            PermissionSummaryRow("Camera (optional)", permissionLabel(snap.camera, true), permissionColor(snap.camera))
            PermissionSummaryRow("Screen capture", "Ready at Go Live", AppSuccess)
        }
        Spacer(Modifier.height(16.dp))
        UlCard {
            Text("Next step", color = AppPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(6.dp))
            Text("Connect YouTube, Facebook, Twitch, TikTok or a Custom RTMP destination from Home when you're ready.", color = AppTextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.height(18.dp))
        UlPrimaryButton(
            "Continue to Home",
            onClick = {
                scope.launch {
                    if (state.completePermissionSetup()) {
                        onNavigate(AppRoute.Main(AppDestination.Home))
                    }
                }
            },
            loading = state.accountLoading,
        )
    }
}

@Composable
private fun PermissionSummaryRow(label: String, status: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, color = AppText, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text(status, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// Compatibility wrappers for old navigation references. They now route into the new 6-step flow.
@Composable
fun MicCameraPermissionScreen(onNavigate: (AppRoute) -> Unit) {
    LaunchedEffect(Unit) { onNavigate(AppRoute.MicrophonePermission) }
}

@Composable
fun StudioReadyScreen(onNavigate: (AppRoute) -> Unit) {
    LaunchedEffect(Unit) { onNavigate(AppRoute.PermissionsComplete) }
}
